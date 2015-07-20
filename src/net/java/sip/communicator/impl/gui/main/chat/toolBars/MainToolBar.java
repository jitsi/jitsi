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
package net.java.sip.communicator.impl.gui.main.chat.toolBars;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.main.call.*;
import net.java.sip.communicator.impl.gui.main.chat.*;
import net.java.sip.communicator.impl.gui.main.chat.conference.*;
import net.java.sip.communicator.impl.gui.main.configforms.*;
import net.java.sip.communicator.impl.gui.main.contactlist.*;
import net.java.sip.communicator.impl.gui.main.contactlist.contactsource.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.plugin.desktoputil.SwingWorker;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.Container;
import net.java.sip.communicator.service.muc.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.call.*;
import net.java.sip.communicator.util.skin.*;

/**
 * The <tt>MainToolBar</tt> is a <tt>JToolBar</tt> which contains buttons
 * for file operations, like save and print, for copy-paste operations, etc.
 * It's the main toolbar in the <tt>ChatWindow</tt>. It contains only
 * <tt>ChatToolbarButton</tt>s, which have a specific background icon and
 * rollover behaviour to differentiates them from normal buttons.
 *
 * @author Yana Stamcheva
 * @author Lubomir Marinov
 * @author Adam Netocny
 */
public class MainToolBar
    extends TransparentPanel
    implements ActionListener,
               ChatChangeListener,
               ChatSessionChangeListener,
               ChatRoomLocalUserRoleListener,
               Skinnable
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = -5572510509556499465L;

    /**
     * The invite button.
     */
    private final ChatToolbarButton inviteButton
        = new ChatToolbarButton(
                ImageLoader.getImage(ImageLoader.ADD_TO_CHAT_ICON));

    /**
     * The history button.
     */
    private final HistorySelectorBox historyButton;

    /**
     * The send file button.
     */
    private final ChatToolbarButton sendFileButton
        = new ChatToolbarButton(
                ImageLoader.getImage(ImageLoader.SEND_FILE_ICON));

    /**
     * The button showing the previous page of the chat history.
     */
    private final ChatToolbarButton previousButton
        = new ChatToolbarButton(
                ImageLoader.getImage(ImageLoader.PREVIOUS_ICON));

    /**
     * The button showing the next page from chat history.
     */
    private final ChatToolbarButton nextButton
        = new ChatToolbarButton(
                ImageLoader.getImage(ImageLoader.NEXT_ICON));

    /**
     * The leave chat room button.
     */
    private final ChatToolbarButton leaveChatRoomButton
        = new ChatToolbarButton(
                ImageLoader.getImage(ImageLoader.LEAVE_ICON));

    /**
     * The call button.
     */
    private final ChatToolbarButton callButton
        = new ChatToolbarButton(
                ImageLoader.getImage(ImageLoader.CHAT_CALL));

    /**
     * The call button.
     */
    private final ChatToolbarButton callVideoButton
        = new ChatToolbarButton(
                ImageLoader.getImage(ImageLoader.CHAT_VIDEO_CALL));

    /**
     * The options button.
     */
    private final ChatToolbarButton optionsButton
            = new ChatToolbarButton(
                    ImageLoader.getImage(ImageLoader.CHAT_CONFIGURE_ICON));

    /**
     * The desktop sharing button.
     */
    private final ChatToolbarButton desktopSharingButton
        = new ChatToolbarButton(
                ImageLoader.getImage(ImageLoader.CHAT_DESKTOP_SHARING));

    /**
     * The font button.
     */
    private final ChatToolbarButton fontButton
        = new ChatToolbarButton(
                ImageLoader.getImage(ImageLoader.FONT_ICON));

    private SmileysSelectorBox smileysBox;

    /**
     * The current <tt>ChatSession</tt> made known to this instance by the last
     * call to its {@link #chatChanged(ChatPanel)}.
     */
    private ChatSession chatSession;

    /**
     * The chat container, where this tool bar is added.
     */
    protected final ChatContainer chatContainer;

    /**
     * The plug-in container contained in this tool bar.
     */
    private final PluginContainer pluginContainer;

    /**
     * The phone util used to enable/disable buttons.
     */
    private MetaContactPhoneUtil contactPhoneUtil = null;

    /**
     * Creates an instance and constructs the <tt>MainToolBar</tt>.
     *
     * @param chatContainer The parent <tt>ChatWindow</tt>.
     */
    public MainToolBar(ChatContainer chatContainer)
    {
        this.chatContainer = chatContainer;

        historyButton = new HistorySelectorBox(chatContainer);

        init();

        pluginContainer
            = new PluginContainer(this, Container.CONTAINER_CHAT_TOOL_BAR);

        this.chatContainer.addChatChangeListener(this);

    }

    /**
     * Initializes this component.
     */
    protected void init()
    {
        this.setLayout(new FlowLayout(FlowLayout.LEFT, 3, 0));
        this.setOpaque(false);

        this.add(inviteButton);

        // if we leave a chat room when we close the window
        // there is no need for this button
        if(!ConfigurationUtils.isLeaveChatRoomOnWindowCloseEnabled())
            this.add(leaveChatRoomButton);

        this.add(callButton);
        this.add(callVideoButton);
        this.add(desktopSharingButton);
        this.add(sendFileButton);

        ChatPanel chatPanel = chatContainer.getCurrentChat();
        if (chatPanel == null
            || !(chatPanel.getChatSession() instanceof MetaContactChatSession))
            sendFileButton.setEnabled(false);
        
        if(chatPanel != null && chatPanel.isPrivateMessagingChat())
        {
            inviteButton.setEnabled(false);
        }

        if (chatPanel == null
            || !(chatPanel.getChatSession() instanceof ConferenceChatSession))
            desktopSharingButton.setEnabled(false);
        
        this.addSeparator();

        SIPCommMenuBar historyMenuBar = new SIPCommMenuBar();
        historyMenuBar.setOpaque(false);
        historyMenuBar.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
        historyMenuBar.add(historyButton);
        this.add(historyMenuBar);

        this.add(previousButton);
        this.add(nextButton);

        // We only add the options button if the property SHOW_OPTIONS_WINDOW
        // specifies so or if it's not set.
        Boolean showOptionsProp
            = GuiActivator.getConfigurationService()
                .getBoolean(ConfigurationFrame.SHOW_OPTIONS_WINDOW_PROPERTY,
                            false);

        if (showOptionsProp.booleanValue())
        {
            this.add(optionsButton);
        }

        this.addSeparator();

        if(ConfigurationUtils.isFontSupportEnabled())
        {
            this.add(fontButton);
            fontButton.setName("font");
            fontButton.setToolTipText(GuiActivator.getResources()
                .getI18NString("service.gui.CHANGE_FONT"));
            fontButton.addActionListener(this);
        }

        initSmiliesSelectorBox();

        this.addSeparator();

        this.inviteButton.setName("invite");
        this.inviteButton.setToolTipText(
            GuiActivator.getResources().getI18NString("service.gui.INVITE"));

        this.leaveChatRoomButton.setName("leave");
        this.leaveChatRoomButton.setToolTipText(
            GuiActivator.getResources().getI18NString("service.gui.LEAVE"));

        setCallButtonsName();
        setCallButtonsIcons();
        
        this.desktopSharingButton.setName("desktop");
        this.desktopSharingButton.setToolTipText(
            GuiActivator.getResources().getI18NString(
                "service.gui.SHARE_DESKTOP_WITH_CONTACT"));

        optionsButton.setName("options");
        optionsButton.setToolTipText(
            GuiActivator.getResources().getI18NString("service.gui.OPTIONS"));

        this.sendFileButton.setName("sendFile");
        this.sendFileButton.setToolTipText(
            GuiActivator.getResources().getI18NString("service.gui.SEND_FILE"));

        this.previousButton.setName("previous");
        this.previousButton.setToolTipText(
            GuiActivator.getResources().getI18NString("service.gui.PREVIOUS"));

        this.nextButton.setName("next");
        this.nextButton.setToolTipText(
            GuiActivator.getResources().getI18NString("service.gui.NEXT"));

        inviteButton.addActionListener(this);
        leaveChatRoomButton.addActionListener(this);
        callButton.addActionListener(this);
        callVideoButton.addActionListener(this);
        desktopSharingButton.addActionListener(this);
        optionsButton.addActionListener(this);
        sendFileButton.addActionListener(this);
        previousButton.addActionListener(this);
        nextButton.addActionListener(this);
    }

    private void initSmiliesSelectorBox()
    {
        this.smileysBox = new SmileysSelectorBox();

        this.smileysBox.setName("smiley");
        this.smileysBox.setToolTipText(GuiActivator.getResources()
            .getI18NString("service.gui.INSERT_SMILEY") + " Ctrl-M");

        SIPCommMenuBar smileyMenuBar = new SIPCommMenuBar();
        smileyMenuBar.setOpaque(false);
        smileyMenuBar.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
        smileyMenuBar.add(smileysBox);

        this.add(smileyMenuBar);
    }

    /**
     * Runs clean-up for associated resources which need explicit disposal (e.g.
     * listeners keeping this instance alive because they were added to the
     * model which operationally outlives this instance).
     */
    public void dispose()
    {
        pluginContainer.dispose();

        historyButton.dispose();

        if (this.chatSession != null
            && this.chatSession instanceof MetaContactChatSession)
            this.chatSession.removeChatTransportChangeListener(this);

        if(this.chatSession != null
            && this.chatSession instanceof ConferenceChatSession)
            ((ConferenceChatSession) this.chatSession)
                .removeLocalUserRoleListener(this);
    }

    /**
     * Implements ChatChangeListener#chatChanged(ChatPanel).
     * @param chatPanel the <tt>ChatPanel</tt>, which changed
     */
    public void chatChanged(ChatPanel chatPanel)
    {
        if (chatPanel == null)
        {
            setChatSession(null);
        }
        else
        {
            MetaContact contact
                = GuiActivator.getUIService().getChatContact(chatPanel);

            for (PluginComponent c : pluginContainer.getPluginComponents())
                c.setCurrentContact(contact);

            ChatSession chatSession = chatPanel.getChatSession();
            setChatSession(chatSession);

            leaveChatRoomButton.setEnabled(
                chatSession instanceof ConferenceChatSession);
            
            desktopSharingButton.setEnabled(
                !(chatSession instanceof ConferenceChatSession));

            inviteButton.setEnabled(
                chatPanel.findInviteChatTransport() != null);

            sendFileButton.setEnabled(
                chatPanel.findFileTransferChatTransport() != null);
            inviteButton.setEnabled(!chatPanel.isPrivateMessagingChat());

            if(chatSession instanceof ConferenceChatSession)
            {
                updateInviteContactButton();

                callButton.setVisible(false);
                callVideoButton.setVisible(false);
                callButton.setEnabled(true);
                callVideoButton.setEnabled(true);
            }
            else if(contact != null)
            {
                callButton.setVisible(true);
                callVideoButton.setVisible(true);
                new UpdateCallButtonWorker(contact).start();
            }

            changeHistoryButtonsState(chatPanel);
            
            setCallButtonsName();
            setCallButtonsIcons();

            currentChatTransportChanged(chatSession);
        }
    }

    /**
     * Returns list of <tt>ChatTransport</tt> (i.e. contact) that supports the
     * specified <tt>OperationSet</tt>.
     *
     * @param transports list of <tt>ChatTransport</tt>
     * @param opSetClass <tt>OperationSet</tt> to find
     * @return list of <tt>ChatTransport</tt> (i.e. contact) that supports the
     * specified <tt>OperationSet</tt>.
     */
    private List<ChatTransport> getOperationSetForCapabilities(
            List<ChatTransport> transports,
            Class<? extends OperationSet> opSetClass)
    {
        List<ChatTransport> list = new ArrayList<ChatTransport>();

        for(ChatTransport transport : transports)
        {
            ProtocolProviderService protocolProvider
                = transport.getProtocolProvider();
            OperationSetContactCapabilities capOpSet
                = protocolProvider.getOperationSet(
                        OperationSetContactCapabilities.class);
            OperationSetPersistentPresence presOpSet
                = protocolProvider.getOperationSet(
                        OperationSetPersistentPresence.class);

            if (capOpSet == null)
            {
                list.add(transport);
            }
            else if (presOpSet != null)
            {
                Contact contact
                    = presOpSet.findContactByID(transport.getName());

                if((contact != null)
                        && (capOpSet.getOperationSet(contact, opSetClass)
                                != null))
                {
                    // It supports OpSet for at least one of its
                    // ChatTransports
                    list.add(transport);
                }
            }
        }

        return list;
    }

    /**
     * Implements
     * ChatSessionChangeListener#currentChatTransportChanged(ChatSession).
     * @param chatSession the <tt>ChatSession</tt>, which transport has changed
     */
    public void currentChatTransportChanged(ChatSession chatSession)
    {
        if (chatSession == null)
            return;

        ChatTransport currentTransport = chatSession.getCurrentChatTransport();
        Object currentDescriptor = currentTransport.getDescriptor();

        if (currentDescriptor instanceof Contact)
        {
            Contact contact = (Contact) currentDescriptor;

            for (PluginComponent c : pluginContainer.getPluginComponents())
                c.setCurrentContact(contact, currentTransport.getResourceName());
        }
    }

    /**
     * When a property of the chatTransport has changed.
     * @param eventID the event id representing the property of the transport
     * that has changed.
     */
    public void currentChatTransportUpdated(int eventID)
    {}

    /**
     * Handles the <tt>ActionEvent</tt>, when one of the tool bar buttons is
     * clicked.
     * @param e the <tt>ActionEvent</tt> that notified us
     */
    public void actionPerformed(ActionEvent e)
    {
        AbstractButton button = (AbstractButton) e.getSource();
        String buttonText = button.getName();

        ChatPanel chatPanel = chatContainer.getCurrentChat();

        if (buttonText.equals("previous"))
        {
            chatPanel.loadPreviousPageFromHistory();
        }
        else if (buttonText.equals("next"))
        {
            chatPanel.loadNextPageFromHistory();
        }
        else if (buttonText.equals("sendFile"))
        {
            SipCommFileChooser scfc = GenericFileDialog.create(
                null, "Send file...", SipCommFileChooser.LOAD_FILE_OPERATION,
                    ConfigurationUtils.getSendFileLastDir());
            File selectedFile = scfc.getFileFromDialog();
            if(selectedFile != null)
            {
                ConfigurationUtils.setSendFileLastDir(
                    selectedFile.getParent());
                chatContainer.getCurrentChat().sendFile(selectedFile);
            }
        }
        else if (buttonText.equals("invite"))
        {
            ChatInviteDialog inviteDialog = new ChatInviteDialog(chatPanel);

            inviteDialog.setVisible(true);
        }
        else if (buttonText.equals("leave"))
        {
            ChatRoomWrapper chatRoomWrapper 
                = (ChatRoomWrapper)chatPanel.getChatSession().getDescriptor();
            ChatRoomWrapper leavedRoomWrapped 
                = GuiActivator.getMUCService().leaveChatRoom(
                    chatRoomWrapper);
        }
        else if (buttonText.equals("call"))
        {
            call(false, false);
        }
        else if (buttonText.equals("callVideo"))
        {
            call(true, false);
        }
        else if (buttonText.equals("desktop"))
        {
            call(true, true);
        }
        else if (buttonText.equals("options"))
        {
            GuiActivator.getUIService()
                .getConfigurationContainer().setVisible(true);
        }
        else if (buttonText.equals("font"))
            chatPanel.showFontChooserDialog();
        else if (buttonText.equals("createConference"))
        {
            chatPanel.showChatConferenceDialog();
        }
    }

    /**
     * Returns the button used to show the history window.
     *
     * @return the button used to show the history window.
     */
    public HistorySelectorBox getHistoryButton()
    {
        return historyButton;
    }

    /**
     * Get the smileys box.
     *
     * @return the smileys box
     */
    public SmileysSelectorBox getSmileysBox()
    {
        return smileysBox;
    }

    /**
     * Disables/Enables history arrow buttons depending on whether the
     * current page is the first, the last page or a middle page.
     * @param chatPanel the <tt>ChatPanel</tt> which has provoked the change.
     */
    public void changeHistoryButtonsState(ChatPanel chatPanel)
    {
        ChatConversationPanel convPanel = chatPanel.getChatConversationPanel();

        long firstMsgInHistory = chatPanel
            .getFirstHistoryMsgTimestamp().getTime();
        long lastMsgInHistory = chatPanel
            .getLastHistoryMsgTimestamp().getTime();
        Date firstMsgInPage = convPanel.getPageFirstMsgTimestamp();
        Date lastMsgInPage = convPanel.getPageLastMsgTimestamp();

        if(firstMsgInHistory == 0 || lastMsgInHistory == 0)
        {
            previousButton.setEnabled(false);
            nextButton.setEnabled(false);
            return;
        }

        previousButton.setEnabled(firstMsgInHistory < firstMsgInPage.getTime());

        nextButton
            .setEnabled(
                (lastMsgInPage.getTime() > 0)
                    && (lastMsgInHistory > lastMsgInPage.getTime()));
    }

    /**
     * Sets the current <tt>ChatSession</tt> made known to this instance by the
     * last call to its {@link #chatChanged(ChatPanel)}.
     *
     * @param chatSession the <tt>ChatSession</tt> to become current for this
     * instance
     */
    private void setChatSession(ChatSession chatSession)
    {
        if (this.chatSession != chatSession)
        {
            if (this.chatSession instanceof MetaContactChatSession)
                this.chatSession.removeChatTransportChangeListener(this);

            if(this.chatSession instanceof ConferenceChatSession)
                ((ConferenceChatSession) this.chatSession)
                    .removeLocalUserRoleListener(this);

            this.chatSession = chatSession;

            if (this.chatSession instanceof MetaContactChatSession)
                this.chatSession.addChatTransportChangeListener(this);

            if(this.chatSession instanceof ConferenceChatSession)
                ((ConferenceChatSession) this.chatSession)
                    .addLocalUserRoleListener(this);
        }
    }

    /**
     * Adds a separator to this tool bar.
     */
    private void addSeparator()
    {
        this.add(new JSeparator(SwingConstants.VERTICAL));
    }

    /**
     * Reloads icons for buttons.
     */
    public void loadSkin()
    {
        inviteButton.setIconImage(ImageLoader.getImage(
                ImageLoader.ADD_TO_CHAT_ICON));

        historyButton.loadSkin();

        sendFileButton.setIconImage(ImageLoader.getImage(
                ImageLoader.SEND_FILE_ICON));

        fontButton.setIconImage(ImageLoader.getImage(
                ImageLoader.FONT_ICON));

        previousButton.setIconImage(ImageLoader.getImage(
                ImageLoader.PREVIOUS_ICON));

        nextButton.setIconImage(ImageLoader.getImage(
                ImageLoader.NEXT_ICON));

        leaveChatRoomButton.setIconImage(ImageLoader.getImage(
                ImageLoader.LEAVE_ICON));

        desktopSharingButton.setIconImage(ImageLoader.getImage(
            ImageLoader.CHAT_DESKTOP_SHARING));

        optionsButton.setIconImage(ImageLoader.getImage(
                ImageLoader.CHAT_CONFIGURE_ICON));
        
        setCallButtonsIcons();
    }

    /**
     * Establishes a call.
     *
     * @param isVideo indicates if a video call should be established.
     * @param isDesktopSharing indicates if a desktopSharing should be
     * established.
     */
    private void call(boolean isVideo, boolean isDesktopSharing)
    {
        ChatPanel chatPanel = chatContainer.getCurrentChat();

        ChatSession chatSession = chatPanel.getChatSession();

        Class<? extends OperationSet> opSetClass;
        if(isVideo)
        {
            if(isDesktopSharing)
                opSetClass = OperationSetDesktopStreaming.class;
            else
                opSetClass = OperationSetVideoTelephony.class;
        }
        else
            opSetClass = OperationSetBasicTelephony.class;

        List<ChatTransport> telTransports = null;
        if (chatSession != null)
            telTransports = chatSession
                .getTransportsForOperationSet(opSetClass);

        List<ChatTransport> contactOpSetSupported;

        contactOpSetSupported =
            getOperationSetForCapabilities(telTransports, opSetClass);

        List<UIContactDetail> res = new ArrayList<UIContactDetail>();
        for(ChatTransport ct : contactOpSetSupported)
        {
            HashMap<Class<? extends OperationSet>, ProtocolProviderService> m =
                new HashMap<Class<? extends OperationSet>,
                            ProtocolProviderService>();
            m.put(opSetClass, ct.getProtocolProvider());

            UIContactDetailImpl d = new UIContactDetailImpl(
                                                ct.getName(),
                                                ct.getDisplayName(),
                                                null,
                                                null,
                                                null,
                                                m,
                                                null,
                                                ct.getName());
            PresenceStatus status = ct.getStatus();
            byte[] statusIconBytes = status.getStatusIcon();

            if (statusIconBytes != null && statusIconBytes.length > 0)
            {
                d.setStatusIcon(new ImageIcon(
                    ImageLoader.getIndexedProtocolImage(
                        ImageUtils.getBytesInImage(statusIconBytes),
                        ct.getProtocolProvider())));
            }

            res.add(d);
        }

        Point location = new Point(callButton.getX(),
            callButton.getY() + callButton.getHeight());

        SwingUtilities.convertPointToScreen(
            location, this);

        MetaContact metaContact
            = GuiActivator.getUIService().getChatContact(chatPanel);
        UIContactImpl uiContact = null;
        if (metaContact != null)
            uiContact = MetaContactListSource.getUIContact(metaContact);

        CallManager.call(
            res,
            uiContact,
            isVideo,
            isDesktopSharing,
            callButton,
            location);
    }

    /**
     * Sets the names of the call buttons depending on the chat session type.
     */
    private void setCallButtonsName()
    {
        if(chatSession instanceof ConferenceChatSession)
        {
            callButton.setName("createConference");
            callVideoButton.setName("createConference");
            this.callButton.setToolTipText(
                GuiActivator.getResources().getI18NString(
                    "service.gui.CREATE_JOIN_VIDEO_CONFERENCE"));

            this.callVideoButton.setToolTipText(
                GuiActivator.getResources().getI18NString(
                    "service.gui.CREATE_JOIN_VIDEO_CONFERENCE"));
        }
        else
        {
            callButton.setName("call");
            callVideoButton.setName("callVideo");
            this.callButton.setToolTipText(
                GuiActivator.getResources().getI18NString(
                    "service.gui.CALL_CONTACT"));

            this.callVideoButton.setToolTipText(
                GuiActivator.getResources().getI18NString(
                    "service.gui.CALL_CONTACT"));
        }
    }

    /**
     * Sets the icons of the call buttons depending on the chat session type.
     */
    private void setCallButtonsIcons()
    {
        if(chatSession instanceof ConferenceChatSession)
        {
            callButton.setIconImage(ImageLoader.getImage(
                ImageLoader.CHAT_ROOM_CALL));
            callVideoButton.setIconImage(ImageLoader.getImage(
                ImageLoader.CHAT_ROOM_VIDEO_CALL));
            callButton.setPreferredSize(new Dimension(29, 25));
            callVideoButton.setPreferredSize(new Dimension(29, 25));
        }
        else
        {
            callButton.setIconImage(ImageLoader.getImage(
                ImageLoader.CHAT_CALL));
            callVideoButton.setIconImage(ImageLoader.getImage(
                ImageLoader.CHAT_VIDEO_CALL));
            callButton.setPreferredSize(new Dimension(25, 25));
            callVideoButton.setPreferredSize(new Dimension(25, 25));
        }
        callButton.repaint();
        callVideoButton.repaint();
    }

    /**
     * Fired when local user role has changed.
     * @param evt the <tt>ChatRoomLocalUserRoleChangeEvent</tt> instance
     */
    @Override
    public void localUserRoleChanged(ChatRoomLocalUserRoleChangeEvent evt)
    {
        updateInviteContactButton();
    }

    /**
     * Updates invite contact button depending on the user role we have.
     */
    private void updateInviteContactButton()
    {
        if(chatSession instanceof ConferenceChatSession)
        {
            ChatRoomMemberRole role =
                ((ChatRoomWrapper)chatSession.getDescriptor())
                    .getChatRoom().getUserRole();

            // it means we are at least a moderator
            inviteButton.setEnabled(role.getRoleIndex() >= 50);
        }
    }

    /**
     * Searches for phone numbers in <tt>MetaContact/tt> operation sets.
     * And changes the call button enable/disable state.
     */
    private class UpdateCallButtonWorker
        extends SwingWorker
    {
        /**
         * The current contact.
         */
        private MetaContact contact;

        /**
         * Has this contact any phone.
         */
        private boolean isCallEnabled = false;

        /**
         * Has this contact any video phone.
         */
        private boolean isVideoCallEnabled = false;

        /**
         * Has this contact has desktop sharing enabled.
         */
        private boolean isDesktopSharingEnabled = false;

        /**
         * Creates worker.
         * @param contact
         */
        UpdateCallButtonWorker(MetaContact contact)
        {
            this.contact = contact;
        }

        /**
         * Executes in worker thread.
         * @return
         * @throws Exception
         */
        @Override
        protected Object construct()
            throws
            Exception
        {
            contactPhoneUtil = MetaContactPhoneUtil.getPhoneUtil(contact);

            isCallEnabled = contactPhoneUtil.isCallEnabled();
            isVideoCallEnabled = contactPhoneUtil.isVideoCallEnabled();
            isDesktopSharingEnabled = contactPhoneUtil.isDesktopSharingEnabled();

            return null;
        }

        /**
         * Called on the event dispatching thread (not on the worker thread)
         * after the <code>construct</code> method has returned.
         */
        @Override
        protected void finished()
        {
            callButton.setEnabled(isCallEnabled);
            callVideoButton.setEnabled(isVideoCallEnabled);
            desktopSharingButton.setEnabled(isDesktopSharingEnabled);
        }

    }
}
