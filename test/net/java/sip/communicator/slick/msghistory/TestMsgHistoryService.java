
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
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.history.*;
import net.java.sip.communicator.service.history.records.*;
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

    private ServiceReference metaCLref = null;
    private MetaContactListService metaClService = null;

    private MetaContact testMetaContact = null;

    Message[] messagesToSend = null;


    public TestMsgHistoryService(String name) throws Exception
    {
        super(name);
    }

    protected void setUp() throws Exception
    {
        metaCLref = MsgHistoryServiceLick.bc.getServiceReference(
            MetaContactListService.class.getName());

        metaClService = (MetaContactListService)MsgHistoryServiceLick.bc.getService(metaCLref);

        testMetaContact =
            metaClService.getRoot().
                    getMetaContact(
                        MsgHistoryServiceLick.mockProvider,
                        MsgHistoryServiceLick.TEST_CONTACT_NAME);

        messagesToSend = new Message[]
            {
                MsgHistoryServiceLick.mockBImOpSet.createMessage("test message word1"),
                MsgHistoryServiceLick.mockBImOpSet.createMessage("test message word2"),
                MsgHistoryServiceLick.mockBImOpSet.createMessage("test message word3"),
                MsgHistoryServiceLick.mockBImOpSet.createMessage("test message word4"),
                MsgHistoryServiceLick.mockBImOpSet.createMessage("test message word5")
            };
    }

    protected void tearDown() throws Exception
    {
        BundleContext context = MsgHistoryServiceLick.bc;

        context.ungetService(this.metaCLref);

        this.metaClService = null;
        this.metaCLref = null;
    }

    /**
     * First send the messages then tests all read methods (finders)
     */
    public void testReadRecords()
    {
        // First deliver message, so they are stored by the message history service
        MsgHistoryServiceLick.mockBImOpSet.deliverMessage(
            MsgHistoryServiceLick.TEST_CONTACT_NAME, messagesToSend[0]);

        Date controlDate1 = new Date();

        Object lock = new Object();
        synchronized(lock){
            // wait a moment
            try{lock.wait(200);}
            catch (InterruptedException ex){}
        }

        MsgHistoryServiceLick.mockBImOpSet.deliverMessage(
            MsgHistoryServiceLick.TEST_CONTACT_NAME, messagesToSend[1]);

        MsgHistoryServiceLick.mockBImOpSet.deliverMessage(
            MsgHistoryServiceLick.TEST_CONTACT_NAME, messagesToSend[2]);

        Date controlDate2 = new Date();
        synchronized(lock){
            // wait a moment
            try{lock.wait(200);}
            catch (InterruptedException ex){}
        }

        MsgHistoryServiceLick.mockBImOpSet.deliverMessage(
            MsgHistoryServiceLick.TEST_CONTACT_NAME, messagesToSend[3]);

        MsgHistoryServiceLick.mockBImOpSet.deliverMessage(
            MsgHistoryServiceLick.TEST_CONTACT_NAME, messagesToSend[4]);


        /**
         * This matches all written messages, they are minimum 5
         */
        QueryResultSet rs = MsgHistoryServiceLick.msgHistoryService.findByKeyword(
                testMetaContact, "test");

        assertTrue("Nothing found findByKeyword ", rs.hasNext());

        Vector msgs = getMessages(rs);

        assertTrue("Messages too few - findByKeyword", msgs.size() >= 5);


        /**
         * This must match also many messages, as tests are run many times
         * but the minimum is 3
         */
        rs = MsgHistoryServiceLick.msgHistoryService.findByEndDate(
                testMetaContact, controlDate2);

        assertTrue("Nothing found findByEndDate", rs.hasNext());

        msgs = getMessages(rs);

        assertTrue("Messages too few - findByEndDate", msgs.size() >= 3);

        /**
         * This must find also many messages but atleast one
         */
        rs = MsgHistoryServiceLick.msgHistoryService.findByKeywords(
                testMetaContact, new String[]{"test", "word2"});

        assertTrue("Nothing found findByKeywords", rs.hasNext());
        msgs = getMessages(rs);
        assertTrue("Messages too few - findByKeywords", msgs.size() >= 1);

        /**
         * Nothing to be found
         */
        rs = MsgHistoryServiceLick.msgHistoryService.findByKeywords(
                testMetaContact, new String[]{"test1", "word2"});

        assertFalse("Something found findByKeywords", rs.hasNext());

        /**
         * must find 2 messages
         */
        rs = MsgHistoryServiceLick.msgHistoryService.findByPeriod(
                testMetaContact, controlDate1, controlDate2);

        assertTrue("Nothing found findByPeriod", rs.hasNext());

        msgs = getMessages(rs);
        assertEquals("Messages must be 2", msgs.size(), 2);

        assertTrue("Message no found", msgs.contains(messagesToSend[1].getContent()));
        assertTrue("Message no found", msgs.contains(messagesToSend[2].getContent()));

        /**
         * must find 1 record
         */
        rs = MsgHistoryServiceLick.msgHistoryService.findByPeriod(
                testMetaContact, controlDate1, controlDate2, new String[]{"word2"});

        assertTrue("Nothing found findByPeriod", rs.hasNext());

        msgs = getMessages(rs);

        assertEquals("Messages must be 1", msgs.size(), 1);
        assertTrue("Message no found", msgs.contains(messagesToSend[1].getContent()));

        /**
         * must find 2 records
         */
        rs = MsgHistoryServiceLick.msgHistoryService.findByStartDate(
                testMetaContact, controlDate2);

        assertTrue("Nothing found findByStartDate", rs.hasNext());
        msgs = getMessages(rs);
        assertEquals("Messages must be 2", msgs.size(), 2);
        assertTrue("Message no found", msgs.contains(messagesToSend[3].getContent()));
        assertTrue("Message no found", msgs.contains(messagesToSend[4].getContent()));

        /**
         * Must return exactly the last 3 messages
         */
        rs = MsgHistoryServiceLick.msgHistoryService.findLast(
                testMetaContact, 3);

        assertTrue("Nothing found 8", rs.hasNext());
        msgs = getMessages(rs);
        assertEquals("Messages must be 3", msgs.size(), 3);
        assertTrue("Message no found", msgs.contains(messagesToSend[2].getContent()));
        assertTrue("Message no found", msgs.contains(messagesToSend[3].getContent()));
        assertTrue("Message no found", msgs.contains(messagesToSend[4].getContent()));
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
