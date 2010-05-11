/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.portaudio.streams;

import java.util.*;

import javax.media.*;

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
    /**
     * The device index we are currently using.
     */
    private int deviceIndex = -1;

    /**
     * The number of bytes to read from a native PortAudio stream in a single
     * invocation. Based on {@link #framesPerBuffer} and {@link #frameSize}.
     */
    private final int bytesPerBuffer;

    /**
     * The number of channel for the current stream.
     */
    private int channels;

    /**
     * This is the output stream which is connected to the current input stream.
     * When using echo cancellation its the actual output stream, otherwise its
     * just a non null object. Need to synch closing stream in order to avoid
     * concurrency: using the output stream(in Pa_ReadStream) while closing it.
     */
    private Object connectedToStreamSync = new Object();

    /**
     * The frame size we use.
     */
    private final int frameSize;

    /**
     * The number of frames to read from a native PortAudio stream in a single
     * invocation.
     */
    private final int framesPerBuffer;

    /**
     * The sample rate for the current stream.
     */
    private double sampleRate;

    /**
     * The <tt>InputPortAudioStream</tt>s which read audio from this
     * <tt>MasterPortAudioStream</tt>s.
     */
    private final List<InputPortAudioStream> slaves
        = new ArrayList<InputPortAudioStream>();

    /**
     * Whether this stream is started.
     */
    private boolean started = false;

    /**
     * The stream pointer we are using or 0 if stopped and not initialized.
     */
    private long stream = 0;

    /**
     * Whether a read is active.
     */
    private boolean readActive = false;
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
        framesPerBuffer = PortAudioManager.getInstance().getFramesPerBuffer();
        bytesPerBuffer = frameSize * framesPerBuffer;
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

    private Object readSync = new Object();
    
    /**
     * Reads audio data from this <tt>MasterPortAudioStream</tt> into a specific
     * <tt>Buffer</tt> blocking until audio data is indeed available.
     *
     * @param buffer the <tt>Buffer</tt> into which the audio data read from
     * this <tt>MasterPortAudioStream</tt> is to be returned
     * @throws PortAudioException if an error occurs while reading
     */    
    public boolean read(Buffer buffer)
        throws PortAudioException
    {
        synchronized (readSync) {
            if (readActive)
            {
                return false;
            }
            readActive = true;
            if (!started)
            {
                buffer.setLength(0);
                return true;
            }
        }

        /*
         * If buffer conatins a data area then check if the type and 
         * length fits. If yes then use it, otherwise allocate a new
         * area and set it in buffer.
         */
        Object data = buffer.getData();
        byte[] bufferData;
        if (data instanceof byte[] && ((byte[])data).length >= bytesPerBuffer) {
            bufferData = (byte[])data;
        }
        else
        {
            bufferData = new byte[bytesPerBuffer];
            buffer.setData(bufferData);
        }
        
        synchronized (connectedToStreamSync)
        {
            PortAudio.Pa_ReadStream(stream, bufferData, framesPerBuffer);
        }

        long bufferTimeStamp = System.nanoTime();

        buffer.setFlags(Buffer.FLAG_SYSTEM_TIME);
        buffer.setLength(bytesPerBuffer);
        buffer.setOffset(0);
        buffer.setTimeStamp(bufferTimeStamp);

        int slaveCount = slaves.size();

        for(int slaveIndex = 0; slaveIndex < slaveCount; slaveIndex++)
        {
            slaves
                .get(slaveIndex)
                    .setBuffer(bufferData, bytesPerBuffer, bufferTimeStamp);
        }
        synchronized(readSync) {
            readActive = false;
            readSync.notify();
        }
        return true;
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
        if(out != null)
            this.connectedToStreamSync = out.getCloseSyncObject();

        long outStream = (out == null) ? 0 : out.getStream();

        PortAudio.setEchoCancelParams(
            stream,
            outStream,
            deNoiseEnabled,
            echoCancelEnabled, frameSize, filterLength);
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
            {
                initStream();
                // if still not initted return
                if(stream == 0)
                    return;
            }

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
            synchronized (readSync) {
                while (readActive) 
                {
                    try {
                        readSync.wait();
                    } catch (InterruptedException e) {
                        continue;
                    }
                }
                // stop
                PortAudio.Pa_CloseStream(stream);
                stream = 0;
                started = false;
                PortAudioManager.getInstance().stoppedInputPortAudioStream(this);
            }
        }
    }
}
