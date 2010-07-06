/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.portaudio;

/**
 * @author Lubomir Marinov
 */
public class PortAudioException
    extends Exception
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * Constructs a <tt>PortAudioException</tt> with <tt>message</tt> as
     * description.
     * @param message description of the exception
     */
    public PortAudioException(String message)
    {
        super(message);
    }
}
