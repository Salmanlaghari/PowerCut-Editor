# =============================================================================
# PowerCut Editor — App-level CMake (app/src/main/cpp/CMakeLists.txt)
#
# Wired into the Gradle build via:
#   android { defaultConfig { externalNativeBuild { cmake { ... } } } }
#   android { externalNativeBuild { cmake { path = ... } } }
#
# Builds the "powercut" shared library loaded by
#   ExportEngine.kt -> System.loadLibrary("powercut")
#
# Always compiled:
#   - native_export.cpp (JNI bridge)
#   - src/core/core_globals.cpp (global decoder/compositor stubs)
#
# Full export engine (FFmpeg + LevelDB) is compiled when the FFmpeg/LevelDB
# libraries are available. On CI without those, the JNI stub still builds
# so the Kotlin layer can load the library and call guarded external methods.
# =============================================================================
cmake_minimum_required(VERSION 3.22.1)
project(powercut LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Root of the repo (two levels up from app/src/main/cpp/)
get_filename_component(POWERCUT_ROOT "${CMAKE_CURRENT_SOURCE_DIR}/../../.." ABSOLUTE)

# Include paths
include_directories(${POWERCUT_ROOT}/include)
include_directories(${CMAKE_CURRENT_SOURCE_DIR})

# ---- Always-compiled sources (no external deps) ----
set(POWERCUT_SOURCES
    ${CMAKE_CURRENT_SOURCE_DIR}/native_export.cpp
    ${POWERCUT_ROOT}/src/core/core_globals.cpp
)

# ---- Full export engine (only when FFmpeg headers are found in third_party) ----
# The FFmpeg submodule provides headers at third_party/ffmpeg/libavformat/avformat.h
# LevelDB provides headers at third_party/leveldb/include/leveldb/db.h
# When both are present, compile the full engine; otherwise skip (stub mode).
if(EXISTS "${POWERCUT_ROOT}/third_party/ffmpeg/libavformat/avformat.h" AND
   EXISTS "${POWERCUT_ROOT}/third_party/leveldb/include/leveldb/db.h")
    message(STATUS "PowerCut: Full FFmpeg + LevelDB export engine detected — compiling full pipeline")
    list(APPEND POWERCUT_SOURCES
        ${POWERCUT_ROOT}/src/export/export_engine.cpp
        ${POWERCUT_ROOT}/src/export/export_presets.cpp
    )
    if(ANDROID)
        list(APPEND POWERCUT_SOURCES
            ${POWERCUT_ROOT}/src/export/android/hw_encoder_android.cpp
        )
    endif()
    include_directories(${POWERCUT_ROOT}/third_party/ffmpeg)
    include_directories(${POWERCUT_ROOT}/third_party/leveldb/include)
    # Tell native_export.cpp to use the real ExportEngine (skip stubs)
    target_compile_definitions(powercut PRIVATE POWERCUT_FULL_EXPORT_ENGINE)
else()
    message(STATUS "PowerCut: FFmpeg/LevelDB not found — building JNI stub only (export engine deferred)")
endif()

add_library(powercut SHARED ${POWERCUT_SOURCES})

# Find log library for Android
find_library(log-lib log)
target_link_libraries(powercut ${log-lib})
