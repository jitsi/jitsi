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
     * @param line the command message line
     */
    @Override
    public void execute(final String source, final String line)
    {
        final String command = line.substring(4);
        this.connection.getClient().act(source, command);
    }
}
