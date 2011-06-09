/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.gtalk;

import net.java.sip.communicator.impl.protocol.jabber.extensions.*;
import net.java.sip.communicator.impl.protocol.jabber.extensions.jingle.*;

import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.provider.*;
import org.xmlpull.v1.*;

/**
 * An implementation of a GTalk session IQ provider that parses incoming session
 * IQs.
 *
 * @author Sebastien Vincent
 */
public class SessionIQProvider
    implements IQProvider
{
    /**
     * Namespace for "audio" description.
     */
    public static final String GTALK_AUDIO_NAMESPACE =
        "http://www.google.com/session/phone";

    /**
     * Namespace for "video" description.
     */
    public static final String GTALK_VIDEO_NAMESPACE =
        "http://www.google.com/session/video";

    /**
     * Creates a new instance of the <tt>SessionIQProvider</tt> and register all
     * GTalk related extension providers. It is the responsibility of the
     * application to register the <tt>SessionIQProvider</tt> itself.
     */
    public SessionIQProvider()
    {
        ProviderManager providerManager = ProviderManager.getInstance();

        providerManager.addExtensionProvider(
            RtpDescriptionPacketExtension.ELEMENT_NAME,
            GTALK_AUDIO_NAMESPACE,
            new DefaultPacketExtensionProvider
                <RtpDescriptionPacketExtension>(
                                RtpDescriptionPacketExtension.class));
        providerManager.addExtensionProvider(
            RtpDescriptionPacketExtension.ELEMENT_NAME,
            GTALK_VIDEO_NAMESPACE,
            new DefaultPacketExtensionProvider
                <RtpDescriptionPacketExtension>(
                                RtpDescriptionPacketExtension.class));

        providerManager.addExtensionProvider(
                PayloadTypePacketExtension.ELEMENT_NAME,
                GTALK_AUDIO_NAMESPACE,
                new DefaultPacketExtensionProvider
                    <PayloadTypePacketExtension>(
                            PayloadTypePacketExtension.class));

        providerManager.addExtensionProvider(
                PayloadTypePacketExtension.ELEMENT_NAME,
                GTALK_VIDEO_NAMESPACE,
                new DefaultPacketExtensionProvider
                    <PayloadTypePacketExtension>(
                            PayloadTypePacketExtension.class));

        providerManager.addExtensionProvider(
                EncryptionPacketExtension.ELEMENT_NAME,
                null,
                new DefaultPacketExtensionProvider<EncryptionPacketExtension>(
                        EncryptionPacketExtension.class));

        providerManager.addExtensionProvider(
                SrcIdPacketExtension.ELEMENT_NAME,
                GTALK_AUDIO_NAMESPACE,
                new SrcIdProvider());

        providerManager.addExtensionProvider(
                SrcIdPacketExtension.ELEMENT_NAME,
                GTALK_VIDEO_NAMESPACE,
                new SrcIdProvider());

        providerManager.addExtensionProvider(
                UsagePacketExtension.ELEMENT_NAME,
                null,
                new DefaultPacketExtensionProvider<UsagePacketExtension>(
                UsagePacketExtension.class));
    }

    /**
     * Parses a GTalk session IQ sub-document and returns a {@link SessionIQ}
     * instance.
     *
     * @param parser an XML parser.
     * @return a new {@link SessionIQ} instance.
     * @throws Exception if an error occurs parsing the XML.
     */
    public IQ parseIQ(XmlPullParser parser) throws Exception
    {
        SessionIQ sessionIQ = new SessionIQ();
        boolean done = false;

        //let's first handle the "session" element params.
        GTalkType type = GTalkType.parseString(parser
                        .getAttributeValue("", SessionIQ.TYPE_ATTR_NAME));
        String initiator = parser
                         .getAttributeValue("", SessionIQ.INITIATOR_ATTR_NAME);
        String id = parser
                        .getAttributeValue("", SessionIQ.ID_ATTR_NAME);

        sessionIQ.setGTalkType(type);
        sessionIQ.setInitiator(initiator);
        sessionIQ.setID(id);

        // Sub-elements providers
        DefaultPacketExtensionProvider<GTalkCandidatePacketExtension>
            candidateProvider
                = new DefaultPacketExtensionProvider<
                    GTalkCandidatePacketExtension>(
                            GTalkCandidatePacketExtension.class);

        DefaultPacketExtensionProvider<RtpDescriptionPacketExtension>
            descriptionProvider
                = new
                DefaultPacketExtensionProvider<RtpDescriptionPacketExtension>(
                    RtpDescriptionPacketExtension.class);

        ReasonProvider reasonProvider = new ReasonProvider();

        //int nbTab = 0;

        // Now go on and parse the session element's content.
        while (!done)
        {
            int eventType = parser.next();
            String elementName = parser.getName();

            if (eventType == XmlPullParser.START_TAG)
            {
                /*
                String namespace = parser.getNamespace();;
                String tab = "";
                nbTab++;

                for(int ii = 0 ; ii < nbTab ; ii++)
                    tab += "\t";

                System.out.println(tab + parser.getName() + " " +
                        parser.getNamespace());

                for(int i = 0 ; i < parser.getAttributeCount() ; i++)
                {

                    System.out.print(tab + "\t");
                    System.out.println(parser.getAttributeName(i) + "=" +
                            parser.getAttributeValue(i));
                }
                */
                if(elementName.equals(CandidatePacketExtension.ELEMENT_NAME))
                {
                    sessionIQ.addExtension(
                            candidateProvider.parseExtension(parser));
                }
                else if(elementName.equals(
                        RtpDescriptionPacketExtension.ELEMENT_NAME))
                {
                    RtpDescriptionPacketExtension ext =
                        descriptionProvider.parseExtension(parser);
                    sessionIQ.addExtension(ext);
                }
                else if(elementName.equals(ReasonPacketExtension.ELEMENT_NAME))
                {
                    ReasonPacketExtension reason
                        = reasonProvider.parseExtension(parser);
                    sessionIQ.setReason(reason);
                }
            }
            if (eventType == XmlPullParser.END_TAG)
            {
                //nbTab--;
                if (parser.getName().equals(SessionIQ.ELEMENT_NAME))
                {
                    done = true;
                }
            }
        }

        return sessionIQ;
    }
}
