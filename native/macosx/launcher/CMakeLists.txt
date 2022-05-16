project(Jitsi OBJC)

find_package(JNI REQUIRED)

add_executable(${PROJECT_NAME} MACOSX_BUNDLE
               launcher.m
               )

target_include_directories(${PROJECT_NAME} PUBLIC
                           ${JNI_INCLUDE_DIRS})

# "An application that intends to embed Foo.framework would then pass
# -rpath @executable_path/../Frameworks to the linker at build time,
# which tells the dynamic linker to search for @rpath frameworks there."
# c.f. https://www.mikeash.com/pyblog/friday-qa-2009-11-06-linking-and-install-names.html
target_link_libraries(${PROJECT_NAME}
                      "-framework Cocoa"
                      "-rpath @executable_path/../Frameworks")
