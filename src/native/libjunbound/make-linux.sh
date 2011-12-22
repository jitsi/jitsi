#!/bin/bash
set -e

out=`pwd`/build/linux
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

cd $out/$expat
./configure --with-pic --prefix=$prefix
make
make install

cd $out/$ldns
./configure --with-pic --prefix=$prefix
make
make install

cd $out/$unbound
patch -p 1 -i $out/../../unbound.patch
./configure --with-pic --prefix=$prefix --with-libexpat=$prefix --with-ldns=$prefix
make
make install

cd $out
gcc $out/../../src/net_java_sip_communicator_util_dns_UnboundApi.cpp -fpic -shared -o libjunbound.so -I/usr/lib/jvm/java-6-openjdk/include -Wl,-Bstatic -L$prefix/lib -lunbound -lldns -I$prefix/include -Wl,-Bdynamic -lcrypto
strip libjunbound.so

