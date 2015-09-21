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
    private IrcConnection connectionMock;
    private ChannelManager channelMock;

    //@before
    public void setUp() throws Exception
    {
        super.setUp();
        this.providerMock =
            EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        this.stackMock = EasyMock.createMock(IrcStack.class);
        this.connectionMock = EasyMock.createMock(IrcConnection.class);
        this.channelMock = EasyMock.createMock(ChannelManager.class);
        EasyMock.expect(this.providerMock.getIrcStack()).andReturn(this.stackMock);
        EasyMock.expect(this.stackMock.getConnection()).andReturn(this.connectionMock);
        EasyMock.expect(this.connectionMock.getChannelManager()).andReturn(this.channelMock);
        EasyMock.expect(this.channelMock.getChannelTypes()).andReturn(
            Collections.unmodifiableSet(Sets.newHashSet('#', '&')));
    }

    //@Test
    public void testConstruction()
    {
        EasyMock.replay(this.providerMock, this.stackMock, this.connectionMock, this.channelMock);
        new ChatRoomIrcImpl("#test", this.providerMock);
    }

    //@Test(expected = IllegalArgumentException.class)
    public void testConstructionNullIdentifier()
    {
        EasyMock.replay(this.providerMock, this.stackMock, this.connectionMock, this.channelMock);
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
        EasyMock.replay(this.providerMock, this.stackMock, this.connectionMock, this.channelMock);
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
    public void testTooLongName()
    {
        EasyMock.replay(this.providerMock, this.stackMock, this.connectionMock, this.channelMock);
        try
        {
            new ChatRoomIrcImpl(
                "thisjustalittlebittoolongtobeachannelnamethereforeiexpectthe"
                    + "testtofailsoweshallseifthisisthecasethisjustalittlebit"
                    + "toolongtobeachannelnamethereforeiexpectthetesttofails"
                    + "orweshallseeifthisisthecaseorweshallseeifthisisthecase",
                this.providerMock);
            fail("Should have failed with IAE.");
        }
        catch (IllegalArgumentException e)
        {
        }
    }

    //@Test
    public void testAutoPrefixBadChannelName()
    {
        EasyMock.replay(this.providerMock, this.stackMock, this.connectionMock, this.channelMock);
        ChatRoomIrcImpl room = new ChatRoomIrcImpl("!test", this.providerMock);
        Assert.assertEquals("#!test", room.getIdentifier());
    }

    //@Test(expected = IllegalArgumentException.class)
    public void testIllegalNameSpace()
    {
        EasyMock.replay(this.providerMock, this.stackMock, this.connectionMock, this.channelMock);
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
        EasyMock.replay(this.providerMock, this.stackMock, this.connectionMock, this.channelMock);
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
        EasyMock.replay(this.providerMock, this.stackMock, this.connectionMock, this.channelMock);
        new ChatRoomIrcImpl("#my-cool-channel", this.providerMock);
    }

    //@Test
    public void testCorrectConstruction()
    {
        EasyMock.replay(this.providerMock, this.stackMock, this.connectionMock, this.channelMock);
        ChatRoomIrcImpl room =
            new ChatRoomIrcImpl("#my-cool-channel", this.providerMock);
        Assert.assertEquals("#my-cool-channel", room.getIdentifier());
        Assert.assertEquals("#my-cool-channel", room.getName());
        Assert.assertSame(this.providerMock, room.getParentProvider());
    }

    //@Test
    public void testHashCodeNotFailing()
    {
        EasyMock.replay(this.providerMock, this.stackMock, this.connectionMock, this.channelMock);
        ChatRoomIrcImpl room =
            new ChatRoomIrcImpl("#my-cool-channel", this.providerMock);
        room.hashCode();
    }

    //@Test
    public void testRoomIsJoined()
    {
        EasyMock.expect(this.providerMock.getIrcStack())
            .andReturn(this.stackMock).times(2);
        EasyMock.expect(this.stackMock.getConnection()).andReturn(
            this.connectionMock).times(2);
        EasyMock.expect(this.connectionMock.getChannelManager()).andReturn(this.channelMock).times(2);
        EasyMock
            .expect(
                this.channelMock.isJoined(EasyMock
                    .anyObject(ChatRoomIrcImpl.class))).andReturn(false)
            .andReturn(true);
        EasyMock.replay(this.providerMock, this.stackMock, this.connectionMock, this.channelMock);
        ChatRoomIrcImpl room =
            new ChatRoomIrcImpl("#my-cool-channel", this.providerMock);
        Assert.assertFalse(room.isJoined());
        Assert.assertTrue(room.isJoined());
    }

    //@Test
    public void testIsPersistentRoom()
    {
        EasyMock.replay(this.providerMock, this.stackMock, this.connectionMock, this.channelMock);
        ChatRoomIrcImpl room =
            new ChatRoomIrcImpl("#my-cool-channel", this.providerMock);
        Assert.assertTrue(room.isPersistent());
    }

    //@Test
    public void testDestroyRoom()
    {
        EasyMock.replay(this.providerMock, this.stackMock, this.connectionMock, this.channelMock);
        ChatRoomIrcImpl room =
            new ChatRoomIrcImpl("#my-cool-channel", this.providerMock);
        Assert.assertTrue(room.destroy("whatever", null));
    }

    //@Test
    public void testSetLocalUserNull()
    {
        EasyMock.replay(this.providerMock, this.stackMock, this.connectionMock, this.channelMock);
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
        EasyMock.replay(this.providerMock, this.stackMock, this.connectionMock, this.channelMock);
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
        EasyMock.replay(this.providerMock, this.stackMock, this.connectionMock, this.channelMock,
            user);
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
        EasyMock.replay(this.providerMock, this.stackMock, this.connectionMock, this.channelMock,
            user);
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
        EasyMock.replay(this.providerMock, this.stackMock, this.connectionMock, this.channelMock,
            user);
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
        EasyMock.replay(this.providerMock, this.stackMock, this.connectionMock, this.channelMock);
        ChatRoomIrcImpl room =
            new ChatRoomIrcImpl("#my-cool-channel", this.providerMock);
        Assert.assertTrue(room.equals(room));
    }

    //@Test
    public void testEqualsNull()
    {
        EasyMock.replay(this.providerMock, this.stackMock, this.connectionMock, this.channelMock);
        ChatRoomIrcImpl room =
            new ChatRoomIrcImpl("#my-cool-channel", this.providerMock);
        Assert.assertFalse(room.equals(null));
    }

    //@Test
    public void testEqualsOtherClassInstance()
    {
        EasyMock.replay(this.providerMock, this.stackMock, this.connectionMock, this.channelMock);
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
        EasyMock.expect(this.stackMock.getConnection()).andReturn(
            this.connectionMock);
        EasyMock.expect(this.connectionMock.getChannelManager()).andReturn(this.channelMock);
        EasyMock.expect(this.channelMock.getChannelTypes()).andReturn(
            Collections.unmodifiableSet(Sets.newHashSet('#', '$')));
        EasyMock.replay(this.providerMock, this.stackMock, this.connectionMock, this.channelMock,
            providerMock2);
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
        EasyMock.expect(this.stackMock.getConnection()).andReturn(
            this.connectionMock);
        EasyMock.expect(this.connectionMock.getChannelManager()).andReturn(this.channelMock);
        EasyMock.expect(this.channelMock.getChannelTypes()).andReturn(
            Collections.unmodifiableSet(Sets.newHashSet('#', '$')));
        EasyMock.replay(this.providerMock, this.stackMock, this.connectionMock, this.channelMock);
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
        EasyMock.expect(this.stackMock.getConnection()).andReturn(
            this.connectionMock);
        EasyMock.expect(this.connectionMock.getChannelManager()).andReturn(this.channelMock);
        EasyMock.expect(this.channelMock.getChannelTypes()).andReturn(
            Collections.unmodifiableSet(Sets.newHashSet('#', '$')));
        EasyMock.replay(this.providerMock, this.stackMock, this.connectionMock, this.channelMock);
        ChatRoomIrcImpl room =
            new ChatRoomIrcImpl("#my-cool-channel", this.providerMock);
        ChatRoomIrcImpl room2 =
            new ChatRoomIrcImpl("#my-cool-channel", this.providerMock);
        Assert.assertTrue(room.equals(room2));
    }

    //@Test
    public void testGetChatRoomSubject()
    {
        EasyMock.replay(this.providerMock, this.stackMock, this.connectionMock, this.channelMock);
        ChatRoomIrcImpl room =
            new ChatRoomIrcImpl("#my-cool-channel", this.providerMock);
        Assert.assertEquals("", room.getSubject());
    }

    //@Test
    public void testSetChatRoomSubject() throws OperationFailedException
    {
        final String newSubject = "My test subject!";
        EasyMock.expect(this.connectionMock.getChannelManager()).andReturn(this.channelMock);
        this.channelMock.setSubject(EasyMock.anyObject(ChatRoomIrcImpl.class),
            EasyMock.eq(newSubject));
        EasyMock.expectLastCall();
        EasyMock.expect(this.providerMock.getIrcStack()).andReturn(
            this.stackMock);
        EasyMock.expect(this.stackMock.getConnection()).andReturn(
            this.connectionMock);
        EasyMock.expect(this.connectionMock.getChannelManager()).andReturn(this.channelMock);
        this.channelMock.setSubject(EasyMock.anyObject(ChatRoomIrcImpl.class),
            EasyMock.eq(newSubject));
        EasyMock.expectLastCall();
        EasyMock.replay(this.providerMock, this.stackMock, this.connectionMock, this.channelMock);
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
        EasyMock.expect(this.connectionMock.getChannelManager()).andReturn(this.channelMock);
        this.channelMock.setSubject(EasyMock.anyObject(ChatRoomIrcImpl.class),
            EasyMock.eq(newSubject));
        EasyMock.expectLastCall().andThrow(
            new RuntimeException("Some error", new IOException("Real cause")));
        EasyMock.expect(this.providerMock.getIrcStack()).andReturn(
            this.stackMock);
        EasyMock.expect(this.stackMock.getConnection()).andReturn(
            this.connectionMock);
        EasyMock.expect(this.connectionMock.getChannelManager()).andReturn(this.channelMock);
        this.channelMock.setSubject(EasyMock.anyObject(ChatRoomIrcImpl.class),
            EasyMock.eq(newSubject));
        EasyMock.expectLastCall();
        EasyMock.replay(this.providerMock, this.stackMock, this.connectionMock, this.channelMock);
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
        EasyMock.expect(this.connectionMock.getChannelManager()).andReturn(this.channelMock);
        this.channelMock.setSubject(EasyMock.anyObject(ChatRoomIrcImpl.class),
            EasyMock.eq(newSubject));
        EasyMock.expectLastCall().andThrow(new RuntimeException("Some error"));
        EasyMock.expect(this.providerMock.getIrcStack()).andReturn(
            this.stackMock);
        EasyMock.expect(this.stackMock.getConnection()).andReturn(
            this.connectionMock);
        EasyMock.expect(this.connectionMock.getChannelManager()).andReturn(this.channelMock);
        this.channelMock.setSubject(EasyMock.anyObject(ChatRoomIrcImpl.class),
            EasyMock.eq(newSubject));
        EasyMock.expectLastCall();
        EasyMock.replay(this.providerMock, this.stackMock, this.connectionMock, this.channelMock);
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

    /**
     * Test creating chat room with alternative prefix. Special check to ensure
     * that we don't forget about less often used prefixes.
     */
    // @Test
    public void testChatRoomWithAlternativePrefix()
    {
        EasyMock.replay(this.providerMock, this.stackMock, this.connectionMock, this.channelMock);
        ChatRoomIrcImpl alternative =
            new ChatRoomIrcImpl("&MyAlternative-channel-prefix",
                this.providerMock);
        Assert.assertEquals("&MyAlternative-channel-prefix",
            alternative.getIdentifier());
    }

    public void testOnlyAlternativeChannelTypesWithDefault()
    {
        ProtocolProviderServiceIrcImpl specialProviderMock =
            EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        IrcStack specialStackMock = EasyMock.createMock(IrcStack.class);
        IrcConnection specialConnectionMock = EasyMock.createMock(IrcConnection.class);
        ChannelManager specialChannelMock = EasyMock.createMock(ChannelManager.class);
        EasyMock.expect(specialProviderMock.getIrcStack()).andReturn(
            specialStackMock);
        EasyMock.expect(specialStackMock.getConnection()).andReturn(specialConnectionMock);
        EasyMock.expect(specialConnectionMock.getChannelManager()).andReturn(specialChannelMock);
        EasyMock.expect(specialChannelMock.getChannelTypes()).andReturn(
            Sets.newHashSet('&'));
        EasyMock.replay(specialProviderMock, specialStackMock, specialConnectionMock, specialChannelMock);
        ChatRoomIrcImpl alternative =
            new ChatRoomIrcImpl("channel-name-without-prefix",
                specialProviderMock);
        Assert.assertEquals("#channel-name-without-prefix",
            alternative.getIdentifier());
    }
}
