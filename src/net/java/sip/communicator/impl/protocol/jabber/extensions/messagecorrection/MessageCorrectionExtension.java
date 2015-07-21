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
package net.java.sip.communicator.impl.protocol.jabber.extensions.messagecorrection;

import org.jivesoftware.smack.packet.*;

/**
 * Represents an XMPP Message correction extension, as defined in XEP-308.
 *
 * @author Ivan Vergiliev
 */
public class MessageCorrectionExtension
    implements PacketExtension
{
    /**
     * The XMPP namespace that this extension belongs to.
     */
    public static final String NAMESPACE = "urn:xmpp:message-correct:0";

    /**
     * The XMPP namespace that Swift IM use to send message corrections.
     * Temporary until they start using the standard one.
     */
    public static final String SWIFT_NAMESPACE =
            "http://swift.im/protocol/replace";

    /**
     * The XML element name of this extension.
     */
    public static final String ELEMENT_NAME = "replace";

    /**
     * Name of the attribute that specifies the ID of the message
     * being corrected.
     */
    public static final String ID_ATTRIBUTE_NAME = "id";

    /**
     * The ID of the message being corrected.
     */
    private String correctedMessageUID;

    /**
     * Creates a new message correction extension that corrects the
     * message specified by the passed ID.
     *
     * @param correctedMessageUID The ID of the message being corrected.
     */
    public MessageCorrectionExtension(String correctedMessageUID)
    {
        this.correctedMessageUID = correctedMessageUID;
    }

    /**
     * Returns the XML element name of this extension.
     *
     * @return The XML element name of this extension.
     */
    public String getElementName()
    {
        return ELEMENT_NAME;
    }

    /**
     * Returns the XML namespace this extension belongs to.
     *
     * @return The XML namespace this extension belongs to.
     */
    public String getNamespace()
    {
        return NAMESPACE;
    }

    /**
     * Construct an XML element representing this extension;
     * has the form '<replace id="..." xmlns="...">'.
     *
     * @return An XML representation of this extension.
     */
    public String toXML()
    {
        return "<" + ELEMENT_NAME + " id='" + correctedMessageUID
                + "' xmlns='" + NAMESPACE + "' />";
    }

    /**
     * Returns the correctedMessageUID The UID of the message being corrected.
     *
     * @return the correctedMessageUID The UID of the message being corrected.
     */
    public String getCorrectedMessageUID()
    {
        return correctedMessageUID;
    }

    /**
     * Sets the UID of the message being corrected.
     *
     * @param correctedMessageUID The UID of the message being corrected.
     */
    public void setCorrectedMessageUID(String correctedMessageUID)
    {
        this.correctedMessageUID = correctedMessageUID;
    }
}
