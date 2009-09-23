/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.mock;

import net.java.sip.communicator.service.protocol.*;

/**
 * Message Impl.
 * 
 * @author Damian Minkov
 * @author Lubomir Marinov
 */
public class MockMessage
    extends AbstractMessage
{
    MockMessage(String content, String contentType, String contentEncoding,
        String subject)
    {
        super(content, contentType, contentEncoding, subject);
    }

    MockMessage(String content)
    {
        this(content, OperationSetBasicInstantMessaging.DEFAULT_MIME_TYPE,
            OperationSetBasicInstantMessaging.DEFAULT_MIME_ENCODING, null);
    }
}
