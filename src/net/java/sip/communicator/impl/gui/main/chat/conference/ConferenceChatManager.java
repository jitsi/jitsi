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
package net.java.sip.communicator.impl.gui.main.chat.conference;

import java.util.*;
import java.util.concurrent.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.chat.*;
import net.java.sip.communicator.impl.gui.main.chat.history.*;
import net.java.sip.communicator.impl.gui.main.chatroomslist.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.muc.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.service.protocol.globalstatus.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.Logger;

import org.jdesktop.swingworker.SwingWorker;
import org.jitsi.util.*;
import org.osgi.framework.*;

/**
 * The <tt>ConferenceChatManager</tt> is the one that manages both chat room and
 * ad-hoc chat rooms invitations.
 *
 * @author Yana Stamcheva
 * @author Lubomir Marinov
 * @author Valentin Martinet
 * @author Hristo Terezov
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
                ServiceListener, ChatRoomLocalUserRoleListener
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
     * The list of ad-hoc chat rooms.
     */
    private final AdHocChatRoomList adHocChatRoomList = new AdHocChatRoomList();

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
            @Override
            public void run()
            {
                adHocChatRoomList.loadList();
            }
        }.start();

        GuiActivator.bundleContext.addServiceListener(this);

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
                null,
                evt.getTimestamp(),
                messageType,
                msg.getContent(),
                msg.getContentType(),
                msg.getMessageUID(),
                null);
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

        boolean createWindow = false;
        String autoOpenConfig
            = MUCService.getChatRoomAutoOpenOption(
                sourceChatRoom.getParentProvider(),
                sourceChatRoom.getIdentifier());
        if(autoOpenConfig == null)
            autoOpenConfig = MUCService.DEFAULT_AUTO_OPEN_BEHAVIOUR;

        if(autoOpenConfig.equals(MUCService.OPEN_ON_ACTIVITY)
            || (autoOpenConfig.equals(MUCService.OPEN_ON_MESSAGE)
                && !evt.isHistoryMessage())
            || evt.isImportantMessage())
            createWindow = true;

        if(sourceChatRoom.isSystem())
        {
            ChatRoomProviderWrapper serverWrapper
                = GuiActivator.getMUCService().findServerWrapperFromProvider(
                    sourceChatRoom.getParentProvider());

            chatPanel = chatWindowManager.getMultiChat(
                serverWrapper.getSystemRoomWrapper(), createWindow);
        }
        else
        {
            chatPanel = chatWindowManager.getMultiChat(
                sourceChatRoom, createWindow, message.getMessageUID());
        }

        if(chatPanel == null)
            return;

        String messageContent = message.getContent();

        if (evt.isHistoryMessage())
        {
            Date timeStamp = chatPanel.getChatConversationPanel()
                .getLastIncomingMsgTimestamp();
            Collection<Object> c =
                chatPanel.getChatSession().getHistoryBeforeDate(
                    new Date(
                        timeStamp.equals(new Date(0))
                        ? System.currentTimeMillis() - 10000
                        : timeStamp.getTime()
                    ), 20);
            if (c.size() > 0)
            {
                boolean isPresent = false;
                for (Object o : c)
                {
                    if (o instanceof ChatRoomMessageDeliveredEvent)
                    {
                        ChatRoomMessageDeliveredEvent ev =
                            (ChatRoomMessageDeliveredEvent) o;
                        if (evt.getTimestamp() != null
                            && evt.getTimestamp().equals(ev.getTimestamp()))
                        {
                            isPresent = true;
                            break;
                        }
                    }
                    else if(o instanceof ChatRoomMessageReceivedEvent)
                    {
                        ChatRoomMessageReceivedEvent ev =
                            (ChatRoomMessageReceivedEvent) o;
                        if (evt.getTimestamp() != null
                            && evt.getTimestamp().equals(ev.getTimestamp()))
                        {
                            isPresent = true;
                            break;
                        }
                    }

                    Message m2 = evt.getMessage();

                    if(m2 != null
                        && m2.getContent().equals(messageContent))
                    {
                        isPresent = true;
                        break;
                    }
                }

                if (isPresent)
                    return;
            }
        }

        chatPanel.addMessage(
            sourceMember.getName(),
            null,
            evt.getTimestamp(),
            messageType,
            messageContent,
            message.getContentType(),
            message.getMessageUID(),
            null);

        if(createWindow)
            chatWindowManager.openChat(chatPanel, false);
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
        else if (evt.getErrorCode()
            == ChatRoomMessageDeliveryFailedEvent.FORBIDDEN)
        {
            errorMsg = GuiActivator.getResources().getI18NString(
                "service.gui.CHAT_ROOM_SEND_MSG_FORBIDDEN");
        }
        else if (evt.getErrorCode()
            == ChatRoomMessageDeliveryFailedEvent.UNSUPPORTED_OPERATION)
        {
            errorMsg =
                GuiActivator.getResources().getI18NString(
                    "service.gui.MSG_DELIVERY_UNSUPPORTED_OPERATION");
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
            destMember != null ? destMember.getName()
                : sourceChatRoom.getName(),
            new Date(),
            Chat.OUTGOING_MESSAGE,
            sourceMessage.getContent(),
            sourceMessage.getContentType());

        chatPanel.addErrorMessage(
            destMember != null ? destMember.getName()
                : sourceChatRoom.getName(),
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
            GuiActivator.getAlertUIService().showAlertPopup(
                GuiActivator.getResources().getI18NString("service.gui.ERROR"),
                GuiActivator.getResources().getI18NString(
                    "service.gui.FAILED_TO_JOIN_CHAT_ROOM",
                    new String[]{sourceAdHocChatRoom.getName()})
                    + evt.getReason());
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
        final LocalUserChatRoomPresenceChangeEvent evt)
    {
        if(!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                @Override
                public void run()
                {
                    localUserPresenceChanged(evt);
                }
            });
            return;
        }

        ChatRoom sourceChatRoom = evt.getChatRoom();
        ChatRoomWrapper chatRoomWrapper
            = GuiActivator.getMUCService().findChatRoomWrapperFromChatRoom(
                sourceChatRoom);

        String eventType = evt.getEventType();

        if (LocalUserChatRoomPresenceChangeEvent
                .LOCAL_USER_JOINED.equals(eventType))
        {
            if(chatRoomWrapper != null)
            {
                GuiActivator.getMUCService().fireChatRoomListChangedEvent(
                    chatRoomWrapper,
                    ChatRoomListChangeEvent.CHAT_ROOM_CHANGED);

                boolean createWindow = false;

                String autoOpenConfig
                = MUCService.getChatRoomAutoOpenOption(
                    sourceChatRoom.getParentProvider(),
                    sourceChatRoom.getIdentifier());

                if(autoOpenConfig != null
                    && autoOpenConfig.equals(MUCService.OPEN_ON_ACTIVITY))
                    createWindow = true;

                ChatWindowManager chatWindowManager
                    = GuiActivator.getUIService().getChatWindowManager();
                ChatPanel chatPanel
                    = chatWindowManager.getMultiChat(
                        chatRoomWrapper, createWindow);

                if(chatPanel != null)
                {
                    chatPanel.setChatIcon(
                        chatPanel.getChatSession().getChatStatusIcon());

                    // Check if we have already opened a chat window for this chat
                    // wrapper and load the real chat room corresponding to the
                    // wrapper.
                    if(chatPanel.isShown())
                    {
                        ((ConferenceChatSession) chatPanel.getChatSession())
                            .loadChatRoom(sourceChatRoom);
                    }
                    else
                    {
                        chatWindowManager.openChat(chatPanel, true);
                    }
                }
            }

            if (sourceChatRoom.isSystem())
            {
                ChatRoomProviderWrapper serverWrapper
                    = GuiActivator.getMUCService()
                        .findServerWrapperFromProvider(
                            sourceChatRoom.getParentProvider());

                serverWrapper.setSystemRoom(sourceChatRoom);
            }

            sourceChatRoom.addMessageListener(this);
            sourceChatRoom.addLocalUserRoleListener(this);
        }
        else if (LocalUserChatRoomPresenceChangeEvent
                    .LOCAL_USER_JOIN_FAILED.equals(eventType))
        {
            GuiActivator.getAlertUIService().showAlertPopup(
                GuiActivator.getResources().getI18NString("service.gui.ERROR"),
                GuiActivator.getResources().getI18NString(
                    "service.gui.FAILED_TO_JOIN_CHAT_ROOM",
                    new String[]{sourceChatRoom.getName()})
                    + evt.getReason());
        }
        else if (LocalUserChatRoomPresenceChangeEvent
                        .LOCAL_USER_LEFT.equals(eventType)
                    || LocalUserChatRoomPresenceChangeEvent
                            .LOCAL_USER_KICKED.equals(eventType)
                    || LocalUserChatRoomPresenceChangeEvent
                            .LOCAL_USER_DROPPED.equals(eventType))
        {
            if(chatRoomWrapper != null)
            {
                if(StringUtils.isNullOrEmpty(evt.getReason()))
                {
                    GuiActivator.getUIService()
                        .closeChatRoomWindow(chatRoomWrapper);
                }
                else
                {
                    // send some system messages informing for the
                    // reason of leaving
                    ChatWindowManager chatWindowManager
                        = GuiActivator.getUIService().getChatWindowManager();

                    ChatPanel chatPanel = chatWindowManager.getMultiChat(
                        sourceChatRoom, false);

                    if(chatPanel != null)
                    {
                        chatPanel.addMessage(
                            sourceChatRoom.getName(),
                            null,
                            new Date(),
                            Chat.SYSTEM_MESSAGE,
                            evt.getReason(),
                            OperationSetBasicInstantMessaging.DEFAULT_MIME_TYPE,
                            null,
                            null);

                        // print and the alternate address
                        if(!StringUtils.isNullOrEmpty(
                                evt.getAlternateAddress()))
                        {
                            chatPanel.addMessage(
                                sourceChatRoom.getName(),
                                null,
                                new Date(),
                                Chat.SYSTEM_MESSAGE,
                                GuiActivator.getResources().getI18NString(
                                    "service.gui.CHAT_ROOM_ALTERNATE_ADDRESS",
                                    new String[]{evt.getAlternateAddress()}),
                                OperationSetBasicInstantMessaging
                                    .DEFAULT_MIME_TYPE,
                                null,
                                null);
                        }
                    }
                }

                // Need to refresh the chat room's list in order to change
                // the state of the chat room to offline.

                GuiActivator.getMUCService().fireChatRoomListChangedEvent(
                    chatRoomWrapper,
                    ChatRoomListChangeEvent.CHAT_ROOM_CHANGED);
            }

            sourceChatRoom.removeMessageListener(this);
            sourceChatRoom.removelocalUserRoleListener(this);
        }
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
            java.util.List<String> members = new LinkedList<String>();

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
                    new String[]{protocolProvider.getProtocolDisplayName()}),
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
                    new String[]{protocolProvider.getProtocolDisplayName()}),
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

        GuiActivator.getUIService().closeChatRoomWindow(chatRoomWrapper);

        GuiActivator.getMUCService().removeChatRoom(chatRoomWrapper);

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
     * Leaves the given <tt>ChatRoom</tt>.
     *
     * @param chatRoomWrapper the <tt>ChatRoom</tt> to leave.
     */
    public void leaveChatRoom(ChatRoomWrapper chatRoomWrapper)
    {
        ChatRoomWrapper leavedRoomWrapped
            = GuiActivator.getMUCService().leaveChatRoom(chatRoomWrapper);
        if(leavedRoomWrapped != null)
            GuiActivator.getUIService().closeChatRoomWindow(leavedRoomWrapped);
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


        Object multiUserChatAdHocOpSet
            = protocolProvider
            .getOperationSet(OperationSetAdHocMultiUserChat.class);

        if (multiUserChatAdHocOpSet != null)
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
        @Override
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
        @Override
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

            ConfigurationUtils.updateChatRoomStatus(
                adHocChatRoomWrapper.getParentProvider().getProtocolProvider(),
                adHocChatRoomWrapper.getAdHocChatRoomID(),
                GlobalStatusEnum.ONLINE_STATUS);

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
                GuiActivator.getAlertUIService().showAlertPopup(
                    GuiActivator.getResources().getI18NString(
                        "service.gui.ERROR"), errorMessage);
            }
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
                    null,
                    evt.getTimestamp(),
                    messageType,
                    msg.getContent(),
                    msg.getContentType(),
                    msg.getMessageUID(),
                    null);
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
        else if (evt.getErrorCode()
                == MessageDeliveryFailedEvent.UNSUPPORTED_OPERATION)
        {
            errorMsg = GuiActivator.getResources().getI18NString(
                "service.gui.MSG_DELIVERY_UNSUPPORTED_OPERATION");
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
                new Date(),
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
            null,
            evt.getTimestamp(),
            messageType,
            messageContent,
            message.getContentType(),
            message.getMessageUID(),
            null);

        chatWindowManager.openChat(chatPanel, false);
    }

    public void invitationRejected(AdHocChatRoomInvitationRejectedEvent evt) {}

    @Override
    public void localUserRoleChanged(ChatRoomLocalUserRoleChangeEvent evt)
    {
        if(evt.isInitial())
            return;
        ChatRoom sourceChatRoom = evt.getSourceChatRoom();
        ChatRoomWrapper chatRoomWrapper
            = GuiActivator.getMUCService().findChatRoomWrapperFromChatRoom(
                sourceChatRoom);
        ChatWindowManager chatWindowManager
            = GuiActivator.getUIService().getChatWindowManager();
        ChatPanel chatPanel
            = chatWindowManager.getMultiChat(chatRoomWrapper, true);
        chatWindowManager.openChat(chatPanel, true);
    }

}
