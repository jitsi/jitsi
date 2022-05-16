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

import java.util.*;

/**
 * This listener receives events whenever a contact has sent us a typing
 * notification. The source contact and the exact type of the notification
 * are indicated in <tt>TypingNotificationEvent</tt> instances.
 *
 * @author Emil Ivov
 */
public interface TypingNotificationsListener
    extends EventListener
{
    /**
     * Called to indicate that a remote <tt>Contact</tt> has sent us a typing
     * notification.
     * @param event a <tt>TypingNotificationEvent</tt> containing the sender
     * of the notification and its type.
     */
    public void typingNotificationReceived(TypingNotificationEvent event);

    /**
     * Called to indicate that sending typing notification has failed.
     *
     * @param event a <tt>TypingNotificationEvent</tt> containing the sender
     * of the notification and its type.
     */
    public void typingNotificationDeliveryFailed(TypingNotificationEvent event);
}
