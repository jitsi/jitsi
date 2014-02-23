/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.event;

import java.util.*;

/**
 * A listener that will be notified when a <tt>ChatRoomMember</tt> publishes a
 * <tt>ConferenceDescription</tt> in a <tt>ChatRoom</tt>.
 *
 * @author Boris Grozev
 */
public interface ChatRoomConferencePublishedListener
        extends EventListener
{
    /**
     * Called to notify interested parties that <tt>ChatRoomMember</tt>
     * in a <tt>ChatRoom</tt> has published a <tt>ConferenceDescription</tt>.
     *
     * @param evt the <tt>ChatRoomMemberPresenceChangeEvent</tt> instance
     * containing the source chat room and type, and reason of the presence
     * change
     */
    public void conferencePublished(ChatRoomConferencePublishedEvent evt);

}
