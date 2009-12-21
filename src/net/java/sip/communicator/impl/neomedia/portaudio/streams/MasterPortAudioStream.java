/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.portaudio.streams;

import java.util.*;

import net.java.sip.communicator.impl.neomedia.portaudio.*;

/**
 * The master audio stream which opens the PortAudio stream and reads from it.
 * We can have multiple slave input streams that are connected to this master
 * stream.
 *
 * @author Damian Minkov
 * @author Lubomir Marinov
 */
public class MasterPortAudioStream
{
    private int deviceIndex = -1;

    private long stream = 0;

    private boolean started = false;

    private int frameSize;

    double sampleRate;

    int channels;

    /**
     * The <tt>InputPortAudioStream</tt>s which read audio from this
     * <tt>MasterPortAudioStream</tt>s.
     */
    private final List<InputPortAudioStream> slaves
        = new ArrayList<InputPortAudioStream>();

    /**
     * Creates new stream.
     * @param deviceIndex the device to use.
     * @param channels the channels to serve.
     * @param sampleRate the sample rate.
     * @throws PortAudioException if stream cannot be opened.
     */
    public MasterPortAudioStream(
        int deviceIndex, double sampleRate, int channels)
        throws PortAudioException
    {
        this.deviceIndex = deviceIndex;
        this.sampleRate = sampleRate;
        this.channels = channels;

        initStream();

        frameSize
            = PortAudio.Pa_GetSampleSize(PortAudio.SAMPLE_FORMAT_INT16)
                * channels;
    }

    /**
     * Create the stream.
     * @throws PortAudioException if stream cannot be opened.
     */
    private void initStream()
        throws PortAudioException
    {
        long parameters = PortAudio.PaStreamParameters_new(
            deviceIndex,
            channels,
            PortAudio.SAMPLE_FORMAT_INT16,
            PortAudioManager.getSuggestedLatency());

        stream = PortAudio.Pa_OpenStream(
            parameters,
            0, // no output parameters
            sampleRate,
            PortAudio.FRAMES_PER_BUFFER_UNSPECIFIED,
            PortAudio.STREAM_FLAGS_CLIP_OFF
                    | PortAudio.STREAM_FLAGS_DITHER_OFF,
            null);
    }

    /**
     * Starts this <tt>MasterPortAudioStream</tt> so that a specific
     * <tt>InputPortAudioStream</tt> can read from it. When the first such
     * <tt>InputPortAudioStream</tt> request the starting of this instance, this
     * instance starts the native PortAudio stream it reads from.
     *
     * @param slave the <tt>InputPortAudioStream</tt> which has been started
     * and wants to read audio from this instance
     * @throws PortAudioException if anything wrong happens while starting the
     * native PortAudio stream this instance is to read from
     */
    synchronized void start(InputPortAudioStream slave)
        throws PortAudioException
    {
        if (slave == null)
            throw new NullPointerException("slave");

        if(slaves.isEmpty())
        {
            if(stream == 0)
                initStream();

            // if still not initted return
            if(stream == 0)
                return;

            // start
            PortAudio.Pa_StartStream(stream);
            started = true;
        }

        slaves.add(slave);
    }

    /**
     * Stops the reading of a specific <tt>InputPortAudioStream</tt> from this
     * <tt>MasterPortAudioStream</tt>. When the last such
     * <tt>InputPortAudioStream</tt> stops reading from this instance, this
     * instance closes the native PortAudio stream it reads from.
     *
     * @param slave the <tt>InputPortAudioStream</tt> which has been stopped and
     * no longer wants to read audio from this instance
     * @throws PortAudioException if anything wrong happens while stopping the
     * native PortAudio stream this instance reads from
     */
    synchronized void stop(InputPortAudioStream slave)
        throws PortAudioException
    {
        if(!started)
            return;

        slaves.remove(slave);

        if(slaves.isEmpty())
        {
            // stop
            PortAudio.Pa_CloseStream(stream);
            stream = 0;
            started = false;
            PortAudioManager.getInstance().stoppedInputPortAudioStream(this);
        }
    }

    /**
     * Returns the index of the device that we use.
     * @return the deviceIndex
     */
    public int getDeviceIndex()
    {
        return deviceIndex;
    }

    /**
     * Returns the pointer to the stream that we use for reading.
     * @return the stream pointer.
     */
    public long getStream()
    {
        return stream;
    }

    /**
     * Block and read a buffer from the stream.
     *
     * @return the bytes that a read from underlying stream.
     * @throws PortAudioException if an error occurs while reading.
     */
    public synchronized byte[] read()
        throws PortAudioException
    {
        if(!started)
            return new byte[0];

        byte[] bytebuff = new byte[PortAudioManager.NUM_SAMPLES*frameSize];
        PortAudio.Pa_ReadStream(
            stream, bytebuff, PortAudioManager.NUM_SAMPLES);

        for(InputPortAudioStream slave : slaves)
            slave.setBuffer(bytebuff);

        return bytebuff;
    }

    /**
     * Sets parameters to the underlying stream.
     * @param out the connected output stream if echo cancel is enabled.
     * @param deNoiseEnabled true if we want to enable noise reduction.
     * @param echoCancelEnabled true to enable echo cancel.
     * @param frameSize Number of samples to process at one time
     *        (should correspond to 10-20 ms).
     * @param filterLength Number of samples of echo to cancel
     *        (should generally correspond to 100-500 ms)
     */
    public void setParams(OutputPortAudioStream out,
        boolean deNoiseEnabled,
        boolean echoCancelEnabled, int frameSize, int filterLength)
    {
        long outStream = (out == null) ? 0 : out.getStream();

        PortAudio.setEchoCancelParams(
            stream,
            outStream,
            deNoiseEnabled,
            echoCancelEnabled, frameSize, filterLength);
    }
}
