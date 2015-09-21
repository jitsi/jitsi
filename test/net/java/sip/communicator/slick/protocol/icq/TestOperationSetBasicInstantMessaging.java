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
package net.java.sip.communicator.slick.protocol.icq;

import java.net.*;
import java.util.*;

import junit.framework.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.Message;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;
import net.kano.joustsim.*;
import net.kano.joustsim.oscar.oscar.service.icbm.*;

/**
 * Performs testing of the basic instant messaging operation set. Tests include
 * going over basic functionality such as sending a message from the tested
 * implementation and asserting reception by the tester agent and vice versa.
 * @author Emil Ivov
 */
public class TestOperationSetBasicInstantMessaging
    extends TestCase
{
    private static final Logger logger =
        Logger.getLogger(TestOperationSetBasicInstantMessaging.class);

    private IcqSlickFixture fixture = new IcqSlickFixture();

    private OperationSetBasicInstantMessaging opSetBasicIM = null;
    private OperationSetPresence opSetPresence = null;

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

        Map<String, OperationSet> supportedOperationSets =
            fixture.provider.getSupportedOperationSets();

        if ( supportedOperationSets == null
            || supportedOperationSets.size() < 1)
            throw new NullPointerException(
                "No OperationSet implementations are supported by "
                +"this ICQ implementation. ");

        //get the operation set presence here.
        opSetBasicIM =
            (OperationSetBasicInstantMessaging)supportedOperationSets.get(
                OperationSetBasicInstantMessaging.class.getName());

        //if the op set is null then the implementation doesn't offer a typing.
        //operation set which is unacceptable for icq.
        if (opSetBasicIM == null)
        {
            throw new NullPointerException(
                "No implementation for basic IM was found");
        }

        //we also need the presence op set in order to retrieve contacts.
        opSetPresence =
            (OperationSetPresence)supportedOperationSets.get(
                OperationSetPresence.class.getName());

        //if the op set is null show that we're not happy.
        if (opSetPresence == null)
        {
            throw new NullPointerException(
                "An implementation of the ICQ service must provide an "
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
        TestSuite suite
            = new TestSuite(TestOperationSetBasicInstantMessaging.class);

        //the following 2 need to be run in the specified order.
        suite.addTest(new TestOperationSetBasicInstantMessaging(
                                "firstTestReceiveMessage"));
        suite.addTest(new TestOperationSetBasicInstantMessaging(
                                "thenTestSendMessage"));

        return suite;
    }


    /**
     * Send an instant message from the tested operation set and assert
     * reception by the icq tester agent.
     */
    public void firstTestReceiveMessage()
    {
        String body = "This is an IM coming from the tester agent"
            + " on " + new Date().toString();

        ImEventCollector evtCollector = new ImEventCollector();

        //add a msg listener and register to the op set and send an instant
        //msg from the tester agent.
        opSetBasicIM.addMessageListener(evtCollector);

        IcqSlickFixture.testerAgent.sendMessage(fixture.ourUserID, body);

        evtCollector.waitForEvent(10000);

        opSetBasicIM.removeMessageListener(evtCollector);

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
                     , IcqSlickFixture.testerAgent.getIcqUIN());

        //assert messageBody == body
        assertEquals("message body", body, evt.getSourceMessage().getContent());
    }

    /**
     * Send an instant message from the tester agent and assert reception by
     * the tested implementation
     */
    public void thenTestSendMessage()
    {
        String body = "This is an IM coming from the tested implementation"
            + " on " + new Date().toString();

        //create the message
        net.java.sip.communicator.service.protocol.Message msg
            = opSetBasicIM.createMessage(body);

        //register a listener in the op set
        ImEventCollector imEvtCollector = new ImEventCollector();
        opSetBasicIM.addMessageListener(imEvtCollector);

        //register a listener in the tester agent
        JoustSimMessageEventCollector jsEvtCollector
            = new JoustSimMessageEventCollector();
        IcqSlickFixture.testerAgent.addConversationListener( fixture.ourUserID
                                                     , jsEvtCollector);

        Contact testerAgentContact
            = opSetPresence.findContactByID(IcqSlickFixture.testerAgent.getIcqUIN());

        opSetBasicIM.sendInstantMessage(testerAgentContact, msg);

        imEvtCollector.waitForEvent(10000);
        jsEvtCollector.waitForEvent(10000);

        IcqSlickFixture.testerAgent.removeConversationListener( fixture.ourUserID
                                                        , jsEvtCollector);
        opSetBasicIM.removeMessageListener(imEvtCollector);

        //verify that the message delivered event was dispatched
        assertTrue( "No events delivered when sending a message"
                    , imEvtCollector.collectedEvents.size() > 0);

        assertTrue( "Received evt was not an instance of "
                    + MessageDeliveredEvent.class.getName()
                    , imEvtCollector.collectedEvents.get(0)
                                instanceof MessageDeliveredEvent);

        MessageDeliveredEvent evt
            = (MessageDeliveredEvent)imEvtCollector.collectedEvents.get(0);
        assertEquals("message destination "
                     , evt.getDestinationContact().getAddress()
                     , IcqSlickFixture.testerAgent.getIcqUIN());

        assertSame("source message", msg, evt.getSourceMessage());


        //verify that the message has successfully arived at the destination
        assertTrue( "No messages received by the tester agent"
                    , jsEvtCollector.collectedMessageInfo.size() > 0);
        String receivedBody = jsEvtCollector.collectedMessageInfo
                               .get(0).getMessage().getMessageBody();

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
            = opSetBasicIM.createMessage(body);

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
            = opSetBasicIM.createMessage(body);
        assertFalse("message uid", msg.getMessageUID().equals(
                                                        msg2.getMessageUID()));
    }

    /**
     * Creates an Message through the advance createMessage() method and
     * inspects its parameters.
     */
    public void testCreateMessage2()
    {
        String body = "This is an IM coming from the tested implementation"
            + " on " + new Date().toString();
        String contentType = "text/html";
        String encoding = "UTF-16";
        String subject = "test message";
        net.java.sip.communicator.service.protocol.Message msg =
            opSetBasicIM.createMessage(body, contentType, encoding, subject);

        assertEquals("message body", body, msg.getContent());
        assertTrue("message body bytes"
                   , Arrays.equals(body.getBytes(), msg.getRawData()));
        assertEquals("message length", body.length(), msg.getSize());
        assertEquals("message content type", contentType, msg.getContentType());
        assertEquals("message encoding", encoding, msg.getEncoding());
        assertNotNull("message uid", msg.getMessageUID());

        //a further test on message uid.
        net.java.sip.communicator.service.protocol.Message msg2
            = opSetBasicIM.createMessage(body);
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

    /**
     * The oscar.jar lib sends us typing events through this listener.
     */
    private class JoustSimMessageEventCollector
        implements ConversationListener
    {
        private List<MessageInfo> collectedMessageInfo = new LinkedList<MessageInfo>();

        /**
         * Adds <tt>minfo</tt> into the list of collected messages.
         * @param c Conversation
         * @param minfo MessageInfo
         */
        public void gotMessage(Conversation c, MessageInfo minfo)
        {

            logger.debug("Message: [" + minfo.getMessage()+ "] received from: "
                             + c.getBuddy());
                synchronized (this)
                {
                    collectedMessageInfo.add(minfo);
                    notifyAll();
                }

        }


        /**
         * Blocks until at least one event is received or until waitFor
         * miliseconds pass (whicever happens first).
         *
         * @param waitFor the number of miliseconds that we should be waiting
         * for an event before simply bailing out.
         */
        public void waitForEvent(long waitFor)
        {
            synchronized (this)
            {
                if (collectedMessageInfo.size() > 0)
                {
                    logger.trace("evt already received. "
                                 + collectedMessageInfo);
                    return;
                }

                try
                {
                    wait(waitFor);
                }
                catch (InterruptedException ex)
                {
                    logger.debug(
                        "Interrupted while waiting for a subscription evt", ex);
                }
            }
        }


        // The following methods only have dummy implementations here as they do
        // not interest us. Complete implementations are provided in the basic
        // instant messaging operation set.
        public void buddyInfoUpdated(IcbmService service, Screenname buddy,
                                     IcbmBuddyInfo info){}
        public void conversationClosed(Conversation c){}
        public void gotOtherEvent(Conversation conversation,
                                  ConversationEventInfo event){}
        public void sentOtherEvent(Conversation conversation,
                                   ConversationEventInfo event){}
        public void canSendMessageChanged(Conversation c, boolean canSend){}
        public void conversationOpened(Conversation c){}
        public void newConversation(IcbmService service, Conversation conv){}
        public void sentMessage(Conversation c, MessageInfo minfo){}
    }

    /**
     * A method that would simply send messages to a group of people so that
     * they would get notified that tests are being run.
     */
    public void testSendFunMessages()
    {
        String hostname = "";

        try{
            hostname = java.net.InetAddress.getLocalHost().getHostName() + ": ";
        }catch (UnknownHostException ex){}

        String message = hostname
                         + "Hello this is the SIP Communicator (version "
                         + System.getProperty("sip-communicator.version")
                         + ") build on: "
                         + new Date().toString()
                         + ". Have a very nice day!";

        String list = System.getProperty("accounts.reporting.ICQ_REPORT_LIST");

        logger.debug("Will send message " + message + " to: " + list);

        //if no property is specified - return
        if(list == null || list.trim().length() == 0)
            return;

        StringTokenizer tokenizer = new StringTokenizer(list, " ");

        while(tokenizer.hasMoreTokens())
        {
            IcqSlickFixture.testerAgent.sendMessage(tokenizer.nextToken(), message);
        }
    }

    /**
     * Tests whether there is a offline message received
     * and whether is the one we have send
     */
    public void testReceiveOfflineMessages()
    {
        String messageText =
            IcqSlickFixture.offlineMsgCollector.getMessageText();

        Message receiveMessage = IcqSlickFixture.offlineMsgCollector.getReceivedMessage();

        assertNotNull("No Offline messages have been received", receiveMessage);
        assertEquals("message body", messageText, receiveMessage.getContent());
    }

}
