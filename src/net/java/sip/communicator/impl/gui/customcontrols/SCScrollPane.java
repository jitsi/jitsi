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

public class SCScrollPane
    extends JScrollPane
{
    private BufferedImage bgImage;

    private TexturePaint texture;

    private boolean isTextureBackground;

    boolean isWindowImageBgEnabled;

    public SCScrollPane()
    {
        super();

        this.setBorder(null);
        this.setOpaque(false);

        this.setViewport(new SCViewport());

        this.getVerticalScrollBar().setUnitIncrement(30);

        String windowImageBackgroundProperty
            = "net.java.sip.communicator.impl.gui.isWindowImageBackgroundEnabled";

        isWindowImageBgEnabled = new Boolean(GuiActivator.getResources()
            .getSettingsString(windowImageBackgroundProperty)).booleanValue();

        if (isWindowImageBgEnabled)
            this.initBackgroundImage();
    }

    private void initBackgroundImage()
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

    private class SCViewport extends JViewport
    {
        public SCViewport()
        {
            this.setOpaque(false);
            this.setBorder(null);
        }

        public void paintComponent(Graphics g)
        {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g;

            AntialiasingManager.activateAntialiasing(g2);

            g2.setColor(Color.WHITE);

            g2.fillRoundRect(0, 0,
                    this.getWidth() - 1,
                    this.getHeight() - 1,
                    15, 15);

            g2.setColor(Color.GRAY);

            g2.drawRoundRect(0, 0,
                this.getWidth() - 1,
                this.getHeight() - 1, 15, 15);

            // paint the image
            if (isWindowImageBgEnabled && bgImage != null)
            {
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
    }
}
