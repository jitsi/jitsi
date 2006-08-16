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
 * <tt>MessageReceivedEvent</tt>s indicate reception of an instant message.
 *
 * @author Emil Ivov
 */
public class MessageReceivedEvent
    extends EventObject
{
    /**
     * The contact that has sent this message.
     */
    private Contact from = null;

    /**
     * A timestamp indicating the exact date when the event occurred.
     */
    private Date timestamp = null;

    /**
     * Creates a <tt>MessageReceivedEvent</tt> representing reception of the
     * <tt>source</tt> message received from the specified <tt>from</tt>
     * contact.
     *
     * @param source the <tt>Message</tt> whose reception this event represents.
     * @param from the <tt>Contact</tt> that has sent this message.
     * @param timestamp the exact date when the event ocurred.
     */
    public MessageReceivedEvent(Message source, Contact from, Date timestamp)
    {
        super(source);

        this.from = from;
        this.timestamp = timestamp;
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
     * @return the <tt>Message</tt> that triggered this event.
     */
    public Message getSourceMessage()
    {
        return (Message)getSource();
    }

    /**
     * A timestamp indicating the exact date when the event ocurred.
     * @return a Date indicating when the event ocurred.
     */
    public Date getTimestamp()
    {
        return timestamp;
    }
}
