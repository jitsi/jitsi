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
 * @author Emil Ivov
 */
public class SipRegistrarConnection
    extends MethodProcessorAdapter
{
    /**
     * Our class logger.
     */
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
    * The next long to use as a cseq header value.
    */
    private long nextCSeqValue = 1;

    /**
    * The client transaction that we used for sending the last REGISTER
    * request.
    */
    ClientTransaction regTrans = null;

    /**
    * Option for specifing keep-alive method
    */
    private static final String KEEP_ALIVE_METHOD = "KEEP_ALIVE_METHOD";

    /**
    * Option for keep-alive interval
    */
    private static final String KEEP_ALIVE_INTERVAL = "KEEP_ALIVE_INTERVAL";

    /**
    * Default value for keep-alive method - register
    */
    private static final int KEEP_ALIVE_INTERVAL_DEFAULT_VALUE = 25;

    /**
    * Specifies whether or not we should be using a route header in register
    * requests. This field is specified by the REGISTERS_USE_ROUTE account
    * property.
    */
    private boolean useRouteHeader = false;

    /**
    * The sip address that we're currently behind (the one that corresponds to
    * our account id). ATTENTION!!! This field must remain <tt>null</tt>
    * when this protocol provider is configured as "No Regsitrar" account and
    * only be initialized if we actually have a registrar.
    */
    private Address ourSipAddressOfRecord = null;

    /**
     * callId must be unique from first register to last one, till we
     * unregister.
     */
    private CallIdHeader callIdHeader = null;

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
    * re-registering.
    * @param sipProviderCallback a reference to the
    * ProtocolProviderServiceSipImpl instance that created us.
    *
    * @throws ParseException in case the specified registrar address is not a
    * valid reigstrar address.
    */
    public SipRegistrarConnection(
                            InetAddress  registrarAddress,
                            int          registrarPort,
                            String       registrationTransport,
                            int          expirationTimeout,
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

        //init our address of record to save time later
        getAddressOfRecord();

        //now let's register ourselves as processor for REGISTER related
        //messages.
        sipProviderCallback.registerMethodProcessor(Request.REGISTER, this);
    }

    /**
    * Empty constructor that we only have in order to allow for classes like
    * SipRegistrarlessConnection to extend this class.
    */
    protected SipRegistrarConnection()
    {

    }

    /**
    * Sends the REGISTER request to the server specified in the constructor.
    *
    * @throws OperationFailedException with the corresponding error code
    * if registration or construction of the Register request fail.
    */
    void register()
        throws OperationFailedException
    {
        // skip REGISTERING event if we are already registered
        // we are refreshing our registration
        if (getRegistrationState() != RegistrationState.REGISTERED)
            setRegistrationState(RegistrationState.REGISTERING,
                                RegistrationStateChangeEvent.REASON_NOT_SPECIFIED,
                                null);

        Request request;
        try
        {
            //We manage the Call ID Header ourselves.The rest will be handled
            //by our SipMessageFactory
            if(callIdHeader == null)
                callIdHeader = this.getJainSipProvider().getNewCallId();

            request = sipProvider.getMessageFactory().createRegisterRequest(
                getAddressOfRecord(), registrationsExpiration, callIdHeader,
                getNextCSeqValue());
        }
        catch (Exception exc)
        {
            //catches InvalidArgumentException, ParseExeption
            //this should never happen so let's just log and bail.
            logger.error("Failed to create a Register request." , exc);
            setRegistrationState(RegistrationState.CONNECTION_FAILED,
                 RegistrationStateChangeEvent.REASON_INTERNAL_ERROR,
                 exc.getMessage());
            throw new OperationFailedException(
                "Failed to generate a from header for our register request.",
                OperationFailedException.INTERNAL_ERROR, exc);
        }

        //Transaction
        try
        {
            regTrans = getJainSipProvider().getNewClientTransaction(request);
        }
        catch (TransactionUnavailableException ex)
        {
            logger.error("Could not create a register transaction!\n"
                        + "Check that the Registrar address is correct!",
                        ex);

            setRegistrationState(RegistrationState.CONNECTION_FAILED,
                RegistrationStateChangeEvent.REASON_INTERNAL_ERROR,
                ex.getMessage());

            throw new OperationFailedException(
                "Could not create a register transaction!\n"
                + "Check that the Registrar address is correct!",
                OperationFailedException.NETWORK_FAILURE,
                ex);
        }

        try
        {
            regTrans.sendRequest();
            logger.debug("sent request=\n" + request);
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
    * @param clientTransatcion the ClientTransaction that we created when
    * sending the register request.
    * @param response the OK Response that we've just received.
    */
    public void processOK(ClientTransaction clientTransatcion,
                        Response          response)
    {
        //first extract the expires value that we requested
        int requestedExpiration = 0;
        Request register = clientTransatcion.getRequest();
        ExpiresHeader expiresHeader = register.getExpires();

        if (expiresHeader != null)
            requestedExpiration = expiresHeader.getExpires();
        else
        {
            //if there is no expires header check the contact header
            ContactHeader contactHeader = (ContactHeader) register
                .getHeader(ContactHeader.NAME);
            if (contactHeader != null)
                requestedExpiration = contactHeader.getExpires();
            else
                requestedExpiration = 0;
        }

        //now check if the registrar has touched our expiration timeout in its
        //response
        int grantedExpiration = registrationsExpiration;

        expiresHeader = response.getExpires();

        if (expiresHeader != null)
        {
            grantedExpiration = expiresHeader.getExpires();
        }
        else
        {
            //if there is no expires header check the contact header
            ContactHeader contactHeader = (ContactHeader) response.getHeader(
                ContactHeader.NAME);
            if (contactHeader != null)
            {
                grantedExpiration = contactHeader.getExpires();
            }
            //else - we simply reuse the expires timeout we stated in our last
            //request
            else
            {
                grantedExpiration = requestedExpiration;
            }
        }

        //If this is a respond to a REGISTER request ending our registration
        //then expires would be 0.
        //fix by Luca Bincoletto <Luca.Bincoletto@tilab.com>

        //we also take into account the requested expiration since if it was 0
        //we don't really care what the server replied (I have an asterisk here
        //that gives me 3600 even if I request 0).
        if (grantedExpiration <= 0 || requestedExpiration <= 0)
        {
            setRegistrationState(RegistrationState.UNREGISTERED
                , RegistrationStateChangeEvent.REASON_USER_REQUEST
                , "Registration terminated.");
        }
        else
        {
            int scheduleTime = grantedExpiration;

            // registration schedule interval can be forced to keep alive
            // with setting property KEEP_ALIVE_METHOD to register and
            // setting the interval with property KEEP_ALIVE_INTERVAL
            // to value in seconds, both properties are account props
            // this does not change expiration header
            // If KEEP_ALIVE_METHOD is null we default send registers on
            // interval of 25 seconds
            String keepAliveMethod =
                sipProvider.getAccountID().getAccountPropertyString(
                    KEEP_ALIVE_METHOD);

            if((keepAliveMethod != null &&
                keepAliveMethod.equalsIgnoreCase("register"))
                || keepAliveMethod == null )
            {
                int registrationInterval =
                    sipProvider.getAccountID().getAccountPropertyInt(
                        KEEP_ALIVE_INTERVAL, KEEP_ALIVE_INTERVAL_DEFAULT_VALUE);

                if (registrationInterval < grantedExpiration)
                {
                    scheduleTime = registrationInterval;
                }
            }


            //schedule a reregistration.
            scheduleReRegistration(scheduleTime);

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
        unregister(true);
    }

    /**
    * Sends a unregistered request to the registrar thus ending our
    * registration.
    *
    * @param sendUnregister indicates whether we should actually send an
    * unREGISTER request or simply set our state to UNREGISTERED.
    *
    * @throws OperationFailedException with the corresponding code if sending
    * or constructing the request fails.
    */
    private void unregister(boolean sendUnregister)
        throws OperationFailedException
    {
        if (getRegistrationState() == RegistrationState.UNREGISTERED)
        {
            logger.trace("Trying to unregister when already unresgistered");
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

        setRegistrationState(RegistrationState.UNREGISTERING,
                RegistrationStateChangeEvent.REASON_USER_REQUEST, "");

        if(!sendUnregister)
            return;

        //We are apparently registered so send an un-Register request.
        Request unregisterRequest;
        try
        {
            unregisterRequest = sipProvider.getMessageFactory()
                .createUnRegisterRequest(registerRequest, getNextCSeqValue());
        }
        catch (InvalidArgumentException ex)
        {
            logger.error("Unable to create an unREGISTER request.", ex);
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
            unregisterTransaction = getJainSipProvider()
                .getNewClientTransaction(unregisterRequest);
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
            // remove current call-id header
            // on next register we will create new one
            callIdHeader = null;

            unregisterTransaction.sendRequest();
            logger.info("sent request:\n" + unregisterRequest);

            //if we're currently registered or in a process of unregistering
            //we'll wait for an ok response before changing the status.
            //otherwise we set it immediately.
            if(!(getRegistrationState().equals(RegistrationState.REGISTERED) ||
            getRegistrationState().equals(RegistrationState.UNREGISTERING)))
            {
                logger.info("Setting state to UNREGISTERED.");
                setRegistrationState(
                    RegistrationState.UNREGISTERED
                    , RegistrationStateChangeEvent.REASON_USER_REQUEST, null);

                //kill the registration tran in case it is still active
                if (regTrans != null
                    && regTrans.getState().getValue()
                            <= TransactionState.PROCEEDING.getValue())
                {
                    logger.trace("Will try to terminate reg tran ...");
                    regTrans.terminate();
                    logger.trace("Transaction terminated!");
                }
            }
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
    * Sets our registration state to <tt>newState</tt> and dispatches an event
    * through the protocol provider service impl.
    * <p>
    * @param newState a reference to the RegistrationState that we're currently
    * detaining.
    * @param reasonCode one of the REASON_XXX error codes specified in
    * {@link RegistrationStateChangeEvent}.
    * @param reason a reason String further explaining the reasonCode.
    */
    void setRegistrationState(RegistrationState newState,
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
    * Returns the next long to use as a cseq header value.
    * @return the next long to use as a cseq header value.
    */
    private long getNextCSeqValue()
    {
        return nextCSeqValue++;
    }

    /**
    * Handles a NOT_IMPLEMENTED response sent in reply of our register request.
    *
    * @param transatcion the transaction that our initial register request
    * belongs to.
    * @param response our initial register request.
    */
    public void processNotImplemented(ClientTransaction transatcion,
                                    Response response)
    {
            setRegistrationState(
                RegistrationState.CONNECTION_FAILED
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
    private InetAddress getRegistrarAddress()
    {
        return registrarAddress;
    }

    /**
    * Returns the listening point that should be used for communication with our
    * current registrar.
    *
    * @return the listening point that should be used for communication with our
    * current registrar.
    */
    ListeningPoint getListeningPoint()
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
    public SipProvider getJainSipProvider()
    {
        return sipProvider.getJainSipProvider(getTransport());
    }

    /**
    * Returns the transport that this connection is currently using to
    * communicate with the Registrar.
    *
    * @return the transport that this connection is using.
    */
    public String getTransport()
    {
        return registrarURI.getTransportParam();
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
        ClientTransaction clientTransaction = responseEvent
            .getClientTransaction();

        Response response = responseEvent.getResponse();

        SipProvider sourceProvider = (SipProvider)responseEvent.getSource();
        boolean processed = false;

        //OK
        if (response.getStatusCode() == Response.OK) {
            processOK(clientTransaction, response);
            processed = true;
        }
        //NOT_IMPLEMENTED
        else if (response.getStatusCode() == Response.NOT_IMPLEMENTED) {
            processNotImplemented(clientTransaction, response);
            processed = true;
        }
        //Trying
        else if (response.getStatusCode() == Response.TRYING) {
            //do nothing
        }
        //401 UNAUTHORIZED,
        //407 PROXY_AUTHENTICATION_REQUIRED,
        //403 FORBIDDEN
        else if (response.getStatusCode() == Response.UNAUTHORIZED
                || response.getStatusCode()
                                == Response.PROXY_AUTHENTICATION_REQUIRED
                || response.getStatusCode() == Response.FORBIDDEN)
        {
            processAuthenticationChallenge(
                    clientTransaction, response, sourceProvider);
            processed = true;
        }
        else if ( response.getStatusCode() >= 400 )
        {
            logger.error("Received an error response.");

            //tell the others we couldn't register
            this.setRegistrationState(
                RegistrationState.CONNECTION_FAILED
                , RegistrationStateChangeEvent.REASON_NOT_SPECIFIED
                , "Received an error while trying to register. "
                + "Server returned error:" + response.getReasonPhrase()
            );
            processed = true;
        }
        //ignore everything else.

        return processed;
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
                        ClientTransaction clientTransaction,
                        Response          response,
                        SipProvider       jainSipProvider)
    {
        try
        {
            logger.debug("Authenticating a Register request.");

            ClientTransaction retryTran;

            if( response.getStatusCode() == Response.UNAUTHORIZED
                || response.getStatusCode()
                    == Response.PROXY_AUTHENTICATION_REQUIRED)
            {
                //respond to the challenge
                retryTran = sipProvider.getSipSecurityManager().handleChallenge(
                    response, clientTransaction, jainSipProvider);
            }
            else
            {
                //we got a BAD PASSWD reply. send a new credential-less request
                //in order to trigger a new challenge and rerequest a password.
                retryTran = sipProvider.getSipSecurityManager()
                    .handleForbiddenResponse(
                            response, clientTransaction, jainSipProvider);
            }

            if(retryTran == null)
            {
                logger.trace("No password supplied or error occured!");
                unregister(false);
                return;
            }

            //the security manager has most probably changed the sequence number
            //so let's make sure we update it here.
            updateRegisterSequenceNumber(retryTran);

            retryTran.sendRequest();
            return;
        }
        catch (OperationFailedException exc)
        {
            if(exc.getErrorCode()
                == OperationFailedException.AUTHENTICATION_CANCELED)
            {
                this.setRegistrationState(
                    RegistrationState.UNREGISTERED,
                    RegistrationStateChangeEvent.REASON_USER_REQUEST,
                    "User has canceled the authentication process.");
            }
            else
            {
                //tell the others we couldn't register
                this.setRegistrationState(
                    RegistrationState.AUTHENTICATION_FAILED,
                    RegistrationStateChangeEvent.REASON_AUTHENTICATION_FAILED,
                    "We failed to authenticate with the server."
                );
            }
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
    * Processes a Request received on a SipProvider upon which this SipListener
    * is registered.
    * <p>
    *
    * @param requestEvent requestEvent fired from the SipProvider to the
    *            SipListener representing a Request received from the network.
    * @return <tt>true</tt> if the specified event has been handled by this
    *         processor and shouldn't be offered to other processors registered
    *         for the same method; <tt>false</tt>, otherwise
    */
    public boolean processRequest(RequestEvent requestEvent)
    {
        /** @todo send not implemented */
        return false;
    }

    /**
    * Processes a retransmit or expiration Timeout of an underlying
    * {@link Transaction}handled by this SipListener.
    *
    * @param timeoutEvent the timeoutEvent received indicating either the
    *            message retransmit or transaction timed out.
    * @return <tt>true</tt> if the specified event has been handled by this
    *         processor and shouldn't be offered to other processors registered
    *         for the same method; <tt>false</tt>, otherwise
    */
    public boolean processTimeout(TimeoutEvent timeoutEvent)
    {
        // don't alert the user if we're already off
        if (getRegistrationState().equals(RegistrationState.UNREGISTERED) == false)
        {
            setRegistrationState(RegistrationState.CONNECTION_FAILED,
                RegistrationStateChangeEvent.REASON_NOT_SPECIFIED,
                "A timeout occurred while trying to connect to the server.");
        }
        return true;
    }

    /**
    * Process an asynchronously reported IO Exception.
    *
    * @param exceptionEvent The Exception event that is reported to the
    *            application.
    * @return <tt>true</tt> if the specified event has been handled by this
    *         processor and shouldn't be offered to other processors registered
    *         for the same method; <tt>false</tt>, otherwise
    */
    public boolean processIOException(IOExceptionEvent exceptionEvent)
    {
        setRegistrationState(
            RegistrationState.CONNECTION_FAILED
            , RegistrationStateChangeEvent.REASON_NOT_SPECIFIED
            , "An error occurred while trying to connect to the server."
            + "[" + exceptionEvent.getHost() + "]:"
            + exceptionEvent.getPort() + "/"
            + exceptionEvent.getTransport());
        return true;
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
            +" addr="+getAddressOfRecord() + "]";
    }

    /**
    * Updates our local sequence counter based on the value in the CSeq header
    * of the request that originated the <tt>lastClientTran</tt> transation.
    * The method is used after running an authentication challenge through
    * the security manager. The Security manager would manually increment the
    * CSeq number of the request so we need to update our local counter or
    * otherwise the next REGISTER we send would have a wrong CSeq.
    *
    * @param lastClientTran the transaction that we should be using to update
    * our local sequence number
    */
    private void updateRegisterSequenceNumber(ClientTransaction lastClientTran)
    {
        Request req = lastClientTran.getRequest();

        CSeqHeader cSeqHeader = (CSeqHeader)req.getHeader(CSeqHeader.NAME);
        long sequenceNumber = cSeqHeader.getSeqNumber();

        //sequenceNumber is the value of the CSeq header in the request we just
        //sent so the next CSeq Value should be set to seqNum + 1.
        this.nextCSeqValue = sequenceNumber + 1;
    }

    /**
    * Determines whether Register requests should be using a route header. The
    * return value of this method is specified by the REGISTERS_USE_ROUTE
    * account property.
    *
    * Jeroen van Bemmel: The reason this may needed, is that standards-
    * compliant registrars check the domain in the request URI. If it contains
    * an IP address, some registrars are unable to match/process it (they may
    * forward instead, and get into a forwarding loop)
    *
    * @return true if we should be using a route header.
    */
    public boolean isRouteHeaderEnabled()
    {
        return useRouteHeader;
    }

    /**
    * Returns true if this is a fake connection that is not actually using
    * a registrar. This method should be overridden in
    * <tt>SipRegistrarlessConnection</tt> and return <tt>true</tt> in there.
    *
    * @return true if this connection is really using a registrar and
    * false if it is a fake connection that doesn't really use a registrar.
    */
    public boolean isRegistrarless()
    {
        return false;
    }

    /**
    * Returns the address of record that we are using to register against our
    * registrar or null if this is a fake or "Registrarless" connection. If
    * our are trying to obtain an address to put in your from header and don't
    * no what to do in the case of registrarless accounts - think about using
    * <tt>ProtocolProviderServiceSipImpl.createAddressOfRecord()</tt>.
    *
    * @return our Address Of Record
    */
    public Address getAddressOfRecord()
    {
        //if we have a registrar we should return our Address of record here.
        if(this.ourSipAddressOfRecord != null)
            return this.ourSipAddressOfRecord;

        // the connection would not have an address of record if it does not
        // have a registrar.
        if(isRegistrarless())
            return null;

        //create our own address.
        String ourUserID =
            sipProvider.getAccountID().getAccountPropertyString(
                ProtocolProviderFactory.USER_ID);

        String sipUriHost = null;
        if( ourUserID.indexOf("@") != -1
            && ourUserID.indexOf("@") < ourUserID.length() -1 )
        {
            //use the domain in the SIP URI if possible.
            sipUriHost = ourUserID.substring( ourUserID.indexOf("@") + 1 );
            ourUserID = ourUserID.substring( 0, ourUserID.indexOf("@") );
        }

        //if there was no domain name in the SIP URI use the registrar address
        if(sipUriHost == null)
            sipUriHost = getRegistrarAddress().getHostName();

        SipURI ourSipURI = null;
        try
        {
            ourSipURI = sipProvider.getAddressFactory().createSipURI(
                            ourUserID, sipUriHost);
            ourSipAddressOfRecord = sipProvider.getAddressFactory()
                .createAddress(sipProvider.getOurDisplayName(), ourSipURI);
        }
        catch (ParseException ex)
        {
            throw new IllegalArgumentException(
                "Could not create a SIP URI for user "
                + ourUserID + "@" + sipUriHost
                + " and registrar "
                + getRegistrarAddress().getHostName());
        }
        return ourSipAddressOfRecord;
    }
}
