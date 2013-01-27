/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.dns;

/**
 * Exception that is being thrown when native Unbound code resulted in an error.
 *
 * @author Ingo Bauersachs
 */
public class UnboundException
    extends Exception
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * Creates a new instance of this class.
     *
     * @param message the detail message.
     */
    public UnboundException(String message)
    {
        super(message);
    }
}
