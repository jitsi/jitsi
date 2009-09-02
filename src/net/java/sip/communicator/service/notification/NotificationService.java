/*
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
     * Creates a <tt>SoundNotificationHandler</tt>, by specifying the
     * path pointing to the sound file and the loop interval if the sound should
     * be played in loop. If the sound should be played just once the loop
     * interval should be set to -1. The <tt>SoundNotificationHandler</tt> is
     * the one that would take care of playing the sound, when a notification
     * is fired.
     * 
     * @param soundFileDescriptor the path pointing to the sound file
     * @param loopInterval the interval of milliseconds to repeat the sound in
     * loop  
     * @return the <tt>SoundNotificationHandler</tt> is the one, that would take
     * care of playing the given sound, when a notification is fired
     */
    public SoundNotificationHandler createSoundNotificationHandler(
                                                    String soundFileDescriptor,
                                                    int loopInterval);
    
    /**
     * Creates a <tt>PopupMessageNotificationHandler</tt>, by specifying the
     * default message to show, when no message is provided to the
     * <tt>fireNotification</tt> method. The
     * <tt>PopupMessageNotificationHandler</tt> is the one that would take care
     * of showing a popup message (through the systray service for example),
     * when a notification is fired.
     * 
     * @param defaultMessage the message to show if not message is provided to
     * the <tt>fireNotification</tt> method
     * @return the <tt>PopupMessageNotificationHandler</tt> is the one, that
     * would take care of showing a popup message (through the systray service
     * for example), when a notification is fired.
     */
    public PopupMessageNotificationHandler createPopupMessageNotificationHandler(
                                                    String defaultMessage);
    
    /**
     * Creates a <tt>LogMessageNotificationHandler</tt>, by specifying the
     * type of the log (error, trace, info, etc.). The
     * <tt>LogMessageNotificationHandler</tt> is the one that would take care
     * of logging a message (through the application log system), when a
     * notification is fired.
     * 
     * @param logType the type of the log (error, trace, etc.). One of the types
     * defined in the <tt>LogMessageNotificationHandler</tt> interface
     * @return the <tt>LogMessageNotificationHandler</tt> is the one, that would
     * take care of logging a message (through the application log system), when
     * a notification is fired.
     */
    public LogMessageNotificationHandler createLogMessageNotificationHandler(
                                                    String logType);
    
    /**
     * Creates a <tt>CommandNotificationHandler</tt>, by specifying the path to
     * the command file to execute, when a notification is fired. The
     * <tt>CommandNotificationHandler</tt> is the one that would take care
     * of executing the given program, when a notification is fired.
     * 
     * @param commandFileDescriptor the path to the file containing the program
     * to execute
     * @return the <tt>CommandNotificationHandler</tt> is the one, that would
     * take care of executing a program, when a notification is fired.
     */
    public CommandNotificationHandler createCommandNotificationHandler(
                                        String commandFileDescriptor);
    
    /**
     * Registers a notification for the given <tt>eventType</tt> by specifying
     * the type of the action to be performed when a notification is fired for
     * this event and the corresponding <tt>handler</tt> that should be used to
     * handle the action. Unlike the other <tt>registerNotificationForEvent</tt>
     * method, this one allows the user to specify its own
     * <tt>NotificationHandler</tt>, which would be used to handle notifications
     * for the specified <tt>actionType</tt>.
     * 
     * @param eventType the name of the event (as defined by the plug-in that's
     * registering it) that we are setting an action for.
     * @param actionType the type of the action that is to be executed when the
     * specified event occurs (could be one of the ACTION_XXX fields).
     * @param handler the <tt>NotificationActionHandler</tt>, which would be
     * used to perform the notification action.
     * @throws IllegalArgumentException if the specified <tt>handler</tt> do not
     * correspond to the given <tt>actionType</tt>.
     */
    public void registerNotificationForEvent(   String eventType,
                                                String actionType,
                                                NotificationActionHandler handler)
        throws IllegalArgumentException;
    
    /**
     * Registers a Default notification for the given <tt>eventType</tt> by specifying
     * the type of the action to be performed when a notification is fired for
     * this event and the corresponding <tt>handler</tt> that should be used to
     * handle the action. Unlike the other 
     * <tt>registerDefaultNotificationForEvent</tt>
     * method, this one allows the user to specify its own
     * <tt>NotificationHandler</tt>, which would be used to handle notifications
     * for the specified <tt>actionType</tt>.
     * Default events are stored or executed at first run or when they are 
     * missing in the configuration. Also the registered default events 
     * are used when restoreDefaults is called.
     * 
     * @param eventType the name of the event (as defined by the plug-in that's
     * registering it) that we are setting an action for.
     * @param actionType the type of the action that is to be executed when the
     * specified event occurs (could be one of the ACTION_XXX fields).
     * @param handler the <tt>NotificationActionHandler</tt>, which would be
     * used to perform the notification action.
     * @throws IllegalArgumentException if the specified <tt>handler</tt> do not
     * correspond to the given <tt>actionType</tt>.
     */
    public void registerDefaultNotificationForEvent(   String eventType,
                                                String actionType,
                                                NotificationActionHandler handler)
        throws IllegalArgumentException;
    
    /**
     * Registers a default notification for the given <tt>eventType</tt> by specifying
     * the type of the action to be performed when a notification is fired for
     * this event, the <tt>actionDescriptor</tt> for sound and command actions
     * and the <tt>defaultMessage</tt> for popup and log actions. Actions
     * registered by this method would be handled by some default
     * <tt>NotificationHandler</tt>s, declared by the implementation.
     * <p>
     * The method allows registering more than one actionType for a specific
     * event. Setting twice the same <tt>actionType</tt> for the same
     * <tt>eventType</tt>  however would cause the first setting to be
     * overridden.
     * Default events are stored or executed at first run or when they are 
     * missing in the configuration. Also the registered default events 
     * are used when restoreDefaults is called.
     *
     * @param eventType the name of the event (as defined by the plug-in that's
     * registering it) that we are setting an action for.
     * @param actionType the type of the action that is to be executed when the
     * specified event occurs (could be one of the ACTION_XXX fields).
     * @param actionDescriptor a String containing a description of the action
     * (a URI to the sound file for audio notifications or a command line for
     * exec action types) that should be executed when the action occurs.
     * @param defaultMessage the default message to use if no specific message
     * has been provided when firing the notification.
     */
    public void registerDefaultNotificationForEvent(   String eventType,
                                                String actionType,
                                                String actionDescriptor,
                                                String defaultMessage);
    
    /**
     * Registers a notification for the given <tt>eventType</tt> by specifying
     * the type of the action to be performed when a notification is fired for
     * this event, the <tt>actionDescriptor</tt> for sound and command actions
     * and the <tt>defaultMessage</tt> for popup and log actions. Actions
     * registered by this method would be handled by some default
     * <tt>NotificationHandler</tt>s, declared by the implementation.
     * <p>
     * The method allows registering more than one actionType for a specific
     * event. Setting twice the same <tt>actionType</tt> for the same
     * <tt>eventType</tt>  however would cause the first setting to be
     * overridden.
     *
     * @param eventType the name of the event (as defined by the plug-in that's
     * registering it) that we are setting an action for.
     * @param actionType the type of the action that is to be executed when the
     * specified event occurs (could be one of the ACTION_XXX fields).
     * @param actionDescriptor a String containing a description of the action
     * (a URI to the sound file for audio notifications or a command line for
     * exec action types) that should be executed when the action occurs.
     * @param defaultMessage the default message to use if no specific message
     * has been provided when firing the notification.
     */
    public void registerNotificationForEvent(   String eventType,
                                                String actionType,
                                                String actionDescriptor,
                                                String defaultMessage);
    
    /**
     * Deletes all registered events and actions 
     * and registers and saves the default events as current.
     */
    public void restoreDefaults();
    
    /**
     * Removes the given <tt>eventType</tt> from the list of event notifications.
     * This means that we delete here all registered notifications for the given
     * <tt>eventType</tt>.
     * <p>
     * This method does nothing if the given <tt>eventType</tt> is not contained
     * in the list of registered event types.
     *  
     * @param eventType the name of the event (as defined by the plugin that's
     * registering it) to be removed.
     */
    public void removeEventNotification(String eventType);
    
    /**
     * Removes the event notification corresponding to the specified
     * <tt>actionType</tt> and <tt>eventType</tt>.
     * <p>
     * This method does nothing if the given <tt>eventType</tt> or
     * <tt>actionType</tt> are not contained in the list of registered types.
     * 
     * @param eventType the name of the event (as defined by the plugin that's
     * registering it) for which we'll remove the notification.
     * @param actionType the type of the action that is to be executed when the
     * specified event occurs (could be one of the ACTION_XXX fields).
     */
    public void removeEventNotificationAction(  String eventType,
                                                String actionType);

    /**
     * Returns an iterator over a list of all events registered in this
     * notification service. Each line in the returned list consists of
     * a String, representing the name of the event (as defined by the plugin
     * that registered it).
     *   
     * @return an iterator over a list of all events registered in this
     * notifications service
     */
    public Iterator<String> getRegisteredEvents();
    
    /**
     * Returns a Map containing all action types (as keys) and actionDescriptors
     * (as values) that have been registered for <tt>eventType</tt>.
     * <p>
     * This method returns <b>null</b> if the given <tt>eventType</tt> is not
     * contained in the list of registered event types.
     * 
     * @param eventType the name of the event that we'd like to retrieve actions
     * for.
     * @return a <tt>Map</tt> containing the <tt>actionType</tt>s (as keys) and
     * <tt>actionHandler</tt>s (as values) that should be executed when
     * an event with the specified name has occurred, or null if no actions
     * have been defined for <tt>eventType</tt>.
     */
    public Map<String, NotificationActionHandler> getEventNotifications(
        String eventType);

    /**
     * Returns the <tt>NotificationActionHandler</tt> corresponding to the given
     * event and action types.
     * <p>
     * This method returns <b>null</b> if the given <tt>eventType</tt> or
     * <tt>actionType</tt> are not contained in the list of registered types.
     *
     * @param eventType the type of the event that we'd like to retrieve.
     * @param actionType the type of the action that we'd like to retrieve a
     * descriptor for.
     * @return the <tt>NotificationActionHandler</tt> corresponding to the given
     * event and action types
     */
    public NotificationActionHandler getEventNotificationActionHandler(
                                                        String eventType,
                                                        String actionType);

    /**
     * Registers a listener that would be notified of changes that have occurred
     * in the registered event notifications.
     *
     * @param listener the listener that we'd like to register for changes in
     * the event notifications stored by this service.
     */
    public void addNotificationChangeListener(
        NotificationChangeListener listener);

    /**
     * Remove the specified listener so that it won't receive further
     * notifications of changes that occur with actions registered for events
     * stored by this service.
     *
     * @param listener the listener to remove.
     */
    public void removeNotificationChangeListener(
        NotificationChangeListener listener);

    /**
     * Fires all notifications registered for the specified <tt>eventType</tt>
     * using <tt>message</tt> as a notification message wherever appropriate
     * (e.g. systray notifications, logs, etc.)
     * <p>
     * This method does nothing if the given <tt>eventType</tt> is not contained
     * in the list of registered event types.
     * 
     * @param eventType the type of the event that we'd like to fire a
     * notification for.
     * @param messageTitle the message title to use if and where appropriate
     * (e.g. with systray)
     * @param message the message to use if and where appropriate (e.g. with
     * systray or log notification.)
     * @param icon the icon to show in the notification if and where
     * appropriate
     * @param tag additional info to be used by the notification handler
     */
    public void fireNotification(   String eventType,
                                    String messageTitle,
                                    String message,
                                    byte[] icon,
                                    Object tag);

    /**
     * Fires all notifications registered for the specified <tt>eventType</tt>
     * using the default message specified upon registration as a notification
     * message wherever appropriate.
     * (e.g. systray notifications, logs, etc.)
     * <p>
     * This method does nothing if the given <tt>eventType</tt> is not contained
     * in the list of registered event types.
     * 
     * @param eventType the type of the event that we'd like to fire a
     * notification for.
     */
    public void fireNotification(String eventType);

    /**
     * Activates or deactivates all notification actions related to the
     * specified <tt>eventType</tt>. This method does nothing if the given
     * <tt>eventType</tt> is not contained in the list of registered event types.
     * 
     * @param eventType the name of the event, which actions should be activated
     * /deactivated. 
     * @param isActive indicates whether to activate or deactivate the actions
     * related to the specified <tt>eventType</tt>.
     */
    public void setActive(String eventType, boolean isActive);
    
    /**
     * Indicates whether or not actions for the specified <tt>eventType</tt>
     * are activated. This method returns <code>false</code> if the given
     * <tt>eventType</tt> is not contained in the list of registered event types.
     * 
     * @param eventType the name of the event (as defined by the plugin that's
     * registered it) that we are checking.
     * @return <code>true</code> if actions for the specified <tt>eventType</tt>
     * are activated, <code>false</code> - otherwise. If the given
     * <tt>eventType</tt> is not contained in the list of registered event
     * types - returns <code>false</code>.
     */
    public boolean isActive(String eventType);
}
