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

import static net.java.sip.communicator.service.muc.ChatRoomWrapper.*;

import java.util.*;

import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.plugin.desktoputil.chat.*;
import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.muc.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.globalstatus.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.resources.*;

/**
 * The <tt>MUCServiceImpl</tt> class implements the service for the chat rooms.
 *
 * @author Hristo Terezov
 */
public class MUCServiceImpl
    extends MUCService
{

    /**
     * The list of persistent chat rooms.
     */
    private final ChatRoomListImpl chatRoomList = new ChatRoomListImpl();

    /**
     * The <tt>Logger</tt> used by the <tt>MUCServiceImpl</tt> class and its
     * instances for logging output.
     */
    private static Logger logger = Logger.getLogger(MUCServiceImpl.class);

    /**
     * Called to accept an incoming invitation. Adds the invitation chat room
     * to the list of chat rooms and joins it.
     *
     * @param invitation the invitation to accept.
     */
    public void acceptInvitation(ChatRoomInvitation invitation)
    {
        ChatRoom chatRoom = invitation.getTargetChatRoom();
        byte[] password = invitation.getChatRoomPassword();

        String nickName =
            ConfigurationUtils.getChatRoomProperty(
                chatRoom.getParentProvider(),
                chatRoom.getIdentifier(), "userNickName");
        if(nickName == null)
        {
            String[] joinOptions = ChatRoomJoinOptionsDialog.getJoinOptions(
                true,
                chatRoom.getParentProvider(),
                chatRoom.getIdentifier(),
                MUCActivator.getGlobalDisplayDetailsService()
                    .getDisplayName(chatRoom.getParentProvider()));
            nickName = joinOptions[0];
        }

        joinChatRoom(chatRoom, nickName, password);
    }

    /**
     * Adds a change listener to the <tt>ChatRoomList</tt>.
     *
     * @param l the listener.
     */
    public void addChatRoomListChangeListener(ChatRoomListChangeListener l)
    {
        chatRoomList.addChatRoomListChangeListener(l);
    }

    /**
     * Removes a change listener to the <tt>ChatRoomList</tt>.
     *
     * @param l the listener.
     */
    public void removeChatRoomListChangeListener(ChatRoomListChangeListener l)
    {
        chatRoomList.removeChatRoomListChangeListener(l);
    }

    /**
     * Fires a <tt>ChatRoomListChangedEvent</tt> event.
     *
     * @param chatRoomWrapper the chat room.
     * @param eventID the id of the event.
     */
    public void fireChatRoomListChangedEvent(  ChatRoomWrapper chatRoomWrapper,
        int eventID)
    {
        chatRoomList.fireChatRoomListChangedEvent(chatRoomWrapper, eventID);
    }

    /**
     * Joins the given chat room with the given password and manages all the
     * exceptions that could occur during the join process.
     *
     * @param chatRoomWrapper the chat room to join.
     * @param nickName the nickname we choose for the given chat room.
     * @param password the password.
     * @param rememberPassword if true the password should be saved.
     * @param isFirstAttempt is this the first attempt to join room, used
     *                       to check whether to show some error messages
     * @param subject the subject which will be set to the room after the user
     * join successful.
     */
    private void joinChatRoom(   ChatRoomWrapper chatRoomWrapper,
                                String nickName,
                                byte[] password,
                                boolean rememberPassword,
                                boolean isFirstAttempt,
                                String subject)
    {
        ChatRoom chatRoom = chatRoomWrapper.getChatRoom();

        if(chatRoom == null)
        {
            MUCActivator.getAlertUIService().showAlertDialog(
                MUCActivator.getResources().getI18NString("service.gui.WARNING"),
                MUCActivator.getResources().getI18NString(
                "service.gui.CHAT_ROOM_NOT_CONNECTED",
                new String[]{chatRoomWrapper.getChatRoomName()}));
            return;
        }

        new JoinChatRoomTask(
                (ChatRoomWrapperImpl)chatRoomWrapper,
                nickName,
                password,
                rememberPassword,
                isFirstAttempt,
                subject)
            .start();
    }

    /**
     * Joins the given chat room with the given password and manages all the
     * exceptions that could occur during the join process.
     *
     * @param chatRoomWrapper the chat room to join.
     * @param nickName the nickname we choose for the given chat room.
     * @param password the password.
     */
    public void joinChatRoom(   ChatRoomWrapper chatRoomWrapper,
                                String nickName,
                                byte[] password)
    {
        if (chatRoomWrapper.getChatRoom() == null)
        {
            chatRoomWrapper = createChatRoom(
                chatRoomWrapper.getChatRoomName(),
                chatRoomWrapper.getParentProvider().getProtocolProvider(),
                new ArrayList<String>(), "", false, false, true);
        }

        ChatRoom chatRoom = chatRoomWrapper.getChatRoom();

        if(chatRoom == null)
        {
            MUCActivator.getAlertUIService().showAlertDialog(
               MUCActivator.getResources().getI18NString("service.gui.WARNING"),
               MUCActivator.getResources().getI18NString(
                    "service.gui.CHAT_ROOM_NOT_CONNECTED",
                    new String[]{chatRoomWrapper.getChatRoomName()}));
            return;
        }

        new JoinChatRoomTask(
                (ChatRoomWrapperImpl)chatRoomWrapper, nickName, password)
            .start();
    }

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
    public void joinChatRoom(   ChatRoomWrapper chatRoomWrapper,
                                String nickName,
                                byte[] password,
                                String subject)
    {
        if (chatRoomWrapper.getChatRoom() == null)
        {
            chatRoomWrapper = createChatRoom(
                chatRoomWrapper.getChatRoomName(),
                chatRoomWrapper.getParentProvider().getProtocolProvider(),
                new ArrayList<String>(), "", false, false, true);
        }

        ChatRoom chatRoom = chatRoomWrapper.getChatRoom();
        if (chatRoom == null)
        {
            MUCActivator.getAlertUIService().showAlertDialog(
               MUCActivator.getResources().getI18NString("service.gui.WARNING"),
               MUCActivator.getResources().getI18NString(
                    "service.gui.CHAT_ROOM_NOT_CONNECTED",
                    new String[]{chatRoomWrapper.getChatRoomName()}));

            return;
        }

        // join from add chat room dialog

        new JoinChatRoomTask(
                (ChatRoomWrapperImpl) chatRoomWrapper,
                nickName,
                password,
                subject)
            .start();
    }


    /**
     * Join chat room.
     * @param chatRoomWrapper
     */
    public void joinChatRoom(ChatRoomWrapper chatRoomWrapper)
    {
        if (chatRoomWrapper.getChatRoom() == null)
        {
            chatRoomWrapper = createChatRoom(
                chatRoomWrapper.getChatRoomName(),
                chatRoomWrapper.getParentProvider().getProtocolProvider(),
                new ArrayList<String>(), "", false, false, true);
        }

        ChatRoom chatRoom = chatRoomWrapper.getChatRoom();

        if(chatRoom == null)
        {
            MUCActivator.getAlertUIService().showAlertDialog(
               MUCActivator.getResources().getI18NString("service.gui.WARNING"),
               MUCActivator.getResources().getI18NString(
                        "service.gui.CHAT_ROOM_NOT_CONNECTED",
                        new String[]{chatRoomWrapper.getChatRoomName()}));

            return;
        }

        new JoinChatRoomTask((ChatRoomWrapperImpl)chatRoomWrapper, null, null)
                .start();
    }


    /**
     * Joins the given chat room and manages all the exceptions that could
     * occur during the join process.
     *
     * @param chatRoom the chat room to join
     * @param nickname the nickname we're using to join
     * @param password the password we're using to join
     */
    public void joinChatRoom(   ChatRoom chatRoom,
                                String nickname,
                                byte[] password)
    {
        ChatRoomWrapper chatRoomWrapper
            = chatRoomList.findChatRoomWrapperFromChatRoom(chatRoom);

        if(chatRoomWrapper == null)
        {
            ChatRoomProviderWrapper parentProvider
                = chatRoomList.findServerWrapperFromProvider(
                    chatRoom.getParentProvider());

            chatRoomWrapper
                = new ChatRoomWrapperImpl(parentProvider, chatRoom);

            chatRoomList.addChatRoom(chatRoomWrapper);

        }

        this.joinChatRoom(chatRoomWrapper, nickname, password);
    }

    /**
     * Joins the room with the given name though the given chat room provider.
     *
     * @param chatRoomName the name of the room to join.
     * @param chatRoomProvider the chat room provider to join through.
     */
    public void joinChatRoom(   String chatRoomName,
                                ChatRoomProviderWrapper chatRoomProvider)
    {
        OperationSetMultiUserChat groupChatOpSet
            = chatRoomProvider
                  .getProtocolProvider().getOperationSet(
                    OperationSetMultiUserChat.class);

        ChatRoom chatRoom = null;
        try
        {
            chatRoom = groupChatOpSet.findRoom(chatRoomName);
        }
        catch (Exception e)
        {
            if (logger.isTraceEnabled())
                logger.trace("Un exception occurred while searching for room:"
                + chatRoomName, e);
        }

        if (chatRoom != null)
        {
            ChatRoomWrapper chatRoomWrapper
                = chatRoomList.findChatRoomWrapperFromChatRoom(chatRoom);

            if(chatRoomWrapper == null)
            {
                ChatRoomProviderWrapper parentProvider
                    = chatRoomList
                        .findServerWrapperFromProvider(
                            chatRoom.getParentProvider());

                chatRoomWrapper
                    = new ChatRoomWrapperImpl(parentProvider, chatRoom);

                chatRoomList.addChatRoom(chatRoomWrapper);

                fireChatRoomListChangedEvent(
                    chatRoomWrapper,
                    ChatRoomListChangeEvent.CHAT_ROOM_ADDED);
            }
            joinChatRoom(chatRoomWrapper);
        }
        else
            MUCActivator.getAlertUIService().showAlertDialog(
                MUCActivator.getResources().getI18NString("service.gui.ERROR"),
                MUCActivator.getResources().getI18NString(
                    "service.gui.CHAT_ROOM_NOT_EXIST",
                    new String[]{chatRoomName,
                    chatRoomProvider.getProtocolProvider()
                        .getAccountID().getService()}));
    }


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
     * @param isPrivate whether the room will be private or public.
     * @return the <tt>ChatRoomWrapper</tt> corresponding to the created room
     */
    public ChatRoomWrapper createChatRoom(
        String roomName,
        ProtocolProviderService protocolProvider,
        Collection<String> contacts,
        String reason,
        boolean persistent,
        boolean isPrivate)
    {
        return createChatRoom(
            roomName, protocolProvider, contacts, reason, true, persistent,
            isPrivate);
    }

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
    public ChatRoomWrapper createChatRoom(
        String roomName,
        ProtocolProviderService protocolProvider,
        Collection<String> contacts,
        String reason,
        boolean persistent)
    {
        return createChatRoom(
            roomName, protocolProvider, contacts, reason, true, persistent,
            false);
    }

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
     *         <tt>null</tt> if the protocol fails to create the chat room.
     */
    public ChatRoomWrapper createChatRoom(
        String roomName,
        ProtocolProviderService protocolProvider,
        Collection<String> contacts,
        String reason,
        boolean join,
        boolean persistent,
        boolean isPrivate)
    {
        ChatRoomWrapper chatRoomWrapper = null;
        OperationSetMultiUserChat groupChatOpSet
            = protocolProvider.getOperationSet(OperationSetMultiUserChat.class);

        // If there's no group chat operation set we have nothing to do here.
        if (groupChatOpSet == null)
            return null;

        ChatRoom chatRoom = null;
        try
        {


            HashMap<String, Object> roomProperties =
                new HashMap<String, Object>();
            roomProperties.put("isPrivate", isPrivate);
            chatRoom = groupChatOpSet.createChatRoom(roomName, roomProperties);

            if(join)
            {
                chatRoom.join();

                for(String contact : contacts)
                    chatRoom.invite(contact, reason);
            }
        }
        catch (OperationFailedException ex)
        {
            logger.error("Failed to create chat room.", ex);

            MUCActivator.getAlertUIService().showAlertDialog(
                MUCActivator.getResources().getI18NString("service.gui.ERROR"),
                MUCActivator.getResources().getI18NString(
                    "service.gui.CREATE_CHAT_ROOM_ERROR",
                    new String[]{protocolProvider.getProtocolDisplayName()}),
                    ex);
        }
        catch (OperationNotSupportedException ex)
        {
            logger.error("Failed to create chat room.", ex);

            MUCActivator.getAlertUIService().showAlertDialog(
                MUCActivator.getResources().getI18NString("service.gui.ERROR"),
                MUCActivator.getResources().getI18NString(
                    "service.gui.CREATE_CHAT_ROOM_ERROR",
                    new String[]{protocolProvider.getProtocolDisplayName()}),
                    ex);
        }

        if(chatRoom != null)
        {
            ChatRoomProviderWrapper parentProvider
                = chatRoomList.findServerWrapperFromProvider(protocolProvider);

            // if there is the same room ids don't add new wrapper as old one
            // maybe already created
            chatRoomWrapper =
                chatRoomList.findChatRoomWrapperFromChatRoom(chatRoom);

            if(chatRoomWrapper == null)
            {
                chatRoomWrapper
                    = new ChatRoomWrapperImpl(parentProvider, chatRoom);
                chatRoomWrapper.setPersistent(persistent);
                chatRoomList.addChatRoom(chatRoomWrapper);
            }
        }

        return chatRoomWrapper;
    }

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
    public ChatRoomWrapper createPrivateChatRoom(
        ProtocolProviderService protocolProvider,
        Collection<String> contacts,
        String reason,
        boolean persistent)
    {
        return this.createChatRoom(
            null, protocolProvider, contacts, reason, persistent, true);
    }


    /**
     * Returns existing chat rooms for the given <tt>chatRoomProvider</tt>.
     * @param chatRoomProvider the <tt>ChatRoomProviderWrapper</tt>, which
     * chat rooms we're looking for
     * @return  existing chat rooms for the given <tt>chatRoomProvider</tt>
     */
    public List<String> getExistingChatRooms(
        ChatRoomProviderWrapper chatRoomProvider)
    {
        if (chatRoomProvider == null)
            return null;

        ProtocolProviderService protocolProvider
            = chatRoomProvider.getProtocolProvider();

        if (protocolProvider == null)
            return null;

        OperationSetMultiUserChat groupChatOpSet
            = protocolProvider
                .getOperationSet(OperationSetMultiUserChat.class);

        if (groupChatOpSet == null)
            return null;

        List<String> chatRooms = null;
        try
        {
            chatRooms = groupChatOpSet.getExistingChatRooms();
        }
        catch (OperationFailedException e)
        {
            if (logger.isTraceEnabled())
                logger.trace("Failed to obtain existing chat rooms for server: "
                + protocolProvider.getAccountID().getService(), e);
        }
        catch (OperationNotSupportedException e)
        {
            if (logger.isTraceEnabled())
                logger.trace("Failed to obtain existing chat rooms for server: "
                + protocolProvider.getAccountID().getService(), e);
        }

        return chatRooms;
    }

    /**
     * Rejects the given invitation with the specified reason.
     *
     * @param multiUserChatOpSet the operation set to use for rejecting the
     * invitation
     * @param invitation the invitation to reject
     * @param reason the reason for the rejection
     */
    public void rejectInvitation(  OperationSetMultiUserChat multiUserChatOpSet,
                                   ChatRoomInvitation invitation,
                                   String reason)
    {
        multiUserChatOpSet.rejectInvitation(invitation, reason);
    }

    /**
     * Leaves the given chat room.
     *
     * @param chatRoomWrapper the chat room to leave.
     * @return <tt>ChatRoomWrapper</tt> instance associated with the chat room.
     */
    public ChatRoomWrapper leaveChatRoom(ChatRoomWrapper chatRoomWrapper)
    {
        ChatRoom chatRoom = chatRoomWrapper.getChatRoom();

        if (chatRoom == null)
        {
            ResourceManagementService resources = MUCActivator.getResources();

            MUCActivator.getAlertUIService().showAlertDialog(
                    resources.getI18NString("service.gui.WARNING"),
                    resources
                        .getI18NString(
                            "service.gui.CHAT_ROOM_LEAVE_NOT_CONNECTED"));

            return null;
        }

        if (chatRoom.isJoined())
            chatRoom.leave();

        ChatRoomWrapper existChatRoomWrapper
            = chatRoomList.findChatRoomWrapperFromChatRoom(chatRoom);

        if(existChatRoomWrapper == null)
            return null;

        // We save the choice of the user, before the chat room is really
        // joined, because even the join fails we want the next time when
        // we login to join this chat room automatically.
        ConfigurationUtils.updateChatRoomStatus(
            chatRoomWrapper.getParentProvider().getProtocolProvider(),
            chatRoomWrapper.getChatRoomID(),
            GlobalStatusEnum.OFFLINE_STATUS);

        return existChatRoomWrapper;
    }

    /**
     * Joins a chat room in an asynchronous way.
     */
    private class JoinChatRoomTask
        extends Thread
    {
        private final ChatRoomWrapperImpl chatRoomWrapper;

        private final String nickName;

        private final byte[] password;

        private final boolean rememberPassword;

        private final boolean isFirstAttempt;

        private final String subject;

        private ResourceManagementService resources
            = MUCActivator.getResources();

        JoinChatRoomTask(   ChatRoomWrapperImpl chatRoomWrapper,
                            String nickName,
                            byte[] password,
                            boolean rememberPassword,
                            boolean isFirstAttempt,
                            String subject)
        {
            this.chatRoomWrapper = chatRoomWrapper;
            this.nickName = nickName;
            this.isFirstAttempt = isFirstAttempt;
            this.subject = subject;

            if(password == null)
            {
                String passString = chatRoomWrapper.loadPassword();
                if(passString != null)
                {
                    this.password = passString.getBytes();
                }
                else
                {
                    this.password = null;
                }
            }
            else
            {
                this.password = password;
            }
            this.rememberPassword = rememberPassword;
        }

        JoinChatRoomTask(   ChatRoomWrapperImpl chatRoomWrapper,
            String nickName,
            byte[] password)
        {
            this(chatRoomWrapper, nickName, password, false, true, null);
        }

        JoinChatRoomTask(   ChatRoomWrapperImpl chatRoomWrapper,
            String nickName,
            byte[] password,
            String subject)
        {
            this(chatRoomWrapper, nickName, password, false, true, subject);
        }

        /**
         * @override {@link Thread}{@link #run()} to perform all asynchronous
         * tasks.
         */
        @Override
        public void run()
        {
            ChatRoom chatRoom = chatRoomWrapper.getChatRoom();

            try
            {
                if(password != null && password.length > 0)
                    chatRoom.joinAs(nickName, password);
                else if (nickName != null)
                    chatRoom.joinAs(nickName);
                else
                    chatRoom.join();

                done(JOIN_SUCCESS_PROP);
            }
            catch (OperationFailedException e)
            {
                if (logger.isTraceEnabled())
                    logger.trace("Failed to join chat room: "
                    + chatRoom.getName(), e);

                switch (e.getErrorCode())
                {
                case OperationFailedException.AUTHENTICATION_FAILED:
                    done(JOIN_AUTHENTICATION_FAILED_PROP);
                    break;
                case OperationFailedException.REGISTRATION_REQUIRED:
                    done(JOIN_REGISTRATION_REQUIRED_PROP);
                    break;
                case OperationFailedException.PROVIDER_NOT_REGISTERED:
                    done(JOIN_PROVIDER_NOT_REGISTERED_PROP);
                    break;
                case OperationFailedException.SUBSCRIPTION_ALREADY_EXISTS:
                    done(JOIN_SUBSCRIPTION_ALREADY_EXISTS_PROP);
                    break;
                default:
                    done(JOIN_UNKNOWN_ERROR_PROP);
                }
            }
        }

        /**
         * Performs UI changes after the chat room join task has finished.
         * @param returnCode the result code from the chat room join task.
         */
        private void done(String returnCode)
        {
            ConfigurationUtils.updateChatRoomStatus(
                chatRoomWrapper.getParentProvider().getProtocolProvider(),
                chatRoomWrapper.getChatRoomID(),
                GlobalStatusEnum.ONLINE_STATUS);

            String errorMessage = null;
            if(JOIN_AUTHENTICATION_FAILED_PROP.equals(returnCode))
            {
                chatRoomWrapper.removePassword();

                AuthenticationWindowService authWindowsService
                    = ServiceUtils.getService(
                        MUCActivator.bundleContext,
                        AuthenticationWindowService.class);

                AuthenticationWindowService.AuthenticationWindow authWindow =
                    authWindowsService.create(
                        null, null, null, false,
                        chatRoomWrapper.isPersistent(),
                        AuthenticationWindow.getAuthenticationWindowIcon(
                            chatRoomWrapper.getParentProvider()
                                .getProtocolProvider()),
                        resources.getI18NString(
                            "service.gui.AUTHENTICATION_WINDOW_TITLE",
                            new String[]{chatRoomWrapper.getParentProvider()
                                            .getName()}),
                        resources.getI18NString(
                                "service.gui.CHAT_ROOM_REQUIRES_PASSWORD",
                                new String[]{
                                        chatRoomWrapper.getChatRoomName()}),
                        "", null,
                        isFirstAttempt ?
                            null :
                        resources.getI18NString(
                                "service.gui.AUTHENTICATION_FAILED",
                                new String[]{chatRoomWrapper.getChatRoomName()}),
                        null);

                authWindow.setVisible(true);

                if (!authWindow.isCanceled())
                {
                    joinChatRoom(
                            chatRoomWrapper,
                            nickName,
                            new String(authWindow.getPassword()).getBytes(),
                            authWindow.isRememberPassword(),
                            false,
                            subject);
                }
            }
            else if(JOIN_REGISTRATION_REQUIRED_PROP.equals(returnCode))
            {
                errorMessage
                    = resources
                        .getI18NString(
                            "service.gui.CHAT_ROOM_REGISTRATION_REQUIRED",
                            new String[]{chatRoomWrapper.getChatRoomName()});
            }
            else if(JOIN_PROVIDER_NOT_REGISTERED_PROP.equals(returnCode))
            {
                errorMessage
                    = resources
                        .getI18NString("service.gui.CHAT_ROOM_NOT_CONNECTED",
                        new String[]{chatRoomWrapper.getChatRoomName()});
            }
            else if(JOIN_SUBSCRIPTION_ALREADY_EXISTS_PROP.equals(returnCode))
            {
                errorMessage
                    = resources
                        .getI18NString("service.gui.CHAT_ROOM_ALREADY_JOINED",
                            new String[]{chatRoomWrapper.getChatRoomName()});
            }
            else
            {
                errorMessage
                    = resources
                        .getI18NString("service.gui.FAILED_TO_JOIN_CHAT_ROOM",
                            new String[]{chatRoomWrapper.getChatRoomName()});
            }

            if (!JOIN_SUCCESS_PROP.equals(returnCode) &&
                !JOIN_AUTHENTICATION_FAILED_PROP.equals(returnCode))
            {
                MUCActivator.getAlertUIService().showAlertPopup(
                    resources.getI18NString("service.gui.ERROR"), errorMessage);
            }

            if (JOIN_SUCCESS_PROP.equals(returnCode))
            {
                if(rememberPassword)
                {
                    chatRoomWrapper.savePassword(new String(password));
                }

                if(subject != null)
                {
                    try
                    {
                        chatRoomWrapper.getChatRoom().setSubject(subject);
                    }
                    catch(OperationFailedException ex)
                    {
                        logger.warn("Failed to set subject.");
                    }
                }
            }

            chatRoomWrapper.firePropertyChange(returnCode);
        }
    }

    /**
     * Finds the <tt>ChatRoomWrapper</tt> instance associated with the
     * source contact.
     * @param contact the source contact.
     * @return the <tt>ChatRoomWrapper</tt> instance.
     */
    public ChatRoomWrapper findChatRoomWrapperFromSourceContact(
        SourceContact contact)
    {
        if(!(contact instanceof ChatRoomSourceContact))
            return null;
        ChatRoomSourceContact chatRoomContact = (ChatRoomSourceContact) contact;
        return chatRoomList.findChatRoomWrapperFromChatRoomID(
                chatRoomContact.getChatRoomID(), chatRoomContact.getProvider());
    }

    /**
     * Finds the <tt>ChatRoomWrapper</tt> instance associated with the
     * chat room.
     * @param chatRoomID the id of the chat room.
     * @param pps the provider of the chat room.
     * @return the <tt>ChatRoomWrapper</tt> instance.
     */
    public ChatRoomWrapper findChatRoomWrapperFromChatRoomID(String chatRoomID,
        ProtocolProviderService pps)
    {
        return chatRoomList.findChatRoomWrapperFromChatRoomID(chatRoomID, pps);
    }

    /**
     * Searches for chat room wrapper in chat room list by chat room.
     *
     * @param chatRoom the chat room.
     * @param create if <tt>true</tt> and the chat room wrapper is not found new
     * chatRoomWrapper is created.
     * @return found chat room wrapper or the created chat room wrapper.
     */
    @Override
    public ChatRoomWrapper getChatRoomWrapperByChatRoom(ChatRoom chatRoom,
        boolean create)
    {
        ChatRoomWrapper chatRoomWrapper
            = chatRoomList.findChatRoomWrapperFromChatRoom(chatRoom);

        if ((chatRoomWrapper == null) && create)
        {
            ChatRoomProviderWrapper parentProvider
                = chatRoomList.findServerWrapperFromProvider(
                    chatRoom.getParentProvider());

            chatRoomWrapper
                = new ChatRoomWrapperImpl(
                    parentProvider, chatRoom);

            chatRoomList.addChatRoom(chatRoomWrapper);
        }
        return chatRoomWrapper;
    }

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
        final OperationSetMultiUserChat opSet)
    {
        ChatRoomProviderWrapper chatRoomProvider
            = findServerWrapperFromProvider(protocolProvider);

        if(chatRoomProvider == null)
        {
            chatRoomProvider = chatRoomList.addRegisteredChatProvider(protocolProvider);
        }

        if (chatRoomProvider != null)
        {
            chatRoomProvider.synchronizeProvider();
        }
    }

    /**
     * Returns an iterator to the list of chat room providers.
     *
     * @return an iterator to the list of chat room providers.
     */
    public Iterator<ChatRoomProviderWrapper> getChatRoomProviders()
    {
        return chatRoomList.getChatRoomProviders();
    }

    /**
     * Removes the given <tt>ChatRoom</tt> from the list of all chat rooms.
     *
     * @param chatRoomWrapper the <tt>ChatRoomWrapper</tt> to remove
     */
    public void removeChatRoom(ChatRoomWrapper chatRoomWrapper)
    {
        chatRoomList.removeChatRoom(chatRoomWrapper);
    }

    /**
     * Destroys the given <tt>ChatRoom</tt> from the list of all chat rooms.
     *
     * @param chatRoomWrapper the <tt>ChatRoomWrapper</tt> to be destroyed.
     * @param reason the reason for destroying.
     * @param alternateAddress the alternate address.
     */
    public void destroyChatRoom(ChatRoomWrapper chatRoomWrapper,
        String reason, String alternateAddress)
    {
        if(chatRoomWrapper.getChatRoom().destroy(reason, alternateAddress))
        {
            MUCActivator.getUIService().closeChatRoomWindow(
                chatRoomWrapper);
            chatRoomList.removeChatRoom(chatRoomWrapper);
        }
        else
        {
            // if we leave a chat room which is not persistent
            // the room can be destroyed on the server, and error is returned
            // when we try to destroy it not-authorized(401)
            if(!chatRoomWrapper.getChatRoom().isPersistent()
                && !chatRoomWrapper.getChatRoom().isJoined())
            {
                chatRoomList.removeChatRoom(chatRoomWrapper);
            }
        }

    }

    /**
     * Adds a ChatRoomProviderWrapperListener to the listener list.
     *
     * @param listener the ChatRoomProviderWrapperListener to be added
     */
    public  void addChatRoomProviderWrapperListener(
        ChatRoomProviderWrapperListener listener)
    {
        chatRoomList.addChatRoomProviderWrapperListener(listener);
    }

    /**
     * Removes the ChatRoomProviderWrapperListener to the listener list.
     *
     * @param listener the ChatRoomProviderWrapperListener to be removed
     */
    public  void removeChatRoomProviderWrapperListener(
        ChatRoomProviderWrapperListener listener)
    {
        chatRoomList.removeChatRoomProviderWrapperListener(listener);
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
        return chatRoomList.findServerWrapperFromProvider(protocolProvider);
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
        return chatRoomList.findChatRoomWrapperFromChatRoom(chatRoom);
    }

    /**
     * Opens a chat window for the chat room.
     *
     * @param room the chat room.
     */
    public void openChatRoom(ChatRoomWrapper room)
    {
        if (room.getChatRoom() == null)
        {
            room = createChatRoom(
                room.getChatRoomName(),
                room.getParentProvider().getProtocolProvider(),
                new ArrayList<String>(),"", false, false, true);

            // leave the chatroom because getChatRoom().isJoined() returns true
            // otherwise
            if (room.getChatRoom().isJoined())
                room.getChatRoom().leave();

        }

        if(!room.getChatRoom().isJoined())
        {
            String savedNick =
                ConfigurationUtils.getChatRoomProperty(room
                    .getParentProvider().getProtocolProvider(), room
                    .getChatRoomID(), "userNickName");
            String subject = null;

            if (savedNick == null)
            {
                String[] joinOptions = ChatRoomJoinOptionsDialog.getJoinOptions(
                    room.getParentProvider().getProtocolProvider(),
                    room.getChatRoomID(),
                    MUCActivator.getGlobalDisplayDetailsService()
                        .getDisplayName(
                            room.getParentProvider().getProtocolProvider()));
                savedNick = joinOptions[0];
                subject = joinOptions[1];

            }

            if (savedNick != null)
            {
                joinChatRoom(room, savedNick, null,
                        subject);
            }
            else
                return;
        }

        MUCActivator.getUIService().openChatRoomWindow(room);
    }

    /**
     * Returns instance of the <tt>ServerChatRoomContactSourceService</tt>
     * contact source.
     * @return instance of the <tt>ServerChatRoomContactSourceService</tt>
     * contact source.
     */
    public ContactSourceService getServerChatRoomsContactSourceForProvider(
        ChatRoomProviderWrapper pps)
    {
        return new ServerChatRoomContactSourceService(pps);
    }

    /**
     * Returns <tt>true</tt> if the contact is <tt>ChatRoomSourceContact</tt>
     *
     * @param contact the contact
     * @return <tt>true</tt> if the contact is <tt>ChatRoomSourceContact</tt>
     */
    public boolean isMUCSourceContact(SourceContact contact)
    {
        return (contact instanceof ChatRoomSourceContact);
    }
}
