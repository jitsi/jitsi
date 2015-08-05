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
package net.java.sip.communicator.impl.gui.main.chat;

import java.awt.Component; // disambiguation
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.main.chat.conference.*;
import net.java.sip.communicator.impl.gui.main.chatroomslist.*;
import net.java.sip.communicator.impl.gui.main.contactlist.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.event.*;
import net.java.sip.communicator.service.muc.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.Logger;
import net.java.sip.communicator.util.account.*;

import org.jitsi.util.*;

/**
 * Manages chat windows and panels.
 *
 * @author Yana Stamcheva
 * @author Valentin Martinet
 * @author Lyubomir Marinov
 * @author Hristo Terezov
 */
public class ChatWindowManager
{
    /**
     * The <tt>Logger</tt> used by the <tt>ChatWindowManager</tt> class and its
     * instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(ChatWindowManager.class);

    private final List<ChatPanel> chatPanels
        = new ArrayList<ChatPanel>();

    private final List <ChatListener> chatListeners
        = new ArrayList <ChatListener> ();

    private final Object chatSyncRoot = new Object();

    /**
     * Opens the specified <tt>ChatPanel</tt> and optionally brings it to the
     * front.
     *
     * @param chatPanel the <tt>ChatPanel</tt> to be opened
     * @param setSelected <tt>true</tt> if <tt>chatPanel</tt> (and respectively
     * its <tt>ChatWindow</tt>) should be brought to the front; otherwise,
     * <tt>false</tt>
     */
    public void openChat(final ChatPanel chatPanel, final boolean setSelected)
    {
        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    openChat(chatPanel, setSelected);
                }
            });
            return;
        }

        synchronized (chatSyncRoot)
        {
            ChatContainer chatContainer = chatPanel.getChatContainer();

            if(!chatPanel.isShown())
                chatContainer.addChat(chatPanel);

            chatContainer.openChat(chatPanel, setSelected);
        }
    }

    /**
     * Opens the specified <tt>ChatPanel</tt> and optionally brings it to the
     * front.
     * 
     * @param room the chat room associated with the contact.
     * @param nickname the nickname of the contact in the chat room.
     */
    public void openPrivateChatForChatRoomMember(final ChatRoom room,
        final String nickname)
    {
        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    openPrivateChatForChatRoomMember(room, nickname);
                }
            });
            return;
        }
        
        Contact sourceContact = room.getPrivateContactByNickname(
            nickname);
        
        openPrivateChatForChatRoomMember(room, sourceContact);
    }
    
    /**
     * Opens the specified <tt>ChatPanel</tt> and optionally brings it to the
     * front.
     * @param room the chat room associated with the contact.
     * @param sourceContact the contact.
     */
    public void openPrivateChatForChatRoomMember(final ChatRoom room, 
        final Contact sourceContact)
    {
        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    openPrivateChatForChatRoomMember(room, sourceContact);
                }
            });
            return;
        }
        
        MetaContact metaContact
            = GuiActivator.getContactListService()
                .findMetaContactByContact(sourceContact);
        
        room.updatePrivateContactPresenceStatus(sourceContact);
        
        ChatPanel chatPanel = getContactChat(metaContact, sourceContact);
        chatPanel.setPrivateMessagingChat(true);
    
        openChat(chatPanel, true);
    }
    /**
     * Returns <tt>true</tt> if there is an opened <tt>ChatPanel</tt> for the
     * given <tt>MetaContact</tt>.
     *
     * @param metaContact the <tt>MetaContact</tt>, for which the chat is about
     * @return <tt>true</tt> if there is an opened <tt>ChatPanel</tt> for the
     * given <tt>MetaContact</tt>
     */
    public boolean isChatOpenedFor(MetaContact metaContact)
    {
        return isChatOpenedForDescriptor(metaContact);
    }

    /**
     * Determines whether there is an opened <tt>ChatPanel</tt> for a specific
     * chat descriptor.
     *
     * @param descriptor the chat descriptor which is to be checked whether
     * there is an opened <tt>ChatPanel</tt> for
     * @return <tt>true</tt> if there is an opened <tt>ChatPanel</tt> for the
     * specified chat descriptor; <tt>false</tt>, otherwise
     */
    private boolean isChatOpenedForDescriptor(Object descriptor)
    {
        synchronized (chatSyncRoot)
        {
            ChatPanel chatPanel = findChatPanelForDescriptor(descriptor);

            return ((chatPanel != null) && chatPanel.isShown());
        }
    }

    /**
     * Closes the given chat panel.
     *
     * @param chatPanel the chat panel to close
     */
    public void closeChat(final ChatPanel chatPanel)
    {
        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    closeChat(chatPanel);
                }
            });
            return;
        }

        synchronized (chatSyncRoot)
        {
            if(containsChat(chatPanel))
            {
                Date lastMsgTimestamp = chatPanel.getChatConversationPanel()
                    .getLastIncomingMsgTimestamp();

                if (!chatPanel.isWriteAreaEmpty())
                {
                    int answer = showWarningMessage(
                            "service.gui.NON_EMPTY_CHAT_WINDOW_CLOSE",
                            chatPanel);

                    if (answer == JOptionPane.OK_OPTION)
                        closeChatPanel(chatPanel);
                }
                else if (System.currentTimeMillis() - lastMsgTimestamp.getTime()
                                                                    < 2 * 1000)
                {
                    int answer = showWarningMessage(
                            "service.gui.CLOSE_CHAT_AFTER_NEW_MESSAGE",
                            chatPanel);

                    if (answer == JOptionPane.OK_OPTION)
                        closeChatPanel(chatPanel);
                }
                else if (chatPanel.containsActiveFileTransfers())
                {
                    int answer = showWarningMessage(
                                "service.gui.CLOSE_CHAT_ACTIVE_FILE_TRANSFER",
                                chatPanel);

                    if (answer == JOptionPane.OK_OPTION)
                    {
                        chatPanel.cancelActiveFileTransfers();
                        closeChatPanel(chatPanel);
                    }
                }
                else if (chatPanel.getChatSession() instanceof
                        AdHocConferenceChatSession)
                {
                    AdHocConferenceChatSession adHocSession
                        = (AdHocConferenceChatSession) chatPanel
                            .getChatSession();

                    GuiActivator.getUIService().getConferenceChatManager()
                        .leaveChatRoom(
                            (AdHocChatRoomWrapper) adHocSession.getDescriptor());

                    closeChatPanel(chatPanel);
                }
                else
                {
                    closeChatPanel(chatPanel);
                }
            }
        }
    }

    /**
     * Disposes the chat window.
     *
     * @param chatContainer the <tt>ChatContainer</tt> to dispose of
     */
    private void closeAllChats(ChatContainer chatContainer)
    {
        Collection<ChatPanel> chatPanelsToClose = new HashSet<ChatPanel>();

        chatPanelsToClose.addAll(chatContainer.getChats());
        synchronized (chatPanels)
        {
            chatPanelsToClose.addAll(chatPanels);
        }

        ChatPanel currentChat = chatContainer.getCurrentChat();

        for (ChatPanel chatPanel : chatPanelsToClose)
        {
            /*
             * We'll close the current ChatPanel last in order to avoid multiple
             * changes of the current ChatPanel.
             */
            if (chatPanel != currentChat)
            {
                try
                {
                    closeChatPanel(chatPanel);
                }
                catch (Exception e)
                {
                    logger.error("Failed to close chat: " + chatPanel, e);
                }
            }
        }
        /* As mentioned earlier, close the current ChatPanel last. */
        if ((currentChat != null) && chatPanels.contains(currentChat))
            closeChatPanel(currentChat);

        // Remove the envelope from the all active contacts in the contact list.
        if (chatContainer.getChatCount() <= 0)
            GuiActivator.getContactList().deactivateAll();
    }

    /**
     * Closes all chats in the specified <tt>ChatContainer</tt> and makes them
     * available for garbage collection.
     *
     * @param chatContainer the <tt>ChatContainer</tt> containing the chats to
     * close
     * @param warningEnabled indicates if the user should be warned that we're
     * closing all the chats. This would be done only if there are currently
     * active file transfers or waiting messages
     */
    void closeAllChats(ChatContainer chatContainer, boolean warningEnabled)
    {
        synchronized (chatSyncRoot)
        {
            // If no warning is enabled we just close all chats without asking
            // and return.
            if (!warningEnabled)
            {
                closeAllChats(chatContainer);
                return;
            }

            ChatPanel activePanel = null;

            for (ChatPanel chatPanel : chatPanels)
            {
                if (chatPanel.getChatSession() instanceof
                    AdHocConferenceChatSession)
                {
                    AdHocConferenceChatSession adHocSession
                        = (AdHocConferenceChatSession) chatPanel
                            .getChatSession();

                    GuiActivator.getUIService().getConferenceChatManager()
                        .leaveChatRoom(
                            (AdHocChatRoomWrapper) adHocSession.getDescriptor());
                }

                Date lastMsgTimestamp = chatPanel.getChatConversationPanel()
                                                .getLastIncomingMsgTimestamp();
                if (!chatPanel.isWriteAreaEmpty()
                    || chatPanel.containsActiveFileTransfers()
                    || System.currentTimeMillis()
                       - lastMsgTimestamp.getTime() < 2 * 1000)
                {
                    activePanel = chatPanel;
                }
            }

            if (activePanel == null)
            {
                this.closeAllChats(chatContainer);
                return;
            }

            Date lastMsgTimestamp = activePanel.getChatConversationPanel()
                                                .getLastIncomingMsgTimestamp();

            if (!activePanel.isWriteAreaEmpty())
            {
                int answer = showWarningMessage(
                        "service.gui.NON_EMPTY_CHAT_WINDOW_CLOSE",
                        chatContainer.getFrame());

                if (answer == JOptionPane.OK_OPTION)
                    this.closeAllChats(chatContainer);
            }
            else if (System.currentTimeMillis()
                     - lastMsgTimestamp.getTime() < 2 * 1000)
            {
                int answer = showWarningMessage(
                        "service.gui.CLOSE_CHAT_AFTER_NEW_MESSAGE",
                        chatContainer.getFrame());

                if (answer == JOptionPane.OK_OPTION)
                    this.closeAllChats(chatContainer);
            }
            else if (activePanel.containsActiveFileTransfers())
            {
                int answer = showWarningMessage(
                        "service.gui.CLOSE_CHAT_ACTIVE_FILE_TRANSFER",
                        chatContainer.getFrame());

                if (answer == JOptionPane.OK_OPTION)
                {
                    for (ChatPanel chatPanel : chatPanels)
                        chatPanel.cancelActiveFileTransfers();

                    this.closeAllChats(chatContainer);
                }
            }
        }
    }

    /**
     * Gets the <tt>ChatPanel</tt> corresponding to the specified
     * <tt>MetaContact</tt> and optionally creates it if it does not exist.
     *
     * @param metaContact the <tt>MetaContact</tt> to get the corresponding
     * <tt>ChatPanel</tt> of
     * @param create <tt>true</tt> to create a <tt>ChatPanel</tt> corresponding
     * to the specified <tt>MetaContact</tt> if such <tt>ChatPanel</tt> does not
     * exist yet
     * @return the <tt>ChatPanel</tt> corresponding to the specified
     * <tt>MetaContact</tt>; <tt>null</tt> if there is no such
     * <tt>ChatPanel</tt> and <tt>create</tt> is <tt>false</tt>
     */
    public ChatPanel getContactChat(MetaContact metaContact, boolean create)
    {
        // if we are not creating a ui we don't need any execution
        // in event dispatch thread, lets execute now
        if(!create)
            return getContactChat(metaContact, null, null, false, null);
        else
        {
            // we may create using event dispatch thread
            MetaContactChatCreateRunnable runnable
                = new MetaContactChatCreateRunnable(
                    metaContact, null, null, null);
            return runnable.getChatPanel();
        }
    }

    /**
     * Gets the <tt>ChatPanel</tt> corresponding to the specified
     * <tt>MetaContact</tt> and optionally creates it if it does not exist.
     *
     * @param metaContact the <tt>MetaContact</tt> to get the corresponding
     * <tt>ChatPanel</tt> of
     * @param create <tt>true</tt> to create a <tt>ChatPanel</tt> corresponding
     * to the specified <tt>MetaContact</tt> if such <tt>ChatPanel</tt> does not
     * exist yet
     * @param escapedMessageID the message ID of the message that should be
     * excluded from the history when the last one is loaded in the chat
     * @return the <tt>ChatPanel</tt> corresponding to the specified
     * <tt>MetaContact</tt>; <tt>null</tt> if there is no such
     * <tt>ChatPanel</tt> and <tt>create</tt> is <tt>false</tt>
     */
    public ChatPanel getContactChat(MetaContact metaContact, boolean create,
        String escapedMessageID)
    {
        // if we are not creating a ui we don't need any execution
        // in event dispatch thread, lets execute now
        if(!create)
            return getContactChat(  metaContact,
                                    null,
                                    null,
                                    false,
                                    escapedMessageID);
        else
        {
            // we may create using event dispatch thread
            MetaContactChatCreateRunnable runnable
                = new MetaContactChatCreateRunnable(
                    metaContact,
                    null,
                    null,
                    escapedMessageID);
            return runnable.getChatPanel();
        }
    }

    /**
     * Returns the chat panel corresponding to the given meta contact
     *
     * @param metaContact the meta contact.
     * @param protocolContact the protocol specific contact
     * @return the chat panel corresponding to the given meta contact
     */
    public ChatPanel getContactChat(MetaContact metaContact,
                                    Contact protocolContact)
    {
        return getContactChat(metaContact, protocolContact, null, null);
    }

    /**
     * Returns the chat panel corresponding to the given meta contact
     *
     * @param metaContact the meta contact.
     * @param protocolContact the protocol specific contact
     * @param contactResource the resource from which the contact is writing
     * @param escapedMessageID the message ID of the message that should be
     * excluded from the history when the last one is loaded in the chat
     * @return the chat panel corresponding to the given meta contact
     */
    public ChatPanel getContactChat(MetaContact metaContact,
                                    Contact protocolContact,
                                    ContactResource contactResource,
                                    String escapedMessageID)
    {
        // we may create using event dispatch thread
        MetaContactChatCreateRunnable runnable
            = new MetaContactChatCreateRunnable(
                metaContact,
                protocolContact,
                contactResource,
                escapedMessageID);
        return runnable.getChatPanel();
    }

    /**
     * Gets the <tt>ChatPanel</tt> corresponding to the specified
     * <tt>MetaContact</tt> and optionally creates it if it does not exist.
     * Must be executed on the event dispatch thread as it is creating UI.
     *
     * @param metaContact the <tt>MetaContact</tt> to get the corresponding
     * <tt>ChatPanel</tt> of
     * @param protocolContact the <tt>Contact</tt> (respectively its
     * <tt>ChatTransport</tt>) to be selected in the newly created
     * <tt>ChatPanel</tt>; <tt>null</tt> to select the default <tt>Contact</tt>
     * of <tt>metaContact</tt> if it is online or one of its <tt>Contact</tt>s
     * which supports offline messaging
     * @param create <tt>true</tt> to create a <tt>ChatPanel</tt> corresponding
     * to the specified <tt>MetaContact</tt> if such <tt>ChatPanel</tt> does not
     * exist yet
     * @param escapedMessageID the message ID of the message to be excluded from
     * the history when the last one is loaded in the newly created
     * <tt>ChatPanel</tt>
     * @return the <tt>ChatPanel</tt> corresponding to the specified
     * <tt>MetaContact</tt>; <tt>null</tt> if there is no such
     * <tt>ChatPanel</tt> and <tt>create</tt> is <tt>false</tt>
     */
    private ChatPanel getContactChat(
            MetaContact metaContact,
            Contact protocolContact,
            ContactResource contactResource,
            boolean create,
            String escapedMessageID)
    {
        synchronized (chatSyncRoot)
        {
            ChatPanel chatPanel = findChatPanelForDescriptor(metaContact);

            if ((chatPanel == null) && create)
                chatPanel
                    = createChat(
                        metaContact,
                        protocolContact,
                        contactResource,
                        escapedMessageID);

            return chatPanel;
        }
    }

    /**
     * Returns the currently selected <tt>ChatPanel</tt>.
     *
     * @return the currently selected <tt>ChatPanel</tt>
     */
    public ChatPanel getSelectedChat()
    {
        ChatPanel selectedChat = null;

        Iterator<ChatPanel> chatPanelsIter = chatPanels.iterator();

        synchronized (chatSyncRoot)
        {
            if (ConfigurationUtils.isMultiChatWindowEnabled())
            {
                if (chatPanelsIter.hasNext())
                {
                    ChatPanel firstChatPanel = chatPanelsIter.next();

                    selectedChat
                        = firstChatPanel.getChatContainer().getCurrentChat();
                }
            }
            else
            {
                while (chatPanelsIter.hasNext())
                {
                    ChatPanel chatPanel = chatPanelsIter.next();

                    if (chatPanel.getChatContainer().getFrame().isFocusOwner())
                        selectedChat = chatPanel;
                }
            }

            return selectedChat;
        }
    }

    /**
     * Gets the <tt>ChatPanel</tt> corresponding to the specified
     * <tt>ChatRoomWrapper</tt> and optionally creates it if it does not exist
     * yet.
     *
     * @param chatRoomWrapper the <tt>ChatRoomWrapper</tt> to get the
     * corresponding <tt>ChatPanel</tt> of
     * @param create <tt>true</tt> to create a new <tt>ChatPanel</tt> for the
     * specified <tt>ChatRoomWrapper</tt> if no such <tt>ChatPanel</tt> exists
     * already; otherwise, <tt>false</tt>
     * @return the <tt>ChatPanel</tt> corresponding to the specified
     * <tt>ChatRoomWrapper</tt> or <tt>null</tt> if no such <tt>ChatPanel</tt>
     * exists and <tt>create</tt> is <tt>false</tt>
     */
    private ChatPanel getMultiChatInternal(
            ChatRoomWrapper chatRoomWrapper,
            boolean create)
    {
        synchronized (chatSyncRoot)
        {
            ChatPanel chatPanel = findChatPanelForDescriptor(chatRoomWrapper);

            if ((chatPanel == null) && create)
                chatPanel = createChat(chatRoomWrapper);
            return chatPanel;
        }
    }

    /**
     * Gets the <tt>ChatPanel</tt> corresponding to the specified
     * <tt>ChatRoomWrapper</tt> and optionally creates it if it does not exist
     * yet.
     * Must be executed on the event dispatch thread.
     *
     * @param chatRoomWrapper the <tt>ChatRoomWrapper</tt> to get the
     * corresponding <tt>ChatPanel</tt> of
     * @param create <tt>true</tt> to create a new <tt>ChatPanel</tt> for the
     * specified <tt>ChatRoomWrapper</tt> if no such <tt>ChatPanel</tt> exists
     * already; otherwise, <tt>false</tt>
     * @return the <tt>ChatPanel</tt> corresponding to the specified
     * <tt>ChatRoomWrapper</tt> or <tt>null</tt> if no such <tt>ChatPanel</tt>
     * exists and <tt>create</tt> is <tt>false</tt>
     */
    public ChatPanel getMultiChat(
            ChatRoomWrapper chatRoomWrapper,
            boolean create)
    {
        if(!create)
            return getMultiChatInternal(chatRoomWrapper, false);
        else
        {
            // tries to execute creating of the ui on the
            // event dispatch thread
            return new CreateChatRoomWrapperRunner(chatRoomWrapper)
                .getChatPanel();
        }
    }

    /**
     * Gets the <tt>ChatPanel</tt> corresponding to the specified
     * <tt>AdHocChatRoomWrapper</tt> and optionally creates it if it does not
     * exist yet.
     *
     * @param chatRoomWrapper the <tt>AdHocChatRoomWrapper</tt> to get the
     * corresponding <tt>ChatPanel</tt> of
     * @param create <tt>true</tt> to create a new <tt>ChatPanel</tt> for the
     * specified <tt>AdHocChatRoomWrapper</tt> if no such <tt>ChatPanel</tt>
     * exists already; otherwise, <tt>false</tt>
     * @return the <tt>ChatPanel</tt> corresponding to the specified
     * <tt>AdHocChatRoomWrapper</tt> or <tt>null</tt> if no such
     * <tt>ChatPanel</tt> exists and <tt>create</tt> is <tt>false</tt>
     */
    private ChatPanel getMultiChatInternal(
            AdHocChatRoomWrapper chatRoomWrapper,
            boolean create)
    {
        synchronized (chatSyncRoot)
        {
            ChatPanel chatPanel = findChatPanelForDescriptor(chatRoomWrapper);

            if ((chatPanel == null) && create)
                chatPanel = createChat(chatRoomWrapper);
            return chatPanel;
        }
    }

    /**
     * Gets the <tt>ChatPanel</tt> corresponding to the specified
     * <tt>AdHocChatRoomWrapper</tt> and optionally creates it if it does not
     * exist yet.
     * Must be executed on the event dispatch thread.
     *
     * @param chatRoomWrapper the <tt>AdHocChatRoomWrapper</tt> to get the
     * corresponding <tt>ChatPanel</tt> of
     * @param create <tt>true</tt> to create a new <tt>ChatPanel</tt> for the
     * specified <tt>AdHocChatRoomWrapper</tt> if no such <tt>ChatPanel</tt>
     * exists already; otherwise, <tt>false</tt>
     * @return the <tt>ChatPanel</tt> corresponding to the specified
     * <tt>AdHocChatRoomWrapper</tt> or <tt>null</tt> if no such
     * <tt>ChatPanel</tt> exists and <tt>create</tt> is <tt>false</tt>
     */
    public ChatPanel getMultiChat(
            AdHocChatRoomWrapper chatRoomWrapper,
            boolean create)
    {
        if(!create)
            return getMultiChatInternal(chatRoomWrapper, false);
        else
        {
            // tries to execute creating of the ui on the
            // event dispatch thread
            return new CreateAdHocChatRoomWrapperRunner(chatRoomWrapper)
                .getChatPanel();
        }
    }

    /**
     * Gets the <tt>ChatPanel</tt> corresponding to the specified
     * <tt>AdHocChatRoom</tt> and optionally creates it if it does not exist.
     *
     * @param adHocChatRoom the <tt>AdHocChatRoom</tt> to get the corresponding
     * <tt>ChatPanel</tt> of
     * @param create <tt>true</tt> to create a <tt>ChatPanel</tt> corresponding
     * to the specified <tt>AdHocChatRoom</tt> if such <tt>ChatPanel</tt> does
     * not exist yet
     * @return the <tt>ChatPanel</tt> corresponding to the specified
     * <tt>AdHocChatRoom</tt>; <tt>null</tt> if there is no such
     * <tt>ChatPanel</tt> and <tt>create</tt> is <tt>false</tt>
     */
    public ChatPanel getMultiChat(AdHocChatRoom adHocChatRoom, boolean create)
    {
        return getMultiChat(adHocChatRoom, create, null);
    }

    /**
     * Gets the <tt>ChatPanel</tt> corresponding to the specified
     * <tt>ChatRoom</tt> and optionally creates it if it does not exist.
     *
     * @param chatRoom the <tt>ChatRoom</tt> to get the corresponding
     * <tt>ChatPanel</tt> of
     * @param create <tt>true</tt> to create a <tt>ChatPanel</tt> corresponding
     * to the specified <tt>ChatRoom</tt> if such <tt>ChatPanel</tt> does not
     * exist yet
     * @return the <tt>ChatPanel</tt> corresponding to the specified
     * <tt>ChatRoom</tt>; <tt>null</tt> if there is no such <tt>ChatPanel</tt>
     * and <tt>create</tt> is <tt>false</tt>
     */
    public ChatPanel getMultiChat(ChatRoom chatRoom, boolean create)
    {
        return getMultiChat(chatRoom, create, null);
    }

    /**
     * Gets the <tt>ChatPanel</tt> corresponding to the specified
     * <tt>ChatRoom</tt> and optionally creates it if it does not exist.
     *
     * @param chatRoom the <tt>ChatRoom</tt> to get the corresponding
     * <tt>ChatPanel</tt> of
     * @param create <tt>true</tt> to create a <tt>ChatPanel</tt> corresponding
     * to the specified <tt>ChatRoom</tt> if such <tt>ChatPanel</tt> does not
     * exist yet
     * @param escapedMessageID the message ID of the message that should be
     * excluded from the history when the last one is loaded in the chat
     * @return the <tt>ChatPanel</tt> corresponding to the specified
     * <tt>ChatRoom</tt>; <tt>null</tt> if there is no such <tt>ChatPanel</tt>
     * and <tt>create</tt> is <tt>false</tt>
     */
    private ChatPanel getMultiChatInternal(ChatRoom chatRoom,
                                  boolean create,
                                  String escapedMessageID)
    {
        synchronized (chatSyncRoot)
        {
            ChatRoomWrapper chatRoomWrapper
                = GuiActivator.getMUCService().getChatRoomWrapperByChatRoom(
                    chatRoom, create);

            ChatPanel chatPanel = null;

            if (chatRoomWrapper != null)
            {
                chatPanel = findChatPanelForDescriptor(chatRoomWrapper);
                if ((chatPanel == null) && create)
                    chatPanel = createChat(chatRoomWrapper, escapedMessageID);
            }

            return chatPanel;
        }
    }

    /**
     * Gets the <tt>ChatPanel</tt> corresponding to the specified
     * <tt>ChatRoom</tt> and optionally creates it if it does not exist.
     * Must be executed on the event dispatch thread.
     *
     * @param chatRoom the <tt>ChatRoom</tt> to get the corresponding
     * <tt>ChatPanel</tt> of
     * @param create <tt>true</tt> to create a <tt>ChatPanel</tt> corresponding
     * to the specified <tt>ChatRoom</tt> if such <tt>ChatPanel</tt> does not
     * exist yet
     * @param escapedMessageID the message ID of the message that should be
     * excluded from the history when the last one is loaded in the chat
     * @return the <tt>ChatPanel</tt> corresponding to the specified
     * <tt>ChatRoom</tt>; <tt>null</tt> if there is no such <tt>ChatPanel</tt>
     * and <tt>create</tt> is <tt>false</tt>
     */
    public ChatPanel getMultiChat(ChatRoom chatRoom,
                                  boolean create,
                                  String escapedMessageID)
    {
        if(!create)
            return getMultiChatInternal(chatRoom, false, escapedMessageID);
        else
        {
            return new CreateChatRoomRunner(chatRoom, escapedMessageID)
                .getChatPanel();
        }
    }

    /**
     * Gets the <tt>ChatPanel</tt> corresponding to the specified
     * <tt>AdHocChatRoom</tt> and optionally creates it if it does not exist.
     * Must be executed on the event dispatch thread.
     *
     * @param adHocChatRoom the <tt>AdHocChatRoom</tt> to get the corresponding
     * <tt>ChatPanel</tt> of
     * @param create <tt>true</tt> to create a <tt>ChatPanel</tt> corresponding
     * to the specified <tt>AdHocChatRoom</tt> if such <tt>ChatPanel</tt> does
     * not exist yet
     * @param escapedMessageID the message ID of the message that should be
     * excluded from the history when the last one is loaded in the chat
     * @return the <tt>ChatPanel</tt> corresponding to the specified
     * <tt>AdHocChatRoom</tt>; <tt>null</tt> if there is no such
     * <tt>ChatPanel</tt> and <tt>create</tt> is <tt>false</tt>
     */
    private ChatPanel getMultiChatInternal(AdHocChatRoom adHocChatRoom,
                                  boolean create,
                                  String escapedMessageID)
    {
        synchronized (chatSyncRoot)
        {
            AdHocChatRoomList chatRoomList = GuiActivator.getUIService()
                .getConferenceChatManager().getAdHocChatRoomList();

            // Search in the chat room's list for a chat room that correspond
            // to the given one.
            AdHocChatRoomWrapper chatRoomWrapper
                = chatRoomList
                    .findChatRoomWrapperFromAdHocChatRoom(adHocChatRoom);

            if ((chatRoomWrapper == null) && create)
            {
                AdHocChatRoomProviderWrapper parentProvider
                    = chatRoomList.findServerWrapperFromProvider(
                        adHocChatRoom.getParentProvider());

                chatRoomWrapper =
                    new AdHocChatRoomWrapper(parentProvider, adHocChatRoom);

                chatRoomList.addAdHocChatRoom(chatRoomWrapper);
            }

            ChatPanel chatPanel = null;

            if (chatRoomWrapper != null)
            {
                chatPanel = findChatPanelForDescriptor(chatRoomWrapper);
                if ((chatPanel == null) && create)
                    chatPanel = createChat(chatRoomWrapper, escapedMessageID);
            }

            return chatPanel;
        }
    }

    /**
     * Gets the <tt>ChatPanel</tt> corresponding to the specified
     * <tt>AdHocChatRoom</tt> and optionally creates it if it does not exist.
     * Must be executed on the event dispatch thread.
     *
     * @param adHocChatRoom the <tt>AdHocChatRoom</tt> to get the corresponding
     * <tt>ChatPanel</tt> of
     * @param create <tt>true</tt> to create a <tt>ChatPanel</tt> corresponding
     * to the specified <tt>AdHocChatRoom</tt> if such <tt>ChatPanel</tt> does
     * not exist yet
     * @param escapedMessageID the message ID of the message that should be
     * excluded from the history when the last one is loaded in the chat
     * @return the <tt>ChatPanel</tt> corresponding to the specified
     * <tt>AdHocChatRoom</tt>; <tt>null</tt> if there is no such
     * <tt>ChatPanel</tt> and <tt>create</tt> is <tt>false</tt>
     */
    public ChatPanel getMultiChat(AdHocChatRoom adHocChatRoom,
                                  boolean create,
                                  String escapedMessageID)
    {
        if(!create)
            return getMultiChatInternal(
                adHocChatRoom, false, escapedMessageID);
        else
            return new CreateAdHocChatRoomRunner(
                adHocChatRoom, escapedMessageID).getChatPanel();
    }

    /**
     * Returns all open <code>ChatPanel</code>s.
     *
     * @return  A list of <code>ChatPanel</code>s
     */
    public List<ChatPanel> getChatPanels()
    {
        return chatPanels;
    }

    /**
     * Starts a chat with the given <tt>MetaContact</tt>.
     * @param metaContact the destination <tt>MetaContact</tt>
     */
    public void startChat(MetaContact metaContact)
    {
        SwingUtilities.invokeLater(new RunChatWindow(metaContact));
    }

    /**
     * Starts a chat with the given <tt>MetaContact</tt>.
     * @param metaContact the destination <tt>MetaContact</tt>
     * @param protocolContact the protocol contact of the destination
     * @param isSmsMessage indicates if the chat should be opened for an SMS
     * message
     */
    public void startChat(  MetaContact metaContact,
                            Contact protocolContact,
                            boolean isSmsMessage)
    {
        SwingUtilities.invokeLater(
                new RunChatWindow(metaContact, protocolContact, isSmsMessage));
    }

    public void startChat(String contactString)
    {
        startChat(contactString, false);
    }

    /**
     * Start the chat with contact which is using the supplied protocol 
     *provider.
     * @param contactID the contact id to start chat with
     * @param pps the protocol provider
     */
    public void startChat(String contactID, ProtocolProviderService pps)
    {
        OperationSetPersistentPresence opSet
            = pps.getOperationSet(OperationSetPersistentPresence.class);

        if (opSet != null)
        {
            Contact c = opSet.findContactByID(contactID);

            if (c != null)
            {
                MetaContact metaContact = GuiActivator.getContactListService()
                    .findMetaContactByContact(c);

                if(metaContact == null)
                {
                    logger.error(
                        "Chat not started. Cannot find metacontact for "
                        + contactID + " and protocol:" + pps);

                    return;
                }

                startChat(metaContact, c, false);
                return;
            }
        }

        logger.error("Cannot start chat for " + contactID + " for "
            + pps.getAccountID().getAccountAddress());
    }

    public void startChat(String contactString, boolean isSmsEnabled)
    {
        List<ProtocolProviderService> imProviders
            = AccountUtils.getRegisteredProviders(
                    OperationSetBasicInstantMessaging.class);
        List<ProtocolProviderService> smsProviders
            = AccountUtils.getRegisteredProviders(
                    OperationSetSmsMessaging.class);

        if ((imProviders.size()
            + (smsProviders == null ? 0 : smsProviders.size())) < 1)
            throw new IllegalStateException("No im or sms providers!");

        Contact contact = null;
        MetaContactListService metaContactListService
            = GuiActivator.getContactListService();
        MetaContact metaContact = null;
        boolean startChat = false;

        for (ProtocolProviderService imProvider : imProviders)
        {
            try
            {
                OperationSetPresence presenceOpSet
                    = imProvider.getOperationSet(OperationSetPresence.class);

                if (presenceOpSet != null)
                {
                    contact = presenceOpSet.findContactByID(contactString);
                    if (contact != null)
                    {
                        metaContact
                            = metaContactListService.findMetaContactByContact(
                                    contact);
                        if (metaContact != null)
                        {
                            startChat = true;
                            break;
                        }
                    }
                    else
                    {
                        contact = 
                            presenceOpSet.createUnresolvedContact(
                                    contactString, null);
                        metaContact = 
                            metaContactListService.findMetaContactByContact(
                                    contact); 
                        if (metaContact != null)
                        {
                            startChat = true;
                            break;
                        }
                    }
                }
            }
            catch (Throwable t)
            {
                if (t instanceof ThreadDeath)
                    throw (ThreadDeath) t;
            }
        }
        if (startChat)
            startChat(metaContact, contact, isSmsEnabled);
        else if(isSmsEnabled)
        {
            // nothing found but we want to send sms, lets check and create
            // the contact as it may not exist
            if(smsProviders == null || smsProviders.size() == 0)
                return;

            OperationSetSmsMessaging smsOpSet
                = smsProviders.get(0)
                        .getOperationSet(OperationSetSmsMessaging.class);

            contact = smsOpSet.getContact(contactString);

            if (contact != null)
            {
                metaContact
                    = metaContactListService.findMetaContactByContact(contact);
                if (metaContact != null)
                {
                    startChat(metaContact, contact, true);
                }
            }
        }
    }

    /**
     * Removes the non read state of the currently selected chat session. This
     * will result in removal of all icons representing the non read state (like
     * envelopes in contact list).
     *
     * @param chatPanel the <tt>ChatPanel</tt> for which we would like to remove
     * non read chat state
     */
    public void removeNonReadChatState(ChatPanel chatPanel)
    {
        ChatSession chatSession = chatPanel.getChatSession();

        if(chatSession instanceof MetaContactChatSession)
        {
            MetaContact selectedMetaContact
                = (MetaContact) chatSession.getDescriptor();

            TreeContactList clist
                = GuiActivator.getContactList();

            // Remove the envelope from the contact when the chat has
            // gained the focus.
            if(clist.isContactActive(selectedMetaContact))
            {
                clist.setActiveContact(selectedMetaContact, false);
            }

            chatPanel.fireChatFocusEvent(ChatFocusEvent.FOCUS_GAINED);
        }
    }

    /**
     * Closes the selected chat tab or the window if there are no tabs.
     *
     * @param chatPanel the chat panel to close.
     */
    private void closeChatPanel(ChatPanel chatPanel)
    {
        ChatContainer chatContainer = chatPanel.getChatContainer();

        if (chatContainer != null)
            chatContainer.removeChat(chatPanel);

        boolean isChatPanelContained;

        synchronized (chatPanels)
        {
            isChatPanelContained = chatPanels.remove(chatPanel);
        }

        if (isChatPanelContained)
        {
            chatPanel.dispose();
            fireChatClosed(chatPanel);
        }
    }

    /**
     * Gets the default <tt>Contact</tt> of the specified <tt>MetaContact</tt>
     * if it is online; otherwise, gets one of its <tt>Contact</tt>s which
     * supports offline messaging.
     *
     * @param metaContact the <tt>MetaContact</tt> to get the default
     * <tt>Contact</tt> of
     * @return the default <tt>Contact</tt> of the specified
     * <tt>MetaContact</tt> if it is online; otherwise, gets one of its
     * <tt>Contact</tt>s which supports offline messaging
     */
    private Contact getDefaultContact(MetaContact metaContact)
    {
        Contact defaultContact = metaContact.getDefaultContact(
                        OperationSetBasicInstantMessaging.class);

        if(defaultContact == null)
        {
            defaultContact = metaContact.getDefaultContact(
                OperationSetSmsMessaging.class);

            if(defaultContact == null)
                return null;
        }

        ProtocolProviderService defaultProvider
            = defaultContact.getProtocolProvider();

        OperationSetBasicInstantMessaging defaultIM
            = defaultProvider
                .getOperationSet(OperationSetBasicInstantMessaging.class);

        if (defaultContact.getPresenceStatus().getStatus() < 1
                && (!defaultIM.isOfflineMessagingSupported()
                        || !defaultProvider.isRegistered()))
        {
            Iterator<Contact> protoContacts = metaContact.getContacts();

            while(protoContacts.hasNext())
            {
                Contact contact = protoContacts.next();
                ProtocolProviderService protoContactProvider
                    = contact.getProtocolProvider();
                OperationSetBasicInstantMessaging protoContactIM
                    = protoContactProvider
                        .getOperationSet(
                            OperationSetBasicInstantMessaging.class);

                if(  protoContactIM != null
                     && protoContactIM.isOfflineMessagingSupported()
                        && protoContactProvider.isRegistered())
                {
                    defaultContact = contact;
                }
            }
        }
        return defaultContact;
    }

    /**
     * Creates a <tt>ChatPanel</tt> for the given contact and saves it in the
     * list of created <tt>ChatPanel</tt>s.
     *
     * @param metaContact the <tt>MetaContact</tt> to create a
     * <tt>ChatPanel</tt> for
     * @param protocolContact the <tt>Contact</tt> (respectively its
     * <tt>ChatTransport</tt>) to be selected in the newly created
     * <tt>ChatPanel</tt>; <tt>null</tt> to select the default <tt>Contact</tt>
     * of <tt>metaContact</tt> if it is online or one of its <tt>Contact</tt>s
     * which supports offline messaging
     * @param contactResource the <tt>ContactResource</tt>, to be selected in
     * the newly created <tt>ChatPanel</tt>
     * @param escapedMessageID the message ID of the message that should be
     * excluded from the history when the last one is loaded in the chat.
     * @return The <code>ChatPanel</code> newly created.
     */
    private ChatPanel createChat(   MetaContact metaContact,
                                    Contact protocolContact,
                                    ContactResource contactResource,
                                    String escapedMessageID)
    {
        if (protocolContact == null)
            protocolContact = getDefaultContact(metaContact);

        if(protocolContact == null)
            return null;

        ChatContainer chatContainer = getChatContainer();
        ChatPanel chatPanel = new ChatPanel(chatContainer);

        MetaContactChatSession chatSession
            = new MetaContactChatSession(   chatPanel,
                                            metaContact,
                                            protocolContact,
                                            contactResource);

        chatPanel.setChatSession(chatSession);

        synchronized (chatPanels)
        {
            this.chatPanels.add(chatPanel);
        }

        chatPanel.loadHistory(escapedMessageID);

        fireChatCreated(chatPanel);
        return chatPanel;
    }

    /**
     * Gets a <tt>ChatContainer</tt> instance. If there is no existing
     * <tt>ChatContainer</tt> or chats are configured to be displayed in their
     * own windows instead of arranged in tabs in a single window, creates a
     * new chat container.
     *
     * @return a <tt>ChatContainer</tt> instance
     */
    private ChatContainer getChatContainer()
    {
        ChatContainer chatContainer
            = GuiActivator.getUIService().getSingleWindowContainer();

        // If we're in a single window mode we just return the chat container.
        if (chatContainer != null)
            return chatContainer;

        // If we're in a multi-window mode we have two possibilities - multi
        // chat window or single chat windows.
        if (ConfigurationUtils.isMultiChatWindowEnabled())
        {
            Iterator<ChatPanel> chatPanelsIter = chatPanels.iterator();

            /*
             * If we're in a tabbed window we're looking for the chat window
             * through one of the existing chats.
             */
            if (chatPanelsIter.hasNext())
                chatContainer = chatPanelsIter.next().getChatContainer();
            else
            {
                chatContainer = new ChatWindow();
                GuiActivator.getUIService().registerExportedWindow(
                    (ExportedWindow) chatContainer);
            }
        }
        else
            chatContainer = new ChatWindow();

        return chatContainer;
    }

    /**
     * Creates a <tt>ChatPanel</tt> for the given <tt>ChatRoom</tt> and saves it
     * in the list of created <tt>ChatPanel</tt>s.
     *
     * @param chatRoomWrapper the <tt>ChatRoom</tt>, for which the chat will be
     * created
     * @return The <code>ChatPanel</code> newly created.
     */
    private ChatPanel createChat(ChatRoomWrapper chatRoomWrapper)
    {
        return createChat(chatRoomWrapper, null);
    }

    /**
     * Creates a <tt>ChatPanel</tt> for the given <tt>AdHocChatRoom</tt> and
     * saves it in the list of created <tt>ChatPanel</tt>s.
     *
     * @param chatRoomWrapper the <tt>AdHocChatRoom</tt>, for which the chat
     * will be created
     * @return The <code>ChatPanel</code> newly created.
     */
    private ChatPanel createChat(AdHocChatRoomWrapper chatRoomWrapper)
    {
        return createChat(chatRoomWrapper, null);
    }

    /**
     * Creates a <tt>ChatPanel</tt> for the given <tt>ChatRoom</tt> and saves it
     * in the list of created <tt>ChatPanel</tt>s.
     *
     * @param chatRoomWrapper the <tt>ChatRoom</tt>, for which the chat will be
     * created
     * @param escapedMessageID the message ID of the message that should be
     * excluded from the history when the last one is loaded in the chat.
     * @return The <code>ChatPanel</code> newly created.
     */
    private ChatPanel createChat(   ChatRoomWrapper chatRoomWrapper,
                                    String escapedMessageID)
    {
        ChatContainer chatContainer = getChatContainer();
        ChatPanel chatPanel = new ChatPanel(chatContainer);

        ConferenceChatSession chatSession
            = new ConferenceChatSession(chatPanel,
                                        chatRoomWrapper);

        chatPanel.setChatSession(chatSession);

        synchronized (chatPanels)
        {
            this.chatPanels.add(chatPanel);
        }

        chatPanel.loadHistory(escapedMessageID);

        fireChatCreated(chatPanel);
        return chatPanel;
    }

    /**
     * Creates a <tt>ChatPanel</tt> for the given <tt>AdHocChatRoom</tt> and
     * saves it in the list of created <tt>ChatPanel</tt>s.
     *
     * @param chatRoomWrapper the <tt>AdHocChatRoom</tt>, for which the chat
     * will be created
     * @param escapedMessageID the message ID of the message that should be
     * excluded from the history when the last one is loaded in the chat.
     * @return The <code>ChatPanel</code> newly created.
     */
    private ChatPanel createChat( AdHocChatRoomWrapper chatRoomWrapper,
                                            String escapedMessageID)
    {
        ChatContainer chatContainer = getChatContainer();
        ChatPanel chatPanel = new ChatPanel(chatContainer);

        AdHocConferenceChatSession chatSession
            = new AdHocConferenceChatSession(chatPanel, chatRoomWrapper);

        chatPanel.setChatSession(chatSession);

        synchronized (chatPanels)
        {
            this.chatPanels.add(chatPanel);
        }

        chatPanel.loadHistory(escapedMessageID);

        fireChatCreated(chatPanel);
        return chatPanel;
    }

    /**
     * Finds the <tt>ChatPanel</tt> corresponding to the given chat descriptor.
     *
     * @param descriptor the chat descriptor.
     * @return the <tt>ChatPanel</tt> corresponding to the given chat descriptor
     * if any; otherwise, <tt>null</tt>
     */
    private ChatPanel findChatPanelForDescriptor(Object descriptor)
    {
        for (ChatPanel chatPanel : chatPanels)
            if (chatPanel.getChatSession().getDescriptor().equals(descriptor))
                return chatPanel;
        return null;
    }

    /**
     * Notifies the <tt>ChatListener</tt>s registered with this instance that
     * a specific <tt>Chat</tt> has been closed.
     *
     * @param chat the <tt>Chat</tt> which has been closed and which the
     * <tt>ChatListener</tt>s registered with this instance are to be notified
     * about
     */
    private void fireChatClosed(Chat chat)
    {
        List <ChatListener> listeners;
        synchronized (chatListeners)
        {
            listeners = new ArrayList<ChatListener>(chatListeners);
        }

        for(ChatListener listener : listeners)
            listener.chatClosed(chat);
    }

    /**
     * Notifies the <tt>ChatListener</tt>s registered with this instance that
     * a specific <tt>Chat</tt> has been created.
     *
     * @param chat the <tt>Chat</tt> which has been created and which the
     * <tt>ChatListener</tt>s registered with this instance are to be notified
     * about
     */
    private void fireChatCreated(Chat chat)
    {
        List <ChatListener> listeners;
        synchronized (chatListeners)
        {
            listeners = new ArrayList<ChatListener>(chatListeners);
        }

        for(ChatListener listener : listeners)
            listener.chatCreated(chat);
    }

    /**
     * Returns <tt>true</tt> if this chat window contains the given chatPanel;
     * <tt>false</tt>, otherwise.
     *
     * @param chatPanel the chat panel that we're looking for.
     * @return <tt>true</tt> if this chat window contains the given chatPanel;
     * <tt>false</tt>, otherwise
     */
    private boolean containsChat(ChatPanel chatPanel)
    {
        synchronized (chatPanels)
        {
            return chatPanels.contains(chatPanel);
        }
    }

    /**
     * Runs the chat window for the specified contact
     */
    private class RunChatWindow implements Runnable
    {
        private final MetaContact metaContact;

        private final Contact protocolContact;

        private final Boolean isSmsSelected;

        /**
         * Creates an instance of <tt>RunMessageWindow</tt> by specifying the
         *
         * @param metaContact the meta contact to which we will talk.
         */
        public RunChatWindow(MetaContact metaContact)
        {
            this(metaContact, null);
        }

        /**
         * Creates a chat window.
         *
         * @param metaContact the destination <tt>MetaContact</tt>
         * @param protocolContact the destination protocol contact
         */
        public RunChatWindow(   MetaContact metaContact,
                                Contact protocolContact)
        {
            this(metaContact, protocolContact, null);
        }

        /**
         * Creates a chat window
         *
         * @param metaContact the meta contact to which we will talk.
         * @param protocolContact the destination protocol contact
         * @param isSmsSelected whether the sms option should be selected
         */
        public RunChatWindow(   MetaContact metaContact,
                                Contact protocolContact,
                                Boolean isSmsSelected)
        {
            this.metaContact = metaContact;
            this.protocolContact = protocolContact;
            this.isSmsSelected = isSmsSelected;
        }

        /**
         * Opens a chat window
         */
        @Override
        public void run()
        {
            ChatPanel chatPanel = getContactChat(metaContact, protocolContact);

            if(chatPanel == null)
                return;

            // if not explicitly set, do not set it, leave it to default or
            // internally make the decision
            if(isSmsSelected != null)
                chatPanel.setSmsSelected(isSmsSelected);

            openChat(chatPanel, true);
        }
    }

    /**
     * Returns all currently instantiated <tt>ChatPanels</tt>.
     * @return all instantiated <tt>ChatPanels</tt>
     */
    public Collection <ChatPanel> getAllChats()
    {
        synchronized (chatSyncRoot)
        {
            return chatPanels;
        }
    }

    /**
     * Registers a <tt>NewChatListener</tt> to be informed when new <tt>Chats</tt>
     * are created.
     * @param listener listener to be registered
     */
    public void addChatListener(ChatListener listener)
    {
        synchronized (chatListeners)
        {
            if (!chatListeners.contains(listener))
                chatListeners.add(listener);
        }
    }

    /**
     * Removes the registration of a <tt>NewChatListener</tt>.
     * @param listener listener to be unregistered
     */
    public void removeChatListener(ChatListener listener)
    {
        synchronized (chatListeners)
        {
            chatListeners.remove(listener);
        }
    }

    /**
     * Displays a custom warning message.
     *
     * @param resourceString The resource name of the message to display.
     * @param parentComponent Determines the Frame in which the dialog is
     * displayed; if null, or if the parentComponent has no Frame, a default
     * Frame is used
     *
     * @return The integer corresponding to the option choosen by the user.
     */
    private static int showWarningMessage(
            String resourceString,
            Component parentComponent)
    {
        SIPCommMsgTextArea msgText
            = new SIPCommMsgTextArea(
                    GuiActivator.getResources().getI18NString(resourceString));
        JComponent textComponent = msgText;
        if(OSUtils.IS_LINUX)
        {
            JScrollPane jScrollPane = new JScrollPane(msgText);
            jScrollPane.setBorder(null);
            textComponent = jScrollPane;
        }

        return JOptionPane.showConfirmDialog(
                parentComponent,
                textComponent,
                GuiActivator.getResources().getI18NString(
                    "service.gui.WARNING"),
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE);
    }

    /**
     * Runnable used as base for all that creates chat panels.
     */
    private abstract class AbstractChatPanelCreateRunnable
        implements Runnable
    {
        /**
         * The result panel.
         */
        private ChatPanel chatPanel;

        /**
         * Returns the result chat panel.
         * @return the result chat panel.
         */
        public ChatPanel getChatPanel()
        {
            try
            {
                if(!SwingUtilities.isEventDispatchThread())
                    SwingUtilities.invokeAndWait(this);
                else
                    this.run();
            }
            catch(Throwable t)
            {
                logger.warn("Cannot dispatch on event dispatch thread", t);
                // if we cannot execute on event dispatch thread
                this.run();
            }

            return chatPanel;
        }

        /**
         * Runs on event dispatch thread.
         */
        public void run()
        {
            chatPanel = createChatPanel();
        }

        /**
         * The method that will create the panel.
         * @return the result chat panel.
         */
        protected abstract ChatPanel createChatPanel();
    }

    /**
     * Creates/Obtains chat panel on swing event dispatch thread.
     */
    private class MetaContactChatCreateRunnable
        extends AbstractChatPanelCreateRunnable
    {
        /**
         * The source meta contact.
         */
        private final MetaContact metaContact;

        /**
         * The protocol contact used for creating chat panel.
         */
        private final Contact protocolContact;

        /**
         * The contact resource, from which the message is sent.
         */
        private final ContactResource contactResource;

        /**
         * The message ID of the message to be excluded from
         * newly created chat panel.
         */
        private final String escapedMessageID;

        /**
         * Creates a chat.
         *
         * @param metaContact the from meta contact
         * @param protocolContact the from protocol contact
         * @param contactResource the contact resource, from which the message
         * is sent
         * @param escapedMessageID the identifier of the escaped message
         */
        private MetaContactChatCreateRunnable(MetaContact metaContact,
                                              Contact protocolContact,
                                              ContactResource contactResource,
                                              String escapedMessageID)
        {
            this.metaContact = metaContact;
            this.protocolContact = protocolContact;
            this.contactResource = contactResource;
            this.escapedMessageID = escapedMessageID;
        }

        /**
         * Runs on event dispatch thread.
         */
        @Override
        protected ChatPanel createChatPanel()
        {
            return getContactChat(
                metaContact,
                protocolContact,
                contactResource,
                true,
                this.escapedMessageID);
        }
    }

    /**
     * Creates chat room wrapper in event dispatch thread.
     */
    private class CreateChatRoomWrapperRunner
        extends AbstractChatPanelCreateRunnable
    {
        /**
         * The source chat room.
         */
        private ChatRoomWrapper chatRoomWrapper;

        /**
         * Constructs.
         * @param chatRoomWrapper the <tt>ChatRoomWrapper</tt> to use
         * for creating a panel.
         */
        private CreateChatRoomWrapperRunner(ChatRoomWrapper chatRoomWrapper)
        {
            this.chatRoomWrapper = chatRoomWrapper;
        }

        /**
         * Runs on event dispatch thread.
         */
        @Override
        protected ChatPanel createChatPanel()
        {
            return getMultiChatInternal(chatRoomWrapper, true);
        }
    }

    /**
     * Creates chat room wrapper in event dispatch thread.
     */
    private class CreateAdHocChatRoomWrapperRunner
        extends AbstractChatPanelCreateRunnable
    {
        /**
         * The source chat room.
         */
        private AdHocChatRoomWrapper chatRoomWrapper;

        /**
         * Constructs.
         * @param chatRoomWrapper the <tt>AdHocChatRoom</tt>, for which
         * the chat will be created.
         */
        private CreateAdHocChatRoomWrapperRunner(
            AdHocChatRoomWrapper chatRoomWrapper)
        {
            this.chatRoomWrapper = chatRoomWrapper;
        }

        /**
         * Runs on event dispatch thread.
         */
        @Override
        protected ChatPanel createChatPanel()
        {
            return getMultiChatInternal(chatRoomWrapper, true);
        }
    }

    /**
     * Creates chat room in event dispatch thread.
     */
    private class CreateChatRoomRunner
        extends AbstractChatPanelCreateRunnable
    {
        /**
         * The source chat room.
         */
        private ChatRoom chatRoom;

        private String escapedMessageID;

        /**
         * Constructs.
         * @param chatRoom the <tt>ChatRoom</tt> used to create the
         * corresponding <tt>ChatPanel</tt>.
         */
        private CreateChatRoomRunner(ChatRoom chatRoom,
                                    String escapedMessageID)
        {
            this.chatRoom = chatRoom;
            this.escapedMessageID = escapedMessageID;
        }

        /**
         * Runs on event dispatch thread.
         */
        @Override
        protected ChatPanel createChatPanel()
        {
            return getMultiChatInternal(chatRoom, true, escapedMessageID);
        }
    }

    /**
     * Creates chat room in event dispatch thread.
     */
    private class CreateAdHocChatRoomRunner
        extends AbstractChatPanelCreateRunnable
    {
        /**
         * The source chat room.
         */
        private AdHocChatRoom adHocChatRoom;

        private String escapedMessageID;

        /**
         * Constructs.
         * @param adHocChatRoom the <tt>AdHocChatRoom</tt> used to create
         * the corresponding <tt>ChatPanel</tt>.
         */
        private CreateAdHocChatRoomRunner(AdHocChatRoom adHocChatRoom,
                                    String escapedMessageID)
        {
            this.adHocChatRoom = adHocChatRoom;
            this.escapedMessageID = escapedMessageID;
        }

        /**
         * Runs on event dispatch thread.
         */
        @Override
        protected ChatPanel createChatPanel()
        {
            return getMultiChatInternal(adHocChatRoom, true, escapedMessageID);
        }
    }
}
