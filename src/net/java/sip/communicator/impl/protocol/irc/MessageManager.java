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

import net.java.sip.communicator.impl.protocol.irc.exception.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

import com.ircclouds.irc.api.*;
import com.ircclouds.irc.api.domain.*;
import com.ircclouds.irc.api.domain.messages.*;
import com.ircclouds.irc.api.state.*;

/**
 * Manager for message-related operations.
 *
 * TODO Implement messaging service for offline messages and such. (MemoServ -
 * message relaying services)
 *
 * @author Danny van Heumen
 */
public class MessageManager
{
    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(MessageManager.class);

    /**
     * Index for the start of the command in a command message.
     */
    private static final int START_OF_COMMAND_INDEX = 1;

    /**
     * Safety net of 5 bytes to use as extra slack to prevent off-by-one
     * failures.
     */
    public static final int SAFETY_NET = 5;

    /**
     * Maximum message size for IRC messages given the spec specifies a buffer
     * of 512 bytes. The command ending (CRLF) takes up 2 bytes, so max is 510.
     */
    public static final int IRC_PROTOCOL_MAX_MESSAGE_SIZE = 510;

    /**
     * IrcConnection instance.
     */
    private final IrcConnection connection;

    /**
     * IRCApi instance.
     *
     * Instance must be thread-safe!
     */
    private final IRCApi irc;

    /**
     * Connection state.
     */
    private final IIRCState connectionState;

    /**
     * Protocol provider service.
     */
    private final ProtocolProviderServiceIrcImpl provider;

    /**
     * The command factory.
     */
    private final CommandFactory commandFactory;

    /**
     * Identity manager.
     */
    private final IdentityManager identity;

    /**
     * Constructor.
     *
     * @param connection IrcConnection instance
     * @param irc thread-safe IRCApi instance
     * @param connectionState the connection state
     * @param provider the provider instance
     * @param identity the identity manager
     */
    public MessageManager(final IrcConnection connection, final IRCApi irc,
            final IIRCState connectionState,
            final ProtocolProviderServiceIrcImpl provider,
            final IdentityManager identity)
    {
        if (connection == null)
        {
            throw new IllegalArgumentException("connection cannot be null");
        }
        this.connection = connection;
        if (irc == null)
        {
            throw new IllegalArgumentException("irc cannot be null");
        }
        this.irc = irc;
        if (connectionState == null)
        {
            throw new IllegalArgumentException(
                "connectionState cannot be null");
        }
        this.connectionState = connectionState;
        if (provider == null)
        {
            throw new IllegalArgumentException("provider cannot be null");
        }
        this.provider = provider;
        if (identity == null)
        {
            throw new IllegalArgumentException("identity cannot be null");
        }
        this.identity = identity;
        this.irc.addListener(new MessageManagerListener());
        this.commandFactory =
                new CommandFactory(this.provider, this.connection);
    }

    /**
     * Send a command to the IRC server.
     *
     * @param chatroom the chat room
     * @param message the command message
     * @throws UnsupportedCommandException for unknown or unsupported commands
     * @throws BadCommandException in case of incompatible command or bad
     *             implementation
     * @throws BadCommandInvocationException in case of bad usage of the
     *             command. An exception will be thrown that contains the root
     *             cause and optionally a help text containing usage information
     *             for that particular command.
     */
    public void command(final ChatRoomIrcImpl chatroom, final String message)
        throws UnsupportedCommandException,
        BadCommandException,
        BadCommandInvocationException
    {
        if (!this.connectionState.isConnected())
        {
            throw new IllegalStateException("Not connected to IRC server.");
        }
        command(chatroom.getIdentifier(), message);
    }

    /**
     * Send a command to the IRC server.
     *
     * @param contact the chat room
     * @param message the command message
     * @throws UnsupportedCommandException for unknown or unsupported commands
     * @throws BadCommandException in case of a bad command implementation
     * @throws BadCommandInvocationException in case of bad usage of the
     *             command. An exception will be thrown that contains the root
     *             cause and optionally a help text containing usage information
     *             for that particular command.
     */
    public void command(final Contact contact, final MessageIrcImpl message)
        throws UnsupportedCommandException,
        BadCommandException,
        BadCommandInvocationException
    {
        if (!this.connectionState.isConnected())
        {
            throw new IllegalStateException("Not connected to IRC server.");
        }
        command(contact.getAddress(), message.getContent());
    }

    /**
     * Issue a command representing a command interaction with IRC server.
     *
     * @param source Source contact or chat room from which the message is sent.
     * @param message Command message
     * @throws UnsupportedCommandException in case a suitable command could not
     *             be found
     * @throws BadCommandException in case of an incompatible command or a bad
     *             implementation
     * @throws BadCommandInvocationException in case of bad usage of the
     *             command. An exception will be thrown that contains the root
     *             cause and optionally a help text containing usage
     *             information for that particular command.
     */
    private void command(final String source, final String message)
        throws UnsupportedCommandException,
        BadCommandException,
        BadCommandInvocationException
    {
        final String msg = message.toLowerCase();
        final int end = msg.indexOf(' ');
        final String command;
        if (end == -1)
        {
            command = msg.substring(START_OF_COMMAND_INDEX);
        }
        else
        {
            command = message.substring(START_OF_COMMAND_INDEX, end);
        }
        final Command cmd = this.commandFactory.createCommand(command);
        try
        {
            cmd.execute(source, msg);
        }
        catch (IllegalArgumentException e)
        {
            // IRC command called incorrectly.
            final String help = cmd.help();
            throw new BadCommandInvocationException(msg, help, e);
        }
        catch (IllegalStateException e)
        {
            // IRC command called at wrong moment/state.
            final String help = cmd.help();
            throw new BadCommandInvocationException(msg, help, e);
        }
        catch (RuntimeException e)
        {
            LOGGER.error(
                "Failed to execute command '" + command + "': "
                    + e.getMessage(), e);
        }
    }

    /**
     * Send an IRC message.
     *
     * @param chatroom The chat room to send the message to.
     * @param message The message to send.
     * @throws OperationFailedException OperationFailedException is thrown when
     *             message is too large to be processed by IRC server.
     */
    public void message(final ChatRoomIrcImpl chatroom, final String message)
        throws OperationFailedException
    {
        if (!this.connectionState.isConnected())
        {
            throw new IllegalStateException("Not connected to an IRC server.");
        }
        final String target = chatroom.getIdentifier();
        // message format as forwarded by IRC server to clients:
        // :<user> PRIVMSG <nick> :<message>
        final int maxMsgSize = calculateMaximumMessageSize(0, target);
        if (maxMsgSize < message.length())
        {
            LOGGER.warn("Message for " + target
                + " is too large. At best you can send the message up to: "
                + message.substring(0, maxMsgSize));
            throw new OperationFailedException(
                "Message is too large for this IRC server.",
                OperationFailedException.ILLEGAL_ARGUMENT);
        }
        try
        {
            this.irc.message(target, message);
            LOGGER.trace("Message delivered to server successfully.");
        }
        catch (RuntimeException e)
        {
            LOGGER.trace("Failed to deliver message: " + e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Send an IRC message.
     *
     * @param contact The contact to send the message to.
     * @param message The message to send.
     * @throws OperationFailedException OperationFailedException is thrown when
     *             message is too large to be processed by IRC server.
     */
    public void message(final Contact contact, final Message message)
        throws OperationFailedException
    {
        if (!this.connectionState.isConnected())
        {
            throw new IllegalStateException("Not connected to an IRC server.");
        }
        final String target = contact.getAddress();
        // message format as forwarded by IRC server to clients:
        // :<user> PRIVMSG <nick> :<message>
        final int maxMsgSize = calculateMaximumMessageSize(0, target);
        if (maxMsgSize < message.getContent().length())
        {
            // Message is definitely too large to be sent to a standard IRC
            // network. Sending is not attempted, since we would send a partial
            // message, even though the user is not informed of this.
            LOGGER.warn("Message for " + target
                + " is too large. At best you can send the message up to: "
                + message.getContent().substring(0, maxMsgSize));
            throw new OperationFailedException(
                "Message is too large for this IRC server.",
                OperationFailedException.ILLEGAL_ARGUMENT);
        }
        try
        {
            this.irc.message(target, message.getContent());
            LOGGER.trace("Message delivered to server successfully.");
        }
        catch (RuntimeException e)
        {
            LOGGER.trace("Failed to deliver message: " + e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Calculate maximum message size that can be transmitted.
     *
     * @param contact receiving contact
     * @return returns maximum message size
     */
    public int calculateMaximumMessageSize(final Contact contact)
    {
        return calculateMaximumMessageSize(SAFETY_NET, contact.getAddress());
    }

    /**
     * Calculate maximum message size that can be transmitted.
     *
     * @param room receiving chat room
     * @return Returns maximum message size.
     */
    public int calculateMaximumMessageSize(final ChatRoomIrcImpl room)
    {
        return calculateMaximumMessageSize(SAFETY_NET, room.getIdentifier());
    }

    /**
     * Calculate maximum message size by given identifier and based on local
     * user's own identity.
     *
     * @param safety Number of chars extra slack as safety measure for resulting
     *            value. (This may just save you in case of off-by-one errors by
     *            an IRC server.)
     * @param identifier the identifier
     * @return Returns number of chars available for message.
     */
    private int calculateMaximumMessageSize(final int safety,
        final String identifier)
    {
        final StringBuilder builder = new StringBuilder(":");
        builder.append(this.identity.getIdentityString());
        builder.append(" PRIVMSG ");
        builder.append(identifier);
        builder.append(" :");
        return IRC_PROTOCOL_MAX_MESSAGE_SIZE - safety - builder.length();
    }

    /**
     * Message manager listener for handling message related events.
     *
     * @author Danny van Heumen
     */
    private final class MessageManagerListener
        extends AbstractIrcMessageListener
    {
        /**
         * IRC reply containing away message.
         */
        private static final int RPL_AWAY = 301;

        /**
         * IRC error code for case of non-existing nick or channel name.
         */
        private static final int ERR_NO_SUCH_NICK_CHANNEL =
            IRCServerNumerics.NO_SUCH_NICK_CHANNEL;

        /**
         * Constructor.
         */
        public MessageManagerListener()
        {
            super(MessageManager.this.irc, MessageManager.this.connectionState);
        }

        /**
         * Message-related server numeric messages.
         *
         * @param msg the message
         */
        @Override
        public void onServerNumericMessage(final ServerNumericMessage msg)
        {
            switch (msg.getNumericCode())
            {
            case ERR_NO_SUCH_NICK_CHANNEL:
                if (LOGGER.isTraceEnabled())
                {
                    LOGGER.trace("Message did not get delivered: "
                        + msg.asRaw());
                }
                final String msgText = msg.getText();
                final int endOfTargetIndex = msgText.indexOf(' ');
                if (endOfTargetIndex == -1)
                {
                    LOGGER.trace("Expected target nick in error message, but "
                        + "it cannot be found. Stop parsing.");
                    break;
                }
                final String targetNick =
                    msgText.substring(0, endOfTargetIndex);
                // Send blank text string as the message, since we don't know
                // what the actual message was. (We cannot reliably relate the
                // NOSUCHNICK reply to the exact message that caused the error.)
                MessageIrcImpl message =
                    new MessageIrcImpl(
                        "",
                        OperationSetBasicInstantMessaging.HTML_MIME_TYPE,
                        OperationSetBasicInstantMessaging.DEFAULT_MIME_ENCODING,
                        null);
                final Contact to =
                    MessageManager.this.provider.getPersistentPresence()
                        .findOrCreateContactByID(targetNick);
                MessageManager.this.provider
                    .getBasicInstantMessaging()
                    .fireMessageDeliveryFailed(
                        message,
                        to,
                        MessageDeliveryFailedEvent
                            .OFFLINE_MESSAGES_NOT_SUPPORTED);
                break;

            case RPL_AWAY:
                final String rawAwayText = msg.getText();
                final String awayUserNick =
                    rawAwayText.substring(0, rawAwayText.indexOf(' '));
                final String awayText =
                    rawAwayText.substring(rawAwayText.indexOf(' ') + 2);
                final MessageIrcImpl awayMessage =
                    MessageIrcImpl.newAwayMessageFromIRC(awayText);
                final Contact awayUser =
                    MessageManager.this.provider.getPersistentPresence()
                        .findOrCreateContactByID(awayUserNick);
                MessageManager.this.provider.getBasicInstantMessaging()
                    .fireMessageReceived(awayMessage, awayUser);
                break;

            default:
                break;
            }
        }

        /**
         * Upon receiving a private message from a user, deliver that to an
         * instant messaging contact and create one if it does not exist. We can
         * ignore normal chat rooms, since they each have their own
         * ChatRoomListener for managing chat room operations.
         *
         * @param msg the private message
         */
        @Override
        public void onUserPrivMessage(final UserPrivMsg msg)
        {
            final String user = msg.getSource().getNick();
            final MessageIrcImpl message =
                MessageIrcImpl.newMessageFromIRC(msg.getText());
            final Contact from =
                MessageManager.this.provider.getPersistentPresence()
                    .findOrCreateContactByID(user);
            try
            {
                MessageManager.this.provider.getBasicInstantMessaging()
                    .fireMessageReceived(message, from);
            }
            catch (RuntimeException e)
            {
                LOGGER.error(
                    "Error occurred while delivering private message from user"
                        + " '" + user + "': " + msg.getText(), e);
            }
        }

        /**
         * Upon receiving a user notice message from a user, deliver that to an
         * instant messaging contact.
         *
         * @param msg user notice message
         */
        @Override
        public void onUserNotice(final UserNotice msg)
        {
            final String user = msg.getSource().getNick();
            final Contact from =
                MessageManager.this.provider.getPersistentPresence()
                    .findOrCreateContactByID(user);
            final MessageIrcImpl message =
                MessageIrcImpl.newNoticeFromIRC(from, msg.getText());
            MessageManager.this.provider.getBasicInstantMessaging()
                .fireMessageReceived(message, from);
        }

        /**
         * Upon receiving a user action message from a user, deliver that to an
         * instant messaging contact.
         *
         * @param msg user action message
         */
        @Override
        public void onUserAction(final UserActionMsg msg)
        {
            final String user = msg.getSource().getNick();
            final Contact from =
                MessageManager.this.provider.getPersistentPresence()
                    .findContactByID(user);
            final MessageIrcImpl message =
                MessageIrcImpl.newActionFromIRC(msg.getText());
            MessageManager.this.provider.getBasicInstantMessaging()
                .fireMessageReceived(message, from);
        }
    }
}
