PORTAUDIO_HOME?=/home/lubomir/src/portaudio
JAVA_HOME?=/usr/lib/jvm/java-6-sun
SPEEX_HOME?=/home/lubomir/src/speex-1.2rc1

TARGET=../../../lib/native/linux/libjportaudio.so

CC=gcc
CPPFLAGS=-DJNI_IMPLEMENTATION \
         -Wall -Wreturn-type \
         -I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/linux \
         -I$(SPEEX_HOME)/include \
         -I$(PORTAUDIO_HOME)/include
LDFLAGS=-shared -fPIC
LIBS=-L$(SPEEX_HOME)/libspeex/.libs -L$(PORTAUDIO_HOME)/lib/.libs \
     -Wl,-Bstatic -lportaudio -lspeexdsp -Wl,-Bdynamic -lasound -lm -lpthread

$(TARGET): net_java_sip_communicator_impl_neomedia_portaudio_PortAudio.c AudioQualityImprovement.c net_java_sip_communicator_impl_neomedia_portaudio_PortAudio.h AudioQualityImprovement.h
	$(CC) $(CPPFLAGS) $^ $(LDFLAGS) -o $@ $(LIBS)
