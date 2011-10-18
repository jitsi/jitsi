/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.event;

import java.util.*;

/**
 * This listener receives events whenever a contact has sent us a typing
 * notification. The source contact and the exact type of the notification
 * are indicated in <tt>TypingNotificationEvent</tt> instances.
 *
 * @author Emil Ivov
 */
public interface TypingNotificationsListener
    extends EventListener
{
    /**
     * Called to indicate that a remote <tt>Contact</tt> has sent us a typing
     * notification.
     * @param event a <tt>TypingNotificationEvent</tt> containing the sender
     * of the notification and its type.
     */
    public void typingNotificationReceived(TypingNotificationEvent event);
}
