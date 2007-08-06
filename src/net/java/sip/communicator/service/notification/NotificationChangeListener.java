/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.notification;

import java.util.*;

/**
 *
 * @author Emil Ivov
 */
public interface NotificationChangeListener
    extends EventListener
{
    /**
     * This method gets called when a new notification action has been defined
     * for one of the event types defined for a particular event type.
     */
    public void actionAdded();

    public void actionRemoved();

    public void actionChanged();

    public void eventTypeAdded();

    public void eventTypeRemoved();

}
