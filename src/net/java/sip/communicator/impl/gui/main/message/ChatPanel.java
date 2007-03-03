/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.message;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

import javax.swing.*;
import javax.swing.text.*;
import javax.swing.text.html.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.msghistory.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

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
public class ChatPanel
    extends JPanel
    implements  ApplicationWindow,
                ChatConversationContainer
{

    private static final Logger logger = Logger
        .getLogger(ChatPanel.class.getName());

    private JSplitPane topSplitPane = new JSplitPane(
            JSplitPane.HORIZONTAL_SPLIT);

    private JSplitPane messagePane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

    private ChatConversationPanel conversationPanel;

    private ChatWritePanel writeMessagePanel;

    private ChatConferencePanel chatConferencePanel;

    private ChatSendPanel sendPanel;

    private ChatWindow chatWindow;

    private OperationSetBasicInstantMessaging imOperationSet;

    private OperationSetTypingNotifications tnOperationSet;

    private MetaContact metaContact;

    private boolean isVisible = false;

    private Date firstHistoryMsgTimestamp;

    private Date lastHistoryMsgTimestamp;

    MessageHistoryService msgHistory
        = GuiActivator.getMsgHistoryService();

    /**
     * Creates a <tt>ChatPanel</tt> which is added to the given chat window.
     *
     * @param chatWindow The parent window of this chat panel.
     * @param metaContact the meta contact that this chat is about.
     * @param protocolContact The subContact which is selected ins
     * the chat.
     */
    public ChatPanel(   ChatWindow chatWindow,
                        MetaContact metaContact,
                        Contact protocolContact) {

        super(new BorderLayout());

        this.chatWindow = chatWindow;
        this.metaContact = metaContact;

        this.imOperationSet = this.chatWindow.getMainFrame().getProtocolIM(
                protocolContact.getProtocolProvider());
        this.tnOperationSet = this.chatWindow.getMainFrame()
                .getTypingNotifications(protocolContact.getProtocolProvider());

        this.conversationPanel = new ChatConversationPanel(this);

        this.chatConferencePanel = new ChatConferencePanel(this,
            metaContact, protocolContact);

        this.sendPanel = new ChatSendPanel(this, metaContact, protocolContact);

        this.writeMessagePanel = new ChatWritePanel(this);

        this.topSplitPane.setResizeWeight(1.0D);
        this.messagePane.setResizeWeight(1.0D);
        this.chatConferencePanel.setPreferredSize(new Dimension(120, 100));
        this.chatConferencePanel.setMinimumSize(new Dimension(120, 100));
        this.writeMessagePanel.setPreferredSize(new Dimension(500, 100));
        this.writeMessagePanel.setMinimumSize(new Dimension(500, 100));
        this.conversationPanel.setPreferredSize(new Dimension(400, 200));

        this.init();

        addComponentListener(new TabSelectionComponentListener());

        new Thread(){
            public void run(){
                loadHistoryPeriod();
            }
        }.start();
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
     * Loads history in another thread.
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
     * escapedMessageID.
     * @param escapedMessageID The id of the message that should be ignored.
     */
    public void loadHistory(String escapedMessageID)
    {
        Collection historyList = msgHistory.findLast(
                metaContact, Constants.CHAT_HISTORY_SIZE);

        processHistory(historyList, escapedMessageID);
    }

    /**
     * Process history messages.
     *
     * @param historyList The collection of messages coming from history.
     * @param escapedMessageID The incoming message needed to be ignored if
     * contained in history.
     */
    public void processHistory(Collection historyList,
            String escapedMessageID)
    {
        Iterator i = historyList.iterator();
        String historyString = "";
        while (i.hasNext()) {
            Object o = i.next();

            if(o instanceof MessageDeliveredEvent) {
                MessageDeliveredEvent evt
                    = (MessageDeliveredEvent)o;

                ProtocolProviderService protocolProvider = evt
                    .getDestinationContact().getProtocolProvider();

                historyString += processHistoryMessage(
                            chatWindow.getMainFrame()
                                .getAccount(protocolProvider),
                            evt.getTimestamp(),
                            Constants.HISTORY_OUTGOING_MESSAGE,
                            evt.getSourceMessage().getContent());
            }
            else if(o instanceof MessageReceivedEvent) {
                MessageReceivedEvent evt = (MessageReceivedEvent)o;

                if(!evt.getSourceMessage().getMessageUID()
                        .equals(escapedMessageID)) {
                historyString += processHistoryMessage(
                            evt.getSourceContact().getDisplayName(),
                            evt.getTimestamp(),
                            Constants.HISTORY_INCOMING_MESSAGE,
                            evt.getSourceMessage().getContent());
                }
            }
        }
        conversationPanel.insertMessageAfterStart(historyString);
    }

    /**
     * Updates the contact status in the chat panel.
     *
     * @param metaContact the meta contact that this chat is about.
     * @param protoContact the protocol contact which status to update.
     */
    public void updateContactStatus(MetaContact metaContact, Contact protoContact)
    {
        PresenceStatus status = sendPanel.getProtoContactSelectorBox()
            .getSelectedProtocolContact().getPresenceStatus();

        this.chatConferencePanel.updateContactStatus(
            metaContact.getDefaultContact().getPresenceStatus());

        this.sendPanel.updateContactStatus(protoContact);

        String message = this.conversationPanel.processMessage(
                this.metaContact.getDisplayName(),
                new Date(System.currentTimeMillis()),
                Constants.SYSTEM_MESSAGE,
                Messages.getI18NString("statusChangedChatMessage",
                        new String[]{status.getStatusName()}).getText());

        this.conversationPanel.appendMessageToEnd(message);
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
     * When user select a chat tab clicking with the mouse we change the
     * currently selected chat panel, thus changing the title of the window,
     * history buttons states, etc.
     */
    private class TabSelectionComponentListener
        implements ComponentListener {

        public TabSelectionComponentListener() {
            super();
        }

        public void componentResized(ComponentEvent e) {
        }

        public void componentMoved(ComponentEvent e) {
        }

        public void componentShown(ComponentEvent e)
        {
            Component component = e.getComponent();
            Container parent = component.getParent();
            if (parent instanceof JTabbedPane)
            {
                JTabbedPane tabbedPane = (JTabbedPane) parent;
                if (tabbedPane.getSelectedComponent() == component)
                {
                    SwingUtilities.invokeLater(new Runnable()
                    {
                        public void run()
                        {
                            String metaContactName
                                = getMetaContact().getDisplayName();

                            if(!chatWindow.getTitle().equals(metaContactName))
                            {
                                chatWindow.setTitle(metaContactName);

                                chatWindow.getMainToolBar()
                                    .changeHistoryButtonsState(ChatPanel.this);
                            }

                            ChatPanel.this.requestFocusInWriteArea();
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
        return sendPanel.getProtoContactSelectorBox()
                    .getSelectedProtocolContact();
    }

    /**
     * Sets the currently selected protocol contact to use for sending messages.
     * @param protocolContact the protocol contact to select.
     */
    public void setProtocolContact(Contact protocolContact) {
        sendPanel.getProtoContactSelectorBox()
                    .setSelected(protocolContact);
    }


    /**
     * Passes the message to the contained <code>ChatConversationPanel</code>
     * for processing and appends it at the end of the conversationPanel
     * document.
     *
     * @param contactName The name of the contact sending the message.
     * @param date The time at which the message is sent or received.
     * @param messageType The type of the message. One of OUTGOING_MESSAGE
     * or INCOMING_MESSAGE.
     * @param message The message text.
     */
    public void processMessage(String contactName, Date date,
            String messageType, String message){
        String processedMessage
            = this.conversationPanel.processMessage(contactName, date,
                                            messageType, message);
        this.conversationPanel.appendMessageToEnd(processedMessage);
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
     *
     * @return a string containingthe processed message.
     */
    public String processHistoryMessage(String contactName, Date date,
            String messageType, String message){
        String processedMessage
            = this.conversationPanel.processMessage(contactName, date,
                                            messageType, message);
        return processedMessage;
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
    public void requestFocusInWriteArea()
    {
        this.writeMessagePanel.getEditorPane().requestFocus();
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
        SIPCommMenu contactSelector
            = this.sendPanel.getProtoContactSelectorBox().getMenu();
        contactSelector.doClick();
    }

    /**
     * Returns the <tt>PresenceStatus</tt> of the default contact for this chat
     * panel.
     * @return the <tt>PresenceStatus</tt> of the default contact for this chat
     * panel.
     */
    public PresenceStatus getDefaultContactStatus() {
        return getMetaContact().getDefaultContact().getPresenceStatus();
    }


    /**
     * Implements the <code>ApplicationWindow.isVisible</code> method, to
     * check whether this chat panel is currently visible.
     * @return <code>true</code> if this chat panel is currently visible,
     * <code>false</code> otherwise.
     */
    public boolean isWindowVisible() {
        return this.isVisible;
    }

    /**
     * Implements the <code>ApplicationWindow.showWindow</code> method, to
     * make a chat panel visible.
     */
    public void showWindow()
    {

    }

    /**
     * Implements the <code>ApplicationWindow.hideWindow</code> method. Hides
     * the chat panel.
     */
    public void hideWindow()
    {

    }

    /**
     * Implements the <code>ApplicationWindow.resizeWindow</code> method.
     * Resizes the chat window to the given width and height.
     * @param width The new width to set.
     * @param height The new height to set.
     */
    public void resizeWindow(int width, int height) {
        this.chatWindow.setSize(width, height);
    }

    /**
     * Implements the <code>ApplicationWindow.moveWindow</code> method. Moves
     * the chat window to the given x and y coordinates.
     * @param x The <code>x</code> coordinate.
     * @param y The <code>y</code> coordinate.
     */
    public void moveWindow(int x, int y) {
        this.chatWindow.setLocation(x, y);
    }

    /**
     * Implements the <code>ApplicationWindow.minimizeWindow</code> method.
     * Minimizes the chat window.
     */
    public void minimizeWindow()
    {
        this.chatWindow.setState(JFrame.ICONIFIED);
    }

    /**
     * Implements the <code>ApplicationWindow.maximizeWindow</code> method.
     * Maximizes the chat window.
     */
    public void maximizeWindow()
    {
        this.chatWindow.setState(JFrame.MAXIMIZED_BOTH);
    }

    /**
     * Sets the chat <code>isVisible</code> variable to <code>true</code> or
     * <code>false</code> to indicate that this chat panel is visible or
     * invisible.
     *
     * @param isVisible specifies whether we'd like this chat panel visible or
     * not.
     */
    public void setChatVisible(boolean isVisible) {
        this.isVisible = isVisible;
    }

    /**
     * Returns the chat conversation panel contained in this chat panel.
     * @return the chat conversation panel contained in this chat panel
     */
    public ChatConversationPanel getChatConversationPanel()
    {
        return conversationPanel;
    }

    /**
     * Returns the chat write panel contained in this chat panel.
     * @return the chat write panel contained in this chat panel
     */
    public ChatWritePanel getChatWritePanel()
    {
        return writeMessagePanel;
    }

    /**
     * Returns the chat send panel contained in this chat panel.
     * @return the chat send panel contained in this chat panel
     */
    public ChatSendPanel getChatSendPanel()
    {
        return sendPanel;
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

        this.chatWindow.getMainToolBar().changeHistoryButtonsState(this);
    }

    public Date getFirstHistoryMsgTimestamp()
    {
        return firstHistoryMsgTimestamp;
    }

    public Date getLastHistoryMsgTimestamp()
    {
        return lastHistoryMsgTimestamp;
    }

    /**
     * Updates all occurences of the contact name in the chat.
     * @param newName the new name
     */
    public void renameContact(String newName)
    {
        chatConferencePanel.renameContact(newName);

        chatWindow.setTabTitle(this, newName);

        if( chatWindow.getCurrentChatPanel() == this)
        {
            chatWindow.setTitle(newName);
        }
    }

    public ChatConferencePanel getChatConferencePanel()
    {
        return chatConferencePanel;
    }
}
