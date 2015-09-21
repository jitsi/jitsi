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
package net.java.sip.communicator.slick.callhistory;

import java.util.*;

import junit.framework.*;
import net.java.sip.communicator.impl.protocol.mock.*;
import net.java.sip.communicator.service.callhistory.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * Tests call history.
 * First installs the MockProtocolProvider to be able to create som calls
 * The call history service stores them
 * and then tests the verious find methods - does they find the calls we have
 * already made
 *
 * @author Damian Minkov
 */
public class TestCallHistoryService
    extends TestCase
{
    private static final Logger logger = Logger.getLogger(TestCallHistoryService.class);

    /**
     * The provider that we use to make a dummy server-stored contactlist
     * used for testing. The mockProvider is instantiated and registered
     * by the metacontactlist slick activator.
     */
    public static MockProvider mockProvider = null;

    public static MockOperationSetBasicTelephony mockBTelphonyOpSet = null;

    private static ServiceReference callHistoryServiceRef = null;
    public static CallHistoryService callHistoryService = null;

    /**
     * A reference to the registration of the first mock provider.
     */
    public static ServiceRegistration mockPrServiceRegistration = null;

    private static Date controlDate1 = null;
    private static Date controlDate2 = null;

    /**
     * The addresses will be used in the generated mock calls
     */
    private static Vector<String> participantAddresses = new Vector<String>();

    public TestCallHistoryService(String name)
    {
        super(name);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite();
        suite.addTest(
            new TestCallHistoryService("readRecords"));
        suite.addTest(
            new TestCallHistoryService("checkRecordCompleteness"));

        return suite;
    }

    @Override
    protected void setUp() throws Exception
    {
        setupContact();
        callHistoryService.eraseLocallyStoredHistory();
        writeRecords();
    }

    @Override
    protected void tearDown() throws Exception
    {
    }

    private void setupContact()
    {
        // changes the history service target derictory
        System.setProperty("HistoryServiceDirectory", "test-callhistory");

        mockProvider = new MockProvider("CallHistoryMockUser");

        //store thre presence op set of the new provider into the fixture
        mockBTelphonyOpSet =
            (MockOperationSetBasicTelephony) mockProvider
                .getOperationSet(OperationSetBasicTelephony.class);

       System.setProperty(MetaContactListService.PROVIDER_MASK_PROPERTY, "1");

       Hashtable<String, String> mockProvProperties = new Hashtable<String, String>();
       mockProvProperties.put(ProtocolProviderFactory.PROTOCOL
                              , mockProvider.getProtocolName());
       mockProvProperties.put(MetaContactListService.PROVIDER_MASK_PROPERTY,
                              "1");

       mockPrServiceRegistration =
           CallHistoryServiceLick.bc.registerService(
               ProtocolProviderService.class.getName(),
               mockProvider,
               mockProvProperties);
       logger.debug("Registered a mock protocol provider! ");

       callHistoryServiceRef =
           CallHistoryServiceLick.bc.
           getServiceReference(CallHistoryService.class.getName());

       callHistoryService =
            (CallHistoryService) CallHistoryServiceLick.bc.
            getService(callHistoryServiceRef);

        // Will genarate 4 Calls with 4 different participants
        participantAddresses.add("participant_address_1");
        participantAddresses.add("participant_address_2");
        participantAddresses.add("participant_address_3");
        participantAddresses.add("participant_address_4");
    }

    /**
     *  First create calls
     */
    private void writeRecords()
    {
        logger.info("write records ");

        generateCall(participantAddresses.get(0));

        waitSeconds(1);
        controlDate1 = new Date();

        generateCall(participantAddresses.get(1));

        generateCall(participantAddresses.get(2));

        waitSeconds(1);
        controlDate2 = new Date();

        generateCall(participantAddresses.get(3));
    }

    private void generateCall(String participant)
    {
        try
        {
            Call newCall = mockBTelphonyOpSet.placeCall(participant);

            Vector<CallPeer> v = new Vector<CallPeer>();

            Iterator<? extends CallPeer> iter = newCall.getCallPeers();
            while (iter.hasNext())
            {
                CallPeer item = iter.next();
                v.add(item);
            }

            waitSeconds(2000);

            iter = v.iterator();
            while (iter.hasNext())
            {
                CallPeer item = iter.next();
                mockBTelphonyOpSet.hangupCallPeer(item);
            }
        }
        catch (Exception ex1)
        {
            logger.error("Cannot place mock call", ex1);
            fail("Cannot place mock call to " + participant);
        }
    }


    private void waitSeconds(long secs)
    {
        Object lock = new Object();
        synchronized (lock){
            // wait a moment
            try{
                lock.wait(secs);
            }
            catch (InterruptedException ex){}
        }
    }

    /**
     * tests all read methods (finders)
     */
    public void readRecords()
    {
        /**
         * This must match also many calls, as tests are run many times
         * but the minimum is 3
         */
        Collection<CallRecord> rs
            = callHistoryService.findByEndDate(controlDate2);
        Iterator<CallRecord> resultIter = rs.iterator();

        assertTrue("Calls too few - findByEndDate", rs.size() >= 3);

        /**
         * must find 2 calls
         */
        rs = callHistoryService.findByPeriod(controlDate1, controlDate2);
        resultIter = rs.iterator();

        assertEquals("Calls must be 2", rs.size(), 2);

        CallRecord rec = resultIter.next();
        CallPeerRecord participant = rec.getPeerRecords().get(0);

        assertTrue("Participant incorrect ",
                   participant.getPeerAddress().
                   equals(participantAddresses.get(2)));

        rec = resultIter.next();
        participant = rec.getPeerRecords().get(0);

        assertTrue("Participant incorrect ",
                   participant.getPeerAddress().
                   equals(participantAddresses.get(1)));

        /**
         * must find 1 record
         */
        rs = callHistoryService.findByStartDate(controlDate2);
        resultIter = rs.iterator();

        assertEquals("Calls must be 1", rs.size(), 1);

        rec = resultIter.next();
        participant = rec.getPeerRecords().get(0);

        assertTrue("Participant incorrect ",
                   participant.getPeerAddress().
                   equals(participantAddresses.get(3)));

        /**
         * Must return exactly the last 3 calls
         */
        rs = callHistoryService.findLast(3);
        resultIter = rs.iterator();

        assertEquals("Calls must be 3", rs.size(), 3);

        rec = resultIter.next();
        participant = rec.getPeerRecords().get(0);

        assertTrue("Participant incorrect ",
                   participant.getPeerAddress().
                   equals(participantAddresses.get(3)));

        rec = resultIter.next();
        participant = rec.getPeerRecords().get(0);

        assertTrue("Participant incorrect ",
                   participant.getPeerAddress().
                   equals(participantAddresses.get(2)));

        rec = resultIter.next();
        participant = rec.getPeerRecords().get(0);

        assertTrue("Participant incorrect ",
                   participant.getPeerAddress().
                   equals(participantAddresses.get(1)));
    }

    public void checkRecordCompleteness()
    {
        logger.info("---=== checkRecordCompleteness ===---");
        String[] partAddresses =
            new String[]{"some_address", "some_new_address"};

        try
        {
            Call newCall =
                mockBTelphonyOpSet.placeCall(partAddresses[0]);

            Vector<CallPeer> v = new Vector<CallPeer>();

            Iterator<? extends CallPeer> iter = newCall.getCallPeers();
            while (iter.hasNext())
            {
                CallPeer item = iter.next();
                v.add(item);
            }

            waitSeconds(2000);

            CallPeer newParticipant =
                mockBTelphonyOpSet.addNewCallPeer(newCall,
                partAddresses[1]);

            mockBTelphonyOpSet.hangupCallPeer(newParticipant);

            waitSeconds(2000);

            iter = v.iterator();
            while (iter.hasNext())
            {
                CallPeer item = iter.next();
                mockBTelphonyOpSet.hangupCallPeer(item);
            }
        }
        catch (Exception ex1)
        {
            logger.error("Cannot place mock call", ex1);
            fail("Cannot place mock call");
        }


        Collection<CallRecord> lastCall = callHistoryService.findLast(1);

        assertEquals("There must be 1 Call", 1, lastCall.size());

        CallRecord callRecord = lastCall.iterator().next();

        assertEquals("There must be 2 participants in the call",
                     2, callRecord.getPeerRecords().size());

        CallPeerRecord callP1 =
            callRecord.findPeerRecord(partAddresses[0]);
        CallPeerRecord callP2 =
            callRecord.findPeerRecord(partAddresses[1]);

        assertTrue("Second participant added after first one",
                   callP2.getStartTime().after(callP1.getStartTime()));

        assertTrue("Second participant hanguped before first one",
                   callP2.getEndTime().before(callP1.getEndTime()));
    }
}
