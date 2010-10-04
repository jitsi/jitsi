/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.event;

import java.awt.*;

/**
 * Represents a <tt>VideoEvent</tt> which notifies about an update to the size
 * of a specific visual <tt>Component</tt> depicting video.
 *
 * @author Lubomir Marinov
 */
public class SizeChangeVideoEvent
    extends VideoEvent
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The type of a <tt>VideoEvent</tt> which notifies about an update to the
     * size of a specific visual <tt>Component</tt> depicting video.
     */
    public static final int VIDEO_SIZE_CHANGE = 3;

    /**
     * The new height of the associated visual <tt>Component</tt>.
     */
    private final int height;

    /**
     * The new width of the associated visual <tt>Component</tt>.
     */
    private final int width;

    /**
     * Initializes a new <tt>SizeChangeVideoEvent</tt> which is to notify about
     * an update to the size of a specific visual <tt>Component</tt> depicting
     * video.
     *
     * @param source the source of the new <tt>SizeChangeVideoEvent</tt>
     * @param visualComponent the visual <tt>Component</tt> depicting video
     * with the updated size
     * @param origin the origin of the video the new
     * <tt>SizeChangeVideoEvent</tt> is to notify about
     * @param width the new width of <tt>visualComponent</tt>
     * @param height the new height of <tt>visualComponent</tt>
     */
    public SizeChangeVideoEvent(
            Object source,
            Component visualComponent,
            int origin,
            int width,
            int height)
    {
        super(source, VIDEO_SIZE_CHANGE, visualComponent, origin);

        this.width = width;
        this.height = height;
    }

    /**
     * Gets the new height of the associated visual <tt>Component</tt>.
     *
     * @return the new height of the associated visual <tt>Component</tt>
     */
    public int getHeight()
    {
        return height;
    }

    /**
     * Gets the new width of the associated visual <tt>Component</tt>.
     *
     * @return the new width of the associated visual <tt>Component</tt>
     */
    public int getWidth()
    {
        return width;
    }
}
