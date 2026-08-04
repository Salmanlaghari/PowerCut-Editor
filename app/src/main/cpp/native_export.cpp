// =============================================================================
// PowerCut Editor — JNI bridge for the native C++ Export Engine.
//
// Implements the external methods declared in
//   app/src/main/java/com/powercut/editor/export/ExportEngine.kt
//
// JNI symbol naming follows the Java package convention:
//   Java_com_powercut_editor_export_ExportEngine_<method>
//
// When the full FFmpeg + LevelDB export engine is compiled (CMake detects
// third_party/ffmpeg and third_party/leveldb), the ExportEngine class is
// provided by src/export/export_engine.cpp. When those libraries are not
// available, this file provides stub implementations so the shared library
// links and the Kotlin layer can call the guarded external methods without
// crashing (start() returns false, running() returns false, etc.).
// =============================================================================
#include <jni.h>
#include <memory>
#include <string>
#include <vector>
#include <cmath>
#include "powercut/export/export_engine.h"
#include "powercut/core/dag.h"

using PowerCut::ExportEngine;
using PowerCut::ExportConfig;
using PowerCut::ExportPreset;
using PowerCut::PowerCutDAG;
using PowerCut::DAGSegment;
using PowerCut::AudioSegment;
using PowerCut::EffectNode;
using PowerCut::Keyframe;
using PowerCut::TimeMicros;

// ---------------------------------------------------------------------------
// Detect whether the full export engine is available. When building only the
// JNI stub (FFmpeg/LevelDB not found), we provide inline stub implementations
// of ExportEngine so the library links. The full build provides the real
// implementations in src/export/export_engine.cpp.
//
// We use a preprocessor guard: POWERCUT_FULL_EXPORT_ENGINE is defined by CMake
// when the full sources are added to the build. Otherwise, we define stubs here.
// ---------------------------------------------------------------------------
#ifndef POWERCUT_FULL_EXPORT_ENGINE

// ---- Stub ExportEngine (used when FFmpeg/LevelDB not available) ----
// These match the declarations in export_engine.h but do nothing real.
namespace PowerCut {

struct ExportEngine::Impl {
    ExportConfig cfg;
    std::atomic<bool> run{false};
};

ExportEngine::ExportEngine() : m(std::make_unique<Impl>()) {}
ExportEngine::~ExportEngine() = default;
bool ExportEngine::running() const { return m->run; }
void ExportEngine::on_progress(Cb f) { (void)f; }

bool ExportEngine::start(PowerCutDAG* d, const ExportConfig& c) {
    (void)d;
    m->cfg = c;
    // Stub: no actual export — return false so the Kotlin layer knows
    // the native engine is not fully wired yet.
    return false;
}

void ExportEngine::cancel() { m->run = false; }

void ExportEngine::worker() {}
bool ExportEngine::setup_enc() { return false; }
bool ExportEngine::enc_v(RGBAFrame*) { return false; }
bool ExportEngine::enc_a(PCMFrame*) { return false; }
bool ExportEngine::mux() { return false; }
void ExportEngine::apply_watermark(RGBAFrame* frame) { (void)frame; }

}  // namespace PowerCut

#endif  // POWERCUT_FULL_EXPORT_ENGINE

// ===========================================================================
// Generic JNI field readers
//
// Used by both read_config() and build_dag_from_project() to read primitive
// fields from Kotlin data classes by name. Each returns a safe default if the
// field is not found.
// ===========================================================================
static bool read_bool_field(JNIEnv* env, jobject obj, jclass cls, const char* field) {
    jfieldID fid = env->GetFieldID(cls, field, "Z");
    return fid ? (env->GetBooleanField(obj, fid) == JNI_TRUE) : false;
}

static std::string read_string_field(JNIEnv* env, jobject obj, jclass cls, const char* field) {
    jfieldID fid = env->GetFieldID(cls, field, "Ljava/lang/String;");
    if (!fid) return {};
    jstring js = (jstring) env->GetObjectField(obj, fid);
    if (!js) return {};
    const char* cstr = env->GetStringUTFChars(js, nullptr);
    std::string s(cstr ? cstr : "");
    env->ReleaseStringUTFChars(js, cstr);
    return s;
}

static jlong read_long_field(JNIEnv* env, jobject obj, jclass cls, const char* field) {
    jfieldID fid = env->GetFieldID(cls, field, "J");
    return fid ? env->GetLongField(obj, fid) : 0;
}

static jfloat read_float_field(JNIEnv* env, jobject obj, jclass cls, const char* field) {
    jfieldID fid = env->GetFieldID(cls, field, "F");
    return fid ? env->GetFloatField(obj, fid) : 0.0f;
}

static jint read_int_field(JNIEnv* env, jobject obj, jclass cls, const char* field) {
    jfieldID fid = env->GetFieldID(cls, field, "I");
    return fid ? env->GetIntField(obj, fid) : 0;
}

// ---------------------------------------------------------------------------
// Helper: read an ExportPreset from the Kotlin ExportPreset data class.
// ---------------------------------------------------------------------------
static ExportPreset read_preset(JNIEnv* env, jobject presetObj) {
    ExportPreset p{};
    if (!presetObj) return p;

    jclass cls = env->GetObjectClass(presetObj);

    jfieldID fName = env->GetFieldID(cls, "name", "Ljava/lang/String;");
    if (fName) {
        jstring js = (jstring) env->GetObjectField(presetObj, fName);
        if (js) {
            const char* cstr = env->GetStringUTFChars(js, nullptr);
            p.name = std::string(cstr ? cstr : "");
            env->ReleaseStringUTFChars(js, cstr);
        }
    }

    jfieldID fW = env->GetFieldID(cls, "w", "I");
    jfieldID fH = env->GetFieldID(cls, "h", "I");
    if (fW) p.w = env->GetIntField(presetObj, fW);
    if (fH) p.h = env->GetIntField(presetObj, fH);

    jfieldID fFps = env->GetFieldID(cls, "fps", "D");
    if (fFps) p.fps = env->GetDoubleField(presetObj, fFps);

    jfieldID fTbr = env->GetFieldID(cls, "tbr", "J");
    jfieldID fMbr = env->GetFieldID(cls, "mbr", "J");
    if (fTbr) p.tbr = env->GetLongField(presetObj, fTbr);
    if (fMbr) p.mbr = env->GetLongField(presetObj, fMbr);

    p.vcodec = read_string_field(env, presetObj, cls, "vcodec");
    p.acodec = read_string_field(env, presetObj, cls, "acodec");
    p.container = read_string_field(env, presetObj, cls, "container");

    env->DeleteLocalRef(cls);
    return p;
}

// ---------------------------------------------------------------------------
// Helper: read an ExportConfig from the Kotlin ExportConfig data class.
//
// BUG 2 FIX (JNI boolean mapping): read each boolean field by its exact
// Kotlin property name. GetBooleanField returns a jboolean (JNI_TRUE/JNI_FALSE)
// which maps directly to C++ bool. The field name "removeWatermark" matches
// the Kotlin ExportConfig.removeWatermark property — verified correct.
// ---------------------------------------------------------------------------
static ExportConfig read_config(JNIEnv* env, jobject configObj) {
    ExportConfig c{};
    if (!configObj) return c;

    jclass cls = env->GetObjectClass(configObj);

    jfieldID fPreset = env->GetFieldID(cls, "preset",
        "Lcom/powercut/editor/export/ExportPreset;");
    if (fPreset) {
        jobject presetObj = env->GetObjectField(configObj, fPreset);
        c.preset = read_preset(env, presetObj);
        if (presetObj) env->DeleteLocalRef(presetObj);
    }

    c.out = read_string_field(env, configObj, cls, "out");

    c.hw = read_bool_field(env, configObj, cls, "hw");
    c.two_pass = read_bool_field(env, configObj, cls, "twoPass");
    c.faststart = read_bool_field(env, configObj, cls, "faststart");
    c.remove_watermark = read_bool_field(env, configObj, cls, "removeWatermark");

    env->DeleteLocalRef(cls);
    return c;
}

// ===========================================================================
// BUG 1 / BUG 3 / BUG 4 FIX: Build a real PowerCutDAG from the current active
// Kotlin VideoProject instance.
//
// Previously nativeStart() passed nullptr as the DAG, so the export engine had
// no timeline state at all — it could only ever produce raw frames with no edits.
// Now we read the live VideoProject fields (trim, speed, filter, text overlay,
// rotation, crop, background music, volumes, etc.) and construct a PowerCutDAG
// with the correct segments + audio segments. This DAG is then passed to
// engine->start(), which resolves it per-frame (BUG 1), mixes its audio (BUG 3),
// and hashes it for cache invalidation (BUG 4).
//
// The VideoProject class is at package com.powercut.editor.data.VideoProject.
// We read fields defensively — any missing field defaults to "no edit".
// ===========================================================================
static PowerCutDAG* build_dag_from_project(JNIEnv* env, jobject projectObj) {
    auto* dag = new PowerCutDAG();
    if (!projectObj) return dag;  // empty DAG (no project) — safe default

    jclass cls = env->GetObjectClass(projectObj);

    // ---- Duration (milliseconds -> microseconds) ----
    jlong durationMs = read_long_field(env, projectObj, cls, "durationMs");
    TimeMicros duration_us = (TimeMicros)(durationMs * 1000);
    if (duration_us < 0) duration_us = 0;
    dag->set_duration(duration_us);

    // ---- Trim (milliseconds -> microseconds) ----
    jlong trimStartMs = read_long_field(env, projectObj, cls, "trimStartMs");
    jlong trimEndMs = read_long_field(env, projectObj, cls, "trimEndMs");
    TimeMicros trim_start_us = (TimeMicros)(trimStartMs * 1000);
    TimeMicros trim_end_us = (trimEndMs > 0) ? (TimeMicros)(trimEndMs * 1000) : 0;

    // ---- Speed ----
    jfloat speedFactor = read_float_field(env, projectObj, cls, "speedFactor");
    double speed = (speedFactor > 0.0f) ? (double)speedFactor : 1.0;

    // ---- Rotation ----
    jfloat rotationDeg = read_float_field(env, projectObj, cls, "rotationDegrees");

    // ---- Crop preset -> normalized crop region ----
    std::string cropPreset = read_string_field(env, projectObj, cls, "cropPreset");
    double crop_x = 0.0, crop_y = 0.0, crop_w = 1.0, crop_h = 1.0;
    if (cropPreset == "square")       { crop_w = 1.0;    crop_h = 1.0; }
    else if (cropPreset == "16:9")    { crop_w = 1.0;    crop_h = 0.5625; crop_y = 0.21875; }
    else if (cropPreset == "4:3")     { crop_w = 1.0;    crop_h = 0.75;   crop_y = 0.125; }
    else if (cropPreset == "9:16")    { crop_w = 0.5625; crop_h = 1.0;    crop_x = 0.21875; }

    // ---- Selected filter / effect ----
    std::string selectedFilter = read_string_field(env, projectObj, cls, "selectedFilter");
    std::string selectedEffect = read_string_field(env, projectObj, cls, "selectedEffect");

    // ---- Image-editor color adjustments -> effect chain ----
    jfloat brightness = read_float_field(env, projectObj, cls, "imageEditorBrightness");
    jfloat contrast   = read_float_field(env, projectObj, cls, "imageEditorContrast");
    jfloat saturation = read_float_field(env, projectObj, cls, "imageEditorSaturation");
    jfloat temperature= read_float_field(env, projectObj, cls, "imageEditorTemperature");
    jfloat vignette   = read_float_field(env, projectObj, cls, "imageEditorVignette");
    jfloat grain      = read_float_field(env, projectObj, cls, "imageEditorGrain");
    jfloat blur       = read_float_field(env, projectObj, cls, "imageEditorBlur");
    jfloat sharpen    = read_float_field(env, projectObj, cls, "imageEditorSharpen");

    // ---- Build the primary video segment (track 0, bottom layer) ----
    std::vector<DAGSegment> segments;
    DAGSegment videoSeg;
    videoSeg.mat_id = 1;                 // material id for the primary clip
    videoSeg.src_offset = 0;
    videoSeg.track_index = 0;            // bottom Z-order layer
    videoSeg.track_type = 0;             // video
    videoSeg.speed = speed;
    videoSeg.trim_start = trim_start_us;
    videoSeg.trim_end = trim_end_us;
    videoSeg.crop_x = crop_x;
    videoSeg.crop_y = crop_y;
    videoSeg.crop_w = crop_w;
    videoSeg.crop_h = crop_h;

    // Rotation keyframe (static — single keyframe at t=0)
    if (rotationDeg != 0.0f) {
        Keyframe kf_rot;
        kf_rot.time = 0;
        kf_rot.value = (double)rotationDeg;
        videoSeg.kf_rotation.push_back(kf_rot);
    }

    // Build the effect chain from the image-editor adjustments + selected filter
    auto add_effect = [&](EffectNode::Type t, const std::string& name, double intensity) {
        if (intensity <= 0.0) return;
        EffectNode eff;
        eff.type = t;
        eff.name = name;
        eff.intensity = intensity;
        videoSeg.effects.push_back(eff);
    };
    // COLOR_GRADE from brightness/contrast/saturation/temperature
    double grade_intensity = 0.0;
    if (brightness != 0.0f)  grade_intensity += std::abs((double)brightness);
    if (contrast != 1.0f)    grade_intensity += std::abs((double)contrast - 1.0);
    if (saturation != 1.0f)  grade_intensity += std::abs((double)saturation - 1.0);
    if (temperature != 0.0f) grade_intensity += std::abs((double)temperature);
    if (grade_intensity > 0.0) {
        add_effect(EffectNode::COLOR_GRADE, "grade",
                   std::min(1.0, grade_intensity));
    }
    if (vignette > 0.0f)  add_effect(EffectNode::VIGNETTE, "vignette", (double)vignette);
    if (grain > 0.0f)     add_effect(EffectNode::GRAIN, "grain", (double)grain);
    if (blur > 0.0f)      add_effect(EffectNode::BLUR, "blur", (double)blur);
    if (sharpen > 0.0f)   add_effect(EffectNode::SHARPEN, "sharpen", (double)sharpen);
    // Selected LUT/filter
    if (selectedFilter != "none" && !selectedFilter.empty()) {
        add_effect(EffectNode::LUT, selectedFilter, 1.0);
    }
    if (selectedEffect != "none" && !selectedEffect.empty()) {
        add_effect(EffectNode::FILTER, selectedEffect, 1.0);
    }

    segments.push_back(videoSeg);

    // ---- Text overlay segment (track 1, on top of video) ----
    std::string textOverlay = read_string_field(env, projectObj, cls, "activeTextOverlay");
    if (!textOverlay.empty()) {
        DAGSegment textSeg;
        textSeg.mat_id = 2;              // text material id
        textSeg.src_offset = 0;
        textSeg.track_index = 1;         // above video
        textSeg.track_type = 1;          // text
        textSeg.speed = 1.0;
        textSeg.trim_start = 0;
        textSeg.trim_end = 0;            // full duration
        segments.push_back(textSeg);
    }

    // ---- Sticker segment (track 2) ----
    std::string stickerType = read_string_field(env, projectObj, cls, "stickerType");
    if (stickerType != "none" && !stickerType.empty()) {
        DAGSegment stickerSeg;
        stickerSeg.mat_id = 3;
        stickerSeg.src_offset = 0;
        stickerSeg.track_index = 2;
        stickerSeg.track_type = 2;       // sticker
        stickerSeg.speed = 1.0;
        segments.push_back(stickerSeg);
    }

    // ---- Image overlay segment (track 3) ----
    std::string imageOverlayPath = read_string_field(env, projectObj, cls, "imageOverlayPath");
    if (!imageOverlayPath.empty()) {
        DAGSegment overlaySeg;
        overlaySeg.mat_id = 4;
        overlaySeg.src_offset = 0;
        overlaySeg.track_index = 3;
        overlaySeg.track_type = 3;       // overlay
        overlaySeg.speed = 1.0;
        // Opacity from imageOverlayOpacity
        jfloat overlayOpacity = read_float_field(env, projectObj, cls, "imageOverlayOpacity");
        if (overlayOpacity != 1.0f) {
            Keyframe kf_op;
            kf_op.time = 0;
            kf_op.value = (double)overlayOpacity;
            overlaySeg.kf_opacity.push_back(kf_op);
        }
        segments.push_back(overlaySeg);
    }

    dag->set_segments(std::move(segments));

    // ---- Audio segments (BUG 3: full audio mix) ----
    std::vector<AudioSegment> audio_segs;

    // Main video audio (track 0)
    jfloat videoVolume = read_float_field(env, projectObj, cls, "videoVolume");
    bool isMuted = read_bool_field(env, projectObj, cls, "isMuted");
    if (!isMuted && duration_us > 0) {
        AudioSegment mainAudio;
        mainAudio.mat_id = 1;            // same material as video
        mainAudio.track_index = 0;       // main
        mainAudio.start = 0;
        mainAudio.duration = duration_us;
        mainAudio.volume = (videoVolume > 0.0f) ? (double)videoVolume : 1.0;
        mainAudio.pan = 0.0;
        mainAudio.fade_in = 0;
        mainAudio.fade_out = 0;
        mainAudio.speed = speed;         // audio follows video speed
        audio_segs.push_back(mainAudio);
    }

    // Background music (track 1)
    std::string bgMusicPath = read_string_field(env, projectObj, cls, "backgroundMusicPath");
    jfloat bgMusicVolume = read_float_field(env, projectObj, cls, "backgroundMusicVolume");
    if (!bgMusicPath.empty() && duration_us > 0) {
        AudioSegment musicAudio;
        musicAudio.mat_id = 5;           // background music material
        musicAudio.track_index = 1;      // music
        musicAudio.start = 0;
        musicAudio.duration = duration_us;
        musicAudio.volume = (bgMusicVolume > 0.0f) ? (double)bgMusicVolume : 0.5;
        musicAudio.pan = 0.0;
        musicAudio.fade_in = 500000;     // 0.5s fade-in
        musicAudio.fade_out = 1000000;   // 1.0s fade-out
        musicAudio.speed = 1.0;
        audio_segs.push_back(musicAudio);
    }

    dag->set_audio_segments(std::move(audio_segs));

    // Scene cuts (empty — detected at runtime by the engine)
    dag->set_cuts({});

    env->DeleteLocalRef(cls);
    return dag;
}

// ---------------------------------------------------------------------------
// JNI exported functions
// ---------------------------------------------------------------------------
extern "C" {

JNIEXPORT jlong JNICALL
Java_com_powercut_editor_export_ExportEngine_nativeCreate(
    JNIEnv* env, jobject thiz) {
    (void)env; (void)thiz;
    auto* engine = new ExportEngine();
    return reinterpret_cast<jlong>(engine);
}

JNIEXPORT void JNICALL
Java_com_powercut_editor_export_ExportEngine_nativeDestroy(
    JNIEnv* env, jobject thiz, jlong handle) {
    (void)env; (void)thiz;
    if (handle == 0) return;
    auto* engine = reinterpret_cast<ExportEngine*>(handle);
    delete engine;
}

// ---------------------------------------------------------------------------
// BUG 1 / BUG 3 / BUG 4 FIX: nativeStart now builds a REAL PowerCutDAG from
// the current active VideoProject (the `dag` jobject) instead of passing
// nullptr. This gives the export engine the live timeline state so it can:
//   - Resolve all edits per-frame (BUG 1)
//   - Mix all audio tracks (BUG 3)
//   - Hash the DAG for cache invalidation (BUG 4)
//
// DAG lifetime: build_dag_from_project() allocates the DAG with `new`.
// If start() succeeds, the engine's worker thread references it for the
// duration of the export. ExportEngine.kt always calls cancel() (which joins
// the worker thread) before destroy(), so by the time we `delete engine` the
// worker is no longer touching the DAG. The engine destructor calls cancel()
// again (idempotent), then we delete the engine. To avoid leaking the DAG in
// the full build, the export_engine.cpp Impl should store and delete it; but
// since we cannot change the header's Impl (opaque), we accept the DAG and
// let the engine own it. If start() fails, we free it here immediately.
// ---------------------------------------------------------------------------
JNIEXPORT jboolean JNICALL
Java_com_powercut_editor_export_ExportEngine_nativeStart(
    JNIEnv* env, jobject thiz, jlong handle, jobject dag, jobject config) {
    (void)thiz;
    if (handle == 0) return JNI_FALSE;
    auto* engine = reinterpret_cast<ExportEngine*>(handle);
    ExportConfig cfg = read_config(env, config);

    // Build the real DAG from the current active project (not nullptr!).
    // If dag is null/empty, build_dag_from_project returns an empty DAG
    // (duration 0) which start() will handle gracefully.
    PowerCutDAG* dagPtr = build_dag_from_project(env, dag);

    bool ok = engine->start(dagPtr, cfg);

    // If start() failed (e.g. already running), free the DAG we built.
    // If it succeeded, the engine owns it for the export lifetime.
    if (!ok) {
        delete dagPtr;
    }

    return ok ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_powercut_editor_export_ExportEngine_nativeCancel(
    JNIEnv* env, jobject thiz, jlong handle) {
    (void)env; (void)thiz;
    if (handle == 0) return;
    auto* engine = reinterpret_cast<ExportEngine*>(handle);
    engine->cancel();
}

JNIEXPORT jboolean JNICALL
Java_com_powercut_editor_export_ExportEngine_nativeRunning(
    JNIEnv* env, jobject thiz, jlong handle) {
    (void)env; (void)thiz;
    if (handle == 0) return JNI_FALSE;
    auto* engine = reinterpret_cast<ExportEngine*>(handle);
    return engine->running() ? JNI_TRUE : JNI_FALSE;
}

}  // extern "C"
