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

import org.jivesoftware.smack.packet.*;

/**
 * The <tt>JingleInfoQueryIQ</tt> is used to discover STUN and relay server via
 * the Google's Jingle Server Discovery extension.
 *
 * @author Sebastien Vincent
 */
public class JingleInfoQueryIQ
    extends IQ
{
    /**
     * The namespace.
     */
    public static final String NAMESPACE = "google:jingleinfo";

    /**
     * The element name.
     */
    public static final String ELEMENT_NAME = "query";

    /**
     * Returns the sub-element XML section of the IQ packet, or null if
     * there isn't one. Packet extensions must be included, if any are defined.
     *
     * @return the child element section of the IQ XML.
     */
    @Override
    public String getChildElementXML()
    {
        StringBuilder bld = new StringBuilder();

        bld.append("<").append(ELEMENT_NAME).append(" xmlns='").
             append(NAMESPACE).append("'");

        if(getExtensions().size() == 0)
        {
            bld.append("/>");
        }
        else
        {
            bld.append(">");

            for(PacketExtension pe : getExtensions())
            {
                bld.append(pe.toXML());
            }

            bld.append("</").append(ELEMENT_NAME).append(">");
        }
        return bld.toString();
    }
}
