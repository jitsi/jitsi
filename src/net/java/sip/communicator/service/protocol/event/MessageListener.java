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
    public void messageReceived(MessageEvent evt);

    public void messageDelivered(MessageEvent evt);

    public void messageDeliveryFailed(MessageEvent evt);
}
