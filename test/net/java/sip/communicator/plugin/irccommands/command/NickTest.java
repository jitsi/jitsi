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

/**
 * @author Danny van Heumen
 */
public class NickTest
        extends TestCase
{

    public NickTest(String testName)
    {
        super(testName);
    }

    public void testNullProviderInit()
    {
        IrcConnection connection = EasyMock.createMock(IrcConnection.class);
        EasyMock.replay(connection);

        new Nick(null, connection);
    }

    public void testNullConnectionInit()
    {
        ProtocolProviderServiceIrcImpl provider =
            EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        EasyMock.replay(provider);

        try
        {
            new Nick(provider, null);
            Assert.fail("Should not reach this as we expected an IAE for null"
                + " connection.");
        }
        catch (IllegalArgumentException e)
        {
            // IAE was expected.
        }
    }

    public void testEmptyNickCommand()
    {
        ProtocolProviderServiceIrcImpl provider =
            EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        IrcConnection connection = EasyMock.createMock(IrcConnection.class);
        EasyMock.replay(provider, connection);

        Nick nick = new Nick(provider, connection);
        try
        {
            nick.execute("#test", "/nick");
            Assert.fail();
        }
        catch (IllegalArgumentException e)
        {
        }
    }

    public void testEmptyNickCommandWithSpace()
    {
        ProtocolProviderServiceIrcImpl provider =
            EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        IrcConnection connection = EasyMock.createMock(IrcConnection.class);
        EasyMock.replay(provider, connection);

        Nick nick = new Nick(provider, connection);
        try
        {
            nick.execute("#test", "/nick ");
            Assert.fail();
        }
        catch (IllegalArgumentException e)
        {
        }
    }

    public void testEmptyNickCommandWithDoubleSpace()
    {
        ProtocolProviderServiceIrcImpl provider =
            EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        IrcConnection connection = EasyMock.createMock(IrcConnection.class);
        EasyMock.replay(provider, connection);

        Nick nick = new Nick(provider, connection);
        nick.execute("#test", "/nick  ");
    }

    public void testNickCommandWithNickAndSpace()
    {
        ProtocolProviderServiceIrcImpl provider =
            EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        IrcConnection connection = EasyMock.createMock(IrcConnection.class);
        IdentityManager idmgr = EasyMock.createMock(IdentityManager.class);

        EasyMock.expect(connection.getIdentityManager()).andReturn(idmgr);
        idmgr.setNick(EasyMock.eq("myNewN1ck"));
        EasyMock.expectLastCall();
        EasyMock.replay(provider, connection, idmgr);

        Nick nick = new Nick(provider, connection);
        nick.execute("#test", "/nick myNewN1ck ");
    }
}
