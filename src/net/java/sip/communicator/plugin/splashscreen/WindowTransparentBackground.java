/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.plugin.splashscreen;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;

import javax.imageio.*;
import javax.swing.*;

/**
 * The <tt>WindowBackground</tt> is a <tt>JComponent</tt>, which is
 * added to a <tt>Window</tt> in order to make it transparent.
 * <p>
 * <b>How to use the <tt>WindowBackground</tt>?</b>
 * The <tt>WindowBackground</tt> is created and added to the content pane
 * of the <tt>Window</tt> that should be made transparent. All other components
 * then are added to the <tt>WindowBackground</tt> component and not
 * directly to the window content pane.
 * <p>
 * <b>How it works?</b>
 * The <tt>WindowBackground</tt> is a <tt>JComponent</tt> which is not
 * really transparent, but only looks like. It overrides the
 * <code>paintComponent</code> method of <tt>JComponent</tt> to paint its
 * own background image, which is an exact image of the screen at the position
 * where the window will apear and with the same size. The
 * <tt>java.awt.Robot</tt> class is used to make the screen capture.
 * <p>
 * Note that the effect of transparence is gone when behind there is an
 * application which shows dynamic images or something the moves, like
 * a movie for example. 
 * 
 * @author Yana Stamcheva
 */

public class WindowTransparentBackground
    extends JPanel
    implements ActionListener
{    
    private BufferedImage bg1;

    private BufferedImage bg2;
    
    private BufferedImage bg3;
    
    private BufferedImage bg4;
    
    private Robot robot;

    private Image bgImage;
    
    private Timer refreshBgTimer;

    private int bgX;
    
    private int bgY;
    
    private int bgWidth;
    
    private int bgHeight;
    
    /**
     * Creates an instance of <tt>WindowBackground</tt> by specifying
     * the parent <tt>Window</tt> - this is the window that should be made
     * transparent.
     * 
     * @param window The parent <tt>Window</tt>
     */
    public WindowTransparentBackground() {
        
        try {
            robot = new Robot();
            
            bgImage = ImageIO.read(
                WindowBackground.class.getResource("aboutWindowBackground.png"));
            
            bgWidth = bgImage.getWidth(null);
            
            bgHeight = bgImage.getHeight(null);
            
            bgX = Toolkit.getDefaultToolkit().getScreenSize().width/2
                - bgWidth/2;

            bgY = Toolkit.getDefaultToolkit().getScreenSize().width/2
                - bgHeight/2;
            
        } catch (AWTException e) {

            e.printStackTrace();

            return;
        }
        catch (IOException e)
        {
            e.printStackTrace();
            
            return;
        }
        
        this.updateBackground(bgX, bgY, bgWidth, bgHeight);
        
        refreshBgTimer = new Timer(4000, this);
        
        refreshBgTimer.setRepeats(true);
        refreshBgTimer.start();
    }

    /**
     * Updates the background. Makes a new screen capture at the given
     * coordiantes.
     * 
     * @param x The x coordinate.
     * @param y The y coordinate.
     */
    public void updateBackground(int x, int y, int w, int h) {

        this.bg1 = robot.createScreenCapture(new Rectangle(x, y, 30, 30));
        
        this.bg2 = robot.createScreenCapture(new Rectangle(w - 30, y, 30, 30));
        
        this.bg3 = robot.createScreenCapture(new Rectangle(x, h - 30, 30, 30));
        
        this.bg4 = robot.createScreenCapture(new Rectangle(w - 30, y - 30, 30, 30));
    }

    public void stopRefresh()
    {
        refreshBgTimer.stop();
        this.robot = null;
    }
    
    
    /**
     * Overrides the <code>paintComponent</code> method in <tt>JComponent</tt>
     * to paint the screen capture image as a background of this component.
     */
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        Graphics2D g2 = (Graphics2D) g;

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        g2.drawImage(this.bg1, 0, 0, null);
        g2.drawImage(this.bg2, bgWidth - 30, 0, null);
        g2.drawImage(this.bg3, 0, bgHeight - 30, null);
        g2.drawImage(this.bg4, bgWidth - 30, bgHeight - 30, null);
        
        g2.drawImage(bgImage, 1, 1, null);
        
        g2.setColor(new Color(255, 255, 255, 130));

        g2.fillRoundRect(0, 0, bgWidth, bgHeight, 40, 40);
    }

    public void actionPerformed(ActionEvent e)
    {
        this.updateBackground(bgX, bgY, bgWidth, bgHeight);
        
        this.revalidate();
        this.repaint();
    }
}