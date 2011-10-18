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
 * The Presence Rules provide-person element. Allows a watcher to see the
 * "person" information present in the presence document.
 * <p/>
 * Compliant with rfc5025.
 *
 * @author Grigorii Balutsel
 */
public class ProvidePersonPermissionType
{
    /**
     * The all-persons element.
     */
    private AllPersonsType allPersons;

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
     * Gets the value of the allPersons property.
     *
     * @return the allPersons property.
     */
    public AllPersonsType getAllPersons()
    {
        return allPersons;
    }

    /**
     * Sets the value of the allPersons property.
     *
     * @param allPersons the allPersons to set.
     */
    public void setAllPersons(
            AllPersonsType allPersons)
    {
        this.allPersons = allPersons;
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
     * The Presence Rules all-persons element.
     * <p/>
     * Compliant with rfc5025
     *
     * @author Grigorii Balutsel
     */
    public static class AllPersonsType
    {
    }
}
