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

/**
 * @author Lubomir Marinov
 */
public class ImageCanvas
    extends TransparentPanel
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    private ImageIcon icon;

    /**
     * Constructor.
     *
     * @param image the image for the canvas
     */
    public ImageCanvas(Image image)
    {
        setImage(image);
    }

    @Override
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
            setMinimumSize(new Dimension(preferredWidth, preferredHeight));
            setPreferredSize(new Dimension(preferredWidth, preferredHeight));
        }
    }
}
