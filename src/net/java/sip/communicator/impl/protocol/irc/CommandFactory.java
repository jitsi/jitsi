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
package net.java.sip.communicator.impl.protocol.irc;

import java.lang.reflect.*;
import java.util.*;
import java.util.Map.Entry;

import net.java.sip.communicator.impl.protocol.irc.exception.*;
import net.java.sip.communicator.util.*;

/**
 * Command factory.
 *
 * TODO Consider implementing 1 "management" command for querying the registered
 * commands at runtime and maybe some other things. Not very urgent, though.
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
                    LOGGER.debug("Unregistered command '" + entry.getKey()
                        + "' (" + type.toString() + ")");
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
     * @throws BadCommandException In case of a incompatible command or bad
     *             implementation.
     */
    public Command createCommand(final String command)
        throws UnsupportedCommandException,
        BadCommandException
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
        try
        {
            Constructor<? extends Command> cmdCtor =
                type.getConstructor(ProtocolProviderServiceIrcImpl.class,
                    IrcConnection.class);
            return cmdCtor.newInstance(this.provider, this.connection);
        }
        catch (NoSuchMethodException e)
        {
            throw new BadCommandException(command, type,
                "There is no compatible constructor for instantiating this "
                    + "command.", e);
        }
        catch (InstantiationException e)
        {
            throw new BadCommandException(command, type,
                "Unable to instantiate this command class.", e);
        }
        catch (IllegalAccessException e)
        {
            throw new BadCommandException(command, type,
                "Unable to access the constructor of this command class.", e);
        }
        catch (InvocationTargetException e)
        {
            throw new BadCommandException(command, type,
                "An exception occurred while executing the constructor.", e);
        }
        catch (IllegalArgumentException e)
        {
            throw new BadCommandException(command, type,
                "Invalid arguments were passed to the "
                    + "constructor. This may be a bug in the CommandFactory "
                    + "implementation.", e);
        }
    }
}
