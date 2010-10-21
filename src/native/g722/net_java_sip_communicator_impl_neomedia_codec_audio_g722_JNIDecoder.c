#include "net_java_sip_communicator_impl_neomedia_codec_audio_g722_JNIDecoder.h"
#include "g722_decoder.h"

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_audio_g722_JNIDecoder_g722_1decoder_1close
    (JNIEnv *jniEnv, jclass clazz, jlong decoder)
{
    g722_decoder_close((void *) decoder);
}

JNIEXPORT jlong JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_audio_g722_JNIDecoder_g722_1decoder_1open
    (JNIEnv *jniEnv, jclass clazz)
{
    return (jlong) g722_decoder_open();
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_audio_g722_JNIDecoder_g722_1decoder_1process
    (JNIEnv *jniEnv, jclass clazz,
    jlong decoder,
    jbyteArray input, jint inputOffset,
    jbyteArray output, jint outputOffset, jint outputLength)
{
    jbyte *outputPtr = (*jniEnv)->GetByteArrayElements(jniEnv, output, NULL);

    if (outputPtr)
    {
        jbyte *inputPtr
            = (*jniEnv)->GetPrimitiveArrayCritical(jniEnv, input, NULL);

        if (inputPtr)
        {
            g722_decoder_process(
                    (void *) decoder,
                    (unsigned short *) (inputPtr + inputOffset),
                    (short *) (outputPtr + outputOffset),
                    outputLength / sizeof(short));
            (*jniEnv)->ReleasePrimitiveArrayCritical(
                    jniEnv,
                    input, inputPtr,
                    JNI_ABORT);
        }
        (*jniEnv)->ReleaseByteArrayElements(jniEnv, output, outputPtr, 0);
    }
}
