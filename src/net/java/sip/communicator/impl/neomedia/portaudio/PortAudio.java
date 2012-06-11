/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.portaudio;

import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.*;

import net.java.sip.communicator.impl.neomedia.*;
import net.java.sip.communicator.impl.neomedia.codec.*;
import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.util.*;

/**
 * Provides the interface to the native PortAudio library.
 *
 * @author Lyubomir Marinov
 * @author Damian Minkov
 * @author Sebastien Vincent
 */
public final class PortAudio
{
    /**
     * The <tt>Logger</tt> used by the <tt>PortAudio</tt> class for logging
     * output.
     */
    private static final Logger logger = Logger.getLogger(PortAudio.class);

    static
    {
        System.loadLibrary("jnportaudio");

        try
        {
            Pa_Initialize();
        }
        catch (PortAudioException paex)
        {
            throw new UndeclaredThrowableException(paex);
        }
    }

    /**
     * The number of milliseconds to be read from or written to a native
     * PortAudio stream in a single transfer of data.
     */
    public static final int DEFAULT_MILLIS_PER_BUFFER = 20;

    /**
     * The default value for the sample rate of the input and the output
     * PortAudio streams with which they are to be opened if no other specific
     * sample rate is specified to the PortAudio <tt>DataSource</tt> or
     * <tt>PortAudioRenderer</tt> that they represent.
     */
    public static final double DEFAULT_SAMPLE_RATE = 44100;

    private static Runnable devicesChangedCallback;

    /**
     * Can be passed as the framesPerBuffer parameter to
     * <tt>Pa_OpenStream()</tt> or <tt>Pa_OpenDefaultStream()</tt> to indicate
     * that the stream callback will accept buffers of any size.
     */
    public static final long FRAMES_PER_BUFFER_UNSPECIFIED = 0;

    /**
     * Used when creating new stream parameters for suggested latency to use
     * high input/output value.
     */
    public static final double LATENCY_HIGH = -1d;

    /**
     * Used when creating new stream parameters for suggested latency to use low
     * input/default value.
     */
    public static final double LATENCY_LOW = -2d;

    /**
     * Used when creating new stream parameters for suggested latency to use
     * default value.
     */
    public static final double LATENCY_UNSPECIFIED = 0d;

    /**
     * PortAudio "no device" constant.
     */
    public static final int paNoDevice = -1;

    /**
     * The name of the <tt>double</tt> property which determines the suggested
     * latency to be used when opening PortAudio streams.
     */
    private static final String PROP_SUGGESTED_LATENCY
        = "net.java.sip.communicator.impl.neomedia.portaudio.suggestedLatency";

    /**
     * A type used to specify one or more sample formats. The standard format
     * <tt>paFloat32</tt>.
     */
    public static final long SAMPLE_FORMAT_FLOAT32 = 0x00000001;

    /**
     * A type used to specify one or more sample formats. The standard format
     * <tt>paInt8</tt>.
     */
    public static final long SAMPLE_FORMAT_INT8 = 0x00000010;

    /**
     * A type used to specify one or more sample formats. The standard format
     * <tt>paInt16</tt>.
     */
    public static final long SAMPLE_FORMAT_INT16 = 0x00000008;

    /**
     * A type used to specify one or more sample formats. The standard format
     * <tt>paInt24</tt>.
     */
    public static final long SAMPLE_FORMAT_INT24 = 0x00000004;

    /**
     * A type used to specify one or more sample formats. The standard format
     * <tt>paInt32</tt>.
     */
    public static final long SAMPLE_FORMAT_INT32 = 0x00000002;

    /**
     * A type used to specify one or more sample formats. The standard format
     * <tt>paUInt8</tt>.
     */
    public static final long SAMPLE_FORMAT_UINT8 = 0x00000020;

    /** Disables default clipping of out of range samples. */
    public static final long STREAM_FLAGS_CLIP_OFF = 0x00000001;

    /** Disables default dithering. */
    public static final long STREAM_FLAGS_DITHER_OFF = 0x00000002;

    /**
     * Flag requests that where possible a full duplex stream will not discard
     * overflowed input samples without calling the stream callback. This flag
     * is only valid for full duplex callback streams and only when used in
     * combination with the <tt>paFramesPerBufferUnspecified</tt> (<tt>0</tt>)
     * framesPerBuffer parameter. Using this flag incorrectly results in a
     * <tt>paInvalidFlag</tt> error being returned from <tt>Pa_OpenStream</tt>
     * and <tt>Pa_OpenDefaultStream</tt>.
     */
    public static final long STREAM_FLAGS_NEVER_DROP_INPUT = 0x00000004;

    /**
     * Flags used to control the behavior of a stream. They are passed as
     * parameters to <tt>Pa_OpenStream</tt> or <tt>Pa_OpenDefaultStream</tt>.
     */
    public static final long STREAM_FLAGS_NO_FLAG = 0;

    /** A mask specifying the platform specific bits. */
    public static final long STREAM_FLAGS_PLATFORM_SPECIFIC_FLAGS = 0xFFFF0000;

    /**
     * Call the stream callback to fill initial output buffers, rather than the
     * default behavior of priming the buffers with zeros (silence). This flag
     * has no effect for input-only and blocking read/write streams.
     */
    public static final long
        STREAM_FLAGS_PRIME_OUTPUT_BUFFERS_USING_STREAM_CALLBACK
            = 0x00000008;

    /**
     * Unchanging unique identifiers for each supported host API. This type
     * is used in the PaHostApiInfo structure. The values are guaranteed to be
     * unique and to never change, thus allowing code to be written that
     * conditionally uses host API specific extensions.
     */
    public static enum PaHostApiTypeId
    {
        undefined(-1), /* Used only outside pa for undefined PaHostApiTypeId */
        paInDevelopment(0),/* use while developing support for a new host API */
        paDirectSound(1),
        paMME(2),
        paASIO(3),
        paSoundManager(4),
        paCoreAudio(5),
        paOSS(7),
        paALSA(8),
        paAL(9),
        paBeOS(10),
        paWDMKS(11),
        paJACK(12),
        paWASAPI(13),
        paAudioScienceHPI(14);

        int value;
        private PaHostApiTypeId(int value)
        {
            this.value = value;
        }

        /**
         * Returns PaHostApiTypeId by its type unique value.
         *
         * @param value the value.
         * @return the corresponding PaHostApiTypeId.
         */
        public static PaHostApiTypeId valueOf(int value)
        {
            for(PaHostApiTypeId ha : values())
            {
                if(ha.value == value)
                    return ha;
            }

            return valueOf(value);
        }
    }

    private static native void free(long ptr);

    /**
     * Gets the native <tt>PaSampleFormat</tt> with a specific size in bits.
     *
     * @param sampleSizeInBits the size in bits of the native
     * <tt>PaSampleFormat</tt> to get
     * @return the native <tt>PaSampleFormat</tt> with the specified size in
     * bits
     */
    public static long getPaSampleFormat(int sampleSizeInBits)
    {
        switch (sampleSizeInBits)
        {
        case 8:
            return PortAudio.SAMPLE_FORMAT_INT8;
        case 24:
            return PortAudio.SAMPLE_FORMAT_INT24;
        case 32:
            return PortAudio.SAMPLE_FORMAT_INT32;
        default:
            return PortAudio.SAMPLE_FORMAT_INT16;
        }
    }

    /**
     * Gets the suggested latency to be used when opening PortAudio streams.
     *
     * @return the suggested latency to be used when opening PortAudio streams
     */
    public static double getSuggestedLatency()
    {
        ConfigurationService cfg = NeomediaActivator.getConfigurationService();

        if (cfg != null)
        {
            String suggestedLatencyString
                = cfg.getString(PROP_SUGGESTED_LATENCY);

            if (suggestedLatencyString != null)
            {
                try
                {
                    double suggestedLatency
                        = Double.parseDouble(suggestedLatencyString);
    
                    if (suggestedLatency != LATENCY_UNSPECIFIED)
                        return suggestedLatency;
                }
                catch (NumberFormatException nfe)
                {
                    logger.error(
                            "Failed to parse configuration property "
                                + PROP_SUGGESTED_LATENCY
                                + " value as a double",
                            nfe);
                }
            }
        }

        if (OSUtils.IS_MAC || OSUtils.IS_LINUX)
            return LATENCY_HIGH;
        else if (OSUtils.IS_WINDOWS)
            return 0.1d;
        else
            return LATENCY_UNSPECIFIED;
    }

    /**
     * Terminates audio processing immediately without waiting for pending
     * buffers to complete.
     *
     * @param stream the steam pointer.
     * @throws PortAudioException
     */
    public static native void Pa_AbortStream(long stream)
        throws PortAudioException;

    /**
     * Closes an audio stream. If the audio stream is active it discards any
     * pending buffers as if <tt>Pa_AbortStream()</tt> had been called.
     *
     * @param stream the steam pointer.
     * @throws PortAudioException
     */
    public static native void Pa_CloseStream(long stream)
        throws PortAudioException;

    /**
     * Retrieve the index of the default input device.
     *
     * @return The default input device index for the default host API, or
     * <tt>paNoDevice</tt> if no default input device is available or an error
     * was encountered.
     */
    public static native int Pa_GetDefaultInputDevice();

    /**
     * Retrieve the index of the default output device.
     *
     * @return The default input device index for the default host API, or
     * <tt>paNoDevice</tt> if no default input device is available or an error
     * was encountered.
     */
    public static native int Pa_GetDefaultOutputDevice();

    /**
     * Retrieve the number of available devices. The number of available devices
     * may be zero.
     *
     * @return the number of devices.
     * @throws PortAudioException
     */
    public static native int Pa_GetDeviceCount()
        throws PortAudioException;

    /**
     * Retrieve a pointer to a PaDeviceInfo structure containing information
     * about the specified device.
     *
     * @param deviceIndex the device index
     * @return pointer to device info structure.
     */
    public static native long Pa_GetDeviceInfo(int deviceIndex);

    /**
     * Retrieve a pointer to a structure containing information about a specific
     * host Api.
     *
     * @param hostApiIndex host api index.
     * @return A pointer to an immutable PaHostApiInfo structure describing a
     * specific host API.
     */
    public static native long Pa_GetHostApiInfo(int hostApiIndex);

    /**
     * Retrieve the size of a given sample format in bytes.
     *
     * @param format the format.
     * @return The size in bytes of a single sample in the specified format, or
     * <tt>paSampleFormatNotSupported</tt> if the format is not supported.
     */
    public static native int Pa_GetSampleSize(long format);

    /**
     * Retrieve the number of frames that can be read from the stream without
     * waiting.
     *
     * @param stream pointer to the stream.
     * @return returns a non-negative value representing the maximum number of
     * frames that can be read from the stream without blocking or busy waiting
     * or, a <tt>PaErrorCode</tt> (which are always negative) if PortAudio is
     * not initialized or an error is encountered.
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
     * Initializes the native PortAudio library.
     *
     * @throws PortAudioException
     */
    private static native void Pa_Initialize()
        throws PortAudioException;

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

    public static void Pa_UpdateAvailableDeviceList()
    {
        updateAvailableDeviceList();
    }

    /**
     * Write samples to an output stream. This function doesn't return until
     * the entire buffer has been consumed - this may involve waiting for the
     * operating system to consume the data.
     * @param stream pointer to the stream
     * @param buffer A buffer of sample frames.
     * @param frames The number of frames to be written from buffer.
     * @throws PortAudioException
     */
    public static void Pa_WriteStream(long stream, byte[] buffer, long frames)
        throws PortAudioException
    {
        Pa_WriteStream(stream, buffer, 0, frames, 1);
    }

    /**
     * Writes samples to an output stream. Does not return until the specified
     * samples have been consumed - this may involve waiting for the operating
     * system to consume the data.
     * <p>
     * Provides better efficiency than achieved through multiple consecutive
     * calls to {@link #Pa_WriteStream(long, byte[], long)} with one and the
     * same buffer because the JNI access to the bytes of the buffer which is
     * likely to copy the whole buffer is only performed once.
     * </p>
     *
     * @param stream the pointer to the PortAudio stream to write the samples to
     * @param buffer the buffer containing the samples to be written
     * @param offset the byte offset in <tt>buffer</tt> at which the samples to
     * be written start
     * @param frames the number of frames from <tt>buffer</tt> starting at
     * <tt>offset</tt> are to be written with a single write
     * @param numberOfWrites the number of writes each writing <tt>frames</tt>
     * number of frames to be performed
     * @throws PortAudioException if anything goes wrong while writing
     */
    public static native void Pa_WriteStream(
            long stream,
            byte[] buffer,
            int offset,
            long frames,
            int numberOfWrites)
        throws PortAudioException;

    /**
     * Gets the human-readable name of the <tt>PaDeviceInfo</tt> specified by a
     * pointer to it.
     *
     * @param deviceInfo the pointer to the <tt>PaDeviceInfo</tt> to get the
     * human-readable name of
     * @return the human-readable name of the <tt>PaDeviceInfo</tt> pointed to
     * by <tt>deviceInfo</tt>
     */
    public static String PaDeviceInfo_getName(long deviceInfo)
    {
        byte[] nameBytes = PaDeviceInfo_getNameBytes(deviceInfo);
        Charset defaultCharset = Charset.defaultCharset();
        String charsetName
            = (defaultCharset == null) ? "UTF-8" : defaultCharset.name();

        try
        {
            return new String(nameBytes, charsetName);
        }
        catch (UnsupportedEncodingException ueex)
        {
            return new String(nameBytes);
        }
    }

    /**
     * Returns defaultHighInputLatency for the device.
     * @param deviceInfo device info pointer.
     * @return defaultHighInputLatency for the device.
     */
    public static native double PaDeviceInfo_getDefaultHighInputLatency(
            long deviceInfo);

    /**
     * Returns defaultHighOutputLatency for the device.
     * @param deviceInfo device info pointer.
     * @return defaultHighOutputLatency for the device.
     */
    public static native double PaDeviceInfo_getDefaultHighOutputLatency(
            long deviceInfo);

    /**
     * Returns defaultLowInputLatency for the device.
     * @param deviceInfo device info pointer.
     * @return defaultLowInputLatency for the device.
     */
    public static native double PaDeviceInfo_getDefaultLowInputLatency(
            long deviceInfo);

    /**
     * Returns defaultLowOutputLatency for the device.
     * @param deviceInfo device info pointer.
     * @return defaultLowOutputLatency for the device.
     */
    public static native double PaDeviceInfo_getDefaultLowOutputLatency(
            long deviceInfo);

    /**
     * The default samplerate for the deviec.
     * @param deviceInfo device info pointer.
     * @return the default sameple rate for the device.
     */
    public static native double PaDeviceInfo_getDefaultSampleRate(
            long deviceInfo);

    /**
     * The host api of the device.
     * @param deviceInfo device info pointer.
     * @return The host api of the device.
     */
    public static native int PaDeviceInfo_getHostApi(long deviceInfo);

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
     * Gets the name as a <tt>byte</tt> array of the PortAudio device specified
     * by the pointer to its <tt>PaDeviceInfo</tt> instance.
     *
     * @param deviceInfo the pointer to the <tt>PaDeviceInfo</tt> instance to
     * get the name of
     * @return the name as a <tt>byte</tt> array of the PortAudio device
     * specified by the <tt>PaDeviceInfo</tt> instance pointed to by
     * <tt>deviceInfo</tt>
     */
    private static native byte[] PaDeviceInfo_getNameBytes(long deviceInfo);

    /**
     * The default input device for this host API.
     * @param hostApiInfo pointer to host api info structure.
     * @return The default input device for this host API.
     */
    public static native int PaHostApiInfo_getDefaultInputDevice(
            long hostApiInfo);

    /**
     * The default output device for this host API.
     * @param hostApiInfo pointer to host api info structure.
     * @return The default output device for this host API.
     */
    public static native int PaHostApiInfo_getDefaultOutputDevice(
            long hostApiInfo);

    /**
     * The number of devices belonging to this host API.
     * @param hostApiInfo pointer to host api info structure.
     * @return The number of devices belonging to this host API.
     */
    public static native int PaHostApiInfo_getDeviceCount(long hostApiInfo);

    /**
     * Gets the human-readable name of the <tt>PaHostApiInfo</tt> specified by a
     * pointer to it.
     *
     * @param hostApiInfo the pointer to the <tt>PaHostApiInfo</tt> to get the
     * human-readable name of
     * @return the human-readable name of the <tt>PaHostApiInfo</tt> pointed to
     * by <tt>hostApiInfo</tt>
     * @deprecated Presumes that the <tt>name</tt> of <tt>PaHostApiInfo</tt> is
     * encoded in modified UTF-8
     */
    @Deprecated
    public static native String PaHostApiInfo_getName(long hostApiInfo);

    /**
     * The well known unique identifier of this host API.
     * @param hostApiInfo pointer to host api info structure.
     * @return The well known unique identifier of this host API.
     *         Enumerator:
     *              paInDevelopment
     *              paDirectSound
     *              paMME
     *              paASIO
     *              paSoundManager
     *              paCoreAudio
     *              paOSS
     *              paALSA
     *              paAL
     *              paBeOS
     *              paWDMKS
     *              paJACK
     *              paWASAPI
     *              paAudioScienceHPI
     */
    public static native int PaHostApiInfo_getType(long hostApiInfo);

    /**
     * Free StreamParameters resources specified by a pointer to it.
     *
     * @param streamParameters the pointer to the <tt>PaStreamParameters</tt>
     * to free
     */
    public static void PaStreamParameters_free(long streamParameters)
    {
        try
        {
            free(streamParameters);
            return;
        }
        catch (UnsatisfiedLinkError ulerr)
        {
            /*
             * Ignore it because we'll try to fallback to another
             * implementation.
             */
            logger.warn(
                    "The JNI library jnportaudio is out-of-date and needs to be"
                        + " recompiled.",
                    ulerr);
        }
        /*
         * FFmpeg on Windows seems to be compiled with --enable-memalign-hack so
         * av_free(void *) isn't the same as free(void *) and thus is not a
         * suitable/safe fallback.
         */
        if (OSUtils.IS_LINUX || OSUtils.IS_MAC)
        {
            try
            {
                FFmpeg.av_free(streamParameters);
            }
            catch (NoClassDefFoundError ncdferr)
            {
                // We'll warn bellow, don't fail.
            }
            catch (UnsatisfiedLinkError ulerr)
            {
                // We'll warn bellow, don't fail.
            }
            return;
        }
        logger.warn("Leaking a PaStreamParameters instance.");
    }

    /**
     * Creates parameters used for opening streams.
     * @param deviceIndex the device.
     * @param channelCount the channels to be used.
     * @param sampleFormat the sample format.
     * @param suggestedLatency the suggested latency in milliseconds:
     *          LATENCY_UNSPECIFIED -
     *              use default(default high input/output latency)
     *          LATENCY_HIGH - use default high input/output latency
     *          LATENCY_LOW - use default low input/output latency
     *          ... - any other value in milliseconds (e.g. 0.1 is acceptable)
     * @return pointer to the params used for Pa_OpenStream.
     */
    public static native long PaStreamParameters_new(
            int deviceIndex,
            int channelCount,
            long sampleFormat,
            double suggestedLatency);

    /**
     * Sets the indicator which determines whether a specific (input) PortAudio
     * stream is to have denoise performed on the audio data it provides.
     *
     * @param stream the (input) PortAudio stream for which denoise is to be
     * enabled or disabled
     * @param denoise <tt>true</tt> if denoise is to be performed on the audio
     * data provided by <tt>stream</tt>; otherwise, <tt>false</tt>
     */
    public static native void setDenoise(long stream, boolean denoise);

    /**
     * Sets the number of milliseconds of echo to be canceled in the audio data
     * provided by a specific (input) PortAudio stream.
     *
     * @param stream the (input) PortAudio stream for which the number of
     * milliseconds of echo to be canceled is to be set
     * @param echoFilterLengthInMillis the number of milliseconds of echo to be
     * canceled in the audio data provided by <tt>stream</tt>
     */
    public static native void setEchoFilterLengthInMillis(
            long stream,
            long echoFilterLengthInMillis);

    /**
     * Updates available device lists in PortAudio.
     */
    private static native void updateAvailableDeviceList();

    /**
     * Implements a (legacy) callback which gets called by the native PortAudio
     * counterpart to notify the Java counterpart that the list of PortAudio
     * devices has changed.
     */
    public static void deviceChanged()
    {
        devicesChangedCallback();
    }

    /**
     * Implements a callback which gets called by the native PortAudio
     * counterpart to notify the Java counterpart that the list of PortAudio
     * devices has changed.
     */
    public static void devicesChangedCallback()
    {
        Runnable devicesChangedCallback = PortAudio.devicesChangedCallback;

        if (devicesChangedCallback != null)
            devicesChangedCallback.run();
    }

    public static void setDevicesChangedCallback(
            Runnable devicesChangedCallback)
    {
        PortAudio.devicesChangedCallback = devicesChangedCallback;
    }

    /**
     * Prevents the initialization of <tt>PortAudio</tt> instances.
     */
    private PortAudio()
    {
    }
}
