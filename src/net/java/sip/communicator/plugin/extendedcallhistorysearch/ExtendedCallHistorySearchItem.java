/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.plugin.extendedcallhistorysearch;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

/**
 * The <tt>ExtendedCallHistorySearchButton</tt> is the button that will be
 * added in the Call List panel and from which the user would be able to access
 * the <tt>ExtendCallHistorySearchDialog</tt>.
 * 
 * @author Bourdon Maxime & Meyer Thomas
 */
public class ExtendedCallHistorySearchItem
    extends JMenuItem
    implements
    ActionListener
{
    private ExtendedCallHistorySearchDialog callHistorySearchDialog = null;

    /**
     * Creates an instance of <tt>ExtendedCallHistoryButton</tt>.
     */
    public ExtendedCallHistorySearchItem()
    {
        super(Resources.getString("advancedCallHistorySearch"));
        
        this.setMnemonic(Resources.getMnemonic("advancedCallHistorySearch"));
        this.addActionListener(this);
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
}
