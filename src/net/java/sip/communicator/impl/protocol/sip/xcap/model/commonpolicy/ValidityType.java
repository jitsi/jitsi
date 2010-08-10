/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip.xcap.model.commonpolicy;

/**
 * <p>Java class for validityType complex type.
 * <p/>
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p/>
 * <pre>
 * &lt;complexType name="validityType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence maxOccurs="unbounded">
 *         &lt;element name="from" type="{http://www.w3.org/2001/XMLSchema}dateTime"/>
 *         &lt;element name="until" type="{http://www.w3.org/2001/XMLSchema}dateTime"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 * @author Grigorii Balutsel
 */
//@XmlAccessorType(XmlAccessType.FIELD)
//@XmlType(name = "validityType", propOrder = {
//        "fromAndUntil"
//        })
public class ValidityType
{

//    @XmlElementRefs({
//    @XmlElementRef(name = "from",
//            namespace = "urn:ietf:params:xml:ns:common-policy",
//            type = JAXBElement.class),
//    @XmlElementRef(name = "until",
//            namespace = "urn:ietf:params:xml:ns:common-policy",
//            type = JAXBElement.class)
//            })
//    protected List<JAXBElement<XMLGregorianCalendar>> fromAndUntil;

//    /**
//     * Gets the value of the fromAndUntil property.
//     * <p/>
//     * <p/>
//     * This accessor method returns a reference to the live list,
//     * not a snapshot. Therefore any modification you make to the
//     * returned list will be present inside the JAXB object.
//     * This is why there is not a <CODE>set</CODE> method for the fromAndUntil property.
//     * <p/>
//     * <p/>
//     * For example, to add a new item, do as follows:
//     * <pre>
//     *    getFromAndUntil().add(newItem);
//     * </pre>
//     * <p/>
//     * <p/>
//     * <p/>
//     * Objects of the following type(s) are allowed in the list
//     * {@link JAXBElement }{@code <}{@link XMLGregorianCalendar }{@code >}
//     * {@link JAXBElement }{@code <}{@link XMLGregorianCalendar }{@code >}
//     */
//    public List<JAXBElement<XMLGregorianCalendar>> getFromAndUntil()
//    {
//        if (fromAndUntil == null)
//        {
//            fromAndUntil = new ArrayList<JAXBElement<XMLGregorianCalendar>>();
//        }
//        return this.fromAndUntil;
//    }
}
