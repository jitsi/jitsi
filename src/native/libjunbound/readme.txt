To compile libjunbound

Linux (libunbound, libldns and lcrypto shared):
----------------------------------------------
a) With autoconf
autoreconf --install
./configure
make
cp ./libs/libjunbound.so ../../lib/native/linux

b) With manual makefile
make -f makefile.linux

Linux (libunbound statically linked, lcrypto shared):
----------------------------------------------------
Use make-linux.sh

Mac:
---
Use make-mac.sh

Windows:
-------
- Get ldns, libexpat, unbound and compile them
- make -f makefile.w32

Expat Win64:
./configure --host=x86_64-w64-mingw32 --build=x86_64-w64-mingw32

Unbound Win64:
The configure script wrongly detects getaddrinfo as Unix. Break the detection
script at line 16085 (1.4.12)

OpenSSL Win64:
configure mingw64 -no-capieng -no-asm
make
make check (http://stackoverflow.com/questions/7256087/error-compiling-openssl-with-mingw-msys)
mkdir lib
cp libcrypto.a lib
cp libssl.a lib
