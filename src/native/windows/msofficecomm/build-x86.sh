#!/bin/sh

CC=i686-w64-mingw32-gcc
JAVA_HOME=$HOME/Downloads/jdk-7u4-windows-i586
OCSDK_HOME=$HOME/Downloads/OCSDK
WINVER=0x0501

BASE_DIR=`dirname $0`
if [ "x${BASE_DIR}" = "x" ]; then
    BASE_DIR=.
else
    BASE_DIR=`cd ${BASE_DIR}/../../../../ && pwd`
fi
SRC_DIR=${BASE_DIR}/src/native/windows/msofficecomm
STRIP=`echo ${CC} | sed 's/gcc/strip/'`
TARGET=${BASE_DIR}/lib/native/windows/jmsofficecomm.dll

for CXX_FILE in ${SRC_DIR}/*.cxx; do
    ${CC} \
        -c \
        -D_JNI_IMPLEMENTATION_ -D_UNICODE -DUNICODE -D_WIN32_WINNT=${WINVER} -DWINVER=${WINVER} \
        -fno-exceptions -fno-rtti \
        -I${JAVA_HOME}/include -I${JAVA_HOME}/include/win32 -I${OCSDK_HOME} \
        -O2 \
        -Wall \
        -x 'c++' \
        ${CXX_FILE} \
        -o ${SRC_DIR}/`basename ${CXX_FILE} .cxx`.o
done
${CC} \
        -shared \
        -Wl,--kill-at \
        ${SRC_DIR}/*.o \
        ${SRC_DIR}/*.res \
        -o ${TARGET} \
        -lole32 -loleaut32 -luuid
${STRIP} -x ${TARGET}
