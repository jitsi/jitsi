/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
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
                "host.name", ChatRoomMemberRole.SILENT_MEMBER);
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
                "host.name", ChatRoomMemberRole.SILENT_MEMBER);
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
                "host.name", ChatRoomMemberRole.SILENT_MEMBER);
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
                "host.name", ChatRoomMemberRole.SILENT_MEMBER);
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
                ChatRoomMemberRole.SILENT_MEMBER);
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
                "host.name", null);
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
            "user", "user", "host.name", ChatRoomMemberRole.SILENT_MEMBER));
    }
    
    public void testCheckGetters()
    {
        ChatRoom chatroom = EasyMock.createMock(ChatRoom.class);
        ProtocolProviderServiceIrcImpl provider =
            EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        ChatRoomMemberIrcImpl member = new ChatRoomMemberIrcImpl(provider, chatroom,
            "user", "user", "host.name", ChatRoomMemberRole.SILENT_MEMBER);
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
                "host.name", ChatRoomMemberRole.SILENT_MEMBER);
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
        ChatRoomMemberIrcImpl member = new ChatRoomMemberIrcImpl(provider, chatroom,
            "user", "user", "host.name", ChatRoomMemberRole.SILENT_MEMBER);
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
                ChatRoomMemberRole.SILENT_MEMBER);
        member.setRole(null);
    }
    
    public void testRoleUnchange()
    {
        ChatRoom chatroom = EasyMock.createMock(ChatRoom.class);
        ProtocolProviderServiceIrcImpl provider =
            EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        ChatRoomMemberIrcImpl member = new ChatRoomMemberIrcImpl(provider, chatroom,
            "user", "user", "host.name", ChatRoomMemberRole.SILENT_MEMBER);
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
                ChatRoomMemberRole.SILENT_MEMBER);
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
                ChatRoomMemberRole.SILENT_MEMBER);
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
                ChatRoomMemberRole.ADMINISTRATOR);
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
                ChatRoomMemberRole.ADMINISTRATOR);
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
                ChatRoomMemberRole.SILENT_MEMBER);
        Assert.assertNull(member.getContact());
    }
    
    public void testGetAvatar()
    {
        ChatRoom chatroom = EasyMock.createMock(ChatRoom.class);
        ProtocolProviderServiceIrcImpl provider =
            EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        ChatRoomMemberIrcImpl member =
            new ChatRoomMemberIrcImpl(provider, chatroom, "user", "user", "host.name",
                ChatRoomMemberRole.SILENT_MEMBER);
        Assert.assertNull(member.getAvatar());
    }
    
    public void testEqualsSame()
    {
        ChatRoom chatroom = EasyMock.createMock(ChatRoom.class);
        ProtocolProviderServiceIrcImpl provider =
            EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        ChatRoomMemberIrcImpl member = new ChatRoomMemberIrcImpl(provider, chatroom,
            "user", "user", "host.name", ChatRoomMemberRole.SILENT_MEMBER);
        Assert.assertTrue(member.equals(member));
    }
    
    public void testEqualsNull()
    {
        ChatRoom chatroom = EasyMock.createMock(ChatRoom.class);
        ProtocolProviderServiceIrcImpl provider =
            EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        ChatRoomMemberIrcImpl member = new ChatRoomMemberIrcImpl(provider, chatroom,
            "user", "user", "host.name", ChatRoomMemberRole.SILENT_MEMBER);
        Assert.assertFalse(member.equals(null));        
    }

    public void testEqualsObject()
    {
        ChatRoom chatroom = EasyMock.createMock(ChatRoom.class);
        ProtocolProviderServiceIrcImpl provider =
            EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        ChatRoomMemberIrcImpl member = new ChatRoomMemberIrcImpl(provider, chatroom,
            "user", "user", "host.name", ChatRoomMemberRole.SILENT_MEMBER);
        Assert.assertFalse(member.equals(new Object()));        
    }
    
    public void testEqualsSameUserDifferentProvider()
    {
        ChatRoom chatroom = EasyMock.createMock(ChatRoom.class);
        ProtocolProviderServiceIrcImpl provider =
            EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        ChatRoomMemberIrcImpl member1 = new ChatRoomMemberIrcImpl(provider, chatroom,
            "user", "user", "host.name", ChatRoomMemberRole.SILENT_MEMBER);
        ProtocolProviderServiceIrcImpl provider2 =
            EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        ChatRoomMemberIrcImpl member2 = new ChatRoomMemberIrcImpl(provider2, chatroom,
            "user", "user", "host.name", ChatRoomMemberRole.SILENT_MEMBER);
        Assert.assertFalse(member1.equals(member2));        
    }
    
    public void testEqualsSameProviderDifferentUser()
    {
        ChatRoom chatroom = EasyMock.createMock(ChatRoom.class);
        ProtocolProviderServiceIrcImpl provider =
            EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        ChatRoomMemberIrcImpl member1 = new ChatRoomMemberIrcImpl(provider, chatroom,
            "user", "user", "host.name", ChatRoomMemberRole.SILENT_MEMBER);
        ChatRoomMemberIrcImpl member2 = new ChatRoomMemberIrcImpl(provider, chatroom,
            "susy", "user", "host.name", ChatRoomMemberRole.SILENT_MEMBER);
        Assert.assertFalse(member1.equals(member2));        
    }
    
    public void testEqualsTrue()
    {
        ChatRoom chatroom = EasyMock.createMock(ChatRoom.class);
        ProtocolProviderServiceIrcImpl provider =
            EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        ChatRoomMemberIrcImpl member1 = new ChatRoomMemberIrcImpl(provider, chatroom,
            "susy", "user", "host.name", ChatRoomMemberRole.SILENT_MEMBER);
        ChatRoomMemberIrcImpl member2 = new ChatRoomMemberIrcImpl(provider, chatroom,
            "susy", "user", "host.name", ChatRoomMemberRole.SILENT_MEMBER);
        Assert.assertTrue(member1.equals(member2));
    }
    
    public void testHashcodeNotFailing()
    {
        ChatRoom chatroom = EasyMock.createMock(ChatRoom.class);
        ProtocolProviderServiceIrcImpl provider =
            EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        ChatRoomMemberIrcImpl member =
            new ChatRoomMemberIrcImpl(provider, chatroom, "ET", "user", "host.name",
                ChatRoomMemberRole.ADMINISTRATOR);
        member.hashCode();
    }
    
    public void testGetIdent()
    {
        ChatRoom chatroom = EasyMock.createMock(ChatRoom.class);
        ProtocolProviderServiceIrcImpl provider =
            EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        ChatRoomMemberIrcImpl member =
            new ChatRoomMemberIrcImpl(provider, chatroom, "ET", "user", "host.name",
                ChatRoomMemberRole.ADMINISTRATOR);
        Assert.assertEquals("user", member.getIdent());
    }

    public void testGetHostname()
    {
        ChatRoom chatroom = EasyMock.createMock(ChatRoom.class);
        ProtocolProviderServiceIrcImpl provider =
            EasyMock.createMock(ProtocolProviderServiceIrcImpl.class);
        ChatRoomMemberIrcImpl member =
            new ChatRoomMemberIrcImpl(provider, chatroom, "ET", "user", "host.name",
                ChatRoomMemberRole.ADMINISTRATOR);
        Assert.assertEquals("host.name", member.getHostname());
    }
}
