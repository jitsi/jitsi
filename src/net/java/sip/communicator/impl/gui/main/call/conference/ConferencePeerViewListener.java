/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call.conference;

import net.java.sip.communicator.impl.gui.event.*;

/**
 * The <tt>ConferencePeerViewListener</tt> is notified when a conference peer
 * panel was added or removed.
 *
 * @author Hristo Terezov
 */
public interface ConferencePeerViewListener
{
    /**
     * Indicates that the peer panel was added.
     *
     * @param ev the event.
     */
    public void peerViewAdded(ConferencePeerViewEvent ev);

    /**
     * Indicates that the peer panel was removed.
     *
     * @param ev the event.
     */
    public void peerViewRemoved(ConferencePeerViewEvent ev);
}
