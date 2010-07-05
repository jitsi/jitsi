/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.jingle;
import net.java.sip.communicator.util.*;

import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.provider.*;
import org.xmlpull.v1.XmlPullParser;


/**
 * An implementation of a Jingle IQ provider that parses incoming Jingle IQs.
 *
 * @author Emil Ivov
 */
public class JingleIQProvider implements IQProvider
{
    /**
     * The <tt>Logger</tt> used by the <tt>JingleIQProvider</tt>
     * class and its instances for logging output.
     */
    private static final Logger logger = Logger
                    .getLogger(JingleIQProvider.class.getName());

    /* (non-Javadoc)
     * @see org.jivesoftware.smack.provider.IQProvider#parseIQ(org.xmlpull.v1.XmlPullParser)
     */
    public IQ parseIQ(XmlPullParser parser) throws Exception
    {
        // TODO Auto-generated method stub
        return null;
    }
}
