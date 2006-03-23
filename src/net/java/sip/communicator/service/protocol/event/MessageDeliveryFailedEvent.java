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
public class MessageDeliveryFailedEvent
    extends EventObject
{
    /**
      * The contact that this message has been sent to.
      */
     private Contact to = null;


     /**
      * An error code indicating the reason for the failure of this delivery.
      */
     private int errorCode = -1;

     /**
      * A timestamp indicating the exact date when the event occurred.
      */
     private Date timestamp = null;


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
      */
     public MessageDeliveryFailedEvent(Message source,
                                       Contact to,
                                       int errorCode,
                                       Date timestamp )
     {
         super(source);

         this.to = to;

         this.errorCode = errorCode;

         this.timestamp = timestamp;
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

}
