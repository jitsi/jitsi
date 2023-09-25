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

import java.io.*;
import java.lang.reflect.*;
import java.math.*;
import java.net.*;
import java.security.*;
import java.security.cert.*;
import java.text.*;
import java.util.*;

import javax.net.ssl.*;

import net.java.sip.communicator.impl.protocol.jabber.debugger.*;
import net.java.sip.communicator.util.osgi.ServiceUtils;
import org.jitsi.xmpp.extensions.inputevt.*;
import org.jitsi.xmpp.extensions.jingle.*;
import net.java.sip.communicator.service.certificate.*;
import net.java.sip.communicator.service.dns.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.service.protocol.jabber.*;
import net.java.sip.communicator.service.protocol.jabberconstants.*;
import net.java.sip.communicator.util.*;
import org.jitsi.service.configuration.*;
import org.jitsi.service.neomedia.*;
import org.jivesoftware.smack.*;
import org.jivesoftware.smack.SmackException.*;
import org.jivesoftware.smack.bosh.*;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.tcp.*;
import org.jivesoftware.smack.util.*;
import org.jivesoftware.smack.roster.*;
import org.jivesoftware.smackx.disco.packet.*;
import org.jivesoftware.smackx.message_correct.element.*;
import org.jivesoftware.smackx.ping.*;
import org.jxmpp.jid.*;
import org.jxmpp.jid.impl.*;
import org.jxmpp.jid.parts.*;
import org.jxmpp.stringprep.XmppStringprepException;
import org.minidns.dnsname.*;
import org.xmlpull.v1.*;
import org.xmpp.jnodes.smack.*;

import static org.jivesoftware.smack.ConnectionConfiguration.SecurityMode.*;
import static net.java.sip.communicator.service.certificate.CertificateService.*;

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
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ProtocolProviderServiceJabberImpl.class);

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
     * Property to disable instant messaging (not muc and muc messaging).
     */
    private static final String IS_IM_DISABLED_PROPERTY
        = "IM_DISABLED";

    /**
     * Property to disable server stored info retrieval and manipulation, as
     * contact and account info and also avatar retrieval.
     */
    public static final String IS_SERVER_STORED_INFO_DISABLED_PROPERTY
        = "SERVER_STORED_INFO_DISABLED";

    /**
     * Property to disable file transfer.
     */
    private static final String IS_FILE_TRANSFER_DISABLED_PROPERTY
        = "IS_FILE_TRANSFER_DISABLED";

    /**
     * Google voice domain name.
     */
    public static final String GOOGLE_VOICE_DOMAIN = "voice.google.com";

    /**
     * Used to connect to a XMPP server.
     */
    private AbstractXMPPConnection connection;

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
    private JabberAccountID accountID = null;

    /**
     * Used when we need to re-register
     */
    private SecurityAuthority authority = null;

    /**
     * The resource we will use when connecting during this run.
     */
    private Resourcepart resource = null;

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
    private final List<String> supportedFeatures = new ArrayList<>();

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
     * Whether keep-alive is enabled for current account.
     */
    private boolean isKeepAliveEnabled = false;

    /**
     * The ping listener if enabled that will trigger reconnection.
     */
    private PingFailedListenerImpl pingFailedListener = null;

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
        {
            if (inConnectAndLogin)
            {
                return RegistrationState.REGISTERING;
            }

            return RegistrationState.UNREGISTERED;
        }
        else if(connection.isConnected() && connection.isAuthenticated())
        {
            return RegistrationState.REGISTERED;
        }
        else
        {
            return RegistrationState.REGISTERING;
        }
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
        catch (XMPPException
            | SmackException
            | InterruptedException
            | IOException ex)
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
        catch (XmppStringprepException e)
        {
            logger.error("User ID is not a valid JID", e);
            fireRegistrationStateChanged(getRegistrationState(),
                RegistrationState.CONNECTION_FAILED,
                RegistrationStateChangeEvent.REASON_NON_EXISTING_USER_ID,
                e.toString());
        }
        catch(XMPPException
            | OperationFailedException
            | InterruptedException
            | IOException
            | SmackException ex)
        {
            logger.error("Error ReRegistering", ex);

            eventDuringLogin = null;

            disconnectAndCleanConnection();

            fireRegistrationStateChanged(getRegistrationState(),
                RegistrationState.CONNECTION_FAILED,
                RegistrationStateChangeEvent.REASON_INTERNAL_ERROR,
                ex.getMessage());
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
        return connection.isSecureConnection();
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
            if(isSignalingTransportSecure())
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
        throws XMPPException, OperationFailedException, IOException, InterruptedException, SmackException
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
            {
                logger.warn("Unsuccessful login, skipping.");
                return;
            }

            DomainBareJid serviceName = JidCreate.domainBareFrom(
                getAccountID().getUserID());

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
                state = connectUsingSRVRecords(serviceName.toString(),
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
                    catch(XMPPException
                        | InterruptedException
                        | IOException
                        | SmackException ex)
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
        String boshURL = accountID.getBoshUrl();
        boolean isBosh = org.apache.commons.lang3.StringUtils.isNotEmpty(boshURL);
        ConnectionConfiguration.Builder ccBuilder;
        if (isBosh)
        {
            ccBuilder = BOSHConfiguration.builder();
        }
        else
        {
            ccBuilder = XMPPTCPConnectionConfiguration.builder();
        }

        if (((JabberAccountIDImpl)getAccountID()).isAnonymousAuthUsed())
        {
            return new AnonymousLoginStrategy(
                getAccountID().getAuthorizationName(), ccBuilder);
        }

        String clientCertId = getAccountID().getAccountPropertyString(
                ProtocolProviderFactory.CLIENT_TLS_CERTIFICATE);
        if(clientCertId != null)
        {
            return new LoginByClientCertificateStrategy(accountID, ccBuilder);
        }
        else
        {
            return new LoginByPasswordStrategy(this, accountID, ccBuilder);
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
        DomainBareJid serviceName,
        boolean[] dnssecState,
        JabberLoginStrategy loginStrategy)
        throws XMPPException, IOException, InterruptedException, SmackException
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
                        return connectAndLogin(
                            isa, serviceName, loginStrategy);
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
        DomainBareJid serviceName,
        JabberLoginStrategy loginStrategy)
        throws XMPPException, IOException, InterruptedException, SmackException
    {
        try
        {
            EntityBareJid userID =
                JidCreate.entityBareFrom(getAccountID().getUserID());
            return connectAndLogin(
                currentAddress, serviceName,
                JidCreate.entityFullFrom(userID, resource),
                loginStrategy);
        }
        catch (ConnectException
            | NoRouteToHostException
            | NoResponseException ex)
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
        catch(XMPPException
            | IOException
            | InterruptedException
            | SmackException ex)
        {
            logger.error("Failed to connect to XMPP service for:" + this, ex);

            // server disconnect us after such an error, do cleanup
            // as we maybe will try again
            disconnectAndCleanConnection();

            // in case this is the last try connecting store the error
            eventDuringLogin = new RegistrationStateChangeEvent(
                this,
                getRegistrationState(),
                RegistrationState.CONNECTION_FAILED,
                RegistrationStateChangeEvent.REASON_INTERNAL_ERROR,
                ex.getMessage());

            return ConnectState.CONTINUE_TRYING;
        }
    }

    /**
     * Initializes the Jabber Resource identifier.
     */
    private void loadResource()
    {
        if(resource != null)
        {
            return;
        }

        SecureRandom random = new SecureRandom();
        try
        {
            resource = Resourcepart.from("jitsi-" +
                new BigInteger(32, random).toString(32));
        }
        catch (XmppStringprepException e)
        {
            // we know the input, this doesn't happen
        }

        boolean autoGenerateResource =
            getAccountID().getAccountPropertyBoolean(
                ProtocolProviderFactory.AUTO_GENERATE_RESOURCE, true);
        if(!autoGenerateResource)
        {
            try
            {
                Resourcepart configured = Resourcepart.from(
                    getAccountID().getAccountPropertyString(
                        ProtocolProviderFactory.RESOURCE));
                if (!Resourcepart.EMPTY.equals(configured))
                {
                    resource = configured;
                }
            }
            catch (XmppStringprepException e)
            {
                // okay, the autogenerated resource is still set
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
        if(globalProxyType != null &&
           !globalProxyType.equals(ProxyInfo.ProxyType.NONE.name()))
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
                globalProxyPort = Integer.parseInt(globalProxyPortStr);
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
     * @param jid the username and the resource.
     * @param loginStrategy the login strategy to use
     * @return return the state how to continue the connect process.
     * @throws XMPPException if we cannot connect for some reason
     */
    private ConnectState connectAndLogin(
            InetSocketAddress address, DomainBareJid serviceName,
            EntityFullJid jid,
            JabberLoginStrategy loginStrategy)
        throws XMPPException, InterruptedException, IOException, SmackException
    {
        // BOSH or TCP ?
        ConnectionConfiguration.Builder confConn =
            loginStrategy.getConnectionConfigurationBuilder();
        String boshURL = accountID.getBoshUrl();
        boolean isBosh = org.apache.commons.lang3.StringUtils.isNotEmpty(boshURL);

        confConn.setXmppDomain(serviceName);
        if (isBosh)
        {
            BOSHConfiguration.Builder boshConfConnBuilder =
                (BOSHConfiguration.Builder)confConn;

            try
            {
                URI boshURI = new URI(boshURL);
                boolean useHttps = boshURI.getScheme().equals("https");

                int port = boshURI.getPort();
                if (port == -1)
                {
                    port = useHttps ? 443 : 80;
                }

                String file = boshURI.getPath();
                // use rawQuery as getQuery() decodes the string
                String query = boshURI.getRawQuery();
                if (org.apache.commons.lang3.StringUtils.isNotEmpty(query))
                {
                    file += "?" + query;
                }

                boshConfConnBuilder
                    .setUseHttps(useHttps)
                    .setFile(file)
                    .setPort(port)
                    .setHost(DnsName.from(boshURI.getHost()))
                    .setProxyInfo(proxy);
            }
            catch (URISyntaxException e)
            {
                throw new JitsiXmppException(
                    "Fail setting bosh URL to XMPPBOSHConnection configuration",
                    e);
            }
        }
        else
        {
            confConn.setHostAddress(address.getAddress())
                    .setPort(address.getPort())
                    .setProxyInfo(proxy);
        }

        // if we have OperationSetPersistentPresence skip sending initial
        // presence while login is executed, the OperationSet will take care
        // of it
        if(getOperationSet(OperationSetPersistentPresence.class) != null)
            confConn.setSendPresence(false);

        // user have the possibility to disable TLS but in this case, it will
        // not be able to connect to a server which requires TLS
        confConn.setSecurityMode(loginStrategy.isTlsRequired()
                ? required
                : ifpossible);

        confConn.setEnabledSSLProtocols(
            new String[] { TLSUtils.PROTO_TLSV1_2 });

        if(connection != null)
        {
            logger.error("Connection is not null and isConnected:"
                + connection.isConnected(),
                new Exception("Trace possible duplicate connections: " +
                    getAccountID().getAccountAddress()));
            disconnectAndCleanConnection();
        }

        // check and default configurations for property
        // if missing default is null - false
        String defaultAlwaysTrustMode = JabberActivator.getResources().getSettingsString(PNAME_ALWAYS_TRUST);

        if(JabberActivator.getConfigurationService().getBoolean(PNAME_ALWAYS_TRUST,
            Boolean.parseBoolean(defaultAlwaysTrustMode)))
        {
            // install all trust manager
            confConn.setCustomX509TrustManager(new TrustAllX509TrustManager());
            // install all hosts verified
            confConn.setHostnameVerifier((hostname, session) -> true);
        }
        else
        {
            try
            {
                CertificateService cvs = getCertificateVerificationService();
                if (cvs != null)
                {
                    var tm = getTrustManager(cvs, serviceName.toString());
                    var km = loginStrategy.getKeyManager(cvs);
                    var sslContext = loginStrategy.createSslContext(cvs, tm);

                    // log SSL/TLS algorithms and protocols
                    if (logger.isDebugEnabled())
                    {
                        logger.debug(
                            "Available TLS protocols and algorithms:\nDefault protocols: {}\nSupported protocols: {}\nDefault cipher suites: {}\nSupported cipher suites: {}",
                            Arrays.toString(sslContext.getDefaultSSLParameters().getProtocols()),
                            Arrays.toString(sslContext.getSupportedSSLParameters().getProtocols()),
                            Arrays.toString(sslContext.getDefaultSSLParameters().getCipherSuites()),
                            Arrays.toString(sslContext.getSupportedSSLParameters().getCipherSuites()));
                    }

                    confConn.setSslContextFactory(() -> sslContext);
                    confConn.setCustomX509TrustManager(tm);
                    confConn.setKeyManagers(km);

                    // this is safe because our trust manager already verifies the hostname!
                    confConn.setHostnameVerifier((hostname, session) -> true);
                }
                else if (loginStrategy.isTlsRequired())
                    throw new JitsiXmppException("Certificate verification service is unavailable and TLS is required");
            }
            catch (GeneralSecurityException e)
            {
                throw new JitsiXmppException("Error creating custom trust manager", e);
            }
        }

        if (isBosh)
        {
            connection =
                new XMPPBOSHConnection((BOSHConfiguration) confConn.build());
        }
        else
        {
            connection =
                new XMPPTCPConnection(
                    (XMPPTCPConnectionConfiguration) confConn.build());
        }

        ReconnectionManager.getInstanceFor(connection).disableAutomaticReconnection();
        this.address = address;

        if(debugger == null)
        {
            // FIXME Smack4.2: implement the smack debugger interface,
            // the StanzaListener won't catch IQs anymore
            debugger = new SmackPacketDebugger();

            // sets the debugger
            debugger.setConnection(connection);
            connection.addAsyncStanzaListener(debugger.inbound, null);
            connection.addStanzaInterceptor(debugger.outbound, null);
        }

        int keepAliveInterval =
                this.getAccountID().getAccountPropertyInt(
                        ProtocolProviderFactory.KEEP_ALIVE_INTERVAL, -1);
        if (this.isKeepAliveEnabled && keepAliveInterval > 0)
        {
            PingManager pm = PingManager.getInstanceFor(connection);
            pm.setPingInterval(keepAliveInterval);

            if (pingFailedListener == null)
            {
                pingFailedListener = new PingFailedListenerImpl();
            }
            pm.registerPingFailedListener(pingFailedListener);
        }

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

        if(!connection.isSecureConnection() && loginStrategy.isTlsRequired())
        {
            throw new JitsiXmppException("TLS is required by client");
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

        if (!loginStrategy.login(connection, jid))
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
    private X509ExtendedTrustManager getTrustManager(CertificateService cvs,
        String serviceName)
        throws GeneralSecurityException
    {
        return new HostTrustManager(
            cvs.getTrustManager(
                Arrays.asList(serviceName, "_xmpp-client." + serviceName)
            )
        );
    }

    /**
     * Registers our ServiceDiscoveryManager
     */
    private void registerServiceDiscoveryManager()
    {
        discoveryManager
            = new ScServiceDiscoveryManager(
                    this,
                    JabberActivator.getConfigurationService(),
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
     * Adds a supported feature to the list and if available to the discovery manager.
     * @param featureName the new feature to add.
     */
    void addSupportedFeature(String featureName)
    {
        supportedFeatures.add(featureName);
        if (discoveryManager != null)
        {
            getDiscoveryManager().addFeature(featureName);
        }
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
                    && org.apache.commons.lang3.StringUtils
                        .isNotEmpty(opSet.getCurrentStatusMessage()))
                {
                    unavailablePresence.setStatus(
                        opSet.getCurrentStatusMessage());
                }

                connection.disconnect(unavailablePresence);
            } catch (Exception e)
            {}

            if (pingFailedListener != null)
            {
                PingManager.getInstanceFor(connection).unregisterPingFailedListener(pingFailedListener);
                pingFailedListener = null;
            }

            if (debugger != null)
            {
                debugger.setConnection(null);
                debugger = null;
            }

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

        synchronized(initializationLock)
        {
            disconnectAndCleanConnection();
        }

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
    protected void initialize(EntityBareJid screenname,
                              JabberAccountID accountID)
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

            ScServiceDiscoveryManager.initIdentity();

            //this feature is mandatory to be compliant with Service Discovery
            supportedFeatures.add("http://jabber.org/protocol/disco#info");

            String keepAliveStrValue
                = accountID.getAccountPropertyString(
                    ProtocolProviderFactory.KEEP_ALIVE_METHOD);

            boolean isServerStoredInfoEnabled
                = !accountID.getAccountPropertyBoolean(
                    IS_SERVER_STORED_INFO_DISABLED_PROPERTY, false);

            InfoRetreiver infoRetreiver = null;
            if (isServerStoredInfoEnabled)
            {
                infoRetreiver = new InfoRetreiver(this);
            }

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

            if (keepAliveStrValue == null
                || keepAliveStrValue.equalsIgnoreCase("XEP-0199"))
            {
                // force class init of the ping manager
                PingManager.class.getName();

                isKeepAliveEnabled = true;
            }

            if(!accountID.getAccountPropertyBoolean(
                IS_IM_DISABLED_PROPERTY, false))
            {
                //initialize the IM operation set
                OperationSetBasicInstantMessagingJabberImpl
                    basicInstantMessaging
                    = new OperationSetBasicInstantMessagingJabberImpl(this);

                addSupportedOperationSet(
                    OperationSetBasicInstantMessaging.class,
                    basicInstantMessaging);

                //initialize the typing notifications operation set
                addSupportedOperationSet(
                    OperationSetTypingNotifications.class,
                    new OperationSetTypingNotificationsJabberImpl(this));

                // The http://jabber.org/protocol/chatstates feature implemented
                // in OperationSetTypingNotifications is included already
                // in smack.

                addSupportedOperationSet(
                    OperationSetInstantMessageTransform.class,
                    new OperationSetInstantMessageTransformImpl());

                supportedFeatures.add(MessageCorrectExtension.NAMESPACE);
                addSupportedOperationSet(OperationSetMessageCorrection.class,
                    basicInstantMessaging);
            }

            // The http://jabber.org/protocol/xhtml-im feature is included
            // already in smack.

            addSupportedOperationSet(
                OperationSetExtendedAuthorizations.class,
                new OperationSetExtendedAuthorizationsJabberImpl(
                    this,
                    persistentPresence));

            //initialize the multi user chat operation set
            addSupportedOperationSet(
                OperationSetMultiUserChat.class,
                new OperationSetMultiUserChatJabberImpl(this));

            addSupportedOperationSet(
                OperationSetJitsiMeetToolsJabber.class,
                new OperationSetJitsiMeetToolsJabberImpl(this));

            if(isServerStoredInfoEnabled)
            {
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
            }

            if(!accountID.getAccountPropertyBoolean(
                IS_FILE_TRANSFER_DISABLED_PROPERTY, false))
            {
                // initialize the file transfer operation set
                addSupportedOperationSet(
                    OperationSetFileTransfer.class,
                    new OperationSetFileTransferJabberImpl(this));

                // Include features we're supporting in addition to the four
                // included by smack itself:
                // http://jabber.org/protocol/si/profile/file-transfer
                // http://jabber.org/protocol/si
                // http://jabber.org/protocol/bytestreams
                // http://jabber.org/protocol/ibb

                supportedFeatures.add("urn:xmpp:bob");
                supportedFeatures.add("urn:xmpp:thumbs:0");
            }

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

                addSupportedOperationSet(
                    OperationSetTelephonyConferencing.class,
                    new OperationSetTelephonyConferencingJabberImpl(this));

                addSupportedOperationSet(
                    OperationSetBasicAutoAnswer.class,
                    new OperationSetAutoAnswerJabberImpl(this));

                addSupportedOperationSet(
                    OperationSetResourceAwareTelephony.class,
                    new OperationSetResAwareTelephonyJabberImpl(basicTelephony));

                // init DTMF
                OperationSetDTMFJabberImpl operationSetDTMF
                    = new OperationSetDTMFJabberImpl(this);
                addSupportedOperationSet(
                    OperationSetDTMF.class, operationSetDTMF);

                addSupportedOperationSet(
                    OperationSetIncomingDTMF.class,
                    new OperationSetIncomingDTMFJabberImpl());

                addJingleFeatures();

                boolean isVideoCallingDisabledForAccount
                    = accountID.getAccountPropertyBoolean(
                        ProtocolProviderFactory
                            .IS_VIDEO_CALLING_DISABLED_FOR_ACCOUNT,
                        false);

                // initialize video telephony OperationSet
                if (!isVideoCallingDisabledForAccount)
                {
                    supportedFeatures.add(URN_XMPP_JINGLE_RTP_VIDEO);

                    addSupportedOperationSet(
                        OperationSetVideoTelephony.class,
                        new OperationSetVideoTelephonyJabberImpl(
                            basicTelephony));

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

                        if (!accountID.getAccountPropertyBoolean(
                            ProtocolProviderFactory
                                .IS_DESKTOP_REMOTE_CONTROL_DISABLED,
                            false))
                        {
                            // initialize desktop sharing OperationSets
                            addSupportedOperationSet(
                                OperationSetDesktopSharingServer.class,
                                new OperationSetDesktopSharingServerJabberImpl(
                                    basicTelephony));

                            // Adds extension to support remote control as a
                            // sharing server (sharer).
                            supportedFeatures.add(InputEvtIQ.NAMESPACE_SERVER);

                            addSupportedOperationSet(
                                OperationSetDesktopSharingClient.class,
                                new OperationSetDesktopSharingClientJabberImpl(
                                    this)
                            );
                            // Adds extension to support remote control as
                            // a sharing client (sharer).
                            supportedFeatures.add(InputEvtIQ.NAMESPACE_CLIENT);
                        }
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

            OperationSetChangePassword opsetChangePassword
                    = new OperationSetChangePasswordJabberImpl(this);
            addSupportedOperationSet(OperationSetChangePassword.class,
                    opsetChangePassword);

            OperationSetCusaxUtils opsetCusaxCusaxUtils
                    = new OperationSetCusaxUtilsJabberImpl();
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
     * Validates the node part of a JID and returns an error message if
     * applicable and a suggested correction.
     *
     * @param contactId the contact identifier to validate
     * @param result Must be supplied as an empty a list. Implementors add
     *            items:
     *            <ol>
     *            <li>is the error message if applicable
     *            <li>a suggested correction. Index 1 is optional and can only
     *            be present if there was a validation failure.
     *            </ol>
     * @return true if the contact id is valid, false otherwise
     */
    @Override
    public boolean validateContactAddress(String contactId, List<String> result)
    {
        if (result == null)
        {
            throw new IllegalArgumentException("result must be an empty list");
        }

        result.clear();
        try
        {
            contactId = contactId.trim();
            if (contactId.length() == 0)
            {
                result.add(JabberActivator.getResources().getI18NString(
                    "impl.protocol.jabber.INVALID_ADDRESS", new String[]
                { contactId }));
                // no suggestion for an empty id
                return false;
            }

            String user = contactId;
            String remainder = "";
            int at = contactId.indexOf('@');
            if (at > -1)
            {
                user = contactId.substring(0, at);
                remainder = contactId.substring(at);
            }

            // <conforming-char> ::= #x21 | [#x23-#x25] | [#x28-#x2E] |
            // [#x30-#x39] | #x3B | #x3D | #x3F |
            // [#x41-#x7E] | [#x80-#xD7FF] |
            // [#xE000-#xFFFD] | [#x10000-#x10FFFF]
            boolean valid = true;
            String suggestion = "";
            for (char c : user.toCharArray())
            {
                if (!(c == 0x21 || (c >= 0x23 && c <= 0x25)
                    || (c >= 0x28 && c <= 0x2e) || (c >= 0x30 && c <= 0x39)
                    || c == 0x3b || c == 0x3d || c == 0x3f
                    || (c >= 0x41 && c <= 0x7e) || (c >= 0x80 && c <= 0xd7ff)
                    || (c >= 0xe000 && c <= 0xfffd)))
                {
                    valid = false;
                }
                else
                {
                    suggestion += c;
                }
            }

            if (!valid)
            {
                result.add(JabberActivator.getResources().getI18NString(
                    "impl.protocol.jabber.INVALID_ADDRESS", new String[]
                { contactId }));
                result.add(suggestion + remainder);
                return false;
            }

            return true;
        }
        catch (Exception ex)
        {
            result.add(JabberActivator.getResources().getI18NString(
                "impl.protocol.jabber.INVALID_ADDRESS", new String[]
            { contactId }));
        }

        return false;
    }

    /**
     * Returns the <tt>Connection</tt>opened by this provider
     * @return a reference to the <tt>Connection</tt> last opened by this
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
    private boolean isAuthenticationFailed(Exception ex)
    {
        String exMsg = ex.getMessage().toLowerCase();

        //FIXME
        // as there are no types or reasons for XMPPException
        // we try determine the reason according to their message
        // all messages that were found in smack 3.1.0 were took in count
        return
            ((exMsg.contains("sasl authentication") && exMsg.contains("failed"))
            || (exMsg.contains("does not support compatible authentication mechanism"))
            || (exMsg.contains("unable to determine password")));
    }

    /**
     * Tries to determine the appropriate message and status to fire,
     * according the exception.
     *
     * @param ex the {@link XMPPException} that caused the state change.
     */
    private void fireRegistrationStateChanged(Exception ex)
    {
        int reason = RegistrationStateChangeEvent.REASON_NOT_SPECIFIED;
        RegistrationState regState = RegistrationState.UNREGISTERED;
        String reasonStr = null;

        if(ex instanceof UnknownHostException
            || ex instanceof ConnectException
            || ex instanceof SocketException)
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
        @Override
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
            // within xmppConnection and calling disconnect again can lock it)
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
        @Override
        public void connectionClosedOnError(Exception exception)
        {
            logger.error("connectionClosedOnError " +
                         exception.getLocalizedMessage(), exception);

            int reason = RegistrationStateChangeEvent.REASON_NOT_SPECIFIED;

            if(exception instanceof XMPPException.StreamErrorException)
            {
                StreamError err = ((XMPPException.StreamErrorException)exception).getStreamError();

                if(err != null && err.getCondition() == StreamError.Condition.conflict)
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

            fireRegistrationStateChanged(getRegistrationState(),
                RegistrationState.CONNECTION_FAILED,
                reason,
                exception.getMessage());

            disconnectAndCleanConnection();
        }

        @Override
        public void connected(XMPPConnection xmppConnection)
        {
            logger.info("Connected");
        }

        @Override
        public void authenticated(XMPPConnection xmppConnection, boolean b)
        {
            logger.info("Authenticated: " + b);
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
    public boolean isFeatureListSupported(Jid jid, String... features)
    {
        if(discoveryManager == null)
            return false;

        DiscoverInfo featureInfo =
            discoveryManager.discoverInfoNonBlocking(jid);

        if(featureInfo == null)
            return false;

        for (String feature : features)
        {
            if (!featureInfo.containsFeature(feature))
            {
                // If one is not supported we return false and don't check
                // the others.
                return false;
            }
        }

        return true;
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
    public boolean isFeatureSupported(Jid jid, String feature)
    {
        return isFeatureListSupported(jid, feature);
    }

    /**
     * Returns the full jabber id (jid) corresponding to the given contact. If
     * the provider is not connected returns null.
     *
     * @param contact the contact, for which we're looking for a jid
     * @return the jid of the specified contact or null if the provider is not
     * yet connected; The Jid can still be bare if there's no presence available.
     */
    public Jid getFullJid(Contact contact) throws XmppStringprepException
    {
        return getFullJid(JidCreate.bareFrom(contact.getAddress()));
    }

    /**
     * Returns the full jabber id (jid) corresponding to the given bare jid. If
     * the provider is not connected returns null.
     *
     * @param bareJid the bare contact address (i.e. no resource) whose full
     * jid we are looking for.
     * @return the jid of the specified contact or null if the provider is not
     * yet connected; The Jid can still be bare if there's no presence available.
     */
    public Jid getFullJid(BareJid bareJid)
    {
        XMPPConnection connection = getConnection();

        // when we are not connected there is no full jid
        if (connection == null || !connection.isConnected())
        {
            return null;
        }

        Roster roster = Roster.getInstanceFor(connection);
        if (roster != null)
            return roster.getPresence(bareJid).getFrom();

        return null;
    }

    /**
     * The trust manager which asks the client whether to trust particular
     * certificate which is not globally trusted.
     */
    private class HostTrustManager
        extends X509ExtendedTrustManager
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

        @Override
        public void checkClientTrusted(X509Certificate[] chain,
            String authType, Socket socket) throws CertificateException
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain,
            String authType, Socket socket) throws CertificateException
        {
            checkServerTrusted(chain, authType);
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain,
            String authType, SSLEngine engine) throws CertificateException
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain,
            String authType, SSLEngine engine) throws CertificateException
        {
            checkServerTrusted(chain, authType);
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
                new Thread(() -> fireRegistrationStateChanged(getRegistrationState(),
                    RegistrationState.UNREGISTERED,
                    RegistrationStateChangeEvent.REASON_USER_REQUEST,
                    "Not trusted certificate")).start();
                throw e;
            }

            if (abortConnecting)
            {
                // connect hasn't finished we will continue normally
                abortConnecting = false;
            }
            else
            {
                // in this situation connect method has finished
                // and it was disconnected so we wont to connect.

                // register.connect in new thread so we can release the
                // current connecting thread, otherwise this blocks
                // jabber
                new Thread(() -> reregister(SecurityAuthority.CONNECTION_FAILED)).start();
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
    public Jid getOurJID()
    {
        Jid jid = null;

        if (connection != null)
            jid = connection.getUser();

        if (jid == null)
        {
            // seems like the connection is not yet initialized so lets try to
            // construct our jid ourselves.
            String accountIDUserID = getAccountID().getUserID();
            try
            {
                jid = JidCreate.bareFrom(accountIDUserID);
            }
            catch (XmppStringprepException e)
            {
                logger.error("Invalid JID", e);
                return null;
            }
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

        if (proxy != null)
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

        if(!accID.isJingleNodesRelayEnabled())
            return;

        final SmackServiceNode service
            = new SmackServiceNode(connection, 60000);
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
    public static void throwOperationFailedException(String message,
        int errorCode,
        Throwable cause,
        org.slf4j.Logger logger)
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
        try
        {
            String domain = JidCreate.domainBareFrom(
                getAccountID().getUserID()).toString();
            return isGmailOrGoogleAppsAccount(domain);
        }
        catch (XmppStringprepException e)
        {
            return false;
        }
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
        Socket s = getSocket();

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
     * Obtains XMPP connection's socket.
     * @return <tt>Socket</tt> instance used by the underlying XMPP connection
     * or <tt>null</tt> if "non socket" type of transport is currently used.
     */
    private Socket getSocket()
    {
        if (connection == null)
        {
            return null;
        }

        if (connection instanceof XMPPTCPConnection)
        {
            try
            {
                Field socket = connection.getClass().getField("socket");
                socket.setAccessible(true);
                return (Socket)socket.get(connection);
            }
            catch (NoSuchFieldException | IllegalAccessException e)
            {
                return null;
            }
        }

        return null;
    }

    /**
     * Return the SSL socket (if TLS used).
     * @return The SSL socket or null if not used
     */
    SSLSocket getSSLSocket()
    {
        final Socket socket = getSocket();

        return (socket instanceof SSLSocket) ? (SSLSocket) socket : null;
    }

    /**
     * Trust all hosts manager, used when {@link CertificateService}.PNAME_ALWAYS_TRUST is enabled.
     */
    public static class TrustAllX509TrustManager
        implements X509TrustManager
    {
        @Override
        public void checkClientTrusted(X509Certificate[] c, String s)
        {}

        @Override
        public void checkServerTrusted(X509Certificate[] c, String s)
        {}

        @Override
        public X509Certificate[] getAcceptedIssuers()
        {
            return new X509Certificate[0];
        }
    }

    /**
     * Detects ping failures and if the connection is still connected and authenticated we will fire connection failed.
     */
    private class PingFailedListenerImpl
        implements PingFailedListener
    {
        @Override
        public void pingFailed()
        {
            logger.warn("Ping failed, the XMPP connection needs to reconnect.");

            XMPPConnection xmppConnection = getConnection();

            if (xmppConnection.isConnected() && xmppConnection.isAuthenticated())
            {
                logger.warn("XMPP connection still connected, will trigger a disconnect.");
                // XMPP connection is connected and authenticated.
                // This is a weird situation that we have seen in the past when using VPN.
                // Everything stays like this forever as the socket remains open on the OS level
                // and it is never dropped. We will trigger reconnect just in case.
                fireRegistrationStateChanged(getRegistrationState(),
                    RegistrationState.CONNECTION_FAILED,
                    RegistrationStateChangeEvent.REASON_TIMEOUT,
                    "Ping failed");
            }
        }
    }
}
