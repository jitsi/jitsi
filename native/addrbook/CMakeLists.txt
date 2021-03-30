project(addrbook CXX)

if (WIN32)
    add_subdirectory(msoutlook)
    add_subdirectory(msoutlook/com)
elseif (APPLE)
    add_subdirectory(macosx)
endif ()
