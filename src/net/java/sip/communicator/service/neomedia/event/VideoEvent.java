/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.neomedia.event;

import java.awt.*;
import java.util.*;

/**
 * Represents an event fired by providers of visual <tt>Component</tt>s
 * depicting video to notify about changes in the availability of such
 * <tt>Component</tt>s.
 *
 * @author Lubomir Marinov
 */
public class VideoEvent
    extends EventObject
{

    /**
     * The video origin of a <tt>VideoEvent</tt> which is local to the executing
     * client such as a local video capture device.
     */
    public static final int LOCAL = 1;

    /**
     * The video origin of a <tt>VideoEvent</tt> which is remote to the
     * executing client such as a video being remotely streamed from a
     * <tt>CallPeer</tt>.
     */
    public static final int REMOTE = 2;

    /**
     * The type of a <tt>VideoEvent</tt> which notifies about a specific visual
     * <tt>Component</tt> depicting video being made available by the firing
     * provider.
     */
    public static final int VIDEO_ADDED = 1;

    /**
     * The type of a <tt>VideoEvent</tt> which notifies about a specific visual
     * <tt>Component</tt> depicting video no longer being made available by the
     * firing provider.
     */
    public static final int VIDEO_REMOVED = 2;

    /**
     * The indicator which determines whether this event and, more specifically,
     * the visual <tt>Component</tt> it describes have been consumed and should
     * be considered owned, referenced (which is important because
     * <tt>Component</tt>s belong to a single <tt>Container</tt> at a time).
     */
    private boolean consumed;

    /**
     * The origin of the video this <tt>VideoEvent</tt> notifies about which is
     * one of {@link #LOCAL} and {@link #REMOTE}.
     */
    private final int origin;

    /**
     * The type of availability change this <tt>VideoEvent</tt> notifies about
     * which is one of {@link #VIDEO_ADDED} and {@link #VIDEO_REMOVED}.
     */
    private final int type;

    /**
     * The visual <tt>Component</tt> depicting video which had its availability
     * changed and which this <tt>VideoEvent</tt> notifies about.
     */
    private final Component visualComponent;

    /**
     * Initializes a new <tt>VideoEvent</tt> which is to notify about a specific
     * change in the availability of a specific visual <tt>Component</tt>
     * depicting video and being provided by a specific source.
     *
     * @param source the source of the new <tt>VideoEvent</tt> and the provider
     * of the visual <tt>Component</tt> depicting video
     * @param type the type of the availability change which has caused the new
     * <tt>VideoEvent</tt> to be fired
     * @param visualComponent the visual <tt>Component</tt> depicting video
     * which had its availability in the <tt>source</tt> provider changed
     * @param origin the origin of the video the new <tt>VideoEvent</tt> is to
     * notify about
     */
    public VideoEvent(
            Object source,
            int type,
            Component visualComponent,
            int origin)
    {
        super(source);

        this.type = type;
        this.visualComponent = visualComponent;
        this.origin = origin;
    }

    /**
     * Consumes this event and, more specifically, marks the <tt>Component</tt>
     * it describes as owned, referenced in order to let other potential
     * consumers know about its current ownership status (which is important
     * because <tt>Component</tt>s belong to a single <tt>Container</tt> at a
     * time).
     */
    public void consume()
    {
        consumed = true;
    }

    /**
     * Gets the origin of the video this <tt>VideoEvent</tt> notifies about
     * which is one of {@link #LOCAL} and {@link #REMOTE}.
     *
     * @return one of {@link #LOCAL} and {@link #REMOTE} which specifies the
     * origin of the video this <tt>VideoEvent</tt> notifies about
     */
    public int getOrigin()
    {
        return origin;
    }

    /**
     * Gets the type of availability change this <tt>VideoEvent</tt> notifies
     * about which is one of {@link #VIDEO_ADDED} and {@link #VIDEO_REMOVED}.
     *
     * @return one of {@link #VIDEO_ADDED} and {@link #VIDEO_REMOVED} which
     * describes the type of availability change this <tt>VideoEvent</tt>
     * notifies about
     */
    public int getType()
    {
        return type;
    }

    /**
     * Gets the visual <tt>Component</tt> depicting video which had its
     * availability changed and which this <tt>VideoEvent</tt> notifies about.
     *
     * @return the visual <tt>Component</tt> depicting video which had its
     * availability changed and which this <tt>VideoEvent</tt> notifies about
     */
    public Component getVisualComponent()
    {
        return visualComponent;
    }

    /**
     * Determines whether this event and, more specifically, the visual
     * <tt>Component</tt> it describes have been consumed and should be
     * considered owned, referenced (which is important because
     * <tt>Component</tt>s belong to a single <tt>Container</tt> at a time).
     *
     * @return <tt>true</tt> if this event and, more specifically, the visual
     * <tt>Component</tt> it describes have been consumed and should be
     * considered owned, referenced (which is important because
     * <tt>Component</tt>s belong to a single <tt>Container</tt> at a time);
     * otherwise, <tt>false</tt>
     */
    public boolean isConsumed()
    {
        return consumed;
    }

    /**
     * Returns a human-readable representation of a specific <tt>VideoEvent</tt>
     * origin constant in the form of a <tt>String</tt> value.
     *
     * @param origin one of the <tt>VideoEvent</tt> origin constants such as
     * {@link #LOCAL} or {@link #REMOTE}
     * @return a <tt>String</tt> value which gives a human-readable
     * representation of the specified <tt>VideoEvent</tt> <tt>origin</tt>
     * constant
     */
    public static String originToString(int origin)
    {
        switch (origin)
        {
        case VideoEvent.LOCAL:
            return "LOCAL";
        case VideoEvent.REMOTE:
            return "REMOTE";
        default:
            throw new IllegalArgumentException("origin");
        }
    }

    /**
     * Returns a human-readable representation of a specific <tt>VideoEvent</tt>
     * type constant in the form of a <tt>String</tt> value.
     *
     * @param type one of the <tt>VideoEvent</tt> type constants such as
     * {@link #VIDEO_ADDED} or {@link #VIDEO_REMOVED}
     * @return a <tt>String</tt> value which gives a human-readable
     * representation of the specified <tt>VideoEvent</tt> <tt>type</tt>
     * constant
     */
    public static String typeToString(int type)
    {
        switch (type)
        {
        case VideoEvent.VIDEO_ADDED:
            return "VIDEO_ADDED";
        case VideoEvent.VIDEO_REMOVED:
            return "VIDEO_REMOVED";
        default:
            throw new IllegalArgumentException("type");
        }
    }
}
