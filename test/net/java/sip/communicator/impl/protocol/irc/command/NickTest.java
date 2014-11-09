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

    public void testConstruction()
    {
        new Nick();
    }

    public void testNullProviderInit()
    {
        IrcConnection connection = EasyMock.createMock(IrcConnection.class);
        EasyMock.replay(connection);

        Nick nick = new Nick();
        nick.init(null, connection);
    }

    public void testNullConnectionInit()
    {
        ProtocolProviderServiceIrcImpl provider =
            EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        EasyMock.replay(provider);

        Nick nick = new Nick();
        try
        {
            nick.init(provider, null);
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

        Nick nick = new Nick();
        nick.init(provider, connection);
        nick.execute("#test", "/nick");
    }

    public void testEmptyNickCommandWithSpace()
    {
        ProtocolProviderServiceIrcImpl provider =
            EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        IrcConnection connection = EasyMock.createMock(IrcConnection.class);
        EasyMock.replay(provider, connection);

        Nick nick = new Nick();
        nick.init(provider, connection);
        nick.execute("#test", "/nick ");
    }

    public void testEmptyNickCommandWithDoubleSpace()
    {
        ProtocolProviderServiceIrcImpl provider =
            EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        IrcConnection connection = EasyMock.createMock(IrcConnection.class);
        EasyMock.replay(provider, connection);

        Nick nick = new Nick();
        nick.init(provider, connection);
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

        Nick nick = new Nick();
        nick.init(provider, connection);
        nick.execute("#test", "/nick myNewN1ck ");
    }
}
