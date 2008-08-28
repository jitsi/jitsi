/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chat;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.contactlist.event.*;
import net.java.sip.communicator.service.msghistory.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * The <tt>MetaContactChatPanel</tt> is the single user chat <tt>ChatPanel</tt>.
 * It extends the <tt>ChatPanel</tt> in order to provide to it single user chat
 * functionalities
 *
 * @author Yana Stamcheva
 * @author Lubomir Marinov
 */
public class MetaContactChatPanel
    extends ChatPanel
    implements  MetaContactListListener,
                SubscriptionListener
{
    private static final Logger logger = Logger
        .getLogger(MetaContactChatPanel.class.getName());

    private MetaContact metaContact;

    private Date firstHistoryMsgTimestamp;

    private Date lastHistoryMsgTimestamp;

    private JCheckBox sendSmsCheckBox = new JCheckBox(
        Messages.getI18NString("sendAsSms").getText());

    /*
     * There is some problem when adding the icon to the check box, the check
     * box disappears.
     * 
     * new ImageIcon(ImageLoader.getImage(ImageLoader.SEND_SMS_ICON))
     */

    private ProtocolContactSelectorBox contactSelectorBox;
    
    private Message sentSmsMessage = null;

    /**
     * Creates a <tt>MetaContactChatPanel</tt> which is added to the given chat
     * window.
     *
     * @param chatWindow The parent window of this chat panel.
     * @param metaContact the meta contact that this chat is about.
     * @param protocolContact The subContact which is selected ins
     * the chat.
     */
    public MetaContactChatPanel(    ChatWindow chatWindow,
                                    MetaContact metaContact,
                                    Contact protocolContact)
    {
        super(chatWindow);

        this.metaContact = metaContact;

        ChatContact chatContact = new ChatContact(metaContact, protocolContact);

        //Add the contact to the list of contacts contained in this panel
        getChatContactListPanel().addContact(chatContact);
        
        setupListeners(true);

        // Initialize the "send via" selector box and adds it to the send panel.
        contactSelectorBox = new ProtocolContactSelectorBox(
            this, metaContact, protocolContact);

        JPanel sendPanel = getChatSendPanel().getSendPanel();
        sendPanel.add(contactSelectorBox, 0);
        sendPanel.add(new JLabel(Messages.getI18NString("sendVia").getText()),
            0);

        //Enables to change the protocol provider by simply pressing the CTRL-P
        //key combination
        ActionMap amap = this.getActionMap();

        amap.put("ChangeProtocol", new ChangeProtocolAction());

        InputMap imap = this.getInputMap(
            JComponent.WHEN_IN_FOCUSED_WINDOW); 

        imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_P,
            KeyEvent.CTRL_DOWN_MASK), "ChangeProtocol");
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.java.sip.communicator.impl.gui.main.chat.ChatPanel#dispose()
     */
    public void dispose()
    {
        try
        {
            setupListeners(false);
        }
        finally
        {
            super.dispose();
        }
    }

    /**
     * Adds or removes the listeners which observe the model displayed by this
     * view.
     * 
     * @param add <tt>true</tt> to add the listeners to the model displayed by
     *            this view; <tt>false</tt> to remove the previously-installed
     *            listeners from the model
     */
    private void setupListeners(boolean add)
    {
        // Obtain the MetaContactListService and add this class to it as a
        // listener of all events concerning the contact list.
        MetaContactListService contactList =
            chatWindow.getMainFrame().getContactList();
        if (add)
        {
            contactList.addMetaContactListListener(this);
        }
        else
        {
            contactList.removeMetaContactListListener(this);
        }

        // Detect contact properties changes (photo) and updates them
        for (Iterator iter = metaContact.getContacts(); iter.hasNext();)
        {
            Contact contact = (Contact) iter.next();
            OperationSetPresence opsPresence =
                (OperationSetPresence) contact.getProtocolProvider()
                    .getOperationSet(OperationSetPresence.class);
            if (opsPresence != null)
            {
                if (add)
                {
                    opsPresence.addSubsciptionListener(this);
                }
                else
                {
                    opsPresence.removeSubscriptionListener(this);
                }
            }
        }
    }

    /**
     * Loads history for the chat meta contact in a separate thread. Implements
     * the <tt>ChatPanel.loadHistory</tt> method.
     */
    public void loadHistory()
    {
        final MessageHistoryService msgHistory
            = GuiActivator.getMsgHistoryService();

        // If the MessageHistoryService is not registered we have nothing to do
        // here. The MessageHistoryService could be "disabled" from the user
        // through one of the configuration forms.
        if (msgHistory == null)
            return;

        new Thread()
        {
            public void run()
            {
                // Load the history period, which initializes the
                // firstMessageTimestamp and the lastMessageTimeStamp variables.
                // Used to disable/enable history flash buttons in the chat
                // window tool bar.
                loadHistoryPeriod();
                
                // Load the last N=CHAT_HISTORY_SIZE messages from history.
                Collection historyList = msgHistory.findLast(
                        metaContact, ConfigurationManager.getChatHistorySize());

                if(historyList.size() > 0) {
                    class ProcessHistory implements Runnable
                    {
                        Collection historyList;
                        
                        ProcessHistory(Collection historyList)
                        {
                            this.historyList = historyList;
                        }
                        
                        public void run()
                        {
                            processHistory(historyList, null);
                        }
                    }
                    SwingUtilities.invokeLater(new ProcessHistory(historyList));
                }
            }
        }.start();
    }

    /**
     * Loads history messages ignoring the message given by the
     * escapedMessageID. Implements the
     * <tt>ChatPanel.loadHistory(String)</tt> method.
     * 
     * @param escapedMessageID The id of the message that should be ignored.
     */
    public void loadHistory(final String escapedMessageID)
    {
        final MessageHistoryService msgHistory
            = GuiActivator.getMsgHistoryService();

        // If the MessageHistoryService is not registered we have nothing to do
        // here. The MessageHistoryService could be "disabled" from the user
        // through one of the configuration forms.
        if (msgHistory == null)
            return;

        // Load the history period, which initializes the
        // firstMessageTimestamp and the lastMessageTimeStamp variables.
        // Used to disable/enable history flash buttons in the chat
        // window tool bar.
        loadHistoryPeriod();
        
        Collection historyList = msgHistory.findLast(
                metaContact, ConfigurationManager.getChatHistorySize());

        processHistory(historyList, escapedMessageID);
    }
    
    /**
     * Loads history period dates for the current chat.
     */
    private void loadHistoryPeriod()
    {
        MessageHistoryService msgHistory
            = GuiActivator.getMsgHistoryService();

        // If the MessageHistoryService is not registered we have nothing to do
        // here. The MessageHistoryService could be "disabled" from the user
        // through one of the configuration forms.
        if (msgHistory == null)
            return;

        Collection firstMessage = msgHistory
            .findFirstMessagesAfter(metaContact, new Date(0), 1);
        
        if(firstMessage.size() > 0)
        {
            Iterator i = firstMessage.iterator();

            Object o = i.next();

            if(o instanceof MessageDeliveredEvent)
            {
                MessageDeliveredEvent evt
                    = (MessageDeliveredEvent)o;
                
                this.firstHistoryMsgTimestamp = evt.getTimestamp();
            }
            else if(o instanceof MessageReceivedEvent)
            {
                MessageReceivedEvent evt = (MessageReceivedEvent)o;
                
                this.firstHistoryMsgTimestamp = evt.getTimestamp();
            }            
        }
        
        Collection lastMessage = msgHistory
            .findLastMessagesBefore(metaContact, new Date(Long.MAX_VALUE), 1);
    
        if(lastMessage.size() > 0)
        {
            Iterator i1 = lastMessage.iterator();
            
            Object o1 = i1.next();
        
            if(o1 instanceof MessageDeliveredEvent)
            {
                MessageDeliveredEvent evt
                    = (MessageDeliveredEvent)o1;
        
                this.lastHistoryMsgTimestamp = evt.getTimestamp();
            }
            else if(o1 instanceof MessageReceivedEvent)
            {
                MessageReceivedEvent evt = (MessageReceivedEvent)o1;
        
                this.lastHistoryMsgTimestamp = evt.getTimestamp();
            }
        }
    }

    /**
     * Returns the date of the first message in history for this chat.
     * 
     * @return the date of the first message in history for this chat.
     */
    public Date getFirstHistoryMsgTimestamp()
    {
        return firstHistoryMsgTimestamp;
    }

    /**
     * Returns the date of the last message in history for this chat.
     * 
     * @return the date of the last message in history for this chat.
     */
    public Date getLastHistoryMsgTimestamp()
    {
        return lastHistoryMsgTimestamp;
    }
    
    /**
     * Returns the name of this chat.
     * 
     * @return the name of this chat
     */
    public String getChatName()
    {
        String displayName = metaContact.getDisplayName();

        if (displayName != null && displayName.length() > 0)
            return metaContact.getDisplayName();

        return Messages.getI18NString("unknown").getText();
    }
    
    /**
     * Returns the identifier of this chat.
     * 
     * @return the identifier of this chat
     */
    public Object getChatIdentifier()
    {
        return metaContact;
    }

    /**
     * Implements the <tt>ChatPanel.getChatStatusIcon</tt> method.
     *
     * @return the status icon corresponding to this chat room
     */
    public ImageIcon getChatStatusIcon()
    {
        PresenceStatus status
            = this.metaContact.getDefaultContact().getPresenceStatus();

        return new ImageIcon(Constants.getStatusIcon(status));
    }

    /**
     * Updates the status of the given contact in this chat panel.
     * 
     * @param contact the contact, which changed the status
     * @param newStatus the new status of the contact
     */
    public void updateContactStatus(Contact contact, PresenceStatus newStatus)
    {
        // Update the status of the given contact in the "send via" selector
        // box.
        contactSelectorBox.updateContactStatus(contact);
        
        // Update the status of the source meta contact in the contact details
        // panel on the right.
        if(metaContact.getDefaultContact().equals(contact))
        {
            ChatContact chatContact
                = findChatContactByMetaContact(metaContact);

            ChatContactPanel chatContactPanel
                = getChatContactListPanel()
                    .getChatContactPanel(chatContact);

            chatContactPanel.updateStatusIcon();
        }

        // Show a status message to the user.
        String message = getChatConversationPanel().processMessage(
            contact.getAddress(),
            new Date(System.currentTimeMillis()),
            Constants.STATUS_MESSAGE,
            Messages.getI18NString("statusChangedChatMessage",
                new String[]{newStatus.getStatusName()}).getText(),
                "text");

        getChatConversationPanel().appendMessageToEnd(message);

        if(ConfigurationManager.isMultiChatWindowEnabled())
        {
            if (getChatWindow().getChatTabCount() > 0) {
                getChatWindow().setTabIcon(this,
                    new ImageIcon(Constants.getStatusIcon(newStatus)));
            }
        }
    }
    
    /**
     * Implements the <tt>ChatPanel.sendMessage</tt> method. Obtains the
     * appropriate operation set and sends the message, contained in the write
     * area, through it.
     */
    protected void sendMessage()
    {
        if (sendSmsCheckBox.isSelected())
        {
            this.sendSmsMessage();

            return;
        }

        this.sendInstantMessage();

        //make sure the focus goes back to the write area
        this.requestFocusInWriteArea();
    }

    /**
     * Implements <tt>MetaContactListListener.metaContactRenamed</tt> method.
     * When a meta contact is renamed, updates all related labels in this
     * chat panel.
     */
    public void metaContactRenamed(MetaContactRenamedEvent evt)
    {
        String newName = evt.getNewDisplayName();
        
        if(evt.getSourceMetaContact().equals(metaContact))
        {
            ChatContact chatContact
                = findChatContactByMetaContact(evt.getSourceMetaContact());
            
            getChatContactListPanel().renameContact(chatContact);

            getChatWindow().setTabTitle(this, newName);

            if( getChatWindow().getCurrentChatPanel() == this)
            {
                getChatWindow().setTitle(newName);
            }
        }
    }
    
    /**
     * Implements <tt>MetaContactListListener.metaContactModified</tt> method.
     * Indicates that a MetaContact has been modified.
     * @param evt the MetaContactModifiedEvent containing the corresponding contact
     */
    public void metaContactModified(MetaContactModifiedEvent evt)
    {}

    /**
     * Implements <tt>MetaContactListListener.protoContactAdded</tt> method.
     * When a proto contact is added, updates the "send via" selector box.
     */
    public void protoContactAdded(ProtoContactEvent evt)
    {   
        if (evt.getNewParent().equals(metaContact))
        {   
            this.contactSelectorBox.addProtoContact(evt.getProtoContact());
        }
    }

    /**
     * Implements <tt>MetaContactListListener.protoContactRemoved</tt> method.
     * When a proto contact is removed, updates the "send via" selector box.
     */
    public void protoContactRemoved(ProtoContactEvent evt)
    {
        if (evt.getOldParent().equals(metaContact))
        {
            contactSelectorBox.removeProtoContact(evt.getProtoContact());
        }
    }

    /**
     * Implements <tt>MetaContactListListener.protoContactMoved</tt> method.
     * When a proto contact is moved, updates the "send via" selector box.
     */
    public void protoContactMoved(ProtoContactEvent evt)
    {
        if (evt.getOldParent().equals(metaContact))
        {
            contactSelectorBox.removeProtoContact(evt.getProtoContact());
        }
        
        if (evt.getNewParent().equals(metaContact))
        {
            contactSelectorBox.addProtoContact(evt.getProtoContact());
        }
    }

    public void metaContactAdded(MetaContactEvent evt)
    {}

    public void metaContactRemoved(MetaContactEvent evt)
    {}

    public void metaContactMoved(MetaContactMovedEvent evt)
    {}

    public void metaContactGroupAdded(MetaContactGroupEvent evt)
    {}

    public void metaContactGroupModified(MetaContactGroupEvent evt)
    {}

    public void metaContactGroupRemoved(MetaContactGroupEvent evt)
    {}

    public void childContactsReordered(MetaContactGroupEvent evt)
    {}

    /**
     * Implements the <tt>ChatPanel.sendTypingNotification</tt> method.
     * Obtains the typing notification's operation set and sends a typing
     * notification through it.
     */
    public int sendTypingNotification(int typingState)
    {
        Contact protocolContact
            = contactSelectorBox.getSelectedProtocolContact();
        
        OperationSetTypingNotifications tnOperationSet
            = (OperationSetTypingNotifications)
               protocolContact.getProtocolProvider()
                    .getOperationSet(OperationSetTypingNotifications.class);
        
        if(protocolContact.getProtocolProvider().isRegistered()
            && tnOperationSet != null)
        {
            try
            {
                tnOperationSet.sendTypingNotification(
                    protocolContact, typingState);
                
                return ChatPanel.TYPING_NOTIFICATION_SUCCESSFULLY_SENT;
            }
            catch (Exception ex)
            {
                logger.error("Failed to send typing notifications.", ex);
                
                return ChatPanel.TYPING_NOTIFICATION_SEND_FAILED;
            }
        }
        
        return ChatPanel.TYPING_NOTIFICATION_SEND_FAILED;
    }

    /**
     * Implements the <tt>ChatPanel.treatReceivedMessage</tt> method.
     * Selects the given contact in the "send via" selector box.
     */
    public void treatReceivedMessage(Contact sourceContact)
    {
        if (!contactSelectorBox.getSelectedProtocolContact().getProtocolProvider()
            .equals(sourceContact.getProtocolProvider()))
        {
            contactSelectorBox.setSelected(sourceContact);
        }
    }
    
    /**
     * The <tt>ChangeProtocolAction</tt> is an <tt>AbstractAction</tt> that
     * opens the menu, containing all available protocol contacts.
     */
    private class ChangeProtocolAction
        extends AbstractAction
    {
        public void actionPerformed(ActionEvent e)
        {
            /*
             * Opens the selector box containing the protocol contact icons.
             * This is the menu, where user could select the protocol specific
             * contact to communicate through.
             */
            SIPCommMenu contactSelector = contactSelectorBox.getMenu();
            contactSelector.doClick();
        }
    }

    /**
     * Implements <tt>ChatPanel.loadPreviousFromHistory</tt>.
     * Loads previous page from history.
     */
    public void loadPreviousPageFromHistory()
    {
        final MessageHistoryService msgHistory
            = GuiActivator.getMsgHistoryService();

        // If the MessageHistoryService is not registered we have nothing to do
        // here. The MessageHistoryService could be "disabled" from the user
        // through one of the configuration forms.
        if (msgHistory == null)
            return;

        new Thread() {
            public void run()
            {
                ChatConversationPanel conversationPanel
                    = getChatConversationPanel();
                
                Date firstMsgDate
                    = conversationPanel.getPageFirstMsgTimestamp();
                
                Collection c = null;
                
                if(firstMsgDate != null)
                {
                    c = msgHistory.findLastMessagesBefore(
                        metaContact,
                        firstMsgDate,
                        MESSAGES_PER_PAGE);
                }
                 
                if(c !=null && c.size() > 0)
                {   
                    SwingUtilities.invokeLater(
                            new HistoryMessagesLoader(c));
                }
            }   
        }.start();
    }

    /**
     * Implements <tt>ChatPanel.loadNextFromHistory</tt>.
     * Loads next page from history.
     */
    public void loadNextPageFromHistory()
    {
        final MessageHistoryService msgHistory
            = GuiActivator.getMsgHistoryService();

        // If the MessageHistoryService is not registered we have nothing to do
        // here. The MessageHistoryService could be "disabled" from the user
        // through one of the configuration forms.
        if (msgHistory == null)
            return;

        new Thread()
        {
            public void run()
            {
                Date lastMsgDate
                    = getChatConversationPanel().getPageLastMsgTimestamp();

                Collection c = null;
                if(lastMsgDate != null)
                {  
                    c = msgHistory.findFirstMessagesAfter(
                        metaContact,
                        lastMsgDate,
                        MESSAGES_PER_PAGE);
                }
                
                if(c != null && c.size() > 0)
                    SwingUtilities.invokeLater(
                            new HistoryMessagesLoader(c));
            }   
        }.start();
    }
    
    /**
     * From a given collection of messages shows the history in the chat window.
     */
    private class HistoryMessagesLoader implements Runnable
    {        
        private Collection msgHistory;
        
        public HistoryMessagesLoader(Collection msgHistory)
        {
            this.msgHistory = msgHistory;
        }
        
        public void run()
        {
            getChatConversationPanel().clear();
                        
            processHistory(msgHistory, "");
            
            getChatConversationPanel().setDefaultContent();
        }
    }

    /**
     * Returns the <tt>MetaContact</tt> corresponding to the chat.
     * 
     * @return the <tt>MetaContact</tt> corresponding to the chat.
     */
    public MetaContact getMetaContact()
    {
        return metaContact;
    }
    
    public void subscriptionCreated(SubscriptionEvent evt)
    {}

    public void subscriptionFailed(SubscriptionEvent evt)
    {}

    public void subscriptionRemoved(SubscriptionEvent evt)
    {}

    public void subscriptionMoved(SubscriptionMovedEvent evt)
    {}

    public void subscriptionResolved(SubscriptionEvent evt)
    {}

    /**
     * Change the contact avatar image when contact details were updated.
     */
    public void contactModified(ContactPropertyChangeEvent evt)
    {
        Contact sourceContact = evt.getSourceContact();

        ChatContact chatContact = findChatContactByContact(sourceContact);

        if(chatContact != null)
        {
            ChatContactPanel chatContactPanel
                = getChatContactListPanel().getChatContactPanel(chatContact);

            chatContactPanel.setContactPhoto(chatContact.getImage());
        }
    }
    
    /**
     * Returns the <tt>ChatContact</tt> corresponding to the given
     * <tt>MetaContact</tt>.
     * 
     * @param metaContact the <tt>MetaContact</tt> to search for
     * @return the <tt>ChatContact</tt> corresponding to the given
     * <tt>MetaContact</tt>.
     */
    private ChatContact findChatContactByMetaContact(MetaContact metaContact)
    {
        Iterator chatContacts
            = getChatContactListPanel().getChatContacts();
            
        while(chatContacts.hasNext())
        {
            ChatContact chatContact
                = (ChatContact) chatContacts.next();
            
            Object chatSourceContact = chatContact.getSourceContact();
            
            if(chatSourceContact instanceof Contact)
            {
                MetaContact parentMetaContact
                    = GuiActivator.getMetaContactListService()
                        .findMetaContactByContact((Contact)chatSourceContact);
                
                if(parentMetaContact != null
                        && parentMetaContact.equals(metaContact))
                    return chatContact;
            }
        }
        
        return null;
    }
    
    /**
     * Returns the <tt>ChatContact</tt> corresponding to the given
     * <tt>Contact</tt>.
     * 
     * @param metaContact the <tt>MetaContact</tt> to search for
     * @return the <tt>ChatContact</tt> corresponding to the given
     * <tt>Contact</tt>.
     */
    private ChatContact findChatContactByContact(Contact contact)
    {
        Iterator chatContacts
            = getChatContactListPanel().getChatContacts();
            
        while(chatContacts.hasNext())
        {
            ChatContact chatContact
                = (ChatContact) chatContacts.next();
            
            Object chatSourceContact = chatContact.getSourceContact();
            
            if(chatSourceContact instanceof Contact
                    && chatSourceContact.equals(contact))
            {
                return chatContact;
            }
        }
        
        return null;
    }

    public void inviteChatContact(String contactAddress, String reason)
    {
    }

    private void sendSmsMessage()
    {
        final String text = this.getTextFromWriteArea(
            OperationSetBasicInstantMessaging.DEFAULT_MIME_TYPE);

        this.refreshWriteArea();

        OperationSetSmsMessaging smsOpSet = null;
        /*
        Contact contact = null;
        Iterator contacts = metaContact.getContacts();

        while (contacts.hasNext())
        {
            contact = (Contact) contacts.next();

            smsOpSet
                = (OperationSetSmsMessaging) contact.getProtocolProvider()
                    .getOperationSet(OperationSetSmsMessaging.class);

            if (smsOpSet != null)
                break;
        }

        if (smsOpSet != null)
        {
            Message message  = smsOpSet.createMessage(text);

            try
            {
                smsOpSet.sendSmsMessage(contact, message);
            }
            catch (IllegalStateException ex)
            {
                logger.error("Failed to send SMS.", ex);

                this.refreshWriteArea();

                this.processMessage(
                        contact.getDisplayName(),
                        new Date(System.currentTimeMillis()),
                        Constants.OUTGOING_MESSAGE,
                        message.getContent(),
                        message.getContentType());

                this.processMessage(
                        contact.getDisplayName(),
                        new Date(System.currentTimeMillis()),
                        Constants.ERROR_MESSAGE,
                        Messages.getI18NString("msgSendConnectionProblem")
                            .getText(), "text");
            }
            catch (Exception ex)
            {
                logger.error("Failed to send SMS.", ex);

                this.refreshWriteArea();

                this.processMessage(
                        contact.getDisplayName(),
                        new Date(System.currentTimeMillis()),
                        Constants.OUTGOING_MESSAGE,
                        message.getContent(), message.getContentType());

                this.processMessage(
                        contact.getDisplayName(),
                        new Date(System.currentTimeMillis()),
                        Constants.ERROR_MESSAGE,
                        Messages.getI18NString("msgDeliveryInternalError")
                            .getText(), "text");
            }
            
            return;
        }
    */
        // If we didn't find a Sms operation set in the current meta contact.
        Iterator protocolProviders
            = chatWindow.getMainFrame().getProtocolProviders();

        while (protocolProviders.hasNext())
        {
            ProtocolProviderService protocolProvider
                = (ProtocolProviderService) protocolProviders.next();

            smsOpSet = (OperationSetSmsMessaging) protocolProvider
                .getOperationSet(OperationSetSmsMessaging.class);

            // When we find the first SMS operation set we stop searching.
            if (smsOpSet != null)
                break;
        }

        // If there's no operation set we show some "not supported" messages
        // and we return.
        if (smsOpSet == null)
        {
            logger.error("Failed to send SMS.");

            this.refreshWriteArea();

            this.processMessage(
                    metaContact.getDisplayName(),
                    new Date(System.currentTimeMillis()),
                    Constants.OUTGOING_MESSAGE,
                    text, "plain/text");

            this.processMessage(
                    metaContact.getDisplayName(),
                    new Date(System.currentTimeMillis()),
                    Constants.ERROR_MESSAGE,
                    Messages.getI18NString("sendSmsNotSupported")
                        .getText(), "plain/text");

            return;
        }

        smsOpSet.addMessageListener(new SmsMessageListener(smsOpSet));

        // Otherwise we create the message.
        sentSmsMessage  = smsOpSet.createMessage(text);

        // We open the send SMS dialog.
        SendSmsDialog smsDialog = new SendSmsDialog(this, sentSmsMessage, smsOpSet);

        smsDialog.setPreferredSize(new Dimension(400, 200));
        smsDialog.setVisible(true);
    }

    private void sendInstantMessage()
    {
        Contact contact = (Contact) contactSelectorBox.getMenu()
            .getSelectedObject();

        OperationSetBasicInstantMessaging im
            = (OperationSetBasicInstantMessaging) contact.getProtocolProvider()
                .getOperationSet(OperationSetBasicInstantMessaging.class);

        OperationSetTypingNotifications tn
            = (OperationSetTypingNotifications) contact.getProtocolProvider()
                .getOperationSet(OperationSetTypingNotifications.class);

        String htmlText = getTextFromWriteArea("text/html");
        String plainText = getTextFromWriteArea(
            OperationSetBasicInstantMessaging.DEFAULT_MIME_TYPE);

        Message msg;
        if (im.isContentTypeSupported("text/html")
            && (htmlText.indexOf("<b") > -1
                || htmlText.indexOf("<i") > -1
                || htmlText.indexOf("<u") > -1
                || htmlText.indexOf("<font") > -1))
        {
            msg = im.createMessage(htmlText, "text/html", "utf-8", "");
        }
        else
        {
            msg = im.createMessage(plainText);
        }

        this.refreshWriteArea();

        if (tn != null)
        {
            // Send TYPING STOPPED event before sending the message
            getChatWritePanel().stopTypingTimer();
        }

        try
        {
            im.sendInstantMessage(contact, msg);
        }
        catch (IllegalStateException ex)
        {
            logger.error("Failed to send message.", ex);

            this.refreshWriteArea();

            this.processMessage(
                    contact.getDisplayName(),
                    new Date(System.currentTimeMillis()),
                    Constants.OUTGOING_MESSAGE,
                    msg.getContent(),
                    msg.getContentType());

            this.processMessage(
                    contact.getDisplayName(),
                    new Date(System.currentTimeMillis()),
                    Constants.ERROR_MESSAGE,
                    Messages.getI18NString("msgSendConnectionProblem")
                        .getText(), "text");
        }
        catch (Exception ex)
        {
            logger.error("Failed to send message.", ex);

            this.refreshWriteArea();

            this.processMessage(
                    contact.getDisplayName(),
                    new Date(System.currentTimeMillis()),
                    Constants.OUTGOING_MESSAGE,
                    msg.getContent(), msg.getContentType());

            this.processMessage(
                    contact.getDisplayName(),
                    new Date(System.currentTimeMillis()),
                    Constants.ERROR_MESSAGE,
                    Messages.getI18NString("msgDeliveryUnknownError",
                        new String[]{ex.getMessage()})
                    .getText(), "text");
        }
    }

    /**
     * Changes the "Send as SMS" check box state.
     * 
     * @param isSmsSelected <code>true</code> to set the "Send as SMS" check box
     * selected, <code>false</code> - otherwise. 
     */
    public void setSmsSelected(boolean isSmsSelected)
    {
        sendSmsCheckBox.setSelected(isSmsSelected);
    }
    
    private class SmsMessageListener implements MessageListener
    {
        private OperationSetSmsMessaging smsOpSet;

        public SmsMessageListener(OperationSetSmsMessaging smsOpSet)
        {
            this.smsOpSet = smsOpSet;
        }

        public void messageDelivered(MessageDeliveredEvent evt)
        {
            Message msg = evt.getSourceMessage();
            
            // if there is no sms sent or this event is not for our sms ignore it
            if(sentSmsMessage == null || !sentSmsMessage.equals(msg))
                return;
            
            sentSmsMessage = null;
            
            Contact contact = evt.getDestinationContact();

            processMessage(
                contact.getDisplayName(),
                new Date(System.currentTimeMillis()),
                Constants.OUTGOING_MESSAGE,
                msg.getContent(), msg.getContentType());

            processMessage(
                    contact.getDisplayName(),
                    new Date(System.currentTimeMillis()),
                    Constants.ACTION_MESSAGE,
                    Messages.getI18NString("smsSuccessfullySent")
                    .getText(), "text");
        }

        public void messageDeliveryFailed(MessageDeliveryFailedEvent evt)
        {
            logger.error(evt.getReason());
            
            String errorMsg = null;

            Message sourceMessage = (Message) evt.getSource();
            
            // if there is no sms sent or this event is not for our sms ignore it
            if(sentSmsMessage == null || !sentSmsMessage.equals(sourceMessage))
                return;
            
            sentSmsMessage = null;

            Contact sourceContact = evt.getDestinationContact();

            MetaContact metaContact = chatWindow.getMainFrame().getContactList()
                .findMetaContactByContact(sourceContact);

            if (evt.getErrorCode() 
                    == MessageDeliveryFailedEvent.OFFLINE_MESSAGES_NOT_SUPPORTED)
            {
                errorMsg = Messages.getI18NString(
                        "msgDeliveryOfflineNotSupported").getText();
            }
            else if (evt.getErrorCode()
                    == MessageDeliveryFailedEvent.NETWORK_FAILURE)
            {
                errorMsg = Messages.getI18NString("msgNotDelivered").getText();
            }
            else if (evt.getErrorCode()
                    == MessageDeliveryFailedEvent.PROVIDER_NOT_REGISTERED)
            {
                errorMsg = Messages.getI18NString(
                        "msgSendConnectionProblem").getText();
            }
            else if (evt.getErrorCode()
                    == MessageDeliveryFailedEvent.INTERNAL_ERROR)
            {
                errorMsg = Messages.getI18NString(
                        "msgDeliveryInternalError").getText();
            }
            else {
                errorMsg = Messages.getI18NString(
                        "msgDeliveryFailedUnknownError").getText();
            }

            processMessage(
                    metaContact.getDisplayName(),
                    new Date(System.currentTimeMillis()),
                    Constants.OUTGOING_MESSAGE,
                    sourceMessage.getContent(),
                    sourceMessage.getContentType());

            processMessage(
                    metaContact.getDisplayName(),
                    new Date(System.currentTimeMillis()),
                    Constants.ERROR_MESSAGE,
                    errorMsg,
                    "text");
        }

        public void messageReceived(MessageReceivedEvent evt)
        {}
    }
    
    /**
     * Returns the selected protocol contact.
     * 
     * @return the selected protocol contact
     */
    public Contact getSelectedProtocolContact()
    {
        return contactSelectorBox.getSelectedProtocolContact();
    }
    
    public ProtocolContactSelectorBox getProtocolContactSelectorBox()
    {
        return contactSelectorBox;
    }
}
