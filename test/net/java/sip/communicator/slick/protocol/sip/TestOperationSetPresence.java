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
package net.java.sip.communicator.slick.protocol.sip;

import java.beans.*;
import java.util.*;

import junit.framework.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * Tests SIP implementations of a Presence Operation Set. Tests in this class
 * verify functionality such as: Changing local (our own) status and
 * corresponding event dispatching; Querying status of contacts, Subscribing
 * for presence notifications upong status changes of specific contacts.
 * <p>
 * Using a custom suite() method, we make sure that apart from standard test
 * methods (those with a <tt>test</tt> prefix) we also execute those that
 * we want run in a specific order like for example - postTestSubscribe() and
 * postTestUnsubscribe().
 * <p>
 *
 * @author Benoit Pradelle
 */
public class TestOperationSetPresence
    extends TestCase
{
    private static final Logger logger =
        Logger.getLogger(TestOperationSetPresence.class);

    private SipSlickFixture fixture = new SipSlickFixture();
    private OperationSetPresence operationSetPresence1 = null;
    private OperationSetPresence operationSetPresence2 = null;
    private String statusMessageRoot = new String("Our status is now: ");

    public TestOperationSetPresence(String name)
    {
        super(name);
    }

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        this.fixture.setUp();

        Map<String, OperationSet> supportedOperationSets1 =
            this.fixture.provider1.getSupportedOperationSets();

        if ( supportedOperationSets1 == null
            || supportedOperationSets1.size() < 1)
            throw new NullPointerException(
                "No OperationSet implementations are supported by "
                +"this implementation. ");

        //get the operation set presence here.
        this.operationSetPresence1 =
            (OperationSetPresence)supportedOperationSets1.get(
                OperationSetPresence.class.getName());

        //if the op set is null then the implementation doesn't offer a presence
        //operation set which is unacceptable for sip.
        if (this.operationSetPresence1 == null)
        {
            throw new NullPointerException(
                "An implementation of the Jabber service must provide an "
                + "implementation of at least the one of the Presence "
                + "Operation Sets");
        }

        // do it once again for the second provider
        Map<String, OperationSet> supportedOperationSets2 =
            this.fixture.provider2.getSupportedOperationSets();

        if (supportedOperationSets2 == null
                || supportedOperationSets2.size() < 1)
            throw new NullPointerException(
                "No OperationSet implementations are supported by "
                +"this SIP implementation. ");

        //get the operation set presence here.
        this.operationSetPresence2 =
            (OperationSetPresence)supportedOperationSets2.get(
                OperationSetPresence.class.getName());

        //if the op set is null then the implementation doesn't offer a presence
        //operation set which is unacceptable for SIP.
        if (this.operationSetPresence2 == null)
        {
            throw new NullPointerException(
                "An implementation of the SIP service must provide an "
                + "implementation of at least the one of the Presence "
                + "Operation Sets");
        }
    }

    @Override
    protected void tearDown() throws Exception
    {
        super.tearDown();

        this.fixture.tearDown();
    }

    /**
     * Creates a test suite containing all tests of this class followed by
     * test methods that we want executed in a specified order.
     * @return Test
     */
    public static Test suite()
    {
        //return an (almost) empty suite if we're running in offline mode.
        if(SipSlickFixture.onlineTestingDisabled)
        {
            TestSuite suite = new TestSuite();
            //currently no tests without online connection
            return suite;
        }

        TestSuite suite = new TestSuite();

        // clear the lists before subscribing users
        suite.addTest(new TestOperationSetPresence("clearLists"));

        // first postTestSubscribe. to be sure that contacts are in the
        // list so we can further continue and test presences each other
        suite.addTest(new TestOperationSetPresence("postTestSubscribe"));

        // add other tests
        suite.addTestSuite(TestOperationSetPresence.class);

        // now test unsubscribe
        suite.addTest(new TestOperationSetPresence("postTestUnsubscribe"));

        return suite;
    }

    /**
     * Verify that changing state to all supported statuses works as expected.
     *
     * @throws Exception in case a failure occurs while the operation set
     * is switching to the new state.
     */
    public void testChangingState() throws Exception
    {
        //first create a local list containing the presence status instances
        //supported by the underlying implementation.
        Iterator<PresenceStatus> supportedStatusSetIter =
            this.operationSetPresence1.getSupportedStatusSet();

        while (supportedStatusSetIter.hasNext())
        {
            PresenceStatus supportedStatus = supportedStatusSetIter.next();

            logger.trace("Will test a transition to "
                         + supportedStatus.getStatusName());

            subtestStateTransition(supportedStatus);
        }
    }

    /**
     * Used by methods testing state transitions
     *
     * @param newStatus the PresenceStatus field corresponding to the status
     * that we'd like the opeation set to enter.
     *
     * @throws Exception in case changing the state causes an exception
     */
    public void subtestStateTransition(PresenceStatus newStatus)
        throws Exception
    {
        logger.trace(" --=== beginning state transition test ===--");

        PresenceStatus oldStatus =
            this.operationSetPresence2.getPresenceStatus();
        String oldStatusMessage =
            this.operationSetPresence2.getCurrentStatusMessage();
        String newStatusMessage =
            this.statusMessageRoot + newStatus;

        logger.debug(   "old status is=" + oldStatus.getStatusName()
                     + " new status=" + newStatus.getStatusName());

        //First register a listener to make sure that all corresponding
        //events have been generated.
        PresenceStatusEventCollector statusEventCollector
            = new PresenceStatusEventCollector();
        this.operationSetPresence2.addProviderPresenceStatusListener(
            statusEventCollector);

        //change the status
        this.operationSetPresence2.publishPresenceStatus(newStatus,
                                                         newStatusMessage);
        pauseAfterStateChanges();

        //test event notification.
        statusEventCollector.waitForPresEvent(10000);
        statusEventCollector.waitForStatMsgEvent(10000);

        this.operationSetPresence2.removeProviderPresenceStatusListener(
            statusEventCollector);

        assertEquals("Events dispatched during an event transition.",
                     1, statusEventCollector.collectedPresEvents.size());
        assertEquals("A status changed event contained wrong old status.",
                     oldStatus,
                     ((ProviderPresenceStatusChangeEvent)
                        statusEventCollector.collectedPresEvents.get(0))
                            .getOldStatus());
        assertEquals("A status changed event contained wrong new status.",
                     newStatus,
                     ((ProviderPresenceStatusChangeEvent)
                        statusEventCollector.collectedPresEvents.get(0))
                            .getNewStatus());

        // verify that the operation set itself is aware of the status change
        assertEquals("opSet.getPresenceStatus() did not return properly.",
            newStatus,
            this.operationSetPresence2.getPresenceStatus());

        // Will wait for the status to be received before quering for it
        Object lock = new Object();
        synchronized(lock)
        {
            logger.trace("Will wait status to be received from the other side!");
            lock.wait(5000);
        }

        PresenceStatus actualStatus =
            this.operationSetPresence1.queryContactStatus(this.fixture.userID2);

        // in case of switching to the OFFLINE state, the contact will appear
        // in the unknown state, not offline
        if (newStatus.getStatus() != 0) {
            assertEquals("The underlying implementation did not switch to the "
                     +"requested presence status.",
                     newStatus,
                     actualStatus);
        } else {
            if (newStatus.getStatus() != 0 && newStatus.getStatus() != 1) {
                fail("The underlying implementation did not switch to the "
                        +"requested presence status.");
            }
        }

        //check whether the server returned the status message that we've set.
        assertEquals("No status message event.",
                     1, statusEventCollector.collectedStatMsgEvents.size());
        assertEquals("A status message event contained wrong old value.",
                     oldStatusMessage,
                     ((PropertyChangeEvent)
                        statusEventCollector.collectedStatMsgEvents.get(0))
                            .getOldValue());
        assertEquals("A status message event contained wrong new value.",
                     newStatusMessage,
                     ((PropertyChangeEvent)
                        statusEventCollector.collectedStatMsgEvents.get(0))
                            .getNewValue());

        // verify that the operation set itself is aware of the new status msg.
        assertEquals("opSet.getCurrentStatusMessage() did not return properly.",
            newStatusMessage,
            this.operationSetPresence2.getCurrentStatusMessage());

        logger.trace(" --=== finished test ===--");
    }

    /**
     * Give time changes to take effect
     */
    private void pauseAfterStateChanges()
    {
        try
        {
            Thread.sleep(2000);
        }
        catch (InterruptedException ex)
        {
            logger.debug("Pausing between state changes was interrupted", ex);
        }
    }

    /**
     * Verifies that querying status works fine. The tester agent would
     * change status and the operation set would have to return the right status
     * after every change.
     *
     * @throws java.lang.Exception if one of the transitions fails
     */
    public void disabled_testQueryContactStatus()
        throws Exception
    {
        //first create a local list containing the presence status instances
        //supported by the underlying implementation.
        Iterator<PresenceStatus> supportedStatusSetIter =
            operationSetPresence1.getSupportedStatusSet();

        while (supportedStatusSetIter.hasNext())
        {
            PresenceStatus supportedStatus = supportedStatusSetIter.next();

            logger.trace("Will test a transition to "
                         + supportedStatus.getStatusName());

            subtestQueryContactStatus(supportedStatus);
        }
    }

    /**
     * Used by functions testing the queryContactStatus method of the
     * presence operation set.
     * @param status the status as specified, that
     * the tester agent should switch to.
     *
     * @throws java.lang.Exception if querying the status causes some exception.
     */
    public void subtestQueryContactStatus(PresenceStatus status)
        throws Exception
    {
        this.operationSetPresence2.publishPresenceStatus(status,
                "status message");

        pauseAfterStateChanges();

        PresenceStatus actualReturn
            = this.operationSetPresence1.queryContactStatus(
                    this.fixture.userID2);
        // in the case of setting the contact offline, it may appear in the
        // UNKNOWN state until the next poll
        if (status.getStatus() != 0) {
            assertEquals("Querying a "
                     + status.getStatusName()
                     + " state did not return as expected"
                     , status, actualReturn);
        } else {
            if (actualReturn.getStatus() != 0 && actualReturn.getStatus() != 1)
            {
                fail("Querying a " + status.getStatusName()
                     + " state did not return as expected");
            }
        }
    }

    /**
     * The method would add a subscription for a contact, wait for a
     * subscription event confirming the subscription, then change the status
     * of the newly added contact (which is actually the testerAgent) and
     * make sure that the corresponding notification events have been generated.
     *
     * @throws java.lang.Exception if an exception occurs during testing.
     */
    public void postTestSubscribe()
        throws Exception
    {
        logger.debug("Testing Subscription and Subscription Event Dispatch.");
         this.operationSetPresence2.subscribe(this.fixture.userID1);
        SubscriptionEventCollector subEvtCollector
            = new SubscriptionEventCollector();
        this.operationSetPresence1.addSubscriptionListener(subEvtCollector);

        synchronized (subEvtCollector)
        {
            this.operationSetPresence1.subscribe(this.fixture.userID2);
            //we may already have the event, but it won't hurt to check.
            subEvtCollector.waitForEvent(10000);
        }

        assertEquals("Subscription event dispatching failed.",
                     1, subEvtCollector.collectedEvents.size());
        SubscriptionEvent subEvt =
            (SubscriptionEvent)subEvtCollector.collectedEvents.get(0);

        assertEquals("SubscriptionEvent Source:",
                this.fixture.userID2,
                     ((Contact)subEvt.getSource()).getAddress());
        assertEquals("SubscriptionEvent Source Contact:",
                this.fixture.userID2,
                     subEvt.getSourceContact().getAddress());
        assertSame("SubscriptionEvent Source Provider:",
                this.fixture.provider1,
                     subEvt.getSourceProvider());

        subEvtCollector.collectedEvents.clear();

        // wait the resolution of the contact before continuing
        synchronized (subEvtCollector)
        {
            subEvtCollector.waitForEvent(10000);
            this.operationSetPresence1
                .removeSubscriptionListener(subEvtCollector);
        }

        subEvtCollector.collectedEvents.clear();

        // wait to be sure that every responses for the subscribe have been
        // received
        Object lock = new Object();
        synchronized(lock)
        {
            logger.info("Will wait all subscription events to be received");
            lock.wait(10000);
        }

        // make the user agent tester change its states and make sure we are
        // notified
        logger.debug("Testing presence notifications.");
        PresenceStatus oldStatus =
            this.operationSetPresence2.getPresenceStatus();

        PresenceStatus newStatus = getSampleStatus1();

        //in case we are by any chance already in that status, we'll
        //be changing to something else
        if(oldStatus.equals(newStatus)){
            newStatus = getSampleStatus2();
        }

        logger.debug("trying to set status " + newStatus + " for contact 2");

        //now do the actual status notification testing
        ContactPresenceEventCollector contactPresEvtCollector
            = new ContactPresenceEventCollector(this.fixture.userID2,
                    newStatus);
        this.operationSetPresence1.addContactPresenceStatusListener(
            contactPresEvtCollector);

        synchronized (contactPresEvtCollector){
            this.operationSetPresence2.publishPresenceStatus(newStatus,
                    "new status");
            // we may already have the event, but it won't hurt to check.
            contactPresEvtCollector.waitForEvent(10000);
            this.operationSetPresence1
                .removeContactPresenceStatusListener(contactPresEvtCollector);
        }

        assertEquals("Presence Notif. event dispatching failed.",
                     1, contactPresEvtCollector.collectedEvents.size());
        ContactPresenceStatusChangeEvent presEvt =
            (ContactPresenceStatusChangeEvent)
                contactPresEvtCollector.collectedEvents.get(0);

        assertEquals("Presence Notif. event  Source:",
                this.fixture.userID2,
                     ((Contact)presEvt.getSource()).getAddress());
        assertEquals("Presence Notif. event  Source Contact:",
                this.fixture.userID2,
                     presEvt.getSourceContact().getAddress());
        assertSame("Presence Notif. event  Source Provider:",
                this.fixture.provider1,
                     presEvt.getSourceProvider());

        PresenceStatus reportedNewStatus = presEvt.getNewStatus();
        PresenceStatus reportedOldStatus = presEvt.getOldStatus();

        assertEquals( "Reported new PresenceStatus: ",
                      newStatus, reportedNewStatus );

        //don't require equality between the reported old PresenceStatus and
        //the actual presence status of the tester agent because a first
        //notification is not supposed to have the old status as it really was.
        assertNotNull( "Reported old PresenceStatus: ", reportedOldStatus );

//        try
//        {
//            // add the the user to the reverse side needed for status tests
//            subEvtCollector.collectedEvents.clear();
//            this.operationSetPresence2.addSubscriptionListener(subEvtCollector);
//
//            synchronized (subEvtCollector)
//            {
//                this.operationSetPresence2.subscribe(this.fixture.userID1);
//                //we may already have the event, but it won't hurt to check.
//                subEvtCollector.waitForEvent(10000);
//
//                // wait the resolved event
//                subEvtCollector.collectedEvents.clear();
//                subEvtCollector.waitForEvent(10000);
//
//                this.operationSetPresence2.removeSubscriptionListener(
//                    subEvtCollector);
//            }
//        }
//        catch (OperationFailedException ex)
//        {
//            // happens if the user is already subscribed
//        }
        synchronized(lock)
        {
            logger.info("Will wait all subscription events to be received by" +
                    " lib");
            lock.wait(3000);
            logger.info("Stopped waiting");
        }
    }

    /**
     * Returns the online status with a highest connectivity index.
     *
     * @return the online <tt>PresenceStatus</tt> with a highest connectivity
     * index.
     */
    private PresenceStatus getSampleStatus1()
    {
        //iterate through all supported statuses and return the one with the
        //highest connectivity index.
        PresenceStatus mostConnectedPresenceStatus = null;
        int mostConnectedPresenceStatusInt = Integer.MIN_VALUE;

        Iterator<PresenceStatus> supportedStatusSetIter =
            this.operationSetPresence1.getSupportedStatusSet();

        while (supportedStatusSetIter.hasNext())
        {
            PresenceStatus supportedStatus = supportedStatusSetIter.next();

            if(supportedStatus.getStatus() > mostConnectedPresenceStatusInt)
            {
                mostConnectedPresenceStatusInt = supportedStatus.getStatus();
                mostConnectedPresenceStatus = supportedStatus;
            }
        }

        return mostConnectedPresenceStatus;
    }

    /**
     * Returns the online status with the second highest connectivity index.
     *
     * @return the online <tt>PresenceStatus</tt> with a lowest connectivity
     * index.
     */
    private PresenceStatus getSampleStatus2()
    {
        //iterate through all supported statuses and return the one with the
        //highest connectivity index.
        int mostConnectedPresenceStatusInt = Integer.MIN_VALUE;
        PresenceStatus secondMostConnectedPresenceStatus = null;
        int secondMostConnectedPresenceStatusInt = Integer.MIN_VALUE;

        Iterator<PresenceStatus> supportedStatusSetIter =
            this.operationSetPresence1.getSupportedStatusSet();

        while (supportedStatusSetIter.hasNext())
        {
            PresenceStatus supportedStatus = supportedStatusSetIter.next();

            if(supportedStatus.getStatus() > mostConnectedPresenceStatusInt)
            {
                mostConnectedPresenceStatusInt = supportedStatus.getStatus();
            } else if(supportedStatus.getStatus() >
                                        secondMostConnectedPresenceStatusInt)
            {
                secondMostConnectedPresenceStatus = supportedStatus;
                secondMostConnectedPresenceStatusInt =
                    supportedStatus.getStatus();
            }
        }

        return secondMostConnectedPresenceStatus;
    }

    /**
     * We unsubscribe from presence notification deliveries concerning
     * testerAgent's presence status and verify that we receive the
     * subscription removed event. We then make the tester agent change status
     * and make sure that no notifications are delivered.
     *
     * @throws java.lang.Exception in case unsubscribing fails.
     */
    public void postTestUnsubscribe()
        throws Exception
    {
        logger.debug("Testing Unsubscribe and unsubscription event dispatch.");

        // First create a subscription and verify that it really gets created.
        SubscriptionEventCollector subEvtCollector
            = new SubscriptionEventCollector();
        this.operationSetPresence1.addSubscriptionListener(subEvtCollector);

        Contact sipTesterAgentContact = this.operationSetPresence1
            .findContactByID(this.fixture.userID2);

        assertNotNull(
            "Failed to find an existing subscription for the tester agent",
            sipTesterAgentContact);

        synchronized(subEvtCollector){
            this.operationSetPresence1.unsubscribe(sipTesterAgentContact);
            subEvtCollector.waitForEvent(10000);
            //don't want any more events
            this.operationSetPresence1.removeSubscriptionListener(
                    subEvtCollector);
        }

        assertEquals("Subscription event dispatching failed.",
                     1, subEvtCollector.collectedEvents.size());
        SubscriptionEvent subEvt =
            (SubscriptionEvent) subEvtCollector.collectedEvents.get(0);

        assertEquals("SubscriptionEvent Source:",
                     sipTesterAgentContact, subEvt.getSource());

        assertEquals("SubscriptionEvent Source Contact:",
                     sipTesterAgentContact, subEvt.getSourceContact());

        assertSame("SubscriptionEvent Source Provider:",
                this.fixture.provider1,
                subEvt.getSourceProvider());

        subEvtCollector.collectedEvents.clear();

        // make the user agent tester change its states and make sure we don't
        // get notifications as we're now unsubscribed.
        logger.debug("Testing (lack of) presence notifications.");
        PresenceStatus oldStatus =
            this.operationSetPresence2.getPresenceStatus();

        PresenceStatus newStatus = getSampleStatus1();

        //in case we are by any chance already in that status, we'll
        //be changing to something else
        if(oldStatus.equals(newStatus)){
            newStatus = getSampleStatus2();
        }

        //now do the actual status notification testing
        ContactPresenceEventCollector contactPresEvtCollector
            = new ContactPresenceEventCollector(this.fixture.userID2, null);
        this.operationSetPresence1.addContactPresenceStatusListener(
            contactPresEvtCollector);

        synchronized (contactPresEvtCollector){
            this.operationSetPresence2.publishPresenceStatus(newStatus,
                    "new status");

            //we may already have the event, but it won't hurt to check.
            contactPresEvtCollector.waitForEvent(10000);
            this.operationSetPresence1
                .removeContactPresenceStatusListener(contactPresEvtCollector);
        }

        assertEquals("Presence Notifications were received after unsubscibing.",
                     0, contactPresEvtCollector.collectedEvents.size());
    }

    public void clearLists()
        throws Exception
    {
        logger.debug("Clear the two lists before tests");

        this.fixture.clearProvidersLists();

        Object o = new Object();
        synchronized(o)
        {
            o.wait(3000);
        }
    }

    /**
     * An event collector that would collect all events generated by a
     * provider after a status change. The collector would also do a notifyAll
     * every time it receives an event.
     */
    private class PresenceStatusEventCollector
        implements ProviderPresenceStatusListener
    {
        public ArrayList<EventObject> collectedPresEvents = new ArrayList<EventObject>();
        public ArrayList<EventObject> collectedStatMsgEvents = new ArrayList<EventObject>();

        public void providerStatusChanged(ProviderPresenceStatusChangeEvent evt)
        {
            synchronized(this)
            {
                logger.debug("Collected evt(" + this.collectedPresEvents.size()
                        + ")= " + evt);
                this.collectedPresEvents.add(evt);
                notifyAll();
            }
        }

        public void providerStatusMessageChanged(PropertyChangeEvent evt)
        {
            synchronized(this)
            {
                logger.debug("Collected stat.msg. evt("
                             + this.collectedPresEvents.size() + ")= " + evt);
                this.collectedStatMsgEvents.add(evt);
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
        public void waitForPresEvent(long waitFor)
        {
            logger.trace("Waiting for a change in provider status.");
            synchronized(this)
            {
                if(this.collectedPresEvents.size() > 0){
                    logger.trace("Change already received. " +
                            this.collectedPresEvents);
                    return;
                }

                try{
                    wait(waitFor);
                    if(this.collectedPresEvents.size() > 0)
                        logger.trace("Received a change in provider status.");
                    else
                        logger.trace("No change received for " + waitFor +
                                "ms.");
                }
                catch (InterruptedException ex){
                    logger.debug("Interrupted while waiting for a provider evt",
                        ex);
                }
            }
        }

        /**
         * Blocks until at least one staus message event is received or until
         * waitFor miliseconds pass (whichever happens first).
         *
         * @param waitFor the number of miliseconds that we should be waiting
         * for a status message event before simply bailing out.
         */
        public void waitForStatMsgEvent(long waitFor)
        {
            logger.trace("Waiting for a provider status message event.");
            synchronized(this)
            {
                if(this.collectedStatMsgEvents.size() > 0){
                    logger.trace("Stat msg. evt already received. "
                                 + this.collectedStatMsgEvents);
                    return;
                }

                try{
                    wait(waitFor);
                    if(this.collectedStatMsgEvents.size() > 0)
                        logger.trace("Received a prov. stat. msg. evt.");
                    else
                        logger.trace("No prov. stat msg. received for "
                                     + waitFor + "ms.");
                }
                catch (InterruptedException ex){
                    logger.debug("Interrupted while waiting for a status " +
                            "msg evt", ex);
                }
            }
        }
    }

    /**
     * The class would listen for and store received subscription modification
     * events.
     */
    private class SubscriptionEventCollector implements SubscriptionListener
    {
        public ArrayList<EventObject> collectedEvents = new ArrayList<EventObject>();

        /**
         * Blocks until at least one event is received or until waitFor
         * miliseconds pass (whicever happens first).
         *
         * @param waitFor the number of miliseconds that we should be waiting
         * for an event before simply bailing out.
         */
        public void waitForEvent(long waitFor)
        {
            synchronized(this)
            {
                if(this.collectedEvents.size() > 0)
                    return;

                try{
                    wait(waitFor);
                }
                catch (InterruptedException ex)
                {
                    logger.debug(
                        "Interrupted while waiting for a subscription evt", ex);
                }
            }
        }

        /**
         * Stores the received subsctiption and notifies all waiting on this
         * object
         * @param evt the SubscriptionEvent containing the corresponding contact
         */
        public void subscriptionCreated(SubscriptionEvent evt)
        {
            synchronized(this)
            {
                logger.debug("Collected evt(" + this.collectedEvents.size() +
                        ")= " + evt);
                this.collectedEvents.add(evt);
                notifyAll();
            }
        }

        /**
         * Stores the received subsctiption and notifies all waiting on this
         * object
         * @param evt the SubscriptionEvent containing the corresponding contact
         */
        public void subscriptionRemoved(SubscriptionEvent evt)
        {
            synchronized(this)
            {
                logger.debug("Collected evt(" + this.collectedEvents.size() +
                        ")= " + evt);
                this.collectedEvents.add(evt);
                notifyAll();
            }
        }

        /**
         * Stores the received subsctiption and notifies all waiting on this
         * object
         * @param evt the SubscriptionEvent containing the corresponding contact
         */
        public void contactModified(ContactPropertyChangeEvent evt)
        {
            synchronized(this)
            {
                logger.debug("Collected evt(" + this.collectedEvents.size()
                        + ")= " + evt);
                this.collectedEvents.add(evt);
                notifyAll();
            }
        }


        /**
         * Stores the received subsctiption and notifies all waiting on this
         * object
         * @param evt the SubscriptionEvent containing the corresponding contact
         */
        public void subscriptionMoved(SubscriptionMovedEvent evt)
        {
            synchronized(this)
            {
                logger.debug("Collected evt(" + this.collectedEvents.size()
                        + ")= " + evt);
                this.collectedEvents.add(evt);
                notifyAll();
            }
        }

        /**
         * Stores the received subsctiption and notifies all waiting on this
         * object
         * @param evt the SubscriptionEvent containing the corresponding contact
         */
        public void subscriptionFailed(SubscriptionEvent evt)
        {
            synchronized(this)
            {
                logger.debug("Collected evt(" + this.collectedEvents.size()
                        + ")= " + evt);
                this.collectedEvents.add(evt);
                notifyAll();
            }
        }

        /**
         * Stores the received subsctiption and notifies all waiting on this
         * object
         * @param evt the SubscriptionEvent containing the corresponding contact
         */
        public void subscriptionResolved(SubscriptionEvent evt)
        {
            synchronized(this)
            {
                logger.debug("Collected evt(" + this.collectedEvents.size()
                        + ")= " + evt);
                this.collectedEvents.add(evt);
                notifyAll();
            }
        }

    }

    /**
     * The class would listen for and store received events caused by changes
     * in contact presence states.
     */
    private class ContactPresenceEventCollector
        implements ContactPresenceStatusListener
    {
        public ArrayList<EventObject> collectedEvents = new ArrayList<EventObject>();
        private String trackedScreenName = null;
        private PresenceStatus status = null;

        ContactPresenceEventCollector(String screenname,
                                      PresenceStatus wantedStatus)
        {
            this.trackedScreenName = screenname;
            this.status = wantedStatus;
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
            synchronized(this)
            {
                if(this.collectedEvents.size() > 0)
                    return;

                try{
                    wait(waitFor);
                }
                catch (InterruptedException ex)
                {
                    logger.debug(
                        "Interrupted while waiting for a subscription evt", ex);
                }
            }
        }

        /**
         * Stores the received status change event and notifies all waiting on
         * this object
         * @param evt the SubscriptionEvent containing the corresponding contact
         */
        public void contactPresenceStatusChanged(
            ContactPresenceStatusChangeEvent evt)
        {
            synchronized(this)
            {
                //if the user has specified event details and the received
                //event does not match - then ignore it.
                if(    this.trackedScreenName != null
                    && !evt.getSourceContact().getAddress()
                            .equals(this.trackedScreenName))
                    return;
                if(this.status != null
                    && this.status != evt.getNewStatus())
                    return;

                logger.debug("Collected evt(" + this.collectedEvents.size()
                        + ")= " + evt);
                this.collectedEvents.add(evt);
                notifyAll();
            }
        }
    }
}
