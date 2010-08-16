/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main;


import net.java.sip.communicator.impl.gui.event.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.OperationSet;
import net.java.sip.communicator.service.protocol.OperationSetPresence;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;
import net.java.sip.communicator.util.swing.*;

/**
 * The main application window. This class is the core of this UI
 * implementation. It stores all available protocol providers and their
 * operation sets, as well as all registered accounts, the
 * <tt>MetaContactListService</tt> and all sent messages that aren't
 * delivered yet.
 *
 * @author Yana Stamcheva
 * @author Lubomir Marinov
 */
public abstract class MainFrame
    extends SIPCommFrame
    implements  ExportedWindow,
<<<<<<< HEAD
                PluginComponentListener
{
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
    private final SearchField searchField;

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
    private final Map<PluginComponent, Component> nativePluginsTable =
        new Hashtable<PluginComponent, Component>();

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
     * Creates an instance of <tt>MainFrame</tt>.
     */
    public MainFrame()
    {
        if (!ConfigurationManager.isWindowDecorated())
        {
            this.setUndecorated(true);
        }

        this.searchField = new SearchField(this);

        this.contactListPanel = new ContactListPane(this);

        this.accountStatusPanel = new AccountStatusPanel(this);

        menu = new MainMenu(this);

        /*
         * If the application is configured to quit when this frame is closed,
         * do so.
         */
        this.addWindowListener(new WindowAdapter()
        {
            /**
             * Invoked when a window has been closed.
             */
            public void windowClosed(WindowEvent event)
            {
                MainFrame.this.windowClosed(event);
            }
            /**
             * Invoked when a window has been opened.
             */
            public void windowOpened(WindowEvent e)
            {
                GuiActivator.getContactList().requestFocusInWindow();
            }
        });

        this.initTitleFont();

        ResourceManagementService resources = GuiActivator.getResources();
        String applicationName
            = resources.getSettingsString("service.gui.APPLICATION_NAME");

        this.setTitle(applicationName);

        this.mainPanel.setBackground(new Color(
                GuiActivator.getResources()
                    .getColor("service.gui.MAIN_WINDOW_BACKGROUND")));

        KeyboardFocusManager keyManager
            = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        keyManager.addKeyEventDispatcher(new MainKeyDispatcher(keyManager));

        this.init();

        this.initPluginComponents();
    }

    /**
     * Requests the focus in the center panel, which contains either the
     * contact list or the unknown contact panel.
     */
    public void requestFocusInCenterPanel()
    {
        centerPanel.requestFocusInWindow();
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

        this.setKeybindingInput(KeybindingSet.Category.MAIN);
        this.addKeybindingAction("main-rename",
                                new RenameAction());

        // Remove the default escape key mapping as its a special
        // one for the main frame and the contactlist
        getRootPane().getInputMap(
            JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .remove(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0));

        TransparentPanel northPanel
            = new TransparentPanel(new BorderLayout(0, 0));

        this.setJMenuBar(menu);

        northPanel.add(accountStatusPanel, BorderLayout.CENTER);

        TransparentPanel searchPanel
            = new TransparentPanel(new BorderLayout(2, 0));
        searchPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 5));
        searchPanel.add(searchField);
        searchPanel.add(new CallHistoryButton(), BorderLayout.EAST);

        centerPanel.add(searchPanel, BorderLayout.NORTH);
        centerPanel.add(contactListPanel, BorderLayout.CENTER);

        this.mainPanel.add(northPanel, BorderLayout.NORTH);
        this.mainPanel.add(centerPanel, BorderLayout.CENTER);

        java.awt.Container contentPane = getContentPane();
        contentPane.add(mainPanel, BorderLayout.CENTER);
        contentPane.add(statusBarPanel, BorderLayout.SOUTH);
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
        String fontName
            = resources.getSettingsString("service.gui.FONT_NAME");

        String titleFontSize
            = resources.getSettingsString("service.gui.FONT_SIZE");

        Font font = new Font(   fontName,
                                Font.BOLD,
                                Integer.parseInt(titleFontSize));

        final int componentCount = layeredPane.getComponentCount();
        for (int i = 0; i < componentCount; i++)
        {
            layeredPane.getComponent(i).setFont(font);
        }
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

            presence.addProviderPresenceStatusListener(
                        new GUIProviderPresenceStatusListener());
            presence.addContactPresenceStatusListener(
                        GuiActivator.getContactList());
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
            OperationSetBasicTelephony telephony
                = (OperationSetBasicTelephony)
                    supportedOperationSets.get(telOpSetClassName);

            telephony.addCallListener(new CallManager.GuiCallListener());
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
        if (logger.isTraceEnabled())
            logger.trace("Add the following protocol provider to the gui: "
            + protocolProvider.getAccountID().getAccountAddress());

        this.protocolProviders.put(protocolProvider,
                initiateProviderIndex(protocolProvider));

        this.addProtocolSupportedOperationSets(protocolProvider);

        this.addAccount(protocolProvider);

        ContactEventHandler contactHandler
            = this.getContactHandlerForProvider(protocolProvider);

        if (contactHandler == null)
            contactHandler = new DefaultContactEventHandler(this);

        this.addProviderContactHandler(protocolProvider, contactHandler);
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
            this.contactListPanel.getContactList()
                    .requestFocus();
        }
    }

    /**
     * Adds an account to the application.
     *
     * @param protocolProvider The protocol provider of the account.
     */
    public void removeProtocolProvider(ProtocolProviderService protocolProvider)
    {
        this.protocolProviders.remove(protocolProvider);
        this.updateProvidersIndexes(protocolProvider);

        if (accountStatusPanel.containsAccount(protocolProvider))
        {
            accountStatusPanel.removeAccount(protocolProvider);
        }
    }

    /**
     * Returns the account user id for the given protocol provider.
     * @param protocolProvider the protocol provider corresponding to the
     * account to add
     * @return The account user id for the given protocol provider.
     */
    public String getAccount(ProtocolProviderService protocolProvider)
    {
        return protocolProvider.getAccountID().getUserID();
    }

=======
                PluginComponentListener, MainFrameInterface
{  
>>>>>>> First checkin of new GUI classes to support touch screen devices.
    /**
     * Returns the presence operation set for the given protocol provider.
     *
     * @param protocolProvider The protocol provider for which the
     * presence operation set is searched.
     * @return the presence operation set for the given protocol provider.
     */
    public static OperationSetPresence getProtocolPresenceOpSet(
            ProtocolProviderService protocolProvider)
    {
        OperationSet opSet
            = protocolProvider.getOperationSet(OperationSetPresence.class);

        return
            (opSet instanceof OperationSetPresence)
                ? (OperationSetPresence) opSet
                : null;
    }
}