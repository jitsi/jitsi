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
package net.java.sip.communicator.impl.protocol.jabber.extensions.health;

import net.java.sip.communicator.service.protocol.jabber.*;

import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.provider.*;

import org.xmlpull.v1.*;

/**
 * The <tt>IQProvider</tt> for {@link HealthCheckIQ}.
 *
 * @author Pawel Domas
 */
public class HealthCheckIQProvider
    implements IQProvider
{
    /**
     * Registers <tt>HealthCheckIQProvider</tt> as an <tt>IQProvider</tt>
     * in {@link AbstractSmackInteroperabilityLayer}.
     */
    public static void registerIQProvider()
    {
        AbstractSmackInteroperabilityLayer smackInteropLayer =
            AbstractSmackInteroperabilityLayer.getInstance();

        // ColibriStatsIQ
        smackInteropLayer.addIQProvider(
            HealthCheckIQ.ELEMENT_NAME,
            HealthCheckIQ.NAMESPACE,
            new HealthCheckIQProvider());
    }

    /**
     * Parses <tt>HealthCheckIQ</tt>.
     *
     * {@inheritDoc}
     */
    @Override
    public IQ parseIQ(XmlPullParser parser)
        throws Exception
    {
        String namespace = parser.getNamespace();
        IQ iq;

        if (HealthCheckIQ.ELEMENT_NAME.equals(parser.getName())
            && HealthCheckIQ.NAMESPACE.equals(namespace))
        {
            String rootElement = parser.getName();

            iq = new HealthCheckIQ();

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
                }
            }
        }
        else
            iq = null;

        return iq;
    }
}
