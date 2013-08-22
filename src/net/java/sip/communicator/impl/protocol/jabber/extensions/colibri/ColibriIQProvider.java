/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.colibri;

import net.java.sip.communicator.impl.protocol.jabber.extensions.jingle.*;

import org.jitsi.service.neomedia.*;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.provider.*;
import org.xmlpull.v1.*;

/**
 * Implements an <tt>org.jivesoftware.smack.provider.IQProvider</tt> for the
 * Jitsi VideoBridge extension <tt>ColibriConferenceIQ</tt>.
 *
 * @author Lyubomir Marinov
 */
public class ColibriIQProvider
    implements IQProvider
{
    /**
     * Parses an IQ sub-document and creates an
     * <tt>org.jivesoftware.smack.packet.IQ</tt> instance.
     *
     * @param parser an <tt>XmlPullParser</tt> which specifies the IQ
     * sub-document to be parsed into a new <tt>IQ</tt> instance
     * @return a new <tt>IQ</tt> instance parsed from the specified IQ
     * sub-document
     * @see IQProvider#parseIQ(XmlPullParser)
     */
    public IQ parseIQ(XmlPullParser parser)
        throws Exception
    {
        String namespace = parser.getNamespace();
        IQ iq;

        if (ColibriConferenceIQ.ELEMENT_NAME.equals(parser.getName())
                && ColibriConferenceIQ.NAMESPACE.equals(namespace))
        {
            ColibriConferenceIQ conference = new ColibriConferenceIQ();
            String conferenceID = parser
                    .getAttributeValue("", ColibriConferenceIQ.ID_ATTR_NAME);

            if ((conferenceID != null) && (conferenceID.length() != 0))
                conference.setID(conferenceID);

            boolean done = false;
            ColibriConferenceIQ.Channel channel = null;
            ColibriConferenceIQ.Content content = null;
            PacketExtensionProvider payloadTypePacketExtensionProvider = null;
            StringBuilder ssrc = null;

            while (!done)
            {
                switch (parser.next())
                {
                case XmlPullParser.END_TAG:
                {
                    String name = parser.getName();

                    if (ColibriConferenceIQ.ELEMENT_NAME.equals(name))
                    {
                        done = true;
                    }
                    else if (ColibriConferenceIQ.Channel.ELEMENT_NAME.equals(
                            name))
                    {
                        content.addChannel(channel);
                        channel = null;
                    }
                    else if (ColibriConferenceIQ.Channel.SSRC_ELEMENT_NAME
                            .equals(name))
                    {
                        channel.addSSRC(Long.parseLong(ssrc.toString().trim()));
                        ssrc = null;
                    }
                    else if (ColibriConferenceIQ.Content.ELEMENT_NAME.equals(
                            name))
                    {
                        conference.addContent(content);
                        content = null;
                    }
                    break;
                }

                case XmlPullParser.START_TAG:
                {
                    String name = parser.getName();

                    if (ColibriConferenceIQ.Channel.ELEMENT_NAME.equals(name))
                    {
                        channel = new ColibriConferenceIQ.Channel();

                        String channelID
                            = parser.getAttributeValue(
                                    "",
                                    ColibriConferenceIQ.Channel.ID_ATTR_NAME);

                        if ((channelID != null) && (channelID.length() != 0))
                            channel.setID(channelID);

                        String host
                            = parser.getAttributeValue(
                                    "",
                                    ColibriConferenceIQ.Channel.HOST_ATTR_NAME);

                        if ((host != null) && (host.length() != 0))
                            channel.setHost(host);

                        String rtpPort
                            = parser.getAttributeValue(
                                    "",
                                    ColibriConferenceIQ.Channel
                                            .RTP_PORT_ATTR_NAME);

                        if ((rtpPort != null) && (rtpPort.length() != 0))
                            channel.setRTPPort(Integer.parseInt(rtpPort));

                        String rtcpPort
                            = parser.getAttributeValue(
                                    "",
                                    ColibriConferenceIQ.Channel
                                            .RTCP_PORT_ATTR_NAME);

                        if ((rtcpPort != null) && (rtcpPort.length() != 0))
                            channel.setRTCPPort(Integer.parseInt(rtcpPort));

                        String directionStr
                            = parser.getAttributeValue(
                                    "" ,
                                    ColibriConferenceIQ.Channel
                                            .DIRECTION_ATTR_NAME);
                        if (directionStr != null)
                            channel.setDirection(
                                    MediaDirection.parseString(directionStr));

                        String expire
                            = parser.getAttributeValue(
                                    "",
                                    ColibriConferenceIQ.Channel.EXPIRE_ATTR_NAME);

                        if ((expire != null) && (expire.length() != 0))
                            channel.setExpire(Integer.parseInt(expire));
                    }
                    else if (ColibriConferenceIQ.Channel.SSRC_ELEMENT_NAME
                            .equals(name))
                    {
                        ssrc = new StringBuilder();
                    }
                    else if (ColibriConferenceIQ.Content.ELEMENT_NAME.equals(
                            name))
                    {
                        content = new ColibriConferenceIQ.Content();

                        String contentName
                            = parser.getAttributeValue(
                                    "",
                                    ColibriConferenceIQ.Content.NAME_ATTR_NAME);

                        if ((contentName != null)
                                && (contentName.length() != 0))
                            content.setName(contentName);
                    }
                    else if (PayloadTypePacketExtension.ELEMENT_NAME.equals(
                            name))
                    {
                        /*
                         * The channel element of the Jitsi VideoBridge protocol
                         * reuses the payload-type element defined in XEP-0167:
                         * Jingle RTP Sessions.
                         */
                        if (payloadTypePacketExtensionProvider == null)
                        {
                            payloadTypePacketExtensionProvider
                                = (PacketExtensionProvider)
                                    ProviderManager.getInstance()
                                        .getExtensionProvider(name, namespace);
                        }
                        if (payloadTypePacketExtensionProvider == null)
                        {
                            /*
                             * Well, the PacketExtensionProvider appears to have
                             * not been registered. Throw away the payload-type
                             * element.
                             */
                            while ((XmlPullParser.END_TAG != parser.next())
                                    || !name.equals(parser.getName()));
                        }
                        else
                        {
                            PayloadTypePacketExtension payloadType
                                = (PayloadTypePacketExtension)
                                    payloadTypePacketExtensionProvider
                                        .parseExtension(parser);

                            if (payloadType != null)
                            {
                                if("opus".equals(payloadType.getName())
                                        && payloadType.getChannels() != 2)
                                {
                                    /*
                                     * We only have a Format for opus with 2
                                     * channels, because it MUST be advertised
                                     * with 2 channels.
                                     * Fixing the number of channels here allows
                                     * us to be compatible with agents who
                                     * advertise it with 1 channel.
                                     */
                                    payloadType.setChannels(2);
                                }
                                channel.addPayloadType(payloadType);
                            }
                        }
                    }
                    break;
                }

                case XmlPullParser.TEXT:
                {
                    if (ssrc != null)
                        ssrc.append(parser.getText());
                    break;
                }
                }
            }

            iq = conference;
        }
        else
            iq = null;

        return iq;
    }
}
