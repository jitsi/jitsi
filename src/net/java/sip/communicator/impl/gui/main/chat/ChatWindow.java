/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.chat;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import javax.swing.event.*;
import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.customcontrols.events.*;
import net.java.sip.communicator.impl.gui.event.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.main.chat.menus.*;
import net.java.sip.communicator.impl.gui.main.chat.toolBars.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.Container;
import net.java.sip.communicator.service.keybindings.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

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
 */
public class ChatWindow
    extends SIPCommFrame
    implements  ExportedWindow,
                PluginComponentListener
{
    private Logger logger = Logger.getLogger(ChatWindow.class.getName());

    private MenusPanel menusPanel;

    private MainFrame mainFrame;

    private SIPCommTabbedPane chatTabbedPane = null;

    private int chatCount = 0;
    
    private Vector chatChangeListeners = new Vector();

    /**
     * Creates an instance of <tt>ChatWindow</tt> by passing to it an instance
     * of the main application window.
     * 
     * @param mainFrame the main application window
     */
    public ChatWindow(MainFrame mainFrame)
    {
        this.mainFrame = mainFrame;

        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        menusPanel = new MenusPanel(this);

        //If in mode TABBED_CHAT_WINDOW initialize the tabbed pane
        if(ConfigurationManager.isMultiChatWindowEnabled())
        {
            chatTabbedPane = new SIPCommTabbedPane(true, false);

            chatTabbedPane.addCloseListener(new CloseListener() {
                public void closeOperation(MouseEvent e)
                {
                    int tabIndex = chatTabbedPane.getOverTabIndex();

                    ChatPanel chatPanel
                        = (ChatPanel) chatTabbedPane.getComponentAt(tabIndex);

                    ChatWindow.this.mainFrame
                        .getChatWindowManager().closeChat(chatPanel);
                }
            });
            
            chatTabbedPane.addChangeListener(new ChangeListener() 
            {
                public void stateChanged(ChangeEvent e) 
                {
                    int tabIndex = chatTabbedPane.getSelectedIndex();

                    if(tabIndex == -1)
                        return;
                    
                    ChatPanel chatPanel
                        = (ChatPanel) chatTabbedPane.getComponentAt(tabIndex);
                    
                    fireChatChangeEvent(chatPanel);
                }
            });
        }

        this.setSizeAndLocation();

        JPanel northPanel = new JPanel(new BorderLayout());

        northPanel.add(new LogoBar(), BorderLayout.NORTH);
        northPanel.add(menusPanel, BorderLayout.CENTER);

        this.getContentPane().setLayout(new BorderLayout(5, 5));
        this.getContentPane().add(northPanel, BorderLayout.NORTH);

        this.initPluginComponents();

        this.setKeybindingInput(KeybindingSet.Category.CHAT);
        this.addKeybindingAction("chat-nextTab", new ForwordTabAction());
        this.addKeybindingAction("chat-previousTab", new BackwordTabAction());
        this.addKeybindingAction("chat-copy", new CopyAction());
        this.addKeybindingAction("chat-paste", new PasteAction());
        this.addKeybindingAction("chat-openSmilies", new OpenSmileyAction());
        this.addKeybindingAction("chat-openHistory", new OpenHistoryAction());
        this.addKeybindingAction("chat-close", new CloseAction());

        this.addWindowListener(new ChatWindowAdapter());
    }
    
    /**
     * Returns the main application widnow.
     * 
     * @return The main application widnow.
     */
    public MainFrame getMainFrame()
    {
        return mainFrame;
    }
    
    /**
     * Returns the main toolbar in this chat window.
     * @return the main toolbar in this chat window
     */
    public MainToolBar getMainToolBar()
    {
        return menusPanel.getMainToolBar();
    }
    
    /**
     * Adds a given <tt>ChatPanel</tt> to this chat window.
     * 
     * @param chatPanel The <tt>ChatPanel</tt> to add.
     */
    public void addChat(ChatPanel chatPanel)
    {
        if (ConfigurationManager.isMultiChatWindowEnabled())
            addChatTab(chatPanel);
        else
            addSimpleChat(chatPanel);

        chatCount ++;

        chatPanel.setShown(true);
    }

    /**
     * Adds a given <tt>ChatPanel</tt> to this chat window.
     * 
     * @param chatPanel The <tt>ChatPanel</tt> to add.
     */
    private void addSimpleChat(ChatPanel chatPanel)
    {
        this.getContentPane().add(chatPanel, BorderLayout.CENTER);
      
        fireChatChangeEvent(chatPanel);
    }

    /**
     * Adds a given <tt>ChatPanel</tt> to the <tt>JTabbedPane</tt> of this
     * chat window.
     * 
     * @param chatPanel The <tt>ChatPanel</tt> to add.
     */
    private void addChatTab(ChatPanel chatPanel)
    {
        String chatName = chatPanel.getChatName();

        if (getCurrentChatPanel() == null)
        {
            this.getContentPane().add(chatPanel, BorderLayout.CENTER);
            fireChatChangeEvent(chatPanel);
        }
        else
        {
            if (getChatTabCount() == 0)
            {
                ChatPanel firstChatPanel = getCurrentChatPanel();

                // Add first two tabs to the tabbed pane.
                chatTabbedPane.addTab(  firstChatPanel.getChatName(),
                                        firstChatPanel.getChatStatusIcon(),
                                        firstChatPanel);

                chatTabbedPane.addTab(  chatName,
                                        chatPanel.getChatStatusIcon(),
                                        chatPanel);

                // When added to the tabbed pane, the first chat panel should
                // rest the selected component.
                chatTabbedPane.setSelectedComponent(firstChatPanel);

                // Workaround for the following problem:
                // The scrollbar in the conversation area moves up when the
                // scrollpane is resized. This happens when ChatWindow is in
                // mode "Group messages in one window" and the first chat panel
                // is added to the tabbed pane. Then the scrollpane in the
                // conversation area is slightly resized and is made smaller,
                // which moves the scrollbar up.
                firstChatPanel.setCaretToEnd();

                //add the chatTabbedPane to the window
                this.getContentPane().add(chatTabbedPane, BorderLayout.CENTER);
                this.getContentPane().validate();
            }
            else
            {
                // The tabbed pane contains already tabs.

                chatTabbedPane.addTab(  chatName,
                                        chatPanel.getChatStatusIcon(),
                                        chatPanel);

                chatTabbedPane.getParent().validate();
            }
        }
    }

    /**
     * Removes a given <tt>ChatPanel</tt> from this chat window.
     * 
     * @param chatPanel The <tt>ChatPanel</tt> to remove.
     */
    public void removeChat(ChatPanel chatPanel)
    {
        logger.debug("Removes chat for contact: "
                + chatPanel.getChatName());

        //if there's no tabs remove the chat panel directly from the content
        //pane.
        if(getChatTabCount() == 0)
        {
            this.getContentPane().remove(chatPanel);

            chatCount --;

            return;
        }

        //in the case of a tabbed chat window
        int index = chatTabbedPane.indexOfComponent(chatPanel);

        if (index != -1)
        {
            if (chatTabbedPane.getTabCount() > 1)
                chatTabbedPane.removeTabAt(index);

            if (chatTabbedPane.getTabCount() == 1)
            {
                ChatPanel currentChatPanel = getCurrentChatPanel();

                this.chatTabbedPane.removeAll();

                this.getContentPane().remove(chatTabbedPane);

                this.getContentPane().add(currentChatPanel, BorderLayout.CENTER);

                this.setCurrentChatPanel(currentChatPanel);
            }

            chatCount --;
        }
    }

    /**
     * Removes all tabs in the chat tabbed pane. If not in mode
     * TABBED_CHAT_WINDOW doesn nothing.
     */
    public void removeAllChats()
    {
        logger.debug("Remove all tabs from the chat window.");
        
        if(getChatTabCount() > 0)
        {
            this.chatTabbedPane.removeAll();

            this.getContentPane().remove(chatTabbedPane);

            chatCount = 0;
        }
        else
        {
            this.removeChat(getCurrentChatPanel());
        }
    }

    /**
     * Selects the chat tab which corresponds to the given <tt>MetaContact</tt>.
     * 
     * @param chatPanel The <tt>ChatPanel</tt> to select.
     */
    public void setCurrentChatPanel(ChatPanel chatPanel)
    {
        logger.debug("Set current chat panel to: "
            + chatPanel.getChatName());
        
        if(getChatTabCount() > 0)
            this.chatTabbedPane.setSelectedComponent(chatPanel);
        
        this.setTitle(chatPanel.getChatName());
        
        chatPanel.requestFocusInWriteArea();
    }
    
    /**
     * Selects the tab given by the index. If there's no tabbed pane does nothing.
     * @param index the index to select
     */
    public void setCurrentChatTab(int index)
    {   
        ChatPanel chatPanel = null;
        if(getChatTabCount() > 0)
        {
            chatPanel = (ChatPanel) this.chatTabbedPane
                .getComponentAt(index);
        
            setCurrentChatPanel(chatPanel);
        }
    }

    /**
     * Returns the currently selected chat panel.
     * 
     * @return the currently selected chat panel.
     */
    public ChatPanel getCurrentChatPanel()
    {   
        if(getChatTabCount() > 0)
            return (ChatPanel)chatTabbedPane.getSelectedComponent();
        else
        {
            int componentCount = getContentPane().getComponentCount();
            
            for (int i = 0; i < componentCount; i ++)
            {
                Component c = getContentPane().getComponent(i);

                if(c instanceof ChatPanel)
                {
                    return (ChatPanel)c;
                }
            }
        }
        return null;
    }

    /**
     * Returns the tab count of the chat tabbed pane. Meant to be used when in
     * "Group chat windows" mode.
     * 
     * @return int The number of opened tabs.
     */
    public int getChatTabCount()
    {
        return (chatTabbedPane == null) ? 0 : chatTabbedPane.getTabCount();
    }

    /**
     * Highlights the corresponding tab for the given chat panel.
     * 
     * @param chatPanel the chat panel which corresponds to the tab to highlight
     */
    public void highlightTab(ChatPanel chatPanel)
    {
        this.chatTabbedPane.highlightTab(
            chatTabbedPane.indexOfComponent(chatPanel));
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
     * The <tt>ForwordTabAction</tt> is an <tt>AbstractAction</tt> that
     * changes the currently selected tab with the next one. Each time when the
     * last tab index is reached the first one is selected.
     */
    private class ForwordTabAction
        extends AbstractAction
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
    };

    /**
     * The <tt>BackwordTabAction</tt> is an <tt>AbstractAction</tt> that
     * changes the currently selected tab with the previous one. Each time when
     * the first tab index is reached the last one is selected.
     */
    private class BackwordTabAction
        extends AbstractAction
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
    };

    /**
     * The <tt>CopyAction</tt> is an <tt>AbstractAction</tt> that copies the
     * text currently selected.
     */
    private class CopyAction
        extends AbstractAction
    {
        public void actionPerformed(ActionEvent e)
        {
            getCurrentChatPanel().copy();
        }
    };

    /**
     * The <tt>PasteAction</tt> is an <tt>AbstractAction</tt> that pastes
     * the text contained in the clipboard in the current <tt>ChatPanel</tt>.
     */
    private class PasteAction
        extends AbstractAction
    {
        public void actionPerformed(ActionEvent e)
        {
            getCurrentChatPanel().paste();
        }
    };

    /**
     * The <tt>SendMessageAction</tt> is an <tt>AbstractAction</tt> that
     * sends the text that is currently in the write message area.
     */
    private class SendMessageAction
        extends AbstractAction
    {
        public void actionPerformed(ActionEvent e)
        {
            ChatPanel chatPanel = getCurrentChatPanel();
            // chatPanel.stopTypingNotifications();
            chatPanel.sendButtonDoClick();
        }
    }

    /**
     * The <tt>OpenSmileyAction</tt> is an <tt>AbstractAction</tt> that
     * opens the menu, containing all available smilies' icons.
     */
    private class OpenSmileyAction
        extends AbstractAction
    {
        public void actionPerformed(ActionEvent e)
        {
            getCurrentChatPanel().getChatWritePanel()
                .getEditTextToolBar().getSmiliesSelectorBox().open();
        }
    }

    /**
     * The <tt>OpenHistoryAction</tt> is an <tt>AbstractAction</tt> that
     * opens the history window for the currently selected contact.
     */
    private class OpenHistoryAction
        extends AbstractAction
    {
        public void actionPerformed(ActionEvent e)
        {
            menusPanel.getMainToolBar().getHistoryButton().doClick();
        }
    }

    
    /**
     * The <tt>CloseAction</tt> is an <tt>AbstractAction</tt> that
     * closes a tab in the chat window.
     */
    private class CloseAction
        extends AbstractAction
    {
        public void actionPerformed(ActionEvent e)
        {
            close(true);
        }
    }
    
        
    /**
     * Returns the time of the last received message.
     *
     * @return The time of the last received message.
     */
    public Date getLastIncomingMsgTimestamp(ChatPanel chatPanel)
    {
        return chatPanel.getChatConversationPanel()
            .getLastIncomingMsgTimestamp();
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
     */
    protected void close(boolean isEscaped)
    {
        ChatPanel chatPanel = getCurrentChatPanel();

        if(isEscaped)
        {
            ChatRightButtonMenu chatRightMenu = getCurrentChatPanel()
                .getChatConversationPanel().getRightButtonMenu();

            WritePanelRightButtonMenu writePanelRightMenu = getCurrentChatPanel()
                .getChatWritePanel().getRightButtonMenu();

            SIPCommMenu selectedMenu
                = menusPanel.getMainMenuBar().getSelectedMenu();
            //SIPCommMenu contactMenu = getCurrentChatPanel()
            //    .getProtoContactSelectorBox().getMenu();
            
            MenuSelectionManager menuSelectionManager
                = MenuSelectionManager.defaultManager();
            
            if (chatRightMenu.isVisible())
            {
                chatRightMenu.setVisible(false);
            }
            else if (writePanelRightMenu.isVisible())
            {
                writePanelRightMenu.setVisible(false);
            }
            else if (selectedMenu != null
                || getCurrentChatPanel().getChatWritePanel()
                    .getEditTextToolBar().hasSelectedMenus())
            {   
                menuSelectionManager.clearSelectedPath();
            }
            else
            {
                mainFrame.getChatWindowManager().closeChat(chatPanel);
            }
        }
        else 
        {
            mainFrame.getChatWindowManager().closeWindow(this);
        }
    }

    /**
     * Implements the <tt>ExportedWindow.getIdentifier()</tt> method.
     * Returns the identifier of this window, which will 
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
        // Search for plugin components registered through the OSGI bundle
        // context.
        ServiceReference[] serRefs = null;

        String osgiFilter = "("
            + Container.CONTAINER_ID
            + "="+Container.CONTAINER_CHAT_WINDOW.getID()+")";

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

            for (int i = 0; i < serRefs.length; i ++)
            {
                PluginComponent c = (PluginComponent) GuiActivator
                    .bundleContext.getService(serRefs[i]);

                Object borderLayoutConstraint = UIServiceImpl
                    .getBorderLayoutConstraintsFromContainer(c.getConstraints());

                this.add((Component)c.getComponent(), borderLayoutConstraint);
            }
        }

        GuiActivator.getUIService().addPluginComponentListener(this);
    }

    public void pluginComponentAdded(PluginComponentEvent event)
    {
        PluginComponent c = event.getPluginComponent();

        if (c.getContainer().equals(Container.CONTAINER_CHAT_WINDOW))
        {
            Object borderLayoutConstraints = UIServiceImpl
                .getBorderLayoutConstraintsFromContainer(c.getConstraints());

            this.add((Component) c.getComponent(), borderLayoutConstraints);
        }
    }

    public void pluginComponentRemoved(PluginComponentEvent event)
    {
        PluginComponent c = event.getPluginComponent();

        if (c.getContainer().equals(Container.CONTAINER_CHAT_WINDOW))
        {
            this.getContentPane().remove((Component) c.getComponent());

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
    
    public void addChatChangeListener(ChatChangeListener listener)
    {
        synchronized (chatChangeListeners)
        {
            chatChangeListeners.add(listener);
        }
    }
    
    public void removeChatChangeListener(ChatChangeListener listener)
    {
        synchronized (chatChangeListeners)
        {
            chatChangeListeners.remove(listener);
        }
    }

    private void fireChatChangeEvent(ChatPanel panel)
    {
        Iterator listeners = null;
        synchronized (chatChangeListeners)
        {
            listeners = new ArrayList(chatChangeListeners).iterator();
        }

        while (listeners.hasNext())
        {
            ChatChangeListener listener
                = (ChatChangeListener) listeners.next();

            listener.chatChanged(panel);
        }
    }
    
    private class LogoBar
    extends JPanel
    {
        /**
         * Creates the logo bar and specify the size.
         */
        public LogoBar()
        {
            int width = GuiActivator.getResources().getSettingsInt("logoBarWidth");
            int height = GuiActivator.getResources().getSettingsInt("logoBarHeight");

            this.setMinimumSize(new Dimension(width, height));
            this.setPreferredSize(new Dimension(width, height));
        }

        /**
         * Paints the logo bar.
         * 
         * @param g the <tt>Graphics</tt> object used to paint the background
         * image of this logo bar.
         */
        public void paintComponent(Graphics g)
        {
            super.paintComponent(g);

            Image backgroundImage
                = ImageLoader.getImage(ImageLoader.WINDOW_TITLE_BAR);

            g.setColor(new Color(
                GuiActivator.getResources().getColor("logoBarBackground")));

            g.fillRect(0, 0, this.getWidth(), this.getHeight());
            g.drawImage(backgroundImage, 0, 0, null);
        }
    }
}
