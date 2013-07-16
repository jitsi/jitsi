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
     * Suggested timeout in ms for hiding the popup if not clicked by the user.
     */
    private long timeout = -1;

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
     * Creates an instance of <tt>PopupMessageNotificationHandlerImpl</tt> by
     * specifying the default message to use if no message is specified.
     *
     * @param defaultMessage the default message to use if no message is
     * specified
     * @param timeout suggested timeout in ms for hiding the popup if not
     *                clicked by the user, -1 for infinity
     */
    public PopupMessageNotificationAction(String defaultMessage, long timeout)
    {
        super(NotificationAction.ACTION_POPUP_MESSAGE);
        this.defaultMessage = defaultMessage;
        this.timeout = timeout;
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
     * Returns suggested timeout value in ms for hiding the popup if not clicked
     * by the user.
     * @return timeout value in ms for hiding the popup, -1 for infinity.
     */
    public long getTimeout()
    {
        return timeout;
    }
}
