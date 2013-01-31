#!/bin/bash

if [[ "$1" == "--help" || "$1" == "-h" || "$1" == "-?" || $# -lt 3 ]]; then
    echo "Usage $0 rpmVersion rpmRelease svnRevisionNumber"
    exit 1
fi

rpmVersion=$1
rpmRelease=$2
svnRevisionNumber=$3
latestVersion=`echo $rpmRelease| sed  "s/build.[0-9]*/latest/g" `
#buildNumber=$(echo $rpmRelease | cut -d '.' -f 3,4 -)
buildNumber=$rpmRelease

rpmbuilddir=~/rpmbuild
SVN_REPO=https://svn.java.net/svn/jitsi~svn/trunk

dashIndex=`expr index $rpmVersion -`
if [[ $dashIndex != 0 ]]; then
    echo "ERROR: $rpmVersion should not contain the - character."
    echo "A correct rpmVersion is, for example: 1.0"
    exit 1
fi

dashIndex=`expr index $rpmRelease -`
if [[ $dashIndex != 0 ]]; then
    echo "ERROR: $rpmRelease should not contain the - character."
    echo "A correct rpmRelease is, for example: alpha3.nightly.build.1234"
    exit 1
fi


if [ ! -d "$rpmbuilddir/SOURCES" ]; then
    mkdir $rpmbuilddir/SOURCES
fi
if [ ! -d "$rpmbuilddir/SOURCES/jitsi" ]; then
    cd $rpmbuilddir/SOURCES
    svn --non-interactive checkout $SVN_REPO jitsi
fi


cd $rpmbuilddir/SOURCES/jitsi
svn --non-interactive update --revision $svnRevisionNumber
exitCode=$?; [[ $exitCode != 0 ]] && exit $exitCode

echo "Creating zip file"
cd $rpmbuilddir/SOURCES
rm -f jitsi-src*.zip
zipFileName=jitsi-src-${rpmVersion}-${rpmRelease/./-}.zip
zip -rq $zipFileName jitsi -x "jitsi/**/.svn/**/*" -x"jitsi/**/.svn/*" -x"jitsi/.svn/*" -x"jitsi/.svn/**/*"
exitCode=$?; [[ $exitCode != 0 ]] && exit $exitCode

sed -i \
-e "s@Version:\( *\).*@Version:\1$rpmVersion@" \
-e "s@Release:\( *\).*@Release:\1$rpmRelease@" \
-e "s@Source0:\( *\).*@Source0:\1http://download.jitsi.org/jitsi/nightly/src/$zipFileName@" \
-e "s@ant -Dlabel=.* rebuild@ant -Dlabel=$buildNumber rebuild@" \
$rpmbuilddir/SPECS/jitsi.spec

echo "=============[ Building i386 RPM package]============="
rm -f $rpmbuilddir/RPMS/i386/jitsi*.rpm
setarch i386 rpmbuild -bb $rpmbuilddir/SPECS/jitsi.spec
exitCode=$?; [[ $exitCode != 0 ]] && exit $exitCode
packagename32=jitsi-${rpmVersion}-${rpmRelease}.i386.rpm

echo "=============[ Building x86_64 RPM package]============="
rm -f $rpmbuilddir/RPMS/x86_64/jitsi*.rpm
setarch x86_64 rpmbuild -bb $rpmbuilddir/SPECS/jitsi.spec
exitCode=$?; [[ $exitCode != 0 ]] && exit $exitCode
packagename64=jitsi-${rpmVersion}-${rpmRelease}.x86_64.rpm
