/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

/**
 * The OperationNotSupportedException is used by telephony providers as an
 * indication that a requested operation is not supported or implemented.
 *
 * @author Emil Ivov
 * @author Lubomir Marinov
 */
public class OperationNotSupportedException
    extends Exception
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * Initializes a new <code>OperationNotSupportedException</code> instance
     * which does not give a human-readable explanation why the operation is
     * not supported.
     */
    public OperationNotSupportedException()
    {
        this(null);
    }

    /**
     * Creates an OperationNotSupportedException instance with the specified
     * reason phrase.
     *
     * @param message
     *            a detailed message explaining any particular details as to why
     *            is not the specified operation supported or null if no
     *            particular details exist.
     */
    public OperationNotSupportedException(String message)
    {
        super(message);
    }
}
