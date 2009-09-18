/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chat;

import java.awt.*;
import java.awt.Container;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.util.*;

import javax.swing.*;
import javax.swing.text.*;
import javax.swing.text.html.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.chat.conference.*;
import net.java.sip.communicator.impl.gui.main.chat.filetransfer.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.filehistory.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.event.*;
import net.java.sip.communicator.service.metahistory.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.swing.*;
import net.java.sip.communicator.util.swing.SwingWorker; // disambiguation

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
 * @author Lubomir Marinov
 */
public class ChatPanel
    extends TransparentPanel
    implements  ChatSessionRenderer,
                Chat,
                ChatConversationContainer,
                FileTransferStatusListener
{
    private static final Logger logger = Logger.getLogger(ChatPanel.class);

    private final JSplitPane messagePane
        = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

    private final JCheckBox sendSmsCheckBox = new SIPCommCheckBox(
        GuiActivator.getResources().getI18NString("service.gui.SEND_AS_SMS"));

    private JSplitPane topSplitPane;

    private ChatTransportSelectorBox transportSelectorBox;

    private JLabel sendViaLabel;

    private final ChatConversationPanel conversationPanel;

    private final ChatWritePanel writeMessagePanel;

    private ChatRoomMemberListPanel chatContactListPanel;

    private final ChatSendPanel sendPanel;

    private final ChatWindow chatWindow;

    private ChatRoomSubjectPanel subjectPanel;

    public int unreadMessageNumber = 0;

    /**
     * Indicates that a typing notification event is successfully sent.
     */
    public static final int TYPING_NOTIFICATION_SUCCESSFULLY_SENT = 1;

    /**
     * Indicates that sending a typing notification event has failed.
     */
    public static final int TYPING_NOTIFICATION_SEND_FAILED = 0;

    protected static final int MESSAGES_PER_PAGE = 20;

    private boolean isShown = false;

    public ChatSession chatSession;

    private long firstHistoryMsgTimestamp;

    private long lastHistoryMsgTimestamp;

    private final java.util.List<ChatFocusListener> focusListeners =
        new Vector<ChatFocusListener>();

    private final Vector<Object> incomingEventBuffer = new Vector<Object>();

    private boolean isHistoryLoaded;

    /**
     * Stores all active  file transfer requests and effective transfers with
     * the identifier of the transfer.
     */
    private final Hashtable<String, Object> activeFileTransfers
        = new Hashtable<String, Object>();

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
        this.conversationPanel.setPreferredSize(new Dimension(400, 200));
        this.conversationPanel.getChatTextPane()
            .setTransferHandler(new ChatTransferHandler(this));

        this.sendPanel = new ChatSendPanel(this);

        this.writeMessagePanel = new ChatWritePanel(this);

        int chatAreaSize = ConfigurationManager.getChatWriteAreaSize();

        Dimension writeMessagePanelDefaultSize;
        if (chatAreaSize > 0)
            writeMessagePanelDefaultSize = new Dimension(500, chatAreaSize);
        else
            writeMessagePanelDefaultSize = new Dimension(500, 100);

        Dimension writeMessagePanelMinSize = new Dimension(500, 45);
        this.writeMessagePanel.setMinimumSize(writeMessagePanelMinSize);
        this.writeMessagePanel.setPreferredSize(writeMessagePanelDefaultSize);

        this.messagePane.setBorder(null);
        this.messagePane.setOpaque(false);
        this.messagePane.addPropertyChangeListener(
            new DividerLocationListener());

        this.messagePane.setResizeWeight(1.0D);
        this.messagePane.setBottomComponent(writeMessagePanel);

        this.add(messagePane, BorderLayout.CENTER);
        this.add(sendPanel, BorderLayout.SOUTH);

        this.addComponentListener(new TabSelectionComponentListener());
    }

    public void setChatSession(ChatSession chatSession)
    {
        this.chatSession = chatSession;

        if ((this.chatSession != null)
                && this.chatSession.isContactListSupported())
        {
            messagePane.remove(conversationPanel);

            TransparentPanel rightPanel
                = new TransparentPanel(new BorderLayout(5, 5));
            Dimension chatContactPanelSize = new Dimension(150, 100);
            rightPanel.setMinimumSize(chatContactPanelSize);
            rightPanel.setPreferredSize(chatContactPanelSize);

            this.chatContactListPanel = new ChatRoomMemberListPanel(this);
            this.chatContactListPanel.setOpaque(false);

            topSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
            topSplitPane.setBorder(null); // remove default borders
            topSplitPane.setOneTouchExpandable(true);
            topSplitPane.setOpaque(false);
            topSplitPane.setResizeWeight(1.0D);

            ChatTransport chatTransport = chatSession.getCurrentChatTransport();

            JLabel localUserLabel = new JLabel(
                chatTransport.getProtocolProvider()
                    .getAccountID().getDisplayName());

            localUserLabel.setFont(
                localUserLabel.getFont().deriveFont(Font.BOLD));

            rightPanel.add(localUserLabel, BorderLayout.NORTH);
            rightPanel.add(chatContactListPanel, BorderLayout.CENTER);

            topSplitPane.setLeftComponent(conversationPanel);
            topSplitPane.setRightComponent(rightPanel);

            messagePane.setTopComponent(topSplitPane);
        }
        else
        {
            if (topSplitPane != null)
            {
                if (chatContactListPanel != null)
                {
                    topSplitPane.remove(chatContactListPanel);
                    chatContactListPanel = null;
                }

                this.messagePane.remove(topSplitPane);
                topSplitPane = null;
            }

            this.messagePane.setTopComponent(conversationPanel);
        }

        if (chatSession instanceof MetaContactChatSession)
        {
            // The subject panel is added here, because it's specific for the
            // multi user chat and is not contained in the single chat chat panel.
            if (subjectPanel != null)
            {
                this.remove(subjectPanel);
                subjectPanel = null;

                this.revalidate();
                this.repaint();
            }

            initChatTransportSelectorBox();

            if (!transportSelectorBox.getMenu().isEnabled())
            {
                // Show a message to the user that IM is not possible.
                getChatConversationPanel().appendMessageToEnd("<h5>" +
                        GuiActivator.getResources().
                            getI18NString("service.gui.MSG_NOT_POSSIBLE") +
                        "</h5>");
            }

            //Enables to change the protocol provider by simply pressing the
            // CTRL-P key combination
            ActionMap amap = this.getActionMap();

            amap.put("ChangeProtocol", new ChangeTransportAction());

            InputMap imap = this.getInputMap(
                JComponent.WHEN_IN_FOCUSED_WINDOW);

            imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_P,
                KeyEvent.CTRL_DOWN_MASK), "ChangeProtocol");
        }
        else if (chatSession instanceof ConferenceChatSession)
        {
            removeChatTransportSelectorBox();


//          We don't add the subject panel for now. It takes too much space
//          and is not used.
//          subjectPanel
//              = new ChatRoomSubjectPanel( chatWindow,
//                                          (ConferenceChatSession) chatSession);
            // The subject panel is added here, because it's specific for the
            // multi user chat and is not contained in the single chat chat panel.
//            this.add(subjectPanel, BorderLayout.NORTH);
        }

        if (chatContactListPanel != null)
        {

            // Initialize chat participants' panel.
            Iterator<ChatContact> chatParticipants
                = chatSession.getParticipants();

            while (chatParticipants.hasNext())
            {
                //Add the contact to the list of contacts contained in this chat.
                chatContactListPanel.addContact(chatParticipants.next());
            }
        }

        if (!chatSession.getCurrentChatTransport().allowsSmsMessage())
            sendSmsCheckBox.setEnabled(false);
    }

    public ChatSession getChatSession()
    {
        return chatSession;
    }
    
    /**
     * Shows or hides the Stylebar depending on the value of parameter b.
     * 
     * @param b if true, makes the Stylebar visible, otherwise hides the Stylebar
     */
    public void setStylebarVisible(boolean b)
    {
        this.writeMessagePanel.setStylebarVisible(b);
    }

    /**
     * Runs clean-up for associated resources which need explicit disposal (e.g.
     * listeners keeping this instance alive because they were added to the
     * model which operationally outlives this instance).
     */
    public void dispose()
    {
        writeMessagePanel.dispose();
        chatSession.dispose();
    }

    /**
     * Returns the chat window, where this chat panel is added.
     *
     * @return the chat window, where this chat panel is added
     */
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
    public Window getConversationContainerWindow()
    {
        return chatWindow;
    }

    /**
     * Sets the message text to the status panel in the bottom of the chat
     * window. Used to show typing notification messages, links' hrefs, etc.
     * @param statusMessage The message text to be displayed.
     */
    public void setStatusMessage(String statusMessage)
    {
        this.sendPanel.getStatusPanel().setStatusMessage(statusMessage);
    }

    /**
     * Returns the conversation panel, contained in this chat panel.
     *
     * @return the conversation panel, contained in this chat panel
     */
    public ChatConversationPanel getChatConversationPanel()
    {
        return this.conversationPanel;
    }

    /**
     * Returns the write area panel, contained in this chat panel.
     *
     * @return the write area panel, contained in this chat panel
     */
    public ChatWritePanel getChatWritePanel()
    {
        return this.writeMessagePanel;
    }

    /**
     * Returns the send panel, contained in this chat panel.
     *
     * @return the send panel, contained in this chat panel
     */
    public ChatSendPanel getChatSendPanel()
    {
        return this.sendPanel;
    }

    /**
     * Every time the chat panel is shown we set it as a current chat panel.
     * This is done here and not in the Tab selection listener, because the tab
     * change event is not fired when the user clicks on the close tab button
     * for example.
     */
    private class TabSelectionComponentListener
        extends ComponentAdapter
    {
        public void componentShown(ComponentEvent evt)
        {
            Component component = evt.getComponent();
            Container parent = component.getParent();

            if (!(parent instanceof JTabbedPane))
                return;

            JTabbedPane tabbedPane = (JTabbedPane) parent;

            if (tabbedPane.getSelectedComponent() != component)
                return;

            chatWindow.setCurrentChatPanel(ChatPanel.this);
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
            .getChatTextPane().getDocument();

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
        getChatWritePanel().getEditorPane().requestFocusInWindow();
    }

    /**
     * Checks if the editor contains text.
     *
     * @return TRUE if editor contains text, FALSE otherwise.
     */
    public boolean isWriteAreaEmpty()
    {
        JEditorPane editorPane = getChatWritePanel().getEditorPane();

        Document doc = editorPane.getDocument();

        try
        {
            String text = doc.getText(0, doc.getLength());

            if (text == null || text.equals(""))
                return true;
        }
        catch (BadLocationException e)
        {
            logger.error("Failed to obtain document text.", e);
        }

        return false;
    }

    /**
     * Process history messages.
     *
     * @param historyList The collection of messages coming from history.
     * @param escapedMessageID The incoming message needed to be ignored if
     * contained in history.
     */
    private void processHistory( Collection<Object> historyList,
                                String escapedMessageID)
    {
        Iterator<Object> iterator = historyList.iterator();

        String messageType;

        while (iterator.hasNext())
        {
            Object o = iterator.next();
            String historyString = "";

            if(o instanceof MessageDeliveredEvent)
            {
                MessageDeliveredEvent evt
                    = (MessageDeliveredEvent)o;

                ProtocolProviderService protocolProvider = evt
                    .getDestinationContact().getProtocolProvider();

                if (isGreyHistoryStyleDisabled(protocolProvider))
                    messageType = Chat.OUTGOING_MESSAGE;
                else
                    messageType = Chat.HISTORY_OUTGOING_MESSAGE;

                historyString = processHistoryMessage(
                            GuiActivator.getUIService().getMainFrame()
                                .getAccount(protocolProvider),
                            evt.getTimestamp(),
                            messageType,
                            evt.getSourceMessage().getContent(),
                            evt.getSourceMessage().getContentType());
            }
            else if(o instanceof MessageReceivedEvent)
            {
                MessageReceivedEvent evt = (MessageReceivedEvent)o;

                ProtocolProviderService protocolProvider
                    = evt.getSourceContact().getProtocolProvider();

                if(!evt.getSourceMessage().getMessageUID()
                        .equals(escapedMessageID))
                {
                    if (isGreyHistoryStyleDisabled(protocolProvider))
                        messageType = Chat.INCOMING_MESSAGE;
                    else
                        messageType = Chat.HISTORY_INCOMING_MESSAGE;

                    historyString = processHistoryMessage(
                                evt.getSourceContact().getDisplayName(),
                                evt.getTimestamp(),
                                messageType,
                                evt.getSourceMessage().getContent(),
                                evt.getSourceMessage().getContentType());
                }
            }
            else if(o instanceof ChatRoomMessageDeliveredEvent)
            {
                ChatRoomMessageDeliveredEvent evt
                    = (ChatRoomMessageDeliveredEvent)o;

                ProtocolProviderService protocolProvider = evt
                    .getSourceChatRoom().getParentProvider();

                historyString = processHistoryMessage(
                            GuiActivator.getUIService().getMainFrame()
                                .getAccount(protocolProvider),
                            evt.getTimestamp(),
                            Chat.HISTORY_OUTGOING_MESSAGE,
                            evt.getMessage().getContent(),
                            evt.getMessage().getContentType());
            }
            else if(o instanceof ChatRoomMessageReceivedEvent)
            {
                ChatRoomMessageReceivedEvent evt
                    = (ChatRoomMessageReceivedEvent) o;

                if(!evt.getMessage().getMessageUID()
                        .equals(escapedMessageID))
                {
                    historyString = processHistoryMessage(
                            evt.getSourceChatRoomMember().getName(),
                            evt.getTimestamp(),
                            Chat.HISTORY_INCOMING_MESSAGE,
                            evt.getMessage().getContent(),
                            evt.getMessage().getContentType());
                }
            }
            else if (o instanceof FileRecord)
            {
                FileRecord fileRecord = (FileRecord) o;

                FileHistoryConversationComponent component
                    = new FileHistoryConversationComponent(fileRecord);

                conversationPanel.addComponent(component);
            }

            if (historyString != null)
                conversationPanel.appendMessageToEnd(historyString);
        }

        getChatWindow().getMainToolBar()
            .changeHistoryButtonsState(this);
    }

    /**
     * Passes the message to the contained <code>ChatConversationPanel</code>
     * for processing and appends it at the end of the conversationPanel
     * document.
     *
     * @param contactName the name of the contact sending the message
     * @param date the time at which the message is sent or received
     * @param messageType the type of the message. One of OUTGOING_MESSAGE
     * or INCOMING_MESSAGE
     * @param message the message text
     * @param contentType the content type
     */
    public void addMessage(String contactName, long date,
            String messageType, String message, String contentType)
    {
        ChatMessage chatMessage = new ChatMessage(contactName, date,
            messageType, message, contentType);

        this.addChatMessage(chatMessage);
    }

    /**
     * Passes the message to the contained <code>ChatConversationPanel</code>
     * for processing and appends it at the end of the conversationPanel
     * document.
     *
     * @param contactName the name of the contact sending the message
     * @param date the time at which the message is sent or received
     * @param messageType the type of the message. One of OUTGOING_MESSAGE
     * or INCOMING_MESSAGE
     * @param title the title of the message
     * @param message the message text
     * @param contentType the content type
     */
    public void addMessage(String contactName, long date,
            String messageType, String title, String message, String contentType)
    {
        ChatMessage chatMessage = new ChatMessage(contactName, date,
            messageType, title, message, contentType);

        this.addChatMessage(chatMessage);
    }

    /**
     * Passes the message to the contained <code>ChatConversationPanel</code>
     * for processing and appends it at the end of the conversationPanel
     * document.
     * 
     * @param chatMessage the chat message to add
     */
    private void addChatMessage(ChatMessage chatMessage)
    {
        if (ConfigurationManager.isHistoryShown() && !isHistoryLoaded)
        {
            synchronized (incomingEventBuffer)
            {
                incomingEventBuffer.add(chatMessage);
            }
        }
        else
        {
            appendChatMessage(chatMessage);
        }

        // change the last history message timestamp after we add one.
        this.lastHistoryMsgTimestamp = chatMessage.getDate();
    }

    /**
     * Adds the given error message to the chat window conversation area.
     * 
     * @param contactName the name of the contact, for which the error occured
     * @param message the error message
     */
    public void addErrorMessage(String contactName,
                                String message)
    {
        this.addMessage(contactName,  System.currentTimeMillis(),
                Chat.ERROR_MESSAGE, 
                GuiActivator.getResources()
                    .getI18NString("service.gui.MSG_DELIVERY_FAILURE"),
                message, "text");
    }

    /**
     * Adds the given error message to the chat window conversation area.
     * 
     * @param contactName the name of the contact, for which the error occured
     * @param message the error message
     */
    public void addErrorMessage(String contactName,
                                String title,
                                String message)
    {
        this.addMessage(contactName,  System.currentTimeMillis(),
                Chat.ERROR_MESSAGE, 
                title,
                message, "text");
    }

    /**
     * Passes the message to the contained <code>ChatConversationPanel</code>
     * for processing and appends it at the end of the conversationPanel
     * document.
     * 
     * @param chatMessage the message to append
     */
    private void appendChatMessage(ChatMessage chatMessage)
    {
        String processedMessage
            = this.conversationPanel.processMessage(chatMessage);

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
     * @param contentType the content type of the message (html or plain text)
     *
     * @return a string containing the processed message.
     */
    private String processHistoryMessage(String contactName,
                                        long date,
                                        String messageType,
                                        String message,
                                        String contentType)
    {
        ChatMessage chatMessage = new ChatMessage(contactName, date,
            messageType, message, contentType);

        return this.conversationPanel.processMessage(chatMessage);
    }

    /**
     * Refreshes write area editor pane. Deletes all existing text
     * content.
     */
    public void refreshWriteArea()
    {
        this.writeMessagePanel.clearWriteArea();
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
    public String getTextFromWriteArea(String mimeType)
    {
        if (mimeType.equals(
            OperationSetBasicInstantMessaging.DEFAULT_MIME_TYPE))
        {
            return writeMessagePanel.getText();
        }
        else
        {
            return writeMessagePanel.getTextAsHtml();
        }
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
        JTextComponent textPane = this.conversationPanel.getChatTextPane();

        if (textPane.getSelectedText() == null)
        {
            textPane = this.writeMessagePanel.getEditorPane();
        }

        textPane.copy();
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
    public void paste()
    {
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
     * Brings the <tt>ChatWindow</tt> containing  this <tt>ChatPanel</tt> to front
     * if <tt>isVisble</tt> is true, hides it otherwise.
     * 
     * @param isVisible tells if the chat will shown or hidden
     */
    public void setChatVisible(boolean isVisible)
    {
        if (isVisible)
            GuiActivator.getUIService().getChatWindowManager()
                .openChat(this, isVisible);
        else
            GuiActivator.getUIService().getChatWindowManager()
                .closeChat(this);
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

        return (currentChatPanel != null
                && currentChatPanel.equals(this)
                && chatWindow.isActive());
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
     * Indicates if the history of a hidden protocol should be shown to the
     * user in the default <b>grey</b> history style or it should be shown as
     * a normal message.
     *
     * @param protocolProvider the protocol provider to check
     * @return <code>true</code> if the given protocol is a hidden one and the
     * "hiddenProtocolGreyHistoryDisabled" property is set to true.
     */
    private boolean isGreyHistoryStyleDisabled(
        ProtocolProviderService protocolProvider)
    {
        boolean isProtocolHidden =
            protocolProvider.getAccountID().getAccountPropertyBoolean(
                ProtocolProviderFactory.IS_PROTOCOL_HIDDEN, false);
        boolean isGreyHistoryDisabled = false;

        String greyHistoryProperty
            = GuiActivator.getResources()
                .getSettingsString("impl.gui.GREY_HISTORY_ENABLED");

        if (greyHistoryProperty != null)
            isGreyHistoryDisabled
                = Boolean.parseBoolean(greyHistoryProperty);

        return isProtocolHidden && isGreyHistoryDisabled;
    }

    /**
     * Sends the given file through the currently selected chat transport by
     * using the given fileComponent to visualize the transfer process in the
     * chat conversation panel.
     * 
     * @param file the file to send
     * @param fileComponent the file component to use for visualization
     */
    public void sendFile(   final File file,
                            final SendFileConversationComponent fileComponent)
    {
        final ChatTransport sendFileTransport
            = this.findFileTransferChatTransport();

        this.setSelectedChatTransport(sendFileTransport);

        SwingWorker worker = new SwingWorker()
        {
            public Object construct()
                throws Exception
            {
                if(file.length() > sendFileTransport.getMaximumFileLength())
                {
                    addMessage(
                        chatSession.getCurrentChatTransport().getName(),
                        System.currentTimeMillis(),
                        Chat.ERROR_MESSAGE,
                        GuiActivator.getResources()
                            .getI18NString("service.gui.FILE_TOO_BIG",
                            new String[]{
                                sendFileTransport.getMaximumFileLength()/1024/1024
                                + " MB"}),
                        "",
                        "text");
                    fileComponent.setFailed();

                    return "";
                }

                final FileTransfer fileTransfer
                    = sendFileTransport.sendFile(file);

                addActiveFileTransfer(fileTransfer.getID(), fileTransfer);

                // Add the status listener that would notify us when the file
                // transfer has been completed and should be removed from
                // active components.
                fileTransfer.addStatusListener(ChatPanel.this);

                SwingUtilities.invokeLater(new Runnable()
                {
                    public void run()
                    {
                        fileComponent.setProtocolFileTransfer(fileTransfer);
                    }
                });

                return "";
            }

            public void catchException(Throwable ex)
            {
                logger.error("Failed to send file.", ex);

                if (ex instanceof IllegalStateException)
                {
                    addErrorMessage(
                        chatSession.getCurrentChatTransport().getName(),
                        GuiActivator.getResources().getI18NString(
                            "service.gui.MSG_SEND_CONNECTION_PROBLEM"));
                }
                else
                {
                    addErrorMessage(
                        chatSession.getCurrentChatTransport().getName(),
                        GuiActivator.getResources().getI18NString(
                            "service.gui.MSG_DELIVERY_UNKNOWN_ERROR",
                            new String[]{ex.getMessage()}));
                }
            }
        };

        worker.start();
    }

    /**
     * Sends the given file through the currently selected chat transport.
     * 
     * @param file the file to send
     */
    public void sendFile(final File file)
    {
        final ChatTransport fileTransferTransport
            = findFileTransferChatTransport();

        // If there's no operation set we show some "not supported" messages
        // and we return.
        if (fileTransferTransport == null)
        {
            logger.error("Failed to send file.");

            this.addErrorMessage(
                chatSession.getChatName(),
                GuiActivator.getResources().getI18NString(
                    "service.gui.FILE_SEND_FAILED",
                    new String[]{file.getName()}),
                GuiActivator.getResources().getI18NString(
                    "service.gui.FILE_TRANSFER_NOT_SUPPORTED"));

            return;
        }

        final SendFileConversationComponent fileComponent
            = new SendFileConversationComponent(
                this,
                fileTransferTransport.getDisplayName(),
                file);

        if (ConfigurationManager.isHistoryShown() && !isHistoryLoaded)
        {
            synchronized (incomingEventBuffer)
            {
                incomingEventBuffer.add(fileComponent);
            }
        }
        else
            getChatConversationPanel().addComponent(fileComponent);

        this.sendFile(file, fileComponent);
    }

    /**
     * Sends the text contained in the write area as an SMS message or an
     * instance message depending on the "send SMS" check box.
     */
    protected void sendMessage()
    {
        if (sendSmsCheckBox.isSelected())
        {
            this.sendSmsMessage();
        }
        else
        {
            this.sendInstantMessage();
        }
    }

    /**
     * Sends the text contained in the write area as an SMS message.
     */
    public void sendSmsMessage()
    {
        String messageText = getTextFromWriteArea(
            OperationSetBasicInstantMessaging.DEFAULT_MIME_TYPE);

        ChatTransport smsChatTransport = chatSession.getCurrentChatTransport();
        if (!smsChatTransport.allowsSmsMessage())
        {
            Iterator<ChatTransport> chatTransports
                = chatSession.getChatTransports();

            while(chatTransports.hasNext())
            {
                ChatTransport transport = chatTransports.next();

                if (transport.allowsSmsMessage())
                {
                    smsChatTransport = transport;
                    break;
                }
            }
        }

        // If there's no operation set we show some "not supported" messages
        // and we return.
        if (!smsChatTransport.allowsSmsMessage())
        {
            logger.error("Failed to send SMS.");

            this.refreshWriteArea();

            this.addMessage(
                smsChatTransport.getName(),
                System.currentTimeMillis(),
                Chat.OUTGOING_MESSAGE,
                messageText,
                "plain/text");

            this.addErrorMessage(
                smsChatTransport.getName(),
                GuiActivator.getResources().getI18NString(
                    "service.gui.SEND_SMS_NOT_SUPPORTED"));

            return;
        }

        smsChatTransport.addSmsMessageListener(
                new SmsMessageListener(smsChatTransport));

        // We open the send SMS dialog.
        SendSmsDialog smsDialog
            = new SendSmsDialog(this, smsChatTransport, messageText);

        smsDialog.setPreferredSize(new Dimension(400, 200));
        smsDialog.setVisible(true);
    }

    /**
     * Implements the <tt>ChatPanel.sendMessage</tt> method. Obtains the
     * appropriate operation set and sends the message, contained in the write
     * area, through it.
     */
    protected void sendInstantMessage()
    {
        String htmlText = getTextFromWriteArea("text/html");
        String plainText = getTextFromWriteArea(
            OperationSetBasicInstantMessaging.DEFAULT_MIME_TYPE);

        String messageText;
        String mimeType;
        if (    (htmlText.indexOf("<b") > -1
                || htmlText.indexOf("<i") > -1
                || htmlText.indexOf("<u") > -1
                || htmlText.indexOf("<font") > -1))
        {
            messageText = htmlText;
            mimeType = OperationSetBasicInstantMessaging.HTML_MIME_TYPE;
        }
        else
        {
            messageText = plainText;
            mimeType = OperationSetBasicInstantMessaging.DEFAULT_MIME_TYPE;
        }

        try
        {
            chatSession.getCurrentChatTransport()
                .sendInstantMessage(messageText, mimeType);
        }
        catch (IllegalStateException ex)
        {
            logger.error("Failed to send message.", ex);

            this.addMessage(
                chatSession.getCurrentChatTransport().getName(),
                System.currentTimeMillis(),
                Chat.OUTGOING_MESSAGE,
                messageText,
                mimeType);

            this.addErrorMessage(
                chatSession.getCurrentChatTransport().getName(),
                GuiActivator.getResources().getI18NString(
                    "service.gui.MSG_SEND_CONNECTION_PROBLEM"));
        }
        catch (Exception ex)
        {
            logger.error("Failed to send message.", ex);

            this.refreshWriteArea();

            this.addMessage(
                chatSession.getCurrentChatTransport().getName(),
                System.currentTimeMillis(),
                Chat.OUTGOING_MESSAGE,
                messageText,
                mimeType);

            this.addErrorMessage(
                chatSession.getCurrentChatTransport().getName(),
                GuiActivator.getResources().getI18NString(
                    "service.gui.MSG_DELIVERY_UNKNOWN_ERROR",
                    new String[]{ex.getMessage()}));
        }

        if (chatSession.getCurrentChatTransport().allowsTypingNotifications())
        {
            // Send TYPING STOPPED event before sending the message
            getChatWritePanel().stopTypingTimer();
        }

        this.refreshWriteArea();
    }

    /**
     * Initializes the send via label and selector box.
     */
    private void initChatTransportSelectorBox()
    {
        // Initialize the "send via" selector box and adds it to the send panel.
        if (transportSelectorBox == null)
        {
            transportSelectorBox = new ChatTransportSelectorBox(
                this, chatSession, chatSession.getCurrentChatTransport());

            sendViaLabel = new JLabel(
                GuiActivator.getResources().getI18NString(
                    "service.gui.SEND_VIA"));
        }

        JPanel sendPanel = getChatSendPanel().getSendPanel();
        sendPanel.add(transportSelectorBox, 0);
        sendPanel.add(sendViaLabel, 0);

        updateSendButtonStatus();

        this.revalidate();
        this.repaint();
    }

    /**
     * Sets the send button to the same state (enabled/ disabled) as the
    * transportSelectorBox.
    */
    private void updateSendButtonStatus()
    {
        getChatSendPanel().getSendButton().
                setEnabled(transportSelectorBox.getMenu().isEnabled());
    }

    /**
     * Removes the send via selector box and label.
     */
    private void removeChatTransportSelectorBox()
    {
        if (transportSelectorBox == null)
            return;

        JPanel sendPanel = getChatSendPanel().getSendPanel();

        sendPanel.remove(transportSelectorBox);
        sendPanel.remove(sendViaLabel);

        this.revalidate();
        this.repaint();
    }

    /**
     * Listens for SMS messages and shows them in the chat.
     */
    private class SmsMessageListener implements MessageListener
    {
        /**
         * @param chatTransport Currently unused 
         */
        public SmsMessageListener(ChatTransport chatTransport)
        {
        }

        public void messageDelivered(MessageDeliveredEvent evt)
        {
            Message msg = evt.getSourceMessage();

            Contact contact = evt.getDestinationContact();

            addMessage(
                contact.getDisplayName(),
                System.currentTimeMillis(),
                Chat.OUTGOING_MESSAGE,
                msg.getContent(), msg.getContentType());

            addMessage(
                    contact.getDisplayName(),
                    System.currentTimeMillis(),
                    Chat.ACTION_MESSAGE,
                    GuiActivator.getResources().getI18NString(
                        "service.gui.SMS_SUCCESSFULLY_SENT"),
                    "text");
        }

        public void messageDeliveryFailed(MessageDeliveryFailedEvent evt)
        {
            logger.error(evt.getReason());

            String errorMsg = null;

            Message sourceMessage = (Message) evt.getSource();

            Contact sourceContact = evt.getDestinationContact();

            MetaContact metaContact = GuiActivator.getUIService().getMainFrame()
                .getContactList().findMetaContactByContact(sourceContact);

            if (evt.getErrorCode()
                    == MessageDeliveryFailedEvent.OFFLINE_MESSAGES_NOT_SUPPORTED)
            {
                errorMsg = GuiActivator.getResources().getI18NString(
                    "service.gui.MSG_DELIVERY_NOT_SUPPORTED");
            }
            else if (evt.getErrorCode()
                    == MessageDeliveryFailedEvent.NETWORK_FAILURE)
            {
                errorMsg = GuiActivator.getResources().getI18NString(
                    "service.gui.MSG_NOT_DELIVERED");
            }
            else if (evt.getErrorCode()
                    == MessageDeliveryFailedEvent.PROVIDER_NOT_REGISTERED)
            {
                errorMsg = GuiActivator.getResources().getI18NString(
                    "service.gui.MSG_SEND_CONNECTION_PROBLEM");
            }
            else if (evt.getErrorCode()
                    == MessageDeliveryFailedEvent.INTERNAL_ERROR)
            {
                errorMsg = GuiActivator.getResources().getI18NString(
                    "service.gui.MSG_DELIVERY_INTERNAL_ERROR");
            }
            else {
                errorMsg = GuiActivator.getResources().getI18NString(
                    "service.gui.MSG_DELIVERY_UNKNOWN_ERROR");
            }

            addMessage(
                    metaContact.getDisplayName(),
                    System.currentTimeMillis(),
                    Chat.OUTGOING_MESSAGE,
                    sourceMessage.getContent(),
                    sourceMessage.getContentType());

            addErrorMessage(
                    metaContact.getDisplayName(),
                    errorMsg);
        }

        public void messageReceived(MessageReceivedEvent evt)
        {}
    }

    /**
     * Returns the date of the first message in history for this chat.
     *
     * @return the date of the first message in history for this chat.
     */
    public long getFirstHistoryMsgTimestamp()
    {
        return firstHistoryMsgTimestamp;
    }

    /**
     * Returns the date of the last message in history for this chat.
     *
     * @return the date of the last message in history for this chat.
     */
    public long getLastHistoryMsgTimestamp()
    {
        return lastHistoryMsgTimestamp;
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
        SwingWorker historyWorker = new SwingWorker()
        {
            private Collection<Object> historyList;

            public Object construct() throws Exception
            {
                // Load the history period, which initializes the
                // firstMessageTimestamp and the lastMessageTimeStamp variables.
                // Used to disable/enable history flash buttons in the chat
                // window tool bar.
                loadHistoryPeriod();

                // Load the last N=CHAT_HISTORY_SIZE messages from history.
                historyList = chatSession.getHistory(
                    ConfigurationManager.getChatHistorySize());

                return historyList;
            }

            /**
             * Called on the event dispatching thread (not on the worker thread)
             * after the <code>construct</code> method has returned.
             */
            public void finished()
            {
                if(historyList != null && historyList.size() > 0)
                {
                    processHistory(historyList, escapedMessageID);
                }
                isHistoryLoaded = true;

                // Add incoming events accumulated while the history was loading
                // at the end of the chat.
                addIncomingEvents();
            }
        };

        historyWorker.start();
    }

    /**
     * Loads history for the chat meta contact in a separate thread. Implements
     * the <tt>ChatPanel.loadHistory</tt> method.
     */
    public void loadHistory()
    {
        this.loadHistory(null);
    }

    /**
     * Loads history period dates for the current chat.
     */
    private void loadHistoryPeriod()
    {
        this.firstHistoryMsgTimestamp = chatSession.getHistoryStartDate();

        this.lastHistoryMsgTimestamp = chatSession.getHistoryEndDate();
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

    /**
     * The <tt>ChangeProtocolAction</tt> is an <tt>AbstractAction</tt> that
     * opens the menu, containing all available protocol contacts.
     */
    private class ChangeTransportAction
        extends AbstractAction
    {
        public void actionPerformed(ActionEvent e)
        {
            /*
             * Opens the selector box containing the protocol contact icons.
             * This is the menu, where user could select the protocol specific
             * contact to communicate through.
             */
            transportSelectorBox.getMenu().doClick();
        }
    }

    /**
     * Renames all occurrences of the given <tt>chatContact</tt> in this chat
     * panel.
     * 
     * @param chatContact the contact to rename
     * @param name the new name
     */
    public void setContactName(ChatContact chatContact, String name)
    {
        if (chatContactListPanel != null)
        {
            chatContactListPanel.renameContact(chatContact);
        }

        ChatWindow chatWindow = getChatWindow();
        chatWindow.setTabTitle(this, name);

        if (chatWindow.getCurrentChatPanel() == this)
        {
            chatWindow.setTitle(name);
        }
    }

    /**
     * Adds the given chatTransport to the given send via selector box.
     * 
     * @param chatTransport the transport to add
     */
    public void addChatTransport(ChatTransport chatTransport)
    {
        if (transportSelectorBox != null)
            transportSelectorBox.addChatTransport(chatTransport);
        updateSendButtonStatus();
    }

    /**
     * Removes the given chat status state from the send via selector box.
     * 
     * @param chatTransport the transport to remove
     */
    public void removeChatTransport(ChatTransport chatTransport)
    {
        if (transportSelectorBox != null)
            transportSelectorBox.removeChatTransport(chatTransport);
        updateSendButtonStatus();
    }

    /**
     * Selects the given chat transport in the send via box.
     * 
     * @param chatTransport the chat transport to be selected
     */
    public void setSelectedChatTransport(ChatTransport chatTransport)
    {
        if (transportSelectorBox != null)
            transportSelectorBox.setSelected(chatTransport);
    }

    /**
     * Updates the status of the given chat transport in the send via selector
     * box and notifies the user for the status change.
     */
    public void updateChatTransportStatus(ChatTransport chatTransport)
    {
        if (transportSelectorBox != null)
            transportSelectorBox.updateTransportStatus(chatTransport);

        // Show a status message to the user.
        this.addMessage(
            chatTransport.getName(),
            System.currentTimeMillis(),
            Chat.STATUS_MESSAGE,
            GuiActivator.getResources().getI18NString(
                "service.gui.STATUS_CHANGED_CHAT_MESSAGE",
                new String[]{chatTransport.getStatus().getStatusName()}),
                "text/plain");

        if(ConfigurationManager.isMultiChatWindowEnabled())
        {
            if (getChatWindow().getChatTabCount() > 0) {
                getChatWindow().setTabIcon(this,
                    new ImageIcon(
                        Constants.getStatusIcon(chatTransport.getStatus())));
            }
        }
    }

    /**
     * Implements <tt>ChatPanel.loadPreviousFromHistory</tt>.
     * Loads previous page from history.
     */
    public void loadPreviousPageFromHistory()
    {
        final MetaHistoryService chatHistory
            = GuiActivator.getMetaHistoryService();

        // If the MetaHistoryService is not registered we have nothing to do
        // here. The history service could be "disabled" from the user
        // through one of the configuration forms.
        if (chatHistory == null)
            return;

        new Thread()
        {
            public void run()
            {
                ChatConversationPanel conversationPanel
                    = getChatConversationPanel();

                Date firstMsgDate
                    = conversationPanel.getPageFirstMsgTimestamp();

                Collection<Object> c = null;

                if(firstMsgDate != null)
                {
                    c = chatSession.getHistoryBeforeDate(
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
        final MetaHistoryService chatHistory
            = GuiActivator.getMetaHistoryService();

        // If the MetaHistoryService is not registered we have nothing to do
        // here. The history could be "disabled" from the user
        // through one of the configuration forms.
        if (chatHistory == null)
            return;

        new Thread()
        {
            public void run()
            {
                Date lastMsgDate
                    = getChatConversationPanel().getPageLastMsgTimestamp();

                Collection<Object> c = null;
                if(lastMsgDate != null)
                {
                    c = chatSession.getHistoryAfterDate(
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
        private final Collection<Object> chatHistory;

        public HistoryMessagesLoader(Collection<Object> history)
        {
            this.chatHistory = history;
        }

        public void run()
        {
            getChatConversationPanel().clear();

            processHistory(chatHistory, "");

            getChatConversationPanel().setDefaultContent();
        }
    }

    /**
     * Adds the given <tt>chatContact</tt> to the list of chat contacts
     * participating in the corresponding to this chat panel chat.
     * @param chatContact the contact to add
     */
    public void addChatContact(ChatContact chatContact)
    {
        if (chatContactListPanel != null)
            chatContactListPanel.addContact(chatContact);
    }

    /**
     * Removes the given <tt>chatContact</tt> from the list of chat contacts
     * participating in the corresponding to this chat panel chat.
     * @param chatContact the contact to remove
     */
    public void removeChatContact(ChatContact chatContact)
    {
        if (chatContactListPanel != null)
            chatContactListPanel.removeContact(chatContact);
    }

    public void updateChatContactStatus(ChatContact chatContact,
        String statusMessage)
    {
        this.addMessage(
            chatContact.getName(),
            System.currentTimeMillis(),
            Chat.STATUS_MESSAGE,
            statusMessage,
            ChatConversationPanel.TEXT_CONTENT_TYPE);
    }

    public void setChatSubject(String subject)
    {
        if (subjectPanel != null)
        {
            subjectPanel.setSubject(subject);

            this.addMessage(
                chatSession.getChatName(),
                System.currentTimeMillis(),
                Chat.STATUS_MESSAGE,
                GuiActivator.getResources().getI18NString(
                    "service.gui.CHAT_ROOM_SUBJECT_CHANGED",
                    new String []{  chatSession.getChatName(),
                                    subject}),
                ChatConversationPanel.TEXT_CONTENT_TYPE);
        }
    }

    /**
     * Adds the given <tt>IncomingFileTransferRequest</tt> to the conversation
     * panel in order to notify the user of the incoming file.
     * 
     * @param request the request to display in the conversation panel
     * @param date the date on which the request has been received
     */
    public void addIncomingFileTransferRequest(
        OperationSetFileTransfer fileTransferOpSet,
        IncomingFileTransferRequest request,
        Date date)
    {
        this.addActiveFileTransfer(request.getID(), request);

        ReceiveFileConversationComponent component
            = new ReceiveFileConversationComponent(
                this, fileTransferOpSet, request, date);

        if (ConfigurationManager.isHistoryShown() && !isHistoryLoaded)
        {
            synchronized (incomingEventBuffer)
            {
                incomingEventBuffer.add(component);
            }
        }
        else
            this.getChatConversationPanel().addComponent(component);
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
            if (!focusListeners.contains(listener))
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
     * Returns the first chat transport for the current chat session that
     * supports file transfer.
     *
     * @return the first chat transport for the current chat session that
     * supports file transfer.
     */
    public ChatTransport findFileTransferChatTransport()
    {
        ChatTransport currentChatTransport
            = chatSession.getCurrentChatTransport();

        if (currentChatTransport.getProtocolProvider()
                .getOperationSet(OperationSetFileTransfer.class) != null)
        {
            return currentChatTransport;
        }
        else
        {
            Iterator<ChatTransport> chatTransportsIter
                = chatSession.getChatTransports();

            while (chatTransportsIter.hasNext())
            {
                ChatTransport chatTransport = chatTransportsIter.next();

                Object fileTransferOpSet
                    = chatTransport.getProtocolProvider()
                        .getOperationSet(OperationSetFileTransfer.class);

                if (fileTransferOpSet != null)
                    return chatTransport;
            }
        }

        return null;
    }

    /**
     * Returns the first chat transport for the current chat session that
     * supports group chat.
     *
     * @return the first chat transport for the current chat session that
     * supports group chat.
     */
    public ChatTransport findInviteChatTransport()
    {
        ChatTransport currentChatTransport
            = chatSession.getCurrentChatTransport();

        ProtocolProviderService protocolProvider
            = currentChatTransport.getProtocolProvider();

        // We choose between OpSets for multi user chat...
        if (protocolProvider.getOperationSet(
            OperationSetMultiUserChat.class) != null
            || protocolProvider.getOperationSet(
                OperationSetAdHocMultiUserChat.class) != null)
        {
            return chatSession.getCurrentChatTransport();
        }

        else
        {
            Iterator<ChatTransport> chatTransportsIter
                = chatSession.getChatTransports();

            while (chatTransportsIter.hasNext())
            {
                ChatTransport chatTransport = chatTransportsIter.next();

                Object groupChatOpSet
                    = chatTransport.getProtocolProvider()
                        .getOperationSet(OperationSetMultiUserChat.class);

                if (groupChatOpSet != null)
                    return chatTransport;
            }
        }

        return null;
    }

    public void inviteContacts( ChatTransport inviteChatTransport,
                                Collection<String> chatContacts,
                                String reason)
    {
        ChatSession conferenceChatSession = null;

        if (chatSession instanceof MetaContactChatSession)
        {
            String newChatName = inviteChatTransport.getDisplayName();

            chatContacts.add(inviteChatTransport.getName());

            ConferenceChatManager conferenceChatManager
                = GuiActivator.getUIService().getConferenceChatManager();

            // the chat session is set regarding to which OpSet is used for MUC
            if(inviteChatTransport.getProtocolProvider().
                    getOperationSet(OperationSetMultiUserChat.class) != null)
            {
                ChatRoomWrapper chatRoomWrapper
                    = conferenceChatManager.createChatRoom(newChatName,
                        inviteChatTransport.getProtocolProvider(), chatContacts);

                conferenceChatSession
                    = new ConferenceChatSession(this, chatRoomWrapper);
            }
            else if (inviteChatTransport.getProtocolProvider().
                getOperationSet(OperationSetAdHocMultiUserChat.class) != null)
            {
                AdHocChatRoomWrapper chatRoomWrapper
                    = conferenceChatManager.createAdHocChatRoom(newChatName,
                            inviteChatTransport.getProtocolProvider(),
                            chatContacts);

                conferenceChatSession
                    = new AdHocConferenceChatSession(this, chatRoomWrapper);
            }

            if (conferenceChatSession != null)
                this.setChatSession(conferenceChatSession);
        }
        // We're already in a conference chat.
        else
        {
            conferenceChatSession = chatSession;

            for (String contactAddress : chatContacts)
            {
                conferenceChatSession.getCurrentChatTransport()
                    .inviteChatContact(contactAddress, reason);
            }
        }
    }

    /**
     * Informs all <tt>ChatFocusListener</tt>s that a <tt>ChatFocusEvent</tt>
     * has been triggered.
     *
     * @param eventID the type of the <tt>ChatFocusEvent</tt>
     */
    public void fireChatFocusEvent(int eventID)
    {
        ChatFocusEvent evt = new ChatFocusEvent(this, eventID);

        logger.trace("Will dispatch the following chat event: " + evt);

        Iterable<ChatFocusListener> listeners;
        synchronized (focusListeners)
        {
            listeners = new ArrayList<ChatFocusListener>(focusListeners);
        }

        for (ChatFocusListener listener : listeners)
        {
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

    /**
     * Handles file transfer status changed in order to remove completed file
     * transfers from the list of active transfers.
     */
    public void statusChanged(FileTransferStatusChangeEvent event)
    {
        FileTransfer fileTransfer = event.getFileTransfer();

        int newStatus = event.getNewStatus();

        if (newStatus == FileTransferStatusChangeEvent.COMPLETED
            || newStatus == FileTransferStatusChangeEvent.CANCELED
            || newStatus == FileTransferStatusChangeEvent.FAILED
            || newStatus == FileTransferStatusChangeEvent.REFUSED)
        {
            removeActiveFileTransfer(fileTransfer.getID());
            fileTransfer.removeStatusListener(this);
        }
    }

    /**
     * Returns <code>true</code> if there are active file transfers, otherwise
     * returns <code>false</code>.
     * @return <code>true</code> if there are active file transfers, otherwise
     * returns <code>false</code>
     */
    public boolean containsActiveFileTransfers()
    {
        return !activeFileTransfers.isEmpty();
    }

    /**
     * Returns the number of messages received but not yet read from the user.
     * 
     * @return the number of messages received but not yet read from the user.
     */
    public int getUnreadMessageNumber()
    {
        return unreadMessageNumber;
    }

    /**
     * Cancels all active file transfers.
     */
    public void cancelActiveFileTransfers()
    {
        Enumeration<String> activeKeys = activeFileTransfers.keys();

        while (activeKeys.hasMoreElements())
        {
            String key = activeKeys.nextElement();
            Object descriptor = activeFileTransfers.get(key);

            if (descriptor instanceof IncomingFileTransferRequest)
            {
                ((IncomingFileTransferRequest) descriptor).rejectFile();
            }
            else if (descriptor instanceof FileTransfer)
            {
                ((FileTransfer) descriptor).cancel();
            }
        }
    }

    /**
     * Stores the current divider position.
     */
    private class DividerLocationListener implements PropertyChangeListener
    {
        public void propertyChange(PropertyChangeEvent evt)
        {
            if (evt.getPropertyName()
                    .equals(JSplitPane.DIVIDER_LOCATION_PROPERTY))
            {
                int dividerLocation = (Integer) evt.getNewValue();
                int writeAreaSize = messagePane.getHeight() - dividerLocation
                                    - messagePane.getDividerSize();

                ConfigurationManager
                    .setChatWriteAreaSize(writeAreaSize);
            }
        }
    }

    /**
     * Adds all events accumulated in the incoming event buffer to the
     * chat conversation panel.
     */
    private void addIncomingEvents()
    {
        synchronized (incomingEventBuffer)
        {
            Iterator<Object> eventBufferIter = incomingEventBuffer.iterator();

            while(eventBufferIter.hasNext())
            {
                Object incomingEvent = eventBufferIter.next();

                if (incomingEvent instanceof ChatMessage)
                {
                    this.appendChatMessage((ChatMessage) incomingEvent);
                }
                else if (incomingEvent instanceof ChatConversationComponent)
                {
                    this.getChatConversationPanel()
                        .addComponent((ChatConversationComponent)incomingEvent);
                }
            }
        }
    }

    /**
     * Adds the given file transfer <tt>id</tt> to the list of active file
     * transfers.
     * 
     * @param id the identifier of the file transfer to add
     */
    public void addActiveFileTransfer(String id, Object descriptor)
    {
        synchronized (activeFileTransfers)
        {
            activeFileTransfers.put(id, descriptor);
        }
    }

    /**
     * Removes the given file transfer <tt>id</tt> from the list of active
     * file transfers.
     * @param id the identifier of the file transfer to remove
     */
    public void removeActiveFileTransfer(String id)
    {
        synchronized (activeFileTransfers)
        {
            activeFileTransfers.remove(id);
        }
    }
}
