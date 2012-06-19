JAVA_HOME?=/usr/lib/jvm/java-6-sun

TARGET=../../../lib/native/linux-64/libjg722.so

CC=gcc
CPPFLAGS=-D_JNI_IMPLEMENTATION_ \
         -I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/linux \
         -O2 \
         -Wall
LDFLAGS=-shared -fPIC
LIBS=

$(TARGET): \
                g722.c \
                net_java_sip_communicator_impl_neomedia_codec_audio_g722_JNIDecoder.c \
                net_java_sip_communicator_impl_neomedia_codec_audio_g722_JNIEncoder.c \
                vector_int.c
	$(CC) $(CPPFLAGS) $^ $(LDFLAGS) -o $@ $(LIBS)
