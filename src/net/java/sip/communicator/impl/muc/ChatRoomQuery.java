/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.muc;

import java.util.*;
import java.util.regex.*;

import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.muc.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;

/**
 * The <tt>ChatRoomQuery</tt> is a query over the
 * <tt>ChatRoomContactSourceService</tt>.
 * 
 * @author Hristo Terezov
 */
public class ChatRoomQuery
    extends AsyncContactQuery<ContactSourceService>
    implements LocalUserChatRoomPresenceListener, ChatRoomListChangeListener
{
    /**
     * The query string.
     */
    private String queryString;

    /**
     * The contact count.
     */
    private int count = 0;
    
    /**
     * Creates an instance of <tt>ChatRoomQuery</tt> by specifying
     * the parent contact source, the query string to match and the maximum
     * result contacts to return.
     *
     * @param contactSource the parent contact source
     * @param queryString the query string to match
     * @param count the maximum result contact count
     */
    public ChatRoomQuery(String queryString,
        int count, ChatRoomContactSourceService contactSource)
    {
        super(contactSource,
            Pattern.compile(queryString, Pattern.CASE_INSENSITIVE
                            | Pattern.LITERAL), true);
        this.count = count;
        this.queryString = queryString;
        for(ProtocolProviderService pps : MUCActivator
            .getChatRoomProviders())
        {
            OperationSetMultiUserChat opSetMUC 
                = pps.getOperationSet(OperationSetMultiUserChat.class);
            if(opSetMUC != null)
            {
                opSetMUC.addPresenceListener(this);
            }
        }
        
        MUCActivator.getMUCService().addChatRoomListChangeListener(this);
    }
    
    @Override
    protected void run()
    {
        Iterator<ChatRoomProviderWrapper> chatRoomProviders
            = MUCActivator
                .getMUCService().getChatRoomList().getChatRoomProviders();
        while (chatRoomProviders.hasNext())
        {
            ChatRoomProviderWrapper provider = chatRoomProviders.next();
            for(int i = 0; i < provider.countChatRooms(); i++)
            {
                if(count > 0 && getQueryResultCount() > count)
                {
                    if (getStatus() != QUERY_CANCELED)
                        setStatus(QUERY_COMPLETED);
                    return;
                }
                ChatRoomWrapper chatRoom = provider.getChatRoom(i);
                addChatRoom( provider.getProtocolProvider(), 
                    chatRoom.getChatRoomName(), chatRoom.getChatRoomID());
            }
        }
        if (getStatus() != QUERY_CANCELED)
            setStatus(QUERY_COMPLETED);
    }
    
    /**
     * Handles chat room presence status updates.
     * 
     * @param evt the <tt>LocalUserChatRoomPresenceChangeEvent</tt> instance 
     * containing the chat room and the type, and reason of the change
     */
    @Override
    public void localUserPresenceChanged(
        LocalUserChatRoomPresenceChangeEvent evt)
    {
        ChatRoom sourceChatRoom = evt.getChatRoom();
    
        String eventType = evt.getEventType();
        
        boolean existingContact = false;
        SourceContact foundContact = null;
        for(SourceContact contact : getQueryResults())
        {
            if(contact.getContactAddress().equals(sourceChatRoom.getName()))
            {
                existingContact = true;
                foundContact = contact;
                break;
            }
        }
        
        if (LocalUserChatRoomPresenceChangeEvent
            .LOCAL_USER_JOINED.equals(eventType))
        {
            if(existingContact)
            {
                ((ChatRoomSourceContact)foundContact).setPresenceStatus(
                    ChatRoomPresenceStatus.CHAT_ROOM_ONLINE);
                fireContactChanged(foundContact);
            }
            else
            {
                if(queryString == null
                    || ((sourceChatRoom.getName().startsWith(
                                    queryString)
                            || sourceChatRoom.getIdentifier().startsWith(queryString)
                            )))
                    addQueryResult(
                        new ChatRoomSourceContact(sourceChatRoom,this));
            }
        }
        else if ((LocalUserChatRoomPresenceChangeEvent
                        .LOCAL_USER_LEFT.equals(eventType)
                    || LocalUserChatRoomPresenceChangeEvent
                            .LOCAL_USER_KICKED.equals(eventType)
                    || LocalUserChatRoomPresenceChangeEvent
                            .LOCAL_USER_DROPPED.equals(eventType)) 
                    )
        {
            if(existingContact)
            {
                ((ChatRoomSourceContact)foundContact)
                    .setPresenceStatus(
                        ChatRoomPresenceStatus.CHAT_ROOM_OFFLINE);
                fireContactChanged(foundContact);
            }
        }
    }
    
    /**
     * Adds found result to the query results.
     * 
     * @param pps the protocol provider associated with the found chat room.
     * @param chatRoomName the name of the chat room.
     * @param chatRoomID the id of the chat room.
     */
    private void addChatRoom(ProtocolProviderService pps, 
        String chatRoomName, String chatRoomID)
    {
        if(queryString == null
            || ((chatRoomName.startsWith(
                            queryString)
                    || chatRoomID.startsWith(queryString)
                    )))
        {
            addQueryResult(
                new ChatRoomSourceContact(chatRoomName, chatRoomID, this, pps));
        }
    }

    /**
     * Indicates that a change has occurred in the chat room data list.
     * @param evt the event that describes the change.
     */
    @Override
    public void contentChanged(ChatRoomListChangeEvent evt)
    {
        ChatRoomWrapper chatRoom = evt.getSourceChatRoom();
        
        switch(evt.getEventID())
        {
            case ChatRoomListChangeEvent.CHAT_ROOM_ADDED:
                if(queryString == null
                    || ((chatRoom.getChatRoomName().startsWith(
                                    queryString)
                            || chatRoom.getChatRoomID().startsWith(queryString)
                            )))
                    addQueryResult(
                        new ChatRoomSourceContact(chatRoom.getChatRoom(),this));
                break;
            case ChatRoomListChangeEvent.CHAT_ROOM_REMOVED:
                for(SourceContact contact : getQueryResults())
                {
                    if(contact.getContactAddress().equals(chatRoom.getChatRoomName()))
                    {
                        fireContactRemoved(contact);
                        break;
                    }
                }
                break;
            default:
                break;
        }
    }
}