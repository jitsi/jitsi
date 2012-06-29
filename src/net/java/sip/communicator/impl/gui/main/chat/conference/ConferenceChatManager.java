/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chat.conference;

import java.util.*;
import java.util.concurrent.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.main.chat.*;
import net.java.sip.communicator.impl.gui.main.chat.history.*;
import net.java.sip.communicator.impl.gui.main.chatroomslist.*;
import net.java.sip.communicator.impl.gui.main.chatroomslist.joinforms.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.impl.gui.utils.Constants;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

import org.jdesktop.swingworker.*;
import org.jitsi.service.resources.*;
import org.osgi.framework.*;
// Java 1.6 has javax.swing.SwingWorker so we have to disambiguate.

/**
 * The <tt>ConferenceChatManager</tt> is the one that manages both chat room and
 * ad-hoc chat rooms invitations.
 *
 * @author Yana Stamcheva
 * @author Lubomir Marinov
 * @author Valentin Martinet
 */
public class ConferenceChatManager
    implements  ChatRoomMessageListener,
                ChatRoomInvitationListener,
                ChatRoomInvitationRejectionListener,
                AdHocChatRoomMessageListener,
                AdHocChatRoomInvitationListener,
                AdHocChatRoomInvitationRejectionListener,
                LocalUserChatRoomPresenceListener,
                LocalUserAdHocChatRoomPresenceListener,
                ServiceListener
{
    /**
     * The object used for logging.
     */
    private static final Logger logger
        = Logger.getLogger(ConferenceChatManager.class);

    /**
     * Maps each history window to a <tt>ChatRoomWrapper</tt>.
     */
    private final Hashtable<ChatRoomWrapper, HistoryWindow> chatRoomHistory =
        new Hashtable<ChatRoomWrapper, HistoryWindow>();

    /**
     * The list of persistent chat rooms.
     */
    private final ChatRoomList chatRoomList = new ChatRoomList();

    /**
     * The list of ad-hoc chat rooms.
     */
    private final AdHocChatRoomList adHocChatRoomList = new AdHocChatRoomList();

    /**
     * A list of all <tt>ChatRoomListChangeListener</tt>-s.
     */
    private final Vector<ChatRoomListChangeListener> listChangeListeners
        = new Vector<ChatRoomListChangeListener>();

    /**
     * A list of all <tt>AdHocChatRoomListChangeListener</tt>-s.
     */
    private final Vector<AdHocChatRoomListChangeListener>
        adHoclistChangeListeners = new Vector<AdHocChatRoomListChangeListener>();

    /**
     * Creates an instance of <tt>ConferenceChatManager</tt>.
     */
    public ConferenceChatManager()
    {
        // Loads the chat rooms list in a separate thread.
        new Thread()
        {
            public void run()
            {
                chatRoomList.loadList();
                adHocChatRoomList.loadList();
            }
        }.start();

        GuiActivator.bundleContext.addServiceListener(this);
    }

    /**
     * Returns all chat room providers currently contained in the chat room
     * list.
     * @return  all chat room providers currently contained in the chat room
     * list.
     */
    public ChatRoomList getChatRoomList()
    {
        return chatRoomList;
    }

    /**
     * Returns all chat room providers currently contained in the ad-hoc chat 
     * room list.
     * 
     * @return  all chat room providers currently contained in the ad-hoc chat 
     * room list.
     */
    public AdHocChatRoomList getAdHocChatRoomList()
    {
        return adHocChatRoomList;
    }

    /**
     * Handles <tt>ChatRoomInvitationReceivedEvent</tt>-s.
     */
    public void invitationReceived(ChatRoomInvitationReceivedEvent evt)
    {
        InvitationReceivedDialog dialog
            = new InvitationReceivedDialog(
                    this,
                    evt.getSourceOperationSet(),
                    evt.getInvitation());

        dialog.setVisible(true);
    }

    public void invitationRejected(ChatRoomInvitationRejectedEvent evt) {}

    /**
     * Implements the <tt>ChatRoomMessageListener.messageDelivered</tt> method.
     * <br>
     * Shows the message in the conversation area and clears the write message
     * area.
     * @param evt the <tt>ChatRoomMessageDeliveredEvent</tt> that notified us
     * that the message was delivered to its destination
     */
    public void messageDelivered(ChatRoomMessageDeliveredEvent evt)
    {
        ChatRoom sourceChatRoom = (ChatRoom) evt.getSource();

        if (logger.isTraceEnabled())
            logger.trace(
                "MESSAGE DELIVERED to chat room: " + sourceChatRoom.getName());

        ChatPanel chatPanel = GuiActivator.getUIService().getChatWindowManager()
            .getMultiChat(sourceChatRoom, false);

        if(chatPanel != null)
        {
            String messageType;

            switch (evt.getEventType())
            {
            case ChatRoomMessageDeliveredEvent.CONVERSATION_MESSAGE_DELIVERED:
                messageType = Chat.OUTGOING_MESSAGE;
                break;
            case ChatRoomMessageDeliveredEvent.ACTION_MESSAGE_DELIVERED:
                messageType = Chat.ACTION_MESSAGE;
                break;
            default:
                messageType = null;
                break;
            }

            Message msg = evt.getMessage();

            chatPanel.addMessage(
                sourceChatRoom.getUserNickname(),
                evt.getTimestamp(),
                messageType,
                msg.getContent(),
                msg.getContentType());
        }
    }

    /**
     * Implements the <tt>ChatRoomMessageListener.messageReceived</tt> method.
     * <br>
     * Obtains the corresponding <tt>ChatPanel</tt> and process the message
     * there.
     * @param evt the <tt>ChatRoomMessageReceivedEvent</tt> that notified us
     * that a message has been received
     */
    public void messageReceived(ChatRoomMessageReceivedEvent evt)
    {
        ChatRoom sourceChatRoom = evt.getSourceChatRoom();
        ChatRoomMember sourceMember = evt.getSourceChatRoomMember();

        String messageType = null;

        switch (evt.getEventType())
        {
        case ChatRoomMessageReceivedEvent.CONVERSATION_MESSAGE_RECEIVED:
            messageType = Chat.INCOMING_MESSAGE;
            break;
        case ChatRoomMessageReceivedEvent.SYSTEM_MESSAGE_RECEIVED:
            messageType = Chat.SYSTEM_MESSAGE;
            break;
        case ChatRoomMessageReceivedEvent.ACTION_MESSAGE_RECEIVED:
            messageType = Chat.ACTION_MESSAGE;
            break;
        }

        if (logger.isTraceEnabled())
            logger.trace("MESSAGE RECEIVED from contact: "
            + sourceMember.getContactAddress());

        Message message = evt.getMessage();

        ChatPanel chatPanel = null;

        ChatWindowManager chatWindowManager
            = GuiActivator.getUIService().getChatWindowManager();

        if(sourceChatRoom.isSystem())
        {
            ChatRoomProviderWrapper serverWrapper
                = chatRoomList.findServerWrapperFromProvider(
                    sourceChatRoom.getParentProvider());

            chatPanel = chatWindowManager.getMultiChat(
                serverWrapper.getSystemRoomWrapper(), true);
        }
        else
        {
            chatPanel = chatWindowManager.getMultiChat(
                sourceChatRoom, true, message.getMessageUID());
        }

        String messageContent = message.getContent();

        if (evt.isHistoryMessage())
        {
            long timeStamp = chatPanel.getChatConversationPanel()
                .getLastIncomingMsgTimestamp();
            Collection<Object> c =
                chatPanel.getChatSession().getHistoryBeforeDate(
                    new Date(timeStamp == 0 ? System.currentTimeMillis() - 10000 : timeStamp), 20);
            if (c.size() > 0)
            {
                boolean isPresent = false;
                for (Object o : c)
                {
                    if (o instanceof ChatRoomMessageDeliveredEvent)
                    {
                        ChatRoomMessageDeliveredEvent ev =
                            (ChatRoomMessageDeliveredEvent) o;
                        if (evt.getTimestamp() == ev.getTimestamp())
                        {
                            isPresent = true;
                            break;
                        }
                    }
                    else if(o instanceof ChatRoomMessageReceivedEvent)
                    {
                        ChatRoomMessageReceivedEvent ev = 
                            (ChatRoomMessageReceivedEvent) o;
                        if (evt.getTimestamp() == ev.getTimestamp())
                        {
                            isPresent = true;
                            break;
                        }
                    }
                }

                if (isPresent)
                    return;
            }
        }

        chatPanel.addMessage(
            sourceMember.getName(),
            evt.getTimestamp(),
            messageType,
            messageContent,
            message.getContentType());

        chatWindowManager.openChat(chatPanel, false);
    }

    /**
     * Determines whether a specific <code>ChatRoom</code> is private i.e.
     * represents a one-to-one conversation which is not a channel. Since the
     * interface {@link ChatRoom} does not expose the private property, an
     * heuristic is used as a workaround: (1) a system <code>ChatRoom</code> is
     * obviously not private and (2) a <code>ChatRoom</code> is private if it
     * has only one <code>ChatRoomMember</code> who is not the local user.
     * 
     * @param chatRoom
     *            the <code>ChatRoom</code> to be determined as private or not
     * @return <tt>true</tt> if the specified <code>ChatRoom</code> is private;
     *         otherwise, <tt>false</tt>
     */
    static boolean isPrivate(ChatRoom chatRoom)
    {
        if (!chatRoom.isSystem()
            && chatRoom.isJoined()
            && (chatRoom.getMembersCount() == 1))
        {
            String nickname = chatRoom.getUserNickname();

            if (nickname != null)
            {
                for (ChatRoomMember member : chatRoom.getMembers())
                    if (nickname.equals(member.getName()))
                        return false;
                return true;
            }
        }
        return false;
    }

    /**
     * Implements the <tt>ChatRoomMessageListener.messageDeliveryFailed</tt>
     * method.
     * <br>
     * In the conversation area shows an error message, explaining the problem.
     * @param evt the <tt>ChatRoomMessageDeliveryFailedEvent</tt> that notified
     * us of a delivery failure
     */
    public void messageDeliveryFailed(ChatRoomMessageDeliveryFailedEvent evt)
    {
        ChatRoom sourceChatRoom = evt.getSourceChatRoom();

        String errorMsg = null;

        /*
         * FIXME ChatRoomMessageDeliveryFailedEvent#getSource() is not a Message
         * instance at the time of this writing and the attempt "(Message)
         * evt.getSource()" seems to be to get the message which failed to be
         * delivered. I'm not sure it's
         * ChatRoomMessageDeliveryFailedEvent#getMessage() but since it's the
         * only message I can get out of ChatRoomMessageDeliveryFailedEvent, I'm
         * using it.
         */
        Message sourceMessage = evt.getMessage();

        ChatRoomMember destMember = evt.getDestinationChatRoomMember();

        if (evt.getErrorCode()
            == MessageDeliveryFailedEvent.OFFLINE_MESSAGES_NOT_SUPPORTED)
        {
            errorMsg = GuiActivator.getResources().getI18NString(
                "service.gui.MSG_DELIVERY_NOT_SUPPORTED",
                new String[]{destMember.getName()});
        }
        else if (evt.getErrorCode()
            == MessageDeliveryFailedEvent.NETWORK_FAILURE)
        {
            errorMsg = GuiActivator.getResources()
                .getI18NString("service.gui.MSG_NOT_DELIVERED");
        }
        else if (evt.getErrorCode()
            == MessageDeliveryFailedEvent.PROVIDER_NOT_REGISTERED)
        {
            errorMsg = GuiActivator.getResources().getI18NString(
                "service.gui.MSG_SEND_CONNECTION_PROBLEM");
        }
        else if (evt.getErrorCode()
            == MessageDeliveryFailedEvent.INTERNAL_ERROR)
        {
            errorMsg = GuiActivator.getResources().getI18NString(
                "service.gui.MSG_DELIVERY_INTERNAL_ERROR");
        }
        else
        {
            errorMsg = GuiActivator.getResources().getI18NString(
                 "service.gui.MSG_DELIVERY_UNKNOWN_ERROR");
        }

        String reason = evt.getReason();
        if (reason != null)
            errorMsg += " " + GuiActivator.getResources().getI18NString(
                "service.gui.ERROR_WAS",
                new String[]{reason});

        ChatWindowManager chatWindowManager
            = GuiActivator.getUIService().getChatWindowManager();
        ChatPanel chatPanel
            = chatWindowManager.getMultiChat(sourceChatRoom, true);

        chatPanel.addMessage(
            destMember.getName(),
            System.currentTimeMillis(),
            Chat.OUTGOING_MESSAGE,
            sourceMessage.getContent(),
            sourceMessage.getContentType());

        chatPanel.addErrorMessage(
            destMember.getName(),
            errorMsg);

        chatWindowManager.openChat(chatPanel, false);
    }

    /**
     * Implements the
     * <tt>LocalUserAdHocChatRoomPresenceListener.localUserPresenceChanged</tt>
     * method
     * 
     * @param evt the <tt>LocalUserAdHocChatRoomPresenceChangeEvent</tt> that
     * notified us of a presence change
     */
    public void localUserAdHocPresenceChanged(
            LocalUserAdHocChatRoomPresenceChangeEvent evt)
    {
        AdHocChatRoom sourceAdHocChatRoom = evt.getAdHocChatRoom();
        AdHocChatRoomWrapper adHocChatRoomWrapper
            = adHocChatRoomList
                .findChatRoomWrapperFromAdHocChatRoom(sourceAdHocChatRoom);

        String eventType = evt.getEventType();

        if (LocalUserAdHocChatRoomPresenceChangeEvent
                .LOCAL_USER_JOINED.equals(eventType))
        {
            if(adHocChatRoomWrapper != null)
            {
                this.fireAdHocChatRoomListChangedEvent(
                        adHocChatRoomWrapper,
                        AdHocChatRoomListChangeEvent.AD_HOC_CHAT_ROOM_CHANGED);

                ChatWindowManager chatWindowManager
                    = GuiActivator.getUIService().getChatWindowManager();
                ChatPanel chatPanel
                    = chatWindowManager
                        .getMultiChat(adHocChatRoomWrapper, true);

                // Check if we have already opened a chat window for this chat
                // wrapper and load the real chat room corresponding to the
                // wrapper.
                if(chatPanel.isShown())
                    ((AdHocConferenceChatSession) chatPanel.getChatSession())
                        .loadChatRoom(sourceAdHocChatRoom);
                else
                    chatWindowManager.openChat(chatPanel, true);
            }

            sourceAdHocChatRoom.addMessageListener(this);
        }
        else if (evt.getEventType().equals(
            LocalUserAdHocChatRoomPresenceChangeEvent.LOCAL_USER_JOIN_FAILED))
        {
            new ErrorDialog(
                GuiActivator.getUIService().getMainFrame(),
                GuiActivator.getResources().getI18NString("service.gui.ERROR"),
                GuiActivator.getResources().getI18NString(
                        "service.gui.FAILED_TO_JOIN_CHAT_ROOM",
                        new String[]{sourceAdHocChatRoom.getName()}) 
                        + evt.getReason())
            .showDialog();
        }
        else if (LocalUserAdHocChatRoomPresenceChangeEvent
                        .LOCAL_USER_LEFT.equals(eventType)
                    || LocalUserAdHocChatRoomPresenceChangeEvent
                            .LOCAL_USER_DROPPED.equals(eventType))
        {
            this.closeAdHocChatRoom(adHocChatRoomWrapper);

            // Need to refresh the chat room's list in order to change
            // the state of the chat room to offline.
            fireAdHocChatRoomListChangedEvent(
                    adHocChatRoomWrapper,
                    AdHocChatRoomListChangeEvent.AD_HOC_CHAT_ROOM_CHANGED);

            sourceAdHocChatRoom.removeMessageListener(this);
        }
    }

    /**
     * Implements the
     * <tt>LocalUserChatRoomPresenceListener.localUserPresenceChanged</tt>
     * method.
     * @param evt the <tt>LocalUserChatRoomPresenceChangeEvent</tt> that
     * notified us
     */
    public void localUserPresenceChanged(
        LocalUserChatRoomPresenceChangeEvent evt)
    {
        ChatRoom sourceChatRoom = evt.getChatRoom();
        ChatRoomWrapper chatRoomWrapper
            = chatRoomList.findChatRoomWrapperFromChatRoom(sourceChatRoom);

        String eventType = evt.getEventType();

        if (LocalUserChatRoomPresenceChangeEvent
                .LOCAL_USER_JOINED.equals(eventType))
        {
            if(chatRoomWrapper != null)
            {
                this.fireChatRoomListChangedEvent(
                    chatRoomWrapper,
                    ChatRoomListChangeEvent.CHAT_ROOM_CHANGED);

                ChatWindowManager chatWindowManager
                    = GuiActivator.getUIService().getChatWindowManager();
                ChatPanel chatPanel
                    = chatWindowManager.getMultiChat(chatRoomWrapper, true);

                // Check if we have already opened a chat window for this chat
                // wrapper and load the real chat room corresponding to the
                // wrapper.
                if(chatPanel.isShown())
                    ((ConferenceChatSession) chatPanel.getChatSession())
                        .loadChatRoom(sourceChatRoom);
                else
                    chatWindowManager.openChat(chatPanel, true);
            }

            if (sourceChatRoom.isSystem())
            {
                ChatRoomProviderWrapper serverWrapper
                    = chatRoomList.findServerWrapperFromProvider(
                        sourceChatRoom.getParentProvider());

                serverWrapper.setSystemRoom(sourceChatRoom);
            }

            sourceChatRoom.addMessageListener(this);
        }
        else if (LocalUserChatRoomPresenceChangeEvent
                    .LOCAL_USER_JOIN_FAILED.equals(eventType))
        {
            new ErrorDialog(
                GuiActivator.getUIService().getMainFrame(),
                GuiActivator.getResources().getI18NString("service.gui.ERROR"),
                GuiActivator.getResources().getI18NString(
                    "service.gui.FAILED_TO_JOIN_CHAT_ROOM",
                    new String[]{sourceChatRoom.getName()}) + evt.getReason())
                .showDialog();
        }
        else if (LocalUserChatRoomPresenceChangeEvent
                        .LOCAL_USER_LEFT.equals(eventType)
                    || LocalUserChatRoomPresenceChangeEvent
                            .LOCAL_USER_KICKED.equals(eventType)
                    || LocalUserChatRoomPresenceChangeEvent
                            .LOCAL_USER_DROPPED.equals(eventType))
        {
            this.closeChatRoom(chatRoomWrapper);

            // Need to refresh the chat room's list in order to change
            // the state of the chat room to offline.
            fireChatRoomListChangedEvent(
                chatRoomWrapper,
                ChatRoomListChangeEvent.CHAT_ROOM_CHANGED);

            sourceChatRoom.removeMessageListener(this);
        }
    }

    /**
     * Called to accept an incoming invitation. Adds the invitation chat room
     * to the list of chat rooms and joins it.
     *
     * @param invitation the invitation to accept.
     */
    public void acceptInvitation(ChatRoomInvitation invitation)
    {
        ChatRoom chatRoom = invitation.getTargetChatRoom();
        byte[] password = invitation.getChatRoomPassword();

        String nickName
            = chatRoom.getParentProvider().getAccountID().getUserID();

        joinChatRoom(chatRoom, nickName, password);
    }

    /**
     * Called to accept an incoming invitation. Adds the invitation chat room
     * to the list of chat rooms and joins it.
     *
     * @param invitation the invitation to accept
     * @param multiUserChatOpSet the operation set for chat conferencing
     * @throws OperationFailedException if the accept fails
     */
    public void acceptInvitation(
        AdHocChatRoomInvitation invitation,
        OperationSetAdHocMultiUserChat multiUserChatOpSet)
        throws OperationFailedException
    {
        AdHocChatRoom chatRoom = invitation.getTargetAdHocChatRoom();

        chatRoom.join();
    }

    /**
     * Rejects the given invitation with the specified reason.
     *
     * @param multiUserChatOpSet the operation set to use for rejecting the
     * invitation
     * @param invitation the invitation to reject
     * @param reason the reason for the rejection
     */
    public void rejectInvitation(  OperationSetMultiUserChat multiUserChatOpSet,
                                   ChatRoomInvitation invitation,
                                   String reason)
    {
        multiUserChatOpSet.rejectInvitation(invitation, reason);
    }

    /**
     * Rejects the given invitation with the specified reason.
     *
     * @param multiUserChatAdHocOpSet the operation set to use for rejecting the
     * invitation
     * @param invitation the invitation to reject
     * @param reason the reason for the rejection
     */
    public void rejectInvitation(  
            OperationSetAdHocMultiUserChat     multiUserChatAdHocOpSet,
            AdHocChatRoomInvitation         invitation,
            String                             reason)
    {
         multiUserChatAdHocOpSet.rejectInvitation(invitation, reason);
    }

    /**
     * Joins the given chat room with the given password and manages all the
     * exceptions that could occur during the join process.
     *
     * @param chatRoomWrapper the chat room to join.
     * @param nickName the nickname we choose for the given chat room.
     * @param password the password.
     */
    public void joinChatRoom(   ChatRoomWrapper chatRoomWrapper,
                                String nickName,
                                byte[] password)
    {
        ChatRoom chatRoom = chatRoomWrapper.getChatRoom();

        if(chatRoom == null)
        {
            new ErrorDialog(
               GuiActivator.getUIService().getMainFrame(),
               GuiActivator.getResources().getI18NString("service.gui.WARNING"),
               GuiActivator.getResources().getI18NString(
                    "service.gui.CHAT_ROOM_NOT_CONNECTED",
                    new String[]{chatRoomWrapper.getChatRoomName()}))
                    .showDialog();

            return;
        }

        new JoinChatRoomTask(chatRoomWrapper, nickName, password).execute();
    }

    /**
     * Creates a chat room, by specifying the chat room name, the parent
     * protocol provider and eventually, the contacts invited to participate in 
     * this chat room.
     *
     * @param protocolProvider the parent protocol provider.
     * @param contacts the contacts invited when creating the chat room.
     * @param reason 
     * @return the <tt>ChatRoomWrapper</tt> corresponding to the created room
     */
    public ChatRoomWrapper createChatRoom(
        ProtocolProviderService protocolProvider,
        Collection<String> contacts,
        String reason)
    {
        return this.createChatRoom(null, protocolProvider, contacts, reason);
    }

    /**
     * Creates a chat room, by specifying the chat room name, the parent
     * protocol provider and eventually, the contacts invited to participate in
     * this chat room.
     *
     * @param roomName the name of the room
     * @param protocolProvider the parent protocol provider.
     * @param contacts the contacts invited when creating the chat room.
     * @param reason
     * @return the <tt>ChatRoomWrapper</tt> corresponding to the created room
     */
    public ChatRoomWrapper createChatRoom(
        String roomName,
        ProtocolProviderService protocolProvider,
        Collection<String> contacts,
        String reason)
    {
        return createChatRoom(
            roomName, protocolProvider, contacts, reason, true, true);
    }

    /**
     * Creates a chat room, by specifying the chat room name, the parent
     * protocol provider and eventually, the contacts invited to participate in 
     * this chat room.
     *
     * @param roomName the name of the room
     * @param protocolProvider the parent protocol provider.
     * @param contacts the contacts invited when creating the chat room.
     * @param reason
     * @param join whether we should join the room after creating it.
     * @param persistent whether the newly created room will be persistent.
     * @return the <tt>ChatRoomWrapper</tt> corresponding to the created room
     */
    public ChatRoomWrapper createChatRoom(
        String roomName,
        ProtocolProviderService protocolProvider,
        Collection<String> contacts,
        String reason,
        boolean join,
        boolean persistent)
    {
        ChatRoomWrapper chatRoomWrapper = null;

        OperationSetMultiUserChat groupChatOpSet
            = protocolProvider.getOperationSet(OperationSetMultiUserChat.class);

        // If there's no group chat operation set we have nothing to do here.
        if (groupChatOpSet == null)
            return null;

        ChatRoom chatRoom = null;
        try
        {
            chatRoom = groupChatOpSet.createChatRoom(roomName, null);

            if(join)
            {
                chatRoom.join();
            
                for(String contact : contacts)
                    chatRoom.invite(contact, reason);
            }
        }
        catch (OperationFailedException ex)
        {
            logger.error("Failed to create chat room.", ex);

            new ErrorDialog(
                GuiActivator.getUIService().getMainFrame(),
                GuiActivator.getResources().getI18NString("service.gui.ERROR"),
                GuiActivator.getResources().getI18NString(
                    "service.gui.CREATE_CHAT_ROOM_ERROR",
                    new String[]{protocolProvider.getProtocolName()}),
                    ex)
            .showDialog();
        }
        catch (OperationNotSupportedException ex)
        {
            logger.error("Failed to create chat room.", ex);

            new ErrorDialog(
                GuiActivator.getUIService().getMainFrame(),
                GuiActivator.getResources().getI18NString("service.gui.ERROR"),
                GuiActivator.getResources().getI18NString(
                    "service.gui.CREATE_CHAT_ROOM_ERROR",
                    new String[]{protocolProvider.getProtocolName()}),
                    ex)
            .showDialog();
        }

        if(chatRoom != null)
        {
            ChatRoomProviderWrapper parentProvider
                = chatRoomList.findServerWrapperFromProvider(protocolProvider);

            // if there is the same room ids don't add new wrapper as old one
            // maybe already created
            chatRoomWrapper =
                chatRoomList.findChatRoomWrapperFromChatRoom(chatRoom);

            if(chatRoomWrapper == null)
            {
                chatRoomWrapper = new ChatRoomWrapper(parentProvider, chatRoom);
                chatRoomWrapper.setPersistent(persistent);
                chatRoomList.addChatRoom(chatRoomWrapper);

                fireChatRoomListChangedEvent(
                    chatRoomWrapper,
                    ChatRoomListChangeEvent.CHAT_ROOM_ADDED);
            }
        }

        return chatRoomWrapper;
    }

    /**
     * Creates an ad-hoc chat room, by specifying the ad-hoc chat room name, the
     * parent protocol provider and eventually, the contacts invited to 
     * participate in this ad-hoc chat room.
     *
     * @param protocolProvider the parent protocol provider.
     * @param contacts the contacts invited when creating the chat room.
     * @param reason the reason for this invitation
     * @return the <tt>AdHocChatRoomWrapper</tt> corresponding to the created
     * ad hoc chat room
     */
    public AdHocChatRoomWrapper createAdHocChatRoom(
        ProtocolProviderService protocolProvider,
        Collection<String> contacts,
        String reason)
    {
        AdHocChatRoomWrapper chatRoomWrapper = null;

        OperationSetAdHocMultiUserChat groupChatOpSet
            = protocolProvider
                .getOperationSet(OperationSetAdHocMultiUserChat.class);

        // If there's no group chat operation set we have nothing to do here.
        if (groupChatOpSet == null)
            return null;

        AdHocChatRoom chatRoom = null;

        try
        {
            List<String> members = new LinkedList<String>();

            for(String address : contacts)
                members.add(address);
            
            chatRoom = groupChatOpSet.createAdHocChatRoom(
                "chatroom-" + new Date().getTime(), members, reason);
        }
        catch (OperationFailedException ex)
        {
            new ErrorDialog(
                GuiActivator.getUIService().getMainFrame(),
                GuiActivator.getResources().getI18NString("service.gui.ERROR"),
                GuiActivator.getResources().getI18NString(
                    "service.gui.CREATE_CHAT_ROOM_ERROR",
                    new String[]{protocolProvider.getProtocolName()}),
                    ex)
            .showDialog();
        }
        catch (OperationNotSupportedException ex)
        {
            new ErrorDialog(
                GuiActivator.getUIService().getMainFrame(),
                GuiActivator.getResources().getI18NString("service.gui.ERROR"),
                GuiActivator.getResources().getI18NString(
                    "service.gui.CREATE_CHAT_ROOM_ERROR",
                    new String[]{protocolProvider.getProtocolName()}),
                    ex)
            .showDialog();
        }

        if(chatRoom != null)
        {
            AdHocChatRoomProviderWrapper parentProvider
                = adHocChatRoomList.findServerWrapperFromProvider(
                        protocolProvider);

            chatRoomWrapper = new AdHocChatRoomWrapper(
                    parentProvider, chatRoom);
            parentProvider.addAdHocChatRoom(chatRoomWrapper);
            adHocChatRoomList.addAdHocChatRoom(chatRoomWrapper);

            fireAdHocChatRoomListChangedEvent(
                chatRoomWrapper,
                AdHocChatRoomListChangeEvent.AD_HOC_CHAT_ROOM_ADDED);
        }

        return chatRoomWrapper;
    }

    /**
     * Join chat room.
     * @param chatRoomWrapper
     */
    public void joinChatRoom(ChatRoomWrapper chatRoomWrapper)
    {
        ChatRoom chatRoom = chatRoomWrapper.getChatRoom();

        if(chatRoom == null)
        {
            new ErrorDialog(
               GuiActivator.getUIService().getMainFrame(),
               GuiActivator.getResources().getI18NString("service.gui.WARNING"),
               GuiActivator.getResources().getI18NString(
                        "service.gui.CHAT_ROOM_NOT_CONNECTED",
                        new String[]{chatRoomWrapper.getChatRoomName()}))
                    .showDialog();

            return;
        }

        new JoinChatRoomTask(chatRoomWrapper, null, null).execute();
    }

    /**
     * Joins the given ad-hoc chat room
     *
     * @param chatRoomWrapper
     */
    public void joinChatRoom(AdHocChatRoomWrapper chatRoomWrapper)
    {
        AdHocChatRoom chatRoom = chatRoomWrapper.getAdHocChatRoom();

        if(chatRoom == null)
        {
            new ErrorDialog(
               GuiActivator.getUIService().getMainFrame(),
               GuiActivator.getResources().getI18NString("service.gui.WARNING"),
               GuiActivator.getResources().getI18NString(
                        "service.gui.CHAT_ROOM_NOT_CONNECTED",
                        new String[]{chatRoomWrapper.getAdHocChatRoomName()}))
                    .showDialog();

            return;
        }

        new JoinAdHocChatRoomTask(chatRoomWrapper).execute();
    }

    /**
     * Removes the given chat room from the UI.
     *
     * @param chatRoomWrapper the chat room to remove.
     */
    public void removeChatRoom(ChatRoomWrapper chatRoomWrapper)
    {
        ChatRoom chatRoom = chatRoomWrapper.getChatRoom();

        if (chatRoom != null)
            leaveChatRoom(chatRoomWrapper);

        this.closeChatRoom(chatRoomWrapper);

        chatRoomList.removeChatRoom(chatRoomWrapper);

        fireChatRoomListChangedEvent(
            chatRoomWrapper,
            ChatRoomListChangeEvent.CHAT_ROOM_REMOVED);
    }

    /**
     * Joins the given chat room and manages all the exceptions that could
     * occur during the join process.
     *
     * @param chatRoom the chat room to join
     */
    public void joinChatRoom(ChatRoom chatRoom)
    {
        ChatRoomWrapper chatRoomWrapper
            = chatRoomList.findChatRoomWrapperFromChatRoom(chatRoom);

        if(chatRoomWrapper == null)
        {
            ChatRoomProviderWrapper parentProvider
                = chatRoomList
                    .findServerWrapperFromProvider(
                        chatRoom.getParentProvider());

            chatRoomWrapper = new ChatRoomWrapper(parentProvider, chatRoom);

            chatRoomList.addChatRoom(chatRoomWrapper);

            fireChatRoomListChangedEvent(
                chatRoomWrapper,
                ChatRoomListChangeEvent.CHAT_ROOM_ADDED);
        }

        this.joinChatRoom(chatRoomWrapper);

        ChatWindowManager chatWindowManager
            = GuiActivator.getUIService().getChatWindowManager();

        chatWindowManager
            .openChat(
                chatWindowManager.getMultiChat(chatRoomWrapper, true),
                true);
    }
    
    /**
     * Joins the given chat room and manages all the exceptions that could
     * occur during the join process.
     *
     * @param chatRoom the chat room to join
     */
    public void joinChatRoom(AdHocChatRoom chatRoom)
    {
        AdHocChatRoomWrapper chatRoomWrapper
            = adHocChatRoomList.findChatRoomWrapperFromAdHocChatRoom(chatRoom);

        if(chatRoomWrapper == null)
        {
            AdHocChatRoomProviderWrapper parentProvider
            = adHocChatRoomList.findServerWrapperFromProvider(
                chatRoom.getParentProvider());

            chatRoomWrapper = 
                new AdHocChatRoomWrapper(parentProvider, chatRoom);

            adHocChatRoomList.addAdHocChatRoom(chatRoomWrapper);

            fireAdHocChatRoomListChangedEvent(
                chatRoomWrapper,
                AdHocChatRoomListChangeEvent.AD_HOC_CHAT_ROOM_ADDED);
        }

        this.joinChatRoom(chatRoomWrapper);

        ChatWindowManager chatWindowManager
            = GuiActivator.getUIService().getChatWindowManager();

        chatWindowManager
            .openChat(
                chatWindowManager.getMultiChat(chatRoomWrapper, true),
                true);
    }

    /**
     * Joins the given chat room and manages all the exceptions that could
     * occur during the join process.
     *
     * @param chatRoom the chat room to join
     * @param nickname the nickname we're using to join
     * @param password the password we're using to join
     */
    public void joinChatRoom(   ChatRoom chatRoom,
                                String nickname,
                                byte[] password)
    {
        ChatRoomWrapper chatRoomWrapper
            = chatRoomList.findChatRoomWrapperFromChatRoom(chatRoom);

        if(chatRoomWrapper == null)
        {
            ChatRoomProviderWrapper parentProvider
                = chatRoomList.findServerWrapperFromProvider(
                    chatRoom.getParentProvider());

            chatRoomWrapper = new ChatRoomWrapper(parentProvider, chatRoom);

            chatRoomList.addChatRoom(chatRoomWrapper);

            fireChatRoomListChangedEvent(
                chatRoomWrapper,
                ChatRoomListChangeEvent.CHAT_ROOM_ADDED);
        }

        this.joinChatRoom(chatRoomWrapper, nickname, password);
    }

    /**
     * Joins the room with the given name though the given chat room provider.
     *
     * @param chatRoomName the name of the room to join.
     * @param chatRoomProvider the chat room provider to join through.
     */
    public void joinChatRoom(   String chatRoomName,
                                ChatRoomProviderWrapper chatRoomProvider)
    {
        FindRoomTask findRoomTask = new FindRoomTask(   chatRoomName,
                                                        chatRoomProvider);

        findRoomTask.execute();

        ChatRoom chatRoom = null;
        try
        {
            chatRoom = findRoomTask.get();
        }
        catch (InterruptedException e)
        {
            if (logger.isTraceEnabled())
                logger.trace("FindRoomTask has been interrupted.", e);
        }
        catch (ExecutionException e)
        {
            if (logger.isTraceEnabled())
                logger.trace("Execution exception occurred in FindRoomTask.", e);
        }

        if (chatRoom != null)
            this.joinChatRoom(chatRoom);
        else
            new ErrorDialog(
                GuiActivator.getUIService().getMainFrame(),
                GuiActivator.getResources().getI18NString("service.gui.ERROR"),
                GuiActivator.getResources().getI18NString(
                    "service.gui.CHAT_ROOM_NOT_EXIST",
                    new String[]{chatRoomName,
                    chatRoomProvider.getProtocolProvider()
                        .getAccountID().getService()}))
                    .showDialog();
    }

    /**
     * Leaves the given <tt>ChatRoom</tt>.
     *
     * @param chatRoomWrapper the <tt>ChatRoom</tt> to leave.
     */
    public void leaveChatRoom(ChatRoomWrapper chatRoomWrapper)
    {
        ChatRoom chatRoom = chatRoomWrapper.getChatRoom();

        if (chatRoom == null)
        {
            ResourceManagementService resources = GuiActivator.getResources();

            new ErrorDialog(
                    GuiActivator.getUIService().getMainFrame(),
                    resources.getI18NString("service.gui.WARNING"),
                    resources
                        .getI18NString(
                            "service.gui.CHAT_ROOM_LEAVE_NOT_CONNECTED"))
                .showDialog();

            return;
        }

        if (chatRoom.isJoined())
            chatRoom.leave();

        ChatRoomWrapper existChatRoomWrapper
            = chatRoomList.findChatRoomWrapperFromChatRoom(chatRoom);

        if(existChatRoomWrapper == null)
            return;

        // We save the choice of the user, before the chat room is really
        // joined, because even the join fails we want the next time when
        // we login to join this chat room automatically.
        ConfigurationManager.updateChatRoomStatus(
            chatRoomWrapper.getParentProvider().getProtocolProvider(),
            chatRoomWrapper.getChatRoomID(),
            Constants.OFFLINE_STATUS);

        this.closeChatRoom(existChatRoomWrapper);
    }

    /**
     * Leaves the given <tt>ChatRoom</tt>.
     *
     * @param chatRoomWrapper the <tt>ChatRoom</tt> to leave.
     */
    public void leaveChatRoom(AdHocChatRoomWrapper chatRoomWrapper)
    {
        AdHocChatRoom chatRoom = chatRoomWrapper.getAdHocChatRoom();

        if (chatRoom != null)
        {
            chatRoom.leave();
        }
        else
        {
            new ErrorDialog(
               GuiActivator.getUIService().getMainFrame(),
               GuiActivator.getResources().getI18NString("service.gui.WARNING"),
               GuiActivator.getResources().getI18NString(
                   "service.gui.CHAT_ROOM_LEAVE_NOT_CONNECTED"))
                   .showDialog();
        }
    }

    /**
     * Returns existing chat rooms for the given <tt>chatRoomProvider</tt>.
     * @param chatRoomProvider the <tt>ChatRoomProviderWrapper</tt>, which
     * chat rooms we're looking for
     * @return  existing chat rooms for the given <tt>chatRoomProvider</tt>
     */
    public List<String> getExistingChatRooms(
        ChatRoomProviderWrapper chatRoomProvider)
    {
        FindAllRoomsTask findAllRoomsTask
            = new FindAllRoomsTask(chatRoomProvider);

        findAllRoomsTask.execute();

        List<String> chatRooms = null;
        try
        {
            chatRooms = findAllRoomsTask.get();
        }
        catch (InterruptedException e)
        {
            if (logger.isTraceEnabled())
                logger.trace("FindAllRoomsTask has been interrupted.", e);
        }
        catch (ExecutionException e)
        {
            if (logger.isTraceEnabled())
                logger.trace("Execution exception occurred in FindAllRoomsTask", e);
        }

        return chatRooms;
    }

    /**
     * Checks if there's an open history window for the given chat room.
     *
     * @param chatRoomWrapper the chat room wrapper to check for
     * @return TRUE if there's an opened history window for the given chat room,
     *         FALSE otherwise.
     */
    public boolean containsHistoryWindowForChatRoom(
        ChatRoomWrapper chatRoomWrapper)
    {
        return chatRoomHistory.containsKey(chatRoomWrapper);
    }

    /**
     * Returns the history window for the given chat room.
     *
     * @param chatRoomWrapper the chat room wrapper to search for
     * @return the history window for the given chat room
     */
    public HistoryWindow getHistoryWindowForChatRoom(
        ChatRoomWrapper chatRoomWrapper)
    {
        return chatRoomHistory.get(chatRoomWrapper);
    }

    /**
     * Adds a history window for a given chat room in the table of opened
     * history windows.
     *
     * @param chatRoomWrapper the chat room wrapper to add
     * @param historyWindow the history window to add
     */
    public void addHistoryWindowForChatRoom(ChatRoomWrapper chatRoomWrapper,
        HistoryWindow historyWindow)
    {
        chatRoomHistory.put(chatRoomWrapper, historyWindow);
    }

    /**
     * Removes the history window for the given chat room.
     *
     * @param chatRoomWrapper the chat room wrapper to remove the history window
     */
    public void removeHistoryWindowForChatRoom(ChatRoomWrapper chatRoomWrapper)
    {
        chatRoomHistory.remove(chatRoomWrapper);
    }

    /**
     * Adds the given <tt>ChatRoomListChangeListener</tt> that will listen for
     * all changes of the chat room list data model.
     *
     * @param l the listener to add.
     */
    public void addChatRoomListChangeListener(ChatRoomListChangeListener l)
    {
        synchronized (listChangeListeners)
        {
            listChangeListeners.add(l);
        }
    }

    /**
     * Removes the given <tt>ChatRoomListChangeListener</tt>.
     *
     * @param l the listener to remove.
     */
    public void removeChatRoomListChangeListener(ChatRoomListChangeListener l)
    {
        synchronized (listChangeListeners)
        {
            listChangeListeners.remove(l);
        }
    }

    /**
     * Adds the given <tt>AdHocChatRoomListChangeListener</tt> that will listen
     * for all changes of the chat room list data model.
     *
     * @param l the listener to add.
     */
    public void addAdHocChatRoomListChangeListener(
            AdHocChatRoomListChangeListener l)
    {
        synchronized (adHoclistChangeListeners)
        {
            adHoclistChangeListeners.add(l);
        }
    }

    /**
     * Removes the given <tt>AdHocChatRoomListChangeListener</tt>.
     *
     * @param l the listener to remove.
     */
    public void removeAdHocChatRoomListChangeListener(
        AdHocChatRoomListChangeListener l)
    {
        synchronized (adHoclistChangeListeners)
        {
            adHoclistChangeListeners.remove(l);
        }
    }

    /**
     * Notifies all interested listeners that a change in the chat room list
     * model has occurred.
     * @param chatRoomWrapper the chat room wrapper that identifies the chat
     * room
     * @param eventID the identifier of the event
     */
    private void fireChatRoomListChangedEvent(  ChatRoomWrapper chatRoomWrapper,
                                                int eventID)
    {
        ChatRoomListChangeEvent evt
            = new ChatRoomListChangeEvent(chatRoomWrapper, eventID);

        for (ChatRoomListChangeListener l : listChangeListeners)
        {
            l.contentChanged(evt);
        }
    }

    /**
     * Notifies all interested listeners that a change in the chat room list
     * model has occurred.
     * @param adHocChatRoomWrapper the chat room wrapper that identifies the
     * chat room
     * @param eventID the identifier of the event
     */
    private void fireAdHocChatRoomListChangedEvent(  
                                    AdHocChatRoomWrapper adHocChatRoomWrapper,
                                    int                  eventID)
    {
        AdHocChatRoomListChangeEvent evt
            = new AdHocChatRoomListChangeEvent(adHocChatRoomWrapper, eventID);

        for (AdHocChatRoomListChangeListener l : adHoclistChangeListeners)
        {
            l.contentChanged(evt);
        }
    }

    /**
     * Closes the chat corresponding to the given chat room wrapper, if such
     * exists.
     *
     * @param chatRoomWrapper the chat room wrapper for which we search a chat
     * to close.
     */
    private void closeChatRoom(ChatRoomWrapper chatRoomWrapper)
    {
        ChatWindowManager chatWindowManager
            = GuiActivator.getUIService().getChatWindowManager();
        ChatPanel chatPanel
            = chatWindowManager.getMultiChat(chatRoomWrapper, false);

        if (chatPanel != null)
            chatWindowManager.closeChat(chatPanel);
    }

    /**
     * Closes the chat corresponding to the given ad-hoc chat room wrapper, if 
     * such exists.
     *
     * @param chatRoomWrapper the ad-hoc chat room wrapper for which we search a
     * chat to close.
     */
    private void closeAdHocChatRoom(AdHocChatRoomWrapper chatRoomWrapper)
    {
        ChatWindowManager chatWindowManager
            = GuiActivator.getUIService().getChatWindowManager();
        ChatPanel chatPanel
            = chatWindowManager.getMultiChat(chatRoomWrapper, false);

        if (chatPanel != null)
            chatWindowManager.closeChat(chatPanel);
    }

    /**
     * Handles <tt>ServiceEvent</tt>s triggered by adding or removing a
     * ProtocolProviderService. Updates the list of available chat rooms and
     * chat room servers.
     *
     * @param event The event to handle.
     */
    public void serviceChanged(ServiceEvent event)
    {
        // if the event is caused by a bundle being stopped, we don't want to
        // know
        if (event.getServiceReference().getBundle().getState()
                == Bundle.STOPPING)
            return;

        Object service = GuiActivator.bundleContext.getService(event
            .getServiceReference());

        // we don't care if the source service is not a protocol provider
        if (!(service instanceof ProtocolProviderService))
            return;

        ProtocolProviderService protocolProvider
            = (ProtocolProviderService) service;

        Object multiUserChatOpSet
            = protocolProvider
                .getOperationSet(OperationSetMultiUserChat.class);

        Object multiUserChatAdHocOpSet
            = protocolProvider
            .getOperationSet(OperationSetAdHocMultiUserChat.class);

        if (multiUserChatOpSet == null && multiUserChatAdHocOpSet != null)
        {
             if (event.getType() == ServiceEvent.REGISTERED)
             {
                 adHocChatRoomList.addChatProvider(protocolProvider);
             }
             else if (event.getType() == ServiceEvent.UNREGISTERING)
             {
                 adHocChatRoomList.removeChatProvider(protocolProvider);
             }
        }
        else if (multiUserChatAdHocOpSet == null && multiUserChatOpSet != null)
        {
             if (event.getType() == ServiceEvent.REGISTERED)
             {
                 chatRoomList.addChatProvider(protocolProvider);
             }
             else if (event.getType() == ServiceEvent.UNREGISTERING)
             {
                 chatRoomList.removeChatProvider(protocolProvider);
             }
        }
    }

    /**
     * Joins a chat room in an asynchronous way.
     */
    private static class JoinChatRoomTask
        extends SwingWorker<String, Object>
    {
        private static final String SUCCESS = "Success";

        private static final String AUTHENTICATION_FAILED
            = "AuthenticationFailed";

        private static final String REGISTRATION_REQUIRED
            = "RegistrationRequired";

        private static final String PROVIDER_NOT_REGISTERED
            = "ProviderNotRegistered";

        private static final String SUBSCRIPTION_ALREADY_EXISTS
            = "SubscriptionAlreadyExists";

        private static final String UNKNOWN_ERROR
            = "UnknownError";

        private final ChatRoomWrapper chatRoomWrapper;

        private final String nickName;

        private final byte[] password;

        JoinChatRoomTask(   ChatRoomWrapper chatRoomWrapper,
                            String nickName,
                            byte[] password)
        {
            this.chatRoomWrapper = chatRoomWrapper;
            this.nickName = nickName;
            this.password = password;
        }

        /**
         * @override {@link SwingWorker}{@link #doInBackground()} to perform
         * all asynchronous tasks.
         * @return SUCCESS if success, otherwise the error code
         */
        public String doInBackground()
        {
            ChatRoom chatRoom = chatRoomWrapper.getChatRoom();

            try
            {
                if(password != null && password.length > 0)
                    chatRoom.joinAs(nickName, password);
                else if (nickName != null)
                    chatRoom.joinAs(nickName);
                else
                    chatRoom.join();

                return SUCCESS;
            }
            catch (OperationFailedException e)
            {
                if (logger.isTraceEnabled())
                    logger.trace("Failed to join chat room: "
                    + chatRoom.getName(), e);

                switch (e.getErrorCode())
                {
                case OperationFailedException.AUTHENTICATION_FAILED:
                    return AUTHENTICATION_FAILED;
                case OperationFailedException.REGISTRATION_REQUIRED:
                    return REGISTRATION_REQUIRED;
                case OperationFailedException.PROVIDER_NOT_REGISTERED:
                    return PROVIDER_NOT_REGISTERED;
                case OperationFailedException.SUBSCRIPTION_ALREADY_EXISTS:
                    return SUBSCRIPTION_ALREADY_EXISTS;
                default:
                    return UNKNOWN_ERROR;
                }
            }
        }

        /**
         * @override {@link SwingWorker}{@link #done()} to perform UI changes
         * after the chat room join task has finished.
         */
        protected void done()
        {
            String returnCode = null;
            try
            {
                returnCode = get();
            }
            catch (InterruptedException ignore)
            {}
            catch (ExecutionException ignore)
            {}

            ConfigurationManager.updateChatRoomStatus(
                chatRoomWrapper.getParentProvider().getProtocolProvider(),
                chatRoomWrapper.getChatRoomID(),
                Constants.ONLINE_STATUS);

            String errorMessage = null;
            if(AUTHENTICATION_FAILED.equals(returnCode))
            {
                ChatRoomAuthenticationWindow authWindow
                    = new ChatRoomAuthenticationWindow(chatRoomWrapper);

                authWindow.setVisible(true);
            }
            else if(REGISTRATION_REQUIRED.equals(returnCode))
            {
                errorMessage
                    = GuiActivator.getResources()
                        .getI18NString(
                            "service.gui.CHAT_ROOM_REGISTRATION_REQUIRED",
                            new String[]{chatRoomWrapper.getChatRoomName()});
            }
            else if(PROVIDER_NOT_REGISTERED.equals(returnCode))
            {
                errorMessage
                    = GuiActivator.getResources()
                        .getI18NString("service.gui.CHAT_ROOM_NOT_CONNECTED",
                        new String[]{chatRoomWrapper.getChatRoomName()});
            }
            else if(SUBSCRIPTION_ALREADY_EXISTS.equals(returnCode))
            {
                errorMessage
                    = GuiActivator.getResources()
                        .getI18NString("service.gui.CHAT_ROOM_ALREADY_JOINED",
                            new String[]{chatRoomWrapper.getChatRoomName()});
            }
            else
            {
                errorMessage
                    = GuiActivator.getResources()
                        .getI18NString("service.gui.FAILED_TO_JOIN_CHAT_ROOM",
                            new String[]{chatRoomWrapper.getChatRoomName()});
            }

            if (!SUCCESS.equals(returnCode)
                    && !AUTHENTICATION_FAILED.equals(returnCode))
            {
                new ErrorDialog(
                    GuiActivator.getUIService().getMainFrame(),
                    GuiActivator.getResources().getI18NString(
                            "service.gui.ERROR"), errorMessage).showDialog();
            }
        }
    }
    
    /**
     * Joins an ad-hoc chat room in an asynchronous way.
     */
    private static class JoinAdHocChatRoomTask
        extends SwingWorker<String, Object>
    {
        private static final String SUCCESS = "Success";

        private static final String AUTHENTICATION_FAILED
            = "AuthenticationFailed";

        private static final String REGISTRATION_REQUIRED
            = "RegistrationRequired";

        private static final String PROVIDER_NOT_REGISTERED
            = "ProviderNotRegistered";

        private static final String SUBSCRIPTION_ALREADY_EXISTS
            = "SubscriptionAlreadyExists";

        private static final String UNKNOWN_ERROR
            = "UnknownError";

        private final AdHocChatRoomWrapper adHocChatRoomWrapper;

        JoinAdHocChatRoomTask(AdHocChatRoomWrapper chatRoomWrapper)
        {
            this.adHocChatRoomWrapper = chatRoomWrapper;
        }

        /**
         * @override {@link SwingWorker}{@link #doInBackground()} to perform
         * all asynchronous tasks.
         * @return SUCCESS if success, otherwise the error code
         */
        public String doInBackground()
        {
            AdHocChatRoom chatRoom = adHocChatRoomWrapper.getAdHocChatRoom();

            try
            {
                chatRoom.join();

                return SUCCESS;
            }
            catch (OperationFailedException e)
            {
                if (logger.isTraceEnabled())
                    logger.trace("Failed to join ad-hoc chat room: "
                    + chatRoom.getName(), e);

                switch (e.getErrorCode())
                {
                case OperationFailedException.AUTHENTICATION_FAILED:
                    return AUTHENTICATION_FAILED;
                case OperationFailedException.REGISTRATION_REQUIRED:
                    return REGISTRATION_REQUIRED;
                case OperationFailedException.PROVIDER_NOT_REGISTERED:
                    return PROVIDER_NOT_REGISTERED;
                case OperationFailedException.SUBSCRIPTION_ALREADY_EXISTS:
                    return SUBSCRIPTION_ALREADY_EXISTS;
                default:
                    return UNKNOWN_ERROR;
                }
            }
        }

        /**
         * @override {@link SwingWorker}{@link #done()} to perform UI changes
         * after the ad-hoc chat room join task has finished.
         */
        protected void done()
        {
            String returnCode = null;
            try
            {
                returnCode = get();
            }
            catch (InterruptedException ignore)
            {}
            catch (ExecutionException ignore)
            {}

            ConfigurationManager.updateChatRoomStatus(
                adHocChatRoomWrapper.getParentProvider().getProtocolProvider(),
                adHocChatRoomWrapper.getAdHocChatRoomID(),
                Constants.ONLINE_STATUS);

            String errorMessage = null;
            if(PROVIDER_NOT_REGISTERED.equals(returnCode))
            {
                errorMessage
                    = GuiActivator.getResources()
                        .getI18NString("service.gui.CHAT_ROOM_NOT_CONNECTED",
                        new String[]{
                            adHocChatRoomWrapper.getAdHocChatRoomName()});
            }
            else if(SUBSCRIPTION_ALREADY_EXISTS.equals(returnCode))
            {
                errorMessage
                    = GuiActivator.getResources()
                        .getI18NString("service.gui.CHAT_ROOM_ALREADY_JOINED",
                            new String[]{
                            adHocChatRoomWrapper.getAdHocChatRoomName()});
            }
            else
            {
                errorMessage
                    = GuiActivator.getResources()
                        .getI18NString("service.gui.FAILED_TO_JOIN_CHAT_ROOM",
                            new String[]{
                            adHocChatRoomWrapper.getAdHocChatRoomName()});
            }

            if (!SUCCESS.equals(returnCode)
                    && !AUTHENTICATION_FAILED.equals(returnCode))
            {
                new ErrorDialog(
                    GuiActivator.getUIService().getMainFrame(),
                    GuiActivator.getResources().getI18NString(
                            "service.gui.ERROR"), errorMessage).showDialog();
            }
        }
    }
    
    /**
     * Finds a chat room in asynchronous way.
     */
    private static class FindRoomTask
        extends SwingWorker<ChatRoom, Object>
    {
        private final String chatRoomName;

        private final ChatRoomProviderWrapper chatRoomProvider;

        FindRoomTask(   String chatRoomName,
                        ChatRoomProviderWrapper chatRoomProvider)
        {
            this.chatRoomName = chatRoomName;
            this.chatRoomProvider = chatRoomProvider;
        }

        /**
         * @override {@link SwingWorker}{@link #doInBackground()} to perform
         * all asynchronous tasks.
         * @return the chat room
         */
        public ChatRoom doInBackground()
        {
            OperationSetMultiUserChat groupChatOpSet
                = chatRoomProvider
                      .getProtocolProvider().getOperationSet(
                        OperationSetMultiUserChat.class);

            ChatRoom chatRoom = null;
            try
            {
                chatRoom = groupChatOpSet.findRoom(chatRoomName);
            }
            catch (Exception e)
            {
                if (logger.isTraceEnabled())
                    logger.trace("Un exception occurred while searching for room:"
                    + chatRoomName, e);
            }

            return chatRoom;
        }
    }

    private static class FindAllRoomsTask
        extends SwingWorker<List<String>, Object>
    {
        private final ChatRoomProviderWrapper chatRoomProvider;

        FindAllRoomsTask(ChatRoomProviderWrapper provider)
        {
            this.chatRoomProvider = provider;
        }

        /**
         * @override {@link SwingWorker}{@link #doInBackground()} to perform
         * all asynchronous tasks.
         * @return a list of existing chat rooms
         */
        public List<String> doInBackground()
        {
            ProtocolProviderService protocolProvider
                = chatRoomProvider.getProtocolProvider();

            if (protocolProvider == null)
                return null;

            OperationSetMultiUserChat groupChatOpSet
                = protocolProvider
                    .getOperationSet(OperationSetMultiUserChat.class);

            if (groupChatOpSet == null)
                return null;

            try
            {
                return groupChatOpSet.getExistingChatRooms();
            }
            catch (OperationFailedException e)
            {
                if (logger.isTraceEnabled())
                    logger.trace("Failed to obtain existing chat rooms for server: "
                    + protocolProvider.getAccountID().getService(), e);
            }
            catch (OperationNotSupportedException e)
            {
                if (logger.isTraceEnabled())
                    logger.trace("Failed to obtain existing chat rooms for server: "
                    + protocolProvider.getAccountID().getService(), e);
            }

            return null;
        }
    }

    /**
     * Indicates that an invitation has been received and opens the invitation
     * dialog to notify the user.
     * @param evt the <tt>AdHocChatRoomInvitationReceivedEvent</tt> that
     * notified us
     */
    public void invitationReceived(AdHocChatRoomInvitationReceivedEvent evt)
    {
        if (logger.isInfoEnabled())
            logger.info("Invitation received: "+evt.toString());
        OperationSetAdHocMultiUserChat multiUserChatOpSet
            = evt.getSourceOperationSet();

        InvitationReceivedDialog dialog = new InvitationReceivedDialog(
                this, multiUserChatOpSet, evt.getInvitation());

        dialog.setVisible(true);
    }

     /**
     * Implements the <tt>AdHocChatRoomMessageListener.messageDelivered</tt> 
     * method.
     * <br>
     * Shows the message in the conversation area and clears the write message
     * area.
     * @param evt the <tt>AdHocChatRoomMessageDeliveredEvent</tt> that notified
     * us
     */
    public void messageDelivered(AdHocChatRoomMessageDeliveredEvent evt)
    {
        AdHocChatRoom sourceChatRoom = (AdHocChatRoom) evt.getSource();

        if (logger.isInfoEnabled())
            logger.info("MESSAGE DELIVERED to ad-hoc chat room: "
            + sourceChatRoom.getName());

        ChatPanel chatPanel
            = GuiActivator
                .getUIService()
                    .getChatWindowManager()
                        .getMultiChat(sourceChatRoom, false);

        if(chatPanel != null)
        {
            String messageType;
            switch (evt.getEventType())
            {
            case AdHocChatRoomMessageDeliveredEvent
                    .CONVERSATION_MESSAGE_DELIVERED:
                messageType = Chat.OUTGOING_MESSAGE;
                break;
            case AdHocChatRoomMessageDeliveredEvent.ACTION_MESSAGE_DELIVERED:
                messageType = Chat.ACTION_MESSAGE;
                break;
            default:
                messageType = null;
            }

            Message msg = evt.getMessage();

            chatPanel
                .addMessage(
                    sourceChatRoom
                        .getParentProvider().getAccountID().getUserID(),
                    evt.getTimestamp(),
                    messageType,
                    msg.getContent(),
                    msg.getContentType());
        }
        else
        {
            logger.error("chat panel is null, message NOT DELIVERED !");
        }
    }

    /**
     * Implements <tt>AdHocChatRoomMessageListener.messageDeliveryFailed</tt>
     * method.
     * <br>
     * In the conversation area shows an error message, explaining the problem.
     * @param evt the <tt>AdHocChatRoomMessageDeliveryFailedEvent</tt> that
     * notified us
     */
    public void messageDeliveryFailed(
            AdHocChatRoomMessageDeliveryFailedEvent evt)
    {
        AdHocChatRoom sourceChatRoom = evt.getSourceChatRoom();
        Message sourceMessage = evt.getMessage();
        Contact destParticipant = evt.getDestinationParticipant();

        String errorMsg = null;
        if (evt.getErrorCode()
                == MessageDeliveryFailedEvent.OFFLINE_MESSAGES_NOT_SUPPORTED)
        {
            errorMsg = GuiActivator.getResources().getI18NString(
                    "service.gui.MSG_DELIVERY_NOT_SUPPORTED",
                    new String[]{destParticipant.getDisplayName()});
        }
        else if (evt.getErrorCode()
                == MessageDeliveryFailedEvent.NETWORK_FAILURE)
        {
            errorMsg = GuiActivator.getResources()
                .getI18NString("service.gui.MSG_NOT_DELIVERED");
        }
        else if (evt.getErrorCode()
                == MessageDeliveryFailedEvent.PROVIDER_NOT_REGISTERED)
        {
            errorMsg = GuiActivator.getResources().getI18NString(
                    "service.gui.MSG_SEND_CONNECTION_PROBLEM");
        }
        else if (evt.getErrorCode()
                == MessageDeliveryFailedEvent.INTERNAL_ERROR)
        {
            errorMsg = GuiActivator.getResources().getI18NString(
                    "service.gui.MSG_DELIVERY_INTERNAL_ERROR");
        }
        else
        {
            errorMsg = GuiActivator.getResources().getI18NString(
                    "service.gui.MSG_DELIVERY_UNKNOWN_ERROR");
        }

        ChatWindowManager chatWindowManager
            = GuiActivator.getUIService().getChatWindowManager();
        ChatPanel chatPanel
            = chatWindowManager.getMultiChat(sourceChatRoom, true);

        chatPanel.addMessage(
                destParticipant.getDisplayName(),
                System.currentTimeMillis(),
                Chat.OUTGOING_MESSAGE,
                sourceMessage.getContent(),
                sourceMessage.getContentType());

        chatPanel.addErrorMessage(
                destParticipant.getDisplayName(),
                errorMsg);

        chatWindowManager.openChat(chatPanel, false);
    }

    /**
     * Implements the <tt>AdHocChatRoomMessageListener.messageReceived</tt>
     * method.
     * <br>
     * Obtains the corresponding <tt>ChatPanel</tt> and process the message
     * there.
     * @param evt the <tt>AdHocChatRoomMessageReceivedEvent</tt> that notified
     * us
     */
    public void messageReceived(AdHocChatRoomMessageReceivedEvent evt)
    {
        AdHocChatRoom sourceChatRoom = evt.getSourceChatRoom();
        Contact sourceParticipant = evt.getSourceChatRoomParticipant();

        String messageType = null;

        switch (evt.getEventType())
        {
        case AdHocChatRoomMessageReceivedEvent.CONVERSATION_MESSAGE_RECEIVED:
            messageType = Chat.INCOMING_MESSAGE;
            break;
        case AdHocChatRoomMessageReceivedEvent.SYSTEM_MESSAGE_RECEIVED:
            messageType = Chat.SYSTEM_MESSAGE;
            break;
        case AdHocChatRoomMessageReceivedEvent.ACTION_MESSAGE_RECEIVED:
            messageType = Chat.ACTION_MESSAGE;
            break;
        }

        if (logger.isInfoEnabled())
            logger.info("MESSAGE RECEIVED from contact: "
            + sourceParticipant.getAddress());

        Message message = evt.getMessage();

        ChatWindowManager chatWindowManager
            = GuiActivator.getUIService().getChatWindowManager();
        ChatPanel chatPanel
            = chatWindowManager
                .getMultiChat(sourceChatRoom, true, message.getMessageUID());

        String messageContent = message.getContent();

        chatPanel.addMessage(
            sourceParticipant.getDisplayName(),
            evt.getTimestamp(),
            messageType,
            messageContent,
            message.getContentType());

        chatWindowManager.openChat(chatPanel, false);
    }

    public void invitationRejected(AdHocChatRoomInvitationRejectedEvent evt) {}
}
