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
        try
        {
            new ChatRoomIrcImpl(null, this.providerMock);
            fail("Should have failed with IAE.");
        }
        catch (IllegalArgumentException e)
        {
        }
    }

    //@Test(expected = IllegalArgumentException.class)
    public void testConstructionNullProvider()
    {
        EasyMock.replay(this.providerMock, this.stackMock);
        try
        {
            new ChatRoomIrcImpl("#test", null);
            fail("Should have failed with IAE.");
        }
        catch (IllegalArgumentException e)
        {
        }
    }

    //@Test(expected = IllegalArgumentException.class)
    public void testEmptyName()
    {
        EasyMock.replay(this.providerMock, this.stackMock);
        try
        {
            new ChatRoomIrcImpl("", this.providerMock);
            fail("Should have failed with IAE.");
        }
        catch (IllegalArgumentException e)
        {
        }
    }

    //@Test(expected = IllegalArgumentException.class)
    public void testIllegalNameBadPrefix()
    {
        EasyMock.replay(this.providerMock, this.stackMock);
        try
        {
            new ChatRoomIrcImpl("!test", this.providerMock);
            fail("Should have failed with IAE.");
        }
        catch (IllegalArgumentException e)
        {
        }
    }

    //@Test(expected = IllegalArgumentException.class)
    public void testIllegalNameSpace()
    {
        EasyMock.replay(this.providerMock, this.stackMock);
        try
        {
            new ChatRoomIrcImpl("#test test", this.providerMock);
            fail("Should have failed with IAE.");
        }
        catch (IllegalArgumentException e)
        {
        }
    }

    //@Test(expected = IllegalArgumentException.class)
    public void testIllegalNameComma()
    {
        EasyMock.replay(this.providerMock, this.stackMock);
        try
        {
            new ChatRoomIrcImpl("#test,test", this.providerMock);
            fail("Should have failed with IAE.");
        }
        catch (IllegalArgumentException e)
        {
        }
    }

    //@Test
    public void testValidName()
    {
        EasyMock.replay(this.providerMock, this.stackMock);
        new ChatRoomIrcImpl("#my-cool-channel", this.providerMock);
    }
    
    //@Test
    public void testCorrectConstruction()
    {
        EasyMock.replay(this.providerMock, this.stackMock);
        ChatRoomIrcImpl room =
            new ChatRoomIrcImpl("#my-cool-channel", this.providerMock);
        Assert.assertEquals("#my-cool-channel", room.getIdentifier());
        Assert.assertEquals("#my-cool-channel", room.getName());
        Assert.assertSame(this.providerMock, room.getParentProvider());
    }

    //@Test
    public void testHashCodeNotFailing()
    {
        EasyMock.replay(this.providerMock, this.stackMock);
        ChatRoomIrcImpl room =
            new ChatRoomIrcImpl("#my-cool-channel", this.providerMock);
        room.hashCode();
    }

    //@Test
    public void testRoomIsJoined()
    {
        EasyMock.expect(this.providerMock.getIrcStack())
            .andReturn(this.stackMock).andReturn(this.stackMock);
        EasyMock
            .expect(
                this.stackMock.isJoined(EasyMock
                    .anyObject(ChatRoomIrcImpl.class))).andReturn(false)
            .andReturn(true);
        EasyMock.replay(this.providerMock, this.stackMock);
        ChatRoomIrcImpl room =
            new ChatRoomIrcImpl("#my-cool-channel", this.providerMock);
        Assert.assertFalse(room.isJoined());
        Assert.assertTrue(room.isJoined());
    }
}
