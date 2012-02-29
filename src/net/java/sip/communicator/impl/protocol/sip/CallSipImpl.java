/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip;

import java.util.*;

import javax.sdp.*;
import javax.sip.*;
import javax.sip.address.*;
import javax.sip.message.*;

import net.java.sip.communicator.impl.protocol.sip.sdp.*;
import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.service.protocol.media.*;
import net.java.sip.communicator.util.*;

/**
 * A SIP implementation of the Call abstract class encapsulating SIP dialogs.
 *
 * @author Emil Ivov
 */
public class CallSipImpl
    extends MediaAwareCall<CallPeerSipImpl,
                                   OperationSetBasicTelephonySipImpl,
                                   ProtocolProviderServiceSipImpl>
    implements CallPeerListener
{
    /**
     * Our class logger.
     */
    private static final Logger logger = Logger.getLogger(CallSipImpl.class);

    /**
     * A reference to the <tt>SipMessageFactory</tt> instance that we should
     * use when creating requests.
     */
    private final SipMessageFactory messageFactory;

    /**
     * When starting call we may have quality preferences we must use
     * for the call.
     */
    private QualityPreset initialQualityPreferences;

    /**
     * Crates a CallSipImpl instance belonging to <tt>sourceProvider</tt> and
     * initiated by <tt>CallCreator</tt>.
     *
     * @param parentOpSet a reference to the operation set that's creating us
     * and that we would be able to use for even dispatching.
     */
    protected CallSipImpl(OperationSetBasicTelephonySipImpl parentOpSet)
    {
        super(parentOpSet);
        this.messageFactory = getProtocolProvider().getMessageFactory();

        //let's add ourselves to the calls repo. we are doing it ourselves just
        //to make sure that no one ever forgets.
        parentOpSet.getActiveCallsRepository().addCall(this);
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
                    logger.trace("Returning cp=" + cp);
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
        return super.getProtocolProvider();
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
        CallPeerMediaHandlerSipImpl mediaHandler = callPeer.getMediaHandler();

        /* enable video if it is a videocall */
        mediaHandler.setLocalVideoTransmissionEnabled(localVideoAllowed);

        if(initialQualityPreferences != null)
        {
            // we are in situation where we init the call and we cannot
            // determine whether the other party supports changing quality
            // so we force it
            mediaHandler.setSupportQualityControls(true);
            mediaHandler.getQualityControl().setRemoteSendMaxPreset(
                    initialQualityPreferences);
        }

        try
        {
            callPeer.invite();
        }
        catch(OperationFailedException ex)
        {
            // if inviting call peer fail for some reason, change its state
            // if not already filed
            callPeer.setState(CallPeerState.FAILED);
            throw ex;
        }

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
            Map<MediaType, MediaDirection> mediaDirections = new
                HashMap<MediaType, MediaDirection>();

            mediaDirections.put(MediaType.AUDIO, MediaDirection.INACTIVE);
            mediaDirections.put(MediaType.VIDEO, MediaDirection.INACTIVE);

            //this check is not mandatory catch all to skip if a problem exists
            try
            {
                // lets check the supported media types.
                // for this call
                Request inviteReq = containingTransaction.getRequest();

                if(inviteReq != null && inviteReq.getRawContent() != null)
                {
                    String sdpStr = SdpUtils.getContentAsString(inviteReq);

                    SessionDescription sesDescr = SdpUtils.parseSdpString(sdpStr);

                    List<MediaDescription> remoteDescriptions = SdpUtils
                            .extractMediaDescriptions(sesDescr);

                    for (MediaDescription mediaDescription : remoteDescriptions)
                    {
                        MediaType mediaType =
                                SdpUtils.getMediaType(mediaDescription);

                        if(mediaType.equals(MediaType.VIDEO))
                        {
                            MediaDirection videoDirection =
                                    SdpUtils.getDirection(mediaDescription);
                            mediaDirections.put(MediaType.VIDEO,
                                videoDirection);
                        }
                        else if(mediaType.equals(MediaType.AUDIO))
                        {
                            MediaDirection audioDirection =
                                SdpUtils.getDirection(mediaDescription);
                            mediaDirections.put(MediaType.AUDIO,
                                audioDirection);
                        }
                    }
                }
            }
            catch(Throwable t)
            {
                logger.warn("Error getting media types", t);
            }

            if(this.getCallGroup() == null)
            {
                getParentOperationSet().fireCallEvent( (incomingCall
                                        ? CallEvent.CALL_RECEIVED
                                        : CallEvent.CALL_INITIATED),
                                        this,
                                        mediaDirections);
            }
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
     * Set a quality preferences we may use when we start the call.
     * @param qualityPreferences the initial quality preferences.
     */
    public void setInitialQualityPreferences(QualityPreset qualityPreferences)
    {
        this.initialQualityPreferences = qualityPreferences;
    }

    /**
     * Notified when a call are added to a <tt>CallGroup</tt>.
     *
     * @param evt event
     */
    public synchronized void callAdded(CallGroupEvent evt)
    {
        Iterator<CallPeerSipImpl> peers = getCallPeers();
        boolean sendReinvite = true;

        if(evt.getSourceCall().getCallPeers().hasNext())
        {
            sendReinvite = !getCrossProtocolCallPeersVector().contains(
                evt.getSourceCall().getCallPeers().next());
        }

        setConferenceFocus(true);

        if(sendReinvite)
        {
            // reinvite peers to reflect conference focus
            while(peers.hasNext())
            {
                CallPeerSipImpl callPeer = peers.next();

                try
                {
                    if(callPeer.getState() == CallPeerState.CONNECTED &&
                        sendReinvite)
                    {
                        callPeer.sendReInvite();
                    }
                }
                catch(OperationFailedException e)
                {
                    logger.info("Failed to reinvite peer: "
                        + callPeer.getAddress());
                }
            }
        }
        super.callAdded(evt);
    }
}
