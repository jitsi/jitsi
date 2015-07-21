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
 * Packet extension that holds RTCP feedback types of the
 * {@link PayloadTypePacketExtension}. Defined in XEP-0293.
 *
 * @author Pawel Domas
 */
public class RtcpFbPacketExtension
    extends AbstractPacketExtension
{
    /**
     * The name space for RTP feedback elements.
     */
    public static final String NAMESPACE = "urn:xmpp:jingle:apps:rtp:rtcp-fb:0";

    /**
     * The name of the RTCP feedback element.
     */
    public static final String ELEMENT_NAME = "rtcp-fb";

    /**
     * The name the attribute that holds the feedback type.
     */
    public static final String TYPE_ATTR_NAME = "type";

    /**
     *  The name the attribute that holds the feedback subtype.
     */
    public static final String SUBTYPE_ATTR_NAME = "subtype";

    /**
     * Creates new empty instance of <tt>RtcpFbPacketExtension</tt>.
     */
    public RtcpFbPacketExtension()
    {
        super(NAMESPACE, ELEMENT_NAME);
    }

    /**
     * Sets RTCP feedback type attribute.
     * @param feedbackType the RTCP feedback type to set.
     */
    public void setFeedbackType(String feedbackType)
    {
        setAttribute(TYPE_ATTR_NAME, feedbackType);
    }

    /**
     * Returns RTCP feedback type attribute value if already set
     * or <tt>null</tt> otherwise.
     *
     * @return RTCP feedback type attribute if already set or <tt>null</tt>
     *         otherwise.
     */
    public String getFeedbackType()
    {
        return getAttributeAsString(TYPE_ATTR_NAME);
    }

    /**
     * Sets RTCP feedback subtype attribute.
     * @param feedbackSubType the RTCP feedback subtype to set.
     */
    public void setFeedbackSubtype(String feedbackSubType)
    {
        setAttribute(SUBTYPE_ATTR_NAME, feedbackSubType);
    }

    /**
     * Returns RTCP feedback subtype attribute value if already set
     * or <tt>null</tt> otherwise.
     *
     * @return RTCP feedback subtype attribute if already set or <tt>null</tt>
     *         otherwise.
     */
    public String getFeedbackSubtype()
    {
        return getAttributeAsString(SUBTYPE_ATTR_NAME);
    }
}
