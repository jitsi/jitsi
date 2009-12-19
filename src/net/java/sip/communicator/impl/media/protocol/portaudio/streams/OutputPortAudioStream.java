/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.media.protocol.portaudio.streams;

import net.java.sip.communicator.impl.media.protocol.portaudio.*;

/**
 * The output stream that opens and writes to the PortAudio stream.
 *
 * @author Damian Minkov
 */
public class OutputPortAudioStream
{
    private int deviceIndex = -1;
    private long stream = 0;
    private byte[] bufferLeft = null;
    private int frameSize;
    private boolean started = false;

    private double sampleRate;
    private int channels;
    private long sampleFormat;

    /**
     * Creates output stream.
     * @param deviceIndex the index of the device to use.
     * @param sampleRate the samepl rate.
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
     * @param sampleRate the samepl rate.
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

        initStream();

        frameSize = PortAudio.Pa_GetSampleSize(sampleFormat)*channels;
    }

    /**
     * Creates the PortAudio stream.
     * @throws PortAudioException if stream fails to open.
     */
    private void initStream()
        throws PortAudioException
    {
        long parameters = PortAudio.PaStreamParameters_new(
            getDeviceIndex(),channels,
            sampleFormat,
            PortAudioManager.getSuggestedLatency());

        stream = PortAudio.Pa_OpenStream(
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
        if(!started)
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
                    getStream(),tmp, tmp.length/frameSize);
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
                    getStream(),tmp, tmp.length/frameSize);
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
                    getStream(),buffer, buffer.length/frameSize);
        }
        
    }

    /**
     * Starts the stream operation
     * @throws PortAudioException
     */
    public synchronized void start()
        throws PortAudioException
    {
        if(started)
            return;

        if(getStream() == 0)
            initStream();

        // start
        PortAudio.Pa_StartStream(getStream());
        started = true;
    }

    /**
     * Stops the stream operation.
     * @throws PortAudioException
     */
    public void stop()
        throws PortAudioException
    {
        if(!started)
            return;

        // stop
        PortAudio.Pa_CloseStream(getStream());

        synchronized(this)
        {
            PortAudioManager.getInstance().stoppedOutputPortAudioStream(this);
            started = false;
            stream = 0;
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
}
