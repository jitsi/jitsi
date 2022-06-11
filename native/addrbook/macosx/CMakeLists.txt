project(jmacosxaddrbook OBJC)

find_package(JNI REQUIRED)

add_library(${PROJECT_NAME} SHARED
        net_java_sip_communicator_plugin_addrbook_macosx_MacOSXAddrBookContactQuery.m
        net_java_sip_communicator_plugin_addrbook_macosx_MacOSXAddrBookContactSourceService.m
        ../AddrBookContactQuery.c
        )

target_include_directories(${PROJECT_NAME} PUBLIC
        ${JNI_INCLUDE_DIRS}
        ${CMAKE_CURRENT_LIST_DIR}/..
        ${CMAKE_CURRENT_LIST_DIR}/../../../target/native/javah
        )

target_link_libraries(${PROJECT_NAME}
        "-framework AddressBook"
        "-framework Foundation"
        )

install(TARGETS ${PROJECT_NAME}
        RUNTIME DESTINATION "."
        LIBRARY DESTINATION ".")
