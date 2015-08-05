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
package net.java.sip.communicator.plugin.desktoputil.plaf;

import java.awt.*;

import javax.swing.*;
import javax.swing.plaf.*;
import javax.swing.plaf.basic.*;

import net.java.sip.communicator.plugin.desktoputil.*;

/**
 * The SIPCommLinkButtonUI implementation.
 * @author ROTH Damien
 */
public class SIPCommLinkButtonUI
    extends BasicButtonUI
{
    private static final SIPCommLinkButtonUI ui = new SIPCommLinkButtonUI();

    public static ComponentUI createUI(JComponent jcomponent)
    {
        return ui;
    }

    @Override
    protected void paintText(
            Graphics g, JComponent com, Rectangle rect, String s)
    {
        SIPCommLinkButton bn = (SIPCommLinkButton) com;

        ButtonModel bnModel = bn.getModel();
        if (bnModel.isEnabled())
        {
            if (bnModel.isPressed())
                bn.setForeground(bn.getActiveLinkColor());
            else if (bn.isLinkVisited())
                bn.setForeground(bn.getVisitedLinkColor());
           else
                bn.setForeground(bn.getLinkColor());
        }
        else
        {
            if (bn.getDisabledLinkColor() != null)
                bn.setForeground(bn.getDisabledLinkColor());
        }

        super.paintText(g, com, rect, s);
        int behaviour = bn.getLinkBehavior();

        if (!(behaviour == SIPCommLinkButton.HOVER_UNDERLINE
                && bnModel.isRollover())
            && behaviour != SIPCommLinkButton.ALWAYS_UNDERLINE)
                return;

        FontMetrics fm = g.getFontMetrics();
        int x = rect.x + getTextShiftOffset();
        int y = (rect.y + fm.getAscent()
                + fm.getDescent() + getTextShiftOffset()) - 1;
        if (bnModel.isEnabled())
        {
            g.setColor(bn.getForeground());
            g.drawLine(x, y, (x + rect.width) - 1, y);
        }
        else
        {
            g.setColor(bn.getBackground().brighter());
            g.drawLine(x, y, (x + rect.width) - 1, y);
        }
    }
}
