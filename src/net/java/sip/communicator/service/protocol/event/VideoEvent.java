/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.event;

import java.awt.*;
import java.util.*;

/**
 * Represents an event fired by providers of visual <code>Component</code>s
 * depicting video to notify about changes in the availability of such
 * <code>Component</code>s.
 *
 * @author Lubomir Marinov
 */
public class VideoEvent
    extends EventObject
{

    /**
     * The video origin of a <code>VideoEvent</code> which is local to the
     * executing client such as a local video capture device.
     */
    public static final int LOCAL = 1;

    /**
     * The video origin of a <code>VideoEvent</code> which is remote to the
     * executing client such as a video being remotely streamed from a
     * <code>CallPeer</code>.
     */
    public static final int REMOTE = 2;

    /**
     * The type of a <code>VideoEvent</code> which notifies about a specific
     * visual <code>Component</code> depicting video being made available by the
     * firing provider.
     */
    public static final int VIDEO_ADDED = 1;

    /**
     * The type of a <code>VideoEvent</code> which notifies about a specific
     * visual <code>Component</code> depicting video no longer being made
     * available by the firing provider.
     */
    public static final int VIDEO_REMOVED = 2;

    /**
     * The indicator which determines whether this event and, more specifically,
     * the visual <code>Component</code> it describes have been consumed and
     * should be considered owned, referenced (which is important because
     * <code>Component</code>s belong to a single <code>Container</code> at a
     * time).
     */
    private boolean consumed;

    /**
     * The origin of the video this <code>VideoEvent</code> notifies about which
     * is one of {@link #LOCAL} and {@link #REMOTE}.
     */
    private final int origin;

    /**
     * The type of availability change this <code>VideoEvent</code> notifies
     * about which is one of {@link #VIDEO_ADDED} and {@link #VIDEO_REMOVED}.
     */
    private final int type;

    /**
     * The visual <code>Component</code> depicting video which had its
     * availability changed and which this <code>VideoEvent</code> notifies
     * about.
     */
    private final Component visualComponent;

    /**
     * Initializes a new <code>VideoEvent</code> which is to notify about a
     * specific change in the availability of a specific visual
     * <code>Component</code> depicting video and being provided by a specific
     * source.
     *
     * @param source the source of the new <code>VideoEvent</code> and the
     *            provider of the visual <code>Component</code> depicting video
     * @param type the type of the availability change which has caused the new
     *            <code>VideoEvent</code> to be fired
     * @param visualComponent the visual <code>Component</code> depicting video
     *            which had its availability in the <code>source</code> provider
     *            changed
     * @param origin the origin of the video the new <code>VideoEvent</code> is
     *            to notify about
     */
    public VideoEvent(Object source, int type, Component visualComponent,
        int origin)
    {
        super(source);

        this.type = type;
        this.visualComponent = visualComponent;
        this.origin = origin;
    }

    /**
     * Consumes this event and, more specifically, marks the
     * <code>Component</code> it describes as owned, referenced in order to let
     * other potential consumers know about its current ownership status (which
     * is important because <code>Component</code>s belong to a single
     * <code>Container</code> at a time).
     */
    public void consume()
    {
        consumed = true;
    }

    /**
     * Gets the origin of the video this <code>VideoEvent</code> notifies about
     * which is one of {@link #LOCAL} and {@link #REMOTE}.
     *
     * @return one of {@link #LOCAL} and {@link #REMOTE} which specifies the
     *         origin of the video this <code>VideoEvent</code> notifies about
     */
    public int getOrigin()
    {
        return origin;
    }

    /**
     * Gets the type of availability change this <code>VideoEvent</code>
     * notifies about which is one of {@link #VIDEO_ADDED} and
     * {@link #VIDEO_REMOVED}.
     *
     * @return one of {@link #VIDEO_ADDED} and {@link #VIDEO_REMOVED} which
     *         describes the type of availability change this
     *         <code>VideoEvent</code> notifies about
     */
    public int getType()
    {
        return type;
    }

    /**
     * Gets the visual <code>Component</code> depicting video which had its
     * availability changed and which this <code>VideoEvent</code> notifies
     * about.
     *
     * @return the visual <code>Component</code> depicting video which had its
     *         availability changed and which this <code>VideoEvent</code>
     *         notifies about
     */
    public Component getVisualComponent()
    {
        return visualComponent;
    }

    /**
     * Determines whether this event and, more specifically, the visual
     * <code>Component</code> it describes have been consumed and should be
     * considered owned, referenced (which is important because
     * <code>Component</code>s belong to a single <code>Container</code> at a
     * time).
     *
     * @return <tt>true</tt> if this event and, more specifically, the visual
     *         <code>Component</code> it describes have been consumed and should
     *         be considered owned, referenced (which is important because
     *         <code>Component</code>s belong to a single <code>Container</code>
     *         at a time); otherwise, <tt>false</tt>
     */
    public boolean isConsumed()
    {
        return consumed;
    }
}
