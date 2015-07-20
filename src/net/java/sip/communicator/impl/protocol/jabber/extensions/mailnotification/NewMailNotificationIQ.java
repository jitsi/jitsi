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
