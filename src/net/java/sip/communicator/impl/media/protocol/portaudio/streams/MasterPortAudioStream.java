/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.media.protocol.portaudio.streams;

import java.util.*;
import net.java.sip.communicator.impl.media.protocol.portaudio.*;

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

    private int numberOfStarts = 0;

    double sampleRate;
    int channels;

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
     * Starts the stream operation if not already started
     * and stores the slave inputstream
     * that starts us.
     * @throws PortAudioException
     */
    synchronized void start(InputPortAudioStream slave)
        throws PortAudioException
    {
        if(numberOfStarts == 0)
        {
            if(stream == 0)
                initStream();

            // start
            PortAudio.Pa_StartStream(stream);
            started = true;
        }

        slaves.add(slave);
        numberOfStarts++;
    }

    /**
     * Stops the PortAudio stream if requested by the last stream that uses us.
     * @throws PortAudioException
     */
    synchronized void stop(InputPortAudioStream slave)
        throws PortAudioException
    {
        if(!started)
            return;

        slaves.remove(slave);
        numberOfStarts--;

        if(numberOfStarts == 0)
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
    public byte[] read()
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
        long outStream = 0;

        if(out != null)
            outStream = out.getStream();

        PortAudio.setEchoCancelParams(
            stream,
            outStream,
            deNoiseEnabled,
            echoCancelEnabled, frameSize, filterLength);
    }
}
