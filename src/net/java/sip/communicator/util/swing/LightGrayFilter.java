/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.util.swing;

import java.awt.*;
import java.awt.image.*;

import javax.swing.*;

/**
 * An image filter that "disables" an image by turning
 * it into a grayscale image, and brightening the pixels
 * in the image. Used by buttons to create an image for
 * a disabled button. Creates a more brighter image than
 * the javax.swing.GrayFilter.
 *
 * @author Yana Stamcheva
 */
public class LightGrayFilter extends GrayFilter
{
    /**
     * Creates an instance of a LightGrayFilter.
     * @param b  a boolean -- true if the pixels should be brightened
     * @param p  an int in the range 0..100 that determines the percentage
     *           of gray, where 100 is the darkest gray, and 0 is the lightest
     */
    public LightGrayFilter(boolean b, int p)
    {
        super(b, p);
    }

    /**
     * Creates a disabled image.
     * @param i The source image.
     * @return A disabled image based on the source image.
     */
    public static Image createDisabledImage(Image i)
    {
        LightGrayFilter filter = new LightGrayFilter(true, 50);
        ImageProducer prod = new FilteredImageSource(i.getSource(), filter);
        Image grayImage = Toolkit.getDefaultToolkit().createImage(prod);

        return grayImage;
    }
}
