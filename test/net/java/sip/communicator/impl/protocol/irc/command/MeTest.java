/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.irc.command;

import junit.framework.*;
import net.java.sip.communicator.impl.protocol.irc.*;

import org.easymock.*;

import com.ircclouds.irc.api.*;

public class MeTest extends TestCase
{

    public void testConstruction()
    {
        new Me();
    }

    public void testGoodInit()
    {
        IrcConnection connection = EasyMock.createMock(IrcConnection.class);
        EasyMock.replay(connection);

        Me me = new Me();
        me.init(null, connection);
    }

    public void testBadInit()
    {
        ProtocolProviderServiceIrcImpl provider = EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        EasyMock.replay(provider);

        Me me = new Me();
        try
        {
            me.init(provider, null);
            Assert.fail();
        }
        catch (IllegalArgumentException e)
        {
        }
    }

    public void testNoMessage()
    {
        ProtocolProviderServiceIrcImpl provider = EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        IrcConnection connection = EasyMock.createMock(IrcConnection.class);
        EasyMock.replay(provider, connection);

        Me me = new Me();
        me.init(provider, connection);
        me.execute("#test", "/me");
    }

    public void testZeroLengthMessage()
    {
        ProtocolProviderServiceIrcImpl provider = EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        IrcConnection connection = EasyMock.createMock(IrcConnection.class);
        EasyMock.replay(provider, connection);

        Me me = new Me();
        me.init(provider, connection);
        try
        {
            me.execute("#test", "/me ");
            Assert.fail();
        }
        catch (IllegalArgumentException e)
        {
        }
    }

    public void testSendMessage()
    {
        ProtocolProviderServiceIrcImpl provider = EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        IrcConnection connection = EasyMock.createMock(IrcConnection.class);
        IRCApi client = EasyMock.createMock(IRCApi.class);
        EasyMock.expect(connection.getClient()).andReturn(client);
        client.act(EasyMock.eq("#test"), EasyMock.eq("says hello world!"));
        EasyMock.expectLastCall();
        EasyMock.replay(provider, connection, client);

        Me me = new Me();
        me.init(provider, connection);
        me.execute("#test", "/me says hello world!");
    }
}
