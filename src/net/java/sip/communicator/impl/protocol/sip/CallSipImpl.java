/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip;

import java.net.*;
import java.text.*;
import java.util.*;

import javax.sip.*;
import javax.sip.address.*;
import javax.sip.address.URI;//disambiguates java.net.URI
import javax.sip.header.*;
import javax.sip.message.*;

import net.java.sip.communicator.service.media.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * A SIP implementation of the Call abstract class encapsulating SIP dialogs.
 *
 * @author Emil Ivov
 */
public class CallSipImpl
    extends Call
    implements CallPeerListener
{
    /**
     * Our class logger.
     */
    private static final Logger logger = Logger.getLogger(CallSipImpl.class);

    /**
     * A list containing all <tt>CallPeer</tt>s of this call.
     */
    private final List<CallPeerSipImpl> callPeers =
        new Vector<CallPeerSipImpl>();

    /**
     * The <tt>CallSession</tt> that the media service has created for this
     * call.
     */
    private CallSession mediaCallSession = null;

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
     * Crates a CallSipImpl instance belonging to <tt>sourceProvider</tt> and
     * initiated by <tt>CallCreator</tt>.
     *
     * @param sourceProvider the ProtocolProviderServiceSipImpl instance in the
     * context of which this call has been created.
     * @param parentOpSet a reference to the operation set that's creating us
     * and that we would be able to use for even dispatching.
     */
    protected CallSipImpl(ProtocolProviderServiceSipImpl    sourceProvider,
                          OperationSetBasicTelephonySipImpl parentOpSet)
    {
        super(sourceProvider);
        this.messageFactory = sourceProvider.getMessageFactory();
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
        if (callPeers.contains(callPeer))
            return;

        callPeer.addCallPeerListener(this);

        this.callPeers.add(callPeer);
        fireCallPeerEvent(callPeer,
            CallPeerEvent.CALL_PEER_ADDED);
    }

    /**
     * Removes <tt>callPeer</tt> from the list of peers in this
     * call. The method has no effect if there was no such peer in the
     * call.
     *
     * @param callPeer the <tt>CallPeer</tt> leaving the call;
     */
    public void removeCallPeer(CallPeerSipImpl callPeer)
    {
        if (!callPeers.contains(callPeer))
            return;

        this.callPeers.remove(callPeer);
        callPeer.removeCallPeerListener(this);

        try
        {
            fireCallPeerEvent(callPeer,
                CallPeerEvent.CALL_PEER_REMVOVED);
        }
        finally
        {

            /*
             * The peer should loose its state once it has finished
             * firing its events in order to allow the listeners to undo.
             */
            callPeer.setCall(null);
        }

        if (callPeers.size() == 0)
            setCallState(CallState.CALL_ENDED);
    }

    /**
     * Returns an iterator over all call peers.
     *
     * @return an Iterator over all peers currently involved in the call.
     */
    public Iterator<CallPeer> getCallPeers()
    {
        return new LinkedList<CallPeer>(callPeers).iterator();
    }

    /**
     * Returns the number of peers currently associated with this call.
     *
     * @return an <tt>int</tt> indicating the number of peers currently
     *         associated with this call.
     */
    public int getCallPeerCount()
    {
        return callPeers.size();
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
        CallPeerState newState =
            (CallPeerState) evt.getNewValue();
        if (newState == CallPeerState.DISCONNECTED
            || newState == CallPeerState.FAILED)
        {
            removeCallPeer((CallPeerSipImpl) evt
                .getSourceCallPeer());
        }
        else if ((newState == CallPeerState.CONNECTED
               || newState == CallPeerState.CONNECTING_WITH_EARLY_MEDIA))
        {
            setCallState(CallState.CALL_IN_PROGRESS);
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
        Iterator<CallPeer> callPeers = this.getCallPeers();

        if (logger.isTraceEnabled())
        {
            logger.trace("Looking for peer with dialog: " + dialog
                + "among " + this.callPeers.size() + " calls");
        }

        while (callPeers.hasNext())
        {
            CallPeerSipImpl cp =
                (CallPeerSipImpl) callPeers.next();

            if (cp.getDialog() == dialog)
            {
                logger.trace("Returing cp=" + cp);
                return cp;
            }
            else
            {
                logger.trace("Ignoring cp=" + cp + " because cp.dialog="
                    + cp.getDialog() + " while dialog=" + dialog);
            }
        }

        return null;
    }

    /**
     * Sets the <tt>CallSession</tt> that the media service has created for this
     * call.
     *
     * @param callSession the <tt>CallSession</tt> that the media service has
     * created for this call.
     */
    public void setMediaCallSession(CallSession callSession)
    {
        this.mediaCallSession = callSession;
    }

    /**
     * Sets the <tt>CallSession</tt> that the media service has created for this
     * call.
     *
     * @return the <tt>CallSession</tt> that the media service has created for
     *         this call or null if no call session has been created so far.
     */
    public CallSession getMediaCallSession()
    {
        return this.mediaCallSession;
    }

    /**
     * Returns a reference to the <tt>ProtocolProviderServiceSipImpl</tt>
     * instance that created this call.
     * @return a reference to the <tt>ProtocolProviderServiceSipImpl</tt>
     * instance that created this call.
     */
    public ProtocolProviderServiceSipImpl getProtocolProvider()
    {
        return (ProtocolProviderServiceSipImpl)super.getProtocolProvider();
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

        // pre-authenticate the request if possible.
        messageFactory.preAuthenticateRequest(invite);

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
        CallPeerSipImpl callPeer =
            createCallPeerFor(inviteTransaction, jainSipProvider);

        // invite content
        attachSdpOffer(invite, callPeer);

        try
        {
            inviteTransaction.sendRequest();
            if (logger.isDebugEnabled())
                logger.debug("sent request:\n" + invite);
        }
        catch (SipException ex)
        {
            ProtocolProviderServiceSipImpl.throwOperationFailedException(
                "An error occurred while sending invite request",
                OperationFailedException.NETWORK_FAILURE, ex, logger);
        }

        return callPeer;
    }

    /**
     * Creates an SDP offer destined to <tt>callPeer</tt> and attaches it to
     * the <tt>invite</tt> request.
     *
     * @param invite the invite <tt>Request</tt> that we'd like to attach an
     * SDP offer to.
     * @param callPeer the <tt>callPeer</tt> that we'd like to address our
     * offer to
     *
     * @throws OperationFailedException if we fail constructing the session
     * description.
     */
    private void attachSdpOffer(Request invite, CallPeerSipImpl callPeer)
        throws OperationFailedException
    {
        try
        {
            CallSession callSession = SipActivator.getMediaService()
                .createCallSession(callPeer.getCall());
            callPeer.getCall().setMediaCallSession(callSession);

            callSession.setSessionCreatorCallback(callPeer);

            // if possible try to indicate the address of the callee so
            // that the media service can choose the most proper local
            // address to advertise.
            URI calleeURI = callPeer.getJainSipAddress().getURI();

            if (calleeURI.isSipURI())
            {
                // content type should be application/sdp (not applications)
                // reported by Oleg Shevchenko (Miratech)
                ContentTypeHeader contentTypeHeader = getProtocolProvider()
                    .getHeaderFactory().createContentTypeHeader(
                        "application", "sdp");

                String host = ((SipURI) calleeURI).getHost();
                InetAddress intendedDestination = getProtocolProvider()
                        .resolveSipAddress(host).getAddress();

                invite.setContent(callSession
                            .createSdpOffer(intendedDestination),
                            contentTypeHeader);
            }
        }
        catch (UnknownHostException ex)
        {
            ProtocolProviderServiceSipImpl.throwOperationFailedException(
                "Failed to obtain an InetAddress for " + ex.getMessage(),
                OperationFailedException.NETWORK_FAILURE, ex, logger);
        }
        catch (ParseException ex)
        {
            ProtocolProviderServiceSipImpl.throwOperationFailedException(
                "Failed to parse sdp data while creating invite request!",
                OperationFailedException.INTERNAL_ERROR, ex, logger);
        }
        catch (MediaException ex)
        {
            ProtocolProviderServiceSipImpl.throwOperationFailedException(
                "Could not access media devices!",
                OperationFailedException.INTERNAL_ERROR, ex, logger);
        }

    }

    /**
     * Creates an SDP description that could be sent to <tt>peer</tt> and adds
     * it to <tt>response</tt>. Provides a hook for this instance to take last
     * configuration steps on a specific <tt>Response</tt> before it is sent to
     * a specific <tt>CallPeer</tt> as part of the execution of.
     *
     * @param peer the <tt>CallPeer</tt> to receive a specific <tt>Response</tt>
     * @param response the <tt>Response</tt> to be sent to the <tt>peer</tt>
     *
     * @throws OperationFailedException if we fail parsing call peer's media.
     * @throws ParseException if we try to attach invalid SDP to response.
     */
    private void attachSdpAnswer(Response response, CallPeerSipImpl peer)
        throws OperationFailedException, ParseException
    {
        /*
         * At the time of this writing, we're only getting called because a
         * response to a call-hold invite is to be sent.
         */

        CallSession callSession = peer.getCall().getMediaCallSession();

        String sdpAnswer = null;
        try
        {
            sdpAnswer = callSession.processSdpOffer(
                        peer, peer.getSdpDescription());
        }
        catch (MediaException ex)
        {
            ProtocolProviderServiceSipImpl.throwOperationFailedException(
                "Failed to create SDP answer to put-on/off-hold request.",
                OperationFailedException.INTERNAL_ERROR, ex, logger);
        }

        response.setContent(
            sdpAnswer,
            getProtocolProvider().getHeaderFactory()
                .createContentTypeHeader("application", "sdp"));
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
                containingTransaction.getDialog().getRemoteParty(), this);
        addCallPeer(callPeer);

        boolean incomingCall
            = (containingTransaction instanceof ServerTransaction);
        callPeer.setState( incomingCall
                        ? CallPeerState.INCOMING_CALL
                        : CallPeerState.INITIATING_CALL);

        callPeer.setDialog(containingTransaction.getDialog());
        callPeer.setFirstTransaction(containingTransaction);
        callPeer.setJainSipProvider(sourceProvider);

        // notify everyone
        parentOpSet.fireCallEvent( (incomingCall
                                    ? CallEvent.CALL_RECEIVED
                                    : CallEvent.CALL_INITIATED),
                                   this);

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
            answerCallPeer(newCallPeer);
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
            hangupCallPeer(callPeerToReplace);
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
     * Updates the media flags for
     *
     * @param callPeer the <tt>CallPeer</tt> who was sent a specific
     * <tt>Response</tt>
     * @param response the <tt>Response</tt> that has just been sent to the
     * <tt>callPeer</tt>
     * @throws OperationFailedException if we fail parsing callPeer's media.
     */
    private void setMediaFlagsForPeer(CallPeerSipImpl callPeer,
                                      Response        response)
        throws OperationFailedException
    {
        /*
         * At the time of this writing, we're only getting called because a
         * response to a call-hold invite is to be sent.
         */

        CallSession callSession = callPeer.getCall().getMediaCallSession();

        int mediaFlags = 0;
        try
        {
            mediaFlags = callSession .getSdpOfferMediaFlags(
                            callPeer.getSdpDescription());
        }
        catch (MediaException ex)
        {
            ProtocolProviderServiceSipImpl.throwOperationFailedException(
                "Failed to create SDP answer to put-on/off-hold request.",
                OperationFailedException.INTERNAL_ERROR, ex, logger);
        }

        /*
         * Comply with the request of the SDP offer with respect to putting on
         * hold.
         */
        boolean on = ((mediaFlags & CallSession.ON_HOLD_REMOTELY) != 0);

        callSession.putOnHold(on, false);

        CallPeerState state = callPeer.getState();
        if (CallPeerState.ON_HOLD_LOCALLY.equals(state))
        {
            if (on)
                callPeer.setState(CallPeerState.ON_HOLD_MUTUALLY);
        }
        else if (CallPeerState.ON_HOLD_MUTUALLY.equals(state))
        {
            if (!on)
                callPeer.setState(CallPeerState.ON_HOLD_LOCALLY);
        }
        else if (CallPeerState.ON_HOLD_REMOTELY.equals(state))
        {
            if (!on)
                callPeer.setState(CallPeerState.CONNECTED);
        }
        else if (on)
        {
            callPeer.setState(CallPeerState.ON_HOLD_REMOTELY);
        }

        /*
         * Reflect the request of the SDP offer with respect to the modification
         * of the availability of media.
         */
        callSession.setReceiveStreaming(mediaFlags);
    }

    /**
     * Ends the call with the specified <tt>peer</tt>. Depending on the state
     * of the call the method would send a CANCEL, BYE, or BUSY_HERE message
     * and set the new state to DISCONNECTED.
     *
     * @param callPeer the peer that we'd like to hang up on.
     *
     * @throws OperationFailedException if we fail to terminate the call.
     */
    public void hangupCallPeer(CallPeerSipImpl callPeer)
        throws OperationFailedException
    {
        // do nothing if the call is already ended
        if (callPeer.getState().equals(CallPeerState.DISCONNECTED)
            || callPeer.getState().equals(CallPeerState.FAILED))
        {
            logger.debug("Ignoring a request to hangup a call peer "
                + "that is already DISCONNECTED");
            return;
        }

        CallPeerState peerState = callPeer.getState();
        if (peerState.equals(CallPeerState.CONNECTED)
            || CallPeerState.isOnHold(peerState))
        {
            sayBye(callPeer);
            callPeer.setState(CallPeerState.DISCONNECTED);
        }
        else if (callPeer.getState().equals(
            CallPeerState.CONNECTING)
            || callPeer.getState().equals(
                CallPeerState.CONNECTING_WITH_EARLY_MEDIA)
            || callPeer.getState().equals(
                CallPeerState.ALERTING_REMOTE_SIDE))
        {
            if (callPeer.getFirstTransaction() != null)
            {
                // Someone knows about us. Let's be polite and say we are
                // leaving
                sayCancel(callPeer);
            }
            callPeer.setState(CallPeerState.DISCONNECTED);
        }
        else if (peerState.equals(CallPeerState.INCOMING_CALL))
        {
            callPeer.setState(CallPeerState.DISCONNECTED);
            sayBusyHere(callPeer);
        }
        // For FAILED and BUSY we only need to update CALL_STATUS
        else if (peerState.equals(CallPeerState.BUSY))
        {
            callPeer.setState(CallPeerState.DISCONNECTED);
        }
        else if (peerState.equals(CallPeerState.FAILED))
        {
            callPeer.setState(CallPeerState.DISCONNECTED);
        }
        else
        {
            callPeer.setState(CallPeerState.DISCONNECTED);
            logger.error("Could not determine call peer state!");
        }

    }

    /**
     * Sends a BYE request to <tt>callPeer</tt>.
     *
     * @param callPeer the call peer that we need to say bye to.
     * @return <tt>true</tt> if the <tt>Dialog</tt> should be considered
     * alive after sending the BYE request (e.g. when there're still active
     * subscriptions); <tt>false</tt>, otherwise
     *
     * @throws OperationFailedException if we failed constructing or sending a
     * SIP Message.
     */
    public boolean sayBye(CallPeerSipImpl callPeer)
        throws OperationFailedException
    {
        Dialog dialog = callPeer.getDialog();

        Request bye = messageFactory.createRequest(dialog, Request.BYE);

        getProtocolProvider().sendInDialogRequest(
                        callPeer.getJainSipProvider(), bye, dialog);

        /*
         * Let subscriptions such as the ones associated with REFER requests
         * keep the dialog alive and correctly delete it when they are
         * terminated.
         */
        try
        {
            return EventPackageUtils.processByeThenIsDialogAlive(dialog);
        }
        catch (SipException ex)
        {
            ProtocolProviderServiceSipImpl.throwOperationFailedException(
                "Failed to determine whether the dialog should stay alive.",
                OperationFailedException.INTERNAL_ERROR, ex, logger);
            return false;
        }
    }

    /**
     * Sends a BUSY_HERE response to <tt>callPeer</tt>.
     *
     * @param callPeer the call peer that we need to send busy tone to.
     *
     * @throws OperationFailedException if we fail to create or send the
     * response
     */
    private void sayBusyHere(CallPeerSipImpl callPeer)
        throws OperationFailedException
    {
        Request request = callPeer.getFirstTransaction().getRequest();
        Response busyHere = null;
        try
        {
            busyHere = messageFactory.createResponse(
                                Response.BUSY_HERE, request);
        }
        catch (ParseException ex)
        {
            ProtocolProviderServiceSipImpl.throwOperationFailedException(
                "Failed to create the BUSY_HERE response!",
                OperationFailedException.INTERNAL_ERROR, ex, logger);
        }

        if (!callPeer.getDialog().isServer())
        {
            logger.error("Cannot send BUSY_HERE in a client transaction");
            throw new OperationFailedException(
                "Cannot send BUSY_HERE in a client transaction",
                OperationFailedException.INTERNAL_ERROR);
        }
        ServerTransaction serverTransaction =
            (ServerTransaction) callPeer.getFirstTransaction();

        try
        {
            serverTransaction.sendResponse(busyHere);
            logger.debug("sent response:\n" + busyHere);
        }
        catch (Exception ex)
        {
            ProtocolProviderServiceSipImpl.throwOperationFailedException(
                "Failed to send the BUSY_HERE response",
                OperationFailedException.NETWORK_FAILURE, ex, logger);
        }
    }

    /**
     * Sends a Cancel request to <tt>callPeer</tt>.
     *
     * @param callPeer the call peer that we need to cancel.
     *
     * @throws OperationFailedException we failed to construct or send the
     * CANCEL request.
     */
    private void sayCancel(CallPeerSipImpl callPeer)
        throws OperationFailedException
    {
        if (callPeer.getDialog().isServer())
        {
            logger.error("Cannot cancel a server transaction");
            throw new OperationFailedException(
                "Cannot cancel a server transaction",
                OperationFailedException.INTERNAL_ERROR);
        }

        ClientTransaction clientTransaction =
            (ClientTransaction) callPeer.getFirstTransaction();
        try
        {
            Request cancel = clientTransaction.createCancel();
            ClientTransaction cancelTransaction =
                callPeer.getJainSipProvider().getNewClientTransaction(
                    cancel);
            cancelTransaction.sendRequest();
            logger.debug("sent request:\n" + cancel);
        }
        catch (SipException ex)
        {
            ProtocolProviderServiceSipImpl.throwOperationFailedException(
                "Failed to send the CANCEL request",
                OperationFailedException.NETWORK_FAILURE, ex, logger);
        }
    }

    /**
     * Indicates a user request to answer an incoming call from the specified
     * CallPeer.
     *
     * Sends an OK response to <tt>callPeer</tt>. Make sure that the call
     * peer contains an sdp description when you call this method.
     *
     * @param callPeer the call peer that we need to send the ok to.
     * @throws OperationFailedException if we fail to create or send the
     * response.
     */
    public synchronized void answerCallPeer(CallPeerSipImpl callPeer)
        throws OperationFailedException
    {
        Transaction transaction = callPeer.getFirstTransaction();

        if (transaction == null ||
            !(transaction instanceof ServerTransaction))
        {
            callPeer.setState(CallPeerState.DISCONNECTED);
            throw new OperationFailedException(
                "Failed to extract a ServerTransaction "
                    + "from the call's associated dialog!",
                OperationFailedException.INTERNAL_ERROR);
        }

        CallPeerState peerState = callPeer.getState();

        if (peerState.equals(CallPeerState.CONNECTED)
            || CallPeerState.isOnHold(peerState))
        {
            logger.info("Ignoring user request to answer a CallPeer "
                + "that is already connected. CP:" + callPeer);
            return;
        }

        ServerTransaction serverTransaction = (ServerTransaction) transaction;
        Response ok = null;
        try
        {
            ok = messageFactory.createResponse(
                            Response.OK, serverTransaction.getRequest());
        }
        catch (ParseException ex)
        {
            callPeer.setState(CallPeerState.DISCONNECTED);
            ProtocolProviderServiceSipImpl.throwOperationFailedException(
                "Failed to construct an OK response to an INVITE request",
                OperationFailedException.INTERNAL_ERROR, ex, logger);
        }

        // Content
        ContentTypeHeader contentTypeHeader = null;
        try
        {
            // content type should be application/sdp (not applications)
            // reported by Oleg Shevchenko (Miratech)
            contentTypeHeader = getProtocolProvider().getHeaderFactory()
                .createContentTypeHeader("application", "sdp");
        }
        catch (ParseException ex)
        {
            // Shouldn't happen
            callPeer.setState(CallPeerState.DISCONNECTED);
            ProtocolProviderServiceSipImpl.throwOperationFailedException(
                "Failed to create a content type header for the OK response",
                OperationFailedException.INTERNAL_ERROR, ex, logger);
        }

        try
        {
            CallSession callSession =
                SipActivator.getMediaService().createCallSession(
                    callPeer.getCall());
            ((CallSipImpl) callPeer.getCall())
                .setMediaCallSession(callSession);

            callSession.setSessionCreatorCallback(callPeer);

            String sdpOffer = callPeer.getSdpDescription();
            String sdp;
            // if the offer was in the invite create an sdp answer
            if ((sdpOffer != null) && (sdpOffer.length() > 0))
            {
                sdp = callSession.processSdpOffer(callPeer, sdpOffer);

                // set the call url in case there was one
                /**
                 * @todo this should be done in CallSession, once we move it
                 *       here.
                 */
                callPeer.setCallInfoURL(callSession.getCallInfoURL());
            }
            // if there was no offer in the invite - create an offer
            else
            {
                sdp = callSession.createSdpOffer();
            }
            ok.setContent(sdp, contentTypeHeader);
        }
        catch (MediaException ex)
        {
            //log, the error and tell the remote party. do not throw an
            //exception as it would go to the stack and there's nothing it could
            //do with it.
            logger.error(
                "Failed to create an SDP description for an OK response "
                    + "to an INVITE request!",
                ex);
            getProtocolProvider().sayError(
                            serverTransaction, Response.NOT_ACCEPTABLE_HERE);
        }
        catch (ParseException ex)
        {
            //log, the error and tell the remote party. do not throw an
            //exception as it would go to the stack and there's nothing it could
            //do with it.
            logger.error("Failed to parse SDP of an invoming INVITE",ex);
            getProtocolProvider().sayError(
                            serverTransaction, Response.NOT_ACCEPTABLE_HERE);
        }

        try
        {
            serverTransaction.sendResponse(ok);
            if (logger.isDebugEnabled())
                logger.debug("sent response\n" + ok);
        }
        catch (Exception ex)
        {
            callPeer.setState(CallPeerState.DISCONNECTED);
            ProtocolProviderServiceSipImpl.throwOperationFailedException(
                "Failed to send an OK response to an INVITE request",
                OperationFailedException.NETWORK_FAILURE, ex, logger);
        }

    }

    /**
     * Creates a new call and sends a RINGING response.
     *
     * @param jainSipProvider the provider containing
     * <tt>sourceTransaction</tt>.
     * @param serverTransaction the transaction containing the received request.
     *
     * @return CallPeerSipImpl the newly created call peer (the one that sent
     * the INVITE).
     */
    public CallPeerSipImpl processInvite(SipProvider       jainSipProvider,
                    ServerTransaction serverTransaction)
{
        Request invite = serverTransaction.getRequest();

        CallPeerSipImpl peer
            = createCallPeerFor(serverTransaction, jainSipProvider);

        // extract the SDP description.
        // beware: SDP description may be in ACKs so it could be that there's
        // nothing here - bug report Laurent Michel
        ContentLengthHeader cl = invite.getContentLength();
        if (cl != null && cl.getContentLength() > 0)
        {
            peer.setSdpDescription(new String(invite.getRawContent()));
        }

        //send a ringing response
        Response response = null;
        try
        {
            logger.trace("will send ringing response: ");
            response = messageFactory.createResponse(Response.RINGING, invite);
            serverTransaction.sendResponse(response);
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
     * Reinitializes the media session of the <tt>CallPeer</tt> that this
     * INVITE request is destined to.
     *
     * @param jainSipProvider the {@link SipProvider} that received the request.
     * @param serverTransaction a reference to the {@link ServerTransaction}
     * that contains the reINVITE request.
     */
    public void processReInvite(SipProvider       jainSipProvider,
                                ServerTransaction serverTransaction)
    {
        Request invite = serverTransaction.getRequest();

        CallPeerSipImpl callPeer = findCallPeer(serverTransaction.getDialog());

        callPeer.setFirstTransaction(serverTransaction);

        // SDP description may be in ACKs - bug report Laurent Michel
        ContentLengthHeader cl = invite.getContentLength();
        if (cl != null && cl.getContentLength() > 0)
        {
            callPeer.setSdpDescription(new String(invite.getRawContent()));
        }

        Response response = null;
        try
        {
            response = messageFactory.createResponse(Response.OK, invite);
            attachSdpAnswer(response, callPeer);

            logger.trace("will send an OK response: ");
            serverTransaction.sendResponse(response);
            logger.debug("sent a an OK response: "+ response);
        }
        catch (Exception ex)//no need to distinguish among exceptions.
        {
            logger.error("Error while trying to send a response", ex);
            callPeer.setState(CallPeerState.FAILED,
                "Internal Error: " + ex.getMessage());
            getProtocolProvider().sayErrorSilently(
                            serverTransaction, Response.SERVER_INTERNAL_ERROR);
            return;
        }

        try
        {
            this.setMediaFlagsForPeer(callPeer, response);
        }
        catch (OperationFailedException ex)
        {
            logger.error("Error after sending response " + response, ex);
        }
    }

    /**
     * Sets <tt>callPeer</tt>'s state to CONNECTED, sends an ACK and processes
     * the SDP description in the <tt>ok</tt> <tt>Response</tt>.
     * sends an ACK.
     *
     * @param clientTransaction the <tt>ClientTransaction</tt> that the response
     * arrived in.
     * @param ok the OK <tt>Response</tt> to process
     * @param callPeer the peer that send the OK <tt>Response</tt>.
     */
    public void processInviteOK(ClientTransaction clientTransaction,
                                 Response         ok,
                                 CallPeerSipImpl  callPeer)
    {
        try
        {
            // Send the ACK. Do it now since we already got all the info we need
            // and processSdpAnswer() can take a while (patch by Michael Koch)
            getProtocolProvider().sendAck(clientTransaction);
        }
        catch (InvalidArgumentException ex)
        {
            // Shouldn't happen
            CallSipImpl.logAndFailCallPeer(
                            "Error creating an ACK (CSeq?)", ex, callPeer);
            return;
        }
        catch (SipException ex)
        {
            CallSipImpl.logAndFailCallPeer(
                            "Failed to create ACK request!", ex, callPeer);
            return;
        }

        // !!! set SDP content before setting call state as that is where
        // listeners get alerted and they need the SDP
        // ignore SDP if we've just had one in early media
        if(!CallPeerState.CONNECTING_WITH_EARLY_MEDIA
               .equals(callPeer.getState()))
        {
            callPeer.setSdpDescription(new String(ok.getRawContent()));
        }

        // notify the media manager of the sdp content
        CallSession callSession = callPeer.getCall().getMediaCallSession();

        try
        {
             //Process SDP unless we've just had an answer in a 18X response
            CallPeerState callPeerState = callPeer.getState();
            if (!CallPeerState.CONNECTING_WITH_EARLY_MEDIA
                    .equals(callPeerState))
            {
                callSession.processSdpAnswer(
                                callPeer, callPeer.getSdpDescription());
            }

            // set the call url in case there was one
            callPeer.setCallInfoURL(callSession.getCallInfoURL());
        }
        //at this point we have already sent our ack so in addition to logging
        //an error we also need to hangup the call peer.
        catch (Exception exc)//Media or parse exception.
        {
            logger.error("There was an error parsing the SDP description of "
                             + callPeer.getDisplayName()
                             + "(" + callPeer.getAddress() + ")",
                         exc);
            try
            {
                //we are connected from a SIP point of view (cause we sent our
                //ACK) so make sure we set the state accordingly or the hangup
                //method won't know how to end the call.
                callPeer.setState(CallPeerState.CONNECTED);
                hangupCallPeer(callPeer);
            }
            catch (Exception e)
            {
                //I don't see what more we could do.
                logger.error(e);
                callPeer.setState(CallPeerState.FAILED,
                                         e.getMessage());
            }
            return;
        }

        // change status
        if (!CallPeerState.isOnHold(callPeer.getState()))
            callPeer.setState(CallPeerState.CONNECTED);
    }

    /**
     * Logs <tt>message</tt> and <tt>cause</tt> and sets <tt>peer</tt> state
     * to <tt>CallPeerState.FAILED</tt>
     *
     * @param message a message to log and display to the user.
     * @param throwable the exception that cause the error we are logging
     * @param peer the peer that caused the error and that we are failing.
     */
    public static void logAndFailCallPeer(String message,
        Throwable throwable, CallPeerSipImpl peer)
    {
        logger.error(message, throwable);
        peer.setState(CallPeerState.FAILED, message);
    }

    /**
     * Handles early media in 183 Session Progress responses. Retrieves the SDP
     * and makes sure that we start transmitting and playing early media that we
     * receive. Puts the call into a CONNECTING_WITH_EARLY_MEDIA state.
     *
     * @param tran the <tt>ClientTransaction</tt> that the response
     * arrived in.
     * @param response the 183 <tt>Response</tt> to process
     * @param peer the peer that the <tt>Response</tt> is pertaining to.
     */
    public void processSessionProgress(ClientTransaction tran,
        Response response, CallPeerSipImpl peer)
    {

        if (response.getContentLength().getContentLength() == 0)
        {
            logger.debug("Ignoring a 183 with no content");
            return;
        }

        ContentTypeHeader contentTypeHeader = (ContentTypeHeader) response
                .getHeader(ContentTypeHeader.NAME);

        if (!contentTypeHeader.getContentType().equalsIgnoreCase("application")
            || !contentTypeHeader.getContentSubType().equalsIgnoreCase("sdp"))
        {
            //This can happen when receiving early media for a second time.
            logger.warn("Ignoring invite 183 since call peer is "
                + "already exchanging early media.");
            return;
        }

        // set sdp content before setting call state as that is where
        // listeners get alerted and they need the sdp
        peer.setSdpDescription(new String(response.getRawContent()));

        // notify the media manager of the sdp content
        CallSession callSession = peer.getCall().getMediaCallSession();

        if (callSession == null)
        {
            // unlikely to happen because it would mean we didn't send an offer
            // in the invite and we always send one.
            logger.warn("Could not find call session.");
            return;
        }

        try
        {
            callSession.processSdpAnswer(peer, peer.getSdpDescription());
        }
        catch (ParseException exc)
        {
            CallSipImpl.logAndFailCallPeer(
                "There was an error parsing the SDP description of "
                + peer.getDisplayName() + "("
                + peer.getAddress() + ")", exc, peer);
            return;
        }
        catch (MediaException exc)
        {
            CallSipImpl.logAndFailCallPeer(
                "We failed to process the SDP description of "
                + peer.getDisplayName() + "("
                + peer.getAddress() + ")" + ". Error was: "
                + exc.getMessage(), exc, peer);
            return;
        }

        // set the call url in case there was one
        peer.setCallInfoURL(callSession.getCallInfoURL());

        // change status
        peer.setState(CallPeerState.CONNECTING_WITH_EARLY_MEDIA);
    }

    /**
     * Sets the state of the specifies call peer as DISCONNECTED.
     *
     * @param serverTransaction the transaction that the cancel was received in.
     * @param cancelRequest the Request that we've just received.
     * @param callPeer the peer that sent the CANCEL request.
     */
    public void processCancel(ServerTransaction serverTransaction,
                               Request cancelRequest,
                               CallPeerSipImpl callPeer)
    {
        // Cancels should be OK-ed and the initial transaction - terminated
        // (report and fix by Ranga)
        try
        {
            Response ok = messageFactory.createResponse(Response.OK,
                            cancelRequest);
            serverTransaction.sendResponse(ok);

            logger.debug("sent an ok response to a CANCEL request:\n" + ok);
        }
        catch (ParseException ex)
        {
            CallSipImpl.logAndFailCallPeer(
                "Failed to create an OK Response to an CANCEL request.", ex,
                callPeer);
            return;
        }
        catch (Exception ex)
        {
            CallSipImpl.logAndFailCallPeer(
                "Failed to send an OK Response to an CANCEL request.", ex,
                callPeer);
            return;
        }

        try
        {
            // stop the invite transaction as well
            Transaction tran = callPeer.getFirstTransaction();
            // should be server transaction and misplaced cancels should be
            // filtered by the stack but it doesn't hurt checking anyway
            if (!(tran instanceof ServerTransaction))
            {
                logger.error("Received a misplaced CANCEL request!");
                return;
            }

            ServerTransaction inviteTran = (ServerTransaction) tran;
            Request invite = callPeer.getFirstTransaction().getRequest();
            Response requestTerminated = messageFactory
                .createResponse(Response.REQUEST_TERMINATED, invite);

            inviteTran.sendResponse(requestTerminated);
            if (logger.isDebugEnabled())
                logger.debug("sent request terminated response:\n"
                    + requestTerminated);
        }
        catch (ParseException ex)
        {
            logger.error("Failed to create a REQUEST_TERMINATED Response to "
                + "an INVITE request.", ex);
        }
        catch (Exception ex)
        {
            logger.error("Failed to send an REQUEST_TERMINATED Response to "
                + "an INVITE request.", ex);
        }

        // change status
        callPeer.setState(CallPeerState.DISCONNECTED);
    }

    /**
     * Sets the state of the corresponding call peer to DISCONNECTED and
     * sends an OK response.
     *
     * @param tran the ServerTransaction the the BYE request arrived in.
     * @param byeRequest the BYE request to process
     * @param callPeer the peer that sent the BYE request
     */
    public void processBye(ServerTransaction tran,
                           Request           byeRequest,
                           CallPeerSipImpl   callPeer)
    {
        // Send OK
        Response ok = null;
        try
        {
            ok = messageFactory.createResponse(Response.OK, byeRequest);
        }
        catch (ParseException ex)
        {
            logger.error("Error while trying to send a response to a bye", ex);
            // no need to let the user know about the error since it doesn't
            // affect them
            return;
        }

        try
        {
            tran.sendResponse(ok);
            logger.debug("sent response " + ok);
        }
        catch (Exception ex)
        {
            // This is not really a problem according to the RFC
            // so just dump to stdout should someone be interested
            logger.error("Failed to send an OK response to BYE request,"
                + "exception was:\n", ex);
        }

        // change status
        boolean dialogIsAlive;
        try
        {
            dialogIsAlive = EventPackageUtils.processByeThenIsDialogAlive(
                            tran.getDialog());
        }
        catch (SipException ex)
        {
            dialogIsAlive = false;

            logger.error(
                "Failed to determine whether the dialog should stay alive.",ex);
        }

        if (dialogIsAlive)
        {
            callPeer.getCall().getMediaCallSession().stopStreaming();
        }
        else
        {
            callPeer.setState(CallPeerState.DISCONNECTED);
        }
    }
}
