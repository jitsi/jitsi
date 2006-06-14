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
import java.awt.FocusTraversalPolicy;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.io.IOException;
import java.util.Date;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JViewport;
import javax.swing.LayoutFocusTraversalPolicy;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.html.HTMLDocument;

import net.java.sip.communicator.impl.gui.main.customcontrols.SIPCommSelectorBox;
import net.java.sip.communicator.service.contactlist.MetaContact;
import net.java.sip.communicator.service.protocol.Contact;
import net.java.sip.communicator.service.protocol.OperationSetBasicInstantMessaging;
import net.java.sip.communicator.service.protocol.OperationSetTypingNotifications;
import net.java.sip.communicator.service.protocol.PresenceStatus;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;
import net.java.sip.communicator.service.protocol.event.MessageReceivedEvent;
import net.java.sip.communicator.util.Logger;

/**
 * The ChatPanel is the panel, where users can write and send messages, view
 * received messages. A ChatPanel is created for a contact or for a group of
 * contacts in case of a chat conference. There is always one default contact
 * for the chat, which is the first contact which was added to the chat.
 * When chat is in mode "open all messages in new window", each ChatPanel
 * corresponds to a ChatWindow. When chat is in mode "group all messages in
 * one chat window", each ChatPanel corresponds to a tab in the ChatWindow.
 * 
 * @author Yana Stamcheva
 */
public class ChatPanel extends JPanel {

    private static final Logger logger = Logger
        .getLogger(ChatPanel.class.getName());
    
    private JSplitPane topSplitPane = new JSplitPane(
            JSplitPane.HORIZONTAL_SPLIT);

    private JSplitPane messagePane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

    private ChatConversationPanel conversationPanel;

    private ChatWritePanel writeMessagePanel;

    private ChatConferencePanel chatConferencePanel = new ChatConferencePanel();

    private ChatSendPanel sendPanel;

    private Vector chatContacts = new Vector();

    private ChatWindow chatWindow;

    private OperationSetBasicInstantMessaging imOperationSet;

    private OperationSetTypingNotifications tnOperationSet;

    private Contact protocolContact;
    
    /**
     * Creates a chat panel which is added to the given chat window.
     * 
     * @param chatWindow The parent window of this chat panel.
     * @param protocolContact The subContact which is selected ins
     * the chat.
     */
    public ChatPanel(ChatWindow chatWindow, Contact protocolContact) {

        super(new BorderLayout());

        this.chatWindow = chatWindow;
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
     * Adds a new MetaContact to this chat panel.
     * 
     * @param contactItem The MetaContact to add.
     * @param status The current presence status of the contact.
     */
    public void addContactToChat(MetaContact contactItem, 
                                PresenceStatus status) {
        this.chatContacts.add(contactItem);

        this.chatConferencePanel.addContactToChat(contactItem, status);

        this.sendPanel.addProtocolContacts(contactItem);

        this.sendPanel.setSelectedProtocolContact(this.protocolContact);
    }

    /**
     * Adds a new MetaContact to this chat panel.
     * 
     * @param contactItem The MetaContact to add.
     */
    public void addContactToChat(MetaContact contactItem) {

        this.chatContacts.add(contactItem);

        this.chatConferencePanel.addContactToChat(contactItem);

        this.sendPanel.addProtocolContacts(contactItem);
    }

    /**
     * Removes a MetaContact from the chat.
     * 
     * @param contactItem The MetaContact to remove.
     */
    public void removeContactFromChat(MetaContact contactItem) {
        this.chatContacts.remove(contactItem);
    }

    /**
     * Returns all contacts for this chat.
     * 
     * @return A Vector containing all MetaContact-s 
     * for the chat.
     */
    public Vector getChatContacts() {
        return chatContacts;
    }

    /**
     * Sets all contacts for this chat. This is in the 
     * case when we creates a conference chat.
     * 
     * @param chatContacts A Vector of MetaContact-s.
     */
    public void setChatContacts(Vector chatContacts) {
        this.chatContacts = chatContacts;
    }

    /**
     * Updates the contact status in the contact info panel.
     * 
     * @param status The presence status of the contact.
     */
    public void updateContactStatus(PresenceStatus status) {
        this.chatConferencePanel.updateContactStatus(status);
    }

    /**
     * Returns the panel that contains the "write" editor 
     * pane of this chat.
     * 
     * @return The ChatWritePanel.
     */
    /*
    public ChatWritePanel getWriteMessagePanel() {
        return writeMessagePanel;
    }
*/
    /**
     * Returns the panel that contains the conversation.
     * 
     * @return The ChatConversationPanel.
     */
    /*
    public ChatConversationPanel getConversationPanel() {
        return conversationPanel;
    }
    */
    /**
     * Returns the default contact for the chat. The case of conference 
     * is not yet implemented and for now it returns the first contact.
     * 
     * @return The default contact for the chat.
     */
    public MetaContact getDefaultContact() {
        return (MetaContact) this.getChatContacts().get(0);
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
                                    getDefaultContact().getDisplayName());

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
     * Sets the current contact typing status.
     */
    public void setChatStatus(String statusMessage){
        this.sendPanel.setChatStatus(statusMessage);
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
     * Copies the selected conversation panel content to the clipboard.
     */
    public void copyConversation(){
        JEditorPane editorPane = this.conversationPanel.getChatEditorPane();

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
        HTMLDocument doc = (HTMLDocument)this.conversationPanel.getChatEditorPane()
            .getDocument();
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
}
