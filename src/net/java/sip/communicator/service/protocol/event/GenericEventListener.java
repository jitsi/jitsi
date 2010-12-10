/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.event;

/**
 * A listener that would gather and notify for incoming generic
 * events.
 *
 * @author Damian Minkov
 * @author Emil Ivov
 */
public interface GenericEventListener
{
    /**
     * Indicates that an incoming <tt>GenericEvent</tt> has been received.
     *
     * @param event the incoming event.
     */
    public void notificationReceived(GenericEvent event);

    /**
     * Indicates that a <tt>GenericEvent</tt> we previously tried to send
     * has not been delivered.
     *
     * @param event the <tt>GenericEvent</tt> instance describing the event
     * that we couldn't send.
     */
    public void notificationDeliveryFailed(GenericEvent event);


}
