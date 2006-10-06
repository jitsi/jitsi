/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.icq;

import java.util.*;

import net.java.sip.communicator.impl.protocol.icq.message.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;
import net.kano.joscar.flap.*;
import net.kano.joscar.flapcmd.*;
import net.kano.joscar.snaccmd.auth.*;
import net.kano.joustsim.*;
import net.kano.joustsim.oscar.*;
import net.kano.joustsim.oscar.State;
import net.kano.joustsim.oscar.oscar.loginstatus.*;
import net.kano.joustsim.oscar.oscar.service.*;
import net.kano.joustsim.oscar.oscar.service.icbm.*;

/**
 * An implementation of the protocol provider service over the AIM/ICQ protocol
 *
 * @author Emil Ivov
 * @author Damian Minkov
 */
public class ProtocolProviderServiceIcqImpl
    implements ProtocolProviderService
{
    private static final Logger logger =
        Logger.getLogger(ProtocolProviderServiceIcqImpl.class);

    /**
     * The hashtable with the operation sets that we support locally.
     */
    private Hashtable supportedOperationSets = new Hashtable();

    private DefaultAppSession session = null;

    private AimSession aimSession = null;

    private AimConnection aimConnection = null;

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
     * A list of all listeners registered for
     * <tt>RegistrationStateChangeEvent</tt>s.
     */
    private List registrationListeners = new ArrayList();

    /**
     * The identifier of the account that this provider represents.
     */
    private AccountID accountID = null;

    /**
     * Retrieves short or full user info, such as Name, Address, Nickname etc.
     */
    private InfoRetreiver infoRetreiver = null;

    /**
     * Returns the state of the registration of this protocol provider
     * @return the <tt>RegistrationState</tt> that this provider is
     * currently in or null in case it is in a unknown state.
     */
    public RegistrationState getRegistrationState()
    {
        if(getAimConnection() == null)
            return RegistrationState.UNREGISTERED;

        State connState = getAimConnection().getState();

        return joustSimStateToRegistrationState(connState);
    }


    /**
     * Converts the specified joust sim connection state to a corresponding
     * RegistrationState.
     * @param jsState the joust sim connection state.
     * @return a RegistrationState corresponding best to the specified
     * joustSimState.
     */
    private RegistrationState joustSimStateToRegistrationState(State jsState)
    {
        return joustSimStateToRegistrationState(jsState, null);
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
            return RegistrationState.UNREGISTERED;
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
     */
    public void register(SecurityAuthority authority)
    {
        if(authority == null)
            throw new IllegalArgumentException(
                "The register method needs a valid non-null authority impl "
                + " in order to be able and retrieve passwords.");

        synchronized(initializationLock)
        {
            //verify whether a password has already been stored for this account
            String password = IcqActivator.getProtocolProviderFactory()
                .loadPassword(getAccountID());

            //decode
            if( password == null )
            {
                //create a default credentials object
                UserCredentials credentials = new UserCredentials();
                credentials.setUserName(this.getAccountID().getUserID());

                //request a password from the user
                credentials = authority.obtainCredentials(ProtocolNames.ICQ
                                                          , credentials);
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
                    IcqActivator.getProtocolProviderFactory()
                        .storePassword(getAccountID(), password);
                }
            }

            //init the necessary objects
            session = new DefaultAppSession();
            aimSession = session.openAimSession(
                new Screenname(getAccountID().getUserID()));
            aimConnection = aimSession.openConnection(
                new AimConnectionProperties(
                    new Screenname(getAccountID().getUserID())
                    , password));

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
        aimConnection.disconnect(true);
    }

    /**
     * Indicates whether or not this provider is signed on the icq service
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
        return ProtocolNames.ICQ;
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
                new OperationSetPersistentPresenceIcqImpl(this, screenname);

            supportedOperationSets.put(
                OperationSetPersistentPresence.class.getName(),
                persistentPresence);

            //register it once again for those that simply need presence
            supportedOperationSets.put( OperationSetPresence.class.getName(),
                                        persistentPresence);

            //initialize the IM operation set
            OperationSetBasicInstantMessaging basicInstantMessaging =
                new OperationSetBasicInstantMessagingIcqImpl(this);

            supportedOperationSets.put(
                OperationSetBasicInstantMessaging.class.getName(),
                basicInstantMessaging);

            //initialize the typing notifications operation set
            OperationSetTypingNotifications typingNotifications =
                new OperationSetTypingNotificationsIcqImpl(this);

            supportedOperationSets.put(
                OperationSetTypingNotifications.class.getName(),
                typingNotifications);

            this.infoRetreiver = new InfoRetreiver(this, screenname);

            OperationSetServerStoredContactInfo serverStoredContactInfo =
                new OperationSetServerStoredContactInfoIcqImpl(infoRetreiver);

            supportedOperationSets.put(
                OperationSetServerStoredContactInfo.class.getName(),
                serverStoredContactInfo);


            OperationSetServerStoredAccountInfo serverStoredAccountInfo =
                new OperationSetServerStoredAccountInfoIcqImpl
                    (infoRetreiver, screenname, this);

            supportedOperationSets.put(
                OperationSetServerStoredAccountInfo.class.getName(),
                serverStoredAccountInfo);

            OperationSetWebAccountRegistration webAccountRegistration =
                new OperationSetWebAccountRegistrationIcqImpl();
            supportedOperationSets.put(
                OperationSetWebAccountRegistration.class.getName(),
                webAccountRegistration);

            OperationSetWebContactInfo webContactInfo =
                new OperationSetWebContactInfoIcqImpl();
            supportedOperationSets.put(
                OperationSetWebContactInfo.class.getName(),
                webContactInfo);

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

        if(newJoustSimStateInfo instanceof LoginFailureStateInfo)
        {
            LoginFailureInfo loginFailure =
                ((LoginFailureStateInfo)newJoustSimStateInfo)
                    .getLoginFailureInfo();

            if(loginFailure instanceof AuthFailureInfo)
            {
                AuthFailureInfo afi = (AuthFailureInfo)loginFailure;
                logger.debug("AuthFailureInfo code : " +
                             afi.getErrorCode());
                int code =  ConnectionClosedListener
                    .convertAuthCodeToReasonCode(afi);
                reasonCode = ConnectionClosedListener
                    .convertCodeToRegistrationStateChangeEvent(code);
                reason = ConnectionClosedListener
                    .convertCodeToStringReason(code);
            }
        }

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
        public void handleStateChange(StateEvent event)
        {
            State newState = event.getNewState();
            State oldState = event.getOldState();

            AimConnection conn = event.getAimConnection();
            logger.debug("ICQ protocol provider " + getProtocolName()
                         + " changed registration status from "
                         + oldState + " to " + newState);

            int reasonCode = RegistrationStateChangeEvent.REASON_NOT_SPECIFIED;
            String reasonStr = null;

            if (newState == State.ONLINE)
            {
                icbmService = conn.getIcbmService();
                icbmService.addIcbmListener(aimIcbmListener);

                //set our own cmd factory as we'd like some extra control on
                //outgoing commands.
                conn.getInfoService().
                    getOscarConnection().getSnacProcessor().
                        getCmdFactoryMgr().getDefaultFactoryList().
                            registerAll(new DefaultCmdFactory());

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
                    logger.debug(
                        "The aim Connection was disconnected! with reason : "
                        + reasonStr);
                }
                else
                    logger.debug("The aim Connection was disconnected!");
            }
            else
                if(newState == State.FAILED)
                {
                    logger.debug("The aim Connection failed! "
                                 + event.getNewStateInfo());
                }

            //as a side note - if this was an AuthenticationFailed error
            //set the stored password to null so that we don't use it any more.
            if(reasonCode == RegistrationStateChangeEvent
                .REASON_AUTHENTICATION_FAILED)
            {
                IcqActivator.getProtocolProviderFactory().storePassword(
                    getAccountID(), null);
            }

            //now tell all interested parties about what happened.
            fireRegistrationStateChanged(oldState, event.getOldStateInfo()
                , newState, event.getNewStateInfo()
                , reasonCode, reasonStr);

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

    public class AimIcbmListener implements IcbmListener
    {

        public void newConversation(IcbmService service, Conversation conv)
        {
            logger.debug("Received a new conversation event");
            conv.addConversationListener(new AimConversationListener());
        }

        public void buddyInfoUpdated(IcbmService service, Screenname buddy,
                                     IcbmBuddyInfo info)
        {
            logger.debug("Got a BuddINFO event");
        }
    }

    public class AimConversationListener implements ConversationListener{
        public void sentOtherEvent(Conversation conversation,
                                   ConversationEventInfo event)
        {
            logger.debug("reveived ConversationEventInfo:" + event);
        }

        // This may be called without ever calling conversationOpened
        public void conversationClosed(Conversation co)
        {
            logger.debug("conversation closed");
        }

        public void gotOtherEvent(Conversation conversation,
                                  ConversationEventInfo event)
        {
            logger.debug("goet other event");
            if(event instanceof TypingInfo)
            {
                TypingInfo ti = (TypingInfo)event;
                logger.debug("got typing info and state is: "
                             + ti.getTypingState());
            }
            else if (event instanceof MessageInfo)
            {
                MessageInfo ti = (MessageInfo)event;
                logger.debug("got message info for msg: " + ti.getMessage());
            }
        }

        public void canSendMessageChanged(Conversation con, boolean canSend)
        {
            logger.debug("can send message event");
        }

        // This may never be called
        public void conversationOpened(Conversation con)
        {
            logger.debug("conversation opened event");
        }

        // This may be called after conversationClosed is called
        public void sentMessage(Conversation con, MessageInfo minfo)
        {
            logger.debug("sent message event");
        }

        // This may be called after conversationClosed is called.
        public void gotMessage(Conversation con, MessageInfo minfo)
        {
            logger.debug("got message event"
                         + minfo.getMessage().getMessageBody());
        }

    }

    /**
     * Fix for late close conenction due to
     * multiple logins.
     * Listening for incoming packets for the close command
     * when this is received we discconect the session to force it
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
}
