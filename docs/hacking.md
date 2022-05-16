# Developing and Debugging Jitsi Desktop

Follow the instructions for [building](building.md) Jitsi Desktop to get the
prerequisites up and running.

## Tools

To develop the Java Code, it's easiest to
use [IntelliJ IDEA](https://www.jetbrains.com/idea/download).
[CLion](https://www.jetbrains.com/clion/) works well on all platforms for the
JNI libraries, but any other IDE with CMake support should do as well.

- Make sure IntelliJ IDEA has the
  [OSGi plugin](https://plugins.jetbrains.com/plugin/1816-osgi)
  installed/enabled.
- Download the
  [Apache Felix Framework Distribution](https://felix.apache.org/downloads.cgi)
  and add it as an OSGi framework (Settings -> Languages & Frameworks -> OSGi)
- Configure the default bundle output
  in `Settings -> Languages & Frameworks -> OSGi project defaults`
  to `<projectdir>/target/bundles`

## Debugging in IntelliJ

Open the directory containing the Jitsi Desktop source code as a Maven project,
then create an OSGi launch configuration with the following properties:

- Select Felix as the Framework
- set the default- and framework start level to 5
- add all imported modules
- set the start level of the `jitsi-launcher` bundle to 1
- set the start level of `libjitsi` to 2
- pass the following additional options
  ```
  -Xbootclasspath/a:./lib
  -Dlogback.configurationFile=./lib/logback.xml
  -Dfelix.config.properties=file:./lib/felix-dev.properties
  -splash:./resources/install/src/main/dist/splash.gif
  -Djava.library.path=./lib/native/windows-x64
  ```

## Debugging dependencies

If you need to debug and develop e.g.
on [libjitsi](https://github.com/jitsi/libjitsi)
as well, add the project as an additional module and make sure the version in
both projects' `pom.xml` match _exactly_, e.g. with 1.2-SNAPSHOT. IntelliJ
recognizes this and replaces the Maven dependency with a project dependency.

## Debugging JNI libraries

To debug JNI libraries, set or prepend a custom JNI library path, e.g.
`-Djava.library.path=/dev/jitsi/jitsi-lgpl-dependencies/cmake-build/cmake/jnffmpeg;./lib/native/linux-x64`

## Running from the command line

After running `mvn package`, run something along this:

```shell
java -cp `cat target/launcher-classpath`:lib/:modules/launcher/target/classes `
  -Djava.library.path=$(pwd)/lib/native/linux-x64 `
  -Dfelix.config.properties=file:./lib/felix-dev.properties `
  net.java.sip.communicator.launcher.SIPCommunicator
```

To debug, use the standard Java debug arguments,
e.g. `-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=1234`
