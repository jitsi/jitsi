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
package net.java.sip.communicator.impl.protocol.jabber.extensions.condesc;

import net.java.sip.communicator.impl.protocol.jabber.extensions.AbstractPacketExtension;

/**
 * A <tt>ExtensionElement</tt> that represents a "callid" element within the
 * <tt>ConferenceDescriptionPacketExtension.NAMESPACE</tt> namespace.
 *
 * @author Boris Grozev
 */
public class CallIdExtension
    extends AbstractPacketExtension
{
    /**
     * The namespace for the XML element.
     */
    public static final String NAMESPACE
        = ConferenceDescriptionExtension.NAMESPACE;

    /**
     * The name of the "callid" element.
     */
    public static final String ELEMENT_NAME = "callid";

    /**
     * Creates a new instance setting the text to <tt>callid</tt>.
     *
     * @param callid
     */
    public CallIdExtension(String callid)
    {
        this();

        setText(callid);
    }

    /**
     * Creates a new instance.
     */
    public CallIdExtension()
    {
        super(NAMESPACE, ELEMENT_NAME);
    }
}
