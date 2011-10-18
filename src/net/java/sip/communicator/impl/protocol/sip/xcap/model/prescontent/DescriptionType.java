/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip.xcap.model.prescontent;

import javax.xml.namespace.*;
import java.util.*;

/**
 * The PRES-CONTENT description element.
 * <p/>
 * Compliant with Presence Content XDM Specification v1.0
 *
 * @author Grigorii Balutsel
 */
public class DescriptionType
{
    /**
     * The element value.
     */
    private String value;

    /**
     * The lang attribute.
     */
    private String lang;

    /**
     * The map of any attributes.
     */
    private Map<QName, String> anyAttributes = new HashMap<QName, String>();

    /**
     * Creates description.
     */
    public DescriptionType()
    {
    }

    /**
     * Creates description with value.
     *
     * @param value the value property.
     */
    public DescriptionType(String value)
    {
        this(value, null);
    }

    /**
     * Creates description with value and lang properties.
     *
     * @param value the value property.
     * @param lang  the lang property.
     */
    public DescriptionType(String value, String lang)
    {
        this.value = value;
        this.lang = lang;
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

    /**
     * Gets the value of the lang property.
     *
     * @return the lang property.
     */
    public String getLang()
    {
        return lang;
    }

    /**
     * Sets the value of the lang property.
     *
     * @param lang the lang to set.
     */
    public void setLang(String lang)
    {
        this.lang = lang;
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
