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
        formatted.append("Hello world!");
        Assert.assertEquals("Hello world!", formatted.done());
    }
}
