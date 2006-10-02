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

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * Contains all functionality that has anything to do with registering and
 * maintaining registrations with a SIP Registrar.
 *
 * @todo make sure that every time we change our state to unregistered we put
 * the proper state code.
 *
 * @author Emil Ivov
 */
public class SipRegistrarConnection
        implements SipListener
{
    private static final Logger logger =
        Logger.getLogger(SipRegistrarConnection.class);

    /**
     * A reference to the sip provider that created us.
     */
    private ProtocolProviderServiceSipImpl sipProvider = null;

    /**
     * The SipURI containing the address port and transport of our registrar
     * server.
     */
    private SipURI registrarURI = null;

    /**
     * The InetAddress of the registrar we are connecting to.
     */
    private InetAddress registrarAddress = null;

    /**
     * The default amount of time (in seconds) that registration take to
     * expire or otherwise put - the number of seconds we wait before re-
     * registering.
     */
    public static final int DEFAULT_REGISTRATION_EXPIRATION = 3600;

    /**
     * The amount of time (in seconds) that registration take to expire or
     * otherwise put - the number of seconds we wait before re-registering.
     */
    private int registrationsExpiration = DEFAULT_REGISTRATION_EXPIRATION;

    /**
     * Keeps our current registration state.
     */
    private RegistrationState currentRegistrationState
        = RegistrationState.UNREGISTERED;

    /**
     * The timer we use for rescheduling registrations.
     */
    private Timer reRegisterTimer = new Timer();

    /**
     * A copy of our last sent register request. (used when unregistering)
     */
    private Request registerRequest = null;

    /**
     * The next long to use as a cseq header velue.
     */
    private long nextCSeqValue = 1;


    /**
     * Creates a new instance of this class.
     *
     * @param registrarAddress the ip address or FQDN of the registrar we will
     * be registering with.
     * @param registrarPort the port on which the specified registrar is
     * accepting connections.
     * @param registrationTransport the transport to use when sending our
     * REGISTER request to the server.
     * @param expirationTimeout the number of seconds to wait before
     * reregistering.
     * @param sipProviderCallback a reference to the
     * ProtocolProviderServiceSipImpl instance that crated us.
     *
     * @throws ParseException in case the specified registrar address is not a
     * valid reigstrar address.
     */
    public SipRegistrarConnection(InetAddress registrarAddress,
                                  int         registrarPort,
                                  String      registrationTransport,
                                  int         expirationTimeout,
                                  ProtocolProviderServiceSipImpl sipProviderCallback)
        throws ParseException
    {
        this.sipProvider = sipProviderCallback;
        this.registrarAddress = registrarAddress;
        registrarURI = sipProvider.getAddressFactory().createSipURI(
                null, registrarAddress.getHostName());

        if(registrarPort != ListeningPoint.PORT_5060)
            registrarURI.setPort(registrarPort);

        registrarURI.setTransportParam(registrationTransport);
        this.registrationsExpiration = expirationTimeout;

        //now let's register ourselves as processor for REGISTER related
        //messages.
        sipProviderCallback.registerMethodProcessor(Request.REGISTER, this);
    }

    /**
     * Sends the REGISTER request to the server speciied in the constructor.
     *
     * @throws OperationFailedException with the corresponding error code
     * if registration or construction of the Register request fail.
     */
    void register()
        throws OperationFailedException
    {
        setRegistrationState(RegistrationState.REGISTERING,
                             RegistrationStateChangeEvent.REASON_NOT_SPECIFIED,
                             null);

        //From
        FromHeader fromHeader = null;
        try
        {
            fromHeader = sipProvider.getHeaderFactory().createFromHeader(
                sipProvider.getOurSipAddress(), ProtocolProviderServiceSipImpl
                .generateLocalTag());
        }
        catch (ParseException ex)
        {
            //this should never happen so let's just log and bail.
            logger.error(
                "Failed to generate a from header for our register request."
                , ex);
            setRegistrationState(RegistrationState.CONNECTION_FAILED
                , RegistrationStateChangeEvent.REASON_INTERNAL_ERROR
                , ex.getMessage());
            throw new OperationFailedException(
                "Failed to generate a from header for our register request."
                , OperationFailedException.INTERNAL_ERROR
                , ex);
        }

        //Call ID Header
        CallIdHeader callIdHeader
            = this.getRegistrarJainSipProvider().getNewCallId();

        //CSeq Header
        CSeqHeader cSeqHeader = null;

        try
        {
            cSeqHeader = sipProvider.getHeaderFactory().createCSeqHeader(
                getNextCSeqValue(), Request.REGISTER);
        }
        catch (ParseException ex)
        {
            //Should never happen
            logger.error("Corrupt Sip Stack", ex);
            setRegistrationState(RegistrationState.CONNECTION_FAILED
                , RegistrationStateChangeEvent.REASON_INTERNAL_ERROR
                , ex.getMessage());

            throw new OperationFailedException(
                "Failed to generate a from header for our register request."
                , OperationFailedException.INTERNAL_ERROR
                , ex);

        }
        catch (InvalidArgumentException ex)
        {
            //Should never happen
            logger.error("The application is corrupt", ex);
            setRegistrationState(RegistrationState.CONNECTION_FAILED
                , RegistrationStateChangeEvent.REASON_INTERNAL_ERROR
                , ex.getMessage());
            throw new OperationFailedException(
                "Failed to generate a from header for our register request."
                , OperationFailedException.INTERNAL_ERROR
                , ex);
        }

        //To Header (Equal to the from header in a registration message.)
        ToHeader toHeader = null;
        try
        {
            toHeader = sipProvider.getHeaderFactory().createToHeader(
                sipProvider.getOurSipAddress(), null);
        }
        catch (ParseException ex)
        {
            logger.error("Could not create a To header for address:"
                          + fromHeader.getAddress(),
                          ex);
            //throw was missing - reported by Eero Vaarnas
            setRegistrationState(RegistrationState.CONNECTION_FAILED
                , RegistrationStateChangeEvent.REASON_INTERNAL_ERROR
                , ex.getMessage());
            throw new OperationFailedException(
                "Could not create a To header for address:"
                + fromHeader.getAddress()
                , OperationFailedException.INTERNAL_ERROR
                , ex);
        }
        //Via Headers
         ArrayList viaHeaders = sipProvider.getLocalViaHeaders(
                registrarAddress, getRegistrarListeningPoint());

        //MaxForwardsHeader
        MaxForwardsHeader maxForwardsHeader = sipProvider.
            getMaxForwardsHeader();
        //Request
        Request request = null;
        try
        {
            request = sipProvider.getMessageFactory().createRequest(
                registrarURI
                , Request.REGISTER
                , callIdHeader
                , cSeqHeader
                , fromHeader
                , toHeader
                , viaHeaders
                , maxForwardsHeader);
        }
        catch (ParseException ex)
        {
            logger.error("Could not create the register request!", ex);
            setRegistrationState(RegistrationState.CONNECTION_FAILED
                , RegistrationStateChangeEvent.REASON_INTERNAL_ERROR
                , ex.getMessage());
            throw new OperationFailedException(
                "Could not create the register request!"
                , OperationFailedException.INTERNAL_ERROR
                , ex);
        }

        //User Agent
        UserAgentHeader userAgentHeader
            = sipProvider.getSipCommUserAgentHeader();
        if(userAgentHeader != null)
            request.addHeader(userAgentHeader);

        //Expires Header - try to generate it twice in case the default
        //expiration period is null
        ExpiresHeader expHeader = null;
        for (int retry = 0; retry < 2; retry++)
        {
            try
            {
                expHeader = sipProvider.getHeaderFactory().createExpiresHeader(
                    registrationsExpiration);
            }
            catch (InvalidArgumentException ex)
            {
                if (retry == 0)
                {
                    registrationsExpiration = 3600;
                    continue;
                }
                logger.error(
                    "Invalid registrations expiration parameter - "
                    + registrationsExpiration,
                    ex);
                setRegistrationState(RegistrationState.CONNECTION_FAILED
                    , RegistrationStateChangeEvent.REASON_INTERNAL_ERROR
                    , ex.getMessage());
                throw new OperationFailedException(
                    "Invalid registrations expiration parameter - "
                    + registrationsExpiration
                    , OperationFailedException.INTERNAL_ERROR
                    , ex);
            }
        }
        request.addHeader(expHeader);

        //Contact Header (should contain IP)
        ContactHeader contactHeader
            = sipProvider.getRegistrationContactHeader(
                registrarAddress, getRegistrarListeningPoint());

        request.addHeader(contactHeader);
        //Transaction
        ClientTransaction regTrans = null;
        try
        {
            sipProvider.getJainSipStack().getSipProviders();
            regTrans = getRegistrarJainSipProvider().getNewClientTransaction(
                request);
        }
        catch (TransactionUnavailableException ex)
        {
            logger.error("Could not create a register transaction!\n"
                          + "Check that the Registrar address is correct!"
                          , ex);
            setRegistrationState(RegistrationState.CONNECTION_FAILED
                , RegistrationStateChangeEvent.REASON_INTERNAL_ERROR
                , ex.getMessage());
            throw new OperationFailedException(
                "Could not create a register transaction!\n"
                + "Check that the Registrar address is correct!"
                , OperationFailedException.INTERNAL_ERROR
                , ex);
        }
        try
        {
            regTrans.sendRequest();
            logger.debug("sent request= " + request);
        }
        //we sometimes get a null pointer exception here so catch them all
        catch (Exception ex)
        {
            logger.error("Could not send out the register request!", ex);
            setRegistrationState(RegistrationState.CONNECTION_FAILED
                , RegistrationStateChangeEvent.REASON_INTERNAL_ERROR
                , ex.getMessage());
            throw new OperationFailedException(
                "Could not send out the register request!"
                , OperationFailedException.NETWORK_FAILURE
                , ex);
        }

        this.registerRequest = request;
    }

    /**
     * An ok here means that our registration has been accepted or terminated
     * (depending on the corresponding REGISTER request). We change state
     * notify listeners and (in the case of a new registration) schedule
     * reregistration.
     *
     * @param clientTransatcion the ClientTransaction that we crated when
     * sending the register request.
     * @param response the OK Response that we've just received.
     */
    public void processOK(ClientTransaction clientTransatcion,
                          Response          response)
    {
        FromHeader fromHeader =
            ( (FromHeader) response.getHeader(FromHeader.NAME));
        Address address = fromHeader.getAddress();

        //check if the registrar has touched our expiration timeout
        int expires = registrationsExpiration;

        ExpiresHeader expiresHeader = response.getExpires();

        if (expiresHeader != null)
        {
            expires = expiresHeader.getExpires();
        }
        else
        {
            //if there is no expires header check the contact header
            ContactHeader contactHeader = (ContactHeader) response.getHeader(
                ContactHeader.NAME);
            if (contactHeader != null)
            {
                expires = contactHeader.getExpires();
            }
            //else - we simply reuse the expires timeout we stated in our last
            //request
            else
            {
                Request register = clientTransatcion.getRequest();
                expiresHeader = register.getExpires();

                if (expiresHeader != null)
                    expires = expiresHeader.getExpires();
                else
                {
                    //if there is no expires header check the contact header
                    contactHeader = (ContactHeader)register
                        .getHeader(ContactHeader.NAME);
                    if (contactHeader != null)
                        expires = contactHeader.getExpires();
                    else
                        expires = 0;}

            }
        }

        //If this is a respond to a REGISTER request ending our registration
        //then expires would be 0.
        //fix by Luca Bincoletto <Luca.Bincoletto@tilab.com>
        if (expires <= 0)
        {
            setRegistrationState(RegistrationState.UNREGISTERED
                , RegistrationStateChangeEvent.REASON_NOT_SPECIFIED
                , "Registration terminated.");
        }
        else
        {
            //schedule a reregistration.
            scheduleReRegistration(expires);

            setRegistrationState(
                RegistrationState.REGISTERED
                , RegistrationStateChangeEvent.REASON_NOT_SPECIFIED
                , null);
        }
    }

    /**
     * Sends a unregistered request to the registrar thus ending our
     * registration.
     * @throws OperationFailedException with the corresponding code if sending
     * or constructing the request fails.
     */
    public void unregister() throws OperationFailedException
    {
        if (getRegistrationState() != RegistrationState.REGISTERED)
        {
            return;
        }

        cancelPendingRegistrations();

        if (this.registerRequest == null)
        {
            logger.error("Couldn't find the initial register request");
            setRegistrationState(RegistrationState.CONNECTION_FAILED
                , RegistrationStateChangeEvent.REASON_INTERNAL_ERROR
                , "Could not find the initial regiest request.");
            throw new OperationFailedException(
                "Could not find the initial register request."
                , OperationFailedException.INTERNAL_ERROR);
        }
        Request unregisterRequest = (Request) registerRequest.clone();
        try
        {
            unregisterRequest.getExpires().setExpires(0);
            CSeqHeader cSeqHeader =
                (CSeqHeader) unregisterRequest.getHeader(CSeqHeader.NAME);
            //[issue 1] - increment registration cseq number
            //reported by - Roberto Tealdi <roby.tea@tin.it>
            cSeqHeader.setSeqNumber(getNextCSeqValue());

            //remove the branch id.
            ViaHeader via
                = (ViaHeader)unregisterRequest.getHeader(ViaHeader.NAME);
            if(via != null)
                via.removeParameter("branch");
        }
        catch (InvalidArgumentException ex)
        {
            logger.error("Unable to set Expires Header", ex);
            //Shouldn't happen
            setRegistrationState(
                RegistrationState.CONNECTION_FAILED
                , RegistrationStateChangeEvent.REASON_INTERNAL_ERROR
                , "Unable to set Expires Header");
            throw new OperationFailedException(
                "Unable to set Expires Header"
                , OperationFailedException.INTERNAL_ERROR
                , ex);
        }

        ClientTransaction unregisterTransaction = null;
        try
        {
            unregisterTransaction =
                this.getRegistrarJainSipProvider().getNewClientTransaction(
                    unregisterRequest);
        }
        catch (TransactionUnavailableException ex)
        {
            logger.error("Unable to create a unregister transaction", ex);
            setRegistrationState(
                RegistrationState.CONNECTION_FAILED
                , RegistrationStateChangeEvent.REASON_INTERNAL_ERROR
                , "Unable to create a unregister transaction");
            throw new OperationFailedException(
                "Unable to create a unregister transaction"
                , OperationFailedException.INTERNAL_ERROR
                , ex);

        }
        try
        {
            unregisterTransaction.sendRequest();
            logger.debug("sent request: " + unregisterRequest);

            //emcho: i think we should not set to unregistered here but rather
            //wait for the ok response.
            // setRegistrationState(RegistrationState.UNREGISTERING,
            //                     RegistrationStateChangeEvent.
            //                     REASON_USER_REQUEST, null);

        }
        catch (SipException ex)
        {
            logger.error("Failed to send unregister request", ex);
            setRegistrationState(
                  RegistrationState.CONNECTION_FAILED
                , RegistrationStateChangeEvent.REASON_INTERNAL_ERROR
                , "Unable to create a unregister transaction");
            throw new OperationFailedException(
                "Failed to send unregister request"
                , OperationFailedException.INTERNAL_ERROR
                , ex);
        }
    }

    /**
     * Returns the state of this connection.
     * @return a RegistrationState instance indicating the state of our
     * registration with the corresponding registrar.
     */
    public RegistrationState getRegistrationState()
    {
        return currentRegistrationState;
    }

    /**
     * Sets our registraton state to <tt>newState</tt> and dispatches an event
     * through the protocol provider service impl.
     * <p>
     * @param newState a reference to the RegistrationState that we're currently
     * detaining.
     * @param reasonCode one of the REASON_XXX error codes specified in
     * {@link RegistrationStateChangeEvent}.
     * @param reason a reason String further explaining the reasonCode.
     */
    private void setRegistrationState(RegistrationState newState,
                                      int               reasonCode,
                                      String            reason)
    {
        if( currentRegistrationState.equals(newState) )
            return;

        RegistrationState oldState = currentRegistrationState;
        this.currentRegistrationState = newState;

        sipProvider.fireRegistrationStateChanged(
            oldState, newState, reasonCode, reason);
    }

    /**
     * The task is started once a registration has been created. It is
     * scheduled to run after the expiration timeout has come to an end when
     * it will resend the REGISTER request.
     */
    private class ReRegisterTask
        extends TimerTask
    {
        /**
         * Creates a new instance of the ReRegister task prepared to reregister
         * us after the specified interval.
         */
        public ReRegisterTask()
        {}

        /**
         * Simply calls the register method.
         */
        public void run()
        {
            try
            {
                if (getRegistrationState() == RegistrationState.REGISTERED)
                    register();
            }
            catch (OperationFailedException ex)
            {
                logger.error("Failed to reRegister", ex);
                setRegistrationState(
                    RegistrationState.CONNECTION_FAILED
                    , RegistrationStateChangeEvent.REASON_INTERNAL_ERROR
                    , "Failed to re register with the SIP server.");
            }
        }
    }

    /**
     * Cancels all pending reregistrations. The method is useful when shutting
     * down.
     */
    private void cancelPendingRegistrations()
    {
        reRegisterTimer.cancel();
        reRegisterTimer = null;

        reRegisterTimer = new Timer();
    }

    /**
     * Schedules a reregistration for after almost <tt>expires</tt>
     * seconds. The method leaves a margin for all intervals larger than
     * 60 seconds, scheduling the registration for slightly earlier by reducing
     * with 10% the number of seconds specified in the expires param.
     * <p>
     * @param expires the number of seconds that we specified in the
     * expires header when registering.
     */
    private void scheduleReRegistration(int expires)
    {
            ReRegisterTask reRegisterTask = new ReRegisterTask();

            //java.util.Timer thinks in miliseconds and expires header contains
            //seconds
            //bug report and fix by Willem Romijn (romijn at lucent.com)
            //We keep a margin of 10% when sending re-registrations (1000
            //becomes 900)
            if (expires > 60)
            {
                expires = expires * 900;
            }
            else{
                expires = expires * 1000;
            }

            reRegisterTimer.schedule(reRegisterTask, expires);
    }

    /**
     * Returns the next long to use as a cseq header velue.
     * @return the next long to use as a cseq header velue.
     */
    private long getNextCSeqValue()
    {
        return nextCSeqValue++;
    }

    /**
     * Handles a timeout of our register request.
     * @param transatcion the transaction that has just timeouted.
     * @param request our initial register request.
     */
    public void processTimeout(Transaction transatcion, Request request)
    {
        setRegistrationState(
            RegistrationState.UNREGISTERED
            , RegistrationStateChangeEvent.REASON_NOT_SPECIFIED
            , "A timeout occurred while trying to connect to the server.");
    }

    /**
     * Handles a NOT_IMPLEMENTED response sent in reply of our register request.
     *
     * @param transatcion the transaction that our initial register reqeust
     * belongs to.
     * @param response our initial register request.
     */
    public void processNotImplemented(ClientTransaction transatcion,
                                      Response response)
    {
            setRegistrationState(
                RegistrationState.UNREGISTERED
                , RegistrationStateChangeEvent.REASON_NOT_SPECIFIED
                , registrarAddress.getHostAddress()
                + " does not appear to be a sip registrar. (Returned a "
                +"NOT_IMPLEMENTED response to a register request)");
    }

    /**
     * Returns the address of this connection's registrar.
     *
     * @return the InetAddress of our registrar server.
     */
    public InetAddress getRegistrarAddress()
    {
        return registrarAddress;
    }

    /**
     * Returns the listening point that should be used for communiction with our
     * current registrar.
     *
     * @return the listening point that should be used for communiction with our
     * current registrar.
     */
    private ListeningPoint getRegistrarListeningPoint()
    {
        return sipProvider.getListeningPoint(registrarURI.getTransportParam());
    }

    /**
     * Returns the JAIN SIP provider that should be used for communication with
     * our current registrar.
     *
     * @return the JAIN SIP provider that should be used for communication with
     * our current registrar.
     */
    private SipProvider getRegistrarJainSipProvider()
    {
        return sipProvider.getJainSipProvider(registrarURI.getTransportParam());
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
        Dialog dialog = clientTransaction.getDialog();
        String method = ( (CSeqHeader) response.getHeader(CSeqHeader.NAME)).
                                                                getMethod();

        Response responseClone = (Response) response.clone();

        SipProvider sourceProvider = (SipProvider)responseEvent.getSource();

        //OK
        if (response.getStatusCode() == Response.OK) {
            processOK(clientTransaction, response);
        }
        //NOT_IMPLEMENTED
        else if (response.getStatusCode() == Response.OK) {
            processNotImplemented(clientTransaction, response);
        }
        //Trying
        else if (response.getStatusCode() == Response.TRYING) {
            //do nothing
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
        else if ( response.getStatusCode() / 100 == 4 )
        {
            logger.error("Received an error response.");

            //tell the others we couldn't register
            this.setRegistrationState(
                RegistrationState.CONNECTION_FAILED
                , RegistrationStateChangeEvent.REASON_NOT_SPECIFIED
                , "Received an error while trying to register. "
                + "Server returned error:" + response.getReasonPhrase()
            );
        }
        //ignore everything else.
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
                        Response          response,
                        SipProvider       jainSipProvider)
    {
        try
        {
            logger.debug("Authenticating a Register request.");

            ClientTransaction retryTran
                = sipProvider.getSipSecurityManager().handleChallenge(
                    response
                    , clientTransaction
                    , jainSipProvider);

            retryTran.sendRequest();
            return;
        }
        catch (Exception exc)
        {
            logger.error("We failed to authenticate a Register request.", exc);

            //tell the others we couldn't register
            this.setRegistrationState(
                    RegistrationState.AUTHENTICATION_FAILED
                    , RegistrationStateChangeEvent.REASON_AUTHENTICATION_FAILED
                    , "We failed to authenticate with the server."
                );
        }
    }

    /**
     * Process an asynchronously reported DialogTerminatedEvent.
     * When a dialog transitions to the Terminated state, the stack
     * keeps no further records of the dialog. This notification can be used by
     * applications to clean up any auxiliary data that is being maintained
     * for the given dialog.
     *
     * @param dialogTerminatedEvent -- an event that indicates that the
     *       dialog has transitioned into the terminated state.
     * @since v1.2
     */
    public void processDialogTerminated(DialogTerminatedEvent
                                        dialogTerminatedEvent)
    {
    }

    /**
     * Processes a Request received on a SipProvider upon which this SipListener
     * is registered.
     * <p>
     * @param requestEvent requestEvent fired from the SipProvider to the
     * SipListener representing a Request received from the network.
     */
    public void processRequest(RequestEvent requestEvent)
    {
        /** @todo send not implemented */
    }

    /**
     * Processes a retransmit or expiration Timeout of an underlying
     * {@link Transaction}handled by this SipListener.
     *
     * @param timeoutEvent the timeoutEvent received indicating either the
     * message retransmit or transaction timed out.
     */
    public void processTimeout(TimeoutEvent timeoutEvent)
    {
    }

    /**
     * Process an asynchronously reported TransactionTerminatedEvent.
     * When a transaction transitions to the Terminated state, the stack
     * keeps no further records of the transaction.
     *
     * @param transactionTerminatedEvent an event that indicates that the
     * transaction has transitioned into the terminated state.
     */
    public void processTransactionTerminated(TransactionTerminatedEvent
                                             transactionTerminatedEvent)
    {
    }

    /**
     * Process an asynchronously reported IO Exception.
     *
     * @param exceptionEvent The Exception event that is reported to the
     * application.
     */
    public void processIOException(IOExceptionEvent exceptionEvent)
    {
    }

    /**
     * Returns a string representation of this connection instance
     * instance including information that would permit to distinguish it among
     * other sip listeners when reading a log file.
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
        return className + "-[dn=" + sipProvider.getOurDisplayName()
               +" addr="+sipProvider.getOurSipAddress() + "]";
    }
}
