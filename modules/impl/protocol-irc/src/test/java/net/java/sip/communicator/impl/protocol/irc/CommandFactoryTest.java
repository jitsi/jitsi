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
package net.java.sip.communicator.impl.protocol.irc;

import static org.junit.Assert.*;

import net.java.sip.communicator.impl.protocol.irc.exception.*;
import org.easymock.*;
import org.junit.*;

public class CommandFactoryTest
{
    @Test
    public void testCommandsAvailable()
    {
        assertNotNull(CommandFactory.getCommands());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRegisterNullCommand()
    {
        CommandFactory.registerCommand(null, TestCmd.class);
        fail();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRegisterNullType()
    {
        CommandFactory.registerCommand("test", null);
        fail();
    }

    @Test
    public void testRegisterCorrectCommand()
    {
        CommandFactory.registerCommand("test", TestCmd.class);
        assertEquals(1, CommandFactory.getCommands().size());
        assertEquals(TestCmd.class, CommandFactory.getCommands()
            .get("test"));
        CommandFactory.unregisterCommand(TestCmd.class, "test");
        assertEquals(0, CommandFactory.getCommands().size());
    }

    @Test
    public void testRegisterMultipleCommandsForType()
    {
        CommandFactory.registerCommand("test", TestCmd.class);
        CommandFactory.registerCommand("bla", TestCmd.class);
        assertEquals(TestCmd.class, CommandFactory.getCommands()
            .get("test"));
        assertEquals(TestCmd.class, CommandFactory.getCommands()
            .get("bla"));
        assertEquals(2, CommandFactory.getCommands().size());
        CommandFactory.unregisterCommand(TestCmd.class, null);
        assertEquals(0, CommandFactory.getCommands().size());
    }

    @Test
    public void testUnregisterMultipleAmongOtherTypes()
    {
        Command anotherType = new Command()
        {

            @Override
            public void execute(String source, String line)
            {
            }

            @Override
            public String help()
            {
                return null;
            }
        };
        CommandFactory.registerCommand("test", TestCmd.class);
        CommandFactory.registerCommand("foo", anotherType.getClass());
        CommandFactory.registerCommand("bla", TestCmd.class);
        assertEquals(TestCmd.class, CommandFactory.getCommands()
            .get("test"));
        assertEquals(TestCmd.class, CommandFactory.getCommands()
            .get("bla"));
        assertNotNull(CommandFactory.getCommands().get("foo"));
        assertEquals(3, CommandFactory.getCommands().size());
        CommandFactory.unregisterCommand(TestCmd.class, null);
        assertEquals(1, CommandFactory.getCommands().size());
        assertNotSame(TestCmd.class, CommandFactory.getCommands().get("foo"));
        CommandFactory.unregisterCommand(anotherType.getClass(), null);
    }

    @Test
    public void testUnregisterOneAmongMultipleSameType()
    {
        CommandFactory.registerCommand("test", TestCmd.class);
        CommandFactory.registerCommand("bla", TestCmd.class);
        assertEquals(TestCmd.class, CommandFactory.getCommands()
            .get("test"));
        assertEquals(TestCmd.class, CommandFactory.getCommands()
            .get("bla"));
        assertEquals(2, CommandFactory.getCommands().size());
        CommandFactory.unregisterCommand(TestCmd.class, "test");
        assertEquals(1, CommandFactory.getCommands().size());
        assertEquals(TestCmd.class, CommandFactory.getCommands().get("bla"));
        CommandFactory.unregisterCommand(TestCmd.class, null);
    }

    public static class TestCmd
        implements Command
    {
        public TestCmd(ProtocolProviderServiceIrcImpl provider,
            IrcConnection connection)
        {
        }

        @Override
        public void execute(String source, String line)
        {
        }

        @Override
        public String help()
        {
            return null;
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructionNullProvider()
    {
        IrcConnection connection = EasyMock.createMock(IrcConnection.class);
        EasyMock.replay(connection);

        new CommandFactory(null, connection);
        fail();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructionNullConnection()
    {
        ProtocolProviderServiceIrcImpl provider =
            EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        EasyMock.replay(provider);

        new CommandFactory(provider, null);
        fail();
    }

    @Test
    public void testConstruction()
    {
        ProtocolProviderServiceIrcImpl provider =
            EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        IrcConnection connection = EasyMock.createMock(IrcConnection.class);
        EasyMock.replay(provider, connection);

        new CommandFactory(provider, connection);
    }

    @Test(expected = UnsupportedCommandException.class)
    public void testNonExistingCommand()
        throws UnsupportedCommandException, BadCommandException
    {
        ProtocolProviderServiceIrcImpl provider =
            EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        IrcConnection connection = EasyMock.createMock(IrcConnection.class);
        EasyMock.replay(provider, connection);

        CommandFactory factory = new CommandFactory(provider, connection);
        factory.createCommand("test");
        fail();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateNullCommandName()
        throws UnsupportedCommandException, BadCommandException
    {
        ProtocolProviderServiceIrcImpl provider =
            EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        IrcConnection connection = EasyMock.createMock(IrcConnection.class);
        EasyMock.replay(provider, connection);

        CommandFactory.registerCommand("test", TestCmd.class);
        try
        {
            CommandFactory factory = new CommandFactory(provider, connection);
            factory.createCommand(null);
            fail();
        }
        finally
        {
            CommandFactory.unregisterCommand(TestCmd.class, null);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateEmptyCommandName()
        throws UnsupportedCommandException, BadCommandException
    {
        ProtocolProviderServiceIrcImpl provider =
            EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        IrcConnection connection = EasyMock.createMock(IrcConnection.class);
        EasyMock.replay(provider, connection);

        CommandFactory.registerCommand("test", Unreachable.class);
        try
        {
            CommandFactory factory = new CommandFactory(provider, connection);
            factory.createCommand("");
            fail();
        }
        finally
        {
            CommandFactory.unregisterCommand(TestCmd.class, null);
        }
    }

    @Test
    public void testExistingCommand()
        throws UnsupportedCommandException, BadCommandException
    {
        ProtocolProviderServiceIrcImpl provider =
            EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        IrcConnection connection = EasyMock.createMock(IrcConnection.class);
        EasyMock.replay(provider, connection);

        CommandFactory.registerCommand("test", TestCmd.class);
        try
        {
            CommandFactory factory = new CommandFactory(provider, connection);
            Command cmd = factory.createCommand("test");
            assertNotNull(cmd);
            assertTrue(cmd instanceof TestCmd);
        }
        finally
        {
            CommandFactory.unregisterCommand(TestCmd.class, null);
        }
    }

    @Test(expected = BadCommandException.class)
    public void testUnreachableCommand()
        throws UnsupportedCommandException, BadCommandException
    {
        ProtocolProviderServiceIrcImpl provider =
            EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        IrcConnection connection = EasyMock.createMock(IrcConnection.class);
        EasyMock.replay(provider, connection);

        CommandFactory.registerCommand("test", Unreachable.class);
        try
        {
            CommandFactory factory = new CommandFactory(provider, connection);
            factory.createCommand("test");
            fail();
        }
        finally
        {
            CommandFactory.unregisterCommand(Unreachable.class, null);
        }
    }

    @Test(expected = BadCommandException.class)
    public void testBadCommand()
        throws UnsupportedCommandException, BadCommandException
    {
        ProtocolProviderServiceIrcImpl provider =
            EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        IrcConnection connection = EasyMock.createMock(IrcConnection.class);
        EasyMock.replay(provider, connection);

        CommandFactory.registerCommand("test", BadImplementation.class);
        try
        {
            CommandFactory factory = new CommandFactory(provider, connection);
            factory.createCommand("test");
            fail();
        }
        finally
        {
            CommandFactory.unregisterCommand(BadImplementation.class, null);
        }
    }

    private static final class Unreachable
        implements Command
    {
        private Unreachable(ProtocolProviderServiceIrcImpl provider,
            IrcConnection connection)
        {
        }

        @Override
        public void execute(String source, String line)
        {
        }

        @Override
        public String help()
        {
            return null;
        }
    }

    public abstract static class BadImplementation
        implements Command
    {
        public BadImplementation(ProtocolProviderServiceIrcImpl provider,
            IrcConnection connection)
        {
        }
    }
}
