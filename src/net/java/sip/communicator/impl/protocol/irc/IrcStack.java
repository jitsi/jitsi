/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.irc;

import java.util.*;

import com.ircclouds.irc.api.Callback;
import com.ircclouds.irc.api.IRCApi;
import com.ircclouds.irc.api.IRCApiImpl;
import com.ircclouds.irc.api.IServerParameters;
import com.ircclouds.irc.api.domain.IRCServer;
import com.ircclouds.irc.api.state.IIRCState;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * An implementation of the PircBot IRC stack.
 */
public class IrcStack
{
    private static final Logger LOGGER = Logger.getLogger(IrcStack.class);

    private final ProtocolProviderServiceIrcImpl provider;
    
    private final IRCApi irc = new IRCApiImpl(true);
    
    private final ServerParameters params;

    private IIRCState connectionState;
    
    public IrcStack(final ProtocolProviderServiceIrcImpl parentProvider, final String nick, final String login, final String version, final String finger)
    {
        if (parentProvider == null)
        {
            throw new NullPointerException("parentProvider cannot be null");
        }
        this.provider = parentProvider;
        this.params = new IrcStack.ServerParameters(nick, "", finger, null);
    }
    
    public boolean isConnected()
    {
        return (this.connectionState != null && this.connectionState.isConnected());
    }
    
    public void connect(String host, int port, String password, boolean autoNickChange)
    {
        this.params.setServer(new IRCServer(host, port, password, false));
        synchronized(this.irc)
        {
            // start connecting to the specified server ...
            this.irc.connect(this.params, new Callback<IIRCState>()
            {

                @Override
                public void onSuccess(IIRCState state)
                {
                    synchronized(IrcStack.this.irc)
                    {
                        System.out.println("IRC connected successfully!");
                        IrcStack.this.connectionState = state;
                        IrcStack.this.irc.notifyAll();
                    }
                }

                @Override
                public void onFailure(Exception e)
                {
                    synchronized(IrcStack.this.irc)
                    {
                        System.out.println("IRC connection FAILED!");
                        e.printStackTrace();
                        IrcStack.this.connectionState = null;
                        IrcStack.this.irc.notifyAll();
                    }
                }
            });
            
            // wait while the irc connection is being established ...
            try
            {
                System.out.println("Waiting for a connection ...");
                this.irc.wait();
            }
            catch (InterruptedException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            
            //TODO do something on connection fail!
        }
    }
    
    public void disconnect()
    {
        if(this.connectionState != null)
        {
            this.irc.disconnect();
            this.connectionState = null;
        }
    }
    
    public void dispose()
    {
        disconnect();
    }
    
    public String getNick()
    {
        return (this.connectionState == null) ? this.params.getNickname() : this.connectionState.getNickname();
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
            throw new IllegalStateException("Please connect to an IRC server first.");
        if (chatroom == null)
            throw new IllegalArgumentException("Cannot have a null chatroom");
        this.irc.changeTopic(chatroom.getName(), subject == null ? "" : subject);
    }
    
    public boolean isJoined(ChatRoomIrcImpl chatroom)
    {
        //TODO Implement this.
        return false;
    }
    
    public List<String> getServerChatRoomList()
    {
        //TODO Implement this.
        return new ArrayList<String>();
    }
    
    public void join(ChatRoomIrcImpl chatroom)
    {
        join(chatroom, "".getBytes());
    }
    
    public void join(ChatRoomIrcImpl chatroom, byte[] password)
    {
        //TODO password as String
        if (chatroom == null)
            throw new IllegalArgumentException("chatroom cannot be null");
        if (password == null)
            throw new IllegalArgumentException("password cannot be null");
        //TODO add chatroom listener
        this.irc.joinChannel(chatroom.getName(), password.toString());
    }
    
    public void leave(ChatRoomIrcImpl chatroom)
    {
        //TODO Implement this.
    }
    
    public void banParticipant(ChatRoomIrcImpl chatroom, ChatRoomMember member, String reason)
    {
        //TODO Implement this.
    }
    
    public void kickParticipant(ChatRoomIrcImpl chatroom, ChatRoomMember member, String reason)
    {
        //TODO Implement this.
    }
    
    public void invite(String memberId, ChatRoomIrcImpl chatroom)
    {
        //TODO Implement this.
    }
    
    public void command(ChatRoomIrcImpl chatroom, String command)
    {
        //TODO Implement this.
    }
    
    public void message(ChatRoomIrcImpl chatroom, String message)
    {
        //TODO Implement this.
    }
    
    private static class ServerParameters implements IServerParameters {

        private String nick;
        private List<String> alternativeNicks = new ArrayList<String>();
        private String real;
        private String ident;
        private IRCServer server;
        
        private ServerParameters(String nickName, String realName, String ident, IRCServer server)
        {
            this.nick = nickName;
            this.alternativeNicks.add(nickName+"_");
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
