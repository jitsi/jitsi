/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.presence.avatar.imagepicker;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;

import javax.swing.*;

import net.java.sip.communicator.util.swing.*;

/**
 * Component allowing the user to easily clip an image
 * 
 * @author Damien Roth
 */
public class ImageClipper
    extends JComponent
    implements MouseListener, MouseMotionListener
{
    private static final int CLIP_PANEL_WIDTH = 320;
    private static final int CLIP_PANEL_HEIGHT = 240;
    
    private static final Color IMAGE_BORDER_COLOR
            = new Color(174, 189, 215);
    private static final Color IMAGE_OVERLAY_COLOR
            = new Color(1.0f, 1.0f, 1.0f, 0.4f);
    
    private BufferedImage image = null;
    private Rectangle imageRect;
    private Point imageBottomRight;
    
    private Rectangle clippingZoneRect;
    private Point clippingZoneBottomRight;
    
    // Mouse drag vars
    private int mouseStartX;
    private int mouseStartY;
    private int xInit;
    private int yInit;
    
    /**
     * Construct an new image clipper
     * 
     * @param cropZoneWidth the width of the clip zone
     * @param cropZoneHeight the height of the clip zone
     */
    public ImageClipper(int cropZoneWidth, int cropZoneHeight)
    {
        Dimension d = new Dimension(CLIP_PANEL_WIDTH, CLIP_PANEL_HEIGHT);
        
        this.setSize(d);
        this.setMaximumSize(d);
        this.setMinimumSize(d);
        this.setPreferredSize(d);
        
        this.initClippingZone(cropZoneWidth, cropZoneHeight);
        this.imageRect = new Rectangle(this.clippingZoneRect.getLocation());
        this.imageBottomRight = new Point(0,0);
        
        this.addMouseListener(this);
        this.addMouseMotionListener(this);
    }
    
    /**
     * Compute static values of the clipping zone
     * 
     * @param width the width of the clipping zone
     * @param height the height of the clipping zone
     */
    private void initClippingZone(int width, int height)
    {
        this.clippingZoneRect = new Rectangle(width, height);
        this.clippingZoneRect.x = (CLIP_PANEL_WIDTH / 2) - (width / 2);
        this.clippingZoneRect.y = (CLIP_PANEL_HEIGHT / 2) - (height / 2);
        
        this.clippingZoneBottomRight = new Point(
                this.clippingZoneRect.x + this.clippingZoneRect.width,
                this.clippingZoneRect.y + this.clippingZoneRect.height
        );
    }
    
    /**
     * Defines the image to be clipped
     * 
     * @param image the image to be clipped
     */
    public void setImage(BufferedImage image)
    {
        boolean updated = false;
        this.image = image;
        
        this.imageRect.width = image.getWidth(this);
        this.imageRect.height = image.getHeight(this);
        
        this.imageBottomRight.x = this.imageRect.x + this.imageRect.width;
        this.imageBottomRight.y = this.imageRect.y + this.imageRect.height;
        
        if (this.imageBottomRight.x < this.clippingZoneBottomRight.x)
        {
            this.imageRect.x +=
                    this.clippingZoneBottomRight.x - this.imageBottomRight.x;
            updated = true;
        }
        if (this.imageBottomRight.y < this.clippingZoneBottomRight.y)
        {
            this.imageRect.y +=
                    this.clippingZoneBottomRight.y - this.imageBottomRight.y;
            updated = true;
        }
        
        if (updated)
        {
            this.imageBottomRight.x = this.imageRect.x + this.imageRect.width;
            this.imageBottomRight.y = this.imageRect.y + this.imageRect.height;
        }
        
        this.repaint();
    }
    
    /**
     * Returns the clipped area of the image
     * 
     * @return the clipped area
     */
    public Rectangle getClipping()
    {
        Rectangle clipping = new Rectangle();
        
        clipping.setSize(this.clippingZoneRect.getSize());
        clipping.x = this.clippingZoneRect.x - this.imageRect.x;
        clipping.y = this.clippingZoneRect.y - this.imageRect.y;
        
        return clipping;
    }
    
    @Override
    protected void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        
        g = g.create();
        AntialiasingManager.activateAntialiasing(g);
        
        // Draw image
        g.drawImage(this.image, this.imageRect.x, this.imageRect.y,
                this.imageRect.width, this.imageRect.height, this);
        
        // Image overlay
        drawImageOverlay(g);
        
        // Image border
        g.setColor(ImageClipper.IMAGE_BORDER_COLOR);
        g.drawRoundRect(this.imageRect.x-2, this.imageRect.y-2,
                this.imageRect.width+3, this.imageRect.height+3, 2, 2);
        g.drawRoundRect(this.imageRect.x-1, this.imageRect.y-1,
                this.imageRect.width+1, this.imageRect.height+1, 2, 2);
        
        // Select rect
        g.setColor(Color.BLACK);
        g.drawRect(this.clippingZoneRect.x, this.clippingZoneRect.y,
                this.clippingZoneRect.width, this.clippingZoneRect.height);
    }
    
    /**
     * Draw an overlay over the parts of the images which are not in the clip zone
     * 
     * @param g the Graphics used to draw
     */
    private void drawImageOverlay(Graphics g)
    {
        int width, height;
        
        g.setColor(ImageClipper.IMAGE_OVERLAY_COLOR);
        
        width = this.clippingZoneRect.x - this.imageRect.x;
        if (width > 0)
            g.fillRect(this.imageRect.x, this.imageRect.y,
                    width, this.imageRect.height);
        
        width = this.imageRect.x + this.imageRect.width
                - this.clippingZoneBottomRight.x;
        if (width > 0)
            g.fillRect(this.clippingZoneBottomRight.x, this.imageRect.y,
                    width, this.imageRect.height);
        
        // Top
        height = this.clippingZoneRect.y - this.imageRect.y;
        if (height > 0)
            g.fillRect(this.clippingZoneRect.x, this.imageRect.y,
                    this.clippingZoneRect.width, height);
        
        // Bottom
        height = (this.imageRect.y + this.imageRect.height)
                - (this.clippingZoneBottomRight.y);
        if (height > 0)
            g.fillRect(this.clippingZoneRect.x, this.clippingZoneBottomRight.y,
                    this.clippingZoneRect.width, height);
    }

    public void mousePressed(MouseEvent e)
    {
        // Init the dragging
        mouseStartX = e.getX();
        mouseStartY = e.getY();
        xInit = this.imageRect.x;
        yInit = this.imageRect.y;
    }

    public void mouseDragged(MouseEvent e)
    {
        // New position of the image
        int xpos = xInit + (e.getX() - mouseStartX);
        int ypos = yInit + (e.getY() - mouseStartY);
        
        // Checks if the image doesn't go out of the clip zone
        if (xpos <= this.clippingZoneRect.x && xpos
                + this.imageRect.width > this.clippingZoneBottomRight.x)
            this.imageRect.x = xpos;

        if (ypos <= this.clippingZoneRect.y && ypos
                + this.imageRect.height > this.clippingZoneBottomRight.y)
            this.imageRect.y = ypos;
        
        this.repaint();
    }

    public void mouseClicked(MouseEvent e) {}

    public void mouseEntered(MouseEvent e) {}

    public void mouseExited(MouseEvent e) {}

    public void mouseReleased(MouseEvent e) {}
    
    public void mouseMoved(MouseEvent e) {}
}
