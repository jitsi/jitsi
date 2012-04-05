/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.media;

import java.beans.*;
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
               RegistrationStateChangeListener,
               PropertyChangeListener,
               CallListener,
               CallChangeListener
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
     * The <tt>CallPeerListener</tt> which listens to modifications in the
     * properties/state of <tt>CallPeer</tt> so that NOTIFY requests can be sent
     * from a conference focus to its conference members to update them with
     * the latest information about the <tt>CallPeer</tt>.
     */
    private final CallPeerListener callPeerListener = new CallPeerAdapter()
    {
        /**
         * Indicates that a change has occurred in the status of the source
         * <tt>CallPeer</tt>.
         *
         * @param evt the <tt>CallPeerChangeEvent</tt> instance containing the
         * source event as well as its previous and its new status
         */
        @Override
        public void peerStateChanged(CallPeerChangeEvent evt)
        {
            CallPeer peer = evt.getSourceCallPeer();

            if (peer != null)
            {
                Call call = peer.getCall();

                if (call != null)
                {
                    CallPeerState state = peer.getState();

                    if ((state != null)
                            && !state.equals(CallPeerState.DISCONNECTED)
                            && !state.equals(CallPeerState.FAILED))
                    {
                        AbstractOperationSetTelephonyConferencing.this
                                .notifyAll(call);
                    }
                }
            }
        }
    };

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
        return createConfCall(callees, null);
    }

    /**
     * Creates a conference call with the specified callees as call peers.
     *
     * @param callees the list of addresses that we should call
     * @param group the <tt>CallGroup</tt> or null
     * @return the newly created conference call containing all CallPeers
     * @throws OperationFailedException if establishing the conference call
     * fails
     * @see OperationSetTelephonyConferencing#createConfCall(String[])
     */
    public Call createConfCall(String[] callees, CallGroup group)
        throws OperationFailedException
    {
        List<CalleeAddressT> calleeAddresses
            = new ArrayList<CalleeAddressT>(callees.length);

        for (String callee : callees)
            calleeAddresses.add(parseAddressString(callee));

        MediaAwareCallT call = createOutgoingCall();

        if(group != null && group.getCalls().size() > 0)
        {
            group.addCall(call);
            call.setCallGroup(group);
        }

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

            CallPeer peer
                = inviteCalleeToCall(calleeAddress, call, wasConferenceFocus);

            // GTalk case
            if (group != null && peer.getCall() != call)
                group.addCall(peer.getCall());

            if (group != null && call.getCallGroup() == null)
                group.addCall(call);
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
    protected abstract CallPeer inviteCalleeToCall(
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

    /**
     * Notifies this <tt>CallChangeListener</tt> that a specific
     * <tt>CallPeer</tt> has been added to a specific <tt>Call</tt>.
     *
     * @param event a <tt>CallPeerEvent</tt> which specifies the
     * <tt>CallPeer</tt> which has been added to a <tt>Call</tt>
     */
    public void callPeerAdded(CallPeerEvent event)
    {
        MediaAwareCallPeer<?,?,?> callPeer =
            (MediaAwareCallPeer<?,?,?>)event.getSourceCallPeer();

        callPeer.addCallPeerListener(callPeerListener);
        callPeer.getMediaHandler().addPropertyChangeListener(this);
        callPeersChanged(event);
    }

    /**
     * Notifies this <tt>CallChangeListener</tt> that a specific
     * <tt>CallPeer</tt> has been remove from a specific <tt>Call</tt>.
     *
     * @param event a <tt>CallPeerEvent</tt> which specifies the
     * <tt>CallPeer</tt> which has been removed from a <tt>Call</tt>
     */
    @SuppressWarnings("unchecked")
    public void callPeerRemoved(CallPeerEvent event)
    {
        MediaAwareCallPeerT callPeer =
            (MediaAwareCallPeerT) event.getSourceCallPeer();

        callPeer.removeCallPeerListener(callPeerListener);
        callPeer.getMediaHandler().removePropertyChangeListener(this);
        callPeersChanged(event);
    }

    /**
     * Notifies this <tt>CallChangeListener</tt> that the <tt>CallPeer</tt> list
     * of a specific <tt>Call</tt> has been modified by adding or removing a
     * specific <tt>CallPeer</tt>.
     *
     * @param event a <tt>CallPeerEvent</tt> which specifies the
     * <tt>CallPeer</tt> which has been added to or removed from a <tt>Call</tt>
     */
    private void callPeersChanged(CallPeerEvent event)
    {
        Call call = event.getSourceCall();

        notifyAll(call);
        notifyCallsInGroup(call);
    }

    /**
     * Notifies this <tt>PropertyChangeListener</tt> that the value of a
     * specific property of the notifier it is registered with has changed.
     *
     * @param event a <tt>PropertyChangeEvent</tt> which describes the source of
     * the event, the name of the property which has changed its value and the
     * old and new values of the property
     * @see PropertyChangeListener#propertyChange(PropertyChangeEvent)
     */
    @SuppressWarnings("unchecked")
    public void propertyChange(PropertyChangeEvent event)
    {
        String propertyName = event.getPropertyName();

        if (CallPeerMediaHandler.AUDIO_LOCAL_SSRC.equals(
                propertyName)
                || CallPeerMediaHandler.AUDIO_REMOTE_SSRC.equals(
                        propertyName)
                || CallPeerMediaHandler.VIDEO_LOCAL_SSRC.equals(
                        propertyName)
                || CallPeerMediaHandler.VIDEO_REMOTE_SSRC.equals(
                        propertyName))
        {
            Call call = ((CallPeerMediaHandler<MediaAwareCallPeerT>)
                    event.getSource()).getPeer().getCall();

            if (call != null)
            {
                notifyAll(call);
                notifyCallsInGroup(call);
            }
        }
    }

    /**
     * Notifies this <tt>CallListener</tt> that a specific incoming
     * <tt>Call</tt> has been received.
     *
     * @param event a <tt>CallEvent</tt> which specifies the newly-received
     * incoming <tt>Call</tt>
     */
    public void incomingCallReceived(CallEvent event)
    {
        callBegun(event);
    }

    /**
     * Notifies this <tt>CallListener</tt> that a specific outgoing
     * <tt>Call</tt> has been created.
     *
     * @param event a <tt>CallEvent</tt> which specifies the newly-created
     * outgoing <tt>Call</tt>
     */
    public void outgoingCallCreated(CallEvent event)
    {
        callBegun(event);
    }

    /**
     * Notifies this <tt>CallListener</tt> that a specific <tt>Call</tt> has
     * ended.
     *
     * @param event a <tt>CallEvent</tt> which specified the <tt>Call</tt> which
     * has just ended
     */
    public void callEnded(CallEvent event)
    {
        Call call = event.getSourceCall();

        /*
         * If there are still CallPeers after our realization that it has ended,
         * pretend that they are removed before that.
         */
        Iterator<? extends CallPeer> callPeerIter = call.getCallPeers();

        while (callPeerIter.hasNext())
            callPeerRemoved(
                new CallPeerEvent(
                        callPeerIter.next(),
                        call,
                        CallPeerEvent.CALL_PEER_REMOVED));

        call.removeCallChangeListener(this);
    }

    /**
     * Notifies this <tt>CallListener</tt> that a specific <tt>Call</tt> has
     * been established.
     *
     * @param event a <tt>CallEvent</tt> which specified the newly-established
     * <tt>Call</tt>
     */
    protected void callBegun(CallEvent event)
    {
        Call call = event.getSourceCall();

        call.addCallChangeListener(this);

        /*
         * If there were any CallPeers in the Call prior to our realization that
         * it has begun, pretend that they are added afterwards.
         */
        Iterator<? extends CallPeer> callPeerIter = call.getCallPeers();

        while (callPeerIter.hasNext())
            callPeerAdded(
                new CallPeerEvent(
                        callPeerIter.next(),
                        call,
                        CallPeerEvent.CALL_PEER_ADDED));
    }

    /**
     * Notifies this <tt>CallChangeListener</tt> that a specific <tt>Call</tt>
     * has changed its state. Does nothing.
     *
     * @param event a <tt>CallChangeEvent</tt> which specifies the <tt>Call</tt>
     * which has changed its state, the very state which has been changed and
     * the values of the state before and after the change
     */
    public void callStateChanged(CallChangeEvent event)
    {
    }

    /**
     * Notify the <tt>Call</tt>s in the <tt>CallGroup</tt> if any.
     *
     * @param call the <tt>Call</tt>
     */
    private void notifyCallsInGroup(Call call)
    {
        if(call.getCallGroup() != null)
        {
            CallGroup group = call.getCallGroup();
            for(Call c : group.getCalls())
            {
                if(c == call)
                    continue;

                AbstractOperationSetTelephonyConferencing<?,?,?,?,?> opSet =
                    (AbstractOperationSetTelephonyConferencing<?,?,?,?,?>)
                    c.getProtocolProvider().getOperationSet(
                        OperationSetTelephonyConferencing.class);
                if(opSet != null)
                {
                    opSet.notifyAll(c);
                }
            }
        }
    }

    /**
     * Notifies all CallPeer associated with and established in a
     * specific call for conference information.
     *
     * @param call the <tt>Call</tt>
     */
    protected abstract void notifyAll(Call call);
}
