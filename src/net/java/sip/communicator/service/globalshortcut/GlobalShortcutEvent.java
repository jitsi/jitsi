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
     * Initializes a new <tt>GlobalShortcutEvent</tt>.
     *
     * @param keyStroke keystroke
     */
    public GlobalShortcutEvent(AWTKeyStroke keyStroke)
    {
        this.keyStroke = keyStroke;
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
}
