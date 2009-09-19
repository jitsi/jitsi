/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.facebook;

/**
 * This exception is thrown if the protocol implementation is broken, i.e. when
 * the responses do not contain the expected results and consequently cannot be
 * parsed or resolve the situation properly.
 * 
 * @author Edgar Poce
 */
public class BrokenFacebookProtocolException
    extends Exception
{
    private static final long serialVersionUID = 0L;

    public BrokenFacebookProtocolException(String message)
    {
        super(message);
    }

    public BrokenFacebookProtocolException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public BrokenFacebookProtocolException(Throwable cause)
    {
        super(cause);
    }
}
