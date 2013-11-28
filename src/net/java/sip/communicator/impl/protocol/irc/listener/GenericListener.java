package net.java.sip.communicator.impl.protocol.irc.listener;

import com.ircclouds.irc.api.domain.messages.ServerNotice;
import com.ircclouds.irc.api.domain.messages.ServerNumericMessage;
import com.ircclouds.irc.api.domain.messages.interfaces.IMessage;
import com.ircclouds.irc.api.listeners.IMessageListener;

public class GenericListener
    implements IMessageListener
{
    // TODO Maybe implement this as a IRC server listener and connect a system
    // chatroom to each listener in order to inform the user of server
    // (chatroom-independent) messages, notices, etc.

    @Override
    public void onMessage(IMessage msg)
    {
        if (msg instanceof ServerNotice)
        {
            System.out.println("NOTICE: " + ((ServerNotice) msg).getText());
        }
        else if (msg instanceof ServerNumericMessage)
        {
            System.out.println("NUM MSG: "
                + ((ServerNumericMessage) msg).getNumericCode() + ": "
                + ((ServerNumericMessage) msg).getText());
        }
    }
}
