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
 * A SIP implementation of the Protocol Provider Service.
 * @todo replace internal errors with something that we've defined.
 * @author Emil Ivov
 */
public class ProtocolProviderServiceSipImpl
  implements ProtocolProviderService, SipListener
{
    private static final Logger logger =
        Logger.getLogger(ProtocolProviderServiceSipImpl.class);

    /**
     * The hashtable with the operation sets that we support locally.
     */
    private Hashtable supportedOperationSets = new Hashtable();

    /**
     * The identifier of the account that this provider represents.
     */
    private AccountID accountID = null;

    /**
     * We use this to lock access to initialization.
     */
    private Object initializationLock = new Object();

    /**
     * indicates whether or not the provider is initialized and ready for use.
     */
    private boolean isInitialized = false;

    /**
     * A list of all listeners registered for
     * <tt>RegistrationStateChangeEvent</tt>s.
     */
    private List registrationListeners = new ArrayList();

    /**
     * The SipFactory instance used to create the SipStack and the Address
     * Message and Header Factories.
     */
    private SipFactory sipFactory;

    /**
     * The AddressFactory used to create URLs ans Address objects.
     */
    private AddressFactory addressFactory;

    /**
     * The HeaderFactory used to create SIP message headers.
     */
    private HeaderFactory headerFactory;

    /**
     * The Message Factory used to create SIP messages.
     */
    private MessageFactory messageFactory;

    /**
     * The sipStack instance that handles SIP communications.
     */
    private SipStack jainSipStack;

    /**
     * The default listening point that we use for UDP communication..
     */
    private ListeningPoint udpListeningPoint = null;

    /**
     * The JAIN SIP SipProvider instance.
     */
    private SipProvider jainSipProvider;

    /**
     * The name of the property under which the jain-sip-ri would expect to find
     * the address, port and transport.
     */
    private static final String JSPNAME_OUTBOUND_PROXY =
        "javax.sip.OUTBOUND_PROXY";

    /**
     * The name of the property under which the jain-sip-ri would expect to find
     * the the name of the stack..
     */
    private static final String JSPNAME_STACK_NAME =
        "javax.sip.STACK_NAME";
    /**
     * The name of the property under which the jain-sip-ri would expect to find
     * the name of a debug log file.
     */
    private static final String NSPNAME_DEBUG_LOG =
        "gov.nist.javax.sip.DEBUG_LOG";

    /**
     * The default name of a debug log file for the jain-sip RI.
     */
    private static final String NSPVALUE_DEBUG_LOG =
        "./log/sc-jainsipdebug.log";

    /**
     * The name of the property under which the jain-sip-ri would expect to find
     * the name of a server log file (I don't really know what is the
     * difference between this and the DEBUG_LOG).
     */
    private static final String NSPNAME_SERVER_LOG =
        "gov.nist.javax.sip.SERVER_LOG";

    /**
     * The default name of a server log file for the jain-sip RI.
     */
    private static final String NSPVALUE_SERVER_LOG =
        "./log/sc-jainsipserver.log";

    /**
     * A random generator we use to generate tags.
     */
    private static Random rand = new Random();

    /**
     * The name of the property under which the jain-sip-ri would expect to find
     * the log level (detail) for all stack logging.
     */
    private static final String NSPNAME_TRACE_LEVEL =
        "gov.nist.javax.sip.TRACE_LEVEL";

    /**
     * A String indicating the default debug level for the jain-sip-ri (must be
     * log4j compatible).
     */
    private static final String NSPVALUE_TRACE_LEVEL = "TRACE";

    /**
     * The name of the property under which the jain-sip-ri would expect to find
     * a property specifying whether or not it is to cache client connections.
     */
    private static final String NSPNAME_CACHE_CLIENT_CONNECTIONS =
        "gov.nist.javax.sip.CACHE_CLIENT_CONNECTIONS";

    /**
     * The name of the property under which the user may specify the number of
     * the port where they would prefer us to bind our sip socket.
     */
    private static final String PREFERRED_SIP_PORT =
        "net.java.sip.communicator.impl.protocol.sip.PREFERRED_SIP_PORT";


    /**
     * The name of the property under which the user may specify the number of
     * seconds that registrations take to expire.
     */
    private static final String REGISTRATION_EXPIRATION =
        "net.java.sip.communicator.impl.protocol.sip.REGISTRATION_EXPIRATION";

    /**
     * A default specifyier telling the stack whether or not to cache client
     * connections.
     */
    private static final String NSPVALUE_CACHE_CLIENT_CONNECTIONS = "false";

    /**
     * Default number of times that our requests can be forwarded.
     */
    private static final int  MAX_FORWARDS = 70;

    /**
     * The default maxForwards header that we use in our requests.
     */
    private MaxForwardsHeader maxForwardsHeader = null;

    /**
     * The contact header we use in non REGISTER requests.
     */
    protected ContactHeader genericContactHeader = null;

    /**
     * The sip address that we're currently behind (the one that corresponds to
     * our account id).
     */
    Address ourSipAddress = null;

    /**
     * The name that we want to send others when calling or chatting with them.
     */
    String ourDisplayName = null;

    /**
     * Our current connection with the registrar.
     */
    private SipRegistrarConnection sipRegistrarConnection = null;

    /**
     * Registers the specified listener with this provider so that it would
     * receive notifications on changes of its state or other properties such
     * as its local address and display name.
     * @param listener the listener to register.
     */
    public void addRegistrationStateChangeListener(
        RegistrationStateChangeListener listener)
    {
        registrationListeners.add(listener);
    }

    /**
     * Creates a RegistrationStateChange event corresponding to the specified
     * old and new jain sip states and notifies all currently registered
     * listeners.
     * <p>
     * @param oldState the state that we had before this transition occurred.
     * @param newState the state that we have now after the transition has
     * occurred
     * @param reasonCode a code indicating the reason for the event.
     * @param reason a text explaining the reason for the event.
     */
    void fireRegistrationStateChanged( RegistrationState oldState,
                                       RegistrationState newState,
                                       int               reasonCode,
                                       String            reason )
    {
        RegistrationStateChangeEvent event
            = new RegistrationStateChangeEvent(
                this, oldState, newState, reasonCode, reason);

        logger.debug("Dispatching " + event + " to "
                     + registrationListeners.size()+ " listeners.");

        for (int i = 0; i < registrationListeners.size(); i++)
        {
            RegistrationStateChangeListener listener
                = (RegistrationStateChangeListener)registrationListeners.get(i);
            listener.registrationStateChanged(event);
        }

        logger.trace("Done.");
    }

    /**
     * Returns the AccountID that uniquely identifies the account represented by
     * this instance of the ProtocolProviderService.
     * @return the id of the account represented by this provider.
     */
    public AccountID getAccountID()
    {
        return accountID;
    }

    /**
     * Returns the state of the registration of this protocol provider with the
     * corresponding registration service.
     * @return ProviderRegistrationState
     */
    public RegistrationState getRegistrationState()
    {
        if(this.sipRegistrarConnection == null )
            return RegistrationState.UNREGISTERED;
        return sipRegistrarConnection.getRegistrationState();
    }

    /**
     * Returns the short name of the protocol that the implementation of this
     * provider is based upon (like SIP, Jabber, ICQ/AIM,  or others for
     * example). If the name of the protocol has been enumerated in
     * ProtocolNames then the value returned by this method must be the same as
     * the one in ProtocolNames.
     * @return a String containing the short name of the protocol this service
     * is implementing (most often that would be a name in ProtocolNames).
     */
    public String getProtocolName()
    {
        return ProtocolNames.SIP;
    }

    /**
     * Indicates whether or not this provider is registered
     * @return true if the provider is currently registered and false otherwise.
     */
    public boolean isRegistered()
    {
        if(this.sipRegistrarConnection == null )
            return false;
        return sipRegistrarConnection.getRegistrationState()
            .equals(RegistrationState.REGISTERED);
    }

    /**
     * Removes the specified listener.
     * @param listener the listener to remove.
     */
    public void removeRegistrationStateChangeListener(
        RegistrationStateChangeListener listener)
    {
        this.registrationListeners.remove(listener);
    }

    /**
     * Returns an array containing all operation sets supported by the current
     * implementation. When querying this method users must be prepared to
     * receive any sybset of the OperationSet-s defined by this service. They
     * MUST ignore any OperationSet-s that they are not aware of and that may be
     * defined by future version of this service. Such "unknown" OperationSet-s
     * though not encouraged, may also be defined by service implementors.
     *
     * @return a java.util.Map containing instance of all supported operation
     * sets mapped against their class names (e.g.
     * OperationSetPresence.class.getName()) .
     */
    public Map getSupportedOperationSets()
    {
        return supportedOperationSets;
    }

    /**
     * Starts the registration process. Connection details such as
     * registration server, user name/number are provided through the
     * configuration service through implementation specific properties.
     *
     * @param authority the security authority that will be used for resolving
     *        any security challenges that may be returned during the
     *        registration or at any moment while wer're registered.
     * @throws OperationFailedException with the corresponding code it the
     * registration fails for some reason (e.g. a networking error or an
     * implementation problem).

     */
    public void register(SecurityAuthority authority)
        throws OperationFailedException
    {
        if (isRegistered())
            return;

        sipRegistrarConnection.register();
//
//        //at this point we are sure we have a sip: prefix in the uri
//        // we construct our pres: uri by replacing that prefix.
//        String presenceUri = "pres"
//            + publicAddress.substring(publicAddress.indexOf(':'));
//
//        presenceStatusManager.setPresenceEntityUriString(presenceUri);
//        presenceStatusManager.addContactUri(publicAddress,
//                                            PresenceStatusManager.DEFAULT_CONTACT_PRIORITY);

    }

    /**
     * Ends the registration of this protocol provider with the current
     * registration service.
     *
     * @throws OperationFailedException with the corresponding code it the
     * registration fails for some reason (e.g. a networking error or an
     * implementation problem).
     */
    public void unregister()
        throws OperationFailedException
    {
        if(!isRegistered())
            return;

        sipRegistrarConnection.unregister();
    }


    /**
     * Initializes the service implementation, and puts it in a state where it
     * could interoperate with other services.
     *
     * @param sipAddress the account id/uin/screenname of the account that we're
     * about to create
     * @param accountID the identifier of the account that this protocol
     * provider represents.
     *
     * @throws OperationFailedException with code INTERNAL_ERROR if we fail
     * initializing the SIP Stack.
     * @throws java.lang.IllegalArgumentException if one or more of the account
     * properties have invalid values.
     *
     * @see net.java.sip.communicator.service.protocol.AccountID
     */
    protected void initialize(String    sipAddress,
                              AccountID accountID)
        throws OperationFailedException, IllegalArgumentException
    {
        synchronized (initializationLock)
        {
            this.accountID = accountID;

            sipFactory = SipFactory.getInstance();
            sipFactory.setPathName("gov.nist");
            Properties properties = new Properties();

            //init the proxy
            initOutboundProxy(accountID, properties);

            // If you want to use UDP then uncomment this.
            properties.setProperty(JSPNAME_STACK_NAME, "SIP Communicator:"
                + getAccountID().getAccountUniqueID());

            // NIST SIP specific properties
            properties.setProperty(NSPNAME_DEBUG_LOG, NSPVALUE_DEBUG_LOG);
            properties.setProperty(NSPNAME_SERVER_LOG, NSPVALUE_SERVER_LOG);

            // Drop the client connection after we are done with the transaction.
            properties.setProperty( NSPNAME_CACHE_CLIENT_CONNECTIONS
                                   , NSPVALUE_CACHE_CLIENT_CONNECTIONS);

            // Log level
            properties.setProperty(NSPNAME_TRACE_LEVEL, NSPVALUE_TRACE_LEVEL);

            try
            {
                // Create SipStack object
                jainSipStack = sipFactory.createSipStack(properties);
                logger.debug("Created stack: " + jainSipStack);
            }
            catch (PeerUnavailableException e)
            {
                // could not find
                // gov.nist.jain.protocol.ip.sip.SipStackImpl
                // in the classpath
                logger.fatal("Failed to initialize SIP Stack.", e);
                throw new OperationFailedException("Failed to create sip stack"
                    , OperationFailedException.INTERNAL_ERROR
                    , e);
            }

            //init proxy port
            int preferredSipPort = ListeningPoint.PORT_5060;

            String proxyPortStr = (String) accountID.getAccountProperties()
                .get(PREFERRED_SIP_PORT);

            if (proxyPortStr != null && proxyPortStr.length() > 0)
            {
                try
                {
                    preferredSipPort = Integer.parseInt(proxyPortStr);
                }
                catch (NumberFormatException ex)
                {
                    logger.error(
                        proxyPortStr
                        + " is not a valid port value. Expected an integer"
                        , ex);
                }

                if (preferredSipPort > NetworkUtils.MAX_PORT_NUMBER)
                    logger.error(preferredSipPort + " is larger than "
                        + NetworkUtils.MAX_PORT_NUMBER + " and does not "
                        + "therefore represent a valid port nubmer.");
            }

            initListeningPoints(preferredSipPort);

            try
            {
                headerFactory = sipFactory.createHeaderFactory();
                addressFactory = sipFactory.createAddressFactory();
                messageFactory = sipFactory.createMessageFactory();
            }
            catch (PeerUnavailableException ex)
            {
                logger.fatal("Failed to ininit SIP factories", ex);
                throw new OperationFailedException(
                    "Failed to init SIP factories"
                    , OperationFailedException.INTERNAL_ERROR
                    , ex);
            }
            initRegistrarConnection(accountID);

            //create our own address.
            String ourUserID = (String)accountID.getAccountProperties()
                .get(ProtocolProviderFactory.USER_ID);

            ourDisplayName = (String)accountID.getAccountProperties()
                .get(ProtocolProviderFactory.DISPLAY_NAME);

            String registrarAddressStr = sipRegistrarConnection
                .getRegistrarAddress().getHostAddress();

            SipURI ourSipURI = null;
            try
            {
                ourSipURI = addressFactory.createSipURI(
                    ourUserID, registrarAddressStr);

                if(ourDisplayName == null
                   || ourDisplayName.trim().length() == 0)
                {
                    ourDisplayName = ourUserID;
                }
                ourSipAddress = addressFactory.createAddress(
                    ourDisplayName, ourSipURI);
            }
            catch (ParseException ex)
            {
                throw new IllegalArgumentException(
                    "Could not create a SIP URI for user " + ourUserID
                    + " and registrar " + registrarAddressStr);

            }

            isInitialized = true;
        }
    }

    /**
     * Traverses all addresses available at this machine and creates a listening
     * points on every one of them. The listening points would be initially
     * bound to the <tt>preferredPortNumber</tt> indicated by the user. If that
     * fails a new random port will be tried. The whole procedure is repeated
     * as many times as specified in the BIND_RETRIES property.
     *
     * @param preferredPortNumber the port number that we'd like listening
     * points to be bound to.
     *
     * @throws OperationFailedException with code NETWORK_ERROR if we faile
     * to bind on a local port while and code INTERNAL_ERROR if a jain-sip
     * operation fails for some reason.
     */
    private void initListeningPoints(int preferredPortNumber)
        throws OperationFailedException
    {
        try
        {
            String bindRetriesStr
                = SipActivator.getConfigurationService().getString(
                    BIND_RETRIES_PROPERTY_NAME);

            int bindRetries = 5;

            if (bindRetriesStr != null)
            {
                try
                {
                    bindRetries = Integer.parseInt(bindRetriesStr);
                }
                catch (NumberFormatException ex)
                {
                    logger.error(bindRetriesStr
                                 + " does not appear to be an integer. "
                                 + "Defaulting port bind retries to 5.", ex);
                    bindRetries = 5;
                }
            }

            int currentlyTriedPort = preferredPortNumber;

            //we'll first try to bind to the port specified by the user. if
            //this fails we'll try again times (bindRetries times in all) until
            //we find a free local port.
            for (int i = 0; i < bindRetries; i++)
            {
                try
                {
                    udpListeningPoint = jainSipStack.createListeningPoint(
                        NetworkUtils.IN_ADDR_ANY
                        , currentlyTriedPort
                        , ListeningPoint.UDP);
                }
                catch (InvalidArgumentException exc)
                {
                    if (!exc.getMessage().contains(
                        "Address already in use"))
                    {
                        logger.fatal("An exception occurred while "
                                     + "trying to create a listening point.", exc);
                        throw new OperationFailedException(
                            "An error occurred while creating listening points. "
                            , OperationFailedException.NETWORK_FAILURE
                            , exc
                            );
                    }
                    //port seems to be taken. try another one.
                    logger.debug("Port " + currentlyTriedPort
                                 + " seems in use.");
                    currentlyTriedPort
                        = NetworkUtils.getRandomPortNumber();
                    logger.debug("Retrying bind on port "
                                 + currentlyTriedPort);
                }
            }

            try
            {
                jainSipProvider
                    = jainSipStack.createSipProvider(udpListeningPoint);
                jainSipProvider.addSipListener(this);
            }
            catch (ObjectInUseException ex)
            {
                logger.fatal("Failed to create a listening point", ex);
                throw new OperationFailedException(
                    "An error occurred while creating SIP Provider for "
                    +"listening point " + udpListeningPoint.toString()
                    , OperationFailedException.INTERNAL_ERROR
                    , ex);
            }

            logger.debug("Created listening point and SIP provider for "
                         + " address:[" + udpListeningPoint.getIPAddress() + "]:"
                         + udpListeningPoint.getPort());
        }
        catch (TransportNotSupportedException ex)
        {
            logger.fatal("Failed to create a listening point", ex);
            throw new OperationFailedException(
                    "A unexpected error occurred while creating listening point"
                    , OperationFailedException.INTERNAL_ERROR
                    , ex);

        }
        catch (TooManyListenersException ex)
        {
            logger.fatal("Failed to add a provider listener", ex);
            throw new OperationFailedException(
                    "A unexpected error occurred while creating listening point"
                    , OperationFailedException.INTERNAL_ERROR
                    , ex);
        }

        logger.trace("Done creating listening points.");
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
        /**@todo implement processIOException() */
        logger.debug("@todo implement processIOException()");
    }

    /**
     * Processes a Response received on a SipProvider upon which this
     * SipListener is registered.
     * <p>
     *
     * @param responseEvent -
     *            the responseEvent fired from the SipProvider to the
     *            SipListener representing a Response received from the network.
     */
    public void processResponse(ResponseEvent responseEvent)
    {
        /**@todo implement processResponse() */
        logger.debug("@todo implement processResponse()");
    }

    /**
     * Processes a retransmit or expiration Timeout of an underlying
     * {@link Transaction} handled by this SipListener. This Event notifies the
     * application that a retransmission or transaction Timer expired in the
     * SipProvider's transaction state machine. The TimeoutEvent encapsulates
     * the specific timeout type and the transaction identifier either client or
     * server upon which the timeout occured. The type of Timeout can by
     * determined by:
     * <code>timeoutType = timeoutEvent.getTimeout().getValue();</code>
     *
     * @param timeoutEvent -
     *            the timeoutEvent received indicating either the message
     *            retransmit or transaction timed out.
     */
    public void processTimeout(TimeoutEvent timeoutEvent)
    {
        /**@todo implement processTimeout() */
        logger.debug("@todo implement processTimeout()");
    }

    /**
     * Process an asynchronously reported TransactionTerminatedEvent.
     * When a transaction transitions to the Terminated state, the stack
     * keeps no further records of the transaction. This notification can be used by
     * applications to clean up any auxiliary data that is being maintained
     * for the given transaction.
     *
     * @param transactionTerminatedEvent -- an event that indicates that the
     *       transaction has transitioned into the terminated state.
     * @since v1.2
     */
    public void processTransactionTerminated(TransactionTerminatedEvent
                                             transactionTerminatedEvent)
    {
        /**@todo implement processTransactionTerminated() */
        logger.debug("@todo implement processTransactionTerminated()");
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
        /**@todo implement processDialogTerminated() */
        logger.debug("@todo implement processDialogTerminated()");
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
        /**@todo implement processRequest() */
        logger.debug("@todo implement processRequest()");
    }

    /**
     * Makes the service implementation close all open sockets and release
     * any resources that it might have taken and prepare for shutdown/garbage
     * collection.
     */
    public void shutdown()
    {
        if(!isInitialized)
            return;

        try
        {
            jainSipProvider.removeListeningPoint(udpListeningPoint);
        }
        catch (ObjectInUseException ex)
        {
            logger.info("An exception occurred while ", ex);
        }

        udpListeningPoint = null;
        jainSipProvider = null;
        headerFactory = null;
        messageFactory = null;
        addressFactory = null;
        sipFactory = null;

        this.jainSipStack.stop();

        isInitialized = false;
    }

    /**
     * Generate a tag for a FROM header or TO header. Just return a random 4
     * digit integer (should be enough to avoid any clashes!) Tags only need to
     * be unique within a call.
     *
     * @return a string that can be used as a tag parameter.
     *
     * synchronized: needed for access to 'rand', else risk to generate same tag
     * twice
     */
    public static synchronized String generateLocalTag()
    {
            return Integer.toHexString(rand.nextInt());
    }

    /**
     * Initializes and returns an ArrayList with a single ViaHeader
     * containing a localhost address usable with the specified
     * s<tt>destination</tt>. This ArrayList may be used when sending
     * requests to that destination.
     * <p>
     * @param destination The address of the destination that the request using
     * the via headers will be sent to.
     * @param srcListeningPoint the listening point that we will be using when
     * accessing destination.
     *
     * @return ViaHeader-s list to be used when sending requests.
     * @throws OperationFailedException code INTERNAL_ERROR if a ParseException
     * occurs while initializing the array list.
     *
     */
    public ArrayList getLocalViaHeaders(InetAddress destination,
                                        ListeningPoint srcListeningPoint)
        throws OperationFailedException
    {
        ArrayList viaHeaders = new ArrayList();
        try
        {
            InetAddress localAddress = SipActivator
                .getNetworkAddressManagerService().getLocalHost(destination);
            ViaHeader viaHeader = headerFactory.createViaHeader(
                localAddress.getHostAddress()
                , srcListeningPoint.getPort()
                , srcListeningPoint.getTransport()
                , null
                );
            viaHeaders.add(viaHeader);
            logger.debug("generated via headers:" + viaHeader);
            return viaHeaders;
        }
        catch (ParseException ex)
        {
            logger.error(
                "A ParseException occurred while creating Via Headers!", ex);
            throw new OperationFailedException(
                "A ParseException occurred while creating Via Headers!"
                ,OperationFailedException.INTERNAL_ERROR
                ,ex);
        }
        catch (InvalidArgumentException ex)
        {
            logger.error(
                "Unable to create a via header for port "
                + udpListeningPoint.getPort(),
                ex);
            throw new OperationFailedException(
                "Unable to create a via header for port "
                + udpListeningPoint.getPort()
                ,OperationFailedException.INTERNAL_ERROR
                ,ex);
        }
    }

    /**
     * Initializes and returns this provider's default maxForwardsHeader field
     * using the value specified by MAX_FORWARDS.
     *
     * @return an instance of a MaxForwardsHeader that can be used when
     * sending requests
     *
     * @throws OperationFailedException with code INTERNAL_ERROR if MAX_FORWARDS
     * has an invalid value.
     */
    public MaxForwardsHeader getMaxForwardsHeader() throws
        OperationFailedException
    {
        if (maxForwardsHeader == null)
        {
            try
            {
                maxForwardsHeader = headerFactory.createMaxForwardsHeader(
                    MAX_FORWARDS);
                logger.debug("generated max forwards: "
                             + maxForwardsHeader.toString());
            }
            catch (InvalidArgumentException ex)
            {
                throw new OperationFailedException(
                    "A problem occurred while creating MaxForwardsHeader"
                    , OperationFailedException.INTERNAL_ERROR
                    , ex);
            }
        }

        return maxForwardsHeader;
    }

    /**
     * Retrns a Contact header containing our sip uri and therefore usable in all
     * but REGISTER requests. Same as calling getContactHeader(false)
     *
     * @return a Contact header containing our sip uri
     */
    public ContactHeader getContactHeader()
    {
        if(this.genericContactHeader == null)
        {
                genericContactHeader = headerFactory.createContactHeader(
                    ourSipAddress);
                logger.debug("generated contactHeader:"
                             + genericContactHeader);

        }
        return genericContactHeader;
    }

    /**
     * Retrns a Contact header containing a sip URI base on a localhost address
     * and thereforeusable in REGISTER requests only.
     *
     * @param registrarAddress the address of the registrar that this contact
     * header is meant for.
     * @param srcListeningPoint the listening point that will be used when
     * accessing the registrar.
     *
     * @return a Contact header based upon a local inet address.
     * @throws OperationFailedException if we fail constructing the contact
     * header.
     */
    ContactHeader getRegistrationContactHeader(InetAddress registrarAddress,
                                               ListeningPoint srcListeningPoint)
        throws OperationFailedException
    {
        ContactHeader registrationContactHeader = null;
        try
        {
            InetAddress localAddress = SipActivator
                .getNetworkAddressManagerService()
                    .getLocalHost(registrarAddress);

            SipURI contactURI = (SipURI) addressFactory.createSipURI(
                ((SipURI)ourSipAddress.getURI()).getUser()
                , localAddress.getHostAddress());

            contactURI.setTransportParam(srcListeningPoint.getTransport());
            contactURI.setPort(srcListeningPoint.getPort());
            Address contactAddress = addressFactory.createAddress( contactURI );

            if (ourDisplayName != null)
            {
                contactAddress.setDisplayName(ourDisplayName);
            }
            registrationContactHeader = headerFactory.createContactHeader(
                contactAddress);
            logger.debug("generated contactHeader:"
                         + registrationContactHeader);
        }
        catch (ParseException ex)
        {
            logger.error(
                "A ParseException occurred while creating From Header!", ex);
            throw new OperationFailedException(
                "A ParseException occurred while creating From Header!"
                , OperationFailedException.INTERNAL_ERROR
                , ex);
        }
        return registrationContactHeader;
    }

    /**
     * Returns the AddressFactory used to create URLs ans Address objects.
     *
     * @return the AddressFactory used to create URLs ans Address objects.
     */
    public AddressFactory getAddressFactory()
    {
        return addressFactory;
    }

    /**
     * Returns the HeaderFactory used to create SIP message headers.
     *
     * @return the HeaderFactory used to create SIP message headers.
     */
    public HeaderFactory getHeaderFactory()
    {
        return headerFactory;
    }

    /**
     * Returns the Message Factory used to create SIP messages.
     *
     * @return the Message Factory used to create SIP messages.
     */
    public MessageFactory getMessageFactory()
    {
        return messageFactory;
    }

    /**
     * Returns the sipStack instance that handles SIP communications.
     *
     * @return  the sipStack instance that handles SIP communications.
     */
    public SipStack getJainSipStack()
    {
        return jainSipStack;
    }

    /**
     * Returns he default listening point that we use for UDP communication.
     *
     * @return the default listening point that we use for UDP communication.
     */
    public ListeningPoint getUdpListeningPoint()
    {
        return udpListeningPoint;
    }

    /**
     * The JAIN SIP SipProvider instance.
     *
     * @return the JAIN SIP SipProvider instance.
     */
    public SipProvider getJainSipProvider()
    {
        return jainSipProvider;
    }

    /**
     * Initializes the SipRegistrarConnection that this class will be using.
     *
     * @param accountID the ID of the account that this registrar is associated
     * with.
     * @throws java.lang.IllegalArgumentException if one or more account
     * properties have invalid values.
     */
    private void initRegistrarConnection(AccountID accountID)
        throws IllegalArgumentException
    {
        //First init the registrar address
        String registrarAddressStr = (String) accountID.getAccountProperties()
            .get(ProtocolProviderFactory.SERVER_ADDRESS);

        InetAddress registrarAddress = null;
        try
        {
            registrarAddress = InetAddress.getByName(registrarAddressStr);
        }
        catch (UnknownHostException ex)
        {
            throw new IllegalArgumentException(
                registrarAddressStr
                + " appears to be an either invalid or inaccessible address"
                , ex);
        }

        //init registrar port
        int registrarPort = ListeningPoint.PORT_5060;

        String registrarPortStr = (String) accountID.getAccountProperties()
            .get(ProtocolProviderFactory.SERVER_PORT);

        if(registrarPortStr != null && registrarPortStr.length() > 0)
        {
            try
            {
                registrarPort = Integer.parseInt(registrarPortStr);
            }
            catch (NumberFormatException ex)
            {
                logger.error(
                    registrarPortStr
                    + " is not a valid port value. Expected an integer"
                    , ex);
            }

            if ( registrarPort > NetworkUtils.MAX_PORT_NUMBER)
                throw new IllegalArgumentException(registrarPort
                    + " is larger than " + NetworkUtils.MAX_PORT_NUMBER
                    + " and does not therefore represent a valid port nubmer.");
        }

        //registrar transport
        String registrarTransport = (String) accountID.getAccountProperties()
            .get(ProtocolProviderFactory.SERVER_TRANSPORT);

        if(registrarPortStr != null && registrarPortStr.length() > 0)
        {
            if( ! registrarTransport.equals(ListeningPoint.UDP)
                || !registrarTransport.equals(ListeningPoint.TCP)
                || !registrarTransport.equals(ListeningPoint.TLS))
            throw new IllegalArgumentException(registrarTransport
                + " is not a valid transport protocol. Transport must be left "
                + "blanc or set to TCP, UDP or TLS.");
        }
        else
        {
            registrarTransport = ListeningPoint.UDP;
        }

        //init expiration timeout
        int expires = SipRegistrarConnection.DEFAULT_REGISTRATION_EXPIRATION;

        String expiresStr = SipActivator.getConfigurationService().getString(
            REGISTRATION_EXPIRATION);

        if(expiresStr != null && expiresStr.length() > 0)
        {
            try
            {
                expires = Integer.parseInt(expiresStr);
            }
            catch (NumberFormatException ex)
            {
                logger.error(
                    expiresStr
                    + " is not a valid expires  value. Expexted an integer"
                    , ex);
            }
        }


        try
        {
            this.sipRegistrarConnection = new SipRegistrarConnection(
                registrarAddress
                , registrarPort
                , registrarTransport
                , expires
                , this);
        }
        catch (ParseException ex)
        {
            //this really shouldn't happen as we're using InetAddress-es
            throw new IllegalArgumentException(
                "Failed to create a registrar connection with "
                +registrarAddress.getHostAddress()
                , ex);
        }
    }

    /**
     * Extracts all properties concerning the usage of an outbound proxy for
     * this account.
     * @param accountID the acount whose ourbound proxy we are currently
     * initializing.
     * @param jainSipProperties the properties that we will be passing to the
     * jain sip stack when initialize it (that's where we'll put all proxy
     * properties).
     */
    private void initOutboundProxy(AccountID accountID,
                                   Hashtable jainSipProperties)
    {
        //First init the proxy address
        String proxyAddressStr = (String) accountID.getAccountProperties()
            .get(ProtocolProviderFactory.PROXY_ADDRESS);

        InetAddress proxyAddress = null;

        //return if no proxy is specified.
        if(proxyAddressStr == null || proxyAddressStr.length() == 0)
            return;

        try
        {
            proxyAddress = InetAddress.getByName(proxyAddressStr);
        }
        catch (UnknownHostException ex)
        {
            throw new IllegalArgumentException(
                proxyAddressStr
                + " appears to be an either invalid or inaccessible address"
                , ex);
        }

        //init proxy port
        int proxyPort = ListeningPoint.PORT_5060;

        String proxyPortStr = (String) accountID.getAccountProperties()
            .get(ProtocolProviderFactory.PROXY_PORT);

        if (proxyPortStr != null && proxyPortStr.length() > 0)
        {
            try
            {
                proxyPort = Integer.parseInt(proxyPortStr);
            }
            catch (NumberFormatException ex)
            {
                logger.error(
                    proxyPortStr
                    + " is not a valid port value. Expected an integer"
                    , ex);
            }

            if (proxyPort > NetworkUtils.MAX_PORT_NUMBER)
                throw new IllegalArgumentException(proxyPort
                    + " is larger than " +
                    NetworkUtils.MAX_PORT_NUMBER
                    +
                    " and does not therefore represent a valid port nubmer.");
        }

        //proxy transport
        String proxyTransport = (String) accountID.getAccountProperties()
            .get(ProtocolProviderFactory.PROXY_TRANSPORT);

        if (proxyPortStr != null && proxyPortStr.length() > 0)
        {
            if (!proxyTransport.equals(ListeningPoint.UDP)
                || !proxyTransport.equals(ListeningPoint.TCP)
                || !proxyTransport.equals(ListeningPoint.TLS))
                throw new IllegalArgumentException(proxyTransport
                    + " is not a valid transport protocol. Transport must be "
                    + "left blanc or set to TCP, UDP or TLS.");
        }
        else
        {
            proxyTransport = ListeningPoint.UDP;
        }

        StringBuffer proxyStringBuffer = new StringBuffer(proxyAddress.getHostAddress());

        if(proxyAddress instanceof Inet6Address)
        {
            proxyStringBuffer.insert(0, '[');
            proxyStringBuffer.append(']');
        }

        proxyStringBuffer.append(':');
        proxyStringBuffer.append(Integer.toString(proxyPort));
        proxyStringBuffer.append('/');
        proxyStringBuffer.append(proxyTransport);

        //done parsing. iInit properties.
        jainSipProperties.put(this.JSPNAME_OUTBOUND_PROXY
                              , proxyStringBuffer.toString());

    }
}
