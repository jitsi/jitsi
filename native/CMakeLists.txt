cmake_minimum_required(VERSION 3.0)
project(jitsi_desktop_natives)

if (CMAKE_INSTALL_PREFIX_INITIALIZED_TO_DEFAULT)
    if (WIN32 AND CMAKE_SIZEOF_VOID_P EQUAL 8)
        set(CMAKE_INSTALL_PREFIX "${CMAKE_CURRENT_LIST_DIR}/../lib/native/windows-x64" CACHE PATH "default install path" FORCE)
    elseif (WIN32)
        set(CMAKE_INSTALL_PREFIX "${CMAKE_CURRENT_LIST_DIR}/../lib/native/windows-x86" CACHE PATH "default install path" FORCE)
    elseif (APPLE)
        set(CMAKE_INSTALL_PREFIX "${CMAKE_CURRENT_LIST_DIR}/../lib/native/mac-x64" CACHE PATH "default install path" FORCE)
    elseif (UNIX AND CMAKE_SIZEOF_VOID_P EQUAL 8)
        set(CMAKE_INSTALL_PREFIX "${CMAKE_CURRENT_LIST_DIR}/../lib/native/linux-x64" CACHE PATH "default install path" FORCE)
    elseif (UNIX)
        set(CMAKE_INSTALL_PREFIX "${CMAKE_CURRENT_LIST_DIR}/../lib/native/linux-x86" CACHE PATH "default install path" FORCE)
    endif ()
    message(STATUS "CMAKE_INSTALL_PREFIX not set, defaulting to >${CMAKE_INSTALL_PREFIX}<")
endif ()

set(CMAKE_DISABLE_IN_SOURCE_BUILD ON)
set(CMAKE_DISABLE_SOURCE_CHANGES ON)

if ("${CMAKE_SOURCE_DIR}" STREQUAL "${CMAKE_BINARY_DIR}")
    message(SEND_ERROR "In-source builds are not allowed.")
endif ()

if (MSVC)
    set(CMAKE_EXE_LINKER_FLAGS /MANIFEST:NO)
endif ()

set(MAIN_VERSION "1.0" CACHE STRING "Main version number")
file(STRINGS ${CMAKE_SOURCE_DIR}/../resources/install/gradle.properties ConfigContents)
foreach (NameAndValue ${ConfigContents})
    # Strip leading spaces
    string(REGEX REPLACE "^[ ]+" "" NameAndValue ${NameAndValue})
    # Find variable name
    string(REGEX MATCH "^[^=]+" Name ${NameAndValue})
    # Find the value
    string(REPLACE "${Name}=" "" Value ${NameAndValue})
    # Set the variable
    set(GRADLE_${Name} "${Value}")
endforeach ()

find_package(Git QUIET REQUIRED)

execute_process(
        COMMAND "${GIT_EXECUTABLE}" describe --match "Jitsi-[0-9\\.]*" --long --dirty --always
        WORKING_DIRECTORY "${CMAKE_SOURCE_DIR}"
        RESULT_VARIABLE res
        OUTPUT_VARIABLE GIT_VERSION
        ERROR_QUIET
        OUTPUT_STRIP_TRAILING_WHITESPACE)

set_property(GLOBAL APPEND
             PROPERTY CMAKE_CONFIGURE_DEPENDS
             "${CMAKE_SOURCE_DIR}/.git/index")

string(REGEX REPLACE "(Jitsi-[0-9.]+-([0-9]+)-)(.*)" "\\2;\\3" _git_parts "${GIT_VERSION}")
string(REGEX REPLACE "([0-9]+)\\.([0-9]+).*" "\\1;\\2" _gradle_parts "${MAIN_VERSION}")
string(REGEX REPLACE ".+(-(.+))$" "\\2" VERSION_SUFFIX "${MAIN_VERSION}")
list(GET _gradle_parts 0 VERSION_MAJOR)
list(GET _gradle_parts 1 VERSION_MINOR)
list(GET _git_parts 0 VERSION_REVISION)
list(GET _git_parts 1 VERSION_HASH)
string(JOIN "." VERSION_SHORT ${VERSION_MAJOR} ${VERSION_MINOR} ${VERSION_REVISION})
string(JOIN "-" VERSION_LONG ${VERSION_SHORT} ${VERSION_SUFFIX})
string(JOIN "+" VERSION_FULL ${VERSION_LONG} ${VERSION_HASH})

set(PRODUCTNAME ${GRADLE_application.name})
set(PRODUCTNAME_EXE "${GRADLE_application.name}.exe")
set(PRODUCTCOPYRIGHT ${GRADLE_application.org})
set(COMPANYNAME ${GRADLE_application.org})
set(PRODUCTBUILDVERSION "${VERSION_SHORT}.0")
set(PRODUCTBUILDVERSION_COMMA "${VERSION_MAJOR},${VERSION_MINOR},${VERSION_REVISION},0")
set(PRODUCT_INFO_VERSION ${VERSION_FULL})
if (NOT "${VERSION_SUFFIX}" STREQUAL "")
    set(PRODUCT_FILEFLAGS "VS_FF_PRERELEASE")
else ()
    set(PRODUCT_FILEFLAGS "0")
endif ()

find_package(JNI REQUIRED)
set(CONFIG_INCLUDE_DIR ${CMAKE_CURRENT_BINARY_DIR}/include)
configure_file(config.h.in include/config.h)
list(APPEND CMAKE_MODULE_PATH "${CMAKE_CURRENT_LIST_DIR}/cmake")

if (WIN32)
    if (NOT DEFINED OCSDK_DIR OR NOT DEFINED MAPI_HEADERS_DIR)
        include(ExternalProject)
        if (NOT DEFINED PRIVATE_SDK_REPO)
            set(PRIVATE_SDK_REPO https://github.com/jitsi/jitsi-3rdparty-nonredist)
        endif ()
        externalproject_add(private_sdks
                            GIT_REPOSITORY https://github.com/jitsi/jitsi-3rdparty-nonredist
                            GIT_TAG 78e50e82a2fca3dbbeb3f0b6921ef6fc14dcdb8e
                            GIT_SHALLOW true
                            CONFIGURE_COMMAND ""
                            BUILD_COMMAND ""
                            INSTALL_COMMAND ""
                            )
        externalproject_get_property(private_sdks SOURCE_DIR)
        if (NOT DEFINED OCSDK_DIR)
            set(OCSDK_DIR ${SOURCE_DIR}/ocsdk2007)
            set(OCSDK_DIR_INTERNAL private_sdks)
        endif ()
        if (NOT DEFINED MAPI_HEADERS_DIR)
            set(MAPI_HEADERS_DIR ${SOURCE_DIR}/outlook2010mapi)
            set(MAPI_HEADERS_DIR_INTERNAL private_sdks)
        endif ()
    endif ()
    add_definitions(-DUNICODE -D_UNICODE -D_CRT_SECURE_NO_WARNINGS)
    add_subdirectory(windows)
elseif (APPLE)
    add_subdirectory(macosx)
elseif (UNIX AND NOT APPLE)
    add_subdirectory(linux/galagonotification)
endif ()
add_subdirectory(addrbook/)
if (NOT APPLE)
    add_subdirectory(globalshortcut)
endif ()
add_subdirectory(hid)
add_subdirectory(sysactivity)
