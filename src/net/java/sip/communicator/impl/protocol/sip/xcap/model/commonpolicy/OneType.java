/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip.xcap.model.commonpolicy;

import org.w3c.dom.*;

/**
 * <p>Java class for oneType complex type.
 * <p/>
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p/>
 * <pre>
 * &lt;complexType name="oneType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;any/>
 *       &lt;/sequence>
 *       &lt;attribute name="id" use="required" type="{http://www.w3.org/2001/XMLSchema}anyURI" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 * @author Grigorii Balutsel
 */
//@XmlAccessorType(XmlAccessType.FIELD)
//@XmlType(name = "oneType", propOrder = {
//        "any"
//        })
//@XmlRootElement(name = "one",
//        namespace = "urn:ietf:params:xml:ns:common-policy")
public class OneType
{

//    @XmlAnyElement(lax = true)
    protected Object any;

//    @XmlAttribute(required = true)
    protected String id;

    /**
     * Gets the value of the any property.
     *
     * @return possible object is
     *         {@link Element }
     *         {@link Object }
     */
    public Object getAny()
    {
        return any;
    }

    /**
     * Sets the value of the any property.
     *
     * @param value allowed object is
     *              {@link Element }
     *              {@link Object }
     */
    public void setAny(Object value)
    {
        this.any = value;
    }

    /**
     * Gets the value of the id property.
     *
     * @return possible object is
     *         {@link String }
     */
    public String getId()
    {
        return id;
    }

    /**
     * Sets the value of the id property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setId(String value)
    {
        this.id = value;
    }
}
