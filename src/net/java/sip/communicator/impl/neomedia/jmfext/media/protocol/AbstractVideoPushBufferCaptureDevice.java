/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.jmfext.media.protocol;

import java.util.*;

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
     * The <tt>FrameRateControl</tt>s of this
     * <tt>AbstractVideoPushBufferCaptureDevice</tt>.
     */
    private FrameRateControl[] frameRateControls;

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
     */
    protected FrameRateControl createFrameRateControl()
    {
        return null;
    }

    /**
     * Implements {@link javax.media.protocol.DataSource#getControls()}. Gets
     * the controls available for this instance.
     *
     * @return an array of <tt>Object</tt>s which represent the controls
     * available for this instance
     */
    @Override
    public Object[] getControls()
    {
        List<Object> controls = new ArrayList<Object>();

        Collections.addAll(controls, super.getControls());

        /*
         * Add any FrameRateControl that this AbstractPushBufferCaptureDevice
         * may want to have.
         */
        synchronized (this)
        {
            if (frameRateControls == null)
            {
                FrameRateControl frameRateControl = createFrameRateControl();

                // Don't try to create the FrameRateControl more than once.
                if (frameRateControl == null)
                    frameRateControls = new FrameRateControl[0];
                else
                    frameRateControls
                        = new FrameRateControl[] { frameRateControl };
            }
            if (frameRateControls != null)
                Collections.addAll(controls, frameRateControls);
        }

        return controls.toArray();
    }
}
