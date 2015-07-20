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

import junit.framework.*;
import net.java.sip.communicator.service.protocol.*;

import org.easymock.*;

public class ChatRoomMemberIrcImplTest
    extends TestCase
{

    public void testConstructorNullProvider()
    {
        ChatRoom chatroom = EasyMock.createMock(ChatRoom.class);
        try
        {
            new ChatRoomMemberIrcImpl(null, chatroom, "user", "user",
                "host.name", ChatRoomMemberRole.SILENT_MEMBER,
                IrcStatusEnum.ONLINE);
            Assert.fail("should throw IAE for parent provider instance");
        }
        catch (IllegalArgumentException e)
        {
            // this is good
        }
    }

    public void testConstructorNullChatRoom()
    {
        ProtocolProviderServiceIrcImpl provider =
            EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        try
        {
            new ChatRoomMemberIrcImpl(provider, null, "user", "user",
                "host.name", ChatRoomMemberRole.SILENT_MEMBER,
                IrcStatusEnum.ONLINE);
            Assert.fail("should throw IAE for ChatRoom instance");
        }
        catch (IllegalArgumentException e)
        {
            // this is good
        }
    }

    public void testConstructorNullContactId()
    {
        ChatRoom chatroom = EasyMock.createMock(ChatRoom.class);
        ProtocolProviderServiceIrcImpl provider =
            EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        try
        {
            new ChatRoomMemberIrcImpl(provider, chatroom, null, "user",
                "host.name", ChatRoomMemberRole.SILENT_MEMBER,
                IrcStatusEnum.ONLINE);
            Assert.fail("should throw IAE for ChatRoom instance");
        }
        catch (IllegalArgumentException e)
        {
            // this is good
        }
    }

    public void testConstructorNullIdent()
    {
        ChatRoom chatroom = EasyMock.createMock(ChatRoom.class);
        ProtocolProviderServiceIrcImpl provider =
            EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        try
        {
            new ChatRoomMemberIrcImpl(provider, chatroom, "user", null,
                "host.name", ChatRoomMemberRole.SILENT_MEMBER,
                IrcStatusEnum.ONLINE);
            Assert.fail("should throw IAE for ChatRoom instance");
        }
        catch (IllegalArgumentException e)
        {
            // this is good
        }
    }

    public void testConstructorNullHostname()
    {
        ChatRoom chatroom = EasyMock.createMock(ChatRoom.class);
        ProtocolProviderServiceIrcImpl provider =
            EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        try
        {
            new ChatRoomMemberIrcImpl(provider, chatroom, "user", "user", null,
                ChatRoomMemberRole.SILENT_MEMBER, IrcStatusEnum.ONLINE);
            Assert.fail("should throw IAE for ChatRoom instance");
        }
        catch (IllegalArgumentException e)
        {
            // this is good
        }
    }

    public void testConstructorNullRole()
    {
        ChatRoom chatroom = EasyMock.createMock(ChatRoom.class);
        ProtocolProviderServiceIrcImpl provider =
            EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        try
        {
            new ChatRoomMemberIrcImpl(provider, chatroom, "user", "user",
                "host.name", null, IrcStatusEnum.ONLINE);
            Assert.fail("should throw IAE for ChatRoom instance");
        }
        catch (IllegalArgumentException e)
        {
            // this is good
        }
    }

    public void testConstructorSuccessful()
    {
        ChatRoom chatroom = EasyMock.createMock(ChatRoom.class);
        ProtocolProviderServiceIrcImpl provider =
            EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        Assert.assertNotNull(new ChatRoomMemberIrcImpl(provider, chatroom,
            "user", "user", "host.name", ChatRoomMemberRole.SILENT_MEMBER,
            IrcStatusEnum.ONLINE));
    }
    
    public void testCheckGetters()
    {
        ChatRoom chatroom = EasyMock.createMock(ChatRoom.class);
        ProtocolProviderServiceIrcImpl provider =
            EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        ChatRoomMemberIrcImpl member =
            new ChatRoomMemberIrcImpl(provider, chatroom, "user", "user",
                "host.name", ChatRoomMemberRole.SILENT_MEMBER,
                IrcStatusEnum.ONLINE);
        Assert.assertEquals(provider, member.getProtocolProvider());
        Assert.assertEquals(chatroom, member.getChatRoom());
        Assert.assertEquals("user", member.getContactAddress());
        Assert.assertEquals("user", member.getName());
        Assert.assertSame(ChatRoomMemberRole.SILENT_MEMBER, member.getRole());
    }
    
    public void testNameNull()
    {
        ChatRoom chatroom = EasyMock.createMock(ChatRoom.class);
        ProtocolProviderServiceIrcImpl provider =
            EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        ChatRoomMemberIrcImpl member =
            new ChatRoomMemberIrcImpl(provider, chatroom, "user", "user",
                "host.name", ChatRoomMemberRole.SILENT_MEMBER,
                IrcStatusEnum.ONLINE);
        Assert.assertEquals("user", member.getContactAddress());
        Assert.assertEquals("user", member.getName());
        try
        {
            member.setName(null);
            Assert.fail("expected IAE to be thrown");
        }
        catch (IllegalArgumentException e)
        {
            // this is good
        }
    }
    
    public void testNameChange()
    {
        ChatRoom chatroom = EasyMock.createMock(ChatRoom.class);
        ProtocolProviderServiceIrcImpl provider =
            EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        ChatRoomMemberIrcImpl member =
            new ChatRoomMemberIrcImpl(provider, chatroom, "user", "user",
                "host.name", ChatRoomMemberRole.SILENT_MEMBER,
                IrcStatusEnum.ONLINE);
        Assert.assertEquals("user", member.getContactAddress());
        Assert.assertEquals("user", member.getName());
        member.setName("myNewName");
        Assert.assertEquals("myNewName", member.getContactAddress());
        Assert.assertEquals("myNewName", member.getName());
    }
    
    public void testRoleNull()
    {
        ChatRoom chatroom = EasyMock.createMock(ChatRoom.class);
        ProtocolProviderServiceIrcImpl provider =
            EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        ChatRoomMemberIrcImpl member =
            new ChatRoomMemberIrcImpl(provider, chatroom, "user", "user", "host.name",
                ChatRoomMemberRole.SILENT_MEMBER, IrcStatusEnum.ONLINE);
        member.setRole(null);
    }
    
    public void testRoleUnchange()
    {
        ChatRoom chatroom = EasyMock.createMock(ChatRoom.class);
        ProtocolProviderServiceIrcImpl provider =
            EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        ChatRoomMemberIrcImpl member =
            new ChatRoomMemberIrcImpl(provider, chatroom, "user", "user",
                "host.name", ChatRoomMemberRole.SILENT_MEMBER,
                IrcStatusEnum.ONLINE);
        Assert.assertSame(ChatRoomMemberRole.SILENT_MEMBER, member.getRole());
        member.setRole(ChatRoomMemberRole.ADMINISTRATOR);
        Assert.assertSame(ChatRoomMemberRole.SILENT_MEMBER, member.getRole());
    }
    
    public void testAddSignificantRole()
    {
        ChatRoom chatroom = EasyMock.createMock(ChatRoom.class);
        ProtocolProviderServiceIrcImpl provider =
            EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        ChatRoomMemberIrcImpl member =
            new ChatRoomMemberIrcImpl(provider, chatroom, "user", "user", "host.name",
                ChatRoomMemberRole.SILENT_MEMBER, IrcStatusEnum.ONLINE);
        Assert.assertSame(ChatRoomMemberRole.SILENT_MEMBER, member.getRole());
        member.addRole(ChatRoomMemberRole.ADMINISTRATOR);
        Assert.assertSame(ChatRoomMemberRole.ADMINISTRATOR, member.getRole());
    }

    public void testRemoveSignificantRole()
    {
        ChatRoom chatroom = EasyMock.createMock(ChatRoom.class);
        ProtocolProviderServiceIrcImpl provider =
            EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        ChatRoomMemberIrcImpl member =
            new ChatRoomMemberIrcImpl(provider, chatroom, "user", "user", "host.name",
                ChatRoomMemberRole.SILENT_MEMBER, IrcStatusEnum.ONLINE);
        member.addRole(ChatRoomMemberRole.ADMINISTRATOR);
        Assert.assertSame(ChatRoomMemberRole.ADMINISTRATOR, member.getRole());
        member.removeRole(ChatRoomMemberRole.ADMINISTRATOR);
        Assert.assertSame(ChatRoomMemberRole.SILENT_MEMBER, member.getRole());
    }

    public void testAddInsignificantRole()
    {
        ChatRoom chatroom = EasyMock.createMock(ChatRoom.class);
        ProtocolProviderServiceIrcImpl provider =
            EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        ChatRoomMemberIrcImpl member =
            new ChatRoomMemberIrcImpl(provider, chatroom, "user", "user", "host.name",
                ChatRoomMemberRole.ADMINISTRATOR, IrcStatusEnum.ONLINE);
        Assert.assertSame(ChatRoomMemberRole.ADMINISTRATOR, member.getRole());
        member.addRole(ChatRoomMemberRole.MEMBER);
        Assert.assertSame(ChatRoomMemberRole.ADMINISTRATOR, member.getRole());
    }

    public void testRemoveInsignificantRole()
    {
        ChatRoom chatroom = EasyMock.createMock(ChatRoom.class);
        ProtocolProviderServiceIrcImpl provider =
            EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        ChatRoomMemberIrcImpl member =
            new ChatRoomMemberIrcImpl(provider, chatroom, "user", "user", "host.name",
                ChatRoomMemberRole.ADMINISTRATOR, IrcStatusEnum.ONLINE);
        member.addRole(ChatRoomMemberRole.MEMBER);
        Assert.assertSame(ChatRoomMemberRole.ADMINISTRATOR, member.getRole());
        member.removeRole(ChatRoomMemberRole.MEMBER);
        Assert.assertSame(ChatRoomMemberRole.ADMINISTRATOR, member.getRole());
    }
    
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
            new ChatRoomMemberIrcImpl(provider, chatroom, "user", "user", "host.name",
                ChatRoomMemberRole.SILENT_MEMBER, IrcStatusEnum.ONLINE);
        Assert.assertNull(member.getContact());
    }
    
    public void testGetAvatar()
    {
        ChatRoom chatroom = EasyMock.createMock(ChatRoom.class);
        ProtocolProviderServiceIrcImpl provider =
            EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        ChatRoomMemberIrcImpl member =
            new ChatRoomMemberIrcImpl(provider, chatroom, "user", "user", "host.name",
                ChatRoomMemberRole.SILENT_MEMBER, IrcStatusEnum.ONLINE);
        Assert.assertNull(member.getAvatar());
    }
    
    public void testEqualsSame()
    {
        ChatRoom chatroom = EasyMock.createMock(ChatRoom.class);
        ProtocolProviderServiceIrcImpl provider =
            EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        ChatRoomMemberIrcImpl member = new ChatRoomMemberIrcImpl(provider, chatroom,
            "user", "user", "host.name", ChatRoomMemberRole.SILENT_MEMBER, IrcStatusEnum.ONLINE);
        Assert.assertTrue(member.equals(member));
    }
    
    public void testEqualsNull()
    {
        ChatRoom chatroom = EasyMock.createMock(ChatRoom.class);
        ProtocolProviderServiceIrcImpl provider =
            EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        ChatRoomMemberIrcImpl member = new ChatRoomMemberIrcImpl(provider, chatroom,
            "user", "user", "host.name", ChatRoomMemberRole.SILENT_MEMBER, IrcStatusEnum.ONLINE);
        Assert.assertFalse(member.equals(null));        
    }

    public void testEqualsObject()
    {
        ChatRoom chatroom = EasyMock.createMock(ChatRoom.class);
        ProtocolProviderServiceIrcImpl provider =
            EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        ChatRoomMemberIrcImpl member = new ChatRoomMemberIrcImpl(provider, chatroom,
            "user", "user", "host.name", ChatRoomMemberRole.SILENT_MEMBER, IrcStatusEnum.ONLINE);
        Assert.assertFalse(member.equals(new Object()));        
    }
    
    public void testEqualsSameUserDifferentProvider()
    {
        ChatRoom chatroom = EasyMock.createMock(ChatRoom.class);
        ProtocolProviderServiceIrcImpl provider =
            EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        ChatRoomMemberIrcImpl member1 = new ChatRoomMemberIrcImpl(provider, chatroom,
            "user", "user", "host.name", ChatRoomMemberRole.SILENT_MEMBER, IrcStatusEnum.ONLINE);
        ProtocolProviderServiceIrcImpl provider2 =
            EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        ChatRoomMemberIrcImpl member2 = new ChatRoomMemberIrcImpl(provider2, chatroom,
            "user", "user", "host.name", ChatRoomMemberRole.SILENT_MEMBER, IrcStatusEnum.ONLINE);
        Assert.assertFalse(member1.equals(member2));        
    }
    
    public void testEqualsSameProviderDifferentUser()
    {
        ChatRoom chatroom = EasyMock.createMock(ChatRoom.class);
        ProtocolProviderServiceIrcImpl provider =
            EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        ChatRoomMemberIrcImpl member1 = new ChatRoomMemberIrcImpl(provider, chatroom,
            "user", "user", "host.name", ChatRoomMemberRole.SILENT_MEMBER, IrcStatusEnum.ONLINE);
        ChatRoomMemberIrcImpl member2 = new ChatRoomMemberIrcImpl(provider, chatroom,
            "susy", "user", "host.name", ChatRoomMemberRole.SILENT_MEMBER, IrcStatusEnum.ONLINE);
        Assert.assertFalse(member1.equals(member2));        
    }
    
    public void testEqualsTrue()
    {
        ChatRoom chatroom = EasyMock.createMock(ChatRoom.class);
        ProtocolProviderServiceIrcImpl provider =
            EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        ChatRoomMemberIrcImpl member1 = new ChatRoomMemberIrcImpl(provider, chatroom,
            "susy", "user", "host.name", ChatRoomMemberRole.SILENT_MEMBER, IrcStatusEnum.ONLINE);
        ChatRoomMemberIrcImpl member2 = new ChatRoomMemberIrcImpl(provider, chatroom,
            "susy", "user", "host.name", ChatRoomMemberRole.SILENT_MEMBER, IrcStatusEnum.ONLINE);
        Assert.assertTrue(member1.equals(member2));
    }
    
    public void testHashcodeNotFailing()
    {
        ChatRoom chatroom = EasyMock.createMock(ChatRoom.class);
        ProtocolProviderServiceIrcImpl provider =
            EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        ChatRoomMemberIrcImpl member =
            new ChatRoomMemberIrcImpl(provider, chatroom, "ET", "user", "host.name",
                ChatRoomMemberRole.ADMINISTRATOR, IrcStatusEnum.ONLINE);
        member.hashCode();
    }
    
    public void testGetIdent()
    {
        ChatRoom chatroom = EasyMock.createMock(ChatRoom.class);
        ProtocolProviderServiceIrcImpl provider =
            EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        ChatRoomMemberIrcImpl member =
            new ChatRoomMemberIrcImpl(provider, chatroom, "ET", "user", "host.name",
                ChatRoomMemberRole.ADMINISTRATOR, IrcStatusEnum.ONLINE);
        Assert.assertEquals("user", member.getIdent());
    }

    public void testGetHostname()
    {
        ChatRoom chatroom = EasyMock.createMock(ChatRoom.class);
        ProtocolProviderServiceIrcImpl provider =
            EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        ChatRoomMemberIrcImpl member =
            new ChatRoomMemberIrcImpl(provider, chatroom, "ET", "user", "host.name",
                ChatRoomMemberRole.ADMINISTRATOR, IrcStatusEnum.ONLINE);
        Assert.assertEquals("host.name", member.getHostname());
    }
}
