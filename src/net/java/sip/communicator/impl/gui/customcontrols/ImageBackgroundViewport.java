/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.customcontrols;

import java.awt.*;
import java.awt.image.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.utils.*;

/**
 * The <tt>ImageBackgroundViewport</tt> is an extension of JViewport, which
 * sets a background image to the viewport. This custom viewport is meant to
 * be used by scrollpanes that would like to have the application background
 * image as a background.
 * 
 * @author Yana Stamcheva
 */
public class ImageBackgroundViewport extends JViewport
{
    private BufferedImage bgImage;

    private TexturePaint texture;

    private boolean isTextureBackground;

    /**
     * Creates an instance of <tt>ImageBackgroundViewport</tt>.
     */
    public ImageBackgroundViewport()
    {
        isTextureBackground = new Boolean(GuiActivator.getResources()
            .getSettingsString("isTextureBackground")).booleanValue();

        bgImage = ImageLoader.getImage(ImageLoader.MAIN_WINDOW_BACKGROUND);

        if (isTextureBackground)
        {
            Rectangle rect
                = new Rectangle(0, 0,
                            bgImage.getWidth(null),
                            bgImage.getHeight(null));

            texture = new TexturePaint(bgImage, rect);
        }
    }

    /**
     * Paints the background image according to the isTextureBackground
     * property.
     */
    public void paintComponent(Graphics g)
    {
        // do the superclass behavior first
        super.paintComponent(g);

        // paint the image
        if (bgImage != null)
        {
            Graphics2D g2 = (Graphics2D) g;

            if (isTextureBackground)
            {
                g2.setPaint(texture);

                g2.fillRect(0, 0, this.getWidth(), this.getHeight());
            }
            else
            {
                g.setColor(new Color(
                    GuiActivator.getResources()
                        .getColor("contactListBackground")));

                // paint the background with the choosen color
                g.fillRect(0, 0, getWidth(), getHeight());

                g2.drawImage(bgImage,
                        this.getWidth() - bgImage.getWidth(),
                        this.getHeight() - bgImage.getHeight(),
                        this);
            }
        }
    }

    /**
     * Sets the view of this JViewport.
     * 
     * @param view the view to set.
     */
    public void setView(JComponent view)
    {
        view.setOpaque(false);
        super.setView(view);
    }

    /**
     * Returns the background image of this viewport.
     * 
     * @return the background image of this viewport.
     */
    public Image getBackgroundImage()
    {
        return bgImage;
    }
}