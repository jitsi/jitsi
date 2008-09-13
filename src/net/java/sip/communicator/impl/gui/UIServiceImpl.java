/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui;

import java.awt.*;
import java.util.*;
import java.util.List;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.event.*;
import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.lookandfeel.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.main.account.*;
import net.java.sip.communicator.impl.gui.main.chat.*;
import net.java.sip.communicator.impl.gui.main.chat.conference.*;
import net.java.sip.communicator.impl.gui.main.configforms.*;
import net.java.sip.communicator.impl.gui.main.contactlist.addcontact.*;
import net.java.sip.communicator.impl.gui.main.login.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.configuration.event.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.Container;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

import com.sun.jna.examples.*;

import org.osgi.framework.*;

/**
 * An implementation of the <tt>UIService</tt> that gives access to other
 * bundles to this particular swing ui implementation.
 *
 * @author Yana Stamcheva
 */
public class UIServiceImpl
    implements  UIService,
                ServiceListener,
                PropertyChangeListener
{
    private static final Logger logger = Logger.getLogger(UIServiceImpl.class);

    private PopupDialogImpl popupDialog;

    private AccountRegWizardContainerImpl wizardContainer;

    private Map<Container, Vector<Object>> registeredPlugins
        = new Hashtable<Container, Vector<Object>>();

    private Vector<PluginComponentListener>
        pluginComponentListeners = new Vector<PluginComponentListener>();

    private static final List<Container> supportedContainers
        = new ArrayList<Container>();
    static
    {
        supportedContainers.add(Container.CONTAINER_MAIN_TOOL_BAR);
        supportedContainers.add(Container.CONTAINER_CONTACT_RIGHT_BUTTON_MENU);
        supportedContainers.add(Container.CONTAINER_GROUP_RIGHT_BUTTON_MENU);
        supportedContainers.add(Container.CONTAINER_TOOLS_MENU);
        supportedContainers.add(Container.CONTAINER_HELP_MENU);
        supportedContainers.add(Container.CONTAINER_CHAT_TOOL_BAR);
        supportedContainers.add(Container.CONTAINER_CALL_HISTORY);
        supportedContainers.add(Container.CONTAINER_MAIN_TABBED_PANE);
        supportedContainers.add(Container.CONTAINER_CHAT_HELP_MENU);
    }

    private static final Hashtable<WindowID, ExportedWindow> exportedWindows
        = new Hashtable<WindowID, ExportedWindow>();

    private MainFrame mainFrame;

    private LoginManager loginManager;

    private ConfigurationFrame configurationFrame;

    private boolean exitOnClose = true;

    /**
     * Creates an instance of <tt>UIServiceImpl</tt>.
     */
    public UIServiceImpl()
    {
    }

    /**
     * Initializes all frames and panels and shows the gui.
     */
    public void loadApplicationGui()
    {
        this.setDefaultThemePack();

        this.mainFrame = new MainFrame();

        GuiActivator.getUIService().registerExportedWindow(mainFrame);

        this.loginManager = new LoginManager(mainFrame);

        this.popupDialog = new PopupDialogImpl();

        this.wizardContainer = new AccountRegWizardContainerImpl(mainFrame);

        this.configurationFrame = new ConfigurationFrame(mainFrame);

        mainFrame.setContactList(GuiActivator.getMetaContactListService());

        if (ConfigurationManager.isTransparentWindowEnabled())
        {
            try
            {
                WindowUtils.setWindowTransparent(mainFrame, true);
            }
            catch (UnsupportedOperationException ex)
            {
                logger.error(ex.getMessage(), ex);
                ConfigurationManager.setTransparentWindowEnabled(false);
            }
        }

        if(ConfigurationManager.isApplicationVisible())
            mainFrame.setVisible(true);

        SwingUtilities.invokeLater(new RunLoginGui());

        this.initExportedWindows();
    }

    /**
     * Implements <code>UISercie.getSupportedContainers</code>. Returns the
     * list of supported containers by this implementation .
     *
     * @see UIService#getSupportedContainers()
     * @return an Iterator over all supported containers.
     */
    public Iterator getSupportedContainers()
    {
        return Collections.unmodifiableList(supportedContainers).iterator();
    }

    /**
     * Creates the corresponding PluginComponentEvent and notifies all
     * <tt>ContainerPluginListener</tt>s that a plugin component is added or
     * removed from the container.
     *
     * @param pluginComponent the plugin component that is added to the
     *            container.
     * @param containerID the containerID that corresponds to the container
     *            where the component is added.
     * @param eventID one of the PLUGIN_COMPONENT_XXX static fields indicating
     *            the nature of the event.
     */
    private void firePluginEvent(   PluginComponent pluginComponent,
                                    int eventID)
    {
        PluginComponentEvent evt
            = new PluginComponentEvent( pluginComponent,
                                        eventID);

        logger.debug("Will dispatch the following plugin component event: "
            + evt);

        synchronized (pluginComponentListeners)
        {
            Iterator listeners = this.pluginComponentListeners.iterator();

            while (listeners.hasNext())
            {
                PluginComponentListener l = (PluginComponentListener) listeners
                    .next();

                switch (evt.getEventID())
                {
                case PluginComponentEvent.PLUGIN_COMPONENT_ADDED:
                    l.pluginComponentAdded(evt);
                    break;
                case PluginComponentEvent.PLUGIN_COMPONENT_REMOVED:
                    l.pluginComponentRemoved(evt);
                    break;
                default:
                    logger.error("Unknown event type " + evt.getEventID());
                }
            }
        }
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
        return mainFrame.isVisible();
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
        this.mainFrame.setVisible(isVisible);
    }

    /**
     * Locates the main application window to the new x and y coordinates.
     *
     * @param x The new x coordinate.
     * @param y The new y coordinate.
     */
    public void setLocation(int x, int y)
    {
        mainFrame.setLocation(x, y);
    }

    /**
     * Returns the current location of the main application window. The returned
     * point is the top left corner of the window.
     *
     * @return The top left corner coordinates of the main application window.
     */
    public Point getLocation()
    {
        return mainFrame.getLocation();
    }

    /**
     * Returns the size of the main application window.
     *
     * @return the size of the main application window.
     */
    public Dimension getSize()
    {
        return mainFrame.getSize();
    }

    /**
     * Sets the size of the main application window.
     *
     * @param width The width of the window.
     * @param height The height of the window.
     */
    public void setSize(int width, int height)
    {
        mainFrame.setSize(width, height);
    }

    /**
     * Implements <code>minimize</code> in the UIService interface. Minimizes
     * the main application window.
     *
     * @see UIService#minimize()
     */
    public void minimize()
    {
        this.mainFrame.minimize();
    }

    /**
     * Implements <code>maximize</code> in the UIService interface. Maximizes
     * the main application window.
     *
     * @see UIService#maximize()
     */
    public void maximize()
    {
        this.mainFrame.maximize();
    }

    /**
     * Implements <code>restore</code> in the UIService interface. Restores
     * the main application window.
     *
     * @see UIService#restore()
     */
    public void restore()
    {
        if (mainFrame.isVisible())
        {
            if (mainFrame.getState() == JFrame.ICONIFIED)
                mainFrame.setState(JFrame.NORMAL);

            mainFrame.toFront();
        }
        else
            mainFrame.setVisible(true);
    }

    /**
     * Implements <code>resize</code> in the UIService interface. Resizes the
     * main application window.
     *
     * @param height the new height of tha main application frame.
     * @param width the new width of the main application window.
     *
     * @see UIService#resize(int, int)
     */
    public void resize(int width, int height)
    {
        this.mainFrame.setSize(width, height);
    }

    /**
     * Implements <code>move</code> in the UIService interface. Moves the main
     * application window to the point with coordinates - x, y.
     *
     * @param x the value of X where the main application frame is to be placed.
     * @param y the value of Y where the main application frame is to be placed.
     *
     * @see UIService#move(int, int)
     */
    public void move(int x, int y)
    {
        this.mainFrame.setLocation(x, y);
    }

    /**
     * Implements the <code>UIService.setExitOnMainWindowClose</code>. Sets a
     * boolean property, which indicates whether the application should be
     * exited when the main application window is closed.
     *
     * @param exitOnClose specifies if closing the main application window
     * should also be exiting the application.
     */
    public void setExitOnMainWindowClose(boolean exitOnClose)
    {
        this.exitOnClose = exitOnClose;

        if (exitOnClose)
            mainFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        else
            mainFrame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
    }

    /**
     * Implements the <code>UIService.getExitOnMainWindowClose</code>.
     * Returns the boolean property, which indicates whether the application
     * should be exited when the main application window is closed.
     *
     * @return determines whether the UI impl would exit the application when
     * the main application window is closed.
     */
    public boolean getExitOnMainWindowClose()
    {
        return this.exitOnClose;
    }

    /**
     * Adds all <tt>ExportedWindow</tt>s to the list of application windows,
     * which could be used from other bundles. Once registered in the
     * <tt>UIService</tt> this window could be obtained through the
     * <tt>getExportedWindow(WindowID)</tt> method and could be shown,
     * hidden, resized, moved, etc.
     */
    public void initExportedWindows()
    {
        registerExportedWindow(configurationFrame);
        registerExportedWindow(new AddContactWizardExportedWindow(mainFrame));
    }

    /**
     * Registers the given <tt>ExportedWindow</tt> to the list of windows that
     * could be accessed from other bundles.
     *
     * @param window the window to be exported
     */
    public void registerExportedWindow(ExportedWindow window)
    {
        exportedWindows.put(window.getIdentifier(), window);
    }

    /**
     * Unregisters the given <tt>ExportedWindow</tt> from the list of windows
     * that could be accessed from other bundles.
     *
     * @param window the window to no longer be exported
     */
    public void unregisterExportedWindow(ExportedWindow window)
    {
        WindowID identifier = window.getIdentifier();
        ExportedWindow removed = exportedWindows.remove(identifier);

        /*
         * In case the unexpected happens and we happen to have the same
         * WindowID for multiple ExportedWindows going through
         * #registerExportedWindow(), we have to make sure we're not
         * unregistering some other ExportedWindow which has overwritten the
         * registration of the specified window.
         */
        if ((removed != null) && !removed.equals(window))
        {

            /*
             * We accidentally unregistered another window so bring back its
             * registration.
             */
            exportedWindows.put(identifier, removed);

            /* Now unregister the right window. */
            for (Iterator<Map.Entry<WindowID, ExportedWindow>> entryIt =
                exportedWindows.entrySet().iterator(); entryIt.hasNext();)
            {
                Map.Entry<WindowID, ExportedWindow> entry = entryIt.next();
                if (window.equals(entry.getValue()))
                    entryIt.remove();
            }
        }
    }

    /**
     * Sets the contact list service to this UI Service implementation.
     * @param contactList the MetaContactList service
     */
    public void setContactList(MetaContactListService contactList)
    {
        this.mainFrame.setContactList(contactList);
    }

    public void addPluginComponentListener(PluginComponentListener l)
    {
        synchronized (pluginComponentListeners)
        {
            pluginComponentListeners.add(l);
        }
    }

    public void removePluginComponentListener(PluginComponentListener l)
    {
        synchronized (pluginComponentListeners)
        {
            pluginComponentListeners.remove(l);
        }
    }

    /**
     * Implements <code>getSupportedExportedWindows</code> in the UIService
     * interface. Returns an iterator over a set of all windows exported by
     * this implementation.
     *
     * @return an Iterator over all windows exported by this implementation of
     * the UI service.
     *
     * @see UIService#getSupportedExportedWindows()
     */
    public Iterator getSupportedExportedWindows()
    {
        return Collections.unmodifiableMap(exportedWindows).keySet().iterator();
    }

    /**
     * Implements the <code>getExportedWindow</code> in the UIService
     * interface. Returns the window corresponding to the given
     * <tt>WindowID</tt>.
     *
     * @param windowID the id of the window we'd like to retrieve.
     *
     * @return a reference to the <tt>ExportedWindow</tt> instance corresponding
     * to <tt>windowID</tt>.
     * @see UIService#getExportedWindow(WindowID)
     */
    public ExportedWindow getExportedWindow(WindowID windowID)
    {
        if (exportedWindows.containsKey(windowID))
        {
            return (ExportedWindow) exportedWindows.get(windowID);
        }
        return null;
    }

    /**
     * Implements the <code>UIService.isExportedWindowSupported</code> method.
     * Checks if there's an exported component for the given
     * <tt>WindowID</tt>.
     *
     * @param windowID the id of the window that we're making the query for.
     *
     * @return true if a window with the corresponding windowID is exported by
     * the UI service implementation and false otherwise.
     *
     * @see UIService#isExportedWindowSupported(WindowID)
     */
    public boolean isExportedWindowSupported(WindowID windowID)
    {
        return exportedWindows.containsKey(windowID);
    }

    /**
     * Implements <code>getPopupDialog</code> in the UIService interface.
     * Returns a <tt>PopupDialog</tt> that could be used to show simple
     * messages, warnings, errors, etc.
     *
     * @return a <tt>PopupDialog</tt> that could be used to show simple
     * messages, warnings, errors, etc.
     *
     * @see UIService#getPopupDialog()
     */
    public PopupDialog getPopupDialog()
    {
        return this.popupDialog;
    }

    /**
     * Implements <code>getChat</code> in the UIService interface. If a
     * chat for the given contact exists already - returns it, otherwise
     * creates a new one.
     *
     * @param contact the contact that we'd like to retrieve a chat window for.
     *
     * @return a Chat corresponding to the specified contact.
     *
     * @see UIService#getChat(Contact)
     */
    public Chat getChat(Contact contact)
    {
        MetaContact metaContact = mainFrame.getContactList()
            .findMetaContactByContact(contact);

        ChatWindowManager chatWindowManager = mainFrame.getChatWindowManager();

        MetaContactChatPanel chatPanel
            = chatWindowManager.getContactChat(metaContact);

        return chatPanel;
    }

    /**
     * Returns the <tt>Chat</tt> corresponding to the given <tt>ChatRoom</tt>.
     *
     * @param chatRoom the <tt>ChatRoom</tt> for which the searched chat is
     * about.
     * @return the <tt>Chat</tt> corresponding to the given <tt>ChatRoom</tt>.
     */
    public Chat getChat(ChatRoom chatRoom)
    {
        ChatWindowManager chatWindowManager = mainFrame.getChatWindowManager();

        ConferenceChatPanel chatPanel
            = chatWindowManager.getMultiChat(chatRoom);

        return chatPanel;
    }

    /**
     * Returns the selected <tt>Chat</tt>.
     *
     * @return the selected <tt>Chat</tt>.
     */
    public Chat getCurrentChat()
    {
        ChatWindowManager chatWindowManager = mainFrame.getChatWindowManager();

        return chatWindowManager.getSelectedChat();
    }

    /**
     * Implements the <code>UIService.isContainerSupported</code> method.
     * Checks if the plugable container with the given Container is supported
     * by this implementation.
     *
     * @param containderID the id of the container that we're making the query
     * for.
     *
     * @return true if the container with the specified id is exported by the
     * implementation of the UI service and false otherwise.
     *
     * @see UIService#isContainerSupported(Container)
     */
    public boolean isContainerSupported(Container containderID)
    {
        return supportedContainers.contains(containderID);
    }

    /**
     * Implements the <code>UIService.getAccountRegWizardContainer</code>
     * method. Returns the current implementation of the
     * <tt>AccountRegistrationWizardContainer</tt>.
     *
     * @see UIService#getAccountRegWizardContainer()
     *
     * @return a reference to the currently valid instance of
     * <tt>AccountRegistrationWizardContainer</tt>.
     */
    public WizardContainer getAccountRegWizardContainer()
    {
        return this.wizardContainer;
    }

    /**
     * Returns an instance of <tt>AuthenticationWindow</tt> for the given
     * protocol provider, realm and user credentials.
     */
    public ExportedWindow getAuthenticationWindow(
        ProtocolProviderService protocolProvider,
        String realm,
        UserCredentials userCredentials,
        boolean isUserNameEditable)
    {
        return new AuthenticationWindow(mainFrame,
                                        protocolProvider,
                                        realm,
                                        userCredentials,
                                        isUserNameEditable);
    }

    /**
     * Returns a default implementation of the <tt>SecurityAuthority</tt>
     * interface that can be used by non-UI components that would like to launch
     * the registration process for a protocol provider. Initially this method
     * was meant for use by the systray bundle and the protocol URI handlers.
     *
     * @param protocolProvider the <tt>ProtocolProviderService</tt> for which
     * the authentication window is about.
     *
     * @return a default implementation of the <tt>SecurityAuthority</tt>
     * interface that can be used by non-UI components that would like to launch
     * the registration process for a protocol provider.
     */
    public SecurityAuthority getDefaultSecurityAuthority(
                    ProtocolProviderService protocolProvider)
    {
        return new DefaultSecurityAuthority(protocolProvider);
    }

    /**
     * Returns the LoginManager.
     * @return the LoginManager
     */
    public LoginManager getLoginManager()
    {
        return loginManager;
    }

    /**
     * Returns the <tt>MainFrame</tt>. This is the class defining the main
     * application window.
     *
     * @return the <tt>MainFrame</tt>
     */
    public MainFrame getMainFrame()
    {
        return mainFrame;
    }

    /**
     * The <tt>RunLogin</tt> implements the Runnable interface and is used to
     * shows the login windows in a seperate thread.
     */
    private class RunLoginGui implements Runnable {
        public void run() {
            loginManager.runLogin(mainFrame);
        }
    }

    /**
     * The <tt>RunApplication</tt> implements the Runnable interface and is used to
     * shows the main application window in a separate thread.
     */
    private class RunApplicationGui implements Runnable
    {
        public void run()
        {
            mainFrame.setVisible(true);
        }
    }

    /**
     * Sets the look&feel and the theme.
     */
    private void setDefaultThemePack()
    {
        // we need to set the UIDefaults class loader so that it may access
        // resources packed inside OSGI bundles
        UIManager.put("ClassLoader", getClass().getClassLoader());

        String osName = System.getProperty("os.name");

        if (!osName.startsWith("Mac"))
        {
            try
            {
                SIPCommLookAndFeel lf = new SIPCommLookAndFeel();
                SIPCommLookAndFeel.setCurrentTheme(new SIPCommDefaultTheme());

                // Check the isLookAndFeelDecorated property and set the appropriate
                // default decoration.
                boolean isDecorated
                    = new Boolean(GuiActivator.getResources().
                        getSettingsString("isLookAndFeelDecorated")).booleanValue();

                if (isDecorated)
                {
                    JFrame.setDefaultLookAndFeelDecorated(true);
                    JDialog.setDefaultLookAndFeelDecorated(true);
                }

                UIManager.setLookAndFeel(lf);
            }
            catch (UnsupportedLookAndFeelException e) {
                logger.error("The provided Look & Feel is not supported.", e);
            }
        }
    }

    /**
     * Notifies all plugin containers of a <tt>PluginComponent</tt>
     * registration.
     */
    public void serviceChanged(ServiceEvent event)
    {
        Object sService = GuiActivator.bundleContext.getService(
            event.getServiceReference());

        // we don't care if the source service is not a plugin component
        if (! (sService instanceof PluginComponent))
        {
            return;
        }

        PluginComponent pluginComponent = (PluginComponent) sService;

        if (event.getType() == ServiceEvent.REGISTERED)
        {
            logger
                .info("Handling registration of a new Plugin Component.");

            if(pluginComponent.getComponent() == null
                || !(pluginComponent.getComponent() instanceof Component))
            {
                logger.error("Plugin Component type is not supported." +
                            "Should provide a plugin in AWT, SWT or Swing.");
                logger.debug("Logging exception to show the calling plugin",
                            new Exception(""));
                return;
            }

            this.firePluginEvent(   pluginComponent,
                                    PluginComponentEvent.PLUGIN_COMPONENT_ADDED);
        }
        else if (event.getType() == ServiceEvent.UNREGISTERING)
        {
            this.firePluginEvent(   pluginComponent,
                                    PluginComponentEvent
                                        .PLUGIN_COMPONENT_REMOVED);
        }
    }

    /**
     * Returns the corresponding <tt>BorderLayout</tt> constraint from the given
     * <tt>Container</tt> constraint.
     *
     * @param containerConstraints constraints defined in the <tt>Container</tt>
     * @return the corresponding <tt>BorderLayout</tt> constraint from the given
     * <tt>Container</tt> constraint.
     */
    public static Object getBorderLayoutConstraintsFromContainer(
        Object containerConstraints)
    {
        Object layoutConstraint = null;
        if (containerConstraints == null)
            return null;

        if (containerConstraints.equals(Container.START))
            layoutConstraint = BorderLayout.LINE_START;
        else if (containerConstraints.equals(Container.END))
            layoutConstraint = BorderLayout.LINE_END;
        else if (containerConstraints.equals(Container.TOP))
            layoutConstraint = BorderLayout.NORTH;
        else if (containerConstraints.equals(Container.BOTTOM))
            layoutConstraint = BorderLayout.SOUTH;
        else if (containerConstraints.equals(Container.LEFT))
            layoutConstraint = BorderLayout.WEST;
        else if (containerConstraints.equals(Container.RIGHT))
            layoutConstraint = BorderLayout.EAST;

        return layoutConstraint;
    }

    private class DefaultPluginComponent implements PluginComponent
    {
        private Component component;

        private Container container;

        public DefaultPluginComponent(  Component component,
                                        Container container)
        {
            this.component = component;
            this.container = container;
        }

        public Object getComponent()
        {
            return component;
        }

        public String getConstraints()
        {
            return Container.END;
        }

        public Container getContainer()
        {
            return container;
        }

        public String getName()
        {
            return component.getName();
        }

        public void setCurrentContact(MetaContact metaContact)
        {
            if (component instanceof ContactAwareComponent)
                ((ContactAwareComponent) component)
                    .setCurrentContact(metaContact);
        }

        public void setCurrentContactGroup(MetaContactGroup metaGroup)
        {
            if (component instanceof ContactAwareComponent)
                ((ContactAwareComponent) component)
                    .setCurrentContactGroup(metaGroup);
        }

        public int getPositionIndex()
        {
            return -1;
        }

        public boolean isNativeComponent()
        {
            return false;
        }
    }

    public void propertyChange(PropertyChangeEvent evt)
    {
        if (evt.getPropertyName().equals(
            "net.java.sip.communicator.impl.gui.isTransparentWindowEnabled"))
        {
            String isTransparentString = (String) evt.getNewValue();

            boolean isTransparentWindowEnabled
                = new Boolean(isTransparentString).booleanValue();

            try
            {
                WindowUtils.setWindowTransparent(   mainFrame,
                    isTransparentWindowEnabled);
            }
            catch (UnsupportedOperationException ex)
            {
                logger.error(ex.getMessage(), ex);

                if (isTransparentWindowEnabled)
                {
                    new ErrorDialog(mainFrame,
                        Messages.getI18NString("error").getText(),
                        Messages.getI18NString("transparencyNotEnabled").getText())
                    .showDialog();
                }
                
                ConfigurationManager.setTransparentWindowEnabled(false);
            }
        }
        else if (evt.getPropertyName().equals(
            "net.java.sip.communicator.impl.gui.windowTransparency"))
        {
            mainFrame.repaint();
        }
    }
}
