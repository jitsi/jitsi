/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia;

import javax.media.*;
import javax.media.control.*;
import javax.media.protocol.*;

/**
 * Represents a <code>PushBufferDataSource</code> which is also a
 * <code>CaptureDevice</code> through delegation to a specific
 * <code>CaptureDevice</code>.
 * 
 * @author Lubomir Marinov
 */
public abstract class CaptureDeviceDelegatePushBufferDataSource
    extends PushBufferDataSource
    implements CaptureDevice
{

    /**
     * The <code>CaptureDevice</code> this instance delegates to in order to
     * implement its <code>CaptureDevice</code> functionality.
     */
    private final CaptureDevice captureDevice;

    /**
     * Initializes a new <code>CaptureDeviceDelegatePushBufferDataSource</code>
     * instance which delegates to a specific <code>CaptureDevice</code> in
     * order to implement its <code>CaptureDevice</code> functionality.
     * 
     * @param captureDevice the <code>CaptureDevice</code> the new instance is
     *            to delegate to in order to provide its
     *            <code>CaptureDevice</code> functionality
     */
    public CaptureDeviceDelegatePushBufferDataSource(
        CaptureDevice captureDevice)
    {
        this.captureDevice = captureDevice;
    }

    /*
     * Implements CaptureDevice#getCaptureDeviceInfo(). Delegates to the wrapped
     * CaptureDevice if available; otherwise, returns null.
     */
    public CaptureDeviceInfo getCaptureDeviceInfo()
    {
        return
            (captureDevice != null)
                ? captureDevice.getCaptureDeviceInfo()
                : null;
    }

    /*
     * Implements CaptureDevice#getFormatControls(). Delegates to the wrapped
     * CaptureDevice if available; otherwise, returns an empty array of
     * FormatControl.
     */
    public FormatControl[] getFormatControls()
    {
        return
            (captureDevice != null)
                ? captureDevice.getFormatControls()
                : new FormatControl[0];
    }
}
