/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.notification.event;

import java.util.*;

import net.java.sip.communicator.service.notification.*;

/**
 * Fired any time an action type is added, removed or changed.
 *
 * @author Emil Ivov
 * @author Yana Stamcheva
 */
public class NotificationActionTypeEvent
    extends EventObject
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * Indicates that a new action is added to an event type.
     */
    public static final String ACTION_ADDED = "ActionAdded";

    /**
     * Indicates that an action was removed for a given event type.
     */
    public static final String ACTION_REMOVED = "ActionRemoved";

    /**
     * Indicates that an action for a given event type has changed. For example
     * the action descriptor is changed.
     */
    public static final String ACTION_CHANGED = "ActionChanged";

    /**
     * The type of the event that a new action is being added for.
     */
    private String sourceEventType = null;

    /**
     * The descriptor of the action (i.e. audio file uri, or a command line
     * string) that will be performed when notifications are being fired for
     * the corresponding event type.
     */
    private NotificationAction actionHandler = null;

    /**
     * The type of this event. One of the static field constants declared in
     * this class.
     */
    private String eventType = null;

    /**
     * Creates an instance of this event according to the specified type.
     *
     * @param source the <tt>NotificationService</tt> that dispatched this event
     * @param eventType the type of this event. One of the static fields
     * declared in this class
     * @param sourceEventType the event type for which this event occured
     * @param actionHandler the <tt>NotificationActionHandler</tt> that handles
     * the given action
     */
    public NotificationActionTypeEvent( NotificationService source,
                                        String eventType,
                                        String sourceEventType,
                                        NotificationAction actionHandler)
    {
        super(source);

        this.eventType = eventType;
        this.sourceEventType = sourceEventType;
        this.actionHandler = actionHandler;
    }

    /**
     * Returns the event type, to which the given action belongs.
     *
     * @return the event type, to which the given action belongs
     */
    public String getSourceEventType()
    {
        return sourceEventType;
    }

    /**
     * Returns the <tt>NotificationActionHandler</tt> that handles the action,
     * for which this event is about.
     *
     * @return the <tt>NotificationActionHandler</tt> that handles the action,
     * for which this event is about.
     */
    public NotificationAction getActionHandler()
    {
        return actionHandler;
    }

    /**
     * The type of this event. One of ACTION_XXX constants declared in this
     * class.
     *
     * @return the type of this event
     */
    public String getEventType()
    {
        return eventType;
    }
}
