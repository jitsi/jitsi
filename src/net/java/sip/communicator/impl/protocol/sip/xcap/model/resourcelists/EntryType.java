/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip.xcap.model.resourcelists;

import java.util.*;
import javax.xml.namespace.*;

import org.w3c.dom.*;

/**
 * The XCAP entry element.
 * <p/>
 * Compliant with rfc4825, rfc4826
 *
 * @author Grigorii Balutsel
 */
public class EntryType
{
    /**
     * The uri attribute.
     */
    private String uri;

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
     * Create the entry element.
     */
    EntryType()
    {
    }

    /**
     * Create the entry element with the uri attribute.
     *
     * @param uri the uri attribute.
     * @throws IllegalArgumentException if uri attribute is null or empty.
     */
    public EntryType(String uri)
    {
        if (uri == null || uri.trim().length() == 0)
        {
            throw new IllegalArgumentException("The uri attribute cannot be " +
                    "null or empry");
        }
        this.uri = uri;
    }

    /**
     * Gets the value of the uri property.
     *
     * @return the uri property.
     */
    public String getUri()
    {
        return uri;
    }

    /**
     * Sets the value of the uri property.
     *
     * @param uri the uri to set.
     */
    public void setUri(String uri)
    {
        this.uri = uri;
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
     * Sets the value of the any property.
     *
     * @param any the any to set.
     */
    public void setAny(List<Element> any)
    {
        this.any = any;
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

    /**
     * Sets the value of the anyAttributes property.
     *
     * @param anyAttributes the anyAttributes to set.
     */
    public void setAnyAttributes(Map<QName, String> anyAttributes)
    {
        this.anyAttributes = anyAttributes;
    }
}
