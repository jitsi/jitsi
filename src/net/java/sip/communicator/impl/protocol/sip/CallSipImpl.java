/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip;

import java.util.*;
import java.beans.*;

import javax.sip.*;
import javax.sip.address.*;
import javax.sip.message.*;

import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.service.neomedia.device.*;
import net.java.sip.communicator.service.neomedia.event.SimpleAudioLevelListener;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * A SIP implementation of the Call abstract class encapsulating SIP dialogs.
 *
 * @author Emil Ivov
 */
public class CallSipImpl
    extends AbstractCall<CallPeerSipImpl, ProtocolProviderServiceSipImpl>
    implements CallPeerListener
{
    /**
     * Our class logger.
     */
    private static final Logger logger = Logger.getLogger(CallSipImpl.class);

    /**
     * The <tt>MediaDevice</tt> which performs audio mixing for this
     * <tt>Call</tt> and its <tt>CallPeer</tt>s when the local peer represented
     * by this <tt>Call</tt> is acting as a conference focus i.e.
     * {@link #conferenceFocus} is <tt>true</tt>.
     */
    private MediaDevice conferenceAudioMixer;

    /**
     * The indicator which determines whether the local peer represented by this
     * <tt>Call</tt> is acting as a conference focus and is thus specifying the
     * &quot;isfocus&quot; parameter in the Contact headers of its outgoing SIP
     * signaling.
     */
    private boolean conferenceFocus = false;

    /**
     * Our video streaming policy.
     */
    private boolean localVideoAllowed = false;

    /**
     * A reference to the <tt>SipMessageFactory</tt> instance that we should
     * use when creating requests.
     */
    private final SipMessageFactory messageFactory;

    /**
     * A reference to the <tt>OperationSetBasicTelephonySipImpl</tt> that
     * created us;
     */
    private final OperationSetBasicTelephonySipImpl parentOpSet;

    /**
     * Holds listeners registered for level changes in local audio.
     */
    private final List<SoundLevelListener> localUserAudioLevelListeners
        = new ArrayList<SoundLevelListener>();

    /**
     * The indicator which determines whether this <tt>Call</tt> is set
     * to transmit "silence" instead of the actual media.
     */
    private boolean mute = false;

    /**
     * Device used in call will be choosen according to <tt>MediaUseCase</tt>.
     */
    MediaUseCase mediaUseCase = MediaUseCase.ANY;

    /**
     * The listener that would actually subscribe for level events from the
     * media handler if there's at least one listener in
     * <tt>localUserAudioLevelListeners</tt>.
     */
    private final SimpleAudioLevelListener localAudioLevelDelegator
        = new SimpleAudioLevelListener()
        {
            public void audioLevelChanged(int level)
            {
                fireLocalUserAudioLevelChangeEvent(level);
            }
        };


    /**
     * Crates a CallSipImpl instance belonging to <tt>sourceProvider</tt> and
     * initiated by <tt>CallCreator</tt>.
     *
     * @param parentOpSet a reference to the operation set that's creating us
     * and that we would be able to use for even dispatching.
     */
    protected CallSipImpl(OperationSetBasicTelephonySipImpl parentOpSet)
    {
        super(parentOpSet.getProtocolProvider());
        this.messageFactory = getProtocolProvider().getMessageFactory();
        this.parentOpSet = parentOpSet;

        //let's add ourselves to the calls repo. we are doing it ourselves just
        //to make sure that no one ever forgets.
        parentOpSet.getActiveCallsRepository().addCall(this);
    }

    /**
     * Adds <tt>callPeer</tt> to the list of peers in this call.
     * If the call peer is already included in the call, the method has
     * no effect.
     *
     * @param callPeer the new <tt>CallPeer</tt>
     */
    private void addCallPeer(CallPeerSipImpl callPeer)
    {
        if (getCallPeersVector().contains(callPeer))
            return;

        callPeer.addCallPeerListener(this);

        synchronized(localUserAudioLevelListeners)
        {
            // if there's someone listening for audio level events then they'd
            // also like to know about the new peer.
            if(getCallPeersVector().size() == 0)
            {
                callPeer.getMediaHandler().setLocalUserAudioLevelListener(
                                localAudioLevelDelegator);
            }
        }

        getCallPeersVector().add(callPeer);
        fireCallPeerEvent(callPeer, CallPeerEvent.CALL_PEER_ADDED);
    }

    /**
     * Removes <tt>callPeer</tt> from the list of peers in this
     * call. The method has no effect if there was no such peer in the
     * call.
     *
     * @param callPeer the <tt>CallPeer</tt> leaving the call;
     */
    private void removeCallPeer(CallPeerSipImpl callPeer)
    {
        if (!getCallPeersVector().contains(callPeer))
            return;

        getCallPeersVector().remove(callPeer);
        callPeer.removeCallPeerListener(this);

        synchronized(localUserAudioLevelListeners)
        {
            // remove sound level listeners from the peer
            callPeer.getMediaHandler().setLocalUserAudioLevelListener(null);
        }

        try
        {
            fireCallPeerEvent(callPeer,
                CallPeerEvent.CALL_PEER_REMOVED);
        }
        finally
        {

            /*
             * The peer should loose its state once it has finished
             * firing its events in order to allow the listeners to undo.
             */
            callPeer.setCall(null);
        }

        if (getCallPeersVector().size() == 0)
            setCallState(CallState.CALL_ENDED);
    }

    /**
     * Dummy implementation of a method (inherited from CallPeerListener)
     * that we don't need.
     *
     * @param evt unused.
     */
    public void peerImageChanged(CallPeerChangeEvent evt)
    {
        //does not concern us
    }

    /**
     * Dummy implementation of a method (inherited from CallPeerListener)
     * that we don't need.
     *
     * @param evt unused.
     */
    public void peerAddressChanged(CallPeerChangeEvent evt)
    {
      //does not concern us
    }

    /**
     * Dummy implementation of a method (inherited from CallPeerListener)
     * that we don't need.
     *
     * @param evt unused.
     */
    public void peerTransportAddressChanged(
        CallPeerChangeEvent evt)
    {
      //does not concern us
    }

    /**
     * Dummy implementation of a method (inherited from CallPeerListener)
     * that we don't need.
     *
     * @param evt unused.
     */
    public void peerDisplayNameChanged(CallPeerChangeEvent evt)
    {
      //does not concern us
    }

    /**
     * Verifies whether the call peer has entered a state.
     *
     * @param evt The <tt>CallPeerChangeEvent</tt> instance containing
     * the source event as well as its previous and its new status.
     */
    public void peerStateChanged(CallPeerChangeEvent evt)
    {
        CallPeerState newState = (CallPeerState) evt.getNewValue();

        if (CallPeerState.DISCONNECTED.equals(newState)
                || CallPeerState.FAILED.equals(newState))
        {
            removeCallPeer((CallPeerSipImpl) evt.getSourceCallPeer());
        }
        else if (CallPeerState.CONNECTED.equals(newState)
                || CallPeerState.CONNECTING_WITH_EARLY_MEDIA.equals(newState))
        {
            setCallState(CallState.CALL_IN_PROGRESS);
        }
        else if (CallPeerState.REFERRED.equals(newState))
        {
            setCallState(CallState.CALL_REFERRED);
        }
    }

    /**
     * Returns <tt>true</tt> if <tt>dialog</tt> matches the jain sip dialog
     * established with one of the peers in this call.
     *
     * @param dialog the dialog whose corresponding peer we're looking for.
     *
     * @return true if this call contains a call peer whose jain sip
     * dialog is the same as the specified and false otherwise.
     */
    public boolean contains(Dialog dialog)
    {
        return findCallPeer(dialog) != null;
    }

    /**
     * Returns the call peer whose associated jain sip dialog matches
     * <tt>dialog</tt>.
     *
     * @param dialog the jain sip dialog whose corresponding peer we're looking
     * for.
     * @return the call peer whose jain sip dialog is the same as the specified
     * or null if no such call peer was found.
     */
    public CallPeerSipImpl findCallPeer(Dialog dialog)
    {
        Iterator<CallPeerSipImpl> callPeers = this.getCallPeers();

        if (logger.isTraceEnabled())
        {
            logger.trace("Looking for peer with dialog: " + dialog
                + "among " + getCallPeerCount() + " calls");
        }

        while (callPeers.hasNext())
        {
            CallPeerSipImpl cp = callPeers.next();

            if (cp.getDialog() == dialog)
            {
                if (logger.isTraceEnabled())
                    logger.trace("Returing cp=" + cp);
                return cp;
            }
            else
            {
                if (logger.isTraceEnabled())
                    logger.trace("Ignoring cp=" + cp + " because cp.dialog="
                            + cp.getDialog() + " while dialog=" + dialog);
            }
        }

        return null;
    }

    /**
     * Returns a reference to the <tt>ProtocolProviderServiceSipImpl</tt>
     * instance that created this call.
     *
     * @return a reference to the <tt>ProtocolProviderServiceSipImpl</tt>
     * instance that created this call.
     */
    @Override
    public ProtocolProviderServiceSipImpl getProtocolProvider()
    {
        return (ProtocolProviderServiceSipImpl)super.getProtocolProvider();
    }

    /**
     * Returns a reference to the <tt>OperationSetBasicTelephonySipImpl</tt>
     * instance that created this call.
     *
     * @return a reference to the <tt>OperationSetBasicTelephonySipImpl</tt>
     * instance that created this call.
     */
    public OperationSetBasicTelephonySipImpl getParentOperationSet()
    {
        return parentOpSet;
    }

    /**
     * Creates a <tt>CallPeerSipImpl</tt> from <tt>calleeAddress</tt> and sends
     * them an invite request. The invite request will be initialized according
     * to any relevant parameters in the <tt>cause</tt> message (if different
     * from <tt>null</tt>) that is the reason for creating this call.
     *
     * @param calleeAddress the party that we would like to invite to this call.
     * @param cause the message (e.g. a Refer request), that is the reason for
     * this invite or <tt>null</tt> if this is a user-initiated invitation
     *
     * @return the newly created <tt>CallPeer</tt> corresponding to
     * <tt>calleeAddress</tt>. All following state change events will be
     * delivered through this call peer.
     *
     * @throws OperationFailedException  with the corresponding code if we fail
     *  to create the call.
     */
    public CallPeerSipImpl invite(Address                   calleeAddress,
                                  javax.sip.message.Message cause)
        throws OperationFailedException
    {
        // create the invite request
        Request invite = messageFactory
            .createInviteRequest(calleeAddress, cause);

        // Transaction
        ClientTransaction inviteTransaction = null;
        SipProvider jainSipProvider
            = getProtocolProvider().getDefaultJainSipProvider();
        try
        {
            inviteTransaction = jainSipProvider.getNewClientTransaction(invite);
        }
        catch (TransactionUnavailableException ex)
        {
            ProtocolProviderServiceSipImpl.throwOperationFailedException(
                "Failed to create inviteTransaction.\n"
                    + "This is most probably a network connection error.",
                OperationFailedException.INTERNAL_ERROR, ex, logger);
        }
        // create the call peer
        CallPeerSipImpl callPeer
            = createCallPeerFor(inviteTransaction, jainSipProvider);

        /* enable video if it is a videocall */
        callPeer.getMediaHandler().setLocalVideoTransmissionEnabled(
                localVideoAllowed);

        callPeer.invite();

        return callPeer;
    }

    /**
     * Send a RE-INVITE request for all current <tt>CallPeer</tt> to reflect
     * possible change in media setup (video start/stop, ...).
     *
     * @throws OperationFailedException if problem occurred during SDP
     * generation or network problem
     */
    public void reInvite() throws OperationFailedException
    {
        Iterator<CallPeerSipImpl> peers = getCallPeers();
        while (peers.hasNext())
        {
            CallPeerSipImpl peer = peers.next();
            peer.sendReInvite();
        }
    }

    /**
     * Creates a new call peer associated with <tt>containingTransaction</tt>
     *
     * @param containingTransaction the transaction that created the call peer.
     * @param sourceProvider the provider that the containingTransaction belongs
     * to.
     *
     * @return a new instance of a <tt>CallPeerSipImpl</tt> corresponding
     * to the <tt>containingTransaction</tt>.
     */
    private CallPeerSipImpl createCallPeerFor(
        Transaction containingTransaction, SipProvider sourceProvider)
    {
        CallPeerSipImpl callPeer = new CallPeerSipImpl(
                containingTransaction.getDialog().getRemoteParty(),
                this, containingTransaction, sourceProvider);
        addCallPeer(callPeer);

        boolean incomingCall
            = (containingTransaction instanceof ServerTransaction);
        callPeer.setState( incomingCall
                        ? CallPeerState.INCOMING_CALL
                        : CallPeerState.INITIATING_CALL);

        // if this was the first peer we added in this call then the call is
        // new and we also need to notify everyone of its creation.
        if(this.getCallPeerCount() == 1)
        {
            parentOpSet.fireCallEvent( (incomingCall
                                        ? CallEvent.CALL_RECEIVED
                                        : CallEvent.CALL_INITIATED),
                                        this);
        }

        return callPeer;
    }


    /**
     * Processes an incoming INVITE that is meant to replace an existing
     * <tt>CallPeerSipImpl</tt> that is participating in this call. Typically
     * this would happen as a result of an attended transfer.
     *
     * @param jainSipProvider the JAIN-SIP <tt>SipProvider</tt> that received
     * the request.
     * @param serverTransaction the transaction containing the INVITE request.
     * @param callPeerToReplace a reference to the <tt>CallPeer</tt> that this
     * INVITE is trying to replace.
     */
    public void processReplacingInvite(SipProvider       jainSipProvider,
                                       ServerTransaction serverTransaction,
                                       CallPeerSipImpl   callPeerToReplace)
    {
        CallPeerSipImpl newCallPeer
                    = createCallPeerFor(serverTransaction, jainSipProvider);
        try
        {
            newCallPeer.answer();
        }
        catch (OperationFailedException ex)
        {
            logger.error(
                "Failed to auto-answer the referred call peer "
                    + newCallPeer, ex);
            /*
             * RFC 3891 says an appropriate error response MUST be returned
             * and callPeerToReplace must be left unchanged.
             */
            //TODO should we send a response here?
            return;
        }

        //we just accepted the new peer and if we got here then it went well
        //now let's hangup the other call.
        try
        {
            callPeerToReplace.hangup();
        }
        catch (OperationFailedException ex)
        {
            logger.error("Failed to hangup the referer "
                            + callPeerToReplace, ex);
            callPeerToReplace.setState(
                            CallPeerState.FAILED, "Internal Error: " + ex);
        }
    }

    /**
     * Creates a new call and sends a RINGING response.
     *
     * @param jainSipProvider the provider containing
     * <tt>sourceTransaction</tt>.
     * @param serverTran the transaction containing the received request.
     *
     * @return CallPeerSipImpl the newly created call peer (the one that sent
     * the INVITE).
     */
    public CallPeerSipImpl processInvite(SipProvider       jainSipProvider,
                                         ServerTransaction serverTran)
{
        Request invite = serverTran.getRequest();

        CallPeerSipImpl peer = createCallPeerFor(serverTran, jainSipProvider);

        //send a ringing response
        Response response = null;
        try
        {
            if (logger.isTraceEnabled())
                logger.trace("will send ringing response: ");
            response = messageFactory.createResponse(Response.RINGING, invite);
            serverTran.sendResponse(response);
            if (logger.isDebugEnabled())
                logger.debug("sent a ringing response: " + response);
        }
        catch (Exception ex)
        {
            logger.error("Error while trying to send a request", ex);
            peer.setState(CallPeerState.FAILED,
                "Internal Error: " + ex.getMessage());
            return peer;
        }

        return peer;
    }

    /**
     * Modifies the local media setup of all peers in the call to reflect the
     * requested setting for the streaming of the local video and then passes
     * the setting to the participating <tt>CallPeerSipImpl</tt> instances.
     *
     * @param allowed <tt>true</tt> if local video transmission is allowed and
     * <tt>false</tt> otherwise.
     * @param useCase usecase for the video (i.e video call or desktop
     * streaming/sharing session)
     *
     *  @throws OperationFailedException if video initialization fails.
     */
    public void setLocalVideoAllowed(boolean allowed, MediaUseCase useCase)
        throws OperationFailedException
    {
        this.localVideoAllowed = allowed;
        mediaUseCase = useCase;

        /*
         * Record the setting locally and notify all peers.
         */
        Iterator<CallPeerSipImpl> peers = getCallPeers();
        while (peers.hasNext())
        {
            CallPeerSipImpl peer = peers.next();

            peer.setLocalVideoAllowed(allowed);
        }
    }

    /**
     * Determines whether the streaming of local video in this <tt>Call</tt>
     * is currently allowed. The setting does not reflect the availability of
     * actual video capture devices, it just expresses the local policy (or
     * desire of the user) to have the local video streamed in the case the
     * system is actually able to do so.
     *
     * @return <tt>true</tt> if the streaming of local video for this
     * <tt>Call</tt> is allowed; otherwise, <tt>false</tt>
     */
    public boolean isLocalVideoAllowed()
    {
        return localVideoAllowed;
    }

    /**
     * Determines whether we are currently streaming video toward at least one
     * of the peers in this call.
     *
     * @return <tt>true</tt> if we are currently streaming video toward at least
     * one of the peers in this call and <tt>false</tt> otherwise.
     */
    public boolean isLocalVideoStreaming()
    {

        Iterator<CallPeerSipImpl> peers = getCallPeers();
        while (peers.hasNext())
        {
            CallPeerSipImpl peer = peers.next();

            if (peer.isLocalVideoStreaming())
                return true;
        }

        return false;
    }

    /**
     * Registers a <tt>listener</tt> with all <tt>CallPeer</tt> currently
     * participating with the call so that it would be notified of changes in
     * video related properties (e.g. <tt>LOCAL_VIDEO_STREAMING</tt>).
     *
     * @param listener the <tt>PropertyChangeListener</tt> to be notified
     * when the properties associated with member <tt>CallPeer</tt>s change
     * their values.
     */
    public void addVideoPropertyChangeListener(
                                          PropertyChangeListener listener)
    {
        Iterator<CallPeerSipImpl> peers = getCallPeers();
        while (peers.hasNext())
        {
            CallPeerSipImpl peer = peers.next();
            peer.addVideoPropertyChangeListener(listener);
        }
    }

    /**
     * Removes <tt>listener</tt> from all <tt>CallPeer</tt>s currently
     * participating with the call so that it won't receive furher notifications
     * on changes in video related properties (e.g.
     * <tt>LOCAL_VIDEO_STREAMING</tt>).
     *
     * @param listener the <tt>PropertyChangeListener</tt> to unregister from
     * member <tt>CallPeer</tt>s change their values.
     */
    public void removeVideoPropertyChangeListener(
                                             PropertyChangeListener listener)
    {
        Iterator<CallPeerSipImpl> peers = getCallPeers();
        while (peers.hasNext())
        {
            CallPeerSipImpl peer = peers.next();
            peer.removeVideoPropertyChangeListener(listener);
        }
    }

    /**
     * Gets the indicator which determines whether the local peer represented by
     * this <tt>Call</tt> is acting as a conference focus and thus should send
     * the &quot;isfocus&quot; parameter in the Contact headers of its outgoing
     * SIP signaling.
     *
     * @return <tt>true</tt> if the local peer represented by this <tt>Call</tt>
     * is acting as a conference focus; otherwise, <tt>false</tt>
     */
    public boolean isConferenceFocus()
    {
        return conferenceFocus;
    }

    /**
     * Sets the indicator which determines whether the local peer represented by
     * this <tt>Call</tt> is acting as a conference focus and thus should send
     * the &quot;isfocus&quot; parameter in the Contact headers of its outgoing
     * SIP signaling
     *
     * @param conferenceFocus <tt>true</tt> if the local peer represented by
     * this <tt>Call</tt> is to act as a conference focus; otherwise,
     * <tt>false</tt>
     */
    void setConferenceFocus(boolean conferenceFocus)
    {
        if (this.conferenceFocus != conferenceFocus)
        {
            this.conferenceFocus = conferenceFocus;

            /*
             * If this Call switches from being a conference focus to not being
             * one, dispose of the audio mixer used when it was a conference
             * focus.
             */
            if (!this.conferenceFocus)
                conferenceAudioMixer = null;

            // fire that the focus property has changed
            fireCallChangeEvent(
                CallChangeEvent.CALL_FOCUS_CHANGE,
                !this.conferenceFocus, this.conferenceFocus);
        }
    }

    /**
     * Gets a <tt>MediaDevice</tt> which is capable of capture and/or playback
     * of media of the specified <tt>MediaType</tt>, is the default choice of
     * the user for a <tt>MediaDevice</tt> with the specified <tt>MediaType</tt>
     * and is appropriate for the current state of this <tt>Call</tt>.
     * <p>
     * For example, when the local peer represented by this <tt>Call</tt>
     * instance is acting as a conference focus, the audio device must be a
     * mixer.
     * </p>
     *
     * @param mediaType the <tt>MediaType</tt> in which the retrieved
     * <tt>MediaDevice</tt> is to capture and/or play back media
     * @return a <tt>MediaDevice</tt> which is capable of capture and/or
     * playback of media of the specified <tt>mediaType</tt>, is the default
     * choice of the user for a <tt>MediaDevice</tt> with the specified
     * <tt>mediaType</tt> and is appropriate for the current state of this
     * <tt>Call</tt>
     */
    MediaDevice getDefaultDevice(MediaType mediaType)
    {
        MediaService mediaService = SipActivator.getMediaService();
        MediaDevice device = mediaService.getDefaultDevice(mediaType,
                mediaUseCase);

        if (MediaType.AUDIO.equals(mediaType) && isConferenceFocus())
        {
            if (conferenceAudioMixer == null)
            {
                if (device != null)
                    conferenceAudioMixer = mediaService.createMixer(device);
            }
            return conferenceAudioMixer;
        }
        return device;
    }

    /**
     * Adds a specific <tt>SoundLevelListener</tt> to the list of
     * listeners interested in and notified about changes in local sound level
     * related information. When the first listener is being registered the
     * method also registers its single listener with the call peer media
     * handlers so that it would receive level change events and delegate them
     * to the listeners that have registered with us.
     *
     * @param l the <tt>SoundLevelListener</tt> to add
     */
    public void addLocalUserSoundLevelListener(SoundLevelListener l)
    {
        synchronized(localUserAudioLevelListeners)
        {

            if (localUserAudioLevelListeners.size() == 0)
            {
                //if this is the first listener that's being registered with
                //us, we also need to register ourselves as an audio level
                //listener with the media handler. we do this so that audio
                //level would only be calculated if anyone is interested in
                //receiving them.
                Iterator<CallPeerSipImpl> cps = getCallPeers();
                while (cps.hasNext())
                {
                    CallPeerSipImpl callPeerSipImpl = cps.next();
                    callPeerSipImpl.getMediaHandler()
                            .setLocalUserAudioLevelListener(
                                                localAudioLevelDelegator);
                }
            }

            localUserAudioLevelListeners.add(l);
        }
    }

    /**
     * Removes a specific <tt>SoundLevelListener</tt> from the list of
     * listeners interested in and notified about changes in local sound level
     * related information. If <tt>l</tt> is the last listener that we had here
     * we are also going to unregister our own level event delegator in order
     * to stop level calculations.
     *
     * @param l the <tt>SoundLevelListener</tt> to remove
     */
    public void removeLocalUserSoundLevelListener(SoundLevelListener l)
    {
        synchronized(localUserAudioLevelListeners)
        {
            localUserAudioLevelListeners.add(l);

            if (localUserAudioLevelListeners.size() == 0)
            {
                //if this was the last listener that was registered with us then
                //no long need to have a delegator registered with the call
                //peer media handlers. We therefore remove it so that audio
                //level calculations would be ceased.
                Iterator<CallPeerSipImpl> cps = getCallPeers();
                while (cps.hasNext())
                {
                    CallPeerSipImpl callPeerSipImpl = cps.next();
                    callPeerSipImpl.getMediaHandler()
                            .setLocalUserAudioLevelListener(null);
                }
            }
        }
    }

    /**
     * Notified by its very majesty the media service about changes in the
     * audio level of the local user, this listener generates the corresponding
     * events and delivers them to the listeners that have registered here.
     *
     * @param newLevel the new audio level of the local user.
     */
    private void fireLocalUserAudioLevelChangeEvent(int newLevel)
    {
        SoundLevelChangeEvent evt
            = new SoundLevelChangeEvent(this, newLevel);

        synchronized( localUserAudioLevelListeners )
        {
            for(SoundLevelListener listener : localUserAudioLevelListeners)
                 listener.soundLevelChanged(evt);
        }
    }

    /**
     * Determines whether this call is mute.
     *
     * @return <tt>true</tt> if an audio streams being sent to the call
     *         peers are currently muted; <tt>false</tt>, otherwise
     */
    public boolean isMute()
    {
        return this.mute;
    }

    /**
     * Sets the mute property for this call.
     *
     * @param newMuteValue the new value of the mute property for this call
     */
    public void setMute(boolean newMuteValue)
    {
        if (this.mute != newMuteValue)
        {
            this.mute = newMuteValue;

            Iterator<CallPeerSipImpl> peers = getCallPeers();
            while (peers.hasNext())
            {
                CallPeerSipImpl peer = peers.next();
                peer.setMute(newMuteValue);
            }
        }
    }
}
