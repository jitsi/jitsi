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
import net.java.sip.communicator.service.version.*;
import net.java.sip.communicator.util.*;
import gov.nist.javax.sip.*;
import gov.nist.javax.sip.header.*;
import gov.nist.javax.sip.address.*;
import gov.nist.javax.sip.message.*;
import gov.nist.javax.sip.stack.*;
import net.java.sip.communicator.impl.protocol.sip.security.*;

/**
 * A SIP implementation of the Protocol Provider Service.
 *
 * @author Emil Ivov
 */
public class ProtocolProviderServiceSipImpl
  extends AbstractProtocolProviderService
  implements SipListener
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
     * A list of all events registered for this provider.
     */
    private List registeredEvents = new ArrayList();

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
     * The properties of the jainSipStack.
     */
    private Properties jainSipStackProperties;

    /**
     * The default listening point that we use for UDP communication..
     */
    private ListeningPoint udpListeningPoint = null;

    /**
     * The default listening point that we use for TCP communication..
     */
    private ListeningPoint tcpListeningPoint = null;

    /**
     * The default listening point that we use for TLS communication..
     */
    private ListeningPoint tlsListeningPoint = null;

    /**
     * The default JAIN SIP provider that we use for UDP communication...
     */
    private SipProvider udpJainSipProvider = null;

    /**
     * The default JAIN SIP provider that we use for TCP communication...
     */
    private SipProvider tcpJainSipProvider = null;

    /**
     * The default JAIN SIP provider that we use for TLS communication...
     */
    private SipProvider tlsJainSipProvider = null;

    /**
     * A table mapping SIP methods to method processors (every processor must
     * implement the SipListener interface). Whenever a new message arrives we
     * extract its method and hand it to the processor instance registered
     */
    private Hashtable methodProcessors = new Hashtable();

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
    private static String NSPVALUE_DEBUG_LOG = "log/sc-jainsipdebug.log";

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
    private static String NSPVALUE_SERVER_LOG  = "log/sc-jainsipserver.log";

    /**
     * The name of the property under which jain-sip will know if it must
     * deliver some unsolicited notify.
     */
    private static final String NSPNAME_DELIVER_UNSOLICITED_NOTIFY =
        "gov.nist.javax.sip.DELIVER_UNSOLICITED_NOTIFY";

    /**
     * The value of the property under which jain-sip will know if it must
     * deliver some unsolicited notify.
     */
    private static final String NSPVALUE_DELIVER_UNSOLICITED_NOTIFY = "true";

    /**
     * A random generator we use to generate tags.
     */
    private static Random localTagGenerator = new Random();

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
    private static final String NSPVALUE_TRACE_LEVEL = "ERROR";

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
        "net.java.sip.communicator.service.protocol.sip.PREFERRED_SIP_PORT";


    /**
     * The name of the property under which the user may specify the number of
     * seconds that registrations take to expire.
     */
    private static final String REGISTRATION_EXPIRATION =
        "net.java.sip.communicator.impl.protocol.sip.REGISTRATION_EXPIRATION";

    /**
     * The name of the property under which the user may specify whether or not
     * REGISTER requests should be using a route header. Default is false
     */
    private static final String REGISTERS_USE_ROUTE =
        "net.java.sip.communicator.impl.protocol.sip.REGISTERS_USE_ROUTE";

    /**
     * A default specifyier telling the stack whether or not to cache client
     * connections.
     */
    private static final String NSPVALUE_CACHE_CLIENT_CONNECTIONS = "true";

    /**
     * The name of the property under which the user may specify a transport
     * to use for destinations whose prefererred transport is unknown.
     */
    private static final String DEFAULT_TRANSPORT
        = "net.java.sip.communicator.impl.protocol.sip.DEFAULT_TRANSPORT";

    /**
     * Default number of times that our requests can be forwarded.
     */
    private static final int  MAX_FORWARDS = 70;

    /**
     * Keep-alive method can be - register,options or udp
     */
    public static final String KEEP_ALIVE_METHOD = "KEEP_ALIVE_METHOD";

    /**
     * The interval for keep-alive
     */
    public static final String KEEP_ALIVE_INTERVAL = "KEEP_ALIVE_INTERVAL";

    /**
     * The default maxForwards header that we use in our requests.
     */
    private MaxForwardsHeader maxForwardsHeader = null;

    /**
     * The contact header we use in non REGISTER requests.
     */
    private ContactHeader genericContactHeader = null;

    /**
     * The header that we use to identify ourselves.
     */
    private UserAgentHeader userAgentHeader = null;

    /**
     * The sip address that we're currently behind (the one that corresponds to
     * our account id).
     */
    private Address ourSipAddress = null;

    /**
     * The name that we want to send others when calling or chatting with them.
     */
    private String ourDisplayName = null;

    /**
     * Our current connection with the registrar.
     */
    private SipRegistrarConnection sipRegistrarConnection = null;

    /**
     * The SipSecurityManager instance that would be taking care of our
     * authentications.
     */
    private SipSecurityManager sipSecurityManager = null;

    /**
     * The address and port of an outbound proxy if we have one (remains null
     * if we are not using a proxy).
     */
    private InetSocketAddress outboundProxySocketAddress = null;

    /**
     * The transport used by our outbound proxy (remains null
     * if we are not using a proxy).
     */
    private String outboundProxyTransport = null;

    /**
     * The logo corresponding to the jabber protocol.
     */
    private ProtocolIconSipImpl protocolIcon;

    private SipStatusEnum sipStatusEnum;

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
        {
            return RegistrationState.UNREGISTERED;
        }
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
     * Register a new event taken in account by this provider. This is usefull
     * to generate the Allow-Events header of the OPTIONS responses and to
     * generate 489 responses.
     *
     * @param event The event to register
     */
    public void registerEvent(String event)
    {
        synchronized (this.registeredEvents) {
            if (!this.registeredEvents.contains(event)) {
                this.registeredEvents.add(event);
            }
        }
    }

    /**
     * Returns the list of all the registered events for this provider.
     *
     * @return The list of all the registered events
     */
    public List getKnownEventsList()
    {
        return this.registeredEvents;
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
     * Returns the operation set corresponding to the specified class or null
     * if this operation set is not supported by the provider implementation.
     *
     * @param opsetClass the <tt>Class</tt>  of the operation set that we're
     * looking for.
     * @return returns an OperationSet of the specified <tt>Class</tt> if the
     * undelying implementation supports it or null otherwise.
     */
    public OperationSet getOperationSet(Class opsetClass)
    {
        return (OperationSet)getSupportedOperationSets()
            .get(opsetClass.getName());
    }



    /**
     * Starts the registration process. Connection details such as
     * registration server, user name/number are provided through the
     * configuration service through implementation specific properties.
     *
     * @param authority the security authority that will be used for resolving
     *        any security challenges that may be returned during the
     *        registration or at any moment while wer're registered.
     *
     * @throws OperationFailedException with the corresponding code it the
     * registration fails for some reason (e.g. a networking error or an
     * implementation problem).

     */
    public void register(SecurityAuthority authority)
        throws OperationFailedException
    {
        if(!isInitialized)
        {
            throw new OperationFailedException(
                "Provided must be initialized before being able to register."
                , OperationFailedException.GENERAL_ERROR);
        }

        if (isRegistered())
        {
            return;
        }

        // Enable the user name modification. Setting this property to true we'll
        // allow the user to change the user name stored in the given authority.
        authority.setUserNameEditable(true);

        //init the security manager before doing the actual registration to
        //avoid being asked for credentials before being ready to provide them
        sipSecurityManager.setSecurityAuthority(authority);

        // We check here if the sipRegistrarConnection is initialized. This is
        // needed in case that in the initialization process we had no internet
        // connection.
        if (sipRegistrarConnection == null)
            initRegistrarConnection((SipAccountID) accountID);

        // The same here, we check if the outbound proxy is initialized in case
        // through the initialization process there was no internet connection.
        if (outboundProxySocketAddress == null)
            initOutboundProxy((SipAccountID)accountID, jainSipStackProperties);

        //connect to the Registrar.
        if (sipRegistrarConnection != null)
            sipRegistrarConnection.register();
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
        if(getRegistrationState().equals(RegistrationState.UNREGISTERED)
            || getRegistrationState().equals(RegistrationState.UNREGISTERING))
        {
            return;
        }

        sipRegistrarConnection.unregister();
        sipSecurityManager.setSecurityAuthority(null);
    }

    /**
     * Initializes the service implementation, and puts it in a state where it
     * could interoperate with other services.
     *
     * @param sipAddress the account id/uin/screenname of the account that we're
     * about to create
     * @param accountID the identifier of the account that this protocol
     * provider represents.
     * @param isInstall indicates if this initialization is made due to a new
     * account installation or just an existing account loading
     *
     * @throws OperationFailedException with code INTERNAL_ERROR if we fail
     * initializing the SIP Stack.
     * @throws java.lang.IllegalArgumentException if one or more of the account
     * properties have invalid values.
     *
     * @see net.java.sip.communicator.service.protocol.AccountID
     */
    protected void initialize(String    sipAddress,
                              SipAccountID accountID)
        throws OperationFailedException, IllegalArgumentException
    {
        synchronized (initializationLock)
        {
            String logDir
                = SipActivator.getConfigurationService().getScHomeDirLocation()
                + System.getProperty("file.separator")
                + SipActivator.getConfigurationService().getScHomeDirName()
                + System.getProperty("file.separator");

            // don't do it more than one time if many provider are initialised
            if (!NSPVALUE_DEBUG_LOG.startsWith(logDir)) {
                NSPVALUE_DEBUG_LOG = logDir + NSPVALUE_DEBUG_LOG;
            }

            if (!NSPVALUE_SERVER_LOG.startsWith(logDir)) {
                NSPVALUE_SERVER_LOG = logDir + NSPVALUE_SERVER_LOG;
            }

            this.accountID = accountID;

            String protocolIconPath = (String) accountID.getAccountProperties()
                .get(ProtocolProviderFactory.PROTOCOL_ICON_PATH);

            if (protocolIconPath == null)
                protocolIconPath = "resources/images/protocol/sip";

            this.protocolIcon = new ProtocolIconSipImpl(protocolIconPath);

            this.sipStatusEnum = new SipStatusEnum(protocolIconPath);

            sipFactory = SipFactory.getInstance();
            sipFactory.setPathName("gov.nist");
            this.jainSipStackProperties = new Properties();

            //init the proxy
            initOutboundProxy(accountID, jainSipStackProperties);

            // If you want to use UDP then uncomment this.
            jainSipStackProperties.setProperty(
                JSPNAME_STACK_NAME, "SIP Communicator:"
                + getAccountID().getAccountUniqueID());

            // NIST SIP specific properties
            jainSipStackProperties
                .setProperty(NSPNAME_DEBUG_LOG, NSPVALUE_DEBUG_LOG);

            jainSipStackProperties
                .setProperty(NSPNAME_SERVER_LOG, NSPVALUE_SERVER_LOG);

            // Drop the client connection after we are done with the transaction.
            jainSipStackProperties
                .setProperty(   NSPNAME_CACHE_CLIENT_CONNECTIONS,
                                NSPVALUE_CACHE_CLIENT_CONNECTIONS);

            // Log level
            jainSipStackProperties
                .setProperty(NSPNAME_TRACE_LEVEL, NSPVALUE_TRACE_LEVEL);

            // deliver unsolicited NOTIFY
            jainSipStackProperties
                .setProperty(   NSPNAME_DELIVER_UNSOLICITED_NOTIFY,
                                NSPVALUE_DELIVER_UNSOLICITED_NOTIFY);

            try
            {
                // Create SipStack object
                jainSipStack = new SipStackImpl(jainSipStackProperties);
                logger.debug("Created stack: " + jainSipStack);
            }
            catch (PeerUnavailableException exc)
            {
                // could not find
                // gov.nist.jain.protocol.ip.sip.SipStackImpl
                // in the classpath
                logger.fatal("Failed to initialize SIP Stack.", exc);
                throw new OperationFailedException("Failed to create sip stack"
                    , OperationFailedException.INTERNAL_ERROR
                    , exc);
            }

            //init proxy port
            int preferredSipPort = ListeningPoint.PORT_5060;

            String proxyPortStr = SipActivator.getConfigurationService().
                    getString(PREFERRED_SIP_PORT);

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
                {
                    logger.error(preferredSipPort + " is larger than "
                                 + NetworkUtils.MAX_PORT_NUMBER + " and does not "
                                 + "therefore represent a valid port nubmer.");
                }
            }

            initListeningPoints(preferredSipPort);

            // get the presence options
            String enablePresenceObj = (String) accountID
                    .getAccountProperties().get(
                            ProtocolProviderFactory.IS_PRESENCE_ENABLED);

            boolean enablePresence = true;
            if (enablePresenceObj != null) {
                enablePresence = Boolean.valueOf(enablePresenceObj)
                    .booleanValue();
            }

            String forceP2PObj = (String) accountID.getAccountProperties()
                    .get(ProtocolProviderFactory.FORCE_P2P_MODE);

            boolean forceP2P = true;
            if (forceP2PObj != null) {
                forceP2P = Boolean.valueOf(forceP2PObj).booleanValue();
            }

            int pollingValue = 30;
            try {
                String pollingString = (String) accountID.getAccountProperties()
                        .get(ProtocolProviderFactory.POLLING_PERIOD);
                if (pollingString != null) {
                    pollingValue = Integer.parseInt(pollingString);
                } else {
                    logger.warn("no polling value found, using default value"
                            + " (" + pollingValue + ")");
                }
            } catch (NumberFormatException e) {
                logger.error("wrong polling value stored", e);
            }

            int subscriptionExpiration = 3600;
            try {
                String subscriptionString = (String) accountID
                    .getAccountProperties().get(ProtocolProviderFactory
                        .SUBSCRIPTION_EXPIRATION);
                if (subscriptionString != null) {
                    subscriptionExpiration = Integer.parseInt(
                            subscriptionString);
                } else {
                    logger.warn("no expiration value found, using default value"
                            + " (" + subscriptionExpiration + ")");
                }
            } catch (NumberFormatException e) {
                logger.error("wrong expiration value stored", e);
            }

            //create SIP factories.
            headerFactory = new HeaderFactoryImpl();
            addressFactory = new AddressFactoryImpl();
            messageFactory = new MessageFactoryImpl();

            //create a connection with the registrar
            initRegistrarConnection(accountID);

            //init our call processor
            OperationSetBasicTelephony opSetBasicTelephony
                = new OperationSetBasicTelephonySipImpl(this);
            this.supportedOperationSets.put(
                OperationSetBasicTelephony.class.getName()
                , opSetBasicTelephony);

            //init presence op set.
            OperationSetPersistentPresence opSetPersPresence
                = new OperationSetPresenceSipImpl(this, enablePresence,
                        forceP2P, pollingValue, subscriptionExpiration);
            this.supportedOperationSets.put(
                OperationSetPersistentPresence.class.getName()
                , opSetPersPresence);
            //also register with standard presence
            this.supportedOperationSets.put(
                OperationSetPresence.class.getName()
                , opSetPersPresence);

            // init instant messaging
            OperationSetBasicInstantMessagingSipImpl opSetBasicIM =
                new OperationSetBasicInstantMessagingSipImpl(this);
            this.supportedOperationSets.put(
                OperationSetBasicInstantMessaging.class.getName(),
                opSetBasicIM);

            // init typing notifications
            OperationSetTypingNotificationsSipImpl opSetTyping =
                new OperationSetTypingNotificationsSipImpl(this, opSetBasicIM);
            this.supportedOperationSets.put(
                OperationSetTypingNotifications.class.getName(),
                opSetTyping);

            // init DTMF (from JM Heitz)
            OperationSetDTMF opSetDTMF = new OperationSetDTMFSipImpl(this);
            this.supportedOperationSets.put(
                OperationSetDTMF.class.getName(), opSetDTMF);


            //create our own address.
            String ourUserID = (String)accountID.getAccountProperties()
                .get(ProtocolProviderFactory.USER_ID);

            String sipUriHost = null;
            if( ourUserID.indexOf("@") != -1
                && ourUserID.indexOf("@") < ourUserID.length() -1 )
            {
                //use the domain in the SIP uri as a default registrar address.
                sipUriHost = ourUserID.substring( ourUserID.indexOf("@") + 1 );
                ourUserID = ourUserID.substring( 0, ourUserID.indexOf("@") );
            }

            ourDisplayName = (String)accountID.getAccountProperties()
                .get(ProtocolProviderFactory.DISPLAY_NAME);

            if(sipUriHost == null)
                sipUriHost = sipRegistrarConnection
                    .getRegistrarAddress().getHostName();

            SipURI ourSipURI = null;
            try
            {
                ourSipURI = addressFactory.createSipURI(
                    ourUserID, sipUriHost);

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
                logger.error("Could not create a SIP URI for user "
                             + ourUserID + "@" + sipUriHost
                             + " and registrar " + sipRegistrarConnection
                             .getRegistrarAddress().getHostName()
                             , ex);
                throw new IllegalArgumentException(
                    "Could not create a SIP URI for user "
                    + ourUserID + "@" + sipUriHost
                    + " and registrar " + sipRegistrarConnection
                                        .getRegistrarAddress().getHostName());
            }

            //init the security manager
            this.sipSecurityManager = new SipSecurityManager(accountID);
            sipSecurityManager.setHeaderFactory(headerFactory);

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

            int bindRetries = BIND_RETRIES_DEFAULT_VALUE;

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
                    bindRetries = BIND_RETRIES_DEFAULT_VALUE;
                }
            }


            udpListeningPoint = createListeningPoint(preferredPortNumber
                                                     , ListeningPoint.UDP
                                                     , bindRetries);
            tcpListeningPoint = createListeningPoint(preferredPortNumber
                                                     , ListeningPoint.TCP
                                                     , bindRetries);

            tlsListeningPoint = createListeningPoint(ListeningPoint.PORT_5061
                                                     , ListeningPoint.TLS
                                                     , bindRetries);



            try
            {
                udpJainSipProvider
                    = jainSipStack.createSipProvider(udpListeningPoint);
                udpJainSipProvider.addSipListener(this);
                tcpJainSipProvider
                    = jainSipStack.createSipProvider(tcpListeningPoint);
                tcpJainSipProvider.addSipListener(this);
                tlsJainSipProvider
                    = jainSipStack.createSipProvider(tlsListeningPoint);
                tlsJainSipProvider.addSipListener(this);

                // set our custom address resolver managing SRV records
                AddressResolverImpl addressResolver =
                    new AddressResolverImpl();

                ((SIPTransactionStack)udpJainSipProvider.getSipStack()).
                    setAddressResolver(addressResolver);
                ((SIPTransactionStack)tcpJainSipProvider.getSipStack()).
                    setAddressResolver(addressResolver);
                ((SIPTransactionStack)tlsJainSipProvider.getSipStack()).
                    setAddressResolver(addressResolver);
            }
            catch (ObjectInUseException ex)
            {
                logger.fatal("Failed to create a SIP Provider", ex);
                throw new OperationFailedException(
                    "An error occurred while creating SIP Provider for"
                    , OperationFailedException.INTERNAL_ERROR
                    , ex);
            }

            logger.debug("Created listening points and SIP provider for account"
                         + "  " + getAccountID().toString());
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
     * Creates a listening point for the specified <tt>transport</tt> first
     * trying to bind on the <tt>preferredPortNumber</tt>. If the
     * preferredPortNumber is already used by another program or another
     * listening point, we will be retrying the operation (<tt>bindRetries</tt>
     * times in all.)
     *
     * @param preferredPortNumber the port number that we should first try to
     * bind to.
     * @param transport the transport (UDP/TCP/TLS) of the listening point that
     * we will be creating
     * @param bindRetries the number of times that we should try to bind on
     * different random ports before giving up.
     * @return the newly creaed <tt>ListeningPoint</tt> instance or
     * <tt>null</tt> if we didn't manage to create a listening point in bind
     * retries.
     *
     * @throws OperationFailedException with code NETWORK_ERROR if we faile
     * to bind on a local port while and code INTERNAL_ERROR if a jain-sip
     * operation fails for some reason.
     * @throws TransportNotSupportedException if <tt>transport</tt> is not
     * a valid SIP transport (i.e. not one of UDP/TCP/TLS).
     */
    private ListeningPoint createListeningPoint(int preferredPortNumber,
                                                String transport,
                                                int bindRetries)
        throws OperationFailedException, TransportNotSupportedException
    {
        int currentlyTriedPort = preferredPortNumber;

        ListeningPoint listeningPoint = null;

        //we'll first try to bind to the port specified by the user. if
        //this fails we'll try again times (bindRetries times in all) until
        //we find a free local port.
        for (int i = 0; i < bindRetries; i++)
        {
            try
            {
                //make sure that the we don't already have some other
                //listening point on this port (possibly initialized from a
                //different account)
                if (listeningPointAlreadyBound(currentlyTriedPort
                                           , transport))
                {
                    logger.debug("The following listeing point alredy existed "
                             + "port: " + currentlyTriedPort + " trans: "
                             + transport);
                    throw new InvalidArgumentException("Address already in use");
                }

                listeningPoint = jainSipStack.createListeningPoint(
                    NetworkUtils.IN_ADDR_ANY
                    , currentlyTriedPort
                    , transport);
                //we succeeded - break so that we don't try to bind again
                logger.debug("Created LP " + listeningPoint.getIPAddress()
                    + ":" + listeningPoint.getPort() + "/"
                    + listeningPoint.getTransport());
                try
                {
                    listeningPoint.setSentBy("0.0.0.0");
                }
                catch (ParseException ex)
                {
                }
                return listeningPoint;
            }
            catch (InvalidArgumentException exc)
            {
                if (exc.getMessage().indexOf("Address already in use")== -1)
                {
                    logger.fatal("An exception occurred while "
                                 + "trying to create a listening point."
                                 , exc);
                    throw new OperationFailedException(
                        "An error occurred while creating listening points. "
                        , OperationFailedException.NETWORK_FAILURE
                        , exc
                        );
                }
            }
            //port seems to be taken. try another one.
            logger.debug("Port " + currentlyTriedPort + " seems in use "
                         +"for transport ." + transport);
            currentlyTriedPort = NetworkUtils.getRandomPortNumber();
            logger.debug("Retrying bind on port " + currentlyTriedPort);

        }

        logger.error("Failed to create a listening point for tranport "
                     + transport);
        return null;
    }

    /**
     * Verifies whether a listening point for the specified port and transport
     * already exists. We need this method because we can only have one single
     * stack per JVM and it is possible for some other account to have already
     * created a listening point for a port that we're trying to bind on. Yet
     * that would be undesirable as we would not like to receive any of the SIP
     * messages meannt for the other account.
     *
     * @param port the port number that we're interested in.
     * @param transport the transport of the listening point we're looking for.
     *
     * @return true if a ListeningPoint already exists for the specified port
     * and tranport and false otherwise.
     */
    private boolean listeningPointAlreadyBound(int     port,
                                               String  transport)
    {
        if(jainSipStack == null)
        {
            return false;
        }

        //What really matters is not the transport of the listening point but
        //whether it is UDP or TCP (i.e. TLS listening points have to be
        //considered as TCP).
        boolean searchTransportIsUDP = transport.equals(ListeningPoint.UDP);

        Iterator existingListeningPoints = jainSipStack.getListeningPoints();

        while(existingListeningPoints.hasNext())
        {
            ListeningPoint lp = (ListeningPoint)existingListeningPoints.next();

            if(lp.getPort() == port)
            {
                boolean lpIsUDP = lp.getTransport().equalsIgnoreCase(
                                                        ListeningPoint.UDP);

                if(lpIsUDP == searchTransportIsUDP)
                {
                    return true;
                }
            }
        }

        return false;
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
     * @param responseEvent the responseEvent fired from the SipProvider to the
     * SipListener representing a Response received from the network.
     */
    public void processResponse(ResponseEvent responseEvent)
    {
        logger.debug("received response=\n" + responseEvent.getResponse());
        ClientTransaction clientTransaction = responseEvent
            .getClientTransaction();
        if (clientTransaction == null) {
            logger.debug("ignoring a transactionless response");
            return;
        }

        Response response = responseEvent.getResponse();
        String method = ( (CSeqHeader) response.getHeader(CSeqHeader.NAME))
            .getMethod();

        //find the object that is supposed to take care of responses with the
        //corresponding method
        SipListener processor = (SipListener)methodProcessors.get(method);

        if(processor == null)
        {
            return;
        }

        logger.debug("Found one processor for method " + method
                     + ", processor is=" + processor.toString());

            processor.processResponse(responseEvent);
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
        Transaction transaction;
        if(timeoutEvent.isServerTransaction())
            transaction = timeoutEvent.getServerTransaction();
        else
            transaction = timeoutEvent.getClientTransaction();

        if (transaction == null) {
            logger.debug("ignoring a transactionless timeout event");
            return;
        }

        Request request = transaction.getRequest();
        logger.debug("received timeout for req=" + request);


        //find the object that is supposed to take care of responses with the
        //corresponding method
        SipListener processor
            = (SipListener)methodProcessors.get(request.getMethod());

        if (processor == null)
        {
            return;
        }

        logger.debug("Found one processor for method " + request.getMethod()
                     + ", processor is=" + processor.toString());

            processor.processTimeout(timeoutEvent);
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
        Transaction transaction;
        if(transactionTerminatedEvent.isServerTransaction())
            transaction = transactionTerminatedEvent.getServerTransaction();
        else
            transaction = transactionTerminatedEvent.getClientTransaction();

        if (transaction == null) {
            logger.debug(
                "ignoring a transactionless transaction terminated event");
            return;
        }

        Request request = transaction.getRequest();
        logger.debug("Transaction terminated for req=" + request);


        //find the object that is supposed to take care of responses with the
        //corresponding method
        SipListener processor
            = (SipListener)methodProcessors.get(request.getMethod());

        if(processor == null)
        {
            return;
        }

        logger.debug("Found one processor for method " + request.getMethod()
                     + ", processor is=" + processor.toString());

            processor.processTransactionTerminated(transactionTerminatedEvent);
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
        logger.debug("Dialog terminated for req="
                     + dialogTerminatedEvent.getDialog());
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
        logger.debug("received request=\n" + requestEvent.getRequest());

        Request request = requestEvent.getRequest();

        // test if an Event header is present and known
        EventHeader eventHeader = (EventHeader)
            request.getHeader(EventHeader.NAME);

        if (eventHeader != null) {
            boolean eventKnown;

            synchronized (this.registeredEvents) {
                eventKnown = this.registeredEvents.contains(
                        eventHeader.getEventType());
            }

            if (!eventKnown) {
                // send a 489 / Bad Event response
                ServerTransaction serverTransaction = requestEvent
                    .getServerTransaction();
                SipProvider jainSipProvider = (SipProvider)
                    requestEvent.getSource();

                if (serverTransaction == null)
                {
                    try
                    {
                        serverTransaction = jainSipProvider
                            .getNewServerTransaction(request);
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

                Response response = null;
                try {
                    response = this.getMessageFactory().createResponse(
                            Response.BAD_EVENT, request);
                } catch (ParseException e) {
                    logger.error("failed to create the 489 response", e);
                    return;
                }

                try {
                    serverTransaction.sendResponse(response);
                } catch (SipException e) {
                    logger.error("failed to send the response", e);
                } catch (InvalidArgumentException e) {
                    // should not happen
                    logger.error("invalid argument provided while trying" +
                            " to send the response", e);
                }
            }
        }


        String method = request.getMethod();

        //find the object that is supposed to take care of responses with the
        //corresponding method
        SipListener processor = (SipListener)methodProcessors.get(method);

        if(processor == null)
        {
            return;
        }

        logger.debug("Found one processor for method " + method
                     + ", processor is=" + processor.toString());

            processor.processRequest(requestEvent);
    }

    /**
     * Makes the service implementation close all open sockets and release
     * any resources that it might have taken and prepare for shutdown/garbage
     * collection.
     */
    public void shutdown()
    {
        if(!isInitialized)
        {
            return;
        }

        // launch the shutdown process in a thread to free the GUI as soon
        // as possible even if the SIP unregistration process may take time
        // especially for ending SIMPLE
        Thread t = new Thread(new ShutdownThread());
        t.setDaemon(false);
        t.run();

    }

    protected class ShutdownThread implements Runnable
    {
        public void run() {
            logger.trace("Killing the SIP Protocol Provider.");
            //kill all active calls
            OperationSetBasicTelephonySipImpl telephony
                = (OperationSetBasicTelephonySipImpl)getOperationSet(
                    OperationSetBasicTelephony.class);
            telephony.shutdown();

            if(isRegistered())
            {
                try
                {
                    //create a listener that would notify us when unregistration
                    //has completed.
                    ShutdownUnregistrationBlockListener listener
                        = new ShutdownUnregistrationBlockListener();
                    addRegistrationStateChangeListener(listener);

                    //do the unregistration
                    unregister();

                    //leave ourselves time to complete unregistration (may include
                    //2 REGISTER requests in case notification is needed.)
                    listener.waitForEvent(5000);
                }
                catch (OperationFailedException ex)
                {
                    //we're shutting down so we need to silence the exception here
                    logger.error(
                        "Failed to properly unregister before shutting down. "
                        + getAccountID()
                        , ex);
                }
            }

            try
            {
                udpJainSipProvider.removeListeningPoint(udpListeningPoint);
                tcpJainSipProvider.removeListeningPoint(tcpListeningPoint);
                tlsJainSipProvider.removeListeningPoint(tlsListeningPoint);
            }
            catch (ObjectInUseException ex)
            {
                logger.info("An exception occurred while ", ex);
            }

            try
            {
//                this.jainSipStack.stop();
            }
            catch (Exception ex)
            {
                //catch anything the stack can throw at us here so that we could
                //peacefully finish our shutdown.
                logger.error("Failed to properly stop the stack!", ex);
            }

            udpListeningPoint = null;
            tcpListeningPoint = null;
            tlsListeningPoint = null;
            udpJainSipProvider = null;
            tcpJainSipProvider = null;
            tlsJainSipProvider = null;
            headerFactory = null;
            messageFactory = null;
            addressFactory = null;
            sipFactory = null;
            sipSecurityManager = null;

            methodProcessors.clear();

            isInitialized = false;
        }
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
            return Integer.toHexString(localTagGenerator.nextInt());
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
            try
            {
                genericContactHeader = getContactHeader(
                    sipRegistrarConnection.getRegistrarAddress(),
                    sipRegistrarConnection.getRegistrarListeningPoint());
            }
            catch(OperationFailedException ex)
            {
                logger.error("Failed to create Contact header.", ex);
            }
            logger.debug("generated contactHeader:"
                         + genericContactHeader);
        }
        return genericContactHeader;
    }

    /**
     * Retrns a Contact header containing a sip URI base on a localhost address
     * and thereforeusable in REGISTER requests only.
     *
     * @param targetAddress the address of the registrar that this contact
     * header is meant for.
     * @param srcListeningPoint the listening point that will be used when
     * accessing the registrar.
     *
     * @return a Contact header based upon a local inet address.
     * @throws OperationFailedException if we fail constructing the contact
     * header.
     */
    ContactHeader getContactHeader(InetAddress targetAddress,
                                   ListeningPoint srcListeningPoint)
        throws OperationFailedException
    {
        ContactHeader registrationContactHeader = null;
        try
        {
            //find the address to use with the target
            InetAddress localAddress = SipActivator
                .getNetworkAddressManagerService().getLocalHost(targetAddress);

            SipURI contactURI = addressFactory.createSipURI(
                ((SipURI)ourSipAddress.getURI()).getUser()
                , localAddress.getHostAddress() );

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
     * Returns the default listening point that we use for communication over
     * <tt>transport</tt>.
     *
     * @param transport the transport that the returned listening point needs
     * to support.
     *
     * @return the default listening point that we use for communication over
     * <tt>transport</tt> or null if no such transport is supported.
     */
    public ListeningPoint getListeningPoint(String transport)
    {
        if(transport.equalsIgnoreCase(ListeningPoint.UDP))
        {
            return udpListeningPoint;
        }
        else if(transport.equalsIgnoreCase(ListeningPoint.TCP))
        {
            return tcpListeningPoint;
        }
        else if(transport.equalsIgnoreCase(ListeningPoint.TLS))
        {
            return tlsListeningPoint;
        }
        return null;
    }

    /**
     * Returns the default jain sip provider that we use for communication over
     * <tt>transport</tt>.
     *
     * @param transport the transport that the returned provider needs
     * to support.
     *
     * @return the default jain sip provider that we use for communication over
     * <tt>transport</tt> or null if no such transport is supported.
     */
    public SipProvider getJainSipProvider(String transport)
    {
        if(transport.equalsIgnoreCase(ListeningPoint.UDP))
        {
            return udpJainSipProvider;
        }
        else if(transport.equalsIgnoreCase(ListeningPoint.TCP))
        {
            return tcpJainSipProvider;
        }
        else if(transport.equalsIgnoreCase(ListeningPoint.TLS))
        {
            return tlsJainSipProvider;
        }
        return null;
    }

    /**
     * Reurns the currently valid sip security manager that everyone should
     * use to authenticate SIP Requests.
     * @return the currently valid instace of a SipSecurityManager that everyone
     * sould use to authenticate SIP Requests.
     */
    public SipSecurityManager getSipSecurityManager()
    {
        return sipSecurityManager;
    }

    /**
     * Initializes the SipRegistrarConnection that this class will be using.
     *
     * @param accountID the ID of the account that this registrar is associated
     * with.
     * @throws java.lang.IllegalArgumentException if one or more account
     * properties have invalid values.
     */
    private void initRegistrarConnection(SipAccountID accountID)
        throws IllegalArgumentException
    {
        //First init the registrar address
        String registrarAddressStr = (String) accountID.getAccountProperties()
            .get(ProtocolProviderFactory.SERVER_ADDRESS);

        //if there is no registrar address, parse the user_id and extract it
        //from the domain part of the SIP URI.
        if (registrarAddressStr == null)
        {
            String userID = (String) accountID.getAccountProperties()
                .get(ProtocolProviderFactory.USER_ID);
            registrarAddressStr = userID.substring( userID.indexOf("@")+1);
        }

        InetAddress registrarAddress = null;

        try
        {
            registrarAddress = InetAddress.getByName(registrarAddressStr);

            // We should set here the property to indicate that the server
            // address is validated. When we load stored accounts we check
            // this property in order to prevent checking again the server
            // address. And this is needed because in the case we don't have
            // network while loading the application we still want to have our
            // accounts loaded.
            accountID.putProperty(
                ProtocolProviderFactory.SERVER_ADDRESS_VALIDATED,
                Boolean.toString(true));

        }
        catch (UnknownHostException ex)
        {
            logger.error(registrarAddressStr
                + " appears to be an either invalid or inaccessible address: "
                , ex);

            String serverValidatedString
                = (String) accountID.getAccountProperties()
                    .get(ProtocolProviderFactory.SERVER_ADDRESS_VALIDATED);

            boolean isServerValidated = false;
            if (serverValidatedString != null)
                isServerValidated = new Boolean(serverValidatedString)
                    .booleanValue();

            // We should check here if the server address was already validated.
            // When we load stored accounts we want to prevent checking again the
            // server address. This is needed because in the case we don't have
            // network while loading the application we still want to have our
            // accounts loaded.
            if (serverValidatedString == null || !isServerValidated)
            {
                throw new IllegalArgumentException(
                    registrarAddressStr
                    + " appears to be an either invalid or inaccessible address: "
                    + ex.getMessage());
            }
        }

        // If the registrar address is null we don't need to continue.
        // If we still have problems with initializing the registrar we are
        // telling the user. We'll enter here only if the server has been
        // already validated (this means that the account is already created
        // and we're trying to login, but we have no internet connection).
        if(registrarAddress == null)
        {
            fireRegistrationStateChanged(
                RegistrationState.UNREGISTERED,
                RegistrationState.CONNECTION_FAILED,
                RegistrationStateChangeEvent.REASON_SERVER_NOT_FOUND,
                "Invalid or inaccessible server address.");

            return;
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
            {
                throw new IllegalArgumentException(registrarPort
                    + " is larger than " + NetworkUtils.MAX_PORT_NUMBER
                    + " and does not therefore represent a valid port nubmer.");
            }
        }

        //registrar transport
        String registrarTransport = (String) accountID.getAccountProperties()
            .get(ProtocolProviderFactory.PREFERRED_TRANSPORT);

        if(registrarTransport != null && registrarTransport.length() > 0)
        {
            if( ! registrarTransport.equals(ListeningPoint.UDP)
                && !registrarTransport.equals(ListeningPoint.TCP)
                && !registrarTransport.equals(ListeningPoint.TLS))
            {
                throw new IllegalArgumentException(registrarTransport
                    + " is not a valid transport protocol. Transport must be "
                    +"left blanc or set to TCP, UDP or TLS.");
            }
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

        //Initialize our connection with the registrar
        try
        {
            this.sipRegistrarConnection = new SipRegistrarConnection(
                registrarAddress
                , registrarPort
                , registrarTransport
                , expires
                , this);

            //determine whether we should be using route headers or not
            String useRouteString = (String) accountID.getAccountProperties()
                .get(REGISTERS_USE_ROUTE);

            boolean useRoute = false;

            if (useRouteString != null)
                useRoute = new Boolean(useRouteString).booleanValue();

            this.sipRegistrarConnection.setRouteHeaderEnabled(useRoute);
        }
        catch (ParseException ex)
        {
            //this really shouldn't happen as we're using InetAddress-es
            logger.error("Failed to create a registrar connection with "
                +registrarAddress.getHostAddress()
                , ex);
            throw new IllegalArgumentException(
                "Failed to create a registrar connection with "
                + registrarAddress.getHostAddress() + ": "
                + ex.getMessage());
        }

        //initialize our OPTIONS handler
        ClientCapabilities capabilities = new ClientCapabilities(this);
    }

    /**
     * Returns the SIP Address (Display Name <user@server.net>) that this
     * account is created for.
     * @return Address
     */
    public Address getOurSipAddress()
    {
        return ourSipAddress;
    }

    /**
     * In case we are using an outbound proxy this method returns its address.
     * The method returns <tt>null</tt> otherwise.
     *
     * @return the address of our outbound proxy if we are using one and
     * <tt>null</tt> otherwise.
     */
    public InetSocketAddress getOutboundProxy()
    {
        return this.outboundProxySocketAddress;
    }

    /**
     * In case we are using an outbound proxy this method returns the transport
     * we are using to connect to it. The method returns <tt>null</tt>
     * otherwise.
     *
     * @return the transport used to connect to our outbound proxy if we are
     * using one and <tt>null</tt> otherwise.
     */
    public String getOutboundProxyTransport()
    {
        return this.outboundProxyTransport;
    }

    /**
     * Extracts all properties concerning the usage of an outbound proxy for
     * this account.
     * @param accountID the account whose outbound proxy we are currently
     * initializing.
     * @param jainSipProperties the properties that we will be passing to the
     * jain sip stack when initialize it (that's where we'll put all proxy
     * properties).
     */
    private void initOutboundProxy(SipAccountID accountID,
                                   Hashtable jainSipProperties)
    {
        //First init the proxy address
        String proxyAddressStr = (String) accountID.getAccountProperties()
            .get(ProtocolProviderFactory.PROXY_ADDRESS);

        InetAddress proxyAddress = null;

        try
        {
            // first check for srv records exists
            try
            {
                String lookupStr = null;

                String proxyTransport = (String) accountID.getAccountProperties()
                            .get(ProtocolProviderFactory.PREFERRED_TRANSPORT);

                if(proxyTransport == null)
                    proxyTransport = getDefaultTransport();

                if(proxyTransport.equalsIgnoreCase(ListeningPoint.UDP))
                    lookupStr = "_sip._udp." + proxyAddressStr;
                else if(proxyTransport.equalsIgnoreCase(ListeningPoint.TCP))
                    lookupStr = "_sip._tcp." + proxyAddressStr;
                else if(proxyTransport.equalsIgnoreCase(ListeningPoint.TLS))
                    lookupStr = "_sips._tcp." + proxyAddressStr;

                InetSocketAddress hosts[] = NetworkUtils.getSRVRecords(lookupStr);

                if(hosts != null && hosts.length > 0)
                {
                    logger.trace("Will set server address from SRV records "
                       + hosts[0]);

                    proxyAddressStr = hosts[0].getHostName();
                }
            }
            catch (Exception ex)
            {
                // no SRV record or error looking for it
            }

            proxyAddress = InetAddress.getByName(proxyAddressStr);

            // We should set here the property to indicate that the proxy
            // address is validated. When we load stored accounts we check
            // this property in order to prevent checking again the proxy
            // address. this is needed because in the case we don't have
            // network while loading the application we still want to have
            // our accounts loaded.
            accountID.putProperty(
                ProtocolProviderFactory.PROXY_ADDRESS_VALIDATED,
                Boolean.toString(true));
        }
        catch (UnknownHostException ex)
        {
            logger.error(proxyAddressStr
                + " appears to be an either invalid or inaccessible address"
                , ex);

            String proxyValidatedString = (String) accountID
            .getAccountProperties().get(
                ProtocolProviderFactory.PROXY_ADDRESS_VALIDATED);

            boolean isProxyValidated = false;
            if (proxyValidatedString != null)
                isProxyValidated
                    = new Boolean(proxyValidatedString).booleanValue();

            // We should check here if the proxy address was already validated.
            // When we load stored accounts we want to prevent checking again the
            // proxy address. This is needed because in the case we don't have
            // network while loading the application we still want to have our
            // accounts loaded.
            if (proxyValidatedString == null || !isProxyValidated)
            {
                throw new IllegalArgumentException(
                    proxyAddressStr
                    + " appears to be an either invalid or inaccessible address "
                    + ex.getMessage());
            }
        }

        // Return if no proxy is specified or if the proxyAddress is null.
        if(proxyAddressStr == null
                || proxyAddressStr.length() == 0
                || proxyAddress == null)
        {
            return;
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
            {
                throw new IllegalArgumentException(proxyPort
                    + " is larger than " +
                    NetworkUtils.MAX_PORT_NUMBER
                    +
                    " and does not therefore represent a valid port nubmer.");
            }
        }

        //proxy transport
        String proxyTransport = (String) accountID.getAccountProperties()
            .get(ProtocolProviderFactory.PREFERRED_TRANSPORT);

        if (proxyTransport != null && proxyTransport.length() > 0)
        {
            if (!proxyTransport.equals(ListeningPoint.UDP)
                && !proxyTransport.equals(ListeningPoint.TCP)
                && !proxyTransport.equals(ListeningPoint.TLS))
            {
                throw new IllegalArgumentException(proxyTransport
                    + " is not a valid transport protocol. Transport must be "
                    + "left blanc or set to TCP, UDP or TLS.");
            }
        }
        else
        {
            proxyTransport = ListeningPoint.UDP;
        }

        StringBuffer proxyStringBuffer
            = new StringBuffer(proxyAddress.getHostAddress());

        if(proxyAddress instanceof Inet6Address)
        {
            proxyStringBuffer.insert(0, '[');
            proxyStringBuffer.append(']');
        }

        proxyStringBuffer.append(':');
        proxyStringBuffer.append(Integer.toString(proxyPort));
        proxyStringBuffer.append('/');
        proxyStringBuffer.append(proxyTransport);

        //done parsing. init properties.
        jainSipProperties.put(  JSPNAME_OUTBOUND_PROXY,
                                proxyStringBuffer.toString());

        //store a reference to our sip proxy so that we can use it when
        //constructing via and contact headers.
        this.outboundProxySocketAddress
            = new InetSocketAddress(proxyAddress, proxyPort);
        this.outboundProxyTransport = proxyTransport;
    }

    /**
     * Registers <tt>methodProcessor</tt> in the <tt>methorProcessors</tt>
     * table so that it would receives all messages in a transaction initiated
     * by a <tt>method</tt> request. If any previous processors exist for the
     * same method, they will be replaced by this one.
     *
     * @param method a String representing the SIP method that we're registering
     * the processor for (e.g. INVITE, REGISTER, or SUBSCRIBE).
     * @param methodProcessor a <tt>SipListener</tt> implementation that would
     * handle all messages received within a <tt>method</tt> transaction.
     */
    public void registerMethodProcessor(String      method,
                                        SipListener methodProcessor)
    {
        this.methodProcessors.put(method, methodProcessor);
    }

    /**
     * Unregisters <tt>methodProcessor</tt> from the <tt>methorProcessors</tt>
     * table so that it won't receive further messages in a transaction
     * initiated by a <tt>method</tt> request.
     *
     * @param method the name of the method whose processor we'd like to
     * unregister.
     */
    public void unregisterMethodProcessor(String      method)
    {
            this.methodProcessors.remove(method);
    }

    /**
     * Returns the transport that we should use if we have no clear idea of our
     * destination's preferred transport. The method would first check if
     * we are running behind an outbound proxy and if so return its transport.
     * If no outbound proxy is set, the method would check the contents of the
     * DEFAULT_TRANSPORT property and return it if not null. Otherwise the
     * method would return UDP;
     *
     * @return The first non null password of the following: a) the transport
     * of our outbound proxy, b) the transport specified by the
     * DEFAULT_TRANSPORT property, c) UDP.
     */
    public String getDefaultTransport()
    {
        if(outboundProxySocketAddress != null
            && outboundProxyTransport != null)
        {
            return outboundProxyTransport;
        }
        else
        {
            String userSpecifiedDefaultTransport
                = SipActivator.getConfigurationService()
                    .getString(DEFAULT_TRANSPORT);

            if(userSpecifiedDefaultTransport != null)
            {
                return userSpecifiedDefaultTransport;
            }
            else
                return ListeningPoint.UDP;
        }
    }

    /**
     * Returns the provider that corresponds to the transport returned by
     * getDefaultTransport(). Equivalent to calling
     * getJainSipProvider(getDefaultTransport())
     *
     * @return the Jain SipProvider that corresponds to the transport returned
     * by getDefaultTransport().
     */
    public SipProvider getDefaultJainSipProvider()
    {
        return getJainSipProvider(getDefaultTransport());
    }

    /**
     * Returns the listening point that corresponds to the transport returned by
     * getDefaultTransport(). Equivalent to calling
     * getListeningPoint(getDefaultTransport())
     *
     * @return the Jain SipProvider that corresponds to the transport returned
     * by getDefaultTransport().
     */
    public ListeningPoint getDefaultListeningPoint()
    {
        return getListeningPoint(getDefaultTransport());
    }

    /**
     * Returns the display name string that the user has set as a display name
     * for this account.
     *
     * @return the display name string that the user has set as a display name
     * for this account.
     */
    public String getOurDisplayName()
    {
        return ourDisplayName;
    }

    /**
     * Returns a User Agent header that could be used for signing our requests.
     *
     * @return a <tt>UserAgentHeader</tt> that could be used for signing our
     * requests.
     */
    public UserAgentHeader getSipCommUserAgentHeader()
    {
        if(userAgentHeader == null)
        {
            try
            {
                List userAgentTokens = new LinkedList();

                Version ver =
                        SipActivator.getVersionService().getCurrentVersion();

                userAgentTokens.add(ver.getApplicationName());
                userAgentTokens.add(ver.toString());

                String osName = System.getProperty("os.name");
                userAgentTokens.add(osName);

                userAgentHeader
                    = this.headerFactory.createUserAgentHeader(userAgentTokens);
            }
            catch (ParseException ex)
            {
                //shouldn't happen
                return null;
            }
        }
        return userAgentHeader;
    }

    /**
     * Generates a ToTag and attaches it to the to header of <tt>response</tt>.
     *
     * @param response the response that is to get the ToTag.
     * @param containingDialog the Dialog instance that is to extract a unique
     * Tag value (containingDialog.hashCode())
     */
    public void attachToTag(Response response, Dialog containingDialog)
    {
        ToHeader to = (ToHeader) response.getHeader(ToHeader.NAME);
        if (to == null) {
            logger.debug("Strange ... no to tag in response:" + response);
            return;
        }

        if(containingDialog.getLocalTag() != null)
        {
            logger.debug("We seem to already have a tag in this dialog. "
                         +"Returning");
            return;
        }

        try
        {
            if (to.getTag() == null || to.getTag().trim().length() == 0)
            {

                String toTag = generateLocalTag();

                logger.debug("generated to tag: " + toTag);
                to.setTag(toTag);
            }
        }
        catch (ParseException ex)
        {
            //a parse exception here mean an internal error so we can only log
            logger.error("Failed to attach a to tag to an outgoing response."
                         , ex);
        }
    }

    /**
     * Returns a List of Strings corresponding to all methods that we have a
     * processor for.
     * @return a List of methods that we support.
     */
    public List getSupportedMethods()
    {
        return new ArrayList(methodProcessors.keySet());
    }

    private class ShutdownUnregistrationBlockListener
        implements RegistrationStateChangeListener
    {
            public List collectedNewStates = new LinkedList();

            /**
             * The method would simply register all received events so that they
             * could be available for later inspection by the unit tests. In the
             * case where a registraiton event notifying us of a completed
             * registration is seen, the method would call notifyAll().
             *
             * @param evt ProviderStatusChangeEvent the event describing the status
             * change.
             */
            public void registrationStateChanged(RegistrationStateChangeEvent evt)
            {
                logger.debug("Received a RegistrationStateChangeEvent: " + evt);

                collectedNewStates.add(evt.getNewState());

                if (evt.getNewState().equals(RegistrationState.UNREGISTERED))
                {
                    logger.debug(
                        "We're unregistered and will notify those who wait");
                    synchronized (this)
                    {
                        notifyAll();
                    }
                }
            }

            /**
             * Blocks until an event notifying us of the awaited state change is
             * received or until waitFor miliseconds pass (whichever happens first).
             *
             * @param waitFor the number of miliseconds that we should be waiting
             * for an event before simply bailing out.
             */
            public void waitForEvent(long waitFor)
            {
                logger.trace("Waiting for a "
                             +"RegistrationStateChangeEvent.UNREGISTERED");

                synchronized (this)
                {
                    if (collectedNewStates.contains(
                            RegistrationState.UNREGISTERED))
                    {
                        logger.trace("Event already received. "
                                     + collectedNewStates);
                        return;
                    }

                    try
                    {
                        wait(waitFor);

                        if (collectedNewStates.size() > 0)
                            logger.trace(
                                "Received a RegistrationStateChangeEvent.");
                        else
                            logger.trace(
                                "No RegistrationStateChangeEvent received for "
                                + waitFor + "ms.");

                    }
                    catch (InterruptedException ex)
                    {
                        logger.debug(
                            "Interrupted while waiting for a "
                            +"RegistrationStateChangeEvent"
                            , ex);
                    }
                }
            }
    }

    /**
     * Returns the sip protocol icon.
     * @return the sip protocol icon
     */
    public ProtocolIcon getProtocolIcon()
    {
        return protocolIcon;
    }

    /**
     * Returns the current instance of <tt>SipStatusEnum</tt>.
     *
     * @return the current instance of <tt>SipStatusEnum</tt>.
     */
    SipStatusEnum getSipStatusEnum()
    {
        return sipStatusEnum;
    }
    /**
     * Returns the current instance of <tt>SipRegistrarConnection</tt>.
     * @return SipRegistrarConnection
     */
    SipRegistrarConnection getRegistrarConnection()
    {
        return sipRegistrarConnection;
    }

    /**
     * Parses the the <tt>uriStr</tt> string and returns a JAIN SIP URI.
     *
     * @param uriStr a <tt>String</tt> containing the uri to parse.
     *
     * @return a URI object corresponding to the <tt>uriStr</tt> string.
     * @throws ParseException if uriStr is not properly formatted.
     */
    public Address parseAddressStr(String uriStr)
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
                + ((SipURI)getOurSipAddress().getURI()).getHost();
        }

        //Let's be uri fault tolerant and add the sip: scheme if there is none.
        if (uriStr.toLowerCase().indexOf("sip:") == -1 //no sip scheme
            && uriStr.indexOf('@') != -1) //most probably a sip uri
        {
            uriStr = "sip:" + uriStr;
        }

        //Request URI
        Address uri = getAddressFactory().createAddress(uriStr);

        return uri;
    }
}
