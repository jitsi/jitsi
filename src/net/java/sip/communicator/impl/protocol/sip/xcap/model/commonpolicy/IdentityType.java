/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip.xcap.model.commonpolicy;

import org.w3c.dom.Element;

//import javax.xml.bind.*;
//import javax.xml.bind.annotation.*;
import java.util.*;

/**
 * <p>Java class for identityType complex type.
 * <p/>
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p/>
 * <pre>
 * &lt;complexType name="identityType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;choice maxOccurs="unbounded">
 *         &lt;element name="one" type="{urn:ietf:params:xml:ns:common-policy}oneType"/>
 *         &lt;element name="many" type="{urn:ietf:params:xml:ns:common-policy}manyType"/>
 *         &lt;any/>
 *       &lt;/choice>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 * @author Grigorii Balutsel
 */

//@XmlAccessorType(XmlAccessType.FIELD)
//@XmlType(name = "identityType", propOrder = {
//        "oneList",
//        "manyList",
//        "any"
//        })
//@XmlRootElement(name = "identity",
//        namespace = "urn:ietf:params:xml:ns:common-policy")
public class IdentityType
{
//    @XmlElementRef(name = "one",
//            namespace = "urn:ietf:params:xml:ns:common-policy",
//            type = OneType.class)
    protected List<OneType> oneList;

//    @XmlElementRef(name = "many",
//            namespace = "urn:ietf:params:xml:ns:common-policy",
//            type = ManyType.class)
    protected List<ManyType> manyList;

//    @XmlAnyElement(lax = true)
    protected List<Object> any;

//    /**
//     * Gets the value of the oneOrManyOrAny property.
//     * <p/>
//     * <p/>
//     * This accessor method returns a reference to the live list,
//     * not a snapshot. Therefore any modification you make to the
//     * returned list will be present inside the JAXB object.
//     * This is why there is not a <CODE>set</CODE> method for the oneOrManyOrAny property.
//     * <p/>
//     * <p/>
//     * For example, to add a new item, do as follows:
//     * <pre>
//     *    getOneOrManyOrAny().add(newItem);
//     * </pre>
//     * <p/>
//     * <p/>
//     * <p/>
//     * Objects of the following type(s) are allowed in the list
//     * {@link JAXBElement }{@code <}{@link ManyType }{@code >}
//     * {@link Element }
//     * {@link Object }
//     * {@link JAXBElement }{@code <}{@link OneType }{@code >}
//     */
    public List<Object> getAny()
    {
        if (any == null)
        {
            any = new ArrayList<Object>();
        }
        return this.any;
    }

    public List<OneType> getOneList()
    {
        if (oneList == null)
        {
            oneList = new ArrayList<OneType>();
        }
        return this.oneList;
    }

    public List<ManyType> getManyList()
    {
        if (manyList == null)
        {
            manyList = new ArrayList<ManyType>();
        }
        return this.manyList;
    }
}
