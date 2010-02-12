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
     * Capture the full desktop screen using native grabber.
     *
     * Contrary to other captureScreen method, it only returns raw bytes
     * and not <tt>BufferedImage</tt>. It is done in order to limit
     * slow operation such as converting ARGB images (uint32_t) to bytes
     * especially for big big screen. For example a 1920x1200 desktop consumes
     * 9 MB of memory for grabbing and another 9 MB array for convertion operation.
     *
     * @param output output buffer to store bytes in.
     * Be sure that output length is sufficient
     * @return true if success, false if JNI error or output length too short
     */
    public boolean captureScreen(byte output[])
    {
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();

        return captureScreen(0, 0, (int)dim.getWidth(), (int)dim.getHeight(), output);
    }

    /**
     * Capture a part of the desktop screen using native grabber.
     *
     * Contrary to other captureScreen method, it only returns raw bytes
     * and not <tt>BufferedImage</tt>. It is done in order to limit
     * slow operation such as converting ARGB images (uint32_t) to bytes 
     * especially for big big screen. For example a 1920x1200 desktop consumes
     * 9 MB of memory for grabbing and another 9 MB array for convertion operation.
     *
     * @param x x position to start capture
     * @param y y position to start capture
     * @param width capture width
     * @param height capture height
     * @param output output buffer to store bytes in.
     * Be sure that output length is sufficient
     * @return true if success, false if JNI error or output length too short
     */
    public boolean captureScreen(int x, int y, int width, int height, byte output[])
    {
        if(OSUtils.IS_LINUX || OSUtils.IS_FREEBSD || OSUtils.IS_WINDOWS
                || OSUtils.IS_MAC)
        {
            return NativeScreenCapture.grabScreen(
                        x, y, width, height, output);
        }

        return false;
    }

    /**
     * Capture the full desktop screen using <tt>java.awt.Robot</tt>.
     *
     * @return <tt>BufferedImage</tt> of the desktop screen
     */
    public BufferedImage captureScreen()
    {
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();

        return captureScreen(0, 0, (int)dim.getWidth(), (int)dim.getHeight());
    }

    /**
     * Capture a part of the desktop screen using <tt>java.awt.Robot</tt>.
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
           
        if(robot == null)
        {
            /* Robot has not been created so abort */
            return null;
        }

        logger.info("Begin capture: " + System.nanoTime());
        rect = new Rectangle(x, y, width, height);
        img = robot.createScreenCapture(rect);
        logger.info("End capture: " + System.nanoTime());
        
        return img;
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
