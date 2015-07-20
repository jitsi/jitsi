/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.sip.communicator.plugin.desktoputil;

import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.net.*;

import javax.imageio.*;
import javax.swing.*;

import net.java.sip.communicator.util.*;

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
     * Different shapes that an image can be cropped to.
     */
    private static enum Shape
    {
        /**
         * Ellipse with the same height and width as the scaled image (this
         * will be a circle if the un-cropped image is square).
         */
        ELLIPSE,

        /**
         * Rectangle with the corners rounded to arcs with radius 3 pixels
         */
        ROUNDED_RECTANGLE;
    }

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
            Image image;

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
        return getScaledImage(image, Shape.ROUNDED_RECTANGLE, width, height);
    }

    /**
     * Creates a elliptical avatar image.
     *
     * @param image image of the initial avatar image.
     * @param width the desired width
     * @param height the desired height
     * @return The elliptical image.
     */
    public static Image getScaledEllipticalImage(  Image image,
                                                int width,
                                                int height)
    {
        return getScaledImage(image, Shape.ELLIPSE, width, height);
    }

    /**
     * Creates an avatar image in the specified shape.
     *
     * @param image image of the initial avatar image.
     * @param shape the desired shape
     * @param width the desired width
     * @param height the desired height
     * @return The cropped, scaled image.
     */
    private static Image getScaledImage(  Image image,
                                          Shape shape,
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

            switch (shape)
            {
            case ELLIPSE:
                g.fillOval(0, 0, scaledImageWidth, scaledImageHeight);
                break;
            case ROUNDED_RECTANGLE:
                g.fillRoundRect(0, 0,
                                scaledImageWidth, scaledImageHeight,
                                5, 5);
                break;
            }

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
     * Returns a scaled rounded instance of the given <tt>image</tt>.
     * @param image the image to scale
     * @param width the desired width
     * @param height the desired height
     * @return a byte array containing the scaled rounded image
     */
    public static byte[] getScaledInstanceInBytes(
        Image image, int width, int height)
    {
        BufferedImage scaledImage
            = (BufferedImage) getScaledRoundedImage(image, width, height);

        return convertImageToBytes(scaledImage);
    }

    /**
     * Returns a scaled elliptical instance of the given <tt>image</tt>.
     * @param image the image to scale
     * @param width the desired width
     * @param height the desired height
     * @return a byte array containing the scaled elliptical image
     */
    public static byte[] getScaledEllipticalInstanceInBytes(
        Image image, int width, int height)
    {
        BufferedImage scaledImage
            = (BufferedImage) getScaledEllipticalImage(image, width, height);

        return convertImageToBytes(scaledImage);
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
     * Returns a scaled elliptical icon from the given <tt>image</tt>, scaled
     * within the given <tt>width</tt> and <tt>height</tt>.
     * @param image the image to scale
     * @param width the maximum width of the scaled icon
     * @param height the maximum height of the scaled icon
     * @return a scaled elliptical icon
     */
    public static ImageIcon getScaledEllipticalIcon(Image image, int width,
        int height)
    {
        Image scaledImage = getScaledEllipticalImage(image, width, height);

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
        return getScaledIcon(imageBytes,
                             Shape.ROUNDED_RECTANGLE,
                             width,
                             height);
    }

    /**
     * Creates a elliptical scaled image.
     *
     * @param imageBytes The bytes of the image to be scaled.
     * @param width The maximum width of the scaled image.
     * @param height The maximum height of the scaled image.
     *
     * @return The elliptical scaled image.
     */
    public static ImageIcon getScaledEllipticalIcon(  byte[] imageBytes,
                                                      int width,
                                                      int height)
    {
        return getScaledIcon(imageBytes, Shape.ELLIPSE, width, height);
    }

    /**
     * Creates a cropped, scaled image.
     *
     * @param imageBytes The bytes of the image to be scaled.
     * @param shape The shape of the scaled image.
     * @param width The maximum width of the scaled image.
     * @param height The maximum height of the scaled image.
     *
     * @return The cropped, scaled image.
     */
    private static ImageIcon getScaledIcon(  byte[] imageBytes,
                                             Shape shape,
                                             int width,
                                             int height)
    {
        if (imageBytes == null || !(imageBytes.length > 0))
            return null;

        ImageIcon imageIcon = null;

        try
        {
            Image image;

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
            {
                switch (shape)
                {
                case ELLIPSE:
                    imageIcon = getScaledEllipticalIcon(image, width, height);
                    break;
                case ROUNDED_RECTANGLE:
                    imageIcon = getScaledRoundedIcon(image, width, height);
                    break;
                }
            }
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
        if (imageBytes == null || !(imageBytes.length > 0))
            return null;

        Image image = null;
        try
        {
            image = ImageIO.read(new ByteArrayInputStream(imageBytes));
        }
        catch (Exception e)
        {
            logger.error("Failed to convert bytes to image.", e);
        }
        return image;
    }

    /**
     * Creates a composed image from two images. If one of the images
     * is missing will add an empty space on its place.
     * @param leftImage the left image.
     * @param rightImage the right image
     * @param imageObserver need to calculate image sizes.
     * @return the composed image.
     */
    public static Image getComposedImage(
            Image leftImage, Image rightImage,
            ImageObserver imageObserver)
    {
        int height;
        int leftWidth;
        int width;

        if (leftImage == null)
        {
            if (rightImage == null)
            {
                return null;
            }
            else
            {
                height = rightImage.getHeight(imageObserver);
                leftWidth = rightImage.getWidth(imageObserver);
                width = leftWidth * 2;
            }
        }
        else if (rightImage == null)
        {
            height = leftImage.getHeight(imageObserver);
            leftWidth = leftImage.getWidth(imageObserver);
            width = leftWidth * 2;
        }
        else
        {
            height
                = Math.max(
                        leftImage.getHeight(imageObserver),
                        rightImage.getHeight(imageObserver));
            leftWidth = leftImage.getWidth(imageObserver);
            width = leftWidth + rightImage.getWidth(imageObserver);
        }

        BufferedImage buffImage
            = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D) buffImage.getGraphics();

        AntialiasingManager.activateAntialiasing(g);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));
        if (leftImage != null)
            g.drawImage(leftImage, 0, 0, null);
        if (rightImage != null)
            g.drawImage(rightImage, leftWidth + 1, 0, null);

        return buffImage;
    }

    /**
     * Returns a bytes of the given <tt>scaledImage</tt>.
     * @param scaledImage the image to scale
     * @return a byte array containing the scaled image
     */
    private static byte[] convertImageToBytes(BufferedImage scaledImage)
    {
        byte[] scaledBytes = null;

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
     * Sets the image of the incoming call notification.
     *
     * @param label the label to set the image to
     * @param image the image to set
     * @param width the desired image width
     * @param height the desired image height
     */
    public static void setScaledLabelImage(
        final JLabel label,
        final byte[] image,
        final int width,
        final int height)
    {
        if(!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    setScaledLabelImage(label, image, width, height);
                }
            });
            return;
        }

        label.setIcon(getScaledRoundedIcon(image, width, height));

        label.repaint();
    }
}
