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

import java.beans.*;
import java.util.*;

import junit.framework.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * Tests Gibberish implementations of a Presence Operation Set. Tests in this
 * class verify functionality such as: Changing local (our own) status and
 * corresponding event dispatching; Querying status of contacts, Subscribing
 * for presence notifications upon status changes of specific contacts.
 * <p>
 * Using a custom suite() method, we make sure that apart from standard test
 * methods (those with a <tt>test</tt> prefix) we also execute those that
 * we want run in a specific order like for example - postTestSubscribe() and
 * postTestUnsubscribe().
 * <p>
 * @author Emil Ivov
 */
public class TestOperationSetPresence
    extends TestCase
{
    private static final Logger logger =
        Logger.getLogger(TestOperationSetPresence.class);

    private GibberishSlickFixture fixture = new GibberishSlickFixture();
    private OperationSetPresence operationSetPresence1 = null;
    private OperationSetPresence operationSetPresence2 = null;
    private String statusMessageRoot = new String("Our status is now: ");

    private static AuthEventCollector authEventCollector1 = new AuthEventCollector();
    private static AuthEventCollector authEventCollector2 = new AuthEventCollector();

    public TestOperationSetPresence(String name)
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
        operationSetPresence1 =
            (OperationSetPresence)supportedOperationSets1.get(
                OperationSetPresence.class.getName());

        //if the op set is null then the implementation doesn't offer a presence
        //operation set which is unacceptable for Gibberish.
        if (operationSetPresence1 == null)
        {
            throw new NullPointerException(
                "An implementation of the Gibberish service must provide an "
                + "implementation of at least the one of the Presence "
                + "Operation Sets");
        }

        // do it once again for the second provider
        Map<String, OperationSet> supportedOperationSets2 =
            fixture.provider2.getSupportedOperationSets();

        if ( supportedOperationSets2 == null
            || supportedOperationSets2.size() < 1)
            throw new NullPointerException(
                "No OperationSet implementations are supported by "
                +"this Gibberish implementation. ");

        //get the operation set presence here.
        operationSetPresence2 =
            (OperationSetPresence)supportedOperationSets2.get(
                OperationSetPresence.class.getName());

        //if the op set is null then the implementation doesn't offer a presence
        //operation set which is unacceptable for Gibberish.
        if (operationSetPresence2 == null)
        {
            throw new NullPointerException(
                "An implementation of the Gibberish service must provide an "
                + "implementation of at least the one of the Presence "
                + "Operation Sets");
        }

    }

    @Override
    protected void tearDown() throws Exception
    {
        super.tearDown();

        fixture.tearDown();
    }

    /**
     * Creates a test suite containing all tests of this class followed by
     * test methods that we want executed in a specified order.
     * @return Test
     */
    public static Test suite()
    {
        //return an (almost) empty suite if we're running in offline mode.
        if(GibberishSlickFixture.onlineTestingDisabled)
        {
            TestSuite suite = new TestSuite();
            //the only test around here that we could run without net
            //connectivity
            suite.addTest(
                new TestOperationSetPresence(
                        "testSupportedStatusSetForCompleteness"));
            return suite;
        }

        TestSuite suite = new TestSuite();

        // clear the lists before subscribing users
        suite.addTest(new TestOperationSetPresence("clearLists"));

        // first postTestSubscribe. to be sure that contacts are in the
        // list so we can further continue and test presences each other
        suite.addTest(new TestOperationSetPresence("postTestSubscribe"));

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
            operationSetPresence1.getSupportedStatusSet();

        while (supportedStatusSetIter.hasNext())
        {
            PresenceStatus supportedStatus = supportedStatusSetIter.next();

            logger.trace("Will test a transition to "
                         + supportedStatus.getStatusName());

            subtestStateTransition(supportedStatus);
        }
    }

    /**
     * Used by methods testing state transiotions
     *
     * @param newStatus the status that we'd like the opeation set to enter.
     *
     * @throws Exception in case changing the state causes an exception
     */
    public void subtestStateTransition( PresenceStatus newStatus)
        throws Exception
    {
        logger.trace(" --=== beginning state transition test ===--");

        PresenceStatus oldStatus = operationSetPresence1.getPresenceStatus();
        String oldStatusMessage = operationSetPresence1.getCurrentStatusMessage();
        String newStatusMessage = statusMessageRoot + newStatus;

        logger.debug(   "old status is=" + oldStatus.getStatusName()
                     + " new status=" + newStatus.getStatusName());

        //First register a listener to make sure that all corresponding
        //events have been generated.
        PresenceStatusEventCollector statusEventCollector
            = new PresenceStatusEventCollector();
        operationSetPresence1.addProviderPresenceStatusListener(
            statusEventCollector);

        //change the status
        operationSetPresence1.publishPresenceStatus(newStatus, newStatusMessage);
        pauseAfterStateChanges();

        //test event notification.
        statusEventCollector.waitForPresEvent(10000);
        statusEventCollector.waitForStatMsgEvent(10000);

        operationSetPresence1.removeProviderPresenceStatusListener(
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
            operationSetPresence1.getPresenceStatus());

        PresenceStatus actualStatus
            = operationSetPresence2.queryContactStatus(fixture.userID1);

        assertEquals("The underlying implementation did not switch to the "
                     +"requested presence status.",
                     newStatus,
                     actualStatus);

        //check whether the server returned the status message that we've set.
        assertEquals("No status message events.",
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
            operationSetPresence1.getCurrentStatusMessage());

        logger.trace(" --=== finished test ===--");
    }

    /**
     * Give time changes to take effect
     */
    private void pauseAfterStateChanges()
    {
        try
        {
            Thread.sleep(1500);
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
    public void testQueryContactStatus()
        throws Exception
    {
        //first create a local list containing the presence status instances
        //supported by the underlying implementation.
        Iterator<PresenceStatus> supportedStatusSetIter = operationSetPresence1.getSupportedStatusSet();

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
        operationSetPresence2.publishPresenceStatus(status, "status message");

        pauseAfterStateChanges();

        PresenceStatus actualReturn
            = operationSetPresence1.queryContactStatus(fixture.userID2);
        assertEquals("Querying a "
                     + status.getStatusName()
                     + " state did not return as expected"
                     , status, actualReturn);
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


        logger.trace("set Auth Handlers");
        operationSetPresence1.setAuthorizationHandler(authEventCollector1);
        operationSetPresence2.setAuthorizationHandler(authEventCollector2);

        /**
         * Testing Scenario
         *
         * user1 add user2
         *     - check user2 receive auth request
         *     - user2 deny
         *      - check user1 received deny
         * user1 add user2
         *     - check user2 receive auth request
         *     - user2 accept
         *     - check user1 received accept
         */


        // first we will reject
        authEventCollector2.responseToRequest =
            new AuthorizationResponse(AuthorizationResponse.REJECT, null);

        operationSetPresence1.subscribe(fixture.userID2);

        authEventCollector2.waitForAuthRequest(10000);

        assertTrue("Error authorization request not received from " +
                        fixture.userID2,
                       authEventCollector2.isAuthorizationRequestReceived);

        authEventCollector1.waitForAuthResponse(10000);

        assertTrue("Error authorization reply not received from " +
                        fixture.userID1,
                       authEventCollector1.isAuthorizationResponseReceived);

        assertEquals("Error received authorization reply not as expected",
             authEventCollector2.responseToRequest.getResponseCode(),
             authEventCollector1.response.getResponseCode());

        pauseAfterStateChanges();

        SubscriptionEventCollector subEvtCollector
            = new SubscriptionEventCollector();
        operationSetPresence1.addSubscriptionListener(subEvtCollector);

        // second we will accept
        authEventCollector2.responseToRequest =
            new AuthorizationResponse(AuthorizationResponse.ACCEPT, null);
        authEventCollector2.isAuthorizationRequestReceived = false;
        authEventCollector1.isAuthorizationResponseReceived = false;

        operationSetPresence1.subscribe(fixture.userID2);

        authEventCollector2.waitForAuthRequest(10000);

        assertTrue("Error authorization request not received from " +
                        fixture.userID2,
                       authEventCollector2.isAuthorizationRequestReceived);

        authEventCollector1.waitForAuthResponse(10000);

        assertTrue("Error authorization reply not received from " +
                        fixture.userID1,
                       authEventCollector1.isAuthorizationResponseReceived);

        assertEquals("Error received authorization reply not as expected",
             authEventCollector2.responseToRequest.getResponseCode(),
             authEventCollector1.response.getResponseCode());

        // fix . from no on accept all subscription request for the two tested accounts
        authEventCollector1.responseToRequest =
            new AuthorizationResponse(AuthorizationResponse.ACCEPT, null);
        authEventCollector2.responseToRequest =
            new AuthorizationResponse(AuthorizationResponse.ACCEPT, null);

        operationSetPresence1.removeSubscriptionListener(subEvtCollector);

        assertEquals("Subscription event dispatching failed."
                     , 1, subEvtCollector.collectedEvents.size());
        SubscriptionEvent subEvt =
            (SubscriptionEvent)subEvtCollector.collectedEvents.get(0);

        assertEquals("SubscriptionEvent Source:",
                     fixture.userID2,
                     ((Contact)subEvt.getSource()).getAddress());
        assertEquals("SubscriptionEvent Source Contact:",
                     fixture.userID2,
                     subEvt.getSourceContact().getAddress());
        assertSame("SubscriptionEvent Source Provider:",
                     fixture.provider1,
                     subEvt.getSourceProvider());

        subEvtCollector.collectedEvents.clear();

        // make the user agent tester change its states and make sure we are
        // notified
        logger.debug("Testing presence notifications.");
        PresenceStatus oldStatus
            = operationSetPresence2.getPresenceStatus();


        PresenceStatus newStatus = getSampleStatus1();

        //in case we are by any chance already in that status, we'll
        //be changing to something else
        if(oldStatus.equals(newStatus)){
            newStatus = getSampleStatus2();
        }

        //now do the actual status notification testing
        ContactPresenceEventCollector contactPresEvtCollector
            = new ContactPresenceEventCollector(
                    fixture.userID2, newStatus);
        operationSetPresence1.addContactPresenceStatusListener(
            contactPresEvtCollector);

        synchronized (contactPresEvtCollector){
            operationSetPresence2.publishPresenceStatus(newStatus, "new status");
            //we may already have the event, but it won't hurt to check.
            contactPresEvtCollector.waitForEvent(2000);
            operationSetPresence1
                .removeContactPresenceStatusListener(contactPresEvtCollector);
        }

        assertEquals("Presence Notif. event dispatching failed. List is "
                     + contactPresEvtCollector.collectedEvents
                     , 1, contactPresEvtCollector.collectedEvents.size());
        ContactPresenceStatusChangeEvent presEvt =
            (ContactPresenceStatusChangeEvent)
                contactPresEvtCollector.collectedEvents.get(0);

        assertEquals("Presence Notif. event  Source:",
                     fixture.userID2,
                     ((Contact)presEvt.getSource()).getAddress());
        assertEquals("Presence Notif. event  Source Contact:",
                     fixture.userID2,
                     presEvt.getSourceContact().getAddress());
        assertSame("Presence Notif. event  Source Provider:",
                     fixture.provider1,
                     presEvt.getSourceProvider());

        PresenceStatus reportedNewStatus = presEvt.getNewStatus();
        PresenceStatus reportedOldStatus = presEvt.getOldStatus();

        assertEquals( "Reported new PresenceStatus: ",
                      newStatus, reportedNewStatus );

        //don't require equality between the reported old PresenceStatus and
        //the actual presence status of the tester agent because a first
        //notification is not supposed to have the old status as it really was.
        assertNotNull( "Reported old PresenceStatus: ", reportedOldStatus );
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
        operationSetPresence1.addSubscriptionListener(subEvtCollector);

        Contact gibberishTesterAgentContact = operationSetPresence1
            .findContactByID(fixture.userID2);

        assertNotNull(
            "Failed to find an existing subscription for the tester agent"
            , gibberishTesterAgentContact);

        synchronized(subEvtCollector){
            operationSetPresence1.unsubscribe(gibberishTesterAgentContact);
            subEvtCollector.waitForEvent(10000);
            //don't want any more events
            operationSetPresence1.removeSubscriptionListener(subEvtCollector);
        }

        assertEquals("Subscription event dispatching failed."
                     , 1, subEvtCollector.collectedEvents.size());
        SubscriptionEvent subEvt =
            (SubscriptionEvent)subEvtCollector.collectedEvents.get(0);

        assertEquals("SubscriptionEvent Source:",
                     gibberishTesterAgentContact, subEvt.getSource());

        assertEquals("SubscriptionEvent Source Contact:",
                     gibberishTesterAgentContact, subEvt.getSourceContact());

        assertSame("SubscriptionEvent Source Provider:",
                     fixture.provider1,
                     subEvt.getSourceProvider());

        subEvtCollector.collectedEvents.clear();

        // make the user agent tester change its states and make sure we don't
        // get notifications as we're now unsubscribed.
        logger.debug("Testing (lack of) presence notifications.");
        PresenceStatus oldStatus
            = operationSetPresence2.getPresenceStatus();
        PresenceStatus newStatus = getSampleStatus1();

        //in case we are by any chance already in that status, we'll
        //be changing to something else
        if(oldStatus.equals(newStatus)){
            newStatus = getSampleStatus2();
        }

        //now do the actual status notification testing
        ContactPresenceEventCollector contactPresEvtCollector
            = new ContactPresenceEventCollector(fixture.userID2, null);
        operationSetPresence1.addContactPresenceStatusListener(
            contactPresEvtCollector);

        synchronized (contactPresEvtCollector){
            operationSetPresence2.publishPresenceStatus(newStatus, "new status");

            //we may already have the event, but it won't hurt to check.
            contactPresEvtCollector.waitForEvent(5000);
            operationSetPresence1
                .removeContactPresenceStatusListener(contactPresEvtCollector);
        }

        assertEquals("Presence Notifications were received after unsubscibing."
                     , 0, contactPresEvtCollector.collectedEvents.size());
    }

    /**
     * Clears server stored lists.
     * @throws Exception if the underlying provider throws an <tt>Exception</tt>
     * while we remove all contacts from the contact list.
     */
    public void clearLists()
        throws Exception
    {
        logger.debug("Clear the two lists before tests");

        fixture.clearProvidersLists();

        Object o = new Object();
        synchronized(o)
        {
            o.wait(3000);
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
            operationSetPresence1.getSupportedStatusSet();

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
     * Returns the online status with a lowest connectivity index.
     *
     * @return the online <tt>PresenceStatus</tt> with a lowest connectivity
     * index.
     */
    private PresenceStatus getSampleStatus2()
    {
        //iterate through all supported statuses and return the one with the
        //highest connectivity index.
        PresenceStatus leastConnectedPresenceStatus = null;
        int leastConnectedPresenceStatusInt = Integer.MAX_VALUE;

        Iterator<PresenceStatus> supportedStatusSetIter =
            operationSetPresence1.getSupportedStatusSet();

        while (supportedStatusSetIter.hasNext())
        {
            PresenceStatus supportedStatus = supportedStatusSetIter.next();

            if(supportedStatus.getStatus() < leastConnectedPresenceStatusInt
                && leastConnectedPresenceStatusInt
                        >= PresenceStatus.ONLINE_THRESHOLD)
            {
                leastConnectedPresenceStatus = supportedStatus;
                leastConnectedPresenceStatusInt = supportedStatus.getStatus();
            }
        }

        return leastConnectedPresenceStatus;
    }

    /**
     * An event collector that would collect all events generated by a
     * provider after a status change. The collector would also do a notidyAll
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
                logger.debug("Collected evt("+collectedPresEvents.size()+")= "+evt);
                collectedPresEvents.add(evt);
                notifyAll();
            }
        }

        public void providerStatusMessageChanged(PropertyChangeEvent evt)
        {
            synchronized(this)
            {
                logger.debug("Collected stat.msg. evt("
                             +collectedPresEvents.size()+")= "+evt);
                collectedStatMsgEvents.add(evt);
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
                if(collectedPresEvents.size() > 0){
                    logger.trace("Change already received. " + collectedPresEvents);
                    return;
                }

                try{
                    wait(waitFor);
                    if(collectedPresEvents.size() > 0)
                        logger.trace("Received a change in provider status.");
                    else
                        logger.trace("No change received for "+waitFor+"ms.");
                }
                catch (InterruptedException ex){
                    logger.debug("Interrupted while waiting for a provider evt"
                        , ex);
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
                if(collectedStatMsgEvents.size() > 0){
                    logger.trace("Stat msg. evt already received. "
                                 + collectedStatMsgEvents);
                    return;
                }

                try{
                    wait(waitFor);
                    if(collectedStatMsgEvents.size() > 0)
                        logger.trace("Received a prov. stat. msg. evt.");
                    else
                        logger.trace("No prov. stat msg. received for "
                                     +waitFor+"ms.");
                }
                catch (InterruptedException ex){
                    logger.debug("Interrupted while waiting for a status msg evt"
                        , ex);
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
                if(collectedEvents.size() > 0)
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
                logger.debug("Collected evt("+collectedEvents.size()+")= "+evt);
                collectedEvents.add(evt);
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
                logger.debug("Collected evt("+collectedEvents.size()+")= "+evt);
                collectedEvents.add(evt);
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
                logger.debug("Collected evt("+collectedEvents.size()+")= "+evt);
                collectedEvents.add(evt);
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
                logger.debug("Collected evt("+collectedEvents.size()+")= "+evt);
                collectedEvents.add(evt);
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
                logger.debug("Collected evt("+collectedEvents.size()+")= "+evt);
                collectedEvents.add(evt);
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
                logger.debug("Collected evt("+collectedEvents.size()+")= "+evt);
                collectedEvents.add(evt);
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

        ContactPresenceEventCollector(String         screenname,
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
                if(collectedEvents.size() > 0)
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
                            .equals(trackedScreenName))
                    return;
                if( status != null
                    && status != evt.getNewStatus())
                    return;

                logger.debug("Collected evt("+collectedEvents.size()+")= "+evt);
                collectedEvents.add(evt);
                notifyAll();
            }
        }
    }

    /**
     * Authorization handler for the implementation tests
     * <p>
     * 1. when authorization request is received we answer with the already set
     * Authorization response, but before that wait some time as a normal user
     * </p>
     * <p>
     * 2. When authorization request is required for adding buddy
     * the request is made with already set authorization reason
     * </p>
     * <p>
     * 3. When authorization replay is received - we store that it is received
     * and the reason that was received
     * </p>
     */
    private static class AuthEventCollector
        implements AuthorizationHandler
    {
        boolean isAuthorizationRequestSent = false;

        boolean isAuthorizationResponseReceived = false;
        AuthorizationResponse response = null;

        // receiving auth request
        AuthorizationResponse responseToRequest = null;
        boolean isAuthorizationRequestReceived = false;

        public AuthorizationResponse processAuthorisationRequest(
                        AuthorizationRequest req, Contact sourceContact)
        {
            logger.debug("Processing in " + this);
            synchronized(this)
            {
                logger.trace("processAuthorisationRequest " + req + " " +
                             sourceContact);

                isAuthorizationRequestReceived = true;

                notifyAll();

                // will wait as a normal user
                Object lock = new Object();
                synchronized (lock)
                {
                    try
                    {
                        lock.wait(2000);
                    }
                    catch (Exception ex)
                    {}
                }

                return responseToRequest;
            }
        }

        public AuthorizationRequest createAuthorizationRequest(Contact contact)
        {
            logger.trace("createAuthorizationRequest " + contact);

            AuthorizationRequest authReq = new AuthorizationRequest();

            isAuthorizationRequestSent = true;

            return authReq;
        }

        public void processAuthorizationResponse(AuthorizationResponse
            response, Contact sourceContact)
        {
            synchronized(this)
            {
                isAuthorizationResponseReceived = true;
                this.response = response;

                logger.trace("processAuthorizationResponse '" +
                             response.getResponseCode() + " " +
                             sourceContact);

                notifyAll();
            }
        }

        public void waitForAuthResponse(long waitFor)
        {
            synchronized(this)
            {
                if(isAuthorizationResponseReceived)
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

        public void waitForAuthRequest(long waitFor)
        {
            synchronized(this)
            {
                if(isAuthorizationRequestReceived)
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
    }
}
