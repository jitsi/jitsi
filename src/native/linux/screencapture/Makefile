##
# \file Makefile
# \brief libscreencapture makefile.
# \author Sebastien Vincent

CC = gcc
JNI_HEADERS = -I${JAVA_HOME}/include -I${JAVA_HOME}/include/linux
CFLAGS = -std=c99 -D_POSIX_C_SOURCE=200112L -D_XOPEN_SOURCE=600 -Wall -Wextra -pedantic -Wstrict-prototypes -Wredundant-decls  $(JNI_HEADERS)

all: libscreencapture

libscreencapture: net_java_sip_communicator_impl_neomedia_imgstreaming_UnixScreenCapture.c
	$(CC) -shared -fPIC $(CFLAGS) -o libscreencapture.so -O $<

# To compile 32 bit library under 64 bit system
libscreencapture-32: net_java_sip_communicator_impl_neomedia_imgstreaming_UnixScreenCapture.c
	$(CC) -shared -m32 $(CFLAGS) -o libscreencapture.so -O $<

clean:
	rm -f *.o *.so
