/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.rayo;

import net.java.sip.communicator.impl.protocol.jabber.extensions.*;

/**
 * Header packet extension optionally included in {@link RayoIqProvider.RayoIq}.
 * Holds 'name' and 'value' attributes.
 *
 * @author Pawel Domas
 */
public class HeaderExtension
    extends AbstractPacketExtension
{
    /**
     * XML element name.
     */
    public static final String ELEMENT_NAME = "header";

    /**
     * The name of 'name' attribute.
     */
    public static final String NAME_ATTR_NAME = "name";

    /**
     * The name of 'value' attribute.
     */
    public static final String VALUE_ATTR_NAME = "value";

    /**
     * Creates new instance of <tt>HeaderPacketExtension</tt>.
     */
    public HeaderExtension()
    {
        super(null, ELEMENT_NAME);
    }

    /**
     * Return the value of 'name' attribute.
     * @return the value of 'name' attribute.
     */
    public String getName()
    {
        return getAttributeAsString(NAME_ATTR_NAME);
    }

    /**
     * Sets new value for 'name' attribute of this extension.
     * @param name the new value to set for 'name' attribute.
     */
    public void setName(String name)
    {
        setAttribute(NAME_ATTR_NAME, name);
    }

    /**
     * Returns the value of 'value' attribute.
     * @return the value of 'value' attribute.
     */
    public String getValue()
    {
        return getAttributeAsString(VALUE_ATTR_NAME);
    }

    /**
     * Sets new value for the 'value' attribute.
     * @param value new value for the 'value' attribute to set.
     */
    public void setValue(String value)
    {
        setAttribute(VALUE_ATTR_NAME, value);
    }
}
