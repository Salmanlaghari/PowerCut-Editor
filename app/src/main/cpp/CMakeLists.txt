cmake_minimum_required(VERSION 3.22.1)
project(powercut_native LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

set(ENGINE_SRC_DIR ${CMAKE_SOURCE_DIR}/engine)

add_library(powercut_native SHARED
    ${ENGINE_SRC_DIR}/src/export/export_engine.cpp
    ${ENGINE_SRC_DIR}/src/core/dag_resolver.cpp
    ${ENGINE_SRC_DIR}/src/core/watermark.cpp
    ${ENGINE_SRC_DIR}/src/core/audio_mixer.cpp
    ${ENGINE_SRC_DIR}/src/core/hw_encoder.cpp
    ${ENGINE_SRC_DIR}/src/core/leveldb_cache.cpp
)

target_include_directories(powercut_native PRIVATE
    ${ENGINE_SRC_DIR}/include
    ${CMAKE_SOURCE_DIR}
)

target_compile_options(powercut_native PRIVATE
    -fexceptions
    -frtti
)

find_library(log-lib log)
find_library(media-lib mediandk)
find_library(egl-lib EGL)
find_library(gles-lib GLESv3)
find_library(android-lib android)

target_link_libraries(powercut_native
    ${log-lib}
    ${media-lib}
    ${egl-lib}
    ${gles-lib}
    ${android-lib}
)

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

if(ANDROID_ABI STREQUAL "arm64-v8a")
    target_compile_options(powercut_native PRIVATE -mcpu=cortex-a76 -fvisibility=hidden)
endif()
