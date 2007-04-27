/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.event;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * <tt>MessageDeliveredEvent</tt>s confirm successful delivery of an instant
 * message.
 *
 * @author Emil Ivov
 */
public class ChatRoomMessageDeliveredEvent
    extends EventObject
{
    /**
      * The chat room member that has sent this message.
      */
     private ChatRoomMember to = null;

     /**
      * A timestamp indicating the exact date when the event occurred.
      */
     private Date timestamp = null;

     /**
      * The received <tt>Message</tt>.
      */
     private Message message = null;

     /**
      * Creates a <tt>MessageDeliveredEvent</tt> representing delivery of the
      * <tt>source</tt> message to the specified <tt>to</tt> contact.
      *
      * @param source the <tt>ChatRoom</tt> which triggered this event.
      * @param to the <tt>ChatRoomMember</tt> that this message was sent to.
      * @param timestamp a date indicating the exact moment when the event
      * ocurred
      * @param chatRoomID the name/id of the chatroom where this message was
      * delivered.
      */
     public ChatRoomMessageDeliveredEvent(ChatRoom source,
                                          ChatRoomMember to,
                                          Date timestamp,
                                          Message message)
     {
         super(source);

         this.to = to;
         this.timestamp = timestamp;
         this.message = message;
     }

     /**
      * Returns a reference to the <tt>ChatRoomMember</tt> that <tt>Message</tt>
      * was sent to.
      *
      * @return a reference to the <tt>Contact</tt> that has send the
      * <tt>Message</tt> whose reception this event represents.
      */
     public ChatRoomMember getDestinationChatRoomMember()
     {
         return to;
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
      * A timestamp indicating the exact date when the event ocurred.
      * @return a Date indicating when the event ocurred.
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
}
