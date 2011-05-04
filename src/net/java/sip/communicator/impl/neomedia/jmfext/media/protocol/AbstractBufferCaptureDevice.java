/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.jmfext.media.protocol;

import java.io.*;
import java.lang.reflect.*;

import javax.media.*;
import javax.media.Controls; // disambiguation
import javax.media.control.*;
import javax.media.protocol.*;

import net.java.sip.communicator.impl.neomedia.control.*;
import net.java.sip.communicator.util.*;

/**
 * Facilitates the implementations of the <tt>CaptureDevice</tt> and
 * <tt>DataSource</tt> interfaces provided by
 * <tt>AbstractPullBufferCaptureDevice</tt> and
 * <tt>AbstractPushBufferCaptureDevice</tt>.
 *
 * @param <AbstractBufferStreamT> the type of <tt>AbstractBufferStream</tt>
 * through which this <tt>AbstractBufferCaptureDevice</tt> is to give access to
 * its media data
 *
 * @author Lyubomir Marinov
 */
public abstract class AbstractBufferCaptureDevice
        <AbstractBufferStreamT extends AbstractBufferStream>
    implements CaptureDevice,
               Controls
{
    /**
     * The <tt>Logger</tt> used by the <tt>AbstractBufferCaptureDevice</tt>
     * class and its instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(AbstractBufferCaptureDevice.class);

    /**
     * The value of the <tt>formatControls</tt> property of
     * <tt>AbstractBufferCaptureDevice</tt> which represents an empty array of
     * <tt>FormatControl</tt>s. Explicitly defined in order to reduce
     * unnecessary allocations.
     */
    private static final FormatControl[] EMPTY_FORMAT_CONTROLS
        = new FormatControl[0];

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
     * <p>
     * Warning: Caution is advised when directly using the field and access to
     * it is to be synchronized with sync root <tt>this</tt>.
     * </p>
     */
    private AbstractBufferStream[] streams;

    /**
     * Opens a connection to the media source of this
     * <tt>AbstractBufferCaptureDevice</tt>.
     *
     * @throws IOException if anything goes wrong while opening the connection
     * to the media source of this <tt>AbstractBufferCaptureDevice</tt>
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
                    format
                        = AbstractBufferCaptureDevice.this.internalGetFormat(
                                streamIndex,
                                format);
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
                        AbstractBufferCaptureDevice.this.getSupportedFormats(
                                streamIndex);
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
                    Format setFormat = super.setFormat(format);

                    if (setFormat != null)
                    {
                        setFormat
                            = AbstractBufferCaptureDevice.this
                                    .internalSetFormat(
                                            streamIndex,
                                            setFormat,
                                            format);
                        if (setFormat != null)
                            this.format = setFormat;
                    }
                    return setFormat;
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
     * Create a new <tt>AbstractBufferStream</tt> which is to be at a specific
     * zero-based index in the list of streams of this
     * <tt>AbstractBufferCaptureDevice</tt>. The <tt>Format</tt>-related
     * information of the new instance is to be abstracted by a specific
     * <tt>FormatControl</tt>.
     *
     * @param streamIndex the zero-based index of the
     * <tt>AbstractBufferStream</tt> in the list of streams of this
     * <tt>AbstractBufferCaptureDevice</tt>
     * @param formatControl the <tt>FormatControl</tt> which is to abstract the
     * <tt>Format</tt>-related information of the new instance
     * @return a new <tt>AbstractBufferStream</tt> which is to be at the
     * specified <tt>streamIndex</tt> in the list of streams of this
     * <tt>AbstractBufferCaptureDevice</tt> and which has its
     * <tt>Format</tt>-related information abstracted by the specified
     * <tt>formatControl</tt>
     */
    protected abstract AbstractBufferStreamT createStream(
            int streamIndex,
            FormatControl formatControl);

    /**
     * Provides the default implementation of
     * <tt>AbstractBufferCaptureDevice</tt> for {@link #doStart()}.
     *
     * @throws IOException if anything goes wrong while starting the transfer of
     * media data from this <tt>AbstractBufferCaptureDevice</tt>
     * @see #doStart()
     */
    final void defaultDoStart()
        throws IOException
    {
        if (streams != null)
            for (AbstractBufferStream stream : streams)
                stream.start();
    }

    /**
     * Provides the default implementation of
     * <tt>AbstractBufferCaptureDevice</tt> for {@link #doStop()}.
     *
     * @throws IOException if anything goes wrong while stopping the transfer of
     * media data from this <tt>AbstractBufferCaptureDevice</tt>
     * @see #doStop()
     */
    final void defaultDoStop()
        throws IOException
    {
        if (streams != null)
            for (AbstractBufferStream stream : streams)
                stream.stop();
    }

    /**
     * Provides the default implementation of
     * <tt>AbstractBufferCaptureDevice</tt> for {@link #getFormat(int, Format)}.
     *
     * @param streamIndex the zero-based index of the
     * <tt>AbstractBufferStream</tt> the <tt>Format</tt> of which is to be
     * retrieved
     * @param oldValue the last-known <tt>Format</tt> for the
     * <tt>AbstractBufferStream</tt> at the specified <tt>streamIndex</tt>
     * @return the <tt>Format</tt> to be reported by the <tt>FormatControl</tt>
     * of the <tt>AbstractBufferStream</tt> at the specified
     * <tt>streamIndex</tt> in the list of streams of this
     * <tt>AbstractBufferCaptureDevice</tt>
     * @see #getFormat(int, Format)
     */
    final Format defaultGetFormat(int streamIndex, Format oldValue)
    {
        if (oldValue != null)
            return oldValue;

        Format[] supportedFormats = getSupportedFormats(streamIndex);

        return
            ((supportedFormats == null) || (supportedFormats.length < 1))
                ? null
                : supportedFormats[0];
    }

    /**
     * Provides the default implementation of
     * <tt>AbstractBufferCaptureDevice</tt> for
     * {@link #getSupportedFormats(int)}.
     *
     * @param streamIndex the zero-based index of the
     * <tt>AbstractBufferStream</tt> for which the specified
     * <tt>FormatControl</tt> is to report the list of supported
     * <tt>Format</tt>s
     * @return an array of <tt>Format</tt>s to be reported by a
     * <tt>FormatControl</tt> as the supported formats for the
     * <tt>AbstractBufferStream</tt> at the specified <tt>streamIndex</tt> in
     * the list of streams of this <tt>AbstractBufferCaptureDevice</tt>
     */
    final Format[] defaultGetSupportedFormats(int streamIndex)
    {
        CaptureDeviceInfo captureDeviceInfo = getCaptureDeviceInfo();

        return
            (captureDeviceInfo == null) ? null : captureDeviceInfo.getFormats();
    }

    /**
     * Closes the connection to the media source specified of this
     * <tt>AbstractBufferCaptureDevice</tt>. If such a connection has not been
     * opened, the call is ignored.
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
     * Opens a connection to the media source of this
     * <tt>AbstractBufferCaptureDevice</tt>. Allows extenders to override and be
     * sure that there will be no request to open a connection if the connection
     * has already been opened.
     *
     * @throws IOException if anything goes wrong while opening the connection
     * to the media source of this <tt>AbstractBufferCaptureDevice</tt>
     */
    protected abstract void doConnect()
        throws IOException;

    /**
     * Closes the connection to the media source of this
     * <tt>AbstractBufferCaptureDevice</tt>. Allows extenders to override and be
     * sure that there will be no request to close a connection if the
     * connection has not been opened yet.
     */
    protected abstract void doDisconnect();

    /**
     * Starts the transfer of media data from this
     * <tt>AbstractBufferCaptureDevice</tt>. Allows extenders to override and be
     * sure that there will be no request to start the transfer of media data if
     * it has already been started.
     *
     * @throws IOException if anything goes wrong while starting the transfer of
     * media data from this <tt>AbstractBufferCaptureDevice</tt>
     */
    protected abstract void doStart()
        throws IOException;

    /**
     * Stops the transfer of media data from this
     * <tt>AbstractBufferCaptureDevice</tt>. Allows extenders to override and be
     * sure that there will be no request to stop the transfer of media data if
     * it has not been started yet.
     *
     * @throws IOException if anything goes wrong while stopping the transfer of
     * media data from this <tt>AbstractBufferCaptureDevice</tt>
     */
    protected abstract void doStop()
        throws IOException;

    /**
     * Gets the <tt>CaptureDeviceInfo</tt> of this <tt>CaptureDevice</tt> which
     * describes it.
     *
     * @return the <tt>CaptureDeviceInfo</tt> of this <tt>CaptureDevice</tt>
     * which describes it
     */
    public abstract CaptureDeviceInfo getCaptureDeviceInfo();

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
     * Implements {@link javax.media.Controls#getControls()}. Gets the controls
     * available for this instance.
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
     * Gets the <tt>Format</tt> to be reported by the <tt>FormatControl</tt> of
     * an <tt>AbstractBufferStream</tt> at a specific zero-based index in the
     * list of streams of this <tt>AbstractBufferCaptureDevice</tt>. The
     * <tt>AbstractBufferStream</tt> may not exist at the time of requesting its
     * <tt>Format</tt>. Allows extenders to override the default behavior which
     * is to report any last-known format or the first <tt>Format</tt> from the
     * list of supported formats as defined in the JMF registration of this
     * <tt>CaptureDevice</tt>.
     *
     * @param streamIndex the zero-based index of the
     * <tt>AbstractBufferStream</tt> the <tt>Format</tt> of which is to be
     * retrieved
     * @param oldValue the last-known <tt>Format</tt> for the
     * <tt>PushBufferStream</tt> at the specified <tt>streamIndex</tt>
     * @return the <tt>Format</tt> to be reported by the <tt>FormatControl</tt>
     * of the <tt>PushBufferStream</tt> at the specified <tt>streamIndex</tt> in
     * the list of streams of this <tt>PushBufferDataSource</tt>.
     */
    protected abstract Format getFormat(int streamIndex, Format oldValue);

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
     * Gets the <tt>Object</tt> which is to synchronize the access to
     * {@link #streams()} and its return value.
     *
     * @return the <tt>Object</tt> which is to synchronize the access to
     * {@link #streams()} and its return value 
     */
    Object getStreamSyncRoot()
    {
        return this;
    }

    /**
     * Gets the <tt>AbstractBufferStream</tt>s through which this
     * <tt>AbstractBufferCaptureDevice</tt> gives access to its media data.
     *
     * @param clz the <tt>Class</tt> of <tt>AbstractBufferStream</tt> which is
     * to be the element type of the returned array
     * @return an array of the <tt>AbstractBufferStream</tt>s through which this
     * <tt>AbstractBufferCaptureDevice</tt> gives access to its media data
     */
    public synchronized AbstractBufferStreamT[] getStreams(
            Class<AbstractBufferStreamT> clz)
    {
        if (streams == null)
        {
            FormatControl[] formatControls = internalGetFormatControls();

            if (formatControls != null)
            {
                int formatControlCount = formatControls.length;

                streams = new AbstractBufferStream[formatControlCount];
                for (int i = 0; i < formatControlCount; i++)
                    streams[i] = createStream(i, formatControls[i]);

                /*
                 * Start the streams if this DataSource has already been
                 * started.
                 */
                if (started)
                    for (AbstractBufferStream stream : streams)
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

        int streamCount = (streams == null) ? 0 : streams.length;
        @SuppressWarnings("unchecked")
        AbstractBufferStreamT[] clone
            = (AbstractBufferStreamT[]) Array.newInstance(clz, streamCount);

        if (streamCount != 0)
            System.arraycopy(streams, 0, clone, 0, streamCount);
        return clone;
    }

    /**
     * Gets the <tt>Format</tt>s which are to be reported by a
     * <tt>FormatControl</tt> as supported formats for a
     * <tt>AbstractBufferStream</tt> at a specific zero-based index in the list
     * of streams of this <tt>AbstractBufferCaptureDevice</tt>.
     *
     * @param streamIndex the zero-based index of the
     * <tt>AbstractBufferStream</tt> for which the specified
     * <tt>FormatControl</tt> is to report the list of supported
     * <tt>Format</tt>s
     * @return an array of <tt>Format</tt>s to be reported by a
     * <tt>FormatControl</tt> as the supported formats for the
     * <tt>AbstractBufferStream</tt> at the specified <tt>streamIndex</tt> in
     * the list of streams of this <tt>AbstractBufferCaptureDevice</tt>
     */
    protected abstract Format[] getSupportedFormats(int streamIndex);

    /**
     * Gets the <tt>Format</tt> to be reported by the <tt>FormatControl</tt> of
     * a <tt>PushBufferStream</tt> at a specific zero-based index in the list of
     * streams of this <tt>PushBufferDataSource</tt>. The
     * <tt>PushBufferStream</tt> may not exist at the time of requesting its
     * <tt>Format</tt>.
     *
     * @param streamIndex the zero-based index of the <tt>PushBufferStream</tt>
     * the <tt>Format</tt> of which is to be retrieved
     * @param oldValue the last-known <tt>Format</tt> for the
     * <tt>PushBufferStream</tt> at the specified <tt>streamIndex</tt>
     * @return the <tt>Format</tt> to be reported by the <tt>FormatControl</tt>
     * of the <tt>PushBufferStream</tt> at the specified <tt>streamIndex</tt> in
     * the list of streams of this <tt>PushBufferDataSource</tt>.
     */
    private Format internalGetFormat(int streamIndex, Format oldValue)
    {
        synchronized (this)
        {
            if (streams != null)
            {
                AbstractBufferStream stream = streams[streamIndex];

                if (stream != null)
                {
                    Format streamFormat = stream.internalGetFormat();

                    if (streamFormat != null)
                        return streamFormat;
                }
            }
        }
        return getFormat(streamIndex, oldValue);
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
     * Attempts to set the <tt>Format</tt> to be reported by the
     * <tt>FormatControl</tt> of a <tt>PushBufferStream</tt> at a specific
     * zero-based index in the list of streams of this
     * <tt>PushBufferDataSource</tt>.
     *
     * @param streamIndex the zero-based index of the <tt>PushBufferStream</tt>
     * the <tt>Format</tt> of which is to be set
     * @param oldValue the last-known <tt>Format</tt> for the
     * <tt>PushBufferStream</tt> at the specified <tt>streamIndex</tt>
     * @param newValue the <tt>Format</tt> which is to be set
     * @return the <tt>Format</tt> to be reported by the <tt>FormatControl</tt>
     * of the <tt>PushBufferStream</tt> at the specified <tt>streamIndex</tt>
     * in the list of streams of this <tt>PushBufferStream</tt> or <tt>null</tt>
     * if the attempt to set the <tt>Format</tt> did not success and any
     * last-known <tt>Format</tt> is to be left in effect
     */
    private Format internalSetFormat(
            int streamIndex,
            Format oldValue, Format newValue)
    {
        synchronized (this)
        {
            if (streams != null)
            {
                AbstractBufferStream stream = streams[streamIndex];

                if (stream != null)
                    return stream.internalSetFormat(newValue);
            }
        }
        return setFormat(streamIndex, oldValue, newValue);
    }

    /**
     * Attempts to set the <tt>Format</tt> to be reported by the
     * <tt>FormatControl</tt> of a <tt>AbstractBufferStream</tt> at a specific
     * zero-based index in the list of streams of this
     * <tt>AbstractBufferCaptureDevice</tt>. The <tt>AbstractBufferStream</tt>
     * does not exist at the time of the attempt to set its <tt>Format</tt>.
     * Allows extenders to override the default behavior which is to not attempt
     * to set the specified <tt>Format</tt> so that they can enable setting the
     * <tt>Format</tt> prior to creating the <tt>AbstractBufferStream</tt>. If
     * setting the <tt>Format</tt> of an existing <tt>AbstractBufferStream</tt>
     * is desired, <tt>AbstractBufferStream#doSetFormat(Format)</tt> should be
     * overridden instead.
     *
     * @param streamIndex the zero-based index of the
     * <tt>AbstractBufferStream</tt> the <tt>Format</tt> of which is to be set
     * @param oldValue the last-known <tt>Format</tt> for the
     * <tt>AbstractBufferStream</tt> at the specified <tt>streamIndex</tt>
     * @param newValue the <tt>Format</tt> which is to be set
     * @return the <tt>Format</tt> to be reported by the <tt>FormatControl</tt>
     * of the <tt>AbstractBufferStream</tt> at the specified
     * <tt>streamIndex</tt> in the list of streams of this
     * <tt>AbstractBufferStream</tt> or <tt>null</tt> if the attempt to set the
     * <tt>Format</tt> did not success and any last-known <tt>Format</tt> is to
     * be left in effect
     */
    protected abstract Format setFormat(
            int streamIndex,
            Format oldValue, Format newValue);

    /**
     * Starts the transfer of media data from this
     * <tt>AbstractBufferCaptureDevice</tt>.
     *
     * @throws IOException if anything goes wrong while starting the transfer of
     * media data from this <tt>AbstractBufferCaptureDevice</tt>
     */
    public synchronized void start()
        throws IOException
    {
        if (!started)
        {
            if (!connected)
                throw new IOException(
                        getClass().getSimpleName() + " not connected");

            doStart();
            started = true;
        }
    }

    /**
     * Stops the transfer of media data from this
     * <tt>AbstractBufferCaptureDevice</tt>.
     *
     * @throws IOException if anything goes wrong while stopping the transfer of
     * media data from this <tt>AbstractBufferCaptureDevice</tt>
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

    /**
     * Gets the internal array of <tt>AbstractBufferStream</tt>s through which
     * this <tt>AbstractBufferCaptureDevice</tt> gives access to its media data.
     *
     * @return the internal array of <tt>AbstractBufferStream</tt>s through
     * which this <tt>AbstractBufferCaptureDevice</tt> gives access to its media
     * data
     */
    AbstractBufferStream[] streams()
    {
        return streams;
    }
}
