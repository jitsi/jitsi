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

#ifdef _WIN32
#define WIN32_LEAN_AND_MEAN
#include <windows.h>

typedef CRITICAL_SECTION Mutex;

static inline int mutex_init(Mutex* mutex, void* arg)
{
    InitializeCriticalSection(mutex);
    arg = NULL; /* unused */
    return 0;
}

static inline int mutex_destroy(Mutex* mutex)
{
    DeleteCriticalSection(mutex);
    return 0;
}

static inline int mutex_lock(Mutex* mutex)
{
    EnterCriticalSection(mutex);
    return 0;
}

static inline int mutex_unlock(Mutex* mutex)
{
    LeaveCriticalSection(mutex);
    return 0;
}

#else /* #ifdef _WIN32 */
#include <pthread.h>

typedef pthread_mutex_t Mutex;

static inline int mutex_init(Mutex* mutex, void* arg)
{
    return pthread_mutex_init(mutex, arg);
}

static inline int mutex_destroy(Mutex* mutex)
{
    return pthread_mutex_destroy(mutex);
}

static inline int mutex_lock(Mutex* mutex)
{
    return pthread_mutex_lock(mutex);
}

static inline int mutex_unlock(Mutex* mutex)
{
    return pthread_mutex_unlock(mutex);
}
#endif /* #ifdef _WIN32 */

#include <speex/speex_echo.h>
#include <speex/speex_preprocess.h>
#include <speex/speex_resampler.h>

typedef struct _AudioQualityImprovement
{
    jboolean denoise;
    SpeexEchoState *echo;
    jlong echoFilterLengthInMillis;

    /** The length of the echo cancelling filter of #echo in samples. */
    int filterLengthOfEcho;
    jint frameSize;
    int frameSizeOfPreprocess;
    jlong longID;
    Mutex *mutex;
    struct _AudioQualityImprovement *next;

    /**
     * The intermediate buffer into which the result of echo cancellation is
     * written for a specific <tt>buffer</tt> of captured audio.
     */
    spx_int16_t *out;

    /** The capacity of #out in bytes. */
    spx_uint32_t outCapacity;
    spx_int16_t *play;

    /**
     * The capacity of #play in bytes i.e. the total number of bytes allocated
     * to #play regardless of whether they are used or not.
     */
    spx_uint32_t playCapacity;

    /** The size in bytes of the valid audio data written into #play. */
    spx_uint32_t playSize;

    /**
     * The time in milliseconds at which the valid audio data written into #play
     * has started playing back.
     */
    jlong playStartTime;
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
    (AudioQualityImprovement *aqi,
    AudioQualityImprovementSampleOrigin sampleOrigin,
    double sampleRate, unsigned long sampleSizeInBits, int channels,
    void *buffer, unsigned long length,
    jlong startTime, jlong endTime);
void AudioQualityImprovement_release(AudioQualityImprovement *aqi);
void AudioQualityImprovement_setDenoise
    (AudioQualityImprovement *aqi, jboolean denoise);
void AudioQualityImprovement_setEchoFilterLengthInMillis
    (AudioQualityImprovement *aqi, jlong echoFilterLengthInMillis);
void AudioQualityImprovement_setSampleRate
    (AudioQualityImprovement *aqi, int sampleRate);

#endif /* #ifndef _NET_JAVA_SIP_COMMUNICATOR_IMPL_NEOMEDIA_PORTAUDIO_AUDIOQUALITYIMPROVEMENT_H_ */
