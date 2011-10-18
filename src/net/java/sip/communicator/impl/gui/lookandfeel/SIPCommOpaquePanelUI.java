/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.lookandfeel;

import java.awt.*;

import javax.swing.*;
import javax.swing.plaf.*;
import javax.swing.plaf.basic.*;

/**
 * @author Yana Stamcheva
 */
public class SIPCommOpaquePanelUI
    extends BasicPanelUI
{
    public static ComponentUI createUI(JComponent c)
    {
        return new SIPCommOpaquePanelUI();
    }

    public void paint(Graphics g, JComponent c)
    {
        super.paint(g, c);

        Color defaultColor = UIManager.getColor("Panel.background");
        int red = defaultColor.getRed();
        int green = defaultColor.getGreen();
        int blue = defaultColor.getBlue();

        g.setColor(new Color(red, green, blue));

        g.fillRect(0, 0, c.getWidth(), c.getHeight());
    }
}
