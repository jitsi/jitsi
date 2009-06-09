/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.media.conference;

import java.io.*;
import java.lang.reflect.*;

import javax.media.*;
import javax.media.control.*;
import javax.media.protocol.*;

/**
 * Represents a <code>PushBufferDataSource</code> which provides a single
 * <code>PushBufferStream</code> containing the result of the audio mixing of
 * <code>DataSource</code>s.
 * 
 * @author Lubomir Marinov
 */
public class AudioMixingPushBufferDataSource
    extends PushBufferDataSource
    implements CaptureDevice
{

    /**
     * The <code>AudioMixer</code> performing the audio mixing, managing the
     * input <code>DataSource</code>s and pushing the data of this output
     * <code>PushBufferDataSource</code>.
     */
    private final AudioMixer audioMixer;

    /**
     * The indicator which determines whether this <code>DataSource</code> is
     * connected.
     */
    private boolean connected;

    /**
     * The one and only <code>PushBufferStream</code> this
     * <code>PushBufferDataSource</code> provides to its clients and containing
     * the result of the audio mixing performed by <code>audioMixer</code>.
     */
    private AudioMixingPushBufferStream outputStream;

    /**
     * The indicator which determines whether this <code>DataSource</code> is
     * started.
     */
    private boolean started;

    /**
     * Initializes a new <code>AudioMixingPushBufferDataSource</code> instance
     * which gives access to the result of the audio mixing performed by a
     * specific <code>AudioMixer</code>.
     * 
     * @param audioMixer the <code>AudioMixer</code> performing audio mixing,
     *            managing the input <code>DataSource</code>s and pushing the
     *            data of the new output <code>PushBufferDataSource</code>
     */
    public AudioMixingPushBufferDataSource(AudioMixer audioMixer)
    {
        this.audioMixer = audioMixer;
    }

    /**
     * Adds a new input <code>DataSource</code> to be mixed by the associated
     * <code>AudioMixer</code> of this instance and to not have its audio
     * contributions included in the mixing output represented by this
     * <code>DataSource</code>.
     * 
     * @param inputDataSource a <code>DataSource</code> to be added for mixing
     *            to the <code>AudioMixer</code> associate with this instance
     *            and to not have its audio contributions included in the mixing
     *            output represented by this <code>DataSource</code>
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
