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
package net.java.sip.communicator.service.protocol.event;

/**
 * A listener that would gather and notify for incoming generic
 * events.
 *
 * @author Damian Minkov
 * @author Emil Ivov
 */
public interface GenericEventListener
{
    /**
     * Indicates that an incoming <tt>GenericEvent</tt> has been received.
     *
     * @param event the incoming event.
     */
    public void notificationReceived(GenericEvent event);

    /**
     * Indicates that a <tt>GenericEvent</tt> we previously tried to send
     * has not been delivered.
     *
     * @param event the <tt>GenericEvent</tt> instance describing the event
     * that we couldn't send.
     */
    public void notificationDeliveryFailed(GenericEvent event);


}
