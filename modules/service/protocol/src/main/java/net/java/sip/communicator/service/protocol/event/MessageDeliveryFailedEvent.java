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
 * <tt>MessageDeliveryFailedEvent</tt>s inform of failed delivery of an instant
 * message.
 *
 * @author Emil Ivov
 */
public class MessageDeliveryFailedEvent
    extends EventObject
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

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
     * Set when delivery fails because of dependency on an operation that is
     * unsupported. For example, because it is unknown or not supported at that
     * particular moment. (Skipped value 6 just in case, since it is used in
     * ChatRoomMessageDeliveryFailedEvent.)
     */
    public static final int UNSUPPORTED_OPERATION = 7;

     /**
      * An error code indicating the reason for the failure of this delivery.
      */
     private int errorCode = UNKNOWN_ERROR;

     /**
      * Contains a human readable message indicating the reason for the failure
      * or null if the reason is unknown.
      */
     private String reasonPhrase = null;

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
      * @param source the message
      * @param to the "to" contact
      * @param errorCode error code
      */
     public MessageDeliveryFailedEvent(Message source,
                                       Contact to,
                                       int errorCode)
     {
         this(source, to, errorCode, System.currentTimeMillis(), null);
     }

     /**
      * Constructor.
      *
      * @param source the message
      * @param to the "to" contact
      * @param correctedMessageUID The ID of the message being corrected.
      * @param errorCode error code
      */
     public MessageDeliveryFailedEvent(Message source,
                                       Contact to,
                                       String correctedMessageUID,
                                       int errorCode)
     {
         this(source, to, errorCode, System.currentTimeMillis(), null);
         this.correctedMessageUID = correctedMessageUID;
     }

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
                                       long timestamp)
     {
         this(source, to, errorCode, timestamp, null);
     }

     /**
      * Creates a <tt>MessageDeliveryFailedEvent</tt> indicating failure of
      * delivery of the <tt>source</tt> message to the specified <tt>to</tt>
      * contact.
      *
      * @param source the <tt>Message</tt> whose delivery this event represents.
      * @param to the <tt>Contact</tt> that this message was sent to.
      * @param errorCode an errorCode indicating the reason of the failure.
      * @param timestamp the exact timestamp when it was determined that delivery
      * had failed.
      * @param reason a human readable message indicating the reason for the
      * failure or null if the reason is unknown.
      */
     public MessageDeliveryFailedEvent(Message source,
                                       Contact to,
                                       int errorCode,
                                       long timestamp,
                                       String reason)
     {
         super(source);

         this.to = to;
         this.errorCode = errorCode;
         this.timestamp = timestamp;
         this.reasonPhrase = reason;
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
      * Returns the message that triggered this event
      * @return the <tt>Message</tt> that triggered this event.
      */
     public Message getSourceMessage()
     {
         return (Message) getSource();
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
     * case it is the moment when it was determined that message delivery has
     * failed).
     *
     * @return a long indicating when the event ocurred in the form of
     *         date timestamp.
     */
    public long getTimestamp()
    {
        return timestamp;
    }

    /**
     * Returns a human readable message indicating the reason for the failure
     * or null if the reason is unknown.
     *
     * @return a human readable message indicating the reason for the failure
     * or null if the reason is unknown.
     */
    public String getReason()
    {
        return reasonPhrase;
    }

    /**
     * Sets the ID of the message being corrected to the passed ID.
     *
     * @param correctedMessageUID The ID of the message being corrected.
     */
    public String getCorrectedMessageUID()
    {
        return correctedMessageUID;
    }
}
