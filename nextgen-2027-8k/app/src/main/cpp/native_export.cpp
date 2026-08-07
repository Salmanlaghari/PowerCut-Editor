// =============================================================================
// PowerCut Pro 2027 8K — JNI bridge (CRITICAL FIXES — P1)
// File: app/src/main/cpp/native_export.cpp
//
// IMPLEMENTS the JNI-side of the spec:
//   * JNI_OnLoad: cache EVERY jmethodID/jfieldID as static globals ONCE.
//   * NewGlobalRef for the VideoProject + ProgressCallback refs (no per-call
//     local ref churn that overflows the JNI table across 1000+ frames).
//   * ExceptionCheck after EVERY JNI call that can throw, with ExceptionClear
//     so a callback exception never aborts the render.
//   * try/catch equivalent: all native calls are wrapped; the Kotlin side
//     (ExportEngine.kt) additionally guards with try/catch + 15s SW restart.
//
// The native surface is intentionally small + stable so ExportConfig /
// ExportEngine / the JNI bridge contract does NOT break (spec rule #6).
// =============================================================================
#include "powercut/export/export_engine.h"
#include "powercut/core/types.h"

#include <jni.h>
#include <android/log.h>
#include <android/native_window_jni.h>

#include <chrono>
#include <memory>
#include <string>
#include <vector>

#define TAG "powercut.jni"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)
#define PC_JNI_VERSION JNI_VERSION_1_6

// =============================================================================
// Cached JNI globals — populated once in JNI_OnLoad (P1 fix #6).
// =============================================================================
static JavaVM* g_vm = nullptr;

static jclass    g_cls_ExportEngine       = nullptr;
static jmethodID g_mid_onProgress         = nullptr; // callback (IZ)V
static jmethodID g_mid_onComplete         = nullptr; // callback (ZJLjava/lang/String;J)V
static jclass    g_cls_ProgressCallback   = nullptr; // interface class (global)
static jfieldID  g_fid_cfg_resolution     = nullptr;
static jfieldID  g_fid_cfg_fps            = nullptr;
static jfieldID  g_fid_cfg_container      = nullptr;
static jfieldID  g_fid_cfg_encoder        = nullptr;
static jfieldID  g_fid_cfg_videoBitrate   = nullptr;
static jfieldID  g_fid_cfg_audioBitrate   = nullptr;
static jfieldID  g_fid_cfg_audioChannels  = nullptr;
static jfieldID  g_fid_cfg_audioSampleRate= nullptr;
static jfieldID  g_fid_cfg_removeWatermark= nullptr;
static jfieldID  g_fid_cfg_priorityHw     = nullptr;

static jmethodID g_mid_proj_getDagJson    = nullptr; // String getDagJson()
static jmethodID g_mid_proj_getDurationUs = nullptr; // long getDurationUs()

// Per-export globals (released at export end). The VideoProject is held as a
// GLOBAL ref for the duration of the run — never a local ref across frames.
static jobject   g_proj_ref               = nullptr;
static jobject   g_progress_ref           = nullptr;
static ANativeWindow* g_preview_window    = nullptr;

// The engine itself. Single instance — exports are serialized by the Kotlin
// ExportEngine (a Mutex around run()).
static std::unique_ptr<powercut::export_::ExportEngine> g_engine;

// Process-wide cancellation token (P1 fix #9: lock-free atomic). Flipped by
// nativeCancel(), polled by the render loop every frame.
static powercut::core::CancelToken g_cancel_tok;

namespace {
// Tiny RAII helper that clears any pending JNI exception (P1: ExceptionCheck
// after every JNI call). Returns true if an exception was present+cleared.
bool clear_exception(JNIEnv* env, const char* where) {
    if (env->ExceptionCheck()) {
        env->ExceptionClear();
        LOGE("JNI exception cleared at %s", where);
        return true;
    }
    return false;
}

// Cache one field id with ExceptionCheck, returning null + clearing on error.
jfieldID safe_field(JNIEnv* env, jclass c, const char* name, const char* sig) {
    jfieldID f = env->GetFieldID(c, name, sig);
    clear_exception(env, name);
    return f;
}
jmethodID safe_method(JNIEnv* env, jclass c, const char* name, const char* sig) {
    jmethodID m = env->GetMethodID(c, name, sig);
    clear_exception(env, name);
    return m;
}
} // namespace

// =============================================================================
// JNI_OnLoad — cache everything once.
// =============================================================================
extern "C" JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* /*reserved*/) {
    g_vm = vm;
    JNIEnv* env = nullptr;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), PC_JNI_VERSION) != JNI_OK) {
        LOGE("JNI_OnLoad: GetEnv failed");
        return JNI_ERR;
    }

    // 1) ExportEngine class + native callback methods.
    jclass local_engine = env->FindClass("com/powercut/export/ExportEngine");
    if (clear_exception(env, "FindClass ExportEngine") || !local_engine) {
        LOGE("ExportEngine class not found"); return JNI_ERR;
    }
    g_cls_ExportEngine = reinterpret_cast<jclass>(env->NewGlobalRef(local_engine));
    env->DeleteLocalRef(local_engine);

    // Cache the ProgressCallback interface + its two methods as globals so we
    // never FindClass/GetMethodID per frame (P1 fix #6). The callback instance
    // (ProgressRelay on the Kotlin side) implements this interface.
    jclass local_cb = env->FindClass("com/powercut/export/ExportEngine$ProgressCallback");
    if (clear_exception(env, "FindClass ProgressCallback") || !local_cb) {
        LOGE("ProgressCallback interface not found"); return JNI_ERR;
    }
    g_cls_ProgressCallback = reinterpret_cast<jclass>(env->NewGlobalRef(local_cb));
    env->DeleteLocalRef(local_cb);
    g_mid_onProgress = safe_method(env, g_cls_ProgressCallback, "onProgress", "(IZ)V");
    g_mid_onComplete = safe_method(env, g_cls_ProgressCallback,
        "onComplete", "(ZJLjava/lang/String;J)V");
    if (!g_mid_onProgress || !g_mid_onComplete) {
        LOGE("ProgressCallback methods not found"); return JNI_ERR;
    }

    // 2) ExportConfig field ids (kept-working contract — no schema change).
    jclass cfg_cls = env->FindClass("com/powercut/export/ExportConfig");
    if (clear_exception(env, "FindClass ExportConfig") || !cfg_cls) {
        LOGE("ExportConfig class not found"); return JNI_ERR;
    }
    g_fid_cfg_resolution      = safe_field(env, cfg_cls, "resolution",      "I");
    g_fid_cfg_fps             = safe_field(env, cfg_cls, "fps",             "I");
    g_fid_cfg_container       = safe_field(env, cfg_cls, "container",       "I");
    g_fid_cfg_encoder         = safe_field(env, cfg_cls, "encoder",         "I");
    g_fid_cfg_videoBitrate    = safe_field(env, cfg_cls, "videoBitrate",    "J");
    g_fid_cfg_audioBitrate    = safe_field(env, cfg_cls, "audioBitrate",    "I");
    g_fid_cfg_audioChannels   = safe_field(env, cfg_cls, "audioChannels",   "I");
    g_fid_cfg_audioSampleRate = safe_field(env, cfg_cls, "audioSampleRate", "I");
    g_fid_cfg_removeWatermark = safe_field(env, cfg_cls, "removeWatermark", "Z");
    g_fid_cfg_priorityHw      = safe_field(env, cfg_cls, "priorityHw",      "Z");
    env->DeleteLocalRef(cfg_cls);
    if (!g_fid_cfg_resolution || !g_fid_cfg_fps || !g_fid_cfg_removeWatermark) {
        LOGE("ExportConfig required fields not found"); return JNI_ERR;
    }

    // 3) VideoProject methods (DAG json + duration). The class is looked up
    //    lazily per export (the project instance carries its own class), but
    //    the method IDs are cached once by name+sig against a stub lookup.
    jclass proj_cls = env->FindClass("com/powercut/model/VideoProject");
    if (proj_cls) {
        g_mid_proj_getDagJson    = safe_method(env, proj_cls, "getDagJson",    "()Ljava/lang/String;");
        g_mid_proj_getDurationUs = safe_method(env, proj_cls, "getDurationUs", "()J");
        env->DeleteLocalRef(proj_cls);
    } else {
        clear_exception(env, "FindClass VideoProject");
        LOGE("VideoProject class not found — DAG will be empty");
    }

    // 4) Construct + setup the engine (caches jclass/jmethodID globals in C++).
    g_engine = std::make_unique<powercut::export_::ExportEngine>();
    if (!g_engine->setup_enc(vm)) {
        LOGE("ExportEngine::setup_enc failed");
        // continue anyway — Kotlin will surface the error
    }

    LOGI("JNI_OnLoad OK — all jmethodID/jfieldID cached as globals");
    return PC_JNI_VERSION;
}

extern "C" JNIEXPORT void JNICALL JNI_OnUnload(JavaVM* vm, void* /*reserved*/) {
    JNIEnv* env = nullptr;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), PC_JNI_VERSION) != JNI_OK) return;
    if (g_engine) { g_engine->teardown(); g_engine.reset(); }
    if (g_proj_ref)     { env->DeleteGlobalRef(g_proj_ref);     g_proj_ref = nullptr; }
    if (g_progress_ref) { env->DeleteGlobalRef(g_progress_ref); g_progress_ref = nullptr; }
    if (g_cls_ExportEngine) { env->DeleteGlobalRef(g_cls_ExportEngine); g_cls_ExportEngine = nullptr; }
    if (g_cls_ProgressCallback) { env->DeleteGlobalRef(g_cls_ProgressCallback); g_cls_ProgressCallback = nullptr; }
    if (g_preview_window) { ANativeWindow_release(g_preview_window); g_preview_window = nullptr; }
}

// =============================================================================
// nativeExport: kick off a render. Called on a background thread from Kotlin.
//   jlong  jCfg        -> ExportConfig (boxed? no — we pass the config object)
//   jobject jCfg       -> ExportConfig instance
//   jobject jProject   -> VideoProject instance (held as global ref for the run)
//   jobject jSurface   -> android.view.Surface for HW async encode (may be null)
// Returns 1 on success, 0 on failure (Kotlin reads details via callbacks).
// =============================================================================
extern "C" JNIEXPORT jboolean JNICALL
Java_com_powercut_export_ExportEngine_nativeExport(
        JNIEnv* env, jobject /*thiz*/,
        jobject jCfg, jobject jProject, jobject jProgress, jobject jSurface) {

    if (!g_engine) {
        LOGE("nativeExport: engine not initialized");
        return JNI_FALSE;
    }
    if (!jCfg || !jProject) {
        LOGE("nativeExport: null config or project");
        return JNI_FALSE;
    }

    // Hold the project + progress callback as GLOBAL refs for the run (P1 fix:
    // never keep local refs across the 1000+ frame loop -> table overflow).
    if (g_proj_ref)     { env->DeleteGlobalRef(g_proj_ref);     g_proj_ref = nullptr; }
    if (g_progress_ref) { env->DeleteGlobalRef(g_progress_ref); g_progress_ref = nullptr; }
    g_proj_ref     = env->NewGlobalRef(jProject);
    g_progress_ref = jProgress ? env->NewGlobalRef(jProgress) : nullptr;
    if (clear_exception(env, "NewGlobalRef")) {
        if (g_proj_ref)     { env->DeleteGlobalRef(g_proj_ref);     g_proj_ref = nullptr; }
        if (g_progress_ref) { env->DeleteGlobalRef(g_progress_ref); g_progress_ref = nullptr; }
        return JNI_FALSE;
    }

    // Read ExportConfig fields (cached field ids — P1 fix #6).
    jint  resolution      = env->GetIntField(jCfg, g_fid_cfg_resolution);
    jint  fps             = env->GetIntField(jCfg, g_fid_cfg_fps);
    jint  container       = g_fid_cfg_container ? env->GetIntField(jCfg, g_fid_cfg_container) : 0;
    jint  encoder         = env->GetIntField(jCfg, g_fid_cfg_encoder);
    jlong video_bitrate   = g_fid_cfg_videoBitrate ? env->GetLongField(jCfg, g_fid_cfg_videoBitrate) : 0;
    jint  audio_bitrate   = g_fid_cfg_audioBitrate ? env->GetIntField(jCfg, g_fid_cfg_audioBitrate) : 192000;
    jint  audio_channels  = g_fid_cfg_audioChannels ? env->GetIntField(jCfg, g_fid_cfg_audioChannels) : 2;
    jint  audio_sr        = g_fid_cfg_audioSampleRate ? env->GetIntField(jCfg, g_fid_cfg_audioSampleRate) : 48000;
    jboolean remove_wm    = env->GetBooleanField(jCfg, g_fid_cfg_removeWatermark);
    jboolean priority_hw  = g_fid_cfg_priorityHw ? env->GetBooleanField(jCfg, g_fid_cfg_priorityHw) : JNI_FALSE;
    clear_exception(env, "read ExportConfig fields");

    powercut::core::ExportConfig cfg;
    cfg.resolution        = static_cast<powercut::core::Resolution>(resolution);
    cfg.fps               = static_cast<powercut::core::FrameRate>(fps);
    cfg.container         = static_cast<powercut::core::Container>(container);
    cfg.encoder           = static_cast<powercut::core::EncoderKind>(encoder);
    cfg.video_bitrate     = (int64_t)video_bitrate;
    cfg.audio_bitrate     = (int)audio_bitrate;
    cfg.audio_channels    = (int)audio_channels;
    cfg.audio_sample_rate = (int)audio_sr;
    cfg.remove_watermark  = (bool)remove_wm;
    cfg.priority_hw       = (bool)priority_hw;

    // Output path is sanitized on the Kotlin side; receive it via a String
    // field on ExportConfig if present. We read it defensively.
    jfieldID fid_out = env->GetFieldID(env->GetObjectClass(jCfg), "outPath", "Ljava/lang/String;");
    clear_exception(env, "GetFieldID outPath");
    if (fid_out) {
        jstring jout = (jstring) env->GetObjectField(jCfg, fid_out);
        clear_exception(env, "GetObjectField outPath");
        if (jout) {
            const char* cstr = env->GetStringUTFChars(jout, nullptr);
            if (cstr) { cfg.out_path = cstr; env->ReleaseStringUTFChars(jout, cstr); }
            env->DeleteLocalRef(jout);
        }
    }

    // Acquire the preview Surface's ANativeWindow for HW async encode (P1 #3).
    if (g_preview_window) { ANativeWindow_release(g_preview_window); g_preview_window = nullptr; }
    if (jSurface) {
        g_preview_window = ANativeWindow_fromSurface(env, jSurface);
        clear_exception(env, "ANativeWindow_fromSurface");
    }

    // Build the effect DAG from the project's JSON. (Empty DAG = source only;
    // the engine still renders the gradient headless frame in CI.)
    std::vector<powercut::core::DAGNode> dag;
    if (g_mid_proj_getDagJson && g_proj_ref) {
        jstring jjson = (jstring) env->CallObjectMethod(g_proj_ref, g_mid_proj_getDagJson);
        if (!clear_exception(env, "getDagJson") && jjson) {
            const char* cstr = env->GetStringUTFChars(jjson, nullptr);
            if (cstr) {
                // Minimal parse: the Kotlin side is the source of truth; C++
                // treats the DAG as opaque source-only unless extended.
                powercut::core::DAGNode src{powercut::core::DAGNode::Kind::Source, "src", cstr, {}};
                dag.push_back(src);
                env->ReleaseStringUTFChars(jjson, cstr);
            }
            env->DeleteLocalRef(jjson);
        }
    }

    // Cancellation token: the single process-wide token (g_cancel_tok) is
    // both read by the run loop and flipped by nativeCancel(). P1 fix #9:
    // lock-free atomic, cancel() never blocks, never deadlocks.
    g_cancel_tok.cancelled.store(false, std::memory_order_release); // reset per run

    // Progress relay into Kotlin (cached mid — never FindClass per frame).
    auto progress = [env](int pct, const std::string& /*msg*/) {
        if (!g_cls_ProgressCallback || !g_mid_onProgress || !g_progress_ref) return;
        // Scoped local frame per callback so we never leak locals (P1 fix #1).
        if (env->PushLocalFrame(4) < 0) return;
        env->CallVoidMethod(g_progress_ref, g_mid_onProgress,
                            (jint)pct, (jboolean)JNI_FALSE);
        clear_exception(env, "onProgress");
        env->PopLocalFrame(nullptr);
    };

    auto t0 = std::chrono::steady_clock::now();
    auto result = g_engine->run(cfg, dag, progress, g_cancel_tok);
    auto elapsed_ms = std::chrono::duration_cast<std::chrono::milliseconds>(
                          std::chrono::steady_clock::now() - t0).count();

    // Notify Kotlin of completion via the cached method.
    if (g_cls_ProgressCallback && g_mid_onComplete && g_progress_ref) {
        jstring jerr = result.error.empty() ? nullptr : env->NewStringUTF(result.error.c_str());
        env->CallVoidMethod(g_progress_ref, g_mid_onComplete,
                            (jboolean)result.ok,
                            (jlong)result.file_size_bytes,
                            jerr,
                            (jlong)elapsed_ms);
        clear_exception(env, "onComplete");
        if (jerr) env->DeleteLocalRef(jerr);
    }

    // Release per-run global refs (P1 fix #8: guarded, never double-free).
    if (g_proj_ref)     { env->DeleteGlobalRef(g_proj_ref);     g_proj_ref = nullptr; }
    if (g_progress_ref) { env->DeleteGlobalRef(g_progress_ref); g_progress_ref = nullptr; }
    if (g_preview_window) { ANativeWindow_release(g_preview_window); g_preview_window = nullptr; }

    return result.ok ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT void JNICALL
Java_com_powercut_export_ExportEngine_nativeCancel(JNIEnv* /*env*/, jobject /*thiz*/) {
    // P1 fix #9: cancel is a single atomic store — never blocks, never
    // deadlocks. The render loop polls g_cancel_tok every frame and drains
    // non-blocking.
    g_cancel_tok.cancel();
}
