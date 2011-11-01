/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.util.swing;

import java.awt.*;

/**
 * A class which reads the screen bounds and provides this information.
 * 
 * @author Ingo Bauersachs
 */
public class ScreenInformation
{
    /**
     * Calculates the bounding box of all available screens. This method is
     * highly inaccurate when screens of different sizes are used or not evenly
     * aligned. A correct implementation should generate a polygon.
     * 
     * @return A polygon of the usable screen area.
     */
    public static Rectangle getScreenBounds()
    {
        final GraphicsEnvironment ge = GraphicsEnvironment
                .getLocalGraphicsEnvironment();

        Rectangle bounds = new Rectangle();
        for(GraphicsDevice gd : ge.getScreenDevices())
        {
            GraphicsConfiguration gc = gd.getDefaultConfiguration();
            bounds = bounds.union(gc.getBounds());
        }
        return bounds;
    }

    /**
     * Checks whether the top edge of the rectangle is contained in any of the
     * available screens.
     * 
     * @param window The bounding box of the window.
     * @return True when the top edge is in a visible screen area; false
     *         otherwise
     */
    public static boolean isTitleOnScreen(Rectangle window)
    {
        final GraphicsEnvironment ge = GraphicsEnvironment
            .getLocalGraphicsEnvironment();

        boolean leftInside = false;
        boolean rightInside = false;
        Point left = new Point(window.x, window.y);
        Point right = new Point(window.x, window.y + window.width);
        for(GraphicsDevice gd : ge.getScreenDevices())
        {
            GraphicsConfiguration gc = gd.getDefaultConfiguration();
            if(gc.getBounds().contains(left))
                leftInside = true;
            if(gc.getBounds().contains(right))
                rightInside = true;
            if(leftInside && rightInside)
                return true;
        }
        return leftInside && rightInside;
    }
}
