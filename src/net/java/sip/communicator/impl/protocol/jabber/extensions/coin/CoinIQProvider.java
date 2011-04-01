/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.coin;

import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.provider.*;
import org.xmlpull.v1.*;

/**
 * An implementation of a Coin IQ provider that parses incoming Coin IQs.
 *
 * @author Sebastien Vincent
 */
public class CoinIQProvider
    implements IQProvider
{
    /**
     * Provider for description packet extension.
     */
    private final PacketExtensionProvider descriptionProvider = new
        DescriptionProvider();

    /**
     * Provider for users packet extension.
     */
    private final PacketExtensionProvider usersProvider = new UsersProvider();

    /**
     * Provider for state packet extension.
     */
    private final StateProvider stateProvider = new StateProvider();

    /**
     * Constructor.
     */
    public CoinIQProvider()
    {
    }

    /**
     * Parse the Coin IQ sub-document and returns the corresponding
     * <tt>CoinIQ</tt>.
     *
     * @param parser XML parser
     * @return <tt>CoinIQ</tt>
     * @throws Exception if something goes wrong during parsing
     */
    public IQ parseIQ(XmlPullParser parser)
        throws Exception
    {
        CoinIQ coinIQ = new CoinIQ();

        String entity = parser
            .getAttributeValue("", CoinIQ.ENTITY_ATTR_NAME);
        String version = parser.getAttributeValue("", CoinIQ.VERSION_ATTR_NAME);
        StateType state = StateType.full;
        String stateStr = parser.getAttributeValue("",
                EndpointPacketExtension.STATE_ATTR_NAME);
        String sid = parser.getAttributeValue("", CoinIQ.SID_ATTR_NAME);

        if(stateStr != null)
        {
            state = StateType.parseString(stateStr);
        }

        coinIQ.setEntity(entity);
        coinIQ.setVersion(Integer.parseInt(version));
        coinIQ.setState(state);
        coinIQ.setSID(sid);

        // Now go on and parse the jingle element's content.
        int eventType;
        String elementName = null;
        boolean done = false;

        while (!done)
        {
            eventType = parser.next();
            elementName = parser.getName();

            if (eventType == XmlPullParser.START_TAG)
            {
                if(elementName.equals(DescriptionPacketExtension.ELEMENT_NAME))
                {
                    PacketExtension childExtension =
                        descriptionProvider.parseExtension(parser);
                    coinIQ.addExtension(childExtension);
                }
                else if(elementName.equals(UsersPacketExtension.ELEMENT_NAME))
                {
                    PacketExtension childExtension =
                        usersProvider.parseExtension(parser);
                    coinIQ.addExtension(childExtension);
                }
                else if(elementName.equals(StatePacketExtension.ELEMENT_NAME))
                {
                    PacketExtension childExtension =
                        stateProvider.parseExtension(parser);
                    coinIQ.addExtension(childExtension);
                }
            }

            if (eventType == XmlPullParser.END_TAG)
            {
                if (parser.getName().equals(CoinIQ.ELEMENT_NAME))
                {
                    done = true;
                }
            }
        }

        return coinIQ;
    }

    /**
     * Returns the content of the next {@link XmlPullParser#TEXT} element that
     * we encounter in <tt>parser</tt>.
     *
     * @param parser the parse that we'll be probing for text.
     *
     * @return the content of the next {@link XmlPullParser#TEXT} element we
     * come across or <tt>null</tt> if we encounter a closing tag first.
     *
     * @throws java.lang.Exception if an error occurs parsing the XML.
     */
    public static String parseText(XmlPullParser parser)
        throws Exception
    {
        boolean done = false;

        int eventType;
        String text = null;

        while (!done)
        {
            eventType = parser.next();

            if (eventType == XmlPullParser.TEXT)
            {
                text = parser.getText();
            }
            else if (eventType == XmlPullParser.END_TAG)
            {
                done = true;
            }
        }

        return text;
    }
}
