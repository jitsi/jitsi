/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
#include "net_java_sip_communicator_impl_neomedia_portaudio_PortAudio.h"

#include <math.h>
#include <portaudio.h>
#include <speex/speex_resampler.h>
#include <speex/speex_preprocess.h>
#include <speex/speex_echo.h>
#include <stdlib.h>
#include <string.h>
//#include <sys/time.h> /* used on Windows */
#include <time.h>

typedef struct
{
    long time;
    short *data;
    struct Buffer *next;
} Buffer;

typedef struct
{
    PaStream *stream;
    jobject streamCallback;
    JavaVM *vm;
    JNIEnv *env;
    jmethodID streamCallbackMethodID;
    jmethodID streamFinishedCallbackMethodID;
    long inputFrameSize;
    long outputFrameSize;
    double samplerate;

    SpeexResamplerState *outputResampler;
    double outputResampleFactor;
    int outputChannelCount;

    SpeexPreprocessState *preprocessor;
    SpeexResamplerState *inputResampler;
    double inputResampleFactor;
    int inputChannelCount;

    SpeexEchoState *echoState;
    struct PortAudioStream *connectedToStream;
    int startCaching;

    Buffer *first;
    Buffer *last;
} PortAudioStream;

#define DEFAULT_SAMPLE_RATE 44100.0

static void PortAudio_throwException(JNIEnv *env, PaError errorCode);
static PaStreamParameters * PortAudio_fixInputParametersSuggestedLatency(
    PaStreamParameters *inputParameters);
static PaStreamParameters * PortAudio_fixOutputParametersSuggestedLatency(
    PaStreamParameters *outputParameters);
static long PortAudio_getFrameSize(PaStreamParameters *streamParameters);

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
static PaError PortAudioStream_write(
    PortAudioStream *stream,
    jbyte *buffer,
    jlong frames);

static void clear(PortAudioStream *st)
{
    int cleared = 0;
    Buffer *curr = st->first;

    while(curr != NULL)
    {
        Buffer *n = curr->next;
        free(curr->data);
        free(curr);
        curr = n;
        cleared++;
    }
    st->first = NULL;
    st->last = NULL;
}

static void addBuffer(PortAudioStream *st, Buffer *b)
{
    if(st->last != NULL)
    {
        st->last->next = b;
        st->last = b;
    }
    else
    {
        st->first = b;
        st->last = b;
    }
}

static Buffer* getBuffer(PortAudioStream *st, int time)
{
    if(st->first == NULL)
        return NULL;

    Buffer *res = st->first;

    st->first = st->first->next;
    if(st->first == NULL)
        st->last = NULL;
    return res;
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_portaudio_PortAudio_setEchoCancelParams(
        JNIEnv *env,
        jclass clazz,
        jlong instream,
        jlong outstream,
        jboolean enableDenoise,
        jboolean enableEchoCancel,
        jint frameSize,
        jint filterLength)
{
    PortAudioStream *inAudioStream = (PortAudioStream *) instream;
    PortAudioStream *outAudioStream = (PortAudioStream *) outstream;

    // only if denoise is enabled and preprocessor is not created
    if(enableDenoise && inAudioStream->preprocessor == NULL)
    {
        SpeexPreprocessState *pp =
                speex_preprocess_state_init(frameSize, inAudioStream->samplerate);
        inAudioStream->preprocessor = pp;

        int option = 1;
        speex_preprocess_ctl(pp, SPEEX_PREPROCESS_SET_DENOISE, &option);
        option = 2;
        speex_preprocess_ctl(pp, SPEEX_PREPROCESS_SET_VAD, &option);
    }

    if(enableEchoCancel && outAudioStream != 0)
    {
        if(inAudioStream->preprocessor == NULL)
            inAudioStream->preprocessor =
                speex_preprocess_state_init(frameSize, inAudioStream->samplerate);

        inAudioStream->echoState =
            speex_echo_state_init(frameSize, filterLength);

        speex_preprocess_ctl(
            inAudioStream->preprocessor,
            SPEEX_PREPROCESS_SET_ECHO_STATE,
            inAudioStream->echoState);

        inAudioStream->connectedToStream = outAudioStream;
        outAudioStream->connectedToStream = inAudioStream;
    }
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

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_portaudio_PortAudio_Pa_1CloseStream(
    JNIEnv *env, jclass clazz, jlong stream)
{
    PortAudioStream *portAudioStream = (PortAudioStream *) stream;

    if(!portAudioStream)
        return;

    /* Before clearing and destroying any part of current stream
     * clear it from any connected stream so it wont be used
     */
    if(portAudioStream->connectedToStream != NULL
        && ((PortAudioStream *)portAudioStream->connectedToStream)->
                connectedToStream != NULL)
    {
        ((PortAudioStream *)portAudioStream->connectedToStream)->
            connectedToStream = NULL;
        portAudioStream->connectedToStream = NULL;
    }

    PaError errorCode = Pa_CloseStream(portAudioStream->stream);

    if(portAudioStream->outputResampleFactor != 1.0
        && portAudioStream->outputResampler)
    {
        speex_resampler_destroy(portAudioStream->outputResampler);
        portAudioStream->outputResampler = NULL;
    }

    if(portAudioStream->inputResampleFactor != 1.0
        && portAudioStream->inputResampler)
    {
        speex_resampler_destroy(portAudioStream->inputResampler);
        portAudioStream->inputResampler = NULL;

        if(portAudioStream->preprocessor)
        {
            speex_preprocess_state_destroy(portAudioStream->preprocessor);
            portAudioStream->preprocessor = NULL;
        }

        if(portAudioStream->echoState)
        {
            speex_echo_state_destroy(portAudioStream->echoState);
            portAudioStream->echoState = NULL;
        }        
        clear(portAudioStream);
    }

    if (paNoError != errorCode)
            PortAudio_throwException(env, errorCode);
    else
            PortAudioStream_free(env, portAudioStream);
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_portaudio_PortAudio_Pa_1AbortStream
  (JNIEnv *env, jclass clazz, jlong stream)
{
    PaError errorCode = Pa_AbortStream(((PortAudioStream *) stream)->stream);

    if (paNoError != errorCode)
        PortAudio_throwException(env, errorCode);
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

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_portaudio_PortAudio_Pa_1Initialize(
    JNIEnv *env, jclass clazz)
{
    PaError errorCode = Pa_Initialize();

    if (paNoError != errorCode)
        PortAudio_throwException(env, errorCode);
}

JNIEXPORT jlong JNICALL
Java_net_java_sip_communicator_impl_neomedia_portaudio_PortAudio_Pa_1OpenStream(
    JNIEnv *env,
    jclass clazz,
    jlong inputParameters,
    jlong outputParameters,
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

    double defSampleRate = DEFAULT_SAMPLE_RATE;

    /*
     *  Obay default sample rate of the device. some devices has default 44.1kHz
     * and some 48kHz.
     */
    if(outputStreamParameters)
        defSampleRate
            = Pa_GetDeviceInfo(outputStreamParameters->device)
                ->defaultSampleRate;
    else if(inputStreamParameters)
        defSampleRate
            = Pa_GetDeviceInfo(inputStreamParameters->device)
                ->defaultSampleRate;

    errorCode
        = Pa_OpenStream(
            &(stream->stream),
            PortAudio_fixInputParametersSuggestedLatency(inputStreamParameters),
            PortAudio_fixOutputParametersSuggestedLatency(
                outputStreamParameters),
            defSampleRate,
            framesPerBuffer,
            streamFlags,
            streamCallback ? PortAudioStream_callback : NULL,
            stream);

    stream->samplerate = sampleRate;

    if(outputStreamParameters)
    {
        stream->outputResampleFactor = defSampleRate / sampleRate;
        if(stream->outputResampleFactor != 1.0)
        {
            stream->outputChannelCount = outputStreamParameters->channelCount;

            // resample quality 3 is for voip
            stream->outputResampler = speex_resampler_init(
                stream->outputChannelCount, sampleRate, defSampleRate, 3, NULL);
        }
    }
    else
        stream->outputResampleFactor = 1.0;
    
    if(inputStreamParameters)
    {
        stream->inputResampleFactor = defSampleRate / sampleRate;

        if(stream->inputResampleFactor != 1.0)
        {
            stream->inputChannelCount = inputStreamParameters->channelCount;

            // resample quality 3 is for voip
            stream->inputResampler = speex_resampler_init(
                stream->inputChannelCount, defSampleRate, sampleRate, 3, NULL);

            stream->inputFrameSize
                = PortAudio_getFrameSize(inputStreamParameters);
        }
    }
    else
        stream->inputResampleFactor = 1.0;

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
Java_net_java_sip_communicator_impl_neomedia_portaudio_PortAudio_Pa_1StartStream(
    JNIEnv *env, jclass clazz, jlong stream)
{
    PaError errorCode = Pa_StartStream(((PortAudioStream *) stream)->stream);

    if (paNoError != errorCode)
        PortAudio_throwException(env, errorCode);
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_portaudio_PortAudio_Pa_1StopStream(
    JNIEnv *env, jclass clazz, jlong stream)
{
    PaError errorCode = Pa_StopStream(((PortAudioStream *) stream)->stream);

    if (paNoError != errorCode)
        PortAudio_throwException(env, errorCode);
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_portaudio_PortAudio_Pa_1WriteStream(
        JNIEnv *env,
        jclass clazz,
        jlong stream,
        jbyteArray buffer,
        jint offset,
        jlong frames,
        jint numberOfWrites)
{
    jbyte *bufferBytes;
    jbyte* data;
    PortAudioStream *portAudioStream;
    long frameSize;
    PaError errorCode;
    jint i;

    bufferBytes = (*env)->GetByteArrayElements(env, buffer, NULL);
    if (!bufferBytes)
        return;

    data = bufferBytes + offset;
    portAudioStream = (PortAudioStream *) stream;
    frameSize = portAudioStream->outputFrameSize;

    for (i = 0; i < numberOfWrites; i++)
    {
        errorCode = PortAudioStream_write(portAudioStream, data, frames);
        if ((paNoError != errorCode) && (errorCode != paOutputUnderflowed))
            break;
        else
            data += frames * frameSize;
    }

    (*env)->ReleaseByteArrayElements(env, buffer, bufferBytes, 0);

    if ((paNoError != errorCode) && (errorCode != paOutputUnderflowed))
        PortAudio_throwException(env, errorCode);
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_portaudio_PortAudio_Pa_1ReadStream
    (JNIEnv *env, jclass clazz, jlong stream, jbyteArray buffer, jlong frames)
{
    jbyte* data = (*env)->GetByteArrayElements(env, buffer, NULL);

    PortAudioStream *inStream = (PortAudioStream *) stream;
    if (data)
    {
        PaError errorCode;
        if(inStream->inputResampleFactor != 1)
        {
            spx_uint32_t in_len;
            in_len = lrint(
                frames
                *inStream->inputChannelCount
                *inStream->inputResampleFactor);

            short res[in_len];

            errorCode = Pa_ReadStream(
                inStream->stream,
                res,
                frames*inStream->inputResampleFactor);

            // if echo is enabled
            if(inStream->echoState != NULL)
            {
                // lets say to player to start caching
                if(inStream->connectedToStream != NULL)
                {
                    if(((PortAudioStream *)inStream->connectedToStream)->startCaching == 0)
                        ((PortAudioStream *)inStream->connectedToStream)->startCaching = 1;
                    else if(((PortAudioStream *)inStream->connectedToStream)->first != NULL)
                    {
                        short tempBuffer[160];

                        speex_resampler_process_interleaved_int(
                                inStream->inputResampler,
                                res,
                                &in_len,
                                tempBuffer,
                                //data,
                                &frames);

                        if(inStream->echoState != NULL)
                        {
                            struct timeval tv;
                            gettimeofday(&tv,NULL);

                            int time = tv.tv_sec*1000 + tv.tv_usec/1000;
                            Buffer *b = getBuffer(
                                (PortAudioStream *)inStream->connectedToStream, time);
                            if(b != NULL)
                            {
                                short *t;
                                t = b->data;
                                speex_echo_cancellation(
                                        inStream->echoState, tempBuffer, t, data);
                                free(b);
                            }
                        }

                        if(inStream->preprocessor != NULL)
                            speex_preprocess_run(inStream->preprocessor, data);
                    }
                    else
                    {
                        // lets just do the job se we wont return empty data
                        speex_resampler_process_interleaved_int(
                            inStream->inputResampler,
                            res,
                            &in_len,
                            data,
                            &frames);

                        if(inStream->preprocessor != NULL)
                        {
                            speex_preprocess_run(inStream->preprocessor, data);
                        }
                    }
                }
            }
            else
            {
                speex_resampler_process_interleaved_int(
                        inStream->inputResampler,
                        res,
                        &in_len,
                        data,
                        &frames);

                if(inStream->preprocessor != NULL)
                {
                    speex_preprocess_run(inStream->preprocessor, data);
                }
            }
        }
        else
        {
            errorCode = Pa_ReadStream(
                inStream->stream,
                data,
                frames);
        }

        (*env)->ReleaseByteArrayElements(env, buffer, data, 0);

        if ((paNoError != errorCode) && (paInputOverflowed != errorCode))
            PortAudio_throwException(env, errorCode);
    }
}

JNIEXPORT jlong JNICALL
Java_net_java_sip_communicator_impl_neomedia_portaudio_PortAudio_Pa_1GetStreamReadAvailable
  (JNIEnv *env, jclass clazz, jlong stream)
{
    return Pa_GetStreamReadAvailable(((PortAudioStream *) stream)->stream) /
            ((PortAudioStream *) stream)->inputResampleFactor;
}

JNIEXPORT jlong JNICALL
Java_net_java_sip_communicator_impl_neomedia_portaudio_PortAudio_Pa_1GetStreamWriteAvailable
  (JNIEnv *env, jclass clazz, jlong stream)
{
    return Pa_GetStreamWriteAvailable(((PortAudioStream *) stream)->stream);
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_portaudio_PortAudio_Pa_1GetSampleSize
  (JNIEnv *env, jclass clazz, jlong format)
{
    return Pa_GetSampleSize(format);
}

JNIEXPORT jboolean JNICALL
Java_net_java_sip_communicator_impl_neomedia_portaudio_PortAudio_Pa_1IsFormatSupported
  (JNIEnv *env, jclass clazz, 
    jlong inputParameters,
    jlong outputParameters,
    jdouble sampleRate)
{
    if(Pa_IsFormatSupported(
            (PaStreamParameters *) inputParameters,
            (PaStreamParameters *) outputParameters,
            sampleRate) == paFormatIsSupported)
        return JNI_TRUE;
    else
        return JNI_FALSE;
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_portaudio_PortAudio_PaDeviceInfo_1getMaxInputChannels(
    JNIEnv *env, jclass clazz, jlong deviceInfo)
{
    return ((PaDeviceInfo *) deviceInfo)->maxInputChannels;
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_portaudio_PortAudio_PaDeviceInfo_1getMaxOutputChannels(
    JNIEnv *env, jclass clazz, jlong deviceInfo)
{
    return ((PaDeviceInfo *) deviceInfo)->maxOutputChannels;
}


JNIEXPORT jstring JNICALL
Java_net_java_sip_communicator_impl_neomedia_portaudio_PortAudio_PaDeviceInfo_1getName(
    JNIEnv *env, jclass clazz, jlong deviceInfo)
{
    const char *name = ((PaDeviceInfo *) deviceInfo)->name;

    return name ? (*env)->NewStringUTF(env, name) : NULL;
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

JNIEXPORT jlong JNICALL
Java_net_java_sip_communicator_impl_neomedia_portaudio_PortAudio_Pa_1GetHostApiInfo
  (JNIEnv *env , jclass clazz, jint hostApiIndex)
{
    return (jlong) Pa_GetHostApiInfo(hostApiIndex);
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_portaudio_PortAudio_PaHostApiInfo_1GetType
  (JNIEnv *env, jclass clazz, jlong hostApi)
{
    return ((PaHostApiInfo *) hostApi)->type;
}

JNIEXPORT jstring JNICALL
Java_net_java_sip_communicator_impl_neomedia_portaudio_PortAudio_PaHostApiInfo_1GetName
  (JNIEnv *env, jclass clazz, jlong hostApi)
{
    const char *name = ((PaHostApiInfo *) hostApi)->name;

    return name ? (*env)->NewStringUTF(env, name) : NULL;
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_portaudio_PortAudio_PaHostApiInfo_1GetDeviceCount
  (JNIEnv *env, jclass clazz, jlong hostApi)
{
    return ((PaHostApiInfo *) hostApi)->deviceCount;
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_portaudio_PortAudio_PaHostApiInfo_1GetDefaultInputDevice
  (JNIEnv *env, jclass clazz, jlong hostApi)
{
    return ((PaHostApiInfo *) hostApi)->defaultInputDevice;
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_portaudio_PortAudio_PaHostApiInfo_1GetDefaultOutputDevice
  (JNIEnv *env, jclass clazz, jlong hostApi)
{
    return ((PaHostApiInfo *) hostApi)->defaultOutputDevice;
}

JNIEXPORT jlong JNICALL
Java_net_java_sip_communicator_impl_neomedia_portaudio_PortAudio_PaStreamParameters_1new(
    JNIEnv *env,
    jclass clazz,
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

static PaStreamParameters *
PortAudio_fixInputParametersSuggestedLatency(
    PaStreamParameters *inputParameters)
{
    if (inputParameters)
    {
        PaDeviceInfo *deviceInfo = Pa_GetDeviceInfo(inputParameters->device);

        if (deviceInfo)
        {
            if(inputParameters->suggestedLatency ==
                net_java_sip_communicator_impl_neomedia_portaudio_PortAudio_LATENCY_LOW)
                inputParameters->suggestedLatency =
                    deviceInfo->defaultLowInputLatency;
            else if(inputParameters->suggestedLatency ==
                net_java_sip_communicator_impl_neomedia_portaudio_PortAudio_LATENCY_HIGH
                    || inputParameters->suggestedLatency == 0)
                inputParameters->suggestedLatency =
                    deviceInfo->defaultHighInputLatency;
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
        PaDeviceInfo *deviceInfo = Pa_GetDeviceInfo(outputParameters->device);

        if (deviceInfo)
        {
            if(outputParameters->suggestedLatency ==
                net_java_sip_communicator_impl_neomedia_portaudio_PortAudio_LATENCY_LOW)
                outputParameters->suggestedLatency =
                    deviceInfo->defaultLowOutputLatency;
            else if(outputParameters->suggestedLatency ==
                net_java_sip_communicator_impl_neomedia_portaudio_PortAudio_LATENCY_HIGH
                    || outputParameters->suggestedLatency == 0)
                outputParameters->suggestedLatency =
                    deviceInfo->defaultHighOutputLatency;
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

static void
PortAudio_throwException(JNIEnv *env, PaError errorCode)
{
    jclass clazz
        = (*env)
            ->FindClass(
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
            = (*env)
                ->GetMethodID(
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
        (*env)
            ->CallIntMethod(
                env,
                streamCallback,
                streamCallbackMethodID,
                input
                    ? (*env)
                        ->NewDirectByteBuffer(
                            env,
                            input,
                            frameCount * stream->inputFrameSize)
                    : NULL,
                output
                    ? (*env)
                        ->NewDirectByteBuffer(
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
            = (*env)
                ->GetMethodID(
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
    {
        (*env)->DeleteGlobalRef(env, stream->streamCallback);
        stream->streamCallback = NULL;
    }
    stream->streamCallbackMethodID = NULL;
    stream->streamFinishedCallbackMethodID = NULL;

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
    stream->stream = NULL;
    stream->preprocessor = NULL;
    stream->echoState = NULL;
    stream->first = NULL;
    stream->last = NULL;
    stream->connectedToStream = NULL;
    stream->startCaching = 0;

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

    stream->env = NULL;
    stream->streamCallbackMethodID = NULL;
    stream->streamFinishedCallbackMethodID = NULL;

    return stream;
}

static PaError
PortAudioStream_write(
        PortAudioStream *stream,
        jbyte *buffer,
        jlong frames)
{
    PaError errorCode;

    if(stream->outputResampleFactor != 1)
    {
        spx_uint32_t out_len;
        out_len = lrint(
            frames
            *stream->outputChannelCount
            *stream->outputResampleFactor);

        short res[out_len];
        speex_resampler_process_interleaved_int(
                stream->outputResampler,
                buffer,
                &frames,
                res,
                &out_len);

        errorCode = Pa_WriteStream(stream->stream, res, out_len);

        if(stream->connectedToStream
                && ((PortAudioStream *)stream->connectedToStream)
                        ->echoState
                && (stream->startCaching == 1))
        {
            Buffer *b;

            b = malloc(sizeof(Buffer));
            if (b)
            {
                struct timeval tv;

                gettimeofday(&tv,NULL);
                b->time = tv.tv_sec*1000 + tv.tv_usec/1000;

                b->data = malloc(sizeof(short)*frames);
                if (b->data)
                {
                    memcpy(b->data, buffer, sizeof(short)*frames);
                    b->next = NULL;
                    addBuffer(stream, b);
                }
                else
                {
                    free(b);
                    errorCode = paInsufficientMemory;
                }
            }
            else
                errorCode = paInsufficientMemory;
        }
    }
    else
        errorCode = Pa_WriteStream(stream->stream, buffer, frames);
    return errorCode;
}
