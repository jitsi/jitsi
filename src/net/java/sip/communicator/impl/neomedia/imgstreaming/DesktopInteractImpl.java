/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.imgstreaming;

import java.awt.*;
import java.awt.image.*;

import net.java.sip.communicator.util.*;

/**
 * This singleton class provide screen capture and key/mouse 
 * events generation by wrapping partial or all <tt>java.awt.Robot</tt>
 * methods to interact with desktop.
 *
 * @see java.awt.Robot
 * @author Sebastien Vincent
 */
public class DesktopInteractImpl implements DesktopInteract 
{
    /**
     * The <tt>Logger</tt>.
     */
    private static final Logger logger = Logger.getLogger(DesktopInteractImpl.class);

    /**
     * Screen capture robot.
     */
    private Robot robot = null;

    /**
     * The unique instance of this class (singleton).
     */
    private static DesktopInteractImpl instance = null;

    /**
     * Constructor.
     * 
     * @throws AWTException if platform configuration does not allow low-level input control
     * @throws SecurityException if Robot creation is not permitted
     */
    public DesktopInteractImpl() throws AWTException, SecurityException 
    {
        robot = new Robot();
    }

    /**
     * Capture the full desktop screen.
     *
     * @return <tt>BufferedImage</tt> of the desktop screen
     */
    public BufferedImage captureScreen()
    {
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();

        return captureScreen(0, 0, (int)dim.getWidth(), (int)dim.getHeight());
    }

    /**
     * Capture a part of the desktop screen.
     *
     * @param x x position to start capture
     * @param y y position to start capture
     * @param width capture width
     * @param height capture height
     * @return <tt>BufferedImage</tt> of a part of the desktop screen
     * or null if Robot problem
     */
    public BufferedImage captureScreen(int x, int y, int width, int height)
    {
        BufferedImage img = null;
        Rectangle rect = null;

        if(OSUtils.IS_LINUX || OSUtils.IS_FREEBSD || OSUtils.IS_WINDOWS)
        {
            return NativeScreenCapture.captureScreen(x, y, width, height);
        }
        else
        {
            /* Robot has not been created so abort */
            if(robot == null)
            {
                return null;
            }

            logger.info("Begin capture: " + System.nanoTime());
            rect = new Rectangle(x, y, width, height);
            img = robot.createScreenCapture(rect);
            logger.info("End capture: " + System.nanoTime());
            return img;
        }
    }

    /**
     * Generates keyPress event.
     *
     * @param keycode keycode the user hit
     */
    public void keyPress(int keycode)
    {
        robot.keyPress(keycode);
    }

    /**
     * Generates keyRelease event.
     *
     * @param keycode keycode the user hit
     */
    public void keyRelease(int keycode)
    {
        robot.keyRelease(keycode);
    }

    /**
     * Generates mouseMove event.
     *
     * @param x position x in the screen
     * @param y position y in the screen
     */
    public void mouseMove(int x, int y)
    {
        robot.mouseMove(x, y);
    }

    /**
     * Generates mousePress event.
     *
     * @param buttons buttons mask (right, middle, left)
     */
    public void mousePress(int buttons)
    {
        robot.mousePress(buttons);
    }

    /**
     * Generates mouseRelease event.
     *
     * @param buttons buttons mask (right, middle, left)
     */
    public void mouseRelease(int buttons)
    {
        robot.mouseRelease(buttons);
    }

    /**
     * Generates mouseWheel event.
     *
     * @param wheelAmt "notches"
     */
    public void mouseWheel(int wheelAmt)
    {
        robot.mouseWheel(wheelAmt);
    }
}
