#!/usr/bin/env bash
set -x

SRC_DIR=$1
VERSION=$2
ARCH=$3

TEMP_MOUNT=$(mktemp -d "${TMPDIR:-/tmp}jitsi-dmg.XXXXXXXXX")
TEMP_DMG="Jitsi-tmp.dmg"
DMG="jitsi-${VERSION}-mac-${ARCH}.dmg"

hdiutil create -srcfolder "${SRC_DIR}" -volname "Jitsi" -fs HFS+ -ov "${TEMP_DMG}" -format UDRW
hdiutil attach "${TEMP_DMG}" -mountroot "${TEMP_MOUNT}/"
SetFile -a C "${TEMP_MOUNT}/Jitsi"
ln -s /Applications "${TEMP_MOUNT}/Jitsi"
hdiutil detach "${TEMP_MOUNT}/Jitsi"
hdiutil convert "${TEMP_DMG}" -format UDZO -o "${DMG}"
