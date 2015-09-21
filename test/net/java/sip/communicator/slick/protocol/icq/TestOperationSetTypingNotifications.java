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

import java.util.*;

import junit.framework.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;
import net.kano.joustsim.*;
import net.kano.joustsim.oscar.oscar.service.icbm.*;

/**
 * Tests functionality of the typing notifications operation set. All we do here
 * is assert that typing notifications sent from the tester agent result in
 * <tt>TypingNotificationEvent</tt>s and that typing notifications sent through
 * the tested operation set are received by the icq tester agent.
 * @author Emil Ivov
 */
public class TestOperationSetTypingNotifications
    extends TestCase
{
    private static final Logger logger =
        Logger.getLogger(TestOperationSetTypingNotifications.class);

    private IcqSlickFixture fixture = new IcqSlickFixture();
    private OperationSetTypingNotifications opSetTypingNotifs = null;
    private OperationSetPresence opSetPresence = null;

    public TestOperationSetTypingNotifications(String name)
    {
            super(name);
    }

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
        opSetTypingNotifs =
            (OperationSetTypingNotifications)supportedOperationSets.get(
                OperationSetTypingNotifications.class.getName());

        //if the op set is null then the implementation doesn't offer a typing.n
        //operation set which is unacceptable for icq.
        if (opSetTypingNotifs == null)
        {
            throw new NullPointerException(
                "No implementation for typing notifications was found");
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

        //the following 2 need to be run in the specified order.
        suite.addTest(new TestOperationSetTypingNotifications(
                                "firstTestTypingNotificationsEventDelivery"));
        suite.addTest(new TestOperationSetTypingNotifications(
                                "thenTestSendTypingNotification"));

        return suite;
    }

    /**
     * Sends a typing notification through the icq tester agent and verifies
     * whether it is properly received by the tested implementation
     */
    public void firstTestTypingNotificationsEventDelivery()
    {
        TypingEventCollector evtCollector = new TypingEventCollector();

        opSetTypingNotifs.addTypingNotificationsListener(evtCollector);

        IcqSlickFixture.testerAgent.sendTypingNotification(
            fixture.ourUserID, TypingState.TYPING);

        evtCollector.waitForEvent(10000);

        opSetTypingNotifs.removeTypingNotificationsListener(evtCollector);

        //check event dispatching
        assertTrue("Number of typing events received was zero."
            , evtCollector.collectedEvents.size() > 0);

        TypingNotificationEvent evt = (TypingNotificationEvent)evtCollector
                                                    .collectedEvents.get(0);

        assertEquals("Source of the typing notification event"
                     , IcqSlickFixture.testerAgent.getIcqUIN()
                     , evt.getSourceContact().getAddress() );

        assertEquals("Source of the typing notification event"
                     , OperationSetTypingNotifications.STATE_TYPING
                     , evt.getTypingState());

    }


    /**
     * Sends a typing notification through the tested implementation and
     * verifies whether it is properly received by the tester agent.
     */
    public void thenTestSendTypingNotification()
    {
        JoustSimTypingEventCollector evtCollector
            = new JoustSimTypingEventCollector();

        Contact contactToNotify = opSetPresence.findContactByID(
                                            IcqSlickFixture.testerAgent.getIcqUIN());

        IcqSlickFixture.testerAgent.addTypingStateInfoListenerForBuddy(
            contactToNotify.getAddress(), evtCollector);

        opSetTypingNotifs.sendTypingNotification(
            contactToNotify, OperationSetTypingNotifications.STATE_TYPING);

        IcqSlickFixture.testerAgent.removeTypingStateInfoListenerForBuddy(
            contactToNotify.getAddress(), evtCollector);
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

    /**
     * The oscar.jar lib sends us typing events through this listener.
     */
    private class JoustSimTypingEventCollector
        implements IcbmListener, TypingListener
    {
        private List<TypingInfo> collectedTypingInfo = new LinkedList<TypingInfo>();


        /**
         * Will register conversation and typing info into the corresponding
         * lists.
         *
         * @param conversation Conversation
         * @param typingInfo TypingInfo
         */
        public void gotTypingState(Conversation conversation,
                                   TypingInfo typingInfo)
        {
            logger.debug(conversation.getBuddy() + " sent typing info: "
                         + typingInfo.getTypingState());
            synchronized (this)
            {
                collectedTypingInfo.add(typingInfo);

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
                if (collectedTypingInfo.size() > 0)
                {
                    logger.trace("evt already received. "
                                 + collectedTypingInfo);
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


        //the follwoing methods only have dummy implementations here as they
        //do not interest us. complete implementatios are provider in the
        //basic instant messaging operation set.
        public void buddyInfoUpdated(IcbmService service, Screenname buddy,
                                     IcbmBuddyInfo info){}
        public void conversationClosed(Conversation c){}
        public void gotMessage(Conversation c, MessageInfo minfo){}
        public void gotOtherEvent(Conversation conversation,
                                  ConversationEventInfo event){}
        public void sentOtherEvent(Conversation conversation,
                                   ConversationEventInfo event){}
        public void canSendMessageChanged(Conversation c, boolean canSend){}
        public void conversationOpened(Conversation c){}
        public void newConversation(IcbmService service, Conversation conv){}
        public void sentMessage(Conversation c, MessageInfo minfo){}

        public void sendAutomaticallyFailed(
            IcbmService service,
            net.kano.joustsim.oscar.oscar.service.icbm.Message message,
            Set<Conversation> triedConversations)
        {
        }
    }
}
