/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.contactlist;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Date;
import java.util.Hashtable;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import net.java.sip.communicator.impl.gui.main.MainFrame;
import net.java.sip.communicator.impl.gui.main.customcontrols.SIPCommMsgTextArea;
import net.java.sip.communicator.impl.gui.main.i18n.Messages;
import net.java.sip.communicator.impl.gui.main.message.ChatPanel;
import net.java.sip.communicator.impl.gui.main.message.ChatWindow;
import net.java.sip.communicator.impl.gui.utils.Constants;
import net.java.sip.communicator.service.contactlist.MetaContact;
import net.java.sip.communicator.service.contactlist.MetaContactGroup;
import net.java.sip.communicator.service.contactlist.MetaContactListService;
import net.java.sip.communicator.service.protocol.Contact;
import net.java.sip.communicator.service.protocol.Message;
import net.java.sip.communicator.service.protocol.OperationSetTypingNotifications;
import net.java.sip.communicator.service.protocol.PresenceStatus;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;
import net.java.sip.communicator.service.protocol.event.MessageDeliveredEvent;
import net.java.sip.communicator.service.protocol.event.MessageDeliveryFailedEvent;
import net.java.sip.communicator.service.protocol.event.MessageListener;
import net.java.sip.communicator.service.protocol.event.MessageReceivedEvent;
import net.java.sip.communicator.service.protocol.event.TypingNotificationEvent;
import net.java.sip.communicator.service.protocol.event.TypingNotificationsListener;

/**
 * The contactlist panel not only contains the contact list but it has the role
 * of a message dispatcher. It process all sent and received messages as well
 * as all typing notifications. Here are managed all contact list mouse events.
 *
 * @author Yana Stamcheva
 */
public class ContactListPanel extends JScrollPane
    implements  MouseListener, 
                MessageListener, 
                TypingNotificationsListener {

    private MainFrame mainFrame;

    private ContactList contactList;

    private JPanel treePanel = new JPanel(new BorderLayout());

    private Hashtable contactMsgWindows = new Hashtable();

    private ChatWindow tabbedChatWindow;

    private TypingTimer typingTimer = new TypingTimer();

    /**
     * Creates the contactlist scroll panel defining the parent frame.
     *
     * @param mainFrame The parent frame.
     */
    public ContactListPanel(MainFrame mainFrame) {

        this.mainFrame = mainFrame;

        this.getViewport().add(treePanel);       
                
        this.setHorizontalScrollBarPolicy(
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        this.getVerticalScrollBar().setUnitIncrement(30);
    }

    /**
     * Initialize the contact list tree.
     * 
     * @param contactListService The MetaContactListService which will
     * be used for a contact list data model.
     */
    public void initTree(MetaContactListService contactListService) {
        
        this.contactList = new ContactList(contactListService);

        this.contactList.addMouseListener(this);
        
        this.treePanel.add(contactList, BorderLayout.NORTH);

        this.treePanel.setBackground(Color.WHITE);
        this.contactList.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        
        this.addKeyListener(new CListKeySearchListener(this.contactList));

        this.getRootPane().getActionMap().put("runChat",
                new RunMessageWindowAction());
        
        InputMap imap = this.getRootPane().getInputMap(
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "runChat");
    }

    public ContactList getContactList() {
        return this.contactList;
    }

    public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() > 1) {

            int selectedIndex = this.contactList.locationToIndex(e.getPoint());

            ContactListModel listModel = (ContactListModel) this.contactList
                    .getModel();

            Object element = listModel.getElementAt(selectedIndex);

            if (element instanceof MetaContactGroup) {

                MetaContactGroup group = (MetaContactGroup) element;

                if (listModel.isGroupClosed(group)) {
                    listModel.openGroup(group);
                } else {
                    listModel.closeGroup(group);
                }
            }
        }
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
        // Select the contact under the right button click.
        if ((e.getModifiers() & InputEvent.BUTTON3_MASK) != 0) {
            this.contactList.setSelectedIndex(contactList.locationToIndex(e
                    .getPoint()));
        }
        
        int selectedIndex = this.contactList.getSelectedIndex();
        Object selectedValue = this.contactList.getSelectedValue();

        ContactListCellRenderer renderer 
            = (ContactListCellRenderer) this.contactList
                .getCellRenderer().getListCellRendererComponent(
                        this.contactList, selectedValue, selectedIndex, true,
                        true);

        Point selectedCellPoint = this.contactList
                .indexToLocation(selectedIndex);

        int translatedX = e.getX() - selectedCellPoint.x;

        int translatedY = e.getY() - selectedCellPoint.y;

        
        if(selectedValue instanceof MetaContactGroup) {
            MetaContactGroup group = (MetaContactGroup) selectedValue;
            
            if ((e.getModifiers() & InputEvent.BUTTON3_MASK) != 0) {
                
                GroupRightButtonMenu popupMenu
                    = new GroupRightButtonMenu(mainFrame, group);

                SwingUtilities.convertPointToScreen(selectedCellPoint,
                        renderer);

                popupMenu.setInvoker(this.contactList);

                popupMenu.setLocation(selectedCellPoint.x,
                        selectedCellPoint.y + renderer.getHeight());

                popupMenu.setVisible(true);
            }
        }
        
        // Open message window, right button menu or contact info when
        // mouse is pressed. Distinguish on which component was pressed
        // the mouse and make the appropriate work.
        if (selectedValue instanceof MetaContact) {
            MetaContact contact = (MetaContact) selectedValue;
            
            //get the component under the mouse
            Component component = renderer.getComponentAt(translatedX,
                    translatedY);
            if (component instanceof JLabel) {
                //Left click on the contact label opens Chat window
                if ((e.getModifiers() & InputEvent.BUTTON1_MASK) != 0) {
                    SwingUtilities.invokeLater(new RunMessageWindow(contact));
                } 
                //Right click on the contact label opens Popup menu
                else if ((e.getModifiers() & InputEvent.BUTTON3_MASK) != 0) {
                    
                    ContactRightButtonMenu popupMenu
                        = new ContactRightButtonMenu(mainFrame, contact);

                    SwingUtilities.convertPointToScreen(selectedCellPoint,
                            renderer);

                    popupMenu.setInvoker(this.contactList);

                    popupMenu.setLocation(selectedCellPoint.x,
                            selectedCellPoint.y + renderer.getHeight());

                    popupMenu.setVisible(true);
                }
            } 
            else if (component instanceof JButton) {
                //Click on the info button opens the info popup panel
                SwingUtilities.invokeLater(new RunInfoWindow(selectedCellPoint,
                        contact));
            } 
            else if (component instanceof JPanel
                    && component.getName().equals("buttonsPanel")) {
                JPanel panel = (JPanel) component;

                int internalX = translatedX
                        - (renderer.getWidth() - panel.getWidth() - 2);
                int internalY = translatedY
                        - (renderer.getHeight() - panel.getHeight());

                Component c = panel.getComponentAt(4, 4);

                if (c instanceof ContactProtocolButton) {

                    SwingUtilities.invokeLater(new RunMessageWindow(contact,
                            ((ContactProtocolButton) c).getProtocolContact()));
                }
            }

        }
    }

    public void mouseReleased(MouseEvent e) {
    }

    /**
     * Runs the chat window for the specified contact.
     *
     * @author Yana Stamcheva
     */
    public class RunMessageWindow implements Runnable {

        private MetaContact contactItem;

        private Contact protocolContact;

        public RunMessageWindow(MetaContact contactItem) {
            this.contactItem = contactItem;
            this.protocolContact = contactItem.getDefaultContact();
        }

        public RunMessageWindow(MetaContact contactItem, 
                                Contact protocolContact) {
            this.contactItem = contactItem;
            this.protocolContact = protocolContact;
        }

        public void run() {
            PresenceStatus contactStatus = ((ContactListModel) contactList
                    .getModel()).getMetaContactStatus(this.contactItem);

            if (!Constants.TABBED_CHAT_WINDOW) {
                //If in mode "open all messages in new window"
                if (contactMsgWindows.containsKey(this.contactItem)) {
                    /*
                     * If a chat window for this contact is already opened
                     * show it.
                     */
                    ChatWindow msgWindow = (ChatWindow) contactMsgWindows
                            .get(this.contactItem);

                    if (msgWindow.getExtendedState() == JFrame.ICONIFIED)
                        msgWindow.setExtendedState(JFrame.NORMAL);

                    msgWindow.setVisible(true);
                } else {
                    /*
                     * If there's no chat window for the contact
                     * create it and show it.
                     */
                    ChatWindow msgWindow = new ChatWindow(mainFrame);

                    contactMsgWindows.put(this.contactItem, msgWindow);

                    ChatPanel chatPanel = msgWindow.createChat(
                            this.contactItem, contactStatus, protocolContact);
                    
                    msgWindow.addChat(chatPanel);
                    
                    msgWindow.pack();

                    msgWindow.setVisible(true);

                    msgWindow.getCurrentChatPanel().requestFocusInWriteArea();
                }
            } else {
                // If in mode "group messages in one chat window"
                if (tabbedChatWindow == null) {
                    // If there's no open chat window
                    tabbedChatWindow = new ChatWindow(mainFrame);

                    tabbedChatWindow.addWindowListener(new WindowAdapter() {
                        public void windowClosing(WindowEvent e) {
                            tabbedChatWindow = null;
                        }
                    });
                }
                /*
                 * Get the hashtable containg all tabs and corresponding
                 * chat panels.
                 */
                Hashtable contactTabsTable = tabbedChatWindow
                        .getContactChatsTable();

                //If there's no open tab for the given contact.
                if (contactTabsTable
                        .get(this.contactItem.getMetaUID()) == null) {
                    ChatPanel chatPanel = tabbedChatWindow.createChat(
                            this.contactItem, contactStatus, protocolContact);

                    tabbedChatWindow.addChatTab(chatPanel);
                    
                    if (tabbedChatWindow.getTabCount() > 1) {
                        tabbedChatWindow
                            .setSelectedContactTab(this.contactItem);
                    }

                    if (tabbedChatWindow.getExtendedState() == JFrame.ICONIFIED)
                        tabbedChatWindow.setExtendedState(JFrame.NORMAL);

                    tabbedChatWindow.setVisible(true);

                    tabbedChatWindow.getCurrentChatPanel()
                        .requestFocusInWriteArea();
                } else {
                    //If a tab for the given contact already exists.
                    if (tabbedChatWindow.getTabCount() > 1) {
                        tabbedChatWindow
                                .setSelectedContactTab(this.contactItem);
                    }

                    if (tabbedChatWindow.getExtendedState() == JFrame.ICONIFIED)
                        tabbedChatWindow.setExtendedState(JFrame.NORMAL);

                    tabbedChatWindow.setVisible(true);

                    tabbedChatWindow.getCurrentChatPanel()
                        .requestFocusInWriteArea();
                }
            }
        }
    }

    /**
     * Runs the info window for the specified contact at the
     * appropriate position.
     *
     * @author Yana Stamcheva
     */
    private class RunInfoWindow implements Runnable {

        private MetaContact contactItem;

        private Point p;

        private RunInfoWindow(Point p, MetaContact contactItem) {

            this.p = p;
            this.contactItem = contactItem;
        }

        public void run() {

            ContactInfoPanel contactInfoPanel = new ContactInfoPanel(mainFrame,
                    contactItem);

            SwingUtilities.convertPointToScreen(p, contactList);

            // TODO: to calculate popup window posititon properly.
            contactInfoPanel.setPopupLocation(p.x - 140, p.y - 15);

            contactInfoPanel.setVisible(true);

            contactInfoPanel.requestFocusInWindow();
        }
    }

    /**
     * When message is received determines whether to open a new chat
     * window or chat window tab, or to indicate that a message is received
     * from a contact which already has an open chat. When the chat is found
     * shows the message in the appropriate chat panel.
     *
     * @param evt the event containing details on the received message
     */
    public void messageReceived(MessageReceivedEvent evt) {
        
        Contact protocolContact = evt.getSourceContact();
        Date date = evt.getTimestamp();
        Message message = evt.getSourceMessage();
        
        MetaContact metaContact = mainFrame.getContactList()
                .findMetaContactByContact(protocolContact);

        PresenceStatus contactStatus = ((ContactListModel) this.contactList
                .getModel()).getMetaContactStatus(metaContact);
        
        if (!Constants.TABBED_CHAT_WINDOW) {
            //If in mode "open all message  s in new window"
            if (contactMsgWindows.containsKey(metaContact)) {
                /*
                 * If a chat window for this contact is already opened
                 * show it.
                 */
                ChatWindow msgWindow = (ChatWindow) contactMsgWindows
                        .get(metaContact);

                msgWindow.getCurrentChatPanel().processMessage(
                                protocolContact.getDisplayName(),
                                date, Constants.INCOMING_MESSAGE,
                                message.getContent());

                msgWindow.setVisible(true);
            }
            else {                
                ChatWindow msgWindow = new ChatWindow(mainFrame);
                
                contactMsgWindows.put(metaContact, msgWindow);

                ChatPanel chatPanel = msgWindow.createChat(metaContact,
                        contactStatus, protocolContact);

                msgWindow.getCurrentChatPanel().processMessage(
                        protocolContact.getDisplayName(),
                        date, Constants.INCOMING_MESSAGE,
                        message.getContent());
                /*
                 * If there's no chat window for the contact
                 * create it and show it.
                 */
                if(Constants.AUTO_POPUP_NEW_MESSAGE) {
                    msgWindow.addChat(chatPanel);
                    
                    msgWindow.pack();
    
                    msgWindow.setVisible(true);
                }
            }
        }
        else {
            // If in mode "group messages in one chat window"
            if (tabbedChatWindow == null) {                
                // If there's no open chat window
                tabbedChatWindow = new ChatWindow(mainFrame);

                tabbedChatWindow.addWindowListener(new WindowAdapter() {

                    public void windowClosing(WindowEvent e) {
                        tabbedChatWindow = null;
                    }
                });
            }
                
            Hashtable contactTabsTable = tabbedChatWindow.getContactChatsTable();
            ChatPanel chatPanel;

            //If there's no open tab for the given contact.
            if (contactTabsTable.get(metaContact.getMetaUID()) == null) {
                chatPanel = tabbedChatWindow.createChat(metaContact,
                        contactStatus, protocolContact);
                
                chatPanel.processMessage(
                        protocolContact.getDisplayName(),
                        date, Constants.INCOMING_MESSAGE,
                        message.getContent());
                
                if (Constants.AUTO_POPUP_NEW_MESSAGE) {
                    tabbedChatWindow.addChatTab(chatPanel);
                    
                    tabbedChatWindow.setVisible(true);

                    tabbedChatWindow.getCurrentChatPanel()
                        .requestFocusInWriteArea();
                    
                    if (tabbedChatWindow.getTabCount() > 1) {
                        tabbedChatWindow.highlightTab(metaContact);
                    }
                }
            }
            else {
                chatPanel = tabbedChatWindow.getChatPanel(metaContact);
                
                chatPanel.processMessage(
                        protocolContact.getDisplayName(),
                        date, Constants.INCOMING_MESSAGE,
                        message.getContent());
                
                tabbedChatWindow.setVisible(true);
                
                if (tabbedChatWindow.getTabCount() > 1) {
                    tabbedChatWindow.highlightTab(metaContact);
                }
            }
        }
        
        if(Constants.AUTO_POPUP_NEW_MESSAGE)
            Constants.getDefaultAudio().play();
    }

    /**
     * Shows message in the chat conversation panel when
     * delivered.
     *
     * @param evt the event containing details on the message delivery
     */
    public void messageDelivered(MessageDeliveredEvent evt) {

        Message msg = evt.getSourceMessage();
        Hashtable waitToBeDelivered = this.mainFrame.getWaitToBeDeliveredMsgs();
        String msgUID = msg.getMessageUID();

        if (waitToBeDelivered.containsKey(msgUID)) {
            ChatPanel chatPanel = (ChatPanel) waitToBeDelivered.get(msgUID);
            
            ProtocolProviderService protocolProvider = evt
                    .getDestinationContact().getProtocolProvider();

            chatPanel.processMessage(
                    this.mainFrame.getAccount(protocolProvider),
                    evt.getTimestamp(), Constants.OUTGOING_MESSAGE,
                    msg.getContent());
            
            chatPanel.refreshWriteArea();
        }
    }

    /**
     * Shows message to the user when message delivery failed.
     *
     * @param evt the event containing details on the message delivery failure
     */
    public void messageDeliveryFailed(MessageDeliveryFailedEvent evt) {
        SIPCommMsgTextArea msg = new SIPCommMsgTextArea(
                Messages.getString("msgNotDelivered", evt
                .getDestinationContact().getDisplayName()));
        String title = Messages.getString("msgDeliveryFailure");

        JOptionPane.showMessageDialog(this, msg, title,
                JOptionPane.WARNING_MESSAGE);
    }

    /**
     * Informs the user what is the typing state of his chat contacts.
     *
     * @param evt the event containing details on the typing notification
     */
    public void typingNotificationReceifed(TypingNotificationEvent evt) {
        if (typingTimer.isRunning())
            typingTimer.stop();

        String notificationMsg = "";

        String contactName = this.mainFrame.getContactList()
                .findMetaContactByContact(evt.getSourceContact())
                .getDisplayName()
                + " ";

        if (contactName.equals("")) {
            contactName = Messages.getString("unknown") + " ";
        }

        int typingState = evt.getTypingState();
        MetaContact metaContact = mainFrame.getContactList()
                .findMetaContactByContact(evt.getSourceContact());

        if (typingState == OperationSetTypingNotifications.STATE_TYPING) {
            notificationMsg = Messages.getString("contactTyping", contactName);
            typingTimer.setMetaContact(metaContact);
            typingTimer.start();
        } 
        else if (typingState == OperationSetTypingNotifications.STATE_PAUSED) {
            notificationMsg = Messages.getString("contactPausedTyping",
                    contactName);
            typingTimer.setMetaContact(metaContact);
            typingTimer.start();
        } 
        else if (typingState == OperationSetTypingNotifications.STATE_STOPPED) {
            notificationMsg = "";
        } 
        else if (typingState == OperationSetTypingNotifications.STATE_STALE) {
            notificationMsg = Messages.getString("contactTypingStateStale");
        } 
        else if (typingState == OperationSetTypingNotifications.STATE_UNKNOWN) {
            //TODO: Implement state unknown
        }
        this.setChatNotificationMsg(metaContact, notificationMsg);
    }

    /**
     * Sets the typing notification message at the appropriate chat.
     *
     * @param metaContact The meta contact.
     * @param notificationMsg The typing notification message.
     */
    public void setChatNotificationMsg(MetaContact metaContact,
            String notificationMsg) {
        if (!Constants.TABBED_CHAT_WINDOW) {
            //If in mode "open all messages in new window"
            if (contactMsgWindows.containsKey(metaContact)) {
                ChatWindow msgWindow = (ChatWindow) contactMsgWindows
                        .get(metaContact);
                msgWindow.getChatPanel(metaContact)
                    .setChatStatus(notificationMsg);
            }
        } else if (tabbedChatWindow != null) {
            Hashtable contactTabsTable 
                = tabbedChatWindow.getContactChatsTable();

            if (contactTabsTable.get(metaContact.getMetaUID()) != null) {

                tabbedChatWindow.getChatPanel(metaContact)
                    .setChatStatus(notificationMsg);
            }
        }
    }

    /**
     * Updates the status of the given metacontact in all opened
     * chats containing this contact.
     *
     * @param metaContact the contact whose status we will be updating
     */
    public void updateChatContactStatus(MetaContact metaContact) {

        ContactListModel listModel = (ContactListModel) this.getContactList()
                .getModel();

        if (!Constants.TABBED_CHAT_WINDOW) {
            if (contactMsgWindows.containsKey(metaContact)) {
                ChatWindow msgWindow = (ChatWindow) contactMsgWindows
                        .get(metaContact);
                msgWindow.getCurrentChatPanel().updateContactStatus(
                        listModel.getMetaContactStatus(metaContact));
            }
        } else if (tabbedChatWindow != null) {

            Hashtable contactTabsTable 
                = tabbedChatWindow.getContactChatsTable();

            ChatPanel chatPanel = (ChatPanel) contactTabsTable.get(metaContact
                    .getMetaUID());

            if (chatPanel != null) {
                if (tabbedChatWindow.getTabCount() > 0) {
                    tabbedChatWindow.setTabIcon(metaContact, listModel
                            .getMetaContactStatusIcon(metaContact));
                }
                chatPanel.updateContactStatus(listModel
                        .getMetaContactStatus(metaContact));
            }
        }
    }

    public ChatWindow getTabbedChatWindow() {
        return tabbedChatWindow;
    }

    public void setTabbedChatWindow(ChatWindow tabbedChatWindow) {
        this.tabbedChatWindow = tabbedChatWindow;
    }

    /**
     * Opens chat window when the selected value is a MetaContact 
     * and opens a group when the selected value is a MetaContactGroup.
     */
    private class RunMessageWindowAction extends AbstractAction {
        public void actionPerformed(ActionEvent e) {
            Object selectedValue = getContactList().getSelectedValue();
            
            if(selectedValue instanceof MetaContact) {
                MetaContact contact = (MetaContact) selectedValue;
                
                SwingUtilities.invokeLater(new RunMessageWindow(contact));
            }
            else if (selectedValue instanceof MetaContactGroup) {
                MetaContactGroup group = (MetaContactGroup) selectedValue;
                
                ContactListModel model 
                    = (ContactListModel)contactList.getModel();
                
                if (model.isGroupClosed(group)) {
                    model.openGroup(group);
                }
            }
        }
    };
    
    /**
     * The TypingTimer is started after a PAUSED typing notification
     * is received. It waits 5 seconds and if no other typing event
     * occurs removes the PAUSED message from the chat status panel.
     */
    private class TypingTimer extends Timer {

        private MetaContact metaContact;

        public TypingTimer() {
            //Set delay
            super(5 * 1000, null);

            this.addActionListener(new TimerActionListener());
        }

        private class TimerActionListener implements ActionListener {
            public void actionPerformed(ActionEvent e) {
                setChatNotificationMsg(metaContact, "");
            }
        }

        private void setMetaContact(MetaContact metaContact) {
            this.metaContact = metaContact;
        }
    }
    
    public boolean isChatOpenedForContact (Contact contact) {
        MetaContact metaContact = mainFrame.getContactList()
            .findMetaContactByContact(contact);        

        if(!Constants.TABBED_CHAT_WINDOW) {
            return contactMsgWindows.containsKey(metaContact);
        }
        else {
            if(tabbedChatWindow != null) {
                return tabbedChatWindow.getContactChatsTable()
                    .contains(metaContact.getMetaUID());
            }
            else {
                return false;
            }
        }
    }    
}
