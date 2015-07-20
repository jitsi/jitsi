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
 * Implementation of the /msg command. Send a targeted private message.
 *
 * @author Danny van Heumen
 */
public class Msg implements Command
{
    /**
     * Index of start of nick and message parameters.
     */
    private static final int END_OF_MSG_COMMAND_PREFIX = 5;

    /**
     * Instance of the IRC connection.
     */
    private IrcConnection connection;

    /**
     * Initialization of the /msg command.
     *
     * @param provider the provider instance
     * @param connection the connection instance
     */
    public Msg(final ProtocolProviderServiceIrcImpl provider,
            final IrcConnection connection)
    {
        if (connection == null)
        {
            throw new IllegalArgumentException("connection cannot be null");
        }
        this.connection = connection;
    }

    /**
     * Execute the command: send the message to the specified target.
     *
     * @param line the command message line
     * @param source the originating channel/user from which the message was
     * sent.
     */
    @Override
    public void execute(final String source, final String line)
    {
        if (line.length() < END_OF_MSG_COMMAND_PREFIX)
        {
            throw new IllegalArgumentException(
                "Both target nick and message are missing.");
        }
        final String part = line.substring(5);
        int endOfNick = part.indexOf(' ');
        if (endOfNick == -1)
        {
            throw new IllegalArgumentException("Invalid private message "
                    + "format: Expecting both nick and message. Message was "
                    + "not sent.");
        }
        final String target = part.substring(0, endOfNick);
        if (target.length() == 0)
        {
            throw new IllegalArgumentException("Invalid private message "
                + "format: Zero-length nick is not allowed. Message was not "
                + "sent.");
        }
        final String message = part.substring(endOfNick + 1);
        if (message.length() == 0)
        {
            throw new IllegalArgumentException("Invalid private message "
                + "format: Zero-length message is not allowed. Message was not "
                + "sent.");
        }
        this.connection.getClient().message(target, message);
    }

    /**
     * Usage instructions.
     */
    @Override
    public String help()
    {
        return "Usage: /msg <user> <message>";
    }
}
