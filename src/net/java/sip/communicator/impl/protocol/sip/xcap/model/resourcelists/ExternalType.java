/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip.xcap.model.resourcelists;

import java.util.*;
import javax.xml.namespace.*;

import org.w3c.dom.*;

/**
 * The XCAP external element.
 * <p/>
 * Compliant with rfc4825, rfc4826
 *
 * @author Grigorii Balutsel
 */
public class ExternalType
{
    /**
     * The danchor attribute.
     */
    protected String anchor;

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
     * Gets the value of the anchor property.
     *
     * @return the anchor property.
     */
    public String getAnchor()
    {
        return anchor;
    }

    /**
     * Sets the value of the anchor property.
     *
     * @param anchor the anchor to set.
     */
    public void setAnchor(String anchor)
    {
        this.anchor = anchor;
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
