package net.java.sip.communicator.impl.protocol.irc;

import junit.framework.*;

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
}
