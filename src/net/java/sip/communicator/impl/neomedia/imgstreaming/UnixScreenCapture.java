/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.imgstreaming;

import java.awt.*;
import java.awt.image.*;

/**
 * This class uses native code to capture
 * desktop screen.
 *
 * It should work on all OS with underlying X11.
 *
 * @author Sebastien Vincent
 */
public class UnixScreenCapture
{
    static
    {
        System.loadLibrary("screencapture");
    }

    /**
     * Capture desktop screen.
     * 
     * @param x x position to start capture
     * @param y y position to start capture
     * @param width capture width
     * @param height capture height
     * @return <tt>BufferedImage</tt> of the desktop screen
     */
    public static BufferedImage captureScreen(int x, int y, int width, int height)
    {
        DirectColorModel model = new DirectColorModel(24, 0xFF0000, 0x00FF00, 0xFF);
        int masks[] = {0xFF0000, 0xFF00, 0xFF};
        WritableRaster raster = null;
        DataBufferInt buffer = null;
        BufferedImage image = null;
        int data[] = null;

        data = grabScreen(x, y, width, height);

        if(data == null)
        {
            return null;
        }

        buffer = new DataBufferInt(data, data.length);
        raster = Raster.createPackedRaster(buffer, width, height, width, masks, null);
        image = new BufferedImage(model, raster, false, null);
        
        return image;
    }

    /**
     * Grab desktop screen and get ARGB pixels.
     * 
     * @param x x position to start capture
     * @param y y position to start capture
     * @param width capture width
     * @param height capture height
     * @return array of ARGB pixels
     */
    private static native int[] grabScreen(int x, int y, int width, int height);
}

