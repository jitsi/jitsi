/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.irc;

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
 * An implementation of IRC using the irc-api library.
 * 
 * @author Danny van Heumen
 * 
 * TODO Make irc-api OSGi aware.
 */
public class IrcStack
{
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
        return (this.irc != null && this.connectionState != null && this.connectionState
            .isConnected());
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

        // A container for storing the exception if connecting fails.
        final Exception[] exceptionContainer = new Exception[1];

        this.irc = new IRCApiImpl(true);
        // FIXME Currently, the secure connection is created by
        // explicitly creating an SSLContext for 'SSL'. According
        // to Ingo (in a mailing list conversation) it is better to
        // use the CertificateService for this. This should be
        // implemented in the irc-api library, though.
        this.params.setServer(new IRCServer(host, port, password,
            secureConnection));
        synchronized (this.irc)
        {
            // register a server listener in order to catch server and
            // cross-/multi-channel messages
            this.irc.addListener(new ServerListener());
            // start connecting to the specified server ...
            // TODO Catch IOException/SocketException in case of early failure
            // in call to connect()
            this.irc.connect(this.params, new Callback<IIRCState>()
            {

                @Override
                public void onSuccess(IIRCState state)
                {
                    synchronized (IrcStack.this.irc)
                    {
                        LOGGER.trace("IRC connected successfully!");
                        IrcStack.this.connectionState = state;
                        IrcStack.this.irc.notifyAll();
                    }
                }

                @Override
                public void onFailure(Exception e)
                {
                    synchronized (IrcStack.this.irc)
                    {
                        LOGGER.trace("IRC connection FAILED! ("
                            + e.getMessage() + ")");
                        exceptionContainer[0] = e;
                        IrcStack.this.connectionState = null;
                        IrcStack.this.irc.notifyAll();
                    }
                }
            });

            try
            {
                // wait while the irc connection is being established ...
                LOGGER.trace("Waiting for the connection to be established ...");
                // TODO Implement connection timeout and a way to recognize that
                // the timeout occurred.
                this.irc.wait();
                if (this.connectionState != null
                    && this.connectionState.isConnected())
                {
                    this.provider
                        .setCurrentRegistrationState(RegistrationState.REGISTERED);
                }
                else
                {
                    this.provider
                        .setCurrentRegistrationState(RegistrationState.UNREGISTERED);

                    if (exceptionContainer[0] != null)
                    {
                        // If an exception happens to be available that explains
                        // the connection problem, throw it.
                        throw exceptionContainer[0];
                    }
                }
            }
            catch (InterruptedException e)
            {
                this.provider
                    .setCurrentRegistrationState(RegistrationState.UNREGISTERED);
                throw e;
            }
        }
    }

    /**
     * Disconnect from the IRC server
     */
    public void disconnect()
    {
        if (this.connectionState == null && this.irc == null)
            return;
        
        synchronized(this.joined)
        {
            // Leave all joined channels.
            for (ChatRoomIrcImpl channel : this.joined.values())
            {
                leave(channel);
            }
        }
        synchronized(this.irc)
        {
            // Disconnect and clean up
            this.irc.disconnect();
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
        if (isConnected() == false)
            throw new IllegalStateException(
                "Please connect to an IRC server first.");
        if (chatroom == null)
            throw new IllegalArgumentException("Cannot have a null chatroom");
        this.irc
            .changeTopic(chatroom.getName(), subject == null ? "" : subject);
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
        return this.joined.containsKey(chatroom.getIdentifier());
    }

    /**
     * Get a list of channels available on the IRC server.
     * 
     * @return List of available channels.
     */
    public List<String> getServerChatRoomList()
    {
        LOGGER.trace("Start retrieve server chat room list.");
        // FIXME Currently, not using an API library method for listing
        // channels, since it isn't available.
        synchronized (this.channellist)
        {
            if (this.channellist.instance == null)
            {
                List<String> list = new LinkedList<String>();
                synchronized (list)
                {
                    try
                    {
                        this.irc.addListener(new ChannelListListener(this.irc,
                            list));
                        this.irc.rawMessage("LIST");
                        LOGGER.trace("Start waiting for list ...");
                        list.wait();
                        LOGGER.trace("Done waiting for list.");
                    }
                    catch (InterruptedException e)
                    {
                        LOGGER.trace("INTERRUPTED while waiting for list.");
                        e.printStackTrace();
                    }
                }
                this.channellist.instance = list;
            }
            LOGGER.trace("Finished retrieve server chat room list.");
            return Collections.unmodifiableList(this.channellist.instance);
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
        if (isConnected() == false)
            throw new IllegalStateException(
                "Please connect to an IRC server first");
        if (chatroom == null)
            throw new IllegalArgumentException("chatroom cannot be null");
        if (password == null)
            throw new IllegalArgumentException("password cannot be null");
        if (this.joined.containsKey(chatroom.getIdentifier()) || chatroom.isPrivate())
        {
            // If we already joined this particular chatroom or if it is a
            // private chat room (i.e. message from one user to another), no
            // further action is required.
            return;
        }

        LOGGER.trace("Start joining channel " + chatroom.getIdentifier());
        final Exception[] joinSignal = new Exception[1];
        synchronized (joinSignal)
        {
            try
            {
                LOGGER
                    .trace("Issue join channel command to IRC library and wait for join operation to complete (un)successfully.");
                // TODO Refactor this ridiculous nesting of functions and
                // classes.
                this.irc.joinChannel(chatroom.getIdentifier(), password,
                    new Callback<IRCChannel>()
                    {

                        @Override
                        public void onSuccess(IRCChannel channel)
                        {
                            LOGGER
                                .trace("Started callback for successful join of channel '"
                                    + chatroom.getIdentifier() + "'.");
                            ChatRoomIrcImpl actualChatRoom = chatroom;
                            synchronized (joinSignal)
                            {
                                try
                                {
                                    if (channel.getName().equals(
                                        actualChatRoom.getIdentifier()) == false)
                                    {
                                        // If the channel name is not the
                                        // original chat room name, then we have
                                        // been forwarded.
                                        actualChatRoom =
                                            new ChatRoomIrcImpl(channel
                                                .getName(),
                                                IrcStack.this.provider);
                                        MessageIrcImpl message =
                                            new MessageIrcImpl(
                                                "Forwarding to channel "
                                                    + channel.getName(),
                                                "text/plain", "UTF-8", null);
                                        IrcStack.this.provider.getMUC()
                                            .registerChatRoomInstance(
                                                actualChatRoom);
                                        chatroom
                                            .fireMessageReceivedEvent(
                                                message,
                                                null,
                                                new Date(),
                                                MessageReceivedEvent.SYSTEM_MESSAGE_RECEIVED);
                                    }

                                    IrcStack.this.joined.put(
                                        actualChatRoom.getIdentifier(),
                                        actualChatRoom);
                                    IrcStack.this.irc
                                        .addListener(new ChatRoomListener(
                                            actualChatRoom));
                                    IRCTopic topic = channel.getTopic();
                                    actualChatRoom.updateSubject(topic
                                        .getValue());

                                    for (IRCUser user : channel.getUsers())
                                    {
                                        ChatRoomMemberRole role =
                                            ChatRoomMemberRole.SILENT_MEMBER;
                                        Set<IRCUserStatus> statuses =
                                            channel.getStatusesForUser(user);
                                        for (IRCUserStatus status : statuses)
                                        {
                                            role =
                                                convertMemberMode(status
                                                    .getChanModeType()
                                                    .charValue());
                                        }

                                        if (IrcStack.this.getNick().equals(
                                            user.getNick()))
                                        {
                                            actualChatRoom.prepUserRole(role);
                                        }
                                        else
                                        {
                                            ChatRoomMemberIrcImpl member =
                                                new ChatRoomMemberIrcImpl(
                                                    IrcStack.this.provider,
                                                    actualChatRoom, user
                                                        .getNick(), role);
                                            actualChatRoom.addChatRoomMember(
                                                member.getContactAddress(),
                                                member);
                                        }
                                    }
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
                                            actualChatRoom,
                                            LocalUserChatRoomPresenceChangeEvent.LOCAL_USER_JOINED,
                                            null);
                                    LOGGER
                                        .trace("Finished successful join callback for channel '"
                                            + chatroom.getIdentifier()
                                            + "'. Waking up original thread.");
                                    // Notify waiting threads of finished
                                    // execution.
                                    joinSignal.notifyAll();
                                }
                            }
                        }

                        @Override
                        public void onFailure(Exception e)
                        {
                            LOGGER
                                .trace("Started callback for failed attempt to join channel '"
                                    + chatroom.getIdentifier() + "'.");
                            // TODO how should we communicate a failed attempt
                            // at joining the channel? (System messages don't
                            // seem to show if there is no actual chat room
                            // presence.)
                            synchronized (joinSignal)
                            {
                                try
                                {
                                    joinSignal[0] = e;
                                    MessageIrcImpl message =
                                        new MessageIrcImpl(
                                            "Failed to join channel "
                                                + chatroom.getIdentifier()
                                                + " (" + e.getMessage() + ")",
                                            "text/plain", "UTF-8",
                                            "Failed to join");
                                    chatroom
                                        .fireMessageReceivedEvent(
                                            message,
                                            null,
                                            new Date(),
                                            MessageReceivedEvent.SYSTEM_MESSAGE_RECEIVED);
                                }
                                finally
                                {
                                    LOGGER
                                        .trace("Finished callback for failed attempt to join channel '"
                                            + chatroom.getIdentifier()
                                            + "'. Waking up original thread.");
                                    // Notify waiting threads of finished
                                    // execution
                                    joinSignal.notifyAll();
                                }
                            }
                        }
                    });
                // Wait until async channel join operation has finished.
                joinSignal.wait();
                LOGGER
                    .trace("Finished waiting for join operation for channel '"
                        + chatroom.getIdentifier() + "' to complete.");
                // TODO How to handle 480 (+j): Channel throttle exceeded?
            }
            catch (InterruptedException e)
            {
                // TODO what should we do with this? Maybe store in joinSignal
                // if there's nothing else?
                e.printStackTrace();
            }

            if (joinSignal[0] != null)
            {
                // in case an exception occurred during join process
                throw new OperationFailedException(joinSignal[0].getMessage(),
                    OperationFailedException.CHAT_ROOM_NOT_JOINED,
                    joinSignal[0]);
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
        if (chatroom.isPrivate() == false)
        {
            // You only actually join non-private chat rooms, so only these ones
            // need to be left.
            leave(chatroom.getIdentifier());
        }
    }

    /**
     * Part from a joined chat room.
     * 
     * @param chatRoomName The chat room to part from.
     */
    private void leave(String chatRoomName)
    {
        this.irc.leaveChannel(chatRoomName);
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
                throw new IllegalArgumentException("Invalid private message format. Message was not sent.");
            }
            target = command.substring(0,  endOfNick);
            command = command.substring(endOfNick+1);
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
     * A listener for server-level messages (any messages that are related to the
     * server, the connection, that are not related to any chatroom in
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
            LOGGER.debug("NOTICE: " + ((ServerNotice) msg).getText());
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
            LOGGER.debug("NUM MSG: "
                + ((ServerNumericMessage) msg).getNumericCode() + ": "
                + ((ServerNumericMessage) msg).getText());
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
            LOGGER.debug("ERROR: " + msg.getSource() + ": "
                + msg.getText());
        }
        
        /**
         * Upon receiving a private message from a user, deliver that to a
         * private chat room and create one if it does not exist. We can ignore
         * normal chat rooms, since they each have their own ChatRoomListener
         * for managing chat room operations.
         * 
         * @param msg the private message
         */
        @Override
        public void onUserPrivMessage(UserPrivMsg msg)
        {
            // TODO handle special formatting/color control codes in message
            // (see also channel private messages)

            ChatRoomIrcImpl chatroom = null;
            String user = msg.getSource().getNick();
            String text = msg.getText();
            chatroom = IrcStack.this.joined.get(user);
            if (chatroom == null)
            {
                chatroom = initiatePrivateChatRoom(user);
            }
            deliverReceivedMessageToPrivateChat(chatroom, user, text);
        }

        /**
         * Deliver a private message to the provided chat room.
         * 
         * @param chatroom the chat room
         * @param user the source user
         * @param text the message
         */
        private void deliverReceivedMessageToPrivateChat(ChatRoomIrcImpl chatroom,
            String user, String text)
        {
            ChatRoomMember member = chatroom.getChatRoomMember(user);
            MessageIrcImpl message =
                new MessageIrcImpl(text, "text/plain", "UTF-8", null);
            chatroom.fireMessageReceivedEvent(message, member, new Date(),
                ChatRoomMessageReceivedEvent.CONVERSATION_MESSAGE_RECEIVED);
        }

        /**
         * Create a private chat room if one does not exist yet.
         * 
         * @param user private chat room for this user
         * @return returns the private chat room
         */
        private ChatRoomIrcImpl initiatePrivateChatRoom(String user)
        {
            ChatRoomIrcImpl chatroom =
                (ChatRoomIrcImpl) IrcStack.this.provider.getMUC()
                    .findRoom(user);
            IrcStack.this.joined.put(chatroom.getIdentifier(), chatroom);
            ChatRoomMemberIrcImpl member =
                new ChatRoomMemberIrcImpl(IrcStack.this.provider, chatroom,
                    user, ChatRoomMemberRole.MEMBER);
            chatroom.addChatRoomMember(member.getContactAddress(), member);
            IrcStack.this.provider.getMUC().fireLocalUserPresenceEvent(
                chatroom,
                LocalUserChatRoomPresenceChangeEvent.LOCAL_USER_JOINED,
                "Private conversation initiated.");
            return chatroom;
        }
    }
    
    /**
     * A chat room listener.
     * 
     * A chat room listener is registered for each chat room that we join. The
     * chat room listener updates chat room data and fires events based on IRC
     * messages that report state changes for the specified channel.
     * 
     * @author danny
     * 
     */
    private class ChatRoomListener
        extends VariousMessageListenerAdapter
    {
        private ChatRoomIrcImpl chatroom;

        private ChatRoomListener(ChatRoomIrcImpl chatroom)
        {
            if (chatroom == null)
                throw new IllegalArgumentException("chatroom cannot be null");

            this.chatroom = chatroom;
        }

        @Override
        public void onTopicChange(TopicMessage msg)
        {
            if (isThisChatRoom(msg.getChannelName()) == false)
                return;
            
            this.chatroom.updateSubject(msg.getTopic().getValue());
        }

        @Override
        public void onChannelMode(ChannelModeMessage msg)
        {
            if (isThisChatRoom(msg.getChannelName()) == false)
                return;
            
            processModeMessage(msg);
        }

        @Override
        public void onChannelJoin(ChanJoinMessage msg)
        {
            if (isThisChatRoom(msg.getChannelName()) == false)
                return;
            
            if (isMe(msg.getSource()))
            {
                // I think that this should not happen.
            }
            else
            {
                String user = msg.getSource().getNick();
                ChatRoomMemberIrcImpl member =
                    new ChatRoomMemberIrcImpl(IrcStack.this.provider,
                        this.chatroom, user, ChatRoomMemberRole.SILENT_MEMBER);
                this.chatroom.fireMemberPresenceEvent(member, null,
                    ChatRoomMemberPresenceChangeEvent.MEMBER_JOINED, null);
            }
        }

        @Override
        public void onChannelPart(ChanPartMessage msg)
        {
            if (isThisChatRoom(msg.getChannelName()) == false)
                return;
            
            if (isMe(msg.getSource()))
            {
                IrcStack.this.irc.deleteListener(this);
                IrcStack.this.joined.remove(this.chatroom);
                IrcStack.this.provider.getMUC().fireLocalUserPresenceEvent(
                    this.chatroom,
                    LocalUserChatRoomPresenceChangeEvent.LOCAL_USER_LEFT, null);
            }
            else
            {
                String user = msg.getSource().getNick();
                ChatRoomMember member = this.chatroom.getChatRoomMember(user);
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
                    System.err
                        .println("This should not have happened. Please report this as it is a bug.");
                    e.printStackTrace();
                }
            }
        }
        
        @Override
        public void onChannelKick(ChannelKick msg)
        {
            if (isThisChatRoom(msg.getChannelName()) == false)
                return;
            
            String kickedUser = msg.getKickedNickname();
            if (isMe(kickedUser))
            {
                IrcStack.this.irc.deleteListener(this);
                IrcStack.this.joined.remove(this.chatroom);
                IrcStack.this.provider.getMUC().fireLocalUserPresenceEvent(
                    this.chatroom,
                    LocalUserChatRoomPresenceChangeEvent.LOCAL_USER_KICKED,
                    msg.getText());
            }
            else
            {
                ChatRoomMember kickedMember =
                    this.chatroom.getChatRoomMember(kickedUser);
                String user = msg.getSource().getNick();
                if (kickedMember != null)
                {
                    ChatRoomMember kicker =
                        this.chatroom.getChatRoomMember(user);
                    this.chatroom.fireMemberPresenceEvent(kickedMember,
                        kicker,
                        ChatRoomMemberPresenceChangeEvent.MEMBER_KICKED,
                        msg.getText());
                }
            }
        }

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
        
        @Override
        public void onChannelMessage(ChannelPrivMsg msg)
        {
            if (isThisChatRoom(msg.getChannelName()) == false)
                return;

            // TODO handle special formatting/color control codes in message
            // (see also user private messages)

            MessageIrcImpl message =
                new MessageIrcImpl(msg.getText(), "text/plain", "UTF-8", null);
            ChatRoomMemberIrcImpl member =
                new ChatRoomMemberIrcImpl(IrcStack.this.provider,
                    this.chatroom, msg.getSource().getNick(),
                    ChatRoomMemberRole.MEMBER);
            this.chatroom.fireMessageReceivedEvent(message, member, new Date(),
                ChatRoomMessageReceivedEvent.CONVERSATION_MESSAGE_RECEIVED);
        }

        private void processModeMessage(ChannelModeMessage msg)
        {
            // TODO Handle or ignore ban channel mode (MODE STRING: +b
            // *!*@some-ip.dynamicIP.provider.net)
            ChatRoomMemberIrcImpl sourceMember = extractChatRoomMember(msg);
                
            ModeParser parser = new ModeParser(msg);
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
                    else
                    {
                        ChatRoomMember owner =
                            this.chatroom
                                .getChatRoomMember(mode.getParams()[0]);
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
                    else
                    {
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
                    else
                    {
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
                    }
                    break;
                case LIMIT:
                    MessageIrcImpl message;
                    if (mode.isAdded())
                    {
                        try
                        {
                            message =
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
                            e.printStackTrace();
                            break;
                        }
                    }
                    else
                    {
                        // FIXME "server" is now easily fakeable if someone
                        // calls himself server. There should be some other way
                        // to represent the server if a message comes from
                        // something other than a normal chat room member.
                        message =
                            new MessageIrcImpl(
                                "channel limit removed by "
                                    + (sourceMember.getContactAddress()
                                        .length() == 0 ? "server"
                                        : sourceMember.getContactAddress()),
                                "text/plain", "UTF-8", null);
                    }
                    this.chatroom.fireMessageReceivedEvent(message,
                        sourceMember, new Date(),
                        ChatRoomMessageReceivedEvent.SYSTEM_MESSAGE_RECEIVED);
                    break;
                case UNKNOWN:
                    LOGGER.info("Unknown mode: "
                        + (mode.isAdded() ? "+" : "-") + mode.getParams()[0]
                        + ". Original mode string: '" + msg.getModeStr() + "'");
                    break;
                default:
                    LOGGER.info("Unsupported mode '"
                        + (mode.isAdded() ? "+" : "-") + mode.getMode()
                        + "' (from modestring '" + msg.getModeStr() + "')");
                    break;
                }
            }
        }

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

        private boolean isThisChatRoom(String chatRoomName)
        {
            return this.chatroom.getIdentifier().equals(chatRoomName);
        }

        private boolean isMe(IRCUser user)
        {
            return IrcStack.this.connectionState.getNickname().equals(
                user.getNick());
        }

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
        private List<String> channels;

        private ChannelListListener(IRCApi api, List<String> list)
        {
            if (api == null)
                throw new IllegalArgumentException("IRC api cannot be null");
            this.api = api;
            this.channels = list;
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
            if (this.channels == null)
                return;

            switch (msg.getNumericCode())
            {
            case 321:
                synchronized (this.channels)
                {
                    this.channels.clear();
                }
                break;
            case 322:
                String channel = parse(msg.getText());
                if (channel != null)
                {
                    synchronized (this.channels)
                    {
                        this.channels.add(channel);
                    }
                }
                break;
            case 323:
                synchronized (this.channels)
                {
                    this.channels.notifyAll();
                    // Done collecting channels. Delete list reference and
                    // remove listener and then we're done.
                    this.channels = null;
                    this.api.deleteListener(this);
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

        private String nick;

        private List<String> alternativeNicks = new ArrayList<String>();

        private String real;

        private String ident;

        private IRCServer server;

        private ServerParameters(String nickName, String realName,
            String ident, IRCServer server)
        {
            this.nick = checkNick(nickName);
            this.alternativeNicks.add(nickName + "_");
            this.real = realName;
            this.ident = ident;
            this.server = server;
        }

        @Override
        public String getNickname()
        {
            return this.nick;
        }

        public void setNickname(String nick)
        {
            this.nick = checkNick(nick);
        }
        
        private String checkNick(String nick)
        {
            if (nick == null)
                throw new IllegalArgumentException("a nick name must be provided");
            if  (nick.startsWith("#"))
                throw new IllegalArgumentException("the nick name must not start with '#' since this is reserved for IRC channels");
            return nick;
        }

        @Override
        public List<String> getAlternativeNicknames()
        {
            return this.alternativeNicks;
        }

        @Override
        public String getIdent()
        {
            return this.ident;
        }

        @Override
        public String getRealname()
        {
            return this.real;
        }

        @Override
        public IRCServer getServer()
        {
            return this.server;
        }

        public void setServer(IRCServer server)
        {
            this.server = server;
        }
    }

    /**
     * Simplest possible container that we can use for locking while we're
     * checking/modifying the contents.
     * 
     * FIXME I would love to get rid of this thing. Is there something similar
     * in Java stdlib?
     * 
     * @param <T> The type of instance to store in the container
     */
    private static class Container<T>
    {
        /**
         * The stored instance. (Can be null)
         */
        public T instance;

        /**
         * Constructor that immediately sets the instance.
         * 
         * @param instance the instance to set
         */
        private Container(T instance)
        {
            this.instance = instance;
        }
    }
}
