/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.irc;

import java.io.*;
import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

import com.ircclouds.irc.api.*;
import com.ircclouds.irc.api.domain.*;
import com.ircclouds.irc.api.domain.messages.*;
import com.ircclouds.irc.api.listeners.*;
import com.ircclouds.irc.api.state.*;

/**
 * IRC Connection.
 *
 * TODO Do we need to cancel any join channel operations still in progress?
 *
 * TODO Separate functionality into separate managers:
 * 1. Channel manager
 *
 * Common IRC network facilities:
 * 1. NickServ - nick related services
 * 2. ChanServ - channel related services
 * 3. MemoServ - message relaying services
 *
 * @author Danny van Heumen
 */
public class IrcConnection
{
    /**
     * TODO In the far far future ...
     *
     * <p>
     * Some of the less pressing features that may one day be useful ...
     * </p>
     *
     * <pre>
     * - Handle 404 ERR_CANNOTSENDTOCHAN in case of +n channel mode and not
     *   joined to the channel where you send a message to.
     * </pre>
     */

    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(IrcConnection.class);

    /**
     * Maximum message size for IRC messages given the spec specifies a buffer
     * of 512 bytes. The command ending (CRLF) takes up 2 bytes.
     */
    private static final int IRC_PROTOCOL_MAXIMUM_MESSAGE_SIZE = 510;

    /**
     * Set of characters with special meanings for IRC, such as: ',' used as
     * separator of list of items (channels, nicks, etc.), ' ' (space) separator
     * of command parameters, etc.
     */
    public static final Set<Character> SPECIAL_CHARACTERS;

    /**
     * Initialize set of special characters.
     */
    static {
        HashSet<Character> specials = new HashSet<Character>();
        specials.add('\0');
        specials.add('\n');
        specials.add('\r');
        specials.add(' ');
        specials.add(',');
        SPECIAL_CHARACTERS = Collections.unmodifiableSet(specials);
    }

    /**
     * Instance of the protocol provider service.
     */
    private final ProtocolProviderServiceIrcImpl provider;

    /**
     * Instance of IRC Api.
     */
    private final IRCApi irc;

    /**
     * Connection state of a successful IRC connection.
     */
    private final IIRCState connectionState;

    /**
     * Manager component that manages current IRC presence.
     */
    private final PresenceManager presence;

    /**
     * Manager component for server channel listing.
     */
    private final ServerChannelLister channelLister;

    /**
     * The local user's identity as it will be used in server-client
     * communication for sent messages.
     */
    private final IdentityManager identity;

    /**
     * The channel manager instance.
     */
    private final ChannelManager channel;

    /**
     * Constructor.
     *
     * @param provider ProtocolProviderService instance
     * @param params connection parameters
     * @param irc IRC api instance
     * @throws Exception Throws IOException in case of connection problems.
     */
    public IrcConnection(final ProtocolProviderServiceIrcImpl provider,
        final IServerParameters params, final IRCApi irc)
        throws Exception
    {
        if (provider == null)
        {
            throw new IllegalArgumentException("provider cannot be null");
        }
        this.provider = provider;
        if (irc == null)
        {
            throw new IllegalArgumentException("irc instance cannot be null");
        }
        // Install a listener for everything that is not directly related to a
        // specific chat room or operation.
        irc.addListener(new ServerListener(irc));
        this.irc = irc;
        this.connectionState = connectSynchronized(this.provider, params, irc);

        // instantiate presence manager for the connection
        this.presence =
            new PresenceManager(this.irc, this.connectionState,
                this.provider.getPersistentPresence());

        // instantiate server channel lister
        this.channelLister =
            new ServerChannelLister(this.irc, this.connectionState);

        // instantiate identity manager for the connection
        this.identity = new IdentityManager(this.irc, this.connectionState);

        // instantiate channel manager for the connection
        this.channel =
            new ChannelManager(this.irc, this.connectionState, this.provider,
                this.identity);

        // TODO Read IRC network capabilities based on RPL_ISUPPORT
        // (005) replies if available. This information should be
        // available in irc-api if possible.
    }

    /**
     * Perform synchronized connect operation.
     *
     * @param provider Parent protocol provider
     * @param params Server connection parameters
     * @param irc IRC Api instance
     * @throws Exception exception thrown when connect fails
     */
    private static IIRCState connectSynchronized(
        final ProtocolProviderServiceIrcImpl provider,
        final IServerParameters params, final IRCApi irc) throws Exception
    {
        final Result<IIRCState, Exception> result =
            new Result<IIRCState, Exception>();
        synchronized (result)
        {
            // start connecting to the specified server ...
            irc.connect(params, new Callback<IIRCState>()
            {

                @Override
                public void onSuccess(final IIRCState state)
                {
                    synchronized (result)
                    {
                        LOGGER.trace("IRC connected successfully!");
                        result.setDone(state);
                        result.notifyAll();
                    }
                }

                @Override
                public void onFailure(final Exception e)
                {
                    synchronized (result)
                    {
                        LOGGER.trace("IRC connection FAILED!", e);
                        result.setDone(e);
                        result.notifyAll();
                    }
                }
            });

            provider.setCurrentRegistrationState(RegistrationState.REGISTERING);

            while (!result.isDone())
            {
                LOGGER.trace("Waiting for the connection to be "
                    + "established ...");
                result.wait();
            }
        }

        // TODO Implement connection timeout and a way to recognize that
        // the timeout occurred.

        final Exception e = result.getException();
        if (e != null)
        {
            throw new IOException(e);
        }

        final IIRCState state = result.getValue();
        if (state == null)
        {
            throw new IOException(
                "Failed to connect to IRC server: connection state is null");
        }

        return state;
    }

    /**
     * Check whether or not a connection is established.
     *
     * @return true if connected, false otherwise.
     */
    public boolean isConnected()
    {
        return this.connectionState != null
            && this.connectionState.isConnected();
    }

    /**
     * Check whether the connection is a secure connection (TLS).
     *
     * @return true if connection is secure, false otherwise.
     */
    public boolean isSecureConnection()
    {
        return isConnected() && this.connectionState.getServer().isSSL();
    }

    /**
     * Disconnect.
     */
    void disconnect()
    {
        try
        {
            synchronized (this.irc)
            {
                this.irc.disconnect();
            }
        }
        catch (RuntimeException e)
        {
            // Disconnect might throw ChannelClosedException. Shouldn't be a
            // problem, but for now lets log it just to be sure.
            LOGGER.debug("exception occurred while disconnecting", e);
        }
    }

    /**
     * Get the presence manager.
     *
     * @return returns the presence manager instance
     */
    public PresenceManager getPresenceManager()
    {
        return this.presence;
    }

    /**
     * Get the channel lister that facilitates server channel queries.
     *
     * @return returns the channel lister instance
     */
    public ServerChannelLister getServerChannelLister()
    {
        return this.channelLister;
    }

    /**
     * Get the identity manager instance.
     *
     * @return returns the identity manager instance
     */
    public IdentityManager getIdentityManager()
    {
        return this.identity;
    }

    /**
     * Get the channel manager instance.
     *
     * @return returns the channel manager instance
     */
    public ChannelManager getChannelManager()
    {
        return this.channel;
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
        if (!isConnected())
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
            synchronized (this.irc)
            {
                this.irc.message(target, command);
            }
        }
        else if (msg.startsWith("/me "))
        {
            final String command = message.substring(4);
            synchronized (this.irc)
            {
                this.irc.act(source, command);
            }
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
                synchronized (this.irc)
                {
                    this.irc.joinChannel(channel, password);
                }
            }
        }
        else
        {
            synchronized (this.irc)
            {
                this.irc.message(source, message);
            }
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
        if (!isConnected())
        {
            throw new IllegalStateException("Not connected to an IRC server.");
        }
        final String target = chatroom.getIdentifier();
        synchronized (this.irc)
        {
            this.irc.message(target, message);
        }
    }

    /**
     * Send an IRC message.
     *
     * @param contact The contact to send the message to.
     * @param message The message to send.
     */
    public void message(final Contact contact, final Message message)
    {
        if (!isConnected())
        {
            throw new IllegalStateException("Not connected to an IRC server.");
        }
        final String target = contact.getAddress();
        try
        {
            synchronized (this.irc)
            {
                this.irc.message(target, message.getContent());
            }
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
     * A listener for server-level messages (any messages that are related to
     * the server, the connection, that are not related to any chatroom in
     * particular) or that are personal message from user to local user.
     */
    private final class ServerListener
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
         * IRCApi instance.
         */
        private final IRCApi irc;

        /**
         * Constructor for Server Listener.
         *
         * @param irc IRCApi instance
         */
        private ServerListener(final IRCApi irc)
        {
            if (irc == null)
            {
                throw new IllegalArgumentException(
                    "irc instance cannot be null");
            }
            this.irc = irc;
        }

        /**
         * Print out server notices for debugging purposes and for simply
         * keeping track of the connections.
         *
         * @param msg the server notice
         */
        @Override
        public void onServerNotice(final ServerNotice msg)
        {
            LOGGER.debug("NOTICE: " + msg.getText());
        }

        /**
         * Print out server numeric messages for debugging purposes and for
         * simply keeping track of the connection.
         *
         * @param msg the numeric message
         */
        @Override
        public void onServerNumericMessage(final ServerNumericMessage msg)
        {
            if (LOGGER.isDebugEnabled())
            {
                LOGGER.debug("NUM MSG: " + msg.getNumericCode() + ": "
                    + msg.getText());
            }

            final Integer code = msg.getNumericCode();
            if (code == null)
            {
                LOGGER.debug("No 'code' in numeric message event.");
                return;
            }

            if (!IrcConnection.this.isConnected())
            {
                // Skip message handling until we're officially connected.
                return;
            }

            switch (code.intValue())
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
                    IrcConnection.this.provider.getPersistentPresence()
                        .findOrCreateContactByID(targetNick);
                IrcConnection.this.provider
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
                    IrcConnection.this.provider.getPersistentPresence()
                        .findOrCreateContactByID(awayUserNick);
                IrcConnection.this.provider.getBasicInstantMessaging()
                    .fireMessageReceived(awayMessage, awayUser);
                break;

            default:
                if (LOGGER.isTraceEnabled())
                {
                    LOGGER.trace("This ServerNumericMessage (" + code
                        + ") will not be handled by the ServerListener.");
                }
                break;
            }
        }

        /**
         * Print out received errors for debugging purposes and may be for
         * expected errors that can be acted upon.
         *
         * @param msg the error message
         */
        @Override
        public void onError(final ErrorMessage msg)
        {
            if (LOGGER.isDebugEnabled())
            {
                LOGGER
                    .debug("ERROR: " + msg.getSource() + ": " + msg.getText());
            }
            if (IrcConnection.this.connectionState != null)
            {
                if (!IrcConnection.this.connectionState.isConnected())
                {
                    IrcConnection.this.provider
                        .setCurrentRegistrationState(
                            RegistrationState.CONNECTION_FAILED);
                }
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
                IrcConnection.this.provider.getPersistentPresence()
                    .findOrCreateContactByID(user);
            try
            {
                IrcConnection.this.provider.getBasicInstantMessaging()
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
                IrcConnection.this.provider.getPersistentPresence()
                    .findOrCreateContactByID(user);
            final MessageIrcImpl message =
                MessageIrcImpl.newNoticeFromIRC(from, msg.getText());
            IrcConnection.this.provider.getBasicInstantMessaging()
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
                IrcConnection.this.provider.getPersistentPresence()
                    .findContactByID(user);
            final MessageIrcImpl message =
                MessageIrcImpl.newActionFromIRC(from, msg.getText());
            IrcConnection.this.provider.getBasicInstantMessaging()
                .fireMessageReceived(message, from);
        }

        /**
         * User quit messages.
         *
         * User quit messages only need to be handled in case quitting users,
         * since that is the only clear signal of presence change we have.
         *
         * @param msg Quit message
         */
        @Override
        public void onUserQuit(final QuitMessage msg)
        {
            final String user = msg.getSource().getNick();
            if (user != null
                && user
                    .equals(IrcConnection.this.connectionState.getNickname()))
            {
                LOGGER.debug("Local user's QUIT message received: removing "
                    + "server listener.");
                this.irc.deleteListener(this);
                return;
            }
        }
    }
}
