/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.contactlist;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import javax.swing.Timer;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.main.message.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * The contactlist panel not only contains the contact list but it has the role
 * of a message dispatcher. It process all sent and received messages as well as
 * all typing notifications. Here are managed all contact list mouse events.
 * 
 * @author Yana Stamcheva
 */
public class ContactListPanel
    extends JScrollPane
    implements  MessageListener,
                TypingNotificationsListener,
                ContactListListener
{

    private MainFrame mainFrame;

    private ContactList contactList;

    private JPanel treePanel = new JPanel(new BorderLayout());

    private TypingTimer typingTimer = new TypingTimer();

    private CommonRightButtonMenu commonRightButtonMenu;
    
    private Logger logger = Logger.getLogger(ContactListPanel.class);
    
    private ChatWindowManager chatWindowManager;

    /**
     * Creates the contactlist scroll panel defining the parent frame.
     * 
     * @param mainFrame The parent frame.
     */
    public ContactListPanel(MainFrame mainFrame) {

        this.mainFrame = mainFrame;

        this.chatWindowManager = mainFrame.getChatWindowManager();
        
        this.getViewport().add(treePanel);

        this.setHorizontalScrollBarPolicy(
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

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
     * Implements the ContactListListener.groupSelected method.
     */
    public void groupSelected(ContactListEvent evt)
    {}

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

        private MetaContact metaContact;

        private Contact protocolContact;

        public RunMessageWindow(MetaContact metaContact)
        {
            this.metaContact = metaContact;
        }

        public RunMessageWindow(MetaContact metaContact, Contact protocolContact) {
            this.metaContact = metaContact;
            this.protocolContact = protocolContact;
        }

        public void run()
        {
            if(protocolContact != null)
                chatWindowManager.openChat(metaContact, protocolContact, true);
            else
                chatWindowManager.openChat(metaContact, true);
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
        logger.trace("MESSAGE RECEIVED from contact: "
            + evt.getSourceContact().getAddress());
        
        Contact protocolContact = evt.getSourceContact();
        Date date = evt.getTimestamp();
        Message message = evt.getSourceMessage();

        MetaContact metaContact = mainFrame.getContactList()
                .findMetaContactByContact(protocolContact);

        ChatPanel chatPanel;
        
        if (!Constants.TABBED_CHAT_WINDOW) {
            // If in mode "open all messages in new window"
            if (chatWindowManager.containsContactChat(metaContact))
            {
                /*
                 * If a chat window for this contact is already opened show it.
                 */
                chatPanel = chatWindowManager.getContactChat(metaContact);
                
                chatPanel.processMessage(
                        protocolContact.getDisplayName(), date,
                        Constants.INCOMING_MESSAGE, message.getContent());
            }
            else
            {   
                chatPanel = chatWindowManager
                    .createChat(metaContact, protocolContact);

                chatPanel.processMessage(protocolContact.getDisplayName(),
                        date, Constants.INCOMING_MESSAGE, message.getContent());
            }
        }
        else {
                       
            // If there's no open tab for the given contact.
            if (!chatWindowManager.containsContactChat(metaContact)) {
                
                logger.trace("MESSAGE RECEIVED: create new chat for contact: "
                    + evt.getSourceContact().getAddress());
                
                chatPanel = chatWindowManager
                    .createChat(metaContact, protocolContact);
                
                logger.trace("MESSAGE RECEIVED: process message in chat for contact: "
                    + evt.getSourceContact().getAddress());
                
                chatPanel.processMessage(protocolContact.getDisplayName(),
                        date, Constants.INCOMING_MESSAGE, message.getContent());
            }
            else {
                logger.trace("MESSAGE RECEIVED: get existing chat for contact: "
                    + evt.getSourceContact().getAddress());
                
                chatPanel = chatWindowManager.getContactChat(metaContact);

                logger.trace("MESSAGE RECEIVED: process message in chat for contact: "
                    + evt.getSourceContact().getAddress());
                
                chatPanel.processMessage(protocolContact.getDisplayName(),
                        date, Constants.INCOMING_MESSAGE, message.getContent());                
            }
        }
        
        chatWindowManager.openChat(metaContact, false);
        
        GuiActivator.getAudioNotifier()
            .createAudio(Sounds.INCOMING_MESSAGE).play();
        
        if (!chatPanel.getProtocolContact().getProtocolProvider()
            .equals(protocolContact.getProtocolProvider()))
        {
            chatPanel.setProtocolContact(protocolContact);
        }
    }

    /**
     * When a sent message is delivered shows it in the chat conversation panel.
     * 
     * @param evt the event containing details on the message delivery
     */
    public void messageDelivered(MessageDeliveredEvent evt)
    {
        logger.trace("MESSAGE DELIVERED to contact: "
            + evt.getDestinationContact().getAddress());
        
        Message msg = evt.getSourceMessage();
        Hashtable waitToBeDelivered = this.mainFrame.getWaitToBeDeliveredMsgs();
        String msgUID = msg.getMessageUID();

        if (waitToBeDelivered.containsKey(msgUID)) {
            ChatPanel chatPanel = (ChatPanel) waitToBeDelivered.get(msgUID);

            ProtocolProviderService protocolProvider = evt
                    .getDestinationContact().getProtocolProvider();

            logger.trace("MESSAGE DELIVERED: process message to chat for contact: "
                    + evt.getDestinationContact().getAddress());
            
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
        String errorMsg = null;
     
        Message sourceMessage = (Message) evt.getSource();
        
        Contact sourceContact = evt.getDestinationContact();
        
        MetaContact metaContact = mainFrame.getContactList()
            .findMetaContactByContact(sourceContact);
        
        PresenceStatus contactStatus = ((ContactListModel) this.contactList
                .getModel()).getMetaContactStatus(metaContact);
                
        if (evt.getErrorCode() 
                == MessageDeliveryFailedEvent.OFFLINE_MESSAGES_NOT_SUPPORTED) {

            errorMsg = Messages.getI18NString(
                    "msgDeliveryOfflineNotSupported").getText();
        }
        else if (evt.getErrorCode()
                == MessageDeliveryFailedEvent.NETWORK_FAILURE) {
            
            errorMsg = Messages.getI18NString("msgNotDelivered").getText();
        }
        else if (evt.getErrorCode()
                == MessageDeliveryFailedEvent.PROVIDER_NOT_REGISTERED) {

            errorMsg = Messages.getI18NString(
                    "msgSendConnectionProblem").getText();
        }
        else if (evt.getErrorCode()
                == MessageDeliveryFailedEvent.INTERNAL_ERROR) {
            
            errorMsg = Messages.getI18NString(
                    "msgDeliveryInternalError").getText();
        }
        else {
            errorMsg = Messages.getI18NString(
                    "msgDeliveryFailedUnknownError").getText();
        }
        
        ChatPanel chatPanel;
        ChatWindow chatWindow;
        
        if (!Constants.TABBED_CHAT_WINDOW) {
            // If in mode "open all messages in new window"
            if (chatWindowManager.containsContactChat(metaContact))
            {   
                chatPanel = chatWindowManager.getContactChat(metaContact);
                
                chatWindow = chatPanel.getChatWindow();
                
                chatPanel.refreshWriteArea();
                
                chatPanel.processMessage(
                        metaContact.getDisplayName(),
                        new Date(System.currentTimeMillis()),
                        Constants.OUTGOING_MESSAGE,
                        sourceMessage.getContent());
                
                chatPanel.processMessage(
                        metaContact.getDisplayName(),
                        new Date(System.currentTimeMillis()),
                        Constants.ERROR_MESSAGE,
                        errorMsg);        
            }
            else
            {   
                chatPanel = chatWindowManager.createChat(
                    metaContact, sourceContact);
                
                chatPanel.processMessage(
                        metaContact.getDisplayName(),
                        new Date(System.currentTimeMillis()),
                        Constants.OUTGOING_MESSAGE,
                        sourceMessage.getContent());
                
                chatPanel.processMessage(
                        metaContact.getDisplayName(),
                        new Date(System.currentTimeMillis()),
                        Constants.ERROR_MESSAGE,
                        errorMsg);
            }
        }
        else
        {   
            if (chatWindowManager.containsContactChat(metaContact))
            {
                chatPanel = chatWindowManager.getContactChat(metaContact);
                
                chatWindow = chatPanel.getChatWindow();
                
                chatPanel.refreshWriteArea();
                chatPanel.processMessage(
                        metaContact.getDisplayName(),
                        new Date(System.currentTimeMillis()),
                        Constants.OUTGOING_MESSAGE,
                        sourceMessage.getContent());
                
                chatPanel.processMessage(
                        metaContact.getDisplayName(),
                        new Date(System.currentTimeMillis()),
                        Constants.ERROR_MESSAGE,
                        errorMsg);
            }
            else {
                chatPanel = chatWindowManager.createChat(
                    metaContact, sourceContact);

                chatPanel.processMessage(
                        metaContact.getDisplayName(),
                        new Date(System.currentTimeMillis()),
                        Constants.OUTGOING_MESSAGE,
                        sourceMessage.getContent());
                
                chatPanel.processMessage(
                        metaContact.getDisplayName(),
                        new Date(System.currentTimeMillis()),
                        Constants.ERROR_MESSAGE,
                        errorMsg);                                
            }            
        }
        
        chatWindowManager.openChat(metaContact, false);
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
            contactName = Messages.getI18NString("unknown").getText() + " ";
        }

        int typingState = evt.getTypingState();
        MetaContact metaContact = mainFrame.getContactList()
                .findMetaContactByContact(evt.getSourceContact());

        if (typingState == OperationSetTypingNotifications.STATE_TYPING) {
            notificationMsg
                = Messages.getI18NString("contactTyping", contactName).getText();
            
            typingTimer.setMetaContact(metaContact);
            typingTimer.start();
        }
        else if (typingState == OperationSetTypingNotifications.STATE_PAUSED) {
            notificationMsg = Messages.getI18NString("contactPausedTyping",
                    contactName).getText();
            typingTimer.setMetaContact(metaContact);
            typingTimer.start();
        }
        else if (typingState == OperationSetTypingNotifications.STATE_STOPPED) {
            notificationMsg = "";
        }
        else if (typingState == OperationSetTypingNotifications.STATE_STALE) {
            notificationMsg
                = Messages.getI18NString("contactTypingStateStale").getText();
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
        if (chatWindowManager.containsContactChat(metaContact))
        {
            chatWindowManager.getContactChat(metaContact)
                .setStatusMessage(notificationMsg);
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
        
        if (chatWindowManager.containsContactChat(metaContact))
        {
            ChatPanel chatPanel = chatWindowManager.getContactChat(metaContact);
            
            chatPanel.updateContactStatus(metaContact, protoContact);
            
            if(Constants.TABBED_CHAT_WINDOW)
            {
                ChatWindow chatWindow = chatPanel.getChatWindow();
                
                if (chatWindow.getTabCount() > 0) {
                    chatWindow.setTabIcon(chatPanel, listModel
                            .getMetaContactStatusIcon(metaContact));
                }
            }
        }
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
    
    public CommonRightButtonMenu getCommonRightButtonMenu()
    {
        return commonRightButtonMenu;
    }    
}
