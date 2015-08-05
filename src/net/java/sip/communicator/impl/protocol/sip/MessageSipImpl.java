/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
