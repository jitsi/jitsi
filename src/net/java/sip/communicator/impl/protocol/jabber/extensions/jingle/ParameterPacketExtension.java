/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.jingle;

import org.jivesoftware.smack.packet.*;

/**
 * Represents the <tt>parameter</tt> elements described in XEP-0167.
 *
 * @author Emil Ivov
 */
public class ParameterPacketExtension implements PacketExtension
{
    /**
     * Parameters do not live in a namespace of their own so we have
     * <tt>null</tt> here.
     */
    public static final String NAMESPACE = null;

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
     * The name of the parameter represented here.
     */
    private String name;

    /**
     * The value of the parameter represented here.
     */
    private String value;

    /**
     * Returns the name of the <tt>parameter</tt> element.
     *
     * @return the name of the <tt>parameter</tt> element.
     */
    public String getElementName()
    {
        return ELEMENT_NAME;
    }

    /**
     * Returns the namespace for the <tt>parameter</tt> element which is
     * <tt>null</tt>.
     *
     * @return <tt>null</tt> since we don't have a namespace for parameters.
     */
    public String getNamespace()
    {
        return NAMESPACE;
    }

    /**
     * Returns the XML representation of this <tt>parameter</tt> element.
     *
     * @return this packet extension as an XML <tt>String</tt>.
     */
    public String toXML()
    {
        StringBuilder bldr = new StringBuilder();

        return bldr.toString();
    }

    /**
     * Sets the name of the format parameter we are representing here.
     *
     * @param name the name of the format parameter we are representing here.
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * Returns the name of the format parameter we are representing here.
     *
     * @return the name of the format parameter we are representing here.
     */
    public String getName()
    {
        return name;
    }

    /**
     * Sets that value of the format parameter we are representing here.
     *
     * @param value the value of the format paramter we are representing here.
     */
    public void setValue(String value)
    {
        this.value = value;
    }

    /**
     * @return the value
     */
    public String getValue()
    {
        return value;
    }
}
