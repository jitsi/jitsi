/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.irc.exception;

import net.java.sip.communicator.impl.protocol.irc.*;

/**
 * Exception indicating a bad command implementation.
 *
 * @author Danny van Heumen
 */
public class BadCommandException
    extends Exception
{
    /**
     * Serialization id.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The command name used to look up the type.
     */
    private final String command;

    /**
     * The command type that caused the exception.
     */
    private Class<? extends Command> type;

    /**
     * Constructor.
     *
     * @param command the command
     * @param type the command implementation
     * @param message the error message
     * @param e the cause
     */
    public BadCommandException(final String command,
        final Class<? extends Command> type, final String message,
        final Throwable e)
    {
        super("An error occurred while preparing command '" + command + "' ("
            + type.getCanonicalName() + "): " + message, e);
        this.command = command;
    }

    /**
     * Get the command.
     *
     * @return returns the name of the failed command
     */
    public String getCommand()
    {
        return this.command;
    }

    /**
     * Get the type of the implementation.
     *
     * @return returns the type that implements the command
     */
    public Class<? extends Command> getType()
    {
        return this.type;
    }
}
