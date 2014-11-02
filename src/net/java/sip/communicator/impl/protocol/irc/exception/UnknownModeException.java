/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.irc.exception;

/**
 * (Checked) Exception for unknown mode symbols.
 *
 * @author Danny van Heumen
 */
public class UnknownModeException extends Exception
{
    /**
     * Serialization id.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Symbol of unknown mode.
     */
    private final char symbol;

    /**
     * Constructor.
     *
     * @param symbol the unknown mode's symbol
     */
    public UnknownModeException(final char symbol)
    {
        super("Encountered an unknown mode: " + symbol + ".");
        this.symbol = symbol;
    }

    /**
     * Get the symbol of the unknown mode.
     *
     * @return returns the IRC mode symbol
     */
    public char getSymbol()
    {
        return this.symbol;
    }
}
