/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.util.swing;

import java.awt.*;

/**
 * A class which reads the screen bounds once and provides this information
 * 
 * @author Thomas Hofer
 * 
 */
public class ScreenInformation
{
    private static Rectangle screenBounds = null;

    public static synchronized void init()
    {
        if (screenBounds == null)
        {
            final GraphicsEnvironment ge = GraphicsEnvironment
                    .getLocalGraphicsEnvironment();
            final GraphicsDevice[] gs = ge.getScreenDevices();

            screenBounds = new Rectangle();

            if (gs.length > 1)
            {

                // create a thread for each display, as the query is very slow
                Thread thread[] = new Thread[gs.length];
                for (int j = 0; j < gs.length; j++)
                {
                    final int j1 = j;
                    thread[j] = new Thread(new Runnable()
                    {
                        public void run()
                        {
                            Rectangle screenDeviceBounds = new Rectangle();
                            GraphicsDevice gd = gs[j1];
                            GraphicsConfiguration[] gc = gd.getConfigurations();
                            for (int i = 0; i < gc.length; i++)
                            {
                                screenDeviceBounds = screenDeviceBounds
                                        .union(gc[i].getBounds());
                            }
                            screenBounds.setBounds(screenBounds
                                    .union(screenDeviceBounds));

                        }
                    });
                    thread[j].start();
                }
                for (int j = 0; j < gs.length; j++)
                {
                    // wait for all threads here
                    try
                    {
                        thread[j].join();
                    } catch (InterruptedException e)
                    {
                    }
                }
            } else
            {
                // only one display, get the screen size directy. this method
                // is much faster, but can only handle the primary display
                Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
                screenBounds.setBounds(0, 0, dim.width, dim.height);
            }

        }
    }

    public static synchronized Rectangle getScreenBounds()
    {
        // the initialization needs a moment
        // prevent a concurrent initalization
        if (screenBounds == null)
        {
            init();
        }

        return screenBounds;
    }
}
