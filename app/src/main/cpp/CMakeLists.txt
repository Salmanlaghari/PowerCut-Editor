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
# Design: The cpp/ directory is self-contained — it includes local copies of
#   the stub headers (powercut/core/*.h, powercut/export/*.h) and
#   core_globals.cpp so the JNI stub always builds even if the repo-root
#   include/ tree is not reachable from the CMake working directory.
#
# Full export engine (FFmpeg + LevelDB) is compiled when those libraries are
# detected in third_party/. On CI without them, the JNI stub still builds
# so the Kotlin layer can load the library and call guarded external methods.
# =============================================================================
cmake_minimum_required(VERSION 3.22.1)
project(powercut LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# ---- Include paths -------------------------------------------------------
# Local copies of the PowerCut headers live in cpp/powercut/.
# The repo-root include/ tree is added as a secondary path (if reachable)
# so the full-engine sources can find the same headers.
include_directories(${CMAKE_CURRENT_SOURCE_DIR})

# Try to locate the repo root for the full-engine sources.
get_filename_component(_THIS_DIR "${CMAKE_CURRENT_SOURCE_DIR}" ABSOLUTE)
get_filename_component(_UP1 "${_THIS_DIR}/.." ABSOLUTE)
get_filename_component(_UP2 "${_UP1}/.." ABSOLUTE)
get_filename_component(_UP3 "${_UP2}/.." ABSOLUTE)

set(POWERCUT_ROOT "")
foreach(_candidate "${_UP3}" "${CMAKE_SOURCE_DIR}" "${_UP2}")
    if(EXISTS "${_candidate}/include/powercut/export/export_engine.h"
       AND EXISTS "${_candidate}/src/core/core_globals.cpp")
        set(POWERCUT_ROOT "${_candidate}")
        message(STATUS "PowerCut: repo root detected at ${POWERCUT_ROOT}")
        break()
    endif()
endforeach()

if(NOT POWERCUT_ROOT STREQUAL "")
    include_directories(${POWERCUT_ROOT}/include)
endif()

# ---- Always-compiled sources (no external deps) --------------------------
# These are LOCAL copies — always available, always build.
set(POWERCUT_SOURCES
    ${CMAKE_CURRENT_SOURCE_DIR}/native_export.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/core_globals.cpp
)

# ---- Full export engine (only when FFmpeg + LevelDB headers are found) ----
if(NOT POWERCUT_ROOT STREQUAL ""
   AND EXISTS "${POWERCUT_ROOT}/third_party/ffmpeg/libavformat/avformat.h"
   AND EXISTS "${POWERCUT_ROOT}/third_party/leveldb/include/leveldb/db.h")
    message(STATUS "PowerCut: Full FFmpeg + LevelDB export engine detected")
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
endif()

add_library(powercut SHARED ${POWERCUT_SOURCES})

# Tell native_export.cpp whether the real ExportEngine is linked.
# POWERCUT_FULL_EXPORT_ENGINE => use real engine; otherwise use inline stubs.
if(POWERCUT_SOURCES MATCHES "export_engine\\.cpp")
    target_compile_definitions(powercut PRIVATE POWERCUT_FULL_EXPORT_ENGINE)
    message(STATUS "PowerCut: Full export engine enabled")
else()
    message(STATUS "PowerCut: Building JNI stub only (export engine deferred)")
endif()

# Find log library for Android
find_library(log-lib log)
target_link_libraries(powercut ${log-lib})
