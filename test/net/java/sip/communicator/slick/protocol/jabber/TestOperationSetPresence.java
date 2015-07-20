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
package net.java.sip.communicator.slick.protocol.jabber;

import java.beans.*;
import java.util.*;

import junit.framework.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.service.protocol.jabberconstants.*;
import net.java.sip.communicator.util.*;

/**
 * Tests Jabber implementations of a Presence Operation Set. Tests in this class
 * verify functionality such as: Changing local (our own) status and
 * corresponding event dispatching; Querying status of contacts, Subscribing
 * for presence notifications upong status changes of specific contacts.
 * <p>
 * Using a custom suite() method, we make sure that apart from standard test
 * methods (those with a <tt>test</tt> prefix) we also execute those that
 * we want run in a specific order like for example - postTestSubscribe() and
 * postTestUnsubscribe().
 * <p>
 * @author Damian Minkov
 * @author Lubomir Marinov
 */
public class TestOperationSetPresence
    extends TestCase
{
    private static final Logger logger =
        Logger.getLogger(TestOperationSetPresence.class);

    private JabberSlickFixture fixture = new JabberSlickFixture();
    private OperationSetPresence operationSetPresence1 = null;
    private final Map<String, PresenceStatus> supportedStatusSet1
        = new HashMap<String, PresenceStatus>();
    private OperationSetPresence operationSetPresence2 = null;
    private final Map<String, PresenceStatus> supportedStatusSet2
        = new HashMap<String, PresenceStatus>();
    private String statusMessageRoot = new String("Our status is now: ");

    private static AuthEventCollector authEventCollector1
        = new AuthEventCollector();
    private static AuthEventCollector authEventCollector2
        = new AuthEventCollector();

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
        //operation set which is unacceptable for jabber.
        if (operationSetPresence1 == null)
        {
            throw new NullPointerException(
                "An implementation of the Jabber service must provide an "
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
                +"this Jabber implementation. ");

        //get the operation set presence here.
        operationSetPresence2 =
            (OperationSetPresence)supportedOperationSets2.get(
                OperationSetPresence.class.getName());

        //if the op set is null then the implementation doesn't offer a presence
        //operation set which is unacceptable for jabber.
        if (operationSetPresence2 == null)
        {
            throw new NullPointerException(
                "An implementation of the Jabber service must provide an "
                + "implementation of at least the one of the Presence "
                + "Operation Sets");
        }

        /*
         * Retrieve the supported PresenceStatus values because the instances
         * are specific to the ProtocolProviderService implementations.
         */
        // operationSetPresence1
        for (Iterator<PresenceStatus> supportedStatusIt
                        = operationSetPresence1.getSupportedStatusSet();
             supportedStatusIt.hasNext();)
        {
            PresenceStatus supportedStatus = supportedStatusIt.next();
            supportedStatusSet1.put(supportedStatus.getStatusName(),
                supportedStatus);
        }
        // operationSetPresence2
        for (Iterator<PresenceStatus> supportedStatusIt
                        = operationSetPresence2.getSupportedStatusSet();
             supportedStatusIt.hasNext();)
        {
            PresenceStatus supportedStatus = supportedStatusIt.next();
            supportedStatusSet2.put(supportedStatus.getStatusName(),
                supportedStatus);
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
        if(JabberSlickFixture.onlineTestingDisabled)
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

//        // add other tests
//        suite.addTestSuite(TestOperationSetPresence.class);
//
        // now test unsubscribe
        suite.addTest(new TestOperationSetPresence("postTestUnsubscribe"));

        return suite;
    }

    /**
     * Verifies that all necessary Jabber test states are supported by the
     * implementation.
     */
    public void testSupportedStatusSetForCompleteness()
    {
        //first create a local list containing the presence status instances
        //supported by the underlying implementation.
        Iterator<PresenceStatus> supportedStatusSetIter =
            operationSetPresence1.getSupportedStatusSet();

        List<String> supportedStatusNames = new LinkedList<String>();
        while (supportedStatusSetIter.hasNext())
        {
            supportedStatusNames.add(supportedStatusSetIter
                .next().getStatusName());
        }

        //create a copy of the MUST status set and remove any matching status
        //that is also present in the supported set.
        List<String> requiredStatusNames =
            Arrays.asList(JabberStatusEnum.getStatusNames());

        requiredStatusNames.removeAll(supportedStatusNames);

        //if we have anything left then the implementation is wrong.
        int unsupported = requiredStatusNames.size();
        assertTrue( "There are " + unsupported + " statuses as follows:"
            + requiredStatusNames, unsupported == 0);
    }

    /**
     * Verify that changing state to AWAY works as supposed to and that it
     * generates the corresponding event.
     * @throws Exception in case a failure occurs while the operation set
     * is switching to the new state.
     */
    public void testChangingStateToAway() throws Exception
    {
        subtestStateTransition(JabberStatusEnum.AWAY);
    }

    /**
     * Verify that changing state to DND works as supposed to and that it
     * generates the corresponding event.
     * @throws Exception in case a failure occurs while the operation set
     * is switching to the new state.
     */
    public void testChangingStateToDnd() throws Exception
    {
        subtestStateTransition(JabberStatusEnum.DO_NOT_DISTURB);
    }

    /**
     * Verify that changing state to FREE_FOR_CHAT works as supposed to and
     * that it generates the corresponding event.
     * @throws Exception in case a failure occurs while the operation set
     * is switching to the new state.
     */
    public void testChangingStateToFreeForChat() throws Exception
    {
        subtestStateTransition(JabberStatusEnum.FREE_FOR_CHAT);
    }

    /**
     * Verify that changing state to ONLINE works as supposed to and that it
     * generates the corresponding event.
     * @throws Exception in case a failure occurs while the operation set
     * is switching to the new state.
     */
    public void testChangingStateToOnline() throws Exception
    {
        subtestStateTransition(JabberStatusEnum.AVAILABLE);
    }

    /**
     * Used by methods testing state transiotions
     *
     * @param newStatus the JabberStatusEnum field corresponding to the status
     * that we'd like the opeation set to enter.
     *
     * @throws Exception in case changing the state causes an exception
     */
    private void subtestStateTransition(String newStatusName) throws Exception
    {
        logger.trace(" --=== beginning state transition test ===--");

        PresenceStatus newStatus = supportedStatusSet1.get(newStatusName);

        PresenceStatus oldStatus = operationSetPresence1.getPresenceStatus();
        String oldStatusMessage
            = operationSetPresence1.getCurrentStatusMessage();
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
        operationSetPresence1
            .publishPresenceStatus(newStatus, newStatusMessage);
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
                     statusEventCollector
                         .collectedPresEvents.get(0).getOldStatus());
        assertEquals("A status changed event contained wrong new status.",
                     newStatus,
                     statusEventCollector
                         .collectedPresEvents.get(0).getNewStatus());

        // verify that the operation set itself is aware of the status change
        assertEquals("opSet.getPresenceStatus() did not return properly.",
            newStatus,
            operationSetPresence1.getPresenceStatus());

        PresenceStatus actualStatus =
            operationSetPresence2.queryContactStatus(fixture.userID1);

        assertEquals("The underlying implementation did not switch to the "
                     +"requested presence status.",
                     newStatus,
                     actualStatus);

        //check whether the server returned the status message that we've set.
        assertEquals("No status message events.",
                     1, statusEventCollector.collectedStatMsgEvents.size());
        assertEquals("A status message event contained wrong old value.",
                     oldStatusMessage,
                        statusEventCollector.collectedStatMsgEvents.get(0)
                            .getOldValue());
        assertEquals("A status message event contained wrong new value.",
                     newStatusMessage,
                        statusEventCollector.collectedStatMsgEvents.get(0)
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
        // --- AWAY ---
        logger.debug("Will Query an AWAY contact.");
        subtestQueryContactStatus(JabberStatusEnum.AWAY,
                                  JabberStatusEnum.AWAY);

        // --- DND ---
        logger.debug("Will Query a DND contact.");
        subtestQueryContactStatus(JabberStatusEnum.DO_NOT_DISTURB,
                                  JabberStatusEnum.DO_NOT_DISTURB);

        // --- FFC ---
        logger.debug("Will Query a Free For Chat contact.");
        subtestQueryContactStatus(JabberStatusEnum.FREE_FOR_CHAT,
                                  JabberStatusEnum.FREE_FOR_CHAT);

        // --- Online ---
        logger.debug("Will Query an Online contact.");
        subtestQueryContactStatus(JabberStatusEnum.AVAILABLE,
                                  JabberStatusEnum.AVAILABLE);
    }

    /**
     * Used by functions testing the queryContactStatus method of the
     * presence operation set.
     * @param status the status as specified, that
     * the tester agent should switch to.
     * @param expectedReturn the PresenceStatus that the presence operation
     * set should see the tester agent in once it has switched to taStatusLong.
     *
     * @throws java.lang.Exception if querying the status causes some exception.
     */
    private void subtestQueryContactStatus(String status, String expectedReturn)
        throws Exception
    {
        operationSetPresence2.publishPresenceStatus(
            supportedStatusSet2.get(status), "status message");

        pauseAfterStateChanges();

        PresenceStatus actualReturn
            = operationSetPresence1.queryContactStatus(fixture.userID2);
        assertEquals("Querying a "
                     + expectedReturn
                     + " state did not return as expected"
                     , supportedStatusSet1.get(expectedReturn)
                     , actualReturn);
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

        // fix . from no on accept all subscription request for the two
        // tested accounts
        authEventCollector1.responseToRequest =
            new AuthorizationResponse(AuthorizationResponse.ACCEPT, null);
        authEventCollector2.responseToRequest =
            new AuthorizationResponse(AuthorizationResponse.ACCEPT, null);

        operationSetPresence1.removeSubscriptionListener(subEvtCollector);


        assertTrue("Subscription event dispatching failed."
                     , subEvtCollector.collectedSubscriptionEvents.size() > 0);

        SubscriptionEvent subEvt = null;

        synchronized(subEvtCollector)
        {
            Iterator<SubscriptionEvent> events
                = subEvtCollector.collectedSubscriptionEvents.iterator();
            while (events.hasNext())
            {
                SubscriptionEvent elem = events.next();
                if(elem.getEventID() == SubscriptionEvent.SUBSCRIPTION_CREATED)
                    subEvt = elem;
            }
        }

        // it happens that when adding contacts which require authorization
        // sometimes the collected events are 3 - added, deleted, added
        // so we get the last one if there is such
        assertNotNull("Subscription event dispatching failed.", subEvt);

        assertEquals("SubscriptionEvent Source:",
                     fixture.userID2,
                     ((Contact)subEvt.getSource()).getAddress());
        assertEquals("SubscriptionEvent Source Contact:",
                     fixture.userID2,
                     subEvt.getSourceContact().getAddress());
        assertSame("SubscriptionEvent Source Provider:",
                     fixture.provider1,
                     subEvt.getSourceProvider());

        subEvtCollector.collectedSubscriptionEvents.clear();
        subEvtCollector.collectedSubscriptionMovedEvents.clear();
        subEvtCollector.collectedContactPropertyChangeEvents.clear();

        // make the user agent tester change its states and make sure we are
        // notified
        logger.debug("Testing presence notifications.");
        PresenceStatus oldStatus
            = operationSetPresence2.getPresenceStatus();

        PresenceStatus newStatus
            = supportedStatusSet2.get(JabberStatusEnum.FREE_FOR_CHAT);

        //in case we are by any chance already in a FREE_FOR_CHAT status, we'll
        //be changing to something else
        if(oldStatus.equals(newStatus)){
            newStatus
                = supportedStatusSet2.get(JabberStatusEnum.DO_NOT_DISTURB);
        }

        //now do the actual status notification testing
        ContactPresenceEventCollector contactPresEvtCollector
            = new ContactPresenceEventCollector(
                    fixture.userID2, newStatus);
        operationSetPresence1.addContactPresenceStatusListener(
            contactPresEvtCollector);

        synchronized (contactPresEvtCollector){
            operationSetPresence2
                .publishPresenceStatus(newStatus, "new status");
            //we may already have the event, but it won't hurt to check.
            contactPresEvtCollector.waitForEvent(10000);
            operationSetPresence1
                .removeContactPresenceStatusListener(contactPresEvtCollector);
        }

        assertEquals(
                "Presence Notif. event dispatching failed.",
                1,
                contactPresEvtCollector.collectedEvents.size());
        ContactPresenceStatusChangeEvent presEvt = contactPresEvtCollector.collectedEvents.get(0);

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

        Contact jabberTesterAgentContact = operationSetPresence1
            .findContactByID(fixture.userID2);

        assertNotNull(
            "Failed to find an existing subscription for the tester agent"
            , jabberTesterAgentContact);

        synchronized(subEvtCollector){
            operationSetPresence1.unsubscribe(jabberTesterAgentContact);
            subEvtCollector.waitForSubscriptionEvent(10000);
            //don't want any more events
            operationSetPresence1.removeSubscriptionListener(subEvtCollector);
        }

        assertEquals(
                "Subscription event dispatching failed.",
                1,
                subEvtCollector.collectedSubscriptionEvents.size());
        SubscriptionEvent subEvt =
            subEvtCollector.collectedSubscriptionEvents.get(0);

        assertEquals("SubscriptionEvent Source:",
                     jabberTesterAgentContact, subEvt.getSource());

        assertEquals("SubscriptionEvent Source Contact:",
                     jabberTesterAgentContact, subEvt.getSourceContact());

        assertSame("SubscriptionEvent Source Provider:",
                     fixture.provider1,
                     subEvt.getSourceProvider());

        subEvtCollector.collectedSubscriptionEvents.clear();
        subEvtCollector.collectedSubscriptionMovedEvents.clear();
        subEvtCollector.collectedContactPropertyChangeEvents.clear();

        // make the user agent tester change its states and make sure we don't
        // get notifications as we're now unsubscribed.
        logger.debug("Testing (lack of) presence notifications.");
        PresenceStatus oldStatus
            = operationSetPresence2.getPresenceStatus();
        PresenceStatus newStatus
            = supportedStatusSet2.get(JabberStatusEnum.FREE_FOR_CHAT);

        //in case we are by any chance already in a FREE_FOR_CHAT status, we'll
        //be changing to something else
        if(oldStatus.equals(newStatus)){
            newStatus
                = supportedStatusSet2.get(JabberStatusEnum.DO_NOT_DISTURB);
        }

        //now do the actual status notification testing
        ContactPresenceEventCollector contactPresEvtCollector
            = new ContactPresenceEventCollector(fixture.userID2, null);
        operationSetPresence1.addContactPresenceStatusListener(
            contactPresEvtCollector);

        synchronized (contactPresEvtCollector)
        {
            operationSetPresence2.publishPresenceStatus(
                            newStatus, "new status");

            //we may already have the event, but it won't hurt to check.
            contactPresEvtCollector.waitForEvent(10000);
            operationSetPresence1
                .removeContactPresenceStatusListener(contactPresEvtCollector);
        }

        assertEquals("Presence Notifications were received after unsubscibing."
                     , 0, contactPresEvtCollector.collectedEvents.size());
    }

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
     * An event collector that would collect all events generated by a
     * provider after a status change. The collector would also do a notidyAll
     * every time it receives an event.
     */
    private class PresenceStatusEventCollector
        implements ProviderPresenceStatusListener
    {
        public ArrayList<ProviderPresenceStatusChangeEvent> collectedPresEvents
            = new ArrayList<ProviderPresenceStatusChangeEvent>();
        public ArrayList<PropertyChangeEvent> collectedStatMsgEvents
            = new ArrayList<PropertyChangeEvent>();

        public void providerStatusChanged(ProviderPresenceStatusChangeEvent evt)
        {
            synchronized(this)
            {
                logger.debug("Collected evt("+collectedPresEvents.size()
                                    +")= "+evt);
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
            TestOperationSetPresence.waitForEvent(
                    this,
                    waitFor,
                    collectedPresEvents);
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
            TestOperationSetPresence.waitForEvent(
                    this,
                    waitFor,
                    collectedStatMsgEvents);
        }
    }

    /**
     * The class would listen for and store received subscription modification
     * events.
     */
    private class SubscriptionEventCollector implements SubscriptionListener
    {
        /**
         * Collects all SubscriptionEvent generated.
         */
        public ArrayList<SubscriptionEvent> collectedSubscriptionEvents
            = new ArrayList<SubscriptionEvent>();

        /**
         * Collects all SubscriptionMovedEvent generated.
         */
        public ArrayList<SubscriptionMovedEvent>
            collectedSubscriptionMovedEvents
            = new ArrayList<SubscriptionMovedEvent>();

        /**
         * Collects all ContactPropertyChangeEvent generated.
         */
        public ArrayList<ContactPropertyChangeEvent>
            collectedContactPropertyChangeEvents
            = new ArrayList<ContactPropertyChangeEvent>();

        /**
         * Blocks until at least one SubscriptionEvent is received or until
         * waitFor miliseconds pass (whicever happens first).
         *
         * @param waitFor the number of miliseconds that we should be waiting
         * for an SubscriptionEvent before simply bailing out.
         */
        public void waitForSubscriptionEvent(long waitFor)
        {
            TestOperationSetPresence.waitForEvent(
                    this,
                    waitFor,
                    collectedSubscriptionEvents);
        }

        /**
         * Blocks until at least one SubscriptionMovedEvent is received or until
         * waitFor miliseconds pass (whicever happens first).
         *
         * @param waitFor the number of miliseconds that we should be waiting
         * for an SubscriptionMovedEvent before simply bailing out.
         */
        public void waitForSubscriptionMovedEvent(long waitFor)
        {
            TestOperationSetPresence.waitForEvent(
                    this,
                    waitFor,
                    collectedSubscriptionMovedEvents);
        }

        /**
         * Blocks until at least one ContactPropertyChangeEvent is received or
         * until waitFor miliseconds pass (whicever happens first).
         *
         * @param waitFor the number of miliseconds that we should be waiting
         * for an ContactPropertyChangeEvent before simply bailing out.
         */
        public void waitForContactPropertyChangeEvent(long waitFor)
        {
            TestOperationSetPresence.waitForEvent(
                    this,
                    waitFor,
                    collectedContactPropertyChangeEvents);
        }

        /**
         * Stores the received subsctiption and notifies all waiting on this
         * object
         * @param evt the SubscriptionEvent containing the corresponding contact
         */
        public void receivedSubscriptionEvent(SubscriptionEvent evt)
        {
            synchronized(this)
            {
                logger.debug(
                        "Collected SubscriptionEvnet("
                        + collectedSubscriptionEvents.size()
                        + ")= "
                        + evt);
                collectedSubscriptionEvents.add(evt);
                notifyAll();
            }
        }

        /**
         * Stores the received subsctiption and notifies all waiting on this
         * object
         * @param evt the SubscriptionEvent containing the corresponding contact
         */
        public void subscriptionCreated(SubscriptionEvent evt)
        {
            receivedSubscriptionEvent(evt);
        }

        /**
         * Stores the received subsctiption and notifies all waiting on this
         * object
         * @param evt the SubscriptionEvent containing the corresponding contact
         */
        public void subscriptionRemoved(SubscriptionEvent evt)
        {
            receivedSubscriptionEvent(evt);
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
                logger.debug(
                        "Collected ContactPropertyChangeEvent("
                        + collectedContactPropertyChangeEvents.size()
                        + ")= "
                        + evt);
                collectedContactPropertyChangeEvents.add(evt);
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
                logger.debug(
                        "Collected evt("
                        + collectedSubscriptionMovedEvents.size()
                        + ")= "
                        + evt);
                collectedSubscriptionMovedEvents.add(evt);
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
            receivedSubscriptionEvent(evt);
        }

        /**
         * Stores the received subsctiption and notifies all waiting on this
         * object
         * @param evt the SubscriptionEvent containing the corresponding contact
         */
        public void subscriptionResolved(SubscriptionEvent evt)
        {
            receivedSubscriptionEvent(evt);
        }

    }

    /**
     * The class would listen for and store received events caused by changes
     * in contact presence states.
     */
    private class ContactPresenceEventCollector
        implements ContactPresenceStatusListener
    {
        public ArrayList<ContactPresenceStatusChangeEvent> collectedEvents
            = new ArrayList<ContactPresenceStatusChangeEvent>();
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
            TestOperationSetPresence.waitForEvent(
                    this,
                    waitFor,
                    collectedEvents);
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

                if( status == null )
                    return;

                if(status != evt.getNewStatus())
                    return;

                logger.debug("Collected evt("+collectedEvents.size()+")= "+evt);
                collectedEvents.add(evt);
                notifyAll();
            }
        }
    }

    /**
     * Blocks until at least one event is received or until waitFor
     * miliseconds pass (whicever happens first).
     * Must be called by with a synchronized collectedEvents Object.
     *
     * @param waitFor the number of miliseconds that we should be waiting
     * for an event before simply bailing out.
     * @param collectedEvents the array used to collect the events. Must be
     * synchronized before calling this function.
     */
    public static void waitForEvent(
            Object eventCollector,
            long waitFor,
            List collectedEvents)
    {
        long startTime = System.currentTimeMillis();
        long elapsedTime = 0;

        synchronized(eventCollector)
        {
            try
            {
                while(collectedEvents.size() == 0
                        && elapsedTime < waitFor)
                {
                    // Wait may be awake by a notifyAll generated by Events
                    // different from those collected by collectedEvents.
                    eventCollector.wait(waitFor - elapsedTime);
                    // Recomputes the time elapsed since the start of this
                    // waitForEvent.
                    elapsedTime = System.currentTimeMillis() - startTime;
                }
            }
            catch (InterruptedException ex)
            {
                logger.debug(
                    "Interrupted while waiting for a subscription evt", ex);
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
                             response.getResponseCode().getCode() + " " +
                             sourceContact);

                notifyAll();
            }
        }

        public void waitForAuthResponse(long waitFor)
        {
            synchronized(this)
            {
                if(isAuthorizationResponseReceived)
                {
                    logger.debug("authorization response already received");
                    return;
                }
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
                {
                    logger.debug("authorization request already received");
                    return;
                }
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
