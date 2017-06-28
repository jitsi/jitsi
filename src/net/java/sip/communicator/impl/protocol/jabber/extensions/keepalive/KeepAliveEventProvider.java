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
package net.java.sip.communicator.impl.protocol.jabber.extensions.keepalive;

import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.provider.*;
import org.xmlpull.v1.*;


/**
 * The KeepAliveEventProvider parses ping iq packets.
 *
 * @author Damian Minkov
 */
public class KeepAliveEventProvider
    extends IQProvider<KeepAliveEvent>
{
    /**
     * Creates a new KeepAliveEventProvider.
     * ProviderManager requires that every ExtensionElementProvider has a public,
     * no-argument constructor
     */
    public KeepAliveEventProvider()
    {}

    /**
     * Parses a ping iq packet .
     *
     * @param parser an XML parser.
     * @return a new IQ instance.
     * @throws Exception if an error occurs parsing the XML.
     */
    @Override
    public KeepAliveEvent parse(XmlPullParser parser, int depth)
        throws Exception
    {
        // No need to use the constructor with arguments. IQ will already
        // have filled out all relevant fields ('from', 'to', 'id').
        return new KeepAliveEvent();
    }
}
