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
package net.java.sip.communicator.impl.protocol.icq;

import net.java.sip.communicator.service.protocol.*;
import net.kano.joustsim.*;

/**
 * Contains methods that would allow service users to re-request authorizations
 * to add a contact to their contact list or, send them an authorization before
 * having been asked.
 *
 * @author Damian Minkov
 */
public class OperationSetExtendedAuthorizationsIcqImpl
    implements OperationSetExtendedAuthorizations
{

    /**
     * A callback to the ICQ provider that created us.
     */
    private ProtocolProviderServiceIcqImpl icqProvider = null;


    /**
     * Creates a new instance of OperationSetExtendedAuthorizationsIcqImpl
     * @param icqProvider IcqProtocolProviderServiceImpl
     */
    public OperationSetExtendedAuthorizationsIcqImpl(
        ProtocolProviderServiceIcqImpl icqProvider)
    {
        this.icqProvider = icqProvider;
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
    public void reRequestAuthorization(AuthorizationRequest request, Contact contact)
        throws OperationFailedException
    {
        assertConnected();

        if(! (contact instanceof ContactIcqImpl) )
            throw new IllegalArgumentException(
                "Argument is not an icq contact (contact=" + contact + ")");

        icqProvider.getAimConnection().getSsiService().requestBuddyAuthorization(
            new Screenname(contact.getAddress()),
            request.getReason());
    }

    /**
     * Send a positive authorization to <tt>contact</tt> thus allowing them to
     * add us to their contact list without needing to first request an
     * authorization.
     *
     * @param contact the <tt>Contact</tt> whom we're granting authorization
     * prior to receiving a request.
     * @throws OperationFailedException if we fail sending the authorization.
     */
    public void explicitAuthorize(Contact contact)
        throws OperationFailedException
    {
        assertConnected();

        if(! (contact instanceof ContactIcqImpl) )
            throw new IllegalArgumentException(
                "Argument is not an icq contact (contact=" + contact + ")");

        icqProvider.getAimConnection().getSsiService().sendFutureBuddyAuthorization(
            new Screenname(contact.getAddress()),
            "");
    }

    /**
     * Utility method throwing an exception if the icq stack is not properly
     * initialized.
     * @throws java.lang.IllegalStateException if the underlying ICQ stack is
     * not registered and initialized.
     */
    private void assertConnected() throws IllegalStateException
    {
        if (icqProvider == null)
            throw new IllegalStateException(
                "The icq provider must be non-null and signed on the ICQ "
                +"service before being able to communicate.");
        if (!icqProvider.isRegistered())
            throw new IllegalStateException(
                "The icq provider must be signed on the ICQ service before "
                +"being able to communicate.");
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
        if(contact == null || ! (contact instanceof ContactIcqImpl) )
            throw new IllegalArgumentException(
                "Argument is not an icq contact (contact=" + contact + ")");

        if(((ContactIcqImpl)contact).getJoustSimBuddy()
            .isAwaitingAuthorization())
        {
            return SubscriptionStatus.SubscriptionPending;
        }
        else
        {
            return SubscriptionStatus.Subscribed;
        }
    }
}
