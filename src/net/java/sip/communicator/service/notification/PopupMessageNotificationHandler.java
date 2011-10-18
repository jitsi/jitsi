/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.notification;

import net.java.sip.communicator.service.systray.*;

/**
 * The <tt>PopupMessageNotificationHandler</tt> interface is meant to be
 * implemented by the notification bundle in order to provide handling of
 * popup message actions.
 * 
 * @author Yana Stamcheva
 */
public interface PopupMessageNotificationHandler
    extends NotificationActionHandler
{
    /**
     * Returns the default message to be used when no message is provided to the
     * <tt>popupMessage</tt> method.
     * 
     * @return the default message to be used when no message is provided to the
     * <tt>popupMessage</tt> method.
     */
    public String getDefaultMessage();
    
    /**
     * Shows the given <tt>PopupMessage</tt>
     * 
     * @param message the message to show in the popup
     */
    public void popupMessage(PopupMessage message);
}
