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
package net.java.sip.communicator.slick.metahistory;

import java.io.*;
import java.util.*;

import junit.framework.*;
import net.java.sip.communicator.impl.protocol.mock.*;
import net.java.sip.communicator.service.callhistory.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.filehistory.*;
import net.java.sip.communicator.service.history.*;
import net.java.sip.communicator.service.metahistory.*;
import net.java.sip.communicator.service.msghistory.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * Tests file message history.
 * First installs the MoxkProtocolProvider to be able to send some files
 * The file history service stores them
 * and then tests the verious find methods - does they find the messsages we have
 * already sent
 *
 * @author Damian Minkov
 */
public class TestMetaHistoryService
    extends TestCase
{
    private static final Logger logger = Logger.getLogger(TestMetaHistoryService.class);

    static final String TEST_CONTACT_NAME_1 = "Mincho_Penchev_the_fisrt";
    static final String TEST_CONTACT_NAME_2 = "Mincho_Penchev_the_second";

//    static final String TEST_ROOM_NAME = "test_room";

    /**
     * The provider that we use to make a dummy server-stored contactlist
     * used for testing. The mockProvider is instantiated and registered
     * by the metacontactlist slick activator.
     */
    public static MockProvider mockProvider = null;
    /**
     * The persistent presence operation set of the default mock provider.
     */
    public static MockPersistentPresenceOperationSet mockPresOpSet = null;
    public static MockOperationSetFileTransfer mockFTOpSet = null;
    public static MockBasicInstantMessaging mockBImOpSet = null;
    public static MockOperationSetBasicTelephony mockBTelphonyOpSet = null;

    private static ServiceReference metaHistoryServiceRef = null;
    public static MetaHistoryService metaHistoryService = null;

    private static MockContact testContact = null;

    private static ServiceReference metaCLref = null;
    private static MetaContactListService metaClService = null;

    private static MetaContact testMetaContact = null;

    /**
     * A reference to the registration of the first mock provider.
     */
    public static ServiceRegistration mockPrServiceRegistration = null;

    private static Message[] messagesToSend = null;

    private static Date controlDate1 = null;
    private static Date controlDate2 = null;

    /**
     * The addresses will be used in the generated mock calls
     */
    private static Vector<String> participantAddresses = new Vector<String>();

    /**
     * Files to receive
     */
    private static File[] files = null;

    public TestMetaHistoryService(String name)
    {
        super(name);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite();
        suite.addTest(
            new TestMetaHistoryService("messageTests"));
        suite.addTest(
            new TestMetaHistoryService("callTests"));
        suite.addTest(
            new TestMetaHistoryService("fileTests"));
        suite.addTest(
            new TestMetaHistoryService("metaTests"));

        return suite;
    }

    @Override
    protected void setUp() throws Exception
    {
        setupContact();
        ServiceUtils.getService(MetaHistoryServiceLick.bc,
            MessageHistoryService.class).eraseLocallyStoredHistory();
        ServiceUtils.getService(MetaHistoryServiceLick.bc,
            HistoryService.class).purgeLocallyCachedHistories();
        writeRecords();
    }

    @Override
    protected void tearDown() throws Exception
    {
        testPurgeLocalContactListCopy();
    }

    public void setupContact()
    {
        // changes the history service target derictory
        System.setProperty("HistoryServiceDirectory", "test-filehistory");

        mockProvider = new MockProvider("FileHistoryMockUser");

        //store thre presence op set of the new provider into the fixture
        Map<String, OperationSet> supportedOperationSets =
            mockProvider.getSupportedOperationSets();

        //get the operation set presence here.
        mockPresOpSet =
            (MockPersistentPresenceOperationSet) supportedOperationSets.get(
                OperationSetPersistentPresence.class.getName());

        mockBTelphonyOpSet =
            (MockOperationSetBasicTelephony) mockProvider
                .getOperationSet(OperationSetBasicTelephony.class);

        mockBImOpSet =
            (MockBasicInstantMessaging) supportedOperationSets.get(
                OperationSetBasicInstantMessaging.class.getName());

        mockFTOpSet =
            (MockOperationSetFileTransfer) supportedOperationSets.get(
                OperationSetFileTransfer.class.getName());

        metaHistoryServiceRef =
            MetaHistoryServiceLick.bc.
            getServiceReference(MetaHistoryService.class.getName());

        metaHistoryService =
            (MetaHistoryService)MetaHistoryServiceLick.bc.
                getService(metaHistoryServiceRef);

        // fill in a contact to comunicate with
        MockContactGroup root =
            (MockContactGroup)mockPresOpSet.getServerStoredContactListRoot();

        testContact = new MockContact(TEST_CONTACT_NAME_1, mockProvider);
        root.addContact(testContact);

        metaCLref = MetaHistoryServiceLick.bc.getServiceReference(
            MetaContactListService.class.getName());

        metaClService =
            (MetaContactListService)MetaHistoryServiceLick.bc.getService(metaCLref);

       System.setProperty(MetaContactListService.PROVIDER_MASK_PROPERTY, "1");

       Hashtable<String, String> mockProvProperties = new Hashtable<String, String>();
       mockProvProperties.put(ProtocolProviderFactory.PROTOCOL
                              , mockProvider.getProtocolName());
       mockProvProperties.put(MetaContactListService.PROVIDER_MASK_PROPERTY,
                              "1");

       mockPrServiceRegistration =
           MetaHistoryServiceLick.bc.registerService(
               ProtocolProviderService.class.getName(),
               mockProvider,
               mockProvProperties);
       logger.debug("Registered a mock protocol provider! ");

       testMetaContact = metaClService.getRoot().
            getMetaContact(mockProvider, TEST_CONTACT_NAME_1);

       // add one more contact as specific problems may happen only when
       // more than one contact is in the metacontact
        metaClService.addNewContactToMetaContact(
            mockProvider, testMetaContact, TEST_CONTACT_NAME_2);

        messagesToSend = new Message[]
            {
                mockBImOpSet.createMessage("test message word1-" + Math.random()),
                mockBImOpSet.createMessage("test message word2-" + Math.random()),
                mockBImOpSet.createMessage("test message word3-" + Math.random()),
                mockBImOpSet.createMessage(
                    "test message word4 participant_address_4 t4 -" + Math.random()),
                mockBImOpSet.createMessage("test message word5-" + Math.random()),
                mockBImOpSet.createMessage(
                    "test message word6 participant_address_1 t1 -" + Math.random())
            };

        participantAddresses.add("participant_address_1");
        participantAddresses.add("participant_address_2");
        participantAddresses.add("participant_address_3");
        participantAddresses.add("participant_address_4");
        participantAddresses.add("participant_address_5");
        participantAddresses.add("participant_address_6");

        files = new File[]
        {
            new File("t1.txt"),
            new File("t2.txt"),
            new File("t3.txt"),
            new File("t4.txt"),
            new File("t5.txt"),
            new File("t6.txt")
        };
    }

    /**
     *  First send the messages
     */
    public void writeRecords()
    {
        logger.info("write records ");

        assertNotNull("No metacontact", testMetaContact);

        // First cancel an out file transfer
        FileTransfer ft = mockFTOpSet.sendFile(testContact, files[0]);
        waitSeconds(200);
        mockFTOpSet.changeFileTransferStatus(
            ft, FileTransferStatusChangeEvent.CANCELED);
        // now receive a filetransfer and accept it
        TransferListener tl = new TransferListener(files[1].getName(), true, true);
        mockFTOpSet.addFileTransferListener(tl);
        waitSeconds(200);
        mockFTOpSet.receiveFile(files[1], testContact);
        mockFTOpSet.removeFileTransferListener(tl);

        waitSeconds(200);
        generateCall(participantAddresses.get(0));
        waitSeconds(200);
        generateCall(participantAddresses.get(1));
        waitSeconds(200);
        mockBImOpSet.deliverMessage(TEST_CONTACT_NAME_1, messagesToSend[0]);
        waitSeconds(200);
        mockBImOpSet.deliverMessage(TEST_CONTACT_NAME_2, messagesToSend[1]);
        waitSeconds(200);

        controlDate1 = new Date();

        waitSeconds(200);

        generateCall(participantAddresses.get(2));
        waitSeconds(200);
        generateCall(participantAddresses.get(3));
        waitSeconds(200);
        mockBImOpSet.deliverMessage(TEST_CONTACT_NAME_1, messagesToSend[2]);
        waitSeconds(200);
        mockBImOpSet.deliverMessage(TEST_CONTACT_NAME_2, messagesToSend[3]);
        waitSeconds(200);
        // finish an out file transfer
        ft = mockFTOpSet.sendFile(testContact, files[2]);
        waitSeconds(200);
        mockFTOpSet.changeFileTransferStatus(
            ft, FileTransferStatusChangeEvent.COMPLETED);
        waitSeconds(200);
        // now receive a filetransfer and decline it
        tl = new TransferListener(files[3].getName(), false, false);
        mockFTOpSet.addFileTransferListener(tl);
        mockFTOpSet.receiveFile(files[3], testContact);
        waitSeconds(200);
        mockFTOpSet.removeFileTransferListener(tl);

        controlDate2 = new Date();
        waitSeconds(200);

        waitSeconds(200);
        generateCall(participantAddresses.get(4));
        waitSeconds(200);
        generateCall(participantAddresses.get(5));
        waitSeconds(200);
        // finish an out file transfer
        ft = mockFTOpSet.sendFile(testContact, files[4]);
        waitSeconds(200);
        mockFTOpSet.changeFileTransferStatus(ft, FileTransferStatusChangeEvent.REFUSED);
        waitSeconds(200);
        // now receive a filetransfer and decline it
        tl = new TransferListener(files[5].getName(), true, true);
        mockFTOpSet.addFileTransferListener(tl);
        waitSeconds(200);
        mockFTOpSet.receiveFile(files[5], testContact);
        waitSeconds(200);
        mockFTOpSet.removeFileTransferListener(tl);
        mockBImOpSet.deliverMessage(TEST_CONTACT_NAME_1, messagesToSend[4]);
        waitSeconds(200);
        mockBImOpSet.deliverMessage(TEST_CONTACT_NAME_2, messagesToSend[5]);
        waitSeconds(200);
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
    public void messageTests()
    {
        /**
         * This matches all written messages, they are minimum 5
         */
        Collection<Object> rs = metaHistoryService.findByKeyword(
            new String[]{MessageHistoryService.class.getName()},
            testMetaContact, "test");

        assertTrue("Nothing found findByKeyword ", !rs.isEmpty());

        List<String> msgs = getMessages(rs);

        assertTrue("Messages too few - findByKeyword", msgs.size() >= 5);

        /**
         * Will test case sernsitive and insensitive search
         */
        rs = metaHistoryService.findByKeyword(
            new String[]{MessageHistoryService.class.getName()},
            testMetaContact, "Test", false);

        assertTrue("Nothing found findByKeyword caseINsensitive search", !rs.isEmpty());

        msgs = getMessages(rs);

        assertTrue("Messages too few - findByKeyword", msgs.size() >= 5);

        rs = metaHistoryService.findByKeyword(
            new String[]{MessageHistoryService.class.getName()},
            testMetaContact, "Test", true);

        assertFalse("Something found by findByKeyword casesensitive search", !rs.isEmpty());

        /**
         * This must match also many messages, as tests are run many times
         * but the minimum is 3
         */
        rs = metaHistoryService.findByEndDate(
            new String[]{MessageHistoryService.class.getName()},
            testMetaContact, controlDate2);

        assertTrue("Nothing found findByEndDate", !rs.isEmpty());

        msgs = getMessages(rs);

        assertTrue("Messages too few - findByEndDate", msgs.size() >= 3);

        /**
         * This must find also many messages but atleast one
         */
        rs = metaHistoryService.findByKeywords(
            new String[]{MessageHistoryService.class.getName()},
            testMetaContact,
            new String[]{"test", "word2"});

        assertTrue("Nothing found findByKeywords", !rs.isEmpty());
        msgs = getMessages(rs);
        assertTrue("Messages too few - findByKeywords", msgs.size() >= 1);

        /**
         * Nothing to be found
         */
        rs = metaHistoryService.findByKeywords(
            new String[]{MessageHistoryService.class.getName()},
            testMetaContact,
            new String[]{"test1", "word2"});

        assertFalse("Something found findByKeywords", !rs.isEmpty());

        /**
         * must find 2 messages
         */
        rs = metaHistoryService.findByPeriod(
            new String[]{MessageHistoryService.class.getName()},
            testMetaContact, controlDate1, controlDate2);

        assertFalse("Nothing found findByPeriod", rs.isEmpty());

        msgs = getMessages(rs);

        assertEquals("Messages must be 2", 2, msgs.size());

        assertTrue("Message no found",
                   msgs.contains(messagesToSend[2].getContent()));
        assertTrue("Message no found",
                   msgs.contains(messagesToSend[3].getContent()));

        /**
         * must find 1 record
         */
        rs = metaHistoryService.findByPeriod(
            new String[]{MessageHistoryService.class.getName()},
            testMetaContact, controlDate1, controlDate2, new String[]{"word3"});

        assertTrue("Nothing found findByPeriod", !rs.isEmpty());

        msgs = getMessages(rs);

        assertEquals("Messages must be 1", msgs.size(), 1);
        assertTrue("Message no found",
                   msgs.contains(messagesToSend[2].getContent()));

        /**
         * must find 2 records
         */
        rs = metaHistoryService.findByStartDate(
            new String[]{MessageHistoryService.class.getName()},
            testMetaContact, controlDate2);

        assertTrue("Nothing found findByStartDate", !rs.isEmpty());
        msgs = getMessages(rs);
        assertEquals("Messages must be 2", msgs.size(), 2);
        assertTrue("Message no found",
                   msgs.contains(messagesToSend[4].getContent()));
        assertTrue("Message no found",
                   msgs.contains(messagesToSend[5].getContent()));

        /**
         * Must return exactly the last 3 messages
         */
        rs = metaHistoryService.findLast(
            new String[]{MessageHistoryService.class.getName()},
            testMetaContact, 3);

        assertTrue("Nothing found 8", !rs.isEmpty());
        msgs = getMessages(rs);
        assertEquals("Messages must be 3", msgs.size(), 3);
        assertTrue("Message no found",
                   msgs.contains(messagesToSend[3].getContent()));
        assertTrue("Message no found",
                   msgs.contains(messagesToSend[4].getContent()));
        assertTrue("Message no found",
                   msgs.contains(messagesToSend[5].getContent()));

        /**
         * Must return exactly the 3 messages after controlDate1
         */
        rs = metaHistoryService.findFirstMessagesAfter(
            new String[]{MessageHistoryService.class.getName()},
            testMetaContact, controlDate1, 3);

        assertTrue("Nothing found 9", !rs.isEmpty());
        msgs = getMessages(rs);
        assertEquals("Messages must be 3", msgs.size(), 3);
        assertTrue("Message no found",
                   msgs.contains(messagesToSend[2].getContent()));
        assertTrue("Message no found",
                   msgs.contains(messagesToSend[3].getContent()));
        assertTrue("Message no found",
                   msgs.contains(messagesToSend[4].getContent()));

        /**
         * Must return exactly the 3 messages before controlDate2
         */
        rs = metaHistoryService.findLastMessagesBefore(
            new String[]{MessageHistoryService.class.getName()},
            testMetaContact, controlDate2, 3);

        assertTrue("Nothing found 10", !rs.isEmpty());
        msgs = getMessages(rs);
        assertEquals("Messages must be 3", msgs.size(), 3);
        assertTrue("Message no found",
                   msgs.contains(messagesToSend[1].getContent()));
        assertTrue("Message no found",
                   msgs.contains(messagesToSend[2].getContent()));
        assertTrue("Message no found",
                   msgs.contains(messagesToSend[3].getContent()));

    }

    public void callTests()
    {
        /**
         * This must match also many calls, as tests are run many times
         * but the minimum is 3
         */
        Collection<Object> rs = metaHistoryService.findByEndDate(
            new String[]{CallHistoryService.class.getName()},
            null,
            controlDate2);
        Iterator<?> resultIter = getCalls(rs).iterator();

        assertTrue("Calls too few - findByEndDate", rs.size() >= 3);

        /**
         * must find 2 calls
         */
        rs = metaHistoryService.findByPeriod(
            new String[]{CallHistoryService.class.getName()},
            null,
            controlDate1, controlDate2);
        resultIter = rs.iterator();

        assertEquals("Calls must be 2", rs.size(), 2);

        CallRecord rec = (CallRecord)resultIter.next();
        CallPeerRecord participant = rec.getPeerRecords().get(0);

        assertTrue("Participant incorrect ",
                   participant.getPeerAddress().
                   equals(participantAddresses.get(2)));

        rec = (CallRecord)resultIter.next();
        participant = rec.getPeerRecords().get(0);

        assertTrue("Participant incorrect ",
                   participant.getPeerAddress().
                   equals(participantAddresses.get(3)));

        /**
         * must find 1 record
         */
        rs = metaHistoryService.findByStartDate(
            new String[]{CallHistoryService.class.getName()},
            null,
            controlDate2);
        resultIter = rs.iterator();

        assertEquals("Calls must be 2", rs.size(), 2);

        rec = (CallRecord)resultIter.next();
        participant = rec.getPeerRecords().get(0);

        assertTrue("Participant incorrect ",
                   participant.getPeerAddress().
                   equals(participantAddresses.get(4)));

        rec = (CallRecord)resultIter.next();
        participant = rec.getPeerRecords().get(0);

        assertTrue("Participant incorrect ",
                   participant.getPeerAddress().
                   equals(participantAddresses.get(5)));

        /**
         * Must return exactly the last 3 calls
         */
        rs = metaHistoryService.findLast(
            new String[]{CallHistoryService.class.getName()},
            null,
            3);
        resultIter = rs.iterator();

        assertEquals("Calls must be 3", rs.size(), 3);

        rec = (CallRecord)resultIter.next();
        participant = rec.getPeerRecords().get(0);

        assertTrue("Participant incorrect ",
                   participant.getPeerAddress().
                   equals(participantAddresses.get(3)));

        rec = (CallRecord)resultIter.next();
        participant = rec.getPeerRecords().get(0);

        assertTrue("Participant incorrect ",
                   participant.getPeerAddress().
                   equals(participantAddresses.get(4)));

        rec = (CallRecord)resultIter.next();
        participant = rec.getPeerRecords().get(0);

        assertTrue("Participant incorrect ",
                   participant.getPeerAddress().
                   equals(participantAddresses.get(5)));
    }

    public void fileTests()
    {
        /**
         * must find 4 results.
         */
        Collection<FileRecord> rs =
            getFileRecords(
                metaHistoryService.findByStartDate(
                    new String[]{FileHistoryService.class.getName()},
                    testMetaContact, controlDate1));

        assertEquals("Filetransfers must be 4", 4, rs.size());

        rs = getFileRecords(
                metaHistoryService.findByEndDate(
                    new String[]{FileHistoryService.class.getName()},
                    testMetaContact, controlDate2));
        assertTrue("FileTransfers too few - findByEndDate", rs.size() >= 4);

        rs = getFileRecords(
                metaHistoryService.findByPeriod(
                    new String[]{FileHistoryService.class.getName()},
                    testMetaContact, controlDate1, controlDate2));
        assertEquals("Filetransfers must be 2", rs.size(), 2);

        Iterator<FileRecord> it = rs.iterator();
        assertTrue("Filetransfers not found",
                    it.next().getFile().getName().
                        equals(files[2].getName()));
        assertTrue("Filetransfers not found",
                    it.next().getFile().getName().
                        equals(files[3].getName()));

        rs = getFileRecords(
                metaHistoryService.findByPeriod(
                    new String[]{FileHistoryService.class.getName()},
                    testMetaContact, controlDate1, controlDate2,
                    new String[]{"t3"}));
        assertEquals("Filetransfers must be 1", rs.size(), 1);

        it = rs.iterator();
        assertTrue("Filetransfers not found",
                    it.next().getFile().getName().
                        equals(files[2].getName()));

        rs = getFileRecords(
                metaHistoryService.findByPeriod(
                    new String[]{FileHistoryService.class.getName()},
                    testMetaContact, controlDate1, controlDate2,
                    new String[]{"T3"}, true));
        assertEquals("Filetransfers must be 0", rs.size(), 0);

        rs = getFileRecords(
                metaHistoryService.findLast(
                    new String[]{FileHistoryService.class.getName()},
                    testMetaContact, 2));
        assertEquals("Filetransfers must be 2", rs.size(), 2);

        it = rs.iterator();
        FileRecord fr = it.next();
        assertTrue("Filetransfers not found",
                    fr.getFile().getName().
                    equals(files[4].getName()));

        assertTrue("Filetransfers status wrong",
                    fr.getStatus().
                    equals("refused"));
        assertTrue("Filetransfers direction wrong",
                    fr.getDirection().
                    equalsIgnoreCase("out"));

        fr = it.next();
        assertTrue("Filetransfers not found",
                    fr.getFile().getName().
                    equals(files[5].getName()));
        assertTrue("Filetransfers status wrong",
                    fr.getStatus().
                    equals("completed"));
        assertTrue("Filetransfers direction wrong",
                    fr.getDirection().
                    equalsIgnoreCase("in"));

        rs = getFileRecords(
                metaHistoryService.findByKeyword(
                    new String[]{FileHistoryService.class.getName()},
                    testMetaContact,
                    "t3"));
        assertTrue("Filetransfers must be atleast 1", rs.size() > 0);

        it = rs.iterator();
        assertTrue("Filetransfers not found",
                    it.next().getFile().getName().
                        equals(files[2].getName()));

        rs = getFileRecords(
                metaHistoryService.findByKeyword(
                    new String[]{FileHistoryService.class.getName()},
                    testMetaContact,
                    "T3", true));
        assertEquals("Filetransfers must be 0", rs.size(), 0);

        rs = getFileRecords(
                metaHistoryService.findByKeywords(
                    new String[]{FileHistoryService.class.getName()},
                    testMetaContact,
                    new String[]{"t3"}));
        assertTrue("Filetransfers must be atleast 1", rs.size() > 0);

        it = rs.iterator();
        assertTrue("Filetransfers not found",
                    it.next().getFile().getName().
                        equals(files[2].getName()));

        rs = getFileRecords(
                metaHistoryService.findByKeywords(
                    new String[]{FileHistoryService.class.getName()},
                    testMetaContact,
                    new String[]{"T3"}, true));
        assertEquals("Filetransfers must be 0", rs.size(), 0);

        rs = getFileRecords(
                metaHistoryService.findFirstMessagesAfter(
                    new String[]{FileHistoryService.class.getName()},
                    testMetaContact,
                    controlDate1,
                    2));
        assertEquals("Filetransfers must be 2", rs.size(), 2);

        it = rs.iterator();
        fr = it.next();
        assertTrue("Filetransfers not found",
                    fr.getFile().getName().
                    equals(files[2].getName()));
        assertTrue("Filetransfers status wrong",
                    fr.getStatus().
                    equals("completed"));
        assertTrue("Filetransfers direction wrong",
                    fr.getDirection().
                    equalsIgnoreCase("out"));

        fr = it.next();
        assertTrue("Filetransfers not found",
                    fr.getFile().getName().
                    equals(files[3].getName()));
        assertTrue("Filetransfers status wrong",
                    fr.getStatus().
                    equals("active"));
        assertTrue("Filetransfers direction wrong",
                    fr.getDirection().
                    equalsIgnoreCase("in"));

        rs = getFileRecords(
                metaHistoryService.findLastMessagesBefore(
                    new String[]{FileHistoryService.class.getName()},
                    testMetaContact,
                    controlDate1,
                    2));
        assertEquals("Filetransfers must be 2", rs.size(), 2);

        it = rs.iterator();
        fr = it.next();
        assertTrue("Filetransfers not found",
                    fr.getFile().getName().
                    equals(files[0].getName()));
        assertTrue("Filetransfers status wrong",
                    fr.getStatus().
                    equals("canceled"));
        assertTrue("Filetransfers direction wrong",
                    fr.getDirection().
                    equalsIgnoreCase("out"));

        fr = it.next();
        assertTrue("Filetransfers not found",
                    fr.getFile().getName().
                    equals(files[1].getName()));
        assertTrue("Filetransfers status wrong",
                    fr.getStatus().
                    equals("completed"));
        assertTrue("Filetransfers direction wrong",
                    fr.getDirection().
                    equalsIgnoreCase("in"));
    }

    public void metaTests()
    {
        Collection<Object> rs = metaHistoryService.findByStartDate(
            new String[]{
                MessageHistoryService.class.getName(),
                FileHistoryService.class.getName(),
                CallHistoryService.class.getName()},
            testMetaContact, controlDate1);

        assertEquals("Records must be 12", 12, rs.size());
        assertEquals("Filetransfers must be 4", 4, getFileRecords(rs).size());
        assertEquals("CallRecords must be 4", 4, getCalls(rs).size());
        assertEquals("MessageRecords must be 4", 4, getMessages(rs).size());

        rs = metaHistoryService.findByEndDate(
            new String[]{
                MessageHistoryService.class.getName(),
                FileHistoryService.class.getName(),
                CallHistoryService.class.getName()},
            testMetaContact, controlDate1);

        assertTrue("Records must be atleast", rs.size() >= 6);

        rs = metaHistoryService.findByPeriod(
            new String[]{
                MessageHistoryService.class.getName(),
                FileHistoryService.class.getName(),
                CallHistoryService.class.getName()},
            testMetaContact, controlDate1, controlDate2);
        assertEquals("Records must be 6", 6, rs.size());
        assertEquals("Filetransfers must be 2", 2, getFileRecords(rs).size());
        assertEquals("CallRecords must be 2", 2, getCalls(rs).size());
        assertEquals("MessageRecords must be 2", 2, getMessages(rs).size());

        rs = metaHistoryService.findByPeriod(
            new String[]{
                MessageHistoryService.class.getName(),
                FileHistoryService.class.getName(),
                CallHistoryService.class.getName()},
            testMetaContact, controlDate1, controlDate2,
            new String[]{"t4", "participant_address_4", "word4"});
        assertEquals("Records must be 1", 1, rs.size());
        assertEquals("Filetransfers must be 0", 0, getFileRecords(rs).size());
        assertEquals("CallRecords must be 0", 0, getCalls(rs).size());
        assertEquals("MessageRecords must be 1", 1, getMessages(rs).size());

        rs = metaHistoryService.findByPeriod(
            new String[]{
                MessageHistoryService.class.getName(),
                FileHistoryService.class.getName(),
                CallHistoryService.class.getName()},
            testMetaContact, controlDate1, controlDate2,
            new String[]{"T4", "participant_address_4", "word4"},
            true);
        assertEquals("Records must be 0", 0, rs.size());

        rs = metaHistoryService.findByKeyword(
            new String[]{
                MessageHistoryService.class.getName(),
                FileHistoryService.class.getName(),
                CallHistoryService.class.getName()},
            testMetaContact,
            "T1");
        assertTrue("Records must be atleast 2", rs.size() >= 2);

        rs = metaHistoryService.findByKeyword(
            new String[]{
                MessageHistoryService.class.getName(),
                FileHistoryService.class.getName(),
                CallHistoryService.class.getName()},
            testMetaContact,
            "Word6", true);
        assertEquals("Records must be 0", 0, rs.size());

        rs = metaHistoryService.findByKeywords(
            new String[]{
                MessageHistoryService.class.getName(),
                FileHistoryService.class.getName(),
                CallHistoryService.class.getName()},
            testMetaContact,
            new String[]{"t4", "participant_address_4", "word4"});
        assertTrue("Records must be atleast 1", rs.size() >= 1);

        rs = metaHistoryService.findByKeywords(
            new String[]{
                MessageHistoryService.class.getName(),
                FileHistoryService.class.getName(),
                CallHistoryService.class.getName()},
            testMetaContact,
            new String[]{"T4", "participant_address_4", "word4"},
            true);
        assertEquals("Records must be 0", 0, rs.size());

        rs = metaHistoryService.findLast(
            new String[]{
                MessageHistoryService.class.getName(),
                FileHistoryService.class.getName(),
                CallHistoryService.class.getName()},
            testMetaContact,
            4);
        assertEquals("Records must be 4", 4, rs.size());
        assertEquals("Filetransfers must be 2", 2, getFileRecords(rs).size());
        assertEquals("CallRecords must be 0", 0, getCalls(rs).size());
        assertEquals("MessageRecords must be 2", 2, getMessages(rs).size());

        rs = metaHistoryService.findFirstMessagesAfter(
            new String[]{
                MessageHistoryService.class.getName(),
                FileHistoryService.class.getName(),
                CallHistoryService.class.getName()},
            testMetaContact,
            controlDate1,
            2);
        assertEquals("Records must be 2", 2, rs.size());
        assertEquals("Filetransfers must be 0", 0, getFileRecords(rs).size());
        assertEquals("CallRecords must be 2", 2, getCalls(rs).size());
        assertEquals("MessageRecords must be 0", 0, getMessages(rs).size());

        rs = metaHistoryService.findLastMessagesBefore(
            new String[]{
                MessageHistoryService.class.getName(),
                FileHistoryService.class.getName(),
                CallHistoryService.class.getName()},
            testMetaContact,
            controlDate1,
            2);
        assertEquals("Records must be 2", 2, rs.size());
        assertEquals("Filetransfers must be 0", 0, getFileRecords(rs).size());
        assertEquals("CallRecords must be 0", 0, getCalls(rs).size());
        assertEquals("MessageRecords must be 2", 2, getMessages(rs).size());
    }

    /**
     * Removes the locally stored contact list copy. The purpose of this is to
     * leave the local list empty for a next round of testing.
     */
    public void testPurgeLocalContactListCopy()
    {
        metaClService.purgeLocallyStoredContactListCopy();
    }

    private List<String> getMessages(Collection<Object> rs)
    {
        List<String> result = new Vector<String>();

        for (Object item : rs)
        {
            if(item instanceof MessageDeliveredEvent)
                result.add(
                        ((MessageDeliveredEvent)item)
                            .getSourceMessage().getContent());
            else if(item instanceof MessageReceivedEvent)
                result.add(
                        ((MessageReceivedEvent)item)
                            .getSourceMessage().getContent());
        }

        return result;
    }

    private List<CallRecord> getCalls(Collection<Object> rs)
    {
        List<CallRecord> result = new Vector<CallRecord>();

        for (Object item : rs)
            if (item instanceof CallRecord)
                result.add((CallRecord) item);

        return result;
    }

    private Collection<FileRecord> getFileRecords(Collection<Object> rs)
    {
        List<FileRecord> result = new Vector<FileRecord>();

        for (Object item : rs)
            if (item instanceof FileRecord)
                result.add((FileRecord) item);

        return result;
    }

    private class TransferListener
        implements FileTransferListener
    {
        String fileName = null;
        boolean accept = true;
        boolean finishTransfer = true;
        TransferListener(String fileName, boolean accept, boolean finishTransfer)
        {
            this.fileName = fileName;
            this.accept = accept;
            this.finishTransfer = finishTransfer;
        }

        public void fileTransferRequestReceived(FileTransferRequestEvent event)
        {
            if(accept)
            {
                FileTransfer ft = event.getRequest().acceptFile(new File(fileName));
                if(finishTransfer)
                {
                    mockFTOpSet.changeFileTransferStatus(
                        ft, FileTransferStatusChangeEvent.COMPLETED);
                }
                else
                {
                    mockFTOpSet.changeFileTransferStatus(
                        ft, FileTransferStatusChangeEvent.CANCELED);
                }
            }
            else
                event.getRequest().rejectFile();
        }

        public void fileTransferCreated(FileTransferCreatedEvent event)
        {

        }

        public void fileTransferRequestRejected(FileTransferRequestEvent event)
        {
        }

        public void fileTransferRequestCanceled(FileTransferRequestEvent event)
        {
        }
    }
}
