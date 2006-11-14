/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.contactlist;

import java.util.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.Timer;

import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.main.message.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;

/**
 * The contactlist panel not only contains the contact list but it has the role
 * of a message dispatcher. It process all sent and received messages as well as
 * all typing notifications. Here are managed all contact list mouse events.
 * 
 * @author Yana Stamcheva
 */
public class ContactListPanel extends JScrollPane implements MessageListener,
        TypingNotificationsListener, ContactListListener
{

    private MainFrame mainFrame;

    private ContactList contactList;

    private JPanel treePanel = new JPanel(new BorderLayout());

    private Hashtable contactMsgWindows = new Hashtable();

    private ChatWindow tabbedChatWindow;

    private TypingTimer typingTimer = new TypingTimer();

    private CommonRightButtonMenu commonRightButtonMenu;

    /**
     * Creates the contactlist scroll panel defining the parent frame.
     * 
     * @param mainFrame The parent frame.
     */
    public ContactListPanel(MainFrame mainFrame) {

        this.mainFrame = mainFrame;

        this.getViewport().add(treePanel);

        this
                .setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        this.getVerticalScrollBar().setUnitIncrement(30);
    }

    /**
     * Initializes the contact list.
     * 
     * @param contactListService The MetaContactListService which will be used
     *            for a contact list data model.
     */
    public void initList(MetaContactListService contactListService)
    {
        this.contactList = new ContactList(mainFrame);

        this.contactList.addContactListListener(this);
        this.treePanel.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e)
            {

                if ((e.getModifiers() & InputEvent.BUTTON3_MASK) != 0) {
                    commonRightButtonMenu = new CommonRightButtonMenu(mainFrame);

                    commonRightButtonMenu.setInvoker(treePanel);

                    commonRightButtonMenu.setLocation(e.getX()
                            + mainFrame.getX() + 5, e.getY() + mainFrame.getY()
                            + 105);

                    commonRightButtonMenu.setVisible(true);
                }
            }
        });

        this.treePanel.add(contactList, BorderLayout.NORTH);

        this.treePanel.setBackground(Color.WHITE);
        this.contactList.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        this.getRootPane().getActionMap().put("runChat",
                new RunMessageWindowAction());

        InputMap imap = this.getRootPane().getInputMap(
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "runChat");
    }

    /**
     * Returns the contact list.
     * 
     * @return the contact list
     */
    public ContactList getContactList()
    {
        return this.contactList;
    }

    /**
     * Implements the ContactListListener.contactSelected method.
     */
    public void contactSelected(ContactListEvent evt)
    {
        SwingUtilities
                .invokeLater(new RunMessageWindow(evt.getSourceContact()));
    }

    /**
     * Implements the ContactListListener.protocolContactSelected method.
     */
    public void protocolContactSelected(ContactListEvent evt)
    {
        SwingUtilities.invokeLater(new RunMessageWindow(evt.getSourceContact(),
                evt.getSourceProtoContact()));
    }

    /**
     * Runs the chat window for the specified contact. We examine different
     * cases here, depending on the chat window mode.
     * 
     * In mode "Open messages in new window" a new window is opened for the
     * given <tt>MetaContact</tt> if there's no opened window for it,
     * otherwise the existing chat window is made visible and focused.
     * 
     * In mode "Group messages in one chat window" a JTabbedPane is used to show
     * chats for different contacts in ona window. A new tab is opened for the
     * given <tt>MetaContact</tt> if there's no opened tab for it, otherwise
     * the existing chat tab is selected and focused.
     * 
     * @author Yana Stamcheva
     */
    public class RunMessageWindow implements Runnable
    {

        private MetaContact contactItem;

        private Contact protocolContact;

        public RunMessageWindow(MetaContact contactItem) {
            this.contactItem = contactItem;
            this.protocolContact = contactItem.getDefaultContact();
        }

        public RunMessageWindow(MetaContact contactItem, Contact protocolContact) {
            this.contactItem = contactItem;
            this.protocolContact = protocolContact;
        }

        public void run()
        {
            PresenceStatus contactStatus = ((ContactListModel) contactList
                    .getModel()).getMetaContactStatus(this.contactItem);

            if (!Constants.TABBED_CHAT_WINDOW) {
                // If in mode "open messages in new window"
                if (contactMsgWindows.containsKey(this.contactItem)) {
                    /*
                     * If a chat window for this contact is already opened show
                     * it.
                     */
                    ChatWindow msgWindow = (ChatWindow) contactMsgWindows
                            .get(this.contactItem);

                    if (msgWindow.isVisible()) {
                        if(msgWindow.getState() == JFrame.ICONIFIED)
                            msgWindow.setState(JFrame.NORMAL);
                        
                        msgWindow.toFront();
                    }
                    else
                        msgWindow.setVisible(true);
                }
                else {
                    /*
                     * If there's no chat window for the contact create it and
                     * show it.
                     */
                    ChatWindow msgWindow = new ChatWindow(mainFrame);

                    contactMsgWindows.put(this.contactItem, msgWindow);

                    ChatPanel chatPanel = msgWindow.createChat(
                            this.contactItem, contactStatus, protocolContact);

                    chatPanel.loadHistory();

                    msgWindow.addChat(chatPanel);

                    msgWindow.pack();

                    msgWindow.setVisible(true);

                    msgWindow.getCurrentChatPanel().requestFocusInWriteArea();
                }
            }
            else {
                // If in mode "group messages in one chat window"
                if (tabbedChatWindow == null) {
                    // If there's no open chat window
                    tabbedChatWindow = new ChatWindow(mainFrame);

                    tabbedChatWindow.addWindowListener(new WindowAdapter() {
                        public void windowClosing(WindowEvent e)
                        {
                            tabbedChatWindow = null;
                        }
                    });
                }
                /*
                 * Get the hashtable containg all tabs and corresponding chat
                 * panels.
                 */
                Hashtable contactTabsTable = tabbedChatWindow
                        .getContactChatsTable();

                // If there's no open tab for the given contact.
                if (contactTabsTable.get(this.contactItem.getMetaUID()) == null) {
                    ChatPanel chatPanel = tabbedChatWindow.createChat(
                            this.contactItem, contactStatus, protocolContact);

                    chatPanel.loadHistory();

                    tabbedChatWindow.addChatTab(chatPanel);

                    if (tabbedChatWindow.getTabCount() > 1) {
                        tabbedChatWindow
                                .setSelectedContactTab(this.contactItem);
                    }

                    if (tabbedChatWindow.isVisible()) {
                        
                        if(tabbedChatWindow.getState() == JFrame.ICONIFIED)
                            tabbedChatWindow.setState(JFrame.NORMAL);
                        
                        tabbedChatWindow.toFront();
                    }
                    else
                        tabbedChatWindow.setVisible(true);

                    tabbedChatWindow.getCurrentChatPanel()
                            .requestFocusInWriteArea();
                }
                else {
                    // If a tab for the given contact already exists.
                    if (tabbedChatWindow.getTabCount() > 1) {
                        tabbedChatWindow
                                .setSelectedContactTab(this.contactItem);
                    }

                    if (tabbedChatWindow.isVisible()) {
                        
                        if(tabbedChatWindow.getState() == JFrame.ICONIFIED)
                            tabbedChatWindow.setState(JFrame.NORMAL);
                        tabbedChatWindow.toFront();
                    }
                    else
                        tabbedChatWindow.setVisible(true);

                    tabbedChatWindow.getCurrentChatPanel()
                            .requestFocusInWriteArea();
                }
            }

        }
    }

    /**
     * When a message is received determines whether to open a new chat window
     * or chat window tab, or to indicate that a message is received from a
     * contact which already has an open chat. When the chat is found checks if
     * in mode "Auto popup enabled" and if this is the case shows the message in
     * the appropriate chat panel.
     * 
     * @param evt the event containing details on the received message
     */
    public void messageReceived(MessageReceivedEvent evt)
    {

        Contact protocolContact = evt.getSourceContact();
        Date date = evt.getTimestamp();
        Message message = evt.getSourceMessage();

        MetaContact metaContact = mainFrame.getContactList()
                .findMetaContactByContact(protocolContact);

        PresenceStatus contactStatus = ((ContactListModel) this.contactList
                .getModel()).getMetaContactStatus(metaContact);

        if (!Constants.TABBED_CHAT_WINDOW) {
            // If in mode "open all messages in new window"
            if (contactMsgWindows.containsKey(metaContact)) {
                /*
                 * If a chat window for this contact is already opened show it.
                 */
                ChatWindow msgWindow = (ChatWindow) contactMsgWindows
                        .get(metaContact);

                msgWindow.getCurrentChatPanel().processMessage(
                        protocolContact.getDisplayName(), date,
                        Constants.INCOMING_MESSAGE, message.getContent());

                if (msgWindow.getState() == JFrame.ICONIFIED) {
                    msgWindow.setTitle("*" + msgWindow.getTitle());
                }

                if (Constants.AUTO_POPUP_NEW_MESSAGE) {
                    if(msgWindow.isVisible()) {
                        
                        if(msgWindow.getState() == JFrame.ICONIFIED)
                            msgWindow.setState(JFrame.NORMAL);
                        
                        msgWindow.toFront();
                    }
                    else
                        msgWindow.setVisible(true);
                }
            }
            else {
                ChatWindow msgWindow = new ChatWindow(mainFrame);

                contactMsgWindows.put(metaContact, msgWindow);

                ChatPanel chatPanel = msgWindow.createChat(metaContact,
                        contactStatus, protocolContact);

                chatPanel.loadHistory(message.getMessageUID());
                chatPanel.processMessage(protocolContact.getDisplayName(),
                        date, Constants.INCOMING_MESSAGE, message.getContent());
                /*
                 * If there's no chat window for the contact create it and show
                 * it.
                 */
                if (Constants.AUTO_POPUP_NEW_MESSAGE) {
                    msgWindow.addChat(chatPanel);

                    msgWindow.pack();

                    msgWindow.setVisible(true);

                    chatPanel.setCaretToEnd();
                }
            }
        }
        else {
            // If in mode "group messages in one chat window"
            if (tabbedChatWindow == null) {
                // If there's no open chat window
                tabbedChatWindow = new ChatWindow(mainFrame);

                tabbedChatWindow.addWindowListener(new WindowAdapter() {

                    public void windowClosing(WindowEvent e)
                    {
                        tabbedChatWindow = null;
                    }
                });
            }
            
            Hashtable contactTabsTable = tabbedChatWindow
                    .getContactChatsTable();

            ChatPanel chatPanel;

            // If there's no open tab for the given contact.
            if (contactTabsTable.get(metaContact.getMetaUID()) == null) {
                chatPanel = tabbedChatWindow.createChat(metaContact,
                        contactStatus, protocolContact);

                chatPanel.loadHistory(message.getMessageUID());
                chatPanel.processMessage(protocolContact.getDisplayName(),
                        date, Constants.INCOMING_MESSAGE, message.getContent());

                if (Constants.AUTO_POPUP_NEW_MESSAGE) {
                    tabbedChatWindow.addChatTab(chatPanel);

                    if(tabbedChatWindow.isVisible()) {
                        if(tabbedChatWindow.getState() == JFrame.ICONIFIED)
                            tabbedChatWindow.setState(JFrame.NORMAL);
                        
                        tabbedChatWindow.toFront();
                    }
                    else
                        tabbedChatWindow.setVisible(true);

                    chatPanel.setCaretToEnd();

                    tabbedChatWindow.getCurrentChatPanel()
                            .requestFocusInWriteArea();

                    if (tabbedChatWindow.getTabCount() > 1) {
                        tabbedChatWindow.highlightTab(metaContact);
                    }
                }
            }
            else {
                chatPanel = tabbedChatWindow.getChatPanel(metaContact);

                chatPanel.processMessage(protocolContact.getDisplayName(),
                        date, Constants.INCOMING_MESSAGE, message.getContent());

                if (tabbedChatWindow.getState() == JFrame.ICONIFIED) {
                    if (tabbedChatWindow.getTabCount() > 1) {
                        tabbedChatWindow.setSelectedContactTab(metaContact);
                    }

                    if (!tabbedChatWindow.getTitle().startsWith("*")) {
                        tabbedChatWindow.setTitle(
                                "*" + tabbedChatWindow.getTitle());
                    }
                }
                else {
                    if (tabbedChatWindow.getTabCount() > 1) {
                        tabbedChatWindow.highlightTab(metaContact);
                    }
                    
                    if(tabbedChatWindow.isVisible())
                        tabbedChatWindow.toFront();
                    else
                        tabbedChatWindow.setVisible(true);
                }
            }
        }

        if (Constants.AUTO_POPUP_NEW_MESSAGE)
            Constants.getDefaultMessageAudio().play();
    }

    /**
     * When a sent message is delivered shows it in the chat conversation panel.
     * 
     * @param evt the event containing details on the message delivery
     */
    public void messageDelivered(MessageDeliveredEvent evt)
    {

        Message msg = evt.getSourceMessage();
        Hashtable waitToBeDelivered = this.mainFrame.getWaitToBeDeliveredMsgs();
        String msgUID = msg.getMessageUID();

        if (waitToBeDelivered.containsKey(msgUID)) {
            ChatPanel chatPanel = (ChatPanel) waitToBeDelivered.get(msgUID);

            ProtocolProviderService protocolProvider = evt
                    .getDestinationContact().getProtocolProvider();

            chatPanel.processMessage(this.mainFrame
                    .getAccount(protocolProvider), evt.getTimestamp(),
                    Constants.OUTGOING_MESSAGE, msg.getContent());

            chatPanel.refreshWriteArea();
        }
    }

    /**
     * Shows a warning message to the user when message delivery has failed.
     * 
     * @param evt the event containing details on the message delivery failure
     */
    public void messageDeliveryFailed(MessageDeliveryFailedEvent evt)
    {

        SIPCommMsgTextArea msg = null;

        if (evt.getErrorCode() == MessageDeliveryFailedEvent.OFFLINE_MESSAGES_NOT_SUPPORTED) {

            msg = new SIPCommMsgTextArea(Messages.getString(
                    "msgDeliveryOfflineNotSupported", evt
                            .getDestinationContact().getDisplayName()));
        }
        else if (evt.getErrorCode() == MessageDeliveryFailedEvent.NETWORK_FAILURE) {
            msg = new SIPCommMsgTextArea(Messages.getString("msgNotDelivered",
                    evt.getDestinationContact().getDisplayName()));
        }
        else if (evt.getErrorCode() == MessageDeliveryFailedEvent.PROVIDER_NOT_REGISTERED) {

            msg = new SIPCommMsgTextArea(Messages.getString(
                    "msgSendConnectionProblem", evt.getDestinationContact()
                            .getDisplayName()));
        }
        else if (evt.getErrorCode() == MessageDeliveryFailedEvent.INTERNAL_ERROR) {
            msg = new SIPCommMsgTextArea(Messages.getString(
                    "msgDeliveryInternalError", evt.getDestinationContact()
                            .getDisplayName()));
        }
        else {
            msg = new SIPCommMsgTextArea(Messages.getString(
                    "msgDeliveryFailedUnknownError", evt
                    .getDestinationContact().getDisplayName()));
        }

        String title = Messages.getString("msgDeliveryFailure");

        JOptionPane.showMessageDialog(this, msg, title,
                JOptionPane.WARNING_MESSAGE);
    }

    /**
     * Informs the user what is the typing state of his chat contacts.
     * 
     * @param evt the event containing details on the typing notification
     */
    public void typingNotificationReceifed(TypingNotificationEvent evt)
    {
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
            // TODO: Implement state unknown
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
            String notificationMsg)
    {
        if (!Constants.TABBED_CHAT_WINDOW) {
            // If in mode "open all messages in new window"
            if (contactMsgWindows.containsKey(metaContact)) {
                ChatWindow msgWindow = (ChatWindow) contactMsgWindows
                        .get(metaContact);
                msgWindow.getChatPanel(metaContact).setStatusMessage(
                        notificationMsg);
            }
        }
        else if (tabbedChatWindow != null) {
            Hashtable contactTabsTable = tabbedChatWindow
                    .getContactChatsTable();

            if (contactTabsTable.get(metaContact.getMetaUID()) != null) {

                tabbedChatWindow.getChatPanel(metaContact).setStatusMessage(
                        notificationMsg);
            }
        }
    }

    /**
     * Updates the status of the given metacontact in all opened chats
     * containing this contact.
     * 
     * @param metaContact the contact whose status we will be updating
     */
    public void updateChatContactStatus(MetaContact metaContact,
            Contact protoContact)
    {

        ContactListModel listModel = (ContactListModel) this.getContactList()
                .getModel();

        if (!Constants.TABBED_CHAT_WINDOW) {
            if (contactMsgWindows.containsKey(metaContact)) {
                ChatWindow msgWindow = (ChatWindow) contactMsgWindows
                        .get(metaContact);
                msgWindow.getCurrentChatPanel().updateContactStatus(
                        protoContact);
            }
        }
        else if (tabbedChatWindow != null) {

            Hashtable contactTabsTable = tabbedChatWindow
                    .getContactChatsTable();

            ChatPanel chatPanel = (ChatPanel) contactTabsTable.get(metaContact
                    .getMetaUID());

            if (chatPanel != null) {
                if (tabbedChatWindow.getTabCount() > 0) {
                    tabbedChatWindow.setTabIcon(metaContact, listModel
                            .getMetaContactStatusIcon(metaContact));
                }
                chatPanel.updateContactStatus(protoContact);
            }
        }
    }

    /**
     * Returns the <tt>ChatWindow</tt>, when in mode "Group messages in one
     * window".
     * 
     * @return the <tt>ChatWindow</tt>, when in mode "Group messages in one
     *         window"
     */
    public ChatWindow getTabbedChatWindow()
    {
        return tabbedChatWindow;
    }

    /**
     * Sets the <tt>ChatWindow</tt>, when in mode "Group messages in one
     * window".
     * 
     * @param tabbedChatWindow The <tt>ChatWindow</tt> to set.
     */
    public void setTabbedChatWindow(ChatWindow tabbedChatWindow)
    {
        this.tabbedChatWindow = tabbedChatWindow;
    }

    /**
     * Opens chat window when the selected value is a MetaContact and opens a
     * group when the selected value is a MetaContactGroup.
     */
    private class RunMessageWindowAction extends AbstractAction
    {
        public void actionPerformed(ActionEvent e)
        {
            Object selectedValue = getContactList().getSelectedValue();

            if (selectedValue instanceof MetaContact) {
                MetaContact contact = (MetaContact) selectedValue;

                SwingUtilities.invokeLater(new RunMessageWindow(contact));
            }
            else if (selectedValue instanceof MetaContactGroup) {
                MetaContactGroup group = (MetaContactGroup) selectedValue;

                ContactListModel model = (ContactListModel) contactList
                        .getModel();

                if (model.isGroupClosed(group)) {
                    model.openGroup(group);
                }
            }
        }
    };

    /**
     * The TypingTimer is started after a PAUSED typing notification is
     * received. It waits 5 seconds and if no other typing event occurs removes
     * the PAUSED message from the chat status panel.
     */
    private class TypingTimer extends Timer
    {

        private MetaContact metaContact;

        public TypingTimer() {
            // Set delay
            super(5 * 1000, null);

            this.addActionListener(new TimerActionListener());
        }

        private class TimerActionListener implements ActionListener
        {
            public void actionPerformed(ActionEvent e)
            {
                setChatNotificationMsg(metaContact, "");
            }
        }

        private void setMetaContact(MetaContact metaContact)
        {
            this.metaContact = metaContact;
        }
    }

    /**
     * Checks if there is an open chat tab or window for the given contact,
     * depending on the chat mode and if this is the case returns
     * <code>true</code>, otherwise returns <code>false</code>.
     * 
     * @param contact The <tt>Contact</tt> for which to check.
     * @return <code>true</code> if there is an open chat tab or chat window
     *         for the given contact, <code>false</code> otherwise.
     */
    public boolean isChatOpenedForContact(Contact contact)
    {
        MetaContact metaContact = mainFrame.getContactList()
                .findMetaContactByContact(contact);

        if (!Constants.TABBED_CHAT_WINDOW) {
            return contactMsgWindows.containsKey(metaContact);
        }
        else {
            if (tabbedChatWindow != null) {
                return tabbedChatWindow.getContactChatsTable().contains(
                        metaContact.getMetaUID());
            }
            else {
                return false;
            }
        }
    }

    public CommonRightButtonMenu getCommonRightButtonMenu()
    {
        return commonRightButtonMenu;
    }
}
