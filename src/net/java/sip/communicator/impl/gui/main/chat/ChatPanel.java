/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chat;

import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.io.*;
import java.util.*;

import javax.swing.*;
import javax.swing.text.*;
import javax.swing.text.html.*;

import net.java.sip.communicator.impl.gui.main.contactlist.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.event.*;
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
    implements  Chat,
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

    public static final int TYPING_NOTIFICATION_SUCCESSFULLY_SENT = 1;

    public static final int TYPING_NOTIFICATION_SEND_FAILED = 0;

    private Date beginLastPageTimeStamp;

    protected static final int MESSAGES_PER_PAGE = 20;

    private boolean isShown = false;

    private Vector focusListeners = new Vector();
    
    /**
     * Creates a <tt>ChatPanel</tt> which is added to the given chat window.
     *
     * @param chatWindow The parent window of this chat panel.
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

        this.addComponentListener(new TabSelectionComponentListener());

        KeyboardFocusManager focusManager =
            KeyboardFocusManager.getCurrentKeyboardFocusManager();

        focusManager.addPropertyChangeListener(
            new FocusPropertyChangeListener()
        );
    }

    public abstract Object getChatIdentifier();

    public abstract String getChatName();

    public abstract PresenceStatus getChatStatus();

    public abstract void loadHistory();

    public abstract void loadHistory(String escapedMessageID);

    public abstract void loadPreviousPageFromHistory();

    public abstract void loadNextPageFromHistory();

    protected abstract void sendMessage(String text);

    public abstract void treatReceivedMessage(Contact sourceContact);

    public abstract int sendTypingNotification(int typingState);

    public abstract Date getFirstHistoryMsgTimestamp();

    public abstract Date getLastHistoryMsgTimestamp();
    
    public abstract void inviteChatContact(String contactAddress, String reason);
    
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

        public void componentResized(ComponentEvent evt) {
        }

        public void componentMoved(ComponentEvent evt) {
        }

        public void componentShown(ComponentEvent evt)
        {
            Component component = evt.getComponent();
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

        public void componentHidden(ComponentEvent evt) {
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
        } catch (BadLocationException exc) {
            logger.error("Insert in the HTMLDocument failed.", exc);
        } catch (IOException exc) {
            logger.error("Insert in the HTMLDocument failed.", exc);
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
        Iterator iterator = historyList.iterator();
        String historyString = "";
        while (iterator.hasNext()) {
            Object o = iterator.next();

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
                            evt.getSourceMessage().getContent(),
                            evt.getSourceMessage().getContentType());
            }
            else if(o instanceof MessageReceivedEvent) {
                MessageReceivedEvent evt = (MessageReceivedEvent)o;

                if(!evt.getSourceMessage().getMessageUID()
                        .equals(escapedMessageID)) {
                historyString += processHistoryMessage(
                            evt.getSourceContact().getDisplayName(),
                            evt.getTimestamp(),
                            Constants.HISTORY_INCOMING_MESSAGE,
                            evt.getSourceMessage().getContent(),
                            evt.getSourceMessage().getContentType());
                }
            }
        }
        
        conversationPanel.insertMessageAfterStart(historyString);
        
        getChatWindow().getMainToolBar()
            .changeHistoryButtonsState(this);
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
            String messageType, String message, String contentType)
    {
        String processedMessage
            = this.conversationPanel.processMessage(contactName, date,
                                            messageType, message, contentType);
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
            String messageType, String message, String contentType){
        String processedMessage
            = this.conversationPanel.processMessage(contactName, date,
                                            messageType, message, contentType);
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
     * Returns TRUE if this chat panel is added to a container (window or
     * tabbed pane), which is shown on the screen, FALSE - otherwise.
     *
     * @return TRUE if this chat panel is added to a container (window or
     * tabbed pane), which is shown on the screen, FALSE - otherwise
     */
    public boolean isShown()
    {
        return isShown;
    }

    /**
     * Marks this chat panel as shown or hidden.
     *
     * @param isShown TRUE to mark this chat panel as shown, FALSE - otherwise
     */
    public void setShown(boolean isShown)
    {
        this.isShown = isShown;
    }

    /**
     * Implements the <tt>Chat.isChatFocused</tt> method. Returns TRUE if this
     * chat panel is the currently selected panel and if the chat window, where
     * it's contained is active.
     *
     * @return true if this chat panel has the focus and false otherwise.
     */
    public boolean isChatFocused()
    {
        ChatPanel currentChatPanel = chatWindow.getCurrentChatPanel();

        if(currentChatPanel != null
                && currentChatPanel.equals(this)
                && chatWindow.isActive())
            return true;

        return false;
    }

    /**
     * The <tt>FocusPropertyChangeListener</tt> listens for events triggered
     * when the "focusOwner" property has changed. It is used to change the
     * state of a contact from active (we have non read messages from this
     * contact) to inactive, when user has opened a chat.
     */
    private class FocusPropertyChangeListener implements PropertyChangeListener
    {
        public void propertyChange(PropertyChangeEvent evt)
        {
            String prop = evt.getPropertyName();
            if ((prop.equals("focusOwner")) &&
                  (evt.getNewValue() != null) &&
                  (evt.getNewValue() instanceof Component) &&
                  ((Component)evt.getNewValue())
                      .getFocusCycleRootAncestor() instanceof ChatWindow)
            {
                ChatWindow chatWindow = (ChatWindow)((Component)evt
                        .getNewValue()).getFocusCycleRootAncestor();

                ChatPanel chatPanel
                    = chatWindow.getCurrentChatPanel();

                if(chatPanel instanceof MetaContactChatPanel)
                {
                    MetaContact selectedMetaContact
                        = ((MetaContactChatPanel)chatPanel).getMetaContact();

                    ContactList clist
                        = chatWindow.getMainFrame()
                            .getContactListPanel().getContactList();
                    ContactListModel clistModel
                        = (ContactListModel) clist.getModel();

                    if(clistModel.isContactActive(selectedMetaContact))
                    {
                        clistModel.removeActiveContact(selectedMetaContact);
                        clist.refreshContact(selectedMetaContact);
                    }

                    fireChatFocusEvent(ChatFocusEvent.FOCUS_GAINED);
                }
            }
        }
    }

    /**
     * Implements <tt>Chat.addChatFocusListener</tt> method. Adds the given
     * <tt>ChatFocusListener</tt> to the list of listeners.
     *
     * @param listener the listener that we'll be adding.
     */
    public void addChatFocusListener(ChatFocusListener listener)
    {
        synchronized (focusListeners)
        {
            focusListeners.add(listener);
        }
    }

    /**
     * Implements <tt>Chat.removeChatFocusListener</tt> method. Removes the given
     * <tt>ChatFocusListener</tt> from the list of listeners.
     *
     * @param listener the listener to remove.
     */
    public void removeChatFocusListener(ChatFocusListener listener)
    {
        synchronized (focusListeners)
        {
            focusListeners.remove(listener);
        }
    }

    /**
     * Adds the given {@link KeyListener} to this <tt>Chat</tt>.
     * The <tt>KeyListener</tt> is used to inform other bundles when a user has
     * typed in the chat editor area.
     * 
     * @param l the <tt>KeyListener</tt> to add
     */
    public void addChatEditorKeyListener(KeyListener l)
    {
        this.getChatWritePanel().getEditorPane().addKeyListener(l);
    }
    
    /**
     * Removes the given {@link KeyListener} from this <tt>Chat</tt>.
     * The <tt>KeyListener</tt> is used to inform other bundles when a user has
     * typed in the chat editor area.
     * 
     * @param l the <tt>ChatFocusListener</tt> to remove
     */
    public void removeChatEditorKeyListener(KeyListener l)
    {
        this.getChatWritePanel().getEditorPane().removeKeyListener(l);
    }
    
    /**
     * Returns the message written by user in the chat write area.
     *
     * @return the message written by user in the chat write area
     */
    public String getMessage()
    {
        return writeMessagePanel.getEditorPane().getText();
    }

    /**
     * Sets the given message as a message in the chat write area.
     *
     * @param message the text that would be set to the chat write area
     */
    public void setMessage(String message)
    {
        writeMessagePanel.getEditorPane().setText(message);
    }
    
    /**
     * Informs all <tt>ChatFocusListener</tt>s that a <tt>ChatFocusEvent</tt>
     * has been triggered.
     *
     * @param eventID the type of the <tt>ChatFocusEvent</tt>
     */
    private void fireChatFocusEvent(int eventID)
    {
        ChatFocusEvent evt = new ChatFocusEvent(this, eventID);

        logger.trace("Will dispatch the following chat event: " + evt);

        Iterator listeners = null;
        synchronized (focusListeners)
        {
            listeners = new ArrayList(focusListeners).iterator();
        }

        while (listeners.hasNext())
        {
            ChatFocusListener listener
                = (ChatFocusListener) listeners.next();

            switch (evt.getEventID())
            {
            case ChatFocusEvent.FOCUS_GAINED:
                listener.chatFocusGained(evt);
                break;
            case ChatFocusEvent.FOCUS_LOST:
                listener.chatFocusLost(evt);
                break;
            default:
                logger.error("Unknown event type " + evt.getEventID());
            }
        }
    }
}
