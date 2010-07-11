/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.jingle;

import java.util.*;

import net.java.sip.communicator.impl.protocol.jabber.extensions.*;

import org.jivesoftware.smack.packet.*;

/**
 * Represents the <tt>payload-type</tt> elements described in XEP-0167.
 *
 * @author Emil Ivov
 */
public class PayloadTypePacketExtension extends AbstractPacketExtension
{
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
     * Creates a new {@link PayloadTypePacketExtension} instance.
     */
    public PayloadTypePacketExtension()
    {
        super(null, ELEMENT_NAME);
    }

    /**
     * Sets the number of channels in this payload type. If omitted, it will be
     * assumed to contain one channel.
     *
     * @param channels the number of channels in this payload type.
     */
    public void setChannels(int channels)
    {
        super.setAttribute(CHANNELS_ARG_NAME, channels);
    }

    /**
     * Returns the number of channels in this payload type.
     *
     * @return the number of channels in this payload type.
     */
    public int getChannels()
    {
        return getAttributeAsInt(CHANNELS_ARG_NAME);
    }

    /**
     * Specifies the sampling frequency in Hertz used by this encoding.
     *
     * @param clockrate the sampling frequency in Hertz used by this encoding.
     */
    public void setClockrate(int clockrate)
    {
        super.setAttribute(CLOCKRATE_ARG_NAME, clockrate);
    }

    /**
     * Returns the sampling frequency in Hertz used by this encoding.
     *
     * @return the sampling frequency in Hertz used by this encoding.
     */
    public int getClockrate()
    {
        return getAttributeAsInt(CLOCKRATE_ARG_NAME);
    }

    /**
     * Specifies the payload identifier for this encoding.
     *
     * @param id the payload type id
     */
    public void setId(int id)
    {
        super.setAttribute(ID_ARG_NAME, id);
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
        return getAttributeAsInt(ID_ARG_NAME);
    }

    /**
     * Sets the maximum packet time as specified in RFC 4566.
     *
     * @param maxptime the maximum packet time as specified in RFC 4566
     */
    public void setMaxptime(int maxptime)
    {
        setAttribute(MAXPTIME_ARG_NAME, maxptime);
    }

    /**
     * Returns maximum packet time as specified in RFC 4566.
     *
     * @return maximum packet time as specified in RFC 4566
     */
    public int getMaxptime()
    {
        return getAttributeAsInt(MAXPTIME_ARG_NAME);
    }

    /**
     * Sets the packet time as specified in RFC 4566.
     *
     * @param ptime the packet time as specified in RFC 4566
     */
    public void setPtime(int ptime)
    {
        super.setAttribute(PTIME_ARG_NAME, ptime);
    }

    /**
     * Returns packet time as specified in RFC 4566.
     *
     * @return packet time as specified in RFC 4566
     */
    public int getPtime()
    {
        return getAttributeAsInt(PTIME_ARG_NAME);
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
        setAttribute(NAME_ARG_NAME, name);
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
        return getAttributeAsString(NAME_ARG_NAME);
    }

    /**
     * Adds an SDP parameter to the list that we already have registered for this
     * payload type.
     *
     * @param parameter an SDP parameter for this encoding.
     */
    public void addParameter(ParameterPacketExtension parameter)
    {
        //parameters are the only extensions we can have so let's use
        //super's list.
        super.addChildExtension(parameter);
    }

    /**
     * Returns a <b>reference</b> to the the list of parameters currently
     * registered for this payload type.
     *
     * @return a <b>reference</b> to the the list of parameters currently
     * registered for this payload type.
     */
    @SuppressWarnings("unchecked") // nothing we could do here.
    public List<ParameterPacketExtension> getParameters()
    {

        return (List<ParameterPacketExtension>)super.getChildExtensions();
    }
}
