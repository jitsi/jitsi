/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.lookandfeel;

import java.awt.*;

import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.util.swing.*;

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
