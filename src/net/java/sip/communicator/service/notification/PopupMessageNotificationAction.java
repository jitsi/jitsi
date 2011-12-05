/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.notification;

/**
 * An implementation of the <tt>PopupMessageNotificationHandler</tt> interface.
 * 
 * @author Yana Stamcheva
 */
public class PopupMessageNotificationAction
    extends NotificationAction
{
    private String defaultMessage;

    /**
     * Creates an instance of <tt>PopupMessageNotificationHandlerImpl</tt> by
     * specifying the default message to use if no message is specified.
     * 
     * @param defaultMessage the default message to use if no message is
     * specified
     */
    public PopupMessageNotificationAction(String defaultMessage)
    {
        super(NotificationAction.ACTION_POPUP_MESSAGE);
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
}
