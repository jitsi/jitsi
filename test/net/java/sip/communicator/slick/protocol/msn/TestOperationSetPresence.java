/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.slick.protocol.msn;

import java.beans.*;
import java.util.*;

import junit.framework.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.service.protocol.msnconstants.*;
import net.java.sip.communicator.util.*;

/**
 * Tests msn implementations of a Presence Operation Set. Tests in this class
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
 */
public class TestOperationSetPresence
    extends TestCase
{
    private static final Logger logger =
        Logger.getLogger(TestOperationSetPresence.class);

    private MsnSlickFixture fixture = new MsnSlickFixture();
    private OperationSetPresence operationSetPresence1 = null;
    private OperationSetPresence operationSetPresence2 = null;

    public TestOperationSetPresence(String name)
    {
        super(name);
    }

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
        //operation set which is unacceptable for msn.
        if (operationSetPresence1 == null)
        {
            throw new NullPointerException(
                "An implementation of the msn service must provide an "
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
                +"this msn implementation. ");

        //get the operation set presence here.
        operationSetPresence2 =
            (OperationSetPresence)supportedOperationSets2.get(
                OperationSetPresence.class.getName());

        //if the op set is null then the implementation doesn't offer a presence
        //operation set which is unacceptable for msn.
        if (operationSetPresence2 == null)
        {
            throw new NullPointerException(
                "An implementation of the msn service must provide an "
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
     * Creates a test suite containing all tests of this class followed by
     * test methods that we want executed in a specified order.
     * @return Test
     */
    public static Test suite()
    {
        //return an (almost) empty suite if we're running in offline mode.
        if(MsnSlickFixture.onlineTestingDisabled)
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

        // add other tests
        suite.addTestSuite(TestOperationSetPresence.class);

        // now test unsubscribe
        suite.addTest(new TestOperationSetPresence("postTestUnsubscribe"));

        return suite;
    }

    /**
     * Verifies that all necessary msn test states are supported by the
     * implementation.
     */
    public void testSupportedStatusSetForCompleteness()
    {
        //first create a local list containing the presence status instances
        //supported by the underlying implementation.
        Iterator<PresenceStatus> supportedStatusSetIter =
            operationSetPresence1.getSupportedStatusSet();

        List<PresenceStatus> supportedStatusSet = new LinkedList<PresenceStatus>();
        while (supportedStatusSetIter.hasNext()){
            supportedStatusSet.add(supportedStatusSetIter.next());
        }

        //create a copy of the MUST status set and remove any matching status
        //that is also present in the supported set.
        List<?> requiredStatusSetCopy
            = (List<?>) MsnStatusEnum.msnStatusSet.clone();

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
        subtestStateTransition(MsnStatusEnum.AWAY);
    }

    /**
     * Verify that changing state to NOT_AVAILABLE works as supposed to and that it
     * generates the corresponding event.
     * @throws Exception in case a failure occurs while the operation set
     * is switching to the new state.
     */
    public void testChangingStateToNotAvailable() throws Exception
    {
        subtestStateTransition(MsnStatusEnum.BE_RIGHT_BACK);
    }

    /**
     * Verify that changing state to DND works as supposed to and that it
     * generates the corresponding event.
     * @throws Exception in case a failure occurs while the operation set
     * is switching to the new state.
     */
    public void testChangingStateToDnd() throws Exception
    {
        subtestStateTransition(MsnStatusEnum.BUSY);
    }

    /**
     * Verify that changing state to FREE_FOR_CHAT works as supposed to and that it
     * generates the corresponding event.
     * @throws Exception in case a failure occurs while the operation set
     * is switching to the new state.
     */
    public void testChangingStateToIdle() throws Exception
    {
        subtestStateTransition(MsnStatusEnum.IDLE);
    }

    /**
     * Verify that changing state to ONLINE works as supposed to and that it
     * generates the corresponding event.
     * @throws Exception in case a failure occurs while the operation set
     * is switching to the new state.
     */
    public void testChangingStateToOnline() throws Exception
    {
        subtestStateTransition(MsnStatusEnum.ONLINE);
    }

    /**
     * Verify that changing state to OUT_TO_LUNCH works as supposed to and that it
     * generates the corresponding event.
     * @throws Exception in case a failure occurs while the operation set
     * is switching to the new state.
     */
    public void testChangingStateToOutToLunch() throws Exception
    {
        subtestStateTransition(MsnStatusEnum.OUT_TO_LUNCH);
    }

    /**
     * Verify that changing state to ON_THE_PHONE works as supposed to and that it
     * generates the corresponding event.
     * @throws Exception in case a failure occurs while the operation set
     * is switching to the new state.
     */
    public void testChangingStateToOnThePhone() throws Exception
    {
        subtestStateTransition(MsnStatusEnum.ON_THE_PHONE);
    }

    /**
     * Used by methods testing state transiotions
     *
     * @param newStatus the MsnStatusEnum field corresponding to the status
     * that we'd like the opeation set to enter.
     *
     * @throws Exception in case changing the state causes an exception
     */
    public void subtestStateTransition( MsnStatusEnum newStatus)
        throws Exception
    {
        logger.trace(" --=== beginning state transition test ===--");

        PresenceStatus oldStatus = operationSetPresence1.getPresenceStatus();

        logger.debug(   "old status is=" + oldStatus.getStatusName()
                     + " new status=" + newStatus.getStatusName());

        //First register a listener to make sure that all corresponding
        //events have been generated.
        PresenceStatusEventCollector statusEventCollector
            = new PresenceStatusEventCollector();
        ContactPresenceEventCollector contactStatusEventCollector
            = new ContactPresenceEventCollector(fixture.userID1, newStatus);
        operationSetPresence1.addProviderPresenceStatusListener(
            statusEventCollector);
        operationSetPresence2.addContactPresenceStatusListener(
            contactStatusEventCollector);

        //change the status
        operationSetPresence1.publishPresenceStatus(newStatus, null);
        pauseAfterStateChanges();

        //test provider event notification.
        statusEventCollector.waitForPresEvent(10000);
        
        // wait for status change in other provider
        // as later its not actually queryed but the last received 
        // status is returned
        contactStatusEventCollector.waitForEvent(10000);

        operationSetPresence1.removeProviderPresenceStatusListener(
            statusEventCollector);
        operationSetPresence2.removeContactPresenceStatusListener(
            contactStatusEventCollector);

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

        logger.trace("will query for contact(" + fixture.userID1 + ") status!");
        
        MsnStatusEnum actualStatus = (MsnStatusEnum)
            operationSetPresence2.queryContactStatus(fixture.userID1);
        
        assertEquals("The underlying implementation did not switch to the "
                     +"requested presence status.",
                     newStatus,
                     actualStatus);

        logger.trace(" --=== finished test ===--");
    }

    /**
     * Give time changes to take effect
     */
    private void pauseAfterStateChanges()
    {
        try
        {
            Thread.sleep(3000);
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
        subtestQueryContactStatus(MsnStatusEnum.AWAY,
                                  MsnStatusEnum.AWAY);

        // --- NA ---
        logger.debug("Will Query an BRB contact.");
        subtestQueryContactStatus(MsnStatusEnum.BE_RIGHT_BACK,
                                  MsnStatusEnum.BE_RIGHT_BACK);

        // --- DND ---
        logger.debug("Will Query a Busy contact.");
        subtestQueryContactStatus(MsnStatusEnum.BUSY,
                                  MsnStatusEnum.BUSY);

        // --- FFC ---
        logger.debug("Will Query a Idle contact.");
        subtestQueryContactStatus(MsnStatusEnum.IDLE,
                                  MsnStatusEnum.IDLE);

        // --- INVISIBLE ---
        logger.debug("Will Query an Invisible contact.");
        subtestQueryContactStatus(MsnStatusEnum.HIDE,
                                  MsnStatusEnum.OFFLINE);

        // --- Online ---
        logger.debug("Will Query an Online contact.");
        subtestQueryContactStatus(MsnStatusEnum.ONLINE,
                                  MsnStatusEnum.ONLINE);
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
    public void subtestQueryContactStatus(PresenceStatus status,
                                          PresenceStatus expectedReturn)
        throws Exception
    {
        operationSetPresence2.publishPresenceStatus(status, "status message");

        pauseAfterStateChanges();

        logger.trace("will query for contact("+ fixture.userID2 + ") status!");
        PresenceStatus actualReturn
            = operationSetPresence1.queryContactStatus(fixture.userID2);
        
        // sometimes happens that no status are received
        // will change the status and try again
        if(!actualReturn.equals(expectedReturn))
        {
            logger.info("subtestQueryContactStatus for " + status + 
                " Failed - trying again!");
            
            // reset the status so we can change it once again
            operationSetPresence2.publishPresenceStatus(status, "status message");
            
            pauseAfterStateChanges();
            
            // now try again
            operationSetPresence2.publishPresenceStatus(status, "status message");

            pauseAfterStateChanges();
            
            actualReturn
                = operationSetPresence1.queryContactStatus(fixture.userID2);
        }
        
        assertEquals("Querying a "
                     + expectedReturn.getStatusName()
                     + " state did not return as expected"
                     , expectedReturn, actualReturn);
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

        SubscriptionEventCollector subEvtCollector
            = new SubscriptionEventCollector();
        operationSetPresence1.addSubscriptionListener(subEvtCollector);


        synchronized (subEvtCollector){
            operationSetPresence1.subscribe(fixture.userID2);
            //we may already have the event, but it won't hurt to check.
            subEvtCollector.waitForEvent(10000);
            operationSetPresence1.removeSubscriptionListener(subEvtCollector);
        }
        
        SubscriptionEventCollector subEvtCollector2
            = new SubscriptionEventCollector();
        operationSetPresence2.addSubscriptionListener(subEvtCollector2);


        synchronized (subEvtCollector2){
            operationSetPresence2.subscribe(fixture.userID1);
            //we may already have the event, but it won't hurt to check.
            subEvtCollector2.waitForEvent(10000);
            operationSetPresence2.removeSubscriptionListener(subEvtCollector2);
        }

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
        MsnStatusEnum oldStatus
            = (MsnStatusEnum)operationSetPresence2.getPresenceStatus();


        MsnStatusEnum newStatus = MsnStatusEnum.IDLE;

        //in case we are by any chance already in a FREE_FOR_CHAT status, we'll
        //be changing to something else
        if(oldStatus.equals(newStatus)){
            newStatus = MsnStatusEnum.BUSY;
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
            contactPresEvtCollector.waitForEvent(10000);
            operationSetPresence1
                .removeContactPresenceStatusListener(contactPresEvtCollector);
        }

        assertEquals("Presence Notif. event dispatching failed."
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

        try
        {
            // add the the user to the reverse side needed for status tests
            subEvtCollector.collectedEvents.clear();
            operationSetPresence2.addSubscriptionListener(subEvtCollector);

            synchronized (subEvtCollector)
            {
                operationSetPresence2.subscribe(fixture.userID1);
                //we may already have the event, but it won't hurt to check.
                subEvtCollector.waitForEvent(10000);
                operationSetPresence2.removeSubscriptionListener(
                    subEvtCollector);
            }
        }
        catch (OperationFailedException ex)
        {
            // happens if the user is already subscribed
        }
        
        Object lock = new Object();
        synchronized(lock)
        {
            logger.info("Will wait all subscriptioin events to be received by lib");
            lock.wait(3000);
            logger.info("Stopped waiting");
        }
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

        Contact msnTesterAgentContact = operationSetPresence1
            .findContactByID(fixture.userID2);

        assertNotNull(
            "Failed to find an existing subscription for the tester agent"
            , msnTesterAgentContact);

        synchronized(subEvtCollector){
            operationSetPresence1.unsubscribe(msnTesterAgentContact);
            subEvtCollector.waitForEvent(10000);
            //don't want any more events
            operationSetPresence1.removeSubscriptionListener(subEvtCollector);
        }

        assertEquals("Subscription event dispatching failed."
                     , 1, subEvtCollector.collectedEvents.size());
        SubscriptionEvent subEvt =
            (SubscriptionEvent)subEvtCollector.collectedEvents.get(0);

        assertEquals("SubscriptionEvent Source:",
                     msnTesterAgentContact, subEvt.getSource());

        assertEquals("SubscriptionEvent Source Contact:",
                     msnTesterAgentContact, subEvt.getSourceContact());

        assertSame("SubscriptionEvent Source Provider:",
                     fixture.provider1,
                     subEvt.getSourceProvider());

        subEvtCollector.collectedEvents.clear();

        // make the user agent tester change its states and make sure we don't
        // get notifications as we're now unsubscribed.
        logger.debug("Testing (lack of) presence notifications.");
        MsnStatusEnum oldStatus
            = (MsnStatusEnum)operationSetPresence2.getPresenceStatus();
        MsnStatusEnum newStatus = MsnStatusEnum.IDLE;

        //in case we are by any chance already in a FREE_FOR_CHAT status, we'll
        //be changing to something else
        if(oldStatus.equals(newStatus)){
            newStatus = MsnStatusEnum.BUSY;
        }

        //now do the actual status notification testing
        ContactPresenceEventCollector contactPresEvtCollector
            = new ContactPresenceEventCollector(fixture.userID2, null);
        operationSetPresence1.addContactPresenceStatusListener(
            contactPresEvtCollector);

        synchronized (contactPresEvtCollector){
            operationSetPresence2.publishPresenceStatus(newStatus, "new status");

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

        Object o = new Object();
        synchronized (o)
        {
            o.wait(10000);
        }
        // wait for a moment
        // give time the impl to get the lists
        logger.debug("start clearing");
        fixture.clearProvidersLists();
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
                {
                    logger.trace("Change already received. " + collectedEvents);
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
        private MsnStatusEnum status = null;

        ContactPresenceEventCollector(String screenname,
                                      MsnStatusEnum wantedStatus)
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
}
