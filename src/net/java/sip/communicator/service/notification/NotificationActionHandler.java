/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.notification;

/**
 * The <tt>NotificationActionHandler</tt> is the parent interface of all specific
 * notification handlers used for handling different action types. This
 * interface is used in the NotificationService in all methods dealing with
 * action handlers.
 * 
 * @author Yana Stamcheva
 */
public interface NotificationActionHandler
{
    /**
     * Returns TRUE if this notification action handler is enabled and FALSE
     * otherwise. While the notification handler for an action type is disabled
     * no notifications will be fired for this action type.
     * 
     * @return TRUE if this notification action handler is enabled and FALSE
     * otherwise
     */
    public boolean isEnabled();

    /**
     * Enables or disables this notification handler. While the notification
     * handler for an action type is disabled no notifications will be fired
     * for this action type.
     * 
     * @param isEnabled TRUE to enable this notification handler, FALSE to
     * disable it.
     */
    public void setEnabled(boolean isEnabled);
}
