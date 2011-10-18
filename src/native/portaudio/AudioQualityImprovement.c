/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
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
    (AudioQualityImprovement *audioQualityImprovement,
    void *buffer,
    unsigned long length);
static void AudioQualityImprovement_free
    (AudioQualityImprovement *audioQualityImprovement);
static AudioQualityImprovement *AudioQualityImprovement_new
    (const char *stringID, jlong longID, AudioQualityImprovement *next);
static void AudioQualityImprovement_resampleInPlay
    (AudioQualityImprovement *audioQualityImprovement,
    double sampleRate,
    unsigned long sampleSizeInBits,
    int channels,
    void *buffer,
    unsigned long length);
static void AudioQualityImprovement_retain
    (AudioQualityImprovement *audioQualityImprovement);
static void AudioQualityImprovement_setFrameSize
    (AudioQualityImprovement *audioQualityImprovement, jint frameSize);
static void AudioQualityImprovement_updatePreprocess
    (AudioQualityImprovement *audioQualityImprovement);

#ifndef _WIN32
static pthread_mutex_t AudioQualityImprovement_sharedInstancesMutex
    = PTHREAD_MUTEX_INITIALIZER;
#else /* Windows */
static CRITICAL_SECTION AudioQualityImprovement_sharedInstancesMutex = {(void*)-1, -1, 0, 0, 0, 0};
#endif

static AudioQualityImprovement *AudioQualityImprovement_sharedInstances
    = NULL;

static void
AudioQualityImprovement_cancelEchoFromPlay
    (AudioQualityImprovement *audioQualityImprovement,
    void *buffer, unsigned long length)
{
    if (!(audioQualityImprovement->out)
            || (audioQualityImprovement->outCapacity < length))
    {
        spx_int16_t *newOut = realloc(audioQualityImprovement->out, length);

        if (newOut)
        {
            audioQualityImprovement->out = newOut;
            audioQualityImprovement->outCapacity = length;
        }
        else
            return;
    }
    speex_echo_cancellation(
        audioQualityImprovement->echo,
        buffer,
        audioQualityImprovement->play,
        audioQualityImprovement->out);
    audioQualityImprovement->playSize = 0;
    memcpy(buffer, audioQualityImprovement->out, length);
}

static void
AudioQualityImprovement_free(AudioQualityImprovement *audioQualityImprovement)
{
    /* mutex */
    mutex_destroy(audioQualityImprovement->mutex);
    free(audioQualityImprovement->mutex);
    /* preprocess */
    if (audioQualityImprovement->preprocess)
        speex_preprocess_state_destroy(audioQualityImprovement->preprocess);
    /* echo */
    if (audioQualityImprovement->echo)
        speex_echo_state_destroy(audioQualityImprovement->echo);
    /* out */
    if (audioQualityImprovement->out)
        free(audioQualityImprovement->out);
    /* play */
    if (audioQualityImprovement->play)
        free(audioQualityImprovement->play);
    /* resampler */
    if (audioQualityImprovement->resampler)
        speex_resampler_destroy(audioQualityImprovement->resampler);
    /* stringID */
    free(audioQualityImprovement->stringID);

    free(audioQualityImprovement);
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
    AudioQualityImprovement *audioQualityImprovement
        = malloc(sizeof(AudioQualityImprovement));

    if (audioQualityImprovement)
    {
        audioQualityImprovement->echo = NULL;
        audioQualityImprovement->mutex = NULL;
        audioQualityImprovement->out = NULL;
        audioQualityImprovement->play = NULL;
        audioQualityImprovement->preprocess = NULL;
        audioQualityImprovement->resampler = NULL;
        /* audioQualityImprovement->stringID = NULL; */

        /* stringID */
        audioQualityImprovement->stringID = strdup(stringID);
        if (!(audioQualityImprovement->stringID))
        {
            AudioQualityImprovement_free(audioQualityImprovement);
            return NULL;
        }
        /* mutex */
        audioQualityImprovement->mutex = malloc(sizeof(Mutex));
        if (!(audioQualityImprovement->mutex)
                || mutex_init(audioQualityImprovement->mutex, NULL))
        {
            AudioQualityImprovement_free(audioQualityImprovement);
            return NULL;
        }

        audioQualityImprovement->denoise = JNI_FALSE;
        audioQualityImprovement->echoFilterLengthInMillis = 0;
        audioQualityImprovement->frameSize = 0;
        audioQualityImprovement->longID = longID;
        audioQualityImprovement->next = next;
        audioQualityImprovement->retainCount = 1;
        audioQualityImprovement->sampleRate = 0;
    }
    return audioQualityImprovement;
}

void AudioQualityImprovement_process
    (AudioQualityImprovement *audioQualityImprovement,
    AudioQualityImprovementSampleOrigin sampleOrigin,
    double sampleRate,
    unsigned long sampleSizeInBits,
    int channels,
    void *buffer,
    unsigned long length)
{
    if ((sampleSizeInBits == 16)
            && (channels == 1)
            && !mutex_lock(audioQualityImprovement->mutex))
    {
        switch (sampleOrigin)
        {
        case AUDIO_QUALITY_IMPROVEMENT_SAMPLE_ORIGIN_INPUT:
            if (sampleRate == audioQualityImprovement->sampleRate)
            {
                AudioQualityImprovement_setFrameSize(
                    audioQualityImprovement,
                    length);
                if (audioQualityImprovement->preprocess)
                {
                    if (audioQualityImprovement->echo
                            && audioQualityImprovement->play
                            && (audioQualityImprovement->playSize
                                    == audioQualityImprovement->frameSize))
                    {
                        AudioQualityImprovement_cancelEchoFromPlay(
                            audioQualityImprovement,
                            buffer,
                            length);
                    }
                    speex_preprocess_run(
                        audioQualityImprovement->preprocess,
                        buffer);
                }
            }
            break;
        case AUDIO_QUALITY_IMPROVEMENT_SAMPLE_ORIGIN_OUTPUT:
            if (audioQualityImprovement->preprocess
                    && audioQualityImprovement->echo)
            {
                AudioQualityImprovement_resampleInPlay(
                    audioQualityImprovement,
                    sampleRate,
                    sampleSizeInBits,
                    channels,
                    buffer,
                    length);
            }
            break;
        }
        mutex_unlock(audioQualityImprovement->mutex);
    }
}

void
AudioQualityImprovement_release
    (AudioQualityImprovement *audioQualityImprovement)
{
    if (!mutex_lock(&AudioQualityImprovement_sharedInstancesMutex))
    {
        if (!mutex_lock(audioQualityImprovement->mutex))
        {
            --(audioQualityImprovement->retainCount);
            if (audioQualityImprovement->retainCount < 1)
            {
                if (audioQualityImprovement
                        == AudioQualityImprovement_sharedInstances)
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

                        if (audioQualityImprovement == nextSharedInstance)
                        {
                            prevSharedInstance->next
                                = audioQualityImprovement->next;
                            break;
                        }
                        prevSharedInstance = nextSharedInstance;
                    }
                }

                mutex_unlock(audioQualityImprovement->mutex);
                AudioQualityImprovement_free(audioQualityImprovement);
            }
            else
                mutex_unlock(audioQualityImprovement->mutex);
        }
        mutex_unlock(&AudioQualityImprovement_sharedInstancesMutex);
    }
}
static void
AudioQualityImprovement_resampleInPlay
    (AudioQualityImprovement *audioQualityImprovement,
    double sampleRate,
    unsigned long sampleSizeInBits,
    int channels,
    void *buffer,
    unsigned long length)
{
    spx_uint32_t playSize;

    if (sampleRate == audioQualityImprovement->sampleRate)
        playSize = length;
    else if (length * audioQualityImprovement->sampleRate
            == audioQualityImprovement->frameSize * sampleRate)
    {
        if (audioQualityImprovement->resampler)
        {
            speex_resampler_set_rate(
                audioQualityImprovement->resampler,
                (spx_uint32_t) sampleRate,
                (spx_uint32_t) (audioQualityImprovement->sampleRate));
            playSize = audioQualityImprovement->frameSize;
        }
        else
        {
            audioQualityImprovement->resampler
                = speex_resampler_init(
                    channels,
                    (spx_uint32_t) sampleRate,
                    (spx_uint32_t) (audioQualityImprovement->sampleRate),
                    SPEEX_RESAMPLER_QUALITY_VOIP,
                    NULL);
            if (!(audioQualityImprovement->resampler))
            {
                audioQualityImprovement->playSize = 0;
                return;
            }
        }
    }
    else
    {
        audioQualityImprovement->playSize = 0;
        return;
    }
    if (!(audioQualityImprovement->play)
            || (audioQualityImprovement->playCapacity < playSize))
    {
        spx_int16_t *newPlay = realloc(audioQualityImprovement->play, playSize);

        if (newPlay)
        {
            audioQualityImprovement->play = newPlay;
            audioQualityImprovement->playCapacity = playSize;
        }
        else
        {
            audioQualityImprovement->playSize = 0;
            return;
        }
    }
    if (length == audioQualityImprovement->frameSize)
    {
        memcpy(audioQualityImprovement->play, buffer, playSize);
        audioQualityImprovement->playSize = playSize;
    }
    else
    {
        unsigned long sampleSizeInBytes = sampleSizeInBits / 8;
        spx_uint32_t bufferSampleCount = length / sampleSizeInBytes;
        spx_uint32_t playSampleCount = playSize / sampleSizeInBytes;

        speex_resampler_process_interleaved_int(
            audioQualityImprovement->resampler,
            buffer,
            &bufferSampleCount,
            audioQualityImprovement->play,
            &playSampleCount);
        audioQualityImprovement->playSize = playSampleCount * sampleSizeInBytes;
    }
}

static void
AudioQualityImprovement_retain
    (AudioQualityImprovement *audioQualityImprovement)
{
    if (!mutex_lock(audioQualityImprovement->mutex))
    {
        ++(audioQualityImprovement->retainCount);
        mutex_unlock(audioQualityImprovement->mutex);
    }
}

void
AudioQualityImprovement_setDenoise
    (AudioQualityImprovement *audioQualityImprovement, jboolean denoise)
{
    if (!mutex_lock(audioQualityImprovement->mutex))
    {
        if (audioQualityImprovement->denoise != denoise)
        {
            audioQualityImprovement->denoise = denoise;
            AudioQualityImprovement_updatePreprocess(audioQualityImprovement);
        }
        mutex_unlock(audioQualityImprovement->mutex);
    }
}

void
AudioQualityImprovement_setEchoFilterLengthInMillis
    (AudioQualityImprovement *audioQualityImprovement,
    jlong echoFilterLengthInMillis)
{
    if (echoFilterLengthInMillis < 0)
        echoFilterLengthInMillis = 0;
    if (!mutex_lock(audioQualityImprovement->mutex))
    {
        if (audioQualityImprovement->echoFilterLengthInMillis
                != echoFilterLengthInMillis)
        {
            audioQualityImprovement->echoFilterLengthInMillis
                = echoFilterLengthInMillis;
            AudioQualityImprovement_updatePreprocess(audioQualityImprovement);
        }
        mutex_unlock(audioQualityImprovement->mutex);
    }
}

static void
AudioQualityImprovement_setFrameSize
    (AudioQualityImprovement *audioQualityImprovement, jint frameSize)
{
    if (audioQualityImprovement->frameSize != frameSize)
    {
        audioQualityImprovement->frameSize = frameSize;
        AudioQualityImprovement_updatePreprocess(audioQualityImprovement);
    }
}

void
AudioQualityImprovement_setSampleRate
    (AudioQualityImprovement *audioQualityImprovement, int sampleRate)
{
    if (!mutex_lock(audioQualityImprovement->mutex))
    {
        if (audioQualityImprovement->sampleRate != sampleRate)
        {
            audioQualityImprovement->sampleRate = sampleRate;
            AudioQualityImprovement_updatePreprocess(audioQualityImprovement);
        }
        mutex_unlock(audioQualityImprovement->mutex);
    }
}

static void
AudioQualityImprovement_updatePreprocess
    (AudioQualityImprovement *audioQualityImprovement)
{
    if (audioQualityImprovement->echo)
    {
        int frameSize = 0;

        if ((audioQualityImprovement->echoFilterLengthInMillis > 0)
                && (audioQualityImprovement->sampleRate > 0)
                && speex_echo_ctl(
                    audioQualityImprovement->echo,
                    SPEEX_ECHO_GET_FRAME_SIZE,
                    &frameSize))
            frameSize = 0;
        if (frameSize
                && (audioQualityImprovement->frameSize
                    == (frameSize * (16 /* sampleSizeInBits */ / 8))))
        {
            int echoFilterLength
                = (int)
                    ((audioQualityImprovement->sampleRate
                            * audioQualityImprovement->echoFilterLengthInMillis)
                        / 1000);

            if (audioQualityImprovement->filterLengthOfEcho != echoFilterLength)
                frameSize = 0;
        }
        else
            frameSize = 0;
        if (frameSize < 1)
        {
            if (audioQualityImprovement->preprocess)
            {
                speex_preprocess_ctl(
                    audioQualityImprovement->preprocess,
                    SPEEX_PREPROCESS_SET_ECHO_STATE,
                    NULL);
            }
            speex_echo_state_destroy(audioQualityImprovement->echo);
            audioQualityImprovement->echo = NULL;
        }
    }
    if (audioQualityImprovement->preprocess
            && ((audioQualityImprovement->frameSize
                    != audioQualityImprovement->frameSizeOfPreprocess)
                || (audioQualityImprovement->sampleRate
                        != audioQualityImprovement->sampleRateOfPreprocess)))
    {
        speex_preprocess_state_destroy(audioQualityImprovement->preprocess);
        audioQualityImprovement->preprocess = NULL;
    }
    if ((audioQualityImprovement->frameSize > 0)
            && (audioQualityImprovement->sampleRate > 0))
    {
        if (audioQualityImprovement->echoFilterLengthInMillis > 0)
        {
            if (!(audioQualityImprovement->echo))
            {
                int echoFilterLength
                    = (int)
                        ((audioQualityImprovement->sampleRate
                                * audioQualityImprovement
                                        ->echoFilterLengthInMillis)
                            / 1000);

                audioQualityImprovement->echo
                    = speex_echo_state_init(
                        audioQualityImprovement->frameSize
                            / (16 /* sampleSizeInBits */ / 8),
                        echoFilterLength);
                audioQualityImprovement->filterLengthOfEcho
                    = echoFilterLength;
            }
            if (audioQualityImprovement->echo)
            {
                speex_echo_ctl(
                    audioQualityImprovement->echo,
                    SPEEX_ECHO_SET_SAMPLING_RATE,
                    &(audioQualityImprovement->sampleRate));
            }
        }
        if (audioQualityImprovement->denoise || audioQualityImprovement->echo)
        {
            if (!(audioQualityImprovement->preprocess))
            {
                audioQualityImprovement->preprocess
                    = speex_preprocess_state_init(
                        audioQualityImprovement->frameSize
                            / (16 /* sampleSizeInBits */ / 8),
                        audioQualityImprovement->sampleRate);
                audioQualityImprovement->frameSizeOfPreprocess
                    = audioQualityImprovement->frameSize;
                audioQualityImprovement->sampleRateOfPreprocess
                    = audioQualityImprovement->sampleRate;
                if (audioQualityImprovement->preprocess)
                {
                    int vad = 1;

                    speex_preprocess_ctl(
                        audioQualityImprovement->preprocess,
                        SPEEX_PREPROCESS_SET_VAD,
                        &vad);
                }
            }
            if (audioQualityImprovement->preprocess)
            {
                int denoise
                    = (audioQualityImprovement->denoise == JNI_TRUE) ? 1 : 0;

                speex_preprocess_ctl(
                    audioQualityImprovement->preprocess,
                    SPEEX_PREPROCESS_SET_DENOISE,
                    &denoise);
                if (audioQualityImprovement->echo)
                {
                    speex_preprocess_ctl(
                        audioQualityImprovement->preprocess,
                        SPEEX_PREPROCESS_SET_ECHO_STATE,
                        audioQualityImprovement->echo);
                }
            }
        }
    }
}
