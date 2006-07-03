
/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.slick.msghistory;

import org.osgi.framework.*;
import junit.framework.*;
import net.java.sip.communicator.service.msghistory.MessageHistoryService;
import net.java.sip.communicator.service.contactlist.MetaContactListService;
import java.util.*;
import net.java.sip.communicator.util.Logger;
import net.java.sip.communicator.service.contactlist.MetaContact;
import net.java.sip.communicator.service.history.QueryResultSet;

public class TestMsgHistoryService
    extends TestCase
{
    private static final Logger logger = Logger.getLogger(TestMsgHistoryService.class);
    private ServiceReference msgHistoryServiceRef = null;
    private MessageHistoryService msgHistoryService = null;

    public TestMsgHistoryService(String name) throws Exception
    {
        super(name);

        logger.info("111111111111111111111111111111111111111111111111111");


        ServiceReference ref = MsgHistoryServiceLick.bc.getServiceReference(
            MetaContactListService.class.getName());
        MetaContactListService metaClService
            = (MetaContactListService)MsgHistoryServiceLick.bc.getService(ref);

        Iterator iter = metaClService.getRoot().getChildContacts();
        while (iter.hasNext())
        {
            Object item = (Object) iter.next();

            logger.debug("item --------- " + item);

        }

    }

    protected void setUp() throws Exception
    {
        BundleContext context = MsgHistoryServiceLick.bc;

        msgHistoryServiceRef =
            context.getServiceReference(MessageHistoryService.class.getName());

        msgHistoryService = (MessageHistoryService)context.getService(msgHistoryServiceRef);
    }

    protected void tearDown() throws Exception
    {
        BundleContext context = MsgHistoryServiceLick.bc;

        context.ungetService(this.msgHistoryServiceRef);

        this.msgHistoryService = null;
        this.msgHistoryServiceRef = null;
    }

    public void testWriteRecords()
    {
        ServiceReference ref = MsgHistoryServiceLick.bc.getServiceReference(
            MetaContactListService.class.getName());
        MetaContactListService metaClService
            = (MetaContactListService)MsgHistoryServiceLick.bc.getService(ref);

        Calendar c = Calendar.getInstance();
        c.set(Calendar.YEAR, c.get(Calendar.YEAR) - 10);
        Date date = c.getTime();

        Iterator iter = metaClService.getRoot().getChildContacts();
        while (iter.hasNext())
        {
            MetaContact item = (MetaContact) iter.next();

            logger.debug("item --------- " + item);
            QueryResultSet rs =
            msgHistoryService.findByStartDate(item, date);
            Iterator iter1 = rs;
            while (iter1.hasNext())
            {
                logger.debug("222222222222222222222 " + iter1.next());
            }
        }
    }

    public void testReadRecords()
    {

    }

}
