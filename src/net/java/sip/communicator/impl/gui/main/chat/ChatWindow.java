/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chat;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.event.*;
import net.java.sip.communicator.impl.gui.main.chat.menus.*;
import net.java.sip.communicator.impl.gui.main.chat.toolBars.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.Container;
import net.java.sip.communicator.service.keybindings.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.skin.*;
import net.java.sip.communicator.util.swing.*;

import org.osgi.framework.*;

import com.explodingpixels.macwidgets.*;

/**
 * The chat window is the place, where users can write and send messages, view
 * received messages. The ChatWindow supports two modes of use: "Group all
 * messages in one window" and "Open messages in new window". In the first case
 * a <tt>JTabbedPane</tt> is added in the window, where each tab contains a
 * <tt>ChatPanel</tt>. In the second case the <tt>ChatPanel</tt> is added
 * like a "write message area", "send" button, etc. It corresponds to a
 * <tt>MetaContact</tt> or to a conference.
 * <p>
 * Note that the conference case is not yet implemented.
 *
 * @author Yana Stamcheva
 * @author Lubomir Marinov
 * @author Adam Netocny
 */
public class ChatWindow
    extends SIPCommFrame
    implements  ChatContainer,
                ExportedWindow,
                PluginComponentListener,
                WindowFocusListener
{
    private final Logger logger = Logger.getLogger(ChatWindow.class);

    private ConversationTabbedPane chatTabbedPane = null;

    private int chatCount = 0;

    private final java.util.List<ChatChangeListener> chatChangeListeners =
        new Vector<ChatChangeListener>();

    private final JPanel mainPanel
        = new TransparentPanel(new BorderLayout());

    private final JPanel statusBarPanel
        = new TransparentPanel(new BorderLayout());

    private final JPanel pluginPanelSouth = new JPanel();
    private final JPanel pluginPanelWest = new JPanel();
    private final JPanel pluginPanelEast = new JPanel();

    private final ContactPhotoPanel contactPhotoPanel = new ContactPhotoPanel();

    private final MessageWindowMenuBar menuBar;

    private MainToolBar mainToolBar;

    private final Component toolbarPanel;

    /**
     * Creates an instance of <tt>ChatWindow</tt> by passing to it an instance
     * of the main application window.
     */
    public ChatWindow()
    {
        if (!ConfigurationManager.isWindowDecorated())
        {
            this.setUndecorated(true);
        }

        this.addWindowFocusListener(this);

        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        //If in mode TABBED_CHAT_WINDOW initialize the tabbed pane
        if(ConfigurationManager.isMultiChatWindowEnabled())
        {
            chatTabbedPane = new ConversationTabbedPane();
        }

        menuBar = new MessageWindowMenuBar(this);

        this.setJMenuBar(menuBar);

        toolbarPanel = createToolBar();

        this.getContentPane().add(toolbarPanel, BorderLayout.NORTH);
        this.getContentPane().add(mainPanel, BorderLayout.CENTER);
        this.getContentPane().add(statusBarPanel, BorderLayout.SOUTH);

        this.initPluginComponents();

        this.setKeybindingInput(KeybindingSet.Category.CHAT);
        this.addKeybindingAction(   "chat-nextTab",
                                    new ForwardTabAction());
        this.addKeybindingAction(   "chat-previousTab",
                                    new BackwordTabAction());
        this.addKeybindingAction(   "chat-copy",
                                    new CopyAction());
        this.addKeybindingAction(   "chat-paste",
                                    new PasteAction());
        this.addKeybindingAction(   "chat-openSmileys",
                                    new OpenSmileyAction());
        this.addKeybindingAction(   "chat-openHistory",
                                    new OpenHistoryAction());
        this.addKeybindingAction(   "chat-close",
                                    new CloseAction());

        this.addWindowListener(new ChatWindowAdapter());

        int width = GuiActivator.getResources()
            .getSettingsInt("impl.gui.CHAT_WINDOW_WIDTH");
        int height = GuiActivator.getResources()
            .getSettingsInt("impl.gui.CHAT_WINDOW_HEIGHT");

        this.setSize(width, height);
    }

    /**
     * Shows or hides the Toolbar depending on the value of parameter b.
     *
     * @param b if true, makes the Toolbar visible, otherwise hides the Toolbar
     */
    public void setToolbarVisible(boolean b)
    {
        // The north panel is the one containing the toolbar and contact photo.
        toolbarPanel.setVisible(b);
    }

    /**
     * @see SIPCommFrame#dispose()
     */
    public void dispose()
    {
        try
        {
            GuiActivator.getUIService().removePluginComponentListener(this);
            mainToolBar.dispose();
            menuBar.dispose();
        }
        finally
        {
            super.dispose();
        }
    }

    /**
     * Returns the main toolbar in this chat window.
     * @return the main toolbar in this chat window
     */
    public MainToolBar getMainToolBar()
    {
        return mainToolBar;
    }

    /**
     * Adds a given <tt>ChatPanel</tt> to this chat window.
     *
     * @param chatPanel The <tt>ChatPanel</tt> to add.
     */
    public void addChat(final ChatPanel chatPanel)
    {
        if (ConfigurationManager.isMultiChatWindowEnabled())
            addChatTab(chatPanel);
        else
            addSimpleChat(chatPanel);

        chatCount ++;

        chatPanel.setShown(true);

        new Thread()
        {
            public void run()
            {
                for (ChatChangeListener l : chatChangeListeners)
                {
                    l.chatChanged(chatPanel);
                }
            }
        }.start();
    }

    /**
     * Creates the toolbar panel for this chat window, depending on the current
     * operating system.
     *
     * @return the created toolbar
     */
    private Component createToolBar()
    {
        Component toolbarPanel = null;

        mainToolBar = new MainToolBar(this);

        boolean chatToolbarVisible
            = ConfigurationManager.isChatToolbarVisible();

        if (OSUtils.IS_MAC)
        {
            UnifiedToolBar macToolbarPanel = new UnifiedToolBar();

            MacUtils.makeWindowLeopardStyle(getRootPane());

            macToolbarPanel.addComponentToLeft(mainToolBar);
            macToolbarPanel.addComponentToRight(contactPhotoPanel);
            macToolbarPanel.disableBackgroundPainter();
            macToolbarPanel.installWindowDraggerOnWindow(this);
            macToolbarPanel.getComponent()
                .setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
            macToolbarPanel.getComponent().setVisible(chatToolbarVisible);

            toolbarPanel = macToolbarPanel.getComponent();
        }
        else
        {
            ToolbarPanel panel = new ToolbarPanel(new BorderLayout());

            panel.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));
            panel.add(mainToolBar, BorderLayout.CENTER);
            panel.add(contactPhotoPanel, BorderLayout.EAST);
            panel.setVisible(chatToolbarVisible);

            toolbarPanel = panel;
        }

        return toolbarPanel;
    }

    /**
     * Adds a given <tt>ChatPanel</tt> to this chat window.
     *
     * @param chatPanel The <tt>ChatPanel</tt> to add.
     */
    private void addSimpleChat(ChatPanel chatPanel)
    {
        this.mainPanel.add(chatPanel, BorderLayout.CENTER);
    }

    /**
     * Adds a given <tt>ChatPanel</tt> to the <tt>JTabbedPane</tt> of this
     * chat window.
     *
     * @param chatPanel The <tt>ChatPanel</tt> to add.
     */
    private void addChatTab(ChatPanel chatPanel)
    {
        ChatSession chatSession = chatPanel.getChatSession();
        String chatName = chatSession.getChatName();

        ChatPanel currentChatPanel = getCurrentChat();

        if (currentChatPanel == null)
        {
            this.mainPanel.add(chatPanel, BorderLayout.CENTER);
        }
        else if (getChatTabCount() == 0)
        {
            ChatSession firstChatSession = currentChatPanel.getChatSession();

            // Add first two tabs to the tabbed pane.
            chatTabbedPane.addTab(  firstChatSession.getChatName(),
                                    firstChatSession.getChatStatusIcon(),
                                    currentChatPanel);

            chatTabbedPane.addTab(  chatName,
                                    chatSession.getChatStatusIcon(),
                                    chatPanel);

            // When added to the tabbed pane, the first chat panel should
            // rest the selected component.
            chatTabbedPane.setSelectedComponent(currentChatPanel);

            //add the chatTabbedPane to the window
            this.mainPanel.add(chatTabbedPane, BorderLayout.CENTER);
            this.mainPanel.validate();
        }
        else
        {
            // The tabbed pane already contains tabs.

            chatTabbedPane.addTab(
                chatName,
                chatSession.getChatStatusIcon(),
                chatPanel);

            chatTabbedPane.getParent().validate();
        }
    }

    /**
     * Removes a given <tt>ChatPanel</tt> from this chat window.
     *
     * @param chatPanel The <tt>ChatPanel</tt> to remove.
     */
    public void removeChat(ChatPanel chatPanel)
    {
        if (logger.isDebugEnabled())
            logger.debug("Removes chat for contact: "
                + chatPanel.getChatSession().getChatName());

        //if there's no tabs remove the chat panel directly from the content
        //pane.
        if(getChatTabCount() == 0)
        {
            this.mainPanel.remove(chatPanel);

            chatCount --;
        }

        //in the case of a tabbed chat window
        int index = chatTabbedPane.indexOfComponent(chatPanel);

        if (index != -1)
        {
            if (chatTabbedPane.getTabCount() > 1)
                chatTabbedPane.removeTabAt(index);

            if (chatTabbedPane.getTabCount() == 1)
            {
                ChatPanel currentChatPanel = getCurrentChat();

                this.chatTabbedPane.removeAll();

                this.mainPanel.remove(chatTabbedPane);

                this.mainPanel.add(currentChatPanel, BorderLayout.CENTER);

                this.setCurrentChat(currentChatPanel);

                // The current chat is now the focused chat, so we remove the
                // non read chat state.
                GuiActivator.getUIService().getChatWindowManager()
                        .removeNonReadChatState(currentChatPanel);
            }

            chatCount --;
        }

        if (getChatCount() == 0)
            dispose();
    }

    /**
     * Removes all tabs in the chat tabbed pane. If not in mode
     * TABBED_CHAT_WINDOW does nothing.
     */
    public void removeAllChats()
    {
        if (logger.isDebugEnabled())
            logger.debug("Remove all tabs from the chat window.");

        if(getChatTabCount() > 0)
        {
            this.chatTabbedPane.removeAll();

            this.mainPanel.remove(chatTabbedPane);

            chatCount = 0;
        }
        else
        {
            this.removeChat(getCurrentChat());
        }

        /*
         * The ChatWindow should seize to exist so we don't want any strong
         * references to it i.e. it cannot be exported anymore.
         */
        GuiActivator.getUIService().unregisterExportedWindow(this);
        dispose();
    }

    /**
     * Selects the chat tab which corresponds to the given <tt>MetaContact</tt>.
     *
     * @param chatPanel The <tt>ChatPanel</tt> to select.
     */
    public void setCurrentChat(final ChatPanel chatPanel)
    {
        ChatSession chatSession = chatPanel.getChatSession();

        if (logger.isDebugEnabled())
            logger.debug(
                "Set current chat panel to: " + chatSession.getChatName());

        if(getChatTabCount() > 0)
            this.chatTabbedPane.setSelectedComponent(chatPanel);

        this.setTitle(chatSession.getChatName());
        this.setChatContactPhoto(chatSession);

        this.mainToolBar.getSmileysBox().setChat(chatPanel);

        chatPanel.requestFocusInWriteArea();

        new Thread()
        {
            public void run()
            {
                for (ChatChangeListener l : chatChangeListeners)
                {
                    l.chatChanged(chatPanel);
                }
            }
        }.start();
    }

    /**
     * Selects the tab given by the index. If there's no tabbed pane, does
     * nothing.
     *
     * @param index the index to select
     */
    public void setCurrentChatTab(int index)
    {
        if(getChatTabCount() > 0)
        {
            ChatPanel chatPanel = (ChatPanel) this.chatTabbedPane
                .getComponentAt(index);

            setCurrentChat(chatPanel);
        }
    }

    /**
     * Returns the currently selected chat panel.
     *
     * @return the currently selected chat panel.
     */
    public ChatPanel getCurrentChat()
    {
        if(getChatTabCount() > 0)
            return (ChatPanel) chatTabbedPane.getSelectedComponent();
        else
        {
            int componentCount = mainPanel.getComponentCount();

            for (int i = 0; i < componentCount; i ++)
            {
                Component c = mainPanel.getComponent(i);

                if(c instanceof ChatPanel)
                {
                    return (ChatPanel) c;
                }
            }
        }
        return null;
    }

    /**
     * Returns the currently available chat panels.
     *
     * @return the currently available chat panels.
     */
    public java.util.List<ChatPanel> getChats()
    {
        ArrayList<ChatPanel> chatPanels = new ArrayList<ChatPanel>();

        if(getChatTabCount() > 0)
        {
            int componentCount = chatTabbedPane.getComponentCount();

            for (int i = 0; i < componentCount; i ++)
            {
                Component c = chatTabbedPane.getComponent(i);

                if(c instanceof ChatPanel)
                {
                    chatPanels.add((ChatPanel)c);
                }
            }
        }
        else
        {
            int componentCount = mainPanel.getComponentCount();

            for (int i = 0; i < componentCount; i ++)
            {
                Component c = mainPanel.getComponent(i);

                if(c instanceof ChatPanel)
                {
                    chatPanels.add((ChatPanel)c);
                }
            }
        }

        return chatPanels;
    }

    /**
     * Returns the tab count of the chat tabbed pane. Meant to be used when in
     * "Group chat windows" mode.
     *
     * @return int The number of opened tabs.
     */
    private int getChatTabCount()
    {
        return (chatTabbedPane == null) ? 0 : chatTabbedPane.getTabCount();
    }

    /**
     * Highlights the corresponding tab for the given chat panel.
     *
     * @param chatPanel the chat panel which corresponds to the tab to highlight
     */
    private void highlightTab(ChatPanel chatPanel)
    {
        int tabIndex = chatTabbedPane.indexOfComponent(chatPanel);

        chatPanel.unreadMessageNumber ++;

        chatTabbedPane.highlightTab(tabIndex, chatPanel.unreadMessageNumber);
    }

    /**
     * Sets the given icon to the tab opened for the given chat panel.
     *
     * @param chatPanel the chat panel, which corresponds the tab
     * @param icon the icon to be set
     */
    public void setTabIcon(ChatPanel chatPanel, Icon icon)
    {
        int index = this.chatTabbedPane.indexOfComponent(chatPanel);
        this.chatTabbedPane.setIconAt(index, icon);
    }

    /**
     * Sets the given title to the tab opened for the given chat panel.
     * @param chatPanel the chat panel
     * @param title the new title of the tab
     */
    public void setTabTitle(ChatPanel chatPanel, String title)
    {
        int index = this.chatTabbedPane.indexOfComponent(chatPanel);

        if(index > -1)
            this.chatTabbedPane.setTitleAt(index, title);
    }

    /**
     * The <tt>ForwardTabAction</tt> is an <tt>AbstractAction</tt> that
     * changes the currently selected tab with the next one. Each time when the
     * last tab index is reached the first one is selected.
     */
    private class ForwardTabAction
        extends UIAction
    {
        public void actionPerformed(ActionEvent e)
        {
            if (getChatTabCount() > 0) {
                int selectedIndex = chatTabbedPane.getSelectedIndex();
                if (selectedIndex < chatTabbedPane.getTabCount() - 1) {
                    setCurrentChatTab(selectedIndex + 1);
                }
                else {
                    setCurrentChatTab(0);
                }
            }
        }
    }

    /**
     * The <tt>BackwordTabAction</tt> is an <tt>AbstractAction</tt> that
     * changes the currently selected tab with the previous one. Each time when
     * the first tab index is reached the last one is selected.
     */
    private class BackwordTabAction
        extends UIAction
    {
        public void actionPerformed(ActionEvent e)
        {
            if (getChatTabCount() > 0) {
                int selectedIndex = chatTabbedPane.getSelectedIndex();
                if (selectedIndex != 0) {
                    setCurrentChatTab(selectedIndex - 1);
                }
                else {
                    setCurrentChatTab(chatTabbedPane.getTabCount() - 1);
                }
            }
        }
    }

    /**
     * The <tt>CopyAction</tt> is an <tt>AbstractAction</tt> that copies the
     * text currently selected.
     */
    private class CopyAction
        extends UIAction
    {
        public void actionPerformed(ActionEvent e)
        {
            getCurrentChat().copy();
        }
    }

    /**
     * The <tt>PasteAction</tt> is an <tt>AbstractAction</tt> that pastes
     * the text contained in the clipboard in the current <tt>ChatPanel</tt>.
     */
    private class PasteAction
        extends UIAction
    {
        public void actionPerformed(ActionEvent e)
        {
            getCurrentChat().paste();
        }
    }

    /**
     * The <tt>OpenSmileyAction</tt> is an <tt>AbstractAction</tt> that
     * opens the menu, containing all available smileys' icons.
     */
    private class OpenSmileyAction
        extends UIAction
    {
        public void actionPerformed(ActionEvent e)
        {
            mainToolBar.getSmileysBox().open();
        }
    }

    /**
     * The <tt>OpenHistoryAction</tt> is an <tt>AbstractAction</tt> that
     * opens the history window for the currently selected contact.
     */
    private class OpenHistoryAction
        extends UIAction
    {
        public void actionPerformed(ActionEvent e)
        {
            mainToolBar.getHistoryButton().doClick();
        }
    }


    /**
     * The <tt>CloseAction</tt> is an <tt>AbstractAction</tt> that
     * closes a tab in the chat window.
     */
    private class CloseAction
        extends UIAction
    {
        public void actionPerformed(ActionEvent e)
        {
            close(true);
        }
    }

    /**
     * Before closing the chat window saves the current size and position
     * through the <tt>ConfigurationService</tt>.
     */
    public class ChatWindowAdapter
        extends WindowAdapter
    {
        public void windowDeiconified(WindowEvent e)
        {
            String title = getTitle();

            if (title.startsWith("*")) {
                setTitle(title.substring(1, title.length()));
            }
        }
    }

    /**
     * Implements the <tt>SIPCommFrame</tt> close method. We check for an open
     * menu and if there's one we close it, otherwise we close the current chat.
     * @param isEscaped indicates if this window was closed by pressing the esc
     * button
     */
    protected void close(boolean isEscaped)
    {
        if(isEscaped)
        {
            ChatPanel chatPanel = getCurrentChat();

            ChatRightButtonMenu chatRightMenu
                = chatPanel.getChatConversationPanel().getRightButtonMenu();

            ChatWritePanel chatWritePanel = chatPanel.getChatWritePanel();
            WritePanelRightButtonMenu writePanelRightMenu
                = chatWritePanel.getRightButtonMenu();

            if (chatRightMenu.isVisible())
            {
                chatRightMenu.setVisible(false);
            }
            else if (writePanelRightMenu.isVisible())
            {
                writePanelRightMenu.setVisible(false);
            }
            else if ((menuBar.getSelectedMenu() != null)
                        || mainToolBar.getSmileysBox()
                            .getPopupMenu().isVisible())
            {
                MenuSelectionManager.defaultManager().clearSelectedPath();
            }
            else
            {
                GuiActivator
                    .getUIService().getChatWindowManager().closeChat(chatPanel);
            }
        }
        else
        {
            GuiActivator
                .getUIService().getChatWindowManager().closeAllChats(this, true);
        }
    }

    /**
     * Implements the <tt>ExportedWindow.getIdentifier()</tt> method.
     * @return the identifier of this window, used as plugin container
     * identifier.
     */
    public WindowID getIdentifier()
    {
        return ExportedWindow.CHAT_WINDOW;
    }

    /**
     * Implements the <tt>ExportedWindow.minimize()</tt> method. Minimizes this
     * window.
     */
    public void minimize()
    {
        this.setExtendedState(JFrame.ICONIFIED);
    }

    /**
     * Implements the <tt>ExportedWindow.maximize()</tt> method. Maximizes this
     * window.
     */
    public void maximize()
    {
        this.setExtendedState(JFrame.MAXIMIZED_BOTH);
    }

    /**
     * Implements the <tt>ExportedWindow.bringToFront()</tt> method. Brings
     * this window to front.
     */
    public void bringToFront()
    {
        if(getExtendedState() == JFrame.ICONIFIED)
            setExtendedState(JFrame.NORMAL);

        this.toFront();
    }

    /**
     * Initialize plugin components already registered for this container.
     */
    private void initPluginComponents()
    {
        pluginPanelEast.setLayout(
            new BoxLayout(pluginPanelEast, BoxLayout.Y_AXIS));
        pluginPanelSouth.setLayout(
            new BoxLayout(pluginPanelSouth, BoxLayout.Y_AXIS));
        pluginPanelWest.setLayout(
            new BoxLayout(pluginPanelWest, BoxLayout.Y_AXIS));

        this.getContentPane().add(pluginPanelEast, BorderLayout.EAST);
        this.getContentPane().add(pluginPanelWest, BorderLayout.WEST);
        this.mainPanel.add(pluginPanelSouth, BorderLayout.SOUTH);

        // Search for plugin components registered through the OSGI bundle
        // context.
        ServiceReference[] serRefs = null;

        String osgiFilter = "(|("
            + Container.CONTAINER_ID
            + "="+Container.CONTAINER_CHAT_WINDOW.getID()+")"
            + "(" + Container.CONTAINER_ID
            + "="+Container.CONTAINER_CHAT_STATUS_BAR.getID()+"))";

        try
        {
            serRefs = GuiActivator.bundleContext.getServiceReferences(
                PluginComponent.class.getName(),
                osgiFilter);
        }
        catch (InvalidSyntaxException exc)
        {
            logger.error("Could not obtain plugin component reference.", exc);
        }

        if (serRefs != null)
        {
            for (ServiceReference serRef : serRefs)
            {
                PluginComponent c
                    = (PluginComponent)
                        GuiActivator .bundleContext.getService(serRef);

                Object borderLayoutConstraint = UIServiceImpl
                    .getBorderLayoutConstraintsFromContainer(c.getConstraints());

                this.addPluginComponent((Component)c.getComponent(),
                                        c.getContainer(),
                                        borderLayoutConstraint);
            }
        }

        GuiActivator.getUIService().addPluginComponentListener(this);
    }

    /**
     * Adds a plugin component to this container.
     * @param event the <tt>PluginComponentEvent</tt> that notified us of the
     * add
     */
    public void pluginComponentAdded(PluginComponentEvent event)
    {
        PluginComponent c = event.getPluginComponent();

        if (c.getContainer().equals(Container.CONTAINER_CHAT_WINDOW)
            || c.getContainer().equals(Container.CONTAINER_CHAT_STATUS_BAR))
        {
            Object borderLayoutConstraints = UIServiceImpl
                .getBorderLayoutConstraintsFromContainer(c.getConstraints());

            this.addPluginComponent((Component) c.getComponent(),
                                    c.getContainer(),
                                    borderLayoutConstraints);
        }
    }

    /**
     * Removes a plugin component from this container.
     * @param event the <tt>PluginComponentEvent</tt> that notified us of the
     * remove
     */
    public void pluginComponentRemoved(PluginComponentEvent event)
    {
        PluginComponent c = event.getPluginComponent();

        if (c.getContainer().equals(Container.CONTAINER_CHAT_WINDOW)
            || c.getContainer().equals(Container.CONTAINER_CHAT_STATUS_BAR))
        {
            Object borderLayoutConstraint = UIServiceImpl
                .getBorderLayoutConstraintsFromContainer(c.getConstraints());

            this.removePluginComponent( (Component) c.getComponent(),
                                        c.getContainer(),
                                        borderLayoutConstraint);

            this.pack();
        }
    }

    /**
     * The source of the window
     * @return the source of the window
     */
    public Object getSource()
    {
        return this;
    }

    /**
     * Returns the number of all open chats.
     *
     * @return the number of all open chats
     */
    public int getChatCount()
    {
        return chatCount;
    }

    /**
     * Adds the given <tt>ChatChangeListener</tt>.
     * @param listener the listener to add
     */
    public void addChatChangeListener(ChatChangeListener listener)
    {
        synchronized (chatChangeListeners)
        {
            if (!chatChangeListeners.contains(listener))
                chatChangeListeners.add(listener);
        }
    }

    /**
     * Removes the given <tt>ChatChangeListener</tt>.
     * @param listener the listener to remove
     */
    public void removeChatChangeListener(ChatChangeListener listener)
    {
        synchronized (chatChangeListeners)
        {
            chatChangeListeners.remove(listener);
        }
    }

    /**
     * Adds the given component with to the container corresponding to the
     * given constraints.
     *
     * @param c the component to add
     * @param container the plugin container
     * @param constraints the constraints determining the container
     */
    private void addPluginComponent(Component c,
                                    Container container,
                                    Object constraints)
    {
        if (container.equals(Container.CONTAINER_CHAT_WINDOW))
        {
            if (constraints.equals(BorderLayout.SOUTH))
            {
                pluginPanelSouth.add(c);
                pluginPanelSouth.repaint();
            }
            else if (constraints.equals(BorderLayout.WEST))
            {
                pluginPanelWest.add(c);
                pluginPanelSouth.repaint();
            }
            else if (constraints.equals(BorderLayout.EAST))
            {
                pluginPanelEast.add(c);
                pluginPanelSouth.repaint();
            }
        }
        else if (container.equals(Container.CONTAINER_CHAT_STATUS_BAR))
        {
            statusBarPanel.add(c);
        }

        this.getContentPane().repaint();
    }

    /**
     * Removes the given component from the container corresponding to the given
     * constraints.
     *
     * @param c the component to remove
     * @param container the plugin container
     * @param constraints the constraints determining the container
     */
    private void removePluginComponent( Component c,
                                        Container container,
                                        Object constraints)
    {
        if (container.equals(Container.CONTAINER_CHAT_WINDOW))
        {
            if (constraints.equals(BorderLayout.SOUTH))
                pluginPanelSouth.remove(c);
            else if (constraints.equals(BorderLayout.WEST))
                pluginPanelWest.remove(c);
            else if (constraints.equals(BorderLayout.EAST))
                pluginPanelEast.remove(c);
        }
        else if (container.equals(Container.CONTAINER_CHAT_STATUS_BAR))
        {
            this.statusBarPanel.remove(c);
        }
    }

    /**
     * Sets the chat panel contact photo to this window.
     *
     * @param chatSession
     */
    private void setChatContactPhoto(ChatSession chatSession)
    {
        this.contactPhotoPanel.setChatSession(chatSession);

        byte[] chatAvatar = chatSession.getChatAvatar();

        ImageIcon contactPhotoIcon;
        if (chatAvatar != null && chatAvatar.length > 0)
        {
            contactPhotoIcon = ImageUtils.getScaledRoundedIcon(chatAvatar,
                                                                10,
                                                                10);

            if (contactPhotoIcon != null)
                this.setIconImage(contactPhotoIcon.getImage());
        }
        else
        {
            this.setIconImage(ImageLoader
                .getImage(ImageLoader.SIP_COMMUNICATOR_LOGO));
        }
    }


    /**
     * Implementation of {@link ExportedWindow#setParams(Object[])}.
     */
    public void setParams(Object[] windowParams) {}

    /**
     * Handles <tt>WindowEvent</tt>s triggered when the window has gained focus.
     * @param evt the <tt>WindowEvent</tt>
     */
    public void windowGainedFocus(WindowEvent evt)
    {
        ChatPanel currentChat = getCurrentChat();

        if (currentChat != null)
            GuiActivator.getUIService().getChatWindowManager()
                .removeNonReadChatState(currentChat);
    }

    public void windowLostFocus(WindowEvent arg0) {}

    private static class ToolbarPanel
        extends TransparentPanel
        implements Skinnable
    {
        private Image logoBgImage;

        public ToolbarPanel(LayoutManager layoutManager)
        {
            super(layoutManager);

            loadSkin();
        }

        public void paintComponent(Graphics g)
        {
            super.paintComponent(g);

            if (logoBgImage != null)
            {
                g.drawImage(
                        logoBgImage,
                        (this.getWidth() - logoBgImage.getWidth(null))/2,
                        0,
                        null);
            }
        }

        /**
         * Reloads bg image.
         */
        public void loadSkin()
        {
            Image logoBgImage
                = ImageLoader.getImage(ImageLoader.WINDOW_TITLE_BAR);
            if (logoBgImage != null)
                logoBgImage
                    = ImageUtils.scaleImageWithinBounds(logoBgImage, 80, 35);

            this.logoBgImage = logoBgImage;
        }
    }

    /**
     * Sends all files from the given directory when it's dropped in the chat
     * window.
     * @param dir the directory to send
     * @param point the point, where the directory was dropped
     */
    public void directoryDropped(File dir, Point point)
    {
        //TODO: Implement send directory
    }

    /**
     * Sends the given file when dropped to the chat window.
     * @param file the file to send
     * @param point the point, where the file was dropped
     */
    public void fileDropped(File file, Point point)
    {
        ChatPanel chatPanel = getCurrentChat();

        chatPanel.sendFile(file);
    }

    /**
     * Opens the specified <tt>ChatPanel</tt> and optinally brings it to the
     * front.
     *
     * @param chatPanel the <tt>ChatPanel</tt> to be opened
     * @param setSelected <tt>true</tt> if <tt>chatPanel</tt> (and respectively
     * this <tt>ChatContainer</tt>) should be brought to the front; otherwise,
     * <tt>false</tt>
     */
    public void openChat(ChatPanel chatPanel, boolean setSelected)
    {
        if(getExtendedState() != JFrame.ICONIFIED)
        {
            if(ConfigurationManager.isAutoPopupNewMessage()
                    || setSelected)
                toFront();
        }
        else
        {
            if(setSelected)
            {
                setExtendedState(JFrame.NORMAL);
                toFront();
            }

            String chatWindowTitle = getTitle();

            if(!chatWindowTitle.startsWith("*"))
                setTitle("*" + chatWindowTitle);
        }

        if(setSelected)
        {
            setCurrentChat(chatPanel);
        }
        else if(!getCurrentChat().equals(chatPanel)
            && getChatTabCount() > 0)
        {
            highlightTab(chatPanel);
        }
    }

    /**
     * Returns the frame to which this container belongs.
     *
     * @return the frame to which this container belongs
     */
    public Frame getFrame()
    {
        return this;
    }

    /**
     * Sets the title of this chat container.
     *
     * @param chatPanel the chat, for which we set the title
     * @param title the title to set
     */
    public void setChatTitle(ChatPanel chatPanel, String title)
    {
        setTabTitle(chatPanel, title);
    }

    public void setChatIcon(ChatPanel chatPanel, Icon icon)
    {
        setTabIcon(chatPanel, icon);
    }

    /**
     * Indicates if the parent frame is currently the active window.
     *
     * @return <tt>true</tt> if the parent window is currently the active
     * window, <tt>false</tt> otherwise
     */
    public boolean isFrameActive()
    {
        return isActive();
    }
}
