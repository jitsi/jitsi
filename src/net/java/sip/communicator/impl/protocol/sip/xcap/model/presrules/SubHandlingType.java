/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip.xcap.model.presrules;

/**
 * Specifies the subscription authorization decision that the server should
 * make.
 * <p/>
 * Compliant with rfc5025.
 *
 * @author Grigorii Balutsel
 */
//@XmlEnum
public enum SubHandlingType
{
    /**
     * This action tells the server to reject the subscription, placing it in
     * the "terminated" state.
     */
//    @XmlEnumValue("block")
    Block("block"),
    /**
     * This action tells the server to place the subscription in the "pending"
     * state, and await input from the presentity to determine how to proceed.
     */
//    @XmlEnumValue("confirm")
    Confirm("confirm"),
    /**
     * This action tells the server to place the subscription into the "active"
     * state, and to produce a presence document that indicates that the
     * presentity is unavailable.
     */
//    @XmlEnumValue("polite-block")
    PoliteBlock("polite-block"),
    /**
     * This action tells the server to place the subscription into the "active"
     * state.
     */
//    @XmlEnumValue("allow")
    Allow("allow");

    /**
     * Current enum value.
     */
    private final String value;

    /**
     * Creates enum whith the specified value.
     *
     * @param value the value to set.
     */
    SubHandlingType(String value)
    {
        this.value = value;
    }

    /**
     * Gets the value.
     *
     * @return the value
     */
    public String value()
    {
        return value;
    }
}