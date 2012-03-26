/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.cobri;

import java.util.*;

import net.java.sip.communicator.impl.protocol.jabber.extensions.jingle.*;

import org.jivesoftware.smack.packet.*;

/**
 * Implements the Jitsi VideoBridge <tt>conference</tt> IQ.
 *
 * @author Lyubomir Marinov
 */
public class CobriConferenceIQ
    extends IQ
{
    /**
     * The XML element name of the Jitsi VideoBridge <tt>conference</tt> IQ.
     */
    public static final String ELEMENT_NAME = "conference";

    /**
     * The XML name of the <tt>id</tt> attribute of the Jitsi VideoBridge
     * <tt>conference</tt> IQ which represents the value of the <tt>id</tt>
     * property of <tt>CobriConferenceIQ</tt>.
     */
    public static final String ID_ATTR_NAME = "id";

    /**
     * The XML namespace of the Jitsi VideoBridge <tt>conference</tt> IQ.
     */
    public static final String NAMESPACE
        = "http://jitsi.org/protocol/videobridge#conference";

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

    public Content getContent(String contentName)
    {
        for (Content content : getContents())
            if (contentName.equals(content.getName()))
                return content;
        return null;
    }

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
         * <tt>CobriConferenceIQ.Channel</tt>.
         */
        public static final String EXPIRE_ATTR_NAME = "expire";

        /**
         * The value of the <tt>expire</tt> property of
         * <tt>CobriConferenceIQ.Channel</tt> which indicates that no actual
         * value has been specified for the property in question.
         */
        public static final int EXPIRE_NOT_SPECIFIED = -1;

        /**
         * The XML name of the <tt>host</tt> attribute of a <tt>channel</tt> of
         * a <tt>content</tt> of a <tt>conference</tt> IQ which represents the
         * value of the <tt>host</tt> property of
         * <tt>CobriConferenceIQ.Channel</tt>.
         */
        public static final String HOST_ATTR_NAME = "host";

        /**
         * The XML name of the <tt>id</tt> attribute of a <tt>channel</tt> of a
         * <tt>content</tt> of a <tt>conference</tt> IQ which represents the
         * value of the <tt>id</tt> property of
         * <tt>CobriConferenceIQ.Channel</tt>.
         */
        public static final String ID_ATTR_NAME = "id";

        /**
         * The XML name of the <tt>rtcpport</tt> attribute of a <tt>channel</tt>
         * of a <tt>content</tt> of a <tt>conference</tt> IQ which represents
         * the value of the <tt>rtcpPort</tt> property of
         * <tt>CobriConferenceIQ.Channel</tt>.
         */
        public static final String RTCP_PORT_ATTR_NAME = "rtcpport";

        /**
         * The XML name of the <tt>rtpport</tt> attribute of a <tt>channel</tt>
         * of a <tt>content</tt> of a <tt>conference</tt> IQ which represents
         * the value of the <tt>rtpPort</tt> property of
         * <tt>CobriConferenceIQ.Channel</tt>.
         */
        public static final String RTP_PORT_ATTR_NAME = "rtpport";

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

            return
                payloadTypes.contains(payloadType)
                    ? false
                    : payloadTypes.add(payloadType);
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

        public List<PayloadTypePacketExtension> getPayloadTypes()
        {
            return Collections.unmodifiableList(payloadTypes);
        }

        public int getRTCPPort()
        {
            return rtcpPort;
        }

        public int getRTPPort()
        {
            return rtpPort;
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

        public void setRTCPPort(int rtcpPort)
        {
            this.rtcpPort = rtcpPort;
        }

        public void setRTPPort(int rtpPort)
        {
            this.rtpPort = rtpPort;
        }

        public void toXML(StringBuilder xml)
        {
            xml.append('<').append(ELEMENT_NAME);

            String id = getID();

            if (id != null)
                xml.append(' ').append(ID_ATTR_NAME).append("='").append(id)
                        .append('\'');

            String host = getHost();

            if (host != null)
                xml.append(' ').append(HOST_ATTR_NAME).append("='").append(host)
                        .append('\'');

            int rtpPort = getRTPPort();

            if (rtpPort > 0)
                xml.append(' ').append(RTP_PORT_ATTR_NAME).append("='")
                        .append(rtpPort).append('\'');

            int rtcpPort = getRTCPPort();

            if (rtcpPort > 0)
                xml.append(' ').append(RTCP_PORT_ATTR_NAME).append("='")
                        .append(rtcpPort).append('\'');

            int expire = getExpire();

            if (expire >= 0)
                xml.append(' ').append(EXPIRE_ATTR_NAME).append("='")
                        .append(expire).append('\'');

            List<PayloadTypePacketExtension> payloadTypes = getPayloadTypes();

            if (payloadTypes.size() == 0)
            {
                xml.append(" />");
            }
            else
            {
                xml.append('>');
                for (PayloadTypePacketExtension payloadType : payloadTypes)
                    xml.append(payloadType.toXML());
                xml.append("</").append(ELEMENT_NAME).append('>');
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
         * of <tt>CobriConferenceIQ.Content</tt>.
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

        public boolean addChannel(Channel channel)
        {
            if (channel == null)
                throw new NullPointerException("channel");

            return channels.contains(channel) ? false : channels.add(channel);
        }

        public Channel getChannel(int channelIndex)
        {
            return getChannels().get(channelIndex);
        }

        public Channel getChannel(String channelID)
        {
            for (Channel channel : getChannels())
                if (channelID.equals(channel.getID()))
                    return channel;
            return null;
        }

        public int getChannelCount()
        {
            return getChannels().size();
        }

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
