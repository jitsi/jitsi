/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.jingle;

import net.java.sip.communicator.impl.protocol.jabber.extensions.*;

/**
 * Represents the <tt>parameter</tt> elements described in XEP-0167.
 *
 * @author Emil Ivov
 */
public class ParameterPacketExtension extends AbstractPacketExtension
{
    /**
     * The name of the "parameter" element.
     */
    public static final String ELEMENT_NAME = "parameter";

    /**
     * The name of the <tt>name</tt> parameter in the <tt>parameter</tt>
     * element.
     */
    public static final String NAME_ARG_NAME = "name";

    /**
     * The name of the <tt>value</tt> parameter in the <tt>parameter</tt>
     * element.
     */
    public static final String VALUE_ARG_NAME = "value";

    /**
     * Creates a new {@link ParameterPacketExtension} instance.
     */
    public ParameterPacketExtension()
    {
        super(null, ELEMENT_NAME);
    }

    /**
     * Sets the name of the format parameter we are representing here.
     *
     * @param name the name of the format parameter we are representing here.
     */
    public void setName(String name)
    {
        super.setAttribute(NAME_ARG_NAME, name);
    }

    /**
     * Returns the name of the format parameter we are representing here.
     *
     * @return the name of the format parameter we are representing here.
     */
    public String getName()
    {
        return super.getAttributeAsString(NAME_ARG_NAME);
    }

    /**
     * Sets that value of the format parameter we are representing here.
     *
     * @param value the value of the format parameter we are representing here.
     */
    public void setValue(String value)
    {
        super.setAttribute(VALUE_ARG_NAME, value);
    }

    /**
     * Returns the value of the format parameter we are representing here.
     *
     * @return the value of the format parameter we are representing here.
     */
    public String getValue()
    {
        return super.getAttributeAsString(VALUE_ARG_NAME);
    }
}
