/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.jmfext.media.protocol;

import javax.media.*;
import javax.media.control.*;

/**
 * Provides a base implementation of <tt>PushBufferDataSource</tt> and
 * <tt>CaptureDevice</tt> for the purposes of video in order to facilitate
 * implementers by taking care of boilerplate in the most common cases.
 *
 * @author Lyubomir Marinov
 */
public abstract class AbstractVideoPushBufferCaptureDevice
    extends AbstractPushBufferCaptureDevice
{

    /**
     * Initializes a new <tt>AbstractVideoPushBufferCaptureDevice</tt> instance.
     */
    protected AbstractVideoPushBufferCaptureDevice()
    {
    }

    /**
     * Initializes a new <tt>AbstractVideoPushBufferCaptureDevice</tt> instance
     * from a specific <tt>MediaLocator</tt>.
     *
     * @param locator the <tt>MediaLocator</tt> to create the new instance from
     */
    protected AbstractVideoPushBufferCaptureDevice(MediaLocator locator)
    {
        super(locator);
    }

    /**
     * Creates a new <tt>FrameRateControl</tt> instance which is to allow the
     * getting and setting of the frame rate of this
     * <tt>AbstractVideoPushBufferCaptureDevice</tt>.
     *
     * @return a new <tt>FrameRateControl</tt> instance which is to allow the
     * getting and setting of the frame rate of this
     * <tt>AbstractVideoPushBufferCaptureDevice</tt>
     * @see AbstractPushBufferCaptureDevice#createFrameRateControl()
     */
    @Override
    protected FrameRateControl createFrameRateControl()
    {
        return null;
    }
}
