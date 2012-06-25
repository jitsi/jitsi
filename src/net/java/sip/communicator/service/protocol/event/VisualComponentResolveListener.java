/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.event;

import java.util.*;

/**
 * In a conference call notifies that a visual <tt>Component</tt> representing
 * video has been resolved to be corresponding to a given
 * <tt>ConferenceMember</tt>.
 *
 * @author Yana Stamcheva
 */
public interface VisualComponentResolveListener
    extends EventListener
{
    /**
     * Notifies that a visual <tt>Component</tt> representing video has been
     * resolved to be corresponding to a given <tt>ConferenceMember</tt>.
     *
     * @param event a <tt>VisualComponentResolveEvent</tt> describing the
     * resolved component and the corresponding <tt>ConferenceMember</tt>
     */
    public void visualComponentResolved(VisualComponentResolveEvent event);
}
