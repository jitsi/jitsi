package net.java.sip.communicator.impl.protocol.irc.listener;

import java.util.*;
import java.util.Map.Entry;

import net.java.sip.communicator.impl.protocol.irc.*;
import net.java.sip.communicator.service.protocol.event.*;

import com.ircclouds.irc.api.domain.messages.*;
import com.ircclouds.irc.api.listeners.*;

public class ServerListener
    extends VariousMessageListenerAdapter
{
    private final List<ChatRoomIrcImpl> channels;
    
    public ServerListener(List<ChatRoomIrcImpl> joinedChannels)
    {
        this.channels = joinedChannels;
    }
    
    // TODO Maybe implement this as a IRC server listener and connect a system
    // chatroom to each listener in order to inform the user of server
    // (chatroom-independent) messages, notices, etc.
    
    @Override
    public void onServerNotice(ServerNotice msg)
    {
        System.out.println("NOTICE: " + ((ServerNotice) msg).getText());
    }
    
    @Override
    public void onServerNumericMessage(ServerNumericMessage msg)
    {
        System.out.println("NUM MSG: "
            + ((ServerNumericMessage) msg).getNumericCode() + ": "
            + ((ServerNumericMessage) msg).getText());
    }
    
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
            channel.updateChatRoomMemberName(oldNick, newNick);
            ChatRoomMemberPropertyChangeEvent evt =
                new ChatRoomMemberPropertyChangeEvent(member, channel,
                    ChatRoomMemberPropertyChangeEvent.MEMBER_NICKNAME, oldNick,
                    newNick);
            channel.fireMemberPropertyChangeEvent(evt);
        }
    }
}
