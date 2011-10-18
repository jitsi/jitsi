/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main;

/**
 * The <tt>UINotificationListener</tt> listens for new notifications received
 * in the user interface. Notifications could be for example missed calls,
 * voicemails or email messages.
 *
 * @author Yana Stamcheva
 */
public interface UINotificationListener
{
    /**
     * Indicates that a new notification is received.
     *
     * @param notification the notification that was received
     */
    public void notificationReceived(UINotification notification);
}
