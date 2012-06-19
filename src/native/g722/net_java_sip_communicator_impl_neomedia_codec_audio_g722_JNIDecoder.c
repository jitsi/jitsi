/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
#include "net_java_sip_communicator_impl_neomedia_codec_audio_g722_JNIDecoder.h"

#include <inttypes.h>
#include <stdint.h>

#include "telephony.h"
#include "g722.h"

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_audio_g722_JNIDecoder_g722_1decoder_1close
    (JNIEnv *jniEnv, jclass clazz, jlong decoder)
{
    g722_decode_state_t *d = (g722_decode_state_t *) (intptr_t) decoder;

    g722_decode_release(d);
    g722_decode_free(d);
}

JNIEXPORT jlong JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_audio_g722_JNIDecoder_g722_1decoder_1open
    (JNIEnv *jniEnv, jclass clazz)
{
    return (jlong) (intptr_t) g722_decode_init(NULL, 64000, 0);
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
            g722_decode(
                    (g722_decode_state_t *) (intptr_t) decoder,
                    (int16_t *) (outputPtr + outputOffset),
                    (const uint8_t *) (inputPtr + inputOffset),
                    outputLength / (sizeof(int16_t) * 2));
            (*jniEnv)->ReleasePrimitiveArrayCritical(
                    jniEnv,
                    input, inputPtr,
                    JNI_ABORT);
        }
        (*jniEnv)->ReleaseByteArrayElements(jniEnv, output, outputPtr, 0);
    }
}
