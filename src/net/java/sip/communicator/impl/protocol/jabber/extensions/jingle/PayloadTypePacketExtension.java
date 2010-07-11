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
public class PayloadTypePacketExtension implements PacketExtension
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
     * contain one channel.
     */
    private int channels = -1;

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
     * Returns the name of the <tt>payload-type</tt> element.
     *
     * @return the name of the <tt>payload-type</tt> element.
     */
    public String getElementName()
    {
        return ELEMENT_NAME;
    }

    /**
     * Returns the namespace for the <tt>payload-type</tt> element.
     *
     * @return the namespace for the <tt>payload-type</tt> element.
     */
    public String getNamespace()
    {
        return NAMESPACE;
    }

    /**
     * Returns the XML representation of the <tt>payload-type</tt> element
     * including all child elements.
     *
     * @return this packet extension as an XML <tt>String</tt>.
     */
    public String toXML()
    {
        StringBuilder bldr = new StringBuilder(
            "<" + getElementName() + " ");

        //channels
        if(getChannels() > -1)
            bldr.append(CHANNELS_ARG_NAME + "='"+ getChannels() +"' ");

        //clockrate
        if(getClockrate() > -1)
            bldr.append(CLOCKRATE_ARG_NAME + "='"+ getClockrate() +"' ");

        //id
        bldr.append(ID_ARG_NAME + "=" + getID() + "' ");

        //maxptime
        if (getMaxptime() != -1)
            bldr.append(MAXPTIME_ARG_NAME + "=" + getMaxptime() + "' ");

        //payload name
        bldr.append(NAME_ARG_NAME + "=" + getName() + "' ");

        //ptime
        if (getPtime() != -1)
            bldr.append(PTIME_ARG_NAME + "=" + getPtime() + "' ");


        if (parameters.size() == 0)
        {
            bldr.append("/>");
        }
        else
        {
            bldr.append(">");

            for (PacketExtension parameter : parameters)
            {
                bldr.append(parameter.toXML());
            }

            bldr.append("</" + getElementName() + ">");
        }

        return bldr.toString();
    }

    /**
     * Sets the number of channels in this payload type. If omitted, it will be
     * assumed to contain one channel.
     *
     * @param channels the number of channels in this payload type.
     */
    public void setChannels(int channels)
    {
        this.channels = channels;
    }

    /**
     * Returns the number of channels in this payload type.
     *
     * @return the number of channels in this payload type.
     */
    public int getChannels()
    {
        return channels;
    }

    /**
     * Specifies the sampling frequency in Hertz used by this encoding.
     *
     * @param clockrate the sampling frequency in Hertz used by this encoding.
     */
    public void setClockrate(int clockrate)
    {
        this.clockrate = clockrate;
    }

    /**
     * Returns the sampling frequency in Hertz used by this encoding.
     *
     * @return the sampling frequency in Hertz used by this encoding.
     */
    public int getClockrate()
    {
        return clockrate;
    }

    /**
     * Specifies the payload identifier for this encoding.
     *
     * @param id the payload type id
     */
    public void setId(int id)
    {
        this.id = id;
    }

    /**
     * Returns the payload identifier for this encoding (as specified by RFC
     * 3551 or a dynamic one).
     *
     * @return the payload identifier for this encoding (as specified by RFC
     * 3551 or a dynamic one).
     */
    public int getID()
    {
        return id;
    }

    /**
     * Sets the maximum packet time as specified in RFC 4566.
     *
     * @param maxptime the maximum packet time as specified in RFC 4566
     */
    public void setMaxptime(int maxptime)
    {
        this.maxptime = maxptime;
    }

    /**
     * Returns maximum packet time as specified in RFC 4566.
     *
     * @return maximum packet time as specified in RFC 4566
     */
    public int getMaxptime()
    {
        return maxptime;
    }

    /**
     * Sets the packet time as specified in RFC 4566.
     *
     * @param ptime the packet time as specified in RFC 4566
     */
    public void setPtime(int ptime)
    {
        this.ptime = ptime;
    }

    /**
     * Returns packet time as specified in RFC 4566.
     *
     * @return packet time as specified in RFC 4566
     */
    public int getPtime()
    {
        return ptime;
    }

    /**
     * Sets the name of the encoding, or as per the XEP: the appropriate subtype
     * of the MIME type. Setting this field is RECOMMENDED for static payload
     * types, REQUIRED for dynamic payload types.
     *
     * @param name the name of this encoding.
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * Returns the name of the encoding, or as per the XEP: the appropriate
     * subtype of the MIME type. Setting this field is RECOMMENDED for static
     * payload types, REQUIRED for dynamic payload types.
     *
     * @return the name of the encoding, or as per the XEP: the appropriate
     * subtype of the MIME type. Setting this field is RECOMMENDED for static
     * payload types, REQUIRED for dynamic payload types.
     */
    public String getName()
    {
        return name;
    }

    /**
     * Adds an SDP parameter to the list that we already have registered for this
     * payload type.
     *
     * @param parameter an SDP parameter for this encoding.
     */
    public void addParameter(PacketExtension parameter)
    {
        this.parameters.add(parameter);
    }

    /**
     * Returns a <b>reference</b> to the the list of parameters currently
     * registered for this payload type.
     *
     * @return a <b>reference</b> to the the list of parameters currently
     * registered for this payload type.
     */
    public List<PacketExtension> getParameters()
    {
        return parameters;
    }
}
