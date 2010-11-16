/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber;

import java.net.*;
import java.security.*;
import java.security.cert.*;
import java.text.*;
import java.util.*;
import javax.net.ssl.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.service.protocol.jabberconstants.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.impl.protocol.jabber.extensions.jingle.*;
import net.java.sip.communicator.impl.protocol.jabber.extensions.inputevt.*;
import net.java.sip.communicator.impl.protocol.jabber.sasl.*;
import net.java.sip.communicator.service.certificate.*;

import org.jivesoftware.smack.*;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.provider.*;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.*;
import org.jivesoftware.smackx.packet.*;

import org.osgi.framework.*;

/**
 * An implementation of the protocol provider service over the Jabber protocol
 *
 * @author Damian Minkov
 * @author Symphorien Wanko
 * @author Lubomir Marinov
 * @author Yana Stamcheva
 * @author Emil Ivov
 */
public class ProtocolProviderServiceJabberImpl
    extends AbstractProtocolProviderService
{
    /**
     * Logger of this class
     */
    private static final Logger logger =
        Logger.getLogger(ProtocolProviderServiceJabberImpl.class);

    /**
     * Jingle's Discovery Info common URN.
     */
    public static final String URN_XMPP_JINGLE = JingleIQ.NAMESPACE;

    /**
     * Jingle's Discovery Info URN for RTP support.
     */
    public static final String URN_XMPP_JINGLE_RTP
        = RtpDescriptionPacketExtension.NAMESPACE;

    /**
     * Jingle's Discovery Info URN for RTP support with audio.
     */
    public static final String URN_XMPP_JINGLE_RTP_AUDIO
        = "urn:xmpp:jingle:apps:rtp:audio";

    /**
     * Jingle's Discovery Info URN for RTP support with video.
     */
    public static final String URN_XMPP_JINGLE_RTP_VIDEO
        = "urn:xmpp:jingle:apps:rtp:video";

    /**
     * Jingle's Discovery Info URN for ZRTP support with RTP.
     */
    public static final String URN_XMPP_JINGLE_RTP_ZRTP
        = ZrtpHashPacketExtension.NAMESPACE;

    /**
     * Jingle's Discovery Info URN for ICE_UDP transport support.
     */
    public static final String URN_XMPP_JINGLE_RAW_UDP_0
        = RawUdpTransportPacketExtension.NAMESPACE;

    /**
     * Jingle's Discovery Info URN for ICE_UDP transport support.
     */
    public static final String URN_XMPP_JINGLE_ICE_UDP_1
        = IceUdpTransportPacketExtension.NAMESPACE;

    /**
     * Jingle's Discover Info URN for "XEP-0251: Jingle Session Transfer"
     * support.
     */
    public static final String URN_XMPP_JINGLE_TRANSFER_0
        = TransferPacketExtension.NAMESPACE;

    /**
     * Used to connect to a XMPP server.
     */
    private XMPPConnection connection = null;

    /**
     * Indicates whether or not the provider is initialized and ready for use.
     */
    private boolean isInitialized = false;

    /**
     * We use this to lock access to initialization.
     */
    private final Object initializationLock = new Object();

    /**
     * The identifier of the account that this provider represents.
     */
    private AccountID accountID = null;

    /**
     * Used when we need to re-register
     */
    private SecurityAuthority authority = null;

    /**
     * The icon corresponding to the jabber protocol.
     */
    private ProtocolIconJabberImpl jabberIcon;

    /**
     * A set of features supported by our Jabber implementation.
     * In general, we add new feature(s) when we add new operation sets.
     * (see xep-0030 : http://www.xmpp.org/extensions/xep-0030.html#info).
     * Example : to tell the world that we support jingle, we simply have
     * to do :
     * supportedFeatures.add("http://www.xmpp.org/extensions/xep-0166.html#ns");
     * Beware there is no canonical mapping between op set and jabber features
     * (op set is a SC "concept"). This means that one op set in SC can
     * correspond to many jabber features. It is also possible that there is no
     * jabber feature corresponding to a SC op set or again,
     * we can currently support some features wich do not have a specific
     * op set in SC (the mandatory feature :
     * http://jabber.org/protocol/disco#info is one example).
     * We can find features corresponding to op set in the xep(s) related
     * to implemented functionality.
     */
    private final List<String> supportedFeatures = new ArrayList<String>();

    /**
     * The <tt>ServiceDiscoveryManager</tt> is responsible for advertising
     * <tt>supportedFeatures</tt> when asked by a remote client. It can also
     * be used to query remote clients for supported features.
     */
    private ScServiceDiscoveryManager discoveryManager = null;

    /**
     * The <tt>OperationSetContactCapabilities</tt> of this
     * <tt>ProtocolProviderService</tt> which is the service-public counterpart
     * of {@link #discoveryManager}.
     */
    private OperationSetContactCapabilitiesJabberImpl opsetContactCapabilities;

    /**
     * The statuses.
     */
    private JabberStatusEnum jabberStatusEnum;

    /**
     * The service we use to interact with user.
     */
    private CertificateVerificationService guiVerification;

    /**
     * Used with tls connecting when certificates are not trusted
     * and we ask the user to confirm connection. When some timeout expires
     * connect method returns, and we use abortConnecting to abort further
     * execution cause after user chooses we make further processing from there.
     */
    private boolean abortConnecting = false;

    /**
     * Shows whether we have already checked the certificate for current server.
     * In case we use TLS.
     */
    private boolean certChecked = false;

    /**
     * Flag indicating are we currently executing connectAndLogin method.
     */
    private boolean inConnectAndLogin = false;

    /**
     * Object used to synchronize the flag inConnectAndLogin.
     */
    private final Object connectAndLoginLock = new Object();

    /**
     * If an event occurs during login we fire it at the end of the login
     * process (at the end of connectAndLogin method).
     */
    private RegistrationStateChangeEvent eventDuringLogin;

    /**
     * Listens for connection closes or errors.
     */
    private JabberConnectionListener connectionListener;

    /**
     * The details of the proxy we are using to connect to the server (if any)
     */
    private org.jivesoftware.smack.proxy.ProxyInfo proxy;

    /**
     * State for connect and login state.
     */
    enum ConnectState
    {
        /**
         * Abort any further connecting.
         */
        ABORT_CONNECTING,
        /**
         * Continue trying with next address.
         */
        CONTINUE_TRYING,
        /**
         * Stop trying we succeeded or just have a final state for
         * the whole connecting procedure.
         */
        STOP_TRYING
    }

    /**
     * Returns the state of the registration of this protocol provider
     * @return the <tt>RegistrationState</tt> that this provider is
     * currently in or null in case it is in a unknown state.
     */
    public RegistrationState getRegistrationState()
    {
        if(connection == null)
            return RegistrationState.UNREGISTERED;
        else if(connection.isConnected() && connection.isAuthenticated())
            return RegistrationState.REGISTERED;
        else
            return RegistrationState.UNREGISTERED;
    }

    /**
     * Return the certificate verification service impl.
     * @return the CertificateVerification service.
     */
    private CertificateVerificationService getCertificateVerificationService()
    {
        if(guiVerification == null)
        {
            ServiceReference guiVerifyReference
                = JabberActivator.getBundleContext().getServiceReference(
                    CertificateVerificationService.class.getName());
            if(guiVerifyReference != null)
                guiVerification = (CertificateVerificationService)
                    JabberActivator.getBundleContext().getService(
                        guiVerifyReference);
        }

        return guiVerification;
    }

    /**
     * Starts the registration process. Connection details such as
     * registration server, user name/number are provided through the
     * configuration service through implementation specific properties.
     *
     * @param authority the security authority that will be used for resolving
     *        any security challenges that may be returned during the
     *        registration or at any moment while we're registered.
     * @throws OperationFailedException with the corresponding code it the
     * registration fails for some reason (e.g. a networking error or an
     * implementation problem).
     */
    public void register(final SecurityAuthority authority)
        throws OperationFailedException
    {
        if(authority == null)
            throw new IllegalArgumentException(
                "The register method needs a valid non-null authority impl "
                + " in order to be able and retrieve passwords.");

        this.authority = authority;

        try
        {
            // reset states
            abortConnecting = false;

            connectAndLogin(authority,
                            SecurityAuthority.AUTHENTICATION_REQUIRED);
        }
        catch (XMPPException ex)
        {
            logger.error("Error registering", ex);

            fireRegistrationStateChanged(ex);
        }
    }

    /**
     * Connects and logins again to the server.
     *
     * @param authReasonCode indicates the reason of the re-authentication.
     */
    void reregister(int authReasonCode)
    {
        try
        {
            if (logger.isTraceEnabled())
                logger.trace("Trying to reregister us!");

            // sets this if any is tring to use us through registration
            // to know we are not registered
            this.unregister(false);

            // reset states
            this.abortConnecting = false;

            connectAndLogin(authority,
                            authReasonCode);
        }
        catch(OperationFailedException ex)
        {
            logger.error("Error ReRegistering", ex);

            fireRegistrationStateChanged(getRegistrationState(),
                RegistrationState.CONNECTION_FAILED,
                RegistrationStateChangeEvent.REASON_INTERNAL_ERROR, null);

            disconnectAndCleanConnection();
        }
        catch (XMPPException ex)
        {
            logger.error("Error ReRegistering", ex);

            fireRegistrationStateChanged(ex);
        }
    }

    /**
     * Connects and logins to the server
     * @param authority SecurityAuthority
     * @param reasonCode the authentication reason code. Indicates the reason of
     * this authentication.
     * @throws XMPPException if we cannot connect to the server - network problem
     * @throws  OperationFailedException if login parameters
     *          as server port are not correct
     */
    private synchronized void connectAndLogin(SecurityAuthority authority,
                                              int reasonCode)
        throws XMPPException, OperationFailedException
    {
        synchronized(connectAndLoginLock)
        {
            inConnectAndLogin = true;
        }

        synchronized(initializationLock)
        {
            //verify whether a password has already been stored for this account
            String password = JabberActivator.
                    getProtocolProviderFactory().loadPassword(getAccountID());

            //decode
            if (password == null)
            {
                //create a default credentials object
                UserCredentials credentials = new UserCredentials();
                credentials.setUserName(getAccountID().getUserID());

                //request a password from the user
                credentials = authority.obtainCredentials(
                    ProtocolNames.JABBER,
                    credentials,
                    reasonCode);

                // in case user has canceled the login window
                if(credentials == null)
                {
                    fireRegistrationStateChanged(
                        getRegistrationState(),
                        RegistrationState.UNREGISTERED,
                        RegistrationStateChangeEvent.REASON_USER_REQUEST, "");
                    return;
                }

                //extract the password the user passed us.
                char[] pass = credentials.getPassword();

                // the user didn't provide us a password (canceled the operation)
                if(pass == null)
                {
                    fireRegistrationStateChanged(
                        getRegistrationState(),
                        RegistrationState.UNREGISTERED,
                        RegistrationStateChangeEvent.REASON_USER_REQUEST, "");
                    return;
                }
                password = new String(pass);

                if (credentials.isPasswordPersistent())
                {
                    JabberActivator.getProtocolProviderFactory()
                        .storePassword(getAccountID(), password);
                }
            }

            //init the necessary objects
            try
            {
                String userID = null;

                /* with a google account (either gmail or google apps
                 * related ones), the userID MUST be the full e-mail address
                 * not just the ID
                 */
                if(getAccountID().getProtocolDisplayName().
                        equals("Google Talk"))
                {
                    userID = getAccountID().getUserID();
                }
                else
                {
                    userID = StringUtils.parseName(getAccountID().getUserID());
                }

                String serviceName
                    = StringUtils.parseServer(getAccountID().getUserID());

                List<String> serverAddresses = new ArrayList<String>();

                String serverAddressUserSetting
                    = getAccountID().getAccountPropertyString(
                        ProtocolProviderFactory.SERVER_ADDRESS);

                int serverPort = getAccountID().getAccountPropertyInt(
                        ProtocolProviderFactory.SERVER_PORT, 5222);

                String accountResource
                    = getAccountID().getAccountPropertyString(
                        ProtocolProviderFactory.RESOURCE);

                if(accountResource == null || accountResource.equals(""))
                    accountResource = "sip-comm";

                // check to see is there SRV records for this server domain
                try
                {
                    InetSocketAddress[] srvAddresses = NetworkUtils
                        .getSRVRecords("xmpp-client", "tcp", serviceName);

                    if(srvAddresses != null)
                    {
                        for (int i = 0; i < srvAddresses.length; i++)
                        {
                            String addr =
                                srvAddresses[i].getAddress().getHostAddress();

                            serverAddresses.add(addr);
                        }
                    }

                    // after SRV records, check A/AAAA records
                    InetSocketAddress addressObj4 = null;
                    InetSocketAddress addressObj6 = null;
                    try
                    {
                        addressObj4 = NetworkUtils.getARecord(
                            serverAddressUserSetting, serverPort);
                    } catch (ParseException ex)
                    {
                        logger.error("Cannot obtain A record for "
                            + serverAddressUserSetting, ex);
                    }
                    try
                    {
                        addressObj6 = NetworkUtils.getAAAARecord(
                            serverAddressUserSetting, serverPort);
                    } catch (ParseException ex)
                    {
                        logger.error("Cannot obtain AAAA record for "
                            + serverAddressUserSetting, ex);
                    }

                    // add address according their priorities setting
                    if(Boolean.getBoolean("java.net.preferIPv6Addresses"))
                    {
                        if(addressObj6 != null)
                        {
                            serverAddresses
                                .add(addressObj6.getAddress().getHostAddress());
                        }
                        if(addressObj4 != null)
                        {
                            serverAddresses
                                .add(addressObj4.getAddress().getHostAddress());
                        }
                    }
                    else
                    {
                        if(addressObj4 != null)
                        {
                            serverAddresses
                                .add(addressObj4.getAddress().getHostAddress());
                        }
                        if(addressObj6 != null)
                        {
                            serverAddresses
                                .add(addressObj6.getAddress().getHostAddress());
                        }
                    }

                    serverAddresses.add(serverAddressUserSetting);
                }
                catch (ParseException ex1)
                {
                    logger.error("Domain not resolved " + ex1.getMessage());
                }

                Roster.setDefaultSubscriptionMode(Roster.SubscriptionMode.manual);

                //Getting global proxy information from configuration files
                proxy = null;
                String globalProxyType =
                    JabberActivator.getConfigurationService()
                    .getString(ProxyInfo.CONNECTON_PROXY_TYPE_PROPERTY_NAME);
                if(globalProxyType == null ||
                   globalProxyType.equals(ProxyInfo.ProxyType.NONE.name()))
                {
                    proxy = org.jivesoftware.smack.proxy.ProxyInfo.forNoProxy();
                }
                else
                {
                    String globalProxyAddress =
                        JabberActivator.getConfigurationService().getString(
                        ProxyInfo.CONNECTON_PROXY_ADDRESS_PROPERTY_NAME);
                    String globalProxyPortStr =
                        JabberActivator.getConfigurationService().getString(
                        ProxyInfo.CONNECTON_PROXY_PORT_PROPERTY_NAME);
                    int globalProxyPort;
                    try
                    {
                        globalProxyPort = Integer.parseInt(
                            globalProxyPortStr);
                    }
                    catch(NumberFormatException ex)
                    {
                        throw new OperationFailedException("Wrong port",
                            OperationFailedException.INVALID_ACCOUNT_PROPERTIES,
                            ex);
                    }
                    String globalProxyUsername =
                        JabberActivator.getConfigurationService().getString(
                        ProxyInfo.CONNECTON_PROXY_USERNAME_PROPERTY_NAME);
                    String globalProxyPassword =
                        JabberActivator.getConfigurationService().getString(
                        ProxyInfo.CONNECTON_PROXY_PASSWORD_PROPERTY_NAME);
                    if(globalProxyAddress == null ||
                        globalProxyAddress.length() <= 0)
                    {
                        throw new OperationFailedException(
                            "Missing Proxy Address",
                            OperationFailedException.INVALID_ACCOUNT_PROPERTIES);
                    }
                    if(globalProxyType.equals(
                        ProxyInfo.ProxyType.HTTP.name()))
                    {
                        proxy = org.jivesoftware.smack.proxy.ProxyInfo
                            .forHttpProxy(
                                globalProxyAddress,
                                globalProxyPort,
                                globalProxyUsername,
                                globalProxyPassword);
                    }
                    else if(globalProxyType.equals(
                        ProxyInfo.ProxyType.SOCKS4.name()))
                    {
                         proxy = org.jivesoftware.smack.proxy.ProxyInfo
                             .forSocks4Proxy(
                                globalProxyAddress,
                                globalProxyPort,
                                globalProxyUsername,
                                globalProxyPassword);
                    }
                    else if(globalProxyType.equals(
                        ProxyInfo.ProxyType.SOCKS5.name()))
                    {
                         proxy = org.jivesoftware.smack.proxy.ProxyInfo
                             .forSocks5Proxy(
                                globalProxyAddress,
                                globalProxyPort,
                                globalProxyUsername,
                                globalProxyPassword);
                    }
                }

                // try connecting to all serverAddresses
                // as if connecting with username fails
                // try with username@serviceName
                for (int i = 0; i < serverAddresses.size(); i++)
                {
                    String currentAddress = serverAddresses.get(i);

                    try
                    {
                        ConnectState state = connectAndLogin(
                            currentAddress, serverPort, serviceName,
                            userID, password, accountResource);

                        if(state == ConnectState.ABORT_CONNECTING)
                            return;
                        else if(state == ConnectState.CONTINUE_TRYING)
                            continue;
                        else if(state == ConnectState.STOP_TRYING)
                            break;

                    }catch(XMPPException ex)
                    {
                        // server disconnect us after such an error
                        // cleanup
                        disconnectAndCleanConnection();

                        try
                        {
                            // after updating to new smack lib
                            // login mechanisum changed
                            // this is a way to avoid the problem

                            // logging in to google need and service name
                            ConnectState state = connectAndLogin(
                                currentAddress, serverPort, serviceName,
                                userID + "@" + serviceName,
                                password, accountResource);

                            if(state == ConnectState.ABORT_CONNECTING)
                                return;
                            else if(state == ConnectState.CONTINUE_TRYING)
                                continue;
                            else if(state == ConnectState.STOP_TRYING)
                                break;
                        } catch (XMPPException e)
                        {
                            if(isAuthenticationFailed(ex))
                                throw ex;

                            disconnectAndCleanConnection();

                            // if it happens once again throw
                            // the original exception
                            if(i == serverAddresses.size())
                            {
                                throw ex;
                            }
                        }
                    }
                }
            }
            catch (NumberFormatException ex)
            {
                throw new OperationFailedException("Wrong port",
                    OperationFailedException.INVALID_ACCOUNT_PROPERTIES, ex);
            }
        }

        synchronized(connectAndLoginLock)
        {
            // Checks if an error has occurred during login, if so we fire
            // it here in order to avoid a deadlock which occurs in
            // reconnect plugin. The deadlock is cause we fired an event during
            // login process and have locked initializationLock and we cannot
            // unregister from reconnect, cause unregister method
            // also needs this lock.
            if(eventDuringLogin != null)
            {
                fireRegistrationStateChanged(
                    eventDuringLogin.getOldState(),
                    eventDuringLogin.getNewState(),
                    eventDuringLogin.getReasonCode(),
                    eventDuringLogin.getReason());

                eventDuringLogin = null;
                inConnectAndLogin = false;
                return;
            }

            inConnectAndLogin = false;
        }
    }

    /**
     * Connects xmpp connection and login. Returning the state whether is it
     * final - Abort due to certificate cancel or keep trying cause only current
     * address has failed or stop trying cause we succeeded.
     * @param address the address to connect to
     * @param serverPort the port to use
     * @param serviceName the service name to use
     * @param userName the username to use
     * @param password the password to use
     * @param resource and the resource.
     * @return return the state how to continue the connect process.
     * @throws XMPPException if we cannot connect for some reason
     */
    private ConnectState connectAndLogin(
            String address, int serverPort, String serviceName,
            String userName, String password, String resource)
        throws XMPPException
    {
        ConnectionConfiguration confConn = new ConnectionConfiguration(
                address, serverPort,
                serviceName, proxy
        );

        confConn.setReconnectionAllowed(false);

        if(connection != null)
        {
            logger.error("Connection is not null and isConnected:"
                + connection.isConnected(),
                new Exception("Trace possible duplicate connections"));
        }

        connection = new XMPPConnection(confConn);

        try
        {
            CertificateVerificationService gvs =
                getCertificateVerificationService();
            if(gvs != null)
            {
                connection.setCustomTrustManager(
                    new HostTrustManager(gvs.getTrustManager(
                        address,
                        serverPort)));
            }
        }
        catch(GeneralSecurityException e)
        {
            logger.error("Error creating custom trust manager", e);
        }

        connection.connect();

        registerServiceDiscoveryManager();

        if(connectionListener == null)
        {
            connectionListener = new JabberConnectionListener();
        }

        connection.addConnectionListener(connectionListener);

        if(abortConnecting)
        {
            abortConnecting = false;
            disconnectAndCleanConnection();

            return ConnectState.ABORT_CONNECTING;
        }

        fireRegistrationStateChanged(
                getRegistrationState()
                , RegistrationState.REGISTERING
                , RegistrationStateChangeEvent.REASON_NOT_SPECIFIED
                , null);

        SASLAuthentication.supportSASLMechanism("PLAIN", 0);

        // Insert our sasl mechanism implementation
        // in order to support some incompatible servers
        SASLAuthentication.unregisterSASLMechanism("DIGEST-MD5");
        SASLAuthentication.registerSASLMechanism("DIGEST-MD5",
            SASLDigestMD5Mechanism.class);
        SASLAuthentication.supportSASLMechanism("DIGEST-MD5");

        connection.login(userName, password, resource);

        if(connection.isAuthenticated())
        {
            connection.getRoster().
                setSubscriptionMode(Roster.SubscriptionMode.manual);

            fireRegistrationStateChanged(
                getRegistrationState(),
                RegistrationState.REGISTERED,
                RegistrationStateChangeEvent.REASON_NOT_SPECIFIED, null);

            return ConnectState.STOP_TRYING;
        }
        else
        {
            fireRegistrationStateChanged(
                getRegistrationState()
                , RegistrationState.UNREGISTERED
                , RegistrationStateChangeEvent.REASON_NOT_SPECIFIED
                , null);

            disconnectAndCleanConnection();

            return ConnectState.CONTINUE_TRYING;
        }
    }

    /**
     * Registers our ServiceDiscoveryManager
     */
    private void registerServiceDiscoveryManager()
    {
        // we setup supported features no packets are actually sent
        //during feature registration so we'd better do it here so that
        //our first presence update would contain a caps with the right
        //features.
        String name
            = System.getProperty(
                    "sip-communicator.application.name",
                    "SIP Communicator ")
                + System.getProperty("sip-communicator.version","SVN");

        ServiceDiscoveryManager.setIdentityName(name);
        ServiceDiscoveryManager.setIdentityType("pc");

        discoveryManager
            = new ScServiceDiscoveryManager(
                    connection,
                    // Remove features supported by smack, but not supported in
                    // SIP Communicator.
                    new String[] { "http://jabber.org/protocol/commands" },
                    // Add features SIP Communicator supports in addition to smack.
                    supportedFeatures.toArray(new String[supportedFeatures.size()]));

        /*
         * Expose the discoveryManager as service-public through the
         * OperationSetContactCapabilities of this ProtocolProviderSerivce.
         */
        if (opsetContactCapabilities != null)
            opsetContactCapabilities.setDiscoveryManager(discoveryManager);
    }

    /**
     * Used to disconnect current connection and clean it.
     */
    private void disconnectAndCleanConnection()
    {
        if(connection != null)
        {
            connection.removeConnectionListener(connectionListener);

            // disconnect anyway cause it will clear any listeners
            // that maybe added even if its not connected
            try
            {
                connection.disconnect();
            } catch (Exception e)
            {}


            connectionListener = null;
            connection = null;
            // make it null as it also holds a reference to the old connection
            // will be created again on new connection
            try
            {
                /*
                 * The discoveryManager is exposed as service-public by the
                 * OperationSetContactCapabilities of this
                 * ProtocolProviderService. No longer expose it because it's
                 * going away.
                 */
                if (opsetContactCapabilities != null)
                    opsetContactCapabilities.setDiscoveryManager(null);
            }
            finally
            {
                discoveryManager = null;
            }
        }
    }

    /**
     * Ends the registration of this protocol provider with the service.
     */
    public void unregister()
    {
        unregister(true);
    }

    /**
     * Unregister and fire the event if requested
     * @param fireEvent boolean
     */
    void unregister(boolean fireEvent)
    {
        synchronized(initializationLock)
        {
            RegistrationState currRegState = getRegistrationState();

            if(fireEvent)
            {
                fireRegistrationStateChanged(
                    currRegState,
                    RegistrationState.UNREGISTERED,
                    RegistrationStateChangeEvent.REASON_USER_REQUEST, null);
            }

            disconnectAndCleanConnection();
        }
    }

    /**
     * Returns the short name of the protocol that the implementation of this
     * provider is based upon (like SIP, Jabber, ICQ/AIM, or others for
     * example).
     *
     * @return a String containing the short name of the protocol this
     *   service is taking care of.
     */
    public String getProtocolName()
    {
        return ProtocolNames.JABBER;
    }

    /**
     * Initialized the service implementation, and puts it in a sate where it
     * could interoperate with other services. It is strongly recommended that
     * properties in this Map be mapped to property names as specified by
     * <tt>AccountProperties</tt>.
     *
     * @param screenname the account id/uin/screenname of the account that
     * we're about to create
     * @param accountID the identifier of the account that this protocol
     * provider represents.
     *
     * @see net.java.sip.communicator.service.protocol.AccountID
     */
    protected void initialize(String screenname,
                              AccountID accountID)
    {
        synchronized(initializationLock)
        {
            this.accountID = accountID;

            String protocolIconPath
                = accountID.getAccountPropertyString(
                        ProtocolProviderFactory.PROTOCOL_ICON_PATH);

            if (protocolIconPath == null)
                protocolIconPath = "resources/images/protocol/jabber";

            jabberIcon = new ProtocolIconJabberImpl(protocolIconPath);

            jabberStatusEnum
                = JabberStatusEnum.getJabberStatusEnum(protocolIconPath);

            //this feature is mandatory to be compliant with Service Discovery
            supportedFeatures.add("http://jabber.org/protocol/disco#info");

            String keepAliveStrValue
                = accountID.getAccountPropertyString("SEND_KEEP_ALIVE");
            String resourcePriority
                = accountID.getAccountPropertyString(
                        ProtocolProviderFactory.RESOURCE_PRIORITY);

            //initialize the presence operationset
            OperationSetPersistentPresenceJabberImpl persistentPresence =
                new OperationSetPersistentPresenceJabberImpl(this);

            if(resourcePriority != null)
            {
                persistentPresence
                    .setResourcePriority(Integer.parseInt(resourcePriority));
                // TODO : is this resource priority related to xep-0168
                // (Resource Application Priority) ?
                // see http://www.xmpp.org/extensions/xep-0168.html
                // If the answer is no, comment the following lines please
                supportedFeatures.add(
                        "http://www.xmpp.org/extensions/xep-0168.html#ns");
            }

            addSupportedOperationSet(
                OperationSetPersistentPresence.class,
                persistentPresence);
            // TODO: add the feature, if any, corresponding to persistent
            // presence, if someone knows
            // supportedFeatures.add(_PRESENCE_);

            //register it once again for those that simply need presence
            addSupportedOperationSet(
                OperationSetPresence.class,
                persistentPresence);

            //initialize the IM operation set
            OperationSetBasicInstantMessagingJabberImpl basicInstantMessaging =
                new OperationSetBasicInstantMessagingJabberImpl(this);

            if (keepAliveStrValue != null)
                basicInstantMessaging.setKeepAliveEnabled(Boolean
                    .parseBoolean(keepAliveStrValue));

            addSupportedOperationSet(
                OperationSetBasicInstantMessaging.class,
                basicInstantMessaging);

            // The http://jabber.org/protocol/xhtml-im feature is included
            // already in smack.

            //initialize the Whiteboard operation set
            addSupportedOperationSet(
                OperationSetWhiteboarding.class,
                new OperationSetWhiteboardingJabberImpl(this));

            //initialize the typing notifications operation set
            addSupportedOperationSet(
                OperationSetTypingNotifications.class,
                new OperationSetTypingNotificationsJabberImpl(this));

            // The http://jabber.org/protocol/chatstates feature implemented in
            // OperationSetTypingNotifications is included already in smack.

            //initialize the multi user chat operation set
            addSupportedOperationSet(
                OperationSetMultiUserChat.class,
                new OperationSetMultiUserChatJabberImpl(this));

            InfoRetreiver infoRetreiver = new InfoRetreiver(this, screenname);

            addSupportedOperationSet(
                OperationSetServerStoredContactInfo.class,
                new OperationSetServerStoredContactInfoJabberImpl(
                        infoRetreiver));

            OperationSetServerStoredAccountInfo accountInfo =
                new OperationSetServerStoredAccountInfoJabberImpl(this,
                        infoRetreiver,
                        screenname);

            addSupportedOperationSet(
                OperationSetServerStoredAccountInfo.class,
                accountInfo);

            // Initialize avatar operation set
            addSupportedOperationSet(
                OperationSetAvatar.class,
                new OperationSetAvatarJabberImpl(this, accountInfo));

            // initialize the file transfer operation set
            addSupportedOperationSet(
                OperationSetFileTransfer.class,
                new OperationSetFileTransferJabberImpl(this));

            addSupportedOperationSet(
                OperationSetInstantMessageTransform.class,
                new OperationSetInstantMessageTransformImpl());

            // Include features we're supporting in addition to the four
            // included by smack itself:
            // http://jabber.org/protocol/si/profile/file-transfer
            // http://jabber.org/protocol/si
            // http://jabber.org/protocol/bytestreams
            // http://jabber.org/protocol/ibb
            supportedFeatures.add("urn:xmpp:thumbs:0");
            supportedFeatures.add("urn:xmpp:bob");

            // initialize the thumbnailed file factory operation set
            addSupportedOperationSet(
                OperationSetThumbnailedFileFactory.class,
                new OperationSetThumbnailedFileFactoryImpl());

            // TODO: this is the "main" feature to advertise when a client
            // support muc. We have to add some features for
            // specific functionality we support in muc.
            // see http://www.xmpp.org/extensions/xep-0045.html

            // The http://jabber.org/protocol/muc feature is already included in
            // smack.
            supportedFeatures.add("http://jabber.org/protocol/muc#rooms");
            supportedFeatures.add("http://jabber.org/protocol/muc#traffic");

            //register our jingle provider
            //register our home grown Jingle Provider.
            ProviderManager providerManager = ProviderManager.getInstance();
            providerManager.addIQProvider( JingleIQ.ELEMENT_NAME,
                                           JingleIQ.NAMESPACE,
                                           new JingleIQProvider());

            // register our input event provider
            providerManager.addIQProvider(InputEvtIQ.ELEMENT_NAME,
                                          InputEvtIQ.NAMESPACE,
                                          new InputEvtIQProvider());

            //initialize the telephony operation set
            OperationSetBasicTelephonyJabberImpl basicTelephony
                    = new OperationSetBasicTelephonyJabberImpl(this);

            addSupportedOperationSet(
                OperationSetAdvancedTelephony.class,
                basicTelephony);
            addSupportedOperationSet(
                OperationSetBasicTelephony.class,
                basicTelephony);
            addSupportedOperationSet(
                OperationSetSecureTelephony.class,
                basicTelephony);

            // initialize video telephony OperationSet
            addSupportedOperationSet(
                OperationSetVideoTelephony.class,
                new OperationSetVideoTelephonyJabberImpl(basicTelephony));

// TODO: Uncomment the following lines when the desktop sharing feature is ready
// to use.
            // initialize desktop streaming OperationSet
//            addSupportedOperationSet(
//                OperationSetDesktopStreaming.class,
//                new OperationSetDesktopStreamingJabberImpl(basicTelephony));

            // initialize desktop sharing OperationSets
//            addSupportedOperationSet(
//                OperationSetDesktopSharingServer.class,
//                new OperationSetDesktopSharingServerJabberImpl(
//                        basicTelephony));
//            addSupportedOperationSet(
//                    OperationSetDesktopSharingClient.class,
//                    new OperationSetDesktopSharingClientJabberImpl(this));

            addSupportedOperationSet(
                OperationSetTelephonyConferencing.class,
                new OperationSetTelephonyConferencingJabberImpl(this));

            // Add Jingle features to supported features.
            supportedFeatures.add(URN_XMPP_JINGLE);
            supportedFeatures.add(URN_XMPP_JINGLE_RTP);
            supportedFeatures.add(URN_XMPP_JINGLE_RAW_UDP_0);

            /*
             * Reflect the preference of the user with respect to the use of
             * ICE.
             */
            if (accountID.getAccountPropertyBoolean(
                    ProtocolProviderFactory.IS_USE_ICE,
                    false))
            {
                supportedFeatures.add(URN_XMPP_JINGLE_ICE_UDP_1);
            }

            supportedFeatures.add(URN_XMPP_JINGLE_RTP_AUDIO);
            supportedFeatures.add(URN_XMPP_JINGLE_RTP_VIDEO);
            supportedFeatures.add(URN_XMPP_JINGLE_RTP_ZRTP);

            /* add extension to support remote control */
            supportedFeatures.add(InputEvtIQ.NAMESPACE);

            // XEP-0251: Jingle Session Transfer
            supportedFeatures.add(URN_XMPP_JINGLE_TRANSFER_0);

            // OperationSetContactCapabilities
            opsetContactCapabilities
                = new OperationSetContactCapabilitiesJabberImpl(this);
            if (discoveryManager != null)
                opsetContactCapabilities.setDiscoveryManager(discoveryManager);
            addSupportedOperationSet(
                OperationSetContactCapabilities.class,
                opsetContactCapabilities);

            isInitialized = true;
        }
    }

    /**
     * Makes the service implementation close all open sockets and release
     * any resources that it might have taken and prepare for
     * shutdown/garbage collection.
     */
    public void shutdown()
    {
        synchronized(initializationLock)
        {
            if (logger.isTraceEnabled())
                logger.trace("Killing the Jabber Protocol Provider.");

            //kill all active calls
            OperationSetBasicTelephonyJabberImpl telephony
                = (OperationSetBasicTelephonyJabberImpl)getOperationSet(
                    OperationSetBasicTelephony.class);
            if (telephony != null)
            {
                telephony.shutdown();
            }

            disconnectAndCleanConnection();

            isInitialized = false;
        }
    }

    /**
     * Returns true if the provider service implementation is initialized and
     * ready for use by other services, and false otherwise.
     *
     * @return true if the provider is initialized and ready for use and false
     * otherwise
     */
    public boolean isInitialized()
    {
        return isInitialized;
    }

    /**
     * Returns the AccountID that uniquely identifies the account represented
     * by this instance of the ProtocolProviderService.
     * @return the id of the account represented by this provider.
     */
    public AccountID getAccountID()
    {
        return accountID;
    }

    /**
     * Returns the <tt>XMPPConnection</tt>opened by this provider
     * @return a reference to the <tt>XMPPConnection</tt> last opened by this
     * provider.
     */
    protected XMPPConnection getConnection()
    {
        return connection;
    }

    /**
     * Determines whether a specific <tt>XMPPException</tt> signals that
     * attempted authentication has failed.
     *
     * @param ex the <tt>XMPPException</tt> which is to be determined whether it
     * signals that attempted authentication has failed
     * @return <tt>true</tt> if the specified <tt>ex</tt> signals that attempted
     * authentication has failed; otherwise, <tt>false</tt>
     */
    private boolean isAuthenticationFailed(XMPPException ex)
    {
        String exMsg = ex.getMessage().toLowerCase();

        // as there are no types or reasons for XMPPException
        // we try determine the reason according to their message
        // all messages that were found in smack 3.1.0 were took in count
        return
            (exMsg.indexOf("authentication failed") != -1)
                || ((exMsg.indexOf("authentication") != -1)
                        && (exMsg.indexOf("failed") != -1))
                || (exMsg.indexOf("login failed") != -1)
                || (exMsg.indexOf("unable to determine password") != -1);
    }

    /**
     * Tries to determine the appropriate message and status to fire,
     * according the exception.
     *
     * @param ex the {@link XMPPException} that caused the state change.
     */
    private void fireRegistrationStateChanged(XMPPException ex)
    {
        int reason = RegistrationStateChangeEvent.REASON_NOT_SPECIFIED;
        RegistrationState regState = RegistrationState.UNREGISTERED;

        Throwable wrappedEx = ex.getWrappedThrowable();
        if(wrappedEx != null
            && (wrappedEx instanceof UnknownHostException
                || wrappedEx instanceof ConnectException
                || wrappedEx instanceof SocketException))
        {
            reason = RegistrationStateChangeEvent.REASON_SERVER_NOT_FOUND;
            regState = RegistrationState.CONNECTION_FAILED;
        }
        else
        {
            String exMsg = ex.getMessage().toLowerCase();

            // as there are no types or reasons for XMPPException
            // we try determine the reason according to their message
            // all messages that were found in smack 3.1.0 were took in count
            if(isAuthenticationFailed(ex))
            {
                JabberActivator.getProtocolProviderFactory().
                    storePassword(getAccountID(), null);

                reason = RegistrationStateChangeEvent
                    .REASON_AUTHENTICATION_FAILED;

                regState = RegistrationState.AUTHENTICATION_FAILED;

                fireRegistrationStateChanged(
                    getRegistrationState(), regState, reason, null);

                // Try to reregister and to ask user for a new password.
                reregister(SecurityAuthority.WRONG_PASSWORD);

                return;
            }
            else if(exMsg.indexOf("no response from the server") != -1
                || exMsg.indexOf("connection failed") != -1)
            {
                reason = RegistrationStateChangeEvent.REASON_NOT_SPECIFIED;
                regState = RegistrationState.CONNECTION_FAILED;
            }
        }

        fireRegistrationStateChanged(
            getRegistrationState(), regState, reason, null);

        if(regState == RegistrationState.UNREGISTERED
            || regState == RegistrationState.CONNECTION_FAILED)
        {
            // we fired that for some reason we are going offline
            // lets clean the connection state for any future connections
            disconnectAndCleanConnection();
        }
    }

    /**
     * Enable to listen for jabber connection events
     */
    private class JabberConnectionListener
        implements ConnectionListener
    {
        /**
         * Implements <tt>connectionClosed</tt> from <tt>ConnectionListener</tt>
         */
        public void connectionClosed()
        {
            OperationSetPersistentPresenceJabberImpl opSetPersPresence =
                (OperationSetPersistentPresenceJabberImpl)
                    getOperationSet(OperationSetPersistentPresence.class);

            opSetPersPresence.fireProviderStatusChangeEvent(
                opSetPersPresence.getPresenceStatus(),
                getJabberStatusEnum().getStatus(JabberStatusEnum.OFFLINE));
        }

        /**
         * Implements <tt>connectionClosedOnError</tt> from
         * <tt>ConnectionListener</tt>.
         *
         * @param exception contains information on the error.
         */
        public void connectionClosedOnError(Exception exception)
        {
            logger.error("connectionClosedOnError " +
                         exception.getLocalizedMessage());

            if(exception instanceof XMPPException)
            {
                StreamError err = ((XMPPException)exception).getStreamError();

                if(err != null && err.getCode().equals(
                    XMPPError.Condition.conflict.toString()))
                {
                    // if we are in the middle of connecting process
                    // do not fire events, will do it later when the method
                    // connectAndLogin finishes its work
                    synchronized(connectAndLoginLock)
                    {
                        if(inConnectAndLogin)
                        {
                            eventDuringLogin = new RegistrationStateChangeEvent(
                                ProtocolProviderServiceJabberImpl.this,
                                getRegistrationState(),
                                RegistrationState.UNREGISTERED,
                                RegistrationStateChangeEvent.REASON_MULTIPLE_LOGINS,
                                "Connecting multiple times with the same resource");
                             return;
                        }
                    }

                    fireRegistrationStateChanged(getRegistrationState(),
                        RegistrationState.UNREGISTERED,
                        RegistrationStateChangeEvent.REASON_MULTIPLE_LOGINS,
                        "Connecting multiple times with the same resource");

                    disconnectAndCleanConnection();

                    return;
                }
            } // Ignore certificate exceptions as we handle them elsewhere
            else if(exception instanceof SSLHandshakeException &&
                exception.getCause() instanceof CertificateException)
            {
                return;
            }

            // if we are in the middle of connecting process
            // do not fire events, will do it later when the method
            // connectAndLogin finishes its work
            synchronized(connectAndLoginLock)
            {
                if(inConnectAndLogin)
                {
                    eventDuringLogin = new RegistrationStateChangeEvent(
                        ProtocolProviderServiceJabberImpl.this,
                        getRegistrationState(),
                        RegistrationState.CONNECTION_FAILED,
                        RegistrationStateChangeEvent.REASON_NOT_SPECIFIED,
                        exception.getMessage());
                     return;
                }
            }

            fireRegistrationStateChanged(getRegistrationState(),
                RegistrationState.CONNECTION_FAILED,
                RegistrationStateChangeEvent.REASON_NOT_SPECIFIED,
                exception.getMessage());

            disconnectAndCleanConnection();
        }

        /**
         * Implements <tt>reconnectingIn</tt> from <tt>ConnectionListener</tt>
         *
         * @param i delay in seconds for reconnection.
         */
        public void reconnectingIn(int i)
        {
            if (logger.isInfoEnabled())
                logger.info("reconnectingIn " + i);
        }

        /**
         * Implements <tt>reconnectingIn</tt> from <tt>ConnectionListener</tt>
         */
        public void reconnectionSuccessful()
        {
            if (logger.isInfoEnabled())
                logger.info("reconnectionSuccessful");
        }

        /**
         * Implements <tt>reconnectionFailed</tt> from
         * <tt>ConnectionListener</tt>.
         *
         * @param exception description of the failure
         */
        public void reconnectionFailed(Exception exception)
        {
            if (logger.isInfoEnabled())
                logger.info("reconnectionFailed ", exception);
        }
    }

    /**
     * Returns the jabber protocol icon.
     * @return the jabber protocol icon
     */
    public ProtocolIcon getProtocolIcon()
    {
        return jabberIcon;
    }

    /**
     * Returns the current instance of <tt>JabberStatusEnum</tt>.
     *
     * @return the current instance of <tt>JabberStatusEnum</tt>.
     */
    JabberStatusEnum getJabberStatusEnum()
    {
        return jabberStatusEnum;
    }

    /**
     * Determines if the given list of <tt>features</tt> is supported by the
     * specified jabber id.
     *
     * @param jid the jabber id for which to check
     * @param features the list of features to check for
     *
     * @return <tt>true</tt> if the list of features is supported; otherwise,
     * <tt>false</tt>
     */
    public boolean isFeatureListSupported(String jid, String... features)
    {
        boolean isFeatureListSupported = true;

        try
        {
            DiscoverInfo featureInfo = discoveryManager.discoverInfo(jid);

            for (String feature : features)
            {
                if (!featureInfo.containsFeature(feature))
                {
                    // If one is not supported we return false and don't check
                    // the others.
                    isFeatureListSupported = false;
                    break;
                }
            }
        }
        catch (XMPPException e)
        {
            if (logger.isDebugEnabled())
                logger.debug("Failed to discover info.", e);
        }
        return isFeatureListSupported;
    }

    /**
     * Determines if the given list of <tt>features</tt> is supported by the
     * specified jabber id.
     *
     * @param jid the jabber id that we'd like to get information about
     * @param feature the feature to check for
     *
     * @return <tt>true</tt> if the list of features is supported, otherwise
     * returns <tt>false</tt>
     */
    public boolean isFeatureSupported(String jid, String feature)
    {
        return isFeatureListSupported(jid, feature);
    }

    /**
     * Returns the full jabber id (jid) corresponding to the given contact.
     *
     * @param contact the contact, for which we're looking for a jid
     * @return the jid of the specified contact;
     */
    public String getFullJid(Contact contact)
    {
        Roster roster = getConnection().getRoster();
        Presence presence = roster.getPresence(contact.getAddress());

        return presence.getFrom();
    }

    /**
     * The trust manager which asks the client whether to trust particular
     * certificate which is not globally trusted.
     */
    private class HostTrustManager
        implements X509TrustManager
    {
        /**
         * The default trust manager.
         */
        private final X509TrustManager tm;

        /**
         * Creates the custom trust manager.
         * @param tm the default trust manager.
         */
        HostTrustManager(X509TrustManager tm)
        {
            this.tm = tm;
        }

        /**
         * Not used.
         *
         * @return nothing.
         */
        public X509Certificate[] getAcceptedIssuers()
        {
            throw new UnsupportedOperationException();
        }

        /**
         * Not used.
         * @param chain the cert chain.
         * @param authType authentication type like: RSA.
         * @throws CertificateException never
         * @throws UnsupportedOperationException always
         */
        public void checkClientTrusted(X509Certificate[] chain, String authType)
            throws CertificateException, UnsupportedOperationException
        {
            throw new UnsupportedOperationException();
        }

        /**
         * Check whether a certificate is trusted, if not as user whether he
         * trust it.
         * @param chain the certificate chain.
         * @param authType authentication type like: RSA.
         * @throws CertificateException not trusted.
         */
        public void checkServerTrusted(X509Certificate[] chain, String authType)
            throws CertificateException
        {
            if(certChecked)
                return;

            abortConnecting = true;
            try
            {
                certChecked = true;
                tm.checkServerTrusted(chain, authType);
            }
            catch(CertificateException e)
            {
                fireRegistrationStateChanged(getRegistrationState(),
                            RegistrationState.UNREGISTERED,
                            RegistrationStateChangeEvent.REASON_USER_REQUEST,
                            "Not trusted certificate");
                throw e;
            }

            if(abortConnecting)
            {
                // connect hasn't finished we will continue normally
                abortConnecting = false;
                return;
            }
            else
            {
                // in this situation connect method has finished
                // and it was disconnected so we wont to connect.

                // register.connect in new thread so we can release the
                // current connecting thread, otherwise this blocks
                // jabber
                new Thread(new Runnable()
                {
                    public void run()
                    {
                        reregister(SecurityAuthority.CONNECTION_FAILED);
                    }
                }).start();
                return;
            }
        }
    }

    /**
     * Returns the currently valid {@link ScServiceDiscoveryManager}.
     *
     * @return the currently valid {@link ScServiceDiscoveryManager}.
     */
    public ScServiceDiscoveryManager getDiscoveryManager()
    {
        return discoveryManager;
    }

    /**
     * Returns our own Jabber ID.
     *
     * @return our own Jabber ID.
     */
    public String getOurJID()
    {
        String jid = null;

        if( connection != null && connection.getUser() != null)
            return connection.getUser();

        if (jid == null)
        {
            //seems like the connection is not yet initialized so lets try
            //to construct our jid ourselves.
            String userID =
                StringUtils.parseName(getAccountID().getUserID());
            String serviceName =
                StringUtils.parseServer(getAccountID().getUserID());

            jid = userID + "@" + serviceName;
        }

        return jid;
    }

    /**
     * Returns the <tt>InetAddress</tt> that is most likely to be to be used
     * as a next hop when contacting our XMPP server. This is an utility method
     * that is used whenever we have to choose one of our local addresses (e.g.
     * when trying to pick a best candidate for raw udp). It is based on the
     * assumption that, in absence of any more specific details, chances are
     * that we will be accessing remote destinations via the same interface
     * that we are using to access our jabber server.
     *
     * @return the <tt>InetAddress</tt> that is most likely to be to be used
     * as a next hop when contacting our server.
     *
     * @throws IllegalArgumentException if we don't have a valid server.
     */
    public InetAddress getNextHop()
        throws IllegalArgumentException
    {
        InetAddress nextHop = null;
        String nextHopStr = null;

        if ( proxy != null
            && proxy.getProxyType()
              != org.jivesoftware.smack.proxy.ProxyInfo.ProxyType.NONE)
        {
            nextHopStr = proxy.getProxyAddress();
        }
        else
        {
            nextHopStr = getConnection().getHost();
        }

        try
        {
            nextHop = NetworkUtils.getInetAddress(nextHopStr);
        }
        catch (UnknownHostException ex)
        {
            throw new IllegalArgumentException(
                "seems we don't have a valid next hop.", ex);
        }

        if(logger.isDebugEnabled())
            logger.debug("Returning address " + nextHop + " as next hop.");

        return nextHop;
    }

    /**
     * Logs a specific message and associated <tt>Throwable</tt> cause as an
     * error using the current <tt>Logger</tt> and then throws a new
     * <tt>OperationFailedException</tt> with the message, a specific error code
     * and the cause.
     *
     * @param message the message to be logged and then wrapped in a new
     * <tt>OperationFailedException</tt>
     * @param errorCode the error code to be assigned to the new
     * <tt>OperationFailedException</tt>
     * @param cause the <tt>Throwable</tt> that has caused the necessity to log
     * an error and have a new <tt>OperationFailedException</tt> thrown
     * @param logger the logger that we'd like to log the error <tt>message</tt>
     * and <tt>cause</tt>.
     *
     * @throws OperationFailedException the exception that we wanted this method
     * to throw.
     */
    public static void throwOperationFailedException( String    message,
                                                      int       errorCode,
                                                      Throwable cause,
                                                      Logger    logger)
        throws OperationFailedException
    {
        logger.error(message, cause);

        if(cause == null)
            throw new OperationFailedException(message, errorCode);
        else
            throw new OperationFailedException(message, errorCode, cause);
    }
}
