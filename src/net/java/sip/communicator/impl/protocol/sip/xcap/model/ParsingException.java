/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip.xcap.model;

/**
 * Exceptions of this class get thrown whenever an error occurs while operating
 * with XCAP server.
 *
 * @author Grigorii Balutsel
 */
public class ParsingException extends Exception
{
    /**
     * Creates a new <code>XCapException</code> instance
     * which does not give a human-readable explanation why the operation is
     * not supported.
     */
    public ParsingException()
    {
    }

    /**
     * Creates a new <code>XCapException</code> instance whith human-readable
     * explanation.
     *
     * @param message the detailed message explaining any particular details as
     *                to why is not the specified operation supported or null if
     *                no particular details exist.
     */
    public ParsingException(String message)
    {
        super(message);
    }

    /**
     * Creates a new <code>XCapException</code> instance with human-readable
     * explanation and the original cause of the problem.
     *
     * @param message the detailed message explaining any particular details as
     *                to why is not the specified operation supported or null if
     *                no particular details exist.
     * @param cause   the original cause of the problem.
     */
    public ParsingException(String message, Throwable cause)
    {
        super(message, cause);
    }

    /**
     * Creates a new <code>XCapException</code> instance with the original cause
     * of the problem.
     *
     * @param cause the original cause of the problem.
     */
    public ParsingException(Throwable cause)
    {
        super(cause);
    }
}