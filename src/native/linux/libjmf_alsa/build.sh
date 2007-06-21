#!/bin/bash

#set the path to your jvm/jni includes here

JNI_INCLUDE_PATH=/usr/lib/jvm/java-6-sun-1.6.0.00/include
LIBS=-lasound

rm -f libjmf_alsa.so

gcc -shared -o libjmf_alsa.so $LIBS -I$JNI_INCLUDE_PATH -I$JNI_INCLUDE_PATH/linux net_java_sip_communicator_impl_media_protocol_alsa_AlsaStream.c
