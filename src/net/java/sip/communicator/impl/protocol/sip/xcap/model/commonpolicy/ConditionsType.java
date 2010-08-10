/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip.xcap.model.commonpolicy;

import java.util.*;

/**
 * <p>Java class for conditionsType complex type.
 * <p/>
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p/>
 * <pre>
 * &lt;complexType name="conditionsType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;choice maxOccurs="unbounded">
 *         &lt;element name="identity" type="{urn:ietf:params:xml:ns:common-policy}identityType" minOccurs="0"/>
 *         &lt;element name="sphere" type="{urn:ietf:params:xml:ns:common-policy}sphereType" minOccurs="0"/>
 *         &lt;element name="validity" type="{urn:ietf:params:xml:ns:common-policy}validityType" minOccurs="0"/>
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
//@XmlType(name = "conditionsType", propOrder = {
//        "identityOrSphereOrValidity"
//        })
public class ConditionsType
{

//    @XmlElementRefs({
//    @XmlElementRef(name = "identity",
//            namespace = "urn:ietf:params:xml:ns:common-policy",
//            type = JAXBElement.class),
//    @XmlElementRef(name = "sphere",
//            namespace = "urn:ietf:params:xml:ns:common-policy",
//            type = JAXBElement.class),
//    @XmlElementRef(name = "validity",
//            namespace = "urn:ietf:params:xml:ns:common-policy",
//            type = JAXBElement.class)
//            })
//    @XmlAnyElement(lax = true)
    protected List<Object> identityOrSphereOrValidity;

   
    public List<Object> getIdentityOrSphereOrValidity()
    {
        if (identityOrSphereOrValidity == null)
        {
            identityOrSphereOrValidity = new ArrayList<Object>();
        }
        return this.identityOrSphereOrValidity;
    }
}
