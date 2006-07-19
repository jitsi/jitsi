/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.customcontrols;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Window;
import java.awt.image.BufferedImage;

import javax.swing.JComponent;

import net.java.sip.communicator.impl.gui.utils.AntialiasingManager;
import net.java.sip.communicator.impl.gui.utils.Constants;

/**
 * The <tt>TransparentBackground</tt> is a <tt>JComponent</tt>, which is
 * added to a <tt>Window</tt> in order to make it transparent.
 * <p>
 * <b>How to use the <tt>TransparentBackground</tt>?</b>
 * The <tt>TransparentBackground</tt> is created and added to the content pane
 * of the <tt>Window</tt> that should be made transparent. All other components
 * then are added to the <tt>TransparentBackground</tt> component and not
 * directly to the window content pane.
 * <p>
 * <b>How it works?</b>
 * The <tt>TransparentBackground</tt> is a <tt>JComponent</tt> which is not
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
public class TransparentBackground extends JComponent {

    private BufferedImage background;

    private Robot robot;

    private Window window;

    /**
     * Creates an instance of <tt>TransparentBackground</tt> by specifying
     * the parent <tt>Window</tt> - this is the window that should be made
     * transparent.
     * 
     * @param window The parent <tt>Window</tt>
     */
    public TransparentBackground(Window window) {

        this.window = window;

        try {

            robot = new Robot();

        } catch (AWTException e) {

            e.printStackTrace();

            return;

        }
    }

    /**
     * Updates the background. Makes a new screen capture at the given
     * coordiantes.
     * 
     * @param x The x coordinate.
     * @param y The y coordinate.
     */
    public void updateBackground(int x, int y) {

        this.background = robot.createScreenCapture(new Rectangle(x, y, x
                + this.window.getWidth(), y + this.window.getHeight()));

    }

    /**
     * Overrides the <code>paintComponent</code> method in <tt>JComponent</tt>
     * to paint the screen capture image as a background of this component.
     */
    protected void paintComponent(Graphics g) {

        AntialiasingManager.activateAntialiasing(g);

        Graphics2D g2 = (Graphics2D) g;

        g2.drawImage(this.background, 0, 0, null);

        g2.setColor(new Color(255, 255, 255, 180));

        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);

        g2.setColor(Constants.LIGHT_GRAY_COLOR);

        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
    }
}