/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.customcontrols;

import java.awt.*;

/**
 * A class which reads the screen bounds once and provides this information
 * 
 * @author Thomas Hofer
 * 
 */
class ScreenInformation
{
    private static Rectangle screenBounds = null;

    static synchronized Rectangle getScreenBounds()
    {
        // the initialization needs a moment
        // prevent a concurrent initalization

        if (screenBounds == null)
        {
            screenBounds = new Rectangle();
            GraphicsEnvironment ge = GraphicsEnvironment
                    .getLocalGraphicsEnvironment();
            GraphicsDevice[] gs = ge.getScreenDevices();
            for (int j = 0; j < gs.length; j++)
            {
                GraphicsDevice gd = gs[j];
                GraphicsConfiguration[] gc = gd.getConfigurations();
                for (int i = 0; i < gc.length; i++)
                {
                    screenBounds = screenBounds.union(gc[i].getBounds());
                }
            }
        }
        return screenBounds;
    }
}
