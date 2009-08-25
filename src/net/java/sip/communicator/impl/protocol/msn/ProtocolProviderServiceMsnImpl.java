/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.msn;

import java.net.*;
import java.nio.channels.*;

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
    extends AbstractProtocolProviderService
{
    private static final Logger logger =
        Logger.getLogger(ProtocolProviderServiceMsnImpl.class);

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

        connectAndLogin(authority, SecurityAuthority.AUTHENTICATION_REQUIRED);
    }

    /**
     * Reconnects if fails fire connection failed.
     * @param reasonCode the appropriate <tt>SecurityAuthority</tt> reasonCode,
     * which would specify the reason for which we're re-calling the login.
     */
    void reconnect(int reasonCode)
    {
        try
        {
            connectAndLogin(authority, reasonCode);
        }
        catch (OperationFailedException ex)
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
     * @throws  OperationFailedException if login parameters
     *          as server port are not correct
     */
    private void connectAndLogin(SecurityAuthority authority, int reasonCode)
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
                credentials = authority.obtainCredentials(
                        ProtocolNames.MSN,
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

            supportedOperationSets.put(OperationSetInstantMessageTransform.class.getName(), 
                new OperationSetInstantMessageTransformMsnImpl());
            
            //initialize the presence operationset
            persistentPresence = new OperationSetPersistentPresenceMsnImpl(this);
            
            supportedOperationSets.put(
                OperationSetPersistentPresence.class.getName(),
                persistentPresence);

            //register it once again for those that simply need presence
            supportedOperationSets.put( OperationSetPresence.class.getName(),
                                        persistentPresence);

            // initialize the multi user chat operation set
           OperationSetMultiUserChat multiUserChat = new OperationSetMultiUserChatMsnImpl(
                   this);

           supportedOperationSets.put(OperationSetMultiUserChat.class
                   .getName(), multiUserChat);

           // initialize the IM operation set
           OperationSetBasicInstantMessagingMsnImpl basicInstantMessaging = new OperationSetBasicInstantMessagingMsnImpl(
                   this);

           supportedOperationSets.put(OperationSetBasicInstantMessaging.class
                   .getName(), basicInstantMessaging);

            //initialize the typing notifications operation set
            typingNotifications =
                new OperationSetTypingNotificationsMsnImpl(this);

            supportedOperationSets.put(
                OperationSetTypingNotifications.class.getName(),
                typingNotifications);

            OperationSetFileTransferMsnImpl fileTransferOpSet =
                new OperationSetFileTransferMsnImpl(this);
            supportedOperationSets.put(
                OperationSetFileTransfer.class.getName(),
                fileTransferOpSet);

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
            if (messenger != null)
            {
                messenger.logout();
                messenger = null;
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
    public void fireRegistrationStateChanged( RegistrationState oldState,
                                               RegistrationState newState,
                                               int               reasonCode,
                                               String            reason)
    {
        if(newState.equals(RegistrationState.UNREGISTERED) ||
            newState.equals(RegistrationState.CONNECTION_FAILED))
            messenger = null;

        super.fireRegistrationStateChanged(oldState, newState, reasonCode, reason);
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

        public void exceptionCaught(MsnMessenger msnMessenger,
                                    Throwable throwable)
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

                // We try to reconnect and ask user to retype password.
                reconnect(SecurityAuthority.WRONG_PASSWORD);
            }
            else if(throwable instanceof SocketException)
            {
                // in case of SocketException just fire event and not trigger
                // unregister it will cause SocketException again and will loop
                fireRegistrationStateChanged(
                    getRegistrationState(),
                    RegistrationState.CONNECTION_FAILED,
                    RegistrationStateChangeEvent.REASON_INTERNAL_ERROR,
                    null);
            }
            else if(throwable instanceof UnknownHostException)
            {
                unregister(false);

                fireRegistrationStateChanged(
                    getRegistrationState(),
                    RegistrationState.CONNECTION_FAILED,
                    RegistrationStateChangeEvent.REASON_SERVER_NOT_FOUND,
                    "A network error occured. Could not connect to server.");
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

                                // We try to reconnect and ask user to retype
                                // password.
                                reconnect(SecurityAuthority.WRONG_PASSWORD);
                            }
                            break;
                    }

                    return;
                }

                logger.error("Error in Msn lib ", throwable);

//                We don't want to disconnect on any error, that's why we're
//                commenting the following lines for now.
//
//                if(isRegistered())
//                {
//                    unregister(false);
//                    fireRegistrationStateChanged(
//                        getRegistrationState(),
//                        RegistrationState.UNREGISTERED,
//                        RegistrationStateChangeEvent.REASON_NOT_SPECIFIED, null);
//                }
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
