/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
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
import net.java.sip.communicator.impl.gui.main.chat.history.*;
import net.java.sip.communicator.impl.gui.main.configforms.*;
import net.java.sip.communicator.impl.gui.main.contactlist.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.Container;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.FaxDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.GenericDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.MobilePhoneDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.PagerDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.PhoneNumberDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.WorkPhoneDetail;
import net.java.sip.communicator.util.skin.*;
import net.java.sip.communicator.util.swing.*;

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
    private final ChatToolbarButton historyButton
        = new ChatToolbarButton(
                ImageLoader.getImage(ImageLoader.HISTORY_ICON));

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
     * Creates an instance and constructs the <tt>MainToolBar</tt>.
     *
     * @param chatContainer The parent <tt>ChatWindow</tt>.
     */
    public MainToolBar(ChatContainer chatContainer)
    {
        this.chatContainer = chatContainer;

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
        if(!ConfigurationManager.isLeaveChatRoomOnWindowCloseEnabled())
            this.add(leaveChatRoomButton);

        this.add(callButton);
        this.add(desktopSharingButton);
        this.add(sendFileButton);

        ChatPanel chatPanel = chatContainer.getCurrentChat();
        if (chatPanel == null
            || !(chatPanel.getChatSession() instanceof MetaContactChatSession))
            sendFileButton.setEnabled(false);

        this.addSeparator();

        this.add(historyButton);
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

        if(ConfigurationManager.isFontSupportEnabled())
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

        this.callButton.setName("call");
        this.callButton.setToolTipText(
            GuiActivator.getResources().getI18NString(
                "service.gui.CALL_CONTACT"));

        this.desktopSharingButton.setName("desktop");
        this.desktopSharingButton.setToolTipText(
            GuiActivator.getResources().getI18NString(
                "service.gui.SHARE_DESKTOP_WITH_CONTACT"));

        this.historyButton.setName("history");
        this.historyButton.setToolTipText(
            GuiActivator.getResources().getI18NString("service.gui.HISTORY")
            + " Ctrl-H");

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
        desktopSharingButton.addActionListener(this);
        historyButton.addActionListener(this);
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

            setChatSession(chatPanel.chatSession);

            leaveChatRoomButton.setEnabled(
                chatPanel.chatSession instanceof ConferenceChatSession);

            inviteButton.setEnabled(
                chatPanel.findInviteChatTransport() != null);

            sendFileButton.setEnabled(
                chatPanel.findFileTransferChatTransport() != null);

            boolean hasPhone = false;
            boolean hasTelephony =
                !getOperationSetForCapabilities(
                    chatPanel.chatSession.getTransportsForOperationSet(
                        OperationSetBasicTelephony.class),
                        OperationSetBasicTelephony.class).isEmpty();

            if(!hasTelephony && contact != null)
            {
                Iterator<Contact> contacts = contact.getContacts();
                while(contacts.hasNext())
                {
                    Contact c = contacts.next();
                    
                    if(!c.getProtocolProvider().isRegistered())
                        continue;
                    
                    OperationSetServerStoredContactInfo infoOpSet =
                        c.getProtocolProvider().getOperationSet(
                            OperationSetServerStoredContactInfo.class);
                    Iterator<GenericDetail> details;

                    if(infoOpSet != null)
                    {
                        details = infoOpSet.requestAllDetailsForContact(c,
                            new DetailsListener(
                                    chatPanel.chatSession, callButton));

                        if(details != null)
                        {
                            while(details.hasNext())
                            {
                                GenericDetail d = details.next();
                                if(d instanceof PhoneNumberDetail &&
                                    !(d instanceof PagerDetail) &&
                                    !(d instanceof FaxDetail))
                                {
                                    PhoneNumberDetail pnd = (PhoneNumberDetail)d;
                                    if(pnd.getNumber() != null &&
                                        pnd.getNumber().length() > 0)
                                    {
                                        hasPhone = true;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            callButton.setEnabled(hasTelephony || hasPhone);
            desktopSharingButton.setEnabled(!getOperationSetForCapabilities(
                chatPanel.chatSession.getTransportsForOperationSet(
                    OperationSetDesktopSharingServer.class),
                    OperationSetDesktopSharingServer.class).isEmpty());

            changeHistoryButtonsState(chatPanel);
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
                c.setCurrentContact(contact);
        }
    }

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
                    ConfigurationManager.getSendFileLastDir());
            File selectedFile = scfc.getFileFromDialog();
            if(selectedFile != null)
            {
                ConfigurationManager.setSendFileLastDir(
                    selectedFile.getParent());
                chatContainer.getCurrentChat().sendFile(selectedFile);
            }
        }
        else if (buttonText.equals("history"))
        {
            HistoryWindow history;

            HistoryWindowManager historyWindowManager
                = GuiActivator.getUIService().getHistoryWindowManager();

            ChatSession chatSession = chatPanel.getChatSession();

            if(historyWindowManager
                .containsHistoryWindowForContact(chatSession.getDescriptor()))
            {
                history = historyWindowManager
                    .getHistoryWindowForContact(chatSession.getDescriptor());

                if(history.getState() == JFrame.ICONIFIED)
                    history.setState(JFrame.NORMAL);

                history.toFront();
            }
            else
            {
                history = new HistoryWindow(
                    chatPanel.getChatSession().getDescriptor());

                history.setVisible(true);

                historyWindowManager
                    .addHistoryWindowForContact(chatSession.getDescriptor(),
                                                    history);
            }
        }
        else if (buttonText.equals("invite"))
        {
            ChatInviteDialog inviteDialog = new ChatInviteDialog(chatPanel);

            inviteDialog.setVisible(true);
        }
        else if (buttonText.equals("leave"))
        {
            ConferenceChatManager conferenceManager
                = GuiActivator.getUIService().getConferenceChatManager();
            conferenceManager.leaveChatRoom(
                (ChatRoomWrapper)chatPanel.getChatSession().getDescriptor());
        }
        else if (buttonText.equals("call"))
        {
            ChatSession chatSession = chatPanel.getChatSession();

            List<ChatTransport> telTransports = null;
            if (chatSession != null)
                telTransports = chatSession
                    .getTransportsForOperationSet(
                        OperationSetBasicTelephony.class);

            List<ChatTransport> contactOpSetSupported =
                getOperationSetForCapabilities(telTransports,
                        OperationSetBasicTelephony.class);
            
            MetaContact metaContact
                = GuiActivator.getUIService().getChatContact(chatPanel);
            Iterator<Contact> contacts = metaContact.getContacts(); 
            List<UIContactDetail> phones = new ArrayList<UIContactDetail>();
        
            while(contacts.hasNext())
            {
                Contact contact = contacts.next();
                
                OperationSetServerStoredContactInfo infoOpSet =
                    contact.getProtocolProvider().getOperationSet(
                        OperationSetServerStoredContactInfo.class);
                Iterator<GenericDetail> details = null;

                if(infoOpSet != null)
                {
                    details = infoOpSet.getAllDetailsForContact(contact);

                    while(details.hasNext())
                    {
                        GenericDetail d = details.next();
                        if(d instanceof PhoneNumberDetail && 
                            !(d instanceof PagerDetail) && 
                            !(d instanceof FaxDetail))
                        {
                            PhoneNumberDetail pnd = (PhoneNumberDetail)d;
                            if(pnd.getNumber() != null &&
                                pnd.getNumber().length() > 0)
                            {
                                String localizedType = null;
                                
                                if(d instanceof WorkPhoneDetail)
                                {
                                    localizedType = 
                                        GuiActivator.getResources().
                                            getI18NString(
                                                "service.gui.WORK_PHONE");
                                }
                                else if(d instanceof MobilePhoneDetail)
                                {
                                    localizedType = 
                                        GuiActivator.getResources().
                                            getI18NString(
                                                "service.gui.MOBILE_PHONE");
                                }
                                else
                                {
                                    localizedType = 
                                        GuiActivator.getResources().
                                            getI18NString(
                                                "service.gui.PHONE");
                                }
                                    
                                UIContactDetail cd =
                                    new UIContactDetail(
                                        pnd.getNumber(),
                                        pnd.getNumber() + 
                                        " (" + localizedType + ")",
                                        null,
                                        new ArrayList<String>(),
                                        null,
                                        null,
                                        null)
                                {
                                    public PresenceStatus getPresenceStatus()
                                    {
                                        return null;
                                    }
                                };
                                phones.add(cd);
                            }
                        }
                    }
                }
            }
            
            if (telTransports != null || phones.size() > 0)
            {
                if (contactOpSetSupported.size() == 1 && phones.size() == 0)
                {
                    ChatTransport transport = contactOpSetSupported.get(0);
                    CallManager.createCall(
                        transport.getProtocolProvider(),
                        transport.getName());
                }
                else if (contactOpSetSupported.size() == 0
                    && phones.size() == 1)
                {
                    UIContactDetail detail = phones.get(0);

                    ProtocolProviderService preferredProvider
                        = detail.getPreferredProtocolProvider(
                            OperationSetBasicTelephony.class);

                    List<ProtocolProviderService> providers
                        = GuiActivator.getOpSetRegisteredProviders(
                            OperationSetBasicTelephony.class,
                            preferredProvider,
                            detail.getPreferredProtocol(
                                OperationSetBasicTelephony.class));

                    if (providers != null)
                    {
                        int providersCount = providers.size();

                        if (providersCount <= 0)
                        {
                            new ErrorDialog(null,
                                GuiActivator.getResources().getI18NString(
                                    "service.gui.CALL_FAILED"),
                                GuiActivator.getResources().getI18NString(
                                    "service.gui.NO_ONLINE_TELEPHONY_ACCOUNT"))
                            .showDialog();
                        }
                        else if (providersCount == 1)
                        {
                            CallManager.createCall(
                                providers.get(0), detail.getAddress());
                        }
                        else if (providersCount > 1)
                        {
                            ChooseCallAccountPopupMenu chooseAccountDialog = 
                                new ChooseCallAccountPopupMenu(
                                    callButton, detail.getAddress(), providers);
                            Point location = new Point(callButton.getX(),
                                callButton.getY() + callButton.getHeight());

                            SwingUtilities.convertPointToScreen(
                                location, this);

                            chooseAccountDialog
                                .showPopupMenu(location.x, location.y);
                        }
                    }
                }
                else if ((contactOpSetSupported.size() + phones.size()) > 1)
                {
                    List<Object> allContacts = new ArrayList<Object>();
                    for(ChatTransport t : contactOpSetSupported)
                    {
                        allContacts.add(t);
                    }
                    for(UIContactDetail t : phones)
                    {
                        allContacts.add(t);
                    }
                                        
                    ChooseCallAccountPopupMenu chooseAccountDialog
                        = new ChooseCallAccountPopupMenu(
                            callButton,
                            allContacts);

                    Point location = new Point(callButton.getX(),
                        callButton.getY() + callButton.getHeight());

                    SwingUtilities.convertPointToScreen(
                        location, this);

                    chooseAccountDialog
                        .showPopupMenu(location.x, location.y);
                }
            }
        }
        else if (buttonText.equals("desktop"))
        {
            ChatSession chatSession = chatPanel.getChatSession();

            List<ChatTransport> desktopTransports = null;
            if (chatSession != null)
                desktopTransports = chatSession
                    .getTransportsForOperationSet(
                        OperationSetDesktopSharingServer.class);

            List<ChatTransport> contactOpSetSupported =
                getOperationSetForCapabilities(desktopTransports,
                        OperationSetDesktopSharingServer.class);

            if (desktopTransports != null)
            {
                if (contactOpSetSupported.size() == 1)
                {
                    ChatTransport transport = contactOpSetSupported.get(0);
                    CallManager.createDesktopSharing(
                        transport.getProtocolProvider(),
                        transport.getName());
                }
                else if (contactOpSetSupported.size() > 1)
                {
                    ChooseCallAccountPopupMenu chooseAccountDialog
                        = new ChooseCallAccountPopupMenu(
                            desktopSharingButton,
                            contactOpSetSupported,
                            OperationSetDesktopSharingServer.class);

                    Point location = new Point(callButton.getX(),
                        desktopSharingButton.getY()
                        + desktopSharingButton.getHeight());

                    SwingUtilities.convertPointToScreen(
                        location, this);

                    chooseAccountDialog
                        .showPopupMenu(location.x, location.y);
                }
            }
        }
        else if (buttonText.equals("options"))
        {
            GuiActivator.getUIService()
                .getConfigurationContainer().setVisible(true);
        }
        else if (buttonText.equals("font"))
            chatPanel.showFontChooserDialog();
    }

    /**
     * Returns the button used to show the history window.
     *
     * @return the button used to show the history window.
     */
    public ChatToolbarButton getHistoryButton()
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

        long firstMsgInHistory = chatPanel.getFirstHistoryMsgTimestamp();
        long lastMsgInHistory = chatPanel.getLastHistoryMsgTimestamp();
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

            this.chatSession = chatSession;

            if (this.chatSession instanceof MetaContactChatSession)
                this.chatSession.addChatTransportChangeListener(this);
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

        historyButton.setIconImage(ImageLoader.getImage(
                ImageLoader.HISTORY_ICON));

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

        callButton.setIconImage(ImageLoader.getImage(
                ImageLoader.CHAT_CALL));

        desktopSharingButton.setIconImage(ImageLoader.getImage(
            ImageLoader.CHAT_DESKTOP_SHARING));

        optionsButton.setIconImage(ImageLoader.getImage(
                ImageLoader.CHAT_CONFIGURE_ICON));
    }

    /**
     * Listens for responses if later received deliver them.
     * If meanwhile chat session has been changed ignore any events.
     */
    private class DetailsListener
        implements OperationSetServerStoredContactInfo.DetailsResponseListener
    {
        private ChatSession source;
        private JButton callButton;

        /**
         * Creates listener.
         * @param chatSession the source chat session.
         * @param callButton the call button.
         */
        DetailsListener(ChatSession chatSession, JButton callButton)
        {
            this.source = chatSession;
            this.callButton = callButton;
        }

        /**
         * Details has been retrieved.
         * @param details the details retrieved if any.
         */
        public void detailsRetrieved(Iterator<GenericDetail> details)
        {
            if(!source.equals(chatSession))
                return;

            if(callButton.isEnabled())
                return;

            while(details.hasNext())
            {
                GenericDetail d = details.next();
                if(d instanceof PhoneNumberDetail &&
                    !(d instanceof PagerDetail) &&
                    !(d instanceof FaxDetail))
                {
                    PhoneNumberDetail pnd = (PhoneNumberDetail)d;
                    if(pnd.getNumber() != null &&
                        pnd.getNumber().length() > 0)
                    {
                        callButton.setEnabled(true);
                        return;
                    }
                }
            }
        }
    }

}
