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
 */
public class MetaContactChatPanel
    extends ChatPanel
    implements  ContactPresenceStatusListener,
                MetaContactListListener,
                SubscriptionListener
{

    private static final Logger logger = Logger
        .getLogger(MetaContactChatPanel.class.getName());
    
    private MetaContact metaContact;

    private Date firstHistoryMsgTimestamp;

    private Date lastHistoryMsgTimestamp;

    MessageHistoryService msgHistory
        = GuiActivator.getMsgHistoryService();

    private JLabel sendViaLabel = new JLabel(
        Messages.getI18NString("sendVia").getText());

    private ProtocolContactSelectorBox contactSelectorBox;
    
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
        
        //Load the history period, to initialize the firstMessageTimestamp and
        //the lastMessageTimeStamp variables. Used to disable/enable history
        //flash buttons in the chat window tool bar.
        new Thread(){
            public void run(){
                loadHistoryPeriod();
            }
        }.start();
                
        //For each subcontact in the given MetaContact adds a
        //ContactPresenceStatusListener in order to have always the contact
        //current status in the chat panel
        Iterator protocolContacts = metaContact.getContacts();
        
        while(protocolContacts.hasNext())
        {
            Contact subContact = (Contact) protocolContacts.next();
            
            Object opSet = subContact.getProtocolProvider()
                .getOperationSet(OperationSetPersistentPresence.class);
            
            if(opSet != null)
            {
                ((OperationSetPersistentPresence)opSet)
                    .addContactPresenceStatusListener(this);
                
                ((OperationSetPersistentPresence)opSet)
                    .addSubsciptionListener(this);
            }
        }
        
        //Obtains the MetaContactListService and adds itself to it as a
        //listener of all events concerning the contact list.
        chatWindow.getMainFrame().getContactList()
            .addMetaContactListListener(this);
        
        //Initializes the "send via" selector box and adds it to the send panel
        contactSelectorBox = new ProtocolContactSelectorBox(
            this, metaContact, protocolContact);

        getChatSendPanel().getSendPanel()
            .add(contactSelectorBox, BorderLayout.CENTER);
        getChatSendPanel().getSendPanel()
            .add(sendViaLabel, BorderLayout.WEST);
     
        //Enables to change the protocol provider by simply pressing the Ctrl-P
        //key combination
        ActionMap amap = this.getActionMap();
        
        amap.put("ChangeProtocol", new ChangeProtocolAction());
        
        InputMap imap = this.getInputMap(
            JComponent.WHEN_IN_FOCUSED_WINDOW); 
        
        imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_P,
            KeyEvent.CTRL_DOWN_MASK), "ChangeProtocol");
    }

    /**
     * Loads history for the chat meta contact in a separate thread. Implements
     * the <tt>ChatPanel.loadHistory</tt> method.
     */
    public void loadHistory()
    {
        new Thread() {
            public void run() {
                Collection historyList = msgHistory.findLast(
                        metaContact, Constants.CHAT_HISTORY_SIZE);

                if(historyList.size() > 0) {
                    class ProcessHistory implements Runnable {
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
    public void loadHistory(String escapedMessageID)
    {
        Collection historyList = msgHistory.findLast(
                metaContact, Constants.CHAT_HISTORY_SIZE);

        processHistory(historyList, escapedMessageID);
    }
    
    /**
     * Loads history period dates for the current chat.
     */
    private void loadHistoryPeriod()
    {
        MessageHistoryService msgHistory
            = GuiActivator.getMsgHistoryService();

        Collection firstMessage = msgHistory
            .findFirstMessagesAfter(metaContact, new Date(0), 1);

        if(firstMessage.size() > 0) {

            Iterator i = firstMessage.iterator();

            Object o = i.next();

            if(o instanceof MessageDeliveredEvent) {
                MessageDeliveredEvent evt
                    = (MessageDeliveredEvent)o;

                this.firstHistoryMsgTimestamp = evt.getTimestamp();
            }
            else if(o instanceof MessageReceivedEvent) {
                MessageReceivedEvent evt = (MessageReceivedEvent)o;

                this.firstHistoryMsgTimestamp = evt.getTimestamp();
            }

            Collection lastMessage = msgHistory
                .findLastMessagesBefore(metaContact, new Date(Long.MAX_VALUE), 1);

            Iterator i1 = lastMessage.iterator();

            Object o1 = i1.next();

            if(o1 instanceof MessageDeliveredEvent) {
                MessageDeliveredEvent evt
                    = (MessageDeliveredEvent)o1;

                this.lastHistoryMsgTimestamp = evt.getTimestamp();
            }
            else if(o1 instanceof MessageReceivedEvent) {
                MessageReceivedEvent evt = (MessageReceivedEvent)o1;

                this.lastHistoryMsgTimestamp = evt.getTimestamp();
            }
        }

        getChatWindow().getMainToolBar().changeHistoryButtonsState(this);
    }

    /**
     * Returns the datetime of the first message in history fot this chat.
     */
    public Date getFirstHistoryMsgTimestamp()
    {
        return firstHistoryMsgTimestamp;
    }

    /**
     * Returns the datetime of the last message in history fot this chat.
     */
    public Date getLastHistoryMsgTimestamp()
    {
        return lastHistoryMsgTimestamp;
    }
    
    /**
     * Returns the name of this chat.
     */
    public String getChatName()
    {
        return metaContact.getDisplayName();
    }
    
    /**
     * Returns the identifier of this chat.
     */
    public Object getChatIdentifier()
    {
        return metaContact;
    }
    
    /**
     * Returns the <tt>PresenceStatus</tt> of the default contact for this chat
     * panel. Implements the <tt>ChatPanel.getChatStatus()</tt>.
     * 
     * @return the <tt>PresenceStatus</tt> of the default contact for this chat
     * panel.
     */
    public PresenceStatus getChatStatus()
    {
        return this.metaContact.getDefaultContact().getPresenceStatus();
    }

    /**
     * Implements the
     * <tt>ContactPresenceStatusListener.contactPresenceStatusChanged</tt>.
     * Updates all status related icons in this chat panel.
     */
    public void contactPresenceStatusChanged(
        ContactPresenceStatusChangeEvent evt)
    {   
        Contact sourceContact = evt.getSourceContact();
        
        MetaContact sourceMetaContact = GuiActivator.getMetaContactListService()
            .findMetaContactByContact(sourceContact);
    
        if (sourceMetaContact != null && metaContact.equals(sourceMetaContact))
        {   
            // Update the status of the given contact in the "send via" selector
            // box.
            contactSelectorBox.updateContactStatus(sourceContact);
            
            // Update the status of the source meta contact in the contact details
            // panel on the right.
            
            if(sourceMetaContact != null
                    && sourceMetaContact.getDefaultContact().equals(sourceContact))
            {
                ChatContact chatContact
                    = findChatContactByMetaContact(sourceMetaContact);
            
                ChatContactPanel chatContactPanel
                    = getChatContactListPanel()
                        .getChatContactPanel(chatContact);
                
                chatContactPanel.setStatusIcon(
                    chatContact.getPresenceStatus());
            }
            
            PresenceStatus status = contactSelectorBox
                .getSelectedProtocolContact().getPresenceStatus();

            // Show a status message to the user.
            String message = getChatConversationPanel().processMessage(
                sourceContact.getAddress(),
                new Date(System.currentTimeMillis()),
                Constants.SYSTEM_MESSAGE,
                Messages.getI18NString("statusChangedChatMessage",
                        new String[]{status.getStatusName()}).getText());

            getChatConversationPanel().appendMessageToEnd(message);
            
            if(Constants.TABBED_CHAT_WINDOW)
            {                
                if (getChatWindow().getChatTabCount() > 0) {
                    getChatWindow().setTabIcon(this,
                        new ImageIcon(Constants.getStatusIcon(status)));
                }
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
        Contact contact = (Contact) contactSelectorBox.getMenu()
            .getSelectedObject();
     
        OperationSetBasicInstantMessaging im
            = (OperationSetBasicInstantMessaging) contact.getProtocolProvider()
                .getOperationSet(OperationSetBasicInstantMessaging.class);
    
        OperationSetTypingNotifications tn
        = (OperationSetTypingNotifications) contact.getProtocolProvider()
            .getOperationSet(OperationSetTypingNotifications.class);
    
        String body = this.getTextFromWriteArea();
        Message msg = im.createMessage(body);
    
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
                    msg.getContent());
    
            this.processMessage(
                    contact.getDisplayName(),
                    new Date(System.currentTimeMillis()),
                    Constants.ERROR_MESSAGE,
                    Messages.getI18NString("msgSendConnectionProblem")
                        .getText());
        }
        catch (Exception ex)
        {
            logger.error("Failed to send message.", ex);
            
            this.refreshWriteArea();
    
            this.processMessage(
                    contact.getDisplayName(),
                    new Date(System.currentTimeMillis()),
                    Constants.OUTGOING_MESSAGE,
                    msg.getContent());
    
            this.processMessage(
                    contact.getDisplayName(),
                    new Date(System.currentTimeMillis()),
                    Constants.ERROR_MESSAGE,
                    Messages.getI18NString("msgDeliveryInternalError")
                        .getText());
        }
    }

    public void metaContactAdded(MetaContactEvent evt)
    {}

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
        Contact protocolContact = contactSelectorBox.getSelectedProtocolContact();
        
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
    public void loadPreviousFromHistory()
    {
        new Thread() {
            public void run()
            {
                MessageHistoryService msgHistory
                    = GuiActivator.getMsgHistoryService();
                
                ChatConversationPanel conversationPanel
                    = getChatConversationPanel();
                
                Collection c = msgHistory.findLastMessagesBefore(
                        metaContact,
                        conversationPanel.getPageFirstMsgTimestamp(),
                        MESSAGES_PER_PAGE);
                
                if(c.size() > 0)
                {   
                    SwingUtilities.invokeLater(
                            new HistoryMessagesLoader(c));
                                            
                    //Save the last before the last page
                    Iterator i = c.iterator();
                    Object lastMessageObject = null;
                    Date lastMessageTimeStamp = null;
                    
                    while(i.hasNext()) {
                        Object o = i.next();
                        
                        if(!i.hasNext()) {
                            lastMessageObject = o;
                        }
                    }                        
                    
                    if(lastMessageObject instanceof MessageDeliveredEvent) {                            
                        MessageDeliveredEvent evt
                            = (MessageDeliveredEvent)lastMessageObject;
                        
                        lastMessageTimeStamp = evt.getTimestamp();
                    }
                    else if(lastMessageObject instanceof MessageReceivedEvent) {
                        MessageReceivedEvent evt
                            = (MessageReceivedEvent) lastMessageObject;
                        
                        lastMessageTimeStamp = evt.getTimestamp();
                    }
                    
                    if(getBeginLastPageTimeStamp() == null)
                        setBeginLastPageTimeStamp(lastMessageTimeStamp);                        
                }
            }   
        }.start();
    }

    /**
     * Implements <tt>ChatPanel.loadNextFromHistory</tt>.
     * Loads next page from history.
     */
    public void loadNextFromHistory()
    {
        new Thread() {
            public void run(){
                MessageHistoryService msgHistory
                    = GuiActivator.getMsgHistoryService();
                                    
                Collection c;
                if(getBeginLastPageTimeStamp().compareTo(
                    getChatConversationPanel().getPageLastMsgTimestamp()) == 0)
                {   
                    c = msgHistory.findByPeriod(
                            metaContact,
                            getBeginLastPageTimeStamp(),
                            new Date(System.currentTimeMillis()));
                }
                else
                {
                    c = msgHistory.findFirstMessagesAfter(
                        metaContact,
                        getChatConversationPanel().getPageLastMsgTimestamp(),
                        MESSAGES_PER_PAGE);
                }
                
                if(c.size() > 0)
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
        Enumeration chatContacts
            = getChatContactListPanel().getChatContacts();
            
        while(chatContacts.hasMoreElements())
        {
            ChatContact chatContact
                = (ChatContact) chatContacts.nextElement();
            
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
        Enumeration chatContacts
            = getChatContactListPanel().getChatContacts();
            
        while(chatContacts.hasMoreElements())
        {
            ChatContact chatContact
                = (ChatContact) chatContacts.nextElement();
            
            Object chatSourceContact = chatContact.getSourceContact();
            
            if(chatSourceContact instanceof Contact
                    && chatSourceContact.equals(contact))
            {
                return chatContact;
            }
        }
        
        return null;
    }
}
