/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.slick.protocol.icq;

import junit.framework.*;

import net.java.sip.communicator.service.protocol.*;
import java.util.*;

import net.java.sip.communicator.service.protocol.icqconstants.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;
import net.kano.joscar.snaccmd.*;
import java.beans.PropertyChangeEvent;

/**
 * Tests ICQ implementations of a Presence Operation Set. Tests in this class
 * verify functionality such as: Changing local (our own) status and
 * corresponding event dispatching; Querying status of contacts, Subscribing
 * for presence notifications upong status changes of specific contacts.
 * <p>
 * Using a custom suite() method, we make sure that apart from standard test
 * methods (those with a <code>test</code> prefix) we also execute those that
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

    private IcqSlickFixture fixture = new IcqSlickFixture();
    private OperationSetPresence    operationSetPresence = null;
    private String statusMessageRoot = new String("Our status is now: ");

    public TestOperationSetPresence(String name)
    {
        super(name);
    }

    protected void setUp() throws Exception
    {
        super.setUp();
        fixture.setUp();

        Map supportedOperationSets =
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

        if (operationSetPresence == null)
        {
            //if the operation set is still null, it's maybe because the impl
            //only registers a persistence operation set. Let's see if that is
            //the case.
            operationSetPresence =
                (OperationSetPersistentPresence) supportedOperationSets.get(
                    OperationSetPersistentPresence.class.getName());

            //if still null then the implementation doesn't offer a presence
            //operation set which is unacceptable for icq.
            if (operationSetPresence == null)
                throw new NullPointerException(
                    "An implementation of the ICQ service must provide an "
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
        TestSuite suite = new TestSuite(TestOperationSetPresence.class);

        //the following 2 need to be run in the specified order.
        //(postTestUnsubscribe() needs the subscription created from
        //postTestSubscribe() )
        suite.addTest(new TestOperationSetPresence("postTestSubscribe"));
        suite.addTest(new TestOperationSetPresence("postTestUnsubscribe"));

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
        Iterator supportedStatusSetIter =
            operationSetPresence.getSupportedStatusSet();

        List supportedStatusSet = new LinkedList();
        while (supportedStatusSetIter.hasNext()){
            supportedStatusSet.add(supportedStatusSetIter.next());
        }

        //create a copy of the MUST status set and remove any matching status
        //that is also present in the supported set.
        List requiredStatusSetCopy = (List)IcqStatusEnum.icqStatusSet.clone();

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
        java.util.logging.Logger.getLogger("net.kano").setLevel(java.util.logging.Level.FINEST);
        subtestStateTransition(IcqStatusEnum.ONLINE);
        java.util.logging.Logger.getLogger("net.kano").setLevel(java.util.logging.Level.WARNING);
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

        IcqStatusEnum actualStatus = fixture.testerAgent.getBuddyStatus(
                                    fixture.icqAccountID.getAccountID());
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
            Thread.currentThread().sleep(2000);
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
        if ( !fixture.testerAgent.enterStatus(taStatusLong) ){
            throw new RuntimeException(
                "Tester UserAgent Failed to switch to the "
                + expectedReturn.getStatusName() + " state.");
        }

        PresenceStatus actualReturn
            = operationSetPresence.queryContactStatus(
                fixture.testerAgent.getIcqUIN());
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
        operationSetPresence.addSubsciptionListener(subEvtCollector);

        synchronized(subEvtCollector){
            operationSetPresence.subscribe(fixture.testerAgent.getIcqUIN());
            subEvtCollector.waitForEvent(10000);
            //don't want any more events
            operationSetPresence.removeSubsciptionListener(subEvtCollector);
        }

        assertEquals("Subscription event dispatching failed."
                     , 1, subEvtCollector.collectedEvents.size());
        SubscriptionEvent subEvt =
            (SubscriptionEvent)subEvtCollector.collectedEvents.get(0);

        assertEquals("SubscriptionEvent Source:",
                     fixture.testerAgent.getIcqUIN(),
                     ((Contact)subEvt.getSource()).getAddress());
        assertEquals("SubscriptionEvent Source Contact:",
                     fixture.testerAgent.getIcqUIN(),
                     subEvt.getSourceContact().getAddress());
        assertSame("SubscriptionEvent Source Provider:",
                     fixture.provider,
                     subEvt.getSourceProvider());

        subEvtCollector.collectedEvents.clear();

        // make the user agent tester change its states and make sure we are
        // notified
        logger.debug("Testing presence notifications.");
        IcqStatusEnum testerAgentOldStatus
            = fixture.testerAgent.getPresneceStatus();
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
                    fixture.testerAgent.getIcqUIN(), testerAgentNewStatus);
        operationSetPresence.addContactPresenceStatusListener(
            contactPresEvtCollector);

        synchronized (contactPresEvtCollector){
            if (!fixture.testerAgent.enterStatus(testerAgentNewStatusLong))
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

        assertEquals("Presence Notif. event dispatching failed."
                     , 1, contactPresEvtCollector.collectedEvents.size());
        ContactPresenceStatusChangeEvent presEvt =
            (ContactPresenceStatusChangeEvent)
                contactPresEvtCollector.collectedEvents.get(0);

        assertEquals("Presence Notif. event  Source:",
                     fixture.testerAgent.getIcqUIN(),
                     ((Contact)presEvt.getSource()).getAddress());
        assertEquals("Presence Notif. event  Source Contact:",
                     fixture.testerAgent.getIcqUIN(),
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
        operationSetPresence.addSubsciptionListener(subEvtCollector);

        Contact icqTesterAgentContact = operationSetPresence
            .findContactByID(fixture.testerAgent.getIcqUIN());

        assertNotNull(
            "Failed to find an existing subscription for the tester agent"
            , icqTesterAgentContact);

        synchronized(subEvtCollector){
            operationSetPresence.unsubscribe(icqTesterAgentContact);
            subEvtCollector.waitForEvent(10000);
            //don't want any more events
            operationSetPresence.removeSubsciptionListener(subEvtCollector);
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
            = fixture.testerAgent.getPresneceStatus();
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
                        fixture.testerAgent.getIcqUIN(), null);
        operationSetPresence.addContactPresenceStatusListener(
            contactPresEvtCollector);

        synchronized (contactPresEvtCollector){
            if (!fixture.testerAgent.enterStatus(testerAgentNewStatusLong))
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
        public ArrayList collectedPresEvents = new ArrayList();
        public ArrayList collectedStatMsgEvents = new ArrayList();

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
            synchronized(this){
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
            synchronized(this){
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
        public ArrayList collectedEvents = new ArrayList();

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
        public void subscriptionFailed(SubscriptionEvent evt)
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
        public ArrayList collectedEvents = new ArrayList();
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
