/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
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
     * @param title the title of the popup
     * @param message the message to show in the popup
     */
    public void popupMessage(String title, String message)
    {
        SystrayService systray = NotificationActivator.getSystray();
        
        if(systray == null)
            return;
        
        systray.showPopupMessage(title, message,
            SystrayService.NONE_MESSAGE_TYPE);
    }
}
