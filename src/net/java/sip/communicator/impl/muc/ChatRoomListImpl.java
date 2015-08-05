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

import net.java.sip.communicator.service.muc.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.configuration.*;
import org.osgi.framework.*;

/**
 * The <tt>ChatRoomsList</tt> is the list containing all chat rooms.
 *
 * @author Yana Stamcheva
 * @author Hristo Terezov
 */
public class ChatRoomListImpl
    implements RegistrationStateChangeListener, ServiceListener
{
    /**
     * The logger.
     */
    private static final Logger logger = Logger.getLogger(ChatRoomListImpl.class);

    /**
     * The list containing all chat servers and rooms.
     */
    private final List<ChatRoomProviderWrapper> providersList
        = new Vector<ChatRoomProviderWrapper>();

    /**
     * All ChatRoomProviderWrapperListener change listeners registered so far.
     */
    private final List<ChatRoomProviderWrapperListener> providerChangeListeners
        = new ArrayList<ChatRoomProviderWrapperListener>();
    
    /**
     * A list of all <tt>ChatRoomListChangeListener</tt>-s.
     */
    private final Vector<ChatRoomListChangeListener> listChangeListeners
        = new Vector<ChatRoomListChangeListener>();

    /**
     * Constructs and initializes new <tt>ChatRoomListImpl</tt> objects. Adds 
     * the created object as service lister to the bundle context.
     */
    public ChatRoomListImpl()
    {
        loadList();
        MUCActivator.bundleContext.addServiceListener(this);
    }
    
    /**
     * Initializes the list of chat rooms.
     */
    public void loadList()
    {
        try
        {
            ServiceReference[] serRefs
                = MUCActivator.bundleContext.getServiceReferences(
                                        ProtocolProviderService.class.getName(),
                                        null);

            // If we don't have providers at this stage we just return.
            if (serRefs == null)
                return;

            for (ServiceReference serRef : serRefs)
            {
                ProtocolProviderService protocolProvider
                    = (ProtocolProviderService)
                        MUCActivator.bundleContext.getService(serRef);

                Object multiUserChatOpSet
                    = protocolProvider
                        .getOperationSet(OperationSetMultiUserChat.class);

                if (multiUserChatOpSet != null)
                {
                    this.addChatProvider(protocolProvider);
                }
            }
        }
        catch (InvalidSyntaxException e)
        {
            logger.error("Failed to obtain service references.", e);
        }
    }

    /**
     * Adds the given <tt>ChatRoomListChangeListener</tt> that will listen for
     * all changes of the chat room list data model.
     *
     * @param l the listener to add.
     */
    public void addChatRoomListChangeListener(ChatRoomListChangeListener l)
    {
        synchronized (listChangeListeners)
        {
            listChangeListeners.add(l);
        }
    }

    /**
     * Removes the given <tt>ChatRoomListChangeListener</tt>.
     *
     * @param l the listener to remove.
     */
    public void removeChatRoomListChangeListener(ChatRoomListChangeListener l)
    {
        synchronized (listChangeListeners)
        {
            listChangeListeners.remove(l);
        }
    }
    

    /**
     * Notifies all interested listeners that a change in the chat room list
     * model has occurred.
     * @param chatRoomWrapper the chat room wrapper that identifies the chat
     * room
     * @param eventID the identifier of the event
     */
    public void fireChatRoomListChangedEvent(  ChatRoomWrapper chatRoomWrapper,
                                                int eventID)
    {
        ChatRoomListChangeEvent evt
            = new ChatRoomListChangeEvent(chatRoomWrapper, eventID);

        for (ChatRoomListChangeListener l : listChangeListeners)
        {
            l.contentChanged(evt);
        }
    }
    
    /**
     * Adds a chat server which is registered and all its existing chat rooms.
     *
     * @param pps the <tt>ProtocolProviderService</tt> corresponding to the chat
     * server
     */
    ChatRoomProviderWrapper
        addRegisteredChatProvider(ProtocolProviderService pps)
    {
        ChatRoomProviderWrapper chatRoomProvider
            = new ChatRoomProviderWrapperImpl(pps);

        providersList.add(chatRoomProvider);

        ConfigurationService configService
            = MUCActivator.getConfigurationService();

        String prefix = "net.java.sip.communicator.impl.gui.accounts";

        List<String> accounts =
            configService.getPropertyNamesByPrefix(prefix, true);

        for (String accountRootPropName : accounts) {
            String accountUID
                = configService.getString(accountRootPropName);

            if(accountUID.equals(pps
                    .getAccountID().getAccountUniqueID()))
            {
                List<String> chatRooms = configService
                    .getPropertyNamesByPrefix(
                        accountRootPropName + ".chatRooms", true);

                for (String chatRoomPropName : chatRooms)
                {
                    String chatRoomID
                        = configService.getString(chatRoomPropName);

                    String chatRoomName = configService.getString(
                        chatRoomPropName + ".chatRoomName");

                    ChatRoomWrapper chatRoomWrapper
                        = new ChatRoomWrapperImpl(  chatRoomProvider,
                                                    chatRoomID,
                                                    chatRoomName);

                    chatRoomProvider.addChatRoom(chatRoomWrapper);
                }
            }
        }

        fireProviderWrapperAdded(chatRoomProvider);

        return chatRoomProvider;
    }

    /**
     * Adds a listener to wait for provider to be registered or unregistered.
     *
     * @param pps the <tt>ProtocolProviderService</tt> corresponding to the chat
     * server
     */
    public void addChatProvider(ProtocolProviderService pps)
    {
        if(pps.isRegistered())
            addRegisteredChatProvider(pps);
        else
            pps.addRegistrationStateChangeListener(this);
    }

    /**
     * Removes the corresponding server and all related chat rooms from this
     * list.
     *
     * @param pps the <tt>ProtocolProviderService</tt> corresponding to the
     *            server to remove
     */
    public void removeChatProvider(ProtocolProviderService pps)
    {
        ChatRoomProviderWrapper wrapper = findServerWrapperFromProvider(pps);

        if (wrapper != null)
            removeChatProvider(wrapper, true);
    }

    /**
     * Removes the corresponding server and all related chat rooms from this
     * list.
     *
     * @param chatRoomProvider the <tt>ChatRoomProviderWrapper</tt>
     *            corresponding to the server to remove
     * @param permanently whether to remove any listener
     *                    and stored configuration
     */
    private void removeChatProvider(ChatRoomProviderWrapper chatRoomProvider,
                                    boolean permanently)
    {
        providersList.remove(chatRoomProvider);

        if(permanently)
        {
            chatRoomProvider.getProtocolProvider()
                .removeRegistrationStateChangeListener(this);

            ConfigurationService configService
                = MUCActivator.getConfigurationService();
            String prefix = "net.java.sip.communicator.impl.gui.accounts";
            AccountID accountID =
                    chatRoomProvider.getProtocolProvider().getAccountID();

            // if provider is just disabled don't remove its stored rooms
            if(!MUCActivator.getAccountManager().getStoredAccounts()
                    .contains(accountID))
            {
                String providerAccountUID = accountID.getAccountUniqueID();

                for (String accountRootPropName
                        : configService.getPropertyNamesByPrefix(prefix, true))
                {
                    String accountUID
                        = configService.getString(accountRootPropName);

                    if(accountUID.equals(providerAccountUID))
                    {
                        List<String> chatRooms
                            = configService.getPropertyNamesByPrefix(
                                    accountRootPropName + ".chatRooms",
                                    true);

                        for (String chatRoomPropName : chatRooms)
                        {
                            configService.setProperty(
                                chatRoomPropName + ".chatRoomName",
                                null);
                        }

                        configService.setProperty(accountRootPropName, null);
                    }
                }
            }
        }
        
        for(int i = 0; i < chatRoomProvider.countChatRooms(); i++)
        {
            ChatRoomWrapper wrapper = chatRoomProvider.getChatRoom(i);
            MUCActivator.getUIService().closeChatRoomWindow(wrapper);

            // clears listeners added by chat room
            wrapper.removeListeners();
        }

        // clears listeners added by the system chat room
        chatRoomProvider.getSystemRoomWrapper().removeListeners();
        
        fireProviderWrapperRemoved(chatRoomProvider);
    }

    /**
     * Adds a chat room to this list.
     *
     * @param chatRoomWrapper the <tt>ChatRoom</tt> to add
     */
    public void addChatRoom(ChatRoomWrapper chatRoomWrapper)
    {
        ChatRoomProviderWrapper chatRoomProvider
            = chatRoomWrapper.getParentProvider();

        if (!chatRoomProvider.containsChatRoom(chatRoomWrapper))
            chatRoomProvider.addChatRoom(chatRoomWrapper);

        if (chatRoomWrapper.isPersistent())
        {
            ConfigurationUtils.saveChatRoom(
                chatRoomProvider.getProtocolProvider(),
                chatRoomWrapper.getChatRoomID(),
                chatRoomWrapper.getChatRoomID(),
                chatRoomWrapper.getChatRoomName());
        }
        
        fireChatRoomListChangedEvent(
            chatRoomWrapper,
            ChatRoomListChangeEvent.CHAT_ROOM_ADDED);
    }

    /**
     * Removes the given <tt>ChatRoom</tt> from the list of all chat rooms.
     *
     * @param chatRoomWrapper the <tt>ChatRoomWrapper</tt> to remove
     */
    public void removeChatRoom(ChatRoomWrapper chatRoomWrapper)
    {
        ChatRoomProviderWrapper chatRoomProvider
            = chatRoomWrapper.getParentProvider();

        if (providersList.contains(chatRoomProvider))
        {
            chatRoomProvider.removeChatRoom(chatRoomWrapper);

            ConfigurationUtils.saveChatRoom(
                chatRoomProvider.getProtocolProvider(),
                chatRoomWrapper.getChatRoomID(),
                null,   // The new identifier.
                null);   // The name of the chat room.
            
            chatRoomWrapper.removeListeners();
            fireChatRoomListChangedEvent(
                chatRoomWrapper,
                ChatRoomListChangeEvent.CHAT_ROOM_REMOVED);
        }
    }

    /**
     * Returns the <tt>ChatRoomWrapper</tt> that correspond to the given
     * <tt>ChatRoom</tt>. If the list of chat rooms doesn't contain a
     * corresponding wrapper - returns null.
     *
     * @param chatRoom the <tt>ChatRoom</tt> that we're looking for
     * @return the <tt>ChatRoomWrapper</tt> object corresponding to the given
     * <tt>ChatRoom</tt>
     */
    public ChatRoomWrapper findChatRoomWrapperFromChatRoom(ChatRoom chatRoom)
    {
        for (ChatRoomProviderWrapper provider : providersList)
        {
            // check only for the right PP
            if(!chatRoom.getParentProvider()
                    .equals(provider.getProtocolProvider()))
                continue;

            ChatRoomWrapper systemRoomWrapper = provider.getSystemRoomWrapper();
            ChatRoom systemRoom = systemRoomWrapper.getChatRoom();

            if ((systemRoom != null) && systemRoom.equals(chatRoom))
            {
                return systemRoomWrapper;
            }
            else
            {
                ChatRoomWrapper chatRoomWrapper
                    = provider.findChatRoomWrapperForChatRoom(chatRoom);

                if (chatRoomWrapper != null)
                {
                    // stored chatrooms has no chatroom, but their
                    // id is the same as the chatroom we are searching wrapper
                    // for. Also during reconnect we don't have the same chat
                    // id for another chat room object.
                    if(chatRoomWrapper.getChatRoom() == null
                        || !chatRoomWrapper.getChatRoom().equals(chatRoom))
                    {
                        chatRoomWrapper.setChatRoom(chatRoom);
                    }

                    return chatRoomWrapper;
                }
            }
        }

        return null;
    }
    
    /**
     * Returns the <tt>ChatRoomWrapper</tt> that correspond to the given id of 
     * chat room and provider. If the list of chat rooms doesn't contain a
     * corresponding wrapper - returns null.
     *
     * @param chatRoomID the id of <tt>ChatRoom</tt> that we're looking for
     * @param pps the procol provider associated with the chat room.
     * @return the <tt>ChatRoomWrapper</tt> object corresponding to the given id
     * of the chat room
     */
    public ChatRoomWrapper findChatRoomWrapperFromChatRoomID(String chatRoomID,
        ProtocolProviderService pps)
    {
        for (ChatRoomProviderWrapper provider : providersList)
        {
            // check only for the right PP
            if(!pps.equals(provider.getProtocolProvider()))
                continue;

            ChatRoomWrapper systemRoomWrapper = provider.getSystemRoomWrapper();
            ChatRoom systemRoom = systemRoomWrapper.getChatRoom();

            if ((systemRoom != null) 
                && systemRoom.getIdentifier().equals(chatRoomID))
            {
                return systemRoomWrapper;
            }
            else
            {
                ChatRoomWrapper chatRoomWrapper
                    = provider.findChatRoomWrapperForChatRoomID(chatRoomID);
                
                return chatRoomWrapper;
            }
        }

        return null;
    }

    /**
     * Returns the <tt>ChatRoomProviderWrapper</tt> that correspond to the
     * given <tt>ProtocolProviderService</tt>. If the list doesn't contain a
     * corresponding wrapper - returns null.
     *
     * @param protocolProvider the protocol provider that we're looking for
     * @return the <tt>ChatRoomProvider</tt> object corresponding to
     * the given <tt>ProtocolProviderService</tt>
     */
    public ChatRoomProviderWrapper findServerWrapperFromProvider(
        ProtocolProviderService protocolProvider)
    {
        for(ChatRoomProviderWrapper chatRoomProvider : providersList)
        {
            if(chatRoomProvider.getProtocolProvider().equals(protocolProvider))
            {
                return chatRoomProvider;
            }
        }

        return null;
    }

    /**
     * Returns an iterator to the list of chat room providers.
     *
     * @return an iterator to the list of chat room providers.
     */
    public Iterator<ChatRoomProviderWrapper> getChatRoomProviders()
    {
        return providersList.iterator();
    }

    /**
     * Adds a ChatRoomProviderWrapperListener to the listener list.
     *
     * @param listener the ChatRoomProviderWrapperListener to be added
     */
    public synchronized void addChatRoomProviderWrapperListener(
        ChatRoomProviderWrapperListener listener)
    {
        providerChangeListeners.add(listener);
    }

    /**
     * Removes a ChatRoomProviderWrapperListener from the listener list.
     *
     * @param listener the ChatRoomProviderWrapperListener to be removed
     */
    public synchronized void removeChatRoomProviderWrapperListener(
        ChatRoomProviderWrapperListener listener)
    {
        providerChangeListeners.remove(listener);
    }

    /**
     * Fire that chat room provider wrapper was added.
     * @param provider which was added.
     */
    private void fireProviderWrapperAdded(ChatRoomProviderWrapper provider)
    {
        if (providerChangeListeners != null)
        {
            for (ChatRoomProviderWrapperListener target : providerChangeListeners)
            {
                target.chatRoomProviderWrapperAdded(provider);
            }
        }
    }

    /**
     * Fire that chat room provider wrapper was removed.
     * @param provider which was removed.
     */
    private void fireProviderWrapperRemoved(ChatRoomProviderWrapper provider)
    {
        if (providerChangeListeners != null)
        {
            for (ChatRoomProviderWrapperListener target : providerChangeListeners)
            {
                target.chatRoomProviderWrapperRemoved(provider);
            }
        }
    }

    /**
     * Listens for changes of providers registration state, so we can use only
     * registered providers.
     * @param evt a <tt>RegistrationStateChangeEvent</tt> which describes the
     *            event that occurred.
     */
    @Override
    public void registrationStateChanged(RegistrationStateChangeEvent evt)
    {
        ProtocolProviderService pps = evt.getProvider();

        if (evt.getNewState() == RegistrationState.REGISTERED)
        {
            // will use synchronizeOpSetWithLocalContactList
            // to avoid any concurrency
        }
        else if(evt.getNewState() == RegistrationState.UNREGISTERED
                 || evt.getNewState() == RegistrationState.AUTHENTICATION_FAILED
                 || evt.getNewState() == RegistrationState.CONNECTION_FAILED)
        {
            ChatRoomProviderWrapper wrapper =
                findServerWrapperFromProvider(pps);

            if (wrapper != null)
            {
                removeChatProvider(wrapper, false);
            }
        }
    }

    @Override
    public void serviceChanged(ServiceEvent event)
    {
        // if the event is caused by a bundle being stopped, we don't want to
        // know
        if (event.getServiceReference().getBundle().getState()
                == Bundle.STOPPING)
            return;

        Object service = MUCActivator.bundleContext.getService(event
            .getServiceReference());

        // we don't care if the source service is not a protocol provider
        if (!(service instanceof ProtocolProviderService))
            return;

        ProtocolProviderService protocolProvider
            = (ProtocolProviderService) service;

        Object multiUserChatOpSet
            = protocolProvider
                .getOperationSet(OperationSetMultiUserChat.class);

         if (multiUserChatOpSet != null)
        {
             if (event.getType() == ServiceEvent.REGISTERED)
             {
                 addChatProvider(protocolProvider);
             }
             else if (event.getType() == ServiceEvent.UNREGISTERING)
             {
                 removeChatProvider(protocolProvider);
             }
        }
    }
}
