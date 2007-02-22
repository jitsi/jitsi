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
      * The contact that has sent this message.
      */
     private Contact to = null;

     /**
      * A timestamp indicating the exact date when the event occurred.
      */
     private Date timestamp = null;

     /**
      * The name/id of the chat room where the message was delivered.
      */
     private String chatRoomID = null;

     /**
      * Creates a <tt>MessageDeliveredEvent</tt> representing delivery of the
      * <tt>source</tt> message to the specified <tt>to</tt> contact.
      *
      * @param source the <tt>Message</tt> whose delivery this event represents.
      * @param to the <tt>Contact</tt> that this message was sent to.
      * @param timestamp a date indicating the exact moment when the event
      * ocurred
      * @param chatRoomID the name/id of the chatroom where this message was
      * delivered.
      */
     public ChatRoomMessageDeliveredEvent(Message source,
                                          Contact to,
                                          Date timestamp,
                                          String chatRoomID)
     {
         super(source);

         this.to = to;
         this.timestamp = timestamp;
         this.chatRoomID = chatRoomID;
     }

     /**
      * Returns a reference to the <tt>Contact</tt> that <tt>Message</tt> was
      * sent to.
      *
      * @return a reference to the <tt>Contact</tt> that has send the
      * <tt>Message</tt> whose reception this event represents.
      */
     public Contact getDestinationContact()
     {
         return to;
     }

     /**
      * Returns the message that triggered this event
      * @return the <tt>Message</tt> that triggered this event.
      */
     public Message getSourceMessage()
     {
         return (Message) getSource();
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
      * Returns the name of the chat room where this message has been delivered.
      * @return the name of the chat room where this message has been delivered.
      */
     public String getChatRoomID()
     {
         return chatRoomID;
     }

}
