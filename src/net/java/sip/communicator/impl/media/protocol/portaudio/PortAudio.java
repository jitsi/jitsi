/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.media.protocol.portaudio;

/**
 * PortAudio functions.
 * @author Lubomir Marinov
 */
public final class PortAudio
{
    static
    {
        System.loadLibrary("jportaudio");
    }

    /**
     * Can be passed as the framesPerBuffer parameter to
     * Pa_OpenStream() or Pa_OpenDefaultStream() to indicate that the stream
     * callback will accept buffers of any size.
     */
    public static final long FRAMES_PER_BUFFER_UNSPECIFIED = 0;

    /**
     * A type used to specify one or more sample formats.
     * The standard format paInt16.
     */
    public static final long SAMPLE_FORMAT_INT16 = 0x00000008;

    /**
     * A type used to specify one or more sample formats.
     * The format paFloat32.
     */
    public static final long SAMPLE_FORMAT_FLOAT32 = 0x00000001;

    /**
     * A type used to specify one or more sample formats.
     * The format paInt32.
     */
    public static final long SAMPLE_FORMAT_INT32 = 0x00000002;

    /**
     * A type used to specify one or more sample formats.
     * The format paInt24.
     */
    public static final long SAMPLE_FORMAT_INT24 = 0x00000004;

    /**
     * A type used to specify one or more sample formats.
     * The format paInt8.
     */
    public static final long SAMPLE_FORMAT_INT8 = 0x00000010;

    /**
     * A type used to specify one or more sample formats.
     * The format paUInt8.
     */
    public static final long SAMPLE_FORMAT_UINT8 = 0x00000020;

    /**
     * Flags used to control the behavior of a stream.
     * They are passed as parameters to Pa_OpenStream or Pa_OpenDefaultStream.
     */
    public static final long STREAM_FLAGS_NO_FLAG = 0;

    /**
     * Disable default clipping of out of range samples.
     */
    public static final long STREAM_FLAGS_CLIP_OFF = 0x00000001;

    /**
     * Disable default dithering.
     */
    public static final long STREAM_FLAGS_DITHER_OFF = 0x00000002;

    /**
     * Flag requests that where possible a full duplex stream will not discard
     * overflowed input samples without calling the stream callback.
     * This flag is only valid for full duplex callback streams and only when
     * used in combination with the paFramesPerBufferUnspecified (0)
     * framesPerBuffer parameter. Using this flag incorrectly results in a
     * paInvalidFlag error being returned from
     * Pa_OpenStream and Pa_OpenDefaultStream.
     */
    public static final long STREAM_FLAGS_NEVER_DROP_INPUT = 0x00000004;

    /**
     * Call the stream callback to fill initial output buffers, rather than
     * the default behavior of priming the buffers with zeros (silence).
     * This flag has no effect for input-only and blocking read/write streams.
     */
    public static final long
        STREAM_FLAGS_PRIME_OUTPUT_BUFFERS_USING_STREAM_CALLBACK = 0x00000008;

    /**
     * A mask specifying the platform specific bits.
     */
    public static final long STREAM_FLAGS_PLATFORM_SPECIFIC_FLAGS = 0xFFFF0000;

    private static boolean initialized;

    /**
     * Used to initialize the portaudio lib.
     *
     * @throws PortAudioException if error comes from portaudio.
     */
    public static synchronized void initialize()
        throws PortAudioException
    {
        if (!initialized)
        {
            Pa_Initialize();
            initialized = true;
        }
    }

    /**
     * Retrieve the index of the default input device.
     * @return The default input device index for the default host API,
     *         or paNoDevice if no default input device is available or
     *         an error was encountered.
     */
    public static native int Pa_GetDefaultInputDevice();

    /**
     * Retrieve the index of the default output device.
     * @return The default input device index for the default host API,
     *         or paNoDevice if no default input device is available or
     *         an error was encountered.
     */
    public static native int Pa_GetDefaultOutputDevice();

    /**
     * Closes an audio stream. If the audio stream is active it discards
     * any pending buffers as if Pa_AbortStream() had been called.
     * @param stream the steam pointer.
     * @throws PortAudioException
     */
    public static native void Pa_CloseStream(long stream)
        throws PortAudioException;

    /**
     * Retrieve the number of available devices.
     * The number of available devices may be zero.
     * @return the number of devices.
     * @throws PortAudioException
     */
    public static native int Pa_GetDeviceCount()
        throws PortAudioException;

    /**
     * Retrieve a pointer to a PaDeviceInfo structure containing
     * information about the specified device.
     * @param deviceIndex the device index
     * @return pointer to device info structure.
     */
    public static native long Pa_GetDeviceInfo(int deviceIndex);

    /**
     * Library initialization function - call this before using PortAudio.s
     * @throws PortAudioException
     */
    private static native void Pa_Initialize()
        throws PortAudioException;

    /**
     * Opens a stream for either input, output or both.
     * @param inputParameters the input params or 0 if absent.
     * @param outputParameters the ouput params or 0 if absent.
     * @param sampleRate The desired sampleRate.
     * @param framesPerBuffer The number of frames passed to the stream
     *        callback function, or the preferred block granularity for
     *        a blocking read/write stream
     * @param streamFlags Flags which modify the
     *        behaviour of the streaming process.
     * @param streamCallback A pointer to a client supplied function that is
     *        responsible for processing and filling input and output buffers.
     *        If this parameter is NULL the stream will be opened in
     *        'blocking read/write' mode.
     * @return pointer to the opened stream.
     * @throws PortAudioException
     */
    public static native long Pa_OpenStream(
            long inputParameters,
            long outputParameters,
            double sampleRate,
            long framesPerBuffer,
            long streamFlags,
            PortAudioStreamCallback streamCallback)
        throws PortAudioException;

    /**
     * Commences audio processing.
     * @param stream pointer to the stream
     * @throws PortAudioException
     */
    public static native void Pa_StartStream(long stream)
        throws PortAudioException;

    /**
     * Terminates audio processing. It waits until all pending audio buffers
     * have been played before it returns.
     * @param stream pointer to the stream
     * @throws PortAudioException
     */
    public static native void Pa_StopStream(long stream)
        throws PortAudioException;

    /**
     * Write samples to an output stream. This function doesn't return until
     * the entire buffer has been consumed - this may involve waiting for the
     * operating system to consume the data.
     * @param stream pointer to the stream
     * @param buffer A buffer of sample frames.
     * @param frames The number of frames to be written from buffer.
     * @throws PortAudioException
     */
    public static native void Pa_WriteStream(
            long stream,
            byte[] buffer,
            long frames)
        throws PortAudioException;

    /**
     * Read samples from an input stream. The function doesn't return until 
     * the entire buffer has been filled - this may involve waiting for
     * the operating system to supply the data.
     * @param stream pointer to the stream.
     * @param buffer a buffer of sample frames.
     * @param frames The number of frames to be read into buffer.
     * @throws PortAudioException
     */
    public static native void Pa_ReadStream(
            long stream, byte[] buffer, long frames)
        throws PortAudioException;

    /**
     * Retrieve the number of frames that can be read from the stream
     * without waiting.
     * @param stream pointer to the stream.
     * @return returns a non-negative value representing the maximum number
     *         of frames that can be read from the stream without blocking
     *         or busy waiting or, a PaErrorCode (which are always negative)
     *         if PortAudio is not initialized or an error is encountered.
     */
    public static native long Pa_GetStreamReadAvailable(long stream);

    /**
     * Retrieve the number of frames that can be written to the stream
     * without waiting.
     * @param stream pointer to the stream.
     * @return returns a non-negative value representing the maximum number
     *         of frames that can be written to the stream without blocking
     *         or busy waiting or, a PaErrorCode (which are always negative)
     *         if PortAudio is not initialized or an error is encountered.
     */
    public static native long Pa_GetStreamWriteAvailable(long stream);

    /**
     * Retrieve the size of a given sample format in bytes.
     * @param format the format.
     * @return The size in bytes of a single sample in the specified format,
     *         or paSampleFormatNotSupported if the format is not supported.
     */
    public static native int Pa_GetSampleSize(long format);

    /**
     * Determine whether it would be possible to open a stream
     * with the specified parameters.
     * @param inputParameters A structure that describes the
     *        input parameters used to open a stream.
     * @param outputParameters A structure that describes the output
     *        parameters used to open a stream.
     * @param sampleRate The required sampleRate.
     * @return returns 0 if the format is supported, and an error code
     *         indicating why the format is not supported otherwise.
     *         The constant paFormatIsSupported is provided to compare
     *         with the return value for success.
     */
    public static native boolean Pa_IsFormatSupported(
        long inputParameters,
        long outputParameters,
        double sampleRate);

    /**
     * Maximum input channels for the device.
     * @param deviceInfo device info pointer.
     * @return Maximum input channels for the device.
     */
    public static native int PaDeviceInfo_getMaxInputChannels(long deviceInfo);

    /**
     * Maximum output channels for the device.
     * @param deviceInfo device info pointer.
     * @return Maximum output channels for the device.
     */
    public static native int PaDeviceInfo_getMaxOutputChannels(long deviceInfo);

    /**
     * The name of the device.
     * @param deviceInfo device info pointer.
     * @return The name of the device.
     */
    public static native String PaDeviceInfo_getName(long deviceInfo);

    /**
     * The default samplerate for the deviec.
     * @param deviceInfo device info pointer.
     * @return the default sameple rate for the device.
     */
    public static native double PaDeviceInfo_getDefaultSampleRate(long deviceInfo);

    /**
     * The host api of the device.
     * @param deviceInfo device info pointer.
     * @return The host api of the device.
     */
    public static native int PaDeviceInfo_getHostApi(long deviceInfo);

    /**
     * Returns defaultLowInputLatency for the device.
     * @param deviceInfo device info pointer.
     * @return defaultLowInputLatency for the device.
     */
    public static native double PaDeviceInfo_getDefaultLowInputLatency(long deviceInfo);

    /**
     * Returns defaultLowOutputLatency for the device.
     * @param deviceInfo device info pointer.
     * @return defaultLowOutputLatency for the device.
     */
    public static native double PaDeviceInfo_getDefaultLowOutputLatency(long deviceInfo);

    /**
     * Returns defaultHighInputLatency for the device.
     * @param deviceInfo device info pointer.
     * @return defaultHighInputLatency for the device.
     */
    public static native double PaDeviceInfo_getDefaultHighInputLatency(long deviceInfo);

    /**
     * Returns defaultHighOutputLatency for the device.
     * @param deviceInfo device info pointer.
     * @return defaultHighOutputLatency for the device.
     */
    public static native double PaDeviceInfo_getDefaultHighOutputLatency(long deviceInfo);

    /**
     * Retrieve a pointer to a structure containing information
     * about a specific host Api.
     * @param hostApiIndex host api index.
     * @return A pointer to an immutable PaHostApiInfo structure
     *         describing a specific host API.
     */
    public static native long Pa_GetHostApiInfo(long hostApiIndex);

    /**
     * The well known unique identifier of this host API.
     * @param hostApiInfo pointer to host api info structure.
     * @return The well known unique identifier of this host API.
     *         Enumerator:
                    paInDevelopment
                    paDirectSound
                    paMME
                    paASIO
                    paSoundManager
                    paCoreAudio
                    paOSS
                    paALSA
                    paAL
                    paBeOS
                    paWDMKS
                    paJACK
                    paWASAPI
                    paAudioScienceHPI
     */
    public static native int PaHostApiInfo_GetType(long hostApiInfo);

    /**
     * A textual description of the host API for display on user interfaces.
     * @param hostApiInfo pointer to host api info structure.
     * @return host api name.
     */
    public static native String PaHostApiInfo_GetName(long hostApiInfo);

    /**
     * The number of devices belonging to this host API.
     * @param hostApiInfo pointer to host api info structure.
     * @return The number of devices belonging to this host API.
     */
    public static native int PaHostApiInfo_GetDeviceCount(long hostApiInfo);

    /**
     * The default input device for this host API.
     * @param hostApiInfo pointer to host api info structure.
     * @return The default input device for this host API.
     */
    public static native int PaHostApiInfo_GetDefaultInputDevice(long hostApiInfo);

    /**
     * The default output device for this host API.
     * @param hostApiInfo pointer to host api info structure.
     * @return The default output device for this host API.
     */
    public static native int PaHostApiInfo_GetDefaultOutputDevice(long hostApiInfo);

    /**
     * Creates parameters used for opening streams.
     * @param deviceIndex the device.
     * @param channelCount the channels to be used.
     * @param sampleFormat the sample format.
     * @return pointer to the params used for Pa_OpenStream.
     */
    public static native long PaStreamParameters_new(
        int deviceIndex,
        int channelCount,
        long sampleFormat);

    /**
     * Prevents the creation of <code>PortAudio</code> instances.
     */
    private PortAudio()
    {
    }
}
