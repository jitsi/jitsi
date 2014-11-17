/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.irc;

import net.java.sip.communicator.impl.protocol.irc.exception.*;

/**
 * Interface for the implementation of individual IRC commands.
 *
 * This interface defines the format for the implementation of commands that can
 * be called for via the messaging input field.
 *
 * A new command object is instantiated for each encounter. Then
 * {@link #execute} is called to execute the command.
 *
 * Instantiation will be done by the CommandFactory. The CommandFactory expects
 * to find a constructor with the following types as arguments (in order):
 * <ol>
 * <li>{@link ProtocolProviderServiceIrcImpl}</li>
 * <li>{@link IrcConnection}</li>
 * </ol>
 *
 * If no suitable constructor is found, an {@link BadCommandException} will be
 * thrown.
 *
 * For more advanced IRC commands, one can register listeners with the
 * underlying IRC client. This way you can intercept IRC messages as they are
 * received. Using this method you can send messages, receive replies and act on
 * (other) events.
 *
 * FIXME Additionally, describe the AbstractIrcMessageListener type once this
 * implementation is merged with Jitsi main line.
 *
 * @author Danny van Heumen
 */
public interface Command
{
    /**
     * Execute the command using the full line.
     *
     * @param source the source channel/user from which the message is sent.
     * (Note that for a normal message this would then be the target/receiver of
     * the message too.)
     * @param line the command message line
     */
    void execute(String source, String line);
}
