/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.event;

/**
 * Represents a default implementation of <code>SubscriptionListener</code>
 * which performs no processing of the received events and allows extenders to
 * easily implement the interface in question by just overriding the methods
 * they are interested in.
 *
 * @author Lubomir Marinov
 */
public class SubscriptionAdapter
    implements SubscriptionListener
{

    /*
     * Implements
     * SubscriptionListener#contactModified(ContactPropertyChangeEvent). Does
     * nothing.
     */
    public void contactModified(ContactPropertyChangeEvent evt)
    {
    }

    /*
     * Implements SubscriptionListener#subscriptionCreated(SubscriptionEvent).
     * Does nothing.
     */
    public void subscriptionCreated(SubscriptionEvent evt)
    {
    }

    /*
     * Implements SubscriptionListener#subscriptionFailed(SubscriptionEvent).
     * Does nothing.
     */
    public void subscriptionFailed(SubscriptionEvent evt)
    {
    }

    /*
     * Implements SubscriptionListener#subscriptionMoved(SubscriptionMovedEvent).
     * Does nothing.
     */
    public void subscriptionMoved(SubscriptionMovedEvent evt)
    {
    }

    /*
     * Implements SubscriptionListener#subscriptionRemoved(SubscriptionEvent).
     * Does nothing.
     */
    public void subscriptionRemoved(SubscriptionEvent evt)
    {
    }

    /*
     * Implements SubscriptionListener#subscriptionResolved(SubscriptionEvent).
     * Does nothing.
     */
    public void subscriptionResolved(SubscriptionEvent evt)
    {
    }
}
