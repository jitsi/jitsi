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
package net.java.sip.communicator.impl.protocol.jabber.extensions.jitsimeet;

import net.java.sip.communicator.impl.protocol.jabber.extensions.*;

/**
 * Jitsi Meet specifics bundle packet extension.
 *
 * @author Pawel Domas
 */
public class BundlePacketExtension
    extends AbstractPacketExtension
{
    /**
     * The XML element name of {@link BundlePacketExtension}.
     */
    public static final String ELEMENT_NAME = "bundle";

    /**
     * The XML element namespace of {@link BundlePacketExtension}.
     */
    public static final String NAMESPACE = "http://estos.de/ns/bundle";

    /**
     * Creates an {@link BundlePacketExtension} instance for the specified
     * <tt>namespace</tt> and <tt>elementName</tt>.
     *
     */
    public BundlePacketExtension()
    {
        super(NAMESPACE, ELEMENT_NAME);
    }
}
