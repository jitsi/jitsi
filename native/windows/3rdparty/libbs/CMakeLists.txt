cmake_minimum_required(VERSION 3.0)
project(bs)

file(GLOB_RECURSE SOURCE_FILES "src/*.c")

add_library(${PROJECT_NAME} STATIC
            ${SOURCE_FILES}
            )
target_link_libraries(${PROJECT_NAME} LINK_PUBLIC
                      bzip2
                      )

# Define headers for this library. PUBLIC headers are used for
# compiling the library, and will be added to consumers' build
# paths.
target_include_directories(${PROJECT_NAME} PUBLIC
                           $<BUILD_INTERFACE:${CMAKE_CURRENT_SOURCE_DIR}/include>
                           $<INSTALL_INTERFACE:include>
                           PRIVATE src)
