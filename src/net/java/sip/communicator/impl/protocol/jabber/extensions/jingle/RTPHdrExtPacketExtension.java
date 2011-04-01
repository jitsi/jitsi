/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.jingle;

import java.net.*;

import org.jivesoftware.smack.packet.*;

import net.java.sip.communicator.impl.protocol.jabber.extensions.*;
import net.java.sip.communicator.impl.protocol.jabber.extensions.jingle.ContentPacketExtension.*;

/**
 * RTP header extension.
 *
 * @author Sebastien Vincent
 */
public class RTPHdrExtPacketExtension
    extends AbstractPacketExtension
{
    /**
     * The namespace.
     */
    public static final String NAMESPACE =
        "urn:xmpp:jingle:apps:rtp:rtp-hdrext:0";

    /**
     * The name of the "candidate" element.
     */
    public static final String ELEMENT_NAME = "rtp-hdrext";

    /**
     * The name of the ID attribute.
     */
    public static final String ID_ATTR_NAME = "id";

    /**
     * The name of the senders attribute.
     */
    public static final String SENDERS_ATTR_NAME = "senders";

    /**
     * The name of the URI attribute.
     */
    public static final String URI_ATTR_NAME = "uri";

    /**
     * The name of the <tt>attributes</tt> attribute in the <tt>extmap</tt>
     * element.
     */
    public static final String ATTRIBUTES_ATTR_NAME = "attributes";

    /**
     * Constructor.
     */
    public RTPHdrExtPacketExtension()
    {
        super(NAMESPACE, ELEMENT_NAME);
    }

    /**
     * Set the ID.
     *
     * @param id ID to set
     */
    public void setID(String id)
    {
        setAttribute(ID_ATTR_NAME, id);
    }

    /**
     * Get the ID.
     *
     * @return the ID
     */
    public String getID()
    {
        return getAttributeAsString(ID_ATTR_NAME);
    }

    /**
     * Set the direction.
     *
     * @param senders the direction
     */
    public void setSenders(SendersEnum senders)
    {
        setAttribute(SENDERS_ATTR_NAME, senders);
    }

    /**
     * Get the direction.
     *
     * @return the direction
     */
    public SendersEnum getSenders()
    {
        String attributeVal = getAttributeAsString(SENDERS_ATTR_NAME);

        return attributeVal == null
            ? null : SendersEnum.valueOf( attributeVal.toString() );
    }

    /**
     * Set the URI.
     *
     * @param uri URI to set
     */
    public void setURI(URI uri)
    {
        setAttribute(URI_ATTR_NAME, uri.toString());
    }

    /**
     * Get the URI.
     *
     * @return the URI
     */
    public URI getURI()
    {
        return getAttributeAsURI(URI_ATTR_NAME);
    }

    /**
     * Set attributes.
     *
     * @param attributes attributes value
     */
    public void setAttributes(String attributes)
    {
        ParameterPacketExtension paramExt =
            new ParameterPacketExtension();

        paramExt.setName(ATTRIBUTES_ATTR_NAME);
        paramExt.setValue(attributes);
        addChildExtension(paramExt);
    }

    /**
     * Get "attributes" value.
     *
     * @return "attributes" value
     */
    public String getAttributes()
    {
        for(PacketExtension ext : getChildExtensions())
        {
            if(ext instanceof ParameterPacketExtension)
            {
                ParameterPacketExtension p = (ParameterPacketExtension)ext;

                if(p.getName().equals(ATTRIBUTES_ATTR_NAME))
                {
                    return p.getValue();
                }
            }
        }
        return null;
    }

}
