/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.irc;

import java.util.*;

/**
 * Represents the informations we get from a user when we use WHOIS
 * 
 * @author Stephane Remy
 */
public class UserInfo
{
    /**
     * The nickname of this user.
     */
    private final String nickName;
    
    /**
     * The login of this user.
     */
    private final String login;
    
    /**
     * The hostname of this user.
     */
    private final String hostname;
    
    /**
     * A list of the chat rooms this user is in.
     */
    private final List<String> joinedChatRoom = new LinkedList<String>();
    
    /**
     * Information about the server.
     */
    private String serverInfo = null;
    
    /**
     * Indicates if this user is an IRC operator.
     */
    private boolean isIrcOp = false;
    
    /**
     * Indicates if this user is idle.
     */
    private long idle = 0;

    /**
     * 
     * @param nickName
     * @param login
     * @param hostname
     */
    public UserInfo(String nickName, String login, String hostname)
    {
        this.nickName = nickName;
        this.login = login;
        this.hostname = hostname;
    }

    /**
     * Adds a chat room to the list of chat rooms joined by this user.
     * 
     * @param chatRoom the name of the chat room we want to add to the list
     */
    public void addJoinedChatRoom(String chatRoom)
    {
        synchronized(joinedChatRoom)
        {
            joinedChatRoom.add(chatRoom);
        }
    }

    /**
     * Removes a chat room from the list of joined chat rooms.
     * 
     * @param chatRoom the chat room we want to remove.
     */
    public void removeJoinedChatRoom(String chatRoom)
    {
        synchronized(joinedChatRoom)
        {
            joinedChatRoom.remove(chatRoom);
        }
    }

    /**
     * Clears the list of joined chat rooms.
     */
    public void clearJoinedChatRoom()
    {
        synchronized(joinedChatRoom)
        {
            joinedChatRoom.clear();
        }
    }

    /**
     * Returns the host name of this user.
     * 
     * @return the hostname of this user
     */
    public String getHostname()
    {
        return hostname;
    }

    /**
     * Returns the time from which this user is idle.
     * 
     * @return the idle time of this user
     */
    public long getIdle()
    {
        return idle;
    }

    /**
     * The list of chat rooms that this user has joined.
     * 
     * @return a list of the joined chat rooms of this user
     */
    public List<String> getJoinedChatRooms()
    {
        return joinedChatRoom;
    }

    /**
     * Returns the login of this user.
     * 
     * @return the login of this user
     */
    public String getLogin()
    {
        return login;
    }

    /**
     * Returns the nickname of this user.
     *  
     * @return the nickname of this user
     */
    public String getNickName()
    {
        return nickName;
    }

    /**
     * Returns TRUE if this user is an IRC operator and false otherwise.
     * 
     * @return true if this user is an IRCOP or false otherwise
     */
    public boolean isIrcOp()
    {
        return isIrcOp;
    }

    /**
     * Returns the server info.
     * 
     * @return a string server information 
     */
    public String getServerInfo()
    {
        return serverInfo;
    }

    /**
     * Set the idle time for this user.
     * 
     * @param idle the idle time of this user
     */
    protected void setIdle(long idle)
    {
        this.idle = idle;
    }
    
    /**
     * Set if this user is an IRC operator.
     * 
     * @param isIrcOp TRUE to indicate that the user is an IRC operator and
     * FALSE otherwise
     */
    protected void setIrcOp(boolean isIrcOp)
    {
        this.isIrcOp = isIrcOp;
    }
    
    /**
     * Set the information for the server
     * 
     * @param serverInfo the information for the server
     */
    protected void setServerInfo(String serverInfo)
    {
        this.serverInfo = serverInfo;
    }
}
