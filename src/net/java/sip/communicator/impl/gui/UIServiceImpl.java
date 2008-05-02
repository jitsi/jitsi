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

import net.java.sip.communicator.impl.gui.event.*;
import net.java.sip.communicator.impl.gui.lookandfeel.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.main.account.*;
import net.java.sip.communicator.impl.gui.main.chat.*;
import net.java.sip.communicator.impl.gui.main.chat.conference.*;
import net.java.sip.communicator.impl.gui.main.configforms.*;
import net.java.sip.communicator.impl.gui.main.contactlist.addcontact.*;
import net.java.sip.communicator.impl.gui.main.login.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.Container;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * An implementation of the <tt>UIService</tt> that gives access to other
 * bundles to this particular swing ui implementation.
 *
 * @author Yana Stamcheva
 */
public class UIServiceImpl
    implements  UIService,
                ServiceListener
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
        supportedContainers.add(UIService.CONTAINER_MAIN_TOOL_BAR);
        supportedContainers.add(UIService.CONTAINER_CONTACT_RIGHT_BUTTON_MENU);
        supportedContainers.add(UIService.CONTAINER_GROUP_RIGHT_BUTTON_MENU);
        supportedContainers.add(UIService.CONTAINER_TOOLS_MENU);
        supportedContainers.add(UIService.CONTAINER_HELP_MENU);
        supportedContainers.add(UIService.CONTAINER_CHAT_TOOL_BAR);
        supportedContainers.add(UIService.CONTAINER_CALL_HISTORY);
        supportedContainers.add(UIService.CONTAINER_MAIN_TABBED_PANE);
        supportedContainers.add(UIService.CONTAINER_CHAT_HELP_MENU);
        supportedContainers.add(UIService.CONTAINER_CHAT_WINDOW_SOUTH);
        supportedContainers.add(UIService.CONTAINER_CONTACT_LIST_EAST);
        supportedContainers.add(UIService.CONTAINER_CONTACT_LIST_WEST);
        supportedContainers.add(UIService.CONTAINER_CONTACT_LIST_SOUTH);
        supportedContainers.add(UIService.CONTAINER_CONTACT_LIST_NORTH);
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

        this.loginManager = new LoginManager(mainFrame);

        this.popupDialog = new PopupDialogImpl();

        this.wizardContainer = new AccountRegWizardContainerImpl(mainFrame);

        this.configurationFrame = new ConfigurationFrame(mainFrame);

        mainFrame.setContactList(GuiActivator.getMetaContactListService());

        if(ConfigurationManager.isApplicationVisible())
            SwingUtilities.invokeLater(new RunApplicationGui());

        SwingUtilities.invokeLater(new RunLoginGui());

        this.initExportedWindows();
    }

    /**
     * Implements removeComponent in UIService interface. Removes a plugin
     * component and fires a PluginComponentEvent to inform all interested
     * listeners that a plugin component has been removed.
     *
     * @param containerID the <tt>Container</tt> of the plugable container,
     * where the component is stored
     * @param component the component to remove
     *
     * @see UIService#removeComponent(Container, Object)
     *
     * @throws java.lang.IllegalArgumentException if no component exists for
     * the specified container id.
     */
    public void removeComponent(Container containerID, Object component)
        throws IllegalArgumentException
    {
        if (!supportedContainers.contains(containerID))
        {
            throw new IllegalArgumentException(
                "The constraint that you specified is not"
                    + " supported by this UIService implementation.");
        }
        else
        {
            if (registeredPlugins.containsKey(containerID))
            {
                ((Vector) registeredPlugins.get(containerID)).remove(component);
            }

            DefaultPluginComponent pluginComponent
                = new DefaultPluginComponent((Component) component, containerID);

            this.firePluginEvent(   pluginComponent,
                                    PluginComponentEvent
                                        .PLUGIN_COMPONENT_REMOVED);
        }
    }

    /**
     * Implements addComponent in UIService interface. Stores a plugin component
     * and fires a PluginComponentEvent to inform all interested listeners that
     * a plugin component has been added.
     *
     * @param containerID The <tt>Container</tt> of the plugable container.
     * @param component The component to add.
     *
     * @see UIService#addComponent(Container, Object)
     *
     * @throws java.lang.ClassCastException if <tt>component</tt> is not an
     * instance of a java.awt.Component
     * @throws java.lang.IllegalArgumentException if no component exists for
     * the specified container id.
     */
    public void addComponent(Container containerID, Object component)
        throws ClassCastException, IllegalArgumentException
    {
        if (!supportedContainers.contains(containerID))
        {
            throw new IllegalArgumentException(
                "The constraint that you specified is not"
                    + " supported by this UIService implementation.");
        }
        else if (!(component instanceof Component))
        {
            throw new ClassCastException(
                "The specified plugin is not a valid swing or awt component.");
        }
        else
        {
            if (registeredPlugins.containsKey(containerID))
            {
                ((Vector) registeredPlugins
                        .get(containerID)).add(component);
            }
            else
            {
                Vector plugins = new Vector();
                plugins.add(component);
                registeredPlugins.put(containerID, plugins);
            }

            DefaultPluginComponent pluginComponent
                = new DefaultPluginComponent((Component) component, containerID);

            this.firePluginEvent(   pluginComponent,
                                    PluginComponentEvent.PLUGIN_COMPONENT_ADDED);
        }
    }

    /**
     * Implements <code>UIService.addComponent(Container, String, Object)
     * </code>.
     * For now this method only invokes addComponent(containerID, component).
     *
     * @param containerID The <tt>Container</tt> of the plugable container.
     * @param constraint a constraint indicating how the component should be
     * added to the container.
     * @param component the component we are adding.
     *
     * @see UIService#addComponent(Container, String, Object)
     * @throws java.lang.ClassCastException if <tt>component</tt> is not an
     * instance of a java.awt.Component
     * @throws java.lang.IllegalArgumentException if no component exists for
     * the specified container id.
     */
    public void addComponent(Container containerID, String constraint,
        Object component) throws ClassCastException, IllegalArgumentException
    {
        this.addComponent(containerID, component);
    }

    /**
     * Implements <code>UIService.addComponent(Container, String, Object)
     * </code>.
     * For now this method only invokes addComponent(containerID, component).
     *
     * @param containerID The <tt>Container</tt> of the plugable container.
     * @param component the component we are adding.
     *
     * @throws java.lang.ClassCastException if <tt>component</tt> is not an
     * instance of a java.awt.Component
     * @throws java.lang.IllegalArgumentException if no component exists for
     * the specified container id.
     */
    public void addComponent(Container containerID,
        ContactAwareComponent component) throws ClassCastException,
        IllegalArgumentException
    {
        if (!(component instanceof Component))
        {

            throw new ClassCastException(
                "The specified plugin is not a valid swing or awt component.");
        }

        this.addComponent(containerID, (Component) component);
    }

    /**
     * Implements <code>UIService.addComponent(Container, String, Object)
     * </code>.
     * For now this method only invokes addComponent(containerID, component).
     *
     * @param containerID The <tt>Container</tt> of the plugable container.
     * @param constraint a constraint indicating how the component should be
     * added to the container.
     * @param component the component we are adding.
     *
     * @throws java.lang.ClassCastException if <tt>component</tt> is not an
     * instance of a java.awt.Component
     * @throws java.lang.IllegalArgumentException if no component exists for
     * the specified container id.
     */
    public void addComponent(Container containerID, String constraint,
        ContactAwareComponent component) throws ClassCastException,
        IllegalArgumentException
    {
        this.addComponent(containerID, constraint, component);
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
     * Implements getComponentsForContainer in UIService interface.
     *
     * @param containerID the id of the container whose components we'll be
     * retrieving.
     * @see UIService#getComponentsForContainer(Container)
     *
     * @return an iterator over all components added in the container with ID
     * <tt>containerID</tt>
     *
     * @throws java.lang.IllegalArgumentException if containerID does not
     * correspond to a container used in this implementation.
     */
    public Iterator getComponentsForContainer(Container containerID)
        throws IllegalArgumentException
    {

        if (!supportedContainers.contains(containerID))
            throw new IllegalArgumentException(
                "The container that you specified is not "
                    + "supported by this UIService implementation.");

        Vector plugins = new Vector();

        Object o = registeredPlugins.get(containerID);

        if (o != null)
        {
            plugins = (Vector) o;
        }

        return plugins.iterator();
    }

    /**
     * Not yet implemented.
     *
     * @param containerID the ID of the container whose constraints we'll be
     * retrieving.
     *
     * @see UIService#getConstraintsForContainer(Container)
     *
     * @return Iterator an <tt>Iterator</tt> for all constraintes supported by
     * the container corresponding to containerID.
     */
    public Iterator getConstraintsForContainer(Container containerID)
    {
        return null;
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
        if (mainFrame.isVisible())
        {
            if (mainFrame.getExtendedState() == JFrame.ICONIFIED)
                return false;
            else
                return true;
        }
        else
            return false;
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
                mainFrame.setVisible(isVisible);

                if(isVisible)
                    mainFrame.toFront();
            }
        });
    }

    /**
     * Implements <code>minimize</code> in the UIService interface. Minimizes
     * the main application window.
     *
     * @see UIService#minimize()
     */
    public void minimize()
    {
        this.mainFrame.setExtendedState(JFrame.ICONIFIED);
    }

    /**
     * Implements <code>maximize</code> in the UIService interface. Maximizes
     * the main application window.
     *
     * @see UIService#maximize()
     */
    public void maximize()
    {
        this.mainFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
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
        AddContactWizard addContactWizard = new AddContactWizard(mainFrame);

        exportedWindows.put(configurationFrame.getIdentifier(),
                configurationFrame);
        exportedWindows.put(addContactWizard.getIdentifier(), addContactWizard);
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
    public AccountRegistrationWizardContainer getAccountRegWizardContainer()
    {
        return this.wizardContainer;
    }

    /**
     * Implements the <code>UIService.getConfigurationWindow</code>. Returns
     * the current implementation of the <tt>ConfigurationWindow</tt>
     * interface.
     *
     * @see UIService#getConfigurationWindow()
     *
     * @return a reference to the currently valid instance of
     * <tt>ConfigurationWindow</tt>.
     */
    public ConfigurationWindow getConfigurationWindow()
    {
        return this.configurationFrame;
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
        SIPCommLookAndFeel lf = new SIPCommLookAndFeel();
        SIPCommLookAndFeel.setCurrentTheme(new SIPCommDefaultTheme());

        // Check the isLookAndFeelDecorated property and set the appropriate
        // default decoration.
        boolean isDecorated
            = new Boolean(ApplicationProperties
                .getProperty("isLookAndFeelDecorated")).booleanValue();

        if (isDecorated)
        {
            JFrame.setDefaultLookAndFeelDecorated(true);
            JDialog.setDefaultLookAndFeelDecorated(true);
        }

        // we need to set the UIDefaults class loader so that it may access
        // resources packed inside OSGI bundles
        UIManager.put("ClassLoader", getClass().getClassLoader());
        try {
            UIManager.setLookAndFeel(lf);
        } catch (UnsupportedLookAndFeelException e) {
            logger.error("The provided Look & Feel is not supported.", e);
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
    }
}
