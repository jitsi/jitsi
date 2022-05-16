/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.sip.communicator.impl.protocol.sip.xcap.model.presrules;

import java.util.*;

import org.w3c.dom.*;

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
