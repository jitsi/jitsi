/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.sip.communicator.impl.protocol.jabber;

import java.math.*;
import java.net.*;
import java.security.*;
import java.security.cert.*;
import java.text.*;
import java.util.*;

import javax.net.ssl.*;

import net.java.sip.communicator.impl.protocol.jabber.debugger.*;
import net.java.sip.communicator.impl.protocol.jabber.extensions.*;
import net.java.sip.communicator.impl.protocol.jabber.extensions.carbon.*;
import net.java.sip.communicator.impl.protocol.jabber.extensions.coin.*;
import net.java.sip.communicator.impl.protocol.jabber.extensions.colibri.*;
import net.java.sip.communicator.impl.protocol.jabber.extensions.inputevt.*;
import net.java.sip.communicator.impl.protocol.jabber.extensions.jingle.*;
import net.java.sip.communicator.impl.protocol.jabber.extensions.jingleinfo.*;
import net.java.sip.communicator.impl.protocol.jabber.extensions.keepalive.*;
import net.java.sip.communicator.impl.protocol.jabber.extensions.messagecorrection.*;
import net.java.sip.communicator.impl.protocol.jabber.extensions.version.*;
import net.java.sip.communicator.service.certificate.*;
import net.java.sip.communicator.service.dns.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.service.protocol.jabberconstants.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.Logger;

import org.jitsi.service.configuration.*;
import org.jitsi.service.neomedia.*;
import org.jitsi.util.*;
import org.jivesoftware.smack.*;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.provider.*;
import org.jivesoftware.smack.util.*;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.*;
import org.jivesoftware.smackx.packet.*;
import org.xmlpull.v1.*;
import org.xmpp.jnodes.smack.*;

/**
 * An implementation of the protocol provider service over the Jabber protocol
 *
 * @author Damian Minkov
 * @author Symphorien Wanko
 * @author Lyubomir Marinov
 * @author Yana Stamcheva
 * @author Emil Ivov
 * @author Hristo Terezov
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
     * Jingle's Discovery Info URN for Jingle Nodes support.
     */
    public static final String URN_XMPP_JINGLE_NODES
        = "http://jabber.org/protocol/jinglenodes";

    /**
     * Jingle's Discovery Info URN for "XEP-0251: Jingle Session Transfer"
     * support.
     */
    public static final String URN_XMPP_JINGLE_TRANSFER_0
        = TransferPacketExtension.NAMESPACE;

    /**
     * Jingle's Discovery Info URN for "XEP-298 :Delivering Conference
     * Information to Jingle Participants (Coin)" support.
     */
    public static final String URN_XMPP_JINGLE_COIN = "urn:xmpp:coin";

    /**
     * Jingle's Discovery Info URN for &quot;XEP-0320: Use of DTLS-SRTP in
     * Jingle Sessions&quot;.
     */
    public static final String URN_XMPP_JINGLE_DTLS_SRTP
        = "urn:xmpp:jingle:apps:dtls:0";

    /**
     * Discovery Info URN for classic RFC3264-style Offer/Answer negotiation
     * with no support for Trickle ICE and low tolerance to transport/payload
     * separation. Defined in XEP-0176
     */
    public static final String URN_IETF_RFC_3264 = "urn:ietf:rfc:3264";

    /**
     * Jingle's Discovery Info URN for "XEP-0294: Jingle RTP Header Extensions
     * Negotiation" support.
     */
    public static final String URN_XMPP_JINGLE_RTP_HDREXT =
        "urn:xmpp:jingle:apps:rtp:rtp-hdrext:0";

    /**
     * URN for XEP-0077 inband registration
     */
    public static final String URN_REGISTER = "jabber:iq:register";

    /**
     * The name of the property under which the user may specify if the desktop
     * streaming or sharing should be disabled.
     */
    private static final String IS_DESKTOP_STREAMING_DISABLED
        = "net.java.sip.communicator.impl.protocol.jabber." +
            "DESKTOP_STREAMING_DISABLED";

    /**
     * The name of the property under which the user may specify if audio/video
     * calls should be disabled.
     */
    private static final String IS_CALLING_DISABLED
        = "net.java.sip.communicator.impl.protocol.jabber.CALLING_DISABLED";

    /**
     * Smack packet reply timeout.
     */
    public static final int SMACK_PACKET_REPLY_TIMEOUT = 45000;

    /**
     * Property for vcard reply timeout. Time to wait before
     * we think vcard retrieving has timeouted, default value
     * of smack is 5000 (5 sec.).
     */
    public static final String VCARD_REPLY_TIMEOUT_PROPERTY =
        "net.java.sip.communicator.impl.protocol.jabber.VCARD_REPLY_TIMEOUT";

    /**
     * XMPP signaling DSCP configuration property name.
     */
    private static final String XMPP_DSCP_PROPERTY =
        "net.java.sip.communicator.impl.protocol.XMPP_DSCP";

    /**
     * Indicates if user search is disabled.
     */
    private static final String IS_USER_SEARCH_ENABLED_PROPERTY
        = "USER_SEARCH_ENABLED";

    /**
     * Google voice domain name.
     */
    public static final String GOOGLE_VOICE_DOMAIN = "voice.google.com";

    /**
     * Used to connect to a XMPP server.
     */
    private XMPPConnection connection;

    /**
     * The socket address of the XMPP server.
     */
    private InetSocketAddress address;

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
     * The resource we will use when connecting during this run.
     */
    private String resource = null;

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
    private CertificateService guiVerification;

    /**
     * Used with tls connecting when certificates are not trusted
     * and we ask the user to confirm connection. When some timeout expires
     * connect method returns, and we use abortConnecting to abort further
     * execution cause after user chooses we make further processing from there.
     */
    private boolean abortConnecting = false;

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
     * The debugger who logs packets.
     */
    private SmackPacketDebugger debugger = null;

    /**
     * Jingle Nodes service.
     */
    private SmackServiceNode jingleNodesServiceNode = null;

    /**
     * Synchronization object to monitore jingle nodes auto discovery.
     */
    private final Object jingleNodesSyncRoot = new Object();

    /**
     * Stores user credentials for local use if user hasn't stored
     * its password.
     */
    private UserCredentials userCredentials = null;

    /**
     * The currently running keepAliveManager if enabled.
     */
    private KeepAliveManager keepAliveManager = null;

    /**
     * The version manager.
     */
    private VersionManager versionManager = null;

    // load xmpp manager classes
    static
    {
        if(OSUtils.IS_ANDROID)
            loadJabberServiceClasses();
    }

    /**
     * An <tt>OperationSet</tt> that allows access to connection information used
     * by the protocol provider.
     */
    private class OperationSetConnectionInfoJabberImpl
       implements OperationSetConnectionInfo
    {
       /**
        * @return The XMPP server address.
        */
        @Override
        public InetSocketAddress getServerAddress()
        {
            return address;
        }
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
    private CertificateService getCertificateVerificationService()
    {
        if(guiVerification == null)
        {
            guiVerification
                = ServiceUtils.getService(
                        JabberActivator.getBundleContext(),
                        CertificateService.class);
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

            // indicate we started connectAndLogin process
            synchronized(connectAndLoginLock)
            {
                inConnectAndLogin = true;
            }

            initializeConnectAndLogin(authority,
                SecurityAuthority.AUTHENTICATION_REQUIRED);
        }
        catch (XMPPException ex)
        {
            logger.error("Error registering", ex);

            eventDuringLogin = null;

            fireRegistrationStateChanged(ex);
        }
        finally
        {
            synchronized(connectAndLoginLock)
            {
                // Checks if an error has occurred during login, if so we fire
                // it here in order to avoid a deadlock which occurs in
                // reconnect plugin. The deadlock is cause we fired an event
                // during login process and have locked initializationLock and
                // we cannot unregister from reconnect, cause unregister method
                // also needs this lock.
                if(eventDuringLogin != null)
                {
                    if(eventDuringLogin.getNewState().equals(
                            RegistrationState.CONNECTION_FAILED) ||
                        eventDuringLogin.getNewState().equals(
                            RegistrationState.UNREGISTERED))
                        disconnectAndCleanConnection();

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

            // sets this if any is trying to use us through registration
            // to know we are not registered
            this.unregisterInternal(false);

            // reset states
            this.abortConnecting = false;

            // indicate we started connectAndLogin process
            synchronized(connectAndLoginLock)
            {
                inConnectAndLogin = true;
            }

            initializeConnectAndLogin(authority, authReasonCode);
        }
        catch(OperationFailedException ex)
        {
            logger.error("Error ReRegistering", ex);

            eventDuringLogin = null;

            disconnectAndCleanConnection();

            fireRegistrationStateChanged(getRegistrationState(),
                RegistrationState.CONNECTION_FAILED,
                RegistrationStateChangeEvent.REASON_INTERNAL_ERROR, null);
        }
        catch (XMPPException ex)
        {
            logger.error("Error ReRegistering", ex);

            eventDuringLogin = null;

            fireRegistrationStateChanged(ex);
        }
        finally
        {
            synchronized(connectAndLoginLock)
            {
                // Checks if an error has occurred during login, if so we fire
                // it here in order to avoid a deadlock which occurs in
                // reconnect plugin. The deadlock is cause we fired an event
                // during login process and have locked initializationLock and
                // we cannot unregister from reconnect, cause unregister method
                // also needs this lock.
                if(eventDuringLogin != null)
                {
                    if(eventDuringLogin.getNewState().equals(
                            RegistrationState.CONNECTION_FAILED) ||
                        eventDuringLogin.getNewState().equals(
                            RegistrationState.UNREGISTERED))
                        disconnectAndCleanConnection();

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
    }

    /**
     * Indicates if the XMPP transport channel is using a TLS secured socket.
     *
     * @return True when TLS is used, false otherwise.
     */
    public boolean isSignalingTransportSecure()
    {
        return connection != null && connection.isUsingTLS();
    }

    /**
     * Returns the "transport" protocol of this instance used to carry the
     * control channel for the current protocol service.
     *
     * @return The "transport" protocol of this instance: TCP, TLS or UNKNOWN.
     */
    public TransportProtocol getTransportProtocol()
    {
        // Without a connection, there is no transport available.
        if(connection != null && connection.isConnected())
        {
            // Transport using a secure connection.
            if(connection.isUsingTLS())
            {
                return TransportProtocol.TLS;
            }
            // Transport using a unsecure connection.
            return TransportProtocol.TCP;
        }
        return TransportProtocol.UNKNOWN;
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
    private void initializeConnectAndLogin(SecurityAuthority authority,
                                              int reasonCode)
        throws XMPPException, OperationFailedException
    {
        synchronized(initializationLock)
        {
            // if a thread is waiting for initializationLock and enters
            // lets check whether someone hasn't already tried login and
            // have succeeded,
            // should prevent "Trace possible duplicate connections" prints
            if(isRegistered())
                return;

            JabberLoginStrategy loginStrategy = createLoginStrategy();
            userCredentials = loginStrategy.prepareLogin(authority, reasonCode);
            if(!loginStrategy.loginPreparationSuccessful())
                return;

            String serviceName
                = StringUtils.parseServer(getAccountID().getUserID());

            loadResource();
            loadProxy();
            Roster.setDefaultSubscriptionMode(Roster.SubscriptionMode.manual);

            ConnectState state;
            //[0] = hadDnsSecException
            boolean[] hadDnsSecException = new boolean[]{false};

            // try connecting with auto-detection if enabled
            boolean isServerOverriden =
                getAccountID().getAccountPropertyBoolean(
                    ProtocolProviderFactory.IS_SERVER_OVERRIDDEN, false);

            if(!isServerOverriden)
            {
                state = connectUsingSRVRecords(serviceName,
                        serviceName, hadDnsSecException, loginStrategy);
                if(hadDnsSecException[0])
                {
                    setDnssecLoginFailure();
                    return;
                }
                if(state == ConnectState.ABORT_CONNECTING
                    || state == ConnectState.STOP_TRYING)
                    return;
            }

            // check for custom xmpp domain which we will check for
            // SRV records for server addresses
            String customXMPPDomain = getAccountID()
                .getAccountPropertyString("CUSTOM_XMPP_DOMAIN");

            if(customXMPPDomain != null && !hadDnsSecException[0])
            {
                logger.info("Connect using custom xmpp domain: " +
                    customXMPPDomain);

                state = connectUsingSRVRecords(
                            customXMPPDomain, serviceName,
                            hadDnsSecException, loginStrategy);

                logger.info("state for connectUsingSRVRecords: " + state);

                if(hadDnsSecException[0])
                {
                    setDnssecLoginFailure();
                    return;
                }

                if(state == ConnectState.ABORT_CONNECTING
                    || state == ConnectState.STOP_TRYING)
                    return;
            }

            // connect with specified server name
            String serverAddressUserSetting
                = getAccountID().getAccountPropertyString(
                    ProtocolProviderFactory.SERVER_ADDRESS);

            int serverPort = getAccountID().getAccountPropertyInt(
                    ProtocolProviderFactory.SERVER_PORT, 5222);

            InetSocketAddress[] addrs = null;
            try
            {
                addrs = NetworkUtils.getAandAAAARecords(
                    serverAddressUserSetting,
                    serverPort
                );
            }
            catch (ParseException e)
            {
                logger.error("Domain not resolved", e);
            }
            catch (DnssecException e)
            {
                logger.error("DNSSEC failure for overridden server", e);
                setDnssecLoginFailure();
                return;
            }

            if (addrs == null || addrs.length == 0)
            {
                logger.error("No server addresses found");

                eventDuringLogin = null;

                fireRegistrationStateChanged(
                    getRegistrationState(),
                    RegistrationState.CONNECTION_FAILED,
                    RegistrationStateChangeEvent.REASON_SERVER_NOT_FOUND,
                    "No server addresses found");
            }
            else
            {
                for (InetSocketAddress isa : addrs)
                {
                    try
                    {
                        state = connectAndLogin(isa, serviceName,
                            loginStrategy);
                        if(state == ConnectState.ABORT_CONNECTING
                            || state == ConnectState.STOP_TRYING)
                            return;
                    }
                    catch(XMPPException ex)
                    {
                        disconnectAndCleanConnection();
                        if(isAuthenticationFailed(ex))
                            throw ex;
                    }
                }
            }
        }
    }

    /**
     * Creates the JabberLoginStrategy to use for the current account.
     */
    private JabberLoginStrategy createLoginStrategy()
    {
        if (((JabberAccountIDImpl)getAccountID()).isAnonymousAuthUsed())
        {
            return new AnonymousLoginStrategy(
                getAccountID().getAuthorizationName());
        }

        String clientCertId = getAccountID().getAccountPropertyString(
                ProtocolProviderFactory.CLIENT_TLS_CERTIFICATE);
        if(clientCertId != null)
        {
            return new LoginByClientCertificateStrategy(getAccountID());
        }
        else
        {
            return new LoginByPasswordStrategy(this, getAccountID());
        }
    }

    private void setDnssecLoginFailure()
    {
        eventDuringLogin = new RegistrationStateChangeEvent(
            this,
            getRegistrationState(),
            RegistrationState.UNREGISTERED,
            RegistrationStateChangeEvent.REASON_USER_REQUEST,
            "No usable host found due to DNSSEC failures");
    }

    /**
     * Connects using the domain specified and its SRV records.
     * @param domain the domain to use
     * @param serviceName the domain name of the user's login
     * @param dnssecState state of possible received DNSSEC exceptions
     * @param loginStrategy the login strategy to use
     * @return whether to continue trying or stop.
     */
    private ConnectState connectUsingSRVRecords(
        String domain,
        String serviceName,
        boolean[] dnssecState,
        JabberLoginStrategy loginStrategy)
        throws XMPPException
    {
        // check to see is there SRV records for this server domain
        SRVRecord srvRecords[] = null;
        try
        {
            srvRecords = NetworkUtils
                .getSRVRecords("xmpp-client", "tcp", domain);
        }
        catch (ParseException e)
        {
            logger.error("SRV record not resolved", e);
        }
        catch (DnssecException e)
        {
            logger.error("DNSSEC failure for SRV lookup", e);
            dnssecState[0] = true;
        }

        if(srvRecords != null)
        {
            for(SRVRecord srv : srvRecords)
            {
                InetSocketAddress[] addrs = null;
                try
                {
                    addrs =
                        NetworkUtils.getAandAAAARecords(
                            srv.getTarget(),
                            srv.getPort()
                        );
                }
                catch (ParseException e)
                {
                    logger.error("Invalid SRV record target", e);
                }
                catch (DnssecException e)
                {
                    logger.error("DNSSEC failure for A/AAAA lookup of SRV", e);
                    dnssecState[0] = true;
                }

                if (addrs == null || addrs.length == 0)
                {
                    logger.error("No A/AAAA addresses found for " +
                        srv.getTarget());
                    continue;
                }

                for (InetSocketAddress isa : addrs)
                {
                    try
                    {
                        // if failover mechanism is enabled, use it,
                        // default is not enabled.
                        if(JabberActivator.getConfigurationService()
                            .getBoolean(FailoverConnectionMonitor
                                                .REVERSE_FAILOVER_ENABLED_PROP,
                                false
                            ))
                        {
                            FailoverConnectionMonitor.getInstance(this)
                                .setCurrent(domain, srv.getTarget());
                        }

                        ConnectState state = connectAndLogin(
                            isa, serviceName, loginStrategy);
                        return state;
                    }
                    catch(XMPPException ex)
                    {
                        logger.error("Error connecting to " + isa
                            + " for domain:" + domain
                            + " serviceName:" + serviceName, ex);

                        disconnectAndCleanConnection();

                        if(isAuthenticationFailed(ex))
                            throw ex;
                    }
                }
            }
        }
        else
            logger.error("No SRV addresses found for _xmpp-client._tcp."
                + domain);

        return ConnectState.CONTINUE_TRYING;
    }

    /**
     * Tries to login to the XMPP server with the supplied user ID. If the
     * protocol is Google Talk, the user ID including the service name is used.
     * For other protocols, if the login with the user ID without the service
     * name fails, a second attempt including the service name is made.
     *
     * @param currentAddress the IP address to connect to
     * @param serviceName the domain name of the user's login
     * @param loginStrategy the login strategy to use
     * @throws XMPPException when a failure occurs
     */
    private ConnectState connectAndLogin(InetSocketAddress currentAddress,
        String serviceName,
        JabberLoginStrategy loginStrategy)
        throws XMPPException
    {
        String userID = null;
        boolean qualifiedUserID;

        /* with a google account (either gmail or google apps
         * related ones), the userID MUST be the full e-mail address
         * not just the ID
         */
        if(getAccountID().getProtocolDisplayName().equals("Google Talk"))
        {
            userID = getAccountID().getUserID();
            qualifiedUserID = true;
        }
        else
        {
            userID = StringUtils.parseName(getAccountID().getUserID());
            qualifiedUserID = false;
        }

        try
        {
            return connectAndLogin(
                currentAddress, serviceName,
                userID, resource, loginStrategy);
        }
        catch(XMPPException ex)
        {
            // server disconnect us after such an error, do cleanup
            disconnectAndCleanConnection();

            //no need to check with a different username if the
            //socket could not be opened
            if (ex.getWrappedThrowable() instanceof ConnectException
                || ex.getWrappedThrowable() instanceof NoRouteToHostException)
            {
                //as we got an exception not handled in connectAndLogin
                //no state was set, so fire it here so we can continue
                //with the re-register process
                //2013-08-07 do not fire event, if we have several
                // addresses and we fire event will activate reconnect
                // but we will continue connecting with other addresses
                // and can register with address, then unregister and try again
                // that is from reconnect plugin.
                // Storing event for fire after all have failed and we have
                // tried every address.
                eventDuringLogin = new RegistrationStateChangeEvent(
                    ProtocolProviderServiceJabberImpl.this,
                    getRegistrationState(),
                    RegistrationState.CONNECTION_FAILED,
                    RegistrationStateChangeEvent.REASON_SERVER_NOT_FOUND,
                    null);

                throw ex;
            }

            // don't attempt to append the service name if it's already there
            if (!qualifiedUserID)
            {
                try
                {
                    // logging in might need the service name
                    return connectAndLogin(
                        currentAddress, serviceName,
                        userID + "@" + serviceName,
                        resource,
                        loginStrategy);
                }
                catch(XMPPException ex2)
                {
                    disconnectAndCleanConnection();
                    throw ex; //throw the original exception
                }
            }
            else
                throw ex;
        }
    }

    /**
     * Initializes the Jabber Resource identifier.
     */
    private void loadResource()
    {
        if(resource == null)
        {
            String defaultResource = "jitsi";
            String autoGenenerateResource =
                getAccountID().getAccountPropertyString(
                    ProtocolProviderFactory.AUTO_GENERATE_RESOURCE);
            if(autoGenenerateResource == null ||
                Boolean.parseBoolean(autoGenenerateResource))
            {
                SecureRandom random = new SecureRandom();

                resource = defaultResource + "-" +
                    new BigInteger(32, random).toString(32);
            }
            else
            {
                resource = getAccountID().getAccountPropertyString(
                    ProtocolProviderFactory.RESOURCE);

                if(resource == null || resource.length() == 0)
                    resource = defaultResource;
            }
        }
    }

    /**
     * Sets the global proxy information based on the configuration
     *
     * @throws OperationFailedException
     */
    private void loadProxy() throws OperationFailedException
    {
        String globalProxyType =
            JabberActivator.getConfigurationService()
            .getString(ProxyInfo.CONNECTION_PROXY_TYPE_PROPERTY_NAME);
        if(globalProxyType == null ||
           globalProxyType.equals(ProxyInfo.ProxyType.NONE.name()))
        {
            proxy = org.jivesoftware.smack.proxy.ProxyInfo.forNoProxy();
        }
        else
        {
            String globalProxyAddress =
                JabberActivator.getConfigurationService().getString(
                ProxyInfo.CONNECTION_PROXY_ADDRESS_PROPERTY_NAME);
            String globalProxyPortStr =
                JabberActivator.getConfigurationService().getString(
                ProxyInfo.CONNECTION_PROXY_PORT_PROPERTY_NAME);
            int globalProxyPort;
            try
            {
                globalProxyPort = Integer.parseInt(
                    globalProxyPortStr);
            }
            catch(NumberFormatException ex)
            {
                throw new OperationFailedException("Wrong proxy port, "
                        + globalProxyPortStr
                        + " does not represent an integer",
                    OperationFailedException.INVALID_ACCOUNT_PROPERTIES,
                    ex);
            }
            String globalProxyUsername =
                JabberActivator.getConfigurationService().getString(
                ProxyInfo.CONNECTION_PROXY_USERNAME_PROPERTY_NAME);
            String globalProxyPassword =
                JabberActivator.getConfigurationService().getString(
                ProxyInfo.CONNECTION_PROXY_PASSWORD_PROPERTY_NAME);
            if(globalProxyAddress == null ||
                globalProxyAddress.length() <= 0)
            {
                throw new OperationFailedException(
                    "Missing Proxy Address",
                    OperationFailedException.INVALID_ACCOUNT_PROPERTIES);
            }
            try
            {
                proxy = new org.jivesoftware.smack.proxy.ProxyInfo(
                    Enum.valueOf(org.jivesoftware.smack.proxy.ProxyInfo.
                        ProxyType.class, globalProxyType),
                    globalProxyAddress, globalProxyPort,
                    globalProxyUsername, globalProxyPassword);
            }
            catch(IllegalArgumentException e)
            {
                logger.error("Invalid value for smack proxy enum", e);
                proxy = null;
            }
        }
    }

    /**
     * Connects xmpp connection and login. Returning the state whether is it
     * final - Abort due to certificate cancel or keep trying cause only current
     * address has failed or stop trying cause we succeeded.
     * @param address the address to connect to
     * @param serviceName the service name to use
     * @param userName the username to use
     * @param resource and the resource.
     * @param loginStrategy the login strategy to use
     * @return return the state how to continue the connect process.
     * @throws XMPPException if we cannot connect for some reason
     */
    private ConnectState connectAndLogin(
            InetSocketAddress address, String serviceName,
            String userName, String resource,
            JabberLoginStrategy loginStrategy)
        throws XMPPException
    {
        ConnectionConfiguration confConn = new ConnectionConfiguration(
                address.getAddress().getHostAddress(),
                address.getPort(),
                serviceName, proxy
        );

        // if we have OperationSetPersistentPresence skip sending initial
        // presence while login is executed, the OperationSet will take care
        // of it
        if(getOperationSet(OperationSetPersistentPresence.class) != null)
            confConn.setSendPresence(false);

        confConn.setReconnectionAllowed(false);
        boolean tlsRequired = loginStrategy.isTlsRequired();

        // user have the possibility to disable TLS but in this case, it will
        // not be able to connect to a server which requires TLS
        confConn.setSecurityMode(
            tlsRequired ? ConnectionConfiguration.SecurityMode.required :
                ConnectionConfiguration.SecurityMode.enabled);

        TLSUtils.setTLSOnly(confConn);

        if(connection != null)
        {
            logger.error("Connection is not null and isConnected:"
                + connection.isConnected(),
                new Exception("Trace possible duplicate connections: " +
                    getAccountID().getAccountAddress()));
            disconnectAndCleanConnection();
        }

        connection = new XMPPConnection(confConn);
        this.address = address;

        try
        {
            CertificateService cvs =
                getCertificateVerificationService();
            if(cvs != null)
            {
                SSLContext sslContext = loginStrategy.createSslContext(cvs,
                        getTrustManager(cvs, serviceName));

                // log SSL/TLS algorithms and protocols
                if (logger.isDebugEnabled())
                {
                    final StringBuilder buff = new StringBuilder();
                    buff.append("Available TLS protocols and algorithms:\n");
                    buff.append("Default protocols: ");
                    buff.append(Arrays.toString(
                        sslContext.getDefaultSSLParameters().getProtocols()));
                    buff.append("\n");
                    buff.append("Supported protocols: ");
                    buff.append(Arrays.toString(
                        sslContext.getSupportedSSLParameters().getProtocols()));
                    buff.append("\n");
                    buff.append("Default cipher suites: ");
                    buff.append(Arrays.toString(
                            sslContext.getDefaultSSLParameters()
                            .getCipherSuites()));
                    buff.append("\n");
                    buff.append("Supported cipher suites: ");
                    buff.append(Arrays.toString(
                            sslContext.getSupportedSSLParameters()
                            .getCipherSuites()));
                    logger.debug(buff.toString());
                }

                confConn.setCustomSSLContext(sslContext);
            }
            else if (tlsRequired)
                throw new XMPPException(
                    "Certificate verification service is "
                    + "unavailable and TLS is required");
        }
        catch(GeneralSecurityException e)
        {
            logger.error("Error creating custom trust manager", e);
            throw new XMPPException("Error creating custom trust manager", e);
        }

        if(debugger == null)
            debugger = new SmackPacketDebugger();

        // sets the debugger
        debugger.setConnection(connection);
        connection.addPacketListener(debugger, null);
        connection.addPacketInterceptor(debugger, null);

        connection.connect();

        setTrafficClass();

        if(abortConnecting)
        {
            abortConnecting = false;
            disconnectAndCleanConnection();

            return ConnectState.ABORT_CONNECTING;
        }

        registerServiceDiscoveryManager();

        if(connectionListener == null)
        {
            connectionListener = new JabberConnectionListener();
        }

        if(!connection.isSecureConnection() && tlsRequired)
        {
            throw new XMPPException("TLS is required by client");
        }

        if(!connection.isConnected())
        {
            // connection is not connected, lets set state to our connection
            // as failed seems there is some lag/problem with network
            // and this way we will inform for it and later reconnect if needed
            // as IllegalStateException that is thrown within
            // addConnectionListener is not handled properly
            disconnectAndCleanConnection();

            logger.error("Connection not established, server not found!");

            eventDuringLogin = null;

            fireRegistrationStateChanged(getRegistrationState(),
                RegistrationState.CONNECTION_FAILED,
                RegistrationStateChangeEvent.REASON_SERVER_NOT_FOUND, null);

            return ConnectState.ABORT_CONNECTING;
        }
        else
        {
            if (connection.getSocket() instanceof SSLSocket)
            {
                final SSLSocket sslSocket = (SSLSocket) connection.getSocket();
                StringBuilder buff = new StringBuilder();
                buff.append("Chosen TLS protocol and algorithm:\n")
                        .append("Protocol: ").append(sslSocket.getSession()
                        .getProtocol()).append("\n")
                        .append("Cipher suite: ").append(sslSocket.getSession()
                        .getCipherSuite());
                logger.info(buff.toString());

                if (logger.isDebugEnabled())
                {
                    buff = new StringBuilder();
                    buff.append("Server TLS certificate chain:\n");
                    try
                    {
                        buff.append(Arrays.toString(
                                sslSocket.getSession().getPeerCertificates()));
                    }
                    catch (SSLPeerUnverifiedException ex)
                    {
                        buff.append("<unavailable: ")
                                .append(ex.getLocalizedMessage()).append(">");
                    }
                    logger.debug(buff.toString());
                }
            }

            connection.addConnectionListener(connectionListener);
        }

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

        if (!loginStrategy.login(connection, userName, resource))
        {
            disconnectAndCleanConnection();
            eventDuringLogin = null;
            fireRegistrationStateChanged(
                getRegistrationState(),
                // not auth failed, or there would be no info-popup
                RegistrationState.CONNECTION_FAILED,
                RegistrationStateChangeEvent.REASON_AUTHENTICATION_FAILED,
                loginStrategy.getClass().getName() + " requests abort");

            return ConnectState.ABORT_CONNECTING;
        }

        if(connection.isAuthenticated())
        {
            eventDuringLogin = null;

            fireRegistrationStateChanged(
                getRegistrationState(),
                RegistrationState.REGISTERED,
                RegistrationStateChangeEvent.REASON_NOT_SPECIFIED, null);

            return ConnectState.STOP_TRYING;
        }
        else
        {
            disconnectAndCleanConnection();

            eventDuringLogin = null;

            fireRegistrationStateChanged(
                getRegistrationState()
                , RegistrationState.UNREGISTERED
                , RegistrationStateChangeEvent.REASON_NOT_SPECIFIED
                , null);

            return ConnectState.CONTINUE_TRYING;
        }
    }

    /**
     * Gets the TrustManager that should be used for the specified service
     *
     * @param serviceName the service name
     * @param cvs The CertificateVerificationService to retrieve the
     *            trust manager
     * @return the trust manager
     */
    private X509TrustManager getTrustManager(CertificateService cvs,
        String serviceName)
        throws GeneralSecurityException
    {
        return new HostTrustManager(
            cvs.getTrustManager(
                Arrays.asList(new String[]{
                        serviceName,
                        "_xmpp-client." + serviceName
                })
            )
        );
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
                    this,
                    connection,
                    new String[] { "http://jabber.org/protocol/commands"},
                    // Add features Jitsi supports in addition to smack.
                    supportedFeatures.toArray(
                            new String[supportedFeatures.size()]),
                    true);

        /*
         * Expose the discoveryManager as service-public through the
         * OperationSetContactCapabilities of this ProtocolProviderService.
         */
        if (opsetContactCapabilities != null)
            opsetContactCapabilities.setDiscoveryManager(discoveryManager);
    }

    /**
     * Used to disconnect current connection and clean it.
     */
    public void disconnectAndCleanConnection()
    {
        if(connection != null)
        {
            connection.removeConnectionListener(connectionListener);

            // disconnect anyway cause it will clear any listeners
            // that maybe added even if its not connected
            try
            {
                OperationSetPersistentPresenceJabberImpl opSet =
                    (OperationSetPersistentPresenceJabberImpl)
                    this.getOperationSet(OperationSetPersistentPresence.class);

                Presence unavailablePresence =
                    new Presence(Presence.Type.unavailable);

                if(opSet != null
                    && !org.jitsi.util.StringUtils
                        .isNullOrEmpty(opSet.getCurrentStatusMessage()))
                {
                    unavailablePresence.setStatus(
                        opSet.getCurrentStatusMessage());
                }

                connection.disconnect(unavailablePresence);
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
                if(discoveryManager != null)
                {
                    discoveryManager.stop();
                    discoveryManager = null;
                }
            }
        }
    }

    /**
     * Ends the registration of this protocol provider with the service.
     */
    public void unregister()
    {
        unregisterInternal(true);
    }

    /**
     * Ends the registration of this protocol provider with the service.
     * @param userRequest is the unregister by user request.
     */
    public void unregister(boolean userRequest)
    {
        unregisterInternal(true, userRequest);
    }

    /**
     * Unregister and fire the event if requested
     * @param fireEvent boolean
     */
    public void unregisterInternal(boolean fireEvent)
    {
        unregisterInternal(fireEvent, false);
    }

    /**
     * Unregister and fire the event if requested
     * @param fireEvent boolean
     */
    public void unregisterInternal(boolean fireEvent, boolean userRequest)
    {
        synchronized(initializationLock)
        {
            if(fireEvent)
            {
                eventDuringLogin = null;
                fireRegistrationStateChanged(
                    getRegistrationState()
                    , RegistrationState.UNREGISTERING
                    , RegistrationStateChangeEvent.REASON_NOT_SPECIFIED
                    , null
                    , userRequest);
            }

            disconnectAndCleanConnection();

            RegistrationState currRegState = getRegistrationState();

            if(fireEvent)
            {
                eventDuringLogin = null;
                fireRegistrationStateChanged(
                    currRegState,
                    RegistrationState.UNREGISTERED,
                    RegistrationStateChangeEvent.REASON_USER_REQUEST, null,
                    userRequest);
            }
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

            // in case of modified account, we clear list of supported features
            // and every state change listeners, otherwise we can have two
            // OperationSet for same feature and it can causes problem (i.e.
            // two OperationSetBasicTelephony can launch two ICE negotiations
            // (with different ufrag/pwd) and peer will failed call. And
            // by the way user will see two dialog for answering/refusing the
            // call
            supportedFeatures.clear();
            this.clearRegistrationStateChangeListener();
            this.clearSupportedOperationSet();

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
                = accountID.getAccountPropertyString(
                    ProtocolProviderFactory.KEEP_ALIVE_METHOD);

            InfoRetreiver infoRetreiver = new InfoRetreiver(this, screenname);

            //initialize the presence OperationSet
            OperationSetPersistentPresenceJabberImpl persistentPresence =
                new OperationSetPersistentPresenceJabberImpl(this, infoRetreiver);

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

            if(accountID.getAccountPropertyString(
                    ProtocolProviderFactory.ACCOUNT_READ_ONLY_GROUPS) != null)
            {
                addSupportedOperationSet(
                    OperationSetPersistentPresencePermissions.class,
                    new OperationSetPersistentPresencePermissionsJabberImpl(
                            this));
            }

            //initialize the IM operation set
            OperationSetBasicInstantMessagingJabberImpl basicInstantMessaging =
                new OperationSetBasicInstantMessagingJabberImpl(this);

            if (keepAliveStrValue == null
                || keepAliveStrValue.equalsIgnoreCase("XEP-0199"))
            {
                if(keepAliveManager == null)
                    keepAliveManager = new KeepAliveManager(this);
            }

            addSupportedOperationSet(
                OperationSetBasicInstantMessaging.class,
                basicInstantMessaging);

            // The http://jabber.org/protocol/xhtml-im feature is included
            // already in smack.

            addSupportedOperationSet(
                OperationSetExtendedAuthorizations.class,
                new OperationSetExtendedAuthorizationsJabberImpl(
                    this,
                    persistentPresence));

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

            addSupportedOperationSet(
                OperationSetJitsiMeetTools.class,
                new OperationSetJitsiMeetToolsJabberImpl(this));

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

            // RTP HDR extension
            supportedFeatures.add(URN_XMPP_JINGLE_RTP_HDREXT);

            ProviderManager providerManager
                = ProtocolProviderFactoryJabberImpl.providerManager;

            //register our jingle provider
            providerManager.addIQProvider( JingleIQ.ELEMENT_NAME,
                                           JingleIQ.NAMESPACE,
                                           new JingleIQProvider());

            // register our input event provider
            providerManager.addIQProvider(InputEvtIQ.ELEMENT_NAME,
                                          InputEvtIQ.NAMESPACE,
                                          new InputEvtIQProvider());

            // register our coin provider
            providerManager.addIQProvider(CoinIQ.ELEMENT_NAME,
                                          CoinIQ.NAMESPACE,
                                          new CoinIQProvider());
            supportedFeatures.add(URN_XMPP_JINGLE_COIN);

            // register our JingleInfo provider
            providerManager.addIQProvider(JingleInfoQueryIQ.ELEMENT_NAME,
                                          JingleInfoQueryIQ.NAMESPACE,
                                          new JingleInfoQueryIQProvider());

            // Jitsi Videobridge IQProvider and PacketExtensionProvider
            providerManager.addIQProvider(
                    ColibriConferenceIQ.ELEMENT_NAME,
                    ColibriConferenceIQ.NAMESPACE,
                    new ColibriIQProvider());

            providerManager.addExtensionProvider(
                    ConferenceDescriptionPacketExtension.ELEMENT_NAME,
                    ConferenceDescriptionPacketExtension.NAMESPACE,
                    new ConferenceDescriptionPacketExtension.Provider());

            providerManager.addExtensionProvider(
                CarbonPacketExtension.RECEIVED_ELEMENT_NAME,
                CarbonPacketExtension.NAMESPACE,
                new CarbonPacketExtension.Provider(
                    CarbonPacketExtension.RECEIVED_ELEMENT_NAME));

            providerManager.addExtensionProvider(
                CarbonPacketExtension.SENT_ELEMENT_NAME,
                CarbonPacketExtension.NAMESPACE,
                new CarbonPacketExtension.Provider(
                    CarbonPacketExtension.SENT_ELEMENT_NAME));

            providerManager.addExtensionProvider(
                Nick.ELEMENT_NAME,
                Nick.NAMESPACE,
                new Nick.Provider());


            //initialize the telephony operation set
            boolean isCallingDisabled
                = JabberActivator.getConfigurationService()
                    .getBoolean(IS_CALLING_DISABLED, false);

            boolean isCallingDisabledForAccount
                = accountID.getAccountPropertyBoolean(
                    ProtocolProviderFactory.IS_CALLING_DISABLED_FOR_ACCOUNT,
                    false);

            // Check if calling is enabled.
            if (!isCallingDisabled && !isCallingDisabledForAccount)
            {
                OperationSetBasicTelephonyJabberImpl basicTelephony
                    = new OperationSetBasicTelephonyJabberImpl(this);

                addSupportedOperationSet(
                    OperationSetAdvancedTelephony.class,
                    basicTelephony);
                addSupportedOperationSet(
                    OperationSetBasicTelephony.class,
                    basicTelephony);
                addSupportedOperationSet(
                    OperationSetSecureZrtpTelephony.class,
                    basicTelephony);
                addSupportedOperationSet(
                    OperationSetSecureSDesTelephony.class,
                    basicTelephony);

                // initialize video telephony OperationSet
                addSupportedOperationSet(
                    OperationSetVideoTelephony.class,
                    new OperationSetVideoTelephonyJabberImpl(basicTelephony));

                addSupportedOperationSet(
                    OperationSetTelephonyConferencing.class,
                    new OperationSetTelephonyConferencingJabberImpl(this));

                addSupportedOperationSet(
                    OperationSetBasicAutoAnswer.class,
                    new OperationSetAutoAnswerJabberImpl(this));

                addSupportedOperationSet(
                    OperationSetResourceAwareTelephony.class,
                    new OperationSetResAwareTelephonyJabberImpl(basicTelephony));

                // Only init video bridge if enabled
                boolean isVideobridgeDisabled
                    = JabberActivator.getConfigurationService()
                      .getBoolean(OperationSetVideoBridge.
                          IS_VIDEO_BRIDGE_DISABLED, false);

                if (!isVideobridgeDisabled)
                {
                    // init video bridge
                    addSupportedOperationSet(
                        OperationSetVideoBridge.class,
                        new OperationSetVideoBridgeImpl(this));
                }

                // init DTMF
                OperationSetDTMFJabberImpl operationSetDTMF
                    = new OperationSetDTMFJabberImpl(this);
                addSupportedOperationSet(
                    OperationSetDTMF.class, operationSetDTMF);

                addSupportedOperationSet(
                    OperationSetIncomingDTMF.class,
                    new OperationSetIncomingDTMFJabberImpl());

                addJingleFeatures();

                // Check if desktop streaming is enabled.
                boolean isDesktopStreamingDisabled
                    = JabberActivator.getConfigurationService()
                        .getBoolean(IS_DESKTOP_STREAMING_DISABLED, false);

                boolean isAccountDesktopStreamingDisabled
                    = accountID.getAccountPropertyBoolean(
                        ProtocolProviderFactory.IS_DESKTOP_STREAMING_DISABLED,
                        false);

                if (!isDesktopStreamingDisabled
                    && !isAccountDesktopStreamingDisabled)
                {
                    // initialize desktop streaming OperationSet
                    addSupportedOperationSet(
                            OperationSetDesktopStreaming.class,
                            new OperationSetDesktopStreamingJabberImpl(
                                basicTelephony));

                    if(!accountID.getAccountPropertyBoolean(
                        ProtocolProviderFactory
                            .IS_DESKTOP_REMOTE_CONTROL_DISABLED,
                        false))
                    {
                        // initialize desktop sharing OperationSets
                        addSupportedOperationSet(
                            OperationSetDesktopSharingServer.class,
                            new OperationSetDesktopSharingServerJabberImpl(
                                basicTelephony));

                        // Adds extension to support remote control as a sharing
                        // server (sharer).
                        supportedFeatures.add(InputEvtIQ.NAMESPACE_SERVER);

                        addSupportedOperationSet(
                            OperationSetDesktopSharingClient.class,
                            new OperationSetDesktopSharingClientJabberImpl(this)
                            );
                        // Adds extension to support remote control as a sharing
                        // client (sharer).
                        supportedFeatures.add(InputEvtIQ.NAMESPACE_CLIENT);
                    }
                }
            }

            // OperationSetContactCapabilities
            opsetContactCapabilities
                = new OperationSetContactCapabilitiesJabberImpl(this);
            if (discoveryManager != null)
                opsetContactCapabilities.setDiscoveryManager(discoveryManager);
            addSupportedOperationSet(
                OperationSetContactCapabilities.class,
                opsetContactCapabilities);

            addSupportedOperationSet(
                OperationSetGenericNotifications.class,
                new OperationSetGenericNotificationsJabberImpl(this));

            supportedFeatures.add("jabber:iq:version");
            if(versionManager == null)
                versionManager = new VersionManager(this);

            supportedFeatures.add(MessageCorrectionExtension.NAMESPACE);
            addSupportedOperationSet(OperationSetMessageCorrection.class,
                    basicInstantMessaging);

            OperationSetChangePassword opsetChangePassword
                    = new OperationSetChangePasswordJabberImpl(this);
            addSupportedOperationSet(OperationSetChangePassword.class,
                    opsetChangePassword);

            OperationSetCusaxUtils opsetCusaxCusaxUtils
                    = new OperationSetCusaxUtilsJabberImpl(this);
            addSupportedOperationSet(OperationSetCusaxUtils.class,
                    opsetCusaxCusaxUtils);

            boolean isUserSearchEnabled = accountID.getAccountPropertyBoolean(
                IS_USER_SEARCH_ENABLED_PROPERTY, false);
            if(isUserSearchEnabled)
            {
                addSupportedOperationSet(OperationSetUserSearch.class,
                    new OperationSetUserSearchJabberImpl(this));
            }

            OperationSetTLS opsetTLS
                    = new OperationSetTLSJabberImpl(this);
            addSupportedOperationSet(OperationSetTLS.class,
                    opsetTLS);

            OperationSetConnectionInfo opsetConnectionInfo
                    = new OperationSetConnectionInfoJabberImpl();
            addSupportedOperationSet(OperationSetConnectionInfo.class,
                    opsetConnectionInfo);

            isInitialized = true;
        }
    }

    /**
     * Adds Jingle related features to the supported features.
     */
    private void addJingleFeatures()
    {
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
                true))
        {
            supportedFeatures.add(URN_XMPP_JINGLE_ICE_UDP_1);
        }

        supportedFeatures.add(URN_XMPP_JINGLE_RTP_AUDIO);
        supportedFeatures.add(URN_XMPP_JINGLE_RTP_VIDEO);
        supportedFeatures.add(URN_XMPP_JINGLE_RTP_ZRTP);

        /*
         * Reflect the preference of the user with respect to the use of
         * Jingle Nodes.
         */
        if (accountID.getAccountPropertyBoolean(
                ProtocolProviderFactoryJabberImpl.IS_USE_JINGLE_NODES,
                true))
        {
            supportedFeatures.add(URN_XMPP_JINGLE_NODES);
        }

        // XEP-0251: Jingle Session Transfer
        supportedFeatures.add(URN_XMPP_JINGLE_TRANSFER_0);

        // XEP-0320: Use of DTLS-SRTP in Jingle Sessions
        if (accountID.getAccountPropertyBoolean(
                    ProtocolProviderFactory.DEFAULT_ENCRYPTION,
                    true)
                && accountID.isEncryptionProtocolEnabled(
                        SrtpControlType.DTLS_SRTP))
        {
            supportedFeatures.add(URN_XMPP_JINGLE_DTLS_SRTP);
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
    public XMPPConnection getConnection()
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
            ((exMsg.indexOf("sasl authentication") != -1)
                    && (exMsg.indexOf("failed") != -1))
            || (exMsg.indexOf(
                    "does not support compatible authentication mechanism")
                        != -1)
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
        String reasonStr = null;

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
            else if(exMsg.indexOf("tls is required") != -1)
            {
                regState = RegistrationState.AUTHENTICATION_FAILED;
                reason = RegistrationStateChangeEvent.REASON_TLS_REQUIRED;
            }
        }

        if(regState == RegistrationState.UNREGISTERED
            || regState == RegistrationState.CONNECTION_FAILED)
        {
            // we fired that for some reason we are going offline
            // lets clean the connection state for any future connections
            disconnectAndCleanConnection();
        }

        fireRegistrationStateChanged(
            getRegistrationState(), regState, reason, reasonStr);
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
                        null);
                     return;
                }
            }
            // fire that a connection failed, the reconnection mechanism
            // will look after us and will clean us, other wise we can do
            // a dead lock (connection closed is called
            // within xmppConneciton and calling disconnect again can lock it)
            fireRegistrationStateChanged(
                getRegistrationState(),
                RegistrationState.CONNECTION_FAILED,
                RegistrationStateChangeEvent.REASON_NOT_SPECIFIED,
                null);
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
                         exception.getLocalizedMessage(), exception);

            int reason = RegistrationStateChangeEvent.REASON_NOT_SPECIFIED;

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

                    disconnectAndCleanConnection();

                    fireRegistrationStateChanged(getRegistrationState(),
                        RegistrationState.UNREGISTERED,
                        RegistrationStateChangeEvent.REASON_MULTIPLE_LOGINS,
                        "Connecting multiple times with the same resource");

                    return;
                }
            } // Ignore certificate exceptions as we handle them elsewhere
            else if(exception instanceof SSLHandshakeException &&
                exception.getCause() instanceof CertificateException)
            {
                return;
            }
            else if(exception instanceof XmlPullParserException)
            {
                reason = RegistrationStateChangeEvent
                    .REASON_SERVER_RETURNED_ERRONEOUS_INPUT;
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
                        reason,
                        exception.getMessage());
                     return;
                }
            }

            disconnectAndCleanConnection();

            fireRegistrationStateChanged(getRegistrationState(),
                RegistrationState.CONNECTION_FAILED,
                reason,
                exception.getMessage());
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
            if(discoveryManager == null)
                return isFeatureListSupported;

            DiscoverInfo featureInfo =
                discoveryManager.discoverInfoNonBlocking(jid);

            if(featureInfo == null)
                return isFeatureListSupported;

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
                logger.debug("Failed to retrive discovery info.", e);
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
     * Returns the full jabber id (jid) corresponding to the given contact. If
     * the provider is not connected returns null.
     *
     * @param contact the contact, for which we're looking for a jid
     * @return the jid of the specified contact or null if the provider is not
     * yet connected;
     */
    public String getFullJid(Contact contact)
    {
        return getFullJid(contact.getAddress());
    }

    /**
     * Returns the full jabber id (jid) corresponding to the given bare jid. If
     * the provider is not connected returns null.
     *
     * @param bareJid the bare contact address (i.e. no resource) whose full
     * jid we are looking for.
     * @return the jid of the specified contact or null if the provider is not
     * yet connected;
     */
    public String getFullJid(String bareJid)
    {
        XMPPConnection connection = getConnection();

        // when we are not connected there is no full jid
        if (connection != null && connection.isConnected())
        {
            Roster roster = connection.getRoster();

            if (roster != null)
                return roster.getPresence(bareJid).getFrom();
        }
        return null;
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
            return new X509Certificate[0];
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
            abortConnecting = true;
            try
            {
                tm.checkServerTrusted(chain, authType);
            }
            catch(CertificateException e)
            {
                // notify in a separate thread to avoid a deadlock when a
                // reg state listener accesses a synchronized XMPPConnection
                // method (like getRoster)
                new Thread(new Runnable()
                {
                    public void run()
                    {
                        fireRegistrationStateChanged(getRegistrationState(),
                            RegistrationState.UNREGISTERED,
                            RegistrationStateChangeEvent.REASON_USER_REQUEST,
                            "Not trusted certificate");
                    }
                }).start();
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

        if (connection != null)
            jid = connection.getUser();

        if (jid == null)
        {
            // seems like the connection is not yet initialized so lets try to
            // construct our jid ourselves.
            String accountIDUserID = getAccountID().getUserID();
            String userID = StringUtils.parseName(accountIDUserID);
            String serviceName = StringUtils.parseServer(accountIDUserID);

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
     * Start auto-discovery of JingleNodes tracker/relays.
     */
    public void startJingleNodesDiscovery()
    {
        // Jingle Nodes Service Initialization
        final JabberAccountIDImpl accID = (JabberAccountIDImpl)getAccountID();
        final SmackServiceNode service = new SmackServiceNode(connection,
                60000);
        // make sure SmackServiceNode will clean up when connection is closed
        connection.addConnectionListener(service);

        for(JingleNodeDescriptor desc : accID.getJingleNodes())
        {
            TrackerEntry entry = new TrackerEntry(
                    desc.isRelaySupported() ? TrackerEntry.Type.relay :
                        TrackerEntry.Type.tracker,
                    TrackerEntry.Policy._public,
                    desc.getJID(),
                    JingleChannelIQ.UDP);

            service.addTrackerEntry(entry);
        }

        new Thread(new JingleNodesServiceDiscovery(
                            service,
                            connection,
                            accID,
                            jingleNodesSyncRoot))
                .start();

        jingleNodesServiceNode = service;
    }

    /**
     * Get the Jingle Nodes service. Note that this method will block until
     * Jingle Nodes auto discovery (if enabled) finished.
     *
     * @return Jingle Nodes service
     */
    public SmackServiceNode getJingleNodesServiceNode()
    {
        synchronized(jingleNodesSyncRoot)
        {
            return jingleNodesServiceNode;
        }
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

    /**
     * Used when we need to re-register or someone needs to obtain credentials.
     * @return the SecurityAuthority.
     */
    public SecurityAuthority getAuthority()
    {
        return authority;
    }

    UserCredentials getUserCredentials()
    {
        return userCredentials;
    }

    /**
     * Returns true if our account is a Gmail or a Google Apps ones.
     *
     * @return true if our account is a Gmail or a Google Apps ones.
     */
    public boolean isGmailOrGoogleAppsAccount()
    {
        String domain = StringUtils.parseServer(
            getAccountID().getUserID());
        return isGmailOrGoogleAppsAccount(domain);
    }

    /**
     * Returns true if our account is a Gmail or a Google Apps ones.
     *
     * @param domain domain to check
     * @return true if our account is a Gmail or a Google Apps ones.
     */
    public static boolean isGmailOrGoogleAppsAccount(String domain)
    {
        SRVRecord srvRecords[] = null;

        try
        {
            srvRecords = NetworkUtils.getSRVRecords("xmpp-client", "tcp",
                domain);
        }
        catch (ParseException e)
        {
            logger.info("Failed to get SRV records for XMPP domain");
            return false;
        }
        catch (DnssecException e)
        {
            logger.error("DNSSEC failure while checking for google domains", e);
            return false;
        }

        if(srvRecords == null)
        {
            return false;
        }

        for(SRVRecord srv : srvRecords)
        {
            if(srv.getTarget().endsWith("google.com") ||
                    srv.getTarget().endsWith("google.com."))
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Sets the traffic class for the XMPP signalling socket.
     */
    private void setTrafficClass()
    {
        Socket s = connection.getSocket();

        if(s != null)
        {
            ConfigurationService configService =
                JabberActivator.getConfigurationService();
            String dscp = configService.getString(XMPP_DSCP_PROPERTY);

            if(dscp != null)
            {
                try
                {
                    int dscpInt = Integer.parseInt(dscp) << 2;

                    if(dscpInt > 0)
                        s.setTrafficClass(dscpInt);
                }
                catch (Exception e)
                {
                    logger.info("Failed to set trafficClass", e);
                }
            }
        }
    }

    /**
     * Gets the entity ID of the first Jitsi Videobridge associated with
     * {@link #connection} i.e. provided by the <tt>serviceName</tt> of
     * <tt>connection</tt>.
     *
     * @return the entity ID of the first Jitsi Videobridge associated with
     * <tt>connection</tt>
     */
    public String getJitsiVideobridge()
    {
        XMPPConnection connection = getConnection();

        if (connection != null)
        {
            ScServiceDiscoveryManager discoveryManager = getDiscoveryManager();
            String serviceName = connection.getServiceName();
            DiscoverItems discoverItems = null;

            try
            {
                discoverItems = discoveryManager.discoverItems(serviceName);
            }
            catch (XMPPException xmppe)
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug(
                            "Failed to discover the items associated with"
                                + " Jabber entity: " + serviceName,
                            xmppe);
                }
            }
            if (discoverItems != null)
            {
                Iterator<DiscoverItems.Item> discoverItemIter
                    = discoverItems.getItems();

                while (discoverItemIter.hasNext())
                {
                    DiscoverItems.Item discoverItem = discoverItemIter.next();
                    String entityID = discoverItem.getEntityID();
                    DiscoverInfo discoverInfo = null;

                    try
                    {
                        discoverInfo = discoveryManager.discoverInfo(entityID);
                    }
                    catch (XMPPException xmppe)
                    {
                        logger.warn(
                                "Failed to discover information about Jabber"
                                    + " entity: " + entityID,
                                xmppe);
                    }
                    if ((discoverInfo != null)
                            && discoverInfo.containsFeature(
                                    ColibriConferenceIQ.NAMESPACE))
                    {
                        return entityID;
                    }
                }
            }
        }

        return null;
    }

    /**
     * Load jabber service class, their static context will register
     * what is needed. Used in android as when using the other jars
     * these services are loaded from the jar manifest.
     */
    private static void loadJabberServiceClasses()
    {
        if(!OSUtils.IS_ANDROID)
            return;

        try
        {
            // pre-configure smack in android
            // just to load class to init their static blocks
            SmackConfiguration.getVersion();
            Class.forName(ServiceDiscoveryManager.class.getName());

            Class.forName(DelayInformation.class.getName());
            Class.forName(org.jivesoftware.smackx
                .provider.DelayInformationProvider.class.getName());
            Class.forName(org.jivesoftware.smackx
                .bytestreams.socks5.Socks5BytestreamManager.class.getName());
            Class.forName(XHTMLManager.class.getName());
            Class.forName(org.jivesoftware.smackx
                .bytestreams.ibb.InBandBytestreamManager.class.getName());

        }
        catch(ClassNotFoundException e)
        {
            logger.error("Error loading classes in smack", e);
        }
    }

    /**
     * Return the SSL socket (if TLS used).
     * @return The SSL socket or null if not used
     */
    public SSLSocket getSSLSocket()
    {
        final SSLSocket result;
        final Socket socket = connection.getSocket();
        if (socket instanceof SSLSocket)
        {
            result = (SSLSocket) socket;
        }
        else
        {
            result = null;
        }
        return result;
    }
}
