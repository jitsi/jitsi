/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.globalshortcut;

import java.awt.*;

/**
 * Event related to global shortcut.
 *
 * @author Sebastien Vincent
 */
public class GlobalShortcutEvent
{
    /**
     * Key stroke.
     */
    private final AWTKeyStroke keyStroke;

    /**
     * Shows the event type:
     * pressed is false
     * released is true
     */
    private final boolean isReleased;

    /**
     * Initializes a new <tt>GlobalShortcutEvent</tt>.
     *
     * @param keyStroke keystroke
     */
    public GlobalShortcutEvent(AWTKeyStroke keyStroke)
    {
        this.keyStroke = keyStroke;
        isReleased = false;
    }

    /**
     * Initializes a new <tt>GlobalShortcutEvent</tt>.
     *
     * @param keyStroke keystroke
     * @param isRelease if the event is for release this parameter is true
     * else this parameter is false
     */
    public GlobalShortcutEvent(AWTKeyStroke keyStroke, boolean isReleased)
    {
        this.keyStroke = keyStroke;
        this.isReleased = isReleased;
    }

    /**
     * Returns keyStroke.
     *
     * @return keystroke
     */
    public AWTKeyStroke getKeyStroke()
    {
        return keyStroke;
    }

    /**
     * Returns isReleased.
     *
     * @return release flag of the event
     */
    public boolean isReleased()
    {
        return this.isReleased;
    }
}
