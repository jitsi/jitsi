/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.sip.communicator.impl.gui.main.chat;

import java.awt.*;
import java.awt.Container;
import java.awt.event.*;
import java.beans.*;
import java.io.*;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.plaf.basic.*;
import javax.swing.text.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.chat.conference.*;
import net.java.sip.communicator.impl.gui.main.chat.filetransfer.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.plugin.desktoputil.SwingWorker;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.filehistory.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.event.*;
import net.java.sip.communicator.service.metahistory.*;
import net.java.sip.communicator.service.muc.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.Logger;
import net.java.sip.communicator.util.skin.*;

import org.apache.commons.lang3.*;
import org.jitsi.util.*;

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
 * @author Lyubomir Marinov
 * @author Adam Netocny
 * @author Hristo Terezov
 */
@SuppressWarnings("serial")
public class ChatPanel
    extends TransparentPanel
    implements  ChatSessionRenderer,
                ChatSessionChangeListener,
                Chat,
                ChatConversationContainer,
                ChatRoomMemberRoleListener,
                ChatRoomLocalUserRoleListener,
                ChatRoomMemberPropertyChangeListener,
                FileTransferStatusListener,
                Skinnable
{
    /**
     * The <tt>Logger</tt> used by the <tt>CallPanel</tt> class and its
     * instances for logging output.
     */
    private static final Logger logger = Logger.getLogger(ChatPanel.class);

    private final JSplitPane messagePane
        = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

    private JSplitPane topSplitPane;

    private final JPanel topPanel = new JPanel(new BorderLayout());

    private final ChatConversationPanel conversationPanel;

    /**
     * Will contain the typing panel on south and centered the
     * conversation panel.
     */
    private final JPanel conversationPanelContainer
        = new JPanel(new BorderLayout());

    private final ChatWritePanel writeMessagePanel;

    private ChatRoomMemberListPanel chatContactListPanel;
    
    private TransparentPanel conferencePanel 
        = new TransparentPanel(new BorderLayout());

    private final ChatContainer chatContainer;

    private ChatRoomSubjectPanel subjectPanel;

    public int unreadMessageNumber = 0;

    /**
     * The label showing current typing notification.
     */
    private JLabel typingNotificationLabel;

    /**
     * The typing notification icon.
     */
    private final Icon typingIcon = GuiActivator.getResources()
        .getImage("service.gui.icons.TYPING");

    /**
     * Indicates that a typing notification event is successfully sent.
     */
    public static final int TYPING_NOTIFICATION_SUCCESSFULLY_SENT = 1;

    /**
     * Indicates that sending a typing notification event has failed.
     */
    public static final int TYPING_NOTIFICATION_SEND_FAILED = 0;

    /**
     * The number of messages shown per page.
     */
    protected static final int MESSAGES_PER_PAGE = 20;

    private boolean isShown = false;

    private ChatSession chatSession;

    private Date firstHistoryMsgTimestamp = new Date(0);

    private Date lastHistoryMsgTimestamp = new Date(0);

    private final List<ChatFocusListener> focusListeners
        = new Vector<ChatFocusListener>();

    private final List<ChatHistoryListener> historyListeners
        = new Vector<ChatHistoryListener>();

    private final Vector<Object> incomingEventBuffer = new Vector<Object>();

    private boolean isHistoryLoaded;

    /**
     * Stores all active  file transfer requests and effective transfers with
     * the identifier of the transfer.
     */
    private final Hashtable<String, Object> activeFileTransfers
        = new Hashtable<String, Object>();

    /**
     * The ID of the message being corrected, or <tt>null</tt> if
     * not correcting any message.
     */
    private String correctedMessageUID = null;

    /**
     * The ID of the last sent message in this chat.
     */
    private String lastSentMessageUID = null;
    
    /**
     * Indicates whether the chat is private messaging chat or not.
     */
    private boolean isPrivateMessagingChat = false;

    /**
     * Dialog used to join or create chat conference call.
     */
    protected ChatConferenceCallDialog chatConferencesDialog = null;

    /**
     * Whether to use all numbers when sending sms, or just the mobiles.
     */
    private static final String USE_ADDITIONAL_NUMBERS_PROP
        = "service.gui.IS_SEND_SMS_USING_ADDITIONAL_NUMBERS";

    /**
     * Creates a <tt>ChatPanel</tt> which is added to the given chat window.
     *
     * @param chatContainer The parent window of this chat panel.
     */
    public ChatPanel(ChatContainer chatContainer)
    {
        super(new BorderLayout());

        this.chatContainer = chatContainer;

        this.conversationPanel = new ChatConversationPanel(this);
        this.conversationPanel.setPreferredSize(new Dimension(400, 200));
        this.conversationPanel.getChatTextPane()
            .setTransferHandler(new ChatTransferHandler(this));

        this.conversationPanelContainer.add(
            conversationPanel, BorderLayout.CENTER);
        this.conversationPanelContainer.setBackground(Color.WHITE);
        initTypingNotificationLabel(conversationPanelContainer);

        topPanel.setBackground(Color.WHITE);
        topPanel.setBorder(
            BorderFactory.createMatteBorder(1, 0, 1, 0, Color.GRAY));

        this.writeMessagePanel = new ChatWritePanel(this);

        this.messagePane.setBorder(null);
        this.messagePane.setOpaque(false);
        this.messagePane.addPropertyChangeListener(
            new DividerLocationListener());

        this.messagePane.setDividerSize(3);
        this.messagePane.setResizeWeight(1.0D);
        this.messagePane.setBottomComponent(writeMessagePanel);

        this.messagePane.setTopComponent(topPanel);

        this.add(messagePane, BorderLayout.CENTER);

        if (OSUtils.IS_MAC)
        {
            setOpaque(true);
            setBackground(
                new Color(GuiActivator.getResources()
                    .getColor("service.gui.MAC_PANEL_BACKGROUND")));
        }

        this.addComponentListener(new TabSelectionComponentListener());
    }

    /**
     * Sets the chat session to associate to this chat panel.
     * @param chatSession the chat session to associate to this chat panel
     */
    public void setChatSession(ChatSession chatSession)
    {
        if(this.chatSession != null)
        {
            // remove old listener
            this.chatSession.removeChatTransportChangeListener(this);
        }

        this.chatSession = chatSession;
        this.chatSession.addChatTransportChangeListener(this);

        if ((this.chatSession != null)
                && this.chatSession.isContactListSupported())
        {
            topPanel.remove(conversationPanelContainer);

            TransparentPanel rightPanel
                = new TransparentPanel(new BorderLayout());
            Dimension chatConferencesListsPanelSize = new Dimension(150, 25);
            Dimension chatContactsListsPanelSize = new Dimension(150, 175);
            Dimension rightPanelSize = new Dimension(150, 200);
            rightPanel.setMinimumSize(rightPanelSize);
            rightPanel.setPreferredSize(rightPanelSize);

            TransparentPanel contactsPanel
                = new TransparentPanel(new BorderLayout());
            contactsPanel.setMinimumSize(chatContactsListsPanelSize);
            contactsPanel.setPreferredSize(chatContactsListsPanelSize);
            
            conferencePanel.setMinimumSize(chatConferencesListsPanelSize);
            conferencePanel.setPreferredSize(chatConferencesListsPanelSize);
            
            this.chatContactListPanel = new ChatRoomMemberListPanel(this);
            this.chatContactListPanel.setOpaque(false);
            
            topSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
            topSplitPane.setBorder(null); // remove default borders
            topSplitPane.setOneTouchExpandable(true);
            topSplitPane.setOpaque(false);
            topSplitPane.setResizeWeight(1.0D);

            Color msgNameBackground =
                Color.decode(ChatHtmlUtils.MSG_NAME_BACKGROUND);

            // add border to the divider
            if(topSplitPane.getUI() instanceof BasicSplitPaneUI)
            {
                ((BasicSplitPaneUI)topSplitPane.getUI()).getDivider()
                    .setBorder(
                        BorderFactory.createLineBorder(msgNameBackground));
            }

            ChatTransport chatTransport = chatSession.getCurrentChatTransport();

            JPanel localUserLabelPanel = new JPanel(new BorderLayout());
            JLabel localUserLabel = new JLabel(
                chatTransport.getProtocolProvider()
                    .getAccountID().getDisplayName());

            localUserLabel.setFont(
                localUserLabel.getFont().deriveFont(Font.BOLD));
            localUserLabel.setHorizontalAlignment(SwingConstants.CENTER);
            localUserLabel.setBorder(
                            BorderFactory.createEmptyBorder(2, 0, 3, 0));
            localUserLabel.setForeground(
                Color.decode(ChatHtmlUtils.MSG_IN_NAME_FOREGROUND));

            localUserLabelPanel.add(localUserLabel, BorderLayout.CENTER);
            localUserLabelPanel.setBackground(msgNameBackground);
            
            
            JButton joinConference = new JButton(GuiActivator.getResources()
                .getI18NString("service.gui.JOIN_VIDEO"));
            
            joinConference.addActionListener(new ActionListener()
            {
                @Override
                public void actionPerformed(ActionEvent arg0)
                {
                    showChatConferenceDialog();
                }
            });
            contactsPanel.add(localUserLabelPanel, BorderLayout.NORTH);
            contactsPanel.add(chatContactListPanel, BorderLayout.CENTER);
            
            conferencePanel.add(joinConference, BorderLayout.CENTER);
            
            rightPanel.add(conferencePanel, BorderLayout.NORTH);
            rightPanel.add(contactsPanel, BorderLayout.CENTER);
            
            topSplitPane.setLeftComponent(conversationPanelContainer);
            topSplitPane.setRightComponent(rightPanel);
            
            topPanel.add(topSplitPane);

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

            topPanel.add(conversationPanelContainer);
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

            writeMessagePanel.initPluginComponents();
            writeMessagePanel.setTransportSelectorBoxVisible(true);

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
            ConferenceChatSession confSession
                = (ConferenceChatSession) chatSession;

            writeMessagePanel.setTransportSelectorBoxVisible(false);

            confSession.addLocalUserRoleListener(this);
            confSession.addMemberRoleListener(this);

            ChatRoom room 
                = ((ChatRoomWrapper) chatSession.getDescriptor()).getChatRoom();
            room.addMemberPropertyChangeListener(this);

            setConferencesPanelVisible(
                room.getCachedConferenceDescriptionSize() > 0);
            subjectPanel
                = new ChatRoomSubjectPanel((ConferenceChatSession) chatSession);

            // The subject panel is added here, because it's specific for the
            // multi user chat and is not contained in the single chat chat panel.
            this.add(subjectPanel, BorderLayout.NORTH);

            this.revalidate();
            this.repaint();
        }

        if (chatContactListPanel != null)
        {
            // Initialize chat participants' panel.
            Iterator<ChatContact<?>> chatParticipants
                = chatSession.getParticipants();

            while (chatParticipants.hasNext())
                chatContactListPanel.addContact(chatParticipants.next());
        }
    }

    
    public void showChatConferenceDialog()
    {
        if(chatConferencesDialog  == null)
        {
            chatConferencesDialog 
                = new ChatConferenceCallDialog(ChatPanel.this);
            chatConferencesDialog.initConferences();
        }
        
        chatConferencesDialog.setVisible(true);
        
        chatConferencesDialog.toFront();
        chatConferencesDialog.pack();
    }
    
    /**
     * Returns the chat session associated with this chat panel.
     * @return the chat session associated with this chat panel
     */
    public ChatSession getChatSession()
    {
        return chatSession;
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
        conversationPanel.dispose();

        if (chatSession instanceof ConferenceChatSession)
        {
            if(chatConferencesDialog != null)
                chatConferencesDialog.dispose();
            ConferenceChatSession confSession
                            = (ConferenceChatSession) chatSession;

            confSession.removeLocalUserRoleListener(this);
            confSession.removeMemberRoleListener(this);
            ((ChatRoomWrapper) chatSession.getDescriptor())
                .getChatRoom().removeMemberPropertyChangeListener(this);
        }

        if(subjectPanel != null)
            subjectPanel.dispose();

        if(this.chatContactListPanel != null)
            this.chatContactListPanel.dispose();
    }

    /**
     * Returns the chat window, where this chat panel is added.
     *
     * @return the chat window, where this chat panel is added
     */
    public ChatContainer getChatContainer()
    {
        return chatContainer;
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
        return chatContainer.getFrame();
    }

    /**
     * Adds a typing notification message to the conversation panel.
     *
     * @param typingNotification the typing notification to show
     */
    public void addTypingNotification(final String typingNotification)
    {
        if(!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    addTypingNotification(typingNotification);
                }
            });
            return;
        }

        typingNotificationLabel.setText(typingNotification);

        if (typingNotification != null && !typingNotification.equals(" "))
            typingNotificationLabel.setIcon(typingIcon);
        else
            typingNotificationLabel.setIcon(null);

        revalidate();
        repaint();
    }

    /**
     * Adds a typing notification message to the conversation panel,
     * saying that typin notifications has not been delivered.
     *
     * @param typingNotification the typing notification to show
     */
    public void addErrorSendingTypingNotification(
                    final String typingNotification)
    {
        if(!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    addErrorSendingTypingNotification(typingNotification);
                }
            });
            return;
        }

        typingNotificationLabel.setText(typingNotification);

        if (typingNotification != null && !typingNotification.equals(" "))
            typingNotificationLabel.setIcon(typingIcon);
        else
            typingNotificationLabel.setIcon(null);

        revalidate();
        repaint();
    }

    /**
     * Removes the typing notification message from the conversation panel.
     */
    public void removeTypingNotification()
    {
        addTypingNotification(" ");
    }

    /**
     * Initializes the typing notification label.
     * @param typingLabelParent the parent container
     *                          of typing notification label.
     */
    private void initTypingNotificationLabel(JPanel typingLabelParent)
    {
        typingNotificationLabel
            = new JLabel(" ", SwingConstants.CENTER);

        typingNotificationLabel.setPreferredSize(new Dimension(500, 20));
        typingNotificationLabel.setForeground(Color.GRAY);
        typingNotificationLabel.setFont(
            typingNotificationLabel.getFont().deriveFont(11f));
        typingNotificationLabel.setVerticalTextPosition(JLabel.BOTTOM);
        typingNotificationLabel.setHorizontalTextPosition(JLabel.LEFT);
        typingNotificationLabel.setIconTextGap(0);
        typingLabelParent.add(typingNotificationLabel, BorderLayout.SOUTH);
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
     * Returns the corresponding role description to the given role index.
     *
     * @param role to role index to analyse
     * @return String the corresponding role description
     */
    public String getRoleDescription(ChatRoomMemberRole role)
    {
        String roleDescription = null;

        switch(role)
        {
            case OWNER: roleDescription
                = GuiActivator.getResources().getI18NString(
                    "service.gui.OWNER");
            break;
            case ADMINISTRATOR: roleDescription
                = GuiActivator.getResources().getI18NString(
                    "service.gui.ADMINISTRATOR");
            break;
            case MODERATOR: roleDescription
                = GuiActivator.getResources().getI18NString(
                    "service.gui.MODERATOR");
            break;
            case MEMBER: roleDescription
                = GuiActivator.getResources().getI18NString(
                    "service.gui.MEMBER");
            break;
            case GUEST: roleDescription
                = GuiActivator.getResources().getI18NString(
                    "service.gui.GUEST");
            break;
            case SILENT_MEMBER: roleDescription
                = GuiActivator.getResources().getI18NString(
                    "service.gui.SILENT_MEMBER");
            break;
            case OUTCAST: roleDescription
                = GuiActivator.getResources().getI18NString(
                    "service.gui.BANNED");
            break;
            default:;
        }

        return roleDescription;
    }

    /**
     * Implements the <tt>memberRoleChanged()</tt> method.
     *
     * @param evt
     */
    public void memberRoleChanged(ChatRoomMemberRoleChangeEvent evt)
    {
        this.conversationPanel.appendMessageToEnd(
            "<DIV identifier=\"message\" style=\"color:#707070;\">"
            + GuiActivator.getResources().getI18NString("service.gui.IS_NOW",
                new String[]{evt.getSourceMember().getName(),
                getRoleDescription(evt.getNewRole())})
                +"</DIV>",
            ChatHtmlUtils.HTML_CONTENT_TYPE);
    }

    /**
     * Implements the <tt>localUserRoleChanged()</tt> method.
     *
     * @param evt
     */
    public void localUserRoleChanged(ChatRoomLocalUserRoleChangeEvent evt)
    {
        this.conversationPanel.appendMessageToEnd(
            "<DIV identifier=\"message\" style=\"color:#707070;\">"
            +GuiActivator.getResources().getI18NString("service.gui.ARE_NOW",
                new String[]{
                getRoleDescription(evt.getNewRole())}) +"</DIV>",
                ChatHtmlUtils.HTML_CONTENT_TYPE);
    }

    /**
     * Returns the ID of the last message sent in this chat, or <tt>null</tt>
     * if no messages have been sent yet.
     *
     * @return the ID of the last message sent in this chat, or <tt>null</tt>
     * if no messages have been sent yet.
     */
    public String getLastSentMessageUID()
    {
        return lastSentMessageUID;
    }

    /**
     * Called when the current {@link ChatTransport} has
     * changed. We will change current icon
     *
     * @param chatSession the {@link ChatSession} it's current
     * {@link ChatTransport} has changed
     */
    @Override
    public void currentChatTransportChanged(ChatSession chatSession)
    {
        setChatIcon(new ImageIcon(Constants.getStatusIcon(
            this.chatSession.getCurrentChatTransport().getStatus())));
        this.writeMessagePanel.currentChatTransportChanged(chatSession);
    }

    /**
     * When a property of the chatTransport has changed.
     */
    @Override
    public void currentChatTransportUpdated(int eventID)
    {
        if(eventID == ChatSessionChangeListener.ICON_UPDATED)
        {
            setChatIcon(new ImageIcon(Constants.getStatusIcon(
                this.chatSession.getCurrentChatTransport().getStatus())));
        }
        this.writeMessagePanel.currentChatTransportUpdated(eventID);
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
        @Override
        public void componentShown(ComponentEvent evt)
        {
            Component component = evt.getComponent();
            Container parent = component.getParent();

            if (!(parent instanceof JTabbedPane))
                return;

            JTabbedPane tabbedPane = (JTabbedPane) parent;

            if (tabbedPane.getSelectedComponent() != component)
                return;

            chatContainer.setCurrentChat(ChatPanel.this);
        }
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
     * Returns <tt>true</tt> if the chat is private messaging chat and 
     * <tt>false</tt> if not.
     * @return <tt>true</tt> if the chat is private messaging chat and 
     * <tt>false</tt> if not.
     */
    public boolean isPrivateMessagingChat()
    {
        return isPrivateMessagingChat;
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
                                .getAccountAddress(protocolProvider),
                            GuiActivator.getUIService().getMainFrame()
                                .getAccountDisplayName(protocolProvider),
                            evt.getTimestamp(),
                            messageType,
                            evt.getSourceMessage().getContent(),
                            evt.getSourceMessage().getContentType(),
                            evt.getSourceMessage().getMessageUID());
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
                                evt.getSourceContact().getAddress(),
                                evt.getSourceContact().getDisplayName(),
                                evt.getTimestamp(),
                                messageType,
                                evt.getSourceMessage().getContent(),
                                evt.getSourceMessage().getContentType(),
                                evt.getSourceMessage().getMessageUID());
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
                                .getAccountAddress(protocolProvider),
                            GuiActivator.getUIService().getMainFrame()
                                .getAccountDisplayName(protocolProvider),
                            evt.getTimestamp(),
                            Chat.HISTORY_OUTGOING_MESSAGE,
                            evt.getMessage().getContent(),
                            evt.getMessage().getContentType(),
                            evt.getMessage().getMessageUID());
            }
            else if(o instanceof ChatRoomMessageReceivedEvent)
            {
                ChatRoomMessageReceivedEvent evt
                    = (ChatRoomMessageReceivedEvent) o;

                if(!evt.getMessage().getMessageUID()
                        .equals(escapedMessageID))
                {
                    historyString = processHistoryMessage(
                            evt.getSourceChatRoomMember().getContactAddress(),
                            evt.getSourceChatRoomMember().getName(),
                            evt.getTimestamp(),
                            Chat.HISTORY_INCOMING_MESSAGE,
                            evt.getMessage().getContent(),
                            evt.getMessage().getContentType(),
                            evt.getMessage().getMessageUID());
                }
            }
            else if (o instanceof FileRecord)
            {
                FileRecord fileRecord = (FileRecord) o;

                if (!fileRecord.getID().equals(escapedMessageID))
                {
                    FileHistoryConversationComponent component
                        = new FileHistoryConversationComponent(fileRecord);

                    conversationPanel.addComponent(component);
                }
            }

            if (historyString != null)
                conversationPanel.appendMessageToEnd(
                    historyString, ChatHtmlUtils.HTML_CONTENT_TYPE);
        }

        fireChatHistoryChange();
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
    public void addMessage(String contactName, Date date,
            String messageType, String message, String contentType)
    {
        addMessage(contactName, null, date, messageType, message, contentType,
                null, null);
    }

    /**
     * Passes the message to the contained <code>ChatConversationPanel</code>
     * for processing and appends it at the end of the conversationPanel
     * document.
     *
     * @param contactName the name of the contact sending the message
     * @param displayName the display name of the contact
     * @param date the time at which the message is sent or received
     * @param messageType the type of the message. One of OUTGOING_MESSAGE
     * or INCOMING_MESSAGE
     * @param message the message text
     * @param contentType the content type
     */
    public void addMessage(String contactName, String displayName, Date date,
            String messageType, String message, String contentType,
            String messageUID, String correctedMessageUID)
    {
        ChatMessage chatMessage = new ChatMessage(contactName, displayName,
                date, messageType, null, message, contentType,
                messageUID, correctedMessageUID);

        this.addChatMessage(chatMessage);

        // A bug Fix for Previous/Next buttons .
        // Must update buttons state after message is processed
        // otherwise states are not proper
        fireChatHistoryChange();
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
    public void addMessage(String contactName, Date date,
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
    private void addChatMessage(final ChatMessage chatMessage)
    {
        // We need to be sure that chat messages are added in the event dispatch
        // thread.
        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    addChatMessage(chatMessage);
                }
            });
            return;
        }

        if (ConfigurationUtils.isHistoryShown() && !isHistoryLoaded)
        {
            synchronized (incomingEventBuffer)
            {
                incomingEventBuffer.add(chatMessage);
            }
        }
        else
        {
            displayChatMessage(chatMessage);
        }

        // change the last history message timestamp after we add one.
        this.lastHistoryMsgTimestamp = chatMessage.getDate();
        if (chatMessage.getMessageType().equals(Chat.OUTGOING_MESSAGE))
        {
            this.lastSentMessageUID = chatMessage.getMessageUID();
        }
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
        this.addMessage(contactName,
                new Date(),
                Chat.ERROR_MESSAGE,
                GuiActivator.getResources()
                    .getI18NString("service.gui.MSG_DELIVERY_FAILURE"),
                message, "text");
    }

    /**
     * Adds the given error message to the chat window conversation area.
     *
     * @param contactName the name of the contact, for which the error occurred
     * @param title the title of the error
     * @param message the error message
     */
    public void addErrorMessage(String contactName,
                                String title,
                                String message)
    {
        this.addMessage(contactName,
                new Date(),
                Chat.ERROR_MESSAGE,
                title,
                message, "text");
    }

    /**
     * Displays the given chat message.
     *
     * @param chatMessage the chat message to display
     */
    private void displayChatMessage(ChatMessage chatMessage)
    {
        if (chatMessage.getCorrectedMessageUID() != null
                && conversationPanel.getMessageContents(
                chatMessage.getCorrectedMessageUID()) != null)
        {
            applyMessageCorrection(chatMessage);
        }
        else
        {
            appendChatMessage(chatMessage);
        }
    }

    /**
     * Passes the message to the contained <code>ChatConversationPanel</code>
     * for processing and appends it at the end of the conversationPanel
     * document.
     *
     * @param chatMessage the message to append
     */
    private void appendChatMessage(final ChatMessage chatMessage)
    {
        String keyword = null;

        if (chatSession instanceof ConferenceChatSession
            && Chat.INCOMING_MESSAGE.equals(chatMessage.getMessageType()))
        {
            keyword =
                ((ChatRoomWrapper) chatSession.getDescriptor()).getChatRoom()
                    .getUserNickname();
        }

        String processedMessage =
            this.conversationPanel.processMessage(chatMessage, keyword,
                chatSession.getCurrentChatTransport().getProtocolProvider(),
                chatSession.getCurrentChatTransport().getName());

        if (chatSession instanceof ConferenceChatSession)
        {
            String meCommandMsg
                = this.conversationPanel.processMeCommand(chatMessage);

            // FIXME I'm pretty sure we are losing the previously prepared
            // processedMessage content.
            if (meCommandMsg.length() > 0)
                processedMessage = meCommandMsg;
        }

        this.conversationPanel.appendMessageToEnd(
            processedMessage, ChatHtmlUtils.HTML_CONTENT_TYPE);
    }

    /**
     * Passes the message to the contained <code>ChatConversationPanel</code>
     * for processing and replaces the specified message with this one.
     *
     * @param message The message used as a correction.
     */
    private void applyMessageCorrection(ChatMessage message)
    {
        conversationPanel.correctMessage(message);
    }

    /**
     * Passes the message to the contained <code>ChatConversationPanel</code>
     * for processing.
     *
     * @param contactName The name of the contact sending the message.
     * @param contactDisplayName the display name of the contact sending the
     * message
     * @param date The time at which the message is sent or received.
     * @param messageType The type of the message. One of OUTGOING_MESSAGE
     * or INCOMING_MESSAGE.
     * @param message The message text.
     * @param contentType the content type of the message (html or plain text)
     * @param messageId The ID of the message.
     *
     * @return a string containing the processed message.
     */
    private String processHistoryMessage(String contactName,
                                         String contactDisplayName,
                                         Date date,
                                         String messageType,
                                         String message,
                                         String contentType,
                                         String messageId)
    {
        ChatMessage chatMessage = new ChatMessage(
            contactName, contactDisplayName, date,
                messageType, null, message, contentType, messageId, null);

        String processedMessage =
            this.conversationPanel.processMessage(chatMessage,
                chatSession.getCurrentChatTransport().getProtocolProvider(),
                chatSession.getCurrentChatTransport().getName());

        if (chatSession instanceof ConferenceChatSession)
        {
            String tempMessage =
                conversationPanel.processMeCommand(chatMessage);

            if (tempMessage.length() > 0)
                processedMessage = tempMessage;
        }

        return processedMessage;
    }

    /**
     * Refreshes write area editor pane. Deletes all existing text
     * content.
     */
    public void refreshWriteArea()
    {
        if(!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    refreshWriteArea();
                }
            });
            return;
        }

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
     * @param mimeType the mime type
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
    public void cut()
    {
        this.writeMessagePanel.getEditorPane().cut();
    }

    /**
     * Copies either the selected write area content or the selected
     * conversation panel content to the clipboard.
     */
    public void copy()
    {
        JTextComponent textPane = this.conversationPanel.getChatTextPane();

        if (textPane.getSelectedText() == null)
            textPane = this.writeMessagePanel.getEditorPane();

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
        if (!isWriteAreaEmpty())
        {
            new Thread()
            {
                @Override
                public void run()
                {
                    sendMessage();
                }
            }.start();
        }

        //make sure the focus goes back to the write area
        requestFocusInWriteArea();
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
     * Brings the <tt>ChatWindow</tt> containing this <tt>ChatPanel</tt> to the
     * front if <tt>isVisble</tt> is <tt>true</tt>; hides it, otherwise.
     *
     * @param isVisible <tt>true</tt> to bring the <tt>ChatWindow</tt> of this
     * <tt>ChatPanel</tt> to the front; <tt>false</tt> to close this
     * <tt>ChatPanel</tt>
     */
    public void setChatVisible(boolean isVisible)
    {
        ChatWindowManager chatWindowManager
            = GuiActivator.getUIService().getChatWindowManager();

        if (isVisible)
            chatWindowManager.openChat(this, isVisible);
        else
            chatWindowManager.closeChat(this);
    }

    /**
     * Sets the visibility of conferences panel to <tt>true</tt> or 
     * <tt>false</tt>
     * 
     * @param isVisible if <tt>true</tt> the panel is visible.
     */
    public void setConferencesPanelVisible(boolean isVisible)
    {
        conferencePanel.setVisible(isVisible);
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
        ChatPanel currentChatPanel = chatContainer.getCurrentChat();

        return (currentChatPanel != null
                && currentChatPanel.equals(this)
                && chatContainer.getFrame().isActive());
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
        Document writeEditorDoc
            = writeMessagePanel.getEditorPane().getDocument();

        try
        {
            return writeEditorDoc.getText(0, writeEditorDoc.getLength());
        }
        catch (BadLocationException e)
        {
            return writeMessagePanel.getEditorPane().getText();
        }
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
        boolean isProtocolHidden = protocolProvider.getAccountID().isHidden();
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

        this.setSelectedChatTransport(sendFileTransport, true);

        if(file.length() > sendFileTransport.getMaximumFileLength())
        {
            addMessage(
                chatSession.getCurrentChatTransport().getName(),
                new Date(),
                Chat.ERROR_MESSAGE,
                GuiActivator.getResources()
                    .getI18NString("service.gui.FILE_TOO_BIG",
                    new String[]{
                        sendFileTransport.getMaximumFileLength()/1024/1024
                        + " MB"}),
                "",
                "text");

            fileComponent.setFailed();

            return;
        }

        SwingWorker worker = new SwingWorker()
        {
            @Override
            public Object construct()
                throws Exception
            {
                FileTransfer ft;
                if (writeMessagePanel.isSmsSelected())
                    ft = sendFileTransport.sendMultimediaFile(file);
                else
                    ft = sendFileTransport.sendFile(file);

                final FileTransfer fileTransfer = ft;

                addActiveFileTransfer(fileTransfer.getID(), fileTransfer);

                // Add the status listener that would notify us when the file
                // transfer has been completed and should be removed from
                // active components.
                fileTransfer.addStatusListener(ChatPanel.this);

                fileComponent.setProtocolFileTransfer(fileTransfer);

                return "";
            }

            @Override
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
                            "service.gui.MSG_DELIVERY_ERROR",
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
        // We need to be sure that the following code is executed in the event
        // dispatch thread.
        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    sendFile(file);
                }
            });
            return;
        }

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

        if (ConfigurationUtils.isHistoryShown() && !isHistoryLoaded)
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
        if (writeMessagePanel.isSmsSelected())
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
                new Date(),
                Chat.OUTGOING_MESSAGE,
                messageText,
                "plain/text");

            this.addErrorMessage(
                smsChatTransport.getName(),
                GuiActivator.getResources().getI18NString(
                    "service.gui.SEND_SMS_NOT_SUPPORTED"));

            return;
        }

        // We open the send SMS dialog.
        SendSmsDialog smsDialog
            = new SendSmsDialog(this, smsChatTransport, messageText);

        if(smsChatTransport.askForSMSNumber())
        {
            Object desc =
                smsChatTransport.getParentChatSession().getDescriptor();
            // descriptor will be metacontact
            if(desc instanceof MetaContact)
            {
                UIPhoneUtil contactPhoneUtil =
                    UIPhoneUtil.getPhoneUtil((MetaContact) desc);

                List<UIContactDetail> uiContactDetailList;

                boolean useAllNumbers =
                    GuiActivator.getConfigurationService().getBoolean(
                        USE_ADDITIONAL_NUMBERS_PROP, false);

                if(useAllNumbers)
                    uiContactDetailList
                        = contactPhoneUtil.getAdditionalNumbers();
                else
                    uiContactDetailList
                        = contactPhoneUtil.getAdditionalMobileNumbers();

                if(uiContactDetailList.size() != 0)
                {
                    SMSManager.sendSMS(
                        this, uiContactDetailList, messageText, this);

                    return;
                }
            }

            smsDialog.setPreferredSize(new Dimension(400, 200));
            smsDialog.setVisible(true);
        }
        else
        {
            smsDialog.sendSmsMessage(null, messageText);
            smsDialog.dispose();
        }
    }

    /**
     * Implements the <tt>ChatPanel.sendMessage</tt> method. Obtains the
     * appropriate operation set and sends the message, contained in the write
     * area, through it.
     */
    protected void sendInstantMessage()
    {
        String htmlText;
        String plainText;

        // read the text and clear it as quick as possible
        // to avoid double sending if the user hits enter too quickly
        synchronized(writeMessagePanel)
        {
            if(isWriteAreaEmpty())
                return;

            // Trims the html message, as it sometimes contains a lot of empty
            // lines, which causes some problems to some protocols.
            htmlText = getTextFromWriteArea(
                OperationSetBasicInstantMessaging.HTML_MIME_TYPE).trim();

            plainText = getTextFromWriteArea(
                OperationSetBasicInstantMessaging.DEFAULT_MIME_TYPE).trim();

            // clear the message earlier
            // to avoid as much as possible to not sending it twice (double enter)
            this.refreshWriteArea();
        }

        String messageText;
        String mimeType;
        if (chatSession.getCurrentChatTransport().isContentTypeSupported(
                    OperationSetBasicInstantMessaging.HTML_MIME_TYPE)
             && (htmlText.indexOf("<b") > -1
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
            if (isMessageCorrectionActive()
                    && chatSession.getCurrentChatTransport()
                        .allowsMessageCorrections())
            {
                chatSession.getCurrentChatTransport().correctInstantMessage(
                        messageText, mimeType, correctedMessageUID);
            }
            else
            {
                chatSession.getCurrentChatTransport().sendInstantMessage(
                        messageText, mimeType);
            }
            stopMessageCorrection();
        }
        catch (IllegalStateException ex)
        {
            logger.error("Failed to send message.", ex);

            this.addMessage(
                chatSession.getCurrentChatTransport().getName(),
                new Date(),
                Chat.OUTGOING_MESSAGE,
                messageText,
                mimeType);

            String protocolError = "";
            if (ex.getMessage() != null)
                protocolError = " " + GuiActivator.getResources().getI18NString(
                    "service.gui.ERROR_WAS",
                    new String[]{ex.getMessage()});

            this.addErrorMessage(
                chatSession.getCurrentChatTransport().getName(),
                GuiActivator.getResources().getI18NString(
                    "service.gui.MSG_SEND_CONNECTION_PROBLEM")
                    + protocolError);
        }
        catch (Exception ex)
        {
            logger.error("Failed to send message.", ex);

            this.refreshWriteArea();

            this.addMessage(
                chatSession.getCurrentChatTransport().getName(),
                new Date(),
                Chat.OUTGOING_MESSAGE,
                messageText,
                mimeType);

            String protocolError = "";
            if (ex.getMessage() != null)
                protocolError = " " + GuiActivator.getResources().getI18NString(
                    "service.gui.ERROR_WAS",
                    new String[]{ex.getMessage()});

            this.addErrorMessage(
                chatSession.getCurrentChatTransport().getName(),
                GuiActivator.getResources().getI18NString(
                    "service.gui.MSG_DELIVERY_ERROR",
                    new String[]{protocolError}));
        }

        if (chatSession.getCurrentChatTransport().allowsTypingNotifications())
        {
            // Send TYPING STOPPED event before sending the message
            getChatWritePanel().stopTypingTimer();
        }
    }

    /**
     * Sets the property which identifies whether the chat is private messaging 
     * chat or not.
     * 
     * @param isPrivateMessagingChat if <tt>true</tt> the chat panel will be 
     * private messaging chat panel.
     */
    public void setPrivateMessagingChat(boolean isPrivateMessagingChat)
    {
        this.isPrivateMessagingChat = isPrivateMessagingChat;
    }

    /**
     * Enters editing mode for the last sent message in this chat.
     */
    public void startLastMessageCorrection()
    {
        startMessageCorrection(lastSentMessageUID);
    }

    /**
     * Enters editing mode for the message with the specified id - puts the
     * message contents in the write panel and changes the background.
     *
     * @param correctedMessageUID The ID of the message being corrected.
     */
    public void startMessageCorrection(String correctedMessageUID)
    {
        if (!showMessageInWriteArea(correctedMessageUID))
        {
            return;
        }
        if (chatSession.getCurrentChatTransport().allowsMessageCorrections())
        {
            this.correctedMessageUID = correctedMessageUID;
            Color bgColor = new Color(GuiActivator.getResources()
                .getColor("service.gui.CHAT_EDIT_MESSAGE_BACKGROUND"));
            this.writeMessagePanel.setEditorPaneBackground(bgColor);
        }
    }

    /**
     * Shows the last sent message in the write area, either in order to
     * correct it or to send it again.
     *
     * @return <tt>true</tt> on success, <tt>false</tt> on failure.
     */
    public boolean showLastMessageInWriteArea()
    {
        return showMessageInWriteArea(lastSentMessageUID);
    }

    /**
     * Shows the message with the specified ID in the write area, either
     * in order to correct it or to send it again.
     *
     * @param messageUID The ID of the message to show.
     * @return <tt>true</tt> on success, <tt>false</tt> on failure.
     */
     public boolean showMessageInWriteArea(String messageUID)
     {
         String messageContents = conversationPanel.getMessageContents(
             messageUID);

         if (messageContents == null)
         {
             return false;
         }
         this.refreshWriteArea();
         this.setMessage(messageContents);
         return true;
     }

    /**
     * Exits editing mode, clears the write panel and the background.
     */
    public void stopMessageCorrection()
    {
        this.correctedMessageUID = null;
        this.writeMessagePanel.setEditorPaneBackground(Color.WHITE);
        this.refreshWriteArea();
    }

    /**
     * Returns whether a message is currently being edited.
     *
     * @return <tt>true</tt> if a message is currently being edited,
     * <tt>false</tt> otherwise.
     */
    public boolean isMessageCorrectionActive()
    {
        return correctedMessageUID != null;
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
     * Loads history messages ignoring the message with the specified id.
     *
     * @param escapedMessageID the id of the message to be ignored;
     * <tt>null</tt> if no message is to be ignored
     */
    public void loadHistory(final String escapedMessageID)
    {
        if (!ConfigurationUtils.isHistoryShown())
        {
            isHistoryLoaded = true;
            return;
        }

        SwingWorker historyWorker = new SwingWorker()
        {
            private Collection<Object> historyList;

            @Override
            public Object construct() throws Exception
            {
                // Load the history period, which initializes the
                // firstMessageTimestamp and the lastMessageTimeStamp variables.
                // Used to disable/enable history flash buttons in the chat
                // window tool bar.
                loadHistoryPeriod();

                // Load the last N=CHAT_HISTORY_SIZE messages from history.
                historyList = chatSession.getHistory(
                    ConfigurationUtils.getChatHistorySize());

                return historyList;
            }

            /**
             * Called on the event dispatching thread (not on the worker thread)
             * after the <code>construct</code> method has returned.
             */
            @Override
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

                chatContainer.updateHistoryButtonState(ChatPanel.this);
            }
        };

        historyWorker.start();
    }

    /**
     * Loads history for the chat meta contact in a separate thread. Equivalent
     * to calling {@link #loadHistory(String)} with <tt>null</tt> for
     * <tt>escapedMessageID</tt>.
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
        writeMessagePanel.setSmsSelected(isSmsSelected);
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
            writeMessagePanel.openChatTransportSelectorBox();
        }
    }

    /**
     * Renames all occurrences of the given <tt>chatContact</tt> in this chat
     * panel.
     *
     * @param chatContact the contact to rename
     * @param name the new name
     */
    public void setContactName(ChatContact<?> chatContact, String name)
    {
        if (chatContactListPanel != null)
        {
            chatContactListPanel.renameContact(chatContact);
        }

        ChatContainer chatContainer = getChatContainer();
        chatContainer.setChatTitle(this, name);

        if (chatContainer.getCurrentChat() == this)
        {
            chatContainer.setTitle(name);
        }
    }

    /**
     * Adds the given chatTransport to the given send via selector box.
     *
     * @param chatTransport the transport to add
     */
    public void addChatTransport(ChatTransport chatTransport)
    {
        writeMessagePanel.addChatTransport(chatTransport);
    }

    /**
     * Removes the given chat status state from the send via selector box.
     *
     * @param chatTransport the transport to remove
     */
    public void removeChatTransport(ChatTransport chatTransport)
    {
        writeMessagePanel.removeChatTransport(chatTransport);
    }

    /**
     * Selects the given chat transport in the send via box.
     *
     * @param chatTransport the chat transport to be selected
     * @param isMessageOrFileTransferReceived Boolean telling us if this change
     * of the chat transport correspond to an effective switch to this new
     * transform (a message received from this transport, or a file transfer
     * request received, or if the resource timeouted), or just a status update
     * telling us a new chatTransport is now available (i.e. another device has
     * startup).
     */
    public void setSelectedChatTransport(
            ChatTransport chatTransport,
            boolean isMessageOrFileTransferReceived)
    {
        writeMessagePanel.setSelectedChatTransport(
                chatTransport,
                isMessageOrFileTransferReceived);
    }

    /**
     * Updates the status of the given chat transport in the send via selector
     * box and notifies the user for the status change.
     * @param chatTransport the <tt>chatTransport</tt> to update
     */
    public void updateChatTransportStatus(final ChatTransport chatTransport)
    {
        if(!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    updateChatTransportStatus(chatTransport);
                }
            });
            return;
        }

        writeMessagePanel.updateChatTransportStatus(chatTransport);

        if (!chatTransport.equals(chatSession.getCurrentChatTransport()))
            return;

        if(ConfigurationUtils.isShowStatusChangedInChat())
        {
            // Show a status message to the user.
            this.addMessage(
                chatTransport.getName(),
                new Date(),
                Chat.STATUS_MESSAGE,
                GuiActivator.getResources().getI18NString(
                    "service.gui.STATUS_CHANGED_CHAT_MESSAGE",
                    new String[]{chatTransport.getStatus().getStatusName()}),
                    "text/plain");
        }
    }

    /**
     * Sets the chat icon.
     *
     * @param icon the chat icon to set
     */
    public void setChatIcon(Icon icon)
    {
        if(ConfigurationUtils.isMultiChatWindowEnabled())
        {
            if (getChatContainer().getChatCount() > 0)
            {
                getChatContainer().setChatIcon(this, icon);
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

        SwingWorker worker = new SwingWorker()
        {
            @Override
            public Object construct() throws Exception
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

                return "";
            }

            @Override
            public void finished()
            {
                getChatContainer().updateHistoryButtonState(ChatPanel.this);
            }
        };
        worker.start();
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

        SwingWorker worker = new SwingWorker()
        {
            @Override
            public Object construct() throws Exception
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

                return "";
            }

            @Override
            public void finished()
            {
                getChatContainer().updateHistoryButtonState(ChatPanel.this);
            }
        };
        worker.start();
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
            ChatConversationPanel chatConversationPanel
                = getChatConversationPanel();

            chatConversationPanel.clear();
            processHistory(chatHistory, "");
            chatConversationPanel.setDefaultContent();
        }
    }

    /**
     * Adds the given <tt>chatContact</tt> to the list of chat contacts
     * participating in the corresponding to this chat panel chat.
     * @param chatContact the contact to add
     */
    public void addChatContact(ChatContact<?> chatContact)
    {
        if (chatContactListPanel != null)
            chatContactListPanel.addContact(chatContact);
    }

    /**
     * Removes the given <tt>chatContact</tt> from the list of chat contacts
     * participating in the corresponding to this chat panel chat.
     * @param chatContact the contact to remove
     */
    public void removeChatContact(ChatContact<?> chatContact)
    {
        if (chatContactListPanel != null)
            chatContactListPanel.removeContact(chatContact);
    }
    
    /**
     * Adds the given <tt>conferenceDescription</tt> to the list of chat 
     * conferences in this chat panel chat.
     * @param conferenceDescription the conference to add.
     */
    @Override
    public void addChatConferenceCall(
        ConferenceDescription conferenceDescription)
    {
        if(chatConferencesDialog != null)
        {
            chatConferencesDialog.addConference(conferenceDescription);
        }
    }

    /**
     * Removes the given <tt>conferenceDescription</tt> from the list of chat 
     * conferences in this chat panel chat.
     * @param conferenceDescription the conference to remove.
     */
    @Override
    public void removeChatConferenceCall(ConferenceDescription 
        conferenceDescription)
    {
        if(chatConferencesDialog != null)
        {
            chatConferencesDialog.removeConference(conferenceDescription);
        }
    }

    /**
     * Removes all chat contacts from the contact list of the chat.
     */
    public void removeAllChatContacts()
    {
        if (chatContactListPanel != null)
            chatContactListPanel.removeAllChatContacts();
    }

    /**
     * Updates the contact status.
     * @param chatContact the chat contact to update
     * @param statusMessage the status message to show
     */
    public void updateChatContactStatus(final ChatContact<?> chatContact,
                                        final String statusMessage)
    {
        if(!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    updateChatContactStatus(chatContact, statusMessage);
                }
            });
            return;
        }

        this.addMessage(
            chatContact.getName(),
            new Date(),
            Chat.STATUS_MESSAGE,
            statusMessage,
            ChatHtmlUtils.TEXT_CONTENT_TYPE);
    }

    /**
     * Sets the given <tt>subject</tt> to this chat.
     * @param subject the subject to set
     */
    public void setChatSubject(final String subject)
    {
        if(!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                @Override
                public void run()
                {
                    setChatSubject(subject);
                }
            });

            return;
        }

        if (subjectPanel != null)
        {
            // Don't do anything if the subject doesn't really change.
            String oldSubject = subjectPanel.getSubject();

            if ((subject == null ) || (subject.length() == 0))
            {
                if ((oldSubject == null) || (oldSubject.length() == 0))
                    return;
            }
            else if (subject.equals(oldSubject))
                return;

            subjectPanel.setSubject(subject);

            this.addMessage(
                chatSession.getChatName(),
                new Date(),
                Chat.STATUS_MESSAGE,
                GuiActivator.getResources().getI18NString(
                    "service.gui.CHAT_ROOM_SUBJECT_CHANGED",
                    new String []{  chatSession.getChatName(),
                                    subject}),
                    ChatHtmlUtils.TEXT_CONTENT_TYPE);
        }
    }

    /**
     * Adds the given <tt>IncomingFileTransferRequest</tt> to the conversation
     * panel in order to notify the user of the incoming file.
     *
     * @param fileTransferOpSet the file transfer operation set
     * @param request the request to display in the conversation panel
     * @param date the date on which the request has been received
     */
    public void addIncomingFileTransferRequest(
        final OperationSetFileTransfer fileTransferOpSet,
        final IncomingFileTransferRequest request,
        final Date date)
    {
        if(!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    addIncomingFileTransferRequest(
                        fileTransferOpSet, request, date);
                }
            });
            return;
        }

        this.addActiveFileTransfer(request.getID(), request);

        ReceiveFileConversationComponent component
            = new ReceiveFileConversationComponent(
                this, fileTransferOpSet, request, date);

        if (ConfigurationUtils.isHistoryShown() && !isHistoryLoaded)
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
        // We currently don't support file transfer in group chats.
        if (chatSession instanceof ConferenceChatSession)
            return null;

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

    /**
     * Invites the given <tt>chatContacts</tt> to this chat.
     * @param inviteChatTransport the chat transport to use to send the invite
     * @param chatContacts the contacts to invite
     * @param reason the reason of the invitation
     */
    public void inviteContacts( ChatTransport inviteChatTransport,
                                Collection<String> chatContacts,
                                String reason)
    {
        ChatSession conferenceChatSession = null;

        if (chatSession instanceof MetaContactChatSession)
        {
            chatContacts.add(inviteChatTransport.getName());

            ConferenceChatManager conferenceChatManager
                = GuiActivator.getUIService().getConferenceChatManager();

            // the chat session is set regarding to which OpSet is used for MUC
            if(inviteChatTransport.getProtocolProvider().
                    getOperationSet(OperationSetMultiUserChat.class) != null)
            {
                ChatRoomWrapper chatRoomWrapper
                    = GuiActivator.getMUCService().createPrivateChatRoom(
                        inviteChatTransport.getProtocolProvider(),
                        chatContacts,
                        reason,
                        false);

                conferenceChatSession
                    = new ConferenceChatSession(this, chatRoomWrapper);
            }
            else if (inviteChatTransport.getProtocolProvider().
                getOperationSet(OperationSetAdHocMultiUserChat.class) != null)
            {
                AdHocChatRoomWrapper chatRoomWrapper
                    = conferenceChatManager.createAdHocChatRoom(
                            inviteChatTransport.getProtocolProvider(),
                            chatContacts,
                            reason);

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

        if (logger.isTraceEnabled())
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
     * @param event the file transfer status change event the notified us for
     * the change
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
     * Cancels all active file transfers.
     */
    public void cancelActiveFileTransfers()
    {
        Enumeration<String> activeKeys = activeFileTransfers.keys();

        while (activeKeys.hasMoreElements())
        {
            // catchall so if anything happens we still
            // will close the chat/window
            try
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
            catch(Throwable t)
            {
                logger.error("Cannot cancel file transfer.", t);
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

                // We store the divider location only when the user drags the
                // divider and not when we've set it programatically.
//                if (dividerLocation != autoDividerLocation)
//                {
                    int writeAreaSize
                        = messagePane.getHeight() - dividerLocation
                                        - messagePane.getDividerSize();

                    ConfigurationUtils
                        .setChatWriteAreaSize(writeAreaSize);

//                    writeMessagePanel.setPreferredSize(
//                        new Dimension(
//                            (int) writeMessagePanel.getPreferredSize()
//                                    .getWidth(),
//                            writeAreaSize));
//                }
            }
        }
    }

    /**
     * Sets the location of the split pane divider.
     *
     * @param location the location of the divider given by the pixel count
     * between the left bottom corner and the left bottom divider location
     */
    public void setDividerLocation(int location)
    {
        int dividerLocation = messagePane.getHeight() - location;

        messagePane.setDividerLocation(dividerLocation);
        messagePane.revalidate();
        messagePane.repaint();
    }

    /**
     * Returns the contained divider location.
     *
     * @return the contained divider location
     */
    public int getDividerLocation()
    {
        return messagePane.getHeight() - messagePane.getDividerLocation();
    }

    /**
     * Returns the contained divider size.
     *
     * @return the contained divider size
     */
    public int getDividerSize()
    {
        return messagePane.getDividerSize();
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
                    this.displayChatMessage((ChatMessage) incomingEvent);
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
     * @param descriptor the descriptor of the file transfer
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

    /**
     * Adds the given {@link ChatMenuListener} to this <tt>Chat</tt>.
     * The <tt>ChatMenuListener</tt> is used to determine menu elements
     * that should be added on right clicks.
     *
     * @param l the <tt>ChatMenuListener</tt> to add
     */
    public void addChatEditorMenuListener(ChatMenuListener l)
    {
        this.getChatWritePanel().addChatEditorMenuListener(l);
    }

    /**
     * Adds the given {@link CaretListener} to this <tt>Chat</tt>.
     * The <tt>CaretListener</tt> is used to inform other bundles when a user has
     * moved the caret in the chat editor area.
     *
     * @param l the <tt>CaretListener</tt> to add
     */
    public void addChatEditorCaretListener(CaretListener l)
    {
        this.getChatWritePanel().getEditorPane().addCaretListener(l);
    }

    /**
     * Adds the given {@link DocumentListener} to this <tt>Chat</tt>.
     * The <tt>DocumentListener</tt> is used to inform other bundles when a user has
     * modified the document in the chat editor area.
     *
     * @param l the <tt>DocumentListener</tt> to add
     */
    public void addChatEditorDocumentListener(DocumentListener l)
    {
        this.getChatWritePanel().getEditorPane()
            .getDocument().addDocumentListener(l);
    }

      /**
     * Removes the given {@link CaretListener} from this <tt>Chat</tt>.
     * The <tt>CaretListener</tt> is used to inform other bundles when a user has
     * moved the caret in the chat editor area.
     *
     * @param l the <tt>CaretListener</tt> to remove
     */
    public void removeChatEditorCaretListener(CaretListener l)
    {
        this.getChatWritePanel().getEditorPane().removeCaretListener(l);
    }

     /**
     * Removes the given {@link ChatMenuListener} to this <tt>Chat</tt>.
     * The <tt>ChatMenuListener</tt> is used to determine menu elements
     * that should be added on right clicks.
     *
     * @param l the <tt>ChatMenuListener</tt> to add
     */
    public void removeChatEditorMenuListener(ChatMenuListener l)
    {
        this.getChatWritePanel().removeChatEditorMenuListener(l);
    }

    /**
     * Removes the given {@link DocumentListener} from this <tt>Chat</tt>.
     * The <tt>DocumentListener</tt> is used to inform other bundles when a user has
     * modified the document in the chat editor area.
     *
     * @param l the <tt>DocumentListener</tt> to remove
     */
    public void removeChatEditorDocumentListener(DocumentListener l)
    {
        this.getChatWritePanel().getEditorPane()
            .getDocument().removeDocumentListener(l);
    }

    /**
     * Adds the given <tt>ChatHistoryListener</tt> to the list of listeners
     * notified when a change occurs in the history shown in this chat panel.
     *
     * @param l the <tt>ChatHistoryListener</tt> to add
     */
    public void addChatHistoryListener(ChatHistoryListener l)
    {
        synchronized (historyListeners)
        {
            historyListeners.add(l);
        }
    }

    /**
     * Removes the given <tt>ChatHistoryListener</tt> from the list of listeners
     * notified when a change occurs in the history shown in this chat panel.
     *
     * @param l the <tt>ChatHistoryListener</tt> to remove
     */
    public void removeChatHistoryListener(ChatHistoryListener l)
    {
        synchronized (historyListeners)
        {
            historyListeners.remove(l);
        }
    }

    /**
     * Notifies all registered <tt>ChatHistoryListener</tt>s that a change has
     * occurred in the history of this chat.
     */
    private void fireChatHistoryChange()
    {
        Iterator<ChatHistoryListener> listeners = historyListeners.iterator();

        while (listeners.hasNext())
        {
            listeners.next().chatHistoryChanged(this);
        }
    }

    /**
     * Provides the {@link Highlighter} used in rendering the chat editor.
     *
     * @return highlighter used to render message being composed
     */
    public Highlighter getHighlighter()
    {
        return this.getChatWritePanel().getEditorPane().getHighlighter();
    }

    /**
     * Gets the caret position in the chat editor.
     * @return index of caret in message being composed
     */
    public int getCaretPosition()
    {
        return this.getChatWritePanel().getEditorPane().getCaretPosition();
    }

    /**
     * Causes the chat to validate its appearance (suggests a repaint operation
     * may be necessary).
     */
    public void promptRepaint()
    {
        this.getChatWritePanel().getEditorPane().repaint();
    }

    /**
     * Shows the font chooser dialog
     */
    public void showFontChooserDialog()
    {
        JEditorPane editorPane = writeMessagePanel.getEditorPane();
        FontChooser fontChooser = new FontChooser();

        int result = fontChooser.showDialog(this);

        if (result != FontChooser.CANCEL_OPTION)
        {
            String fontFamily = fontChooser.getFontFamily();
            int fontSize = fontChooser.getFontSize();
            boolean isBold = fontChooser.isBoldStyleSelected();
            boolean isItalic = fontChooser.isItalicStyleSelected();
            boolean isUnderline = fontChooser.isUnderlineStyleSelected();
            Color fontColor = fontChooser.getFontColor();

            // Font family and size
            writeMessagePanel.setFontFamilyAndSize(fontFamily, fontSize);

            // Font style
            writeMessagePanel.setBoldStyleEnable(isBold);
            writeMessagePanel.setItalicStyleEnable(isItalic);
            writeMessagePanel.setUnderlineStyleEnable(isUnderline);

            // Font color
            writeMessagePanel.setFontColor(fontColor);

            writeMessagePanel.saveDefaultFontConfiguration( fontFamily,
                                                            fontSize,
                                                            isBold,
                                                            isItalic,
                                                            isUnderline,
                                                            fontColor);
        }

        editorPane.requestFocus();
    }

    /**
     * Reloads chat messages.
     */
    public void loadSkin()
    {
        getChatConversationPanel().clear();
        loadHistory();
        getChatConversationPanel().setDefaultContent();
    }

    /**
     * Notifies the user if any member of the chatroom changes nickname.
     *
     * @param event a <tt>ChatRoomMemberPropertyChangeEvent</tt> which carries
     * the specific of the change
     */
    public void chatRoomPropertyChanged(ChatRoomMemberPropertyChangeEvent event)
    {
        if (ChatRoomMemberPropertyChangeEvent.MEMBER_NICKNAME.equals(event
            .getPropertyName()))
        {
            String message =
                GuiActivator.getResources().getI18NString(
                    "service.gui.CHAT_NICKNAME_CHANGE",
                    new String[]
                    { (String) event.getOldValue(),
                        (String) event.getNewValue() });
            this.conversationPanel.appendMessageToEnd(
                "<DIV identifier=\"message\" style=\"color:#707070;\">"
                    + StringEscapeUtils.escapeHtml4(message) + "</DIV>",
                ChatHtmlUtils.HTML_CONTENT_TYPE);
        }
    }

    /**
     * Add a new ChatLinkClickedListener
     *
     * @param listener ChatLinkClickedListener
     */
    public void addChatLinkClickedListener(ChatLinkClickedListener listener)
    {
        conversationPanel.addChatLinkClickedListener(listener);
    }

    /**
     * Remove existing ChatLinkClickedListener
     *
     * @param listener ChatLinkClickedListener
     */
    public void removeChatLinkClickedListener(ChatLinkClickedListener listener)
    {
        conversationPanel.removeChatLinkClickedListener(listener);
    }

    /**
     * Changes the chat conference dialog layout. This method is called when the 
     * local user publishes a <tt>ConferenceDescription</tt> instance.
     * 
     * @param conferenceDescription the <tt>ConferenceDescription</tt> instance 
     * associated with the conference.
     */
    @Override
    public void chatConferenceDescriptionSent(
        ConferenceDescription conferenceDescription)
    {
        boolean available = conferenceDescription.isAvailable();
        chatConferencesDialog.setCreatePanelEnabled(!available);
        chatConferencesDialog.setEndConferenceButtonEnabled(available);
    }
}
