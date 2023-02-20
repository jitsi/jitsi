[![Java Version Builds](https://github.com/jitsi/jitsi/actions/workflows/java.yml/badge.svg?branch=new-build)](https://github.com/jitsi/jitsi/actions/workflows/java.yml)
[![Installers](https://github.com/jitsi/jitsi/actions/workflows/installers.yml/badge.svg?branch=new-build)](https://github.com/jitsi/jitsi/actions/workflows/installers.yml)

Jitsi Desktop
=============

Jitsi Desktop is a free open-source audio/video and chat communicator that
supports protocols such as SIP, XMPP/Jabber, IRC and many other useful features.

Please do not confuse this project
with [Jitsi Meet](https://github.com/jitsi/jitsi-meet), the online video
conferencing solution with a free instance at https://meet.jit.si.

<img align=right src="docs/jitsi-main-window.png" alt="Jitsi Desktop" width=210>

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

#### Debian/Ubuntu
An APT repository is available at https://nexus.ingo.ch/jitsi-desktop/.
Note the trailing slash at the end of the distro-name.
This is required since the repository has no components.
```deb
deb https://nexus.ingo.ch/jitsi-desktop-unstable/ <distro>/
```

#### RPM Distros

Sorry, there are currently no rpm packages available.

### Snapshots

Snapshot or pre-release builds are also available in additional repositories.

- Windows and macOS: See https://github.com/jitsi/jitsi/releases
- Debian/Ubuntu: https://nexus.ingo.ch/jitsi-desktop-unstable/

## Helpful Resources

- [Documentation Wiki](https://github.com/jitsi/jitsi/wiki)
- [Community Forums](https://community.jitsi.org/c/jitsi-desktop/)
- [The old website](https://desktop.jitsi.org)

## Contributing

Please, read the [contribution guidelines](CONTRIBUTING.md) before opening a new
issue or pull request.
