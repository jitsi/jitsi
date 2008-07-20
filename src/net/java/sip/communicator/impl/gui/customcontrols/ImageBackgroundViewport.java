package net.java.sip.communicator.impl.gui.customcontrols;

import java.awt.*;
import java.awt.image.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.utils.*;

public class ImageBackgroundViewport extends JViewport
{
    private BufferedImage bgImage;

    private TexturePaint texture;

    private boolean isTextureBackground;

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
                    GuiActivator.getResources().getColor("contactListBackground")));

                // paint the background with the choosen color
                g.fillRect(0, 0, getWidth(), getHeight());

                g2.drawImage(bgImage,
                        this.getWidth() - bgImage.getWidth(),
                        this.getHeight() - bgImage.getHeight(),
                        this);
            }
        }
    }

    public void setView(JComponent view)
    {
        view.setOpaque(false);
        super.setView(view);
    }
    
    public Image getBackgroundImage()
    {
        return bgImage;
    }
}