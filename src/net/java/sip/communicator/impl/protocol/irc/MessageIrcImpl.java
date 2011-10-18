/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.irc;

import net.java.sip.communicator.service.protocol.*;

/**
 * Very simple message implementation for the IRC protocol.
 * 
 * @author Stephane Remy
 * @author Loic Kempf
 * @author Lubomir Marinov
 */
public class MessageIrcImpl
    extends AbstractMessage
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
     * Creates a message instance according to the specified parameters.
     * 
     * @param content the message body
     * @param contentType message content type or null for text/plain
     * @param contentEncoding message encoding or null for UTF8
     * @param subject the subject of the message or null for no subject.
     */
    public MessageIrcImpl(String content, String contentType,
        String contentEncoding, String subject)
    {
        super(content, contentType, contentEncoding, subject);
    }

    /**
     * Checks if this message is a command. In IRC all messages that start with
     * the '/' character are commands.
     * 
     * @return TRUE if this <tt>Message</tt> is a command, FALSE otherwise
     */
    public boolean isCommand()
    {
        return getContent().startsWith("/");
    }

    /**
     * Checks if this message is an action. All message starting with '/me' are
     * actions.
     * 
     * @return TRUE if this message is an action, FALSE otherwise
     */
    public boolean isAction()
    {
        return getContent().startsWith("/me");
    }

    /**
     * Sets the content to this <tt>Message</tt>. Used to change the content,
     * before showing action messages to the user.
     * 
     * @param messageContent the new message content
     */
    protected void setContent(String messageContent)
    {
        super.setContent(messageContent);
    }
}
