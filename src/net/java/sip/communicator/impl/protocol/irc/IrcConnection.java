/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.irc;

import java.io.*;
import java.util.*;

import net.java.sip.communicator.impl.protocol.irc.ModeParser.ModeEntry;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

import com.ircclouds.irc.api.*;
import com.ircclouds.irc.api.domain.*;
import com.ircclouds.irc.api.domain.messages.*;
import com.ircclouds.irc.api.domain.messages.interfaces.*;
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
     * Container for joined channels.
     *
     * There are two different cases:
     *
     * <pre>
     * - null value: joining is initiated but still in progress.
     * - non-null value: joining is finished, chat room instance is available.
     * </pre>
     */
    private final Map<String, ChatRoomIrcImpl> joined = Collections
        .synchronizedMap(new HashMap<String, ChatRoomIrcImpl>());

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
     * Get a set of channel type indicators.
     *
     * @return returns set of channel type indicators.
     */
    public Set<Character> getChannelTypes()
    {
        if (!isConnected())
        {
            throw new IllegalStateException("not connected to IRC server");
        }
        return this.connectionState.getServerOptions().getChanTypes();
    }

    /**
     * Get the nick name of the user.
     *
     * @return Returns either the acting nick if a connection is established or
     *         the configured nick.
     */
    public String getNick()
    {
        return this.connectionState.getNickname();
    }

    /**
     * Set the subject of the specified chat room.
     *
     * @param chatroom The chat room for which to set the subject.
     * @param subject The subject.
     */
    public void setSubject(final ChatRoomIrcImpl chatroom, final String subject)
    {
        if (!isConnected())
        {
            throw new IllegalStateException(
                "Please connect to an IRC server first.");
        }
        if (chatroom == null)
        {
            throw new IllegalArgumentException("Cannot have a null chatroom");
        }
        LOGGER.trace("Setting chat room topic to '" + subject + "'");
        synchronized (this.irc)
        {
            this.irc.changeTopic(chatroom.getIdentifier(), subject == null ? ""
                : subject);
        }
    }

    /**
     * Check whether the user has joined a particular chat room.
     *
     * @param chatroom Chat room to check for.
     * @return Returns true in case the user is already joined, or false if the
     *         user has not joined.
     */
    public boolean isJoined(final ChatRoomIrcImpl chatroom)
    {
        return this.joined.get(chatroom.getIdentifier()) != null;
    }

    /**
     * Join a particular chat room.
     *
     * @param chatroom Chat room to join.
     * @throws OperationFailedException failed to join the chat room
     */
    public void join(final ChatRoomIrcImpl chatroom)
        throws OperationFailedException
    {
        join(chatroom, "");
    }

    /**
     * Join a particular chat room.
     *
     * Issue a join channel IRC operation and wait for the join operation to
     * complete (either successfully or failing).
     *
     * @param chatroom The chatroom to join.
     * @param password Optionally, a password that may be required for some
     *            channels.
     * @throws OperationFailedException failed to join the chat room
     */
    public void join(final ChatRoomIrcImpl chatroom, final String password)
        throws OperationFailedException
    {
        if (!isConnected())
        {
            throw new IllegalStateException(
                "Please connect to an IRC server first");
        }
        if (chatroom == null)
        {
            throw new IllegalArgumentException("chatroom cannot be null");
        }
        if (password == null)
        {
            throw new IllegalArgumentException("password cannot be null");
        }

        final String chatRoomId = chatroom.getIdentifier();
        if (this.joined.containsKey(chatRoomId))
        {
            // If we already joined this particular chatroom, no further action
            // is required.
            return;
        }

        LOGGER.trace("Start joining channel " + chatRoomId);
        final Result<Object, Exception> joinSignal =
            new Result<Object, Exception>();
        synchronized (joinSignal)
        {
            LOGGER.trace("Issue join channel command to IRC library and wait "
                + "for join operation to complete (un)successfully.");

            this.joined.put(chatRoomId, null);
            synchronized (this.irc)
            {
                // TODO Refactor this ridiculous nesting of functions and
                // classes.
                this.irc.joinChannel(chatRoomId, password,
                    new Callback<IRCChannel>()
                    {

                        @Override
                        public void onSuccess(final IRCChannel channel)
                        {
                            if (LOGGER.isTraceEnabled())
                            {
                                LOGGER
                                    .trace("Started callback for successful "
                                        + "join of channel '"
                                        + chatroom.getIdentifier() + "'.");
                            }
                            boolean isRequestedChatRoom =
                                channel.getName().equalsIgnoreCase(chatRoomId);
                            synchronized (joinSignal)
                            {
                                if (!isRequestedChatRoom)
                                {
                                    // We joined another chat room than the one
                                    // we requested initially.
                                    if (LOGGER.isTraceEnabled())
                                    {
                                        LOGGER.trace("Callback for successful "
                                            + "join finished prematurely "
                                            + "since we got forwarded from "
                                            + "'"
                                            + chatRoomId
                                            + "' to '"
                                            + channel.getName()
                                            + "'. Joining of forwarded channel "
                                            + "gets handled by Server Listener "
                                            + "since that channel was not "
                                            + "announced.");
                                    }
                                    // Remove original chat room id from
                                    // joined-list since we aren't actually
                                    // attempting to join this room anymore.
                                    IrcConnection.this.joined
                                        .remove(chatRoomId);
                                    IrcConnection.this.provider
                                        .getMUC()
                                        .fireLocalUserPresenceEvent(
                                            chatroom,
                                            LocalUserChatRoomPresenceChangeEvent
                                                .LOCAL_USER_JOIN_FAILED,
                                            "We got forwarded to channel '"
                                                + channel.getName() + "'.");
                                    // Notify waiting threads of finished
                                    // execution.
                                    joinSignal.setDone();
                                    joinSignal.notifyAll();
                                    // The channel that we were forwarded to
                                    // will be handled by the Server Listener,
                                    // since the channel join was unannounced,
                                    // and we are done here.
                                    return;
                                }

                                try
                                {
                                    IrcConnection.this.joined.put(chatRoomId,
                                        chatroom);
                                    IrcConnection.this.irc
                                        .addListener(new ChatRoomListener(
                                            IrcConnection.this.irc, chatroom));
                                    prepareChatRoom(chatroom, channel);
                                }
                                finally
                                {
                                    // In any case, issue the local user
                                    // presence, since the irc library notified
                                    // us of a successful join. We should wait
                                    // as long as possible though. First we need
                                    // to fill the list of chat room members and
                                    // other chat room properties.
                                    IrcConnection.this.provider
                                        .getMUC()
                                        .fireLocalUserPresenceEvent(
                                            chatroom,
                                            LocalUserChatRoomPresenceChangeEvent
                                                .LOCAL_USER_JOINED,
                                            null);
                                    if (LOGGER.isTraceEnabled())
                                    {
                                        LOGGER
                                            .trace("Finished successful join "
                                                + "callback for channel '"
                                                + chatRoomId
                                                + "'. Waking up original "
                                                + "thread.");
                                    }
                                    // Notify waiting threads of finished
                                    // execution.
                                    joinSignal.setDone();
                                    joinSignal.notifyAll();
                                }
                            }
                        }

                        @Override
                        public void onFailure(final Exception e)
                        {
                            LOGGER
                                .trace("Started callback for failed attempt to "
                                    + "join channel '" + chatRoomId + "'.");
                            synchronized (joinSignal)
                            {
                                try
                                {
                                    IrcConnection.this.joined
                                        .remove(chatRoomId);
                                    IrcConnection.this.provider
                                        .getMUC()
                                        .fireLocalUserPresenceEvent(
                                            chatroom,
                                            LocalUserChatRoomPresenceChangeEvent
                                                .LOCAL_USER_JOIN_FAILED,
                                            e.getMessage());
                                }
                                finally
                                {
                                    if (LOGGER.isTraceEnabled())
                                    {
                                        LOGGER
                                            .trace("Finished callback for "
                                                + "failed attempt to join "
                                                + "channel '"
                                                + chatRoomId
                                                + "'. Waking up original "
                                                + "thread.");
                                    }
                                    // Notify waiting threads of finished
                                    // execution
                                    joinSignal.setDone(e);
                                    joinSignal.notifyAll();
                                }
                            }
                        }
                    });
            }

            try
            {
                while (!joinSignal.isDone())
                {
                    LOGGER.trace("Waiting for channel join message ...");
                    // Wait until async channel join operation has finished.
                    joinSignal.wait();
                }

                LOGGER
                    .trace("Finished waiting for join operation for channel '"
                        + chatroom.getIdentifier() + "' to complete.");
                // TODO How to handle 480 (+j): Channel throttle exceeded?
            }
            catch (InterruptedException e)
            {
                LOGGER.error("Wait for join operation was interrupted.", e);
                throw new OperationFailedException(e.getMessage(),
                    OperationFailedException.INTERNAL_ERROR, e);
            }
        }
    }

    /**
     * Part from a joined chat room.
     *
     * @param chatroom The chat room to part from.
     */
    public void leave(final ChatRoomIrcImpl chatroom)
    {
        LOGGER.trace("Leaving chat room '" + chatroom.getIdentifier() + "'.");
        leave(chatroom.getIdentifier());
    }

    /**
     * Part from a joined chat room.
     *
     * @param chatRoomName The chat room to part from.
     */
    private void leave(final String chatRoomName)
    {
        if (!isConnected())
        {
            throw new IllegalStateException("Not connected to an IRC server.");
        }

        try
        {
            synchronized (this.irc)
            {
                this.irc.leaveChannel(chatRoomName);
            }
        }
        catch (ApiException e)
        {
            LOGGER.warn("exception occurred while leaving channel", e);
        }
    }

    /**
     * Ban chat room member.
     *
     * @param chatroom chat room to ban from
     * @param member member to ban
     * @param reason reason for banning
     * @throws OperationFailedException throws operation failed in case of
     *             trouble.
     */
    public void banParticipant(final ChatRoomIrcImpl chatroom,
        final ChatRoomMember member, final String reason)
        throws OperationFailedException
    {
        // TODO Implement banParticipant.
        throw new OperationFailedException("Not implemented yet.",
            OperationFailedException.NOT_SUPPORTED_OPERATION);
    }

    /**
     * Kick channel member.
     *
     * @param chatroom channel to kick from
     * @param member member to kick
     * @param reason kick message to deliver
     */
    public void kickParticipant(final ChatRoomIrcImpl chatroom,
        final ChatRoomMember member, final String reason)
    {
        if (!isConnected())
        {
            return;
        }
        synchronized (this.irc)
        {
            this.irc.kick(chatroom.getIdentifier(), member.getContactAddress(),
                reason);
        }
    }

    /**
     * Issue invite command to IRC server.
     *
     * @param memberId member to invite
     * @param chatroom channel to invite to
     */
    public void invite(final String memberId, final ChatRoomIrcImpl chatroom)
    {
        if (!isConnected())
        {
            throw new IllegalStateException("Not connected to an IRC server.");
        }
        synchronized (this.irc)
        {
            this.irc.rawMessage("INVITE " + memberId + " "
                + chatroom.getIdentifier());
        }
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
     * Grant user permissions to specified user.
     *
     * @param chatRoom chat room to grant permissions for
     * @param userAddress user to grant permissions to
     * @param mode mode to grant
     */
    public void grant(final ChatRoomIrcImpl chatRoom, final String userAddress,
        final Mode mode)
    {
        if (!isConnected())
        {
            throw new IllegalStateException("Not connected to an IRC server.");
        }
        if (mode.getRole() == null)
        {
            throw new IllegalArgumentException(
                "This mode does not modify user permissions.");
        }
        synchronized (this.irc)
        {
            this.irc.changeMode(chatRoom.getIdentifier() + " +"
                + mode.getSymbol() + " " + userAddress);
        }
    }

    /**
     * Revoke user permissions of chat room for user.
     *
     * @param chatRoom chat room
     * @param userAddress user
     * @param mode mode
     */
    public void revoke(final ChatRoomIrcImpl chatRoom,
        final String userAddress, final Mode mode)
    {
        if (!isConnected())
        {
            throw new IllegalStateException("Not connected to an IRC server.");
        }
        if (mode.getRole() == null)
        {
            throw new IllegalArgumentException(
                "This mode does not modify user permissions.");
        }
        synchronized (this.irc)
        {
            this.irc.changeMode(chatRoom.getIdentifier() + " -"
                + mode.getSymbol() + " " + userAddress);
        }
    }

    /**
     * Prepare a chat room for initial opening.
     *
     * @param channel The IRC channel which is the source of data.
     * @param chatRoom The chatroom to prepare.
     */
    private void prepareChatRoom(final ChatRoomIrcImpl chatRoom,
        final IRCChannel channel)
    {
        final IRCTopic topic = channel.getTopic();
        chatRoom.updateSubject(topic.getValue());

        for (IRCUser user : channel.getUsers())
        {
            ChatRoomMemberIrcImpl member =
                new ChatRoomMemberIrcImpl(this.provider, chatRoom,
                    user.getNick(), ChatRoomMemberRole.SILENT_MEMBER);
            ChatRoomMemberRole role;
            for (IRCUserStatus status : channel.getStatusesForUser(user))
            {
                role = convertMemberMode(status.getChanModeType().charValue());
                member.addRole(role);
            }
            chatRoom.addChatRoomMember(member.getContactAddress(), member);
            if (this.getNick().equals(user.getNick()))
            {
                chatRoom.setLocalUser(member);
                if (member.getRole() != ChatRoomMemberRole.SILENT_MEMBER)
                {
                    ChatRoomLocalUserRoleChangeEvent event =
                        new ChatRoomLocalUserRoleChangeEvent(chatRoom,
                            ChatRoomMemberRole.SILENT_MEMBER, member.getRole(),
                            true);
                    chatRoom.fireLocalUserRoleChangedEvent(event);
                }
            }
        }
    }

    /**
     * Convert a member mode character to a ChatRoomMemberRole instance.
     *
     * @param modeSymbol The member mode character.
     * @return Return the instance of ChatRoomMemberRole corresponding to the
     *         member mode character.
     */
    private static ChatRoomMemberRole convertMemberMode(final char modeSymbol)
    {
        return Mode.bySymbol(modeSymbol).getRole();
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
         * IRC reply code for end of list.
         */
        private static final int RPL_LISTEND =
            IRCServerNumerics.CHANNEL_NICKS_END_OF_LIST;

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
            case RPL_LISTEND:
                // CHANNEL_NICKS_END_OF_LIST indicates the end of a nick list as
                // you will receive when joining a channel. This is used as the
                // indicator that we have joined a channel. Now we have to
                // determine whether or not we already know about this
                // particular join attempt. If not, we continue to inform Jitsi
                // and to create a listener for this new chat room.
                final String text = msg.getText();
                final String channelName = text.substring(0, text.indexOf(' '));
                final ChatRoomIrcImpl chatRoom;
                final IRCChannel channel;
                synchronized (IrcConnection.this.joined)
                {
                    // Synchronize the section that checks then adds a chat
                    // room. This way we can be sure that there are no 2
                    // simultaneous creation events.
                    if (IrcConnection.this.joined.containsKey(channelName))
                    {
                        LOGGER.trace("Chat room '" + channelName
                            + "' join event was announced or already "
                            + "finished. Stop handling this event.");
                        break;
                    }
                    // We aren't currently attempting to join, so this join is
                    // unannounced.
                    LOGGER.trace("Starting unannounced join of chat room '"
                        + channelName);
                    // Assuming that at the time that NICKS_END_OF_LIST is
                    // propagated, the channel join event has been completely
                    // handled by IRCApi.
                    channel =
                        IrcConnection.this.connectionState
                            .getChannelByName(channelName);
                    chatRoom = new ChatRoomIrcImpl(
                        channelName, IrcConnection.this.provider);
                    IrcConnection.this.joined.put(channelName, chatRoom);
                }
                this.irc.addListener(new ChatRoomListener(this.irc, chatRoom));
                try
                {
                    IrcConnection.this.provider.getMUC().openChatRoomWindow(
                        chatRoom);
                }
                catch (NullPointerException e)
                {
                    LOGGER.error("failed to open chat room window", e);
                }
                IrcConnection.this.prepareChatRoom(chatRoom, channel);
                IrcConnection.this.provider.getMUC().fireLocalUserPresenceEvent(
                    chatRoom,
                    LocalUserChatRoomPresenceChangeEvent.LOCAL_USER_JOINED,
                    null);
                LOGGER.trace("Unannounced join of chat room '" + channelName
                    + "' completed.");
                break;

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

    /**
     * A chat room listener.
     *
     * A chat room listener is registered for each chat room that we join. The
     * chat room listener updates chat room data and fires events based on IRC
     * messages that report state changes for the specified channel.
     *
     * @author Danny van Heumen
     *
     */
    private final class ChatRoomListener
        extends VariousMessageListenerAdapter
    {
        /**
         * IRC error code for case when user cannot send a message to the
         * channel, for example when this channel is moderated and user does not
         * have VOICE (+v).
         */
        private static final int IRC_ERR_CANNOTSENDTOCHAN = 404;

        /**
         * IRC error code for case where user is not joined to that channel.
         */
        private static final int IRC_ERR_NOTONCHANNEL = 442;

        /**
         * IRCApi instance.
         */
        private final IRCApi irc;

        /**
         * Chat room for which this listener is working.
         */
        private final ChatRoomIrcImpl chatroom;

        /**
         * Constructor. Instantiate listener for the provided chat room.
         *
         * @param irc IRCApi instance
         * @param chatroom the chat room
         */
        private ChatRoomListener(final IRCApi irc,
            final ChatRoomIrcImpl chatroom)
        {
            if (chatroom == null)
            {
                throw new IllegalArgumentException("chatroom cannot be null");
            }
            this.chatroom = chatroom;
            if (irc == null)
            {
                throw new IllegalArgumentException("irc cannot be null");
            }
            this.irc = irc;
        }

        /**
         * Event in case of topic change.
         *
         * @param msg topic change message
         */
        @Override
        public void onTopicChange(final TopicMessage msg)
        {
            if (!isThisChatRoom(msg.getChannelName()))
            {
                return;
            }

            // FIXME Topic change event report message interprets HTML chars in
            // channel name.
            this.chatroom.updateSubject(msg.getTopic().getValue());
        }

        /**
         * Event in case of channel mode changes.
         *
         * @param msg channel mode message
         */
        @Override
        public void onChannelMode(final ChannelModeMessage msg)
        {
            if (!isThisChatRoom(msg.getChannelName()))
            {
                return;
            }

            processModeMessage(msg);
        }

        /**
         * Event in case of channel join message.
         *
         * @param msg channel join message
         */
        @Override
        public void onChannelJoin(final ChanJoinMessage msg)
        {
            if (!isThisChatRoom(msg.getChannelName()))
            {
                return;
            }

            final String user = msg.getSource().getNick();
            final ChatRoomMemberIrcImpl member =
                new ChatRoomMemberIrcImpl(IrcConnection.this.provider,
                    this.chatroom, user, ChatRoomMemberRole.SILENT_MEMBER);
            this.chatroom.fireMemberPresenceEvent(member, null,
                ChatRoomMemberPresenceChangeEvent.MEMBER_JOINED, null);
        }

        /**
         * Event in case of channel part.
         *
         * @param msg channel part message
         */
        @Override
        public void onChannelPart(final ChanPartMessage msg)
        {
            if (!isThisChatRoom(msg.getChannelName()))
            {
                return;
            }

            final IRCUser user = msg.getSource();
            if (isMe(user))
            {
                leaveChatRoom();
                return;
            }

            final String userNick = msg.getSource().getNick();
            final ChatRoomMember member =
                this.chatroom.getChatRoomMember(userNick);
            if (member != null)
            {
                // When the account has been disabled, the chat room may return
                // null. If that is NOT the case, continue handling.
                try
                {
                    this.chatroom.fireMemberPresenceEvent(member, null,
                        ChatRoomMemberPresenceChangeEvent.MEMBER_LEFT,
                        msg.getPartMsg());
                }
                catch (NullPointerException e)
                {
                    LOGGER.warn(
                        "This should not have happened. Please report this "
                            + "as it is a bug.", e);
                }
            }
        }

        /**
         * Some of the generic message are relevant to us, so keep an eye on
         * general numeric messages.
         *
         * @param msg IRC server numeric message
         */
        public void onServerNumericMessage(final ServerNumericMessage msg)
        {
            final Integer code = msg.getNumericCode();
            if (code == null)
            {
                return;
            }
            final String raw = msg.getText();
            switch (code.intValue())
            {
            case IRC_ERR_NOTONCHANNEL:
                final String channel = raw.substring(0, raw.indexOf(" "));
                if (isThisChatRoom(channel))
                {
                    LOGGER
                        .warn("Just discovered that we are no longer joined to "
                            + "channel "
                            + channel
                            + ". Leaving quietly. (This is most likely due to a"
                            + " bug in the implementation.)");
                    // If for some reason we missed the message that we aren't
                    // joined (anymore) to this particular chat room, correct
                    // our problem ASAP.
                    leaveChatRoom();
                }
                break;

            case IRC_ERR_CANNOTSENDTOCHAN:
                final String cannotSendChannel =
                    raw.substring(0, raw.indexOf(" "));
                if (isThisChatRoom(cannotSendChannel))
                {
                    final MessageIrcImpl message =
                        new MessageIrcImpl("", "text/plain", "UTF-8", null);
                    this.chatroom.fireMessageDeliveryFailedEvent(
                        ChatRoomMessageDeliveryFailedEvent.FORBIDDEN,
                        "This channel is moderated.", new Date(), message);
                }
                break;

            default:
                break;
            }
        }

        /**
         * Event in case of channel kick.
         *
         * @param msg channel kick message
         */
        @Override
        public void onChannelKick(final ChannelKick msg)
        {
            if (!isThisChatRoom(msg.getChannelName()))
            {
                return;
            }

            if (!IrcConnection.this.isConnected())
            {
                LOGGER.error("Not currently connected to IRC Server. "
                    + "Aborting message handling.");
                return;
            }

            final String kickedUser = msg.getKickedNickname();
            final ChatRoomMember kickedMember =
                this.chatroom.getChatRoomMember(kickedUser);
            final String user = msg.getSource().getNick();
            if (kickedMember != null)
            {
                ChatRoomMember kicker = this.chatroom.getChatRoomMember(user);
                this.chatroom.fireMemberPresenceEvent(kickedMember, kicker,
                    ChatRoomMemberPresenceChangeEvent.MEMBER_KICKED,
                    msg.getText());
            }
            if (isMe(kickedUser))
            {
                LOGGER.debug(
                    "Local user is kicked. Removing chat room listener.");
                this.irc.deleteListener(this);
                IrcConnection.this.joined.remove(this.chatroom.getIdentifier());
                IrcConnection.this.provider.getMUC().fireLocalUserPresenceEvent(
                    this.chatroom,
                    LocalUserChatRoomPresenceChangeEvent.LOCAL_USER_KICKED,
                    msg.getText());
            }
        }

        /**
         * Event in case of user quit.
         *
         * @param msg user quit message
         */
        @Override
        public void onUserQuit(final QuitMessage msg)
        {
            String user = msg.getSource().getNick();
            if (user == null)
            {
                return;
            }
            if (user.equals(IrcConnection.this.connectionState.getNickname()))
            {
                LOGGER.debug("Local user QUIT message received: removing chat "
                    + "room listener.");
                this.irc.deleteListener(this);
                return;
            }
            final ChatRoomMember member = this.chatroom.getChatRoomMember(user);
            if (member != null)
            {
                this.chatroom.fireMemberPresenceEvent(member, null,
                    ChatRoomMemberPresenceChangeEvent.MEMBER_QUIT,
                    msg.getQuitMsg());
            }
        }

        /**
         * Event in case of nick change.
         *
         * @param msg nick change message
         */
        @Override
        public void onNickChange(final NickMessage msg)
        {
            if (msg == null)
            {
                return;
            }

            final String oldNick = msg.getSource().getNick();
            final String newNick = msg.getNewNick();

            final ChatRoomMemberIrcImpl member =
                (ChatRoomMemberIrcImpl) this.chatroom
                    .getChatRoomMember(oldNick);
            if (member != null)
            {
                member.setName(newNick);
                this.chatroom.updateChatRoomMemberName(oldNick);
                ChatRoomMemberPropertyChangeEvent evt =
                    new ChatRoomMemberPropertyChangeEvent(member,
                        this.chatroom,
                        ChatRoomMemberPropertyChangeEvent.MEMBER_NICKNAME,
                        oldNick, newNick);
                this.chatroom.fireMemberPropertyChangeEvent(evt);
            }
        }

        /**
         * Event in case of channel message arrival.
         *
         * @param msg channel message
         */
        @Override
        public void onChannelMessage(final ChannelPrivMsg msg)
        {
            if (!isThisChatRoom(msg.getChannelName()))
            {
                return;
            }

            final MessageIrcImpl message =
                MessageIrcImpl.newMessageFromIRC(msg.getText());
            final ChatRoomMemberIrcImpl member =
                new ChatRoomMemberIrcImpl(IrcConnection.this.provider,
                    this.chatroom, msg.getSource().getNick(),
                    ChatRoomMemberRole.MEMBER);
            this.chatroom.fireMessageReceivedEvent(message, member, new Date(),
                ChatRoomMessageReceivedEvent.CONVERSATION_MESSAGE_RECEIVED);
        }

        /**
         * Event in case of channel action message arrival.
         *
         * @param msg channel action message
         */
        @Override
        public void onChannelAction(final ChannelActionMsg msg)
        {
            if (!isThisChatRoom(msg.getChannelName()))
            {
                return;
            }

            String userNick = msg.getSource().getNick();
            ChatRoomMemberIrcImpl member =
                new ChatRoomMemberIrcImpl(IrcConnection.this.provider,
                    this.chatroom, userNick, ChatRoomMemberRole.MEMBER);
            MessageIrcImpl message =
                MessageIrcImpl.newActionFromIRC(member, msg.getText());
            this.chatroom.fireMessageReceivedEvent(message, member, new Date(),
                ChatRoomMessageReceivedEvent.CONVERSATION_MESSAGE_RECEIVED);
        }

        /**
         * Event in case of channel notice message arrival.
         *
         * @param msg channel notice message
         */
        @Override
        public void onChannelNotice(final ChannelNotice msg)
        {
            if (!isThisChatRoom(msg.getChannelName()))
            {
                return;
            }

            final String userNick = msg.getSource().getNick();
            final ChatRoomMemberIrcImpl member =
                new ChatRoomMemberIrcImpl(IrcConnection.this.provider,
                    this.chatroom, userNick, ChatRoomMemberRole.MEMBER);
            final MessageIrcImpl message =
                MessageIrcImpl.newNoticeFromIRC(member, msg.getText());
            this.chatroom.fireMessageReceivedEvent(message, member, new Date(),
                ChatRoomMessageReceivedEvent.CONVERSATION_MESSAGE_RECEIVED);
        }

        /**
         * Leave this chat room.
         */
        private void leaveChatRoom()
        {
            this.irc.deleteListener(this);
            IrcConnection.this.joined.remove(this.chatroom.getIdentifier());
            LOGGER.debug("Leaving chat room " + this.chatroom.getIdentifier()
                + ". Chat room listener removed.");
            IrcConnection.this.provider.getMUC().fireLocalUserPresenceEvent(
                this.chatroom,
                LocalUserChatRoomPresenceChangeEvent.LOCAL_USER_LEFT, null);
        }

        /**
         * Process mode changes.
         *
         * @param msg raw mode message
         */
        private void processModeMessage(final ChannelModeMessage msg)
        {
            final ChatRoomMemberIrcImpl source = extractChatRoomMember(msg);
            final ModeParser parser = new ModeParser(msg.getModeStr());
            for (ModeEntry mode : parser.getModes())
            {
                switch (mode.getMode())
                {
                case OWNER:
                case OPERATOR:
                case HALFOP:
                case VOICE:
                    processRoleChange(source, mode);
                    break;
                case LIMIT:
                    processLimitChange(source, mode);
                    break;
                case BAN:
                    processBanChange(source, mode);
                    break;
                case UNKNOWN:
                    if (LOGGER.isInfoEnabled())
                    {
                        LOGGER.info("Unknown mode: "
                            + (mode.isAdded() ? "+" : "-")
                            + mode.getParams()[0] + ". Original mode string: '"
                            + msg.getModeStr() + "'");
                    }
                    break;
                default:
                    if (LOGGER.isInfoEnabled())
                    {
                        LOGGER.info("Unsupported mode '"
                            + (mode.isAdded() ? "+" : "-") + mode.getMode()
                            + "' (from modestring '" + msg.getModeStr() + "')");
                    }
                    break;
                }
            }
        }

        /**
         * Process changes for ban patterns.
         *
         * @param sourceMember the originating member
         * @param mode the ban mode change
         */
        private void processBanChange(final ChatRoomMemberIrcImpl sourceMember,
            final ModeEntry mode)
        {
            final MessageIrcImpl banMessage =
                new MessageIrcImpl(
                    "channel ban mask was "
                        + (mode.isAdded() ? "added" : "removed")
                        + ": "
                        + mode.getParams()[0]
                        + " by "
                        + (sourceMember.getContactAddress().length() == 0
                            ? "server"
                            : sourceMember.getContactAddress()),
                    MessageIrcImpl.DEFAULT_MIME_TYPE,
                    MessageIrcImpl.DEFAULT_MIME_ENCODING, null);
            this.chatroom.fireMessageReceivedEvent(banMessage, sourceMember,
                new Date(),
                ChatRoomMessageReceivedEvent.SYSTEM_MESSAGE_RECEIVED);
        }

        /**
         * Process mode changes resulting in role manipulation.
         *
         * @param sourceMember the originating member
         * @param mode the mode change
         */
        private void processRoleChange(
            final ChatRoomMemberIrcImpl sourceMember, final ModeEntry mode)
        {
            final String targetNick = mode.getParams()[0];
            final ChatRoomMemberIrcImpl targetMember =
                (ChatRoomMemberIrcImpl) this.chatroom
                    .getChatRoomMember(targetNick);
            final ChatRoomMemberRole originalRole = targetMember.getRole();
            if (mode.isAdded())
            {
                targetMember.addRole(mode.getMode().getRole());
            }
            else
            {
                targetMember.removeRole(mode.getMode().getRole());
            }
            final ChatRoomMemberRole newRole = targetMember.getRole();
            if (newRole != originalRole)
            {
                // Mode change actually caused a role change.
                final ChatRoomLocalUserRoleChangeEvent event =
                    new ChatRoomLocalUserRoleChangeEvent(this.chatroom,
                        originalRole, newRole, false);
                if (isMe(targetMember.getContactAddress()))
                {
                    this.chatroom.fireLocalUserRoleChangedEvent(event);
                }
                else
                {
                    this.chatroom.fireMemberRoleEvent(targetMember,
                        newRole);
                }
            }
            else
            {
                // Mode change did not cause an immediate role change.
                // Display a system message for the mode change.
                final String text =
                    sourceMember.getName()
                        + (mode.isAdded() ? " gives "
                            + mode.getMode().name().toLowerCase()
                            + " to " : " removes "
                            + mode.getMode().name().toLowerCase()
                            + " from ") + targetMember.getName();
                final MessageIrcImpl message =
                    new MessageIrcImpl(text,
                        MessageIrcImpl.DEFAULT_MIME_TYPE,
                        MessageIrcImpl.DEFAULT_MIME_ENCODING, null);
                this.chatroom
                    .fireMessageReceivedEvent(
                        message,
                        sourceMember,
                        new Date(),
                        ChatRoomMessageReceivedEvent.SYSTEM_MESSAGE_RECEIVED);
            }
        }

        /**
         * Process mode change that represents a channel limit modification.
         *
         * @param sourceMember the originating member
         * @param mode the limit mode change
         */
        private void processLimitChange(
            final ChatRoomMemberIrcImpl sourceMember, final ModeEntry mode)
        {
            final MessageIrcImpl limitMessage;
            if (mode.isAdded())
            {
                try
                {
                    limitMessage =
                        new MessageIrcImpl(
                            "channel limit set to "
                                + Integer.parseInt(mode.getParams()[0])
                                + " by "
                                + (sourceMember.getContactAddress()
                                        .length() == 0
                                    ? "server"
                                    : sourceMember.getContactAddress()),
                            "text/plain", "UTF-8", null);
                }
                catch (NumberFormatException e)
                {
                    LOGGER.warn("server sent incorrect limit: "
                        + "limit is not a number", e);
                    return;
                }
            }
            else
            {
                // TODO "server" is now easily fakeable if someone
                // calls himself server. There should be some other way
                // to represent the server if a message comes from
                // something other than a normal chat room member.
                limitMessage =
                    new MessageIrcImpl(
                        "channel limit removed by "
                            + (sourceMember.getContactAddress().length() == 0
                                ? "server"
                                : sourceMember.getContactAddress()),
                        "text/plain", "UTF-8", null);
            }
            this.chatroom.fireMessageReceivedEvent(limitMessage, sourceMember,
                new Date(),
                ChatRoomMessageReceivedEvent.SYSTEM_MESSAGE_RECEIVED);
        }

        /**
         * Extract chat room member identifier from message.
         *
         * @param msg raw mode message
         * @return returns member instance
         */
        private ChatRoomMemberIrcImpl extractChatRoomMember(
            final ChannelModeMessage msg)
        {
            ChatRoomMemberIrcImpl member;
            ISource source = msg.getSource();
            if (source instanceof IRCServer)
            {
                // TODO Created chat room member with creepy empty contact ID.
                // Interacting with this contact might screw up other sections
                // of code which is not good. Is there a better way to represent
                // an IRC server as a chat room member?
                member =
                    new ChatRoomMemberIrcImpl(IrcConnection.this.provider,
                        this.chatroom, "", ChatRoomMemberRole.ADMINISTRATOR);
            }
            else if (source instanceof IRCUser)
            {
                String nick = ((IRCUser) source).getNick();
                member =
                    (ChatRoomMemberIrcImpl) this.chatroom
                        .getChatRoomMember(nick);
            }
            else
            {
                throw new IllegalArgumentException("Unknown source type: "
                    + source.getClass().getName());
            }
            return member;
        }

        /**
         * Test whether this listener corresponds to the chat room.
         *
         * @param chatRoomName chat room name
         * @return returns true if this listener applies, false otherwise
         */
        private boolean isThisChatRoom(final String chatRoomName)
        {
            return this.chatroom.getIdentifier().equalsIgnoreCase(chatRoomName);
        }

        /**
         * Test whether the source user is this user.
         *
         * @param user the source user
         * @return returns true if this use, or false otherwise
         */
        private boolean isMe(final IRCUser user)
        {
            return isMe(user.getNick());
        }

        /**
         * Test whether the user nick is this user.
         *
         * @param name nick of the user
         * @return returns true if so, false otherwise
         */
        private boolean isMe(final String name)
        {
            final String userNick =
                IrcConnection.this.connectionState.getNickname();
            if (userNick == null)
            {
                return false;
            }
            return userNick.equals(name);
        }
    }
}
