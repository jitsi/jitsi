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

/**
 * An implementation of the <tt>CommandNotificationHandler</tt> interface.
 *
 * @author Yana Stamcheva
 */
public class CommandNotificationAction
    extends NotificationAction
{
    private String commandDescriptor;

    /**
     * Creates an instance of <tt>CommandNotification</tt> by
     * specifying the <tt>commandDescriptor</tt>, which will point us to the
     * command to execute.
     *
     * @param commandDescriptor a String that should point us to the command to
     * execute
     */
    public CommandNotificationAction(String commandDescriptor)
    {
        super(NotificationAction.ACTION_COMMAND);
        this.commandDescriptor = commandDescriptor;
    }

    /**
     * Returns the command descriptor.
     *
     * @return the command descriptor
     */
    public String getDescriptor()
    {
        return commandDescriptor;
    }
}
