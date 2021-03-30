project(globalshortcut C)

set(OS_SOURCES "")
set(OS_LIBS "")
if (WIN32)
    set(OS_SOURCES
        globalshortcut.rc
        net_java_sip_communicator_impl_globalshortcut_NativeKeyboardHook.cpp
        )
elseif (APPLE)
    set(OS_LIBS
        "-framework Foundation"
        "-framework Carbon"
        "-framework Cocoa"
        )
elseif (UNIX AND NOT APPLE)
    find_package(X11 REQUIRED)
    if (NOT X11_FOUND)
        message(FATAL_ERROR "libx11-dev is missing")
    endif ()
    set(OS_SOURCES
        net_java_sip_communicator_impl_globalshortcut_NativeKeyboardHook.cc
        )
endif ()

add_library(${PROJECT_NAME} SHARED
            ${OS_SOURCES})

target_link_libraries(${PROJECT_NAME} LINK_PUBLIC
                      ${OS_LIBS}
                      )

target_include_directories(${PROJECT_NAME} PUBLIC
                           ${CMAKE_CURRENT_LIST_DIR}/../../modules/service/globalshortcut/target/native/javah
                           ${JNI_INCLUDE_DIRS}
                           ${CMAKE_BINARY_DIR}/include/
                           )

install(TARGETS ${PROJECT_NAME}
        RUNTIME DESTINATION "."
        LIBRARY DESTINATION ".")
