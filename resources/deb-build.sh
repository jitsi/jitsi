#!/usr/bin/env bash
if [ "$#" -ne 4 ]; then
    echo "Usage: $0 <VERSION> <DIST> <ARCH> [GPG_ID]"
    echo "  VERSION: Source package version, e.g. 2.14.123-gcaffee"
    echo "  DIST: Debian/Ubuntu distribution name (e.g. focal or bullseye)"
    echo "  ARCH: Architecture (e.g. amd64, aarch64)"
    echo "  GPG_ID: id for package signing"
    exit 1
fi;

set -e
set -x
VERSION=$1
DIST=$2
ARCH=$3
GPG_ID=$4
PROJECT_DIR="$(realpath "$(dirname "$0")/../")"
cd "${PROJECT_DIR}" || exit
# export for sbuildrc sourcing
export BUILD_DIR=${PROJECT_DIR}/target/debian/${DIST}
mkdir -p "${BUILD_DIR}"

# use tmpfs for sbuild
sudo tee -a /etc/fstab < "${PROJECT_DIR}/resources/sbuild-tmpfs"
sudo tee -a /etc/schroot/sbuild/fstab <<END
/var/cache/m2-sbuild /var/cache/m2-sbuild none rw,bind 0 0
END

export SBUILD_CONFIG="${PROJECT_DIR}/resources/sbuildrc"
SBUILD_ARGS=(\
  --dist="${DIST}" \
  --extra-repository='deb https://nexus.ingo.ch/jitsi-desktop-unstable/ '"${DIST}"'/'
  "--extra-repository-key=${PROJECT_DIR}/resources/jitsi-desktop-key.asc"
  )

if [[ "${ARCH}" != "amd64" ]]; then
  SBUILD_ARGS+=(--host="${ARCH}")
  if [ ! -f /var/lib/schroot/tarballs/"${DIST}"-amd64-"${ARCH}".tgz ]; then
    mk-sbuild "${DIST}" --target "${ARCH}" --type=file || true
  fi

  # union-type= is not valid for type=file, remove to prevent warnings
  sudo sed -i s/union-type=.*//g "/etc/schroot/chroot.d/sbuild-${DIST}-amd64-${ARCH}"
  sudo sbuild-update -ud "${DIST}"-amd64-"${ARCH}"
  if debian-distro-info --all | grep -Fqxi "${DIST}"; then
    SBUILD_ARGS+=(--extra-repository='deb http://ftp.debian.org/debian/ '"${DIST}"'-backports main')
  elif ubuntu-distro-info --all | grep -Fqxi "${DIST}"; then
    SBUILD_BACKPORTS_MIRROR=${DEBOOTSTRAP_MIRROR:-$UBUNTUTOOLS_UBUNTU_MIRROR}
    SBUILD_BACKPORTS_MIRROR=${SBUILD_BACKPORTS_MIRROR:-http://archive.ubuntu.com/ubuntu/}
    SBUILD_ARGS+=(--extra-repository='deb [arch=amd64] '"$SBUILD_BACKPORTS_MIRROR"' '"${DIST}"'-backports main universe')
    SBUILD_ARGS+=(--extra-repository='deb [arch='"${ARCH}"'] http://ports.ubuntu.com/ubuntu-ports/ '"${DIST}"'-backports main universe')
  fi
else
  if debian-distro-info --all | grep -Fqxi "${DIST}"; then
    SBUILD_BACKPORTS_MIRROR=${UBUNTUTOOLS_DEBIAN_MIRROR:-http://ftp.debian.org/debian/}
    export DEBOOTSTRAP_MIRROR=${DEBOOTSTRAP_MIRROR:-$UBUNTUTOOLS_DEBIAN_MIRROR}
    SBUILD_ARGS+=(--extra-repository='deb [arch=amd64] '"$SBUILD_BACKPORTS_MIRROR"' '"${DIST}"'-backports main')
  elif ubuntu-distro-info --all | grep -Fqxi "${DIST}"; then
    SBUILD_BACKPORTS_MIRROR=${DEBOOTSTRAP_MIRROR:-$UBUNTUTOOLS_UBUNTU_MIRROR}
    SBUILD_BACKPORTS_MIRROR=${SBUILD_BACKPORTS_MIRROR:-http://archive.ubuntu.com/ubuntu/}
    export DEBOOTSTRAP_MIRROR=${DEBOOTSTRAP_MIRROR:-$UBUNTUTOOLS_UBUNTU_MIRROR}
    SBUILD_ARGS+=(--extra-repository='deb [arch=amd64] '"$SBUILD_BACKPORTS_MIRROR"' '"${DIST}"'-backports main universe')
  fi

  if [ ! -f /var/lib/schroot/tarballs/"${DIST}"-amd64.tgz ]; then
    mk-sbuild "${DIST}" --type=file || true
  fi

  # union-type= is not valid for type=file, remove to prevent warnings
  sudo sed -i s/union-type=.*//g "/etc/schroot/chroot.d/sbuild-${DIST}-amd64"
  sudo sbuild-update -v -ud "${DIST}"-amd64
fi

mvn -B versions:set -DnewVersion="${VERSION}" -DgenerateBackupPoms=false
"${PROJECT_DIR}/resources/deb-gen-source.sh" "${VERSION}" "${DIST}"
SBUILD_ARGS+=("${PROJECT_DIR}"/../jitsi_*.dsc)
if [[ "${ARCH}" != "amd64" ]]; then
  sbuild --no-arch-all "${SBUILD_ARGS[@]}"
else
  sbuild --arch-all "${SBUILD_ARGS[@]}"
  cp "${PROJECT_DIR}"/../jitsi_* "$BUILD_DIR"
fi

debsign -S -e"${GPG_ID}" "${BUILD_DIR}"/*.changes --re-sign -p"${PROJECT_DIR}"/resources/gpg-wrap.sh

#make build files readable for Windows and archivable for GitHub Actions
rename 's|:|-|g' "$BUILD_DIR"/*.build
