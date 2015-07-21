/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.sip.communicator.slick.protocol.gibberish;

import java.io.*;
import java.util.*;

import junit.framework.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * Performs testing of the basic instant messaging operation set. Tests include
 * going over basic functionality such as sending a message from the tested
 * implementation and asserting reception by the tester agent and vice versa.
 *
 * @author Emil Ivov
 */
public class TestOperationSetBasicInstantMessaging
    extends TestCase
{
    private static final Logger logger =
        Logger.getLogger(TestOperationSetBasicInstantMessaging.class);

    private GibberishSlickFixture fixture = new GibberishSlickFixture();

    private OperationSetBasicInstantMessaging opSetBasicIM1 = null;
    private OperationSetBasicInstantMessaging opSetBasicIM2 = null;

    private OperationSetPresence opSetPresence1 = null;
    private OperationSetPresence opSetPresence2 = null;

    public TestOperationSetBasicInstantMessaging(String name)
    {
        super(name);
    }

    /**
     * Get a reference to the basic IM operation set.
     * @throws Exception if this is not a good day.
     */
    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        fixture.setUp();

        Map<String, OperationSet> supportedOperationSets1 =
            fixture.provider1.getSupportedOperationSets();

        if ( supportedOperationSets1 == null
            || supportedOperationSets1.size() < 1)
            throw new NullPointerException(
                "No OperationSet implementations are supported by "
                +"this implementation. ");

        //get the operation set presence here.
        opSetBasicIM1 =
            (OperationSetBasicInstantMessaging)supportedOperationSets1.get(
                OperationSetBasicInstantMessaging.class.getName());

        if (opSetBasicIM1 == null)
        {
            throw new NullPointerException(
                "No implementation for basic IM was found");
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

        Map<String, OperationSet> supportedOperationSets2 =
            fixture.provider2.getSupportedOperationSets();

        if ( supportedOperationSets2 == null
            || supportedOperationSets2.size() < 1)
            throw new NullPointerException(
                "No OperationSet implementations are supported by "
                +"this implementation. ");

        //get the operation set presence here.
        opSetBasicIM2 =
            (OperationSetBasicInstantMessaging)supportedOperationSets2.get(
                OperationSetBasicInstantMessaging.class.getName());

        if (opSetBasicIM2 == null)
        {
            throw new NullPointerException(
                "No implementation for basic IM was found");
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

    @Override
    protected void tearDown() throws Exception
    {
        super.tearDown();

        fixture.tearDown();
    }

    /**
     * Creates a test suite containing tests of this class in a specific order.
     * We'll first execute tests beginning with the "test" prefix and then go to
     * ordered tests.We first execture tests for receiving messagese, so that
     * a volatile contact is created for the sender. we'll then be able to
     * retrieve this volatile contact and send them a message on our turn.
     * We need to do things this way as the contact corresponding to the tester
     * agent has been removed in the previous test and we no longer have it
     * in our contact list.
     *
     * @return Test a testsuite containing all tests to execute.
     */
    public static Test suite()
    {
        TestSuite suite = new TestSuite();

        suite.addTest(new TestOperationSetBasicInstantMessaging(
                                "prepareContactList"));

        suite.addTestSuite(TestOperationSetBasicInstantMessaging.class);

        //the following 2 need to be run in the specified order.
        suite.addTest(new TestOperationSetBasicInstantMessaging(
                                "firstTestReceiveMessage"));
        suite.addTest(new TestOperationSetBasicInstantMessaging(
                                "thenTestSendMessage"));

        return suite;
    }

    /**
     * Create the list to be sure that contacts exchanging messages
     * exists in each other lists
     * @throws Exception
     */
    public void prepareContactList()
        throws Exception
    {
        fixture.clearProvidersLists();

        Object o = new Object();
        synchronized(o)
        {
            o.wait(2000);
        }

        try
        {
            opSetPresence1.subscribe(fixture.userID2);
        }
        catch (OperationFailedException ex)
        {
            // the contact already exist its OK
        }

        try
        {
            opSetPresence2.subscribe(fixture.userID1);
        }
        catch (OperationFailedException ex1)
        {
            // the contact already exist its OK
        }

        synchronized(o)
        {
            o.wait(2000);
        }
    }

    /**
     * Send an instant message from the tested operation set and assert
     * reception by the tester agent.
     */
    public void firstTestReceiveMessage()
    {
        String body = "This is an IM coming from the tester agent"
            + " on " + new Date().toString();

        ImEventCollector evtCollector = new ImEventCollector();

        //add a msg listener and register to the op set and send an instant
        //msg from the tester agent.
        opSetBasicIM1.addMessageListener(evtCollector);

        Contact testerAgentContact
            = opSetPresence2.findContactByID(fixture.userID1);

        logger.debug("Will send message " + body + " to: " + testerAgentContact);

        opSetBasicIM2.sendInstantMessage(testerAgentContact,
                                         opSetBasicIM2.createMessage(body));

        evtCollector.waitForEvent(10000);

        opSetBasicIM1.removeMessageListener(evtCollector);

        //assert reception of a message event
        assertTrue( "No events delivered upon a received message"
                    , evtCollector.collectedEvents.size() > 0);

        //assert event instance of Message Received Evt
        assertTrue( "Received evt was not an instance of "
                    + MessageReceivedEvent.class.getName()
                    , evtCollector.collectedEvents.get(0)
                                instanceof MessageReceivedEvent);

        //assert source contact == testAgent.uin
        MessageReceivedEvent evt
            = (MessageReceivedEvent)evtCollector.collectedEvents.get(0);
        assertEquals("message sender "
                     , evt.getSourceContact().getAddress()
                     , fixture.userID2);

        //assert messageBody == body
        assertEquals("message body", body, evt.getSourceMessage().getContent());
    }

    /**
     * Send an instant message from the tester agent and assert reception by
     * the tested implementation
     */
    public void thenTestSendMessage()
    {
        logger.debug("Printing Server Stored list to see if message fails are contacts in each other lists");
        ContactGroup rootGroup1
            = ((OperationSetPersistentPresence)opSetPresence1).
            getServerStoredContactListRoot();

        logger.debug("=========== Server Stored Contact List 1 =================");

        logger.debug("rootGroup="+rootGroup1.getGroupName()
                     +" rootGroup.childContacts="+rootGroup1.countContacts()
                     + "rootGroup.childGroups="+rootGroup1.countSubgroups()
                     + "Printing rootGroupContents=\n"+rootGroup1.toString());

        ContactGroup rootGroup2
            = ((OperationSetPersistentPresence)opSetPresence2).
            getServerStoredContactListRoot();

        logger.debug("=========== Server Stored Contact List 2 =================");

        logger.debug("rootGroup="+rootGroup2.getGroupName()
                     +" rootGroup.childContacts="+rootGroup2.countContacts()
                     + "rootGroup.childGroups="+rootGroup2.countSubgroups()
                     + "Printing rootGroupContents=\n"+rootGroup2.toString());

        String body = "This is an IM coming from the tested implementation"
            + " on " + new Date().toString();

        //create the message
        net.java.sip.communicator.service.protocol.Message msg
            = opSetBasicIM1.createMessage(body);

        //register a listener in the op set
        ImEventCollector imEvtCollector1 = new ImEventCollector();
        opSetBasicIM1.addMessageListener(imEvtCollector1);

        //register a listener in the tester agent
        ImEventCollector imEvtCollector2 = new ImEventCollector();
        opSetBasicIM2.addMessageListener(imEvtCollector2);

        Contact testerAgentContact
            = opSetPresence1.findContactByID(fixture.userID2);

        opSetBasicIM1.sendInstantMessage(testerAgentContact, msg);

        imEvtCollector1.waitForEvent(10000);
        imEvtCollector2.waitForEvent(10000);

        opSetBasicIM1.removeMessageListener(imEvtCollector1);
        opSetBasicIM2.removeMessageListener(imEvtCollector2);

        //verify that the message delivered event was dispatched
        assertTrue( "No events delivered when sending a message"
                    , imEvtCollector1.collectedEvents.size() > 0);

        assertTrue( "Received evt was not an instance of "
                    + MessageDeliveredEvent.class.getName()
                    , imEvtCollector1.collectedEvents.get(0)
                                instanceof MessageDeliveredEvent);

        MessageDeliveredEvent evt
            = (MessageDeliveredEvent)imEvtCollector1.collectedEvents.get(0);
        assertEquals("message destination "
                     , evt.getDestinationContact().getAddress()
                     , fixture.userID2);

        assertSame("source message", msg, evt.getSourceMessage());


        //verify that the message has successfully arived at the destination
        assertTrue( "No messages received by the tester agent"
                    , imEvtCollector2.collectedEvents.size() > 0);

        assertFalse( "Message was unable to deliver !",
            imEvtCollector2.collectedEvents.get(0)
            instanceof MessageDeliveryFailedEvent);

        String receivedBody =
            ((MessageReceivedEvent)imEvtCollector2.collectedEvents
                               .get(0)).getSourceMessage().getContent();

        assertEquals("received message body", msg.getContent(), receivedBody);
    }

    /**
     * Creates an Message through the simple createMessage() method and inspects
     * its parameters.
     */
    public void testCreateMessage1()
    {
        String body = "This is an IM coming from the tested implementation"
            + " on " + new Date().toString();
        net.java.sip.communicator.service.protocol.Message msg
            = opSetBasicIM1.createMessage(body);

        assertEquals("message body", body, msg.getContent());
        assertTrue("message body bytes"
                   , Arrays.equals(body.getBytes(), msg.getRawData()));
        assertEquals("message length", body.length(), msg.getSize());
        assertEquals("message content type"
                     , OperationSetBasicInstantMessaging.DEFAULT_MIME_TYPE
                     , msg.getContentType());

        assertEquals("message encoding"
                     , OperationSetBasicInstantMessaging.DEFAULT_MIME_ENCODING
                     , msg.getEncoding());

        assertNotNull("message uid", msg.getMessageUID());

        //a further test on message uid.
        net.java.sip.communicator.service.protocol.Message msg2
            = opSetBasicIM1.createMessage(body);
        assertFalse("message uid", msg.getMessageUID().equals(
                                                        msg2.getMessageUID()));
    }

    /**
     * Creates an Message through the advance createMessage() method and
     * inspects its parameters.
     */
    public void testCreateMessage2()
        throws UnsupportedEncodingException
    {
        String body = "This is an IM coming from the tested implementation"
            + " on " + new Date().toString();
        String contentType = "text/html";
        String encoding = "UTF-16";
        String subject = "test message";
        net.java.sip.communicator.service.protocol.Message msg =
            opSetBasicIM1.createMessage(body, contentType, encoding, subject);
        byte[] bodyBytes = body.getBytes(encoding);

        assertEquals("message body", body, msg.getContent());
        assertTrue("message body bytes"
                   , Arrays.equals(bodyBytes, msg.getRawData()));
        assertEquals("message length", bodyBytes.length, msg.getSize());
        assertEquals("message content type", contentType, msg.getContentType());
        assertEquals("message encoding", encoding, msg.getEncoding());
        assertNotNull("message uid", msg.getMessageUID());

        //a further test on message uid.
        net.java.sip.communicator.service.protocol.Message msg2
            = opSetBasicIM1.createMessage(body);
        assertFalse("message uid", msg.getMessageUID().equals(
            msg2.getMessageUID()));
    }

    /**
     * Collects instant messaging events.
     */
    private class ImEventCollector implements MessageListener
    {
        private List<EventObject> collectedEvents = new LinkedList<EventObject>();
        /**
         * Called when a new incoming <tt>Message</tt> has been received.
         * @param evt the <tt>MessageReceivedEvent</tt> containing the newly
         * received message, its sender and other details.
         */
        public void messageReceived(MessageReceivedEvent evt)
        {
            logger.debug("Received a MessageReceivedEvent: " + evt);

            synchronized(this)
            {
                collectedEvents.add(evt);
                notifyAll();
            }
        }

        /**
         * Called to indicated that delivery of a message sent earlier has failed.
         * Reason code and phrase are contained by the <tt>MessageFailedEvent</tt>
         * @param evt the <tt>MessageFailedEvent</tt> containing the ID of the
         * message whose delivery has failed.
         */
        public void messageDeliveryFailed(MessageDeliveryFailedEvent evt)
        {
            logger.debug("Received a MessageDeliveryFailedEvent: " + evt);

            synchronized(this)
            {
                collectedEvents.add(evt);
                notifyAll();
            }
        }


        /**
         * Called when the underlying implementation has received an indication
         * that a message, sent earlier has been successfully received by the
         * destination.
         * @param evt the MessageDeliveredEvent containing the id of the message
         * that has caused the event.
         */
        public void messageDelivered(MessageDeliveredEvent evt)
        {
            logger.debug("Received a MessageDeliveredEvent: " + evt);

            synchronized(this)
            {
                collectedEvents.add(evt);
                notifyAll();
            }
        }

        /**
         * Blocks until at least one event is received or until waitFor
         * miliseconds pass (whichever happens first).
         *
         * @param waitFor the number of miliseconds that we should be waiting
         * for an event before simply bailing out.
         */
        public void waitForEvent(long waitFor)
        {
            synchronized(this)
            {

                if(collectedEvents.size() > 0)
                    return;

                try{
                    wait(waitFor);
                }
                catch (InterruptedException ex)
                {
                    logger.debug(
                        "Interrupted while waiting for a message evt", ex);
                }
            }
        }
    }
}
