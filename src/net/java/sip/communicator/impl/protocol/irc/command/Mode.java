/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.irc.command;

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
            return;
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
}
