/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.notification;

import java.util.*;

import net.java.sip.communicator.impl.notification.EventNotification.*;
import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.notification.*;
import net.java.sip.communicator.service.notification.event.*;
import net.java.sip.communicator.service.systray.*;
import net.java.sip.communicator.util.*;

/**
 * The implementation of the <tt>NotificationService</tt>.
 * 
 * @author Yana Stamcheva
 */
public class NotificationServiceImpl
    implements NotificationService
{
    private final Logger logger =
        Logger.getLogger(NotificationServiceImpl.class);

    private static final String NOTIFICATIONS_PREFIX = 
        "net.java.sip.communicator.impl.notifications";

    /**
     * A set of all registered event notifications.
     */
    private final Hashtable<String, EventNotification> notificationsTable =
        new Hashtable<String, EventNotification>();

    /**
     * A set of all registered event notifications.
     */
    private final Map<String, EventNotification> defaultNotificationsTable =
        new Hashtable<String, EventNotification>();

    /**
     * A list of all registered <tt>NotificationChangeListener</tt>s.
     */
    private final List<NotificationChangeListener> changeListeners =
        new Vector<NotificationChangeListener>();
    
    private final ConfigurationService configService =
        NotificationActivator.getConfigurationService();

    /**
     * Creates an instance of <tt>NotificationServiceImpl</tt> by loading all
     * previously saved notifications. 
     */
    public NotificationServiceImpl()
    {
        // Load all previously saved notifications.
        this.loadNotifications();
    }
    
    /**
     * Returns an instance of <tt>CommandNotificationHandlerImpl</tt>.
     * 
     * @return an instance of <tt>CommandNotificationHandlerImpl</tt>.
     */
    public CommandNotificationHandler createCommandNotificationHandler(
        String commandDescriptor)
    {
        return new CommandNotificationHandlerImpl(commandDescriptor);
    }

    /**
     * Returns an instance of <tt>LogMessageNotificationHandlerImpl</tt>.
     * 
     * @return an instance of <tt>LogMessageNotificationHandlerImpl</tt>.
     */
    public LogMessageNotificationHandler createLogMessageNotificationHandler(
                                                                String logType)
    {
        return new LogMessageNotificationHandlerImpl(logType);
    }

    /**
     * Returns an instance of <tt>PopupMessageNotificationHandlerImpl</tt>.
     * 
     * @return an instance of <tt>PopupMessageNotificationHandlerImpl</tt>.
     */
    public PopupMessageNotificationHandler createPopupMessageNotificationHandler(
                                                        String defaultMessage)
    {
        return new PopupMessageNotificationHandlerImpl(defaultMessage);
    }

    /**
     * Returns an instance of <tt>SoundNotificationHandlerImpl</tt>.
     * 
     * @return an instance of <tt>SoundNotificationHandlerImpl</tt>.
     */
    public SoundNotificationHandler createSoundNotificationHandler(
        String soundFileDescriptor, int loopInterval)
    {
        return new SoundNotificationHandlerImpl(
            soundFileDescriptor, loopInterval);
    }
    
    /**
     * Creates a new <tt>EventNotification</tt> or obtains the corresponding
     * existing one and registers a new action in it.
     * 
     * @param eventType the name of the event (as defined by the plugin that's
     * registering it) that we are setting an action for.
     * @param actionType the type of the action that is to be executed when the
     * specified event occurs (could be one of the ACTION_XXX fields).
     * @param handler the <tt>NotificationActionHandler</tt> responsible for
     * handling the given <tt>actionType</tt> 
     */
    public void registerNotificationForEvent(   String eventType,
                                                String actionType,
                                                NotificationActionHandler handler)
    {
        EventNotification notification = null;

        if(notificationsTable.containsKey(eventType))
            notification = notificationsTable.get(eventType);
        else
        {
            notification = new EventNotification(eventType);
            
            notificationsTable.put(eventType, notification);
            
            this.fireNotificationEventTypeEvent(
                NotificationEventTypeEvent.EVENT_TYPE_ADDED, eventType);
        }

        Object existingAction = notification.addAction(actionType, handler);

        // We fire the appropriate event depending on whether this is an
        // already existing actionType or a new one.
        if (existingAction != null)
        {
            fireNotificationActionTypeEvent(
                NotificationActionTypeEvent.ACTION_CHANGED,
                eventType,
                actionType,
                handler);
        }
        else
        {
            fireNotificationActionTypeEvent(
                NotificationActionTypeEvent.ACTION_ADDED,
                eventType,
                actionType,
                handler);
        }

        // Save the notification through the ConfigurationService.
        this.saveNotification(  eventType,
                                actionType,
                                handler,
                                true,
                                false);
    }

    /**
     * Creates a new <tt>EventNotification</tt> or obtains the corresponding
     * existing one and registers a new action in it.
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
    public void registerNotificationForEvent(   String eventType,
                                                String actionType,
                                                String actionDescriptor,
                                                String defaultMessage)
    {
        if (logger.isDebugEnabled())
            logger.debug("Registering event " + eventType + "/" + 
            actionType + "/" + actionDescriptor + "/" + defaultMessage);
        
        if (actionType.equals(NotificationService.ACTION_SOUND))
        {
            registerNotificationForEvent (eventType, actionType,
                new SoundNotificationHandlerImpl(actionDescriptor, -1));
        }
        else if (actionType.equals(NotificationService.ACTION_LOG_MESSAGE))
        {
            registerNotificationForEvent (eventType, actionType,
                new LogMessageNotificationHandlerImpl(
                    LogMessageNotificationHandler.INFO_LOG_TYPE));
        }
        else if (actionType.equals(NotificationService.ACTION_POPUP_MESSAGE))
        {
            registerNotificationForEvent (eventType, actionType,
                new PopupMessageNotificationHandlerImpl(defaultMessage));
        }
        else if (actionType.equals(NotificationService.ACTION_COMMAND))
        {
            registerNotificationForEvent (eventType, actionType,
                new CommandNotificationHandlerImpl(actionDescriptor));
        }
    }

    /**
     * Removes the <tt>EventNotification</tt> corresponding to the given
     * <tt>eventType</tt> from the table of registered event notifications.
     * 
     * @param eventType the name of the event (as defined by the plugin that's
     * registering it) to be removed.
     */
    public void removeEventNotification(String eventType)
    {
        notificationsTable.remove(eventType);
        
        this.fireNotificationEventTypeEvent(
            NotificationEventTypeEvent.EVENT_TYPE_REMOVED, eventType);
    }

    /**
     * Removes the given actionType from the list of actions registered for the
     * given <tt>eventType</tt>.
     * 
     * @param eventType the name of the event (as defined by the plugin that's
     * registering it) for which we'll remove the notification.
     * @param actionType the type of the action that is to be executed when the
     * specified event occurs (could be one of the ACTION_XXX fields).
     */
    public void removeEventNotificationAction(  String eventType,
                                                String actionType)
    {
        EventNotification notification
            = notificationsTable.get(eventType);
        
        if(notification == null)
            return;

        Action action = notification.getAction(actionType);
        
        if(action == null)
            return;

        notification.removeAction(actionType);
        
        saveNotification(
            eventType, 
            actionType, 
            action.getActionHandler(), 
            false, 
            false);
        
        fireNotificationActionTypeEvent(
            NotificationActionTypeEvent.ACTION_REMOVED,
            eventType,
            action.getActionType(),
            action.getActionHandler());
    }

    /**
     * Returns an iterator over a list of all events registered in this
     * notification service. Each line in the returned list consists of
     * a String, representing the name of the event (as defined by the plugin
     * that registered it).
     *   
     * @return an iterator over a list of all events registered in this
     * notifications service
     */
    public Iterator<String> getRegisteredEvents()
    {
        return Collections.unmodifiableSet(
            notificationsTable.keySet()).iterator();
    }
    
    /**
     * Goes through all actions registered for the given <tt>eventType</tt> and
     * returns a Map of all (actionType, actionDescriptor) key-value pairs. 
     * 
     * @param eventType the name of the event that we'd like to retrieve actions
     * for
     * @return a <tt>Map</tt> containing the <tt>actionType</tt>s (as keys) and
     * <tt>actionHandler</tt>s (as values) that should be executed when
     * an event with the specified name has occurred, or null if no actions
     * have been defined for <tt>eventType</tt>.
     */
    public Map<String, NotificationActionHandler> getEventNotifications(String eventType)
    {
        EventNotification notification = notificationsTable.get(eventType);

        if(notification == null)
            return null;

        Hashtable<String, NotificationActionHandler> actions
            = new Hashtable<String, NotificationActionHandler>();

        for (Object value : notification.getActions().values())
        {
            Action action = (Action) value;
            NotificationActionHandler handler = action.getActionHandler();

            actions.put(action.getActionType(), handler);
        }

        return actions;
    }

    /**
     * Returns the notification handler corresponding to the given
     * <tt>eventType</tt> and <tt>actionType</tt>.
     * 
     * @param eventType the type of the event that we'd like to retrieve.
     * @param actionType the type of the action that we'd like to retrieve a
     * descriptor for.
     * @return the notification handler of the action to be executed
     * when an event of the specified type has occurred.
     */
    public NotificationActionHandler getEventNotificationActionHandler(
                                                            String eventType,
                                                            String actionType)
    {
        EventNotification notification
            = notificationsTable.get(eventType);

        if(notification == null)
            return null;

        EventNotification.Action action = notification.getAction(actionType);

        if(action == null)
            return null;

        return action.getActionHandler();
    }

    /**
     * Adds the given <tt>listener</tt> to the list of change listeners.
     * 
     * @param listener the listener that we'd like to register to listen for
     * changes in the event notifications stored by this service.
     */
    public void addNotificationChangeListener(
        NotificationChangeListener listener)
    {
        synchronized (changeListeners)
        {
            changeListeners.add(listener);
        }
    }

    /**
     * Removes the given <tt>listener</tt> from the list of change listeners.
     * 
     * @param listener the listener that we'd like to remove
     */
    public void removeNotificationChangeListener(
        NotificationChangeListener listener)
    {
        synchronized (changeListeners)
        {
            changeListeners.remove(listener);
        }
    }

    /**
     * If there is a registered event notification of the given
     * <tt>eventType</tt> and the event notification is currently activated, we
     * go through the list of registered actions and execute them.
     * 
     * @param eventType the type of the event that we'd like to fire a
     * notification for.
     * @param title the title of the given message
     * @param message the message to use if and where appropriate (e.g. with
     * systray or log notification.)
     * @param icon the icon to show in the notification if and where
     * appropriate
     * @param tag additional info to be used by the notification handler
     */
    public void fireNotification(
        String eventType,
        String title,
        String message,
        byte[] icon,
        Object tag)
    {
        EventNotification notification
            = notificationsTable.get(eventType);

        if(notification == null || !notification.isActive())
            return;

        Iterator<Action> actions = notification.getActions().values().iterator();

        while(actions.hasNext())
        {
            Action action = actions.next();

            String actionType = action.getActionType();

            NotificationActionHandler handler = action.getActionHandler();

            if ((handler == null) || !handler.isEnabled())
                continue;

            if (actionType.equals(NotificationService.ACTION_POPUP_MESSAGE))
            {
                ((PopupMessageNotificationHandler) handler)
                    .popupMessage(new PopupMessage(title, message, icon, tag));
            }
            else if (actionType.equals(NotificationService.ACTION_LOG_MESSAGE))
            {
                ((LogMessageNotificationHandler) handler)
                    .logMessage(message);
            }
            else if (actionType.equals(NotificationService.ACTION_SOUND))
            {
                ((SoundNotificationHandler) handler)
                    .start();
            }
            else if (actionType.equals(NotificationService.ACTION_COMMAND))
            {
                ((CommandNotificationHandler) handler)
                    .execute();
            }
        }
    }

    /**
     * If there is a registered event notification of the given
     * <tt>eventType</tt> and the event notification is currently activated, we
     * go through the list of registered actions and execute them.
     * 
     * @param eventType the type of the event that we'd like to fire a
     * notification for.
     */
    public void fireNotification(String eventType)
    {
        this.fireNotification(eventType, null, null, null, null);
    }

    /**
     * Saves the event notification given by these parameters through the
     * <tt>ConfigurationService</tt>.
     * 
     * @param eventType the name of the event
     * @param actionType the type of action
     * @param actionHandler the notification action handler responsible for
     * handling the given <tt>actionType</tt>
     */
    private void saveNotification(  String eventType,
                                    String actionType,
                                    NotificationActionHandler actionHandler,
                                    boolean isActive,
                                    boolean isDefault)
    {
        String eventTypeNodeName = null;
        String actionTypeNodeName = null;

        List<String> eventTypes = configService
                .getPropertyNamesByPrefix(NOTIFICATIONS_PREFIX, true);

        for (String eventTypeRootPropName : eventTypes)
        {
            String eType
                = configService.getString(eventTypeRootPropName);
            
            if(eType.equals(eventType))
                eventTypeNodeName = eventTypeRootPropName;
        }

        // If we didn't find the given event type in the configuration we save
        // it here.
        if(eventTypeNodeName == null)
        {
            eventTypeNodeName = NOTIFICATIONS_PREFIX
                                + ".eventType" 
                                + Long.toString(System.currentTimeMillis());
            
            configService.setProperty(eventTypeNodeName, eventType);
        }

        // if we set active/inactive for the whole event notification
        if(actionType == null && actionHandler == null)
        {
            configService.setProperty(
                eventTypeNodeName + ".active",
                Boolean.toString(isActive));
            return;
        }

        // Go through contained actions.
        String actionPrefix = eventTypeNodeName + ".actions";

        List<String> actionTypes = configService
                .getPropertyNamesByPrefix(actionPrefix, true);

        for (String actionTypeRootPropName : actionTypes)
        {
            String aType
                = configService.getString(actionTypeRootPropName);
            
            if(aType.equals(actionType))
                actionTypeNodeName = actionTypeRootPropName;
        }

        Map<String, Object> configProperties = new HashMap<String, Object>();

        // If we didn't find the given actionType in the configuration we save
        // it here.
        if(actionTypeNodeName == null)
        {
            actionTypeNodeName = actionPrefix
                                    + ".actionType"
                                    + Long.toString(System.currentTimeMillis());

            configProperties.put(actionTypeNodeName, actionType);
        }

        if(actionHandler instanceof SoundNotificationHandler)
        {
            SoundNotificationHandler soundHandler
                = (SoundNotificationHandler) actionHandler;

            configProperties.put(
                actionTypeNodeName + ".soundFileDescriptor",
                soundHandler.getDescriptor());

            configProperties.put(
                actionTypeNodeName + ".loopInterval",
                soundHandler.getLoopInterval());

            configProperties.put(
                actionTypeNodeName + ".enabled",
                Boolean.toString(isActive));

            configProperties.put(
                actionTypeNodeName + ".default",
                Boolean.toString(isDefault));
        }
        else if(actionHandler instanceof PopupMessageNotificationHandler)
        {
            PopupMessageNotificationHandler messageHandler
                = (PopupMessageNotificationHandler) actionHandler;

            configProperties.put(
                actionTypeNodeName + ".defaultMessage",
                messageHandler.getDefaultMessage());

            configProperties.put(
                actionTypeNodeName + ".enabled",
                Boolean.toString(isActive));

            configProperties.put(
                actionTypeNodeName + ".default",
                Boolean.toString(isDefault));
        }
        else if(actionHandler instanceof LogMessageNotificationHandler)
        {
            LogMessageNotificationHandler logMessageHandler
                = (LogMessageNotificationHandler) actionHandler;

            configProperties.put(
                actionTypeNodeName + ".logType",
                logMessageHandler.getLogType());

            configProperties.put(
                actionTypeNodeName + ".enabled",
                Boolean.toString(isActive));

            configProperties.put(
                actionTypeNodeName + ".default",
                Boolean.toString(isDefault));
        }
        else if(actionHandler instanceof CommandNotificationHandler)
        {
            CommandNotificationHandler commandHandler
                = (CommandNotificationHandler) actionHandler;

            configProperties.put(
                actionTypeNodeName + ".commandDescriptor",
                commandHandler.getDescriptor());

            configProperties.put(
                actionTypeNodeName + ".enabled",
                Boolean.toString(isActive));

            configProperties.put(
                actionTypeNodeName + ".default",
                Boolean.toString(isDefault));
        }

        if (configProperties.size() > 0)
            configService.setProperties(configProperties);
    }

    /**
     * Loads all previously saved event notifications.
     */
    private void loadNotifications()
    {
        List<String> eventTypes = configService
                .getPropertyNamesByPrefix(NOTIFICATIONS_PREFIX, true);

        for (String eventTypeRootPropName : eventTypes)
        {
            boolean isEventActive = 
                isEnabled(eventTypeRootPropName + ".active");

            String eventType
                = configService.getString(eventTypeRootPropName);

            List<String> actions = configService
                .getPropertyNamesByPrefix(
                    eventTypeRootPropName + ".actions", true);

            for (String actionPropName : actions)
            {
                String actionType
                    = configService.getString(actionPropName);

                NotificationActionHandler handler = null;

                if(actionType.equals(ACTION_SOUND))
                {
                    String soundFileDescriptor
                        = configService.getString(
                            actionPropName + ".soundFileDescriptor");

                    String loopInterval
                        = configService.getString(
                            actionPropName + ".loopInterval");

                    handler = new SoundNotificationHandlerImpl(
                        soundFileDescriptor,
                        Integer.parseInt(loopInterval));

                    handler.setEnabled(
                        isEnabled(actionPropName + ".enabled"));
                }
                else if(actionType.equals(ACTION_POPUP_MESSAGE))
                {
                    String defaultMessage
                        = configService.getString(
                            actionPropName + ".defaultMessage");

                    handler = new PopupMessageNotificationHandlerImpl(
                                                                defaultMessage);
                    handler.setEnabled(
                        isEnabled(actionPropName + ".enabled"));
                }
                else if(actionType.equals(ACTION_LOG_MESSAGE))
                {
                    String logType
                        = configService.getString(
                            actionPropName + ".logType");

                    handler = new LogMessageNotificationHandlerImpl(logType);

                    handler.setEnabled(isEnabled(actionPropName + ".enabled"));
                }
                else if(actionType.equals(ACTION_COMMAND))
                {
                    String commandDescriptor
                        = configService.getString(
                            actionPropName + ".commandDescriptor");

                    handler = new CommandNotificationHandlerImpl(
                                                            commandDescriptor);
                    handler.setEnabled(isEnabled(actionPropName + ".enabled"));
                }

                // Load the data in the notifications table.
                EventNotification notification
                    = notificationsTable.get(eventType);
                    
                if(notification == null)
                {
                    notification = new EventNotification(eventType);
                    notificationsTable.put(eventType, notification);
                }
                notification.setActive(isEventActive);

                notification.addAction(actionType, handler);
            }
        }
    }

    private boolean isEnabled(String configProperty)
    {
        Object isEnabledObj = configService.getProperty(configProperty);

        // if setting is missing we accept it is true 
        // this way we not affect old saved settings
        if(isEnabledObj == null)
            return true;
        else
            return Boolean.parseBoolean((String)isEnabledObj);
    }

    /**
     * Finds the <tt>EventNotification</tt> corresponding to the given
     * <tt>eventType</tt> and marks it as activated/deactivated.
     * 
     * @param eventType the name of the event, which actions should be activated
     * /deactivated. 
     * @param isActive indicates whether to activate or deactivate the actions
     * related to the specified <tt>eventType</tt>.
     */
    public void setActive(String eventType, boolean isActive)
    {
        EventNotification eventNotification
            = notificationsTable.get(eventType);
        
        if(eventNotification == null)
            return;
        
        eventNotification.setActive(isActive);
        
        saveNotification(eventType, null, null, isActive, false);
    }

    /**
     * Finds the <tt>EventNotification</tt> corresponding to the given
     * <tt>eventType</tt> and returns its isActive status.
     * 
     * @param eventType the name of the event (as defined by the plugin that's
     * registered it) that we are checking.
     * @return <code>true</code> if actions for the specified <tt>eventType</tt>
     * are activated, <code>false</code> - otherwise. If the given
     * <tt>eventType</tt> is not contained in the list of registered event
     * types - returns <code>false</code>.
     */
    public boolean isActive(String eventType)
    {
        EventNotification eventNotification
            = notificationsTable.get(eventType);
        
        if(eventNotification == null)
            return false;
        
        return eventNotification.isActive();
    }

    /**
     * Notifies all registered <tt>NotificationChangeListener</tt>s that a
     * <tt>NotificationEventTypeEvent</tt> has occurred.
     * 
     * @param eventType the type of the event, which is one of EVENT_TYPE_XXX
     * constants declared in the <tt>NotificationEventTypeEvent</tt> class.
     * @param sourceEventType the <tt>eventType</tt>, for which this event is
     * about
     */
    private void fireNotificationEventTypeEvent(String eventType,
                                                String sourceEventType)
    {
        if (logger.isDebugEnabled())
            logger.debug("Dispatching NotificationEventType Change. Listeners="
                     + changeListeners.size()
                     + " evt=" + eventType);

        NotificationEventTypeEvent event
            = new NotificationEventTypeEvent(this, eventType, sourceEventType);

        for (NotificationChangeListener listener : changeListeners)
        {
            if (eventType.equals(NotificationEventTypeEvent.EVENT_TYPE_ADDED))
            {
                listener.eventTypeAdded(event);
            }
            else if (eventType.equals(
                NotificationEventTypeEvent.EVENT_TYPE_REMOVED))
            {
                listener.eventTypeRemoved(event);
            }
        }
    }
    
    /**
     * Notifies all registered <tt>NotificationChangeListener</tt>s that a
     * <tt>NotificationActionTypeEvent</tt> has occurred.
     * 
     * @param eventType the type of the event, which is one of ACTION_XXX
     * constants declared in the <tt>NotificationActionTypeEvent</tt> class.
     * @param sourceEventType the <tt>eventType</tt>, which is the parent of the
     * action
     * @param sourceActionType the <tt>actionType</tt>, for which the event is
     * about
     * @param actionHandler the notification action handler
     */
    private void fireNotificationActionTypeEvent(
                                        String eventType,
                                        String sourceEventType,
                                        String sourceActionType,
                                        NotificationActionHandler actionHandler)
    {
        NotificationActionTypeEvent event
            = new NotificationActionTypeEvent(  this,
                                                eventType,
                                                sourceEventType,
                                                sourceActionType,
                                                actionHandler);

        NotificationChangeListener listener;

        for (int i = 0 ; i < changeListeners.size(); i ++)
        {
            listener = changeListeners.get(i);

            if (eventType.equals(NotificationActionTypeEvent.ACTION_ADDED))
            {
                listener.actionAdded(event);
            }
            else if (eventType.equals(
                NotificationActionTypeEvent.ACTION_REMOVED))
            {
                listener.actionRemoved(event);
            }
            else if (eventType.equals(
                NotificationActionTypeEvent.ACTION_CHANGED))
            {
                listener.actionChanged(event);
            }
        }
    }

    private boolean isDefault(String eventType, String actionType)
    {
        List<String> eventTypes = configService
                .getPropertyNamesByPrefix(NOTIFICATIONS_PREFIX, true);

        for (String eventTypeRootPropName : eventTypes)
        {
            String eType
                = configService.getString(eventTypeRootPropName);

            if(!eType.equals(eventType))
                continue;

            List<String> actions = configService
                .getPropertyNamesByPrefix(
                    eventTypeRootPropName + ".actions", true);

            for (String actionPropName : actions)
            {
                String aType
                    = configService.getString(actionPropName);

                if(!aType.equals(actionType))
                    continue;

                Object isDefaultdObj = 
                    configService.getProperty(actionPropName + ".default");

                // if setting is missing we accept it is true 
                // this way we override old saved settings
                if(isDefaultdObj == null)
                    return true;
                else
                    return Boolean.parseBoolean((String)isDefaultdObj);
            }
        }
        return true;
    }

    /**
     * Creates a new default <tt>EventNotification</tt> or obtains the
     * corresponding existing one and registers a new action in it.
     * 
     * @param eventType the name of the event (as defined by the plugin that's
     * registering it) that we are setting an action for.
     * @param actionType the type of the action that is to be executed when the
     * specified event occurs (could be one of the ACTION_XXX fields).
     * @param handler the <tt>NotificationActionHandler</tt> responsible for
     * handling the given <tt>actionType</tt> 
     */
    public void registerDefaultNotificationForEvent(
        String eventType,
        String actionType,
        NotificationActionHandler handler)
            throws IllegalArgumentException
    {
        if(isDefault(eventType, actionType))
        {
            NotificationActionHandler h = 
                getEventNotificationActionHandler(eventType, actionType);
            
            boolean isNew = false;
            
            if(h == null)
            {
                isNew = true;
                h = handler;
            }
            
            this.saveNotification(  eventType,
                                    actionType,
                                    handler,
                                    h.isEnabled(),
                                    true);

            EventNotification notification = null;

            if(notificationsTable.containsKey(eventType))
                notification = notificationsTable.get(eventType);
            else
            {
                notification = new EventNotification(eventType);

                notificationsTable.put(eventType, notification);
            }

            notification.addAction(actionType, handler);

            // We fire the appropriate event depending on whether this is an
            // already existing actionType or a new one.
            fireNotificationActionTypeEvent(
                isNew ? NotificationActionTypeEvent.ACTION_ADDED
                    : NotificationActionTypeEvent.ACTION_CHANGED, eventType,
                actionType, handler);
        }

        // now store this default events if we want to restore them
        EventNotification notification = null;

        if(defaultNotificationsTable.containsKey(eventType))
            notification = defaultNotificationsTable.get(eventType);
        else
        {
            notification = new EventNotification(eventType);
            
            defaultNotificationsTable.put(eventType, notification);
        }

        notification.addAction(actionType, handler);
    }

    /**
     * Creates a new default <tt>EventNotification</tt> or obtains the corresponding
     * existing one and registers a new action in it.
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
    public void registerDefaultNotificationForEvent(    String eventType,
                                                        String actionType,
                                                        String actionDescriptor,
                                                        String defaultMessage)
    {
        if (logger.isDebugEnabled())
            logger.debug("Registering default event " + eventType + "/" + 
            actionType + "/" + actionDescriptor + "/" + defaultMessage);

        if(isDefault(eventType, actionType))
        {
            NotificationActionHandler handler = 
                getEventNotificationActionHandler(eventType, actionType);
            boolean isNew = false;

            if(handler == null)
            {
                isNew = true;

                if (actionType.equals(NotificationService.ACTION_SOUND))
                {
                    handler = new SoundNotificationHandlerImpl(actionDescriptor, -1);
                }
                else if (actionType.equals(NotificationService.ACTION_LOG_MESSAGE))
                {
                    handler = new LogMessageNotificationHandlerImpl(
                            LogMessageNotificationHandler.INFO_LOG_TYPE);
                }
                else if (actionType.equals(NotificationService.ACTION_POPUP_MESSAGE))
                {
                    handler = new PopupMessageNotificationHandlerImpl(defaultMessage);
                }
                else if (actionType.equals(NotificationService.ACTION_COMMAND))
                {
                    handler = new CommandNotificationHandlerImpl(actionDescriptor);
                }
            }

            this.saveNotification(  eventType,
                                    actionType,
                                    handler,
                                    handler.isEnabled(),
                                    true);

            EventNotification notification = null;

            if(notificationsTable.containsKey(eventType))
                notification = notificationsTable.get(eventType);
            else
            {
                notification = new EventNotification(eventType);

                notificationsTable.put(eventType, notification);
            }
            
            notification.addAction(actionType, handler);
            
            // We fire the appropriate event depending on whether this is an
            // already existing actionType or a new one.
            fireNotificationActionTypeEvent(
                isNew ? NotificationActionTypeEvent.ACTION_ADDED
                    : NotificationActionTypeEvent.ACTION_CHANGED, eventType,
                actionType, handler);
        }
        
        // now store this default events if we want to restore them
        EventNotification notification = null;

        if(defaultNotificationsTable.containsKey(eventType))
            notification = defaultNotificationsTable.get(eventType);
        else
        {
            notification = new EventNotification(eventType);

            defaultNotificationsTable.put(eventType, notification);
        }

        NotificationActionHandler handler = null;
        
        if (actionType.equals(NotificationService.ACTION_SOUND))
        {
            handler = new SoundNotificationHandlerImpl(actionDescriptor, -1);
        }
        else if (actionType.equals(NotificationService.ACTION_LOG_MESSAGE))
        {
            handler = new LogMessageNotificationHandlerImpl(
                    LogMessageNotificationHandler.INFO_LOG_TYPE);
        }
        else if (actionType.equals(NotificationService.ACTION_POPUP_MESSAGE))
        {
            handler = new PopupMessageNotificationHandlerImpl(defaultMessage);
        }
        else if (actionType.equals(NotificationService.ACTION_COMMAND))
        {
            handler = new CommandNotificationHandlerImpl(actionDescriptor);
        }
        
        notification.addAction(actionType, handler);
    }

    /**
     * Deletes all registered events and actions 
     * and registers and saves the default events as current.
     */
    public void restoreDefaults()
    {
        for (String eventType : new Vector<String>(notificationsTable.keySet()))
        {
            EventNotification notification = notificationsTable.get(eventType);

            for (String actionType
                    : new Vector<String>(notification.getActions().keySet()))
                removeEventNotificationAction(eventType, actionType);

            removeEventNotification(eventType);
        }

        for (Map.Entry<String, EventNotification> entry
                : defaultNotificationsTable.entrySet())
        {
            String eventType = entry.getKey();
            EventNotification notification = entry.getValue();

            for (String actionType : notification.getActions().keySet())
                registerNotificationForEvent(
                    eventType, 
                    actionType, 
                    notification.getAction(actionType).getActionHandler());
        }
    }
}
