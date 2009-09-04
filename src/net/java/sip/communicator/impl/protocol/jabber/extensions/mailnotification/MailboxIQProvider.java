/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.mailnotification;

import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.provider.*;
import org.xmlpull.v1.XmlPullParser;

import net.java.sip.communicator.util.*;

/**
 * A straightforward implementation of the IQProvider. Parses custom IQ packets.
 * We receive IQ mailbox packets from google mail servers and we use them to
 * create <tt>Mailbox</tt> objects which contain all the information from the
 * packet.
 *
 * @author Matthieu Helleringer
 * @author Alain Knaebel
 */
public class MailboxIQProvider
        implements IQProvider
{
    /**
     * Logger for this class
     */
    private static final Logger logger =
        Logger.getLogger(MailboxIQProvider.class);

    /**
     * Return an <tt>IQ</tt> (i.e. <tt>Mailbox</tt>) object which will contain
     * the information we get from the parser.
     *
     * @param parser the <tt>XmlPullParser</tt> that we can use to get
     * packet details.
     *
     * @return a new IQ instance which is the result of the XmlPullParser.
     * @throws Exception if an error occurs parsing the XML.
     */
    public IQ parseIQ(final XmlPullParser parser) throws Exception
    {
        MailboxIQ mailboxIQ = new MailboxIQ();
        mailboxIQ.setFrom(parser.getAttributeValue("", "from"));
        mailboxIQ.setTo(parser.getAttributeValue("", "to"));
        mailboxIQ.setPacketID(parser.getAttributeValue("", "id"));
        mailboxIQ.setResultTime(Long.parseLong(
                parser.getAttributeValue("", "result-time")));
        mailboxIQ.setTotalMatched(Integer.parseInt(
                parser.getAttributeValue("", "total-matched")));
        mailboxIQ.setTotalEstimate("1".equals(
                parser.getAttributeValue("", "total-estimate")));
        mailboxIQ.setUrl(parser.getAttributeValue("", "url"));


        boolean done = false;
        while(!done)
        {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG)
            {
                String name = parser.getName();
                if(MailThreadInfo.ELEMENT_NAME.equals(name))
                {
                    //parse mail thread information

                }

            }
            else if (eventType == XmlPullParser.END_TAG)
            {
                if (parser.getName().equals("field"))
                {
                    done = true;
                }
            }
            else
            {
                if(logger.isTraceEnabled())
                {
                    logger.trace("xml parser returned eventType=" + eventType);
                    logger.trace("parser="+parser);
                }
            }
        }
        int eventType = 1;
        String name = null;
        if (eventType == XmlPullParser.START_TAG)
        {
            if ("mail-thread-info".equals(name))
            {
                mailboxIQ.setDate(Long.parseLong(
                                parser.getAttributeValue("", "date")));
                for (int i =0;i<10;i++)
                {
                    eventType = parser.next();
                    if (eventType == XmlPullParser.START_TAG)
                    {
                        name = parser.getName();
                        if ("sender".equals(name))
                        {
                            if ( "1".equals(
                                    parser.getAttributeValue("","originator")))
                            {
                                mailboxIQ.setSender(parser.
                                        getAttributeValue("", "address"));
                            }
                        }
                    }

                    if (eventType == XmlPullParser.START_TAG)
                    {
                        name = parser.getName();
                        if ("subject".equals(name))
                        {
                            name = parser.nextText();
                            mailboxIQ.setSubject(name);
                        }
                    }
                }
            }
        }
        return mailboxIQ;
    }
}
