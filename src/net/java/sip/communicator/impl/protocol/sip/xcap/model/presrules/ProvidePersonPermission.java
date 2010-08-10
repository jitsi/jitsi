/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip.xcap.model.presrules;

import java.util.*;


/**
 * Allows a watcher to see the "person" information present in the presence
 * document.
 * <p/>
 * Compliant with rfc5025.
 *
 * @author Grigorii Balutsel
 */
//@XmlAccessorType(XmlAccessType.FIELD)
//@XmlType(name = "providePersonPermission", propOrder = {
//        "allPersons",
//        "occurrenceIdOrClazzOrAny"
//        })
//@XmlRootElement(name = "provide-persons",
//        namespace = "urn:ietf:params:xml:ns:pres-rules")
public class ProvidePersonPermission
{
//
//    @XmlElement(name = "all-persons",
//            namespace = "urn:ietf:params:xml:ns:pres-rules")
    protected AllPersons allPersons;
//    @XmlElementRefs({
//        @XmlElementRef(name = "occurrence-id", namespace = "urn:ietf:params:xml:ns:pres-rules", type = JAXBElement.class),

    //        @XmlElementRef(name = "class", namespace = "urn:ietf:params:xml:ns:pres-rules", type = JAXBElement.class)
    //    })
//    @XmlAnyElement(lax = true)
    protected List<Object> occurrenceIdOrClazzOrAny;

    /**
     * Gets the value of the allPersons property.
     *
     * @return possible object is
     *         {@link AllPersons }
     */
    public AllPersons getAllPersons()
    {
        return allPersons;
    }

    /**
     * Sets the value of the allPersons property.
     *
     * @param value allowed object is
     *              {@link AllPersons }
     */
    public void setAllPersons(AllPersons value)
    {
        this.allPersons = value;
    }


    public List<Object> getOccurrenceIdOrClazzOrAny()
    {
        if (occurrenceIdOrClazzOrAny == null)
        {
            occurrenceIdOrClazzOrAny = new ArrayList<Object>();
        }
        return this.occurrenceIdOrClazzOrAny;
    }


//
//    @XmlAccessorType(XmlAccessType.FIELD)
//    @XmlType(name = "")
    public static class AllPersons
    {


    }
}
