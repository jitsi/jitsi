/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chat;

import net.java.sip.communicator.util.skin.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The <tt>ChatTabbedPane</tt> is a <tt>SIPCommTabbedPane</tt> that takes into
 * account the number of unread messages received in a certain tab and shows
 * this information to the user.
 * 
 * @author Yana Stamcheva
 * @author Adam Netocny
 */
public class ChatTabbedPane
    extends SIPCommTabbedPane
    implements Skinnable
{
    /**
     * Creates a <tt>ChatTabbedPane</tt> with a close tab funcion.
     */
    public ChatTabbedPane()
    {
        super(true, false);
    }

    /**
     * Overrides setSelectedIndex in SIPCommTabbedPane in order to remove the
     * indicator of number of unread messages previously set.
     * 
     * @param tabIndex the index of the tab to be selected
     */
    public void setSelectedIndex(int tabIndex)
    {
        if (tabIndex < 0)
            return;

        ChatPanel chatPanel = (ChatPanel) this.getComponentAt(tabIndex);
        int unreadMessageNumber = chatPanel.unreadMessageNumber;

        if (unreadMessageNumber > 0)
        {
            String tabTitle = chatPanel.getChatSession().getChatName();
            this.setTitleAt(tabIndex, tabTitle);
        }

        chatPanel.unreadMessageNumber = 0;

        super.setSelectedIndex(tabIndex);
    }

    /**
     * When a tab is highlighted sets an indicator of the number of unread
     * messages in this tab.
     *
     * @param tabIndex the index of the tab
     * @param unreadMessageNumber the number of messages that the user hasn't
     * yet read
     */
    public void highlightTab(int tabIndex, int unreadMessageNumber)
    {
        ChatPanel chatPanel = (ChatPanel) this.getComponentAt(tabIndex);
        String tabTitle = chatPanel.getChatSession().getChatName();

        if (unreadMessageNumber > 0)
            tabTitle = "(" + unreadMessageNumber + ") " + tabTitle;

        this.setTitleAt(tabIndex, tabTitle);

        super.highlightTab(tabIndex);
    }

    /**
     * Reloads status icons.
     */
    public void loadSkin()
    {
        super.loadSkin();

        int count = getTabCount();

        for(int i = 0; i < count; i++)
        {
            ChatPanel chatPanel = (ChatPanel) this.getComponentAt(i);
            setIconAt(i, chatPanel.getChatSession().getChatStatusIcon());
        }
    }
}
