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

import java.util.*;
import java.util.Map.Entry;

import net.java.sip.communicator.impl.protocol.irc.ModeParser.ModeEntry;
import net.java.sip.communicator.impl.protocol.irc.exception.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

import com.ircclouds.irc.api.*;
import com.ircclouds.irc.api.domain.*;
import com.ircclouds.irc.api.domain.messages.*;
import com.ircclouds.irc.api.domain.messages.interfaces.*;
import com.ircclouds.irc.api.state.*;

/**
 * Channel manager.
 *
 * TODO Implement channel services (ChanServ - channel related services) that
 * can be used for accessing remove channel facilities.
 *
 * TODO Do we need to cancel any join channel operations still in progress?
 *
 * @author Danny van Heumen
 */
public class ChannelManager
{
    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(ChannelManager.class);

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
     * Provider.
     */
    private final ProtocolProviderServiceIrcImpl provider;

    /**
     * Client configuration.
     */
    private final ClientConfig config;

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
     * Maximum channel name length according to server ISUPPORT instructions.
     *
     * <p>This value is not guaranteed, so it may be <tt>null</tt>.</p>
     */
    private final Integer isupportChannelLen;

    /**
     * Maximum topic length according to server ISUPPORT instructions.
     *
     * <p>This value is not guaranteed, so it may be <tt>null</tt>.</p>
     */
    private final Integer isupportTopicLen;

    /**
     * Maximum kick message length according to server ISUPPORT instructions.
     *
     * <p>This value is not guaranteed, so it may be <tt>null</tt>.</p>
     */
    private final Integer isupportKickLen;

    /**
     * Maximum number of joined channels according to server ISUPPORT
     * instructions. Limits are stored per channel type (#, &, etc.)
     *
     * <p>This value is not guaranteed, so it may be <tt>null</tt>.</p>
     */
    private final Map<Character, Integer> isupportChanLimit
            = new HashMap<Character, Integer>();

    /**
     * Flag for indicating availability of Away Notify capability.
     */
    private final boolean awayNotify;

    /**
     * Constructor.
     *
     * @param irc thread-safe IRCApi instance
     * @param connectionState the connection state
     * @param provider the provider instance
     * @param config client configuration
     */
    public ChannelManager(final IRCApi irc, final IIRCState connectionState,
        final ProtocolProviderServiceIrcImpl provider,
        final ClientConfig config, final boolean awayNotifyCapability)
    {
        if (irc == null)
        {
            throw new IllegalArgumentException("irc instance cannot be null");
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
        if (config == null)
        {
            throw new IllegalArgumentException("client config cannot be null");
        }
        this.config = config;
        this.irc.addListener(new ManagerListener());

        // parse ISUPPORT parameters
        this.isupportChannelLen = parseISupportInteger(this.connectionState,
                ISupport.CHANNELLEN);
        this.isupportTopicLen = parseISupportInteger(this.connectionState,
                ISupport.TOPICLEN);
        this.isupportKickLen = parseISupportInteger(this.connectionState,
                ISupport.KICKLEN);
        parseISupportChanLimit(this.isupportChanLimit, this.connectionState);
        this.awayNotify = awayNotifyCapability;
    }

    /**
     * Parse the ISUPPORT parameter for Integer value.
     *
     * @param state the connection state
     * @return returns instance with parameter value or <tt>null</tt> if
     *         not specified.
     */
    private static Integer parseISupportInteger(final IIRCState state,
            final ISupport param)
    {
        final String value = state.getServerOptions().getKey(param.name());
        if (value == null)
        {
            return null;
        }
        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug("Setting ISUPPORT parameter " + param.name() + " to "
                    + value);
        }
        return new Integer(value);
    }

    /**
     * Parse the raw ISUPPORT CHANLIMIT value, extract its values into the
     * destination map.
     *
     * @param destination the destination map
     * @param state the IRC connection state
     */
    private static void parseISupportChanLimit(
        final Map<Character, Integer> destination, final IIRCState state)
    {
        final String rawChanLimitValue =
            state.getServerOptions().getKey(ISupport.CHANLIMIT.name());
        ISupport.parseChanLimit(destination, rawChanLimitValue);
        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug("Parsed ISUPPORT CHANLIMIT parameter: "
                + rawChanLimitValue);
            for (Entry<Character, Integer> e : destination.entrySet())
            {
                LOGGER.debug(e.getKey() + ":" + e.getValue());
            }
        }
    }

    /**
     * Get a set of channel type indicators.
     *
     * @return returns set of channel type indicators.
     */
    public Set<Character> getChannelTypes()
    {
        return this.connectionState.getServerOptions().getChanTypes();
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
        if (!this.connectionState.isConnected())
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
        if (this.isupportChannelLen != null
            && chatRoomId.length() > this.isupportChannelLen)
        {
            throw new IllegalArgumentException("the channel name must not be "
                + "longer than " + this.isupportChannelLen.intValue()
                + " characters according to server parameters.");
        }

        // Verify max channel limit based on server parameters (ISupport)
        final Integer limit = this.isupportChanLimit.get(chatRoomId.charAt(0));
        if (limit != null && this.joined.size() >= limit)
        {
            throw new IllegalStateException("already joined to the maximum "
                    + "allowed number of channels ("
                    + this.isupportChanLimit.toString() + ") according to "
                    + "server parameters.");
        }

        LOGGER.trace("Start joining channel " + chatRoomId);
        final Result<Object, Exception> joinSignal =
            new Result<Object, Exception>();
        synchronized (joinSignal)
        {
            LOGGER.trace("Issue join channel command to IRC library and wait "
                + "for join operation to complete (un)successfully.");

            this.joined.put(chatRoomId, null);
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
                            LOGGER.trace("Started callback for successful "
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
                                        + "since we got forwarded from '"
                                        + chatRoomId + "' to '"
                                        + channel.getName()
                                        + "'. Joining of forwarded channel "
                                        + "gets handled by Server Listener "
                                        + "since that channel was not "
                                        + "announced.");
                                }
                                // Remove original chat room id from
                                // joined-list since we aren't actually
                                // attempting to join this room anymore.
                                ChannelManager.this.joined.remove(chatRoomId);
                                ChannelManager.this.provider
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
                                ChannelManager.this.joined.put(chatRoomId,
                                    chatroom);
                                ChannelManager.this.irc
                                    .addListener(new ChatRoomListener(chatroom,
                                        ChannelManager.this.config
                                            .isChannelPresenceTaskEnabled(),
                                            ChannelManager.this.awayNotify));
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
                                ChannelManager.this.provider
                                    .getMUC()
                                    .fireLocalUserPresenceEvent(
                                        chatroom,
                                        LocalUserChatRoomPresenceChangeEvent
                                            .LOCAL_USER_JOINED,
                                        null);
                                if (LOGGER.isTraceEnabled())
                                {
                                    LOGGER.trace("Finished successful join "
                                        + "callback for channel '" + chatRoomId
                                        + "'. Waking up original thread.");
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
                        LOGGER.trace("Started callback for failed attempt to "
                            + "join channel '" + chatRoomId + "'.");
                        synchronized (joinSignal)
                        {
                            try
                            {
                                ChannelManager.this.joined.remove(chatRoomId);
                                ChannelManager.this.provider
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
                                    LOGGER.trace("Finished callback for "
                                        + "failed attempt to join "
                                        + "channel '" + chatRoomId
                                        + "'. Waking up original thread.");
                                }
                                // Notify waiting threads of finished
                                // execution
                                joinSignal.setDone(e);
                                joinSignal.notifyAll();
                            }
                        }
                    }
                });

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

        for (final IRCUser user : channel.getUsers())
        {
            final ChatRoomMemberIrcImpl member =
                new ChatRoomMemberIrcImpl(this.provider, chatRoom,
                    user.getNick(), user.getIdent(), user.getHostname(),
                    ChatRoomMemberRole.SILENT_MEMBER, IrcStatusEnum.ONLINE);
            ChatRoomMemberRole role;
            for (final IRCUserStatus status : channel.getStatusesForUser(user))
            {
                try
                {
                    if (LOGGER.isTraceEnabled())
                    {
                        LOGGER.trace("Processing role " + status.getPrefix()
                            + " for member " + user.getNick() + " in channel "
                            + channel.getName());
                    }
                    role = convertMemberMode(status.getChanModeType());
                    member.addRole(role);
                }
                catch (UnknownModeException e)
                {
                    LOGGER.info(
                        "Unknown mode encountered. This mode will be ignored.",
                        e);
                }
            }
            chatRoom.addChatRoomMember(member.getContactAddress(), member);
            if (this.connectionState.getNickname().equals(user.getNick()))
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
     * Set the subject of the specified chat room.
     *
     * @param chatroom The chat room for which to set the subject.
     * @param subject The subject.
     */
    public void setSubject(final ChatRoomIrcImpl chatroom, final String subject)
    {
        if (!this.connectionState.isConnected())
        {
            throw new IllegalStateException(
                "Please connect to an IRC server first.");
        }
        if (chatroom == null)
        {
            throw new IllegalArgumentException("Cannot have a null chatroom");
        }
        if (this.isupportTopicLen != null
            && subject.length() > this.isupportTopicLen)
        {
            throw new IllegalArgumentException("the topic length must not be "
                + "longer than " + this.isupportTopicLen
                + " characters according to server parameters.");
        }
        LOGGER.trace("Setting chat room topic to '" + subject + "'");
        this.irc.changeTopic(chatroom.getIdentifier(), subject == null ? ""
            : subject);
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
        if (!this.connectionState.isConnected())
        {
            throw new IllegalStateException("Not connected to an IRC server.");
        }

        try
        {
            this.irc.leaveChannel(chatRoomName);
        }
        catch (ApiException e)
        {
            LOGGER.warn("exception occurred while leaving channel", e);
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
        if (!this.connectionState.isConnected())
        {
            throw new IllegalStateException("Not connected to an IRC server.");
        }
        if (mode.getRole() == null)
        {
            throw new IllegalArgumentException(
                "This mode does not modify user permissions.");
        }
        this.irc.changeMode(chatRoom.getIdentifier() + " +" + mode.getSymbol()
            + " " + userAddress);
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
        if (!this.connectionState.isConnected())
        {
            throw new IllegalStateException("Not connected to an IRC server.");
        }
        if (mode.getRole() == null)
        {
            throw new IllegalArgumentException(
                "This mode does not modify user permissions.");
        }
        this.irc.changeMode(chatRoom.getIdentifier() + " -" + mode.getSymbol()
            + " " + userAddress);
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
        final ChatRoomMemberIrcImpl member, final String reason)
        throws OperationFailedException
    {
        if (!this.connectionState.isConnected())
        {
            return;
        }
        kickParticipant(chatroom, member, reason);
        this.irc.changeMode(String.format("%s +b %s!%s@%s",
            chatroom.getIdentifier(), "*",
            member.getIdent(), member.getHostname()));
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
        if (!this.connectionState.isConnected())
        {
            return;
        }
        if (this.isupportKickLen != null
            && reason.length() > this.isupportKickLen)
        {
            throw new IllegalArgumentException("the kick reason must not be "
                + "longer than " + this.isupportKickLen.intValue()
                + " characters according to server parameters.");
        }
        this.irc.kick(chatroom.getIdentifier(), member.getContactAddress(),
            reason);
    }

    /**
     * Issue invite command to IRC server.
     *
     * @param memberId member to invite
     * @param chatroom channel to invite to
     */
    public void invite(final String memberId, final ChatRoomIrcImpl chatroom)
    {
        if (!this.connectionState.isConnected())
        {
            throw new IllegalStateException("Not connected to an IRC server.");
        }
        this.irc.rawMessage("INVITE " + memberId + " "
            + chatroom.getIdentifier());
    }

    /**
     * Convert a member mode character to a ChatRoomMemberRole instance.
     *
     * @param modeSymbol The member mode character.
     * @return Return the instance of ChatRoomMemberRole corresponding to the
     *         member mode character.
     * @throws UnknownModeException returns UnknownModeException in case unknown
     *             mode is encountered
     */
    private static ChatRoomMemberRole convertMemberMode(final char modeSymbol)
        throws UnknownModeException
    {
        return Mode.bySymbol(modeSymbol).getRole();
    }

    /**
     * The channel manager listener. This listener is used for any events that
     * are not directly related to an open, managed chat room. This includes
     * events signaling that a channel has been joined on initiative of the IRC
     * server, such that it isn't managed yet.
     *
     * @author Danny van Heumen
     */
    private final class ManagerListener extends AbstractIrcMessageListener
    {
        /**
         * IRC reply code for end of list.
         */
        private static final int RPL_LISTEND =
            IRCServerNumerics.CHANNEL_NICKS_END_OF_LIST;

        /**
         * Constructor.
         */
        public ManagerListener()
        {
            super(ChannelManager.this.irc, ChannelManager.this.connectionState);
        }

        /**
         * Server numeric message.
         *
         * @param msg server numeric message
         */
        @Override
        public void onServerNumericMessage(final ServerNumericMessage msg)
        {
            switch (msg.getNumericCode())
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
                synchronized (ChannelManager.this.joined)
                {
                    // Synchronize the section that checks then adds a chat
                    // room. This way we can be sure that there are no 2
                    // simultaneous creation events.
                    if (ChannelManager.this.joined.containsKey(channelName))
                    {
                        LOGGER.trace("Chat room '" + channelName
                            + "' join event was announced or already "
                            + "finished. Stop handling this event.");
                        break;
                    }
                    // We aren't currently attempting to join, so this join is
                    // unannounced.
                    LOGGER.trace("Starting unannounced join of chat room '"
                        + channelName + "'");
                    // Assuming that at the time that NICKS_END_OF_LIST is
                    // propagated, the channel join event has been completely
                    // handled by IRCApi.
                    channel =
                        this.connectionState.getChannelByName(channelName);
                    chatRoom =
                        new ChatRoomIrcImpl(channelName,
                            ChannelManager.this.provider);
                    ChannelManager.this.joined.put(channelName, chatRoom);
                }
                this.irc.addListener(new ChatRoomListener(chatRoom,
                    ChannelManager.this.config.isChannelPresenceTaskEnabled(),
                    ChannelManager.this.awayNotify));
                try
                {
                    ChannelManager.this.provider.getMUC().openChatRoomWindow(
                        chatRoom);
                }
                catch (NullPointerException e)
                {
                    LOGGER.error("failed to open chat room window", e);
                }
                ChannelManager.this.prepareChatRoom(chatRoom, channel);
                ChannelManager.this.provider.getMUC()
                    .fireLocalUserPresenceEvent(chatRoom,
                    LocalUserChatRoomPresenceChangeEvent.LOCAL_USER_JOINED,
                    null);
                LOGGER.trace("Unannounced join of chat room '" + channelName
                    + "' completed.");
                break;

            default:
                break;
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
     */
    private final class ChatRoomListener
        extends AbstractIrcMessageListener
    {
        /**
         * Indicator for those members whose AWAY message is set.
         */
        private static final String GONE = "G";

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
         * IRC reply code for WHO reply entry for an individual user.
         */
        private static final int IRC_RPL_WHOREPLY = 352;

        /**
         * IRC reply code for end of WHO reply list.
         */
        private static final int IRC_RPL_ENDOFWHO = 315;

        /**
         * Presence task period.
         */
        private static final long TASK_PERIOD = 60000L;

        /**
         * Presence task initial delay.
         *
         * The first WHO-request is fired during ChatRoomListener constructions,
         * as we need at least 1 such request, even if away-notify capability is
         * active.
         */
        private static final long TASK_INITIAL_DELAY = TASK_PERIOD;

        /**
         * Chat room for which this listener is working.
         */
        private final ChatRoomIrcImpl chatroom;

        /**
         * Presence task timer.
         */
        private final Timer presenceTaskTimer = new Timer();

        /**
         * Constructor. Instantiate listener for the provided chat room.
         *
         * @param chatroom the chat room
         * @param activatePresenceWatcher flag indicating whether or not to
         *            activate the periodic presence watcher task
         * @param awayNotifyCapability flag indicating whether or not away
         *            notifications are active. If they are active, there is no
         *            need to periodically query presence status.
         */
        private ChatRoomListener(final ChatRoomIrcImpl chatroom,
            final boolean activatePresenceWatcher,
            final boolean awayNotifyCapability)
        {
            super(ChannelManager.this.irc, ChannelManager.this.connectionState);
            if (chatroom == null)
            {
                throw new IllegalArgumentException("chatroom cannot be null");
            }
            this.chatroom = chatroom;
            if (activatePresenceWatcher && !awayNotifyCapability)
            {
                createPeriodicPresenceWatcher();
            }
            else
            {
                LOGGER.info("Not activating periodic presence watcher. "
                    + "(away-notify capability is " + awayNotifyCapability
                    + ")");
            }
            this.irc.rawMessage("WHO " + chatroom.getIdentifier());
        }

        /**
         * Create periodic task for updating channel presence statuses.
         */
        private void createPeriodicPresenceWatcher() {
            final TimerTask task = new TimerTask()
            {
                @Override
                public void run()
                {
                    irc.rawMessage("WHO " + chatroom.getIdentifier());
                }
            };
            this.presenceTaskTimer.schedule(task, TASK_INITIAL_DELAY,
                TASK_PERIOD);
            LOGGER.debug("Scheduled periodic task for querying member presence "
                + "for channel " + this.chatroom.getIdentifier());
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
            final String ident = msg.getSource().getIdent();
            final String host = msg.getSource().getHostname();
            final ChatRoomMemberIrcImpl member =
                new ChatRoomMemberIrcImpl(ChannelManager.this.provider,
                    this.chatroom, user, ident, host,
                    ChatRoomMemberRole.SILENT_MEMBER, IrcStatusEnum.ONLINE);
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
            if (localUser(user))
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
            switch (code)
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

            case IRC_RPL_WHOREPLY:
                final String[] messageComponents = msg.getText().split(" ");
                if (messageComponents.length < 6
                    || !isThisChatRoom(messageComponents[0]))
                {
                    // We need at least 6 components in order to process this
                    // message correctly, so stop processing if this is not the
                    // case. Or if this reply was not targeted at this channel.
                    return;
                }
                final String nick = messageComponents[4];
                final ChatRoomMemberIrcImpl member =
                    (ChatRoomMemberIrcImpl) this.chatroom
                        .getChatRoomMember(nick);
                if (member != null)
                {
                    final IrcStatusEnum status =
                        determineStatus(messageComponents[5]);
                    updateMemberPresence(member, status);
                }
                break;

            default:
                break;
            }
        }

        /**
         * Determine the presence status by the code in the IRC WHO reply.
         *
         * @param presenceReply presence code
         * @return returns corresponding IrcStatusEnum instance
         */
        private IrcStatusEnum determineStatus(final String presenceReply)
        {
            if (presenceReply != null && presenceReply.startsWith(GONE))
            {
                return IrcStatusEnum.AWAY;
            }
            return IrcStatusEnum.ONLINE;
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

            if (!this.connectionState.isConnected())
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
            if (localUser(kickedUser))
            {
                LOGGER.debug(
                    "Local user is kicked. Removing chat room listener.");
                this.irc.deleteListener(this);
                ChannelManager.this.joined
                    .remove(this.chatroom.getIdentifier());
                ChannelManager.this.provider.getMUC()
                    .fireLocalUserPresenceEvent(this.chatroom,
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
            final String user = msg.getSource().getNick();
            if (localUser(user))
            {
                this.presenceTaskTimer.cancel();
            }
            else
            {
                final ChatRoomMember member =
                    this.chatroom.getChatRoomMember(user);
                if (member != null)
                {
                    this.chatroom.fireMemberPresenceEvent(member, null,
                        ChatRoomMemberPresenceChangeEvent.MEMBER_QUIT,
                        msg.getQuitMsg());
                }
            }
            super.onUserQuit(msg);
        }

        /**
         * Event in case of error. Cancel running timer then do the regular
         * onError stuff.
         */
        @Override
        public void onError(final ErrorMessage msg)
        {
            this.presenceTaskTimer.cancel();
            super.onError(msg);
        }

        /**
         * Event in case of client-side error. Cancel running timer then do the
         * regular onClientError stuff.
         */
        @Override
        public void onClientError(final ClientErrorMessage msg)
        {
            this.presenceTaskTimer.cancel();
            super.onClientError(msg);
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
                new ChatRoomMemberIrcImpl(ChannelManager.this.provider,
                    this.chatroom, msg.getSource().getNick(), msg.getSource()
                        .getIdent(), msg.getSource().getHostname(),
                    ChatRoomMemberRole.MEMBER, IrcStatusEnum.ONLINE);
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
                new ChatRoomMemberIrcImpl(ChannelManager.this.provider,
                    this.chatroom, userNick, msg.getSource().getIdent(), msg
                        .getSource().getHostname(), ChatRoomMemberRole.MEMBER,
                    IrcStatusEnum.ONLINE);
            MessageIrcImpl message =
                MessageIrcImpl.newActionFromIRC(msg.getText());
            this.chatroom.fireMessageReceivedEvent(message, member, new Date(),
                ChatRoomMessageReceivedEvent.ACTION_MESSAGE_RECEIVED);
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
                new ChatRoomMemberIrcImpl(ChannelManager.this.provider,
                    this.chatroom, userNick, msg.getSource().getIdent(), msg
                        .getSource().getHostname(), ChatRoomMemberRole.MEMBER,
                    IrcStatusEnum.ONLINE);
            final MessageIrcImpl message =
                MessageIrcImpl.newNoticeFromIRC(member, msg.getText());
            this.chatroom.fireMessageReceivedEvent(message, member, new Date(),
                ChatRoomMessageReceivedEvent.CONVERSATION_MESSAGE_RECEIVED);
        }

        /**
         * Event in case of user away message (CAP away-notify)
         *
         * @param aMsg away message
         */
        @Override
        public void onUserAway(AwayMessage msg)
        {
            final ChatRoomMemberIrcImpl member =
                (ChatRoomMemberIrcImpl) this.chatroom.getChatRoomMember(msg
                    .getSource().getNick());
            if (member != null)
            {
                final IrcStatusEnum status =
                    msg.isAway() ? IrcStatusEnum.AWAY : IrcStatusEnum.ONLINE;
                updateMemberPresence(member, status);
            }
        }

        /**
         * Update member presence status.
         *
         * @param member the member
         * @param newStatus the new presence status
         */
        private void updateMemberPresence(ChatRoomMemberIrcImpl member,
            IrcStatusEnum newStatus)
        {
            final IrcStatusEnum previous = member.setPresenceStatus(newStatus);
            if (previous == newStatus) {
                // if there is no change in status, do not fire member
                // property change event
                return;
            }
            final ChatRoomMemberPropertyChangeEvent presenceEvent =
                new ChatRoomMemberPropertyChangeEvent(member,
                    this.chatroom,
                    ChatRoomMemberPropertyChangeEvent.MEMBER_PRESENCE,
                    previous, newStatus);
            this.chatroom.fireMemberPropertyChangeEvent(presenceEvent);
        }

        /**
         * Leave this chat room.
         */
        private void leaveChatRoom()
        {
            this.presenceTaskTimer.cancel();
            this.irc.deleteListener(this);
            ChannelManager.this.joined.remove(this.chatroom.getIdentifier());
            LOGGER.debug("Leaving chat room " + this.chatroom.getIdentifier()
                + ". Chat room listener removed.");
            ChannelManager.this.provider.getMUC().fireLocalUserPresenceEvent(
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
                if (localUser(targetMember.getContactAddress()))
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
                    new ChatRoomMemberIrcImpl(ChannelManager.this.provider,
                        this.chatroom, "", "", "",
                        ChatRoomMemberRole.ADMINISTRATOR, IrcStatusEnum.ONLINE);
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
        private boolean localUser(final IRCUser user)
        {
            return localUser(user.getNick());
        }
    }
}
