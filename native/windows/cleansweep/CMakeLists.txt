cmake_minimum_required(VERSION 3.0)
project(cleansweep C)

add_executable(${PROJECT_NAME} WIN32
               src/cleansweep.c
               src/cleansweep.rc
               )
target_link_libraries(${PROJECT_NAME} LINK_PUBLIC
                      win_shared)
target_include_directories(${PROJECT_NAME} PUBLIC
                           include
                           ${CONFIG_INCLUDE_DIR}
                           ${CMAKE_CURRENT_SOURCE_DIR}/../../../resources/install/windows
                           )
