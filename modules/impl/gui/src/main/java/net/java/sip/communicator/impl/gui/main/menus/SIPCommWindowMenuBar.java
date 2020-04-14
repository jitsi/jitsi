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
package net.java.sip.communicator.impl.gui.main.menus;

import java.awt.*;
import java.awt.image.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.util.skin.*;

/**
 * Represents a <tt>JMenuBar</tt> with SIP Communicator specific decorations. In
 * contrast to <tt>SIPCommMenuBar</tt> which doesn't look as a 100% menu bar,
 * <tt>SIPCommWindowMenuBar</tt> does look that way.
 *
 * @author Lubomir Marinov
 * @author Yana Stamcheva
 * @author Adam Netocny
 */
public class SIPCommWindowMenuBar
    extends JMenuBar
    implements Skinnable
{
    /**
     * The <tt>Image</tt> to be drawn as the background of this
     * <tt>JMenuBar</tt>.
     */
    private Image backgroundImage;

    /**
     * Color code for the foreground.
     */
    private String foreground;

    /**
     * Initializes a new <tt>SIPCommWindowMenuBar</tt> instance which is to use
     * a specific foreground given by its id in the resources.
     *
     * @param foreground the id of the color resource to use as the foreground
     * of the new instance
     */
    public SIPCommWindowMenuBar(String foreground)
    {
        this.foreground = foreground;

        loadSkin();
    }

    /**
     * Determines whether a specific <tt>Image</tt> is transparent and thus does
     * not need drawing because nothing will be drawn.
     *
     * @param image the <tt>Image</tt> to be determined whether it is
     * transparent and thus does not need drawing
     * @return <tt>true</tt> if <tt>image</tt> is transparent and thus does not
     * need drawing; otherwise, <tt>false</tt>
     */
    private static boolean isTransparent(Image image)
    {
        if (image instanceof BufferedImage)
        {
            BufferedImage bufferedImage = (BufferedImage) image;
            int width = bufferedImage.getWidth();

            /*
             * Checking big images may be a performance hit so do it for small
             * ones only.
             */
            if ((-1 < width) && (width < 17))
            {
                int height = bufferedImage.getHeight();

                if ((-1 < height)
                        && (height < 17)
                        && (bufferedImage.getTransparency()
                                != Transparency.OPAQUE))
                {
                    boolean transparent = true;

                    /*
                     * Checking all pixels may be a performance hit so do it for
                     * some of them only.
                     */
                    for (int i = 0, min = Math.min(width, height);
                            i < min;
                            i++)
                    {
                        int rgb = bufferedImage.getRGB(i, i);

                        if ((rgb & 0xFF000000) != 0)
                        {
                            transparent = false;
                            break;
                        }
                    }
                    return transparent;
                }
            }
        }
        return false;
    }

    /**
     * Paints the MENU_BACKGROUND image on the background of this container.
     *
     * @param g the <tt>Graphics</tt> object that does the painting
     */
    @Override
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);

        if (backgroundImage != null)
            g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
    }

    /**
     * Loads images and colors used in this menu bar.
     */
    public void loadSkin()
    {
        backgroundImage = ImageLoader.getImage(ImageLoader.MENU_BACKGROUND);

        LookAndFeel laf = UIManager.getLookAndFeel();
        boolean setForeground = true;

        if (laf != null)
        {
            String lafClassName = laf.getClass().getName();

            if (lafClassName.equals(UIManager.getSystemLookAndFeelClassName())
                    && !lafClassName.equals(
                            UIManager.getCrossPlatformLookAndFeelClassName())
                    && ((backgroundImage == null)
                            || isTransparent(backgroundImage)))
            {
                backgroundImage = null;
                setForeground = false;
            }
        }

        if (setForeground)
        {
            setForeground(
                new Color(GuiActivator.getResources().getColor(foreground)));
        }
    }
}
