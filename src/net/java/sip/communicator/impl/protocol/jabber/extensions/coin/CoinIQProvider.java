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
package net.java.sip.communicator.impl.protocol.jabber.extensions.coin;

import net.java.sip.communicator.impl.protocol.jabber.extensions.*;

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
     * Provider for URIs packet extension.
     */
    private final DefaultPacketExtensionProvider<URIsPacketExtension>
       urisProvider = new DefaultPacketExtensionProvider<URIsPacketExtension>(
               URIsPacketExtension.class);

    /**
     * Provider for sidbars by val packet extension.
     */
    private final DefaultPacketExtensionProvider<SidebarsByValPacketExtension>
       sidebarsByValProvider =
           new DefaultPacketExtensionProvider<SidebarsByValPacketExtension>(
               SidebarsByValPacketExtension.class);

    /**
     * Constructor.
     */
    public CoinIQProvider()
    {
        ProviderManager providerManager = ProviderManager.getInstance();

        providerManager.addExtensionProvider(
                UserRolesPacketExtension.ELEMENT_NAME,
                UserRolesPacketExtension.NAMESPACE,
                new DefaultPacketExtensionProvider
                    <UserRolesPacketExtension>(
                                    UserRolesPacketExtension.class));

        providerManager.addExtensionProvider(
                URIPacketExtension.ELEMENT_NAME,
                URIPacketExtension.NAMESPACE,
                new DefaultPacketExtensionProvider
                    <URIPacketExtension>(
                                    URIPacketExtension.class));

        providerManager.addExtensionProvider(
                SIPDialogIDPacketExtension.ELEMENT_NAME,
                SIPDialogIDPacketExtension.NAMESPACE,
                new DefaultPacketExtensionProvider
                    <SIPDialogIDPacketExtension>(
                                    SIPDialogIDPacketExtension.class));

        providerManager.addExtensionProvider(
                ConferenceMediumPacketExtension.ELEMENT_NAME,
                ConferenceMediumPacketExtension.NAMESPACE,
                new ConferenceMediumProvider());

        providerManager.addExtensionProvider(
                ConferenceMediaPacketExtension.ELEMENT_NAME,
                ConferenceMediaPacketExtension.NAMESPACE,
                new DefaultPacketExtensionProvider
                    <ConferenceMediaPacketExtension>(
                                    ConferenceMediaPacketExtension.class));

        providerManager.addExtensionProvider(
                CallInfoPacketExtension.ELEMENT_NAME,
                CallInfoPacketExtension.NAMESPACE,
                new DefaultPacketExtensionProvider
                    <CallInfoPacketExtension>(
                                    CallInfoPacketExtension.class));
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
                else if(elementName.equals(URIsPacketExtension.ELEMENT_NAME))
                {
                    PacketExtension childExtension =
                        urisProvider.parseExtension(parser);
                    coinIQ.addExtension(childExtension);
                }
                else if(elementName.equals(
                        SidebarsByValPacketExtension.ELEMENT_NAME))
                {
                    PacketExtension childExtension =
                        sidebarsByValProvider.parseExtension(parser);
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
