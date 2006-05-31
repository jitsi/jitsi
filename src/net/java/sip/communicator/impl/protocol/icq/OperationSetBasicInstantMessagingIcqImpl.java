/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.icq;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.impl.protocol.icq.message.common.*;
import net.java.sip.communicator.impl.protocol.icq.message.offline.*;
import net.java.sip.communicator.impl.protocol.icq.message.imicbm.*;
import java.util.*;
import net.kano.joustsim.*;
import net.kano.joscar.snac.*;
import net.kano.joscar.snaccmd.icbm.*;
import net.kano.joscar.snaccmd.error.*;
import net.kano.joscar.flapcmd.*;
import net.java.sip.communicator.util.*;

//the package net.kano.joustsim.oscar.oscar.service.icbm contains a Message
//class which conflicts with net.java.sip.communicator.service.protocol.Message
//and therefore the following imports must remain explicit.
import net.kano.joustsim.oscar.oscar.service.icbm.IcbmListener;
import net.kano.joustsim.oscar.oscar.service.icbm.IcbmService;
import net.kano.joustsim.oscar.oscar.service.icbm.Conversation;
import net.kano.joustsim.oscar.oscar.service.icbm.IcbmBuddyInfo;
import net.kano.joustsim.oscar.oscar.service.icbm.SimpleMessage;
import net.kano.joustsim.oscar.oscar.service.icbm.MessageInfo;
import net.kano.joustsim.oscar.oscar.service.icbm.ConversationEventInfo;
import net.kano.joustsim.oscar.oscar.service.icbm.ImConversation;
import net.kano.joustsim.oscar.oscar.service.icbm.TypingInfo;
import net.kano.joustsim.oscar.oscar.service.icbm.MissedImInfo;
import net.kano.joustsim.oscar.oscar.service.icbm.ImConversationListener;

/**
 * A straightforward implementation of the basic instant messaging operation
 * set.
 *
 * @author Emil Ivov
 * @author Damian Minkov
 */
public class OperationSetBasicInstantMessagingIcqImpl
    implements OperationSetBasicInstantMessaging
{

    private static final Logger logger =
        Logger.getLogger(OperationSetBasicInstantMessagingIcqImpl.class);

    /**
     * A list of listeneres registered for message events.
     */
    private Vector messageListeners = new Vector();

    /**
     * The icq provider that created us.
     */
    private ProtocolProviderServiceIcqImpl icqProvider = null;

    /**
     * The registration listener that would get notified when the unerlying
     * icq provider gets registered.
     */
    private RegistrationStateListener providerRegListener
        = new RegistrationStateListener();

    /**
     * The listener that would receive instant messaging events from oscar.jar
     */
    private JoustSimIcbmListener joustSimIcbmListener
        = new JoustSimIcbmListener();

    /**
     * The listener that would receive conversation events from oscar.jar
     */
    private JoustSimConversationListener joustSimConversationListener
        = new JoustSimConversationListener();

    /**
     * A reference to the persistent presence operation set that we use
     * to match incoming messages to <tt>Contact</tt>s and vice versa.
     */
    private OperationSetPersistentPresenceIcqImpl opSetPersPresence = null;

    /**
     * Incoming messages has channels.
     * joscar and joustsim handle only channel one and two.
     * This is patch to handle and fourth one.
     */
    private ChannelFourCmdFactory channelFourCmdFactory = new ChannelFourCmdFactory();

    /**
     * Creates an instance of this operation set.
     * @param icqProvider a ref to the <tt>ProtocolProviderServiceIcqImpl</tt>
     * that created us and that we'll use for retrieving the underlying aim
     * connection.
     */
    OperationSetBasicInstantMessagingIcqImpl(
        ProtocolProviderServiceIcqImpl icqProvider)
    {
        this.icqProvider = icqProvider;
        icqProvider.addRegistrationStateChangeListener(providerRegListener);

        channelFourCmdFactory.addCommandHandler
            (IcbmChannelFourCommand.MTYPE_PLAIN, new PlainMessageHandler());
    }

    /**
     * Registeres a MessageListener with this operation set so that it gets
     * notifications of successful message delivery, failure or reception of
     * incoming messages..
     *
     * @param listener the <tt>MessageListener</tt> to register.
     */
    public void addMessageListener(MessageListener listener)
    {
        synchronized(messageListeners)
        {
            this.messageListeners.add(listener);
        }
    }

    /**
     * Unregisteres <tt>listener</tt> so that it won't receive any further
     * notifications upon successful message delivery, failure or reception of
     * incoming messages..
     *
     * @param listener the <tt>MessageListener</tt> to unregister.
     */
    public void removeMessageListener(MessageListener listener)
    {
        synchronized(messageListeners)
        {
            this.messageListeners.remove(listener);
        }
    }

    /**
     * Create a Message instance for sending arbitrary MIME-encoding content.
     *
     * @param content content value
     * @param contentType the MIME-type for <tt>content</tt>
     * @param contentEncoding encoding used for <tt>content</tt>
     * @param subject a <tt>String</tt> subject or <tt>null</tt> for now subject.
     * @return the newly created message.
     */
    public Message createMessage(byte[] content, String contentType,
                                 String contentEncoding, String subject)
    {
        return new MessageIcqImpl(new String(content), contentType
                                  , contentEncoding, subject);
    }

    /**
     * Create a Message instance for sending a simple text messages with
     * default (text/plain) content type and encoding.
     *
     * @param messageText the string content of the message.
     * @return Message the newly created message
     */
    public Message createMessage(String messageText)
    {
        return new MessageIcqImpl(messageText, DEFAULT_MIME_TYPE
                                  , DEFAULT_MIME_ENCODING, null);
    }

    /**
     * Sends the <tt>message</tt> to the destination indicated by the
     * <tt>to</tt> contact.
     *
     * @param to the <tt>Contact</tt> to send <tt>message</tt> to
     * @param message the <tt>Message</tt> to send.
     * @throws java.lang.IllegalStateException if the underlying ICQ stack is
     * not registered and initialized.
     * @throws java.lang.IllegalArgumentException if <tt>to</tt> is not an
     * instance of ContactIcqImpl.
     */
    public void sendInstantMessage(Contact to, Message message)
        throws IllegalStateException, IllegalArgumentException
    {
        assertConnected();

        if (to.getPresenceStatus().isOnline())
        {
            ImConversation imConversation =
                icqProvider.getAimConnection().getIcbmService().
                getImConversation(
                    new Screenname(to.getAddress()));

            //do not add the conversation listener in here. we'll add it
            //inside the icbm listener

            imConversation.sendMessage(new SimpleMessage(message.getContent()));
        }
        else
            icqProvider.getAimConnection().getIcbmService().
                sendSnac(new SendOfflineIm(to.getAddress(), message.getContent()));

        //temporarily and uglity fire the sent event here.
        /** @todo move elsewhaere */
        MessageDeliveredEvent msgDeliveredEvt
            = new MessageDeliveredEvent(
                message, to, new Date());

        fireMessageEvent(msgDeliveredEvt);

    }

    /**
     * Retreives all offline Messages If any.
     * Then delete them from the server.
     *
     * @param listener the <tt>MessageListener</tt> receiving the messages.
     */
    private static int offlineMessageRequestID = 0;
    private void retreiveOfflineMessages()
    {
        OfflineMsgRequest offlineMsgsReq = new OfflineMsgRequest();
        int requestID = offlineMessageRequestID++;
        SnacCommand cmd = new ToIcqCmd(
            Long.parseLong(
                icqProvider.getAimSession().getScreenname().getNormal()),
            offlineMsgsReq.getType(),
            requestID,
            offlineMsgsReq);

        OfflineMessagesRetriever responseRetriever =
            new OfflineMessagesRetriever(requestID);
        SnacRequest snReq = new SnacRequest(cmd, responseRetriever);

        icqProvider.getAimConnection().getInfoService().sendSnacRequest(snReq);
    }

    private class OfflineMessagesRetriever
        extends SnacRequestAdapter
    {
        private int requestID;

        public OfflineMessagesRetriever(int requestID)
        {
            this.requestID = requestID;
        }

        public void handleResponse(SnacResponseEvent e)
        {
            SnacCommand snac = e.getSnacCommand();
            logger.debug("Received a response to our offline message request: " +
                         snac);

            if (snac instanceof OfflineMsgCmd)
            {
                OfflineMsgCmd offlineMsgCmd = (OfflineMsgCmd) snac;
                if (!offlineMsgCmd.isEndOfOfflineMessages())
                {
                    String contactUIN = String.valueOf(offlineMsgCmd.getUin());
                    Contact sourceContact =
                        opSetPersPresence.findContactByID(contactUIN);
                    if (sourceContact == null)
                    {
                        logger.debug(
                            "received a message from a unknown contact: "
                            + contactUIN);
                        //create the volatile contact
                        sourceContact = opSetPersPresence
                            .createVolatileContact(contactUIN);
                    }

                    MessageReceivedEvent msgReceivedEvt
                        = new MessageReceivedEvent(
                            createMessage(offlineMsgCmd.getContents()),
                            sourceContact,
                            offlineMsgCmd.getDate());
                    logger.debug("fire msg received for : " +
                                 offlineMsgCmd.getContents());
                    fireMessageEvent(msgReceivedEvt);
                }
                else
                {
                    logger.debug("send ack to delete offline messages");

                    OfflineMsgDeleteRequest offlineMsgDeleteReq = new
                        OfflineMsgDeleteRequest();
                    SnacCommand cmd = new ToIcqCmd(
                        Long.parseLong(
                            icqProvider.getAimSession().getScreenname().
                            getNormal()),
                        offlineMsgDeleteReq.getType(),
                        requestID,
                        offlineMsgDeleteReq);

                    icqProvider.getAimConnection().getInfoService().sendSnac(
                        cmd);
                }
            }
            else
            if (snac instanceof SnacError)
            {
                logger.debug("error receiving offline messages");
            }
        }
    }


    /**
     * Utility method throwing an exception if the icq stack is not properly
     * initialized.
     * @throws java.lang.IllegalStateException if the underlying ICQ stack is
     * not registered and initialized.
     */
    private void assertConnected() throws IllegalStateException
    {
        if (icqProvider == null)
            throw new IllegalStateException(
                "The icq provider must be non-null and signed on the ICQ "
                +"service before being able to communicate.");
        if (!icqProvider.isRegistered())
            throw new IllegalStateException(
                "The icq provider must be signed on the ICQ service before "
                +"being able to communicate.");
    }

    /**
     * This method is used to provide the factory instance so
     * othe operations can add their handlers in it, for certain commands.
     *
     * @return ChannelFourCmdFactory the factory instance
     */
    public ChannelFourCmdFactory getChannelFourFactory()
    {
        return channelFourCmdFactory;
    }

    /**
     * Fix of issue 142
     * Some clients send messages through channel 4
     */
    private class PlainMessageHandler
        implements ChFourPacketHandler
    {
        public SnacCommand handle(IcbmChannelFourCommand cmd)
        {
            return new RecvImIcbm(
                        cmd.getRequestID(),
                        cmd.getUserInfo(),
                        new InstantMessage(cmd.getReason()),
                        cmd.isAutoResponse(),
                        cmd.senderWantsIcon(),
                        cmd.getIconInfo(),
                        null,
                        cmd.getFeaturesBlock(),
                        true);
        }

    }

    /**
     * Our listener that will tell us when we're registered to icq and joust
     * sim is ready to accept us as a listener.
     */
    private class RegistrationStateListener
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
            logger.debug("The ICQ provider changed state from: "
                         + evt.getOldState()
                         + " to: " + evt.getNewState());
            if (evt.getNewState() == RegistrationState.REGISTERED)
            {
                System.out.println("adding a Bos Service Listener");
                icqProvider.getAimConnection().getIcbmService()
                    .addIcbmListener(joustSimIcbmListener);

                opSetPersPresence = (OperationSetPersistentPresenceIcqImpl)
                    icqProvider.getSupportedOperationSets()
                        .get(OperationSetPersistentPresence.class.getName());

                // registers the factory handling channel 4 messages
                icqProvider.getAimConnection().getIcbmService().
                    getOscarConnection().getSnacProcessor().getCmdFactoryMgr().
                    getDefaultFactoryList().registerAll(channelFourCmdFactory);

                retreiveOfflineMessages();
            }
        }
    }

    /**
     * Delivers the specified event to all registered message listeners.
     * @param evt the <tt>EventObject</tt> that we'd like delivered to all
     * registered message listerners.
     */
    private void fireMessageEvent(EventObject evt)
    {
        synchronized(messageListeners)
        {
            for (int i = 0; i < messageListeners.size(); i++)
            {
                MessageListener l = (MessageListener)messageListeners.get(i);

                if (evt instanceof MessageDeliveredEvent )
                {
                    l.messageDelivered((MessageDeliveredEvent)evt);
                }
                else if (evt instanceof MessageReceivedEvent)
                {
                    l.messageReceived((MessageReceivedEvent) evt);
                }
                else if (evt instanceof MessageDeliveryFailedEvent)
                {
                    l.messageDeliveryFailed((MessageDeliveryFailedEvent) evt);
                }
            }
        }
    }

    /**
     * The listener that would retrieve instant messaging events from oscar.jar.
     */
    private class JoustSimIcbmListener implements IcbmListener
    {
        /**
         * Register our icbm listener so that we get notified when new
         * conversations are cretaed and register ourselvers as listeners in
         * them.
         *
         * @param service the <tt>IcbmService</tt> that is clling us
         * @param conv the <tt>Conversation</tt> that has just been created.
         */
        public void newConversation(IcbmService service, Conversation conv)
        {
            conv.addConversationListener(joustSimConversationListener);
        }

        /**
         * Currently Unused.
         * @param service Currently Unused.
         * @param buddy Currently Unused.
         * @param info Currently Unused.
         */
        public void buddyInfoUpdated(IcbmService service, Screenname buddy,
                                     IcbmBuddyInfo info)
        {
            System.out.println("buddy info pudated for " + buddy
                                + " new info is: " + info);
        }

    }

    /**
     * Joust SIM supports the notion of instant messaging conversations and
     * all message events are delivered through this listener. Right now we
     * won't burden ourselves with conversations and would simply deliver
     * events as we get them. If we need conversation support we'll implement it
     * some other day.
     */
    private class JoustSimConversationListener implements ImConversationListener
    {
        /**
         * Create a corresponding message object and fire a
         * <tt>MessageReceivedEvent</tt>.
         *
         * @param conversation the conversation where the message is received in.
         * @param minfo informtion about the received message
         */
        public void gotMessage(Conversation conversation, MessageInfo minfo)
        {
            if(logger.isDebugEnabled())
                logger.debug("Received from "
                             + conversation.getBuddy()
                             + " the message "
                             + minfo.getMessage().getMessageBody());

            Message newMessage = createMessage(
                minfo.getMessage().getMessageBody());

            Contact sourceContact =
                opSetPersPresence.findContactByID( conversation.getBuddy()
                                                             .getFormatted());
            if(sourceContact == null)
            {
                logger.debug("received a message from a unknown contact: "
                                   + conversation.getBuddy());
                //create the volatile contact
                sourceContact = opSetPersPresence
                    .createVolatileContact(
                        conversation.getBuddy().getFormatted());

            }

            MessageReceivedEvent msgReceivedEvt
                = new MessageReceivedEvent(
                    newMessage, sourceContact , minfo.getTimestamp() );

            fireMessageEvent(msgReceivedEvt);
        }

        public void sentOtherEvent(Conversation conversation,
                                   ConversationEventInfo event)
        {
            /**@todo implement sentOtherEvent() */
            System.out.println("@todo implement sentOtherEvent()");
        }

        public void canSendMessageChanged(Conversation c, boolean canSend)
        {
            /**@todo implement canSendMessageChanged() */
            System.out.println("@todo implement canSendMessageChanged()");
        }

        public void conversationClosed(Conversation c)
        {
            /**@todo implement conversationClosed() */
            System.out.println("@todo implement conversationClosed()");
        }

        public void conversationOpened(Conversation c)
        {
            /**@todo implement conversationOpened() */
            System.out.println("@todo implement conversationOpened()");
        }

        public void gotOtherEvent(Conversation conversation,
                                  ConversationEventInfo event)
        {
            /**@todo implement gotOtherEvent() */
            System.out.println("@todo implement gotOtherEvent()");
        }

        public void sentMessage(Conversation c, MessageInfo minfo)
        {
            /**@todo implement sentMessage() */
            /**
             * there's no id in this event and besides we have no message failed
             * method so refiring an event here would be difficult.
             *
             * we'll deal with that some other day.
             */
            System.out.println("@todo implement sentMessage()");
        }

        public void missedMessages(ImConversation conv, MissedImInfo info)
        {
            /**@todo implement missedMessages() */
            System.out.println("@todo implement missedMessages()");
        }

        public void gotTypingState(Conversation conversation,
                                   TypingInfo typingInfo)
        {
            //typing events are handled in OperationSetTypingNotifications
        }

    }
}
