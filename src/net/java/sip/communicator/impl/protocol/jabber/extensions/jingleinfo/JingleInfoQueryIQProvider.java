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
package net.java.sip.communicator.impl.protocol.jabber.extensions.jingleinfo;

import net.java.sip.communicator.impl.protocol.jabber.extensions.*;

import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.provider.*;
import org.xmlpull.v1.*;

/**
 * Provider for the <tt>JingleInfoQueryIQ</tt>.
 *
 * @author Sebastien Vincent
 */
public class JingleInfoQueryIQProvider
    implements IQProvider
{
    /**
     * STUN packet extension provider.
     */
    private final PacketExtensionProvider stunProvider =
        new StunProvider();

    /**
     * Relay packet extension provider.
     */
    private final PacketExtensionProvider relayProvider =
        new RelayProvider();

    /**
     * Creates a new instance of the <tt>JingleInfoQueryIQProvider</tt> and
     * register all related extension providers. It is the responsibility of the
     * application to register the <tt>JingleInfoQueryIQProvider</tt> itself.
     */
    public JingleInfoQueryIQProvider()
    {
        ProviderManager providerManager = ProviderManager.getInstance();

        providerManager.addExtensionProvider(
                ServerPacketExtension.ELEMENT_NAME,
                ServerPacketExtension.NAMESPACE,
                new DefaultPacketExtensionProvider
                    <ServerPacketExtension>(ServerPacketExtension.class));
    }

    /**
     * Parses a JingleInfoQueryIQ</tt>.
     *
     * @param parser an XML parser.
     * @return a new {@link JingleInfoQueryIQ} instance.
     * @throws Exception if an error occurs parsing the XML.
     */
    public IQ parseIQ(XmlPullParser parser)
        throws Exception
    {
        boolean done = false;
        JingleInfoQueryIQ iq = new JingleInfoQueryIQ();

        // Now go on and parse the session element's content.
        while (!done)
        {
            int eventType = parser.next();
            String elementName = parser.getName();

            if (eventType == XmlPullParser.START_TAG)
            {
                if(elementName.equals(StunPacketExtension.ELEMENT_NAME))
                {
                    iq.addExtension(stunProvider.parseExtension(parser));
                }
                else if(elementName.equals(RelayPacketExtension.ELEMENT_NAME))
                {
                    iq.addExtension(relayProvider.parseExtension(parser));
                }
            }
            if (eventType == XmlPullParser.END_TAG)
            {
                if (parser.getName().equals(JingleInfoQueryIQ.ELEMENT_NAME))
                {
                    done = true;
                }
            }
        }

        return iq;
    }
}
