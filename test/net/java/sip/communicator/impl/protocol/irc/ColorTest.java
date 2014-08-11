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
public class ColorTest
    extends TestCase
{

    public void testHtmlRepresentation()
    {
        Assert.assertEquals("White", Color.WHITE.getHtml());
    }

    protected void tearDown() throws Exception
    {
        super.tearDown();
    }
}
