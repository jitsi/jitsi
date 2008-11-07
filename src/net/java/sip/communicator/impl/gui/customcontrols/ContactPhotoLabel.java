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
 * A custom label, used to show contact images in a frame, where appropriate.
 * 
 * @author Yana Stamcheva
 */
public class ContactPhotoLabel extends JLabel
{
    private Image photoFrameImage
        = ImageLoader.getImage(ImageLoader.USER_PHOTO_FRAME);

    private ImageIcon labelIcon;

    /**
     * Creates a ContactPhotoLabel by specifying the width and the height of the
     * label. These are used to paint the frame in the correct bounds.
     * 
     * @param width the width of the label.
     * @param height the height of the label.
     */
    public ContactPhotoLabel(int width, int height)
    {
        this.labelIcon
            = ImageUtils.scaleIconWithinBounds( photoFrameImage,
                                                width,
                                                height);

        this.setAlignmentX(CENTER_ALIGNMENT);
        this.setAlignmentY(CENTER_ALIGNMENT);
    }

    /**
     * Overrides {@link JComponent#paintComponent(Graphics)}.
     */
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);

        g.drawImage(labelIcon.getImage(), 0, 0, null);
    }
}
