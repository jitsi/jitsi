/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.media.protocol.portaudio;

/**
 * @author Lubomir Marinov
 */
public final class PortAudio
{
    static
    {
        System.loadLibrary("jportaudio");
    }

    public static final long FRAMES_PER_BUFFER_UNSPECIFIED = 0;

    public static final long SAMPLE_FORMAT_INT16 = 0x00000008;

    public static final long STREAM_FLAGS_NO_FLAG = 0;

    private static boolean initialized;

    public static synchronized void initialize()
        throws PortAudioException
    {
        if (!initialized)
        {
            Pa_Initialize();
            initialized = true;
        }
    }

    public static native void Pa_CloseStream(long stream)
        throws PortAudioException;

    public static native int Pa_GetDeviceCount()
        throws PortAudioException;

    public static native long Pa_GetDeviceInfo(int deviceIndex);

    private static native void Pa_Initialize()
        throws PortAudioException;

    public static native long Pa_OpenStream(
            long inputParameters,
            long outputParameters,
            double sampleRate,
            long framesPerBuffer,
            long streamFlags,
            PortAudioStreamCallback streamCallback)
        throws PortAudioException;

    public static native void Pa_StartStream(long stream)
        throws PortAudioException;

    public static native void Pa_StopStream(long stream)
        throws PortAudioException;

    public static native void Pa_WriteStream(
            long stream,
            byte[] buffer,
            long frames)
        throws PortAudioException;

    public static native void Pa_ReadStream(
            long stream, byte[] buffer, long frames)
        throws PortAudioException;

    public static native long Pa_GetStreamReadAvailable(long stream);

    public static native long Pa_GetStreamWriteAvailable(long stream);

    public static native int Pa_GetSampleSize(long format);

    public static native boolean Pa_IsFormatSupported(
        long inputParameters,
        long outputParameters,
        double sampleRate);

    public static native int PaDeviceInfo_getMaxInputChannels(long deviceInfo);

    public static native int PaDeviceInfo_getMaxOutputChannels(long deviceInfo);

    public static native String PaDeviceInfo_getName(long deviceInfo);

    public static native double PaDeviceInfo_getDefaultSampleRate(long deviceInfo);

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
