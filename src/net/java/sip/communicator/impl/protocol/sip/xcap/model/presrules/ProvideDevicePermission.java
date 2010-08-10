/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip.xcap.model.presrules;

import java.util.*;

/**
 * Allows a watcher to see "device" information present in the presence
 * document.
 * <p/>
 * Compliant with rfc5025.
 *
 * @author Grigorii Balutsel
 */
//@XmlAccessorType(XmlAccessType.FIELD)
//@XmlType(name = "provideDevicePermission", propOrder = {
//        "allDevices",
//        "deviceIDOrOccurrenceIdOrClazz"
//        })
//@XmlRootElement(name = "providedevices",
//        namespace = "urn:ietf:params:xml:ns:pres-rules")
public class ProvideDevicePermission
{
//
//    @XmlElement(name = "all-devices",
//            namespace = "urn:ietf:params:xml:ns:pres-rules")
    protected AllDevices allDevices;
//    @XmlElementRefs({
//        @XmlElementRef(name = "deviceID", namespace = "urn:ietf:params:xml:ns:pres-rules", type = JAXBElement.class),
//        @XmlElementRef(name = "occurrence-id", namespace = "urn:ietf:params:xml:ns:pres-rules", type = JAXBElement.class),
//        @XmlElementRef(name = "class", namespace = "urn:ietf:params:xml:ns:pres-rules", type = JAXBElement.class)

    //    })

//    @XmlAnyElement(lax = true)
    protected List<Object> deviceIDOrOccurrenceIdOrClazz;

    /**
     * Gets the value of the allDevices property.
     *
     * @return possible object is
     *         {@link AllDevices }
     */
    public AllDevices getAllDevices()
    {
        return allDevices;
    }

    /**
     * Sets the value of the allDevices property.
     *
     * @param value allowed object is
     *              {@link AllDevices }
     */
    public void setAllDevices(AllDevices value)
    {
        this.allDevices = value;
    }

    public List<Object> getDeviceIDOrOccurrenceIdOrClazz()
    {
        if (deviceIDOrOccurrenceIdOrClazz == null)
        {
            deviceIDOrOccurrenceIdOrClazz = new ArrayList<Object>();
        }
        return this.deviceIDOrOccurrenceIdOrClazz;
    }



    public static class AllDevices
    {
    }
}
