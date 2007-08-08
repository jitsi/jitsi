/*r
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.notification;

import java.util.*;

/**
 * This service is previewed for use by bundles that implement some kind of
 * user notification (e.g. playing sounds, poping systray tooltips, or
 * triggering commands.) In the case when such bundles would like to leave the
 * user the possibility to enable/disable or configure these notifications they
 * could register an event type in this Registry service.
 *
 * @todo write an example once we have completed the definition of the service.
 *
 * @author Emil Ivov
 * @author Yana Stamcheva
 */
public interface NotificationService
{
    /**
     * The log message action type indicates that a message would be logged,
     * when a notification is fired.
     */
    public static final String ACTION_LOG_MESSAGE = "LogMessageAction";
    
    /**
     * The popup message action type indicates that a window (or a systray
     * popup), containing the corresponding notification message would be poped
     * up, when a notification is fired.
     */
    public static final String ACTION_POPUP_MESSAGE = "PopupMessageAction";
    
    /**
     * The sound action type indicates that a sound would be played, when a
     * notification is fired.
     */
    public static final String ACTION_SOUND = "SoundAction";
    
    /**
     * The command action type indicates that a command would be executed,
     * when a notification is fired.
     */
    public static final String ACTION_COMMAND = "CommandAction";
    
    /**
     * Registers the specified <tt>actionDescriptor</tt> as a notification that
     * should be used every time an event with the specified <tt>eventType</tt>
     * has occurred.
     * <p>
     * The method allows registering more than one actionType for a specific
     * event. Setting twice the same <tt>actionType</tt> for the same
     * <tt>eventType</tt>  however would cause the first setting to be
     * overridden.
     *
     * @param eventType the name of the event (as defined by the plugin that's
     * registering it) that we are setting an action for.
     * @param actionType the type of the action that is to be executed when the
     * specified event occurs (could be one of the ACTION_XXX fields).
     * @param actionDescriptor a String containing a description of the action
     * (a URI to the sound file for audio notifications or a command line for
     * exec action types) that should be executed when the action occurs.
     * @param defaultMessage the default message to use if no specific message
     * has been provided when firing the notification.
     */
    public void registerEventNotification(String eventType,
                                          String actionType,
                                          String actionDescriptor,
                                          String defaultMessage);
    
    /**
     * Removes the given <tt>eventType</tt> from the list of event notifications.
     * This means that we delete here all registered notifications for the given
     * <tt>eventType</tt>.
     *  
     * @param eventType the name of the event (as defined by the plugin that's
     * registering it) to be removed.
     */
    public void removeEventNotification(String eventType);
    
    /**
     * Removes the event notification corresponding to the specified
     * <tt>actionType</tt> and <tt>eventType</tt>.
     * 
     * @param eventType the name of the event (as defined by the plugin that's
     * registering it) for which we'll remove the notification.
     * @param actionType the type of the action that is to be executed when the
     * specified event occurs (could be one of the ACTION_XXX fields).
     */
    public void removeEventNotificationAction(  String eventType,
                                                String actionType);

    /**
     * Returns a Map containing all action types (as keys) and actionDescriptors
     * (as values) that have been registered for <tt>eventType</tt>.
     *
     * @param eventType the name of the event that we'd like to retrieve actions
     * for.
     *
     * @return a <tt>Map</tt> containing the <tt>actionType</tt>s (as keys) and
     * <tt>actionDescriptor</tt>s (as values) that should be executed when
     * an event with the specified name has occurred, or null if no actions
     * have been defined for <tt>eventType</tt>.
     */
    public Map getEventNotifications(String eventType);

    /**
     * Returns the descriptor of the action of type <tt>actionType</tt> that
     * should be executed when an event of <tt>eventType</tt> has occurred.
     *
     * @param eventType the type of the event that we'd like to retrieve.
     * @param actionType the type of the action that we'd like to retrieve a
     * descriptor for.
     * @return a String containing a descriptor of the action to be executed
     * when an event of the specified type has occurred.
     */
    public String getEventNotificationActionDescriptor(String eventType,
                                                       String actionType);

    /**
     * Registers a listener that would be notified of changes that have occurred
     * in the registered event notifications.
     *
     * @param listener the listener that we'd like to register for changes in
     * the event notifications stored by this service.
     */
    public void addEventNotificationChangeListener(Object listener);

    /**
     * Remove the specified listener so that it won't receive further
     * notifications of changes that occur with actions registered for events
     * stored by this service.
     *
     * @param listener the listener to remove.
     */
    public void removeEventNotificationChangeListener(Object listener);

    /**
     * Fires all notifications registered for the specified <tt>eventType</tt>
     * using <tt>message</tt> as a notification message whereever appropriate
     * (e.g. systray notifications, logs, etc.)
     *
     * @param eventType the type of the event that we'd like to fire a
     * notification for.
     * @param message the message to use if and where appropriate (e.g. with
     * systray or log notification.)
     */
    public void fireNotification(String eventType, String message);

    /**
     * Fires all notifications registered for the specified <tt>eventType</tt>
     * using the default message specified upon registration as a notification
     * message whereever appropriate.
     * (e.g. systray notifications, logs, etc.)
     *
     * @param eventType the type of the event that we'd like to fire a
     * notification for.
     */
    public void fireNotification(String eventType);
    
    /**
     * Activates or desactivates all notification actions related to the
     * specified <tt>eventType</tt>.
     * 
     * @param eventType the name of the event, which actions should be activated
     * /desactivated. 
     * @param isActive indicates whether to activate or desactivate the actions
     * related to the specified <tt>eventType</tt>. 
     */
    public void setActive(String eventType, boolean isActive);
    
    /**
     * Indicates whether or not actions for the specified <tt>eventType</tt>
     * are activated. 
     * 
     * @param eventType the name of the event (as defined by the plugin that's
     * registered it) that we are checking.
     * @return <code>true</code> if actions for the specified <tt>eventType</tt>
     * are activated, <code>false</code> - otherwise.  
     */
    public boolean isActive(String eventType);
}
