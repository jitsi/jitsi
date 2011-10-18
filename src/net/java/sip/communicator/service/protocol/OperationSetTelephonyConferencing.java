/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

/**
 * Provides operations necessary to create and handle conferencing calls.
 *
 * @author Emil Ivov
 * @author Lubomir Marinov
 */
public interface OperationSetTelephonyConferencing
    extends OperationSet
{
    /**
     * Creates a conference call with the specified callees as call peers.
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
     * that inviteCalleeToCall allows a user to transform an existing 1 to 1
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
}
