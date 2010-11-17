/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

#define AUDIO_QUALITY_IMPROVEMENT_IMPLEMENTATION
#include "AudioQualityImprovement.h"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

static void AudioQualityImprovement_cancelEchoFromPlay
    (AudioQualityImprovement *aqi,
    void *buffer, unsigned long length,
    jlong startTime, jlong endTime);
static void AudioQualityImprovement_free(AudioQualityImprovement *aqi);
static AudioQualityImprovement *AudioQualityImprovement_new
    (const char *stringID, jlong longID, AudioQualityImprovement *next);
static void AudioQualityImprovement_resampleInPlay
    (AudioQualityImprovement *aqi,
    double sampleRate, unsigned long sampleSizeInBits, int channels,
    void *buffer, unsigned long length,
    jlong startTime, jlong endTime);
static void AudioQualityImprovement_retain(AudioQualityImprovement *aqi);
static void AudioQualityImprovement_setFrameSize
    (AudioQualityImprovement *aqi, jint frameSize);
static void AudioQualityImprovement_updatePreprocess
    (AudioQualityImprovement *aqi);

#ifndef _WIN32
static pthread_mutex_t AudioQualityImprovement_sharedInstancesMutex
    = PTHREAD_MUTEX_INITIALIZER;
#else /* #ifndef _WIN32 */
static CRITICAL_SECTION AudioQualityImprovement_sharedInstancesMutex
    = {(void*)-1, -1, 0, 0, 0, 0};
#endif /* #ifndef _WIN32 */

static AudioQualityImprovement *AudioQualityImprovement_sharedInstances
    = NULL;

/**
 *
 * @param aqi
 * @param buffer
 * @param length the length of <tt>buffer</tt> in bytes
 * @param startTime the time in milliseconds at which <tt>buffer</tt> was given
 * to the audio capture implementation
 * @param endTime the time in milliseconds at which <tt>buffer</tt> was returned
 * from the audio capture implementation
 */
static void
AudioQualityImprovement_cancelEchoFromPlay
    (AudioQualityImprovement *aqi,
    void *buffer, unsigned long length,
    jlong startTime, jlong endTime)
{
    spx_uint32_t playOffsetInSamples, playOffsetInBytes;

    /*
     * Account for the delay between the giving of the audio data to the
     * playback implementation and its actual playback.
     */
/*
    startTime
        -= 2
            * ((aqi->frameSize / sizeof(spx_int16_t))
                / (aqi->sampleRate / 1000));
 */
    /*
     * Depending on startTime, find the part of play which is to be used by the
     * echo cancellation for buffer.
     */
    if (startTime < aqi->playStartTime)
        return;
    playOffsetInSamples
        = (startTime - aqi->playStartTime) * (aqi->sampleRate / 1000);
    playOffsetInBytes = playOffsetInSamples * sizeof(spx_int16_t);
    if (playOffsetInBytes + length > aqi->playSize)
        return;

    /*
     * Ensure that out exists and is large enough to receive the result of the
     * echo cancellation.
     */
    if (!(aqi->out) || (aqi->outCapacity < length))
    {
        spx_int16_t *newOut = realloc(aqi->out, length);

        if (newOut)
        {
            aqi->out = newOut;
            aqi->outCapacity = length;
        }
        else
            return;
    }

    /* Perform the echo cancellation and return the result in buffer. */
    speex_echo_cancellation(
        aqi->echo,
        buffer, aqi->play + playOffsetInSamples, aqi->out);
    memcpy(buffer, aqi->out, length);
}

static void
AudioQualityImprovement_free(AudioQualityImprovement *aqi)
{
    /* mutex */
    mutex_destroy(aqi->mutex);
    free(aqi->mutex);
    /* preprocess */
    if (aqi->preprocess)
        speex_preprocess_state_destroy(aqi->preprocess);
    /* echo */
    if (aqi->echo)
        speex_echo_state_destroy(aqi->echo);
    /* out */
    if (aqi->out)
        free(aqi->out);
    /* play */
    if (aqi->play)
        free(aqi->play);
    /* resampler */
    if (aqi->resampler)
        speex_resampler_destroy(aqi->resampler);
    /* stringID */
    free(aqi->stringID);

    free(aqi);
}

AudioQualityImprovement *
AudioQualityImprovement_getSharedInstance(const char *stringID, jlong longID)
{
    AudioQualityImprovement *theSharedInstance = NULL;

    if (!mutex_lock(&AudioQualityImprovement_sharedInstancesMutex))
    {
        AudioQualityImprovement *aSharedInstance
            = AudioQualityImprovement_sharedInstances;

        while (aSharedInstance)
        {
            if ((aSharedInstance->longID == longID)
                    && ((aSharedInstance->stringID == stringID)
                        || (0 == strcmp(aSharedInstance->stringID, stringID))))
            {
                theSharedInstance = aSharedInstance;
                break;
            }
            aSharedInstance = aSharedInstance->next;
        }
        if (theSharedInstance)
            AudioQualityImprovement_retain(theSharedInstance);
        else
        {
            theSharedInstance
                = AudioQualityImprovement_new(
                    stringID,
                    longID,
                    AudioQualityImprovement_sharedInstances);
            if (theSharedInstance)
                AudioQualityImprovement_sharedInstances = theSharedInstance;
        }
        mutex_unlock(&AudioQualityImprovement_sharedInstancesMutex);
    }
    return theSharedInstance;
}

static AudioQualityImprovement *
AudioQualityImprovement_new
    (const char *stringID, jlong longID, AudioQualityImprovement *next)
{
    AudioQualityImprovement *aqi = malloc(sizeof(AudioQualityImprovement));

    if (aqi)
    {
        aqi->echo = NULL;
        aqi->mutex = NULL;
        aqi->out = NULL;
        aqi->play = NULL;
        aqi->preprocess = NULL;
        aqi->resampler = NULL;
        /* aqi->stringID = NULL; */

        /* stringID */
        aqi->stringID = strdup(stringID);
        if (!(aqi->stringID))
        {
            AudioQualityImprovement_free(aqi);
            return NULL;
        }
        /* mutex */
        aqi->mutex = malloc(sizeof(Mutex));
        if (!(aqi->mutex) || mutex_init(aqi->mutex, NULL))
        {
            AudioQualityImprovement_free(aqi);
            return NULL;
        }

        aqi->denoise = JNI_FALSE;
        aqi->echoFilterLengthInMillis = 0;
        aqi->frameSize = 0;
        aqi->longID = longID;
        aqi->next = next;
        aqi->retainCount = 1;
        aqi->sampleRate = 0;
    }
    return aqi;
}

/**
 *
 * @param aqi
 * @param sampleOrigin
 * @param sampleRate
 * @param sampleSizeInBits
 * @param channels
 * @param buffer
 * @param length the length of <tt>buffer</tt> in bytes
 * @param startTime the time in milliseconds at which <tt>buffer</tt> was given
 * to the audio capture or playback implementation
 * @param endTime the time in milliseconds at which <tt>buffer</tt> was returned
 * from the audio capture or playback implementation
 */
void
AudioQualityImprovement_process
    (AudioQualityImprovement *aqi,
    AudioQualityImprovementSampleOrigin sampleOrigin,
    double sampleRate, unsigned long sampleSizeInBits, int channels,
    void *buffer, unsigned long length,
    jlong startTime, jlong endTime)
{
    if ((sampleSizeInBits == 16) && (channels == 1) && !mutex_lock(aqi->mutex))
    {
        switch (sampleOrigin)
        {
        case AUDIO_QUALITY_IMPROVEMENT_SAMPLE_ORIGIN_INPUT:
            if (sampleRate == aqi->sampleRate)
            {
                AudioQualityImprovement_setFrameSize(aqi, length);
                if (aqi->preprocess)
                {
                    if (aqi->echo && aqi->play && aqi->playSize)
                    {
                        AudioQualityImprovement_cancelEchoFromPlay(
                            aqi,
                            buffer, length,
                            startTime, endTime);
                    }
                    speex_preprocess_run(aqi->preprocess, buffer);
                }
            }
            break;
        case AUDIO_QUALITY_IMPROVEMENT_SAMPLE_ORIGIN_OUTPUT:
            if (aqi->preprocess && aqi->echo)
            {
                AudioQualityImprovement_resampleInPlay(
                    aqi,
                    sampleRate, sampleSizeInBits, channels,
                    buffer, length,
                    startTime, endTime);
            }
            break;
        }
        mutex_unlock(aqi->mutex);
    }
}

void
AudioQualityImprovement_release(AudioQualityImprovement *aqi)
{
    if (!mutex_lock(&AudioQualityImprovement_sharedInstancesMutex))
    {
        if (!mutex_lock(aqi->mutex))
        {
            --(aqi->retainCount);
            if (aqi->retainCount < 1)
            {
                if (aqi == AudioQualityImprovement_sharedInstances)
                {
                    AudioQualityImprovement_sharedInstances
                        = AudioQualityImprovement_sharedInstances->next;
                }
                else
                {
                    AudioQualityImprovement *prevSharedInstance
                        = AudioQualityImprovement_sharedInstances;

                    while (prevSharedInstance)
                    {
                        AudioQualityImprovement *nextSharedInstance
                            = prevSharedInstance->next;

                        if (aqi == nextSharedInstance)
                        {
                            prevSharedInstance->next = aqi->next;
                            break;
                        }
                        prevSharedInstance = nextSharedInstance;
                    }
                }

                mutex_unlock(aqi->mutex);
                AudioQualityImprovement_free(aqi);
            }
            else
                mutex_unlock(aqi->mutex);
        }
        mutex_unlock(&AudioQualityImprovement_sharedInstancesMutex);
    }
}

/**
 *
 * @param aqi
 * @param sampleRate
 * @param sampleSizeInBits
 * @param channels
 * @param buffer
 * @param length the length of <tt>buffer</tt> in bytes
 * @param startTime the time in milliseconds at which <tt>buffer</tt> was given
 * to the audio playback implementation
 * @param endTime the time in milliseconds at which <tt>buffer</tt> was returned
 * from the audio playback implementation
 */
static void
AudioQualityImprovement_resampleInPlay
    (AudioQualityImprovement *aqi,
    double sampleRate, unsigned long sampleSizeInBits, int channels,
    void *buffer, unsigned long length,
    jlong startTime, jlong endTime)
{
    spx_uint32_t playSize;
    unsigned long sampleSizeInBytes;
    spx_uint32_t newPlaySize;
    jlong oldPlayStartTime;
    spx_int16_t *play;

    if (sampleRate == aqi->sampleRate)
        playSize = length;
    else if (length * aqi->sampleRate == aqi->frameSize * sampleRate)
    {
        if (aqi->resampler)
        {
            speex_resampler_set_rate(
                aqi->resampler,
                (spx_uint32_t) sampleRate,
                (spx_uint32_t) (aqi->sampleRate));
            playSize = aqi->frameSize;
        }
        else
        {
            aqi->resampler
                = speex_resampler_init(
                    channels,
                    (spx_uint32_t) sampleRate,
                    (spx_uint32_t) (aqi->sampleRate),
                    SPEEX_RESAMPLER_QUALITY_VOIP,
                    NULL);
            if (aqi->resampler)
                playSize = aqi->frameSize;
            else
            {
                aqi->playSize = 0;
                return;
            }
        }
    }
    else
    {
        /*
         * The specified buffer neither is in the format of the audio capture
         * nor can be resampled to it.
         */
        aqi->playSize = 0;
        return;
    }

    /* Ensure that play exists and is large enough. */
    sampleSizeInBytes = sampleSizeInBits / 8;
    if (!(aqi->play) || (aqi->playCapacity < playSize))
    {
        spx_uint32_t playCapacity;
        spx_int16_t *newPlay;

        playCapacity = aqi->filterLengthOfEcho * sampleSizeInBytes;
        if (playCapacity < playSize)
            playCapacity = playSize;
        newPlay = realloc(aqi->play, playCapacity);
        if (newPlay)
        {
            if (!(aqi->play))
                aqi->playSize = 0;
            aqi->play = newPlay;
            aqi->playCapacity = playCapacity;
        }
        else
        {
            aqi->playSize = 0;
            return;
        }
    }

    /* Ensure that there is room for buffer in play. */
    newPlaySize = aqi->playSize + playSize;
    if (newPlaySize > aqi->playCapacity)
    {
        spx_uint32_t i;
        spx_uint32_t playBytesToDiscard = newPlaySize - aqi->playCapacity;
        spx_uint32_t playSamplesToMove
            = (aqi->playSize - playBytesToDiscard) / sizeof(spx_int16_t);
        spx_int16_t *playNew = aqi->play;
        spx_uint32_t playSamplesToDiscard
            = playBytesToDiscard / sizeof(spx_int16_t);
        spx_int16_t *playOld = aqi->play + playSamplesToDiscard;

        for (i = 0; i < playSamplesToMove; i++)
            *playNew++ = *playOld++;
        aqi->playSize -= playBytesToDiscard;
        newPlaySize = aqi->playSize + playSize;

        aqi->playStartTime += playSamplesToDiscard / ( aqi->sampleRate / 1000);
    }

    if (endTime > startTime)
    {
        spx_uint32_t lengthInMillis
            = (aqi->frameSize / sampleSizeInBytes) / (aqi->sampleRate / 1000);

        if (endTime - startTime > lengthInMillis)
            startTime = endTime - lengthInMillis;
    }
    oldPlayStartTime = aqi->playStartTime;
    aqi->playStartTime
        = startTime
            - ((aqi->playSize / sampleSizeInBytes) / (aqi->sampleRate / 1000));
    if (aqi->playStartTime != oldPlayStartTime)
        fprintf(stderr, "start time delta = %ld\n", aqi->playStartTime - oldPlayStartTime);

    /* Place buffer in play. */
    play = aqi->play + (aqi->playSize / sizeof(spx_int16_t));
    if (length == aqi->frameSize)
    {
        memcpy(play, buffer, playSize);
        aqi->playSize = newPlaySize;
    }
    else
    {
        spx_uint32_t bufferSampleCount = length / sampleSizeInBytes;
        spx_uint32_t playSampleCount = playSize / sampleSizeInBytes;

        speex_resampler_process_interleaved_int(
            aqi->resampler,
            buffer, &bufferSampleCount,
            play, &playSampleCount);
        aqi->playSize += playSampleCount * sampleSizeInBytes;
    }
}

static void
AudioQualityImprovement_retain(AudioQualityImprovement *aqi)
{
    if (!mutex_lock(aqi->mutex))
    {
        ++(aqi->retainCount);
        mutex_unlock(aqi->mutex);
    }
}

void
AudioQualityImprovement_setDenoise
    (AudioQualityImprovement *aqi, jboolean denoise)
{
    if (!mutex_lock(aqi->mutex))
    {
        if (aqi->denoise != denoise)
        {
            aqi->denoise = denoise;
            AudioQualityImprovement_updatePreprocess(aqi);
        }
        mutex_unlock(aqi->mutex);
    }
}

void
AudioQualityImprovement_setEchoFilterLengthInMillis
    (AudioQualityImprovement *aqi, jlong echoFilterLengthInMillis)
{
    if (echoFilterLengthInMillis < 0)
        echoFilterLengthInMillis = 0;
    if (!mutex_lock(aqi->mutex))
    {
        if (aqi->echoFilterLengthInMillis != echoFilterLengthInMillis)
        {
            aqi->echoFilterLengthInMillis = echoFilterLengthInMillis;
            AudioQualityImprovement_updatePreprocess(aqi);
        }
        mutex_unlock(aqi->mutex);
    }
}

static void
AudioQualityImprovement_setFrameSize
    (AudioQualityImprovement *aqi, jint frameSize)
{
    if (aqi->frameSize != frameSize)
    {
        aqi->frameSize = frameSize;
        AudioQualityImprovement_updatePreprocess(aqi);
    }
}

void
AudioQualityImprovement_setSampleRate
    (AudioQualityImprovement *aqi, int sampleRate)
{
    if (!mutex_lock(aqi->mutex))
    {
        if (aqi->sampleRate != sampleRate)
        {
            aqi->sampleRate = sampleRate;
            AudioQualityImprovement_updatePreprocess(aqi);
        }
        mutex_unlock(aqi->mutex);
    }
}

static void
AudioQualityImprovement_updatePreprocess
    (AudioQualityImprovement *aqi)
{
    if (aqi->echo)
    {
        int frameSize = 0;

        if ((aqi->echoFilterLengthInMillis > 0)
                && (aqi->sampleRate > 0)
                && speex_echo_ctl(
                    aqi->echo,
                    SPEEX_ECHO_GET_FRAME_SIZE, &frameSize))
            frameSize = 0;
        if (frameSize
                && (aqi->frameSize
                    == (frameSize * (16 /* sampleSizeInBits */ / 8))))
        {
            int echoFilterLength
                = (int)
                    ((aqi->sampleRate * aqi->echoFilterLengthInMillis)
                        / 1000);

            if (aqi->filterLengthOfEcho != echoFilterLength)
                frameSize = 0;
        }
        else
            frameSize = 0;
        if (frameSize < 1)
        {
            if (aqi->preprocess)
            {
                speex_preprocess_ctl(
                    aqi->preprocess,
                    SPEEX_PREPROCESS_SET_ECHO_STATE, NULL);
            }
            speex_echo_state_destroy(aqi->echo);
            aqi->echo = NULL;
        }
    }
    if (aqi->preprocess
            && ((aqi->frameSize != aqi->frameSizeOfPreprocess)
                || (aqi->sampleRate != aqi->sampleRateOfPreprocess)))
    {
        speex_preprocess_state_destroy(aqi->preprocess);
        aqi->preprocess = NULL;
    }
    if ((aqi->frameSize > 0) && (aqi->sampleRate > 0))
    {
        if (aqi->echoFilterLengthInMillis > 0)
        {
            if (!(aqi->echo))
            {
                int echoFilterLength
                    = (int)
                        ((aqi->sampleRate * aqi->echoFilterLengthInMillis)
                            / 1000);

                aqi->echo
                    = speex_echo_state_init(
                        aqi->frameSize / (16 /* sampleSizeInBits */ / 8),
                        echoFilterLength);
                aqi->filterLengthOfEcho = echoFilterLength;
            }
            if (aqi->echo)
            {
                speex_echo_ctl(
                    aqi->echo,
                    SPEEX_ECHO_SET_SAMPLING_RATE, &(aqi->sampleRate));
            }
        }
        if (aqi->denoise || aqi->echo)
        {
            if (!(aqi->preprocess))
            {
                aqi->preprocess
                    = speex_preprocess_state_init(
                        aqi->frameSize / (16 /* sampleSizeInBits */ / 8),
                        aqi->sampleRate);
                aqi->frameSizeOfPreprocess = aqi->frameSize;
                aqi->sampleRateOfPreprocess = aqi->sampleRate;
                if (aqi->preprocess)
                {
                    int vad = 1;

                    speex_preprocess_ctl(
                        aqi->preprocess,
                        SPEEX_PREPROCESS_SET_VAD, &vad);
                }
            }
            if (aqi->preprocess)
            {
                int denoise = (aqi->denoise == JNI_TRUE) ? 1 : 0;

                speex_preprocess_ctl(
                    aqi->preprocess,
                    SPEEX_PREPROCESS_SET_DENOISE, &denoise);
                if (aqi->echo)
                {
                    speex_preprocess_ctl(
                        aqi->preprocess,
                        SPEEX_PREPROCESS_SET_ECHO_STATE, aqi->echo);
                }
            }
        }
    }
}
