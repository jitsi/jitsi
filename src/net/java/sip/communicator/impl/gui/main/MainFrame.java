/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main;

import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.*;
import java.util.List;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.event.*;
import net.java.sip.communicator.impl.gui.lookandfeel.*;
import net.java.sip.communicator.impl.gui.main.call.*;
import net.java.sip.communicator.impl.gui.main.chat.conference.*;
import net.java.sip.communicator.impl.gui.main.contactlist.*;
import net.java.sip.communicator.impl.gui.main.menus.*;
import net.java.sip.communicator.impl.gui.main.presence.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.contacteventhandler.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.Container;
import net.java.sip.communicator.service.keybindings.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.swing.*;

import org.osgi.framework.*;

/**
 * The main application window. This class is the core of this ui
 * implementation. It stores all available protocol providers and their
 * operation sets, as well as all registered accounts, the
 * <tt>MetaContactListService</tt> and all sent messages that aren't
 * delivered yet.
 *
 * @author Yana Stamcheva
 * @author Lubomir Marinov
 */
public class MainFrame
    extends SIPCommFrame
    implements  ExportedWindow,
                PluginComponentListener
{
    private final Logger logger = Logger.getLogger(MainFrame.class);

    private final TransparentPanel mainPanel
        = new TransparentPanel(new BorderLayout(0, 0));

    private final TransparentPanel statusBarPanel
        = new TransparentPanel(new BorderLayout());

    private final ImageIcon moreActionsIcon = new ImageIcon(ImageLoader
        .getImage(ImageLoader.MORE_ACTIONS_BUTTON));

    private final ImageIcon moreActionsRolloverIcon = new ImageIcon(ImageLoader
        .getImage(ImageLoader.MORE_ACTIONS_ROLLOVER_BUTTON));

    private MainMenu menu;

    private MainCallPanel mainCallPanel;

    private final HashMap<ProtocolProviderService, Integer> protocolProviders =
        new LinkedHashMap<ProtocolProviderService, Integer>();

    private AccountStatusPanel accountStatusPanel;

    private MetaContactListService contactList;

    private final Map<ProtocolProviderService, ContactEventHandler>
        providerContactHandlers =
            new Hashtable<ProtocolProviderService, ContactEventHandler>();

    private final Map<PluginComponent, Component> nativePluginsTable =
        new Hashtable<PluginComponent, Component>();

    private final JPanel pluginPanelNorth = new TransparentPanel();
    private final JPanel pluginPanelSouth = new TransparentPanel();
    private final JPanel pluginPanelWest = new TransparentPanel();
    private final JPanel pluginPanelEast = new TransparentPanel();

    private ContactListPane contactListPanel;

    private ActionMenuGlassPane glassPane = null;

    /**
     * Creates an instance of <tt>MainFrame</tt>.
     */
    public MainFrame()
    {
        if (!ConfigurationManager.isWindowDecorated())
        {
            this.setUndecorated(true);
        }

        this.mainCallPanel = new MainCallPanel(this);

        this.contactListPanel = new ContactListPane(this);

        this.accountStatusPanel = new AccountStatusPanel(this);

        menu = new MainMenu(this);

        /*
         * If the application is configured to quit when this frame is closed,
         * do so.
         */
        this.addWindowListener(new WindowAdapter()
        {
            public void windowClosed(WindowEvent event)
            {
                MainFrame.this.windowClosed(event);
            }
        });

        addComponentListener(new ComponentAdapter()
        {
            @Override
            public void componentResized(ComponentEvent e)
            {
                if ((glassPane != null) && glassPane.isVisible())
                    glassPane.revalidate();
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

        this.init();

        this.initPluginComponents();
    }

    /**
     * Initiates the content of this frame.
     */
    private void init()
    {
        if (GuiActivator.getUIService().getExitOnMainWindowClose())
            this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        else
            this.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);

        this.setKeybindingInput(KeybindingSet.Category.MAIN);
        this.addKeybindingAction("main-rename",
                                new RenameAction());

        TransparentPanel northPanel = new TransparentPanel(new BorderLayout());

        TransparentPanel centerPanel
            = new TransparentPanel(new BorderLayout(0, 0));

        String isToolbarExtendedString
            = GuiActivator.getResources().
                getSettingsString("impl.gui.IS_TOOLBAR_EXTENDED");

        boolean isToolBarExtended
            = Boolean.parseBoolean(isToolbarExtendedString);

        JPanel menusPanel = new JPanel(new BorderLayout());

        if (isToolBarExtended)
        {
            menusPanel.add(new ExtendedQuickMenu(this), BorderLayout.SOUTH);
        }
        else
        {
            JLabel moreActionsLabel = new JLabel(moreActionsIcon);
            moreActionsLabel.setToolTipText(GuiActivator.getResources()
                .getI18NString("service.gui.OPEN_TOOLS"));
            moreActionsLabel.addMouseListener(new ActionMenuMouseListener());

            TransparentPanel moreActionsPanel
                = new TransparentPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
            moreActionsPanel.add(moreActionsLabel);

            centerPanel.add(moreActionsPanel, BorderLayout.NORTH);
        }

        this.setJMenuBar(menu);

        menusPanel.setUI(new SIPCommOpaquePanelUI());

        northPanel.add(menusPanel, BorderLayout.CENTER);
        northPanel.add(accountStatusPanel, BorderLayout.SOUTH);

        centerPanel.add(contactListPanel, BorderLayout.CENTER);
        centerPanel.add(mainCallPanel, BorderLayout.SOUTH);

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
     * Returns the <tt>MetaContactListService</tt>.
     *
     * @return <tt>MetaContactListService</tt> The current meta contact list.
     */
    public MetaContactListService getContactList()
    {
        return this.contactList;
    }

    /**
     * Initializes the contact list panel.
     *
     * @param contactList The <tt>MetaContactListService</tt> containing
     * the contact list data.
     */
    public void setContactList(MetaContactListService contactList)
    {
        this.contactList = contactList;

        contactListPanel.initList(contactList);

        contactListPanel.getContactList()
            .addListSelectionListener(mainCallPanel);
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
                        new GUIContactPresenceStatusListener());
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

        // Obtain file transfer operation set.
        OperationSetFileTransfer fileTransferOpSet
            = (OperationSetFileTransfer) protocolProvider
                .getOperationSet(OperationSetFileTransfer.class);

        if (fileTransferOpSet != null)
        {
            fileTransferOpSet.addFileTransferListener(
                getContactListPanel());
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
            logger.trace("Add the following account to the status bar: "
                + protocolProvider.getAccountID().getAccountAddress());

            accountStatusPanel.addAccount(protocolProvider);

            //request the focus in the contact list panel, which
            //permits to search in the contact list
            this.contactListPanel.getContactList()
                    .requestFocus();
        }

        if(!mainCallPanel.containsCallAccount(protocolProvider)
            && getTelephonyOpSet(protocolProvider) != null)
        {
            mainCallPanel.addCallAccount(protocolProvider);
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

        if(mainCallPanel.containsCallAccount(protocolProvider))
        {
            mainCallPanel.removeCallAccount(protocolProvider);
        }
    }

    /**
     * Returns the account user id for the given protocol provider.
     * @return The account user id for the given protocol provider.
     */
    public String getAccount(ProtocolProviderService protocolProvider)
    {
        return protocolProvider.getAccountID().getUserID();
    }

    /**
     * Returns the presence operation set for the given protocol provider.
     *
     * @param protocolProvider The protocol provider for which the
     * presence operation set is searched.
     * @return the presence operation set for the given protocol provider.
     */
    public OperationSetPresence getProtocolPresenceOpSet(
            ProtocolProviderService protocolProvider)
    {
        OperationSet opSet
            = protocolProvider.getOperationSet(OperationSetPresence.class);

        return (opSet instanceof OperationSetPresence)
            ? (OperationSetPresence) opSet
            : null;
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
    public OperationSetBasicTelephony getTelephonyOpSet(
            ProtocolProviderService protocolProvider)
    {
        OperationSet opSet
            = protocolProvider.getOperationSet(OperationSetBasicTelephony.class);

        return (opSet instanceof OperationSetBasicTelephony)
            ? (OperationSetBasicTelephony) opSet
            : null;
    }

    /**
     * Returns the multi user chat operation set for the given protocol provider.
     *
     * @param protocolProvider The protocol provider for which the multi user
     * chat operation set is about.
     * @return OperationSetMultiUserChat The telephony operation
     * set for the given protocol provider.
     */
    public OperationSetMultiUserChat getMultiUserChatOpSet(
            ProtocolProviderService protocolProvider)
    {
        OperationSet opSet
            = protocolProvider.getOperationSet(OperationSetMultiUserChat.class);

        return (opSet instanceof OperationSetMultiUserChat)
            ? (OperationSetMultiUserChat) opSet
            : null;
    }

    /**
     * Listens for all contactPresenceStatusChanged events in order
     * to refresh the contact list, when a status is changed.
     */
    private class GUIContactPresenceStatusListener implements
            ContactPresenceStatusListener
    {
        /**
         * Indicates that a contact has changed its status.
         *
         * @param evt the presence event containing information about the
         * contact status change
         */
        public void contactPresenceStatusChanged(
                ContactPresenceStatusChangeEvent evt)
        {
            Contact sourceContact = evt.getSourceContact();

            MetaContact metaContact = contactList
                    .findMetaContactByContact(sourceContact);

            if (metaContact != null
                && (evt.getOldStatus() != evt.getNewStatus()))
            {
                // Update the status in the contact list.
                contactListPanel.getContactList().refreshContact(metaContact);
            }
        }
    }

    /**
     * Listens for all providerStatusChanged and providerStatusMessageChanged
     * events in order to refresh the account status panel, when a status is
     * changed.
     */
    private class GUIProviderPresenceStatusListener implements
            ProviderPresenceStatusListener
    {
        public void providerStatusChanged(ProviderPresenceStatusChangeEvent evt)
        {
            ProtocolProviderService pps = evt.getProvider();

            accountStatusPanel.updateStatus(pps, evt.getNewStatus());

            if(mainCallPanel.containsCallAccount(pps))
            {
                mainCallPanel.updateCallAccountStatus(pps);
            }
        }

        public void providerStatusMessageChanged(PropertyChangeEvent evt) {
        }
    }

    /**
     * Returns the list of all groups.
     * @return The list of all groups.
     */
    public Iterator<MetaContactGroup> getAllGroups()
    {
        return getContactListPanel().getContactList().getAllGroups();
    }

    /**
     * Returns the Meta Contact Group corresponding to the given MetaUID.
     *
     * @param metaUID An identifier of a group.
     * @return The Meta Contact Group corresponding to the given MetaUID.
     */
    public MetaContactGroup getGroupByID(String metaUID)
    {
        return getContactListPanel()
            .getContactList().getGroupByID(metaUID);
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
     * If the protocol provider supports presence operation set searches the
     * last status which was selected, otherwise returns null.
     *
     * @param protocolProvider the protocol provider we're interested in.
     * @return the last protocol provider presence status, or null if this
     * provider doesn't support presence operation set
     */
    public Object getProtocolProviderLastStatus(
            ProtocolProviderService protocolProvider)
    {
        if(getProtocolPresenceOpSet(protocolProvider) != null)
            return accountStatusPanel
                .getLastPresenceStatus(protocolProvider);
        else
            return accountStatusPanel.getLastStatusString(protocolProvider);
    }

    /**
     * <tt>RenameAction</tt> is invoked when user presses the F2 key. Depending
     * on the selection opens the appropriate form for renaming.
     */
    private class RenameAction extends AbstractAction
    {
        private static final long serialVersionUID = 0L;

        public void actionPerformed(ActionEvent e)
        {
            Object selectedObject
                = getContactListPanel().getContactList().getSelectedValue();

            if(selectedObject instanceof MetaContact) {
                RenameContactDialog dialog = new RenameContactDialog(
                        MainFrame.this, (MetaContact)selectedObject);

                dialog.setLocation(
                        Toolkit.getDefaultToolkit().getScreenSize().width/2
                            - 200,
                        Toolkit.getDefaultToolkit().getScreenSize().height/2
                            - 50
                        );

                dialog.setVisible(true);

                dialog.requestFocusInFiled();
            }
            else if(selectedObject instanceof MetaContactGroup) {

                RenameGroupDialog dialog = new RenameGroupDialog(
                        MainFrame.this, (MetaContactGroup)selectedObject);

                Dimension screenSize =
                    Toolkit.getDefaultToolkit().getScreenSize();
                dialog.setLocation(screenSize.width / 2 - 200,
                    screenSize.height / 2 - 50);

                dialog.setVisible(true);

                dialog.requestFocusInFiled();
            }
        }
    }

    /**
     * Overwrites the <tt>SIPCommFrame</tt> close method. This method is
     * invoked when user presses the Escape key.
     */
    protected void close(boolean isEscaped)
    {
        ContactList contactList = getContactListPanel().getContactList();

        ContactRightButtonMenu contactPopupMenu
            = contactList.getContactRightButtonMenu();

        GroupRightButtonMenu groupPopupMenu
            = contactList.getGroupRightButtonMenu();

        CommonRightButtonMenu commonPopupMenu
            = getContactListPanel().getCommonRightButtonMenu();

        if(contactPopupMenu != null && contactPopupMenu.isVisible())
        {
            contactPopupMenu.setVisible(false);
        }
        else if(groupPopupMenu != null && groupPopupMenu.isVisible())
        {
            groupPopupMenu.setVisible(false);
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
     *
     * @param protocolProvider
     * @param contactHandler
     */
    public void addProviderContactHandler(
        ProtocolProviderService protocolProvider,
        ContactEventHandler contactHandler)
    {
        providerContactHandlers.put(protocolProvider, contactHandler);
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
     *
     * @param protocolProvider
     * @return
     */
    private ContactEventHandler getContactHandlerForProvider(
        ProtocolProviderService protocolProvider)
    {
        ServiceReference[] serRefs = null;

        String osgiFilter = "("
            + ProtocolProviderFactory.PROTOCOL
            + "=" + protocolProvider.getProtocolName()+")";

        try
        {
            serRefs = GuiActivator.bundleContext.getServiceReferences(
                ContactEventHandler.class.getName(), osgiFilter);
        }
        catch (InvalidSyntaxException ex){
            logger.error("GuiActivator : " + ex);
        }

        if(serRefs == null)
            return null;

        return (ContactEventHandler) GuiActivator.bundleContext
            .getService(serRefs[0]);
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
        ServiceReference[] serRefs = null;

        String osgiFilter = "(|("
            + Container.CONTAINER_ID
            + "="+Container.CONTAINER_MAIN_WINDOW.getID()+")"
            + "(" + Container.CONTAINER_ID
            + "="+Container.CONTAINER_STATUS_BAR.getID()+"))";

        try
        {
            serRefs = GuiActivator.bundleContext.getServiceReferences(
                PluginComponent.class.getName(),
                osgiFilter);
        }
        catch (InvalidSyntaxException exc)
        {
            logger.error("Could not obtain plugin reference.", exc);
        }

        if (serRefs != null)
        {
            for (int i = 0; i < serRefs.length; i++)
            {
                PluginComponent c =
                    (PluginComponent) GuiActivator.bundleContext
                        .getService(serRefs[i]);

                if (c.isNativeComponent())
                    nativePluginsTable.put(c, new JPanel());
                else
                {
                    String pluginConstraints = c.getConstraints();
                    Object constraints;

                    if (pluginConstraints != null)
                        constraints =
                            UIServiceImpl
                                .getBorderLayoutConstraintsFromContainer(pluginConstraints);
                    else
                        constraints = BorderLayout.SOUTH;

                    this.addPluginComponent((Component) c.getComponent(), c
                        .getContainer(), constraints);
                }
            }
        }

        GuiActivator.getUIService().addPluginComponentListener(this);
    }

    /**
     * Adds the associated with this <tt>PluginComponentEvent</tt> component to
     * the appropriate container.
     */
    public void pluginComponentAdded(PluginComponentEvent event)
    {
        PluginComponent pluginComponent = event.getPluginComponent();
        Container pluginContainer = pluginComponent.getContainer();

        if (pluginContainer.equals(Container.CONTAINER_MAIN_WINDOW)
            || pluginContainer.equals(Container.CONTAINER_STATUS_BAR))
        {
            String pluginConstraints = pluginComponent.getConstraints();
            Object constraints;

            if (pluginConstraints != null)
                constraints =
                    UIServiceImpl
                        .getBorderLayoutConstraintsFromContainer(pluginConstraints);
            else
                constraints = BorderLayout.SOUTH;

            if (pluginComponent.isNativeComponent())
            {
                this.nativePluginsTable.put(pluginComponent, new JPanel());

                if (isVisible())
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
                this.addPluginComponent((Component) pluginComponent
                    .getComponent(), pluginContainer, constraints);
            }
        }
    }

    /**
     * Removes the associated with this <tt>PluginComponentEvent</tt> component
     * from this container.
     */
    public void pluginComponentRemoved(PluginComponentEvent event)
    {
        final PluginComponent pluginComponent = event.getPluginComponent();

        final Container containerID = pluginComponent.getContainer();

        if (containerID.equals(Container.CONTAINER_MAIN_WINDOW))
        {
            Object constraints = UIServiceImpl
                    .getBorderLayoutConstraintsFromContainer(
                        pluginComponent.getConstraints());

            if (constraints == null)
                constraints = BorderLayout.SOUTH;

            if (pluginComponent.isNativeComponent())
            {
                if (nativePluginsTable.containsKey(pluginComponent))
                {
                    final Component c = nativePluginsTable.get(pluginComponent);

                    final Object finalConstraints = constraints;

                    SwingUtilities.invokeLater(new Runnable()
                    {
                        public void run()
                        {
                            removePluginComponent(c, containerID,
                                finalConstraints);

                            getContentPane().repaint();
                        }
                    });
                }
            }
            else
            {
                this.removePluginComponent((Component) pluginComponent
                    .getComponent(), containerID, constraints);
            }

            nativePluginsTable.remove(pluginComponent);
        }
    }

    /**
     * Removes all native plugins from this container.
     */
    private void removeNativePlugins()
    {
        Object constraints;
        for (Map.Entry<PluginComponent, Component> entry : nativePluginsTable
            .entrySet())
        {
            PluginComponent pluginComponent = entry.getKey();
            Component c = entry.getValue();

            constraints =
                UIServiceImpl
                    .getBorderLayoutConstraintsFromContainer(pluginComponent
                        .getConstraints());

            if (constraints == null)
                constraints = BorderLayout.SOUTH;

            this.removePluginComponent(c, pluginComponent.getContainer(),
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

        for (Map.Entry<PluginComponent, Component> pluginEntry : nativePluginsTable
            .entrySet())
        {
            PluginComponent plugin = pluginEntry.getKey();

            Object constraints =
                UIServiceImpl.getBorderLayoutConstraintsFromContainer(plugin
                    .getConstraints());

            Component c = (Component) plugin.getComponent();

            this.addPluginComponent(c, plugin.getContainer(), constraints);

            this.nativePluginsTable.put(plugin, c);
        }
    }

    public void bringToFront()
    {
        this.toFront();
    }

    public WindowID getIdentifier()
    {
        return ExportedWindow.MAIN_WINDOW;
    }

    public Object getSource()
    {
        return this;
    }

    public void maximize()
    {
        this.setExtendedState(JFrame.MAXIMIZED_BOTH);
    }

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
    public boolean isVisible()
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
    public void setVisible(final boolean isVisible)
    {
        SwingUtilities.invokeLater(new Runnable(){
            public void run()
            {
                if(isVisible)
                {
                    MainFrame.this.addNativePlugins();
                    MainFrame.super.setVisible(isVisible);
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
     * Returns the phone number currently entered in the phone number field.
     *
     * @return the phone number currently entered in the phone number field.
     */
    public String getCurrentPhoneNumber()
    {
        return mainCallPanel.getPhoneNumberComboText();
    }

    /**
     * Changes the phone number currently entered in the phone number field.
     *
     * @param number the phone number to enter in the phone number field.
     */
    public void setCurrentPhoneNumber(String number)
    {
        mainCallPanel.setPhoneNumberComboText(number);
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

    /*
     * Overrides SIPCommFrame#windowClosing(WindowEvent). Reflects the closed
     * state of this MainFrame in the configuration in order to make it
     * accessible to interested parties, displays the warning that the
     * application will not quit.
     */
    protected void windowClosing(WindowEvent event)
    {
        super.windowClosing(event);

        if (!GuiActivator.getUIService().getExitOnMainWindowClose())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    if (ConfigurationManager.isQuitWarningShown())
                    {
                        MessageDialog dialog =
                            new MessageDialog(null,
                                GuiActivator.getResources().getI18NString(
                                "service.gui.CLOSE"),
                                GuiActivator.getResources().getI18NString(
                                "service.gui.HIDE_MAIN_WINDOW"), false);

                        if (dialog.showDialog() == MessageDialog.OK_DONT_ASK_CODE)
                            ConfigurationManager.setQuitWarningShown(false);
                    }
                }
            });

            ConfigurationManager.setApplicationVisible(false);
        }
    }

    /**
     * Initializes the more actions panel.
     */
    private class ActionMenuMouseListener extends MouseAdapter
    {
        public void mouseEntered(MouseEvent e)
        {
            JLabel moreActionsLabel = (JLabel) e.getComponent();

            moreActionsLabel.setIcon(moreActionsRolloverIcon);
            moreActionsLabel.revalidate();
            moreActionsLabel.repaint();
        }

        public void mouseExited(MouseEvent e)
        {
            JLabel moreActionsLabel = (JLabel) e.getComponent();

            moreActionsLabel.setIcon(moreActionsIcon);
            moreActionsLabel.revalidate();
            moreActionsLabel.repaint();
        }

        public void mousePressed(MouseEvent e)
        {
            if ((glassPane == null) && (rootPane != null))
            {
                glassPane = new ActionMenuGlassPane();
                glassPane.add(new ActionMenuPanel());
                KeyboardFocusManager.getCurrentKeyboardFocusManager()
                    .addPropertyChangeListener(new PropertyChangeListener()
                    {
                        public void propertyChange(PropertyChangeEvent e)
                        {
                            if ("focusOwner".equals(e.getPropertyName())
                                && glassPane.isVisible())
                            {
                                // we don't want the glasspane staying on top
                                // of certain components
                                if (e.getNewValue() == rootPane)
                                {
                                    glassPane.setVisible(false);
                                }
                            }
                        }
                    });

                rootPane.setGlassPane(glassPane);
                glassPane.setVisible(true);
            }
            else if (!glassPane.isVisible())
            {
                // we re-set the same glasspane in the root pane otherwise
                // we are not guaranteed it will be painted in some case
                rootPane.setGlassPane(glassPane);
                glassPane.setVisible(true);
            }
            else
                glassPane.setVisible(false);
        }
    }
}
