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
        if(!SwingUtilities.isEventDispatchThread())
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
            ChatWindow chatWindow = chatPanel.getChatWindow();

            if(!chatPanel.isShown())
                chatWindow.addChat(chatPanel);

            if(chatWindow.isVisible())
            {
                if(chatWindow.getExtendedState() != JFrame.ICONIFIED)
                {
                    if(ConfigurationManager.isAutoPopupNewMessage()
                            || setSelected)
                        chatWindow.toFront();
                }
                else
                {
                    if(setSelected)
                    {
                        chatWindow.setExtendedState(JFrame.NORMAL);
                        chatWindow.toFront();
                    }

                    String chatWindowTitle = chatWindow.getTitle();

                    if(!chatWindowTitle.startsWith("*"))
                        chatWindow.setTitle("*" + chatWindowTitle);
                }

                if(setSelected)
                {
                    chatWindow.setCurrentChatPanel(chatPanel);
                }
                else if(!chatWindow.getCurrentChatPanel().equals(chatPanel)
                    && chatWindow.getChatTabCount() > 0)
                {
                    chatWindow.highlightTab(chatPanel);

                    chatPanel.setCaretToEnd();
                }
            }
            else
            {
                chatWindow.setVisible(true);
                chatWindow.setCurrentChatPanel(chatPanel);

                chatPanel.setCaretToEnd();
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
     * Returns <tt>true</tt> if there is an opened <tt>ChatPanel</tt> for the
     * given <tt>AdHocChatRoom</tt>.
     * 
     * @param adHocChatRoomWrapper the <tt>AdHocChatRoomWrapper</tt> for which
     * the ad-hoc chat is about
     * @return <tt>true</tt> if there is an opened <tt>ChatPanel</tt> for the
     * given <tt>AdHocChatRoom</tt>
     */
    public boolean isChatOpenedFor(AdHocChatRoomWrapper adHocChatRoomWrapper)
    {
        return isChatOpenedForDescriptor(adHocChatRoomWrapper);
    }

    /**
     * Returns <tt>true</tt> if there is an opened <tt>ChatPanel</tt> for the
     * given <tt>ChatRoom</tt>.
     *
     * @param chatRoomWrapper the <tt>ChatRoomWrapper</tt>, for which the chat
     * is about
     * @return <tt>true</tt> if there is an opened <tt>ChatPanel</tt> for the
     * given <tt>ChatRoom</tt>
     */
    public boolean isChatOpenedFor(ChatRoomWrapper chatRoomWrapper)
    {
        return isChatOpenedForDescriptor(chatRoomWrapper);
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
    public void closeChat(ChatPanel chatPanel)
    {
        synchronized (syncChat)
        {
            if(containsChat(chatPanel))
            {
                ChatWindow chatWindow = chatPanel.getChatWindow();

                if (!chatPanel.isWriteAreaEmpty())
                {
                    SIPCommMsgTextArea msgText = new SIPCommMsgTextArea(
                        GuiActivator.getResources().getI18NString(
                            "service.gui.NON_EMPTY_CHAT_WINDOW_CLOSE"));

                    int answer = JOptionPane.showConfirmDialog(
                        chatWindow,
                        msgText,
                        GuiActivator.getResources().getI18NString(
                            "service.gui.WARNING"),
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.WARNING_MESSAGE);

                    if (answer == JOptionPane.OK_OPTION)
                    {
                        closeChatPanel(chatPanel);
                    }
                }
                else if (System.currentTimeMillis() - chatWindow
                    .getLastIncomingMsgTimestamp(chatPanel) < 2 * 1000)
                {
                    SIPCommMsgTextArea msgText
                        = new SIPCommMsgTextArea(GuiActivator.getResources()
                            .getI18NString(
                                "service.gui.CLOSE_CHAT_AFTER_NEW_MESSAGE"));

                    int answer = JOptionPane.showConfirmDialog(
                        chatWindow,
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
                        chatWindow,
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
     * Closes the chat window. Removes all contained chats and invokes
     * <code>setVisible(false)</code> to the window.
     * 
     * @param chatWindow the chat window
     */
    public void closeWindow(ChatWindow chatWindow)
    {
        synchronized (syncChat)
        {
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

                if (!chatPanel.isWriteAreaEmpty()
                    || chatPanel.containsActiveFileTransfers()
                    || System.currentTimeMillis() - chatWindow
                    .getLastIncomingMsgTimestamp(chatPanel) < 2 * 1000)
                {
                    activePanel = chatPanel;
                }
            }

            if (activePanel == null)
            {
                this.disposeChatWindow(chatWindow);
                return;
            }

            if (!activePanel.isWriteAreaEmpty())
            {
                SIPCommMsgTextArea msgText = new SIPCommMsgTextArea(
                    GuiActivator.getResources().getI18NString(
                        "service.gui.NON_EMPTY_CHAT_WINDOW_CLOSE"));
                int answer = JOptionPane.showConfirmDialog(
                    chatWindow,
                    msgText,
                    GuiActivator.getResources()
                        .getI18NString("service.gui.WARNING"),
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE);

                if (answer == JOptionPane.OK_OPTION)
                {
                    this.disposeChatWindow(chatWindow);
                }
            }
            else if (System.currentTimeMillis() - chatWindow
                .getLastIncomingMsgTimestamp(activePanel) < 2 * 1000)
            {
                SIPCommMsgTextArea msgText = new SIPCommMsgTextArea(
                    GuiActivator.getResources()
                    .getI18NString("service.gui.CLOSE_CHAT_AFTER_NEW_MESSAGE"));

                int answer = JOptionPane.showConfirmDialog(
                    chatWindow,
                    msgText,
                    GuiActivator.getResources()
                        .getI18NString("service.gui.WARNING"),
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);

                if (answer == JOptionPane.OK_OPTION)
                {
                    this.disposeChatWindow(chatWindow);
                }
            }
            else if (activePanel.containsActiveFileTransfers())
            {
                SIPCommMsgTextArea msgText
                    = new SIPCommMsgTextArea(GuiActivator.getResources()
                        .getI18NString(
                            "service.gui.CLOSE_CHAT_ACTIVE_FILE_TRANSFER"));

                int answer = JOptionPane.showConfirmDialog(
                    chatWindow,
                    msgText,
                    GuiActivator.getResources()
                        .getI18NString("service.gui.WARNING"),
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE);

                if (answer == JOptionPane.OK_OPTION)
                {
                    for (ChatPanel chatPanel : chatPanels)
                    {
                        chatPanel.cancelActiveFileTransfers();
                    }

                    this.disposeChatWindow(chatWindow);
                }
            }
        }
    }

    /**
     * Returns the chat panel corresponding to the given meta contact
     *
     * @param metaContact the meta contact.
     * @return the chat panel corresponding to the given meta contact
     */
    public ChatPanel getContactChat(MetaContact metaContact)
    {
        synchronized (syncChat)
        {
            ChatPanel chatPanel = findChatPanelForDescriptor(metaContact);

            return (chatPanel != null) ? chatPanel : createChat(metaContact);
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
        synchronized (syncChat)
        {
            ChatPanel chatPanel = findChatPanelForDescriptor(metaContact);

            return
                (chatPanel != null)
                    ? chatPanel
                    : createChat(
                            metaContact,
                            protocolContact,
                            escapedMessageID);
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
                        = firstChatPanel.getChatWindow().getCurrentChatPanel();
                }
            }
            else
            {
                while (chatPanelsIter.hasNext())
                {
                    ChatPanel chatPanel = chatPanelsIter.next();

                    if (chatPanel.getChatWindow().isFocusOwner())
                        selectedChat = chatPanel;
                }
            }

            return selectedChat;
        }
    }

    /**
     * Returns the chat panel corresponding to the given chat room wrapper.
     *
     * @param chatRoomWrapper the chat room wrapper, corresponding to the chat
     * room for which the chat panel is about
     * @return the chat panel corresponding to the given chat room
     */
    public ChatPanel getMultiChat(ChatRoomWrapper chatRoomWrapper)
    {
        synchronized (syncChat)
        {
            ChatPanel chatPanel = findChatPanelForDescriptor(chatRoomWrapper);

            return
                (chatPanel != null) ? chatPanel : createChat(chatRoomWrapper);
        }
    }

    /**
     * Returns the chat panel corresponding to the given chat room wrapper.
     *
     * @param chatRoomWrapper the ad-hoc chat room wrapper, corresponding to the
     * ad-hoc chat room for which the chat panel is about
     * @return the chat panel corresponding to the given chat room
     */
    public ChatPanel getMultiChat(AdHocChatRoomWrapper chatRoomWrapper)
    {
        synchronized (syncChat)
        {
            ChatPanel chatPanel = findChatPanelForDescriptor(chatRoomWrapper);

            return
                (chatPanel != null) ? chatPanel : createChat(chatRoomWrapper);
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
     * Closes the selected chat tab or the window if there are no tabs.
     *
     * @param chatPanel the chat panel to close.
     */
    private void closeChatPanel(ChatPanel chatPanel)
    {
        ChatWindow chatWindow = chatPanel.getChatWindow();

        chatWindow.removeChat(chatPanel);

        if (chatWindow.getChatCount() == 0)
            disposeChatWindow(chatWindow);

        boolean isChatPanelContained;
        synchronized (chatPanels)
        {
            isChatPanelContained = chatPanels.remove(chatPanel);
        }

        if (isChatPanelContained)
            chatPanel.dispose();
    }

    /**
     * Creates a chat for the given meta contact. If the most connected proto
     * contact of the meta contact is offline choose the proto contact that
     * supports offline messaging.
     *
     * @param metaContact the meta contact for the chat
     * @return the newly created ChatPanel
     */
    private ChatPanel createChat(MetaContact metaContact)
    {
        Contact defaultContact = metaContact.getDefaultContact();
        ProtocolProviderService defaultProvider
            = defaultContact.getProtocolProvider();
        OperationSetBasicInstantMessaging defaultIM
            = (OperationSetBasicInstantMessaging)
                defaultProvider
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
                    = (OperationSetBasicInstantMessaging)
                        protoContactProvider
                            .getOperationSet(
                                OperationSetBasicInstantMessaging.class);

                if(protoContactIM.isOfflineMessagingSupported()
                        && protoContactProvider.isRegistered())
                {
                    defaultContact = contact;
                }
            }
        }

        return createChat(metaContact, defaultContact, null);
    }

    /**
     * Creates a <tt>ChatPanel</tt> for the given contact and saves it in the
     * list of created <tt>ChatPanel</tt>s.
     *
     * @param metaContact The MetaContact for this chat.
     * @param protocolContact The protocol contact.
     * @param escapedMessageID the message ID of the message that should be
     * excluded from the history when the last one is loaded in the chat.
     * @return The <code>ChatPanel</code> newly created.
     */
    private ChatPanel createChat(   MetaContact metaContact,
                                    Contact protocolContact,
                                    String escapedMessageID)
    {
        ChatWindow chatWindow = getChatWindow();
        ChatPanel chatPanel = new ChatPanel(chatWindow);

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

        return chatPanel;
    }

    /**
     * Gets a <tt>ChatWindow</tt> instance. If there is no existing
     * <tt>ChatWindow</tt> or chats are configured to be displayed in their own
     * <tt>ChatWindow</tt>s instead of arranged in tabs in a single
     * <tt>ChatWindow</tt>, creates a new one.
     *
     * @return a <tt>ChatWindow</tt> instance
     */
    private ChatWindow getChatWindow()
    {
        ChatWindow chatWindow;

        if (ConfigurationManager.isMultiChatWindowEnabled())
        {
            Iterator<ChatPanel> chatPanelsIter = chatPanels.iterator();

            /*
             * If we're in a tabbed window we're looking for the chat window
             * through one of the existing chats.
             */
            if (chatPanelsIter.hasNext())
                chatWindow = chatPanelsIter.next().getChatWindow();
            else
            {
                chatWindow = new ChatWindow();
                GuiActivator.getUIService().registerExportedWindow(chatWindow);
            }
        }
        else
            chatWindow = new ChatWindow();
        return chatWindow;
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
    private ChatPanel createChat( ChatRoomWrapper chatRoomWrapper,
                                            String escapedMessageID)
    {
        ChatWindow chatWindow = getChatWindow();
        ChatPanel chatPanel = new ChatPanel(chatWindow);

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
        ChatWindow chatWindow = getChatWindow();
        ChatPanel chatPanel = new ChatPanel(chatWindow);

        AdHocConferenceChatSession chatSession
            = new AdHocConferenceChatSession(chatPanel, chatRoomWrapper);

        chatPanel.setChatSession(chatSession);

        synchronized (chatPanels)
        {
            this.chatPanels.add(chatPanel);
        }

        if (ConfigurationManager.isHistoryShown())
            chatPanel.loadHistory(escapedMessageID);

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
     * Disposes the chat window.
     *
     * @param chatWindow the <tt>ChatWindow</tt> to dispose of
     */
    private void disposeChatWindow(ChatWindow chatWindow)
    {
        for (ChatPanel chatPanel : chatPanels)
            chatPanel.dispose();

        synchronized (chatPanels)
        {
            chatPanels.clear();
        }

        if (chatWindow.getChatCount() > 0)
            chatWindow.removeAllChats();

        /*
         * The ChatWindow should seize to exist so we don't want any strong
         * references to it i.e. it cannot be exported anymore.
         */
        GuiActivator.getUIService().unregisterExportedWindow(chatWindow);
        chatWindow.dispose();

        ContactList clist
            = GuiActivator.getUIService().getMainFrame()
                .getContactListPanel().getContactList();

        // Remove the envelope from the all active contacts in the contact list.
        clist.removeAllActiveContacts();
    }
}
