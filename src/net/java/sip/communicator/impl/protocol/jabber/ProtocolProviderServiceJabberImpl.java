/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber;

import java.net.*;
import java.util.*;

import org.jivesoftware.smack.*;
import org.jivesoftware.smack.util.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * An implementation of the protocol provider service over the Jabber protocol
 *
 * @author Damian Minkov
 */
public class ProtocolProviderServiceJabberImpl
    implements ProtocolProviderService
{
    private static final Logger logger =
        Logger.getLogger(ProtocolProviderServiceJabberImpl.class);

    /**
     * The hashtable with the operation sets that we support locally.
     */
    private Hashtable supportedOperationSets = new Hashtable();

    private XMPPConnection connection = null;

    private RegistrationState currentConnectionState =
        RegistrationState.UNREGISTERED;

    /**
     * indicates whether or not the provider is initialized and ready for use.
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
     * Returns the state of the registration of this protocol provider
     * @return the <tt>RegistrationState</tt> that this provider is
     * currently in or null in case it is in a unknown state.
     */
    public RegistrationState getRegistrationState()
    {
        return currentConnectionState;
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
     */
    public void register(final SecurityAuthority authority)
    {
        if(authority == null)
            throw new IllegalArgumentException(
                "The register method needs a valid non-null authority impl "
                + " in order to be able and retrieve passwords.");

        synchronized(initializationLock)
        {
            new Thread()
            {
                public void run() {

                    //verify whether a password has already been stored for this account
                    String password = JabberActivator.
                        getProtocolProviderFactory()
                        .loadPassword(getAccountID());

                    //decode
                    if (password == null)
                    {
                        //create a default credentials object
                        UserCredentials credentials = new UserCredentials();
                        credentials.setUserName(getAccountID().getUserID());

                        //request a password from the user
                        credentials = authority.obtainCredentials(ProtocolNames.
                            JABBER
                            , credentials);

                        //extract the password the user passed us.
                        password = new String(credentials.getPassword());

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

                        connection = new XMPPConnection(
                                serverAddress,
                                Integer.parseInt(serverPort),
                                serviceName);

                        connection.addConnectionListener(
                            new JabberConnectionListener());

                        connection.login(userID, password, "sip-comm");

                        if(connection.isAuthenticated())
                        {
                            currentConnectionState = RegistrationState.REGISTERED;

                            connection.getRoster().
                                setSubscriptionMode(Roster.SUBSCRIPTION_ACCEPT_ALL);


                            fireRegistrationStateChanged(
                                RegistrationState.UNREGISTERED,
                                RegistrationState.REGISTERED,
                                RegistrationStateChangeEvent.REASON_NOT_SPECIFIED, null);
                        }
                    }
                    catch (XMPPException ex)
                    {
                        logger.error("Error registering", ex);

                        int reason =
                            RegistrationStateChangeEvent.REASON_NOT_SPECIFIED;

                        if(ex.getWrappedThrowable() instanceof UnknownHostException)
                            reason =
                                RegistrationStateChangeEvent.REASON_SERVER_NOT_FOUND;

                        fireRegistrationStateChanged(RegistrationState.UNREGISTERED,
                            RegistrationState.CONNECTION_FAILED, reason, null);
                    }
                }
            }.start();
        }
    }

    /**
     * Ends the registration of this protocol provider with the service.
     */
    public void unregister()
    {
        connection.close();
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
     * undelying implementation supports it or null otherwise.
     */
    public OperationSet getOperationSet(Class opsetClass)
    {
        return (OperationSet)getSupportedOperationSets()
            .get(opsetClass.getName());
    }

    /**
     * Initialized the service implementation, and puts it in a sate where it
     * could interoperate with other services. It is strongly recomended that
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

            //initialize the presence operationset
            OperationSetPersistentPresence persistentPresence =
                new OperationSetPersistentPresenceJabberImpl(this);

            supportedOperationSets.put(
                OperationSetPersistentPresence.class.getName(),
                persistentPresence);

            //register it once again for those that simply need presence
            supportedOperationSets.put( OperationSetPresence.class.getName(),
                                        persistentPresence);

            //initialize the IM operation set
            OperationSetBasicInstantMessaging basicInstantMessaging =
                new OperationSetBasicInstantMessagingJabberImpl(this);

            supportedOperationSets.put(
                OperationSetBasicInstantMessaging.class.getName(),
                basicInstantMessaging);

            //initialize the typing notifications operation set
            OperationSetTypingNotifications typingNotifications =
                new OperationSetTypingNotificationsJabberImpl(this);

            supportedOperationSets.put(
                OperationSetTypingNotifications.class.getName(),
                typingNotifications);

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
        synchronized(initializationLock){
            connection.close();
            connection = null;
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
        registrationListeners.remove(listener);
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
        registrationListeners.add(listener);
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

        for (int i = 0; i < registrationListeners.size(); i++)
        {
            RegistrationStateChangeListener listener
                = (RegistrationStateChangeListener)registrationListeners.get(i);
            listener.registrationStateChanged(event);
        }

        logger.trace("Done.");
    }

    private class JabberConnectionListener
        implements ConnectionListener
    {
        public void connectionClosed()
        {
            RegistrationState oldConnectionState = currentConnectionState;

            currentConnectionState = RegistrationState.UNREGISTERED;
            fireRegistrationStateChanged(
                    oldConnectionState,
                    currentConnectionState,
                    RegistrationStateChangeEvent.REASON_USER_REQUEST, null);
        }

        public void connectionClosedOnError(Exception exception)
        {
            RegistrationState oldConnectionState = currentConnectionState;

            currentConnectionState = RegistrationState.UNREGISTERED;
            fireRegistrationStateChanged(
                    oldConnectionState,
                    currentConnectionState,
                    RegistrationStateChangeEvent.REASON_INTERNAL_ERROR,
                    exception.getLocalizedMessage());
        }
    }
}
