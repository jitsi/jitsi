/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.beans.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.event.*;
import net.java.sip.communicator.impl.gui.lookandfeel.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.main.account.*;
import net.java.sip.communicator.impl.gui.main.chat.*;
import net.java.sip.communicator.impl.gui.main.chat.conference.*;
import net.java.sip.communicator.impl.gui.main.chat.history.*;
import net.java.sip.communicator.impl.gui.main.configforms.*;
import net.java.sip.communicator.impl.gui.main.contactlist.*;
import net.java.sip.communicator.impl.gui.main.login.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.Container;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.service.shutdown.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.swing.*;

import org.osgi.framework.*;

import com.sun.jna.examples.*;

/**
 * An implementation of the <tt>UIService</tt> that gives access to other
 * bundles to this particular swing ui implementation.
 *
 * @author Yana Stamcheva
 * @author Lubomir Marinov
 */
public class UIServiceImpl
    implements UIService,
               ShutdownService,
               ServiceListener,
               PropertyChangeListener
{
    private static final Logger logger = Logger.getLogger(UIServiceImpl.class);

    private PopupDialogImpl popupDialog;

    private AccountRegWizardContainerImpl wizardContainer;

    private final List<PluginComponentListener> pluginComponentListeners =
        new Vector<PluginComponentListener>();

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

    private ChatWindowManager chatWindowManager
        = new ChatWindowManager();

    private ConferenceChatManager conferenceChatManager
        = new ConferenceChatManager();

    private ConfigurationFrame configurationFrame;

    private HistoryWindowManager historyWindowManager
        = new HistoryWindowManager();

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

        if (UIManager.getLookAndFeel() instanceof SIPCommLookAndFeel)
            initCustomFonts();

        /*
         * The mainFrame isn't fully ready without the MetaContactListService so
         * make sure it's set before allowing anything, such as LoginManager, to
         * use the mainFrame. Otherwise, LoginManager, for example, will call
         * back from its event listener(s) into the mainFrame and cause a
         * NullPointerException.
         */
        mainFrame.setContactList(GuiActivator.getContactListService());

        this.mainFrame.initBounds();

        GuiActivator.getUIService().registerExportedWindow(mainFrame);

        this.loginManager = new LoginManager(mainFrame);

        this.popupDialog = new PopupDialogImpl();

        this.wizardContainer = new AccountRegWizardContainerImpl(mainFrame);

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

        KeyboardFocusManager focusManager =
            KeyboardFocusManager.getCurrentKeyboardFocusManager();
        focusManager.
            addKeyEventDispatcher(new KeyBindingsDispatching(focusManager));
    }

    /**
     * Implements <code>UISercie.getSupportedContainers</code>. Returns the
     * list of supported containers by this implementation .
     *
     * @see UIService#getSupportedContainers()
     * @return an Iterator over all supported containers.
     */
    public Iterator<Container> getSupportedContainers()
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
     * @param eventID one of the PLUGIN_COMPONENT_XXX static fields indicating
     *            the nature of the event.
     */
    private void firePluginEvent(PluginComponent pluginComponent, int eventID)
    {
        PluginComponentEvent evt =
            new PluginComponentEvent(pluginComponent, eventID);

        logger.debug("Will dispatch the following plugin component event: "
            + evt);

        synchronized (pluginComponentListeners)
        {
            for (PluginComponentListener l : pluginComponentListeners)
            {
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
                    break;
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
     * Brings the focus to the main application window.
     */
    public void bringToFront()
    {
        if (mainFrame.getState() == Frame.ICONIFIED) 
            mainFrame.setState(Frame.NORMAL);
        // Because toFront() method gives us no guarantee that our frame would
        // go on top we'll try to also first request the focus and set our
        // window always on top to put all the chances on our side.
        mainFrame.requestFocus();
        mainFrame.setAlwaysOnTop(true);
        mainFrame.toFront();
        mainFrame.setAlwaysOnTop(false);
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

        if (mainFrame != null)
            mainFrame
                .setDefaultCloseOperation(exitOnClose ? JFrame.DISPOSE_ON_CLOSE
                    : JFrame.HIDE_ON_CLOSE);
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
        registerExportedWindow(new AddContactDialog(mainFrame));
        registerExportedWindow(new AuthenticationExportedWindow(mainFrame));
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
    public Iterator<WindowID> getSupportedExportedWindows()
    {
        return Collections.unmodifiableMap(exportedWindows).keySet().iterator();
    }

    /**
     * Implements the <code>getExportedWindow</code> in the UIService interface.
     * Returns the window corresponding to the given <tt>WindowID</tt>.
     *
     * @param windowID the id of the window we'd like to retrieve.
     * @param params the params to be passed to the returned window.
     * @return a reference to the <tt>ExportedWindow</tt> instance corresponding
     *         to <tt>windowID</tt>.
     * @see UIService#getExportedWindow(WindowID)
     */
    public ExportedWindow getExportedWindow(WindowID windowID, Object[] params)
    {
        ExportedWindow win = exportedWindows.get(windowID);

        if (win != null)
            win.setParams(params);

        return win;
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
        return getExportedWindow(windowID, null);
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
     * Implements {@link UIService#getChat(Contact)}. If a chat for the given
     * contact exists already, returns it; otherwise, creates a new one.
     *
     * @param contact the contact that we'd like to retrieve a chat window for.
     * @return the <tt>Chat</tt> corresponding to the specified contact.
     * @see UIService#getChat(Contact)
     */
    public ChatPanel getChat(Contact contact)
    {
        MetaContact metaContact
            = GuiActivator.getContactListService()
                .findMetaContactByContact(contact);

        return chatWindowManager.getContactChat(metaContact, true);
    }

    /**
     * Returns the <tt>Chat</tt> corresponding to the given <tt>ChatRoom</tt>.
     *
     * @param chatRoom the <tt>ChatRoom</tt> for which the searched chat is
     * about.
     * @return the <tt>Chat</tt> corresponding to the given <tt>ChatRoom</tt>.
     */
    public ChatPanel getChat(ChatRoom chatRoom)
    {
        return chatWindowManager.getMultiChat(chatRoom, true);
    }

    /**
     * Returns the selected <tt>Chat</tt>.
     *
     * @return the selected <tt>Chat</tt>.
     */
    public ChatPanel getCurrentChat()
    {
        return chatWindowManager.getSelectedChat();
    }

    /**
     * Returns the phone number currently entered in the phone number field.
     *
     * @return the phone number currently entered in the phone number field.
     */
    public String getCurrentPhoneNumber()
    {
        return null;
    }

    /**
     * Changes the phone number currently entered in the phone number field.
     *
     * @param phoneNumber the phone number to enter in the phone number field.
     */
    public void setCurrentPhoneNumber(String phoneNumber)
    {

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
        return
            new AuthenticationExportedWindow(
                new AuthenticationWindow(mainFrame,
                                         protocolProvider,
                                         realm,
                                         userCredentials,
                                         isUserNameEditable));
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
     * Returns the chat conference manager.
     *
     * @return the chat conference manager.
     */
    public ConferenceChatManager getConferenceChatManager()
    {
        return conferenceChatManager;
    }

    /**
     * Returns the chat window manager.
     *
     * @return the chat window manager.
     */
    public ChatWindowManager getChatWindowManager()
    {
        return chatWindowManager;
    }

    /**
     * Returns the <tt>HistoryWindowManager</tt>.
     * @return the <tt>HistoryWindowManager</tt>
     */
    public HistoryWindowManager getHistoryWindowManager()
    {
        return historyWindowManager;
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
     * Sets the look&feel and the theme.
     */
    private void setDefaultThemePack()
    {
        // Show tooltips immediately and specify a custom background.
        ToolTipManager toolTipManager = ToolTipManager.sharedInstance();
        UIManager.put("ToolTip.background",
            new Color(GuiActivator.getResources()
                    .getColor("service.gui.TOOLTIP_BACKGROUND")));
        toolTipManager.setInitialDelay(500);
        toolTipManager.setDismissDelay(60000);
        toolTipManager.setEnabled(true);

        // we need to set the UIDefaults class loader so that it may access
        // resources packed inside OSGI bundles
        UIManager.put("ClassLoader", getClass().getClassLoader());

        /*
         * Attempt to use the OS-native LookAndFeel instead of
         * SIPCommLookAndFeel.
         */
        String laf = UIManager.getSystemLookAndFeelClassName();
        boolean lafIsSet = false;

        /*
         * SIPCommLookAndFeel used to be set in case the system L&F was the same
         * as the cross-platform L&F. Unfortunately, SIPCommLookAndFeel is now
         * broken because its classes are loaded by different ClassLoaders which
         * results in exceptions. That's why the check
         * !laf.equals(UIManager.getCrossPlatformLookAndFeelClassName()) is
         * removed from the if statement bellow and thus the cross-platform L&F
         * is preferred over SIPCommLookAndFeel.
         */
        if (laf != null)
        {
            try
            {
                UIManager.setLookAndFeel(laf);

                lafIsSet = true;

                UIDefaults uiDefaults = UIManager.getDefaults();
                if (OSUtils.IS_WINDOWS)
                    fixWindowsUIDefaults(uiDefaults);
                // Workaround for SC issue #516
                // "GNOME SCScrollPane has rounded and rectangular borders"
                if (laf.equals("com.sun.java.swing.plaf.gtk.GTKLookAndFeel")
                        || laf.equals("com.sun.java.swing.plaf.motif.MotifLookAndFeel"))
                {
                    uiDefaults.put(
                        "ScrollPaneUI",
                        new javax.swing.plaf.metal.MetalLookAndFeel()
                            .getDefaults().get("ScrollPaneUI"));
                }
            }
            catch (ClassNotFoundException ex)
            {
                /*
                 * Ignore the exceptions because we're only trying to set
                 * the native LookAndFeel and, if it fails, we'll use
                 * SIPCommLookAndFeel.
                 */
            }
            catch (InstantiationException ex)
            {
            }
            catch (IllegalAccessException ex)
            {
            }
            catch (UnsupportedLookAndFeelException ex)
            {
            }
        }

        if (!lafIsSet)
        {
            try
            {
                SIPCommLookAndFeel lf = new SIPCommLookAndFeel();
                SIPCommLookAndFeel
                    .setCurrentTheme(new SIPCommDefaultTheme());

                // Check the isLookAndFeelDecorated property and set the
                // appropriate
                // default decoration.
                boolean isDecorated =
                    Boolean.parseBoolean(GuiActivator.getResources()
                        .getSettingsString(
                                        "impl.gui.IS_LOOK_AND_FEEL_DECORATED"));

                if (isDecorated)
                {
                    JFrame.setDefaultLookAndFeelDecorated(true);
                    JDialog.setDefaultLookAndFeelDecorated(true);
                }

                UIManager.setLookAndFeel(lf);
            }
            catch (UnsupportedLookAndFeelException e)
            {
                logger.error("The provided Look & Feel is not supported.",
                    e);
            }
        }
    }

    private void fixWindowsUIDefaults(UIDefaults uiDefaults)
    {

        /*
         * Windows actually uses different fonts for the controls in windows and
         * the controls in dialogs. Unfortunately, win.defaultGUI.font may not
         * be the font Windows will use for controls in windows but the one to
         * be used for dialogs. And win.messagebox.font will be the font for
         * windows but the L&F will use it for OptionPane which in turn should
         * rather use the font for dialogs. So swap the meanings of the two to
         * get standard fonts in the windows while compromizing that dialogs may
         * appear in it as well (if the dialogs are created as non-OptionPanes
         * and in this case SIP Communicator will behave as Mozilla Firfox and
         * Eclipse with respect to using the window font for the dialogs).
         */
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Object menuFont = toolkit.getDesktopProperty("win.menu.font");
        Object messageboxFont
            = toolkit.getDesktopProperty("win.messagebox.font");
        if ((messageboxFont != null) && messageboxFont.equals(menuFont))
        {
            Object defaultGUIFont
                = toolkit.getDesktopProperty("win.defaultGUI.font");
            if ((defaultGUIFont != null)
                    && !defaultGUIFont.equals(messageboxFont))
            {
                Object messageFont = uiDefaults.get("OptionPane.font");
                Object controlFont = uiDefaults.get("Panel.font");
                if ((messageFont != null) && !messageFont.equals(controlFont))
                {
                    uiDefaults.put("OptionPane.font", controlFont);
                    uiDefaults.put("OptionPane.messageFont", controlFont);
                    uiDefaults.put("OptionPane.buttonFont", controlFont);

                    uiDefaults.put("Button.font", messageFont);
                    uiDefaults.put("CheckBox.font", messageFont);
                    uiDefaults.put("ComboBox.font", messageFont);
                    uiDefaults.put("EditorPane.font", messageFont);
                    uiDefaults.put("FormattedTextField.font", messageFont);
                    uiDefaults.put("Label.font", messageFont);
                    uiDefaults.put("List.font", messageFont);
                    uiDefaults.put("RadioButton.font", messageFont);
                    uiDefaults.put("Panel.font", messageFont);
                    uiDefaults.put("PasswordField.font", messageFont);
                    uiDefaults.put("ProgressBar.font", messageFont);
                    uiDefaults.put("ScrollPane.font", messageFont);
                    uiDefaults.put("Slider.font", messageFont);
                    uiDefaults.put("Spinner.font", messageFont);
                    uiDefaults.put("TabbedPane.font", messageFont);
                    uiDefaults.put("Table.font", messageFont);
                    uiDefaults.put("TableHeader.font", messageFont);
                    uiDefaults.put("TextField.font", messageFont);
                    uiDefaults.put("TextPane.font", messageFont);
                    uiDefaults.put("TitledBorder.font", messageFont);
                    uiDefaults.put("ToggleButton.font", messageFont);
                    uiDefaults.put("Tree.font", messageFont);
                    uiDefaults.put("Viewport.font", messageFont);
                }
            }
        }

        // Workaround for bug 6396936 (http://bugs.sun.com): WinL&F : font for
        // text area is incorrect.
        uiDefaults.put("TextArea.font", uiDefaults.get("TextField.font"));
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

        switch (event.getType())
        {
        case ServiceEvent.REGISTERED:
            logger.info("Handling registration of a new Plugin Component.");

            Object component = pluginComponent.getComponent();
            if (!(component instanceof Component))
            {
                logger.error("Plugin Component type is not supported."
                    + "Should provide a plugin in AWT, SWT or Swing.");
                logger.debug("Logging exception to show the calling plugin",
                    new Exception(""));
                return;
            }

            this.firePluginEvent(pluginComponent,
                PluginComponentEvent.PLUGIN_COMPONENT_ADDED);
            break;

        case ServiceEvent.UNREGISTERING:
            this.firePluginEvent(pluginComponent,
                PluginComponentEvent.PLUGIN_COMPONENT_REMOVED);
            break;
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

    public void propertyChange(PropertyChangeEvent evt)
    {
        String propertyName = evt.getPropertyName();

        if (propertyName.equals(
            "impl.gui.IS_TRANSPARENT_WINDOW_ENABLED"))
        {
            String isTransparentString = (String) evt.getNewValue();

            boolean isTransparentWindowEnabled
                = Boolean.parseBoolean(isTransparentString);

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
                    ResourceManagementService resources =
                        GuiActivator.getResources();

                    new ErrorDialog(mainFrame, resources
                        .getI18NString("service.gui.ERROR"), resources
                        .getI18NString("service.gui.TRANSPARENCY_NOT_ENABLED"))
                        .showDialog();
                }

                ConfigurationManager.setTransparentWindowEnabled(false);
            }
        }
        else if (propertyName.equals(
            "impl.gui.WINDOW_TRANSPARENCY"))
        {
            mainFrame.repaint();
        }
    }

    /**
     * Initialize main window font.
     */
    private void initCustomFonts()
    {
        JComponent layeredPane = mainFrame.getLayeredPane();

        ResourceManagementService resources = GuiActivator.getResources();
        String fontName = resources.getSettingsString("service.gui.FONT_NAME");
        String titleFontSize =
            resources.getSettingsString("service.gui.FONT_SIZE");

        Font font = new Font(   fontName,
                                Font.BOLD,
                                Integer.parseInt(titleFontSize));

        for (int i = 0; i < layeredPane.getComponentCount(); i++)
        {
            layeredPane.getComponent(i).setFont(font);
        }
    }

    /*
     * Implements UIService#useMacOSXScreenMenuBar(). Indicates that the Mac OS
     * X screen menu bar is to be used on Mac OS X and the Windows-like
     * per-window menu bars are to be used on non-Mac OS X operating systems.
     */
    public boolean useMacOSXScreenMenuBar()
    {
        return OSUtils.IS_MAC;
    }

    /*
     * Implements ShutdownService#beginShutdown(). Disposes of the mainFrame (if
     * it exists) and then instructs Felix to start shutting down the bundles so
     * that the application can gracefully quit.
     */
    public void beginShutdown()
    {
        try
        {
            if (mainFrame != null)
                mainFrame.dispose();
        }
        finally
        {
            try
            {
                GuiActivator.bundleContext.getBundle(0).stop();
            }
            catch (BundleException ex)
            {
                logger.error("Failed to being gentle shutdown of Felix.", ex);
                System.exit(0);
            }
        }
    }

    /*
     * Implements UIService#setConfigurationWindowVisible(boolean). Makes sure
     * there is only one ConfigurationFrame instance visible at one and the same
     * time.
     */
    public void setConfigurationWindowVisible(boolean visible)
    {
        if (configurationFrame == null)
            configurationFrame = new ConfigurationFrame(mainFrame);
        configurationFrame.setVisible(visible);
    }

    /**
     * Dispatcher which ensures that our custom keybindings will
     * be executed before any other focused(or not focused) component
     * will consume our key event. This way we override some components
     * keybindings.
     */
    private static class KeyBindingsDispatching
        implements KeyEventDispatcher
    {
        KeyboardFocusManager focusManager = null;

        KeyBindingsDispatching(KeyboardFocusManager focusManager)
        {
            this.focusManager = focusManager;
        }

        public boolean dispatchKeyEvent(KeyEvent e)
        {
            if(e.getID() == KeyEvent.KEY_PRESSED)
            {
                Window w = focusManager.getActiveWindow();
                JRootPane rpane = null;

                if(w instanceof JFrame)
                    rpane = ((JFrame)w).getRootPane();

                if(w instanceof JDialog)
                    rpane = ((JDialog)w).getRootPane();

                if(rpane == null)
                    return false;

                Object binding = rpane.
                    getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).
                        get(KeyStroke.getKeyStrokeForEvent(e));

                if(binding == null)
                    return false;

                Object actObj = rpane.getActionMap().get(binding);

                if(actObj != null && actObj instanceof UIAction)
                {
                    ((UIAction)actObj).actionPerformed(
                        new ActionEvent(w, -1, (String)binding));
                    return true;
                }
            }

            return false;
        }
    }

    /**
     * Returns a list containing all open Chats
     *
     * @return  A list of all open Chats.
     */
    public List<Chat> getChats()
    {
        return new ArrayList<Chat>(chatWindowManager.getChatPanels());
    }

    /**
     * Get the MetaContact corresponding to the chat.
     * The chat must correspond to a one on one conversation, otherwise this
     * method will return null.
     *
     * @param chat  The chat to get the MetaContact from
     * @return      The MetaContact corresponding to the chat or null in case
     *              it is a chat with more then one person.
     */
    public MetaContact getChatContact(Chat chat)
    {
        Object contact = ((ChatPanel) chat).getChatSession().getDescriptor();
        // If it is a one on one conversation this would be a MetaContact
        if (contact instanceof MetaContact)
            return (MetaContact) contact;
        // If not, we are talking to more then one person and we return null
        else
            return null;
    }

    /**
     * Adds the given <tt>WindowListener</tt> to the main application window.
     * @param l the <tt>WindowListener</tt> to add
     */
    public void addWindowListener(WindowListener l)
    {
        mainFrame.addWindowListener(l);
    }

    /**
     * Removes the given <tt>WindowListener</tt> from the main application
     * window.
     * @param l the <tt>WindowListener</tt> to remove
     */
    public void removeWindowListener(WindowListener l)
    {
        mainFrame.removeWindowListener(l);
    }
}