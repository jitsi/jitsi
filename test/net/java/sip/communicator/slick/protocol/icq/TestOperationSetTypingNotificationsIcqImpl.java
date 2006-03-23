/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.slick.protocol.icq;

import junit.framework.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.service.protocol.*;
import java.util.*;

/**
 * Tests functionality of the typing notifications operation set. All we do here
 * is assert that typing notifications sent from the tester agent result in
 * <tt>TypingNotificationEvent</tt>s and that typing notifications sent through
 * the tested operation set are received by the icq tester agent.
 * @author Emil Ivov
 */
public class TestOperationSetTypingNotificationsIcqImpl
    extends TestCase
{
    private static final Logger logger =
        Logger.getLogger(TestOperationSetTypingNotificationsIcqImpl.class);

    private IcqSlickFixture fixture = new IcqSlickFixture();
    private OperationSetTypingNotifications opSetTypingNotifs = null;

    public TestOperationSetTypingNotificationsIcqImpl(String name)
    {
            super(name);
    }

    protected void setUp() throws Exception
    {
        super.setUp();
        fixture.setUp();

        Map supportedOperationSets =
            fixture.provider.getSupportedOperationSets();

        if ( supportedOperationSets == null
            || supportedOperationSets.size() < 1)
            throw new NullPointerException(
                "No OperationSet implementations are supported by "
                +"this ICQ implementation. ");

        //get the operation set presence here.
        opSetTypingNotifs =
            (OperationSetTypingNotifications)supportedOperationSets.get(
                OperationSetTypingNotifications.class.getName());

        //if the op set is null then the implementation doesn't offer a typing.n
        //operation set which is unacceptable for icq.
        if (opSetTypingNotifs == null)
        {
            throw new NullPointerException(
                "An implementation of the ICQ service must provide an "
                + "implementation of at least the one of the Presence "
                + "Operation Sets");
        }
    }

    protected void tearDown() throws Exception
    {
        super.tearDown();

        fixture.tearDown();
    }

    /**
     * Sends a typing notification through the tested implementation and
     * verifies whether it is properly received by the tester agent.
     */
    public void testSendTypingNotification()
    {

        /**@todo implement testSendTypingNotification() */
        fail("@todo implement testSendTypingNotification()");
    }

    /**
     * Sends a typing notification through the icq tester agent and verifies
     * whether it is properly received by the tested implementation
     */
    public void testTypingNotificationsEventDelivery()
    {
        /**@todo implement testTypingNotificationsEventDelivery() */
        fail("@todo implement testTypingNotificationsEventDelivery()");
    }

}
