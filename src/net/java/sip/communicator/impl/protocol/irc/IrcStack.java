/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.irc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.java.sip.communicator.service.muc.ChatRoomPresenceStatus;
import net.java.sip.communicator.service.protocol.ChatRoomMember;
import net.java.sip.communicator.service.protocol.ChatRoomMemberRole;
import net.java.sip.communicator.service.protocol.RegistrationState;
import net.java.sip.communicator.service.protocol.event.ChatRoomMemberPresenceChangeEvent;
import net.java.sip.communicator.service.protocol.event.ChatRoomMessageReceivedEvent;
import net.java.sip.communicator.service.protocol.event.ChatRoomPropertyChangeEvent;
import net.java.sip.communicator.service.protocol.event.LocalUserChatRoomPresenceChangeEvent;
import net.java.sip.communicator.service.protocol.event.RegistrationStateChangeEvent;

import com.ircclouds.irc.api.Callback;
import com.ircclouds.irc.api.IRCApi;
import com.ircclouds.irc.api.IRCApiImpl;
import com.ircclouds.irc.api.IServerParameters;
import com.ircclouds.irc.api.domain.IRCChannel;
import com.ircclouds.irc.api.domain.IRCServer;
import com.ircclouds.irc.api.domain.IRCTopic;
import com.ircclouds.irc.api.domain.IRCUser;
import com.ircclouds.irc.api.domain.IRCUserStatus;
import com.ircclouds.irc.api.domain.messages.ChanJoinMessage;
import com.ircclouds.irc.api.domain.messages.ChanPartMessage;
import com.ircclouds.irc.api.domain.messages.ChannelModeMessage;
import com.ircclouds.irc.api.domain.messages.ChannelPrivMsg;
import com.ircclouds.irc.api.domain.messages.QuitMessage;
import com.ircclouds.irc.api.domain.messages.ServerNotice;
import com.ircclouds.irc.api.domain.messages.ServerNumericMessage;
import com.ircclouds.irc.api.domain.messages.TopicMessage;
import com.ircclouds.irc.api.domain.messages.interfaces.IMessage;
import com.ircclouds.irc.api.listeners.IMessageListener;
import com.ircclouds.irc.api.listeners.VariousMessageListenerAdapter;
import com.ircclouds.irc.api.state.IIRCState;

/**
 * An implementation of the PircBot IRC stack.
 */
public class IrcStack
{
    // private static final Logger LOGGER = Logger.getLogger(IrcStack.class);
    
    private static final char MODE_MEMBER_OWNER = 'O';
    private static final char MODE_MEMBER_OPERATOR = 'o';
    private static final char MODE_MEMBER_VOICE = 'v';

    private final ProtocolProviderServiceIrcImpl provider;

    private final Map<String, IRCChannel> joined = Collections
        .synchronizedMap(new HashMap<String, IRCChannel>());

    private final ServerParameters params;

    private IRCApi irc;

    private IIRCState connectionState;

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

    public boolean isConnected()
    {
        return (this.irc != null && this.connectionState != null && this.connectionState
            .isConnected());
    }

    public void connect(String host, int port, String password,
        boolean autoNickChange)
    {
        if (this.irc != null && this.connectionState != null && this.connectionState.isConnected())
            return;

        this.irc = new IRCApiImpl(true);
        this.params.setServer(new IRCServer(host, port, password, false));
        synchronized (this.irc)
        {
            this.irc.addListener(new IMessageListener()
            {

                @Override
                public void onMessage(IMessage msg)
                {
                    if (msg instanceof ServerNotice)
                    {
                        System.out.println("NOTICE: "
                            + ((ServerNotice) msg).getText());
                    }
                    else if (msg instanceof ServerNumericMessage)
                    {
                        System.out.println("NUM MSG: "
                            + ((ServerNumericMessage) msg).getNumericCode()
                            + ": " + ((ServerNumericMessage) msg).getText());
                    }
                }
            });
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
                        System.out.println("IRC connection FAILED!");
                        e.printStackTrace();
                        IrcStack.this.connectionState = null;
                        IrcStack.this.disconnect();
                        IrcStack.this.irc.notifyAll();
                    }
                }
            });

            // wait while the irc connection is being established ...
            try
            {
                System.out.println("Waiting for a connection ...");
                this.irc.wait();
                if (this.connectionState != null
                    && this.connectionState.isConnected())
                {
                    this.provider
                        .setCurrentRegistrationState(RegistrationState.REGISTERED);
                }
                else
                {
                    // TODO Get reason from other thread.
                    this.provider
                        .setCurrentRegistrationState(RegistrationState.UNREGISTERED);
                }
            }
            catch (InterruptedException e)
            {
                this.provider
                    .setCurrentRegistrationState(RegistrationState.UNREGISTERED);
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public void disconnect()
    {
        if (this.connectionState != null)
        {
            for (String channel : this.joined.keySet())
            {
                leave(channel);
            }
            this.joined.clear();
            this.irc.disconnect();
            this.irc = null;
            this.connectionState = null;
            this.provider.setCurrentRegistrationState(RegistrationState.UNREGISTERED);
        }
    }

    public void dispose()
    {
        disconnect();
    }

    public String getNick()
    {
        return (this.connectionState == null) ? this.params.getNickname()
            : this.connectionState.getNickname();
    }

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

    public void setSubject(ChatRoomIrcImpl chatroom, String subject)
    {
        if (this.connectionState == null)
            throw new IllegalStateException(
                "Please connect to an IRC server first.");
        if (chatroom == null)
            throw new IllegalArgumentException("Cannot have a null chatroom");
        this.irc
            .changeTopic(chatroom.getName(), subject == null ? "" : subject);
    }

    public boolean isJoined(ChatRoomIrcImpl chatroom)
    {
        String chatRoomName = chatroom.getIdentifier();
        return this.joined.containsKey(chatRoomName);
    }

    public List<String> getServerChatRoomList()
    {
        // TODO Implement this.
        return new ArrayList<String>();
    }

    public void join(ChatRoomIrcImpl chatroom)
    {
        join(chatroom, "");
    }

    public void join(final ChatRoomIrcImpl chatroom, final String password)
    {
        // TODO password as String
        if (chatroom == null)
            throw new IllegalArgumentException("chatroom cannot be null");
        if (password == null)
            throw new IllegalArgumentException("password cannot be null");

        this.irc.joinChannel(chatroom.getIdentifier(), password,
            new Callback<IRCChannel>()
            {

                @Override
                public void onSuccess(IRCChannel channel)
                {
                    String name = channel.getName();
                    IrcStack.this.joined.put(name, channel);

                    IrcStack.this.irc
                        .addListener(new ChatRoomListener(chatroom));

                    IRCTopic topic = channel.getTopic();
                    chatroom.updateSubject(topic.getValue());

                    for (Entry<IRCUser, Set<IRCUserStatus>> userEntry : channel
                        .getUsers().entrySet())
                    {
                        IRCUser user = userEntry.getKey();
                        Set<IRCUserStatus> statuses = userEntry.getValue();
                        ChatRoomMemberRole role =
                            ChatRoomMemberRole.SILENT_MEMBER;
                        for (IRCUserStatus status : statuses)
                        {
                            role = convertMemberMode(status.getChanModeType().charValue());
                        }
                        
                        if (IrcStack.this.getNick().equals(user.getNick()))
                        {
                            chatroom.prepUserRole(role);
                        }
                        else
                        {
                            ChatRoomMemberIrcImpl member =
                                new ChatRoomMemberIrcImpl(IrcStack.this.provider,
                                    chatroom, user.getNick(), role);
                            chatroom.addChatRoomMember(member.getContactAddress(),
                                member);
                        }
                    }

                    IrcStack.this.provider.getMUC().fireLocalUserPresenceEvent(
                        chatroom,
                        LocalUserChatRoomPresenceChangeEvent.LOCAL_USER_JOINED,
                        null);
                }

                @Override
                public void onFailure(Exception aExc)
                {
                    // TODO Auto-generated method stub
                }
            });
    }

    public void leave(ChatRoomIrcImpl chatroom)
    {
        leave(chatroom.getIdentifier());
    }

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

    public void command(ChatRoomIrcImpl chatroom, String command)
    {
        // TODO Implement this.
    }

    public void message(ChatRoomIrcImpl chatroom, String message)
    {
        this.irc.message(chatroom.getIdentifier(), message);
    }
    
    private static ChatRoomMemberRole convertMemberMode(char mode)
    {
        switch(mode)
        {
        case MODE_MEMBER_OWNER:
            return ChatRoomMemberRole.OWNER;
        case MODE_MEMBER_OPERATOR:
            return ChatRoomMemberRole.ADMINISTRATOR;
        case MODE_MEMBER_VOICE:
            return ChatRoomMemberRole.MEMBER;
        default:
            throw new IllegalArgumentException("Unknown member mode '"+mode+"'.");
        }
    }

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
        
        private boolean isThisChatRoom(String chatRoomName)
        {
            return this.chatroom.getIdentifier().equals(chatRoomName);
        }

        @Override
        public void onTopicChange(TopicMessage msg)
        {
            if (isThisChatRoom(msg.getChannelName()))
            {
                this.chatroom.updateSubject(msg.getTopic().getValue());
            }
        }

        @Override
        public void onChannelMode(ChannelModeMessage msg)
        {
            if (isThisChatRoom(msg.getChannelName()))
            {
                System.out.println("MODE: " + msg.getModeStr());
            }
        }

        @Override
        public void onChannelJoin(ChanJoinMessage msg)
        {
            if (isThisChatRoom(msg.getChannelName()))
            {
                String user = msg.getSource().getNick();
                
                if (IrcStack.this.getNick().equals(msg.getSource().getNick()))
                {
                    //I think that this should not happen.
                }
                else
                {
                    ChatRoomMemberIrcImpl member =
                        new ChatRoomMemberIrcImpl(IrcStack.this.provider,
                            this.chatroom, user, ChatRoomMemberRole.SILENT_MEMBER);
                    this.chatroom.fireMemberPresenceEvent(member, null,
                        ChatRoomMemberPresenceChangeEvent.MEMBER_JOINED, null);
                }
            }
        }

        @Override
        public void onChannelPart(ChanPartMessage msg)
        {
            if (isThisChatRoom(msg.getChannelName()))
            {
                String user = msg.getSource().getNick();
                
                if (IrcStack.this.getNick().equals(msg.getSource().getNick()))
                {
                    IrcStack.this.provider.getMUC().fireLocalUserPresenceEvent(
                        this.chatroom,
                        LocalUserChatRoomPresenceChangeEvent.LOCAL_USER_LEFT, null);
                    IrcStack.this.joined.remove(this.chatroom.getIdentifier());
                    IrcStack.this.irc.deleteListener(this);
                }
                else
                {
                    ChatRoomMember member =
                        this.chatroom.getChatRoomMember(user);
                    this.chatroom.fireMemberPresenceEvent(member, null,
                        ChatRoomMemberPresenceChangeEvent.MEMBER_LEFT,
                        msg.getPartMsg());
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
        public void onChannelMessage(ChannelPrivMsg msg)
        {
            if (isThisChatRoom(msg.getChannelName()))
            {
                MessageIrcImpl message =
                    new MessageIrcImpl(msg.getText(), "text/plain", "UTF-8",
                        null);
                ChatRoomMemberIrcImpl member =
                    new ChatRoomMemberIrcImpl(IrcStack.this.provider,
                        this.chatroom, msg.getSource().getNick(),
                        ChatRoomMemberRole.MEMBER);
                this.chatroom.fireMessageReceivedEvent(message, member,
                    new Date(),
                    ChatRoomMessageReceivedEvent.CONVERSATION_MESSAGE_RECEIVED);
            }
        }
    }

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

        public void setAlternativeNicknames(List<String> names)
        {
            this.alternativeNicks = names;
        }

        @Override
        public String getIdent()
        {
            return this.ident;
        }

        public void setIdent(String ident)
        {
            this.ident = ident;
        }

        @Override
        public String getRealname()
        {
            return this.real;
        }

        public void setRealname(String realname)
        {
            this.real = realname;
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
