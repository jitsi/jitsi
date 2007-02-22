/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.event;

import java.util.*;

/**
 * Dispatched to notify interested parties that a change in the status of the
 * source room participant has changed. Changes may include the participant
 * being kicked, banned, or granted admin permissions.
 *
 * @author Emil Ivov
 */
public class ChatRoomParticipantStatusChangeEvent
    extends EventObject
{
    public ChatRoomParticipantStatusChangeEvent(Object source)
    {
        super(source);
    }
}
