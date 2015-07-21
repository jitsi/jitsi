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
package net.java.sip.communicator.impl.protocol.jabber.extensions;

import java.util.logging.*;

import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.provider.*;
import org.xmlpull.v1.*;

/**
 * A provider that parses incoming packet extensions into instances of the
 * {@link Class} that it has been instantiated for.
 *
 * @param <C> Class that the packets we will be parsing belong to
 * @author Emil Ivov
 */
public class DefaultPacketExtensionProvider<C extends AbstractPacketExtension>
    implements PacketExtensionProvider
{
    /**
     * The <tt>Logger</tt> used by the <tt>DefaultPacketExtensionProvider</tt>
     * class and its instances for logging output.
     */
    private static final Logger logger = Logger
                    .getLogger(DefaultPacketExtensionProvider.class.getName());

    /**
     * The {@link Class} that the packets we will be parsing here belong to.
     */
    private final Class<C> packetClass;

    /**
     * Creates a new packet provider for the specified packet extensions.
     *
     * @param c the {@link Class} that the packets we will be parsing belong to.
     */
    public DefaultPacketExtensionProvider(Class<C> c)
    {
        this.packetClass = c;
    }

    /**
     * Parse an extension sub-packet and create a <tt>C</tt> instance. At
     * the beginning of the method call, the xml parser will be positioned on
     * the opening element of the packet extension and at the end of the method
     * call it will be on the closing element of the packet extension.
     *
     * @param parser an XML parser positioned at the packet's starting element.
     *
     * @return a new packet extension instance.
     *
     * @throws java.lang.Exception if an error occurs parsing the XML.
     */
    public C parseExtension(XmlPullParser parser) throws Exception
    {
        C packetExtension = packetClass.newInstance();

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
        int eventType;
        String elementName;
        String namespace;

        while (!done)
        {
            eventType = parser.next();
            elementName = parser.getName();
            namespace = parser.getNamespace();

            if (logger.isLoggable(Level.FINEST))
                logger.finest("Will parse " + elementName
                    + " ns=" + namespace
                    + " class=" + packetExtension.getClass().getSimpleName());

            if (eventType == XmlPullParser.START_TAG)
            {
                PacketExtensionProvider provider
                    = (PacketExtensionProvider)ProviderManager.getInstance()
                        .getExtensionProvider( elementName, namespace );

                if(provider == null)
                {
                    //we don't know how to handle this kind of extensions.
                    logger.fine("Could not add a provider for element "
                        + elementName + " from namespace " + namespace);
                }
                else
                {
                    PacketExtension childExtension
                        = provider.parseExtension(parser);

                    if(namespace != null)
                    {
                        if(childExtension instanceof AbstractPacketExtension)
                        {
                            ((AbstractPacketExtension)childExtension).
                                setNamespace(namespace);
                        }
                    }
                    packetExtension.addChildExtension(childExtension);
                }
            }
            if (eventType == XmlPullParser.END_TAG)
            {
                if (parser.getName().equals(packetExtension.getElementName()))
                {
                    done = true;
                }
            }
            if (eventType == XmlPullParser.TEXT)
            {
                String text = parser.getText();
                packetExtension.setText(text);
            }

            if (logger.isLoggable(Level.FINEST))
                logger.finest("Done parsing " + elementName);
        }

        return packetExtension;
    }
}
