#!/bin/bash
set -e

out=`pwd`/build/mac
prefix=$out/libs

mkdir -p $out
mkdir -p $prefix

cd $out

expat=expat-2.0.1
ldns=ldns-1.6.11
unbound=unbound-1.4.14

wget -nc http://downloads.sourceforge.net/project/expat/expat/2.0.1/$expat.tar.gz
wget -nc http://nlnetlabs.nl/downloads/ldns/$ldns.tar.gz
wget -nc http://unbound.net/downloads/$unbound.tar.gz

tar -xzvf $expat.tar.gz
tar -xzvf $ldns.tar.gz
tar -xzvf $unbound.tar.gz

mv $expat expat
mv $ldns ldns
mv $unbound unbound

function build_arch {
    CC="gcc -mmacosx-version-min=10.5 -arch $1"
    prefixarch="${prefix}_$1"
    mkdir -p $prefixarch

    cd $out/$2
    ./configure --with-pic --prefix=$prefixarch
    make clean
    make
    make install
}

function build_lib {
    #build each architecture
    build_arch i386 $1
    build_arch x86_64 $1
    build_arch ppc $1

    #Combine the libraries
    lipo -create ${prefix}_i386/lib/lib$1.a ${prefix}_x86_64/lib/lib$1.a ${prefix}_ppc/lib/lib$1.a -output ${prefix}/lib/lib$1.a
}

build_lib expat
build_lib ldns
build_lib unbound

cp -r ${prefix}_i386/include $prefix/
cd $out
g++ -mmacosx-version-min=10.5 -arch x86_64 -arch i386 -arch ppc $out/../../src/net_java_sip_communicator_util_dns_UnboundApi.cpp -fpic -shared -o $out/libjunbound.jnilib -I/System/Library/Frameworks/JavaVM.framework/Version/CurrentJDK/Home/include -I$prefix/include -L$prefix/lib -lunbound -lldns -lcrypto

