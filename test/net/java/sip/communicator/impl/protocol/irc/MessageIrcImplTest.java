package net.java.sip.communicator.impl.protocol.irc;

import junit.framework.*;

import org.easymock.*;

public class MessageIrcImplTest
    extends TestCase
{

    public void testConstruction()
    {
        new MessageIrcImpl("Message", MessageIrcImpl.DEFAULT_MIME_TYPE,
            MessageIrcImpl.DEFAULT_MIME_ENCODING, null);
    }

    public void testCorrectConstruction()
    {
        MessageIrcImpl message =
            new MessageIrcImpl("Message", MessageIrcImpl.DEFAULT_MIME_TYPE,
                MessageIrcImpl.DEFAULT_MIME_ENCODING, null);
        Assert.assertEquals("Message", message.getContent());
        Assert.assertEquals(MessageIrcImpl.DEFAULT_MIME_TYPE,
            message.getContentType());
        Assert.assertEquals(MessageIrcImpl.DEFAULT_MIME_ENCODING,
            message.getEncoding());
        Assert.assertNull(message.getSubject());
        Assert.assertFalse(message.isAction());
        Assert.assertFalse(message.isCommand());
    }

    public void testActionRecognized()
    {
        MessageIrcImpl message =
            new MessageIrcImpl("/me is a genius!",
                MessageIrcImpl.DEFAULT_MIME_TYPE,
                MessageIrcImpl.DEFAULT_MIME_ENCODING, null);
        Assert.assertTrue(message.isAction());
        Assert.assertTrue(message.isCommand());
    }

    public void testCommandNonActionRecognized()
    {
        MessageIrcImpl message =
            new MessageIrcImpl("/msg user Hi!",
                MessageIrcImpl.DEFAULT_MIME_TYPE,
                MessageIrcImpl.DEFAULT_MIME_ENCODING, null);
        Assert.assertFalse(message.isAction());
        Assert.assertTrue(message.isCommand());
    }
    
    public void testNewMessageFromIRC()
    {
        MessageIrcImpl message =
            MessageIrcImpl.newMessageFromIRC("Hello world.");
        Assert.assertEquals("Hello world.", message.getContent());
        Assert.assertEquals(MessageIrcImpl.HTML_MIME_TYPE,
            message.getContentType());
        Assert.assertEquals(MessageIrcImpl.DEFAULT_MIME_ENCODING,
            message.getEncoding());
        Assert.assertNull(message.getSubject());
    }

    public void testNewActionFromIRC()
    {
        final String text = "Hello world.";

        // Test with chat room member.
        ChatRoomMemberIrcImpl chatRoomMember =
            EasyMock.createMock(ChatRoomMemberIrcImpl.class);
        EasyMock.replay(chatRoomMember);
        MessageIrcImpl message = MessageIrcImpl.newActionFromIRC(text);
        Assert.assertEquals("/me Hello world.", message.getContent());
        Assert.assertEquals(MessageIrcImpl.HTML_MIME_TYPE,
            message.getContentType());
        Assert.assertEquals(MessageIrcImpl.DEFAULT_MIME_ENCODING,
            message.getEncoding());
        Assert.assertNull(message.getSubject());

        // Test with contact.
        ContactIrcImpl contact = EasyMock.createMock(ContactIrcImpl.class);
        EasyMock.replay(contact);
        message = MessageIrcImpl.newActionFromIRC(text);
        Assert.assertEquals("/me Hello world.", message.getContent());
        Assert.assertEquals(MessageIrcImpl.HTML_MIME_TYPE,
            message.getContentType());
        Assert.assertEquals(MessageIrcImpl.DEFAULT_MIME_ENCODING,
            message.getEncoding());
        Assert.assertNull(message.getSubject());

    }

    public void testNewNoticeFromIRC()
    {
        final String text = "Hello world.";

        // Test with chat room member.
        ChatRoomMemberIrcImpl chatRoomMember =
            EasyMock.createMock(ChatRoomMemberIrcImpl.class);
        EasyMock.expect(chatRoomMember.getContactAddress())
            .andReturn("IamUser");
        EasyMock.replay(chatRoomMember);
        MessageIrcImpl message =
            MessageIrcImpl.newNoticeFromIRC(chatRoomMember, text);
        Assert.assertEquals(Utils.styleAsNotice(text, "IamUser"),
            message.getContent());
        Assert.assertEquals(MessageIrcImpl.HTML_MIME_TYPE,
            message.getContentType());
        Assert.assertEquals(MessageIrcImpl.DEFAULT_MIME_ENCODING,
            message.getEncoding());
        Assert.assertNull(message.getSubject());

        // Test with contact.
        ContactIrcImpl contact = EasyMock.createMock(ContactIrcImpl.class);
        EasyMock.expect(contact.getAddress()).andReturn("IamUser");
        EasyMock.replay(contact);
        message = MessageIrcImpl.newNoticeFromIRC(contact, text);
        Assert.assertEquals(Utils.styleAsNotice(text, "IamUser"),
            message.getContent());
        Assert.assertEquals(MessageIrcImpl.HTML_MIME_TYPE,
            message.getContentType());
        Assert.assertEquals(MessageIrcImpl.DEFAULT_MIME_ENCODING,
            message.getEncoding());
        Assert.assertNull(message.getSubject());

    }
}
