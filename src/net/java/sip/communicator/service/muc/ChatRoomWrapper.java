/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.muc;

import net.java.sip.communicator.service.protocol.*;

/**
 * The <tt>ChatRoomWrapper</tt> is the representation of the <tt>ChatRoom</tt>
 * in the GUI. It stores the information for the chat room even when the
 * corresponding protocol provider is not connected.
 *
 * @author Yana Stamcheva
 * @author Damian Minkov
 */
public interface ChatRoomWrapper
{

    /**
     * Returns the <tt>ChatRoom</tt> that this wrapper represents.
     *
     * @return the <tt>ChatRoom</tt> that this wrapper represents.
     */
    public ChatRoom getChatRoom();

    /**
     * Sets the <tt>ChatRoom</tt> that this wrapper represents.
     *
     * @param chatRoom the chat room
     */
    public void setChatRoom(ChatRoom chatRoom);

    /**
     * Returns the chat room name.
     *
     * @return the chat room name
     */
    public String getChatRoomName();

    /**
     * Returns the identifier of the chat room.
     *
     * @return the identifier of the chat room
     */
    public String getChatRoomID();

    /**
     * Returns the parent protocol provider.
     *
     * @return the parent protocol provider
     */
    public ChatRoomProviderWrapper getParentProvider();

    /**
     * Returns <code>true</code> if the chat room is persistent,
     * otherwise - returns <code>false</code>.
     *
     * @return <code>true</code> if the chat room is persistent,
     * otherwise - returns <code>false</code>.
     */
    public boolean isPersistent();

    /**
     * Change persistence of this room.
     * @param value set persistent state.
     */
    public void setPersistent(boolean value);
    
    /**
     * Stores the password for the chat room.
     * 
     * @param password the password to store
     */
    public void savePassword(String password);
    
    /**
     * Returns the password for the chat room.
     * 
     * @return the password
     */
    public String loadPassword();
    
    /**
     * Removes the saved password for the chat room.
     */
    public void removePassword();

    /**
     * Is room set to auto join on start-up.
     * @return is auto joining enabled.
     */
    public boolean isAutojoin();

    /**
     * Changes auto join value in configuration service.
     *
     * @param value change of auto join property.
     */
    public void setAutoJoin(boolean value);
}
