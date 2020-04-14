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
 * Extends <code>OperationSetBasicTelephony</code> with advanced telephony
 * operations such as call transfer.
 *
 * @param <T> the implementation specific provider class like for example
 * <tt>ProtocolProviderServiceSipImpl</tt>.
 *
 * @author Lyubomir Marinov
 */
public interface OperationSetAdvancedTelephony<T extends ProtocolProviderService>
    extends OperationSetBasicTelephony<T>
{

    /**
     * Transfers (in the sense of call transfer) a specific <tt>CallPeer</tt> to
     * a specific callee address which already participates in an active
     * <tt>Call</tt>.
     * <p>
     * The method is suitable for providing the implementation of attended call
     * transfer (though no such requirement is imposed).
     * </p>
     *
     * @param peer the <tt>CallPeer</tt> to be transfered to the specified
     * callee address
     * @param target the address in the form of <tt>CallPeer</tt> of the callee
     * to transfer <tt>peer</tt> to
     * @throws OperationFailedException if something goes wrong.
     */
    void transfer(CallPeer peer, CallPeer target)
        throws OperationFailedException;

    /**
     * Transfers (in the sense of call transfer) a specific <tt>CallPeer</tt> to
     * a specific callee address which may or may not already be participating
     * in an active <tt>Call</tt>.
     * <p>
     * The method is suitable for providing the implementation of unattended
     * call transfer (though no such requirement is imposed).
     * </p>
     *
     * @param peer the <tt>CallPeer</tt> to be transfered to the specified
     * callee address
     * @param target the address of the callee to transfer <tt>peer</tt> to
     * @throws OperationFailedException if something goes wrong.
     */
    void transfer(CallPeer peer, String target)
        throws OperationFailedException;

    /**
     * Transfer authority used for interacting with user for unknown
     * call transfer requests.
     * @param authority transfer authority asks user for accepting a particular
     * transfer request.
     */
    public void setTransferAuthority(TransferAuthority authority);
}
