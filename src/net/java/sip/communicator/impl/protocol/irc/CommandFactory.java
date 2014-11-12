/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.irc;

import java.util.*;
import java.util.Map.Entry;

import net.java.sip.communicator.impl.protocol.irc.exception.*;
import net.java.sip.communicator.util.*;

/**
 * Command factory.
 *
 * @author Danny van Heumen
 */
public class CommandFactory
{
    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(CommandFactory.class);

    /**
     * Registry for commands. Contains all available commands.
     */
    private static final Map<String, Class<? extends Command>> COMMANDS
            = new HashMap<String, Class<? extends Command>>();

    /**
     * Get an unmodifiable map of registered commands.
     *
     * @return returns an unmodifiable map of registered commands
     */
    public static synchronized Map<String, Class<? extends Command>>
    getCommands()
    {
        return new HashMap<String, Class<? extends Command>>(COMMANDS);
    }

    /**
     * Register a new command at the factory.
     *
     * @param command the command word
     * @param type the type to instantiate for command execution
     */
    public static synchronized void registerCommand(final String command,
            final Class<? extends Command> type)
    {
        if (command == null)
        {
            throw new IllegalArgumentException("command cannot be null");
        }
        if (type == null)
        {
            throw new IllegalArgumentException("type cannot be null");
        }
        COMMANDS.put(command, type);
        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug("Registered command '" + command + "' ("
                    + type.toString() + ")");
        }
    }

    /**
     * Unregister a command from the factory.
     *
     * @param type the type to unregister
     * @param command (optional) specify the command for which the type is
     *            registered. This can be used to unregister only one of
     *            multiple commands for the same implementation type.
     */
    public static synchronized void unregisterCommand(
            final Class<? extends Command> type,
            final String command)
    {
        Iterator<Entry<String, Class<? extends Command>>> it =
            COMMANDS.entrySet().iterator();
        while (it.hasNext())
        {
            Entry<String, Class<? extends Command>> entry = it.next();
            if (entry.getValue() == type
                && (command == null || command.equals(entry.getKey())))
            {
                it.remove();
                if (LOGGER.isDebugEnabled())
                {
                    LOGGER.debug("Unregistered command '" + command + "' ("
                            + type.toString() + ")");
                }
            }
        }
    }

    /**
     * Instance of protocol provider service.
     */
    private final ProtocolProviderServiceIrcImpl provider;

    /**
     * Instance of IRC connection.
     */
    private final IrcConnection connection;

    /**
     * Constructor for instantiating a command factory.
     *
     * @param provider the protocol provider service
     * @param connection the IRC connection
     */
    CommandFactory(final ProtocolProviderServiceIrcImpl provider,
            final IrcConnection connection)
    {
        if (provider == null)
        {
            throw new IllegalArgumentException("provider cannot be null");
        }
        this.provider = provider;
        if (connection == null)
        {
            throw new IllegalArgumentException("connection cannot be null");
        }
        this.connection = connection;
    }

    /**
     * Create new command based on the provided key if available in the command
     * registry.
     *
     * @param command the command to look up and instantiate
     * @return returns a command instance
     * @throws UnsupportedCommandException in case command cannot be found
     */
    public Command createCommand(final String command)
            throws UnsupportedCommandException
    {
        if (command == null || command.isEmpty())
        {
            throw new IllegalArgumentException(
                "command cannot be null or empty");
        }
        final Class<? extends Command> type = COMMANDS.get(command);
        if (type == null)
        {
            throw new UnsupportedCommandException(command);
        }
        final Command cmd;
        try
        {
            cmd = type.newInstance();
            cmd.init(this.provider, this.connection);
            return cmd;
        }
        catch (InstantiationException ex)
        {
            throw new IllegalStateException(
                    "A bad command implementation has been registered. It fails"
                    + " to instantiate.",
                    ex);
        }
        catch (IllegalAccessException ex)
        {
            throw new IllegalStateException(
                    "A bad command implementation has been registered. It does "
                    + "not allow access to its constructor and therefore cannot"
                    + " be instantiated.",
                    ex);
        }
    }
}
