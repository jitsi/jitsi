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
import javax.sip.header.*;
import javax.sip.message.*;

import gov.nist.javax.sip.stack.*;

import net.java.sip.communicator.impl.protocol.sip.sdp.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.service.protocol.media.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.configuration.*;
import org.jitsi.service.neomedia.*;
import org.jitsi.service.neomedia.MediaType;

/**
 * A SIP implementation of the abstract <tt>Call</tt> class encapsulating SIP
 * dialogs.
 *
 * @author Emil Ivov
 * @author Hristo Terezov
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
     * When starting call we may have quality preferences we must use
     * for the call.
     */
    private QualityPreset initialQualityPreferences;

    /**
     * A reference to the <tt>SipMessageFactory</tt> instance that we should
     * use when creating requests.
     */
    private final SipMessageFactory messageFactory;

    /**
     * The name of the property under which the user may specify the number of
     * milliseconds for the initial interval for retransmissions of response
     * 180.
     */
    private static final String RETRANSMITS_RINGING_INTERVAL
        = "net.java.sip.communicator.impl.protocol.sip"
                + ".RETRANSMITS_RINGING_INTERVAL";

    /**
    * The default amount of time (in milliseconds) for the initial interval for
    *  retransmissions of response 180.
    */
    private static final int DEFAULT_RETRANSMITS_RINGING_INTERVAL = 500;

    /**
     * Maximum number of retransmissions that will be sent.
     */
    private static final int MAX_RETRANSMISSIONS = 3;

    /**
    * The amount of time (in milliseconds) for the initial interval for
    * retransmissions of response 180.
    */
    private int retransmitsRingingInterval
        = DEFAULT_RETRANSMITS_RINGING_INTERVAL;

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

        ConfigurationService cfg = SipActivator.getConfigurationService();
        retransmitsRingingInterval = cfg.getInt(RETRANSMITS_RINGING_INTERVAL,
                DEFAULT_RETRANSMITS_RINGING_INTERVAL);
    }

    /**
     * {@inheritDoc}
     *
     * Re-INVITEs the <tt>CallPeer</tt>s associated with this
     * <tt>CallSipImpl</tt> in order to include/exclude the &quot;isfocus&quot;
     * parameter in the Contact header.
     */
    @Override
    protected void conferenceFocusChanged(boolean oldValue, boolean newValue)
    {
        try
        {
            reInvite();
        }
        catch (OperationFailedException ofe)
        {
            logger.info("Failed to re-INVITE this Call: " + this, ofe);
        }
        finally
        {
            super.conferenceFocusChanged(oldValue, newValue);
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
     * Creates a new call peer associated with <tt>containingTransaction</tt>
     *
     * @param containingTransaction the transaction that created the call peer.
     * @param sourceProvider the provider that the containingTransaction belongs
     * to.
     * @return a new instance of a <tt>CallPeerSipImpl</tt> corresponding
     * to the <tt>containingTransaction</tt>.
     */
    private CallPeerSipImpl createCallPeerFor(
            Transaction containingTransaction,
            SipProvider sourceProvider)
    {
        CallPeerSipImpl callPeer
            = new CallPeerSipImpl(
                    containingTransaction.getDialog().getRemoteParty(),
                    this,
                    containingTransaction,
                    sourceProvider);

        addCallPeer(callPeer);

        boolean incomingCall
            = (containingTransaction instanceof ServerTransaction);

        callPeer.setState(
                incomingCall
                    ? CallPeerState.INCOMING_CALL
                    : CallPeerState.INITIATING_CALL);

        // if this was the first peer we added in this call then the call is
        // new and we also need to notify everyone of its creation.
        if(getCallPeerCount() == 1)
        {
            Map<MediaType, MediaDirection> mediaDirections
                = new HashMap<MediaType, MediaDirection>();

            mediaDirections.put(MediaType.AUDIO, MediaDirection.INACTIVE);
            mediaDirections.put(MediaType.VIDEO, MediaDirection.INACTIVE);

            boolean hasZrtp = false;
            boolean hasSdes = false;

            //this check is not mandatory catch all to skip if a problem exists
            try
            {
                // lets check the supported media types.
                // for this call
                Request inviteReq = containingTransaction.getRequest();

                if(inviteReq != null && inviteReq.getRawContent() != null)
                {
                    String sdpStr = SdpUtils.getContentAsString(inviteReq);
                    SessionDescription sesDescr
                        = SdpUtils.parseSdpString(sdpStr);
                    List<MediaDescription> remoteDescriptions
                        = SdpUtils.extractMediaDescriptions(sesDescr);

                    for (MediaDescription mediaDescription : remoteDescriptions)
                    {
                        MediaType mediaType
                            = SdpUtils.getMediaType(mediaDescription);

                        mediaDirections.put(
                                mediaType,
                                SdpUtils.getDirection(mediaDescription));

                        // hasZrtp?
                        if (!hasZrtp)
                        {
                            hasZrtp
                                = (mediaDescription.getAttribute(
                                        SdpUtils.ZRTP_HASH_ATTR)
                                    != null);
                        }
                        // hasSdes?
                        if (!hasSdes)
                        {
                            @SuppressWarnings("unchecked")
                            Vector<Attribute> attrs
                                = mediaDescription.getAttributes(true);

                            for (Attribute attr : attrs)
                            {
                                try
                                {
                                    if ("crypto".equals(attr.getName()))
                                    {
                                        hasSdes = true;
                                        break;
                                    }
                                }
                                catch (SdpParseException spe)
                                {
                                    logger.error(
                                            "Failed to parse SDP attribute",
                                            spe);
                                }
                            }
                        }
                    }
                }
            }
            catch(Throwable t)
            {
                logger.warn("Error getting media types", t);
            }

            getParentOperationSet().fireCallEvent(
                    incomingCall
                        ? CallEvent.CALL_RECEIVED
                        : CallEvent.CALL_INITIATED,
                    this,
                    mediaDirections);

            if(hasZrtp)
            {
                callPeer.getMediaHandler().addAdvertisedEncryptionMethod(
                        SrtpControlType.ZRTP);
            }
            if(hasSdes)
            {
                callPeer.getMediaHandler().addAdvertisedEncryptionMethod(
                        SrtpControlType.SDES);
            }
        }

        return callPeer;
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

        final CallPeerSipImpl peer
            = createCallPeerFor(serverTran, jainSipProvider);

        CallInfoHeader infoHeader
            = (CallInfoHeader) invite.getHeader(CallInfoHeader.NAME);

        // Sets an alternative impp address if such is available in the
        // call-info header.
        String alternativeIMPPAddress = null;
        if (infoHeader != null
            && infoHeader.getParameter("purpose") != null
            && infoHeader.getParameter("purpose").equals("impp"))
        {
            alternativeIMPPAddress = infoHeader.getInfo().toString();
        }

        if (alternativeIMPPAddress != null)
            peer.setAlternativeIMPPAddress(alternativeIMPPAddress);

        //send a ringing response
        Response response = null;
        try
        {
            if (logger.isTraceEnabled())
                logger.trace("will send ringing response: ");
            response = messageFactory.createResponse(Response.RINGING, invite);
            serverTran.sendResponse(response);

            if(serverTran instanceof SIPTransaction
                && !((SIPTransaction)serverTran).isReliable())
            {
                final Timer timer = new Timer();
                CallPeerAdapter stateListener = new CallPeerAdapter()
                {
                    @Override
                    public void peerStateChanged(CallPeerChangeEvent evt)
                    {
                        if(!evt.getNewValue()
                                .equals(CallPeerState.INCOMING_CALL))
                        {
                            timer.cancel();
                            peer.removeCallPeerListener(this);
                        }
                    }
                };
                int interval = retransmitsRingingInterval;
                int delay = 0;
                for(int i = 0; i < MAX_RETRANSMISSIONS; i++)
                {
                    delay += interval;
                    timer.schedule(new RingingResponseTask(response,
                        serverTran, peer, timer, stateListener), delay);
                    interval *= 2;
                }

                peer.addCallPeerListener(stateListener);
            }
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
                        + newCallPeer,
                    ex);
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
     * Sends a re-INVITE request to all <tt>CallPeer</tt>s to reflect possible
     * changes in the media setup (video start/stop, ...).
     *
     * @throws OperationFailedException if a problem occurred during the SDP
     * generation or there was a network problem
     */
    public void reInvite() throws OperationFailedException
    {
        Iterator<CallPeerSipImpl> peers = getCallPeers();

        while (peers.hasNext())
            peers.next().sendReInvite();
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
     * Task that will retransmit ringing response
     */
    private class RingingResponseTask
        extends TimerTask
    {
        /**
         * The response that will be sent
         */
        private final Response response;

        /**
         * The transaction containing the received request.
         */
        private final ServerTransaction serverTran;

        /**
         * The peer corresponding to the transaction.
         */
        private final CallPeerSipImpl peer;

        /**
         * The timer that starts the task.
         */
        private final Timer timer;

        /**
         * Listener for the state of the peer.
         */
        private CallPeerAdapter stateListener;

        /**
         * Create ringing response task.
         * @param response the response.
         * @param serverTran the transaction.
         * @param peer the peer.
         * @param timer the timer.
         * @param stateListener the state listener.
         */
        RingingResponseTask(Response response, ServerTransaction serverTran,
            CallPeerSipImpl peer, Timer timer, CallPeerAdapter stateListener)
        {
            this.response = response;
            this.serverTran = serverTran;
            this.peer = peer;
            this.timer = timer;
            this.stateListener = stateListener;
        }

        /**
         * Sends the ringing response.
         */
        @Override
        public void run()
        {
            try
            {
                serverTran.sendResponse(response);
            }
            catch (Exception ex)
            {
                timer.cancel();
                peer.removeCallPeerListener(stateListener);
            }
        }
    }
}
