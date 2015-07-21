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

package net.java.sip.communicator.impl.hid;

import java.awt.*;
import java.awt.event.*;

import net.java.sip.communicator.service.hid.*;
import net.java.sip.communicator.util.Logger;

import org.jitsi.util.*;

/**
 * Implementation of the HID service to provide way of regenerate key press
 * and mouse interactions.
 *
 * @author Sebastien Vincent
 */
public class HIDServiceImpl implements HIDService
{
    /**
     * The <tt>Logger</tt> used by the <tt>NeomediaActivator</tt> class and its
     * instances for logging output.
     */
    private final Logger logger = Logger.getLogger(HIDServiceImpl.class);

    /**
     * The robot used to perform some operations (mouse/key interactions).
     */
    private Robot robot = null;

    /**
     * Object to regenerates keys with JNI.
     */
    private NativeKeyboard nativeKeyboard = null;

    /**
     * Constructor.
     */
    protected HIDServiceImpl()
    {
        try
        {
            robot = new Robot();
            nativeKeyboard = new NativeKeyboard();
        }
        catch(Throwable e)
        {
            if(!GraphicsEnvironment.isHeadless())
                logger.error(
                    "Error when creating Robot/NativeKeyboard instance", e);
        }
    }

    /**
     * Press a specific key using its keycode.
     *
     * @param keycode the Java keycode, all available keycodes can be found
     * in java.awt.event.KeyEvent class (VK_A, VK_SPACE, ...)
     * @see java.awt.event.KeyEvent
     */
    public void keyPress(int keycode)
    {
        if(OSUtils.IS_WINDOWS || OSUtils.IS_MAC)
        {
            /* do not allow modifiers for Windows (as
             * they are handled in native code with
             * VkScanCode) and Mac OS X
             */
            if(keycode == KeyEvent.VK_ALT ||
                    keycode == KeyEvent.VK_SHIFT ||
                    keycode == KeyEvent.VK_ALT_GRAPH)
            {
                return;
            }
        }

        /* AltGr does not seems to work with robot, handle it via our
         * JNI code
         */
        if(keycode == KeyEvent.VK_ALT_GRAPH)
        {
            symbolPress("altgr");
        }
        else
        {
            robot.keyPress(keycode);
        }
    }

    /**
     * Release a specific key using its keycode.
     *
     * @param keycode the Java keycode, all available keycode can be found
     * in java.awt.event.KeyEvent class (VK_A, VK_SPACE, ...)
     * @see java.awt.event.KeyEvent
     */
    public void keyRelease(int keycode)
    {
        /* AltGr does not seems to work with robot, handle it via our
         * JNI code
         */
        if(keycode == KeyEvent.VK_ALT_GRAPH)
        {
            symbolRelease("altgr");
        }
        else
        {
            robot.keyRelease(keycode);
        }
    }

    /**
     * Press a specific key using its char representation.
     *
     * @param key char representation of the key
     */
    public void keyPress(char key)
    {
        /* check for CTRL+X where X is [A-Z]
         * CTRL+A = 1, A = 65
         */
        if(key >= 1 && key <= 0x1A)
        {
            key = (char)(key + 64);
            robot.keyPress(key);
            return;
        }

        nativeKeyboard.keyPress(key);
    }

    /**
     * Release a specific key using its char representation.
     *
     * @param key char representation of the key
     */
    public void keyRelease(char key)
    {
        /* check for CTRL+X where X is [A-Z]
         * CTRL+A = 1, A = 65
         */
        if(key >= 1 && key <= 0x1A)
        {
            key = (char)(key + 64);
            robot.keyRelease(key);
            return;
        }

        if(nativeKeyboard != null)
            nativeKeyboard.keyRelease(key);
    }

    /**
     * Press a specific symbol (such as SHIFT or CTRL).
     *
     * @param symbol symbol name
     */
    private void symbolPress(String symbol)
    {
        if(nativeKeyboard != null)
            nativeKeyboard.symbolPress(symbol);
    }

    /**
     * Release a specific symbol (such as SHIFT or CTRL).
     *
     * @param symbol symbol name
     */
    private void symbolRelease(String symbol)
    {
        if(nativeKeyboard != null)
            nativeKeyboard.symbolRelease(symbol);
    }

    /**
     * Press a mouse button(s).
     *
     * @param btns button masks
     * @see java.awt.Robot#mousePress(int btns)
     */
    public void mousePress(int btns)
    {
        robot.mousePress(btns);
    }

    /**
     * Release a mouse button(s).
     *
     * @param btns button masks
     * @see java.awt.Robot#mouseRelease(int btns)
     */
    public void mouseRelease(int btns)
    {
        robot.mouseRelease(btns);
    }

    /**
     * Move the mouse on the screen.
     *
     * @param x x position on the screen
     * @param y y position on the screen
     * @see java.awt.Robot#mouseMove(int x, int y)
     */
    public void mouseMove(int x, int y)
    {
        robot.mouseMove(x, y);
    }

    /**
     * Release a mouse button(s).
     *
     * @param rotation wheel rotation (could be negative or positive depending
     * on the direction).
     * @see java.awt.Robot#mouseWheel(int wheelAmt)
     */
    public void mouseWheel(int rotation)
    {
        robot.mouseWheel(rotation);
    }
}
