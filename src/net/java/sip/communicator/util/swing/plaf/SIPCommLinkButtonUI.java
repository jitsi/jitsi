/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.util.swing.plaf;

import net.java.sip.communicator.util.swing.*;

import java.awt.*;

import javax.swing.*;
import javax.swing.plaf.*;
import javax.swing.plaf.basic.*;

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
