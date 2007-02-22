/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.event;

import java.util.*;

/**
 * <tt>ChatRoomChangeEvent</tt>s are fired to indicate that a property of
 * the corresponding chat room (e.g. its subject or type) have been modified.
 * The event contains references to the source chat room and provider, the name
 * of the property that has just changed as well as its old and new values.
 *
 * @author Emil Ivov
 */
public class ChatRoomPropertyChangeEvent
    extends EventObject
{
    public ChatRoomPropertyChangeEvent(Object source)
    {
        super(source);
    }

}
