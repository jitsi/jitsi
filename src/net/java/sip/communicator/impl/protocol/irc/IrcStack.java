/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.irc;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * An implementation of the PircBot IRC stack.
 */
public class IrcStack
{
    private static final Logger logger = Logger.getLogger(IrcStack.class);

    /**
     * Timeout for server response.
     */
    private static final int TIMEOUT = 10000;

    public IrcStack(ProtocolProviderServiceIrcImpl parentProvider, String nick, String login, String version, String finger)
    {
        //TODO Implement this.
    }
    
    public boolean isConnected()
    {
        //TODO Implement this.
        return false;
    }
    
    public void connect(String host, int port, String password, boolean autoNickChange)
    {
        //TODO Implement this.
    }
    
    public void disconnect()
    {
        //TODO Implement this.
    }
    
    public void dispose()
    {
        //TODO Implement this.
    }
    
    public String getNick()
    {
        //TODO Implement this.
        return "";
    }
    
    public void setUserNickname(String nick)
    {
        //TODO Implement this.
    }
    
    public void setSubject(ChatRoomIrcImpl chatroom, String subject)
    {
        //TODO Implement this.
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
        //TODO Implement this.
    }
    
    public void join(ChatRoomIrcImpl chatroom, byte[] password)
    {
        //TODO Implement this.
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
}
