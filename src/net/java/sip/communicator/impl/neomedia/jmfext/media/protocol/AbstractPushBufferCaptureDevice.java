/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.jmfext.media.protocol;

import java.io.*;
import java.util.*;

import javax.media.*;
import javax.media.control.*;
import javax.media.protocol.*;

/**
 * Provides a base implementation of <tt>PushBufferDataSource</tt> and
 * <tt>CaptureDevice</tt> in order to facilitate implementers by taking care of
 * boilerplate in the most common cases.
 *
 * @author Lyubomir Marinov
 */
public abstract class AbstractPushBufferCaptureDevice
    extends PushBufferDataSource
    implements CaptureDevice
{
    /**
     * The <tt>AbstractBufferCaptureDevice</tt> which provides the
     * implementation of this <tt>AbstractPushBufferCaptureDevice</tt>.
     */
    private final AbstractBufferCaptureDevice<AbstractPushBufferStream> impl
        = new AbstractBufferCaptureDevice<AbstractPushBufferStream>()
                {
                    protected AbstractPushBufferStream createStream(
                            int streamIndex,
                            FormatControl formatControl)
                    {
                        return
                            AbstractPushBufferCaptureDevice.this.createStream(
                                    streamIndex,
                                    formatControl);
                    }

                    protected void doConnect()
                        throws IOException
                    {
                        AbstractPushBufferCaptureDevice.this.doConnect();
                    }

                    protected void doDisconnect()
                    {
                        AbstractPushBufferCaptureDevice.this.doDisconnect();
                    }

                    protected void doStart()
                        throws IOException
                    {
                        AbstractPushBufferCaptureDevice.this.doStart();
                    }

                    protected void doStop()
                        throws IOException
                    {
                        AbstractPushBufferCaptureDevice.this.doStop();
                    }

                    public CaptureDeviceInfo getCaptureDeviceInfo()
                    {
                        return
                            AbstractPushBufferCaptureDevice.this
                                    .getCaptureDeviceInfo();
                    }

                    protected Format getFormat(int streamIndex, Format oldValue)
                    {
                        return
                            AbstractPushBufferCaptureDevice.this.getFormat(
                                    streamIndex,
                                    oldValue);
                    }

                    protected Format[] getSupportedFormats(int streamIndex)
                    {
                        return
                            AbstractPushBufferCaptureDevice.this
                                    .getSupportedFormats(streamIndex);
                    }

                    protected Format setFormat(
                            int streamIndex,
                            Format oldValue, Format newValue)
                    {
                        return
                            AbstractPushBufferCaptureDevice.this.setFormat(
                                    streamIndex,
                                    oldValue, newValue);
                    }
                };

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
     * Opens a connection to the media source specified by the
     * <tt>MediaLocator</tt> of this <tt>DataSource</tt>.
     *
     * @throws IOException if anything goes wrong while opening the connection
     * to the media source specified by the <tt>MediaLocator</tt> of this
     * <tt>DataSource</tt>
     */
    public void connect()
        throws IOException
    {
        impl.connect();
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
    public void disconnect()
    {
        impl.disconnect();
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
    protected void doConnect()
        throws IOException
    {
    }

    /**
     * Closes the connection to the media source specified by the
     * <tt>MediaLocator</tt> of this <tt>DataSource</tt>. Allows extenders to
     * override and be sure that there will be no request to close a connection
     * if the connection has not been opened yet.
     */
    protected void doDisconnect()
    {
        /*
         * While it is not clear whether the streams can be released upon
         * disconnect, com.imb.media.protocol.SuperCloneableDataSource gets the
         * streams of the DataSource it adapts (i.e. this DataSource when
         * SourceCloneable support is to be created for it) before #connect().
         * Unfortunately, it means that it isn't clear when the streams are to
         * be disposed.
         */
    }

    /**
     * Starts the transfer of media data from this <tt>DataSource</tt>. Allows
     * extenders to override and be sure that there will be no request to start
     * the transfer of media data if it has already been started.
     *
     * @throws IOException if anything goes wrong while starting the transfer of
     * media data from this <tt>DataSource</tt>
     */
    protected void doStart()
        throws IOException
    {
        impl.defaultDoStart();
    }

    /**
     * Stops the transfer of media data from this <tt>DataSource</tt>. Allows
     * extenders to override and be sure that there will be no request to stop
     * the transfer of media data if it has not been started yet.
     *
     * @throws IOException if anything goes wrong while stopping the transfer of
     * media data from this <tt>DataSource</tt>
     */
    protected void doStop()
        throws IOException
    {
        impl.defaultDoStop();
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
            if (captureDeviceInfo.getLocator().toString().equals(
                    locator.toString()))
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
        return impl.getControl(controlType);
    }

    /**
     * Implements {@link javax.media.protocol.DataSource#getControls()}. Gets
     * the controls available for this instance.
     *
     * @return an array of <tt>Object</tt>s which represent the controls
     * available for this instance
     */
    public Object[] getControls()
    {
        return impl.getControls();
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
     * Gets the <tt>Format</tt> to be reported by the <tt>FormatControl</tt> of
     * a <tt>PushBufferStream</tt> at a specific zero-based index in the list of
     * streams of this <tt>PushBufferDataSource</tt>. The
     * <tt>PushBufferStream</tt> may not exist at the time of requesting its
     * <tt>Format</tt>. Allows extenders to override the default behavior which
     * is to report any last-known format or the first <tt>Format</tt> from the
     * list of supported formats as defined in the JMF registration of this
     * <tt>CaptureDevice</tt>.
     *
     * @param streamIndex the zero-based index of the <tt>PushBufferStream</tt>
     * the <tt>Format</tt> of which is to be retrieved
     * @param oldValue the last-known <tt>Format</tt> for the
     * <tt>PushBufferStream</tt> at the specified <tt>streamIndex</tt>
     * @return the <tt>Format</tt> to be reported by the <tt>FormatControl</tt>
     * of the <tt>PushBufferStream</tt> at the specified <tt>streamIndex</tt> in
     * the list of streams of this <tt>PushBufferDataSource</tt>.
     */
    protected Format getFormat(int streamIndex, Format oldValue)
    {
        return impl.defaultGetFormat(streamIndex, oldValue);
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
        return impl.getFormatControls();
    }

    /**
     * Gets the <tt>Object</tt> which is to synchronize the access to
     * {@link #streams()} and its return value.
     *
     * @return the <tt>Object</tt> which is to synchronize the access to
     * {@link #streams()} and its return value 
     */
    protected Object getStreamSyncRoot()
    {
        return impl.getStreamSyncRoot();
    }

    /**
     * Gets the <tt>PushBufferStream</tt>s through which this
     * <tt>PushBufferDataSource</tt> gives access to its media data.
     *
     * @return an array of the <tt>PushBufferStream</tt>s through which this
     * <tt>PushBufferDataSource</tt> gives access to its media data
     */
    public PushBufferStream[] getStreams()
    {
        return impl.getStreams(AbstractPushBufferStream.class);
    }

    /**
     * Gets the <tt>Format</tt>s which are to be reported by a
     * <tt>FormatControl</tt> as supported formats for a
     * <tt>PushBufferStream</tt> at a specific zero-based index in the list of
     * streams of this <tt>PushBufferDataSource</tt>.
     *
     * @param streamIndex the zero-based index of the <tt>PushBufferStream</tt>
     * for which the specified <tt>FormatControl</tt> is to report the list of
     * supported <tt>Format</tt>s
     * @return an array of <tt>Format</tt>s to be reported by a
     * <tt>FormatControl</tt> as the supported formats for the
     * <tt>PushBufferStream</tt> at the specified <tt>streamIndex</tt> in the
     * list of streams of this <tt>PushBufferDataSource</tt>
     */
    protected Format[] getSupportedFormats(int streamIndex)
    {
        return impl.defaultGetSupportedFormats(streamIndex);
    }

    /**
     * Attempts to set the <tt>Format</tt> to be reported by the
     * <tt>FormatControl</tt> of a <tt>PushBufferStream</tt> at a specific
     * zero-based index in the list of streams of this
     * <tt>PushBufferDataSource</tt>. The <tt>PushBufferStream</tt> does not
     * exist at the time of the attempt to set its <tt>Format</tt>. Allows
     * extenders to override the default behavior which is to not attempt to set
     * the specified <tt>Format</tt> so that they can enable setting the
     * <tt>Format</tt> prior to creating the <tt>PushBufferStream</tt>. If
     * setting the <tt>Format</tt> of an existing <tt>PushBufferStream</tt> is
     * desired, <tt>AbstractPushBufferStream#doSetFormat(Format)</tt> should be
     * overridden instead.
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
    protected Format setFormat(
            int streamIndex,
            Format oldValue, Format newValue)
    {
        return oldValue;
    }

    /**
     * Starts the transfer of media data from this <tt>DataSource</tt>.
     *
     * @throws IOException if anything goes wrong while starting the transfer of
     * media data from this <tt>DataSource</tt>
     */
    public void start()
        throws IOException
    {
        impl.start();
    }

    /**
     * Stops the transfer of media data from this <tt>DataSource</tt>.
     *
     * @throws IOException if anything goes wrong while stopping the transfer of
     * media data from this <tt>DataSource</tt>
     */
    public void stop()
        throws IOException
    {
        impl.stop();
    }

    /**
     * Gets the internal array of <tt>AbstractPushBufferStream</tt>s through
     * which this <tt>AbstractPushBufferCaptureDevice</tt> gives access to its
     * media data.
     * 
     * @return the internal array of <tt>AbstractPushBufferStream</tt>s through
     * which this <tt>AbstractPushBufferCaptureDevice</tt> gives access to its
     * media data
     */
    protected AbstractBufferStream[] streams()
    {
        return impl.streams();
    }
}
