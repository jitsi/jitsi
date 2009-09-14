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
 * message. Here, it's applied to an <tt>AdHocChatRoom</tt>.
 *
 * @author Valentin Martinet
 */
@SuppressWarnings("serial")
public class AdHocChatRoomMessageDeliveredEvent
    extends EventObject
{
    /**
     * An event type indicating that the message being received is a standard
     * conversation message sent by another participant of the ad-hoc chat room
     * to all current participants.
     */
    public static final int CONVERSATION_MESSAGE_DELIVERED = 1;

    /**
     * An event type indicating that the message being received is a special
     * message that sent by either another participant or the server itself,
     * indicating that some kind of action (other than the delivery of a
     * conversation message) has occurred. Action messages are widely used
     * in IRC through the /action and /me commands
     */
    public static final int ACTION_MESSAGE_DELIVERED = 2;

     /**
      * A timestamp indicating the exact date when the event occurred.
      */
     private final long timestamp;

     /**
      * The received <tt>Message</tt>.
      */
     private Message message = null;

     /**
      * The type of message event that this instance represents.
      */
     private int eventType = -1;

     /**
      * Creates a <tt>MessageDeliveredEvent</tt> representing delivery of the
      * <tt>source</tt> message to the specified <tt>to</tt> contact.
      *
      * @param source the <tt>AdHocChatRoom</tt> which triggered this event.
      * @param timestamp a date indicating the exact moment when the event
      * occurred
      * @param message the message that triggered this event.
      * @param eventType indicating the type of the delivered event. It's
      * either an ACTION_MESSAGE_DELIVERED or a CONVERSATION_MESSAGE_DELIVERED.
      */
     public AdHocChatRoomMessageDeliveredEvent(  AdHocChatRoom source,
                                                 long timestamp,
                                                 Message message,
                                                 int eventType)
     {
         super(source);

         this.timestamp = timestamp;
         this.message = message;
         this.eventType = eventType;
     }

     /**
      * Returns the received message.
      * 
      * @return the <tt>Message</tt> that triggered this event.
      */
     public Message getMessage()
     {
         return this.message;
     }

     /**
      * A timestamp indicating the exact date when the event occurred.
      * 
      * @return a Date indicating when the event occurred.
      */
     public long getTimestamp()
     {
         return this.timestamp;
     }

     /**
      * Returns the <tt>AdHocChatRoom</tt> that triggered this event.
      * 
      * @return the <tt>AdHocChatRoom</tt> that triggered this event.
      */
     public AdHocChatRoom getSourceAdHocChatRoom()
     {
         return (AdHocChatRoom) this.getSource();
     }

     /**
      * Returns the type of message event represented by this event instance.
      * Message event type is one of the XXX_MESSAGE_DELIVERED fields of this
      * class.
      * 
      * @return one of the XXX_MESSAGE_DELIVERED fields of this
      * class indicating the type of this event.
      */
     public int getEventType()
     {
         return this.eventType;
     }
}
