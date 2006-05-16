/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.utils;

import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageProducer;

import javax.swing.GrayFilter;

/**
 * An image filter that "disables" an image by turning
 * it into a grayscale image, and brightening the pixels
 * in the image. Used by buttons to create an image for
 * a disabled button. Creates a more brighter image than 
 * the javax.swing.GrayFilter.
 * 
 * @author Yana Stamcheva
 */
public class LightGrayFilter extends GrayFilter {

    public LightGrayFilter(boolean b, int p) {
        super(b, p);
    }
    /**
     * Creates a disabled image
     */
    public static Image createDisabledImage (Image i) {
        LightGrayFilter filter = new LightGrayFilter(true, 65);
        ImageProducer prod = new FilteredImageSource(i.getSource(), filter);
        Image grayImage = Toolkit.getDefaultToolkit().createImage(prod);
        
        return grayImage;
    }    
}
