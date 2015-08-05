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
    @Override
    public String getContentType()
    {
        return contentType;
    }
}
