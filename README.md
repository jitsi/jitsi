[![Java Version Builds](https://github.com/jitsi/jitsi/actions/workflows/java.yml/badge.svg?branch=new-build)](https://github.com/jitsi/jitsi/actions/workflows/java.yml)
[![Installers](https://github.com/jitsi/jitsi/actions/workflows/installers.yml/badge.svg?branch=new-build)](https://github.com/jitsi/jitsi/actions/workflows/installers.yml)
[![Hosted By: Cloudsmith](https://img.shields.io/badge/Debian%20package%20hosting%20by-cloudsmith-blue?logo=cloudsmith)](https://cloudsmith.com)

Jitsi Desktop
=============

Jitsi Desktop is a free open-source audio/video and chat communicator that
supports protocols such as SIP, XMPP/Jabber, IRC and many other useful features.

Please do not confuse this project
with [Jitsi Meet](https://github.com/jitsi/jitsi-meet), the online video
conferencing solution with a free instance at https://meet.jit.si.

<img style="float: right;" src="docs/jitsi-main-window.png" alt="Jitsi Desktop">

## Support

Jitsi Desktop is the heritage of Jitsi Meet. While some components are still
used in e.g. [Jigasi](https://github.com/jitsi/jigasi), the project is not
actively developed anymore. Improvements, bugfixes and builds are entirely based
on community contributions.

## Installation

### Releases

#### Windows and macOS

Download the installers
from [GitHub releases](https://github.com/jitsi/jitsi/releases/latest/).

#### Ubuntu

Ubuntu users can add
the [PPA](https://launchpad.net/~jitsi/+archive/ubuntu/jitsi-desktop)

```
sudo add-apt-repository ppa:jitsi/jitsi-desktop
sudo apt-get update
sudo apt-get install jitsi
```

#### Debian

Lacking an infrastructure like Ubuntu's PPA, Debian package repository hosting
is graciously provided by [Cloudsmith](https://cloudsmith.com).

```
curl -1sLf 'https://dl.cloudsmith.io/public/jitsi/jitsi-desktop/gpg.1BB13FEC36C8131E.key' | sudo apt-key add -
sudo add-apt-repository https://dl.cloudsmith.io/public/jitsi/jitsi-desktop/deb/debian
sudo apt-get update
sudo apt-get install jitsi
```

Or manually add the
line `deb https://dl.cloudsmith.io/public/jitsi/jitsi-desktop/deb/debian <distro> main`
to `/etc/apt/sources.list` if you don't have the
package `software-properties-common` that provides the
command `add-apt-repository`.

![Cloudsmith Logo](https://cloudsmith.com/img/cloudsmith-logo-dark.svg)

#### RPM Distros

Sorry, there are currently no rpm packages available.

### Snapshots

Snapshot or pre-release builds are also available in additional repositories.

- Windows and macOS: See https://github.com/jitsi/jitsi/releases
- Ubuntu: Use the snapshots ppa `ppa:jitsi/jitsi-desktop-snapshots`
- Debian: https://cloudsmith.io/~jitsi/repos/jitsi-desktop-snapshots

## Helpful Resources

- [Documentation Wiki](https://github.com/jitsi/jitsi/wiki)
- [Community Forums](https://community.jitsi.org/c/jitsi-desktop/)
- [The old website](https://desktop.jitsi.org)

## Contributing

Please, read the [contribution guidelines](CONTRIBUTING.md) before opening a new
issue or pull request.
