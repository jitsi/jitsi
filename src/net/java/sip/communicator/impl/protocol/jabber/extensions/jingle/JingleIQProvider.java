/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.jingle;
import java.util.logging.*;

import net.java.sip.communicator.impl.protocol.jabber.extensions.*;

import org.jivesoftware.smack.provider.*;
import org.xmlpull.v1.XmlPullParser;


/**
 * An implementation of a Jingle IQ provider that parses incoming Jingle IQs.
 *
 * @author Emil Ivov
 */
public class JingleIQProvider implements IQProvider
{
    /**
     * The <tt>Logger</tt> used by the <tt>JingleIQProvider</tt>
     * class and its instances for logging output.
     */
    private static final Logger logger = Logger
                    .getLogger(JingleIQProvider.class.getName());

    /**
     * Creates a new instance of the <tt>JingleIQProvider</tt> and register all
     * jingle related extension providers. It is the responsibility of the
     * application to register the <tt>JingleIQProvider</tt> itself.
     */
    public JingleIQProvider()
    {
        ProviderManager providerManager = ProviderManager.getInstance();

        //<description/> provider
        providerManager.addExtensionProvider(
            RtpDescriptionPacketExtension.ELEMENT_NAME,
            RtpDescriptionPacketExtension.NAMESPACE,
            new DefaultPacketExtensionProvider
                <RtpDescriptionPacketExtension>(
                                RtpDescriptionPacketExtension.class));

        //<payload-type/> provider
        providerManager.addExtensionProvider(
            PayloadTypePacketExtension.ELEMENT_NAME,
            RtpDescriptionPacketExtension.NAMESPACE,
            new DefaultPacketExtensionProvider
                <PayloadTypePacketExtension>(
                                PayloadTypePacketExtension.class));

        //<parameter/> provider
        providerManager.addExtensionProvider(
            ParameterPacketExtension.ELEMENT_NAME,
            RtpDescriptionPacketExtension.NAMESPACE,
            new DefaultPacketExtensionProvider
                <ParameterPacketExtension>(ParameterPacketExtension.class));

        //<extmap/> provider
        providerManager.addExtensionProvider(
            ExtmapPacketExtension.ELEMENT_NAME,
            RtpDescriptionPacketExtension.NAMESPACE,
            new DefaultPacketExtensionProvider
                <ExtmapPacketExtension>(ExtmapPacketExtension.class));

        //ice-udp transport
        providerManager.addExtensionProvider(
            IceUdpTransportPacketExtension.ELEMENT_NAME,
            IceUdpTransportPacketExtension.NAMESPACE,
            new DefaultPacketExtensionProvider<IceUdpTransportPacketExtension>(
                            IceUdpTransportPacketExtension.class));

        //<raw-udp/> provider
        providerManager.addExtensionProvider(
            RawUdpTransportPacketExtension.ELEMENT_NAME,
            RawUdpTransportPacketExtension.NAMESPACE,
            new DefaultPacketExtensionProvider<RawUdpTransportPacketExtension>(
                            RawUdpTransportPacketExtension.class));

        //ice-udp <candidate/> provider
        providerManager.addExtensionProvider(
            CandidatePacketExtension.ELEMENT_NAME,
            IceUdpTransportPacketExtension.NAMESPACE,
            new DefaultPacketExtensionProvider<CandidatePacketExtension>(
                            CandidatePacketExtension.class));

        //raw-udp <candidate/> provider
        providerManager.addExtensionProvider(
            CandidatePacketExtension.ELEMENT_NAME,
            RawUdpTransportPacketExtension.NAMESPACE,
            new DefaultPacketExtensionProvider<CandidatePacketExtension>(
                            CandidatePacketExtension.class));

        //ice-udp <remote-candidate/> provider
        providerManager.addExtensionProvider(
            RemoteCandidatePacketExtension.ELEMENT_NAME,
            IceUdpTransportPacketExtension.NAMESPACE,
            new DefaultPacketExtensionProvider<RemoteCandidatePacketExtension>(
                            RemoteCandidatePacketExtension.class));
    }

    /**
     * Parses a Jingle IQ sub-document and returns a {@link JingleIQ} instance.
     *
     * @param parser an XML parser.
     *
     * @return a new {@link JingleIQ} instance.
     *
     * @throws Exception if an error occurs parsing the XML.
     */
    public JingleIQ parseIQ(XmlPullParser parser)
        throws Exception
    {
        JingleIQ jingleIQ = new JingleIQ();

        //let's first handle the "jingle" element params.
        JingleAction action = JingleAction.parseString(parser
                        .getAttributeValue("", JingleIQ.ACTION_ATTR_NAME));
        String initiator = parser
                         .getAttributeValue("", JingleIQ.INITIATOR_ATTR_NAME);
        String responder = parser
                        .getAttributeValue("", JingleIQ.RESPONDER_ATTR_NAME);
        String sid = parser
                        .getAttributeValue("", JingleIQ.SID_ATTR_NAME);

        jingleIQ.setAction(action);
        jingleIQ.setInitiator(initiator);
        jingleIQ.setResponder(responder);
        jingleIQ.setSID(sid);

        boolean done = false;

        // Sub-elements providers
        DefaultPacketExtensionProvider<ContentPacketExtension> contentProvider
            = new DefaultPacketExtensionProvider
                <ContentPacketExtension>(ContentPacketExtension.class);
        ReasonProvider reasonProvider = new ReasonProvider();

        // Now go on and parse the jingle element's content.
        int eventType;
        String elementName;
        String namespace;

        while (!done)
        {
            eventType = parser.next();
            elementName = parser.getName();
            namespace = parser.getNamespace();

            if (eventType == XmlPullParser.START_TAG)
            {
                // <content/>
                if (elementName.equals(ContentPacketExtension.ELEMENT_NAME))
                {
                    ContentPacketExtension content
                        = contentProvider.parseExtension(parser);
                    jingleIQ.addContent(content);
                }
                // <reason/>
                if (elementName.equals(ReasonPacketExtension.ELEMENT_NAME))
                {
                    ReasonPacketExtension reason
                        = reasonProvider.parseExtension(parser);
                    jingleIQ.setReason(reason);
                }

                //<mute/> <active/> and other session-info elements
                if (namespace.equals( SessionInfoPacketExtension.NAMESPACE))
                {
                    SessionInfoType type = SessionInfoType.valueOf(elementName);

                    //<mute/>
                    if( type == SessionInfoType.mute
                        || type == SessionInfoType.unmute)
                    {
                        String name = parser.getAttributeValue("",
                                MuteSessionInfoPacketExtension.NAME_ATTR_VALUE);

                        jingleIQ.setSessionInfo(
                                new MuteSessionInfoPacketExtension(
                                        type == SessionInfoType.mute, name));
                    }
                    //<hold/>, <unhold/>, <active/>, etc.
                    else
                    {
                        jingleIQ.setSessionInfo(
                                        new SessionInfoPacketExtension(type));
                    }

                }
            }

            if (eventType == XmlPullParser.END_TAG)
            {
                if (parser.getName().equals(JingleIQ.ELEMENT_NAME))
                {
                    done = true;
                }
            }
        }
        return jingleIQ;
    }
}
