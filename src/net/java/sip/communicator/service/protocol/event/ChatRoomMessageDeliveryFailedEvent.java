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
 * <tt>ChatRoomMessageDeliveredEvent</tt>s confirm successful delivery of an
 * instant message.
 *
 * @author Emil Ivov
 */
public class ChatRoomMessageDeliveryFailedEvent
    extends EventObject
{
    /**
      * The contact that this message has been sent to.
      */
     private Contact to = null;

     /**
      * Set when no other error code can describe the exception that occurred.
      */
     public static final int UNKNOWN_ERROR = 1;

     /**
      * Set when delivery fails due to a failure in network communications or
      * a transport error.
      */
     public static final int NETWORK_FAILURE = 2;

     /**
      * Set to indicate that delivery has failed because the provider was not
      * registered.
      */
     public static final int PROVIDER_NOT_REGISTERED = 3;

     /**
      * Set when delivery fails for implementation specific reasons.
      */
     public static final int INTERNAL_ERROR = 4;

     /**
      * Set when delivery fails because we're trying to send a message to a
      * contact that is currently offline and the server does not support
      * offline messages.
      */
     public static final int OFFLINE_MESSAGES_NOT_SUPPORTED = 5;

     /**
      * An error code indicating the reason for the failure of this delivery.
      */
     private int errorCode = UNKNOWN_ERROR;

     /**
      * A timestamp indicating the exact date when the event occurred.
      */
     private Date timestamp = null;

     /**
      * The name/id of the chat room where the message was delivered.
      */
     private String chatRoomID = null;

     /**
      * Creates a <tt>MessageDeliveryFailedEvent</tt> indicating failure of
      * delivery of the <tt>source</tt> message to the specified <tt>to</tt>
      * contact.
      *
      * @param source the <tt>Message</tt> whose delivery this event represents.
      * @param to the <tt>Contact</tt> that this message was sent to.
      * @param errorCode an errorCode indicating the reason of the failure.
      * @param timestamp the exacte Date when it was determined that delivery
      * had failed.
      * @param chatRoomID the name/id of the chat room that the failed message
      * was destined to.
      */
     public ChatRoomMessageDeliveryFailedEvent(Message source,
                                               Contact to,
                                               int errorCode,
                                               Date timestamp,
                                               String chatRoomID)
     {
         super(source);

         this.to = to;
         this.errorCode = errorCode;
         this.timestamp = timestamp;
         this.chatRoomID = chatRoomID;
     }
     /**
      * Returns a reference to the <tt>Contact</tt> that the source (failed)
      * <tt>Message</tt> was sent to.
      *
      * @return a reference to the <tt>Contact</tt> that the source failed
      * <tt>Message</tt> wwas sent to.
      */
     public Contact getDestinationContact()
     {
         return to;
     }

     /**
      * Returns an error code descibing the reason for the failure of the
      * message delivery.
      * @return an error code descibing the reason for the failure of the
      * message delivery.
      */
     public int getErrorCode()
     {
        return errorCode;
     }


     /**
      * A timestamp indicating the exact date when the event ocurred (in this
      * case it is the moment when it was determined that message delivery
      * has failed).
      * @return a Date indicating when the event ocurred.
      */
     public Date getTimestamp()
     {
         return timestamp;
     }

     /**
      * Returns the name of the chat room where this message was meant to be
      * delivered.
      * @return the name of the chat room where this message was meant to be
      * delivered.
      */
     public String getChatRoomID()
     {
         return chatRoomID;
     }
}
