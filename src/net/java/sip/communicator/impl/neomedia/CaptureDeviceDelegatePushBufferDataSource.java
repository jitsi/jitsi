/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia;

import java.io.*;

import javax.media.*;
import javax.media.control.*;
import javax.media.protocol.*;

/**
 * Represents a <tt>PushBufferDataSource</tt> which is also a
 * <tt>CaptureDevice</tt> through delegation to a specific
 * <tt>CaptureDevice</tt>.
 * 
 * @author Lubomir Marinov
 */
public class CaptureDeviceDelegatePushBufferDataSource
    extends PushBufferDataSource
    implements CaptureDevice
{
    /**
     * The constant which represents an empty array with <tt>Object</tt> element
     * type giving no controls for a <tt>DataSource</tt>. Explicitly defined in
     * order to reduce unnecessary allocations.
     */
    protected static final Object[] EMPTY_CONTROLS = new Object[0];

    /**
     * The constant which represents an empty array with
     * <tt>PushBufferStream</tt> element type. Explicitly defined in order to
     * reduce unnecessary allocations.
     */
    protected static final PushBufferStream[] EMPTY_STREAMS
        = new PushBufferStream[0];

    /**
     * The <tt>CaptureDevice</tt> this instance delegates to in order to
     * implement its <tt>CaptureDevice</tt> functionality.
     */
    protected final CaptureDevice captureDevice;

    /**
     * Initializes a new <tt>CaptureDeviceDelegatePushBufferDataSource</tt>
     * instance which delegates to a specific <tt>CaptureDevice</tt> in order to
     * implement its <tt>CaptureDevice</tt> functionality.
     * 
     * @param captureDevice the <tt>CaptureDevice</tt> the new instance is to
     * delegate to in order to provide its <tt>CaptureDevice</tt> functionality
     */
    public CaptureDeviceDelegatePushBufferDataSource(
        CaptureDevice captureDevice)
    {
        this.captureDevice = captureDevice;
    }

    /**
     * Implements {@link CaptureDevice#connect()}. Delegates to the wrapped
     * <tt>CaptureDevice</tt> if available; otherwise, does nothing.
     *
     * @throws IOException if the wrapped <tt>CaptureDevice</tt> throws such an
     * exception
     */
    public void connect()
        throws IOException
    {
        if (captureDevice != null)
            captureDevice.connect();
    }

    /**
     * Implements {@link CaptureDevice#disconnect()}. Delegates to the wrapped
     * <tt>CaptureDevice</tt> if available; otherwise, does nothing.
     */
    public void disconnect()
    {
        if (captureDevice != null)
            captureDevice.disconnect();
    }

    /**
     * Implements {@link CaptureDevice#getCaptureDeviceInfo()}. Delegates to the
     * wrapped <tt>CaptureDevice</tt> if available; otherwise, returns
     * <tt>null</tt>.
     *
     * @return the <tt>CaptureDeviceInfo</tt> of the wrapped
     * <tt>CaptureDevice</tt> if available; otherwise, <tt>null</tt>
     */
    public CaptureDeviceInfo getCaptureDeviceInfo()
    {
        return
            (captureDevice != null)
                ? captureDevice.getCaptureDeviceInfo()
                : null;
    }

    /**
     * Implements {@link DataSource#getContentType()}. Delegates to the wrapped
     * <tt>CaptureDevice</tt> if it implements <tt>DataSource</tt>; otherwise,
     * returns {@link ContentDescriptor#CONTENT_UNKNOWN}.
     *
     * @return a <tt>String</tt> value which describes the content type of the
     * wrapped <tt>CaptureDevice</tt> if it implements <tt>DataSource</tt>;
     * otherwise, <tt>ContentDescriptor#CONTENT_UNKNOWN</tt>
     */
    public String getContentType()
    {
        if (captureDevice instanceof DataSource)
            return ((DataSource) captureDevice).getContentType();
        return ContentDescriptor.CONTENT_UNKNOWN;
    }

    /**
     * Implements {@link DataSource#getControl(String)}. Delegates to the
     * wrapped <tt>CaptureDevice</tt> if it implements <tt>DataSource</tt>;
     * otherwise, returns <tt>null</tt>.
     *
     * @param controlType a <tt>String</tt> value which names the type of the
     * control to be retrieved
     * @return an <tt>Object</tt> which represents the control of the requested
     * <tt>controlType</tt> of the wrapped <tt>CaptureDevice</tt> if it
     * implements <tt>DataSource</tt>; otherwise, <tt>null</tt>
     */
    public Object getControl(String controlType)
    {
        if (captureDevice instanceof DataSource)
            return ((DataSource) captureDevice).getControl(controlType);
        return null;
    }

    /**
     * Implements {@link DataSource#getControls()}. Delegates to the wrapped
     * <tt>CaptureDevice</tt> if it implements <tt>DataSource</tt>; otherwise,
     * returns an empty array with <tt>Object</tt> element type.
     *
     * @return the array of controls for the wrapped <tt>CaptureDevice</tt> if
     * it implements <tt>DataSource</tt>; otherwise, an empty array with
     * <tt>Object</tt> element type
     */
    public Object[] getControls()
    {
        if (captureDevice instanceof DataSource)
            return ((DataSource) captureDevice).getControls();
        return EMPTY_CONTROLS;
    }

    /**
     * Implements {@link DataSource#getDuration()}. Delegates to the wrapped
     * <tt>CaptureDevice</tt> if it implements <tt>DataSource</tt>; otherwise,
     * returns {@link DataSource#DURATION_UNKNOWN}.
     *
     * @return the duration of the wrapped <tt>CaptureDevice</tt> as returned by
     * its implementation of <tt>DataSource</tt> if any; otherwise, returns
     * <tt>DataSource#DURATION_UNKNOWN</tt>
     */
    public Time getDuration()
    {
        if (captureDevice instanceof DataSource)
            return ((DataSource) captureDevice).getDuration();
        return DataSource.DURATION_UNKNOWN;
    }

    /**
     * Implements {@link CaptureDevice#getFormatControls()}. Delegates to the
     * wrapped <tt>CaptureDevice</tt> if available; otherwise, returns an empty
     * array with <tt>FormatControl</tt> element type.
     *
     * @return the array of <tt>FormatControl</tt>s of the wrapped
     * <tt>CaptureDevice</tt> if available; otherwise, an empty array with
     * <tt>FormatControl</tt> element type
     */
    public FormatControl[] getFormatControls()
    {
        return
            (captureDevice != null)
                ? captureDevice.getFormatControls()
                : new FormatControl[0];
    }

    /**
     * Implements {@link PushBufferDataSource#getStreams()}. Delegates to the
     * wrapped <tt>CaptureDevice</tt> if it implements
     * <tt>PushBufferDataSource</tt>; otherwise, returns an empty array with
     * <tt>PushBufferStream</tt> element type.
     *
     * @return an array of <tt>PushBufferStream</tt>s as returned by the wrapped
     * <tt>CaptureDevice</tt> if it implements <tt>PushBufferDataSource</tt>;
     * otherwise, an empty array with <tt>PushBufferStream</tt> element type
     */
    public PushBufferStream[] getStreams()
    {
        if (captureDevice instanceof PushBufferDataSource)
            return ((PushBufferDataSource) captureDevice).getStreams();
        return EMPTY_STREAMS;
    }

    /**
     * Implements {@link CaptureDevice#start()}. Delegates to the wrapped
     * <tt>CaptureDevice</tt> if available; otherwise, does nothing.
     *
     * @throws IOException if the wrapped <tt>CaptureDevice</tt> throws such an
     * exception
     */
    public void start()
        throws IOException
    {
        if (captureDevice != null)
            captureDevice.start();
    }

    /**
     * Implements {@link CaptureDevice#stop()}. Delegates to the wrapped
     * <tt>CaptureDevice</tt> if available; otherwise, does nothing.
     *
     * @throws IOException if the wrapped <tt>CaptureDevice</tt> throws such an
     * exception
     */
    public void stop()
        throws IOException
    {
        if (captureDevice != null)
            captureDevice.stop();
    }
}
