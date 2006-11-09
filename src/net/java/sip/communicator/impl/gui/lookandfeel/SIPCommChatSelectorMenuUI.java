/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.lookandfeel;

import java.awt.*;

import javax.swing.*;
import javax.swing.plaf.*;
import javax.swing.plaf.basic.*;

import net.java.sip.communicator.impl.gui.utils.*;

/**
 * The SIPCommChatSelectorMenuUI implementation.
 * 
 * @author Yana Stamcheva
 */
public class SIPCommChatSelectorMenuUI
    extends BasicMenuUI
{
    private Image menuBgImage
        = ImageLoader.getImage(ImageLoader.CHAT_TOOLBAR_BUTTON_BG);
    private Image menuRolloverImage
        = ImageLoader.getImage(ImageLoader.CHAT_TOOLBAR_ROLLOVER_BUTTON_BG);
            
    /**
     * Creates a new SIPCommChatSelectorMenuUI instance.
     */
    public static ComponentUI createUI(JComponent x)
    {
        return new SIPCommChatSelectorMenuUI();
    }

    public void paint(Graphics g, JComponent c)
    {
        AntialiasingManager.activateAntialiasing(g);
        super.paint(g, c);
    }

    /**
     * Draws the background of the menu item.
     * 
     * @param g the paint graphics
     * @param menuItem menu item to be painted
     * @param bgColor selection background color
     * @since 1.4
     */
    protected void paintBackground(Graphics g, JMenuItem menuItem, Color bgColor)
    {
        AntialiasingManager.activateAntialiasing(g);
        
        super.paintBackground(g, menuItem, bgColor);
        
        int menuWidth = menuItem.getWidth();
        int menuHeight = menuItem.getHeight();
        
        g.drawImage(menuBgImage, 0, 0, menuWidth, menuHeight, null);
    }
}
