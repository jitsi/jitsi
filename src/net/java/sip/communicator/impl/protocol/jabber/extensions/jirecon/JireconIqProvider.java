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
package net.java.sip.communicator.impl.protocol.jabber.extensions.jirecon;

import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.provider.*;

import org.xmlpull.v1.*;

/**
 * The IQ provider implementation for {@link JireconIq}.
 *
 * @author Pawel Domas
 */
public class JireconIqProvider
    implements IQProvider
{
    /**
     * Name space of Jirecon packet extension.
     */
    public static final String NAMESPACE = "http://jitsi.org/protocol/jirecon";

    /**
     * Registers this IQ provider into given <tt>ProviderManager</tt>.
     * @param providerManager the <tt>ProviderManager</tt> to which this
     *                        instance wil be bound to.
     */
    public void registerJireconIQs(ProviderManager providerManager)
    {
        // <recording/>
        providerManager.addIQProvider(
                JireconIq.ELEMENT_NAME,
                JireconIq.NAMESPACE,
                this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IQ parseIQ(XmlPullParser parser)
        throws Exception
    {
        String namespace = parser.getNamespace();

        // Check the namespace
        if (!NAMESPACE.equals(namespace))
        {
            return null;
        }

        String rootElement = parser.getName();

        JireconIq iq;

        if (JireconIq.ELEMENT_NAME.equals(rootElement))
        {
            iq = new JireconIq();

            String action
                = parser.getAttributeValue("", JireconIq.ACTION_ATTR_NAME);
            String mucjid
                = parser.getAttributeValue("", JireconIq.MUCJID_ATTR_NAME);
            String output
                = parser.getAttributeValue("", JireconIq.OUTPUT_ATTR_NAME);
            String rid
                = parser.getAttributeValue("", JireconIq.RID_ATTR_NAME);
            String status
                = parser.getAttributeValue("", JireconIq.STATUS_ATTR_NAME);

            iq.setAction(
                JireconIq.Action.parse(action));

            iq.setStatus(
                JireconIq.Status.parse(status));

            iq.setMucJid(mucjid);

            iq.setOutput(output);

            iq.setRid(rid);
        }
        else
        {
            return null;
        }

        boolean done = false;

        while (!done)
        {
            switch (parser.next())
            {
                case XmlPullParser.END_TAG:
                {
                    String name = parser.getName();

                    if (rootElement.equals(name))
                    {
                        done = true;
                    }
                    break;
                }

                case XmlPullParser.TEXT:
                {
                    // Parse some text here
                    break;
                }
            }
        }

        return iq;
    }
}
