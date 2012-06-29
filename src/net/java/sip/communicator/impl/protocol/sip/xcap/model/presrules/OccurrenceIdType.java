package net.java.sip.communicator.impl.protocol.sip.xcap.model.presrules;

/**
 * The Presence Rules occurrence-id element.
 * <p/>
 * Compliant with rfc5025
 *
 * @author Grigorii Balutsel
 */
public class OccurrenceIdType
{
    /**
     * The element value.
     */
    private String value;

    /**
     * Creates occurrence-id element with value.
     *
     * @param value the elemenent value to set.
     */
    public OccurrenceIdType(String value)
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
