/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.icq;

import net.java.sip.communicator.service.protocol.*;

/**
 * A simple implementation of the <tt>Message</tt> interface. Right now the
 * message only supports test contents and no binary data.
 *
 * @author Emil Ivov
 */
public class MessageIcqImpl
    implements Message
{
    /**
     * The content of this message.
     */
    private String textContent = null;

    /**
     * The content type of text. Right now only text content types (such as
     * text/plain or text/html) are supported.
     */
    private String contentType = null;

    /**
     * The encoding under which the contennt of this message is encoded.
     */
    private String contentEncoding = null;

    /**
     * An String uniquely identifying this Message.
     */
    private String messageUID = null;

    /**
     * The subject of the message if any (may remain null).
     */
    private String subject = null;

    /**
     * Creates an instance of this Message with the specified parameters.
     *
     * @param content the text content of the message.
     * @param contentType a MIME string indicating the content type of the
     * <tt>content</tt> String.
     * @param contentEncoding a MIME String indicating the content encoding of
     * the <tt>content</tt> String.
     * @param subject the subject of the message or null for empty.
     * @param messageUID an UID in case we'd like to set our own or null for an
     * automatically generated one.
     */
    public MessageIcqImpl(String content,
                          String contentType,
                          String contentEncoding,
                          String subject,
                          String messageUID)
    {
        this.textContent = content;
        this.contentType = contentType;
        this.contentEncoding = contentEncoding;
        this.subject = subject;

        if(messageUID == null)
        {
            //generate the uid
            this.messageUID = String.valueOf(System.currentTimeMillis())
                + String.valueOf(hashCode());
        }
        else
        {
            this.messageUID = messageUID;
        }
    }

    /**
     * Returns the content of this message if representable in text form or
     * null if this message does not contain text data.
     *
     * @return a String containing the content of this message or null if
     *   the message does not contain data representable in text form.
     */
    public String getContent()
    {
        return textContent;
    }

    /**
     * Returns the MIME type for the message content.
     *
     * @return a String containing the mime type of the message contant.
     */
    public String getContentType()
    {
        return contentType;
    }

    /**
     * Returns the MIME content encoding of this message.
     *
     * @return a String indicating the MIME encoding of this message.
     */
    public String getEncoding()
    {
        return contentEncoding;
    }

    /**
     * Returns a unique identifier of this message.
     *
     * @return a String that uniquely represents this message in the scope
     *   of this protocol.
     */
    public String getMessageUID()
    {
        return messageUID;
    }

    /**
     * Get the raw/binary content of an instant message.
     *
     * @return a byte[] array containing message bytes.
     */
    public byte[] getRawData()
    {
        return getContent().getBytes();
    }

    /**
     * Returns the size of the content stored in this message.
     *
     * @return an int indicating the number of bytes that this message
     *   contains.
     */
    public int getSize()
    {
        return getContent().length();
    }

    /**
     * Returns the subject of this message or null if the message contains no
     * subject.
     *
     * @return the subject of this message or null if the message contains
     *   no subject.
     */
    public String getSubject()
    {
        return subject;
    }
}
