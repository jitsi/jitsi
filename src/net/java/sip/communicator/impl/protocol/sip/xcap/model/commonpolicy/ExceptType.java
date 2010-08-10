/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip.xcap.model.commonpolicy;

//import javax.xml.bind.annotation.*;

/**
 * <p>Java class for exceptType complex type.
 * <p/>
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p/>
 * <pre>
 * &lt;complexType name="exceptType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attribute name="domain" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="id" type="{http://www.w3.org/2001/XMLSchema}anyURI" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 * @author Grigorii Balutsel
 */
//@XmlAccessorType(XmlAccessType.FIELD)
//@XmlType(name = "exceptType")
public class ExceptType
{

//    @XmlAttribute
    protected String domain;

//    @XmlAttribute
    protected String id;

    /**
     * Gets the value of the domain property.
     *
     * @return possible object is
     *         {@link String }
     */
    public String getDomain()
    {
        return domain;
    }

    /**
     * Sets the value of the domain property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setDomain(String value)
    {
        this.domain = value;
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
