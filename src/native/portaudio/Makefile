PORTAUDIO_HOME=/home/damencho/dev/portaudio/portaudio
JAVA_HOME=/usr/lib/jvm/java-6-sun

ARCH=$(shell uname -m | sed -e s/x86_64/-64/ -e s/i.86//)
TARGET=../../../lib/native/linux$(ARCH)/libjportaudio.so

CC=gcc
CPPFLAGS=-DJNI_IMPLEMENTATION \
         -Wall -Wreturn-type \
         -I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/linux \
         -I$(PORTAUDIO_HOME)/include
LDFLAGS=-shared
LIBS=-L$(PORTAUDIO_HOME)/lib/.libs -lasound -lm -lpthread -ljack -static -lportaudio

$(TARGET): net_java_sip_communicator_impl_media_protocol_portaudio_PortAudio.c net_java_sip_communicator_impl_media_protocol_portaudio_PortAudio.h
	$(CC) $(CPPFLAGS) $< $(LDFLAGS) -o $@ $(LIBS)
