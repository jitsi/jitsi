/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.jmfext.media.protocol.alsa;

import java.io.*;

import javax.media.*;
import javax.media.protocol.*;

import net.java.sip.communicator.impl.neomedia.control.*;

/**
 * low-latency ALSA access through JNI wrapper
 *
 * @author Jean Lorchat
 * @author Lubomir Marinov
 */
public class DataSource
    extends PushBufferDataSource
{
    protected boolean started = false;
    protected boolean connected = false;
    protected Time duration = DURATION_UNKNOWN;
    protected AlsaStream [] streams = null;
    protected AlsaStream stream = null;

    /**
     * Constructs a new ALSA <tt>DataSource</tt>.
     */
    public DataSource()
    {
    }

    /**
     * Gets the type of the content made available by this <tt>DataSource</tt>
     * (i.e. raw).
     *
     * @return the type of the content made available by this
     * <tt>DataSource</tt> (i.e. raw)
     */
    public String getContentType()
    {
        return ContentDescriptor.RAW;
    }

    /**
     * Connect the datasource
     */
    public void connect() throws IOException
    {
        if (connected)
            return;
        connected = true;
    }

    /**
     * Disconnect the datasource
     */
    public void disconnect()
    {
        try
        {
            if (started)
                stop();
        }
        catch (IOException e) {}
        connected = false;
    }

    /**
     * Start the datasource and the underlying stream
     */
    public void start() throws IOException
    {
        if (!connected)
            throw new java.lang.Error("DataSource must be connected");

        if (started)
            return;

        started = true;
        stream.start(true);
    }

    /**
     * Stop the datasource and it's underlying stream
     */
    public void stop() throws IOException
    {
        if ((!connected) || (!started))
            return;

        started = false;
        stream.start(false);
    }

    /**
     * Gives control information to the caller.
     */
    public Object [] getControls()
    {
        return ControlsAdapter.EMPTY_CONTROLS;
    }

    /**
     * Return required control from the Control[] array
     * if exists, that is
     */
    public Object getControl(String controlType)
    {
        return AbstractControls.getControl(this, controlType);
    }

    /**
     * Gives to the caller the duration information of our stream
     * Which is, obviously unknown. Better turn on that premonition
     * switch again, Mrs Cake.
     *
     * @return DURATION_UNKNOWN
     */
    public Time getDuration()
    {
        return duration;
    }

    /**
     * Returns an array of PushBufferStream containing all the streams
     * i.e. only one in our case : only sound
     *
     * If no stream actually exists, instanciate one on the fly
     *
     * @return Array of one stream
     */
    public PushBufferStream [] getStreams()
    {
        if (streams == null)
        {
            streams = new AlsaStream[1];
            stream = streams[0] = new AlsaStream();
        }
        return streams;
    }
}
