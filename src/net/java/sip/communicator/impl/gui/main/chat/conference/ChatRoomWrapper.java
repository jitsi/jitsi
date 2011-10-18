/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chat.conference;

import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * The <tt>ChatRoomWrapper</tt> is the representation of the <tt>ChatRoom</tt>
 * in the GUI. It stores the information for the chat room even when the
 * corresponding protocol provider is not connected.
 * 
 * @author Yana Stamcheva
 * @author Damian Minkov
 */
public class ChatRoomWrapper
{
    /**
     * The protocol provider to which the corresponding chat room belongs.
     */
    private final ChatRoomProviderWrapper parentProvider;

    /**
     * The room that is wrapped.
     */
    private ChatRoom chatRoom;

    /**
     * The room name.
     */
    private final String chatRoomName;

    /**
     * The room id.
     */
    private final String chatRoomID;

    /**
     * The property we use to store values in configuration service.
     */
    private static final String AUTOJOIN_PROPERTY_NAME = "autoJoin";

    /**
     * As isAutoJoin can be called from GUI many times we store its value once
     * retrieved to minimize calls to configuration service.
     */
    private Boolean autoJoin = null;

    /**
     * By default all chat rooms are persistent from UI point of view.
     * But we can override this and force not saving it.
     * If not overridden we query the wrapped room.
     */
    private Boolean persistent = null;

    /**
     * Creates a <tt>ChatRoomWrapper</tt> by specifying the protocol provider,
     * the identifier and the name of the chat room.
     * 
     * @param parentProvider the protocol provider to which the corresponding
     * chat room belongs
     * @param chatRoomID the identifier of the corresponding chat room
     * @param chatRoomName the name of the corresponding chat room
     */
    public ChatRoomWrapper( ChatRoomProviderWrapper parentProvider,
                            String chatRoomID,
                            String chatRoomName)
    {
        this.parentProvider = parentProvider;
        this.chatRoomID = chatRoomID;
        this.chatRoomName = chatRoomName;
    }

    /**
     * Creates a <tt>ChatRoomWrapper</tt> by specifying the corresponding chat
     * room.
     * 
     * @param parentProvider the protocol provider to which the corresponding
     * chat room belongs
     * @param chatRoom the chat room to which this wrapper corresponds.
     */
    public ChatRoomWrapper( ChatRoomProviderWrapper parentProvider,
                            ChatRoom chatRoom)
    {
        this(   parentProvider,
                chatRoom.getIdentifier(),
                chatRoom.getName());

        this.chatRoom = chatRoom;
    }

    /**
     * Returns the <tt>ChatRoom</tt> that this wrapper represents.
     * 
     * @return the <tt>ChatRoom</tt> that this wrapper represents.
     */
    public ChatRoom getChatRoom()
    {
        return chatRoom;
    }

    /**
     * Sets the <tt>ChatRoom</tt> that this wrapper represents.
     * 
     * @param chatRoom the chat room
     */
    public void setChatRoom(ChatRoom chatRoom)
    {
        this.chatRoom = chatRoom;
    }

    /**
     * Returns the chat room name.
     * 
     * @return the chat room name
     */
    public String getChatRoomName()
    {
        return chatRoomName;
    }

    /**
     * Returns the identifier of the chat room.
     * 
     * @return the identifier of the chat room
     */
    public String getChatRoomID()
    {
        return chatRoomID;
    }

    /**
     * Returns the parent protocol provider.
     * 
     * @return the parent protocol provider
     */
    public ChatRoomProviderWrapper getParentProvider()
    {
        return this.parentProvider;
    }

    /**
     * Returns <code>true</code> if the chat room is persistent,
     * otherwise - returns <code>false</code>.
     * 
     * @return <code>true</code> if the chat room is persistent,
     * otherwise - returns <code>false</code>.
     */
    public boolean isPersistent()
    {
        if(persistent == null)
        {
            if(chatRoom != null)
                persistent = chatRoom.isPersistent();
            else
                return true;
        }

        return persistent;
    }

    /**
     * Change persistence of this room.
     * @param value set persistent state.
     */
    public void setPersistent(boolean value)
    {
        this.persistent = value;
    }

    /**
     * Is room set to auto join on start-up.
     * @return is auto joining enabled.
     */
    public boolean isAutojoin()
    {
        if(autoJoin == null)
        {
            String val = ConfigurationManager.getChatRoomProperty(
                getParentProvider().getProtocolProvider(),
                getChatRoomID(), AUTOJOIN_PROPERTY_NAME);

            autoJoin = Boolean.valueOf(val);
        }

        return autoJoin;
    }

    /**
     * Changes auto join value in configuration service.
     *
     * @param value change of auto join property.
     */
    public void setAutoJoin(boolean value)
    {
        autoJoin = value;

        // as the user wants to autojoin this room
        // and it maybe already created as non persistent
        // we must set it persistent and store it
        if(!isPersistent())
        {
            setPersistent(true);

            ConfigurationManager.saveChatRoom(
                getParentProvider().getProtocolProvider(),
                getChatRoomID(),
                getChatRoomID(),
                getChatRoomName());
        }

        if(value)
        {
            ConfigurationManager.updateChatRoomProperty(
                getParentProvider().getProtocolProvider(),
                chatRoomID, AUTOJOIN_PROPERTY_NAME, Boolean.toString(autoJoin));
        }
        else
        {
            ConfigurationManager.updateChatRoomProperty(
                getParentProvider().getProtocolProvider(),
                chatRoomID, AUTOJOIN_PROPERTY_NAME, null);
        }
    }
}
