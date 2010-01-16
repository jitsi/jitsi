/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.imgstreaming;

import java.awt.geom.*;
import java.awt.image.*;

/**
 * This class provides utility functions and fields for image streaming.
 *
 * @author Sebastien Vincent
 */
public class ImageStreamingUtils
{
    /**
     * The locator prefix used when creating or parsing <tt>MediaLocator</tt>s.
     */
    public static final String LOCATOR_PREFIX = "imgstreaming";

    /**
     * Get a scaled <tt>BufferedImage</tt>.
     *
     * Mainly inspired by:
     * http://java.developpez.com/faq/gui/?page=graphique_general_images
     * #GRAPHIQUE_IMAGE_redimensionner
     *
     * @param src source image
     * @param width width of scaled image
     * @param height height of scaled image
     * @return scaled <tt>BufferedImage</tt>
     */
    public static BufferedImage getScaledImage(BufferedImage src,
                                               int width,
                                               int height,
                                               int type)
    {
        AffineTransform tx = new AffineTransform();
        AffineTransformOp op = null;
        double scaleWidth = ((double)width) / ((double)src.getWidth());
        double scaleHeight = ((double)height) / ((double)src.getHeight());
        BufferedImage dst = null;

        /* skip rescaling if input and output size are the same */
        if(scaleWidth != 1 || scaleHeight != 1)
        {
            tx.scale(scaleWidth, scaleHeight);
        }

        op = new AffineTransformOp(tx, AffineTransformOp.TYPE_BILINEAR);
        dst = new BufferedImage(width, height, type);

        return op.filter(src, dst);
    }

    /**
     * Get raw bytes from ARGB <tt>BufferedImage</tt>.
     *
     * @param src ARGB <BufferImage</tt>
     * @return raw bytes or null if src is not an ARGB
     * <tt>BufferedImage</tt>
     */
    public static byte[] getImageByte(BufferedImage src)
    {
        if(src.getType() != BufferedImage.TYPE_INT_ARGB)
        {
            return null;
        }

        WritableRaster raster = src.getRaster();
        byte data[] = null;
        int pixel[] = new int[4];
        int width = src.getWidth();
        int height = src.getHeight();
        int off = 0;

        /* allocate our bytes array */
        data = new byte[width * height * 4];
        
        for(int y = 0 ; y < height ; y++)
        {
            for(int x = 0 ; x < width ; x++)
            {
                raster.getPixel(x, y, pixel);
                data[off++] = (byte)pixel[0];
                data[off++] = (byte)pixel[1];
                data[off++] = (byte)pixel[2];
                data[off++] = (byte)pixel[3];
            }
        }

        raster = null;
        pixel = null;
        return data;
    }
}
