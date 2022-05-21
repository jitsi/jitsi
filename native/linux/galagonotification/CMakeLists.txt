project(galagonotification C)

find_package(PkgConfig REQUIRED)
pkg_check_modules(DBUS REQUIRED dbus-1)
add_library(${PROJECT_NAME} SHARED
            net_java_sip_communicator_impl_galagonotification_GalagoNotification.c
            )

target_link_libraries(${PROJECT_NAME} LINK_PUBLIC
                      ${DBUS_LIBRARIES})

target_include_directories(${PROJECT_NAME} PUBLIC
                           ${CMAKE_CURRENT_LIST_DIR}/../../../target/native/javah
                           ${DBUS_INCLUDE_DIRS}
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
