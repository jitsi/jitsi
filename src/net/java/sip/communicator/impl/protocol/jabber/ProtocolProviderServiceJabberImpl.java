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
import org.jivesoftware.smack.util.*;
import org.jivesoftware.smack.filter.*;
import org.jivesoftware.smack.packet.*;

import org.jivesoftware.smackx.*;
import org.jivesoftware.smackx.packet.*;

/**
 * An implementation of the protocol provider service over the Jabber protocol
 *
 * @author Damian Minkov
 * @author Symphorien Wanko
 */
public class ProtocolProviderServiceJabberImpl
    implements ProtocolProviderService
{
    /**
     * Logger of this class
     */
    private static final Logger logger =
        Logger.getLogger(ProtocolProviderServiceJabberImpl.class);

    /**
     * The hashtable with the operation sets that we support locally.
     */
    private Hashtable supportedOperationSets = new Hashtable();

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
    private Object initializationLock = new Object();

    /**
     * A list of all listeners registered for
     * <tt>RegistrationStateChangeEvent</tt>s.
     */
    private List registrationListeners = new ArrayList();

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
    private ProtocolIconJabberImpl jabberIcon
        = new ProtocolIconJabberImpl();

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
    private List supportedFeatures = new ArrayList();

    /**
     * The <tt>ServiceDiscoveryManager</tt> is responsible for advertising
     * <tt>supportedFeatures</tt> when asked by a remote client. It can also
     * be used to query remote clients for supported features.
     */
    private ServiceDiscoveryManager discoveryManager = null;

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
            connectAndLogin(authority);
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
                    reason
                        = RegistrationStateChangeEvent.REASON_AUTHENTICATION_FAILED;
                    regState = RegistrationState.AUTHENTICATION_FAILED;
                }

            fireRegistrationStateChanged(
                getRegistrationState(), regState, reason, null);
        }
    }

    /**
     * Connects and logins again to the server
     */
    void reregister()
    {
        try
        {
            logger.trace("Trying to reregister us!");

            // sets this if any is tring to use us through registration
            // to know we are not registered
            this.unregister(false);

            this.reconnecting = true;

            connectAndLogin(authority);
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
     * @throws XMPPException if we cannot connect to the server - network problem
     * @throws  OperationFailedException if login parameters
     *          as server port are not correct
     */
    private void connectAndLogin(SecurityAuthority authority)
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
                credentials = authority.obtainCredentials(ProtocolNames.JABBER
                    , credentials);

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

                String serverAddress = (String)getAccountID().
                    getAccountProperties().get(
                            ProtocolProviderFactory.SERVER_ADDRESS);

                String serverPort = (String)getAccountID().
                    getAccountProperties().get(
                            ProtocolProviderFactory.SERVER_PORT);

				String accountResource = (String)getAccountID().
                    getAccountProperties().get(
                            ProtocolProviderFactory.RESOURCE);

                // check to see is there SRV records for this server domain
                try
                {
                    String hosts[] =
                        NetworkUtils.getSRVRecords(
                            "_xmpp-client._tcp." + serviceName);

                    if(hosts != null && hosts.length > 0)
                    {
                        logger.trace("Will set server address from SRV records "
                           + hosts[0]);
                        serverAddress = hosts[0];
                    }
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

                if(accountResource == null || accountResource == "")
                    accountResource = "sip-comm";
                
                connection.login(userID, password, accountResource);

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
        if (getRegistrationState() == RegistrationState.REGISTERED)
        {
            discoveryManager = ServiceDiscoveryManager.
                    getInstanceFor(connection);
            discoveryManager.setIdentityName("sip-comm");
            discoveryManager.setIdentityType("registered");
            Iterator it = supportedFeatures.iterator();
            while (it.hasNext())
            {
                discoveryManager.addFeature((String) it.next());
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

        if(connection != null)
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
     * Indicates whether or not this provider is signed on the service
     * @return true if the provider is currently signed on (and hence online)
     * and false otherwise.
     */
    public boolean isRegistered()
    {
        return getRegistrationState().equals(RegistrationState.REGISTERED);
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
     * Returns an array containing all operation sets supported by the
     * current implementation.
     *
     * @return an array of OperationSet-s supported by this protocol
     *   provider implementation.
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
     * underlying implementation supports it or null otherwise.
     */
    public OperationSet getOperationSet(Class opsetClass)
    {
        return (OperationSet)getSupportedOperationSets()
            .get(opsetClass.getName());
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

            //this feature is mandatory to be compliant with Service Discovery
            supportedFeatures.add("http://jabber.org/protocol/disco#info");


            String keepAliveStrValue = (String)accountID.getAccountProperties().
                                get("SEND_KEEP_ALIVE");

            String resourcePriority = (String)accountID.getAccountProperties().
                get(ProtocolProviderFactory.RESOURCE_PRIORITY);

            //initialize the presence operationset
            OperationSetPersistentPresenceJabberImpl persistentPresence =
                new OperationSetPersistentPresenceJabberImpl(this);

            if(resourcePriority != null)
            {
                persistentPresence.setResourcePriority(
                    new Integer(resourcePriority).intValue());
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

            if(keepAliveStrValue != null)
                basicInstantMessaging.
                    setKeepAliveEnabled(
                        new Boolean(keepAliveStrValue).booleanValue());

            supportedOperationSets.put(
                OperationSetBasicInstantMessaging.class.getName(),
                basicInstantMessaging);

            // TODO: add the feature, if any, corresponding to IM if someone
            // knows
            // supportedFeatures.add(_IM_);

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
            supportedFeatures.add("http://jabber.org/protocol/chatstates");

            //initialize the multi user chat operation set
            OperationSetMultiUserChat multiUserChat =
                new OperationSetMultiUserChatJabberImpl(this);

            supportedOperationSets.put(
                OperationSetMultiUserChat.class.getName(),
                multiUserChat);
            
            OperationSetServerStoredContactInfo contactInfo =
                new OperationSetServerStoredContactInfoJabberImpl(this);

            supportedOperationSets.put(
                OperationSetServerStoredContactInfo.class.getName(),
                contactInfo);

            // TODO: this is the "main" feature to advertise when a client
            // support muc. We have to add some features for
            // specific functionnality we support in muc.
            // see http://www.xmpp.org/extensions/xep-0045.html
            supportedFeatures.add("http://jabber.org/protocol/muc");

            //initialize the telephony opset
            if(JabberActivator.getMediaService() != null)
            {
                OperationSetBasicTelephony opSetBasicTelephony
                    = new OperationSetBasicTelephonyJabberImpl(this);

                supportedOperationSets.put(
                    OperationSetBasicTelephony.class.getName(), 
                    opSetBasicTelephony);

                supportedFeatures.add(
                    "http://www.xmpp.org/extensions/xep-0166.html#ns");
                supportedFeatures.add(
                    "http://www.xmpp.org/extensions/xep-0167.html#ns");
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
     * Removes the specified registration state change listener so that it does
     * not receive any further notifications upon changes of the
     * RegistrationState of this provider.
     *
     * @param listener the listener to register for
     * <tt>RegistrationStateChangeEvent</tt>s.
     */
    public void removeRegistrationStateChangeListener(
        RegistrationStateChangeListener listener)
    {
        synchronized(registrationListeners)
        {
            registrationListeners.remove(listener);
        }
    }

    /**
     * Registers the specified listener with this provider so that it would
     * receive notifications on changes of its state or other properties such
     * as its local address and display name.
     *
     * @param listener the listener to register.
     */
    public void addRegistrationStateChangeListener(
        RegistrationStateChangeListener listener)
    {
        synchronized(registrationListeners)
        {
            if (!registrationListeners.contains(listener))
                registrationListeners.add(listener);
        }
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
     * Creates a RegistrationStateChange event corresponding to the specified
     * old and new states and notifies all currently registered listeners.
     *
     * @param oldState the state that the provider had before the change
     * occurred
     * @param newState the state that the provider is currently in.
     * @param reasonCode a value corresponding to one of the REASON_XXX fields
     * of the RegistrationStateChangeEvent class, indicating the reason for
     * this state transition.
     * @param reason a String further explaining the reason code or null if
     * no such explanation is necessary.
     */
    void fireRegistrationStateChanged( RegistrationState oldState,
                                               RegistrationState newState,
                                               int               reasonCode,
                                               String            reason)
    {
        RegistrationStateChangeEvent event =
            new RegistrationStateChangeEvent(
                            this, oldState, newState, reasonCode, reason);

        logger.debug("Dispatching " + event + " to "
                     + registrationListeners.size()+ " listeners.");

        Iterator listeners = null;
        synchronized (registrationListeners)
        {
            listeners = new ArrayList(registrationListeners).iterator();
        }

        while (listeners.hasNext())
        {
            RegistrationStateChangeListener listener
                = (RegistrationStateChangeListener) listeners.next();

            listener.registrationStateChanged(event);
        }

        logger.trace("Done.");
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
                    getSupportedOperationSets()
                        .get(OperationSetPersistentPresence.class.getName());

            opSetPersPresence.fireProviderPresenceStatusChangeEvent(
                opSetPersPresence.getPresenceStatus(),
                JabberStatusEnum.OFFLINE);
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

            if(!reconnecting)
                reregister();
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
}
