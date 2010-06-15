/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

#ifndef _NET_JAVA_SIP_COMMUNICATOR_IMPL_NEOMEDIA_PORTAUDIO_AUDIOQUALITYIMPROVEMENT_H_
#define _NET_JAVA_SIP_COMMUNICATOR_IMPL_NEOMEDIA_PORTAUDIO_AUDIOQUALITYIMPROVEMENT_H_

#include <jni.h>

#ifndef AUDIO_QUALITY_IMPROVEMENT_IMPLEMENTATION
typedef void *AudioQualityImprovement;
#else /* #ifndef AUDIO_QUALITY_IMPROVEMENT_IMPLEMENTATION */
#include <pthread.h>
#include <speex/speex_echo.h>
#include <speex/speex_preprocess.h>
#include <speex/speex_resampler.h>

typedef struct _AudioQualityImprovement
{
    jboolean denoise;
    SpeexEchoState *echo;
    jlong echoFilterLengthInMillis;
    int filterLengthOfEcho;
    jint frameSize;
    int frameSizeOfPreprocess;
    jlong longID;
    pthread_mutex_t *mutex;
    struct _AudioQualityImprovement *next;
    spx_int16_t *out;
    spx_uint32_t outCapacity;
    spx_int16_t *play;
    spx_uint32_t playCapacity;
    spx_uint32_t playSize;
    SpeexPreprocessState *preprocess;
    SpeexResamplerState *resampler;
    int retainCount;
    int sampleRate;
    int sampleRateOfPreprocess;
    char *stringID;
} AudioQualityImprovement;
#endif /* #ifndef AUDIO_QUALITY_IMPROVEMENT_IMPLEMENTATION */

typedef enum
{
    AUDIO_QUALITY_IMPROVEMENT_SAMPLE_ORIGIN_INPUT,
    AUDIO_QUALITY_IMPROVEMENT_SAMPLE_ORIGIN_OUTPUT
} AudioQualityImprovementSampleOrigin;

AudioQualityImprovement *AudioQualityImprovement_getSharedInstance
    (const char *stringID, jlong longID);
void AudioQualityImprovement_process
    (AudioQualityImprovement *audioQualityImprovement,
    AudioQualityImprovementSampleOrigin sampleOrigin,
    double sampleRate,
    unsigned long sampleSizeInBits,
    int channels,
    void *buffer,
    unsigned long length);
void AudioQualityImprovement_release
    (AudioQualityImprovement *audioQualityImprovement);
void AudioQualityImprovement_setDenoise
    (AudioQualityImprovement *audioQualityImprovement, jboolean denoise);
void AudioQualityImprovement_setEchoFilterLengthInMillis
    (AudioQualityImprovement *audioQualityImprovement,
    jlong echoFilterLengthInMillis);
void AudioQualityImprovement_setSampleRate
    (AudioQualityImprovement *audioQualityImprovement, int sampleRate);

#endif /* #ifndef _NET_JAVA_SIP_COMMUNICATOR_IMPL_NEOMEDIA_PORTAUDIO_AUDIOQUALITYIMPROVEMENT_H_ */
