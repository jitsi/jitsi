/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.irc;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

import com.ircclouds.irc.api.*;
import com.ircclouds.irc.api.domain.*;
import com.ircclouds.irc.api.domain.messages.*;
import com.ircclouds.irc.api.listeners.*;
import com.ircclouds.irc.api.state.*;

/**
 * Manager for message-related operations.
 *
 * FIXME MessageManager may miss some quick events such as NickServ messages
 * right after establishing a connection.
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
     * Maximum message size for IRC messages given the spec specifies a buffer
     * of 512 bytes. The command ending (CRLF) takes up 2 bytes.
     */
    private static final int IRC_PROTOCOL_MAXIMUM_MESSAGE_SIZE = 510;

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
     * Identity manager.
     */
    private final IdentityManager identity;

    /**
     * Constructor.
     *
     * @param irc thread-safe IRCApi instance
     * @param connectionState the connection state
     * @param provider the provider instance
     * @param identity the identity manager
     */
    public MessageManager(final IRCApi irc, final IIRCState connectionState,
        final ProtocolProviderServiceIrcImpl provider,
        final IdentityManager identity)
    {
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
    }

    /**
     * Send a command to the IRC server.
     *
     * @param chatroom the chat room
     * @param message the command message
     */
    public void command(final ChatRoomIrcImpl chatroom, final String message)
    {
        this.command(chatroom.getIdentifier(), message);
    }

    /**
     * Send a command to the IRC server.
     *
     * @param contact the chat room
     * @param message the command message
     */
    public void command(final Contact contact, final MessageIrcImpl message)
    {
        this.command(contact.getAddress(), message.getContent());
    }

    /**
     * Implementation of some commands. If the command is not recognized or
     * implemented, it will be sent as if it were a normal message.
     *
     * TODO Eventually replace this with a factory such that we can easily
     * extend with new commands.
     *
     * @param source Source contact or chat room from which the message is sent.
     * @param message Command message that is sent.
     */
    private void command(final String source, final String message)
    {
        if (!this.connectionState.isConnected())
        {
            throw new IllegalStateException("Not connected to IRC server.");
        }
        final String msg = message.toLowerCase();
        if (msg.startsWith("/msg "))
        {
            final String part = message.substring(5);
            int endOfNick = part.indexOf(' ');
            if (endOfNick == -1)
            {
                throw new IllegalArgumentException("Invalid private message "
                    + "format. Message was not sent.");
            }
            final String target = part.substring(0, endOfNick);
            final String command = part.substring(endOfNick + 1);
            this.irc.message(target, command);
        }
        else if (msg.startsWith("/me "))
        {
            final String command = message.substring(4);
            this.irc.act(source, command);
        }
        else if (msg.startsWith("/join "))
        {
            final String part = message.substring(6);
            final String channel;
            final String password;
            int indexOfSep = part.indexOf(' ');
            if (indexOfSep == -1)
            {
                channel = part;
                password = "";
            }
            else
            {
                channel = part.substring(0, indexOfSep);
                password = part.substring(indexOfSep + 1);
            }
            if (channel.matches("[^,\\n\\r\\s\\a]+"))
            {
                this.irc.joinChannel(channel, password);
            }
        }
        // TODO add /nick command for nick changing
        else
        {
            // FIXME don't send as normal message just in case it contains
            // valuable/vulnerable information (or if simply wasn't intended)
            this.irc.message(source, message);
        }
    }

    /**
     * Send an IRC message.
     *
     * @param chatroom The chat room to send the message to.
     * @param message The message to send.
     */
    public void message(final ChatRoomIrcImpl chatroom, final String message)
    {
        if (!this.connectionState.isConnected())
        {
            throw new IllegalStateException("Not connected to an IRC server.");
        }
        final String target = chatroom.getIdentifier();
        this.irc.message(target, message);
    }

    /**
     * Send an IRC message.
     *
     * @param contact The contact to send the message to.
     * @param message The message to send.
     */
    public void message(final Contact contact, final Message message)
    {
        if (!this.connectionState.isConnected())
        {
            throw new IllegalStateException("Not connected to an IRC server.");
        }
        final String target = contact.getAddress();
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
        final StringBuilder builder = new StringBuilder(":");
        builder.append(this.identity.getIdentityString());
        builder.append(" PRIVMSG ");
        builder.append(contact.getAddress());
        builder.append(" :");
        return IRC_PROTOCOL_MAXIMUM_MESSAGE_SIZE - builder.length();
    }

    /**
     * Message manager listener for handling message related events.
     *
     * @author Danny van Heumen
     */
    private final class MessageManagerListener
        extends VariousMessageListenerAdapter
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
         * On User Quit event.
         *
         * @param msg user quit message
         */
        @Override
        public void onUserQuit(final QuitMessage msg)
        {
            final String user = msg.getSource().getNick();
            if (MessageManager.this.connectionState.getNickname().equals(user))
            {
                LOGGER.debug("Local user QUIT message received: removing "
                    + "message manager listener.");
                MessageManager.this.irc.deleteListener(this);
            }
        }

        @Override
        public void onServerNumericMessage(final ServerNumericMessage msg)
        {
            switch (msg.getNumericCode().intValue())
            {
            case ERR_NO_SUCH_NICK_CHANNEL:
                // TODO Check if target is Contact, then update contact presence
                // status to off-line since the nick apparently does not exist
                // anymore.
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
                // TODO remove once this is stable. Don't want to lose message
                // when an accidental error occurs.
                // It is likely that errors occurred because of some issues with
                // MetaContactGroup for NonPersistent group, since this is an
                // outstanding error.
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
                MessageIrcImpl.newActionFromIRC(from, msg.getText());
            MessageManager.this.provider.getBasicInstantMessaging()
                .fireMessageReceived(message, from);
        }
    }
}
