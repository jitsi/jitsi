/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.jmfext.media.protocol.portaudio;

import java.io.*;
import java.util.*;

import javax.media.*;
import javax.media.control.*;
import javax.media.format.*;
import javax.media.protocol.*;

import net.java.sip.communicator.impl.neomedia.*;
import net.java.sip.communicator.impl.neomedia.control.*;
import net.java.sip.communicator.impl.neomedia.portaudio.*;
import net.java.sip.communicator.util.*;

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

    /**
     * The <tt>Logger</tt> used by the <tt>DataSource</tt> class and its
     * instances for logging output.
     */
    private static final Logger logger = Logger.getLogger(DataSource.class);

    /**
     * The value of the <tt>streams</tt> property of <tt>DataSource</tt> which
     * represents an empty array of <tt>PullBufferStream</tt>s i.e. no
     * <tt>DSAudioStream</tt>s in <tt>DataSource</tt>. Explicitly defined in
     * order to reduce unnecessary allocations.
     */
    private static final PullBufferStream[] EMPTY_STREAMS
        = new PullBufferStream[0];

    /**
     * Indicates whether the datasource is connected or not.
     */
    private boolean connected = false;

    /**
     * The JMF controls (which are likely of type <tt>Control</tt>) available
     * for this <tt>DataSource</tt>.
     */
    private final Object[] controls
        = new Object[]
                {
                    new FormatControlImpl()
                };

    /**
     * Indicates whether the datasource is starteded or not.
     */
    private boolean started = false;

    /**
     * The stream created by the datasource.
     */
    private DSAudioStream stream = null;

    /**
     * The format of the media captured by the datasource.
     */
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
    public synchronized void connect()
        throws IOException
    {
        if (connected)
            return;

        connected = true;
        if (logger.isTraceEnabled())
            logger.trace("Connected " + MediaStreamImpl.toString(this));
    }

    /**
     * Disconnect the datasource
     */
    public synchronized void disconnect()
    {
        if (!connected)
            return;

        try
        {
            stop();
        }
        catch (IOException ioex)
        {
            logger
                .warn(
                    "Failed to stop DataSource with locator " + getLocator(),
                    ioex);
        }

        connected = false;
        if (logger.isTraceEnabled())
            logger.trace("Disconnected " + MediaStreamImpl.toString(this));
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
        return AbstractControls.getControl(this, controlType);
    }

    /**
     * Gives control information to the caller
     * @return the collection of object controls.
     */
    public Object[] getControls()
    {
        /*
         * The field controls is private so we cannot directly return it.
         * Otherwise, the caller will be able to modify it.
         */
        return controls.clone();
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
        return AbstractFormatControl.getFormatControls(this);
    }

    /**
     * Returns an array of PullBufferStream containing all the streams
     * i.e. only one in our case : only sound.
     *
     * If no stream actually exists, instantiate one on the fly.
     *
     * @return Array of one stream
     */
    public synchronized PullBufferStream[] getStreams()
    {
        MediaLocator locator = null;

        try
        {
            if (stream == null)
            {
                locator = getLocator();
                stream = new DSAudioStream(locator);
            }
        }
        catch (Exception e)
        {
            // if we cannot parse desired device we will not open a stream
            // so there is no stream returned
            stream = null;

            logger
                .error(
                    "Failed to create DSAudioStream from locator " + locator,
                    e);
        }

        return
            (stream == null)
                ? EMPTY_STREAMS
                : new PullBufferStream[] { stream };
    }

    /**
     * Start the datasource and the underlying stream
     * @throws IOException 
     */
    public synchronized void start()
        throws IOException
    {
        if (started)
            return;

        if (!connected)
            throw new IOException("DataSource must be connected");

        try
        {
            stream.start();
        }
        catch (PortAudioException pae)
        {
            IOException ioe = new IOException();

            ioe.initCause(pae);
            throw ioe;
        }

        started = true;
        if (logger.isTraceEnabled())
            logger.trace("Started " + MediaStreamImpl.toString(this));
    }

    /**
     * Stop the datasource and it's underlying stream
     * @throws IOException 
     */
    public synchronized void stop()
        throws IOException
    {
        if (!started)
            return;

        try
        {
            stream.stop();
        }
        catch (PortAudioException pae)
        {
            IOException ioe = new IOException();

            ioe.initCause(pae);
            throw ioe;
        }

        started = false;
        if (logger.isTraceEnabled())
            logger.trace("Stopped " + MediaStreamImpl.toString(this));
    }

    /**
     * Implements <tt>FormatControl</tt> for this <tt>DataSource</tt> instance.
     * At least getting the <tt>Format</tt> of the <tt>DataSource</tt> is
     * important because, for example, <tt>AudioMixer</tt> will ask for it.
     */
    private class FormatControlImpl
        extends AbstractFormatControl
    {

        /**
         * Implements {@link FormatControl#getFormat()}.
         *
         * @return the <tt>Format</tt> of this <tt>DataSource</tt>
         */
        public Format getFormat()
        {
            return getCaptureFormat();
        }

        /**
         * Implements {@link FormatControl#getSupportedFormats()}.
         *
         * @return an array of <tt>Format</tt> element type which lists the JMF
         * formats supported by this <tt>DataSource</tt> i.e. the ones in which
         * it is able to output
         */
        public Format[] getSupportedFormats()
        {
            return new Format[] { getCaptureFormat() };
        }
    }
}
