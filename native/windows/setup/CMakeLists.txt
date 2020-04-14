cmake_minimum_required(VERSION 3.0)
project(setup C)

file(GLOB C_FILES src/*.c)
add_executable(${PROJECT_NAME} WIN32
               ${C_FILES}
               src/setup.rc)

add_library(lzma STATIC IMPORTED)
if ("${CMAKE_SYSTEM_PROCESSOR}" MATCHES "(x86_64|amd64)")
    set_property(TARGET lzma PROPERTY IMPORTED_IMPLIB ${CMAKE_CURRENT_SOURCE_DIR}/../3rdparty/xz/x86-64/liblzma.a)
else ()
    set_property(TARGET lzma PROPERTY IMPORTED_IMPLIB ${CMAKE_CURRENT_SOURCE_DIR}/../3rdparty/xz/x86/liblzma.a)
endif ()


target_link_libraries(${PROJECT_NAME} LINK_PUBLIC
                      win_shared
                      bs
                      bzip2
                      lzma
                      advapi32
                      psapi
                      )
target_include_directories(${PROJECT_NAME} PUBLIC
                           include
                           ${JNI_INCLUDE_DIRS}
                           ${CMAKE_CURRENT_SOURCE_DIR}/../3rdparty/xz/include
                           ${CMAKE_CURRENT_SOURCE_DIR}/../../../resources/install/windows
                           )
