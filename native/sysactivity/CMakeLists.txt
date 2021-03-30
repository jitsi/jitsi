project(sysactivitynotifications C)

set(OS_SOURCES "")
set(OS_LIBS "")
if (WIN32)
    set(OS_LIBS
        iphlpapi
        )
    set(OS_SOURCES
        sysactivity.rc
        net_java_sip_communicator_impl_sysactivity_SystemActivityNotifications.cpp
        )
elseif (APPLE)
    set(OS_LIBS
        "-framework AppKit"
        "-framework Carbon"
        "-framework SystemConfiguration"
        "-framework IOKit"
        )
    set(OS_SOURCES
        net_java_sip_communicator_impl_sysactivity_SystemActivityNotifications.m
        )
elseif (UNIX AND NOT APPLE)
    find_package(X11 REQUIRED)
    find_package(GTK2 REQUIRED)
    if (NOT X11_FOUND)
        message(FATAL_ERROR "libx11-dev is missing")
    endif ()
    if (NOT X11_Xss_FOUND)
        message(FATAL_ERROR "libxss-dev is missing")
    endif ()
    if (NOT GTK2_FOUND)
        message(FATAL_ERROR "libgtk2.0-dev is missing")
    endif ()
    set(OS_SOURCES
        net_java_sip_communicator_impl_sysactivity_SystemActivityNotifications.c
        )
    set(OS_HEADERS
        ${X11_INCLUDE_DIR}
        ${X11_Xss_INCLUDE_PATH}
        ${GTK2_INCLUDE_DIRS}
        )
endif ()

add_library(${PROJECT_NAME} SHARED
            ${OS_SOURCES})

target_link_libraries(${PROJECT_NAME} LINK_PUBLIC
                      ${OS_LIBS}
                      )

target_include_directories(${PROJECT_NAME} PUBLIC
                           ${CMAKE_CURRENT_LIST_DIR}/../../modules/service/sysactivity/target/native/javah
                           ${JNI_INCLUDE_DIRS}
                           ${OS_HEADERS}
                           ${CMAKE_BINARY_DIR}/include/
                           )

install(TARGETS ${PROJECT_NAME}
        RUNTIME DESTINATION "."
        LIBRARY DESTINATION ".")
