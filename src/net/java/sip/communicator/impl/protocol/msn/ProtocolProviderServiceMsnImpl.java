
/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.msn;

import java.nio.channels.*;
import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;
import net.sf.jml.*;
import net.sf.jml.event.*;
import net.sf.jml.exception.*;
import net.sf.jml.impl.*;

/**
 * An implementation of the protocol provider service over the Msn protocol
 *
 * @author Damian Minkov
 */
public class ProtocolProviderServiceMsnImpl
    implements ProtocolProviderService
{
    private static final Logger logger =
        Logger.getLogger(ProtocolProviderServiceMsnImpl.class);

    /**
     * The hashtable with the operation sets that we support locally.
     */
    private Hashtable supportedOperationSets = new Hashtable();

    private MsnMessenger messenger = null;

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
     * Used when we need to re-register
     */
    private SecurityAuthority authority = null;

    private OperationSetPersistentPresenceMsnImpl persistentPresence = null;

    private OperationSetTypingNotificationsMsnImpl typingNotifications = null;

    /**
     * The icon corresponding to the msn protocol.
     */
    private ProtocolIconMsnImpl msnIcon
        = new ProtocolIconMsnImpl();
    
    /**
     * Returns the state of the registration of this protocol provider
     * @return the <tt>RegistrationState</tt> that this provider is
     * currently in or null in case it is in a unknown state.
     */
    public RegistrationState getRegistrationState()
    {
        if(messenger == null || messenger.getConnection() == null)
            return RegistrationState.UNREGISTERED;
        else
            return RegistrationState.REGISTERED;
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

        connectAndLogin(authority);
    }
    
    /**
     * Reconnects if fails fire connection failed.
     */
    void reconnect()
    {
        try
        {
            connectAndLogin(authority);
        } catch (OperationFailedException ex)
        {
            fireRegistrationStateChanged(
                getRegistrationState(),
                RegistrationState.CONNECTION_FAILED,
                RegistrationStateChangeEvent.REASON_NOT_SPECIFIED, null);
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
        throws OperationFailedException
    {
        synchronized(initializationLock)
        {
            //verify whether a password has already been stored for this account
            String password = MsnActivator.
                getProtocolProviderFactory().loadPassword(getAccountID());

            //decode
            if (password == null)
            {
                //create a default credentials object
                UserCredentials credentials = new UserCredentials();
                credentials.setUserName(getAccountID().getUserID());

                //request a password from the user
                credentials = authority.obtainCredentials(ProtocolNames.MSN
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
                    MsnActivator.getProtocolProviderFactory()
                        .storePassword(getAccountID(), password);
                }
            }


            messenger = MsnMessengerFactory.createMsnMessenger(
                getAccountID().getUserID(),
                password);

            messenger.addMessengerListener(new MsnConnectionListener());

            persistentPresence.setMessenger(messenger);
            typingNotifications.setMessenger(messenger);

            try
            {
                messenger.login();
            }
            catch (UnresolvedAddressException ex)
            {
                fireRegistrationStateChanged(
                    getRegistrationState(),
                    RegistrationState.CONNECTION_FAILED,
                    RegistrationStateChangeEvent.REASON_SERVER_NOT_FOUND, null);
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
        RegistrationState currRegState = getRegistrationState();

        if(messenger != null)
            messenger.logout();

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
     * provider is based upon (like SIP, Msn, ICQ/AIM, or others for
     * example).
     *
     * @return a String containing the short name of the protocol this
     *   service is taking care of.
     */
    public String getProtocolName()
    {
        return ProtocolNames.MSN;
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
            persistentPresence = new OperationSetPersistentPresenceMsnImpl(this);

            supportedOperationSets.put(
                OperationSetPersistentPresence.class.getName(),
                persistentPresence);

            //register it once again for those that simply need presence
            supportedOperationSets.put( OperationSetPresence.class.getName(),
                                        persistentPresence);

            //initialize the IM operation set
            OperationSetBasicInstantMessagingMsnImpl basicInstantMessaging =
                new OperationSetBasicInstantMessagingMsnImpl(this);

            supportedOperationSets.put(
                OperationSetBasicInstantMessaging.class.getName(),
                basicInstantMessaging);

            //initialize the typing notifications operation set
            typingNotifications =
                new OperationSetTypingNotificationsMsnImpl(this);

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
            messenger.logout();
            messenger = null;
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
    MsnMessenger getMessenger()
    {
        return messenger;
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

        if(newState.equals(RegistrationState.UNREGISTERED) ||
            newState.equals(RegistrationState.CONNECTION_FAILED))
            messenger = null;

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
     * Listens when we are logged in or out from the server
     * or incoming exception in the lib impl.
     */
    private class MsnConnectionListener
        implements MsnMessengerListener
    {
        public void loginCompleted(MsnMessenger msnMessenger)
        {
            logger.trace("loginCompleted " + msnMessenger.getActualMsnProtocol());
            fireRegistrationStateChanged(
                getRegistrationState(),
                RegistrationState.REGISTERED,
                RegistrationStateChangeEvent.REASON_NOT_SPECIFIED, null);
        }

        public void logout(MsnMessenger msnMessenger)
        {
            logger.trace("logout");
            unregister(true);
//            if(isRegistered())
//                fireRegistrationStateChanged(
//                    getRegistrationState(),
//                    RegistrationState.UNREGISTERED,
//                    RegistrationStateChangeEvent.REASON_NOT_SPECIFIED, null);

        }

        public void exceptionCaught(MsnMessenger msnMessenger, Throwable throwable)
        {
            if(throwable instanceof IncorrectPasswordException)
            {
                unregister(false);
                MsnActivator.getProtocolProviderFactory().
                    storePassword(getAccountID(), null);
                fireRegistrationStateChanged(
                    getRegistrationState(),
                    RegistrationState.AUTHENTICATION_FAILED,
                    RegistrationStateChangeEvent.REASON_AUTHENTICATION_FAILED,
                    "Incorrect Password");
            }
            else
            {
                if(throwable instanceof MsnProtocolException)
                {
                    MsnProtocolException exception =
                        (MsnProtocolException)throwable;


                    logger.error("Error in Msn lib ", exception);

                    switch(exception.getErrorCode())
                    {
                        case 500:
                        case 540:
                        case 601:
                            if(isRegistered())
                            {
                                unregister(false);
                                fireRegistrationStateChanged(
                                    getRegistrationState(),
                                    RegistrationState.UNREGISTERED,
                                    RegistrationStateChangeEvent.
                                    REASON_INTERNAL_ERROR, null);
                            }
                            break;
                        case 911:
                            if(isRegistered())
                            {
                                unregister(false);
                                MsnActivator.getProtocolProviderFactory().
                                    storePassword(getAccountID(), null);
                                fireRegistrationStateChanged(
                                    getRegistrationState(),
                                    RegistrationState.AUTHENTICATION_FAILED,
                                    RegistrationStateChangeEvent.
                                    REASON_AUTHENTICATION_FAILED, null);
                            }
                            break;
                    }

                    return;
                }

                logger.error("Error in Msn lib ", throwable);

                if(isRegistered())
                {
                    unregister(false);
                    fireRegistrationStateChanged(
                        getRegistrationState(),
                        RegistrationState.UNREGISTERED,
                        RegistrationStateChangeEvent.REASON_NOT_SPECIFIED, null);
                }
            }
        }
    }
    
    /**
     * Returns the msn protocol icon.
     * @return the msn protocol icon
     */
    public ProtocolIcon getProtocolIcon()
    {
        return msnIcon;
    }
}
