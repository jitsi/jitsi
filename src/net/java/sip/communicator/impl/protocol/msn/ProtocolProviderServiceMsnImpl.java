/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
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
import net.java.sip.communicator.util.dns.*;
import net.sf.jml.*;
import net.sf.jml.event.*;
import net.sf.jml.exception.*;
import net.sf.jml.impl.*;

/**
 * An implementation of the protocol provider service over the Msn protocol
 *
 * @author Damian Minkov
 * @author Lubomir Marinov
 */
public class ProtocolProviderServiceMsnImpl
    extends AbstractProtocolProviderService
{
    /**
     * Logger of this class
     */
    private static final Logger logger
        = Logger.getLogger(ProtocolProviderServiceMsnImpl.class);

    /**
     * The lib messenger.
     */
    private MsnMessenger messenger = null;

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
     * Operation set for persistent presence.
     */
    private OperationSetPersistentPresenceMsnImpl persistentPresence = null;

    /**
     * Operation set for typing notifications.
     */
    private OperationSetTypingNotificationsMsnImpl typingNotifications = null;

    /**
     * The icon corresponding to the msn protocol.
     */
    private final ProtocolIconMsnImpl msnIcon = new ProtocolIconMsnImpl();

    /**
     * The indicator which determines whether
     * {@link MsnMessengerListener#logout(MsnMessenger)} has been received for
     * {@link #messenger} and it is thus an error to call
     * {@link MsnMessenger#logout()} on it.
     */
    private boolean logoutReceived = false;

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
                RegistrationStateChangeEvent.REASON_NOT_SPECIFIED,
                null);
        }
    }

    /**
     * Connects and logins to the server
     * @param authority SecurityAuthority
     * @param reasonCode 
     * @throws  OperationFailedException if login parameters
     *          as server port are not correct
     */
    private void connectAndLogin(SecurityAuthority authority, int reasonCode)
        throws OperationFailedException
    {
        synchronized(initializationLock)
        {
            //verify whether a password has already been stored for this account
            ProtocolProviderFactory protocolProviderFactory
                = MsnActivator.getProtocolProviderFactory();
            AccountID accountID = getAccountID();
            String password = protocolProviderFactory.loadPassword(accountID);

            //decode
            if (password == null)
            {
                //create a default credentials object
                UserCredentials credentials = new UserCredentials();
                credentials.setUserName(accountID.getUserID());

                //request a password from the user
                credentials
                    = authority
                        .obtainCredentials(
                            ProtocolNames.MSN,
                            credentials,
                            reasonCode);

                // in case user has canceled the login window
                if(credentials == null)
                {
                    fireRegistrationStateChanged(
                        getRegistrationState(),
                        RegistrationState.UNREGISTERED,
                        RegistrationStateChangeEvent.REASON_USER_REQUEST,
                        "");
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
                        RegistrationStateChangeEvent.REASON_USER_REQUEST,
                        "");
                    return;
                }
                password = new String(pass);

                if (credentials.isPasswordPersistent())
                    protocolProviderFactory.storePassword(accountID, password);
            }

            messenger
                = MsnMessengerFactory
                    .createMsnMessenger(accountID.getUserID(), password);

            /*
             * We've just created the messenger so we're sure we haven't
             * received a logout for it.
             */
            logoutReceived = false;
            messenger.addMessengerListener(new MsnConnectionListener());

            persistentPresence.setMessenger(messenger);
            typingNotifications.setMessenger(messenger);

            fireRegistrationStateChanged(
                getRegistrationState(),
                RegistrationState.REGISTERING,
                RegistrationStateChangeEvent.REASON_NOT_SPECIFIED,
                null);

            try
            {
                messenger.login();
            }
            catch (UnresolvedAddressException ex)
            {
                fireRegistrationStateChanged(
                    getRegistrationState(),
                    RegistrationState.CONNECTION_FAILED,
                    RegistrationStateChangeEvent.REASON_SERVER_NOT_FOUND,
                    null);
            }
            catch(DnssecRuntimeException ex)
            {
                fireRegistrationStateChanged(
                    getRegistrationState(),
                    RegistrationState.UNREGISTERED,
                    RegistrationStateChangeEvent.REASON_USER_REQUEST,
                    null);
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

    /*
     * (non-Javadoc)
     * 
     * @see net.java.sip.communicator.service.protocol.ProtocolProviderService#
     * isSignallingTransportSecure()
     */
    public boolean isSignalingTransportSecure()
    {
        return false;
    }

    /**
     * Returns the "transport" protocol of this instance used to carry the
     * control channel for the current protocol service.
     *
     * @return The "transport" protocol of this instance: TCP.
     */
    public TransportProtocol getTransportProtocol()
    {
        return TransportProtocol.TCP;
    }

    /**
     * Unregister and fire the event if requested
     * @param fireEvent boolean
     */
    void unregister(boolean fireEvent)
    {
        RegistrationState currRegState = getRegistrationState();

        if(fireEvent)
            fireRegistrationStateChanged(
                currRegState,
                RegistrationState.UNREGISTERING,
                RegistrationStateChangeEvent.REASON_USER_REQUEST,
                null);

        // The synchronization is for logoutReceived at least.
        synchronized (initializationLock)
        {
            if((messenger != null) && !logoutReceived)
                messenger.logout();

            persistentPresence.setMessenger(null);
            typingNotifications.setMessenger(null);
        }

        // if messenger is null we have already fired unregister
        if(fireEvent && messenger != null)
            fireRegistrationStateChanged(
                currRegState,
                RegistrationState.UNREGISTERED,
                RegistrationStateChangeEvent.REASON_USER_REQUEST,
                null);
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
    protected void initialize(String screenname, AccountID accountID)
    {
        synchronized(initializationLock)
        {
            this.accountID = accountID;

            addSupportedOperationSet(
                OperationSetInstantMessageTransform.class,
                new OperationSetInstantMessageTransformImpl());

            //initialize the presence operationset
            persistentPresence = new OperationSetPersistentPresenceMsnImpl(this);
            addSupportedOperationSet(
                OperationSetPersistentPresence.class,
                persistentPresence);
            //register it once again for those that simply need presence
            addSupportedOperationSet(
                OperationSetPresence.class,
                persistentPresence);

            //initialize AccountInfo
            OperationSetServerStoredAccountInfoMsnImpl accountInfo
                = new OperationSetServerStoredAccountInfoMsnImpl(
                    this, screenname);
            addSupportedOperationSet(
                    OperationSetServerStoredAccountInfo.class,
                    accountInfo);
            addSupportedOperationSet(
                    OperationSetAvatar.class,
                    new OperationSetAvatarMsnImpl(this, accountInfo));

            addSupportedOperationSet(
                OperationSetAdHocMultiUserChat.class,
                new OperationSetAdHocMultiUserChatMsnImpl(this));

            // initialize the IM operation set
            addSupportedOperationSet(
                OperationSetBasicInstantMessaging.class,
                new OperationSetBasicInstantMessagingMsnImpl(this));

            //initialize the typing notifications operation set
            typingNotifications
                = new OperationSetTypingNotificationsMsnImpl(this);
            addSupportedOperationSet(
                OperationSetTypingNotifications.class,
                typingNotifications);

            addSupportedOperationSet(
                OperationSetFileTransfer.class,
                new OperationSetFileTransferMsnImpl(this));
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
            unregister(false);
            messenger = null;
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
    public void fireRegistrationStateChanged(RegistrationState oldState,
                                             RegistrationState newState,
                                             int reasonCode,
                                             String reason)
    {
        if (newState.equals(RegistrationState.UNREGISTERED)
                || newState.equals(RegistrationState.CONNECTION_FAILED))
            messenger = null;

        super.fireRegistrationStateChanged(oldState, newState, reasonCode, reason);
    }

    /**
     * Listens when we are logged in or out from the server or incoming
     * exception in the lib impl.
     */
    private class MsnConnectionListener
        implements MsnMessengerListener
    {
        /**
         * Fired when login has completed.
         * @param msnMessenger
         */
        public void loginCompleted(MsnMessenger msnMessenger)
        {
            if (logger.isTraceEnabled())
                logger.trace("loginCompleted " + msnMessenger.getActualMsnProtocol());
            fireRegistrationStateChanged(
                getRegistrationState(),
                RegistrationState.REGISTERED,
                RegistrationStateChangeEvent.REASON_NOT_SPECIFIED,
                null);
        }

        /**
         * Fire when lib logs out.
         * @param msnMessenger
         */
        public void logout(MsnMessenger msnMessenger)
        {
            if (logger.isTraceEnabled())
                logger.trace("logout");

            // The synchronization is for logoutReceived at least.
            synchronized (initializationLock)
            {
                logoutReceived = true;
                unregister(true);
            }
        }

        /**
         * Fired when an exception has occurred.
         * @param msnMessenger
         * @param throwable
         */
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
                fireRegistrationStateChanged(
                    getRegistrationState(),
                    RegistrationState.CONNECTION_FAILED,
                    RegistrationStateChangeEvent.REASON_SERVER_NOT_FOUND,
                    "A network error occured. Could not connect to server.");
            }
            else if(throwable instanceof MsnProtocolException)
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
                            RegistrationStateChangeEvent.REASON_INTERNAL_ERROR,
                            null);
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
                            RegistrationStateChangeEvent
                                .REASON_AUTHENTICATION_FAILED,
                            null);

                        // We try to reconnect and ask user to retype
                        // password.
                        reconnect(SecurityAuthority.WRONG_PASSWORD);
                    }
                    break;
                }
            }
            else
            {
                logger.error("Error in Msn lib ", throwable);

                if(throwable instanceof LoginException)
                {
                    MsnActivator.getProtocolProviderFactory().
                        storePassword(getAccountID(), null);

                    fireRegistrationStateChanged(
                        getRegistrationState(),
                        RegistrationState.AUTHENTICATION_FAILED,
                        RegistrationStateChangeEvent
                            .REASON_AUTHENTICATION_FAILED,
                        null);
                    // We try to reconnect and ask user to retype
                    // password.
                    reconnect(SecurityAuthority.WRONG_PASSWORD);
                }

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
