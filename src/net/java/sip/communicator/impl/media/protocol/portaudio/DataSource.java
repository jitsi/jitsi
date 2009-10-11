/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.media.protocol.portaudio;

import java.io.*;

import javax.media.*;
import javax.media.protocol.*;

/**
 * Portaudio datasource.
 *
 * @author Damian Minkov
 */
public class DataSource
    extends PullBufferDataSource
{
    private boolean connected = false;

    private final Object[] controls = new Object[0];

    private boolean started = false;

    private PortAudioStream[] streams = null;

    /**
     * Connect the datasource
     * @throws IOException if we cannot initialize portaudio.
     */
    public void connect()
        throws IOException
    {
        if (connected)
            return;

        try
        {
            PortAudio.initialize();
        }
        catch (PortAudioException paex)
        {
            IOException ioex = new IOException();
            ioex.initCause(paex);
            throw ioex;
        }

        connected = true;
    }

    /**
     * Disconnect the datasource
     */
    public void disconnect()
    {
        if (!connected)
            return;

        try
        {
            stop();
        }
        catch (IOException ioex)
        {
            ioex.printStackTrace();
        }

        connected = false;
    }

    /**
     * Tell we are a raw datasource
     *
     * @return "raw"
     */
    public String getContentType()
    {
        return ContentDescriptor.RAW;
    }

    /**
     * Return required control from the Control[] array
     * if exists.
     * @param controlType the control we are interested in.
     * @return the object that implements the control, or null.
     */
    public Object getControl(String controlType)
    {
        try
        {
            Class<?> controlClass = Class.forName(controlType);

            for (Object control : getControls())
                if (controlClass.isInstance(control))
                    return control;
        }
        catch (ClassNotFoundException cnfex)
        {
            cnfex.printStackTrace();
        }
        return null;
    }

    /**
     * Gives control information to the caller
     * @return the collection of object controls.
     */
    public Object[] getControls()
    {
        return controls;
    }

    /**
     * Gives to the caller the duration information of our stream
     * Which is, obviously unknown. 
     *
     * @return DURATION_UNKNOWN
     */
    public Time getDuration()
    {
        return DURATION_UNKNOWN;
    }

    /**
     * Returns an array of PullBufferStream containing all the streams
     * i.e. only one in our case : only sound.
     *
     * If no stream actually exists, instantiate one on the fly.
     *
     * @return Array of one stream
     */
    public PullBufferStream[] getStreams()
    {
        try
        {
            if (streams == null)
                streams = new PortAudioStream[]
                    {new PortAudioStream(getLocator())};
        }
        catch (Exception e)
        {
            e.printStackTrace();
            // if we cannot parse desired device we will not open a stream
            // so there is no stream returned
            streams = new PortAudioStream[0];
        }

        return streams;
    }

    /**
     * Start the datasource and the underlying stream
     * @throws IOException 
     */
    public void start()
        throws IOException
    {
        if (started)
            return;

        if (!connected)
            throw new IOException("DataSource must be connected");

        try
        {
            streams[0].start();
        }
        catch (PortAudioException pae)
        {
            IOException ioe = new IOException();

            ioe.initCause(pae);
            throw ioe;
        }

        started = true;
    }

    /**
     * Stop the datasource and it's underlying stream
     * @throws IOException 
     */
    public void stop()
        throws IOException
    {
        if (!started)
            return;

        try
        {
            streams[0].stop();
        }
        catch (PortAudioException pae)
        {
            IOException ioe = new IOException();

            ioe.initCause(pae);
            throw ioe;
        }

        started = false;
    }
}
