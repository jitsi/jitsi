package net.java.sip.communicator.impl.protocol.irc;

import org.easymock.*;

import net.java.sip.communicator.service.protocol.*;
import junit.framework.*;

public class ChatRoomMemberIrcImplTest
    extends TestCase
{

    public void testConstructorNullProvider()
    {
        ChatRoom chatroom = EasyMock.createMock(ChatRoom.class);
        try
        {
            new ChatRoomMemberIrcImpl(null, chatroom, "user",
                ChatRoomMemberRole.SILENT_MEMBER);
            Assert.fail("should throw IAE for parent provider instance");
        }
        catch (IllegalArgumentException e)
        {
            // this is good
        }
    }

    public void testConstructorNullChatRoom()
    {
        ProtocolProviderService provider =
            EasyMock.createMock(ProtocolProviderService.class);
        try
        {
            new ChatRoomMemberIrcImpl(provider, null, "user",
                ChatRoomMemberRole.SILENT_MEMBER);
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
        ProtocolProviderService provider =
            EasyMock.createMock(ProtocolProviderService.class);
        try
        {
            new ChatRoomMemberIrcImpl(provider, chatroom, null,
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
        ProtocolProviderService provider =
            EasyMock.createMock(ProtocolProviderService.class);
        try
        {
            new ChatRoomMemberIrcImpl(provider, chatroom, "user", null);
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
        ProtocolProviderService provider =
            EasyMock.createMock(ProtocolProviderService.class);
        Assert.assertNotNull(new ChatRoomMemberIrcImpl(provider, chatroom,
            "user", ChatRoomMemberRole.SILENT_MEMBER));
    }
    
    public void testCheckGetters()
    {
        ChatRoom chatroom = EasyMock.createMock(ChatRoom.class);
        ProtocolProviderService provider =
            EasyMock.createMock(ProtocolProviderService.class);
        ChatRoomMemberIrcImpl member = new ChatRoomMemberIrcImpl(provider, chatroom,
            "user", ChatRoomMemberRole.SILENT_MEMBER);
        Assert.assertEquals(provider, member.getProtocolProvider());
        Assert.assertEquals(chatroom, member.getChatRoom());
        Assert.assertEquals("user", member.getContactAddress());
        Assert.assertEquals("user", member.getName());
        Assert.assertSame(ChatRoomMemberRole.SILENT_MEMBER, member.getRole());
    }
    
    public void testNameNull()
    {
        ChatRoom chatroom = EasyMock.createMock(ChatRoom.class);
        ProtocolProviderService provider =
            EasyMock.createMock(ProtocolProviderService.class);
        ChatRoomMemberIrcImpl member =
            new ChatRoomMemberIrcImpl(provider, chatroom, "user",
                ChatRoomMemberRole.SILENT_MEMBER);
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
        ProtocolProviderService provider =
            EasyMock.createMock(ProtocolProviderService.class);
        ChatRoomMemberIrcImpl member = new ChatRoomMemberIrcImpl(provider, chatroom,
            "user", ChatRoomMemberRole.SILENT_MEMBER);
        Assert.assertEquals("user", member.getContactAddress());
        Assert.assertEquals("user", member.getName());
        member.setName("myNewName");
        Assert.assertEquals("myNewName", member.getContactAddress());
        Assert.assertEquals("myNewName", member.getName());
    }
    
    public void testRoleNull()
    {
        ChatRoom chatroom = EasyMock.createMock(ChatRoom.class);
        ProtocolProviderService provider =
            EasyMock.createMock(ProtocolProviderService.class);
        ChatRoomMemberIrcImpl member =
            new ChatRoomMemberIrcImpl(provider, chatroom, "user",
                ChatRoomMemberRole.SILENT_MEMBER);
        Assert.assertSame(ChatRoomMemberRole.SILENT_MEMBER, member.getRole());
        try
        {
            member.setRole(null);
            Assert.fail("expected IAE because of null role");
        }
        catch (IllegalArgumentException e)
        {
            // this is good
        }
    }
    
    public void testRoleChange()
    {
        ChatRoom chatroom = EasyMock.createMock(ChatRoom.class);
        ProtocolProviderService provider =
            EasyMock.createMock(ProtocolProviderService.class);
        ChatRoomMemberIrcImpl member = new ChatRoomMemberIrcImpl(provider, chatroom,
            "user", ChatRoomMemberRole.SILENT_MEMBER);
        Assert.assertSame(ChatRoomMemberRole.SILENT_MEMBER, member.getRole());
        member.setRole(ChatRoomMemberRole.ADMINISTRATOR);
        Assert.assertSame(ChatRoomMemberRole.ADMINISTRATOR, member.getRole());
    }
    
    public void testGetContact()
    {
        ChatRoom chatroom = EasyMock.createMock(ChatRoom.class);
        ProtocolProviderService provider =
            EasyMock.createMock(ProtocolProviderService.class);
        ChatRoomMemberIrcImpl member = new ChatRoomMemberIrcImpl(provider, chatroom,
            "user", ChatRoomMemberRole.SILENT_MEMBER);
        Assert.assertNull(member.getContact());
    }
    
    public void testGetAvatar()
    {
        ChatRoom chatroom = EasyMock.createMock(ChatRoom.class);
        ProtocolProviderService provider =
            EasyMock.createMock(ProtocolProviderService.class);
        ChatRoomMemberIrcImpl member = new ChatRoomMemberIrcImpl(provider, chatroom,
            "user", ChatRoomMemberRole.SILENT_MEMBER);
        Assert.assertNull(member.getAvatar());
    }
    
    public void testEqualsSame()
    {
        ChatRoom chatroom = EasyMock.createMock(ChatRoom.class);
        ProtocolProviderService provider =
            EasyMock.createMock(ProtocolProviderService.class);
        ChatRoomMemberIrcImpl member = new ChatRoomMemberIrcImpl(provider, chatroom,
            "user", ChatRoomMemberRole.SILENT_MEMBER);
        Assert.assertTrue(member.equals(member));
    }
    
    public void testEqualsNull()
    {
        ChatRoom chatroom = EasyMock.createMock(ChatRoom.class);
        ProtocolProviderService provider =
            EasyMock.createMock(ProtocolProviderService.class);
        ChatRoomMemberIrcImpl member = new ChatRoomMemberIrcImpl(provider, chatroom,
            "user", ChatRoomMemberRole.SILENT_MEMBER);
        Assert.assertFalse(member.equals(null));        
    }

    public void testEqualsObject()
    {
        ChatRoom chatroom = EasyMock.createMock(ChatRoom.class);
        ProtocolProviderService provider =
            EasyMock.createMock(ProtocolProviderService.class);
        ChatRoomMemberIrcImpl member = new ChatRoomMemberIrcImpl(provider, chatroom,
            "user", ChatRoomMemberRole.SILENT_MEMBER);
        Assert.assertFalse(member.equals(new Object()));        
    }
    
    public void testEqualsSameUserDifferentProvider()
    {
        ChatRoom chatroom = EasyMock.createMock(ChatRoom.class);
        ProtocolProviderService provider =
            EasyMock.createMock(ProtocolProviderService.class);
        ChatRoomMemberIrcImpl member1 = new ChatRoomMemberIrcImpl(provider, chatroom,
            "user", ChatRoomMemberRole.SILENT_MEMBER);
        ProtocolProviderService provider2 =
            EasyMock.createMock(ProtocolProviderService.class);
        ChatRoomMemberIrcImpl member2 = new ChatRoomMemberIrcImpl(provider2, chatroom,
            "user", ChatRoomMemberRole.SILENT_MEMBER);
        Assert.assertFalse(member1.equals(member2));        
    }
    
    public void testEqualsSameProviderDifferentUser()
    {
        ChatRoom chatroom = EasyMock.createMock(ChatRoom.class);
        ProtocolProviderService provider =
            EasyMock.createMock(ProtocolProviderService.class);
        ChatRoomMemberIrcImpl member1 = new ChatRoomMemberIrcImpl(provider, chatroom,
            "user", ChatRoomMemberRole.SILENT_MEMBER);
        ChatRoomMemberIrcImpl member2 = new ChatRoomMemberIrcImpl(provider, chatroom,
            "susy", ChatRoomMemberRole.SILENT_MEMBER);
        Assert.assertFalse(member1.equals(member2));        
    }
    
    public void testEqualsTrue()
    {
        ChatRoom chatroom = EasyMock.createMock(ChatRoom.class);
        ProtocolProviderService provider =
            EasyMock.createMock(ProtocolProviderService.class);
        ChatRoomMemberIrcImpl member1 = new ChatRoomMemberIrcImpl(provider, chatroom,
            "susy", ChatRoomMemberRole.SILENT_MEMBER);
        ChatRoomMemberIrcImpl member2 = new ChatRoomMemberIrcImpl(provider, chatroom,
            "susy", ChatRoomMemberRole.SILENT_MEMBER);
        Assert.assertTrue(member1.equals(member2));
    }
}
