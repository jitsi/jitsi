/*
 * MockMessage.java
 * 
 * Created on Jun 21, 2007, 3:10:21 PM
 * 
 * To change this template, choose Tools | Template Manager and open the
 * template in the editor.
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
