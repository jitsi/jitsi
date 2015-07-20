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
     * The <tt>ContactResource</tt>, to which the message was sent.
     */
    private ContactResource toResource = null;

    /**
     * A timestamp indicating the exact date when the event occurred.
     */
    private final Date timestamp;

    /**
     * The ID of the message being corrected, or null if this was a new message
     * and not a message correction.
     */
    private String correctedMessageUID;

    /**
     * Whether the delivered message is a sms message.
     */
    private boolean smsMessage = false;
    
    /**
     * Whether the delivered message is encrypted or not.
     */
    private boolean isMessageEncrypted = false;

     /**
      * Constructor.
      *
      * @param source message source
      * @param to the "to" contact
      */
     public MessageDeliveredEvent(Message source, Contact to)
     {
         this(source, to, new Date());
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
         this(source, to, new Date());
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
     public MessageDeliveredEvent(Message source, Contact to, Date timestamp)
     {
         super(source);

         this.to = to;
         this.timestamp = timestamp;
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
     public MessageDeliveredEvent(
         Message source, Contact to, ContactResource toResource, Date timestamp)
     {
         super(source);

         this.to = to;
         this.toResource = toResource;
         this.timestamp = timestamp;
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
     public MessageDeliveredEvent(
         Message source, Contact to, ContactResource toResource)
     {
         this(source, to, new Date());

         this.toResource = toResource;
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
     public Date getTimestamp()
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

    /**
     * Sets whether the message is a sms one.
     * @param smsMessage whether it is a sms one.
     */
    public void setSmsMessage(boolean smsMessage)
    {
        this.smsMessage = smsMessage;
    }

    /**
     * Returns whether the delivered message is a sms one.
     * @return whether the delivered message is a sms one.
     */
    public boolean isSmsMessage()
    {
        return smsMessage;
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
        return toResource;
    }

    /**
     * Returns <tt>true</tt> if the message is encrypted and <tt>false</tt> if 
     * not.
     * @return <tt>true</tt> if the message is encrypted and <tt>false</tt> if 
     * not.
     */
    public boolean isMessageEncrypted()
    {
        return isMessageEncrypted;
    }

    /**
     * Sets the message encrypted flag of the event.
     * @param isMessageEncrypted the value to be set.
     */
    public void setMessageEncrypted(boolean isMessageEncrypted)
    {
        this.isMessageEncrypted = isMessageEncrypted;
    }
}
