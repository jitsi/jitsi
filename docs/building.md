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
compatible downloaded Jitsi Desktop build into `lib/native/<platform>`.

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
cmake -B cmake-build
cmake --build cmake-build --target install
```

## Installers

Create the .msi/.dmg/.deb binaries by invoking the respective Gradle targets
in `resources/install`. These commands assume that the Java and JNI libraries
have been built.

- Windows: `gradlew windowsZip signMsi -Papplication.target=x64`
- MacOS: `./gradlew createDmg`
- Linux: `./gradlew buildDeb -Papplication.target=x64`
