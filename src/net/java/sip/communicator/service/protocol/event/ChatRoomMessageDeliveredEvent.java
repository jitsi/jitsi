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
 * <tt>MessageDeliveredEvent</tt>s confirm successful delivery of an instant
 * message.
 *
 * @author Emil Ivov
 * @author Yana Stamcheva
 */
public class ChatRoomMessageDeliveredEvent
    extends EventObject
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * An event type indicating that the message being received is a standard
     * conversation message sent by another member of the chat room to all
     * current participants.
     */
    public static final int CONVERSATION_MESSAGE_DELIVERED = 1;

    /**
     * An event type indicating that the message being received is a special
     * message that sent by either another member or the server itself,
     * indicating that some kind of action (other than the delivery of a
     * conversation message) has occurred. Action messages are widely used
     * in IRC through the /action and /me commands
     */
    public static final int ACTION_MESSAGE_DELIVERED = 2;

     /**
      * A timestamp indicating the exact date when the event occurred.
      */
     private final Date timestamp;

     /**
      * The received <tt>Message</tt>.
      */
     private Message message = null;

     /**
      * The type of message event that this instance represents.
      */
     private int eventType = -1;

     /**
      * Some services can fill our room with message history.
      */
     private boolean historyMessage = false;

     /**
      * Creates a <tt>MessageDeliveredEvent</tt> representing delivery of the
      * <tt>source</tt> message to the specified <tt>to</tt> contact.
      *
      * @param source the <tt>ChatRoom</tt> which triggered this event.
      * @param timestamp a date indicating the exact moment when the event
      * occurred
      * @param message the message that triggered this event.
      * @param eventType indicating the type of the delivered event. It's
      * either an ACTION_MESSAGE_DELIVERED or a CONVERSATION_MESSAGE_DELIVERED.
      */
     public ChatRoomMessageDeliveredEvent(ChatRoom  source,
                                          Date      timestamp,
                                          Message   message,
                                          int       eventType)
     {
         super(source);

         this.timestamp = timestamp;
         this.message = message;
         this.eventType = eventType;
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
      * Message event type is one of the XXX_MESSAGE_DELIVERED fields of this
      * class.
      * @return one of the XXX_MESSAGE_DELIVERED fields of this
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
}
