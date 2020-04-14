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

import net.java.sip.communicator.plugin.desktoputil.*;

/**
 * Component allowing the user to easily crop an image
 *
 * @author Damien Roth
 * @author Damian Minkov
 */
public class ImageClipper
    extends JComponent
    implements MouseListener,
               MouseMotionListener
{
    /**
     * Border of the image.
     */
    private static final Color IMAGE_BORDER_COLOR
            = new Color(174, 189, 215);

    /**
     * Image overlay color.
     */
    private static final Color IMAGE_OVERLAY_COLOR
            = new Color(1.0f, 1.0f, 1.0f, 0.4f);

    /**
     * The last remembered component width, to see when component is resized.
     */
    private int lastComponentWidth = 0;

    /**
     * The last remembered component height, to see when component is resized.
     */
    private int lastComponentHeight = 0;

    /**
     * The image that we will crop.
     */
    private BufferedImage image = null;

    /**
     * The rectangle in which we are currently drawing the image.
     */
    private Rectangle imageRect = new Rectangle();

    /**
     * The zone that we will crop later from the image.
     */
    private Rectangle cropZoneRect;

    /**
     * Used for mouse dragging.
     * This is every time the initial X coordinate of the mouse
     * and the coordinates are according the image.
     */
    private int mouseStartX;

    /**
     * Used for mouse dragging.
     * This is every time the initial Y coordinate of the mouse
     * and the coordinates are according the image.
     */
    private int mouseStartY;

    /**
     * Construct an new image cropper
     *
     * @param cropZoneWidth the width of the crop zone
     * @param cropZoneHeight the height of the crop zone
     */
    public ImageClipper(int cropZoneWidth, int cropZoneHeight)
    {
        this.cropZoneRect = new Rectangle(cropZoneWidth, cropZoneHeight);
        updateCropZone();

        Dimension d = new Dimension(320, 240);

        this.setSize(d);
        this.setMinimumSize(d);
        this.setPreferredSize(d);

        this.addMouseListener(this);
        this.addMouseMotionListener(this);
    }

    /**
     * Compute static values of the cropping zone
     */
    private void updateCropZone()
    {
        this.cropZoneRect.x =
            (this.getWidth() / 2) - (this.cropZoneRect.width / 2);
        this.cropZoneRect.y =
            (this.getHeight() / 2) - (this.cropZoneRect.height / 2);
    }

    /**
     * Defines the image to be cropped
     *
     * @param image the image to be cropped
     */
    public void setImage(BufferedImage image)
    {
        this.image = image;

        this.imageRect.width = image.getWidth(this);
        this.imageRect.height = image.getHeight(this);
        // put the image in the center
        this.imageRect.x = (this.getWidth() - this.imageRect.width)/2;
        this.imageRect.y = (this.getHeight() - this.imageRect.height)/2;

        // set the initial values
        this.lastComponentHeight = this.getHeight();
        this.lastComponentWidth = this.getWidth();

        updateImagePoints();

        this.repaint();
    }

    /**
     * Update image points if needed, when component is resized.
     */
    private void updateImagePoints()
    {
        if(lastComponentWidth != this.getWidth())
        {
            this.imageRect.x += (this.getWidth() - lastComponentWidth)/2;
            lastComponentWidth = this.getWidth();
        }

        if(lastComponentHeight != this.getHeight())
        {
            this.imageRect.y += (this.getHeight() - lastComponentHeight)/2;
            lastComponentHeight = this.getHeight();
        }
    }

    /**
     * Returns the cropped area of the image
     *
     * @return the cropped area
     */
    public Rectangle getCroppedArea()
    {
        Rectangle croppedArea = new Rectangle();

        croppedArea.setSize(this.cropZoneRect.getSize());
        croppedArea.x = this.cropZoneRect.x - this.imageRect.x;
        croppedArea.y = this.cropZoneRect.y - this.imageRect.y;

        return croppedArea;
    }

    /**
     * Paint the component with the image we have and the settings
     * we have for it.
     * @param g the graphics to draw.
     */
    @Override
    protected void paintComponent(Graphics g)
    {
        super.paintComponent(g);

        g = g.create();
        AntialiasingManager.activateAntialiasing(g);

        // Draw image
        updateImagePoints();
        g.drawImage(this.image, this.imageRect.x, this.imageRect.y,
                this.imageRect.width, this.imageRect.height, this);

        // Select rect
        updateCropZone();
        g.setColor(Color.BLACK);
        g.drawRect(this.cropZoneRect.x, this.cropZoneRect.y,
                this.cropZoneRect.width, this.cropZoneRect.height);

        // Image overlay
        drawImageOverlay(g);

        // Image border
        g.setColor(IMAGE_BORDER_COLOR);
        g.drawRoundRect(this.imageRect.x-2, this.imageRect.y-2,
                this.imageRect.width+3, this.imageRect.height+3, 2, 2);
        g.drawRoundRect(this.imageRect.x-1, this.imageRect.y-1,
                this.imageRect.width+1, this.imageRect.height+1, 2, 2);
    }

    /**
     * Draw an overlay over the parts of the images
     * which are not in the crop zone
     *
     * @param g the Graphics used to draw
     */
    private void drawImageOverlay(Graphics g)
    {
        int width, height;

        g.setColor(IMAGE_OVERLAY_COLOR);

        // left vertical non cropped part
        width = this.cropZoneRect.x - this.imageRect.x;
        if (width > 0)
        {
            g.fillRect(this.imageRect.x, this.imageRect.y,
                    width, this.imageRect.height);
        }

        // right vertical non cropped
        width = this.imageRect.x + this.imageRect.width
                - (this.cropZoneRect.x + this.cropZoneRect.width);
        if (width > 0)
        {
            g.fillRect(
                this.cropZoneRect.x + this.cropZoneRect.width,
                this.imageRect.y,
                width,
                this.imageRect.height);
        }

        // Top horizontal non croppped part
        height = this.cropZoneRect.y - this.imageRect.y;
        if (height > 0)
        {
            g.fillRect(this.cropZoneRect.x, this.imageRect.y,
                    this.cropZoneRect.width, height);
        }

        // Bottom horizontal non croppped part
        height = (this.imageRect.y + this.imageRect.height)
            - (this.cropZoneRect.y + this.cropZoneRect.height);
        if (height > 0)
        {
            g.fillRect(
                this.cropZoneRect.x,
                this.cropZoneRect.y + this.cropZoneRect.height,
                this.cropZoneRect.width,
                height);
        }
    }

    /**
     * Start image cropping action.
     * @param e the mouse event, initial clicking.
     */
    public void mousePressed(MouseEvent e)
    {
        // Init the dragging
        mouseStartX = e.getX();
        mouseStartY = e.getY();
    }

    /**
     * Event that user is dragging the mouse.
     * @param e the mouse event.
     */
    public void mouseDragged(MouseEvent e)
    {
        // New position of the image
        int newXpos = this.imageRect.x + e.getX() - mouseStartX;
        int newYpos = this.imageRect.y + e.getY() - mouseStartY;

        if(newXpos <= cropZoneRect.x
           && newXpos + imageRect.width
                >= cropZoneRect.x + cropZoneRect.width)
        {
            this.imageRect.x = newXpos;
            mouseStartX = e.getX();
        }

        if(newYpos < cropZoneRect.y
           && newYpos + imageRect.height
                >= cropZoneRect.y + cropZoneRect.height)
        {
            this.imageRect.y = newYpos;
            mouseStartY = e.getY();
        }

        this.repaint();
    }

    /**
     * Not used.
     * @param e
     */
    public void mouseClicked(MouseEvent e) {}

    /**
     * Not used.
     * @param e
     */
    public void mouseEntered(MouseEvent e) {}

    /**
     * Not used.
     * @param e
     */
    public void mouseExited(MouseEvent e) {}

    /**
     * Not used.
     * @param e
     */
    public void mouseReleased(MouseEvent e) {}

    /**
     * Not used.
     * @param e
     */
    public void mouseMoved(MouseEvent e) {}
}
