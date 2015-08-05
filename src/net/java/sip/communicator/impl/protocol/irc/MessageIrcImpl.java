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
package net.java.sip.communicator.impl.protocol.irc;

import net.java.sip.communicator.service.protocol.*;

/**
 * Very simple message implementation for the IRC protocol.
 *
 * @author Stephane Remy
 * @author Loic Kempf
 * @author Lubomir Marinov
 * @author Danny van Heumen
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
     * Default mime type for HTML messages.
     */
    public static final String HTML_MIME_TYPE = "text/html";

    /**
     * Create a Message instance from a piece of text directly from IRC. This
     * text might contain control characters for formatting as well as html
     * entities that have yet to be escaped.
     *
     * The IRC message is parsed, control codes replaced with html tags and html
     * entities in the original message are escaped.
     *
     * @param message the message from IRC
     * @return returns a Message instance with content
     */
    public static MessageIrcImpl newMessageFromIRC(final String message)
    {
        String text = Utils.parseIrcMessage(message);
        text = Utils.styleAsMessage(text);
        return new MessageIrcImpl(text, HTML_MIME_TYPE, DEFAULT_MIME_ENCODING,
            null);
    }

    /**
     * Create a new instance from an IRC text and parse the IRC message. (See
     * {@link #newMessageFromIRC(String)}.)
     *
     * @param user the originating user
     * @param message the IRC notice message
     * @return returns a new message instance
     */
    public static MessageIrcImpl newNoticeFromIRC(
        final ChatRoomMemberIrcImpl user, final String message)
    {
        return newNoticeFromIRC(user.getContactAddress(), message);
    }

    /**
     * Create a new instance from an IRC text and parse the IRC message. (See
     * {@link #newMessageFromIRC(String)}.)
     *
     * @param user the originating user
     * @param message the IRC notice message
     * @return returns a new message instance
     */
    public static MessageIrcImpl newNoticeFromIRC(final Contact user,
        final String message)
    {
        return newNoticeFromIRC(user.getAddress(), message);
    }

    /**
     * Construct the new notice message.
     *
     * @param user the originating user
     * @param message the IRC notice message
     * @return returns a new message instance
     */
    private static MessageIrcImpl newNoticeFromIRC(final String user,
        final String message)
    {
        String text = Utils.parseIrcMessage(message);
        text = Utils.styleAsNotice(text, user);
        return new MessageIrcImpl(text, HTML_MIME_TYPE, DEFAULT_MIME_ENCODING,
            null);
    }

    /**
     * Construct the new action message.
     *
     * @param message the IRC action message
     * @return returns a new message instance
     */
    public static MessageIrcImpl newActionFromIRC(final String message)
    {
        String text = Utils.parseIrcMessage(message);
        return new MessageIrcImpl("/me " + text, HTML_MIME_TYPE,
                DEFAULT_MIME_ENCODING, null);
    }

    /**
     * Construct the new away message.
     *
     * @param message the IRC away message
     * @return returns a new message instance
     */
    public static MessageIrcImpl newAwayMessageFromIRC(final String message)
    {
        String text = Utils.parseIrcMessage(message);
        text = Utils.styleAsAwayMessage(text);
        return new MessageIrcImpl(text, HTML_MIME_TYPE, DEFAULT_MIME_ENCODING,
            null);
    }

    /**
     * Creates a message instance according to the specified parameters.
     *
     * @param content the message body
     * @param contentType message content type or null for text/plain
     * @param contentEncoding message encoding or null for UTF8
     * @param subject the subject of the message or null for no subject.
     */
    public MessageIrcImpl(final String content, final String contentType,
        final String contentEncoding, final String subject)
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
        return getContent().startsWith("/me ");
    }

    /**
     * Sets the content to this <tt>Message</tt>. Used to change the content,
     * before showing action messages to the user.
     *
     * @param messageContent the new message content
     */
    @Override
    protected void setContent(final String messageContent)
    {
        super.setContent(messageContent);
    }
}
