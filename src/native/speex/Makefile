JAVA_HOME?=/System/Library/Frameworks/JavaVM.framework/Versions/1.5/Home
SPEEX_HOME?=/Users/Lubomir/src/speex-1.2rc1

TARGET=../../../lib/native/mac/libjspeex.jnilib

CC=gcc -arch i386 -arch ppc -arch x86_64 -mmacosx-version-min=10.4
CPPFLAGS=-DJNI_IMPLEMENTATION \
         -Wall -Wreturn-type \
         -I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/linux \
         -I$(SPEEX_HOME)/include
LDFLAGS=-dynamiclib
LIBS=-L$(SPEEX_HOME)/libspeex/.libs -dynamic -lspeex -lspeexdsp -lm

$(TARGET): net_java_sip_communicator_impl_neomedia_codec_audio_speex_Speex.c net_java_sip_communicator_impl_neomedia_codec_audio_speex_Speex.h
	$(CC) $(CPPFLAGS) $< $(LDFLAGS) -o $@ $(LIBS)
