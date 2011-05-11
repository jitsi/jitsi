/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.control;

import java.awt.*;

import javax.media.control.*;

/**
 * Provides a base implementation of <tt>FrameProcessingControl</tt> which keeps
 * track of the <tt>minimalProcessing</tt> property, switches its value to
 * <tt>true</tt> when it's notified that its owner is at least one frame behind
 * and doesn't implement the <tt>controlComponent</tt> and
 * <tt>framesDropped</tt> properties.
 *
 * @author Lyubomir Marinov
 */
public class FrameProcessingControlImpl
    implements FrameProcessingControl
{

    /**
     * The indicator which determines whether the owner of this
     * <tt>FrameProcessingControl</tt> is to perform only the minimum operations
     * necessary to keep it working normally but without producing output.
     */
    private boolean minimalProcessing = false;

    /**
     * Gets the UI <tt>Component</tt> associated with this <tt>Control</tt>
     * object.
     *
     * @return the UI <tt>Component</tt> associated with this <tt>Control</tt>
     * object
     */
    public Component getControlComponent()
    {
        /*
         * We totally don't care about providing a UI component which controls
         * frame drop from inside the media implementation.
         */
        return null;
    }

    /**
     * Gets the number of output frames that were dropped during processing
     * since the last call to this method.
     *
     * @return the number of output frame that were dropped during processing
     * since the last call to this method
     */
    public int getFramesDropped()
    {
        return 0; // Not implemented.
    }

    /**
     * Determines whether the owner of this <tt>FrameProcessingControl</tt> is
     * to perform only the minimum operations necessary to keep it working
     * normally but without producing output.
     *
     * @return <tt>true</tt> if the owner of this
     * <tt>FrameProcessingControl</tt> is to perform only the minimum operations
     * necessary to keep it working normally but without producing output;
     * otherwise, <tt>false</tt>
     */
    public boolean isMinimalProcessing()
    {
        return minimalProcessing;
    }

    /**
     * Sets the number of frames the owner of this
     * <tt>FrameProcessingControl</tt> is lagging behind. It is a hint to do
     * minimal processing for the next <tt>framesBehind</tt> frames in order to
     * catch up.
     *
     * @param framesBehind the number of frames the owner of this
     * <tt>FrameProcessingControl</tt> is lagging behind
     */
    public void setFramesBehind(float framesBehind)
    {
        setMinimalProcessing(framesBehind > 0);
    }

    /**
     * Sets the indicator which determines whether the owner of this
     * <tt>FrameProcessingControl</tt> is to perform only the minimal operations
     * necessary to keep it working normally but without producing output.
     *
     * @param minimalProcessing <tt>true</tt> if minimal processing mode is to
     * be turned on or <tt>false</tt> if minimal processing mode is to be turned
     * off
     * @return the actual minimal processing mode in effect after the set
     * attempt
     */
    public boolean setMinimalProcessing(boolean minimalProcessing)
    {
        this.minimalProcessing = minimalProcessing;
        return this.minimalProcessing;
    }
}
