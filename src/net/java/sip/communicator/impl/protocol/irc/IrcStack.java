/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.irc;

import java.util.*;
import java.util.Map.Entry;

import net.java.sip.communicator.impl.protocol.irc.ModeParser.ModeEntry;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;

import com.ircclouds.irc.api.*;
import com.ircclouds.irc.api.domain.*;
import com.ircclouds.irc.api.domain.messages.*;
import com.ircclouds.irc.api.listeners.*;
import com.ircclouds.irc.api.state.*;

/**
 * An implementation of the PircBot IRC stack.
 */
public class IrcStack
{
    // private static final Logger LOGGER = Logger.getLogger(IrcStack.class);

    private final ProtocolProviderServiceIrcImpl provider;

    // TODO should make this thing a map or set?
    /**
     * Container for joined channels.
     */
    private final List<ChatRoomIrcImpl> joined = Collections
        .synchronizedList(new ArrayList<ChatRoomIrcImpl>());

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
        boolean autoNickChange) throws Exception
    {
        if (this.irc != null && this.connectionState != null
            && this.connectionState.isConnected())
            return;

        // Make sure we start with an empty joined-channel list.
        this.joined.clear();

        // A container for storing the exception if connecting fails.
        final Exception[] exceptionContainer = new Exception[1];

        this.irc = new IRCApiImpl(true);
        this.params.setServer(new IRCServer(host, port, password, false));
        synchronized (this.irc)
        {
            // register a server listener in order to catch server and cross-/multi-channel messages
            this.irc.addListener(new ServerListener());
            // start connecting to the specified server ...
            this.irc.connect(this.params, new Callback<IIRCState>()
            {

                @Override
                public void onSuccess(IIRCState state)
                {
                    synchronized (IrcStack.this.irc)
                    {
                        System.out.println("IRC connected successfully!");
                        IrcStack.this.connectionState = state;
                        IrcStack.this.irc.notifyAll();
                    }
                }

                @Override
                public void onFailure(Exception e)
                {
                    synchronized (IrcStack.this.irc)
                    {
                        System.out.println("IRC connection FAILED! ("
                            + e.getMessage() + ")");
                        exceptionContainer[0] = e;
                        IrcStack.this.connectionState = null;
                        IrcStack.this.irc.notifyAll();
                    }
                }
            });

            // wait while the irc connection is being established ...
            try
            {
                System.out.println("Waiting for the connection to be established ...");
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
            for (ChatRoomIrcImpl channel : this.joined)
            {
                leave(channel);
            }
        }
        this.irc.disconnect();
        this.irc = null;
        this.connectionState = null;
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
        return this.joined.contains(chatroom);
    }

    /**
     * Get a list of channels available on the IRC server.
     * 
     * @return List of available channels.
     */
    public List<String> getServerChatRoomList()
    {
        // TODO Implement this. (Also probably cache the list, to prevent doing too many requests.)
        return new ArrayList<String>();
    }

    /**
     * Join a particular chat room.
     * 
     * @param chatroom Chat room to join.
     */
    public void join(ChatRoomIrcImpl chatroom)
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
     */
    public void join(final ChatRoomIrcImpl chatroom, final String password)
    {
        if (isConnected() == false)
            throw new IllegalStateException(
                "Please connect to an IRC server first");
        if (chatroom == null)
            throw new IllegalArgumentException("chatroom cannot be null");
        if (password == null)
            throw new IllegalArgumentException("password cannot be null");
        
        if (this.joined.contains(chatroom) || chatroom.isPrivate())
        {
            // If we already joined this particular chatroom or if it is a
            // private chat room (i.e. message from one user to another), no
            // further action is required.
            return;
        }

        // TODO Handle forward to another channel (470) channel name change.
        // (Testable on irc.freenode.net#linux, forwards to ##linux)
        // Currently drops into an infinite wait because the callback is never
        // executed. Not sure how to catch/detect this forward operation and act
        // appropriately. Seems reasonable to just expect the same callback, but
        // with different channel information.
        final Object joinSignal = new Object();
        synchronized (joinSignal)
        {
            try
            {
                // TODO Refactor this ridiculous nesting of functions and
                // classes.
                this.irc.joinChannel(chatroom.getIdentifier(), password,
                    new Callback<IRCChannel>()
                    {

                        @Override
                        public void onSuccess(IRCChannel channel)
                        {
                            synchronized (joinSignal)
                            {
                                try
                                {
                                    IrcStack.this.joined.add(chatroom);
                                    IrcStack.this.irc
                                        .addListener(new ChatRoomListener(
                                            chatroom));
                                    IRCTopic topic = channel.getTopic();
                                    chatroom.updateSubject(topic.getValue());

                                    for (Entry<IRCUser, Set<IRCUserStatus>> userEntry : channel
                                        .getUsers().entrySet())
                                    {
                                        IRCUser user = userEntry.getKey();
                                        Set<IRCUserStatus> statuses =
                                            userEntry.getValue();
                                        ChatRoomMemberRole role =
                                            ChatRoomMemberRole.SILENT_MEMBER;
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
                                            chatroom.prepUserRole(role);
                                        }
                                        else
                                        {
                                            ChatRoomMemberIrcImpl member =
                                                new ChatRoomMemberIrcImpl(
                                                    IrcStack.this.provider,
                                                    chatroom, user.getNick(),
                                                    role);
                                            chatroom.addChatRoomMember(
                                                member.getContactAddress(),
                                                member);
                                        }
                                    }
                                }
                                finally
                                {
                                    // Notify waiting threads of finished
                                    // execution.
                                    joinSignal.notifyAll();
                                }
                            }
                        }

                        @Override
                        public void onFailure(Exception e)
                        {
                            synchronized (joinSignal)
                            {
                                try
                                {
                                    MessageIrcImpl message =
                                        new MessageIrcImpl(
                                            "Failed to join channel "
                                                + chatroom.getIdentifier()
                                                + "(message: " + e.getMessage()
                                                + ")", "text/plain", "UTF-8",
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
                                    // Notify waiting threads of finished
                                    // execution
                                    joinSignal.notifyAll();
                                }
                            }
                        }
                    });
                // Wait until async channel join operation has finished.
                joinSignal.wait();
                if (isJoined(chatroom))
                {
                    // In case waiting ends with successful join
                    IrcStack.this.provider.getMUC().fireLocalUserPresenceEvent(
                        chatroom,
                        LocalUserChatRoomPresenceChangeEvent.LOCAL_USER_JOINED,
                        null);
                }
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
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

    public void kickParticipant(ChatRoomIrcImpl chatroom,
        ChatRoomMember member, String reason)
    {
        this.irc.kick(chatroom.getIdentifier(), member.getContactAddress());
    }

    public void invite(String memberId, ChatRoomIrcImpl chatroom)
    {
        // TODO Implement this.
    }

    /**
     * Send a command to the IRC server.
     * @param chatroom the chat room
     * @param command the command message
     */
    public void command(ChatRoomIrcImpl chatroom, String command)
    {
        String target;
        if (command.startsWith("/msg "))
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
         */
        @Override
        public void onServerNotice(ServerNotice msg)
        {
            System.out.println("NOTICE: " + ((ServerNotice) msg).getText());
        }

        /**
         * Print out server numeric messages for debugging purposes and for
         * simply keeping track of the connection.
         */
        @Override
        public void onServerNumericMessage(ServerNumericMessage msg)
        {
            System.out.println("NUM MSG: "
                + ((ServerNumericMessage) msg).getNumericCode() + ": "
                + ((ServerNumericMessage) msg).getText());
        }

        @Override
        public void onUserPrivMessage(UserPrivMsg msg)
        {
            ChatRoomIrcImpl chatroom = null;
            String user = msg.getSource().getNick();
            String text = msg.getText();
            synchronized (IrcStack.this.joined)
            {
                // Find the chatroom matching the user.
                for (ChatRoomIrcImpl room : IrcStack.this.joined)
                {
                    if (room.isPrivate() == false)
                        continue;

                    if (user.equals(room.getIdentifier()))
                    {
                        chatroom = room;
                        break;
                    }
                }
            }
            if (chatroom == null)
            {
                chatroom = initiatePrivateChatRoom(user);
            }
            deliverReceivedMessageToPrivateChat(chatroom, user, text);
        }

        private void deliverReceivedMessageToPrivateChat(ChatRoomIrcImpl chatroom,
            String user, String text)
        {
            ChatRoomMember member = chatroom.getChatRoomMember(user);
            MessageIrcImpl message =
                new MessageIrcImpl(text, "text/plain", "UTF-8", null);
            chatroom.fireMessageReceivedEvent(message, member, new Date(),
                ChatRoomMessageReceivedEvent.CONVERSATION_MESSAGE_RECEIVED);
        }

        private ChatRoomIrcImpl initiatePrivateChatRoom(String user)
        {
            ChatRoomIrcImpl chatroom =
                (ChatRoomIrcImpl) IrcStack.this.provider.getMUC()
                    .findRoom(user);
            IrcStack.this.joined.add(chatroom);
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
                IrcStack.this.provider.getMUC().fireLocalUserPresenceEvent(
                    this.chatroom,
                    LocalUserChatRoomPresenceChangeEvent.LOCAL_USER_LEFT, null);
                IrcStack.this.irc.deleteListener(this);
                IrcStack.this.joined.remove(this.chatroom);
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
            
            String kickedUser = msg.getKickedUser();
            if (isMe(kickedUser))
            {
                IrcStack.this.provider.getMUC().fireLocalUserPresenceEvent(
                    this.chatroom,
                    LocalUserChatRoomPresenceChangeEvent.LOCAL_USER_KICKED,
                    msg.getText());
                IrcStack.this.irc.deleteListener(this);
                IrcStack.this.joined.remove(this.chatroom);
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
            ModeParser parser = new ModeParser(msg);
            for (ModeEntry mode : parser.getModes())
            {
                switch (mode.getMode())
                {
                case OWNER:
                    String ownerUserName = mode.getParams()[0];
                    if (isMe(ownerUserName))
                    {
                        System.out.println("Local user owner change! (no business logic yet, just discovery) "
                            + mode.isAdded());
                        // TODO Do something on local user owner change.
                    }
                    else
                    {
                        ChatRoomMember owner =
                            this.chatroom
                                .getChatRoomMember(mode.getParams()[0]);
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
                        System.out.println("Local user op change! "
                            + mode.isAdded());
                        // TODO How to fire a local user role change event?
                        // TODO Do something in case the local user is affected,
                        // hence the member cannot be found using
                        // getChatRoomMember.
                    }
                    else
                    {
                        ChatRoomMember op =
                            this.chatroom.getChatRoomMember(opUserName);
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
                        System.out.println("Local user voice change! "+mode.isAdded());
                        // TODO Do something when local user is affected.
                    }
                    else
                    {
                        ChatRoomMember voice =
                            this.chatroom.getChatRoomMember(voiceUserName);
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
                default:
                    System.out.println("Unsupported mode '"
                        + (mode.isAdded() ? "+" : "-") + mode.getMode()
                        + "' (from modestring '" + msg.getModeStr() + "')");
                    break;
                }
            }
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
            this.nick = nickName;
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
            this.nick = nick;
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
}
