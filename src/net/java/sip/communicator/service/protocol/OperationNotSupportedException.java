/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
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
 */
public class OperationNotSupportedException
    extends Exception
{
    /**
     * Creates an OperationNotSupportedException instance with the specified
     * reason phrase.
     * @param message a detailed message explaining any particular details as
     * to why is not the specified operation supported or null if no particular
     * details exist.
     */
    public OperationNotSupportedException(String message)
    {
        super(message);
    }

}
