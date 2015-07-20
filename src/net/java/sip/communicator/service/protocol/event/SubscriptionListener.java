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

/**
 * Instances of this interface would listen for events generated as a result
 * of creating or removing a presence subscription through the subscribe and
 * unsubscribe methods of the OperationSetPresence and
 * OperationSetPersistentPresence operation sets.
 * <p>
 * Note that these are events that are most often triggered by remote/server
 * side messages. This means that users of the protocol provider service (such
 * as the User Interface for example) should wait for one of them before
 * announcing a subscription as created or deleted (i.e. before showing or
 * removing a user in/from a displayed contact list).
 * <p>
 * @author Emil Ivov
 */
public interface SubscriptionListener
    extends EventListener
{
    /**
     * Indicates that a subscription has been successfully created and accepted
     * by the remote party.
     * @param evt the SubscriptionEvent containing the corresponding contact
     */
    public void subscriptionCreated(SubscriptionEvent evt);

    /**
     * Indicates that a subscription has failed and/or was not accepted by the
     * remote party.
     * @param evt the SubscriptionEvent containing the corresponding contact
     */
    public void subscriptionFailed(SubscriptionEvent evt);

    /**
     * Indicates that a subscription has been successfully removed and that
     * the remote party has acknowledged its removal.
     * @param evt the SubscriptionEvent containing the corresponding contact
     */
    public void subscriptionRemoved(SubscriptionEvent evt);

    /**
     * Indicates that a contact/subscription has been moved from one server
     * stored group to another. The method would only be called by
     * implementations of OperationSetPersistentPresence as non persistent
     * presence operation sets do not support the notion of server stored groups.
     *
     * @param evt a reference to the SubscriptionMovedEvent containing previous
     * and new parents as well as a ref to the source contact.
     */
    public void subscriptionMoved(SubscriptionMovedEvent evt);

    /**
     * Indicates that a subscription has been successfully resolved and that
     * the server has acknowledged taking it into account.
     * @param evt the SubscriptionEvent containing the source contact
     */
    public void subscriptionResolved(SubscriptionEvent evt);

    /**
     * Indicates that the source contact has had one of its properties changed.
     * @param evt the <tt>ContactPropertyChangeEvent</tt> containing the source
     * contact and the old and new values of the changed property.
     */
    public void contactModified(ContactPropertyChangeEvent evt);
}
