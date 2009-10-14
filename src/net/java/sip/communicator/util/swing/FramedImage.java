/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.util.swing;

import java.awt.*;

import javax.swing.*;

import net.java.sip.communicator.util.*;

/**
 * A custom component, used to show images in a frame.
 * 
 * @author Yana Stamcheva
 */
public class FramedImage
    extends JComponent
{
    private final Image frameImage;

    private Image image;

    private final int width;

    private final int height;

    /**
     * Creates a FramedImage by specifying the width and the height of the
     * label. These are used to paint the image frame in the correct bounds.
     * 
     * @param imageIcon the icon to show within the frame
     * @param width the width of the frame
     * @param height the height of the frame
     */
    public FramedImage(ImageIcon imageIcon, int width, int height)
    {
        this.width = width;
        this.height = height;

        this.setPreferredSize(new Dimension(width, height));

        this.frameImage
            = ImageUtils
                .scaleImageWithinBounds(
                    UtilActivator
                        .getResources()
                            .getImage("service.gui.USER_PHOTO_FRAME").getImage(),
                    width,
                    height);

        if (imageIcon != null)
            this.setImageIcon(imageIcon);
    }

    /**
     * Creates a FramedImage by specifying the width and the height of the frame.
     * 
     * @param width the width of the frame
     * @param height the height of the frame
     */
    public FramedImage(int width, int height)
    {
        this(null, width, height);
    }

    /**
     * Sets the image to display in the frame.
     * 
     * @param imageIcon the image to display in the frame
     */
    public void setImageIcon(ImageIcon imageIcon)
    {
        image = ImageUtils.getScaledRoundedImage(   imageIcon.getImage(),
                                                    width - 2,
                                                    height - 2);

        if (this.isVisible())
        {
            this.revalidate();
            this.repaint();
        }
    }

    /**
     * Paints the contained image in a frame.
     *
     * Overrides {@link JComponent#paintComponent(Graphics)}.
     */
    public void paintComponent(Graphics g)
    {
        if (image != null)
        {
            int imageWidth = image.getWidth(this);
            int imageHeight = image.getHeight(this);
            if ((imageWidth != -1) && (imageHeight != -1))
                g.drawImage(
                    image,
                    width / 2 - imageWidth / 2,
                    height / 2 - imageHeight / 2,
                    null);
        }

        int frameWidth = frameImage.getWidth(this);
        int frameHeight = frameImage.getHeight(this);
        if ((frameWidth != -1) && (frameHeight != -1))
            g.drawImage(
                frameImage,
                width / 2 - frameWidth / 2,
                height / 2 - frameHeight / 2,
                null);
    }
}
