/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.slick.msghistory;

import java.util.*;

import org.osgi.framework.*;
import junit.framework.*;
import net.java.sip.communicator.impl.protocol.mock.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.history.*;
import net.java.sip.communicator.service.history.records.*;
import net.java.sip.communicator.service.msghistory.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * Tests message history.
 * First installs the MoxkProtocolProvider to be able to send some messages
 * The message history service stores them
 * and then tests the verious find methods - does they find the messsages we have
 * already sent
 *
 * @author Damian Minkov
 */
public class TestMsgHistoryService
    extends TestCase
{
    private static final Logger logger = Logger.getLogger(TestMsgHistoryService.class);

    static final String TEST_CONTACT_NAME = "Mincho_Penchev";

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
    public static MockBasicInstantMessaging mockBImOpSet = null;

    private static ServiceReference msgHistoryServiceRef = null;
    public static MessageHistoryService msgHistoryService = null;

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

    public TestMsgHistoryService(String name)
    {
        super(name);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite();
        suite.addTest(
            new TestMsgHistoryService("setupContact"));
        suite.addTest(
            new TestMsgHistoryService("writeRecords"));
        suite.addTest(
            new TestMsgHistoryService("readRecords"));
        suite.addTest(
            new TestMsgHistoryService("testPurgeLocalContactListCopy"));

        return suite;
    }

    protected void setUp() throws Exception
    {
    }

    protected void tearDown() throws Exception
    {
    }

    public void setupContact()
    {

        mockProvider = new MockProvider("MessageHistoryMockUser");

        //store thre presence op set of the new provider into the fixture
        Map supportedOperationSets =
            mockProvider.getSupportedOperationSets();

        //get the operation set presence here.
        mockPresOpSet =
            (MockPersistentPresenceOperationSet) supportedOperationSets.get(
                OperationSetPersistentPresence.class.getName());

        mockBImOpSet =
            (MockBasicInstantMessaging) supportedOperationSets.get(
                OperationSetBasicInstantMessaging.class.getName());

        msgHistoryServiceRef =
            MsgHistoryServiceLick.bc.
            getServiceReference(MessageHistoryService.class.getName());

        msgHistoryService =
            (MessageHistoryService)MsgHistoryServiceLick.bc.
                getService(msgHistoryServiceRef);

        // fill in a contact to comunicate with
        MockContactGroup root =
            (MockContactGroup)mockPresOpSet.getServerStoredContactListRoot();

        testContact = new MockContact(TEST_CONTACT_NAME, mockProvider);
        root.addContact(testContact);

        metaCLref = MsgHistoryServiceLick.bc.getServiceReference(
            MetaContactListService.class.getName());

        metaClService =
            (MetaContactListService)MsgHistoryServiceLick.bc.getService(metaCLref);

       System.setProperty(MetaContactListService.PROVIDER_MASK_PROPERTY, "1");

       Hashtable mockProvProperties = new Hashtable();
       mockProvProperties.put(ProtocolProviderFactory.PROTOCOL
                              , mockProvider.getProtocolName());
       mockProvProperties.put(MetaContactListService.PROVIDER_MASK_PROPERTY,
                              "1");

       mockPrServiceRegistration =
           MsgHistoryServiceLick.bc.registerService(
               ProtocolProviderService.class.getName(),
               mockProvider,
               mockProvProperties);
       logger.debug("Registered a mock protocol provider! ");

       testMetaContact = metaClService.getRoot().
            getMetaContact(mockProvider, TEST_CONTACT_NAME);

        messagesToSend = new Message[]
            {
                mockBImOpSet.createMessage("test message word1"),
                mockBImOpSet.createMessage("test message word2"),
                mockBImOpSet.createMessage("test message word3"),
                mockBImOpSet.createMessage("test message word4"),
                mockBImOpSet.createMessage("test message word5")
            };

    }

    /**
     *  First send the messages
     */
    public void writeRecords()
    {

        logger.info("write records ");

        assertNotNull("No metacontact", testMetaContact);

        // First deliver message, so they are stored by the message history service
        mockBImOpSet.deliverMessage(TEST_CONTACT_NAME, messagesToSend[0]);

        this.controlDate1 = new Date();

        Object lock = new Object();
        synchronized (lock)
        {
            // wait a moment
            try
            {
                lock.wait(200);
            }
            catch (InterruptedException ex)
            {
            }
        }

        mockBImOpSet.deliverMessage(TEST_CONTACT_NAME, messagesToSend[1]);

        mockBImOpSet.deliverMessage(TEST_CONTACT_NAME, messagesToSend[2]);

        this.controlDate2 = new Date();
        synchronized (lock)
        {
            // wait a moment
            try
            {
                lock.wait(200);
            }
            catch (InterruptedException ex)
            {
            }
        }

        mockBImOpSet.deliverMessage(TEST_CONTACT_NAME, messagesToSend[3]);

        mockBImOpSet.deliverMessage(TEST_CONTACT_NAME, messagesToSend[4]);

    }

    /**
     * tests all read methods (finders)
     */
    public void readRecords()
    {
        /**
         * This matches all written messages, they are minimum 5
         */
        QueryResultSet rs = msgHistoryService.findByKeyword(testMetaContact, "test");

        assertTrue("Nothing found findByKeyword ", rs.hasNext());

        Vector msgs = getMessages(rs);

        assertTrue("Messages too few - findByKeyword", msgs.size() >= 5);

        /**
         * This must match also many messages, as tests are run many times
         * but the minimum is 3
         */
        rs = msgHistoryService.findByEndDate(testMetaContact, controlDate2);

        assertTrue("Nothing found findByEndDate", rs.hasNext());

        msgs = getMessages(rs);

        assertTrue("Messages too few - findByEndDate", msgs.size() >= 3);

        /**
         * This must find also many messages but atleast one
         */
        rs = msgHistoryService.findByKeywords(
            testMetaContact,
            new String[]{"test", "word2"});

        assertTrue("Nothing found findByKeywords", rs.hasNext());
        msgs = getMessages(rs);
        assertTrue("Messages too few - findByKeywords", msgs.size() >= 1);

        /**
         * Nothing to be found
         */
        rs = msgHistoryService.findByKeywords(
            testMetaContact,
            new String[]{"test1", "word2"});

        assertFalse("Something found findByKeywords", rs.hasNext());

        /**
         * must find 2 messages
         */
        rs = msgHistoryService.findByPeriod(
            testMetaContact, controlDate1, controlDate2);

        assertTrue("Nothing found findByPeriod", rs.hasNext());

        msgs = getMessages(rs);
        assertEquals("Messages must be 2", msgs.size(), 2);

        assertTrue("Message no found",
                   msgs.contains(messagesToSend[1].getContent()));
        assertTrue("Message no found",
                   msgs.contains(messagesToSend[2].getContent()));

        /**
         * must find 1 record
         */
        rs = msgHistoryService.findByPeriod(
            testMetaContact, controlDate1, controlDate2, new String[]{"word2"});

        assertTrue("Nothing found findByPeriod", rs.hasNext());

        msgs = getMessages(rs);

        assertEquals("Messages must be 1", msgs.size(), 1);
        assertTrue("Message no found",
                   msgs.contains(messagesToSend[1].getContent()));

        /**
         * must find 2 records
         */
        rs = msgHistoryService.findByStartDate(testMetaContact, controlDate2);

        assertTrue("Nothing found findByStartDate", rs.hasNext());
        msgs = getMessages(rs);
        assertEquals("Messages must be 2", msgs.size(), 2);
        assertTrue("Message no found",
                   msgs.contains(messagesToSend[3].getContent()));
        assertTrue("Message no found",
                   msgs.contains(messagesToSend[4].getContent()));

        /**
         * Must return exactly the last 3 messages
         */
        rs = msgHistoryService.findLast(testMetaContact, 3);

        assertTrue("Nothing found 8", rs.hasNext());
        msgs = getMessages(rs);
        assertEquals("Messages must be 3", msgs.size(), 3);
        assertTrue("Message no found",
                   msgs.contains(messagesToSend[2].getContent()));
        assertTrue("Message no found",
                   msgs.contains(messagesToSend[3].getContent()));
        assertTrue("Message no found",
                   msgs.contains(messagesToSend[4].getContent()));

    }

    /**
     * Removes the locally stored contact list copy. The purpose of this is to
     * leave the local list empty for a next round of testing.
     */
    public void testPurgeLocalContactListCopy()
    {
        metaClService.purgeLocallyStoredContactListCopy();
    }

    private Vector getMessages(QueryResultSet rs)
    {
        Vector result = new Vector();
        while (rs.hasNext())
        {
            HistoryRecord hr = (HistoryRecord)rs.next();
            for (int i = 0; i < hr.getPropertyNames().length; i++)
            {
                if(hr.getPropertyNames()[i].equals("msg"))
                {
                    result.add(hr.getPropertyValues()[i]);
                    break;
                }
            }
        }

        return result;
    }

    private void dumpResult(QueryResultSet rs)
    {
        while (rs.hasNext())
        {
            HistoryRecord hr = (HistoryRecord)rs.next();
            logger.info("----------------------");

            for (int i = 0; i < hr.getPropertyNames().length; i++)
            {
                logger.info(hr.getPropertyNames()[i] + " => " + hr.getPropertyValues()[i]);
            }

            logger.info("----------------------");
        }
    }
}
