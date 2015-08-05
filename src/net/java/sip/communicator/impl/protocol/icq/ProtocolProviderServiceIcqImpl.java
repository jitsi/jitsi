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
package net.java.sip.communicator.impl.protocol.icq;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;
import net.kano.joscar.flap.*;
import net.kano.joscar.flapcmd.*;
import net.kano.joscar.net.*;
import net.kano.joscar.snaccmd.auth.*;
import net.kano.joustsim.*;
import net.kano.joustsim.oscar.*;
import net.kano.joustsim.oscar.oscar.loginstatus.*;
import net.kano.joustsim.oscar.oscar.service.*;
import net.kano.joustsim.oscar.oscar.service.icbm.*;
import net.kano.joustsim.oscar.proxy.*;

/**
 * An implementation of the protocol provider service over the AIM/ICQ protocol
 *
 * @author Emil Ivov
 * @author Damian Minkov
 * @author Valentin Martinet
 */
public class ProtocolProviderServiceIcqImpl
    extends AbstractProtocolProviderService
{
    /**
     * This class logger.
     */
    private static final Logger logger =
        Logger.getLogger(ProtocolProviderServiceIcqImpl.class);

    /**
     * Application session.
     */
    private DefaultAppSession session = null;

    /**
     * Protocol stack session.
     */
    private AimSession aimSession = null;

    /**
     * Protocol stack connection.
     */
    private AimConnection aimConnection = null;

    /**
     * Messenger service.
     */
    private IcbmService icbmService = null;

    /**
     * Listener that catches all connection events originating from joscar
     * during connection to icq.
     */
    private AimConnStateListener aimConnStateListener = null;

    /**
     * Listener that catches all incoming and outgoing chat events generated
     * by joscar.
     */
    private AimIcbmListener aimIcbmListener = new AimIcbmListener();

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
     * Retrieves short or full user info, such as Name, Address, Nickname etc.
     */
    private InfoRetreiver infoRetreiver = null;

    /**
     * The icon corresponding to the icq protocol.
     */
    private ProtocolIconIcqImpl icqIcon
        = new ProtocolIconIcqImpl();

    /**
     * The icon corresponding to the aim protocol.
     */
    private ProtocolIconAimImpl aimIcon
        = new ProtocolIconAimImpl();

    /**
     *  Property whether we are using AIM or ICQ service
     */
    boolean USING_ICQ = true;

    /**
     * Used when we need to re-register
     */
    private SecurityAuthority authority = null;

    /**
     * Keeping track of the last fired registration state.
     */
    private RegistrationState lastRegistrationState = null;

    /**
     * The server to use for aim service.
     */
    private static final String AIM_DEFAULT_LOGIN_SERVER = "login.oscar.aol.com";

    /**
     * The server to use for icq service.
     */
    private static final String ICQ_DEFAULT_LOGIN_SERVER = "login.icq.com";

    /**
     * Returns the state of the registration of this protocol provider
     * @return the <tt>RegistrationState</tt> that this provider is
     * currently in or null in case it is in a unknown state.
     */
    public RegistrationState getRegistrationState()
    {
        if(getAimConnection() == null || lastRegistrationState == null)
            return RegistrationState.UNREGISTERED;

        return lastRegistrationState;
    }

    /**
     * Converts the specified joust sim connection state to a corresponding
     * RegistrationState.
     * @param joustSimConnState the joust sim connection state.
     * @param joustSimConnStateInfo additional stateinfo if available (may be
     * null)
     * @return a RegistrationState corresponding best to the specified
     * joustSimState.
     */
    private RegistrationState joustSimStateToRegistrationState(
        State joustSimConnState,
        StateInfo joustSimConnStateInfo)
    {
        if(joustSimConnState == State.ONLINE)
            return RegistrationState.REGISTERED;
        else if (joustSimConnState == State.CONNECTING)
            return RegistrationState.REGISTERING;
        else if( joustSimConnState == State.AUTHORIZING)
            return RegistrationState.AUTHENTICATING;
        else if (joustSimConnState == State.CONNECTINGAUTH)
            return RegistrationState.AUTHENTICATING;
        else if (joustSimConnState == State.SIGNINGON)
            return RegistrationState.REGISTERING;
        else if (joustSimConnState == State.DISCONNECTED
                 || joustSimConnState == State.NOTCONNECTED)
        {
            if(joustSimConnStateInfo != null
                && joustSimConnStateInfo instanceof DisconnectedStateInfo
                && !((DisconnectedStateInfo)joustSimConnStateInfo).isOnPurpose())
            {
                return RegistrationState.CONNECTION_FAILED;
            }

            return RegistrationState.UNREGISTERED;
        }
        else if (joustSimConnState == State.FAILED)
        {
            if(joustSimConnStateInfo != null
                && joustSimConnStateInfo instanceof LoginFailureStateInfo)
            {
                LoginFailureInfo lfInfo = ((LoginFailureStateInfo)
                                joustSimConnStateInfo).getLoginFailureInfo();

                if (lfInfo instanceof AuthFailureInfo)
                    return RegistrationState.AUTHENTICATION_FAILED;
            }
            return RegistrationState.CONNECTION_FAILED;
        }
        else{
            logger.warn("Unknown state " + joustSimConnState
                        + ". Defaulting to " + RegistrationState.UNREGISTERED);
            return RegistrationState.UNREGISTERED;
        }
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
     * @throws OperationFailedException with the corresponding code it the
     *        registration fails for some reason (e.g. a networking error or an
     *        implementation problem).
     */
    public void register(SecurityAuthority authority)
        throws OperationFailedException
    {
        if(authority == null)
            throw new IllegalArgumentException(
                "The register method needs a valid non-null authority impl "
                + " in order to be able and retrieve passwords.");

        // Keep the authority in case we need to re-register.
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
     * @param reasonCode reason code in case of reconnect.
     * @throws  OperationFailedException if login parameters
     *          as server port are not correct
     */
    private void connectAndLogin(SecurityAuthority authority, int reasonCode)
        throws OperationFailedException
    {
        synchronized(initializationLock)
        {
            ProtocolProviderFactoryIcqImpl protocolProviderFactory = null;

            if(USING_ICQ)
                protocolProviderFactory
                    = IcqActivator.getIcqProtocolProviderFactory();
            else
                protocolProviderFactory
                    = IcqActivator.getAimProtocolProviderFactory();

            //verify whether a password has already been stored for this account
            String password
                = protocolProviderFactory.loadPassword(getAccountID());

            //decode
            if( password == null )
            {
                //create a default credentials object
                UserCredentials credentials = new UserCredentials();
                credentials.setUserName(this.getAccountID().getUserID());

                //request a password from the user
                credentials = authority.obtainCredentials(
                    accountID.getDisplayName(),
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
                        RegistrationState.UNREGISTERED,
                        RegistrationState.UNREGISTERED,
                        RegistrationStateChangeEvent.REASON_USER_REQUEST, "");
                    return;
                }

                password = new String(pass);

                if (credentials.isPasswordPersistent())
                {
                    protocolProviderFactory
                        .storePassword(getAccountID(), password);
                }
            }

            // it seems icq servers doesn't accept password with
            // length more then 8. But allow such registrations
            // we must trim such passwords to 8 characters
            if(USING_ICQ && password.length() > 8)
                password = password.substring(0, 8);

            //init the necessary objects
            session = new DefaultAppSession();
            aimSession = session.openAimSession(
                new Screenname(getAccountID().getUserID()));

            String globalProxyType =
                IcqActivator.getConfigurationService()
                    .getString(ProxyInfo.CONNECTION_PROXY_TYPE_PROPERTY_NAME);
            if(globalProxyType != null &&
               globalProxyType.equals(ProxyInfo.ProxyType.HTTP.name()))
            {
                String globalProxyAddress =
                    IcqActivator.getConfigurationService().getString(
                        ProxyInfo.CONNECTION_PROXY_ADDRESS_PROPERTY_NAME);
                String globalProxyPortStr =
                    IcqActivator.getConfigurationService().getString(
                        ProxyInfo.CONNECTION_PROXY_PORT_PROPERTY_NAME);

                int proxyPort;
                try
                {
                    proxyPort = Integer.parseInt(globalProxyPortStr);
                }
                catch (NumberFormatException ex)
                {
                    throw new OperationFailedException("Wrong port",
                        OperationFailedException.INVALID_ACCOUNT_PROPERTIES, ex);
                }

                String globalProxyUsername =
                    IcqActivator.getConfigurationService().getString(
                        ProxyInfo.CONNECTION_PROXY_USERNAME_PROPERTY_NAME);
                String globalProxyPassword =
                    IcqActivator.getConfigurationService().getString(
                        ProxyInfo.CONNECTION_PROXY_PASSWORD_PROPERTY_NAME);
                if(globalProxyAddress == null ||
                    globalProxyAddress.length() <= 0)
                {
                    throw new OperationFailedException(
                        "Missing Proxy Address",
                        OperationFailedException.INVALID_ACCOUNT_PROPERTIES);
                }

                // If we are using http proxy, sometimes
                // default port 5190 is forbidden, so force
                // http/https port
                AimConnectionProperties connProps =
                    new AimConnectionProperties(
                        new Screenname(getAccountID().getUserID())
                        , password);

                if(USING_ICQ)
                    connProps.setLoginHost(ICQ_DEFAULT_LOGIN_SERVER);
                else
                    connProps.setLoginHost(AIM_DEFAULT_LOGIN_SERVER);
                connProps.setLoginPort(443);

                aimConnection = aimSession.openConnection(connProps);
                aimConnection.setProxy(AimProxyInfo.forHttp(
                    globalProxyAddress, proxyPort,
                    globalProxyUsername, globalProxyPassword));
            }
            else
            {
                AimConnectionProperties connProps =
                    new AimConnectionProperties(
                            new Screenname(getAccountID().getUserID())
                            , password);

                if(USING_ICQ)
                    connProps.setLoginHost(ICQ_DEFAULT_LOGIN_SERVER);
                else
                    connProps.setLoginHost(AIM_DEFAULT_LOGIN_SERVER);

                aimConnection = aimSession.openConnection(connProps);
            }

            aimConnStateListener = new AimConnStateListener();
            aimConnection.addStateListener(aimConnStateListener);

            aimConnection.connect();
        }
    }

    /**
     * Ends the registration of this protocol provider with the service.
     */
    public void unregister()
    {
        fireRegistrationStateChanged(
                getRegistrationState(),
                RegistrationState.UNREGISTERING,
                RegistrationStateChangeEvent.REASON_USER_REQUEST,
                null);

        if(aimConnection != null)
            aimConnection.disconnect(true);
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
     * provider is based upon (like SIP, Jabber, ICQ/AIM, or others for
     * example).
     *
     * @return a String containing the short name of the protocol this
     *   service is taking care of.
     */
    public String getProtocolName()
    {
        if(USING_ICQ)
            return ProtocolNames.ICQ;
        else
            return ProtocolNames.AIM;
    }

    /**
     * Returns the protocol display name. This is the name that would be used
     * by the GUI to display the protocol name.
     *
     * @return a String containing the display name of the protocol this service
     * is implementing
     */
    @Override
    public String getProtocolDisplayName()
    {
        if(USING_ICQ)
            return ProtocolNames.ICQ;
        else
            return ProtocolNames.AIM;
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

            if(IcqAccountID.isAIM(accountID.getAccountProperties()))
                    USING_ICQ = false;

            addSupportedOperationSet(
                OperationSetInstantMessageTransform.class,
                new OperationSetInstantMessageTransformImpl());

            //initialize the presence operationset
            OperationSetPersistentPresence persistentPresence =
                new OperationSetPersistentPresenceIcqImpl(this, screenname);

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
                new OperationSetBasicInstantMessagingIcqImpl(this));

            //initialize the multi chat operation set
            addSupportedOperationSet(
                OperationSetAdHocMultiUserChat.class,
                new OperationSetAdHocMultiUserChatIcqImpl(this));

            //initialize the typing notifications operation set
            addSupportedOperationSet(
                OperationSetTypingNotifications.class,
                new OperationSetTypingNotificationsIcqImpl(this));

            if(USING_ICQ)
            {
                this.infoRetreiver = new InfoRetreiver(this, screenname);

                addSupportedOperationSet(
                    OperationSetServerStoredContactInfo.class,
                    new OperationSetServerStoredContactInfoIcqImpl(
                            infoRetreiver,
                            this));

                OperationSetServerStoredAccountInfoIcqImpl
                    serverStoredAccountInfoOpSet =
                        new OperationSetServerStoredAccountInfoIcqImpl(
                            infoRetreiver,
                            screenname,
                            this);
                addSupportedOperationSet(
                    OperationSetServerStoredAccountInfo.class,
                    serverStoredAccountInfoOpSet);

//                Currently disabled as when we send avatar
//                we receive an error from server
//                addSupportedOperationSet(
//                    OperationSetAvatar.class,
//                    new OperationSetAvatarIcqImpl(
//                            this,
//                            serverStoredAccountInfoOpSet));

                addSupportedOperationSet(
                    OperationSetWebAccountRegistration.class,
                    new OperationSetWebAccountRegistrationIcqImpl());

                addSupportedOperationSet(
                    OperationSetWebContactInfo.class,
                    new OperationSetWebContactInfoIcqImpl());

                addSupportedOperationSet(
                    OperationSetExtendedAuthorizations.class,
                    new OperationSetExtendedAuthorizationsIcqImpl(this));
            }

            addSupportedOperationSet(
                OperationSetFileTransfer.class,
                new OperationSetFileTransferIcqImpl(this));

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
        /** @todo is there anything else to add here? */
        synchronized(initializationLock){
            icbmService = null;
            session = null;
            aimSession = null;
            aimConnection = null;
            aimConnStateListener = null;
            aimIcbmListener = null;
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
     * Creates a RegistrationStateChange event corresponding to the specified
     * old and new joust sim states and notifies all currently registered
     * listeners.
     *
     * @param oldJoustSimState the state that the joust sim connection had
     * before the change occurred
     * @param oldJoustSimStateInfo the state info associated with the state of
     * the underlying connection state as it is after the change.
     * @param newJoustSimState the state that the underlying joust sim
     * connection is currently in.
     * @param newJoustSimStateInfo the state info associated with the state of
     * the underlying connection state as it was before the change.
     * @param reasonCode a value corresponding to one of the REASON_XXX fields
     * of the RegistrationStateChangeEvent class, indicating the reason for this
     * state transition.
     * @param reason a String further explaining the reason code or null if
     * no such explanation is necessary.
     */
    private void fireRegistrationStateChanged(  State      oldJoustSimState,
                                                StateInfo oldJoustSimStateInfo,
                                                State     newJoustSimState,
                                                StateInfo newJoustSimStateInfo,
                                                int       reasonCode,
                                                String    reason)
    {
        RegistrationState oldRegistrationState
            = joustSimStateToRegistrationState(oldJoustSimState
                                               , oldJoustSimStateInfo);
        RegistrationState newRegistrationState
            = joustSimStateToRegistrationState(newJoustSimState
                                               , newJoustSimStateInfo);

        fireRegistrationStateChanged(oldRegistrationState, newRegistrationState
                                     , reasonCode, reason);
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
        lastRegistrationState = newState;

        super.fireRegistrationStateChanged(
            oldState, newState, reasonCode, reason);
    }

    /**
     * Returns the info retriever that we've initialized for the current
     * session.
     *
     * @return the info retriever that we've initialized for the current
     * session.
     */
    protected InfoRetreiver getInfoRetreiver()
    {
        return infoRetreiver;
    }

    /**
     * This class handles connection state events that have originated in this
     * provider's aim connection. Events are acted upon accordingly and,
     * if necessary, forwarded to registered listeners (asynchronously).
     */
    private class AimConnStateListener implements StateListener
    {
        /**
         * Connection state changes being reported here.
         * @param event
         */
        public void handleStateChange(StateEvent event)
        {
            State newState = event.getNewState();
            State oldState = event.getOldState();

            AimConnection conn = event.getAimConnection();
            if (logger.isDebugEnabled())
                logger.debug("ICQ protocol provider " + getProtocolName()
                         + " changed registration status from "
                         + oldState + " to " + newState);

            int reasonCode = RegistrationStateChangeEvent.REASON_NOT_SPECIFIED;
            String reasonStr = null;

            if (newState == State.ONLINE)
            {
                icbmService = conn.getIcbmService();
                icbmService.addIcbmListener(aimIcbmListener);

                conn.getInfoService().
                    getOscarConnection().getSnacProcessor().
                        getFlapProcessor().addPacketListener(
                            new ConnectionClosedListener(conn));
            }
            else if (newState == State.DISCONNECTED)
            {
                // we need a Service here. no metter which
                // I've choose BosService
                // we just need the oscar conenction from the service
                Service service = aimConnection.getBosService();
                if(service != null)
                {
                    int discconectCode = service.getOscarConnection()
                        .getLastCloseCode();
                    reasonCode = ConnectionClosedListener
                        .convertCodeToRegistrationStateChangeEvent(
                            discconectCode);
                    reasonStr = ConnectionClosedListener
                        .convertCodeToStringReason(discconectCode);
                    if (logger.isDebugEnabled())
                        logger.debug(
                        "The aim Connection was disconnected! with reason : "
                        + reasonStr);
                }

                if(reasonCode == RegistrationStateChangeEvent.REASON_NOT_SPECIFIED
                   && event.getNewStateInfo() instanceof DisconnectedStateInfo)
                {
                    // if its on purpose it is user request
                    if(((DisconnectedStateInfo)event.getNewStateInfo()).isOnPurpose())
                    {
                        reasonCode =
                            RegistrationStateChangeEvent.REASON_USER_REQUEST;
                    }
                }
                else
                    if (logger.isDebugEnabled())
                        logger.debug("The aim Connection was disconnected!");
            }
            else if(newState == State.FAILED)
            {
                // assume that a failure during connect&resolve is a DNSSEC
                // validation error
                if (oldState == State.CONNECTINGAUTH &&
                    conn.getLoginService() != null &&
                    conn.getLoginService().getOscarConnection() != null &&
                    conn.getLoginService().getOscarConnection()
                        .getConnectionState() == ClientConn.STATE_RESOLVING
                )
                {
                    fireRegistrationStateChanged(
                        getRegistrationState(),
                        RegistrationState.UNREGISTERED,
                        RegistrationStateChangeEvent.REASON_USER_REQUEST,
                        "Disconnected due to assumed DNSSEC failure");
                    return;
                }
                else if (logger.isDebugEnabled())
                    logger.debug("The aim Connection failed! "
                             + event.getNewStateInfo());
            }

            if(event.getNewStateInfo() instanceof LoginFailureStateInfo)
            {
                LoginFailureInfo loginFailure =
                    ((LoginFailureStateInfo)event.getNewStateInfo())
                        .getLoginFailureInfo();

                if(loginFailure instanceof AuthFailureInfo)
                {
                    AuthFailureInfo afi = (AuthFailureInfo)loginFailure;
                    if (logger.isDebugEnabled())
                        logger.debug("AuthFailureInfo code : " +
                                 afi.getErrorCode());
                    int code =  ConnectionClosedListener
                        .convertAuthCodeToReasonCode(afi);
                    reasonCode = ConnectionClosedListener
                        .convertCodeToRegistrationStateChangeEvent(code);
                    reasonStr = ConnectionClosedListener
                        .convertCodeToStringReason(code);
                }
            }

            //as a side note - if this was an AuthenticationFailed error
            //set the stored password to null so that we don't use it any more.
            if(reasonCode == RegistrationStateChangeEvent
                .REASON_AUTHENTICATION_FAILED)
            {
                if(USING_ICQ)
                    IcqActivator.getIcqProtocolProviderFactory().storePassword(
                        getAccountID(), null);
                else
                    IcqActivator.getAimProtocolProviderFactory().storePassword(
                        getAccountID(), null);

                reconnect(SecurityAuthority.WRONG_PASSWORD);
            }

            if (newState == State.ONLINE)
            {
                // we will fire FINALIZING_REGISTRATION and will wait
                // for the registration to finnish.
                fireRegistrationStateChanged(lastRegistrationState,
                    RegistrationState.FINALIZING_REGISTRATION, -1, null);

                // we must wait a little bit before firing registered
                // event , waiting for ClientReadyCommand to be sent successfully
                new RegisteredEventThread().start();
            }
            else
            {
                //now tell all interested parties about what happened.
                fireRegistrationStateChanged(
                    oldState,
                    event.getOldStateInfo(),
                    newState,
                    event.getNewStateInfo(),
                    reasonCode,
                    reasonStr);
            }
        }
    }

    /**
     * Throw registered with 2 seconds delay.
     * We must wait a little bit before firing registered
     * event , waiting for ClientReadyCommand to be sent successfully
     */
    private class RegisteredEventThread extends Thread
    {
        @Override
        public void run()
        {
            Object w = new Object();
            synchronized(w)
            {
                try
                {
                    w.wait(2000);
                }
                catch (Exception e)
                {}
            }

            fireRegistrationStateChanged(lastRegistrationState,
                RegistrationState.REGISTERED, -1, null);
        }
    }

    /**
     * Returns the <tt>AimSession</tt> opened by this provider.
     * @return a reference to the <tt>AimSession</tt> that this provider
     * last opened.
     */
    protected AimSession getAimSession()
    {
        return aimSession;
    }

    /**
     * Returns the <tt>AimConnection</tt>opened by this provider
     * @return a reference to the <tt>AimConnection</tt> last opened by this
     * provider.
     */
    protected AimConnection getAimConnection()
    {
        return aimConnection;
    }

    /**
     * Message listener.
     */
    public static class AimIcbmListener implements IcbmListener
    {

        /**
         * New conversations.
         * @param service the messenger service.
         * @param conv the new conversation.
         */
        public void newConversation(IcbmService service, Conversation conv)
        {
            if (logger.isDebugEnabled())
                logger.debug("Received a new conversation event");
            conv.addConversationListener(new AimConversationListener());
        }

        /**
         * Does nothing.
         * @param service
         * @param buddy
         * @param info
         */
        public void buddyInfoUpdated(IcbmService service, Screenname buddy,
                                     IcbmBuddyInfo info)
        {
            if (logger.isDebugEnabled())
                logger.debug("Got a BuddINFO event");
        }

        /**
         * Does nothing.
         * @param service
         * @param message
         * @param triedConversations
         */
        public void sendAutomaticallyFailed(
            IcbmService service,
            net.kano.joustsim.oscar.oscar.service.icbm.Message message,
            Set<Conversation> triedConversations)
        {
        }
    }

    public static class AimConversationListener
        implements ConversationListener
    {
        public void sentOtherEvent(Conversation conversation,
                                   ConversationEventInfo event)
        {
            if (logger.isDebugEnabled())
                logger.debug("reveived ConversationEventInfo:" + event);
        }

        // This may be called without ever calling conversationOpened
        public void conversationClosed(Conversation co)
        {
            if (logger.isDebugEnabled())
                logger.debug("conversation closed");
        }

        public void gotOtherEvent(Conversation conversation,
                                  ConversationEventInfo event)
        {
            if (logger.isDebugEnabled())
                logger.debug("goet other event");
            if(event instanceof TypingInfo)
            {
                TypingInfo ti = (TypingInfo)event;
                if (logger.isDebugEnabled())
                    logger.debug("got typing info and state is: "
                             + ti.getTypingState());
            }
            else if (event instanceof MessageInfo)
            {
                MessageInfo ti = (MessageInfo)event;
                if (logger.isDebugEnabled())
                    logger.debug("got message info for msg: " + ti.getMessage());
            }
        }

        public void canSendMessageChanged(Conversation con, boolean canSend)
        {
            if (logger.isDebugEnabled())
                logger.debug("can send message event");
        }

        // This may never be called
        public void conversationOpened(Conversation con)
        {
            if (logger.isDebugEnabled())
                logger.debug("conversation opened event");
        }

        // This may be called after conversationClosed is called
        public void sentMessage(Conversation con, MessageInfo minfo)
        {
            if (logger.isDebugEnabled())
                logger.debug("sent message event");
        }

        // This may be called after conversationClosed is called.
        public void gotMessage(Conversation con, MessageInfo minfo)
        {
            if (logger.isDebugEnabled())
                logger.debug("got message event"
                         + minfo.getMessage().getMessageBody());
        }

    }

    /**
     * Fix for late close connection due to
     * multiple logins.
     * Listening for incoming packets for the close command
     * when this is received we disconnect the session to force it
     * because otherwise is wait for timeout of reading from the socket stream
     * which leads to from 10 to 20 seconds delay of closing the session
     * and connection
     * */
    public static class ConnectionClosedListener
        implements FlapPacketListener
    {
        private AimConnection aimConnection = null;

        private final static int REASON_MULTIPLE_LOGINS = 0x0001;
        private final static int REASON_BAD_PASSWORD_A = 0x0004;
        private final static int REASON_BAD_PASSWORD_B = 0x0005;
        private final static int REASON_NON_EXISTING_ICQ_UIN_A = 0x0007;
        private final static int REASON_NON_EXISTING_ICQ_UIN_B = 0x0008;
        private final static int REASON_MANY_CLIENTS_FROM_SAME_IP_A = 0x0015;
        private final static int REASON_MANY_CLIENTS_FROM_SAME_IP_B = 0x0016;
        private final static int REASON_CONNECTION_RATE_EXCEEDED = 0x0018;
        private final static int REASON_CONNECTION_TOO_FAST = 0x001D;
        private final static int REASON_TRY_AGAIN = 0x001E;

        private final static String REASON_STRING_MULTIPLE_LOGINS
            = "multiple logins (on same UIN)";
        private final static String REASON_STRING_BAD_PASSWORD
            = "bad password";
        private final static String REASON_STRING_NON_EXISTING_ICQ_UIN
            = "non-existant UIN";
        private final static String REASON_STRING_MANY_CLIENTS_FROM_SAME_IP
            = "too many clients from same IP";
        private final static String REASON_STRING_CONNECTION_RATE_EXCEEDED
            = "Rate exceeded. The server temporarily bans you.";
        private final static String REASON_STRING_CONNECTION_TOO_FAST
            = "You are reconnecting too fast";
        private final static String REASON_STRING_TRY_AGAIN
            = "Can't register on ICQ network, try again soon.";
        private final static String REASON_STRING_NOT_SPECIFIED
            = "Not Specified";

        ConnectionClosedListener(AimConnection aimConnection)
        {
            this.aimConnection = aimConnection;
        }


        public void handleFlapPacket(FlapPacketEvent evt)
        {
            FlapCommand flapCommand = evt.getFlapCommand();
            if (flapCommand instanceof CloseFlapCmd)
            {
                CloseFlapCmd closeCmd = (CloseFlapCmd)flapCommand;
                if (logger.isTraceEnabled())
                    logger.trace("received close command with code : "
                             + closeCmd.getCode());

                aimConnection.disconnect();
            }
        }

        /**
         * Converts the codes in the close command
         * or the lastCloseCode of OscarConnection to the states
         * which are registered in the service protocol events
         *
         * @param reasonCode int the reason of close connection
         * @return int corresponding RegistrationStateChangeEvent
         */
        static int convertCodeToRegistrationStateChangeEvent(int reasonCode)
        {
            switch(reasonCode)
            {
                case REASON_MULTIPLE_LOGINS :
                    return RegistrationStateChangeEvent
                        .REASON_MULTIPLE_LOGINS;
                case REASON_BAD_PASSWORD_A :
                    return RegistrationStateChangeEvent
                        .REASON_AUTHENTICATION_FAILED;
                case REASON_BAD_PASSWORD_B :
                    return RegistrationStateChangeEvent
                        .REASON_AUTHENTICATION_FAILED;
                case REASON_NON_EXISTING_ICQ_UIN_A :
                    return RegistrationStateChangeEvent
                        .REASON_NON_EXISTING_USER_ID;
                case REASON_NON_EXISTING_ICQ_UIN_B :
                    return RegistrationStateChangeEvent
                        .REASON_NON_EXISTING_USER_ID;
                case REASON_MANY_CLIENTS_FROM_SAME_IP_A :
                    return RegistrationStateChangeEvent
                        .REASON_CLIENT_LIMIT_REACHED_FOR_IP;
                case REASON_MANY_CLIENTS_FROM_SAME_IP_B :
                    return RegistrationStateChangeEvent
                        .REASON_CLIENT_LIMIT_REACHED_FOR_IP;
                case REASON_CONNECTION_RATE_EXCEEDED :
                    return RegistrationStateChangeEvent
                        .REASON_RECONNECTION_RATE_LIMIT_EXCEEDED;
                case REASON_CONNECTION_TOO_FAST :
                    return RegistrationStateChangeEvent
                        .REASON_RECONNECTION_RATE_LIMIT_EXCEEDED;
                case REASON_TRY_AGAIN :
                    return RegistrationStateChangeEvent
                        .REASON_RECONNECTION_RATE_LIMIT_EXCEEDED;
                default :
                    return RegistrationStateChangeEvent
                        .REASON_NOT_SPECIFIED;
            }
        }

        /**
         * returns the reason string corresponding to the code
         * in the close command
         *
         * @param reasonCode int the reason of close connection
         * @return String describing the reason
         */
        static String convertCodeToStringReason(int reasonCode)
        {
            switch(reasonCode)
            {
                case REASON_MULTIPLE_LOGINS :
                    return REASON_STRING_MULTIPLE_LOGINS;
                case REASON_BAD_PASSWORD_A :
                    return REASON_STRING_BAD_PASSWORD;
                case REASON_BAD_PASSWORD_B :
                    return REASON_STRING_BAD_PASSWORD;
                case REASON_NON_EXISTING_ICQ_UIN_A :
                    return REASON_STRING_NON_EXISTING_ICQ_UIN;
                case REASON_NON_EXISTING_ICQ_UIN_B :
                    return REASON_STRING_NON_EXISTING_ICQ_UIN;
                case REASON_MANY_CLIENTS_FROM_SAME_IP_A :
                    return REASON_STRING_MANY_CLIENTS_FROM_SAME_IP;
                case REASON_MANY_CLIENTS_FROM_SAME_IP_B :
                    return REASON_STRING_MANY_CLIENTS_FROM_SAME_IP;
                case REASON_CONNECTION_RATE_EXCEEDED :
                    return REASON_STRING_CONNECTION_RATE_EXCEEDED;
                case REASON_CONNECTION_TOO_FAST :
                    return REASON_STRING_CONNECTION_TOO_FAST;
                case REASON_TRY_AGAIN :
                    return REASON_STRING_TRY_AGAIN;
                default :
                    return REASON_STRING_NOT_SPECIFIED;
            }
        }

        /**
         * When receiving login failure
         * the reasons for the failure are in the authorization
         * part of the protocol ( 0x13 )
         * In the AuthResponse are the possible reason codes
         * here they are converted to those in the ConnectionClosedListener
         * so the they can be converted to the one in service protocol events
         *
         * @param afi AuthFailureInfo the failure info
         * @return int the corresponding code to this failure
         */
        static int convertAuthCodeToReasonCode(AuthFailureInfo afi)
        {
            switch(afi.getErrorCode())
            {
                case AuthResponse.ERROR_BAD_PASSWORD :
                    return REASON_BAD_PASSWORD_A;
                case AuthResponse.ERROR_CONNECTING_TOO_MUCH_A :
                    return REASON_CONNECTION_RATE_EXCEEDED;
                case AuthResponse.ERROR_CONNECTING_TOO_MUCH_B :
                    return REASON_CONNECTION_RATE_EXCEEDED;
                case AuthResponse.ERROR_INVALID_SN_OR_PASS_A :
                    return REASON_NON_EXISTING_ICQ_UIN_A;
                case AuthResponse.ERROR_INVALID_SN_OR_PASS_B :
                    return REASON_NON_EXISTING_ICQ_UIN_B;
                // 16 is also used for blocked from same IP
                case 16 :
                    return REASON_MANY_CLIENTS_FROM_SAME_IP_A;
                case AuthResponse.ERROR_SIGNON_BLOCKED :
                    return REASON_MANY_CLIENTS_FROM_SAME_IP_B;
                default :
                    return RegistrationStateChangeEvent.REASON_NOT_SPECIFIED;
            }
        }
    }

    /**
     * Returns the icq/aim protocol icon.
     * @return the icq/aim protocol icon
     */
    public ProtocolIcon getProtocolIcon()
    {
        if(USING_ICQ)
            return icqIcon;
        else
            return aimIcon;
    }
}
