/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.plugin.callhistoryform;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.Container;
import net.java.sip.communicator.service.protocol.*;

/**
 * The <tt>ExtendedCallHistorySearchButton</tt> is the button that will be
 * added in the Call List panel and from which the user would be able to access
 * the <tt>ExtendCallHistorySearchDialog</tt>.
 * 
 * @author Bourdon Maxime & Meyer Thomas
 */
public class ExtendedCallHistorySearchItem
    implements  ActionListener,
                PluginComponent
{
    private JMenuItem historyMenuItem
        = new JMenuItem(ExtendedCallHistorySearchActivator.getResources()
                .getI18NString("plugin.callhistoryform.TITLE"),
            ExtendedCallHistorySearchActivator.getResources()
                .getImage("plugin.callhistorysearch.HISTORY_MENU_ICON"));

    private ExtendedCallHistorySearchDialog callHistorySearchDialog = null;

    /**
     * Creates an instance of <tt>ExtendedCallHistoryButton</tt>.
     */
    public ExtendedCallHistorySearchItem()
    {
        this.historyMenuItem.setMnemonic(
            ExtendedCallHistorySearchActivator.getResources()
                .getI18nMnemonic("plugin.callhistoryform.TITLE"));
        this.historyMenuItem.addActionListener(this);
    }
 
    /**
     * Launches the extended call history dialog when user clicks on this button.
     */
    public void actionPerformed(ActionEvent e)
    {
        if (callHistorySearchDialog == null)
        {
            callHistorySearchDialog = new ExtendedCallHistorySearchDialog();
            callHistorySearchDialog.setLocation(Toolkit.getDefaultToolkit()
                .getScreenSize().width
                / 2 - callHistorySearchDialog.getWidth() / 2, Toolkit
                .getDefaultToolkit().getScreenSize().height
                / 2 - callHistorySearchDialog.getHeight() / 2);
        }

        callHistorySearchDialog.loadHistoryCalls();

        callHistorySearchDialog.setVisible(true);
    }

    public Object getComponent()
    {
        return historyMenuItem;
    }

    public String getConstraints()
    {
        return null;
    }

    public Container getContainer()
    {
        return Container.CONTAINER_TOOLS_MENU;
    }

    public String getName()
    {
        return historyMenuItem.getText();
    }

    public void setCurrentContact(Contact contact)
    {
    }
    
    public void setCurrentContact(MetaContact metaContact)
    {
    }

    public void setCurrentContactGroup(MetaContactGroup metaGroup)
    {
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
