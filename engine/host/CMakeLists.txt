cmake_minimum_required(VERSION 3.16)
project(retra_host CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
if(NOT CMAKE_BUILD_TYPE)
  set(CMAKE_BUILD_TYPE Release)
endif()
set(CMAKE_CXX_FLAGS_RELEASE "-O2 -DNDEBUG")

set(CORE_DIR ${CMAKE_CURRENT_SOURCE_DIR}/../src/main/cpp/core)
file(GLOB CORE_SOURCES ${CORE_DIR}/*.cpp)

add_library(retra_core STATIC ${CORE_SOURCES})
target_include_directories(retra_core PUBLIC ${CORE_DIR})
target_compile_options(retra_core PRIVATE -Wall -Wextra -Wno-unused-parameter -fno-exceptions -fno-rtti)

add_executable(retra-headless main.cpp)
target_link_libraries(retra-headless PRIVATE retra_core)
target_compile_options(retra-headless PRIVATE -Wall)

add_executable(retra-unit-tests unit_tests.cpp)
target_link_libraries(retra-unit-tests PRIVATE retra_core)
