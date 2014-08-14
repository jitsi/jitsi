package net.java.sip.communicator.impl.protocol.irc;

import java.io.*;
import java.util.*;

import junit.framework.*;
import net.java.sip.communicator.service.protocol.*;

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

    //@Test
    public void testAutoPrefixBadChannelName()
    {
        EasyMock.replay(this.providerMock, this.stackMock);
        ChatRoomIrcImpl room = new ChatRoomIrcImpl("!test", this.providerMock);
        Assert.assertEquals("#!test", room.getIdentifier());
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

    //@Test
    public void testIsPersistentRoom()
    {
        EasyMock.replay(this.providerMock, this.stackMock);
        ChatRoomIrcImpl room =
            new ChatRoomIrcImpl("#my-cool-channel", this.providerMock);
        Assert.assertTrue(room.isPersistent());
    }

    //@Test
    public void testDestroyRoom()
    {
        EasyMock.replay(this.providerMock, this.stackMock);
        ChatRoomIrcImpl room =
            new ChatRoomIrcImpl("#my-cool-channel", this.providerMock);
        Assert.assertTrue(room.destroy("whatever", null));
    }

    //@Test
    public void testSetLocalUserNull()
    {
        EasyMock.replay(this.providerMock, this.stackMock);
        ChatRoomIrcImpl room =
            new ChatRoomIrcImpl("#my-cool-channel", this.providerMock);
        try
        {
            room.setLocalUser(null);
            Assert.fail("Should have failed with IAE.");
        }
        catch (IllegalArgumentException e)
        {
        }
    }

    //@Test
    public void testSetLocalUser()
    {
        EasyMock.replay(this.providerMock, this.stackMock);
        ChatRoomIrcImpl room =
            new ChatRoomIrcImpl("#my-cool-channel", this.providerMock);
        Assert.assertEquals(ChatRoomMemberRole.SILENT_MEMBER,
            room.getUserRole());

        ChatRoomMemberIrcImpl user =
            EasyMock.createMock(ChatRoomMemberIrcImpl.class);
        EasyMock.expect(user.getRole())
            .andReturn(ChatRoomMemberRole.ADMINISTRATOR)
            .andReturn(ChatRoomMemberRole.MEMBER);
        EasyMock.replay(user);
        room.setLocalUser(user);
        Assert.assertEquals(ChatRoomMemberRole.ADMINISTRATOR,
            room.getUserRole());
        // simulate changing user role by returning a different value the second
        // time
        Assert.assertEquals(ChatRoomMemberRole.MEMBER, room.getUserRole());
    }

    //@Test
    public void testMemberCount()
    {
        ChatRoomMemberIrcImpl user =
            EasyMock.createMock(ChatRoomMemberIrcImpl.class);
        EasyMock.replay(this.providerMock, this.stackMock, user);
        ChatRoomIrcImpl room =
            new ChatRoomIrcImpl("#my-cool-channel", this.providerMock);
        Assert.assertEquals(0, room.getMembersCount());

        // add a user
        room.addChatRoomMember("user", user);
        Assert.assertEquals(1, room.getMembersCount());

        room.clearChatRoomMemberList();
        Assert.assertEquals(0, room.getMembersCount());
    }

    //@Test
    public void testAddMember()
    {
        ChatRoomMemberIrcImpl user =
            EasyMock.createMock(ChatRoomMemberIrcImpl.class);
        EasyMock.replay(this.providerMock, this.stackMock, user);
        ChatRoomIrcImpl room =
            new ChatRoomIrcImpl("#my-cool-channel", this.providerMock);
        Assert.assertNull(room.getChatRoomMember("user"));

        // add a user
        room.addChatRoomMember("user", user);
        Assert.assertSame(user, room.getChatRoomMember("user"));
    }

    //@Test
    public void testRemoveMember()
    {
        ChatRoomMemberIrcImpl user =
            EasyMock.createMock(ChatRoomMemberIrcImpl.class);
        EasyMock.replay(this.providerMock, this.stackMock, user);
        ChatRoomIrcImpl room =
            new ChatRoomIrcImpl("#my-cool-channel", this.providerMock);
        room.addChatRoomMember("user", user);
        Assert.assertSame(user, room.getChatRoomMember("user"));

        // remove a user
        room.removeChatRoomMember("user");
        Assert.assertNull(room.getChatRoomMember("user"));
    }

    //@Test
    public void testEqualsSame()
    {
        EasyMock.replay(this.providerMock, this.stackMock);
        ChatRoomIrcImpl room =
            new ChatRoomIrcImpl("#my-cool-channel", this.providerMock);
        Assert.assertTrue(room.equals(room));
    }

    //@Test
    public void testEqualsNull()
    {
        EasyMock.replay(this.providerMock, this.stackMock);
        ChatRoomIrcImpl room =
            new ChatRoomIrcImpl("#my-cool-channel", this.providerMock);
        Assert.assertFalse(room.equals(null));
    }

    //@Test
    public void testEqualsOtherClassInstance()
    {
        EasyMock.replay(this.providerMock, this.stackMock);
        ChatRoomIrcImpl room =
            new ChatRoomIrcImpl("#my-cool-channel", this.providerMock);
        Assert.assertFalse(room.equals(new Object()));
    }

    //@Test
    public void testEqualsOtherProviderInstance()
    {
        ProtocolProviderServiceIrcImpl providerMock2 =
            EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        EasyMock.expect(providerMock2.getIrcStack()).andReturn(this.stackMock);
        EasyMock.expect(this.stackMock.getChannelTypes()).andReturn(
            Collections.unmodifiableSet(Sets.newHashSet('#', '$')));
        EasyMock.replay(this.providerMock, this.stackMock, providerMock2);
        ChatRoomIrcImpl room =
            new ChatRoomIrcImpl("#my-cool-channel", this.providerMock);
        ChatRoomIrcImpl room2 =
            new ChatRoomIrcImpl("#my-cool-channel", providerMock2);
        Assert.assertFalse(room.equals(room2));
    }

    //@Test
    public void testEqualsOtherRoomInstance()
    {
        EasyMock.expect(this.providerMock.getIrcStack()).andReturn(stackMock);
        EasyMock.expect(this.stackMock.getChannelTypes()).andReturn(
            Collections.unmodifiableSet(Sets.newHashSet('#', '$')));
        EasyMock.replay(this.providerMock, this.stackMock);
        ChatRoomIrcImpl room =
            new ChatRoomIrcImpl("#my-cool-channel", this.providerMock);
        ChatRoomIrcImpl room2 =
            new ChatRoomIrcImpl("#my-cool-channel-2", this.providerMock);
        Assert.assertFalse(room.equals(room2));
    }

    //@Test
    public void testEqualsSameRoomRepresentation()
    {
        EasyMock.expect(this.providerMock.getIrcStack()).andReturn(stackMock);
        EasyMock.expect(this.stackMock.getChannelTypes()).andReturn(
            Collections.unmodifiableSet(Sets.newHashSet('#', '$')));
        EasyMock.replay(this.providerMock, this.stackMock);
        ChatRoomIrcImpl room =
            new ChatRoomIrcImpl("#my-cool-channel", this.providerMock);
        ChatRoomIrcImpl room2 =
            new ChatRoomIrcImpl("#my-cool-channel", this.providerMock);
        Assert.assertTrue(room.equals(room2));
    }

    //@Test
    public void testGetChatRoomSubject()
    {
        EasyMock.replay(this.providerMock, this.stackMock);
        ChatRoomIrcImpl room =
            new ChatRoomIrcImpl("#my-cool-channel", this.providerMock);
        Assert.assertEquals("", room.getSubject());
    }

    //@Test
    public void testSetChatRoomSubject() throws OperationFailedException
    {
        final String newSubject = "My test subject!";
        this.stackMock.setSubject(EasyMock.anyObject(ChatRoomIrcImpl.class),
            EasyMock.eq(newSubject));
        EasyMock.expectLastCall();
        EasyMock.expect(this.providerMock.getIrcStack()).andReturn(
            this.stackMock);
        this.stackMock.setSubject(EasyMock.anyObject(ChatRoomIrcImpl.class),
            EasyMock.eq(newSubject));
        EasyMock.expectLastCall();
        EasyMock.replay(this.providerMock, this.stackMock);
        ChatRoomIrcImpl room =
            new ChatRoomIrcImpl("#my-cool-channel", this.providerMock);

        // set a subject
        Assert.assertEquals("", room.getSubject());
        room.setSubject(newSubject);
        // setting subject happens as a result of server accepting topic change,
        // so it should not change immediately
        Assert.assertEquals("", room.getSubject());
    }

    // @Test(expected = OperationFailedException.class)
    public void testSetChatRoomSubjectFailedByIndirectIOException()
        throws OperationFailedException
    {
        final String newSubject = "My test subject!";
        this.stackMock.setSubject(EasyMock.anyObject(ChatRoomIrcImpl.class),
            EasyMock.eq(newSubject));
        EasyMock.expectLastCall().andThrow(
            new RuntimeException("Some error", new IOException("Real cause")));
        EasyMock.expect(this.providerMock.getIrcStack()).andReturn(
            this.stackMock);
        this.stackMock.setSubject(EasyMock.anyObject(ChatRoomIrcImpl.class),
            EasyMock.eq(newSubject));
        EasyMock.expectLastCall();
        EasyMock.replay(this.providerMock, this.stackMock);
        ChatRoomIrcImpl room =
            new ChatRoomIrcImpl("#my-cool-channel", this.providerMock);

        // set a subject
        Assert.assertEquals("", room.getSubject());
        try
        {
            room.setSubject(newSubject);
            Assert.fail("Should have failed with OFE.");
        }
        catch (OperationFailedException e)
        {
        }
    }

    // @Test(expected = OperationFailedException.class)
    public void testSetChatRoomSubjectFailedByOtherRuntimeException()
        throws OperationFailedException
    {
        final String newSubject = "My test subject!";
        this.stackMock.setSubject(EasyMock.anyObject(ChatRoomIrcImpl.class),
            EasyMock.eq(newSubject));
        EasyMock.expectLastCall().andThrow(new RuntimeException("Some error"));
        EasyMock.expect(this.providerMock.getIrcStack()).andReturn(
            this.stackMock);
        this.stackMock.setSubject(EasyMock.anyObject(ChatRoomIrcImpl.class),
            EasyMock.eq(newSubject));
        EasyMock.expectLastCall();
        EasyMock.replay(this.providerMock, this.stackMock);
        ChatRoomIrcImpl room =
            new ChatRoomIrcImpl("#my-cool-channel", this.providerMock);

        // set a subject
        Assert.assertEquals("", room.getSubject());
        try
        {
            room.setSubject(newSubject);
            Assert.fail("Should have failed with OFE.");
        }
        catch (OperationFailedException e)
        {
        }
    }
}
