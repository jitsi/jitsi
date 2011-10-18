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
 * <tt>MessageReceivedEvent</tt>s indicate reception of an instant message.
 *
 * @author Emil Ivov
 */
public class MessageReceivedEvent
    extends EventObject
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * An event type indicating that the message being received is a standard
     * conversation message sent by another contact.
     */
    public static final int CONVERSATION_MESSAGE_RECEIVED = 1;

    /**
     * An event type indicating that the message being received is a system
     * message being sent by the server or a system administrator.
     */
    public static final int SYSTEM_MESSAGE_RECEIVED = 2;

    /**
     * an event type indicating that the message being received is an SMS
     * message.
     */
    public static final int SMS_MESSAGE_RECEIVED = 3;

    /**
     * The contact that has sent this message.
     */
    private Contact from = null;

    /**
     * A timestamp indicating the exact date when the event occurred.
     */
    private final long timestamp;

    /**
     * The type of message event that this instance represents.
     */
    private int eventType = -1;

    /**
     * Creates a <tt>MessageReceivedEvent</tt> representing reception of the
     * <tt>source</tt> message received from the specified <tt>from</tt>
     * contact.
     *
     * @param source the <tt>Message</tt> whose reception this event represents.
     * @param from the <tt>Contact</tt> that has sent this message.
     * @param timestamp the exact date when the event ocurred.
     */
    public MessageReceivedEvent(Message source, Contact from, long timestamp)
    {
       this(source, from, timestamp, CONVERSATION_MESSAGE_RECEIVED);
    }

    /**
     * Creates a <tt>MessageReceivedEvent</tt> representing reception of the
     * <tt>source</tt> message received from the specified <tt>from</tt>
     * contact.
     *
     * @param source the <tt>Message</tt> whose reception this event represents.
     * @param from the <tt>Contact</tt> that has sent this message.
     * @param timestamp the exact date when the event occurred.
     * @param eventType the type of message event that this instance represents
     * (one of the XXX_MESSAGE_RECEIVED static fields).
     */
    public MessageReceivedEvent(Message source, Contact from,
        long timestamp, int eventType)
    {
        super(source);

        this.from = from;
        this.timestamp = timestamp;
        this.eventType = eventType;
    }

    /**
     * Returns a reference to the <tt>Contact</tt> that has send the
     * <tt>Message</tt> whose reception this event represents.
     *
     * @return a reference to the <tt>Contact</tt> that has send the
     * <tt>Message</tt> whose reception this event represents.
     */
    public Contact getSourceContact()
    {
        return from;
    }

    /**
     * Returns the message that triggered this event
     *
     * @return the <tt>Message</tt> that triggered this event.
     */
    public Message getSourceMessage()
    {
        return (Message)getSource();
    }

    /**
     * A timestamp indicating the exact date when the event occurred.
     *
     * @return a Date indicating when the event occurred.
     */
    public long getTimestamp()
    {
        return timestamp;
    }

    /**
     * Returns the type of message event represented by this event instance.
     * Message event type is one of the XXX_MESSAGE_RECEIVED fields of this
     * class.
     *
     * @return one of the XXX_MESSAGE_RECEIVED fields of this class indicating
     * the type of this event.
     */
    public int getEventType()
    {
        return eventType;
    }
}
