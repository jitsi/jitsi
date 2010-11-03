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
import net.java.sip.communicator.util.skin.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The SCScrollPane is a JScrollPane with a custom viewport that allows to
 * set an image as a background. Depending on the
 * "impl.gui.IS_CONTACT_LIST_TEXTURE_BG_ENABLED" property we'll be setting a
 * single image or a texture of images.
 *
 * @author Yana Stamcheva
 * @author Adam Netocny
 */
public class SCScrollPane
    extends JScrollPane
    implements Skinnable
{
    private static final long serialVersionUID = 0L;

    /**
     * Creates an <tt>SCSCrollPane</tt>.
     */
    public SCScrollPane()
    {
        this.setBorder(BorderFactory.createMatteBorder(
            1, 0, 1, 0, Color.GRAY));

        this.setViewport(new SCViewport());

        this.getVerticalScrollBar().setUnitIncrement(100);
    }

    /**
     * Sets the view of this JViewport.
     *
     * @param view the view to set.
     */
    public void setViewportView(JComponent view)
    {
        view.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
        view.setOpaque(false);

        super.setViewportView(view);
    }

    /**
     * Reloads skin information in viewport.
     */
    public void loadSkin()
    {
        ((SCViewport)this.getViewport()).loadSkin();
    }

    /**
     * The <tt>SCViewport</tt> used as viewport in this scrollpane.
     */
    private static class SCViewport
        extends JViewport
        implements Skinnable
    {
        private static final long serialVersionUID = 1L;

        private BufferedImage bgImage;

        private Color color;

        private TexturePaint texture;

        /**
         * Creates the <tt>SCViewport</tt>.
         */
        public SCViewport()
        {
            this.setBackground(Color.WHITE);

            loadSkin();
        }

        /**
         * Returns the boolean value of the property given by <tt>key</tt>.
         * @param key the key of the property we look for
         * @return the boolean value of the searched property
         */
        private boolean getSettingsBoolean(String key)
        {
            return new Boolean(GuiActivator.getResources().getSettingsString(
                key)).booleanValue();
        }

        /**
         * Paints this viewport.
         * @param g the <tt>Graphics</tt> object used for painting
         */
        public void paintComponent(Graphics g)
        {
            super.paintComponent(g);

            g = g.create();
            try
            {
                AntialiasingManager.activateAntialiasing(g);

                Graphics2D g2 = (Graphics2D) g;
                int width = getWidth();
                int height = getHeight();

                // paint the image
                if (bgImage != null)
                {
                    if (texture != null)
                    {
                        g2.setPaint(texture);

                        g2.fillRect(0, 0, width, height);
                    }
                    else
                    {
                        g.setColor(color);

                        // paint the background with the chosen color
                        g.fillRect(0, 0, width, height);

                        g2.drawImage(bgImage, width - bgImage.getWidth(),
                            height - bgImage.getHeight(), this);
                    }
                }
            }
            finally
            {
                g.dispose();
            }
        }

        /**
         * Reloads background.
         */
        public void loadSkin()
        {
            if(getSettingsBoolean("impl.gui.IS_CONTACT_LIST_IMG_BG_ENABLED"))
            {
                bgImage =
                    ImageLoader.getImage(ImageLoader.MAIN_WINDOW_BACKGROUND);

                if (getSettingsBoolean(
                    "impl.gui.IS_CONTACT_LIST_TEXTURE_BG_ENABLED")
                    && (bgImage != null))
                {
                    texture =
                        new TexturePaint(bgImage, new Rectangle(0, 0, bgImage
                            .getWidth(null), bgImage.getHeight(null)));

                    color = null;
                }
                else
                {
                    texture = null;
                    color =
                        new Color(GuiActivator.getResources().getColor(
                            "service.gui.CONTACT_LIST_BACKGROUND"));
                }
            }
            else
            {
                bgImage = null;
                texture = null;
                color = null;
            }
        }
    }
}
