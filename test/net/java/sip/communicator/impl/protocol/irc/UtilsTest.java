package net.java.sip.communicator.impl.protocol.irc;

import junit.framework.*;

public class UtilsTest
    extends TestCase
{

    protected void setUp() throws Exception
    {
        super.setUp();
    }
    
    protected void tearDown() throws Exception
    {
        super.tearDown();
    }

    public void testNullText()
    {
        Assert.assertEquals(null, Utils.parse(null));
    }
    
    public void testParseEmptyString()
    {
        Assert.assertEquals("", Utils.parse(""));
    }
    
    public void testParseStringWithoutControlCodes()
    {
        final String message = "My normal message without any control codes.";
        Assert.assertEquals(message, Utils.parse(message));
    }
    
    public void testParseStringWithBoldCode()
    {
        final String ircMessage = "My \u0002bold\u0002 message.";
        final String htmlMessage = "My <b>bold</b> message.";
        Assert.assertEquals(htmlMessage, Utils.parse(ircMessage));
    }
    
    public void testParseStringWithItalicsCode()
    {
        final String ircMessage = "My \u001Ditalics\u001D message.";
        final String htmlMessage = "My <i>italics</i> message.";
        Assert.assertEquals(htmlMessage, Utils.parse(ircMessage));
    }

    public void testParseStringWithUnderlineCode()
    {
        final String ircMessage = "My \u001Funderlined\u001F message.";
        final String htmlMessage = "My <u>italics</u> message.";
        Assert.assertEquals(htmlMessage, Utils.parse(ircMessage));
    }

    public void testParseStringWithForegroundColorCode()
    {
        final String ircMessage = "My \u000304RED\u0003 message.";
        final String htmlMessage = "My <font color=\"red\">RED</font> message.";
        Assert.assertEquals(htmlMessage, Utils.parse(ircMessage));
    }

    public void testParseStringWithForegroundAndBackgroundColorCode()
    {
        final String ircMessage = "My \u000304,12RED on Light Blue\u0003 message.";
        final String htmlMessage = "My <font color=\"red\" bgcolor=\"lightblue\">RED on Light Blue</font> message.";
        Assert.assertEquals(htmlMessage, Utils.parse(ircMessage));
    }
}
