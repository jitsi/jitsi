/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
#include "net_java_sip_communicator_impl_neomedia_codec_audio_g722_JNIEncoder.h"

#include <inttypes.h>
#include "telephony.h"
#include "g722.h"

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_audio_g722_JNIEncoder_g722_1encoder_1close
    (JNIEnv *jniEnv, jclass clazz, jlong encoder)
{
    g722_encode_state_t *e = (g722_encode_state_t *) (intptr_t) encoder;

    g722_encode_release(e);
    g722_encode_free(e);
}

JNIEXPORT jlong JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_audio_g722_JNIEncoder_g722_1encoder_1open
    (JNIEnv *jniEnv, jclass clazz)
{
    return (jlong) (intptr_t) g722_encode_init(NULL, 64000, 0);
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
            g722_encode(
                    (g722_encode_state_t *) (intptr_t) encoder,
                    (uint8_t *) (outputPtr + outputOffset),
                    (const int16_t *) (inputPtr + inputOffset),
                    2 * (outputLength / sizeof(uint8_t)));
            (*jniEnv)->ReleasePrimitiveArrayCritical(
                    jniEnv,
                    input, inputPtr,
                    JNI_ABORT);
        }
        (*jniEnv)->ReleaseByteArrayElements(jniEnv, output, outputPtr, 0);
    }
}
