#!/usr/bin/env bash
MVNVER=`mvn help:evaluate -Dexpression=project.version -q -DforceStdout`
MVNVER_MATCHER="^([0-9]+)\.([0-9]+)(\.([0-9]+))?(\.([0-9]+))?(-([A-Za-z0-9-]+))?"
if [[ $MVNVER =~ $MVNVER_MATCHER ]]; then
  MAJOR=${BASH_REMATCH[1]}
  MINOR=${BASH_REMATCH[2]}
  SUFFIX=${BASH_REMATCH[8]}
  echo "MAJOR=${MAJOR}"
  echo "MINOR=${MINOR}"
  echo "SUFFIX=${SUFFIX}"
  echo "jitsi_version_major=${MAJOR}" >> $GITHUB_OUTPUT
  echo "jitsi_version_minor=${MINOR}" >> $GITHUB_OUTPUT
  echo "jitsi_version_suffix=${SUFFIX}" >> $GITHUB_OUTPUT
else
  echo "$MVNVER did not match $MVNVER_MATCHER"
  exit 1
fi;

GITVER=`git describe --match Jitsi-[0-9.]* --long --dirty --always`
echo "GITVER=${GITVER}"
echo "jitsi_version_git=${GITVER}" >> $GITHUB_OUTPUT
GITVER_MATCHER="^((Jitsi-[0-9.]+)-([0-9]+)-)?([A-Za-z0-9-]+)$"
if [[ $GITVER =~ $GITVER_MATCHER ]]; then
  NCOMMITS=${BASH_REMATCH[3]}
  HASH=${BASH_REMATCH[4]}
  echo "NCOMMITS=${NCOMMITS}"
  echo "HASH=${HASH}"
  echo "jitsi_version_ncommits=${NCOMMITS}" >> $GITHUB_OUTPUT
  echo "jitsi_version_hash=${HASH}" >> $GITHUB_OUTPUT
else
  echo "$GITVER did not match $GITVER_MATCHER"
  exit 1
fi;

VERSION_SHORT="${MAJOR}.${MINOR}.${NCOMMITS}"
if [[ "$SUFFIX" == "" ]]; then
  VERSION_FULL="${VERSION_SHORT}+${HASH}"
elif [[ "$SUFFIX" == "SNAPSHOT" ]]; then
  VERSION_FULL="${VERSION_SHORT}-$(date -u '+%Y%m%d%H%M%S')+${HASH}"
  VERSION_DEB="${VERSION_SHORT}.$(date -u '+%Y%m%d%H%M%S')+${HASH}"
else
  VERSION_FULL="${VERSION_SHORT}-${SUFFIX}+${HASH}"
  VERSION_DEB="${VERSION_SHORT}.${SUFFIX}+${HASH}"
fi;
echo "VERSION_SHORT=${VERSION_SHORT}"
echo "VERSION_FULL=${VERSION_FULL}"
echo "VERSION_DEB=${VERSION_DEB}"
echo "jitsi_version_short=${VERSION_SHORT}" >> $GITHUB_OUTPUT
echo "jitsi_version_full=${VERSION_FULL}" >> $GITHUB_OUTPUT
echo "jitsi_version_deb=${VERSION_DEB}" >> $GITHUB_OUTPUT
