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
package net.java.sip.communicator.service.protocol;

import net.java.sip.communicator.service.protocol.event.*;

/**
 * Provides notification for custom/generic events and possibility to generate
 * such events.
 *
 * @author Damian Minkov
 */
public interface OperationSetGenericNotifications
    extends OperationSet
{
    /**
     * Generates new generic event notification and send it to the
     * supplied contact.
     * @param contact the contact to receive the event notification.
     * @param eventName the event name of the notification.
     * @param eventValue the event value of the notification.
     */
    public void notifyForEvent(
            Contact contact,
            String eventName,
            String eventValue);

    /**
     * Generates new generic event notification and send it to the
     * supplied contact.
     * @param jid the contact jid which will receive the event notification.
     * @param eventName the event name of the notification.
     * @param eventValue the event value of the notification.
     */
    public void notifyForEvent(
            String jid,
            String eventName,
            String eventValue);

    /**
     * Generates new generic event notification and send it to the
     * supplied contact.
     * @param jid the contact jid which will receive the event notification.
     * @param eventName the event name of the notification.
     * @param eventValue the event value of the notification.
     * @param source the source that will be reported in the event.
     */
    public void notifyForEvent(
            String jid,
            String eventName,
            String eventValue,
            String source);

    /**
     * Registers a <tt>GenericEventListener</tt> with this
     * operation set so that it gets notifications for new
     * event notifications.
     *
     * @param eventName register the listener for certain event name.
     * @param listener the <tt>GenericEventListener</tt>
     * to register.
     */
    public void addGenericEventListener(
            String eventName,
            GenericEventListener listener);

    /**
     * Unregisters <tt>listener</tt> so that it won't receive any further
     * notifications upon new event notifications.
     *
     * @param eventName unregister the listener for certain event name.
     * @param listener the <tt>GenericEventListener</tt>
     * to unregister.
     */
    public void removeGenericEventListener(
            String eventName,
            GenericEventListener listener);
}
