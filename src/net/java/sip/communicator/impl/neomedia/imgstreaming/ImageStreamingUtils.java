/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
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
     * Get a scaled <tt>BufferedImage</tt>.
     *
     * Mainly inspired by:
     * http://java.developpez.com/faq/gui/?page=graphique_general_images
     * #GRAPHIQUE_IMAGE_redimensionner
     *
     * @param src source image
     * @param width width of scaled image
     * @param height height of scaled image
     * @param type <tt>BufferedImage</tt> type
     * @return scaled <tt>BufferedImage</tt>
     */
    public static BufferedImage getScaledImage(BufferedImage src,
                                               int width,
                                               int height,
                                               int type)
    {
        double scaleWidth = width / ((double)src.getWidth());
        double scaleHeight = height / ((double)src.getHeight());
        AffineTransform tx = new AffineTransform();

        // Skip rescaling if input and output size are the same.
        if ((Double.compare(scaleWidth, 1) != 0)
                || (Double.compare(scaleHeight, 1) != 0))
            tx.scale(scaleWidth, scaleHeight);

        AffineTransformOp op
            = new AffineTransformOp(tx, AffineTransformOp.TYPE_BILINEAR);
        BufferedImage dst = new BufferedImage(width, height, type);

        return op.filter(src, dst);
    }

    /**
     * Get raw bytes from ARGB <tt>BufferedImage</tt>.
     *
     * @param src ARGB <BufferImage</tt>
     * @param output output buffer, if not null and if its length is at least
     * image's (width * height) * 4, method will put bytes in it.
     * @return raw bytes or null if src is not an ARGB
     * <tt>BufferedImage</tt>
     */
    public static byte[] getImageBytes(BufferedImage src, byte output[])
    {
        if(src.getType() != BufferedImage.TYPE_INT_ARGB)
            throw new IllegalArgumentException("src.type");

        WritableRaster raster = src.getRaster();
        int width = src.getWidth();
        int height = src.getHeight();
        int size = width * height * 4;
        int off = 0;
        int pixel[] = new int[4];
        byte data[] = null;

        if(output == null || output.length < size)
        {
            /* allocate our bytes array */
            data = new byte[size];
        }
        else
        {
            /* use output */
            data = output;
        }

        for(int y = 0 ; y < height ; y++)
            for(int x = 0 ; x < width ; x++)
            {
                raster.getPixel(x, y, pixel);
                data[off++] = (byte)pixel[0];
                data[off++] = (byte)pixel[1];
                data[off++] = (byte)pixel[2];
                data[off++] = (byte)pixel[3];
            }

        return data;
    }
}
