/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.customcontrols;

import java.awt.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.util.*;

/**
 * A custom component, used to show contact images in a frame,
 * where appropriate.
 * 
 * @author Yana Stamcheva
 */
public class FramedImage
    extends JComponent
{
    private final Image shadowImage;

    private final Image frameImage;

    private Image image;

    private final int width;

    private final int height;

    /**
     * Creates a ContactPhotoLabel by specifying the width and the height of the
     * label. These are used to paint the frame in the correct bounds.
     * 
     * @param imageIcon the icon to show within the frame
     * @param width the width of the label.
     * @param height the height of the label.
     */
    public FramedImage(ImageIcon imageIcon, int width, int height)
    {
        this.width = width;
        this.height = height;

        this.setPreferredSize(new Dimension(width, height));

        this.frameImage =
            ImageUtils.scaleImageWithinBounds(ImageLoader
                .getImage(ImageLoader.USER_PHOTO_FRAME), width, height);
        this.shadowImage =
            ImageUtils.scaleImageWithinBounds(ImageLoader
                .getImage(ImageLoader.USER_PHOTO_SHADOW), width, height);

        if (imageIcon != null)
        {
            this.setImageIcon(imageIcon);
        }
    }

    public FramedImage(int width, int height)
    {
        this(null, width, height);
    }

    public void setImageIcon(ImageIcon imageIcon)
    {
        this.image =
            ImageUtils.getScaledRoundedImage(imageIcon.getImage(), width - 2,
                height - 2);
    }

    /*
     * Overrides {@link JComponent#paintComponent(Graphics)}.
     */
    public void paintComponent(Graphics g)
    {
        g.drawImage(image, width / 2 - image.getWidth(null) / 2, height / 2
            - image.getHeight(null) / 2, null);

        g.drawImage(frameImage, width / 2 - frameImage.getWidth(null) / 2,
            height / 2 - frameImage.getHeight(null) / 2, null);

        g.drawImage(shadowImage, width / 2 - shadowImage.getWidth(null) / 2, 1,
            null);
    }
}
