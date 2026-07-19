cmake_minimum_required(VERSION 3.22.1)
project(retra_native LANGUAGES CXX)

add_library(retra_native SHARED
    reference_engine.cpp
    libretro_mgba_engine.cpp
    jni_bridge.cpp
)

target_include_directories(retra_native PRIVATE ${CMAKE_CURRENT_SOURCE_DIR})
target_compile_features(retra_native PRIVATE cxx_std_20)
target_compile_options(retra_native PRIVATE -Wall -Wextra -Werror)

find_library(log-lib log)
find_library(dl-lib dl)
target_link_libraries(retra_native PRIVATE ${log-lib} ${dl-lib})
