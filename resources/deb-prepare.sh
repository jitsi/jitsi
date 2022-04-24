#!/usr/bin/env bash
set -e
set -x
sudo apt-get update -
# dev-tools from backports because https://bugs.launchpad.net/ubuntu/+source/ubuntu-dev-tools/+bug/1916633
sudo apt-get install -y \
  debhelper \
  sbuild \
  schroot \
  ubuntu-dev-tools/$(lsb_release -c -s)-backports \
  python3-ubuntutools/$(lsb_release -c -s)-backports \
  debian-archive-keyring \
  git-buildpackage \
  rename \
  distro-info
sudo adduser $USER sbuild
