cmake_minimum_required(VERSION 3.0)
project(jmsofficecomm C)

find_package(JNI REQUIRED)
find_package(IDL REQUIRED)

add_definitions(-DUNICODE -D_UNICODE)

file(GLOB CXX_FILES *.cxx)
add_library(${PROJECT_NAME} SHARED
            ${CXX_FILES}
            msofficecomm.rc
            jsmofficecomm.def
            )

add_idl(msgrua_idl ${OCSDK_DIR}/msgrua.idl)

target_link_libraries(${PROJECT_NAME} LINK_PUBLIC
                      advapi32
                      uuid
                      ole32
                      oleaut32
                      msi
                      msgrua_idl
                      )

target_include_directories(${PROJECT_NAME} PUBLIC
                           ${OCSDK_DIR}
                           ${JNI_INCLUDE_DIRS}
                           ${CMAKE_BINARY_DIR}/include/
                           ${CMAKE_CURRENT_LIST_DIR}/../../../target/native/javah
                           ${CMAKE_CURRENT_SOURCE_DIR}/../../../resources/install/windows
                           )

install(TARGETS ${PROJECT_NAME}
        RUNTIME DESTINATION "."
        LIBRARY DESTINATION ".")
