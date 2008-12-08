/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.lookandfeel;

import java.awt.*;
import java.awt.image.*;

import javax.swing.*;
import javax.swing.plaf.*;
import javax.swing.plaf.basic.*;

import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.util.swing.*;

/**
 * SIPCommToggleButtonUI implementation.
 * 
 * @author Yana Stamcheva
 */
public class SIPCommToggleButtonUI
    extends BasicToggleButtonUI
{
    private final static BufferedImage buttonBG =
        ImageLoader.getImage(ImageLoader.BUTTON);

    private final static BufferedImage buttonPressedBG =
        ImageLoader.getImage(ImageLoader.TOGGLE_BUTTON_PRESSED);

    private boolean bufferIsPressed = false;

    private BufferedImage paintBuffer = null;

    private Component bufferedComponent = null;

    // ********************************
    // Create PLAF
    // ********************************
    public static ComponentUI createUI(JComponent c)
    {
        return new SIPCommToggleButtonUI();
    }

    // ********************************
    // Install
    // ********************************
    public void installDefaults(AbstractButton b)
    {
        super.installDefaults(b);

        b.setOpaque(false);
        b.setBorderPainted(false);
        b.setFocusPainted(true);
    }

    public void uninstallDefaults(AbstractButton b)
    {
        super.uninstallDefaults(b);

        b.setBorderPainted(true);
        b.setFocusPainted(false);
        b.setOpaque(true);
    }

    public void paint(Graphics g, JComponent c)
    {
        AbstractButton button = (AbstractButton) c;
        ButtonModel model = button.getModel();

        boolean isShownPressed =
            model.isArmed() && model.isPressed() || model.isSelected();

        // check if the context of the buffer is consistent or else recreate it
        if (paintBuffer == null || c != bufferedComponent
            || bufferIsPressed != isShownPressed)
        {
            // create a buffer in the best available format
            paintBuffer =
                ((Graphics2D) g).getDeviceConfiguration()
                    .createCompatibleImage(c.getWidth(), c.getHeight(),
                        Transparency.TRANSLUCENT);

            // save the context
            bufferedComponent = c;
            bufferIsPressed = isShownPressed;

            BufferedImage leftImg;
            BufferedImage middleImg;
            BufferedImage rightImg;

            int imgWidth;
            int imgHeight;
            int indentWidth = 10;

            if (isShownPressed)
            {
                imgWidth = buttonPressedBG.getWidth();
                imgHeight = buttonPressedBG.getHeight();

                leftImg = buttonPressedBG.getSubimage(0, 0, 10, imgHeight);
                middleImg =
                    buttonPressedBG
                        .getSubimage(10, 0, imgWidth - 20, imgHeight);
                rightImg =
                    buttonPressedBG
                        .getSubimage(imgWidth - 10, 0, 10, imgHeight);
            }
            else
            {
                imgWidth = buttonBG.getWidth();
                imgHeight = buttonBG.getHeight();

                leftImg = buttonBG.getSubimage(0, 0, 10, imgHeight);
                middleImg =
                    buttonBG.getSubimage(10, 0, imgWidth - 20, imgHeight);
                rightImg =
                    buttonBG.getSubimage(imgWidth - 10, 0, 10, imgHeight);
            }

            Graphics2D g2 = paintBuffer.createGraphics();

            AntialiasingManager.activateAntialiasing(g2);

            g2.drawImage(leftImg, 0, 0, indentWidth, c.getHeight(), null);
            g2.drawImage(middleImg, indentWidth, 0, c.getWidth() - 2
                * indentWidth, c.getHeight(), null);
            g2.drawImage(rightImg, c.getWidth() - indentWidth, 0, indentWidth,
                c.getHeight(), null);
        }

        AntialiasingManager.activateAntialiasing(g);

        // draw the buffer in the graphics object
        g.drawImage(paintBuffer, 0, 0, c.getWidth(), c.getHeight(), 0, 0, c
            .getWidth(), c.getHeight(), null);

        super.paint(g, c);
    }
}
