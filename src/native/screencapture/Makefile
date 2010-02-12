##
# \file Makefile
# \brief libscreencapture makefile.
# \author Sebastien Vincent

CC = gcc
CFLAGS = -std=c99 -D_POSIX_C_SOURCE=200112L -D_XOPEN_SOURCE=600 -Wall -Wextra -pedantic -Wstrict-prototypes -Wredundant-decls -O3

# uncomment to compile on Linux
LDFLAGS =
CFLAGS += -fPIC
JNI_HEADERS = -I${JAVA_HOME}/include -I${JAVA_HOME}/include/linux
TARGET = libscreencapture.so 

# uncomment to compile on Windows
#LDFLAGS = -lgdi32
#JNI_HEADERS = -I${JAVA_HOME}/include -I${JAVA_HOME}/include/win32
#CFLAGS += -Wl,--kill-at 
#TARGET = screencapture.dll

# uncomment to compile on Mac OS X
#CFLAGS += -fPIC -arch i386 -arch x86_64 -arch ppc -I/System/Library/Frameworks/ApplicationServices.framework/Headers
#LDFLAGS = -framework ApplicationServices
#JNI_HEADERS = -I/System/Library/Frameworks/JavaVM.framework/Headers 
#TARGET = libscreencapture.jnilib 

# uncomment to compile on FreeBSD
#CFLAGS += -fPIC -I/usr/local/include
#LDFLAGS =
#JNI_HEADERS = -I${JAVA_HOME}/include -I${JAVA_HOME}/include/freebsd
#TARGET = libscreencapture.so 

CFLAGS += $(JNI_HEADERS)

all: libscreencapture

libscreencapture: net_java_sip_communicator_impl_neomedia_imgstreaming_NativeScreenCapture.c
	$(CC) -shared $(CFLAGS) -o $(TARGET) -O $< $(LDFLAGS)

# To compile 32 bit library under 64 bit system
# Note: On Windows, cross-compilation is supported from gcc-4.5
libscreencapture-32: net_java_sip_communicator_impl_neomedia_imgstreaming_NativeScreenCapture.c
	$(CC) -shared -m32 $(CFLAGS) -o $(TARGET) -O $< $(LDFLAGS)

clean:
	rm -f *.o *.so *.dll

