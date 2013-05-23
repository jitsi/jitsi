/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.dict;

import net.java.sip.communicator.service.protocol.*;

/**
 * Very simple message implementation for the Dict protocol.
 *
 * @author ROTH Damien
 * @author LITZELMANN Cedric
 * @author Lubomir Marinov
 */
public class MessageDictImpl
    extends AbstractMessage
{

    /**
     * Creates a message instance according to the specified parameters.
     *
     * @param content the message body
     * @param contentType message content type or null for text/plain
     * @param contentEncoding message encoding or null for UTF8
     * @param subject the subject of the message or null for no subject.
     */
    public MessageDictImpl(String content, String contentType,
        String contentEncoding, String subject)
    {
        super(content, contentType, contentEncoding, subject);
    }
}
