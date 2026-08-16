# ==============================================================================
# PowerCut Desktop — CMake Build
# Top-level CMakeLists.txt for nextgen-desktop/
# Builds a Qt6 desktop app that shares the C++ engine from the parent project.
# ==============================================================================
cmake_minimum_required(VERSION 3.18.1)
project(powercut-desktop LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Project root (parent of nextgen-desktop/)
get_filename_component(PROJECT_ROOT ${CMAKE_CURRENT_SOURCE_DIR}/.. REALPATH)

# ==============================================================================
# Qt6
# ==============================================================================
find_package(Qt6 REQUIRED COMPONENTS Core Widgets)

# ==============================================================================
# FFmpeg
# ==============================================================================
find_package(FFmpeg REQUIRED COMPONENTS avcodec avformat avutil swscale swresample avfilter)

# ==============================================================================
# Shared C++ engine library (from parent src/ and include/)
# ==============================================================================
add_library(powercut_engine STATIC
  ${PROJECT_ROOT}/src/core/core_globals.cpp
  ${PROJECT_ROOT}/src/export/export_engine.cpp
  ${PROJECT_ROOT}/src/export/export_presets.cpp
)

target_include_directories(powercut_engine PUBLIC
  ${PROJECT_ROOT}/include
)

target_link_libraries(powercut_engine PRIVATE
  FFmpeg::avcodec
  FFmpeg::avformat
  FFmpeg::avutil
  FFmpeg::swscale
  FFmpeg::swresample
)

# ==============================================================================
# LevelDB (required by export engine)
# ==============================================================================
find_package(LevelDB REQUIRED)
target_link_libraries(powercut_engine PRIVATE LevelDB::LevelDB)

# ==============================================================================
# Desktop application
# ==============================================================================
set(CMAKE_AUTOMOC ON)
set(CMAKE_AUTORCC ON)
set(CMAKE_AUTOUIC ON)

add_executable(powercut-desktop
  src/main.cpp
  src/main_window.cpp
  src/project_importer.cpp
  src/timeline_renderer.cpp
  src/export_manager.cpp
)

target_include_directories(powercut-desktop PRIVATE
  src
  ${PROJECT_ROOT}/include
)

target_link_libraries(powercut-desktop PRIVATE
  powercut_engine
  Qt6::Core
  Qt6::Widgets
  FFmpeg::avcodec
  FFmpeg::avformat
  FFmpeg::avutil
  FFmpeg::swscale
  FFmpeg::swresample
  FFmpeg::avfilter
)
