/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.jingle;

import java.util.*;

import org.jivesoftware.smack.packet.*;

/**
 * Represents the content <tt>description</tt> elements described in XEP-0167.
 *
 * @author Emil Ivov
 */
public class RtpDescriptionPacketExtension
    implements PacketExtension
{
    /**
     * The name space for RTP description elements.
     */
    public static final String NAMESPACE = "urn:xmpp:jingle:apps:rtp:1";

    /**
     * The name of the "description" element.
     */
    public static final String ELEMENT_NAME = "description";

    /**
     * The name of the <tt>media</tt> description argument.
     */
    public static final String MEDIA_ARG_NAME = "media";

    /**
     * The name of the <tt>ssrc</tt> description argument.
     */
    public static final String SSRC_ARG_NAME = "ssrc";

    /**
     * An argument that specifies the media type, such as "audio" or "video".
     */
    private String media;

    /**
     * An attribute that specifies the 32-bit synchronization source for this
     * media stream, as defined in RFC 3550
     */
    private String ssrc;

    /**
     * The list of payload types that this description element contains.
     */
    private final List<PayloadTypePacketExtension> payloadTypes
                                    = new ArrayList<PayloadTypePacketExtension>();

    /**
     * An optional encryption element that contains encryption parameters for
     * this session.
     */
    private PacketExtension encryptionElement;

    /**
     * An optional bandwidth element that specifies the allowable or preferred
     * bandwidth for use by this application type.
     */
    private PacketExtension bandwidthElement;

    /**
     * Specifies the media type for the stream that this description element
     * represents, such as "audio" or "video".
     *
     * @param media the media type for the stream that this element represents
     * such as "audio" or "video".
     */
    public void setMedia(String media)
    {
        this.media = media;
    }

    /**
     * Returns the media type for the stream that this description element
     * represents, such as "audio" or "video".
     *
     * @return  the media type for the stream that this description element
     * represents, such as "audio" or "video".
     */
    public String getMedia()
    {
        return media;
    }

    /**
     * Sets the synchronization source ID (SSRC as per RFC 3550) that the stream
     * represented by this description element will be using.
     *
     * @param ssrc the SSRC ID that the RTP stream represented here will be
     * using.
     */
    public void setSsrc(String ssrc)
    {
        this.ssrc = ssrc;
    }

    /**
     * Returns the synchronization source ID (SSRC as per RFC 3550) that the
     * stream represented by this description element will be using.
     *
     * @return the synchronization source ID (SSRC as per RFC 3550) that the
     * stream represented by this description element will be using.
     */
    public String getSsrc()
    {
        return ssrc;
    }

    /**
     * Returns the name of the <tt>description</tt> element.
     *
     * @return the name of the <tt>description</tt> element.
     */
    public String getElementName()
    {
        return ELEMENT_NAME;
    }

    /**
     * Returns the namespace for the <tt>description</tt> element.
     *
     * @return the namespace for the <tt>description</tt> element.
     */
    public String getNamespace()
    {
        return NAMESPACE;
    }

    /**
     * Adds a new payload type to this description element.
     *
     * @param payloadType the new payload to add.
     */
    public void addPayloadType(PayloadTypePacketExtension payloadType)
    {
        this.payloadTypes.add(payloadType);
    }

    /**
     * Returns a <b>reference</b> to the list of payload types that we have
     * registered with this description so far.
     *
     * @return a <b>reference</b> to the list of payload types that we have
     * registered with this description so far.
     */
    public List<PayloadTypePacketExtension> getPayloadTypes()
    {
        return payloadTypes;
    }

    /**
     * Returns the XML representation of this <tt>description</tt> packet
     * extension including all child elements.
     *
     * @return this packet extension as an XML <tt>String</tt>.
     */
    public String toXML()
    {
        StringBuilder bldr = new StringBuilder(
            "<" + ELEMENT_NAME + " xmlns='" + NAMESPACE + "' "
                + MEDIA_ARG_NAME + "='" + getMedia() + "'");

        if(getSsrc() != null)
            bldr.append(SSRC_ARG_NAME + "='" + getSsrc() +"'");

        bldr.append(">");

        //payload types
        for(PayloadTypePacketExtension payloadType : payloadTypes)
        {
            bldr.append(payloadType.toXML());
        }

        //encryption element
        if (encryptionElement != null)
            bldr.append(encryptionElement);

        //bandwidth element
        if (bandwidthElement != null)
            bldr.append(bandwidthElement);

        bldr.append("</" + ELEMENT_NAME + ">");

        return bldr.toString();
    }

}
