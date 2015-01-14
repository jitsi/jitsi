/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
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
