/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip.xcap.model.commonpolicy;

import net.java.sip.communicator.impl.protocol.sip.xcap.model.presrules.*;

import java.util.*;

/**
 * <p>Java class for extensibleType complex type.
 * <p/>
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p/>
 * <pre>
 * &lt;complexType name="extensibleType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;any/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 * @author Grigorii Balutsel
 */
//@XmlAccessorType(XmlAccessType.FIELD)
//@XmlType(name = "actionsType", propOrder = {
//        "subHandling",
//        "any"
//        })
public class ActionsType
{

//    @XmlElement(name = "sub-handling",
//            namespace = "urn:ietf:params:xml:ns:pres-rules", required = false)
    protected SubHandlingType subHandling;

//    @XmlAnyElement(lax = true)
    protected List<Object> any;

    /**
     * Gets the value of the any property.
     * <p/>
     * <p/>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the any property.
     * <p/>
     * <p/>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getAny().add(newItem);
     * </pre>
     * <p/>
     * <p/>
     * <p/>
     * Objects of the following type(s) are allowed in the list
     * {@link org.w3c.dom.Element }
     * {@link Object }
     */
    public List<Object> getAny()
    {
        if (any == null)
        {
            any = new ArrayList<Object>();
        }
        return this.any;
    }

    public SubHandlingType getSubHandling()
    {
        return subHandling;
    }

    public void setSubHandling(SubHandlingType subHandling)
    {
        this.subHandling = subHandling;
    }
}
