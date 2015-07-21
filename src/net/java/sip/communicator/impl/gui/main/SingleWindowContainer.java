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
package net.java.sip.communicator.impl.gui.main;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.event.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.call.*;
import net.java.sip.communicator.impl.gui.main.chat.*;
import net.java.sip.communicator.impl.gui.main.chat.toolBars.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.util.*;

/**
 * The container of the single window interface.
 *
 * @author Yana Stamcheva
 * @author Lyubomir Marinov
 */
public class SingleWindowContainer
    extends TransparentPanel
    implements ChatContainer,
               CallContainer,
               CallTitleListener,
               ChangeListener
{
    /**
     * Chat change listeners.
     */
    private final List<ChatChangeListener> chatChangeListeners
        = new Vector<ChatChangeListener>();

    /**
     * The contact photo panel.
     */
    private final ContactPhotoPanel contactPhotoPanel;

    /**
     * The count of current conversations.
     */
    private int conversationCount = 0;

    /**
     * The <tt>Logger</tt> used by this instance for logging output.
     */
    private final Logger logger = Logger.getLogger(SingleWindowContainer.class);

    /**
     * The main toolbar.
     */
    private MainToolBar mainToolBar;

    /**
     * The tabbed pane, containing all conversations.
     */
    private final ConversationTabbedPane tabbedPane;

    /**
     * Creates an instance of the <tt>SingleWindowContainer</tt>.
     */
    public SingleWindowContainer()
    {
        super(new BorderLayout());
        setPreferredSize(new Dimension(620, 580));

        tabbedPane = new ConversationTabbedPane();
        contactPhotoPanel = new ContactPhotoPanel();

        add(createToolbar(), BorderLayout.NORTH);
        tabbedPane.addChangeListener(this);

        add(tabbedPane);
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

        callPanel.requestFocus();
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

        tabbedPane.addTab(name, icon, conversation);
        tabbedPane.getParent().validate();

        // If not specified explicitly, when added to the tabbed pane, the first
        // chat panel should rest the selected component.
        tabbedPane.setSelectedComponent(
                (currentConversation != null && !isSelected)
                    ? currentConversation
                    : conversation);
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
     * {@inheritDoc}
     *
     * The delay implemented by <tt>SingleWindowContainer</tt> is 5 seconds.
     */
    public void close(CallPanel callPanel, boolean delay)
    {
        if (delay)
        {
            Timer timer = new Timer(5000, new CloseCallListener(callPanel));

            timer.setRepeats(false);
            timer.start();
        }
        else
            removeConversation(callPanel);
    }

    /**
     * Indicates if one of the contained components is currently the owner of
     * the keyboard focus.
     *
     * @return <tt>true</tt> to indicate that a component contained in this
     * container currently owns the keyboard focus, <tt>false</tt> - otherwise
     */
    public boolean containsFocusOwner()
    {
        ChatPanel chat = getCurrentChat();

        return
            (chat != null)
                && chat.getChatWritePanel().getEditorPane().isFocusOwner();
    }

    private Component createToolbar()
    {
        mainToolBar = new MainToolBar(this);

        // The toolbar would be only visible when a chat is opened.
        mainToolBar.setVisible(false);

        JPanel northPanel = new TransparentPanel(new BorderLayout());

        northPanel.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));
        northPanel.setPreferredSize(new Dimension(500, 35));
        northPanel.setVisible(ConfigurationUtils.isChatToolbarVisible());
        northPanel.add(mainToolBar, BorderLayout.EAST);
        northPanel.add(contactPhotoPanel, BorderLayout.WEST);

        return northPanel;
    }

    /**
     * {@inheritDoc}
     *
     * <tt>SingleWindowContainer</tt> does nothing.
     */
    public void ensureSize(Component component, int width, int height)
    {
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
     * Returns the frame to which this container belongs.
     *
     * @return the frame to which this container belongs
     */
    public JFrame getFrame()
    {
        return GuiActivator.getUIService().getMainFrame();
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

    /**
     * {@inheritDoc}
     *
     * <tt>SingleWindowContainer</tt> does not support display in full-screen
     * mode and thus is expected to return <tt>false</tt>.
     *
     * @see CallDialog#isFullScreen(Window)
     */
    public boolean isFullScreen()
    {
        return CallDialog.isFullScreen(getFrame());
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
            if(ConfigurationUtils.isAutoPopupNewMessage()
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

    /**
     * Packs the content of this call window.
     */
    public void pack()
    {
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

    public void setChatIcon(ChatPanel chatPanel, Icon icon) {}

    public void setChatTitle(ChatPanel chatPanel, String title) {}

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
     * {@inheritDoc}
     *
     * <tt>SingleWindowContainer</tt> does not support display in full-screen
     * mode and thus does nothing.
     */
    public void setFullScreen(boolean fullScreen)
    {
        // TODO Auto-generated method stub
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

    public void setTitle(String title)
    {
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
     * Updates history buttons state.
     *
     * @param chatPanel the chat panel for which we should update button states
     */
    public void updateHistoryButtonState(ChatPanel chatPanel)
    {
        mainToolBar.changeHistoryButtonsState(chatPanel);
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
}
