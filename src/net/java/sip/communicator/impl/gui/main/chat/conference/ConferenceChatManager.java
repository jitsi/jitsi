/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chat.conference;

import java.util.*;
import java.util.concurrent.*;

import javax.swing.*;

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

// Java 1.6 has javax.swing.SwingWorker so we have to disambiguate.
import org.jdesktop.swingworker.SwingWorker;
import org.osgi.framework.*;

/**
 * The <tt>ConferenceChatManager</tt> is the one that manages chat room
 * invitations.
 *
 * @author Yana Stamcheva
 * @author Lubomir Marinov
 */
public class ConferenceChatManager
    implements  ChatRoomMessageListener,
                ChatRoomInvitationListener,
                ChatRoomInvitationRejectionListener,
                LocalUserChatRoomPresenceListener,
                ServiceListener
{
    private static final Logger logger
        = Logger.getLogger(ConferenceChatManager.class);

    private final Hashtable<ChatRoomWrapper, HistoryWindow> chatRoomHistory =
        new Hashtable<ChatRoomWrapper, HistoryWindow>();

    private final ChatRoomList chatRoomList = new ChatRoomList();

    private final Vector<ChatRoomListChangeListener> listChangeListeners
        = new Vector<ChatRoomListChangeListener>();

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
     * Handles <tt>ChatRoomInvitationReceivedEvent</tt>-s.
     */
    public void invitationReceived(ChatRoomInvitationReceivedEvent evt)
    {
        OperationSetMultiUserChat multiUserChatOpSet
            = evt.getSourceOperationSet();

        InvitationReceivedDialog dialog = new InvitationReceivedDialog(
            this, multiUserChatOpSet, evt.getInvitation());

        dialog.setVisible(true);
    }

    public void invitationRejected(ChatRoomInvitationRejectedEvent evt)
    {
    }

    /**
     * Implements the <tt>ChatRoomMessageListener.messageDelivered</tt> method.
     * <br>
     * Shows the message in the conversation area and clears the write message
     * area.
     */
    public void messageDelivered(ChatRoomMessageDeliveredEvent evt)
    {
        ChatRoom sourceChatRoom = (ChatRoom) evt.getSource();

        logger.trace("MESSAGE DELIVERED to chat room: "
            + sourceChatRoom.getName());

        Message msg = evt.getMessage();

        ChatPanel chatPanel = null;

        ChatWindowManager chatWindowManager
            = GuiActivator.getUIService().getChatWindowManager();

        if(chatWindowManager.isChatOpenedForChatRoom(sourceChatRoom))
        {
            chatPanel = chatWindowManager.getMultiChat(sourceChatRoom);
        }

        String messageType = null;

        if (evt.getEventType()
            == ChatRoomMessageDeliveredEvent.CONVERSATION_MESSAGE_DELIVERED)
        {
            messageType = Chat.OUTGOING_MESSAGE;
        }
        else if (evt.getEventType()
            == ChatRoomMessageDeliveredEvent.ACTION_MESSAGE_DELIVERED)
        {
            messageType = Chat.ACTION_MESSAGE;
        }

        if(chatPanel != null)
        {
            chatPanel.addMessage(sourceChatRoom.getParentProvider()
                .getAccountID().getUserID(),
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
     */
    public void messageReceived(ChatRoomMessageReceivedEvent evt)
    {
        ChatRoom sourceChatRoom = (ChatRoom) evt.getSource();

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

            chatPanel
                = chatWindowManager.getMultiChat(
                    serverWrapper.getSystemRoomWrapper());
        }
        else
        {
            chatPanel = chatWindowManager
                .getMultiChat(sourceChatRoom, message.getMessageUID());
        }

        String messageContent = message.getContent();

        chatPanel.addMessage(
            sourceMember.getName(),
            evt.getTimestamp(),
            messageType,
            messageContent,
            message.getContentType());

        chatWindowManager.openChat(chatPanel, false);

        // Fire notification
        boolean fireChatNotification;

        /*
         * It is uncommon for IRC clients to display popup notifications for
         * messages which are sent to public channels and which do not mention
         * the nickname of the local user.
         */
        if (sourceChatRoom.isSystem()
                || isPrivate(sourceChatRoom)
                || (messageContent == null))
            fireChatNotification = true;
        else
        {
            String nickname = sourceChatRoom.getUserNickname();

            fireChatNotification =
                (nickname == null)
                    || messageContent.toLowerCase().contains(
                            nickname.toLowerCase());
        }
        if (fireChatNotification)
        {
            String title
                = GuiActivator.getResources().getI18NString(
                        "service.gui.MSG_RECEIVED",
                        new String[] { sourceMember.getName() });

            NotificationManager.fireChatNotification(
                sourceChatRoom,
                NotificationManager.INCOMING_MESSAGE,
                title,
                messageContent);
        }
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
     */
    public void messageDeliveryFailed(ChatRoomMessageDeliveryFailedEvent evt)
    {
        ChatRoom sourceChatRoom = (ChatRoom) evt.getSource();

        String errorMsg = null;

        Message sourceMessage = (Message) evt.getSource();

        ChatRoomMember destMember = evt.getDestinationChatRoomMember();

        if (evt.getErrorCode()
                == MessageDeliveryFailedEvent.OFFLINE_MESSAGES_NOT_SUPPORTED) {

            errorMsg = GuiActivator.getResources().getI18NString(
                    "service.gui.MSG_DELIVERY_NOT_SUPPORTED");
        }
        else if (evt.getErrorCode()
                == MessageDeliveryFailedEvent.NETWORK_FAILURE) {

            errorMsg = GuiActivator.getResources()
                .getI18NString("service.gui.MSG_NOT_DELIVERED");
        }
        else if (evt.getErrorCode()
                == MessageDeliveryFailedEvent.PROVIDER_NOT_REGISTERED) {

            errorMsg = GuiActivator.getResources().getI18NString(
                    "service.gui.MSG_SEND_CONNECTION_PROBLEM");
        }
        else if (evt.getErrorCode()
                == MessageDeliveryFailedEvent.INTERNAL_ERROR) {

            errorMsg = GuiActivator.getResources().getI18NString(
                    "service.gui.MSG_DELIVERY_INTERNAL_ERROR");
        }
        else {
            errorMsg = GuiActivator.getResources().getI18NString(
                    "service.gui.MSG_DELIVERY_UNKNOWN_ERROR");
        }

        ChatWindowManager chatWindowManager
            = GuiActivator.getUIService().getChatWindowManager();

        ChatPanel chatPanel
            = chatWindowManager.getMultiChat(sourceChatRoom);

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
     * <tt>LocalUserChatRoomPresenceListener.localUserPresenceChanged</tt>
     * method.
     */
    public void localUserPresenceChanged(
        LocalUserChatRoomPresenceChangeEvent evt)
    {
        ChatRoom sourceChatRoom = evt.getChatRoom();

        ChatRoomWrapper chatRoomWrapper = chatRoomList
            .findChatRoomWrapperFromChatRoom(sourceChatRoom);

        if (evt.getEventType().equals(
            LocalUserChatRoomPresenceChangeEvent.LOCAL_USER_JOINED))
        {
            if(chatRoomWrapper != null)
            {
                this.fireChatRoomListChangedEvent(
                    chatRoomWrapper,
                    ChatRoomListChangeEvent.CHAT_ROOM_CHANGED);

                ChatWindowManager chatWindowManager
                    = GuiActivator.getUIService().getChatWindowManager();

                ChatPanel chatPanel
                    = chatWindowManager.getMultiChat(chatRoomWrapper);

                // Check if we have already opened a chat window for this chat
                // wrapper and load the real chat room corresponding to the
                // wrapper.
                if(chatWindowManager
                    .isChatOpenedForChatRoom(chatRoomWrapper))
                {
                    ((ConferenceChatSession) chatPanel.getChatSession())
                        .loadChatRoom(sourceChatRoom);
                }
                else
                {
                    chatWindowManager.openChat(chatPanel, true);
                }
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
        else if (evt.getEventType().equals(
            LocalUserChatRoomPresenceChangeEvent.LOCAL_USER_JOIN_FAILED))
        {
            new ErrorDialog(
                GuiActivator.getUIService().getMainFrame(),
                GuiActivator.getResources().getI18NString("service.gui.ERROR"),
                GuiActivator.getResources().getI18NString(
                    "service.gui.FAILED_TO_JOIN_CHAT_ROOM",
                    new String[]{sourceChatRoom.getName()}) + evt.getReason())
                .showDialog();
        }
        else if (evt.getEventType().equals(
            LocalUserChatRoomPresenceChangeEvent.LOCAL_USER_LEFT))
        {
            this.closeChatRoom(chatRoomWrapper);

            // Need to refresh the chat room's list in order to change
            // the state of the chat room to offline.
            fireChatRoomListChangedEvent(
                chatRoomWrapper,
                ChatRoomListChangeEvent.CHAT_ROOM_CHANGED);

            sourceChatRoom.removeMessageListener(this);
        }
        else if (evt.getEventType().equals(
            LocalUserChatRoomPresenceChangeEvent.LOCAL_USER_KICKED))
        {
            this.closeChatRoom(chatRoomWrapper);

            // Need to refresh the chat room's list in order to change
            // the state of the chat room to offline.
            fireChatRoomListChangedEvent(
                chatRoomWrapper,
                ChatRoomListChangeEvent.CHAT_ROOM_CHANGED);

            sourceChatRoom.removeMessageListener(this);
        }
        else if (evt.getEventType().equals(
            LocalUserChatRoomPresenceChangeEvent.LOCAL_USER_DROPPED))
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
     * Rejects the given invitation with the specified reason.
     *
     * @param multiUserChatOpSet the operation set to use for rejecting the
     * invitation
     * @param invitation the invitation to reject
     * @param reason the reason for the rejection
     */
    public void rejectInvitation(   OperationSetMultiUserChat multiUserChatOpSet,
                                    ChatRoomInvitation invitation,
                                    String reason)
    {
        multiUserChatOpSet.rejectInvitation(invitation, reason);
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
     * Creates a chatroom, by specifying the chat room name and the parent
     * protocol provider.
     * @param chatRoomName the name of the chat room to create.
     * @param protocolProvider the parent protocol provider.
     */
    public ChatRoomWrapper createChatRoom(
        String chatRoomName,
        ProtocolProviderService protocolProvider)
    {
        ChatRoomWrapper chatRoomWrapper = null;

        OperationSetMultiUserChat groupChatOpSet
            = (OperationSetMultiUserChat) protocolProvider
                .getOperationSet(OperationSetMultiUserChat.class);

        // If there's no group chat operation set we have nothing to do here.
        if (groupChatOpSet == null)
            return null;

        ChatRoom chatRoom = null;
        try
        {
            chatRoom = groupChatOpSet.createChatRoom(chatRoomName, null);
        }
        catch (OperationFailedException ex)
        {
            logger.error("Failed to create chat room.", ex);

            new ErrorDialog(
                GuiActivator.getUIService().getMainFrame(),
                GuiActivator.getResources().getI18NString("service.gui.ERROR"),
                GuiActivator.getResources().getI18NString(
                    "service.gui.CREATE_CHAT_ROOM_ERROR",
                    new String[]{chatRoomName}),
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
                    new String[]{chatRoomName}),
                    ex)
            .showDialog();
        }

        if(chatRoom != null)
        {
            ChatRoomProviderWrapper parentProvider
                = chatRoomList.findServerWrapperFromProvider(protocolProvider);

            chatRoomWrapper = new ChatRoomWrapper(parentProvider, chatRoom);
            chatRoomList.addChatRoom(chatRoomWrapper);

            fireChatRoomListChangedEvent(
                chatRoomWrapper,
                ChatRoomListChangeEvent.CHAT_ROOM_ADDED);
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
     * Removes the given chat room from the UI.
     *
     * @param chatRoomWrapper the chat room to remove.
     */
    public void removeChatRoom(ChatRoomWrapper chatRoomWrapper)
    {
        this.leaveChatRoom(chatRoomWrapper);

        this.closeChatRoom(chatRoomWrapper);

        chatRoomList.removeChatRoom(chatRoomWrapper);

        this.fireChatRoomListChangedEvent(
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
            = chatRoomList.findServerWrapperFromProvider(
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
        chatWindowManager.openChat(
            chatWindowManager.getMultiChat(chatRoomWrapper), true);
    }

    /**
     * Joins the given chat room and manages all the exceptions that could
     * occur during the join process.
     *
     * @param chatRoom the chat room to join
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
            logger.trace("FindRoomTask has been interrupted.", e);
        }
        catch (ExecutionException e)
        {
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
            new ErrorDialog(
                GuiActivator.getUIService().getMainFrame(),
                GuiActivator.getResources().getI18NString("service.gui.WARNING"),
                GuiActivator.getResources().getI18NString(
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
    }

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
            logger.trace("FindAllRoomsTask has been interrupted.", e);
        }
        catch (ExecutionException e)
        {
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
     * Notifies all interested listeners that a change in the chat room list
     * model has occurred.
     */
    private void fireChatRoomListChangedEvent(   ChatRoomWrapper chatRoomWrapper,
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
     * Closes the chat corresponding to the given chat room wrapper, if such
     * exists.
     *
     * @param chatRoomWrapper the chat room wrapper for which we search a chat
     * to close.
     */
    private void closeChatRoom(ChatRoomWrapper chatRoomWrapper)
    {
        final ChatWindowManager chatWindowManager
            = GuiActivator.getUIService().getChatWindowManager();

        if(chatWindowManager.isChatOpenedForChatRoom(chatRoomWrapper))
        {
            final ChatPanel chatPanel
                = chatWindowManager.getMultiChat(chatRoomWrapper);

            // We have to be sure that we close the chat in the swing thread
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    chatWindowManager.closeChat(chatPanel);
                }
            });
        }
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
        {
            return;
        }

        Object service = GuiActivator.bundleContext.getService(event
            .getServiceReference());

        // we don't care if the source service is not a protocol provider
        if (!(service instanceof ProtocolProviderService))
        {
            return;
        }

        ProtocolProviderService protocolProvider
            = (ProtocolProviderService) service;

        Object multiUserChatOpSet
            = protocolProvider
                .getOperationSet(OperationSetMultiUserChat.class);

        // We don't care if there's no group chat operation set.
        if (multiUserChatOpSet == null)
        {
            return;
        }

        if (event.getType() == ServiceEvent.REGISTERED)
        {
            chatRoomList.addChatProvider(protocolProvider);
        }
        else if (event.getType() == ServiceEvent.UNREGISTERING)
        {
            chatRoomList.removeChatProvider(protocolProvider);
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
                    GuiActivator.getResources().getI18NString("service.gui.ERROR"),
                    errorMessage).showDialog();
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
         */
        public ChatRoom doInBackground()
        {
            OperationSetMultiUserChat groupChatOpSet
                = (OperationSetMultiUserChat) chatRoomProvider
                    .getProtocolProvider().getOperationSet(
                        OperationSetMultiUserChat.class);

            ChatRoom chatRoom = null;
            try
            {
                chatRoom = groupChatOpSet.findRoom(chatRoomName);
            }
            catch (Exception e)
            {
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
         */
        public List<String> doInBackground()
        {
            ProtocolProviderService protocolProvider
                = chatRoomProvider.getProtocolProvider();

            if (protocolProvider == null)
                return null;

            OperationSetMultiUserChat groupChatOpSet
                = (OperationSetMultiUserChat) protocolProvider
                    .getOperationSet(OperationSetMultiUserChat.class);

            if (groupChatOpSet == null)
                return null;

            try
            {
                return groupChatOpSet.getExistingChatRooms();
            }
            catch (OperationFailedException e)
            {
                logger.trace("Failed to obtain existing chat rooms for server: "
                    + protocolProvider.getAccountID().getService(), e);
            }
            catch (OperationNotSupportedException e)
            {
                logger.trace("Failed to obtain existing chat rooms for server: "
                    + protocolProvider.getAccountID().getService(), e);
            }

            return null;
        }
    }
}
