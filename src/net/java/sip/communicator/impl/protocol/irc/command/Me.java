/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.irc.command;

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
    private IrcConnection connection;

    /**
     * Initialization of the /me command.
     *
     * @param provider the provider instance
     * @param connection the connection instance
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
            return;
        }
        final String message = line.substring(4);
        if (message.isEmpty())
        {
            throw new IllegalArgumentException(
                "Invalid /me command: message cannot be empty.");
        }
        this.connection.getClient().act(source, message);
    }
}
