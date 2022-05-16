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

import net.java.sip.communicator.service.notification.event.*;

/**
 * The <tt>NotificationChangeListener</tt> is notified any time an action
 * type or an event type is added, removed or changed.
 *
 * @author Emil Ivov
 * @author Yana Stamcheva
 */
public interface NotificationChangeListener
    extends EventListener
{
    /**
     * This method gets called when a new notification action has been defined
     * for a particular event type.
     *
     * @param event the <tt>NotificationActionTypeEvent</tt>, which is
     * dispatched when a new action has been added.
     */
    public void actionAdded(NotificationActionTypeEvent event);

    /**
     * This method gets called when a notification action for a particular event
     * type has been removed.
     *
     * @param event the <tt>NotificationActionTypeEvent</tt>, which is
     * dispatched when an action has been removed.
     */
    public void actionRemoved(NotificationActionTypeEvent event);

    /**
     * This method gets called when a notification action for a particular event
     * type has been changed (for example the corresponding descriptor has
     * changed).
     *
     * @param event the <tt>NotificationActionTypeEvent</tt>, which is
     * dispatched when an action has been changed.
     */
    public void actionChanged(NotificationActionTypeEvent event);

    /**
     * This method gets called when a new event type has been added.
     *
     * @param event the <tt>NotificationEventTypeEvent</tt>, which is dispatched
     * when a new event type has been added
     */
    public void eventTypeAdded(NotificationEventTypeEvent event);

    /**
     * This method gets called when an event type has been removed.
     *
     * @param event the <tt>NotificationEventTypeEvent</tt>, which is dispatched
     * when an event type has been removed.
     */
    public void eventTypeRemoved(NotificationEventTypeEvent event);
}
