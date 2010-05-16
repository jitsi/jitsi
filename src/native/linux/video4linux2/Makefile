JAVA_HOME?=/usr/lib/jvm/java-6-sun

ARCH=$(shell uname -m | sed -e s/x86_64/-64/ -e s/i.86//)
TARGET=../../../../lib/native/linux$(ARCH)/libjvideo4linux2.so

CC=gcc
CPPFLAGS=-DJNI_IMPLEMENTATION \
         -fPIC \
         -Wall -Wreturn-type \
         -I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/linux
LDFLAGS=-shared
LIBS=

$(TARGET): net_java_sip_communicator_impl_neomedia_jmfext_media_protocol_video4linux2_Video4Linux2.c net_java_sip_communicator_impl_neomedia_jmfext_media_protocol_video4linux2_Video4Linux2.h
	$(CC) $(CPPFLAGS) $< $(LDFLAGS) -o $@ $(LIBS)
