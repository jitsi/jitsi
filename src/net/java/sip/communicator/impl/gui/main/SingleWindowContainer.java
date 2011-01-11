/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.event.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.call.*;
import net.java.sip.communicator.impl.gui.main.call.CallDialog.*;
import net.java.sip.communicator.impl.gui.main.chat.*;
import net.java.sip.communicator.impl.gui.main.chat.toolBars.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The container of the single window interface.
 *
 * @author Yana Stamcheva
 */
public class SingleWindowContainer
    extends TransparentPanel
    implements  ChatContainer,
                CallContainer,
                CallTitleListener,
                ChangeListener
{
    /**
     * The logger for this class.
     */
    private final Logger logger = Logger.getLogger(SingleWindowContainer.class);

    /**
     * The tabbed pane, containing all conversations.
     */
    private ConversationTabbedPane tabbedPane = null;

    /**
     * The count of current conversations.
     */
    private int conversationCount = 0;

    /**
     * Chat change listeners.
     */
    private final java.util.List<ChatChangeListener> chatChangeListeners =
        new Vector<ChatChangeListener>();

    /**
     * The contact photo panel.
     */
    private final ContactPhotoPanel contactPhotoPanel = new ContactPhotoPanel();

    /**
     * The main toolbar.
     */
    private MainToolBar mainToolBar;

    /**
     * Creates an instance of the <tt>SingleWindowContainer</tt>.
     */
    public SingleWindowContainer()
    {
        super(new BorderLayout());
        setPreferredSize(new Dimension(620, 580));

        add(createToolbar(), BorderLayout.NORTH);

        tabbedPane = new ConversationTabbedPane();
        tabbedPane.addChangeListener(this);

        add(tabbedPane);
    }

    public void setTitle(String title)
    {
    }

    /**
     * Adds a given <tt>ChatPanel</tt> to this chat window.
     * 
     * @param chatPanel The <tt>ChatPanel</tt> to add.
     */
    public void addChat(ChatPanel chatPanel)
    {
        ChatSession chatSession = chatPanel.getChatSession();

        addConversationTab(chatSession.getChatName(),
            chatSession.getChatStatusIcon(), chatPanel, false);

        conversationCount ++;

        chatPanel.setShown(true);

        for (ChatChangeListener l : this.chatChangeListeners)
            l.chatChanged(chatPanel);
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

        removeConversation(chatPanel);
    }

    /**
     * Removes a given <tt>ChatPanel</tt> from this chat window.
     * 
     * @param c the conversation component
     */
    private void removeConversation(Component c)
    {
        int index = tabbedPane.indexOfComponent(c);

        if (index > -1)
        {
            tabbedPane.removeTabAt(index);

            conversationCount --;
        }

        if (tabbedPane.getTabCount() == 0)
            setToolbarVisible(false);
    }

    /**
     * Returns the currently selected chat panel.
     * 
     * @return the currently selected chat panel.
     */
    public ChatPanel getCurrentChat()
    {
        Component c = getCurrentConversation();

        if (c instanceof ChatPanel)
            return (ChatPanel) c;

        return null;
    }

    /**
     * Returns the currently selected chat panel.
     * 
     * @return the currently selected chat panel.
     */
    private Component getCurrentConversation()
    {
        if(tabbedPane.getTabCount() > 0)
            return tabbedPane.getSelectedComponent();

        return null;
    }

    /**
     * Selects the chat tab which corresponds to the given <tt>MetaContact</tt>.
     * 
     * @param chatPanel The <tt>ChatPanel</tt> to select.
     */
    public void setCurrentChat(ChatPanel chatPanel)
    {
        ChatSession chatSession = chatPanel.getChatSession();

        if (logger.isDebugEnabled())
            logger.debug(
                "Set current chat panel to: " + chatSession.getChatName());

        if(tabbedPane.getTabCount() > 0)
            this.tabbedPane.setSelectedComponent(chatPanel);

        this.setTitle(chatSession.getChatName());
        this.contactPhotoPanel.setChatSession(chatSession);

        chatPanel.requestFocusInWriteArea();

        for (ChatChangeListener l : this.chatChangeListeners)
        {
            l.chatChanged(chatPanel);
        }
    }

    /**
     * Shows/hides the toolbar.
     *
     * @param isVisible 
     */
    public void setToolbarVisible(boolean isVisible)
    {
        mainToolBar.setVisible(isVisible);
        contactPhotoPanel.setVisible(isVisible);

        revalidate();
        repaint();
    }

    /**
     * Removes all tabs in the chat tabbed pane. If not in mode
     * TABBED_CHAT_WINDOW does nothing.
     */
    public void removeAllChats()
    {
        if (logger.isDebugEnabled())
            logger.debug("Remove all tabs from the chat window.");

        if(tabbedPane.getTabCount() > 0)
        {
            this.tabbedPane.removeAll();

            conversationCount = 0;

            if (tabbedPane.getTabCount() == 0)
                setToolbarVisible(false);
        }
    }

    /**
     * Returns the number of all open chats.
     * 
     * @return the number of all open chats
     */
    public int getChatCount()
    {
        return conversationCount;
    }

    /**
     * Returns the currently available chat panels.
     *
     * @return the currently available chat panels.
     */
    public java.util.List<ChatPanel> getChats()
    {
        ArrayList<ChatPanel> chatPanels = new ArrayList<ChatPanel>();

        if(tabbedPane.getTabCount() > 0)
        {
            int componentCount = tabbedPane.getComponentCount();

            for (int i = 0; i < componentCount; i ++)
            {
                Component c = tabbedPane.getComponent(i);

                if(c instanceof ChatPanel)
                {
                    chatPanels.add((ChatPanel)c);
                }
            }
        }

        return chatPanels;
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
        MainFrame mainWindow = GuiActivator.getUIService().getMainFrame();
        if(mainWindow.getExtendedState() != JFrame.ICONIFIED)
        {
            if(ConfigurationManager.isAutoPopupNewMessage()
                    || setSelected)
                mainWindow.toFront();
        }
        else
        {
            if(setSelected)
            {
                mainWindow.setExtendedState(JFrame.NORMAL);
                mainWindow.toFront();
            }

//            String chatWindowTitle = getTitle();
            String chatWindowTitle = "TEST";

            if(!chatWindowTitle.startsWith("*"))
                setTitle("*" + chatWindowTitle);
        }

        if(setSelected)
        {
            setCurrentChat(chatPanel);
        }
        else if(!getCurrentChat().equals(chatPanel)
            && tabbedPane.getTabCount() > 0)
        {
            highlightTab(chatPanel);
        }
    }

    public void setChatTitle(ChatPanel chatPanel, String title) {}

    public void setChatIcon(ChatPanel chatPanel, Icon icon) {}

    /**
     * Returns the frame to which this container belongs.
     *
     * @return the frame to which this container belongs
     */
    public JFrame getFrame()
    {
        return GuiActivator.getUIService().getMainFrame();
    }

    /**
     * Adds the given <tt>ChatChangeListener</tt>.
     *
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
     * Adds a given <tt>ChatPanel</tt> to the <tt>JTabbedPane</tt> of this
     * chat window.
     *
     * @param name the name of the tab
     * @param icon the tab icon
     * @param conversation the conversation component to add in the tab
     * @param isSelected indicates if this tab should be selected
     */
    private void addConversationTab(String name,
                                    Icon icon,
                                    Component conversation,
                                    boolean isSelected)
    {
        Component currentConversation = getCurrentConversation();

        tabbedPane.addTab(
            name,
            icon,
            conversation);

        tabbedPane.getParent().validate();

        // If not specified explicitly, when added to the tabbed pane,
        // the first chat panel should rest the selected component.
        if (currentConversation != null && !isSelected)
            tabbedPane.setSelectedComponent(currentConversation);
        else
            tabbedPane.setSelectedComponent(conversation);
    }

    /**
     * Highlights the corresponding tab for the given chat panel.
     * 
     * @param chatPanel the chat panel which corresponds to the tab to highlight
     */
    private void highlightTab(ChatPanel chatPanel)
    {
        int tabIndex = tabbedPane.indexOfComponent(chatPanel);

        chatPanel.unreadMessageNumber ++;

        tabbedPane.highlightTab(tabIndex, chatPanel.unreadMessageNumber);
    }

    private Component createToolbar()
    {
        JPanel northPanel = new TransparentPanel(new BorderLayout());

        mainToolBar = new MainToolBar(this);

        boolean chatToolbarVisible = ConfigurationManager.isChatToolbarVisible();
        northPanel.setVisible(chatToolbarVisible);

        northPanel.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));
        northPanel.add(mainToolBar, BorderLayout.EAST);
        northPanel.add(contactPhotoPanel, BorderLayout.WEST);

        northPanel.setPreferredSize(new Dimension(500, 35));

        return northPanel;
    }

    /**
     * Indicates if one of the contained components is currently the owner of
     * the keyboard focus.
     *
     * @return <tt>true</tt> to indicate that a component contained in this
     * container currently owns the keyboard focus, <tt>false</tt> - otherwise
     */
    public boolean containsFocus()
    {
        ChatPanel chat = getCurrentChat();

        if (chat != null
            && chat.getChatWritePanel().getEditorPane().isFocusOwner())
            return true;

        return false;
    }

    /**
     * Closes the given <tt>CallPanel</tt>.
     *
     * @param callPanel the <tt>CallPanel</tt> to close
     */
    public void close(CallPanel callPanel)
    {
        removeConversation(callPanel);
    }

    /**
     * Closes the given <tt>CallPanel</tt>.
     *
     * @param callPanel the <tt>CallPanel</tt> to close
     */
    public void closeWait(CallPanel callPanel)
    {
        Timer timer
            = new Timer(5000, new CloseCallListener(callPanel));

        timer.setRepeats(false);
        timer.start();
    }

    /**
     * Removes the given CallPanel from the main tabbed pane.
     */
    private class CloseCallListener
        implements ActionListener
    {
        private final CallPanel callPanel;

        public CloseCallListener(CallPanel callPanel)
        {
            this.callPanel = callPanel;
        }

        public void actionPerformed(ActionEvent e)
        {
            removeConversation(callPanel);
        }
    }

    /**
     * Packs the content of this call window.
     */
    public void pack()
    {
        revalidate();
        repaint();
    }

    /**
     * Adds the given <tt>CallPanel</tt> to this call window.
     *
     * @param callPanel the <tt>CallPanel</tt> to add
     */
    public void addCallPanel(CallPanel callPanel)
    {
        conversationCount ++;

        callPanel.setBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, Color.GRAY));

        callPanel.addCallTitleListener(this);

        addConversationTab(
            callPanel.getCallTitle(), null, callPanel, true);
    }

    /**
     * Called when the title of the given <tt>CallPanel</tt> changes.
     *
     * @param callPanel the <tt>CallPanel</tt>, which title has changed
     */
    public void callTitleChanged(CallPanel callPanel)
    {
        int i = tabbedPane.indexOfComponent(callPanel);

        if (i > -1)
            tabbedPane.setTitleAt(i, callPanel.getCallTitle());
    }

    /**
     * Shows/hides the toolbar depending on the selected tab.
     *
     * @param event the <tt>ChangeEvent</tt> that notified us of the tab
     * selection change
     */
    public void stateChanged(ChangeEvent event)
    {
        int index = tabbedPane.getSelectedIndex();

        // If there's no chat panel selected we do nothing.
        if (index > -1)
        {
            Component c = tabbedPane.getComponentAt(index);

            setToolbarVisible(c instanceof ChatPanel);
        }
    }

    /**
     * Sets the given icon to the tab opened for the given chat panel.
     * 
     * @param chatPanel the chat panel, which corresponds the tab
     * @param icon the icon to be set
     */
    public void setTabIcon(ChatPanel chatPanel, Icon icon)
    {
        int index = this.tabbedPane.indexOfComponent(chatPanel);
        this.tabbedPane.setIconAt(index, icon);
    }

    /**
     * Sets the given title to the tab opened for the given chat panel.
     * @param chatPanel the chat panel
     * @param title the new title of the tab
     */
    public void setTabTitle(ChatPanel chatPanel, String title)
    {
        int index = this.tabbedPane.indexOfComponent(chatPanel);

        if(index > -1)
            this.tabbedPane.setTitleAt(index, title);
    }
}
