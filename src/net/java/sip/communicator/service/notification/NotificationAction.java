/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.notification;

/**
 * Base class for actions of a notification.
 * 
 * @author Ingo Bauersachs
 */
public abstract class NotificationAction
{
    /**
     * The sound action type indicates that a sound would be played, when a
     * notification is fired.
     */
    public static final String ACTION_SOUND = "SoundAction";

    /**
     * The popup message action type indicates that a window (or a systray
     * popup), containing the corresponding notification message would be poped
     * up, when a notification is fired.
     */
    public static final String ACTION_POPUP_MESSAGE = "PopupMessageAction";

    /**
     * The log message action type indicates that a message would be logged,
     * when a notification is fired.
     */
    public static final String ACTION_LOG_MESSAGE = "LogMessageAction";

    /**
     * The command action type indicates that a command would be executed,
     * when a notification is fired.
     */
    public static final String ACTION_COMMAND = "CommandAction";

    /**
     * Defines the number of actions.
     */
    public static final int NUM_ACTIONS = 4;

    /**
     * Indicates if this handler is enabled.
     */
    private boolean isEnabled = true;

    /**
     * The action type name.
     */
    private String actionType;

    /**
     * Creates a new instance of this class.
     * @param actionType The action type name.
     */
    protected NotificationAction(String actionType)
    {
        this.actionType = actionType;
    }

    /**
     * Return the action type name.
     * @return the action type name.
     */
    public String getActionType()
    {
        return actionType;
    }

    /**
     * Returns TRUE if this notification action handler is enabled and FALSE
     * otherwise. While the notification handler for the sound action type is
     * disabled no sounds will be played when the <tt>fireNotification</tt>
     * method is called.
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
     * handler for the sound action type is disabled no sounds will be played
     * when the <tt>fireNotification</tt> method is called.
     * 
     * @param isEnabled TRUE to enable this notification handler, FALSE to
     * disable it.
     */
    public void setEnabled(boolean isEnabled)
    {
        this.isEnabled = isEnabled;
    }
}