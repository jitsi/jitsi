/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.sip.communicator.service.protocol.event;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * <tt>MessageReceivedEvent</tt>s indicate reception of an instant message.
 *
 * @author Emil Ivov
 */
public class MessageReceivedEvent
    extends EventObject
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * An event type indicating that the message being received is a standard
     * conversation message sent by another contact.
     */
    public static final int CONVERSATION_MESSAGE_RECEIVED = 1;

    /**
     * An event type indicating that the message being received is a system
     * message being sent by the server or a system administrator.
     */
    public static final int SYSTEM_MESSAGE_RECEIVED = 2;

    /**
     * an event type indicating that the message being received is an SMS
     * message.
     */
    public static final int SMS_MESSAGE_RECEIVED = 3;

    /**
     * The contact that has sent this message.
     */
    private Contact from = null;

    /**
     * The <tt>ContactResource</tt>, from which the message was sent.
     */
    private ContactResource fromResource = null;

    /**
     * A timestamp indicating the exact date when the event occurred.
     */
    private final Date timestamp;

    /**
     * The type of message event that this instance represents.
     */
    private int eventType = -1;

    /**
     * The ID of the message being corrected, or null if this is a new message
     * and not a correction.
     */
    private String correctedMessageUID = null;
    
    /**
     * Indicates whether this is private messaging event or not.
     */
    private boolean isPrivateMessaging = false;
    
    /**
     * The room associated with the contact which sent the message.
     */
    private ChatRoom privateMessagingContactRoom = null;

    /**
     * Creates a <tt>MessageReceivedEvent</tt> representing reception of the
     * <tt>source</tt> message received from the specified <tt>from</tt>
     * contact.
     *
     * @param source the <tt>Message</tt> whose reception this event represents.
     * @param from the <tt>Contact</tt> that has sent this message.
     * @param timestamp the exact date when the event ocurred.
     */
    public MessageReceivedEvent(Message source,
                                Contact from,
                                Date timestamp)
    {
       this(source, from, timestamp, CONVERSATION_MESSAGE_RECEIVED);
    }

    /**
     * Creates a <tt>MessageReceivedEvent</tt> representing reception of the
     * <tt>source</tt> message received from the specified <tt>from</tt>
     * contact.
     *
     * @param source the <tt>Message</tt> whose reception this event represents.
     * @param from the <tt>Contact</tt> that has sent this message.
     * @param correctedMessageUID The ID of the message being corrected, or null
     * if this is a new message
     * and not a correction.
     */
    public MessageReceivedEvent(Message source,
                                Contact from,
                                String correctedMessageUID)
    {
       this(source, from, new Date(),
               CONVERSATION_MESSAGE_RECEIVED);
       this.correctedMessageUID = correctedMessageUID;
    }

    /**
     * Creates a <tt>MessageReceivedEvent</tt> representing reception of the
     * <tt>source</tt> message received from the specified <tt>from</tt>
     * contact.
     *
     * @param source the <tt>Message</tt> whose reception this event represents.
     * @param from the <tt>Contact</tt> that has sent this message.
     * @param timestamp the exact date when the event occurred.
     * @param correctedMessageUID The ID of the message being corrected, or null
     * if this is a new message
     * and not a correction.
     */
    public MessageReceivedEvent(Message source,
                                Contact from,
                                Date timestamp,
                                String correctedMessageUID)
    {
        this(   source,
                from,
                null,
                timestamp,
                correctedMessageUID);
    }

    /**
     * Creates a <tt>MessageReceivedEvent</tt> representing reception of the
     * <tt>source</tt> message received from the specified <tt>from</tt>
     * contact.
     *
     * @param source the <tt>Message</tt> whose reception this event represents.
     * @param from the <tt>Contact</tt> that has sent this message.
     * @param fromResource the <tt>ContactResource</tt>, from which this message
     * was sent
     * @param timestamp the exact date when the event occurred.
     * @param correctedMessageUID The ID of the message being corrected, or null
     * if this is a new message
     * and not a correction.
     */
    public MessageReceivedEvent(Message source,
                                Contact from,
                                ContactResource fromResource,
                                Date timestamp,
                                String correctedMessageUID)
    {
        this(   source,
                from,
                fromResource,
                timestamp,
                CONVERSATION_MESSAGE_RECEIVED);

        this.correctedMessageUID = correctedMessageUID;
    }
    
    /**
     * Creates a <tt>MessageReceivedEvent</tt> representing reception of the
     * <tt>source</tt> message received from the specified <tt>from</tt>
     * contact.
     *
     * @param source the <tt>Message</tt> whose reception this event represents.
     * @param from the <tt>Contact</tt> that has sent this message.
     * @param fromResource the <tt>ContactResource</tt>, from which this message
     * was sent
     * @param timestamp the exact date when the event occurred.
     * @param correctedMessageUID The ID of the message being corrected, or null
     * if this is a new message and not a correction.
     * @param isPrivateMessaging indicates whether the this is private messaging
     * event or not.
     * @param privateContactRoom the chat room associated with the contact.
     */
    public MessageReceivedEvent(Message source,
                                Contact from,
                                ContactResource fromResource,
                                Date timestamp,
                                String correctedMessageUID,
                                boolean isPrivateMessaging,
                                ChatRoom privateContactRoom)
    {
        this(   source,
                from,
                fromResource,
                timestamp,
                CONVERSATION_MESSAGE_RECEIVED,
                isPrivateMessaging,
                privateContactRoom);
        
        this.correctedMessageUID = correctedMessageUID;
    }

    /**
     * Creates a <tt>MessageReceivedEvent</tt> representing reception of the
     * <tt>source</tt> message received from the specified <tt>from</tt>
     * contact.
     *
     * @param source the <tt>Message</tt> whose reception this event represents.
     * @param from the <tt>Contact</tt> that has sent this message.
     * @param timestamp the exact date when the event occurred.
     * @param eventType the type of message event that this instance represents
     * (one of the XXX_MESSAGE_RECEIVED static fields).
     */
    public MessageReceivedEvent(Message source,
                                Contact from,
                                Date timestamp,
                                int eventType)
    {
        this(source, from, null, timestamp, eventType);
    }

    /**
     * Creates a <tt>MessageReceivedEvent</tt> representing reception of the
     * <tt>source</tt> message received from the specified <tt>from</tt>
     * contact.
     *
     * @param source the <tt>Message</tt> whose reception this event represents.
     * @param from the <tt>Contact</tt> that has sent this message.
     * @param fromResource the <tt>ContactResource</tt>, from which this message
     * was sent
     * @param timestamp the exact date when the event occurred.
     * @param eventType the type of message event that this instance represents
     * (one of the XXX_MESSAGE_RECEIVED static fields).
     */
    public MessageReceivedEvent(Message source,
                                Contact from,
                                ContactResource fromResource,
                                Date timestamp,
                                int eventType)
    {
        this(source, from, fromResource, timestamp, eventType, false, null);
    }

    /**
     * Creates a <tt>MessageReceivedEvent</tt> representing reception of the
     * <tt>source</tt> message received from the specified <tt>from</tt>
     * contact.
     *
     * @param source the <tt>Message</tt> whose reception this event represents.
     * @param from the <tt>Contact</tt> that has sent this message.
     * @param fromResource the <tt>ContactResource</tt>, from which this message
     * was sent
     * @param timestamp the exact date when the event occurred.
     * @param eventType the type of message event that this instance represents
     * (one of the XXX_MESSAGE_RECEIVED static fields).
     * @param isPrivateMessaging indicates whether the this is private messaging
     * event or not.
     * @param privateContactRoom the chat room associated with the contact.
     */
    public MessageReceivedEvent(Message source,
                                Contact from,
                                ContactResource fromResource,
                                Date timestamp,
                                int eventType,
                                boolean isPrivateMessaging,
                                ChatRoom privateContactRoom)
    {
        super(source);
        
        this.from = from;
        this.fromResource = fromResource;
        this.timestamp = timestamp;
        this.eventType = eventType;
        this.isPrivateMessaging = isPrivateMessaging;
        this.privateMessagingContactRoom = privateContactRoom;
    }

    /**
     * Returns a reference to the <tt>Contact</tt> that has sent the
     * <tt>Message</tt> whose reception this event represents.
     *
     * @return a reference to the <tt>Contact</tt> that has sent the
     * <tt>Message</tt> whose reception this event represents.
     */
    public Contact getSourceContact()
    {
        return from;
    }

    /**
     * Returns a reference to the <tt>ContactResource</tt> that has sent the
     * <tt>Message</tt> whose reception this event represents.
     *
     * @return a reference to the <tt>ContactResource</tt> that has sent the
     * <tt>Message</tt> whose reception this event represents.
     */
    public ContactResource getContactResource()
    {
        return fromResource;
    }

    /**
     * Returns the message that triggered this event
     *
     * @return the <tt>Message</tt> that triggered this event.
     */
    public Message getSourceMessage()
    {
        return (Message)getSource();
    }

    /**
     * A timestamp indicating the exact date when the event occurred.
     *
     * @return a Date indicating when the event occurred.
     */
    public Date getTimestamp()
    {
        return timestamp;
    }

    /**
     * Returns the type of message event represented by this event instance.
     * Message event type is one of the XXX_MESSAGE_RECEIVED fields of this
     * class.
     *
     * @return one of the XXX_MESSAGE_RECEIVED fields of this class indicating
     * the type of this event.
     */
    public int getEventType()
    {
        return eventType;
    }

    /**
     * Returns the correctedMessageUID The ID of the message being corrected,
     * or null if this is a new message and not a correction.
     *
     * @return the correctedMessageUID The ID of the message being corrected,
     * or null if this is a new message and not a correction.
     */
    public String getCorrectedMessageUID()
    {
        return correctedMessageUID;
    }

    /**
     * Returns the chat room of the private messaging contact associated with 
     * the event and null if the contact is not private messaging contact.
     * 
     * @return the chat room associated with the contact or null if no chat room
     * is associated with the contact.
     */
    public ChatRoom getPrivateMessagingContactRoom()
    {
        return privateMessagingContactRoom;
    }
    
    /**
     * Returns <tt>true</true> if this is private messaging event and 
     * <tt>false</tt> if not.
     * 
     * @return <tt>true</true> if this is private messaging event and 
     * <tt>false</tt> if not.
     */
    public boolean isPrivateMessaging()
    {
        return isPrivateMessaging;
    }


}
