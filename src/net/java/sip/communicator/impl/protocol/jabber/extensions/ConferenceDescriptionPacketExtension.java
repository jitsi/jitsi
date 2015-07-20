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

import net.java.sip.communicator.service.protocol.*;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.provider.*;
import org.jivesoftware.smack.util.*;
import org.xmlpull.v1.*;

import java.util.*;

/**
 * A <tt>PacketExtension</tt> that represents a <tt>ConferenceDescription</tt>
 * object in XML.
 *
 * @author Boris Grozev
 */
public class ConferenceDescriptionPacketExtension
    extends AbstractPacketExtension
{
    /**
     * The namespace for the XML element.
     */
    public static final String NAMESPACE = "http://jitsi.org/protocol/condesc";

    /**
     * The name of the "conference" XML element.
     */
    public static final String ELEMENT_NAME = "conference";

    /**
     * The name of the "transport" element.
     */
    public static final String TRANSPORT_ELEM_NAME = "transport";

    /**
     * The name of the "uri" attribute.
     */
    public static final String URI_ATTR_NAME = "uri";

    /**
     * The name of the "password" attribute.
     */
    public static final String PASSWORD_ATTR_NAME = "auth";

    /**
     * The name of the "callid" attribute.
     */
    public static final String CALLID_ATTR_NAME = "callid";

    /**
     * The name of the "callid" element.
     */
    public static final String CALLID_ELEM_NAME = "callid";

    /**
     * The name of the "available" attribute.
     */
    public static final String AVAILABLE_ATTR_NAME = "available";
    
    /**
     * The name of the conference name attribute.
     */
    public static final String CONFERENCE_NAME_ATTR_NAME = "conference_name";

    /**
     * Creates a new instance without any attributes or children.
     */
    public ConferenceDescriptionPacketExtension()
    {
        this(null, null, null);
    }

    /**
     * Creates a new instance and sets the "uri" attribute.
     *
     * @param uri the value to use for the "uri" attribute.
     */
    public ConferenceDescriptionPacketExtension(String uri)
    {
        this(uri, null, null);
    }

    /**
     * Creates a new instance and sets the "uri" and "callid" attributes.
     *
     * @param uri the value to use for the "uri" attribute.
     * @param callId the value to use for the "callid" attribute.
     */
    public ConferenceDescriptionPacketExtension(String uri, String callId)
    {
        this(uri, callId, null);
    }

    /**
     * Creates a new instance and sets the "uri", "callid" and "password"
     * attributes.
     *
     * @param uri the value to use for the "uri" attribute.
     * @param callId the value to use for the "callid" attribute.
     * @param password the value to use for the "auth" attribute.
     */
    public ConferenceDescriptionPacketExtension(
            String uri, String callId, String password)
    {
        super(NAMESPACE, ELEMENT_NAME);

        if(uri != null)
            setUri(uri);
        if(callId != null)
            setCallId(callId);
        if(password != null)
            setAuth(password);
    }

    /**
     * Creates a new instance which represents <tt>ca</tt>.
     * @param cd the <tt>ConferenceDescription</tt> which to represent in the
     * new instance.
     */
    public ConferenceDescriptionPacketExtension(ConferenceDescription cd)
    {
        this(cd.getUri(), cd.getCallId(), cd.getPassword());
        setAvailable(cd.isAvailable());
        if(cd.getDisplayName() != null)
            setName(cd.getDisplayName());

        Set<String> transports = cd.getSupportedTransports();
        for(String transport : transports)
        {
            addChildExtension(new TransportPacketExtension(transport));
        }
    }

    /**
     * Gets the value of the "uri" attribute.
     * @return the value of the "uri" attribute.
     */
    public String getUri()
    {
        return getAttributeAsString(URI_ATTR_NAME);
    }

    /**
     * Gets the value of the "callid" attribute.
     * @return the value of the "callid" attribute.
     */
    public String getCallId()
    {
        return getAttributeAsString(CALLID_ATTR_NAME);
    }

    /**
     * Gets the value of the "password" attribute.
     * @return the value of the "password" attribute.
     */
    public String getPassword()
    {
        return getAttributeAsString(PASSWORD_ATTR_NAME);
    }

    /**
     * Sets the value of the "uri" attribute.
     * @param uri the value to set
     */
    public void setUri(String uri)
    {
        setAttribute(URI_ATTR_NAME, StringUtils.escapeForXML(uri));
    }

    /**
     * Sets the value of the "callid" attribute.
     * @param callId the value to set
     */
    public void setCallId(String callId)
    {
        setAttribute(CALLID_ATTR_NAME, callId);
    }

    /**
     * Sets the value of the "password" attribute.
     * @param password the value to set
     */
    public void setAuth(String password)
    {
        setAttribute(PASSWORD_ATTR_NAME, password);
    }

    /**
     * Sets the value of the "available" attribute.
     * @param available the value to set
     */
    public void setAvailable(boolean available)
    {
       setAttribute(AVAILABLE_ATTR_NAME, available);
    }
    
    /**
     * Sets the value of the "available" attribute.
     * @param available the value to set
     */
    public void setName(String name)
    {
       setAttribute(CONFERENCE_NAME_ATTR_NAME, StringUtils.escapeForXML(name));
    }

    /**
     * Gets the value of the "available" attribute.
     */
    public boolean isAvailable()
    {
        return Boolean.parseBoolean(getAttributeAsString(AVAILABLE_ATTR_NAME));
    }

    /**
     * Adds a "transport" child element with the given value.
     *
     * @param transport the transport to add.
     */
    public void addTransport(String transport)
    {
        addChildExtension(new TransportPacketExtension(transport));
    }

    /**
     * Creates a <tt>ConferenceDescription</tt> corresponding to this
     * <tt>ConferenceDescriptionPacketExtension</tt>
     * @return a <tt>ConferenceDescription</tt> corresponding to this
     * <tt>ConferenceDescriptionPacketExtension</tt>
     */
    public ConferenceDescription toConferenceDescription()
    {
        ConferenceDescription conferenceDescription
                = new ConferenceDescription(getUri(), getCallId(), getPassword());
        conferenceDescription.setAvailable(isAvailable());
        conferenceDescription.setDisplayName(getName());
        for (TransportPacketExtension t
                : getChildExtensionsOfType(TransportPacketExtension.class))
        {
            conferenceDescription.addTransport(t.getNamespace());
        }

        return conferenceDescription;
    }

    /**
     * Returns the value of the <tt>CONFERENCE_NAME_ATTR_NAME</tt> attribute.
     * @return the name of the conference.
     */
    private String getName()
    {
        return getAttributeAsString(CONFERENCE_NAME_ATTR_NAME);
    }

    /**
     * A <tt>PacketExtension</tt> that represents a "transport" child element.
     */
    public static class TransportPacketExtension
        extends AbstractPacketExtension
    {
        /**
         * Creates a new instance and sets the XML namespace to
         * <tt>transport</tt>
         *
         * @param namespace the XML namespace of the "transport" element.
         */
        public TransportPacketExtension(String namespace)
        {
            super(namespace, TRANSPORT_ELEM_NAME);
        }
    }

    /**
     * Parses elements with the <tt>NAMESPACE</tt> namespace.
     */
    public static class Provider
        implements PacketExtensionProvider
    {
        /**
         * Creates a <tt>ConferenceDescriptionPacketExtension</tt> by parsing
         * an XML document.
         * @param parser the parser to use.
         * @return the created <tt>ConferenceDescriptionPacketExtension</tt>.
         * @throws Exception
         */
        @Override
        public PacketExtension parseExtension(XmlPullParser parser)
                throws Exception
        {
            ConferenceDescriptionPacketExtension packetExtension
                    = new ConferenceDescriptionPacketExtension();

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
            TransportPacketExtension transportExt = null;

            while (!done)
            {
                switch (parser.next())
                {
                case XmlPullParser.START_TAG:
                {
                    elementName = parser.getName();
                    if (TRANSPORT_ELEM_NAME.equals(elementName))
                    {
                        String transportNs = parser.getNamespace();

                        if (transportNs != null)
                        {
                            transportExt
                                    = new TransportPacketExtension(transportNs);
                        }
                    }
                    break;
                }
                case XmlPullParser.END_TAG:
                {
                    elementName = parser.getName();
                    if (ELEMENT_NAME.equals(elementName))
                    {
                        done = true;
                    }
                    else if (TRANSPORT_ELEM_NAME.equals(elementName))
                    {
                        if (transportExt != null)
                        {
                            packetExtension.addChildExtension(transportExt);
                        }
                    }
                    break;
                }
                }
            }
            return packetExtension;
        }
    }
}
