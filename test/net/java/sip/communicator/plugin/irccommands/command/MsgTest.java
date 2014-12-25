/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.irccommands.command;

import junit.framework.*;
import net.java.sip.communicator.impl.protocol.irc.*;

import org.easymock.*;

import com.ircclouds.irc.api.*;

/**
 * @author Danny van Heumen
 */
public class MsgTest
    extends TestCase
{
    public void testGoodInit()
    {
        IrcConnection connection = EasyMock.createMock(IrcConnection.class);
        EasyMock.replay(connection);

        new Msg(null, connection);
    }

    public void testBadInit()
    {
        ProtocolProviderServiceIrcImpl provider = EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        EasyMock.replay(provider);

        try
        {
            new Msg(provider, null);
            Assert.fail("Should not reach this, expected IAE.");
        }
        catch (IllegalArgumentException e)
        {
        }
    }

    public void testMessageNoNickNoMsg()
    {
        ProtocolProviderServiceIrcImpl provider = EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        IrcConnection connection = EasyMock.createMock(IrcConnection.class);
        EasyMock.replay(provider, connection);

        Msg msg = new Msg(provider, connection);
        try
        {
            msg.execute("#test", "/msg");
            Assert
                .fail("Should have thrown IAE for missing target and message.");
        }
        catch (IllegalArgumentException e)
        {
        }
    }

    public void testMessageZeroLengthMsg()
    {
        ProtocolProviderServiceIrcImpl provider = EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        IrcConnection connection = EasyMock.createMock(IrcConnection.class);
        EasyMock.replay(provider, connection);

        Msg msg = new Msg(provider, connection);
        try
        {
            msg.execute("#test", "/msg ");
            Assert.fail();
        }
        catch (IllegalArgumentException e)
        {
        }
    }

    public void testMessageZeroLengthNickMsg()
    {
        ProtocolProviderServiceIrcImpl provider = EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        IrcConnection connection = EasyMock.createMock(IrcConnection.class);
        EasyMock.replay(provider, connection);

        Msg msg = new Msg(provider, connection);
        try
        {
            msg.execute("#test", "/msg  ");
            Assert.fail();
        }
        catch (IllegalArgumentException e)
        {
        }
    }

    public void testMessageNickZeroLengthMsg()
    {
        ProtocolProviderServiceIrcImpl provider = EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        IrcConnection connection = EasyMock.createMock(IrcConnection.class);
        EasyMock.replay(provider, connection);

        Msg msg = new Msg(provider, connection);
        try
        {
            msg.execute("#test", "/msg target ");
            Assert.fail();
        }
        catch (IllegalArgumentException e)
        {
        }
    }

    public void testPrivateMessage()
    {
        ProtocolProviderServiceIrcImpl provider = EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        IrcConnection connection = EasyMock.createMock(IrcConnection.class);
        IRCApi client = EasyMock.createMock(IRCApi.class);
        EasyMock.expect(connection.getClient()).andReturn(client);
        client.message(EasyMock.eq("target"), EasyMock.eq("This is my target message."));
        EasyMock.expectLastCall();
        EasyMock.replay(provider, connection);

        Msg msg = new Msg(provider, connection);
        msg.execute("#test", "/msg target This is my target message.");
    }
}
