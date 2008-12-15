/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.lookandfeel;

import java.awt.*;

import javax.swing.*;
import javax.swing.plaf.*;
import javax.swing.plaf.basic.*;

import net.java.sip.communicator.util.swing.*;

/**
 * The SIPCommMenuUI implementation.
 * 
 * @author Yana Stamcheva
 */
public class SIPCommStatusMenuUI
    extends BasicMenuUI
{
    /**
     * Creates a new SIPCommMenuUI instance.
     */
    public static ComponentUI createUI(JComponent x)
    {
        return new SIPCommStatusMenuUI();
    }

    /**
     * Draws the background of the menu.
     * 
     * @param g the paint graphics
     * @param menuItem menu item to be painted
     * @param bgColor selection background color
     * @since 1.4
     */
    protected void paintBackground(Graphics g, JMenuItem menuItem, Color bgColor)
    {
        super.paintBackground(g, menuItem, bgColor);

        g = g.create();
        try
        {
            internalPaintBackground(g, menuItem, bgColor);
        }
        finally
        {
            g.dispose();
        }
    }

    private void internalPaintBackground(Graphics g, JMenuItem menuItem,
        Color bgColor)
    {
        AntialiasingManager.activateAntialiasing(g);

        Color oldColor = g.getColor();

        int menuWidth = menuItem.getWidth();
        int menuHeight = menuItem.getHeight();

        if (menuItem.isSelected())
        {
            g.setColor(Color.LIGHT_GRAY);
            g.fillRoundRect(0, 0, menuWidth, menuHeight, 8, 8);
        }

        g.setColor(oldColor);
    }
}
