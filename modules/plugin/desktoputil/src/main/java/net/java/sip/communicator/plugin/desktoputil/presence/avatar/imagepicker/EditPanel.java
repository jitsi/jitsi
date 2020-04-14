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
package net.java.sip.communicator.plugin.desktoputil.presence.avatar.imagepicker;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;

import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.plugin.desktoputil.*;

/**
 * The <tt>EditPanel</tt> manage the image size and the clipper component
 *
 * @author Damien Roth
 * @author Shashank Tyagi
 */
public class EditPanel
    extends TransparentPanel
    implements ChangeListener,
               ActionListener
{
    private ImageClipper imageClipper;
    private BufferedImage image;
    private JSlider imageSizeSlider;
    private JButton zoomIn, zoomOut, reset;

    private boolean resizeOnWidth = true;
    private boolean smallImage = false;

    // Clipping zone dimension
    private int clippingZoneWidth = 96;
    private int clippingZoneHeight = 96;

    /**
     * Create a new <tt>EditPanel</tt>
     *
     * @param clippingZoneWidth the width of the clipping zone
     * @param clippingZoneHeight the height of the clipping zone
     */
    public EditPanel(int clippingZoneWidth, int clippingZoneHeight)
    {
        super();
        this.setLayout(new BorderLayout());

        this.clippingZoneWidth = clippingZoneWidth;
        this.clippingZoneHeight = clippingZoneHeight;

        this.zoomOut = new JButton(DesktopUtilActivator.getResources()
                .getImage("service.gui.buttons.ZOOM_OUT"));
        this.zoomOut.addActionListener(this);
        this.zoomIn = new JButton(DesktopUtilActivator.getResources()
                .getImage("service.gui.buttons.ZOOM_IN"));
        this.zoomIn.addActionListener(this);
        this.reset = new JButton(DesktopUtilActivator.getResources()
                .getImage("service.gui.buttons.RESET"));
        this.reset.setToolTipText(DesktopUtilActivator.getResources()
                .getI18NString("service.gui.avatar.imagepicker.RESET"));
        this.reset.addActionListener(this);

        imageSizeSlider = new JSlider(clippingZoneWidth, clippingZoneWidth,
                clippingZoneWidth);
        imageSizeSlider.addChangeListener(this);
        imageSizeSlider.setOpaque(false);
        imageSizeSlider.setToolTipText(DesktopUtilActivator.getResources()
                .getI18NString("service.gui.avatar.imagepicker.IMAGE_SIZE"));

        TransparentPanel sliderPanel = new TransparentPanel();
        sliderPanel.add(zoomOut);
        sliderPanel.add(this.imageSizeSlider);
        sliderPanel.add(this.zoomIn);
        sliderPanel.add(this.reset);

        this.imageClipper = new ImageClipper(this.clippingZoneWidth,
                this.clippingZoneHeight);

        this.add(imageClipper, BorderLayout.CENTER);
        this.add(sliderPanel, BorderLayout.SOUTH);
    }

    /**
     * Sets the image to be edited
     *
     * @param image the image to be edited
     */
    public void setImage(BufferedImage image)
    {
        int width = image.getWidth(null);
        int height = image.getHeight(null);


        // Checks if one dimension of the image is smaller than the clipping zone
        if (width < this.clippingZoneWidth || height < this.clippingZoneHeight)
        {
            // Disable the slider used to set the size of the image
            this.enableSlider(false);
            this.smallImage = true;


            /* Resize the image to match the clipping zone
             * First case :
             *   * Image wider than high and the clipping zone
             *   * Image wider than high and included in the clipping zone
             * Second case :
             *   * Image higher than wide and the clipping zone
             *   * Image wider than high and included in the clipping zone
             */
            if ((width > height && width > this.clippingZoneWidth)
                    || (height > width && height <= this.clippingZoneHeight))
            {
                this.image = ImageUtils.getBufferedImage(
                    image.getScaledInstance(
                            -1, this.clippingZoneHeight, Image.SCALE_SMOOTH));
            }
            else
            {
                this.image = ImageUtils.getBufferedImage(
                    image.getScaledInstance(
                            this.clippingZoneWidth, -1, Image.SCALE_SMOOTH));
            }
        }
        else
        {
            this.image = image;
            this.enableSlider(true);
            this.resizeOnWidth = !(height < width);
            this.imageSizeSlider.setMaximum(Math.min(width, height));
        }

        SwingUtilities.invokeLater(new Runnable() {
            public void run()
            {
                reset();
            }
        });
    }

    /**
     * Reset the editor
     */
    public void reset()
    {
        imageSizeSlider.setValue(this.imageSizeSlider.getMinimum());
        drawImage();
    }

    /**
     * Returns the clipped image.
     *
     * @return the clipped image
     */
    public byte[] getClippedImage()
    {
        BufferedImage fullImage = getResizedImage(true);

        if(fullImage == null)
            return null;

        Rectangle clipping = this.imageClipper.getCroppedArea();

        BufferedImage subImage = fullImage.getSubimage(clipping.x, clipping.y,
                clipping.width, clipping.height);

        byte[] result = ImageUtils.toByteArray(subImage);

        return result;
    }

    /**
     * Resize the image.
     * @param hq
     * @return
     */
    private BufferedImage getResizedImage(boolean hq)
    {
        if(this.image == null)
            return null;

        BufferedImage i = null;
        int size = this.imageSizeSlider.getValue();

        if (this.resizeOnWidth)
        {
            i = ImageUtils.getBufferedImage(
                    this.image.getScaledInstance(
                            size,
                            -1,
                            (hq) ? Image.SCALE_SMOOTH : Image.SCALE_FAST));
        }
        else
        {
            i = ImageUtils.getBufferedImage(
                    this.image.getScaledInstance(
                            -1,
                            size,
                            (hq) ? Image.SCALE_SMOOTH : Image.SCALE_FAST));
        }

        return i;
    }

    /**
     * Draw the image.
     */
    private void drawImage()
    {
        // Use high quality scalling when the image is smaller than the clipper
        this.imageClipper.setImage(getResizedImage(smallImage));
    }

    private void enableSlider(boolean enabled)
    {
        this.imageSizeSlider.setEnabled(enabled);
        this.zoomIn.setEnabled(enabled);
        this.zoomOut.setEnabled(enabled);
        this.reset.setEnabled(enabled);
    }

    /**
     * New size image selected update the clipper.
     * @param e the event.
     */
    public void stateChanged(ChangeEvent e)
    {
        drawImage();
    }

    /**
     * Listens for button actions.
     * @param e
     */
    public void actionPerformed(ActionEvent e)
    {
        if(e.getSource().equals(zoomIn))
        {
            if(imageSizeSlider.getValue() < imageSizeSlider.getMaximum())
            {
                imageSizeSlider.setValue(imageSizeSlider.getValue() + 1);
                drawImage();
            }
        }
        else if(e.getSource().equals(zoomOut))
        {
            if(imageSizeSlider.getValue() > imageSizeSlider.getMinimum())
            {
                imageSizeSlider.setValue(imageSizeSlider.getValue() - 1);
                drawImage();
            }
        }
        else if(e.getSource().equals(reset))
        {
            reset();
        }
    }
}
