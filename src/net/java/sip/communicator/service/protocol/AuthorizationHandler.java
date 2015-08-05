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
 * Instances of this interface are used by the protocol provider in order to
 * make the user act upon requests coming from contacts that would like to
 * add us to their contact list or simply track oour presence status, or
 * whenever a subscription request has failed for a particular contact because
 * we need to first generate an authorization request demanding permission to
 * subscibe.
 * <p>
 * The <tt>AuthorizationHandler</tt> is to be implmented by the User Interface
 * as all its methods would require user intervention.
 *<p>
 * Note that some protocols do not support authorizations or allow them to be
 * turned off. In such cases the methods from this interface will never be
 * called.
 * <p>
 * @author Emil Ivov
 */
public interface AuthorizationHandler
{
    /**
     * Called by the protocol provider whenever someone would like to add us to
     * their contact list.
     * <p>
     * The returned AuthorizationResponse object is to be created by the
     * implementation of this interface, and it should contain a reason
     * phrase (especially in the case of a negative response) that will be sent
     * to the remote user.
     * <p>
     * Note that some protocols do not support authorizations or allow them to
     * be turned off. In such cases the method will never be called.
     * <p>
     * @param req the authorization request that we should act upon.
     * @param sourceContact a reference to the Contact demanding authorization
     * @return a new authorization response instance indicating whether
     * or not the request has been accepted and (if applicable) a reason for
     * turning it down.
     */
    public AuthorizationResponse processAuthorisationRequest(
                AuthorizationRequest req, Contact sourceContact);
    /**
     * Called by the protocol provider, this method should be implemented by the
     * user interface. The method will be called any time when the user has
     * tried to add a contact to the contact list and this contact requires
     * authorization. The returned AuthorizationRequest object is to be created
     * by the implementation of this interface, and it should also contain a
     * reason phrase that will be sent to the remote user as well as an
     * inidication of whether the specified contact should on its turn be
     * authorized to add us to their contact list. In case the user would prefer
     * to cancel the whole process, this method is to return null (this will
     * be interpreted the same way as a response withe  an IGNORED response
     * code). This would subsequently lead to a SubscriptionFailedEvent.
     * <p>
     * Note that some protocols do not support authorizations or allow them to
     * be turned off. In such cases the method will never be called.
     * <p>
     * @param contact the <tt>Contact</tt> whose authorization we'll be
     * requesting.
     *
     * @return the <tt>AuthorizationRequest</tt> instance that the user
     * interface has created, and which contains a reason phrase and/or a
     * pre-request authorization grant.
     */
    public AuthorizationRequest createAuthorizationRequest(
                                                             Contact contact );

    /**
     * Called by the protocol provider, this method should be implemented by the
     * user interface. The method will be called any whenever someone acts
     * upone an authorization request that we have previously sent.
     * @param response the <tt>AuthorizationResponse</tt> that we have received.
     * @param sourceContact a reference to the Contact demanding authorization
     */
    public void processAuthorizationResponse(
                AuthorizationResponse response, Contact sourceContact);
}
