/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.jmfext.media.protocol;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import javax.media.*;
import javax.media.control.*;
import javax.media.protocol.*;

import net.java.sip.communicator.impl.neomedia.control.*;
import net.java.sip.communicator.util.*;

/**
 * Provides a base implementation of <tt>PushBufferDataSource</tt> and
 * <tt>CaptureDevice</tt> in order to facilitate implementers by taking care of
 * boilerplate in the most common cases.
 *
 * @author Lubomir Marinov
 */
public abstract class AbstractPushBufferCaptureDevice
    extends PushBufferDataSource
    implements CaptureDevice
{

    /**
     * The <tt>Logger</tt> used by the <tt>AbstractPushBufferCaptureDevice</tt>
     * class and its instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(AbstractPushBufferCaptureDevice.class);

    /**
     * The value of the <tt>formatControls</tt> property of
     * <tt>AbstractPushBufferCaptureDevice</tt> which represents an empty array
     * of <tt>FormatControl</tt>s. Explicitly defined in order to reduce
     * unnecessary allocations.
     */
    protected static final FormatControl[] EMPTY_FORMAT_CONTROLS
        = new FormatControl[0];

    /**
     * The value of the <tt>streams</tt> property of
     * <tt>AbstractPushBufferCaptureDevice</tt> which represents an empty array
     * of <tt>PushBufferStream</tt>s. Explicitly defined in order to reduce
     * unnecessary allocations.
     */
    protected static final PushBufferStream[] EMPTY_STREAMS
        = new PushBufferStream[0];

    /**
     * The indicator which determines whether a connection to the media source
     * specified by the <tt>MediaLocator</tt> of this <tt>DataSource</tt> has
     * been opened.
     */
    private boolean connected = false;

    /**
     * The array of <tt>FormatControl</tt> instances each one of which can be
     * used before {@link #connect()} to get and set the capture <tt>Format</tt>
     * of each one of the capture streams.
     */
    private FormatControl[] formatControls;

    /**
     * The indicator which determines whether the transfer of media data from
     * this <tt>DataSource</tt> has been started.
     */
    private boolean started = false;

    /**
     * The <tt>PushBufferStream</tt>s through which this
     * <tt>PushBufferDataSource</tt> gives access to its media data.
     */
    private AbstractPushBufferStream[] streams;

    /**
     * Initializes a new <tt>AbstractPushBufferCaptureDevice</tt> instance.
     */
    protected AbstractPushBufferCaptureDevice()
    {
    }

    /**
     * Initializes a new <tt>AbstractPushBufferCaptureDevice</tt> instance from
     * a specific <tt>MediaLocator</tt>.
     *
     * @param locator the <tt>MediaLocator</tt> to create the new instance from
     */
    protected AbstractPushBufferCaptureDevice(MediaLocator locator)
    {
        setLocator(locator);
    }

    /**
     * Determines whether it is allowed to set the format of a specific
     * <tt>FormatControl</tt> of the <tt>PushBufferStream</tt> of this
     * <tt>PushBufferDataSource</tt> with a specific index to a specific
     * <tt>Format</tt>. The <tt>AbstractPushBufferCaptureDevice</tt>
     * implementation always returns <tt>false</tt>.
     *
     * @param format the <tt>Format</tt> which is to be set
     * @param streamIndex the zero-based index of the <tt>PushBufferStream</tt>
     * in the list of streams of this <tt>PushBufferDataSource</tt> which is to
     * have its <tt>Format</tt> set
     * @param formatControl the <tt>FormatControl</tt> of the stream through
     * which the set request is received
     * @return <tt>true</tt> if the specified <tt>Format</tt> is to be set as
     * the format of <tt>formatControl</tt>; otherwise, <tt>false</tt>
     */
    protected boolean canSetFormat(
            Format format,
            int streamIndex,
            FormatControl formatControl)
    {
        return false;
    }

    /**
     * Opens a connection to the media source specified by the
     * <tt>MediaLocator</tt> of this <tt>DataSource</tt>.
     *
     * @throws IOException if anything goes wrong while opening the connection
     * to the media source specified by the <tt>MediaLocator</tt> of this
     * <tt>DataSource</tt>
     */
    public synchronized void connect()
        throws IOException
    {
        if (!connected)
        {
            doConnect();
            connected = true;
        }
    }

    /**
     * Creates a new <tt>FormatControl</tt> instance which is to be associated
     * with a <tt>PushBufferStream</tt> at a specific zero-based index in the
     * list of streams of this <tt>PushBufferDataSource</tt>. As the
     * <tt>FormatControl</tt>s of a <tt>PushBufferDataSource</tt> can be
     * requested before {@link #connect()}, its <tt>PushBufferStream</tt>s may
     * not exist at the time of the request for the creation of the
     * <tt>FormatControl</tt>.
     *
     * @param streamIndex the zero-based index of the <tt>PushBufferStream</tt>
     * in the list of streams of this <tt>PushBufferDataSource</tt> which is to
     * be associated with the new <tt>FormatControl</tt> instance
     * @return a new <tt>FormatControl</tt> instance which is to be associated
     * with a <tt>PushBufferStream</tt> at the specified <tt>streamIndex</tt> in
     * the list of streams of this <tt>PushBufferDataSource</tt>
     */
    protected FormatControl createFormatControl(final int streamIndex)
    {
        return
            new AbstractFormatControl()
            {
                /**
                 * The <tt>Format</tt> of this <tt>FormatControl</tt> and,
                 * respectively, of the media data of its owner.
                 */
                private Format format;

                /**
                 * Gets the <tt>Format</tt> of the media data of the owner of
                 * this <tt>FormatControl</tt>.
                 *
                 * @return the <tt>Format</tt> of the media data of the owner of
                 * this <tt>FormatControl</tt>
                 */
                public Format getFormat()
                {
                    if (format == null)
                        format
                            = AbstractPushBufferCaptureDevice.this
                                    .getFormat(streamIndex, this);
                    return format;
                }

                /**
                 * Gets the <tt>Format</tt>s in which the owner of this
                 * <tt>FormatControl</tt> is capable of providing media data.
                 *
                 * @return an array of <tt>Format</tt>s in which the owner of
                 * this <tt>FormatControl</tt> is capable of providing media
                 * data
                 */
                public Format[] getSupportedFormats()
                {
                    return
                        AbstractPushBufferCaptureDevice.this
                                .getSupportedFormats(streamIndex, this);
                }

                /**
                 * Implements {@link FormatControl#setFormat(Format)}. Attempts
                 * to set the <tt>Format</tt> in which the owner of this
                 * <tt>FormatControl</tt> is to provide media data.
                 *
                 * @param format the <tt>Format</tt> to be set on this instance
                 * @return the currently set <tt>Format</tt> after the attempt
                 * to set it on this instance if <tt>format</tt> is supported by
                 * this instance and regardless of whether it was actually set;
                 * <tt>null</tt> if <tt>format</tt> is not supported by this
                 * instance
                 */
                @Override
                public Format setFormat(Format format)
                {
                    format = super.setFormat(format);

                    if ((format != null)
                            && canSetFormat(format, streamIndex, this))
                        this.format = format;
                    return getFormat();
                }
            };
    }

    /**
     * Creates the <tt>FormatControl</tt>s of this <tt>CaptureDevice</tt>.
     *
     * @return an array of the <tt>FormatControl</tt>s of this
     * <tt>CaptureDevice</tt>
     */
    protected FormatControl[] createFormatControls()
    {
        FormatControl formatControl = createFormatControl(0);

        return
            (formatControl == null)
                ? EMPTY_FORMAT_CONTROLS
                : new FormatControl[] { formatControl };
    }

    /**
     * Create a new <tt>PushBufferStream</tt> which is to be at a specific
     * zero-based index in the list of streams of this
     * <tt>PushBufferDataSource</tt>. The <tt>Format</tt>-related information of
     * the new instance is to be abstracted by a specific
     * <tt>FormatControl</tt>.
     *
     * @param streamIndex the zero-based index of the <tt>PushBufferStream</tt>
     * in the list of streams of this <tt>PushBufferDataSource</tt>
     * @param formatControl the <tt>FormatControl</tt> which is to abstract the
     * <tt>Format</tt>-related information of the new instance
     * @return a new <tt>PushBufferStream</tt> which is to be at the specified
     * <tt>streamIndex</tt> in the list of streams of this
     * <tt>PushBufferDataSource</tt> and which has its <tt>Format</tt>-related
     * information abstracted by the specified <tt>formatControl</tt>
     */
    protected abstract AbstractPushBufferStream createStream(
            int streamIndex,
            FormatControl formatControl);

    /**
     * Closes the connection to the media source specified by the
     * <tt>MediaLocator</tt> of this <tt>DataSource</tt>. If such a connection
     * has not been opened, the call is ignored.
     */
    public synchronized void disconnect()
    {
        try
        {
            stop();
        }
        catch (IOException ioex)
        {
            logger.error("Failed to stop " + getClass().getSimpleName(), ioex);
        }

        if (connected)
        {
            doDisconnect();
            connected = false;
        }
    }

    /**
     * Opens a connection to the media source specified by the
     * <tt>MediaLocator</tt> of this <tt>DataSource</tt>. Allows extenders to
     * override and be sure that there will be no request to open a connection
     * if the connection has already been opened.
     *
     * @throws IOException if anything goes wrong while opening the connection
     * to the media source specified by the <tt>MediaLocator</tt> of this
     * <tt>DataSource</tt>
     */
    protected synchronized void doConnect()
        throws IOException
    {
    }

    /**
     * Closes the connection to the media source specified by the
     * <tt>MediaLocator</tt> of this <tt>DataSource</tt>. Allows extenders to
     * override and be sure that there will be no request to close a connection
     * if the connection has not been opened yet.
     */
    protected synchronized void doDisconnect()
    {
        if (streams != null)
            try
            {
                for (AbstractPushBufferStream stream : streams)
                    stream.close();
            }
            finally
            {
                streams = null;
            }
    }

    /**
     * Starts the transfer of media data from this <tt>DataSource</tt>. Allows
     * extenders to override and be sure that there will be no request to start
     * the transfer of media data if it has already been started.
     *
     * @throws IOException if anything goes wrong while starting the transfer of
     * media data from this <tt>DataSource</tt>
     */
    protected synchronized void doStart()
        throws IOException
    {
        if (streams != null)
            for (AbstractPushBufferStream stream : streams)
                stream.start();
    }

    /**
     * Stops the transfer of media data from this <tt>DataSource</tt>. Allows
     * extenders to override and be sure that there will be no request to stop
     * the transfer of media data if it has not been started yet.
     *
     * @throws IOException if anything goes wrong while stopping the transfer of
     * media data from this <tt>DataSource</tt>
     */
    protected synchronized void doStop()
        throws IOException
    {
        if (streams != null)
            for (AbstractPushBufferStream stream : streams)
                stream.stop();
    }

    /**
     * Gets the <tt>CaptureDeviceInfo</tt> of this <tt>CaptureDevice</tt> which
     * describes it.
     *
     * @return the <tt>CaptureDeviceInfo</tt> of this <tt>CaptureDevice</tt>
     * which describes it
     */
    public CaptureDeviceInfo getCaptureDeviceInfo()
    {
        return getCaptureDeviceInfo(this);
    }

    /**
     * Gets the <tt>CaptureDeviceInfo</tt> of a specific <tt>CaptureDevice</tt>
     * by locating its registration in JMF using its <tt>MediaLocator</tt>.
     *
     * @param captureDevice the <tt>CaptureDevice</tt> to gets the
     * <tt>CaptureDeviceInfo</tt> of
     * @return the <tt>CaptureDeviceInfo</tt> of the specified
     * <tt>CaptureDevice</tt> as registered in JMF
     */
    public static CaptureDeviceInfo getCaptureDeviceInfo(
            DataSource captureDevice)
    {
        /*
         * TODO The implemented search for the CaptureDeviceInfo of this
         * CaptureDevice by looking for its MediaLocator is inefficient.
         */
        @SuppressWarnings("unchecked")
        Vector<CaptureDeviceInfo> captureDeviceInfos
            = (Vector<CaptureDeviceInfo>)
                CaptureDeviceManager.getDeviceList(null);
        MediaLocator locator = captureDevice.getLocator();

        for (CaptureDeviceInfo captureDeviceInfo : captureDeviceInfos)
            if (captureDeviceInfo.getLocator().equals(locator))
                return captureDeviceInfo;
        return null;
    }

    /**
     * Gets the content type of the media represented by this instance. The
     * <tt>AbstractPushBufferCaptureDevice</tt> implementation always returns
     * {@link ContentDescriptor#RAW}.
     *
     * @return the content type of the media represented by this instance
     */
    public String getContentType()
    {
        return ContentDescriptor.RAW;
    }

    /**
     * Gets the control of the specified type available for this instance.
     *
     * @param controlType the type of the control available for this instance to
     * be retrieved
     * @return an <tt>Object</tt> which represents the control of the specified
     * type available for this instance if such a control is indeed available;
     * otherwise, <tt>null</tt>
     */
    public Object getControl(String controlType)
    {
        return AbstractControls.getControl(this, controlType);
    }

    /**
     * Implements {@link Controls#getControls()}. Gets the controls available
     * for this instance.
     *
     * @return an array of <tt>Object</tt>s which represent the controls
     * available for this instance
     */
    public Object[] getControls()
    {
        FormatControl[] formatControls = internalGetFormatControls();

        if ((formatControls == null) || (formatControls.length == 0))
            return ControlsAdapter.EMPTY_CONTROLS;
        else
        {
            Object[] controls = new Object[formatControls.length];

            System.arraycopy(
                    formatControls,
                    0,
                    controls,
                    0,
                    formatControls.length);
            return controls;
        }
    }

    /**
     * Gets the duration of the media represented by this instance. The
     * <tt>AbstractPushBufferCaptureDevice</tt> always returns
     * {@link #DURATION_UNBOUNDED}.
     *
     * @return the duration of the media represented by this instance
     */
    public Time getDuration()
    {
        return DURATION_UNBOUNDED;
    }

    /**
     * Gets the <tt>Format</tt> which is to be reported by a specific
     * <tt>FormatControl</tt> for a <tt>PushBufferStream</tt> at a specific
     * zero-based index in the list of streams of this
     * <tt>PushBufferDataSource</tt>.
     *
     * @param streamIndex the zero-based index of the <tt>PushBufferStream</tt>
     * in the list of streams of this <tt>PushBufferDataSource</tt> for which
     * the specified <tt>FormatControl</tt> is to report a <tt>Format</tt>
     * @param formatControl the <tt>FormatControl</tt> which si to report the
     * <tt>Format</tt>
     * @return the <tt>Format</tt> which is to be reported by
     * <tt>formatControl</tt> as the format of the <tt>PushBufferStream</tt> at
     * the specified <tt>streamIndex</tt> in the list of streams of this
     * <tt>PushBufferDataSource</tt>
     */
    protected Format getFormat(int streamIndex, FormatControl formatControl)
    {
        Format[] supportedFormats
            = getSupportedFormats(streamIndex, formatControl);

        return
            ((supportedFormats == null) || (supportedFormats.length < 1))
                ? null
                : supportedFormats[0];
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
     * Gets the <tt>PushBufferStream</tt>s through which this
     * <tt>PushBufferDataSource</tt> gives access to its media data.
     *
     * @return an array of the <tt>PushBufferStream</tt>s through which this
     * <tt>PushBufferDataSource</tt> gives access to its media data
     */
    public synchronized PushBufferStream[] getStreams()
    {
        if (streams == null)
        {
            FormatControl[] formatControls = internalGetFormatControls();

            if (formatControls != null)
            {
                int formatControlCount = formatControls.length;

                streams = new AbstractPushBufferStream[formatControlCount];
                for (int i = 0; i < formatControlCount; i++)
                    streams[i] = createStream(i, formatControls[i]);

                /*
                 * Start the streams if this DataSource has already been
                 * started.
                 */
                if (started)
                    for (AbstractPushBufferStream stream : streams)
                        try
                        {
                            stream.start();
                        }
                        catch (IOException ioex)
                        {
                            throw new UndeclaredThrowableException(ioex);
                        }
            }
        }
        if (streams == null)
            return EMPTY_STREAMS;
        else
        {
            PushBufferStream[] clone = new PushBufferStream[streams.length];

            System.arraycopy(streams, 0, clone, 0, streams.length);
            return clone;
        }
    }

    /**
     * Gets the <tt>Format</tt>s which are to be reported by a specific
     * <tt>FormatControl</tt> as supported formats for a
     * <tt>PushBufferStream</tt> at a specific zero-based index in the list of
     * streams of this <tt>PushBufferDataSource</tt>.
     *
     * @param streamIndex the zero-based index of the <tt>PushBufferStream</tt>
     * for which the specified <tt>FormatControl</tt> is to report the list of
     * supported <tt>Format</tt>s
     * @param formatControl the <tt>FormatControl</tt> which is to reports a
     * list of supported <tt>Format</tt>s
     * @return an array of <tt>Format</tt>s to be reported by
     * <tt>formatControl</tt> as the supported formats for the
     * <tt>PushBufferStream</tt> at the specified <tt>streamIndex</tt> in the
     * list of streams of this <tt>PushBufferDataSource</tt>
     */
    protected Format[] getSupportedFormats(
            int streamIndex,
            FormatControl formatControl)
    {
        CaptureDeviceInfo captureDeviceInfo = getCaptureDeviceInfo();

        return
            (captureDeviceInfo == null) ? null : captureDeviceInfo.getFormats();
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
    private synchronized FormatControl[] internalGetFormatControls()
    {
        if (formatControls == null)
            formatControls = createFormatControls();
        return formatControls;
    }

    /**
     * Starts the transfer of media data from this <tt>DataSource</tt>
     *
     * @throws IOException if anything goes wrong while starting the transfer of
     * media data from this <tt>DataSource</tt>
     */
    public synchronized void start()
        throws IOException
    {
        if (!started)
        {
            if (!connected)
                throw
                    new IOException(
                            getClass().getSimpleName() + " not connected");

            doStart();
            started = true;
        }
    }

    /**
     * Stops the transfer of media data from this <tt>DataSource</tt>.
     *
     * @throws IOException if anything goes wrong while stopping the transfer of
     * media data from this <tt>DataSource</tt>
     */
    public synchronized void stop()
        throws IOException
    {
        if (started)
        {
            doStop();
            started = false;
        }
    }
}
