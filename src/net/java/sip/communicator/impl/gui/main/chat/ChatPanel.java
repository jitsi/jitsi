/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chat;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

import javax.swing.*;
import javax.swing.text.*;
import javax.swing.text.html.*;

import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.gui.*;
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
public abstract class ChatPanel
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
    
    private ChatContactListPanel chatContactListPanel;
    
    private ChatSendPanel sendPanel;

    private ChatWindow chatWindow;
    
    private boolean isChatVisible;
    
    public static final int TYPING_NOTIFICATION_SUCCESSFULLY_SENT = 1;
    
    public static final int TYPING_NOTIFICATION_SEND_FAILED = 0;
    
    private Date beginLastPageTimeStamp; 
    
    protected static final int MESSAGES_PER_PAGE = 20;
    
    /**
     * Creates a <tt>ChatPanel</tt> which is added to the given chat window.
     *
     * @param chatWindow The parent window of this chat panel.
     * @param metaContacts the list of meta contacts contained in this chat
     * @param protocolContacts the list of selected protocol contacts in this
     * chat
     */
    public ChatPanel(ChatWindow chatWindow)
    {
        super(new BorderLayout());
        
        this.chatWindow = chatWindow;
        
        this.conversationPanel = new ChatConversationPanel(this);

        this.chatContactListPanel = new ChatContactListPanel(this);

        this.sendPanel = new ChatSendPanel(this);

        this.writeMessagePanel = new ChatWritePanel(this);

        this.topSplitPane.setResizeWeight(1.0D);
        this.messagePane.setResizeWeight(1.0D);
        this.chatContactListPanel.setPreferredSize(new Dimension(120, 100));
        this.chatContactListPanel.setMinimumSize(new Dimension(120, 100));
        this.writeMessagePanel.setPreferredSize(new Dimension(500, 100));
        this.writeMessagePanel.setMinimumSize(new Dimension(500, 100));
        this.conversationPanel.setPreferredSize(new Dimension(400, 200));
        
        this.topSplitPane.setOneTouchExpandable(true);
        
        topSplitPane.setLeftComponent(conversationPanel);
        topSplitPane.setRightComponent(chatContactListPanel);

        this.messagePane.setTopComponent(topSplitPane);
        this.messagePane.setBottomComponent(writeMessagePanel);

        this.add(messagePane, BorderLayout.CENTER);
        this.add(sendPanel, BorderLayout.SOUTH);
        
        addComponentListener(new TabSelectionComponentListener());
    }
  

    public abstract Object getChatIdentifier();
    
    public abstract String getChatName();
    
    public abstract PresenceStatus getChatStatus();
        
    public abstract void loadHistory();
    
    public abstract void loadHistory(String escapedMessageID);
    
    public abstract void loadPreviousFromHistory();
    
    public abstract void loadNextFromHistory();
    
    protected abstract void sendMessage();
    
    public abstract void treatReceivedMessage(Contact sourceContact);
    
    public abstract int sendTypingNotification(int typingState);
    
    public abstract Date getFirstHistoryMsgTimestamp();

    public abstract Date getLastHistoryMsgTimestamp();
    
    public ChatWindow getChatWindow()
    {
        return chatWindow;    
    }

    /**
     * Returns the chat window, where this chat panel
     * is located. Implements the
     * <tt>ChatConversationContainer.getConversationContainerWindow()</tt>
     * method.
     *
     * @return ChatWindow The chat window, where this
     * chat panel is located.
     */
    public Window getConversationContainerWindow() {
        return chatWindow;
    }
    
    /**
     * Sets the message text to the status panel in the bottom of the chat
     * window. Used to show typing notification messages, links' hrefs, etc.
     * @param statusMessage The message text to be displayed.
     */
    public void setStatusMessage(String statusMessage){
        this.sendPanel.setStatusMessage(statusMessage);
    }

    public ChatConversationPanel getChatConversationPanel()
    {
        return this.conversationPanel;
    }
    
    public ChatWritePanel getChatWritePanel()
    {
        return this.writeMessagePanel;
    }
    
    public ChatContactListPanel getChatContactListPanel()
    {
        return this.chatContactListPanel;
    }
    
    public ChatSendPanel getChatSendPanel()
    {
        return this.sendPanel;
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
            
            if (!(parent instanceof JTabbedPane))
                return;
            
            JTabbedPane tabbedPane = (JTabbedPane) parent;
            
            if (tabbedPane.getSelectedComponent() != component)
                return;
            
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    String chatName = getChatName();

                    if(!getChatWindow().getTitle().equals(chatName))
                    {
                        getChatWindow().setTitle(chatName);

                        getChatWindow().getMainToolBar()
                            .changeHistoryButtonsState(ChatPanel.this);
                    }

                    ChatPanel.this.requestFocusInWriteArea();
                }
            });            
        }

        public void componentHidden(ComponentEvent e) {
        }
    }

    /**
     * Moves the caret to the end of the conversation panel, contained in the
     * given chat panel.
     *
     * Workaround for the following problem:
     * The scrollbar in the conversation area moves up when the
     * scrollpane is resized. This happens when ChatWindow is in
     * mode "Group messages in one window" and the first chat panel
     * is added to the tabbed pane. Then the scrollpane in the
     * conversation area is slightly resized and is made smaller,
     * which moves the scrollbar up.
     */
    public void setCaretToEnd()
    {
        ChatConversationPanel chatConversationPanel
            = getChatConversationPanel();
        
        HTMLDocument doc = (HTMLDocument) chatConversationPanel
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
        chatConversationPanel.setCarretToEnd();
    }

    /**
     * Requests the focus in the write message area.
     */
    public void requestFocusInWriteArea()
    {
        getChatWritePanel().getEditorPane().requestFocus();
    }
    
    /**
     * Checks if the editor contains text.
     *
     * @return TRUE if editor contains text, FALSE otherwise.
     */
    public boolean isWriteAreaEmpty()
    {
        JEditorPane editorPane = getChatWritePanel().getEditorPane();

        if (editorPane.getText() == null
                || editorPane.getText().equals(""))
            return true;
        else
            return false;
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
                            getChatWindow().getMainFrame()
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
            String messageType, String message)
    {
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
    public void sendButtonDoClick()
    {
        JButton sendButton = this.sendPanel.getSendButton();

        sendButton.requestFocus();
        sendButton.doClick();
    }

    /**
     * Implements the <code>ApplicationWindow.isVisible</code> method, to
     * check whether this chat panel is currently visible.
     * @return <code>true</code> if this chat panel is currently visible,
     * <code>false</code> otherwise.
     */
    public boolean isWindowVisible() {
        return isChatVisible;
    }

    /**
     * Implements the <code>ApplicationWindow.showWindow</code> method, to
     * make a chat panel visible.
     */
    public void showWindow()
    {
        //TODO: Implement the showWindow method coming from Application Window
    }

    /**
     * Implements the <code>ApplicationWindow.hideWindow</code> method. Hides
     * the chat panel.
     */
    public void hideWindow()
    {
        //TODO: Implement the hideWindow method coming from Application Window
    }

    /**
     * Implements the <code>ApplicationWindow.resizeWindow</code> method.
     * Resizes the chat window to the given width and height.
     * @param width The new width to set.
     * @param height The new height to set.
     */
    public void resizeWindow(int width, int height) {
        getChatWindow().setSize(width, height);
    }

    /**
     * Implements the <code>ApplicationWindow.moveWindow</code> method. Moves
     * the chat window to the given x and y coordinates.
     * @param x The <code>x</code> coordinate.
     * @param y The <code>y</code> coordinate.
     */
    public void moveWindow(int x, int y) {
        getChatWindow().setLocation(x, y);
    }

    /**
     * Implements the <code>ApplicationWindow.minimizeWindow</code> method.
     * Minimizes the chat window.
     */
    public void minimizeWindow()
    {
        getChatWindow().setState(JFrame.ICONIFIED);
    }

    /**
     * Implements the <code>ApplicationWindow.maximizeWindow</code> method.
     * Maximizes the chat window.
     */
    public void maximizeWindow()
    {
        getChatWindow().setState(JFrame.MAXIMIZED_BOTH);
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
        this.isChatVisible = isVisible;
    }
    
    public void setBeginLastPageTimeStamp(Date pageFirstMsgTimestamp)
    {
        this.beginLastPageTimeStamp = pageFirstMsgTimestamp;
    }


    public Date getBeginLastPageTimeStamp()
    {
        return beginLastPageTimeStamp;
    }
}
