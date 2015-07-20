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

import javax.swing.*;

import net.java.sip.communicator.util.skin.*;

/**
 * A custom component, used to show images in a frame.
 *
 * @author Yana Stamcheva
 * @author Adam Netocny
 */
public class FramedImage
    extends JComponent
    implements Skinnable
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The frame image.
     */
    private Image frameImage;

    /**
     * The icon image.
     */
    private ImageIcon icon;

    /**
     * The default width of the image.
     */
    protected final int width;

    /**
     * The default height of the image.
     */
    protected final int height;

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

        loadSkin();

        if (imageIcon != null)
            this.icon = getScaledImage(imageIcon.getImage());
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
     * @param image the image to display in the frame
     */
    public void setImageIcon(byte[] image)
    {
        icon = getScaledImage(image);

        if (this.isVisible())
        {
            this.revalidate();
            this.repaint();
        }
    }

    /**
     * Sets the image to display in the frame.
     *
     * @param image the image to display in the frame
     */
    public void setImageIcon(Image image)
    {
        icon = getScaledImage(image);

        if (this.isVisible())
        {
            this.revalidate();
            this.repaint();
        }
    }

    /**
     * Returns the image that is shown.
     * @return the image that is shown
     */
    public Image getImage()
    {
        if(icon != null)
            return icon.getImage();
        return null;
    }

    /**
     * Paints the contained image in a frame.
     *
     * Overrides {@link JComponent#paintComponent(Graphics)}.
     */
    @Override
    public void paintComponent(Graphics g)
    {
        if (icon != null)
        {
            int imageWidth = icon.getIconWidth();
            int imageHeight = icon.getIconHeight();
            if ((imageWidth != -1) && (imageHeight != -1))
                g.drawImage(
                    icon.getImage(),
                    width / 2 - imageWidth / 2,
                    height / 2 - imageHeight / 2,
                    null);
        }

        if (frameImage != null)
        {
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

    /**
     * Loads the framed image.
     */
    public void loadSkin()
    {
        ImageIcon frameIcon = DesktopUtilActivator
            .getResources()
                .getImage("service.gui.USER_PHOTO_FRAME");

        // Frame image will be drawn only if's bigger or equal to the underlying
        // image. We would like to avoid pixelated results!
        if (frameIcon.getIconWidth() >= width
            && frameIcon.getIconHeight() >= frameIcon.getIconHeight())
        {
            this.frameImage
                = ImageUtils
                    .scaleImageWithinBounds(frameIcon.getImage(),
                        width,
                        height);
        }
    }

    /**
     * Returns the scaled image version of the given image.
     *
     * @param image the image to transform
     * @return the scaled image version of the given image
     */
    private ImageIcon getScaledImage(Image image)
    {
        return ImageUtils.getScaledRoundedIcon(image, width - 2, height - 2);
    }

    /**
     * Returns the scaled image version of the given image.
     *
     * @param image the image to transform
     * @return the scaled image version of the given image
     */
    private ImageIcon getScaledImage(byte[] image)
    {
        return ImageUtils.getScaledRoundedIcon(image, width - 2, height - 2);
    }
}
