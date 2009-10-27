/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.conference;

import java.io.*;
import java.lang.reflect.*;

import javax.media.*;
import javax.media.control.*;
import javax.media.protocol.*;

/**
 * Represents a <tt>PushBufferDataSource</tt> which provides a single
 * <tt>PushBufferStream</tt> containing the result of the audio mixing of
 * <tt>DataSource</tt>s.
 * 
 * @author Lubomir Marinov
 */
public class AudioMixingPushBufferDataSource
    extends PushBufferDataSource
    implements CaptureDevice
{

    /**
     * The <tt>AudioMixer</tt> performing the audio mixing, managing the
     * input <tt>DataSource</tt>s and pushing the data of this output
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
     * <tt>PushBufferDataSource</tt> provides to its clients and containing
     * the result of the audio mixing performed by <tt>audioMixer</tt>.
     */
    private AudioMixingPushBufferStream outputStream;

    /**
     * The indicator which determines whether this <tt>DataSource</tt> is
     * started.
     */
    private boolean started;

    /**
     * Initializes a new <tt>AudioMixingPushBufferDataSource</tt> instance
     * which gives access to the result of the audio mixing performed by a
     * specific <tt>AudioMixer</tt>.
     * 
     * @param audioMixer the <tt>AudioMixer</tt> performing audio mixing,
     *            managing the input <tt>DataSource</tt>s and pushing the
     *            data of the new output <tt>PushBufferDataSource</tt>
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
     * @param inputDataSource a <tt>DataSource</tt> to be added for mixing
     *            to the <tt>AudioMixer</tt> associate with this instance
     *            and to not have its audio contributions included in the mixing
     *            output represented by this <tt>DataSource</tt>
     */
    public void addInputDataSource(DataSource inputDataSource)
    {
        audioMixer.addInputDataSource(inputDataSource, this);
    }

    /*
     * Implements DataSource#connect(). Lets the AudioMixer know that one of its
     * output PushBufferDataSources has been connected and marks this DataSource
     * as connected.
     */
    public void connect()
        throws IOException
    {
        if (!connected)
        {
            audioMixer.connect();
            connected = true;
        }
    }

    /*
     * Implements DataSource#disconnect(). Marks this DataSource as disconnected
     * and notifies the AudioMixer that one of its output PushBufferDataSources
     * has been disconnected.
     */
    public void disconnect()
    {
        try
        {
            stop();
        }
        catch (IOException ex)
        {
            throw new UndeclaredThrowableException(ex);
        }

        if (connected)
        {
            outputStream = null;
            connected = false;

            audioMixer.disconnect();
        }
    }

    /*
     * Implements CaptureDevice#getCaptureDeviceInfo(). Delegates to the
     * associated AudioMixer because it knows which CaptureDevice is being
     * wrapped.
     */
    public CaptureDeviceInfo getCaptureDeviceInfo()
    {
        return audioMixer.getCaptureDeviceInfo();
    }

    /*
     * Implements DataSource#getContentType(). Delegates to the associated
     * AudioMixer because it manages the inputs and knows their characteristics.
     */
    public String getContentType()
    {
        return audioMixer.getContentType();
    }

    /*
     * Implements DataSource#getControl(String). Does nothing.
     */
    public Object getControl(String controlType)
    {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * Implements DataSource#getControls(). Does nothing.
     */
    public Object[] getControls()
    {
        // TODO Auto-generated method stub
        return new Object[0];
    }

    /*
     * Implements DataSource#getDuration(). Delegates to the associated
     * AudioMixer because it manages the inputs and knows their characteristics.
     */
    public Time getDuration()
    {
        return audioMixer.getDuration();
    }

    /*
     * Implements CaptureDevice#getFormatControls(). Delegates to the associated
     * AudioMixer because it knows which CaptureDevice is being wrapped.
     */
    public FormatControl[] getFormatControls()
    {
        return audioMixer.getFormatControls();
    }

    /*
     * Implements PushBufferDataSource#getStreams(). Gets a PushBufferStream
     * which reads data from the associated AudioMixer and mixes it.
     */
    public PushBufferStream[] getStreams()
    {
        if (outputStream == null)
        {
            AudioMixer.AudioMixerPushBufferStream audioMixerOutputStream
                = audioMixer.getOutputStream();

            if (audioMixerOutputStream != null)
            {
                outputStream
                    = new AudioMixingPushBufferStream(
                            audioMixerOutputStream,
                            this);
                if (started)
                    outputStream.start();
            }
        }
        return
            (outputStream == null)
                ? new PushBufferStream[0]
                : new PushBufferStream[] { outputStream };
    }

    /*
     * Implements DataSource#start(). Starts the output PushBufferStream of
     * this DataSource (if it exists) and notifies the AudioMixer that one of
     * its output PushBufferDataSources has been started.
     */
    public void start()
        throws IOException
    {
        if (!started)
        {
            if (outputStream != null)
                outputStream.start();
            audioMixer.start();
            started = true;
        }
    }

    /*
     * Implements DataSource#stop(). Notifies the AudioMixer that one of its
     * output PushBufferDataSources has been stopped and stops the output
     * PushBufferStream of this DataSource (if it exists).
     */
    public void stop()
        throws IOException
    {
        if (started)
        {
            audioMixer.stop();
            if (outputStream != null)
                outputStream.stop();
            started = false;
        }
    }
}
