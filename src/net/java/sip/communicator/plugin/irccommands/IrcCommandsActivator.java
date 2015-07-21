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
package net.java.sip.communicator.plugin.irccommands;

import java.util.*;
import java.util.Map.Entry;

import net.java.sip.communicator.impl.protocol.irc.*;
import net.java.sip.communicator.plugin.irccommands.command.*;
import net.java.sip.communicator.plugin.irccommands.command.Mode;

import org.osgi.framework.*;

/**
 * Activator of the IRC commands plugin.
 *
 * @author Danny van Heumen
 */
public class IrcCommandsActivator
    implements BundleActivator
{
    /**
     * Map of command-implementation mapping for IRC {@link Command}
     * implementations.
     *
     * This map is used as reference when registering and unregistering the
     * commands upon (resp.) starting and stopping the plugin bundle.
     */
    private static final Map<String, Class<? extends Command>> COMMANDS;

    /**
     * Building the commands registration reference.
     */
    static
    {
        HashMap<String, Class<? extends Command>> commands =
            new HashMap<String, Class<? extends Command>>();
        commands.put("me", Me.class);
        commands.put("msg", Msg.class);
        commands.put("mode", Mode.class);
        commands.put("nick", Nick.class);
        commands.put("join", Join.class);
        COMMANDS = Collections.unmodifiableMap(commands);
    }

    /**
     * Stopping the bundle.
     *
     * @param context the bundle context
     */
    @Override
    public void stop(final BundleContext context)
    {
        final Set<Class<? extends Command>> implementations =
            new HashSet<Class<? extends Command>>(COMMANDS.values());
        for (Class<? extends Command> impl : implementations)
        {
            CommandFactory.unregisterCommand(impl, null);
        }
    }

    /**
     * Starting the bundle.
     *
     * @param context the bundle context
     */
    @Override
    public void start(final BundleContext context)
    {
        for (Entry<String, Class<? extends Command>> entry : COMMANDS
            .entrySet())
        {
            CommandFactory.registerCommand(entry.getKey(), entry.getValue());
        }
    }
}
