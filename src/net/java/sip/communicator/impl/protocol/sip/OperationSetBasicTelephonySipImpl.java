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
 */
public class OperationSetBasicTelephonySipImpl
    extends AbstractOperationSetBasicTelephony
    implements OperationSetAdvancedTelephony,
               SipListener
{
    private static final Logger logger =
        Logger.getLogger(OperationSetBasicTelephonySipImpl.class);

    /**
     * A reference to the <tt>ProtocolProviderServiceSipImpl</tt> instance that
     * created us.
     */
    private final ProtocolProviderServiceSipImpl protocolProvider;

    /**
     * Contains references for all currently active (non ended) calls.
     */
    private ActiveCallsRepository activeCallsRepository =
        new ActiveCallsRepository(this);

    /**
     * The name of the boolean property that the user could use to specify
     * whether incoming calls should be rejected if the user name in the
     * destination (to) address does not match the one that we have in our sip
     * address.
     */
    private static final String FAIL_CALLS_ON_DEST_USER_MISMATCH =
        "net.java.sip.communicator.impl.protocol.sip."
            + "FAIL_CALLS_ON_DEST_USER_MISMATCH";

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

        protocolProvider.registerMethodProcessor(Request.INVITE, this);
        protocolProvider.registerMethodProcessor(Request.CANCEL, this);
        protocolProvider.registerMethodProcessor(Request.ACK, this);
        protocolProvider.registerMethodProcessor(Request.BYE, this);
        protocolProvider.registerMethodProcessor(Request.REFER, this);
        protocolProvider.registerMethodProcessor(Request.NOTIFY, this);

        protocolProvider.registerEvent("refer");
    }

    /**
     * Create a new call and invite the specified CallParticipant to it.
     * 
     * @param callee the sip address of the callee that we should invite to a
     *            new call.
     * @return CallParticipant the CallParticipant that will represented by the
     *         specified uri. All following state change events will be
     *         delivered through that call participant. The Call that this
     *         participant is a member of could be retrieved from the
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
        Address toAddress = protocolProvider.parseAddressStr(callee);

        return createOutgoingCall(toAddress);
    }

    /**
     * Create a new call and invite the specified CallParticipant to it.
     * 
     * @param callee the address of the callee that we should invite to a new
     *            call.
     * @return CallParticipant the CallParticipant that will represented by the
     *         specified uri. All following state change events will be
     *         delivered through that call participant. The Call that this
     *         participant is a member of could be retrieved from the
     *         CallParticipatn instance with the use of the corresponding
     *         method.
     * @throws OperationFailedException with the corresponding code if we fail
     *             to create the call.
     */
    public Call createCall(Contact callee) throws OperationFailedException
    {
        Address toAddress = null;

        try
        {
            toAddress = protocolProvider.parseAddressStr(callee.getAddress());
        }
        catch (ParseException ex)
        {
            // couldn't happen
            logger.error(ex.getMessage(), ex);
            throw new IllegalArgumentException(ex.getMessage());
        }

        return createOutgoingCall(toAddress);
    }

    /**
     * Init and establish the specified call.
     * 
     * @param calleeAddress the address of the callee that we'd like to connect
     *            with.
     * 
     * @return CallParticipant the CallParticipant that will represented by the
     *         specified uri. All following state change events will be
     *         delivered through that call participant. The Call that this
     *         participant is a member of could be retrieved from the
     *         CallParticipatn instance with the use of the corresponding
     *         method.
     * 
     * @throws OperationFailedException with the corresponding code if we fail
     *             to create the call.
     */
    private synchronized CallSipImpl createOutgoingCall(Address calleeAddress)
        throws OperationFailedException
    {
        // create the invite request
        Request invite = createInviteRequest(calleeAddress);

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
            logger.error(
                "Failed to create a content type header for the INVITE "
                    + "request", ex);
            throw new OperationFailedException(
                "Failed to create a content type header for the INVITE "
                    + "request", OperationFailedException.INTERNAL_ERROR, ex);
        }

        // check whether there's a cached authorization header for this
        // call id and if so - attach it to the request.
        // add authorization header
        CallIdHeader call = (CallIdHeader) invite.getHeader(CallIdHeader.NAME);
        String callid = call.getCallId();

        AuthorizationHeader authorization =
            protocolProvider.getSipSecurityManager()
                .getCachedAuthorizationHeader(callid);

        if (authorization != null)
            invite.addHeader(authorization);

        // Transaction
        ClientTransaction inviteTransaction;
        SipProvider jainSipProvider =
            protocolProvider.getDefaultJainSipProvider();
        try
        {
            inviteTransaction = jainSipProvider.getNewClientTransaction(invite);
        }
        catch (TransactionUnavailableException ex)
        {
            logger.error("Failed to create inviteTransaction.\n"
                + "This is most probably a network connection error.", ex);
            throw new OperationFailedException(
                "Failed to create inviteTransaction.\n"
                    + "This is most probably a network connection error.",
                OperationFailedException.INTERNAL_ERROR, ex);
        }

        // create the call participant
        CallParticipantSipImpl callParticipant =
            createCallParticipantFor(inviteTransaction, jainSipProvider);

        // invite content
        try
        {
            CallSession callSession =
                SipActivator.getMediaService().createCallSession(
                    callParticipant.getCall());
            ((CallSipImpl) callParticipant.getCall())
                .setMediaCallSession(callSession);

            // if possible try to indicate the address of the callee so
            // that the media service can choose the most proper local
            // address to advertise.
            javax.sip.address.URI calleeURI = calleeAddress.getURI();
            InetAddress intendedDestination = null;
            if (calleeURI.isSipURI())
            {
                String host = ((SipURI) calleeURI).getHost();

                try
                {
                    intendedDestination = InetAddress.getByName(host);
                    invite
                        .setContent(callSession
                            .createSdpOffer(intendedDestination),
                            contentTypeHeader);
                }
                catch (UnknownHostException ex)
                {
                    logger.warn("Failed to obtain an InetAddress for " + host,
                        ex);
                }
            }
        }
        catch (ParseException ex)
        {
            logger.error(
                "Failed to parse sdp data while creating invite request!", ex);
            throw new OperationFailedException(
                "Failed to parse sdp data while creating invite request!",
                OperationFailedException.INTERNAL_ERROR, ex);
        }
        catch (MediaException ex)
        {
            logger.error("Could not access media devices!", ex);
            throw new OperationFailedException(
                "Could not access media devices!",
                OperationFailedException.INTERNAL_ERROR, ex);
        }

        try
        {
            inviteTransaction.sendRequest();
            if (logger.isDebugEnabled())
                logger.debug("sent request: " + invite);
        }
        catch (SipException ex)
        {
            logger.error("An error occurred while sending invite request", ex);
            throw new OperationFailedException(
                "An error occurred while sending invite request",
                OperationFailedException.NETWORK_FAILURE, ex);
        }

        return (CallSipImpl) callParticipant.getCall();
    }

    /**
     * Returns an iterator over all currently active calls.
     * 
     * @return an iterator over all currently active calls.
     */
    public Iterator getActiveCalls()
    {
        return activeCallsRepository.getActiveCalls();
    }

    /**
     * Resumes communication with a call participant previously put on hold.
     * 
     * @param participant the call participant to put on hold.
     * @throws OperationFailedException
     */
    public synchronized void putOffHold(CallParticipant participant)
        throws OperationFailedException
    {
        putOnHold(participant, false);
    }

    /**
     * Puts the specified CallParticipant "on hold".
     * 
     * @param participant the participant that we'd like to put on hold.
     * @throws OperationFailedException
     */
    public synchronized void putOnHold(CallParticipant participant)
        throws OperationFailedException
    {
        putOnHold(participant, true);
    }

    /**
     * Puts the specified <tt>CallParticipant</tt> on or off hold.
     * 
     * @param participant the <tt>CallParticipant</tt> to be put on or off hold
     * @param on <tt>true</tt> to have the specified <tt>CallParticipant</tt>
     *            put on hold; <tt>false</tt>, otherwise
     * @throws OperationFailedException
     */
    private void putOnHold(CallParticipant participant, boolean on)
        throws OperationFailedException
    {
        CallSession callSession =
            ((CallSipImpl) participant.getCall()).getMediaCallSession();
        CallParticipantSipImpl sipParticipant =
            (CallParticipantSipImpl) participant;

        try
        {
            sendInviteRequest(sipParticipant, callSession
                .createSdpDescriptionForHold(
                    sipParticipant.getSdpDescription(), on));
        }
        catch (MediaException ex)
        {
            throwOperationFailedException(
                "Failed to create SDP offer to hold.",
                OperationFailedException.INTERNAL_ERROR, ex);
        }

        /*
         * Putting on hold isn't a negotiation (i.e. the issuing side takes the
         * decision and executes it) so we're muting now regardless of the
         * desire of the participant to accept the offer.
         */
        callSession.putOnHold(on, true);

        CallParticipantState state = sipParticipant.getState();
        if (CallParticipantState.ON_HOLD_LOCALLY.equals(state))
        {
            if (!on)
                sipParticipant.setState(CallParticipantState.CONNECTED);
        }
        else if (CallParticipantState.ON_HOLD_MUTUALLY.equals(state))
        {
            if (!on)
                sipParticipant.setState(CallParticipantState.ON_HOLD_REMOTELY);
        }
        else if (CallParticipantState.ON_HOLD_REMOTELY.equals(state))
        {
            if (on)
                sipParticipant.setState(CallParticipantState.ON_HOLD_MUTUALLY);
        }
        else if (on)
        {
            sipParticipant.setState(CallParticipantState.ON_HOLD_LOCALLY);
        }
    }

    /**
     * Sends an invite request with a specific SDP offer (description) within
     * the current <tt>Dialog</tt> with a specific call participant.
     * 
     * @param sipParticipant the SIP-specific call participant to send the
     *            invite to within the current <tt>Dialog</tt>
     * @param sdpOffer the description of the SDP offer to be made to the
     *            specified call participant with the sent invite
     * @throws OperationFailedException
     */
    private void sendInviteRequest(CallParticipantSipImpl sipParticipant,
        String sdpOffer) throws OperationFailedException
    {
        Dialog dialog = sipParticipant.getDialog();
        Request invite = createRequest(dialog, Request.INVITE);

        try
        {
            invite.setContent(sdpOffer, protocolProvider.getHeaderFactory()
                .createContentTypeHeader("application", "sdp"));
        }
        catch (ParseException ex)
        {
            throwOperationFailedException(
                "Failed to parse SDP offer for the new invite.",
                OperationFailedException.INTERNAL_ERROR, ex);
        }

        sendRequest(sipParticipant.getJainSipProvider(), invite, dialog);
    }

    /**
     * Sends a specific <code>Request</code> through a given
     * <code>SipProvider</code> as part of the conversation associated with a
     * specific <code>Dialog</code>.
     * 
     * @param sipProvider the <code>SipProvider</code> to send the specified
     *            request through
     * @param request the <code>Request</code> to send through
     *            <code>sipProvider</code>
     * @param dialog the <code>Dialog</code> as part of which the specified
     *            <code>request</code> is to be sent
     * @throws OperationFailedException
     */
    private void sendRequest(SipProvider sipProvider, Request request,
        Dialog dialog) throws OperationFailedException
    {
        ClientTransaction clientTransaction = null;
        try
        {
            clientTransaction = sipProvider.getNewClientTransaction(request);
        }
        catch (TransactionUnavailableException ex)
        {
            throwOperationFailedException(
                "Failed to create a client transaction for request:\n"
                    + request, OperationFailedException.INTERNAL_ERROR, ex);
        }

        try
        {
            dialog.sendRequest(clientTransaction);
        }
        catch (SipException ex)
        {
            throwOperationFailedException(
                "Failed to send request:\n" + request,
                OperationFailedException.NETWORK_FAILURE, ex);
        }

        logger.debug("Sent request:\n" + request);
    }

    /**
     * Logs a specific message and associated <tt>Throwable</tt> cause as an
     * error using the current <tt>Logger</tt> and then throws a new
     * <tt>OperationFailedException</tt> with the message, a specific error code
     * and the cause.
     * 
     * @param message the message to be logged and then wrapped in a new
     *            <tt>OperationFailedException</tt>
     * @param errorCode the error code to be assigned to the new
     *            <tt>OperationFailedException</tt>
     * @param cause the <tt>Throwable</tt> that has caused the necessity to log
     *            an error and have a new <tt>OperationFailedException</tt>
     *            thrown
     * @throws OperationFailedException
     */
    private void throwOperationFailedException(String message, int errorCode,
        Throwable cause) throws OperationFailedException
    {
        logger.error(message, cause);
        throw new OperationFailedException(message, errorCode, cause);
    }

    /**
     * Processes a Request received on a SipProvider upon which this SipListener
     * is registered.
     * <p>
     * 
     * @param requestEvent requestEvent fired from the SipProvider to the
     *            <tt>SipListener</tt> representing a Request received from the
     *            network.
     */
    public void processRequest(RequestEvent requestEvent)
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
                    jainSipProvider.getNewServerTransaction(request);
            }
            catch (TransactionAlreadyExistsException ex)
            {
                // let's not scare the user and only log a message
                logger.error("Failed to create a new server"
                    + "transaction for an incoming request\n"
                    + "(Next message contains the request)", ex);
                return;
            }
            catch (TransactionUnavailableException ex)
            {
                // let's not scare the user and only log a message
                logger.error("Failed to create a new server"
                    + "transaction for an incoming request\n"
                    + "(Next message contains the request)", ex);
                return;
            }
        }

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
                processInvite(jainSipProvider, serverTransaction, request);
            }
            else
            {
                logger
                    .error("reINVITEs while the dialog is not confirmed are not currently supported.");
            }
        }
        // ACK
        else if (requestMethod.equals(Request.ACK))
        {
            processAck(serverTransaction, request);
        }
        // BYE
        else if (requestMethod.equals(Request.BYE))
        {
            processBye(serverTransaction, request);
        }
        // CANCEL
        else if (requestMethod.equals(Request.CANCEL))
        {
            processCancel(serverTransaction, request);
        }
        // REFER
        else if (requestMethod.equals(Request.REFER))
        {
            logger.debug("received REFER");
            processRefer(serverTransaction, request, jainSipProvider);
        }
        // NOTIFY
        else if (requestMethod.equals(Request.NOTIFY))
        {
            logger.debug("received NOTIFY");
            processNotify(serverTransaction, request);
        }
    }

    /**
     * Process an asynchronously reported TransactionTerminatedEvent.
     * 
     * @param transactionTerminatedEvent -- an event that indicates that the
     *            transaction has transitioned into the terminated state.
     */
    public void processTransactionTerminated(
        TransactionTerminatedEvent transactionTerminatedEvent)
    {
        // nothing to do here.
    }

    /**
     * Analyzes the incoming <tt>responseEvent</tt> and then forwards it to the
     * proper event handler.
     * 
     * @param responseEvent the responseEvent that we received
     *            ProtocolProviderService.
     */
    public void processResponse(ResponseEvent responseEvent)
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
        switch (responseStatusCode)
        {

        // OK
        case Response.OK:
            if (method.equals(Request.INVITE))
            {
                processInviteOK(clientTransaction, response);
            }
            else if (method.equals(Request.BYE))
            {
                // ignore
            }
            break;

        // Ringing
        case Response.RINGING:
            processRinging(clientTransaction, response);
            break;

        // Session Progress
        case Response.SESSION_PROGRESS:
            processSessionProgress(clientTransaction, response);
            break;

        // Trying
        case Response.TRYING:
            processTrying(clientTransaction, response);
            break;

        // Busy here.
        case Response.BUSY_HERE:
            processBusyHere(clientTransaction, response);
            break;

        // Accepted
        case Response.ACCEPTED:
            if (Request.REFER.equals(method))
            {
                processReferAccepted(clientTransaction, response);
            }
            break;

        // 401 UNAUTHORIZED
        case Response.UNAUTHORIZED:
        case Response.PROXY_AUTHENTICATION_REQUIRED:
            processAuthenticationChallenge(clientTransaction, response,
                sourceProvider);
            break;

        // errors
        default:
            if ((responseStatusCode / 100 == 4)
                || (responseStatusCode / 100 == 5))
            {
                CallParticipantSipImpl callParticipant =
                    activeCallsRepository.findCallParticipant(clientTransaction
                        .getDialog());

                logger.error("Received error: " + response.getStatusCode()
                    + " " + response.getReasonPhrase());

                if (callParticipant != null)
                    callParticipant.setState(CallParticipantState.FAILED);
            }
            // ignore everything else.
            break;
        }
    }

    /**
     * Processes a specific <code>Response.ACCEPTED</code> response of an
     * earlier <code>Request.REFER</code> request.
     * 
     * @param clientTransaction the <code>ClientTransaction</code> which brought
     *            the response
     * @param accepted the <code>Response.ACCEPTED</code> response to an earlier
     *            <code>Request.REFER</code> request
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
            logger.error(
                "Failed to make Accepted REFER response keep the dialog alive after BYE:\n"
                    + accepted, ex);
        }
    }

    /**
     * Updates the call state of the corresponding call participant.
     * 
     * @param clientTransaction the transaction in which the response was
     *            received.
     * @param response the trying response.
     */
    private void processTrying(ClientTransaction clientTransaction,
        Response response)
    {
        Dialog dialog = clientTransaction.getDialog();
        // find the call participant
        CallParticipantSipImpl callParticipant =
            activeCallsRepository.findCallParticipant(dialog);

        if (callParticipant == null)
        {
            logger.debug("Received a stray trying response.");
            return;
        }

        // change status
        CallParticipantState callParticipantState = callParticipant.getState();
        if (!CallParticipantState.CONNECTED.equals(callParticipantState)
            && !CallParticipantState.isOnHold(callParticipantState))
            callParticipant.setState(CallParticipantState.CONNECTING);
    }

    /**
     * Updates the call state of the corresponding call participant. We'll also
     * try to extract any details here that might be of use for call participant
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
        // find the call participant
        CallParticipantSipImpl callParticipant =
            activeCallsRepository.findCallParticipant(dialog);

        if (callParticipant == null)
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
                callParticipant.setDisplayName(displayName);
            }
        }

        // change status.
        callParticipant.setState(CallParticipantState.ALERTING_REMOTE_SIDE);
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
        CallParticipantSipImpl callParticipant =
            activeCallsRepository.findCallParticipant(dialog);

        if (callParticipant.getState() == CallParticipantState.CONNECTING_WITH_EARLY_MEDIA)
        {
            // This can happen if we are receigin early media for a second time.
            logger.warn("Ignoring invite 183 since call participant is "
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
            // This can happen if we are receigin early media for a second time.
            logger.warn("Ignoring invite 183 since call participant is "
                + "already exchanging early media.");
            return;
        }

        // set sdp content before setting call state as that is where
        // listeners get alerted and they need the sdp
        callParticipant.setSdpDescription(new String(sessionProgress
            .getRawContent()));

        // notify the media manager of the sdp content
        CallSession callSession =
            ((CallSipImpl) callParticipant.getCall()).getMediaCallSession();

        if (callSession == null)
        {
            // unlikely to happen because it would mean we didn't send an offer
            // in the invite and we always send one.
            logger.warn("Could not find call session.");
            return;
        }

        try
        {
            callSession.processSdpAnswer(callParticipant, new String(
                sessionProgress.getRawContent()));
        }
        catch (ParseException exc)
        {
            logger.error("There was an error parsing the SDP description of "
                + callParticipant.getDisplayName() + "("
                + callParticipant.getAddress() + ")", exc);
            callParticipant.setState(CallParticipantState.FAILED,
                "There was an error parsing the SDP description of "
                    + callParticipant.getDisplayName() + "("
                    + callParticipant.getAddress() + ")");
        }
        catch (MediaException exc)
        {
            logger.error("We failed to process the SDP description of "
                + callParticipant.getDisplayName() + "("
                + callParticipant.getAddress() + ")" + ". Error was: "
                + exc.getMessage(), exc);
            callParticipant.setState(CallParticipantState.FAILED,
                "We failed to process the SDP description of "
                    + callParticipant.getDisplayName() + "("
                    + callParticipant.getAddress() + ")" + ". Error was: "
                    + exc.getMessage());
        }

        // set the call url in case there was one
        /**
         * @todo this should be done in CallSession, once we move it here.
         */
        callParticipant.setCallInfoURL(callSession.getCallInfoURL());

        // change status
        callParticipant
            .setState(CallParticipantState.CONNECTING_WITH_EARLY_MEDIA);
    }

    /**
     * Sets to CONNECTED that state of the corresponding call participant and
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
        CallParticipantSipImpl callParticipant =
            activeCallsRepository.findCallParticipant(dialog);

        if (callParticipant == null)
        {
            // In case of forwarding a call, the dialog maybe forked.
            // If the dialog is forked
            // we must check whether we have early state dialogs
            // established and we must end them, do this by replacing the dialog
            // with new one
            CallIdHeader call = (CallIdHeader) ok.getHeader(CallIdHeader.NAME);
            String callid = call.getCallId();

            Iterator activeCallsIter = activeCallsRepository.getActiveCalls();
            while (activeCallsIter.hasNext())
            {
                CallSipImpl activeCall = (CallSipImpl) activeCallsIter.next();
                Iterator callParticipantsIter =
                    activeCall.getCallParticipants();
                while (callParticipantsIter.hasNext())
                {
                    CallParticipantSipImpl cp =
                        (CallParticipantSipImpl) callParticipantsIter.next();
                    Dialog callPartDialog = cp.getDialog();
                    // check if participant in same call
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
                        callParticipant = cp;
                        cp.setDialog(dialog);
                    }
                }
            }

            if (callParticipant == null)
            {
                logger.debug("Received a stray ok response.");
                return;
            }
        }

        /*
         * Receiving an Invite OK is allowed even when the participant is
         * already connected for the purposes of call hold.
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
            callParticipant.setState(CallParticipantState.FAILED,
                "Failed to create a content type header for the ACK request");
            logger.error(
                "Failed to create a content type header for the ACK request",
                ex);
        }
        catch (InvalidArgumentException ex)
        {
            // Shouldn't happen
            callParticipant.setState(CallParticipantState.FAILED,
                "Failed ACK request, problem with the supplied cseq");
            logger.error("Failed ACK request, problem with the supplied cseq",
                ex);
        }
        catch (SipException ex)
        {
            logger.error("Failed to create ACK request!", ex);
            callParticipant.setState(CallParticipantState.FAILED);
            return;
        }

        // !!! set sdp content before setting call state as that is where
        // listeners get alerted and they need the sdp
        callParticipant.setSdpDescription(new String(ok.getRawContent()));

        // notify the media manager of the sdp content
        CallSession callSession =
            ((CallSipImpl) callParticipant.getCall()).getMediaCallSession();

        try
        {
            try
            {
                if (callSession == null)
                {
                    // non existent call session - that means we didn't send sdp
                    // in the invide and this is the offer so we need to create
                    // the answer.
                    callSession =
                        SipActivator.getMediaService().createCallSession(
                            callParticipant.getCall());
                    String sdp =
                        callSession.processSdpOffer(callParticipant,
                            callParticipant.getSdpDescription());
                    ack.setContent(sdp, contentTypeHeader);

                    // set the call url in case there was one
                    /**
                     * @todo this should be done in CallSession, once we move it
                     *       here.
                     */
                    callParticipant
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
                    logger.error("Failed to acknowledge call!", ex);
                    callParticipant.setState(CallParticipantState.FAILED);
                    return;
                }
            }
            CallParticipantState callParticipantState =
                callParticipant.getState();
            if ((callParticipantState != CallParticipantState.CONNECTED)
                && !CallParticipantState.isOnHold(callParticipantState))
            {
                callSession.processSdpAnswer(callParticipant, callParticipant
                    .getSdpDescription());
            }
            // set the call url in case there was one
            /**
             * @todo this should be done in CallSession, once we move it here.
             */
            callParticipant.setCallInfoURL(callSession.getCallInfoURL());
        }
        catch (ParseException exc)
        {
            logger.error("There was an error parsing the SDP description of "
                + callParticipant.getDisplayName() + "("
                + callParticipant.getAddress() + ")", exc);
            callParticipant.setState(CallParticipantState.FAILED,
                "There was an error parsing the SDP description of "
                    + callParticipant.getDisplayName() + "("
                    + callParticipant.getAddress() + ")");
        }
        catch (MediaException exc)
        {
            logger.error("We failed to process the SDP description of "
                + callParticipant.getDisplayName() + "("
                + callParticipant.getAddress() + ")" + ". Error was: "
                + exc.getMessage(), exc);
            callParticipant.setState(CallParticipantState.FAILED,
                "We failed to process the SDP description of "
                    + callParticipant.getDisplayName() + "("
                    + callParticipant.getAddress() + ")" + ". Error was: "
                    + exc.getMessage());
        }

        // change status
        if (!CallParticipantState.isOnHold(callParticipant.getState()))
            callParticipant.setState(CallParticipantState.CONNECTED);
    }

    /**
     * Sets corresponding state to the call participant associated with this
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
        CallParticipantSipImpl callParticipant =
            activeCallsRepository.findCallParticipant(dialog);

        if (callParticipant == null)
        {
            logger.debug("Received a stray busyHere response.");
            return;
        }

        // change status
        callParticipant.setState(CallParticipantState.BUSY);
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
        // First find the call and the call participant that this authentication
        // request concerns.
        CallParticipantSipImpl callParticipant =
            activeCallsRepository.findCallParticipant(clientTransaction
                .getDialog());

        if (callParticipant == null)
        {
            logger.debug("Received an authorization challenge for no "
                + "participant. authorizing anyway.");
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
            // if the call and the call participant are no longer there
            if (callParticipant != null)
            {
                callParticipant.setDialog(retryTran.getDialog());
                callParticipant.setFirstTransaction(retryTran);
                callParticipant.setJainSipProvider(jainSipProvider);
            }
            retryTran.sendRequest();
        }
        catch (Exception exc)
        {
            logger.error("We failed to authenticate an INVITE request.", exc);

            // tell the others we couldn't register
            callParticipant.setState(CallParticipantState.FAILED);
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
     * <code>timeoutType = timeoutEvent.getTimeout().getValue();</code>
     * 
     * @param timeoutEvent the timeoutEvent received indicating either the
     *            message retransmit or transaction timed out.
     */
    public void processTimeout(TimeoutEvent timeoutEvent)
    {
        Transaction transaction;
        if (timeoutEvent.isServerTransaction())
        {
            // don't care. or maybe a stack bug?
            return;
        }
        else
        {
            transaction = timeoutEvent.getClientTransaction();
        }

        CallParticipantSipImpl callParticipant =
            activeCallsRepository.findCallParticipant(transaction.getDialog());

        if (callParticipant == null)
        {
            logger.debug("Got a headless timeout event." + timeoutEvent);
            return;
        }

        // change status
        callParticipant.setState(CallParticipantState.FAILED,
            "The remote party has not replied!"
                + "The call will be disconnected");
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
     */
    public void processIOException(IOExceptionEvent exceptionEvent)
    {
        logger.error("Got an asynchronous exception event. host="
            + exceptionEvent.getHost() + " port=" + exceptionEvent.getPort());
    }

    /**
     * Process an asynchronously reported DialogTerminatedEvent.
     * 
     * @param dialogTerminatedEvent -- an event that indicates that the dialog
     *            has transitioned into the terminated state.
     */
    public void processDialogTerminated(
        DialogTerminatedEvent dialogTerminatedEvent)
    {
        CallParticipantSipImpl callParticipant =
            activeCallsRepository.findCallParticipant(dialogTerminatedEvent
                .getDialog());

        if (callParticipant == null)
        {
            return;
        }

        // change status
        callParticipant.setState(CallParticipantState.DISCONNECTED);
    }

    /**
     * Creates an invite request destined for <tt>callee</tt>.
     * 
     * @param toAddress the sip address of the callee that the request is meant
     *            for.
     * @return a newly created sip <tt>Request</tt> destined for <tt>callee</tt>
     *         .
     * @throws OperationFailedException with the corresponding code if creating
     *             the request fails.
     */
    private Request createInviteRequest(Address toAddress)
        throws OperationFailedException
    {
        InetAddress destinationInetAddress = null;
        try
        {
            destinationInetAddress =
                InetAddress.getByName(((SipURI) toAddress.getURI()).getHost());
        }
        catch (UnknownHostException ex)
        {
            throw new IllegalArgumentException(((SipURI) toAddress.getURI())
                .getHost()
                + " is not a valid internet address " + ex.getMessage());
        }
        // Call ID
        CallIdHeader callIdHeader =
            protocolProvider.getDefaultJainSipProvider().getNewCallId();

        // CSeq
        CSeqHeader cSeqHeader = null;
        try
        {
            cSeqHeader =
                protocolProvider.getHeaderFactory().createCSeqHeader(1l,
                    Request.INVITE);
        }
        catch (InvalidArgumentException ex)
        {
            // Shouldn't happen
            logger.error("An unexpected erro occurred while"
                + "constructing the CSeqHeadder", ex);
            throw new OperationFailedException(
                "An unexpected erro occurred while"
                    + "constructing the CSeqHeadder",
                OperationFailedException.INTERNAL_ERROR, ex);
        }
        catch (ParseException exc)
        {
            // shouldn't happen
            logger.error("An unexpected erro occurred while"
                + "constructing the CSeqHeadder", exc);
            throw new OperationFailedException(
                "An unexpected erro occurred while"
                    + "constructing the CSeqHeadder",
                OperationFailedException.INTERNAL_ERROR, exc);
        }

        // FromHeader
        String localTag = protocolProvider.generateLocalTag();
        FromHeader fromHeader = null;
        ToHeader toHeader = null;
        try
        {
            // FromHeader
            fromHeader =
                protocolProvider.getHeaderFactory().createFromHeader(
                    protocolProvider.getOurSipAddress(), localTag);

            // ToHeader
            toHeader =
                protocolProvider.getHeaderFactory().createToHeader(toAddress,
                    null);
        }
        catch (ParseException ex)
        {
            // these two should never happen.
            logger.error("An unexpected erro occurred while"
                + "constructing the ToHeader", ex);
            throw new OperationFailedException(
                "An unexpected erro occurred while"
                    + "constructing the ToHeader",
                OperationFailedException.INTERNAL_ERROR, ex);
        }

        // ViaHeaders
        ArrayList viaHeaders =
            protocolProvider.getLocalViaHeaders(destinationInetAddress,
                protocolProvider.getDefaultListeningPoint());

        // MaxForwards
        MaxForwardsHeader maxForwards = protocolProvider.getMaxForwardsHeader();

        // Contact
        ContactHeader contactHeader = protocolProvider.getContactHeader();

        Request invite = null;
        try
        {
            invite =
                protocolProvider.getMessageFactory().createRequest(
                    toHeader.getAddress().getURI(), Request.INVITE,
                    callIdHeader, cSeqHeader, fromHeader, toHeader, viaHeaders,
                    maxForwards);

        }
        catch (ParseException ex)
        {
            // shouldn't happen
            logger.error("Failed to create invite Request!", ex);
            throw new OperationFailedException(
                "Failed to create invite Request!",
                OperationFailedException.INTERNAL_ERROR, ex);
        }

        // User Agent
        UserAgentHeader userAgentHeader =
            protocolProvider.getSipCommUserAgentHeader();
        if (userAgentHeader != null)
            invite.addHeader(userAgentHeader);

        // add the contact header.
        invite.addHeader(contactHeader);

        return invite;
    }

    /**
     * Creates a new call and sends a RINGING response.
     * 
     * @param sourceProvider the provider containing <tt>sourceTransaction</tt>.
     * @param serverTransaction the transaction containing the received request.
     * @param invite the Request that we've just received.
     */
    private void processInvite(SipProvider sourceProvider,
        ServerTransaction serverTransaction, Request invite)
    {
        Dialog dialog = serverTransaction.getDialog();
        CallParticipantSipImpl callParticipant =
            activeCallsRepository.findCallParticipant(dialog);
        int statusCode;
        if (callParticipant == null)
        {
            statusCode = Response.RINGING;

            logger.trace("Creating call participant.");
            callParticipant =
                createCallParticipantFor(serverTransaction, sourceProvider);
            logger.trace("call participant created = " + callParticipant);
        }
        else
            statusCode = Response.OK;

        // sdp description may be in acks - bug report Laurent Michel
        ContentLengthHeader cl = invite.getContentLength();
        if (cl != null && cl.getContentLength() > 0)
        {
            callParticipant
                .setSdpDescription(new String(invite.getRawContent()));
        }

        logger.trace("Will verify whether INVITE is properly addressed.");
        // Are we the one they are looking for?
        javax.sip.address.URI calleeURI = dialog.getLocalParty().getURI();

        if (calleeURI.isSipURI())
        {
            boolean assertUserMatch =
                Boolean.valueOf(
                    SipActivator.getConfigurationService().getString(
                        FAIL_CALLS_ON_DEST_USER_MISMATCH)).booleanValue();

            if (assertUserMatch)
            {
                // user info is case sensitive according to rfc3261
                String calleeUser = ((SipURI) calleeURI).getUser();
                String localUser =
                    ((SipURI) protocolProvider.getOurSipAddress().getURI())
                        .getUser();

                if (calleeUser != null && !calleeUser.equals(localUser))
                {
                    callParticipant
                        .setState(
                            CallParticipantState.FAILED,
                            "A call was received here while it appeared "
                                + "destined to someone else. The call was rejected.");

                    Response notFound = null;
                    try
                    {
                        notFound =
                            protocolProvider.getMessageFactory()
                                .createResponse(Response.NOT_FOUND, invite);

                        // attach a to tag
                        protocolProvider.attachToTag(notFound, dialog);
                        notFound.setHeader(protocolProvider
                            .getSipCommUserAgentHeader());
                    }
                    catch (ParseException ex)
                    {
                        logger.error("Error while trying to create a response",
                            ex);
                        callParticipant.setState(CallParticipantState.FAILED,
                            "InernalError: " + ex.getMessage());
                        return;
                    }
                    try
                    {
                        serverTransaction.sendResponse(notFound);
                        logger.debug("sent a not found response: " + notFound);
                    }
                    catch (Exception ex)
                    {
                        logger.error("Error while trying to send a response",
                            ex);
                        callParticipant.setState(CallParticipantState.FAILED,
                            "Internal Error: " + ex.getMessage());
                        return;
                    }
                    return;
                }
            }
        }

        // Send statusCode
        String statusCodeString =
            (statusCode == Response.RINGING) ? "RINGING" : "OK";
        logger.debug("Invite seems ok, we'll say " + statusCodeString + ".");
        Response response = null;
        try
        {
            response =
                protocolProvider.getMessageFactory().createResponse(statusCode,
                    invite);
            protocolProvider.attachToTag(response, dialog);
            response.setHeader(protocolProvider.getSipCommUserAgentHeader());

            // set our display name
            ((ToHeader) response.getHeader(ToHeader.NAME)).getAddress()
                .setDisplayName(protocolProvider.getOurDisplayName());

            response.addHeader(protocolProvider.getContactHeader());

            if (statusCode != Response.RINGING)
            {
                try
                {
                    processInviteSendingResponse(callParticipant, response);
                }
                catch (OperationFailedException ex)
                {
                    logger.error("Error while trying to send a request", ex);
                    callParticipant.setState(CallParticipantState.FAILED,
                        "Internal Error: " + ex.getMessage());
                    return;
                }
            }
        }
        catch (ParseException ex)
        {
            logger.error("Error while trying to send a request", ex);
            callParticipant.setState(CallParticipantState.FAILED,
                "Internal Error: " + ex.getMessage());
            return;
        }
        try
        {
            logger.trace("will send " + statusCodeString + " response: ");
            serverTransaction.sendResponse(response);
            logger.debug("sent a " + statusCodeString + " response: "
                + response);
        }
        catch (Exception ex)
        {
            logger.error("Error while trying to send a request", ex);
            callParticipant.setState(CallParticipantState.FAILED,
                "Internal Error: " + ex.getMessage());
            return;
        }

        if (statusCode != Response.RINGING)
        {
            try
            {
                processInviteSentResponse(callParticipant, response);
            }
            catch (OperationFailedException ex)
            {
                logger.error("Error after sending a request", ex);
            }
        }
    }

    /**
     * Provides a hook for this instance to take last configuration steps on a
     * specific <tt>Response</tt> before it is sent to a specific
     * <tt>CallParticipant</tt> as part of the execution of
     * {@link #processInvite(SipProvider, ServerTransaction, Request)}.
     * 
     * @param participant the <tt>CallParticipant</tt> to receive a specific
     *            <tt>Response</tt>
     * @param response the <tt>Response</tt> to be sent to the
     *            <tt>participant</tt>
     * @throws OperationFailedException
     * @throws ParseException
     */
    private void processInviteSendingResponse(CallParticipant participant,
        Response response) throws OperationFailedException, ParseException
    {
        /*
         * At the time of this writing, we're only getting called because a
         * response to a call-hold invite is to be sent.
         */

        CallSession callSession =
            ((CallSipImpl) participant.getCall()).getMediaCallSession();
        CallParticipantSipImpl sipParticipant =
            (CallParticipantSipImpl) participant;
        String sdpOffer = sipParticipant.getSdpDescription();

        String sdpAnswer = null;
        try
        {
            sdpAnswer =
                callSession.createSdpDescriptionForHold(sdpOffer, callSession
                    .isSdpOfferToHold(sdpOffer));
        }
        catch (MediaException ex)
        {
            throwOperationFailedException(
                "Failed to create SDP answer to put-on/off-hold request.",
                OperationFailedException.INTERNAL_ERROR, ex);
        }

        response.setContent(sdpAnswer, protocolProvider.getHeaderFactory()
            .createContentTypeHeader("application", "sdp"));
    }

    /**
     * Provides a hook for this instance to take immediate steps after a
     * specific <tt>Response</tt> has been sent to a specific
     * <tt>CallParticipant</tt> as part of the execution of
     * {@link #processInvite(SipProvider, ServerTransaction, Request)}.
     * 
     * @param participant the <tt>CallParticipant</tt> who was sent a specific
     *            <tt>Response</tt>
     * @param response the <tt>Response</tt> that has just been sent to the
     *            <tt>participant</tt>
     * @throws OperationFailedException
     * @throws ParseException
     */
    private void processInviteSentResponse(CallParticipant participant,
        Response response) throws OperationFailedException
    {
        /*
         * At the time of this writing, we're only getting called because a
         * response to a call-hold invite is to be sent.
         */

        CallSession callSession =
            ((CallSipImpl) participant.getCall()).getMediaCallSession();
        CallParticipantSipImpl sipParticipant =
            (CallParticipantSipImpl) participant;

        boolean on = false;
        try
        {
            on =
                callSession
                    .isSdpOfferToHold(sipParticipant.getSdpDescription());
        }
        catch (MediaException ex)
        {
            throwOperationFailedException(
                "Failed to create SDP answer to put-on/off-hold request.",
                OperationFailedException.INTERNAL_ERROR, ex);
        }

        callSession.putOnHold(on, false);

        CallParticipantState state = sipParticipant.getState();
        if (CallParticipantState.ON_HOLD_LOCALLY.equals(state))
        {
            if (on)
                sipParticipant.setState(CallParticipantState.ON_HOLD_MUTUALLY);
        }
        else if (CallParticipantState.ON_HOLD_MUTUALLY.equals(state))
        {
            if (!on)
                sipParticipant.setState(CallParticipantState.ON_HOLD_LOCALLY);
        }
        else if (CallParticipantState.ON_HOLD_REMOTELY.equals(state))
        {
            if (!on)
                sipParticipant.setState(CallParticipantState.CONNECTED);
        }
        else if (on)
        {
            sipParticipant.setState(CallParticipantState.ON_HOLD_REMOTELY);
        }
    }

    /**
     * Sets the state of the corresponding call participant to DISCONNECTED and
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
        CallParticipantSipImpl callParticipant =
            activeCallsRepository.findCallParticipant(dialog);

        if (callParticipant == null)
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
            ((CallSipImpl) callParticipant.getCall()).getMediaCallSession()
                .stopStreaming();
        }
        else
        {
            callParticipant.setState(CallParticipantState.DISCONNECTED);
        }
    }

    /**
     * Updates the session description and sends the state of the corresponding
     * call participant to CONNECTED.
     * 
     * @param serverTransaction the transaction that the Ack was received in.
     * @param ackRequest Request
     */
    void processAck(ServerTransaction serverTransaction, Request ackRequest)
    {
        // find the call
        CallParticipantSipImpl callParticipant =
            activeCallsRepository.findCallParticipant(serverTransaction
                .getDialog());

        if (callParticipant == null)
        {
            // this is most probably the ack for a killed call - don't signal it
            logger.debug("didn't find an ack's call, returning");
            return;
        }

        ContentLengthHeader cl = ackRequest.getContentLength();
        if (cl != null && cl.getContentLength() > 0)
        {
            callParticipant.setSdpDescription(new String(ackRequest
                .getRawContent()));
        }
        // change status
        if (!CallParticipantState.isOnHold(callParticipant.getState()))
            callParticipant.setState(CallParticipantState.CONNECTED);
    }

    /**
     * Sets the state of the specifies call participant as DISCONNECTED.
     * 
     * @param serverTransaction the transaction that the cancel was received in.
     * @param cancelRequest the Request that we've just received.
     */
    void processCancel(ServerTransaction serverTransaction,
        Request cancelRequest)
    {
        // find the call
        CallParticipantSipImpl callParticipant =
            activeCallsRepository.findCallParticipant(serverTransaction
                .getDialog());

        if (callParticipant == null)
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
            logger.error(
                "Failed to create an OK Response to an CANCEL request.", ex);
            callParticipant.setState(CallParticipantState.FAILED,
                "Failed to create an OK Response to an CANCEL request.");
        }
        catch (Exception ex)
        {
            logger.error("Failed to send an OK Response to an CANCEL request.",
                ex);
            callParticipant.setState(CallParticipantState.FAILED,
                "Failed to send an OK Response to an CANCEL request.");
        }
        try
        {
            // stop the invite transaction as well
            Transaction tran = callParticipant.getFirstTransaction();
            // should be server transaction and misplaced cancels should be
            // filtered by the stack but it doesn't hurt checking anyway
            if (!(tran instanceof ServerTransaction))
            {
                logger.error("Received a misplaced CANCEL request!");
                return;
            }

            ServerTransaction inviteTran = (ServerTransaction) tran;
            Request invite = callParticipant.getFirstTransaction().getRequest();
            Response requestTerminated =
                protocolProvider.getMessageFactory().createResponse(
                    Response.REQUEST_TERMINATED, invite);
            requestTerminated.setHeader(protocolProvider
                .getSipCommUserAgentHeader());
            protocolProvider.attachToTag(requestTerminated, callParticipant
                .getDialog());
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
        callParticipant.setState(CallParticipantState.DISCONNECTED);
    }

    /**
     * Processes a specific REFER request i.e. attempts to transfer the
     * call/call participant receiving the request to a specific transfer
     * target.
     * 
     * @param serverTransaction the <code>ServerTransaction</code> containing
     *            the REFER request
     * @param referRequest the very REFER request
     * @param sipProvider the provider containing <code>serverTransaction</code>
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
            accepted =
                protocolProvider.getMessageFactory().createResponse(
                    Response.ACCEPTED, referRequest);
            protocolProvider.attachToTag(accepted, dialog);
            accepted.setHeader(protocolProvider.getSipCommUserAgentHeader());
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
                    logger.error(
                        "Failed to make the REFER request keep the dialog alive after BYE:\n"
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
            referToCall = createOutgoingCall(referToAddress);
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
     * Processes a specific <code>Request.NOTIFY</code> request for the purposes
     * of telephony.
     * 
     * @param serverTransaction the <code>ServerTransaction</code> containing
     *            the <code>Request.NOTIFY</code> request
     * @param notifyRequest the <code>Request.NOTIFY</code> request to be
     *            processed
     */
    private void processNotify(ServerTransaction serverTransaction,
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
            return;
        }

        SubscriptionStateHeader ssHeader =
            (SubscriptionStateHeader) notifyRequest
                .getHeader(SubscriptionStateHeader.NAME);
        if (ssHeader == null)
        {
            logger
                .error("NOTIFY of refer event type with no Subscription-State header.");
            return;
        }

        Dialog dialog = serverTransaction.getDialog();
        CallParticipantSipImpl participant = activeCallsRepository.findCallParticipant(dialog);

        if (participant == null)
        {
            logger.debug("Received a stray refer NOTIFY request.");
            return;
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
            participant.setState(CallParticipantState.DISCONNECTED, message);
            return;
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
            participant.setState(CallParticipantState.DISCONNECTED, message);
            return;
        }

        if (SubscriptionStateHeader.TERMINATED.equals(ssHeader.getState())
            && (DialogUtils
                .removeSubscriptionThenIsDialogAlive(dialog, "refer") == false))
        {
            participant.setState(CallParticipantState.DISCONNECTED);
        }

        if ((CallParticipantState.DISCONNECTED.equals(participant.getState()) == false)
            && (DialogUtils.isByeProcessed(dialog) == false))
        {
            boolean dialogIsAlive;
            try
            {
                dialogIsAlive = sayBye(participant);
            }
            catch (OperationFailedException ex)
            {
                logger.error(
                    "Failed to send BYE in response to refer NOTIFY request.",
                    ex);
                dialogIsAlive = false;
            }
            if (dialogIsAlive == false)
            {
                participant.setState(CallParticipantState.DISCONNECTED);
            }
        }
    }

    /**
     * Tracks the state changes of a specific <code>Call</code> and sends a
     * session-terminating NOTIFY request to the <code>Dialog</code> which
     * referred to the call in question as soon as the outcome of the refer is
     * determined.
     * 
     * @param referToCall the <code>Call</code> to track and send a NOTIFY
     *            request for
     * @param sendNotifyRequest <tt>true</tt> if a session-terminating NOTIFY
     *            request should be sent to the <code>Dialog</code> which
     *            referred to <code>referToCall</code>; <tt>false</tt> to send
     *            no such NOTIFY request
     * @param dialog the <code>Dialog</code> which initiated the specified call
     *            as part of processing a REFER request
     * @param sipProvider the <code>SipProvider</code> to send the NOTIFY
     *            request through
     * @param subscription the subscription to be terminated when the NOTIFY
     *            request is sent
     * @return <tt>true</tt> if a session-terminating NOTIFY request was sent
     *         and the state of <code>referToCall</code> should no longer be
     *         tracked; <tt>false</tt> if it's too early to send a
     *         session-terminating NOTIFY request and the tracking of the state
     *         of <code>referToCall</code> should continue
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
                CallState.CALL_IN_PROGRESS.equals(referToCallState) ? "SIP/2.0 200 OK"
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
        if (DialogUtils.removeSubscriptionThenIsDialogAlive(dialog,
            subscription) == false)
        {
            CallParticipantSipImpl callParticipant =
                activeCallsRepository.findCallParticipant(dialog);
            if (callParticipant != null)
            {
                callParticipant.setState(CallParticipantState.DISCONNECTED);
            }
        }
        return true;
    }

    /**
     * Sends a <code>Request.NOTIFY</code> request in a specific
     * <code>Dialog</code> as part of the communication associated with an
     * earlier-received <code>Request.REFER<code> request. The sent NOTIFY has
     * a specific <code>Subscription-State</code> header and reason, carries a
     * specific body content and is sent through a specific
     * <code>SipProvider</code>.
     * 
     * @param dialog the <code>Dialog</code> to send the NOTIFY request in
     * @param subscriptionState the <code>Subscription-State</code> header to be
     *            sent with the NOTIFY request
     * @param reasonCode the reason for the specified
     *            <code>subscriptionState</code> if any; <tt>null</tt> otherwise
     * @param content the content to be carried in the body of the sent NOTIFY
     *            request
     * @param sipProvider the <code>SipProvider</code> to send the NOTIFY
     *            request through
     * @throws OperationFailedException
     */
    private void sendReferNotifyRequest(Dialog dialog,
        String subscriptionState, String reasonCode, Object content,
        SipProvider sipProvider) throws OperationFailedException
    {
        Request notify = createRequest(dialog, Request.NOTIFY);
        HeaderFactory headerFactory = protocolProvider.getHeaderFactory();

        // Populate the request.
        String eventType = "refer";
        try
        {
            notify.setHeader(headerFactory.createEventHeader(eventType));
        }
        catch (ParseException ex)
        {
            throwOperationFailedException("Failed to create " + eventType
                + " Event header.", OperationFailedException.INTERNAL_ERROR, ex);
        }

        SubscriptionStateHeader ssHeader = null;
        try
        {
            ssHeader =
                headerFactory.createSubscriptionStateHeader(subscriptionState);
            if (reasonCode != null)
                ssHeader.setReasonCode(reasonCode);
        }
        catch (ParseException ex)
        {
            throwOperationFailedException("Failed to create "
                + subscriptionState + " Subscription-State header.",
                OperationFailedException.INTERNAL_ERROR, ex);
        }
        notify.setHeader(ssHeader);

        ContentTypeHeader ctHeader = null;
        try
        {
            ctHeader =
                headerFactory.createContentTypeHeader("message", "sipfrag");
        }
        catch (ParseException ex)
        {
            throwOperationFailedException(
                "Failed to create Content-Type header.",
                OperationFailedException.INTERNAL_ERROR, ex);
        }
        try
        {
            notify.setContent(content, ctHeader);
        }
        catch (ParseException ex)
        {
            throwOperationFailedException("Failed to set NOTIFY body/content.",
                OperationFailedException.INTERNAL_ERROR, ex);
        }

        sendRequest(sipProvider, notify, dialog);
    }

    /**
     * Creates a new {@link Request} of a specific method which is to be sent in
     * a specific <code>Dialog</code> and populates its generally-necessary
     * headers such as the Authorization header.
     * 
     * @param dialog the <code>Dialog</code> to create the new
     *            <code>Request</code> in
     * @param method the method of the newly-created <code>Request<code>
     * @return a new {@link Request} of the specified <code>method</code> which
     *         is to be sent in the specified <code>dialog</code> and populated
     *         with its generally-necessary headers such as the Authorization
     *         header
     * @throws OperationFailedException
     */
    private Request createRequest(Dialog dialog, String method)
        throws OperationFailedException
    {
        Request request = null;
        try
        {
            request = dialog.createRequest(method);
        }
        catch (SipException ex)
        {
            throwOperationFailedException("Failed to create " + method
                + " request.", OperationFailedException.INTERNAL_ERROR, ex);
        }

        /*
         * The authorization-related headers are the responsibility of the
         * application (according to the Javadoc of JAIN-SIP).
         */
        AuthorizationHeader authorization =
            protocolProvider.getSipSecurityManager()
                .getCachedAuthorizationHeader(
                    ((CallIdHeader) request.getHeader(CallIdHeader.NAME))
                        .getCallId());
        if (authorization != null)
        {
            request.setHeader(authorization);
        }

        return request;
    }

    /**
     * Indicates a user request to end a call with the specified call
     * participant. Depending on the state of the call the method would send a
     * CANCEL, BYE, or BUSY_HERE and set the new state to DISCONNECTED.
     * 
     * @param participant the participant that we'd like to hang up on.
     * @throws ClassCastException if participant is not an instance of this
     *             CallParticipantSipImpl.
     * @throws OperationFailedException if we fail to terminate the call.
     */
    public synchronized void hangupCallParticipant(CallParticipant participant)
        throws ClassCastException,
        OperationFailedException
    {
        // do nothing if the call is already ended
        if (participant.getState().equals(CallParticipantState.DISCONNECTED)
            || participant.getState().equals(CallParticipantState.FAILED))
        {
            logger.debug("Ignoring a request to hangup a call participant "
                + "that is already DISCONNECTED");
            return;
        }

        CallParticipantSipImpl callParticipant =
            (CallParticipantSipImpl) participant;

        CallParticipantState participantState = callParticipant.getState();
        if (participantState.equals(CallParticipantState.CONNECTED)
            || CallParticipantState.isOnHold(participantState))
        {
            sayBye(callParticipant);
            callParticipant.setState(CallParticipantState.DISCONNECTED);
        }
        else if (callParticipant.getState().equals(
            CallParticipantState.CONNECTING)
            || callParticipant.getState().equals(
                CallParticipantState.CONNECTING_WITH_EARLY_MEDIA)
            || callParticipant.getState().equals(
                CallParticipantState.ALERTING_REMOTE_SIDE))
        {
            if (callParticipant.getFirstTransaction() != null)
            {
                // Someone knows about us. Let's be polite and say we are
                // leaving
                sayCancel(callParticipant);
            }
            callParticipant.setState(CallParticipantState.DISCONNECTED);
        }
        else if (participantState.equals(CallParticipantState.INCOMING_CALL))
        {
            callParticipant.setState(CallParticipantState.DISCONNECTED);
            sayBusyHere(callParticipant);
        }
        // For FAILE and BUSY we only need to update CALL_STATUS
        else if (participantState.equals(CallParticipantState.BUSY))
        {
            callParticipant.setState(CallParticipantState.DISCONNECTED);
        }
        else if (participantState.equals(CallParticipantState.FAILED))
        {
            callParticipant.setState(CallParticipantState.DISCONNECTED);
        }
        else
        {
            callParticipant.setState(CallParticipantState.DISCONNECTED);
            logger.error("Could not determine call participant state!");
        }
    } // end call

    /**
     * Sends an Internal Error response to <tt>callParticipant</tt>.
     * 
     * @param callParticipant the call participant that we need to say bye to.
     * 
     * @throws OperationFailedException if we failed constructing or sending a
     *             SIP Message.
     */
    public void sayInternalError(CallParticipantSipImpl callParticipant)
        throws OperationFailedException
    {
        sayError(callParticipant, Response.SERVER_INTERNAL_ERROR);
    }

    /**
     * Send an error response with the <tt>errorCode</tt> code to
     * <tt>callParticipant</tt>.
     * 
     * @param callParticipant the call participant that we need to say bye to.
     * @param errorCode the code that the response should have.
     * 
     * @throws OperationFailedException if we failed constructing or sending a
     *             SIP Message.
     */
    public void sayError(CallParticipantSipImpl callParticipant, int errorCode)
        throws OperationFailedException
    {
        Dialog dialog = callParticipant.getDialog();
        callParticipant.setState(CallParticipantState.FAILED);
        if (dialog == null)
        {
            logger.error("Failed to extract participant's associated dialog! "
                + "Ending Call!");
            throw new OperationFailedException(
                "Failed to extract participant's associated dialog! "
                    + "Ending Call!", OperationFailedException.INTERNAL_ERROR);
        }
        Transaction transaction = callParticipant.getFirstTransaction();
        if (transaction == null || !dialog.isServer())
        {
            logger.error("Failed to extract a transaction"
                + " from the call's associated dialog!");
            throw new OperationFailedException(
                "Failed to extract a transaction from the participant's "
                    + "associated dialog!",
                OperationFailedException.INTERNAL_ERROR);
        }

        ServerTransaction serverTransaction = (ServerTransaction) transaction;
        Response internalError = null;
        try
        {
            internalError =
                protocolProvider.getMessageFactory().createResponse(errorCode,
                    callParticipant.getFirstTransaction().getRequest());
            protocolProvider.attachToTag(internalError, dialog);
        }
        catch (ParseException ex)
        {
            logger.error(
                "Failed to construct an OK response to an INVITE request", ex);
            throw new OperationFailedException(
                "Failed to construct an OK response to an INVITE request",
                OperationFailedException.INTERNAL_ERROR, ex);
        }
        ContactHeader contactHeader = protocolProvider.getContactHeader();
        internalError.addHeader(contactHeader);
        try
        {
            serverTransaction.sendResponse(internalError);
            if (logger.isDebugEnabled())
                logger.debug("sent response: " + internalError);
        }
        catch (Exception ex)
        {
            logger.error("Failed to send an OK response to an INVITE request",
                ex);
            throw new OperationFailedException(
                "Failed to send an OK response to an INVITE request",
                OperationFailedException.INTERNAL_ERROR, ex);
        }
    } // internal error

    /**
     * Sends a BYE request to <tt>callParticipant</tt>.
     * 
     * @param callParticipant the call participant that we need to say bye to.
     * @return <tt>true</tt> if the <code>Dialog</code> should be considered
     *         alive after sending the BYE request (e.g. when there're still
     *         active subscriptions); <tt>false</tt>, otherwise
     * @throws OperationFailedException if we failed constructing or sending a
     *             SIP Message.
     */
    private boolean sayBye(CallParticipantSipImpl callParticipant)
        throws OperationFailedException
    {
        Dialog dialog = callParticipant.getDialog();

        Request request = callParticipant.getFirstTransaction().getRequest();
        Request bye = null;
        try
        {
            bye = dialog.createRequest(Request.BYE);

            // we have to set the via headers our selves because otherwise
            // jain sip would send them with a 0.0.0.0 address
            InetAddress destinationInetAddress = null;
            String host = ((SipURI) bye.getRequestURI()).getHost();
            try
            {
                destinationInetAddress = InetAddress.getByName(host);
            }
            catch (UnknownHostException ex)
            {
                throw new IllegalArgumentException(host
                    + " is not a valid internet address " + ex.getMessage());
            }

            ArrayList viaHeaders =
                protocolProvider.getLocalViaHeaders(destinationInetAddress,
                    protocolProvider.getRegistrarConnection()
                        .getRegistrarListeningPoint());
            bye.setHeader((ViaHeader) viaHeaders.get(0));
            bye.addHeader(protocolProvider.getSipCommUserAgentHeader());
        }
        catch (SipException ex)
        {
            throwOperationFailedException("Failed to create bye request!",
                OperationFailedException.INTERNAL_ERROR, ex);
        }

        sendRequest(callParticipant.getJainSipProvider(), bye, dialog);

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
            throwOperationFailedException(
                "Failed to determine whether the dialog should stay alive.",
                OperationFailedException.INTERNAL_ERROR, ex);
            return false;
        }
    } // bye

    /**
     * Sends a Cancel request to <tt>callParticipant</tt>.
     * 
     * @param callParticipant the call participant that we need to cancel.
     * 
     * @throws OperationFailedException we failed to construct or send the
     *             CANCEL request.
     */
    private void sayCancel(CallParticipantSipImpl callParticipant)
        throws OperationFailedException
    {
        Request request = callParticipant.getFirstTransaction().getRequest();
        if (callParticipant.getDialog().isServer())
        {
            logger.error("Cannot cancel a server transaction");
            throw new OperationFailedException(
                "Cannot cancel a server transaction",
                OperationFailedException.INTERNAL_ERROR);
        }

        ClientTransaction clientTransaction =
            (ClientTransaction) callParticipant.getFirstTransaction();
        try
        {
            Request cancel = clientTransaction.createCancel();
            ClientTransaction cancelTransaction =
                callParticipant.getJainSipProvider().getNewClientTransaction(
                    cancel);
            cancelTransaction.sendRequest();
            logger.debug("sent request:\n" + cancel);
        }
        catch (SipException ex)
        {
            logger.error("Failed to send the CANCEL request", ex);
            throw new OperationFailedException(
                "Failed to send the CANCEL request",
                OperationFailedException.NETWORK_FAILURE, ex);
        }
    } // cancel

    /**
     * Sends a BUSY_HERE response to <tt>callParticipant</tt>.
     * 
     * @param callParticipant the call participant that we need to send busy
     *            tone to.
     * @throws OperationFailedException if we fail to create or send the
     *             response
     */
    private void sayBusyHere(CallParticipantSipImpl callParticipant)
        throws OperationFailedException
    {
        Request request = callParticipant.getFirstTransaction().getRequest();
        Response busyHere = null;
        try
        {
            busyHere =
                protocolProvider.getMessageFactory().createResponse(
                    Response.BUSY_HERE, request);
            busyHere.setHeader(protocolProvider.getSipCommUserAgentHeader());
            protocolProvider.attachToTag(busyHere, callParticipant.getDialog());
        }
        catch (ParseException ex)
        {
            logger.error("Failed to create the BUSY_HERE response!", ex);
            throw new OperationFailedException(
                "Failed to create the BUSY_HERE response!",
                OperationFailedException.INTERNAL_ERROR, ex);
        }

        if (!callParticipant.getDialog().isServer())
        {
            logger.error("Cannot send BUSY_HERE in a client transaction");
            throw new OperationFailedException(
                "Cannot send BUSY_HERE in a client transaction",
                OperationFailedException.INTERNAL_ERROR);
        }
        ServerTransaction serverTransaction =
            (ServerTransaction) callParticipant.getFirstTransaction();

        try
        {
            serverTransaction.sendResponse(busyHere);
            logger.debug("sent response:\n" + busyHere);
        }
        catch (Exception ex)
        {
            logger.error("Failed to send the BUSY_HERE response", ex);
            throw new OperationFailedException(
                "Failed to send the BUSY_HERE response",
                OperationFailedException.NETWORK_FAILURE, ex);
        }
    } // busy here

    /**
     * * Indicates a user request to answer an incoming call from the specified
     * CallParticipant.
     * 
     * Sends an OK response to <tt>callParticipant</tt>. Make sure that the call
     * participant contains an sdp description when you call this method.
     * 
     * @param participant the call participant that we need to send the ok to.
     * @throws OperationFailedException if we fail to create or send the
     *             response.
     */
    public synchronized void answerCallParticipant(CallParticipant participant)
        throws OperationFailedException
    {
        CallParticipantSipImpl callParticipant =
            (CallParticipantSipImpl) participant;
        Transaction transaction = callParticipant.getFirstTransaction();
        Dialog dialog = callParticipant.getDialog();

        if (transaction == null || !dialog.isServer())
        {
            callParticipant.setState(CallParticipantState.DISCONNECTED);
            throw new OperationFailedException(
                "Failed to extract a ServerTransaction "
                    + "from the call's associated dialog!",
                OperationFailedException.INTERNAL_ERROR);
        }

        CallParticipantState participantState = participant.getState();

        if (participantState.equals(CallParticipantState.CONNECTED)
            || CallParticipantState.isOnHold(participantState))
        {
            logger.info("Ignoring user request to answer a CallParticipant "
                + "that is already connected. CP:" + participant);
            return;
        }

        ServerTransaction serverTransaction = (ServerTransaction) transaction;
        Response ok = null;
        try
        {
            ok =
                createOKResponse(callParticipant.getFirstTransaction()
                    .getRequest(), dialog);
        }
        catch (ParseException ex)
        {
            callParticipant.setState(CallParticipantState.DISCONNECTED);
            logger.error(
                "Failed to construct an OK response to an INVITE request", ex);
            throw new OperationFailedException(
                "Failed to construct an OK response to an INVITE request",
                OperationFailedException.INTERNAL_ERROR, ex);
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
            callParticipant.setState(CallParticipantState.DISCONNECTED);
            logger.error(
                "Failed to create a content type header for the OK response",
                ex);
            throw new OperationFailedException(
                "Failed to create a content type header for the OK response",
                OperationFailedException.INTERNAL_ERROR, ex);
        }

        try
        {
            CallSession callSession =
                SipActivator.getMediaService().createCallSession(
                    callParticipant.getCall());
            ((CallSipImpl) callParticipant.getCall())
                .setMediaCallSession(callSession);

            String sdp = null;
            // if the offer was in the invite create an sdp answer
            if (callParticipant.getSdpDescription() != null
                && callParticipant.getSdpDescription().length() > 0)
            {

                sdp =
                    callSession.processSdpOffer(callParticipant,
                        callParticipant.getSdpDescription());

                // set the call url in case there was one
                /**
                 * @todo this should be done in CallSession, once we move it
                 *       here.
                 */
                callParticipant.setCallInfoURL(callSession.getCallInfoURL());
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
            this.sayError((CallParticipantSipImpl) participant,
                Response.NOT_ACCEPTABLE_HERE);
            logger.error("No sdp data was provided for the ok response to "
                + "an INVITE request!", ex);
            throw new OperationFailedException(
                "Failed to created an SDP description for an ok response "
                    + "to an INVITE request!",
                OperationFailedException.INTERNAL_ERROR, ex);
        }
        catch (ParseException ex)
        {
            callParticipant.setState(CallParticipantState.DISCONNECTED);
            logger.error(
                "Failed to parse sdp data while creating invite request!", ex);
            throw new OperationFailedException(
                "Failed to parse sdp data while creating invite request!",
                OperationFailedException.INTERNAL_ERROR, ex);
        }

        ContactHeader contactHeader = protocolProvider.getContactHeader();
        ok.addHeader(contactHeader);
        try
        {
            serverTransaction.sendResponse(ok);
            if (logger.isDebugEnabled())
                logger.debug("sent response\n" + ok);
        }
        catch (Exception ex)
        {
            callParticipant.setState(CallParticipantState.DISCONNECTED);
            logger.error("Failed to send an OK response to an INVITE request",
                ex);
            throw new OperationFailedException(
                "Failed to send an OK response to an INVITE request",
                OperationFailedException.NETWORK_FAILURE, ex);
        }
    } // answer call

    /**
     * Creates a new {@link Response#OK} response to a specific {@link Request}
     * which is to be sent as part of a specific {@link Dialog}.
     * 
     * @param request the <code>Request</code> to create the OK response for
     * @param containingDialog the <code>Dialog</code> to send the response in
     * @return a new
     *         <code>Response.OK<code> response to the specified <code>request</code>
     *         to be sent as part of the specified <code>containingDialog</code>
     * @throws ParseException
     */
    private Response createOKResponse(Request request, Dialog containingDialog)
        throws ParseException
    {
        Response ok =
            protocolProvider.getMessageFactory().createResponse(Response.OK,
                request);
        protocolProvider.attachToTag(ok, containingDialog);
        ok.setHeader(protocolProvider.getSipCommUserAgentHeader());
        return ok;
    }

    /**
     * Creates a new call and call participant associated with
     * <tt>containingTransaction</tt>
     * 
     * @param containingTransaction the transaction that created the call.
     * @param sourceProvider the provider that the containingTransaction belongs
     *            to.
     * 
     * @return a new instance of a <tt>CallParticipantSipImpl</tt> corresponding
     *         to the <tt>containingTransaction</tt>.
     */
    private CallParticipantSipImpl createCallParticipantFor(
        Transaction containingTransaction, SipProvider sourceProvider)
    {
        CallSipImpl call = new CallSipImpl(protocolProvider);
        CallParticipantSipImpl callParticipant =
            new CallParticipantSipImpl(containingTransaction.getDialog()
                .getRemoteParty(), call);

        if (containingTransaction instanceof ServerTransaction)
            callParticipant.setState(CallParticipantState.INCOMING_CALL);
        else
            callParticipant.setState(CallParticipantState.INITIATING_CALL);

        callParticipant.setDialog(containingTransaction.getDialog());
        callParticipant.setFirstTransaction(containingTransaction);
        callParticipant.setJainSipProvider(sourceProvider);

        activeCallsRepository.addCall(call);

        // notify everyone
        if (containingTransaction instanceof ServerTransaction)
            fireCallEvent(CallEvent.CALL_RECEIVED, call);
        else
            fireCallEvent(CallEvent.CALL_INITIATED, call);

        return callParticipant;
    }

    /**
     * Returns a string representation of this OperationSetBasicTelephony
     * instance including information that would permit to distinguish it among
     * other instances when reading a log file.
     * <p>
     * 
     * @return a string representation of this operation set.
     */
    public String toString()
    {
        String className = getClass().getName();
        try
        {
            className = className.substring(className.lastIndexOf('.') + 1);
        }
        catch (Exception ex)
        {
            // we don't want to fail in this method because we've messed up
            // something with indexes, so just ignore.
        }
        return className + "-[dn=" + protocolProvider.getOurDisplayName()
            + " addr=" + protocolProvider.getOurSipAddress() + "]";
    }

    /**
     * Closes all active calls. And releases resources.
     */
    public synchronized void shutdown()
    {
        logger.trace("Ending all active calls.");
        Iterator activeCalls = this.activeCallsRepository.getActiveCalls();

        // go through all active calls.
        while (activeCalls.hasNext())
        {
            CallSipImpl call = (CallSipImpl) activeCalls.next();

            Iterator callParticipants = call.getCallParticipants();

            // go through all call participants and say bye to every one.
            while (callParticipants.hasNext())
            {
                CallParticipant participant =
                    (CallParticipant) callParticipants.next();
                try
                {
                    this.hangupCallParticipant(participant);
                }
                catch (Exception ex)
                {
                    logger.warn("Failed to properly hangup particpant "
                        + participant, ex);
                }
            }
        }
    }

    /**
     * Sets the mute state of the audio stream being sent to a specific
     * <tt>CallParticipant</tt>.
     * <p>
     * The implementation sends silence through the audio stream.
     * </p>
     * 
     * @param participant the <tt>CallParticipant</tt> who receives the audio
     *            stream to have its mute state set
     * @param mute <tt>true</tt> to mute the audio stream being sent to
     *            <tt>participant</tt>; otherwise, <tt>false</tt>
     */
    public void setMute(CallParticipant participant, boolean mute)
    {
        ((CallSipImpl) participant.getCall()).getMediaCallSession().setMute(
            mute);
    }

    /**
     * Transfers (in the sense of call transfer) a specific
     * <code>CallParticipant</code> to a specific callee address.
     * 
     * @param participant the <code>CallParticipant</code> to be transfered to
     *            the specified callee address
     * @param target the address of the callee to transfer
     *            <code>participant</code> to
     * @throws OperationFailedException
     */
    public void transfer(CallParticipant participant, String target)
        throws OperationFailedException
    {
        Address targetAddress = null;
        try
        {
            targetAddress = protocolProvider.parseAddressStr(target);
        }
        catch (ParseException ex)
        {
            throwOperationFailedException(
                "Failed to parse target address string.",
                OperationFailedException.ILLEGAL_ARGUMENT, ex);
        }

        CallParticipantSipImpl sipParticipant =
            (CallParticipantSipImpl) participant;
        Dialog dialog = sipParticipant.getDialog();
        Request refer = createRequest(dialog, Request.REFER);

        refer.addHeader(protocolProvider.getHeaderFactory()
            .createReferToHeader(targetAddress));

        sendRequest(sipParticipant.getJainSipProvider(), refer, dialog);
    }
}
