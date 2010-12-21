/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chat;

import java.awt.*;

import javax.swing.*;

import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The <tt>StatusPanel</tt> is the panel shown on the bottom of the chat
 * window and containing the typing notification messages.
 * 
 * @author Yana Stamcheva
 */
public class StatusPanel extends TransparentPanel
{
    private final JLabel statusLabel = new JLabel();

    public StatusPanel()
    {
        super(new BorderLayout());

        this.setPreferredSize(new Dimension(100, getFontHeight() + 5));

        this.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));

        this.add(statusLabel, BorderLayout.WEST);
    }

    /**
     * Sets the message text to the status panel in the bottom of the chat
     * window. Used to show typing notification messages, links' hrefs, etc.
     *
     * @param statusMessage The message text to be displayed.
     */
    public void setStatusMessage(String statusMessage)
    {
        int stringWidth = GuiUtils.getStringWidth(statusLabel, statusMessage);

        final int dot3 = GuiUtils.getStringWidth(statusLabel, "... ");

        // first, we avoid to loop if it is useless.
        final int statusPanelWidth = this.getWidth();
        if (dot3 >= statusPanelWidth)
        {
            if (stringWidth > dot3)
                statusMessage = "...";
        }
        else
        {
            while ((stringWidth > (statusPanelWidth - dot3))
                    && !statusMessage.equals("..."))
            {
                if (statusMessage.endsWith("..."))
                {
                    statusMessage = statusMessage.substring(0,
                        statusMessage.indexOf("...") - 1).concat("...");
                }
                else
                {
                    statusMessage = statusMessage.substring(0,
                        statusMessage.length() - 3).concat("...");
                }
                stringWidth
                    = GuiUtils.getStringWidth(statusLabel, statusMessage);
            }
        }
        statusLabel.setText(statusMessage);
    }

    /**
     * Returns the height of the default status label font.
     * 
     * @return the height of the default status label font.
     */
    private int getFontHeight()
    {
        FontMetrics statusLabelFontMetrics
            = statusLabel.getFontMetrics(statusLabel.getFont());

        return statusLabelFontMetrics.getMaxAscent()
                + statusLabelFontMetrics.getMaxDescent();
    }
}
