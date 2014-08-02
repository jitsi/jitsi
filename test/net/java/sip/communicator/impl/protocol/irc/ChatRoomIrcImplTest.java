package net.java.sip.communicator.impl.protocol.irc;

import java.util.*;

import junit.framework.*;

import org.easymock.*;

import com.google.common.collect.*;

public class ChatRoomIrcImplTest
    extends TestCase
{
    private ProtocolProviderServiceIrcImpl providerMock;
    private IrcStack stackMock;

    //@before
    public void setUp() throws Exception
    {
        super.setUp();
        this.providerMock =
            EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        this.stackMock = EasyMock.createMock(IrcStack.class);
        EasyMock.expect(this.providerMock.getIrcStack()).andReturn(stackMock);
        EasyMock.expect(this.stackMock.getChannelTypes()).andReturn(
            Collections.unmodifiableSet(Sets.newHashSet('#', '$')));
    }

    //@Test
    public void testConstruction()
    {
        EasyMock.replay(this.providerMock, this.stackMock);
        new ChatRoomIrcImpl("#test", this.providerMock);
    }

    //@Test(expected = IllegalArgumentException.class)
    public void testConstructionNullIdentifier()
    {
        EasyMock.replay(this.providerMock, this.stackMock);
        new ChatRoomIrcImpl(null, this.providerMock);
    }

    //@Test(expected = IllegalArgumentException.class)
    public void testConstructionNullProvider()
    {
        EasyMock.replay(this.providerMock, this.stackMock);
        new ChatRoomIrcImpl("#test", null);
    }

    //@Test(expected = IllegalArgumentException.class)
    public void testIllegalNameBadPrefix()
    {
        EasyMock.replay(this.providerMock, this.stackMock);
        new ChatRoomIrcImpl("!test", this.providerMock);
    }

    //@Test(expected = IllegalArgumentException.class)
    public void testIllegalNameSpace()
    {
        EasyMock.replay(this.providerMock, this.stackMock);
        new ChatRoomIrcImpl("#test test", this.providerMock);
    }

    //@Test(expected = IllegalArgumentException.class)
    public void testIllegalNameComma()
    {
        EasyMock.replay(this.providerMock, this.stackMock);
        new ChatRoomIrcImpl("#test,test", this.providerMock);
    }

    //@Test
    public void testValidName()
    {
        EasyMock.replay(this.providerMock, this.stackMock);
        new ChatRoomIrcImpl("#my-cool-channel", this.providerMock);
    }
}
