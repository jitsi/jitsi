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

    /** The playback latency in milliseconds. */
    jlong outputLatency;
    spx_int16_t *play;

    /**
     * The number of samples allocated to #play regardless of whether they are
     * valid or not.
     */
    spx_uint32_t playCapacity;

    /** The number of frames to delay playback with. */
    spx_uint32_t playDelay;

    /**
     * The indicator which determines whether #play is currently delaying the
     * access to it from #echo.
     */
    jboolean playIsDelaying;

    /** The number of valid samples written into #play. */
    spx_uint32_t playLength;
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
    /**
     * The constant which indicates that the associated samples have originated
     * from an input stream i.e. capture.
     */
    AUDIO_QUALITY_IMPROVEMENT_SAMPLE_ORIGIN_INPUT,

    /**
     * The constant which indicates that the associated samples have originated
     * from an output stream i.e. playback.
     */
    AUDIO_QUALITY_IMPROVEMENT_SAMPLE_ORIGIN_OUTPUT
} AudioQualityImprovementSampleOrigin;

AudioQualityImprovement *AudioQualityImprovement_getSharedInstance
    (const char *stringID, jlong longID);
void AudioQualityImprovement_process
    (AudioQualityImprovement *aqi,
    AudioQualityImprovementSampleOrigin sampleOrigin,
    double sampleRate, unsigned long sampleSizeInBits, int channels,
    jlong latency,
    void *buffer, unsigned long length);
void AudioQualityImprovement_release(AudioQualityImprovement *aqi);

/**
 * Sets the indicator which determines whether noise suppression is to be
 * performed by the specified <tt>AudioQualityImprovement</tt> (for captured
 * audio).
 *
 * @param aqi the <tt>AudioQualityImprovement</tt> on which to set the indicator
 * which determines whether it is to perform noise suppression (for captured audio)
 * @param denoise <tt>JNI_TRUE</tt> if the specified <tt>aqi</tt> is to perform
 * noise suppression (for captured audio); otherwise, <tt>JNI_FALSE</tt>
 */
void AudioQualityImprovement_setDenoise
    (AudioQualityImprovement *aqi, jboolean denoise);

/**
 * Sets the filter length in milliseconds of the echo cancellation
 * implementation of the specified <tt>AudioQualityImprovement</tt>. The
 * recommended filter length is approximately the third of the room
 * reverberation time. For example, in a small room, reverberation time is in
 * the order of 300 ms, so a filter length of 100 ms is a good choice (800
 * samples at 8000 Hz sampling rate).
 *
 * @param aqi the <tt>AudioQualityImprovement</tt> to set the filter length of
 * @param echoFilterLengthInMillis the filter length in milliseconds of the echo
 * cancellation of <tt>aqi</tt>
 */
void AudioQualityImprovement_setEchoFilterLengthInMillis
    (AudioQualityImprovement *aqi, jlong echoFilterLengthInMillis);
void AudioQualityImprovement_setSampleRate
    (AudioQualityImprovement *aqi, int sampleRate);

#endif /* #ifndef _NET_JAVA_SIP_COMMUNICATOR_IMPL_NEOMEDIA_PORTAUDIO_AUDIOQUALITYIMPROVEMENT_H_ */
