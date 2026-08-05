cmake_minimum_required(VERSION 3.22.1)
project(powercut LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# ---- PowerCut native core (fixed backend) -------------------------------------
# Compile the C++ engine + JNI bridge together. FFmpeg is prebuilt & linked
# from app/src/main/cpp/ffmpeg/{arm64-v8a,armeabi-v7a,x86_64}/.
set(POWERCUT_CORE_DIR ${CMAKE_SOURCE_DIR}/../../../..)
# NOTE: in real repo, src/ and include/ live at repo root, here we ship a copy
# under cpp/engine so the AGP CMake scope can see them. Adjust path to match
# repo layout (PowerCut/src and PowerCut/include) when copying into repo.
set(ENGINE_SRC_DIR ${CMAKE_SOURCE_DIR}/engine)

add_library(powercut_native SHARED
    ${ENGINE_SRC_DIR}/export/export_engine.cpp
    ${ENGINE_SRC_DIR}/core/dag_resolver.cpp
    ${ENGINE_SRC_DIR}/core/watermark.cpp
    ${ENGINE_SRC_DIR}/core/audio_mixer.cpp
    ${ENGINE_SRC_DIR}/core/hw_encoder.cpp
    ${ENGINE_SRC_DIR}/core/leveldb_cache.cpp
    native_export.cpp
)

target_include_directories(powercut_native PRIVATE
    ${ENGINE_SRC_DIR}/include
    ${CMAKE_SOURCE_DIR}
)

# Find Android log + media + EGL + GLES for live preview
find_library(log-lib log)
find_library(media-lib mediandk)
find_library(egl-lib EGL)
find_library(gles-lib GLESv3)
find_library(android-lib android)

# FFmpeg prebuilt (provide via prefab or local folder). When prefab packaging
# is used, swap to find_package(FFmpeg REQUIRED). The symbols below are guarded
# at runtime so the app builds & links even if FFmpeg is added later.
target_link_libraries(powercut_native
    ${log-lib}
    ${media-lib}
    ${egl-lib}
    ${gles-lib}
    ${android-lib}
)

# Optional FFmpeg — if present under cpp/ffmpeg/<abi>/libavcodec.so etc.
set(FF_DIR ${CMAKE_SOURCE_DIR}/ffmpeg/${ANDROID_ABI})
if(EXISTS ${FF_DIR}/include)
    target_include_directories(powercut_native PRIVATE ${FF_DIR}/include)
    foreach(lib avcodec avformat avutil swscale swresample avfilter)
        if(EXISTS ${FF_DIR}/lib${lib}.so)
            add_library(pc_${lib} SHARED IMPORTED)
            set_target_properties(pc_${lib} PROPERTIES IMPORTED_LOCATION ${FF_DIR}/lib${lib}.so)
            target_link_libraries(powercut_native pc_${lib})
        endif()
    endforeach()
    target_compile_definitions(powercut_native PRIVATE POWERCUT_FFMPEG_ENABLED=1)
else()
    target_compile_definitions(powercut_native PRIVATE POWERCUT_FFMPEG_ENABLED=0)
endif()

# Enable hard-float + NEON on arm
if(ANDROID_ABI STREQUAL "arm64-v8a")
    target_compile_options(powercut_native PRIVATE -mcpu=cortex-a76 -fvisibility=hidden)
endif()
