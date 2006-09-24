/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.slick.protocol.sip;

import junit.framework.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.service.protocol.*;
import java.text.ParseException;
import net.java.sip.communicator.service.protocol.event.*;
import java.util.*;

/**
 * Tests Basic telephony functionality by making one provider call the other.
 *
 * @author Emil Ivov
 */
public class TestOperationSetBasicTelephonySipImpl
    extends TestCase
{
    private static final Logger logger
        = Logger.getLogger(TestOperationSetBasicTelephonySipImpl.class);

    /**
     *
     */
    private SipSlickFixture fixture = new SipSlickFixture();

    /**
     * Initializes the test with the specified <tt>name</tt>.
     *
     * @param name the name of the test to initialize.
     */
    public TestOperationSetBasicTelephonySipImpl(String name)
    {
        super(name);
    }

    /**
     * JUnit setup method.
     * @throws Exception in case anything goes wrong.
     */
    protected void setUp() throws Exception
    {
        super.setUp();
        fixture.setUp();
    }

    /**
     * JUnit teardown method.
     * @throws Exception in case anything goes wrong.
     */
    protected void tearDown() throws Exception
    {
        fixture.tearDown();
        super.tearDown();
    }

    /**
     * Creates a call from provider1 to provider2 then cancels it without
     * waiting for provider1 to answer.
     *
     * @throws ParseException if we hand a malformed URI to someone
     * @throws OperationFailedException if something weird happens.
     */
    public void testCreateCancelCall()
        throws ParseException, OperationFailedException
    {
        OperationSetBasicTelephony basicTelephonyP1
            = (OperationSetBasicTelephony)fixture.provider1.getOperationSet(
                OperationSetBasicTelephony.class);
        OperationSetBasicTelephony basicTelephonyP2
            = (OperationSetBasicTelephony)fixture.provider2.getOperationSet(
                OperationSetBasicTelephony.class);

        CallEventCollector call1Listener
            = new CallEventCollector(basicTelephonyP1);
        CallEventCollector call2Listener
            = new CallEventCollector(basicTelephonyP2);

        //Provider1 calls Provider2
        String provider2Address
            = fixture.provider2.getAccountID().getAccountAddress();

        Call callAtP1 = basicTelephonyP1.createCall(provider2Address);

        call1Listener.waitForEvent(10000);
        call2Listener.waitForEvent(10000);

        //make sure that both listeners have received their events.
        assertEquals("The provider that created the call did not dispatch "
                     + "an event that it has done so."
                     , 1, call1Listener.collectedEvents.size());
        //call1 listener checks
        CallEvent callCreatedEvent
            = (CallEvent)call1Listener.collectedEvents.get(0);

        assertEquals("CallEvent.getEventID()"
                     ,CallEvent.CALL_INITIATED, callCreatedEvent.getEventID());
        assertSame("CallEvent.getSource()"
                     , callAtP1, callCreatedEvent.getSource());

        //call2 listener checks
        assertTrue("The callee provider did not receive a call or did "
                     +"not issue an event."
                     , call2Listener.collectedEvents.size() > 0);
        CallEvent callReceivedEvent
            = (CallEvent)call2Listener.collectedEvents.get(0);
        Call callAtP2 = callReceivedEvent.getSourceCall();

        assertEquals("CallEvent.getEventID()"
                     ,CallEvent.CALL_RECEIVED, callReceivedEvent.getEventID());
        assertNotNull("CallEvent.getSource()", callAtP2);

        //verify that call participants are properly created
        assertEquals("callAtP1.getCallParticipantsCount()"
                     , 1, callAtP1.getCallParticipantsCount());
        assertEquals("callAtP2.getCallParticipantsCount()"
                     , 1, callAtP2.getCallParticipantsCount());

        CallParticipant participantAtP1
            = (CallParticipant)callAtP1.getCallParticipants().next();
        CallParticipant participantAtP2
            = (CallParticipant)callAtP2.getCallParticipants().next();

        //now add listeners to the participants and make sure they have entered
        //the states they were expected to.
        //check states for call participants at both parties
        CallParticipantStateEventCollector stateCollectorForPp1
            = new CallParticipantStateEventCollector(
                participantAtP1, CallParticipantState.ALERTING_REMOTE_SIDE);
        CallParticipantStateEventCollector stateCollectorForPp2
            = new CallParticipantStateEventCollector(
                participantAtP2, CallParticipantState.INCOMING_CALL);

        participantAtP1.addCallParticipantListener(stateCollectorForPp1);
        participantAtP2.addCallParticipantListener(stateCollectorForPp2);

        stateCollectorForPp1.waitForEvent(10000);
        stateCollectorForPp2.waitForEvent(10000);

        //check properties on the remote call participant for the party that
        //initiated the call.
        String expectedParticipant1Address
            = fixture.provider2.getAccountID().getAccountAddress();
        String expectedParticipant1DisplayName
            = System.getProperty(
                SipProtocolProviderServiceLick.ACCOUNT_2_PREFIX
                + ProtocolProviderFactory.DISPLAY_NAME);

        //do not asser equals here since one of the addresses may contain a
        //display name or something of the kind
        assertTrue("Provider 2 did not advertise their "
                     +"accountID.getAccoutAddress() address."
                     , expectedParticipant1Address.indexOf(
                                        participantAtP1.getAddress())!= -1
                       || participantAtP1.getAddress().indexOf(
                                        expectedParticipant1Address)!= -1);
        assertEquals("Provider 2 did not properly advertise their "
                     +"display name."
                     , expectedParticipant1DisplayName
                     , participantAtP1.getDisplayName());
        assertSame("participantAtP1.getCall"
                   , participantAtP1.getCall(), callAtP1);

        //check properties on the remote call participant for the party that
        //receives the call.
        String expectedParticipant2Address
            = fixture.provider1.getAccountID().getAccountAddress();
        String expectedParticipant2DisplayName
            = System.getProperty(
                SipProtocolProviderServiceLick.ACCOUNT_1_PREFIX
                + ProtocolProviderFactory.DISPLAY_NAME);

        //do not asser equals here since one of the addresses may contain a
        //display name or something of the kind
        assertTrue("Provider 1 did not advertise their "
                     +"accountID.getAccoutAddress() address."
                     , expectedParticipant2Address.indexOf(
                                        participantAtP2.getAddress())!= -1
                       || participantAtP2.getAddress().indexOf(
                                        expectedParticipant2Address)!= -1);
        assertEquals("Provider 1 did not properly advertise their "
                     +"display name."
                     , expectedParticipant2DisplayName
                     , participantAtP2.getDisplayName());
        assertSame("participantAtP2.getCall"
                   , participantAtP2.getCall(), callAtP2);

        //make sure that the participants are in the proper state
        assertEquals("The participant at provider one was not in the "
                     +"right state."
                    , CallParticipantState.ALERTING_REMOTE_SIDE
                    , participantAtP1.getState());
        assertEquals("The participant at provider two was not in the "
                     +"right state."
                    , CallParticipantState.INCOMING_CALL
                    , participantAtP2.getState());



    }

    /**
     * Creates a call from provider1 to provider2 then rejects the call from the
     * side of provider2.
     */
    public void testCreateRejectCall()
    {

    }

    /**
     * Creates a call from provider1 to provider2, makes provider2 answer it
     * and then reject it.
     */
    public void testCreateAnswerHangupCall()
    {

    }

    /**
     * Allows tests to wait for and collect events issued upon creation and
     * reception of calls.
     */
    public class CallEventCollector implements CallListener
    {
        public ArrayList collectedEvents = new ArrayList();
        public OperationSetBasicTelephony listenedOpSet = null;

        /**
         * Creates an instance of this call event collector and registers it
         * with listenedOpSet.
         * @param listenedOpSet the operation set that we will be scanning for
         * new calls.
         */
        public CallEventCollector(OperationSetBasicTelephony listenedOpSet)
        {
            this.listenedOpSet = listenedOpSet;
            this.listenedOpSet.addCallListener(this);
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
            logger.trace("Waiting for a CallEvent");

            synchronized(this)
            {
                if(collectedEvents.size() > 0){
                    logger.trace("Event already received. " + collectedEvents);
                    return;
                }

                try{
                    wait(waitFor);
                    if(collectedEvents.size() > 0)
                        logger.trace("Received a CallEvent.");
                    else
                        logger.trace("No CallEvent received for "+waitFor+"ms.");

                    listenedOpSet.removeCallListener(this);
                }
                catch (InterruptedException ex)
                {
                    logger.debug(
                        "Interrupted while waiting for a call event", ex);
                }
            }
        }

        /**
         * Stores the received event and notifies all waiting on this object
         * @param event the event containing the source call.
         */
        public void incomingCallReceived(CallEvent event)
        {
            synchronized(this)
            {
                logger.debug(
                    "Collected evt("+collectedEvents.size()+")= "+event);

                this.collectedEvents.add(event);
                notifyAll();
            }
        }

        /**
         * Stores the received event and notifies all waiting on this object
         * @param event the event containing the source call.
         */
        public void outgoingCallCreated(CallEvent event)
        {
            synchronized(this)
            {
                logger.debug(
                    "Collected evt("+collectedEvents.size()+")= "+event);

                this.collectedEvents.add(event);
                notifyAll();
            }

        }

        /**
         * Unused by this collector.
         * @param event ignored.
         */
        public void callEnded(CallEvent event)
        {}

    }

    /**
     * Allows tests to wait for and collect events issued upon call participant
     * status changes.
     */
    public class CallParticipantStateEventCollector
        implements CallParticipantListener
    {
        public ArrayList collectedEvents = new ArrayList();
        private CallParticipant listenedCallParticipant = null;
        public CallParticipantState awaitedState = null;

        /**
         * Creates an instance of this collector and adds it as a listener
         * to <tt>callParticipant</tt>.
         * @param callParticipant the CallParticipant that we will be listening
         * to.
         * @param awaitedState the state that we will be waiting for inside
         * this collector.
         */
        public CallParticipantStateEventCollector(
                                            CallParticipant      callParticipant,
                                            CallParticipantState awaitedState)
        {
            this.listenedCallParticipant = callParticipant;
            this.listenedCallParticipant.addCallParticipantListener(this);
            this.awaitedState = awaitedState;
        }

        /**
         * Stores the received event and notifies all waiting on this object
         *
         * @param event the event containing the source call.
         */
        public void participantStateChanged(CallParticipantChangeEvent event)
        {
            synchronized(this)
            {
                logger.debug(
                    "Collected evt("+collectedEvents.size()+")= "+event);

                this.collectedEvents.add(event);
                if(((CallParticipantState)event.getNewValue())
                    .equals(awaitedState))
                {
                    notifyAll();
                }
            }
        }

        /**
         * Unused by this collector.
         * @param event ignored.
         */
        public void participantImageChanged(CallParticipantChangeEvent event)
        {}

        /**
         * Unused by this collector
         * @param event ignored.
         */
        public void participantAddressChanged(CallParticipantChangeEvent event)
        {}

        /**
         * Unused by this collector
         * @param event ignored.
         */
        public void participantDisplayNameChanged(
                                            CallParticipantChangeEvent event)
        {}

        /**
         * Blocks until an event notifying us of the awaited state change is
         * received or until waitFor miliseconds pass (whichever happens first).
         *
         * @param waitFor the number of miliseconds that we should be waiting
         * for an event before simply bailing out.
         */
        public void waitForEvent(long waitFor)
        {
            logger.trace("Waiting for a CallParticpantEvent");
            synchronized (this)
            {
                if (listenedCallParticipant.getState() == awaitedState)
                {
                    logger.trace("Event already received. " +
                                 collectedEvents);
                    return;
                }

                try
                {
                    wait(waitFor);

                    if (collectedEvents.size() > 0)
                        logger.trace("Received a CallParticpantEvent.");
                    else
                        logger.trace("No CallParticpantEvent received for "
                                     + waitFor + "ms.");

                    listenedCallParticipant
                        .removeCallParticipantListener(this);
                }
                catch (InterruptedException ex)
                {
                    logger.debug("Interrupted while waiting for a "
                                 + "CallParticpantEvent"
                                 , ex);
                }
            }
        }
    }
}
