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
package net.java.sip.communicator.impl.protocol.jabber;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.service.protocol.jabberconstants.*;

import org.jivesoftware.smack.*;
import org.jivesoftware.smack.SmackException.*;
import org.jivesoftware.smack.filter.*;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.chatstates.ChatState;
import org.jivesoftware.smackx.chatstates.packet.ChatStateExtension;
import org.jivesoftware.smackx.xevent.MessageEventManager;
import org.jivesoftware.smackx.xevent.MessageEventNotificationListener;
import org.jivesoftware.smackx.xevent.MessageEventRequestListener;
import org.jxmpp.jid.*;

/**
 * Maps SIP Communicator typing notifications to those going and coming from
 * smack lib.
 *
 * @author Damian Minkov
 * @author Emil Ivov
 * @author Hristo Terezov
 */
public class OperationSetTypingNotificationsJabberImpl
    extends AbstractOperationSetTypingNotifications<ProtocolProviderServiceJabberImpl>
{
    /**
     * The logger.
     */
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(OperationSetTypingNotificationsJabberImpl.class);

    /**
     * An active instance of the opSetPersPresence operation set. We're using
     * it to map incoming events to contacts in our contact list.
     */
    private OperationSetPersistentPresenceJabberImpl opSetPersPresence = null;

    /**
     * An active instance of the opSetBasicIM operation set.
     */
    private OperationSetBasicInstantMessagingJabberImpl opSetBasicIM = null;

    /**
     * We use this listener to cease the moment when the protocol provider
     * has been successfully registered.
     */
    private ProviderRegListener providerRegListener = new ProviderRegListener();

    /**
     * The manger which send us the typing info and through which we send inf
     */
    private MessageEventManager messageEventManager = null;

    /**
     * The listener instance that we use to track chat states according to
     * XEP-0085;
     */
    private SmackChatStateListener smackChatStateListener = null;

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

        //now handle XEP-0085
        try
        {
            sendXep85ChatState((ContactJabberImpl)notifiedContact, typingState);
        }
        catch (NotConnectedException | InterruptedException e)
        {
            throw new IllegalStateException(
                "Failed to send typing notification",
                e
            );
        }
    }

    /**
     * Converts <tt>state</tt> into the corresponding smack <tt>ChatState</tt>
     * and sends it to contact.
     *
     * @param contact the contact that we'd like to send our state to.
     * @param state the state we'd like to sent.
     */
    private void sendXep85ChatState(ContactJabberImpl contact, int state)
        throws NotConnectedException, InterruptedException
    {
        if(opSetBasicIM == null
            || parentProvider.getConnection() == null)
            return;

        Jid toJID = opSetBasicIM.getRecentJIDForAddress(
            contact.getAddressAsJid().asBareJid());

        // find the currently contacted jid to send typing info to him
        // or if we do not have a jid and we have already sent message to the
        // bare jid we will also send typing info there


        // if we haven't sent a message yet, do not send typing notifications
        if(toJID == null)
            return;

        if (logger.isTraceEnabled())
            logger.trace("Sending XEP-0085 chat state=" + state
                + " to " + toJID);


        ChatState chatState;

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

        setCurrentState(chatState, toJID);
    }

    /**
     * Creates and sends a packet for the new chat state.
     * @param chatState the new chat state.
     * @param jid the JID of the receiver.
     */
    private void setCurrentState(ChatState chatState, Jid jid)
        throws NotConnectedException, InterruptedException
    {
        String threadID = opSetBasicIM.getThreadIDForAddress(jid.asBareJid());
        if(threadID == null)
            return;

        Message message = new Message();
        ChatStateExtension extension = new ChatStateExtension(chatState);
        message.addExtension(extension);

        message.setTo(jid);
        message.setType(Message.Type.chat);
        message.setThread(threadID);
        message.setFrom(parentProvider.getConnection().getUser());
        parentProvider.getConnection().sendStanza(message);
    }

    /**
     * Utility method throwing an exception if the stack is not properly
     * initialized.
     *
     * @throws java.lang.IllegalStateException
     *             if the underlying stack is not registered and initialized.
     */
    @Override
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

                opSetBasicIM =
                    (OperationSetBasicInstantMessagingJabberImpl)parentProvider
                        .getOperationSet(
                            OperationSetBasicInstantMessaging.class);

                messageEventManager = MessageEventManager.getInstanceFor(
                    parentProvider.getConnection());

                messageEventManager.addMessageEventRequestListener(
                    new JabberMessageEventRequestListener());
                messageEventManager.addMessageEventNotificationListener(
                    new IncomingMessageEventsListener());


                if(smackChatStateListener == null)
                    smackChatStateListener = new SmackChatStateListener();

                parentProvider.getConnection().addAsyncStanzaListener(
                    smackChatStateListener,
                    new StanzaTypeFilter(Message.class));

            }
            else if(evt.getNewState() == RegistrationState.UNREGISTERED
                 || evt.getNewState() == RegistrationState.AUTHENTICATION_FAILED
                 || evt.getNewState() == RegistrationState.CONNECTION_FAILED)
            {
                if(parentProvider.getConnection() != null)
                {
                    parentProvider.getConnection()
                        .removeAsyncStanzaListener(smackChatStateListener);
                }

                smackChatStateListener = null;
                messageEventManager = null;
            }
        }
    }

    /**
     * Listens for incoming request for typing info
     */
    private class JabberMessageEventRequestListener
        implements MessageEventRequestListener
    {
        @Override
        public void deliveredNotificationRequested(Jid from, String packetID,
            MessageEventManager messageEventManager)
            throws NotConnectedException, InterruptedException
        {
            messageEventManager.sendDeliveredNotification(from, packetID);
        }

        @Override
        public void displayedNotificationRequested(Jid from, String packetID,
                                                   MessageEventManager messageEventManager)
        {
            try
            {
                messageEventManager.sendDisplayedNotification(from, packetID);
            }
            catch (NotConnectedException | InterruptedException e)
            {
                logger.error("Could not send displayed notification", e);
            }
        }

        @Override
        public void composingNotificationRequested(Jid from, String packetID,
            MessageEventManager messageEventManager)
        {
//            if(packetID != null)
//            {
//                String fromID = StringUtils.parseBareAddress(from);
//                packetIDsTable.put(fromID, packetID);
//            }
        }

        @Override
        public void offlineNotificationRequested(Jid from, String packetID,
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
        @Override
        public void deliveredNotification(Jid from, String packetID)
        {
        }

        @Override
        public void displayedNotification(Jid from, String packetID)
        {
        }

        @Override
        public void composingNotification(Jid from, String packetID)
        {
            Contact sourceContact = opSetPersPresence.findContactByID(from.asBareJid());

            if(sourceContact == null)
            {
                //create the volatile contact
                sourceContact = opSetPersPresence.createVolatileContact(from);
            }

            fireTypingNotificationsEvent(sourceContact, STATE_TYPING);
        }

        @Override
        public void offlineNotification(Jid from, String packetID)
        {
        }

        @Override
        public void cancelledNotification(Jid from, String packetID)
        {
            Contact sourceContact = opSetPersPresence.findContactByID(from.asBareJid());

            if(sourceContact == null)
            {
                //create the volatile contact
                sourceContact = opSetPersPresence.createVolatileContact(from);
            }

            fireTypingNotificationsEvent(sourceContact, STATE_STOPPED);
        }
    }


    /**
     * The listener that we use to track chat state notifications according
     * to XEP-0085.
     */
    private class SmackChatStateListener
        implements StanzaListener
    {
        /**
         * Called by smack when the state of a chat changes.
         *
         * @param state the new state of the chat.
         * @param message the message containing the new chat state
         */
        public void stateChanged(ChatState state,
                                 org.jivesoftware.smack.packet.Message message)
        {
            Jid fromJID = message.getFrom();
            if (logger.isTraceEnabled())
                logger.trace(fromJID + " entered the "
                + state.name()+ " state.");


            BareJid fromID = fromJID.asBareJid();

            boolean isPrivateMessagingAddress = false;
            OperationSetMultiUserChat mucOpSet = parentProvider
                .getOperationSet(OperationSetMultiUserChat.class);
            if(mucOpSet != null)
            {
                List<ChatRoom> chatRooms
                    = mucOpSet.getCurrentlyJoinedChatRooms();
                for(ChatRoom chatRoom : chatRooms)
                {
                    if(chatRoom.getName().equals(fromID.toString()))
                    {
                        isPrivateMessagingAddress = true;
                        break;
                    }
                }
            }

            Contact sourceContact = opSetPersPresence.findContactByID(
                (isPrivateMessagingAddress? message.getFrom() : fromID));
            if(sourceContact == null)
            {
                // in private messaging we can receive some errors
                // when we left room (isPrivateMessagingAddress == false)
                // and we try to send some message
                if(message.getError() != null)
                    sourceContact = opSetPersPresence.findContactByID(
                        message.getFrom());

                if(sourceContact == null)
                {
                    //create the volatile contact
                    sourceContact = opSetPersPresence.createVolatileContact(
                        message.getFrom(), isPrivateMessagingAddress);
                }
            }

            int evtCode = STATE_UNKNOWN;

            if (ChatState.composing.equals(state))
            {
                evtCode = STATE_TYPING;
            }
            else if (ChatState.paused.equals(state)
                || ChatState.active.equals(state) )
            {
                evtCode = STATE_PAUSED;
            }
            else if (ChatState.inactive.equals(state)
                || ChatState.gone.equals(state) )
            {
                evtCode = STATE_STOPPED;
            }

            if(message.getError() != null)
                fireTypingNotificationsDeliveryFailedEvent(
                    sourceContact, evtCode);
            else  if(evtCode != STATE_UNKNOWN)
                fireTypingNotificationsEvent(sourceContact, evtCode);
            else
                logger.warn("Unknown typing state!");
        }

        @Override
        public void processStanza(Stanza packet)
        {
            Message msg = (Message) packet;
            ChatStateExtension ext = (ChatStateExtension) msg.getExtension(
                "http://jabber.org/protocol/chatstates");
            if(ext == null)
                return;
            stateChanged(ChatState.valueOf(ext.getElementName()), msg);
        }
    }
}
