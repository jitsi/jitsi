/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.globalshortcut;

import net.java.sip.communicator.util.*;

/**
 * Native hook for keyboard. It is used to notify a
 * <tt>NativeKeyboardHookDelegate</tt> for key (even if key are pressed when
 * application is not in foreground).
 *
 * @author Sebastien Vincent
 */
public class NativeKeyboardHook
{
    /**
     * The <tt>Logger</tt> used by the <tt>NativeKeyboardHook</tt> class and its
     * instances for logging output.
     */
    private static final Logger logger = Logger.getLogger(
        NativeKeyboardHook.class);

    /**
     * If it is started.
     */
    private boolean isStarted = false;

    /**
     * Native pointer.
     */
    private static long ptr = 0;

    /**
     * If the special key detection is enable.
     */
    private boolean specialKeydetection = false;

    /**
     * Constructor.
     */
    public NativeKeyboardHook()
    {
    }

    /**
     * Start the <tt>NativeKeyboardHook</tt>.
     */
    public synchronized void start()
    {
        if(!isStarted && ptr != 0)
        {
            isStarted = true;
            start(ptr);
        }
    }

    /**
     * Stop the <tt>NativeKeyboardHook</tt>.
     */
    public synchronized void stop()
    {
        if(isStarted && ptr != 0)
        {
            isStarted = false;
            stop(ptr);
        }
    }

    /**
     * Set delegate object for event notification.
     *
     * @param delegate delegate object
     */
    public synchronized void setDelegate(NativeKeyboardHookDelegate delegate)
    {
        if(ptr != 0)
            setDelegate(ptr, delegate);
    }

    /**
     * Register a shortcut.
     *
     * @param keycode keycode of the shortcut
     * @param modifiers modifiers (CTRL, ALT, ...)
     * @return true if success, false otherwise
     */
    public synchronized boolean registerShortcut(int keycode,
        int modifiers)
    {
        if(ptr != 0)
            return registerShortcut(ptr, keycode, modifiers);

        return false;
    }

    /**
     * Unregister a shortcut.
     *
     * @param keycode keycode of the shortcut
     * @param modifiers modifiers (CTRL, ALT, ...)
     */
    public synchronized void unregisterShortcut(int keycode,
        int modifiers)
    {
        if(ptr != 0)
            unregisterShortcut(ptr, keycode, modifiers);
    }

    /**
     * Register a special key shortcut (for example key coming from headset).
     *
     * @param keycode keycode of the shortcut
     * @param modifiers modifiers (CTRL, ALT, ...)
     * @return true if success, false otherwise
     */
    public synchronized boolean registerSpecial(int keycode)
    {
        if(ptr != 0)
            return registerSpecial(ptr, keycode);

        return false;
    }

    /**
     * Unregister a special key shortcut (for example key coming from headset).
     *
     * @param keycode keycode of the shortcut
     * @param modifiers modifiers (CTRL, ALT, ...)
     */
    public synchronized void unregisterSpecial(int keycode)
    {
        if(ptr != 0)
            unregisterSpecial(ptr, keycode);
    }

    /**
     * Detect special key press.
     *
     * @param enable enable or not the special key press detection.
     */
    public synchronized void detectSpecialKeyPress(boolean enable)
    {
        if(ptr != 0)
        {
            detectSpecialKeyPress(ptr, enable);
            this.specialKeydetection = enable;
        }
    }

    /**
     * Returns whether or not special key detection is enabled.
     *
     * @return true if special key detection is enabled, false otherwise
     */
    public boolean isSpecialKeyDetection()
    {
        return specialKeydetection;
    }

    /**
     * Native method to initialize <tt>NativeKeyboardHook</tt>.
     *
     * @return native pointer.
     */
    private static native long init();

    /**
     * Native method to start <tt>NativeKeyboardHook</tt>.
     *
     * @param ptr native pointer
     */
    private static native void start(long ptr);

    /**
     * Native method to stop <tt>NativeKeyboardHook</tt>.
     *
     * @param ptr native pointer
     */
    private static native void stop(long ptr);

    /**
     * Native method to set the delegate object.
     *
     * @param ptr native pointer
     * @param delegate delegate object to set
     */
    private static native void setDelegate(long ptr,
            NativeKeyboardHookDelegate delegate);

    /**
     * Native method to register a shortcut.
     *
     * @param ptr native pointer
     * @param keycode keycode of the shortcut
     * @param modifiers modifiers (CTRL, ALT, ...)
     * @return true if registration is successful, false otherwise
     */
    private static native boolean registerShortcut(long ptr, int keycode,
        int modifiers);

    /**
     * Native method to unregister a shortcut.
     *
     * @param ptr native pointer
     * @param keycode keycode of the shortcut
     * @param modifiers modifiers (CTRL, ALT, ...)
     */
    private static native void unregisterShortcut(long ptr, int keycode,
        int modifiers);

    /**
     * Native method to register a special key shortcut (for example key coming
     * from a headset).
     *
     * @param ptr native pointer
     * @param keycode keycode of the shortcut
     * @return true if registration is successful, false otherwise
     */
    private static native boolean registerSpecial(long ptr, int keycode);

    /**
     * Native method to unregister a special key shortcut (for example key
     * coming from a headset).
     *
     * @param ptr native pointer
     * @param keycode keycode of the shortcut
     */
    private static native void unregisterSpecial(long ptr, int keycode);

    /**
     * Native method to ook for special key press.
     *
     * @param ptr native pointer
     * @param enable enable or not the special key press detection.
     */
    private static native void detectSpecialKeyPress(long ptr, boolean enable);

    static
    {
        try
        {
            System.loadLibrary("globalshortcut");
            ptr = init();
        }
        catch(Exception e)
        {
            logger.warn("Failed to load globalshortcut", e);
            ptr = 0;
        }
    }
}
