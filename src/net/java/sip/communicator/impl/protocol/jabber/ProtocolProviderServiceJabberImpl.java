/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber;

import java.net.*;
import java.text.*;
import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.service.protocol.jabberconstants.*;
import net.java.sip.communicator.util.*;

import org.jivesoftware.smack.*;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.util.*;
import org.jivesoftware.smackx.*;
import org.jivesoftware.smackx.packet.*;

/**
 * An implementation of the protocol provider service over the Jabber protocol
 *
 * @author Damian Minkov
 * @author Symphorien Wanko
 * @author Lubomir Marinov
 * @author Yana Stamcheva
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
     * The name of the property that tells us whether we are supposed to start
     * experimental support for Jingle.
     */
    private static final String PNAME_ENABLE_JINGLE = "net.java.sip"
        + ".communicator.impl.protocol.jabber.ENABLE_EXPERIMENTAL_JINGLE";

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
     * True if we are reconnecting, false otherwise.
     */
    private boolean reconnecting = false;

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
    private ServiceDiscoveryManager discoveryManager = null;

    private JabberStatusEnum jabberStatusEnum;

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
            connectAndLogin(authority,
                            SecurityAuthority.AUTHENTICATION_REQUIRED);
        }
        catch (XMPPException ex)
        {
            logger.error("Error registering", ex);

            int reason
                = RegistrationStateChangeEvent.REASON_NOT_SPECIFIED;

            RegistrationState regState = RegistrationState.UNREGISTERED;

            if(ex.getWrappedThrowable() instanceof UnknownHostException)
            {
                reason
                    = RegistrationStateChangeEvent.REASON_SERVER_NOT_FOUND;
                regState = RegistrationState.CONNECTION_FAILED;
            }
            else
                if((connection.getSASLAuthentication() != null &&
                    connection.getSASLAuthentication().isAuthenticated()) ||
                    !connection.isAuthenticated())
                {
                    JabberActivator.getProtocolProviderFactory().
                        storePassword(getAccountID(), null);
                    reason = RegistrationStateChangeEvent
                        .REASON_AUTHENTICATION_FAILED;

                    regState = RegistrationState.AUTHENTICATION_FAILED;

                    // Try to reregister and to ask user for a new password.
                    reregister(SecurityAuthority.WRONG_PASSWORD);
                }

            fireRegistrationStateChanged(
                getRegistrationState(), regState, reason, null);
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
            logger.trace("Trying to reregister us!");

            // sets this if any is tring to use us through registration
            // to know we are not registered
            this.unregister(false);

            this.reconnecting = true;

            connectAndLogin(authority,
                            authReasonCode);
        }
        catch(OperationFailedException ex)
        {
            logger.error("Error ReRegistering", ex);

            fireRegistrationStateChanged(getRegistrationState(),
                RegistrationState.CONNECTION_FAILED,
                RegistrationStateChangeEvent.REASON_INTERNAL_ERROR, null);
        }
        catch (XMPPException ex)
        {
            logger.error("Error ReRegistering", ex);

            int reason =
                RegistrationStateChangeEvent.REASON_NOT_SPECIFIED;

            if(ex.getWrappedThrowable() instanceof UnknownHostException)
                reason =
                    RegistrationStateChangeEvent.REASON_SERVER_NOT_FOUND;

            fireRegistrationStateChanged(getRegistrationState(),
                RegistrationState.CONNECTION_FAILED, reason, null);
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
                //XMPPConnection.DEBUG_ENABLED = true;
                String userID =
                    StringUtils.parseName(getAccountID().getUserID());
                String serviceName =
                    StringUtils.parseServer(getAccountID().getUserID());

                String serverAddress =
                    getAccountID().getAccountPropertyString(
                        ProtocolProviderFactory.SERVER_ADDRESS);

                String serverPort =
                    getAccountID().getAccountPropertyString(
                        ProtocolProviderFactory.SERVER_PORT);

                String accountResource =
                    getAccountID().getAccountPropertyString(
                        ProtocolProviderFactory.RESOURCE);

                // check to see is there SRV records for this server domain
                try
                {
                    InetSocketAddress srvAddress = NetworkUtils
                        .getSRVRecord("xmpp-client", "tcp", serviceName);

                    if (srvAddress != null)
                        serverAddress = srvAddress.getHostName();

                }
                catch (ParseException ex1)
                {
                    logger.error("Domain not resolved " + ex1.getMessage());
                }

                Roster.setDefaultSubscriptionMode(Roster.SubscriptionMode.manual);

                try
                {
                    ConnectionConfiguration confConn =
                    new ConnectionConfiguration(
                            serverAddress,
                            Integer.parseInt(serverPort),
                            serviceName
                    );
                    confConn.setReconnectionAllowed(false);
                    connection = new XMPPConnection(confConn);

                    connection.connect();
                }
                catch (XMPPException exc)
                {
                    logger.error("Failed to establish a Jabber connection for "
                        + getAccountID().getAccountUniqueID(), exc);

                    throw new OperationFailedException(
                        "Failed to establish a Jabber connection for "
                        + getAccountID().getAccountUniqueID()
                        , OperationFailedException.NETWORK_FAILURE
                        , exc);
                }

                connection.addConnectionListener(
                    new JabberConnectionListener());

                fireRegistrationStateChanged(
                        getRegistrationState()
                        , RegistrationState.REGISTERING
                        , RegistrationStateChangeEvent.REASON_NOT_SPECIFIED
                        , null);

                if(accountResource == null || accountResource.equals(""))
                    accountResource = "sip-comm";

                SASLAuthentication.supportSASLMechanism("PLAIN", 0);

                try
                {
                    connection.login(userID, password, accountResource);
                } catch (XMPPException e1)
                {
                    // after updating to new smack lib
                    // login mechanisum changed
                    // this is a way to avoid the problem
                    try
                    {
                        // server disconnect us after such un error
                        // cleanup
                        try
                        {
                            connection.disconnect();
                        } catch (Exception e)
                        {}
                        // and connect again
                        connection.connect();
                        // logging in to google need and service name
                        connection.login(userID + "@" + serviceName,
                                password, accountResource);
                    } catch (XMPPException e2)
                    {
                        // if it happens once again throw the original exception
                        throw e1;
                    }
                }

                if(connection.isAuthenticated())
                {
                    this.reconnecting = false;

                    connection.getRoster().
                        setSubscriptionMode(Roster.SubscriptionMode.manual);

                    fireRegistrationStateChanged(
                        getRegistrationState(),
                        RegistrationState.REGISTERED,
                        RegistrationStateChangeEvent.REASON_NOT_SPECIFIED, null);
                }
                else
                {
                    fireRegistrationStateChanged(
                        getRegistrationState()
                        , RegistrationState.UNREGISTERED
                        , RegistrationStateChangeEvent.REASON_NOT_SPECIFIED
                        , null);
                }

            }
            catch (NumberFormatException ex)
            {
                throw new OperationFailedException("Wrong port",
                    OperationFailedException.INVALID_ACCOUNT_PROPERTIES, ex);
            }
        }

        // we setup supported features
        // List of features that smack already supports:
        // http://jabber.org/protocol/xhtml-im
        // http://jabber.org/protocol/muc
        // http://jabber.org/protocol/commands
        // http://jabber.org/protocol/chatstates
        // http://jabber.org/protocol/si/profile/file-transfer
        // http://jabber.org/protocol/si
        // http://jabber.org/protocol/bytestreams
        // http://jabber.org/protocol/ibb
        if (getRegistrationState() == RegistrationState.REGISTERED)
        {
            discoveryManager = ServiceDiscoveryManager.
                    getInstanceFor(connection);

            ServiceDiscoveryManager.setIdentityName("sip-comm");
            ServiceDiscoveryManager.setIdentityType("pc");
            Iterator<String> it = supportedFeatures.iterator();

            // Remove features supported by smack, but not supported in
            // SIP Communicator.
            discoveryManager.removeFeature(
                "http://jabber.org/protocol/commands");

            // Add features the SIP Communicator supports in plus of smack.
            while (it.hasNext())
            {
                String feature = it.next();

                if (!discoveryManager.includesFeature(feature))
                    discoveryManager.addFeature(feature);
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
    private void unregister(boolean fireEvent)
    {
        RegistrationState currRegState = getRegistrationState();

        if(connection != null && connection.isConnected())
            connection.disconnect();

        if(fireEvent)
        {
            fireRegistrationStateChanged(
                currRegState,
                RegistrationState.UNREGISTERED,
                RegistrationStateChangeEvent.REASON_USER_REQUEST, null);
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

            String protocolIconPath =
                accountID
                    .getAccountPropertyString(ProtocolProviderFactory.PROTOCOL_ICON_PATH);
            if (protocolIconPath == null)
            {
                protocolIconPath = "resources/images/protocol/jabber";
            }

            jabberIcon = new ProtocolIconJabberImpl(protocolIconPath);

            jabberStatusEnum
                = JabberStatusEnum.getJabberStatusEnum(protocolIconPath);

            //this feature is mandatory to be compliant with Service Discovery
            supportedFeatures.add("http://jabber.org/protocol/disco#info");

            String keepAliveStrValue =
                accountID.getAccountPropertyString("SEND_KEEP_ALIVE");

            String resourcePriority =
                accountID
                    .getAccountPropertyString(ProtocolProviderFactory.RESOURCE_PRIORITY);

            //initialize the presence operationset
            OperationSetPersistentPresenceJabberImpl persistentPresence =
                new OperationSetPersistentPresenceJabberImpl(this);

            if(resourcePriority != null)
            {
                persistentPresence.setResourcePriority(Integer
                    .parseInt(resourcePriority));
                // TODO : is this resource priority related to xep-0168
                // (Resource Application Priority) ?
                // see http://www.xmpp.org/extensions/xep-0168.html
                // If the answer is no, comment the following lines please
                supportedFeatures.add(
                        "http://www.xmpp.org/extensions/xep-0168.html#ns");
            }

            supportedOperationSets.put(
                OperationSetPersistentPresence.class.getName(),
                persistentPresence);
            // TODO: add the feature, if any, corresponding to persistent
            // presence, if someone knows
            // supportedFeatures.add(_PRESENCE_);

            //register it once again for those that simply need presence
            supportedOperationSets.put( OperationSetPresence.class.getName(),
                                        persistentPresence);

            //initialize the IM operation set
            OperationSetBasicInstantMessagingJabberImpl basicInstantMessaging =
                new OperationSetBasicInstantMessagingJabberImpl(this);

            if (keepAliveStrValue != null)
                basicInstantMessaging.setKeepAliveEnabled(Boolean
                    .parseBoolean(keepAliveStrValue));

            supportedOperationSets.put(
                OperationSetBasicInstantMessaging.class.getName(),
                basicInstantMessaging);

            // The http://jabber.org/protocol/xhtml-im feature is included
            // already in smack.

            //initialize the Whiteboard operation set
            OperationSetWhiteboardingJabberImpl whiteboard =
                new OperationSetWhiteboardingJabberImpl (this);

            supportedOperationSets.put (
                OperationSetWhiteboarding.class.getName(), whiteboard);

            //initialize the typing notifications operation set
            OperationSetTypingNotifications typingNotifications =
                new OperationSetTypingNotificationsJabberImpl(this);

            supportedOperationSets.put(
                OperationSetTypingNotifications.class.getName(),
                typingNotifications);

            // The http://jabber.org/protocol/chatstates feature implemented in
            // OperationSetTypingNotifications is included already in smack.

            //initialize the multi user chat operation set
            OperationSetMultiUserChat multiUserChat =
                new OperationSetMultiUserChatJabberImpl(this);

            supportedOperationSets.put(
                OperationSetMultiUserChat.class.getName(),
                multiUserChat);

            InfoRetreiver infoRetreiver = new InfoRetreiver(this, screenname);

            OperationSetServerStoredContactInfo contactInfo =
                new OperationSetServerStoredContactInfoJabberImpl(infoRetreiver);

            supportedOperationSets.put(
                OperationSetServerStoredContactInfo.class.getName(),
                contactInfo);

            OperationSetServerStoredAccountInfo accountInfo =
                new OperationSetServerStoredAccountInfoJabberImpl(
                        this, infoRetreiver, screenname);

            supportedOperationSets.put(
                OperationSetServerStoredAccountInfo.class.getName(),
                accountInfo);

            // initialize the file transfer operation set
            OperationSetFileTransfer fileTransfer
                = new OperationSetFileTransferJabberImpl(this);

            supportedOperationSets.put(
                OperationSetFileTransfer.class.getName(),
                fileTransfer);

            OperationSetInstantMessageTransform messageTransform
                = new OperationSetInstantMessageTransformImpl();
            
            supportedOperationSets.put(
                OperationSetInstantMessageTransform.class.getName(),
                messageTransform);
            
            // Include features we're supporting in plus of the four that
            // included by smack itself:
            // http://jabber.org/protocol/si/profile/file-transfer
            // http://jabber.org/protocol/si
            // http://jabber.org/protocol/bytestreams
            // http://jabber.org/protocol/ibb
            supportedFeatures.add("urn:xmpp:thumbs:0");
            supportedFeatures.add("urn:xmpp:bob");

            // initialize the thumbnailed file factory operation set
            OperationSetThumbnailedFileFactory thumbnailFactory
                = new OperationSetThumbnailedFileFactoryImpl();

            supportedOperationSets.put(
                OperationSetThumbnailedFileFactory.class.getName(),
                thumbnailFactory);

            // TODO: this is the "main" feature to advertise when a client
            // support muc. We have to add some features for
            // specific functionality we support in muc.
            // see http://www.xmpp.org/extensions/xep-0045.html

            // The http://jabber.org/protocol/muc feature is already included in
            // smack.
            supportedFeatures.add("http://jabber.org/protocol/muc#rooms");
            supportedFeatures.add("http://jabber.org/protocol/muc#traffic");

            //check if we are supposed to start telephony

            //initialize the telephony opset
            String enableJingle = (String)JabberActivator
                .getConfigurationService().getProperty(PNAME_ENABLE_JINGLE);
            if( Boolean.getBoolean(enableJingle)
                && JabberActivator.getMediaService() != null)
            {
                OperationSetBasicTelephony opSetBasicTelephony
                    = new OperationSetBasicTelephonyJabberImpl(this);

                supportedOperationSets.put(
                    OperationSetBasicTelephony.class.getName(),
                    opSetBasicTelephony);

                // Add Jingle features to supported features.
                supportedFeatures.add("urn:xmpp:jingle:1");
                supportedFeatures.add("urn:xmpp:jingle:apps:rtp:1");
                supportedFeatures.add("urn:xmpp:jingle:apps:rtp:audio");

                //one day:
                //supportedFeatures.add("urn:xmpp:jingle:apps:rtp:video");
            }

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
            logger.trace("Killing the Jabber Protocol Provider.");

            //kill all active calls
            OperationSetBasicTelephonyJabberImpl telephony
                = (OperationSetBasicTelephonyJabberImpl)getOperationSet(
                    OperationSetBasicTelephony.class);
            if (telephony != null)
            {
                telephony.shutdown();
            }

            if(connection != null)
            {
                connection.disconnect();
                connection = null;
            }
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
                    fireRegistrationStateChanged(getRegistrationState(),
                        RegistrationState.CONNECTION_FAILED,
                        RegistrationStateChangeEvent.REASON_MULTIPLE_LOGINS,
                        "Connecting multiple times with the same resource");
                    return;
                }
            }

            if(!reconnecting)
                reregister(SecurityAuthority.CONNECTION_FAILED);
            else
                reconnecting = false;
        }

        /**
         * Implements <tt>reconnectingIn</tt> from <tt>ConnectionListener</tt>
         *
         * @param i delay in seconds for reconnection.
         */
        public void reconnectingIn(int i)
        {
            logger.info("reconnectingIn " + i);
        }

        /**
         * Implements <tt>reconnectingIn</tt> from <tt>ConnectionListener</tt>
         */
        public void reconnectionSuccessful()
        {
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
     * Checks if the given list of <tt>features</tt> is supported by the given
     * jabber id.
     * @param jid the jabber id for which to check
     * @param features the list of features to check for
     * @return <code>true</code> if the list of features is supported, otherwise
     * returns <code>false</code>
     */
    boolean isFeatureListSupported(String jid, String[] features)
    {
        boolean isFeatureListSupported = true;

        ServiceDiscoveryManager disco = ServiceDiscoveryManager
            .getInstanceFor(getConnection());
        try
        {
            DiscoverInfo featureInfo = disco.discoverInfo(jid);

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
            logger.debug("Failed to discover info.", e);
        }

        return isFeatureListSupported;
    }

    /**
     * Returns the full jabber id (jid) corresponding to the given contact.
     * @param contact the contact, for which we're looking for a jid
     * @return the jid
     */
    String getFullJid(Contact contact)
    {
        Roster roster = getConnection().getRoster();
        Presence presence = roster.getPresence(contact.getAddress());

        return presence.getFrom();
    }
}
