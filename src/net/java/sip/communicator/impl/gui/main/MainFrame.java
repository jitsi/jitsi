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
package net.java.sip.communicator.impl.gui.main;

import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.util.*;
import java.util.List;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.event.*;
import net.java.sip.communicator.impl.gui.main.call.*;
import net.java.sip.communicator.impl.gui.main.chat.conference.*;
import net.java.sip.communicator.impl.gui.main.contactlist.*;
import net.java.sip.communicator.impl.gui.main.menus.*;
import net.java.sip.communicator.impl.gui.main.presence.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.plugin.desktoputil.event.*;
import net.java.sip.communicator.service.contacteventhandler.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.Container;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.OperationSetMessageWaiting.MessageType;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.Logger;
import net.java.sip.communicator.util.skin.*;

import org.jitsi.service.configuration.*;
import org.jitsi.service.resources.*;
import org.jitsi.util.*;
import org.osgi.framework.*;

import com.explodingpixels.macwidgets.*;

/**
 * The main application window. This class is the core of this UI
 * implementation. It stores all available protocol providers and their
 * operation sets, as well as all registered accounts, the
 * <tt>MetaContactListService</tt> and all sent messages that aren't
 * delivered yet.
 *
 * @author Yana Stamcheva
 * @author Lubomir Marinov
 * @author Adam Netocny
 */
public class MainFrame
    extends SIPCommFrame
    implements  ContactListContainer,
                ExportedWindow,
                PluginComponentListener,
                Skinnable
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The logger.
     */
    private final Logger logger = Logger.getLogger(MainFrame.class);

    /**
     * The main container.
     */
    private final TransparentPanel mainPanel
        = new TransparentPanel(new BorderLayout(0, 0));

    /**
     * The status bar panel.
     */
    private final TransparentPanel statusBarPanel
        = new TransparentPanel(new BorderLayout());

    /**
     * The center panel, containing the contact list.
     */
    private final TransparentPanel centerPanel
        = new TransparentPanel(new BorderLayout(0, 0));

    /**
     * The main menu.
     */
    private MainMenu menu;

    /**
     * The search field shown above the contact list.
     */
    private SearchField searchField;

    /**
     * A mapping of <tt>ProtocolProviderService</tt>s and their indexes.
     */
    private final HashMap<ProtocolProviderService, Integer> protocolProviders
        = new LinkedHashMap<ProtocolProviderService, Integer>();

    /**
     * The panel containing the accounts status menu.
     */
    private AccountStatusPanel accountStatusPanel;

    /**
     * The panel replacing the contact list, shown when no matching is found
     * for the search filter.
     */
    private UnknownContactPanel unknownContactPanel;

    /**
     * A mapping of <tt>ProtocolProviderService</tt>s and corresponding
     * <tt>ContactEventHandler</tt>s.
     */
    private final Map<ProtocolProviderService, ContactEventHandler>
        providerContactHandlers =
            new Hashtable<ProtocolProviderService, ContactEventHandler>();

    /**
     * A mapping of plug-in components and their corresponding native components.
     */
    private final List<PluginComponentFactory> nativePluginsTable =
        new ArrayList<PluginComponentFactory>();

    /**
     * The north plug-in panel.
     */
    private final JPanel pluginPanelNorth = new TransparentPanel();

    /**
     * The south plug-in panel.
     */
    private final JPanel pluginPanelSouth = new TransparentPanel();

    /**
     * The west plug-in panel.
     */
    private final JPanel pluginPanelWest = new TransparentPanel();

    /**
     * The east plug-in panel.
     */
    private final JPanel pluginPanelEast = new TransparentPanel();

    /**
     * The container containing the contact list.
     */
    private ContactListPane contactListPanel;

    /**
     * The user interface provider presence listener.
     */
    private ProviderPresenceStatusListener uiProviderPresenceListener;

    /**
     * The user interface call listener.
     */
    private CallListener uiCallListener;

    /**
     * Contact list search key dispatcher;
     */
    private final ContactListSearchKeyDispatcher clKeyDispatcher;

    /**
     * The keyboard focus manager.
     */
    final KeyboardFocusManager keyManager;

    /**
     * Creates an instance of <tt>MainFrame</tt>.
     */
    public MainFrame()
    {
        if (!ConfigurationUtils.isWindowDecorated())
        {
            this.setUndecorated(true);
        }

        this.contactListPanel = new ContactListPane(this);

        this.accountStatusPanel = new AccountStatusPanel();

        this.searchField = new SearchField( this,
                                            TreeContactList.searchFilter,
                                            true,
                                            true);

        menu = new MainMenu(this);

        this.initTitleFont();

        ResourceManagementService resources = GuiActivator.getResources();
        String applicationName
            = resources.getSettingsString("service.gui.APPLICATION_NAME");

        this.setTitle(applicationName);

        // sets the title to application name
        // fix for some windows managers(gnome3)
        try
        {
            Toolkit xToolkit = Toolkit.getDefaultToolkit();
            java.lang.reflect.Field awtAppClassNameField =
            xToolkit.getClass().getDeclaredField("awtAppClassName");
            awtAppClassNameField.setAccessible(true);
            awtAppClassNameField.set(xToolkit, applicationName);
        }
        catch(Throwable t)
        {
            // we do nothing for it
        }

        this.mainPanel.setBackground(new Color(
                GuiActivator.getResources()
                    .getColor("service.gui.MAIN_WINDOW_BACKGROUND")));

        keyManager = KeyboardFocusManager.getCurrentKeyboardFocusManager();

        clKeyDispatcher = new ContactListSearchKeyDispatcher(   keyManager,
                                                                searchField,
                                                                this);
        keyManager.addKeyEventDispatcher(clKeyDispatcher);

        /*
         * If the application is configured to quit when this frame is closed,
         * do so.
         */
        this.addWindowListener(new WindowAdapter()
        {
            /**
             * Invoked when a window has been closed.
             */
            @Override
            public void windowClosed(WindowEvent event)
            {
                MainFrame.this.windowClosed(event);
            }

            /**
             * Invoked when a window has been opened.
             */
            @Override
            public void windowOpened(WindowEvent e)
            {
                Window focusedWindow = keyManager.getFocusedWindow();

                // If there's no other focused window we request the focus
                // in the contact list.
                if (focusedWindow == null)
                {
                    requestFocusInContactList();
                }
                // If some other window keeps the focus we'll wait until it's
                // closed.
                else if (!focusedWindow.equals(MainFrame.this))
                {
                    requestFocusLater(focusedWindow);
                }
            }
        });

        this.init();

        this.initPluginComponents();
    }

    /**
     * Adds a WindowListener to the given <tt>focusedWindow</tt> and once it's
     * closed we check again if we can request the focus.
     *
     * @param focusedWindow the currently focused window
     */
    private void requestFocusLater(Window focusedWindow)
    {
        focusedWindow.addWindowListener(new WindowAdapter()
        {
            /**
             * Invoked when a window has been closed.
             */
            @Override
            public void windowClosed(WindowEvent event)
            {
                event.getWindow().removeWindowListener(this);

                Window focusedWindow = keyManager.getFocusedWindow();

                // If the focused window is null or it's the shared owner frame,
                // which keeps focus for closed dialogs without owner, we'll
                // request the focus in the contact list.
                if (focusedWindow == null
                    || focusedWindow.getClass().getName().equals(
                        "javax.swing.SwingUtilities$SharedOwnerFrame"))
                {
                    requestFocusInContactList();
                }
                else if (!focusedWindow.equals(MainFrame.this))
                {
                    requestFocusLater(focusedWindow);
                }
            }
        });
    }

    /**
     * Requests the focus in the center panel, which contains either the
     * contact list or the unknown contact panel.
     */
    public void requestFocusInContactList()
    {
        centerPanel.requestFocusInWindow();
        GuiActivator.getContactList().requestFocus();
    }

    /**
     * Initiates the content of this frame.
     */
    private void init()
    {
        setDefaultCloseOperation(
            GuiActivator.getUIService().getExitOnMainWindowClose()
                ? JFrame.DISPOSE_ON_CLOSE
                : JFrame.HIDE_ON_CLOSE);

        registerKeyActions();

        JComponent northPanel = createTopComponent();

        this.setJMenuBar(menu);

        TransparentPanel searchPanel
            = new TransparentPanel(new BorderLayout(5, 0));

        searchPanel.add(searchField);
        searchPanel.add(new DialPadButton(), BorderLayout.WEST);

        if(!GuiActivator.getConfigurationService().getBoolean(
            "net.java.sip.communicator.impl.gui.CALL_HISTORY_BUTTON_DISABLED",
            false))
        {
            searchPanel.add(createButtonPanel(), BorderLayout.EAST);
        }

        northPanel.add(accountStatusPanel, BorderLayout.CENTER);
        northPanel.add(searchPanel, BorderLayout.SOUTH);

        centerPanel.add(contactListPanel, BorderLayout.CENTER);

        this.mainPanel.add(northPanel, BorderLayout.NORTH);

        SingleWindowContainer singleWContainer
            = GuiActivator.getUIService().getSingleWindowContainer();

        this.mainPanel.add(centerPanel, BorderLayout.CENTER);

        if (singleWContainer != null)
        {
            JSplitPane topSplitPane
                = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
            topSplitPane.setBorder(null); // remove default borders
            topSplitPane.setOneTouchExpandable(true);
            topSplitPane.setResizeWeight(0);
            topSplitPane.setOpaque(false);
            topSplitPane.setDividerLocation(200);

            topSplitPane.add(mainPanel);
            topSplitPane.add(singleWContainer);

            getContentPane().add(topSplitPane, BorderLayout.CENTER);
            getContentPane().add(statusBarPanel, BorderLayout.SOUTH);
        }
        else
        {
            java.awt.Container contentPane = getContentPane();
            contentPane.add(mainPanel, BorderLayout.CENTER);
            contentPane.add(statusBarPanel, BorderLayout.SOUTH);
        }
    }

    private Component createButtonPanel()
    {
        boolean isCallButtonEnabled = false;

        // Indicates if the big call button outside the search is enabled.
        String callButtonEnabledString = GuiActivator.getResources()
            .getSettingsString("impl.gui.CALL_BUTTON_ENABLED");

        if (callButtonEnabledString != null
                && callButtonEnabledString.length() > 0)
        {
            isCallButtonEnabled
                = new Boolean(callButtonEnabledString).booleanValue();
        }

        CallHistoryButton historyButton = new CallHistoryButton();
        if (isCallButtonEnabled)
        {
            JPanel panel
                = new TransparentPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));

            panel.add(new CallButton(this));
            panel.add(historyButton);

            return panel;
        }
        else
            return historyButton;
    }

    /**
     * Creates the toolbar panel for this chat window, depending on the current
     * operating system.
     *
     * @return the created toolbar
     */
    private JComponent createTopComponent()
    {
        JComponent topComponent = null;

        if (OSUtils.IS_MAC)
        {
            UnifiedToolBar macToolbarPanel = new UnifiedToolBar();

            MacUtils.makeWindowLeopardStyle(getRootPane());

            macToolbarPanel.getComponent().setLayout(new BorderLayout(5, 5));
            macToolbarPanel.getComponent()
                .setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 5));
            macToolbarPanel.disableBackgroundPainter();
            macToolbarPanel.installWindowDraggerOnWindow(this);

            // Set the color of the center panel.
            centerPanel.setOpaque(true);
            centerPanel.setBackground(
                new Color(GuiActivator.getResources()
                    .getColor("service.gui.MAC_PANEL_BACKGROUND")));

            topComponent = macToolbarPanel.getComponent();
        }
        else
        {
            JPanel panel = new TransparentPanel(new BorderLayout(5, 5));

            panel.setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 5));

            topComponent = panel;
        }

        return topComponent;
    }

    /**
     * Sets frame size and position.
     */
    public void initBounds()
    {
        int width = GuiActivator.getResources()
            .getSettingsInt("impl.gui.MAIN_WINDOW_WIDTH");

        int height = GuiActivator.getResources()
            .getSettingsInt("impl.gui.MAIN_WINDOW_HEIGHT");

        int minWidth = GuiActivator.getResources()
            .getSettingsInt("impl.gui.MAIN_WINDOW_MIN_WIDTH");

        int minHeight = GuiActivator.getResources()
            .getSettingsInt("impl.gui.MAIN_WINDOW_MIN_HEIGHT");

        this.getContentPane().setMinimumSize(new Dimension(minWidth, minHeight));

        this.setSize(width, height);

        this.setLocation(Toolkit.getDefaultToolkit().getScreenSize().width
                - this.getWidth(), 50);
    }

    /**
     * Initialize main window font.
     */
    private void initTitleFont()
    {
        JComponent layeredPane = this.getLayeredPane();

        ResourceManagementService resources = GuiActivator.getResources();
        String fontName = resources.getSettingsString("service.gui.FONT_NAME");
        int fontSize = resources.getSettingsInt("service.gui.FONT_SIZE");
        Font font = new Font(fontName, Font.BOLD, fontSize);

        for (int i = 0, componentCount = layeredPane.getComponentCount();
                i < componentCount;
                i++)
            layeredPane.getComponent(i).setFont(font);
    }

    /**
     * Enters or exits the "unknown contact" view. This view will propose to
     * the user some specific operations if the current filter doesn't match
     * any contacts.
     * @param isEnabled <tt>true</tt> to enable the "unknown contact" view,
     * <tt>false</tt> - otherwise.
     */
    public void enableUnknownContactView(boolean isEnabled)
    {
        if (isEnabled)
        {
            if (unknownContactPanel == null)
                unknownContactPanel = new UnknownContactPanel(this);

            if (unknownContactPanel.getParent() != centerPanel)
            {
                contactListPanel.setVisible(false);
                unknownContactPanel.setVisible(true);
                centerPanel.remove(contactListPanel);
                centerPanel.add(unknownContactPanel, BorderLayout.CENTER);
            }
        }
        else
        {
            if (contactListPanel.getParent() != centerPanel)
            {
                if (unknownContactPanel != null)
                {
                    unknownContactPanel.setVisible(false);
                    centerPanel.remove(unknownContactPanel);
                }
                contactListPanel.setVisible(true);
                centerPanel.add(contactListPanel, BorderLayout.CENTER);
            }
        }
        centerPanel.revalidate();
        centerPanel.repaint();
    }

    /**
     * Initializes the contact list panel.
     *
     * @param contactList The <tt>MetaContactListService</tt> containing
     * the contact list data.
     */
    public void setContactList(MetaContactListService contactList)
    {
        contactListPanel.initList(contactList);

        searchField.setContactList(GuiActivator.getContactList());
        clKeyDispatcher.setContactList(GuiActivator.getContactList());
    }

    /**
     * Adds all protocol supported operation sets.
     *
     * @param protocolProvider The protocol provider.
     */
    public void addProtocolSupportedOperationSets(
            ProtocolProviderService protocolProvider)
    {
        Map<String, OperationSet> supportedOperationSets
            = protocolProvider.getSupportedOperationSets();

        String ppOpSetClassName = OperationSetPersistentPresence
                                    .class.getName();
        String pOpSetClassName = OperationSetPresence.class.getName();

        // Obtain the presence operation set.
        if (supportedOperationSets.containsKey(ppOpSetClassName)
                || supportedOperationSets.containsKey(pOpSetClassName))
        {
            OperationSetPresence presence = (OperationSetPresence)
                supportedOperationSets.get(ppOpSetClassName);

            if(presence == null) {
                presence = (OperationSetPresence)
                    supportedOperationSets.get(pOpSetClassName);
            }

            uiProviderPresenceListener
                = new GUIProviderPresenceStatusListener();

            presence.addProviderPresenceStatusListener(
                uiProviderPresenceListener);
            presence.addContactPresenceStatusListener(
                GuiActivator.getContactList().getMetaContactListSource());
        }

        // Obtain the basic instant messaging operation set.
        String imOpSetClassName = OperationSetBasicInstantMessaging
                                    .class.getName();

        if (supportedOperationSets.containsKey(imOpSetClassName))
        {
            OperationSetBasicInstantMessaging im
                = (OperationSetBasicInstantMessaging)
                    supportedOperationSets.get(imOpSetClassName);

            //Add to all instant messaging operation sets the Message
            //listener implemented in the ContactListPanel, which handles
            //all received messages.
            im.addMessageListener(getContactListPanel());
        }

        // Obtain the sms messaging operation set.
        String smsOpSetClassName = OperationSetSmsMessaging
                                    .class.getName();

        if (supportedOperationSets.containsKey(smsOpSetClassName))
        {
            OperationSetSmsMessaging sms
                = (OperationSetSmsMessaging)
                    supportedOperationSets.get(smsOpSetClassName);

            sms.addMessageListener(getContactListPanel());
        }

        // Obtain the typing notifications operation set.
        String tnOpSetClassName = OperationSetTypingNotifications
                                    .class.getName();

        if (supportedOperationSets.containsKey(tnOpSetClassName))
        {
            OperationSetTypingNotifications tn
                = (OperationSetTypingNotifications)
                    supportedOperationSets.get(tnOpSetClassName);

            //Add to all typing notification operation sets the Message
            //listener implemented in the ContactListPanel, which handles
            //all received messages.
            tn.addTypingNotificationsListener(this.getContactListPanel());
        }

        // Obtain the basic telephony operation set.
        String telOpSetClassName = OperationSetBasicTelephony.class.getName();

        if (supportedOperationSets.containsKey(telOpSetClassName))
        {
            OperationSetBasicTelephony<?> telephony
                = (OperationSetBasicTelephony<?>)
                    supportedOperationSets.get(telOpSetClassName);

            uiCallListener = new CallManager.GuiCallListener();

            telephony.addCallListener(uiCallListener);
        }

        // Obtain the multi user chat operation set.
        String multiChatClassName = OperationSetMultiUserChat.class.getName();

        if (supportedOperationSets.containsKey(multiChatClassName))
        {
            OperationSetMultiUserChat multiUserChat
                = (OperationSetMultiUserChat)
                    supportedOperationSets.get(multiChatClassName);

            ConferenceChatManager conferenceManager
                = GuiActivator.getUIService().getConferenceChatManager();

            multiUserChat.addInvitationListener(conferenceManager);
            multiUserChat.addInvitationRejectionListener(conferenceManager);
            multiUserChat.addPresenceListener(conferenceManager);
        }

        // Obtain the ad-hoc multi user chat operation set.
        OperationSetAdHocMultiUserChat adHocMultiChatOpSet
            = protocolProvider
                .getOperationSet(OperationSetAdHocMultiUserChat.class);

        if (adHocMultiChatOpSet != null)
        {
            ConferenceChatManager conferenceManager
                = GuiActivator.getUIService().getConferenceChatManager();

            adHocMultiChatOpSet.addInvitationListener(conferenceManager);
            adHocMultiChatOpSet.addInvitationRejectionListener(conferenceManager);
            adHocMultiChatOpSet.addPresenceListener(conferenceManager);
        }

        // Obtain file transfer operation set.
        OperationSetFileTransfer fileTransferOpSet
            = protocolProvider.getOperationSet(OperationSetFileTransfer.class);

        if (fileTransferOpSet != null)
        {
            fileTransferOpSet.addFileTransferListener(getContactListPanel());
        }

        OperationSetMessageWaiting messageWaiting
            = protocolProvider.getOperationSet(OperationSetMessageWaiting.class);

        if (messageWaiting != null)
        {
            messageWaiting.addMessageWaitingNotificationListener(
                MessageType.VOICE,
                TreeContactList.getNotificationContactSource());
        }
    }

    /**
     * Removes all protocol supported operation sets.
     *
     * @param protocolProvider The protocol provider.
     */
    public void removeProtocolSupportedOperationSets(
            ProtocolProviderService protocolProvider)
    {
        Map<String, OperationSet> supportedOperationSets
            = protocolProvider.getSupportedOperationSets();

        String ppOpSetClassName = OperationSetPersistentPresence
                                    .class.getName();
        String pOpSetClassName = OperationSetPresence.class.getName();

        // Obtain the presence operation set.
        if (supportedOperationSets.containsKey(ppOpSetClassName)
                || supportedOperationSets.containsKey(pOpSetClassName))
        {
            OperationSetPresence presence = (OperationSetPresence)
                supportedOperationSets.get(ppOpSetClassName);

            if(presence == null)
            {
                presence = (OperationSetPresence)
                    supportedOperationSets.get(pOpSetClassName);
            }

            if (uiProviderPresenceListener != null)
                presence.removeProviderPresenceStatusListener(
                    uiProviderPresenceListener);
            presence.removeContactPresenceStatusListener(
                GuiActivator.getContactList().getMetaContactListSource());
        }

        // Obtain the basic instant messaging operation set.
        String imOpSetClassName = OperationSetBasicInstantMessaging
                                    .class.getName();

        if (supportedOperationSets.containsKey(imOpSetClassName))
        {
            OperationSetBasicInstantMessaging im
                = (OperationSetBasicInstantMessaging)
                    supportedOperationSets.get(imOpSetClassName);

            im.removeMessageListener(getContactListPanel());
        }

        // Obtain the sms messaging operation set.
        String smsOpSetClassName = OperationSetSmsMessaging.class.getName();

        if (supportedOperationSets.containsKey(smsOpSetClassName))
        {
            OperationSetSmsMessaging sms
                = (OperationSetSmsMessaging)
                    supportedOperationSets.get(smsOpSetClassName);

            sms.removeMessageListener(getContactListPanel());
        }

        // Obtain the typing notifications operation set.
        String tnOpSetClassName = OperationSetTypingNotifications
                                    .class.getName();

        if (supportedOperationSets.containsKey(tnOpSetClassName))
        {
            OperationSetTypingNotifications tn
                = (OperationSetTypingNotifications)
                    supportedOperationSets.get(tnOpSetClassName);

            //Add to all typing notification operation sets the Message
            //listener implemented in the ContactListPanel, which handles
            //all received messages.
            tn.removeTypingNotificationsListener(this.getContactListPanel());
        }

        // Obtain the basic telephony operation set.
        String telOpSetClassName = OperationSetBasicTelephony.class.getName();

        if (supportedOperationSets.containsKey(telOpSetClassName))
        {
            OperationSetBasicTelephony<?> telephony
                = (OperationSetBasicTelephony<?>)
                    supportedOperationSets.get(telOpSetClassName);

            if (uiCallListener != null)
                telephony.removeCallListener(uiCallListener);
        }

        // Obtain the multi user chat operation set.
        String multiChatClassName = OperationSetMultiUserChat.class.getName();

        if (supportedOperationSets.containsKey(multiChatClassName))
        {
            OperationSetMultiUserChat multiUserChat
                = (OperationSetMultiUserChat)
                    supportedOperationSets.get(multiChatClassName);

            ConferenceChatManager conferenceManager
                = GuiActivator.getUIService().getConferenceChatManager();

            multiUserChat.removeInvitationListener(conferenceManager);
            multiUserChat.removeInvitationRejectionListener(conferenceManager);
            multiUserChat.removePresenceListener(conferenceManager);
        }

        // Obtain the ad-hoc multi user chat operation set.
        OperationSetAdHocMultiUserChat adHocMultiChatOpSet
            = protocolProvider
                .getOperationSet(OperationSetAdHocMultiUserChat.class);

        if (adHocMultiChatOpSet != null)
        {
            ConferenceChatManager conferenceManager
                = GuiActivator.getUIService().getConferenceChatManager();

            adHocMultiChatOpSet
                .removeInvitationListener(conferenceManager);
            adHocMultiChatOpSet
                .removeInvitationRejectionListener(conferenceManager);
            adHocMultiChatOpSet
                .removePresenceListener(conferenceManager);
        }

        // Obtain file transfer operation set.
        OperationSetFileTransfer fileTransferOpSet
            = protocolProvider.getOperationSet(OperationSetFileTransfer.class);

        if (fileTransferOpSet != null)
        {
            fileTransferOpSet.removeFileTransferListener(getContactListPanel());
        }

        OperationSetMessageWaiting messageWaiting
            = protocolProvider.getOperationSet(OperationSetMessageWaiting.class);

        if (messageWaiting != null)
        {
            messageWaiting.removeMessageWaitingNotificationListener(
                MessageType.VOICE,
                TreeContactList.getNotificationContactSource());
        }
    }

    /**
     * Returns a set of all protocol providers.
     *
     * @return a set of all protocol providers.
     */
    public Iterator<ProtocolProviderService> getProtocolProviders()
    {
        return new LinkedList<ProtocolProviderService>(
                        protocolProviders.keySet())
                        .iterator();
    }

    /**
     * Returns the protocol provider associated to the account given
     * by the account user identifier.
     *
     * @param accountName The account user identifier.
     * @return The protocol provider associated to the given account.
     */
    public ProtocolProviderService getProtocolProviderForAccount(
            String accountName)
    {
        for (ProtocolProviderService pps : protocolProviders.keySet())
        {
            if (pps.getAccountID().getUserID().equals(accountName))
            {
               return pps;
            }
        }
        return null;
    }

    /**
     * Adds a protocol provider.
     * @param protocolProvider The protocol provider to add.
     */
    public void addProtocolProvider(ProtocolProviderService protocolProvider)
    {
        synchronized(this.protocolProviders)
        {
            if(this.protocolProviders.containsKey(protocolProvider))
                return;

            this.protocolProviders.put(protocolProvider,
                    initiateProviderIndex(protocolProvider));
        }

        if (logger.isTraceEnabled())
            logger.trace("Add the following protocol provider to the gui: "
                + protocolProvider.getAccountID().getAccountAddress());

        this.addProtocolSupportedOperationSets(protocolProvider);

        this.addAccount(protocolProvider);

        ContactEventHandler contactHandler
            = this.getContactHandlerForProvider(protocolProvider);

        if (contactHandler == null)
            contactHandler = new DefaultContactEventHandler(this);

        this.addProviderContactHandler(protocolProvider, contactHandler);
    }

    /**
     * Checks whether we have already loaded the protocol provider.
     * @param protocolProvider the provider to check.
     * @return whether we have already loaded the specified provider.
     */
    public boolean hasProtocolProvider(
        ProtocolProviderService protocolProvider)
    {
        synchronized(this.protocolProviders)
        {
            return this.protocolProviders.containsKey(protocolProvider);
        }
    }

    /**
     * Checks whether we have the operation set in already loaded
     * protocol providers.
     * @param opSet the operation set to check.
     * @return whether we have provider to handle operation set.
     */
    public boolean hasOperationSet(Class<? extends OperationSet> opSet)
    {
        synchronized(this.protocolProviders)
        {
            Iterator<ProtocolProviderService> iter =
                this.protocolProviders.keySet().iterator();
            while(iter.hasNext())
            {
                ProtocolProviderService pp = iter.next();
                if(pp.getOperationSet(opSet) != null)
                {
                    return true;
                }
            }

            return false;
        }
    }

    /**
     * Adds an account to the application.
     *
     * @param protocolProvider The protocol provider of the account.
     */
    public void removeProtocolProvider(ProtocolProviderService protocolProvider)
    {
        if (logger.isTraceEnabled())
            logger.trace("Remove the following protocol provider to the gui: "
                + protocolProvider.getAccountID().getAccountAddress());

        synchronized(this.protocolProviders)
        {
            this.protocolProviders.remove(protocolProvider);
        }

        this.removeProtocolSupportedOperationSets(protocolProvider);

        removeProviderContactHandler(protocolProvider);

        this.updateProvidersIndexes(protocolProvider);

        accountStatusPanel.removeAccount(protocolProvider);
    }

    /**
     * Returns the index of the given protocol provider.
     * @param protocolProvider the protocol provider to search for
     * @return the index of the given protocol provider
     */
    public int getProviderIndex(ProtocolProviderService protocolProvider)
    {
        Integer o = protocolProviders.get(protocolProvider);

        return (o != null) ? o : 0;
    }

    /**
     * Adds an account to the application.
     *
     * @param protocolProvider The protocol provider of the account.
     */
    public void addAccount(ProtocolProviderService protocolProvider)
    {
        if (!accountStatusPanel.containsAccount(protocolProvider))
        {
            if (logger.isTraceEnabled())
                logger.trace("Add the following account to the status bar: "
                + protocolProvider.getAccountID().getAccountAddress());

            accountStatusPanel.addAccount(protocolProvider);

            //request the focus in the contact list panel, which
            //permits to search in the contact list
//            this.contactListPanel.getContactList()
//                    .requestFocus();
        }
    }

    /**
     * Returns the account user id for the given protocol provider.
     * @param protocolProvider the protocol provider corresponding to the
     * account to add
     * @return The account user id for the given protocol provider.
     */
    public String getAccountAddress(ProtocolProviderService protocolProvider)
    {
        return protocolProvider.getAccountID().getAccountAddress();
    }

    /**
     * Returns the account user display name for the given protocol provider.
     * @param protocolProvider the protocol provider corresponding to the
     * account to add
     * @return The account user display name for the given protocol provider.
     */
    public String getAccountDisplayName(ProtocolProviderService protocolProvider)
    {
        final OperationSetServerStoredAccountInfo accountInfoOpSet
            = protocolProvider.getOperationSet(
                    OperationSetServerStoredAccountInfo.class);

        try
        {
            if (accountInfoOpSet != null)
            {
                String displayName
                    = AccountInfoUtils.getDisplayName(accountInfoOpSet);
                if(displayName != null && displayName.length() > 0)
                    return displayName;
            }
        }
        catch(Throwable e)
        {
            logger.error("Cannot obtain display name through OPSet");
        }

        return protocolProvider.getAccountID().getDisplayName();
    }

    /**
     * Returns the Web Contact Info operation set for the given
     * protocol provider.
     *
     * @param protocolProvider The protocol provider for which the TN
     * is searched.
     * @return OperationSetWebContactInfo The Web Contact Info operation
     * set for the given protocol provider.
     */
    public OperationSetWebContactInfo getWebContactInfoOpSet(
            ProtocolProviderService protocolProvider)
    {
        OperationSet opSet
            = protocolProvider.getOperationSet(OperationSetWebContactInfo.class);

        return (opSet instanceof OperationSetWebContactInfo)
            ? (OperationSetWebContactInfo) opSet
            : null;
    }

    /**
     * Returns the telephony operation set for the given protocol provider.
     *
     * @param protocolProvider The protocol provider for which the telephony
     * operation set is about.
     * @return OperationSetBasicTelephony The telephony operation
     * set for the given protocol provider.
     */
    public OperationSetBasicTelephony<?> getTelephonyOpSet(
            ProtocolProviderService protocolProvider)
    {
        OperationSet opSet
            = protocolProvider.getOperationSet(OperationSetBasicTelephony.class);

        return (opSet instanceof OperationSetBasicTelephony<?>)
            ? (OperationSetBasicTelephony<?>) opSet
            : null;
    }

    /**
     * Returns the multi user chat operation set for the given protocol provider.
     *
     * @param protocolProvider The protocol provider for which the multi user
     * chat operation set is about.
     * @return OperationSetAdHocMultiUserChat The telephony operation
     * set for the given protocol provider.
     */
    public OperationSetAdHocMultiUserChat getAdHocMultiUserChatOpSet(
            ProtocolProviderService protocolProvider)
    {
        OperationSet opSet
            = protocolProvider.getOperationSet(
                    OperationSetAdHocMultiUserChat.class);

        return (opSet instanceof OperationSetAdHocMultiUserChat)
            ? (OperationSetAdHocMultiUserChat) opSet
            : null;
    }

    /**
     * Returns <tt>true</tt> if there's any currently selected menu related to
     * this <tt>ContactListContainer</tt>, <tt>false</tt> - otherwise.
     *
     * @return <tt>true</tt> if there's any currently selected menu related to
     * this <tt>ContactListContainer</tt>, <tt>false</tt> - otherwise
     */
    public boolean isMenuSelected()
    {
        return menu.hasSelectedMenus();
    }

    /**
     * Listens for all providerStatusChanged and providerStatusMessageChanged
     * events in order to refresh the account status panel, when a status is
     * changed.
     */
    private class GUIProviderPresenceStatusListener
        implements ProviderPresenceStatusListener
    {
        public void providerStatusChanged(ProviderPresenceStatusChangeEvent evt)
        {
            ProtocolProviderService pps = evt.getProvider();

            accountStatusPanel.updateStatus(pps, evt.getNewStatus());
        }

        public void providerStatusMessageChanged(PropertyChangeEvent evt) {}
    }

    /**
     * Returns the panel containing the ContactList.
     * @return ContactListPanel the panel containing the ContactList
     */
    public ContactListPane getContactListPanel()
    {
        return this.contactListPanel;
    }

    /**
     * Returns the text currently shown in the search field.
     * @return the text currently shown in the search field
     */
    public String getCurrentSearchText()
    {
        return searchField.getText();
    }

    /**
     * Clears the current text in the search field.
     */
    public void clearCurrentSearchText()
    {
        searchField.setText("");
    }

    /**
     * Adds the given <tt>TextFieldChangeListener</tt> to listen for any changes
     * that occur in the search field.
     * @param l the <tt>TextFieldChangeListener</tt> to add
     */
    public void addSearchFieldListener(TextFieldChangeListener l)
    {
        searchField.addTextChangeListener(l);
    }

    /**
     * Removes the given <tt>TextFieldChangeListener</tt> that listens for any
     * changes that occur in the search field.
     * @param l the <tt>TextFieldChangeListener</tt> to remove
     */
    public void removeSearchFieldListener(TextFieldChangeListener l)
    {
        searchField.addTextChangeListener(l);
    }

    /**
     * Checks in the configuration xml if there is already stored index for
     * this provider and if yes, returns it, otherwise creates a new account
     * index and stores it.
     *
     * @param protocolProvider the protocol provider
     * @return the protocol provider index
     */
    private int initiateProviderIndex(
            ProtocolProviderService protocolProvider)
    {
        ConfigurationService configService
            = GuiActivator.getConfigurationService();

        String prefix = "net.java.sip.communicator.impl.gui.accounts";

        List<String> accounts = configService
                .getPropertyNamesByPrefix(prefix, true);

        for (String accountRootPropName : accounts) {
            String accountUID
                = configService.getString(accountRootPropName);

            if(accountUID.equals(protocolProvider
                    .getAccountID().getAccountUniqueID()))
            {
                String  index = configService.getString(
                        accountRootPropName + ".accountIndex");

                if(index != null) {
                    //if we have found the accountIndex for this protocol provider
                    //return this index
                    return Integer.parseInt(index);
                }
                else
                {
                    //if there's no stored accountIndex for this protocol
                    //provider, calculate the index, set it in the configuration
                    //service and return it.

                    return createAccountIndex(protocolProvider,
                            accountRootPropName);
                }
            }
        }

        String accNodeName
            = "acc" + Long.toString(System.currentTimeMillis());

        String accountPackage
            = "net.java.sip.communicator.impl.gui.accounts."
                    + accNodeName;

        configService.setProperty(accountPackage,
                protocolProvider.getAccountID().getAccountUniqueID());

        return createAccountIndex(protocolProvider,
                accountPackage);
    }

    /**
     * Creates and calculates the account index for the given protocol
     * provider.
     * @param protocolProvider the protocol provider
     * @param accountRootPropName the path to where the index should be saved
     * in the configuration xml
     * @return the created index
     */
    private int createAccountIndex(ProtocolProviderService protocolProvider,
            String accountRootPropName)
    {
        ConfigurationService configService
            = GuiActivator.getConfigurationService();
        int accountIndex = -1;

        for (ProtocolProviderService pps : protocolProviders.keySet())
        {
            if (pps.getProtocolDisplayName().equals(
                protocolProvider.getProtocolDisplayName())
                && !pps.equals(protocolProvider))
            {

                int index = protocolProviders.get(pps);

                if (accountIndex < index)
                    accountIndex = index;
            }
        }
        accountIndex++;
        configService.setProperty(
                accountRootPropName + ".accountIndex",
                accountIndex);

        return accountIndex;
    }

    /**
     * Updates the indexes in the configuration xml, when a provider has been
     * removed.
     * @param removedProvider the removed protocol provider
     */
    private void updateProvidersIndexes(ProtocolProviderService removedProvider)
    {
        ConfigurationService configService
            = GuiActivator.getConfigurationService();

        String prefix = "net.java.sip.communicator.impl.gui.accounts";

        ProtocolProviderService currentProvider = null;
        int sameProtocolProvidersCount = 0;

        for (ProtocolProviderService pps : protocolProviders.keySet()) {
            if(pps.getProtocolDisplayName().equals(
                    removedProvider.getProtocolDisplayName())) {

                sameProtocolProvidersCount++;
                if(sameProtocolProvidersCount > 1) {
                    break;
                }
                currentProvider = pps;
            }
        }

        if(sameProtocolProvidersCount < 2 && currentProvider != null) {
            protocolProviders.put(currentProvider, 0);

            List<String> accounts = configService
                .getPropertyNamesByPrefix(prefix, true);

            for (String rootPropName : accounts) {
                String accountUID
                    = configService.getString(rootPropName);

                if(accountUID.equals(currentProvider
                        .getAccountID().getAccountUniqueID())) {

                    configService.setProperty(
                            rootPropName + ".accountIndex",
                            0);
                }
            }
        }
    }

    /**
     * Overwrites the <tt>SIPCommFrame</tt> close method. This method is
     * invoked when user presses the Escape key.
     * @param isEscaped indicates if this window has been closed by pressing
     * the escape key
     */
    @Override
    protected void close(boolean isEscaped)
    {
        TreeContactList contactList = GuiActivator.getContactList();

        Component contactListRightMenu
            = contactList.getRightButtonMenu();

        CommonRightButtonMenu commonPopupMenu
            = getContactListPanel().getCommonRightButtonMenu();

        if(contactListRightMenu != null && contactListRightMenu.isVisible())
        {
            contactListRightMenu.setVisible(false);
        }
        else if(commonPopupMenu != null && commonPopupMenu.isVisible())
        {
            commonPopupMenu.setVisible(false);
        }
        else if(accountStatusPanel.hasSelectedMenus()
                || menu.hasSelectedMenus())
        {
            MenuSelectionManager selectionManager
                = MenuSelectionManager.defaultManager();

            selectionManager.clearSelectedPath();
        }
    }

    /**
     * Returns the main menu in the application window.
     * @return the main menu in the application window
     */
    public MainMenu getMainMenu()
    {
        return menu;
    }

    /**
     * Adds the given <tt>contactHandler</tt> to handle contact events for the
     * given <tt>protocolProvider</tt>.
     *
     * @param protocolProvider the <tt>ProtocolProviderService</tt>, which
     * contacts should be handled by the given <tt>contactHandler</tt>
     * @param contactHandler the <tt>ContactEventHandler</tt> that would handle
     * events coming from the UI for any contacts belonging to the given
     * provider
     */
    public void addProviderContactHandler(
        ProtocolProviderService protocolProvider,
        ContactEventHandler contactHandler)
    {
        providerContactHandlers.put(protocolProvider, contactHandler);
    }

    /**
     * Removes the <tt>ContactEventHandler</tt> corresponding to the given
     * <tt>protocolProvider</tt>.
     *
     * @param protocolProvider the protocol provider, which contact handler
     * we would like to remove
     */
    public void removeProviderContactHandler(
        ProtocolProviderService protocolProvider)
    {
        providerContactHandlers.remove(protocolProvider);
    }

    /**
     * Returns the <tt>ContactEventHandler</tt> registered for this protocol
     * provider.
     *
     * @param protocolProvider the <tt>ProtocolProviderService</tt> for which
     * we are searching a <tt>ContactEventHandler</tt>.
     * @return the <tt>ContactEventHandler</tt> registered for this protocol
     * provider
     */
    public ContactEventHandler getContactHandler(
        ProtocolProviderService protocolProvider)
    {
        return providerContactHandlers.get(protocolProvider);
    }

    /**
     * Returns the <tt>ContactEventHandler</tt> for contacts given by the
     * <tt>protocolProvider</tt>. The <tt>ContactEventHandler</tt> is meant to
     * be used from other bundles in order to change the default behavior of
     * events generated when clicking a contact.
     * @param protocolProvider the protocol provider for which we want to obtain
     * a contact event handler
     * @return the <tt>ContactEventHandler</tt> for contacts given by the
     * <tt>protocolProvider</tt>
     */
    private ContactEventHandler getContactHandlerForProvider(
        ProtocolProviderService protocolProvider)
    {
        Collection<ServiceReference<ContactEventHandler>> serRefs;
        String osgiFilter
            = "(" + ProtocolProviderFactory.PROTOCOL + "="
                + protocolProvider.getProtocolName() + ")";

        try
        {
            serRefs
                = GuiActivator.bundleContext.getServiceReferences(
                        ContactEventHandler.class,
                        osgiFilter);
        }
        catch (InvalidSyntaxException ex)
        {
            serRefs = null;
            logger.error("GuiActivator : " + ex);
        }

        if ((serRefs == null) || serRefs.isEmpty())
            return null;

        return GuiActivator.bundleContext.getService(serRefs.iterator().next());
    }

    /**
     * Initialize plugin components already registered for this container.
     */
    private void initPluginComponents()
    {
        pluginPanelSouth.setLayout(
            new BoxLayout(pluginPanelSouth, BoxLayout.Y_AXIS));
        pluginPanelNorth.setLayout(
            new BoxLayout(pluginPanelNorth, BoxLayout.Y_AXIS));
        pluginPanelEast.setLayout(
            new BoxLayout(pluginPanelEast, BoxLayout.Y_AXIS));
        pluginPanelWest.setLayout(
            new BoxLayout(pluginPanelWest, BoxLayout.Y_AXIS));

        java.awt.Container contentPane = getContentPane();
        contentPane.add(pluginPanelNorth, BorderLayout.NORTH);
        contentPane.add(pluginPanelEast, BorderLayout.EAST);
        contentPane.add(pluginPanelWest, BorderLayout.WEST);
        this.mainPanel.add(pluginPanelSouth, BorderLayout.SOUTH);

        // Search for plugin components registered through the OSGI bundle
        // context.
        Collection<ServiceReference<PluginComponentFactory>> serRefs;

        try
        {
            serRefs
                = GuiActivator.bundleContext.getServiceReferences(
                        PluginComponentFactory.class,
                        "(|(" + Container.CONTAINER_ID + "="
                            + Container.CONTAINER_MAIN_WINDOW.getID() + ")("
                            + Container.CONTAINER_ID + "="
                            + Container.CONTAINER_STATUS_BAR.getID() + "))");
        }
        catch (InvalidSyntaxException exc)
        {
            serRefs = null;
            logger.error("Could not obtain plugin reference.", exc);
        }

        if ((serRefs != null) && !serRefs.isEmpty())
        {
            for (ServiceReference<PluginComponentFactory> serRef : serRefs)
            {
                PluginComponentFactory factory
                    = GuiActivator.bundleContext.getService(serRef);

                if (factory.isNativeComponent())
                    nativePluginsTable.add(factory);
                else
                {
                    String pluginConstraints = factory.getConstraints();
                    Object constraints;

                    if (pluginConstraints != null)
                        constraints
                            = UIServiceImpl
                                .getBorderLayoutConstraintsFromContainer(
                                    pluginConstraints);
                    else
                        constraints = BorderLayout.SOUTH;

                    this.addPluginComponent(
                        (Component)factory.getPluginComponentInstance(this)
                            .getComponent(),
                        factory.getContainer(),
                        constraints);
                }
            }
        }

        GuiActivator.getUIService().addPluginComponentListener(this);
    }

    /**
     * Adds the associated with this <tt>PluginComponentEvent</tt> component to
     * the appropriate container.
     * @param event the <tt>PluginComponentEvent</tt> that has notified us of
     * the add of a plugin component
     */
    public void pluginComponentAdded(PluginComponentEvent event)
    {
        PluginComponentFactory factory = event.getPluginComponentFactory();
        Container pluginContainer = factory.getContainer();

        if (pluginContainer.equals(Container.CONTAINER_MAIN_WINDOW)
            || pluginContainer.equals(Container.CONTAINER_STATUS_BAR))
        {
            String pluginConstraints = factory.getConstraints();
            Object constraints;

            if (pluginConstraints != null)
                constraints =
                    UIServiceImpl
                        .getBorderLayoutConstraintsFromContainer(pluginConstraints);
            else
                constraints = BorderLayout.SOUTH;

            if (factory.isNativeComponent())
            {
                this.nativePluginsTable.add(factory);

                if (isFrameVisible())
                {
                    SwingUtilities.invokeLater(new Runnable()
                    {
                        public void run()
                        {
                            addNativePlugins();
                        }
                    });
                }
            }
            else
            {
                this.addPluginComponent(
                    (Component)factory
                        .getPluginComponentInstance(MainFrame.this)
                            .getComponent(),
                    pluginContainer,
                    constraints);
            }
        }
    }

    /**
     * Removes the associated with this <tt>PluginComponentEvent</tt> component
     * from this container.
     * @param event the <tt>PluginComponentEvent</tt> that notified us of the
     * remove of a plugin component
     */
    public void pluginComponentRemoved(PluginComponentEvent event)
    {
        final PluginComponentFactory factory =
            event.getPluginComponentFactory();
        final Container containerID = factory.getContainer();

        if (containerID.equals(Container.CONTAINER_MAIN_WINDOW))
        {
            Object constraints = UIServiceImpl
                    .getBorderLayoutConstraintsFromContainer(
                        factory.getConstraints());

            if (constraints == null)
                constraints = BorderLayout.SOUTH;

            if (factory.isNativeComponent())
            {
                if (nativePluginsTable.contains(factory))
                {
                    final Object finalConstraints = constraints;

                    SwingUtilities.invokeLater(new Runnable()
                    {
                        public void run()
                        {
                            removePluginComponent(
                                (Component)factory.getPluginComponentInstance(
                                    MainFrame.this).getComponent(),
                                containerID,
                                finalConstraints);

                            getContentPane().repaint();
                        }
                    });
                }
            }
            else
            {
                this.removePluginComponent(
                    (Component) factory
                        .getPluginComponentInstance(MainFrame.this)
                            .getComponent(),
                    containerID,
                    constraints);
            }

            nativePluginsTable.remove(factory);
        }
    }

    /**
     * Removes all native plugins from this container.
     */
    private void removeNativePlugins()
    {
        for (PluginComponentFactory factory: nativePluginsTable)
        {
            Object constraints
                = UIServiceImpl
                    .getBorderLayoutConstraintsFromContainer(factory
                        .getConstraints());

            if (constraints == null)
                constraints = BorderLayout.SOUTH;

            this.removePluginComponent(
                (Component)factory.getPluginComponentInstance(MainFrame.this)
                    .getComponent(),
                factory.getContainer(),
                constraints);

            this.getContentPane().repaint();
        }
    }

    /**
     * Adds all native plugins to this container.
     */
    public void addNativePlugins()
    {
        this.removeNativePlugins();

        for (PluginComponentFactory factory: nativePluginsTable)
        {
            Object constraints
                = UIServiceImpl
                    .getBorderLayoutConstraintsFromContainer(
                        factory.getConstraints());

            Component c = (Component) factory
                .getPluginComponentInstance(MainFrame.this)
                    .getComponent();

            this.addPluginComponent(c, factory.getContainer(), constraints);

            this.nativePluginsTable.add(factory);
        }
    }

    /**
     * Brings this window to front.
     */
    public void bringToFront()
    {
        this.toFront();
    }

    /**
     * Returns the identifier of this window.
     * @return the identifier of this window
     */
    public WindowID getIdentifier()
    {
        return ExportedWindow.MAIN_WINDOW;
    }

    /**
     * Returns this window.
     * @return this window
     */
    public Object getSource()
    {
        return this;
    }

    /**
     * Maximizes this window.
     */
    public void maximize()
    {
        this.setExtendedState(JFrame.MAXIMIZED_BOTH);
    }

    /**
     * Minimizes this window.
     */
    public void minimize()
    {
        this.setExtendedState(JFrame.ICONIFIED);
    }

    /**
     * Implements <code>isVisible</code> in the UIService interface. Checks if
     * the main application window is visible.
     *
     * @return <code>true</code> if main application window is visible,
     *         <code>false</code> otherwise
     * @see UIService#isVisible()
     */
    public boolean isFrameVisible()
    {
        return super.isVisible()
            && (super.getExtendedState() != JFrame.ICONIFIED);
    }

    /**
     * Implements <code>setVisible</code> in the UIService interface. Shows or
     * hides the main application window depending on the parameter
     * <code>visible</code>.
     *
     * @param isVisible true if we are to show the main application frame and
     * false otherwise.
     *
     * @see UIService#setVisible(boolean)
     */
    public void setFrameVisible(final boolean isVisible)
    {
        ConfigurationUtils.setApplicationVisible(isVisible);

        SwingUtilities.invokeLater(new Runnable(){
            public void run()
            {
                if(isVisible)
                {
                    MainFrame.this.addNativePlugins();

                    Window focusedWindow = keyManager.getFocusedWindow();

                    // If there's another currently focused window we prevent
                    // this frame from steeling the focus. This happens for
                    // example in the case of a Master Password window which is
                    // opened before the contact list window.
                    if (focusedWindow != null)
                        setFocusableWindowState(false);

                    MainFrame.super.setVisible(isVisible);

                    if (focusedWindow != null)
                        setFocusableWindowState(true);

                    MainFrame.super.setExtendedState(MainFrame.NORMAL);
                    MainFrame.super.toFront();
                }
                else
                {
                    MainFrame.super.setVisible(isVisible);
                }
            }
        });
    }

    /**
     * Adds the given component with to the container corresponding to the
     * given constraints.
     *
     * @param c the component to add
     * @param container the container to which to add the given component
     * @param constraints the constraints determining the container
     */
    private void addPluginComponent(Component c,
                                    Container container,
                                    Object constraints)
    {
        if (container.equals(Container.CONTAINER_MAIN_WINDOW))
        {
            if (constraints.equals(BorderLayout.NORTH))
            {
                pluginPanelNorth.add(c);
                pluginPanelNorth.repaint();
            }
            else if (constraints.equals(BorderLayout.SOUTH))
            {
                pluginPanelSouth.add(c);
                pluginPanelSouth.repaint();
            }
            else if (constraints.equals(BorderLayout.WEST))
            {
                pluginPanelWest.add(c);
                pluginPanelWest.repaint();
            }
            else if (constraints.equals(BorderLayout.EAST))
            {
                pluginPanelEast.add(c);
                pluginPanelEast.repaint();
            }
        }
        else if (container.equals(Container.CONTAINER_STATUS_BAR))
        {
            statusBarPanel.add(c);
        }

        this.getContentPane().repaint();
        this.getContentPane().validate();
    }

    /**
     * Removes the given component from the container corresponding to the given
     * constraints.
     *
     * @param c the component to remove
     * @param container the container from which to remove the given component
     * @param constraints the constraints determining the container
     */
    private void removePluginComponent( Component c,
                                        Container container,
                                        Object constraints)
    {
        if (container.equals(Container.CONTAINER_MAIN_WINDOW))
        {
            if (constraints.equals(BorderLayout.NORTH))
                pluginPanelNorth.remove(c);
            else if (constraints.equals(BorderLayout.SOUTH))
                pluginPanelSouth.remove(c);
            else if (constraints.equals(BorderLayout.WEST))
                pluginPanelWest.remove(c);
            else if (constraints.equals(BorderLayout.EAST))
                pluginPanelEast.remove(c);
        }
        else if (container.equals(Container.CONTAINER_STATUS_BAR))
        {
            this.statusBarPanel.remove(c);
        }
    }

    /**
     * Returns the account status panel.
     * @return the account status panel.
     */
    public AccountStatusPanel getAccountStatusPanel()
    {
        return accountStatusPanel;
    }

    /**
     * Implementation of {@link ExportedWindow#setParams(Object[])}.
     */
    public void setParams(Object[] windowParams) {}

    /**
     * @param event Currently not used
     */
    protected void windowClosed(WindowEvent event)
    {
        if(GuiActivator.getUIService().getExitOnMainWindowClose())
        {
            try
            {
                GuiActivator.bundleContext.getBundle(0).stop();
            }
            catch (BundleException ex)
            {
                logger.error("Failed to gently shutdown Felix", ex);
                System.exit(0);
            }
            //stopping a bundle doesn't leave the time to the felix thread to
            //properly end all bundles and call their Activator.stop() methods.
            //if this causes problems don't uncomment the following line but
            //try and see why felix isn't exiting (suggesting: is it running
            //in embedded mode?)
            //System.exit(0);
        }
    }

    /**
     * Overrides SIPCommFrame#windowClosing(WindowEvent). Reflects the closed
     * state of this MainFrame in the configuration in order to make it
     * accessible to interested parties, displays the warning that the
     * application will not quit.
     * @param event the <tt>WindowEvent</tt> that notified us
     */
    @Override
    protected void windowClosing(WindowEvent event)
    {
        super.windowClosing(event);

        // On Mac systems the application is not quited on window close, so we
        // don't need to warn the user.
        if (!GuiActivator.getUIService().getExitOnMainWindowClose()
            && !OSUtils.IS_MAC)
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    if (ConfigurationUtils.isQuitWarningShown())
                    {
                        MessageDialog dialog =
                            new MessageDialog(null,
                                GuiActivator.getResources().getI18NString(
                                "service.gui.CLOSE"),
                                GuiActivator.getResources().getI18NString(
                                "service.gui.HIDE_MAIN_WINDOW"), false);

                        if (dialog.showDialog() == MessageDialog.OK_DONT_ASK_CODE)
                            ConfigurationUtils.setQuitWarningShown(false);
                    }
                }
            });

            ConfigurationUtils.setApplicationVisible(false);
        }
    }

    /**
     * Called when the ENTER key was typed when this window was the focused
     * window. Performs the appropriate actions depending on the current state
     * of the contact list.
     */
    public void enterKeyTyped()
    {
        if (unknownContactPanel != null && unknownContactPanel.isVisible())
        {
            unknownContactPanel.addUnknownContact();
        }
        else if (contactListPanel.isVisible())
        {
            // Starts a chat with the currently selected contact.
            GuiActivator.getContactList().startSelectedContactChat();
        }
    }

    /**
     * Called when the CTRL-ENTER or CMD-ENTER keys were typed when this window
     * was the focused window. Performs the appropriate actions depending on the
     * current state of the contact list.
     */
    public void ctrlEnterKeyTyped()
    {
        if (unknownContactPanel != null && unknownContactPanel.isVisible())
        {
            unknownContactPanel.startCall();
        }
        else if (contactListPanel.isVisible())
        {
            // Starts a chat with the currently selected contact.
            GuiActivator.getContactList().startSelectedContactCall();
        }
    }

    /**
     * Registers key actions for this window.
     */
    private void registerKeyActions()
    {
        InputMap inputMap = getRootPane()
            .getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        // Remove the default escape key mapping as its a special
        // one for the main frame and the contactlist
        inputMap.remove(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0));
    }

    /**
     * Reloads skin information
     */
    public void loadSkin()
    {
        this.mainPanel.setBackground(new Color(
                GuiActivator.getResources()
                    .getColor("service.gui.MAIN_WINDOW_BACKGROUND")));
    }
}
