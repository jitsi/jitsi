/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip;

import gov.nist.javax.sip.header.HeaderFactoryImpl; // disambiguates Contact
import gov.nist.javax.sip.header.extensions.*;

import java.net.*;
import java.text.*;
import java.util.*;

import javax.sip.*;
import javax.sip.address.*;
import javax.sip.header.*;
import javax.sip.message.*;

import net.java.sip.communicator.service.media.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * Implements all call management logic and exports basic telephony support by
 * implementing OperationSetBasicTelephony.
 *
 * @author Emil Ivov
 * @author Lubomir Marinov
 * @author Alan Kelly
 * @author Emanuel Onica
 */
public class OperationSetBasicTelephonySipImpl
    extends AbstractOperationSetBasicTelephony
    implements MethodProcessor,
               OperationSetAdvancedTelephony,
               OperationSetSecureTelephony
{
    private static final Logger logger =
        Logger.getLogger(OperationSetBasicTelephonySipImpl.class);

    /**
     * A reference to the <tt>ProtocolProviderServiceSipImpl</tt> instance that
     * created us.
     */
    private final ProtocolProviderServiceSipImpl protocolProvider;

    /**
     * A reference to the <tt>SipMessageFactory</tt> instance that we should
     * use when creating requests.
     */
    private final SipMessageFactory messageFactory;

    /**
     * Contains references for all currently active (non ended) calls.
     */
    private final ActiveCallsRepository activeCallsRepository =
        new ActiveCallsRepository(this);

    /**
     * Creates a new instance and adds itself as an <tt>INVITE</tt> method
     * handler in the creating protocolProvider.
     *
     * @param protocolProvider a reference to the
     *            <tt>ProtocolProviderServiceSipImpl</tt> instance that created
     *            us.
     */
    public OperationSetBasicTelephonySipImpl(
        ProtocolProviderServiceSipImpl protocolProvider)
    {
        this.protocolProvider = protocolProvider;
        this.messageFactory = protocolProvider.getMessageFactory();

        protocolProvider.registerMethodProcessor(Request.INVITE, this);
        protocolProvider.registerMethodProcessor(Request.CANCEL, this);
        protocolProvider.registerMethodProcessor(Request.ACK, this);
        protocolProvider.registerMethodProcessor(Request.BYE, this);
        protocolProvider.registerMethodProcessor(Request.REFER, this);
        protocolProvider.registerMethodProcessor(Request.NOTIFY, this);

        protocolProvider.registerEvent("refer");
    }

    /**
     * Create a new call and invite the specified CallPeer to it.
     *
     * @param callee the sip address of the callee that we should invite to a
     *            new call.
     * @return CallPeer the CallPeer that will represented by the
     *         specified uri. All following state change events will be
     *         delivered through that call peer. The Call that this
     *         peer is a member of could be retrieved from the
     *         CallParticipatn instance with the use of the corresponding
     *         method.
     * @throws OperationFailedException with the corresponding code if we fail
     *             to create the call.
     * @throws ParseException if <tt>callee</tt> is not a valid sip address
     *             string.
     */
    public Call createCall(String callee)
        throws OperationFailedException,
        ParseException
    {
        Address toAddress = protocolProvider.parseAddressString(callee);

        return createOutgoingCall(toAddress, null);
    }

    /**
     * Create a new call and invite the specified CallPeer to it.
     *
     * @param callee the address of the callee that we should invite to a new
     *            call.
     * @return CallPeer the CallPeer that will represented by the
     *         specified uri. All following state change events will be
     *         delivered through that call peer. The Call that this
     *         peer is a member of could be retrieved from the
     *         CallParticipatn instance with the use of the corresponding
     *         method.
     * @throws OperationFailedException with the corresponding code if we fail
     *             to create the call.
     */
    public Call createCall(Contact callee) throws OperationFailedException
    {
        Address toAddress;

        try
        {
            toAddress = protocolProvider.parseAddressString(callee.getAddress());
        }
        catch (ParseException ex)
        {
            // couldn't happen
            logger.error(ex.getMessage(), ex);
            throw new IllegalArgumentException(ex.getMessage());
        }

        return createOutgoingCall(toAddress, null);
    }

    /**
     * Init and establish the specified call.
     *
     * @param calleeAddress the address of the callee that we'd like to connect
     *            with.
     * @param cause the <tt>Message</tt>, if any, which is the cause for the
     *            outgoing call to be placed and which carries additional
     *            information to be included in the call initiation (e.g. a
     *            Referred-To header and token in a Refer request)
     * @return CallPeer the CallPeer that will represented by the
     *         specified uri. All following state change events will be
     *         delivered through that call peer. The Call that this
     *         peer is a member of could be retrieved from the
     *         CallParticipatn instance with the use of the corresponding
     *         method.
     *
     * @throws OperationFailedException with the corresponding code if we fail
     *             to create the call.
     */
    private synchronized CallSipImpl createOutgoingCall(Address calleeAddress,
        javax.sip.message.Message cause) throws OperationFailedException
    {
        assertRegistered();

        CallSipImpl call = new CallSipImpl(protocolProvider);

        activeCallsRepository.addCall(call);
        call.invite(calleeAddress, cause);

        // notify everyone
        fireCallEvent( CallEvent.CALL_INITIATED, call);

        return call;
    }

    /**
     * Returns an iterator over all currently active calls.
     *
     * @return an iterator over all currently active calls.
     */
    public Iterator<CallSipImpl> getActiveCalls()
    {
        return activeCallsRepository.getActiveCalls();
    }

    /**
     * Resumes communication with a call peer previously put on hold.
     *
     * @param peer the call peer to put on hold.
     * @throws OperationFailedException
     */
    public synchronized void putOffHold(CallPeer peer)
        throws OperationFailedException
    {
        putOnHold(peer, false);
    }

    /**
     * Puts the specified CallPeer "on hold".
     *
     * @param peer the peer that we'd like to put on hold.
     * @throws OperationFailedException
     */
    public synchronized void putOnHold(CallPeer peer)
        throws OperationFailedException
    {
        putOnHold(peer, true);
    }

    /**
     * Puts the specified <tt>CallPeer</tt> on or off hold.
     *
     * @param peer the <tt>CallPeer</tt> to be put on or off hold
     * @param on <tt>true</tt> to have the specified <tt>CallPeer</tt>
     *            put on hold; <tt>false</tt>, otherwise
     * @throws OperationFailedException
     */
    private void putOnHold(CallPeer peer, boolean on)
        throws OperationFailedException
    {
        CallSession callSession =
            ((CallSipImpl) peer.getCall()).getMediaCallSession();
        CallPeerSipImpl sipPeer =
            (CallPeerSipImpl) peer;

        try
        {
            sendInviteRequest(sipPeer, callSession
                .createSdpDescriptionForHold(
                    sipPeer.getSdpDescription(), on));
        }
        catch (MediaException ex)
        {
            ProtocolProviderServiceSipImpl.throwOperationFailedException(
                "Failed to create SDP offer to hold.",
                OperationFailedException.INTERNAL_ERROR, ex, logger);
        }

        /*
         * Putting on hold isn't a negotiation (i.e. the issuing side takes the
         * decision and executes it) so we're muting now regardless of the
         * desire of the peer to accept the offer.
         */
        callSession.putOnHold(on, true);

        CallPeerState state = sipPeer.getState();
        if (CallPeerState.ON_HOLD_LOCALLY.equals(state))
        {
            if (!on)
                sipPeer.setState(CallPeerState.CONNECTED);
        }
        else if (CallPeerState.ON_HOLD_MUTUALLY.equals(state))
        {
            if (!on)
                sipPeer.setState(CallPeerState.ON_HOLD_REMOTELY);
        }
        else if (CallPeerState.ON_HOLD_REMOTELY.equals(state))
        {
            if (on)
                sipPeer.setState(CallPeerState.ON_HOLD_MUTUALLY);
        }
        else if (on)
        {
            sipPeer.setState(CallPeerState.ON_HOLD_LOCALLY);
        }
    }

    /**
     * Sends an invite request with a specific SDP offer (description) within
     * the current <tt>Dialog</tt> with a specific call peer.
     *
     * @param sipPeer the SIP-specific call peer to send the
     *            invite to within the current <tt>Dialog</tt>
     * @param sdpOffer the description of the SDP offer to be made to the
     *            specified call peer with the sent invite
     * @throws OperationFailedException
     */
    void sendInviteRequest(CallPeerSipImpl sipPeer, String sdpOffer)
        throws OperationFailedException
    {
        Dialog dialog = sipPeer.getDialog();
        Request invite = messageFactory.createRequest(dialog, Request.INVITE);

        try
        {
            invite.setContent(sdpOffer, protocolProvider.getHeaderFactory()
                .createContentTypeHeader("application", "sdp"));
        }
        catch (ParseException ex)
        {
            ProtocolProviderServiceSipImpl.throwOperationFailedException(
                "Failed to parse SDP offer for the new invite.",
                OperationFailedException.INTERNAL_ERROR, ex, logger);
        }

        sendRequest(sipPeer.getJainSipProvider(), invite, dialog);
    }

    /**
     * Sends a specific <tt>Request</tt> through a given
     * <tt>SipProvider</tt> as part of the conversation associated with a
     * specific <tt>Dialog</tt>.
     *
     * @param sipProvider the <tt>SipProvider</tt> to send the specified
     *            request through
     * @param request the <tt>Request</tt> to send through
     *            <tt>sipProvider</tt>
     * @param dialog the <tt>Dialog</tt> as part of which the specified
     *            <tt>request</tt> is to be sent
     * @throws OperationFailedException
     */
    private void sendRequest(SipProvider sipProvider,
                             Request request,
                             Dialog dialog)
        throws OperationFailedException
    {
        ClientTransaction clientTransaction = null;
        try
        {
            clientTransaction = sipProvider.getNewClientTransaction(request);
        }
        catch (TransactionUnavailableException ex)
        {
            ProtocolProviderServiceSipImpl.throwOperationFailedException(
                "Failed to create a client transaction for request:\n"
                + request, OperationFailedException.INTERNAL_ERROR, ex, logger);
        }

        try
        {
            dialog.sendRequest(clientTransaction);
        }
        catch (SipException ex)
        {
            ProtocolProviderServiceSipImpl.throwOperationFailedException(
                "Failed to send request:\n" + request,
                OperationFailedException.NETWORK_FAILURE,
                ex,
                logger);
        }

        logger.debug("Sent request:\n" + request);
    }

    /**
     * Processes a Request received on a SipProvider upon which this SipListener
     * is registered.
     * <p>
     *
     * @param requestEvent requestEvent fired from the SipProvider to the
     *            <tt>SipListener</tt> representing a Request received from the
     *            network.
     * @return <tt>true</tt> if the specified event has been handled by this
     *         processor and shouldn't be offered to other processors registered
     *         for the same method; <tt>false</tt>, otherwise
     */
    public boolean processRequest(RequestEvent requestEvent)
    {
        ServerTransaction serverTransaction =
            requestEvent.getServerTransaction();
        SipProvider jainSipProvider = (SipProvider) requestEvent.getSource();
        Request request = requestEvent.getRequest();
        String requestMethod = request.getMethod();

        if (serverTransaction == null)
        {
            try
            {
                serverTransaction =
                    SipStackSharing.getOrCreateServerTransaction(requestEvent);
            }
            catch (TransactionAlreadyExistsException ex)
            {
                // let's not scare the user and only log a message
                logger.error("Failed to create a new server"
                    + "transaction for an incoming request\n"
                    + "(Next message contains the request)", ex);
                return false;
            }
            catch (TransactionUnavailableException ex)
            {
                // let's not scare the user and only log a message
                logger.error("Failed to create a new server"
                    + "transaction for an incoming request\n"
                    + "(Next message contains the request)", ex);
                return false;
            }
        }

        boolean processed = false;

        // INVITE
        if (requestMethod.equals(Request.INVITE))
        {
            logger.debug("received INVITE");
            DialogState dialogState = serverTransaction.getDialog().getState();
            if ((dialogState == null)
                || dialogState.equals(DialogState.CONFIRMED))
            {
                if (logger.isDebugEnabled())
                    logger.debug("request is an INVITE. Dialog state="
                        + dialogState);
                processInvite(jainSipProvider, serverTransaction);
                processed = true;
            }
            else
            {
                logger.error("reINVITEs while the dialog is not"
                            + "confirmed are not currently supported.");
            }
        }
        // ACK
        else if (requestMethod.equals(Request.ACK))
        {
            processAck(serverTransaction, request);
            processed = true;
        }
        // BYE
        else if (requestMethod.equals(Request.BYE))
        {
            processBye(serverTransaction, request);
            processed = true;
        }
        // CANCEL
        else if (requestMethod.equals(Request.CANCEL))
        {
            processCancel(serverTransaction, request);
            processed = true;
        }
        // REFER
        else if (requestMethod.equals(Request.REFER))
        {
            logger.debug("received REFER");
            processRefer(serverTransaction, request, jainSipProvider);
            processed = true;
        }
        // NOTIFY
        else if (requestMethod.equals(Request.NOTIFY))
        {
            logger.debug("received NOTIFY");
            processed = processNotify(serverTransaction, request);
        }

        return processed;
    }

    /**
     * Process an asynchronously reported TransactionTerminatedEvent.
     *
     * @param transactionTerminatedEvent -- an event that indicates that the
     *            transaction has transitioned into the terminated state.
     * @return <tt>true</tt> if the specified event has been handled by this
     *         processor and shouldn't be offered to other processors registered
     *         for the same method; <tt>false</tt>, otherwise
     */
    public boolean processTransactionTerminated(
        TransactionTerminatedEvent transactionTerminatedEvent)
    {
        // nothing to do here.
        return false;
    }

    /**
     * Analyzes the incoming <tt>responseEvent</tt> and then forwards it to the
     * proper event handler.
     *
     * @param responseEvent the responseEvent that we received
     *            ProtocolProviderService.
     * @return <tt>true</tt> if the specified event has been handled by this
     *         processor and shouldn't be offered to other processors registered
     *         for the same method; <tt>false</tt>, otherwise
     */
    public boolean processResponse(ResponseEvent responseEvent)
    {
        ClientTransaction clientTransaction =
            responseEvent.getClientTransaction();

        Response response = responseEvent.getResponse();

        CSeqHeader cseq = ((CSeqHeader) response.getHeader(CSeqHeader.NAME));

        if (cseq == null)
        {
            logger.error("An incoming response did not contain a CSeq header");
        }

        String method = cseq.getMethod();

        SipProvider sourceProvider = (SipProvider) responseEvent.getSource();

        int responseStatusCode = response.getStatusCode();
        boolean processed = false;
        switch (responseStatusCode)
        {

        // OK
        case Response.OK:
            if (method.equals(Request.INVITE))
            {
                processInviteOK(clientTransaction, response);
                processed = true;
            }
            // Ignore the case of method.equals(Request.BYE)
            break;

        // Ringing
        case Response.RINGING:
            processRinging(clientTransaction, response);
            processed = true;
            break;

        // Session Progress
        case Response.SESSION_PROGRESS:
            processSessionProgress(clientTransaction, response);
            processed = true;
            break;

        // Trying
        case Response.TRYING:
            processTrying(clientTransaction, response);
            processed = true;
            break;

        // Busy
        case Response.BUSY_HERE:
        case Response.BUSY_EVERYWHERE:
        case Response.DECLINE:
            processBusyHere(clientTransaction, response);
            processed = true;
            break;

        // Accepted
        case Response.ACCEPTED:
            if (Request.REFER.equals(method))
            {
                processReferAccepted(clientTransaction, response);
                processed = true;
            }
            break;

        // 401 UNAUTHORIZED
        case Response.UNAUTHORIZED:
        case Response.PROXY_AUTHENTICATION_REQUIRED:
            processAuthenticationChallenge(clientTransaction, response,
                sourceProvider);
            processed = true;
            break;

        // errors
        default:
            if ((responseStatusCode / 100 == 4)
                || (responseStatusCode / 100 == 5)
                || (responseStatusCode / 100 == 6))
            {
                CallPeerSipImpl callPeer =
                    activeCallsRepository.findCallPeer(clientTransaction
                        .getDialog());

                logger.error("Received error: " + response.getStatusCode()
                    + " " + response.getReasonPhrase());

                if (callPeer != null)
                    callPeer.setState(CallPeerState.FAILED);

                processed = true;
            }
            // ignore everything else.
            break;
        }
        return processed;
    }

    /**
     * Processes a specific <tt>Response.ACCEPTED</tt> response of an
     * earlier <tt>Request.REFER</tt> request.
     *
     * @param clientTransaction the <tt>ClientTransaction</tt> which brought
     *            the response
     * @param accepted the <tt>Response.ACCEPTED</tt> response to an earlier
     *            <tt>Request.REFER</tt> request
     */
    private void processReferAccepted(ClientTransaction clientTransaction,
        Response accepted)
    {
        try
        {
            DialogUtils.addSubscription(clientTransaction.getDialog(), "refer");
        }
        catch (SipException ex)
        {
            logger.error("Failed to make Accepted REFER response"
                        + " keep the dialog alive after BYE:\n"
                    + accepted, ex);
        }
    }

    /**
     * Updates the call state of the corresponding call peer.
     *
     * @param clientTransaction the transaction in which the response was
     *            received.
     * @param response the trying response.
     */
    private void processTrying(ClientTransaction clientTransaction,
        Response response)
    {
        Dialog dialog = clientTransaction.getDialog();
        // find the call peer
        CallPeerSipImpl callPeer =
            activeCallsRepository.findCallPeer(dialog);

        if (callPeer == null)
        {
            logger.debug("Received a stray trying response.");
            return;
        }

        // change status
        CallPeerState callPeerState = callPeer.getState();
        if (!CallPeerState.CONNECTED.equals(callPeerState)
            && !CallPeerState.isOnHold(callPeerState))
            callPeer.setState(CallPeerState.CONNECTING);
    }

    /**
     * Updates the call state of the corresponding call peer. We'll also
     * try to extract any details here that might be of use for call peer
     * presentation and that we didn't have when establishing the call.
     *
     * @param clientTransaction the transaction in which the response was
     *            received.
     * @param response the Trying response.
     */
    private void processRinging(ClientTransaction clientTransaction,
        Response response)
    {
        Dialog dialog = clientTransaction.getDialog();
        // find the call peer
        CallPeerSipImpl callPeer =
            activeCallsRepository.findCallPeer(dialog);

        if (callPeer == null)
        {
            logger.debug("Received a stray trying response.");
            return;
        }

        // try to update the display name.
        ContactHeader remotePartyContactHeader =
            (ContactHeader) response.getHeader(ContactHeader.NAME);

        if (remotePartyContactHeader != null)
        {
            Address remotePartyAddress = remotePartyContactHeader.getAddress();

            String displayName = remotePartyAddress.getDisplayName();

            if (displayName != null && displayName.trim().length() > 0)
            {
                callPeer.setDisplayName(displayName);
            }
        }

        // change status.
        callPeer.setState(CallPeerState.ALERTING_REMOTE_SIDE);
    }

    /**
     * Handles early media in 183 Session Progress responses. Retrieves the SDP
     * and makes sure that we start transmitting and playing early media that we
     * receive. Puts the call into a CONNECTING_WITH_EARLY_MEDIA state.
     *
     * @param clientTransaction the <tt>ClientTransaction</tt> that the response
     *            arrived in.
     * @param sessionProgress the 183 <tt>Response</tt> to process
     */
    private void processSessionProgress(ClientTransaction clientTransaction,
        Response sessionProgress)
    {

        Dialog dialog = clientTransaction.getDialog();
        // find the call
        CallPeerSipImpl callPeer =
            activeCallsRepository.findCallPeer(dialog);

        if (callPeer.getState()
                == CallPeerState.CONNECTING_WITH_EARLY_MEDIA)
        {
            // This can happen if we are receiving early media for a second time.
            logger.warn("Ignoring invite 183 since call peer is "
                + "already exchanging early media.");
            return;
        }

        if (sessionProgress.getContentLength().getContentLength() == 0)
        {
            logger.warn("Ignoring a 183 with no content");
            return;
        }

        ContentTypeHeader contentTypeHeader =
            (ContentTypeHeader) sessionProgress
                .getHeader(ContentTypeHeader.NAME);

        if (!contentTypeHeader.getContentType().equalsIgnoreCase("application")
            || !contentTypeHeader.getContentSubType().equalsIgnoreCase("sdp"))
        {
            // This can happen if we are receiving early media for a second time.
            logger.warn("Ignoring invite 183 since call peer is "
                + "already exchanging early media.");
            return;
        }

        // set sdp content before setting call state as that is where
        // listeners get alerted and they need the sdp
        callPeer.setSdpDescription(new String(sessionProgress
            .getRawContent()));

        // notify the media manager of the sdp content
        CallSession callSession =
            ((CallSipImpl) callPeer.getCall()).getMediaCallSession();

        if (callSession == null)
        {
            // unlikely to happen because it would mean we didn't send an offer
            // in the invite and we always send one.
            logger.warn("Could not find call session.");
            return;
        }

        try
        {
            callSession.processSdpAnswer(callPeer, new String(
                sessionProgress.getRawContent()));
        }
        catch (ParseException exc)
        {
            logErrorAndFailCallPeer(
                "There was an error parsing the SDP description of "
                    + callPeer.getDisplayName() + "("
                    + callPeer.getAddress() + ")", exc, callPeer);
            return;
        }
        catch (MediaException exc)
        {
            logErrorAndFailCallPeer(
                "We failed to process the SDP description of "
                    + callPeer.getDisplayName() + "("
                    + callPeer.getAddress() + ")" + ". Error was: "
                    + exc.getMessage(), exc, callPeer);
            return;
        }

        // set the call url in case there was one
        /**
         * @todo this should be done in CallSession, once we move it here.
         */
        callPeer.setCallInfoURL(callSession.getCallInfoURL());

        // change status
        callPeer
            .setState(CallPeerState.CONNECTING_WITH_EARLY_MEDIA);
    }

    /**
     * Sets to CONNECTED that state of the corresponding call peer and
     * sends an ACK.
     *
     * @param clientTransaction the <tt>ClientTransaction</tt> that the response
     *            arrived in.
     * @param ok the OK <tt>Response</tt> to process
     */
    private void processInviteOK(ClientTransaction clientTransaction,
        Response ok)
    {
        Dialog dialog = clientTransaction.getDialog();
        // find the call
        CallPeerSipImpl callPeer =
            activeCallsRepository.findCallPeer(dialog);

        if (callPeer == null)
        {

            /*
             * In case of forwarding a call, the dialog may have forked. If the
             * dialog is forked, we must end early state dialogs by replacing
             * the dialog with the new one.
             */
            CallIdHeader call = (CallIdHeader) ok.getHeader(CallIdHeader.NAME);
            String callid = call.getCallId();

            Iterator<CallSipImpl> activeCallsIter
                                    = activeCallsRepository.getActiveCalls();
            while (activeCallsIter.hasNext())
            {
                CallSipImpl activeCall = activeCallsIter.next();
                Iterator<CallPeer> callPeersIter =
                    activeCall.getCallPeers();
                while (callPeersIter.hasNext())
                {
                    CallPeerSipImpl cp =
                        (CallPeerSipImpl) callPeersIter.next();
                    Dialog callPartDialog = cp.getDialog();
                    // check if peer in same call
                    // and has the same transaction
                    if (callPartDialog != null
                        && callPartDialog.getCallId() != null
                        && cp.getFirstTransaction() != null
                        && cp.getDialog().getCallId().getCallId()
                            .equals(callid)
                        && clientTransaction.getBranchId().equals(
                            cp.getFirstTransaction().getBranchId()))
                    {
                        // change to the forked dialog
                        callPeer = cp;
                        cp.setDialog(dialog);
                    }
                }
            }

            if (callPeer == null)
            {
                logger.debug("Received a stray ok response.");
                return;
            }
        }

        /*
         * Receiving an Invite OK is allowed even when the peer is
         * already connected. Examples include call hold, enabling/disabling the
         * streaming of local video while in a call.
         */

        Request ack = null;
        ContentTypeHeader contentTypeHeader = null;
        // Create ACK
        try
        {
            // Need to use dialog generated ACKs so that the remote UA core
            // sees them - Fixed by M.Ranganathan
            CSeqHeader cseq = ((CSeqHeader) ok.getHeader(CSeqHeader.NAME));
            ack = clientTransaction.getDialog().createAck(cseq.getSeqNumber());

            // Content should it be necessary.

            // content type should be application/sdp (not applications)
            // reported by Oleg Shevchenko (Miratech)
            contentTypeHeader =
                protocolProvider.getHeaderFactory().createContentTypeHeader(
                    "application", "sdp");
        }
        catch (ParseException ex)
        {
            // Shouldn't happen
            logErrorAndFailCallPeer(
                "Failed to create a content type header for the ACK request",
                ex, callPeer);
            return;
        }
        catch (InvalidArgumentException ex)
        {
            // Shouldn't happen
            logErrorAndFailCallPeer(
                "Failed ACK request, problem with the supplied cseq", ex,
                callPeer);
            return;
        }
        catch (SipException ex)
        {
            logErrorAndFailCallPeer("Failed to create ACK request!", ex,
                callPeer);
            return;
        }

        // !!! set sdp content before setting call state as that is where
        // listeners get alerted and they need the sdp
        // ignore sdp if we have already received one in early media
        if(!CallPeerState.CONNECTING_WITH_EARLY_MEDIA
               .equals(callPeer.getState()))
            callPeer.setSdpDescription(new String(ok.getRawContent()));

        // notify the media manager of the sdp content
        CallSession callSession =
            ((CallSipImpl) callPeer.getCall()).getMediaCallSession();

        try
        {
            try
            {
                if (callSession == null)
                {
                    // non existent call session - that means we didn't send sdp
                    // in the invite and this is the offer so we need to create
                    // the answer.
                    callSession =
                        SipActivator.getMediaService().createCallSession(
                            callPeer.getCall());

                    callSession
                        .setSessionCreatorCallback(callPeer);

                    String sdp
                        = callSession.processSdpOffer(
                                callPeer,
                                callPeer.getSdpDescription());
                    ack.setContent(sdp, contentTypeHeader);

                    // set the call url in case there was one
                    /**
                     * @todo this should be done in CallSession, once we move it
                     *       here.
                     */
                    callPeer
                        .setCallInfoURL(callSession.getCallInfoURL());
                }
            }
            finally
            {
                // Send the ACK now since we got all the info we need,
                // and callSession.processSdpAnswer can take a few seconds.
                // (patch by Michael Koch)
                try
                {
                    clientTransaction.getDialog().sendAck(ack);
                }
                catch (SipException ex)
                {
                    logErrorAndFailCallPeer(
                        "Failed to acknowledge call!", ex, callPeer);
                    return;
                }
            }

            /*
             * We used to not process the SDP if we had already received one in
             * early media. But functionality using re-invites (e.g. toggling
             * the streaming of local video while in a call) may need to process
             * the SDP (e.g. because of re-negotiating the media after toggling
             * the streaming of local video).
             */
            CallPeerState callPeerState =
                callPeer.getState();
            if (!CallPeerState.CONNECTING_WITH_EARLY_MEDIA
                    .equals(callPeerState))
            {
                callSession.processSdpAnswer(
                    callPeer,
                    callPeer.getSdpDescription());
            }

            // set the call url in case there was one
            /**
             * @todo this should be done in CallSession, once we move it here.
             */
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
                //ack) so make sure we set the state accordingly or the hangup
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

    private void logErrorAndFailCallPeer(String message,
        Throwable throwable, CallPeerSipImpl peer)
    {
        logger.error(message, throwable);
        peer.setState(CallPeerState.FAILED, message);
    }

    /**
     * Sets corresponding state to the call peer associated with this
     * transaction.
     *
     * @param clientTransaction the transaction in which
     * @param busyHere the busy here Response
     */
    private void processBusyHere(ClientTransaction clientTransaction,
        Response busyHere)
    {
        Dialog dialog = clientTransaction.getDialog();
        // find the call
        CallPeerSipImpl callPeer =
            activeCallsRepository.findCallPeer(dialog);

        if (callPeer == null)
        {
            logger.debug("Received a stray busyHere response.");
            return;
        }

        // change status
        callPeer.setState(CallPeerState.BUSY);
    }

    /**
     * Attempts to re-generate the corresponding request with the proper
     * credentials and terminates the call if it fails.
     *
     * @param clientTransaction the corresponding transaction
     * @param response the challenge
     * @param jainSipProvider the provider that received the challenge
     */
    private void processAuthenticationChallenge(
        ClientTransaction clientTransaction, Response response,
        SipProvider jainSipProvider)
    {
        // First find the call and the call peer that this authentication
        // request concerns.
        CallPeerSipImpl callPeer =
            activeCallsRepository.findCallPeer(clientTransaction
                .getDialog());

        if (callPeer == null)
        {
            logger.debug("Received an authorization challenge for no "
                + "peer. authorizing anyway.");
        }

        try
        {
            logger.debug("Authenticating an INVITE request.");

            ClientTransaction retryTran =
                protocolProvider.getSipSecurityManager().handleChallenge(
                    response, clientTransaction, jainSipProvider);

            if (retryTran == null)
            {
                logger.trace("No password supplied or error occured!");
                return;
            }

            // There is a new dialog that will be started with this request. Get
            // that dialog and record it into the Call objet for later use (by
            // Bye-s for example).
            // if the request was BYE then we need to authorize it anyway even
            // if the call and the call peer are no longer there
            if (callPeer != null)
            {
                callPeer.setDialog(retryTran.getDialog());
                callPeer.setFirstTransaction(retryTran);
                callPeer.setJainSipProvider(jainSipProvider);
            }
            retryTran.sendRequest();
        }
        catch (Exception exc)
        {
            // tell the others we couldn't register
            logErrorAndFailCallPeer(
                "We failed to authenticate an INVITE request.", exc,
                callPeer);
            return;
        }
    }

    /**
     * Processes a retransmit or expiration Timeout of an underlying
     * {@link Transaction}handled by this SipListener. This Event notifies the
     * application that a retransmission or transaction Timer expired in the
     * SipProvider's transaction state machine. The TimeoutEvent encapsulates
     * the specific timeout type and the transaction identifier either client or
     * server upon which the timeout occurred. The type of Timeout can by
     * determined by:
     * <tt>timeoutType = timeoutEvent.getTimeout().getValue();</tt>
     *
     * @param timeoutEvent the timeoutEvent received indicating either the
     *            message retransmit or transaction timed out.
     * @return <tt>true</tt> if the specified event has been handled by this
     *         processor and shouldn't be offered to other processors registered
     *         for the same method; <tt>false</tt>, otherwise
     */
    public boolean processTimeout(TimeoutEvent timeoutEvent)
    {
        Transaction transaction;
        if (timeoutEvent.isServerTransaction())
        {
            // don't care. or maybe a stack bug?
            return false;
        }
        else
        {
            transaction = timeoutEvent.getClientTransaction();
        }

        CallPeerSipImpl callPeer =
            activeCallsRepository.findCallPeer(transaction.getDialog());

        if (callPeer == null)
        {
            logger.debug("Got a headless timeout event." + timeoutEvent);
            return false;
        }

        // change status
        callPeer.setState(CallPeerState.FAILED,
            "The remote party has not replied!"
                + "The call will be disconnected");
        return true;
    }

    /**
     * Process an asynchronously reported IO Exception. Asynchronous IO
     * Exceptions may occur as a result of errors during retransmission of
     * requests. The transaction state machine requires to report IO Exceptions
     * to the application immediately (according to RFC 3261). This method
     * enables an implementation to propagate the asynchronous handling of IO
     * Exceptions to the application.
     *
     * @param exceptionEvent The Exception event that is reported to the
     *            application.
     * @return <tt>true</tt> if the specified event has been handled by this
     *         processor and shouldn't be offered to other processors registered
     *         for the same method; <tt>false</tt>, otherwise
     */
    public boolean processIOException(IOExceptionEvent exceptionEvent)
    {
        logger.error("Got an asynchronous exception event. host="
            + exceptionEvent.getHost() + " port=" + exceptionEvent.getPort());
        return true;
    }

    /**
     * Process an asynchronously reported DialogTerminatedEvent.
     *
     * @param dialogTerminatedEvent -- an event that indicates that the dialog
     *            has transitioned into the terminated state.
     * @return <tt>true</tt> if the specified event has been handled by this
     *         processor and shouldn't be offered to other processors registered
     *         for the same method; <tt>false</tt>, otherwise
     */
    public boolean processDialogTerminated(
        DialogTerminatedEvent dialogTerminatedEvent)
    {
        CallPeerSipImpl callPeer =
            activeCallsRepository.findCallPeer(dialogTerminatedEvent
                .getDialog());

        if (callPeer == null)
        {
            return false;
        }

        // change status
        callPeer.setState(CallPeerState.DISCONNECTED);
        return true;
    }



    /**
     * Creates a new call and sends a RINGING response.
     *
     * @param sourceProvider the provider containing <tt>sourceTransaction</tt>.
     * @param serverTransaction the transaction containing the received request.
     * @param invite the Request that we've just received.
     */
    private void processInvite(SipProvider       sourceProvider,
                               ServerTransaction serverTransaction)
    {
        //first check whether this is a reINVITE or a brand new one.
        Request     invite      = serverTransaction.getRequest();
        Dialog      dialog      = serverTransaction.getDialog();
        CallSipImpl callSipImpl = activeCallsRepository.findCall(dialog);

        if(callSipImpl == null)
        {
            //this is not a reINVITE. check if it's a transfer
            //(i.e. replacing an existing call).
            ReplacesHeader replacesHeader =
                (ReplacesHeader) invite.getHeader(ReplacesHeader.NAME);

            if (replacesHeader == null)
            {
                //this is a brand new call (not a transfered one)
                callSipImpl = new CallSipImpl(protocolProvider);
                callSipImpl.processInvite(sourceProvider, serverTransaction);
            }
            else
            {
                //this is a transfered call which is replacing an existing one
                //(i.e. an attended transfer).
                callSipImpl = activeCallsRepository.findCall(
                    replacesHeader.getCallId(), replacesHeader.getToTag(),
                    replacesHeader.getFromTag());

                if (callSipImpl != null)
                {
                    callSipImpl.processReplacingInvite(sourceProvider,
                                                       serverTransaction);
                }
                else
                {
                    this.sayErrorSilently(
                        serverTransaction,
                        Response.CALL_OR_TRANSACTION_DOES_NOT_EXIST);
                }
            }
        }
        else
        {
            //this is a reINVITE.
            callSipImpl.processReInvite(sourceProvider, serverTransaction);
        }



    }

    /**
     * Creates a new call and sends a RINGING response.
     *
     * @param sourceProvider the provider containing <tt>sourceTransaction</tt>.
     * @param serverTransaction the transaction containing the received request.
     * @param invite the Request that we've just received.
     */
    private void originalProcessInvite(SipProvider sourceProvider,
                               ServerTransaction serverTransaction,
                               Request invite)
    {
        Dialog dialog = serverTransaction.getDialog();
        CallPeerSipImpl callPeer =
            activeCallsRepository.findCallPeer(dialog);
        int statusCode = 0;
        CallPeerSipImpl callPeerToReplace = null;

        if (callPeer == null)
        {
            ReplacesHeader replacesHeader =
                (ReplacesHeader) invite.getHeader(ReplacesHeader.NAME);

            if (replacesHeader == null)
            {
                //this is not a transfered call so start ringing
                statusCode = Response.RINGING;
            }
            else
            {
                //this is a transfered call
                //.. ok or error if no peer found
            }

            logger.trace("Creating call peer.");
            callPeer =
                createCallPeerFor(serverTransaction, sourceProvider);
            logger.trace("call peer created = " + callPeer);
        }
        else
        {
            //this is a reINVITE - so we'll OK it without ringing
            statusCode = Response.OK;
        }

        // extract the SDP description.
        // beware: SDP description may be in ACKs - bug report Laurent Michel
        ContentLengthHeader cl = invite.getContentLength();
        if (cl != null && cl.getContentLength() > 0)
        {
            callPeer.setSdpDescription(new String(invite.getRawContent()));
        }

        // INVITE w/ Replaces
        //..

        // Send statusCode
        //...
        Response response = null;
        try
        {
            response = protocolProvider.getMessageFactory()
                .createResponse(statusCode, invite);

           // set our display name
            ((ToHeader) response.getHeader(ToHeader.NAME)).getAddress()
                .setDisplayName(protocolProvider.getOurDisplayName());

            response.setHeader(protocolProvider
                            .getContactHeaderForResponse(invite));

            //add the SDP
            if (statusCode == Response.OK)
            {
                try
                {
                    attachSDP(callPeer, response);
                }
                catch (OperationFailedException ex)
                {
                    logger.error("Error while trying to send response "
                        + response, ex);
                    callPeer.setState(CallPeerState.FAILED,
                        "Internal Error: " + ex.getMessage());
                    return;
                }
            }
        }
        catch (ParseException ex)
        {
            logger.error("Error while trying to send a response", ex);
            callPeer.setState(CallPeerState.FAILED,
                "Internal Error: " + ex.getMessage());
            return;
        }
        try
        {
            logger.trace("will send " + statusCode + " response: ");
            serverTransaction.sendResponse(response);
            logger.debug("sent a " + statusCode + " response: "
                + response);
        }
        catch (Exception ex)
        {
            logger.error("Error while trying to send a request", ex);
            callPeer.setState(CallPeerState.FAILED,
                "Internal Error: " + ex.getMessage());
            return;
        }

        if (statusCode == Response.OK)
        {
            try
            {
                setMediaFlagsForPeer(callPeer, response);
            }
            catch (OperationFailedException ex)
            {
                logger.error("Error after sending response " + response, ex);
            }
        }
    }

    /**
     * Provides a hook for this instance to take last configuration steps on a
     * specific <tt>Response</tt> before it is sent to a specific
     * <tt>CallPeer</tt> as part of the execution of
     * {@link #processInvite(SipProvider, ServerTransaction, Request)}.
     *
     * @param peer the <tt>CallPeer</tt> to receive a specific
     *            <tt>Response</tt>
     * @param response the <tt>Response</tt> to be sent to the
     *            <tt>peer</tt>
     * @throws OperationFailedException
     * @throws ParseException
     */
    private void attachSDP(CallPeer peer, Response response)
        throws OperationFailedException, ParseException
    {
        /*
         * At the time of this writing, we're only getting called because a
         * response to a call-hold invite is to be sent.
         */

        CallSession callSession =
            ((CallSipImpl) peer.getCall()).getMediaCallSession();

        String sdpAnswer = null;
        try
        {
            sdpAnswer
                = callSession.processSdpOffer(
                        peer,
                        ((CallPeerSipImpl) peer)
                            .getSdpDescription());
        }
        catch (MediaException ex)
        {
            ProtocolProviderServiceSipImpl.throwOperationFailedException(
                "Failed to create SDP answer to put-on/off-hold request.",
                OperationFailedException.INTERNAL_ERROR, ex, logger);
        }

        response.setContent(
            sdpAnswer,
            protocolProvider.getHeaderFactory()
                .createContentTypeHeader("application", "sdp"));
    }

    /**
     * Provides a hook for this instance to take immediate steps after a
     * specific <tt>Response</tt> has been sent to a specific
     * <tt>CallPeer</tt> as part of the execution of
     * {@link #processInvite(SipProvider, ServerTransaction, Request)}.
     *
     * @param peer the <tt>CallPeer</tt> who was sent a specific
     *            <tt>Response</tt>
     * @param response the <tt>Response</tt> that has just been sent to the
     *            <tt>peer</tt>
     * @throws OperationFailedException
     * @throws ParseException
     */
    private void setMediaFlagsForPeer(CallPeer peer, Response response)
        throws OperationFailedException
    {
        /*
         * At the time of this writing, we're only getting called because a
         * response to a call-hold invite is to be sent.
         */

        CallSession callSession =
            ((CallSipImpl) peer.getCall()).getMediaCallSession();
        CallPeerSipImpl sipPeer =
            (CallPeerSipImpl) peer;

        int mediaFlags = 0;
        try
        {
            mediaFlags = callSession .getSdpOfferMediaFlags(
                            sipPeer.getSdpDescription());
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

        CallPeerState state = sipPeer.getState();
        if (CallPeerState.ON_HOLD_LOCALLY.equals(state))
        {
            if (on)
                sipPeer.setState(CallPeerState.ON_HOLD_MUTUALLY);
        }
        else if (CallPeerState.ON_HOLD_MUTUALLY.equals(state))
        {
            if (!on)
                sipPeer.setState(CallPeerState.ON_HOLD_LOCALLY);
        }
        else if (CallPeerState.ON_HOLD_REMOTELY.equals(state))
        {
            if (!on)
                sipPeer.setState(CallPeerState.CONNECTED);
        }
        else if (on)
        {
            sipPeer.setState(CallPeerState.ON_HOLD_REMOTELY);
        }

        /*
         * Reflect the request of the SDP offer with respect to the modification
         * of the availability of media.
         */
        callSession.setReceiveStreaming(mediaFlags);
    }

    /**
     * Sets the state of the corresponding call peer to DISCONNECTED and
     * sends an OK response.
     *
     * @param serverTransaction the ServerTransaction the the BYE request
     *            arrived in.
     * @param byeRequest the BYE request to process
     */
    private void processBye(ServerTransaction serverTransaction,
        Request byeRequest)
    {
        // find the call
        Dialog dialog = serverTransaction.getDialog();
        CallPeerSipImpl callPeer =
            activeCallsRepository.findCallPeer(dialog);

        if (callPeer == null)
        {
            logger.debug("Received a stray bye request.");
            return;
        }

        // Send OK
        Response ok = null;
        try
        {
            ok = createOKResponse(byeRequest, dialog);
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
            serverTransaction.sendResponse(ok);
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
            dialogIsAlive = DialogUtils.processByeThenIsDialogAlive(dialog);
        }
        catch (SipException ex)
        {
            dialogIsAlive = false;

            logger
                .error(
                    "Failed to determine whether the dialog should stay alive.",
                    ex);
        }
        if (dialogIsAlive)
        {
            ((CallSipImpl) callPeer.getCall()).getMediaCallSession()
                .stopStreaming();
        }
        else
        {
            callPeer.setState(CallPeerState.DISCONNECTED);
        }
    }

    /**
     * Updates the session description and sends the state of the corresponding
     * call peer to CONNECTED.
     *
     * @param serverTransaction the transaction that the Ack was received in.
     * @param ackRequest Request
     */
    private void processAck(ServerTransaction serverTransaction,
                            Request ackRequest)
    {
        // find the call
        CallPeerSipImpl peer
            = activeCallsRepository.findCallPeer(
                    serverTransaction.getDialog());

        if (peer == null)
        {
            // this is most probably the ack for a killed call - don't signal it
            logger.debug("didn't find an ack's call, returning");
            return;
        }

        ContentLengthHeader contentLength = ackRequest.getContentLength();
        if ((contentLength != null) && (contentLength.getContentLength() > 0))
        {
            peer.setSdpDescription(
                new String(ackRequest.getRawContent()));
        }

        // change status
        CallPeerState peerState = peer.getState();
        if (!CallPeerState.isOnHold(peerState))
        {
            if (CallPeerState.CONNECTED.equals(peerState))
            {
                try
                {
                    ((CallSipImpl) peer.getCall())
                        .getMediaCallSession()
                            .startStreamingAndProcessingMedia();
                }
                catch (MediaException ex)
                {
                    logger.error(
                        "Failed to start the streaming"
                            + " and the processing of the media",
                        ex);
                }
            }
            else
                peer.setState(CallPeerState.CONNECTED);
        }
    }

    /**
     * Sets the state of the specifies call peer as DISCONNECTED.
     *
     * @param serverTransaction the transaction that the cancel was received in.
     * @param cancelRequest the Request that we've just received.
     */
    private void processCancel(ServerTransaction serverTransaction,
                               Request cancelRequest)
    {
        // find the call
        CallPeerSipImpl callPeer =
            activeCallsRepository.findCallPeer(serverTransaction
                .getDialog());

        if (callPeer == null)
        {
            logger.debug("received a stray CANCEL req. ignoring");
            return;
        }

        // Cancels should be OK-ed and the initial transaction - terminated
        // (report and fix by Ranga)
        try
        {
            Response ok =
                createOKResponse(cancelRequest, serverTransaction.getDialog());
            serverTransaction.sendResponse(ok);

            logger.debug("sent an ok response to a CANCEL request:\n" + ok);
        }
        catch (ParseException ex)
        {
            logErrorAndFailCallPeer(
                "Failed to create an OK Response to an CANCEL request.", ex,
                callPeer);
            return;
        }
        catch (Exception ex)
        {
            logErrorAndFailCallPeer(
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
            Response requestTerminated = protocolProvider.getMessageFactory()
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
     * Processes a specific REFER request i.e. attempts to transfer the
     * call/call peer receiving the request to a specific transfer
     * target.
     *
     * @param serverTransaction the <tt>ServerTransaction</tt> containing
     *            the REFER request
     * @param referRequest the very REFER request
     * @param sipProvider the provider containing <tt>serverTransaction</tt>
     */
    private void processRefer(ServerTransaction serverTransaction,
        final Request referRequest, final SipProvider sipProvider)
    {
        ReferToHeader referToHeader =
            (ReferToHeader) referRequest.getHeader(ReferToHeader.NAME);
        if (referToHeader == null)
        {
            logger.error("No Refer-To header in REFER request:\n"
                + referRequest);
            return;
        }
        Address referToAddress = referToHeader.getAddress();
        if (referToAddress == null)
        {
            logger.error("No address in REFER request Refer-To header:\n"
                + referRequest);
            return;
        }

        // Accepted
        final Dialog dialog = serverTransaction.getDialog();
        Response accepted = null;
        try
        {
            accepted = protocolProvider.getMessageFactory().createResponse(
                    Response.ACCEPTED, referRequest);
        }
        catch (ParseException ex)
        {
            logger.error(
                "Failed to create Accepted response to REFER request:\n"
                    + referRequest, ex);
            /*
             * TODO Should the call transfer not be attempted because the
             * Accepted couldn't be sent?
             */
        }
        boolean removeSubscription = false;
        if (accepted != null)
        {
            Throwable failure = null;
            try
            {
                serverTransaction.sendResponse(accepted);
            }
            catch (InvalidArgumentException ex)
            {
                failure = ex;
            }
            catch (SipException ex)
            {
                failure = ex;
            }
            if (failure != null)
            {
                accepted = null;

                logger.error(
                    "Failed to send Accepted response to REFER request:\n"
                        + referRequest, failure);
                /*
                 * TODO Should the call transfer not be attempted because the
                 * Accepted couldn't be sent?
                 */
            }
            else
            {

                /*
                 * The REFER request has created a subscription. Take it into
                 * consideration in order to not disconnect on BYE but rather
                 * when the last subscription terminates.
                 */
                try
                {
                    removeSubscription =
                        DialogUtils.addSubscription(dialog, referRequest);
                }
                catch (SipException ex)
                {
                    logger.error("Failed to make the REFER request"
                                + "keep the dialog alive after BYE:\n"
                                + referRequest, ex);
                }

                // NOTIFY Trying
                try
                {
                    sendReferNotifyRequest(dialog,
                        SubscriptionStateHeader.ACTIVE, null,
                        "SIP/2.0 100 Trying", sipProvider);
                }
                catch (OperationFailedException ex)
                {
                    /*
                     * TODO Determine whether the failure to send the Trying
                     * refer NOTIFY should prevent the sending of the
                     * session-terminating refer NOTIFY.
                     */
                }
            }
        }

        /*
         * Regardless of whether the Accepted, NOTIFY, etc. succeeded, try to
         * transfer the call because it's the most important goal.
         */
        Call referToCall;
        try
        {
            referToCall = createOutgoingCall(referToAddress, referRequest);
        }
        catch (OperationFailedException ex)
        {
            referToCall = null;

            logger.error("Failed to create outgoing call to " + referToAddress,
                ex);
        }

        /*
         * Start monitoring the call in order to discover when the
         * subscription-terminating NOTIFY with the final result of the REFER is
         * to be sent.
         */
        final Call referToCallListenerSource = referToCall;
        final boolean sendNotifyRequest = (accepted != null);
        final Object subscription = (removeSubscription ? referRequest : null);
        CallChangeListener referToCallListener = new CallChangeAdapter()
        {

            /**
             * The indicator which determines whether the job of this listener
             * has been done i.e. whether a single subscription-terminating
             * NOTIFY with the final result of the REFER has been sent.
             */
            private boolean done;

            public synchronized void callStateChanged(CallChangeEvent evt)
            {
                if (!done
                    && referToCallStateChanged(referToCallListenerSource,
                        sendNotifyRequest, dialog, sipProvider, subscription))
                {
                    done = true;
                    if (referToCallListenerSource != null)
                    {
                        referToCallListenerSource
                            .removeCallChangeListener(this);
                    }
                }
            }
        };
        if (referToCall != null)
        {
            referToCall.addCallChangeListener(referToCallListener);
        }
        referToCallListener.callStateChanged(null);
    }

    /**
     * Processes a specific <tt>Request.NOTIFY</tt> request for the purposes
     * of telephony.
     *
     * @param serverTransaction the <tt>ServerTransaction</tt> containing
     *            the <tt>Request.NOTIFY</tt> request
     * @param notifyRequest the <tt>Request.NOTIFY</tt> request to be
     *            processed
     */
    private boolean processNotify(ServerTransaction serverTransaction,
        Request notifyRequest)
    {

        /*
         * We're only handling NOTIFY as part of call transfer (i.e. refer)
         * right now.
         */
        EventHeader eventHeader =
            (EventHeader) notifyRequest.getHeader(EventHeader.NAME);
        if ((eventHeader == null)
            || !"refer".equals(eventHeader.getEventType()))
        {
            return false;
        }

        SubscriptionStateHeader ssHeader =
            (SubscriptionStateHeader) notifyRequest
                .getHeader(SubscriptionStateHeader.NAME);
        if (ssHeader == null)
        {
            logger.error("NOTIFY of refer event type"
                        + "with no Subscription-State header.");

            return false;
        }

        Dialog dialog = serverTransaction.getDialog();
        CallPeerSipImpl peer
            = activeCallsRepository.findCallPeer(dialog);

        if (peer == null)
        {
            logger.debug("Received a stray refer NOTIFY request.");
            return false;
        }

        // OK
        Response ok;
        try
        {
            ok = createOKResponse(notifyRequest, dialog);
        }
        catch (ParseException ex)
        {
            String message =
                "Failed to create OK response to refer NOTIFY request.";

            logger.error(message, ex);
            peer.setState(CallPeerState.DISCONNECTED, message);
            return false;
        }
        try
        {
            serverTransaction.sendResponse(ok);
        }
        catch (Exception ex)
        {
            String message =
                "Failed to send OK response to refer NOTIFY request.";

            logger.error(message, ex);
            peer.setState(CallPeerState.DISCONNECTED, message);
            return false;
        }

        if (SubscriptionStateHeader.TERMINATED.equals(ssHeader.getState())
            && !DialogUtils
                .removeSubscriptionThenIsDialogAlive(dialog, "refer"))
        {
            peer.setState(CallPeerState.DISCONNECTED);
        }

        if (!CallPeerState.DISCONNECTED.equals(peer.getState())
            && !DialogUtils.isByeProcessed(dialog))
        {
            boolean dialogIsAlive;
            try
            {
                dialogIsAlive = sayBye(peer);
            }
            catch (OperationFailedException ex)
            {
                logger.error(
                    "Failed to send BYE in response to refer NOTIFY request.",
                    ex);
                dialogIsAlive = false;
            }
            if (!dialogIsAlive)
            {
                peer.setState(CallPeerState.DISCONNECTED);
            }
        }

        return true;
    }

    /**
     * Tracks the state changes of a specific <tt>Call</tt> and sends a
     * session-terminating NOTIFY request to the <tt>Dialog</tt> which
     * referred to the call in question as soon as the outcome of the refer is
     * determined.
     *
     * @param referToCall the <tt>Call</tt> to track and send a NOTIFY
     *            request for
     * @param sendNotifyRequest <tt>true</tt> if a session-terminating NOTIFY
     *            request should be sent to the <tt>Dialog</tt> which
     *            referred to <tt>referToCall</tt>; <tt>false</tt> to send
     *            no such NOTIFY request
     * @param dialog the <tt>Dialog</tt> which initiated the specified call
     *            as part of processing a REFER request
     * @param sipProvider the <tt>SipProvider</tt> to send the NOTIFY
     *            request through
     * @param subscription the subscription to be terminated when the NOTIFY
     *            request is sent
     * @return <tt>true</tt> if a session-terminating NOTIFY request was sent
     *         and the state of <tt>referToCall</tt> should no longer be
     *         tracked; <tt>false</tt> if it's too early to send a
     *         session-terminating NOTIFY request and the tracking of the state
     *         of <tt>referToCall</tt> should continue
     */
    private boolean referToCallStateChanged(Call referToCall,
        boolean sendNotifyRequest, Dialog dialog, SipProvider sipProvider,
        Object subscription)
    {
        CallState referToCallState =
            (referToCall == null) ? null : referToCall.getCallState();
        if (CallState.CALL_INITIALIZATION.equals(referToCallState))
        {
            return false;
        }

        /*
         * NOTIFY OK/Declined
         *
         * It doesn't sound like sending NOTIFY Service Unavailable is
         * appropriate because the REFER request has (presumably) already been
         * accepted.
         */
        if (sendNotifyRequest)
        {
            String referStatus =
                CallState.CALL_IN_PROGRESS.equals(referToCallState)
                    ? "SIP/2.0 200 OK"
                    : "SIP/2.0 603 Declined";
            try
            {
                sendReferNotifyRequest(dialog,
                    SubscriptionStateHeader.TERMINATED,
                    SubscriptionStateHeader.NO_RESOURCE, referStatus,
                    sipProvider);
            }
            catch (OperationFailedException ex)
            {
                // The exception has already been logged.
            }
        }

        /*
         * Whatever the status of the REFER is, the subscription created by it
         * is terminated with the final NOTIFY.
         */
        if (!DialogUtils.removeSubscriptionThenIsDialogAlive(dialog,
            subscription))
        {
            CallPeerSipImpl callPeer =
                activeCallsRepository.findCallPeer(dialog);
            if (callPeer != null)
            {
                callPeer.setState(CallPeerState.DISCONNECTED);
            }
        }
        return true;
    }

    /**
     * Sends a <tt>Request.NOTIFY</tt> request in a specific
     * <tt>Dialog</tt> as part of the communication associated with an
     * earlier-received <tt>Request.REFER</tt> request. The sent NOTIFY has
     * a specific <tt>Subscription-State</tt> header and reason, carries a
     * specific body content and is sent through a specific
     * <tt>SipProvider</tt>.
     *
     * @param dialog the <tt>Dialog</tt> to send the NOTIFY request in
     * @param subscriptionState the <tt>Subscription-State</tt> header to be
     *            sent with the NOTIFY request
     * @param reasonCode the reason for the specified
     *            <tt>subscriptionState</tt> if any; <tt>null</tt> otherwise
     * @param content the content to be carried in the body of the sent NOTIFY
     *            request
     * @param sipProvider the <tt>SipProvider</tt> to send the NOTIFY
     *            request through
     * @throws OperationFailedException
     */
    private void sendReferNotifyRequest(Dialog dialog,
        String subscriptionState, String reasonCode, Object content,
        SipProvider sipProvider) throws OperationFailedException
    {
        Request notify = messageFactory.createRequest(dialog, Request.NOTIFY);
        HeaderFactory headerFactory = protocolProvider.getHeaderFactory();

        // Populate the request.
        String eventType = "refer";
        try
        {
            notify.setHeader(headerFactory.createEventHeader(eventType));
        }
        catch (ParseException ex)
        {
            ProtocolProviderServiceSipImpl.throwOperationFailedException(
                "Failed to create " + eventType + " Event header.",
                OperationFailedException.INTERNAL_ERROR, ex, logger);
        }

        SubscriptionStateHeader ssHeader = null;
        try
        {
            ssHeader =  headerFactory
                .createSubscriptionStateHeader(subscriptionState);
            if (reasonCode != null)
                ssHeader.setReasonCode(reasonCode);
        }
        catch (ParseException ex)
        {
            ProtocolProviderServiceSipImpl.throwOperationFailedException(
                "Failed to create " + subscriptionState
                + " Subscription-State header.",
                OperationFailedException.INTERNAL_ERROR, ex, logger);
        }
        notify.setHeader(ssHeader);

        ContentTypeHeader ctHeader = null;
        try
        {
            ctHeader = headerFactory
                .createContentTypeHeader("message", "sipfrag");
        }
        catch (ParseException ex)
        {
            ProtocolProviderServiceSipImpl.throwOperationFailedException(
                "Failed to create Content-Type header.",
                OperationFailedException.INTERNAL_ERROR, ex, logger);
        }
        try
        {
            notify.setContent(content, ctHeader);
        }
        catch (ParseException ex)
        {
            ProtocolProviderServiceSipImpl.throwOperationFailedException(
                "Failed to set NOTIFY body/content.",
                OperationFailedException.INTERNAL_ERROR, ex, logger);
        }

        sendRequest(sipProvider, notify, dialog);
    }

    /**
     * Indicates a user request to end a call with the specified call
     * peer. Depending on the state of the call the method would send a
     * CANCEL, BYE, or BUSY_HERE and set the new state to DISCONNECTED.
     *
     * @param peer the peer that we'd like to hang up on.
     * @throws ClassCastException if peer is not an instance of this
     *             CallPeerSipImpl.
     * @throws OperationFailedException if we fail to terminate the call.
     */
    public synchronized void hangupCallPeer(CallPeer peer)
        throws ClassCastException,
        OperationFailedException
    {
        // do nothing if the call is already ended
        if (peer.getState().equals(CallPeerState.DISCONNECTED)
            || peer.getState().equals(CallPeerState.FAILED))
        {
            logger.debug("Ignoring a request to hangup a call peer "
                + "that is already DISCONNECTED");
            return;
        }

        CallPeerSipImpl callPeer =
            (CallPeerSipImpl) peer;

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
        // For FAILE and BUSY we only need to update CALL_STATUS
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
    } // end call

    /**
     * Sends an Internal Error response inside <tt>serverTransaction</tt>.
     *
     * @param serverTransaction the transaction that we'd like to send the
     * response in.
     *
     * @throws OperationFailedException if we failed constructing or sending a
     * SIP Message.
     */
    public void sayInternalError(ServerTransaction serverTransaction)
        throws OperationFailedException
    {
        sayError(serverTransaction, Response.SERVER_INTERNAL_ERROR);
    }

    /**
     * Send an error response with the <tt>errorCode</tt> code using
     * <tt>serverTransaction</tt>.
     *
     * @param serverTransaction the transaction that we'd like to send an error
     * response in.
     * @param errorCode the code that the response should have.
     *
     * @throws OperationFailedException if we failed constructing or sending a
     * SIP Message.
     */
    public void sayError(ServerTransaction serverTransaction, int errorCode)
        throws OperationFailedException
    {
        Request request = serverTransaction.getRequest();
        Response errorResponse = null;
        try
        {
            errorResponse = protocolProvider.getMessageFactory()
                .createResponse(errorCode, request);

            //we used to be adding a To tag here and we shouldn't. 3261 says:
            //"Dialogs are created through [...] non-failure responses". and
            //we are using this method for failure responses only.

        }
        catch (ParseException ex)
        {
            ProtocolProviderServiceSipImpl.throwOperationFailedException(
                "Failed to construct an OK response to an INVITE request",
                OperationFailedException.INTERNAL_ERROR, ex, logger);
        }

         ContactHeader contactHeader = protocolProvider
                .getContactHeaderForResponse(request);
         errorResponse.setHeader(contactHeader);

        try
        {
            serverTransaction.sendResponse(errorResponse);
            if (logger.isDebugEnabled())
                logger.debug("sent response: " + errorResponse);
        }
        catch (Exception ex)
        {
            ProtocolProviderServiceSipImpl.throwOperationFailedException(
                "Failed to send an OK response to an INVITE request",
                OperationFailedException.INTERNAL_ERROR, ex, logger);
        }
    }

    /**
     * Send an error response with the <tt>errorCode</tt> code using
     * <tt>serverTransaction</tt> and do not surface exceptions. The method
     * is useful when we are sending the error response in a stack initiated
     * operation and don't have the possibility to escalate potential
     * exceptions, so we can only log them.
     *
     * @param serverTransaction the transaction that we'd like to send an error
     * response in.
     * @param errorCode the code that the response should have.
     */
    public void sayErrorSilently(ServerTransaction serverTransaction,
                                 int               errorCode)
    {
        try
        {
            sayError(serverTransaction, errorCode);
        }
        catch (OperationFailedException exc)
        {
            logger.debug("Failed to send an error " + errorCode + " response",
                            exc);
        }
    }

    /**
     * Sends a BYE request to <tt>callPeer</tt>.
     *
     * @param callPeer the call peer that we need to say bye to.
     * @return <tt>true</tt> if the <tt>Dialog</tt> should be considered
     *         alive after sending the BYE request (e.g. when there're still
     *         active subscriptions); <tt>false</tt>, otherwise
     * @throws OperationFailedException if we failed constructing or sending a
     *             SIP Message.
     */
    private boolean sayBye(CallPeerSipImpl callPeer)
        throws OperationFailedException
    {
        Dialog dialog = callPeer.getDialog();

        Request bye = messageFactory.createRequest(dialog, Request.BYE);

        sendRequest(callPeer.getJainSipProvider(), bye, dialog);

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
    } // bye

    /**
     * Sends a Cancel request to <tt>callPeer</tt>.
     *
     * @param callPeer the call peer that we need to cancel.
     *
     * @throws OperationFailedException we failed to construct or send the
     *             CANCEL request.
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
    } // cancel

    /**
     * Sends a BUSY_HERE response to <tt>callPeer</tt>.
     *
     * @param callPeer the call peer that we need to send busy
     *            tone to.
     * @throws OperationFailedException if we fail to create or send the
     *             response
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
    } // busy here

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
    public synchronized void answerCallPeer(CallPeer peer)
        throws OperationFailedException
    {
        CallPeerSipImpl callPeer =
            (CallPeerSipImpl) peer;
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

        CallPeerState peerState = peer.getState();

        if (peerState.equals(CallPeerState.CONNECTED)
            || CallPeerState.isOnHold(peerState))
        {
            logger.info("Ignoring user request to answer a CallPeer "
                + "that is already connected. CP:" + peer);
            return;
        }

        ServerTransaction serverTransaction = (ServerTransaction) transaction;
        Response ok = null;
        try
        {
            ok =
                createOKResponse(callPeer.getFirstTransaction()
                    .getRequest(), dialog);
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
            contentTypeHeader =
                protocolProvider.getHeaderFactory().createContentTypeHeader(
                    "application", "sdp");
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
            this.sayError(serverTransaction, Response.NOT_ACCEPTABLE_HERE);
        }
        catch (ParseException ex)
        {
            //log, the error and tell the remote party. do not throw an
            //exception as it would go to the stack and there's nothing it could
            //do with it.
            logger.error(
                "Failed to parse sdp data while creating invite request!",
                ex);
            this.sayError(serverTransaction, Response.NOT_ACCEPTABLE_HERE);
        }

        ContactHeader contactHeader = protocolProvider.getContactHeader(
                        dialog.getRemoteTarget());
        ok.setHeader(contactHeader);
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
    } // answer call

    /**
     * Creates a new {@link Response#OK} response to a specific {@link Request}
     * which is to be sent as part of a specific {@link Dialog}.
     *
     * @param request the <tt>Request</tt> to create the OK response for
     * @param containingDialog the <tt>Dialog</tt> to send the response in
     * @return a new <tt>Response.OK</tt> response to the specified
     *         <tt>request</tt> to be sent as part of the specified
     *         <tt>containingDialog</tt>
     * @throws ParseException
     */
    private Response createOKResponse(Request request, Dialog containingDialog)
        throws ParseException
    {
        Response ok = messageFactory.createResponse(Response.OK, request);
        return ok;
    }

    /**
     * Creates a new call and call peer associated with
     * <tt>containingTransaction</tt>
     *
     * @param containingTransaction the transaction that created the call.
     * @param sourceProvider the provider that the containingTransaction belongs
     *            to.
     *
     * @return a new instance of a <tt>CallPeerSipImpl</tt> corresponding
     *         to the <tt>containingTransaction</tt>.
     */
    private CallPeerSipImpl createCallPeerFor(
        Transaction containingTransaction, SipProvider sourceProvider)
    {
        CallSipImpl call = new CallSipImpl(protocolProvider);
        CallPeerSipImpl callPeer =
            new CallPeerSipImpl(
                containingTransaction.getDialog().getRemoteParty(),
                call);
        boolean incomingCall =
            (containingTransaction instanceof ServerTransaction);

        callPeer.setState(
             incomingCall ?
                 CallPeerState.INCOMING_CALL :
                 CallPeerState.INITIATING_CALL);

        callPeer.setDialog(containingTransaction.getDialog());
        callPeer.setFirstTransaction(containingTransaction);
        callPeer.setJainSipProvider(sourceProvider);

        activeCallsRepository.addCall(call);

        // notify everyone
        fireCallEvent(
            incomingCall ? CallEvent.CALL_RECEIVED : CallEvent.CALL_INITIATED,
            call);

        return callPeer;
    }

    /**
     * Returns a string representation of this OperationSetBasicTelephony
     * instance including information that would permit to distinguish it among
     * other instances when reading a log file.
     *
     * @return a string representation of this operation set.
     */
    public String toString()
    {
        return getClass().getSimpleName() + "-[dn="
            + protocolProvider.getOurDisplayName() + " addr=["
            + protocolProvider.getRegistrarConnection().getAddressOfRecord()
            + "]";
    }

    /**
     * Closes all active calls. And releases resources.
     */
    public synchronized void shutdown()
    {
        logger.trace("Ending all active calls.");
        Iterator<CallSipImpl> activeCalls
            = this.activeCallsRepository.getActiveCalls();

        // go through all active calls.
        while (activeCalls.hasNext())
        {
            CallSipImpl call = activeCalls.next();

            Iterator<CallPeer> callPeers  = call.getCallPeers();

            // go through all call peers and say bye to every one.
            while (callPeers.hasNext())
            {
                CallPeer peer = callPeers.next();
                try
                {
                    this.hangupCallPeer(peer);
                }
                catch (Exception ex)
                {
                    logger.warn("Failed to properly hangup particpant "
                        + peer, ex);
                }
            }
        }
    }

    /**
     * Sets the mute state of the audio stream being sent to a specific
     * <tt>CallPeer</tt>.
     * <p>
     * The implementation sends silence through the audio stream.
     * </p>
     *
     * @param peer the <tt>CallPeer</tt> who receives the audio
     *            stream to have its mute state set
     * @param mute <tt>true</tt> to mute the audio stream being sent to
     *            <tt>peer</tt>; otherwise, <tt>false</tt>
     */
    public void setMute(CallPeer peer, boolean mute)
    {
        CallPeerSipImpl sipPeer = (CallPeerSipImpl) peer;

        ((CallSipImpl) sipPeer.getCall())
            .getMediaCallSession().setMute(mute);

        sipPeer.setMute(mute);
    }

    /**
     * Returns <tt>true</tt> to indicate that the call associated with the
     * given peer is secured, otherwise returns <tt>false</tt>.
     *
     * @return <tt>true</tt> to indicate that the call associated with the
     * given peer is secured, otherwise returns <tt>false</tt>.
     */
    public boolean isSecure(CallPeer peer)
    {
        CallSession cs
            = ((CallSipImpl) peer.getCall()).getMediaCallSession();

        return (cs != null) && cs.getSecureCommunicationStatus();
    }

    /**
     * Sets the SAS verification property value for the given call peer.
     *
     * @param peer the call peer, for which we set the
     * @param isVerified indicates whether the SAS string is verified or not
     * for the given peer.
     */
    public boolean setSasVerified(  CallPeer peer,
                                    boolean isVerified)
    {
        CallSession cs
            = ((CallSipImpl) peer.getCall()).getMediaCallSession();

        return (cs != null) && cs.setZrtpSASVerification(isVerified);
    }


    /**
     * Transfers (in the sense of call transfer) a specific
     * <tt>CallPeer</tt> to a specific callee address.
     *
     * @param peer the <tt>CallPeer</tt> to be transfered to
     *            the specified callee address
     * @param target the <tt>Address</tt> the callee to transfer
     *            <tt>peer</tt> to
     * @throws OperationFailedException
     */
    private void transfer(CallPeer peer, Address target)
        throws OperationFailedException
    {
        CallPeerSipImpl sipPeer =
            (CallPeerSipImpl) peer;
        Dialog dialog = sipPeer.getDialog();
        Request refer = messageFactory.createRequest(dialog, Request.REFER);
        HeaderFactory headerFactory = protocolProvider.getHeaderFactory();

        // Refer-To is required.
        refer.setHeader(headerFactory.createReferToHeader(target));

        /*
         * Referred-By is optional but only to the extent that the refer target
         * may choose to require a valid Referred-By token.
         */
        refer.addHeader(
            ((HeaderFactoryImpl) headerFactory)
                .createReferredByHeader(sipPeer.getJainSipAddress()));

        sendRequest(sipPeer.getJainSipProvider(), refer, dialog);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * net.java.sip.communicator.service.protocol.OperationSetAdvancedTelephony
     * #transfer(net.java.sip.communicator.service.protocol.CallPeer,
     * net.java.sip.communicator.service.protocol.CallPeer)
     */
    public void transfer(CallPeer peer, CallPeer target)
        throws OperationFailedException
    {
        Address targetAddress = parseAddressString(target.getAddress());

        Dialog targetDialog = ((CallPeerSipImpl) target).getDialog();
        String remoteTag = targetDialog.getRemoteTag();
        String localTag = targetDialog.getLocalTag();
        Replaces replacesHeader = null;
        SipURI sipURI = (SipURI) targetAddress.getURI();

        try
        {
            replacesHeader = (Replaces)
                ((HeaderFactoryImpl) protocolProvider.getHeaderFactory())
                    .createReplacesHeader(
                        targetDialog.getCallId().getCallId(),
                        (remoteTag == null) ? "0" : remoteTag,
                        (localTag == null) ? "0" : localTag);
        }
        catch (ParseException ex)
        {
            ProtocolProviderServiceSipImpl.throwOperationFailedException(
                "Failed to create Replaces header for target dialog "
                    + targetDialog,
                OperationFailedException.ILLEGAL_ARGUMENT, ex, logger);
        }
        try
        {
            sipURI.setHeader(ReplacesHeader.NAME, replacesHeader.encodeBody());
        }
        catch (ParseException ex)
        {
            ProtocolProviderServiceSipImpl.throwOperationFailedException(
                "Failed to set Replaces header " + replacesHeader
                + " to SipURI " + sipURI,
                OperationFailedException.INTERNAL_ERROR, ex, logger);
        }

        putOnHold(peer);
        putOnHold(target);

        transfer(peer, targetAddress);
    }

    /**
     * Transfers (in the sense of call transfer) a specific
     * <tt>CallPeer</tt> to a specific callee address which already
     * participates in an active <tt>Call</tt>.
     * <p>
     * The method is suitable for providing the implementation of attended call
     * transfer (though no such requirement is imposed).
     * </p>
     *
     * @param peer the <tt>CallPeer</tt> to be transfered to
     *            the specified callee address
     * @param target the address in the form of <tt>CallPeer</tt> of
     *            the callee to transfer <tt>peer</tt> to
     * @throws OperationFailedException
     */
    public void transfer(CallPeer peer, String target)
        throws OperationFailedException
    {
        transfer(peer, parseAddressString(target));
    }

    /**
     * Parses a specific string into a JAIN SIP <tt>Address</tt>.
     *
     * @param addressString the <tt>String</tt> to be parsed into an
     *            <tt>Address</tt>
     * @return the <tt>Address</tt> representation of
     *         <tt>addressString</tt>
     * @throws OperationFailedException if <tt>addressString</tt> is not
     *             properly formatted
     */
    private Address parseAddressString(String addressString)
        throws OperationFailedException
    {
        Address address = null;
        try
        {
            address = protocolProvider.parseAddressString(addressString);
        }
        catch (ParseException ex)
        {
            ProtocolProviderServiceSipImpl.throwOperationFailedException(
                 "Failed to parse address string " + addressString,
                 OperationFailedException.ILLEGAL_ARGUMENT, ex, logger);
        }
        return address;
    }

    /**
     * Verifies that our protocol provider is properly registered and throws
     * an <tt>OperationFailedException</tt> if that's not the case.
     *
     * @throws OperationFailedException if the protocol provider that created us
     * is not registered.
     */
    private void assertRegistered()
        throws OperationFailedException
    {
        if(!protocolProvider.isRegistered())
        {
            throw new OperationFailedException(
                "The protocol provider should be registered "
                +"before placing an outgoing call.",
                OperationFailedException.PROVIDER_NOT_REGISTERED);
        }
    }
}

