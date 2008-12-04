/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.customcontrols;

import java.awt.*;

import javax.swing.*;

import net.java.sip.communicator.swing.*;
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
    private ImageIcon shadowIcon;

    private ImageIcon frameIcon;

    private ImageIcon imageIcon;

    private int width;

    private int height;

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
        super();

        this.width = width;

        this.height = height;

        this.setPreferredSize(new Dimension(width, height));

        Image photoFrameImage
            = ImageLoader.getImage(ImageLoader.USER_PHOTO_FRAME);

        this.frameIcon
            = ImageUtils.scaleIconWithinBounds( photoFrameImage,
                                                width,
                                                height);

        this.shadowIcon
            = ImageUtils.scaleIconWithinBounds(
                    ImageLoader.getImage(ImageLoader.USER_PHOTO_SHADOW),
                    width,
                    height);

        if (imageIcon != null)
        {
            this.setImageIcon(imageIcon);
        }
    }

    public FramedImage(int width, int height)
    {
        this(null, width, height);
    }

    public ImageIcon getImageIcon()
    {
        return imageIcon;
    }

    public void setImageIcon(ImageIcon imageIcon)
    {
        this.imageIcon
            = ImageUtils.getScaledRoundedImage( imageIcon.getImage(),
                                                width - 2,
                                                height - 2);
    }

    /**
     * Overrides {@link JComponent#paintComponent(Graphics)}.
     */
    public void paintComponent(Graphics g)
    {
        g.drawImage(imageIcon.getImage(),
                    width/2 - imageIcon.getIconWidth()/2,
                    height/2 - imageIcon.getIconHeight()/2, null);

        g.drawImage(frameIcon.getImage(),
                    width/2 - frameIcon.getIconWidth()/2,
                    height/2 - frameIcon.getIconHeight()/2, null);

        g.drawImage(shadowIcon.getImage(),
            width/2 - shadowIcon.getIconWidth()/2,
            1, null);
    }
}
