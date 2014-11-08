/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.irc.exception;

/**
 * Exception indicating that a certain command is unsupported or unknown.
 * (Either way it is not being handled.)
 *
 * @author Danny van Heumen
 */
public class UnsupportedCommandException
    extends Exception
{
    /**
     * Serialization version.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The command.
     */
    private final String command;

    /**
     * Constructor.
     *
     * @param command the command
     */
    public UnsupportedCommandException(final String command)
    {
        super("Command '" + command + "' is unknown or unsupported.");
        this.command = command;
    }

    /**
     * The unsupported command that is the reason for this exception.
     *
     * @return returns command that is unsupported
     */
    public String getCommand()
    {
        return this.command;
    }
}
