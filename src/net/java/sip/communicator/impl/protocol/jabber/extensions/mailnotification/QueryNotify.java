/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.mailnotification;

import org.jivesoftware.smack.packet.*;
import net.java.sip.communicator.util.*;

/**
 * A straightforward <tt>IQ</tt> extension. The <tt>QueryNotify</tt> object is
 * used to create queries for the GMail mail server. It creates a simple
 * <tt>IQ</tt> packet which represents the query.
 *
 * @author Matthieu Helleringer
 * @author Alain Knaebel
 */
public class QueryNotify extends IQ
{
    /**
     * Logger for this class
     */
    private static final Logger logger =
        Logger.getLogger(QueryNotify.class);

    /**
     * the value of the result time of the last packet
     */
    private long timeResult;

    /**
     * Returns the sub-element XML section of the IQ packet.
     *
     * @return the child element section of the IQ XML. String
     */
    @Override
    public String getChildElementXML()
    {
        logger.debug("QueryNotify.getChildElementXML usage");

        return "<query xmlns='google:mail:notify'"
               +"newer-than-time='" +this.timeResult+"'/>";
    }

    /**
     * Creates a <tt>QueryNotify</tt> instance with <tt>time</tt> as the value
     * of the "newer-than-time" attribute.
     *
     * @param time the value of the "newer-than-time" attribute.
     */
    public QueryNotify(long time)
    {
        this.timeResult = time;
    }
}
