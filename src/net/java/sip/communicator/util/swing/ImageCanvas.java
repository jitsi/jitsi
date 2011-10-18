/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.util.swing;

import java.awt.*;

import javax.swing.*;

/**
 * @author Lubomir Marinov
 */
public class ImageCanvas
    extends TransparentPanel
{
    private ImageIcon icon;

    public ImageCanvas(Image image)
    {
        setImage(image);
    }

    protected void paintComponent(Graphics g)
    {
        super.paintComponent(g);

        if (icon == null)
            return;

        int imageWidth = icon.getIconWidth();
        if (imageWidth < 1)
            return;
        int imageHeight = icon.getIconHeight();
        if (imageHeight < 1)
            return;

        int width = getWidth();
        boolean scale = false;
        float scaleFactor = 1;
        if (imageWidth > width)
        {
            scale = true;
            scaleFactor = width / (float) imageWidth;
        }
        int height = getHeight();
        if (imageHeight > height)
        {
            scale = true;
            scaleFactor = Math.min(scaleFactor, height / (float) imageHeight);
        }
        if (scale)
        {
            imageWidth = Math.round(imageWidth * scaleFactor);
            imageHeight = Math.round(imageHeight * scaleFactor);
        }

        g.drawImage(icon.getImage(), (width - imageWidth) / 2,
            (height - imageHeight) / 2, imageWidth, imageHeight, null);
    }

    /**
     * Sets image to be painted.
     * @param image Image to be painted.
     */
    public void setImage(Image image)
    {
        icon = (image == null) ? null : new ImageIcon(image);

        if (icon != null)
        {
            final int preferredWidth = icon.getIconWidth();
            final int preferredHeight = icon.getIconHeight();
            setMinimumSize(new Dimension(preferredWidth / 2,
                preferredHeight / 2));
            setPreferredSize(new Dimension(preferredWidth, preferredHeight));
        }
    }
}
