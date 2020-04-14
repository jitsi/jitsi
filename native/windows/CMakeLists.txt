cmake_minimum_required(VERSION 3.0)
project(windows)

set(CMAKE_DISABLE_IN_SOURCE_BUILD ON)
set(CMAKE_DISABLE_SOURCE_CHANGES  ON)

if ("${CMAKE_SOURCE_DIR}" STREQUAL "${CMAKE_BINARY_DIR}")
    message(SEND_ERROR "In-source builds are not allowed.")
endif ()

if (MSVC)
    set(CMAKE_EXE_LINKER_FLAGS /MANIFEST:NO)
endif ()

add_subdirectory(3rdparty/libbs)
add_subdirectory(3rdparty/libbzip2)
add_subdirectory(shared)
add_subdirectory(cleansweep)
#add_subdirectory(setup)
add_subdirectory(run)