/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.util.dns;

import java.net.UnknownHostException;

/**
 * Runtime exception that is thrown when a DNSSEC validation failure occurred.
 * This is not a checked exception or a derivative of
 * {@link UnknownHostException} so that existing code does not retry the lookup
 * (potentially in a loop).
 * 
 * @author Ingo Bauersachs
 */
public class DnssecRuntimeException
    extends RuntimeException
{
    /**
     * Creates a new instance of this class.
     * @param message The reason why this exception is thrown.
     */
    public DnssecRuntimeException(String message)
    {
        super(message);
    }
}
