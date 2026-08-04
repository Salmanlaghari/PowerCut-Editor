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
#include "powercut/export/export_engine.h"
#include "powercut/core/dag.h"

using PowerCut::ExportEngine;
using PowerCut::ExportConfig;
using PowerCut::ExportPreset;
using PowerCut::PowerCutDAG;

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

// ---------------------------------------------------------------------------
// Helper: read an ExportPreset from the Kotlin ExportPreset data class.
// ---------------------------------------------------------------------------
static ExportPreset read_preset(JNIEnv* env, jobject presetObj) {
    ExportPreset p{};
    if (!presetObj) return p;

    jclass cls = env->GetObjectClass(presetObj);

    jfieldId fName = env->GetFieldID(cls, "name", "Ljava/lang/String;");
    if (fName) {
        jstring js = (jstring) env->GetObjectField(presetObj, fName);
        if (js) {
            const char* cstr = env->GetStringUTFChars(js, nullptr);
            p.name = std::string(cstr ? cstr : "");
            env->ReleaseStringUTFChars(js, cstr);
        }
    }

    jfieldId fW = env->GetFieldID(cls, "w", "I");
    jfieldId fH = env->GetFieldID(cls, "h", "I");
    if (fW) p.w = env->GetIntField(presetObj, fW);
    if (fH) p.h = env->GetIntField(presetObj, fH);

    jfieldId fFps = env->GetFieldID(cls, "fps", "D");
    if (fFps) p.fps = env->GetDoubleField(presetObj, fFps);

    jfieldId fTbr = env->GetFieldID(cls, "tbr", "J");
    jfieldId fMbr = env->GetFieldID(cls, "mbr", "J");
    if (fTbr) p.tbr = env->GetLongField(presetObj, fTbr);
    if (fMbr) p.mbr = env->GetLongField(presetObj, fMbr);

    auto read_str = [&](const char* field) -> std::string {
        jfieldId fid = env->GetFieldID(cls, field, "Ljava/lang/String;");
        if (!fid) return {};
        jstring js = (jstring) env->GetObjectField(presetObj, fid);
        if (!js) return {};
        const char* cstr = env->GetStringUTFChars(js, nullptr);
        std::string s(cstr ? cstr : "");
        env->ReleaseStringUTFChars(js, cstr);
        return s;
    };
    p.vcodec = read_str("vcodec");
    p.acodec = read_str("acodec");
    p.container = read_str("container");

    env->DeleteLocalRef(cls);
    return p;
}

// ---------------------------------------------------------------------------
// Helper: read an ExportConfig from the Kotlin ExportConfig data class.
// ---------------------------------------------------------------------------
static ExportConfig read_config(JNIEnv* env, jobject configObj) {
    ExportConfig c{};
    if (!configObj) return c;

    jclass cls = env->GetObjectClass(configObj);

    jfieldId fPreset = env->GetFieldID(cls, "preset",
        "Lcom/powercut/editor/export/ExportPreset;");
    if (fPreset) {
        jobject presetObj = env->GetObjectField(configObj, fPreset);
        c.preset = read_preset(env, presetObj);
        if (presetObj) env->DeleteLocalRef(presetObj);
    }

    jfieldId fOut = env->GetFieldID(cls, "out", "Ljava/lang/String;");
    if (fOut) {
        jstring js = (jstring) env->GetObjectField(configObj, fOut);
        if (js) {
            const char* cstr = env->GetStringUTFChars(js, nullptr);
            c.out = std::string(cstr ? cstr : "");
            env->ReleaseStringUTFChars(js, cstr);
        }
    }

    auto read_bool = [&](const char* field) -> bool {
        jfieldId fid = env->GetFieldID(cls, field, "Z");
        return fid ? env->GetBooleanField(configObj, fid) : false;
    };
    c.hw = read_bool("hw");
    c.two_pass = read_bool("twoPass");
    c.faststart = read_bool("faststart");
    c.remove_watermark = read_bool("removeWatermark");

    env->DeleteLocalRef(cls);
    return c;
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

JNIEXPORT jboolean JNICALL
Java_com_powercut_editor_export_ExportEngine_nativeStart(
    JNIEnv* env, jobject thiz, jlong handle, jobject dag, jobject config) {
    (void)thiz; (void)dag;
    if (handle == 0) return JNI_FALSE;
    auto* engine = reinterpret_cast<ExportEngine*>(handle);
    ExportConfig cfg = read_config(env, config);
    PowerCutDAG* dagPtr = nullptr;
    bool ok = engine->start(dagPtr, cfg);
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
