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

import net.java.sip.communicator.impl.protocol.jabber.extensions.*;

import net.java.sip.communicator.impl.protocol.jabber.extensions.colibri.*;
import net.java.sip.communicator.impl.protocol.jabber.extensions.condesc.*;
import net.java.sip.communicator.impl.protocol.jabber.extensions.jitsimeet.*;
import org.jivesoftware.smack.provider.*;
import org.jxmpp.jid.*;
import org.jxmpp.jid.impl.*;
import org.xmlpull.v1.*;

/**
 * An implementation of a Jingle IQ provider that parses incoming Jingle IQs.
 *
 * @author Emil Ivov
 */
public class JingleIQProvider extends IQProvider<JingleIQ>
{
    /**
     * Creates a new instance of the <tt>JingleIQProvider</tt> and register all
     * jingle related extension providers. It is the responsibility of the
     * application to register the <tt>JingleIQProvider</tt> itself.
     */
    public JingleIQProvider()
    {
        //<description/> provider
        ProviderManager.addExtensionProvider(
                RtpDescriptionPacketExtension.ELEMENT_NAME,
                RtpDescriptionPacketExtension.NAMESPACE,
                new DefaultPacketExtensionProvider
                        <RtpDescriptionPacketExtension>(
                        RtpDescriptionPacketExtension.class));

        //<payload-type/> provider
        ProviderManager.addExtensionProvider(
                PayloadTypePacketExtension.ELEMENT_NAME,
                RtpDescriptionPacketExtension.NAMESPACE,
                new DefaultPacketExtensionProvider
                        <PayloadTypePacketExtension>(
                        PayloadTypePacketExtension.class));

        //<parameter/> provider
        ProviderManager.addExtensionProvider(
                ParameterPacketExtension.ELEMENT_NAME,
                RtpDescriptionPacketExtension.NAMESPACE,
                new DefaultPacketExtensionProvider
                        <ParameterPacketExtension>
                        (ParameterPacketExtension.class));

        //<rtp-hdrext/> provider
        ProviderManager.addExtensionProvider(
                RTPHdrExtPacketExtension.ELEMENT_NAME,
                RTPHdrExtPacketExtension.NAMESPACE,
                new DefaultPacketExtensionProvider
                        <RTPHdrExtPacketExtension>
                        (RTPHdrExtPacketExtension.class));

        // <sctpmap/> provider
        ProviderManager.addExtensionProvider(
                SctpMapExtension.ELEMENT_NAME,
                SctpMapExtension.NAMESPACE,
                new SctpMapExtensionProvider());

        //<encryption/> provider
        ProviderManager.addExtensionProvider(
                EncryptionPacketExtension.ELEMENT_NAME,
                RtpDescriptionPacketExtension.NAMESPACE,
                new DefaultPacketExtensionProvider
                        <EncryptionPacketExtension>
                        (EncryptionPacketExtension.class));

        //<zrtp-hash/> provider
        ProviderManager.addExtensionProvider(
                ZrtpHashPacketExtension.ELEMENT_NAME,
                ZrtpHashPacketExtension.NAMESPACE,
                new DefaultPacketExtensionProvider
                        <ZrtpHashPacketExtension>
                        (ZrtpHashPacketExtension.class));

        //<crypto/> provider
        ProviderManager.addExtensionProvider(
                CryptoPacketExtension.ELEMENT_NAME,
                RtpDescriptionPacketExtension.NAMESPACE,
                new DefaultPacketExtensionProvider
                        <CryptoPacketExtension>
                        (CryptoPacketExtension.class));

        // <bundle/> provider
        ProviderManager.addExtensionProvider(
                BundlePacketExtension.ELEMENT_NAME,
                BundlePacketExtension.NAMESPACE,
                new DefaultPacketExtensionProvider
                        <BundlePacketExtension>
                        (BundlePacketExtension.class));

        // <group/> provider
        ProviderManager.addExtensionProvider(
                GroupPacketExtension.ELEMENT_NAME,
                GroupPacketExtension.NAMESPACE,
                new DefaultPacketExtensionProvider
                        <GroupPacketExtension>(GroupPacketExtension.class));

        //ice-udp transport
        ProviderManager.addExtensionProvider(
                IceUdpTransportPacketExtension.ELEMENT_NAME,
                IceUdpTransportPacketExtension.NAMESPACE,
                new DefaultPacketExtensionProvider
                        <IceUdpTransportPacketExtension>(
                        IceUdpTransportPacketExtension.class));

        //<raw-udp/> provider
        ProviderManager.addExtensionProvider(
                RawUdpTransportPacketExtension.ELEMENT_NAME,
                RawUdpTransportPacketExtension.NAMESPACE,
                new DefaultPacketExtensionProvider
                        <RawUdpTransportPacketExtension>(
                        RawUdpTransportPacketExtension.class));

        //ice-udp <candidate/> provider
        ProviderManager.addExtensionProvider(
                CandidatePacketExtension.ELEMENT_NAME,
                IceUdpTransportPacketExtension.NAMESPACE,
                new DefaultPacketExtensionProvider
                        <CandidatePacketExtension>(
                        CandidatePacketExtension.class));

        //raw-udp <candidate/> provider
        ProviderManager.addExtensionProvider(
                CandidatePacketExtension.ELEMENT_NAME,
                RawUdpTransportPacketExtension.NAMESPACE,
                new DefaultPacketExtensionProvider
                        <CandidatePacketExtension>(
                        CandidatePacketExtension.class));

        //ice-udp <remote-candidate/> provider
        ProviderManager.addExtensionProvider(
                RemoteCandidatePacketExtension.ELEMENT_NAME,
                IceUdpTransportPacketExtension.NAMESPACE,
                new DefaultPacketExtensionProvider
                        <RemoteCandidatePacketExtension>(
                        RemoteCandidatePacketExtension.class));

        //inputevt <inputevt/> provider
        ProviderManager.addExtensionProvider(
                InputEvtPacketExtension.ELEMENT_NAME,
                InputEvtPacketExtension.NAMESPACE,
                new DefaultPacketExtensionProvider<InputEvtPacketExtension>(
                        InputEvtPacketExtension.class));

        //coin <conference-info/> provider
        ProviderManager.addExtensionProvider(
                CoinPacketExtension.ELEMENT_NAME,
                CoinPacketExtension.NAMESPACE,
                new DefaultPacketExtensionProvider<CoinPacketExtension>(
                        CoinPacketExtension.class));

        // DTLS-SRTP
        ProviderManager.addExtensionProvider(
                DtlsFingerprintPacketExtension.ELEMENT_NAME,
                DtlsFingerprintPacketExtension.NAMESPACE,
                new DefaultPacketExtensionProvider
                        <DtlsFingerprintPacketExtension>(
                        DtlsFingerprintPacketExtension.class));

        /*
         * XEP-0251: Jingle Session Transfer <transfer/> and <transferred>
         * providers
         */
        ProviderManager.addExtensionProvider(
                TransferPacketExtension.ELEMENT_NAME,
                TransferPacketExtension.NAMESPACE,
                new DefaultPacketExtensionProvider<TransferPacketExtension>(
                        TransferPacketExtension.class));
        ProviderManager.addExtensionProvider(
                TransferredPacketExtension.ELEMENT_NAME,
                TransferredPacketExtension.NAMESPACE,
                new DefaultPacketExtensionProvider<TransferredPacketExtension>(
                        TransferredPacketExtension.class));

        //conference description <callid/> provider
        ProviderManager.addExtensionProvider(
                CallIdExtension.ELEMENT_NAME,
                ConferenceDescriptionExtension.NAMESPACE,
                new DefaultPacketExtensionProvider<CallIdExtension>(
                        CallIdExtension.class));

        //rtcp-fb
        ProviderManager.addExtensionProvider(
                RtcpFbPacketExtension.ELEMENT_NAME,
                RtcpFbPacketExtension.NAMESPACE,
                new DefaultPacketExtensionProvider<RtcpFbPacketExtension>(
                        RtcpFbPacketExtension.class));

        //rtcp-mux
        ProviderManager.addExtensionProvider(
                RtcpmuxPacketExtension.ELEMENT_NAME,
                IceUdpTransportPacketExtension.NAMESPACE,
                new DefaultPacketExtensionProvider<RtcpmuxPacketExtension>(
                        RtcpmuxPacketExtension.class));

        //web-socket
        ProviderManager.addExtensionProvider(
            WebSocketPacketExtension.ELEMENT_NAME,
            WebSocketPacketExtension.NAMESPACE,
            new DefaultPacketExtensionProvider<>(
                WebSocketPacketExtension.class));

        //ssrcInfo
        ProviderManager.addExtensionProvider(
                SSRCInfoPacketExtension.ELEMENT_NAME,
                SSRCInfoPacketExtension.NAMESPACE,
                new DefaultPacketExtensionProvider<SSRCInfoPacketExtension>(
                        SSRCInfoPacketExtension.class));
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
    @Override
    public JingleIQ parse(XmlPullParser parser, int depth)
        throws Exception
    {
        //let's first handle the "jingle" element params.
        JingleAction action = JingleAction.parseString(parser
                        .getAttributeValue("", JingleIQ.ACTION_ATTR_NAME));
        String initiator = parser
                         .getAttributeValue("", JingleIQ.INITIATOR_ATTR_NAME);
        String responder = parser
                        .getAttributeValue("", JingleIQ.RESPONDER_ATTR_NAME);
        String sid = parser
                        .getAttributeValue("", JingleIQ.SID_ATTR_NAME);

        JingleIQ jingleIQ = new JingleIQ(action, sid);
        if (initiator != null)
        {
            Jid initiatorJid = JidCreate.from(initiator);
            jingleIQ.setInitiator(initiatorJid);
        }

        if (responder != null)
        {
            Jid responderJid = JidCreate.from(responder);
            jingleIQ.setResponder(responderJid);
        }

        boolean done = false;

        // Sub-elements providers
        DefaultPacketExtensionProvider<ContentPacketExtension> contentProvider
            = new DefaultPacketExtensionProvider<ContentPacketExtension>(
                    ContentPacketExtension.class);
        ReasonProvider reasonProvider = new ReasonProvider();
        DefaultPacketExtensionProvider<TransferPacketExtension> transferProvider
            = new DefaultPacketExtensionProvider<TransferPacketExtension>(
                    TransferPacketExtension.class);
        DefaultPacketExtensionProvider<CoinPacketExtension> coinProvider
            = new DefaultPacketExtensionProvider<CoinPacketExtension>(
                    CoinPacketExtension.class);
        DefaultPacketExtensionProvider<CallIdExtension> callidProvider
            = new DefaultPacketExtensionProvider<CallIdExtension>(
                    CallIdExtension.class);

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
                        = contentProvider.parse(parser);
                    jingleIQ.addContent(content);
                }
                // <reason/>
                else if(elementName.equals(ReasonPacketExtension.ELEMENT_NAME))
                {
                    ReasonPacketExtension reason
                        = reasonProvider.parse(parser);
                    jingleIQ.setReason(reason);
                }
                // <transfer/>
                else if (elementName.equals(
                                TransferPacketExtension.ELEMENT_NAME)
                        && namespace.equals(TransferPacketExtension.NAMESPACE))
                {
                    jingleIQ.addExtension(transferProvider.parse(parser));
                }
                // <conference-info/>
                else if(elementName.equals(CoinPacketExtension.ELEMENT_NAME))
                {
                    jingleIQ.addExtension(coinProvider.parse(parser));
                }
                else if (elementName.equals(
                        CallIdExtension.ELEMENT_NAME))
                {
                    jingleIQ.addExtension(callidProvider.parse(parser));
                }
                else if (elementName.equals(
                        GroupPacketExtension.ELEMENT_NAME))
                {
                    jingleIQ.addExtension(
                        GroupPacketExtension.parseExtension(parser));
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

            if ((eventType == XmlPullParser.END_TAG)
                    && parser.getName().equals(JingleIQ.ELEMENT_NAME))
            {
                    done = true;
            }
        }
        return jingleIQ;
    }
}
