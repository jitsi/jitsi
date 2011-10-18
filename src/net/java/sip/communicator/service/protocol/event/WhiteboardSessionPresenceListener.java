 /*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.event;

import java.util.*;

/**
 * A listener that will be notified of changes in our presence in the
 * white-board, such as joined, left, dropped, etc.
 * 
 * @author Yana Stamcheva
 */
public interface WhiteboardSessionPresenceListener
    extends EventListener
{
    /**
     * Called to notify interested parties that a change in our presence in
     * a white-board has occured. Changes may include us being joined,
     * left, dropped.
     * @param evt the <tt>WhiteboardSessionPresenceChangeEvent</tt> instance
     * containing the session and the type, and reason of the change
     */
    public void whiteboardSessionPresenceChanged(
        WhiteboardSessionPresenceChangeEvent evt);
}
