#!/bin/bash

TEMP=`getopt -o V --long version -n 'SIP Communicator' -- "$@"`

if [ $? != 0 ] ; then echo "Terminating..." >&2 ; exit 1 ; fi

eval set -- "$TEMP"

while true ; do
		case "$1" in
				-V|--version) echo "SIP Communicator version 1.0-alpha1"; exit 0;;
				--) shift ; break ;;
				*) echo "Internal error!" ; exit 1 ;;
		esac
done

javabin=`which java`

SCDIR=/usr/share/sip-communicator
LIBPATH=$SCDIR/lib

export LD_PRELOAD=/usr/lib/libaoss.so

cd $SCDIR

if [ -f $javabin ]
then
		$javabin -classpath "$LIBPATH/BrowserLauncher2.jar:$LIBPATH/JainSipApi1.2.jar:$LIBPATH/JainSipRi1.2.jar:$LIBPATH/Stun4J.jar:$LIBPATH/cindy.jar:$LIBPATH/commons-logging.jar:$LIBPATH/concurrent.jar:$LIBPATH/felix.jar:$LIBPATH/jml-1.0a3.jar:$LIBPATH/joscar-0.9.4-cvs-bin.jar:$LIBPATH/kxml-min.jar:$LIBPATH/nist-sdp-1.0.jar:$LIBPATH/oscar-aim.jar:$LIBPATH/retroweaver-rt.jar:$LIBPATH/retroweaver.jar:$LIBPATH/servicebinder.jar:$LIBPATH/sip-sdp.jar:$LIBPATH/smack.jar:$LIBPATH/smackx.jar" -Dicq.custom.message.charset=windows-1251 -Dfelix.config.properties=file:$LIBPATH/felix.client.run.properties -Djava.util.logging.config.file=$LIBPATH/logging.properties org.apache.felix.main.Main
		exit $?
fi
