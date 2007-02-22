/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.event;

import java.util.*;

/**
 * Adds a listener that will be notified of changes in our status in the room
 * such as us being kicked, banned, or granted admin permissions.
 * @author Emil Ivov
 */
public interface ChatRoomLocalUserStatusListener
    extends EventListener
{
    /**
     * Called to notify interested parties that a change in our status in the
     * source soom has changed. Changes may include us being kicked, banned, or
     * granted admin permissions.
     */
    public void localUserStatusChanged(ChatRoomLocalUserStatusChangeEvent evt);
}
