/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.irc.command;

import net.java.sip.communicator.impl.protocol.irc.*;

/**
 * Implementation of the /msg command. Send a targeted private message.
 *
 * @author Danny van Heumen
 */
public class Msg implements Command
{
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
     * Execute the command: send the message to the specified target.
     *
     * @param line the command message line
     * @param source the originating channel/user from which the message was
     * sent.
     */
    @Override
    public void execute(final String source, final String line)
    {
        final String part = line.substring(5);
        int endOfNick = part.indexOf(' ');
        if (endOfNick == -1)
        {
            throw new IllegalArgumentException("Invalid private message "
                    + "format. Message was not sent.");
        }
        final String target = part.substring(0, endOfNick);
        final String command = part.substring(endOfNick + 1);
        this.connection.getClient().message(target, command);
    }
}
