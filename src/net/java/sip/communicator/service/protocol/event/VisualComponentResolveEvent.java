/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.event;

import java.awt.*;
import java.util.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * In a conference call notifies that a visual <tt>Component</tt> representing
 * video has been resolved to be corresponding to a given
 * <tt>ConferenceMember</tt>. The event contains information about the concerned
 * visual component and it's corresponding member.
 *
 * @author Yana Stamcheva
 */
public class VisualComponentResolveEvent
    extends EventObject
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The visual component that has been resolved.
     */
    private final ConferenceMember conferenceMember;

    /**
     * The visual <tt>Component</tt> depicting video which had its availability
     * changed and which this <tt>VideoEvent</tt> notifies about.
     */
    private final Component visualComponent;

    /**
     * 
     * @param source the source of the new <tt>VideoEvent</tt> and the provider
     * of the visual <tt>Component</tt> depicting video
     */
    public VisualComponentResolveEvent( Object source,
                                        Component visualComponent,
                                        ConferenceMember resolvedMember)
    {
        super(source);

        this.visualComponent = visualComponent;
        this.conferenceMember = resolvedMember;
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
     * Gets the <tt>ConferenceMember</tt> that was resolved to be corresponding
     * to the source visual component.
     *
     * @return the <tt>ConferenceMember</tt> that was resolved to be
     * corresponding to the source visual component
     */
    public ConferenceMember getConferenceMember()
    {
        return conferenceMember;
    }
}
