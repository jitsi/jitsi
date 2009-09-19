/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.facebook;

import net.java.sip.communicator.service.protocol.*;

/**
 * Very simple message implementation for the Facebook protocol.
 * 
 * @author Dai Zhiwei
 * @author Edgar Poce
 */
public class MessageFacebookImpl
    extends AbstractMessage 
{
    public MessageFacebookImpl(
            String content,
            String contentType,
            String encoding,
            String subject)
    {
        super(content, contentType, encoding, subject);
    }
}
