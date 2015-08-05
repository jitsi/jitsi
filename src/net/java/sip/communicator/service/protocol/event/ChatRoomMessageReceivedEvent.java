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
 * @author Lubomir Marinov
 */
public class ChatRoomMessageReceivedEvent
    extends EventObject
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * An event type indicating that the message being received is a standard
     * conversation message sent by another member of the chatroom to all
     * current participants.
     */
    public static final int CONVERSATION_MESSAGE_RECEIVED = 1;

    /**
     * An event type indicating that the message being received is a special
     * message that sent by either another member or the server itself,
     * indicating that some kind of action (other than the delivery of a
     * conversation message) has occurred. Action messages are widely used
     * in IRC through the /action and /me commands
     */
    public static final int ACTION_MESSAGE_RECEIVED = 2;

    /**
     * An event type indicting that the message being received is a system
     * message being sent by the server or a system administrator, possibly
     * notifying us of something important such as ongoing maintenance
     * activities or server downtime.
     */
    public static final int SYSTEM_MESSAGE_RECEIVED = 3;

    /**
     * The chat room member that has sent this message.
     */
    private final ChatRoomMember from;

    /**
     * A timestamp indicating the exact date when the event occurred.
     */
    private final Date timestamp;

    /**
     * The received <tt>Message</tt>.
     */
    private final Message message;

    /**
     * The type of message event that this instance represents.
     */
    private final int eventType;

    /**
     * Some services can fill our room with message history.
     */
    private boolean historyMessage = false;
    
    /**
     * Indicates whether the message is important or not.
     */
    private boolean isImportantMessage = false;

    /**
     * Creates a <tt>MessageReceivedEvent</tt> representing reception of the
     * <tt>source</tt> message received from the specified <tt>from</tt>
     * contact.
     *
     * @param source the <tt>ChatRoom</tt> for which the message is received.
     * @param from the <tt>ChatRoomMember</tt> that has sent this message.
     * @param timestamp the exact date when the event occurred.
     * @param message the received <tt>Message</tt>.
     * @param eventType the type of message event that this instance represents
     * (one of the XXX_MESSAGE_RECEIVED static fields).
     */
    public ChatRoomMessageReceivedEvent(ChatRoom        source,
                                        ChatRoomMember  from,
                                        Date            timestamp,
                                        Message         message,
                                        int             eventType)
    {
        super(source);

        this.from = from;
        this.timestamp = timestamp;
        this.message = message;
        this.eventType = eventType;
    }

    /**
     * Returns a reference to the <tt>ChatRoomMember</tt> that has send the
     * <tt>Message</tt> whose reception this event represents.
     *
     * @return a reference to the <tt>ChatRoomMember</tt> that has send the
     * <tt>Message</tt> whose reception this event represents.
     */
    public ChatRoomMember getSourceChatRoomMember()
    {
        return from;
    }

    /**
     * Returns the received message.
     * @return the <tt>Message</tt> that triggered this event.
     */
    public Message getMessage()
    {
        return message;
    }

    /**
     * A timestamp indicating the exact date when the event occurred.
     * @return a Date indicating when the event occurred.
     */
    public Date getTimestamp()
    {
        return timestamp;
    }

    /**
     * Returns the <tt>ChatRoom</tt> that triggered this event.
     * @return the <tt>ChatRoom</tt> that triggered this event.
     */
    public ChatRoom getSourceChatRoom()
    {
        return (ChatRoom) getSource();
    }

    /**
     * Returns the type of message event represented by this event instance.
     * Message event type is one of the XXX_MESSAGE_RECEIVED fields of this class.
     * @return one of the XXX_MESSAGE_RECEIVED fields of this
     * class indicating the type of this event.
     */
    public int getEventType()
    {
        return eventType;
    }

    /**
     * Is current event for history message.
     * @return is current event for history message.
     */
    public boolean isHistoryMessage()
    {
        return historyMessage;
    }

    /**
     * Changes property, whether this event is for a history message.
     *
     * @param historyMessage whether its event for history message.
     */
    public void setHistoryMessage(boolean historyMessage)
    {
        this.historyMessage = historyMessage;
    }

    /**
     * Sets the the important message flag of the event.
     * @param isImportant the value to be set.
     */
    public void setImportantMessage(boolean isImportant)
    {
        isImportantMessage = isImportant;
    }
    
    /**
     * Returns <tt>true</tt> if message is important and <tt>false</tt> if not.
     * @return <tt>true</tt> if message is important and <tt>false</tt> if not.
     */
    public boolean isImportantMessage()
    {
        return isImportantMessage;
    }
}
