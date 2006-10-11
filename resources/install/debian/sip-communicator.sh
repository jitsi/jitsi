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

cd $SCDIR

if [ -f $javabin ]
then
		$javabin -classpath "$LIBPATH/BrowserLauncher2-10rc4.jar:$LIBPATH/Stun4J.jar:$LIBPATH/bcprov-jdk14-130.jar:$LIBPATH/concurrent.jar:$LIBPATH/joscar-0.9.4-cvs-bin.jar:$LIBPATH/junit.jar:$LIBPATH/log4j-1.2.8.jar:$LIBPATH/moduleloader.jar:$LIBPATH/oscar-aim.jar:$LIBPATH/oscar.jar:$LIBPATH/osgi.jar:$LIBPATH/retroweaver-rt.jar:$LIBPATH/retroweaver.jar:$LIBPATH/servicebinder.jar:$LIBPATH/sip-sdp.jar:$LIBPATH/smack.jar:$LIBPATH/smackx.jar:$LIBPATH/xalan-2.6.0.jar.ant" -Dicq.custom.message.charset=windows-1251 -Doscar.config.properties=file:$LIBPATH/oscar.client.run.properties -Djava.util.logging.config.file=$LIBPATH/logging.properties org.ungoverned.oscar.Main
		exit $?
fi
