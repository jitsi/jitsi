project(hid C)

set(OS_SOURCES "")
set(OS_LIBS "")
if (WIN32)
    set(OS_SOURCES
        hid.rc
        KeyboardUtil_windows.c
        )
elseif (APPLE)
    set(OS_LIBS
        "-framework ApplicationServices"
        "-framework Carbon"
        )
    set(OS_SOURCES
        KeyboardUtil_mac.c
        )
elseif (UNIX AND NOT APPLE)
    find_package(X11 REQUIRED)
    if (NOT X11_FOUND)
        message(FATAL_ERROR "libx11-dev is missing")
    endif ()
    if (NOT X11_Xtst_FOUND)
        message(FATAL_ERROR "libxtst-dev is missing")
    endif ()
    set(OS_SOURCES
        KeyboardUtil_unix.c
        )
    set(OS_HEADERS
        ${X11_INCLUDE_DIR}
        ${X11_Xtst_INCLUDE_PATH}
        )
endif ()

add_library(${PROJECT_NAME} SHARED
            net_java_sip_communicator_impl_hid_NativeKeyboard.c
            ${OS_SOURCES})

target_link_libraries(${PROJECT_NAME} LINK_PUBLIC
                      ${OS_LIBS}
                      )

target_include_directories(${PROJECT_NAME} PUBLIC
                           ${CMAKE_CURRENT_LIST_DIR}/../../modules/impl/hid/target/native/javah
                           ${JNI_INCLUDE_DIRS}
                           ${OS_HEADERS}
                           ${CMAKE_BINARY_DIR}/include/
                           )

install(TARGETS ${PROJECT_NAME}
        RUNTIME DESTINATION "."
        LIBRARY DESTINATION ".")
