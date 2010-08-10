/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip.xcap.model.presrules;

/**
 * Indicates that the unknown presence attribute with the given name and
 * namespace should be included in the document.
 * <p/>
 * Compliant with rfc5025
 *
 * @author Grigorii Balutsel
 */
//@XmlAccessorType(XmlAccessType.FIELD)
//@XmlType(name = "unknownBooleanPermission", propOrder = {
//        "value"
//        })
public class UnknownBooleanPermission
{
    /**
     * The value.
     */
//    @XmlValue
    protected boolean value;

    /**
     * Presence name.
     */
//    @XmlAttribute(required = true)
    protected String name;

    /**
     * Namespace URI.
     */
//    @XmlAttribute(required = true)
    protected String ns;

    /**
     * Gets the value of the value property.
     */
    public boolean isValue()
    {
        return value;
    }

    /**
     * Sets the value of the value property.
     */
    public void setValue(boolean value)
    {
        this.value = value;
    }

    /**
     * Gets the value of the name property.
     *
     * @return possible object is
     *         {@link String }
     */
    public String getName()
    {
        return name;
    }

    /**
     * Sets the value of the name property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setName(String value)
    {
        this.name = value;
    }

    /**
     * Gets the value of the ns property.
     *
     * @return possible object is
     *         {@link String }
     */
    public String getNs()
    {
        return ns;
    }

    /**
     * Sets the value of the ns property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setNs(String value)
    {
        this.ns = value;
    }
}
