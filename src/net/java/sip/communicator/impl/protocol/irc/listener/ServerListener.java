package net.java.sip.communicator.impl.protocol.irc.listener;

import java.util.*;
import java.util.Map.Entry;

import net.java.sip.communicator.impl.protocol.irc.*;
import net.java.sip.communicator.service.protocol.event.*;

import com.ircclouds.irc.api.domain.messages.*;
import com.ircclouds.irc.api.listeners.*;

/**
 * A listener for server-level messages (any messages that are related to the
 * server, the connection or that are not related to any chatroom in
 * particular).
 */
public class ServerListener
    extends VariousMessageListenerAdapter
{
    /**
     * Reference to the list of joined channels from the IRC connection.
     */
    private final List<ChatRoomIrcImpl> channels;

    /**
     * ServerListener
     * 
     * @param joinedChannels Reference to the list of joined channels.
     */
    public ServerListener(List<ChatRoomIrcImpl> joinedChannels)
    {
        if (joinedChannels == null)
            throw new IllegalArgumentException(
                "joined channels reference cannot be null");
        this.channels = joinedChannels;
    }

    /**
     * Print out server notices for debugging purposes and for simply keeping
     * track of the connections.
     */
    @Override
    public void onServerNotice(ServerNotice msg)
    {
        System.out.println("NOTICE: " + ((ServerNotice) msg).getText());
    }

    /**
     * Print out server numeric messages for debugging purposes and for simply
     * keeping track of the connection.
     */
    @Override
    public void onServerNumericMessage(ServerNumericMessage msg)
    {
        System.out.println("NUM MSG: "
            + ((ServerNumericMessage) msg).getNumericCode() + ": "
            + ((ServerNumericMessage) msg).getText());
    }

    /**
     * Act on nick change messages.
     * 
     * Nick change messages span multiple chat rooms. Specifically every chat
     * room where this particular user is joined, needs to get an update for the
     * nick change.
     */
    @Override
    public void onNickChange(NickMessage msg)
    {
        if (msg == null)
            return;

        // Find all affected channels.
        HashMap<ChatRoomIrcImpl, ChatRoomMemberIrcImpl> affected =
            new HashMap<ChatRoomIrcImpl, ChatRoomMemberIrcImpl>();
        String oldNick = msg.getSource().getNick();
        synchronized (this.channels)
        {
            for (ChatRoomIrcImpl channel : this.channels)
            {
                ChatRoomMemberIrcImpl member =
                    (ChatRoomMemberIrcImpl) channel.getChatRoomMember(oldNick);
                if (member != null)
                {
                    affected.put(channel, member);
                }
            }
        }

        // Process nick change for all of those channels and fire corresponding
        // property change event.
        String newNick = msg.getNewNick();
        for (Entry<ChatRoomIrcImpl, ChatRoomMemberIrcImpl> record : affected
            .entrySet())
        {
            ChatRoomIrcImpl channel = record.getKey();
            ChatRoomMemberIrcImpl member = record.getValue();
            member.setName(newNick);
            channel.updateChatRoomMemberName(oldNick);
            ChatRoomMemberPropertyChangeEvent evt =
                new ChatRoomMemberPropertyChangeEvent(member, channel,
                    ChatRoomMemberPropertyChangeEvent.MEMBER_NICKNAME, oldNick,
                    newNick);
            channel.fireMemberPropertyChangeEvent(evt);
        }
    }
}
