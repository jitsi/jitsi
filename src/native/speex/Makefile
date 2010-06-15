JAVA_HOME?=/usr/lib/jvm/java-6-sun
SPEEX_HOME?=/home/lubomir/src/speex-1.2rc1

TARGET=../../../lib/native/linux/libjspeex.so

CC=gcc
CPPFLAGS=-DJNI_IMPLEMENTATION \
         -Wall -Wreturn-type \
         -I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/linux \
         -I$(SPEEX_HOME)/include
LDFLAGS=-shared -fPIC
LIBS=-L$(SPEEX_HOME)/libspeex/.libs -Wl,-Bstatic -lspeex -lspeexdsp -Wl,-Bdynamic -lm

$(TARGET): net_java_sip_communicator_impl_neomedia_codec_audio_speex_Speex.c net_java_sip_communicator_impl_neomedia_codec_audio_speex_Speex.h
	$(CC) $(CPPFLAGS) $< $(LDFLAGS) -o $@ $(LIBS)
