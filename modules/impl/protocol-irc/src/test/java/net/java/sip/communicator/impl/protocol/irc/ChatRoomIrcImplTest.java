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

import java.io.*;
import java.util.*;
import net.java.sip.communicator.service.protocol.*;
import org.easymock.*;
import org.junit.*;

public class ChatRoomIrcImplTest
{
    private ProtocolProviderServiceIrcImpl providerMock;

    private IrcStack stackMock;

    private IrcConnection connectionMock;

    private ChannelManager channelMock;

    @Before
    public void setUp()
    {
        this.providerMock =
            EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        this.stackMock = EasyMock.createMock(IrcStack.class);
        this.connectionMock = EasyMock.createMock(IrcConnection.class);
        this.channelMock = EasyMock.createMock(ChannelManager.class);
        EasyMock.expect(this.providerMock.getIrcStack())
            .andReturn(this.stackMock);
        EasyMock.expect(this.stackMock.getConnection())
            .andReturn(this.connectionMock);
        EasyMock.expect(this.connectionMock.getChannelManager())
            .andReturn(this.channelMock);
        EasyMock.expect(this.channelMock.getChannelTypes()).andReturn(
            Set.of('#', '&'));
    }

    @Test
    public void testConstruction()
    {
        EasyMock.replay(this.providerMock, this.stackMock, this.connectionMock,
            this.channelMock);
        new ChatRoomIrcImpl("#test", this.providerMock);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructionNullIdentifier()
    {
        EasyMock.replay(this.providerMock, this.stackMock, this.connectionMock,
            this.channelMock);
        new ChatRoomIrcImpl(null, this.providerMock);
        fail("Should have failed with IAE.");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructionNullProvider()
    {
        EasyMock.replay(this.providerMock, this.stackMock);
        new ChatRoomIrcImpl("#test", null);
        fail("Should have failed with IAE.");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyName()
    {
        EasyMock.replay(this.providerMock, this.stackMock, this.connectionMock,
            this.channelMock);
        new ChatRoomIrcImpl("", this.providerMock);
        fail("Should have failed with IAE.");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTooLongName()
    {
        EasyMock.replay(this.providerMock, this.stackMock, this.connectionMock,
            this.channelMock);
        new ChatRoomIrcImpl(
            "thisjustalittlebittoolongtobeachannelnamethereforeiexpectthe"
                + "testtofailsoweshallseifthisisthecasethisjustalittlebit"
                + "toolongtobeachannelnamethereforeiexpectthetesttofails"
                + "orweshallseeifthisisthecaseorweshallseeifthisisthecase",
            this.providerMock);
        fail("Should have failed with IAE.");
    }

    @Test
    public void testAutoPrefixBadChannelName()
    {
        EasyMock.replay(this.providerMock, this.stackMock, this.connectionMock,
            this.channelMock);
        ChatRoomIrcImpl room = new ChatRoomIrcImpl("!test", this.providerMock);
        assertEquals("#!test", room.getIdentifier());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalNameSpace()
    {
        EasyMock.replay(this.providerMock, this.stackMock, this.connectionMock,
            this.channelMock);
        new ChatRoomIrcImpl("#test test", this.providerMock);
        fail("Should have failed with IAE.");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalNameComma()
    {
        EasyMock.replay(this.providerMock, this.stackMock, this.connectionMock,
            this.channelMock);
        new ChatRoomIrcImpl("#test,test", this.providerMock);
        fail("Should have failed with IAE.");
    }

    @Test
    public void testValidName()
    {
        EasyMock.replay(this.providerMock, this.stackMock, this.connectionMock,
            this.channelMock);
        new ChatRoomIrcImpl("#my-cool-channel", this.providerMock);
    }

    @Test
    public void testCorrectConstruction()
    {
        EasyMock.replay(this.providerMock, this.stackMock, this.connectionMock,
            this.channelMock);
        ChatRoomIrcImpl room =
            new ChatRoomIrcImpl("#my-cool-channel", this.providerMock);
        assertEquals("#my-cool-channel", room.getIdentifier());
        assertEquals("#my-cool-channel", room.getName());
        assertSame(this.providerMock, room.getParentProvider());
    }

    @Test
    public void testHashCodeNotFailing()
    {
        EasyMock.replay(this.providerMock, this.stackMock, this.connectionMock,
            this.channelMock);
        ChatRoomIrcImpl room =
            new ChatRoomIrcImpl("#my-cool-channel", this.providerMock);
        room.hashCode();
    }

    @Test
    public void testRoomIsJoined()
    {
        EasyMock.expect(this.providerMock.getIrcStack())
            .andReturn(this.stackMock).times(2);
        EasyMock.expect(this.stackMock.getConnection()).andReturn(
            this.connectionMock).times(2);
        EasyMock.expect(this.connectionMock.getChannelManager())
            .andReturn(this.channelMock).times(2);
        EasyMock
            .expect(
                this.channelMock.isJoined(EasyMock
                    .anyObject(ChatRoomIrcImpl.class))).andReturn(false)
            .andReturn(true);
        EasyMock.replay(this.providerMock, this.stackMock, this.connectionMock,
            this.channelMock);
        ChatRoomIrcImpl room =
            new ChatRoomIrcImpl("#my-cool-channel", this.providerMock);
        assertFalse(room.isJoined());
        assertTrue(room.isJoined());
    }

    @Test
    public void testIsPersistentRoom()
    {
        EasyMock.replay(this.providerMock, this.stackMock, this.connectionMock,
            this.channelMock);
        ChatRoomIrcImpl room =
            new ChatRoomIrcImpl("#my-cool-channel", this.providerMock);
        assertTrue(room.isPersistent());
    }

    @Test
    public void testDestroyRoom()
    {
        EasyMock.replay(this.providerMock, this.stackMock, this.connectionMock,
            this.channelMock);
        ChatRoomIrcImpl room =
            new ChatRoomIrcImpl("#my-cool-channel", this.providerMock);
        assertTrue(room.destroy("whatever", null));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetLocalUserNull()
    {
        EasyMock.replay(this.providerMock, this.stackMock, this.connectionMock,
            this.channelMock);
        ChatRoomIrcImpl room =
            new ChatRoomIrcImpl("#my-cool-channel", this.providerMock);
        room.setLocalUser(null);
        fail("Should have failed with IAE.");
    }

    @Test
    public void testSetLocalUser()
    {
        EasyMock.replay(this.providerMock, this.stackMock, this.connectionMock,
            this.channelMock);
        ChatRoomIrcImpl room =
            new ChatRoomIrcImpl("#my-cool-channel", this.providerMock);
        assertEquals(ChatRoomMemberRole.SILENT_MEMBER,
            room.getUserRole());

        ChatRoomMemberIrcImpl user =
            EasyMock.createMock(ChatRoomMemberIrcImpl.class);
        EasyMock.expect(user.getRole())
            .andReturn(ChatRoomMemberRole.ADMINISTRATOR)
            .andReturn(ChatRoomMemberRole.MEMBER);
        EasyMock.replay(user);
        room.setLocalUser(user);
        assertEquals(ChatRoomMemberRole.ADMINISTRATOR,
            room.getUserRole());
        // simulate changing user role by returning a different value the second
        // time
        assertEquals(ChatRoomMemberRole.MEMBER, room.getUserRole());
    }

    @Test
    public void testMemberCount()
    {
        ChatRoomMemberIrcImpl user =
            EasyMock.createMock(ChatRoomMemberIrcImpl.class);
        EasyMock.replay(this.providerMock, this.stackMock, this.connectionMock,
            this.channelMock,
            user);
        ChatRoomIrcImpl room =
            new ChatRoomIrcImpl("#my-cool-channel", this.providerMock);
        assertEquals(0, room.getMembersCount());

        // add a user
        room.addChatRoomMember("user", user);
        assertEquals(1, room.getMembersCount());

        room.clearChatRoomMemberList();
        assertEquals(0, room.getMembersCount());
    }

    @Test
    public void testAddMember()
    {
        ChatRoomMemberIrcImpl user =
            EasyMock.createMock(ChatRoomMemberIrcImpl.class);
        EasyMock.replay(this.providerMock, this.stackMock, this.connectionMock,
            this.channelMock,
            user);
        ChatRoomIrcImpl room =
            new ChatRoomIrcImpl("#my-cool-channel", this.providerMock);
        assertNull(room.getChatRoomMember("user"));

        // add a user
        room.addChatRoomMember("user", user);
        assertSame(user, room.getChatRoomMember("user"));
    }

    @Test
    public void testRemoveMember()
    {
        ChatRoomMemberIrcImpl user =
            EasyMock.createMock(ChatRoomMemberIrcImpl.class);
        EasyMock.replay(this.providerMock, this.stackMock, this.connectionMock,
            this.channelMock,
            user);
        ChatRoomIrcImpl room =
            new ChatRoomIrcImpl("#my-cool-channel", this.providerMock);
        room.addChatRoomMember("user", user);
        assertSame(user, room.getChatRoomMember("user"));

        // remove a user
        room.removeChatRoomMember("user");
        assertNull(room.getChatRoomMember("user"));
    }

    @Test
    public void testEqualsSame()
    {
        EasyMock.replay(this.providerMock, this.stackMock, this.connectionMock,
            this.channelMock);
        ChatRoomIrcImpl room =
            new ChatRoomIrcImpl("#my-cool-channel", this.providerMock);
        assertEquals(room, room);
    }

    @Test
    public void testEqualsNull()
    {
        EasyMock.replay(this.providerMock, this.stackMock, this.connectionMock,
            this.channelMock);
        ChatRoomIrcImpl room =
            new ChatRoomIrcImpl("#my-cool-channel", this.providerMock);
        assertNotEquals(null, room);
    }

    @Test
    public void testEqualsOtherClassInstance()
    {
        EasyMock.replay(this.providerMock, this.stackMock, this.connectionMock,
            this.channelMock);
        ChatRoomIrcImpl room =
            new ChatRoomIrcImpl("#my-cool-channel", this.providerMock);
        assertNotEquals(room, new Object());
    }

    @Test
    public void testEqualsOtherProviderInstance()
    {
        ProtocolProviderServiceIrcImpl providerMock2 =
            EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        EasyMock.expect(providerMock2.getIrcStack()).andReturn(this.stackMock);
        EasyMock.expect(this.stackMock.getConnection()).andReturn(
            this.connectionMock);
        EasyMock.expect(this.connectionMock.getChannelManager())
            .andReturn(this.channelMock);
        EasyMock.expect(this.channelMock.getChannelTypes()).andReturn(
            Set.of('#', '&'));
        EasyMock.replay(this.providerMock, this.stackMock, this.connectionMock,
            this.channelMock,
            providerMock2);
        ChatRoomIrcImpl room =
            new ChatRoomIrcImpl("#my-cool-channel", this.providerMock);
        ChatRoomIrcImpl room2 =
            new ChatRoomIrcImpl("#my-cool-channel", providerMock2);
        assertNotEquals(room, room2);
    }

    @Test
    public void testEqualsOtherRoomInstance()
    {
        EasyMock.expect(this.providerMock.getIrcStack()).andReturn(stackMock);
        EasyMock.expect(this.stackMock.getConnection()).andReturn(
            this.connectionMock);
        EasyMock.expect(this.connectionMock.getChannelManager())
            .andReturn(this.channelMock);
        EasyMock.expect(this.channelMock.getChannelTypes()).andReturn(
            Set.of('#', '&'));
        EasyMock.replay(this.providerMock, this.stackMock, this.connectionMock,
            this.channelMock);
        ChatRoomIrcImpl room =
            new ChatRoomIrcImpl("#my-cool-channel", this.providerMock);
        ChatRoomIrcImpl room2 =
            new ChatRoomIrcImpl("#my-cool-channel-2", this.providerMock);
        assertNotEquals(room, room2);
    }

    @Test
    public void testEqualsSameRoomRepresentation()
    {
        EasyMock.expect(this.providerMock.getIrcStack()).andReturn(stackMock);
        EasyMock.expect(this.stackMock.getConnection()).andReturn(
            this.connectionMock);
        EasyMock.expect(this.connectionMock.getChannelManager())
            .andReturn(this.channelMock);
        EasyMock.expect(this.channelMock.getChannelTypes()).andReturn(
            Set.of('#', '&'));
        EasyMock.replay(this.providerMock, this.stackMock, this.connectionMock,
            this.channelMock);
        ChatRoomIrcImpl room =
            new ChatRoomIrcImpl("#my-cool-channel", this.providerMock);
        ChatRoomIrcImpl room2 =
            new ChatRoomIrcImpl("#my-cool-channel", this.providerMock);
        assertEquals(room, room2);
    }

    @Test
    public void testGetChatRoomSubject()
    {
        EasyMock.replay(this.providerMock, this.stackMock, this.connectionMock,
            this.channelMock);
        ChatRoomIrcImpl room =
            new ChatRoomIrcImpl("#my-cool-channel", this.providerMock);
        assertEquals("", room.getSubject());
    }

    @Test
    public void testSetChatRoomSubject() throws OperationFailedException
    {
        final String newSubject = "My test subject!";
        EasyMock.expect(this.connectionMock.getChannelManager())
            .andReturn(this.channelMock);
        this.channelMock.setSubject(EasyMock.anyObject(ChatRoomIrcImpl.class),
            EasyMock.eq(newSubject));
        EasyMock.expectLastCall();
        EasyMock.expect(this.providerMock.getIrcStack()).andReturn(
            this.stackMock);
        EasyMock.expect(this.stackMock.getConnection()).andReturn(
            this.connectionMock);
        EasyMock.expect(this.connectionMock.getChannelManager())
            .andReturn(this.channelMock);
        this.channelMock.setSubject(EasyMock.anyObject(ChatRoomIrcImpl.class),
            EasyMock.eq(newSubject));
        EasyMock.expectLastCall();
        EasyMock.replay(this.providerMock, this.stackMock, this.connectionMock,
            this.channelMock);
        ChatRoomIrcImpl room =
            new ChatRoomIrcImpl("#my-cool-channel", this.providerMock);

        // set a subject
        assertEquals("", room.getSubject());
        room.setSubject(newSubject);
        // setting subject happens as a result of server accepting topic change,
        // so it should not change immediately
        assertEquals("", room.getSubject());
    }

    @Test(expected = OperationFailedException.class)
    public void testSetChatRoomSubjectFailedByIndirectIOException()
        throws OperationFailedException
    {
        final String newSubject = "My test subject!";
        EasyMock.expect(this.connectionMock.getChannelManager())
            .andReturn(this.channelMock);
        this.channelMock.setSubject(EasyMock.anyObject(ChatRoomIrcImpl.class),
            EasyMock.eq(newSubject));
        EasyMock.expectLastCall().andThrow(
            new RuntimeException("Some error", new IOException("Real cause")));
        EasyMock.expect(this.providerMock.getIrcStack()).andReturn(
            this.stackMock);
        EasyMock.expect(this.stackMock.getConnection()).andReturn(
            this.connectionMock);
        EasyMock.expect(this.connectionMock.getChannelManager())
            .andReturn(this.channelMock);
        this.channelMock.setSubject(EasyMock.anyObject(ChatRoomIrcImpl.class),
            EasyMock.eq(newSubject));
        EasyMock.expectLastCall();
        EasyMock.replay(this.providerMock, this.stackMock, this.connectionMock,
            this.channelMock);
        ChatRoomIrcImpl room =
            new ChatRoomIrcImpl("#my-cool-channel", this.providerMock);

        // set a subject
        assertEquals("", room.getSubject());
        room.setSubject(newSubject);
        fail("Should have failed with OFE.");
    }

    @Test(expected = OperationFailedException.class)
    public void testSetChatRoomSubjectFailedByOtherRuntimeException()
        throws OperationFailedException
    {
        final String newSubject = "My test subject!";
        EasyMock.expect(this.connectionMock.getChannelManager())
            .andReturn(this.channelMock);
        this.channelMock.setSubject(EasyMock.anyObject(ChatRoomIrcImpl.class),
            EasyMock.eq(newSubject));
        EasyMock.expectLastCall().andThrow(new RuntimeException("Some error"));
        EasyMock.expect(this.providerMock.getIrcStack()).andReturn(
            this.stackMock);
        EasyMock.expect(this.stackMock.getConnection()).andReturn(
            this.connectionMock);
        EasyMock.expect(this.connectionMock.getChannelManager())
            .andReturn(this.channelMock);
        this.channelMock.setSubject(EasyMock.anyObject(ChatRoomIrcImpl.class),
            EasyMock.eq(newSubject));
        EasyMock.expectLastCall();
        EasyMock.replay(this.providerMock, this.stackMock, this.connectionMock,
            this.channelMock);
        ChatRoomIrcImpl room =
            new ChatRoomIrcImpl("#my-cool-channel", this.providerMock);

        // set a subject
        assertEquals("", room.getSubject());
        room.setSubject(newSubject);
        fail("Should have failed with OFE.");
    }

    /**
     * Test creating chat room with alternative prefix. Special check to ensure
     * that we don't forget about less often used prefixes.
     */
    @Test
    public void testChatRoomWithAlternativePrefix()
    {
        EasyMock.replay(this.providerMock, this.stackMock, this.connectionMock,
            this.channelMock);
        ChatRoomIrcImpl alternative =
            new ChatRoomIrcImpl("&MyAlternative-channel-prefix",
                this.providerMock);
        assertEquals("&MyAlternative-channel-prefix",
            alternative.getIdentifier());
    }

    @Test
    public void testOnlyAlternativeChannelTypesWithDefault()
    {
        ProtocolProviderServiceIrcImpl specialProviderMock =
            EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        IrcStack specialStackMock = EasyMock.createMock(IrcStack.class);
        IrcConnection specialConnectionMock =
            EasyMock.createMock(IrcConnection.class);
        ChannelManager specialChannelMock =
            EasyMock.createMock(ChannelManager.class);
        EasyMock.expect(specialProviderMock.getIrcStack()).andReturn(
            specialStackMock);
        EasyMock.expect(specialStackMock.getConnection())
            .andReturn(specialConnectionMock);
        EasyMock.expect(specialConnectionMock.getChannelManager())
            .andReturn(specialChannelMock);
        EasyMock.expect(specialChannelMock.getChannelTypes()).andReturn(
            Set.of('&'));
        EasyMock.replay(specialProviderMock, specialStackMock,
            specialConnectionMock, specialChannelMock);
        ChatRoomIrcImpl alternative =
            new ChatRoomIrcImpl("channel-name-without-prefix",
                specialProviderMock);
        assertEquals("#channel-name-without-prefix",
            alternative.getIdentifier());
    }
}
