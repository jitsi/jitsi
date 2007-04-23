/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.event;

import java.util.*;

/**
 * Adds a listener that will be notified of changes in the status of the chat
 * participants in a particular chat room, such as us being kicked, banned, or
 * granted admin permissions.
 * @author Emil Ivov
 */
public interface ChatRoomMemberListener
    extends EventListener
{
    /**
     * Called to notify interested parties that a change in the status of the
     * source room participant has changed. Changes may include the participant
     * being kicked, banned, or granted admin permissions.
     */
    public void memberStatusChanged( ChatRoomMemberEvent evt );

}
