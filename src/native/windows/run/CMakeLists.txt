cmake_minimum_required(VERSION 3.0)
project(run)

if(MSVC)
    set(CMAKE_EXE_LINKER_FLAGS /MANIFEST:NO)
endif()

find_package(JNI REQUIRED)
include_directories(
        ${JNI_INCLUDE_DIRS}
        ../../../../resources/install/windows
        ../../../../release/windows/tmp
        ../setup
)
add_executable(${PROJECT_NAME} WIN32 run.c registry.c ../setup/nls.c run.rc)
target_link_libraries(${PROJECT_NAME}
        advapi32
        psapi
)
