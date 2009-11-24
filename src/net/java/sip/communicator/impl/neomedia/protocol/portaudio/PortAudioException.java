/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.protocol.portaudio;

/**
 * @author Lubomir Marinov
 */
public class PortAudioException
    extends Exception
{
    private static final long serialVersionUID = 0L;

    public PortAudioException(String message)
    {
        super(message);
    }
}
