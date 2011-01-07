/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chat;

import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.main.chat.conference.*;
import net.java.sip.communicator.impl.gui.main.chatroomslist.*;
import net.java.sip.communicator.impl.gui.main.contactlist.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.gui.event.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * Manages chat windows and panels.
 *
 * @author Yana Stamcheva
 * @author Valentin Martinet
 * @author Lubomir Marinov
 */
public class ChatWindowManager
{
    private final java.util.List<ChatPanel> chatPanels
        = new ArrayList<ChatPanel>();

    private ArrayList <ChatListener> newChatListeners
        = new ArrayList <ChatListener> ();

    private final Object syncChat = new Object();

    /**
     * Opens the specified <tt>ChatPanel</tt> and optinally brings it to the
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

        synchronized (syncChat)
        {
            ChatContainer chatContainer = chatPanel.getChatContainer();

            if(!chatPanel.isShown())
                chatContainer.addChat(chatPanel);

            if(chatContainer.getFrame().isVisible())
            {
                chatContainer.openChat(chatPanel, setSelected);
            }
            else
            {
                chatContainer.getFrame().setVisible(true);
                chatContainer.setCurrentChat(chatPanel);
            }
        }
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
        synchronized (syncChat)
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

        synchronized (syncChat)
        {
            if(containsChat(chatPanel))
            {
                long lastMsgTimestamp = chatPanel.getChatConversationPanel()
                    .getLastIncomingMsgTimestamp();

                if (!chatPanel.isWriteAreaEmpty())
                {
                    SIPCommMsgTextArea msgText = new SIPCommMsgTextArea(
                        GuiActivator.getResources().getI18NString(
                            "service.gui.NON_EMPTY_CHAT_WINDOW_CLOSE"));

                    int answer = JOptionPane.showConfirmDialog(
                        null,
                        msgText,
                        GuiActivator.getResources().getI18NString(
                            "service.gui.WARNING"),
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.WARNING_MESSAGE);

                    if (answer == JOptionPane.OK_OPTION)
                        closeChatPanel(chatPanel);
                }
                else if (System.currentTimeMillis() - lastMsgTimestamp
                                                                    < 2 * 1000)
                {
                    SIPCommMsgTextArea msgText
                        = new SIPCommMsgTextArea(GuiActivator.getResources()
                            .getI18NString(
                                "service.gui.CLOSE_CHAT_AFTER_NEW_MESSAGE"));

                    int answer = JOptionPane.showConfirmDialog(
                        null,
                        msgText,
                        GuiActivator.getResources()
                            .getI18NString("service.gui.WARNING"),
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.WARNING_MESSAGE);

                    if (answer == JOptionPane.OK_OPTION)
                        closeChatPanel(chatPanel);
                }
                else if (chatPanel.containsActiveFileTransfers())
                {
                    SIPCommMsgTextArea msgText
                        = new SIPCommMsgTextArea(GuiActivator.getResources()
                            .getI18NString(
                                "service.gui.CLOSE_CHAT_ACTIVE_FILE_TRANSFER"));

                    int answer = JOptionPane.showConfirmDialog(
                        null,
                        msgText,
                        GuiActivator.getResources()
                            .getI18NString("service.gui.WARNING"),
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.WARNING_MESSAGE);

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
        List<ChatPanel> chatPanelsToDispose = chatContainer.getChats();

        for (ChatPanel chatPanel : chatPanelsToDispose)
            chatPanel.dispose();

        synchronized (chatPanels)
        {
            chatPanels.removeAll(chatPanelsToDispose);
        }

        if (chatContainer.getChatCount() > 0)
            chatContainer.removeAllChats();

        // Remove the envelope from the all active contacts in the contact list.
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
        synchronized (syncChat)
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

                long lastMsgTimestamp = chatPanel.getChatConversationPanel()
                                                .getLastIncomingMsgTimestamp();
                if (!chatPanel.isWriteAreaEmpty()
                    || chatPanel.containsActiveFileTransfers()
                    || System.currentTimeMillis() - lastMsgTimestamp < 2 * 1000)
                {
                    activePanel = chatPanel;
                }
            }

            if (activePanel == null)
            {
                this.closeAllChats(chatContainer);
                return;
            }

            long lastMsgTimestamp = activePanel.getChatConversationPanel()
                                                .getLastIncomingMsgTimestamp();

            if (!activePanel.isWriteAreaEmpty())
            {
                SIPCommMsgTextArea msgText = new SIPCommMsgTextArea(
                    GuiActivator.getResources().getI18NString(
                        "service.gui.NON_EMPTY_CHAT_WINDOW_CLOSE"));
                int answer = JOptionPane.showConfirmDialog(
                    chatContainer.getFrame(),
                    msgText,
                    GuiActivator.getResources()
                        .getI18NString("service.gui.WARNING"),
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE);

                if (answer == JOptionPane.OK_OPTION)
                    this.closeAllChats(chatContainer);
            }
            else if (System.currentTimeMillis() - lastMsgTimestamp < 2 * 1000)
            {
                SIPCommMsgTextArea msgText = new SIPCommMsgTextArea(
                    GuiActivator.getResources()
                    .getI18NString("service.gui.CLOSE_CHAT_AFTER_NEW_MESSAGE"));

                int answer = JOptionPane.showConfirmDialog(
                    chatContainer.getFrame(),
                    msgText,
                    GuiActivator.getResources()
                        .getI18NString("service.gui.WARNING"),
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);

                if (answer == JOptionPane.OK_OPTION)
                    this.closeAllChats(chatContainer);
            }
            else if (activePanel.containsActiveFileTransfers())
            {
                SIPCommMsgTextArea msgText
                    = new SIPCommMsgTextArea(GuiActivator.getResources()
                        .getI18NString(
                            "service.gui.CLOSE_CHAT_ACTIVE_FILE_TRANSFER"));

                int answer = JOptionPane.showConfirmDialog(
                    chatContainer.getFrame(),
                    msgText,
                    GuiActivator.getResources()
                        .getI18NString("service.gui.WARNING"),
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE);

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
        return getContactChat(metaContact, null, create, null);
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
        return getContactChat(metaContact, protocolContact, null);
    }

    /**
     * Returns the chat panel corresponding to the given meta contact
     *
     * @param metaContact the meta contact.
     * @param protocolContact the protocol specific contact
     * @param escapedMessageID the message ID of the message that should be
     * excluded from the history when the last one is loaded in the chat
     * @return the chat panel corresponding to the given meta contact
     */
    public ChatPanel getContactChat(MetaContact metaContact,
                                    Contact protocolContact,
                                    String escapedMessageID)
    {
        return
            getContactChat(
                metaContact,
                protocolContact,
                true,
                escapedMessageID);
    }

    /**
     * Gets the <tt>ChatPanel</tt> corresponding to the specified
     * <tt>MetaContact</tt> and optionally creates it if it does not exist.
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
            boolean create,
            String escapedMessageID)
    {
        synchronized (syncChat)
        {
            ChatPanel chatPanel = findChatPanelForDescriptor(metaContact);

            if ((chatPanel == null) && create)
                chatPanel
                    = createChat(
                        metaContact,
                        protocolContact,
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

        synchronized (syncChat)
        {
            if (ConfigurationManager.isMultiChatWindowEnabled())
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
    public ChatPanel getMultiChat(
            ChatRoomWrapper chatRoomWrapper,
            boolean create)
    {
        synchronized (syncChat)
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
        synchronized (syncChat)
        {
            ChatPanel chatPanel = findChatPanelForDescriptor(chatRoomWrapper);

            if ((chatPanel == null) && create)
                chatPanel = createChat(chatRoomWrapper);
            return chatPanel;
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
    public ChatPanel getMultiChat(ChatRoom chatRoom,
                                  boolean create,
                                  String escapedMessageID)
    {
        synchronized (syncChat)
        {
            ChatRoomList chatRoomList
                = GuiActivator
                    .getUIService()
                        .getConferenceChatManager().getChatRoomList();

            // Search in the chat room's list for a chat room that correspond
            // to the given one.
            ChatRoomWrapper chatRoomWrapper
                = chatRoomList.findChatRoomWrapperFromChatRoom(chatRoom);

            if ((chatRoomWrapper == null) && create)
            {
                ChatRoomProviderWrapper parentProvider
                    = chatRoomList.findServerWrapperFromProvider(
                        chatRoom.getParentProvider());

                chatRoomWrapper = new ChatRoomWrapper(parentProvider, chatRoom);

                chatRoomList.addChatRoom(chatRoomWrapper);
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
        synchronized (syncChat)
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
        SwingUtilities.invokeLater(new RunChatWindow(
            metaContact, protocolContact, isSmsMessage));
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

        chatContainer.removeChat(chatPanel);

        boolean isChatPanelContained;
        synchronized (chatPanels)
        {
            isChatPanelContained = chatPanels.remove(chatPanel);
        }

        if (isChatPanelContained)
            chatPanel.dispose();
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
     * @param escapedMessageID the message ID of the message that should be
     * excluded from the history when the last one is loaded in the chat.
     * @return The <code>ChatPanel</code> newly created.
     */
    private ChatPanel createChat(   MetaContact metaContact,
                                    Contact protocolContact,
                                    String escapedMessageID)
    {
        if (protocolContact == null)
            protocolContact = getDefaultContact(metaContact);

        ChatContainer chatContainer = getChatContainer();
        ChatPanel chatPanel = new ChatPanel(chatContainer);

        MetaContactChatSession chatSession
            = new MetaContactChatSession(   chatPanel,
                                            metaContact,
                                            protocolContact);

        chatPanel.setChatSession(chatSession);

        synchronized (chatPanels)
        {
            this.chatPanels.add(chatPanel);
        }

        if (ConfigurationManager.isHistoryShown())
            chatPanel.loadHistory(escapedMessageID);

        // notifies listeners interested in the chat's instantiation
        synchronized (newChatListeners)
        {
            for (ChatListener listener : newChatListeners)
            {
                listener.chatCreated(chatPanel);
            }
        }

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
        if (ConfigurationManager.isMultiChatWindowEnabled())
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
                GuiActivator.getUIService()
                    .registerExportedWindow((ChatWindow) chatContainer);
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

        if (ConfigurationManager.isHistoryShown())
            chatPanel.loadHistory(escapedMessageID);

        // notifies listeners interested in the chat's instantiation
        synchronized (newChatListeners)
        {
            for (ChatListener listener : newChatListeners)
            {
                listener.chatCreated(chatPanel);
            }
        }

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

        if (ConfigurationManager.isHistoryShown())
            chatPanel.loadHistory(escapedMessageID);

        // notifies listeners interested in the chat's instantiation
        synchronized (newChatListeners)
        {
            for (ChatListener listener : newChatListeners)
            {
                listener.chatCreated(chatPanel);
            }
        }

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
        private MetaContact metaContact;

        private Contact protocolContact;

        private boolean isSmsSelected = false;

        /**
         * Creates an instance of <tt>RunMessageWindow</tt> by specifying the
         *
         * @param metaContact the meta contact to which we will talk.
         */
        public RunChatWindow(MetaContact metaContact)
        {
            this.metaContact = metaContact;
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
            this.metaContact = metaContact;
            this.protocolContact = protocolContact;
        }

        /**
         * Creates a chat window
         *
         * @param metaContact
         * @param protocolContact
         * @param isSmsSelected
         */
        public RunChatWindow(   MetaContact metaContact,
                                Contact protocolContact,
                                boolean isSmsSelected)
        {
            this.metaContact = metaContact;
            this.protocolContact = protocolContact;
            this.isSmsSelected = isSmsSelected;
        }

        /**
         * Opens a chat window
         */
        public void run()
        {
            ChatPanel chatPanel
                = getContactChat(metaContact, protocolContact);

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
        synchronized (syncChat)
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
        synchronized (newChatListeners)
        {
            newChatListeners.add(listener);
        }
    }

    /**
     * Removes the registration of a <tt>NewChatListener</tt>.
     * @param listener listener to be unregistered
     */
    public void removeChatListener(ChatListener listener)
    {
        synchronized (newChatListeners)
        {
            newChatListeners.remove(listener);
        }
    }
}
