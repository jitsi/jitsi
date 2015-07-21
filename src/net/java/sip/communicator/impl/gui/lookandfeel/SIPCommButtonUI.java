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
package net.java.sip.communicator.impl.gui.lookandfeel;

import java.awt.*;
import java.awt.image.*;

import javax.swing.*;
import javax.swing.plaf.*;
import javax.swing.plaf.metal.*;

import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.util.skin.*;

/**
 * SIPCommButtonUI implementation.
 *
 * @author Yana Stamcheva
 */
public class SIPCommButtonUI
    extends MetalButtonUI
    implements Skinnable
{
    /**
     * The background button image.
     */
    private static BufferedImage buttonBG
        = ImageLoader.getImage(ImageLoader.BUTTON);

    /**
     * The background image for the pressed button state.
     */
    private static BufferedImage buttonRolloverBG
        = ImageLoader.getImage(ImageLoader.BUTTON_ROLLOVER);

    /**
     * Indicates if the button is in rollover state.
     */
    private boolean bufferIsRollover = false;

    /**
     * The paint buffer.
     */
    private BufferedImage paintBuffer = null;

    /**
     * The buffered component.
     */
    private Component bufferedComponent = null;

    /**
     * Creates the UI for the given <tt>JComponent</tt>.
     *
     * @param c the <tt>JComponent</tt>, for which to create an UI
     * @return the component UI
     */
    public static ComponentUI createUI(JComponent c)
    {
        return new SIPCommButtonUI();
    }

    /**
     * Installs default configurations for the given <tt>AbstractButton</tt>.
     *
     * @param b the button, for which we're installing the defaults
     */
    @Override
    public void installDefaults(AbstractButton b)
    {
        super.installDefaults(b);

        b.setOpaque(false);
        b.setBorderPainted(false);
        b.setFocusPainted(true);
        b.setRolloverEnabled(true);
    }

    /**
     * Uninstalls default configurations for the given <tt>AbstractButton</tt>.
     *
     * @param b the button, for which we're uninstalling the defaults
     */
    @Override
    public void uninstallDefaults(AbstractButton b)
    {
        super.uninstallDefaults(b);

        b.setBorderPainted(true);
        b.setFocusPainted(false);
        b.setOpaque(true);
        b.setRolloverEnabled(false);
    }

    /**
     * Paints this button UI.
     *
     * @param g the <tt>Graphics</tt> object used for painting
     * @param c the <tt>Component</tt> to paint
     */
    @Override
    public void paint(Graphics g, JComponent c)
    {
        AbstractButton button = (AbstractButton)c;
        ButtonModel model = button.getModel();

        // check if the context of the buffer is consistent or else recreate it
        if (paintBuffer == null || c != bufferedComponent
                || bufferIsRollover != model.isRollover())
        {
            // create a buffer in the best available format
            paintBuffer = ((Graphics2D) g).getDeviceConfiguration().
                createCompatibleImage(c.getWidth(), c.getHeight(),
                        Transparency.TRANSLUCENT);

            // save the context
            bufferedComponent = c;
            bufferIsRollover = model.isRollover();

            // draw in the buffer
            BufferedImage leftImg;
            BufferedImage middleImg;
            BufferedImage rightImg;

            int imgWidth;
            int imgHeight;
            int indentWidth  = 10;
            if(bufferIsRollover){
                imgWidth = buttonRolloverBG.getWidth();
                imgHeight = buttonRolloverBG.getHeight();

                leftImg = buttonRolloverBG.getSubimage(0, 0, indentWidth,
                                                        imgHeight);
                middleImg = buttonRolloverBG.getSubimage(indentWidth, 0,
                                                        imgWidth-2*indentWidth,
                                                        imgHeight);
                rightImg = buttonRolloverBG.getSubimage(imgWidth-indentWidth, 0,
                                                        indentWidth, imgHeight);
            }
            else{
                imgWidth = buttonBG.getWidth();
                imgHeight = buttonBG.getHeight();

                leftImg = buttonBG.getSubimage(0, 0, 10, imgHeight);
                middleImg = buttonBG.getSubimage(10, 0, imgWidth-20, imgHeight);
                rightImg = buttonBG.getSubimage(imgWidth-10, 0, 10, imgHeight);
            }

            Graphics2D g2 = paintBuffer.createGraphics();

            AntialiasingManager.activateAntialiasing(g2);

            g2.drawImage(leftImg, 0, 0, indentWidth, c.getHeight(), null);
            g2.drawImage(middleImg, indentWidth, 0,
                    c.getWidth() - 2 * indentWidth, c.getHeight(), null);
            g2.drawImage(rightImg, c.getWidth() - indentWidth, 0,
                    indentWidth, c.getHeight(), null);
        }

        AntialiasingManager.activateAntialiasing(g);

        // draw the buffer in the graphics object
        g.drawImage(paintBuffer, 0, 0, c.getWidth(), c.getHeight(),
                0, 0, c.getWidth(), c.getHeight(), null);

        super.paint(g, c);
    }

    /**
     * Paints the focused view of the given <tt>AbstractButton</tt>.
     *
     * @param g the <tt>Graphics</tt> object used for painting
     * @param b the button to paint
     * @param viewRect the rectangle indicating the bounds of the focused button
     * @param textRect the rectangle indicating the bounds of the text
     * @param iconRect the rectangle indicating the bounds of the icon
     */
    @Override
    protected void paintFocus(Graphics g, AbstractButton b,
            Rectangle viewRect, Rectangle textRect, Rectangle iconRect)
    {
        Graphics2D g2 = (Graphics2D)g;

        Rectangle focusRect = new Rectangle();
        String text = b.getText();
        boolean isIcon = b.getIcon() != null;

        // If there is text
        if ( text != null && !text.equals( "" ) )
        {
            if ( !isIcon )
            {
                focusRect.setBounds( textRect );
            }
            else
            {
                focusRect.setBounds( iconRect.union( textRect ) );
            }
        }
        // If there is an icon and no text
        else if ( isIcon )
        {
            focusRect.setBounds( iconRect );
        }

        g2.setStroke(new BasicStroke(0.5f,// Width
                BasicStroke.CAP_ROUND,    // End cap
                BasicStroke.JOIN_ROUND,   // Join style
                10.0f,                    // Miter limit
                new float[] {1.0f,1.0f},// Dash pattern
                2.0f));
        g2.setColor(Color.GRAY);
        g2.drawRoundRect((focusRect.x-3), (focusRect.y-3),
                focusRect.width+4, focusRect.height+4, 5, 5);
    }

    /**
     * Overriden to do nothing.
     */
    @Override
    protected void paintButtonPressed(Graphics g, AbstractButton b)
    {
        if ( b.isContentAreaFilled() )
        {
            Dimension size = b.getSize();
            g.setColor(getSelectColor());
            g.fillRoundRect(0, 0, size.width, size.height, 5, 5);
        }
    }

    /**
     * Reloads buffered images.
     */
    public void loadSkin()
    {
        buttonBG
            = ImageLoader.getImage(ImageLoader.BUTTON);

        buttonRolloverBG
            = ImageLoader.getImage(ImageLoader.BUTTON_ROLLOVER);

        paintBuffer = null;
    }
}
