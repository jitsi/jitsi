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
package net.java.sip.communicator.impl.gui.main.chat;

import java.awt.*;

import javax.swing.*;

import net.java.sip.communicator.plugin.desktoputil.*;

/**
 * The <tt>StatusPanel</tt> is the panel shown on the bottom of the chat
 * window and containing the typing notification messages.
 *
 * @author Yana Stamcheva
 */
public class StatusPanel
    extends TransparentPanel
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
        int stringWidth
            = ComponentUtils.getStringWidth(statusLabel, statusMessage);

        final int dot3 = ComponentUtils.getStringWidth(statusLabel, "... ");

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
                    = ComponentUtils.getStringWidth(statusLabel, statusMessage);
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
