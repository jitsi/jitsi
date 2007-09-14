/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.irc;

import net.java.sip.communicator.service.protocol.*;

/**
 * Very simple message implementation for the IRC protocol.
 *
 * @author Stephane Remy
 * @author Loic Kempf
 */
public class MessageIrcImpl
    implements Message
{

    /**
     * Default encoding for outgoing messages.
     */
    public static final String DEFAULT_MIME_ENCODING = "UTF-8";

    /**
     * Default mime type for outgoing messages.
     */
    public static final String DEFAULT_MIME_TYPE = "text/plain";

    /**
     * The actual message content.
     */
    private String textContent = null;

    /**
     * The content type of the message. (text/html if null)
     */
    private String contentType = DEFAULT_MIME_TYPE;

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
    public MessageIrcImpl(String content,
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
        return textContent.getBytes();
    }

    /**
     * Return the length of this message.
     *
     * @return the length of this message.
     */
    public int getSize()
    {
        return textContent.length();
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
    /**
     * Checks if this message is a command. In IRC all messages that start with
     * the '/' character are commands.
     * 
     * @return TRUE if this <tt>Message</tt> is a command, FALSE otherwise
     */
    public boolean isCommand()
    {
        return textContent.startsWith("/");
    }

    /**
     * Checks if this message is an action. All message starting with '/me' are
     * actions.
     * 
     * @return TRUE if this message is an action, FALSE otherwise
     */
    public boolean isAction()
    {
        return textContent.startsWith("/me");
    }

    /**
     * Sets the content to this <tt>Message</tt>. Used to change the content,
     * before showing action messages to the user.
     * 
     * @param messageContent the new message content
     */
    protected void setContent(String messageContent)
    {
        this.textContent = messageContent;
    }
}