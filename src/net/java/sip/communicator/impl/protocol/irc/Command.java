/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.irc;

/**
 * Interface for the implementation of individual IRC commands.
 *
 * This interface defines the format for the implementation of commands that can
 * be called for via the messaging input field.
 *
 * A command is instantiated for each encounter. {@link #init} will be upon
 * instantiation to set up the state before actually performing the command.
 * Then {@link #execute} is called in order to execute the command.
 *
 * @author Danny van Heumen
 */
public interface Command
{
    /**
     * Initialize this instance of a command. Initialization may include
     * registering listeners for the current connection such that a command can
     * act upon the server's response.
     *
     * {@link #init} is guaranteed to be called before the command gets
     * executed and thus can be used to initialize any dependencies or listeners
     * that are required for the command to be executed.
     *
     * @param provider the protocol provider service
     * @param connection the IRC connection instance
     */
    void init(ProtocolProviderServiceIrcImpl provider,
            IrcConnection connection);

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
