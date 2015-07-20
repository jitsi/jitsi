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
package net.java.sip.communicator.impl.protocol.irc.exception;

import net.java.sip.communicator.impl.protocol.irc.*;

/**
 * Exception indicating a bad command implementation.
 *
 * @author Danny van Heumen
 */
public class BadCommandException
    extends Exception
{
    /**
     * Serialization id.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The command name used to look up the type.
     */
    private final String command;

    /**
     * The command type that caused the exception.
     */
    private Class<? extends Command> type;

    /**
     * Constructor.
     *
     * @param command the command
     * @param type the command implementation
     * @param message the error message
     * @param e the cause
     */
    public BadCommandException(final String command,
        final Class<? extends Command> type, final String message,
        final Throwable e)
    {
        super("An error occurred while preparing command '" + command + "' ("
            + type.getCanonicalName() + "): " + message, e);
        this.command = command;
    }

    /**
     * Get the command.
     *
     * @return returns the name of the failed command
     */
    public String getCommand()
    {
        return this.command;
    }

    /**
     * Get the type of the implementation.
     *
     * @return returns the type that implements the command
     */
    public Class<? extends Command> getType()
    {
        return this.type;
    }
}
