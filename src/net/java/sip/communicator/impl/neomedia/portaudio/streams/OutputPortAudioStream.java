/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.portaudio.streams;

import net.java.sip.communicator.impl.neomedia.portaudio.*;

/**
 * The output stream that opens and writes to the PortAudio stream.
 *
 * @author Damian Minkov
 * @author Lubomir Marinov
 */
public class OutputPortAudioStream
{
    /**
     * The device index we are currently using.
     */
    private final int deviceIndex;

    /**
     * The sample rate for the current stream.
     */
    private final double sampleRate;
    /**
     * The number of channel for the current stream.
     */
    private final int channels;
    /**
     * The sample format for the current stream.
     */
    private final long sampleFormat;

    /**
     * The frame size we use.
     */
    private final int frameSize;

    /**
     * The number of frames to write to the native PortAudioStream represented
     * by this instance with a single invocation.
     */
    private final int framesPerBuffer;

    /**
     * The number of bytes to write to a native PortAudio stream with a single
     * invocation. Based on {@link #framesPerBuffer} and {@link #frameSize}.
     */
    private final int bytesPerBuffer;

    /**
     * The stream pointer we are using or 0 if stopped and not initialized.
     */
    private long stream;

    /**
     * Whether this stream is started.
     */
    private boolean started = false;

    /**
     * The audio samples left unwritten by a previous call to
     * {@link #write(byte[], int, int)}. As {@link #bytesPerBuffer} number of
     * bytes are always written, the number of the unwritten audio samples is
     * always less than that.
     */
    private byte[] bufferLeft = null;

    /**
     * The number of bytes in {@link #bufferLeft} representing unwritten audio
     * samples.
     */
    private int bufferLeftLength = 0;

    /**
     * We use this object to sync input stream reads with this output stream
     * closes, if this output stream is connected to input stream(when using
     * echo cancellation). Cause input stream uses output stream while reading
     * and must not use it while this stream is closing.
     */
    private Object closeSyncObject = new Object();

    /**
     * In process of stopping.
     */
    private boolean stopping = false;

    /**
     * Creates output stream.
     * @param deviceIndex the index of the device to use.
     * @param sampleRate the sample rate.
     * @param channels the channels to serve.
     * @throws PortAudioException if stream fails to open.
     */
    public OutputPortAudioStream(
        int deviceIndex, double sampleRate, int channels)
        throws PortAudioException
    {
        this(deviceIndex, sampleRate, channels, PortAudio.SAMPLE_FORMAT_INT16);
    }

    /**
     * Creates output stream.
     * @param deviceIndex the index of the device to use.
     * @param sampleRate the sample rate.
     * @param channels the channels to serve.
     * @param sampleFormat the sample format to use.
     * @throws PortAudioException if stream fails to open.
     */
    public OutputPortAudioStream(
            int deviceIndex, double sampleRate, int channels, long sampleFormat)
        throws PortAudioException
    {
        this.deviceIndex = deviceIndex;
        this.sampleRate = sampleRate;
        this.channels = channels;
        this.sampleFormat = sampleFormat;

        frameSize = PortAudio.Pa_GetSampleSize(sampleFormat)*channels;
        framesPerBuffer = PortAudioManager.getInstance().getFramesPerBuffer();
        bytesPerBuffer = frameSize * framesPerBuffer;

        stream = createStream();
    }

    /**
     * Closes this <tt>OutputPortAudioStream</tt> and prepares it to be garbage
     * collected.
     *
     * @throws PortAudioException if anything wrong happens while closing this
     * <tt>OutputPortAudioStream</tt>
     */
    public synchronized void close()
        throws PortAudioException
    {
        stop();

        if(stream != 0)
        {
            // stop
            synchronized(closeSyncObject)
            {
                PortAudio.Pa_CloseStream(stream);
                stream = 0;
            }

            PortAudioManager.getInstance().closedOutputPortAudioStream(this);
        }
    }

    /**
     * Creates the PortAudio stream.
     *
     * @return the pointer to the native PortAudio stream
     * @throws PortAudioException if stream fails to open.
     */
    private long createStream()
        throws PortAudioException
    {
        long parameters = PortAudio.PaStreamParameters_new(
            getDeviceIndex(),
            channels,
            sampleFormat,
            PortAudioManager.getSuggestedLatency());

        return PortAudio.Pa_OpenStream(
            0,
            parameters,
            sampleRate,
            PortAudio.FRAMES_PER_BUFFER_UNSPECIFIED,
            PortAudio.STREAM_FLAGS_CLIP_OFF
                    | PortAudio.STREAM_FLAGS_DITHER_OFF,
            null);
    }
    
    /**
     * Writes a specific <tt>byte</tt> buffer of audio samples into the native
     * PortAudio stream represented by this instance.
     *<p>
     * Splits the specified buffer and performs multiple writes with
     * {@link PortAudioManager#getFramesPerBuffer()} number of frames at a time.
     * If any bytes from the specified buffer remain unwritten, they are
     * retained for the next write to be prepended to its buffer.
     * </p>
     *
     * @param buffer the <tt>byte</tt> buffer to the written into the native
     * PortAudio stream represented by this instance
     * @param offset the offset in <tt>buffer</tt> at which the audio samples to
     * be written begin
     * @param length the length of the audio samples in <tt>buffer</tt> to be
     * written
     * @throws PortAudioException if anything goes wrong while writing
     */
    public synchronized void write(byte[] buffer, int offset, int length)
        throws PortAudioException
    {
        if((stream == 0) || !started || stopping)
            return;

        /*
         * If there are audio samples left unwritten from a previous write,
         * prepend them to the specified buffer. If it's possible to write them
         * now, do it.
         */
        if ((bufferLeft != null) && (bufferLeftLength > 0))
        {
            int numberOfBytesInBufferLeftToBytesPerBuffer
                = bytesPerBuffer - bufferLeftLength;
            int numberOfBytesToCopyToBufferLeft
                = (numberOfBytesInBufferLeftToBytesPerBuffer < length)
                    ? numberOfBytesInBufferLeftToBytesPerBuffer
                    : length;

            System
                .arraycopy(
                    buffer,
                    offset,
                    bufferLeft,
                    bufferLeftLength,
                    numberOfBytesToCopyToBufferLeft);
            offset += numberOfBytesToCopyToBufferLeft;
            length -= numberOfBytesToCopyToBufferLeft;
            bufferLeftLength += numberOfBytesToCopyToBufferLeft;

            if (bufferLeftLength == bytesPerBuffer)
            {
                PortAudio.Pa_WriteStream(stream, bufferLeft, framesPerBuffer);
                bufferLeftLength = 0;
            }
        }

        // Write the audio samples from the specified buffer.
        int numberOfWrites = length / bytesPerBuffer;

        if (numberOfWrites > 0)
        {
            PortAudio
                .Pa_WriteStream(
                    stream,
                    buffer,
                    offset,
                    framesPerBuffer,
                    numberOfWrites);

            int bytesWritten = numberOfWrites * bytesPerBuffer;

            offset += bytesWritten;
            length -= bytesWritten;
        }

        // If anything was left unwritten, remember it for next time.
        if (length > 0)
        {
            if (bufferLeft == null)
            {
                bufferLeft = new byte[bytesPerBuffer];
            }
            System.arraycopy(buffer, offset, bufferLeft, 0, length);
            bufferLeftLength = length;
        }
    }

    /**
     * Starts the stream operation
     * @throws PortAudioException
     */
    public synchronized void start()
        throws PortAudioException
    {
        if (!started && (stream != 0))
        {
            // start
            PortAudio.Pa_StartStream(stream);
            started = true;
        }
    }

    /**
     * Stops the stream operation.
     * @throws PortAudioException
     */
    public void stop()
        throws PortAudioException
    {
        stopping = true;

        synchronized(this)
        {
            if (started && (stream != 0))
            {
                started = false;
                PortAudio.Pa_StopStream(stream);

                bufferLeft = null;
                stopping = false;
            }
        }
    }

    /**
     * The index of the device that we use.
     * @return the deviceIndex
     */
    public int getDeviceIndex()
    {
        return deviceIndex;
    }

    /**
     * The pointer of the PortAudio stream.
     * @return the stream pointer.
     */
    public long getStream()
    {
        return stream;
    }

    /**
     * Return the object we have used to synchronize Pa_CloseStream.
     * @return the closeSyncObject the sync object.
     */
    Object getCloseSyncObject()
    {
        return closeSyncObject;
    }
}
