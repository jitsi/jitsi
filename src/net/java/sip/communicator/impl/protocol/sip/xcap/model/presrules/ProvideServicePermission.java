/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip.xcap.model.presrules;

import java.util.*;

/**
 * Allows a watcher to see service information present in "tuple" elements in
 * the presence document the subscription authorization decision that the server
 * should make.
 * <p/>
 * Compliant with rfc5025.
 *
 * @author Grigorii Balutsel
 */
//@XmlAccessorType(XmlAccessType.FIELD)
//@XmlType(name = "provideServicePermission", propOrder = {
//        "allServices",
//        "serviceUriOrServiceUriSchemeOrOccurrenceId"
//        })
//@XmlRootElement(name = "provide-services",
//        namespace = "urn:ietf:params:xml:ns:pres-rules")
public class ProvideServicePermission
{
//
//    @XmlElement(name = "all-services",
//            namespace = "urn:ietf:params:xml:ns:pres-rules")
    protected AllServices allServices;
//    @XmlElementRefs({
//        @XmlElementRef(name = "service-uri", namespace = "urn:ietf:params:xml:ns:pres-rules", type = JAXBElement.class),
//        @XmlElementRef(name = "service-uri-scheme", namespace = "urn:ietf:params:xml:ns:pres-rules", type = JAXBElement.class),
//        @XmlElementRef(name = "occurrence-id", namespace = "urn:ietf:params:xml:ns:pres-rules", type = JAXBElement.class),
//        @XmlElementRef(name = "class", namespace = "urn:ietf:params:xml:ns:pres-rules", type = JAXBElement.class)

    //    })

//    @XmlAnyElement(lax = true)
    protected List<Object> serviceUriOrServiceUriSchemeOrOccurrenceId;

    /**
     * Gets the value of the allServices property.
     *
     * @return possible object is
     *         {@link AllServices }
     */
    public AllServices getAllServices()
    {
        return allServices;
    }

    /**
     * Sets the value of the allServices property.
     *
     * @param value allowed object is
     *              {@link AllServices }
     */
    public void setAllServices(AllServices value)
    {
        this.allServices = value;
    }


    public List<Object> getServiceUriOrServiceUriSchemeOrOccurrenceId()
    {
        if (serviceUriOrServiceUriSchemeOrOccurrenceId == null)
        {
            serviceUriOrServiceUriSchemeOrOccurrenceId =
                    new ArrayList<Object>();
        }
        return this.serviceUriOrServiceUriSchemeOrOccurrenceId;
    }



//    @XmlAccessorType(XmlAccessType.FIELD)
//    @XmlType(name = "")
    public static class AllServices
    {


    }
}
