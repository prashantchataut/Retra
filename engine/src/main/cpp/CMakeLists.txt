# Retra engine native library (libretra_engine.so).
cmake_minimum_required(VERSION 3.22.1)
project(retra_engine CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

file(GLOB RETRA_CORE_SOURCES CONFIGURE_DEPENDS ${CMAKE_CURRENT_SOURCE_DIR}/core/*.cpp)

add_library(retra_engine SHARED
    ${RETRA_CORE_SOURCES}
    ${CMAKE_CURRENT_SOURCE_DIR}/jni/retra_jni.cpp)

target_include_directories(retra_engine PRIVATE ${CMAKE_CURRENT_SOURCE_DIR}/core)

# The interpreter must be optimised even in debug builds of the app, otherwise games
# run far below full speed. Later flags override the build-type defaults.
target_compile_options(retra_engine PRIVATE
    -O3
    -fno-exceptions
    -fno-rtti
    -fvisibility=hidden
    -ffunction-sections
    -fdata-sections
    -Wall
    -Wno-unused-parameter)

target_link_options(retra_engine PRIVATE
    -Wl,--gc-sections
    -Wl,--exclude-libs,ALL
    -Wl,-z,max-page-size=16384)
