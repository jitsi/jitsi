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

import net.java.sip.communicator.util.swing.*;

/**
 * The SIPCommMenuItemUI implementation.
 * 
 * @author Yana Stamcheva
 */
public class SIPCommMenuItemUI
    extends BasicMenuItemUI
{
    public SIPCommMenuItemUI()
    {
        UIManager.put("MenuItem.selectionBackground", Color.WHITE);
        UIManager.put("MenuItem.background", Color.WHITE);
    }

    /**
     * Creates a new SIPCommMenuItemUI instance.
     */
    public static ComponentUI createUI(JComponent x)
    {
        return new SIPCommMenuItemUI();
    }

    private void internalPaintRollover(Graphics g, JMenuItem menuItem)
    {
        AntialiasingManager.activateAntialiasing(g);

        ButtonModel model = menuItem.getModel();
        Color oldColor = g.getColor();

        int menuWidth = menuItem.getWidth();
        int menuHeight = menuItem.getHeight();

        if (menuItem.isOpaque())
        {
            if (model.isArmed()
                || (menuItem instanceof JMenu && model.isSelected()))
            {
                g.setColor(SIPCommLookAndFeel.getControlDarkShadow());
                g.drawRoundRect(0, 0, menuWidth - 1, menuHeight - 1, 10, 10);
            }
            g.setColor(oldColor);
        }
    }

    protected void paintMenuItem (Graphics g, JComponent c, Icon
        checkIcon, Icon arrowIcon, Color background,
        Color foreground, int defaultTextIconGap)
    {
        super.paintMenuItem(g, c, null, arrowIcon, background, foreground, 0);

        this.internalPaintRollover(g, menuItem);
    }

    protected void paintText (Graphics g, JMenuItem menuItem, Rectangle
        textRect, String text)
    {
        textRect.x += 6;
        super.paintText(g, menuItem, textRect, text);
    }
}
