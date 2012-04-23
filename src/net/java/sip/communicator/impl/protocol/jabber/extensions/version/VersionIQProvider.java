/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.version;

import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.provider.*;
import org.xmlpull.v1.*;

/**
 * XEP-0092: Software Version.
 *
 * @author Damian Minkov
 */
public class VersionIQProvider
    implements IQProvider
{
    /**
     * Creates empty Version packet to register the request.
     *
     * @param parser the parser.
     * @return the new packet.
     * @throws Exception if
     */
    public IQ parseIQ(XmlPullParser parser)
        throws
        Exception
    {
        return new org.jivesoftware.smackx.packet.Version();
    }
}
