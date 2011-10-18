/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.event;

import java.util.*;

/**
 * Represents a listener of changes in the capabilities of a <tt>Contact</tt> as
 * known by an associated protocol provider delivered in the form of
 * <tt>ContactCapabilitiesEvent</tt>s.
 *
 * @author Lubomir Marinov
 */
public interface ContactCapabilitiesListener
    extends EventListener
{
    /**
     * Notifies this listener that the list of the <tt>OperationSet</tt>
     * capabilities of a <tt>Contact</tt> has changed.
     * 
     * @param event a <tt>ContactCapabilitiesEvent</tt> with ID
     * {@link ContactCapabilitiesEvent#SUPPORTED_OPERATION_SETS_CHANGED} which
     * specifies the <tt>Contact</tt> whose list of <tt>OperationSet</tt>
     * capabilities has changed
     */
    public void supportedOperationSetsChanged(ContactCapabilitiesEvent event);
}
