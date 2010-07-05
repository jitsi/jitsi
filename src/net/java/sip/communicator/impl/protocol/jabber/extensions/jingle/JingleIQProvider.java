/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.jingle;
import net.java.sip.communicator.util.*;

import org.jivesoftware.smack.*;
import org.jivesoftware.smack.packet.*;
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
     * Parses a Jingle IQ sub-document and returns a {@link JingleIQ} instance.
     *
     * @param parser an XML parser.
     *
     * @return a new {@link JingleIQ} instance.
     *
     * @throws Exception if an error occurs parsing the XML.
     */
    public IQ parseIQ(XmlPullParser parser) throws Exception
    {
        JingleIQ jingleIQ = new JingleIQ();
        String sessionID = null;
        String initiator = null;
        String responder = null;
        boolean done = false;
        JingleContent currentContent = null;

        // Sub-elements providers
        JingleContentProvider jcp = new JingleContentProvider();
        JingleDescriptionProvider jdpAudio = new JingleDescriptionProvider.Audio();
        JingleTransportProvider jtpRawUdp = new JingleTransportProvider.RawUdp();
        JingleTransportProvider jtpIce = new JingleTransportProvider.Ice();
        JingleContentInfoProvider jmipAudio = new JingleContentInfoProvider.Audio();

        int eventType;
        String elementName;
        String namespace;

        // Get some attributes for the <jingle> element
        sessionID = parser.getAttributeValue("", "sid");
        action = JingleActionEnum.getAction(parser.getAttributeValue("", "action"));
        initiator = parser.getAttributeValue("", "initiator");
        responder = parser.getAttributeValue("", "responder");

        jingleIQ.setSid(sessionID);
        jingleIQ.setAction(action);
        jingleIQ.setInitiator(initiator);
        jingleIQ.setResponder(responder);

        // Start processing sub-elements
        while (!done) {
            eventType = parser.next();
            elementName = parser.getName();
            namespace = parser.getNamespace();

            if (eventType == XmlPullParser.START_TAG) {

                // Parse some well know subelements, depending on the namespaces
                // and element names...

                if (elementName.equals(JingleContent.NODENAME)) {
                    // Add a new <content> element to the jingle
                    currentContent = (JingleContent) jcp.parseExtension(parser);
                    jingleIQ.addContent(currentContent);
                } else if (elementName.equals(JingleDescription.NODENAME) && namespace.equals(JingleDescription.Audio.NAMESPACE)) {
                    // Set the <description> element of the <content>
                    currentContent.setDescription((JingleDescription) jdpAudio.parseExtension(parser));
                } else if (elementName.equals(JingleTransport.NODENAME)) {
                    // Add all of the <transport> elements to the <content> of the jingle

                    // Parse the possible transport namespaces
                    if (namespace.equals(JingleTransport.RawUdp.NAMESPACE)) {
                        currentContent.addJingleTransport((JingleTransport) jtpRawUdp.parseExtension(parser));
                    } else if (namespace.equals(JingleTransport.Ice.NAMESPACE)) {
                        currentContent.addJingleTransport((JingleTransport) jtpIce.parseExtension(parser));
                    } else {
                        throw new XMPPException("Unknown transport namespace \"" + namespace + "\" in Jingle packet.");
                    }
                } else if (namespace.equals(JingleContentInfo.Audio.NAMESPACE)) {
                    jingleIQ.setContentInfo((JingleContentInfo) jmipAudio.parseExtension(parser));
                } else {
                    throw new XMPPException("Unknown combination of namespace \"" + namespace + "\" and element name \""
                            + elementName + "\" in Jingle packet.");
                }

            } else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals(Jingle.getElementName())) {
                    done = true;
                }
            }
        }

        return jingleIQ;
    }
}
