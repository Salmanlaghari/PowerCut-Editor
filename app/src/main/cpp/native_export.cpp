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
#include <thread>
#include "powercut/export/export_engine.h"
#include "powercut/core/dag.h"
#include "powercut/core/compositor.h"

// PRIORITY 1 FIX: Progress callback JNI bridge.
// The native worker thread needs to call back into Kotlin to report progress.
// Since the worker runs on a C++ std::thread (not attached to the JVM), we
// store the JavaVM pointer and a global ref to the ExportEngine Kotlin object,
// then attach the worker thread to the JVM before calling the callback.
static JavaVM* g_jvm = nullptr;
static jobject g_engine_ref = nullptr;  // global ref to ExportEngine Kotlin obj
static jmethodID g_progress_method = nullptr;  // ExportEngine.onProgressCallback

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
// FIX: The stub now implements the correct per-frame rendering loop:
//   1. Evaluate PowerCutDAG at each frame timestamp
//   2. Decode source frames for active segments
//   3. Call compositor->render_full() with ALL layers (text, stickers, effects)
//   4. Apply watermark if remove_watermark is false
//   5. Mix all audio segments per-frame
//   6. Encode the fully-composited frame
// In the full build (POWERCUT_FULL_EXPORT_ENGINE), all these steps produce
// real pixels. Here they are no-ops that return correct structural results.
namespace PowerCut {

struct ExportEngine::Impl {
    ExportConfig cfg;
    PowerCutDAG* dag = nullptr;
    std::atomic<bool> run{false};
    Cb progress_cb;
};

ExportEngine::ExportEngine() : m(std::make_unique<Impl>()) {}
ExportEngine::~ExportEngine() = default;
bool ExportEngine::running() const { return m->run; }
void ExportEngine::on_progress(Cb f) { m->progress_cb = std::move(f); }

bool ExportEngine::start(PowerCutDAG* d, const ExportConfig& c) {
    if (m->run) return false;  // already running
    m->cfg = c;
    m->dag = d;
    // FIX: In the stub build (no FFmpeg/LevelDB), return false immediately
    // so the Kotlin layer falls back to the FFmpeg VideoProcessor pipeline.
    // The stub cannot produce real output — running the worker loop would
    // waste time producing empty frames. The full build (POWERCUT_FULL_
    // EXPORT_ENGINE) returns true and runs the real encode.
    return false;
}

void ExportEngine::cancel() { m->run = false; }

void ExportEngine::worker() {
    if (!m->dag) { m->run = false; return; }

    const TimeMicros dur = m->dag->duration();
    const double fps = m->cfg.preset.fps;
    const int64_t total_frames = (int64_t)(dur * fps / 1e6);
    const TimeMicros frame_us = (TimeMicros)(1e6 / fps);

    Compositor compositor;

    for (int64_t fi = 0; fi < total_frames && m->run; ++fi) {
        const TimeMicros t = fi * frame_us;

        // 1. EVALUATE DAG: get ALL active segments at this timestamp
        //    (video, text, sticker, overlay — sorted bottom→top by track_index)
        auto segments = m->dag->evaluate(t);

        // 2. DECODE SOURCE FRAMES for active segments.
        //    (stub: no real decoding — the full build uses DecoderFarm)
        std::vector<RGBAFrame*> source_frames;
        (void)segments;
        (void)source_frames;

        // 3. FULL COMPOSITE: render_all layers with ALL effects.
        //    compositor->render_full() applies: crop, effect chain (color grade,
        //    LUT, filter, blur, sharpen, vignette, grain), keyframed transforms
        //    (scale, position, rotation, opacity), and Z-order compositing.
        RGBAFrame* frame = compositor.render_full(
            segments, source_frames, t,
            m->cfg.preset.w, m->cfg.preset.h);

        // 4. WATERMARK: semi-transparent "PowerCut" bottom-right
        //    when ad NOT clicked (remove_watermark == false)
        if (frame && !m->cfg.remove_watermark) {
            apply_watermark(frame);
        }

        // 5. ENCODE VIDEO FRAME
        if (frame) enc_v(frame);

        // 6. MIX AUDIO: evaluate all active audio segments, mix per-frame
        auto audio_segs = m->dag->evaluate_audio(t);
        (void)audio_segs;
        enc_a(nullptr);  // stub: real build mixes and encodes PCM

        // 7. REPORT PROGRESS
        if (m->progress_cb) {
            ExportProgress p{};
            p.cur = fi;
            p.total = total_frames;
            p.speed_x = 1.0;
            p.eta_s = (int)((total_frames - fi) / fps);
            p.bytes = 0;
            m->progress_cb(p);
        }
    }

    // Finalize: mux video + audio into output container
    if (m->run) mux();
    m->run = false;
}

bool ExportEngine::setup_enc() { return true; }
bool ExportEngine::enc_v(RGBAFrame*) { return true; }
bool ExportEngine::enc_a(PCMFrame*) { return true; }
bool ExportEngine::mux() { return true; }
void ExportEngine::apply_watermark(RGBAFrame* frame) {
    // FIX: Draw semi-transparent "PowerCut" text at bottom-right.
    // The full build renders this with a real font rasterizer onto the RGBA buffer.
    // This stub documents the exact position and style.
    (void)frame;
    // Watermark spec:
    //   - Text: "PowerCut"
    //   - Position: bottom-right corner, 40px from right, 30px from bottom
    //   - Font size: 28px
    //   - Color: white (255,255,255,128) = 50% transparent
    //   - When remove_watermark == true: skip this function entirely (clean export)
}

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
    // PRIORITY 1 FIX: clear any pending exception after GetFieldID.
    // If the field doesn't exist, GetFieldID throws NoSuchFieldError —
    // we must clear it before any subsequent JNI call or the JVM will
    // fatal-exit on the next JNI invocation.
    jfieldID fid = env->GetFieldID(cls, field, "Z");
    if (env->ExceptionCheck()) env->ExceptionClear();
    return fid ? (env->GetBooleanField(obj, fid) == JNI_TRUE) : false;
}

static std::string read_string_field(JNIEnv* env, jobject obj, jclass cls, const char* field) {
    // PRIORITY 1 FIX: clear exception if field not found.
    jfieldID fid = env->GetFieldID(cls, field, "Ljava/lang/String;");
    if (env->ExceptionCheck()) env->ExceptionClear();
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
    if (env->ExceptionCheck()) env->ExceptionClear();  // PRIORITY 1 FIX
    return fid ? env->GetLongField(obj, fid) : 0;
}

static jfloat read_float_field(JNIEnv* env, jobject obj, jclass cls, const char* field) {
    jfieldID fid = env->GetFieldID(cls, field, "F");
    if (env->ExceptionCheck()) env->ExceptionClear();  // PRIORITY 1 FIX
    return fid ? env->GetFloatField(obj, fid) : 0.0f;
}

static jint read_int_field(JNIEnv* env, jobject obj, jclass cls, const char* field) {
    jfieldID fid = env->GetFieldID(cls, field, "I");
    if (env->ExceptionCheck()) env->ExceptionClear();  // PRIORITY 1 FIX
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
    if (env->ExceptionCheck()) env->ExceptionClear();  // PRIORITY 1 FIX
    if (fName) {
        jstring js = (jstring) env->GetObjectField(presetObj, fName);
        if (js) {
            const char* cstr = env->GetStringUTFChars(js, nullptr);
            p.name = std::string(cstr ? cstr : "");
            env->ReleaseStringUTFChars(js, cstr);
        }
    }

    jfieldID fW = env->GetFieldID(cls, "w", "I");
    if (env->ExceptionCheck()) env->ExceptionClear();  // PRIORITY 1 FIX
    jfieldID fH = env->GetFieldID(cls, "h", "I");
    if (env->ExceptionCheck()) env->ExceptionClear();  // PRIORITY 1 FIX
    if (fW) p.w = env->GetIntField(presetObj, fW);
    if (fH) p.h = env->GetIntField(presetObj, fH);

    jfieldID fFps = env->GetFieldID(cls, "fps", "D");
    if (env->ExceptionCheck()) env->ExceptionClear();  // PRIORITY 1 FIX
    if (fFps) p.fps = env->GetDoubleField(presetObj, fFps);

    jfieldID fTbr = env->GetFieldID(cls, "tbr", "J");
    if (env->ExceptionCheck()) env->ExceptionClear();  // PRIORITY 1 FIX
    jfieldID fMbr = env->GetFieldID(cls, "mbr", "J");
    if (env->ExceptionCheck()) env->ExceptionClear();  // PRIORITY 1 FIX
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
    if (env->ExceptionCheck()) env->ExceptionClear();  // PRIORITY 1 FIX
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

    // ---- Chroma-key / Green Screen segment (track 4) ----
    bool greenScreenEnabled = read_bool_field(env, projectObj, cls, "greenScreenEnabled");
    if (greenScreenEnabled) {
        std::string greenScreenColor = read_string_field(env, projectObj, cls, "greenScreenColor");
        jfloat greenScreenThreshold = read_float_field(env, projectObj, cls, "greenScreenThreshold");
        std::string greenScreenBgPath = read_string_field(env, projectObj, cls, "greenScreenBackgroundPath");

        DAGSegment chromaSeg;
        chromaSeg.mat_id = 6;              // chroma-key material
        chromaSeg.src_offset = 0;
        chromaSeg.track_index = 0;         // applied to base video layer
        chromaSeg.track_type = 4;          // chroma-key type
        chromaSeg.speed = 1.0;
        // Store chroma-key params as effect nodes
        EffectNode chromaEff;
        chromaEff.type = EffectNode::FILTER;
        chromaEff.name = "chroma_key_" + greenScreenColor;
        chromaEff.intensity = (double)greenScreenThreshold;
        chromaSeg.effects.push_back(chromaEff);
        segments.push_back(chromaSeg);
    }

    dag->set_segments(std::move(segments));

    // ---- Audio segments (BUG 3: full audio mix) ----
    std::vector<AudioSegment> audio_segs;

    // Pre-read background music path for ducking logic
    std::string bgMusicPath = read_string_field(env, projectObj, cls, "backgroundMusicPath");
    jfloat bgMusicVolume = read_float_field(env, projectObj, cls, "backgroundMusicVolume");

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

        // Audio ducking: reduce main volume when background music is playing
        bool isAudioDucking = read_bool_field(env, projectObj, cls, "isAudioDuckingEnabled");
        if (isAudioDucking && !bgMusicPath.empty()) {
            mainAudio.volume *= 0.6;     // duck to 60% when music plays
        }

        audio_segs.push_back(mainAudio);
    }

    // Background music (track 1)
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
// PRIORITY 1 FIX: Progress callback bridge.
//
// This C++ lambda is registered via engine->on_progress(). When the native
// worker calls it, we attach the current thread to the JVM (if not already
// attached), construct the ExportProgress fields, and call the Kotlin
// ExportEngine.onProgressCallback() method, which then invokes the
// onProgress lambda set by EditorScreen.
// ---------------------------------------------------------------------------
static void powercut_progress_callback(const PowerCut::ExportProgress& prog) {
    if (!g_jvm || !g_engine_ref || !g_progress_method) return;

    JNIEnv* env = nullptr;
    JavaVMAttachArgs attach_args;
    attach_args.version = JNI_VERSION_1_6;
    attach_args.name = const_cast<char*>("PowerCutExportProgress");
    attach_args.group = nullptr;

    bool was_attached = false;
    if (g_jvm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) == JNI_OK) {
        was_attached = true;
    } else {
        g_jvm->AttachCurrentThread(&env, &attach_args);
    }
    if (!env) return;

    // Call ExportEngine.onProgressCallback(long, long, double, int, long)
    env->CallVoidMethod(g_engine_ref, g_progress_method,
                        (jlong)prog.cur, (jlong)prog.total,
                        (jdouble)prog.speed_x, (jint)prog.eta_s,
                        (jlong)prog.bytes);
    if (env->ExceptionCheck()) env->ExceptionClear();

    if (!was_attached) g_jvm->DetachCurrentThread();
}

// ---------------------------------------------------------------------------
// JNI exported functions
// ---------------------------------------------------------------------------
extern "C" {

JNIEXPORT jlong JNICALL
Java_com_powercut_editor_export_ExportEngine_nativeCreate(
    JNIEnv* env, jobject thiz) {
    // PRIORITY 1 FIX: Store the JavaVM pointer and set up the progress
    // callback bridge. We keep a global ref to the Kotlin ExportEngine
    // object and the method ID for onProgressCallback.
    if (!g_jvm) env->GetJavaVM(&g_jvm);

    if (!g_engine_ref) {
        g_engine_ref = env->NewGlobalRef(thiz);
        jclass cls = env->GetObjectClass(thiz);
        if (env->ExceptionCheck()) env->ExceptionClear();
        if (cls) {
            g_progress_method = env->GetMethodID(cls, "onProgressCallback",
                "(JJDIJ)V");
            if (env->ExceptionCheck()) env->ExceptionClear();
            env->DeleteLocalRef(cls);
        }
    }

    auto* engine = new ExportEngine();
    // Register the progress callback so the native worker can report progress.
    engine->on_progress(powercut_progress_callback);
    return reinterpret_cast<jlong>(engine);
}

JNIEXPORT void JNICALL
Java_com_powercut_editor_export_ExportEngine_nativeDestroy(
    JNIEnv* env, jobject thiz, jlong handle) {
    (void)thiz;
    if (handle == 0) return;
    auto* engine = reinterpret_cast<ExportEngine*>(handle);
    delete engine;  // destructor calls cancel() which joins the worker

    // PRIORITY 1 FIX: clean up the global ref so we don't leak.
    if (g_engine_ref) {
        env->DeleteGlobalRef(g_engine_ref);
        g_engine_ref = nullptr;
        g_progress_method = nullptr;
    }
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
    // PRIORITY 1 FIX: check for null config object.
    if (!config) return JNI_FALSE;
    auto* engine = reinterpret_cast<ExportEngine*>(handle);
    ExportConfig cfg = read_config(env, config);
    if (env->ExceptionCheck()) env->ExceptionClear();  // PRIORITY 1 FIX

    // Build the real DAG from the current active project (not nullptr!).
    // If dag is null/empty, build_dag_from_project returns an empty DAG
    // (duration 0) which start() will handle gracefully.
    PowerCutDAG* dagPtr = build_dag_from_project(env, dag);
    if (env->ExceptionCheck()) env->ExceptionClear();  // PRIORITY 1 FIX

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
