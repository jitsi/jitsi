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

import java.util.*;

import net.java.sip.communicator.impl.protocol.jabber.extensions.*;

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
    public static final String CHANNELS_ATTR_NAME = "channels";

    /**
     * The name of the <tt>clockrate</tt> SDP argument.
     */
    public static final String CLOCKRATE_ATTR_NAME = "clockrate";

    /**
     * The name of the payload <tt>id</tt> SDP argument.
     */
    public static final String ID_ATTR_NAME = "id";

    /**
     * The name of the <tt>maxptime</tt> SDP argument.
     */
    public static final String MAXPTIME_ATTR_NAME = "maxptime";

    /**
     * The name of the <tt>name</tt> SDP argument.
     */
    public static final String NAME_ATTR_NAME = "name";

    /**
     * The name of the <tt>ptime</tt> SDP argument.
     */
    public static final String PTIME_ATTR_NAME = "ptime";

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
        super.setAttribute(CHANNELS_ATTR_NAME, channels);
    }

    /**
     * Returns the number of channels in this payload type.
     *
     * @return the number of channels in this payload type.
     */
    public int getChannels()
    {
        /*
         * XEP-0167: Jingle RTP Sessions says: if omitted, it MUST be assumed
         * to contain one channel.
         */
        return getAttributeAsInt(CHANNELS_ATTR_NAME, 1);
    }

    /**
     * Specifies the sampling frequency in Hertz used by this encoding.
     *
     * @param clockrate the sampling frequency in Hertz used by this encoding.
     */
    public void setClockrate(int clockrate)
    {
        super.setAttribute(CLOCKRATE_ATTR_NAME, clockrate);
    }

    /**
     * Returns the sampling frequency in Hertz used by this encoding.
     *
     * @return the sampling frequency in Hertz used by this encoding.
     */
    public int getClockrate()
    {
        return getAttributeAsInt(CLOCKRATE_ATTR_NAME);
    }

    /**
     * Specifies the payload identifier for this encoding.
     *
     * @param id the payload type id
     */
    public void setId(int id)
    {
        super.setAttribute(ID_ATTR_NAME, id);
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
        return getAttributeAsInt(ID_ATTR_NAME);
    }

    /**
     * Sets the maximum packet time as specified in RFC 4566.
     *
     * @param maxptime the maximum packet time as specified in RFC 4566
     */
    public void setMaxptime(int maxptime)
    {
        setAttribute(MAXPTIME_ATTR_NAME, maxptime);
    }

    /**
     * Returns maximum packet time as specified in RFC 4566.
     *
     * @return maximum packet time as specified in RFC 4566
     */
    public int getMaxptime()
    {
        return getAttributeAsInt(MAXPTIME_ATTR_NAME);
    }

    /**
     * Sets the packet time as specified in RFC 4566.
     *
     * @param ptime the packet time as specified in RFC 4566
     */
    public void setPtime(int ptime)
    {
        super.setAttribute(PTIME_ATTR_NAME, ptime);
    }

    /**
     * Returns packet time as specified in RFC 4566.
     *
     * @return packet time as specified in RFC 4566
     */
    public int getPtime()
    {
        return getAttributeAsInt(PTIME_ATTR_NAME);
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
        setAttribute(NAME_ATTR_NAME, name);
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
        return getAttributeAsString(NAME_ATTR_NAME);
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
        addChildExtension(parameter);
    }

    /**
     * Returns a <b>reference</b> to the the list of parameters currently
     * registered for this payload type.
     *
     * @return a <b>reference</b> to the the list of parameters currently
     * registered for this payload type.
     */
    public List<ParameterPacketExtension> getParameters()
    {
        return getChildExtensionsOfType(ParameterPacketExtension.class);
    }

    /**
     * Adds an RTCP feedback type to the list that we already have registered
     * for this payload type.
     *
     * @param rtcpFbPacketExtension RTCP feedback type for this encoding.
     */
    public void addRtcpFeedbackType(RtcpFbPacketExtension rtcpFbPacketExtension)
    {
        addChildExtension(rtcpFbPacketExtension);
    }

    /**
     * Returns the list of RTCP feedback types currently registered for this
     * payload type.
     *
     * @return the list of RTCP feedback types currently registered for this
     *         payload type.
     */
    public List<RtcpFbPacketExtension> getRtcpFeedbackTypeList()
    {
        return getChildExtensionsOfType(RtcpFbPacketExtension.class);
    }
}
