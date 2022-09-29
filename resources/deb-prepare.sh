#!/usr/bin/env bash
set -e
set -x
if grep -qi 'Ubuntu' $(cat /etc/os-release); then
  ubuntu-os="/$(lsb_release -c -s)-backports"
fi
sudo apt-get update
# dev-tools from backports because https://bugs.launchpad.net/ubuntu/+source/ubuntu-dev-tools/+bug/1916633
sudo apt-get install -y \
  debhelper \
  aptitude \
  sbuild \
  schroot \
  ubuntu-dev-tools$(ubuntu-os) \
  python3-ubuntutools$(ubuntu-os) \
  debian-archive-keyring \
  git-buildpackage \
  rename \
  distro-info
sudo adduser $USER sbuild
