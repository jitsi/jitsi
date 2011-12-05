/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.notification;

import java.util.*;

import net.java.sip.communicator.service.notification.*;

/**
 * Represents an event notification.
 * 
 * @author Yana Stamcheva
 */
public class EventNotification
{
    /**
     * Indicates if this event notification is currently active. By default all
     * notifications are active.
     */
    private boolean isActive = true;

    /**
     * Contains all actions which will be executed when this event notification
     * is fired.
     */
    private final Hashtable<String, Action> actionsTable
        = new Hashtable<String, Action>();

    /**
     * Creates an instance of <tt>EventNotification</tt> by specifying the
     * event type as declared by the bundle registering it.
     * 
     * @param eventType the name of the event
     */
    public EventNotification(String eventType)
    {
    }

    /**
     * Adds the given <tt>actionType</tt> to the list of actions for this event
     * notifications.
     *  
     * @param actionType one of NotificationService.ACTION_XXX constants
     * @param actionHandler the the handler that will process the given action
     * type.
     * @return the previous value of the actionHandler for the given actionType,
     * if one existed, NULL if the actionType is a new one  
     */
    public Object addAction(String actionType,
                            NotificationActionHandler actionHandler)
    {
        Action action = new Action(actionType, actionHandler);
        
        return actionsTable.put(actionType, action);
    }

    /**
     * Removes the action corresponding to the given <tt>actionType</tt>.
     * 
     * @param actionType one of NotificationService.ACTION_XXX constants
     */
    public void removeAction(String actionType)
    {
        actionsTable.remove(actionType);
    }

    /**
     * Returns the set of actions registered for this event notification.
     * 
     * @return the set of actions registered for this event notification
     */
    public Map<String, Action> getActions()
    {   
        return actionsTable;
    }

    /**
     * Returns the <tt>Action</tt> corresponding to the given
     * <tt>actionType</tt>.
     * 
     * @param actionType one of NotificationService.ACTION_XXX constants
     * 
     * @return the <tt>Action</tt> corresponding to the given
     * <tt>actionType</tt>
     */
    public Action getAction(String actionType)
    {
        return actionsTable.get(actionType);
    }

    /**
     * The representation of an action, containing the corresponding
     * action type, action descriptor and the default message associated with
     * the action.
     */
    public static class Action
    {
        private final String actionType;
        private final NotificationActionHandler actionHandler;

        /**
         * Creates an instance of <tt>Action</tt> by specifying the type of the
         * action, the descriptor and the default message.
         * 
         * @param actionType one of NotificationService.ACTION_XXX constants
         * @param actionHandler the handler that will process the given action
         * type
         */
        Action( String actionType,
                NotificationActionHandler actionHandler)
        {
            this.actionType = actionType;
            this.actionHandler = actionHandler;
        }

        /**
         * Returns the the handler that will process the given action
         * type.
         * @return the the handler that will process the given action
         * type.
         */
        public NotificationActionHandler getActionHandler()
        {
            return actionHandler;
        }

        /**
         * Return the action type name.
         * @return the action type name.
         */
        public String getActionType()
        {
            return actionType;
        }
    }

    /**
     * Indicates if this event notification is currently active.
     * 
     * @return true if this event notification is active, false otherwise.
     */
    public boolean isActive()
    {
        return isActive;
    }

    /**
     * Activates or deactivates this event notification.
     * 
     * @param isActive indicates if this event notification is active
     */
    public void setActive(boolean isActive)
    {
        this.isActive = isActive;
    }
}
