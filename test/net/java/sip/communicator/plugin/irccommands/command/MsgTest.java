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
