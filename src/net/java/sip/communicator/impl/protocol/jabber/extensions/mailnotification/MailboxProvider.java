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
public class MailboxProvider
        implements IQProvider
{
    /**
     * Logger for this class
     */
    private static final Logger logger =
        Logger.getLogger(MailboxProvider.class);

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
        logger.debug("Mailbox.getChildElementXML usage");
        Mailbox iq = new Mailbox();
        iq.setFrom(parser.getAttributeValue("", "from"));
        iq.setTo(parser.getAttributeValue("", "to"));
        iq.setPacketID(parser.getAttributeValue("", "id"));
        iq.setResultTime(Long.parseLong(
                parser.getAttributeValue("", "result-time")));
        iq.setTotalMatched(Integer.parseInt(
                parser.getAttributeValue("", "total-matched")));
        iq.setTotalEstimate("1".equals(
                parser.getAttributeValue("", "total-estimate")));
        iq.setUrl(parser.getAttributeValue("", "url"));

        int eventType = parser.next();
        String name = parser.getName();
        if (eventType == XmlPullParser.START_TAG)
        {
            if ("mail-thread-info".equals(name))
            {
                iq.setDate(Long.parseLong(
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
                                iq.setSender(parser.
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
                            iq.setSubject(name);
                        }
                    }
                }
            }
        }
        return iq;
    }
}
