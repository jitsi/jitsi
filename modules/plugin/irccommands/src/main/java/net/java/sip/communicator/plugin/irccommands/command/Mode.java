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
 * Implementation of the /mode command. Manually manage modes.
 *
 * @author Danny van Heumen
 */
public class Mode implements Command
{
    /**
     * Constant for end index of command prefix.
     */
    private static final int END_OF_MODE_COMMAND_PREFIX = 6;

    /**
     * Instance of the IRC connection.
     */
    private IrcConnection connection;

    /**
     * Initialization of the /mode command.
     *
     * @param provider the provider instance
     * @param connection the connection instance
     */
    public Mode(final ProtocolProviderServiceIrcImpl provider,
            final IrcConnection connection)
    {
        if (connection == null)
        {
            throw new IllegalArgumentException("connection cannot be null");
        }
        this.connection = connection;
    }

    /**
     * Execute the command: send the mode change message.
     *
     * @param line the command message line
     * @param source the originating channel/user from which the message was
     * sent.
     */
    @Override
    public void execute(final String source, final String line)
    {
        if (line.length() <= END_OF_MODE_COMMAND_PREFIX)
        {
            // does not currently support requesting (and displaying) mode query
            // results.
            throw new IllegalArgumentException("Mode parameters are missing.");
        }
        final String rawModeString =
            line.substring(END_OF_MODE_COMMAND_PREFIX);
        if (rawModeString.trim().isEmpty())
        {
            throw new IllegalArgumentException(
                "The mode command needs mode parameters to function.");
        }
        this.connection.getClient().changeMode(source + " " + rawModeString);
    }

    /**
     * Usage instructions.
     */
    @Override
    public String help()
    {
        return "Usage: /mode <mode> [params ...]";
    }
}
