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
package net.java.sip.communicator.plugin.desktoputil;

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
        Point topLeft = new Point(window.x, window.y);
        Point topRight = new Point(window.x + window.width, window.y);
        for(GraphicsDevice gd : ge.getScreenDevices())
        {
            GraphicsConfiguration gc = gd.getDefaultConfiguration();
            if(gc.getBounds().contains(topLeft))
                leftInside = true;
            if(gc.getBounds().contains(topRight))
                rightInside = true;
            if(leftInside && rightInside)
                return true;
        }
        return leftInside && rightInside;
    }
}
