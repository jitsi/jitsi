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

import java.beans.*;
import java.util.*;

import junit.framework.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.service.protocol.icqconstants.*;
import net.java.sip.communicator.util.*;
import net.kano.joscar.snaccmd.*;

/**
 * Tests ICQ implementations of a Presence Operation Set. Tests in this class
 * verify functionality such as: Changing local (our own) status and
 * corresponding event dispatching; Querying status of contacts, Subscribing
 * for presence notifications upong status changes of specific contacts.
 * <p>
 * Using a custom suite() method, we make sure that apart from standard test
 * methods (those with a <tt>test</tt> prefix) we also execute those that
 * we want run in a specific order like for example - postTestSubscribe() and
 * postTestUnsubscribe().
 * <p>
 * @author Emil Ivov
 * @author Damian Minkov
 */
public class TestOperationSetPresence
    extends TestCase
{
    private static final Logger logger =
        Logger.getLogger(TestOperationSetPresence.class);

    private IcqSlickFixture fixture = new IcqSlickFixture();
    private OperationSetPresence    operationSetPresence = null;
    private String statusMessageRoot = new String("Our status is now: ");

    // be sure its only one
    private static AuthEventCollector authEventCollector = new AuthEventCollector();

    public TestOperationSetPresence(String name)
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
        operationSetPresence =
            (OperationSetPresence)supportedOperationSets.get(
                OperationSetPresence.class.getName());

        //if the op set is null then the implementation doesn't offer a presence
        //operation set which is unacceptable for icq.
        if (operationSetPresence == null)
        {
            throw new NullPointerException(
                "An implementation of the ICQ service must provide an "
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
        if(IcqSlickFixture.onlineTestingDisabled)
        {
            TestSuite suite = new TestSuite();
            //the only test around here that we could run without net
            //connectivity
            suite.addTest(
                new TestOperationSetPresence(
                        "testSupportedStatusSetForCompleteness"));
            return suite;
        }

        TestSuite suite = new TestSuite(TestOperationSetPresence.class);

        //the following 2 need to be run in the specified order.
        //(postTestUnsubscribe() needs the subscription created from
        //postTestSubscribe() )
        suite.addTest(new TestOperationSetPresence("postTestSubscribe"));
        suite.addTest(new TestOperationSetPresence("postTestUnsubscribe"));

        // execute this test after postTestSubscribe
        // to be sure that AuthorizationHandler is installed
        suite.addTest(new TestOperationSetPresence("postTestReceiveAuthorizatinonRequest"));

        return suite;
    }

    /**
     * Verifies that all necessary ICQ test states are supported by the
     * implementation.
     */
    public void testSupportedStatusSetForCompleteness()
    {
        //first create a local list containing the presence status instances
        //supported by the underlying implementation.
        Iterator<PresenceStatus> supportedStatusSetIter =
            operationSetPresence.getSupportedStatusSet();

        List<PresenceStatus> supportedStatusSet
            = new LinkedList<PresenceStatus>();
        while (supportedStatusSetIter.hasNext()){
            supportedStatusSet.add(supportedStatusSetIter.next());
        }

        //create a copy of the MUST status set and remove any matching status
        //that is also present in the supported set.
        List<?> requiredStatusSetCopy
            = (List<?>) IcqStatusEnum.icqStatusSet.clone();

        requiredStatusSetCopy.removeAll(supportedStatusSet);

        //if we have anything left then the implementation is wrong.
        int unsupported = requiredStatusSetCopy.size();
        assertTrue( "There are " + unsupported + " statuses as follows:"
                    + requiredStatusSetCopy,
                    unsupported == 0);
    }

    /**
     * Verify that changing state to AWAY works as supposed to and that it
     * generates the corresponding event.
     * @throws Exception in case a failure occurs while the operation set
     * is switching to the new state.
     */
    public void testChangingStateToAway() throws Exception
    {
        subtestStateTransition(IcqStatusEnum.AWAY);
    }

    /**
     * Verify that changing state to NOT_AVAILABLE works as supposed to and that it
     * generates the corresponding event.
     * @throws Exception in case a failure occurs while the operation set
     * is switching to the new state.
     */
    public void testChangingStateToNotAvailable() throws Exception
    {
        subtestStateTransition(IcqStatusEnum.NOT_AVAILABLE);
    }

    /**
     * Verify that changing state to DND works as supposed to and that it
     * generates the corresponding event.
     * @throws Exception in case a failure occurs while the operation set
     * is switching to the new state.
     */
    public void testChangingStateToDnd() throws Exception
    {
        subtestStateTransition(IcqStatusEnum.DO_NOT_DISTURB);
    }

    /**
     * Verify that changing state to INVISIBLE works as supposed to and that it
     * generates the corresponding event.
     * @throws Exception in case a failure occurs while the operation set
     * is switching to the new state.
     */
    public void testChangingStateToInvisible() throws Exception
    {
        subtestStateTransition(IcqStatusEnum.INVISIBLE);
    }

    /**
     * Verify that changing state to OCCUPIED works as supposed to and that it
     * generates the corresponding event.
     * @throws Exception in case a failure occurs while the operation set
     * is switching to the new state.
     */
    public void testChangingStateToOccupied() throws Exception
    {
        subtestStateTransition(IcqStatusEnum.OCCUPIED);
    }

    /**
     * Verify that changing state to FREE_FOR_CHAT works as supposed to and that it
     * generates the corresponding event.
     * @throws Exception in case a failure occurs while the operation set
     * is switching to the new state.
     */
    public void testChangingStateToFreeForChat() throws Exception
    {
        subtestStateTransition(IcqStatusEnum.FREE_FOR_CHAT);
    }

    /**
     * Verify that changing state to ONLINE works as supposed to and that it
     * generates the corresponding event.
     * @throws Exception in case a failure occurs while the operation set
     * is switching to the new state.
     */
    public void testChangingStateToOnline() throws Exception
    {
//        java.util.logging.Logger.getLogger("net.kano").setLevel(java.util.logging.Level.FINEST);
        subtestStateTransition(IcqStatusEnum.ONLINE);
//        java.util.logging.Logger.getLogger("net.kano").setLevel(java.util.logging.Level.WARNING);
    }

    /**
     * Used by methods testing state transiotions
     *
     * @param newStatus the IcqStatusEnum field corresponding to the status
     * that we'd like the opeation set to enter.
     *
     * @throws Exception in case changing the state causes an exception
     */
    public void subtestStateTransition( IcqStatusEnum newStatus)
        throws Exception
    {
        logger.trace(" --=== beginning state transition test ===--");

        PresenceStatus oldStatus = operationSetPresence.getPresenceStatus();
        String oldStatusMessage = operationSetPresence.getCurrentStatusMessage();
        String newStatusMessage = statusMessageRoot + newStatus;

        logger.debug(   "old status is=" + oldStatus.getStatusName()
                     + " new status=" + newStatus.getStatusName());

        //First register a listener to make sure that all corresponding
        //events have been generated.
        PresenceStatusEventCollector statusEventCollector
            = new PresenceStatusEventCollector();
        operationSetPresence.addProviderPresenceStatusListener(
            statusEventCollector);

        //change the status
        operationSetPresence.publishPresenceStatus(newStatus, newStatusMessage);

        //test event notification.
        statusEventCollector.waitForPresEvent(10000);
        statusEventCollector.waitForStatMsgEvent(10000);

        // sometimes we don't get response from the server for the
        // changed status. we will query it once again.
        // and wait for the response
        if(statusEventCollector.collectedPresEvents.size() == 0)
        {
            logger.trace("Will query again status as we haven't received one");
            operationSetPresence.queryContactStatus(IcqSlickFixture.icqAccountID.getUserID());
            statusEventCollector.waitForPresEvent(10000);
        }

        operationSetPresence.removeProviderPresenceStatusListener(
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
            operationSetPresence.getPresenceStatus());

        IcqStatusEnum actualStatus = IcqSlickFixture.testerAgent.getBuddyStatus(
                                    IcqSlickFixture.icqAccountID.getUserID());
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
            operationSetPresence.getCurrentStatusMessage());

        logger.trace(" --=== finished test ===--");
        //make it sleep a bit cause the aol server gets mad otherwise.
        pauseBetweenStateChanges();
    }

    /**
     * The AIM server doesn't like it if we change states too often and we
     * use this method to slow things down.
     */
    private void pauseBetweenStateChanges()
    {
        try
        {
            Thread.sleep(5000);
        }
        catch (InterruptedException ex)
        {
            logger.debug("Pausing between state changes was interrupted", ex);
        }
    }
    /**
     * Verifies that querying status works fine. The ICQ tester agent would
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
        subtestQueryContactStatus(FullUserInfo.ICQSTATUS_AWAY,
                                  IcqStatusEnum.AWAY);

        pauseBetweenStateChanges();

        // --- NA ---
        logger.debug("Will Query an NA contact.");
        subtestQueryContactStatus(FullUserInfo.ICQSTATUS_NA,
                                  IcqStatusEnum.NOT_AVAILABLE);

        pauseBetweenStateChanges();

        // --- DND ---
        logger.debug("Will Query a DND contact.");
        subtestQueryContactStatus(FullUserInfo.ICQSTATUS_DND,
                                  IcqStatusEnum.DO_NOT_DISTURB);

        pauseBetweenStateChanges();

        // --- FFC ---
        logger.debug("Will Query a Free For Chat contact.");
        subtestQueryContactStatus(FullUserInfo.ICQSTATUS_FFC,
                                  IcqStatusEnum.FREE_FOR_CHAT);

        pauseBetweenStateChanges();

        // --- INVISIBLE ---
        logger.debug("Will Query an Invisible contact.");
        subtestQueryContactStatus(FullUserInfo.ICQSTATUS_INVISIBLE,
                                  IcqStatusEnum.INVISIBLE);

        pauseBetweenStateChanges();

        // --- Occupied ---
        logger.debug("Will Query an Occupied contact.");
        subtestQueryContactStatus(FullUserInfo.ICQSTATUS_OCCUPIED,
                                  IcqStatusEnum.OCCUPIED);

        pauseBetweenStateChanges();

        // --- Online ---
        logger.debug("Will Query an Online contact.");
        subtestQueryContactStatus(IcqTesterAgent.ICQ_ONLINE_MASK,
                                  IcqStatusEnum.ONLINE);

        pauseBetweenStateChanges();
    }

    /**
     * Used by functions testing the queryContactStatus method of the
     * presence operation set.
     * @param taStatusLong the icq status as specified by FullUserInfo, that
     * the tester agent should switch to.
     * @param expectedReturn the PresenceStatus that the presence operation
     * set should see the tester agent in once it has switched to taStatusLong.
     *
     * @throws java.lang.Exception if querying the status causes some exception.
     */
    public void subtestQueryContactStatus(long taStatusLong,
                                          PresenceStatus expectedReturn)
        throws Exception
    {
        if ( !IcqSlickFixture.testerAgent.enterStatus(taStatusLong) ){
            throw new RuntimeException(
                "Tester UserAgent Failed to switch to the "
                + expectedReturn.getStatusName() + " state.");
        }

        PresenceStatus actualReturn
            = operationSetPresence.queryContactStatus(
                IcqSlickFixture.testerAgent.getIcqUIN());
        assertEquals("Querying a "
                     + expectedReturn.getStatusName()
                     + " state did not return as expected"
                     , expectedReturn, actualReturn);
    }

    /**
     * The method would add a subscription for a contact, wait for a
     * subscription event confirming the subscription, then change the status
     * of the newly added contact (which is actually the IcqTesterAgent) and
     * make sure that the corresponding notification events have been generated.
     *
     * @throws java.lang.Exception if an exception occurs during testing.
     */
    public void postTestSubscribe()
        throws Exception
    {
        logger.debug("Testing Subscription and Subscription Event Dispatch.");

        // First create a subscription and verify that it really gets created.
        SubscriptionEventCollector subEvtCollector
            = new SubscriptionEventCollector();

        logger.trace("set Auth Handler");
        operationSetPresence.setAuthorizationHandler(authEventCollector);

        synchronized(authEventCollector)
        {
            authEventCollector.authorizationRequestReason =
                "Please deny my request!";
            IcqSlickFixture.testerAgent.getAuthCmdFactory().responseReasonStr =
                "First authorization I will Deny!!!";
            IcqSlickFixture.testerAgent.getAuthCmdFactory().ACCEPT = false;
            operationSetPresence.subscribe(IcqSlickFixture.testerAgent.getIcqUIN());

            // this one collects event that the buddy has been added
            // to the list as awaiting
            SubscriptionEventCollector moveEvtCollector
                = new SubscriptionEventCollector();
            operationSetPresence.addSubscriptionListener(moveEvtCollector);

            logger.debug("Waiting for authorization error and authorization response...");
            authEventCollector.waitForAuthResponse(15000);
            assertTrue("Error adding buddy not recieved or the buddy(" +
                       IcqSlickFixture.testerAgent.getIcqUIN() +
                       ") doesn't require authorization",
                       authEventCollector.isAuthorizationRequestSent);

            assertNotNull("Agent haven't received any reason for authorization",
                       IcqSlickFixture.testerAgent.getAuthCmdFactory().requestReasonStr);
            assertEquals("Error sent request reason is not as the received one",
                         authEventCollector.authorizationRequestReason,
                         IcqSlickFixture.testerAgent.getAuthCmdFactory().requestReasonStr
                );

            logger.debug("authEventCollector.isAuthorizationResponseReceived " +
                         authEventCollector.isAuthorizationResponseReceived);

            assertTrue("Response not received!",
                       authEventCollector.isAuthorizationResponseReceived);

            boolean isAcceptedAuthReuest =
                authEventCollector.response.getResponseCode().equals(AuthorizationResponse.ACCEPT);
            assertEquals("Response is not as the sent one",
                         IcqSlickFixture.testerAgent.getAuthCmdFactory().ACCEPT,
                         isAcceptedAuthReuest);
            assertNotNull("We didn't receive any reason! ",
                       authEventCollector.authorizationResponseString);

            assertEquals("The sent response reason is not as the received one",
                         IcqSlickFixture.testerAgent.getAuthCmdFactory().responseReasonStr,
                         authEventCollector.authorizationResponseString);

            // here we must wait for server to move the awaiting buddy
            // to the first specified  group
            synchronized(moveEvtCollector){
                moveEvtCollector.waitForEvent(20000);
                //don't want any more events
                operationSetPresence.removeSubscriptionListener(moveEvtCollector);
            }

            Contact c = operationSetPresence.findContactByID(
                    IcqSlickFixture.testerAgent.getIcqUIN());
            logger.debug("I will remove " + c +
                         " from group : " + c.getParentContactGroup());

            UnsubscribeWait unsubscribeEvtCollector
                = new UnsubscribeWait();
            operationSetPresence.addSubscriptionListener(unsubscribeEvtCollector);

            synchronized(unsubscribeEvtCollector){
                operationSetPresence.unsubscribe(c);
                logger.debug("Waiting to be removed...");
                unsubscribeEvtCollector.waitForUnsubscribre(20000);

                logger.debug("Received unsubscribed ok or we lost patients!");

                //don't want any more events
                operationSetPresence.removeSubscriptionListener(unsubscribeEvtCollector);
            }

            // so we haven't asserted so everithing is fine lets try to be authorized
            authEventCollector.authorizationRequestReason =
                "Please accept my request!";
            IcqSlickFixture.testerAgent.getAuthCmdFactory().responseReasonStr =
                "Second authorization I will Accept!!!";
            IcqSlickFixture.testerAgent.getAuthCmdFactory().ACCEPT = true;

            // clear some things
            authEventCollector.isAuthorizationRequestSent = false;
            authEventCollector.isAuthorizationResponseReceived = false;
            authEventCollector.authorizationResponseString = null;

            logger.debug("I will add buddy does it exists ?  " +
                         (operationSetPresence.findContactByID(IcqSlickFixture.testerAgent.getIcqUIN()) != null));
            // add the listener beacuse now our authorization will be accepted
            // and so the buddy will be finally added to the list
            operationSetPresence.addSubscriptionListener(subEvtCollector);
            // subscribe again so we can trigger again the authorization procedure
            operationSetPresence.subscribe(IcqSlickFixture.testerAgent.getIcqUIN());

            logger.debug("Waiting ... Subscribe must fail and the authorization process " +
                         "to be trigered again so waiting for auth response ...");
            authEventCollector.waitForAuthResponse(15000);

            assertTrue("Error adding buddy not recieved or the buddy(" +
                       IcqSlickFixture.testerAgent.getIcqUIN() +
                       ") doesn't require authorization",
                       authEventCollector.isAuthorizationRequestSent);

            assertNotNull("Agent haven't received any reason for authorization",
                       IcqSlickFixture.testerAgent.getAuthCmdFactory().requestReasonStr);

            // not working for now
            assertEquals("Error sent request reason",
                         authEventCollector.authorizationRequestReason,
                         IcqSlickFixture.testerAgent.getAuthCmdFactory().requestReasonStr
                );

            // wait for authorization process to be finnished
            // the modification of buddy (server will inform us
            // that he removed - awaiting authorization flag)
            Object obj = new Object();
            synchronized(obj)
            {
                logger.debug("wait for authorization process to be finnished");
                obj.wait(10000);
                logger.debug("Stop waiting!");
            }

            subEvtCollector.waitForEvent(10000);
            //don't want any more events
            operationSetPresence.removeSubscriptionListener(subEvtCollector);
        }

        // after adding awaitingAuthorization group here are catched 3 events
        // 1 - creating unresolved contact
        // 2 - move of the contact to awaitingAuthorization group
        // 3 - move of the contact from awaitingAuthorization group to original group
        assertTrue("Subscription event dispatching failed."
                     , subEvtCollector.collectedEvents.size() > 0);

        EventObject evt = null;

        Iterator<EventObject> events
            = subEvtCollector.collectedEvents.iterator();
        while (events.hasNext())
        {
            EventObject elem = events.next();
            if(elem instanceof SubscriptionEvent)
            {
                if(((SubscriptionEvent)elem).getEventID()
                    == SubscriptionEvent.SUBSCRIPTION_CREATED)
                    evt = elem;
            }
        }

        Object source = null;
        Contact srcContact = null;
        ProtocolProviderService srcProvider = null;

        // the event can be SubscriptionEvent and the new added one
        // SubscriptionMovedEvent

        if(evt instanceof SubscriptionEvent)
        {
            SubscriptionEvent subEvt = (SubscriptionEvent)evt;

            source = subEvt.getSource();
            srcContact = subEvt.getSourceContact();
            srcProvider = subEvt.getSourceProvider();
        }

        assertEquals("SubscriptionEvent Source:",
                     IcqSlickFixture.testerAgent.getIcqUIN(),
                     ((Contact)source).getAddress());
        assertEquals("SubscriptionEvent Source Contact:",
                     IcqSlickFixture.testerAgent.getIcqUIN(),
                     srcContact.getAddress());
        assertSame("SubscriptionEvent Source Provider:",
                     fixture.provider,
                     srcProvider);

        subEvtCollector.collectedEvents.clear();

        // make the user agent tester change its states and make sure we are
        // notified
        logger.debug("Testing presence notifications.");
        IcqStatusEnum testerAgentOldStatus
            = IcqSlickFixture.testerAgent.getPresneceStatus();
        IcqStatusEnum testerAgentNewStatus = IcqStatusEnum.FREE_FOR_CHAT;
        long testerAgentNewStatusLong = FullUserInfo.ICQSTATUS_FFC;

        //in case we are by any chance already in a FREE_FOR_CHAT status, we'll
        //be changing to something else
        if(testerAgentOldStatus.equals(testerAgentNewStatus)){
            testerAgentNewStatus = IcqStatusEnum.DO_NOT_DISTURB;
            testerAgentNewStatusLong = FullUserInfo.ICQSTATUS_DND;
        }

        //now do the actual status notification testing
        ContactPresenceEventCollector contactPresEvtCollector
            = new ContactPresenceEventCollector(
                    IcqSlickFixture.testerAgent.getIcqUIN(), testerAgentNewStatus);
        operationSetPresence.addContactPresenceStatusListener(
            contactPresEvtCollector);

        synchronized (contactPresEvtCollector){
            if (!IcqSlickFixture.testerAgent.enterStatus(testerAgentNewStatusLong))
            {
                throw new RuntimeException(
                    "Tester UserAgent Failed to switch to the "
                    + testerAgentNewStatus.getStatusName() + " state.");
            }
            //we may already have the event, but it won't hurt to check.
            contactPresEvtCollector.waitForEvent(12000);
            operationSetPresence
                .removeContactPresenceStatusListener(contactPresEvtCollector);
        }

        if(contactPresEvtCollector.collectedEvents.size() == 0)
        {
            logger.info("PROBLEM. Authorisation process doesn't have finnished " +
                "Server doesn't report us for changing authorization flag! Will try to authorize once again");

            IcqSlickFixture.testerAgent.sendAuthorizationReplay(
                IcqSlickFixture.icqAccountID.getUserID(),
                IcqSlickFixture.testerAgent.getAuthCmdFactory().responseReasonStr,
                IcqSlickFixture.testerAgent.getAuthCmdFactory().ACCEPT);

            Object obj = new Object();
            synchronized(obj)
            {
                logger.debug("wait for authorization process to be finnished for second time");
                obj.wait(10000);
                logger.debug("Stop waiting!");
            }

            testerAgentOldStatus = IcqSlickFixture.testerAgent.getPresneceStatus();
            testerAgentNewStatusLong = FullUserInfo.ICQSTATUS_FFC;

            //in case we are by any chance already in a FREE_FOR_CHAT status, we'll
            //be changing to something else
            if(testerAgentOldStatus.equals(testerAgentNewStatus)){
                testerAgentNewStatus = IcqStatusEnum.OCCUPIED;
                testerAgentNewStatusLong = FullUserInfo.ICQSTATUS_OCCUPIED;
            }

            contactPresEvtCollector = new ContactPresenceEventCollector(
                    IcqSlickFixture.testerAgent.getIcqUIN(), testerAgentNewStatus);
            operationSetPresence.addContactPresenceStatusListener(
                contactPresEvtCollector);

            synchronized (contactPresEvtCollector){
                if (!IcqSlickFixture.testerAgent.enterStatus(testerAgentNewStatusLong))
                {
                    throw new RuntimeException(
                        "Tester UserAgent Failed to switch to the "
                        + testerAgentNewStatus.getStatusName() + " state.");
                }
                //we may already have the event, but it won't hurt to check.
                contactPresEvtCollector.waitForEvent(12000);
                operationSetPresence
                    .removeContactPresenceStatusListener(contactPresEvtCollector);
            }
        }


        assertEquals("Presence Notif. event dispatching failed."
                     , 1, contactPresEvtCollector.collectedEvents.size());
        ContactPresenceStatusChangeEvent presEvt =
            (ContactPresenceStatusChangeEvent)
                contactPresEvtCollector.collectedEvents.get(0);

        assertEquals("Presence Notif. event  Source:",
                     IcqSlickFixture.testerAgent.getIcqUIN(),
                     ((Contact)presEvt.getSource()).getAddress());
        assertEquals("Presence Notif. event  Source Contact:",
                     IcqSlickFixture.testerAgent.getIcqUIN(),
                     presEvt.getSourceContact().getAddress());
        assertSame("Presence Notif. event  Source Provider:",
                     fixture.provider,
                     presEvt.getSourceProvider());

        PresenceStatus reportedNewStatus = presEvt.getNewStatus();
        PresenceStatus reportedOldStatus = presEvt.getOldStatus();

        assertEquals( "Reported new PresenceStatus: ",
                      testerAgentNewStatus, reportedNewStatus );

        //don't require equality between the reported old PresenceStatus and
        //the actual presence status of the tester agent because a first
        //notification is not supposed to have the old status as it really was.
        assertNotNull( "Reported old PresenceStatus: ", reportedOldStatus );

        /** @todo tester agent changes status message we see the new message */
        /** @todo we should see the alias of the tester agent. */

        Object obj = new Object();
        synchronized(obj)
        {
            logger.debug("wait a moment. give time to server");
            obj.wait(4000);
        }
    }

    /**
     * We unsubscribe from presence notification deliveries concerning
     * IcqTesterAgent's presence status and verify that we receive the
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
        operationSetPresence.addSubscriptionListener(subEvtCollector);

        Contact icqTesterAgentContact = operationSetPresence
            .findContactByID(IcqSlickFixture.testerAgent.getIcqUIN());

        assertNotNull(
            "Failed to find an existing subscription for the tester agent"
            , icqTesterAgentContact);

        synchronized(subEvtCollector)
        {
            operationSetPresence.unsubscribe(icqTesterAgentContact);
            subEvtCollector.waitForEvent(40000);
            //don't want any more events
            operationSetPresence.removeSubscriptionListener(subEvtCollector);
        }

        assertEquals("Subscription event dispatching failed."
                     , 1, subEvtCollector.collectedEvents.size());
        SubscriptionEvent subEvt =
            (SubscriptionEvent)subEvtCollector.collectedEvents.get(0);

        assertEquals("SubscriptionEvent Source:",
                     icqTesterAgentContact, subEvt.getSource());

        assertEquals("SubscriptionEvent Source Contact:",
                     icqTesterAgentContact, subEvt.getSourceContact());

        assertSame("SubscriptionEvent Source Provider:",
                     fixture.provider,
                     subEvt.getSourceProvider());

        subEvtCollector.collectedEvents.clear();

        // make the user agent tester change its states and make sure we don't
        // get notifications as we're now unsubscribed.
        logger.debug("Testing (lack of) presence notifications.");
        IcqStatusEnum testerAgentOldStatus
            = IcqSlickFixture.testerAgent.getPresneceStatus();
        IcqStatusEnum testerAgentNewStatus = IcqStatusEnum.FREE_FOR_CHAT;
        long testerAgentNewStatusLong = FullUserInfo.ICQSTATUS_FFC;

        //in case we are by any chance already in a FREE_FOR_CHAT status, we'll
        //be changing to something else
        if(testerAgentOldStatus.equals(testerAgentNewStatus))
        {
            testerAgentNewStatus = IcqStatusEnum.DO_NOT_DISTURB;
            testerAgentNewStatusLong = FullUserInfo.ICQSTATUS_DND;
        }

        //now do the actual status notification testing
        ContactPresenceEventCollector contactPresEvtCollector
            = new ContactPresenceEventCollector(
                        IcqSlickFixture.testerAgent.getIcqUIN(), null);
        operationSetPresence.addContactPresenceStatusListener(
            contactPresEvtCollector);

        synchronized (contactPresEvtCollector)
        {
            if (!IcqSlickFixture.testerAgent.enterStatus(testerAgentNewStatusLong))
            {
                throw new RuntimeException(
                    "Tester UserAgent Failed to switch to the "
                    + testerAgentNewStatus.getStatusName() + " state.");
            }
            //we may already have the event, but it won't hurt to check.
            contactPresEvtCollector.waitForEvent(10000);
            operationSetPresence
                .removeContactPresenceStatusListener(contactPresEvtCollector);
        }

        assertEquals("Presence Notifications were received after unsubscibing."
                     , 0, contactPresEvtCollector.collectedEvents.size());
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
         * milliseconds pass (whichever happens first).
         *
         * @param waitFor the number of milliseconds that we should be waiting
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
         * Blocks until at least one status message event is received or until
         * waitFor milliseconds pass (whichever happens first).
         *
         * @param waitFor the number of milliseconds that we should be waiting
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
         * milliseconds pass (whichever happens first).
         *
         * @param waitFor the number of milliseconds that we should be waiting
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
         * Stores the received subscription and notifies all waiting on this
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
         * Stores the received subscription and notifies all waiting on this
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
         * Stores the received subscription and notifies all waiting on this
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
         * Stores the received subscription and notifies all waiting on this
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
         * Stores the received subscription and notifies all waiting on this
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
         * Stores the received subscription and notifies all waiting on this
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
        private IcqStatusEnum status = null;

        ContactPresenceEventCollector(String screenname,
                                      IcqStatusEnum wantedStatus)
        {
            this.trackedScreenName = screenname;
            this.status = wantedStatus;
        }

        /**
         * Blocks until at least one event is received or until waitFor
         * milliseconds pass (whichever happens first).
         *
         * @param waitFor the number of milliseconds that we should be waiting
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
        String authorizationRequestReason = null;

        boolean isAuthorizationResponseReceived = false;
        AuthorizationResponse response = null;
        String authorizationResponseString = null;

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
                authorizationRequestReason = req.getReason();

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
            authReq.setReason(authorizationRequestReason);

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
                authorizationResponseString = response.getReason();

                logger.trace("processAuthorizationResponse '" +
                             authorizationResponseString + "' " +
                             response.getResponseCode() + " " +
                             sourceContact);

                notifyAll();
            }
        }

        public void waitForAuthResponse(long waitFor)
        {
            synchronized(this)
            {
                if(isAuthorizationResponseReceived) return;
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
                if(isAuthorizationRequestReceived) return;
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

    /**
     * Used to wait till buddy is removed from our contact list.
     * Used in the authorization process tests
     */
    private static class UnsubscribeWait
        extends SubscriptionAdapter
    {
        public void waitForUnsubscribre(long waitFor)
        {
            synchronized(this)
            {
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

        @Override
        public void subscriptionRemoved(SubscriptionEvent evt)
        {
            synchronized(this)
            {
                logger.debug("Got subscriptionRemoved " + evt);
                notifyAll();
            }
        }
    }

    /**
     * Tests for receiving authorization requests
     */
    public void postTestReceiveAuthorizatinonRequest()
    {
        logger.debug("Testing receive of authorization request!");

        // set first response isAccepted and responseString
        // the first authorization process is negative
        // the agent try to add us to his contact list and ask us for
        // authorization but we deny him
        String firstRequestResponse = "First Request will be denied!!!";
        authEventCollector.responseToRequest = new AuthorizationResponse(AuthorizationResponse.REJECT, firstRequestResponse);
        logger.debug("authEventCollector " + authEventCollector);
        authEventCollector.isAuthorizationRequestReceived = false;
        authEventCollector.authorizationRequestReason = null;
        IcqSlickFixture.testerAgent.getAuthCmdFactory().requestReasonStr = "Deny my first request!";
        IcqSlickFixture.testerAgent.getAuthCmdFactory().isErrorAddingReceived = false;
        IcqSlickFixture.testerAgent.getAuthCmdFactory().responseReasonStr = null;
        IcqSlickFixture.testerAgent.getAuthCmdFactory().isRequestAccepted = false;

        // be sure buddy is not already in the list
        IcqSlickFixture.testerAgent.deleteBuddy(fixture.ourUserID);
        IcqSlickFixture.testerAgent.addBuddy(fixture.ourUserID);

        // wait agent to receive error and to request us for our authorization
        authEventCollector.waitForAuthRequest(25000);

        // check have we received authorization request?
        assertTrue("Error adding buddy not recieved or the buddy(" +
                       fixture.ourUserID +
                       ") doesn't require authorization 1",
                       IcqSlickFixture.testerAgent.getAuthCmdFactory().isErrorAddingReceived);

        assertTrue("We haven't received any authorization request ",
                       authEventCollector.isAuthorizationRequestReceived);

        assertNotNull("We haven't received any reason for authorization",
                      authEventCollector.authorizationRequestReason);

        assertEquals("Error sent request reason is not as the received one",
                     IcqSlickFixture.testerAgent.getAuthCmdFactory().requestReasonStr,
                     authEventCollector.authorizationRequestReason
        );

        // wait agent to receive our response
        Object lock = new Object();
        synchronized(lock)
        {
            try{
                lock.wait(5000);
            }
            catch (Exception ex){}
        }


        // check is correct - the received response from the agent
        assertNotNull("Agent haven't received any reason from authorization reply",
                      authEventCollector.authorizationRequestReason);

        assertEquals("Received auth response from agent is not as the sent one",
                     IcqSlickFixture.testerAgent.getAuthCmdFactory().responseReasonStr,
                     firstRequestResponse);

        boolean isAcceptedAuthReuest =
                authEventCollector.responseToRequest.getResponseCode().equals(AuthorizationResponse.ACCEPT);
        assertEquals("Agent received Response is not as the sent one",
                     IcqSlickFixture.testerAgent.getAuthCmdFactory().isRequestAccepted,
                     isAcceptedAuthReuest);

        // delete us from his list
        // be sure buddy is not already in the list
        IcqSlickFixture.testerAgent.deleteBuddy(fixture.ourUserID);

        // set second response isAccepted and responseString
        // the second test is the same as first, but this time we accept
        // the request and check that everything is OK.
        String secondRequestResponse = "Second Request will be accepted!!!";
        authEventCollector.responseToRequest =
            new AuthorizationResponse(AuthorizationResponse.ACCEPT, secondRequestResponse);
        authEventCollector.isAuthorizationRequestReceived = false;
        authEventCollector.authorizationRequestReason = null;
        IcqSlickFixture.testerAgent.getAuthCmdFactory().requestReasonStr = "Accept my second request!";
        IcqSlickFixture.testerAgent.getAuthCmdFactory().isErrorAddingReceived = false;
        IcqSlickFixture.testerAgent.getAuthCmdFactory().responseReasonStr = null;
        IcqSlickFixture.testerAgent.getAuthCmdFactory().isRequestAccepted = false;

        // add us to his list again
        IcqSlickFixture.testerAgent.addBuddy(fixture.ourUserID);

        // wait agent to receive error and to request us for our authorization
        authEventCollector.waitForAuthRequest(25000);

        // check have we received authorization request?
        assertTrue("Error adding buddy not recieved or the buddy(" +
                              fixture.ourUserID +
                              ") doesn't require authorization 2",
                              IcqSlickFixture.testerAgent.getAuthCmdFactory().isErrorAddingReceived);

       assertTrue("We haven't received any authorization request ",
                      authEventCollector.isAuthorizationRequestReceived);

       assertNotNull("We haven't received any reason for authorization",
                     authEventCollector.authorizationRequestReason);

       assertEquals("Error sent request reason is not as the received one",
                    IcqSlickFixture.testerAgent.getAuthCmdFactory().requestReasonStr,
                    authEventCollector.authorizationRequestReason
        );
        // wait agent to receive our response
        synchronized(lock)
        {
            try{
                lock.wait(5000);
            }
            catch (Exception ex){}
        }
        // check is correct the received response from the agent
        assertNotNull("Agent haven't received any reason from authorization reply",
                              authEventCollector.authorizationRequestReason);

        assertEquals("Received auth response from agent is not as the sent one",
                     IcqSlickFixture.testerAgent.getAuthCmdFactory().responseReasonStr,
                     secondRequestResponse);

        isAcceptedAuthReuest =
                authEventCollector.responseToRequest.getResponseCode().equals(AuthorizationResponse.ACCEPT);
        assertEquals("Agent received Response is not as the sent one",
                     IcqSlickFixture.testerAgent.getAuthCmdFactory().isRequestAccepted,
             isAcceptedAuthReuest);
    }
}
