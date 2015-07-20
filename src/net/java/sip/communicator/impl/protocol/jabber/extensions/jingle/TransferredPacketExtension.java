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
package net.java.sip.communicator.impl.protocol.jabber.extensions.jingle;

import net.java.sip.communicator.impl.protocol.jabber.extensions.*;

/**
 * Implements <tt>AbstractPacketExtension</tt> for the "transferred" element
 * defined by XEP-0251: Jingle Session Transfer.
 *
 * @author Lyubomir Marinov
 */
public class TransferredPacketExtension
    extends AbstractPacketExtension
{
    /**
     * The name of the "transfer" element.
     */
    public static final String ELEMENT_NAME = "transferred";

    /**
     * The namespace of the "transfer" element.
     */
    public static final String NAMESPACE = "urn:xmpp:jingle:transfer:0";

    /**
     * Initializes a new <tt>TransferredPacketExtension</tt> instance.
     */
    public TransferredPacketExtension()
    {
        super(NAMESPACE, ELEMENT_NAME);
    }
}
