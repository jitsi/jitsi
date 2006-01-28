/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.slick.protocol.icq;

import junit.framework.*;

/**
 * @todo describe
 * @author Emil Ivov
 */
public class TestOperationSetBasicInstantMessaging
    extends TestCase
{
    IcqSlickFixture fixture = new IcqSlickFixture();

    public TestOperationSetBasicInstantMessaging(String name)
    {
        super(name);
    }

    protected void setUp() throws Exception
    {
        super.setUp();
        fixture.setUp();

        /** @todo extract corresponding presence set here */

    }

    protected void tearDown() throws Exception
    {
        fixture.tearDown();
        super.tearDown();
    }

    public void testSendMessage()
    {
        /** @todo implement testSendMessage() */
    }

    public void testReceiveMessage()
    {
        /**
         * @todo implement testReceiveMessage()
         */
    }
}
