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
 * Represents an event notification.
 *
 * @author Yana Stamcheva
 */
public class Notification
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
    private final Hashtable<String, NotificationAction> actionsTable
        = new Hashtable<String, NotificationAction>();

    /**
     * Creates an instance of <tt>EventNotification</tt> by specifying the
     * event type as declared by the bundle registering it.
     *
     * @param eventType the name of the event
     */
    public Notification(String eventType)
    {
    }

    /**
     * Adds the given <tt>actionType</tt> to the list of actions for this event
     * notifications.
     * @param action the the handler that will process the given action
     * type.
     *
     * @return the previous value of the actionHandler for the given actionType,
     * if one existed, NULL if the actionType is a new one
     */
    public Object addAction(NotificationAction action)
    {
        return actionsTable.put(action.getActionType(), action);
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
    public Map<String, NotificationAction> getActions()
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
    public NotificationAction getAction(String actionType)
    {
        return actionsTable.get(actionType);
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
