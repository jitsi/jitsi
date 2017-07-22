package net.java.sip.communicator.impl.protocol.jabber.extensions.condesc;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.xmlpull.v1.XmlPullParser;

/**
 * Parses elements with the {@value ConferenceDescriptionExtension#NAMESPACE}
 * namespace.
 */
public class ConferenceDescriptionExtensionProvider
    extends ExtensionElementProvider<ConferenceDescriptionExtension>
{
    /**
     * Creates a <tt>ConferenceDescriptionPacketExtension</tt> by parsing
     * an XML document.
     * @param parser the parser to use.
     * @return the created <tt>ConferenceDescriptionPacketExtension</tt>.
     * @throws Exception
     */
    @Override
    public ConferenceDescriptionExtension parse(XmlPullParser parser, int depth)
            throws Exception
    {
        ConferenceDescriptionExtension packetExtension
                = new ConferenceDescriptionExtension();

        //first, set all attributes
        int attrCount = parser.getAttributeCount();

        for (int i = 0; i < attrCount; i++)
        {
            packetExtension.setAttribute(
                    parser.getAttributeName(i),
                    parser.getAttributeValue(i));
        }

        //now parse the sub elements
        boolean done = false;
        String elementName;
        TransportExtension transportExt = null;

        while (!done)
        {
            switch (parser.next())
            {
                case XmlPullParser.START_TAG:
                    elementName = parser.getName();
                    if (TransportExtension.ELEMENT_NAME.equals(elementName))
                    {
                        String transportNs = parser.getNamespace();
                        if (transportNs != null)
                        {
                            transportExt = new TransportExtension(transportNs);
                        }
                    }

                    break;

                case XmlPullParser.END_TAG:
                    switch (parser.getName())
                    {
                        case ConferenceDescriptionExtension.ELEMENT_NAME:
                            done = true;
                            break;

                        case TransportExtension.ELEMENT_NAME:
                            if (transportExt != null)
                            {
                                packetExtension.addChildExtension(transportExt);
                            }
                            break;
                    }
            }
        }

        return packetExtension;
    }
}