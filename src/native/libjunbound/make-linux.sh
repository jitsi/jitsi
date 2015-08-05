#!/bin/bash
set -e

out=`pwd`/build/linux
prefix=$out/libs
export JAVA_HOME=/usr/lib/jvm/default-java/

mkdir -p $out
mkdir -p $prefix

cd $out

expat=expat-2.1.0
unbound=unbound-1.5.1

wget -nc http://downloads.sourceforge.net/project/expat/expat/2.1.0/$expat.tar.gz
wget -nc http://unbound.net/downloads/$unbound.tar.gz

tar -xzvf $expat.tar.gz
tar -xzvf $unbound.tar.gz

cd $out/$expat
./configure --with-pic --prefix=$prefix
make
make install

cd $out/$unbound
patch -p 1 -i $out/../../unbound.patch
./configure --with-pic --prefix=$prefix --with-libexpat=$prefix
make
make install

cd $out
gcc $out/../../src/net_java_sip_communicator_impl_dns_UnboundApi.cpp -fpic -shared -o libjunbound.so -I$JAVA_HOME/include -Wl,-Bstatic -L$prefix/lib -lunbound -I$prefix/include -Wl,-Bdynamic -lcrypto -lssl
strip libjunbound.so
