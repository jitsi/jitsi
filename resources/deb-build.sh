#!/usr/bin/env bash
set -e
set -x
VERSION=$1
DIST=$2
ARCH=$3
PROJECT_DIR="$(realpath "$(dirname "$0")/../")"
cd "${PROJECT_DIR}" || exit
# export for sbuildrc sourcing
export BUILD_DIR=${PROJECT_DIR}/target/debian/${DIST}
mkdir -p "${BUILD_DIR}"

# https://bugs.launchpad.net/ubuntu/+source/ubuntu-dev-tools/+bug/1964670
sudo sed -i s/pkg-config-\$target_tuple//g /usr/bin/mk-sbuild

# use tmpfs for sbuild
sudo tee -a /etc/fstab < "${PROJECT_DIR}/resources/sbuild-tmpfs"

# --skip-security because: https://bugs.launchpad.net/ubuntu/+source/ubuntu-dev-tools/+bug/1955116
# -debootstrap-include=default-jdk because: https://bugs.debian.org/cgi-bin/bugreport.cgi?bug=994152
if [[ "${ARCH}" != "amd64" ]]; then
  mk-sbuild "${DIST}" --target "${ARCH}" --skip-security --type=file --debootstrap-include=default-jdk || sbuild-update -udc "${DIST}"-amd64-"${ARCH}"
else
  if debian-distro-info --all | grep -Fqxi "${DIST}"; then
    export DEBOOTSTRAP_MIRROR=${DEBOOTSTRAP_MIRROR:-$UBUNTUTOOLS_DEBIAN_MIRROR}
  elif ubuntu-distro-info --all | grep -Fqxi "${DIST}"; then
    export DEBOOTSTRAP_MIRROR=${DEBOOTSTRAP_MIRROR:-$UBUNTUTOOLS_UBUNTU_MIRROR}
  fi
  mk-sbuild "${DIST}" --skip-security --type=file --debootstrap-include=default-jdk || sbuild-update -udc "${DIST}"-amd64
fi

mvn -B versions:set -DnewVersion="${VERSION}" -DgenerateBackupPoms=false
"${PROJECT_DIR}/resources/deb-gen-source.sh" "${VERSION}" "${DIST}"
export SBUILD_CONFIG="${PROJECT_DIR}/resources/sbuildrc"
if [[ "${ARCH}" != "amd64" ]]; then
  sbuild --dist "${DIST}" --no-arch-all --host "${ARCH}" "${PROJECT_DIR}"/../jitsi_*.dsc
else
  sbuild --dist "${DIST}" --arch-all "${PROJECT_DIR}"/../jitsi_*.dsc
  cp "${PROJECT_DIR}"/../jitsi_* "$BUILD_DIR"
fi

debsign -S -edev+maven@jitsi.org "${BUILD_DIR}"/*.changes --re-sign -p"${PROJECT_DIR}"/resources/gpg-wrap.sh

#make build files readable for Windows and archivable for GitHub Actions
rename 's|:|-|g' "$BUILD_DIR"/*.build
