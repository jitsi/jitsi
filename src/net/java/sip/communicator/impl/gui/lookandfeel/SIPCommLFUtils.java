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

import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.plugin.desktoputil.*;

/**
 * The <tt>SIPCommLFUtils</tt> is an utility class helping in drawing some
 *
 * @author Yana Stamcheva
 */
public class SIPCommLFUtils
{
    /**
     * Draws the "Round Border" which is used throughout the SIPComm L&F
     * @param g the <tt>Graphics</tt> object used for painting
     * @param x the x coordinate to start the border
     * @param y the y coordinate to start the border
     * @param w the width of the border
     * @param h the height of the border
     * @param r1 the arc width
     * @param r2 the arc height
     */
    static void drawRoundBorder(Graphics g, int x, int y, int w,
        int h, int r1, int r2)
    {
        AntialiasingManager.activateAntialiasing(g);
        Graphics2D g2 = (Graphics2D) g.create();
        try
        {
            g2.setColor(SIPCommLookAndFeel.getControlDarkShadow());

            g2.drawRoundRect(0, 0, w - 1, h - 1, r1, r2);
        }
        finally
        {
            g2.dispose();
        }
    }

   /**
     * Draws the "Round Disabled Border" which is used throughout the SIPComm
     * L&F.
     * @param g the <tt>Graphics</tt> object used for painting
     * @param x the x coordinate to start the border
     * @param y the y coordinate to start the border
     * @param w the width of the border
     * @param h the height of the border
     * @param r1 the arc width
     * @param r2 the arc height
     */
    static void drawRoundDisabledBorder(Graphics g, int x, int y, int w, int h,
        int r1, int r2)
    {
        AntialiasingManager.activateAntialiasing(g);
        Graphics2D g2 = (Graphics2D) g.create();
        try
        {
            g2.setColor(SIPCommLookAndFeel.getControlShadow());

            g2.drawRoundRect(0, 0, w - 1, h - 1, r1, r2);
        }
        finally
        {
            g.dispose();
        }
    }

   /**
     * Draws the "Bold Round Disabled Border" which is used throughout the
     * SIPComm L&F.
     * @param g the <tt>Graphics</tt> object used for painting
     * @param x the x coordinate to start the border
     * @param y the y coordinate to start the border
     * @param w the width of the border
     * @param h the height of the border
     * @param r1 the arc width
     * @param r2 the arc height
     */
    static void drawBoldRoundBorder(Graphics g, int x, int y, int w, int h,
        int r1, int r2)
    {
        g = g.create();
        try
        {
            AntialiasingManager.activateAntialiasing(g);

            Graphics2D g2 = (Graphics2D) g;

            g2.setColor(Constants.BORDER_COLOR);
            g2.setStroke(new BasicStroke(1.5f));

            g2.drawRoundRect(x, y, w - 1, h - 1, r1, r2);
        }
        finally
        {
            g.dispose();
        }
    }
}
