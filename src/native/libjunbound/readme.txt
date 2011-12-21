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
Install libexpat, libldns and unbound to a separate directory ($libdir)

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

Download libexpat, ldns and unbound.

Untar them in the same repository:
mkdir repos
tar -xzvf expat-2.0.1.tar.gz
tar -xzvf unbound-1.4.14.tar.gz
tar -xzvf ldns-1.6.11.tar.gz

Create prefix for i386, x86_64 and ppc:
mkdir prefix32 prefix64 prefixppc

First compile for i386:

libdir=/path/to/prefix32

cd expat-2.0.1
CC="gcc -arch i386" ./configure --with-pic --prefix=$libdir && make && make install

cd ../ldns-1.6.11
CC="gcc -arch i386" ./configure --disable-gost --with-pic --prefix=$libdir && make && make install

cd ../unbound-1.4.14
CC="gcc -arch i386" ./configure --with-pic --prefix=$libdir && make && make install

Then for x86_64:

libdir=/path/to/prefix64

cd expat-2.0.1
make clean
CC="gcc -arch x86_64" ./configure --with-pic --prefix=$libdir && make && make install

cd ../ldns-1.6.11
make clean
CC="gcc -arch x86_64" ./configure --disable-gost --with-pic --prefix=$libdir && make && make install

cd ../unbound-1.4.14
make clean
CC="gcc -arch x86_64" ./configure --with-pic --prefix=$libdir && make && make install

Finally for ppc:

libdir=/path/to/prefixppc

cd expat-2.0.1
make clean
CC="gcc -arch ppc" ./configure --with-pic --prefix=$libdir && make && make install

cd ../ldns-1.6.11
make clean
CC="gcc -arch ppc" ./configure --disable-gost --with-pic --prefix=$libdir && make && make install

cd ../unbound-1.4.14
make clean
CC="gcc -arch ppc" ./configure --with-pic --prefix=$libdir && make && make install

Combine the libraries:
mkdir -p prefixuniversal/lib

lipo -create prefix32/lib/libexpat.a prefix64/lib/libexpat.a prefixppc/lib/libexpat.a -output prefixuniversal/lib/libexpat.a
lipo -create prefix32/lib/libldns.a prefix64/lib/libldns.a prefixppc/lib/libldns.a -output prefixuniversal/lib/libldns.a
lipo -create prefix32/lib/libunbound.a prefix64/lib/libunbound.a prefixppc/lib/libunbound.a -output prefixuniversal/lib/libunbound.a
cp -r prefix32/include prefixuniversal/

libjunbound:
g++ -arch x86_64 -arch i386 -arch ppc src/net_java_sip_communicator_util_dns_UnboundApi.cpp -fpic -shared -o libjunbound.jnilib -I/System/Library/Frameworks/JavaVM.framework/Version/CurrentJDK/Home/include -I$libdir/include -L$libdir/lib -lunbound -lldns -lcrypto
