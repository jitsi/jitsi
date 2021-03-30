cmake_minimum_required(VERSION 3.0)
project(run C)

find_package(JNI REQUIRED)
add_executable(${PRODUCTNAME} WIN32
               src/run.c
               src/run.rc)

target_link_libraries(${PRODUCTNAME} LINK_PUBLIC
                      win_shared
                      advapi32
                      psapi
                      )
target_include_directories(${PRODUCTNAME} PUBLIC
                           include
                           ${CONFIG_INCLUDE_DIR}
                           ${JNI_INCLUDE_DIRS}
                           ${CMAKE_CURRENT_SOURCE_DIR}/../../../resources/install/windows
                           )
