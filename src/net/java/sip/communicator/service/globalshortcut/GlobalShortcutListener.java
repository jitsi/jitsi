/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.globalshortcut;

/**
 * Global shortcut listener.
 *
 * @author Sebastien Vincent
 */
public interface GlobalShortcutListener
{
    /**
     * Callback when an shortcut is typed
     *
     * @param evt <tt>GlobalShortcutEvent</tt>
     */
    public void shortcutReceived(GlobalShortcutEvent evt);
}
