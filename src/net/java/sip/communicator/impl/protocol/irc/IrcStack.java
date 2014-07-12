/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.irc;

import java.io.*;
import java.security.*;
import java.util.*;

import javax.net.ssl.*;

import net.java.sip.communicator.impl.protocol.irc.ModeParser.ModeEntry;
import net.java.sip.communicator.impl.protocol.irc.OperationSetBasicInstantMessagingIrcImpl.IrcMessage;
import net.java.sip.communicator.service.certificate.*;
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
 * An implementation of IRC using the irc-api library.
 * 
 * @author Danny van Heumen
 */
public class IrcStack
{
    /**
     * Expiration time for chat room list cache.
     */
    private static final long CHAT_ROOM_LIST_CACHE_EXPIRATION = 60000000000L;

    /**
     * Logger
     */
    private static final Logger LOGGER = Logger.getLogger(IrcStack.class);

    /**
     * Parent provider for IRC
     */
    private final ProtocolProviderServiceIrcImpl provider;

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
     * Server parameters that are set and provided during the connection
     * process.
     */
    private final ServerParameters params;

    /**
     * Instance of the IRC library.
     */
    private IRCApi irc;

    /**
     * Connection state of a successful IRC connection.
     */
    private IIRCState connectionState;

    /**
     * The cached channel list.
     * 
     * Contained inside a simple container object in order to lock the container
     * while accessing the contents.
     */
    private final Container<List<String>> channellist =
        new Container<List<String>>(null);

    /**
     * Constructor
     * 
     * @param parentProvider Parent provider
     * @param nick User's nick name
     * @param login User's login name
     * @param version Version
     * @param finger Finger
     */
    public IrcStack(final ProtocolProviderServiceIrcImpl parentProvider,
        final String nick, final String login, final String version,
        final String finger)
    {
        if (parentProvider == null)
        {
            throw new NullPointerException("parentProvider cannot be null");
        }
        this.provider = parentProvider;
        this.params = new IrcStack.ServerParameters(nick, login, finger, null);
    }

    /**
     * Check whether or not a connection is established.
     * 
     * @return true if connected, false otherwise.
     */
    public boolean isConnected()
    {
        return (this.irc != null && this.connectionState != null
            && this.connectionState.isConnected());
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
     * Connect to specified host, port, optionally using a password.
     * 
     * @param host IRC server's host name
     * @param port IRC port
     * @param password
     * @param autoNickChange
     * @throws Exception
     */
    public void connect(String host, int port, String password,
        boolean secureConnection, boolean autoNickChange) throws Exception
    {
        if (this.irc != null && this.connectionState != null
            && this.connectionState.isConnected())
            return;

        // Make sure we start with an empty joined-channel list.
        this.joined.clear();
        
        final IRCServer server;
        if (secureConnection)
        {
            server =
                new SecureIRCServer(host, port, password,
                    getCustomSSLContext(host));
        }
        else
        {
            server = new IRCServer(host, port, password, false);
        }

        this.irc = new IRCApiImpl(true);
        synchronized (this.irc)
        {
            this.params.setServer(server);
            this.irc.addListener(new ServerListener());

            if (LOGGER.isTraceEnabled())
            {
                // If tracing is enabled, register another listener that logs
                // all IRC messages as published by the IRC client library.
                this.irc.addListener(new IMessageListener()
                {

                    @Override
                    public void onMessage(IMessage aMessage)
                    {
                        LOGGER.trace("(" + aMessage + ") " + aMessage.asRaw());
                    }
                });
            }

            connectSynchronized();
        }
    }

    /**
     * Perform synchronized connect operation.
     * 
     * @return returns true upon successful connection, false otherwise
     * @throws Exception exception thrown when connect fails
     */
    private void connectSynchronized() throws Exception
    {
        final Result<IIRCState, Exception> result =
            new Result<IIRCState, Exception>();
        synchronized (result)
        {
            // start connecting to the specified server ...
            try
            {
                this.irc.connect(this.params, new Callback<IIRCState>()
                {

                    @Override
                    public void onSuccess(IIRCState state)
                    {
                        synchronized (result)
                        {
                            LOGGER.trace("IRC connected successfully!");
                            result.setDone(state);
                            result.notifyAll();
                        }
                    }

                    @Override
                    public void onFailure(Exception e)
                    {
                        synchronized (result)
                        {
                            LOGGER.trace("IRC connection FAILED!", e);
                            result.setDone(e);
                            result.notifyAll();
                        }
                    }
                });

                this.provider
                    .setCurrentRegistrationState(RegistrationState.REGISTERING);
                
                while (!result.isDone())
                {
                    LOGGER.trace("Waiting for the connection to be "
                        + "established ...");
                    result.wait();
                }

                this.connectionState = result.getValue();
                // TODO Implement connection timeout and a way to recognize that
                // the timeout occurred.
                if (this.connectionState != null
                    && this.connectionState.isConnected())
                {
                    // if connecting succeeded, set state to registered
                    this.provider.setCurrentRegistrationState(
                        RegistrationState.REGISTERED);
                }
                else
                {
                    // if connecting failed, set state to unregistered and throw
                    // the exception if one exists
                    this.provider
                        .setCurrentRegistrationState(
                            RegistrationState.CONNECTION_FAILED);
                    Exception e = result.getException();
                    if (e != null)
                        throw e;
                }
            }
            catch (IOException e)
            {
                // Also SSL exceptions will be caught here.
                this.provider
                    .setCurrentRegistrationState(
                        RegistrationState.CONNECTION_FAILED);
                throw e;
            }
            catch (InterruptedException e)
            {
                this.provider
                    .setCurrentRegistrationState(
                        RegistrationState.UNREGISTERED);
                throw e;
            }
        }
    }

    /**
     * Create a custom SSL context for this particular server.
     * 
     * @return returns a customized SSL context or <tt>null</tt> if one cannot
     *         be created.
     */
    private SSLContext getCustomSSLContext(String hostname)
    {
        SSLContext context = null;
        try
        {
            CertificateService cs =
                IrcActivator.getCertificateService();
            X509TrustManager tm =
                cs.getTrustManager(hostname);
            context = cs.getSSLContext(tm);
        }
        catch (GeneralSecurityException e)
        {
            LOGGER.error("failed to create custom SSL context", e);
        }
        return context;
    }

    /**
     * Disconnect from the IRC server
     */
    public void disconnect()
    {
        if (this.connectionState == null && this.irc == null)
            return;

        synchronized (this.joined)
        {
            // Leave all joined channels.
            for (ChatRoomIrcImpl channel : this.joined.values())
            {
                if (channel == null)
                {
                    // TODO how to cancel running joining attempt
                    continue;
                }
                leave(channel);
            }
        }
        synchronized (this.irc)
        {
            // Disconnect and clean up
            try
            {
                this.irc.disconnect();
            }
            catch (RuntimeException e)
            {
                // Disconnect might throw ChannelClosedException. Shouldn't be a
                // problem, but for now lets log it just to be sure.
                LOGGER.debug("exception occurred while disconnecting", e);
            }
            this.irc = null;
            this.connectionState = null;
        }
        this.provider
            .setCurrentRegistrationState(RegistrationState.UNREGISTERED);
    }

    /**
     * Dispose
     */
    public void dispose()
    {
        disconnect();
    }
    
    /**
     * Get a set of channel type indicators.
     * 
     * @return returns set of channel type indicators.
     */
    public Set<Character> getChannelTypes()
    {
        if (!isConnected())
            throw new IllegalStateException("not connected to IRC server");
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
        return (this.connectionState == null) ? this.params.getNickname()
            : this.connectionState.getNickname();
    }

    /**
     * Set the user's new nick name.
     * 
     * @param nick the new nick name
     */
    public void setUserNickname(String nick)
    {
        LOGGER.trace("Setting user's nick name to " + nick);
        if (this.connectionState == null)
        {
            this.params.setNickname(nick);
        }
        else
        {
            this.irc.changeNick(nick);
        }
    }

    /**
     * Set the subject of the specified chat room.
     * 
     * @param chatroom The chat room for which to set the subject.
     * @param subject The subject.
     */
    public void setSubject(ChatRoomIrcImpl chatroom, String subject)
    {
        if (!isConnected())
            throw new IllegalStateException(
                "Please connect to an IRC server first.");
        if (chatroom == null)
            throw new IllegalArgumentException("Cannot have a null chatroom");
        LOGGER.trace("Setting chat room topic to '" + subject + "'");
        this.irc
            .changeTopic(chatroom.getIdentifier(),
                         subject == null ? "" : subject);
    }

    /**
     * Check whether the user has joined a particular chat room.
     * 
     * @param chatroom Chat room to check for.
     * @return Returns true in case the user is already joined, or false if the
     *         user has not joined.
     */
    public boolean isJoined(ChatRoomIrcImpl chatroom)
    {
        return this.joined.get(chatroom.getIdentifier()) != null;
    }

    /**
     * Get a list of channels available on the IRC server.
     * 
     * @return List of available channels.
     */
    public List<String> getServerChatRoomList()
    {
        LOGGER.trace("Start retrieve server chat room list.");
        // TODO Currently, not using an API library method for listing
        // channels, since it isn't available.
        synchronized (this.channellist)
        {
            List<String> list =
                this.channellist.get(CHAT_ROOM_LIST_CACHE_EXPIRATION);
            if (list == null)
            {
                LOGGER
                    .trace("Chat room list null or outdated. Start retrieving "
                        + "new chat room list.");
                Result<List<String>, Exception> listSignal =
                    new Result<List<String>, Exception>(
                        new LinkedList<String>());
                synchronized (listSignal)
                {
                    try
                    {
                        this.irc.addListener(new ChannelListListener(this.irc,
                            listSignal));
                        this.irc.rawMessage("LIST");
                        while (!listSignal.isDone())
                        {
                            LOGGER.trace("Start waiting for list ...");
                            listSignal.wait();
                        }
                        LOGGER.trace("Done waiting for list.");
                    }
                    catch (InterruptedException e)
                    {
                        LOGGER.warn("INTERRUPTED while waiting for list.", e);
                    }
                }
                list = listSignal.getValue();
                this.channellist.set(list);
                LOGGER.trace("Finished retrieve server chat room list.");
            }
            else
            {
                LOGGER.trace("Using cached list of server chat rooms.");
            }
            return Collections.unmodifiableList(list);
        }
    }

    /**
     * Join a particular chat room.
     * 
     * @param chatroom Chat room to join.
     * @throws OperationFailedException failed to join the chat room
     */
    public void join(ChatRoomIrcImpl chatroom) throws OperationFailedException
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
            throw new IllegalStateException(
                "Please connect to an IRC server first");
        if (chatroom == null || chatroom.getIdentifier() == null
            || chatroom.getIdentifier().isEmpty())
            throw new IllegalArgumentException("chatroom cannot be null or emtpy");
        if (password == null)
            throw new IllegalArgumentException("password cannot be null");
        
        final String chatRoomId = chatroom.getIdentifier();
        if (!getChannelTypes().contains(chatRoomId.charAt(0)))
        {
            // I'm not going to throw an exception, but I believe that this case
            // cannot happen (anymore).
            LOGGER.warn("Is chat room '" + chatRoomId
                + "' really an IRC channel? It does not start with any of "
                + "the channel type symbols.");
        }

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
            LOGGER
                .trace("Issue join channel command to IRC library and wait for"
                    + " join operation to complete (un)successfully.");

            this.joined.put(chatRoomId, null);
            // TODO Refactor this ridiculous nesting of functions and
            // classes.
            this.irc.joinChannel(chatRoomId, password,
                new Callback<IRCChannel>()
                {

                    @Override
                    public void onSuccess(IRCChannel channel)
                    {
                        if (LOGGER.isTraceEnabled())
                        {
                            LOGGER
                                .trace("Started callback for successful join "
                                    + "of channel '" + chatroom.getIdentifier()
                                    + "'.");
                        }
                        boolean isRequestedChatRoom = channel.getName()
                            .equalsIgnoreCase(chatRoomId);
                        synchronized (joinSignal)
                        {
                            if (!isRequestedChatRoom)
                            {
                                // We joined another chat room than the one we
                                // requested initially.
                                if (LOGGER.isTraceEnabled())
                                {
                                    LOGGER
                                        .trace("Callback for successful join "
                                            + "finished prematurely since we "
                                            + "got forwarded from '"
                                            + chatRoomId
                                            + "' to '"
                                            + channel.getName()
                                            + "'. Joining of forwarded channel "
                                            + "gets handled by Server Listener "
                                            + "since that channel was not "
                                            + "announced.");
                                }
                                // Remove original chat room id from joined-list
                                // since we aren't actually attempting to join
                                // this room anymore.
                                IrcStack.this.joined.remove(chatRoomId);
                                IrcStack.this.provider
                                    .getMUC()
                                    .fireLocalUserPresenceEvent(
                                        chatroom,
                                        LocalUserChatRoomPresenceChangeEvent
                                            .LOCAL_USER_JOIN_FAILED,
                                        "We got forwarded to channel '"
                                            + channel.getName() + "'.");
                                // Notify waiting threads of finished execution.
                                joinSignal.setDone();
                                joinSignal.notifyAll();
                                // The channel that we were forwarded to will be
                                // handled by the Server Listener, since the
                                // channel join was unannounced, and we are done
                                // here.
                                return;
                            }

                            try
                            {
                                IrcStack.this.joined.put(chatRoomId, chatroom);
                                IrcStack.this.irc
                                    .addListener(new ChatRoomListener(
                                        chatroom));
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
                                IrcStack.this.provider
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
                    public void onFailure(Exception e)
                    {
                        LOGGER.trace("Started callback for failed attempt to "
                            + "join channel '" + chatRoomId + "'.");
                        synchronized (joinSignal)
                        {
                            try
                            {
                                IrcStack.this.joined.remove(chatRoomId);
                                IrcStack.this.provider
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
                                        .trace("Finished callback for failed "
                                            + "attempt to join channel '"
                                            + chatRoomId
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
                // TODO what should we do with this? Maybe store in
                // joinSignal if there's nothing else?
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
    public void leave(ChatRoomIrcImpl chatroom)
    {
        LOGGER.trace("Leaving chat room '" + chatroom.getIdentifier() + "'.");

        // You only actually join non-private chat rooms, so only these ones
        // need to be left.
        leave(chatroom.getIdentifier());
    }

    /**
     * Part from a joined chat room.
     * 
     * @param chatRoomName The chat room to part from.
     */
    private void leave(String chatRoomName)
    {
        if (this.connectionState == null || !this.connectionState.isConnected())
            return;

        try
        {
            this.irc.leaveChannel(chatRoomName);
        }
        catch (ApiException e)
        {
            LOGGER.warn("exception occurred while leaving channel", e);
        }
    }

    public void banParticipant(ChatRoomIrcImpl chatroom, ChatRoomMember member,
        String reason)
    {
        // TODO Implement this.
    }

    /**
     * Kick channel member.
     * 
     * @param chatroom channel to kick from
     * @param member member to kick
     * @param reason kick message to deliver
     */
    public void kickParticipant(ChatRoomIrcImpl chatroom,
        ChatRoomMember member, String reason)
    {
        this.irc.kick(chatroom.getIdentifier(), member.getContactAddress(),
            reason);
    }

    /**
     * Issue invite command to IRC server.
     * 
     * @param memberId member to invite
     * @param chatroom channel to invite to
     */
    public void invite(String memberId, ChatRoomIrcImpl chatroom)
    {
        this.irc.rawMessage("INVITE " + memberId + " "
            + chatroom.getIdentifier());
    }

    /**
     * Send a command to the IRC server.
     * 
     * @param chatroom the chat room
     * @param command the command message
     */
    public void command(ChatRoomIrcImpl chatroom, String command)
    {
        String target;
        if (command.toLowerCase().startsWith("/msg "))
        {
            command = command.substring(5);
            int endOfNick = command.indexOf(' ');
            if (endOfNick == -1)
            {
                throw new IllegalArgumentException("Invalid private message "
                    + "format. Message was not sent.");
            }
            target = command.substring(0, endOfNick);
            command = command.substring(endOfNick + 1);
        }
        else
        {
            target = chatroom.getIdentifier();
        }
        this.irc.message(target, command);
    }

    /**
     * Send an IRC message.
     * 
     * @param chatroom The chat room to send the message to.
     * @param message The message to send.
     */
    public void message(ChatRoomIrcImpl chatroom, String message)
    {
        String target = chatroom.getIdentifier();
        this.irc.message(target, message);
    }

    /**
     * Send an IRC message.
     * 
     * @param contact The contact to send the message to.
     * @param message The message to send.
     */
    public void message(Contact contact, String message)
    {
        String target = contact.getAddress();
        this.irc.message(target, message);
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

        OperationSetPersistentPresenceIrcImpl opSetPersistentPresence =
            this.provider.getPersistentPresence();
        ContactGroupIrcImpl nonPersistentGroup =
            opSetPersistentPresence.getNonPersistentGroup();
        for (IRCUser user : channel.getUsers())
        {
            // TODO Correctly gather active member statuses and choose strongest
            // normal + voice + half-ops (a.k.a. moderator) + ops
            ChatRoomMemberRole role = ChatRoomMemberRole.SILENT_MEMBER;
            for (IRCUserStatus status : channel.getStatusesForUser(user))
            {
                role = convertMemberMode(status.getChanModeType().charValue());
            }

            if (this.getNick().equals(user.getNick()))
            {
                chatRoom.prepUserRole(role);
            }
            ChatRoomMemberIrcImpl member =
                new ChatRoomMemberIrcImpl(this.provider, chatRoom,
                    user.getNick(), role);
            chatRoom.addChatRoomMember(member.getContactAddress(), member);

            // FIXME working on persistent presence
            // Prepare Contact and MetaContact
            // ContactIrcImpl sourceContact =
            // opSetPersistentPresence.createVolatileContact(
            // member.getContactAddress(), true);
            // opSetPersistentPresence.fireSubscriptionEvent(sourceContact,
            // nonPersistentGroup, SubscriptionEvent.SUBSCRIPTION_CREATED);
        }
    }

    /**
     * Convert a member mode character to a ChatRoomMemberRole instance.
     * 
     * @param modeSymbol The member mode character.
     * @return Return the instance of ChatRoomMemberRole corresponding to the
     *         member mode character.
     */
    private static ChatRoomMemberRole convertMemberMode(char modeSymbol)
    {
        return Mode.bySymbol(modeSymbol).getRole();
    }

    /**
     * A listener for server-level messages (any messages that are related to
     * the server, the connection, that are not related to any chatroom in
     * particular) or that are personal message from user to local user.
     */
    private class ServerListener
        extends VariousMessageListenerAdapter
    {
        /**
         * Print out server notices for debugging purposes and for simply
         * keeping track of the connections.
         * 
         * @param msg the server notice
         */
        @Override
        public void onServerNotice(ServerNotice msg)
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
        public void onServerNumericMessage(ServerNumericMessage msg)
        {
            if (LOGGER.isDebugEnabled())
            {
                LOGGER.debug("NUM MSG: " + msg.getNumericCode() + ": "
                    + msg.getText());
            }

            Integer code = msg.getNumericCode();
            if (code == null)
            {
                LOGGER.debug("No 'code' in numeric message event.");
                return;
            }

            switch (code.intValue())
            {
            case IRCServerNumerics.CHANNEL_NICKS_END_OF_LIST:
                String text = msg.getText();
                String channelName = text.substring(0, text.indexOf(' '));
                final ChatRoomIrcImpl chatRoom;
                final IRCChannel channel;
                synchronized (IrcStack.this.joined)
                {
                    // Synchronize the section that checks then adds a chat
                    // room. This way we can be sure that there are no 2
                    // simultaneous creation events.
                    if (IrcStack.this.joined.containsKey(channelName))
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
                        IrcStack.this.connectionState
                            .getChannelByName(channelName);
                    chatRoom =
                        new ChatRoomIrcImpl(channelName, IrcStack.this.provider);
                    IrcStack.this.joined.put(channelName, chatRoom);
                }
                IrcStack.this.irc.addListener(new ChatRoomListener(chatRoom));
                try
                {
                    IrcStack.this.provider.getMUC()
                        .openChatRoomWindow(chatRoom);
                }
                catch (NullPointerException e)
                {
                    LOGGER.error("failed to open chat room window", e);
                }
                IrcStack.this.prepareChatRoom(chatRoom, channel);
                IrcStack.this.provider.getMUC().fireLocalUserPresenceEvent(
                    chatRoom,
                    LocalUserChatRoomPresenceChangeEvent.LOCAL_USER_JOINED,
                    null);
                LOGGER.trace("Unannounced join of chat room '" + channelName
                    + "' completed.");
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
        public void onError(ErrorMessage msg)
        {
            if (LOGGER.isDebugEnabled())
            {
                LOGGER
                    .debug("ERROR: " + msg.getSource() + ": " + msg.getText());
            }
            if (IrcStack.this.connectionState != null)
            {
                if (!IrcStack.this.connectionState.isConnected())
                {
                    IrcStack.this.provider
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
         * TODO Move to using MessageIrcImpl implementation.
         * 
         * @param msg the private message
         */
        @Override
        public void onUserPrivMessage(final UserPrivMsg msg)
        {
            final String user = msg.getSource().getNick();
            final String text = Utils.parse(msg.getText());
            IrcMessage message =
                new OperationSetBasicInstantMessagingIrcImpl.IrcMessage(text);
            Contact from =
                IrcStack.this.provider.getPersistentPresence().findContactByID(
                    user);
            IrcStack.this.provider.getBasicInstantMessaging()
                .fireMessageReceived(message, from);
        }

        /**
         * Upon receiving a user notice message from a user, deliver that to an
         * instant messaging contact.
         * 
         * TODO Create special IRC Notice Message class.
         * 
         * TODO Move to using MessageIrcImpl message class.
         * 
         * @param msg user notice message
         */
        @Override
        public void onUserNotice(UserNotice msg)
        {
            final String user = msg.getSource().getNick();
            final String text = Utils.parse(msg.getText());
            // TODO distinguish between notice and normal message in formatting?
            IrcMessage message =
                new OperationSetBasicInstantMessagingIrcImpl.IrcMessage(text);
            Contact from =
                IrcStack.this.provider.getPersistentPresence().findContactByID(
                    user);
            IrcStack.this.provider.getBasicInstantMessaging()
                .fireMessageReceived(message, from);
        }

        /**
         * Upon receiving a user notice message from a user, deliver that to an
         * instant messaging contact.
         * 
         * TODO Create special IRC Notice Message class.
         * 
         * TODO Move to using MessageIrcImpl message class.
         * 
         * @param msg user notice message
         */
        @Override
        public void onUserAction(UserActionMsg msg)
        {
            final String user = msg.getSource().getNick();
            final String text = Utils.parse(msg.getText());
            // TODO distinguish between action and normal message in formatting?
            IrcMessage message =
                new OperationSetBasicInstantMessagingIrcImpl.IrcMessage(text);
            Contact from =
                IrcStack.this.provider.getPersistentPresence().findContactByID(
                    user);
            IrcStack.this.provider.getBasicInstantMessaging()
                .fireMessageReceived(message, from);
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
    private class ChatRoomListener
        extends VariousMessageListenerAdapter
    {
        /**
         * IRC error code for case where user is not joined to that channel.
         */
        private static final int IRC_ERR_NOTONCHANNEL = 442;

        /**
         * Chat room for which this listener is working.
         */
        private ChatRoomIrcImpl chatroom;

        /**
         * Constructor. Instantiate listener for the provided chat room.
         * 
         * @param chatroom
         */
        private ChatRoomListener(ChatRoomIrcImpl chatroom)
        {
            if (chatroom == null)
                throw new IllegalArgumentException("chatroom cannot be null");

            this.chatroom = chatroom;
        }

        /**
         * Event in case of topic change.
         * 
         * @param msg topic change message
         */
        @Override
        public void onTopicChange(TopicMessage msg)
        {
            if (!isThisChatRoom(msg.getChannelName()))
                return;

            this.chatroom.updateSubject(msg.getTopic().getValue());
        }

        /**
         * Event in case of channel mode changes.
         * 
         * @param msg channel mode message
         */
        @Override
        public void onChannelMode(ChannelModeMessage msg)
        {
            if (!isThisChatRoom(msg.getChannelName()))
                return;

            processModeMessage(msg);
        }

        /**
         * Event in case of channel join message.
         * 
         * @param msg channel join message
         */
        @Override
        public void onChannelJoin(ChanJoinMessage msg)
        {
            if (!isThisChatRoom(msg.getChannelName()))
                return;

            String user = msg.getSource().getNick();
            ChatRoomMemberIrcImpl member =
                new ChatRoomMemberIrcImpl(IrcStack.this.provider,
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
        public void onChannelPart(ChanPartMessage msg)
        {
            if (!isThisChatRoom(msg.getChannelName()))
                return;

            IRCUser user = msg.getSource();
            if (isMe(user))
            {
                leaveChatRoom();
                return;
            }
            
            String userNick = msg.getSource().getNick();
            ChatRoomMember member = this.chatroom.getChatRoomMember(userNick);
            try
            {
                // Possibility that 'member' is null (should be fixed now
                // that race condition in irc-api is fixed)
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

        /**
         * Some of the generic message are relevant to us, so keep an eye on
         * general numeric messages.
         * 
         * @param msg IRC server numeric message
         */
        public void onServerNumericMessage(ServerNumericMessage msg)
        {
            Integer code = msg.getNumericCode();
            if (code == null)
            {
                return;
            }
            String raw = msg.getText();
            if (code.intValue() == IRC_ERR_NOTONCHANNEL)
            {
                String channel = raw.substring(0, raw.indexOf(" "));
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
            }
        }

        /**
         * Event in case of channel kick.
         * 
         * @param msg channel kick message
         */
        @Override
        public void onChannelKick(ChannelKick msg)
        {
            if (!isThisChatRoom(msg.getChannelName()))
                return;

            String kickedUser = msg.getKickedNickname();
            ChatRoomMember kickedMember =
                this.chatroom.getChatRoomMember(kickedUser);
            String user = msg.getSource().getNick();
            if (kickedMember != null)
            {
                ChatRoomMember kicker = this.chatroom.getChatRoomMember(user);
                this.chatroom.fireMemberPresenceEvent(kickedMember, kicker,
                    ChatRoomMemberPresenceChangeEvent.MEMBER_KICKED,
                    msg.getText());
            }

            if (isMe(kickedUser))
            {
                IrcStack.this.irc.deleteListener(this);
                IrcStack.this.joined.remove(this.chatroom.getIdentifier());
                IrcStack.this.provider.getMUC().fireLocalUserPresenceEvent(
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
        public void onUserQuit(QuitMessage msg)
        {
            String user = msg.getSource().getNick();
            ChatRoomMember member = this.chatroom.getChatRoomMember(user);
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
        public void onNickChange(NickMessage msg)
        {
            if (msg == null)
                return;

            String oldNick = msg.getSource().getNick();
            String newNick = msg.getNewNick();

            ChatRoomMemberIrcImpl member =
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
        public void onChannelMessage(ChannelPrivMsg msg)
        {
            if (!isThisChatRoom(msg.getChannelName()))
                return;

            String text = Utils.parse(msg.getText());            
            MessageIrcImpl message =
                new MessageIrcImpl(text, "text/html", "UTF-8", null);
            ChatRoomMemberIrcImpl member =
                new ChatRoomMemberIrcImpl(IrcStack.this.provider,
                    this.chatroom, msg.getSource().getNick(),
                    ChatRoomMemberRole.MEMBER);
            this.chatroom.fireMessageReceivedEvent(message, member, new Date(),
                ChatRoomMessageReceivedEvent.CONVERSATION_MESSAGE_RECEIVED);
        }

        /**
         * Event in case of channel action message arrival.
         * 
         * TODO Create special IRC Action Message class.
         * 
         * @param msg channel action message
         */
        @Override
        public void onChannelAction(ChannelActionMsg msg)
        {
            if (!isThisChatRoom(msg.getChannelName()))
                return;

            String userNick = msg.getSource().getNick();
            ChatRoomMemberIrcImpl member =
                new ChatRoomMemberIrcImpl(IrcStack.this.provider,
                    this.chatroom, userNick, ChatRoomMemberRole.MEMBER);
            String text =
                "<b>*" + userNick + "</b> " + Utils.parse(msg.getText());
            MessageIrcImpl message =
                new MessageIrcImpl(text, "text/html", "UTF-8", null);
            this.chatroom.fireMessageReceivedEvent(message, member, new Date(),
                ChatRoomMessageReceivedEvent.CONVERSATION_MESSAGE_RECEIVED);
        }

        /**
         * Event in case of channel notice message arrival.
         * 
         * TODO Create special IRC Notice Message class.
         * 
         * @param msg channel notice message
         */
        @Override
        public void onChannelNotice(ChannelNotice msg)
        {
            if (!isThisChatRoom(msg.getChannelName()))
                return;

            String userNick = msg.getSource().getNick();
            ChatRoomMemberIrcImpl member =
                new ChatRoomMemberIrcImpl(IrcStack.this.provider,
                    this.chatroom, userNick, ChatRoomMemberRole.MEMBER);
            String text =
                "<i>" + userNick + "</i>: " + Utils.parse(msg.getText());
            MessageIrcImpl message =
                new MessageIrcImpl(text, "text/html", "UTF-8", null);
            this.chatroom.fireMessageReceivedEvent(message, member, new Date(),
                ChatRoomMessageReceivedEvent.CONVERSATION_MESSAGE_RECEIVED);
        }

        /**
         * Leave this chat room.
         */
        private void leaveChatRoom()
        {
            IrcStack.this.irc.deleteListener(this);
            IrcStack.this.joined.remove(this.chatroom.getIdentifier());
            IrcStack.this.provider.getMUC().fireLocalUserPresenceEvent(
                this.chatroom,
                LocalUserChatRoomPresenceChangeEvent.LOCAL_USER_LEFT, null);
        }
        
        /**
         * Process mode changes.
         * 
         * @param msg raw mode message
         */
        private void processModeMessage(ChannelModeMessage msg)
        {
            ChatRoomMemberIrcImpl sourceMember = extractChatRoomMember(msg);

            ModeParser parser = new ModeParser(msg.getModeStr());
            for (ModeEntry mode : parser.getModes())
            {
                switch (mode.getMode())
                {
                case OWNER:
                    String ownerUserName = mode.getParams()[0];
                    if (isMe(ownerUserName))
                    {
                        ChatRoomLocalUserRoleChangeEvent event;
                        if (mode.isAdded())
                        {
                            event =
                                new ChatRoomLocalUserRoleChangeEvent(
                                    this.chatroom,
                                    ChatRoomMemberRole.SILENT_MEMBER,
                                    ChatRoomMemberRole.OWNER, false);
                        }
                        else
                        {
                            event =
                                new ChatRoomLocalUserRoleChangeEvent(
                                    this.chatroom, ChatRoomMemberRole.OWNER,
                                    ChatRoomMemberRole.SILENT_MEMBER, false);
                        }
                        this.chatroom.fireLocalUserRoleChangedEvent(event);
                    }
                    ChatRoomMember owner =
                        this.chatroom.getChatRoomMember(mode.getParams()[0]);
                    if (owner != null)
                    {
                        if (mode.isAdded())
                        {
                            this.chatroom.fireMemberRoleEvent(owner,
                                ChatRoomMemberRole.OWNER);
                        }
                        else
                        {
                            this.chatroom.fireMemberRoleEvent(owner,
                                ChatRoomMemberRole.SILENT_MEMBER);
                        }
                    }
                    break;
                case OPERATOR:
                    String opUserName = mode.getParams()[0];
                    if (isMe(opUserName))
                    {
                        ChatRoomLocalUserRoleChangeEvent event;
                        if (mode.isAdded())
                        {
                            event =
                                new ChatRoomLocalUserRoleChangeEvent(
                                    this.chatroom,
                                    ChatRoomMemberRole.SILENT_MEMBER,
                                    ChatRoomMemberRole.ADMINISTRATOR, false);
                        }
                        else
                        {
                            event =
                                new ChatRoomLocalUserRoleChangeEvent(
                                    this.chatroom,
                                    ChatRoomMemberRole.ADMINISTRATOR,
                                    ChatRoomMemberRole.SILENT_MEMBER, false);
                        }
                        this.chatroom.fireLocalUserRoleChangedEvent(event);
                    }
                    ChatRoomMember op =
                        this.chatroom.getChatRoomMember(opUserName);
                    if (op != null)
                    {
                        if (mode.isAdded())
                        {
                            this.chatroom.fireMemberRoleEvent(op,
                                ChatRoomMemberRole.ADMINISTRATOR);
                        }
                        else
                        {
                            this.chatroom.fireMemberRoleEvent(op,
                                ChatRoomMemberRole.SILENT_MEMBER);
                        }
                    }
                    break;
                case VOICE:
                    String voiceUserName = mode.getParams()[0];
                    if (isMe(voiceUserName))
                    {
                        ChatRoomLocalUserRoleChangeEvent event;
                        if (mode.isAdded())
                        {
                            event =
                                new ChatRoomLocalUserRoleChangeEvent(
                                    this.chatroom,
                                    ChatRoomMemberRole.SILENT_MEMBER,
                                    ChatRoomMemberRole.MEMBER, false);
                        }
                        else
                        {
                            event =
                                new ChatRoomLocalUserRoleChangeEvent(
                                    this.chatroom, ChatRoomMemberRole.MEMBER,
                                    ChatRoomMemberRole.SILENT_MEMBER, false);
                        }
                        this.chatroom.fireLocalUserRoleChangedEvent(event);
                    }
                    ChatRoomMember voice =
                        this.chatroom.getChatRoomMember(voiceUserName);
                    if (voice != null)
                    {
                        if (mode.isAdded())
                        {
                            this.chatroom.fireMemberRoleEvent(voice,
                                ChatRoomMemberRole.MEMBER);
                        }
                        else
                        {
                            this.chatroom.fireMemberRoleEvent(voice,
                                ChatRoomMemberRole.SILENT_MEMBER);
                        }
                    }
                    break;
                case LIMIT:
                    MessageIrcImpl limitMessage;
                    if (mode.isAdded())
                    {
                        try
                        {
                            limitMessage =
                                new MessageIrcImpl("channel limit set to "
                                    + Integer.parseInt(mode.getParams()[0])
                                    + " by "
                                    + (sourceMember.getContactAddress()
                                        .length() == 0 ? "server"
                                        : sourceMember.getContactAddress()),
                                    "text/plain", "UTF-8", null);
                        }
                        catch (NumberFormatException e)
                        {
                            LOGGER.warn("server sent incorrect limit: "
                                + "limit is not a number", e);
                            break;
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
                                    + (sourceMember.getContactAddress()
                                        .length() == 0 ? "server"
                                        : sourceMember.getContactAddress()),
                                "text/plain", "UTF-8", null);
                    }
                    this.chatroom.fireMessageReceivedEvent(limitMessage,
                        sourceMember, new Date(),
                        ChatRoomMessageReceivedEvent.SYSTEM_MESSAGE_RECEIVED);
                    break;
                case BAN:
                    MessageIrcImpl banMessage =
                        new MessageIrcImpl(
                            "channel ban mask was "
                                + (mode.isAdded() ? "added" : "removed")
                                + ": "
                                + mode.getParams()[0]
                                + " by "
                                + (sourceMember.getContactAddress()
                                    .length() == 0 ? "server"
                                    : sourceMember.getContactAddress()),
                            "text/plain", "UTF-8", null);
                    this.chatroom.fireMessageReceivedEvent(banMessage,
                        sourceMember, new Date(),
                        ChatRoomMessageReceivedEvent.SYSTEM_MESSAGE_RECEIVED);
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
         * Extract chat room member identifier from message.
         * 
         * @param msg raw mode message
         * @return returns member instance
         */
        private ChatRoomMemberIrcImpl extractChatRoomMember(
            ChannelModeMessage msg)
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
                    new ChatRoomMemberIrcImpl(IrcStack.this.provider,
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
        private boolean isThisChatRoom(String chatRoomName)
        {
            return this.chatroom.getIdentifier().equalsIgnoreCase(chatRoomName);
        }

        /**
         * Test whether the source user is this user.
         * 
         * @param user the source user
         * @return returns true if this use, or false otherwise
         */
        private boolean isMe(IRCUser user)
        {
            return IrcStack.this.connectionState.getNickname().equals(
                user.getNick());
        }

        /**
         * Test whether the user nick is this user.
         * 
         * @param name nick of the user
         * @return returns true if so, false otherwise
         */
        private boolean isMe(String name)
        {
            return IrcStack.this.connectionState.getNickname().equals(name);
        }
    }

    /**
     * Special listener that processes LIST replies and signals once the list is
     * completely filled.
     */
    private static class ChannelListListener
        extends VariousMessageListenerAdapter
    {
        /**
         * Reference to the IRC API instance.
         */
        private final IRCApi api;

        /**
         * Reference to the provided list instance.
         */
        private Result<List<String>, Exception> signal;

        /**
         * Constructor for channel list listener.
         * @param api irc-api library instance
         * @param list signal for sync signaling
         */
        private ChannelListListener(IRCApi api,
            Result<List<String>, Exception> list)
        {
            if (api == null)
                throw new IllegalArgumentException(
                    "IRC api instance cannot be null");
            this.api = api;
            this.signal = list;
        }

        /**
         * Act on LIST messages: 321 RPL_LISTSTART, 322 RPL_LIST, 323
         * RPL_LISTEND
         * 
         * Clears the list upon starting. All received channels are added to the
         * list. Upon receiving RPL_LISTEND finalize the list and signal the
         * waiting thread that it can continue processing the list.
         */
        @Override
        public void onServerNumericMessage(ServerNumericMessage msg)
        {
            if (this.signal.isDone())
                return;
            
            switch (msg.getNumericCode())
            {
            case 321:
                synchronized (this.signal)
                {
                    this.signal.getValue().clear();
                }
                break;
            case 322:
                String channel = parse(msg.getText());
                if (channel != null)
                {
                    synchronized (this.signal)
                    {
                        this.signal.getValue().add(channel);
                    }
                }
                break;
            case 323:
                synchronized (this.signal)
                {
                    // Done collecting channels. Remove listener and then we're
                    // done.
                    this.api.deleteListener(this);
                    this.signal.setDone();
                    this.signal.notifyAll();
                }
                break;
            // TODO Add support for REPLY 416: LIST :output too large, truncated
            default:
                break;
            }
        }

        /**
         * Parse an IRC server response RPL_LIST. Extract the channel name.
         * 
         * @param text raw server response
         * @return returns the channel name
         */
        private String parse(String text)
        {
            int endOfChannelName = text.indexOf(' ');
            if (endOfChannelName == -1)
                return null;
            // Create a new string to make sure that the original (larger)
            // strings can be GC'ed.
            return new String(text.substring(0, endOfChannelName));
        }
    }

    /**
     * Container for storing server parameters.
     */
    private static class ServerParameters
        implements IServerParameters
    {

        /**
         * Nick name.
         */
        private String nick;

        /**
         * Alternative nick names.
         */
        private List<String> alternativeNicks = new ArrayList<String>();

        /**
         * Real name.
         */
        private String real;

        /**
         * Ident.
         */
        private String ident;

        /**
         * IRC server.
         */
        private IRCServer server;
        
        /**
         * Construct ServerParameters instance.
         * @param nickName nick name
         * @param realName real name
         * @param ident ident
         * @param server IRC server instance
         */
        private ServerParameters(String nickName, String realName,
            String ident, IRCServer server)
        {
            this.nick = checkNick(nickName);
            this.alternativeNicks.add(nickName + "_");
            this.alternativeNicks.add(nickName + "__");
            this.alternativeNicks.add(nickName + "___");
            this.alternativeNicks.add(nickName + "____");
            for (int i = 1; i < 10; i++)
            {
                this.alternativeNicks.add(nickName + i);
            }
            this.real = realName;
            this.ident = ident;
            this.server = server;
        }

        /**
         * Get nick name.
         * 
         * @return returns nick name
         */
        @Override
        public String getNickname()
        {
            return this.nick;
        }

        /**
         * Set new nick name.
         * 
         * @param nick nick name
         */
        public void setNickname(String nick)
        {
            this.nick = checkNick(nick);
        }

        /**
         * Verify nick name.
         * 
         * @param nick nick name
         * @return returns nick name
         * @throws IllegalArgumentException throws
         *             <tt>IllegalArgumentException</tt> if an invalid nick name
         *             is provided.
         */
        private String checkNick(String nick)
        {
            if (nick == null)
                throw new IllegalArgumentException(
                    "a nick name must be provided");
            if (nick.startsWith("#") || nick.startsWith("&"))
                throw new IllegalArgumentException(
                    "the nick name must not start with '#' or '&' "
                        + "since this is reserved for IRC channels");
            return nick;
        }

        /**
         * Get alternative nick names.
         * 
         * @return returns list of alternatives
         */
        @Override
        public List<String> getAlternativeNicknames()
        {
            return this.alternativeNicks;
        }

        /**
         * Get ident string.
         * 
         * @return returns ident
         */
        @Override
        public String getIdent()
        {
            return this.ident;
        }

        /**
         * Get real name
         * 
         * @return returns real name
         */
        @Override
        public String getRealname()
        {
            return this.real;
        }

        /**
         * Get server
         * 
         * @return returns server instance
         */
        @Override
        public IRCServer getServer()
        {
            return this.server;
        }

        /**
         * Set server instance.
         * 
         * @param server IRC server instance
         */
        public void setServer(IRCServer server)
        {
            if (server == null)
                throw new IllegalArgumentException("server cannot be null");
            
            this.server = server;
        }
    }

    /**
     * Simplest possible container that we can use for locking while we're
     * checking/modifying the contents.
     * 
     * @param <T> The type of instance to store in the container
     */
    private static class Container<T>
    {
        /**
         * The stored instance. (Can be null)
         */
        private T instance;

        /**
         * Time of stored instance.
         */
        private long time;

        /**
         * Constructor that immediately sets the instance.
         * 
         * @param instance the instance to set
         */
        private Container(T instance)
        {
            this.instance = instance;
            this.time = System.nanoTime();
        }

        /**
         * Conditionally get the stored instance. Get the instance when time
         * difference is within specified bound. Otherwise return null.
         * 
         * @param bound maximum time difference that is allowed.
         * @return returns instance if within bounds, or null otherwise
         */
        public T get(long bound)
        {
            if (System.nanoTime() - this.time > bound)
            {
                return null;
            }
            return this.instance;
        }

        /**
         * Set an instance
         * 
         * @param instance the instance
         */
        public void set(T instance)
        {
            this.instance = instance;
            this.time = System.nanoTime();
        }
    }
}
