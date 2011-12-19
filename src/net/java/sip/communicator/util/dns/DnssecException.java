/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.util.dns;

/**
 * Checked DNSSEC exception for code that knows how to deal with it.
 * 
 * @author Ingo Bauersachs
 */
public class DnssecException
    extends Exception
{
    /**
     * Creates a new instance of this class.
     * @param e the DNSSEC runtime exception to encapsulate.
     */
    public DnssecException(DnssecRuntimeException e)
    {
        super(e);
    }
}
