cmake_minimum_required(VERSION 3.16)
set(CMAKE_OSX_DEPLOYMENT_TARGET "10.10" CACHE STRING "Minimum OS X deployment version")
project(macosx)

add_subdirectory(launcher)
add_subdirectory(sparkle)
