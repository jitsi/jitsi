/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.media.protocol.portaudio;

import java.io.*;
import java.util.*;

import javax.media.*;
import javax.media.control.*;
import javax.media.format.*;
import javax.media.protocol.*;

/**
 * Implements <tt>DataSource</tt> and <tt>CaptureDevice</tt> for PortAudio.
 *
 * @author Damian Minkov
 * @author Lubomir Marinov
 */
public class DataSource
    extends PullBufferDataSource
    implements CaptureDevice
{
    private boolean connected = false;

    private final Object[] controls = new Object[0];

    private boolean started = false;

    private DSAudioStream[] streams = null;

    private static AudioFormat captureAudioFormat =
        new AudioFormat(
                AudioFormat.LINEAR,
                  8000,
                  16,
                  1,
                  AudioFormat.LITTLE_ENDIAN,
                  AudioFormat.SIGNED,
                  16,
                  Format.NOT_SPECIFIED,
                  Format.byteArray);

    /**
     * Return the formats supported by the datasource.
     *
     * @return the supported formats.
     */
    public static Format getCaptureFormat()
    {
        return captureAudioFormat;
    }

    /**
     * Connect the datasource
     * @throws IOException if we cannot initialize portaudio.
     */
    public void connect()
        throws IOException
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
     * Gets the <tt>CaptureDeviceInfo</tt> that describes this
     * <tt>CaptureDevice</tt>.
     *
     * @return the <tt>CaptureDeviceInfo</tt> that describes this
     * <tt>CaptureDevice</tt>
     */
    public CaptureDeviceInfo getCaptureDeviceInfo()
    {
        /*
         * TODO The implemented search for the CaptureDeviceInfo of this
         * CaptureDevice by looking for its MediaLocator is inefficient.
         */
        @SuppressWarnings("unchecked")
        Vector<CaptureDeviceInfo> captureDeviceInfos
            = (Vector<CaptureDeviceInfo>)
                CaptureDeviceManager.getDeviceList(null);
        MediaLocator locator = getLocator();

        for (CaptureDeviceInfo captureDeviceInfo : captureDeviceInfos)
            if (captureDeviceInfo.getLocator().equals(locator))
                return captureDeviceInfo;
        return null;
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
     * Gets an array of <tt>FormatControl</tt> instances each one of which can
     * be used before {@link #connect()} to get and set the capture
     * <tt>Format</tt> of each one of the capture streams.
     *
     * @return an array of <tt>FormatControl</tt> instances each one of which
     * can be used before {@link #connect()} to get and set the capture
     * <tt>Format</tt> of each one of the capture streams
     */
    public FormatControl[] getFormatControls()
    {
        // TODO Make getControls() actually return a FormatControl instance.
        List<FormatControl> formatControls = new ArrayList<FormatControl>();

        for (Object control : getControls())
            if (control instanceof FormatControl)
                formatControls.add((FormatControl) control);
        return formatControls.toArray(new FormatControl[formatControls.size()]);
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
                streams = new DSAudioStream[]
                    {new DSAudioStream(getLocator())};
        }
        catch (Exception e)
        {
            e.printStackTrace();
            // if we cannot parse desired device we will not open a stream
            // so there is no stream returned
            streams = new DSAudioStream[0];
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
