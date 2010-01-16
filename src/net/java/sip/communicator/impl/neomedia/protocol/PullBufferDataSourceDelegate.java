/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.protocol;

import java.io.*;

import javax.media.*;
import javax.media.protocol.*;

/**
 * Implements most of <tt>PullBufferDataSource</tt> for a particular
 * <tt>DataSource</tt> and requires extenders to only implement
 * {@link PullBufferDataSource#getStreams()}. Intended to allow easier
 * overriding of the streams returned by a <tt>DataSource</tt>.
 *
 * @param <T> the very type of <tt>DataSource</tt> to be wrapped in a
 * <tt>PullBufferDataSourceDelegate</tt>
 *
 * @author Damian Minkov
 */
public abstract class PullBufferDataSourceDelegate<T extends DataSource>
    extends CaptureDeviceDelegatePullBufferDataSource
{
    /**
     * The wrapped <tt>DataSource</tt> this instance delegates to.
     */
    protected final T dataSource;

    /**
     * Initializes a new <tt>PullBufferDataSourceDelegate</tt> which is to
     * delegate to a specific <tt>DataSource</tt>.
     *
     * @param dataSource the <tt>DataSource</tt> the new instance is to delegate
     * to
     */
    public PullBufferDataSourceDelegate(T dataSource)
    {
        super(
            (dataSource instanceof CaptureDevice)
                ? (CaptureDevice) dataSource
                : null);

        if (dataSource == null)
            throw new NullPointerException("dataSource");

        this.dataSource = dataSource;
    }

    /**
     * Implements {@link DataSource#connect()}. Delegates to the wrapped
     * <tt>DataSource</tt>. Overrides
     * {@link CaptureDeviceDelegatePullBufferDataSource#connect()} because the
     * wrapped <tt>DataSource</tt> may not be a <tt>CaptureDevice</tt> yet it
     * still needs to be connected.
     *
     * @throws IOException if the wrapped <tt>DataSource</tt> throws such an
     * exception
     */
    @Override
    public void connect()
        throws IOException
    {
        dataSource.connect();
    }

    /**
     * Implements {@link DataSource#disconnect()}. Delegates to the wrapped
     * <tt>DataSource</tt>. Overrides
     * {@link CaptureDeviceDelegatePullBufferDataSource#disconnect()} because
     * the wrapped <tt>DataSource</tt> may not be a <tt>CaptureDevice</tt> yet
     * it still needs to be disconnected.
     */
    @Override
    public void disconnect()
    {
        dataSource.disconnect();
    }

    /**
     * Implements {@link DataSource#getContentType()}. Delegates to the wrapped
     * <tt>DataSource</tt>. Overrides
     * {@link CaptureDeviceDelegatePullBufferDataSource#getContentType()}
     * because the wrapped <tt>DataSource</tt> may not be a
     * <tt>CaptureDevice</tt> yet it still needs to report the content type.
     *
     * @return a <tt>String</tt> value which describes the content type of the
     * wrapped <tt>DataSource</tt>
     */
    @Override
    public String getContentType()
    {
        return dataSource.getContentType();
    }

    /**
     * Implements {@link DataSource#getLocator()}. Delegates to the wrapped
     * <tt>DataSource</tt>. 
     *
     * @return a <tt>MediaLocator</tt> value which describes the locator of the
     * wrapped <tt>DataSource</tt>
     */
    @Override
    public MediaLocator getLocator()
    {
        return dataSource.getLocator();
    }

    /**
     * Implements {@link DataSource#getControl(String)}. Delegates to the
     * wrapped <tt>DataSource</tt>. Overrides
     * {@link CaptureDeviceDelegatePullBufferDataSource#getControl(String)}
     * because the wrapped <tt>DataSource</tt> may not be a
     * <tt>CaptureDevice</tt> yet it still needs to give access to the control.
     *
     * @param controlType a <tt>String</tt> value which names the type of the
     * control to be retrieved
     * @return an <tt>Object</tt> which represents the control of the requested
     * <tt>controlType</tt> of the wrapped <tt>DataSource</tt>
     */
    @Override
    public Object getControl(String controlType)
    {
        return dataSource.getControl(controlType);
    }

    /**
     * Implements {@link DataSource#getControls()}. Delegates to the wrapped
     * <tt>PullBufferDataSource</tt>. Overrides
     * {@link CaptureDeviceDelegatePullBufferDataSource#getControls()} because
     * the wrapped <tt>DataSource</tt> may not be a <tt>CaptureDevice</tt> yet
     * it still needs to give access to the controls.
     *
     * @return an array of <tt>Objects</tt> which represent the controls of the
     * wrapped <tt>DataSource</tt>
     */
    @Override
    public Object[] getControls()
    {
        return dataSource.getControls();
    }

    /**
     * Gets the <tt>DataSource</tt> wrapped by this instance.
     *
     * @return the <tt>DataSource</tt> wrapped by this instance
     */
    public T getDataSource()
    {
        return dataSource;
    }

    /**
     * Implements {@link DataSource#getDuration()}. Delegates to the wrapped
     * <tt>DataSource</tt>. Overrides
     * {@link CaptureDeviceDelegatePullBufferDataSource#getDuration()} because
     * the wrapped <tt>DataSource</tt> may not be a <tt>CaptureDevice</tt> yet
     * it still needs to report the duration.
     *
     * @return the duration of the wrapped <tt>DataSource</tt>
     */
    @Override
    public Time getDuration()
    {
        return dataSource.getDuration();
    }

    /**
     * Gets the <tt>PullBufferStream</tt>s through which this
     * <tt>PullBufferDataSource</tt> gives access to its media data.
     *
     * @return an array of <tt>PullBufferStream</tt>s through which this
     * <tt>PullBufferDataSource</tt> gives access to its media data
     */
    @Override
    public abstract PullBufferStream[] getStreams();

    /**
     * Implements {@link DataSource#start()}. Delegates to the wrapped
     * <tt>DataSource</tt>. Overrides
     * {@link CaptureDeviceDelegatePullBufferDataSource#start()} because the
     * wrapped <tt>DataSource</tt> may not be a <tt>CaptureDevice</tt> yet it
     * still needs to be started.
     *
     * @throws IOException if the wrapped <tt>DataSource</tt> throws such an
     * exception
     */
    @Override
    public void start()
        throws IOException
    {
        dataSource.start();
    }

    /**
     * Implements {@link DataSource#stop()}. Delegates to the wrapped
     * <tt>DataSource</tt>. Overrides
     * {@link CaptureDeviceDelegatePullBufferDataSource#stop()} because the
     * wrapped <tt>DataSource</tt> may not be a <tt>CaptureDevice</tt> yet it
     * still needs to be stopped.
     *
     * @throws IOException if the wrapped <tt>DataSource</tt> throws such an
     * exception
     */
    @Override
    public void stop()
        throws IOException
    {
        dataSource.stop();
    }
}
