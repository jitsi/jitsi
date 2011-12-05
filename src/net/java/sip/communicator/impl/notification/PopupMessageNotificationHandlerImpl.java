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
    /**
     * {@inheritDoc}
     */
    public String getActionType()
    {
        return NotificationAction.ACTION_POPUP_MESSAGE;
    }

    /**
     * Shows the given <tt>PopupMessage</tt>
     * 
     * @param action the action to act upon
     * @param title the title of the given message
     * @param message the message to use if and where appropriate (e.g. with
     * systray or log notification.)
     * @param icon the icon to show in the notification if and where
     * appropriate
     * @param tag additional info to be used by the notification handler
     */
    public void popupMessage(PopupMessageNotificationAction action,
        String title,
        String message,
        byte[] icon,
        Object tag)
    {
        SystrayService systray = NotificationActivator.getSystray();
        if(systray == null)
            return;

        systray.showPopupMessage(new PopupMessage(title, message, icon, tag));
    }
}
