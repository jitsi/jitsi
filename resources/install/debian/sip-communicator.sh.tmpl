#!/bin/bash

# Get architecture
ARCH=`uname -m | sed -e s/x86_64/64/ -e s/i.86/32/`

# Additionnal JVM arguments
CLIENTARGS=""

if [ $ARCH -eq 32 ]
then
    CLIENTARGS="-client -Xmx256m"
fi

javabin=`which java`

SCDIR=/usr/lib/sip-communicator
LIBPATH=$SCDIR/lib
CLASSPATH=$LIBPATH/jdic_stub.jar:$LIBPATH/jdic-all.jar:$LIBPATH/felix.jar:$LIBPATH/bcprovider.jar:$SCDIR/sc-bundles/sc-launcher.jar:$SCDIR/sc-bundles/util.jar
FELIX_CONFIG=$LIBPATH/felix.client.run.properties
LOG_CONFIG=$LIBPATH/logging.properties
COMMAND="$javabin $CLIENTARGS -classpath $CLASSPATH -Djna.library.path=$LIBPATH/native -Dfelix.config.properties=file:$FELIX_CONFIG -Djava.util.logging.config.file=$LOG_CONFIG net.java.sip.communicator.launcher.SIPCommunicator"

# set add LIBPATH to LD_LIBRARY_PATH for any sc natives (e.g. jmf .so's)
export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:$LIBPATH/native

# make LD_PRELOAD use libaoss so that java doesn't hog on the audio device.
export LD_PRELOAD=/usr/lib/libaoss.so

# create .sip-commnicator/log in home or otherwise java.util.logging will freak
mkdir -p $HOME/.sip-communicator/log

cd $SCDIR

exec $COMMAND $*
