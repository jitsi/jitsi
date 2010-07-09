/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.service.protocol.jabberconstants.*;
import net.java.sip.communicator.util.*;

import org.jivesoftware.smack.*;
import org.jivesoftware.smack.util.*;
import org.jivesoftware.smackx.*;

/**
 * Maps SIP Communicator typing notifications to those going and coming from
 * smack lib.
 *
 * @author Damian Minkov
 * @author Emil Ivov
 */
public class OperationSetTypingNotificationsJabberImpl
    extends AbstractOperationSetTypingNotifications<ProtocolProviderServiceJabberImpl>
{
    /**
     * The logger.
     */
    private static final Logger logger =
        Logger.getLogger(OperationSetTypingNotificationsJabberImpl.class);

    /**
     * An active instance of the opSetPersPresence operation set. We're using
     * it to map incoming events to contacts in our contact list.
     */
    private OperationSetPersistentPresenceJabberImpl opSetPersPresence = null;

    /**
     * We use this listener to cease the moment when the protocol provider
     * has been successfully registered.
     */
    private ProviderRegListener providerRegListener = new ProviderRegListener();

    /**
     * The manger which send us the typing info and through which we send inf
     */
    private MessageEventManager messageEventManager = null;

    private final Map<String, String> packetIDsTable
        = new Hashtable<String, String>();

    /**
     * The listener instance that we use to track chat states according to
     * XEP-0085;
     */
    private SmackChatStateListener smackChatStateListener = null;

    /**
     * The listener instance that we use in order to track for new chats so that
     * we could stick a SmackChatStateListener to them.
     */
    private SmackChatManagerListener smackChatManagerListener = null;

    /**
     * @param provider a ref to the <tt>ProtocolProviderServiceImpl</tt>
     * that created us and that we'll use for retrieving the underlying aim
     * connection.
     */
    OperationSetTypingNotificationsJabberImpl(
        ProtocolProviderServiceJabberImpl provider)
    {
        super(provider);

        provider.addRegistrationStateChangeListener(providerRegListener);
    }

    /**
     * Sends a notification to <tt>notifiedContatct</tt> that we have entered
     * <tt>typingState</tt>.
     *
     * @param notifiedContact the <tt>Contact</tt> to notify
     * @param typingState the typing state that we have entered.
     *
     * @throws java.lang.IllegalStateException if the underlying stack is
     * not registered and initialized.
     * @throws java.lang.IllegalArgumentException if <tt>notifiedContact</tt> is
     * not an instance belonging to the underlying implementation.
     */
    public void sendTypingNotification(Contact notifiedContact, int typingState)
        throws IllegalStateException, IllegalArgumentException
    {
        assertConnected();

        if( !(notifiedContact instanceof ContactJabberImpl) )
           throw new IllegalArgumentException(
               "The specified contact is not a Jabber contact."
               + notifiedContact);

        /**
         * Emil Ivov: We used to use this in while we were still using XEP-0022
         * to send typing notifications. I am commenting it out today on
         * 2008-08-20 as we now also support XEP-0085 (see below) and using both
         * mechanisms sends double notifications which, apart from simply being
         * redundant, is also causing the jabber slick to fail.
         *
        String packetID =
            (String)packetIDsTable.get(notifiedContact.getAddress());

        //First do XEP-0022 notifications
        if(packetID != null)
        {
            if(typingState == STATE_TYPING)
            {
                messageEventManager.
                    sendComposingNotification(notifiedContact.getAddress(),
                                              packetID);
            }
            else if(typingState == STATE_STOPPED)
            {
                messageEventManager.
                    sendCancelledNotification(notifiedContact.getAddress(),
                                              packetID);
                packetIDsTable.remove(notifiedContact.getAddress());
            }
        }
        */

        //now handle XEP-0085
        sendXep85ChatState(notifiedContact, typingState);
    }

    /**
     * Converts <tt>state</tt> into the corresponding smack <tt>ChatState</tt>
     * and sends it to contact.
     *
     * @param contact the contact that we'd like to send our state to.
     * @param state the state we'd like to sent.
     */
    private void sendXep85ChatState(Contact contact, int state)
    {
        if (logger.isTraceEnabled())
            logger.trace("Sending XEP-0085 chat state=" + state
            + " to " + contact.getAddress());

        Chat chat = parentProvider.getConnection()
            .getChatManager().createChat(contact.getAddress(), null);

        ChatState chatState = null;


        if(state == STATE_TYPING)
        {
            chatState = ChatState.composing;
        }
        else if(state == STATE_STOPPED)
        {
            chatState = ChatState.inactive;
        }
        else if(state == STATE_PAUSED)
        {
            chatState = ChatState.paused;
        }
        else
        {
            chatState = ChatState.gone;
        }

        try
        {
            ChatStateManager.getInstance(parentProvider.getConnection())
                .setCurrentState(chatState, chat);
        }
        catch(XMPPException exc)
        {
            //we don't want to bother the user with network exceptions
            //so let's simply log it.
            logger.warn("Failed to send state [" + state + "] to ["
                + contact.getAddress() + "].", exc);
        }
    }

    /**
     * Utility method throwing an exception if the stack is not properly
     * initialized.
     *
     * @throws java.lang.IllegalStateException
     *             if the underlying stack is not registered and initialized.
     */
    protected void assertConnected()
        throws IllegalStateException
    {
        if(parentProvider != null && !parentProvider.isRegistered()
            && opSetPersPresence.getPresenceStatus().isOnline())
        {
            // if we are not registered but the current status is online
            // change the current status
            opSetPersPresence.fireProviderStatusChangeEvent(
                    opSetPersPresence.getPresenceStatus(),
                    parentProvider.getJabberStatusEnum().getStatus(
                        JabberStatusEnum.OFFLINE));
        }

        super.assertConnected();
    }

    /**
     * Our listener that will tell us when we're registered and
     * ready to accept us as a listener.
     */
    private class ProviderRegListener
        implements RegistrationStateChangeListener
    {
        /**
         * The method is called by a ProtocolProvider implementation whenver
         * a change in the registration state of the corresponding provider had
         * occurred.
         * @param evt ProviderStatusChangeEvent the event describing the status
         * change.
         */
        public void registrationStateChanged(RegistrationStateChangeEvent evt)
        {
            if (logger.isDebugEnabled())
                logger.debug("The provider changed state from: "
                         + evt.getOldState()
                         + " to: " + evt.getNewState());
            if (evt.getNewState() == RegistrationState.REGISTERED)
            {
                opSetPersPresence =
                    (OperationSetPersistentPresenceJabberImpl) parentProvider
                        .getOperationSet(OperationSetPersistentPresence.class);

                messageEventManager =
                    new MessageEventManager(parentProvider.getConnection());

                messageEventManager.addMessageEventRequestListener(
                    new JabberMessageEventRequestListener());
                messageEventManager.addMessageEventNotificationListener(
                    new IncomingMessageEventsListener());

                //according to the smack api documentation we need to do this
                //every time we connect in order to reinitialize the chat state
                //manager (@see http://tinyurl.com/6j9uqs )

                ChatStateManager.getInstance(parentProvider.getConnection());

                if(smackChatManagerListener == null)
                    smackChatManagerListener = new SmackChatManagerListener();

                parentProvider.getConnection().getChatManager()
                    .addChatListener(smackChatManagerListener);
            }
            else if(evt.getNewState() == RegistrationState.UNREGISTERED
                 || evt.getNewState() == RegistrationState.AUTHENTICATION_FAILED
                 || evt.getNewState() == RegistrationState.CONNECTION_FAILED)
            {
                if(parentProvider.getConnection() != null
                    && parentProvider.getConnection().getChatManager() != null)
                {
                    parentProvider.getConnection().getChatManager()
                        .removeChatListener(smackChatManagerListener);
                }

                smackChatManagerListener = null;

                if(messageEventManager != null)
                {
                    messageEventManager.destroy();
                    messageEventManager = null;
                }
            }
        }
    }

    /**
     * The class that we use when listening for new chats so that we could start
     * tracking them for chat events.
     */
    private class SmackChatManagerListener implements ChatManagerListener
    {
        /**
         * Simply adds a chat state listener to every newly created chat
         * so that we could track it for chat state events.
         *
         * @param chat the chat that we need to add a state listener to.
         * @param isLocal indicates whether the chat has been initiated by us
         */
        public void chatCreated(Chat chat, boolean isLocal)
        {
            if (logger.isTraceEnabled())
                logger.trace("Created a chat with "
                + chat.getParticipant() + " local="+isLocal);

            if(smackChatStateListener == null)
                smackChatStateListener = new SmackChatStateListener();

            chat.addMessageListener(smackChatStateListener);
        };
    }

    /**
     * Listens for incoming request for typing info
     */
    private class JabberMessageEventRequestListener
        implements MessageEventRequestListener
    {
        public void deliveredNotificationRequested(String from, String packetID,
            MessageEventManager messageEventManager)
        {
            messageEventManager.sendDeliveredNotification(from, packetID);
        }

        public void displayedNotificationRequested(String from, String packetID,
            MessageEventManager messageEventManager)
        {
            messageEventManager.sendDisplayedNotification(from, packetID);
        }

        public void composingNotificationRequested(String from, String packetID,
            MessageEventManager messageEventManager)
        {
            if(packetID != null)
            {
                String fromID = StringUtils.parseBareAddress(from);
                packetIDsTable.put(fromID, packetID);
            }
        }

        public void offlineNotificationRequested(String from, String packetID,
                                                 MessageEventManager
                                                 messageEventManager)
        {}
    }

    /**
     * Receives incoming typing info
     */
    private class IncomingMessageEventsListener
        implements MessageEventNotificationListener
    {
        public void deliveredNotification(String from, String packetID)
        {
        }

        public void displayedNotification(String from, String packetID)
        {
        }

        public void composingNotification(String from, String packetID)
        {
            String fromID = StringUtils.parseBareAddress(from);

            Contact sourceContact = opSetPersPresence.findContactByID(fromID);

            if(sourceContact == null)
            {
                //create the volatile contact
                sourceContact = opSetPersPresence.createVolatileContact(fromID);
            }

            fireTypingNotificationsEvent(sourceContact, STATE_TYPING);
        }

        public void offlineNotification(String from, String packetID)
        {
        }

        public void cancelledNotification(String from, String packetID)
        {
            String fromID = StringUtils.parseBareAddress(from);
            Contact sourceContact = opSetPersPresence.findContactByID(fromID);

            if(sourceContact == null)
            {
                //create the volatile contact
                sourceContact = opSetPersPresence.createVolatileContact(fromID);
            }

            fireTypingNotificationsEvent(sourceContact, STATE_STOPPED);
        }
    }


    /**
     * The listener that we use to track chat state notifications according
     * to XEP-0085.
     */
    private class SmackChatStateListener implements ChatStateListener
    {
        /**
         * Called by smack when the state of a chat changes.
         *
         * @param chat the chat that is concerned by this event.
         * @param state the new state of the chat.
         */
        public void stateChanged(Chat chat, ChatState state)
        {
            if (logger.isTraceEnabled())
                logger.trace(chat.getParticipant() + " entered the "
                + state.name()+ " state.");

            String fromID = StringUtils.parseBareAddress(chat.getParticipant());
            Contact sourceContact = opSetPersPresence.findContactByID(fromID);

            if(sourceContact == null)
            {
                //create the volatile contact
                sourceContact = opSetPersPresence.createVolatileContact(fromID);
            }

            if (ChatState.composing.equals(state))
            {
                fireTypingNotificationsEvent(sourceContact, STATE_TYPING);
            }
            else if (ChatState.paused.equals(state)
                || ChatState.active.equals(state) )
            {
                fireTypingNotificationsEvent(sourceContact, STATE_PAUSED);
            }
            else if (ChatState.inactive.equals(state)
                || ChatState.gone.equals(state) )
            {
                fireTypingNotificationsEvent(sourceContact, STATE_STOPPED);
            }
        }

        /**
         * Called when a new message is received. We ignore this one since
         * we handle message reception on a lower level.
         *
         * @param chat the chat that the message belongs to
         * @param msg the message that we need to process.
         */
        public void processMessage(Chat chat,
                                    org.jivesoftware.smack.packet.Message msg)
        {
            if (logger.isTraceEnabled())
                logger.trace("ignoring a process message");

        }
    }
}
