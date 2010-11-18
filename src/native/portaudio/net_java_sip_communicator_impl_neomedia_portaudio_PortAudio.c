/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

#include "net_java_sip_communicator_impl_neomedia_portaudio_PortAudio.h"

#include "AudioQualityImprovement.h"
#include <portaudio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/time.h>

typedef struct
{
    AudioQualityImprovement *audioQualityImprovement;
    int channels;
    JNIEnv *env;
    long inputFrameSize;
    long outputFrameSize;
    double sampleRate;
    int sampleSizeInBits;
    PaStream *stream;
    jobject streamCallback;
    jmethodID streamCallbackMethodID;
    jmethodID streamFinishedCallbackMethodID;
    JavaVM *vm;
} PortAudioStream;

static PaStreamParameters * PortAudio_fixInputParametersSuggestedLatency
    (PaStreamParameters *inputParameters);
static PaStreamParameters * PortAudio_fixOutputParametersSuggestedLatency
    (PaStreamParameters *outputParameters);
static long PortAudio_getFrameSize(PaStreamParameters *streamParameters);
static unsigned long PortAudio_getSampleSizeInBits
    (PaStreamParameters *streamParameters);
static void PortAudio_throwException(JNIEnv *env, PaError errorCode);
static jlong System_currentTimeMillis();

static int PortAudioStream_callback(
    const void *input,
    void *output,
    unsigned long frameCount,
    const PaStreamCallbackTimeInfo *timeInfo,
    PaStreamCallbackFlags statusFlags,
    void *userData);
static void PortAudioStream_finishedCallback(void *userData);
static void PortAudioStream_free(JNIEnv *env, PortAudioStream *stream);
static PortAudioStream * PortAudioStream_new(
    JNIEnv *env, jobject streamCallback);

static const char *AUDIO_QUALITY_IMPROVEMENT_STRING_ID = "portaudio";
#define LATENCY_HIGH net_java_sip_communicator_impl_neomedia_portaudio_PortAudio_LATENCY_HIGH
#define LATENCY_LOW net_java_sip_communicator_impl_neomedia_portaudio_PortAudio_LATENCY_LOW

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
    PaError errorCode;
    PaStreamParameters *inputStreamParameters
        = (PaStreamParameters *) inputParameters;
    PaStreamParameters *outputStreamParameters
        = (PaStreamParameters *) outputParameters;

    if (!stream)
        return 0;

    errorCode
        = Pa_OpenStream(
            &(stream->stream),
            PortAudio_fixInputParametersSuggestedLatency(inputStreamParameters),
            PortAudio_fixOutputParametersSuggestedLatency(
                outputStreamParameters),
            sampleRate,
            framesPerBuffer,
            streamFlags,
            streamCallback ? PortAudioStream_callback : NULL,
            stream);

    if (paNoError == errorCode)
    {
        stream->inputFrameSize
                = PortAudio_getFrameSize(inputStreamParameters);
        stream->outputFrameSize
                = PortAudio_getFrameSize(outputStreamParameters);

        if (streamCallback)
            errorCode
                = Pa_SetStreamFinishedCallback(
                    stream->stream,
                    PortAudioStream_finishedCallback);
        
        stream->audioQualityImprovement
            = AudioQualityImprovement_getSharedInstance(
                AUDIO_QUALITY_IMPROVEMENT_STRING_ID,
                0);
        stream->sampleRate = sampleRate;
        if (inputStreamParameters)
        {
            stream->sampleSizeInBits
                = PortAudio_getSampleSizeInBits(inputStreamParameters);
            stream->channels = inputStreamParameters->channelCount;
            if (stream->audioQualityImprovement)
            {
                AudioQualityImprovement_setSampleRate(
                    stream->audioQualityImprovement,
                    (int) sampleRate);
            }
        }
        else if (outputStreamParameters)
        {
            stream->sampleSizeInBits
                = PortAudio_getSampleSizeInBits(outputStreamParameters);
            stream->channels = outputStreamParameters->channelCount;
        }

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
        jlong startTime, endTime;
        PortAudioStream *portAudioStream = (PortAudioStream *) stream;
        PaError errorCode;

/*
        startTime = System_currentTimeMillis();
 */
        errorCode = Pa_ReadStream(portAudioStream->stream, data, frames);
/*
        endTime = System_currentTimeMillis();
 */
        if ((paNoError == errorCode) || (paInputOverflowed == errorCode))
        {
            if (portAudioStream->audioQualityImprovement)
            {
                AudioQualityImprovement_process(
                    portAudioStream->audioQualityImprovement,
                    AUDIO_QUALITY_IMPROVEMENT_SAMPLE_ORIGIN_INPUT,
                    portAudioStream->sampleRate,
                    portAudioStream->sampleSizeInBits,
                    portAudioStream->channels,
                    data, frames * portAudioStream->inputFrameSize,
                    startTime, endTime);
            }
            (*env)->ReleaseByteArrayElements(env, buffer, data, 0);
        }
        else
        {
            (*env)->ReleaseByteArrayElements(env, buffer, data, 0);
            PortAudio_throwException(env, errorCode);
        }
    }
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_portaudio_PortAudio_Pa_1StartStream
    (JNIEnv *env, jclass clazz, jlong stream)
{
    PaError errorCode = Pa_StartStream(((PortAudioStream *) stream)->stream);

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
    PaStream *paStream;
    AudioQualityImprovement *audioQualityImprovement;
    double sampleRate;
    unsigned long sampleSizeInBits;
    int channels;
    long framesInBytes;
    PaError errorCode;
    jint i;

    bufferBytes = (*env)->GetByteArrayElements(env, buffer, NULL);
    if (!bufferBytes)
        return;
    data = bufferBytes + offset;

    portAudioStream = (PortAudioStream *) stream;
    paStream = portAudioStream->stream;
    audioQualityImprovement = portAudioStream->audioQualityImprovement;
    sampleRate = portAudioStream->sampleRate;
    sampleSizeInBits = portAudioStream->sampleSizeInBits;
    channels = portAudioStream->channels;
    framesInBytes = frames * portAudioStream->outputFrameSize;

    for (i = 0; i < numberOfWrites; i++)
    {
        jlong startTime, endTime;

/*
        startTime = System_currentTimeMillis();
 */
        errorCode = Pa_WriteStream(paStream, data, frames);
/*
        endTime = System_currentTimeMillis();
 */
        if ((paNoError != errorCode) && (errorCode != paOutputUnderflowed))
            break;
        else
        {
            if (audioQualityImprovement)
            {
                AudioQualityImprovement_process(
                    audioQualityImprovement,
                    AUDIO_QUALITY_IMPROVEMENT_SAMPLE_ORIGIN_OUTPUT,
                    sampleRate, sampleSizeInBits, channels,
                    data, framesInBytes,
                    startTime, endTime);
            }
            data += framesInBytes;
        }
    }

    (*env)->ReleaseByteArrayElements(env, buffer, bufferBytes, JNI_ABORT);

    if ((paNoError != errorCode) && (errorCode != paOutputUnderflowed))
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

static PaStreamParameters *
PortAudio_fixInputParametersSuggestedLatency
    (PaStreamParameters *inputParameters)
{
    if (inputParameters)
    {
        const PaDeviceInfo *deviceInfo
            = Pa_GetDeviceInfo(inputParameters->device);

        if (deviceInfo)
        {
            if (inputParameters->suggestedLatency == LATENCY_LOW)
            {
                inputParameters->suggestedLatency
                    = deviceInfo->defaultLowInputLatency;
            }
            else if ((inputParameters->suggestedLatency == LATENCY_HIGH)
                    || (inputParameters->suggestedLatency == 0))
            {
                inputParameters->suggestedLatency
                    = deviceInfo->defaultHighInputLatency;
            }
        }
    }
    return inputParameters;
}

static PaStreamParameters *
PortAudio_fixOutputParametersSuggestedLatency(
    PaStreamParameters *outputParameters)
{
    if (outputParameters)
    {
        const PaDeviceInfo *deviceInfo
            = Pa_GetDeviceInfo(outputParameters->device);

        if (deviceInfo)
        {
            if (outputParameters->suggestedLatency == LATENCY_LOW)
            {
                outputParameters->suggestedLatency
                    = deviceInfo->defaultLowOutputLatency;
            }
            else if ((outputParameters->suggestedLatency == LATENCY_HIGH)
                    || (outputParameters->suggestedLatency == 0))
            {
                outputParameters->suggestedLatency
                    = deviceInfo->defaultHighOutputLatency;
            }
        }
    }
    return outputParameters;
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

static int
PortAudioStream_callback(
    const void *input,
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
PortAudioStream_finishedCallback(void *userData)
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

static void
PortAudioStream_free(JNIEnv *env, PortAudioStream *stream)
{
    if (stream->streamCallback)
        (*env)->DeleteGlobalRef(env, stream->streamCallback);

    if (stream->audioQualityImprovement)
        AudioQualityImprovement_release(stream->audioQualityImprovement);

    free(stream);
}

static PortAudioStream *
PortAudioStream_new(JNIEnv *env, jobject streamCallback)
{
    PortAudioStream *stream = malloc(sizeof(PortAudioStream));

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
    else
    {
        stream->vm = NULL;
        stream->streamCallback = NULL;
    }

    stream->audioQualityImprovement = NULL;
    stream->env = NULL;
    stream->stream = NULL;
    stream->streamCallbackMethodID = NULL;
    stream->streamFinishedCallbackMethodID = NULL;

    return stream;
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
