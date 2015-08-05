/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.mailnotification;

import net.java.sip.communicator.util.*;

import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.provider.*;
import org.xmlpull.v1.*;

/**
 * A straightforward implementation of an <tt>IQProvider</tt>. Parses custom
 * IQ packets related to new mail notifications from Google servers.
 * We receive IQ packets from the mail server to notify us that new mails are
 * available.
 *
 * @author Matthieu Helleringer
 * @author Alain Knaebel
 */
public class NewMailNotificationProvider
        implements IQProvider
{
    /**
     * Logger for this class
     */
    private static final Logger logger =
        Logger.getLogger(NewMailNotificationProvider.class);

    /**
     * Returns an <tt>NewMailNotification</tt> instance containing the result
     * of the XMPP's packet parsing.
     *
     * @param parser the <tt>XmlPullParser</tt> that has the content of the
     * packet.
     * @return a new <tt>NewMailNotification</tt> instance with the result from
     * the <tt>XmlPullParser</tt>.
     * @throws Exception if an error occurs parsing the XML.
     */
    public IQ parseIQ(final XmlPullParser parser) throws Exception
    {
        if (logger.isDebugEnabled())
            logger.debug("NewMailNotificationProvider.getChildElementXML usage");
        NewMailNotificationIQ iq = new NewMailNotificationIQ();

        return iq;
    }
}
