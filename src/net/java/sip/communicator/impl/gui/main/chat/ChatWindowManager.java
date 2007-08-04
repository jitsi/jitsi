/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chat;

import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.GuiActivator;
import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.main.chat.conference.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * Manages chat windows and panels.
 *
 * @author Yana Stamcheva
 */
public class ChatWindowManager
{
    private Logger logger = Logger.getLogger(ChatWindowManager.class);

    private ChatWindow chatWindow;

    private Hashtable chats = new Hashtable();

    private MainFrame mainFrame;

    private Object syncChat = new Object();

    public ChatWindowManager(MainFrame mainFrame)
    {
        this.mainFrame = mainFrame;
    }

    /**
     * Opens a the specified chatPanel and brings it to the front if so
     * specified.
     *
     * @param chatPanel the chat panel that we will be opening
     * @param setSelected specifies whether we should bring the chat to front
     * after creating it.
     */
    public void openChat(ChatPanel chatPanel, boolean setSelected)
    {
        synchronized (syncChat)
        {
            ChatWindow chatWindow = chatPanel.getChatWindow();

            boolean isChatVisible = chatPanel.isShown();
            
            if(!isChatVisible)
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
                    
                    if(!chatWindow.getTitle().startsWith("*"))
                        chatWindow.setTitle(
                            "*" + chatWindow.getTitle());
                }
                
                if(setSelected)
                {
                    chatWindow.setCurrentChatPanel(chatPanel);
                }
                else if(!chatWindow.getCurrentChatPanel().equals(chatPanel)
                    && chatWindow.getChatTabCount() > 0)
                {
                    chatPanel.getChatWindow().highlightTab(chatPanel);
                    
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
     * Returns TRUE if there is an opened <tt>ChatPanel</tt> for the given
     * <tt>MetaContact</tt>.
     * @param metaContact the <tt>MetaContact</tt>, for which the chat is about
     * @return TRUE if there is an opened <tt>ChatPanel</tt> for the given
     * <tt>MetaContact</tt>
     */
    public boolean isChatOpenedForContact(MetaContact metaContact)
    {
        synchronized (syncChat)
        {
            if(containsChat(metaContact)
                && getChat(metaContact).isVisible())
                return true;
            
            return false;
        }
    }
    
    /**
     * Returns TRUE if there is an opened <tt>ChatPanel</tt> for the given
     * <tt>ChatRoom</tt>.
     * @param chatRoomWrapper the <tt>ChatRoomWrapper</tt>, for which the chat
     * is about
     * @return TRUE if there is an opened <tt>ChatPanel</tt> for the given
     * <tt>ChatRoom</tt>
     */
    public boolean isChatOpenedForChatRoom(ChatRoomWrapper chatRoomWrapper)
    {   
        synchronized (syncChat)
        {
            if(containsChat(chatRoomWrapper)
                && getChat(chatRoomWrapper).isVisible())
                return true;
            
            return false;
        }
    }

    /**
     * Returns TRUE if there is an opened <tt>ChatPanel</tt> for the given
     * <tt>ChatRoom</tt>.
     * @param chatRoom the <tt>ChatRoom</tt>, for which the chat is about
     * @return TRUE if there is an opened <tt>ChatPanel</tt> for the given
     * <tt>ChatRoom</tt>
     */
    public boolean isChatOpenedForChatRoom(ChatRoom chatRoom)
    {   
        synchronized (syncChat)
        {
            ChatRoomWrapper chatRoomWrapper = null;
            
            Enumeration chatKeys = chats.keys();
            while(chatKeys.hasMoreElements())
            {
                Object o = chatKeys.nextElement();
                
                if(o instanceof ChatRoomWrapper)
                {
                    if(((ChatRoomWrapper)o).getChatRoom()
                        .equals(chatRoom))
                    {
                        chatRoomWrapper = (ChatRoomWrapper)o;
                        
                        break;
                    }
                }
            }
            
            if(containsChat(chatRoomWrapper)
                && getChat(chatRoomWrapper).isVisible())
                return true;
            
            return false;
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
                    SIPCommMsgTextArea msgText = new SIPCommMsgTextArea(Messages
                        .getI18NString("nonEmptyChatWindowClose").getText());
                    int answer = JOptionPane.showConfirmDialog(chatWindow,
                        msgText, Messages.getI18NString("warning").getText(),
                        JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);

                    if (answer == JOptionPane.OK_OPTION)
                    {
                        closeChatPanel(chatPanel);
                    }
                }
                else if (System.currentTimeMillis() - chatWindow
                    .getLastIncomingMsgTimestamp(chatPanel).getTime() < 2 * 1000)
                {
                    SIPCommMsgTextArea msgText = new SIPCommMsgTextArea(Messages
                        .getI18NString("closeChatAfterNewMsg").getText());

                    int answer = JOptionPane.showConfirmDialog(chatWindow,
                        msgText, Messages.getI18NString("warning").getText(),
                        JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);

                    if (answer == JOptionPane.OK_OPTION)
                        closeChatPanel(chatPanel);
                }
                else {
                    closeChatPanel(chatPanel);
                }
            }
        }
    }

    /**
     * Closes the chat window. Removes all contained chats and invokes
     * setVisible(false) to the window.
     */
    public void closeWindow()
    {
        synchronized (syncChat)
        {
            ChatPanel chatPanel = chatWindow.getCurrentChatPanel();
            
            if (!chatPanel.isWriteAreaEmpty())
            {
                SIPCommMsgTextArea msgText = new SIPCommMsgTextArea(Messages
                    .getI18NString("nonEmptyChatWindowClose").getText());
                int answer = JOptionPane.showConfirmDialog(chatWindow,
                    msgText, Messages.getI18NString("warning").getText(),
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);

                if (answer == JOptionPane.OK_OPTION)
                {
                    chatWindow.removeAllChats();
                    chatWindow.dispose();

                    synchronized (chats)
                    {
                        chats.clear();
                    }
                }
            }
            else if (System.currentTimeMillis() - chatWindow
                .getLastIncomingMsgTimestamp(chatPanel).getTime() < 2 * 1000)
            {
                SIPCommMsgTextArea msgText = new SIPCommMsgTextArea(Messages
                    .getI18NString("closeChatAfterNewMsg").getText());

                int answer = JOptionPane.showConfirmDialog(chatWindow,
                    msgText, Messages.getI18NString("warning").getText(),
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);

                if (answer == JOptionPane.OK_OPTION)
                {
                    chatWindow.removeAllChats();
                    chatWindow.dispose();

                    synchronized (chats)
                    {
                        chats.clear();
                    }
                }
            }
            else
            {
                chatWindow.removeAllChats();
                chatWindow.dispose();

                synchronized (chats)
                {
                    chats.clear();
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
            if(containsChat(metaContact))
            {
                return getChat(metaContact);
            }
            else
            {
                return createChat(metaContact);
            }
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
        synchronized (syncChat)
        {
            if(containsChat(metaContact))
            {
                return getChat(metaContact);                
            }
            else         
                return createChat(metaContact, protocolContact);
        }
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
        Contact protocolContact, String escapedMessageID)
    {
        synchronized (syncChat)
        {
            if(containsChat(metaContact))
            {
                return getChat(metaContact);
            }
            else         
                return createChat(metaContact, protocolContact, escapedMessageID);
        }
    }
    
    /**
     * Returns the currently selected <tt>ChatPanel</tt>.
     * @return the currently selected <tt>ChatPanel</tt>
     */
    public ChatPanel getSelectedChat()
    {
        synchronized (syncChat)
        {
            return chatWindow.getCurrentChatPanel();
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
            if(containsChat(chatRoomWrapper))
            {
                return getChat(chatRoomWrapper);
            }
            else         
                return createChat(chatRoomWrapper);
        }
    }
    
    /**
     * Returns the chat panel corresponding to the given chat room.
     *
     * @param chatRoom the chat room, for which the chat panel is about
     * @return the chat panel corresponding to the given chat room
     */
    public ChatPanel getMultiChat(ChatRoom chatRoom)
    {
        synchronized (syncChat)
        {   
            Enumeration chatKeys = chats.keys();
            while(chatKeys.hasMoreElements())
            {
                Object o = chatKeys.nextElement();
                
                if(o instanceof ChatRoomWrapper)
                {
                    if(((ChatRoomWrapper)o).getChatRoom()
                            .equals(chatRoom))
                    {
                        return getChat((ChatRoomWrapper) o);
                    }
                }
            }
            
            // Search in the chat room's list for a chat room that correspond
            // to the given one.
            ChatRoomWrapper chatRoomWrapper
                = mainFrame.getChatRoomsListPanel().getChatRoomsList()
                    .findChatRoomWrapperFromChatRoom(chatRoom);
            
            if(chatRoomWrapper == null)
                chatRoomWrapper = new ChatRoomWrapper(chatRoom);
            
            return createChat(chatRoomWrapper);
        }
    }
            
    /**
     * Closes the selected chat tab or the window if there are no tabs.
     *
     * @param chatPanel the chat panel to close.
     */
    private void closeChatPanel(ChatPanel chatPanel)
    {        
        this.chatWindow.removeChat(chatPanel);
        
        synchronized (chats)
        {
            chats.remove(chatPanel.getChatIdentifier());
        }
    }

    /**
     * Creates a chat for the given meta contact. If the most connected proto
     * contact of the meta contact is offline choose the proto contact that
     * supports offline messaging.
     *
     * @param metaContact the meta contact for the chat
     *
     * @return the newly created ChatPanel
     */
    private ChatPanel createChat(MetaContact metaContact)
    {
        Contact defaultContact = metaContact.getDefaultContact();

        ProtocolProviderService defaultProvider
            = defaultContact.getProtocolProvider();

        OperationSetBasicInstantMessaging
            defaultIM = (OperationSetBasicInstantMessaging)
                defaultProvider.getOperationSet(
                        OperationSetBasicInstantMessaging.class);

        ProtocolProviderService protoContactProvider;
        OperationSetBasicInstantMessaging protoContactIM;

        if (defaultContact.getPresenceStatus().getStatus() < 1
                && (!defaultIM.isOfflineMessagingSupported()
                        || !defaultProvider.isRegistered()))
        {
            Iterator protoContacts = metaContact.getContacts();

            while(protoContacts.hasNext())
            {
                Contact contact = (Contact) protoContacts.next();

                protoContactProvider = contact.getProtocolProvider();

                protoContactIM = (OperationSetBasicInstantMessaging)
                    protoContactProvider.getOperationSet(
                        OperationSetBasicInstantMessaging.class);

                if(protoContactIM.isOfflineMessagingSupported()
                        && protoContactProvider.isRegistered())
                {
                    defaultContact = contact;
                }
            }
        }

        return createChat(metaContact, defaultContact);
    }


    /**
     * Creates a <tt>ChatPanel</tt> for the given contact and saves it in the
     * list ot created <tt>ChatPanel</tt>s.
     *
     * @param contact The MetaContact for this chat.
     * @param protocolContact The protocol contact.
     * @return The <code>ChatPanel</code> newly created.
     */
    private ChatPanel createChat(MetaContact contact, Contact protocolContact)
    {
        return createChat(contact, protocolContact, null);
    }

    /**
     * Creates a <tt>ChatPanel</tt> for the given contact and saves it in the
     * list ot created <tt>ChatPanel</tt>s.
     *
     * @param contact The MetaContact for this chat.
     * @param protocolContact The protocol contact.
     * @param escapedMessageID the message ID of the message that should be
     * excluded from the history when the last one is loaded in the chat.
     * @return The <code>ChatPanel</code> newly created.
     */
    private ChatPanel createChat(MetaContact contact,
        Contact protocolContact, String escapedMessageID)
    {        
        ChatWindow chatWindow;

        if(Constants.TABBED_CHAT_WINDOW)
        {
            if(this.chatWindow == null)
            {
                this.chatWindow = new ChatWindow(mainFrame);
                
                GuiActivator.getUIService()
                    .registerExportedWindow(this.chatWindow);
            }
            
            chatWindow = this.chatWindow;
        }
        else
        {
            chatWindow = new ChatWindow(mainFrame);

            this.chatWindow = chatWindow;
        }

        ChatPanel chatPanel
            = new MetaContactChatPanel(chatWindow, contact, protocolContact);

        synchronized (chats)
        {
            this.chats.put(contact, chatPanel);
        }

        if(escapedMessageID != null)
            chatPanel.loadHistory(escapedMessageID);
        else
            chatPanel.loadHistory();

        return chatPanel;
    }

    /**
     * Creates a <tt>ChatPanel</tt> for the given <tt>ChatRoom</tt> and saves it
     * in the list ot created <tt>ChatPanel</tt>s.
     *
     * @param chatRoom the <tt>ChatRoom</tt>, for which the chat will be created
     * @return The <code>ChatPanel</code> newly created.
     */
    private ChatPanel createChat(ChatRoomWrapper chatRoomWrapper)
    {
        ChatWindow chatWindow;

        if(Constants.TABBED_CHAT_WINDOW)
        {
            if(this.chatWindow == null)
            {
                this.chatWindow = new ChatWindow(mainFrame);
                
                GuiActivator.getUIService()
                    .registerExportedWindow(this.chatWindow);
            }
            
            chatWindow = this.chatWindow;
        }
        else
        {
            chatWindow = new ChatWindow(mainFrame);
            GuiActivator.getUIService().registerExportedWindow(chatWindow);
            
            this.chatWindow = chatWindow;
        }

        ChatPanel chatPanel
            = new ConferenceChatPanel(chatWindow, chatRoomWrapper);

        synchronized (chats)
        {
            this.chats.put(chatRoomWrapper, chatPanel);
        }

        return chatPanel;
    }

    /**
     * Returns TRUE if this chat window contains a chat for the given contact,
     * FALSE otherwise.
     *
     * @param key the key, which corresponds to the chat we are looking for. It
     * could be a <tt>MetaContact</tt> in the case of single user chat and
     * a <tt>ChatRoom</tt> in the case of a multi user chat
     * @return TRUE if this chat window contains a chat corresponding to the
     * given key, FALSE otherwise
     */
    private boolean containsChat(Object key)
    {
        synchronized (chats)
        {
            return chats.containsKey(key);
        }
    }

    /**
     * Returns TRUE if this chat window contains the given chatPanel,
     * FALSE otherwise.
     *
     * @param chatPanel the chat panel that we're looking for.
     * @return TRUE if this chat window contains the given chatPanel,
     * FALSE otherwise
     */
    private boolean containsChat(ChatPanel chatPanel)
    {
        synchronized (chats)
        {
            return chats.containsValue(chatPanel);
        }
    }
    
    /**
     * Returns the <tt>ChatPanel</tt> corresponding to the given meta contact.
     * 
     * @param key the key, which corresponds to the chat we are looking for. It
     * could be a <tt>MetaContact</tt> in the case of single user chat and
     * a <tt>ChatRoom</tt> in the case of a multi user chat
     * @return the <tt>ChatPanel</tt> corresponding to the given meta contact
     */
    private ChatPanel getChat(Object key)
    {
        synchronized (chats)
        {
            return (ChatPanel) chats.get(key);
        }
    }
}
