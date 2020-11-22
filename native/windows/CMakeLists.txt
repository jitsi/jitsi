cmake_minimum_required(VERSION 3.0)
project(windows)

set(CMAKE_DISABLE_IN_SOURCE_BUILD ON)
set(CMAKE_DISABLE_SOURCE_CHANGES ON)

if ("${CMAKE_SOURCE_DIR}" STREQUAL "${CMAKE_BINARY_DIR}")
    message(SEND_ERROR "In-source builds are not allowed.")
endif ()

if (MSVC)
    set(CMAKE_EXE_LINKER_FLAGS /MANIFEST:NO)
endif ()

file(STRINGS ${CMAKE_SOURCE_DIR}/../../resources/install/gradle.properties ConfigContents)
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
string(REGEX REPLACE "([0-9]+)\\.([0-9]+).*" "\\1;\\2" _gradle_parts "${GRADLE_version}")
string(REGEX REPLACE ".+(-(.+))$" "\\2" VERSION_SUFFIX "${GRADLE_version}")
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

add_subdirectory(3rdparty/libbs)
add_subdirectory(3rdparty/libbzip2)
add_subdirectory(shared)
add_subdirectory(cleansweep)
#add_subdirectory(setup)
add_subdirectory(run)