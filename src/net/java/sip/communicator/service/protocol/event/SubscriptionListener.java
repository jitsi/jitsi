/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
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
}
