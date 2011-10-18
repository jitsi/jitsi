/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.notification;

import net.java.sip.communicator.service.notification.*;
import net.java.sip.communicator.service.systray.*;


/**
 * An implementation of the <tt>PopupMessageNotificationHandler</tt> interface.
 * 
 * @author Yana Stamcheva
 */
public class PopupMessageNotificationHandlerImpl
    implements PopupMessageNotificationHandler
{
    private String defaultMessage;

    private boolean isEnabled = true;

    /**
     * Creates an instance of <tt>PopupMessageNotificationHandlerImpl</tt> by
     * specifying the default message to use if no message is specified.
     * 
     * @param defaultMessage the default message to use if no message is
     * specified
     */
    public PopupMessageNotificationHandlerImpl(String defaultMessage)
    {
        this.defaultMessage = defaultMessage;
    }
    
    /**
     * Return the default message to use if no message is specified.
     * 
     * @return the default message to use if no message is specified.
     */
    public String getDefaultMessage()
    {
        return defaultMessage;
    }

    /**
     * Shows a popup message through the <tt>SystrayService</tt>.
     *
     * @param message the message to show in the popup
     */
    public void popupMessage(PopupMessage message)
    {
        SystrayService systray = NotificationActivator.getSystray();

        if(systray == null)
            return;

        systray.showPopupMessage(message);
    }

    /**
     * Returns TRUE if this notification action handler is enabled and FALSE
     * otherwise. While the notification handler for the pop-up message action
     * type is disabled no messages will be popped up when the
     * <tt>fireNotification</tt> method is called.
     * 
     * @return TRUE if this notification action handler is enabled and FALSE
     * otherwise
     */
    public boolean isEnabled()
    {
        return isEnabled;
    }

    /**
     * Enables or disables this notification handler. While the notification
     * handler for the pop-up message action type is disabled no messages will
     * be popped up when the <tt>fireNotification</tt> method is called.
     * 
     * @param isEnabled TRUE to enable this notification handler, FALSE to
     * disable it.
     */
    public void setEnabled(boolean isEnabled)
    {
        this.isEnabled = isEnabled;
    }
}
