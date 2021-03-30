project(galagonotification C)

find_package(DBus REQUIRED)
add_library(${PROJECT_NAME} SHARED
            net_java_sip_communicator_impl_galagonotification_GalagoNotification.c
            )

target_link_libraries(${PROJECT_NAME} LINK_PUBLIC
                      ${DBUS_LIBRARIES})

target_include_directories(${PROJECT_NAME} PUBLIC
                           ${CMAKE_CURRENT_LIST_DIR}/../../../modules/impl/galagonotification/target/native/javah
                           ${DBUS_INCLUDE_DIRS}
                           ${JNI_INCLUDE_DIRS}
                           ${CMAKE_BINARY_DIR}/include/
                           )

install(TARGETS ${PROJECT_NAME}
        RUNTIME DESTINATION "."
        LIBRARY DESTINATION ".")
