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
     * The stream pointer we are using or 0 if stopped and not initialized.
     */
    private long stream;
    /**
     * Whether this stream is started.
     */
    private boolean started = false;

    /**
     * Buffer left for writing from previous write,
     * as everything is split into parts of PortAudioManager.NUM_SAMPLES,
     * this is what has left.
     */
    private byte[] bufferLeft = null;

    /**
     * We use this object to sync input stream reads with this output stream
     * closes, if this output stream is connected to input stream(when using
     * echo cancellation). Cause input stream uses output stream while reading
     * and must not use it while this stream is closing.
     */
    private Object closeSyncObject = new Object();

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
     * We will split everything into parts of PortAudioManager.NUM_SAMPLES.
     * If something is left we will save it for next write and use it than.
     *
     * @param buffer the current buffer.
     * @throws PortAudioException error while writing to device.
     */
    public synchronized void write(byte[] buffer)
        throws PortAudioException
    {
        if((stream == 0) || !started)
            return;

        int numSamples = PortAudioManager.NUM_SAMPLES*frameSize;

        int currentIx = 0;

        // if there are bytes from previous run
        if(bufferLeft != null && bufferLeft.length > 0)
        {
            if(buffer.length + bufferLeft.length >= numSamples)
            {
                byte[] tmp = new byte[numSamples];
                System.arraycopy(bufferLeft, 0, tmp, 0, bufferLeft.length);
                System.arraycopy(buffer, currentIx, tmp,
                    bufferLeft.length, numSamples - bufferLeft.length);
                currentIx += numSamples - bufferLeft.length;
                bufferLeft = null;
                
                PortAudio.Pa_WriteStream(
                    stream,tmp, tmp.length/frameSize);
            }
            else
            {
                // not enough bytes even with previous left
                // so let store everything
                byte[] tmp = new byte[numSamples];
                System.arraycopy(bufferLeft, 0, tmp, 0, bufferLeft.length);
                System.arraycopy(buffer, currentIx, tmp,
                    bufferLeft.length, numSamples - bufferLeft.length);
                bufferLeft = null;
                return;
            }
        }

        // now use all the current buffer
        if(buffer.length > numSamples)
        {
            while(currentIx <= buffer.length - numSamples)
            {
                byte[] tmp = new byte[numSamples];
                System.arraycopy(buffer, currentIx, tmp, 0, numSamples);

                PortAudio.Pa_WriteStream(
                    stream,tmp, tmp.length/frameSize);
                currentIx += numSamples;
            }

            if(currentIx < buffer.length)
            {
                bufferLeft = new byte[buffer.length - currentIx];
                System.arraycopy(buffer, currentIx, bufferLeft, 0, bufferLeft.length);
            }
        }
        else if(buffer.length < numSamples)
        {
            bufferLeft = buffer;
        }
        else
        {
            PortAudio.Pa_WriteStream(
                    stream,buffer, buffer.length/frameSize);
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
    public synchronized void stop()
        throws PortAudioException
    {
        if (started && (stream != 0))
        {
            started = false;
            PortAudio.Pa_StopStream(stream);

            bufferLeft = null;
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
