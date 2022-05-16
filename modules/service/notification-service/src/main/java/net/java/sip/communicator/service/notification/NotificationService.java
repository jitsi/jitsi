/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
     * Registers a notification for the given <tt>eventType</tt> by specifying
     * the action to be performed when a notification is fired for this event.
     *
     * Unlike the other <tt>registerNotificationForEvent</tt>
     * method, this one allows the user to specify its own
     * <tt>NotificationAction</tt>, which would be used to handle notifications
     * for the specified <tt>actionType</tt>.
     *
     * @param eventType the name of the event (as defined by the plug-in that's
     * registering it) that we are setting an action for.
     * @param action the <tt>NotificationAction</tt>, which would be
     * used to perform the notification action.
     */
    public void registerNotificationForEvent(String eventType,
                                             NotificationAction action);

    /**
     * Registers a default notification for the given <tt>eventType</tt> by
     * specifying the action to be performed when a notification is fired for
     * this event.
     *
     * Unlike the other <tt>registerDefaultNotificationForEvent</tt> method,
     * this one allows the user to specify its own <tt>NotificationAction</tt>,
     * which would be used to handle notifications.
     *
     * Default events are stored or executed at first run or when they are
     * missing in the configuration. Also the registered default events are used
     * when restoreDefaults is called.
     *
     * @param eventType the name of the event (as defined by the plug-in that's
     *            registering it) that we are setting an action for.
     * @param handler the <tt>NotificationActionHandler</tt>, which would be
     *            used to perform the notification action.
     */
    public void registerDefaultNotificationForEvent(String eventType,
                                                    NotificationAction handler);

    /**
     * Registers a default notification for the given <tt>eventType</tt> by
     * specifying the type of the action to be performed when a notification is
     * fired for this event, the <tt>actionDescriptor</tt> for sound and command
     * actions and the <tt>defaultMessage</tt> for popup and log actions.
     *
     * Actions registered by this method would be handled by some default
     * <tt>NotificationHandler</tt>s, declared by the implementation.
     * <p>
     * The method allows registering more than one actionType for a specific
     * event. Setting the same <tt>actionType</tt> for the same
     * <tt>eventType</tt> twice however would cause the first setting to be
     * overridden.
     *
     * Default events are stored or executed at first run or when
     * they are missing in the configuration. Also the registered default events
     * are used when restoreDefaults is called.
     *
     * @param eventType the name of the event (as defined by the plug-in that's
     *            registering it) that we are setting an action for.
     * @param actionType the type of the action that is to be executed when the
     *            specified event occurs (could be one of the ACTION_XXX
     *            fields).
     * @param actionDescriptor a String containing a description of the action
     *            (a URI to the sound file for audio notifications or a command
     *            line for exec action types) that should be executed when the
     *            action occurs.
     * @param defaultMessage the default message to use if no specific message
     *            has been provided when firing the notification.
     */
    public void registerDefaultNotificationForEvent(String eventType,
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
     * event. Setting the same <tt>actionType</tt> for the same
     * <tt>eventType</tt> twice however would cause the first setting to be
     * overridden.
     *
     * @param eventType the name of the event (as defined by the plug-in that's
     *            registering it) that we are setting an action for.
     * @param actionType the type of the action that is to be executed when the
     *            specified event occurs (could be one of the ACTION_XXX
     *            fields).
     * @param actionDescriptor a String containing a description of the action
     *            (a URI to the sound file for audio notifications or a command
     *            line for exec action types) that should be executed when the
     *            action occurs.
     * @param defaultMessage the default message to use if no specific message
     *            has been provided when firing the notification.
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
     * Removes the given <tt>eventType</tt> from the list of event
     * notifications. This means that we delete here all registered
     * notifications for the given <tt>eventType</tt>.
     * <p>
     * This method does nothing if the given <tt>eventType</tt> is not contained
     * in the list of registered event types.
     *
     * @param eventType the name of the event (as defined by the plugin that's
     *            registering it) to be removed.
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
     *            registering it) for which we'll remove the notification.
     * @param actionType the type of the action that is to be executed when the
     *            specified event occurs (could be one of the ACTION_XXX
     *            fields).
     */
    public void removeEventNotificationAction(  String eventType,
                                                String actionType);

    /**
     * Returns an iterator over a list of all events registered in this
     * notification service. Each line in the returned list consists of a
     * String, representing the name of the event (as defined by the plugin that
     * registered it).
     *
     * @return an iterator over a list of all events registered in this
     *         notifications service
     */
    public Iterable<String> getRegisteredEvents();

    /**
     * Returns the <tt>NotificationAction</tt> corresponding to the given event
     * and action type.
     * <p>
     * This method returns <b>null</b> if the given <tt>eventType</tt> or
     * <tt>actionType</tt> are not contained in the list of registered types.
     *
     * @param eventType the type of the event that we'd like to retrieve.
     * @param actionType the type of the action that we'd like to retrieve a
     *            descriptor for.
     * @return the <tt>NotificationAction</tt> corresponding to the given event
     *         and action type
     */
    public NotificationAction getEventNotificationAction(String eventType,
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
     * Adds an object that executes the actual action of a notification action.
     * @param handler The handler that executes the action.
     */
    public void addActionHandler(NotificationHandler handler);

    /**
     * Removes an object that executes the actual action of notification action.
     * @param actionType The handler type to remove.
     */
    public void removeActionHandler(String actionType);

    /**
     * Gets at list of handler for the specified action type.
     *
     * @param actionType the type for which the list of handlers should be
     *            retrieved or <tt>null</tt> if all handlers shall be returned.
     * @return Iterable of NotificationHandler objects
     */
    public Iterable<NotificationHandler> getActionHandlers(String actionType);

    /**
     * Fires all notifications registered for the specified <tt>eventType</tt>
     * using <tt>message</tt> as a notification message wherever appropriate
     * (e.g. systray notifications, logs, etc.)
     * <p>
     * This method does nothing if the given <tt>eventType</tt> is not contained
     * in the list of registered event types.
     * </p>
     *
     * @param eventType the type of the event that we'd like to fire a
     *            notification for.
     * @param messageTitle the message title to use if and where appropriate
     *            (e.g. with systray)
     * @param message the message to use if and where appropriate (e.g. with
     *            systray or log notification.)
     * @param icon the icon to show in the notification if and where appropriate
     * @return An object referencing the notification. It may be used to stop a
     *         still running notification. Can be null if the eventType is
     *         unknown or the notification is not active.
     */
    public NotificationData fireNotification(   String eventType,
                                    String messageTitle,
                                    String message,
                                    byte[] icon);

    /**
     * Fires all notifications registered for the specified <tt>eventType</tt>
     * using <tt>message</tt> as a notification message wherever appropriate
     * (e.g. systray notifications, logs, etc.)
     * <p>
     * This method does nothing if the given <tt>eventType</tt> is not contained
     * in the list of registered event types.
     *
     * @param eventType the type of the event that we'd like to fire a
     *            notification for.
     * @param messageTitle the message title to use if and where appropriate
     *            (e.g. with systray)
     * @param message the message to use if and where appropriate (e.g. with
     *            systray or log notification.)
     * @param icon the icon to show in the notification if and where appropriate
     * @param extras additional/extra {@link NotificationHandler}-specific data
     * to be provided to the firing of the specified notification(s). The
     * well-known keys are defined by the <tt>NotificationData</tt>
     * <tt>XXX_EXTRA</tt> constants.
     * @return An object referencing the notification. It may be used to stop a
     *         still running notification. Can be null if the eventType is
     *         unknown or the notification is not active.
     */
    public NotificationData fireNotification(
            String eventType,
            String messageTitle,
            String message,
            byte[] icon,
            Map<String,Object> extras);

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
     *
     * @return An object referencing the notification. It may be used to stop a
     *         still running notification. Can be null if the eventType is
     *         unknown or the notification is not active.
     */
    public NotificationData fireNotification(String eventType);

    /**
     * Stops a notification if notification is continuous, like playing sounds
     * in loop. Do nothing if there are no such events currently processing.
     *
     * @param data the data that has been returned when firing the event..
     */
    public void stopNotification(NotificationData data);

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

    /**
     * Tells if the given sound notification is currently played.
     *
     * @param data Additional data for the event.
     */
    public boolean isPlayingNotification(NotificationData data);
}
