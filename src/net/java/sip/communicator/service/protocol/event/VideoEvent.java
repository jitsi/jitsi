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
     */
    public VideoEvent(Object source, int type, Component visualComponent)
    {
        super(source);

        this.type = type;
        this.visualComponent = visualComponent;
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
}
