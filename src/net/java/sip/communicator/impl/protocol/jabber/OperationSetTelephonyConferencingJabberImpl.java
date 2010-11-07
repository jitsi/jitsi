/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.media.*;

/**
 * Implements <tt>OperationSetTelephonyConferencing</tt> for Jabber.
 *
 * @author Lyubomir Marinov
 */
public class OperationSetTelephonyConferencingJabberImpl
    extends AbstractOperationSetTelephonyConferencing<
            ProtocolProviderServiceJabberImpl,
            OperationSetBasicTelephonyJabberImpl,
            CallJabberImpl,
            CallPeerJabberImpl,
            String>
{

    /**
     * Initializes a new <tt>OperationSetTelephonyConferencingJabberImpl</tt>
     * instance which is to provide telephony conferencing services for the
     * specified Jabber <tt>ProtocolProviderService</tt> implementation.
     *
     * @param parentProvider the Jabber <tt>ProtocolProviderService</tt>
     * implementation which has requested the creation of the new instance and
     * for which the new instance is to provide telephony conferencing services
     */
    public OperationSetTelephonyConferencingJabberImpl(
        ProtocolProviderServiceJabberImpl parentProvider)
    {
        super(parentProvider);
    }

    /**
     * Creates a new outgoing <tt>Call</tt> into which conference callees are to
     * be invited by this <tt>OperationSetTelephonyConferencing</tt>.
     *
     * @return a new outgoing <tt>Call</tt> into which conference callees are to
     * be invited by this <tt>OperationSetTelephonyConferencing</tt>
     * @throws OperationFailedException if anything goes wrong
     */
    protected CallJabberImpl createOutgoingCall()
        throws OperationFailedException
    {
        return new CallJabberImpl(getBasicTelephony());
    }

    /**
     * Invites a callee with a specific address to be joined in a specific
     * <tt>Call</tt> in the sense of conferencing.
     *
     * @param calleeAddress the address of the callee to be invited to the
     * specified existing <tt>Call</tt>
     * @param call the existing <tt>Call</tt> to invite the callee with the
     * specified address to
     * @param wasConferenceFocus the value of the <tt>conferenceFocus</tt>
     * property of the specified <tt>call</tt> prior to the request to invite
     * the specified <tt>calleeAddress</tt>
     * @return a new <tt>CallPeer</tt> instance which describes the signaling
     * and the media streaming of the newly-invited callee within the specified
     * <tt>Call</tt>
     * @throws OperationFailedException if inviting the specified callee to the
     * specified call fails
     */
    protected CallPeerJabberImpl inviteCalleeToCall(
            String calleeAddress,
            CallJabberImpl call,
            boolean wasConferenceFocus)
        throws OperationFailedException
    {
        return getBasicTelephony().createOutgoingCall(call, calleeAddress);
    }

    /**
     * Parses a <tt>String</tt> value which represents a callee address
     * specified by the user into an object which is to actually represent the
     * callee during the invitation to a conference <tt>Call</tt>.
     *
     * @param calleeAddressString a <tt>String</tt> value which represents a
     * callee address to be parsed into an object which is to actually represent
     * the callee during the invitation to a conference <tt>Call</tt>
     * @return an object which is to actually represent the specified
     * <tt>calleeAddressString</tt> during the invitation to a conference
     * <tt>Call</tt>
     * @throws OperationFailedException if parsing the specified
     * <tt>calleeAddressString</tt> fails
     */
    protected String parseAddressString(String calleeAddressString)
        throws OperationFailedException
    {
        return getBasicTelephony().getFullCalleeURI(calleeAddressString);
    }
}
