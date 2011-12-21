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
Install libexpoat, libldns and unbound to a separate directory ($libdir)

expat:
./configure --with-pic --prefix=$libdir && make && make install

ldns:
./configure --with-pic --prefix=$libdir && make && make install

unbound:
// Rename context_new in libunbound/context.c, libundounb/context.h and
// libunbound.c to context_create (or you'll get 'out of memory' errors). Or use
// unbound.patch
./configure --with-pic --prefix=$libdir --with-ldns=$libdir --with-libexpat=$libdir && make && make install

libjunbound:
gcc src/net_java_sip_communicator_util_dns_UnboundApi.cpp -fpic -shared -o libjunbound.so -I/usr/lib/jvm/java-6-openjdk/include -Wl,-Bstatic -L$libdir/lib -lunbound -lldns -I$libdir/include -Wl,-Bdynamic -lcrypto


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


Mac:
---
Same as Linux, except:
libjunbound:
g++ src/net_java_sip_communicator_util_dns_UnboundApi.cpp -fpic -shared -o libjunbound.jnilib -I/System/Library/Frameworks/JavaVM.framework/Version/CurrentJDK/Home/include -I$libdir/include -L$libdir/lib -lunbound -lldns -lcrypto
