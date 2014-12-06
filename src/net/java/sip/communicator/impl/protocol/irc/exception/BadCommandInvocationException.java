/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.irc.exception;

/**
 * Exception thrown in case an IRC command is used in the wrong way.
 *
 * @author Danny van Heumen
 */
public class BadCommandInvocationException
    extends Exception
{
    /**
     * Serialization id.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The original command line.
     */
    private final String line;

    /**
     * The help text provided by the command.
     */
    private final String help;

    /**
     * Constructor.
     *
     * @param line the original command line
     * @param help the help text provided by the command
     * @param cause the cause of the exception
     */
    public BadCommandInvocationException(final String line, final String help, final Throwable cause)
    {
        super("The command failed because of incorrect usage: "
            + cause.getMessage(), cause);
        this.line = line;
        this.help = help;
    }

    /**
     * Get original command line.
     *
     * @return returns the original command line
     */
    public String getLine()
    {
        return this.line;
    }

    /**
     * Get the help text provided by the command.
     *
     * @return returns command's help text
     */
    public String getHelp()
    {
        return this.help;
    }
}
