/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.slick.protocol.jabber;

import net.java.sip.communicator.util.*;
import junit.framework.*;
import net.java.sip.communicator.service.protocol.*;
import java.util.*;

/**
 * Creates a chat room on the server, then tries to make both users join the
 * chatroom. Users would then exchange messages and perform a number of chat
 * room operations.
 *
 * @author Emil Ivov
 */
public class TestOperationSetMultiUserChat
    extends TestCase
{
    private static final Logger logger
        = Logger.getLogger(TestOperationSetMultiUserChat.class);

    private JabberSlickFixture fixture = new JabberSlickFixture();

    private OperationSetPresence opSetPresence1 = null;
    private OperationSetPresence opSetPresence2 = null;

    private OperationSetMultiUserChat opSetMultiChat1 = null;
    private OperationSetMultiUserChat opSetMultiChat2 = null;

    /**
     * Creates the test with the specified method name.
     * @param name the name of the method to execute.
     */
    public TestOperationSetMultiUserChat(String name)
    {
        super(name);
    }

    /**
     * Creates a test suite containing tests of this class in a specific order.
     * We'll first execute a test where we receive a typing notification, and
     * a volatile contact is created for the sender. we'll then be able to
     * retrieve this volatile contact and them a notification on our turn.
     * We need to do things this way as the contact corresponding to the tester
     * agent has been removed in the previous test and we no longer have it
     * in our contact list.
     *
     * @return Test a testsuite containing all tests to execute.
     */
    public static Test suite()
    {
        TestSuite suite = new TestSuite(TestOperationSetMultiUserChat.class);

        return suite;
    }


    /**
     * JUnit setup method.
     * @throws Exception in case anything goes wrong.
     */
    protected void setUp() throws Exception
    {
        super.setUp();
        fixture.setUp();

        Map supportedOperationSets1 =
            fixture.provider1.getSupportedOperationSets();

        if ( supportedOperationSets1 == null
            || supportedOperationSets1.size() < 1)
            throw new NullPointerException(
                "No OperationSet implementations are supported by "
                +"this implementation. ");

        //get the operation set presence here.
        opSetMultiChat1 =
            (OperationSetMultiUserChat)supportedOperationSets1.get(
                OperationSetMultiUserChat.class.getName());

        if (opSetMultiChat1 == null)
        {
            throw new NullPointerException(
                "No implementation for multi user chat was found");
        }

        //we also need the presence op set in order to retrieve contacts.
        opSetPresence1 =
            (OperationSetPresence)supportedOperationSets1.get(
                OperationSetPresence.class.getName());

        //if the op set is null show that we're not happy.
        if (opSetPresence1 == null)
        {
            throw new NullPointerException(
                "An implementation of the service must provide an "
                + "implementation of at least one of the PresenceOperationSets");
        }

        Map supportedOperationSets2 =
            fixture.provider2.getSupportedOperationSets();

        if ( supportedOperationSets2 == null
            || supportedOperationSets2.size() < 1)
            throw new NullPointerException(
                "No OperationSet implementations are supported by "
                +"this implementation. ");

        //get the operation set presence here.
        opSetMultiChat2 =
            (OperationSetMultiUserChat)supportedOperationSets2.get(
                OperationSetMultiUserChat.class.getName());

        if (opSetMultiChat2 == null)
        {
            throw new NullPointerException(
                "No implementation for multi user chat was found");
        }

        opSetPresence2 =
            (OperationSetPresence) supportedOperationSets2.get(
                OperationSetPresence.class.getName());

        //if the op set is null show that we're not happy.
        if (opSetPresence2 == null)
        {
            throw new NullPointerException(
                "An implementation of the service must provide an "
                + "implementation of at least one of the PresenceOperationSets");
        }

    }

    /**
     * JUnit teardown method.
     *
     * @throws Exception in case anything goes wrong.
     */
    protected void tearDown() throws Exception
    {
        super.tearDown();
        fixture.tearDown();
    }

    /**
     * Creates a chat room and verifies that it has been properly created.
     *
     * @throws Exception if any Exception is thrown during the test.
     */
    public void testCreateChatRoom()
        throws Exception
    {
        ChatRoom chatRoom1
            = opSetMultiChat1.createChatRoom("mychatroom@conference.voipgw.u-strasbg.fr", new Hashtable());

        chatRoom1.join();
        try{ Thread.currentThread().wait(100000); }catch (InterruptedException ex){}
//        chatRoom1.
    }
}
