/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 *
 * MessageSSHImpl.java
 *
 * SSH Suport in SIP Communicator - GSoC' 07 Project
 *
 */

package net.java.sip.communicator.impl.protocol.ssh;

import net.java.sip.communicator.service.protocol.*;

/**
 * Very simple message implementation for the SSH protocol.
 *
 * @author Shobhit Jindal
 */
public class MessageSSHImpl
    implements Message
{
    /**
     * The actual message content.
     */
    private String textContent = null;

    /**
     * The content type of the message. 
     */
    public static String contentType = "text/plain";

    /**
     * The message encoding. (UTF8 if null).
     */
    private String contentEncoding = null;

    /**
     * A String uniquely identifying the message
     */
    private String messageUID = null;

    /**
     * The subject of the message. (most often is null)
     */
    private String subject = null;

    /**
     * Creates a message instance according to the specified parameters.
     *
     * @param content the message body
     * @param contentType message content type or null for text/plain
     * @param contentEncoding message encoding or null for UTF8
     * @param subject the subject of the message or null for no subject.
     */
    public MessageSSHImpl(String content,
                                String contentType,
                                String contentEncoding,
                                String subject)
    {
        this.textContent = content;
        this.contentType = contentType;
        this.contentEncoding = contentEncoding;
        this.subject = subject;

        //generate the uid
        this.messageUID = String.valueOf(System.currentTimeMillis())
            + String.valueOf(hashCode());

    }

    /**
     * Returns the message body.
     *
     * @return the message content.
     */
    public String getContent()
    {
        return textContent;
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

    /**
     * Returns the encoding used for the message content.
     *
     * @return the encoding of the message body.
     */
    public String getEncoding()
    {
        return contentEncoding;
    }

    /**
     * A string uniquely identifying the message.
     *
     * @return a <tt>String</tt> uniquely identifying the message.
     */
    public String getMessageUID()
    {
        return messageUID;
    }

    /**
     * Returns the message body in a binary form.
     *
     * @return a <tt>byte[]</tt> representation of the message body.
     */
    public byte[] getRawData()
    {
        return getContent().getBytes();
    }

    /**
     * Return the length of this message.
     *
     * @return the length of this message.
     */
    public int getSize()
    {
        return getContent().length();
    }

    /**
     * Returns the message subject.
     *
     * @return the message subject.
     */
    public String getSubject()
    {
        return subject;
    }
}
