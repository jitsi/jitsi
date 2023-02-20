set(OS_SOURCES "")
set(OS_INCLUDE "")
set(OS_LIBS "")

if (WIN32)
    project(globalshortcut C)
    set(OS_SOURCES
        globalshortcut.rc
        net_java_sip_communicator_impl_globalshortcut_NativeKeyboardHook.cpp
        )
elseif (APPLE)
    project(globalshortcut OBJC)
    externalproject_add(DDHotKey
                        GIT_REPOSITORY https://github.com/jitsi/ddhotkey
                        GIT_TAG 75ac26d9b7efbc878096f931f2e50d402938ea8c
                        GIT_SHALLOW true
                        CONFIGURE_COMMAND ""
                        BUILD_COMMAND ""
                        INSTALL_COMMAND ""
                        )
    externalproject_get_property(DDHotKey SOURCE_DIR)
    set(OS_SOURCES
        ${SOURCE_DIR}/DDHotKeyCenter.m
        ${SOURCE_DIR}/src/native/globalshortcut/net_java_sip_communicator_impl_globalshortcut_NativeKeyboardHook.m
        )
    set(OS_INCLUDE
        ${SOURCE_DIR}
        )
    set_source_files_properties(${OS_SOURCES} PROPERTIES GENERATED TRUE)
    set(OS_LIBS
        "-framework Foundation"
        "-framework Carbon"
        "-framework Cocoa"
        )
elseif (UNIX AND NOT APPLE)
    project(globalshortcut C)
    find_package(X11 REQUIRED)
    if (NOT X11_FOUND)
        message(FATAL_ERROR "libx11-dev is missing")
    endif ()

    find_package(Threads REQUIRED)
    if (NOT Threads_FOUND)
        message(FATAL_ERROR "Could not find pthreads")
    endif ()

    set(OS_LIBS
        ${X11_LIBRARIES}
        Threads::Threads
        )
    set(OS_SOURCES
        net_java_sip_communicator_impl_globalshortcut_NativeKeyboardHook.cc
        )
endif ()

add_library(${PROJECT_NAME} SHARED
            ${OS_SOURCES})

if (APPLE)
    add_dependencies(${PROJECT_NAME} DDHotKey)
endif ()

target_link_libraries(${PROJECT_NAME} LINK_PUBLIC
                      ${OS_LIBS}
                      )

target_include_directories(${PROJECT_NAME} PUBLIC
                           ${OS_INCLUDE}
                           ${CMAKE_CURRENT_LIST_DIR}/../../target/native/javah
                           ${JNI_INCLUDE_DIRS}
                           ${CMAKE_BINARY_DIR}/include/
                           )

if (NOT DEFINED SKIP_CUSTOM_INSTALL_DESTINATION)
    install(TARGETS ${PROJECT_NAME}
            RUNTIME DESTINATION "."
            LIBRARY DESTINATION ".")
else ()
    install(TARGETS ${PROJECT_NAME}
            RUNTIME DESTINATION ${CMAKE_INSTALL_BINDIR}
            LIBRARY DESTINATION ${CMAKE_INSTALL_LIBDIR})
endif ()
