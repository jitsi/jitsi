/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.mailnotification;

import net.java.sip.communicator.util.*;

import org.jivesoftware.smack.packet.*;

/**
 * A straightforward IQ extension. A <tt>NewMailNotification</tt> object is
 * created via the <tt>NewMailNotificationProvider</tt>. It contains the
 * information we need in order to determine whether there are new mails waiting
 * for us on the mail server.
 *
 * @author Matthieu Helleringer
 * @author Alain Knaebel
 * @author Emil Ivov
 */
public class NewMailNotificationIQ extends IQ
{
    /**
     * Logger for this class
     */
    private static final Logger logger =
        Logger.getLogger(NewMailNotificationIQ.class);

    /**
     * The name space for new mail notification packets.
     */
    public static final String NAMESPACE = "google:mail:notify";

    /**
     * The name of the element that Google use to transport new mail
     * notifications.
     */
    public static final String ELEMENT_NAME = "new-mail";

    /**
     * Returns the sub-element XML section of the IQ packet.
     *
     * @return the child element section of the IQ XML
     */
    @Override
    public String getChildElementXML()
    {
        if (logger.isTraceEnabled())
            logger.trace("NewMailNotification.getChildElementXML usage");
        return "<iq type='"+"result"+"' "+
                "from='"+getFrom()+"' "+
                "to='"+getTo()+"' "+
                "id='"+getPacketID()+"' />";
    }
}
