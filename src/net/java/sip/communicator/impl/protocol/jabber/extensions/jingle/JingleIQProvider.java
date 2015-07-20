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

import net.java.sip.communicator.impl.protocol.jabber.extensions.jitsimeet.*;
import org.jivesoftware.smack.provider.*;
import org.xmlpull.v1.*;

/**
 * An implementation of a Jingle IQ provider that parses incoming Jingle IQs.
 *
 * @author Emil Ivov
 */
public class JingleIQProvider implements IQProvider
{
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

        //<rtp-hdrext/> provider
        providerManager.addExtensionProvider(
            RTPHdrExtPacketExtension.ELEMENT_NAME,
            RTPHdrExtPacketExtension.NAMESPACE,
            new DefaultPacketExtensionProvider
                <RTPHdrExtPacketExtension>(RTPHdrExtPacketExtension.class));

        // <sctpmap/> provider
        providerManager.addExtensionProvider(
            SctpMapExtension.ELEMENT_NAME,
            SctpMapExtension.NAMESPACE,
            new SctpMapExtensionProvider());

        //<encryption/> provider
        providerManager.addExtensionProvider(
            EncryptionPacketExtension.ELEMENT_NAME,
            RtpDescriptionPacketExtension.NAMESPACE,
            new DefaultPacketExtensionProvider
                <EncryptionPacketExtension>(EncryptionPacketExtension.class));

        //<zrtp-hash/> provider
        providerManager.addExtensionProvider(
            ZrtpHashPacketExtension.ELEMENT_NAME,
            ZrtpHashPacketExtension.NAMESPACE,
            new DefaultPacketExtensionProvider
                <ZrtpHashPacketExtension>(ZrtpHashPacketExtension.class));

        //<crypto/> provider
        providerManager.addExtensionProvider(
            CryptoPacketExtension.ELEMENT_NAME,
            RtpDescriptionPacketExtension.NAMESPACE,
            new DefaultPacketExtensionProvider
                <CryptoPacketExtension>(CryptoPacketExtension.class));

        // <bundle/> provider
        providerManager.addExtensionProvider(
            BundlePacketExtension.ELEMENT_NAME,
            BundlePacketExtension.NAMESPACE,
            new DefaultPacketExtensionProvider
                <BundlePacketExtension>(BundlePacketExtension.class));

        // <group/> provider
        providerManager.addExtensionProvider(
            GroupPacketExtension.ELEMENT_NAME,
            GroupPacketExtension.NAMESPACE,
            new DefaultPacketExtensionProvider
                <GroupPacketExtension>(GroupPacketExtension.class));

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

        //inputevt <inputevt/> provider
        providerManager.addExtensionProvider(
                InputEvtPacketExtension.ELEMENT_NAME,
                InputEvtPacketExtension.NAMESPACE,
                new DefaultPacketExtensionProvider<InputEvtPacketExtension>(
                        InputEvtPacketExtension.class));

        //coin <conference-info/> provider
        providerManager.addExtensionProvider(
                CoinPacketExtension.ELEMENT_NAME,
                CoinPacketExtension.NAMESPACE,
                new DefaultPacketExtensionProvider<CoinPacketExtension>(
                        CoinPacketExtension.class));

        // DTLS-SRTP
        providerManager.addExtensionProvider(
                DtlsFingerprintPacketExtension.ELEMENT_NAME,
                DtlsFingerprintPacketExtension.NAMESPACE,
                new DefaultPacketExtensionProvider
                    <DtlsFingerprintPacketExtension>(
                        DtlsFingerprintPacketExtension.class));

        /*
         * XEP-0251: Jingle Session Transfer <transfer/> and <transferred>
         * providers
         */
        providerManager.addExtensionProvider(
                TransferPacketExtension.ELEMENT_NAME,
                TransferPacketExtension.NAMESPACE,
                new DefaultPacketExtensionProvider<TransferPacketExtension>(
                        TransferPacketExtension.class));
        providerManager.addExtensionProvider(
                TransferredPacketExtension.ELEMENT_NAME,
                TransferredPacketExtension.NAMESPACE,
                new DefaultPacketExtensionProvider<TransferredPacketExtension>(
                        TransferredPacketExtension.class));

        //conference description <callid/> provider
        providerManager.addExtensionProvider(
                ConferenceDescriptionPacketExtension.CALLID_ELEM_NAME,
                ConferenceDescriptionPacketExtension.NAMESPACE,
                new DefaultPacketExtensionProvider<CallIdPacketExtension>(
                        CallIdPacketExtension.class));

        //rtcp-fb
        providerManager.addExtensionProvider(
            RtcpFbPacketExtension.ELEMENT_NAME,
            RtcpFbPacketExtension.NAMESPACE,
            new DefaultPacketExtensionProvider<RtcpFbPacketExtension>(
                RtcpFbPacketExtension.class));

        //rtcp-mux
        providerManager.addExtensionProvider(
                RtcpmuxPacketExtension.ELEMENT_NAME,
                IceUdpTransportPacketExtension.NAMESPACE,
                new DefaultPacketExtensionProvider<RtcpmuxPacketExtension>(
                        RtcpmuxPacketExtension.class));

        //ssrcInfo
        providerManager.addExtensionProvider(
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
            = new DefaultPacketExtensionProvider<ContentPacketExtension>(
                    ContentPacketExtension.class);
        ReasonProvider reasonProvider = new ReasonProvider();
        DefaultPacketExtensionProvider<TransferPacketExtension> transferProvider
            = new DefaultPacketExtensionProvider<TransferPacketExtension>(
                    TransferPacketExtension.class);
        DefaultPacketExtensionProvider<CoinPacketExtension> coinProvider
            = new DefaultPacketExtensionProvider<CoinPacketExtension>(
                    CoinPacketExtension.class);
        DefaultPacketExtensionProvider<CallIdPacketExtension> callidProvider
            = new DefaultPacketExtensionProvider<CallIdPacketExtension>(
                    CallIdPacketExtension.class);

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
                else if(elementName.equals(ReasonPacketExtension.ELEMENT_NAME))
                {
                    ReasonPacketExtension reason
                        = reasonProvider.parseExtension(parser);
                    jingleIQ.setReason(reason);
                }
                // <transfer/>
                else if (elementName.equals(
                                TransferPacketExtension.ELEMENT_NAME)
                        && namespace.equals(TransferPacketExtension.NAMESPACE))
                {
                    jingleIQ.addExtension(
                            transferProvider.parseExtension(parser));
                }
                else if(elementName.equals(CoinPacketExtension.ELEMENT_NAME))
                {
                    jingleIQ.addExtension(coinProvider.parseExtension(parser));
                }
                else if (elementName.equals(
                        ConferenceDescriptionPacketExtension.CALLID_ELEM_NAME))
                {
                    jingleIQ.addExtension(callidProvider.parseExtension(parser));
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
