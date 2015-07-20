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
 * Represents the conference information.
 *
 * @author Sebastien Vincent
 */
public class CoinPacketExtension
    extends AbstractPacketExtension
{
    /**
     * Name of the XML element representing the extension.
     */
    public final static String ELEMENT_NAME = "conference-info";

    /**
     * Namespace.
     */
    public final static String NAMESPACE = "";

    /**
     * IsFocus attribute name.
     */
    public final static String ISFOCUS_ATTR_NAME = "isfocus";

    /**
     * Constructs a new <tt>coin</tt> extension.
     *
     */
    public CoinPacketExtension()
    {
        super(NAMESPACE, ELEMENT_NAME);
    }

    /**
     * Constructs a new <tt>coin</tt> extension.
     *
     * @param isFocus <tt>true</tt> if the peer is a conference focus;
     * otherwise, <tt>false</tt>
     */
    public CoinPacketExtension(boolean isFocus)
    {
        super(NAMESPACE, ELEMENT_NAME);
        setAttribute(ISFOCUS_ATTR_NAME, isFocus);
    }
}
