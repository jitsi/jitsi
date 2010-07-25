/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.jingle;

import java.net.*;

import net.java.sip.communicator.impl.protocol.jabber.extensions.*;

/**
 * Represents the <tt>extmap</tt> elements described in RFC 5285.
 *
 * @author Emil Ivov
 */
public class ExtmapPacketExtension
    extends AbstractPacketExtension
{
    /**
     * The name of the "extmap" element.
     */
    public static final String ELEMENT_NAME = "extmap";

    /**
     * The name of the <tt>uri</tt> attribute in the <tt>extmap</tt> element.
     */
    public static final String URI_ATTR_NAME = "uri";

    /**
     * The name of the <tt>value</tt> attribute in the <tt>extmap</tt> element.
     */
    public static final String VALUE_ATTR_NAME = "value";

    /**
     * The name of the <tt>direction</tt> attribute in the <tt>extmap</tt>
     * element.
     */
    public static final String DIRECTION_ATTR_NAME = "direction";

    /**
     * The name of the <tt>attributes</tt> attribute in the <tt>extmap</tt>
     * element.
     */
    public static final String ATTRIBUTES_ATTR_NAME = "attributes";

    /**
     * Creates a new {@link ExtmapPacketExtension} instance.
     */
    public ExtmapPacketExtension()
    {
        super(null, ELEMENT_NAME);
    }

    /**
     * Sets the uri of the extmap attribute we are representing here.
     *
     * @param uri the uri of the extmap attribute we are representing here.
     */
    public void setUri(URI uri)
    {
        super.setAttribute(URI_ATTR_NAME, uri.toString());
    }

    /**
     * Returns the uri of the extmap attribute we are representing here.
     *
     * @return the uri of the extmap attribute we are representing here.
     */
    public URI getUri()
    {
        return super.getAttributeAsURI(URI_ATTR_NAME);
    }

    /**
     * Sets that value of the extmap attribute we are representing here.
     *
     * @param value the value of the extmap attribute we are representing here.
     */
    public void setValue(String value)
    {
        super.setAttribute(VALUE_ATTR_NAME, value);
    }

    /**
     * Returns the value of the extmap attribute we are representing here.
     *
     * @return the value of the extmap attribute we are representing here.
     */
    public String getValue()
    {
        return super.getAttributeAsString(VALUE_ATTR_NAME);
    }

    /**
     * Sets the direction that this extmap element is to be transmitted in.
     *
     * @param direction the direction that this extmap element is to be
     * transmitted in.
     */
    public void setDirection(String direction)
    {
        super.setAttribute(DIRECTION_ATTR_NAME, direction);
    }

    /**
     * Returns the direction that this extmap element is to be transmitted in.
     *
     * @return the direction that this extmap element is to be transmitted in.
     */
    public String getDirection()
    {
        return super.getAttributeAsString(DIRECTION_ATTR_NAME);
    }

    /**
     * Sets optional attributes for this extmap element.
     *
     * @param attributes optional attributes for this extmap element..
     */
    public void setAttributes(String attributes)
    {
        super.setAttribute(ATTRIBUTES_ATTR_NAME, attributes);
    }

    /**
     * Returns optional attributes for this extmap element.
     *
     * @return optional attributes for this extmap element.
     */
    public String getAttributes()
    {
        return super.getAttributeAsString(ATTRIBUTES_ATTR_NAME);
    }

}
