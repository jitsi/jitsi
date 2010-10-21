#include "net_java_sip_communicator_impl_neomedia_codec_audio_g722_JNIEncoder.h"
#include "g722_encoder.h"

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_audio_g722_JNIEncoder_g722_1encoder_1close
    (JNIEnv *jniEnv, jclass clazz, jlong encoder)
{
    g722_encoder_close((void *) encoder);
}

JNIEXPORT jlong JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_audio_g722_JNIEncoder_g722_1encoder_1open
    (JNIEnv *jniEnv, jclass clazz)
{
    return (jlong) g722_encoder_open();
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_audio_g722_JNIEncoder_g722_1encoder_1process
    (JNIEnv *jniEnv, jclass clazz,
    jlong encoder,
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
            g722_encoder_process(
                    (void *) encoder,
                    (short *) (inputPtr + inputOffset),
                    (unsigned short *) (outputPtr + outputOffset),
                    outputLength / sizeof(unsigned short));
            (*jniEnv)->ReleasePrimitiveArrayCritical(
                    jniEnv,
                    input, inputPtr,
                    JNI_ABORT);
        }
        (*jniEnv)->ReleaseByteArrayElements(jniEnv, output, outputPtr, 0);
    }
}
