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
import net.java.sip.communicator.util.swing.*;

/**
 * The SCScrollPane is a JScrollPane with a custom viewport that allows to
 * set an image as a background. Depending on the
 * "impl.gui.IS_TEXTURE_BACKGROUND" property we'll be setting a single image or
 * a texture of images.
 * 
 * @author Yana Stamcheva
 */
public class SCScrollPane
    extends JScrollPane
{
    private static final long serialVersionUID = 0L;

	public SCScrollPane()
    {
	    this.setBorder(BorderFactory.createMatteBorder(
            1, 0, 1, 0, Color.GRAY));
        this.setOpaque(false);

        this.setViewport(new SCViewport());

        this.getVerticalScrollBar().setUnitIncrement(30);
    }

    /**
     * Sets the view of this JViewport.
     * 
     * @param view the view to set.
     */
    public void setViewportView(JComponent view)
    {
        view.setOpaque(false);
        view.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));

        super.setViewportView(view);
    }

    private static class SCViewport
        extends JViewport
    {
        private static final long serialVersionUID = 1L;

        private final BufferedImage bgImage;

        private final Color color;

        private final TexturePaint texture;

        public SCViewport()
        {
            if (getSettingsBoolean("impl.gui.IS_WINDOW_BACKGROUND_ENABLED"))
            {
                bgImage =
                    ImageLoader.getImage(ImageLoader.MAIN_WINDOW_BACKGROUND);

                if (getSettingsBoolean("impl.gui.IS_TEXTURE_BACKGROUND")
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

        private boolean getSettingsBoolean(String key)
        {
            return new Boolean(GuiActivator.getResources().getSettingsString(
                key)).booleanValue();
        }

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
    }
}
