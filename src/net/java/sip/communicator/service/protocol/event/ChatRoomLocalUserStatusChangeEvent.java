/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.event;

/**
 * Dispatched to notify interested parties that a change in our status in the
 * source room has changed. Changes may include us being kicked, banned, or
 * granted admin permissions.
 *
 * @author Emil Ivov
 */
public class ChatRoomLocalUserStatusChangeEvent
{
    public ChatRoomLocalUserStatusChangeEvent()
    {
        super();
    }

}
