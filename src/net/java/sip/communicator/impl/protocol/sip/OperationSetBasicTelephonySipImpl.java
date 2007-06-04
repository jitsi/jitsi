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
 */
public class OperationSetBasicTelephonySipImpl
    implements  OperationSetBasicTelephony
              , SipListener
{
    private static final Logger logger
        = Logger.getLogger(OperationSetBasicTelephonySipImpl.class);

    /**
     * A reference to the <tt>ProtocolProviderServiceSipImpl</tt> instance
     * that created us.
     */
    private ProtocolProviderServiceSipImpl protocolProvider = null;

    /**
     * A liste of listeners registered for call events.
     */
    private Vector callListeners = new Vector();

    /**
     * Contains references for all currently active (non ended) calls.
     */
    private ActiveCallsRepository activeCallsRepository
        = new ActiveCallsRepository(this);

    /**
     * The name of the boolean property that the user could use to specify
     * whether incoming calls should be rejected if the user name in the
     * destination (to) address does not match the one that we have in our
     * sip address.
     */
    private static final String FAIL_CALLS_ON_DEST_USER_MISMATCH
        = "net.java.sip.communicator.impl.protocol.sip."
        +"FAIL_CALLS_ON_DEST_USER_MISMATCH";

    /**
     * Creates a new instance and adds itself as an <tt>INVITE</tt> method
     * handler in the creating protocolProvider.
     *
     * @param protocolProvider a reference to the
     * <tt>ProtocolProviderServiceSipImpl</tt> instance that created us.
     */
    public OperationSetBasicTelephonySipImpl(
                ProtocolProviderServiceSipImpl protocolProvider)
    {

        this.protocolProvider = protocolProvider;

        protocolProvider.registerMethodProcessor(Request.INVITE, this);
        protocolProvider.registerMethodProcessor(Request.CANCEL, this);
        protocolProvider.registerMethodProcessor(Request.ACK, this);
        protocolProvider.registerMethodProcessor(Request.BYE, this);
    }

    /**
     * Registers <tt>listener</tt> with this provider so that it
     * could be notified when incoming calls are received.
     *
     * @param listener the listener to register with this provider.
     */
    public void addCallListener(CallListener listener)
    {
        synchronized(callListeners)
        {
            if (!callListeners.contains(listener))
                callListeners.add(listener);
        }
    }

    /**
     * Create a new call and invite the specified CallParticipant to it.
     *
     * @param callee the sip address of the callee that we should invite to a
     * new call.
     * @return CallParticipant the CallParticipant that will represented by
     *   the specified uri. All following state change events will be
     *   delivered through that call participant. The Call that this
     *   participant is a member of could be retrieved from the
     *   CallParticipatn instance with the use of the corresponding method.
     * @throws OperationFailedException with the corresponding code if we fail
     * to create the call.
     * @throws ParseException if <tt>callee</tt> is not a valid sip address
     * string.
     */
    public Call createCall(String callee)
        throws OperationFailedException, ParseException
    {
        Address toAddress = parseAddressStr(callee);

        return createOutgoingCall(toAddress);
    }

    /**
     * Create a new call and invite the specified CallParticipant to it.
     *
     * @param callee the address of the callee that we should invite to a
     *   new call.
     * @return CallParticipant the CallParticipant that will represented by
     *   the specified uri. All following state change events will be
     *   delivered through that call participant. The Call that this
     *   participant is a member of could be retrieved from the
     *   CallParticipatn instance with the use of the corresponding method.
     * @throws OperationFailedException with the corresponding code if we fail
     * to create the call.
     */
    public Call createCall(Contact callee)
        throws OperationFailedException
    {
        Address toAddress = null;

        try
        {
            toAddress = parseAddressStr(callee.getAddress());
        }
        catch (ParseException ex)
        {
            //couldn't happen
            logger.error(ex.getMessage(), ex);
            throw new IllegalArgumentException(ex.getMessage());
        }

        return createOutgoingCall(toAddress);
    }


    /**
     * Init and establish the specified call.
     *
     * @param calleeAddress the address of the callee that we'd like to connect
     * with.
     *
     * @return CallParticipant the CallParticipant that will represented by
     *   the specified uri. All following state change events will be
     *   delivered through that call participant. The Call that this
     *   participant is a member of could be retrieved from the
     *   CallParticipatn instance with the use of the corresponding method.
     *
     * @throws OperationFailedException with the corresponding code if we fail
     * to create the call.
     */
    private CallSipImpl createOutgoingCall(Address calleeAddress)
        throws OperationFailedException
    {
        //create the invite request
        Request invite = createInviteRequest(calleeAddress);

        //Content
        ContentTypeHeader contentTypeHeader = null;
        try
        {
            //content type should be application/sdp (not applications)
            //reported by Oleg Shevchenko (Miratech)
            contentTypeHeader =
                protocolProvider.getHeaderFactory().createContentTypeHeader(
                    "application", "sdp");
        }
        catch (ParseException ex)
        {
            //Shouldn't happen
            logger.error(
                "Failed to create a content type header for the INVITE "
                + "request"
                , ex);
            throw new OperationFailedException(
                "Failed to create a content type header for the INVITE "
                + "request"
                , OperationFailedException.INTERNAL_ERROR
                , ex);
        }

        //Transaction
        ClientTransaction inviteTransaction;
        SipProvider jainSipProvider
            = protocolProvider.getDefaultJainSipProvider();
        try
        {
            inviteTransaction = jainSipProvider.getNewClientTransaction(invite);
        }
        catch (TransactionUnavailableException ex)
        {
            logger.error(
                "Failed to create inviteTransaction.\n"
                + "This is most probably a network connection error."
                , ex);
            throw new OperationFailedException(
                "Failed to create inviteTransaction.\n"
                + "This is most probably a network connection error."
                , OperationFailedException.INTERNAL_ERROR
                , ex);
        }

        //create the call participant
        CallParticipantSipImpl callParticipant
            = createCallParticipantFor(inviteTransaction, jainSipProvider);


        //invite content
        try
        {
            CallSession callSession = SipActivator.getMediaService()
                .createCallSession(callParticipant.getCall());
            ((CallSipImpl)callParticipant.getCall())
                .setMediaCallSession(callSession);

            //if possible try to indicate the address of the callee so
            //that the media service can choose the most proper local
            //address to advertise.
            javax.sip.address.URI calleeURI = calleeAddress.getURI();
            InetAddress intendedDestination = null;
            if(calleeURI.isSipURI())
            {
                String host = ((SipURI)calleeURI).getHost();

                try
                {
                    intendedDestination = InetAddress.getByName(host);
                    invite.setContent(
                        callSession.createSdpOffer(intendedDestination)
                        , contentTypeHeader);
                }
                catch (UnknownHostException ex)
                {
                    logger.warn("Failed to obtain an InetAddress for "
                                + host
                                , ex);
                }
            }

            invite.setContent(
                callSession.createSdpOffer(intendedDestination)
                , contentTypeHeader);
        }
        catch (ParseException ex)
        {
            logger.error(
                "Failed to parse sdp data while creating invite request!"
                , ex);
            throw new OperationFailedException(
                "Failed to parse sdp data while creating invite request!"
                , OperationFailedException.INTERNAL_ERROR
                , ex);
        }
        catch (MediaException ex)
        {
            logger.error(
                "Failed to parse sdp data while creating invite request!"
                , ex);
            throw new OperationFailedException(
                "Failed to parse sdp data while creating invite request!"
                , OperationFailedException.INTERNAL_ERROR
                , ex);
        }



        try
        {
            inviteTransaction.sendRequest();
            if (logger.isDebugEnabled())
                logger.debug("sent request: " + invite);
        }
        catch (SipException ex)
        {
            logger.error(
                "An error occurred while sending invite request", ex);
            throw new OperationFailedException(
                "An error occurred while sending invite request"
                , OperationFailedException.NETWORK_FAILURE
                , ex);
        }

        return (CallSipImpl)callParticipant.getCall();
    }

    /**
     * Creates and dispatches a <tt>CallEvent</tt> notifying registered
     * listeners that an event with id <tt>eventID</tt> has occurred on
     * <tt>sourceCall</tt>.
     *
     * @param eventID the ID of the event to dispatch
     * @param sourceCall the call on which the event has occurred.
     */
    protected void fireCallEvent( int         eventID,
                                  CallSipImpl sourceCall)
    {
        CallEvent cEvent = new CallEvent(sourceCall, eventID);

        logger.debug("Dispatching a CallEvent to "
                     + callListeners.size()
                     +" listeners. event is: " + cEvent.toString());

        Iterator listeners = null;
        synchronized(callListeners)
        {
            listeners = new ArrayList(callListeners).iterator();
        }

        while(listeners.hasNext())
        {
            CallListener listener = (CallListener)listeners.next();

            if(eventID == CallEvent.CALL_INITIATED)
                listener.outgoingCallCreated(cEvent);
            else if(eventID == CallEvent.CALL_RECEIVED)
                listener.incomingCallReceived(cEvent);
            else if(eventID == CallEvent.CALL_ENDED)
                listener.callEnded(cEvent);
        }
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
     */
    public void putOffHold(CallParticipant participant)
    {
        /** @todo implement putOffHold() */
    }

    /**
     * Puts the specified CallParticipant "on hold".
     *
     * @param participant the participant that we'd like to put on hold.
     */
    public void putOnHold(CallParticipant participant)
    {
        /** @todo implement putOnHold() */
    }

    /**
     * Removes the <tt>listener</tt> from the list of call listeners.
     *
     * @param listener the listener to unregister.
     */
    public void removeCallListener(CallListener listener)
    {
        synchronized(callListeners)
        {
            callListeners.remove(listener);
        }
    }

    /**
     * Processes a Request received on a SipProvider upon which this SipListener
     * is registered.
     * <p>
     *
     * @param requestEvent requestEvent fired from the SipProvider to the
     * <tt>SipListener</tt> representing a Request received from the network.
     */
    public void processRequest(RequestEvent requestEvent)
    {
        ServerTransaction serverTransaction = requestEvent
            .getServerTransaction();
        SipProvider jainSipProvider = (SipProvider)requestEvent.getSource();
        Request request = requestEvent.getRequest();

        if (serverTransaction == null)
        {
            try
            {
                serverTransaction = jainSipProvider.getNewServerTransaction(
                    request);
            }
            catch (TransactionAlreadyExistsException ex)
            {
                //let's not scare the user and only log a message
                logger.error("Failed to create a new server"
                    + "transaction for an incoming request\n"
                    + "(Next message contains the request)"
                    , ex);
                return;
            }
            catch (TransactionUnavailableException ex)
            {
                //let's not scare the user and only log a message
                logger.error("Failed to create a new server"
                    + "transaction for an incoming request\n"
                    + "(Next message contains the request)"
                    , ex);
                    return;
            }
        }

        //INVITE
        if (request.getMethod().equals(Request.INVITE))
        {
            logger.debug("received INVITE");
            if (serverTransaction.getDialog().getState() == null)
            {
                if (logger.isDebugEnabled())
                    logger.debug("request is an INVITE. Dialog state="
                                 + serverTransaction.getDialog().getState());
                processInvite(jainSipProvider, serverTransaction, request);
            }
            else
            {
                logger.error("reINVITE-s are not currently supported.");
            }
        }
        //ACK
        else if (request.getMethod().equals(Request.ACK))
        {
            processAck(serverTransaction, request);
        }
        //BYE
        else if (request.getMethod().equals(Request.BYE))
        {
            processBye(serverTransaction, request);
        }
        //CANCEL
        else if (request.getMethod().equals(Request.CANCEL))
        {
            processCancel(serverTransaction, request);
        }
    }

    /**
     * Process an asynchronously reported TransactionTerminatedEvent.
     *
     * @param transactionTerminatedEvent -- an event that indicates that the
     * transaction has transitioned into the terminated state.
     */
    public void processTransactionTerminated(
                    TransactionTerminatedEvent transactionTerminatedEvent)
    {
        //nothing to do here.
    }

    /**
     * Analyzes the incoming <tt>responseEvent</tt> and then forwards it to the
     * proper event handler.
     *
     * @param responseEvent the responseEvent that we received
     * ProtocolProviderService.
     */
    public void processResponse(ResponseEvent responseEvent)
    {
        ClientTransaction clientTransaction = responseEvent
            .getClientTransaction();

        Response response = responseEvent.getResponse();

        CSeqHeader cseq = ((CSeqHeader)response.getHeader(CSeqHeader.NAME));

        if (cseq == null)
        {
            logger.error("An incoming response did not contain a CSeq header");
        }

        String method = cseq.getMethod();

        SipProvider sourceProvider = (SipProvider)responseEvent.getSource();

        //OK
        if (response.getStatusCode() == Response.OK)
        {
            if(method.equals(Request.INVITE))
            {
                processInviteOK(clientTransaction, response);
            }
            else if (method.equals(Request.BYE))
            {
                //ignore
            }
        }
        //Ringing
        else if (response.getStatusCode() == Response.RINGING)
        {
            processRinging(clientTransaction, response);
        }
        //Trying
        else if (response.getStatusCode() == Response.TRYING)
        {
            processTrying(clientTransaction, response);
        }
        //Busy here.
        else if (response.getStatusCode() == Response.BUSY_HERE)
        {
            processBusyHere(clientTransaction, response);
        }

        //401 UNAUTHORIZED
        else if (response.getStatusCode() == Response.UNAUTHORIZED
                 || response.getStatusCode()
                                == Response.PROXY_AUTHENTICATION_REQUIRED)
        {
            processAuthenticationChallenge(clientTransaction
                                           , response
                                           , sourceProvider);
        }
        //errors
        else if ( response.getStatusCode() / 100 == 4 ||
            response.getStatusCode() / 100 == 5)
        {
            CallParticipantSipImpl callParticipant = activeCallsRepository
                .findCallParticipant(clientTransaction.getDialog());

            logger.error("Received error: " +response.getStatusCode()
                         +" "+ response.getReasonPhrase());

            if(callParticipant != null)
                callParticipant.setState(CallParticipantState.FAILED);
        }
        //ignore everything else.
    }

    /**
     * Updates the call state of the corresponding call participant.
     *
     * @param clientTransaction the transaction in which the response was
     * received.
     * @param response the Ttrying response.
     */
    private void processTrying(ClientTransaction clientTransaction,
                               Response          response)
    {
        Dialog dialog = clientTransaction.getDialog();
        //find the call participant
        CallParticipantSipImpl callParticipant
            = activeCallsRepository.findCallParticipant(dialog);

        if (callParticipant == null)
        {
            logger.debug("Received a stray trying response.");
            return;
        }

        //change status
        callParticipant.setState(CallParticipantState.CONNECTING);
    }

    /**
     * Updates the call state of the corresponding call participant. We'll
     * also try to extract any details here that might be of use for call
     * participant presentation and that we didn't have when establishing the
     * call.
     *
     * @param clientTransaction the transaction in which the response was
     * received.
     * @param response the Ttrying response.
     */
    private void processRinging(ClientTransaction clientTransaction,
                                Response          response)
    {
        Dialog dialog = clientTransaction.getDialog();
        //find the call participant
        CallParticipantSipImpl callParticipant
            = activeCallsRepository.findCallParticipant(dialog);

        if (callParticipant == null)
        {
            logger.debug("Received a stray trying response.");
            return;
        }

        //try to update the display name.
        ContactHeader remotePartyContactHeader
            = (ContactHeader)response.getHeader(ContactHeader.NAME);

        if(remotePartyContactHeader != null)
        {
            Address remotePartyAddress = remotePartyContactHeader.getAddress();

            String displayName = remotePartyAddress.getDisplayName();

            if(displayName != null && displayName.trim().length() > 0)
            {
                callParticipant.setDisplayName(displayName);
            }
        }

        //change status.
        callParticipant.setState(CallParticipantState.ALERTING_REMOTE_SIDE);
    }

    /**
     * Sets to CONNECTED that state of the corresponding call participant and
     * sends an ACK.
     * @param clientTransaction the <tt>ClientTransaction</tt> that the response
     * arrived in.
     * @param ok the OK <tt>Response</tt> to process
     */
    private void processInviteOK(ClientTransaction clientTransaction,
                                 Response          ok)
    {
        Dialog dialog = clientTransaction.getDialog();
        //find the call
        CallParticipantSipImpl callParticipant
            = activeCallsRepository.findCallParticipant(dialog);

        if (callParticipant == null)
        {
            logger.debug("Received a stray ok response.");
            return;
        }

        Request ack = null;
        ContentTypeHeader contentTypeHeader = null;
        //Create ACK
        try
        {
            //Need to use dialog generated ACKs so that the remote UA core
            //sees them - Fixed by M.Ranganathan
            ack = clientTransaction.getDialog().createRequest(Request.ACK);

            //Content should it be necessary.

            //content type should be application/sdp (not applications)
            //reported by Oleg Shevchenko (Miratech)
            contentTypeHeader =
                protocolProvider.getHeaderFactory().createContentTypeHeader(
                "application", "sdp");
        }
        catch (ParseException ex)
        {
            //Shouldn't happen
            callParticipant.setState(CallParticipantState.FAILED
                , "Failed to create a content type header for the ACK request");
            logger.error(
                "Failed to create a content type header for the ACK request"
                , ex);
        }
        catch (SipException ex)
        {
            logger.error("Failed to create ACK request!", ex);
            callParticipant.setState(CallParticipantState.FAILED);
            return;
        }

        //!!! set sdp content before setting call state as that is where
       //listeners get alerted and they need the sdp
       callParticipant.setSdpDescription(new String(ok.getRawContent()));

        //notify the media manager of the sdp content
        CallSession callSession
            = ((CallSipImpl)callParticipant.getCall()).getMediaCallSession();

        try
        {
            if(callSession == null)
            {
                //non existent call session - that means we didn't send sdp in
                //the invide and this is the offer so we need to create the
                //answer.
                callSession = SipActivator.getMediaService()
                    .createCallSession(callParticipant.getCall());
                String sdp = callSession.processSdpOffer(
                                      callParticipant
                                    , callParticipant.getSdpDescription());
                ack.setContent(sdp, contentTypeHeader);

            }
            callSession.processSdpAnswer(callParticipant
                                         , callParticipant.getSdpDescription());
        }
        catch (ParseException exc)
        {
            logger.error("There was an error parsing the SDP description of "
                         + callParticipant.getDisplayName()
                         + "(" + callParticipant.getAddress() + ")"
                        , exc);
            callParticipant.setState(CallParticipantState.FAILED
                , "There was an error parsing the SDP description of "
                + callParticipant.getDisplayName()
                + "(" + callParticipant.getAddress() + ")");
        }
        catch (MediaException exc)
        {
            logger.error("We failed to process the SDP description of "
                         + callParticipant.getDisplayName()
                         + "(" + callParticipant.getAddress() + ")"
                         + ". Error was: "
                         + exc.getMessage()
                        , exc);
            callParticipant.setState(CallParticipantState.FAILED
                , "We failed to process the SDP description of "
                + callParticipant.getDisplayName()
                + "(" + callParticipant.getAddress() + ")"
                + ". Error was: "
                + exc.getMessage());
        }

        //send ack
        try{
            clientTransaction.getDialog().sendAck(ack);
        }
        catch (SipException ex)
        {
            logger.error("Failed to acknowledge call!", ex);
            callParticipant.setState(CallParticipantState.FAILED);
            return;
        }


        //change status
        callParticipant.setState(CallParticipantState.CONNECTED);
    }

    /**
     * Sets corresponding state to the call participant associated with this
     * transaction.
     * @param clientTransaction the transaction in which
     * @param busyHere the busy here Response
     */
    private void processBusyHere(ClientTransaction clientTransaction,
                                 Response          busyHere)
    {
        Dialog dialog = clientTransaction.getDialog();
        //find the call
        CallParticipantSipImpl callParticipant
            = activeCallsRepository.findCallParticipant(dialog);

        if (callParticipant == null)
        {
            logger.debug("Received a stray busyHere response.");
            return;
        }

        //change status
        callParticipant.setState(CallParticipantState.BUSY);
    }

    /**
     * Attempts to re-ogenerate the corresponding request with the proper
     * credentials and terminates the call if it fails.
     *
     * @param clientTransaction the corresponding transaction
     * @param response the challenge
     * @param jainSipProvider the provider that received the challende
     */
    private void processAuthenticationChallenge(
        ClientTransaction clientTransaction,
        Response response,
        SipProvider jainSipProvider)
    {
        //First find the call and the call participant that this authentication
        //request concerns.
        CallParticipantSipImpl callParticipant = activeCallsRepository
            .findCallParticipant(clientTransaction.getDialog());

        if (callParticipant == null) {
            logger.debug("Received an authorization challenge for no "
                         +"participant. authorizing anyway.");
        }

        try
        {
            logger.debug("Authenticating an INVITE request.");

            ClientTransaction retryTran
                = protocolProvider.getSipSecurityManager().handleChallenge(
                    response
                    , clientTransaction
                    , jainSipProvider);

            //There is a new dialog that will be started with this request. Get
            //that dialog and record it into the Call objet for later use (by
            //Bye-s for example).
            //if the request was BYE then we need to authorize it anyway even
            //if the call and the call participant are no longer there
            if(callParticipant !=null)
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

            //tell the others we couldn't register
            callParticipant.setState(CallParticipantState.FAILED);
        }
    }

    /**
     * Processes a retransmit or expiration Timeout of an underlying
     * {@link Transaction}handled by this SipListener. This Event notifies the
     * application that a retransmission or transaction Timer expired in the
     * SipProvider's transaction state machine. The TimeoutEvent encapsulates
     * the specific timeout type and the transaction identifier either client or
     * server upon which the timeout occured. The type of Timeout can by
     * determined by:
     * <code>timeoutType = timeoutEvent.getTimeout().getValue();</code>
     *
     * @param timeoutEvent the timeoutEvent received indicating either the
     * message retransmit or transaction timed out.
     */
    public void processTimeout(TimeoutEvent timeoutEvent)
    {
        Transaction transaction;
        if (timeoutEvent.isServerTransaction()) {
            //don't care. or maybe a stack bug?
            return;
        }
        else {
            transaction = timeoutEvent.getClientTransaction();
        }

        CallParticipantSipImpl callParticipant = activeCallsRepository
            .findCallParticipant(transaction.getDialog());

        if (callParticipant == null) {
            logger.debug("Got a headless timeout event." + timeoutEvent);
            return;
        }

        //change status
        callParticipant.setState(CallParticipantState.FAILED
                                , "The remote party has not replied!"
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
     * application.
     */
    public void processIOException(IOExceptionEvent exceptionEvent)
    {
        logger.error("Got an asynchronous exception event. host="
            + exceptionEvent.getHost() + " port=" + exceptionEvent.getPort());
    }

    /**
     * Process an asynchronously reported DialogTerminatedEvent.
     *
     * @param dialogTerminatedEvent -- an event that indicates that the
     *       dialog has transitioned into the terminated state.
     */
    public void processDialogTerminated(DialogTerminatedEvent
                                        dialogTerminatedEvent)
    {
        CallParticipantSipImpl callParticipant = activeCallsRepository
            .findCallParticipant(dialogTerminatedEvent.getDialog());

        if (callParticipant == null)
        {
            return;
        }

        //change status
        callParticipant.setState(CallParticipantState.DISCONNECTED);

    }

    /**
     * Parses the the <tt>uriStr</tt> string and returns a JAIN SIP URI.
     *
     * @param uriStr a <tt>String</tt> containing the uri to parse.
     *
     * @return a URI object corresponding to the <tt>uriStr</tt> string.
     * @throws ParseException if uriStr is not properly formatted.
     */
    private Address parseAddressStr(String uriStr)
        throws ParseException
    {
        uriStr = uriStr.trim();

        //Handle default domain name (i.e. transform 1234 -> 1234@sip.com)
        //assuming that if no domain name is specified then it should be the
        //same as ours.
        if (uriStr.indexOf('@') == -1
            && !uriStr.trim().startsWith("tel:"))
        {
            uriStr = uriStr + "@"
                + ((SipURI)protocolProvider.getOurSipAddress().getURI())
                    .getHost();
        }

        //Let's be uri fault tolerant and add the sip: scheme if there is none.
        if (uriStr.toLowerCase().indexOf("sip:") == -1 //no sip scheme
            && uriStr.indexOf('@') != -1) //most probably a sip uri
        {
            uriStr = "sip:" + uriStr;
        }

        //Request URI
        Address uri
            = protocolProvider.getAddressFactory().createAddress(uriStr);

        return uri;
    }

    /**
     * Creates an invite request destined for <tt>callee</tt>.
     *
     * @param toAddress the sip address of the callee that the request is meant
     * for.
     * @return a newly created sip <tt>Request</tt> destined for
     * <tt>callee</tt>.
     * @throws OperationFailedException with the correspoding code if creating
     * the request fails.
     */
    private Request createInviteRequest(Address toAddress)
        throws OperationFailedException
    {
        InetAddress destinationInetAddress = null;
        try
        {
            destinationInetAddress = InetAddress.getByName(
                ( (SipURI) toAddress.getURI()).getHost());
        }
        catch (UnknownHostException ex)
        {
            throw new IllegalArgumentException(
                ( (SipURI) toAddress.getURI()).getHost()
                + " is not a valid internet address " + ex.getMessage());
        }
        //Call ID
        CallIdHeader callIdHeader = protocolProvider
                                .getDefaultJainSipProvider().getNewCallId();

        //CSeq
        CSeqHeader cSeqHeader = null;
        try
        {
            cSeqHeader = protocolProvider.getHeaderFactory()
                .createCSeqHeader(1l, Request.INVITE);
        }
        catch (InvalidArgumentException ex)
        {
            //Shouldn't happen
            logger.error(
                "An unexpected erro occurred while"
                + "constructing the CSeqHeadder", ex);
            throw new OperationFailedException(
                "An unexpected erro occurred while"
                + "constructing the CSeqHeadder"
                , OperationFailedException.INTERNAL_ERROR
                , ex);
        }
        catch(ParseException exc)
        {
            //shouldn't happen
            logger.error(
                "An unexpected erro occurred while"
                + "constructing the CSeqHeadder", exc);
            throw new OperationFailedException(
                "An unexpected erro occurred while"
                + "constructing the CSeqHeadder"
                , OperationFailedException.INTERNAL_ERROR
                , exc);
        }

        //FromHeader
        String localTag = protocolProvider.generateLocalTag();
        FromHeader fromHeader = null;
        ToHeader   toHeader = null;
        try
        {
            //FromHeader
            fromHeader = protocolProvider.getHeaderFactory()
                .createFromHeader(protocolProvider.getOurSipAddress()
                                  , localTag);

            //ToHeader
            toHeader = protocolProvider.getHeaderFactory()
                .createToHeader(toAddress, null);
        }
        catch (ParseException ex)
        {
            //these two should never happen.
            logger.error(
                "An unexpected erro occurred while"
                + "constructing the ToHeader", ex);
            throw new OperationFailedException(
                "An unexpected erro occurred while"
                + "constructing the ToHeader"
                , OperationFailedException.INTERNAL_ERROR
                , ex);
        }

        //ViaHeaders
        ArrayList viaHeaders = protocolProvider.getLocalViaHeaders(
            destinationInetAddress
            , protocolProvider.getDefaultListeningPoint());

        //MaxForwards
        MaxForwardsHeader maxForwards = protocolProvider
            .getMaxForwardsHeader();

        //Contact
        ContactHeader contactHeader = protocolProvider.getContactHeader();

        Request invite = null;
        try
        {
            invite = protocolProvider.getMessageFactory().createRequest(
                toHeader.getAddress().getURI()
                , Request.INVITE
                , callIdHeader
                , cSeqHeader
                , fromHeader
                , toHeader
                , viaHeaders
                , maxForwards);

        }
        catch (ParseException ex)
        {
            //shouldn't happen
            logger.error(
                "Failed to create invite Request!", ex);
            throw new OperationFailedException(
                "Failed to create invite Request!"
                , OperationFailedException.INTERNAL_ERROR
                , ex);
        }

        //User Agent
        UserAgentHeader userAgentHeader
            = protocolProvider.getSipCommUserAgentHeader();
        if(userAgentHeader != null)
            invite.addHeader(userAgentHeader);


        //add the contact header.
        invite.addHeader(contactHeader);

        return invite;
    }

    /**
     * Creates a new call and sends a RINGING response.
     *
     * @param sourceProvider the provider containin <tt>sourceTransaction</tt>.
     * @param serverTransaction the transaction containing the received request.
     * @param invite the Request that we've just received.
     */
    private void processInvite( SipProvider       sourceProvider,
                                ServerTransaction serverTransaction,
                                Request           invite)
    {
        logger.trace("Creating call participant.");
        Dialog dialog = serverTransaction.getDialog();
        CallParticipantSipImpl callParticipant
            = createCallParticipantFor(serverTransaction, sourceProvider);
        logger.trace("call participant created = " + callParticipant);


        //sdp description may be in acks - bug report Laurent Michel
        ContentLengthHeader cl = invite.getContentLength();
        if (cl != null
            && cl.getContentLength() > 0)
        {
            callParticipant.setSdpDescription(
                                        new String(invite.getRawContent()));
        }

        logger.trace("Will verify whether INVITE is properly addressed.");
        //Are we the one they are looking for?
        javax.sip.address.URI calleeURI = dialog.getLocalParty().getURI();

        if (calleeURI.isSipURI())
        {
            boolean assertUserMatch = Boolean.valueOf(
                SipActivator.getConfigurationService().getString(
                    FAIL_CALLS_ON_DEST_USER_MISMATCH)).booleanValue();

            if (assertUserMatch)
            {
                //user info is case sensitive according to rfc3261
                String calleeUser = ( (SipURI) calleeURI).getUser();
                String localUser = ((SipURI)protocolProvider.getOurSipAddress()
                                    .getURI()).getUser();

                if (calleeUser != null && !calleeUser.equals(localUser))
                {
                    callParticipant.setState(
                        CallParticipantState.FAILED
                        , "A call was received here while it appeared "
                        +"destined to someone else. The call was rejected.");

                    Response notFound = null;
                    try {
                        notFound = protocolProvider.getMessageFactory()
                            .createResponse( Response.NOT_FOUND, invite);

                        //attach a to tag
                        protocolProvider.attachToTag(notFound, dialog);
                        notFound.setHeader(
                            protocolProvider.getSipCommUserAgentHeader());
                    }
                    catch (ParseException ex) {
                        logger.error("Error while trying to create a response"
                                     , ex);
                        callParticipant.setState(
                            CallParticipantState.FAILED
                            , "InernalError: " +ex.getMessage());
                        return;
                    }
                    try {
                        serverTransaction.sendResponse(notFound);
                        logger.debug("sent a not found response: " + notFound);
                    }
                    catch (Exception ex) {
                        logger.error("Error while trying to send a response"
                                     , ex);
                        callParticipant.setState(
                            CallParticipantState.FAILED
                            , "Internal Error: " + ex.getMessage());
                        return;
                    }
                    return;
                }
            }
        }

        //Send RINGING
        logger.debug("Invite seems ok, we'll say RINGING.");
        Response ringing = null;
        try {
            ringing = protocolProvider.getMessageFactory().createResponse(
                Response.RINGING, invite);
            protocolProvider.attachToTag(ringing, dialog);
            ringing.setHeader(protocolProvider.getSipCommUserAgentHeader());

            //set our display name
            ((ToHeader)ringing.getHeader(ToHeader.NAME))
                .getAddress().setDisplayName(protocolProvider
                                                .getOurDisplayName());

            ringing.addHeader(protocolProvider.getContactHeader());
        }
        catch (ParseException ex) {
            logger.error("Error while trying to send a request"
                                     , ex);
            callParticipant.setState(CallParticipantState.FAILED
                                    ,  "Internal Error: " + ex.getMessage());
            return;
        }
        try {
            logger.trace("will send ringing response: ");
            serverTransaction.sendResponse(ringing);
            logger.debug("sent a ringing response: " + ringing);
        }
        catch (Exception ex) {
            logger.error("Error while trying to send a request"
                                     , ex);
            callParticipant.setState(
                            CallParticipantState.FAILED
                            , "Internal Error: " + ex.getMessage());
            return;
        }
    }

    /**
     * Sets the state of the corresponding call participant to DISCONNECTED
     * and sends an OK response.
     *
     * @param serverTransaction the ServerTransaction the the BYE request
     * arrived in.
     * @param byeRequest the BYE request to process
     */
    private void processBye(ServerTransaction serverTransaction,
                            Request byeRequest)
    {
        //find the call
        CallParticipantSipImpl callParticipant = activeCallsRepository
            .findCallParticipant( serverTransaction.getDialog());

        if (callParticipant == null) {
            logger.debug("Received a stray bye request.");
            return;
        }

        //Send OK
        Response ok = null;
        try {
            ok = protocolProvider.getMessageFactory()
                .createResponse(Response.OK, byeRequest);
            protocolProvider.attachToTag(ok, serverTransaction.getDialog());
            ok.setHeader(protocolProvider.getSipCommUserAgentHeader());
        }
        catch (ParseException ex) {
            logger.error("Error while trying to send a response to a bye", ex);
            //no need to let the user know about the error since it doesn't
            //affect them
            return;
        }

        try {
            serverTransaction.sendResponse(ok);
            logger.debug("sent response " + ok);
        }
        catch (Exception ex) {
            //This is not really a problem according to the RFC
            //so just dump to stdout should someone be interested
            logger.error("Failed to send an OK response to BYE request,"
                          + "exception was:\n",
                          ex);
        }

        //change status
        callParticipant.setState(CallParticipantState.DISCONNECTED);

    }

    /**
     * Updates the sesion description and sends the state of the corresponding
     * call participant to CONNECTED.
     *
     * @param serverTransaction the transaction that the Ack was received in.
     * @param ackRequest Request
     */
    void processAck(ServerTransaction serverTransaction,
                    Request ackRequest)
    {
        //find the call
        CallParticipantSipImpl callParticipant = activeCallsRepository
            .findCallParticipant(serverTransaction.getDialog());

        if (callParticipant == null) {
            //this is most probably the ack for a killed call - don't signal it
            logger.debug("didn't find an ack's call, returning");
            return;
        }

        ContentLengthHeader cl = ackRequest.getContentLength();
        if (cl != null
            && cl.getContentLength() > 0)
        {
            callParticipant.setSdpDescription(
                                    new String(ackRequest.getRawContent()));
        }
        //change status
        callParticipant.setState(CallParticipantState.CONNECTED);
    }

    /**
     * Sets the state of the specifies call participant as DISCONNECTED.
     *
     * @param serverTransaction the transaction that the cancel was received in.
     * @param cancelRequest the Request that we've just received.
     */
    void processCancel(ServerTransaction serverTransaction,
                       Request           cancelRequest)
    {
        //find the call
        CallParticipantSipImpl callParticipant = activeCallsRepository
            .findCallParticipant( serverTransaction.getDialog() );

        if (callParticipant == null) {
            logger.debug("received a stray CANCEL req. ignoring");
            return;
        }

        // Cancels should be OK-ed and the initial transaction - terminated
        // (report and fix by Ranga)
        try {
            Response ok = protocolProvider.getMessageFactory().createResponse(
                Response.OK, cancelRequest);

            protocolProvider.attachToTag(ok, serverTransaction.getDialog());
            ok.setHeader(protocolProvider.getSipCommUserAgentHeader());
            serverTransaction.sendResponse(ok);

            logger.debug("sent an ok response to a CANCEL request:\n" + ok);
        }
        catch (ParseException ex) {
            logger.error(
                "Failed to create an OK Response to an CANCEL request.", ex);
            callParticipant.setState(CallParticipantState.FAILED
                ,"Failed to create an OK Response to an CANCEL request.");
        }
        catch (Exception ex) {
            logger.error(
                "Failed to send an OK Response to an CANCEL request.", ex);
            callParticipant.setState(CallParticipantState.FAILED
                ,"Failed to send an OK Response to an CANCEL request.");
        }
        try {
            //stop the invite transaction as well
            Transaction tran = callParticipant.getFirstTransaction();
            //should be server transaction and misplaced cancels should be
            //filtered by the stack but it doesn't hurt checking anyway
            if (! (tran instanceof ServerTransaction)) {
                logger.error("Received a misplaced CANCEL request!");
                return;
            }

            ServerTransaction inviteTran = (ServerTransaction) tran;
            Request invite = callParticipant.getFirstTransaction().getRequest();
            Response requestTerminated =
                protocolProvider.getMessageFactory()
                    .createResponse(Response.REQUEST_TERMINATED, invite);
            requestTerminated.setHeader(
                            protocolProvider.getSipCommUserAgentHeader());
            protocolProvider.attachToTag(requestTerminated
                                         , callParticipant.getDialog());
            inviteTran.sendResponse(requestTerminated);
            if( logger.isDebugEnabled() )
                logger.debug("sent request terminated response:\n"
                              + requestTerminated);
        }
        catch (ParseException ex)
        {
            logger.error("Failed to create a REQUEST_TERMINATED Response to "
                         + "an INVITE request."
                         , ex);
        }
        catch (Exception ex)
        {
            logger.error("Failed to send an REQUEST_TERMINATED Response to "
                         + "an INVITE request."
                         , ex);
        }

        //change status
        callParticipant.setState(CallParticipantState.DISCONNECTED);
    }

    /**
     * Indicates a user request to end a call with the specified call
     * particiapnt. Depending on the state of the call the method would send a
     * CANCEL, BYE, or BUSY_HERE and set the new state to DISCONNECTED.
     *
     * @param participant the participant that we'd like to hang up on.
     * @throws ClassCastException if participant is not an instance of this
     * CallParticipantSipImpl.
     * @throws OperationFailedException if we fail to terminate the call.
     */
    public void hangupCallParticipant(CallParticipant participant)
        throws ClassCastException, OperationFailedException
    {
        //do nothing if the call is already ended
        if (participant.getState().equals(CallParticipantState.DISCONNECTED)
            || participant.getState().equals(CallParticipantState.FAILED))
        {
            logger.debug("Ignoring a request to hangup a call participant "
                         +"that is already DISCONNECTED");
            return;
        }

        CallParticipantSipImpl callParticipant
            = (CallParticipantSipImpl)participant;

        Dialog dialog = callParticipant.getDialog();
        if (callParticipant.getState().equals(CallParticipantState.CONNECTED))
        {
            sayBye(callParticipant);
            callParticipant.setState(CallParticipantState.DISCONNECTED);
        }
        else if (callParticipant.getState()
                            .equals(CallParticipantState.CONNECTING)
                 || callParticipant.getState()
                            .equals(CallParticipantState.ALERTING_REMOTE_SIDE))
        {
            if (callParticipant.getFirstTransaction() != null)
            {
                //Someone knows about us. Let's be polite and say we are
                //leaving
                sayCancel(callParticipant);
            }
            callParticipant.setState(CallParticipantState.DISCONNECTED);
        }
        else if (callParticipant.getState()
                    .equals(CallParticipantState.INCOMING_CALL))
        {
            callParticipant.setState(CallParticipantState.DISCONNECTED);
            sayBusyHere(callParticipant);
        }
        //For FAILE and BUSY we only need to update CALL_STATUS
        else if (callParticipant.getState().equals(CallParticipantState.BUSY))
        {
            callParticipant.setState(CallParticipantState.DISCONNECTED);
        }
        else if (callParticipant.getState().equals(CallParticipantState.FAILED))
        {
            callParticipant.setState(CallParticipantState.DISCONNECTED);
        }
        else
        {
            callParticipant.setState(CallParticipantState.DISCONNECTED);
            logger.error("Could not determine call participant state!");
        }
    } //end call

    /**
     * Sends an Internal Error response to <tt>callParticipant</tt>.
     *
     * @param callParticipant the call participant that we need to say bye to.
     *
     * @throws OperationFailedException if we failed constructing or sending a
     * SIP Message.
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
     * SIP Message.
     */
    public void sayError(CallParticipantSipImpl callParticipant,
                                 int                    errorCode)
        throws OperationFailedException
    {
        Dialog dialog = callParticipant.getDialog();
        callParticipant.setState(CallParticipantState.FAILED);
        if (dialog == null)
        {
            logger.error(
                "Failed to extract participant's associated dialog! "
                +"Ending Call!");
            throw new OperationFailedException(
                "Failed to extract participant's associated dialog! "
                +"Ending Call!"
                ,OperationFailedException.INTERNAL_ERROR);
        }
        Transaction transaction = callParticipant.getFirstTransaction();
        if (transaction == null || !dialog.isServer())
        {
            logger.error(
                "Failed to extract a transaction"
                +" from the call's associated dialog!");
            throw new OperationFailedException(
                "Failed to extract a transaction from the participant's "
                +"associated dialog!"
                , OperationFailedException.INTERNAL_ERROR);
        }

        ServerTransaction serverTransaction = (ServerTransaction) transaction;
        Response internalError = null;
        try
        {
            internalError = protocolProvider.getMessageFactory()
                .createResponse(
                    errorCode
                    , callParticipant.getFirstTransaction().getRequest());
            protocolProvider.attachToTag(internalError, dialog);
        }
        catch (ParseException ex) {
            logger.error(
                "Failed to construct an OK response to an INVITE request"
                , ex);
            throw new OperationFailedException(
                "Failed to construct an OK response to an INVITE request"
                , OperationFailedException.INTERNAL_ERROR
                , ex);
        }
        ContactHeader contactHeader = protocolProvider.getContactHeader();
        internalError.addHeader(contactHeader);
        try {
            serverTransaction.sendResponse(internalError);
            if( logger.isDebugEnabled() )
                logger.debug("sent response: " + internalError);
        }
        catch (Exception ex) {
            logger.error(
                "Failed to send an OK response to an INVITE request"
                , ex);
            throw new OperationFailedException(
                "Failed to send an OK response to an INVITE request"
                , OperationFailedException.INTERNAL_ERROR
                , ex);
        }
    } //internal error


    /**
     * Sends a BYE request to <tt>callParticipant</tt>.
     *
     * @param callParticipant the call participant that we need to say bye to.
     * @since OperationFailedException if we fail to create an outgoing request
     * or send it.
     *
     * @throws OperationFailedException if we faile constructing or sending a
     * SIP Message.
     */
    private void sayBye(CallParticipantSipImpl callParticipant)
        throws OperationFailedException
    {
        Request request = callParticipant.getFirstTransaction().getRequest();
        Request bye = null;
        try
        {
            bye = callParticipant.getDialog().createRequest(Request.BYE);
        }
        catch (SipException ex)
        {
            logger.error("Failed to create bye request!", ex);
            throw new OperationFailedException(
                                "Failed to create bye request!"
                                , OperationFailedException.INTERNAL_ERROR
                                , ex);
        }
        ClientTransaction clientTransaction = null;
        try
        {
            clientTransaction = callParticipant.getJainSipProvider()
                .getNewClientTransaction(bye);
        }
        catch (TransactionUnavailableException ex)
        {
            logger.error(
                "Failed to construct a client transaction from the BYE request"
                , ex);
            throw new OperationFailedException(
                "Failed to construct a client transaction from the BYE request"
                , OperationFailedException.INTERNAL_ERROR
                ,ex);
        }

        try
        {
            callParticipant.getDialog().sendRequest(clientTransaction);
            logger.debug("sent request:\n" + bye);
        }
        catch (SipException ex)
        {
            throw new OperationFailedException(
                "Failed to send the BYE request"
                , OperationFailedException.NETWORK_FAILURE
                , ex);
        }
    } //bye

    /**
     * Sends a Cancel request to <tt>callParticipant</tt>.
     *
     * @param callParticipant the call participant that we need to cancel.
     *
     * @throws OperationFailedException we faile to construct or send the
     * CANCEL request.
     */
    private void sayCancel(CallParticipantSipImpl callParticipant)
        throws OperationFailedException
    {
        Request request = callParticipant.getFirstTransaction().getRequest();
        if (callParticipant.getDialog().isServer())
        {
            logger.error("Cannot cancel a server transaction");
            throw new OperationFailedException(
                "Cannot cancel a server transaction"
                , OperationFailedException.INTERNAL_ERROR);
        }

        ClientTransaction clientTransaction =
            (ClientTransaction) callParticipant.getFirstTransaction();
        try
        {
            Request cancel = clientTransaction.createCancel();
            ClientTransaction cancelTransaction = callParticipant
                .getJainSipProvider().getNewClientTransaction(cancel);
            cancelTransaction.sendRequest();
            logger.debug("sent request:\n" + cancel);
        }
        catch (SipException ex) {
            logger.error("Failed to send the CANCEL request", ex);
            throw new OperationFailedException(
                "Failed to send the CANCEL request"
                , OperationFailedException.NETWORK_FAILURE
                , ex);
        }
    } //cancel

    /**
     * Sends a BUSY_HERE response to <tt>callParticipant</tt>.
     *
     * @param callParticipant the call participant that we need to send busy
     * tone to.
     * @throws OperationFailedException if we fail to create or send the
     * response
     */
    private void sayBusyHere(CallParticipantSipImpl callParticipant)
        throws OperationFailedException
    {
        Request request = callParticipant.getFirstTransaction().getRequest();
        Response busyHere = null;
        try
        {
            busyHere = protocolProvider.getMessageFactory()
                .createResponse(Response.BUSY_HERE, request);
            busyHere.setHeader(
                            protocolProvider.getSipCommUserAgentHeader());
            protocolProvider.attachToTag(busyHere
                                         , callParticipant.getDialog());
        }
        catch (ParseException ex)
        {
            logger.error("Failed to create the BUSY_HERE response!", ex);
            throw new OperationFailedException(
                "Failed to create the BUSY_HERE response!"
                , OperationFailedException.INTERNAL_ERROR
                , ex);
        }

        if (!callParticipant.getDialog().isServer())
        {
            logger.error("Cannot send BUSY_HERE in a client transaction");
            throw new OperationFailedException(
                "Cannot send BUSY_HERE in a client transaction"
                , OperationFailedException.INTERNAL_ERROR);
        }
        ServerTransaction serverTransaction
            = (ServerTransaction) callParticipant.getFirstTransaction();

        try
        {
            serverTransaction.sendResponse(busyHere);
            logger.debug("sent response:\n" + busyHere);
        }
        catch (Exception ex)
        {
            logger.error("Failed to send the BUSY_HERE response", ex);
            throw new OperationFailedException(
                "Failed to send the BUSY_HERE response"
                , OperationFailedException.NETWORK_FAILURE
                , ex);
        }
    } //busy here

    /**
     * * Indicates a user request to answer an incoming call from the specified
     * CallParticipant.
     *
     * Sends an OK response to <tt>callParticipant</tt>. Make sure that the call
     * participant contains an sdp description when you call this method.
     *
     * @param participant the call participant that we need to send the ok
     * to.
     * @throws OperationFailedException if we fail to create or send the
     * response.
     */
    public void answerCallParticipant(CallParticipant participant)
        throws OperationFailedException
    {
        CallParticipantSipImpl callParticipant
            = (CallParticipantSipImpl)participant;
        Transaction transaction = callParticipant.getFirstTransaction();
        Dialog dialog = callParticipant.getDialog();

        if (transaction == null  || !dialog.isServer())
        {
            callParticipant.setState(CallParticipantState.DISCONNECTED);
            throw new OperationFailedException(
                "Failed to extract a ServerTransaction "
                + "from the call's associated dialog!"
                , OperationFailedException.INTERNAL_ERROR);
        }

        if(participant.getState().equals(CallParticipantState.CONNECTED))
        {
            logger.info("Ignoring user request to answer a CallParticipant "
                        + "that is already connected. CP:" + participant);
            return;
        }

        ServerTransaction serverTransaction = (ServerTransaction) transaction;
        Response ok = null;
        try {
            ok = protocolProvider.getMessageFactory().createResponse(
                Response.OK
                ,callParticipant.getFirstTransaction().getRequest());
            ok.setHeader(protocolProvider.getSipCommUserAgentHeader());
            protocolProvider.attachToTag(ok, dialog);
        }
        catch (ParseException ex) {
            callParticipant.setState(CallParticipantState.DISCONNECTED);
            logger.error(
                "Failed to construct an OK response to an INVITE request"
                , ex);
            throw new OperationFailedException(
                "Failed to construct an OK response to an INVITE request"
                , OperationFailedException.INTERNAL_ERROR
                , ex);
        }

        //Content
        ContentTypeHeader contentTypeHeader = null;
        try
        {
            //content type should be application/sdp (not applications)
            //reported by Oleg Shevchenko (Miratech)
            contentTypeHeader =
                protocolProvider.getHeaderFactory().createContentTypeHeader(
                "application", "sdp");
        }
        catch (ParseException ex)
        {
            //Shouldn't happen
            callParticipant.setState(CallParticipantState.DISCONNECTED);
            logger.error(
                "Failed to create a content type header for the OK response"
                , ex);
            throw new OperationFailedException(
                "Failed to create a content type header for the OK response"
                , OperationFailedException.INTERNAL_ERROR
                , ex);
        }

        try
        {
            CallSession callSession = SipActivator.getMediaService()
                .createCallSession(callParticipant.getCall());
            ((CallSipImpl)callParticipant.getCall())
                .setMediaCallSession(callSession);

            String sdp = null;
            //if the offer was in the invite create an sdp answer
            if (callParticipant.getSdpDescription() != null
                && callParticipant.getSdpDescription().length() > 0)
            {

                sdp = callSession.processSdpOffer(
                        callParticipant
                        , callParticipant.getSdpDescription());
            }
            //if there was no offer in the invite - create an offer
            else
            {
                sdp = callSession.createSdpOffer();
            }
            ok.setContent(sdp, contentTypeHeader);
        }
        catch (MediaException ex)
        {
            this.sayError((CallParticipantSipImpl)participant
                                  , Response.NOT_ACCEPTABLE_HERE);
            logger.error( "No sdp data was provided for the ok response to "
                          + "an INVITE request!"
                          , ex);
            throw new OperationFailedException(
                "Failed to created an SDP description for an ok response "
                + "to an INVITE request!"
                , OperationFailedException.INTERNAL_ERROR
                , ex);
        }
        catch (ParseException ex)
        {
            callParticipant.setState(CallParticipantState.DISCONNECTED);
            logger.error(
                "Failed to parse sdp data while creating invite request!", ex);
            throw new OperationFailedException(
                "Failed to parse sdp data while creating invite request!"
                , OperationFailedException.INTERNAL_ERROR
                , ex);
        }

        ContactHeader contactHeader = protocolProvider.getContactHeader();
        ok.addHeader(contactHeader);
        try
        {
            serverTransaction.sendResponse(ok);
            if( logger.isDebugEnabled() )
                logger.debug("sent response\n" + ok);
        }
        catch (Exception ex)
        {
            callParticipant.setState(CallParticipantState.DISCONNECTED);
            logger.error(
                "Failed to send an OK response to an INVITE request"
                ,ex);
            throw new OperationFailedException(
                "Failed to send an OK response to an INVITE request"
                , OperationFailedException.NETWORK_FAILURE
                , ex);
        }
    } //answer call

    /**
     * Creates a new call and call participant associated with
     * <tt>containingTransaction</tt>
     *
     * @param containingTransaction the transaction that created the call.
     * @param sourceProvider the provider that the containingTransaction
     * belongs to.
     *
     * @return a new instance of a <tt>CallParticipantSipImpl</tt>
     * corresponding to the <tt>containingTransaction</tt>.
     */
    private CallParticipantSipImpl createCallParticipantFor(
                                       Transaction containingTransaction,
                                       SipProvider sourceProvider)
    {
        CallSipImpl call = new CallSipImpl(protocolProvider);
        CallParticipantSipImpl callParticipant = new CallParticipantSipImpl(
            containingTransaction.getDialog().getRemoteParty(), call);

        if(containingTransaction instanceof ServerTransaction)
            callParticipant.setState(CallParticipantState.INCOMING_CALL);
        else
            callParticipant.setState(CallParticipantState.INITIATING_CALL);

        callParticipant.setDialog(containingTransaction.getDialog());
        callParticipant.setFirstTransaction(containingTransaction);
        callParticipant.setJainSipProvider(sourceProvider);

        activeCallsRepository.addCall(call);

        //notify everyone
        if(containingTransaction instanceof ServerTransaction)
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
     * @return  a string representation of this operation set.
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
            //something with indexes, so just ignore.
        }
        return className + "-[dn=" + protocolProvider.getOurDisplayName()
               +" addr="+protocolProvider.getOurSipAddress() + "]";
    }

    /**
     * Closes all active calls. And releases resources.
     */
    public void shutdown()
    {
        logger.trace("Ending all active calls.");
        Iterator activeCalls = this.activeCallsRepository.getActiveCalls();

        //go through all active calls.
        while(activeCalls.hasNext())
        {
            CallSipImpl call = (CallSipImpl)activeCalls.next();

            Iterator callParticipants = call.getCallParticipants();

            //go through all call participants and say bye to every one.
            while(callParticipants.hasNext())
            {
                CallParticipant participant
                    = (CallParticipant) callParticipants.next();
                try
                {
                    this.hangupCallParticipant(participant);
                }
                catch (Exception ex)
                {
                    logger.warn("Failed to properly hangup particpant "
                                + participant
                                , ex);
                }
            }
        }
    }
}
