/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.irc;

import junit.framework.*;
import net.java.sip.communicator.impl.protocol.irc.exception.*;
import net.java.sip.communicator.service.protocol.*;

public class ModeTest
    extends TestCase
{

    protected void setUp() throws Exception
    {
        super.setUp();
    }

    public void testGetSymbol()
    {
        Assert.assertEquals('o', Mode.OPERATOR.getSymbol());
    }

    public void testGetRole()
    {
        Assert
            .assertTrue(Mode.OPERATOR.getRole() instanceof ChatRoomMemberRole);
    }

    public void testGetBySymbol() throws UnknownModeException
    {
        Assert.assertSame(Mode.OPERATOR, Mode.bySymbol('o'));
    }

    public void testGetBySymbolNonExisting()
    {
        try
        {
            Mode.bySymbol('&');
            Assert.fail("Expected UnknownModeException");
        }
        catch (UnknownModeException e)
        {
        }
    }

    protected void tearDown() throws Exception
    {
        super.tearDown();
    }

}
