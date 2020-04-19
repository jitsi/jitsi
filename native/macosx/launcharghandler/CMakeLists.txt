cmake_minimum_required(VERSION 3.16)
project(AEGetURLEventHandler OBJC)

find_package(JNI REQUIRED)

add_library(${PROJECT_NAME} SHARED
        AEGetURLEventHandlerAgent.m
)

target_include_directories(${PROJECT_NAME} PUBLIC
                           ${JNI_INCLUDE_DIRS})

target_link_libraries(${PROJECT_NAME}
                      "-framework Foundation"
                      )
