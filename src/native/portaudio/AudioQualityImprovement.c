/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

#define AUDIO_QUALITY_IMPROVEMENT_IMPLEMENTATION
#include "AudioQualityImprovement.h"

#include <limits.h>
#include <math.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/time.h>

#define MIN_SOUND_PRESSURE_LEVEL 40
#define MAX_SOUND_PRESSURE_LEVEL 85

/**
 *
 * @param aqi
 * @param samples
 * @param count the number of samples specified in <tt>samples</tt>
 * @return the sound pressure level in dB of the specified <tt>samples</tt>
 */
static float AudioQualityImprovement_calculateSoundPressureLevel
    (AudioQualityImprovement *aqi, spx_int16_t *samples, spx_uint32_t count);

/**
 *
 * @param aqi
 * @param buffer
 * @param length the length of <tt>buffer</tt> in bytes
 * @return the sound pressure level in dB of the playback which has been matched
 * to the specified capture <tt>buffer</tt> for the purposes of echo
 * cancellation if echo suppression is enabled; otherwise, <tt>0</tt>
 */
static float AudioQualityImprovement_cancelEchoFromPlay
    (AudioQualityImprovement *aqi,
    void *buffer, unsigned long length);
static void AudioQualityImprovement_free(AudioQualityImprovement *aqi);
static AudioQualityImprovement *AudioQualityImprovement_new
    (const char *stringID, jlong longID, AudioQualityImprovement *next);
static void AudioQualityImprovement_popFromPlay
    (AudioQualityImprovement *aqi, spx_uint32_t sampleCount);
static void AudioQualityImprovement_resampleInPlay
    (AudioQualityImprovement *aqi,
    double sampleRate, unsigned long sampleSizeInBits, int channels,
    void *buffer, unsigned long length);
static void AudioQualityImprovement_retain(AudioQualityImprovement *aqi);
static void AudioQualityImprovement_setFrameSize
    (AudioQualityImprovement *aqi, jint frameSize);
static void AudioQualityImprovement_setInputLatency
    (AudioQualityImprovement *aqi, jlong inputLatency);
static void AudioQualityImprovement_setOutputLatency
    (AudioQualityImprovement *aqi, jlong outputLatency);
static void AudioQualityImprovement_suppressEcho
    (AudioQualityImprovement *aqi,
    spx_int16_t *buffer, spx_uint32_t length,
    float spl);

/**
 * Updates the indicator of the specified <tt>AudioQualityImprovement</tt> which
 * determines whether <tt>AudioQualityImprovement#play</tt> delays the access to
 * it from <tt>AudioQualityImprovement#echo</tt>.
 *
 * @param aqi the <tt>AudioQualityImprovement</tt> of which to update the
 * indicator which determines whether <tt>AudioQualityImprovement#play</tt>
 * delays the access to it from <tt>AudioQualityImprovement#echo</tt>
 */
static void AudioQualityImprovement_updatePlayDelay
    (AudioQualityImprovement *aqi);
static void AudioQualityImprovement_updatePlayIsDelaying
    (AudioQualityImprovement *aqi);
static void AudioQualityImprovement_updatePreprocess
    (AudioQualityImprovement *aqi);

/**
 * Returns the current time in milliseconds (akin to
 * <tt>java.lang.System#currentTimeMillis()</tt>).
 *
 * @return the current time in milliseconds
 */
static jlong System_currentTimeMillis();

static Mutex *AudioQualityImprovement_sharedInstancesMutex = NULL;
static AudioQualityImprovement *AudioQualityImprovement_sharedInstances = NULL;

/**
 *
 * @param aqi
 * @param samples
 * @param count the number of samples specified in <tt>samples</tt>
 * @return the sound pressure level in dB of the specified <tt>samples</tt>
 */
static float
AudioQualityImprovement_calculateSoundPressureLevel
    (AudioQualityImprovement *aqi, spx_int16_t *samples, spx_uint32_t count)
{
    spx_uint32_t i;
    float rms;
    float spl;

    if (!count)
        return 0;

    rms = 0;
    for (i = 0; i < count; i++)
    {
        float sample = samples[i] / (float) SHRT_MAX;

        rms += sample * sample;
    }
    rms = sqrtf(rms / count);

    spl = (rms > 0) ? 20 * log10f(rms / 0.00002) : -MAX_SOUND_PRESSURE_LEVEL;
    return spl;
}

/**
 *
 * @param aqi
 * @param buffer
 * @param length the length of <tt>buffer</tt> in bytes
 * @return the sound pressure level in dB of the playback which has been matched
 * to the specified capture <tt>buffer</tt> for the purposes of echo
 * cancellation if echo suppression is enabled; otherwise, <tt>0</tt>
 */
static float
AudioQualityImprovement_cancelEchoFromPlay
    (AudioQualityImprovement *aqi,
    void *buffer, unsigned long length)
{
    spx_uint32_t sampleCount;
    float spl;

    if (aqi->playIsDelaying == JNI_TRUE)
        return 0;

    sampleCount = length / sizeof(spx_int16_t);
    if (aqi->playLength < sampleCount)
        return 0;

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
            return 0;
    }

    /* Perform the echo cancellation and return the result in buffer. */
    speex_echo_cancellation(aqi->echo, buffer, aqi->play, aqi->out);
    memcpy(buffer, aqi->out, length);

    /*
     * Calculate the sound pressure level in dB to be returned (if echo
     * suppression is enabled and, thus, needs it).
     */
    spl
        = (JNI_TRUE == aqi->suppressEcho)
            ? AudioQualityImprovement_calculateSoundPressureLevel(
                aqi,
                aqi->play, sampleCount)
            : 0;

    AudioQualityImprovement_popFromPlay(aqi, sampleCount);

    return spl;
}

static void
AudioQualityImprovement_free(AudioQualityImprovement *aqi)
{
    /* mutex */
    Mutex_free(aqi->mutex);
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

    if (!Mutex_lock(AudioQualityImprovement_sharedInstancesMutex))
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
        Mutex_unlock(AudioQualityImprovement_sharedInstancesMutex);
    }
    return theSharedInstance;
}

/** Loads the <tt>AudioQualityImprovement</tt> class. */
void
AudioQualityImprovement_load()
{
    AudioQualityImprovement_sharedInstancesMutex = Mutex_new(NULL);
}

static AudioQualityImprovement *
AudioQualityImprovement_new
    (const char *stringID, jlong longID, AudioQualityImprovement *next)
{
    AudioQualityImprovement *aqi = calloc(1, sizeof(AudioQualityImprovement));

    if (aqi)
    {
        /* stringID */
        size_t slen = strlen(stringID);
        aqi->stringID = malloc(slen + 1);

        if (!(aqi->stringID))
        {
            AudioQualityImprovement_free(aqi);
            return NULL;
        }
        strncpy(aqi->stringID, stringID, slen);
        aqi->stringID[slen] = 0x00;

        /* mutex */
        aqi->mutex = Mutex_new(NULL);
        if (!(aqi->mutex))
        {
            AudioQualityImprovement_free(aqi);
            return NULL;
        }

        aqi->inputLatency = -1;
        aqi->longID = longID;
        aqi->next = next;
        aqi->outputLatency = -1;
        aqi->retainCount = 1;
        aqi->suppressEcho = JNI_TRUE;
    }
    return aqi;
}

static void
AudioQualityImprovement_popFromPlay
    (AudioQualityImprovement *aqi, spx_uint32_t sampleCount)
{
    spx_uint32_t i;
    spx_uint32_t sampleCountToMove = aqi->playLength - sampleCount;
    spx_int16_t *playNew = aqi->play;
    spx_int16_t *playOld = aqi->play + sampleCount;

    for (i = 0; i < sampleCountToMove; i++)
        *playNew++ = *playOld++;
    aqi->playLength -= sampleCount;
}

/**
 *
 * @param aqi
 * @param sampleOrigin
 * @param sampleRate
 * @param sampleSizeInBits
 * @param channels
 * @param latency the latency of the stream associated with <tt>buffer</tt> in
 * milliseconds
 * @param buffer
 * @param length the length of <tt>buffer</tt> in bytes
 */
void
AudioQualityImprovement_process
    (AudioQualityImprovement *aqi,
    AudioQualityImprovementSampleOrigin sampleOrigin,
    double sampleRate, unsigned long sampleSizeInBits, int channels,
    jlong latency,
    void *buffer, unsigned long length)
{
    if ((sampleSizeInBits == 16) && (channels == 1) && !Mutex_lock(aqi->mutex))
    {
        switch (sampleOrigin)
        {
        case AUDIO_QUALITY_IMPROVEMENT_SAMPLE_ORIGIN_INPUT:
            if (sampleRate == aqi->sampleRate)
            {
                AudioQualityImprovement_setFrameSize(aqi, length);
                if (aqi->preprocess)
                {
                    float spl;
                    jboolean suppressEcho;

                    AudioQualityImprovement_setInputLatency(aqi, latency);

                    if (aqi->echo && aqi->play && aqi->playLength)
                    {
                        spl
                            = AudioQualityImprovement_cancelEchoFromPlay(
                                aqi,
                                buffer, length);
                        suppressEcho = aqi->suppressEcho;
                    }
                    else
                    {
                        spl = 0;
                        /*
                         * Let the echo suppression fade out if it's enabled and
                         * there hasn't been recent playback.
                         */
                        suppressEcho
                            = (aqi->suppressEcho && !(aqi->playLength))
                                ? JNI_TRUE
                                : JNI_FALSE;
                    }

                    speex_preprocess_run(aqi->preprocess, buffer);

                    if (JNI_TRUE == suppressEcho)
                        AudioQualityImprovement_suppressEcho(
                            aqi,
                            buffer, length / sizeof(spx_int16_t),
                            spl);
                }
            }
            break;

        case AUDIO_QUALITY_IMPROVEMENT_SAMPLE_ORIGIN_OUTPUT:
            if (aqi->preprocess && aqi->echo)
            {
                AudioQualityImprovement_setOutputLatency(aqi, latency);
                AudioQualityImprovement_resampleInPlay(
                    aqi,
                    sampleRate, sampleSizeInBits, channels,
                    buffer, length);
            }
            break;
        }
        Mutex_unlock(aqi->mutex);
    }
}

void
AudioQualityImprovement_release(AudioQualityImprovement *aqi)
{
    if (!Mutex_lock(AudioQualityImprovement_sharedInstancesMutex))
    {
        if (!Mutex_lock(aqi->mutex))
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

                Mutex_unlock(aqi->mutex);
                AudioQualityImprovement_free(aqi);
            }
            else
                Mutex_unlock(aqi->mutex);
        }
        Mutex_unlock(AudioQualityImprovement_sharedInstancesMutex);
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
 */
static void
AudioQualityImprovement_resampleInPlay
    (AudioQualityImprovement *aqi,
    double sampleRate, unsigned long sampleSizeInBits, int channels,
    void *buffer, unsigned long length)
{
    spx_uint32_t playSize;
    spx_uint32_t playCapacity;
    spx_uint32_t playLength;;
    spx_int16_t *play;

    if (sampleRate == aqi->sampleRate)
        playSize = length;
    else if (length * aqi->sampleRate == aqi->frameSize * sampleRate)
    {
        if (aqi->resampler)
        {
            speex_resampler_set_rate(
                aqi->resampler,
                (spx_uint32_t) sampleRate, (spx_uint32_t) (aqi->sampleRate));
            playSize = aqi->frameSize;
        }
        else
        {
            aqi->resampler
                = speex_resampler_init(
                    channels,
                    (spx_uint32_t) sampleRate, (spx_uint32_t) (aqi->sampleRate),
                    SPEEX_RESAMPLER_QUALITY_VOIP,
                    NULL);
            if (aqi->resampler)
                playSize = aqi->frameSize;
            else
            {
                aqi->playIsDelaying = JNI_TRUE;
                aqi->playLength = 0;
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
        aqi->playIsDelaying = JNI_TRUE;
        aqi->playLength = 0;
        return;
    }

    /* Ensure that play exists and is large enough. */
    playCapacity
        = ((1 + aqi->playDelay) + 1) * (aqi->frameSize / sizeof(spx_int16_t));
    playLength = playSize / sizeof(spx_int16_t);
    if (playCapacity < playLength)
        playCapacity = playLength;
    if (!(aqi->play) || (aqi->playCapacity < playCapacity))
    {
        spx_int16_t *newPlay;

        newPlay = realloc(aqi->play, playCapacity * sizeof(spx_int16_t));
        if (newPlay)
        {
            if (!(aqi->play))
            {
                aqi->playIsDelaying = JNI_TRUE;
                aqi->playLength = 0;
            }

            aqi->play = newPlay;
            aqi->playCapacity = playCapacity;
        }
        else
        {
            aqi->playIsDelaying = JNI_TRUE;
            aqi->playLength = 0;
            return;
        }
    }

    /* Ensure that there is room for buffer in play. */
    if (aqi->playLength + playLength > aqi->playCapacity)
    {
        aqi->playIsDelaying = JNI_TRUE;
        aqi->playLength = 0;
        /*
         * We don't have enough room in play for buffer which means that we'll
         * have to throw some samples away. But it'll effectively mean that
         * we'll enlarge the drift which will disrupt the echo cancellation. So
         * it seems the least of two evils to just reset the echo cancellation.
         */
        speex_echo_state_reset(aqi->echo);
    }

    /* Place buffer in play. */
    play = aqi->play + aqi->playLength;
    if (length == aqi->frameSize)
        memcpy(play, buffer, playSize);
    else
    {
        unsigned long sampleSizeInBytes = sampleSizeInBits / 8;
        spx_uint32_t bufferSampleCount = length / sampleSizeInBytes;

        speex_resampler_process_interleaved_int(
            aqi->resampler,
            buffer, &bufferSampleCount, play, &playLength);
    }
    aqi->playLength += playLength;

    /* Take into account the latency. */
    if (aqi->playIsDelaying == JNI_TRUE)
        AudioQualityImprovement_updatePlayIsDelaying(aqi);
}

static void
AudioQualityImprovement_retain(AudioQualityImprovement *aqi)
{
    if (!Mutex_lock(aqi->mutex))
    {
        ++(aqi->retainCount);
        Mutex_unlock(aqi->mutex);
    }
}

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
void
AudioQualityImprovement_setDenoise
    (AudioQualityImprovement *aqi, jboolean denoise)
{
    if (!Mutex_lock(aqi->mutex))
    {
        if (aqi->denoise != denoise)
        {
            aqi->denoise = denoise;
            AudioQualityImprovement_updatePreprocess(aqi);
        }
        Mutex_unlock(aqi->mutex);
    }
}

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
void
AudioQualityImprovement_setEchoFilterLengthInMillis
    (AudioQualityImprovement *aqi, jlong echoFilterLengthInMillis)
{
    if (echoFilterLengthInMillis < 0)
        echoFilterLengthInMillis = 0;
    if (!Mutex_lock(aqi->mutex))
    {
        if (aqi->echoFilterLengthInMillis != echoFilterLengthInMillis)
        {
            aqi->echoFilterLengthInMillis = echoFilterLengthInMillis;
            AudioQualityImprovement_updatePreprocess(aqi);
        }
        Mutex_unlock(aqi->mutex);
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

static void
AudioQualityImprovement_setInputLatency
    (AudioQualityImprovement *aqi, jlong inputLatency)
{
    if (aqi->inputLatency != inputLatency)
    {
        aqi->inputLatency = inputLatency;
        AudioQualityImprovement_updatePlayDelay(aqi);
    }
}

static void
AudioQualityImprovement_setOutputLatency
    (AudioQualityImprovement *aqi, jlong outputLatency)
{
    if (aqi->outputLatency != outputLatency)
    {
        aqi->outputLatency = outputLatency;
        AudioQualityImprovement_updatePlayDelay(aqi);
    }
}

void
AudioQualityImprovement_setSampleRate
    (AudioQualityImprovement *aqi, int sampleRate)
{
    if (!Mutex_lock(aqi->mutex))
    {
        if (aqi->sampleRate != sampleRate)
        {
            aqi->sampleRate = sampleRate;
            AudioQualityImprovement_updatePlayDelay(aqi);
            AudioQualityImprovement_updatePreprocess(aqi);
        }
        Mutex_unlock(aqi->mutex);
    }
}
static void
AudioQualityImprovement_suppressEcho
    (AudioQualityImprovement *aqi,
    spx_int16_t *buffer, spx_uint32_t length,
    float spl)
{
    float amplifier;
    spx_int16_t i;

    if (spl < MIN_SOUND_PRESSURE_LEVEL)
        spl = MIN_SOUND_PRESSURE_LEVEL;
    else if (spl > MAX_SOUND_PRESSURE_LEVEL)
        spl = MAX_SOUND_PRESSURE_LEVEL;

    /* Decay #suppressEchoSPL. */
    if (aqi->suppressEchoSPLTime
            && (aqi->suppressEchoSPL > MIN_SOUND_PRESSURE_LEVEL))
    {
        aqi->suppressEchoSPL
            -= (System_currentTimeMillis() - aqi->suppressEchoSPLTime)
                * aqi->suppressEchoSPLDecay;
        if (aqi->suppressEchoSPL <= MIN_SOUND_PRESSURE_LEVEL)
        {
            aqi->suppressEchoSPLDecay = 0;
            aqi->suppressEchoSPLTime = 0;
        }
    }

    if (spl < aqi->suppressEchoSPL)
        spl = aqi->suppressEchoSPL;
    else
    {
        aqi->suppressEchoSPL = spl;
        aqi->suppressEchoSPLDecay
            = ((MIN_SOUND_PRESSURE_LEVEL == spl)
                    ? 1
                    : (spl - MIN_SOUND_PRESSURE_LEVEL))
                / 1000.0;
        aqi->suppressEchoSPLTime = System_currentTimeMillis();
    }

    amplifier
        = 1
            - (spl - MIN_SOUND_PRESSURE_LEVEL)
                / (float) (MAX_SOUND_PRESSURE_LEVEL - MIN_SOUND_PRESSURE_LEVEL);

    for (i = 0; i < length; i++)
        buffer[i] = (spx_int16_t) (amplifier * buffer[i]);
}

/** Unloads the <tt>AudioQualityImprovement</tt> class. */
void
AudioQualityImprovement_unload()
{
    if (AudioQualityImprovement_sharedInstancesMutex)
    {
        Mutex_free(AudioQualityImprovement_sharedInstancesMutex);
        AudioQualityImprovement_sharedInstancesMutex = NULL;
    }
}

static void
AudioQualityImprovement_updatePlayDelay(AudioQualityImprovement *aqi)
{
    spx_uint32_t playDelay;

    if ((aqi->inputLatency < 0)
            || (aqi->outputLatency < 0)
            || !(aqi->frameSize)
            || !(aqi->sampleRate))
    {
        playDelay = MIN_PLAY_DELAY_IN_FRAMES;
    }
    else
    {
        playDelay
            = (aqi->outputLatency * aqi->sampleRate)
                / ((aqi->frameSize / sizeof(spx_int16_t)) * 1000);
        if (playDelay < MIN_PLAY_DELAY_IN_FRAMES)
            playDelay = MIN_PLAY_DELAY_IN_FRAMES;
    }

    if (aqi->playDelay != playDelay)
    {
        aqi->playDelay = playDelay;
        if (aqi->play && (aqi->playIsDelaying == JNI_TRUE))
            AudioQualityImprovement_updatePlayIsDelaying(aqi);
    }
}

/**
 * Updates the indicator of the specified <tt>AudioQualityImprovement</tt> which
 * determines whether <tt>AudioQualityImprovement#play</tt> delays the access to
 * it from <tt>AudioQualityImprovement#echo</tt>.
 *
 * @param aqi the <tt>AudioQualityImprovement</tt> of which to update the
 * indicator which determines whether <tt>AudioQualityImprovement#play</tt>
 * delays the access to it from <tt>AudioQualityImprovement#echo</tt>
 */
static void
AudioQualityImprovement_updatePlayIsDelaying(AudioQualityImprovement *aqi)
{
    spx_uint32_t playDelay
        = aqi->playDelay * (aqi->frameSize / sizeof(spx_int16_t));

    aqi->playIsDelaying
        = ((aqi->playLength < playDelay) && (playDelay <= aqi->playCapacity))
            ? JNI_TRUE
            : JNI_FALSE;
}

static void
AudioQualityImprovement_updatePreprocess(AudioQualityImprovement *aqi)
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
                int echoFrameSize
                    = aqi->frameSize / (16 /* sampleSizeInBits */ / 8);
                int echoFilterLength
                    = (int)
                        ((aqi->sampleRate * aqi->echoFilterLengthInMillis)
                            / 1000);

                aqi->echo
                    = speex_echo_state_init(echoFrameSize, echoFilterLength);
                aqi->filterLengthOfEcho = echoFilterLength;
                /*
                 * Since echo has just been (re)created, make sure that the
                 * delay in play will happen again taking into consideration the
                 * latest frameSize.
                 */
                if (aqi->play)
                    AudioQualityImprovement_updatePlayIsDelaying(aqi);
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
                    int on = 1;

                    speex_preprocess_ctl(
                        aqi->preprocess,
                        SPEEX_PREPROCESS_SET_DEREVERB, &on);
                    speex_preprocess_ctl(
                        aqi->preprocess,
                        SPEEX_PREPROCESS_SET_VAD, &on);
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

/**
 * Returns the current time in milliseconds (akin to
 * <tt>java.lang.System#currentTimeMillis()</tt>).
 *
 * @return the current time in milliseconds
 */
static jlong
System_currentTimeMillis()
{
    struct timeval tv;

    return
        (gettimeofday(&tv, NULL) == 0)
            ? ((tv.tv_sec * 1000) + (tv.tv_usec / 1000))
            : -1;
}
