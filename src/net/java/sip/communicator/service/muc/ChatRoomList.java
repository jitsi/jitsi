/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.muc;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;

/**
 * The <tt>ChatRoomsList</tt> is the list containing all chat rooms.
 *
 * @author Yana Stamcheva
 * @author Hristo Terezov
 */
public interface ChatRoomList
    extends RegistrationStateChangeListener
{

    /**
     * Initializes the list of chat rooms.
     */
    public void loadList();

    /**
     * Adds a listener to wait for provider to be registered or unregistered.
     *
     * @param pps the <tt>ProtocolProviderService</tt> corresponding to the chat
     * server
     */
    public void addChatProvider(ProtocolProviderService pps);

    /**
     * Removes the corresponding server and all related chat rooms from this
     * list.
     *
     * @param pps the <tt>ProtocolProviderService</tt> corresponding to the
     *            server to remove
     */
    public void removeChatProvider(ProtocolProviderService pps);


    /**
     * Adds a chat room to this list.
     *
     * @param chatRoomWrapper the <tt>ChatRoom</tt> to add
     */
    public void addChatRoom(ChatRoomWrapper chatRoomWrapper);

    /**
     * Removes the given <tt>ChatRoom</tt> from the list of all chat rooms.
     *
     * @param chatRoomWrapper the <tt>ChatRoomWrapper</tt> to remove
     */
    public void removeChatRoom(ChatRoomWrapper chatRoomWrapper);

    /**
     * Returns the <tt>ChatRoomWrapper</tt> that correspond to the given
     * <tt>ChatRoom</tt>. If the list of chat rooms doesn't contain a
     * corresponding wrapper - returns null.
     *
     * @param chatRoom the <tt>ChatRoom</tt> that we're looking for
     * @return the <tt>ChatRoomWrapper</tt> object corresponding to the given
     * <tt>ChatRoom</tt>
     */
    public ChatRoomWrapper findChatRoomWrapperFromChatRoom(ChatRoom chatRoom);
    
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
        ProtocolProviderService pps);

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
        ProtocolProviderService protocolProvider);

    /**
     * Goes through the locally stored chat rooms list and for each
     * {@link ChatRoomWrapper} tries to find the corresponding server stored
     * {@link ChatRoom} in the specified operation set. Joins automatically all
     * found chat rooms.
     *
     * @param protocolProvider the protocol provider for the account to
     * synchronize
     * @param opSet the multi user chat operation set, which give us access to
     * chat room server
     */
    public void synchronizeOpSetWithLocalContactList(
        ProtocolProviderService protocolProvider,
        final OperationSetMultiUserChat opSet);

    /**
     * Returns an iterator to the list of chat room providers.
     *
     * @return an iterator to the list of chat room providers.
     */
    public Iterator<ChatRoomProviderWrapper> getChatRoomProviders();

    /**
     * Adds a ChatRoomProviderWrapperListener to the listener list.
     *
     * @param listener the ChatRoomProviderWrapperListener to be added
     */
    public void addChatRoomProviderWrapperListener(
        ChatRoomProviderWrapperListener listener);

    /**
     * Removes a ChatRoomProviderWrapperListener from the listener list.
     *
     * @param listener the ChatRoomProviderWrapperListener to be removed
     */
    public void removeChatRoomProviderWrapperListener(
        ChatRoomProviderWrapperListener listener);

    /**
     * Listens for changes of providers registration state, so we can use only
     * registered providers.
     * @param evt a <tt>RegistrationStateChangeEvent</tt> which describes the
     *            event that occurred.
     */
    @Override
    public void registrationStateChanged(RegistrationStateChangeEvent evt);

    /**
     * Listener which registers for provider add/remove changes.
     */
    public static interface ChatRoomProviderWrapperListener
    {
        /**
         * When a provider wrapper is added this method is called to inform
         * listeners.
         * @param provider which was added.
         */
        public void chatRoomProviderWrapperAdded(
            ChatRoomProviderWrapper provider);

        /**
         * When a provider wrapper is removed this method is called to inform
         * listeners.
         * @param provider which was removed.
         */
        public void chatRoomProviderWrapperRemoved(
            ChatRoomProviderWrapper provider);
    }
}
