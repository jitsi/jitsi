/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.sip.communicator.impl.muc;

import java.util.*;
import java.util.regex.*;

import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.muc.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;

import org.osgi.framework.*;

/**
 * The <tt>ChatRoomQuery</tt> is a query over the
 * <tt>ChatRoomContactSourceService</tt>.
 * 
 * @author Hristo Terezov
 */
public class ChatRoomQuery
    extends AsyncContactQuery<ContactSourceService>
    implements LocalUserChatRoomPresenceListener,
               ChatRoomListChangeListener,
               ChatRoomProviderWrapperListener
{
    /**
     * The query string.
     */
    private String queryString;

    /**
     * List with the current results for the query.
     */
    private Set<ChatRoomSourceContact> contactResults
        = new TreeSet<ChatRoomSourceContact>();

    /**
     * MUC service.
     */
    private MUCServiceImpl mucService;

    /**
     * The number of contact query listeners.
     */
    private int contactQueryListenersCount = 0;

    /**
     * The protocol provider registration listener.
     */
    private ServiceListener protolProviderRegistrationListener = null;

    /**
     * Creates an instance of <tt>ChatRoomQuery</tt> by specifying
     * the parent contact source, the query string to match and the maximum
     * result contacts to return.
     *
     * @param queryString the query string to match
     * @param contactSource the parent contact source
     */
    public ChatRoomQuery(String queryString, 
        ChatRoomContactSourceService contactSource)
    {
        super(contactSource,
            Pattern.compile(queryString, Pattern.CASE_INSENSITIVE
                            | Pattern.LITERAL), true);
        this.queryString = queryString;
        
        mucService = MUCActivator.getMUCService();
        
    }

    /**
     * Adds listeners for the query
     */
    private void initListeners()
    {
        for(ProtocolProviderService pps : MUCActivator.getChatRoomProviders())
        {
            addQueryToProviderPresenceListeners(pps);
        }
        
        mucService.addChatRoomListChangeListener(this);
        mucService.addChatRoomProviderWrapperListener(this);
        protolProviderRegistrationListener = new ProtocolProviderRegListener();
        MUCActivator.bundleContext.addServiceListener(
            protolProviderRegistrationListener);
    }

    /**
     * Adds the query as presence listener to protocol provider service.
     * @param pps the protocol provider service.
     */
    public void addQueryToProviderPresenceListeners(ProtocolProviderService pps)
    {
        OperationSetMultiUserChat opSetMUC 
            = pps.getOperationSet(OperationSetMultiUserChat.class);
        if(opSetMUC != null)
        {
            opSetMUC.addPresenceListener(this);
        }
    }

    /**
     * Removes the query from protocol provider service presence listeners.
     * @param pps the protocol provider service.
     */
    public void removeQueryFromProviderPresenceListeners(
        ProtocolProviderService pps)
    {
        OperationSetMultiUserChat opSetMUC 
            = pps.getOperationSet(OperationSetMultiUserChat.class);
        if(opSetMUC != null)
        {
            opSetMUC.removePresenceListener(this);
        }
    }

    @Override
    protected void run()
    {
        Iterator<ChatRoomProviderWrapper> chatRoomProviders
            = mucService.getChatRoomProviders();
        
        while (chatRoomProviders.hasNext())
        {
            ChatRoomProviderWrapper provider = chatRoomProviders.next();
            providerAdded(provider, true);
        }
        
        if (getStatus() != QUERY_CANCELED)
            setStatus(QUERY_COMPLETED);
    }

    /**
     * Handles adding a chat room provider.
     * @param provider the provider.
     * @param addQueryResult indicates whether we should add the chat room to 
     * the query results or fire an event without adding it to the results. 
     */
    private void providerAdded(ChatRoomProviderWrapper provider, 
        boolean addQueryResult)
    {
       
        for(int i = 0; i < provider.countChatRooms(); i++)
        {
            ChatRoomWrapper chatRoom = provider.getChatRoom(i);
            addChatRoom( provider.getProtocolProvider(), 
                chatRoom.getChatRoomName(), chatRoom.getChatRoomID(), 
                addQueryResult, chatRoom.isAutojoin());
        }
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
        ChatRoomSourceContact foundContact = null;
        synchronized (contactResults)
        {
            for (ChatRoomSourceContact contact : contactResults)
            {
                if (contactEqualsChatRoom(contact, sourceChatRoom))
                {
                    existingContact = true;
                    foundContact = contact;
                    contactResults.remove(contact);
                    break;
                }
            }
        }

        if (LocalUserChatRoomPresenceChangeEvent
            .LOCAL_USER_JOINED.equals(eventType))
        {
            if(existingContact)
            {
                foundContact.setPresenceStatus(
                    ChatRoomPresenceStatus.CHAT_ROOM_ONLINE);
                synchronized (contactResults)
                {
                    contactResults.add(foundContact);
                }
                fireContactChanged(foundContact);
            }
            else
            {
                ChatRoomWrapper chatRoom 
                    = MUCActivator.getMUCService()
                        .findChatRoomWrapperFromChatRoom(sourceChatRoom);
                if(chatRoom != null)
                    addChatRoom(sourceChatRoom, false, chatRoom.isAutojoin());
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
                foundContact.setPresenceStatus(
                        ChatRoomPresenceStatus.CHAT_ROOM_OFFLINE);
                synchronized (contactResults)
                {
                    contactResults.add(foundContact);
                }
                fireContactChanged(foundContact);
            }
        }
    }

    /**
     * Adds found result to the query results.
     * 
     * @param room the chat room.
     * @param addQueryResult indicates whether we should add the chat room to
     * the query results or fire an event without adding it to the results.
     * @param isAutoJoin the auto join state of the contact.
     */
    private void addChatRoom(ChatRoom room, boolean addQueryResult, 
        boolean isAutoJoin)
    {
        if(queryString == null
            || ((room.getName().contains(
                            queryString)
                    || room.getIdentifier().contains(queryString)
                    )))
        {
            ChatRoomSourceContact contact 
                = new ChatRoomSourceContact(room, this, isAutoJoin);
            synchronized (contactResults)
            {
                contactResults.add(contact);
            }
            
            if(addQueryResult)
            {
                addQueryResult(contact, false);
            }
            else
            {
                fireContactReceived(contact, false);
            }
        }
    }

    /**
     * Adds found result to the query results.
     * 
     * @param pps the protocol provider associated with the found chat room.
     * @param chatRoomName the name of the chat room.
     * @param chatRoomID the id of the chat room.
     * @param addQueryResult indicates whether we should add the chat room to 
     * the query results or fire an event without adding it to the results.
     * @param isAutoJoin the auto join state of the contact.
     */
    private void addChatRoom(ProtocolProviderService pps, 
        String chatRoomName, String chatRoomID, boolean addQueryResult, 
        boolean isAutoJoin)
    {
        if(queryString == null
            || ((chatRoomName.contains(
                            queryString)
                    || chatRoomID.contains(queryString)
                    )))
        {
            ChatRoomSourceContact contact 
                = new ChatRoomSourceContact(chatRoomName, chatRoomID, this, pps,
                    isAutoJoin);
            synchronized (contactResults)
            {
                contactResults.add(contact);
            }
            
            if(addQueryResult)
            {
                addQueryResult(contact, false);
            }
            else
            {
                fireContactReceived(contact, false);
            }
        }
    }

    /**
     * Indicates that a change has occurred in the chat room data list.
     * @param evt the event that describes the change.
     */
    @Override
    public void contentChanged(final ChatRoomListChangeEvent evt)
    {
        ChatRoomWrapper chatRoom = evt.getSourceChatRoom();
        switch (evt.getEventID())
        {
            case ChatRoomListChangeEvent.CHAT_ROOM_ADDED:
                addChatRoom(chatRoom.getChatRoom(), false,
                    chatRoom.isAutojoin());
                break;
            case ChatRoomListChangeEvent.CHAT_ROOM_REMOVED:
                LinkedList<ChatRoomSourceContact> tmpContactResults;
                synchronized (contactResults)
                {
                    tmpContactResults
                        = new LinkedList<ChatRoomSourceContact>(contactResults);

                    for (ChatRoomSourceContact contact : tmpContactResults)
                    {
                    if (contactEqualsChatRoom(contact, chatRoom))
                        {
                            contactResults.remove(contact);
                            fireContactRemoved(contact);
                            break;
                        }
                    }
                }
                break;
            case ChatRoomListChangeEvent.CHAT_ROOM_CHANGED:
                synchronized (contactResults)
                {
                    for (ChatRoomSourceContact contact : contactResults)
                    {
                        if (contactEqualsChatRoom(contact,
                            chatRoom.getChatRoom()))
                        {
                            if (chatRoom.isAutojoin() != contact.isAutoJoin())
                            {
                                contact.setAutoJoin(chatRoom.isAutojoin());
                                fireContactChanged(contact);
                            }
                            break;
                        }
                    }
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void chatRoomProviderWrapperAdded(ChatRoomProviderWrapper provider)
    {
        providerAdded(provider, false);
    }

    @Override
    public void chatRoomProviderWrapperRemoved(ChatRoomProviderWrapper provider)
    {
        LinkedList<ChatRoomSourceContact> tmpContactResults;
        synchronized (contactResults)
        {
            tmpContactResults 
                = new LinkedList<ChatRoomSourceContact>(contactResults);
        
            for(ChatRoomSourceContact contact : tmpContactResults)
            {
                if(contact.getProvider().equals(provider.getProtocolProvider()))
                {
                    contactResults.remove(contact);
                    fireContactRemoved(contact);
                }
            }
        }
    }

    /**
     * Test equality of contact to chat room. This test recognizes that chat
     * rooms may have equal names but connected to different accounts.
     *
     * @param contact the contact
     * @param chatRoom the chat room
     * @return returns <tt>true</tt> if they are equal, or <tt>false</tt> if
     *         they are different
     */
    private boolean contactEqualsChatRoom(final ChatRoomSourceContact contact,
        final ChatRoom chatRoom)
    {
        return contact.getProvider() == chatRoom.getParentProvider()
            && chatRoom.getIdentifier().equals(contact.getContactAddress());
    }

    /**
     * Test equality of contact to chat room wrapper. This method does not rely
     * on a chat room instance, since that may not be available in case of
     * removal.
     *
     * @param contact the contact
     * @param chatRoomWrapper the chat room wrapper
     * @return returns <tt>true</tt> if they are equal, or <tt>false</tt> if
     *         they are different.
     */
    private boolean contactEqualsChatRoom(final ChatRoomSourceContact contact,
        final ChatRoomWrapper chatRoomWrapper)
    {
        return contact.getProvider() == chatRoomWrapper.getParentProvider()
            .getProtocolProvider()
            && contact.getContactAddress().equals(
                chatRoomWrapper.getChatRoomID());
    }

    /**
     * Returns the index of the contact in the contact results list.
     * @param contact the contact.
     * @return the index of the contact in the contact results list.
     */
    public synchronized int indexOf(ChatRoomSourceContact contact)
    {
       Iterator<ChatRoomSourceContact> it = contactResults.iterator();
       int i = 0;
       while(it.hasNext())
       {
           if(contact.equals(it.next()))
           {
               return i;
           }
           i++;
       }
       return -1;
    }

    /**
     * Clears any listener we used.
     */
    private void clearListeners()
    {
        mucService.removeChatRoomListChangeListener(this);
        mucService.removeChatRoomProviderWrapperListener(this);
        if(protolProviderRegistrationListener != null)
            MUCActivator.bundleContext.removeServiceListener(
                protolProviderRegistrationListener);
        protolProviderRegistrationListener = null;
        for(ProtocolProviderService pps : MUCActivator.getChatRoomProviders())
        {
            removeQueryFromProviderPresenceListeners(pps);
        }
    }

    /**
     * Cancels this <tt>ContactQuery</tt>.
     *
     * @see ContactQuery#cancel()
     */
    public void cancel()
    {
        clearListeners();

        super.cancel();
    }

    /**
     * If query has status changed to cancel, let's clear listeners.
     * @param status {@link ContactQuery#QUERY_CANCELED},
     * {@link ContactQuery#QUERY_COMPLETED}
     */
    public void setStatus(int status)
    {
        if(status == QUERY_CANCELED)
            clearListeners();

        super.setStatus(status);
    }

    @Override
    public void addContactQueryListener(ContactQueryListener l)
    {
        super.addContactQueryListener(l);
        contactQueryListenersCount++;
        if(contactQueryListenersCount == 1)
        {
            initListeners();
        }
    }

    @Override
    public void removeContactQueryListener(ContactQueryListener l)
    {
        super.removeContactQueryListener(l);
        contactQueryListenersCount--;
        if(contactQueryListenersCount == 0)
        {
            clearListeners();
        }
    }

    /**
     * Listens for <tt>ProtocolProviderService</tt> registrations.
     */
    private class ProtocolProviderRegListener
        implements ServiceListener
    {
        /**
         * Handles service change events.
         */
        public void serviceChanged(ServiceEvent event)
        {
            ServiceReference serviceRef = event.getServiceReference();

            // if the event is caused by a bundle being stopped, we don't want to
            // know
            if (serviceRef.getBundle().getState() == Bundle.STOPPING)
            {
                return;
            }

            Object service = MUCActivator.bundleContext.getService(serviceRef);

            // we don't care if the source service is not a protocol provider
            if (!(service instanceof ProtocolProviderService))
            {
                return;
            }

            switch (event.getType())
            {
            case ServiceEvent.REGISTERED:
                    addQueryToProviderPresenceListeners(
                        (ProtocolProviderService) service);
                break;
            case ServiceEvent.UNREGISTERING:
                    removeQueryFromProviderPresenceListeners(
                        (ProtocolProviderService) service);
                break;
            }
        }
    }
}
