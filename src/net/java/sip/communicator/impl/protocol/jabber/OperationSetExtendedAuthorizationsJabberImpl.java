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
package net.java.sip.communicator.impl.protocol.jabber;

import net.java.sip.communicator.service.protocol.*;

import org.jivesoftware.smack.*;
import org.jivesoftware.smack.packet.*;

/**
 * Extended authorization implementation for jabber provider.
 *
 * @author Damian Minkov
 */
public class OperationSetExtendedAuthorizationsJabberImpl
    implements OperationSetExtendedAuthorizations
{
    /**
     * A reference to the persistent presence operation set that we use
     * to match incoming messages to <tt>Contact</tt>s and vice versa.
     */
    private OperationSetPersistentPresenceJabberImpl opSetPersPresence = null;

    /**
     * The parent provider.
     */
    private ProtocolProviderServiceJabberImpl parentProvider;

    /**
     * Creates OperationSetExtendedAuthorizations.
     * @param opSetPersPresence the presence opset.
     * @param provider the parent provider
     */
    OperationSetExtendedAuthorizationsJabberImpl(
        ProtocolProviderServiceJabberImpl provider,
        OperationSetPersistentPresenceJabberImpl opSetPersPresence)
    {
        this.opSetPersPresence = opSetPersPresence;
        this.parentProvider = provider;
    }

    /**
     * Send a positive authorization to <tt>contact</tt> thus allowing them to
     * add us to their contact list without needing to first request an
     * authorization.
     * @param contact the <tt>Contact</tt> whom we're granting authorization
     * prior to receiving a request.
     * @throws OperationFailedException if we fail sending the authorization.
     */
    public void explicitAuthorize(Contact contact)
        throws
        OperationFailedException
    {
        opSetPersPresence.assertConnected();

        if( !(contact instanceof ContactJabberImpl) )
            throw new IllegalArgumentException(
                "The specified contact is not an jabber contact." +
                    contact);

        Presence responsePacket = new Presence(Presence.Type.subscribed);
        responsePacket.setTo(contact.getAddress());
        parentProvider.getConnection().sendPacket(responsePacket);
    }

    /**
     * Send an authorization request, requesting <tt>contact</tt> to add them
     * to our contact list?
     *
     * @param request the <tt>AuthorizationRequest</tt> that we'd like the
     * protocol provider to send to <tt>contact</tt>.
     * @param contact the <tt>Contact</tt> who we'd be asking for an
     * authorization.
     * @throws OperationFailedException if we fail sending the authorization
     * request.
     */
    public void reRequestAuthorization(AuthorizationRequest request,
                                       Contact contact)
        throws
        OperationFailedException
    {
        opSetPersPresence.assertConnected();

        if( !(contact instanceof ContactJabberImpl) )
            throw new IllegalArgumentException(
                "The specified contact is not an jabber contact." +
                    contact);

        Presence responsePacket = new Presence(Presence.Type.subscribe);
        responsePacket.setTo(contact.getAddress());
        parentProvider.getConnection().sendPacket(responsePacket);
    }

    /**
     * Returns the subscription status for the <tt>contact</tt> or
     * if not available returns null.
     * @param contact the contact to query for subscription status.
     * @return the subscription status for the <tt>contact</tt> or
     *         if not available returns null.
     */
    public SubscriptionStatus getSubscriptionStatus(Contact contact)
    {
        if( !(contact instanceof ContactJabberImpl) )
            throw new IllegalArgumentException(
                "The specified contact is not an jabber contact." +
                    contact);

        RosterEntry entry = ((ContactJabberImpl) contact).getSourceEntry();

        if(entry != null)
        {
            if((entry.getType() == RosterPacket.ItemType.none
                    || entry.getType() == RosterPacket.ItemType.from)
               && RosterPacket.ItemStatus.SUBSCRIPTION_PENDING
                    == entry.getStatus())
            {
                return SubscriptionStatus.SubscriptionPending;
            }
            else if(entry.getType() == RosterPacket.ItemType.to
                    || entry.getType() == RosterPacket.ItemType.both)
                return SubscriptionStatus.Subscribed;
            else
                return SubscriptionStatus.NotSubscribed;
        }

        return null;
    }
}
