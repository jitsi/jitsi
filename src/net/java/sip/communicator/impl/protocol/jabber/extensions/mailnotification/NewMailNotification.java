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
 * A straightforward IQ extension. A <tt>NewMailNotification</tt> object is
 * created via the <tt>NewMailNotificationProvider</tt>. It contains the
 * information we need in order to determine whether there are new mails waiting
 * for us on the mail server.
 *
 * @author Matthieu Helleringer
 * @author Alain Knaebel
 */
public class NewMailNotification extends IQ
{
    /**
     * Logger for this class
     */
    private static final Logger logger =
        Logger.getLogger(NewMailNotification.class);

    /**
     * The name space for new mail notification packets.
     */
    public static final String NAMESPACE = "google:mail:notify";

    /**
     * Returns the name space of the new-mail element
     *
     * @return the XML name space of the new-mail element.
     */
    public String getXmlsns()
    {
        return NAMESPACE;
    }

    /**
     * Returns the sub-element XML section of the IQ packet.
     *
     * @return the child element section of the IQ XML
     */
    @Override
    public String getChildElementXML()
    {
        logger.trace("NewMailNotification.getChildElementXML usage");
        return "<iq type='"+"result"+"' "+
                "from='"+getFrom()+"' "+
                "to='"+getTo()+"' "+
                "id='"+getPacketID()+"' />";
    }
}
