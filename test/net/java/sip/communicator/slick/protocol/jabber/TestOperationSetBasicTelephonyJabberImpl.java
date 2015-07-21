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

import java.text.*;
import java.util.*;

import junit.framework.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * Tests Basic telephony functionality by making one provider call the other.
 *
 * @author Emil Ivov
 * @author Symphorien Wanko
 */
public class TestOperationSetBasicTelephonyJabberImpl
    extends TestCase
{
    private static final Logger logger
        = Logger.getLogger(TestOperationSetBasicTelephonyJabberImpl.class);

    /**
     * Provides constants and some utilities method.
     */
    private JabberSlickFixture fixture = new JabberSlickFixture();

    /**
     * Initializes the test with the specified <tt>name</tt>.
     *
     * @param name the name of the test to initialize.
     */
    public TestOperationSetBasicTelephonyJabberImpl(String name)
    {
        super(name);
    }

    /**
     * JUnit setup method.
     * @throws Exception in case anything goes wrong.
     */
    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        fixture.setUp();
    }

    /**
     * JUnit teardown method.
     * @throws Exception in case anything goes wrong.
     */
    @Override
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
        OperationSetBasicTelephony<?> basicTelephonyP1
            = fixture.provider1.getOperationSet(
                OperationSetBasicTelephony.class);
        OperationSetBasicTelephony<?> basicTelephonyP2
            = fixture.provider2.getOperationSet(
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

        //verify that call peers are properly created
        assertEquals("callAtP1.getCallPeerCount()"
                     , 1, callAtP1.getCallPeerCount());
        assertEquals("callAtP2.getCallPeerCount()"
                     , 1, callAtP2.getCallPeerCount());

        CallPeer peerAtP1 = callAtP1.getCallPeers().next();
        CallPeer peerAtP2 = callAtP2.getCallPeers().next();

        //now add listeners to the peers and make sure they have entered
        //the states they were expected to.
        //check states for call peers at both parties
        CallPeerStateEventCollector stateCollectorForPp1
            = new CallPeerStateEventCollector(
                peerAtP1, CallPeerState.ALERTING_REMOTE_SIDE);
        CallPeerStateEventCollector stateCollectorForPp2
            = new CallPeerStateEventCollector(
                peerAtP2, CallPeerState.INCOMING_CALL);

        stateCollectorForPp1.waitForEvent(10000, true);
        stateCollectorForPp2.waitForEvent(10000, true);

        assertSame("peerAtP1.getCall"
                   , peerAtP1.getCall(), callAtP1);
        assertSame("peerAtP2.getCall"
                   , peerAtP2.getCall(), callAtP2);

        //make sure that the peers are in the proper state
        assertEquals("The peer at provider one was not in the "
                     +"right state."
                    , CallPeerState.ALERTING_REMOTE_SIDE
                    , peerAtP1.getState());
        assertEquals("The peer at provider two was not in the "
                     +"right state."
                    , CallPeerState.INCOMING_CALL
                    , peerAtP2.getState());


        //test whether caller/callee info is properly distributed in case
        //the server is said to support it.
        if(Boolean.getBoolean("accounts.jabber.PRESERVE_PEER_INFO"))
        {
            //check properties on the remote call peer for the party that
            //initiated the call.
            String expectedPeer1Address
                = fixture.provider2.getAccountID().getAccountAddress();
            String expectedPeer1DisplayName
                = System.getProperty(
                    JabberProtocolProviderServiceLick.ACCOUNT_2_PREFIX
                    + ProtocolProviderFactory.DISPLAY_NAME);

            //do not asser equals here since one of the addresses may contain a
            //display name or something of the kind
            assertTrue("Provider 2 did not advertise their "
                       + "accountID.getAccoutAddress() address."
                       , expectedPeer1Address.indexOf(
                           peerAtP1.getAddress()) != -1
                       || peerAtP1.getAddress().indexOf(
                           expectedPeer1Address) != -1);
            assertEquals("Provider 2 did not properly advertise their "
                         + "display name."
                         , expectedPeer1DisplayName
                         , peerAtP1.getDisplayName());

            //check properties on the remote call peer for the party that
            //receives the call.
            String expectedPeer2Address
                = fixture.provider1.getAccountID().getAccountAddress();
            String expectedPeer2DisplayName
                = System.getProperty(
                    JabberProtocolProviderServiceLick.ACCOUNT_1_PREFIX
                    + ProtocolProviderFactory.DISPLAY_NAME);

            //do not asser equals here since one of the addresses may contain a
            //display name or something of the kind
            assertTrue("Provider 1 did not advertise their "
                       + "accountID.getAccoutAddress() address."
                       , expectedPeer2Address.indexOf(
                           peerAtP2.getAddress()) != -1
                       || peerAtP2.getAddress().indexOf(
                           expectedPeer2Address) != -1);
            assertEquals("Provider 1 did not properly advertise their "
                         + "display name."
                         , expectedPeer2DisplayName
                         , peerAtP2.getDisplayName());
        }

        //we'll now try to cancel the call

        //listeners monitoring state change of the peer
        stateCollectorForPp1 = new CallPeerStateEventCollector(
                peerAtP1, CallPeerState.DISCONNECTED);
        stateCollectorForPp2 = new CallPeerStateEventCollector(
                peerAtP2, CallPeerState.DISCONNECTED);

        //listeners waiting for the op set to announce the end of the call
        call1Listener = new CallEventCollector(basicTelephonyP1);
        call2Listener = new CallEventCollector(basicTelephonyP2);

        //listeners monitoring the state of the call
        CallStateEventCollector call1StateCollector
            = new CallStateEventCollector(callAtP1, CallState.CALL_ENDED);
        CallStateEventCollector call2StateCollector
            = new CallStateEventCollector(callAtP2, CallState.CALL_ENDED);

        //Now make the caller CANCEL the call.
        basicTelephonyP1.hangupCallPeer(peerAtP1);

        //wait for everything to happen
        call1Listener.waitForEvent(10000);
        call2Listener.waitForEvent(10000);
        stateCollectorForPp1.waitForEvent(10000);
        stateCollectorForPp2.waitForEvent(10000);
        call1StateCollector.waitForEvent(10000);
        call2StateCollector.waitForEvent(10000);


        //make sure that the peer is disconnected
        assertEquals("The peer at provider one was not in the "
                     +"right state."
                    , CallPeerState.DISCONNECTED
                    , peerAtP1.getState());

        //make sure the telephony operation set distributed an event for the end
        //of the call
        assertEquals("The basic telephony operation set did not distribute "
                     +"an event to notify us that a call has been ended."
                    , 1
                    , call1Listener.collectedEvents.size());

        CallEvent collectedEvent
            = (CallEvent)call1Listener.collectedEvents.get(0);

        assertEquals("The basic telephony operation set did not distribute "
                     +"an event to notify us that a call has been ended."
                     , CallEvent.CALL_ENDED
                     , collectedEvent.getEventID());

        //same for provider 2

        //make sure that the peer is disconnected
        assertEquals("The peer at provider one was not in the "
                     +"right state."
                    , CallPeerState.DISCONNECTED
                    , peerAtP2.getState());

        //make sure the telephony operation set distributed an event for the end
        //of the call
        assertEquals("The basic telephony operation set did not distribute "
                     +"an event to notify us that a call has been ended."
                    , 1
                    , call2Listener.collectedEvents.size());

        collectedEvent
            = (CallEvent)call2Listener.collectedEvents.get(0);

        assertEquals("The basic telephony operation set did not distribute "
                     +"an event to notify us that a call has been ended."
                     , CallEvent.CALL_ENDED
                     , collectedEvent.getEventID());

        //make sure that the call objects are in an ENDED state.
        assertEquals("A call did not change its state to CallState.CALL_ENDED "
                     +"when it ended."
                     , CallState.CALL_ENDED
                     , callAtP1.getCallState());
        assertEquals("A call did not change its state to CallState.CALL_ENDED "
                     +"when it ended."
                     , CallState.CALL_ENDED
                     , callAtP2.getCallState());
    }

    /**
     * Creates a call from provider1 to provider2 then rejects the call from the
     * side of provider2 (provider2 replies with busy-tone).
     *
     * @throws ParseException if we hand a malformed URI to someone
     * @throws OperationFailedException if something weird happens.
     */
    public void testCreateRejectCall()
        throws ParseException, OperationFailedException
    {
        OperationSetBasicTelephony<?> basicTelephonyP1
            = fixture.provider1.getOperationSet(
                OperationSetBasicTelephony.class);
        OperationSetBasicTelephony<?> basicTelephonyP2
            = fixture.provider2.getOperationSet(
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

        //verify that call peers are properly created
        assertEquals("callAtP1.getCallPeerCount()"
                     , 1, callAtP1.getCallPeerCount());
        assertEquals("callAtP2.getCallPeerCount()"
                     , 1, callAtP2.getCallPeerCount());

        CallPeer peerAtP1 = callAtP1.getCallPeers().next();
        CallPeer peerAtP2 = callAtP2.getCallPeers().next();

        //now add listeners to the peers and make sure they have entered
        //the states they were expected to.
        //check states for call peers at both parties
        CallPeerStateEventCollector stateCollectorForPp1
            = new CallPeerStateEventCollector(
                peerAtP1, CallPeerState.ALERTING_REMOTE_SIDE);
        CallPeerStateEventCollector stateCollectorForPp2
            = new CallPeerStateEventCollector(
                peerAtP2, CallPeerState.INCOMING_CALL);

        stateCollectorForPp1.waitForEvent(10000, true);
        stateCollectorForPp2.waitForEvent(10000, true);

        assertSame("peerAtP1.getCall"
                   , peerAtP1.getCall(), callAtP1);
        assertSame("peerAtP2.getCall"
                   , peerAtP2.getCall(), callAtP2);

        //make sure that the peers are in the proper state
        assertEquals("The peer at provider one was not in the "
                     +"right state."
                    , CallPeerState.ALERTING_REMOTE_SIDE
                    , peerAtP1.getState());
        assertEquals("The peer at provider two was not in the "
                     +"right state."
                    , CallPeerState.INCOMING_CALL
                    , peerAtP2.getState());


        //test whether caller/callee info is properly distributed in case
        //the server is said to support it.
        if(Boolean.getBoolean("accounts.jabber.PRESERVE_PEER_INFO"))
        {
            //check properties on the remote call peer for the party that
            //initiated the call.
            String expectedPeer1Address
                = fixture.provider2.getAccountID().getAccountAddress();
            String expectedPeer1DisplayName
                = System.getProperty(
                    JabberProtocolProviderServiceLick.ACCOUNT_2_PREFIX
                    + ProtocolProviderFactory.DISPLAY_NAME);

            //do not asser equals here since one of the addresses may contain a
            //display name or something of the kind
            assertTrue("Provider 2 did not advertise their "
                       + "accountID.getAccoutAddress() address."
                       , expectedPeer1Address.indexOf(
                           peerAtP1.getAddress()) != -1
                       || peerAtP1.getAddress().indexOf(
                           expectedPeer1Address) != -1);
            assertEquals("Provider 2 did not properly advertise their "
                         + "display name."
                         , expectedPeer1DisplayName
                         , peerAtP1.getDisplayName());

            //check properties on the remote call peer for the party that
            //receives the call.
            String expectedPeer2Address
                = fixture.provider1.getAccountID().getAccountAddress();
            String expectedPeer2DisplayName
                = System.getProperty(
                    JabberProtocolProviderServiceLick.ACCOUNT_1_PREFIX
                    + ProtocolProviderFactory.DISPLAY_NAME);

            //do not asser equals here since one of the addresses may contain a
            //display name or something of the kind
            assertTrue("Provider 1 did not advertise their "
                       + "accountID.getAccoutAddress() address."
                       , expectedPeer2Address.indexOf(
                           peerAtP2.getAddress()) != -1
                       || peerAtP2.getAddress().indexOf(
                           expectedPeer2Address) != -1);
            assertEquals("Provider 1 did not properly advertise their "
                         + "display name."
                         , expectedPeer2DisplayName
                         , peerAtP2.getDisplayName());
        }

        //we'll now try to send busy tone.

        //listeners monitoring state change of the peer
        CallPeerStateEventCollector busyStateCollectorForPp1
            = new CallPeerStateEventCollector(
                peerAtP1, CallPeerState.BUSY);
        stateCollectorForPp1 = new CallPeerStateEventCollector(
                peerAtP1, CallPeerState.DISCONNECTED);
        stateCollectorForPp2 = new CallPeerStateEventCollector(
                peerAtP2, CallPeerState.DISCONNECTED);

        //listeners waiting for the op set to announce the end of the call
        call1Listener = new CallEventCollector(basicTelephonyP1);
        call2Listener = new CallEventCollector(basicTelephonyP2);

        //listeners monitoring the state of the call
        CallStateEventCollector call1StateCollector
            = new CallStateEventCollector(callAtP1, CallState.CALL_ENDED);
        CallStateEventCollector call2StateCollector
            = new CallStateEventCollector(callAtP2, CallState.CALL_ENDED);

        //Now make the caller CANCEL the call.
        basicTelephonyP2.hangupCallPeer(peerAtP2);
        busyStateCollectorForPp1.waitForEvent(10000);
        basicTelephonyP1.hangupCallPeer(peerAtP1);

        //wait for everything to happen
        call1Listener.waitForEvent(10000);
        call2Listener.waitForEvent(10000);
        stateCollectorForPp1.waitForEvent(10000);
        stateCollectorForPp2.waitForEvent(10000);
        call1StateCollector.waitForEvent(10000);
        call2StateCollector.waitForEvent(10000);


        //make sure that the peer is disconnected
        assertEquals("The peer at provider one was not in the "
                     +"right state."
                    , CallPeerState.DISCONNECTED
                    , peerAtP1.getState());



        //make sure the telephony operation set distributed an event for the end
        //of the call
        assertEquals("The basic telephony operation set did not distribute "
                     +"an event to notify us that a call has been ended."
                    , 1
                    , call1Listener.collectedEvents.size());

        CallEvent collectedEvent
            = (CallEvent)call1Listener.collectedEvents.get(0);

        assertEquals("The basic telephony operation set did not distribute "
                     +"an event to notify us that a call has been ended."
                     , CallEvent.CALL_ENDED
                     , collectedEvent.getEventID());

        //same for provider 2

        //make sure that the peer is disconnected
        assertEquals("The peer at provider one was not in the "
                     +"right state."
                    , CallPeerState.DISCONNECTED
                    , peerAtP2.getState());

        //make sure the telephony operation set distributed an event for the end
        //of the call
        assertEquals("The basic telephony operation set did not distribute "
                     +"an event to notify us that a call has been ended."
                    , 1
                    , call2Listener.collectedEvents.size());

        collectedEvent
            = (CallEvent)call2Listener.collectedEvents.get(0);

        assertEquals("The basic telephony operation set did not distribute "
                     +"an event to notify us that a call has been ended."
                     , CallEvent.CALL_ENDED
                     , collectedEvent.getEventID());

        //make sure that the call objects are in an ENDED state.
        assertEquals("A call did not change its state to CallState.CALL_ENDED "
                     +"when it ended."
                     , CallState.CALL_ENDED
                     , callAtP1.getCallState());
        assertEquals("A call did not change its state to CallState.CALL_ENDED "
                     +"when it ended."
                     , CallState.CALL_ENDED
                     , callAtP2.getCallState());
    }

    /**
     * Creates a call from provider1 to provider2, makes provider2 answer it
     * and then reject it.
     *
     * @throws ParseException if we hand a malformed URI to someone
     * @throws OperationFailedException if something weird happens.
     */
    public void aTestCreateAnswerHangupCall()
        throws ParseException, OperationFailedException
    {
        OperationSetBasicTelephony<?> basicTelephonyP1
            = fixture.provider1.getOperationSet(
                OperationSetBasicTelephony.class);
        OperationSetBasicTelephony<?> basicTelephonyP2
            = fixture.provider2.getOperationSet(
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

        //verify that call peers are properly created
        assertEquals("callAtP1.getCallPeerCount()"
                     , 1, callAtP1.getCallPeerCount());
        assertEquals("callAtP2.getCallPeerCount()"
                     , 1, callAtP2.getCallPeerCount());

        CallPeer peerAtP1 = callAtP1.getCallPeers().next();
        CallPeer peerAtP2 = callAtP2.getCallPeers().next();

        //now add listeners to the peers and make sure they have entered
        //the states they were expected to.
        //check states for call peers at both parties
        CallPeerStateEventCollector stateCollectorForPp1
            = new CallPeerStateEventCollector(
                peerAtP1, CallPeerState.ALERTING_REMOTE_SIDE);
        CallPeerStateEventCollector stateCollectorForPp2
            = new CallPeerStateEventCollector(
                peerAtP2, CallPeerState.INCOMING_CALL);

        stateCollectorForPp1.waitForEvent(10000, true);
        stateCollectorForPp2.waitForEvent(10000, true);

        assertSame("peerAtP1.getCall"
                   , peerAtP1.getCall(), callAtP1);
        assertSame("peerAtP2.getCall"
                   , peerAtP2.getCall(), callAtP2);

        //make sure that the peers are in the proper state
        assertEquals("The peer at provider one was not in the "
                     +"right state."
                    , CallPeerState.ALERTING_REMOTE_SIDE
                    , peerAtP1.getState());
        assertEquals("The peer at provider two was not in the "
                     +"right state."
                    , CallPeerState.INCOMING_CALL
                    , peerAtP2.getState());


        //test whether caller/callee info is properly distributed in case
        //the server is said to support it.
        if(Boolean.getBoolean("accounts.jabber.PRESERVE_PEER_INFO"))
        {
            //check properties on the remote call peer for the party that
            //initiated the call.
            String expectedPeer1Address
                = fixture.provider2.getAccountID().getAccountAddress();
            String expectedPeer1DisplayName
                = System.getProperty(
                    JabberProtocolProviderServiceLick.ACCOUNT_2_PREFIX
                    + ProtocolProviderFactory.DISPLAY_NAME);

            //do not assert equals here since one of the addresses may contain a
            //display name or something of the kind
            assertTrue("Provider 2 did not advertise their "
                       + "accountID.getAccoutAddress() address."
                       , expectedPeer1Address.indexOf(
                           peerAtP1.getAddress()) != -1
                       || peerAtP1.getAddress().indexOf(
                           expectedPeer1Address) != -1);
            assertEquals("Provider 2 did not properly advertise their "
                         + "display name."
                         , expectedPeer1DisplayName
                         , peerAtP1.getDisplayName());

            //check properties on the remote call peer for the party that
            //receives the call.
            String expectedPeer2Address
                = fixture.provider1.getAccountID().getAccountAddress();
            String expectedPeer2DisplayName
                = System.getProperty(
                    JabberProtocolProviderServiceLick.ACCOUNT_1_PREFIX
                    + ProtocolProviderFactory.DISPLAY_NAME);

            //do not asser equals here since one of the addresses may contain a
            //display name or something of the kind
            assertTrue("Provider 1 did not advertise their "
                       + "accountID.getAccoutAddress() address."
                       , expectedPeer2Address.indexOf(
                           peerAtP2.getAddress()) != -1
                       || peerAtP2.getAddress().indexOf(
                           expectedPeer2Address) != -1);
            assertEquals("Provider 1 did not properly advertise their "
                         + "display name."
                         , expectedPeer2DisplayName
                         , peerAtP2.getDisplayName());
        }

        //add listeners to the peers and make sure enter
        //a connected state after we answer
        stateCollectorForPp1
            = new CallPeerStateEventCollector(
                peerAtP1, CallPeerState.CONNECTED);
        stateCollectorForPp2
            = new CallPeerStateEventCollector(
                peerAtP2, CallPeerState.CONNECTED);

        //we will now anser the call and verify that both parties change states
        //accordingly.
        basicTelephonyP2.answerCallPeer(peerAtP2);

        stateCollectorForPp1.waitForEvent(10000);
        stateCollectorForPp2.waitForEvent(10000);

        //make sure that the peers are in the proper state
        assertEquals("The peer at provider one was not in the "
                     +"right state."
                    , CallPeerState.CONNECTED
                    , peerAtP1.getState());
        assertEquals("The peer at provider two was not in the "
                     +"right state."
                    , CallPeerState.CONNECTED
                    , peerAtP2.getState());

        //make sure that events have been distributed when states were changed.
        assertEquals("No event was dispatched when a call peer changed "
                     +"its state."
                    , 1
                    , stateCollectorForPp1.collectedEvents.size());
        assertEquals("No event was dispatched when a call peer changed "
                     +"its state."
                     , 1
                     , stateCollectorForPp2.collectedEvents.size());

        //add listeners to the peers and make sure they have entered
        //the states they are expected to.
        stateCollectorForPp1
            = new CallPeerStateEventCollector(
                peerAtP1, CallPeerState.DISCONNECTED);
        stateCollectorForPp2
            = new CallPeerStateEventCollector(
                peerAtP2, CallPeerState.DISCONNECTED);

        //we will now end the call and verify that both parties change states
        //accordingly.
        basicTelephonyP2.hangupCallPeer(peerAtP2);

        stateCollectorForPp1.waitForEvent(10000);
        stateCollectorForPp2.waitForEvent(10000);

        //make sure that the peers are in the proper state
        assertEquals("The peer at provider one was not in the "
                     +"right state."
                    , CallPeerState.DISCONNECTED
                    , peerAtP1.getState());
        assertEquals("The peer at provider two was not in the "
                     +"right state."
                    , CallPeerState.DISCONNECTED
                    , peerAtP2.getState());

        //make sure that the corresponding events were delivered.
        assertEquals("a provider did not distribute an event when a call "
                     +"peer changed states."
                    , 1
                    , stateCollectorForPp1.collectedEvents.size());
        assertEquals("a provider did not distribute an event when a call "
                     +"peer changed states."
                    , 1
                    , stateCollectorForPp2.collectedEvents.size());

    }

    /**
     * Allows tests to wait for and collect events issued upon creation and
     * reception of calls.
     */
    public class CallEventCollector implements CallListener
    {
        public ArrayList<EventObject> collectedEvents = new ArrayList<EventObject>();
        public OperationSetBasicTelephony<?> listenedOpSet = null;

        /**
         * Creates an instance of this call event collector and registers it
         * with listenedOpSet.
         * @param listenedOpSet the operation set that we will be scanning for
         * new calls.
         */
        public CallEventCollector(OperationSetBasicTelephony<?> listenedOpSet)
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
                    listenedOpSet.removeCallListener(this);
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
         * Stores the received event and notifies all waiting on this object
         * @param event the event containing the source call.
         */
        public void callEnded(CallEvent event)
        {
            synchronized(this)
            {
                logger.debug(
                    "Collected evt("+collectedEvents.size()+")= "+event);

                this.collectedEvents.add(event);
                notifyAll();
            }
        }
    }

    /**
     * Allows tests to wait for and collect events issued upon call peer
     * status changes.
     */
    public class CallPeerStateEventCollector
        extends CallPeerAdapter
    {
        public ArrayList<EventObject> collectedEvents = new ArrayList<EventObject>();
        private CallPeer listenedCallPeer = null;
        public CallPeerState awaitedState = null;

        /**
         * Creates an instance of this collector and adds it as a listener
         * to <tt>callPeer</tt>.
         * @param callPeer the CallPeer that we will be listening
         * to.
         * @param awaitedState the state that we will be waiting for inside
         * this collector.
         */
        public CallPeerStateEventCollector(
                                            CallPeer      callPeer,
                                            CallPeerState awaitedState)
        {
            this.listenedCallPeer = callPeer;
            this.listenedCallPeer.addCallPeerListener(this);
            this.awaitedState = awaitedState;
        }

        /**
         * Stores the received event and notifies all waiting on this object
         *
         * @param event the event containing the source call.
         */
        @Override
        public void peerStateChanged(CallPeerChangeEvent event)
        {
            synchronized(this)
            {
                logger.debug(
                    "Collected evt("+collectedEvents.size()+")= "+event);

                if(((CallPeerState)event.getNewValue())
                    .equals(awaitedState))
                {
                    this.collectedEvents.add(event);
                    notifyAll();
                }
            }
        }

        /**
         * Blocks until an event notifying us of the awaited state change is
         * received or until waitFor miliseconds pass (whichever happens first).
         *
         * @param waitFor the number of miliseconds that we should be waiting
         * for an event before simply bailing out.
         */
        public void waitForEvent(long waitFor)
        {
            waitForEvent(waitFor, false);
        }

        /**
         * Blocks until an event notifying us of the awaited state change is
         * received or until waitFor miliseconds pass (whichever happens first).
         *
         * @param waitFor the number of miliseconds that we should be waiting
         * for an event before simply bailing out.
         * @param exitIfAlreadyInState specifies whether the method is to return
         * if the call peer is already in such a state even if no event
         * has been received for the sate change.
         */
        public void waitForEvent(long waitFor, boolean exitIfAlreadyInState)
        {
            logger.trace("Waiting for a CallPeerEvent with newState="
                            + awaitedState + " for peer "
                            + this.listenedCallPeer);
            synchronized (this)
            {
                if(exitIfAlreadyInState
                   && listenedCallPeer.getState().equals(awaitedState))
                {
                    logger.trace("Src peer is already in the awaited "
                                 + "state."
                                 + collectedEvents);
                    listenedCallPeer.removeCallPeerListener(this);
                    return;
                }
                if(collectedEvents.size() > 0)
                {
                    CallPeerChangeEvent lastEvent
                        = (CallPeerChangeEvent) collectedEvents
                        .get(collectedEvents.size() - 1);

                    if (lastEvent.getNewValue().equals(awaitedState))
                    {
                        logger.trace("Event already received. " +
                                     collectedEvents);
                        listenedCallPeer
                            .removeCallPeerListener(this);
                        return;
                    }
                }
                try
                {
                    wait(waitFor);

                    if (collectedEvents.size() > 0)
                        logger.trace("Received a CallParticpantStateEvent.");
                    else
                        logger.trace("No CallParticpantStateEvent received for "
                                     + waitFor + "ms.");

                    listenedCallPeer
                        .removeCallPeerListener(this);
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

    /**
     * Allows tests to wait for and collect events issued upon call state
     * changes.
     */
    public class CallStateEventCollector
        extends CallChangeAdapter
    {
        public ArrayList<EventObject> collectedEvents = new ArrayList<EventObject>();
        private Call listenedCall = null;
        public CallState awaitedState = null;

        /**
         * Creates an instance of this collector and adds it as a listener
         * to <tt>call</tt>.
         * @param call the Call that we will be listening to.
         * @param awaitedState the state that we will be waiting for inside
         * this collector.
         */
        public CallStateEventCollector(Call      call,
                                       CallState awaitedState)
        {
            this.listenedCall = call;
            this.listenedCall.addCallChangeListener(this);
            this.awaitedState = awaitedState;
        }

        /**
         * Stores the received event and notifies all waiting on this object
         *
         * @param event the event containing the source call.
         */
        @Override
        public void callStateChanged(CallChangeEvent event)
        {
            synchronized(this)
            {
                logger.debug(
                    "Collected evt("+collectedEvents.size()+")= "+event);

                if(((CallState)event.getNewValue()).equals(awaitedState))
                {
                    this.collectedEvents.add(event);
                    notifyAll();
                }
            }
        }

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
                if (listenedCall.getCallState() == awaitedState)
                {
                    logger.trace("Event already received. " +
                                 collectedEvents);
                    listenedCall.removeCallChangeListener(this);
                    return;
                }

                try
                {
                    wait(waitFor);

                    if (collectedEvents.size() > 0)
                        logger.trace("Received a CallChangeEvent.");
                    else
                        logger.trace("No CallChangeEvent received for "
                                     + waitFor + "ms.");

                    listenedCall.removeCallChangeListener(this);
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
