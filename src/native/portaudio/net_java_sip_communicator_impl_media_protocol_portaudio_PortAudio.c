#include "net_java_sip_communicator_impl_media_protocol_portaudio_PortAudio.h"

#include <portaudio.h>
#include <stdlib.h>

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
}
PortAudioStream;

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

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_media_protocol_portaudio_PortAudio_Pa_1CloseStream(
	JNIEnv *env, jclass clazz, jlong stream)
{
	PortAudioStream *portAudioStream = (PortAudioStream *) stream;
	PaError errorCode = Pa_CloseStream(portAudioStream->stream);

	if (paNoError != errorCode)
		PortAudio_throwException(env, errorCode);
	else
		PortAudioStream_free(env, portAudioStream);
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_media_protocol_portaudio_PortAudio_Pa_1GetDeviceCount(
	JNIEnv *env, jclass clazz)
{
	PaDeviceIndex deviceCount = Pa_GetDeviceCount();

	if (deviceCount < 0)
		PortAudio_throwException(env, deviceCount);
	return deviceCount;
}

JNIEXPORT jlong JNICALL
Java_net_java_sip_communicator_impl_media_protocol_portaudio_PortAudio_Pa_1GetDeviceInfo(
	JNIEnv *env, jclass clazz, jint deviceIndex)
{
	return (jlong) Pa_GetDeviceInfo(deviceIndex);
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_media_protocol_portaudio_PortAudio_Pa_1Initialize(
	JNIEnv *env, jclass clazz)
{
	PaError errorCode = Pa_Initialize();

	if (paNoError != errorCode)
		PortAudio_throwException(env, errorCode);
}

JNIEXPORT jlong JNICALL
Java_net_java_sip_communicator_impl_media_protocol_portaudio_PortAudio_Pa_1OpenStream(
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

	errorCode
		= Pa_OpenStream(
			&(stream->stream),
			PortAudio_fixInputParametersSuggestedLatency(inputStreamParameters),
			PortAudio_fixOutputParametersSuggestedLatency(
				outputStreamParameters),
			sampleRate,
			framesPerBuffer,
			streamFlags,
                        streamCallback == NULL ? NULL : PortAudioStream_callback,
			stream);

	if (paNoError == errorCode)
	{
		stream->inputFrameSize = PortAudio_getFrameSize(inputStreamParameters);
		stream->outputFrameSize
			= PortAudio_getFrameSize(outputStreamParameters);

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
Java_net_java_sip_communicator_impl_media_protocol_portaudio_PortAudio_Pa_1StartStream(
	JNIEnv *env, jclass clazz, jlong stream)
{
	PaError errorCode = Pa_StartStream(((PortAudioStream *) stream)->stream);

	if (paNoError != errorCode)
		PortAudio_throwException(env, errorCode);
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_media_protocol_portaudio_PortAudio_Pa_1StopStream(
	JNIEnv *env, jclass clazz, jlong stream)
{
	PaError errorCode = Pa_StopStream(((PortAudioStream *) stream)->stream);

	if (paNoError != errorCode)
		PortAudio_throwException(env, errorCode);
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_media_protocol_portaudio_PortAudio_Pa_1WriteStream(
	JNIEnv *env, jclass clazz, jlong stream, jbyteArray buffer, jlong frames)
{
	jbyte* data = (*env)->GetByteArrayElements(env, buffer, NULL);

	if (data)
	{
		PaError errorCode
			= Pa_WriteStream(
				((PortAudioStream *) stream)->stream,
				data,
				frames);

		(*env)->ReleaseByteArrayElements(env, buffer, data, 0);

		if (paNoError != errorCode && errorCode != paOutputUnderflowed)
			PortAudio_throwException(env, errorCode);

/*
                if(errorCode == paOutputUnderflowed)
                {
                    printf("OutputUnderflowed\n");fflush(stdout);
                }
*/
	}
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_media_protocol_portaudio_PortAudio_Pa_1ReadStream
  (JNIEnv *env, jclass clazz, jlong stream, jbyteArray buffer, jlong frames)
{
    jbyte* data = (*env)->GetByteArrayElements(env, buffer, NULL);
    PaError errorCode = Pa_ReadStream(
            ((PortAudioStream *) stream)->stream,
            data,
            frames);
    (*env)->ReleaseByteArrayElements(env, buffer, data, 0);

    if (paNoError != errorCode && errorCode != paInputOverflowed)
        PortAudio_throwException(env, errorCode);
}

JNIEXPORT jlong JNICALL
Java_net_java_sip_communicator_impl_media_protocol_portaudio_PortAudio_Pa_1GetStreamReadAvailable
  (JNIEnv *env, jclass clazz, jlong stream)
{
    return Pa_GetStreamReadAvailable(((PortAudioStream *) stream)->stream);
}

JNIEXPORT jlong JNICALL
Java_net_java_sip_communicator_impl_media_protocol_portaudio_PortAudio_Pa_1GetStreamWriteAvailable
  (JNIEnv *env, jclass clazz, jlong stream)
{
    return Pa_GetStreamWriteAvailable(((PortAudioStream *) stream)->stream);
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_media_protocol_portaudio_PortAudio_Pa_1GetSampleSize
  (JNIEnv *env, jclass clazz, jlong format)
{
    return Pa_GetSampleSize(format);
}

JNIEXPORT jboolean JNICALL
Java_net_java_sip_communicator_impl_media_protocol_portaudio_PortAudio_Pa_1IsFormatSupported
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
Java_net_java_sip_communicator_impl_media_protocol_portaudio_PortAudio_PaDeviceInfo_1getMaxInputChannels(
	JNIEnv *env, jclass clazz, jlong deviceInfo)
{
	return ((PaDeviceInfo *) deviceInfo)->maxInputChannels;
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_media_protocol_portaudio_PortAudio_PaDeviceInfo_1getMaxOutputChannels(
	JNIEnv *env, jclass clazz, jlong deviceInfo)
{
	return ((PaDeviceInfo *) deviceInfo)->maxOutputChannels;
}


JNIEXPORT jstring JNICALL
Java_net_java_sip_communicator_impl_media_protocol_portaudio_PortAudio_PaDeviceInfo_1getName(
	JNIEnv *env, jclass clazz, jlong deviceInfo)
{
	const char *name = ((PaDeviceInfo *) deviceInfo)->name;

	return name ? (*env)->NewStringUTF(env, name) : NULL;
}

JNIEXPORT jdouble JNICALL
Java_net_java_sip_communicator_impl_media_protocol_portaudio_PortAudio_PaDeviceInfo_1getDefaultSampleRate
  (JNIEnv *env, jclass clazz, jlong deviceInfo)
{
    return ((PaDeviceInfo *) deviceInfo)->defaultSampleRate;
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_media_protocol_portaudio_PortAudio_PaDeviceInfo_1getHostApi
  (JNIEnv *env, jclass clazz, jlong deviceInfo)
{
    return ((PaDeviceInfo *) deviceInfo)->hostApi;
}

JNIEXPORT jlong JNICALL
Java_net_java_sip_communicator_impl_media_protocol_portaudio_PortAudio_Pa_1GetHostApiInfo
  (JNIEnv *env , jclass clazz, jint hostApiIndex)
{
    return (jlong) Pa_GetHostApiInfo(hostApiIndex);
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_media_protocol_portaudio_PortAudio_PaHostApiInfo_1GetType
  (JNIEnv *env, jclass clazz, jlong hostApi)
{
    return ((PaHostApiInfo *) hostApi)->type;
}

JNIEXPORT jstring JNICALL
Java_net_java_sip_communicator_impl_media_protocol_portaudio_PortAudio_PaHostApiInfo_1GetName
  (JNIEnv *env, jclass clazz, jlong hostApi)
{
    const char *name = ((PaHostApiInfo *) hostApi)->name;

    return name ? (*env)->NewStringUTF(env, name) : NULL;
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_media_protocol_portaudio_PortAudio_PaHostApiInfo_1GetDeviceCount
  (JNIEnv *env, jclass clazz, jlong hostApi)
{
    return ((PaHostApiInfo *) hostApi)->deviceCount;
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_media_protocol_portaudio_PortAudio_PaHostApiInfo_1GetDefaultInputDevice
  (JNIEnv *env, jclass clazz, jlong hostApi)
{
    return ((PaHostApiInfo *) hostApi)->defaultInputDevice;
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_media_protocol_portaudio_PortAudio_PaHostApiInfo_1GetDefaultOutputDevice
  (JNIEnv *env, jclass clazz, jlong hostApi)
{
    return ((PaHostApiInfo *) hostApi)->defaultOutputDevice;
}

JNIEXPORT jlong JNICALL
Java_net_java_sip_communicator_impl_media_protocol_portaudio_PortAudio_PaStreamParameters_1new(
	JNIEnv *env,
	jclass clazz,
	jint deviceIndex,
	jint channelCount,
	jlong sampleFormat)
{
	PaStreamParameters *streamParameters
		= (PaStreamParameters *) malloc(sizeof(PaStreamParameters));

	if (streamParameters)
	{
		streamParameters->device = deviceIndex;
		streamParameters->channelCount = channelCount;
		streamParameters->sampleFormat = sampleFormat;
		streamParameters->suggestedLatency = 0,4;
		streamParameters->hostApiSpecificStreamInfo = NULL;
	}
	return (jlong) streamParameters;
}

static PaStreamParameters *
PortAudio_fixInputParametersSuggestedLatency(
	PaStreamParameters *inputParameters)
{
	if (inputParameters && (0 == inputParameters->suggestedLatency))
	{
		PaDeviceInfo *deviceInfo = Pa_GetDeviceInfo(inputParameters->device);

		if (deviceInfo)
			inputParameters->suggestedLatency
				= deviceInfo->defaultHighInputLatency;
	}
	return inputParameters;
}

static PaStreamParameters *
PortAudio_fixOutputParametersSuggestedLatency(
	PaStreamParameters *outputParameters)
{
	if (outputParameters && (0 == outputParameters->suggestedLatency))
	{
		PaDeviceInfo *deviceInfo = Pa_GetDeviceInfo(outputParameters->device);

		if (deviceInfo)
			outputParameters->suggestedLatency
				= deviceInfo->defaultHighOutputLatency;
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
				"net/java/sip/communicator/impl/media/protocol/portaudio/PortAudioException");

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

	if ((*env)->GetJavaVM(env, &(stream->vm)) < 0)
	{
		free(stream);
		PortAudio_throwException(env, paInternalError);
		return NULL;
	}

	if (streamCallback)
	{
		stream->streamCallback = (*env)->NewGlobalRef(env, streamCallback);
		if (!(stream->streamCallback))
		{
			free(stream);
			PortAudio_throwException(env, paInsufficientMemory);
			return NULL;
		}
	}
	else
		stream->streamCallback = NULL;

	stream->env = NULL;
	stream->streamCallbackMethodID = NULL;
	stream->streamFinishedCallbackMethodID = NULL;

	return stream;
}
