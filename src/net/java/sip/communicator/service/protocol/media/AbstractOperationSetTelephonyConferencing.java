/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.media;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;

/**
 * Represents a default implementation of
 * <tt>OperationSetTelephonyConferencing</tt> in order to make it easier for
 * implementers to provide complete solutions while focusing on
 * implementation-specific details.
 *
 * @param <ProtocolProviderServiceT>
 * @param <OperationSetBasicTelephonyT>
 * @param <MediaAwareCallT>
 * @param <MediaAwareCallPeerT>
 * @param <CalleeAddressT>
 *
 * @author Lyubomir Marinov
 */
public abstract class AbstractOperationSetTelephonyConferencing<
        ProtocolProviderServiceT extends ProtocolProviderService,
        OperationSetBasicTelephonyT
                extends OperationSetBasicTelephony<ProtocolProviderServiceT>,
        MediaAwareCallT
                extends MediaAwareCall<
                        MediaAwareCallPeerT,
                        OperationSetBasicTelephonyT,
                        ProtocolProviderServiceT>,
        MediaAwareCallPeerT
                extends MediaAwareCallPeer<
                        MediaAwareCallT,
                        ?,
                        ProtocolProviderServiceT>,
        CalleeAddressT extends Object>
    implements OperationSetTelephonyConferencing,
               RegistrationStateChangeListener
{

    /**
     * The <tt>OperationSetBasicTelephony</tt> implementation which this
     * instance uses to carry out tasks such as establishing <tt>Call</tt>s.
     */
    private OperationSetBasicTelephonyT basicTelephony;

    /**
     * The <tt>ProtocolProviderService</tt> implementation which created this
     * instance and for which telephony conferencing services are being provided
     * by this instance.
     */
    protected final ProtocolProviderServiceT parentProvider;

    /**
     * Initializes a new <tt>AbstractOperationSetTelephonyConferencing</tt>
     * instance which is to provide telephony conferencing services for the
     * specified <tt>ProtocolProviderService</tt> implementation.
     *
     * @param parentProvider the <tt>ProtocolProviderService</tt> implementation
     * which has requested the creation of the new instance and for which the
     * new instance is to provide telephony conferencing services
     */
    protected AbstractOperationSetTelephonyConferencing(
            ProtocolProviderServiceT parentProvider)
    {
        this.parentProvider = parentProvider;
        this.parentProvider.addRegistrationStateChangeListener(this);
    }

    /**
     * Notifies this <tt>OperationSetTelephonyConferencing</tt> that its
     * <tt>basicTelephony</tt> property has changed its value from a specific
     * <tt>oldValue</tt> to a specific <tt>newValue</tt>
     *
     * @param oldValue the old value of the <tt>basicTelephony</tt> property
     * @param newValue the new value of the <tt>basicTelephony</tt> property
     */
    protected void basicTelephonyChanged(
            OperationSetBasicTelephonyT oldValue,
            OperationSetBasicTelephonyT newValue)
    {
    }

    /**
     * Creates a conference call with the specified callees as call peers.
     *
     * @param callees the list of addresses that we should call
     * @return the newly created conference call containing all CallPeers
     * @throws OperationFailedException if establishing the conference call
     * fails
     * @see OperationSetTelephonyConferencing#createConfCall(String[])
     */
    public Call createConfCall(String[] callees)
        throws OperationFailedException
    {
        List<CalleeAddressT> calleeAddresses
            = new ArrayList<CalleeAddressT>(callees.length);

        for (String callee : callees)
            calleeAddresses.add(parseAddressString(callee));

        MediaAwareCallT call = createOutgoingCall();

        call.setConferenceFocus(true);

        for (CalleeAddressT calleeAddress : calleeAddresses)
        {
            boolean wasConferenceFocus;

            if (call.isConferenceFocus())
                wasConferenceFocus = true;
            else
            {
                wasConferenceFocus = false;
                call.setConferenceFocus(true);
            }
            inviteCalleeToCall(calleeAddress, call, wasConferenceFocus);
        }
        return call;
    }

    /**
     * Creates a new outgoing <tt>Call</tt> into which conference callees are to
     * be invited by this <tt>OperationSetTelephonyConferencing</tt>.
     *
     * @return a new outgoing <tt>Call</tt> into which conference callees are to
     * be invited by this <tt>OperationSetTelephonyConferencing</tt>
     * @throws OperationFailedException if anything goes wrong
     */
    protected abstract MediaAwareCallT createOutgoingCall()
        throws OperationFailedException;

    /**
     * Gets the <tt>OperationSetBasicTelephony</tt> implementation which this
     * instance uses to carry out tasks such as establishing <tt>Call</tt>s.
     *
     * @return the <tt>OperationSetBasicTelephony</tt> implementation which this
     * instance uses to carry out tasks such as establishing <tt>Call</tt>s
     */
    public OperationSetBasicTelephonyT getBasicTelephony()
    {
        return basicTelephony;
    }

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
     */
    public CallPeer inviteCalleeToCall(String uri, Call call)
        throws OperationFailedException
    {
        CalleeAddressT calleeAddress = parseAddressString(uri);
        @SuppressWarnings("unchecked")
        MediaAwareCallT mediaAwareCall = (MediaAwareCallT) call;
        boolean wasConferenceFocus;

        if (mediaAwareCall.isConferenceFocus())
            wasConferenceFocus = true;
        else
        {
            wasConferenceFocus = false;
            mediaAwareCall.setConferenceFocus(true);
        }
        return
            inviteCalleeToCall(
                calleeAddress,
                mediaAwareCall,
                wasConferenceFocus);
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
    protected abstract MediaAwareCallPeerT inviteCalleeToCall(
            CalleeAddressT calleeAddress,
            MediaAwareCallT call,
            boolean wasConferenceFocus)
        throws OperationFailedException;

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
    protected abstract CalleeAddressT parseAddressString(
            String calleeAddressString)
        throws OperationFailedException;

    /**
     * Notifies this <tt>RegistrationStateChangeListener</tt> that the
     * <tt>ProtocolProviderSerivce</tt> it is registered with has changed its
     * registration state.
     *
     * @param event a <tt>RegistrationStateChangeEvent</tt> which specifies the
     * old and the new value of the registration state of the
     * <tt>ProtocolProviderService</tt> this
     * <tt>RegistrationStateChangeListener</tt> listens to
     */
    public void registrationStateChanged(RegistrationStateChangeEvent event)
    {
        RegistrationState newState = event.getNewState();

        if (RegistrationState.REGISTERED.equals(newState))
        {
            @SuppressWarnings("unchecked")
            OperationSetBasicTelephonyT basicTelephony
                = (OperationSetBasicTelephonyT)
                    parentProvider.getOperationSet(
                            OperationSetBasicTelephony.class);

            if (this.basicTelephony != basicTelephony)
            {
                OperationSetBasicTelephonyT oldValue = this.basicTelephony;

                this.basicTelephony = basicTelephony;
                basicTelephonyChanged(oldValue, this.basicTelephony);
            }
        }
        else if (RegistrationState.UNREGISTERED.equals(newState))
        {
            if (basicTelephony != null)
            {
                OperationSetBasicTelephonyT oldValue = basicTelephony;

                basicTelephony = null;
                basicTelephonyChanged(oldValue, null);
            }
        }
    }
}
