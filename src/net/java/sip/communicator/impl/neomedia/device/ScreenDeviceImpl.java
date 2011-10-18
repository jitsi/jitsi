/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.device;

import java.awt.*;

import net.java.sip.communicator.service.neomedia.device.*;

/**
 * Implementation of <tt>ScreenDevice</tt>.
 *
 * @author Sebastien Vincent
 */
public class ScreenDeviceImpl implements ScreenDevice
{
    /**
     * AWT <tt>GraphicsDevice</tt>.
     */
    final GraphicsDevice screen;

    /**
     * Screen index.
     */
    private final int index;

    /**
     * Returns all available <tt>ScreenDevice</tt> device.
     *
     * @return array of <tt>ScreenDevice</tt> device
     */
    public static ScreenDevice[] getAvailableScreenDevice()
    {
        ScreenDevice screens[] = null;
        GraphicsDevice devices[] = null;
        GraphicsEnvironment ge = null;
        int i = 0;

        try
        {
            ge = GraphicsEnvironment.
                getLocalGraphicsEnvironment();
        }
        catch(NoClassDefFoundError e)
        {
            ge = null;
        }

        if(ge == null)
            return null;

        devices = ge.getScreenDevices();

        if(devices == null || devices.length == 0)
            return null;

        screens = new ScreenDevice[devices.length];

        for(GraphicsDevice dev : devices)
        {
            /* we know that GraphicsDevice type is TYPE_RASTER_SCREEN */
            screens[i] = new ScreenDeviceImpl(i, dev);
            i++;
        }

        return screens;
    }

    /**
     * Constructor.
     *
     * @param index screen index
     * @param screen screen device
     */
    protected ScreenDeviceImpl(int index, GraphicsDevice screen)
    {
        this.index = index;
        this.screen = screen;
    }

    /**
     * Get the screen index.
     *
     * @return screen index
     */
    public int getIndex()
    {
        return index;
    }

    /**
     * Get current resolution of <tt>ScreenDevice</tt> device.
     *
     * @return current resolution of the screen
     */
    public Dimension getSize()
    {
        /* get current display resolution */
        DisplayMode mode = screen.getDisplayMode();

        return
            (mode == null)
                ? null
                : new Dimension(mode.getWidth(), mode.getHeight());
    }

    /**
     * Get the identifier of the screen.
     *
     * @return ID of the screen
     */
    public String getName()
    {
        return screen.getIDstring();
    }

    /**
     * If the screen contains specified point.
     *
     * @param p point coordinate
     * @return true if point belongs to screen, false otherwise
     */
    public boolean containsPoint(Point p)
    {
        return screen.getDefaultConfiguration().getBounds().contains(p);
    }

    /**
     * Get bounds of the screen.
     *
     * @return bounds of the screen
     */
    public Rectangle getBounds()
    {
        return screen.getDefaultConfiguration().getBounds();
    }
}
