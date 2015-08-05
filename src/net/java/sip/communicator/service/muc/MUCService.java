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
package net.java.sip.communicator.service.muc;

import java.util.*;

import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * The MUC service provides interface for the chat rooms. It connects the GUI
 * with the protcol.
 *
 * @author Hristo Terezov
 */
public abstract class MUCService
{
    /**
     * The configuration property to disable
     */
    public static final String DISABLED_PROPERTY
        = "net.java.sip.communicator.impl.muc.MUC_SERVICE_DISABLED";

    /**
     * Key for auto-open configuration entry.
     */
    private static String AUTO_OPEN_CONFIG_KEY = "openAutomatically";

    /**
     * The value for chat room configuration property to open automatically on
     * activity
     */
    public static String OPEN_ON_ACTIVITY = "on_activity";

    /**
     * The value for chat room configuration property to open automatically on
     * message
     */
    public static String OPEN_ON_MESSAGE = "on_message";

    /**
     * The value for chat room configuration property to open automatically on
     * important messages.
     */
    public static String OPEN_ON_IMPORTANT_MESSAGE = "on_important_message";

    /**
     * The default for chat room auto-open behaviour.
     */
    public static String DEFAULT_AUTO_OPEN_BEHAVIOUR = OPEN_ON_MESSAGE;

    /**
     * Map for the auto open configuration values and their text representation
     */
    public static Map<String, String> autoOpenConfigValuesTexts
        = new HashMap<String, String>();

    static
    {
        autoOpenConfigValuesTexts.put(OPEN_ON_ACTIVITY,
            "service.gui.OPEN_ON_ACTIVITY");
        autoOpenConfigValuesTexts.put(OPEN_ON_MESSAGE,
            "service.gui.OPEN_ON_MESSAGE");
        autoOpenConfigValuesTexts.put(OPEN_ON_IMPORTANT_MESSAGE,
            "service.gui.OPEN_ON_IMPORTANT_MESSAGE");
    }

    /**
     * Sets chat room open automatically property
     * @param pps the provider
     * @param chatRoomId the chat room id
     * @param value the new value for the property
     */
    public static void setChatRoomAutoOpenOption(
        ProtocolProviderService pps,
        String chatRoomId,
        String value)
    {
        ConfigurationUtils.updateChatRoomProperty(
            pps,
            chatRoomId, AUTO_OPEN_CONFIG_KEY, value);
    }

    /**
     * Returns the value of the chat room open automatically property
     * @param pps the provider
     * @param chatRoomId the chat room id
     * @return the value of the chat room open automatically property
     */
    public static String getChatRoomAutoOpenOption(
        ProtocolProviderService pps,
        String chatRoomId)
    {
        return ConfigurationUtils.getChatRoomProperty(
            pps,
            chatRoomId, AUTO_OPEN_CONFIG_KEY);
    }

    /**
     * Fires a <tt>ChatRoomListChangedEvent</tt> event.
     *
     * @param chatRoomWrapper the chat room.
     * @param eventID the id of the event.
     */
    public abstract void fireChatRoomListChangedEvent(
        ChatRoomWrapper chatRoomWrapper, int eventID);

    /**
    * Joins the given chat room with the given password and manages all the
    * exceptions that could occur during the join process.
    *
    * @param chatRoomWrapper the chat room to join.
    * @param nickName the nickname we choose for the given chat room.
    * @param password the password.
    * @param subject the subject which will be set to the room after the user
    * join successful.
    */
    public abstract void joinChatRoom(   ChatRoomWrapper chatRoomWrapper,
        String nickName, byte[] password, String subject);

    /**
    * Creates a chat room, by specifying the chat room name, the parent
    * protocol provider and eventually, the contacts invited to participate in
    * this chat room.
    *
    * @param roomName the name of the room
    * @param protocolProvider the parent protocol provider.
    * @param contacts the contacts invited when creating the chat room.
    * @param reason
    * @param persistent is the room persistent
    * @return the <tt>ChatRoomWrapper</tt> corresponding to the created room
    */
    public abstract ChatRoomWrapper createChatRoom(String roomName,
        ProtocolProviderService protocolProvider, Collection<String> contacts,
        String reason, boolean persistent);



    /**
    * Creates a private chat room, by specifying the parent
    * protocol provider and eventually, the contacts invited to participate in
    * this chat room.
    *
    * @param protocolProvider the parent protocol provider.
    * @param contacts the contacts invited when creating the chat room.
    * @param reason
    * @param persistent is the room persistent
    * @return the <tt>ChatRoomWrapper</tt> corresponding to the created room
    */
    public abstract ChatRoomWrapper createPrivateChatRoom(
        ProtocolProviderService protocolProvider, Collection<String> contacts,
        String reason, boolean persistent);


    /**
    * Creates a chat room, by specifying the chat room name, the parent
    * protocol provider and eventually, the contacts invited to participate in
    * this chat room.
    *
    * @param roomName the name of the room
    * @param protocolProvider the parent protocol provider.
    * @param contacts the contacts invited when creating the chat room.
    * @param reason
    * @param join whether we should join the room after creating it.
    * @param persistent whether the newly created room will be persistent.
    * @param isPrivate whether the room will be private or public.
    * @return the <tt>ChatRoomWrapper</tt> corresponding to the created room or
    *         <tt>null</tt> if the protocol failed to create the chat room
    */
    public abstract ChatRoomWrapper createChatRoom(String roomName,
        ProtocolProviderService protocolProvider, Collection<String> contacts,
        String reason, boolean join, boolean persistent, boolean isPrivate);

    /**
    * Joins the room with the given name though the given chat room provider.
    *
    * @param chatRoomName the name of the room to join.
    * @param chatRoomProvider the chat room provider to join through.
    */
    public abstract void joinChatRoom(   String chatRoomName,
        ChatRoomProviderWrapper chatRoomProvider);

    /**
    * Returns existing chat rooms for the given <tt>chatRoomProvider</tt>.
    * @param chatRoomProvider the <tt>ChatRoomProviderWrapper</tt>, which
    * chat rooms we're looking for
    * @return  existing chat rooms for the given <tt>chatRoomProvider</tt>
    */
    public abstract List<String> getExistingChatRooms(
        ChatRoomProviderWrapper chatRoomProvider);


    /**
    * Called to accept an incoming invitation. Adds the invitation chat room
    * to the list of chat rooms and joins it.
    *
    * @param invitation the invitation to accept.
    */
    public abstract void acceptInvitation(ChatRoomInvitation invitation);

    /**
     * Rejects the given invitation with the specified reason.
     *
     * @param multiUserChatOpSet the operation set to use for rejecting the
     * invitation
     * @param invitation the invitation to reject
     * @param reason the reason for the rejection
     */
    public abstract void rejectInvitation(  OperationSetMultiUserChat multiUserChatOpSet,
                                   ChatRoomInvitation invitation,
                                   String reason);

    /**
     * Determines whether a specific <code>ChatRoom</code> is private i.e.
     * represents a one-to-one conversation which is not a channel. Since the
     * interface {@link ChatRoom} does not expose the private property, an
     * heuristic is used as a workaround: (1) a system <code>ChatRoom</code> is
     * obviously not private and (2) a <code>ChatRoom</code> is private if it
     * has only one <code>ChatRoomMember</code> who is not the local user.
     *
     * @param chatRoom
     *            the <code>ChatRoom</code> to be determined as private or not
     * @return <tt>true</tt> if the specified <code>ChatRoom</code> is private;
     *         otherwise, <tt>false</tt>
     */
    public static boolean isPrivate(ChatRoom chatRoom)
    {
        if (!chatRoom.isSystem()
            && chatRoom.isJoined()
            && (chatRoom.getMembersCount() == 1))
        {
            String nickname = chatRoom.getUserNickname();

            if (nickname != null)
            {
                for (ChatRoomMember member : chatRoom.getMembers())
                    if (nickname.equals(member.getName()))
                        return false;
                return true;
            }
        }
        return false;
    }

    /**
     * Leaves the given chat room.
     *
     * @param chatRoomWrapper the chat room to leave.
     * @return <tt>ChatRoomWrapper</tt> instance associated with the chat room.
     */
    public abstract ChatRoomWrapper leaveChatRoom(ChatRoomWrapper chatRoomWrapper);

    /**
     * Finds <tt>ChatRoomWrapper</tt> instance associated with the given source
     * contact.
     * @param contact the contact.
     * @return <tt>ChatRoomWrapper</tt> instance associated with the given
     * source contact.
     */
    public abstract ChatRoomWrapper findChatRoomWrapperFromSourceContact(
        SourceContact contact);

    /**
     * Searches for chat room wrapper in chat room list by chat room.
     *
     * @param chatRoom the chat room.
     * @param create if <tt>true</tt> and the chat room wrapper is not found new
     * chatRoomWrapper is created.
     * @return found chat room wrapper or the created chat room wrapper.
     */
    public abstract ChatRoomWrapper getChatRoomWrapperByChatRoom(
        ChatRoom chatRoom, boolean create);

    /**
     * Returns the multi user chat operation set for the given protocol provider.
     *
     * @param protocolProvider The protocol provider for which the multi user
     * chat operation set is about.
     * @return OperationSetMultiUserChat The telephony operation
     * set for the given protocol provider.
     */
    public static OperationSetMultiUserChat getMultiUserChatOpSet(
            ProtocolProviderService protocolProvider)
    {
        OperationSet opSet
            = protocolProvider.getOperationSet(OperationSetMultiUserChat.class);

        return (opSet instanceof OperationSetMultiUserChat)
            ? (OperationSetMultiUserChat) opSet
            : null;
    }

    /**
     * Finds the <tt>ChatRoomWrapper</tt> instance associated with the
     * chat room.
     * @param chatRoomID the id of the chat room.
     * @param pps the provider of the chat room.
     * @return the <tt>ChatRoomWrapper</tt> instance.
     */
    public abstract ChatRoomWrapper findChatRoomWrapperFromChatRoomID(
        String chatRoomID,
        ProtocolProviderService pps);

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
    public abstract void synchronizeOpSetWithLocalContactList(
        ProtocolProviderService protocolProvider,
        final OperationSetMultiUserChat opSet);

    /**
     * Returns an iterator to the list of chat room providers.
     *
     * @return an iterator to the list of chat room providers.
     */
    public abstract Iterator<ChatRoomProviderWrapper> getChatRoomProviders();

    /**
     * Removes the given <tt>ChatRoom</tt> from the list of all chat rooms.
     *
     * @param chatRoomWrapper the <tt>ChatRoomWrapper</tt> to remove
     */
    public abstract void removeChatRoom(ChatRoomWrapper chatRoomWrapper);

    /**
     * Adds a ChatRoomProviderWrapperListener to the listener list.
     *
     * @param listener the ChatRoomProviderWrapperListener to be added
     */
    public abstract void addChatRoomProviderWrapperListener(
        ChatRoomProviderWrapperListener listener);

    /**
     * Removes the ChatRoomProviderWrapperListener to the listener list.
     *
     * @param listener the ChatRoomProviderWrapperListener to be removed
     */
    public abstract void removeChatRoomProviderWrapperListener(
        ChatRoomProviderWrapperListener listener);

    /**
     * Returns the <tt>ChatRoomProviderWrapper</tt> that correspond to the
     * given <tt>ProtocolProviderService</tt>. If the list doesn't contain a
     * corresponding wrapper - returns null.
     *
     * @param protocolProvider the protocol provider that we're looking for
     * @return the <tt>ChatRoomProvider</tt> object corresponding to
     * the given <tt>ProtocolProviderService</tt>
     */
    public abstract ChatRoomProviderWrapper findServerWrapperFromProvider(
        ProtocolProviderService protocolProvider);

    /**
     * Returns the <tt>ChatRoomWrapper</tt> that correspond to the given
     * <tt>ChatRoom</tt>. If the list of chat rooms doesn't contain a
     * corresponding wrapper - returns null.
     *
     * @param chatRoom the <tt>ChatRoom</tt> that we're looking for
     * @return the <tt>ChatRoomWrapper</tt> object corresponding to the given
     * <tt>ChatRoom</tt>
     */
    public abstract ChatRoomWrapper findChatRoomWrapperFromChatRoom(
        ChatRoom chatRoom);

    /**
     * Opens a chat window for the chat room.
     *
     * @param room the chat room.
     */
    public abstract void openChatRoom(ChatRoomWrapper room);

    /**
     * Returns instance of the <tt>ServerChatRoomContactSourceService</tt>
     * contact source.
     * @return instance of the <tt>ServerChatRoomContactSourceService</tt>
     * contact source.
     */
    public abstract ContactSourceService
        getServerChatRoomsContactSourceForProvider(ChatRoomProviderWrapper pps);

    /**
     * Returns <tt>true</tt> if the contact is <tt>ChatRoomSourceContact</tt>
     *
     * @param contact the contact
     * @return <tt>true</tt> if the contact is <tt>ChatRoomSourceContact</tt>
     */
    public abstract boolean isMUCSourceContact(SourceContact contact);
}
