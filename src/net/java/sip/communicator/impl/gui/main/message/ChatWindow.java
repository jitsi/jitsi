/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.message;

import java.awt.BorderLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Hashtable;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

import net.java.sip.communicator.impl.gui.GuiActivator;
import net.java.sip.communicator.impl.gui.customcontrols.SIPCommMsgTextArea;
import net.java.sip.communicator.impl.gui.customcontrols.SIPCommTabbedPane;
import net.java.sip.communicator.impl.gui.customcontrols.events.CloseListener;
import net.java.sip.communicator.impl.gui.i18n.Messages;
import net.java.sip.communicator.impl.gui.main.MainFrame;
import net.java.sip.communicator.impl.gui.utils.Constants;
import net.java.sip.communicator.impl.gui.utils.ImageLoader;
import net.java.sip.communicator.service.configuration.ConfigurationService;
import net.java.sip.communicator.service.configuration.PropertyVetoException;
import net.java.sip.communicator.service.contactlist.MetaContact;
import net.java.sip.communicator.service.protocol.Contact;
import net.java.sip.communicator.service.protocol.PresenceStatus;
import net.java.sip.communicator.util.Logger;

/**
 * The chat window is the place, where users can write and send messages, view
 * received messages. The ChatWindow supports two modes of use: "Group all
 * messages in one window" and "Open messages in new window". In the first case
 * a <tt>JTabbedPane</tt> is added in the window, where each tab contains a
 * <tt>ChatPanel</tt>. In the second case the <tt>ChatPanel</tt> is added
 * directly to the window. The <tt>ChatPanel</tt> contains all chat elements
 * like a "write message area", "send" button, etc. It corresponds to a
 * <tt>MetaContact</tt> or to a conference.
 * <p>
 * Note that the conference case is not yet implemented.
 * 
 * @author Yana Stamcheva
 */
public class ChatWindow extends JFrame {

    private Logger logger = Logger.getLogger(ChatWindow.class.getName());
    
    private static final String CHAT_WINDOW_WIDTH_PROPERTY
        = "net.java.sip.communicator.impl.ui.chatWindowWidth";
    
    private static final String CHAT_WINDOW_HEIGHT_PROPERTY
        = "net.java.sip.communicator.impl.ui.chatWindowHeight";
    
    private static final String CHAT_WINDOW_X_PROPERTY
        = "net.java.sip.communicator.impl.ui.chatWindowX";
    
    private static final String CHAT_WINDOW_Y_PROPERTY
        = "net.java.sip.communicator.impl.ui.chatWindowY";


    private ChatPanel currentChatPanel;

    private MenusPanel menusPanel;

    private MainFrame mainFrame;

    private String windowTitle = "";

    private SIPCommTabbedPane chatTabbedPane = null;

    private Hashtable contactChats = new Hashtable();

    private boolean enableTypingNotification = true;
    
    /**
     * Creates an instance of <tt>ChatWindow</tt>.
     * 
     * @param mainFrame The parent MainFrame.
     */
    public ChatWindow(MainFrame mainFrame) {

        this.mainFrame = mainFrame;

        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        
        this.setSize(550, 450);

        this.setIconImage(ImageLoader.getImage(ImageLoader.SIP_LOGO));

        menusPanel = new MenusPanel(this);

        this.setSizeAndLocation();
        
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
                .put("openSmilies", new OpenSmileyAction());
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
        imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_C,
                KeyEvent.META_MASK), "copy");
        imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_V,
                KeyEvent.META_MASK), "paste");
        imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,
                KeyEvent.CTRL_DOWN_MASK), "sendMessage");
        imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_S,
                KeyEvent.ALT_DOWN_MASK), "openSmilies");
        imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_P,
                KeyEvent.ALT_DOWN_MASK), "changeProtocol");
        
        this.addWindowListener(new ChatWindowAdapter());       
    }

    /**
     * Initializes this window, by adding the menus.
     */
    public void init() {
        this.getContentPane().add(menusPanel, BorderLayout.NORTH);
    }

    /**
     * Positions this window in the center of the screen.
     */
    private void setCenterLocation(){
        this.setLocation(
                Toolkit.getDefaultToolkit().getScreenSize().width/2 
                    - this.getWidth()/2,
                Toolkit.getDefaultToolkit().getScreenSize().height/2 
                    - this.getHeight()/2
                );
    }
    
    /**
     * Returns the main application widnow.
     * 
     * @return The main application widnow.
     */
    public MainFrame getMainFrame() {
        return mainFrame;
    }

    /**
     * Sets the main application widnow.
     * 
     * @param mainFrame The main application widnow.
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
     * Creates a <tt>ChatPanel</tt> for the given contact and saves it
     * in the list ot created <tt>ChatPanel</tt>s.
     * 
     * @param contact The MetaContact for this chat.
     * @param status The current status.
     * @param protocolContact The protocol contact.
     * @return The <code>ChatPanel</code> newly created.
     */
    public ChatPanel createChat(MetaContact contact,
            PresenceStatus status, Contact protocolContact) {

        ChatPanel chatPanel = new ChatPanel(
                this, contact, protocolContact);
        
        this.contactChats.put(contact.getMetaUID(), chatPanel);
        
//      this.sendPanel.addProtocols(contactItem.getProtocolList());
        
        return chatPanel;
    }
    
    /**
     * Adds a given <tt>ChatPanel</tt> to this chat window.
     * 
     * @param chatPanel The <tt>ChatPanel</tt> to add.
     */
    public void addChat(ChatPanel chatPanel) {
        chatPanel.setChatVisible(true);
        
        this.setCurrentChatPanel(chatPanel);
        
        this.getContentPane().add(this.currentChatPanel, BorderLayout.CENTER);
        
        this.windowTitle += chatPanel.getMetaContact()
            .getDisplayName() + " ";

        this.setTitle(this.windowTitle);
    }
   
    
    /**
     * Adds a given <tt>ChatPanel</tt> to the <tt>JTabbedPane</tt> of this chat
     * window.
     * 
     * @param chatPanel The <tt>ChatPanel</tt> to add.
     */
    public void addChatTab(ChatPanel chatPanel) {
        chatPanel.setChatVisible(true);
        
        String contactName = chatPanel.getMetaContact().getDisplayName();
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
                chatTabbedPane.addTab(firstChatPanel.getMetaContact()
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
     * Selects the chat tab which corresponds to the given <tt>MetaContact</tt>.
     * 
     * @param contact The <tt>MetaContact</tt> to select.
     */
    public void setSelectedContactTab(MetaContact contact) {

        if (this.contactChats != null 
                && contactChats.get(contact.getMetaUID()) != null) {

            ChatPanel chatPanel = ((ChatPanel) this.contactChats
                    .get(contact.getMetaUID()));

            this.chatTabbedPane.setSelectedComponent(chatPanel);            
            this.setTitle(chatPanel.getMetaContact().getDisplayName());
            this.setCurrentChatPanel(chatPanel);            
            chatPanel.requestFocusInWriteArea();
        }
    }

    /**
     * Selects the contact tab given by <code>index</code>.
     * @param index The index of the tab to select.
     */
    public void setSelectedContactTab(int index) {
        ChatPanel chatPanel = (ChatPanel) this.chatTabbedPane
                .getComponentAt(index);

        this.setCurrentChatPanel(chatPanel);
        this.chatTabbedPane.setSelectedIndex(index);
        this.setTitle(chatPanel.getMetaContact().getDisplayName());
        this.setVisible(true);
        chatPanel.requestFocusInWriteArea();
    }

    /**
     * Removes the tab with the given <code>index</code>.
     * 
     * @param index The index of the tab to remove.
     */
    public void removeContactTab(int index) {

        String title = chatTabbedPane.getTitleAt(index);

        ChatPanel closeChat = (ChatPanel) chatTabbedPane.getComponentAt(index);

        if (title != null) {
            if (chatTabbedPane.getTabCount() > 1) {
                this.contactChats.remove(closeChat.getMetaContact()
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
    
    /**
     * Removes a given <tt>ChatPanel</tt>, when not in tabbed chat mode.
     * @param chatPanel The <tt>ChatPanel</tt> to remove.
     */
    public void removeChat(ChatPanel chatPanel) {
        this.close();
    }

    /**
     * Removes a given <tt>ChatPanel</tt> from the tabbed pane.
     * @param chatPanel The <tt>ChatPanel</tt> to remove.
     */
    public void removeChatTab(ChatPanel chatPanel) {
        this.chatTabbedPane.remove(chatPanel);
        this.contactChats.remove(chatPanel.getMetaContact().getMetaUID());
        this.validate();
    }
    
    /**
     * Returns the table of all <tt>MetaContact</tt>s for this chat window. 
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
    
    /**
     * The <tt>CloseAction</tt> is an <tt>AbstractAction</tt> that closes the
     * current chat.
     */
    private class CloseAction extends AbstractAction {
        public void actionPerformed(ActionEvent e) {
            close();
        }
    };

    /**
     * The <tt>ForwordTabAction</tt> is an <tt>AbstractAction</tt> that changes
     * the currently selected tab with the next one. Each time when the last tab
     * index is reached the first one is selected.
     */
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

    /**
     * The <tt>BackwordTabAction</tt> is an <tt>AbstractAction</tt> that changes
     * the currently selected tab with the previous one. Each time when the
     * first tab index is reached the last one is selected.
     */
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
    
    /**
     * The <tt>CopyAction</tt> is an <tt>AbstractAction</tt> that copies the
     * text currently selected.
     */
    private class CopyAction extends AbstractAction {
        public void actionPerformed(ActionEvent e) {
            getCurrentChatPanel().copy();
        }
    };
    
    /**
     * The <tt>PasteAction</tt> is an <tt>AbstractAction</tt> that pastes the
     * text contained in the clipboard in the current <tt>ChatPanel</tt>.
     */
    private class PasteAction extends AbstractAction {
        public void actionPerformed(ActionEvent e) {
            getCurrentChatPanel().paste();
        }
    };
    
    /**
     * The <tt>SendMessageAction</tt> is an <tt>AbstractAction</tt> that
     * sends the text that is currently in the write message area.
     */
    private class SendMessageAction extends AbstractAction {
        public void actionPerformed(ActionEvent e) {
            ChatPanel chatPanel = getCurrentChatPanel();
            chatPanel.stopTypingNotifications();

            chatPanel.sendMessage();
        } 
    }
    
    /**
     * The <tt>OpenSmileyAction</tt> is an <tt>AbstractAction</tt> that
     * opens the menu, containing all available smilies' icons.
     */
    private class OpenSmileyAction extends AbstractAction {
        public void actionPerformed(ActionEvent e) {
            menusPanel.getMainToolBar().getSmileyButton().doClick();
        } 
    }
    
    /**
     * The <tt>ChangeProtocolAction</tt> is an <tt>AbstractAction</tt> that
     * opens the menu, containing all available protocol contacts.
     */
    private class ChangeProtocolAction extends AbstractAction {
        public void actionPerformed(ActionEvent e) {
            getCurrentChatPanel().openProtocolSelectorBox();
        } 
    }
    
    /**
     * Enables typing notifications.
     * 
     * @param enable <code>true</code> to enable typing notifications,
     * <code>false</code> to disable them.
     */
    public void enableTypingNotification(boolean enable) {
        this.enableTypingNotification = enable;
    }
    
    /**
     * Checks whether typing notifications are enabled or not.
     * @return <code>true</code> if typing notifications are enabled,
     * <code>false</code> otherwise.
     */
    public boolean isTypingNotificationEnabled(){
        return enableTypingNotification;
    }
    
    /**
     * Before closing the chat window saves the current size and position
     * through the <tt>ConfigurationService</tt>.
     */
    public class ChatWindowAdapter extends WindowAdapter {

        public void windowDeiconified(WindowEvent e) {
            String title = getTitle();
            
            if (title.endsWith("*")) {
                setTitle(title.substring(0, title.length() - 1));
            }
        }
       
        public void windowClosing(WindowEvent e) {
            ConfigurationService configService
                = GuiActivator.getConfigurationService();
            
            try {
                configService.setProperty(
                    CHAT_WINDOW_WIDTH_PROPERTY,
                    new Integer(getWidth()));
                
                configService.setProperty(
                    CHAT_WINDOW_HEIGHT_PROPERTY,
                    new Integer(getHeight()));
                
                configService.setProperty(
                    CHAT_WINDOW_X_PROPERTY,
                    new Integer(getX()));
                
                configService.setProperty(
                    CHAT_WINDOW_Y_PROPERTY,
                    new Integer(getY()));
            }
            catch (PropertyVetoException e1) {
                logger.error("The proposed property change "
                        + "represents an unacceptable value");
            }
            
            close();
        }
    }
    
    /**
     * Sets the window size and position.
     */
    public void setSizeAndLocation() {
        ConfigurationService configService
            = GuiActivator.getConfigurationService();
        
        String width = configService.getString(CHAT_WINDOW_WIDTH_PROPERTY);
        
        String height = configService.getString(CHAT_WINDOW_HEIGHT_PROPERTY);
        
        String x = configService.getString(CHAT_WINDOW_X_PROPERTY);
        
        String y = configService.getString(CHAT_WINDOW_Y_PROPERTY);
        
       
        if(width != null && height != null)
            this.setSize(new Integer(width).intValue(), 
                    new Integer(height).intValue());
        
        if(x != null && y != null)
            this.setLocation(new Integer(x).intValue(), 
                    new Integer(y).intValue());
        else
            this.setCenterLocation();
    }
}
