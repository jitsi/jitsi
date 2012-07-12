/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
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
public class MessageDeliveredEvent
    extends EventObject
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
      * The contact that has sent this message.
      */
     private Contact to = null;

     /**
      * A timestamp indicating the exact date when the event occurred.
      */
     private final long timestamp;
     
     /**
      * The ID of the message being corrected, or null if this was a new message
      * and not a message correction.
      */
     private String correctedMessageUID;

     /**
      * Constructor.
      *
      * @param source message source
      * @param to the "to" contact
      */
     public MessageDeliveredEvent(Message source, Contact to)
     {
         this(source, to, System.currentTimeMillis());
     }
     
     /**
      * Creates a <tt>MessageDeliveredEvent</tt> representing delivery of the
      * <tt>source</tt> message to the specified <tt>to</tt> contact.
      *
      * @param source the <tt>Message</tt> whose delivery this event represents.
      * @param to the <tt>Contact</tt> that this message was sent to.
      * @param correctedMessageUID The ID of the message being corrected.
      */
     public MessageDeliveredEvent(Message source, Contact to,
             String correctedMessageUID)
     {
         this(source, to, System.currentTimeMillis());
         this.correctedMessageUID = correctedMessageUID;
     }

     /**
      * Creates a <tt>MessageDeliveredEvent</tt> representing delivery of the
      * <tt>source</tt> message to the specified <tt>to</tt> contact.
      *
      * @param source the <tt>Message</tt> whose delivery this event represents.
      * @param to the <tt>Contact</tt> that this message was sent to.
      * @param timestamp a date indicating the exact moment when the event
      * ocurred
      */
     public MessageDeliveredEvent(Message source, Contact to, long timestamp)
     {
         super(source);

         this.to = to;
         this.timestamp = timestamp;
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
      * A timestamp indicating the exact date when the event occurred.
      * @return a Date indicating when the event occurred.
      */
     public long getTimestamp()
     {
         return timestamp;
     }

    /**
     * Returns the ID of the message being corrected, or null if this was a
     * new message and not a message correction.
     *
     * @return the ID of the message being corrected, or null if this was a
     * new message and not a message correction.
     */
    public String getCorrectedMessageUID()
    {
        return correctedMessageUID;
    }
    
    /**
     * Sets the ID of the message being corrected to the passed ID.
     * 
     * @param correctedMessageUID The ID of the message being corrected.
     */
    public void setCorrectedMessageUID(String correctedMessageUID)
    {
        this.correctedMessageUID = correctedMessageUID;
    }

}
