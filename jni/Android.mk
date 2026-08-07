# ==============================================================================
# PowerCut Editor — Android NDK Build (jni/Android.mk)
# Builds the native C++ export engine for the Android app target.
# Compiled via ndk-build; wired into the Gradle externalNativeBuild pipeline.
# ==============================================================================
LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := powercut
LOCAL_CPPFLAGS  := -std=c++17 -fexceptions -frtti -O2 -DNDEBUG
LOCAL_C_INCLUDES += $(LOCAL_PATH)/include

# ------------------------------------------------------------------------------
# Core engine sources (PowerCutDAG + DecoderFarm + GPU Compositor)
# ------------------------------------------------------------------------------
LOCAL_SRC_FILES += \
    src/core/dag.cpp \
    src/core/decoder_farm.cpp \
    src/core/compositor.cpp

# ------------------------------------------------------------------------------
# ============= POWERCUT EXPORT ENGINE (WITH WATERMARK SYSTEM) =============
# ------------------------------------------------------------------------------
LOCAL_SRC_FILES += src/export/export_engine.cpp src/export/export_presets.cpp src/export/android/hw_encoder_android.cpp
LOCAL_C_INCLUDES += $(LOCAL_PATH)/include
LOCAL_LDLIBS += -lavcodec -lavformat -lavutil -lswscale -lswresample -lleveldb

include $(BUILD_SHARED_LIBRARY)
