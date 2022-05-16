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

import net.java.sip.communicator.impl.protocol.irc.exception.*;

/**
 * Interface for the implementation of individual IRC commands.
 *
 * <p>
 * This interface defines the format for the implementation of commands that can
 * be called for via the messaging input field.
 * </p>
 *
 * <p>
 * A new command object is instantiated for each encounter. Then
 * {@link #execute} is called to execute the command.
 * </p>
 *
 * <p>
 * Instantiation will be done by the CommandFactory. The CommandFactory expects
 * to find a constructor with the following types as arguments (in order):
 * </p>
 * <ol>
 * <li>{@link ProtocolProviderServiceIrcImpl}</li>
 * <li>{@link IrcConnection}</li>
 * </ol>
 *
 * <p>
 * If no suitable constructor is found, an {@link BadCommandException} will be
 * thrown.
 * </p>
 *
 * <p>
 * For more advanced IRC commands, one can register listeners with the
 * underlying IRC client. This way you can intercept IRC messages as they are
 * received. Using this method you can send messages, receive replies and act on
 * (other) events.
 * </p>
 *
 * <p>
 * In the case of more advanced commands that register message listeners, please
 * base your listeners on the AbstractIrcMessageListener or ensure by yourself
 * that listeners get unregistered upon ERROR or QUIT events. It is expected
 * that a listener that gets registered can determine for themselves when to
 * unregister itself in order to properly dispose of it.
 * </p>
 *
 * @author Danny van Heumen
 */
public interface Command
{
    /**
     * Execute the command using the full line.
     *
     * @param source the source channel/user from which the message is sent.
     *            (Note that for a normal message this would then be the
     *            target/receiver of the message too.)
     * @param line the command message line
     * @throws IllegalArgumentException Special meaning has been given to
     *             IllegalArgumentException: it signals bad usage of a command.
     *             Jitsi will consequently call {@link #help()} to query a help
     *             message that will be passed on as a system message to the
     *             user.
     * @throws IllegalStateException IllegalStateException signals bad usage of
     *             a command given the current state, i.e. called at the wrong
     *             time. Jitsi will consequently call {@link #help()} to query a
     *             help message that will be passed on as a system message to
     *             the user.
     */
    void execute(String source, String line);

    /**
     * Return help information to output.
     *
     * <p>
     * {@link #help()} is called whenever a command execution fails with
     * {@link IllegalArgumentException} or an {@link IllegalStateException}.
     * IllegalArgumentException suggests that the command was called
     * incorrectly. IllegalStateException suggests that the command was called
     * at the wrong time, when it is not appropriate to call this command. The
     * help information will then be displayed to explain the user how to use
     * the command.
     * </p>
     * 
     * <p>
     * Since a new command instance is constructed for each command message, the
     * help message can be adapted to reflect the earlier call to
     * {@link #execute}, if any.
     * </p>
     *
     * @return returns help information to display
     */
    String help();
}
