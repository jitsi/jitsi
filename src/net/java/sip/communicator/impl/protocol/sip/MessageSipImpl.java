/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip;

import net.java.sip.communicator.service.protocol.*;

/**
 * A simple implementation of the <tt>Message</tt> interface for SIP/SIMPLE.
 * 
 * @author Benoit Pradelle
 * @author Lubomir Marinov
 */
public class MessageSipImpl
    extends AbstractMessage
{

    /**
     * Creates an instance of this Message with the specified parameters.
     * 
     * @param content the text content of the message.
     * @param contentType a MIME string indicating the content type of the
     *            <tt>content</tt> String.
     * @param contentEncoding a MIME String indicating the content encoding of
     *            the <tt>content</tt> String.
     * @param subject the subject of the message or null for empty.
     */
    public MessageSipImpl(String content, String contentType,
        String contentEncoding, String subject)
    {
        super(content, contentType, contentEncoding, subject);
    }
}
