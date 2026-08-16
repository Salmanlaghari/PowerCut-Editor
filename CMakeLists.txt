# ==============================================================================
# PowerCut Editor — Native C++ Build (CMake)
# Root CMakeLists.txt for the PowerCut native export engine layer.
# Driven by jni/Android.mk on Android (NDK) and by CMake on Apple/Windows.
# ==============================================================================
cmake_minimum_required(VERSION 3.18.1)
project(powercut LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

# Default build type
if(NOT CMAKE_BUILD_TYPE)
  set(CMAKE_BUILD_TYPE Release)
endif()

# ==============================================================================
# Core engine (PowerCutDAG + DecoderFarm + GPU Compositor) — provided by repo.
# The include/ and src/core/ trees already exist for the core engine layer.
# ==============================================================================
include_directories(${CMAKE_SOURCE_DIR}/include)

set(PW_CORE_SRC
  src/core/core_globals.cpp
)

# ==============================================================================
# ============= POWERCUT EXPORT ENGINE (WITH WATERMARK SYSTEM) =============
# ==============================================================================
set(PW_EXPORT_SRC
  src/export/export_engine.cpp
  src/export/export_presets.cpp
  $<$<PLATFORM_ID:Android>:src/export/android/hw_encoder_android.cpp>
  $<$<PLATFORM_ID:iOS,Darwin>:src/export/apple/hw_encoder_apple.mm>
  $<$<PLATFORM_ID:Windows>:src/export/windows/hw_encoder_windows.cpp>
)

add_library(powercut STATIC ${PW_CORE_SRC} ${PW_EXPORT_SRC})

find_package(FFmpeg REQUIRED COMPONENTS avcodec avformat avutil swscale swresample)
find_package(LevelDB REQUIRED)
target_link_libraries(powercut PRIVATE FFmpeg::avcodec FFmpeg::avformat FFmpeg::avutil FFmpeg::swscale FFmpeg::swresample LevelDB::LevelDB)
