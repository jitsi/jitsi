/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.util.xml;

/**
 * The class is used to mask any XML specific exceptions thrown during parsing
 * various descriptors.
 *
 * @author Emil Ivov
 * @version 1.0
 */
public class XMLException
    extends Exception
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * Constructs a new XMLException with the specified detail message and cause.
     *
     * @param message a message specifying the reason that caused the
     * exception.
     *
     * @param cause the cause (which is saved
     * for later retrieval by the Throwable.getCause() method). (A null value is
     * permitted, and indicates that the cause is nonexistent or unknown.)
     */
    public XMLException(String message, Throwable cause)
    {
        super (message, cause);
    }

    /**
       * Constructs a new XMLException with the specified detail message.
       *
       * @param message a message specifying the reason that caused the
       * exception.
       */
    public XMLException(String message)
    {
        super(message);
    }
}
