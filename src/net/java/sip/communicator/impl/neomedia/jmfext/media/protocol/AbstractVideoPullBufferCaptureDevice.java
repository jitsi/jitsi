/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.jmfext.media.protocol;

import javax.media.*;
import javax.media.control.*;

import net.java.sip.communicator.impl.neomedia.control.*;

/**
 * Provides a base implementation of <tt>PullBufferDataSource</tt> and
 * <tt>CaptureDevice</tt> for the purposes of video in order to facilitate
 * implementers by taking care of boilerplate in the most common cases.
 *
 * @author Lyubomir Marinov
 */
public abstract class AbstractVideoPullBufferCaptureDevice
    extends AbstractPullBufferCaptureDevice
{

    /**
     * Initializes a new <tt>AbstractVideoPullBufferCaptureDevice</tt> instance.
     */
    protected AbstractVideoPullBufferCaptureDevice()
    {
    }

    /**
     * Initializes a new <tt>AbstractVideoPullBufferCaptureDevice</tt> instance
     * from a specific <tt>MediaLocator</tt>.
     *
     * @param locator the <tt>MediaLocator</tt> to create the new instance from
     */
    protected AbstractVideoPullBufferCaptureDevice(MediaLocator locator)
    {
        super(locator);
    }

    /**
     * Creates a new <tt>FrameRateControl</tt> instance which is to allow the
     * getting and setting of the frame rate of this
     * <tt>AbstractVideoPullBufferCaptureDevice</tt>.
     *
     * @return a new <tt>FrameRateControl</tt> instance which is to allow the
     * getting and setting of the frame rate of this
     * <tt>AbstractVideoPullBufferCaptureDevice</tt>
     * @see AbstractPullBufferCaptureDevice#createFrameRateControl()
     */
    @Override
    protected FrameRateControl createFrameRateControl()
    {
        return
            new FrameRateControlAdapter()
            {
                /**
                 * The output frame rate of this
                 * <tt>AbstractVideoPullBufferCaptureDevice</tt>.
                 */
                private float frameRate = -1;

                @Override
                public float getFrameRate()
                {
                    return frameRate;
                }

                @Override
                public float setFrameRate(float frameRate)
                {
                    this.frameRate = frameRate;
                    return this.frameRate;
                }
            };
    }
}
