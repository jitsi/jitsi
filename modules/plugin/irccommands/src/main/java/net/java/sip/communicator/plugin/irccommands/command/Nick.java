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
 * Implementation of the /nick command. Change local user's nick name.
 *
 * @author Danny van Heumen
 */
public class Nick implements Command
{
    /**
     * Index for end of nick command prefix.
     */
    private static final int END_OF_COMMAND_PREFIX_INDEX = 6;

    /**
     * Instance of the IRC connection.
     */
    private IrcConnection connection;

    /**
     * Initialization of the command. Issue nick change.
     *
     * @param provider the provider instance
     * @param connection the connection instance
     */
    public Nick(final ProtocolProviderServiceIrcImpl provider,
            final IrcConnection connection)
    {
        if (connection == null)
        {
            throw new IllegalArgumentException("connection cannot be null");
        }
        this.connection = connection;
    }

    /**
     * Execute the /nick command. Issue nick change to IRC server.
     *
     * @param source the source channel/user
     * @param line the command message line
     */
    @Override
    public void execute(final String source, final String line)
    {
        if (line.length() <= END_OF_COMMAND_PREFIX_INDEX)
        {
            // no name parameter available, so nothing to do here
            throw new IllegalArgumentException("New nick name is missing.");
        }
        final String part = line.substring(END_OF_COMMAND_PREFIX_INDEX);
        final String newNick;
        int indexOfSep = part.indexOf(' ');
        if (indexOfSep == -1)
        {
            newNick = part;
        }
        else
        {
            newNick = part.substring(0, indexOfSep);
        }
        if (!newNick.isEmpty())
        {
            this.connection.getIdentityManager().setNick(newNick);
        }
    }

    /**
     * Usage instructions.
     */
    @Override
    public String help()
    {
        return "Usage: /nick <new-nick>";
    }
}
