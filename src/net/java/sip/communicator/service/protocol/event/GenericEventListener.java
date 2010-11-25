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
 */
public interface GenericEventListener
{
    /**
     * Notify for incoming <tt>GenericEvent</tt>.
     * @param event the incoming event.
     */
    public void notify(GenericEvent event);
}
