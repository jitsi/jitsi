/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.colibri;

import java.util.*;

import net.java.sip.communicator.impl.protocol.jabber.extensions.jingle.*;

import org.jitsi.service.neomedia.*;
import org.jivesoftware.smack.packet.*;

/**
 * Implements the Jitsi Videobridge <tt>conference</tt> IQ within the
 * COnferencing with LIghtweight BRIdging.
 *
 * @author Lyubomir Marinov
 * @author Boris Grozev
 */
public class ColibriConferenceIQ
    extends IQ
{
    /**
     * The XML element name of the Jitsi Videobridge <tt>conference</tt> IQ.
     */
    public static final String ELEMENT_NAME = "conference";

    /**
     * The XML name of the <tt>id</tt> attribute of the Jitsi Videobridge
     * <tt>conference</tt> IQ which represents the value of the <tt>id</tt>
     * property of <tt>ColibriConferenceIQ</tt>.
     */
    public static final String ID_ATTR_NAME = "id";

    /**
     * The XML COnferencing with LIghtweight BRIdging namespace of the Jitsi
     * Videobridge <tt>conference</tt> IQ.
     */
    public static final String NAMESPACE
        = "http://jitsi.org/protocol/colibri";

    /**
     * An array of <tt>int</tt>s which represents the lack of any (RTP) SSRCs
     * seen/received on a <tt>Channel</tt>. Explicitly defined to reduce
     * unnecessary allocations.
     */
    public static final int[] NO_SSRCS = new int[0];

    /**
     * The list of {@link Content}s included into this <tt>conference</tt> IQ.
     */
    private final List<Content> contents = new LinkedList<Content>();

    /**
     * The ID of the conference represented by this IQ.
     */
    private String id;

    /**
     * Media recording.
     */
    public Recording recording = null;

    private RTCPTerminationStrategy rtcpTerminationStrategy = null;

    /** Initializes a new <tt>ColibriConferenceIQ</tt> instance. */
    public ColibriConferenceIQ()
    {
    }

    /**
     * Initializes a new {@link Content} instance with a specific name and adds
     * it to the list of <tt>Content</tt> instances included into this
     * <tt>conference</tt> IQ.
     *
     * @param contentName the name which which the new <tt>Content</tt> instance
     * is to be initialized
     * @return <tt>true</tt> if the list of <tt>Content</tt> instances included
     * into this <tt>conference</tt> IQ has been modified as a result of the
     * method call; otherwise, <tt>false</tt>
     */
    public boolean addContent(String contentName)
    {
        return addContent(new Content(contentName));
    }

    /**
     * Adds a specific {@link Content} instance to the list of <tt>Content</tt>
     * instances included into this <tt>conference</tt> IQ.
     *
     * @param content the <tt>Content</tt> instance to be added to this list of
     * <tt>Content</tt> instances included into this <tt>conference</tt> IQ
     * @return <tt>true</tt> if the list of <tt>Content</tt> instances included
     * into this <tt>conference</tt> IQ has been modified as a result of the
     * method call; otherwise, <tt>false</tt>
     * @throws NullPointerException if the specified <tt>content</tt> is
     * <tt>null</tt>
     */
    public boolean addContent(Content content)
    {
        if (content == null)
            throw new NullPointerException("content");

        return contents.contains(content) ? false : contents.add(content);
    }

    /**
     * Returns an XML <tt>String</tt> representation of this <tt>IQ</tt>.
     *
     * @return an XML <tt>String</tt> representation of this <tt>IQ</tt>
     */
    @Override
    public String getChildElementXML()
    {
        StringBuilder xml = new StringBuilder();

        xml.append('<').append(ELEMENT_NAME);
        xml.append(" xmlns='").append(NAMESPACE).append('\'');

        String id = getID();

        if (id != null)
            xml.append(' ').append(ID_ATTR_NAME).append("='").append(id)
                    .append('\'');

        List<Content> contents = getContents();

        int childrenCount = contents.size();
        if (recording != null)
            childrenCount++;

        if (childrenCount == 0)
        {
            xml.append(" />");
        }
        else
        {
            xml.append('>');
            for (Content content : contents)
                content.toXML(xml);
            if (recording != null)
                recording.toXML(xml);

            if (rtcpTerminationStrategy != null)
                rtcpTerminationStrategy.toXML(xml);

            xml.append("</").append(ELEMENT_NAME).append('>');
        }
        return xml.toString();
    }

    /**
     * Returns a <tt>Content</tt> from the list of <tt>Content</tt>s of this
     * <tt>conference</tt> IQ which has a specific name. If no such
     * <tt>Content</tt> exists, returns <tt>null</tt>.
     *
     * @param contentName the name of the <tt>Content</tt> to be returned
     * @return a <tt>Content</tt> from the list of <tt>Content</tt>s of this
     * <tt>conference</tt> IQ which has the specified <tt>contentName</tt> if
     * such a <tt>Content</tt> exists; otherwise, <tt>null</tt>
     */
    public Content getContent(String contentName)
    {
        for (Content content : getContents())
            if (contentName.equals(content.getName()))
                return content;
        return null;
    }

    /**
     * Returns a list of the <tt>Content</tt>s included into this
     * <tt>conference</tt> IQ.
     *
     * @return an unmodifiable <tt>List</tt> of the <tt>Content</tt>s included
     * into this <tt>conference</tt> IQ
     */
    public List<Content> getContents()
    {
        return Collections.unmodifiableList(contents);
    }

    /**
     * Gets the ID of the conference represented by this IQ.
     *
     * @return the ID of the conference represented by this IQ
     */
    public String getID()
    {
        return id;
    }

    /**
     * Gets the value of the recording field.
     * @return the value of the recording field.
     */
    public Recording getRecording()
    {
        return recording;
    }

    /**
     * Sets the recording field.
     * @param recording the value to set.
     */
    public void setRecording(Recording recording)
    {
        this.recording = recording;
    }

    /**
     * Returns a <tt>Content</tt> from the list of <tt>Content</tt>s of this
     * <tt>conference</tt> IQ which has a specific name. If no such
     * <tt>Content</tt> exists at the time of the invocation of the method,
     * initializes a new <tt>Content</tt> instance with the specified
     * <tt>contentName</tt> and includes it into this <tt>conference</tt> IQ.
     *
     * @param contentName the name of the <tt>Content</tt> to be returned
     * @return a <tt>Content</tt> from the list of <tt>Content</tt>s of this
     * <tt>conference</tt> IQ which has the specified <tt>contentName</tt>
     */
    public Content getOrCreateContent(String contentName)
    {
        Content content = getContent(contentName);

        if (content == null)
        {
            content = new Content(contentName);
            addContent(content);
        }

        return content;
    }

    /**
     * Removes a specific {@link Content} instance from the list of
     * <tt>Content</tt> instances included into this <tt>conference</tt> IQ.
     *
     * @param content the <tt>Content</tt> instance to be removed from the list
     * of <tt>Content</tt> instances included into this <tt>conference</tt> IQ
     * @return <tt>true</tt> if the list of <tt>Content</tt> instances included
     * into this <tt>conference</tt> IQ has been modified as a result of the
     * method call; otherwise, <tt>false</tt>
     */
    public boolean removeContent(Content content)
    {
        return contents.remove(content);
    }

    /**
     * Sets the ID of the conference represented by this IQ.
     *
     * @param id the ID of the conference represented by this IQ
     */
    public void setID(String id)
    {
        this.id = id;
    }

    public RTCPTerminationStrategy getRTCPTerminationStrategy()
    {
        return rtcpTerminationStrategy;
    }

    public void setRTCPTerminationStrategy(RTCPTerminationStrategy rtcpTerminationStrategy)
    {
        this.rtcpTerminationStrategy = rtcpTerminationStrategy;
    }

    /**
     * Class contains common code for both <tt>Channel</tt> and
     * <tt>SctpConnection</tt> IQ classes.
     *
     * @author Pawel Domas
     */
    public static abstract class ChannelCommon
    {
        /**
         * The XML name of the <tt>endpoint</tt> attribute which specifies the
         * optional identifier of the endpoint of the conference participant
         * associated with a <tt>channel</tt>. The value of the
         * <tt>endpoint</tt> attribute is an opaque <tt>String</tt> from the
         * point of view of Jitsi Videobridge.
         */
        public static final String ENDPOINT_ATTR_NAME = "endpoint";

        /**
         * The XML name of the <tt>expire</tt> attribute of a <tt>channel</tt>
         * of a <tt>content</tt> of a <tt>conference</tt> IQ which represents
         * the value of the <tt>expire</tt> property of
         * <tt>ColibriConferenceIQ.Channel</tt>.
         */
        public static final String EXPIRE_ATTR_NAME = "expire";

        /**
         * The value of the <tt>expire</tt> property of
         * <tt>ColibriConferenceIQ.Channel</tt> which indicates that no actual
         * value has been specified for the property in question.
         */
        public static final int EXPIRE_NOT_SPECIFIED = -1;

        /**
         * The XML name of the <tt>initiator</tt> attribute of a
         * <tt>channel</tt> of a <tt>content</tt> of a <tt>conference</tt> IQ
         * which represents the value of the <tt>initiator</tt> property of
         * <tt>ColibriConferenceIQ.Channel</tt>.
         */
        public static final String INITIATOR_ATTR_NAME = "initiator";

        /**
         * The identifier of the endpoint of the conference participant
         * associated with this <tt>Channel</tt>.
         */
        private String endpoint;

        /**
         * The number of seconds of inactivity after which the <tt>channel</tt>
         * represented by this instance expires.
         */
        private int expire = EXPIRE_NOT_SPECIFIED;

        /**
         * The indicator which determines whether the conference focus is the
         * initiator/offerer (as opposed to the responder/answerer) of the media
         * negotiation associated with this instance.
         */
        private Boolean initiator;

        private IceUdpTransportPacketExtension transport;

        /**
         * XML element name.
         */
        private String elementName;

        /**
         * Initializes this class with given XML <tt>elementName</tt>.
         * @param elementName XML element name to be used for producing XML
         *                    representation of derived IQ class.
         */
        protected ChannelCommon(String elementName)
        {
            this.elementName = elementName;
        }

        /**
         * Gets the identifier of the endpoint of the conference participant
         * associated with this <tt>Channel</tt>.
         *
         * @return the identifier of the endpoint of the conference participant
         * associated with this <tt>Channel</tt>
         */
        public String getEndpoint()
        {
            return endpoint;
        }

        /**
         * Gets the number of seconds of inactivity after which the
         * <tt>channel</tt> represented by this instance expires.
         *
         * @return the number of seconds of inactivity after which the
         * <tt>channel</tt> represented by this instance expires
         */
        public int getExpire()
        {
            return expire;
        }

        public IceUdpTransportPacketExtension getTransport()
        {
            return transport;
        }

        /**
         * Gets the indicator which determines whether the conference focus is
         * the initiator/offerer (as opposed to the responder/answerer) of the
         * media negotiation associated with this instance.
         *
         * @return {@link Boolean#TRUE} if the conference focus is the
         * initiator/offerer of the media negotiation associated with this
         * instance, {@link Boolean#FALSE} if the conference focus is the
         * responder/answerer or <tt>null</tt> if the <tt>initiator</tt> state
         * is unspecified
         */
        public Boolean isInitiator()
        {
            return initiator;
        }

        /**
         * Sets the identifier of the endpoint of the conference participant
         * associated with this <tt>Channel</tt>.
         *
         * @param endpoint the identifier of the endpoint of the conference
         * participant associated with this <tt>Channel</tt>
         */
        public void setEndpoint(String endpoint)
        {
            this.endpoint = endpoint;
        }

        /**
         * Sets the number of seconds of inactivity after which the
         * <tt>channel</tt> represented by this instance expires.
         *
         * @param expire the number of seconds of activity after which the
         * <tt>channel</tt> represented by this instance expires
         * @throws IllegalArgumentException if the value of the specified
         * <tt>expire</tt> is other than {@link #EXPIRE_NOT_SPECIFIED} and
         * negative
         */
        public void setExpire(int expire)
        {
            if ((expire != EXPIRE_NOT_SPECIFIED) && (expire < 0))
                throw new IllegalArgumentException("expire");

            this.expire = expire;
        }

        /**
         * Sets the indicator which determines whether the conference focus is
         * the initiator/offerer (as opposed to the responder/answerer) of the
         * media negotiation associated with this instance.
         *
         * @param initiator {@link Boolean#TRUE} if the conference focus is the
         * initiator/offerer of the media negotiation associated with this
         * instance, {@link Boolean#FALSE} if the conference focus is the
         * responder/answerer or <tt>null</tt> if the <tt>initiator</tt> state
         * is to be unspecified
         */
        public void setInitiator(Boolean initiator)
        {
            this.initiator = initiator;
        }

        public void setTransport(IceUdpTransportPacketExtension transport)
        {
            this.transport = transport;
        }

        /**
         * Derived class implements this method in order to print additional
         * attributes to main XML element.
         * @param xml <the <tt>StringBuilder</tt> to which the XML
         *            <tt>String</tt> representation of this <tt>Channel</tt>
         *            is to be appended</tt>
         */
        protected abstract void printAttributes(StringBuilder xml);

        /**
         * Indicates whether there are some contents that should be printed as
         * child elements of this IQ. If <tt>true</tt> is returned
         * {@link #printContent(StringBuilder)} method will be called when
         * XML representation of this IQ is being constructed.
         * @return <tt>true</tt> if there are content to be printed as child
         *         elements of this IQ or <tt>false</tt> otherwise.
         */
        protected abstract boolean hasContent();

        /**
         * Implement in order to print content child elements of this IQ using
         * given <tt>StringBuilder</tt>. Called during construction of XML
         * representation if {@link #hasContent()} returns <tt>true</tt>.
         *
         * @param xml the <tt>StringBuilder</tt> to which the XML
         *        <tt>String</tt> representation of this <tt>Channel</tt>
         *        is to be appended</tt></tt>.
         */
        protected abstract void printContent(StringBuilder xml);

        /**
         * Appends the XML <tt>String</tt> representation of this
         * <tt>Channel</tt> to a specific <tt>StringBuilder</tt>.
         *
         * @param xml the <tt>StringBuilder</tt> to which the XML
         * <tt>String</tt> representation of this <tt>Channel</tt> is to be
         * appended
         */
        public void toXML(StringBuilder xml)
        {
            xml.append('<').append(elementName);

            // endpoint
            String endpoint = getEndpoint();

            if (endpoint != null)
            {
                xml.append(' ').append(ENDPOINT_ATTR_NAME).append("='")
                    .append(endpoint).append('\'');
            }

            // expire
            int expire = getExpire();

            if (expire >= 0)
            {
                xml.append(' ').append(EXPIRE_ATTR_NAME).append("='")
                    .append(expire).append('\'');
            }

            // initiator
            Boolean initiator = isInitiator();

            if (initiator != null)
            {
                xml.append(' ').append(INITIATOR_ATTR_NAME).append("='")
                    .append(initiator).append('\'');
            }

            // Print derived class attributes
            printAttributes(xml);

            IceUdpTransportPacketExtension transport = getTransport();
            boolean hasTransport = (transport != null);
            if (hasTransport || hasContent())
            {
                xml.append('>');
                if(hasContent())
                    printContent(xml);
                if (hasTransport)
                    xml.append(transport.toXML());
                xml.append("</").append(elementName).append('>');
            }
            else
            {
                xml.append(" />");
            }
        }
    }

    public static class RTCPTerminationStrategy
    {

        public static final String ELEMENT_NAME = "rtcp-termination-strategy";
        public static final String NAME_ATTR_NAME = "name";

        private String name;

        public void setName(String name)
        {
            this.name = name;
        }

        public String getName()
        {
            return name;
        }

        public void toXML(StringBuilder xml)
        {
            xml.append('<').append(ELEMENT_NAME);
            xml.append(' ').append(NAME_ATTR_NAME).append("='")
                    .append(name).append('\'');
            xml.append("/>");
        }
    }

    /**
     * Represents a <tt>channel</tt> included into a <tt>content</tt> of a Jitsi
     * Videobridge <tt>conference</tt> IQ.
     */
    public static class Channel
        extends ChannelCommon
    {
        /**
         * The name of the XML attribute of a <tt>channel</tt> which represents
         * its direction.
         */
        public static final String DIRECTION_ATTR_NAME = "direction";

        /**
         * The XML element name of a <tt>channel</tt> of a <tt>content</tt> of a
         * Jitsi Videobridge <tt>conference</tt> IQ.
         */
        public static final String ELEMENT_NAME = "channel";

        /**
         * The XML name of the <tt>host</tt> attribute of a <tt>channel</tt> of
         * a <tt>content</tt> of a <tt>conference</tt> IQ which represents the
         * value of the <tt>host</tt> property of
         * <tt>ColibriConferenceIQ.Channel</tt>.
         *
         * @deprecated The attribute is supported for the purposes of
         * compatibility with legacy versions of Jitsi and Jitsi Videobridge. 
         */
        @Deprecated
        public static final String HOST_ATTR_NAME = "host";

        /**
         * The XML name of the <tt>id</tt> attribute of a <tt>channel</tt> of a
         * <tt>content</tt> of a <tt>conference</tt> IQ which represents the
         * value of the <tt>id</tt> property of
         * <tt>ColibriConferenceIQ.Channel</tt>.
         */
        public static final String ID_ATTR_NAME = "id";

        /**
         * The XML name of the <tt>last-n</tt> attribute of a video
         * <tt>channel</tt> which specifies the maximum number of video RTP
         * streams to be sent from Jitsi Videobridge to the endpoint associated
         * with the video <tt>channel</tt>. The value of the <tt>last-n</tt>
         * attribute is a positive number.
         */
        public static final String LAST_N_ATTR_NAME = "last-n";

        /**
         * The XML name of the <tt>rtcpport</tt> attribute of a <tt>channel</tt>
         * of a <tt>content</tt> of a <tt>conference</tt> IQ which represents
         * the value of the <tt>rtcpPort</tt> property of
         * <tt>ColibriConferenceIQ.Channel</tt>.
         *
         * @deprecated The attribute is supported for the purposes of
         * compatibility with legacy versions of Jitsi and Jitsi Videobridge. 
         */
        @Deprecated
        public static final String RTCP_PORT_ATTR_NAME = "rtcpport";

        public static final String RTP_LEVEL_RELAY_TYPE_ATTR_NAME
            = "rtp-level-relay-type";

        /**
         * The XML name of the <tt>rtpport</tt> attribute of a <tt>channel</tt>
         * of a <tt>content</tt> of a <tt>conference</tt> IQ which represents
         * the value of the <tt>rtpPort</tt> property of
         * <tt>ColibriConferenceIQ.Channel</tt>.
         *
         * @deprecated The attribute is supported for the purposes of
         * compatibility with legacy versions of Jitsi and Jitsi Videobridge. 
         */
        @Deprecated
        public static final String RTP_PORT_ATTR_NAME = "rtpport";

        /**
         * The name of the XML element which is a child of the &lt;channel&gt;
         * element and which identifies/specifies an (RTP) SSRC which has been
         * seen/received on the respective <tt>Channel</tt>.
         */
        public static final String SSRC_ELEMENT_NAME = "ssrc";

        /**
         * The direction of the <tt>channel</tt> represented by this instance.
         */
        private MediaDirection direction;

        /**
         * The host of the <tt>channel</tt> represented by this instance.
         *
         * @deprecated The field is supported for the purposes of compatibility
         * with legacy versions of Jitsi and Jitsi Videobridge. 
         */
        @Deprecated
        private String host;

        /**
         * The ID of the <tt>channel</tt> represented by this instance.
         */
        private String id;

        /**
         * The maximum number of video RTP streams to be sent from Jitsi
         * Videobridge to the endpoint associated with this video
         * <tt>Channel</tt>.
         */
        private Integer lastN;

        /**
         * The <tt>payload-type</tt> elements defined by XEP-0167: Jingle RTP
         * Sessions associated with this <tt>channel</tt>.
         */
        private final List<PayloadTypePacketExtension> payloadTypes
            = new ArrayList<PayloadTypePacketExtension>();

        /**
         * The RTCP port of the <tt>channel</tt> represented by this instance.
         *
         * @deprecated The field is supported for the purposes of compatibility
         * with legacy versions of Jitsi and Jitsi Videobridge. 
         */
        @Deprecated
        private int rtcpPort;

        /**
         * The type of RTP-level relay (in the terms specified by RFC 3550
         * &quot;RTP: A Transport Protocol for Real-Time Applications&quot; in
         * section 2.3 &quot;Mixers and Translators&quot;) used for this
         * <tt>Channel</tt>.
         */
        private RTPLevelRelayType rtpLevelRelayType;

        /**
         * The RTP port of the <tt>channel</tt> represented by this instance.
         *
         * @deprecated The field is supported for the purposes of compatibility
         * with legacy versions of Jitsi and Jitsi Videobridge. 
         */
        @Deprecated
        private int rtpPort;

        /**
         * The <tt>SourcePacketExtension</tt>s of this channel.
         */
        private final List<SourcePacketExtension> sources
            = new LinkedList<SourcePacketExtension>();

        /**
         * The list of (RTP) SSRCs which have been seen/received on this
         * <tt>Channel</tt> by now. These may exclude SSRCs which are no longer
         * active. Set by the Jitsi Videobridge server, not its clients.
         */
        private int[] ssrcs = NO_SSRCS;

        /** Initializes a new <tt>Channel</tt> instance. */
        public Channel()
        {
            super(Channel.ELEMENT_NAME);
        }

        /**
         * Adds a <tt>payload-type</tt> element defined by XEP-0167: Jingle RTP
         * Sessions to this <tt>channel</tt>.
         *
         * @param payloadType the <tt>payload-type</tt> element to be added to
         * this <tt>channel</tt>
         * @return <tt>true</tt> if the list of <tt>payload-type</tt> elements
         * associated with this <tt>channel</tt> has been modified as part of
         * the method call; otherwise, <tt>false</tt>
         * @throws NullPointerException if the specified <tt>payloadType</tt> is
         * <tt>null</tt>
         */
        public boolean addPayloadType(PayloadTypePacketExtension payloadType)
        {
            if (payloadType == null)
                throw new NullPointerException("payloadType");

            // Make sure that the COLIBRI namespace is used.
            payloadType.setNamespace(null);
            for (ParameterPacketExtension p : payloadType.getParameters())
                p.setNamespace(null);

            return
                payloadTypes.contains(payloadType)
                    ? false
                    : payloadTypes.add(payloadType);
        }

        /**
         * Adds a <tt>SourcePacketExtension</tt> to the list of sources of this
         * channel.
         *
         * @param source the <tt>SourcePacketExtension</tt> to add to the list
         * of sources of this channel
         * @return <tt>true</tt> if the list of sources of this channel changed
         * as a result of the execution of the method; otherwise, <tt>false</tt>
         */
        public synchronized boolean addSource(SourcePacketExtension source)
        {
            if (source == null)
                throw new NullPointerException("source");

            return sources.contains(source) ? false : sources.add(source);
        }

        /**
         * Adds a specific (RTP) SSRC to the list of SSRCs seen/received on this
         * <tt>Channel</tt>. Invoked by the Jitsi Videobridge server, not its
         * clients.
         *
         * @param ssrc the (RTP) SSRC to be added to the list of SSRCs
         * seen/received on this <tt>Channel</tt>
         * @return <tt>true</tt> if the list of SSRCs seen/received on this
         * <tt>Channel</tt> has been modified as part of the method call;
         * otherwise, <tt>false</tt>
         */
        public synchronized boolean addSSRC(int ssrc)
        {
            // contains
            for (int i = 0; i < ssrcs.length; i++)
                if (ssrcs[i] == ssrc)
                    return false;

            // add
            int[] newSSRCs = new int[ssrcs.length + 1];

            System.arraycopy(ssrcs, 0, newSSRCs, 0, ssrcs.length);
            newSSRCs[ssrcs.length] = ssrc;
            ssrcs = newSSRCs;
            return true;
        }

        /**
         * Gets the <tt>direction</tt> of this <tt>Channel</tt>.
         *
         * @return the <tt>direction</tt> of this <tt>Channel</tt>.
         */
        public MediaDirection getDirection()
        {
            return (direction == null) ? MediaDirection.SENDRECV : direction;
        }

        /**
         * Gets the IP address (as a <tt>String</tt> value) of the host on which
         * the <tt>channel</tt> represented by this instance has been allocated.
         *
         * @return a <tt>String</tt> value which represents the IP address of
         * the host on which the <tt>channel</tt> represented by this instance
         * has been allocated
         *
         * @deprecated The method is supported for the purposes of compatibility
         * with legacy versions of Jitsi and Jitsi Videobridge. 
         */
        @Deprecated
        public String getHost()
        {
            return host;
        }

        /**
         * Gets the ID of the <tt>channel</tt> represented by this instance.
         *
         * @return the ID of the <tt>channel</tt> represented by this instance
         */
        public String getID()
        {
            return id;
        }

        /**
         * Gets the maximum number of video RTP streams to be sent from Jitsi
         * Videobridge to the endpoint associated with this video
         * <tt>Channel</tt>.
         *
         * @return the maximum number of video RTP streams to be sent from Jitsi
         * Videobridge to the endpoint associated with this video
         * <tt>Channel</tt>
         */
        public Integer getLastN()
        {
            return lastN;
        }

        /**
         * Gets a list of <tt>payload-type</tt> elements defined by XEP-0167:
         * Jingle RTP Sessions added to this <tt>channel</tt>.
         *
         * @return an unmodifiable <tt>List</tt> of <tt>payload-type</tt>
         * elements defined by XEP-0167: Jingle RTP Sessions added to this
         * <tt>channel</tt>
         */
        public List<PayloadTypePacketExtension> getPayloadTypes()
        {
            return Collections.unmodifiableList(payloadTypes);
        }

        /**
         * Gets the port which has been allocated to this <tt>channel</tt> for
         * the purposes of transmitting RTCP packets.
         *
         * @return the port which has been allocated to this <tt>channel</tt>
         * for the purposes of transmitting RTCP packets
         *
         * @deprecated The method is supported for the purposes of compatibility
         * with legacy versions of Jitsi and Jitsi Videobridge. 
         */
        @Deprecated
        public int getRTCPPort()
        {
            return rtcpPort;
        }

        /**
         * Gets the type of RTP-level relay (in the terms specified by RFC 3550
         * &quot;RTP: A Transport Protocol for Real-Time Applications&quot; in
         * section 2.3 &quot;Mixers and Translators&quot;) used for this
         * <tt>Channel</tt>.
         *
         * @return the type of RTP-level relay used for this <tt>Channel</tt>
         */
        public RTPLevelRelayType getRTPLevelRelayType()
        {
            return rtpLevelRelayType;
        }

        /**
         * Gets the port which has been allocated to this <tt>channel</tt> for
         * the purposes of transmitting RTP packets.
         *
         * @return the port which has been allocated to this <tt>channel</tt>
         * for the purposes of transmitting RTP packets
         *
         * @deprecated The method is supported for the purposes of compatibility
         * with legacy versions of Jitsi and Jitsi Videobridge. 
         */
        @Deprecated
        public int getRTPPort()
        {
            return rtpPort;
        }

        /**
         * Gets the list of <tt>SourcePacketExtensions</tt>s which represent the
         * sources of this channel.
         *
         * @return a <tt>List</tt> of <tt>SourcePacketExtension</tt>s which
         * represent the sources of this channel
         */
        public synchronized List<SourcePacketExtension> getSources()
        {
            return new ArrayList<SourcePacketExtension>(sources);
        }

        /**
         * Gets (a copy of) the list of (RTP) SSRCs seen/received on this
         * <tt>Channel</tt>.
         *
         * @return an array of <tt>int</tt>s which represents (a copy of) the
         * list of (RTP) SSRCs seen/received on this <tt>Channel</tt>
         */
        public synchronized int[] getSSRCs()
        {
            return (ssrcs.length == 0) ? NO_SSRCS : ssrcs.clone();
        }

        /**
         * Removes a <tt>payload-type</tt> element defined by XEP-0167: Jingle
         * RTP Sessions from this <tt>channel</tt>.
         *
         * @param payloadType the <tt>payload-type</tt> element to be removed
         * from this <tt>channel</tt>
         * @return <tt>true</tt> if the list of <tt>payload-type</tt> elements
         * associated with this <tt>channel</tt> has been modified as part of
         * the method call; otherwise, <tt>false</tt>
         */
        public boolean removePayloadType(PayloadTypePacketExtension payloadType)
        {
            return payloadTypes.remove(payloadType);
        }

        /**
         * Removes a <tt>SourcePacketExtension</tt> from the list of sources of
         * this channel.
         *
         * @param source the <tt>SourcePacketExtension</tt> to remove from the
         * list of sources of this channel
         * @return <tt>true</tt> if the list of sources of this channel changed
         * as a result of the execution of the method; otherwise, <tt>false</tt>
         */
        public synchronized boolean removeSource(SourcePacketExtension source)
        {
            return sources.remove(source);
        }

        /**
         * Removes a specific (RTP) SSRC from the list of SSRCs seen/received on
         * this <tt>Channel</tt>. Invoked by the Jitsi Videobridge server, not
         * its clients.
         *
         * @param ssrc the (RTP) SSRC to be removed from the list of SSRCs
         * seen/received on this <tt>Channel</tt>
         * @return <tt>true</tt> if the list of SSRCs seen/received on this
         * <tt>Channel</tt> has been modified as part of the method call;
         * otherwise, <tt>false</tt>
         */
        public synchronized boolean removeSSRC(int ssrc)
        {
            if (ssrcs.length == 1)
            {
                if (ssrcs[0] == ssrc)
                {
                    ssrcs = NO_SSRCS;
                    return true;
                }
                else
                    return false;
            }
            else
            {
                for (int i = 0; i < ssrcs.length; i++)
                {
                    if (ssrcs[i] == ssrc)
                    {
                        int[] newSSRCs = new int[ssrcs.length - 1];

                        if (i != 0)
                            System.arraycopy(ssrcs, 0, newSSRCs, 0, i);
                        if (i != newSSRCs.length)
                        {
                            System.arraycopy(
                                    ssrcs, i + 1,
                                    newSSRCs, i,
                                    newSSRCs.length - i);
                        }
                        ssrcs = newSSRCs;
                        return true;
                    }
                }
                return false;
            }
        }

        /**
         * Sets the <tt>direction</tt> of this <tt>Channel</tt>
         *
         * @param direction the <tt>MediaDirection</tt> to set the
         * <tt>direction</tt> of this <tt>Channel</tt> to.
         */
        public void setDirection(MediaDirection direction)
        {
            this.direction = direction;
        }

        /**
         * Sets the IP address (as a <tt>String</tt> value) of the host on which
         * the <tt>channel</tt> represented by this instance has been allocated.
         *
         * @param host a <tt>String</tt> value which represents the IP address
         * of the host on which the <tt>channel</tt> represented by this
         * instance has been allocated
         *
         * @deprecated The method is supported for the purposes of compatibility
         * with legacy versions of Jitsi and Jitsi Videobridge. 
         */
        @Deprecated
        public void setHost(String host)
        {
            this.host = host;
        }

        /**
         * Sets the ID of the <tt>channel</tt> represented by this instance.
         *
         * @param id the ID of the <tt>channel</tt> represented by this instance
         */
        public void setID(String id)
        {
            this.id = id;
        }

        /**
         * Sets the maximum number of video RTP streams to be sent from Jitsi
         * Videobridge to the endpoint associated with this video
         * <tt>Channel</tt>.
         *
         * @param lastN the maximum number of video RTP streams to be sent from
         * Jitsi Videobridge to the endpoint associated with this video
         * <tt>Channel</tt>
         */
        public void setLastN(Integer lastN)
        {
            this.lastN = lastN;
        }

        /**
         * Sets the port which has been allocated to this <tt>channel</tt> for
         * the purposes of transmitting RTCP packets.
         *
         * @param rtcpPort the port which has been allocated to this
         * <tt>channel</tt> for the purposes of transmitting RTCP packets
         *
         * @deprecated The method is supported for the purposes of compatibility
         * with legacy versions of Jitsi and Jitsi Videobridge. 
         */
        @Deprecated
        public void setRTCPPort(int rtcpPort)
        {
            this.rtcpPort = rtcpPort;
        }

        /**
         * Sets the type of RTP-level relay (in the terms specified by RFC 3550
         * &quot;RTP: A Transport Protocol for Real-Time Applications&quot; in
         * section 2.3 &quot;Mixers and Translators&quot;) used for this
         * <tt>Channel</tt>.
         *
         * @param rtpLevelRelayType the type of RTP-level relay used for
         * this <tt>Channel</tt>
         */
        public void setRTPLevelRelayType(RTPLevelRelayType rtpLevelRelayType)
        {
            this.rtpLevelRelayType = rtpLevelRelayType;
        }

        /**
         * Sets the type of RTP-level relay (in the terms specified by RFC 3550
         * &quot;RTP: A Transport Protocol for Real-Time Applications&quot; in
         * section 2.3 &quot;Mixers and Translators&quot;) used for this
         * <tt>Channel</tt>.
         *
         * @param s the type of RTP-level relay used for this <tt>Channel</tt>
         */
        public void setRTPLevelRelayType(String s)
        {
            setRTPLevelRelayType(RTPLevelRelayType.parseRTPLevelRelayType(s));
        }

        /**
         * Sets the port which has been allocated to this <tt>channel</tt> for
         * the purposes of transmitting RTP packets.
         *
         * @param rtpPort the port which has been allocated to this
         * <tt>channel</tt> for the purposes of transmitting RTP packets
         *
         * @deprecated The method is supported for the purposes of compatibility
         * with legacy versions of Jitsi and Jitsi Videobridge. 
         */
        @Deprecated
        public void setRTPPort(int rtpPort)
        {
            this.rtpPort = rtpPort;
        }

        /**
         * Sets the list of (RTP) SSRCs seen/received on this <tt>Channel</tt>.
         *
         * @param ssrcs the list of (RTP) SSRCs to be set as seen/received on
         * this <tt>Channel</tt>
         */
        public void setSSRCs(int[] ssrcs)
        {
            /*
             * TODO Make sure that the SSRCs set on this instance do not contain
             * duplicates.
             */
            this.ssrcs
                = ((ssrcs == null) || (ssrcs.length == 0))
                    ? NO_SSRCS
                    : ssrcs.clone();
        }

        @Override
        protected void printAttributes(StringBuilder xml)
        {
            // direction
            MediaDirection direction = getDirection();

            if ((direction != null) && (direction != MediaDirection.SENDRECV))
            {
                xml.append(' ').append(DIRECTION_ATTR_NAME).append("='")
                        .append(direction.toString()).append('\'');
            }

            // host
            String host = getHost();

            if (host != null)
            {
                xml.append(' ').append(HOST_ATTR_NAME).append("='").append(host)
                        .append('\'');
            }

            // id
            String id = getID();

            if (id != null)
            {
                xml.append(' ').append(ID_ATTR_NAME).append("='").append(id)
                        .append('\'');
            }

            // lastN
            Integer lastN = getLastN();

            if (lastN != null)
            {
                xml.append(' ').append(LAST_N_ATTR_NAME).append("='")
                        .append(lastN).append('\'');
            }

            // rtcpPort
            int rtcpPort = getRTCPPort();

            if (rtcpPort > 0)
            {
                xml.append(' ').append(RTCP_PORT_ATTR_NAME).append("='")
                        .append(rtcpPort).append('\'');
            }

            // rtpLevelRelayType
            RTPLevelRelayType rtpLevelRelayType = getRTPLevelRelayType();

            if (rtpLevelRelayType != null)
            {
                xml.append(' ').append(RTP_LEVEL_RELAY_TYPE_ATTR_NAME)
                        .append("='").append(rtpLevelRelayType).append('\'');
            }

            // rtpPort
            int rtpPort = getRTPPort();

            if (rtpPort > 0)
            {
                xml.append(' ').append(RTP_PORT_ATTR_NAME).append("='")
                        .append(rtpPort).append('\'');
            }
        }

        @Override
        protected boolean hasContent()
        {
            List<PayloadTypePacketExtension> payloadTypes = getPayloadTypes();
            boolean hasPayloadTypes = !payloadTypes.isEmpty();
            List<SourcePacketExtension> sources = getSources();
            boolean hasSources = !sources.isEmpty();
            int[] ssrcs = getSSRCs();
            boolean hasSSRCs = (ssrcs.length != 0);

            return hasPayloadTypes || hasSources || hasSSRCs;
        }

        @Override
        protected void printContent(StringBuilder xml)
        {
            List<PayloadTypePacketExtension> payloadTypes = getPayloadTypes();
            List<SourcePacketExtension> sources = getSources();
            int[] ssrcs = getSSRCs();

            for (PayloadTypePacketExtension payloadType : payloadTypes)
                xml.append(payloadType.toXML());

            for (SourcePacketExtension source : sources)
                xml.append(source.toXML());

            for (int i = 0; i < ssrcs.length; i++)
            {
                xml.append('<').append(SSRC_ELEMENT_NAME).append('>')
                    .append(Long.toString(ssrcs[i] & 0xFFFFFFFFL))
                    .append("</").append(SSRC_ELEMENT_NAME)
                    .append('>');
            }
        }
    }

    /**
     * Represents a <tt>content</tt> included into a Jitsi Videobridge
     * <tt>conference</tt> IQ.
     */
    public static class Content
    {
        /**
         * The XML element name of a <tt>content</tt> of a Jitsi Videobridge
         * <tt>conference</tt> IQ.
         */
        public static final String ELEMENT_NAME = "content";

        /**
         * The XML name of the <tt>name</tt> attribute of a <tt>content</tt> of
         * a <tt>conference</tt> IQ which represents the <tt>name</tt> property
         * of <tt>ColibriConferenceIQ.Content</tt>.
         */
        public static final String NAME_ATTR_NAME = "name";

        /**
         * The list of {@link Channel}s included into this <tt>content</tt> of a
         * <tt>conference</tt> IQ.
         */
        private final List<Channel> channels = new LinkedList<Channel>();

        /**
         * The list of {@link SctpConnection}s included into this
         * <tt>content</tt> of a <tt>conference</tt> IQ.
         */
        private final List<SctpConnection> sctpConnections
            = new LinkedList<SctpConnection>();

        /**
         * The name of the <tt>content</tt> represented by this instance.
         */
        private String name;

        /**
         * Initializes a new <tt>Content</tt> instance without a name and
         * channels.
         */
        public Content()
        {
        }

        /**
         * Initializes a new <tt>Content</tt> instance with a specific name and
         * without channels.
         *
         * @param name the name to initialize the new instance with
         */
        public Content(String name)
        {
            setName(name);
        }

        /**
         * Adds a specific <tt>Channel</tt> to the list of <tt>Channel</tt>s
         * included into this <tt>Content</tt>.
         *
         * @param channel the <tt>Channel</tt> to be included into this
         * <tt>Content</tt>
         * @return <tt>true</tt> if the list of <tt>Channel</tt>s included into
         * this <tt>Content</tt> was modified as a result of the execution of
         * the method; otherwise, <tt>false</tt>
         * @throws NullPointerException if the specified <tt>channel</tt> is
         * <tt>null</tt>
         */
        public boolean addChannel(Channel channel)
        {
            if (channel == null)
                throw new NullPointerException("channel");

            return channels.contains(channel) ? false : channels.add(channel);
        }

        /**
         * Gets the <tt>Channel</tt> at a specific index/position within the
         * list of <tt>Channel</tt>s included in this <tt>Content</tt>.
         *
         * @param channelIndex the index/position within the list of
         * <tt>Channel</tt>s included in this <tt>Content</tt> of the
         * <tt>Channel</tt> to be returned
         * @return the <tt>Channel</tt> at the specified <tt>channelIndex</tt>
         * within the list of <tt>Channel</tt>s included in this
         * <tt>Content</tt>
         */
        public Channel getChannel(int channelIndex)
        {
            return getChannels().get(channelIndex);
        }

        /**
         * Gets a <tt>Channel</tt> which is included into this <tt>Content</tt>
         * and which has a specific ID.
         *
         * @param channelID the ID of the <tt>Channel</tt> included into this
         * <tt>Content</tt> to be returned
         * @return the <tt>Channel</tt> which is included into this
         * <tt>Content</tt> and which has the specified <tt>channelID</tt> if
         * such a <tt>Channel</tt> exists; otherwise, <tt>null</tt>
         */
        public Channel getChannel(String channelID)
        {
            for (Channel channel : getChannels())
                if (channelID.equals(channel.getID()))
                    return channel;
            return null;
        }

        /**
         * Gets the number of <tt>Channel</tt>s included into/associated with
         * this <tt>Content</tt>.
         *
         * @return the number of <tt>Channel</tt>s included into/associated with
         * this <tt>Content</tt>
         */
        public int getChannelCount()
        {
            return getChannels().size();
        }

        /**
         * Gets a list of the <tt>Channel</tt> included into/associated with
         * this <tt>Content</tt>.
         *
         * @return an unmodifiable <tt>List</tt> of the <tt>Channel</tt>s
         * included into/associated with this <tt>Content</tt>
         */
        public List<Channel> getChannels()
        {
            return Collections.unmodifiableList(channels);
        }

        /**
         * Adds a specific <tt>SctpConnection</tt> to the list of
         * <tt>SctpConnection</tt>s included into this <tt>Content</tt>.
         *
         * @param conn the <tt>SctpConnection</tt> to be included into this
         * <tt>Content</tt>
         * @return <tt>true</tt> if the list of <tt>SctpConnection</tt>s
         * included into this <tt>Content</tt> was modified as a result of
         * the execution of the method; otherwise, <tt>false</tt>
         * @throws NullPointerException if the specified <tt>conn</tt> is
         * <tt>null</tt>
         */
        public boolean addSctpConnection(SctpConnection conn)
        {
            if(conn == null)
                throw new NullPointerException("Sctp connection");

            return !sctpConnections.contains(conn) && sctpConnections.add(conn);
        }

        /**
         * Gets a list of the <tt>SctpConnection</tt>s included into/associated
         * with this <tt>Content</tt>.
         *
         * @return an unmodifiable <tt>List</tt> of the <tt>SctpConnection</tt>s
         * included into/associated with this <tt>Content</tt>
         */
        public List<SctpConnection> getSctpConnections()
        {
            return Collections.unmodifiableList(sctpConnections);
        }

        /**
         * Gets the name of the <tt>content</tt> represented by this instance.
         *
         * @return the name of the <tt>content</tt> represented by this instance
         */
        public String getName()
        {
            return name;
        }

        /**
         * Removes a specific <tt>Channel</tt> from the list of
         * <tt>Channel</tt>s included into this <tt>Content</tt>.
         *
         * @param channel the <tt>Channel</tt> to be excluded from this
         * <tt>Content</tt>
         * @return <tt>true</tt> if the list of <tt>Channel</tt>s included into
         * this <tt>Content</tt> was modified as a result of the execution of
         * the method; otherwise, <tt>false</tt>
         */
        public boolean removeChannel(Channel channel)
        {
            return channels.remove(channel);
        }

        /**
         * Sets the name of the <tt>content</tt> represented by this instance.
         *
         * @param name the name of the <tt>content</tt> represented by this
         * instance
         * @throws NullPointerException if the specified <tt>name</tt> is
         * <tt>null</tt>
         */
        public void setName(String name)
        {
            if (name == null)
                throw new NullPointerException("name");

            this.name = name;
        }

        /**
         * Appends the XML <tt>String</tt> representation of this
         * <tt>Content</tt> to a specific <tt>StringBuilder</tt>.
         *
         * @param xml the <tt>StringBuilder</tt> to which the XML
         * <tt>String</tt> representation of this <tt>Content</tt> is to be
         * appended
         */
        public void toXML(StringBuilder xml)
        {
            xml.append('<').append(ELEMENT_NAME);
            xml.append(' ').append(NAME_ATTR_NAME).append("='")
                    .append(getName()).append('\'');

            List<Channel> channels = getChannels();
            List<SctpConnection> connections = getSctpConnections();

            if (channels.size() == 0 && connections.size() == 0)
            {
                xml.append(" />");
            }
            else
            {
                xml.append('>');
                for (Channel channel : channels)
                    channel.toXML(xml);
                for(SctpConnection conn : connections)
                    conn.toXML(xml);
                xml.append("</").append(ELEMENT_NAME).append('>');
            }
        }
    }

    /**
     * Represents a <tt>SCTP connection</tt> included into a <tt>content</tt>
     * of a Jitsi Videobridge <tt>conference</tt> IQ.
     *
     * @author Pawel Domas
     */
    public static class SctpConnection
        extends ChannelCommon
    {
        /**
         * The XML element name of a <tt>content</tt> of a Jitsi Videobridge
         * <tt>conference</tt> IQ.
         */
        public static final String ELEMENT_NAME = "sctpconnection";

        /**
         * The XML name of the <tt>port</tt> attribute of a
         * <tt>SctpConnection</tt> of a <tt>conference</tt> IQ which represents
         * the SCTP port property of
         * <tt>ColibriConferenceIQ.SctpConnection</tt>.
         */
        public static final String PORT_ATTR_NAME = "port";

        /**
         * SCTP port attribute. 5000 by default.
         */
        private int port = 5000;

        /**
         * Initializes a new <tt>SctpConnection</tt> instance without an
         * endpoint name and with default port value set.
         */
        public SctpConnection()
        {
            super(SctpConnection.ELEMENT_NAME);
        }

        /**
         * Gets the SCTP port of the <tt>SctpConnection</tt> described by this
         * instance.
         *
         * @return the SCTP port of the <tt>SctpConnection</tt> represented by
         *         this instance.
         */
        public int getPort()
        {
            return port;
        }

        /**
         * Sets the SCTP port of the <tt>SctpConnection</tt> represented by this
         * instance.
         *
         * @param port the SCTP port of the <tt>SctpConnection</tt>
         *             represented by this instance
         */
        public void setPort(int port)
        {
            this.port = port;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void printAttributes(StringBuilder xml)
        {
            xml.append(' ').append(PORT_ATTR_NAME).append("='")
                .append(getPort()).append('\'');
        }

        /**
         * {@inheritDoc}
         *
         * No content other than transport for <tt>SctpConnection</tt>.
         */
        @Override
        protected boolean hasContent()
        {
            return false;
        }

        @Override
        protected void printContent(StringBuilder xml)
        {
            // No other content than the transport shared from ChannelCommon
        }
    }

    /**
     * Represents a <tt>recording</tt> element.
     */
    public static class Recording
    {
        /**
         * The XML name of the <tt>recording</tt> element.
         */
        public static final String ELEMENT_NAME = "recording";

        /**
         * The XML name of the <tt>state</tt> attribute.
         */
        public static final String STATE_ATTR_NAME = "state";

        /**
         * The XML name of the <tt>token</tt> attribute.
         */
        public static final String TOKEN_ATTR_NAME = "token";

        /**
         * The XML name of the <tt>path</tt> attribute.
         */
        public static final String PATH_ATTR_NAME = "path";

        private String token = null;
        private boolean state;
        private String path = null;

        public Recording(boolean state)
        {
            this.state = state;
        }

        public Recording(boolean state, String token)
        {
            this(state);

            this.token = token;
        }

        public String getToken()
        {
            return token;
        }

        public String getPath()
        {
            return path;
        }

        public void setPath(String path)
        {
            this.path = path;
        }

        public boolean getState()
        {
            return state;
        }

        public void toXML(StringBuilder xml)
        {
            xml.append('<').append(ELEMENT_NAME);
            xml.append(' ').append(STATE_ATTR_NAME).append("='")
                    .append(state).append('\'');
            if (token != null)
            {
                xml.append(' ').append(TOKEN_ATTR_NAME).append("='")
                        .append(token).append('\'');
            }
            if (path != null)
            {
                xml.append(' ').append(PATH_ATTR_NAME).append("='")
                        .append(path).append('\'');
            }
            xml.append("/>");
        }
    }
}
