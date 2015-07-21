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

public class ModeTest
    extends TestCase
{

    public void testGoodInit()
    {
        IrcConnection connection = EasyMock.createMock(IrcConnection.class);
        EasyMock.replay(connection);

        Mode mode = new Mode(null, connection);
    }

    public void testBadInit()
    {
        ProtocolProviderServiceIrcImpl provider =
            EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        EasyMock.replay(provider);

        try
        {
            Mode mode = new Mode(provider, null);
            Assert.fail();
        }
        catch (IllegalArgumentException e)
        {
        }
    }

    public void testEmptyCommand()
    {
        IrcConnection connection = EasyMock.createMock(IrcConnection.class);
        EasyMock.replay(connection);

        Mode mode = new Mode(null, connection);
        try
        {
            mode.execute("#test", "/mode");
            Assert.fail();
        }
        catch (IllegalArgumentException e)
        {
        }
    }

    public void testEmptyModeLine()
    {
        IrcConnection connection = EasyMock.createMock(IrcConnection.class);
        EasyMock.replay(connection);

        Mode mode = new Mode(null, connection);
        try
        {
            mode.execute("#test", "/mode ");
        }
        catch (IllegalArgumentException e)
        {
        }
    }

    public void testSpacesModeLine()
    {
        IrcConnection connection = EasyMock.createMock(IrcConnection.class);
        EasyMock.replay(connection);

        Mode mode = new Mode(null, connection);
        try
        {
            mode.execute("#test", "/mode   ");
        }
        catch (IllegalArgumentException e)
        {
        }
    }

    public void testCorrectModeCommand()
    {
        IrcConnection connection = EasyMock.createMock(IrcConnection.class);
        IRCApi client = EasyMock.createMock(IRCApi.class);
        EasyMock.expect(connection.getClient()).andReturn(client);
        client.changeMode(EasyMock.eq("#test +o ThaDud3"));
        EasyMock.expectLastCall();
        EasyMock.replay(connection, client);

        Mode mode = new Mode(null, connection);
        mode.execute("#test", "/mode +o ThaDud3");
    }
}
