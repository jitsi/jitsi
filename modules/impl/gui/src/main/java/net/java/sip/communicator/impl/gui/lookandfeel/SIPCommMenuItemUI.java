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
package net.java.sip.communicator.impl.gui.lookandfeel;

import java.awt.*;

import javax.swing.*;
import javax.swing.plaf.*;
import javax.swing.plaf.basic.*;

import net.java.sip.communicator.plugin.desktoputil.*;

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

    @Override
    protected void paintMenuItem (Graphics g, JComponent c, Icon
        checkIcon, Icon arrowIcon, Color background,
        Color foreground, int defaultTextIconGap)
    {
        super.paintMenuItem(g, c, null, arrowIcon, background, foreground, 0);

        this.internalPaintRollover(g, menuItem);
    }

    @Override
    protected void paintText (Graphics g, JMenuItem menuItem, Rectangle
        textRect, String text)
    {
        textRect.x += 6;
        super.paintText(g, menuItem, textRect, text);
    }
}
