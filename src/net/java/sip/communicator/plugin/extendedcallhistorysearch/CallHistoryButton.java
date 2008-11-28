/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.plugin.extendedcallhistorysearch;

import java.awt.*;

import net.java.sip.communicator.service.gui.*;

/**
 * Tha button that will open the call history through the favorites menu.
 * 
 * @author Yana Stamcheva
 */
public class CallHistoryButton
    implements FavoritesButton
{
    byte[] buttonIcon = ExtendedCallHistorySearchActivator.getResources()
        .getImageInBytes("plugin.callhistorysearch.HISTORY_BUTTON");

    public void actionPerformed()
    {
        ExtendedCallHistorySearchDialog callHistorySearchDialog
            = new ExtendedCallHistorySearchDialog();

        callHistorySearchDialog.setLocation(Toolkit.getDefaultToolkit()
            .getScreenSize().width
            / 2 - callHistorySearchDialog.getWidth() / 2, Toolkit
            .getDefaultToolkit().getScreenSize().height
            / 2 - callHistorySearchDialog.getHeight() / 2);

        callHistorySearchDialog.loadHistoryCalls();

        callHistorySearchDialog.setVisible(true);
    }

    public byte[] getImage()
    {
        return ExtendedCallHistorySearchActivator.getResources()
        .getImageInBytes("plugin.callhistorysearch.HISTORY_BUTTON");
    }

    public String getText()
    {
        return ExtendedCallHistorySearchActivator.getResources()
            .getI18NString("history");
    }
}
