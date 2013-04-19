#!/bin/bash
#-------------
#Note: To compile the PPC support, you must have XCode 3.x installed!
#      Remove the "-arch ppc" arguments to build only for 32/64 bit
#-------------

set -e

out=`pwd`/build/mac
prefix=$out/libs

mkdir -p $out
mkdir -p $prefix/lib
#mkdir -p $prefix/include

cd $out

expat=expat-2.0.1
ldns=ldns-1.6.11
unbound=unbound-1.4.14

curl -L http://downloads.sourceforge.net/project/expat/expat/2.0.1/expat-2.0.1.tar.gz -o $expat.tar.gz
curl -L http://nlnetlabs.nl/downloads/ldns/$ldns.tar.gz -o $ldns.tar.gz
curl -L http://unbound.net/downloads/$unbound.tar.gz -o $unbound.tar.gz

tar -xzvf $expat.tar.gz
tar -xzvf $ldns.tar.gz
tar -xzvf $unbound.tar.gz

mv $expat expat
mv $ldns ldns
mv $unbound unbound

export MACOSX_DEPLOYMENT_TARGET=10.5
export CC="/usr/bin/gcc -arch i386 -arch x86_64 -mmacosx-version-min=10.5"
export CPP="/usr/bin/gcc -E"
    
function build_arch {
    prefixarch="${prefix}_$1"
    mkdir -p $prefixarch

    cd $out/$2

    ./configure --with-ssl=/usr --disable-gost --with-pic --with-ldns=$prefixarch --with-expat=$prefixarch --prefix=$prefixarch

    make clean
    make
    make install
}

function build_lib {
    build_arch all $1
}

build_lib expat
build_lib ldns
build_lib unbound

cp -r ${prefix}_all/include $prefix/
# remove all dynamic libs as we do not use them and compile is first searching
# for them
rm ${prefix}_all/lib/*.dylib
cd $out
g++ -mmacosx-version-min=10.4 -arch x86_64 -arch i386 \
 $out/../../src/net_java_sip_communicator_impl_dns_UnboundApi.cpp \
 -D_JNI_IMPLEMENTATION_ \
 -fPIC -shared -O2 -Wall \
 -I/System/Library/Frameworks/JavaVM.framework/Versions/Current/Headers \
 -I${prefix}_all/include \
 -L${prefix}_all/lib \
 -L/usr/lib \
 -dynamiclib \
 -lunbound -lldns -lcrypto \
 -dynamic \
 -lcrypto -lssl \
 -o libjunbound.jnilib
