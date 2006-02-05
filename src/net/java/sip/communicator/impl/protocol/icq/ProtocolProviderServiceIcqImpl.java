/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.icq;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.kano.joustsim.oscar.*;
import net.kano.joustsim.*;
import net.kano.joustsim.oscar.oscar.service.icbm.*;
import net.java.sip.communicator.util.*;

/**
 * An implementation of the protocol provider service over the AIM/ICQ protocol
 *
 * @author Emil Ivov
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
    private AimIcbmListener aimIcbmListener = null;

    /**
     * indicates whether or not the provider is initialized and ready for use.
     */
    private boolean isInitialized = false;
    private Object initializationLock = new Object();

    /**
     * A list of all listeners registered for
     * <tt>RegistrationStateChangeEvent</tt>s.
     */
    private List registrationListeners = new ArrayList();

    /**
     * Returns the state of the registration of this protocol provider
     * @return the <tt>RegistrationState</tt> that this provider is
     * currently in or null in case it is in a unknown state.
     */
    public RegistrationState getRegistrationState()
    {
        State connState = getAimConnection().getState();

        return joustSimStateToRegistrationState(connState);
    }


    /**
     * Converts the specified joust sim connection state to a corresponding
     * RegistrationState.
     * @param joustSimConnState the joust sim connection state.
     * @return a RegistrationState corresponding best to the specified
     * joustSimState.
     */
    private RegistrationState joustSimStateToRegistrationState(
        State joustSimConnState)
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
            return RegistrationState.CONNECTION_FAILED;
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
     */
    public void register(SecurityAuthority authority)
    {
        aimConnection.connect();
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
     * Initialized the service implementation, and puts it in a sate where it
     * could interoperate with other services. It is strongly recomended that
     * properties in this Map be mapped to property names as specified by
     * <tt>AccountProperties</tt>.
     *
     * @param screenname the account id/uin/screenname of the account that we're
     * about to create
     * @param initializationProperties all properties needed fo initializing the
     * account.
     *
     * @see net.java.sip.communicator.service.protocol.AccountProperties
     */
    protected void initialize(String screenname, Map initializationProperties)
    {
        synchronized(initializationLock)
        {
            //extract the necessary properties and validate them
            String password =
                (String)initializationProperties.get(AccountProperties.PASSWORD);

            //init the necessary objects
            session = new DefaultAppSession();
            aimSession = session.openAimSession(new Screenname(screenname));
            aimConnection = aimSession.openConnection(
                new AimConnectionProperties(new Screenname(screenname),
                                            password));

            aimConnStateListener = new AimConnStateListener();
            aimConnection.addStateListener(aimConnStateListener);
            aimIcbmListener = new AimIcbmListener();

            //initialize all the supported operation sets
            OperationSetPersistentPresence persistentPresence =
                new OperationSetPersistentPresenceIcqImpl(this, screenname);

            supportedOperationSets.put(
                OperationSetPersistentPresence.class.getName(),
                persistentPresence);

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
        registrationListeners.remove(listener);
    }

    /**
     * Registers the specified listener with this provider so that it would
     * receive notifications on changes of its state or other properties such
     * as its local address and display name.
     *
     * @param listener the listener to register.
     */
    public void addRegistrationStateChangeListener(RegistrationStateChangeListener listener)
    {
        registrationListeners.add(listener);
    }

    /**
     * Creates a RegistrationStateChange event corresponding to the specified
     * old and new joust sim states and notifies all currently registered
     * listeners.
     *
     * @param oldJoustSimState the state that the joust sim connection had
     * before the change occurred
     * @param newJoustSimState the state that the underlying joust sim
     * connection is currently in.
     */
    private void fireRegistrationStateChanged( State oldJoustSimState,
                                               State newJoustSimState)
    {
        RegistrationState oldRegistrationState
            = joustSimStateToRegistrationState(oldJoustSimState);
        RegistrationState newRegistrationState
            = joustSimStateToRegistrationState(newJoustSimState);

        RegistrationStateChangeEvent event =
            new RegistrationStateChangeEvent(
                this, oldRegistrationState, newRegistrationState);

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
                         + "changed registration status from "
                         + oldState + " to " + newState);

            if (newState == State.ONLINE)
            {
                icbmService = conn.getIcbmService();
                icbmService.addIcbmListener(aimIcbmListener);
            }
            else if (newState == State.DISCONNECTED
                     || newState == State.FAILED)
            {
                logger.debug("The aim Connection was disconnected!");
            }

            //now tell all interested parties about what happened.
            fireRegistrationStateChanged(oldState, newState);

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
     * @return a reference to the <tt>AimConnection</tt> last opened by this provider.
     */
    protected AimConnection getAimConnection()
    {
        return aimConnection;
    }

    public class AimIcbmListener implements IcbmListener
    {

        public void newConversation(IcbmService service, Conversation conv)
        {
            System.out.println("Received a new conversation event");
            conv.addConversationListener(new AimConversationListener());
        }

        public void buddyInfoUpdated(IcbmService service, Screenname buddy,
                                     IcbmBuddyInfo info)
        {
            System.out.println("Got a BuddINFO event");
        }
    }

    public class AimConversationListener implements ConversationListener{
        public void sentOtherEvent(Conversation conversation,
                                   ConversationEventInfo event)
        {
            System.out.println("reveived ConversationEventInfo:" + event);
        }

        // This may be called without ever calling conversationOpened
        public void conversationClosed(Conversation c)
        {
            System.out.println("conversation closed");
        }

        public void gotOtherEvent(Conversation conversation,
                                  ConversationEventInfo event)
        {
            System.out.println("goet other event");
            if(event instanceof TypingInfo)
            {
                TypingInfo ti = (TypingInfo)event;
                System.out.println("got typing info and state is: " + ti.getTypingState());
            }
            else if (event instanceof MessageInfo)
            {
                MessageInfo ti = (MessageInfo)event;
                System.out.println("got message info for msg: " + ti.getMessage());
            }
        }

        public void canSendMessageChanged(Conversation c, boolean canSend)
        {
            System.out.println("can send message event");
        }

        // This may never be called
        public void conversationOpened(Conversation c)
        {
            System.out.println("conversation opened event");
        }

        // This may be called after conversationClosed is called
        public void sentMessage(Conversation c, MessageInfo minfo)
        {
            System.out.println("sent message event");
        }

        // This may be called after conversationClosed is called.
        public void gotMessage(Conversation c, MessageInfo minfo)
        {
            System.out.println("got message event" + minfo.getMessage().getMessageBody());
        }

    }

}
