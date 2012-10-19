/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.menus;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.event.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.main.call.*;
import net.java.sip.communicator.impl.gui.main.call.conference.*;
import net.java.sip.communicator.impl.gui.main.configforms.*;
import net.java.sip.communicator.impl.gui.main.contactlist.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.Container;
import net.java.sip.communicator.service.notification.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.skin.*;
import net.java.sip.communicator.util.swing.*;

import org.osgi.framework.*;

/**
 * The <tt>FileMenu</tt> is a menu in the main application menu bar that
 * contains "New account".
 *
 * @author Yana Stamcheva
 * @author Lubomir Marinov
 * @author Adam Netocny
 */
public class ToolsMenu
    extends SIPCommMenu
    implements  ActionListener,
                PluginComponentListener,
                ServiceListener,
                Skinnable
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * Local logger.
     */
    private final Logger logger = Logger.getLogger(ToolsMenu.class);

    /**
     * Property to disable auto answer menu.
     */
    private static final String AUTO_ANSWER_MENU_DISABLED_PROP =
        "net.java.sip.communicator.impl.gui.main.menus.AUTO_ANSWER_MENU_DISABLED";

   /**
    * Conference call menu item.
    */
    private JMenuItem conferenceMenuItem;

    /**
     * Video bridge conference call menu. In the case of more than one account.
     */
    private JMenuItem videoBridgeMenuItem;

   /**
    * Show/Hide offline contacts menu item.
    */
    private JMenuItem hideOfflineMenuItem;

   /**
    * Sound menu item.
    */
    private JMenuItem soundMenuItem;

   /**
    * Preferences menu item.
    */
    JMenuItem configMenuItem;

    /**
     * Creates an instance of <tt>FileMenu</tt>.
     * @param parentWindow The parent <tt>ChatWindow</tt>.
     */
    public ToolsMenu(MainFrame parentWindow) {

        super(GuiActivator.getResources().getI18NString("service.gui.TOOLS"));

        this.setMnemonic(
            GuiActivator.getResources().getI18nMnemonic("service.gui.TOOLS"));

        this.registerMenuItems();

        this.initPluginComponents();
    }

    /**
     * Initialize plugin components already registered for this container.
     */
    private void initPluginComponents()
    {
        // Search for plugin components registered through the OSGI bundle
        // context.
        ServiceReference[] serRefs = null;

        String osgiFilter = "("
            + Container.CONTAINER_ID
            + "="+Container.CONTAINER_TOOLS_MENU.getID()+")";

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
            for (ServiceReference serRef : serRefs)
            {
                final PluginComponent component = (PluginComponent) GuiActivator
                    .bundleContext.getService(serRef);

                SwingUtilities.invokeLater(new Runnable()
                {
                    public void run()
                    {
                        add((Component) component.getComponent());
                    }
                });
            }
        }

        GuiActivator.getUIService().addPluginComponentListener(this);
    }

    /**
     * Handles the <tt>ActionEvent</tt> when one of the menu items is selected.
     * @param e the <tt>ActionEvent</tt> that notified us
     */
    public void actionPerformed(ActionEvent e)
    {
        JMenuItem menuItem = (JMenuItem) e.getSource();
        String itemName = menuItem.getName();

        if (itemName.equalsIgnoreCase("config"))
        {
            configActionPerformed();
        }
        else if (itemName.equals("conference"))
        {
            java.util.List<ProtocolProviderService> confProviders
                = CallManager.getTelephonyConferencingProviders();

            if (confProviders != null && confProviders.size() > 0)
            {
                ConferenceInviteDialog confInviteDialog
                    = new ConferenceInviteDialog();

                confInviteDialog.setVisible(true);
            }
            else
                new ErrorDialog(
                    null,
                    GuiActivator.getResources().getI18NString(
                        "service.gui.WARNING"),
                    GuiActivator.getResources().getI18NString(
                        "service.gui.NO_ONLINE_CONFERENCING_ACCOUNT"))
                .showDialog();
        }
        else if (itemName.equals("showHideOffline"))
        {
            boolean isShowOffline = ConfigurationManager.isShowOffline();

            TreeContactList.presenceFilter.setShowOffline(!isShowOffline);

            GuiActivator.getContactList()
                .setDefaultFilter(TreeContactList.presenceFilter);
            GuiActivator.getContactList().applyDefaultFilter();

            String itemTextKey = !isShowOffline
                    ? "service.gui.HIDE_OFFLINE_CONTACTS"
                    : "service.gui.SHOW_OFFLINE_CONTACTS";

            menuItem.setText(
                GuiActivator.getResources().getI18NString(itemTextKey));
        }
        else if (itemName.equals("sound"))
        {
            boolean mute = !GuiActivator.getAudioNotifier().isMute();

            GuiActivator.getAudioNotifier().setMute(mute);
            {
                // Distribute the mute state to the SoundNotificaitonHandler.
                for(NotificationHandler handler
                        : GuiActivator.getNotificationService()
                            .getActionHandlers(NotificationAction.ACTION_SOUND))
                {
                    if(handler instanceof SoundNotificationHandler)
                    {
                        SoundNotificationHandler soundHandler
                            = (SoundNotificationHandler) handler;

                        soundHandler.setMute(mute);
                    }
                }
            }

            menuItem.setText(
                    GuiActivator.getResources().getI18NString(
                            mute
                                ? "service.gui.SOUND_ON"
                                : "service.gui.SOUND_OFF"));
        }
    }

    /**
     * Shows the configuration window.
     */
    void configActionPerformed()
    {
        GuiActivator.getUIService().getConfigurationContainer().setVisible(true);
    }

    /**
     * Adds the plugin component contained in the event to this container.
     * @param event the <tt>PluginComponentEvent</tt> that notified us
     */
    public void pluginComponentAdded(PluginComponentEvent event)
    {
        final PluginComponent c = event.getPluginComponent();

        if(c.getContainer().equals(Container.CONTAINER_TOOLS_MENU))
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    add((Component) c.getComponent());
                }
            });

            this.revalidate();
            this.repaint();
        }
    }

    /**
     * Indicates that a plugin component has been removed. Removes it from this
     * container if it is contained in it.
     * @param event the <tt>PluginComponentEvent</tt> that notified us
     */
    public void pluginComponentRemoved(PluginComponentEvent event)
    {
        final PluginComponent c = event.getPluginComponent();

        if(c.getContainer().equals(Container.CONTAINER_TOOLS_MENU))
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    remove((Component) c.getComponent());
                }
            });
        }
    }

    /**
     * Registers all menu items.
     */
    private void registerMenuItems()
    {
        // We only add the options button if the property SHOW_OPTIONS_WINDOW
        // specifies so or if it's not set.
        Boolean showOptionsProp
            = GuiActivator.getConfigurationService()
                .getBoolean(ConfigurationFrame.SHOW_OPTIONS_WINDOW_PROPERTY,
                            true);

        if (showOptionsProp.booleanValue())
        {
            UIService uiService = GuiActivator.getUIService();
            if ((uiService == null) || !uiService.useMacOSXScreenMenuBar()
                || !registerConfigMenuItemMacOSX())
            {
                registerConfigMenuItemNonMacOSX();
            }
        }

        conferenceMenuItem = new JMenuItem(
            GuiActivator.getResources().getI18NString(
                "service.gui.CREATE_CONFERENCE_CALL"));

        conferenceMenuItem.setMnemonic(GuiActivator.getResources()
            .getI18nMnemonic("service.gui.CREATE_CONFERENCE_CALL"));
        conferenceMenuItem.setName("conference");
        conferenceMenuItem.addActionListener(this);
        this.add(conferenceMenuItem);

        initVideoBridgeMenu();

        if(!GuiActivator.getConfigurationService().getBoolean(
            AUTO_ANSWER_MENU_DISABLED_PROP,
            false))
        {
            if(ConfigurationManager.isAutoAnswerDisableSubmenu())
            {
                this.addSeparator();
                AutoAnswerMenu.registerMenuItems(this);
            }
            else
            {
                AutoAnswerMenu autoAnswerMenu = new AutoAnswerMenu();
                this.add(autoAnswerMenu);
            }
        }

        this.addSeparator();

        // Show/hide offline contacts menu item.
        String offlineTextKey = ConfigurationManager.isShowOffline()
                            ? "service.gui.HIDE_OFFLINE_CONTACTS"
                            : "service.gui.SHOW_OFFLINE_CONTACTS";

        hideOfflineMenuItem = new JMenuItem(
            GuiActivator.getResources().getI18NString(offlineTextKey));

        hideOfflineMenuItem.setMnemonic(GuiActivator.getResources()
            .getI18nMnemonic(offlineTextKey));
        hideOfflineMenuItem.setName("showHideOffline");
        hideOfflineMenuItem.addActionListener(this);
        this.add(hideOfflineMenuItem);

        // Sound on/off menu item.
        String soundTextKey = GuiActivator.getAudioNotifier().isMute()
                            ? "service.gui.SOUND_ON"
                            : "service.gui.SOUND_OFF";

        soundMenuItem = new JMenuItem(
            GuiActivator.getResources().getI18NString(soundTextKey));

        soundMenuItem.setMnemonic(GuiActivator.getResources()
            .getI18nMnemonic(soundTextKey));
        soundMenuItem.setName("sound");
        soundMenuItem.addActionListener(this);
        this.add(soundMenuItem);

        // All items are now instantiated and we could safely load the skin.
        loadSkin();
    }

    /**
     * Returns a list of all available video bridge providers.
     *
     * @return a list of all available video bridge providers
     */
    private List<ProtocolProviderService> getVideoBridgeProviders()
    {
        List<ProtocolProviderService> activeBridgeProviders
            = new ArrayList<ProtocolProviderService>();

        for (ProtocolProviderService videoBridgeProvider
                : GuiActivator.getRegisteredProviders(
                        OperationSetVideoBridge.class))
        {
            OperationSetVideoBridge videoBridgeOpSet
                = videoBridgeProvider.getOperationSet(
                    OperationSetVideoBridge.class);

            // Check if the video bridge is actually active before adding it to
            // the list of active providers.
            if (videoBridgeOpSet.isActive())
                activeBridgeProviders.add(videoBridgeProvider);
        }

        return activeBridgeProviders;
    }

    /**
     * Initializes the appropriate video bridge menu depending on how many
     * registered providers do we have that support the
     * <tt>OperationSetVideoBridge</tt>.
     */
    private void initVideoBridgeMenu()
    {
        // If already created remove the previous menu in order to reinitialize
        // it.
        if (videoBridgeMenuItem != null)
        {
            remove(videoBridgeMenuItem);
            videoBridgeMenuItem = null;
        }
        else
        {
            // For now we re-init the video bridge menu item each time the
            // parent menu is selected in order to be able to refresh the list
            // of available video bridge active providers.
            addMenuListener(new MenuListener()
            {
                public void menuSelected(MenuEvent arg0)
                {
                    initVideoBridgeMenu();
                }

                public void menuDeselected(MenuEvent arg0) {}

                public void menuCanceled(MenuEvent arg0) {}
            });
        }

        List<ProtocolProviderService> videoBridgeProviders
            = getVideoBridgeProviders();

        // Add a service listener in order to be notified when a new protocol
        // privder is added or removed and the list should be refreshed.
        GuiActivator.bundleContext.addServiceListener(this);

        if (videoBridgeProviders == null || videoBridgeProviders.size() <= 0)
        {
            videoBridgeMenuItem = new VideoBridgeProviderMenuItem(
                GuiActivator.getResources().getI18NString(
                    "service.gui.CREATE_VIDEO_BRIDGE"), null);
            videoBridgeMenuItem.setEnabled(false);
        }
        else if (videoBridgeProviders.size() == 1)
        {
            videoBridgeMenuItem = new VideoBridgeProviderMenuItem(
                GuiActivator.getResources().getI18NString(
                    "service.gui.CREATE_VIDEO_BRIDGE"),
                    videoBridgeProviders.get(0));
            videoBridgeMenuItem.setName("videoBridge");
            videoBridgeMenuItem.addActionListener(this);
        }
        else if (videoBridgeProviders.size() > 1)
        {
            videoBridgeMenuItem = new SIPCommMenu(
                GuiActivator.getResources().getI18NString(
                    "service.gui.CREATE_VIDEO_BRIDGE_MENU"));

            for (ProtocolProviderService videoBridgeProvider
                                                        : videoBridgeProviders)
            {
                VideoBridgeProviderMenuItem videoBridgeItem
                    = new VideoBridgeProviderMenuItem(videoBridgeProvider);

                ((JMenu) videoBridgeMenuItem).add(videoBridgeItem);
                videoBridgeItem.setIcon(
                    ImageLoader.getAccountStatusImage(videoBridgeProvider));
            }
        }

        videoBridgeMenuItem.setIcon(GuiActivator.getResources().getImage(
            "service.gui.icons.VIDEO_BRIDGE"));
        videoBridgeMenuItem.setMnemonic(GuiActivator.getResources()
            .getI18nMnemonic("service.gui.CREATE_VIDEO_BRIDGE"));

        insert(videoBridgeMenuItem, 1);
    }

    /**
     * Registers the preferences item in the MacOS X menu.
     * @return <tt>true</tt> if the operation succeeds, otherwise - returns
     * <tt>false</tt>
     */
    private boolean registerConfigMenuItemMacOSX()
    {
        return FileMenu.registerMenuItemMacOSX("Preferences", this);
    }

    /**
     * Registers the settings item for non-MacOS X OS.
     */
    private void registerConfigMenuItemNonMacOSX()
    {
        configMenuItem = new JMenuItem(
            GuiActivator.getResources().getI18NString("service.gui.SETTINGS"),
            GuiActivator.getResources().getImage(
                                "service.gui.icons.CONFIGURE_ICON"));

        this.add(configMenuItem);
        configMenuItem.setMnemonic(GuiActivator.getResources()
            .getI18nMnemonic("service.gui.SETTINGS"));
        configMenuItem.setName("config");
        configMenuItem.addActionListener(this);
    }

    /**
     * Loads menu item icons.
     */
    public void loadSkin()
    {
        conferenceMenuItem.setIcon(GuiActivator.getResources().getImage(
                "service.gui.icons.CONFERENCE_CALL"));
        videoBridgeMenuItem.setIcon(GuiActivator.getResources().getImage(
                "service.gui.icons.VIDEO_BRIDGE"));
        hideOfflineMenuItem.setIcon(GuiActivator.getResources().getImage(
                "service.gui.icons.SHOW_HIDE_OFFLINE_ICON"));
        soundMenuItem.setIcon(GuiActivator.getResources().getImage(
                "service.gui.icons.SOUND_MENU_ICON"));

        if(configMenuItem != null)
        {
            configMenuItem.setIcon(GuiActivator.getResources().getImage(
                    "service.gui.icons.CONFIGURE_ICON"));
        }
    }

    /**
     * The <tt>VideoBridgeProviderMenuItem</tt> for each protocol provider.
     */
    private class VideoBridgeProviderMenuItem
        extends JMenuItem
        implements ActionListener
    {
        private final ProtocolProviderService protocolProvider;

        /**
         * Creates an instance of <tt>VideoBridgeProviderMenuItem</tt> by
         * specifying the corresponding <tt>ProtocolProviderService</tt> that
         * provides the video bridge.
         *
         * @param protocolProvider the <tt>ProtocolProviderService</tt> that
         * provides the video bridge
         */
        public VideoBridgeProviderMenuItem(
            ProtocolProviderService protocolProvider)
        {
            this (null, protocolProvider);
        }

        /**
         * Creates an instance of <tt>VideoBridgeProviderMenuItem</tt> by
         * specifying the corresponding <tt>ProtocolProviderService</tt> that
         * provides the video bridge.
         *
         * @param name the name of the menu item
         * @param protocolProvider the <tt>ProtocolProviderService</tt> that
         * provides the video bridge
         */
        public VideoBridgeProviderMenuItem(
                                    String name,
                                    ProtocolProviderService protocolProvider)
        {
            if (name != null && name.length() > 0)
                setText(name);
            else
                setText(protocolProvider.getAccountID().getDisplayName());

            this.protocolProvider = protocolProvider;

            addActionListener(this);
        }

        /**
         * Opens a conference invite dialog when this menu is selected.
         */
        public void actionPerformed(ActionEvent arg0)
        {
            new ConferenceInviteDialog(protocolProvider, true).setVisible(true);
        }
    }

    /**
     * Implements the <tt>ServiceListener</tt> method. Verifies whether the
     * passed event concerns a <tt>ProtocolProviderService</tt> and adds the
     * corresponding UI controls in the menu.
     *
     * @param event The <tt>ServiceEvent</tt> object.
     */
    public void serviceChanged(ServiceEvent event)
    {
        ServiceReference serviceRef = event.getServiceReference();

        // if the event is caused by a bundle being stopped, we don't want to
        // know
        if (serviceRef.getBundle().getState() == Bundle.STOPPING)
        {
            return;
        }

        Object service = GuiActivator.bundleContext.getService(serviceRef);

        // we don't care if the source service is not a protocol provider
        if (!(service instanceof ProtocolProviderService))
        {
            return;
        }

        switch (event.getType())
        {
            case ServiceEvent.REGISTERED:
            case ServiceEvent.UNREGISTERING:
                initVideoBridgeMenu();
                break;
        }
    }
}
