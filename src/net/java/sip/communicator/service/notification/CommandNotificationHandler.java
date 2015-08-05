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
 * The <tt>CommandNotificationHandler</tt> interface is meant to be implemented
 * by the notification bundle in order to provide handling of command actions.
 *
 * @author Yana Stamcheva
 */
public interface CommandNotificationHandler
    extends NotificationHandler
{
    /**
     * Executes the program pointed by the descriptor.
     *
     * @param action the action to act upon
     * @param cmdargs arguments that are passed to the command line specified
     * in the action
     */
    public void execute(
            CommandNotificationAction action,
            Map<String,String> cmdargs);
}
