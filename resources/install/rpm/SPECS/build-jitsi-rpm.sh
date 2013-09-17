#!/bin/bash -x

SCRIPT_DIR="$(dirname "$(readlink -f "$0")")"
#exec > "${0%.*}.log" 2>&1

#exec -x

if [[ "$1" == "--help" || "$1" == "-h" || "$1" == "-?" || $# -lt 2 ]]; then
    echo "Usage $0 VERSION BUILD_NUMBER"
    exit 1
fi

version=$1
buildNumber=$2

# Deletes everything but the newest files matching the specified pattern
clean_oldies() {
    pattern="$1"
    leaveCount="$2"
    
    fileCount=$(ls -1 $pattern | wc -l)
    tailCount=$((fileCount - leaveCount))

    (( $tailCount < 0 )) && tailCount=0

    ls -t $pattern | tail -$tailCount | xargs rm -f
}

cd $SCRIPT_DIR/SOURCES

[[ ! -d jitsi ]] && git clone https://github.com/jitsi/jitsi
cd jitsi
git stash
git pull --rebase
git stash pop

echo "Creating zip file"
cd $SCRIPT_DIR/SOURCES
rm -f jitsi-src*.zip
zipFileName=jitsi-src-${version}-${buildNumber/./-}.zip
zip -rq $zipFileName jitsi -x 'jitsi/.git/* jitsi/.gitignore'

sed -i \
-e "s@Version:\( *\).*@Version:\1$version@" \
-e "s@Release:\( *\).*@Release:\1$buildNumber@" \
-e "s@Source0:\( *\).*@Source0:\1http://download.jitsi.org/jitsi/nightly/src/$zipFileName@" \
-e "s@ant -Dlabel=.* rebuild@ant -Dlabel=$buildNumber rebuild@" \
$SCRIPT_DIR/SPECS/jitsi.spec

rm -f $SCRIPT_DIR/RPMS/i686/jitsi*.rpm
echo "=============[ Building i686 RPM package ]============="
# Note! rpmbuild --target is known not to work correctly, so we use setarch
setarch i686 rpmbuild -bb $SCRIPT_DIR/SPECS/jitsi.spec
packagename32=jitsi-${version}-${buildNumber}.i686.rpm

rm -f $SCRIPT_DIR/RPMS/x86_64/jitsi*.rpm
echo "=============[ Building x86_64 RPM package ]============="
# Note! rpmbuild --target is known not to work correctly, so we use setarch
setarch x86_64 rpmbuild -bb $SCRIPT_DIR/SPECS/jitsi.spec
packagename64=jitsi-${version}-${buildNumber}.x86_64.rpm

mkdir -p $SCRIPT_DIR/RPMS/common

cp $SCRIPT_DIR/RPMS/i686/$packagename32 $SCRIPT_DIR/RPMS/common/
cp $SCRIPT_DIR/RPMS/x86_64/$packagename64 $SCRIPT_DIR/RPMS/common/
clean_oldies "$SCRIPT_DIR/RPMS/common/jitsi*.rpm" 20

createrepo --database --deltas $SCRIPT_DIR/RPMS/common/
