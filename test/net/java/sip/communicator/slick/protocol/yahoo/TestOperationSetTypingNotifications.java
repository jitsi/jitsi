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
package net.java.sip.communicator.slick.protocol.yahoo;

import java.util.*;

import junit.framework.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * Tests functionality of the typing notifications operation set.
 *
 * @author Damian Minkov
 */
public class TestOperationSetTypingNotifications
    extends TestCase
{
    private static final Logger logger =
        Logger.getLogger(TestOperationSetTypingNotifications.class);

    private YahooSlickFixture fixture = new YahooSlickFixture();
    private OperationSetTypingNotifications opSetTypingNotifs1 = null;
    private OperationSetPresence opSetPresence1 = null;
    private OperationSetTypingNotifications opSetTypingNotifs2 = null;
    private OperationSetPresence opSetPresence2 = null;

    private OperationSetBasicInstantMessaging opSetBasicIM1 = null;
    private OperationSetBasicInstantMessaging opSetBasicIM2 = null;


    public TestOperationSetTypingNotifications(String name)
    {
            super(name);
    }

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
        opSetTypingNotifs1 =
            (OperationSetTypingNotifications)supportedOperationSets1.get(
                OperationSetTypingNotifications.class.getName());

        //if the op set is null then the implementation doesn't offer a typing.n
        //operation set which is unacceptable.
        if (opSetTypingNotifs1 == null)
        {
            throw new NullPointerException(
                "No implementation for typing notifications was found");
        }

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
        opSetTypingNotifs2 =
            (OperationSetTypingNotifications)supportedOperationSets2.get(
                OperationSetTypingNotifications.class.getName());

        //if the op set is null then the implementation doesn't offer a typing.n
        //operation set which is unacceptable for.
        if (opSetTypingNotifs2 == null)
        {
            throw new NullPointerException(
                "No implementation for typing notifications was found");
        }

        opSetBasicIM2 =
            (OperationSetBasicInstantMessaging)supportedOperationSets2.get(
                OperationSetBasicInstantMessaging.class.getName());

        if (opSetBasicIM2 == null)
        {
            throw new NullPointerException(
                "No implementation for basic IM was found");
        }


        //we also need the presence op set in order to retrieve contacts.
        opSetPresence2 =
            (OperationSetPresence)supportedOperationSets2.get(
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
     * Create the list to be sure that contacts exchanging messages
     * exists in each other lists
     * @throws Exception
     */
    public void prepareContactList() throws Exception
    {
        // be sure that contacts are in their lists
        try{
            opSetPresence1.subscribe(fixture.userID2);
        }
        catch (OperationFailedException ex){
            // the contact already exist its OK
        }

        try{
            opSetPresence2.subscribe(fixture.userID1);
        }
        catch (OperationFailedException ex1){
            // the contact already exist its OK
        }

        Object o = new Object();
        synchronized (o)
        {
            o.wait(2000);
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
        TestSuite suite = new TestSuite();

        suite.addTest(new TestOperationSetTypingNotifications(
                                "prepareContactList"));

        //the following 2 need to be run in the specified order.
        suite.addTest(new TestOperationSetTypingNotifications(
                                "testTypingNotificationsEventDelivery"));
        return suite;
    }

    /**
     * Sends a typing notification and verifies
     * whether it is properly received by the tested implementation
     */
    public void testTypingNotificationsEventDelivery()
    {
        TypingEventCollector evtCollector = new TypingEventCollector();

        // send message so request for receiving notifications also to be set
        Contact notifingContact =
            opSetPresence1.findContactByID(fixture.userID2);
        opSetBasicIM1.sendInstantMessage(notifingContact,
                                         opSetBasicIM1.createMessage("ping"));

        opSetTypingNotifs1.addTypingNotificationsListener(evtCollector);

        Contact contactToNotify =
            opSetPresence2.findContactByID(fixture.userID1);

        opSetBasicIM2.sendInstantMessage(contactToNotify,
                                         opSetBasicIM2.createMessage("pong"));

        Object w = new Object();
        synchronized(w){try {w.wait(500);}catch (Exception e) {}}

        opSetTypingNotifs2.sendTypingNotification(
            contactToNotify, OperationSetTypingNotifications.STATE_TYPING);

        evtCollector.waitForEvent(10000);

        opSetTypingNotifs1.removeTypingNotificationsListener(evtCollector);

        //check event dispatching
        assertTrue("Number of typing events received was zero."
            , evtCollector.collectedEvents.size() > 0);

        TypingNotificationEvent evt = (TypingNotificationEvent)evtCollector
                                                    .collectedEvents.get(0);

        assertEquals("Source of the typing notification event"
                     , fixture.userID2
                     , evt.getSourceContact().getAddress() );

        assertEquals("Source of the typing notification event"
                     , OperationSetTypingNotifications.STATE_TYPING
                     , evt.getTypingState());
    }

    /**
     * Simply collects allre received events and provides a mechanisim for
     * waiting for the next event.
     */
    private class TypingEventCollector implements TypingNotificationsListener
    {
        private List<EventObject> collectedEvents = new LinkedList<EventObject>();
        /**
         * Called to indicate that a remote <tt>Contact</tt> has sent us a typing
         * notification. The method adds the <tt>event</tt> to the list of
         * captured events.
         * @param event a <tt>TypingNotificationEvent</tt> containing the sender
         * of the notification and its type.
         */
        public void typingNotificationReceived(TypingNotificationEvent event)
        {
            logger.debug("Received a typing notification: " + event);
            synchronized (this)
            {
                collectedEvents.add(event);
                notifyAll();
            }
        }

        /**
         * Called to indicate that sending typing notification has failed.
         *
         * @param event a <tt>TypingNotificationEvent</tt> containing the sender
         * of the notification and its type.
         */
        public void typingNotificationDeliveryFailed(TypingNotificationEvent event)
        {}

        /**
         * Blocks until at least one event is received or until waitFor
         * miliseconds pass (whicever happens first).
         *
         * @param waitFor the number of miliseconds that we should be waiting
         * for an event before simply bailing out.
         */
        public void waitForEvent(long waitFor)
        {
            synchronized(this){

                if(collectedEvents.size() > 0)
                    return;

                try{
                    wait(waitFor);
                }
                catch (InterruptedException ex){
                    logger.debug(
                        "Interrupted while waiting for a subscription evt", ex);
                }
            }
        }
    }
}
