/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.util.dns;

import java.io.*;

import org.xbill.DNS.*;

/**
 * DNS Message that adds DNSSEC validation information.
 * 
 * @author Ingo Bauersachs
 */
public class SecureMessage
    extends Message
{
    private boolean secure;
    private boolean bogus;
    private String bogusReason;

    /**
     * Creates a new instance of this class based on data received from an
     * Unbound resolve.
     * 
     * @param msg The answer of the Unbound resolver.
     * @throws IOException
     */
    public SecureMessage(UnboundResult msg) throws IOException
    {
        super(msg.answerPacket);
        secure = msg.secure;
        bogus = msg.bogus;
        bogusReason = msg.whyBogus;
    }

    /**
     * Indicates whether the answer is secure.
     * @return True, if the result is validated securely.
     */
    public boolean isSecure()
    {
        return secure;
    }

    /**
     * Indicates if there was a validation failure.
     * 
     * @return If the result was not secure (secure == false), and this result
     *         is due to a security failure, bogus is true.
     */
    public boolean isBogus()
    {
        return bogus;
    }

    /**
     * If the result is bogus this contains a string that describes the failure.
     * 
     * @return string that describes the failure.
     */
    public String getBogusReason()
    {
        return bogusReason;
    }

    /**
     * Converts the Message to a String. The fields secure, bogus and whyBogus
     * are append as a comment.
     */
    @Override
    public String toString()
    {
        StringBuilder s = new StringBuilder(super.toString());
        s.append('\n');
        s.append(";; Secure: ");
        s.append(secure);
        s.append('\n');
        s.append(";; Bogus:  ");
        s.append(bogus);
        s.append('\n');
        if(bogus)
        {
            s.append(";;  Reason: ");
            s.append(bogusReason);
            s.append('\n');
        }
        return s.toString();
    }
}
