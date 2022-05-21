project(jmsoutlookaddrbookcomserver CXX)

add_idl(IMsOutlookAddrBookServer IMsOutlookAddrBookServer.idl)

file(GLOB CXX_FILES_MAIN ../*.cxx)
file(GLOB CXX_FILES_COM *.cxx)
file(GLOB CXX_FILES_SERVER server/*.cxx)

add_executable(${PROJECT_NAME} WIN32
               ${CXX_FILES_MAIN}
               ${CXX_FILES_COM}
               ${CXX_FILES_SERVER}
               jmsoutlookaddrbookcomserver.rc
               )

if (CMAKE_SIZEOF_VOID_P EQUAL 8)
    set(BITNESS_SUFFIX "64")
else ()
    set(BITNESS_SUFFIX "32")
endif ()

set_target_properties(${PROJECT_NAME}
                      PROPERTIES LINK_FLAGS /SUBSYSTEM:CONSOLE
                      OUTPUT_NAME "${PROJECT_NAME}${BITNESS_SUFFIX}"
                      )

target_link_libraries(${PROJECT_NAME} LINK_PUBLIC
                      advapi32
                      uuid
                      ole32
                      oleaut32
                      msi
                      IMsOutlookAddrBookClient
                      IMsOutlookAddrBookServer
                      )

target_include_directories(${PROJECT_NAME} PUBLIC
                           ${CMAKE_CURRENT_LIST_DIR}/../../../../target/native/javah
                           ${JNI_INCLUDE_DIRS}
                           ${MAPI_HEADERS_DIR}
                           ${CMAKE_BINARY_DIR}/include/
                           )

install(TARGETS ${PROJECT_NAME}
        RUNTIME DESTINATION "."
        LIBRARY DESTINATION ".")

add_custom_target(install.${PROJECT_NAME}
                  ${CMAKE_COMMAND}
                  -DBUILD_TYPE=${CMAKE_BUILD_TYPE}
                  -P "${CMAKE_CURRENT_BINARY_DIR}/cmake_install.cmake")
add_dependencies(install.${PROJECT_NAME} ${PROJECT_NAME})
