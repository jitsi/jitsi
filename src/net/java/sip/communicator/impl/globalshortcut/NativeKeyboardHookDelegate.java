/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.globalshortcut;

/**
 * NativeKeyboardHookDelegate interface.
 *
 * @author Sebastien Vincent
 */
public interface NativeKeyboardHookDelegate
{
    /**
     * CTRL modifier.
     */
    public static final int MODIFIERS_CTRL = 1;

    /**
     * ALT modifier.
     */
    public static final int MODIFIERS_ALT = 2;

    /**
     * SHIFT modifier.
     */
    public static final int MODIFIERS_SHIFT = 4;

    /**
     * Logo modifier (i.e. CMD/Apple key on Mac OS X, Windows key on
     * MS Windows).
     */
    public static final int MODIFIERS_LOGO = 8;

   /**
     * Receive a key press event.
     *
     * @param keycode keycode received
     * @param modifiers modifiers received (ALT or CTRL + letter, ...)
     */
    public void receiveKey(int keycode, int modifiers);
}