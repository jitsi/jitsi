/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.cobri;

import net.java.sip.communicator.impl.protocol.jabber.extensions.jingle.*;

import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.provider.*;
import org.xmlpull.v1.*;

/**
 * Implements an <tt>org.jivesoftware.smack.provider.IQProvider</tt> for the
 * Jitsi VideoBridge extension <tt>CobriConferenceIQ</tt>.
 *
 * @author Lyubomir Marinov
 */
public class CobriIQProvider
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

        if (CobriConferenceIQ.ELEMENT_NAME.equals(parser.getName())
                && CobriConferenceIQ.NAMESPACE.equals(namespace))
        {
            CobriConferenceIQ conference = new CobriConferenceIQ();
            String conferenceID
                = parser.getAttributeValue("", CobriConferenceIQ.ID_ATTR_NAME);

            if ((conferenceID != null) && (conferenceID.length() != 0))
                conference.setID(conferenceID);

            boolean done = false;
            CobriConferenceIQ.Channel channel = null;
            CobriConferenceIQ.Content content = null;
            PacketExtensionProvider payloadTypePacketExtensionProvider = null;

            while (!done)
            {
                switch (parser.next())
                {
                case XmlPullParser.END_TAG:
                {
                    String name = parser.getName();

                    if (CobriConferenceIQ.ELEMENT_NAME.equals(name))
                    {
                        done = true;
                    }
                    else if (CobriConferenceIQ.Channel.ELEMENT_NAME
                            .equals(name))
                    {
                        content.addChannel(channel);
                        channel = null;
                    }
                    else if (CobriConferenceIQ.Content.ELEMENT_NAME
                            .equals(name))
                    {
                        conference.addContent(content);
                        content = null;
                    }
                    break;
                }

                case XmlPullParser.START_TAG:
                {
                    String name = parser.getName();

                    if (CobriConferenceIQ.Channel.ELEMENT_NAME.equals(name))
                    {
                        channel = new CobriConferenceIQ.Channel();

                        String channelID
                            = parser.getAttributeValue(
                                    "",
                                    CobriConferenceIQ.Channel.ID_ATTR_NAME);

                        if ((channelID != null) && (channelID.length() != 0))
                            channel.setID(channelID);

                        String host
                            = parser.getAttributeValue(
                                    "",
                                    CobriConferenceIQ.Channel.HOST_ATTR_NAME);

                        if ((host != null) && (host.length() != 0))
                            channel.setHost(host);

                        String rtpPort
                            = parser.getAttributeValue(
                                    "",
                                    CobriConferenceIQ.Channel
                                            .RTP_PORT_ATTR_NAME);

                        if ((rtpPort != null) && (rtpPort.length() != 0))
                            channel.setRTPPort(Integer.parseInt(rtpPort));

                        String rtcpPort
                            = parser.getAttributeValue(
                                    "",
                                    CobriConferenceIQ.Channel
                                            .RTCP_PORT_ATTR_NAME);

                        if ((rtcpPort != null) && (rtcpPort.length() != 0))
                            channel.setRTCPPort(Integer.parseInt(rtcpPort));

                    }
                    else if (CobriConferenceIQ.Content.ELEMENT_NAME
                            .equals(name))
                    {
                        content = new CobriConferenceIQ.Content();

                        String contentName
                            = parser.getAttributeValue(
                                    "",
                                    CobriConferenceIQ.Content.NAME_ATTR_NAME);

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
                                channel.addPayloadType(payloadType);
                        }
                    }
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
