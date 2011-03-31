/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.util;

import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.net.*;

import javax.imageio.*;
import javax.swing.*;

import net.java.sip.communicator.util.swing.*;

/**
 * Utility methods for image manipulation.
 *
 * @author Sebastien Mazy
 * @author Yana Stamcheva
 * @author Lubomir Marinov
 */
public class ImageUtils
{
    /**
     * The <tt>Logger</tt> used by the <tt>ImageUtils</tt> class for logging
     * output.
     */
    private static final Logger logger = Logger.getLogger(ImageUtils.class);

    /**
     * Returns a scaled image fitting within the given bounds while keeping the
     * aspect ratio.
     *
     * @param image the image to scale
     * @param width maximum width of the scaled image
     * @param height maximum height of the scaled image
     * @return the scaled image
     */
    public static Image scaleImageWithinBounds( Image image,
                                                int width,
                                                int height)
    {
        int initialWidth = image.getWidth(null);
        int initialHeight = image.getHeight(null);

        Image scaledImage;
        int scaleHint = Image.SCALE_SMOOTH;
        double originalRatio =
            (double) initialWidth / initialHeight;
        double areaRatio = (double) width / height;

        if(originalRatio > areaRatio)
            scaledImage = image.getScaledInstance(width, -1, scaleHint);
        else
            scaledImage = image.getScaledInstance(-1, height, scaleHint);
        return scaledImage;
    }

    /**
     * Scales the given <tt>image</tt> to fit in the given <tt>width</tt> and
     * <tt>height</tt>.
     * @param image the image to scale
     * @param width the desired width
     * @param height the desired height
     * @return the scaled image
     */
    public static ImageIcon scaleIconWithinBounds(Image image, int width,
        int height)
    {
        return new ImageIcon(scaleImageWithinBounds(image, width, height));
    }

    /**
     * Scales the given <tt>image</tt> to fit in the given <tt>width</tt> and
     * <tt>height</tt>.
     *
     * @param imageBytes the bytes of the image to scale
     * @param width the desired width
     * @param height the desired height
     * @return the scaled image
     */
    public static ImageIcon scaleImageWithinBounds( byte[] imageBytes,
                                                    int width,
                                                    int height)
    {

        if (imageBytes == null || !(imageBytes.length > 0))
            return null;

        Image imageIcon = null;

        try
        {
            Image image = null;

            // sometimes ImageIO fails, will fall back to awt Toolkit
            try
            {
                image = ImageIO.read(new ByteArrayInputStream(imageBytes));
            } catch (Exception e)
            {
                try
                {
                    image = Toolkit.getDefaultToolkit().createImage(imageBytes);
                } catch (Exception e1)
                {
                    // if it fails throw the original exception
                    throw e;
                }
            }
            if(image != null)
                imageIcon = scaleImageWithinBounds(image, width, height);
            else
                if (logger.isTraceEnabled())
                    logger.trace("Unknown image format or error reading image");
        }
        catch (Exception e)
        {
            if (logger.isDebugEnabled())
                logger.debug("Could not create image.", e);
        }

        if (imageIcon != null)
            return new ImageIcon(imageIcon);

        return null;
    }

    /**
     * Creates a rounded avatar image.
     *
     * @param image image of the initial avatar image.
     * @param width the desired width
     * @param height the desired height
     * @return The rounded corner image.
     */
    public static Image getScaledRoundedImage(  Image image,
                                                int width,
                                                int height)
    {
        ImageIcon scaledImage =
            ImageUtils.scaleIconWithinBounds(image, width, height);
        int scaledImageWidth = scaledImage.getIconWidth();
        int scaledImageHeight = scaledImage.getIconHeight();

        if(scaledImageHeight <= 0 ||
           scaledImageWidth <= 0)
            return null;

        // Just clipping the image would cause jaggies on Windows and Linux.
        // The following is a soft clipping solution based on the solution
        // proposed by Chris Campbell:
        // http://java.sun.com/mailers/techtips/corejava/2006/tt0923.html
        BufferedImage destImage
            = new BufferedImage(scaledImageWidth, scaledImageHeight,
                BufferedImage.TYPE_INT_ARGB);

        Graphics2D g = destImage.createGraphics();

        try
        {
            // Render our clip shape into the image.  Note that we enable
            // antialiasing to achieve the soft clipping effect.
            g.setComposite(AlphaComposite.Src);
            AntialiasingManager.activateAntialiasing(g);
            g.setColor(Color.WHITE);
            g.fillRoundRect(0, 0, scaledImageWidth, scaledImageHeight, 15, 15);

            // We use SrcAtop, which effectively uses the
            // alpha value as a coverage value for each pixel stored in the
            // destination.  For the areas outside our clip shape, the
            // destination alpha will be zero, so nothing is rendered in those
            // areas. For the areas inside our clip shape, the destination alpha
            // will be fully opaque, so the full color is rendered. At the edges,
            // the original antialiasing is carried over to give us the desired
            // soft clipping effect.
            g.setComposite(AlphaComposite.SrcAtop);
            g.drawImage(scaledImage.getImage(), 0, 0, null);
        }
        finally
        {
            g.dispose();
        }
        return destImage;
    }

    /**
     * Returns a scaled instance of the given <tt>image</tt>.
     * @param image the image to scale
     * @param width the desired width
     * @param height the desired height
     * @return a byte array containing the scaled image
     */
    public static byte[] getScaledInstanceInBytes(
        Image image, int width, int height)
    {
        byte[] scaledBytes = null;

        BufferedImage scaledImage
            = (BufferedImage) getScaledRoundedImage(image, width, height);

        if (scaledImage != null)
        {
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();

            try
            {
                ImageIO.write(scaledImage, "png", outStream);
                scaledBytes = outStream.toByteArray();
            }
            catch (IOException e)
            {
                if (logger.isDebugEnabled())
                    logger.debug("Could not scale image in bytes.", e);
            }

        }

        return scaledBytes;
    }

    /**
     * Returns a scaled rounded icon from the given <tt>image</tt>, scaled
     * within the given <tt>width</tt> and <tt>height</tt>.
     * @param image the image to scale
     * @param width the maximum width of the scaled icon
     * @param height the maximum height of the scaled icon
     * @return a scaled rounded icon
     */
    public static ImageIcon getScaledRoundedIcon(Image image, int width,
        int height)
    {
        Image scaledImage = getScaledRoundedImage(image, width, height);

        if (scaledImage != null)
            return new ImageIcon(scaledImage);

        return null;
    }

    /**
     * Creates a rounded corner scaled image.
     *
     * @param imageBytes The bytes of the image to be scaled.
     * @param width The maximum width of the scaled image.
     * @param height The maximum height of the scaled image.
     *
     * @return The rounded corner scaled image.
     */
    public static ImageIcon getScaledRoundedIcon(  byte[] imageBytes,
                                                    int width,
                                                    int height)
    {
        if (imageBytes == null || !(imageBytes.length > 0))
            return null;

        ImageIcon imageIcon = null;

        try
        {
            Image image = null;

            // sometimes ImageIO fails, will fall back to awt Toolkit
            try
            {
                image = ImageIO.read(new ByteArrayInputStream(imageBytes));
            } catch (Exception e)
            {
                try
                {
                    image = Toolkit.getDefaultToolkit().createImage(imageBytes);
                } catch (Exception e1)
                {
                    // if it fails throw the original exception
                    throw e;
                }
            }
            if(image != null)
                imageIcon = getScaledRoundedIcon(image, width, height);
            else
                if (logger.isTraceEnabled())
                    logger.trace("Unknown image format or error reading image");
        }
        catch (Exception e)
        {
            if (logger.isDebugEnabled())
                logger.debug("Could not create image.", e);
        }

        return imageIcon;
    }

    /**
     * Returns the buffered image corresponding to the given url image path.
     *
     * @param imagePath the path indicating, where we can find the image.
     *
     * @return the buffered image corresponding to the given url image path.
     */
    public static BufferedImage getBufferedImage(URL imagePath)
    {
        BufferedImage image = null;

        if (imagePath != null)
        {
            try
            {
                image = ImageIO.read(imagePath);
            }
            catch (IOException ex)
            {
                if (logger.isDebugEnabled())
                    logger.debug("Failed to load image:" + imagePath, ex);
            }
        }
        return image;
    }

    /**
     * Returns the buffered image corresponding to the given image
     * @param source an image
     * @return the buffered image corresponding to the given image
     */
    public static BufferedImage getBufferedImage(Image source)
    {
        if (source == null)
        {
            return null;
        }
        else if (source instanceof BufferedImage)
        {
            return (BufferedImage) source;
        }

        int width = source.getWidth(null);
        int height = source.getHeight(null);

        BufferedImage image
            = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        Graphics graphics = image.createGraphics();
        graphics.drawImage(source, 0, 0, null);
        graphics.dispose();

        return image;
    }

    /**
     * Extracts bytes from image.
     * @param image the image.
     * @return the bytes of the image.
     */
    public static byte[] toByteArray(BufferedImage image)
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try
        {
            ImageIO.write(image, "png", out);
        }
        catch (IOException e)
        {
            logger.debug("Cannot convert buffered image to byte[]", e);
            return null;
        }

        return out.toByteArray();
    }

    /**
     * Loads an image from a given bytes array.
     * @param imageBytes The bytes array to load the image from.
     * @return The image for the given bytes array.
     */
    public static Image getBytesInImage(byte[] imageBytes)
    {
        Image image = null;
        try
        {
            image = ImageIO.read(
                    new ByteArrayInputStream(imageBytes));

        }
        catch (Exception e)
        {
            logger.error("Failed to convert bytes to image.", e);
        }
        return image;
    }
}
