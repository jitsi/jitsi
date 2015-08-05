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
package net.java.sip.communicator.impl.protocol.sip.xcap.model.resourcelists;

import java.util.*;

import javax.xml.namespace.*;

import org.w3c.dom.*;

/**
 * The XCAP entry-ref element.
 * <p/>
 * Compliant with rfc4825, rfc4826
 *
 * @author Grigorii Balutsel
 */
public class EntryRefType
{
    /**
     * The ref attribute.
     */
    private String ref;

    /**
     * The display-name element.
     */
    private DisplayNameType displayName;

    /**
     * The list of any elements.
     */
    private List<Element> any;

    /**
     * The map of any attributes.
     */
    private Map<QName, String> anyAttributes = new HashMap<QName, String>();

    /**
     * Creates the entry-ref element
     */
    EntryRefType()
    {
    }

    /**
     * Creates the entry-ref element with the ref attribute.
     *
     * @param ref the ref attribute.
     * @throws IllegalArgumentException if ref attribute is null or empty.
     */
    public EntryRefType(String ref)
    {
        if (ref == null || ref.trim().length() == 0)
        {
            throw new IllegalArgumentException("The ref attribute cannot be " +
                    "null or empry");
        }
        this.ref = ref;
    }

    /**
     * Gets the value of the ref property.
     *
     * @return the ref property.
     */
    public String getRef()
    {
        return ref;
    }

    /**
     * Sets the value of the ref property.
     *
     * @param ref the ref to set.
     */
    public void setRef(String ref)
    {
        this.ref = ref;
    }

    /**
     * Gets the value of the displayName property.
     *
     * @return the displayName property.
     */
    public DisplayNameType getDisplayName()
    {
        return displayName;
    }

    /**
     * Sets the value of the displayName property.
     *
     * @param displayName the displayName to set.
     */
    public void setDisplayName(DisplayNameType displayName)
    {
        this.displayName = displayName;
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
        return this.any;
    }

    /**
     * Gets the value of the anyAttributes property.
     *
     * @return the anyAttributes property.
     */
    public Map<QName, String> getAnyAttributes()
    {
        return anyAttributes;
    }
}
