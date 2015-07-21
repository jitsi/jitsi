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
package net.java.sip.communicator.impl.protocol.yahoo;

import java.io.*;

import net.java.sip.communicator.service.dns.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;
import ymsg.network.*;
import ymsg.network.event.*;

/**
 * An implementation of the protocol provider service over the Yahoo protocol
 *
 * @author Damian Minkov
 */
public class ProtocolProviderServiceYahooImpl
    extends AbstractProtocolProviderService
{
    /**
     * This class logger.
     */
    private static final Logger logger =
        Logger.getLogger(ProtocolProviderServiceYahooImpl.class);

    /**
     * The current yahoo session.
     */
    private YahooSession yahooSession = null;

    /**
     * indicates whether or not the provider is initialized and ready for use.
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
     * The persistent presence operations set.
     */
    private OperationSetPersistentPresenceYahooImpl persistentPresence = null;

    /**
     * Typing notifications operations set.
     */
    private OperationSetTypingNotificationsYahooImpl typingNotifications = null;

    /**
     * The logo corresponding to the msn protocol.
     */
    private ProtocolIconYahooImpl yahooIcon
        = new ProtocolIconYahooImpl();

    /**
     * The connection listener.
     */
    private YahooConnectionListener connectionListener = null;

    /**
     * Returns the state of the registration of this protocol provider
     * @return the <tt>RegistrationState</tt> that this provider is
     * currently in or null in case it is in a unknown state.
     */
    public RegistrationState getRegistrationState()
    {
        if(yahooSession != null &&
            yahooSession.getSessionStatus() == StatusConstants.MESSAGING)
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

        connectAndLogin(authority, SecurityAuthority.AUTHENTICATION_REQUIRED);
    }

    /**
     * Connects and logins to the server
     * @param authority SecurityAuthority
     * @param authReasonCode the authentication reason code, which should
     * indicate why are making an authentication request
     * @throws  OperationFailedException if login parameters
     *          as server port are not correct
     */
    private void connectAndLogin(   SecurityAuthority authority,
                                    int authReasonCode)
        throws OperationFailedException
    {
        synchronized(initializationLock)
        {
            //verify whether a password has already been stored for this account
            String password = YahooActivator.
                getProtocolProviderFactory().loadPassword(getAccountID());

            // If the password hasn't been saved or the reason is one of those
            // listed below we need to ask the user for credentials again.
            if (password == null
                || authReasonCode == SecurityAuthority.WRONG_PASSWORD
                || authReasonCode == SecurityAuthority.WRONG_USERNAME)
            {
                //create a default credentials object
                UserCredentials credentials = new UserCredentials();
                credentials.setUserName(getAccountID().getUserID());

                //request a password from the user
                credentials = authority.obtainCredentials(
                    getAccountID().getDisplayName(),
                    credentials,
                    authReasonCode);

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
                    YahooActivator.getProtocolProviderFactory()
                        .storePassword(getAccountID(), password);
                }
            }

            yahooSession = new YahooSession();
            connectionListener = new YahooConnectionListener();
            yahooSession.addSessionListener(connectionListener);

            try
            {
                fireRegistrationStateChanged(
                        getRegistrationState(),
                        RegistrationState.REGISTERING,
                        RegistrationStateChangeEvent.REASON_NOT_SPECIFIED, null);

                yahooSession.login(getAccountID().getUserID(), password);

                if(yahooSession.getSessionStatus()==StatusConstants.MESSAGING)
                {
                    persistentPresence.fireProviderStatusChangeEvent(
                        persistentPresence.getPresenceStatus(),
                        persistentPresence.yahooStatusToPresenceStatus(
                            yahooSession.getStatus()));

                    fireRegistrationStateChanged(
                        getRegistrationState(),
                        RegistrationState.REGISTERED,
                        RegistrationStateChangeEvent.REASON_NOT_SPECIFIED, null);
                }
                else
                {
                    fireRegistrationStateChanged(
                        getRegistrationState(),
                        RegistrationState.UNREGISTERED,
                        RegistrationStateChangeEvent.REASON_NOT_SPECIFIED, null);
                }
            }
            catch (LoginRefusedException ex)
            {
                if(ex.getStatus() == StatusConstants.STATUS_BADUSERNAME)
                {
                    fireRegistrationStateChanged(
                        getRegistrationState(),
                        RegistrationState.AUTHENTICATION_FAILED,
                        RegistrationStateChangeEvent.REASON_NON_EXISTING_USER_ID,
                        null);

                    reregister(SecurityAuthority.WRONG_USERNAME);
                }
                else if(ex.getStatus() == StatusConstants.STATUS_BAD)
                {
                    YahooActivator.getProtocolProviderFactory()
                        .storePassword(getAccountID(), null);

                    fireRegistrationStateChanged(
                        getRegistrationState(),
                        RegistrationState.AUTHENTICATION_FAILED,
                        RegistrationStateChangeEvent.REASON_AUTHENTICATION_FAILED,
                        null);

                    // Try to re-register and ask the user to retype the password.
                    reregister(SecurityAuthority.WRONG_PASSWORD);
                }
                else if(ex.getStatus() == StatusConstants.STATUS_LOCKED)
                {
                    fireRegistrationStateChanged(
                        getRegistrationState(),
                        RegistrationState.AUTHENTICATION_FAILED,
                        RegistrationStateChangeEvent.REASON_RECONNECTION_RATE_LIMIT_EXCEEDED,
                        null);
                }
            }
            catch (IOException ex)
            {
                fireRegistrationStateChanged(
                    getRegistrationState(),
                    RegistrationState.CONNECTION_FAILED,
                    RegistrationStateChangeEvent.REASON_NOT_SPECIFIED, null);
            }
            catch (DnssecRuntimeException ex)
            {
                fireRegistrationStateChanged(
                    getRegistrationState(),
                    RegistrationState.UNREGISTERED,
                    RegistrationStateChangeEvent.REASON_USER_REQUEST, null);
            }
        }
    }

    /**
     * Reconnects if fails fire connection failed.
     * @param reasonCode the appropriate <tt>SecurityAuthority</tt> reasonCode,
     * which would specify the reason for which we're re-calling the login.
     */
    void reregister(int reasonCode)
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
     * Ends the registration of this protocol provider with the service.
     */
    public void unregister()
    {
        unregisterInternal(true);
    }

    /**
     * Unregister and fire the event if requested
     * @param fireEvent boolean
     */
    void unregisterInternal(boolean fireEvent)
    {
        RegistrationState currRegState = getRegistrationState();

        if(fireEvent)
            fireRegistrationStateChanged(
                currRegState,
                RegistrationState.UNREGISTERING,
                RegistrationStateChangeEvent.REASON_USER_REQUEST,
                null);

        try
        {
            if(connectionListener != null && yahooSession != null)
            {
                yahooSession.removeSessionListener(connectionListener);
                connectionListener = null;
            }

            if((yahooSession != null)
                    && (yahooSession.getSessionStatus() == StatusConstants.MESSAGING))
                yahooSession.logout();
        }
        catch(Exception ex)
        {
            logger.error("Cannot logout! ", ex);
        }

        yahooSession = null;

        if(fireEvent)
            fireRegistrationStateChanged(
                currRegState,
                RegistrationState.UNREGISTERED,
                RegistrationStateChangeEvent.REASON_USER_REQUEST,
                null);
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
     * Returns the short name of the protocol that the implementation of this
     * provider is based upon (like SIP, Msn, ICQ/AIM, or others for
     * example).
     *
     * @return a String containing the short name of the protocol this
     *   service is taking care of.
     */
    public String getProtocolName()
    {
        return ProtocolNames.YAHOO;
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

            addSupportedOperationSet(
                OperationSetInstantMessageTransform.class,
                new OperationSetInstantMessageTransformImpl());

            //initialize the presence operationset
            persistentPresence
                = new OperationSetPersistentPresenceYahooImpl(this);
            addSupportedOperationSet(
                OperationSetPersistentPresence.class,
                persistentPresence);
            //register it once again for those that simply need presence
            addSupportedOperationSet(
                OperationSetPresence.class,
                persistentPresence);

            //initialize the IM operation set
            addSupportedOperationSet(
                OperationSetBasicInstantMessaging.class,
                new OperationSetBasicInstantMessagingYahooImpl(this));

            //initialize the multi user chat operation set
            addSupportedOperationSet(
                OperationSetAdHocMultiUserChat.class,
                new OperationSetAdHocMultiUserChatYahooImpl(this));

            //initialize the typing notifications operation set
            typingNotifications
                = new OperationSetTypingNotificationsYahooImpl(this);
            addSupportedOperationSet(
                OperationSetTypingNotifications.class,
                typingNotifications);

            addSupportedOperationSet(
                OperationSetFileTransfer.class,
                new OperationSetFileTransferYahooImpl(this));

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
            unregisterInternal(false);
            yahooSession = null;
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
     * Returns the Yahoo<tt>Session</tt>opened by this provider
     * @return a reference to the <tt>Session</tt> last opened by this
     * provider.
     */
    YahooSession getYahooSession()
    {
        return yahooSession;
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
    @Override
    public void fireRegistrationStateChanged( RegistrationState oldState,
                                               RegistrationState newState,
                                               int               reasonCode,
                                               String            reason)
    {
        if(newState.equals(RegistrationState.UNREGISTERED))
        {
            unregisterInternal(false);
            yahooSession = null;
        }

        super.fireRegistrationStateChanged(oldState, newState, reasonCode, reason);
    }

    /**
     * Listens when we are logged in the server
     * or incoming exception in the lib impl.
     */
    private class YahooConnectionListener
        extends SessionAdapter
    {
        /**
         * Yahoo has logged us off the system, or the connection was lost
         *
         * @param ev the event
         */
        @Override
        public void connectionClosed(SessionEvent ev)
        {
            if(isRegistered())
                fireRegistrationStateChanged(
                    getRegistrationState(),
                    RegistrationState.CONNECTION_FAILED,
                    RegistrationStateChangeEvent.REASON_NOT_SPECIFIED, null);
        }

        /**
         * Some exception has occurred in stack.
         * @param ev
         */
        @Override
        public void inputExceptionThrown(SessionExceptionEvent ev)
        {
            if(ev.getException() instanceof YMSG9BadFormatException)
            {
                logger.error("Yahoo protocol exception occured exception",
                    ev.getException());
                logger.error("Yahoo protocol exception occured exception cause",
                    ev.getException().getCause());
            }
            else
                    logger.error(
                        "Yahoo protocol exception occured", ev.getException());

            unregisterInternal(false);
            if(isRegistered())
                fireRegistrationStateChanged(
                    getRegistrationState(),
                    RegistrationState.UNREGISTERED,
                    RegistrationStateChangeEvent.REASON_INTERNAL_ERROR, null);
        }
    }

    /**
     * Returns the yahoo protocol icon.
     * @return the yahoo protocol icon
     */
    public ProtocolIcon getProtocolIcon()
    {
        return yahooIcon;
    }
}
