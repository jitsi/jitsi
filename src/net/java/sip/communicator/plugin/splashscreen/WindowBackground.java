package net.java.sip.communicator.plugin.splashscreen;

import java.awt.*;
import java.io.*;

import javax.imageio.*;
import javax.swing.*;

/**
 * Constructs the window background in order to have a background image.
 */
public class WindowBackground
    extends JPanel
{
    private Image bgImage;

    public WindowBackground()
    {
        this.setOpaque(true);
        
        try
        {
            bgImage = ImageIO.read(WindowBackground.class
                .getResource("resources/aboutWindowBackground.png"));
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        this.setPreferredSize(new Dimension(bgImage.getWidth(this), bgImage
            .getHeight(this)));
    }

    protected void paintComponent(Graphics g)
    {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON);

        g2.drawImage(bgImage, 0, 0, null);

        g2.setColor(new Color(255, 255, 255, 170));

        g2.fillRect(0, 0, getWidth(), getHeight());
        
        g2.setColor(new Color(150, 150, 150));
        
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 5, 5);
    }
}
