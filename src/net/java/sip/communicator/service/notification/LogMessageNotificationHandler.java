/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.notification;

/**
 * The <tt>LogMessageNotificationHandler</tt> interface is meant to be
 * implemented by the notification bundle in order to provide handling of
 * log actions.
 *
 * @author Yana Stamcheva
 */
public interface LogMessageNotificationHandler
    extends NotificationHandler
{
    /**
     * Logs the given message.
     *
     * @param action the action to act upon
     * @param message the message to log
     */
    public void logMessage(LogMessageNotificationAction action, String message);
}
