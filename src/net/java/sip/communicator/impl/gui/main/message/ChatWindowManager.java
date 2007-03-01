/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.message;

import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.main.contactlist.*;
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
        
        this.chatWindow = new ChatWindow(mainFrame);
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

            if(!chatPanel.isWindowVisible())
                chatWindow.addChat(chatPanel);

            if(chatWindow.isVisible())
            {   
                if (ConfigurationManager.isAutoPopupNewMessage() || setSelected)
                {   
                    if(chatWindow.getState() == JFrame.ICONIFIED)
                        chatWindow.setState(JFrame.NORMAL);

                    chatWindow.toFront();
                }
                else
                {
                    if (chatWindow.getState() == JFrame.ICONIFIED
                        && !chatWindow.getTitle().startsWith("*"))
                    {
                        chatWindow.setTitle(
                                "*" + chatWindow.getTitle());
                    }
                }
            }
            else
            {
                if(!setSelected)
                    chatWindow.setCurrentChatPanel(chatPanel);
                
                chatWindow.setVisible(true);
            }

            chatPanel.setCaretToEnd();
            
            if(setSelected)
                chatWindow.setCurrentChatPanel(chatPanel);
            else
            {
                if(chatWindow.getChatTabCount() > 0)
                    chatPanel.getChatWindow().highlightTab(chatPanel);
            }
        }
    }
    
    public boolean isChatOpenedForContact(MetaContact metaContact)
    {
        if(containsContactChat(metaContact)
            && getContactChat(metaContact).isVisible())
            return true;
        
        return false;
    }

    /**
     * Closes the chat corresponding to the given meta contact.
     *
     * @param metaContact the meta contact
     */
    public void closeChat(MetaContact metaContact)
    {
        if(containsContactChat(metaContact))
            closeChat(getContactChat(metaContact));
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
            if(containsContactChat(chatPanel))
            {
                ChatWindow chatWindow = chatPanel.getChatWindow();

                if (!chatPanel.isWriteAreaEmpty())
                {
                    SIPCommMsgTextArea msgText = new SIPCommMsgTextArea(Messages
                        .getI18NString("nonEmptyChatWindowClose").getText());
                    int answer = JOptionPane.showConfirmDialog(chatWindow,
                        msgText, Messages.getI18NString("warning").getText(),
                        JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);

                    if (answer == JOptionPane.OK_OPTION) {
                        closeChatPanel(chatPanel);
                    }
                }
                else if (System.currentTimeMillis() - chatPanel
                    .getLastIncomingMsgTimestamp().getTime() < 2 * 1000) {
                    SIPCommMsgTextArea msgText = new SIPCommMsgTextArea(Messages
                        .getI18NString("closeChatAfterNewMsg").getText());

                    int answer = JOptionPane.showConfirmDialog(chatWindow,
                        msgText, Messages.getI18NString("warning").getText(),
                        JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);

                    if (answer == JOptionPane.OK_OPTION) {
                        closeChatPanel(chatPanel);
                    }
                }
                else {
                    closeChatPanel(chatPanel);
                }
            }
        }
    }

    public void closeWindow()
    {
        synchronized (syncChat)
        {
            if (!chatWindow.getCurrentChatPanel().isWriteAreaEmpty())
            {
                SIPCommMsgTextArea msgText = new SIPCommMsgTextArea(Messages
                    .getI18NString("nonEmptyChatWindowClose").getText());
                int answer = JOptionPane.showConfirmDialog(chatWindow,
                    msgText, Messages.getI18NString("warning").getText(),
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);

                if (answer == JOptionPane.OK_OPTION)
                {
                    chatWindow.removeAllChats();
                    chatWindow.setVisible(false);

                    synchronized (chats)
                    {
                        chats.clear();
                    }
                }
            }
            else if (System.currentTimeMillis() - chatWindow.getCurrentChatPanel()
                .getLastIncomingMsgTimestamp().getTime() < 2 * 1000)
            {
                SIPCommMsgTextArea msgText = new SIPCommMsgTextArea(Messages
                    .getI18NString("closeChatAfterNewMsg").getText());

                int answer = JOptionPane.showConfirmDialog(chatWindow,
                    msgText, Messages.getI18NString("warning").getText(),
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);

                if (answer == JOptionPane.OK_OPTION)
                {
                    chatWindow.removeAllChats();
                    chatWindow.setVisible(false);

                    synchronized (chats)
                    {
                        chats.clear();
                    }
                }
            }
            else
            {
                chatWindow.removeAllChats();
                chatWindow.setVisible(false);

                synchronized (chats)
                {
                    chats.clear();
                }
            }
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
            chats.remove(chatPanel.getMetaContact());
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
        synchronized (syncChat)
        {
            ChatWindow chatWindow;

            if(Constants.TABBED_CHAT_WINDOW)
                chatWindow = this.chatWindow;
            else
            {
                chatWindow = new ChatWindow(mainFrame);

                this.chatWindow = chatWindow;
            }

            ChatPanel chatPanel
                = new ChatPanel(chatWindow, contact, protocolContact);

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
    }

    /**
     * Returns TRUE if this chat window contains a chat for the given contact,
     * FALSE otherwise.
     *
     * @param metaContact the meta contact whose corresponding chat we're
     * looking for.
     * @return TRUE if this chat window contains a chat for the given contact,
     * FALSE otherwise
     */
    private boolean containsContactChat(MetaContact metaContact)
    {
        synchronized (chats)
        {
            return chats.containsKey(metaContact);
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
    private boolean containsContactChat(ChatPanel chatPanel)
    {
        synchronized (chats)
        {
            return chats.containsValue(chatPanel);
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
        if(containsContactChat(metaContact))
        {
            synchronized (chats)
            {
                return (ChatPanel) chats.get(metaContact);
            }
        }
        else         
            return createChat(metaContact);
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
        if(containsContactChat(metaContact))
        {
            synchronized (chats)
            {
                return (ChatPanel) chats.get(metaContact);
            }
        }
        else         
            return createChat(metaContact, protocolContact);
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
        if(containsContactChat(metaContact))
        {
            synchronized (chats)
            {
                return (ChatPanel) chats.get(metaContact);
            }
        }
        else         
            return createChat(metaContact, protocolContact, escapedMessageID);
    }
        
    /**
     * Updates the status of the given metacontact in all opened chats
     * containing this contact.
     * 
     * @param metaContact the contact whose status we will be updating
     * @param protoContact the protocol specific contact
     */
    public void updateChatContactStatus(MetaContact metaContact,
            Contact protoContact)
    {
        if(containsContactChat(metaContact))
        {
            ContactListModel listModel
                = (ContactListModel) mainFrame.getContactListPanel()
                    .getContactList().getModel();
                    
            ChatPanel chatPanel;
            
            synchronized (chats)
            {
                chatPanel = (ChatPanel) chats.get(metaContact);
            }
            
            chatPanel.updateContactStatus(metaContact, protoContact);
            
            if(Constants.TABBED_CHAT_WINDOW)
            {
                ChatWindow chatWindow = chatPanel.getChatWindow();
                
                if (chatWindow.getChatTabCount() > 0) {
                    chatWindow.setTabIcon(chatPanel, listModel
                            .getMetaContactStatusIcon(metaContact));
                }
            }
        }
    }
}
