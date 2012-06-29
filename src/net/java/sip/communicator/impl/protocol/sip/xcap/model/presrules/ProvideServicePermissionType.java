/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip.xcap.model.presrules;

import org.w3c.dom.*;

import java.util.*;

/**
 * The Presence Rules provide-dservices element. Allows a watcher to see service
 * information present in "tuple" elements in the presence document the
 * subscription authorization decision that the server should make.
 * <p/>
 * Compliant with rfc5025.
 *
 * @author Grigorii Balutsel
 */
public class ProvideServicePermissionType
{
    /**
     * The all-services element.
     */
    private AllServicesType allServices;

    /**
     * The list of service-uri elements.
     */
    private List<ServiceUriType> serviceUriList;

    /**
     * The list of service-uri-scheme elements.
     */
    private List<ServiceUriSchemeType> serviceUriSchemeList;

    /**
     * The list of occurrenceId elements.
     */
    private List<OccurrenceIdType> occurrences;

    /**
     * The list of class elements.
     */
    private List<ClassType> classes;

    /**
     * The list of elements.
     */
    private List<Element> any;

    /**
     * Sets the value of the allServices property.
     *
     * @param allServices the allServices to set.
     */
    public void setAllServices(AllServicesType allServices)
    {
        this.allServices = allServices;
    }

    /**
     * Gets the value of the allServices property.
     *
     * @return the allServices property.
     */
    public AllServicesType getAllServices()
    {
        return allServices;
    }

    /**
     * Gets the value of the serviceUriSchemeList property.
     *
     * @return the serviceUriSchemeList property.
     */
    public List<ServiceUriType> getServiceUriList()
    {
        if (serviceUriList == null)
        {
            serviceUriList = new ArrayList<ServiceUriType>();
        }
        return serviceUriList;
    }

    /**
     * Gets the value of the serviceUriSchemeList property.
     *
     * @return the serviceUriSchemeList property.
     */
    public List<ServiceUriSchemeType> getServiceUriSchemeList()
    {
        if (serviceUriSchemeList == null)
        {
            serviceUriSchemeList = new ArrayList<ServiceUriSchemeType>();
        }
        return serviceUriSchemeList;
    }

    /**
     * Gets the value of the occurrences property.
     *
     * @return the occurrences property.
     */
    public List<OccurrenceIdType> getOccurrences()
    {
        if (occurrences == null)
        {
            occurrences = new ArrayList<OccurrenceIdType>();
        }
        return occurrences;
    }

    /**
     * Gets the value of the classes property.
     *
     * @return the classes property.
     */
    public List<ClassType> getClasses()
    {
        if (classes == null)
        {
            classes = new ArrayList<ClassType>();
        }
        return classes;
    }

    /**
     * Gets the value of the any property.
     *
     * @return the any property.
     */
    public List<Element> getAny()
    {
        if (any == null)
        {
            any = new ArrayList<Element>();
        }
        return any;
    }

    /**
     * The Presence Rules all-services element.
     * <p/>
     * Compliant with rfc5025
     *
     * @author Grigorii Balutsel
     */
    public static class AllServicesType
    {
    }

    /**
     * The Presence Rules service-uri element.
     * <p/>
     * Compliant with rfc5025
     *
     * @author Grigorii Balutsel
     */
    public static class ServiceUriType
    {
        /**
         * The element value.
         */
        private String value;

        /**
         * Creates service-uri element with value.
         *
         * @param value the elemenent value to set.
         */
        public ServiceUriType(String value)
        {
            this.value = value;
        }

        /**
         * Gets the value of the value property.
         *
         * @return the value property.
         */
        public String getValue()
        {
            return value;
        }

        /**
         * Sets the value of the value property.
         *
         * @param value the value to set.
         */
        public void setValue(String value)
        {
            this.value = value;
        }
    }

    /**
     * The Presence Rules service-uri-scheme element.
     * <p/>
     * Compliant with rfc5025
     *
     * @author Grigorii Balutsel
     */
    public static class ServiceUriSchemeType
    {
        /**
         * The element value.
         */
        private String value;

        /**
         * Creates service-uri-scheme element with value.
         *
         * @param value the elemenent value to set.
         */
        public ServiceUriSchemeType(String value)
        {
            this.value = value;
        }

        /**
         * Gets the value of the value property.
         *
         * @return the value property.
         */
        public String getValue()
        {
            return value;
        }

        /**
         * Sets the value of the value property.
         *
         * @param value the value to set.
         */
        public void setValue(String value)
        {
            this.value = value;
        }
    }
}
