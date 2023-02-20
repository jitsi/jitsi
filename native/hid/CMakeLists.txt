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
        if (NOT X11_XTest_FOUND)
            message(FATAL_ERROR "libxtst-dev is missing")
        else ()
            set(XTST_LIB ${X11_XTest_LIB})
            set(XSS_HEADERS ${X11_XTest_INCLUDE_PATH})
        endif ()
    else ()
        set(XTST_LIB ${X11_Xtst_LIB})
        set(XSS_HEADERS ${X11_Xtst_INCLUDE_PATH})
    endif ()

    set(OS_LIBS
        ${X11_LIBRARIES}
        ${XTST_LIB}
        )
    set(OS_SOURCES
        KeyboardUtil_unix.c
        )
    set(OS_HEADERS
        ${X11_INCLUDE_DIR}
        ${XSS_HEADERS}
        )
endif ()

add_library(${PROJECT_NAME} SHARED
            net_java_sip_communicator_impl_hid_NativeKeyboard.c
            ${OS_SOURCES})

target_link_libraries(${PROJECT_NAME} LINK_PUBLIC
                      ${OS_LIBS}
                      )

target_include_directories(${PROJECT_NAME} PUBLIC
                           ${CMAKE_CURRENT_LIST_DIR}/../../target/native/javah
                           ${JNI_INCLUDE_DIRS}
                           ${OS_HEADERS}
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
