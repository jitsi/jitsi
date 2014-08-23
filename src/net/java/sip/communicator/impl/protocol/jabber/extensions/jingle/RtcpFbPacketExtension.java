/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
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
