/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip.xcap.model.resourcelists;

/**
 * The XCAP display-name element.
 * <p/>
 * Compliant with rfc4825, rfc4826
 *
 * @author Grigorii Balutsel
 */
public class DisplayNameType
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
     * Creates display-name.
     */
    public DisplayNameType()
    {
    }

    /**
     * Creates display-name with value and lang properties.
     *
     * @param value the value property.
     * @param lang  the lang property.
     */
    public DisplayNameType(String value, String lang)
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
}
