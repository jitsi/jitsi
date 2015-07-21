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
package net.java.sip.communicator.plugin.irccommands.command;

import net.java.sip.communicator.impl.protocol.irc.*;

/**
 * Implementation of the /me command. Send a message describing an act by local
 * user.
 *
 * @author Danny van Heumen
 */
public class Me
    implements Command
{
    /**
     * Me command prefix index.
     */
    private static final int END_OF_ME_COMMAND_PREFIX = 4;

    /**
     * IRC connection instance.
     */
    private final IrcConnection connection;

    /**
     * Initialization of the /me command.
     *
     * @param provider the provider instance
     * @param connection the connection instance
     */
    public Me(final ProtocolProviderServiceIrcImpl provider,
            final IrcConnection connection)
    {
        if (connection == null)
        {
            throw new IllegalArgumentException("connection cannot be null");
        }
        this.connection = connection;
    }

    /**
     * Execute the /me command: send ACT command.
     *
     * @param source source chat room or user from which the command originated.
     * @param line the command message line
     */
    @Override
    public void execute(final String source, final String line)
    {
        if (line.length() < END_OF_ME_COMMAND_PREFIX)
        {
            throw new IllegalArgumentException("The message is missing.");
        }
        final String message = line.substring(4);
        if (message.isEmpty())
        {
            throw new IllegalArgumentException(
                "Invalid /me command: message cannot be empty.");
        }
        this.connection.getClient().act(source, message);
    }

    /**
     * Command usage instructions.
     */
    @Override
    public String help()
    {
        return "Usage: /me <message>";
    }
}
