cmake_minimum_required(VERSION 3.0)
project(win_shared)

set(PRODUCTNAME "Jitsi" CACHE STRING "Application product name")
set(PRODUCTNAME_EXE "jitsi.exe" CACHE STRING "Application executable name")
set(PRODUCTCOPYRIGHT "jitsi.org" CACHE STRING "Copyright vendor")
set(COMPANYNAME "jitsi.org" CACHE STRING "Application vendor")
set(PRODUCTBUILDVERSION "1.0.0.0" CACHE STRING "Product version")
set(PRODUCTBUILDVERSION_COMMA "1,0,0,0" CACHE STRING "Product version for parseable fields")

file(GLOB_RECURSE SOURCE_FILES "src/*.c")

configure_file(config.h.in include/config.h)

add_library(${PROJECT_NAME} STATIC
            ${SOURCE_FILES}
            )

# Define headers for this library. PUBLIC headers are used for
# compiling the library, and will be added to consumers' build
# paths.
target_include_directories(${PROJECT_NAME} PUBLIC
                           $<BUILD_INTERFACE:${CMAKE_CURRENT_SOURCE_DIR}/include>
                           $<INSTALL_INTERFACE:include>
                           PRIVATE src)
target_include_directories(${PROJECT_NAME} PUBLIC
                           ${CMAKE_CURRENT_BINARY_DIR}/include/
                           )
