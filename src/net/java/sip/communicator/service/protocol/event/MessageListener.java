/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.event;

import java.util.*;

/**
 * A listener that would gather events notifying of message delivery status.
 * Message received
 *
 * @author Emil Ivov
 */
public interface MessageListener
    extends EventListener
{
    /**
     * Called when a new incoming <tt>Message</tt> has been received.
     * @param evt the <tt>MessageReceivedEvent</tt> containing the newly
     * received message, its sender and other details.
     */
    public void messageReceived(MessageReceivedEvent evt);

    /**
     * Called when the underlying implementation has received an indication
     * that a message, sent earlier has been successfully received by the
     * destination.
     * @param evt the MessageDeliveredEvent containing the id of the message
     * that has caused the event.
     */
    public void messageDelivered(MessageDeliveredEvent evt);

    /**
     * Called to indicated that delivery of a message sent earlier has failed.
     * Reason code and phrase are contained by the <tt>MessageFailedEvent</tt>
     * @param evt the <tt>MessageFailedEvent</tt> containing the ID of the
     * message whose delivery has failed.
     */
    public void messageDeliveryFailed(MessageDeliveryFailedEvent evt);
}
