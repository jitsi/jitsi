/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
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
    GraphicsDevice screen = null;
    
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
        {
            return null;
        }

        devices = ge.getScreenDevices();
        
        if(devices == null || devices.length == 0)
        {
            return null;
        }
        
        screens = new ScreenDevice[devices.length];
        
        for(GraphicsDevice dev : devices)
        {
            /* we know that GraphicsDevice type is TYPE_RASTER_SCREEN */
            screens[i] = new ScreenDeviceImpl(dev);
            i++;
        }
        
        return screens;
    }
    
    /**
     * Constructor.
     *
     * @param screen screen device
     */
    protected ScreenDeviceImpl(GraphicsDevice screen)
    {
        this.screen = screen;
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
        return new Dimension(mode.getWidth(), mode.getHeight());
    }
}

