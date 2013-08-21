/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions;

import net.java.sip.communicator.service.protocol.*;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.provider.*;
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
     * Creates a new instance and sets the "uri", "callid" and "password" attributes.
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

        Set<ConferenceDescription.Transport> transports
                = cd.getSupportedTransports();
        for(ConferenceDescription.Transport transport : transports)
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
        setAttribute(URI_ATTR_NAME, uri);
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
     * A <tt>PacketExtension</tt> that represents a "transport" child element.
     * It corresponds to a <tt>ConferenceDescription.Transport</tt>.
     */
    public static class TransportPacketExtension
        extends AbstractPacketExtension
    {
        /**
         * The name of the "name" attribute.
         */
        public static final String NAME_ATTR_NAME = "name";

        /**
         * Creates a new instance and sets the "name" attribute to the
         * <tt>String</tt> value of <tt>transport</tt>
         *
         * @param transport the <tt>ConferenceDescription.Transport</tt> to
         * use to get set the "name" attribute.
         */
        public TransportPacketExtension(
                ConferenceDescription.Transport transport)
        {
            this();

            if (transport != null)
                setAttribute(NAME_ATTR_NAME, transport.toString());
        }

        /**
         * Creates a new instance.
         */
        public TransportPacketExtension()
        {
            super(null, TRANSPORT_ELEM_NAME);
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
                        String transportStr = parser.getAttributeValue(
                                "",
                                TransportPacketExtension.NAME_ATTR_NAME);
                        ConferenceDescription.Transport transport =
                                ConferenceDescription.Transport.
                                        parseString(transportStr);

                        if (transport != null)
                        {
                            transportExt
                                    = new TransportPacketExtension(transport);
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
