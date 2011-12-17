/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

#include "net_java_sip_communicator_impl_neomedia_portaudio_PortAudio.h"

#include "AudioQualityImprovement.h"
#include "ConditionVariable.h"
#include "Mutex.h"
#include <portaudio.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/time.h>

typedef struct
{
    AudioQualityImprovement *audioQualityImprovement;
    int channels;
    JNIEnv *env;
    jboolean finished;

    /**
     * The value specified as the <tt>framesPerBuffer</tt> argument to the
     * <tt>Pa_OpenStream</tt> function call which has opened #stream.
     */
    unsigned long framesPerBuffer;
    void *input;
    size_t inputCapacity;
    ConditionVariable *inputCondVar;
    long inputFrameSize;

    /** The input latency of #stream. */
    jlong inputLatency;
    size_t inputLength;
    Mutex *inputMutex;
    Mutex *mutex;
    void *output;
    size_t outputCapacity;
    ConditionVariable *outputCondVar;
    long outputFrameSize;

    /** The output latency of #stream. */
    jlong outputLatency;
    size_t outputLength;
    Mutex *outputMutex;

    /**
     * The indicator which determines whether this <tt>PortAudioStream</tt>
     * implements the blocking stream interface on top of the non-blocking
     * stream interface.
     */
    jboolean pseudoBlocking;
    jlong retainCount;
    double sampleRate;
    int sampleSizeInBits;
    PaStream *stream;
    jobject streamCallback;
    jmethodID streamCallbackMethodID;
    jmethodID streamFinishedCallbackMethodID;
    JavaVM *vm;
} PortAudioStream;

static PaStreamParameters *PortAudio_fixInputParametersSuggestedLatency
    (PaStreamParameters *inputParameters,
    jdouble sampleRate, jlong framesPerBuffer,
    PaHostApiTypeId hostApiType);
static PaStreamParameters *PortAudio_fixOutputParametersSuggestedLatency
    (PaStreamParameters *outputParameters,
    jdouble sampleRate, jlong framesPerBuffer,
    PaHostApiTypeId hostApiType);
static PaStreamParameters *PortAudio_fixStreamParametersSuggestedLatency
    (PaStreamParameters *streamParameters,
    jdouble sampleRate, jlong framesPerBuffer,
    PaHostApiTypeId hostApiType);
static long PortAudio_getFrameSize(PaStreamParameters *streamParameters);
static unsigned long PortAudio_getSampleSizeInBits
    (PaStreamParameters *streamParameters);
static void PortAudio_throwException(JNIEnv *env, PaError errorCode);

/**
 * Allocates (and initializes) the memory and its associated variables for a
 * specific buffer to be used by the pseudo-blocking stream interface
 * implementation of a <tt>PortAudioStream</tt>.
 *
 * @param capacity the number of bytes to be allocated to the buffer
 * @param bufferPtr a pointer which specifies where the location of the
 * allocated buffer is to be stored
 * @param bufferLengthPtr a pointer which specifies where the initial length
 * (i.e. zero) is to be stored
 * @param bufferCapacityPtr a pointer which specifies where the capacity of the
 * allocated buffer is to be stored
 * @param bufferMutexPtr a pointer which specifies where the <tt>Mute</tt> to
 * synchronize the access to the allocated buffer is to be stored
 * @param bufferCondVarPtr a pointer which specifies where the
 * <tt>ConditionVariable</tt> to synchronize the access to the allocated buffer
 * is to be stored
 * @return the location of the allocated buffer upon success; otherwise,
 * <tt>NULL</tt>
 */
static void *PortAudioStream_allocPseudoBlockingBuffer
    (size_t capacity,
    void **bufferPtr, size_t *bufferLengthPtr, size_t *bufferCapacityPtr,
    Mutex **bufferMutexPtr, ConditionVariable **bufferCondVarPtr);
static void PortAudioStream_free(JNIEnv *env, PortAudioStream *stream);
static int PortAudioStream_javaCallback
    (const void *input,
    void *output,
    unsigned long frameCount,
    const PaStreamCallbackTimeInfo *timeInfo,
    PaStreamCallbackFlags statusFlags,
    void *userData);
static void PortAudioStream_javaFinishedCallback(void *userData);
static PortAudioStream * PortAudioStream_new
    (JNIEnv *env, jobject streamCallback);
static void PortAudioStream_popFromPseudoBlockingBuffer
    (void *buffer, size_t length, size_t *bufferLengthPtr);
static int PortAudioStream_pseudoBlockingCallback
    (const void *input,
    void *output,
    unsigned long frameCount,
    const PaStreamCallbackTimeInfo *timeInfo,
    PaStreamCallbackFlags statusFlags,
    void *userData);
static void PortAudioStream_pseudoBlockingFinishedCallback(void *userData);
static void PortAudioStream_release(PortAudioStream *stream);
static void PortAudioStream_retain(PortAudioStream *stream);

static const char *AUDIO_QUALITY_IMPROVEMENT_STRING_ID = "portaudio";
#define LATENCY_HIGH net_java_sip_communicator_impl_neomedia_portaudio_PortAudio_LATENCY_HIGH
#define LATENCY_LOW net_java_sip_communicator_impl_neomedia_portaudio_PortAudio_LATENCY_LOW
#define LATENCY_UNSPECIFIED net_java_sip_communicator_impl_neomedia_portaudio_PortAudio_LATENCY_UNSPECIFIED

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_portaudio_PortAudio_free
    (JNIEnv *jniEnv, jclass clazz, jlong ptr)
{
    free((void *) ptr);
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_portaudio_PortAudio_Pa_1AbortStream
    (JNIEnv *env, jclass clazz, jlong stream)
{
    PaError errorCode = Pa_AbortStream(((PortAudioStream *) stream)->stream);

    if (paNoError != errorCode)
        PortAudio_throwException(env, errorCode);
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_portaudio_PortAudio_Pa_1CloseStream
    (JNIEnv *env, jclass clazz, jlong stream)
{
    PortAudioStream *portAudioStream = (PortAudioStream *) stream;
    PaError errorCode = Pa_CloseStream(portAudioStream->stream);

    if (paNoError != errorCode)
        PortAudio_throwException(env, errorCode);
    else if (portAudioStream->pseudoBlocking)
        PortAudioStream_release(portAudioStream);
    else
        PortAudioStream_free(env, portAudioStream);
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_portaudio_PortAudio_Pa_1GetDefaultInputDevice
    (JNIEnv *env, jclass clazz)
{
    return Pa_GetDefaultInputDevice();
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_portaudio_PortAudio_Pa_1GetDefaultOutputDevice
    (JNIEnv *env, jclass clazz)
{
    return Pa_GetDefaultOutputDevice();
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_portaudio_PortAudio_Pa_1GetDeviceCount(
    JNIEnv *env, jclass clazz)
{
    PaDeviceIndex deviceCount = Pa_GetDeviceCount();

    if (deviceCount < 0)
        PortAudio_throwException(env, deviceCount);
    return deviceCount;
}

JNIEXPORT jlong JNICALL
Java_net_java_sip_communicator_impl_neomedia_portaudio_PortAudio_Pa_1GetDeviceInfo(
    JNIEnv *env, jclass clazz, jint deviceIndex)
{
    return (jlong) Pa_GetDeviceInfo(deviceIndex);
}

JNIEXPORT jlong JNICALL
Java_net_java_sip_communicator_impl_neomedia_portaudio_PortAudio_Pa_1GetHostApiInfo
    (JNIEnv *env , jclass clazz, jint hostApiIndex)
{
    return (jlong) Pa_GetHostApiInfo(hostApiIndex);
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_portaudio_PortAudio_Pa_1GetSampleSize
  (JNIEnv *env, jclass clazz, jlong format)
{
    return Pa_GetSampleSize(format);
}

JNIEXPORT jlong JNICALL
Java_net_java_sip_communicator_impl_neomedia_portaudio_PortAudio_Pa_1GetStreamReadAvailable
    (JNIEnv *env, jclass clazz, jlong stream)
{
    return Pa_GetStreamReadAvailable(((PortAudioStream *) stream)->stream);
}

JNIEXPORT jlong JNICALL
Java_net_java_sip_communicator_impl_neomedia_portaudio_PortAudio_Pa_1GetStreamWriteAvailable
    (JNIEnv *env, jclass clazz, jlong stream)
{
    return Pa_GetStreamWriteAvailable(((PortAudioStream *) stream)->stream);
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_portaudio_PortAudio_Pa_1Initialize(
    JNIEnv *env, jclass clazz)
{
    PaError errorCode = Pa_Initialize();

    if (paNoError != errorCode)
        PortAudio_throwException(env, errorCode);
}

JNIEXPORT jboolean JNICALL
Java_net_java_sip_communicator_impl_neomedia_portaudio_PortAudio_Pa_1IsFormatSupported
    (JNIEnv *env, jclass clazz,
    jlong inputParameters, jlong outputParameters, jdouble sampleRate)
{
    if (Pa_IsFormatSupported(
                (PaStreamParameters *) inputParameters,
                (PaStreamParameters *) outputParameters,
                sampleRate)
            == paFormatIsSupported)
        return JNI_TRUE;
    else
        return JNI_FALSE;
}

JNIEXPORT jlong JNICALL
Java_net_java_sip_communicator_impl_neomedia_portaudio_PortAudio_Pa_1OpenStream
    (JNIEnv *env, jclass clazz,
    jlong inputParameters, jlong outputParameters,
    jdouble sampleRate,
    jlong framesPerBuffer,
    jlong streamFlags,
    jobject streamCallback)
{
    PortAudioStream *stream = PortAudioStream_new(env, streamCallback);
    PaStreamCallback *effectiveStreamCallback;
    PaStreamFinishedCallback *effectiveStreamFinishedCallback;
    unsigned long effectiveFramesPerBuffer = framesPerBuffer;
    PaHostApiTypeId hostApiType = paInDevelopment;
    PaError errorCode;
    PaStreamParameters *inputStreamParameters
        = (PaStreamParameters *) inputParameters;
    PaStreamParameters *outputStreamParameters
        = (PaStreamParameters *) outputParameters;

    if (!stream)
        return 0;

    if (streamCallback)
    {
        effectiveStreamCallback = PortAudioStream_javaCallback;
        effectiveStreamFinishedCallback = PortAudioStream_javaFinishedCallback;
        stream->pseudoBlocking = JNI_FALSE;
    }
    else
    {
        /*
         * Some host APIs such as DirectSound don't really implement the
         * blocking stream interface. If we're to ever be able to try them out,
         * we'll have to implement the blocking stream interface on top of the
         * non-blocking stream interface.
         */

        effectiveStreamCallback = NULL;
        effectiveStreamFinishedCallback = NULL;
        stream->pseudoBlocking = JNI_FALSE;

        /*
         * TODO It should be possible to implement the blocking stream interface
         * without a specific framesPerBuffer.
         */
        if ((paFramesPerBufferUnspecified != framesPerBuffer)
                && (framesPerBuffer > 0))
        {
            PaDeviceIndex device;

            if (outputStreamParameters)
                device = outputStreamParameters->device;
            else if (inputStreamParameters)
                device = inputStreamParameters->device;
            else
                device = paNoDevice;
            if (device != paNoDevice)
            {
                const PaDeviceInfo *deviceInfo = Pa_GetDeviceInfo(device);

                if (deviceInfo)
                {
                    const PaHostApiInfo *hostApiInfo
                        = Pa_GetHostApiInfo(deviceInfo->hostApi);

                    if (hostApiInfo)
                    {
                        switch (hostApiInfo->type)
                        {
                        case paCoreAudio:
                            /*
                             * If we are to ever succeed in requesting a higher
                             * latency in
                             * PortAudio_fixOutputParametersSuggestedLatency, we
                             * have to specify paFramesPerBufferUnspecified.
                             * Otherwise, the CoreAudio implementation of
                             * PortAudio will ignore our suggestedLatency.
                             */
                            if (outputStreamParameters
                                    && ((LATENCY_HIGH
                                            == outputStreamParameters
                                                ->suggestedLatency)
                                        || (LATENCY_UNSPECIFIED
                                            == outputStreamParameters
                                                ->suggestedLatency)))
                            {
                                effectiveFramesPerBuffer
                                    = paFramesPerBufferUnspecified;
                                hostApiType = hostApiInfo->type;
                            }
                            if (inputStreamParameters
                                    && ((LATENCY_HIGH
                                            == inputStreamParameters
                                                ->suggestedLatency)
                                        || (LATENCY_UNSPECIFIED
                                            == inputStreamParameters
                                                ->suggestedLatency)))
                            {
                                effectiveFramesPerBuffer
                                    = paFramesPerBufferUnspecified;
                                hostApiType = hostApiInfo->type;
                            }
                            break;
                        case paDirectSound:
                            effectiveStreamCallback
                                = PortAudioStream_pseudoBlockingCallback;
                            effectiveStreamFinishedCallback
                                = PortAudioStream_pseudoBlockingFinishedCallback;
                            stream->pseudoBlocking = JNI_TRUE;
                            break;
                        default:
                            break;
                        }
                    }
                }
            }
        }
    }

    if (JNI_TRUE == stream->pseudoBlocking)
    {
        stream->mutex = Mutex_new(NULL);
        errorCode = (stream->mutex) ? paNoError : paInsufficientMemory;
    }
    else
        errorCode = paNoError;

    if (paNoError == errorCode)
    {
        errorCode
            = Pa_OpenStream(
                &(stream->stream),
                PortAudio_fixInputParametersSuggestedLatency(
                    inputStreamParameters,
                    sampleRate, framesPerBuffer,
                    hostApiType),
                PortAudio_fixOutputParametersSuggestedLatency(
                    outputStreamParameters,
                    sampleRate, framesPerBuffer,
                    hostApiType),
                sampleRate,
                effectiveFramesPerBuffer,
                streamFlags,
                effectiveStreamCallback,
                stream);
    }

    if (paNoError == errorCode)
    {
        stream->framesPerBuffer = effectiveFramesPerBuffer;
        stream->inputFrameSize
                = PortAudio_getFrameSize(inputStreamParameters);
        stream->outputFrameSize
                = PortAudio_getFrameSize(outputStreamParameters);
        stream->sampleRate = sampleRate;

        if (effectiveStreamFinishedCallback)
        {
            errorCode
                = Pa_SetStreamFinishedCallback(
                    stream->stream,
                    effectiveStreamFinishedCallback);
        }

        stream->audioQualityImprovement
            = AudioQualityImprovement_getSharedInstance(
                AUDIO_QUALITY_IMPROVEMENT_STRING_ID,
                0);
        if (inputStreamParameters)
        {
            stream->sampleSizeInBits
                = PortAudio_getSampleSizeInBits(inputStreamParameters);
            stream->channels = inputStreamParameters->channelCount;

            /*
             * Prepare whatever is necessary for the pseudo-blocking stream
             * interface implementation. For example, allocate its memory early
             * because doing it in the stream callback may introduce latency.
             */
            if (stream->pseudoBlocking
                    && !PortAudioStream_allocPseudoBlockingBuffer(
                            2 * framesPerBuffer * (stream->inputFrameSize),
                            &(stream->input),
                            &(stream->inputLength),
                            &(stream->inputCapacity),
                            &(stream->inputMutex),
                            &(stream->inputCondVar)))
            {
                Java_net_java_sip_communicator_impl_neomedia_portaudio_PortAudio_Pa_1CloseStream(
                    env, clazz,
                    (jlong) stream);
                if (JNI_FALSE == (*env)->ExceptionCheck(env))
                {
                    PortAudio_throwException(env, paInsufficientMemory);
                    return 0;
                }
            }

            if (stream->audioQualityImprovement)
            {
                AudioQualityImprovement_setSampleRate(
                    stream->audioQualityImprovement,
                    (int) sampleRate);

                if (stream->pseudoBlocking)
                {
                    const PaStreamInfo *streamInfo;

                    streamInfo = Pa_GetStreamInfo(stream->stream);
                    if (streamInfo)
                    {
                        stream->inputLatency
                                = (jlong) (streamInfo->inputLatency * 1000);
                    }
                }
            }
        }
        if (outputStreamParameters)
        {
            stream->sampleSizeInBits
                = PortAudio_getSampleSizeInBits(outputStreamParameters);
            stream->channels = outputStreamParameters->channelCount;

            if (stream->pseudoBlocking
                    && !PortAudioStream_allocPseudoBlockingBuffer(
                            2 * framesPerBuffer * (stream->outputFrameSize),
                            &(stream->output),
                            &(stream->outputLength),
                            &(stream->outputCapacity),
                            &(stream->outputMutex),
                            &(stream->outputCondVar)))
            {
                Java_net_java_sip_communicator_impl_neomedia_portaudio_PortAudio_Pa_1CloseStream(
                    env, clazz,
                    (jlong) stream);
                if (JNI_FALSE == (*env)->ExceptionCheck(env))
                {
                    PortAudio_throwException(env, paInsufficientMemory);
                    return 0;
                }
            }

            if (stream->audioQualityImprovement)
            {
                const PaStreamInfo *streamInfo;

                streamInfo = Pa_GetStreamInfo(stream->stream);
                if (streamInfo)
                {
                    stream->outputLatency
                            = (jlong) (streamInfo->outputLatency * 1000);
                }
            }
        }

        if (stream->pseudoBlocking)
            PortAudioStream_retain(stream);

        return (jlong) stream;
    }
    else
    {
        PortAudioStream_free(env, stream);
        PortAudio_throwException(env, errorCode);
        return 0;
    }
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_portaudio_PortAudio_Pa_1ReadStream
    (JNIEnv *env, jclass clazz, jlong stream, jbyteArray buffer, jlong frames)
{
    jbyte* data = (*env)->GetByteArrayElements(env, buffer, NULL);

    if (data)
    {
        PortAudioStream *portAudioStream = (PortAudioStream *) stream;
        PaError errorCode;
        jlong framesInBytes = frames * portAudioStream->inputFrameSize;

        if (portAudioStream->pseudoBlocking)
        {
            if (Mutex_lock(portAudioStream->inputMutex))
                errorCode = paInternalError;
            else
            {
                jlong bytesRead = 0;

                errorCode = paNoError;
                while (bytesRead < framesInBytes)
                {
                    jlong bytesToRead;

                    if (JNI_TRUE == portAudioStream->finished)
                    {
                        errorCode = paStreamIsStopped;
                        break;
                    }
                    if (!(portAudioStream->inputLength))
                    {
                        ConditionVariable_wait(
                            portAudioStream->inputCondVar,
                            portAudioStream->inputMutex);
                        continue;
                    }

                    bytesToRead = framesInBytes - bytesRead;
                    if (bytesToRead > portAudioStream->inputLength)
                        bytesToRead = portAudioStream->inputLength;
                    memcpy(
                        data + bytesRead,
                        portAudioStream->input,
                        bytesToRead);
                    PortAudioStream_popFromPseudoBlockingBuffer(
                        portAudioStream->input,
                        bytesToRead,
                        &(portAudioStream->inputLength));
                    bytesRead += bytesToRead;
                }
                Mutex_unlock(portAudioStream->inputMutex);
            }

            /* Improve the audio quality of the input if possible. */
            if ((paNoError == errorCode)
                    && portAudioStream->audioQualityImprovement)
            {
                AudioQualityImprovement_process(
                    portAudioStream->audioQualityImprovement,
                    AUDIO_QUALITY_IMPROVEMENT_SAMPLE_ORIGIN_INPUT,
                    portAudioStream->sampleRate,
                    portAudioStream->sampleSizeInBits,
                    portAudioStream->channels,
                    portAudioStream->inputLatency,
                    data, framesInBytes);
            }
        }
        else
        {
            errorCode = Pa_ReadStream(portAudioStream->stream, data, frames);
            if ((paNoError == errorCode) || (paInputOverflowed == errorCode))
            {
                errorCode = paNoError;

                if (portAudioStream->audioQualityImprovement)
                {
                    AudioQualityImprovement_process(
                        portAudioStream->audioQualityImprovement,
                        AUDIO_QUALITY_IMPROVEMENT_SAMPLE_ORIGIN_INPUT,
                        portAudioStream->sampleRate,
                        portAudioStream->sampleSizeInBits,
                        portAudioStream->channels,
                        portAudioStream->inputLatency,
                        data, framesInBytes);
                }
            }
        }

        if (paNoError == errorCode)
            (*env)->ReleaseByteArrayElements(env, buffer, data, 0);
        else
        {
            (*env)->ReleaseByteArrayElements(env, buffer, data, JNI_ABORT);
            PortAudio_throwException(env, errorCode);
        }
    }
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_portaudio_PortAudio_Pa_1StartStream
    (JNIEnv *env, jclass clazz, jlong stream)
{
    PortAudioStream *portAudioStream = (PortAudioStream *) stream;
    PaError errorCode;

    if (portAudioStream->pseudoBlocking)
    {
        PortAudioStream_retain(portAudioStream);
        if (Mutex_lock(portAudioStream->mutex))
            errorCode = paInternalError;
        else
        {
            portAudioStream->finished = JNI_FALSE;
            errorCode = Pa_StartStream(portAudioStream->stream);
            if (paNoError != errorCode)
                portAudioStream->finished = JNI_TRUE;
            Mutex_unlock(portAudioStream->mutex);
        }
        if (paNoError != errorCode)
            PortAudioStream_release(portAudioStream);
    }
    else
        errorCode = Pa_StartStream(portAudioStream->stream);
    if (paNoError != errorCode)
        PortAudio_throwException(env, errorCode);
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_portaudio_PortAudio_Pa_1StopStream
    (JNIEnv *env, jclass clazz, jlong stream)
{
    PaError errorCode = Pa_StopStream(((PortAudioStream *) stream)->stream);

    if (paNoError != errorCode)
        PortAudio_throwException(env, errorCode);
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_portaudio_PortAudio_Pa_1WriteStream
    (JNIEnv *env, jclass clazz,
    jlong stream,
    jbyteArray buffer, jint offset, jlong frames,
    jint numberOfWrites)
{
    jbyte *bufferBytes;
    jbyte* data;
    PortAudioStream *portAudioStream;
    jint i;
    PaError errorCode = paNoError;
    jlong framesInBytes;
    AudioQualityImprovement *audioQualityImprovement;
    double sampleRate;
    unsigned long sampleSizeInBits;
    int channels;
    jlong outputLatency;

    bufferBytes = (*env)->GetByteArrayElements(env, buffer, NULL);
    if (!bufferBytes)
        return;
    data = bufferBytes + offset;

    portAudioStream = (PortAudioStream *) stream;
    framesInBytes = frames * portAudioStream->outputFrameSize;
    audioQualityImprovement = portAudioStream->audioQualityImprovement;
    sampleRate = portAudioStream->sampleRate;
    sampleSizeInBits = portAudioStream->sampleSizeInBits;
    channels = portAudioStream->channels;
    outputLatency = portAudioStream->outputLatency;

    if (portAudioStream->pseudoBlocking)
    {
        for (i = 0; i < numberOfWrites; i++)
        {
            if (Mutex_lock(portAudioStream->outputMutex))
                errorCode = paInternalError;
            else
            {
                jlong bytesWritten = 0;

                errorCode = paNoError;
                while (bytesWritten < framesInBytes)
                {
                    size_t outputCapacity
                        = portAudioStream->outputCapacity
                            - portAudioStream->outputLength;
                    jlong bytesToWrite;

                    if (JNI_TRUE == portAudioStream->finished)
                    {
                        errorCode = paStreamIsStopped;
                        break;
                    }
                    if (outputCapacity < 1)
                    {
                        ConditionVariable_wait(
                            portAudioStream->outputCondVar,
                            portAudioStream->outputMutex);
                        continue;
                    }

                    bytesToWrite = framesInBytes - bytesWritten;
                    if (bytesToWrite > outputCapacity)
                        bytesToWrite = outputCapacity;
                    memcpy(
                        ((jbyte *) portAudioStream->output)
                            + portAudioStream->outputLength,
                        data + bytesWritten,
                        bytesToWrite);

                    portAudioStream->outputLength += bytesToWrite;
                    bytesWritten += bytesToWrite;
                }
                Mutex_unlock(portAudioStream->outputMutex);
            }

            if (paNoError == errorCode)
            {
                if (audioQualityImprovement)
                {
                    AudioQualityImprovement_process(
                        audioQualityImprovement,
                        AUDIO_QUALITY_IMPROVEMENT_SAMPLE_ORIGIN_OUTPUT,
                        sampleRate, sampleSizeInBits, channels,
                        outputLatency,
                        data, framesInBytes);
                }

                data += framesInBytes;
            }
        }
    }
    else
    {
        PaStream *paStream = portAudioStream->stream;

        for (i = 0; i < numberOfWrites; i++)
        {
            errorCode = Pa_WriteStream(paStream, data, frames);
            if ((paNoError != errorCode) && (paOutputUnderflowed != errorCode))
                break;
            else
            {
                if (audioQualityImprovement)
                {
                    AudioQualityImprovement_process(
                        audioQualityImprovement,
                        AUDIO_QUALITY_IMPROVEMENT_SAMPLE_ORIGIN_OUTPUT,
                        sampleRate, sampleSizeInBits, channels,
                        outputLatency,
                        data, framesInBytes);
                }
                data += framesInBytes;
            }
        }
    }

    (*env)->ReleaseByteArrayElements(env, buffer, bufferBytes, JNI_ABORT);

    if ((paNoError != errorCode) && (paOutputUnderflowed != errorCode))
        PortAudio_throwException(env, errorCode);
}

JNIEXPORT jdouble JNICALL
Java_net_java_sip_communicator_impl_neomedia_portaudio_PortAudio_PaDeviceInfo_1getDefaultHighInputLatency
    (JNIEnv *env, jclass clazz, jlong deviceInfo)
{
    return ((PaDeviceInfo *) deviceInfo)->defaultHighInputLatency;
}

JNIEXPORT jdouble JNICALL
Java_net_java_sip_communicator_impl_neomedia_portaudio_PortAudio_PaDeviceInfo_1getDefaultHighOutputLatency
    (JNIEnv *env, jclass clazz, jlong deviceInfo)
{
    return ((PaDeviceInfo *) deviceInfo)->defaultHighOutputLatency;
}

JNIEXPORT jdouble JNICALL
Java_net_java_sip_communicator_impl_neomedia_portaudio_PortAudio_PaDeviceInfo_1getDefaultLowInputLatency
    (JNIEnv *env, jclass clazz, jlong deviceInfo)
{
    return ((PaDeviceInfo *) deviceInfo)->defaultLowInputLatency;
}

JNIEXPORT jdouble JNICALL
Java_net_java_sip_communicator_impl_neomedia_portaudio_PortAudio_PaDeviceInfo_1getDefaultLowOutputLatency
    (JNIEnv *env, jclass clazz, jlong deviceInfo)
{
    return ((PaDeviceInfo *) deviceInfo)->defaultLowOutputLatency;
}

JNIEXPORT jdouble JNICALL
Java_net_java_sip_communicator_impl_neomedia_portaudio_PortAudio_PaDeviceInfo_1getDefaultSampleRate
    (JNIEnv *env, jclass clazz, jlong deviceInfo)
{
    return ((PaDeviceInfo *) deviceInfo)->defaultSampleRate;
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_portaudio_PortAudio_PaDeviceInfo_1getHostApi
    (JNIEnv *env, jclass clazz, jlong deviceInfo)
{
    return ((PaDeviceInfo *) deviceInfo)->hostApi;
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_portaudio_PortAudio_PaDeviceInfo_1getMaxInputChannels
    (JNIEnv *env, jclass clazz, jlong deviceInfo)
{
    return ((PaDeviceInfo *) deviceInfo)->maxInputChannels;
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_portaudio_PortAudio_PaDeviceInfo_1getMaxOutputChannels
    (JNIEnv *env, jclass clazz, jlong deviceInfo)
{
    return ((PaDeviceInfo *) deviceInfo)->maxOutputChannels;
}

JNIEXPORT jbyteArray JNICALL
Java_net_java_sip_communicator_impl_neomedia_portaudio_PortAudio_PaDeviceInfo_1getNameBytes
    (JNIEnv *jniEnv, jclass clazz, jlong deviceInfo)
{
    const char *name = ((PaDeviceInfo *) deviceInfo)->name;
    jbyteArray nameBytes;

    if (name)
    {
        size_t nameLength = strlen(name);

        nameBytes = (*jniEnv)->NewByteArray(jniEnv, nameLength);
        if (nameBytes && nameLength)
        {
            (*jniEnv)->SetByteArrayRegion(
                    jniEnv,
                    nameBytes, 0, nameLength,
                    (jbyte *) name);
        }
    }
    else
        nameBytes = NULL;
    return nameBytes;
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_portaudio_PortAudio_PaHostApiInfo_1getDefaultInputDevice
    (JNIEnv *env, jclass clazz, jlong hostApi)
{
    return ((PaHostApiInfo *) hostApi)->defaultInputDevice;
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_portaudio_PortAudio_PaHostApiInfo_1getDefaultOutputDevice
    (JNIEnv *env, jclass clazz, jlong hostApi)
{
    return ((PaHostApiInfo *) hostApi)->defaultOutputDevice;
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_portaudio_PortAudio_PaHostApiInfo_1getDeviceCount
    (JNIEnv *env, jclass clazz, jlong hostApi)
{
    return ((PaHostApiInfo *) hostApi)->deviceCount;
}

JNIEXPORT jstring JNICALL
Java_net_java_sip_communicator_impl_neomedia_portaudio_PortAudio_PaHostApiInfo_1getName
    (JNIEnv *env, jclass clazz, jlong hostApi)
{
    const char *name = ((PaHostApiInfo *) hostApi)->name;

    /* PaHostApiInfo_GetName has been deprected in the Java source code. */
    return name ? (*env)->NewStringUTF(env, name) : NULL;
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_portaudio_PortAudio_PaHostApiInfo_1getType
    (JNIEnv *env, jclass clazz, jlong hostApi)
{
    return ((PaHostApiInfo *) hostApi)->type;
}

JNIEXPORT jlong JNICALL
Java_net_java_sip_communicator_impl_neomedia_portaudio_PortAudio_PaStreamParameters_1new
    (JNIEnv *env, jclass clazz,
    jint deviceIndex,
    jint channelCount,
    jlong sampleFormat,
    jdouble suggestedLatency)
{
    PaStreamParameters *streamParameters
        = (PaStreamParameters *) malloc(sizeof(PaStreamParameters));

    if (streamParameters)
    {
        streamParameters->device = deviceIndex;
        streamParameters->channelCount = channelCount;
        streamParameters->sampleFormat = sampleFormat;
        streamParameters->suggestedLatency = suggestedLatency;
        streamParameters->hostApiSpecificStreamInfo = NULL;
    }
    return (jlong) streamParameters;
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_portaudio_PortAudio_setDenoise
    (JNIEnv *jniEnv, jclass clazz, jlong stream, jboolean denoise)
{
    AudioQualityImprovement *audioQualityImprovement
        = ((PortAudioStream *) stream)->audioQualityImprovement;

    if (audioQualityImprovement)
        AudioQualityImprovement_setDenoise(audioQualityImprovement, denoise);
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_portaudio_PortAudio_setEchoFilterLengthInMillis
    (JNIEnv *jniEnv, jclass clazz, jlong stream, jlong echoFilterLengthInMillis)
{
    AudioQualityImprovement *audioQualityImprovement
        = ((PortAudioStream *) stream)->audioQualityImprovement;

    if (audioQualityImprovement)
    {
        AudioQualityImprovement_setEchoFilterLengthInMillis(
            audioQualityImprovement,
            echoFilterLengthInMillis);
    }
}

JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM *vm, void *reserved)
{
    AudioQualityImprovement_load();

    return JNI_VERSION_1_4;
}

JNIEXPORT void JNICALL
JNI_OnUnload(JavaVM *vm, void *reserved)
{
    AudioQualityImprovement_unload();
}

static PaStreamParameters *
PortAudio_fixInputParametersSuggestedLatency
    (PaStreamParameters *inputParameters,
    jdouble sampleRate, jlong framesPerBuffer,
    PaHostApiTypeId hostApiType)
{
    if (inputParameters)
    {
        const PaDeviceInfo *deviceInfo
            = Pa_GetDeviceInfo(inputParameters->device);

        if (deviceInfo)
        {
            PaTime suggestedLatency = inputParameters->suggestedLatency;

            if (suggestedLatency == LATENCY_LOW)
            {
                inputParameters->suggestedLatency
                    = deviceInfo->defaultLowInputLatency;
            }
            else if ((suggestedLatency == LATENCY_HIGH)
                    || (suggestedLatency == LATENCY_UNSPECIFIED))
            {
                inputParameters->suggestedLatency
                    = deviceInfo->defaultHighInputLatency;

                /*
                 * When the input latency is too low, we do not have a great
                 * chance to perform echo cancellation using it. Since the
                 * caller does not care about the input latency, try to request
                 * an input latency which increases our chances.
                 */
                PortAudio_fixStreamParametersSuggestedLatency(
                    inputParameters,
                    sampleRate, framesPerBuffer,
                    hostApiType);
            }
        }
    }
    return inputParameters;
}

static PaStreamParameters *
PortAudio_fixOutputParametersSuggestedLatency(
    PaStreamParameters *outputParameters,
    jdouble sampleRate, jlong framesPerBuffer,
    PaHostApiTypeId hostApiType)
{
    if (outputParameters)
    {
        const PaDeviceInfo *deviceInfo
            = Pa_GetDeviceInfo(outputParameters->device);

        if (deviceInfo)
        {
            PaTime suggestedLatency = outputParameters->suggestedLatency;

            if (suggestedLatency == LATENCY_LOW)
            {
                outputParameters->suggestedLatency
                    = deviceInfo->defaultLowOutputLatency;
            }
            else if ((suggestedLatency == LATENCY_HIGH)
                    || (suggestedLatency == LATENCY_UNSPECIFIED))
            {
                outputParameters->suggestedLatency
                    = deviceInfo->defaultHighOutputLatency;

                /*
                 * When the output latency is too low, we do not have a great
                 * chance to perform echo cancellation using it. Since the
                 * caller does not care about the output latency, try to request
                 * an output latency which increases our chances.
                 */
                PortAudio_fixStreamParametersSuggestedLatency(
                    outputParameters,
                    sampleRate, framesPerBuffer,
                    hostApiType);
            }
        }
    }
    return outputParameters;
}

static PaStreamParameters *
PortAudio_fixStreamParametersSuggestedLatency
    (PaStreamParameters *streamParameters,
    jdouble sampleRate, jlong framesPerBuffer,
    PaHostApiTypeId hostApiType)
{
    if ((paCoreAudio == hostApiType)
            && sampleRate
            && (paFramesPerBufferUnspecified != framesPerBuffer))
    {
        PaTime minLatency
            = (MIN_PLAY_DELAY_IN_FRAMES
                    * streamParameters->channelCount
                    * framesPerBuffer)
                / (2 * sampleRate);

        if (streamParameters->suggestedLatency < minLatency)
            streamParameters->suggestedLatency = minLatency;
    }
    return streamParameters;
}

static long
PortAudio_getFrameSize(PaStreamParameters *streamParameters)
{
    if (streamParameters)
    {
        PaError sampleSize = Pa_GetSampleSize(streamParameters->sampleFormat);

        if (paSampleFormatNotSupported != sampleSize)
            return sampleSize * streamParameters->channelCount;
    }
    return 0;
}

static unsigned long
PortAudio_getSampleSizeInBits(PaStreamParameters *streamParameters)
{
    if (streamParameters)
    {
        PaError sampleSize = Pa_GetSampleSize(streamParameters->sampleFormat);

        if (paSampleFormatNotSupported != sampleSize)
            return sampleSize * 8;
    }
    return 0;
}

static void
PortAudio_throwException(JNIEnv *env, PaError errorCode)
{
    jclass clazz
        = (*env)->FindClass(
                env,
                "net/java/sip/communicator/impl/neomedia/portaudio/PortAudioException");

    if (clazz)
        (*env)->ThrowNew(env, clazz, Pa_GetErrorText(errorCode));
}

/**
 * Allocates (and initializes) the memory and its associated variables for a
 * specific buffer to be used by the pseudo-blocking stream interface
 * implementation of a <tt>PortAudioStream</tt>.
 *
 * @param capacity the number of bytes to be allocated to the buffer
 * @param bufferPtr a pointer which specifies where the location of the
 * allocated buffer is to be stored
 * @param bufferLengthPtr a pointer which specifies where the initial length
 * (i.e. zero) is to be stored
 * @param bufferCapacityPtr a pointer which specifies where the capacity of the
 * allocated buffer is to be stored
 * @param bufferMutexPtr a pointer which specifies where the <tt>Mute</tt> to
 * synchronize the access to the allocated buffer is to be stored
 * @param bufferCondVarPtr a pointer which specifies where the
 * <tt>ConditionVariable</tt> to synchronize the access to the allocated buffer
 * is to be stored
 * @return the location of the allocated buffer upon success; otherwise,
 * <tt>NULL</tt>
 */
static void *
PortAudioStream_allocPseudoBlockingBuffer
    (size_t capacity,
    void **bufferPtr, size_t *bufferLengthPtr, size_t *bufferCapacityPtr,
    Mutex **bufferMutexPtr, ConditionVariable **bufferCondVarPtr)
{
    void *buffer = malloc(capacity);

    if (buffer)
    {
        Mutex *mutex = Mutex_new(NULL);

        if (mutex)
        {
            ConditionVariable *condVar = ConditionVariable_new(NULL);

            if (condVar)
            {
                if (bufferPtr)
                    *bufferPtr = buffer;
                if (bufferLengthPtr)
                    *bufferLengthPtr = 0;
                if (bufferCapacityPtr)
                    *bufferCapacityPtr = capacity;
                *bufferMutexPtr = mutex;
                *bufferCondVarPtr = condVar;
            }
            else
            {
                Mutex_free(mutex);
                free(buffer);
                buffer = NULL;
            }
        }
        else
        {
            free(buffer);
            buffer = NULL;
        }
    }
    return buffer;
}

static void
PortAudioStream_free(JNIEnv *env, PortAudioStream *stream)
{
    if (stream->streamCallback)
        (*env)->DeleteGlobalRef(env, stream->streamCallback);

    if (stream->inputMutex && !Mutex_lock(stream->inputMutex))
    {
        if (stream->input)
            free(stream->input);
        ConditionVariable_free(stream->inputCondVar);
        Mutex_unlock(stream->inputMutex);
        Mutex_free(stream->inputMutex);
    }

    if (stream->outputMutex && !Mutex_lock(stream->outputMutex))
    {
        if (stream->output)
            free(stream->output);
        ConditionVariable_free(stream->outputCondVar);
        Mutex_unlock(stream->outputMutex);
        Mutex_free(stream->outputMutex);
    }

    if (stream->audioQualityImprovement)
        AudioQualityImprovement_release(stream->audioQualityImprovement);

    if (stream->mutex)
        Mutex_free(stream->mutex);

    free(stream);
}

static int
PortAudioStream_javaCallback
    (const void *input,
    void *output,
    unsigned long frameCount,
    const PaStreamCallbackTimeInfo *timeInfo,
    PaStreamCallbackFlags statusFlags,
    void *userData)
{
    PortAudioStream *stream = (PortAudioStream *) userData;
    jobject streamCallback = stream->streamCallback;
    JNIEnv *env;
    jmethodID streamCallbackMethodID;

    if (!streamCallback)
        return paContinue;

    env = stream->env;
    if (!env)
    {
        JavaVM *vm = stream->vm;

        if ((*vm)->AttachCurrentThreadAsDaemon(vm, (void **) &env, NULL) < 0)
            return paAbort;
        else
            stream->env = env;
    }
    streamCallbackMethodID = stream->streamCallbackMethodID;
    if (!streamCallbackMethodID)
    {
        jclass streamCallbackClass
            = (*env)->GetObjectClass(env, streamCallback);

        streamCallbackMethodID
            = (*env)->GetMethodID(
                    env,
                    streamCallbackClass,
                    "callback",
                    "(Ljava/nio/ByteBuffer;Ljava/nio/ByteBuffer;)I");
        if (streamCallbackMethodID)
            stream->streamCallbackMethodID = streamCallbackMethodID;
        else
            return paAbort;
    }

    return
        (*env)->CallIntMethod(
                env,
                streamCallback,
                streamCallbackMethodID,
                input
                    ? (*env)->NewDirectByteBuffer(
                            env,
                            (void *) input,
                            frameCount * stream->inputFrameSize)
                    : NULL,
                output
                    ? (*env)->NewDirectByteBuffer(
                            env,
                            output,
                            frameCount * stream->outputFrameSize)
                    : NULL);
}

static void
PortAudioStream_javaFinishedCallback(void *userData)
{
    PortAudioStream *stream = (PortAudioStream *) userData;
    jobject streamCallback = stream->streamCallback;
    JNIEnv *env;
    jmethodID streamFinishedCallbackMethodID;

    if (!streamCallback)
        return;

    env = stream->env;
    if (!env)
    {
        JavaVM *vm = stream->vm;

        if ((*vm)->AttachCurrentThreadAsDaemon(vm, (void **) &env, NULL) < 0)
            return;
        else
            stream->env = env;
    }
    streamFinishedCallbackMethodID = stream->streamFinishedCallbackMethodID;
    if (!streamFinishedCallbackMethodID)
    {
        jclass streamCallbackClass
            = (*env)->GetObjectClass(env, streamCallback);

        streamFinishedCallbackMethodID
            = (*env)->GetMethodID(
                    env,
                    streamCallbackClass,
                    "finishedCallback",
                    "()V");
        if (streamFinishedCallbackMethodID)
            stream->streamFinishedCallbackMethodID
                = streamFinishedCallbackMethodID;
        else
            return;
    }

    (*env)->CallVoidMethod(env, streamCallback, streamFinishedCallbackMethodID);
}

static PortAudioStream *
PortAudioStream_new(JNIEnv *env, jobject streamCallback)
{
    PortAudioStream *stream = calloc(1, sizeof(PortAudioStream));

    if (!stream)
    {
        PortAudio_throwException(env, paInsufficientMemory);
        return NULL;
    }

    if (streamCallback)
    {
        if ((*env)->GetJavaVM(env, &(stream->vm)) < 0)
        {
            free(stream);
            PortAudio_throwException(env, paInternalError);
            return NULL;
        }

        stream->streamCallback = (*env)->NewGlobalRef(env, streamCallback);
        if (!(stream->streamCallback))
        {
            free(stream);
            PortAudio_throwException(env, paInsufficientMemory);
            return NULL;
        }
    }

    return stream;
}

static void
PortAudioStream_popFromPseudoBlockingBuffer
    (void *buffer, size_t length, size_t *bufferLengthPtr)
{
    size_t i;
    size_t newLength = *bufferLengthPtr - length;
    jbyte *oldBuffer = (jbyte *) buffer;
    jbyte *newBuffer = ((jbyte *) buffer) + length;

    for (i = 0; i < newLength; i++)
        *oldBuffer++ = *newBuffer++;
    *bufferLengthPtr = newLength;
}

static int
PortAudioStream_pseudoBlockingCallback
    (const void *input,
    void *output,
    unsigned long frameCount,
    const PaStreamCallbackTimeInfo *timeInfo,
    PaStreamCallbackFlags statusFlags,
    void *userData)
{
    PortAudioStream *stream = (PortAudioStream *) userData;

    if (input && stream->inputMutex && !Mutex_lock(stream->inputMutex))
    {
        size_t inputLength = frameCount * stream->inputFrameSize;
        size_t newInputLength;
        void *inputInStream;

        /*
         * Remember the specified input so that it can be retrieved later on in
         * our pseudo-blocking Pa_ReadStream().
         */
        newInputLength = stream->inputLength + inputLength;
        if (newInputLength > stream->inputCapacity)
        {
            PortAudioStream_popFromPseudoBlockingBuffer(
                stream->input,
                newInputLength - stream->inputCapacity,
                &(stream->inputLength));
        }
        inputInStream = ((jbyte *) (stream->input)) + stream->inputLength;
        memcpy(inputInStream, input, inputLength);
        stream->inputLength += inputLength;

        ConditionVariable_notify(stream->inputCondVar);
        Mutex_unlock(stream->inputMutex);
    }
    if (output && stream->outputMutex && !Mutex_lock(stream->outputMutex))
    {
        size_t outputLength = frameCount * stream->outputFrameSize;
        size_t availableOutputLength = outputLength;

        if (availableOutputLength > stream->outputLength)
            availableOutputLength = stream->outputLength;
        memcpy(output, stream->output, availableOutputLength);
        PortAudioStream_popFromPseudoBlockingBuffer(
            stream->output,
            availableOutputLength,
            &(stream->outputLength));
        if (availableOutputLength < outputLength)
        {
            memset(
                ((jbyte *) output) + availableOutputLength,
                0,
                outputLength - availableOutputLength);
        }

        ConditionVariable_notify(stream->outputCondVar);
        Mutex_unlock(stream->outputMutex);
    }
    return paContinue;
}

static void
PortAudioStream_pseudoBlockingFinishedCallback(void *userData)
{
    PortAudioStream *stream = (PortAudioStream *) userData;

    if (!Mutex_lock(stream->mutex))
    {
        stream->finished = JNI_TRUE;
        if (stream->inputMutex && !Mutex_lock(stream->inputMutex))
        {
            ConditionVariable_notify(stream->inputCondVar);
            Mutex_unlock(stream->inputMutex);
        }
        if (stream->outputMutex && !Mutex_lock(stream->outputMutex))
        {
            ConditionVariable_notify(stream->outputCondVar);
            Mutex_unlock(stream->outputMutex);
        }
        Mutex_unlock(stream->mutex);
    }
    PortAudioStream_release(stream);
}

static void
PortAudioStream_release(PortAudioStream *stream)
{
    if (!Mutex_lock(stream->mutex))
    {
        --(stream->retainCount);
        if (stream->retainCount < 1)
        {
            Mutex_unlock(stream->mutex);
            PortAudioStream_free(NULL, stream);
        }
        else
            Mutex_unlock(stream->mutex);
    }
}

static void
PortAudioStream_retain(PortAudioStream *stream)
{
    if (!Mutex_lock(stream->mutex))
    {
        ++(stream->retainCount);
        Mutex_unlock(stream->mutex);
    }
}
