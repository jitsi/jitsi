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
 * Implements the Jitsi VideoBridge <tt>conference</tt> IQ within the
 * COnferencing with LIghtweight BRIdging.
 *
 * @author Lyubomir Marinov
 */
public class ColibriConferenceIQ
    extends IQ
{
    /**
     * The XML element name of the Jitsi VideoBridge <tt>conference</tt> IQ.
     */
    public static final String ELEMENT_NAME = "conference";

    /**
     * The XML name of the <tt>id</tt> attribute of the Jitsi VideoBridge
     * <tt>conference</tt> IQ which represents the value of the <tt>id</tt>
     * property of <tt>ColibriConferenceIQ</tt>.
     */
    public static final String ID_ATTR_NAME = "id";

    /**
     * The XML COnferencing with LIghtweight BRIdging namespace of the Jitsi
     * VideoBridge <tt>conference</tt> IQ.
     */
    public static final String NAMESPACE
        = "http://jitsi.org/protocol/colibri";

    /**
     * An array of <tt>long</tt>s which represents the lack of any (RTP) SSRCs
     * seen/received on a <tt>Channel</tt>. Explicitly defined to reduce
     * unnecessary allocations.
     */
    public static final long[] NO_SSRCS = new long[0];

    /**
     * The list of {@link Content}s included into this <tt>conference</tt> IQ.
     */
    private final List<Content> contents = new LinkedList<Content>();

    /**
     * The ID of the conference represented by this IQ.
     */
    private String id;

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

        if (contents.size() == 0)
        {
            xml.append(" />");
        }
        else
        {
            xml.append('>');
            for (Content content : contents)
                content.toXML(xml);
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

    /**
     * Represents a <tt>channel</tt> included into a <tt>content</tt> of a Jitsi
     * VideoBridge <tt>conference</tt> IQ.
     */
    public static class Channel
    {
        /**
         * The XML element name of a <tt>channel</tt> of a <tt>content</tt> of a
         * Jitsi VideoBridge <tt>conference</tt> IQ.
         */
        public static final String ELEMENT_NAME = "channel";

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
         * The XML name of the <tt>host</tt> attribute of a <tt>channel</tt> of
         * a <tt>content</tt> of a <tt>conference</tt> IQ which represents the
         * value of the <tt>host</tt> property of
         * <tt>ColibriConferenceIQ.Channel</tt>.
         */
        public static final String HOST_ATTR_NAME = "host";

        /**
         * The XML name of the <tt>id</tt> attribute of a <tt>channel</tt> of a
         * <tt>content</tt> of a <tt>conference</tt> IQ which represents the
         * value of the <tt>id</tt> property of
         * <tt>ColibriConferenceIQ.Channel</tt>.
         */
        public static final String ID_ATTR_NAME = "id";

        /**
         * The XML name of the <tt>rtcpport</tt> attribute of a <tt>channel</tt>
         * of a <tt>content</tt> of a <tt>conference</tt> IQ which represents
         * the value of the <tt>rtcpPort</tt> property of
         * <tt>ColibriConferenceIQ.Channel</tt>.
         */
        public static final String RTCP_PORT_ATTR_NAME = "rtcpport";

        /**
         * The XML name of the <tt>rtpport</tt> attribute of a <tt>channel</tt>
         * of a <tt>content</tt> of a <tt>conference</tt> IQ which represents
         * the value of the <tt>rtpPort</tt> property of
         * <tt>ColibriConferenceIQ.Channel</tt>.
         */
        public static final String RTP_PORT_ATTR_NAME = "rtpport";

        /**
         * The name of the XML element which is a child of the &lt;channel&gt;
         * element and which identifies/specifies an (RTP) SSRC which has been
         * seen/received on the respective <tt>Channel</tt>.
         */
        public static final String SSRC_ELEMENT_NAME = "ssrc";

        /**
         * The name of the XML attribute of a <tt>channel</tt> which represents
         * its direction.
         */
        public static final String DIRECTION_ATTR_NAME = "direction";

        /**
         * The number of seconds of inactivity after which the <tt>channel</tt>
         * represented by this instance expires.
         */
        private int expire = EXPIRE_NOT_SPECIFIED;

        /**
         * The host of the <tt>channel</tt> represented by this instance.
         */
        private String host;

        /**
         * The ID of the <tt>channel</tt> represented by this instance.
         */
        private String id;

        /**
         * The <tt>payload-type</tt> elements defined by XEP-0167: Jingle RTP
         * Sessions associated with this <tt>channel</tt>.
         */
        private final List<PayloadTypePacketExtension> payloadTypes
            = new ArrayList<PayloadTypePacketExtension>();

        /**
         * The RTCP port of the <tt>channel</tt> represented by this instance.
         */
        private int rtcpPort;

        /**
         * The RTP port of the <tt>channel</tt> represented by this instance.
         */
        private int rtpPort;

        /**
         * The list of (RTP) SSRCs which have been seen/received on this
         * <tt>Channel</tt> by now. These may exclude SSRCs which are no longer
         * active. Set by the Jitsi VideoBridge server, not its clients.
         */
        private long[] ssrcs = NO_SSRCS;

        /**
         * The direction of the <tt>channel</tt> represented by this instance.
         */
        private MediaDirection direction;

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
         * Adds a specific (RTP) SSRC to the list of SSRCs seen/received on this
         * <tt>Channel</tt>. Invoked by the Jitsi VideoBridge server, not its
         * clients.
         *
         * @param ssrc the (RTP) SSRC to be added to the list of SSRCs
         * seen/received on this <tt>Channel</tt>
         * @return <tt>true</tt> if the list of SSRCs seen/received on this
         * <tt>Channel</tt> has been modified as part of the method call;
         * otherwise, <tt>false</tt>
         */
        public synchronized boolean addSSRC(long ssrc)
        {
            // contains
            for (long element : ssrcs)
                if (element == ssrc)
                    return false;

            // add
            long[] newSSRCs = new long[ssrcs.length + 1];

            System.arraycopy(ssrcs, 0, newSSRCs, 0, ssrcs.length);
            newSSRCs[ssrcs.length] = ssrc;
            ssrcs = newSSRCs;
            return true;
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

        /**
         * Gets the IP address (as a <tt>String</tt> value) of the host on which
         * the <tt>channel</tt> represented by this instance has been allocated.
         *
         * @return a <tt>String</tt> value which represents the IP address of
         * the host on which the <tt>channel</tt> represented by this instance
         * has been allocated
         */
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
         */
        public int getRTCPPort()
        {
            return rtcpPort;
        }

        /**
         * Gets the port which has been allocated to this <tt>channel</tt> for
         * the purposes of transmitting RTP packets.
         *
         * @return the port which has been allocated to this <tt>channel</tt>
         * for the purposes of transmitting RTP packets
         */
        public int getRTPPort()
        {
            return rtpPort;
        }

        /**
         * Gets (a copy of) the list of (RTP) SSRCs seen/received on this
         * <tt>Channel</tt>.
         *
         * @return an array of <tt>long</tt>s which represents (a copy of) the
         * list of (RTP) SSRCs seen/received on this <tt>Channel</tt>
         */
        public synchronized long[] getSSRCs()
        {
            return (ssrcs.length == 0) ? NO_SSRCS : ssrcs.clone();
        }

        /**
         * Gets the <tt>direction</tt> of this <tt>Channel</tt>.
         *
         * @return the <tt>direction</tt> of this <tt>Channel</tt>.
         */
        public MediaDirection getDirection()
        {
            return direction == null
                    ? MediaDirection.SENDRECV
                    : direction;
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
         * Removes a specific (RTP) SSRC from the list of SSRCs seen/received on
         * this <tt>Channel</tt>. Invoked by the Jitsi VideoBridge server, not
         * its clients.
         *
         * @param ssrc the (RTP) SSRC to be removed from the list of SSRCs
         * seen/received on this <tt>Channel</tt>
         * @return <tt>true</tt> if the list of SSRCs seen/received on this
         * <tt>Channel</tt> has been modified as part of the method call;
         * otherwise, <tt>false</tt>
         */
        public synchronized boolean removeSSRC(long ssrc)
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
                        long[] newSSRCs = new long[ssrcs.length - 1];

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
         * Sets the IP address (as a <tt>String</tt> value) of the host on which
         * the <tt>channel</tt> represented by this instance has been allocated.
         *
         * @param host a <tt>String</tt> value which represents the IP address
         * of the host on which the <tt>channel</tt> represented by this
         * instance has been allocated
         */
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
         * Sets the port which has been allocated to this <tt>channel</tt> for
         * the purposes of transmitting RTCP packets.
         *
         * @param rtcpPort the port which has been allocated to this
         * <tt>channel</tt> for the purposes of transmitting RTCP packets
         */
        public void setRTCPPort(int rtcpPort)
        {
            this.rtcpPort = rtcpPort;
        }

        /**
         * Sets the port which has been allocated to this <tt>channel</tt> for
         * the purposes of transmitting RTP packets.
         *
         * @param rtpPort the port which has been allocated to this
         * <tt>channel</tt> for the purposes of transmitting RTP packets
         */
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
        public void setSSRCs(long[] ssrcs)
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
         * Appends the XML <tt>String</tt> representation of this
         * <tt>Channel</tt> to a specific <tt>StringBuilder</tt>.
         *
         * @param xml the <tt>StringBuilder</tt> to which the XML
         * <tt>String</tt> representation of this <tt>Channel</tt> is to be
         * appended
         */
        public void toXML(StringBuilder xml)
        {
            xml.append('<').append(ELEMENT_NAME);

            String id = getID();

            if (id != null)
            {
                xml.append(' ').append(ID_ATTR_NAME).append("='").append(id)
                        .append('\'');
            }

            String host = getHost();

            if (host != null)
            {
                xml.append(' ').append(HOST_ATTR_NAME).append("='").append(host)
                        .append('\'');
            }

            int rtpPort = getRTPPort();

            if (rtpPort > 0)
            {
                xml.append(' ').append(RTP_PORT_ATTR_NAME).append("='")
                        .append(rtpPort).append('\'');
            }

            int rtcpPort = getRTCPPort();

            if (rtcpPort > 0)
            {
                xml.append(' ').append(RTCP_PORT_ATTR_NAME).append("='")
                        .append(rtcpPort).append('\'');
            }

            int expire = getExpire();

            if (expire >= 0)
            {
                xml.append(' ').append(EXPIRE_ATTR_NAME).append("='")
                        .append(expire).append('\'');
            }

            MediaDirection direction = getDirection();
            if (direction != null && direction != MediaDirection.SENDRECV)
            {
                xml.append(' ').append(DIRECTION_ATTR_NAME).append("='")
                        .append(direction.toString()).append('\'');
            }

            List<PayloadTypePacketExtension> payloadTypes = getPayloadTypes();
            boolean hasPayloadTypes = (payloadTypes.size() != 0);
            long[] ssrcs = getSSRCs();
            boolean hasSSRCs = (ssrcs.length != 0);

            if (hasPayloadTypes || hasSSRCs)
            {
                xml.append('>');
                if (hasPayloadTypes)
                {
                    for (PayloadTypePacketExtension payloadType : payloadTypes)
                        xml.append(payloadType.toXML());
                }
                if (hasSSRCs)
                {
                    for (long ssrc : ssrcs)
                    {
                        xml.append('<').append(SSRC_ELEMENT_NAME).append('>')
                                .append(ssrc).append("</")
                                        .append(SSRC_ELEMENT_NAME).append('>');
                    }
                }
                xml.append("</").append(ELEMENT_NAME).append('>');
            }
            else
            {
                xml.append(" />");
            }
        }
    }

    /**
     * Represents a <tt>content</tt> included into a Jitsi VideoBridge
     * <tt>conference</tt> IQ.
     */
    public static class Content
    {
        /**
         * The XML element name of a <tt>content</tt> of a Jitsi VideoBridge
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

            if (channels.size() == 0)
            {
                xml.append(" />");
            }
            else
            {
                xml.append('>');
                for (Channel channel : channels)
                    channel.toXML(xml);
                xml.append("</").append(ELEMENT_NAME).append('>');
            }
        }
    }
}
