#!/usr/bin/env bash
set -e
set -x
cd "$(realpath "$(dirname "$0")/../")"
VERSION=$1
DIST=$2
SINCE=$(git describe --match "v[0-9\.]*" --abbrev=0)
if debian-distro-info --all | grep -Fqxi "${DIST}"; then
    DIST_VERSION=$(debian-distro-info --series="${DIST}" -r)
elif ubuntu-distro-info --all | grep -Fqxi "${DIST}"; then
    DIST_VERSION=$(ubuntu-distro-info --series="${DIST}" -r)
    # strip LTS suffix if present
    DIST_VERSION="${DIST_VERSION%%\ *}"
fi

FULL_VERSION="${VERSION}-${DIST_VERSION}~${DIST}"

rm -rf debian/javah
cp -r target/native/javah debian/ || (echo "Need pre-compiled javah files, run 'mvn compile' first" && exit 1)

export DEBFULLNAME="$GITHUB_ACTOR via GitHub Actions"
export DEBEMAIL="dev@jitsi.org"
git config --local user.name "$GITHUB_ACTOR via GitHub Actions"
git config --local user.email "$DEBEMAIL"
gbp dch \
  --ignore-branch \
  --since "${SINCE}" \
  --meta \
  --release \
  --distribution="${DIST}" \
  --force-distribution \
  --spawn-editor=never \
  --new-version="${FULL_VERSION}"
dpkg-source -I.git -I.target -b .

# Manually create the changes file instead of using sbuild --source-only-changes
# as sbuild would include _amd64.buildinfo in the source.changes. This confuses
# mini-dinstall because the buildinfo file is also referenced in _amd64.changes,
# so either of the two files fail to install.
dpkg-genchanges -S > ../jitsi_"${FULL_VERSION}"_source.changes
cat ../jitsi_"${FULL_VERSION}"_source.changes
