/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.message;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

import net.java.sip.communicator.impl.gui.main.MainFrame;
import net.java.sip.communicator.impl.gui.main.customcontrols.SIPCommMsgTextArea;
import net.java.sip.communicator.impl.gui.main.customcontrols.SIPCommTabbedPane;
import net.java.sip.communicator.impl.gui.main.customcontrols.events.CloseListener;
import net.java.sip.communicator.impl.gui.main.i18n.Messages;
import net.java.sip.communicator.impl.gui.utils.Constants;
import net.java.sip.communicator.impl.gui.utils.ImageLoader;
import net.java.sip.communicator.service.contactlist.MetaContact;
import net.java.sip.communicator.service.protocol.Contact;
import net.java.sip.communicator.service.protocol.PresenceStatus;

/**
 * The chat window is the place, where users can write and send messages, view
 * received messages. The ChatWindow supports two modes of use: "Group all
 * messages in one window" and "Open messages in new window". In the first case
 * a TabbedPane is added in the window, where each tab contains a ChatPanel. In
 * the second case the ChatPanel is added directly to the window. The ChatPanel
 * itself contains all message containers and corresponds to a contact or a
 * conference.
 * 
 * @author Yana Stamcheva
 */
public class ChatWindow extends JFrame {

    private ChatPanel currentChatPanel;

    private MenusPanel menusPanel;

    private MainFrame mainFrame;

    private String windowTitle = "";

    private SIPCommTabbedPane chatTabbedPane = null;

    private Hashtable contactChats = new Hashtable();

    private boolean enableTypingNotification = true;
    
    /**
     * Creates a chat window.
     * 
     * @param mainFrame The parent MainFrame.
     */
    public ChatWindow(MainFrame mainFrame) {

        this.mainFrame = mainFrame;

        this.setSize(550, 450);

        this.setIconImage(ImageLoader.getImage(ImageLoader.SIP_LOGO));

        menusPanel = new MenusPanel(this);

        this.init();

        getRootPane().getActionMap()
                .put("close", new CloseAction());
        getRootPane().getActionMap()
                .put("changeTabForword", new ForwordTabAction());
        getRootPane().getActionMap()
                .put("changeTabBackword", new BackwordTabAction());
        getRootPane().getActionMap()
                .put("sendMessage", new SendMessageAction());
        getRootPane().getActionMap()
                .put("openSmilies", new OpenSmilyAction());
        getRootPane().getActionMap()
                .put("changeProtocol", new ChangeProtocolAction());
        
        getRootPane().getActionMap().put("copy", new CopyAction());
        getRootPane().getActionMap().put("paste", new PasteAction());
        
        InputMap imap = this.getRootPane().getInputMap(
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close");
        imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT,
                KeyEvent.ALT_DOWN_MASK), "changeTabForword");
        imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT,
                KeyEvent.ALT_DOWN_MASK), "changeTabBackword");
        imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_INSERT,
                KeyEvent.CTRL_DOWN_MASK), "copy");
        imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_INSERT,
                KeyEvent.SHIFT_DOWN_MASK), "paste");
        imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,
                KeyEvent.CTRL_DOWN_MASK), "sendMessage");
        imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_S,
                KeyEvent.ALT_DOWN_MASK), "openSmilies");
        imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_P,
                KeyEvent.ALT_DOWN_MASK), "changeProtocol");
        
        this.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                close();
            }
        });
    }

    /**
     * Initialize the chat window.
     */
    public void init() {
        this.getContentPane().add(menusPanel, BorderLayout.NORTH);
    }

    /**
     * Returns the main frame.
     * 
     * @return The parent window.
     */
    public MainFrame getMainFrame() {
        return mainFrame;
    }

    /**
     * Sets the main frame.
     * 
     * @param mainFrame The parent window for this chat window.
     */
    public void setMainFrame(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
    }
    
    /**
     * Closes the current chat, triggering warnings to the user 
     * when there are non-sent messages or a message is received
     * in last 2 seconds.
     */
    private void close() {
        if (!getCurrentChatPanel().isWriteAreaEmpty()) {
            SIPCommMsgTextArea msgText = new SIPCommMsgTextArea(
                    Messages.getString("nonEmptyChatWindowClose"));
            int answer = JOptionPane.showConfirmDialog(ChatWindow.this,
                    msgText, Messages
                            .getString("warning"),
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);

            if (answer == JOptionPane.OK_OPTION) {
                closeChat();
            }
        } else if (System.currentTimeMillis()
                - getCurrentChatPanel().getLastIncomingMsgTimestamp()
                        .getTime() < 2 * 1000) {
            SIPCommMsgTextArea msgText = new SIPCommMsgTextArea(
                    Messages.getString("closeChatAfterNewMsg"));
            
            int answer = JOptionPane.showConfirmDialog(ChatWindow.this,
                    msgText, Messages
                            .getString("warning"),
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);

            if (answer == JOptionPane.OK_OPTION) {
                closeChat();
            }
        } else {
            closeChat();
        }
    }

    /**
     * Closes the selected chat tab or the window if there
     * are no tabs.
     */
    private void closeChat() {
        if (chatTabbedPane.getTabCount() > 1) {
            removeContactTab(chatTabbedPane.getSelectedIndex());
        } else {
            ChatWindow.this.dispose();
            mainFrame.getTabbedPane().getContactListPanel()
                    .setTabbedChatWindow(null);
        }
    }

    /**
     * Creates a ChatPanel for the given contact and 
     * adds it directly to the chat window.
     * @param contact The MetaContact for this chat.
     * @param status The current status.
     * @param protocolContact The protocol contact.
     * @return The <code>ChatPanel</code> newly created.
     */
    public ChatPanel createChat(MetaContact contact,
            PresenceStatus status, Contact protocolContact) {

        ChatPanel chatPanel = new ChatPanel(this, protocolContact);
        
        chatPanel.addContactToChat(contact, status);

        this.contactChats.put(contact.getMetaUID(), chatPanel);
        
//      this.sendPanel.addProtocols(contactItem.getProtocolList());
        
        return chatPanel;
    }
    
    public void addChat(ChatPanel chatPanel) {
        chatPanel.setChatVisible(true);
        
        this.setCurrentChatPanel(chatPanel);
        
        this.getContentPane().add(this.currentChatPanel, BorderLayout.CENTER);
        
        this.windowTitle += chatPanel.getDefaultContact()
            .getDisplayName() + " ";

        this.setTitle(this.windowTitle);
    }
   
    
    /**
     * Creates a ChatPanel for the given contact and adds it to a tabbedPane.
     * 
     * @param contact The MetaContact added to the chat.
     * @param status The current status.
     * @param protocolContact The protocol contact.
     */
    public void addChatTab(ChatPanel chatPanel) {
        String contactName = chatPanel.getDefaultContact().getDisplayName();
        PresenceStatus status = chatPanel.getPresenceStatus();
        
        if (chatTabbedPane == null) {
            //Initialize the tabbed pane for the first time            

            chatTabbedPane = new SIPCommTabbedPane(true);

            chatTabbedPane.addCloseListener(new CloseListener() {
                public void closeOperation(MouseEvent e) {

                    int tabIndex = chatTabbedPane.getOverTabIndex();
                    removeContactTab(tabIndex);
                }
            });

            this.getContentPane().add(chatPanel, BorderLayout.CENTER);

            this.setTitle(contactName);
            
            this.setCurrentChatPanel(chatPanel);
        }
        else {
            if (chatTabbedPane.getTabCount() > 0) {
                //The tabbed pane contains already tabs.              
                
                chatTabbedPane.addTab(contactName, new ImageIcon(
                        Constants.getStatusIcon(status)),
                        chatPanel);

                chatTabbedPane.getParent().validate();                
            } else {
                ChatPanel firstChatPanel = getCurrentChatPanel();
                
                PresenceStatus currentContactStatus = firstChatPanel
                        .getPresenceStatus();
                //Add first two tabs to the tabbed pane.
                chatTabbedPane.addTab(firstChatPanel.getDefaultContact()
                        .getDisplayName(), new ImageIcon(Constants
                        .getStatusIcon(currentContactStatus)), firstChatPanel);
                
                chatTabbedPane.addTab(contactName, new ImageIcon(
                        Constants.getStatusIcon(status)), chatPanel);
                
                // Workaround for the following problem:
                // The scrollbar in the conversation area moves up when the
                // scrollpane is resized. This happens when ChatWindow is in
                // mode "Group messages in one window" and the first chat panel
                // is added to the tabbed pane. Then the scrollpane in the 
                // conversation area is slightly resized and is made smaller,
                // which moves the scrollbar up.
                firstChatPanel.setCaretToEnd();                
            }

            this.getContentPane().add(chatTabbedPane, BorderLayout.CENTER);
            this.getContentPane().validate();
            
            int chatIndex = chatTabbedPane.getTabCount() - 1;
            if(chatTabbedPane.getSelectedIndex() == chatIndex)
                this.setCurrentChatPanel(chatPanel);
        }
    }

    /**
     * Selects the chat tab which corresponds to the given MetaContact.
     * 
     * @param contact The MetaContact to select.
     */
    public void setSelectedContactTab(MetaContact contact) {

        if (this.contactChats != null 
                && contactChats.get(contact.getMetaUID()) != null) {

            ChatPanel chatPanel = ((ChatPanel) this.contactChats
                    .get(contact.getMetaUID()));

            this.chatTabbedPane.setSelectedComponent(chatPanel);
            chatPanel.requestFocusInWriteArea();
        }
    }

    /**
     * Selects the contact tab given by the index.
     * @param index The index of the tab to select.
     */
    public void setSelectedContactTab(int index) {
        ChatPanel chatPanel = (ChatPanel) this.chatTabbedPane
                .getComponentAt(index);

        this.setCurrentChatPanel(chatPanel);
        this.chatTabbedPane.setSelectedIndex(index);
        this.setVisible(true);
        chatPanel.requestFocusInWriteArea();
    }

    /**
     * Removes the tab with the given index.
     * 
     * @param index Tab index.
     */
    public void removeContactTab(int index) {

        String title = chatTabbedPane.getTitleAt(index);

        ChatPanel closeChat = (ChatPanel) chatTabbedPane.getComponentAt(index);

        if (title != null) {
            if (chatTabbedPane.getTabCount() > 1) {
                this.contactChats.remove(closeChat.getDefaultContact()
                        .getMetaUID());
            }

            if (chatTabbedPane.getTabCount() > 1)
                chatTabbedPane.remove(index);

            if (chatTabbedPane.getTabCount() == 1) {

                String onlyTabtitle = chatTabbedPane.getTitleAt(0);

                ChatPanel chatPanel = (ChatPanel) this.chatTabbedPane
                        .getComponentAt(0);

                this.getContentPane().remove(chatTabbedPane);

                this.chatTabbedPane.removeAll();

                this.getContentPane().add(chatPanel, BorderLayout.CENTER);

                this.setCurrentChatPanel(chatPanel);

                this.setTitle(onlyTabtitle);
            }
        }
    }
    
    public void removeChat(ChatPanel chatPanel) {
        this.close();
    }

    public void removeChatTab(ChatPanel chatPanel) {
        this.chatTabbedPane.remove(chatPanel);
        this.contactChats.remove(chatPanel.getDefaultContact().getMetaUID());
        this.validate();
    }
    
    /**
     * Returns the table of all MetaContact-s for this chat window. 
     * This is used in case of tabbed chat window.
     * 
     * @return The table of all MetaContact-s for this chat window.
     */
    public Hashtable getContactChatsTable() {
        return this.contactChats;
    }

    /**
     * Returns the currently selected chat panel.
     * 
     * @return the currently selected chat panel.
     */
    public ChatPanel getCurrentChatPanel() {
        return this.currentChatPanel;
    }

    /**
     * Sets the currently selected chat panel.
     * 
     * @param currentChatPanel The chat panel which is currently selected.
     */
    public void setCurrentChatPanel(ChatPanel currentChatPanel) {
        this.currentChatPanel = currentChatPanel;
    }

    /**
     * Returns the tab count of the chat tabbed pane. Meant to be
     * used when in "Group chat windows" mode.
     * 
     * @return int The number of opened tabs.
     */
    public int getTabCount() {
        return this.chatTabbedPane.getTabCount();
    }

    /**
     * Returns the chat tab index for the given MetaContact.
     * 
     * @param contact The MetaContact we are searching for.
     * @return int The chat tab index for the given MetaContact.
     */
    public int getTabInex(MetaContact contact) {
        return this.chatTabbedPane.indexOfComponent(getChatPanel(contact));
    }

    /**
     * Highlights the corresponding tab when a message from
     * the given MetaContact is received.
     * 
     * @param contact The MetaContact to highlight.
     */
    public void highlightTab(MetaContact contact) {
        this.chatTabbedPane.highlightTab(getTabInex(contact));
    }

    /**
     * Returns the ChatPanel for the given MetaContact.
     * @param contact The MetaContact.
     * @return ChatPanel The ChatPanel for the given MetaContact.
     */
    public ChatPanel getChatPanel(MetaContact contact) {
        return (ChatPanel) this.contactChats.get(contact.getMetaUID());
    }
    
    /**
     * Sets the given icon to the tab opened for the given MetaContact.
     * @param metaContact The MetaContact.
     * @param icon The icon to set. 
     */
    public void setTabIcon(MetaContact metaContact, Icon icon) {
        int index = this.chatTabbedPane.indexOfComponent(this
                .getChatPanel(metaContact));
        this.chatTabbedPane.setIconAt(index, icon);
    }
    
    private class CloseAction extends AbstractAction {
        public void actionPerformed(ActionEvent e) {
            close();
        }
    };

    private class ForwordTabAction extends AbstractAction {
        public void actionPerformed(ActionEvent e) {
            if (chatTabbedPane != null) {
                int selectedIndex = chatTabbedPane.getSelectedIndex();
                if (selectedIndex < chatTabbedPane.getTabCount() - 1) {
                    setSelectedContactTab(selectedIndex + 1);
                } else {
                    setSelectedContactTab(0);
                }
            }
        }
    };

    private class BackwordTabAction extends AbstractAction {
        public void actionPerformed(ActionEvent e) {
            if (chatTabbedPane != null) {
                int selectedIndex = chatTabbedPane.getSelectedIndex();
                if (selectedIndex != 0) {
                    setSelectedContactTab(selectedIndex - 1);
                } else {
                    setSelectedContactTab(chatTabbedPane.getTabCount() - 1);
                }
            }
        }
    };
    
    private class CopyAction extends AbstractAction {
        public void actionPerformed(ActionEvent e) {
            getCurrentChatPanel().copy();
        }
    };
    
    private class PasteAction extends AbstractAction {
        public void actionPerformed(ActionEvent e) {
            getCurrentChatPanel().paste();
        }
    };
    
    private class SendMessageAction extends AbstractAction {
        public void actionPerformed(ActionEvent e) {
            ChatPanel chatPanel = getCurrentChatPanel();
            chatPanel.stopTypingNotifications();

            chatPanel.sendMessage();
        } 
    }
    
    private class OpenSmilyAction extends AbstractAction {
        public void actionPerformed(ActionEvent e) {
            menusPanel.getMainToolBar().getSmileyButton().doClick();
        } 
    }
    
    private class ChangeProtocolAction extends AbstractAction {
        public void actionPerformed(ActionEvent e) {
            getCurrentChatPanel().openProtocolSelectorBox();
        } 
    }
    
    public void enableTypingNotification(boolean enable) {
        this.enableTypingNotification = enable;
    }
    
    public boolean isTypingNotificationEnabled(){
        return enableTypingNotification;
    }
}
