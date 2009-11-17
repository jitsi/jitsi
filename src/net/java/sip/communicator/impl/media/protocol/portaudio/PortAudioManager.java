/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.media.protocol.portaudio;

import net.java.sip.communicator.impl.media.protocol.portaudio.streams.*;
import java.util.*;

/**
 * Manages PortAudio stream creation and setting necessary properties when using
 * them.
 *
 * @author Damian Minkov
 */
public class PortAudioManager
{
    /**
     * 20ms in 8kHz is 160 samples.
     */
    public static final int NUM_SAMPLES = 160;

    /**
     * The static instance of portaudio manager.
     */
    private static PortAudioManager instance = null;

    private Hashtable<Integer, MasterPortAudioStream> inputStreams =
        new Hashtable<Integer, MasterPortAudioStream>();

    private ArrayList<OutputPortAudioStream> outputStreams =
        new ArrayList<OutputPortAudioStream>();

    /**
     * default values for the params.
     */
    private boolean enabledEchoCancel = false;
    private boolean enabledDeNoise = true;
    private int frameSize = NUM_SAMPLES;
    private int filterLength = 2048;

    private static double suggestedLatency = PortAudio.LATENCY_UNSEPCIFIED;

    /**
     * Private constructor as we have one static instance of this manager.
     * @throws PortAudioException
     */
    private PortAudioManager()
        throws PortAudioException
    {
        PortAudio.initialize();
    }

    /**
     * Gets the only instance of PortAudioManager, if its not already created
     * will be created.
     * @return the static instance.
     * @throws PortAudioException if portaudio cannot be initialized for
     *          some reason.
     */
    public static PortAudioManager getInstance()
        throws PortAudioException
    {
        if(instance == null)
            instance = new PortAudioManager();
        return instance;
    }

    /**
     * Creates input stream from the device with given index.
     * @param deviceIndex the device index.
     * @param sampleRate the sample rate to use, its the sample rate of the
     *        input stream.
     * @param channels the channels that the stream will serve.
     * @return the stream.
     * @throws PortAudioException if opening of the stream failes.
     */
    public InputPortAudioStream getInputStream(
        int deviceIndex, double sampleRate, int channels)
        throws PortAudioException
    {
        MasterPortAudioStream st = inputStreams.get(deviceIndex);
        if(st == null)
        {
            st = new MasterPortAudioStream(deviceIndex, sampleRate, channels);
            inputStreams.put(deviceIndex, st);

            // if there is a output streams, get the latest one
            // and connect them
            // todo: we must link input to all outputs ???
            if(isEnabledEchoCancel() && outputStreams.size() > 0)
            {
                OutputPortAudioStream out = outputStreams.get(
                        outputStreams.size() - 1);
                st.setParams(out, isEnabledDeNoise(),
                    isEnabledEchoCancel(), getFrameSize(), getFilterLength());
            }
            else
                st.setParams(null, isEnabledDeNoise(),
                    isEnabledEchoCancel(), getFrameSize(), getFilterLength());
        }

        return new InputPortAudioStream(st);
    }

    /**
     * Creates output stream from the device with given index.
     * @param deviceIndex the device index.
     * @param sampleRate the sample rate to use, its the sample rate of the
     *        output stream.
     * @param channels the channels that the stream will serve.
     * @return the stream.
     * @throws PortAudioException if opening of the stream failes.
     */
    public OutputPortAudioStream getOutputStream(
        int deviceIndex, double sampleRate, int channels)
        throws PortAudioException
    {
        OutputPortAudioStream out = 
            new OutputPortAudioStream(deviceIndex, sampleRate, channels);
        outputStreams.add(out);

        // if there are input streams created, get the first one
        // and link it to this output
        // todo: what to do with the others
        if(isEnabledEchoCancel() && inputStreams.size() > 0)
        {
            MasterPortAudioStream st = inputStreams.values().iterator().next();

            st.setParams(out, isEnabledEchoCancel(),
                isEnabledDeNoise(), getFrameSize(), getFilterLength());
        }

        return out;
    }

    /**
     * Output stream is stopped.
     * @param st the stream that is stopped.
     */
    public void stoppedOutputPortAudioStream(OutputPortAudioStream st)
    {
        outputStreams.remove(st);
    }

    /**
     * Input stream is stopped.
     * @param st the input stream that is stopped.
     */
    public void stoppedInputPortAudioStream(MasterPortAudioStream st)
    {
        inputStreams.remove(st.getDeviceIndex());
    }

    /**
     * Creates output stream from the device with given index.
     * @param deviceIndex the device index.
     * @param sampleRate the sample rate to use, its the sample rate of the
     *        output stream.
     * @param channels the channels that the stream will serve.
     * @param sampleFormat the format the will be used by the stream.
     * @return the stream.
     * @throws PortAudioException if opening of the stream failes.
     */
    public OutputPortAudioStream getOutputStream(
        int deviceIndex, double sampleRate, int channels, long sampleFormat)
        throws PortAudioException
    {
        return new OutputPortAudioStream(
            deviceIndex, sampleRate, channels, sampleFormat);
    }

    /**
     * Enables or disables echo cancel.
     * @param enabled should we enable or disable echo cancelation
     * @param frameSize Number of samples to process at one time
     *          (should correspond to 10-20 ms)
     * @param filterLength Number of samples of echo to cancel
     *          (should generally correspond to 100-500 ms)
     */
    public void setEchoCancel(boolean enabled, int frameSize, int filterLength)
    {
        this.enabledEchoCancel = enabled;
        this.frameSize = frameSize;
        this.filterLength = filterLength;
    }

    /**
     * Enables or disables noise suppression.
     * @param enabled should we enable or disable noise suppression.
     */
    public void setDeNoise(boolean enabled)
    {
        this.enabledDeNoise = enabled;
    }

    /**
     * Returns the default values of the latency to be used when
     * openning new streams.
     * @return the latency.
     */
    public static double getSuggestedLatency()
    {
        if(suggestedLatency != PortAudio.LATENCY_UNSEPCIFIED)
            return suggestedLatency;

        String osName = System.getProperty("os.name");
        if (osName.startsWith("Mac"))
            return PortAudio.LATENCY_HIGH;
        else if (osName.startsWith("Linux"))
            return PortAudio.LATENCY_HIGH;
        else if (osName.startsWith("Windows"))
            return 0.1d;
        return PortAudio.LATENCY_UNSEPCIFIED;
    }

    /**
     * Changes the suggested latency.
     * @param aSuggestedLatency the suggestedLatency to set.
     */
    public static void setSuggestedLatency(double aSuggestedLatency)
    {
        suggestedLatency = aSuggestedLatency;
    }

    /**
     * Is echo cancel enabled.
     * @return true if echo cancel is enabled, false otherwise.
     */
    public boolean isEnabledEchoCancel()
    {
        return enabledEchoCancel;
    }

    /**
     * Is noise reduction enabled.
     * @return true if noise reduction is enabled, false otherwise.
     */
    public boolean isEnabledDeNoise()
    {
        return enabledDeNoise;
    }

    /**
     * Number of samples to process at one time (should correspond to 10-20 ms).
     * @return the frameSize.
     */
    public int getFrameSize()
    {
        return frameSize;
    }

    /**
     * Number of samples of echo to cancel
     * (should generally correspond to 100-500 ms)
     * @return the filterLength.
     */
    public int getFilterLength()
    {
        return filterLength;
    }
}
