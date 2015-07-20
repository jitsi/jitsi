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
package net.java.sip.communicator.service.protocol;

import java.io.*;
import java.nio.charset.*;

import net.java.sip.communicator.util.*;

/**
 * Represents a default implementation of {@link Message} in order to make it
 * easier for implementers to provide complete solutions while focusing on
 * implementation-specific details.
 *
 * @author Lubomir Marinov
 */
public abstract class AbstractMessage
    implements Message
{
    /**
     * The <tt>Logger</tt> used by the <tt>AbstractMessage</tt> class and its
     * instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(AbstractMessage.class);

    private static boolean equals(String a, String b)
    {
        return (a == null) ? (b == null) : a.equals(b);
    }

    private String content;

    private final String contentType;

    private String encoding;

    private final String messageUID;

    /**
     * The content of this message, in raw bytes according to the encoding.
     */
    private byte[] rawData;

    private final String subject;

    protected AbstractMessage(String content, String contentType,
        String encoding, String subject)
    {
        this.contentType = contentType;
        this.subject = subject;

        setEncoding(encoding);
        setContent(content);

        this.messageUID = createMessageUID();
    }

    protected AbstractMessage(String content, String contentType,
        String encoding, String subject, String messageUID)
    {
        this.contentType = contentType;
        this.subject = subject;

        setEncoding(encoding);
        setContent(content);

        this.messageUID = messageUID == null ? createMessageUID() : messageUID;
    }

    protected String createMessageUID()
    {
        return String.valueOf(System.currentTimeMillis())
            + String.valueOf(hashCode());
    }

    /**
     * Returns the content of this message if representable in text form or null
     * if this message does not contain text data.
     * <p>
     * The implementation is final because it caches the raw data of the
     * content.
     * </p>
     *
     * @return a String containing the content of this message or null if the
     *         message does not contain data representable in text form.
     */
    public final String getContent()
    {
        return content;
    }

    /*
     * (non-Javadoc)
     *
     * @see net.java.sip.communicator.service.protocol.Message#getContentType()
     */
    public String getContentType()
    {
        return contentType;
    }

    /**
     * Returns the MIME content encoding of this message.
     * <p>
     * The implementation is final because of the presumption it can set the
     * encoding.
     * </p>
     *
     * @return a String indicating the MIME encoding of this message.
     */
    public final String getEncoding()
    {
        return encoding;
    }

    /*
     * (non-Javadoc)
     *
     * @see net.java.sip.communicator.service.protocol.Message#getMessageUID()
     */
    public String getMessageUID()
    {
        return messageUID;
    }

    /*
     * (non-Javadoc)
     *
     * @see net.java.sip.communicator.service.protocol.Message#getRawData()
     */
    public byte[] getRawData()
    {
        if (rawData == null)
        {
            String content = getContent();
            String encoding = getEncoding();
            boolean useDefaultEncoding = true;
            if (encoding != null)
            {
                try
                {
                    rawData = content.getBytes(encoding);
                    useDefaultEncoding = false;
                }
                catch (UnsupportedEncodingException ex)
                {
                    logger.warn(
                        "Failed to get raw data from content using encoding "
                            + encoding, ex);

                    // We'll use the default encoding
                }
            }
            if (useDefaultEncoding)
            {
                setEncoding(Charset.defaultCharset().name());
                rawData = content.getBytes();
            }
        }
        return rawData;
    }

    /*
     * (non-Javadoc)
     *
     * @see net.java.sip.communicator.service.protocol.Message#getSize()
     */
    public int getSize()
    {
        return getRawData().length;
    }

    /*
     * (non-Javadoc)
     *
     * @see net.java.sip.communicator.service.protocol.Message#getSubject()
     */
    public String getSubject()
    {
        return subject;
    }

    protected void setContent(String content)
    {
        if (equals(this.content, content) == false)
        {
            this.content = content;
            this.rawData = null;
        }
    }

    private void setEncoding(String encoding)
    {
        if (equals(this.encoding, encoding) == false)
        {
            this.encoding = encoding;
            this.rawData = null;
        }
    }
}
