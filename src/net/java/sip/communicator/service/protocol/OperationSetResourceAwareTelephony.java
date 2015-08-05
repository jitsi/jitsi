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
 * The <tt>OperationSetResourceAwareTelephony</tt> defines methods for creating
 * a call toward a specific resource, from which a callee is connected.
 *
 * @author Yana Stamcheva
 */
public interface OperationSetResourceAwareTelephony
    extends OperationSet
{
    /**
     * Creates a new <tt>Call</tt> and invites a specific <tt>CallPeer</tt>
     * given by her <tt>Contact</tt> on a specific <tt>ContactResource</tt> to
     * it.
     *
     * @param callee the address of the callee who we should invite to a new
     * call
     * @param calleeResource the specific resource to which the invite should be
     * sent
     * @return a newly created <tt>Call</tt>. The specified <tt>callee</tt> is
     * available in the <tt>Call</tt> as a <tt>CallPeer</tt>
     * @throws OperationFailedException with the corresponding code if we fail
     * to create the call
     */
    public Call createCall(Contact callee, ContactResource calleeResource)
        throws OperationFailedException;

    /**
     * Creates a new <tt>Call</tt> and invites a specific <tt>CallPeer</tt>
     * given by her <tt>Contact</tt> on a specific <tt>ContactResource</tt> to
     * it.
     *
     * @param callee the address of the callee who we should invite to a new
     * call
     * @param calleeResource the specific resource to which the invite should be
     * sent
     * @return a newly created <tt>Call</tt>. The specified <tt>callee</tt> is
     * available in the <tt>Call</tt> as a <tt>CallPeer</tt>
     * @throws OperationFailedException with the corresponding code if we fail
     * to create the call
     */
    public Call createCall(String callee, String calleeResource)
        throws  OperationFailedException;

    /**
     * Creates a new <tt>Call</tt> and invites a specific <tt>CallPeer</tt> to
     * it given by her <tt>String</tt> URI.
     *
     * @param uri the address of the callee who we should invite to a new
     * <tt>Call</tt>
     * @param calleeResource the specific resource to which the invite should be
     * sent
     * @param conference the <tt>CallConference</tt> in which the newly-created
     * <tt>Call</tt> is to participate
     * @return a newly created <tt>Call</tt>. The specified <tt>callee</tt> is
     * available in the <tt>Call</tt> as a <tt>CallPeer</tt>
     * @throws OperationFailedException with the corresponding code if we fail
     * to create the call
     */
    public Call createCall(String uri, String calleeResource,
        CallConference conference)
        throws OperationFailedException;

    /**
     * Creates a new <tt>Call</tt> and invites a specific <tt>CallPeer</tt>
     * given by her <tt>Contact</tt> to it.
     *
     * @param callee the address of the callee who we should invite to a new
     * call
     * @param calleeResource the specific resource to which the invite should be
     * sent
     * @param conference the <tt>CallConference</tt> in which the newly-created
     * <tt>Call</tt> is to participate
     * @return a newly created <tt>Call</tt>. The specified <tt>callee</tt> is
     * available in the <tt>Call</tt> as a <tt>CallPeer</tt>
     * @throws OperationFailedException with the corresponding code if we fail
     * to create the call
     */
    public Call createCall(Contact callee, ContactResource calleeResource,
                            CallConference conference)
        throws OperationFailedException;
}
