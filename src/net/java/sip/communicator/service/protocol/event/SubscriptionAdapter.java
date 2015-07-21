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
