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
 * Provides operations necessary to create and handle conferencing calls through
 * a video bridge.
 *
 * @author Yana Stamcheva
 */
public interface OperationSetVideoBridge
    extends OperationSet
{
    /**
     * The name of the property under which the user may specify if
     * video bridge should be disabled.
     */
    public static final String IS_VIDEO_BRIDGE_DISABLED
        = "net.java.sip.communicator.service.protocol.VIDEO_BRIDGE_DISABLED";

    /**
     * Creates a conference call with the specified callees as call peers via a
     * video bridge provided by the parent Jabber provider.
     *
     * @param callees the list of addresses that we should call
     * @return the newly created conference call containing all CallPeers
     * @throws OperationFailedException if establishing the conference call
     * fails
     * @throws OperationNotSupportedException if the provider does not have any
     * conferencing features.
     */
    public Call createConfCall(String[] callees)
        throws OperationFailedException,
               OperationNotSupportedException;

    /**
     * Invites the callee represented by the specified uri to an already
     * existing call. The difference between this method and createConfCall is
     * that inviteCalleeToCall allows a user to transform an existing 1-to-1
     * call into a conference call, or add new peers to an already established
     * conference.
     *
     * @param uri the callee to invite to an existing conf call.
     * @param call the call that we should invite the callee to.
     * @return the CallPeer object corresponding to the callee represented by
     * the specified uri.
     * @throws OperationFailedException if inviting the specified callee to the
     * specified call fails
     * @throws OperationNotSupportedException if allowing additional callees to
     * a pre-established call is not supported.
     */
    public CallPeer inviteCalleeToCall(String uri, Call call)
        throws OperationFailedException,
               OperationNotSupportedException;

    /**
     * Indicates if there's an active video bridge available at this moment. The
     * Jabber provider may announce support for video bridge, but it should not
     * be used for calling until it becomes actually active.
     *
     * @return <tt>true</tt> to indicate that there's currently an active
     * available video bridge, <tt>false</tt> - otherwise
     */
    public boolean isActive();
}
