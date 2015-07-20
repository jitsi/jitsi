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
package net.java.sip.communicator.impl.gui.main.menus;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.event.*;
import net.java.sip.communicator.impl.gui.main.call.*;
import net.java.sip.communicator.impl.gui.main.call.conference.*;
import net.java.sip.communicator.impl.gui.main.configforms.*;
import net.java.sip.communicator.impl.gui.main.contactlist.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.plugin.desktoputil.SwingWorker;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.Container;
import net.java.sip.communicator.service.notification.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.Logger;
import net.java.sip.communicator.util.account.*;
import net.java.sip.communicator.util.skin.*;

import org.jitsi.service.configuration.*;
import org.jitsi.service.resources.*;
import org.jitsi.util.*;
import org.osgi.framework.*;

/**
 * The <tt>ToolsMenu</tt> is a menu in the contact list / chat panel bars that
 * contains "Tools". This menu is separated in different sections by
 * <tt>JSeparator</tt>s. These sections are ordered in the following matter:
 * 0 0 0 | 1 1 | 2 2... where numbers indicate the indices of the corresponding
 * sections and | are separators. Currently, section 0 contains "Options",
 * "Create a video bridge", "Create a conference call"... until the first
 * <tt>JSeparator</tt> after which starts the next section - section 1.
 *
 * @author Yana Stamcheva
 * @author Lyubomir Marinov
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
     * Property to disable conference initialization.
     */
    private static final String CONFERENCE_CALL_DISABLED_PROP =
        "net.java.sip.communicator.impl.gui.main.menus"
            + ".CONFERENCE_CALL_MENU_ITEM_DISABLED";

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
     * The <tt>SwingWorker</tt> creating the video bridge menu item depending
     * on the number of <tt>ProtocolProviderService</tt>-s supporting the video
     * bridge functionality.
     */
    private SwingWorker initVideoBridgeMenuWorker;

    /**
     * The menu listener that would update the video bridge menu item every
     * time the user clicks on the Tools menu.
     */
    private MenuListener videoBridgeMenuListener;

    /**
     * Indicates if this menu is shown for the chat window or the contact list
     * window.
     */
    private boolean isChatMenu;

    /**
     * Creates an instance of <tt>FileMenu</tt>.
     */
    public ToolsMenu()
    {
        this(false);
    }

    /**
     * Creates an instance of <tt>FileMenu</tt>, by specifying if this menu
     * would be shown for a chat window.
     *
     * @param isChatMenu indicates if this menu would be shown for a chat
     * window
     */
    public ToolsMenu(boolean isChatMenu)
    {
        this.isChatMenu = isChatMenu;

        ResourceManagementService r = GuiActivator.getResources();

        setText(r.getI18NString("service.gui.TOOLS"));
        setMnemonic(r.getI18nMnemonic("service.gui.TOOLS"));

        registerMenuItems();

        initPluginComponents();
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
                PluginComponentFactory.class.getName(),
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
                final PluginComponentFactory f = (PluginComponentFactory) GuiActivator
                    .bundleContext.getService(serRef);

                SwingUtilities.invokeLater(new Runnable()
                {
                    public void run()
                    {
                        PluginComponent pluginComponent =
                            f.getPluginComponentInstance(ToolsMenu.this);
                        insertInSection(
                            (JMenuItem) pluginComponent.getComponent(),
                            pluginComponent.getPositionIndex());
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

        if (itemName == null)
            return;

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
            {
                ResourceManagementService r = GuiActivator.getResources();

                new ErrorDialog(
                        null,
                        r.getI18NString("service.gui.WARNING"),
                        r.getI18NString(
                                "service.gui.NO_ONLINE_CONFERENCING_ACCOUNT"))
                    .showDialog();
            }
        }
        else if (itemName.equals("showHideOffline"))
        {
            boolean isShowOffline = ConfigurationUtils.isShowOffline();

            TreeContactList.presenceFilter.setShowOffline(!isShowOffline);

            // Only re-apply the filter if the presence filter is showing.
            // Otherwise we might end up with contacts in the call history
            if (GuiActivator.getContactList().getDefaultFilter() ==
                    TreeContactList.presenceFilter)
            {
                GuiActivator.getContactList()
                    .setDefaultFilter(TreeContactList.presenceFilter);
                GuiActivator.getContactList().applyDefaultFilter();
            }

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
        GuiActivator.getUIService()
            .getConfigurationContainer().setVisible(true);
    }

    /**
     * Adds the plugin component contained in the event to this container.
     * @param event the <tt>PluginComponentEvent</tt> that notified us
     */
    public void pluginComponentAdded(PluginComponentEvent event)
    {
        final PluginComponentFactory c = event.getPluginComponentFactory();

        if(c.getContainer().equals(Container.CONTAINER_TOOLS_MENU))
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    PluginComponent pluginComponent =
                        c.getPluginComponentInstance(ToolsMenu.this);
                    insertInSection(
                        (JMenuItem) pluginComponent.getComponent(),
                        pluginComponent.getPositionIndex());
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
        final PluginComponentFactory c = event.getPluginComponentFactory();

        if(c.getContainer().equals(Container.CONTAINER_TOOLS_MENU))
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    remove((Component) c.getPluginComponentInstance(ToolsMenu.this).getComponent());
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
        ConfigurationService cfg = GuiActivator.getConfigurationService();
        Boolean showOptionsProp
            = cfg.getBoolean(
                    ConfigurationFrame.SHOW_OPTIONS_WINDOW_PROPERTY,
                    true);

        if (showOptionsProp.booleanValue())
        {
            UIService uiService = GuiActivator.getUIService();

            if ((uiService == null)
                    || !uiService.useMacOSXScreenMenuBar()
                    || !registerConfigMenuItemMacOSX())
            {
                registerConfigMenuItemNonMacOSX();
            }
        }

        ResourceManagementService r = GuiActivator.getResources();

        Boolean showConferenceMenuItemProp
            = cfg.getBoolean(CONFERENCE_CALL_DISABLED_PROP,
                            false);

        if(!showConferenceMenuItemProp.booleanValue())
        {
            conferenceMenuItem
                = new JMenuItem(
                    r.getI18NString("service.gui.CREATE_CONFERENCE_CALL"));
            conferenceMenuItem.setMnemonic(
                r.getI18nMnemonic("service.gui.CREATE_CONFERENCE_CALL"));
            conferenceMenuItem.setName("conference");
            conferenceMenuItem.addActionListener(this);
            add(conferenceMenuItem);
        }

        // Add a service listener in order to be notified when a new protocol
        // provider is added or removed and the list should be refreshed.
        GuiActivator.bundleContext.addServiceListener(this);

        initVideoBridgeMenu();

        if(!cfg.getBoolean(AUTO_ANSWER_MENU_DISABLED_PROP, false))
        {
            if(ConfigurationUtils.isAutoAnswerDisableSubmenu())
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
        String offlineTextKey = ConfigurationUtils.isShowOffline()
                            ? "service.gui.HIDE_OFFLINE_CONTACTS"
                            : "service.gui.SHOW_OFFLINE_CONTACTS";

        // The hide offline menu item only makes sense in the contact list.
        if (!isChatMenu)
        {
            hideOfflineMenuItem = new JMenuItem(r.getI18NString(offlineTextKey));
            hideOfflineMenuItem.setMnemonic(r.getI18nMnemonic(offlineTextKey));
            hideOfflineMenuItem.setName("showHideOffline");
            hideOfflineMenuItem.addActionListener(this);
            this.add(hideOfflineMenuItem);
        }

        // Sound on/off menu item.
        String soundTextKey
            = GuiActivator.getAudioNotifier().isMute()
                ? "service.gui.SOUND_ON"
                : "service.gui.SOUND_OFF";

        soundMenuItem = new JMenuItem(r.getI18NString(soundTextKey));
        soundMenuItem.setMnemonic(r.getI18nMnemonic(soundTextKey));
        soundMenuItem.setName("sound");
        soundMenuItem.addActionListener(this);
        this.add(soundMenuItem);

        // All items are now instantiated and we could safely load the skin.
        loadSkin();
    }

    /**
     * Keeps track of the indices of <tt>JSeparator</tt>s
     * that are places within this <tt>Container</tt>
     */
    private List<Integer> separatorIndices = new LinkedList<Integer>();

    /**
     * When a new separator is added to this <tt>Container</tt> its position
     * will be saved in separatorIndices.
     */
    public void addSeparator()
    {
        super.addSeparator();
        separatorIndices.add(this.getMenuComponentCount() - 1);
    }

    /**
     * Inserts the given <tt>JMenuItem</tt> at the end of the specified section.
     * Sections are ordered in the following matter: 0 0 0 | 1 1 | 2 2 ...
     * 
     * @param item The <tt>JMenuItem</tt> that we insert
     * 
     * @param section The section index in which we want to insert the specified
     * <tt>JMenuItem</tt>. If section is < 0 or section is >= the
     * <tt>JSeparator</tt>s count in this menu, this item will be inserted at
     * the end of the menu.
     * 
     * @return The inserted <tt>JMenuItem</tt>
     */
    private JMenuItem insertInSection(JMenuItem item, int section)
    {
        if (section < 0 || section >= separatorIndices.size())
        {
            add(item);
            return item;
        }

        // Gets the index of the separator so we can insert the JMenuItem
        // before it.
        int separatorIndex = separatorIndices.get(section);

        // All following separators' positions must be incremented because we
        // will insert a new JMenuItem before them.
        ListIterator<Integer> it = separatorIndices.listIterator(section);
        while (it.hasNext())
        {
            int i = it.next() + 1;
            it.remove();
            it.add(i);
        }

        insert(item, separatorIndex);
        return item;
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
                : AccountUtils.getRegisteredProviders(
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
        // If video bridge is enabled in the config then add the menu item
        if (GuiActivator.getConfigurationService()
             .getBoolean(OperationSetVideoBridge
                .IS_VIDEO_BRIDGE_DISABLED, false))
        {
            return;
        }

        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    initVideoBridgeMenu();
                }
            });
            return;
        }

        // We create the video default video bridge menu item and set it
        // disabled until we have more information on video bridge support.
        if (videoBridgeMenuItem == null)
        {
            videoBridgeMenuItem = new VideoBridgeProviderMenuItem(
                    GuiActivator.getResources()
                        .getI18NString("service.gui.CREATE_VIDEO_BRIDGE"),
                        null);

            videoBridgeMenuItem.setEnabled(false);

            insert(videoBridgeMenuItem, 1);
        }

        // We re-init the video bridge menu item each time the
        // parent menu is selected in order to be able to refresh the list
        // of available video bridge active providers.
        if (videoBridgeMenuListener == null)
        {
            videoBridgeMenuListener = new VideoBridgeMenuListener();

            addMenuListener(videoBridgeMenuListener);
        }

        // Check the protocol providers supporting video bridge in a new thread.
        if (initVideoBridgeMenuWorker == null)
            initVideoBridgeMenuWorker
                = (OSUtils.IS_MAC)
                    ? new InitVideoBridgeMenuWorkerMacOSX()
                    : new InitVideoBridgeMenuWorker();
        else
            initVideoBridgeMenuWorker.interrupt();

        initVideoBridgeMenuWorker.start();
    }

    /**
     * Runs clean-up for associated resources which need explicit disposal (e.g.
     * listeners keeping this instance alive because they were added to the
     * model which operationally outlives this instance).
     */
    public void dispose()
    {
        GuiActivator.bundleContext.removeServiceListener(this);

        GuiActivator.getUIService().removePluginComponentListener(this);

        /*
         * Let go of all Components contributed by PluginComponents because the
         * latter will still live in the contribution store.
         */
        removeAll();
    }

    /**
     * Initializes the video bridge menu on Mac OSX.
     */
    private class InitVideoBridgeMenuWorkerMacOSX
        extends SwingWorker
    {
        @Override
        protected Object construct()
        {
            Boolean enableMenu = true;
            List<ProtocolProviderService> videoBridgeProviders
                = getVideoBridgeProviders();

            int videoBridgeProviderCount
                = (videoBridgeProviders == null)
                    ? 0 : videoBridgeProviders.size();

            VideoBridgeProviderMenuItem menuItem
                = ((VideoBridgeProviderMenuItem) videoBridgeMenuItem);

            if (videoBridgeProviderCount <= 0)
                enableMenu = false;
            else if (videoBridgeProviderCount == 1)
            {
                menuItem.setPreselectedProvider(videoBridgeProviders.get(0));
                enableMenu = true;
            }
            else if (videoBridgeProviderCount > 1)
            {
                menuItem.setPreselectedProvider(null);
                menuItem.setVideoBridgeProviders(videoBridgeProviders);
                enableMenu = true;
            }

            return enableMenu;
        }

        /**
         * Called on the event dispatching thread (not on the worker thread)
         * after the <code>construct</code> method has returned.
         */
        @Override
        protected void finished()
        {
            Boolean enabled = (Boolean) get();
            if (enabled != null)
                videoBridgeMenuItem.setEnabled(enabled);
        }
    }

    /**
     * The <tt>InitVideoBridgeMenuWorker</tt> initializes the video bridge
     * menu item depending on the number of providers currently supporting video
     * bridge calls.
     */
    private class InitVideoBridgeMenuWorker
        extends SwingWorker
    {
        private ResourceManagementService r = GuiActivator.getResources();

        @Override
        protected Object construct() //throws Exception
        {
            return getVideoBridgeProviders();
        }

        /**
         * Creates the menu item.
         * @param videoBridgeProviders the list of available providers.
         * @return the list of available providers.
         */
        private JMenuItem createNewMenuItem(
            List<ProtocolProviderService> videoBridgeProviders)
        {
            int videoBridgeProviderCount
                = (videoBridgeProviders == null)
                    ? 0 : videoBridgeProviders.size();

            JMenuItem newMenuItem = null;
            if (videoBridgeProviderCount <= 0)
            {
                newMenuItem
                    = new VideoBridgeProviderMenuItem(
                            r.getI18NString("service.gui.CREATE_VIDEO_BRIDGE"),
                            null);
                newMenuItem.setEnabled(false);
            }
            else if (videoBridgeProviderCount == 1)
            {
                newMenuItem
                    = new VideoBridgeProviderMenuItem(
                            r.getI18NString("service.gui.CREATE_VIDEO_BRIDGE"),
                            videoBridgeProviders.get(0));
                newMenuItem.setName("videoBridge");
                newMenuItem.addActionListener(ToolsMenu.this);
            }
            else if (videoBridgeProviderCount > 1)
            {
                newMenuItem
                    = new SIPCommMenu(
                            r.getI18NString(
                                    "service.gui.CREATE_VIDEO_BRIDGE_MENU"));

                for (ProtocolProviderService videoBridgeProvider
                        : videoBridgeProviders)
                {
                    VideoBridgeProviderMenuItem videoBridgeItem
                        = new VideoBridgeProviderMenuItem(videoBridgeProvider);

                    ((JMenu) newMenuItem).add(videoBridgeItem);
                    videoBridgeItem.setIcon(
                        ImageLoader.getAccountStatusImage(videoBridgeProvider));
                }
            }
            return newMenuItem;
        }

        @Override
        @SuppressWarnings("unchecked")
        protected void finished()
        {
            if (videoBridgeMenuItem != null)
            {
                // If the menu item is already created we're going to remove it
                // in order to reinitialize it.
                remove(videoBridgeMenuItem);
            }

            // create the menu items in event dispatch thread
            videoBridgeMenuItem =
                createNewMenuItem((List<ProtocolProviderService>)get());

            videoBridgeMenuItem.setIcon(
                    r.getImage("service.gui.icons.VIDEO_BRIDGE"));
            videoBridgeMenuItem.setMnemonic(
                    r.getI18nMnemonic("service.gui.CREATE_VIDEO_BRIDGE"));

            insert(videoBridgeMenuItem, 1);

            if (isPopupMenuVisible())
            {
                java.awt.Container c = videoBridgeMenuItem.getParent();

                if (c instanceof JComponent)
                {
                    ((JComponent) c).revalidate();
                }
                c.repaint();
            }
        }
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
        ResourceManagementService r = GuiActivator.getResources();

        configMenuItem
            = new JMenuItem(
                    r.getI18NString("service.gui.SETTINGS"),
                    r.getImage("service.gui.icons.CONFIGURE_ICON"));
        add(configMenuItem);
        configMenuItem.setMnemonic(
                r.getI18nMnemonic("service.gui.SETTINGS"));
        configMenuItem.setName("config");
        configMenuItem.addActionListener(this);
    }

    /**
     * Loads menu item icons.
     */
    public void loadSkin()
    {
        ResourceManagementService r = GuiActivator.getResources();

        if (conferenceMenuItem != null)
        {
            conferenceMenuItem.setIcon(
                    r.getImage("service.gui.icons.CONFERENCE_CALL"));
        }

        if (configMenuItem != null)
        {
            configMenuItem.setIcon(
                    r.getImage("service.gui.icons.CONFIGURE_ICON"));
        }

        // The hide offline menu item could be null if the parent window of this
        // menu is a chat window.
        if (hideOfflineMenuItem != null)
            hideOfflineMenuItem.setIcon(
                    r.getImage("service.gui.icons.SHOW_HIDE_OFFLINE_ICON"));

        soundMenuItem.setIcon(
                r.getImage("service.gui.icons.SOUND_MENU_ICON"));
        if (videoBridgeMenuItem != null)
        {
            videoBridgeMenuItem.setIcon(
                    r.getImage("service.gui.icons.VIDEO_BRIDGE"));
        }
    }

    /**
     * The <tt>VideoBridgeProviderMenuItem</tt> for each protocol provider.
     */
    @SuppressWarnings("serial")
    private class VideoBridgeProviderMenuItem
        extends JMenuItem
        implements ActionListener
    {
        private ProtocolProviderService preselectedProvider;

        private List<ProtocolProviderService> videoBridgeProviders;

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
         * @param preselectedProvider the <tt>ProtocolProviderService</tt> that
         * provides the video bridge
         */
        public VideoBridgeProviderMenuItem(
                                    String name,
                                    ProtocolProviderService preselectedProvider)
        {
            if (name != null && name.length() > 0)
                setText(name);
            else
                setText(preselectedProvider.getAccountID().getDisplayName());

            this.preselectedProvider = preselectedProvider;

            ResourceManagementService r = GuiActivator.getResources();

            setIcon(r.getImage("service.gui.icons.VIDEO_BRIDGE"));
            setMnemonic(r.getI18nMnemonic("service.gui.CREATE_VIDEO_BRIDGE"));

            addActionListener(this);
        }

        /**
         * Opens a conference invite dialog when this menu is selected.
         */
        public void actionPerformed(ActionEvent event)
        {
            ConferenceInviteDialog inviteDialog;

            if (preselectedProvider != null)
                inviteDialog
                    = new ConferenceInviteDialog(preselectedProvider, true);
            else
                inviteDialog
                    = new ConferenceInviteDialog(videoBridgeProviders, true);

            inviteDialog.setVisible(true);
        }

        public void setPreselectedProvider(
                ProtocolProviderService protocolProvider)
        {
            this.preselectedProvider = protocolProvider;
        }

        public void setVideoBridgeProviders(
                List<ProtocolProviderService> videoBridgeProviders)
        {
            this.videoBridgeProviders = videoBridgeProviders;
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

    private class VideoBridgeMenuListener implements MenuListener
    {
        public void menuSelected(MenuEvent arg0)
        {
            initVideoBridgeMenu();
        }

        public void menuDeselected(MenuEvent arg0) {}

        public void menuCanceled(MenuEvent arg0) {}
    }
}
