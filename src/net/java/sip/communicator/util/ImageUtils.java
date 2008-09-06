/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.util;

import java.awt.*;
import javax.swing.*;

/**
 * Utility methods for image manipulation.
 *
 * @author Sebastien Mazy
 */
public class ImageUtils
{
    /**
     * Returns a scaled image fitting within the given bounds
     * while keeping the aspect ratio.
     *
     * @param image the image to scale
     * @param width maximum width of the scaled image
     * @param height maximum height of the scaled image
     * @return the scaled image
     */
    public static ImageIcon
        scaleIconWithinBounds(ImageIcon image, int width, int height)
    {
        ImageIcon scaledImage;
        int scaleHint = Image.SCALE_SMOOTH;
        double originalRatio =
            (double) image.getIconWidth() / image.getIconHeight();
        double areaRatio = (double) width / height;

        if(originalRatio > areaRatio)
        {
            scaledImage = new ImageIcon(image.getImage().
                    getScaledInstance(width, -1,scaleHint));
        }
        else
        {
            scaledImage = new ImageIcon(image.getImage().
                    getScaledInstance(-1, height, scaleHint));
        }
        return scaledImage;
    }
}
