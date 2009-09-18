PORTAUDIO_HOME=/home/damencho/dev/portaudio/portaudio
JAVA_HOME=/usr/lib/jvm/java-6-sun

TARGET=../../../lib/native/linux-64/libjportaudio.so

CC=gcc
CPPFLAGS=-DJNI_IMPLEMENTATION \
         -Wall -Wreturn-type \
         -I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/linux \
         -I$(PORTAUDIO_HOME)/include
LDFLAGS=-shared -fPIC
LIBS=-L$(PORTAUDIO_HOME)/lib/.libs -lportaudio

$(TARGET): net_java_sip_communicator_impl_media_protocol_portaudio_PortAudio.c net_java_sip_communicator_impl_media_protocol_portaudio_PortAudio.h
	$(CC) $(CPPFLAGS) $< $(LDFLAGS) -o $@ $(LIBS)
