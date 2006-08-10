/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.message;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.html.HTMLDocument;

import net.java.sip.communicator.impl.gui.GuiActivator;
import net.java.sip.communicator.impl.gui.customcontrols.SIPCommSelectorBox;
import net.java.sip.communicator.impl.gui.i18n.Messages;
import net.java.sip.communicator.impl.gui.utils.Constants;
import net.java.sip.communicator.service.contactlist.MetaContact;
import net.java.sip.communicator.service.gui.ExportedDialog;
import net.java.sip.communicator.service.msghistory.MessageHistoryService;
import net.java.sip.communicator.service.protocol.Contact;
import net.java.sip.communicator.service.protocol.OperationSetBasicInstantMessaging;
import net.java.sip.communicator.service.protocol.OperationSetTypingNotifications;
import net.java.sip.communicator.service.protocol.PresenceStatus;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;
import net.java.sip.communicator.service.protocol.event.MessageDeliveredEvent;
import net.java.sip.communicator.service.protocol.event.MessageReceivedEvent;
import net.java.sip.communicator.util.Logger;

/**
 * The <tt>ChatPanel</tt> is the panel, where users can write and send messages,
 * view received messages. A ChatPanel is created for a contact or for a group
 * of contacts in case of a chat conference. There is always one default contact
 * for the chat, which is the first contact which was added to the chat.
 * When chat is in mode "open all messages in new window", each ChatPanel
 * corresponds to a ChatWindow. When chat is in mode "group all messages in
 * one chat window", each ChatPanel corresponds to a tab in the ChatWindow.
 * 
 * @author Yana Stamcheva
 */
public class ChatPanel extends JPanel
    implements ExportedDialog, ChatConversationContainer {

    private static final Logger logger = Logger
        .getLogger(ChatPanel.class.getName());
    
    private JSplitPane topSplitPane = new JSplitPane(
            JSplitPane.HORIZONTAL_SPLIT);

    private JSplitPane messagePane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

    private ChatConversationPanel conversationPanel;

    private ChatWritePanel writeMessagePanel;

    private ChatConferencePanel chatConferencePanel = new ChatConferencePanel();

    private ChatSendPanel sendPanel;

    private ChatWindow chatWindow;

    private OperationSetBasicInstantMessaging imOperationSet;

    private OperationSetTypingNotifications tnOperationSet;

    private Contact protocolContact;
    
    private MetaContact metaContact;
    
    private boolean isVisible = false;
    
    MessageHistoryService msgHistory
        = GuiActivator.getMsgHistoryService();
    
    /**
     * Creates a <tt>ChatPanel</tt> which is added to the given chat window.
     * 
     * @param chatWindow The parent window of this chat panel.
     * @param protocolContact The subContact which is selected ins
     * the chat.
     */
    public ChatPanel(   ChatWindow chatWindow,
                        MetaContact metaContact,
                        Contact protocolContact) {

        super(new BorderLayout());

        this.chatWindow = chatWindow;
        this.metaContact = metaContact;
        this.protocolContact = protocolContact;
        
        this.imOperationSet = this.chatWindow.getMainFrame().getProtocolIM(
                protocolContact.getProtocolProvider());
        this.tnOperationSet = this.chatWindow.getMainFrame()
                .getTypingNotifications(protocolContact.getProtocolProvider());
        
        this.conversationPanel = new ChatConversationPanel(this);

        this.sendPanel = new ChatSendPanel(this);

        this.writeMessagePanel = new ChatWritePanel(this);

        this.topSplitPane.setResizeWeight(1.0D);
        this.messagePane.setResizeWeight(1.0D);
        this.chatConferencePanel.setPreferredSize(new Dimension(120, 100));
        this.chatConferencePanel.setMinimumSize(new Dimension(120, 100));
        this.writeMessagePanel.setPreferredSize(new Dimension(400, 100));
        this.writeMessagePanel.setMinimumSize(new Dimension(400, 100));

        this.init();

        this.setChatMetaContact(metaContact, protocolContact.getPresenceStatus());
        
        addComponentListener(new TabSelectionFocusGainListener());
    }

    /**
     * Initializes this panel.
     */
    private void init() {
        this.topSplitPane.setOneTouchExpandable(true);

        topSplitPane.setLeftComponent(conversationPanel);
        topSplitPane.setRightComponent(chatConferencePanel);

        this.messagePane.setTopComponent(topSplitPane);
        this.messagePane.setBottomComponent(writeMessagePanel);

        this.add(messagePane, BorderLayout.CENTER);
        this.add(sendPanel, BorderLayout.SOUTH);
    }

    /**
     * 
     */
    public void loadHistory() {
        this.loadHistory(new Date(0));
    }
    
    /**
     * 
     * @param lastMsgTimestamp
     */
    public void loadHistory(Date lastMsgTimestamp) {        
        Collection historyList = msgHistory.findLast(
                this.metaContact, Constants.CHAT_HISTORY_SIZE);
    
        if(historyList.size() > 0) {
            
            Iterator i = historyList.iterator();
            
            while (i.hasNext()) {
                
                Object o = i.next();
                
                if(o instanceof MessageDeliveredEvent) {
                    
                    MessageDeliveredEvent evt = (MessageDeliveredEvent)o;
                    
                    ProtocolProviderService protocolProvider = evt
                        .getDestinationContact().getProtocolProvider();
                    
                    conversationPanel.processMessage(
                            this.chatWindow.getMainFrame()
                                .getAccount(protocolProvider),
                            evt.getTimestamp(), Constants.OUTGOING_MESSAGE,
                            evt.getSourceMessage().getContent());
                }
                else if(o instanceof MessageReceivedEvent) {
                    MessageReceivedEvent evt = (MessageReceivedEvent)o;
                    
                    if(evt.getTimestamp().compareTo(lastMsgTimestamp) < 0) {
                        conversationPanel.processMessage(
                            evt.getSourceContact().getDisplayName(),
                            evt.getTimestamp(), Constants.INCOMING_MESSAGE,
                            evt.getSourceMessage().getContent());
                    }
                }
            }
        }
    }
    
    /**
     * Adds a new <tt>MetaContact</tt> to this chat panel.
     * 
     * @param contactItem The MetaContact to add.
     * @param status The current presence status of the contact.
     */
    private void setChatMetaContact(MetaContact metaContact, 
                                PresenceStatus status) {
        this.metaContact = metaContact;

        this.chatConferencePanel.setChatMetaContact(metaContact, status);

        this.sendPanel.addProtocolContacts(metaContact);

        this.sendPanel.setSelectedProtocolContact(this.protocolContact);
    }

    /**
     * Adds a new <tt>MetaContact</tt> to this chat panel.
     * 
     * @param contactItem The <tt>MetaContact</tt> to add.
     */
    private void setChatMetaContact(MetaContact metaContact) {

        this.metaContact = metaContact;

        this.chatConferencePanel.setChatMetaContact(metaContact);

        this.sendPanel.addProtocolContacts(metaContact);
    }

    /**
     * Updates the contact status in the contact info panel.
     * 
     * @param status The presence status of the contact.
     */
    public void updateContactStatus(PresenceStatus status) {
        this.chatConferencePanel.updateContactStatus(status);
        this.conversationPanel.processMessage(
                this.metaContact.getDisplayName(),
                new Date(System.currentTimeMillis()),
                Constants.SYSTEM_MESSAGE,
                Messages.getString("statusChangedChatMessage",
                        status.getStatusName()));
    }

    /**
     * Returns the default contact for the chat. The case of conference 
     * is not yet implemented and for now it returns the first contact.
     * 
     * @return The default contact for the chat.
     */
    public MetaContact getMetaContact() {
        return this.metaContact;
    }

    /**
     * Returns the chat window, where this chat panel
     * is located.
     * 
     * @return ChatWindow The chat window, where this 
     * chat panel is located.
     */
    public ChatWindow getChatWindow() {
        return chatWindow;
    }

    /**
     * Returns the chat window, where this chat panel
     * is located.
     * 
     * @return ChatWindow The chat window, where this 
     * chat panel is located.
     */
    public Window getWindow() {
        return chatWindow;
    }
    
    /**
     * Returns the instant messaging operation set for 
     * this chat panel.
     * 
     * @return OperationSetBasicInstantMessaging The instant 
     * messaging operation set for this chat panel.
     */
    public OperationSetBasicInstantMessaging getImOperationSet() {
        return imOperationSet;
    }

    /**
     * Sets the instant messaging operation set for 
     * this chat panel.
     * @param imOperationSet The operation set to be set.
     */
    public void setImOperationSet(
            OperationSetBasicInstantMessaging imOperationSet) {
        this.imOperationSet = imOperationSet;
    }

    /**
     * Returns the typing notifications operation set for 
     * this chat panel.
     * 
     * @return OperationSetTypingNotifications The typing
     * notifications operation set for this chat panel.
     */
    public OperationSetTypingNotifications getTnOperationSet() {
        return tnOperationSet;
    }

    /**
     * Sets the typing notifications operation set for 
     * this chat panel.
     * @param tnOperationSet The operation set to be set.
     */
    public void setTnOperationSet(
            OperationSetTypingNotifications tnOperationSet) {
        this.tnOperationSet = tnOperationSet;
    }

    /**
     * Returns the chat send panel.
     * @return ChatSendPanel The chat send panel.
     */
    /*
    public ChatSendPanel getSendPanel() {
        return sendPanel;
    }
    */
    private class TabSelectionFocusGainListener 
        implements ComponentListener {

        public TabSelectionFocusGainListener() {
            super();
        }

        public void componentResized(ComponentEvent e) {
        }

        public void componentMoved(ComponentEvent e) {
        }

        public void componentShown(ComponentEvent e) {
            Component component = e.getComponent();
            Container parent = component.getParent();
            if (parent instanceof JTabbedPane) {
                JTabbedPane tabbedPane = (JTabbedPane) parent;
                if (tabbedPane.getSelectedComponent() == component) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            getChatWindow().setTitle(
                                    getMetaContact().getDisplayName());

                            chatWindow.setCurrentChatPanel(ChatPanel.this);

                            writeMessagePanel.getEditorPane()
                                    .requestFocus();
                        }
                    });
                }
            }
        }
        
        public void componentHidden(ComponentEvent e) {
        }
    }

    /**
     * Returns the protocol contact for this chat.
     * @return The protocol contact for this chat.
     */
    public Contact getProtocolContact() {
        return protocolContact;
    }

    /**
     * Sets the protocol contact for this chat.
     * @param protocolContact The subcontact for the protocol.
     */
    public void setProtocolContact(Contact protocolContact) {
        this.protocolContact = protocolContact;
    }
    
    /**
     * Passes the message to the contained <code>ChatConversationPanel</code>
     * for processing.
     * 
     * @param contactName The name of the contact sending the message.
     * @param date The time at which the message is sent or received.
     * @param messageType The type of the message. One of OUTGOING_MESSAGE 
     * or INCOMING_MESSAGE. 
     * @param message The message text.
     */
    public void processMessage(String contactName, Date date,
            String messageType, String message){
        this.conversationPanel.processMessage(contactName, date, 
                                            messageType, message);
    }
    
    /**
     * Refreshes write area editor pane. Deletes all existing text
     * content.
     */
    public void refreshWriteArea(){
        JEditorPane writeMsgPane = this.writeMessagePanel.getEditorPane();
        
        writeMsgPane.setText("");
    }
    
    /**
     * Requests the focus in the write message area.
     */
    public void requestFocusInWriteArea(){
        JEditorPane writeMsgPane = this.writeMessagePanel.getEditorPane();
        
        writeMsgPane.requestFocus();
    }
    
    /**
     * Sets the message text to the status panel in the bottom of the chat
     * window. Used to show typing notification messages, links' hrefs, etc.
     * @param statusMessage The message text to be displayed. 
     */
    public void setStatusMessage(String statusMessage){
        this.sendPanel.setStatusMessage(statusMessage);
    }
    
    /**
     * Returns the time of the last received message.
     * 
     * @return The time of the last received message.
     */
    public Date getLastIncomingMsgTimestamp() {
        return this.conversationPanel.getLastIncomingMsgTimestamp();
    }
    
    /**
     * Checks if the editor contains text.
     * 
     * @return TRUE if editor contains text, FALSE otherwise.
     */
    public boolean isWriteAreaEmpty(){
        JEditorPane editorPane = this.writeMessagePanel.getEditorPane();
        
        if (editorPane.getText() == null
                || editorPane.getText().equals(""))
            return true;
        else
            return false;
    }
    
    /**
     * Adds text to the write area editor.
     * 
     * @param text The text to add.
     */
    public void addTextInWriteArea(String text){
        JEditorPane editorPane = this.writeMessagePanel.getEditorPane();
        
        editorPane.setText(editorPane.getText() + text);
    }
    
    /**
     * Returns the text contained in the write area editor.
     * @return The text contained in the write area editor.
     */
    public String getTextFromWriteArea(){
        JEditorPane editorPane = this.writeMessagePanel.getEditorPane();
        
        return editorPane.getText();
    }
    
    /**
     * Stops typing notifications sending.
     */
    public void stopTypingNotifications(){
        this.writeMessagePanel.stopTypingTimer();
    }
    
    /**
     * Cuts the write area selected content to the clipboard.
     */
    public void cut(){
        this.writeMessagePanel.getEditorPane().cut();
    }
    
    /**
     * Copies either the selected write area content or the selected
     * conversation panel content to the clipboard.
     */
    public void copy(){
        JEditorPane editorPane = this.conversationPanel.getChatEditorPane();

        if (editorPane.getSelectedText() == null) {
            editorPane = this.writeMessagePanel.getEditorPane();
        }
        editorPane.copy();
    }
    
    /**
     * Copies the selected write panel content to the clipboard.
     */
    public void copyWriteArea(){
        JEditorPane editorPane = this.writeMessagePanel.getEditorPane();

        editorPane.copy();
    }
    
    /**
     * Pastes the content of the clipboard to the write area.
     */
    public void paste(){
        JEditorPane editorPane = this.writeMessagePanel.getEditorPane();

        editorPane.paste();

        editorPane.requestFocus();
    }
    
    /**
     * Sends current write area content.
     */
    public void sendMessage(){
        JButton sendButton = this.sendPanel.getSendButton();

        sendButton.requestFocus();
        sendButton.doClick();
    }
    
    /**
     * Moves the caret to the end of the conversation panel.
     * 
     * Workaround for the following problem:  
     * The scrollbar in the conversation area moves up when the
     * scrollpane is resized. This happens when ChatWindow is in
     * mode "Group messages in one window" and the first chat panel
     * is added to the tabbed pane. Then the scrollpane in the 
     * conversation area is slightly resized and is made smaller,
     * which moves the scrollbar up.
     */
    public void setCaretToEnd(){
        HTMLDocument doc = (HTMLDocument)this.conversationPanel
            .getChatEditorPane().getDocument();
        Element root = doc.getDefaultRootElement();

        try {
            doc.insertAfterEnd(root
                    .getElement(root.getElementCount() - 1), "<br>");
        } catch (BadLocationException e) {
            logger.error("Insert in the HTMLDocument failed.", e);
        } catch (IOException e) {
            logger.error("Insert in the HTMLDocument failed.", e);
        }
        //Scroll to the last inserted text in the document.
        this.conversationPanel.setCarretToEnd();
    }
    
    /**
     * Opens the selector box containing the protocol contact icons. This is the
     * menu, where user could select the protocol specific contact to
     * communicate through.
     */
    public void openProtocolSelectorBox() {
        SIPCommSelectorBox contactSelector 
            = this.sendPanel.getContactSelectorBox();
        JPopupMenu popup 
            = contactSelector.getPopup();
        
        if (!popup.isVisible()) {
            popup.setLocation(contactSelector.calculatePopupLocation());
            popup.setVisible(true);
        }
    }
    
    /**
     * Returns the <tt>PresenceStatus</tt> of the default contact for this chat
     * panel.
     * @return the <tt>PresenceStatus</tt> of the default contact for this chat
     * panel.
     */
    public PresenceStatus getPresenceStatus() {
        return getMetaContact().getDefaultContact().getPresenceStatus();
    }

    /**
     * Implements the <code>ExportedDialog.isDialogVisible</code> method, to 
     * check whether this chat panel is currently visible.
     * @return <code>true</code> if this chat panel is currently visible,
     * <code>false</code> otherwise.
     */
    public boolean isDialogVisible() {
        return this.isVisible;
    }

    /**
     * Implements the <code>ExportedDialog.showDialog</code> method, to 
     * make a chat panel visible.
     */
    public void showDialog() {
        Hashtable contactChats = chatWindow.getContactChatsTable();
        
        if(Constants.TABBED_CHAT_WINDOW) {
            if(!contactChats.containsValue(this))
                this.chatWindow.addChatTab(this);
            else
                this.chatWindow.setSelectedContactTab(getMetaContact());
        }
        else {
            if(!contactChats.containsValue(this))
                this.chatWindow.addChat(this);
        }
        
        this.chatWindow.setVisible(true);
    }

    /**
     * Implements the <code>ExportedDialog.hideDialog</code> method, to 
     * hide a chat panel.
     */
    public void hideDialog() {
        this.isVisible = false;
        
        if(Constants.TABBED_CHAT_WINDOW) {
            this.chatWindow.removeChatTab(this);
        }
        else {
            this.chatWindow.removeChat(this);
        }
    }

    /**
     * Implements the <code>ExportedDialog.resizeDialog</code> method, to 
     * resize the chat window to the given width and height.
     * @param width The new width to set.
     * @param height The new height to set.
     */
    public void resizeDialog(int width, int height) {
        this.chatWindow.setSize(width, height);
    }

    /**
     * Implements the <code>ExportedDialog.moveDialog</code> method, to 
     * move the chat window to the given x and y coordinates.
     * @param x The <code>x</code> coordinate.
     * @param y The <code>y</code> coordinate.
     */
    public void moveDialog(int x, int y) {
        this.chatWindow.setLocation(x, y);
    }

    /**
     * Sets the chat <code>isVisible</code> variable to <code>true</code> or
     * <code>false</code> to indicate that this chat panel is visible or
     * invisible.
     */
    public void setChatVisible(boolean isVisible) {
        this.isVisible = isVisible;
    }
}
