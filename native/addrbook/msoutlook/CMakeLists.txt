project(jmsoutlookaddrbook CXX)

add_idl(IMsOutlookAddrBookClient com/IMsOutlookAddrBookClient.idl)

file(GLOB CXX_FILES_MAIN *.cxx)
file(GLOB CXX_FILES_COM com/*.cxx)
file(GLOB CXX_FILES_SERVER com/server/*.cxx)
add_library(${PROJECT_NAME} SHARED
            ${CXX_FILES_MAIN}
            ${CXX_FILES_COM}
            ${CXX_FILES_SERVER}
            jmsoutlookaddrbook.rc
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
                           ${CMAKE_CURRENT_LIST_DIR}/../../../target/native/javah
                           ${JNI_INCLUDE_DIRS}
                           ${MAPI_HEADERS_DIR}
                           ${CMAKE_BINARY_DIR}/include/
                           )

install(TARGETS ${PROJECT_NAME}
        RUNTIME DESTINATION "."
        LIBRARY DESTINATION ".")
