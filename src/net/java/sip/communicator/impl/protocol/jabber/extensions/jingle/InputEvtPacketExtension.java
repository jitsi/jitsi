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
 * Represents the content <tt>inputevt</tt> element that may be find in
 * <tt>content</tt> part of a Jingle media negociation.
 *
 * @author Sebastien Vincent
 */
public class InputEvtPacketExtension extends AbstractPacketExtension
{
    /**
     * Name of the XML element representing the extension.
     */
    public final static String ELEMENT_NAME = "inputevt";

    /**
     * Namespace..
     */
    public final static String NAMESPACE =
        "http://jitsi.org/protocol/inputevt";

    /**
     * Constructs a new <tt>inputevt</tt> extension.
     */
    public InputEvtPacketExtension()
    {
        super(NAMESPACE, ELEMENT_NAME);
    }
}
