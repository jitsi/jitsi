package net.java.sip.communicator.impl.protocol.irc;

import junit.framework.*;

public class FormattedTextBuilderTest
    extends TestCase
{

    public void testConstructFormattedTextBuilder()
    {
        new FormattedTextBuilder();
    }
    
    public void testFormatNothing()
    {
        FormattedTextBuilder formatted = new FormattedTextBuilder();
        Assert.assertEquals("", formatted.done());
    }
    
    public void testPlainText()
    {
        FormattedTextBuilder formatted = new FormattedTextBuilder();
        formatted.append("Hello world!");
        Assert.assertEquals("Hello world!", formatted.done());
    }
    
    public void testPlainChar()
    {
        FormattedTextBuilder formatted = new FormattedTextBuilder();
        formatted.append('H');
        formatted.append('e');
        formatted.append('l');
        formatted.append('l');
        formatted.append('o');
        Assert.assertEquals("Hello", formatted.done());
    }
    
    public void testDoneWithoutFormatting()
    {
        FormattedTextBuilder formatted = new FormattedTextBuilder();
        formatted.append("Hello world!");
        Assert.assertEquals("Hello world!", formatted.done());
    }
    
    public void testDoneRepeatedly()
    {
        FormattedTextBuilder formatted = new FormattedTextBuilder();
        formatted.append("Hello world!");
        formatted.done();
        formatted.done();
        Assert.assertEquals("Hello world!", formatted.done());
    }
    
    public void testOnlyFormatting()
    {
        FormattedTextBuilder formatted = new FormattedTextBuilder();
        formatted.apply(ControlChar.BOLD);
        Assert.assertEquals("<b></b>", formatted.done());
    }
    
    public void testMixedFormattingContent()
    {
        FormattedTextBuilder formatted = new FormattedTextBuilder();
        formatted.apply(ControlChar.BOLD);
        formatted.append("Hello ");
        formatted.apply(ControlChar.ITALICS);
        formatted.append("world");
        formatted.apply(ControlChar.BOLD);
        formatted.append("!!!");
        Assert.assertEquals("<b>Hello <i>world</i></b><i>!!!</i>",
            formatted.done());
    }
    
    public void testToStringIntermediateResult()
    {
        FormattedTextBuilder formatted = new FormattedTextBuilder();
        formatted.apply(ControlChar.BOLD);
        formatted.append("Hello ");
        formatted.apply(ControlChar.ITALICS);
        Assert.assertEquals("<b>Hello <i>", formatted.toString());
        formatted.append("world");
        formatted.apply(ControlChar.BOLD);
        formatted.append("!!!");
        Assert.assertEquals("<b>Hello <i>world</i></b><i>!!!",
            formatted.toString());
        Assert.assertEquals("<b>Hello <i>world</i></b><i>!!!</i>",
            formatted.done());
        Assert.assertEquals("<b>Hello <i>world</i></b><i>!!!</i>",
            formatted.toString());
    }
    
    public void testActiveFormatting()
    {
        FormattedTextBuilder formatted = new FormattedTextBuilder();
        Assert.assertFalse(formatted.isActive(ControlChar.BOLD));
        formatted.apply(ControlChar.BOLD);
        Assert.assertTrue(formatted.isActive(ControlChar.BOLD));
        formatted.append("Hello ");
        Assert.assertFalse(formatted.isActive(ControlChar.ITALICS));
        formatted.apply(ControlChar.ITALICS);
        Assert.assertTrue(formatted.isActive(ControlChar.ITALICS));
        formatted.done();
        Assert.assertFalse(formatted.isActive(ControlChar.BOLD));
        Assert.assertFalse(formatted.isActive(ControlChar.ITALICS));
    }
}
