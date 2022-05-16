package net.java.sip.communicator.impl.protocol.jabber;

import org.jivesoftware.smack.XMPPException;

/**
 * Created by Ingo on 05.02.2017.
 */
public class JitsiXmppException extends XMPPException
{
    public JitsiXmppException(String message)
    {
        super(message);
    }

    public JitsiXmppException(String message, Exception inner)
    {
        super(message, inner);
    }
}
