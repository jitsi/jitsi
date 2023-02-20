#!/usr/bin/env bash
LIBJITSI_MVN_VER=$(xmllint --xpath "//*[local-name()='dependency' and ./*[local-name()='artifactId' and text()='libjitsi']]/*[local-name()='version']/text()" pom.xml)
RELEASE=$(lsb_release -rs)
SUITE=$(lsb_release -cs)
echo "$LIBJITSI_MVN_VER-$RELEASE~$SUITE"
