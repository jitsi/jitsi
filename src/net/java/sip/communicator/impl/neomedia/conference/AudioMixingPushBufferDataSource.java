/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.conference;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import javax.media.*;
import javax.media.control.*;
import javax.media.protocol.*;

import net.java.sip.communicator.impl.neomedia.control.*;
import net.java.sip.communicator.impl.neomedia.protocol.*;
import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.util.*;

/**
 * Represents a <tt>PushBufferDataSource</tt> which provides a single
 * <tt>PushBufferStream</tt> containing the result of the audio mixing of
 * <tt>DataSource</tt>s.
 * 
 * @author Lubomir Marinov
 */
public class AudioMixingPushBufferDataSource
    extends PushBufferDataSource
    implements CaptureDevice,
               MuteDataSource,
               InbandDTMFDataSource
{

    /**
     * The <tt>Logger</tt> used by the <tt>AudioMixingPushBufferDataSource</tt>
     * class and its instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(AudioMixingPushBufferDataSource.class);

    /**
     * The <tt>AudioMixer</tt> performing the audio mixing, managing the input
     * <tt>DataSource</tt>s and pushing the data of this output
     * <tt>PushBufferDataSource</tt>.
     */
    private final AudioMixer audioMixer;

    /**
     * The indicator which determines whether this <tt>DataSource</tt> is
     * connected.
     */
    private boolean connected;

    /**
     * The one and only <tt>PushBufferStream</tt> this
     * <tt>PushBufferDataSource</tt> provides to its clients and containing the
     * result of the audio mixing performed by <tt>audioMixer</tt>.
     */
    private AudioMixingPushBufferStream outputStream;

    /**
     * The indicator which determines whether this <tt>DataSource</tt> is
     * started.
     */
    private boolean started;

    /**
     * The indicator which determines whether this <tt>DataSource</tt> is set
     * to transmit "silence" instead of the actual media.
     */
    private boolean mute = false;

    /**
     * The tones to send via inband DTMF, if not empty.
     */
    private LinkedList<DTMFInbandTone> tones = new LinkedList<DTMFInbandTone>();

    /**
     * Initializes a new <tt>AudioMixingPushBufferDataSource</tt> instance which
     * gives access to the result of the audio mixing performed by a specific
     * <tt>AudioMixer</tt>.
     * 
     * @param audioMixer the <tt>AudioMixer</tt> performing audio mixing,
     * managing the input <tt>DataSource</tt>s and pushing the data of the new
     * output <tt>PushBufferDataSource</tt>
     */
    public AudioMixingPushBufferDataSource(AudioMixer audioMixer)
    {
        this.audioMixer = audioMixer;
    }

    /**
     * Adds a new input <tt>DataSource</tt> to be mixed by the associated
     * <tt>AudioMixer</tt> of this instance and to not have its audio
     * contributions included in the mixing output represented by this
     * <tt>DataSource</tt>.
     * 
     * @param inputDataSource a <tt>DataSource</tt> to be added for mixing to
     * the <tt>AudioMixer</tt> associate with this instance and to not have its
     * audio contributions included in the mixing output represented by this
     * <tt>DataSource</tt>
     */
    public void addInputDataSource(DataSource inputDataSource)
    {
        audioMixer.addInputDataSource(inputDataSource, this);
    }

    /**
     * Implements {@link DataSource#connect()}. Lets the <tt>AudioMixer</tt>
     * know that one of its output <tt>PushBufferDataSources</tt> has been
     * connected and marks this <tt>DataSource</tt> as connected.
     *
     * @throws IOException if the <tt>AudioMixer</tt> fails to connect
     */
    public synchronized void connect()
        throws IOException
    {
        if (!connected)
        {
            audioMixer.connect();
            connected = true;
        }
    }

    /**
     * Implements {@link DataSource#disconnect()}. Marks this
     * <tt>DataSource</tt> as disconnected and notifies the <tt>AudioMixer</tt>
     * that one of its output <tt>PushBufferDataSources</tt> has been
     * disconnected.
     */
    public synchronized void disconnect()
    {
        try
        {
            stop();
        }
        catch (IOException ioex)
        {
            throw new UndeclaredThrowableException(ioex);
        }

        if (connected)
        {
            outputStream = null;
            connected = false;

            audioMixer.disconnect();
        }
    }

    /**
     * Gets the <tt>BufferControl</tt> available for this <tt>DataSource</tt>.
     * Delegates to the <tt>AudioMixer</tt> because this instance is just a
     * facet to it.
     *
     * @return the <tt>BufferControl</tt> available for this <tt>DataSource</tt>
     */
    private BufferControl getBufferControl()
    {
        return audioMixer.getBufferControl();
    }

    /**
     * Implements {@link CaptureDevice#getCaptureDeviceInfo()}. Delegates to the
     * associated <tt>AudioMixer</tt> because it knows which
     * <tt>CaptureDevice</tt> is being wrapped.
     *
     * @return the <tt>CaptureDeviceInfo</tt> of the <tt>CaptureDevice</tt> of
     * the <tt>AudioMixer</tt>
     */
    public CaptureDeviceInfo getCaptureDeviceInfo()
    {
        return audioMixer.getCaptureDeviceInfo();
    }

    /**
     * Implements {@link DataSource#getContentType()}. Delegates to the
     * associated <tt>AudioMixer</tt> because it manages the inputs and knows
     * their characteristics.
     *
     * @return a <tt>String</tt> value which represents the type of the content
     * being made available by this <tt>DataSource</tt> i.e. the associated
     * <tt>AudioMixer</tt>
     */
    public String getContentType()
    {
        return audioMixer.getContentType();
    }

    /**
     * Implements {@link DataSource#getControl(String)}.
     *
     * @param controlType a <tt>String</tt> value which names the type of the
     * control of this instance to be retrieved
     * @return an <tt>Object</tt> which represents the control of this instance
     * with the specified type if such a control is available; otherwise,
     * <tt>null</tt>
     */
    public Object getControl(String controlType)
    {
        return AbstractControls.getControl(this, controlType);
    }

    /**
     * Implements {@link DataSource#getControls()}. Gets an array of
     * <tt>Object</tt>s which represent the controls available for this
     * <tt>DataSource</tt>.
     *
     * @return an array of <tt>Object</tt>s which represent the controls
     * available for this <tt>DataSource</tt>
     */
    public Object[] getControls()
    {
        BufferControl bufferControl = getBufferControl();
        FormatControl[] formatControls = getFormatControls();

        if (bufferControl == null)
            return formatControls;
        else if ((formatControls == null) || (formatControls.length < 1))
            return new Object[] { bufferControl };
        else
        {
            Object[] controls = new Object[1 + formatControls.length];

            controls[0] = bufferControl;
            System
                .arraycopy(
                    formatControls,
                    0,
                    controls,
                    1,
                    formatControls.length);
            return controls;
        }
    }

    /**
     * Implements {@link DataSource#getDuration()}. Delegates to the associated
     * <tt>AudioMixer</tt> because it manages the inputs and knows their
     * characteristics.
     *
     * @return a <tt>Time</tt> value which represents the duration of the media
     * being made available through this <tt>DataSource</tt>
     */
    public Time getDuration()
    {
        return audioMixer.getDuration();
    }

    /**
     * Implements {@link CaptureDevice#getFormatControls()}. Delegates to the
     * associated <tt>AudioMixer</tt> because it knows which
     * <tt>CaptureDevice</tt> is being wrapped.
     *
     * @return an array of <tt>FormatControl</tt>s of the <tt>CaptureDevice</tt>
     * of the associated <tt>AudioMixer</tt>
     */
    public FormatControl[] getFormatControls()
    {
        return audioMixer.getFormatControls();
    }

    /**
     * Implements {@link PushBufferDataSource#getStreams()}. Gets a
     * <tt>PushBufferStream</tt> which reads data from the associated
     * <tt>AudioMixer</tt> and mixes its inputs.
     *
     * @return an array with a single <tt>PushBufferStream</tt> which reads data
     * from the associated <tt>AudioMixer</tt> and mixes its inputs if this
     * <tt>DataSource</tt> is connected; otherwise, an empty array
     */
    public synchronized PushBufferStream[] getStreams()
    {
        if (connected && (outputStream == null))
        {
            AudioMixerPushBufferStream audioMixerOutputStream
                = audioMixer.getOutputStream();

            if (audioMixerOutputStream != null)
            {
                outputStream
                    = new AudioMixingPushBufferStream(
                            audioMixerOutputStream,
                            this);
                if (started)
                    try
                    {
                        outputStream.start();
                    }
                    catch (IOException ioex)
                    {
                        logger
                            .error(
                                "Failed to start "
                                    + outputStream.getClass().getSimpleName()
                                    + " with hashCode "
                                    + outputStream.hashCode(),
                                ioex);
                    }
            }
        }
        return
            (outputStream == null)
                ? new PushBufferStream[0]
                : new PushBufferStream[] { outputStream };
    }

    /**
     * Implements {@link DataSource#start()}. Starts the output
     * <tt>PushBufferStream</tt> of this <tt>DataSource</tt> (if it exists) and
     * notifies the <tt>AudioMixer</tt> that one of its output
     * <tt>PushBufferDataSources</tt> has been started.
     *
     * @throws IOException if anything wrong happens while starting the output
     * <tt>PushBufferStream</tt> of this <tt>DataSource</tt>
     */
    public synchronized void start()
        throws IOException
    {
        if (!started)
        {
            started = true;
            if (outputStream != null)
                outputStream.start();
        }
    }

    /**
     * Implements {@link DataSource#stop()}. Notifies the <tt>AudioMixer</tt>
     * that one of its output <tt>PushBufferDataSources</tt> has been stopped
     * and stops the output <tt>PushBufferStream</tt> of this
     * <tt>DataSource</tt> (if it exists).
     *
     * @throws IOException if anything wrong happens while stopping the output
     * <tt>PushBufferStream</tt> of this <tt>DataSource</tt>
     */
    public synchronized void stop()
        throws IOException
    {
        if (started)
        {
            started = false;
            if (outputStream != null)
                outputStream.stop();
        }
    }

    /**
     * Determines whether this <tt>DataSource</tt> is mute.
     *
     * @return <tt>true</tt> if this <tt>DataSource</tt> is mute; otherwise,
     *         <tt>false</tt>
     */
    public boolean isMute()
    {
        return this.mute;
    }

    /**
     * Sets the mute state of this <tt>DataSource</tt>.
     *
     * @param mute <tt>true</tt> to mute this <tt>DataSource</tt>; otherwise,
     *            <tt>false</tt>
     */
    public void setMute(boolean mute)
    {
        if (this.mute != mute)
        {
            this.mute = mute;
        }
    }

    /**
     * Adds a new inband DTMF tone to send.
     *
     * @param tone the DTMF tone to send.
     */
    public void addDTMF(DTMFInbandTone tone)
    {
        this.tones.add(tone);
    }

    /**
     * Determines whether this <tt>DataSource</tt> sends a DTMF tone.
     *
     * @return <tt>true</tt> if this <tt>DataSource</tt> is sending a DTMF tone;
     * otherwise, <tt>false</tt>.
     */
    public boolean isSendingDTMF()
    {
        return !this.tones.isEmpty();
    }

    /**
     * Gets the next inband DTMF tone signal.
     *
     * @param samplingFrequency The sampling frequency (codec clock rate) in Hz
     * of the stream which will encapsulate this signal.
     * @param sampleSizeInBits The size of each sample (8 for a byte, 16 for a
     * short and 32 for an int)
     *
     * @return The data array containing the DTMF signal.
     */
    public int[] getNextToneSignal(
            double samplingFrequency,
            int sampleSizeInBits)
    {
        DTMFInbandTone tone = tones.poll();
        return tone.getAudioSamples(samplingFrequency, sampleSizeInBits);
    }
}
