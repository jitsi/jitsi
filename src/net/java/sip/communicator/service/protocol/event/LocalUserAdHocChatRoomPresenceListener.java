 /*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.event;

import java.util.*;

/**
 * A listener that will be notified of changes in our presence in the chat
 * room such as us being kicked, join, left.
 * 
 * @author Valentin Martinet
 */
public interface LocalUserAdHocChatRoomPresenceListener
    extends EventListener
{
    /**
     * Called to notify interested parties that a change in our presence in
     * an ad-hoc chat room has occurred. Changes may include us being join,
     * left.
     * @param evt the <tt>LocalUserAdHocChatRoomPresenceChangeEvent</tt>
     * instance containing the ad-hoc chat room and the type, and reason of the
     * change
     */
    public void localUserAdHocPresenceChanged(
        LocalUserAdHocChatRoomPresenceChangeEvent evt);
}
