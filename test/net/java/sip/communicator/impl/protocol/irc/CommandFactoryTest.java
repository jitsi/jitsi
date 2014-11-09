/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.irc;

import junit.framework.*;
import net.java.sip.communicator.impl.protocol.irc.exception.*;

import org.easymock.*;

public class CommandFactoryTest
    extends TestCase
{
    public void testCommandsAvailable()
    {
        Assert.assertNotNull(CommandFactory.getCommands());
    }

    public void testRegisterNullCommand()
    {
        try
        {
            CommandFactory.registerCommand(null, new Command()
            {
                @Override
                public void init(ProtocolProviderServiceIrcImpl provider,
                    IrcConnection connection)
                {
                }

                @Override
                public void execute(String source, String line)
                {
                }
            }.getClass());
            Assert.fail();
        }
        catch (IllegalArgumentException e)
        {
        }
    }

    public void testRegisterNullType()
    {
        try
        {
            CommandFactory.registerCommand("test", null);
            Assert.fail();
        }
        catch (IllegalArgumentException e)
        {
        }
    }

    public void testRegisterCorrectCommand()
    {
        CommandFactory.registerCommand("test", Test.class);
        Assert.assertEquals(1,  CommandFactory.getCommands().size());
        Assert.assertEquals(Test.class, CommandFactory.getCommands()
            .get("test"));
        CommandFactory.unregisterCommand(Test.class, "test");
        Assert.assertEquals(0, CommandFactory.getCommands().size());
    }

    public void testRegisterMultipleCommandsForType()
    {
        CommandFactory.registerCommand("test", Test.class);
        CommandFactory.registerCommand("bla", Test.class);
        Assert.assertEquals(Test.class, CommandFactory.getCommands()
            .get("test"));
        Assert.assertEquals(Test.class, CommandFactory.getCommands()
            .get("bla"));
        Assert.assertEquals(2, CommandFactory.getCommands().size());
        CommandFactory.unregisterCommand(Test.class, null);
        Assert.assertEquals(0,  CommandFactory.getCommands().size());
    }

    public void testUnregisterMultipleAmongOtherTypes()
    {
        Command anotherType = new Command() {

            @Override
            public void init(ProtocolProviderServiceIrcImpl provider,
                IrcConnection connection)
            {
            }

            @Override
            public void execute(String source, String line)
            {
            }};
        CommandFactory.registerCommand("test", Test.class);
        CommandFactory.registerCommand("foo", anotherType.getClass());
        CommandFactory.registerCommand("bla", Test.class);
        Assert.assertEquals(Test.class, CommandFactory.getCommands()
            .get("test"));
        Assert.assertEquals(Test.class, CommandFactory.getCommands()
            .get("bla"));
        Assert.assertNotNull(CommandFactory.getCommands().get("foo"));
        Assert.assertEquals(3, CommandFactory.getCommands().size());
        CommandFactory.unregisterCommand(Test.class, null);
        Assert.assertEquals(1, CommandFactory.getCommands().size());
        Assert.assertNotSame(Test.class, CommandFactory.getCommands().get("foo"));
        CommandFactory.unregisterCommand(anotherType.getClass(), null);
    }

    public void testUnregisterOneAmongMultipleSameType()
    {
        CommandFactory.registerCommand("test", Test.class);
        CommandFactory.registerCommand("bla", Test.class);
        Assert.assertEquals(Test.class, CommandFactory.getCommands()
            .get("test"));
        Assert.assertEquals(Test.class, CommandFactory.getCommands()
            .get("bla"));
        Assert.assertEquals(2, CommandFactory.getCommands().size());
        CommandFactory.unregisterCommand(Test.class, "test");
        Assert.assertEquals(1, CommandFactory.getCommands().size());
        Assert.assertEquals(Test.class, CommandFactory.getCommands().get("bla"));
        CommandFactory.unregisterCommand(Test.class, null);
    }

    public static class Test implements Command
    {

        @Override
        public void init(ProtocolProviderServiceIrcImpl provider,
            IrcConnection connection)
        {
        }

        @Override
        public void execute(String source, String line)
        {
        }
    }

    public void testConstructionNullProvider()
    {
        IrcConnection connection = EasyMock.createMock(IrcConnection.class);
        EasyMock.replay(connection);

        try
        {
            new CommandFactory(null, connection);
            Assert.fail();
        }
        catch (IllegalArgumentException e)
        {
        }
    }

    public void testConstructionNullConnection()
    {
        ProtocolProviderServiceIrcImpl provider = EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        EasyMock.replay(provider);


        try
        {
            new CommandFactory(provider, null);
            Assert.fail();
        }
        catch (IllegalArgumentException e)
        {
        }
    }

    public void testConstruction()
    {
        ProtocolProviderServiceIrcImpl provider = EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        IrcConnection connection = EasyMock.createMock(IrcConnection.class);
        EasyMock.replay(provider, connection);

        new CommandFactory(provider, connection);
    }

    public void testNonExistingCommand()
    {
        ProtocolProviderServiceIrcImpl provider = EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        IrcConnection connection = EasyMock.createMock(IrcConnection.class);
        EasyMock.replay(provider, connection);

        try
        {
            CommandFactory factory = new CommandFactory(provider, connection);
            factory.createCommand("test");
            Assert.fail();
        }
        catch (UnsupportedCommandException e)
        {
        }
    }

    public void testCreateNullCommandName() throws UnsupportedCommandException
    {
        ProtocolProviderServiceIrcImpl provider = EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        IrcConnection connection = EasyMock.createMock(IrcConnection.class);
        EasyMock.replay(provider, connection);

        CommandFactory.registerCommand("test", Test.class);
        try
        {
            CommandFactory factory = new CommandFactory(provider, connection);
            factory.createCommand(null);
            Assert.fail();
        }
        catch (IllegalArgumentException e)
        {
        }
        CommandFactory.unregisterCommand(Test.class, null);
    }

    public void testCreateEmptyCommandName() throws UnsupportedCommandException
    {
        ProtocolProviderServiceIrcImpl provider = EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        IrcConnection connection = EasyMock.createMock(IrcConnection.class);
        EasyMock.replay(provider, connection);

        CommandFactory.registerCommand("test", Unreachable.class);
        try
        {
            CommandFactory factory = new CommandFactory(provider, connection);
            factory.createCommand("");
            Assert.fail();
        }
        catch (IllegalArgumentException e)
        {
        }
        finally
        {
            CommandFactory.unregisterCommand(Test.class, null);
        }
    }

    public void testExistingCommand() throws UnsupportedCommandException
    {
        ProtocolProviderServiceIrcImpl provider = EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        IrcConnection connection = EasyMock.createMock(IrcConnection.class);
        EasyMock.replay(provider, connection);

        CommandFactory.registerCommand("test", Test.class);
        try
        {
            CommandFactory factory = new CommandFactory(provider, connection);
            Command cmd = factory.createCommand("test");
            Assert.assertNotNull(cmd);
            Assert.assertTrue(cmd instanceof Test);
        }
        finally
        {
            CommandFactory.unregisterCommand(Test.class, null);
        }
    }

    public void testUnreachableCommand() throws UnsupportedCommandException
    {
        ProtocolProviderServiceIrcImpl provider = EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        IrcConnection connection = EasyMock.createMock(IrcConnection.class);
        EasyMock.replay(provider, connection);

        CommandFactory.registerCommand("test", Unreachable.class);
        try
        {
            CommandFactory factory = new CommandFactory(provider, connection);
            factory.createCommand("test");
            Assert.fail();
        }
        catch (IllegalStateException e)
        {
        }
        finally
        {
            CommandFactory.unregisterCommand(Unreachable.class, null);
        }
    }

    public void testBadCommand() throws UnsupportedCommandException
    {
        ProtocolProviderServiceIrcImpl provider = EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        IrcConnection connection = EasyMock.createMock(IrcConnection.class);
        EasyMock.replay(provider, connection);

        CommandFactory.registerCommand("test", BadImplementation.class);
        try
        {
            CommandFactory factory = new CommandFactory(provider, connection);
            factory.createCommand("test");
            Assert.fail();
        }
        catch (IllegalStateException e)
        {
        }
        finally
        {
            CommandFactory.unregisterCommand(BadImplementation.class, null);
        }
    }

    private static class Unreachable implements Command
    {

        @Override
        public void init(ProtocolProviderServiceIrcImpl provider,
            IrcConnection connection)
        {
        }

        @Override
        public void execute(String source, String line)
        {
        }
    }

    public abstract static class BadImplementation implements Command
    {
    }
}
