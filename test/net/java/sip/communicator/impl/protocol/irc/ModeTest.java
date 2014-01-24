package net.java.sip.communicator.impl.protocol.irc;

import net.java.sip.communicator.service.protocol.*;
import junit.framework.*;

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

    public void testGetBySymbol()
    {
        Assert.assertSame(Mode.OPERATOR, Mode.bySymbol('o'));
    }

    public void testGetBySymbolNonExisting()
    {
        try
        {
            Mode.bySymbol('&');
            Assert.fail("Expected IllegalArgumentException");
        }
        catch (IllegalArgumentException e)
        {
        }
    }

    protected void tearDown() throws Exception
    {
        super.tearDown();
    }

}
