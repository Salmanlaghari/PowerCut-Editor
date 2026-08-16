// =============================================================================
// PowerCut Editor — JNI bridge for the native C++ Export Engine (P1 fixes).
//
// IMPLEMENTS:
//   * JNI_OnLoad: cache EVERY jclass/jmethodID/jfieldID as static globals ONCE
//     using NewGlobalRef (P1 fix #6 — no FindClass/GetMethodID per frame).
//   * nativeExport: bridges Kotlin VideoProject → C++ powercut::export_::ExportConfig
//     → export_engine.run() with progress callbacks and cancellation.
//   * Legacy JNI symbols (nativeCreate/nativeStart/etc.) are preserved as stubs
//     for binary compatibility with any compiled callers.
//
// JNI symbol naming: Java_com_powercut_editor_export_ExportEngine_<method>
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
static jmethodID g_mid_onProgress         = nullptr;
static jmethodID g_mid_onComplete         = nullptr;
static jclass    g_cls_ProgressCallback   = nullptr;

static jfieldID  g_fid_cfg_preset           = nullptr;
static jfieldID  g_fid_cfg_outPath          = nullptr;

static jfieldID  g_fid_preset_w             = nullptr;
static jfieldID  g_fid_preset_h             = nullptr;
static jfieldID  g_fid_preset_fps           = nullptr;
static jfieldID  g_fid_preset_container     = nullptr;

static jmethodID g_mid_proj_toJson        = nullptr;
static jmethodID g_mid_proj_getDurationMs = nullptr;

static jobject   g_proj_ref               = nullptr;
static jobject   g_progress_ref           = nullptr;
static ANativeWindow* g_preview_window    = nullptr;

static std::unique_ptr<powercut::export_::ExportEngine> g_engine;
static powercut::core::CancelToken g_cancel_tok;

namespace {
bool clear_exception(JNIEnv* env, const char* where) {
    if (env->ExceptionCheck()) {
        env->ExceptionClear();
        LOGE("JNI exception cleared at %s", where);
        return true;
    }
    return false;
}

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
}

// =============================================================================
// JNI_OnLoad — cache everything once via NewGlobalRef (P1 fix #6).
// =============================================================================
extern "C" JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* /*reserved*/) {
    g_vm = vm;
    JNIEnv* env = nullptr;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), PC_JNI_VERSION) != JNI_OK) {
        LOGE("JNI_OnLoad: GetEnv failed");
        return JNI_ERR;
    }

    jclass local_engine = env->FindClass("com/powercut/editor/export/ExportEngine");
    if (clear_exception(env, "FindClass ExportEngine") || !local_engine) {
        LOGE("ExportEngine class not found"); return JNI_ERR;
    }
    g_cls_ExportEngine = reinterpret_cast<jclass>(env->NewGlobalRef(local_engine));
    env->DeleteLocalRef(local_engine);

    jclass local_cb = env->FindClass("com/powercut/editor/export/ExportEngine$ProgressCallback");
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

    jclass cfg_cls = env->FindClass("com/powercut/editor/export/ExportConfig");
    if (clear_exception(env, "FindClass ExportConfig") || !cfg_cls) {
        LOGE("ExportConfig class not found"); return JNI_ERR;
    }
    g_fid_cfg_preset     = safe_field(env, cfg_cls, "preset", "Lcom/powercut/editor/export/ExportPreset;");
    g_fid_cfg_outPath    = safe_field(env, cfg_cls, "out", "Ljava/lang/String;");
    env->DeleteLocalRef(cfg_cls);

    jclass preset_cls = env->FindClass("com/powercut/editor/export/ExportPreset");
    if (preset_cls) {
        g_fid_preset_w         = safe_field(env, preset_cls, "w", "I");
        g_fid_preset_h         = safe_field(env, preset_cls, "h", "I");
        g_fid_preset_fps       = safe_field(env, preset_cls, "fps", "D");
        g_fid_preset_container = safe_field(env, preset_cls, "container", "Ljava/lang/String;");
        env->DeleteLocalRef(preset_cls);
    }

    jclass proj_cls = env->FindClass("com/powercut/editor/data/VideoProject");
    if (proj_cls) {
        g_mid_proj_toJson        = safe_method(env, proj_cls, "toJson", "()Lorg/json/JSONObject;");
        g_mid_proj_getDurationMs = safe_method(env, proj_cls, "getDurationMs", "()J");
        env->DeleteLocalRef(proj_cls);
    } else {
        clear_exception(env, "FindClass VideoProject");
        LOGE("VideoProject class not found — DAG will use project fields");
    }

    g_engine = std::make_unique<powercut::export_::ExportEngine>();
    if (!g_engine->setup_enc(vm)) {
        LOGE("ExportEngine::setup_enc failed — continuing, Kotlin will surface errors");
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
    if (g_cls_ExportEngine)     { env->DeleteGlobalRef(g_cls_ExportEngine);     g_cls_ExportEngine = nullptr; }
    if (g_cls_ProgressCallback) { env->DeleteGlobalRef(g_cls_ProgressCallback); g_cls_ProgressCallback = nullptr; }
    if (g_preview_window) { ANativeWindow_release(g_preview_window); g_preview_window = nullptr; }
}

// =============================================================================
// Helper: convert old ExportPreset fields to new powercut::core::ExportConfig
// =============================================================================
static powercut::core::Resolution preset_resolution(const std::string& targetRes, int w, int h) {
    int res = (w > 2160) ? 2160 : (w > 1440 ? 1440 : (w > 1080 ? 1080 : (w > 720 ? 720 : 480)));
    if (targetRes == "4k" || targetRes == "2160p") res = 2160;
    else if (targetRes == "2k" || targetRes == "1440p") res = 1440;
    else if (targetRes == "1080p" || targetRes == "fhd") res = 1080;
    else if (targetRes == "720p" || targetRes == "hd") res = 720;
    else if (targetRes == "480p" || targetRes == "sd") res = 480;
    return static_cast<powercut::core::Resolution>(res);
}

static powercut::core::FrameRate preset_fps(int fps) {
    if (fps >= 120) return powercut::core::FrameRate::FPS120;
    if (fps >= 60)  return powercut::core::FrameRate::FPS60;
    if (fps >= 25)  return powercut::core::FrameRate::FPS30;
    return powercut::core::FrameRate::FPS24;
}

static powercut::core::Container preset_container(const std::string& c) {
    if (c == "mov")  return powercut::core::Container::MOV;
    if (c == "webm") return powercut::core::Container::WEBM;
    return powercut::core::Container::MP4;
}

static int64_t preset_bitrate(int w, int h, int fps) {
    double pixels = (double)w * h * fps;
    if (pixels > 33'000'000.0)  return 45'000'000;
    if (pixels > 8'000'000.0)   return 20'000'000;
    if (pixels > 2'000'000.0)   return 12'000'000;
    return 4'000'000;
}

// =============================================================================
// nativeExport: bridges Kotlin VideoProject + ExportConfig → new engine.run()
// =============================================================================
extern "C" JNIEXPORT jboolean JNICALL
Java_com_powercut_editor_export_ExportEngine_nativeExport(
        JNIEnv* env, jobject /*thiz*/,
        jobject jConfig, jobject jProject, jobject jProgress, jobject jSurface) {

    if (!g_engine) {
        LOGE("nativeExport: engine not initialized");
        return JNI_FALSE;
    }
    if (!jConfig || !jProject) {
        LOGE("nativeExport: null config or project");
        return JNI_FALSE;
    }

    if (g_proj_ref)     { env->DeleteGlobalRef(g_proj_ref);     g_proj_ref = nullptr; }
    if (g_progress_ref) { env->DeleteGlobalRef(g_progress_ref); g_progress_ref = nullptr; }
    g_proj_ref     = env->NewGlobalRef(jProject);
    g_progress_ref = jProgress ? env->NewGlobalRef(jProgress) : nullptr;
    if (clear_exception(env, "NewGlobalRef")) {
        if (g_proj_ref)     { env->DeleteGlobalRef(g_proj_ref);     g_proj_ref = nullptr; }
        if (g_progress_ref) { env->DeleteGlobalRef(g_progress_ref); g_progress_ref = nullptr; }
        return JNI_FALSE;
    }

    jclass configClass = env->GetObjectClass(jConfig);
    jobject presetObj  = nullptr;

    presetObj = env->GetObjectField(jConfig, g_fid_cfg_preset);
    if (configClass) env->DeleteLocalRef(configClass);

    int presetW = 1920, presetH = 1080, presetFps = 30;
    std::string presetContainer = "mp4";
    if (presetObj) {
        jclass presetClass = env->GetObjectClass(presetObj);
        if (presetClass) {
            if (g_fid_preset_w)         presetW = env->GetIntField(presetObj, g_fid_preset_w);
            if (g_fid_preset_h)         presetH = env->GetIntField(presetObj, g_fid_preset_h);
            if (g_fid_preset_fps)       presetFps = (int)env->GetDoubleField(presetObj, g_fid_preset_fps);
            if (g_fid_preset_container) {
                jstring jCont = (jstring)env->GetObjectField(presetObj, g_fid_preset_container);
                if (jCont) {
                    const char* cstr = env->GetStringUTFChars(jCont, nullptr);
                    if (cstr) { presetContainer = cstr; env->ReleaseStringUTFChars(jCont, cstr); }
                    env->DeleteLocalRef(jCont);
                }
            }
            env->DeleteLocalRef(presetClass);
        }
        env->DeleteLocalRef(presetObj);
    }

    std::string targetResolution = "1080p";
    jclass projClass = env->GetObjectClass(jProject);
    if (projClass) {
        jfieldID fTargetRes = env->GetFieldID(projClass, "targetResolution", "Ljava/lang/String;");
        if (fTargetRes) {
            jstring jTR = (jstring)env->GetObjectField(jProject, fTargetRes);
            if (jTR) {
                const char* cstr = env->GetStringUTFChars(jTR, nullptr);
                if (cstr) { targetResolution = cstr; env->ReleaseStringUTFChars(jTR, cstr); }
                env->DeleteLocalRef(jTR);
            }
        }
        clear_exception(env, "read targetResolution");
        env->DeleteLocalRef(projClass);
    }

    powercut::core::ExportConfig cfg;
    cfg.resolution     = preset_resolution(targetResolution, presetW, presetH);
    cfg.fps            = preset_fps(presetFps);
    cfg.container      = preset_container(presetContainer);
    cfg.encoder        = powercut::core::EncoderKind::AUTO;
    cfg.video_bitrate  = preset_bitrate(presetW, presetH, presetFps);
    cfg.audio_bitrate  = 192000;
    cfg.audio_channels = 2;
    cfg.audio_sample_rate = 48000;
    cfg.remove_watermark = false;
    cfg.priority_hw    = false;

    if (g_fid_cfg_outPath) {
        jstring jout = (jstring)env->GetObjectField(jConfig, g_fid_cfg_outPath);
        if (jout) {
            const char* cstr = env->GetStringUTFChars(jout, nullptr);
            if (cstr) { cfg.out_path = cstr; env->ReleaseStringUTFChars(jout, cstr); }
            env->DeleteLocalRef(jout);
        }
    }
    clear_exception(env, "read outPath");

    if (g_preview_window) { ANativeWindow_release(g_preview_window); g_preview_window = nullptr; }
    if (jSurface) {
        g_preview_window = ANativeWindow_fromSurface(env, jSurface);
        clear_exception(env, "ANativeWindow_fromSurface");
    }

    std::vector<powercut::core::DAGNode> dag;
    jstring jjson = nullptr;
    if (g_mid_proj_toJson && g_proj_ref) {
        jjson = (jstring)env->CallObjectMethod(g_proj_ref, g_mid_proj_toJson);
        clear_exception(env, "toJson");
    }
    if (jjson) {
        const char* cstr = env->GetStringUTFChars(jjson, nullptr);
        if (cstr) {
            powercut::core::DAGNode src{
                powercut::core::DAGNode::Kind::Source, "project", cstr, {}
            };
            dag.push_back(src);
            env->ReleaseStringUTFChars(jjson, cstr);
        }
        env->DeleteLocalRef(jjson);
    }

    g_cancel_tok.cancelled.store(false, std::memory_order_release);

    auto progress = [env](int pct, const std::string& /*msg*/) {
        if (!g_cls_ProgressCallback || !g_mid_onProgress || !g_progress_ref) return;
        if (env->PushLocalFrame(2) < 0) return;
        env->CallVoidMethod(g_progress_ref, g_mid_onProgress,
                            (jint)pct, (jboolean)JNI_FALSE);
        clear_exception(env, "onProgress");
        env->PopLocalFrame(nullptr);
    };

    auto t0 = std::chrono::steady_clock::now();
    auto result = g_engine->run(cfg, dag, progress, g_cancel_tok);
    auto elapsed_ms = std::chrono::duration_cast<std::chrono::milliseconds>(
                          std::chrono::steady_clock::now() - t0).count();

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

    if (g_proj_ref)     { env->DeleteGlobalRef(g_proj_ref);     g_proj_ref = nullptr; }
    if (g_progress_ref) { env->DeleteGlobalRef(g_progress_ref); g_progress_ref = nullptr; }
    if (g_preview_window) { ANativeWindow_release(g_preview_window); g_preview_window = nullptr; }

    return result.ok ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT void JNICALL
Java_com_powercut_editor_export_ExportEngine_nativeCancel(
        JNIEnv* /*env*/, jobject /*thiz*/) {
    g_cancel_tok.cancel();
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_powercut_editor_export_ExportEngine_nativeIsAvailable(
        JNIEnv* /*env*/, jobject /*thiz*/) {
    return g_engine ? JNI_TRUE : JNI_FALSE;
}

// =============================================================================
// Legacy JNI symbols — preserved as stubs for binary compatibility.
// The new code path uses nativeExport/nativeCancel exclusively.
// =============================================================================
extern "C" {

JNIEXPORT jlong JNICALL
Java_com_powercut_editor_export_ExportEngine_nativeCreate(JNIEnv* env, jobject thiz) {
    if (!g_vm) env->GetJavaVM(&g_vm);
    g_engine = std::make_unique<powercut::export_::ExportEngine>();
    g_engine->setup_enc(g_vm);
    return 1;
}

JNIEXPORT void JNICALL
Java_com_powercut_editor_export_ExportEngine_nativeDestroy(
        JNIEnv* env, jobject thiz, jlong handle) {
    (void)env; (void)thiz; (void)handle;
    if (g_engine) { g_engine->teardown(); g_engine.reset(); }
}

JNIEXPORT jboolean JNICALL
Java_com_powercut_editor_export_ExportEngine_nativeStart(
        JNIEnv* env, jobject thiz, jlong handle, jobject dag, jobject config) {
    (void)env; (void)thiz; (void)handle; (void)dag; (void)config;
    return JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_com_powercut_editor_export_ExportEngine_nativeRunning(
        JNIEnv* env, jobject thiz, jlong handle) {
    (void)env; (void)thiz; (void)handle;
    return JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_com_powercut_editor_export_ExportEngine_nativeIsFullEngine(JNIEnv* env, jobject thiz) {
    (void)env; (void)thiz;
    return JNI_TRUE;
}

JNIEXPORT jbyteArray JNICALL
Java_com_powercut_editor_export_ExportEngine_nativeGetRenderedFrame(
        JNIEnv* env, jobject thiz, jobject dagObj, jlong timeMicros, jint width, jint height) {
    (void)env; (void)thiz; (void)dagObj; (void)timeMicros; (void)width; (void)height;
    return nullptr;
}

}  // extern "C"
