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
package net.java.sip.communicator.service.protocol;

/**
 * Contains methods that would allow service users to re-request authorizations
 * to add a contact to their contact list or, send them an authorization before
 * having been asked.
 *
 * @author Emil Ivov
 */
public interface OperationSetExtendedAuthorizations
    extends OperationSet
{
    /**
     * The available subscription of the contact.
     */
    public enum SubscriptionStatus
    {
        /**
         * Subscription state when we are not subscribed
         * for the contacts presence statuses.
         */
        NotSubscribed,
        /**
         * Subscription state when we are subscribed for the contact statuses.
         */
        Subscribed,
        /**
         * When we have subscribed for contact statuses, but haven't
         * received authorization yet.
         */
        SubscriptionPending
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
        throws OperationFailedException;

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
        throws OperationFailedException;

    /**
     * Returns the subscription status for the <tt>contact</tt> or
     * if not available returns null.
     * @param contact the contact to query for subscription status.
     * @return the subscription status for the <tt>contact</tt> or
     *         if not available returns null.
     */
    public SubscriptionStatus getSubscriptionStatus(Contact contact);
}
