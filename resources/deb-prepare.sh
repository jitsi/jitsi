#!/usr/bin/env bash
set -e
set -x
sudo apt-get update
sudo apt-get install -y distro-info

# dev-tools from backports because https://bugs.launchpad.net/ubuntu/+source/ubuntu-dev-tools/+bug/1916633
DEVTOOLS_PACKAGE=ubuntu-dev-tools
UBUNTUTOOLS_PACKAGE=python3-ubuntutools
if lsb_release -i -s == "Ubuntu"; then
  case $(lsb_release -c -s) in
    bionic | focal)
      DEVTOOLS_PACKAGE=$DEVTOOLS_PACKAGE/$(lsb_release -c -s)-backports
      UBUNTUTOOLS_PACKAGE=$UBUNTUTOOLS_PACKAGE/$(lsb_release -c -s)-backports
      ;;
  esac
elif  lsb_release -i -s == "Debian"; then
  case $(lsb_release -c -s) in
    stretch | buster)
      >&2 echo "Packaging needs 'ubuntu-dev-tools' >= 1.181, which is only available in Bullseye"
      exit 1
      ;;
  esac
fi

sudo apt-get install -y \
  debhelper \
  aptitude \
  sbuild \
  schroot \
  "$DEVTOOLS_PACKAGE" \
  "$UBUNTUTOOLS_PACKAGE" \
  debian-archive-keyring \
  git-buildpackage \
  rename
sudo adduser $USER sbuild
