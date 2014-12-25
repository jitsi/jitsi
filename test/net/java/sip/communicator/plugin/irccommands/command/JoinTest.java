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

public class JoinTest
    extends TestCase
{

    public void testGoodInit()
    {
        IrcConnection connection = EasyMock.createMock(IrcConnection.class);
        EasyMock.replay(connection);

        new Join(null, connection);
    }

    public void testBadInit()
    {
        ProtocolProviderServiceIrcImpl provider = EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        EasyMock.replay(provider);

        try
        {
            new Join(provider, null);
            Assert.fail();
        }
        catch (IllegalArgumentException e)
        {
        }
    }

    public void testEmptyJoin()
    {
        IrcConnection connection = EasyMock.createMock(IrcConnection.class);
        EasyMock.replay(connection);

        Join join = new Join(null, connection);
        try
        {
            join.execute("#test", "/join");
            Assert.fail();
        }
        catch (IllegalArgumentException e)
        {
        }
    }

    public void testJoinEmptyChannelNoPassword()
    {
        IrcConnection connection = EasyMock.createMock(IrcConnection.class);
        EasyMock.replay(connection);

        Join join = new Join(null, connection);
        try
        {
            join.execute("#test", "/join ");
            Assert.fail();
        }
        catch (IllegalArgumentException e)
        {
        }
    }

    public void testJoinWithChannelNoPassword()
    {
        IrcConnection connection = EasyMock.createMock(IrcConnection.class);
        IRCApi client = EasyMock.createMock(IRCApi.class);
        EasyMock.expect(connection.getClient()).andReturn(client);
        client.joinChannel(EasyMock.eq("#test"), EasyMock.eq(""));
        EasyMock.expectLastCall();
        EasyMock.replay(connection, client);

        Join join = new Join(null, connection);
        join.execute("#test", "/join #test");
    }

    public void testJoinWithChannelWithPassword()
    {
        IrcConnection connection = EasyMock.createMock(IrcConnection.class);
        IRCApi client = EasyMock.createMock(IRCApi.class);
        EasyMock.expect(connection.getClient()).andReturn(client);
        client.joinChannel(EasyMock.eq("#test"), EasyMock.eq("top-secret"));
        EasyMock.expectLastCall();
        EasyMock.replay(connection, client);

        Join join = new Join(null, connection);
        join.execute("#test", "/join #test top-secret");
    }
}
