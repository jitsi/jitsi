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

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.Container;

/**
 * The <tt>ExtendedCallHistorySearchButton</tt> is the button that will be
 * added in the Call List panel and from which the user would be able to access
 * the <tt>ExtendCallHistorySearchDialog</tt>.
 * 
 * @author Bourdon Maxime & Meyer Thomas
 */
public class ExtendedCallHistorySearchItem
    extends AbstractPluginComponent
    implements ActionListener
{
    private final JMenuItem historyMenuItem
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
        super(Container.CONTAINER_TOOLS_MENU);

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

            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            callHistorySearchDialog
                .setLocation(
                    screenSize.width / 2
                        - callHistorySearchDialog.getWidth() / 2,
                    screenSize.height / 2
                        - callHistorySearchDialog.getHeight() / 2);
        }

        callHistorySearchDialog.loadHistoryCalls();

        callHistorySearchDialog.setVisible(true);
    }

    public Object getComponent()
    {
        return historyMenuItem;
    }

    public String getName()
    {
        return historyMenuItem.getText();
    }
}
