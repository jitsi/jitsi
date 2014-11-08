/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.irc.command;

import net.java.sip.communicator.impl.protocol.irc.*;

/**
 * Implementation of the /join command. Join a channel from the message input
 * line.
 *
 * @author Danny van Heumen
 */
public class Join implements Command
{
    /**
     * Instance of the IRC connection.
     */
    private IrcConnection connection;

    /**
     * Initialization of the /join command. Join a channel.
     *
     * @param provider the provider instance
     * @param connection the IRC connection instance
     */
    @Override
    public void init(final ProtocolProviderServiceIrcImpl provider,
            final IrcConnection connection)
    {
        if (connection == null)
        {
            throw new IllegalArgumentException("connection cannot be null");
        }
        this.connection = connection;
    }

    /**
     * Execute join command.
     *
     * @param source the source channel/user from which the command was typed.
     * @param line the command message line
     */
    @Override
    public void execute(final String source, final String line)
    {
        final String part = line.substring(6);
        final String channel;
        final String password;
        int indexOfSep = part.indexOf(' ');
        if (indexOfSep == -1)
        {
            channel = part;
            password = "";
        }
        else
        {
            channel = part.substring(0, indexOfSep);
            password = part.substring(indexOfSep + 1);
        }
        if (channel.matches("[^,\\n\\r\\s\\a]+"))
        {
            this.connection.getClient().joinChannel(channel, password);
        }
    }
}
