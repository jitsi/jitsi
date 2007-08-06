/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.notification.event;

import java.util.*;
import net.java.sip.communicator.service.notification.*;

/**
 * Fired any time that a new action (i.e. of an action type previously not
 * registered for the corresponding event type) has been added for an event
 * type.
 *
 * @author Emil Ivov
 */
public class NotificationActionAddedEvent
    extends EventObject
{
    /**
     * The type of the notification action that is being added.
     */
    private String actionType = null;

    /**
     * The type of the event that a new action is being added for.
     */
    private String eventType = null;

    /**
     * The descriptor of the action (i.e. audio file uri, or a command line
     * string) that will be performed when notifications are being fired for
     * the corresponding event type.
     */
    private String actionDescriptor = null;

    /**
     * Creates an instance of this event according to the specified type.
     * @param source NotificationService
     * @param eventType String
     * @param actionType String
     * @param actionDescriptor String
     */
    public NotificationActionAddedEvent(NotificationService source,
                                        String eventType,
                                        String actionType,
                                        String actionDescriptor)
    {
        super(source);
        this.eventType = eventType;
        this.actionType = actionType;
        this.actionDescriptor = actionDescriptor;
    }

}
