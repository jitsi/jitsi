/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.irc.command;

import net.java.sip.communicator.impl.protocol.irc.*;

/**
 * Implementation of the /nick command. Change local user's nick name.
 *
 * @author Danny van Heumen
 */
public class Nick implements Command
{
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
     * Execute the /nick command. Issue nick change to IRC server.
     *
     * @param source the source channel/user
     * @param line the command message line
     */
    @Override
    public void execute(final String source, final String line)
    {
        if (line.length() <= 5)
        {
            // no name parameter available, so nothing to do here
            return;
        }
        final String part = line.substring(6);
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
        if (newNick.length() > 0)
        {
            this.connection.getIdentityManager().setNick(newNick);
        }
    }
}
