# Find Sparkle.framework
#
# Once done this will define
#  SPARKLE_FOUND - system has Sparkle
#  SPARKLE_INCLUDE_DIR - the Sparkle include directory
#  SPARKLE_LIBRARY - The library needed to use Sparkle
# Copyright (c) 2009, Vittorio Giovara <vittorio.giovara@gmail.com>
#
# Distributed under the OSI-approved BSD License (the "License")
#
# This software is distributed WITHOUT ANY WARRANTY; without even the
# implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
# See the License for more information.
include(FindPackageHandleStandardArgs)

find_path(SPARKLE_INCLUDE_DIR Sparkle.h)
find_library(SPARKLE_LIBRARY NAMES Sparkle)

find_package_handle_standard_args(Sparkle DEFAULT_MSG SPARKLE_INCLUDE_DIR SPARKLE_LIBRARY)
mark_as_advanced(SPARKLE_INCLUDE_DIR SPARKLE_LIBRARY)
