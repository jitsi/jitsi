/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chat;

import net.java.sip.communicator.util.swing.*;

/**
 * The <tt>ChatTabbedPane</tt> is a <tt>SIPCommTabbedPane</tt> that takes into
 * account the number of unread messages received in a certain tab and shows
 * this information to the user.
 * 
 * @author Yana Stamcheva
 */
public class ChatTabbedPane
    extends SIPCommTabbedPane
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

        String tabTitle = this.getTitleAt(tabIndex);
        int bracketIndex = tabTitle.indexOf(")");

        if (bracketIndex > - 1)
            // We count the extra space after the bracket. 
            tabTitle = tabTitle.substring(bracketIndex + 2);

        this.setTitleAt(tabIndex, tabTitle);

        ChatPanel chatPanel = (ChatPanel) this.getComponentAt(tabIndex);
        chatPanel.unreadMessageNumber = 0;

        super.setSelectedIndex(tabIndex);
    }

    /**
     * When a tab is highlighted sets an indicator of the number of unread
     * messages in this tab.
     *
     * @param tabIndex the index of the tab
     */
    public void highlightTab(int tabIndex, int unreadMessageNumber)
    {
        String tabTitle = this.getTitleAt(tabIndex);
        int bracketIndex = tabTitle.indexOf(")");

        if (bracketIndex > - 1)
            tabTitle = "(" + unreadMessageNumber + ")"
                        + tabTitle.substring(bracketIndex + 1);
        else
            tabTitle = "(" + unreadMessageNumber + ") " + tabTitle;

        this.setTitleAt(tabIndex, tabTitle);

        super.highlightTab(tabIndex);
    }
}
