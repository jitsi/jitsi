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
     * Crates a CallSipImpl instance belonging to <tt>sourceProvider</tt> and
     * initiated by <tt>CallCreator</tt>.
     *
     * @param sourceProvider the ProtocolProviderServiceSipImpl instance in the
     *            context of which this call has been created.
     */
    protected CallSipImpl(ProtocolProviderServiceSipImpl sourceProvider)
    {
        super(sourceProvider);
        this.messageFactory = sourceProvider.getMessageFactory();
    }

    /**
     * Adds <tt>callPeer</tt> to the list of peers in this call.
     * If the call peer is already included in the call, the method has
     * no effect.
     *
     * @param callPeer the new <tt>CallPeer</tt>
     */
    public void addCallPeer(CallPeerSipImpl callPeer)
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
     *            the source event as well as its previous and its new status.
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
     * @param dialog the dialog whose corresponding peer we're looking
     *            for.
     * @return true if this call contains a call peer whose jain sip
     *         dialog is the same as the specified and false otherwise.
     */
    public boolean contains(Dialog dialog)
    {
        return findCallPeer(dialog) != null;
    }

    /**
     * Returns the call peer whose associated jain sip dialog matches
     * <tt>dialog</tt>.
     *
     * @param dialog the jain sip dialog whose corresponding peer we're
     *            looking for.
     * @return the call peer whose jain sip dialog is the same as the
     *         specified or null if no such call peer was found.
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
     *            created for this call.
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
     */
    private void attachSdpOffer(Request invite, CallPeerSipImpl callPeer)
        throws OperationFailedException
    {
        try
        {
            CallSession callSession =
                SipActivator.getMediaService().createCallSession(
                    callPeer.getCall());
            ((CallSipImpl) callPeer.getCall())
                .setMediaCallSession(callSession);

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

        callPeer.setState( (containingTransaction instanceof ServerTransaction)
                        ? CallPeerState.INCOMING_CALL
                        : CallPeerState.INITIATING_CALL);

        callPeer.setDialog(containingTransaction.getDialog());
        callPeer.setFirstTransaction(containingTransaction);
        callPeer.setJainSipProvider(sourceProvider);

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
        Request request = serverTransaction.getRequest();
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
     * Ends the call with the specified <tt>peer</tt>. Depending on the state
     * of the call the method would send a CANCEL, BYE, or BUSY_HERE message
     * and set the new state to DISCONNECTED.
     *
     * @param peer the peer that we'd like to hang up on.
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
            return DialogUtils.processByeThenIsDialogAlive(dialog);
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
     * @param peer the call peer that we need to send the ok to.
     * @throws OperationFailedException if we fail to create or send the
     *             response.
     */
    public synchronized void answerCallPeer(CallPeerSipImpl callPeer)
        throws OperationFailedException
    {
        Transaction transaction = callPeer.getFirstTransaction();
        Dialog dialog = callPeer.getDialog();

        if (transaction == null || !dialog.isServer())
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
     * @param sourceProvider the provider containing <tt>sourceTransaction</tt>.
     * @param serverTransaction the transaction containing the received request.
     * @param invite the Request that we've just received.
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
        catch (ParseException ex)
        {
            logger.error("Error while trying to create a response", ex);
            peer.setState(CallPeerState.FAILED,
                "Internal Error: " + ex.getMessage());
            return peer;
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

    public void processReInvite(SipProvider       jainSipProvider,
                                ServerTransaction serverTransaction)
    {
        Request request = serverTransaction.getRequest();

    }
}
