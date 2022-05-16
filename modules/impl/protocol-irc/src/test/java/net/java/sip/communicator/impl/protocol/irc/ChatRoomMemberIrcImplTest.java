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

import net.java.sip.communicator.service.protocol.*;
import org.easymock.*;
import org.junit.*;

public class ChatRoomMemberIrcImplTest
{
    @Test(expected = IllegalArgumentException.class)
    public void testConstructorNullProvider()
    {
        ChatRoom chatroom = EasyMock.createMock(ChatRoom.class);
        new ChatRoomMemberIrcImpl(null, chatroom, "user", "user",
            "host.name", ChatRoomMemberRole.SILENT_MEMBER,
            IrcStatusEnum.ONLINE);
        fail("should throw IAE for parent provider instance");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorNullChatRoom()
    {
        ProtocolProviderServiceIrcImpl provider =
            EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        new ChatRoomMemberIrcImpl(provider, null, "user", "user",
            "host.name", ChatRoomMemberRole.SILENT_MEMBER,
            IrcStatusEnum.ONLINE);
        fail("should throw IAE for ChatRoom instance");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorNullContactId()
    {
        ChatRoom chatroom = EasyMock.createMock(ChatRoom.class);
        ProtocolProviderServiceIrcImpl provider =
            EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        new ChatRoomMemberIrcImpl(provider, chatroom, null, "user",
            "host.name", ChatRoomMemberRole.SILENT_MEMBER,
            IrcStatusEnum.ONLINE);
        fail("should throw IAE for ChatRoom instance");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorNullIdent()
    {
        ChatRoom chatroom = EasyMock.createMock(ChatRoom.class);
        ProtocolProviderServiceIrcImpl provider =
            EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        new ChatRoomMemberIrcImpl(provider, chatroom, "user", null,
            "host.name", ChatRoomMemberRole.SILENT_MEMBER,
            IrcStatusEnum.ONLINE);
        fail("should throw IAE for ChatRoom instance");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorNullHostname()
    {
        ChatRoom chatroom = EasyMock.createMock(ChatRoom.class);
        ProtocolProviderServiceIrcImpl provider =
            EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        new ChatRoomMemberIrcImpl(provider, chatroom, "user", "user", null,
            ChatRoomMemberRole.SILENT_MEMBER, IrcStatusEnum.ONLINE);
        fail("should throw IAE for ChatRoom instance");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorNullRole()
    {
        ChatRoom chatroom = EasyMock.createMock(ChatRoom.class);
        ProtocolProviderServiceIrcImpl provider =
            EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        new ChatRoomMemberIrcImpl(provider, chatroom, "user", "user",
            "host.name", null, IrcStatusEnum.ONLINE);
        fail("should throw IAE for ChatRoom instance");
    }

    @Test
    public void testConstructorSuccessful()
    {
        ChatRoom chatroom = EasyMock.createMock(ChatRoom.class);
        ProtocolProviderServiceIrcImpl provider =
            EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        new ChatRoomMemberIrcImpl(provider, chatroom,
            "user", "user", "host.name", ChatRoomMemberRole.SILENT_MEMBER,
            IrcStatusEnum.ONLINE);
    }

    @Test
    public void testCheckGetters()
    {
        ChatRoom chatroom = EasyMock.createMock(ChatRoom.class);
        ProtocolProviderServiceIrcImpl provider =
            EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        ChatRoomMemberIrcImpl member =
            new ChatRoomMemberIrcImpl(provider, chatroom, "user", "user",
                "host.name", ChatRoomMemberRole.SILENT_MEMBER,
                IrcStatusEnum.ONLINE);
        assertEquals(provider, member.getProtocolProvider());
        assertEquals(chatroom, member.getChatRoom());
        assertEquals("user", member.getContactAddress());
        assertEquals("user", member.getName());
        assertSame(ChatRoomMemberRole.SILENT_MEMBER, member.getRole());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNameNull()
    {
        ChatRoom chatroom = EasyMock.createMock(ChatRoom.class);
        ProtocolProviderServiceIrcImpl provider =
            EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        ChatRoomMemberIrcImpl member =
            new ChatRoomMemberIrcImpl(provider, chatroom, "user", "user",
                "host.name", ChatRoomMemberRole.SILENT_MEMBER,
                IrcStatusEnum.ONLINE);
        assertEquals("user", member.getContactAddress());
        assertEquals("user", member.getName());
        member.setName(null);
        fail("expected IAE to be thrown");
    }

    @Test
    public void testNameChange()
    {
        ChatRoom chatroom = EasyMock.createMock(ChatRoom.class);
        ProtocolProviderServiceIrcImpl provider =
            EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        ChatRoomMemberIrcImpl member =
            new ChatRoomMemberIrcImpl(provider, chatroom, "user", "user",
                "host.name", ChatRoomMemberRole.SILENT_MEMBER,
                IrcStatusEnum.ONLINE);
        assertEquals("user", member.getContactAddress());
        assertEquals("user", member.getName());
        member.setName("myNewName");
        assertEquals("myNewName", member.getContactAddress());
        assertEquals("myNewName", member.getName());
    }

    @Test
    public void testRoleNull()
    {
        ChatRoom chatroom = EasyMock.createMock(ChatRoom.class);
        ProtocolProviderServiceIrcImpl provider =
            EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        ChatRoomMemberIrcImpl member =
            new ChatRoomMemberIrcImpl(provider, chatroom, "user", "user",
                "host.name",
                ChatRoomMemberRole.SILENT_MEMBER, IrcStatusEnum.ONLINE);
        member.setRole(null);
    }

    @Test
    public void testRoleUnchange()
    {
        ChatRoom chatroom = EasyMock.createMock(ChatRoom.class);
        ProtocolProviderServiceIrcImpl provider =
            EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        ChatRoomMemberIrcImpl member =
            new ChatRoomMemberIrcImpl(provider, chatroom, "user", "user",
                "host.name", ChatRoomMemberRole.SILENT_MEMBER,
                IrcStatusEnum.ONLINE);
        assertSame(ChatRoomMemberRole.SILENT_MEMBER, member.getRole());
        member.setRole(ChatRoomMemberRole.ADMINISTRATOR);
        assertSame(ChatRoomMemberRole.SILENT_MEMBER, member.getRole());
    }

    @Test
    public void testAddSignificantRole()
    {
        ChatRoom chatroom = EasyMock.createMock(ChatRoom.class);
        ProtocolProviderServiceIrcImpl provider =
            EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        ChatRoomMemberIrcImpl member =
            new ChatRoomMemberIrcImpl(provider, chatroom, "user", "user",
                "host.name",
                ChatRoomMemberRole.SILENT_MEMBER, IrcStatusEnum.ONLINE);
        assertSame(ChatRoomMemberRole.SILENT_MEMBER, member.getRole());
        member.addRole(ChatRoomMemberRole.ADMINISTRATOR);
        assertSame(ChatRoomMemberRole.ADMINISTRATOR, member.getRole());
    }

    @Test
    public void testRemoveSignificantRole()
    {
        ChatRoom chatroom = EasyMock.createMock(ChatRoom.class);
        ProtocolProviderServiceIrcImpl provider =
            EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        ChatRoomMemberIrcImpl member =
            new ChatRoomMemberIrcImpl(provider, chatroom, "user", "user",
                "host.name",
                ChatRoomMemberRole.SILENT_MEMBER, IrcStatusEnum.ONLINE);
        member.addRole(ChatRoomMemberRole.ADMINISTRATOR);
        assertSame(ChatRoomMemberRole.ADMINISTRATOR, member.getRole());
        member.removeRole(ChatRoomMemberRole.ADMINISTRATOR);
        assertSame(ChatRoomMemberRole.SILENT_MEMBER, member.getRole());
    }

    @Test
    public void testAddInsignificantRole()
    {
        ChatRoom chatroom = EasyMock.createMock(ChatRoom.class);
        ProtocolProviderServiceIrcImpl provider =
            EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        ChatRoomMemberIrcImpl member =
            new ChatRoomMemberIrcImpl(provider, chatroom, "user", "user",
                "host.name",
                ChatRoomMemberRole.ADMINISTRATOR, IrcStatusEnum.ONLINE);
        assertSame(ChatRoomMemberRole.ADMINISTRATOR, member.getRole());
        member.addRole(ChatRoomMemberRole.MEMBER);
        assertSame(ChatRoomMemberRole.ADMINISTRATOR, member.getRole());
    }

    @Test
    public void testRemoveInsignificantRole()
    {
        ChatRoom chatroom = EasyMock.createMock(ChatRoom.class);
        ProtocolProviderServiceIrcImpl provider =
            EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        ChatRoomMemberIrcImpl member =
            new ChatRoomMemberIrcImpl(provider, chatroom, "user", "user",
                "host.name",
                ChatRoomMemberRole.ADMINISTRATOR, IrcStatusEnum.ONLINE);
        member.addRole(ChatRoomMemberRole.MEMBER);
        assertSame(ChatRoomMemberRole.ADMINISTRATOR, member.getRole());
        member.removeRole(ChatRoomMemberRole.MEMBER);
        assertSame(ChatRoomMemberRole.ADMINISTRATOR, member.getRole());
    }

    @Test
    public void testGetContact()
    {
        ChatRoom chatroom = EasyMock.createMock(ChatRoom.class);
        ProtocolProviderServiceIrcImpl provider =
            EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        OperationSetPersistentPresenceIrcImpl pp =
            EasyMock.createMock(OperationSetPersistentPresenceIrcImpl.class);
        EasyMock.expect(provider.getPersistentPresence()).andReturn(pp);
        EasyMock.replay(provider);
        ChatRoomMemberIrcImpl member =
            new ChatRoomMemberIrcImpl(provider, chatroom, "user", "user",
                "host.name",
                ChatRoomMemberRole.SILENT_MEMBER, IrcStatusEnum.ONLINE);
        assertNull(member.getContact());
    }

    @Test
    public void testGetAvatar()
    {
        ChatRoom chatroom = EasyMock.createMock(ChatRoom.class);
        ProtocolProviderServiceIrcImpl provider =
            EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        ChatRoomMemberIrcImpl member =
            new ChatRoomMemberIrcImpl(provider, chatroom, "user", "user",
                "host.name",
                ChatRoomMemberRole.SILENT_MEMBER, IrcStatusEnum.ONLINE);
        assertNull(member.getAvatar());
    }

    @Test
    public void testEqualsSame()
    {
        ChatRoom chatroom = EasyMock.createMock(ChatRoom.class);
        ProtocolProviderServiceIrcImpl provider =
            EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        ChatRoomMemberIrcImpl member =
            new ChatRoomMemberIrcImpl(provider, chatroom,
                "user", "user", "host.name", ChatRoomMemberRole.SILENT_MEMBER,
                IrcStatusEnum.ONLINE);
        assertEquals(member, member);
    }

    @Test
    public void testEqualsNull()
    {
        ChatRoom chatroom = EasyMock.createMock(ChatRoom.class);
        ProtocolProviderServiceIrcImpl provider =
            EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        ChatRoomMemberIrcImpl member =
            new ChatRoomMemberIrcImpl(provider, chatroom,
                "user", "user", "host.name", ChatRoomMemberRole.SILENT_MEMBER,
                IrcStatusEnum.ONLINE);
        assertNotEquals(null, member);
    }

    @Test
    public void testEqualsObject()
    {
        ChatRoom chatroom = EasyMock.createMock(ChatRoom.class);
        ProtocolProviderServiceIrcImpl provider =
            EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        ChatRoomMemberIrcImpl member =
            new ChatRoomMemberIrcImpl(provider, chatroom,
                "user", "user", "host.name", ChatRoomMemberRole.SILENT_MEMBER,
                IrcStatusEnum.ONLINE);
        assertNotEquals(member, new Object());
    }

    @Test
    public void testEqualsSameUserDifferentProvider()
    {
        ChatRoom chatroom = EasyMock.createMock(ChatRoom.class);
        ProtocolProviderServiceIrcImpl provider =
            EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        ChatRoomMemberIrcImpl member1 =
            new ChatRoomMemberIrcImpl(provider, chatroom,
                "user", "user", "host.name", ChatRoomMemberRole.SILENT_MEMBER,
                IrcStatusEnum.ONLINE);
        ProtocolProviderServiceIrcImpl provider2 =
            EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        ChatRoomMemberIrcImpl member2 =
            new ChatRoomMemberIrcImpl(provider2, chatroom,
                "user", "user", "host.name", ChatRoomMemberRole.SILENT_MEMBER,
                IrcStatusEnum.ONLINE);
        assertNotEquals(member1, member2);
    }

    @Test
    public void testEqualsSameProviderDifferentUser()
    {
        ChatRoom chatroom = EasyMock.createMock(ChatRoom.class);
        ProtocolProviderServiceIrcImpl provider =
            EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        ChatRoomMemberIrcImpl member1 =
            new ChatRoomMemberIrcImpl(provider, chatroom,
                "user", "user", "host.name", ChatRoomMemberRole.SILENT_MEMBER,
                IrcStatusEnum.ONLINE);
        ChatRoomMemberIrcImpl member2 =
            new ChatRoomMemberIrcImpl(provider, chatroom,
                "susy", "user", "host.name", ChatRoomMemberRole.SILENT_MEMBER,
                IrcStatusEnum.ONLINE);
        assertNotEquals(member1, member2);
    }

    @Test
    public void testEqualsTrue()
    {
        ChatRoom chatroom = EasyMock.createMock(ChatRoom.class);
        ProtocolProviderServiceIrcImpl provider =
            EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        ChatRoomMemberIrcImpl member1 =
            new ChatRoomMemberIrcImpl(provider, chatroom,
                "susy", "user", "host.name", ChatRoomMemberRole.SILENT_MEMBER,
                IrcStatusEnum.ONLINE);
        ChatRoomMemberIrcImpl member2 =
            new ChatRoomMemberIrcImpl(provider, chatroom,
                "susy", "user", "host.name", ChatRoomMemberRole.SILENT_MEMBER,
                IrcStatusEnum.ONLINE);
        assertEquals(member1, member2);
    }

    @Test
    public void testHashcodeNotFailing()
    {
        ChatRoom chatroom = EasyMock.createMock(ChatRoom.class);
        ProtocolProviderServiceIrcImpl provider =
            EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        ChatRoomMemberIrcImpl member =
            new ChatRoomMemberIrcImpl(provider, chatroom, "ET", "user",
                "host.name",
                ChatRoomMemberRole.ADMINISTRATOR, IrcStatusEnum.ONLINE);
        member.hashCode();
    }

    @Test
    public void testGetIdent()
    {
        ChatRoom chatroom = EasyMock.createMock(ChatRoom.class);
        ProtocolProviderServiceIrcImpl provider =
            EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        ChatRoomMemberIrcImpl member =
            new ChatRoomMemberIrcImpl(provider, chatroom, "ET", "user",
                "host.name",
                ChatRoomMemberRole.ADMINISTRATOR, IrcStatusEnum.ONLINE);
        assertEquals("user", member.getIdent());
    }

    @Test
    public void testGetHostname()
    {
        ChatRoom chatroom = EasyMock.createMock(ChatRoom.class);
        ProtocolProviderServiceIrcImpl provider =
            EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        ChatRoomMemberIrcImpl member =
            new ChatRoomMemberIrcImpl(provider, chatroom, "ET", "user",
                "host.name",
                ChatRoomMemberRole.ADMINISTRATOR, IrcStatusEnum.ONLINE);
        assertEquals("host.name", member.getHostname());
    }
}
