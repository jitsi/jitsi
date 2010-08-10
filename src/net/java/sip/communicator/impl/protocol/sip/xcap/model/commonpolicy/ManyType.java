/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip.xcap.model.commonpolicy;

import java.util.*;
//import javax.xml.bind.*;
//import javax.xml.bind.annotation.*;

/**
 * <p>Java class for manyType complex type.
 * <p/>
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p/>
 * <pre>
 * &lt;complexType name="manyType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;choice maxOccurs="unbounded" minOccurs="0">
 *         &lt;element name="except" type="{urn:ietf:params:xml:ns:common-policy}exceptType"/>
 *         &lt;any/>
 *       &lt;/choice>
 *       &lt;attribute name="domain" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 * @author Grigorii Balutsel
 */
//@XmlAccessorType(XmlAccessType.FIELD)
//@XmlType(name = "manyType", propOrder = {
//        "exceptOrAny"
//        })
//@XmlRootElement(name = "many",
//        namespace = "urn:ietf:params:xml:ns:common-policy")
public class ManyType
{

//    @XmlElementRef(name = "except",
//            namespace = "urn:ietf:params:xml:ns:common-policy",
//            type = JAXBElement.class)
//    @XmlAnyElement(lax = true)
    protected List<Object> exceptOrAny;

//    @XmlAttribute
    protected String domain;

//    /**
//     * Gets the value of the exceptOrAny property.
//     * <p/>
//     * <p/>
//     * This accessor method returns a reference to the live list,
//     * not a snapshot. Therefore any modification you make to the
//     * returned list will be present inside the JAXB object.
//     * This is why there is not a <CODE>set</CODE> method for the exceptOrAny property.
//     * <p/>
//     * <p/>
//     * For example, to add a new item, do as follows:
//     * <pre>
//     *    getExceptOrAny().add(newItem);
//     * </pre>
//     * <p/>
//     * <p/>
//     * <p/>
//     * Objects of the following type(s) are allowed in the list
//     * {@link Element }
//     * {@link JAXBElement }{@code <}{@link ExceptType }{@code >}
//     * {@link Object }
//     */
    public List<Object> getExceptOrAny()
    {
        if (exceptOrAny == null)
        {
            exceptOrAny = new ArrayList<Object>();
        }
        return this.exceptOrAny;
    }

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
}
