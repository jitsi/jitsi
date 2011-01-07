/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chat;

import java.awt.*;
import java.awt.event.*;

import javax.swing.event.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.call.*;
import net.java.sip.communicator.util.skin.*;
import net.java.sip.communicator.util.swing.*;
import net.java.sip.communicator.util.swing.event.*;

/**
 * The <tt>ConversationTabbedPane</tt> is a <tt>SIPCommTabbedPane</tt> that
 * takes into account the number of unread messages received in a certain tab
 * and shows this information to the user.
 * 
 * @author Yana Stamcheva
 * @author Adam Netocny
 */
public class ConversationTabbedPane
    extends SIPCommTabbedPane
    implements Skinnable
{
    /**
     * Creates a <tt>ChatTabbedPane</tt> with a close tab function.
     */
    public ConversationTabbedPane()
    {
        super(true, false);

        addCloseListener(new CloseListener()
        {
            public void closeOperation(MouseEvent e)
            {
                int tabIndex = getOverTabIndex();
                Component c = getComponentAt(tabIndex);

                if (c instanceof ChatPanel)
                    GuiActivator.getUIService()
                        .getChatWindowManager().closeChat((ChatPanel) c);
                else if (c instanceof CallPanel)
                    CallManager.hangupCall(((CallPanel) c).getCall());
            }
        });

        addChangeListener(new ChangeListener()
        {
            public void stateChanged(ChangeEvent evt)
            {
                int index = getSelectedIndex();

                // If there's no chat panel selected we do nothing.
                if (index > 0)
                {
                    Component c = getComponentAt(index);

                    if (c instanceof ChatPanel)
                        GuiActivator.getUIService().getChatWindowManager()
                            .removeNonReadChatState((ChatPanel) c);
                }
            }
        });
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

        Component c = this.getComponentAt(tabIndex);

        if (c instanceof ChatPanel)
        {
            ChatPanel chatPanel = (ChatPanel) c;

            int unreadMessageNumber = chatPanel.unreadMessageNumber;

            if (unreadMessageNumber > 0)
            {
                String tabTitle = chatPanel.getChatSession().getChatName();
                this.setTitleAt(tabIndex, tabTitle);
            }

            chatPanel.unreadMessageNumber = 0;
        }

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
        Component c = this.getComponentAt(tabIndex);

        String tabTitle = "";
        if (c instanceof ChatPanel)
            tabTitle = ((ChatPanel) c).getChatSession().getChatName();
        else if (c instanceof CallPanel)
            tabTitle = ((CallPanel) c).getCallTitle();

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
            Component c = this.getComponentAt(i);

            if (c instanceof ChatPanel)
                setIconAt(i,
                    ((ChatPanel) c).getChatSession().getChatStatusIcon());
        }
    }
}
