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
 * SIPCommToolBarSeparatorUI implementation.
 *
 * @author Yana Stamcheva
 */
public class SIPCommToolBarSeparatorUI extends BasicToolBarSeparatorUI {

    public static ComponentUI createUI(JComponent c) {
        return new SIPCommToolBarSeparatorUI();
    }

    @Override
    public void paint(Graphics g, JComponent c)
    {
        Graphics2D g2 = (Graphics2D)g;

        g2.setColor(UIManager.getColor("ToolBar.separatorColor"));
        g2.drawLine(c.getWidth()/2, 0, c.getWidth()/2, c.getHeight());
    }
}
