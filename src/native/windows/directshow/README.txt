JDirectShow
============

1. Requirements

- Microsoft C++ compiler from Microsoft Platform SDK or Visual Studio (express or not);

Open Visual C++ command line,

for 32-bit compilation with Visual Studio:
C:\Program Files\Microsoft Visual Studio 9.0\VC\bin\vcvars32.bat

for 64-bit compilation with Visual Studio:
C:\Program Files (x86)\Microsoft Visual Studio 9.0\VC\bin\vcvars64.bat

Set environment variable with Java SDK path:
set JAVA_HOME=C:\Progra~1\Java\jdk1.6.0_20

2. Build instructions

Go to jdirectshow native source directory:
cd \Path\to\sip-communicator\src\native\windows\directshow

Build and install:
for 32-bit:
nmake install32

for 64-bit:
nmake install64

