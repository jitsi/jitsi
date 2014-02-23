/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.muc;

import net.java.sip.communicator.service.muc.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * Source contact for the chat rooms.
 * 
 * @author Hristo Terezov
 */
public class ChatRoomSourceContact
    extends BaseChatRoomSourceContact
{
    /**
     * The parent contact query.
     */
    private final ChatRoomQuery parentQuery;

    /**
     * The protocol provider of the chat room associated with the contact.
     */
    private boolean isAutoJoin;

    /**
     * Constructs a new chat room source contact.
     *
     * @param chatRoomName the name of the chat room associated with the room.
     * @param chatRoomID the id of the chat room associated with the room.
     * @param query the query associated with the contact.
     * @param pps the protocol provider of the contact.
     * @param isAutoJoin the auto join state.
     */
    public ChatRoomSourceContact(String chatRoomName, 
        String chatRoomID, ChatRoomQuery query, ProtocolProviderService pps,
        boolean isAutoJoin)
    {
        super(chatRoomName, chatRoomID, query, pps);
        
        this.parentQuery = query;
        this.isAutoJoin = isAutoJoin;
        
        initContactProperties(getChatRoomStateByName());
    }

    /**
     * Constructs new chat room source contact.
     *
     * @param chatRoom the chat room associated with the contact.
     * @param query the query associated with the contact.
     * @param isAutoJoin the auto join state
     */
    public ChatRoomSourceContact(ChatRoom chatRoom, ChatRoomQuery query,
        boolean isAutoJoin)
    { 
        super(chatRoom.getName(), chatRoom.getIdentifier(), query, 
            chatRoom.getParentProvider());
        this.parentQuery = query;
        this.isAutoJoin = isAutoJoin;
        
        initContactProperties(
                chatRoom.isJoined()
                    ? ChatRoomPresenceStatus.CHAT_ROOM_ONLINE
                    : ChatRoomPresenceStatus.CHAT_ROOM_OFFLINE);
        
    }

    /**
     * Checks if the chat room associated with the contact is joined or not and 
     * returns it presence status.
     *
     * @return the presence status of the chat room associated with the contact.
     */
    private PresenceStatus getChatRoomStateByName()
    {
        for(ChatRoom room : 
                getProvider().getOperationSet(OperationSetMultiUserChat.class)
                    .getCurrentlyJoinedChatRooms())
        {
            if(room.getName().equals(getChatRoomName()))
            {
                return ChatRoomPresenceStatus.CHAT_ROOM_ONLINE;
            }
        }
        return ChatRoomPresenceStatus.CHAT_ROOM_OFFLINE;
    }

    /**
     * Returns the index of this source contact in its parent group.
     *
     * @return the index of this contact in its parent
     */
    @Override
    public int getIndex()
    {
        return parentQuery.indexOf(this);
    }

    /**
     * Returns the auto join state of the contact.
     *
     * @return the auto join state of the contact.
     */
    public boolean isAutoJoin()
    {
        return isAutoJoin;
    }

    /**
     * Sets the auto join state of the contact.
     *
     * @param isAutoJoin the auto join state to be set.
     */
    public void setAutoJoin(boolean isAutoJoin)
    {
        this.isAutoJoin = isAutoJoin;
    }
}
