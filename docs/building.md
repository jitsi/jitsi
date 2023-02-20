# Building Jitsi Desktop

Jitsi Desktop is a complex project with many dependencies. As long you only want
to hack in the protocols and the UI, the setup is not too complicated. But as
soon as you need to also modify dependencies, it gets a little more complicated.

## Prerequisites

- Git
- JDK 11 (64bit)
- Maven 3.6 (or newer)
- CMake 3.10 (or newer)
    - Windows: Visual Studio (Community should suffice) with Windows 10 SDK
    - Linux: compilers from `build-essentials` and dev-libraries (see below)
    - macOS: XCode

## Building

These are simplified instruction that should get you up and running. Have a look
at the GitHub Actions `installers.yml` file that builds the releases for
official downloads with more detailed steps, e.g. for the version numbers and
cross architecture builds.

### Java

Build the Java packages with `mvn package` in the project root directory.

### JNI libraries

Build the JNI libraries with CMake in the native directory, or, copy them from a
compatible downloaded Jitsi Desktop build into `lib/native/<platform>-<arch>`.

#### Windows

To build the JNI libraries for the Office integration, you need to SDKs from
Microsoft that are unfortunately cannot be included. You'll need to download,
extract and specify the path to them. These libraries are not strictly required,
skip them if you can't be bothered.

- Outlook 2010 MAPI Headers:
    - https://docs.microsoft.com/en-us/office/client-developer/outlook/mapi/how-to-install-mapi-header-files
    - https://www.microsoft.com/en-us/download/details.aspx?id=12905
- Office Communicator 2007 Automation API
    - http://www.microsoft.com/downloads/details.aspx?FamilyID=ed1cce45-cc22-46e1-bd50-660fe6d2c98c&displaylang=en
    - The link does not work anymore in 2021, another source is currently
      unknown. It might be possible to extract the IDL from the typelib of an
      old Office installation.

```shell
cmake -B cmake-build-x64 -A x64 ^
            -DOCSDK_DIR=C:\build\private-sdk\ocsdk2007 ^
            -DMAPI_HEADERS_DIR=C:\build\private-sdk\outlook2010mapi
cmake --build cmake-build-x64 --target install
```

### Linux

```shell
sudo apt-get update && sudo apt-get install -y \
            libdbus-1-dev \
            libxss-dev \
            libxtst-dev \
            libgtk2.0-dev \
            libx11-dev \
            libxext-dev \
            libxtst-dev

cmake -B cmake-build
cmake --build cmake-build --target install
```

### MacOS

```shell
./resources/mac-cmake.sh $JAVA_HOME x86-64
```

## Installers (Windows, Mac)

Create the .msi/.dmg binaries by invoking the respective Gradle targets
in `resources/install`. These commands assume that the Java and JNI libraries
have been built.

- Windows: `gradlew windowsZip signMsi`
- MacOS: `./gradlew createDmg`

Change or set the property `application.target` if not building for `x86-64`.
Valid values are:

- `x86` (Windows)
- `x86-64` (Windows, Mac)

## Packages (Debian/Ubuntu)

- Install the required tools (see `resources/deb-prepare.sh`)
- Set a static version with
  `mvn -B versions:set -DnewVersion="2.14.123-gcaffee" -DgenerateBackupPoms=false`
- Generate the source package with `resources/deb-gen-source.sh`
- Build the package using your preferred tool (e.g. `sbuild` or `pbuilder`).
  Note that the packaging requires network access to download Maven packages.

  See `resources/deb-build.sh` for guidance, this script is used by GitHub
  Actions to generate the packages.
