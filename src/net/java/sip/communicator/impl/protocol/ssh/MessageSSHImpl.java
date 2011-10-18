/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 * 
 * MessageSSHImpl.java
 * 
 * SSH Suport in SIP Communicator - GSoC' 07 Project
 */
package net.java.sip.communicator.impl.protocol.ssh;

import net.java.sip.communicator.service.protocol.*;

/**
 * Very simple message implementation for the SSH protocol.
 * 
 * @author Shobhit Jindal
 * @author Lubomir Marinov
 */
public class MessageSSHImpl
    extends AbstractMessage
{

    /**
     * The content type of the message.
     */
    public static String contentType = "text/plain";

    /**
     * Creates a message instance according to the specified parameters.
     * 
     * @param content the message body
     * @param contentType message content type or null for text/plain
     * @param contentEncoding message encoding or null for UTF8
     * @param subject the subject of the message or null for no subject.
     */
    public MessageSSHImpl(String content, String contentType,
        String contentEncoding, String subject)
    {
        super(content, null, contentEncoding, subject);

        MessageSSHImpl.contentType = contentType;
    }

    /**
     * Returns the type of the content of this message.
     * 
     * @return the type of the content of this message.
     */
    public String getContentType()
    {
        return contentType;
    }
}
