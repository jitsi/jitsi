JAVA_HOME?=/usr/lib/jvm/java-6-sun

TARGET=../../../lib/native/linux-64/libjg722.so

CC=gcc
CPPFLAGS=-DJNI_IMPLEMENTATION \
         -Wall -Wreturn-type \
         -I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/linux
LDFLAGS=-shared -fPIC
LIBS=

$(TARGET): \
                g722_decoder.c \
                g722_encoder.c \
                net_java_sip_communicator_impl_neomedia_codec_audio_g722_JNIDecoder.c \
                net_java_sip_communicator_impl_neomedia_codec_audio_g722_JNIEncoder.c \
                g722_decoder.h \
                g722_encoder.h \
                net_java_sip_communicator_impl_neomedia_codec_audio_g722_JNIDecoder.h \
                net_java_sip_communicator_impl_neomedia_codec_audio_g722_JNIEncoder.h
	$(CC) $(CPPFLAGS) $^ $(LDFLAGS) -o $@ $(LIBS)
