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
package net.java.sip.communicator.impl.protocol.jabber.extensions.geolocation;

import net.java.sip.communicator.util.*;

import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.provider.*;
import org.xmlpull.v1.*;

/**
 * This class parses incoming messages and extracts the geolocation parameters
 * from the raw XML messages.
 *
 * @author Guillaume Schreiner
 */
public class GeolocationPacketExtensionProvider
    implements PacketExtensionProvider
{
    /**
     * The logger of this class.
     */
    private static final Logger logger =
        Logger.getLogger(GeolocationPacketExtensionProvider.class);

    /**
     * The name of the XML element used for transport of geolocation parameters.
     */
    public static final String ELEMENT_NAME = "geoloc";

    /**
     * The names XMPP space that the geolocation elements belong to.
     */
    public static final String NAMESPACE = "http://jabber.org/protocol/geoloc";

    /**
     * Creates a new GeolocationPacketExtensionProvider.
     * ProviderManager requires that every PacketExtensionProvider has a public,
     * no-argument constructor
     */
    public GeolocationPacketExtensionProvider()
    {}

    /**
     * Parses a GeolocationPacketExtension packet (extension sub-packet).
     *
     * @param parser an XML parser.
     * @return a new GeolocationPacketExtension instance.
     * @throws Exception if an error occurs parsing the XML.
     * @todo Implement this
     *   org.jivesoftware.smack.provider.PacketExtensionProvider method
     */
    public PacketExtension parseExtension(XmlPullParser parser)
        throws Exception
    {

        GeolocationPacketExtension result = new GeolocationPacketExtension();

        if (logger.isTraceEnabled())
            logger.trace("Trying to map XML Geolocation Extension");

        boolean done = false;
        while (!done)
        {
            try
            {
                int eventType = parser.next();
                if (eventType == XmlPullParser.START_TAG)
                {
                    if (parser.getName().equals(GeolocationPacketExtension.ALT))
                    {
                        result.setAlt(Float.parseFloat(parser.nextText()));
                    }
                    if (parser.getName()
                            .equals(GeolocationPacketExtension.AREA))
                    {
                        result.setArea(parser.nextText());
                    }
                    if (parser.getName()
                            .equals(GeolocationPacketExtension.BEARING))
                    {
                        result.setBearing(Float.parseFloat(parser.nextText()));
                    }
                    if (parser.getName()
                            .equals(GeolocationPacketExtension.BUILDING))
                    {
                        result.setBuilding(parser.nextText());
                    }
                    if (parser.getName()
                            .equals(GeolocationPacketExtension.COUNTRY))
                    {
                        result.setCountry(parser.nextText());
                    }
                    if (parser.getName()
                            .equals(GeolocationPacketExtension.DATUM))
                    {
                        result.setDatum(parser.nextText());
                    }
                    if (parser.getName().equals(GeolocationPacketExtension.
                                                DESCRIPTION))
                    {
                        result.setDescription(parser.nextText());
                    }
                    if (parser.getName()
                            .equals(GeolocationPacketExtension.ERROR))
                    {
                        result.setError(Float.parseFloat(parser.nextText()));
                    }
                    if (parser.getName()
                            .equals(GeolocationPacketExtension.FLOOR))
                    {
                        result.setFloor(parser.nextText());
                    }
                    if (parser.getName().equals(GeolocationPacketExtension.LAT))
                    {
                        result.setLat(Float.parseFloat(parser.nextText()));
                    }
                    if (parser.getName()
                            .equals(GeolocationPacketExtension.LOCALITY))
                    {
                        result.setLocality(parser.nextText());
                    }
                    if (parser.getName().equals(GeolocationPacketExtension.LON))
                    {
                        result.setLon(Float.parseFloat(parser.nextText()));
                    }
                    if (parser.getName()
                            .equals(GeolocationPacketExtension.POSTALCODE))
                    {
                        result.setPostalCode(parser.nextText());
                    }
                    if (parser.getName()
                            .equals(GeolocationPacketExtension.REGION))
                    {
                        result.setRegion(parser.nextText());
                    }
                    if (parser.getName()
                            .equals(GeolocationPacketExtension.ROOM))
                    {
                        result.setRoom(parser.nextText());
                    }
                    if (parser.getName()
                            .equals(GeolocationPacketExtension.STREET))
                    {
                        result.setStreet(parser.nextText());
                    }
                    if (parser.getName()
                            .equals(GeolocationPacketExtension.TEXT))
                    {
                        result.setText(parser.nextText());
                    }
                    if (parser.getName()
                            .equals(GeolocationPacketExtension.TIMESTAMP))
                    {
                        result.setText(parser.nextText());
                    }
                }
                else if (eventType == XmlPullParser.END_TAG)
                {
                    if (parser.getName().equals(
                            GeolocationPacketExtensionProvider.ELEMENT_NAME))
                    {
                        done = true;
                        if (logger.isTraceEnabled())
                            logger.trace("Parsing finish");
                    }
                }
            }
            catch (NumberFormatException ex)
            {
                ex.printStackTrace();
            }
        }

        return result;
    }
}
