/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.irc;

import junit.framework.*;

/**
 * @author Danny van Heumen
 */
public class ControlCharTest
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

    public void testFindByControlCharUnknown()
    {
        Assert.assertNull(ControlChar.byCode(' '));
    }

    public void testFindByControlCharBold()
    {
        Assert.assertSame(ControlChar.BOLD, ControlChar.byCode('\u0002'));
    }

    public void testGetHtmlStartSimple()
    {
        Assert.assertEquals("<b>", ControlChar.BOLD.getHtmlStart());
    }

    public void testGetHtmlStartAdvanced()
    {
        Assert.assertEquals("<b bla=\"foo\">",
            ControlChar.BOLD.getHtmlStart("bla=\"foo\""));
    }

    public void testGetHtmlEnd()
    {
        Assert.assertEquals("</b>", ControlChar.BOLD.getHtmlEnd());
    }
}
