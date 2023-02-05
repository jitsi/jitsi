# Developing and Debugging Jitsi Desktop

Follow the instructions for [building](building.md) Jitsi Desktop to get the
prerequisites up and running.

## Tools

To develop the Java Code, it's easiest to
use [IntelliJ IDEA](https://www.jetbrains.com/idea/download).
[CLion](https://www.jetbrains.com/clion/) works well on all platforms for the
JNI libraries, but any other IDE with CMake support should do as well.

## Debugging in IntelliJ

Open the directory containing the Jitsi Desktop source code as a Maven project,
then create a launch configuration with the following options:

- Classpath: `jitsi-launcher` project
- Main class: `net.java.sip.communicator.launcher.Jitsi`
- System properties:
  ```
  -Xbootclasspath/a:./lib
  -Dlogback.configurationFile=./lib/logback.xml
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

After running `mvn package -DskipTests`, run something along this:

```shell
java -cp "target/bundles/*:$(pwd)/lib/" `
  -Djava.library.path=$(pwd)/lib/native/linux-x64 `
  net.java.sip.communicator.launcher.Jitsi
```

To debug, use the standard Java debug arguments,
e.g. `-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=1234`
