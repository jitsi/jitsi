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
 * Represents the <tt>payload-type</tt> elements described in XEP-0167.
 *
 * @author Emil Ivov
 */
public class PayloadPacketExtension
{
    /**
     * Payload types do not live in a namespace of their own so we have
     * <tt>null</tt> here.
     */
    public static final String NAMESPACE = null;

    /**
     * The name of the "payload-type" element.
     */
    public static final String ELEMENT_NAME = "payload-type";

    /**
     * The name of the <tt>channels</tt> <tt>payload-type</tt> argument.
     */
    public static final String CHANNELS_ARG_NAME = "channels";

    /**
     * The name of the <tt>clockrate</tt> SDP argument.
     */
    public static final String CLOCKRATE_ARG_NAME = "clockrate";

    /**
     * The name of the payload <tt>id</tt> SDP argument.
     */
    public static final String ID_ARG_NAME = "id";

    /**
     * The name of the <tt>maxptime</tt> SDP argument.
     */
    public static final String MAXPTIME_ARG_NAME = "maxptime";

    /**
     * The name of the <tt>name</tt> SDP argument.
     */
    public static final String NAME_ARG_NAME = "name";

    /**
     * The name of the <tt>ptime</tt> SDP argument.
     */
    public static final String PTIME_ARG_NAME = "ptime";

    /**
     * Number of channels in this payload. If omitted, it MUST be assumed to
     * contain one channel which is why we set a default value of 1.
     */
    private int channels = 1;

    /**
     * The sampling frequency in Hertz used by this encoding.
     */
    private int clockrate = -1;

    /**
     * The payload identifier for this encoding.
     */
    private int id = -1;

    /**
     * The maximum packet time as specified in RFC 4566
     */
    private int maxptime = -1;

    /**
     * The name of the encoding, or as per the XEP. The appropriate subtype of
     * the MIME type. Setting this field is RECOMMENDED for static payload
     * types, REQUIRED for dynamic payload types.
     */
    private String name;

    /**
     * The packet time as specified in RFC 4566
     */
    private int ptime = -1;

    /**
     * An optional list of format parameters (like the one we get with the
     * fmtp: SDP param).
     */
    private List<PacketExtension> parameters = new ArrayList<PacketExtension>();

    /**
     * Returns the XML representation of the <tt>payload-type</tt> element
     * including all child elements.
     *
     * @return this packet extension as an XML <tt>String</tt>.
     */
    public String toXML()
    {
        StringBuilder bldr = new StringBuilder();

        return bldr.toString();
    }
}
