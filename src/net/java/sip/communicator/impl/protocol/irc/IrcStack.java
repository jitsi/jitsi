/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.irc;

import java.io.*;
import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

import org.jibble.pircbot.*;

/**
 * An implementation of the PircBot IRC stack.
 * 
 * @author Stephane Remy
 * @author Loic Kempf
 * @author Yana Stamcheva
 */
public class IrcStack
    extends PircBot
{
    private static final Logger logger = Logger.getLogger(IrcStack.class);

    /**
     * Timeout for server response.
     */
    private static final int TIMEOUT = 10000;

    /**
     * A list of timers indicating when a chat room join fails.
     */
    private final Map<ChatRoom, Timer> joinTimeoutTimers
        = new Hashtable<ChatRoom, Timer>();

    /**
     * A list of the channels on this server
     */
    private final List<String> serverChatRoomList = new ArrayList<String>();

    /**
     * A list of users that we have info about, it is used to stock "whois"
     * responses
     */
    private final Map<String, UserInfo> userInfoTable
        = new Hashtable<String, UserInfo>();

    /**
     * The IRC multi-user chat operation set.
     */
    private final OperationSetMultiUserChatIrcImpl ircMUCOpSet;

    /**
     * The IRC protocol provider service.
     */
    private final ProtocolProviderServiceIrcImpl parentProvider;


    private final Object operationLock = new Object();

    /**
     * The operation response code indicates 
     */
    private int operationResponseCode = 0;

    /**
     * Indicates if the IRC server has been initialized.
     */
    private boolean isInitialized = false;

    /**
     * Keeps all join requests received before the server is initialized.
     */
    private final List<ChatRoom> joinCache = new ArrayList<ChatRoom>();

    /**
     * The indicator which determines whether #onConnect() has been invoked and
     * thus a manual invocation of its old functionality is pending.
     */
    private boolean onConnectInvoked = false;

    /**
     * Creates an instance of <tt>IrcStack</tt>.
     *
     * @param parentProvider the IRC protocol provider service
     * @param nickname our nickname
     * @param login our login
     * @param version the version
     * @param finger the finger
     */
    public IrcStack(    ProtocolProviderServiceIrcImpl parentProvider,
                        String nickname,
                        String login,
                        String version,
                        String finger)
    {
        this.parentProvider = parentProvider;
        this.ircMUCOpSet
            = (OperationSetMultiUserChatIrcImpl) parentProvider
                .getOperationSet(OperationSetMultiUserChat.class);
        this.setName(nickname);
        this.setLogin(login);
        this.setVersion(version);
        this.setFinger(finger);
    }

    /**
     * Connects to the server.
     * 
     * @param serverAddress the address of the server
     * @param serverPort the port to connect to
     * @param serverPassword the password to use for connect
     * @param autoNickChange indicates if the nick name should be changed in 
     * case there exist already a participant with the same nick name
     * 
     * @throws OperationFailedException
     */
    public void connect(String serverAddress,
                        int serverPort,
                        String serverPassword,
                        boolean autoNickChange)
        throws OperationFailedException
    {
        this.setVerbose(false);
        this.setAutoNickChange(autoNickChange);

        boolean onConnectInvoked;

        try
        {
            // avoids deadlock - issue#620. Call the event
            // in non synchronized code
            synchronized (this)
            {
                this.onConnectInvoked = false;

                if (serverPassword == null)
                    this.connect(serverAddress, serverPort);
                else
                    this.connect(serverAddress, serverPort, serverPassword);

                onConnectInvoked = this.onConnectInvoked;
            }
        }
        catch (IOException e)
        {
            throw new OperationFailedException(e.getMessage(),
                OperationFailedException.INTERNAL_SERVER_ERROR);
        }
        catch (NickAlreadyInUseException e)
        {
            throw new OperationFailedException(e.getMessage(),
                OperationFailedException.SUBSCRIPTION_ALREADY_EXISTS);
        }
        catch (IrcException e)
        {
            throw new OperationFailedException(e.getMessage(),
                OperationFailedException.GENERAL_ERROR);
        }

        // if onConnect method is called fire event from code that
        // is not synchronized - issue#620
        if (onConnectInvoked)
        {
            parentProvider
                .setCurrentRegistrationState(RegistrationState.REGISTERED);

            // It should be done when a getExistingChatRooms request is processed.
            // Obtain information for all channels on this server.
            // this.listChannels();
        }
    }

    /**
     * Called when we're connected to the IRC server.
     */
    protected synchronized void onConnect()
    {

        /*
         * Just mark that the onConnect method is invoked so that its old
         * functionality can be invoked manually elsewhere later on thus
         * avoiding a deadlock - issue #620.
         */
        onConnectInvoked = true;
    }

    /**
     * Called when we're disconnected from the IRC server.
     */
    protected void onDisconnect()
    {
        parentProvider
            .setCurrentRegistrationState(RegistrationState.UNREGISTERED);
    }

    /**
     * Indicates that a message has arrived from the IRC stack.
     * @param channel the channel where the message is received
     * @param sender the sender of the message
     * @param login the login
     * @param hostname the host name
     * @param messageContent the content of the message
     */
    protected void onMessage(   String channel,
                                String sender,
                                String login,
                                String hostname,
                                String messageContent)
    {
        if (logger.isTraceEnabled())
            logger.trace("MESSAGE received in chat room : " + channel
                        + ": from " + sender
                        + " " + login + "@" + hostname
                        + " the message: " + messageContent);

        MessageIrcImpl message
            = new MessageIrcImpl(   messageContent,
                                    MessageIrcImpl.DEFAULT_MIME_TYPE,
                                    MessageIrcImpl.DEFAULT_MIME_ENCODING,
                                    null);
 
        ChatRoomIrcImpl chatRoom = ircMUCOpSet.getChatRoom(channel);

        if (chatRoom == null)
            chatRoom = ircMUCOpSet.findSystemRoom();

        if(chatRoom == null || !chatRoom.isJoined())
            return;

        ChatRoomMember sourceMember = chatRoom.getChatRoomMember(sender);

        if (sourceMember == null)
            return;

        chatRoom.fireMessageReceivedEvent(
            message,
            sourceMember,
            System.currentTimeMillis(),
            ChatRoomMessageReceivedEvent.CONVERSATION_MESSAGE_RECEIVED);
    }

    /**
     * Indicates that a private message has been received.
     * Note that for now this method only logs the message.
     * @param sender the sender of the message
     * @param login the login
     * @param hostname the host name
     * @param messageContent the content of the message
     */
    protected void onPrivateMessage(String sender,
                                    String login,
                                    String hostname,
                                    String messageContent)
    {
        if (logger.isTraceEnabled())
            logger.trace("PRIVATE MESSAGE received from " + sender
                        + " " + login + "@" + hostname
                        + " the message: " + messageContent);

        MessageIrcImpl message
            = new MessageIrcImpl(   messageContent,
                                    MessageIrcImpl.DEFAULT_MIME_TYPE,
                                    MessageIrcImpl.DEFAULT_MIME_ENCODING,
                                    null);

        ChatRoomIrcImpl chatRoom
            = ircMUCOpSet.findPrivateChatRoom(sender);

        if(chatRoom == null || !chatRoom.isJoined())
            return;

        ChatRoomMember sourceMember = chatRoom.getChatRoomMember(sender);

        if (sourceMember == null)
        {
            sourceMember
                = new ChatRoomMemberIrcImpl(parentProvider,
                                            chatRoom,
                                            sender,
                                            ChatRoomMemberRole.GUEST);

            chatRoom.addChatRoomMember(sender, sourceMember);

            chatRoom.fireMemberPresenceEvent(
                sourceMember,
                null, // There's no other actors in this presence event.
                ChatRoomMemberPresenceChangeEvent.MEMBER_JOINED,
                "A message received from unknown member.");
        }

        chatRoom.fireMessageReceivedEvent(
            message,
            sourceMember,
            System.currentTimeMillis(),
            ChatRoomMessageReceivedEvent.CONVERSATION_MESSAGE_RECEIVED);
    }

    /**
     * This method is called whenever an ACTION is sent from a user.  E.g.
     * such events generated by typing "/me goes shopping" in most IRC clients.
     * 
     * @param sender The nick of the user that sent the action.
     * @param login The login of the user that sent the action.
     * @param hostname The host name of the user that sent the action.
     * @param target The target of the action, be it a channel or our nick.
     * @param action The action carried out by the user.
     */
    protected void onAction(String sender,
                            String login,
                            String hostname,
                            String target,
                            String action)
    {
        if (logger.isTraceEnabled())
            logger.trace("ACTION on " + target + " : Received from " + sender
                + " " + login + "@" + hostname + " the action: " + action);
        
        MessageIrcImpl actionMessage = new MessageIrcImpl(
                                            action,
                                            MessageIrcImpl.DEFAULT_MIME_TYPE,
                                            MessageIrcImpl.DEFAULT_MIME_ENCODING,
                                            null);

        // We presume that the target is a chat room, as we have not yet
        // implemented private messages.
        ChatRoomIrcImpl chatRoom = ircMUCOpSet.getChatRoom(target);

        if (chatRoom == null)
            chatRoom = ircMUCOpSet.findSystemRoom();

        if(chatRoom == null || !chatRoom.isJoined())
            return;

        ChatRoomMember sourceMember = chatRoom.getChatRoomMember(sender);

        if (sourceMember == null)
            return;

        chatRoom.fireMessageReceivedEvent(
            actionMessage,
            sourceMember,
            System.currentTimeMillis(),
            ChatRoomMessageReceivedEvent.ACTION_MESSAGE_RECEIVED);
    }

    /**
     * After calling the listChannels() method in PircBot, the server
     * will start to send us information about each channel on the
     * server.
     * 
     * @param channel The name of the channel.
     * @param userCount The number of users visible in this channel.
     * @param topic The topic for this channel.
     */
    protected void onChannelInfo(String channel, int userCount, String topic)
    {
        this.addServerChatRoom(channel);
    }

    /**
     * Called when a user (possibly us) gets operator status taken away.
     *  <p>
     * This is a type of mode change and is also passed to the onMode
     * method in the PircBot class.
     * 
     * @param channel The channel in which the mode change took place.
     * @param sourceNick The nick of the user that performed the mode change.
     * @param sourceLogin The login of the user that performed the mode change.
     * @param sourceHostname The host name of the user that performed the mode
     * change.
     * @param recipient The nick of the user that got 'de-opp-ed'.
     */
    protected void onDeop(String channel,
                          String sourceNick,
                          String sourceLogin,
                          String sourceHostname,
                          String recipient)
    {
        if (logger.isTraceEnabled())
            logger.trace("DEOP on " + channel + ": Received from " + sourceNick
                + " " + sourceLogin + "@" + sourceHostname + "on " + recipient);

        ChatRoomIrcImpl chatRoom = ircMUCOpSet.getChatRoom(channel);

        if (chatRoom == null || !chatRoom.isJoined())
            return;

        ChatRoomMember sourceMember = chatRoom.getChatRoomMember(sourceNick);

        if (sourceMember == null)
            return;

        chatRoom.fireMemberRoleEvent(sourceMember, ChatRoomMemberRole.GUEST);
    }

    /**
     * Called when a user (possibly us) gets voice status removed.
     *  <p>
     * This is a type of mode change and is also passed to the onMode
     * method in the PircBot class.
     * 
     * @param channel The channel in which the mode change took place.
     * @param sourceNick The nick of the user that performed the mode change.
     * @param sourceLogin The login of the user that performed the mode change.
     * @param sourceHostname The host name of the user that performed the mode
     * change.
     * @param recipient The nick of the user that got 'de-voiced'.
     */
    protected void onDeVoice(String channel, String sourceNick,
        String sourceLogin, String sourceHostname, String recipient)
    {
        if (logger.isDebugEnabled())
            logger.debug("DEVOICE on " + channel + ": Received from "
                + sourceNick + " " + sourceLogin + "@" + sourceHostname + "on "
                + recipient);

        ChatRoomIrcImpl chatRoom = ircMUCOpSet.getChatRoom(channel);

        if (chatRoom == null || !chatRoom.isJoined())
            return;

        ChatRoomMember sourceMember = chatRoom.getChatRoomMember(sourceNick);

        if (sourceMember == null)
            return;

        chatRoom.fireMemberRoleEvent(   sourceMember,
                                        ChatRoomMemberRole.SILENT_MEMBER);
    }

    /**
     * Called when we are invited to a channel by a user.
     * 
     * @param targetNick The nick of the user being invited - should be us!
     * @param sourceNick The nick of the user that sent the invitation.
     * @param sourceLogin The login of the user that sent the invitation.
     * @param sourceHostname The host name of the user that sent the invitation.
     * @param channel The channel that we're being invited to.
     */
    protected void onInvite(String targetNick,
                            String sourceNick,
                            String sourceLogin,
                            String sourceHostname,
                            String channel)
    {
        if (logger.isTraceEnabled())
            logger.trace("INVITE on " + channel + ": Received from "
                + sourceNick + " " + sourceLogin + "@" + sourceHostname);

        ChatRoom targetChatRoom = ircMUCOpSet.findRoom(channel);

        ircMUCOpSet.fireInvitationEvent(targetChatRoom, 
                                        sourceNick,
                                        "",
                                        null);
    }

    /**
     * This method is called whenever someone (possibly us) joins a channel
     * which we are on.
     * 
     * @param channel The channel which somebody joined.
     * @param sender The nick of the user who joined the channel.
     * @param login The login of the user who joined the channel.
     * @param hostname The host name of the user who joined the channel.
     */
    protected void onJoin(  String channel,
                            String sender,
                            String login,
                            String hostname)
    {
        if (logger.isTraceEnabled())
            logger.trace("JOIN on " + channel + ": Received from " + sender
                + " " + login + "@" + hostname);

        ChatRoomIrcImpl chatRoom
            = (ChatRoomIrcImpl) ircMUCOpSet.findRoom(channel);

        if(joinTimeoutTimers.containsKey(chatRoom))
        {
            Timer timer = joinTimeoutTimers.get(chatRoom);

            timer.cancel();
            joinTimeoutTimers.remove(chatRoom);
        }

        if(chatRoom.getUserNickname().equals(sender))
        {
            ircMUCOpSet.fireLocalUserPresenceEvent(
                chatRoom,
                LocalUserChatRoomPresenceChangeEvent.LOCAL_USER_JOINED,
                "");
        }
        else
        {
            ChatRoomMemberIrcImpl member = new ChatRoomMemberIrcImpl(
                    parentProvider,
                    chatRoom,
                    sender,
                    ChatRoomMemberRole.GUEST);

            chatRoom.addChatRoomMember(sender, member);

            //we don't specify a reason
            chatRoom.fireMemberPresenceEvent(
                member,
                null,
                ChatRoomMemberPresenceChangeEvent.MEMBER_JOINED,
                null);
        }
    }

    /**
     * This method is called whenever someone (possibly us) is kicked from
     * any of the channels that we are in.
     * 
     * @param channel The channel from which the recipient was kicked.
     * @param kickerNick The nick of the user who performed the kick.
     * @param kickerLogin The login of the user who performed the kick.
     * @param kickerHostname The host name of the user who performed the kick.
     * @param recipientNick The unfortunate recipient of the kick.
     * @param reason The reason given by the user who performed the kick.
     */
    protected void onKick(  String channel,
                            String kickerNick,
                            String kickerLogin,
                            String kickerHostname,
                            String recipientNick,
                            String reason)
    {
        if (logger.isTraceEnabled())
            logger.trace("KICK on " + channel
                    + ": Received from " + kickerNick
                    + " " + kickerLogin + "@" + kickerHostname);

        ChatRoomIrcImpl chatRoom = ircMUCOpSet.getChatRoom(channel);

        if (chatRoom == null || !chatRoom.isJoined())
            return;

        if(chatRoom.getUserNickname().equals(kickerNick))
        {
            notifyChatRoomOperation(0);
        }

        if(chatRoom.getUserNickname().equals(recipientNick))
            ircMUCOpSet.fireLocalUserPresenceEvent(
                chatRoom,
                LocalUserChatRoomPresenceChangeEvent.LOCAL_USER_KICKED,
                reason);
        else
        {
            ChatRoomMember member
                = chatRoom.getChatRoomMember(recipientNick);

            ChatRoomMember actorMember
                = chatRoom.getChatRoomMember(kickerNick);

            chatRoom.removeChatRoomMember(recipientNick);

            chatRoom.fireMemberPresenceEvent(
                member,
                actorMember,
                ChatRoomMemberPresenceChangeEvent.MEMBER_KICKED,
                reason);
        }
    }

    /**
     * This method is called whenever someone (possibly us) changes nick on any
     * of the channels that we are on.
     * 
     * @param oldNick The old nick.
     * @param login The login of the user.
     * @param hostname The host name of the user.
     * @param newNick The new nick.
     */
    protected void onNickChange(String oldNick,
                                String login,
                                String hostname,
                                String newNick)
    {
        if (logger.isTraceEnabled())
            logger.trace("NICK changed: from " + oldNick + " changed to "
                + newNick);

        this.notifyChatRoomOperation(0);

        for (ChatRoom chatRoom : ircMUCOpSet.getCurrentlyJoinedChatRooms())
        {
            ChatRoomIrcImpl chatRoomIrcImpl = (ChatRoomIrcImpl) chatRoom;

            if (chatRoom.getUserNickname().equals(oldNick))
            {
                chatRoomIrcImpl.setNickName(newNick);
                return;
            }

            ChatRoomMember member = chatRoomIrcImpl.getChatRoomMember(oldNick);

            if (member == null)
                continue;

            ChatRoomMemberPropertyChangeEvent evt
                = new ChatRoomMemberPropertyChangeEvent(
                        member,
                        chatRoom,
                        ChatRoomMemberPropertyChangeEvent.MEMBER_NICKNAME,
                        oldNick,
                        newNick);

            chatRoomIrcImpl.fireMemberPropertyChangeEvent(evt);
        }
    }

    /**
     * This method is called whenever we receive a notice.
     * 
     * @param sourceNick The nick of the user that sent the notice.
     * @param sourceLogin The login of the user that sent the notice.
     * @param sourceHostname The host name of the user that sent the notice.
     * @param target The target of the notice, be it our nick or a channel name.
     * @param notice The notice message.
     */
    protected void onNotice(String sourceNick,
                            String sourceLogin,
                            String sourceHostname,
                            String target,
                            String notice)
    {
        if (logger.isTraceEnabled())
            logger.trace("NOTICE on " + target + ": Received from "
                + sourceNick + " " + sourceLogin + "@" + sourceHostname
                + " the message: " + notice);

        MessageIrcImpl message
            = new MessageIrcImpl(   notice,
                                    MessageIrcImpl.DEFAULT_MIME_TYPE,
                                    MessageIrcImpl.DEFAULT_MIME_ENCODING,
                                    null);

        ChatRoomIrcImpl chatRoom = ircMUCOpSet.getChatRoom(target);

        ChatRoomMember sourceMember = null;

        if(chatRoom == null || !chatRoom.isJoined())
        {
            chatRoom = ircMUCOpSet.findSystemRoom();

            sourceMember = ircMUCOpSet.findSystemMember();
        }
        else
        {
            sourceMember = chatRoom.getChatRoomMember(sourceNick);
        }

        if (sourceMember == null)
            return;

        chatRoom.fireMessageReceivedEvent(
                message,
                sourceMember,
                System.currentTimeMillis(),
                ChatRoomMessageReceivedEvent.ACTION_MESSAGE_RECEIVED);
    }

    /**
     * Called when a user (possibly us) gets granted operator status for a
     * channel.
     *  <p>
     * This is a type of mode change and is also passed to the onMode
     * method in the PircBot class.
     * 
     * @param channel The channel in which the mode change took place.
     * @param sourceNick The nick of the user that performed the mode change.
     * @param sourceLogin The login of the user that performed the mode change.
     * @param sourceHostname The host name of the user that performed the mode 
     * change.
     * @param recipient The nick of the user that got 'opp-ed'.
     */
    protected void onOp(String channel,
                        String sourceNick,
                        String sourceLogin,
                        String sourceHostname,
                        String recipient)
    {
        if (logger.isDebugEnabled())
            logger.debug("MODE OP on " + channel + ": from " + sourceNick + " "
                + sourceLogin + "@" + sourceHostname + " on " + recipient);

        ChatRoomIrcImpl chatRoom = ircMUCOpSet.getChatRoom(channel);

        if (chatRoom == null || !chatRoom.isJoined())
            return;

        ChatRoomMember sourceMember = chatRoom.getChatRoomMember(sourceNick);

        if (sourceMember == null)
            return;

        chatRoom.fireMemberRoleEvent(   sourceMember,
                                        ChatRoomMemberRole.ADMINISTRATOR);
    }

    /**
     * This method is called whenever someone (possibly us) leaves a channel
     * which we are on.
     * 
     * @param channel The channel which somebody parted from.
     * @param sender The nick of the user who parted from the channel.
     * @param login The login of the user who parted from the channel.
     * @param hostname The host name of the user who parted from the channel.
     */
    protected void onPart(  String channel,
                            String sender,
                            String login,
                            String hostname)
    {
        if (logger.isDebugEnabled())
            logger.debug("LEAVE on " + channel + ": Received from " + sender
                + " " + login + "@" + hostname);

        ChatRoomIrcImpl chatRoom
            = (ChatRoomIrcImpl) ircMUCOpSet.findRoom(channel);

        if(chatRoom.getUserNickname().equals(sender))
        {
            ircMUCOpSet.fireLocalUserPresenceEvent(
                chatRoom,
                LocalUserChatRoomPresenceChangeEvent.LOCAL_USER_LEFT,
                "");

            for (ChatRoomMember member : chatRoom.getMembers())
                chatRoom.fireMemberPresenceEvent(
                    member,
                    null,
                    ChatRoomMemberPresenceChangeEvent.MEMBER_LEFT,
                    "Local user has left the chat room.");

            // Delete the list of members
            chatRoom.clearChatRoomMemberList();
        }
        else
        {
            ChatRoomMember member = chatRoom.getChatRoomMember(sender);

            if (member == null)
                return;

            chatRoom.removeChatRoomMember(sender);

            //we don't specify a reason
            chatRoom.fireMemberPresenceEvent(
                member,
                null,
                ChatRoomMemberPresenceChangeEvent.MEMBER_LEFT,
                null);
        }
    }

    /**
     * This method is called whenever someone (possibly us) quits from the
     * server. We will only observe this if the user was in one of the
     * channels to which we are connected.
     * 
     * @param sourceNick The nick of the user that quit from the server.
     * @param sourceLogin The login of the user that quit from the server.
     * @param sourceHostname The host name of the user that quit from the server.
     * @param reason The reason given for quitting the server.
     */
    protected void onQuit(  String sourceNick,
                            String sourceLogin,
                            String sourceHostname,
                            String reason)
    {
        if (logger.isDebugEnabled())
            logger.debug("QUIT : Received from " + sourceNick + " "
                + sourceLogin + "@" + sourceHostname);

        for (ChatRoom chatRoom : ircMUCOpSet.getCurrentlyJoinedChatRooms())
        {
            ChatRoomIrcImpl chatRoomIrcImpl = (ChatRoomIrcImpl) chatRoom;

            if(chatRoom.getUserNickname().equals(sourceNick))
                ircMUCOpSet.fireLocalUserPresenceEvent(
                    chatRoom,
                    LocalUserChatRoomPresenceChangeEvent.LOCAL_USER_DROPPED,
                    reason);
            else
            {
                ChatRoomMember member
                    = chatRoomIrcImpl.getChatRoomMember(sourceNick);

                if (member == null)
                    return;

                chatRoomIrcImpl.removeChatRoomMember(sourceNick);

                chatRoomIrcImpl.fireMemberPresenceEvent(
                    member,
                    null,
                    ChatRoomMemberPresenceChangeEvent.MEMBER_QUIT,
                    reason);
            }
        }
    }

    /**
     * Called when a host mask ban is removed from a channel.
     *  <p>
     * This is a type of mode change and is also passed to the onMode
     * method in the PircBot class.
     * 
     * @param channel The channel in which the mode change took place.
     * @param sourceNick The nick of the user that performed the mode change.
     * @param sourceLogin The login of the user that performed the mode change.
     * @param sourceHostname The host name of the user that performed the mode
     * change.
     * @param hostmask
     */
    protected void onRemoveChannelBan(  String channel,
                                        String sourceNick,
                                        String sourceLogin,
                                        String sourceHostname,
                                        String hostmask)
    {
        if (logger.isDebugEnabled())
            logger.debug("MODE on " + channel + ": Received from " + sourceNick
                + " " + sourceLogin + "@" + sourceHostname);

        ChatRoom chatRoom = ircMUCOpSet.getChatRoom(channel);

        if (chatRoom == null)
            return;

        //TODO: Implement IrcStack.onRemoveChannelBan.
    }

    /**
     * Called when a channel key is removed.
     * 
     * @param channel The channel in which the mode change took place.
     * @param sourceNick The nick of the user that performed the mode change.
     * @param sourceLogin The login of the user that performed the mode change.
     * @param sourceHostname The host name of the user that performed the mode
     * change.
     * @param key The key that was in use before the channel key was removed.
     */
    protected void onRemoveChannelKey(  String channel,
                                        String sourceNick,
                                        String sourceLogin,
                                        String sourceHostname,
                                        String key)
    {
        if (logger.isDebugEnabled())
            logger.debug("MODE on " + channel + ": Received from " + sourceNick
                + " " + sourceLogin + "@" + sourceHostname);
        
        //TODO: Implement IrcStack.onRemoveChannelKey().
    }

    /**
     * Called when the user limit is removed for a channel.
     * 
     * @param channel The channel in which the mode change took place.
     * @param sourceNick The nick of the user that performed the mode change.
     * @param sourceLogin The login of the user that performed the mode change.
     * @param sourceHostname The host name of the user that performed the mode
     * change.
     */
    protected void onRemoveChannelLimit(String channel, String sourceNick,
        String sourceLogin, String sourceHostname)
    {
        if (logger.isDebugEnabled())
            logger.debug("MODE on " + channel + ": Received from " + sourceNick
                + " " + sourceLogin + "@" + sourceHostname);

        ChatRoom chatRoom = ircMUCOpSet.getChatRoom(channel);

        if (chatRoom == null)
            return;

      //TODO: Implement IrcStack.onRemoveChannelLimit().
    }

    /**
     * Called when a channel has 'invite only' removed.
     * 
     * @param channel The channel in which the mode change took place.
     * @param sourceNick The nick of the user that performed the mode change.
     * @param sourceLogin The login of the user that performed the mode change.
     * @param sourceHostname The host name of the user that performed the mode
     * change.
     */
    protected void onRemoveInviteOnly(  String channel,
                                        String sourceNick,
                                        String sourceLogin,
                                        String sourceHostname)
    {
        if (logger.isDebugEnabled())
            logger.debug("MODE on " + channel + ": Received from " + sourceNick
                + " " + sourceLogin + "@" + sourceHostname);

        // TODO: Implement IrcStack.onRemoveInviteOnly().
    }

    /**
     * Called when a channel has moderated mode removed.
     * 
     * @param channel The channel in which the mode change took place.
     * @param sourceNick The nick of the user that performed the mode change.
     * @param sourceLogin The login of the user that performed the mode change.
     * @param sourceHostname The host name of the user that performed the mode
     * change.
     */
    protected void onRemoveModerated(   String channel,
                                        String sourceNick,
                                        String sourceLogin,
                                        String sourceHostname)
    {
        if (logger.isDebugEnabled())
            logger.debug("MODE on " + channel + ": Received from " + sourceNick
                + " " + sourceLogin + "@" + sourceHostname);

        // TODO: Implement IrcStack.onRemoveModerated().
    }

    /**
     * Called when a channel is set to allow messages from any user, even
     * if they are not actually in the channel.
     * 
     * @param channel The channel in which the mode change took place.
     * @param sourceNick The nick of the user that performed the mode change.
     * @param sourceLogin The login of the user that performed the mode change.
     * @param sourceHostname The host name of the user that performed the mode
     * change.
     */
    protected void onRemoveNoExternalMessages(  String channel,
                                                String sourceNick,
                                                String sourceLogin,
                                                String sourceHostname)
    {
        if (logger.isDebugEnabled())
            logger.debug("MODE on " + channel + ": Received from " + sourceNick
                + " " + sourceLogin + "@" + sourceHostname);

        // TODO: Implement IrcStack.onRemoveNoExternalMessages().
    }

    /**
     * Called when a channel is marked as not being in private mode.
     * 
     * @param channel The channel in which the mode change took place.
     * @param sourceNick The nick of the user that performed the mode change.
     * @param sourceLogin The login of the user that performed the mode change.
     * @param sourceHostname The host name of the user that performed the mode
     * change.
     */
    protected void onRemovePrivate( String channel,
                                    String sourceNick,
                                    String sourceLogin,
                                    String sourceHostname)
    {
        if (logger.isDebugEnabled())
            logger.debug("MODE on " + channel + ": Received from " + sourceNick
                + " " + sourceLogin + "@" + sourceHostname);

        // TODO: Implement IrcStack.onRemovePrivate().
    }

    /**
     * Called when a channel has 'secret' mode removed.
     * 
     * @param channel The channel in which the mode change took place.
     * @param sourceNick The nick of the user that performed the mode change.
     * @param sourceLogin The login of the user that performed the mode change.
     * @param sourceHostname The host name of the user that performed the mode
     * change.
     */
    protected void onRemoveSecret(  String channel,
                                    String sourceNick,
                                    String sourceLogin,
                                    String sourceHostname)
    {
        if (logger.isDebugEnabled())
            logger.debug("MODE on " + channel + ": Received from " + sourceNick
                + " " + sourceLogin + "@" + sourceHostname);

        // TODO: Implement IrcStack.onRemoveSecret().
    }

    /**
     * Called when topic protection is removed for a channel.
     * 
     * @param channel The channel in which the mode change took place.
     * @param sourceNick The nick of the user that performed the mode change.
     * @param sourceLogin The login of the user that performed the mode change.
     * @param sourceHostname The host name of the user that performed the mode
     * change.
     */
    protected void onRemoveTopicProtection(String channel, String sourceNick,
        String sourceLogin, String sourceHostname)
    {
        if (logger.isDebugEnabled())
            logger.debug("MODE on " + channel + ": Received from " + sourceNick
                + " " + sourceLogin + "@" + sourceHostname);

        // TODO: Implement IrcStack.onRemoveSecret().
    }

    /**
     * 
     * @param code The three-digit numerical code for the response.
     * @param response The full response from the IRC server.
     * 
     * @see ReplyConstants
     */
    protected void onServerResponse (int code, String response)
    {
        if (code == ERR_NOSUCHCHANNEL)
        {
            logger.error("No such channel:" + code
                + ": Response :" + response);

            this.notifyChatRoomOperation(ERR_NOSUCHCHANNEL);
        }
        else if (code == ERR_BADCHANMASK)
        {
            logger.error("Bad channel mask :" + code
                + ": Response :" + response);

            this.notifyChatRoomOperation(ERR_BADCHANMASK);
        }
        else if (code == ERR_BADCHANNELKEY)
        {
            logger.error("Bad channel key :" + code
                + ": Response :" + response);

            this.notifyChatRoomOperation(ERR_BADCHANNELKEY);
        }
        else if (code == ERR_BANNEDFROMCHAN)
        {
            logger.error("Banned from channel :" + code
                + ": Response :" + response);

            this.notifyChatRoomOperation(ERR_BANNEDFROMCHAN);
        }
        else if (code == ERR_CHANNELISFULL)
        {
            logger.error("Channel is full :" + code
                + ": Response :" + response);

            this.notifyChatRoomOperation(ERR_CHANNELISFULL);
        }
        else if (code == ERR_CHANOPRIVSNEEDED)
        {
            logger.error("Channel operator privilages needed :" + code
                + ": Response :" + response);

            this.notifyChatRoomOperation(ERR_CHANOPRIVSNEEDED);
        }
        else if (code == ERR_ERRONEUSNICKNAME)
        {
            logger.error("ERR_ERRONEUSNICKNAME :" + code
                + ": Response :" + response);

            this.notifyChatRoomOperation(ERR_ERRONEUSNICKNAME);
        }
        else if (code == ERR_INVITEONLYCHAN)
        {
            logger.error("Invite only channel :" + code
                + ": Response :" + response);

            this.notifyChatRoomOperation(ERR_INVITEONLYCHAN);
        }
        else if (code == ERR_NEEDMOREPARAMS)
        {
            logger.error("Need more params :" + code
                + ": Response :" + response);

            this.notifyChatRoomOperation(ERR_NEEDMOREPARAMS);
        }
        else if (code == ERR_NICKCOLLISION)
        {
            logger.error("Nick collision :" + code
                + ": Response :" + response);

            this.notifyChatRoomOperation(ERR_NICKCOLLISION);
        }
        else if (code == ERR_NICKNAMEINUSE)
        {
            logger.error("Nickname in use :" + code
                + ": Response :" + response);

            this.notifyChatRoomOperation(ERR_NICKNAMEINUSE);
        }
        else if (code == ERR_NONICKNAMEGIVEN)
        {
            logger.error("No nickname given :" + code
                + ": Response :" + response);

            this.notifyChatRoomOperation(ERR_NONICKNAMEGIVEN);
        }
        else if (code == ERR_NOTONCHANNEL)
        {
            logger.error("Not on channel :" + code
                + ": Response :" + response);

            this.notifyChatRoomOperation(ERR_NOTONCHANNEL);
        }
        else if (code == ERR_TOOMANYCHANNELS)
        {
            logger.error("Too many channels :" + code
                + ": Response :" + response);

            this.notifyChatRoomOperation(ERR_TOOMANYCHANNELS);
        }
        // reply responses
        else if (code == RPL_WHOISUSER)
        {
            StringTokenizer tokenizer = new StringTokenizer(response);
            tokenizer.nextToken();

            String nickname = tokenizer.nextToken();
            String login = tokenizer.nextToken();
            String hostname = tokenizer.nextToken();

            UserInfo userInfo = new UserInfo(nickname, login, hostname);

            this.userInfoTable.put(nickname, userInfo);
        }
        else if (code == RPL_WHOISSERVER)
        {
            StringTokenizer tokenizer = new StringTokenizer(response);
            tokenizer.nextToken();
            String userNickName = tokenizer.nextToken();

            int end = response.indexOf(':');
            String serverInfo = response.substring(end + 1);

            if (userInfoTable.containsKey(userNickName))
            {
                userInfoTable.get(userNickName).setServerInfo(serverInfo);
            }
        }
        else if (code == RPL_WHOISOPERATOR)
        {
            StringTokenizer tokenizer = new StringTokenizer(response);
            tokenizer.nextToken();
            String userNickName = tokenizer.nextToken();

            if (userInfoTable.containsKey(userNickName))
            {
                userInfoTable.get(userNickName).setIrcOp(true);
            }
        }
        else if (code == RPL_WHOISIDLE)
        {
            StringTokenizer tokenizer = new StringTokenizer(response);
            tokenizer.nextToken();
            String userNickName = tokenizer.nextToken();
            long idle = Long.parseLong(tokenizer.nextToken());

            if (userInfoTable.containsKey(userNickName))
            {
                userInfoTable.get(userNickName).setIdle(idle);
            }
        }
        else if (code == RPL_WHOISCHANNELS)
        {
            StringTokenizer tokenizer = new StringTokenizer(response);
            tokenizer.nextToken();
            String userNickName = tokenizer.nextToken();

            if (userInfoTable.containsKey(userNickName))
            {
                userInfoTable.get(userNickName).clearJoinedChatRoom();

                while(tokenizer.hasMoreTokens())
                {
                    String channel = tokenizer.nextToken();

                    if(channel.startsWith(":"))
                        channel = channel.substring(1);

                    userInfoTable.get(userNickName).addJoinedChatRoom(channel);
                }
            }
        }
        else if (code == RPL_ENDOFWHOIS)
        {
            StringTokenizer tokenizer = new StringTokenizer(response);
            tokenizer.nextToken();
            String userNickName = tokenizer.nextToken();

            if (userInfoTable.containsKey(userNickName))
            {
                UserInfo userInfo = userInfoTable.get(userNickName);
                
                this.onWhoIs(userInfo);
            }
        }
        else if (code == RPL_ENDOFMOTD)
        {
            this.isInitialized = true;

            ChatRoom[] joinCacheCopy
                = joinCache.toArray(new ChatRoom[joinCache.size()]);

            joinCache.clear();

            for (ChatRoom joinCacheElement : joinCacheCopy)
            {
                this.join(joinCacheElement);
            }
        }
        else if (code != RPL_LISTSTART
                    && code != RPL_LIST
                    && code != RPL_LISTEND
                    && code != RPL_ENDOFNAMES)
        {
            if (logger.isTraceEnabled())
                logger.trace(
                "Server response: Code : "
                + code
                + " Response : "
                + response);

            int delimiterIndex = response.indexOf(':');

            if(delimiterIndex != -1 && delimiterIndex < response.length() - 1)
                response = response.substring(delimiterIndex + 1);

            MessageIrcImpl message
                = new MessageIrcImpl(
                    response,
                    MessageIrcImpl.DEFAULT_MIME_TYPE,
                    MessageIrcImpl.DEFAULT_MIME_ENCODING,
                    null);

            ChatRoomIrcImpl serverRoom = ircMUCOpSet.findSystemRoom();

            ChatRoomMember serverMember = ircMUCOpSet.findSystemMember();

            serverRoom.fireMessageReceivedEvent(
                    message,
                    serverMember,
                    System.currentTimeMillis(),
                    ChatRoomMessageReceivedEvent.SYSTEM_MESSAGE_RECEIVED);
        }
    }

    /**
     * Called when a user (possibly us) gets banned from a channel. Being
     * banned from a channel prevents any user with a matching host mask from
     * joining the channel.  For this reason, most bans are usually directly
     * followed by the user being kicked .
     * 
     * @param channel The channel in which the mode change took place.
     * @param sourceNick The nick of the user that performed the mode change.
     * @param sourceLogin The login of the user that performed the mode change.
     * @param sourceHostname The host name of the user that performed the mode
     * change.
     * @param hostmask The host mask of the user that has been banned.
     */
    protected void onSetChannelBan( String channel,
                                    String sourceNick,
                                    String sourceLogin,
                                    String sourceHostname,
                                    String hostmask)
    {
        if (logger.isDebugEnabled())
            logger.debug("MODE on " + channel + ": Received from " + sourceNick
                + " " + sourceLogin + "@" + sourceHostname);

        // TODO: Implement IrcStack.onSetChannelBan().
    }

    /**
     * Called when a channel key is set.  When the channel key has been set,
     * other users may only join that channel if they know the key.  Channel
     * keys are sometimes referred to as passwords.
     * 
     * @param channel The channel in which the mode change took place.
     * @param sourceNick The nick of the user that performed the mode change.
     * @param sourceLogin The login of the user that performed the mode change.
     * @param sourceHostname The host name of the user that performed the mode
     * change.
     * @param key The new key for the channel.
     */
    protected void onSetChannelKey(String channel, String sourceNick,
        String sourceLogin, String sourceHostname, String key)
    {
        if (logger.isDebugEnabled())
            logger.debug("MODE on " + channel + ": Received from " + sourceNick
                + " " + sourceLogin + "@" + sourceHostname);

     // TODO: Implement IrcStack.onSetChannelKey().
    }

    /**
     * Called when a user limit is set for a channel.  The number of users in
     * the channel cannot exceed this limit.
     * 
     * @param channel The channel in which the mode change took place.
     * @param sourceNick The nick of the user that performed the mode change.
     * @param sourceLogin The login of the user that performed the mode change.
     * @param sourceHostname The host name of the user that performed the mode
     * change.
     * @param limit The maximum number of users that may be in this channel at
     * the same time.
     */
    protected void onSetChannelLimit(   String channel,
                                        String sourceNick,
                                        String sourceLogin,
                                        String sourceHostname,
                                        int limit)
    {
        if (logger.isDebugEnabled())
            logger.debug("MODE on " + channel + ": Received from " + sourceNick
                + " " + sourceLogin + "@" + sourceHostname);

        // TODO: Implement IrcStack.onSetChannelLimit().
    }

    /**
     * Called when a channel is set to 'invite only' mode.  A user may only
     * join the channel if they are invited by someone who is already in the
     * channel.
     * 
     * @param channel The channel in which the mode change took place.
     * @param sourceNick The nick of the user that performed the mode change.
     * @param sourceLogin The login of the user that performed the mode change.
     * @param sourceHostname The host name of the user that performed the mode
     * change.
     */
    protected void onSetInviteOnly( String channel,
                                    String sourceNick,
                                    String sourceLogin,
                                    String sourceHostname)
    {
        if (logger.isDebugEnabled())
            logger.debug("MODE on " + channel + ": Received from " + sourceNick
                + " " + sourceLogin + "@" + sourceHostname);

        // TODO: Implement IrcStack.onSetChannelLimit().
    }

    /**
     * Called when a channel is set to 'moderated' mode. If a channel is
     * moderated, then only users who have been 'voiced' or 'opp-ed' may speak
     * or change their nicks.
     * 
     * @param channel The channel in which the mode change took place.
     * @param sourceNick The nick of the user that performed the mode change.
     * @param sourceLogin The login of the user that performed the mode change.
     * @param sourceHostname The host name of the user that performed the mode
     * change.
     */
    protected void onSetModerated(  String channel,
                                    String sourceNick,
                                    String sourceLogin,
                                    String sourceHostname)
    {
        if (logger.isDebugEnabled())
            logger.debug("MODE on " + channel + ": Received from " + sourceNick
                + " " + sourceLogin + "@" + sourceHostname);

        // TODO: Implement IrcStack.onSetModerated().
    }

    /**
     * Called when a channel is set to only allow messages from users that
     * are in the channel.
     * 
     * @param channel The channel in which the mode change took place.
     * @param sourceNick The nick of the user that performed the mode change.
     * @param sourceLogin The login of the user that performed the mode change.
     * @param sourceHostname The hostname of the user that performed the mode
     * change.
     */
    protected void onSetNoExternalMessages( String channel,
                                            String sourceNick,
                                            String sourceLogin,
                                            String sourceHostname)
    {
        if (logger.isDebugEnabled())
            logger.debug("MODE on " + channel + ": Received from " + sourceNick
                + " " + sourceLogin + "@" + sourceHostname);

        // TODO: Implement IrcStack.onSetNoExternalMessages().
    }

    /**
     * Called when a channel is marked as being in private mode.
     * 
     * @param channel The channel in which the mode change took place.
     * @param sourceNick The nick of the user that performed the mode change.
     * @param sourceLogin The login of the user that performed the mode change.
     * @param sourceHostname The host name of the user that performed the mode
     * change.
     */
    protected void onSetPrivate(String channel, String sourceNick,
        String sourceLogin, String sourceHostname)
    {
        if (logger.isDebugEnabled())
            logger.debug("MODE on " + channel + ": Received from " + sourceNick
                + " " + sourceLogin + "@" + sourceHostname);

        // TODO: Implement IrcStack.onSetPrivate().
    }

    /**
     * Called when a channel is set to be in 'secret' mode.  Such channels
     * typically do not appear on a server's channel listing.
     * 
     * @param channel The channel in which the mode change took place.
     * @param sourceNick The nick of the user that performed the mode change.
     * @param sourceLogin The login of the user that performed the mode change.
     * @param sourceHostname The host name of the user that performed the mode
     * change.
     */
    protected void onSetSecret(String channel, String sourceNick,
        String sourceLogin, String sourceHostname)
    {
        if (logger.isDebugEnabled())
            logger.debug("MODE on " + channel + ": Received from " + sourceNick
                + " " + sourceLogin + "@" + sourceHostname);

        //TODO: Implement IrcStack.onSetPrivate().
    }

    /**
     * Called when topic protection is enabled for a channel.  Topic protection
     * means that only operators in a channel may change the topic.
     * 
     * @param channel The channel in which the mode change took place.
     * @param sourceNick The nick of the user that performed the mode change.
     * @param sourceLogin The login of the user that performed the mode change.
     * @param sourceHostname The host name of the user that performed the mode
     * change.
     */
    protected void onSetTopicProtection(String channel, String sourceNick,
        String sourceLogin, String sourceHostname)
    {
        if (logger.isDebugEnabled())
            logger.debug("MODE on " + channel + ": Received from " + sourceNick
                + " " + sourceLogin + "@" + sourceHostname);

        //TODO: Implement IrcStack.onSetPrivate().
    }

    /**
     * This method is called whenever a user sets the topic, or when
     * PircBot joins a new channel and discovers its topic.
     * 
     * @param channel The channel that the topic belongs to.
     * @param topic The topic for the channel.
     * @param setBy The nick of the user that set the topic.
     * @param date When the topic was set (milliseconds since the epoch).
     * @param changed True if the topic has just been changed, false if
     *                the topic was already there.
     * 
     */
    protected void onTopic( String channel,
                            String topic,
                            String setBy,
                            long date,
                            boolean changed)
    {
        if (logger.isTraceEnabled())
            logger.trace("TOPIC on " + channel + ": " + topic + " setBy: "
                + setBy + " on: " + date);

        this.notifyChatRoomOperation(0);

        ChatRoomIrcImpl chatRoom = ircMUCOpSet.getChatRoom(channel);

        ChatRoomPropertyChangeEvent evt
            = new ChatRoomPropertyChangeEvent(
                chatRoom,
                ChatRoomPropertyChangeEvent.CHAT_ROOM_SUBJECT,
                chatRoom.getSubject(),
                topic);

        // After creating the event with the old and new value of the subject
        // we could change the subject property of the chat room.
        chatRoom.setSubjectFromServer(topic);

        chatRoom.firePropertyChangeEvent(evt);
    }

    /**
     * This method is called whenever we receive a line from the server that
     * the PircBot has not been programmed to recognize.
     * 
     * @param line The raw line that was received from the server.
     */
    protected void onUnknown(String line)
    {
        if (logger.isTraceEnabled())
            logger.trace("Unknown message received from the server : " + line);
    }

    /**
     * This method is called when we receive a user list from the server
     * after joining a channel.
     * 
     * @param channel The name of the channel.
     * @param users An array of User objects belonging to this channel.
     * 
     * @see User
     */
    protected void onUserList(String channel, User[] users)
    {
        if (logger.isDebugEnabled())
            logger.debug("NAMES on " + channel);

        ChatRoomIrcImpl chatRoom = ircMUCOpSet.getChatRoom(channel);

        chatRoom.clearChatRoomMemberList();

        for (User user : users)
        {
            String userPrefix = user.getPrefix();
            ChatRoomMemberRole newMemberRole;

            if (userPrefix.contains("@"))
                newMemberRole = ChatRoomMemberRole.ADMINISTRATOR;
            else if (userPrefix.contains("%"))
                newMemberRole = ChatRoomMemberRole.MODERATOR;
            else if (userPrefix.contains("+"))
                newMemberRole = ChatRoomMemberRole.MEMBER;
            else
                newMemberRole = ChatRoomMemberRole.GUEST;

            ChatRoomMemberIrcImpl newMember
                = new ChatRoomMemberIrcImpl(parentProvider,
                                            chatRoom,
                                            user.getNick(),
                                            newMemberRole);

            chatRoom.addChatRoomMember(user.getNick(), newMember);

            chatRoom.fireMemberPresenceEvent(
                newMember,
                null,
                ChatRoomMemberPresenceChangeEvent.MEMBER_JOINED,
                ChatRoomMemberPresenceChangeEvent.REASON_USER_LIST);
        }
    }

    /**
     * Called when a user (possibly us) gets voice status granted in a channel.
     * 
     * @param channel The channel in which the mode change took place.
     * @param sourceNick The nick of the user that performed the mode change.
     * @param sourceLogin The login of the user that performed the mode change.
     * @param sourceHostname The host name of the user that performed the mode
     * change.
     * @param recipient The nick of the user that got 'voiced'.
     */
    protected void onVoice(String channel, String sourceNick,
        String sourceLogin, String sourceHostname, String recipient)
    {
        if (logger.isDebugEnabled())
            logger.debug("VOICE on " + channel + ": Received from "
                + sourceNick + " " + sourceLogin + "@" + sourceHostname + "on "
                + recipient);

        ChatRoomIrcImpl chatRoom = ircMUCOpSet.getChatRoom(channel);

        if (chatRoom == null || !chatRoom.isJoined())
            return;

        ChatRoomMember sourceMember = chatRoom.getChatRoomMember(sourceNick);

        if (sourceMember == null)
            return;

        chatRoom.fireMemberRoleEvent(   sourceMember,
                                        ChatRoomMemberRole.GUEST);
    }

    /**
     * Returns the list of chat rooms on this server.
     * 
     * @return the list of chat rooms on this server
     */
    public List<String> getServerChatRoomList()
    {
        return serverChatRoomList;
    }

    /**
     * Tests if this chat room is joined
     * 
     * @param chatRoom the chat room we want to test
     * @return true if the chat room is joined, false otherwise
     */
    protected boolean isJoined(ChatRoomIrcImpl chatRoom)
    {
        // If we are asked for the status of the server channel, we return true
        // if the server is connected and false otherwise.
        if(ircMUCOpSet.findSystemRoom().equals(chatRoom))
            return isConnected();

        // Private rooms are joined if they exist.
        if(chatRoom.isPrivate())
            return true;

        // For all other channels on the server.
        if (this.isConnected())
        {
            String[] channels = this.getChannels();

            for (String channel : channels)
            {
                if (chatRoom.getName().equals(channel))
                    return true;
            }
            return false;
        }
        else
        {
            return false;
        }
    }

    /**
     * Join a chat room on this server.
     * 
     * @param chatRoom the chat room to join
     */
    public void join(ChatRoom chatRoom)
    {
        if (!isInitialized)
        {
            joinCache.add(chatRoom);

            return;
        }

        this.joinChannel(chatRoom.getName());

        Timer joinTimeoutTimer = new Timer();

        joinTimeoutTimers.put(chatRoom, joinTimeoutTimer);

        joinTimeoutTimer.schedule(new JoinTimeoutTask(chatRoom), TIMEOUT);
    }

    /**
     * Join a chat room on this server.
     * 
     * @param chatRoom the chat room to join
     * @param password the password of the chat room
     */
    public void join(ChatRoom chatRoom, byte[] password)
    {
        this.joinChannel(chatRoom.getName(), new String(password));

        Timer joinTimeoutTimer = new Timer();

        joinTimeoutTimers.put(chatRoom, joinTimeoutTimer);
        
        joinTimeoutTimer.schedule(new JoinTimeoutTask(chatRoom), TIMEOUT);
    }

    /**
     * Leaves the given chat room.
     * 
     * @param chatRoom the chat room we want to leave
     */
    protected void leave(ChatRoom chatRoom)
    {
        this.partChannel(chatRoom.getName());
    }

    /**
     * This method sends a command to the server which can also be an action or
     * a notice.
     * 
     * @param chatRoom the chat room corresponding to the command
     * @param command the command we want to send
     */
    protected void sendCommand(ChatRoomIrcImpl chatRoom, String command)
    {
        if (command.startsWith("/me"))
        {
            this.sendAction(chatRoom.getName(), command.substring(3));
        }
        else if (command.startsWith("/notice"))
        {
            this.sendNotice(chatRoom.getName(), command.substring(7));
        }
        else if (command.startsWith("/msg"))
        {
            StringTokenizer tokenizer = new StringTokenizer(command);

            String target = "";
            String messageContent = "";

            // We don't need the /msg command text.
            tokenizer.nextToken();

            if(tokenizer.hasMoreTokens())
                target = tokenizer.nextToken();

            while(tokenizer.hasMoreTokens())
            {
                messageContent += tokenizer.nextToken() + " ";
            }

            this.sendMessage(target, messageContent);
        }
        else if (command.startsWith("/query"))
        {
            StringTokenizer tokenizer = new StringTokenizer(command);

            String target = null;

            tokenizer.nextToken();

            if(tokenizer.hasMoreTokens())
                target = tokenizer.nextToken();

            this.createPrivateChatRoom(target);
        }
        else
        {
            this.sendRawLine(command.substring(1));
        }
    }

    /**
     * Called to ban the given user from the given channel. The reason is not
     * passed to the server, as it doesn't support this parameter.
     * 
     * @param chatRoom the chat room for which the user should be banned
     * @param hostmask the host mask of the user to ban
     * @param reason the reason of the ban
     * @throws OperationFailedException if something goes wrong
     */
    protected void banParticipant( String chatRoom,
                        String hostmask,
                        String reason)
        throws OperationFailedException
    {
        this.ban(chatRoom, hostmask);

        this.lockChatRoomOperation();

        if (operationResponseCode == ERR_NEEDMOREPARAMS)
            throw new OperationFailedException(
                "Need more parameters.",
                OperationFailedException.GENERAL_ERROR);
        else if (operationResponseCode == ERR_CHANOPRIVSNEEDED)
            throw new OperationFailedException(
                "Need more parameters.",
                OperationFailedException.NOT_ENOUGH_PRIVILEGES);
        else if (operationResponseCode == ERR_NOTONCHANNEL)
            throw new OperationFailedException(
                "Need more parameters.",
                OperationFailedException.GENERAL_ERROR);
        else if (operationResponseCode == ERR_USERSDONTMATCH)
            throw new OperationFailedException(
                "Need more parameters.",
                OperationFailedException.GENERAL_ERROR);
        else if (operationResponseCode == ERR_NOSUCHCHANNEL)
            throw new OperationFailedException(
                "Need more parameters.",
                OperationFailedException.GENERAL_ERROR);
        else if (operationResponseCode == ERR_NOSUCHNICK)
            throw new OperationFailedException(
                "Need more parameters.",
                OperationFailedException.GENERAL_ERROR);
        else if (operationResponseCode == ERR_KEYSET)
            throw new OperationFailedException(
                "Need more parameters.",
                OperationFailedException.GENERAL_ERROR);
        else if (operationResponseCode == ERR_UMODEUNKNOWNFLAG)
            throw new OperationFailedException(
                "Need more parameters.",
                OperationFailedException.GENERAL_ERROR);
        else if (operationResponseCode == ERR_UNKNOWNMODE)
            throw new OperationFailedException(
                "Need more parameters.",
                OperationFailedException.GENERAL_ERROR);
    }

    /**
     * Who is.
     * 
     * @param userInfo
     */
    private void onWhoIs(UserInfo userInfo)
    {
        ChatRoomIrcImpl chatRoom = ircMUCOpSet.findSystemRoom();

        if((chatRoom == null) || !chatRoom.isJoined())
            return;

        if (logger.isTraceEnabled())
            logger.trace("WHOIS on: " + userInfo.getNickName() + "!"
                + userInfo.getLogin() + "@" + userInfo.getHostname());

        String whoisMessage
            = "Nickname: " + userInfo.getNickName() + "\n"
                + "Host name: " + userInfo.getHostname() + "\n"
                + "Login: " + userInfo.getLogin() + "\n"
                + "Server info: " + userInfo.getServerInfo() + "\n"
                + "Joined chat rooms:";

        for (String joinedChatRoom : userInfo.getJoinedChatRooms())
            whoisMessage += " " + joinedChatRoom;

        MessageIrcImpl message
            = new MessageIrcImpl(   whoisMessage,
                                    MessageIrcImpl.DEFAULT_MIME_TYPE,
                                    MessageIrcImpl.DEFAULT_MIME_ENCODING,
                                    null);

        chatRoom.fireMessageReceivedEvent(
            message,
            ircMUCOpSet.findSystemMember(),
            System.currentTimeMillis(),
            ChatRoomMessageReceivedEvent.SYSTEM_MESSAGE_RECEIVED);
    }

    /**
     * Adds a chat room to the server chat room list.
     * 
     * @param chatRoomName the name of the chat room to add
     */
    private void addServerChatRoom(String chatRoomName)
    {
        synchronized (serverChatRoomList)
        {
            if (!serverChatRoomList.contains(chatRoomName))
                serverChatRoomList.add(chatRoomName);
        }
    }

    /**
     * After waiting a certain time notifies all interested listeners that a
     * join has failed, because there's no response from the server.
     */
    private class JoinTimeoutTask extends TimerTask
    {
        private ChatRoom chatRoom;

        /**
         * Creates an instance of <tt>JoinTimeoutTask</tt>.
         * 
         * @param chatRoom the chat room for which the join has been timed out.
         */
        public JoinTimeoutTask(ChatRoom chatRoom)
        {
            this.chatRoom = chatRoom;
        }
        
        /**
         * Notifies all interested listeners that a join has failed, because 
         * there's no response from the server.
         * @see java.util.TimerTask#run()
         */
        public void run()
        {
            ((OperationSetMultiUserChatIrcImpl) parentProvider
                .getOperationSet(OperationSetMultiUserChat.class))
                    .fireLocalUserPresenceEvent(chatRoom,
                    LocalUserChatRoomPresenceChangeEvent.LOCAL_USER_JOIN_FAILED,
                    "Failed to join the  " + chatRoom.getName()
                    + " chat room, because there is no response from the server.");
        }
    }

    /**
     * Locks a chat room operation.
     */
    private void lockChatRoomOperation()
    {
        synchronized (operationLock)
        {
            try
            {
                operationLock.wait(5000);
            }
            catch (InterruptedException e)
            {
                logger.error("Chat Room operation lock thread interrupted.", e);
            }
        }
    }

    /**
     * Notifies the waiting chat room operation.
     * @param responseCode the response code of the operation to notify for
     */
    private void notifyChatRoomOperation(int responseCode)
    {
        this.operationResponseCode = responseCode;

        synchronized (operationLock)
        {
            operationLock.notify();
        }
    }

    /**
     * Kicks the participant with the given contact address from the given
     * channel.
     * 
     * @param chatRoomName the name of the chat room
     * @param contactAddress the address of the contact to kick
     * @param reason the reason of the kick
     * 
     * @throws OperationFailedException if we are not joined or we don't have
     * enough privileges to kick a participant.
     */
    public void kickParticipant(String chatRoomName,
                                String contactAddress,
                                String reason)
        throws OperationFailedException
    {
        this.kick(chatRoomName, contactAddress, reason);

        this.lockChatRoomOperation();

        if (operationResponseCode == ERR_CHANOPRIVSNEEDED)
            throw new OperationFailedException(
                "You need to have operator privileges"
                + "in order to kick a contact.",
                OperationFailedException.NOT_ENOUGH_PRIVILEGES);
        else if (operationResponseCode == ERR_NEEDMOREPARAMS)
            throw new OperationFailedException(
                "The server need more parameters in order to perform"
                + "this operation.",
                OperationFailedException.GENERAL_ERROR);
        else if (operationResponseCode == ERR_NOSUCHCHANNEL)
            throw new OperationFailedException(
                "The channel from which the contact should be kicked"
                + "was not found.",
                OperationFailedException.NOT_FOUND);
        else if (operationResponseCode == ERR_BADCHANMASK)
            throw new OperationFailedException(
                "The channel from which the contact should be kicked"
                + "was not found.",
                OperationFailedException.NOT_FOUND);
        else if (operationResponseCode == ERR_NOTONCHANNEL)
            throw new OperationFailedException(
                "You need to be joined to the chat room in order"
                + "to kick a contact from it.",
                OperationFailedException.CHAT_ROOM_NOT_JOINED);
    }

    /**
     * Changes the topic of the given channel.
     * 
     * @param channel the channel to change
     * @param topic the new topic to set
     * @throws OperationFailedException thrown if the user is not joined to the
     * channel or if he/she doesn't have enough privileges to change the
     * topic or if the topic is null.
     */
    public void setSubject(String channel, String topic)
        throws OperationFailedException
    {
        this.setTopic(channel, topic);

        this.lockChatRoomOperation();

        if (operationResponseCode == ERR_NEEDMOREPARAMS)
            new OperationFailedException(
                "More parameters should be provided to the server.",
                OperationFailedException.GENERAL_ERROR);
        else if (operationResponseCode == ERR_NOTONCHANNEL)
            new OperationFailedException(
                "You need to be joined in order to"
                + " change the subject of the chat room.",
                OperationFailedException.CHAT_ROOM_NOT_JOINED);
        else if (operationResponseCode == ERR_CHANOPRIVSNEEDED)
            new OperationFailedException(
                "You don't have enough privileges"
                + " to change the chat room subject.",
                OperationFailedException.NOT_ENOUGH_PRIVILEGES);
    }

    /**
     * Changes the user nick name on the IRC server.
     * 
     * @param nickname the new nickname
     * @throws OperationFailedException if the nickname is already used by
     * someone else
     */
    public void setUserNickname(String nickname)
        throws OperationFailedException
    {
        this.changeNick(nickname);

        this.lockChatRoomOperation();

        if (operationResponseCode == ERR_NICKNAMEINUSE)
            throw new OperationFailedException(
                "The nickname you chosed is already used by someone else.",
                OperationFailedException.IDENTIFICATION_CONFLICT);
        else if (operationResponseCode == ERR_NICKCOLLISION)
            throw new OperationFailedException(
                "The nickname you chosed is already used by someone else.",
                OperationFailedException.IDENTIFICATION_CONFLICT);
        else if (operationResponseCode == ERR_NONICKNAMEGIVEN)
            throw new OperationFailedException(
                "You need to enter a valid nickname.",
                OperationFailedException.ILLEGAL_ARGUMENT);
        else if (operationResponseCode == ERR_ERRONEUSNICKNAME)
            throw new OperationFailedException(
                "You need to enter a valid nickname.",
                OperationFailedException.ILLEGAL_ARGUMENT);
    }

    /**
     * Creates the chat room given by <tt>target</tt>.
     * @param target the name of the chat room to create
     */
    protected void createPrivateChatRoom(String target)
    {
        ChatRoomIrcImpl privateChatRoom
            = ircMUCOpSet.findPrivateChatRoom(target);

        if(privateChatRoom == null)
            return;

        ChatRoomMember sourceMember = privateChatRoom.getChatRoomMember(
            parentProvider.getAccountID().getService());

        if (sourceMember == null)
            sourceMember
                = new ChatRoomMemberIrcImpl(
                        parentProvider,
                        privateChatRoom,
                        parentProvider.getAccountID().getService(),
                        ChatRoomMemberRole.GUEST);

        MessageIrcImpl queryMessage
            = new MessageIrcImpl(   "Private conversation initiated.",
                                    MessageIrcImpl.DEFAULT_MIME_TYPE,
                                    MessageIrcImpl.DEFAULT_MIME_ENCODING,
                                    null);

        privateChatRoom.fireMessageReceivedEvent(
            queryMessage,
            sourceMember,
            System.currentTimeMillis(),
            ChatRoomMessageReceivedEvent.SYSTEM_MESSAGE_RECEIVED);
    }
}
