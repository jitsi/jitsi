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

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.util.skin.*;

/**
 * The SIPCommChatSelectorMenuUI implementation.
 *
 * @author Yana Stamcheva
 * @author Adam Netocny
 */
public class SIPCommChatSelectorMenuUI
    extends BasicMenuUI
    implements Skinnable
{
    private Image menuBgImage
        = ImageLoader.getImage(ImageLoader.CHAT_TOOLBAR_BUTTON_BG);

    /**
     * Creates a new SIPCommChatSelectorMenuUI instance.
     *
     * @param x the component for which this UI is created
     * @return the component UI
     */
    public static ComponentUI createUI(JComponent x)
    {
        return new SIPCommChatSelectorMenuUI();
    }

    /**
     * Draws the background of the menu item.
     *
     * @param g the paint graphics
     * @param menuItem menu item to be painted
     * @param bgColor selection background color
     * @since 1.4
     */
    @Override
    protected void paintBackground( Graphics g,
                                    JMenuItem menuItem,
                                    Color bgColor)
    {
        super.paintBackground(g, menuItem, bgColor);

        boolean isToolBarExtended =
            new Boolean(GuiActivator.getResources().getSettingsString(
                "impl.gui.IS_TOOLBAR_EXTENDED")).booleanValue();

        if (!isToolBarExtended)
        {
            g = g.create();
            try
            {
                AntialiasingManager.activateAntialiasing(g);

                int menuWidth = menuItem.getWidth();
                int menuHeight = menuItem.getHeight();

                g.drawImage(menuBgImage, 0, 0, menuWidth, menuHeight, null);
            }
            finally
            {
                g.dispose();
            }
        }
    }

    /**
     * Loads skin.
     */
    public void loadSkin()
    {
        menuBgImage
            = ImageLoader.getImage(ImageLoader.CHAT_TOOLBAR_BUTTON_BG);
    }
}
